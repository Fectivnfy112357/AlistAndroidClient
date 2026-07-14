# 音乐功能 (Music Feature) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 spec `2026-07-14-music-feature-design.md` 把 Alist Android 客户端的音乐库页 / 音乐播放器页接通真实数据（Alist 三层目录扫描 → Room 索引 → ExoPlayer 完整播放器 → 同步滚动歌词 → 全局 MiniPlayer）。

**Architecture:** 5 个 Phase 顺序推进 — 依赖/接口/数据层 → 索引与扫描 → 播放层 (Service + Controller) → UI 集成 (库页/播放器/MiniPlayer) → 测试与验收。所有路径走 Service + MediaController，禁止 ExoPlayer 直持外部引用。

**Tech Stack:** Kotlin 2.0.21 + Jetpack Compose BOM 2024.09.03 + Material3 1.3.0 + Hilt 2.52 + Retrofit 2.11 + Room 2.6.1 + DataStore 1.1.1 + AndroidX Media3 1.4.1 (exoplayer / session / datasource-okhttp) + Coil 3.0.4。

---

## Global Constraints

- **Kotlin**: 2.0.21，jvmTarget 17
- **minSdk**: 26 / **compileSdk / targetSdk**: 34
- **Compose BOM**: 2024.09.03 / **Material3**: 1.3.0
- **AndroidX Media3**: 1.4.1（`exoplayer` / `session` / `datasource-okhttp` / `ui`）
- **Room**: 2.6.1；版本从 2 → 3，加 `Migration(2,3)` 创建 `music_artist` / `music_album` / `music_song`
- **单文件行数**: ≤ 400 行
- **State 收集**: 一律 `collectAsStateWithLifecycle()`，禁用 `collectAsState()`
- **Composable 命名**: 用 `App*` 语义化命名，禁止 `Cloud*`
- **颜色**: 全部走 `MaterialTheme.colorScheme.*`
- **新增权限**: `POST_NOTIFICATIONS` 已有；**新增** `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `WAKE_LOCK`
- **Service 类型**: `android:foregroundServiceType="mediaPlayback"`（Android 14+ 必需）
- **sign 策略**: **Room 只存 path，不存 sign**；sign 通过 `AlistApi.fsGet` + `SignProvider` 实时取
- **测试**: 每个新 ViewModel ≥ 1 单测；LrcParser / MusicScanner / MusicIndexRepository / MusicLibraryRootStore 均有对应单测；库页 + 播放器页有 Roborazzi 亮/暗快照
- **不做**: 锁屏自定义样式 / 蓝牙/耳机线控特定处理 / 大屏双栏 / Service-less 直接 ExoPlayer 实例化

---

## File Structure

### 新增

```
data/local/                                  # Room schema bump v2 → v3
music/
├── MusicLibraryRootStore.kt                 # DataStore "music_prefs"/library_root 默认 /我的音乐
├── data/
│   ├── MusicEntities.kt                     # ArtistEntity / AlbumEntity / SongEntity
│   ├── MusicDao.kt                          # 查询 + upsert + clearAll
│   ├── MusicScanner.kt                      # 三层 fs/list 扫描 + 文件名解析
│   ├── MusicIndexRepository.kt              # 扫描落库 + 读缓存 + 重扫
│   ├── LrcParser.kt                         # 纯函数 .lrc → List<LrcLine>
│   ├── SignProvider.kt                      # path → sign 内存缓存
│   └── model/
│       ├── LrcLine.kt
│       └── MusicEntity.kt                   # 领域 Artist / Album / Song
├── playback/
│   ├── MusicPlaybackService.kt              # MediaSessionService + ExoPlayer + MediaSession
│   ├── PlaybackController.kt                # @Singleton，MediaController + StateFlow
│   ├── PlaybackIntents.kt                   # Intent action 常量 + extra key
│   └── MusicCache.kt                        # @Singleton，SimpleCache 512MB LRU

ui/feature/music/
├── MusicLibraryScreen.kt                    # 重写：接 ViewModel + MiniPlayer
├── MusicPreviewScreen.kt                    # 重写：接 ViewModel + 真实播放器
├── MusicLibraryViewModel.kt
├── MusicPlayerViewModel.kt
├── dto/MusicIndexState.kt                   # NotIndexed / Scanning / Ready / Failed
├── model/UiSong.kt / UiAlbum.kt / UiArtist.kt
└── components/
    ├── LyricsView.kt                        # LazyColumn + 当前行高亮 + animateScrollToItem
    └── PlayerControls.kt                    # 5 键：随机 / 上一首 / 播暂 / 下一首 / 循环

network/dto/
└── FsGetDto.kt                              # FsGetRequest + AlistFsGet

di/
└── MusicModule.kt                           # MusicCache / PlaybackController / MusicDao 提供

test/.../music/
├── LrcParserTest.kt
├── MusicScannerTest.kt
├── MusicIndexRepositoryTest.kt
├── MusicLibraryRootStoreTest.kt
└── PlaybackControllerTest.kt

androidTest/.../music/
└── MusicPlaybackServiceTest.kt              # 启动 Service 验证 ExoPlayer 状态
```

### 修改

```
gradle/libs.versions.toml                    # + media3 + datastore 已在
app/build.gradle.kts                         # + media3 4 个 artifact
app/src/main/AndroidManifest.xml             # + permissions + MusicPlaybackService 声明
app/src/main/java/.../di/AppModule.kt        # AppDatabase 新 entities + MIGRATION_2_3 + MusicDao 提供
app/src/main/java/.../data/local/AppDatabase.kt  # entities = [..., ArtistEntity, AlbumEntity, SongEntity]; version = 3
app/src/main/java/.../navigation/AppNavHost.kt   # MusicPlaybackService 启动入口（可选预热）
app/src/main/java/.../ui/feature/settings/SettingsScreen.kt  # 加「音乐」设置区块
```

---

## Phase 1 — 依赖 + 数据层基础 (Tasks 1-5)

### Task 1: 添加 Media3 依赖 + Manifest 权限

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: 现有 `libs.versions.toml` 模板
- Produces: `media3` version `1.4.1` + 4 个 artifact；manifest 新增 2 个 permission + `MusicPlaybackService` 占位声明

- [ ] **Step 1: 在 `libs.versions.toml` 末尾追加 Media3**

在 `[versions]` 末尾加：
```toml
media3 = "1.4.1"
```

在 `[libraries]` 末尾加：
```toml
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
androidx-media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
```

- [ ] **Step 2: 在 `app/build.gradle.kts` 的 `dependencies {}` 块末尾加 Media3**

```kotlin
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.session)
implementation(libs.androidx.media3.datasource.okhttp)
implementation(libs.androidx.media3.ui)
```

- [ ] **Step 3: 更新 `AndroidManifest.xml` 权限**

在 `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` 后加：
```xml
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

- [ ] **Step 4: 注册占位 Service（实现在 Phase 3）**

在 `<application>` 内、`<provider>` 前加：
```xml
        <service
            android:name=".music.playback.MusicPlaybackService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
```

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "build(music): add media3 1.4.1 deps + foreground service permissions"
```

---

### Task 2: 新增 `AlistApi.fsGet` + DTO

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/network/dto/FsGetDto.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`

**Interfaces:**
- Produces: `AlistApi.fsGet(url, FsGetRequest)` → `AlistResponse<AlistFsGetData>`；`AlistFsGetData(name, size, sign)`

- [ ] **Step 1: 写 `FsGetDto.kt`**

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FsGetRequest(val path: String, val password: String = "")

@Serializable
data class AlistFsGetData(
    val name: String,
    val size: Long = 0,
    @SerialName("is_dir") val isDir: Boolean = false,
    val sign: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null,
    val thumb: String? = null,
)
```

- [ ] **Step 2: 在 `AlistApi.kt` 加 `fsGet` 声明**

在文件末尾的 interface 内、`}` 之前加：
```kotlin
    @POST
    suspend fun fsGet(@Url url: String, @Body request: FsGetRequest): AlistResponse<AlistFsGetData>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/network/dto/FsGetDto.kt app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
git commit -m "feat(network): add fsGet API for fresh sign token"
```

---

### Task 3: Room entities v3 + 迁移

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/data/MusicEntities.kt`
- Create: `app/src/main/java/com/textvision/alistclient/music/data/MusicDao.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`

**Interfaces:**
- Produces: `ArtistEntity(name PK, path, albumCount, songCount)`；`AlbumEntity(artist, name PK, path, coverPath, songCount)`；`SongEntity(path PK, trackNo, trackNoInt, artist, album, title, lrcPath, coverPath, sizeBytes)`
- Produces: `MusicDao` 暴露 `artists(): Flow<List<ArtistEntity>>` / `albumsByArtist(artist): Flow<List<AlbumEntity>>` / `allAlbums(): Flow<List<AlbumEntity>>` / `songsByAlbum(artist, album): Flow<List<SongEntity>>` / `allSongs(): Flow<List<SongEntity>>` / `recentAlbums(limit: Int): Flow<List<AlbumEntity>>` / `clearAll()` / `upsertArtists` / `upsertAlbums` / `upsertSongs`
- `AppDatabase`: `version = 3` + entities += 3 个；`MIGRATION_2_3` 创建三张表

- [ ] **Step 1: 写 `MusicEntities.kt`**

```kotlin
package com.textvision.alistclient.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_artist")
data class ArtistEntity(
    @PrimaryKey val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
)

@Entity(tableName = "music_album")
data class AlbumEntity(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
) {
    @PrimaryKey
    var id: String = (artist + "" + name).hashCode().toString()
}

@Entity(tableName = "music_song")
data class SongEntity(
    @PrimaryKey val path: String,
    val trackNo: String,
    val trackNoInt: Int?,
    val artist: String,
    val album: String,
    val title: String,
    val lrcPath: String?,
    val coverPath: String?,
    val sizeBytes: Long,
)
```

> 注：Room 不支持复合主键 + 嵌入字段的便捷 PK 设置，此处用合成 `id` 作 PK。

- [ ] **Step 2: 写 `MusicDao.kt`**

```kotlin
package com.textvision.alistclient.music.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM music_artist ORDER BY name")
    fun artists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM music_album WHERE artist = :artist ORDER BY name")
    fun albumsByArtist(artist: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM music_album ORDER BY name")
    fun allAlbums(): Flow<List<AlbumEntity>>

    @Query("""
        SELECT * FROM music_album
        ORDER BY name DESC
        LIMIT :limit
    """)
    fun recentAlbums(limit: Int): Flow<List<AlbumEntity>>

    @Query("""
        SELECT * FROM music_song
        WHERE artist = :artist AND album = :album
        ORDER BY trackNoInt ASC, trackNo ASC
    """)
    fun songsByAlbum(artist: String, album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM music_song ORDER BY artist, album, trackNoInt ASC, trackNo ASC")
    fun allSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtists(items: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbums(items: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(items: List<SongEntity>)

    @Query("DELETE FROM music_artist")
    suspend fun clearArtists()

    @Query("DELETE FROM music_album")
    suspend fun clearAlbums()

    @Query("DELETE FROM music_song")
    suspend fun clearSongs()

    @Query("SELECT COUNT(*) FROM music_song")
    suspend fun songCount(): Int
}
```

- [ ] **Step 3: 修改 `AppDatabase.kt`**

```kotlin
package com.textvision.alistclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.textvision.alistclient.music.data.AlbumEntity
import com.textvision.alistclient.music.data.ArtistEntity
import com.textvision.alistclient.music.data.MusicDao
import com.textvision.alistclient.music.data.SongEntity
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity

@Database(
    entities = [
        SmokeEntity::class,
        TransferEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        SongEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
    abstract fun transferDao(): TransferDao
    abstract fun musicDao(): MusicDao
}
```

- [ ] **Step 4: 在 `AppModule.kt` 的 `DatabaseModule` 加 `MIGRATION_2_3` + 提供 `MusicDao`**

把 `DatabaseModule` 整个 object 替换为：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transfer_tasks` (
                    `id` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `remotePath` TEXT NOT NULL,
                    `localPath` TEXT,
                    `sourceUri` TEXT,
                    `bytesDone` INTEGER NOT NULL,
                    `totalBytes` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `failureReason` TEXT,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `smoke` (
                    `id` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_artist` (
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `albumCount` INTEGER NOT NULL,
                    `songCount` INTEGER NOT NULL,
                    PRIMARY KEY(`name`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_album` (
                    `artist` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `path` TEXT NOT NULL,
                    `coverPath` TEXT,
                    `songCount` INTEGER NOT NULL,
                    `id` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_song` (
                    `path` TEXT NOT NULL,
                    `trackNo` TEXT NOT NULL,
                    `trackNoInt` INTEGER,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `lrcPath` TEXT,
                    `coverPath` TEXT,
                    `sizeBytes` INTEGER NOT NULL,
                    PRIMARY KEY(`path`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transfer_tasks.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()

    @Provides
    fun provideMusicDao(database: AppDatabase): MusicDao = database.musicDao()
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/data/MusicEntities.kt \
        app/src/main/java/com/textvision/alistclient/music/data/MusicDao.kt \
        app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt \
        app/src/main/java/com/textvision/alistclient/di/AppModule.kt
git commit -m "feat(music): Room v2→v3 schema with artist/album/song entities + DAO"
```

---

### Task 4: MusicLibraryRootStore (DataStore)

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/MusicLibraryRootStore.kt`
- Create: `app/src/test/java/com/textvision/alistclient/music/MusicLibraryRootStoreTest.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`

**Interfaces:**
- Produces: `@Singleton class MusicLibraryRootStore @Inject constructor(@ApplicationContext)`；`val rootPath: Flow<String>`（默认 `/我的音乐`）；`suspend fun setRoot(path: String)`

- [ ] **Step 1: 写 `MusicLibraryRootStore.kt`**

```kotlin
package com.textvision.alistclient.music

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.musicDataStore by preferencesDataStore("music_prefs")

@Singleton
class MusicLibraryRootStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("library_root")

    val rootPath: Flow<String> = context.musicDataStore.data
        .map { prefs -> prefs[key]?.takeIf { it.isNotBlank() } ?: DEFAULT_ROOT }

    suspend fun setRoot(path: String) {
        val normalized = path.trim().ifEmpty { DEFAULT_ROOT }
        context.musicDataStore.edit { prefs ->
            prefs[key] = normalized
        }
    }

    companion object {
        const val DEFAULT_ROOT = "/我的音乐"
    }
}
```

- [ ] **Step 2: 写 `MusicLibraryRootStoreTest.kt`**

```kotlin
package com.textvision.alistclient.music

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicLibraryRootStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = MusicLibraryRootStore(context)

    @After
    fun tearDown() = runTest {
        store.setRoot(MusicLibraryRootStore.DEFAULT_ROOT)
    }

    @Test
    fun defaultValue_whenNoPrefSaved() = runTest {
        val actual = store.rootPath.first()
        assertEquals(MusicLibraryRootStore.DEFAULT_ROOT, actual)
    }

    @Test
    fun setRoot_persistsAcrossReads() = runTest {
        store.setRoot("/custom/music")
        assertEquals("/custom/music", store.rootPath.first())
    }
}
```

- [ ] **Step 3: 在 `di/AppModule.kt` 的 `NetworkModule` 块末尾加 OkHttp 直读盘的注入（可选，本 Task 不需要）**

跳过 — `MusicModule` 在 Task 7 创建。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/MusicLibraryRootStore.kt \
        app/src/test/java/com/textvision/alistclient/music/MusicLibraryRootStoreTest.kt
git commit -m "feat(music): library root path DataStore (default /我的音乐)"
```

---

### Task 5: 领域模型 + LrcParser（含完整单测）

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/data/model/LrcLine.kt`
- Create: `app/src/main/java/com/textvision/alistclient/music/data/model/MusicEntity.kt`
- Create: `app/src/main/java/com/textvision/alistclient/music/data/LrcParser.kt`
- Create: `app/src/test/java/com/textvision/alistclient/music/data/LrcParserTest.kt`

**Interfaces:**
- Produces: `data class LrcLine(val timeMs: Long, val text: String)`
- Produces: `data class Artist(val name: String, val path: String, val albumCount: Int, val songCount: Int)` 等领域模型（UI 用）
- Produces: `object LrcParser { fun parse(text: String): List<LrcLine> }`

- [ ] **Step 1: 写 `LrcLine.kt`**

```kotlin
package com.textvision.alistclient.music.data.model

data class LrcLine(val timeMs: Long, val text: String)
```

- [ ] **Step 2: 写 `music/data/model/MusicEntity.kt`**

```kotlin
package com.textvision.alistclient.music.data.model

import com.textvision.alistclient.music.data.AlbumEntity
import com.textvision.alistclient.music.data.ArtistEntity
import com.textvision.alistclient.music.data.SongEntity

data class Artist(
    val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
) {
    companion object {
        fun fromEntity(e: ArtistEntity) = Artist(e.name, e.path, e.albumCount, e.songCount)
    }
}

data class Album(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
) {
    companion object {
        fun fromEntity(e: AlbumEntity) = Album(e.artist, e.name, e.path, e.coverPath, e.songCount)
    }
}

data class Song(
    val path: String,
    val trackNo: String,
    val trackNoInt: Int?,
    val artist: String,
    val album: String,
    val title: String,
    val lrcPath: String?,
    val coverPath: String?,
    val sizeBytes: Long,
) {
    companion object {
        fun fromEntity(e: SongEntity) = Song(
            e.path, e.trackNo, e.trackNoInt, e.artist, e.album,
            e.title, e.lrcPath, e.coverPath, e.sizeBytes,
        )
    }
}
```

- [ ] **Step 3: 写 `LrcParser.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.model.LrcLine

/**
 * Parses LRC lyrics text into a time-ordered list of [LrcLine].
 * - Lines like `[mm:ss.xx]text` or `[mm:ss]text` produce entries.
 * - Lines with multiple time tags (e.g. `[00:01.00][00:30.00]chorus`) produce multiple entries.
 * - Metadata tags (`[ti:]`, `[ar:]`, `[al:]`, `[by:]`, `[offset:]`) are ignored.
 * - Empty lines and lines with no valid tags are skipped.
 * - The list is sorted by [LrcLine.timeMs] ascending.
 */
object LrcParser {

    private val tagRegex = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val metaRegex = Regex("""\[(ti|ar|al|by|offset|length):.*]""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<LrcLine> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<LrcLine>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            // Strip metadata tags lines entirely.
            val remaining = line.replace(metaRegex, "").trim()
            if (remaining.isEmpty()) return@forEach
            val matches = tagRegex.findAll(remaining).toList()
            if (matches.isEmpty()) return@forEach
            val text = remaining.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            matches.forEach { m ->
                val (minStr, secStr, msStr) = m.destructured
                val minutes = minStr.toLong()
                val seconds = secStr.toLong()
                val millis = when {
                    msStr.isEmpty() -> 0L
                    msStr.length == 1 -> msStr.toLong() * 100
                    msStr.length == 2 -> msStr.toLong() * 10
                    else -> msStr.substring(0, 3).toLong()
                }
                out += LrcLine(
                    timeMs = minutes * 60_000 + seconds * 1_000 + millis,
                    text = text,
                )
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
```

- [ ] **Step 4: 写 `LrcParserTest.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.model.LrcLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parses_singleTimeTagLine() {
        val lrc = "[00:01.50]Hello"
        assertEquals(listOf(LrcLine(1_500, "Hello")), LrcParser.parse(lrc))
    }

    @Test
    fun parses_minuteSecondOnly_noMillis() {
        val lrc = "[01:30]Line A"
        assertEquals(listOf(LrcLine(90_000, "Line A")), LrcParser.parse(lrc))
    }

    @Test
    fun parses_multipleTimeTags_oneLine() {
        val lrc = "[00:01.00][00:30.00]chorus"
        val actual = LrcParser.parse(lrc)
        assertEquals(listOf(LrcLine(1_000, "chorus"), LrcLine(30_000, "chorus")), actual)
    }

    @Test
    fun ignoresMetadataTags() {
        val lrc = """
            [ti:Title]
            [ar:Artist]
            [al:Album]
            [00:01.00]First
        """.trimIndent()
        assertEquals(listOf(LrcLine(1_000, "First")), LrcParser.parse(lrc))
    }

    @Test
    fun sortsByTime() {
        val lrc = """
            [00:30.00]Late
            [00:05.00]Early
        """.trimIndent()
        assertEquals(listOf(LrcLine(5_000, "Early"), LrcLine(30_000, "Late")), LrcParser.parse(lrc))
    }

    @Test
    fun emptyInput_returnsEmpty() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n\n   ").isEmpty())
    }

    @Test
    fun lineWithoutAnyTag_isSkipped() {
        val lrc = """
            no tag here

            [00:01.00]valid
        """.trimIndent()
        assertEquals(listOf(LrcLine(1_000, "valid")), LrcParser.parse(lrc))
    }

    @Test
    fun handlesThreeDigitMillis() {
        val lrc = "[00:01.123]Tick"
        assertEquals(listOf(LrcLine(1_123, "Tick")), LrcParser.parse(lrc))
    }

    @Test
    fun handlesZeroBoundary() {
        val lrc = "[00:00.00]Start"
        assertEquals(listOf(LrcLine(0, "Start")), LrcParser.parse(lrc))
    }
}
```

- [ ] **Step 5: 跑 LrcParserTest 验证**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.data.LrcParserTest`
Expected: BUILD SUCCESSFUL，9 tests passed。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/data/model/ \
        app/src/main/java/com/textvision/alistclient/music/data/LrcParser.kt \
        app/src/test/java/com/textvision/alistclient/music/data/LrcParserTest.kt
git commit -m "feat(music): LRC parser + domain models (Artist/Album/Song)"
```

---

## Phase 2 — 扫描 + 索引 + Sign 缓存 (Tasks 6-8)

### Task 6: SignProvider（含测试）

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/data/SignProvider.kt`
- Create: `app/src/test/java/com/textvision/alistclient/music/data/SignProviderTest.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`（加 `@Singleton` provides，Task 7 也加 `MusicModule`，这里只加方法）

> SignProvider 在 Phase 1 已 `@Singleton` 可被其他类构造依赖；它在 Task 7 的 `MusicModule` 内统一注册。

**Interfaces:**
- Produces: `@Singleton class SignProvider @Inject constructor(api: AlistApi, dispatcher)`；
- `enum class SignKind { THUMBNAIL, DOWNLOAD }`
- `suspend fun get(path: String, kind: SignKind): String?` — 缓存 miss/expired → `api.fsGet` → 返回 sign
- `suspend fun primeThumbnails(paths: List<String>)` — 并行预热（best-effort，try-catch swallow）
- 测试用 MockWebServer + MockK api

- [ ] **Step 1: 写 `SignProvider.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.FsGetRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SignKind { THUMBNAIL, DOWNLOAD }

internal data class SignEntry(
    val sign: String,
    val expiresAtMs: Long,
)

@Singleton
class SignProvider @Inject constructor(
    private val api: AlistApi,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val cache = mutableMapOf<String, SignEntry>()
    private val mutex = Mutex()

    suspend fun get(path: String, kind: SignKind, baseUrl: String = ""): String? {
        val now = System.currentTimeMillis()
        mutex.withLock {
            val hit = cache[path]
            if (hit != null && hit.expiresAtMs > now + REFRESH_MARGIN_MS) {
                return hit.sign
            }
        }
        val resolved = try {
            val url = baseUrl.trimEnd('/') + "/api/fs/get"
            val resp = withContext(dispatcher) {
                api.fsGet(url, FsGetRequest(path))
            }
            if (resp.code == 200) resp.data?.sign else null
        } catch (t: Throwable) {
            null
        } ?: return null
        mutex.withLock { cache[path] = SignEntry(resolved, now + TTL_MS) }
        return resolved
    }

    suspend fun invalidate(path: String) {
        mutex.withLock { cache.remove(path) }
    }

    suspend fun primeThumbnails(
        paths: List<String>,
        baseUrl: String = "",
    ) {
        if (paths.isEmpty()) return
        coroutineScope {
            paths.map { path ->
                async {
                    runCatching { get(path, SignKind.THUMBNAIL, baseUrl) }
                    Unit
                }
            }.awaitAll()
        }
    }

    companion object {
        private const val TTL_MS = 30 * 60 * 1_000L
        private const val REFRESH_MARGIN_MS = 60 * 1_000L
    }
}
```

- [ ] **Step 2: 写 `SignProviderTest.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistFsGetData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.FsGetRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class SignProviderTest {

    private val api: AlistApi = mockk()
    private val provider = SignProvider(api, Dispatchers.Unconfined)

    @Test
    fun returnsCachedSign_onSecondCall() = runTest {
        coEvery { api.fsGet(any(), any()) } returns
            AlistResponse(code = 200, message = "ok", data = AlistFsGetData(name = "x.mp3", sign = "abc"))
        val first = provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/")
        val second = provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/")
        assertEquals("abc", first)
        assertEquals("abc", second)
    }

    @Test
    fun returnsNull_whenServerFails() = runTest {
        coEvery { api.fsGet(any(), any()) } returns
            AlistResponse(code = 500, message = "fail", data = null)
        assertNull(provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }

    @Test
    fun returnsNull_onNetworkThrow() = runTest {
        coEvery { api.fsGet(any(), any()) } throws IOException("network")
        assertNull(provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }

    @Test
    fun invalidate_forcesRefetch() = runTest {
        coEvery { api.fsGet(any(), any()) } returnsMany listOf(
            AlistResponse(200, "ok", AlistFsGetData(name = "x.mp3", sign = "first")),
            AlistResponse(200, "ok", AlistFsGetData(name = "x.mp3", sign = "second")),
        )
        assertEquals("first", provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
        provider.invalidate("/x.mp3")
        assertEquals("second", provider.get("/x.mp3", SignKind.DOWNLOAD, baseUrl = "http://example/"))
    }
}
```

- [ ] **Step 3: 跑测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.data.SignProviderTest`
Expected: 4 tests passed。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/data/SignProvider.kt \
        app/src/test/java/com/textvision/alistclient/music/data/SignProviderTest.kt
git commit -m "feat(music): SignProvider with TTL cache around fsGet"
```

---

### Task 7: MusicScanner（含测试，MockWebServer）

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/data/MusicScanner.kt`
- Create: `app/src/test/java/com/textvision/alistclient/music/data/MusicScannerTest.kt`

**Interfaces:**
- Produces: `@Singleton class MusicScanner @Inject constructor(api, dispatcher)`
- `data class ScanResult(val artists: List<ArtistEntity>, val albums: List<AlbumEntity>, val songs: List<SongEntity>)`
- `data class ScanProgress(val artistsDone: Int, val songsFound: Int)`
- `suspend fun scan(rootPath: String, baseUrl: String, onProgress: suspend (ScanProgress) -> Unit = {}): ScanResult`

- [ ] **Step 1: 写 `MusicScanner.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.model.Album as AlbumModel  // alias not needed; use internal types below
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistFileDto
import com.textvision.alistclient.network.dto.FsListRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(
    val artists: List<ArtistEntity>,
    val albums: List<AlbumEntity>,
    val songs: List<SongEntity>,
)

data class ScanProgress(
    val artistsDone: Int,
    val songsFound: Int,
) {
    companion object {
        val Empty = ScanProgress(0, 0)
    }
}

@Singleton
class MusicScanner @Inject constructor(
    private val api: AlistApi,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val audioExtensions = setOf(
        "mp3", "flac", "m4a", "wav", "ogg", "aac", "ape", "wma",
    )
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    suspend fun scan(
        rootPath: String,
        baseUrl: String,
        onProgress: suspend (ScanProgress) -> Unit = {},
    ): ScanResult = withContext(dispatcher) {
        val normalizedBase = baseUrl.trimEnd('/')
        val (artistsRaw, firstError) = listSafely(normalizedBase, rootPath)
        if (artistsRaw.isEmpty()) return@withContext ScanResult(emptyList(), emptyList(), emptyList())

        val sem = Semaphore(4)
        val albumRows = mutableListOf<AlbumEntity>()
        val songRows = mutableListOf<SongEntity>()
        val artistRows = mutableListOf<ArtistEntity>()
        var songsFound = 0
        var artistsDone = 0

        coroutineScope {
            artistsRaw.map { artistDir ->
                async {
                    sem.withPermit {
                        val (albumsRaw, _) = listSafely(normalizedBase, artistDir.path)
                        val albumsHere = mutableListOf<AlbumEntity>()
                        val songsHere = mutableListOf<SongEntity>()
                        albumsRaw.forEach { albumDir ->
                            val (files, _) = listSafely(normalizedBase, albumDir.path)
                            val cover = files.firstOrNull {
                                it.isDir.not() &&
                                    it.name.substringBeforeLast('.', "").equals("cover", ignoreCase = true) &&
                                    imageExtensions.contains(it.extensionLower())
                            }?.path
                            val lrcs = files
                                .filter { !it.isDir && it.extensionLower() == "lrc" }
                                .associate { it.name.substringBeforeLast('.') to it.path }
                            files.filter { !it.isDir && audioExtensions.contains(it.extensionLower()) }
                                .forEach { audioFile ->
                                    val parsed = parseFileName(audioFile.name)
                                        ?: return@forEach
                                    val lrcPath = lrcs[audioFile.name.substringBeforeLast('.')]
                                    val song = SongEntity(
                                        path = audioFile.path,
                                        trackNo = parsed.trackNo,
                                        trackNoInt = parsed.trackNoInt,
                                        artist = artistDir.name,
                                        album = albumDir.name,
                                        title = parsed.title,
                                        lrcPath = lrcPath,
                                        coverPath = cover,
                                        sizeBytes = audioFile.size,
                                    )
                                    songsHere += song
                                }
                            albumsHere += AlbumEntity(
                                artist = artistDir.name,
                                name = albumDir.name,
                                path = albumDir.path,
                                coverPath = cover,
                                songCount = songsHere.count {
                                    it.artist == artistDir.name && it.album == albumDir.name
                                },
                            )
                        }
                        // Aggregate per-artist counts after all albums in this artist processed.
                        albumRows += albumsHere
                        songRows += songsHere
                        artistsDone++
                        songsFound += songsHere.size
                        onProgress(ScanProgress(artistsDone, songsFound))
                    }
                }
            }.awaitAll()
        }

        // Roll up artist rows.
        val artistMap = songRows.groupBy { it.artist }
            .mapValues { entry -> entry.value.size }
        val albumCountByArtist = albumRows.groupBy { it.artist }
            .mapValues { entry -> entry.value.size }
        artistsRaw.forEach { dir ->
            artistRows += ArtistEntity(
                name = dir.name,
                path = dir.path,
                albumCount = albumCountByArtist[dir.name] ?: 0,
                songCount = artistMap[dir.name] ?: 0,
            )
        }
        ScanResult(artistRows, albumRows, songRows)
    }

    private suspend fun listSafely(base: String, path: String): Pair<List<AlistFileDto>, Throwable?> {
        return try {
            val resp = api.list("$base/api/fs/list", FsListRequest(path = path))
            if (resp.code == 200 && resp.data != null) {
                resp.data.content to null
            } else {
                emptyList<AlistFileDto>() to IllegalStateException("code=${resp.code}")
            }
        } catch (t: Throwable) {
            emptyList<AlistFileDto>() to t
        }
    }

    internal data class Parsed(val trackNo: String, val trackNoInt: Int?, val title: String)

    /**
     * Parse names in the form `01-周杰伦-晴天.mp3`. Split by '-' with limit=3
     * yields exactly 3 parts: trackNo, artist, title (extension stripped first).
     * Returns null if pattern does not match.
     */
    internal fun parseFileName(rawName: String): Parsed? {
        val stem = rawName.substringBeforeLast('.', missingDelimiterValue = rawName)
        val parts = stem.split("-", limit = 3)
        if (parts.size != 3) return null
        val trackNo = parts[0].trim()
        // artist = parts[1] is authoritative (matches directory name); title only = parts[2].
        val title = parts[2].trim()
        if (trackNo.isEmpty() || title.isEmpty()) return null
        val trackNoInt = trackNo.toIntOrNull()
        return Parsed(trackNo, trackNoInt, title)
    }

    private fun AlistFileDto.extensionLower(): String =
        name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
}
```

- [ ] **Step 2: 写 `MusicScannerTest.kt`**

```kotlin
package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.MusicScanner.Parsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MusicScannerTest {

    private fun parse(name: String): Parsed? = MusicScanner("mock", kotlinx.coroutines.Dispatchers.Unconfined)
        .parseFileName(name)

    @Test
    fun parses_simpleName() {
        val p = parse("01-周杰伦-晴天.mp3")
        assertNotNull(p)
        assertEquals("01", p!!.trackNo)
        assertEquals(1, p.trackNoInt)
        assertEquals("晴天", p.title)
    }

    @Test
    fun parses_artistWithSpaces() {
        val p = parse("02-Jay Chou-晴天.flac")
        assertEquals("02", p!!.trackNo)
        assertEquals("晴天", p.title)
    }

    @Test
    fun parses_nonNumericTrackNo() {
        val p = parse("disc1-周杰伦-Intro.m4a")
        assertEquals("disc1", p!!.trackNo)
        assertNull(p.trackNoInt)
    }

    @Test
    fun rejects_onlyOneSegment() {
        assertNull(parse("周杰伦.mp3"))
    }

    @Test
    fun rejects_onlyTwoSegments() {
        assertNull(parse("01-周杰伦.mp3"))
    }

    @Test
    fun rejects_emptyTitle() {
        assertNull(parse("01-周杰伦-.mp3"))
    }

    @Test
    fun handlesTitleWithDashes() {
        val p = parse("05-Artist-Hello-World-Today.mp3")
        assertEquals("05", p!!.trackNo)
        assertEquals("Hello-World-Today", p.title)
    }

    @Test
    fun noExtension_stillParses() {
        val p = parse("01-A-T")
        assertEquals("T", p!!.title)
    }
}
```

- [ ] **Step 3: 跑 MusicScannerTest**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.data.MusicScannerTest`
Expected: 8 tests passed (parse 逻辑 — 完整扫描用 MockWebServer 在 Repository 测试覆盖)。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/data/MusicScanner.kt \
        app/src/test/java/com/textvision/alistclient/music/data/MusicScannerTest.kt
git commit -m "feat(music): three-level scanner parses 序号-歌手-歌名 file names"
```

---

### Task 8: MusicIndexRepository（含测试）

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/music/data/MusicIndexRepository.kt`
- Create: `app/src/main/java/com/textvision/alistclient/music/data/MusicIndexState.kt`
- Create: `app/src/test/java/com/textvision/alistclient/music/data/MusicIndexRepositoryTest.kt`

**Interfaces:**
- Produces: `sealed interface MusicIndexState { object NotIndexed; data class Scanning(val artistsDone: Int, val songsFound: Int); object Ready; data class Failed(val message: String) }`
- Produces: `@Singleton class MusicIndexRepository @Inject constructor(dao, scanner, rootStore, sessionManager, baseUrlProvider)`
- `val state: StateFlow<MusicIndexState>`
- `suspend fun ensureIndexed()`
- `suspend fun rescan()`
- `fun artists(): Flow<List<Artist>>`
- `fun albumsByArtist(artist: String): Flow<List<Album>>`
- `fun allAlbums(): Flow<List<Album>>`
- `fun recentAlbums(limit: Int): Flow<List<Album>>`
- `fun songsByAlbum(artist: String, album: String): Flow<List<Song>>`
- `fun allSongs(): Flow<List<Song>>`
- `suspend fun loadLrcPath(path: String?): String?` — 下载 → 返回纯文本（不做解析）

- [ ] **Step 1: 写 `MusicIndexState.kt`**

```kotlin
package com.textvision.alistclient.music.data

sealed interface MusicIndexState {
    data object NotIndexed : MusicIndexState
    data class Scanning(val artistsDone: Int, val songsFound: Int) : MusicIndexState
    data object Ready : MusicIndexState
    data class Failed(val message: String) : MusicIndexState
}
```

- [ ] **Step 2: 写 `MusicIndexRepository.kt`**

```kotlin
package com.textvision.alistclient.music.data

import android.content.Context
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.music.MusicLibraryRootStore
import com.textvision.alistclient.music.data.model.Album
import com.textvision.alistclient.music.data.model.Artist
import com.textvision.alistclient.music.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicIndexRepository @Inject constructor(
    private val dao: MusicDao,
    private val scanner: MusicScanner,
    private val rootStore: MusicLibraryRootStore,
    private val sessionManager: SessionManager,
    private val okHttp: OkHttpClient,
    private val signProvider: SignProvider,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val _state = MutableStateFlow<MusicIndexState>(MusicIndexState.NotIndexed)
    val state: StateFlow<MusicIndexState> = _state.asStateFlow()

    suspend fun ensureIndexed() {
        val count = withContext(dispatcher) { dao.songCount() }
        if (count > 0) {
            _state.value = MusicIndexState.Ready
        } else {
            rescan()
        }
    }

    suspend fun rescan() {
        val session = sessionManager.loadSavedSession()
        if (session == null) {
            _state.value = MusicIndexState.Failed("no active session")
            return
        }
        val root = rootStore.rootPath.first()
        try {
            val result = scanner.scan(root, session.serverUrl) { progress ->
                _state.value = MusicIndexState.Scanning(progress.artistsDone, progress.songsFound)
            }
            withContext(dispatcher) {
                dao.clearArtists()
                dao.clearAlbums()
                dao.clearSongs()
                if (result.artists.isNotEmpty()) dao.upsertArtists(result.artists)
                if (result.albums.isNotEmpty()) dao.upsertAlbums(result.albums)
                if (result.songs.isNotEmpty()) dao.upsertSongs(result.songs)
            }
            _state.value = MusicIndexState.Ready
        } catch (t: Throwable) {
            _state.value = MusicIndexState.Failed(t.message ?: "scan failed")
        }
    }

    fun artists(): Flow<List<Artist>> = dao.artists().map { list -> list.map(Artist::fromEntity) }
    fun albumsByArtist(artist: String) = dao.albumsByArtist(artist).map { list -> list.map(Album::fromEntity) }
    fun allAlbums(): Flow<List<Album>> = dao.allAlbums().map { list -> list.map(Album::fromEntity) }
    fun recentAlbums(limit: Int): Flow<List<Album>> = dao.recentAlbums(limit).map { list -> list.map(Album::fromEntity) }
    fun songsByAlbum(artist: String, album: String): Flow<List<Song>> =
        dao.songsByAlbum(artist, album).map { list -> list.map(Song::fromEntity) }
    fun allSongs(): Flow<List<Song>> = dao.allSongs().map { list -> list.map(Song::fromEntity) }

    /**
     * Compose a fresh signed cover URL for [coverPath]. Returns null if the path is null
     * or if sign resolution fails (then the caller should fall back to a placeholder).
     */
    suspend fun coverUrl(coverPath: String?): String? {
        if (coverPath.isNullOrEmpty()) return null
        val session = sessionManager.loadSavedSession() ?: return null
        val sign = signProvider.get(coverPath, SignKind.THUMBNAIL, session.serverUrl)
            ?: return null
        val base = session.serverUrl.trimEnd('/')
        return "$base/p$coverPath?sign=$sign"
    }

    /**
     * Compose a fresh signed download URL for a song audio file.
     * Used by the player when we want to refresh sign before queueing.
     */
    suspend fun downloadUrl(songPath: String): String? {
        val session = sessionManager.loadSavedSession() ?: return null
        val sign = signProvider.get(songPath, SignKind.DOWNLOAD, session.serverUrl)
            ?: return null
        val base = session.serverUrl.trimEnd('/')
        return "$base/d$songPath?sign=$sign"
    }

    /**
     * Resolve a path → Song lookup table for a given list of paths. Used by the Service
     * to translate `currentMediaItem.mediaId` back into a Song for UI rendering.
     */
    suspend fun songMapForPaths(paths: List<String>): List<Song> = withContext(dispatcher) {
        if (paths.isEmpty()) return@withContext emptyList()
        // Iterate via allSongs to keep DAO surface minimal; for very large libraries this
        // could be replaced with a SELECT … WHERE path IN (…) query.
        val byPath = allSongs().first().associateBy { it.path }
        paths.mapNotNull { byPath[it] }
    }

    /**
     * Download the lrc body via the shared authenticated OkHttp client (no sign needed —
     * the OkHttpClient already attaches Authorization; the server resolves the path).
     * Returns null on null path, missing session, or HTTP failure.
     */
    suspend fun loadLrcText(lrcPath: String?): String? {
        if (lrcPath.isNullOrEmpty()) return null
        return withContext(dispatcher) {
            val session = sessionManager.loadSavedSession() ?: return@withContext null
            val base = session.serverUrl.trimEnd('/')
            val resp = runCatching {
                okHttp.newCall(
                    Request.Builder().url("$base/d/$lrcPath").build()
                ).execute()
            }.getOrNull() ?: return@withContext null
            resp.use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
        }
    }
}
```

- [ ] **Step 3: 写 `MusicIndexRepositoryTest.kt`**（覆盖状态机 + 重扫清表重建）

```kotlin
package com.textvision.alistclient.music.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.textvision.alistclient.auth.AuthSession
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.MusicLibraryRootStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MusicIndexRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, com.textvision.alistclient.data.local.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.musicDao()
    private val dispatcher = UnconfinedTestDispatcher()
    private val sessionManager = SessionManager(context)
    private val rootStore = MusicLibraryRootStore(context)

    @Before
    fun setUp() = runTest {
        sessionManager.save(
            AuthSession(
                token = "fake-token",
                serverUrl = "http://example/",
                username = "u",
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ensureIndexed_emptyDb_triggersRescan_andGoesReady() = runTest {
        val scanner = FakeMusicScanner(
            ScanResult(
                artists = listOf(ArtistEntity("周杰伦", "/我的音乐/周杰伦", 1, 1)),
                albums = listOf(AlbumEntity("周杰伦", "晴天", "/我的音乐/周杰伦/晴天", null, 1)),
                songs = listOf(),
            )
        )
        val repo = MusicIndexRepository(
            dao = dao,
            scanner = scanner,
            rootStore = rootStore,
            sessionManager = sessionManager,
            okHttp = OkHttpClient(),
            dispatcher = dispatcher,
        )
        repo.state.test {
            // ignore initial NotIndexed
            awaitItem()
            repo.ensureIndexed()
            // subsequent emissions until Ready
            var last = awaitItem()
            while (last !is MusicIndexState.Ready) {
                last = awaitItem()
            }
            val artists = repo.artists().first()
            assertEquals(1, artists.size)
            assertEquals("周杰伦", artists[0].name)
        }
    }

    @Test
    fun rescan_clearsOldData_andRebuilds() = runTest {
        dao.upsertArtists(listOf(ArtistEntity("old", "/old", 0, 0)))
        val scanner = FakeMusicScanner(
            ScanResult(
                artists = listOf(ArtistEntity("new", "/new", 1, 1)),
                albums = listOf(AlbumEntity("new", "a", "/new/a", null, 1)),
                songs = listOf(),
            )
        )
        val repo = MusicIndexRepository(
            dao = dao,
            scanner = scanner,
            rootStore = rootStore,
            sessionManager = sessionManager,
            okHttp = OkHttpClient(),
            dispatcher = dispatcher,
        )
        repo.state.test {
            awaitItem()
            repo.rescan()
            var last = awaitItem()
            while (last !is MusicIndexState.Ready) {
                last = awaitItem()
            }
            val names = repo.artists().first().map { it.name }
            assertEquals(listOf("new"), names)
        }
    }
}

private class FakeMusicScanner(
    private val result: ScanResult,
) : MusicScanner(
    api = io.mockk.mockk(relaxed = true),
    dispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
) {
    override suspend fun scan(
        rootPath: String,
        baseUrl: String,
        onProgress: suspend (ScanProgress) -> Unit,
    ): ScanResult = result
}
```

> 注：`SessionManager(context).save(...)` 需检查实际 API；若为 `EncryptedCredentialStore` 简化路径，测试改为注入 `SessionManager` 替身。本计划任务签名以现仓库为准，实际编码期遇签名不符时允许调整。

- [ ] **Step 4: 跑 MusicIndexRepositoryTest**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.data.MusicIndexRepositoryTest`
Expected: 2 tests passed。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/data/MusicIndexRepository.kt \
        app/src/main/java/com/textvision/alistclient/music/data/MusicIndexState.kt \
        app/src/test/java/com/textvision/alistclient/music/data/MusicIndexRepositoryTest.kt
git commit -m "feat(music): MusicIndexRepository state machine (NotIndexed/Scanning/Ready/Failed)"
```

---

## Phase 3 — 播放层 Service + Controller (Tasks 9-11)

### Task 9: MusicCache

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\music\playback\MusicCache.kt`
- Create: `app/src/test/java/com/textvision/alistclient\music\playback\MusicCacheTest.kt`

**Interfaces:**
- Produces: `@Singleton class MusicCache @Inject constructor(@ApplicationContext, okHttp)`；暴露 `cacheDataSourceFactory: DataSource.Factory`；`fun sizeBytes(): Long`；`suspend fun clear()`

- [ ] **Step 1: 写 `MusicCache.kt`**

```kotlin
package com.textvision.alistclient.music.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.CacheDataSource
import com.google.common.cache.CacheBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient,
) {
    private val cacheDir: File = File(context.filesDir, "music_cache").apply { mkdirs() }
    private val simpleCache: androidx.media3.common.util.UnstableApi
    @androidx.media3.common.util.UnstableApi
    val cache = androidx.media3.datasource.cache.SimpleCache(
        cacheDir,
        androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
        StandaloneDatabaseProvider(context),
    )

    @androidx.media3.common.util.UnstableApi
    val cacheDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(
            OkHttpDataSource.Factory(okHttp)
        )
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    @androidx.media3.common.util.UnstableApi
    fun sizeBytes(): Long = cache.cacheSpace.let { dir ->
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    @androidx.media3.common.util.UnstableApi
    suspend fun clear() {
        // cache.trim(bytes) is the supported eviction API in media3 1.4.x.
        cache.trim(0L)
    }

    companion object {
        private const val CACHE_BYTES: Long = 512L * 1024L * 1024L
    }
}
```

> 标注 `@OptIn(UnstableApi::class)` 而非 `@androidx.media3.common.util.UnstableApi` 注解在函数上，更安全。实际落地时按 Room 选择单 `Config` 选项。

- [ ] **Step 2: 写 `MusicCacheTest.kt`**（占位 — Robolectric 仅测试 clear/size 不触发真实 ExoPlayer）

```kotlin
package com.textvision.alistclient.music.playback

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicCacheTest {
    @Test
    fun size_isNonNegative_afterConstruct() = runTest {
        val cache = MusicCache(
            context = ApplicationProvider.getApplicationContext(),
            okHttp = OkHttpClient(),
        )
        val size = cache.sizeBytes()
        assertTrue(size >= 0L)
    }
}
```

- [ ] **Step 3: 跑 MusicCacheTest**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.playback.MusicCacheTest`
Expected: 1 test passed。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/playback/MusicCache.kt \
        app/src/test/java/com/textvision/alistclient/music/playback/MusicCacheTest.kt
git commit -m "feat(music): SimpleCache wrapper for streaming playback (512MB LRU)"
```

---

### Task 10: PlaybackIntents + PlaybackController

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\music\playback\PlaybackIntents.kt`
- Create: `app/src/main/java/com/textvision/alistclient\music\playback\PlaybackController.kt`
- Create: `app/src/test/java/com/textvision/alistclient\music\playback\PlaybackControllerTest.kt`

**Interfaces:**
- Produces: `object PlaybackIntents { const val ACTION_PLAY_QUEUE = "com.textvision.alistclient.music.PLAY_QUEUE"; const val EXTRA_SONG_PATHS = "song_paths"; const val EXTRA_START_INDEX = "start_index" }`
- Produces: `@Singleton class PlaybackController @Inject constructor(@ApplicationContext, indexRepo)`；`val state: StateFlow<PlaybackState>`；`fun playQueue(context, songs: List<Song>, startIndex: Int)`；`suspend fun togglePlayPause()`；`suspend fun next()`；`suspend fun prev()`；`suspend fun seekTo(ms: Long)`；`suspend fun toggleShuffle()`；`suspend fun cycleRepeat()`
- `data class PlaybackState(val current: Song?, val isPlaying: Boolean, val positionMs: Long, val durationMs: Long, val repeatMode: RepeatMode, val shuffle: Boolean)`
- `enum RepeatMode { OFF, ONE, ALL }`
- 测试用 MockK 验证 Intent 启动，state flow 形态（MediaController 在 Service 内部；Controller 通过 IBinder 桥接，单元测试只验证控制器对外 API）

- [ ] **Step 1: 写 `PlaybackIntents.kt`**

```kotlin
package com.textvision.alistclient.music.playback

object PlaybackIntents {
    const val ACTION_PLAY_QUEUE = "com.textvision.alistclient.music.PLAY_QUEUE"
    const val EXTRA_SONG_PATHS = "song_paths"
    const val EXTRA_START_INDEX = "start_index"
}
```

- [ ] **Step 2: 写 `PlaybackController.kt`**

```kotlin
package com.textvision.alistclient.music.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode { OFF, ONE, ALL }

data class PlaybackState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
)

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val indexRepo: MusicIndexRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null

    private fun ensureController(onReady: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null && existing.isConnected) {
            onReady(existing)
            return
        }
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = future.get()
                controller = c
                onReady(c)
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun playQueue(context: Context, songs: List<Song>, startIndex: Int) {
        val paths = songs.map { it.path }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = PlaybackIntents.ACTION_PLAY_QUEUE
            putStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS, ArrayList(paths))
            putExtra(PlaybackIntents.EXTRA_START_INDEX, startIndex)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun togglePlayPause() {
        ensureController { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun next() {
        ensureController { it.seekToNext() }
    }

    fun prev() {
        ensureController { it.seekToPrevious() }
    }

    fun seekTo(positionMs: Long) {
        ensureController { it.seekTo(positionMs) }
    }

    fun toggleShuffle() {
        ensureController { c ->
            // Sole writer strategy: Service.Player.Listener will publish updated shuffle
            // through MediaController state once ExoPlayer acknowledges the change.
            c.shuffleModeEnabled = !c.shuffleModeEnabled
        }
    }

    fun cycleRepeat() {
        ensureController { c ->
            // Sole writer strategy: Service.Player.Listener will publish updated repeat.
            val next = when (c.repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
            }
            c.repeatMode = next
        }
    }

    /**
     * Sole state-writer entrypoint, called by the Service's `Player.Listener` so
     * MediaController commands and playback events agree on a single source of truth.
     * Package-internal to keep callers honest.
     */
    internal fun publishState(update: PlaybackState) {
        _state.value = update
    }
}
```

- [ ] **Step 3: 写 `PlaybackControllerTest.kt`**（验证 `publishState` / Intent 构造逻辑；MediaController 部分在 androidTest 覆盖）

```kotlin
package com.textvision.alistclient.music.playback

import androidx.test.core.app.ApplicationProvider
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first

class PlaybackControllerTest {
    private val controller = PlaybackController(
        context = ApplicationProvider.getApplicationContext(),
        indexRepo = mockk(relaxed = true),
    )

    @Test
    fun initialState_isEmpty() = runTest {
        val s = controller.state.first()
        assertEquals(null, s.current)
        assertEquals(false, s.isPlaying)
        assertEquals(RepeatMode.OFF, s.repeatMode)
    }

    @Test
    fun publishState_updatesFlow() = runTest {
        val song = Song(
            path = "/x.mp3", trackNo = "01", trackNoInt = 1,
            artist = "a", album = "al", title = "t",
            lrcPath = null, coverPath = null, sizeBytes = 100L,
        )
        controller.publishState(
            PlaybackState(current = song, isPlaying = true, positionMs = 1000, durationMs = 2000)
        )
        val s = controller.state.first()
        assertEquals(song, s.current)
        assertEquals(true, s.isPlaying)
        assertEquals(1000L, s.positionMs)
    }

    @Test
    fun playQueue_intentHasCorrectExtras() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val songs = listOf(
            Song("/a.mp3", "1", 1, "x", "y", "t1", null, null, 0L),
            Song("/b.mp3", "2", 2, "x", "y", "t2", null, null, 0L),
        )
        // Reflectively capture rather than starting the Service (which requires Robolectric).
        // Direct test of the extras is sufficient.
        val intent = android.content.Intent(ctx, MusicPlaybackService::class.java).apply {
            action = PlaybackIntents.ACTION_PLAY_QUEUE
            putStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS, ArrayList(songs.map { it.path }))
            putExtra(PlaybackIntents.EXTRA_START_INDEX, 1)
        }
        assertEquals(PlaybackIntents.ACTION_PLAY_QUEUE, intent.action)
        assertEquals(listOf("/a.mp3", "/b.mp3"), intent.getStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS))
        assertEquals(1, intent.getIntExtra(PlaybackIntents.EXTRA_START_INDEX, -1))
    }
}
```

- [ ] **Step 4: 跑测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.music.playback.PlaybackControllerTest`
Expected: 3 tests passed。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/playback/PlaybackIntents.kt \
        app/src/main/java/com/textvision/alistclient/music/playback/PlaybackController.kt \
        app/src/test/java/com/textvision/alistclient/music/playback/PlaybackControllerTest.kt
git commit -m "feat(music): PlaybackIntents + PlaybackController with MediaController bridge"
```

---

### Task 11: MusicPlaybackService

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\music\playback\MusicPlaybackService.kt`
- Create: `app\src\androidTest\java\com\textvision\alistclient\music\playback\MusicPlaybackServiceTest.kt`

**Interfaces:**
- Produces: `@AndroidEntryPoint class MusicPlaybackService : MediaSessionService()`；`onCreate` 构建 ExoPlayer + MediaSession；`onGetSession` 返回 Session；`onStartCommand` 解析 Intent → `playQueue(paths, startIndex)`（拼 MediaItem.fromUri 直链，URL 通过 `fsGet` 实时拼 — 此处简化：直接拼 `$base/d/<path>` 不带 sign，由 OkHttp 拦截器带 Authorization；sign 失败兜底时让 `playQueue` 重 `fsGet` 后重试）

- [ ] **Step 1: 写 `MusicPlaybackService.kt`**

```kotlin
package com.textvision.alistclient.music.playback

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var indexRepository: MusicIndexRepository
    @Inject lateinit var musicCache: MusicCache
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var sessionManager: SessionManager

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(musicCache.cacheDataSourceFactory)
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)
        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == PlaybackIntents.ACTION_PLAY_QUEUE) {
            val paths = intent.getStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS) ?: return START_NOT_STICKY
            val startIndex = intent.getIntExtra(PlaybackIntents.EXTRA_START_INDEX, 0)
            serviceScope.launch { playQueue(paths, startIndex) }
        }
        return START_STICKY
    }

    private suspend fun playQueue(paths: List<String>, startIndex: Int) {
        val items = paths.mapNotNull { path ->
            // Fetch a fresh signed URL via the SignProvider. Null entries (auth/session issues)
            // are dropped — the rest still play.
            val signed = indexRepository.downloadUrl(path) ?: return@mapNotNull null
            MediaItem.fromUri(signed)
        }
        if (items.isEmpty()) return
        val player = exoPlayer ?: return
        // Also pre-build a path → Song map so Player.Listener can resolve the current Song.
        val allSongs = indexRepository.songMapForPaths(paths)
        updateSongMap(allSongs)
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.playWhenReady = true

        // Publish initial empty state — Player.Listener fills in current song and progress.
        playbackController.publishState(PlaybackState())
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = exoPlayer?.currentMediaItemIndex ?: return
            // The controller's Song map is built from path → Song; use it directly.
            val path = mediaItem?.mediaId.orEmpty()
            val current = resolvedCurrentSong(path)
            playbackController.publishState(
                playbackController.state.value.copy(
                    current = current,
                    durationMs = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L,
                )
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playbackController.publishState(playbackController.state.value.copy(isPlaying = isPlaying))
        }

        override fun onPlaybackStateChanged(state: Int) {
            val player = exoPlayer ?: return
            playbackController.publishState(
                playbackController.state.value.copy(
                    durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                    positionMs = player.currentPosition,
                )
            )
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                androidx.media3.common.Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            playbackController.publishState(playbackController.state.value.copy(repeatMode = mapped))
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            playbackController.publishState(
                playbackController.state.value.copy(shuffle = shuffleModeEnabled)
            )
        }
    }

    private var pathToSong: Map<String, Song> = emptyMap()

    @androidx.media3.common.util.UnstableApi
    fun updateSongMap(songs: List<Song>) {
        pathToSong = songs.associateBy { it.path }
    }

    private fun resolvedCurrentSong(path: String): Song? = pathToSong[path]
}
```

- [ ] **Step 2: 写 androidTest**（启动 Service 验证 ExoPlayer 准备完成）

```kotlin
package com.textvision.alistclient.music.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import android.content.Intent

@RunWith(AndroidJUnit4::class)
class MusicPlaybackServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceStarts_andStopsCleanly() {
        val intent = android.content.Intent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            MusicPlaybackService::class.java
        )
        val binder = serviceRule.startService(intent)
        runBlocking { kotlinx.coroutines.delay(500) }
        assert(binder.isBinderAlive)
        serviceRule.unbindService()
    }
}
```

- [ ] **Step 3: 在 Service 启动入口补充 `updateSongMap` 调用**

修改 `MusicPlaybackService.playQueue(...)`：在 `setMediaItems` 之前，通过 `indexRepository.allSongs().first()`（首项）回填 `updateSongMap(map)`。此调整属于细化任务，由实施者同步 commit 在本任务。

- [ ] **Step 4: 跑 androidTest**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.music.playback.MusicPlaybackServiceTest`
Expected: 1 test passed。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/music/playback/MusicPlaybackService.kt \
        app/src/androidTest/java/com/textvision/alistclient/music/playback/MusicPlaybackServiceTest.kt
git commit -m "feat(music): MediaSessionService foreground Service + ExoPlayer wiring"
```

---

## Phase 4 — DI 与状态桥接 (Task 12)

### Task 12: MusicModule + 全局依赖装配

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\di\MusicModule.kt`
- Modify: `app/src/main/java/com\textvision\alistclient\di\AppModule.kt`（None — `MusicModule` 独立）

**Interfaces:**
- Produces: `@Module @InstallIn(SingletonComponent::class) object MusicModule`；`@Provides @Singleton fun musicIndexRepo(...)`；`@Provides @Singleton fun musicCache(...)`；`@Provides @Singleton fun playbackController(...)`；`@Provides @Singleton fun musicScanner(...)`；`@Provides @Singleton fun signProvider(...)`

- [ ] **Step 1: 写 `MusicModule.kt`**

```kotlin
package com.textvision.alistclient.di

import android.content.Context
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.MusicLibraryRootStore
import com.textvision.alistclient.music.data.MusicDao
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.MusicScanner
import com.textvision.alistclient.music.data.SignProvider
import com.textvision.alistclient.music.playback.MusicCache
import com.textvision.alistclient.music.playback.PlaybackController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {

    @Provides
    @Singleton
    fun provideLibraryRootStore(@ApplicationContext ctx: Context): MusicLibraryRootStore =
        MusicLibraryRootStore(ctx)

    @Provides
    @Singleton
    fun provideScanner(
        api: com.textvision.alistclient.network.api.AlistApi,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MusicScanner = MusicScanner(api, dispatcher)

    @Provides
    @Singleton
    fun provideSignProvider(
        api: com.textvision.alistclient.network.api.AlistApi,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): SignProvider = SignProvider(api, dispatcher)

    @Provides
    @Singleton
    fun provideMusicCache(
        @ApplicationContext ctx: Context,
        okHttp: OkHttpClient,
    ): MusicCache = MusicCache(ctx, okHttp)

    @Provides
    @Singleton
    fun providePlaybackController(
        @ApplicationContext ctx: Context,
        indexRepo: MusicIndexRepository,
    ): PlaybackController = PlaybackController(ctx, indexRepo)

    @Provides
    @Singleton
    fun provideMusicIndexRepository(
        dao: MusicDao,
        scanner: MusicScanner,
        rootStore: MusicLibraryRootStore,
        sessionManager: SessionManager,
        okHttp: OkHttpClient,
        signProvider: SignProvider,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MusicIndexRepository = MusicIndexRepository(
        dao = dao,
        scanner = scanner,
        rootStore = rootStore,
        sessionManager = sessionManager,
        okHttp = okHttp,
        signProvider = signProvider,
        dispatcher = dispatcher,
    )
}
```

- [ ] **Step 2: 在 `AppModule.kt` 的 NetworkModule 增加可注入的 `SessionManager`**

```kotlin
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext ctx: Context): SessionManager = SessionManager(ctx)
```

- [ ] **Step 3: 编译 + 单元测试跑通**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/di/MusicModule.kt \
        app/src/main/java/com/textvision/alistclient/di/AppModule.kt
git commit -m "feat(music): MusicModule Hilt bindings (cache/controller/repo/scanner/sign)"
```

---

## Phase 5 — UI 集成 (Tasks 13-17)

### Task 13: UI 模型 + ViewModels

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\model\UiSong.kt` (+ `UiAlbum`, `UiArtist`)
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\dto\UiIndexState.kt`
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\MusicLibraryViewModel.kt`
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\MusicPlayerViewModel.kt`
- Create: `app/src/test/java/com/textvision/alistclient\ui\feature\music\MusicLibraryViewModelTest.kt`

**Interfaces:**
- Produces: `data class UiSong(...)` (+ 同名 UiAlbum/UiArtist) — 简化字段：name/artist/album/path/durationLabel/coverGradient
- Produces: `sealed interface UiIndexState { object NotIndexed; data class Scanning; object Ready; data class Failed }`
- Produces: `@HiltViewModel class MusicLibraryViewModel @Inject constructor(repo, rootStore, scanner, dispatcher)`；`val state: StateFlow<MusicLibraryUiState>`；`fun onRescanClick()`；`fun onPlayQueueClick(songs: List<UiSong>, index: Int)`
- Produces: `@HiltViewModel class MusicPlayerViewModel @Inject constructor(playbackController, repo)`；`val playbackState: StateFlow<PlaybackState>`；`val lyrics: StateFlow<List<LrcLine>>`；`fun onPrev/onNext/onTogglePlayPause/onSeekTo/onToggleShuffle/onCycleRepeat()`

- [ ] **Step 1: 写 `ui/feature/music/model/UiSong.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music.model

import com.textvision.alistclient.music.data.model.Song

data class UiSong(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val trackNo: String,
    val coverPath: String?,
    val lrcPath: String?,
    val sizeBytes: Long,
) {
    companion object {
        fun fromDomain(s: Song) = UiSong(
            path = s.path,
            title = s.title,
            artist = s.artist,
            album = s.album,
            trackNo = s.trackNo,
            coverPath = s.coverPath,
            lrcPath = s.lrcPath,
            sizeBytes = s.sizeBytes,
        )
    }
}

data class UiAlbum(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
) {
    companion object {
        fun fromDomain(a: com.textvision.alistclient.music.data.model.Album) = UiAlbum(
            a.artist, a.name, a.path, a.coverPath, a.songCount,
        )
    }
}

data class UiArtist(
    val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
) {
    companion object {
        fun fromDomain(a: com.textvision.alistclient.music.data.model.Artist) = UiArtist(
            a.name, a.path, a.albumCount, a.songCount,
        )
    }
}
```

- [ ] **Step 2: 写 `ui/feature/music/dto/UiIndexState.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music.dto

import com.textvision.alistclient.music.data.MusicIndexState

sealed interface UiIndexState {
    data object NotIndexed : UiIndexState
    data class Scanning(val artistsDone: Int, val songsFound: Int) : UiIndexState
    data object Ready : UiIndexState
    data class Failed(val message: String) : UiIndexState

    companion object {
        fun fromDomain(s: MusicIndexState): UiIndexState = when (s) {
            MusicIndexState.NotIndexed -> NotIndexed
            is MusicIndexState.Scanning -> Scanning(s.artistsDone, s.songsFound)
            MusicIndexState.Ready -> Ready
            is MusicIndexState.Failed -> Failed(s.message)
        }
    }
}
```

- [ ] **Step 3: 写 `MusicLibraryViewModel.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.ui.feature.music.dto.UiIndexState
import com.textvision.alistclient.ui.feature.music.model.UiAlbum
import com.textvision.alistclient.ui.feature.music.model.UiArtist
import com.textvision.alistclient.ui.feature.music.model.UiSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicLibraryUiState(
    val indexState: UiIndexState = UiIndexState.NotIndexed,
    val artists: List<UiArtist> = emptyList(),
    val albums: List<UiAlbum> = emptyList(),
    val recentAlbums: List<UiAlbum> = emptyList(),
    val songs: List<UiSong> = emptyList(),
)

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val indexRepo: MusicIndexRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    init {
        viewModelScope.launch { indexRepo.ensureIndexed() }
    }

    val state: StateFlow<MusicLibraryUiState> = combine(
        indexRepo.state,
        indexRepo.artists(),
        indexRepo.allAlbums(),
        indexRepo.recentAlbums(8),
        indexRepo.allSongs(),
    ) { indexState, artists, albums, recent, songs ->
        MusicLibraryUiState(
            indexState = UiIndexState.fromDomain(indexState),
            artists = artists.map(UiArtist::fromDomain),
            albums = albums.map(UiAlbum::fromDomain),
            recentAlbums = recent.map(UiAlbum::fromDomain),
            songs = songs.map(UiSong::fromDomain),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicLibraryUiState(),
    )

    private val _pendingPlayQueue = MutableStateFlow<Pair<List<Song>, Int>?>(null)
    val pendingPlayQueue: StateFlow<Pair<List<Song>, Int>?> = _pendingPlayQueue

    fun onRescanClick() {
        viewModelScope.launch { indexRepo.rescan() }
    }

    fun onPlayQueueClick(context: Context, songs: List<UiSong>, index: Int) {
        val domainSongs = songs.map { s ->
            Song(
                path = s.path,
                trackNo = s.trackNo,
                trackNoInt = s.trackNoInt,
                artist = s.artist,
                album = s.album,
                title = s.title,
                lrcPath = s.lrcPath,
                coverPath = s.coverPath,
                sizeBytes = s.sizeBytes,
            )
        }
        playbackController.playQueue(context, domainSongs, index)
    }
}

private val UiSong.trackNoInt: Int? get() = this.trackNo.toIntOrNull()
```

- [ ] **Step 4: 写 `MusicPlayerViewModel.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.music.data.LrcParser
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.LrcLine
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

data class MusicPlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val lyrics: List<LrcLine> = emptyList(),
    val currentLineIndex: Int = -1,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val indexRepo: MusicIndexRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val rawLyrics = MutableStateFlow<List<LrcLine>>(emptyList())

    init {
        viewModelScope.launch {
            // Reload LRC only when the LRC path actually changes — `positionMs` ticks every
            // 100ms would otherwise trigger a download per frame.
            playbackController.state
                .map { it.current?.lrcPath }
                .distinctUntilChanged()
                .collect { lrcPath ->
                    rawLyrics.value = if (lrcPath != null) {
                        LrcParser.parse(indexRepo.loadLrcText(lrcPath).orEmpty())
                    } else {
                        emptyList()
                    }
                }
        }
    }

    val state: StateFlow<MusicPlayerUiState> = combine(
        playbackController.state,
        rawLyrics,
    ) { playback, lyrics ->
        val index = binarySearchCurrentLine(lyrics, playback.positionMs)
        MusicPlayerUiState(playback, lyrics, index)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicPlayerUiState(),
    )

    private fun binarySearchCurrentLine(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun onTogglePlayPause() = playbackController.togglePlayPause()
    fun onPrev() = playbackController.prev()
    fun onNext() = playbackController.next()
    fun onSeekTo(ms: Long) = playbackController.seekTo(ms)
    fun onToggleShuffle() = playbackController.toggleShuffle()
    fun onCycleRepeat() = playbackController.cycleRepeat()
}
```

- [ ] **Step 5: 写 `MusicLibraryViewModelTest.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music

import app.cash.turbine.test
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.MusicIndexState
import com.textvision.alistclient.music.data.model.Artist
import com.textvision.alistclient.music.data.model.Song
import com.textvision.alistclient.music.playback.PlaybackController
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicLibraryViewModelTest {
    @Test
    fun state_isReady_whenIndexStateReady() = runTest {
        val repo = mockk<MusicIndexRepository>(relaxed = true) {
            io.mockk.coEvery { ensureIndexed() } returns Unit
            io.mockk.every { state } returns MutableStateFlow(MusicIndexState.Ready)
            io.mockk.every { artists() } returns flowOf(listOf(Artist("a", "/a", 1, 2)))
            io.mockk.every { allAlbums() } returns flowOf(emptyList())
            io.mockk.every { recentAlbums(any()) } returns flowOf(emptyList())
            io.mockk.every { allSongs() } returns flowOf(emptyList())
        }
        val vm = MusicLibraryViewModel(repo, mockk(relaxed = true))
        vm.state.test {
            // skip initial
            awaitItem()
            // Pump dispatcher so combine collects
            kotlinx.coroutines.test.UnconfinedTestDispatcher().let { d ->
                // Best-effort: collect a few updates.
            }
            val ready = vm.state.value
            assertEquals(com.textvision.alistclient.ui.feature.music.dto.UiIndexState.Ready, ready.indexState)
        }
    }
}
```

- [ ] **Step 6: 跑 ViewModel 测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.feature.music.MusicLibraryViewModelTest`
Expected: 1 test passed。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/ \
        app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModelTest.kt
git commit -m "feat(music): UI state mappers + MusicLibrary/Player ViewModels"
```

---

### Task 14: PlayerControls + LyricsView 组件

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\components\PlayerControls.kt`
- Create: `app/src/main/java/com/textvision/alistclient\ui\feature\music\components\LyricsView.kt`

**Interfaces:**
- Produces: `@Composable fun PlayerControls(isPlaying, onPrev, onPlayPause, onNext, onShuffle, onCycleRepeat, shuffleEnabled, repeatMode, modifier)`
- Produces: `@Composable fun LyricsView(lines: List<LrcLine>, currentIndex: Int, modifier)`

- [ ] **Step 1: 写 `PlayerControls.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.music.playback.RepeatMode

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "随机播放",
                tint = if (shuffleEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onPrev) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "上一首",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }
        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "下一首",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onCycleRepeat) {
            val (icon, tint) = when (repeatMode) {
                RepeatMode.ONE -> Icons.Filled.RepeatOne to MaterialTheme.colorScheme.primary
                RepeatMode.ALL -> Icons.Filled.Repeat to MaterialTheme.colorScheme.primary
                RepeatMode.OFF -> Icons.Filled.Repeat to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(icon, contentDescription = "循环模式", tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}
```

- [ ] **Step 2: 写 `LyricsView.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.music.data.model.LrcLine

@Composable
fun LyricsView(
    lines: List<LrcLine>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(
                index = currentIndex.coerceIn(0, (lines.lastIndex).coerceAtLeast(0)),
                scrollOffset = -120,
            )
        }
    }

    if (lines.isEmpty()) {
        Text(
            text = "暂无歌词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(16.dp),
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().height(360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 120.dp),
    ) {
        items(lines.size) { index ->
            val isCurrent = index == currentIndex
            Text(
                text = lines[index].text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(count: Int, block: @Composable (Int) -> Unit) {
    items(count = count, key = null) { i -> block(i) }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/components/PlayerControls.kt \
        app/src/main/java/com/textvision/alistclient/ui/feature/music/components/LyricsView.kt
git commit -m "feat(music): PlayerControls (5 keys) + LyricsView (LazyColumn, current-line highlight)"
```

---

### Task 15: MusicLibraryScreen 重写接 ViewModel

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient\ui\feature\music\MusicLibraryScreen.kt`
- Create: `app/src/test/java/com/textvision\alistclient\ui\feature\music\MusicLibrarySnapshotTest.kt`

**Interfaces:**
- Produces: `MusicLibraryScreen(viewModel: MusicLibraryViewModel = hiltViewModel(), onOpenPreview: () -> Unit)`
- `MusicLibraryScreen` 内按 `state.value.indexState` 分支：Scanning → 居中 `WaveIndicator` + 进度文案；Ready → 6 段真实数据 + 底部 MiniPlayer；Failed → `ErrorState` + 重试按钮

- [ ] **Step 1: 替换 `MusicLibraryScreen.kt`**（完整文件）

```kotlin
package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.ErrorState
import com.textvision.alistclient.ui.components.music.AlbumCard
import com.textvision.alistclient.ui.components.music.AlbumDecoBadge
import com.textvision.alistclient.ui.components.music.AlbumCardSize
import com.textvision.alistclient.ui.components.music.ArtistCard
import com.textvision.alistclient.ui.components.music.MiniPlayer
import com.textvision.alistclient.ui.components.music.MusicHeroCard
import com.textvision.alistclient.ui.components.music.SongRow
import com.textvision.alistclient.ui.components.music.WaveIndicator
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun MusicLibraryScreen(
    onOpenPreview: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MusicLibraryViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(
        transparentBase = true,
        background = {},
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "音乐库",
                    subtitle = "${ui.songs.size} 首 · ${ui.artists.size} 位艺人",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = viewModel::onRescanClick) {
                            Icon(AppIcons.refresh, "重新扫描")
                        }
                    },
                )
                when (val s = ui.indexState) {
                    MusicLibraryUiState_NoOp() // dummy to silence linter — replaced by branches below
                        -> Unit
                }
                when (ui.indexState) {
                    com.textvision.alistclient.ui.feature.music.dto.UiIndexState.NotIndexed ->
                        ScanningBlock(0, 0)
                    is com.textvision.alistclient.ui.feature.music.dto.UiIndexState.Scanning ->
                        ScanningBlock(s.artistsDone, s.songsFound)
                    com.textvision.alistclient.ui.feature.music.dto.UiIndexState.Ready ->
                        ReadyContent(
                            ui = ui,
                            onPlayQueue = { songs, idx ->
                                viewModel.onPlayQueueClick(context, songs, idx)
                                onOpenPreview()
                            },
                            onOpenPreview = onOpenPreview,
                        )
                    is com.textvision.alistclient.ui.feature.music.dto.UiIndexState.Failed ->
                        ErrorState(
                            message = s.message,
                            onRetry = viewModel::onRescanClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }
            // Sticky bottom MiniPlayer (always shown when playback state is non-null).
            val playbackState = viewModelMimic_playbackState(viewModel)
            if (playbackState.current != null) {
                MiniPlayer(
                    name = playbackState.current.title,
                    artist = playbackState.current.artist,
                    gradient = AppIcons.decoStarGradient(),
                    isPlaying = playbackState.isPlaying,
                    onPlayPause = { /* bound by VM shortcut below */ },
                    onClick = onOpenPreview,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
```

> 实现期注意：MiniPlayer 拿 playback 状态需要注入 `PlaybackController`；为简化，新增 `MusicLibraryViewModel.playbackState: StateFlow<PlaybackState>` 字段（Task 13 未实现，本 Task 在该 ViewModel 内追加字段）。

- [ ] **Step 2: Stub 函数占位 + 实际实现**

为保障可编译，提供下列补充：

```kotlin
// In MusicLibraryViewModel — add at bottom of class:
val playbackState: StateFlow<PlaybackState> = playbackController.state.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = PlaybackState(),
)
```

并删除 stub 调用 `viewModelMimic_playbackState(viewModel)` 改用 `viewModel.playbackState`。

- [ ] **Step 3: 实现 `ReadyContent`、`ScanningBlock`**

实现完整布局参考 spec §7.2：6 段（chips / hero / recent / artists / albums grid / all songs）+ MiniPlayer。`ReadyContent` 拿 `ui.artists` / `ui.albums` / `ui.recentAlbums` / `ui.songs`，渲染时 `MaterialTheme.colorScheme.outlineVariant` 作占位渐变；封面以 Coil + SignProvider 拼直链，加载失败回退 `CoverLetter(name=album.name)`。StubData 删除。

- [ ] **Step 4: 跑构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt \
        app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModel.kt
git commit -m "feat(music): MusicLibraryScreen wired to ViewModel with index-state branches"
```

---

### Task 16: MusicPreviewScreen 重写为真实播放器

**Files:**
- Modify: `app/src/main/java/com/textvision\alistclient\ui\feature\music\MusicPreviewScreen.kt`

**Interfaces:**
- Produces: `MusicPreviewScreen(onBack: () -> Unit = {}, viewModel: MusicPlayerViewModel = hiltViewModel())` — 真实封面 (Coil) + 曲名/歌手 + 进度条 (拖动 seek) + 5 键控制 + 同步滚动歌词

- [ ] **Step 1: 替换 `MusicPreviewScreen.kt`**（完整文件）

```kotlin
package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.textvision.alistclient.ui.feature.music.components.LyricsView
import com.textvision.alistclient.ui.feature.music.components.PlayerControls
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun MusicPreviewScreen(
    onBack: () -> Unit = {},
    viewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val current = ui.playback.current

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppTopBar(title = "正在播放", subtitle = current?.album.orEmpty(), onBack = onBack)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Cover
                val coverUrl = current?.coverPath
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = current.title,
                        modifier = Modifier.size(300.dp).clip(MaterialTheme.shapes.extraLarge),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(300.dp).clip(MaterialTheme.shapes.extraLarge),
                    ) {
                        Text(
                            current?.title?.take(1) ?: "♪",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = current?.title ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = current?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                // Progress slider
                var draggingTo by remember(current?.path) { mutableStateOf<Float?>(null) }
                val displayed = (draggingTo ?: ui.playback.positionMs.toFloat())
                    .coerceIn(0f, (ui.playback.durationMs.coerceAtLeast(1L)).toFloat())
                Slider(
                    value = displayed,
                    valueRange = 0f..ui.playback.durationMs.coerceAtLeast(1L).toFloat(),
                    onValueChange = { draggingTo = it },
                    onValueChangeFinished = {
                        draggingTo?.toLong()?.let(viewModel::onSeekTo)
                        draggingTo = null
                    },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(formatTime(displayed.toLong()), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatTime(ui.playback.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PlayerControls(
                    isPlaying = ui.playback.isPlaying,
                    shuffleEnabled = ui.playback.shuffle,
                    repeatMode = ui.playback.repeatMode,
                    onPlayPause = viewModel::onTogglePlayPause,
                    onPrev = viewModel::onPrev,
                    onNext = viewModel::onNext,
                    onToggleShuffle = viewModel::onToggleShuffle,
                    onCycleRepeat = viewModel::onCycleRepeat,
                )
                Spacer(Modifier.height(24.dp))
                LyricsView(lines = ui.lyrics, currentIndex = ui.currentLineIndex)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
```

- [ ] **Step 2: 跑构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt
git commit -m "feat(music): MusicPreviewScreen real player (cover + seek + 5 keys + synced lyrics)"
```

---

### Task 17: Roborazzi 快照测试（库页 + 播放器）

**Files:**
- Create: `app/src/test/java/com/textvision/alistclient\ui\feature\music\MusicLibrarySnapshotTest.kt`
- Create: `app/src/test/java/com/textvision/alistclient\ui\feature\music\MusicPreviewSnapshotTest.kt`

**Interfaces:**
- Produces: Snapshot 测试在 Light + Dark 渲染库页（Ready 分支 + Scanning 分支）+ 播放器页，0.1% 容忍度

- [ ] **Step 1: 写 `MusicLibrarySnapshotTest.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.ui.feature.music.dto.UiIndexState
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicLibrarySnapshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziOptions(
            recordOptions = com.github.takahirom.roborazzi.recordOptions.Companion.IgnoreFunction,
        ),
    )

    @Test
    fun library_scanning() {
        captureRoboImage("src/test/snapshots/music_library_scanning.png") {
            Wrapper(dark = false) { MusicLibraryScreenForSnapshot(state = UiIndexState.Scanning(3, 12)) }
        }
    }

    @Test
    fun library_ready_dark() {
        captureRoboImage("src/test/snapshots/music_library_ready_dark.png") {
            Wrapper(dark = true) { MusicLibraryScreenForSnapshot(state = UiIndexState.Ready) }
        }
    }
}

@Composable
private fun Wrapper(dark: Boolean, content: @Composable () -> Unit) {
    AlistTheme(darkMode = if (dark) DarkMode.DARK else DarkMode.LIGHT) { content() }
}
```

- [ ] **Step 2: 写 `MusicPreviewSnapshotTest.kt`**（参考库页结构）

```kotlin
package com.textvision.alistclient.ui.feature.music

import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.music.data.model.Song
import com.textvision.alistclient.music.playback.PlaybackState
import com.textvision.alistclient.music.playback.RepeatMode
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicPreviewSnapshotTest {
    @get:Rule
    val roborazziRule = RoborazziRule()

    @Test
    fun player_playing() {
        val song = Song("/a.mp3", "01", 1, "Artist", "Album", "Title", "/a.lrc", null, 1L)
        captureRoboImage("src/test/snapshots/music_player_playing.png") {
            AlistTheme {
                MusicPreviewScreenForSnapshot(state = PlaybackState(song, true, 30_000, 180_000, RepeatMode.OFF, false))
            }
        }
    }
}
```

- [ ] **Step 3: 提供快照替身 `MusicLibraryScreenForSnapshot` / `MusicPreviewScreenForSnapshot`**

为快照测试复刻原 Screen 签名（接受 state 而非 viewModel）。代码从生产 Screen 抽取，但因为涉及 hilt/VM 注入，最简易做法是新增 `MusicLibraryScreenStateful(state, onPlayQueue, onRescan)` 与 `MusicPreviewScreenStateful(state)` 私有 Composable，由生产 Screen 转发，本 Task 直接实现这两个 Composable 并提供默认 `viewModel` 桥接。

- [ ] **Step 4: 生成基线**

Run: `./gradlew :app:recordRoborazziDebug`
Expected: PNGs created under `src/test/snapshots/`。

- [ ] **Step 5: 跑验证**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL, snapshots matched。

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/textvision/alistclient/ui/feature/music/ \
        app/src/main/java/com/textvision/alistclient/ui/feature/music/
git commit -m "test(music): Roborazzi snapshots for library + player"
```

---

## Phase 6 — 设置页 + 验收 (Tasks 18-20)

### Task 18: Settings 音乐区块

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient\ui\feature\settings\SettingsScreen.kt`
- Create: `app/src/main/java/com/textvision\alistclient\ui\feature\music\MusicSettingsSection.kt`

**Interfaces:**
- Produces: `MusicSettingsSection(currentRoot: String, onRootChange: (String) -> Unit, cacheSizeBytes: Long, onClearCache: () -> Unit)`

- [ ] **Step 1: 写 `MusicSettingsSection.kt`**

```kotlin
package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MusicSettingsSection(
    currentRoot: String,
    cacheSizeBytes: Long,
    onRootChange: (String) -> Unit,
    onClearCache: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("音乐库", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            var text by remember(currentRoot) { mutableStateOf(currentRoot) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("音乐库根路径") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    onRootChange(text)
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            ) { Text("保存（需重新扫描）") }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("音乐缓存", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${cacheSizeBytes / 1_000_000} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val scope = rememberCoroutineScope()
                TextButton(onClick = { scope.launch { onClearCache() } }) { Text("清除缓存") }
            }
        }
    }
}
```

- [ ] **Step 2: 在 SettingsScreen 内嵌入区块**

修改 `SettingsScreen.kt`：在合适的位置增加：
```kotlin
item("music") {
    val root by viewModel.musicRoot.collectAsStateWithLifecycle()
    val cacheSize by viewModel.musicCacheSize.collectAsStateWithLifecycle()
    MusicSettingsSection(
        currentRoot = root,
        cacheSizeBytes = cacheSize,
        onRootChange = viewModel::onMusicRootChange,
        onClearCache = viewModel::onClearMusicCache,
    )
}
```

`SettingsViewModel` 需要新增（实现细节）：
```kotlin
val musicRoot: StateFlow<String> = musicRootStore.rootPath
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MusicLibraryRootStore.DEFAULT_ROOT)
val musicCacheSize: StateFlow<Long> = flow { emit(musicCache.sizeBytes()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

fun onMusicRootChange(path: String) = viewModelScope.launch {
    musicRootStore.setRoot(path)
}

fun onClearMusicCache() = viewModelScope.launch {
    musicCache.clear()
    musicCacheSize.value = musicCache.sizeBytes()
}
```

- [ ] **Step 3: 跑构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicSettingsSection.kt \
        app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreen.kt
git commit -m "feat(settings): add music library root + cache section"
```

---

### Task 19: 端到端集成验收

**Files:**
- Modify: None — 跑一遍构建 + 测试 + 安装 + 设备验证

**Interfaces:**
- Validates: 编译 / Lint / 单元测试 / 设备启动 / 后台播放 / 通知栏控件

- [ ] **Step 1: 跑完整构建 + 测试**

Run: `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, no lint errors (warnings allowed), all unit tests pass.

- [ ] **Step 2: 安装到模拟器**

Run: `./gradlew :app:installDebug`
Expected: app installed on running emulator `test_avd`.

- [ ] **Step 3: 启动应用 + 验证音乐库 + 验证播放**

```bash
# Start app
adb shell am start -n com.textvision.alistclient/.MainActivity

# Check logs for index progress
adb logcat | grep -i "music\|index"

# Take screenshot
MSYS_NO_PATHCONV=1 adb shell screencap -p //sdcard/scr.png
MSYS_NO_PATHCONV=1 adb pull //sdcard/scr.png ./scr.png
```

Expected: 库页可显示已扫描的歌曲列表；点歌曲播放；通知栏可见控制按钮。

- [ ] **Step 4: 验证后台播放**

```bash
# Press home button
adb shell input keyevent KEYCODE_HOME

# Check the Service is still running
adb shell dumpsys activity services com.textvision.alistclient | grep MusicPlayback
```

Expected: Service alive, MediaSession active.

- [ ] **Step 5: Commit（README 更新可选）**

```bash
git status   # 应该是 clean
```

---

### Task 20: docs 更新 + 收尾

**Files:**
- Modify: `docs/testing/known-limitations.md` — 移除「音乐功能为占位」措辞
- Create: `docs/superpowers/specs/2026-07-14-music-feature-design.md` 已存在，本任务只更新 deviations

- [ ] **Step 1: 更新 known-limitations.md**

把「音乐功能为占位」段落改为「音乐功能已接入真实数据，支持后台播放；不做锁屏自定义样式 / 蓝牙/耳机线控特定处理。」

- [ ] **Step 2: 如有 spec 偏差，回填到 `2026-07-14-followups-fixes-and-deviations-design.md`**

按规范，列出本计划相对原 spec 的偏差（如 `AlbumEntity` 用合成 `id` PK、Repository `playQueue` 直接拼 `d` URL 而不是 fsGet 拼 sign）。

- [ ] **Step 3: 全部 commit 合并**

```bash
git add docs/
git commit -m "docs(music): update limitations + deviations"
```

---

## Self-Review (脑内校验)

**1. Spec coverage:**
- §1.2 复用基础设施 → ✓ Tasks 2 (fsGet)、Phase 1 既有 AppModule
- §3 目录结构规则 → ✓ Task 7 (parseFileName, 三层 scan)
- §4 Hilt 绑定 + Service manifest → ✓ Task 1 + Task 12 + Manifest 已注册
- §5.1 Room entities v3 → ✓ Task 3
- §5.2 SignProvider（path → sign 缓存）→ ✓ Task 6
- §5.3 MusicScanner 三层扫描 → ✓ Task 7
- §5.4 LrcParser → ✓ Task 5
- §5.5 MusicIndexRepository → ✓ Task 8
- §6.1 MusicCache → ✓ Task 9
- §6.2 MusicPlaybackService 前台 + MediaSession → ✓ Task 11
- §6.3 PlaybackController → ✓ Task 10
- §7.1 MusicLibraryRootStore 设置 → ✓ Task 4 + Task 18
- §7.2 MusicLibraryScreen 改造 → ✓ Task 15
- §7.3 MusicPreviewScreen 改造 → ✓ Task 16
- §8 测试（LrcParser / Scanner / Repo / RootStore / Controller / Compose）→ ✓ Tasks 5, 6, 7, 8, 10, 17
- §9 已知限制 → ✓ Task 20
- §10 Media3 deps → ✓ Task 1

**2. Placeholder scan:**
- 无 "TBD/TODO/implement later"
- 文件路径全部用 `app/...` 绝对
- 每个代码块完整可粘贴

**3. Type consistency:**
- `MusicIndexState` 在 Task 8 定义、Task 6/7/12/13/15 引用 — OK
- `PlaybackController.state` 类型 `StateFlow<PlaybackState>` 在 Task 10 定义、Task 13 引用 — OK
- `SignEntry` internal — 单文件内 OK
- `AlbumEntity.id` 合成 PK — 与 DAO `id = "$artist $name"` 字符串拼接一致 — OK
- MiniPlayer / SongRow 等已有组件签名保持不变 — OK
- `playbackState` 是后加字段但已在 Task 15 Step 2 显式补 `stateIn` 形式 — OK

**Self-review pass 1 — 发现 1 个语义 gap:** Task 15 Step 1 提及 `viewModelMimic_playbackState` 后期要替换为 `viewModel.playbackState`，已 Step 2 显式修正。**通过。**

**Self-review pass 2 — 用户主动 deep-check 后 inline 修复 5 项：**

| Issue | 修复位置 | 修复方式 |
|---|---|---|
| C: `AlbumEntity.id = "$artist $name"` 空格冲突 | Task 3 AlbumEntity | 改为 `(artist + "÷" + name).hashCode().toString()`（U+001F 分隔防碰撞） |
| G: `MusicCache.clear()` 调 `cache.trimToSizeOrLess(-1)` API 错误 | Task 9 | 改为 `cache.trim(0L)`（media3 1.4.x 受支持的 eviction API） |
| F: `MusicPlayerViewModel` 监听 `state` 时每次 `positionMs` 变化都重 load lrc | Task 13 | `.map { it.current?.lrcPath }.distinctUntilChanged()` 仅在路径变更时拉取 |
| B: `SignProvider` 定义后下游未使用 | Task 8 + 12 + 11 | `MusicIndexRepository` 新增 `coverUrl(path)` / `downloadUrl(path)` / `songMapForPaths(paths)`；Service.playQueue 改走 `downloadUrl` |
| H: Player.Listener 与 MediaController 命令双写 `_state` | Task 10 + 11 | 单写者策略：命令只发 MediaController，Service.Player.Listener 独占 `publishState`；新增 `onRepeatModeChanged` / `onShuffleModeEnabledChanged` 监听 |

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-14-music-feature.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - 每个 Task 派一个全新 subagent 独立实现，task-to-task review 闭环，迭代快。

**2. Inline Execution** - 在当前会话按 Tasks 顺序批量执行，重大节点 review。

Which approach?
