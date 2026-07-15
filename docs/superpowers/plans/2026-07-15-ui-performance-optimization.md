# UI Performance Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以 120Hz Wi-Fi ADB 真机为基准，系统降低首页、音乐页、设置页滚动、主 Tab 切换和播放页进入/加载时的 UI 主线程帧耗时。

**Architecture:** 先用固定 ADB 手势和 `gfxinfo` 建立可复跑基线，再按“根导航 → 普通列表 → 音乐库 → 播放页”逐层缩小状态订阅和重组范围。性能数据与功能测试共同作为门禁；不新增第三方库，不改变现有视觉信息架构。

**Tech Stack:** Kotlin 2.0.21、Jetpack Compose / Material 3、Navigation Compose、StateFlow、JUnit 4、Compose UI Test、PowerShell、ADB、`dumpsys gfxinfo`、Perfetto。

---

## File Map

- Create: `tools/perf/Measure-UiPerformance.ps1` — 固定设备、清零/采集 `gfxinfo`、执行滚动和 Tab 切换场景、输出 JSON/文本结果。
- Create: `docs/testing/ui-performance-baseline-2026-07-15.md` — 优化前设备条件、三轮数据和 Perfetto 热点。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt` — 底栏静态模型与稳定点击边界。
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/BottomNavBar.kt` — 顶层目的地计算和防重复导航。
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt` — 播放页低成本过渡。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt` — 首页 Lazy item 身份与内容类型。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsContent.kt` — 设置页派生数据与 Lazy item 热路径。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModel.kt` — 分离库内容和迷你播放器更新源，缓存 UI 映射结果。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt` — 分离状态收集、稳定列表/网格内容、固定图片身份。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt` — 静态区、进度区、控制区、歌词区独立组合边界。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/components/LyricsView.kt` — 去重自动滚动并避免重复动画。
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/components/PlayerControls.kt` — loading/播放动画限制在绘制子树。
- Test: `app/src/test/java/com/textvision/alistclient/navigation/BottomNavigationPolicyTest.kt` — 当前 Tab 重复点击策略。
- Test: `app/src/test/java/com/textvision/alistclient/navigation/MusicPreviewTransitionSourceTest.kt` — 播放页不做双树 alpha 混合。
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeScreenSourceTest.kt` — 首页 Lazy 内容类型门禁。
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/settings/SettingsContentTest.kt` — 设置数据派生行为。
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModelTest.kt` — 库状态和播放 tick 隔离。
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicPlayerViewModelTest.kt` — 进度/歌词去重。
- Create: `docs/testing/ui-performance-report-2026-07-15.md` — 优化前后数据、残余风险与复测命令。

## Task 1: 真机性能采样工具与基线

**Files:**
- Create: `tools/perf/Measure-UiPerformance.ps1`
- Create: `docs/testing/ui-performance-baseline-2026-07-15.md`

- [ ] **Step 1: 创建先失败的工具契约检查**

Run:

```powershell
$script = 'tools/perf/Measure-UiPerformance.ps1'
if (-not (Test-Path $script)) { throw "missing $script" }
```

Expected: FAIL with `missing tools/perf/Measure-UiPerformance.ps1`。

- [ ] **Step 2: 实现单场景采样脚本**

Create the script with this public contract:

```powershell
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [ValidateSet('home-scroll','settings-scroll','tab-cycle','music-scroll','player-enter','player-playback')]
    [string]$Scenario,
    [int]$Rounds = 3,
    [string]$OutputDirectory = 'build/perf'
)

$ErrorActionPreference = 'Stop'
$adb = 'D:\programming\devtools\android\sdk\platform-tools\adb.exe'
$package = 'com.textvision.alistclient'

function Invoke-Adb([string[]]$Arguments) {
    & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
}

function Invoke-Swipe([int]$x1, [int]$y1, [int]$x2, [int]$y2, [int]$duration = 250) {
    Invoke-Adb @('shell','input','swipe',"$x1","$y1","$x2","$y2","$duration")
}

function Invoke-Scenario([string]$Name) {
    switch ($Name) {
        'home-scroll' { 1..5 | ForEach-Object { Invoke-Swipe 600 2050 600 500; Invoke-Swipe 600 500 600 2050 } }
        'settings-scroll' { Invoke-Adb @('shell','input','tap','1065','2460'); Start-Sleep -Milliseconds 500; 1..5 | ForEach-Object { Invoke-Swipe 600 2050 600 500; Invoke-Swipe 600 500 600 2050 } }
        'tab-cycle' { 1..3 | ForEach-Object { 132,365,600,833,1065,132 | ForEach-Object { Invoke-Adb @('shell','input','tap',"$_",'2460'); Start-Sleep -Milliseconds 250 } } }
        'music-scroll' { Invoke-Adb @('shell','input','tap','600','2460'); Start-Sleep -Milliseconds 500; 1..5 | ForEach-Object { Invoke-Swipe 600 2050 600 550; Invoke-Swipe 600 550 600 2050 } }
        default { throw "$Name requires UI-node coordinates captured in the baseline document before running" }
    }
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
1..$Rounds | ForEach-Object {
    Invoke-Adb @('shell','dumpsys','gfxinfo',$package,'reset') | Out-Null
    Invoke-Scenario $Scenario
    $result = Invoke-Adb @('shell','dumpsys','gfxinfo',$package)
    $result | Set-Content -Encoding utf8 (Join-Path $OutputDirectory "$Scenario-$_.txt")
}
```

For `player-enter` and `player-playback`, extend `Invoke-Scenario` only after capturing the current semantics tree; use deterministic `input tap` coordinates documented in the baseline rather than sleeps longer than 1 second.

- [ ] **Step 3: 验证参数、设备选择和输出**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario home-scroll -Rounds 1
Get-Content build/perf/home-scroll-1.txt | Select-String 'Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|Frame deadline missed'
```

Expected: one result file and all requested metrics present。

- [ ] **Step 4: 采集六个场景三轮基线并写报告**

Run each scenario with `-Rounds 3`. Record device model, Android version, resolution, active refresh rate during gesture, build commit, P50/P90/P95/P99, deadline missed and slow UI thread. For the worst `player-enter` and scroll trace, run:

```powershell
$adb='D:\programming\devtools\android\sdk\platform-tools\adb.exe'
& $adb -s 192.168.0.109:43453 shell perfetto --txt -c /data/misc/perfetto-configs/trace_config.pbtxt -o /data/misc/perfetto-traces/alist-ui.perfetto-trace
```

If the device rejects that system config, use Android Studio System Trace and record that fact; do not fabricate trace results.

- [ ] **Step 5: Commit**

```powershell
git add tools/perf/Measure-UiPerformance.ps1 docs/testing/ui-performance-baseline-2026-07-15.md
git commit -m "test(perf): 添加真机 UI 帧时间基线"
```

## Task 2: 根导航与底栏热路径

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt:110`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/BottomNavBar.kt:23`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt:49`
- Create: `app/src/test/java/com/textvision/alistclient/navigation/BottomNavigationPolicyTest.kt`
- Modify: `app/src/test/java/com/textvision/alistclient/navigation/MusicPreviewTransitionSourceTest.kt`

- [ ] **Step 1: 对三个待改符号运行 GitNexus impact**

Run `impact({target:"AppBottomBar",direction:"upstream",repo:"alist"})`, `impact({target:"AppBottomNavBar",direction:"upstream",repo:"alist"})`, and `impact({target:"hyperOsEnterTransition",direction:"upstream",repo:"alist"})`. Report direct callers, processes and risk. Stop for user confirmation if any result is HIGH or CRITICAL.

- [ ] **Step 2: 写重复点击策略失败测试**

```kotlin
package com.textvision.alistclient.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationPolicyTest {
    @Test fun sameTabDoesNotNavigate() {
        assertFalse(shouldNavigateToTab(currentTab = "music", targetTab = "music"))
    }

    @Test fun differentTabNavigates() {
        assertTrue(shouldNavigateToTab(currentTab = "home", targetTab = "music"))
    }
}
```

- [ ] **Step 3: 运行测试确认 RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.textvision.alistclient.navigation.BottomNavigationPolicyTest`

Expected: FAIL because `shouldNavigateToTab` does not exist。

- [ ] **Step 4: 实现稳定模型和导航策略**

Move bottom items to a file-level immutable reference in `AppBars.kt`:

```kotlin
private val BottomItems = listOf(
    BottomItem("home", "首页", AppIcons.home),
    BottomItem("files", "文件", AppIcons.file),
    BottomItem("music", "音乐", AppIcons.musicNote),
    BottomItem("transfers", "传输", AppIcons.transfer),
    BottomItem("settings", "设置", AppIcons.settings),
)
```

Add to `BottomNavBar.kt` and guard the callback:

```kotlin
internal fun shouldNavigateToTab(currentTab: String, targetTab: String): Boolean =
    currentTab != targetTab

onNavigate = { route ->
    if (!shouldNavigateToTab(currentTab, route)) return@AppBottomBar
    val target: Any = when (route) {
        "home" -> HomeDest
        "files" -> FilesDest()
        "music" -> MusicLibraryDest
        "transfers" -> TransfersDest
        "settings" -> SettingsDest
        else -> return@AppBottomBar
    }
    navController.navigate(target) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

Keep main-tab transitions as `EnterTransition.None` / `ExitTransition.None`. Change music preview entry to an opaque, short slide only and assert the transition source contains no `fadeIn` or `fadeOut` in the music-preview branches.

- [ ] **Step 5: 运行导航测试和 Tab 真机采样**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.navigation.*"
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario tab-cycle -Rounds 3 -OutputDirectory build/perf/after-nav
```

Expected: tests PASS; no duplicate destination on repeated current-tab taps; Tab P90/P95 improves or remains within noise while deadline misses decrease.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt app/src/main/java/com/textvision/alistclient/navigation/BottomNavBar.kt app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt app/src/test/java/com/textvision/alistclient/navigation
git commit -m "perf(navigation): 精简主导航组合热路径"
```

## Task 3: 首页与设置页 Lazy 列表

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt:103`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsContent.kt:39`
- Create: `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeScreenSourceTest.kt`
- Modify: `app/src/test/java/com/textvision/alistclient/ui/feature/settings/SettingsContentTest.kt`

- [ ] **Step 1: 对 `DashboardList` 和 `SettingsContent` 运行 upstream impact 并报告风险**

- [ ] **Step 2: 写 Lazy identity 源码门禁失败测试**

```kotlin
package com.textvision.alistclient.ui.feature.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenSourceTest {
    @Test fun dashboardDeclaresStableContentTypes() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt").readText()
        assertTrue(source.contains("key = \"hero\", contentType = \"hero\""))
        assertTrue(source.contains("key = { it.mountPath }"))
        assertTrue(source.contains("contentType = { \"storage\" }"))
    }
}
```

- [ ] **Step 3: 运行测试确认 RED**

Run the new test class; expect failure on missing `contentType` declarations。

- [ ] **Step 4: 为首页每类条目声明固定身份和类型**

Use explicit keyed items:

```kotlin
item(key = "hero", contentType = "hero") { HeroServerCard(data.publicSection, online = isOnline) }
item(key = "metrics", contentType = "metrics") {
    MetricRow(
        serverStats = data.serverStatsSection,
        session = data.sessionSection,
        onRetryServerStats = { onRetrySection(SectionKey.ServerStats) },
        onRetrySession = { onRetrySection(SectionKey.Session) },
    )
}
item(key = "tasks", contentType = "tasks") {
    Column(modifier = Modifier.testTag("home_task_section")) {
        TaskHeader()
        Spacer(Modifier.height(8.dp))
        TaskSection(data.taskSection) { onRetrySection(SectionKey.Task) }
    }
}
item(key = "storage-header", contentType = "header") { StorageHeader(onManage = onManageStorage) }
items(
    items = storages,
    key = { it.mountPath },
    contentType = { "storage" },
) { storage ->
    StorageCard(storage = storage) { onStorageClick(storage.mountPath) }
}
```

In settings, compute `announcement` and the three visible storages once before `LazyColumn`:

```kotlin
val visibleStorages = uiState.storages.take(3)
val announcement = uiState.quickSettings.firstOrNull { it.key == "announcement" }
```

Use those stable local references in item lambdas; preserve all existing item keys/content types and behavior.

- [ ] **Step 5: 跑测试与真机滚动对照**

Run home/settings unit tests, then three rounds of `home-scroll` and `settings-scroll`. Expected: no functional changes; P90/P95 and deadline misses do not regress, and repeated rounds do not worsen.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsContent.kt app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeScreenSourceTest.kt app/src/test/java/com/textvision/alistclient/ui/feature/settings/SettingsContentTest.kt
git commit -m "perf(ui): 稳定首页和设置列表内容复用"
```

## Task 4: 音乐库状态与列表隔离

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModel.kt:25`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt:90`
- Modify: `app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModelTest.kt`

- [ ] **Step 1: 对 `MusicLibraryViewModel.state`、`MusicLibraryScreen`、`PagedList` 运行 upstream impact**

- [ ] **Step 2: 写状态收集边界的失败测试**

Create `MusicLibraryScreenStateIsolationTest.kt`:

```kotlin
class MusicLibraryScreenStateIsolationTest {
    @Test fun playbackCollectionLivesOnlyInMiniPlayerBoundary() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt").readText()
        val root = source.substringBefore("private fun MusicLibraryMiniPlayer(")
        val boundary = source.substringAfter("private fun MusicLibraryMiniPlayer(")
        assertFalse(root.contains("viewModel.playbackState.collectAsStateWithLifecycle()"))
        assertTrue(boundary.contains("viewModel.playbackState.collectAsStateWithLifecycle()"))
    }
}
```

- [ ] **Step 3: 运行测试确认 RED**

Expected: FAIL because `MusicLibraryMiniPlayer` does not exist and playback is collected in the root screen.

- [ ] **Step 4: 分离屏幕状态收集边界**

Keep library content and mini-player state in separate composables:

```kotlin
@Composable
fun MusicLibraryScreen(
    onOpenPreview: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MusicLibraryViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    MusicLibraryContent(
        ui = ui,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onPlayQueue = { songs, index ->
            viewModel.onPlayQueueClick(context, songs, index)
            onOpenPreview()
        },
        onRescan = viewModel::onRescanClick,
        onBack = onBack,
    )
    MusicLibraryMiniPlayer(viewModel = viewModel, onOpenPreview = onOpenPreview)
}

@Composable
private fun MusicLibraryMiniPlayer(viewModel: MusicLibraryViewModel, onOpenPreview: () -> Unit) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    playback.current?.let { song ->
        MiniPlayer(
            title = song.title,
            artist = song.artist,
            isPlaying = playback.isPlaying,
            gradient = MiniPlayerGradient,
            onPlayPause = viewModel::onTogglePlayPause,
            onClick = onOpenPreview,
        )
    }
}
```

All `LazyColumn`, `LazyRow`, and `LazyVerticalGrid` dynamic items must use path/album identity keys plus `contentType`. Precompute currently visible slices with `remember(list, visibleCount) { list.take(visibleCount) }`; do not sort/map inside item blocks.

- [ ] **Step 5: 运行音乐库测试与三个子页滚动采样**

Expected: tests PASS; position ticks do not recompose library lists; each music tab shows identical content and preserves its scroll state during tab switching.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModel.kt app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicLibraryViewModelTest.kt
git commit -m "perf(music): 隔离音乐库高频播放状态"
```

## Task 5: 播放页重组边界与封面加载

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt:51`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPlayerViewModel.kt:25`
- Modify: `app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicPlayerViewModelTest.kt`

- [ ] **Step 1: 对 `MusicPreviewScreen`、`ArtworkLayer`、`MusicPlayerViewModel.state` 运行 upstream impact**

- [ ] **Step 2: 写控制状态投影忽略 progress 的失败测试**

```kotlin
@Test fun chromeProjectionIgnoresPositionAndDuration() {
    val first = MusicPlayerUiState(playback = PlaybackState(positionMs = 1_000L, durationMs = 180_000L))
    val second = MusicPlayerUiState(playback = PlaybackState(positionMs = 2_000L, durationMs = 180_000L))
    assertEquals(first.toPlayerChromeState(), second.toPlayerChromeState())
}
```

The new `PlayerChromeState` contains only current song identity/title/artist/album, playing, repeat, shuffle, preparing and artwork; it contains no `positionMs` or `durationMs`.

- [ ] **Step 3: 运行测试确认 RED**

Expected: compile failure because `toPlayerChromeState` does not exist。

- [ ] **Step 4: 实现页面五区隔离**

Add a stable projected flow in the ViewModel:

```kotlin
data class PlayerChromeState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val preparing: Boolean = false,
    val artworkData: ByteArray? = null,
)

val chromeState = state
    .map(MusicPlayerUiState::toPlayerChromeState)
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerChromeState())

internal fun MusicPlayerUiState.toPlayerChromeState() = PlayerChromeState(
    current = playback.current,
    isPlaying = playback.isPlaying,
    repeatMode = playback.repeatMode,
    shuffle = playback.shuffle,
    preparing = playback.preparing,
    artworkData = playback.artworkData,
)
```

In `MusicPreviewScreen`, collect `chromeState` at the page level, collect `progress` only inside `PlayerProgressSection`, and collect `currentLineIndex` only inside `PlayerLyricsSection`. Keep artwork container at a fixed `aspectRatio(1f)` so placeholder-to-image does not remeasure the page. Decode artwork through the injected IO dispatcher and downsample to the displayed pixel target before converting to `ImageBitmap`.

- [ ] **Step 5: 跑测试和三个入口的播放页采样**

Run `MusicPlayerViewModelTest`, Compose smoke tests, and `player-enter` three rounds from song/album/artist. Expected: no full-page recomposition on progress, no main-thread bitmap decode, and lower player-enter P90/P95.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPlayerViewModel.kt app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicPlayerViewModelTest.kt
git commit -m "perf(music): 隔离播放页进度和静态内容"
```

## Task 6: 歌词自动滚动与 Loading 动画

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/components/LyricsView.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/components/PlayerControls.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicComposableSmokeTest.kt`

- [ ] **Step 1: 对 `LyricsView`、`PlayerControls` 和 loading 指示器符号运行 upstream impact**

- [ ] **Step 2: 写歌词目标索引纯函数失败测试**

```kotlin
@Test fun lyricScrollTargetIgnoresInvalidAndRepeatedIndex() {
    assertNull(nextLyricScrollTarget(previous = 4, current = 4, lineCount = 20))
    assertNull(nextLyricScrollTarget(previous = 4, current = -1, lineCount = 20))
    assertEquals(7, nextLyricScrollTarget(previous = 4, current = 7, lineCount = 20))
}
```

- [ ] **Step 3: 运行测试确认 RED**

Expected: FAIL because `nextLyricScrollTarget` does not exist。

- [ ] **Step 4: 只在歌词目标真正变化时滚动**

```kotlin
internal fun nextLyricScrollTarget(previous: Int, current: Int, lineCount: Int): Int? =
    current.takeIf { it in 0 until lineCount && it != previous }
```

Track the last requested index with `remember { mutableIntStateOf(-1) }`. Key `LaunchedEffect` by the validated target, use one `animateScrollToItem` call per lyric change, and let effect cancellation replace an in-flight animation instead of launching parallel jobs.

Keep player loading inside a fixed 48dp control slot. Any wave/loading infinite transition may update only `graphicsLayer`/draw properties of that slot; it must not switch the size or presence of surrounding controls.

- [ ] **Step 5: 跑测试与播放中采样**

Expected: loading/control layout remains fixed; lyric changes do not produce overlapping scroll animations; `player-playback` P95 and slow UI frames improve.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/textvision/alistclient/ui/feature/music/components/LyricsView.kt app/src/main/java/com/textvision/alistclient/ui/feature/music/components/PlayerControls.kt app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt app/src/test/java/com/textvision/alistclient/ui/feature/music/MusicComposableSmokeTest.kt
git commit -m "perf(music): 限制歌词和加载动画更新范围"
```

## Task 7: 完整验证与性能报告

**Files:**
- Create: `docs/testing/ui-performance-report-2026-07-15.md`
- Modify: no production files; a failed verification returns execution to the owning task before this task continues

- [ ] **Step 1: 运行完整静态与单元验证**

Run:

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, no lint errors, all unit tests pass。

- [ ] **Step 2: 安装同一构建并预热真机**

```powershell
./gradlew.bat :app:installDebug
$adb='D:\programming\devtools\android\sdk\platform-tools\adb.exe'
& $adb -s 192.168.0.109:43453 shell am force-stop com.textvision.alistclient
& $adb -s 192.168.0.109:43453 shell am start -n com.textvision.alistclient/.MainActivity
```

Open each target screen once before measurement; do not include first install/dex warm-up in final comparison.

- [ ] **Step 3: 重跑六个场景，每个三轮**

Use `Measure-UiPerformance.ps1` with `build/perf/final`. Record actual refresh mode during gestures and compare medians, not the single best run.

- [ ] **Step 4: 编写最终报告**

The report must contain a table with baseline/final P50, P90, P95, P99, deadline missed, slow UI frames, percentage delta, build commits, device temperature/refresh conditions, functional verification, and any remaining hotspot. Do not claim 120Hz success if gesture-time refresh-rate evidence is absent.

- [ ] **Step 5: GitNexus 变更检测**

Run `detect_changes({scope:"compare",base_ref:"main",repo:"alist"})`. Review every changed symbol and affected process; resolve unexpected scope before committing the report.

- [ ] **Step 6: Commit final report**

```powershell
git add docs/testing/ui-performance-report-2026-07-15.md
git commit -m "docs(perf): 记录 UI 性能优化验收结果"
```

## Self-Review

- Spec §3 measurement: Task 1 and Task 7 cover six fixed real-device scenarios, refresh-rate evidence, `gfxinfo`, and Perfetto fallback.
- Spec §4.1 navigation: Task 2 covers bottom-bar allocation, duplicate navigation, main-tab transitions, and player transition.
- Spec §4.2 home/settings: Task 3 covers keys, content types, and hot-path derivation.
- Spec §4.3 music library: Task 4 covers state isolation, list/grid identity, paging slices, and playback tick isolation.
- Spec §4.4 player: Tasks 5–6 cover five recomposition boundaries, artwork decode, loading size, progress, and lyrics.
- Spec §5 testing: every production behavior change has a RED/GREEN test step; Task 7 runs full verification.
- Type consistency: `PlayerChromeState`, `shouldNavigateToTab`, and `nextLyricScrollTarget` are defined before downstream use.
- Placeholder scan completed; every code-changing step names its concrete implementation.
