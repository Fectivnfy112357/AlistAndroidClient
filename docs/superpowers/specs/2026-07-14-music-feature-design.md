# 音乐功能实现设计

> 状态：待评审 · 日期 2026-07-14 · 作者 Claude + 用户

把当前**纯占位**的音乐库页 / 音乐播放器页接上真实数据：从可配置的音乐库存储（默认 `/我的音乐`）扫描 `歌手/专辑/歌曲` 三层结构，建 Room 索引，实现完整播放器（ExoPlayer）、同步滚动歌词、自动播放缓存、全局常驻 MiniPlayer。

## 1. 背景与现状

### 1.1 现状（占位）

- `ui/feature/music/MusicLibraryScreen.kt` — 6 段布局（筛选 chips / hero / 最近专辑 / 艺人 / 专辑网格 / 全部歌曲）+ 底部 MiniPlayer，**全部 `StubData`**，无播放、无 API。
- `ui/feature/music/MusicPreviewScreen.kt` — 沉浸式播放器占位，300dp 渐变封面 + "即将推出" + 骨架控件 + 骨架歌词卡。
- 组件库已就绪：`ui/components/music/` 下 `AlbumCard / ArtistCard / SongRow / MiniPlayer / MusicHeroCard / CoverLetter / WaveIndicator`，签名接受 `gradient` 等参数，可直接复用。
- 导航：`AppNavHost.kt` 已有 `MusicLibraryDest` / `MusicPreviewDest`。

### 1.2 已具备的基础设施（复用）

- `AlistApi.list(url, FsListRequest(path))` → `api/fs/list` 返回目录内容。
- `FileDtos.toFileItem`：用返回的 `sign` 构建 `/d/<path>?sign=<sign>` 下载直链、`/p/<path>?sign=<sign>` 缩略图链。
- 带鉴权的 `OkHttpClient`（`di/AppModule.NetworkModule`）+ `AuthInterceptor`。
- Room 单库 `AppDatabase`（`transfer_tasks.db`，当前 version 2），已有 KSP + Room 配置。
- DataStore prefs 模式（`ThemeRepository` 用 `preferencesDataStore`）。
- `PreviewAudio.kt` 里已验证的 MediaPlayer 生命周期模式（本设计改用 ExoPlayer，仅作参考）。
- Coil 3（`coil-compose` + `coil-network-okhttp`）加载网络图片。

## 2. 需求决策（已与用户确认）

| 维度 | 决策 |
|------|------|
| 播放器范围 | **完整播放器**：播/暂停/seek + 上一首/下一首 + 队列 + 随机 + 循环（关/单曲/列表） |
| 音乐库定位 | **设置里可配置根路径**，默认 `/我的音乐` |
| 歌词 | **同步滚动歌词**（解析 .lrc 时间轴，高亮 + 居中当前行） |
| 加载策略 | **启动时全量扫描 → 落地 Room 索引 → 下次直接读缓存**；首次构建显示**构建动画** |
| 索引刷新 | **手动「重新扫描」按钮**（顶栏 action） |
| 扫描深度 | **固定三层结构**：根 / 歌手 / 专辑 / 文件 |
| 文件名解析失败 | **跳过不入库** |
| 歌曲缓存 | **自动播放缓存**（ExoPlayer SimpleCache，边播边存，LRU 淘汰） |

## 3. 目录结构规则

```
<音乐库根，默认 /我的音乐>/
├── <歌手名>/                     # 第 1 层目录 = 歌手
│   ├── <专辑名>/                 # 第 2 层目录 = 专辑
│   │   ├── cover.jpg|png|...     # 封面（可选，文件名固定 cover.*）
│   │   ├── 01-周杰伦-晴天.mp3     # 歌曲：序号-歌手名-歌曲名.<音频扩展名>
│   │   ├── 01-周杰伦-晴天.lrc     # 歌词：同名 .lrc（可选）
│   │   └── ...
```

- **音频扩展名**：`mp3 flac m4a wav ogg aac ape wma`（大小写不敏感）。
- **歌词**：`.lrc`，按去扩展名的文件名与歌曲精确匹配（`01-周杰伦-晴天.lrc` ↔ `01-周杰伦-晴天.mp3`）。
- **封面**：文件名（去扩展名）等于 `cover` 的图片文件；专辑级共享。
- **文件名解析**：按第一个和最后一个能切出三段的规则，`序号-歌手名-歌曲名` 用 `-` 分割成恰好 3 段（`split("-", limit=3)`）。序号可非数字（保留原串用于排序，数字优先）。**切不出 3 段 → 跳过该文件，不入库**（记 log）。歌手/歌名两端去空白。

## 4. 架构

```
music/
├── MusicLibraryRootStore.kt        # DataStore：音乐库根路径（默认 /我的音乐）
├── data/
│   ├── MusicEntities.kt            # ArtistEntity / AlbumEntity / SongEntity（Room）
│   ├── MusicDao.kt                 # 查询 + upsert + clearAll
│   ├── MusicScanner.kt            # fs/list 递归三层 + 文件名解析 → 扫描结果
│   ├── LrcParser.kt                # .lrc 文本 → List<LrcLine(timeMs, text)>
│   ├── MusicIndexRepository.kt     # 扫描落库 + 读缓存 + 重扫；暴露 IndexState
│   └── model/                      # Artist / Album / Song / LrcLine 领域模型
├── playback/
│   ├── PlaybackController.kt       # @Singleton，封装 ExoPlayer + 队列/随机/循环，暴露 StateFlow<PlaybackState>
│   └── MusicCache.kt               # @Singleton，SimpleCache（LRU 512MB）+ CacheDataSource.Factory
├── MusicLibraryViewModel.kt        # 索引状态 + 库数据流 → UI
├── MusicPlayerViewModel.kt         # 当前曲目 + 进度 + 歌词同步行
└── (ui/feature/music/ 下改造两个 Screen + 接 MiniPlayer)
```

Hilt 绑定加入 `di/AppModule.kt`（或新建 `MusicModule`）：`MusicDao`（从 AppDatabase 提供）、`MusicCache`、`PlaybackController`。

## 5. 数据层设计

### 5.1 Room 实体（加入现有 AppDatabase，version 2 → 3）

```kotlin
@Entity(tableName = "music_artist")
data class ArtistEntity(
    @PrimaryKey val name: String,        // 歌手目录名
    val path: String,                    // /我的音乐/周杰伦
    val albumCount: Int,
    val songCount: Int,
)

@Entity(tableName = "music_album", primaryKeys = ["artist", "name"])
data class AlbumEntity(
    val artist: String,
    val name: String,                    // 专辑目录名
    val path: String,
    val coverUrl: String?,               // /d/.../cover.jpg?sign=... ；无则 null
    val songCount: Int,
)

@Entity(tableName = "music_song")
data class SongEntity(
    @PrimaryKey val path: String,        // 歌曲完整路径（唯一）
    val trackNo: String,                 // 序号原串
    val trackNoInt: Int?,                // 数字序号（排序用，解析失败 null）
    val artist: String,
    val album: String,
    val title: String,                   // 歌曲名
    val downloadUrl: String,             // /d/.../xxx.mp3?sign=...
    val lrcUrl: String?,                 // 同名 .lrc 直链，无则 null
    val coverUrl: String?,               // 继承专辑封面
    val sizeBytes: Long,
)
```

> 注：`sign` 会随 Alist 配置变化而过期。索引存 URL 简单，但签名过期会导致直链失效。**MVP 接受此限制**：直链失效时播放/加载失败，用户点「重新扫描」即可刷新签名。（后续可改存 path，播放时实时取 sign —— 见 §9 已知限制。）

### 5.2 MusicScanner

- 输入：音乐库根路径。
- 流程（全部走 `AlistApi.list` + `toFileItem` 拿 sign）：
  1. `list(根)` → 目录项 = 歌手列表。
  2. 对每个歌手 `list(歌手路径)` → 目录项 = 专辑列表。
  3. 对每个专辑 `list(专辑路径)` → 文件项，分类：
     - 封面：名（去扩展名）== `cover` 且为图片 → 记 coverUrl。
     - 歌词：`.lrc` → 存进 map（key = 去扩展名文件名）。
     - 音频：扩展名 ∈ 音频集 → 解析文件名 `序号-歌手-歌名`；成功则建 SongEntity（lrcUrl 从 map 按同名取），失败跳过。
- 进度回调：`onProgress(artistsScanned, albumsScanned, songsFound)` → 驱动构建动画。
- 并发：歌手层可用有限并发（如 `Semaphore(4)`）加速，避免打爆服务器。错误容忍：单个目录 list 失败记 log 跳过，不中断整体扫描。
- 输出：`ScanResult(artists, albums, songs)`。

### 5.3 LrcParser

- 输入：.lrc 文本。解析 `[mm:ss.xx]歌词` 行（一行可有多个时间标签）。忽略 `[ti:][ar:][al:][by:][offset:]` 等元数据标签（offset 可选支持：整体时移毫秒）。
- 输出：`List<LrcLine(timeMs: Long, text: String)>`，按 timeMs 升序。空/无有效行 → 空列表。
- 纯函数，易单测。

### 5.4 MusicIndexRepository

- `val indexState: StateFlow<IndexState>`，`IndexState = NotIndexed | Scanning(artists, songs) | Ready | Failed(msg)`。
- `suspend fun ensureIndexed()`：若 Room 空 → 触发 `rescan()`；否则置 Ready。
- `suspend fun rescan()`：置 Scanning → 调 Scanner（转发进度）→ `clearAll()` + 批量 upsert → 置 Ready（失败置 Failed）。
- 读缓存查询（Flow）：`artists()` / `albumsByArtist(artist)` / `allAlbums()` / `songsByAlbum(...)` / `allSongs()` / `recentAlbums(limit)`。
- 下载 lrc 文本：`suspend fun loadLrc(url): String?`（走 OkHttp，供播放器页解析）。

## 6. 播放层设计

### 6.1 MusicCache

- `@Singleton`，持 `SimpleCache(cacheDir/music, LeastRecentlyUsedCacheEvictor(512MB), StandaloneDatabaseProvider)`。
- 暴露 `cacheDataSourceFactory: DataSource.Factory` = `CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(okHttpUpstream)`。
- `fun sizeBytes(): Long` / `suspend fun clear()`（供设置页）。
- 上游用 `OkHttpDataSource.Factory(existingOkHttpClient)`（media3-datasource-okhttp）。

### 6.2 PlaybackController

- `@Singleton`，注入 `@ApplicationScope` 作用域 + `MusicCache` + `@ApplicationContext`。
- 内部 `ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory)).build()`。
- 暴露 `val state: StateFlow<PlaybackState>`：

```kotlin
data class PlaybackState(
    val current: Song?,
    val queue: List<Song>,
    val index: Int,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffle: Boolean,
    val repeat: RepeatMode,        // OFF / ONE / ALL
    val isBuffering: Boolean,
)
```

- API：`playQueue(songs, startIndex)` / `togglePlayPause()` / `next()` / `prev()` / `seekTo(ms)` / `toggleShuffle()` / `cycleRepeat()`。
- 队列直接用 ExoPlayer 原生：`setMediaItems(songs.map { MediaItem })` + `seekTo(index,0)`、`seekToNext/Previous`、`shuffleModeEnabled`、`repeatMode`。
- 状态同步：`Player.Listener`（`onIsPlayingChanged` / `onPlaybackStateChanged` / `onMediaItemTransition` / `onPositionDiscontinuity`）更新 StateFlow；播放中一个 `while(isPlaying){ position=..; delay(250) }` 协程刷新 positionMs。
- **单例 + ApplicationScope** → MiniPlayer 与播放器页共享同一状态，跨页保持播放。ViewModel 只读 state、转发意图，不持 player。
- 线程：ExoPlayer 必须主线程调用；Controller 内部切主线程。

### 6.3 MVP 限制

不做前台 Service / 通知栏 / 锁屏控制 / 后台播放（与 `docs/testing/known-limitations.md` 一致，音频仅 App 前台）。App 退到后台由系统常规行为处理。

## 7. UI 设计

### 7.1 音乐库根路径设置（MusicLibraryRootStore）

- DataStore（复用 `preferencesDataStore` 模式，名 `music_prefs`，key `library_root`），默认 `/我的音乐`。
- 设置页 `SettingsContent` 加「音乐」区块：
  - 音乐库根路径（可编辑文本，改后提示需重新扫描）。
  - 音乐缓存：显示已用大小 + 「清除缓存」按钮（调 `MusicCache.clear()`）。

### 7.2 MusicLibraryScreen（改造）

- 删除 `StubData`，接 `MusicLibraryViewModel`。
- 按 `indexState` 分支：
  - `Scanning(artists, songs)` → **构建动画**：居中 `WaveIndicator`/进度圈 + "正在建立音乐索引…" + "已扫描 N 位歌手 · M 首歌曲"。
  - `Ready` → 现有 6 段布局，真实数据填充。封面用 Coil 加载 `coverUrl`，无则回退渐变 + `CoverLetter`（首字）。空库 → `EmptyState`。
  - `NotIndexed` → 进入即触发 `ensureIndexed()`（→ Scanning）。
  - `Failed` → `ErrorState` + 重试。
- 顶栏 action：「重新扫描」（调 `rescan()`）+ 搜索（本地过滤，可选）。
- 点歌曲/专辑 → `playQueue(...)` 并可跳播放器页。
- 底部常驻 `MiniPlayer` 读 `PlaybackController.state`，`current != null` 才显示，点击进播放器页。

### 7.3 MusicPreviewScreen（改造为真实播放器）

- 接 `MusicPlayerViewModel`（读 `PlaybackController.state` + 歌词）。
- 封面（Coil，回退渐变）+ 曲名/歌手。
- 进度条：真实 `positionMs/durationMs`，可拖动 `seekTo`；时间标签。
- 5 键控制：随机（高亮态）/ 上一首 / 播放暂停 / 下一首 / 循环（关-单曲-列表 三态图标）。
- **同步歌词**：`MusicPlayerViewModel` 用 `MusicIndexRepository.loadLrc(current.lrcUrl)` → `LrcParser` → 行列表；按 `positionMs` 定位当前行索引；`LazyColumn` 高亮当前行（primary 色 + 加粗），`LaunchedEffect(currentLine)` `animateScrollToItem` 居中。无 lrcUrl / 空 → "暂无歌词"。

## 8. 测试

| 测试 | 覆盖 |
|------|------|
| `LrcParserTest` | 单/多时间标签、元数据忽略、offset、空文件、非法行、时间排序、边界（0/超长） |
| `MusicScannerTest` | 三层遍历、文件名解析成功/失败跳过、封面/歌词匹配、目录 list 失败容错（MockWebServer） |
| `MusicIndexRepositoryTest` | 空库触发扫描、读缓存、rescan 清表重建、Failed 状态（MockK + Room in-memory） |
| `PlaybackControllerTest` | 队列设置、next/prev、shuffle、repeat 三态、边界（队首 prev / 队尾 next）。ExoPlayer 用 Robolectric 或抽象接口 mock |
| `MusicLibraryRootStoreTest` | 默认值、读写 |
| Compose UI + Roborazzi | 库页（Scanning / Ready / Empty）+ 播放器页（播放中 + 歌词）亮/暗快照 |

## 9. 已知限制与后续

- **签名过期**：索引存的 `/d/?sign=` 直链会随 Alist 签名配置过期而失效；MVP 靠「重新扫描」刷新。后续可改存 path、播放时实时取 sign。
- 无后台播放 / 通知栏（同项目既有 MVP 限制）。
- 缓存容量 512MB 硬编码，后续可做成设置项。
- 搜索为本地内存过滤（若做），不调服务器 search。

## 10. 依赖新增（libs.versions.toml）

```toml
media3 = "1.4.1"
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }
```

（version 以实现时兼容 minSdk 26 / compileSdk 34 的最新稳定版为准。）
