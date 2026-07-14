# 音乐功能实现设计

> 状态：待评审 · 日期 2026-07-14 · 作者 Claude + 用户

把当前**纯占位**的音乐库页 / 音乐播放器页接上真实数据：从可配置的音乐库存储（默认 `/我的音乐`）扫描 `歌手/专辑/歌曲` 三层结构，建 Room 索引（仅路径），实现完整播放器（ExoPlayer）、同步滚动歌词、自动播放缓存、全局常驻 MiniPlayer。sign 不存索引、播放时实时取 — 索引永远不过期。

## 1. 背景与现状

### 1.1 现状（占位）

- `ui/feature/music/MusicLibraryScreen.kt` — 6 段布局（筛选 chips / hero / 最近专辑 / 艺人 / 专辑网格 / 全部歌曲）+ 底部 MiniPlayer，**全部 `StubData`**，无播放、无 API。
- `ui/feature/music/MusicPreviewScreen.kt` — 沉浸式播放器占位，300dp 渐变封面 + "即将推出" + 骨架控件 + 骨架歌词卡。
- 组件库已就绪：`ui/components/music/` 下 `AlbumCard / ArtistCard / SongRow / MiniPlayer / MusicHeroCard / CoverLetter / WaveIndicator`，签名接受 `gradient` 等参数，可直接复用。
- 导航：`AppNavHost.kt` 已有 `MusicLibraryDest` / `MusicPreviewDest`。

### 1.2 已具备的基础设施（复用）

- `AlistApi.list(url, FsListRequest(path))` → `api/fs/list` 返回目录内容（只用 name/isDir，不依赖 sign）。
- `FileDtos.toFileItem`：当前版本会顺手构造 sign URL，本设计**不使用其 downloadUrl/thumbnailUrl 字段**，只取 `name/isDir/size` 等基础字段（封装一个轻量 `toFileItemBasic` 或复用 toFileItem 后丢弃 URL 字段均可）。
- 带鉴权的 `OkHttpClient`（`di/AppModule.NetworkModule`）+ `AuthInterceptor`。
- **新增** `AlistApi.fsGet(path)`：`POST api/fs/get` → 返回 `{ name, size, sign }`，供 SignProvider 用（Alist v3 现有接口；需新增 Retrofit 方法 + DTO + 测试）。
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
│   ├── SignProvider.kt             # path → sign 内存缓存 + fsGet 调用器
│   ├── MusicIndexRepository.kt     # 扫描落库 + 读缓存 + 重扫；暴露 IndexState
│   └── model/                      # Artist / Album / Song / LrcLine 领域模型
├── playback/
│   ├── MusicPlaybackService.kt     # 前台 Service（MediaSessionService）+ ExoPlayer + 通知栏
│   ├── PlaybackController.kt       # @Singleton，封装 MediaController 命令 + state StateFlow
│   ├── PlaybackIntents.kt          # ACTION_PLAY_QUEUE / ACTION_PAUSE 等 Intent action 常量
│   └── MusicCache.kt               # @Singleton，SimpleCache（LRU 512MB）+ CacheDataSource.Factory
├── MusicLibraryViewModel.kt        # 索引状态 + 库数据流 → UI
├── MusicPlayerViewModel.kt         # 当前曲目 + 进度 + 歌词同步行
└── (ui/feature/music/ 下改造两个 Screen + 接 MiniPlayer)
```

Hilt 绑定加入 `di/AppModule.kt`（或新建 `MusicModule`）：`MusicDao`（从 AppDatabase 提供）、`MusicCache`、`PlaybackController`。Service 用 `@AndroidEntryPoint` 自动注入。

**AndroidManifest**：注册 `MusicPlaybackService`，声明 `android:foregroundServiceType="mediaPlayback"`（Android 14+ 必需）+ `android:exported="false"` + `intent-filter` 接收 `androidx.media3.session.MediaSessionService`。

## 5. 数据层设计

### 5.1 Room 实体（加入现有 AppDatabase，version 2 → 3）

> **设计原则：Room 只存 path，不存 sign。** sign 是 Alist 服务端时效性参数，会过期；索引不应该依赖它过期才能刷新。所有封面/音频/歌词直链在播放或加载时**实时拼**（`$base/p/<path>?sign=<latest>` 或 `/d/...`），sign 通过新增的 `fsGet` 接口 + `SignProvider` 缓存层提供。这样索引永远有效，「重新扫描」只为发现新增/删除，不为修 sign。

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
    val coverPath: String?,              // 封面文件路径（无则 null）
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
    val lrcPath: String?,                // 同名 .lrc 路径，无则 null
    val coverPath: String?,              // 继承专辑封面
    val sizeBytes: Long,
)
```

### 5.2 SignProvider（新模块）

- `@Singleton`，内存缓存 `Map<path, SignEntry(path, sign, expiresAtMs)>`。
- `suspend fun get(path: String, kind: SignKind /* THUMBNAIL / DOWNLOAD */): String?`：先查缓存，未命中或过期 → 调 `AlistApi.fsGet(path)` → 拿 `sign` → 存入缓存返回。
- `suspend fun primeThumbnails(paths: List<String>)`：批量预热封面 sign（库页首次进入时调）。
- 专辑封面、播放器封面、歌词加载都通过此 provider 拼 URL；ExoPlayer 准备失败（401）时调用方回调 → 让 SignProvider 强制 refresh 该 path → 重试。

### 5.2 MusicScanner（移位，原编号改为 §5.3 后统一调整）

- 输入：音乐库根路径。
- 流程（全部走 `AlistApi.list`，不取 sign）：
  1. `list(根)` → 目录项 = 歌手列表。
  2. 对每个歌手 `list(歌手路径)` → 目录项 = 专辑列表。
  3. 对每个专辑 `list(专辑路径)` → 文件项，分类：
     - 封面：名（去扩展名）== `cover` 且为图片 → 记 coverPath。
     - 歌词：`.lrc` → 存进 map（key = 去扩展名文件名）。
     - 音频：扩展名 ∈ 音频集 → 解析文件名 `序号-歌手-歌名`；成功则建 SongEntity（lrcPath 从 map 按同名取），失败跳过。
- 进度回调：`onProgress(artistsScanned, albumsScanned, songsFound)` → 驱动构建动画。
- 并发：歌手层可用有限并发（如 `Semaphore(4)`）加速，避免打爆服务器。错误容忍：单个目录 list 失败记 log 跳过，不中断整体扫描。
- 输出：`ScanResult(artists, albums, songs)`。所有路径为相对服务端根的绝对路径（如 `/我的音乐/周杰伦/晴天/01-周杰伦-晴天.mp3`）。

### 5.3 LrcParser（移位）

- 输入：.lrc 文本（通过 SignProvider 拿到直链后 OkHttp 下载）。解析 `[mm:ss.xx]歌词` 行（一行可有多个时间标签）。忽略 `[ti:][ar:][al:][by:][offset:]` 等元数据标签（offset 可选支持：整体时移毫秒）。
- 输出：`List<LrcLine(timeMs: Long, text: String)>`，按 timeMs 升序。空/无有效行 → 空列表。
- 纯函数，易单测。

### 5.4 MusicIndexRepository（移位）

- `val indexState: StateFlow<IndexState>`，`IndexState = NotIndexed | Scanning(artists, songs) | Ready | Failed(msg)`。
- `suspend fun ensureIndexed()`：若 Room 空 → 触发 `rescan()`；否则置 Ready。
- `suspend fun rescan()`：置 Scanning → 调 Scanner（转发进度）→ `clearAll()` + 批量 upsert → 置 Ready（失败置 Failed）。
- 读缓存查询（Flow）：`artists()` / `albumsByArtist(artist)` / `allAlbums()` / `songsByAlbum(...)` / `allSongs()` / `recentAlbums(limit)`。
- 下载 lrc 文本：`suspend fun loadLrc(path): String?`（先查 SignProvider 拿直链 → OkHttp 下载，供播放器页解析）。

## 6. 播放层设计

### 6.1 MusicCache

- `@Singleton`，持 `SimpleCache(cacheDir/music, LeastRecentlyUsedCacheEvictor(512MB), StandaloneDatabaseProvider)`。
- 暴露 `cacheDataSourceFactory: DataSource.Factory` = `CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(okHttpUpstream)`。
- `fun sizeBytes(): Long` / `suspend fun clear()`（供设置页）。
- 上游用 `OkHttpDataSource.Factory(existingOkHttpClient)`（media3-datasource-okhttp）。

### 6.2 MusicPlaybackService（前台 Service）

> **2026-07-14 修订**：用户要求后台播放 + 通知栏控制。ExoPlayer 必须放在前台 `Service` 里而不是 ApplicationScope 单例，这样才能在 App 退到后台后继续播放，并显示 MediaStyle 通知栏。

- 继承 `androidx.media3.session.MediaSessionService`（Media3 提供的 Service 基类，已经处理 MediaSession 生命周期 + AudioFocus + 通知栏）。
- `@AndroidEntryPoint`，Hilt 注入 `SignProvider`、`MusicCache`、`MusicIndexRepository`。
- `onCreate` 中：
  - 创建 `ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory)).setHandleAudioBecomingNoisy(true).build()`。
  - 创建 `MediaSession.Builder(this, exoPlayer).build()`，持有 session 让系统管理通知栏。
  - 设置 `Player.Listener` 更新 StateFlow（与原方案一致）。
- `onGetSession` 返回 MediaSession（系统用此与 Service 通信）。
- 通知栏：Media3 MediaSessionService 自动提供（基于 `playback_state`、专辑封面、曲名、艺人、前后台控制按钮）。无需手写 Notification。
- **音频焦点**：Media3 默认通过 `MediaSession` 处理音频焦点变更（其他 App 播歌时自动暂停、来电暂停等）。
- 启动方式：从播放入口（库页点歌、播放器页/MiniPlayer 控制）调 `ContextCompat.startForegroundService(...)` + `Intent` + 携带 `action = ACTION_PLAY_QUEUE` + 队列 + startIndex；Service `onStartCommand` 解析 → `playQueue(...)`。
- 与 ApplicationScope 关系：Service 自身生命周期决定播放，**不再用 ApplicationScope 单例**。`PlaybackController` 改为 Service 持有的内部状态暴露器（仍提供 `StateFlow<PlaybackState>`），但实际 ExoPlayer 在 Service 里。

### 6.3 PlaybackController（轻量化）

- `@Singleton`，提供**对 Service 的引用接口**：
  - `val state: StateFlow<PlaybackState>`（绑定 Service 生命周期，Service 启动时开始发射，停止时保持最后状态）。
  - `fun playQueue(context: Context, songs: List<Song>, startIndex: Int)`：启动 Service + 投递意图。
  - `fun togglePlayPause()`：通过 `MediaController`（Media3）发命令（`controller.play() / pause()`），不用直接持 player 引用。
  - `fun next() / prev() / seekTo(ms) / toggleShuffle() / cycleRepeat()`：同样通过 MediaController。
- UI 层不直接拿 ExoPlayer；用 MediaController 走 IPC 通道调用 MediaSession。

### 6.4 MVP 限制

- **支持后台播放**（前台 Service + 通知栏）。App 完全退后台仍能播。
- 不做锁屏控制（Android 8+ 锁屏控制由 MediaSession 通知栏投影自动提供，但样式依赖系统）。
- 不做蓝牙/耳机线控特定处理（Media3 默认通过 AudioFocus + MediaSession 处理基础 case）。

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
  - `Ready` → 现有 6 段布局，真实数据填充。封面用 Coil + SignProvider 拼直链加载，无则回退渐变 + `CoverLetter`（首字）。空库 → `EmptyState`。
  - `NotIndexed` → 进入即触发 `ensureIndexed()`（→ Scanning）。
  - `Failed` → `ErrorState` + 重试。
- 顶栏 action：「重新扫描」（调 `rescan()`）+ 搜索（本地过滤，可选）。
- 点歌曲/专辑 → `playQueue(...)` 并可跳播放器页。
- 底部常驻 `MiniPlayer` 读 `PlaybackController.state`，`current != null` 才显示，点击进播放器页。

### 7.3 MusicPreviewScreen（改造为真实播放器）

- 接 `MusicPlayerViewModel`（读 `PlaybackController.state` + 歌词）。
- 封面（Coil，通过 SignProvider 拼直链，无则回退渐变）+ 曲名/歌手。
- 进度条：真实 `positionMs/durationMs`，可拖动 `seekTo`；时间标签。
- 5 键控制：随机（高亮态）/ 上一首 / 播放暂停 / 下一首 / 循环（关-单曲-列表 三态图标）。
- **同步歌词**：`MusicPlayerViewModel` 用 `MusicIndexRepository.loadLrc(current.lrcPath)` → `LrcParser` → 行列表；按 `positionMs` 定位当前行索引；`LazyColumn` 高亮当前行（primary 色 + 加粗），`LaunchedEffect(currentLine)` `animateScrollToItem` 居中。无 lrcPath / 空 → "暂无歌词"。

## 8. 测试

| 测试 | 覆盖 |
|------|------|
| `LrcParserTest` | 单/多时间标签、元数据忽略、offset、空文件、非法行、时间排序、边界（0/超长） |
| `MusicScannerTest` | 三层遍历、文件名解析成功/失败跳过、封面/歌词匹配、目录 list 失败容错（MockWebServer） |
| `MusicIndexRepositoryTest` | 空库触发扫描、读缓存、rescan 清表重建、Failed 状态（MockK + Room in-memory） |
| `PlaybackControllerTest` | playQueue 启动 Service、命令通过 MediaController 转发（mock MediaController 验证）。Service 启动在 androidTest 覆盖（启动后断言 ExoPlayer 状态）。 |
| `MusicLibraryRootStoreTest` | 默认值、读写 |
| Compose UI + Roborazzi | 库页（Scanning / Ready / Empty）+ 播放器页（播放中 + 歌词）亮/暗快照 |

## 9. 已知限制与后续

- **sign 永远实时取**（通过 SignProvider + fsGet），索引不依赖 sign 永不过期；重新扫描只为发现文件变更（新增/删除）。
- **支持后台播放**：前台 Service + MediaSession 通知栏（Android 14+ 已声明 foregroundServiceType=mediaPlayback）。
- 不做锁屏自定义样式（依赖系统 MediaSession 投影）。
- 缓存容量 512MB 硬编码，后续可做成设置项。
- 搜索为本地内存过滤（若做），不调服务器 search。

## 10. 依赖新增（libs.versions.toml）

```toml
media3 = "1.4.1"
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
androidx-media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
```

（version 以实现时兼容 minSdk 26 / compileSdk 34 / targetSdk 34 的最新稳定版为准。`media3-ui` 用于可选的 PlayerView 控件，不强制使用。）
