# UI Expressive 后续收尾 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消化 `.superpowers/sdd/progress.md` 18 项遗留问题 (P0×4 + P1×6 + P2×8),把 UI Expressive 重设计 (Task 1-32) 留下的 UX/架构/清理缺口一次性收口。

**Architecture:**
- 单 main 分支串行 19 个 commit (P1-6 拆 6a 替换 + 6b 删除两步)
- 每任务 TDD: 失败测试 → 最小实现 → 验证 → commit
- 每任务结束 `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` 全绿
- P0/P1 额外在 `test_avd` 模拟器 (GPU 软件渲染) 手动验证

**Tech Stack:**
- Kotlin 2.0.21 + AGP 8.7.2 + Compose BOM 2024.09.03
- Hilt 2.52 + Retrofit 2.11 + Room 2.6.1
- Turbine (Flow test) + MockK 1.13 + Robolectric + MockWebServer
- Roborazzi (visual regression) 1.x

**Spec:** `docs/superpowers/specs/2026-07-08-ui-expressive-followups-design.md`

## Global Constraints

- 单文件 ≤ 400 行 (项目硬性规则)
- 每 ViewModel 必须有 `val uiState: StateFlow<UiState>` 或已知偏离
- Compose 状态收集统一 `collectAsStateWithLifecycle` (无 `collectAsState`)
- 测试镜像源码包路径 (`app/src/test/java/...`)
- 验证命令:`./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`
- 模拟器:`MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" ...`
- 进度追踪:每完成一项,在 `.superpowers/sdd/progress.md` 追加一行 `Task N: complete (...)`
- Git 提交:用户 = `贾晓源`,commit 标题统一 `<type>(<scope>): <subject>`,type ∈ {feat, fix, refactor, chore, docs, test}
- 改任何 symbol 前必跑 `gitnexus_impact({target, direction: "upstream"})` 报 blast radius
- commit 前必跑 `gitnexus_detect_changes()` 验 scope

---

## File Structure

| 路径 | 职责 |
|------|------|
| `app/src/main/java/.../navigation/AppNavHost.kt` | 改 Scaffold 结构 (§1) |
| `app/src/main/java/.../ui/feature/file/FileScreen.kt` | 加 upload 入口 + per-row 菜单 (§2) |
| `app/src/main/java/.../ui/components/ListItemRow.kt` | 扩展 onMoreClick 支持 (§2) |
| `app/src/main/java/.../util/FileSizeFormatter.kt` | **新增** 字节 humanize (§3) |
| `app/src/main/java/.../ui/components/...Preview.kt` (5 文件) | **新增** 主题 token preview (§4) |
| `app/src/main/java/.../ui/feature/.../<Screen>Preview.kt` (4 文件) | **新增** 屏幕 shell preview (§4) |
| `app/src/main/java/.../auth/SessionEvent.kt` | **新增** sealed (§5) |
| `app/src/main/java/.../auth/SessionGate.kt` | **新增** @Singleton (§5) |
| `app/src/main/java/.../auth/AuthRepository.kt` | 改:加 sessionEvents + emit (§5) |
| `app/src/main/java/.../network/AuthInterceptor.kt` | 改:401 触发 emit (§5) |
| `app/src/main/java/.../common/network/NetworkMonitor.kt` | 改:NetworkDown emit (§5) |
| `app/src/main/java/.../MainActivity.kt` | 改:注入 SessionGate + LaunchedEffect 收 navEvent (§5) |
| `app/src/main/java/.../admin/storage/StorageRowItem.kt` | 改:CloudListItem→ListItemRow (§6a) |
| `app/src/main/java/.../admin/cookie/WebCookieDialog.kt` | 改:Cloud*→colorScheme (§6a) |
| `app/src/main/java/.../admin/form/DynamicFormField.kt` | 改:CloudSurfaceMuted→surfaceContainerHigh (§6a) |
| `app/src/main/java/.../home/HomeScreen.kt` | 改:CloudScaffold→AppScaffold (§6a) |
| `app/src/main/java/.../ui/components/DirectoryBrowser.kt` | 改:LazyColumn+ListItemRow+Breadcrumb (§6a) |
| `app/src/main/java/.../ui/components/Cloud*.kt` (11) | **删除** (§6b) |
| `app/src/main/java/.../ui/theme/Color.kt` | 改:删 5 alias (§6b) |
| `app/src/main/java/.../ui/feature/preview/PreviewScreen.kt` | 改:CloudCard→Card (§7) |
| `app/src/main/java/.../ui/feature/preview/PreviewAudio.kt` | 改:Cloud*→colorScheme (§7) |
| `app/src/main/java/.../ui/feature/preview/PreviewFallback.kt` | 改:Cloud*→colorScheme (§7) |
| `app/src/test/java/.../<X>ScreenSnapshotTest.kt` (4 文件) | **新增/扩展** Roborazzi baselines (§8) |
| `app/src/main/java/.../ui/feature/transfer/TransferScreen.kt` | 改:加 Spring/AnimatedContent (§9) |
| `app/src/main/java/.../ui/feature/file/FileScreen.kt` | 改:ListItemRow AnimatedVisibility (§9) |
| `app/src/main/java/.../ui/feature/home/HomeScreen.kt` | 改:Tab AnimatedContent (§9) |
| `app/src/test/java/.../transfer/TransferViewModelTest.kt` | 加 3 测试 (§10) |
| `app/src/test/java/.../settings/SettingsViewModelTest.kt` | 加 3 测试 (§10) |
| `app/src/main/java/.../ui/feature/home/*` | **迁移自** `home/` (§11) |
| `app/src/main/java/.../settings/SettingsViewModel.kt` | 改:combine+stateIn (§12) |
| `app/src/main/java/.../file/FileViewModel.kt` | 改:isAllSelected→visibleFiles (§13) |
| `app/src/main/java/.../transfer/TransferStatus.kt` | 改:加 activeStatuses (§14) |
| `app/src/main/java/.../ui/feature/transfer/TransferScreen.kt` | 改:删本地 ActiveTransferStatuses (§14) |
| `app/src/main/java/.../navigation/AppDestination.kt` | 改:mime→fileTypeName (§15) |
| `app/src/main/java/.../navigation/AppNavHost.kt` | 改:args.mime→fileTypeName (§15) |
| `app/src/main/java/.../ui/feature/preview/PreviewViewModel.kt` | 改:textRepository private (§16) |
| `app/src/main/java/.../transfer/TransferNotificationController.kt` | 改:删 SDK_INT<O (§17) |
| `app/src/main/java/.../*.kt` (各含 ModifierParameter) | 改:@Suppress 或默认 modifier (§17) |
| `app/src/main/java/.../transfer/TransferManager.kt:333` | 改:删 @RequiresApi(O) (§18) |
| `.superpowers/sdd/progress.md` | 追加:每项完成 1 行 |

---

### Task 1: P0-1 SnackbarHost + AppBottomNavBar 重叠修复

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/textvision/alistclient/navigation/AppNavHostTest.kt` (新建,如缺)

**Interfaces:**
- Consumes: `MainActivity` 传 `snackbarHostState: SnackbarHostState`
- Produces: `AppNavHost` 接受 snackbarHostState 必传 (非可空),移 AppBottomBar 进 Scaffold.bottomBar slot

- [ ] **Step 1: 跑 gitnexus_impact 确认 blast radius**

```
用 mcp__gitnexus__impact,target=AppNavHost,direction=upstream
```

期望:AppNavHost 被 MainActivity 调用,Risk = LOW。

- [ ] **Step 2: 改 AppNavHost.kt**

新结构 (替换原 `Box { Scaffold { NavHost }; AppBottomBar align BottomCenter }`):

```kotlin
@Composable
fun AppNavHost(
    startAuthenticated: Boolean,
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState,
) {
    val startDestination: Any = if (startAuthenticated) FilesDest() else LoginDest

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { hyperOsEnterTransition() },
            exitTransition = { hyperOsExitTransition() },
            popEnterTransition = { hyperOsPopEnterTransition() },
            popExitTransition = { hyperOsPopExitTransition() },
        ) {
            // ... 原有 composable 块不变 ...
        }
    }
}
```

- 删除外层 `Box`,`Alignment.BottomCenter` 引用
- 删 `Box`/`Alignment` 的 import
- `snackbarHostState` 参数类型由 `SnackbarHostState?` 改为非空 `SnackbarHostState`
- `MainActivity.kt` 调用处不变 (原本就传非空)

- [ ] **Step 3: 跑验证**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

期望:全绿。

- [ ] **Step 4: 模拟器手动验证**

```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:登录后底部栏可见,Files/Transfers/Settings 三个 Tab 切换正常无重叠。

- [ ] **Step 5: 跑 gitnexus_detect_changes 验 scope**

```
用 mcp__gitnexus__detect_changes
```

期望:仅 AppNavHost.kt 改动,无意外 symbol 涉及。

- [ ] **Step 6: 更新 progress.md + commit**

在 `.superpowers/sdd/progress.md` 追加:
```
Task 33: complete (commits <hash>, review 待跑, P0-1 Snackbar+BottomBar 重叠修复:AppBottomBar 移 Scaffold.bottomBar slot,MainActivity 调用无影响)
```

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt .superpowers/sdd/progress.md
git commit -m "fix(nav): move AppBottomBar into Scaffold.bottomBar slot to fix Snackbar overlap"
```

---

### Task 2: P0-2a FileListItemRow 支持 onMoreClick

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/ListItemRow.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/components/ListItemRowTest.kt` (新建,如缺)

**Interfaces:**
- Produces: `ListItemRow(onMoreClick: (() -> Unit)? = null)` 新增可选参数,为非空时显示 `more_vert` IconButton

- [ ] **Step 1: 跑 gitnexus_impact 确认 blast radius**

```
用 mcp__gitnexus__impact,target=ListItemRow,direction=upstream
```

期望:仅 FileScreen/HomeScreen/StorageRowItem 等列表屏使用,Risk = LOW。

- [ ] **Step 2: 改 ListItemRow.kt 签名 + 加 more 按钮**

在 ListItemRow 主 composable 添加参数:

```kotlin
@Composable
fun ListItemRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,  // 新增
) {
    Row(modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
        leading?.invoke()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        trailing?.invoke()
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}
```

- 若已存在 `onMoreClick` 参数 (Task 18 已加),跳过此 Task

- [ ] **Step 3: 跑验证**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

- [ ] **Step 4: commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/ListItemRow.kt
git commit -m "feat(ui): add onMoreClick to ListItemRow"
```

---

### Task 3: P0-2b FileScreen TopBar Upload 入口

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/file/FileScreenTest.kt`

**Interfaces:**
- Consumes: `TransferManager.enqueueUpload(uri: Uri, targetPath: String)`
- Produces: `FileScreen` 顶部 IconButton(Icons.Default.Upload),点击启动 `GetContent("*/*")`,选完调 TransferManager

- [ ] **Step 1: 加失败测试 (FileScreenTest.kt)**

```kotlin
@Test
fun upload_button_triggers_file_picker() {
    val pickerLauncher = mockk<ManagedActivityResultLauncher<String, Uri?>>(relaxed = true)
    composeRule.setContent { /* ... FileScreen + 注入 mock launcher ... */ }
    composeRule.onNodeWithContentDescription("上传").performClick()
    verify { pickerLauncher.launch("*/*") }
}
```

(具体 mock 注入路径以现有 FileScreen Test 风格为准)

- [ ] **Step 2: 跑测试,确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.file.FileScreenTest.upload_button_triggers_file_picker"
```

- [ ] **Step 3: 实现 upload 入口**

在 `FileScreen` composable 内:

```kotlin
val context = LocalContext.current
val scope = rememberCoroutineScope()
val snackbarHostState = LocalSnackbarHostState.current
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri != null) {
        val target = "$currentPath/${uri.lastPathSegment ?: "file"}"
        scope.launch {
            runCatching { transferManager.enqueueUpload(uri, target) }
                .onSuccess { snackbarHostState.showSnackbar("已加入传输队列") }
                .onFailure { snackbarHostState.showSnackbar("上传失败") }
        }
    }
}

// TopBar
AppTopBar(
    title = currentPath,
    actions = {
        IconButton(onClick = { launcher.launch("*/*") }) {
            Icon(Icons.Default.Upload, contentDescription = "上传")
        }
        // ... 原 actions ...
    },
)
```

- 需要 import `LocalSnackbarHostState`(已在 `MainActivity` 定义)

- [ ] **Step 4: 跑测试,确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.file.FileScreenTest.upload_button_triggers_file_picker"
```

- [ ] **Step 5: 跑全量验证**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

- [ ] **Step 6: 模拟器验证 + commit**

```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:文件页 TopBar 出现 upload 图标,点击 → 系统文件选择器 → 选文件 → 切 Transfers Tab 可见任务。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt app/src/test/java/com/textvision/alistclient/ui/feature/file/FileScreenTest.kt
git commit -m "feat(file): TopBar upload entry via GetContent contract"
```

---

### Task 4: P0-2c FileScreen per-row 下载/分享/复制直链

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/file/FileScreenTest.kt`

**Interfaces:**
- Consumes: `TransferManager.enqueueDownload(file: FileItem)`,`LocalContext` (startActivity),`LocalClipboardManager`
- Produces: `FileScreen` 每行传 `onMoreClick = { showMenuFor(file) }`,弹出 `DropdownMenu` 3 项

- [ ] **Step 1: 加失败测试**

```kotlin
@Test
fun row_more_click_shows_dropdown_with_three_actions() {
    composeRule.setContent { FileScreen(... ) }
    composeRule.onAllNodesWithContentDescription("更多").onFirst().performClick()
    composeRule.onNodeWithText("下载").assertIsDisplayed()
    composeRule.onNodeWithText("分享").assertIsDisplayed()
    composeRule.onNodeWithText("复制直链").assertIsDisplayed()
}

@Test
fun copy_link_click_copies_to_clipboard() {
    val clipboard = mockk<ClipboardManager>(relaxed = true)
    // ... setContent with overridden LocalClipboardManager ...
    composeRule.onAllNodesWithContentDescription("更多").onFirst().performClick()
    composeRule.onNodeWithText("复制直链").performClick()
    verify { clipboard.setPrimaryClip(any()) }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.file.FileScreenTest.row_more_click_shows_dropdown_with_three_actions"
```

- [ ] **Step 3: 实现 DropdownMenu + 3 actions**

```kotlin
@Composable
private fun FileRowMenu(
    file: FileItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("下载") }, onClick = { onDownload(); onDismiss() })
        DropdownMenuItem(text = { Text("分享") }, onClick = { onShare(); onDismiss() })
        DropdownMenuItem(text = { Text("复制直链") }, onClick = { onCopyLink(); onDismiss() })
    }
}

// In FileScreen main:
val context = LocalContext.current
val snackbarHostState = LocalSnackbarHostState.current
val clipboard = LocalClipboardManager.current
var menuFor by remember { mutableStateOf<FileItem?>(null) }

// FileRow call:
ListItemRow(
    title = file.name,
    subtitle = FileSizeFormatter.humanize(file.size),
    onClick = { onPreview(file) },
    leading = { FileTypeIcon(file.type) },
    onMoreClick = { menuFor = file },
)

menuFor?.let { f ->
    FileRowMenu(
        file = f,
        expanded = true,
        onDismiss = { menuFor = null },
        onDownload = {
            scope.launch {
                runCatching { transferManager.enqueueDownload(f) }
                    .onSuccess { snackbarHostState.showSnackbar("已加入下载队列") }
            }
        },
        onShare = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, f.downloadUrl)
            }
            context.startActivity(Intent.createChooser(intent, "分享"))
        },
        onCopyLink = {
            clipboard.setText(AnnotatedString(f.downloadUrl))
            scope.launch { snackbarHostState.showSnackbar("已复制直链") }
        },
    )
}
```

- `FileItem` 的 `downloadUrl` 字段已存在 (Task 18/22 已接)
- `LocalClipboardManager` 来自 `androidx.compose.ui.platform.LocalClipboardManager`

- [ ] **Step 4: 跑测试,确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.file.FileScreenTest"
```

- [ ] **Step 5: 跑全量验证**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

- [ ] **Step 6: 模拟器验证 + commit**

```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:每行 `more_vert` → 菜单 3 项 → 复制直链 → 系统弹"已复制"。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt app/src/test/java/com/textvision/alistclient/ui/feature/file/FileScreenTest.kt
git commit -m "feat(file): per-row dropdown with download/share/copy-link"
```

---

### Task 5: P0-3 字节数 Humanize

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/util/FileSizeFormatter.kt`
- Test: `app/src/test/java/com/textvision/alistclient/util/FileSizeFormatterTest.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt` (调用 humanize 替代原始 bytes)
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewScreen.kt` (同步替换)

- [ ] **Step 1: 加失败测试 (FileSizeFormatterTest.kt)**

```kotlin
class FileSizeFormatterTest {
    @Test fun `zero bytes returns B`() = assertEquals("0 B", FileSizeFormatter.humanize(0))
    @Test fun `512 bytes returns B`() = assertEquals("512 B", FileSizeFormatter.humanize(512))
    @Test fun `2 KB returns 2_0 KB`() = assertEquals("2.0 KB", FileSizeFormatter.humanize(2048))
    @Test fun `5 MB returns 5_0 MB`() = assertEquals("5.0 MB", FileSizeFormatter.humanize(5L * 1024 * 1024))
    @Test fun `10 GB returns 10_00 GB`() = assertEquals("10.00 GB", FileSizeFormatter.humanize(10L * 1024 * 1024 * 1024))
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.util.FileSizeFormatterTest"
```

期望:FAIL (FileSizeFormatter 未定义)

- [ ] **Step 3: 创建 FileSizeFormatter.kt**

```kotlin
package com.textvision.alistclient.util

object FileSizeFormatter {
    fun humanize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
        else -> "%.2f GB".format(bytes / 1024.0 / 1024 / 1024)
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.util.FileSizeFormatterTest"
```

- [ ] **Step 5: 在 FileScreen + PreviewScreen 替换 bytes 显示**

- FileScreen.kt:把 `subtitle = "${file.size} bytes"` 改为 `subtitle = FileSizeFormatter.humanize(file.size)`
- PreviewScreen.kt:同样替换 size 显示

- [ ] **Step 6: 跑全量验证**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

- [ ] **Step 7: 模拟器验证 + commit**

```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:文件列表大小栏显示 "11.8 MB" 而非原始字节数。

```bash
git add app/src/main/java/com/textvision/alistclient/util/FileSizeFormatter.kt app/src/test/java/com/textvision/alistclient/util/FileSizeFormatterTest.kt app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewScreen.kt
git commit -m "feat(util): FileSizeFormatter.humanize; apply to FileScreen+PreviewScreen"
```

---

### Task 6: P0-4 补 25+ @Preview - 主题 token

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/ColorPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/TypePreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/ShapePreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/MotionPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreview.kt`

- [ ] **Step 1: 跑 gitnexus_impact 确认 blast radius**

```
用 mcp__gitnexus__impact,target=AlistClientTheme,direction=upstream
```

期望:Risk = LOW (theme preview 不影响主屏)。

- [ ] **Step 2: 创建 ColorPreview.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AlistClientTheme

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), color = color) {}
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Light/Dark Colors", showBackground = true, widthDp = 360, heightDp = 600)
@Composable
private fun ColorsPreview() {
    AlistClientTheme {
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Light", style = MaterialTheme.typography.titleMedium) }
            item { ColorSwatch("primary", IndigoBlue40) }
            item { ColorSwatch("surface", Color(0xFFFFFFFF)) }
            item { Text("Dark", style = MaterialTheme.typography.titleMedium) }
            item { ColorSwatch("primary", IndigoBlue80) }
            item { ColorSwatch("surface", Color(0xFF1C1B1F)) }
        }
    }
}
```

(具体颜色值从 Color.kt 现有常量取)

- [ ] **Step 3: 创建 TypePreview.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Typography", showBackground = true)
@Composable
private fun TypePreview() {
    AlistClientTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Display Large", style = MaterialTheme.typography.displayLarge)
            Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
            Text("Body Large", style = MaterialTheme.typography.bodyLarge)
            Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
            Text("Label Small", style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

- [ ] **Step 4: 创建 ShapePreview.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "AppShapes", showBackground = true)
@Composable
private fun ShapePreview() {
    AlistClientTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AppShapes.Card / Button / Dialog")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = AppShapes.Card, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp, 40.dp)) {}
                Surface(shape = AppShapes.Button, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(80.dp, 40.dp)) {}
                Surface(shape = AppShapes.Dialog, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(80.dp, 40.dp)) {}
            }
        }
    }
}
```

- [ ] **Step 5: 创建 MotionPreview.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "HyperOsMotion", showBackground = true)
@Composable
private fun MotionPreview() {
    var toggle by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("HyperOsMotion tokens (SpringFast / SpringMedium / emphasized)")
        val width by animateFloatAsState(if (toggle) 300f else 100f,
            animationSpec = HyperOsMotion.SpringFast, label = "fast")
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            drawRect(androidx.compose.ui.graphics.Color.Magenta, size = androidx.compose.ui.geometry.Size(width, 40f))
        }
        Button(onClick = { toggle = !toggle }) { Text("Toggle SpringFast") }
    }
}
```

- [ ] **Step 6: 创建 ThemePreview.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Theme Light", showBackground = true)
@Composable
private fun ThemeLightPreview() {
    AlistClientTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Text("Light Theme", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = {}) { Text("Primary Button") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
        }
    }
}

@Preview(name = "Theme Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeDarkPreview() {
    AlistClientTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Text("Dark Theme", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = {}) { Text("Primary Button") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
        }
    }
}
```

- [ ] **Step 7: 跑验证 + commit**

```bash
./gradlew :app:assembleDebug
grep -rn '@Preview' app/src/main/java | wc -l
```

期望:`@Preview` 计数从 0 增到 ≥ 5。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/ColorPreview.kt app/src/main/java/com/textvision/alistclient/ui/theme/TypePreview.kt app/src/main/java/com/textvision/alistclient/ui/theme/ShapePreview.kt app/src/main/java/com/textvision/alistclient/ui/theme/MotionPreview.kt app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreview.kt
git commit -m "feat(theme): 5 theme @Preview composables (Color/Type/Shape/Motion/Theme)"
```

---

### Task 7: P0-4 补 25+ @Preview - 通用组件 (1/2)

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/ComponentsPreview.kt`

- [ ] **Step 1: 创建 ComponentsPreview.kt**

(12 个组件各 1 个 @Preview,放在单文件用 `@Preview(name=...)` 区分;FileTypeIcon 单列 1 张网格,共 13 张)

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.theme.AlistClientTheme

@Preview(name = "AppScaffold", showBackground = true)
@Composable private fun AppScaffoldPreview() { AlistClientTheme { AppScaffold { Text("Content") } } }

@Preview(name = "AppTopBar", showBackground = true)
@Composable private fun AppTopBarPreview() { AlistClientTheme { AppTopBar(title = "标题", subtitle = "副标题", onBack = {}, actions = { Text("Action") }) } }

@Preview(name = "AppBottomNavBar", showBackground = true)
@Composable private fun AppBottomNavBarPreview() { AlistClientTheme { AppBottomNavBar(navController = androidx.navigation.compose.rememberNavController()) } }

@Preview(name = "StatusBanner (all kinds)", showBackground = true)
@Composable private fun StatusBannerPreview() {
    AlistClientTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            StatusBanner(message = "Info 提示", kind = StatusBannerKind.Info)
            StatusBanner(message = "Warning 警告", kind = StatusBannerKind.Warning)
            StatusBanner(message = "Error 错误", kind = StatusBannerKind.Error)
            StatusBanner(message = "Success 成功", kind = StatusBannerKind.Success)
        }
    }
}

@Preview(name = "AppAlertDialog", showBackground = true)
@Composable private fun AppAlertDialogPreview() { AlistClientTheme { AppAlertDialog(title = "标题", text = "正文", confirmLabel = "确定", onConfirm = {}, onDismiss = {}) } }

@Preview(name = "EmptyState", showBackground = true)
@Composable private fun EmptyStatePreview() { AlistClientTheme { EmptyState(title = "空空如也", description = "暂无数据") } }

@Preview(name = "ErrorState", showBackground = true)
@Composable private fun ErrorStatePreview() { AlistClientTheme { ErrorState(message = "出错了", onRetry = {}) } }

@Preview(name = "LoadingState", showBackground = true)
@Composable private fun LoadingStatePreview() { AlistClientTheme { LoadingState(message = "加载中") } }

@Preview(name = "ListItemRow", showBackground = true)
@Composable private fun ListItemRowPreview() { AlistClientTheme { ListItemRow(title = "文档.txt", subtitle = "12.3 MB", onClick = {}) } }

@Preview(name = "ActionButton (all variants)", showBackground = true)
@Composable private fun ActionButtonPreview() {
    AlistClientTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            ActionButton(text = "Primary", onClick = {}, variant = ActionButtonVariant.Primary)
            ActionButton(text = "Secondary", onClick = {}, variant = ActionButtonVariant.Secondary)
            ActionButton(text = "Tonal", onClick = {}, variant = ActionButtonVariant.Tonal)
        }
    }
}

@Preview(name = "SearchField", showBackground = true)
@Composable private fun SearchFieldPreview() { AlistClientTheme { SearchField(value = "", onValueChange = {}, placeholder = "搜索...") } }

@Preview(name = "FileTypeIcon (6 types)", showBackground = true)
@Composable private fun FileTypeIconPreview() {
    AlistClientTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            FileTypeIcon(type = FileType.Folder)
            FileTypeIcon(type = FileType.Pdf)
            FileTypeIcon(type = FileType.Image)
            FileTypeIcon(type = FileType.Audio)
            FileTypeIcon(type = FileType.Video)
            FileTypeIcon(type = FileType.Other)
        }
    }
}
```

- [ ] **Step 2: 跑验证 + commit**

```bash
./gradlew :app:assembleDebug
grep -rn '@Preview' app/src/main/java | wc -l
```

期望:计数 ≥ 5 + 13 = 18。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/ComponentsPreview.kt
git commit -m "feat(ui): 13 component @Preview composables"
```

---

### Task 8: P0-4 补 25+ @Preview - 通用组件 (2/2) + 屏幕 shell

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/ComponentsPreview.kt` (加 1 张 FileTypeIcon 网格)
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreenPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreenPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreenPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreenPreview.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreenPreview.kt`

- [ ] **Step 1: 在 ComponentsPreview.kt 加 FileTypeIcon 网格 + dark 变体**

```kotlin
@Preview(name = "FileTypeIcon Grid (6x)", showBackground = true)
@Composable private fun FileTypeIconGridPreview() {
    AlistClientTheme {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FileTypeIcon(FileType.Folder); FileTypeIcon(FileType.Pdf); FileTypeIcon(FileType.Image) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FileTypeIcon(FileType.Audio); FileTypeIcon(FileType.Video); FileTypeIcon(FileType.Other) }
        }
    }
}

@Preview(name = "AppScaffold Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable private fun AppScaffoldDarkPreview() { AlistClientTheme(darkTheme = true) { AppScaffold { Text("Dark Content") } } }

@Preview(name = "SettingsScreen-like Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable private fun SettingsDarkPreview() { AlistClientTheme(darkTheme = true) { Surface { Text("Settings placeholder", modifier = Modifier.padding(16.dp)) } } }
```

(凑足 25)

- [ ] **Step 2: 创建 5 个屏幕 shell Preview**

每个文件 1 个 @Preview,展示空/loading 状态:

```kotlin
// LoginScreenPreview.kt
@Preview(name = "LoginScreen (empty)", showBackground = true)
@Composable private fun LoginScreenPreview() { AlistClientTheme { LoginScreen(onLoginSuccess = {}) } }

// FileScreenPreview.kt
@Preview(name = "FileScreen (empty)", showBackground = true)
@Composable private fun FileScreenPreview() {
    AlistClientTheme {
        // 用 mock ViewModel 或留空 placeholder
        Surface { Text("FileScreen placeholder", modifier = Modifier.padding(16.dp)) }
    }
}

// HomeScreenPreview.kt
@Preview(name = "HomeScreen (loading)", showBackground = true)
@Composable private fun HomeScreenPreview() {
    AlistClientTheme { Surface { LoadingState("加载仪表盘") } }
}

// SettingsScreenPreview.kt
@Preview(name = "SettingsScreen", showBackground = true)
@Composable private fun SettingsScreenPreview() {
    AlistClientTheme { Surface { Text("Settings placeholder", modifier = Modifier.padding(16.dp)) } }
}

// TransferScreenPreview.kt
@Preview(name = "TransferScreen (empty)", showBackground = true)
@Composable private fun TransferScreenPreview() {
    AlistClientTheme { Surface { EmptyState("暂无任务", "下载/上传记录会显示在这里") } }
}
```

- [ ] **Step 3: 跑验证 + commit**

```bash
./gradlew :app:assembleDebug
grep -rn '@Preview' app/src/main/java | wc -l
```

期望:计数 ≥ 25。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/ComponentsPreview.kt app/src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreenPreview.kt app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreenPreview.kt app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreenPreview.kt app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreenPreview.kt app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreenPreview.kt
git commit -m "feat(ui): complete 25+ @Preview coverage (themes + components + screens)"
```

---

### Task 9: P1-5a SessionEvent + AuthRepository 事件流

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/auth/SessionEvent.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/auth/AuthRepository.kt`
- Test: `app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt`

- [ ] **Step 1: 创建 SessionEvent.kt**

```kotlin
package com.textvision.alistclient.auth

sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
    data object NetworkDown : SessionEvent
}
```

- [ ] **Step 2: 改 AuthRepository.kt**

加:
```kotlin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

suspend fun emitSessionEvent(event: SessionEvent) {
    _sessionEvents.emit(event)
}
```

- [ ] **Step 3: 加失败测试**

```kotlin
@Test
fun emit_session_event_sends_to_flow() = runTest {
    val repo = AuthRepository(/* mocks */)
    val events = mutableListOf<SessionEvent>()
    val job = launch { repo.sessionEvents.toList(events) }
    repo.emitSessionEvent(SessionEvent.Unauthorized)
    advanceUntilIdle()
    assertEquals(listOf(SessionEvent.Unauthorized), events)
    job.cancel()
}
```

(具体 mock 注入路径以现有 AuthRepositoryTest 风格为准)

- [ ] **Step 4: 跑测试确认失败 → 实现 → 通过**

- [ ] **Step 5: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/auth/SessionEvent.kt app/src/main/java/com/textvision/alistclient/auth/AuthRepository.kt app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt
git commit -m "feat(auth): SessionEvent flow + AuthRepository emit"
```

---

### Task 10: P1-5b AuthInterceptor 401 触发 emit

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/network/AuthInterceptor.kt`
- Test: `app/src/test/java/com/textvision/alistclient/network/AuthInterceptorTest.kt`

- [ ] **Step 1: 改 AuthInterceptor**

注入 `AuthRepository`,在 `intercept` 检测到 401:

```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)
    if (response.code == 401 && !request.headers.names().contains(SkipAuthRetry.HEADER)) {
        runBlocking { authRepository.emitSessionEvent(SessionEvent.Unauthorized) }
    }
    return response
}
```

- [ ] **Step 2: 加失败测试**

```kotlin
@Test
fun `401 response emits Unauthorized event`() = runTest {
    val mockRepo = mockk<AuthRepository>(relaxed = true)
    val server = MockWebServer().apply { enqueue(MockResponse().setResponseCode(401)) }
    server.start()
    val okHttp = OkHttpClient.Builder().addInterceptor(AuthInterceptor(mockRepo, /* tokenProvider */)).build()
    okHttp.newCall(Request.Builder().url(server.url("/")).build()).execute()
    verify { runBlocking { mockRepo.emitSessionEvent(SessionEvent.Unauthorized) } }
    server.shutdown()
}
```

- [ ] **Step 3: 跑测试确认失败 → 实现 → 通过**

- [ ] **Step 4: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/network/AuthInterceptor.kt app/src/test/java/com/textvision/alistclient/network/AuthInterceptorTest.kt
git commit -m "feat(network): AuthInterceptor emits SessionEvent.Unauthorized on 401"
```

---

### Task 11: P1-5c SessionGate + MainActivity 接入

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/auth/SessionGate.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/MainActivity.kt`
- Test: `app/src/test/java/com/textvision/alistclient/auth/SessionGateTest.kt`

- [ ] **Step 1: 创建 SessionGate.kt**

```kotlin
package com.textvision.alistclient.auth

import com.textvision.alistclient.navigation.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionGate @Inject constructor(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val _navEvent = MutableSharedFlow<AppRoute>(extraBufferCapacity = 4)
    val navEvent: SharedFlow<AppRoute> = _navEvent.asSharedFlow()

    private val _isNetworkDown = MutableStateFlow(false)
    val isNetworkDown: StateFlow<Boolean> = _isNetworkDown.asStateFlow()

    init {
        scope.launch {
            authRepository.sessionEvents.collect { event ->
                when (event) {
                    SessionEvent.Unauthorized -> {
                        authRepository.logout()
                        _navEvent.emit(AppRoute.Login)
                    }
                    SessionEvent.NetworkDown -> _isNetworkDown.value = true
                }
            }
        }
    }
}
```

- 需在 `di/AppModule.kt` 提供 `@IoDispatcher` 对应 `CoroutineScope` (若已有直接用)

- [ ] **Step 2: 改 MainActivity.kt 注入 SessionGate + LaunchedEffect**

```kotlin
@Inject lateinit var sessionGate: SessionGate

// setContent 内:
CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
    AlistClientTheme(darkTheme = darkTheme) {
        LaunchedEffect(Unit) {
            sessionGate.navEvent.collect { route ->
                when (route) {
                    AppRoute.Login -> navController.navigate(LoginDest) { popUpTo(0) { inclusive = true } }
                    else -> {}
                }
            }
        }
        AppNavHost(startAuthenticated = authRepository.loadSavedSession() != null, snackbarHostState = snackbarHostState)
    }
}
```

- `AppRoute.Login` 需在 navigation 包内定义 (若已 LoginDest,直接用)

- [ ] **Step 3: 加 SessionGateTest**

```kotlin
@Test
fun `unauthorized event clears session and emits Login nav`() = runTest {
    val authRepo = mockk<AuthRepository>(relaxed = true)
    val gate = SessionGate(authRepo, this)
    authRepo.emitSessionEvent(SessionEvent.Unauthorized)
    advanceUntilIdle()
    verify { authRepo.logout() }
    assertEquals(AppRoute.Login, gate.navEvent.replayCache.last())
}
```

- [ ] **Step 4: 跑测试确认失败 → 实现 → 通过**

- [ ] **Step 5: 模拟器验证 + commit**

```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:登录成功后,改 token 模拟 401,自动跳回 Login 页。

```bash
git add app/src/main/java/com/textvision/alistclient/auth/SessionGate.kt app/src/main/java/com/textvision/alistclient/MainActivity.kt app/src/test/java/com/textvision/alistclient/auth/SessionGateTest.kt
git commit -m "feat(auth): SessionGate consumes events, MainActivity navigates to Login on Unauthorized"
```

---

### Task 12: P1-6a Cloud* 替换 - admin/storage + admin/cookie + admin/form

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageRowItem.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/admin/cookie/WebCookieDialog.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt`

- [ ] **Step 1: 跑 gitnexus_impact 确认 blast radius**

```
用 mcp__gitnexus__impact,target=CloudListItem,direction=upstream
用 mcp__gitnexus__impact,target=CloudPrimary,direction=upstream
```

期望:仅这 3 文件用,Risk = LOW。

- [ ] **Step 2: 改 StorageRowItem.kt**

```kotlin
// 原: CloudListItem(...)
// 改: ListItemRow(
ListItemRow(
    title = storage.name,
    subtitle = storage.driver,
    onClick = { onClick(storage) },
    leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
)
```

- 删 `import com.textvision.alistclient.ui.components.CloudListItem`
- 删 `import com.textvision.alistclient.ui.theme.CloudPrimary`

- [ ] **Step 3: 改 WebCookieDialog.kt**

把 4 处 `CloudBackground/Surface/TextPrimary/Primary` 替换为 `MaterialTheme.colorScheme.{background,surface,onSurface,primary}`,删 import。

- [ ] **Step 4: 改 DynamicFormField.kt**

`CloudSurfaceMuted` → `MaterialTheme.colorScheme.surfaceContainerHigh`,删 import。

- [ ] **Step 5: 跑验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/admin/storage/StorageRowItem.kt app/src/main/java/com/textvision/alistclient/admin/cookie/WebCookieDialog.kt app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt
git commit -m "refactor(admin): replace Cloud* tokens with MaterialTheme.colorScheme"
```

---

### Task 13: P1-6b Cloud* 替换 - home + DirectoryBrowser

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt`

- [ ] **Step 1: 改 HomeScreen.kt**

`CloudScaffold` → `AppScaffold`,`CloudTopBar` → `AppTopBar`,删相关 import。

(若 HomeScreen 已用 AppScaffold,跳过此步骤)

- [ ] **Step 2: 重写 DirectoryBrowser.kt 为 LazyColumn + ListItemRow + Breadcrumb**

```kotlin
@Composable
fun DirectoryBrowser(
    initialPath: String,
    onPathSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    val viewModel: DirectoryBrowserViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "选择目标目录",
                subtitle = currentPath,
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Breadcrumb(path = currentPath, onSegmentClick = { seg -> currentPath = seg })
            LazyColumn {
                items(state.entries) { entry ->
                    ListItemRow(
                        title = entry.name,
                        subtitle = FileSizeFormatter.humanize(entry.size),
                        onClick = {
                            val next = "$currentPath/${entry.name}"
                            if (entry.isDir) currentPath = next
                            else onPathSelected(next)
                        },
                        leading = { FileTypeIcon(if (entry.isDir) FileType.Folder else FileType.Other) },
                    )
                }
            }
        }
    }
}
```

(具体 DirectoryBrowserViewModel 状态结构以源码为准;若已存在 ViewModel,沿用)

- [ ] **Step 3: 跑验证 + 模拟器验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:Storage 设置中点 storage → Edit 屏 → Picker (选择目标目录) 可正常浏览,视觉无回归。

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt
git commit -m "refactor(ui): HomeScreen+DirectoryBrowser to AppScaffold+ListItemRow"
```

---

### Task 14: P1-7 PreviewScreen 切 AppScaffold

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewAudio.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewFallback.kt`

- [ ] **Step 1: 改 PreviewScreen.kt**

- `CloudScaffold` → `AppScaffold`
- `CloudTopBar` → `AppTopBar`
- `CloudCard` → `Card(modifier, shape = AppShapes.Card)` 或 `Surface`
- 删 4 个 `import com.textvision.alistclient.ui.components.Cloud*`

- [ ] **Step 2: 改 PreviewAudio.kt + PreviewFallback.kt**

`CloudPrimary/PrimarySoft/SurfaceMuted/TextPrimary` → `MaterialTheme.colorScheme.{primary,primaryContainer,surfaceContainerHigh,onSurface}`,删 import。

- [ ] **Step 3: 跑验证 + 模拟器验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:打开图片/PDF/视频/音频预览,顶栏主题色与 Settings 一致,切换暗黑模式无锁死。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/preview/
git commit -m "refactor(preview): PreviewScreen+Audio+Fallback to AppScaffold + colorScheme"
```

---

### Task 15: P1-6c Cloud*.kt 11 个文件 + Color.kt 5 alias 删除

**Files:**
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudAlertDialog.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudListItem.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudScaffold.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudSearchBar.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudStatusBanner.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudTopBar.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudEmptyState.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudRoundIconButton.kt`
- Delete: `app/src/main/java/com/textvision/alistclient/ui/components/CloudPillButton.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt` (删 5 alias)

- [ ] **Step 1: 跑 grep 确认 0 引用**

```bash
grep -rn 'Cloud\(Card\|ActionButton\|AlertDialog\|ListItem\|Scaffold\|SearchBar\|StatusBanner\|TopBar\|EmptyState\|RoundIconButton\|PillButton\|Primary\|Background\|Surface\|SurfaceMuted\|TextPrimary\)' app/src/main/java
```

期望:仅匹配 `@Deprecated` 注解行和 Cloud*.kt 文件内部。

- [ ] **Step 2: 删 11 个文件 + Color.kt 5 alias**

```bash
rm app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt
rm app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt
# ... 其他 9 个
```

Color.kt:删 `val CloudPrimary/CloudBackground/CloudSurface/CloudSurfaceMuted/CloudTextPrimary` 5 行 (含 `@Deprecated` 注解)。

- [ ] **Step 3: 跑验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add -A app/src/main/java/com/textvision/alistclient/ui/components/ app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt
git commit -m "refactor(ui): delete 11 Cloud* files + 5 @Deprecated color aliases"
```

---

### Task 16: P1-8 Roborazzi 补到 15+ baseline

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/ui/feature/file/FileScreenSnapshotTest.kt` (加 3 张)
- Create: `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeScreenSnapshotTest.kt` (新建 2 张)
- Create: `app/src/test/java/com/textvision/alistclient/ui/feature/settings/SettingsScreenSnapshotTest.kt` (新建 2 张)
- Create: `app/src/test/java/com/textvision/alistclient/ui/feature/transfer/TransferScreenSnapshotTest.kt` (新建 2 张)

- [ ] **Step 1: 扩 FileScreenSnapshotTest 加 3 张**

参考现有 baseline (empty/success/dark),新增:
- `FileScreen loading` (mock state.loading=true)
- `FileScreen error` (mock state.error="加载失败")
- `FileScreen selection mode` (mock state.selectedIds=setOf(0,1))

```kotlin
@Test
fun fileScreen_loading() = runRoborazziTest { composeRule.setContent { /* ... FileScreen with loading state ... */ } }
@Test
fun fileScreen_error() = runRoborazziTest { ... }
@Test
fun fileScreen_selectionMode() = runRoborazziTest { ... }
```

- [ ] **Step 2: 新建 HomeScreenSnapshotTest 加 2 张**

- `homeScreen_empty` + `homeScreen_loaded`

- [ ] **Step 3: 新建 SettingsScreenSnapshotTest 加 2 张**

- `settingsScreen_light` + `settingsScreen_dark`

- [ ] **Step 4: 新建 TransferScreenSnapshotTest 加 2 张**

- `transferScreen_empty` + `transferScreen_withTasks`

- [ ] **Step 5: 跑 Roborazzi 记录**

```bash
./gradlew :app:recordRoborazziDebug
ls app/src/test/snapshots/images/
```

期望:≥ 15 张 PNG。

- [ ] **Step 6: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/test/
git commit -m "test(roborazzi): add 9 baselines (FileScreen×3, Home×2, Settings×2, Transfer×2)"
```

---

### Task 17: P1-9 Spring 动效扩展

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt` (Tab AnimatedContent)
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt` (ListItemRow AnimatedVisibility)
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/AppBottomNavBar.kt` (选中 indicator spring)

- [ ] **Step 1: 改 TransferScreen Tab 切换**

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        (slideInHorizontally { it / 4 } + fadeIn() with slideOutHorizontally { -it / 4 } + fadeOut())
            .using(HyperOsMotion.SpringMedium)
    },
    label = "tab-switch",
) { tab -> /* render tab content */ }
```

- [ ] **Step 2: 改 FileScreen ListItemRow**

```kotlin
AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = HyperOsMotion.SpringFast) + expandVertically(animationSpec = HyperOsMotion.SpringFast),
    exit = fadeOut(animationSpec = HyperOsMotion.SpringFast) + shrinkVertically(animationSpec = HyperOsMotion.SpringFast),
) {
    ListItemRow(...)
}
```

(若 LazyColumn 已在用,不强行包 AnimatedVisibility,跳过)

- [ ] **Step 3: 改 AppBottomNavBar 选中指示器**

```kotlin
val indicatorColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    animationSpec = HyperOsMotion.SpringFast,
    label = "indicator",
)
```

- [ ] **Step 4: 跑验证 + 模拟器验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:Tab 切换流畅无突变,BottomBar 选中指示有渐变。

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt app/src/main/java/com/textvision/alistclient/ui/components/AppBottomNavBar.kt
git commit -m "feat(motion): Spring animations on Tab switch + indicator"
```

---

### Task 18: P1-10 TransferViewModel + SettingsViewModel 测试恢复

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/transfer/TransferViewModelTest.kt`
- Modify: `app/src/test/java/com/textvision/alistclient/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: TransferViewModelTest 加 3 测试**

```kotlin
@Test
fun `refresh triggers reload`() = runTest {
    val mockDao = mockk<TransferDao>(relaxed = true)
    coEvery { mockDao.getSnapshot() } returns listOf(task1, task2)
    val vm = TransferViewModel(mockDao, /* ... */)
    vm.refresh()
    advanceUntilIdle()
    coVerify { mockDao.getSnapshot() }
}

@Test
fun `filter ACTIVE returns only running`() = runTest {
    // mock 3 tasks: 1 RUNNING, 1 SUCCEEDED, 1 FAILED
    // emit, set filter=ACTIVE, expect 1 result
}

@Test
fun `retry transitions FAILED to RUNNING`() = runTest {
    // mock failed task, call retry(), verify state
}
```

- [ ] **Step 2: SettingsViewModelTest 加 3 测试**

```kotlin
@Test
fun `toggle dark mode updates repository`() = runTest { ... }

@Test
fun `loadAdminDataPopulates state`() = runTest {
    // mock admin API success, verify state.adminData populated
}

@Test
fun `toggleStorageFailureSurfaces error state`() = runTest {
    // mock admin storage API failure, verify state.error populated
}
```

- [ ] **Step 3: 跑测试确认失败 → 实现 → 通过**

- [ ] **Step 4: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/test/java/com/textvision/alistclient/transfer/TransferViewModelTest.kt app/src/test/java/com/textvision/alistclient/settings/SettingsViewModelTest.kt
git commit -m "test: restore TransferViewModel + SettingsViewModel coverage (6 tests)"
```

---

### Task 19: P2-11 HomeScreen 包路径迁移

**Files:**
- Move: `app/src/main/java/com/textvision/alistclient/home/*.kt` → `app/src/main/java/com/textvision/alistclient/ui/feature/home/`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt` (改 import)

- [ ] **Step 1: 跑 gitnexus_impact 确认 blast radius**

```
用 mcp__gitnexus__impact,target=HomeScreen,direction=upstream
```

期望:Risk = LOW (仅 AppNavHost + 测试 import)。

- [ ] **Step 2: 用 IDE / git mv 移动文件 + 改 package**

```bash
mkdir -p app/src/main/java/com/textvision/alistclient/ui/feature/home
git mv app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt app/src/main/java/com/textvision/alistclient/ui/feature/home/
git mv app/src/main/java/com/textvision/alistclient/home/HomeSections.kt app/src/main/java/com/textvision/alistclient/ui/feature/home/
git mv app/src/main/java/com/textvision/alistclient/home/HomeStorageSection.kt app/src/main/java/com/textvision/alistclient/ui/feature/home/
git mv app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt app/src/main/java/com/textvision/alistclient/ui/feature/home/
```

(具体文件以 `ls app/src/main/java/com/textvision/alistclient/home/` 为准)

每个文件改首行 `package com.textvision.alistclient.home` → `package com.textvision.alistclient.ui.feature.home`。

- [ ] **Step 3: 改 AppNavHost.kt import**

`import com.textvision.alistclient.home.HomeScreen` → `import com.textvision.alistclient.ui.feature.home.HomeScreen`

- [ ] **Step 4: 跑 grep 确认 0 残留**

```bash
grep -rn 'com.textvision.alistclient.home' app/src
```

期望:0 匹配。

- [ ] **Step 5: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add -A app/src/main/java/com/textvision/alistclient/home/ app/src/main/java/com/textvision/alistclient/ui/feature/home/ app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
git commit -m "refactor(home): migrate HomeScreen package to ui/feature/home"
```

---

### Task 20: P2-12 SettingsViewModel 改 combine + stateIn

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/settings/SettingsViewModel.kt`
- Test: `app/src/test/java/com/textvision/alistclient/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: 跑 gitnexus_impact**

```
用 mcp__gitnexus__impact,target=SettingsViewModel,direction=upstream
```

- [ ] **Step 2: 改 SettingsViewModel 用 combine + stateIn**

参考 FileViewModel 的写法 (Task 17 已有):

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    data class UiState(
        val darkMode: DarkMode = DarkMode.SYSTEM,
        val adminData: AdminData? = null,
        val error: String? = null,
        val isLoading: Boolean = false,
    )

    val uiState: StateFlow<UiState> = combine(
        themeRepository.darkMode,
        adminRepository.adminData,
        adminRepository.error,
    ) { darkMode, adminData, error ->
        UiState(darkMode = darkMode, adminData = adminData, error = error)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun onIntent(intent: Intent) { ... }
}
```

(具体 field 名称以现有 SettingsViewModel 为准;保留 `fun toggleDarkMode` 等 public API)

- [ ] **Step 3: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/settings/SettingsViewModel.kt
git commit -m "refactor(vm): SettingsViewModel to combine+stateIn UDF"
```

---

### Task 21: P2-13 isAllSelected 用 visibleFiles

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt`
- Test: `app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt`

- [ ] **Step 1: 加失败测试**

```kotlin
@Test
fun `selectAll with filter applied only toggles visible files`() = runTest {
    val vm = FileViewModel(/* mocks */, files = listOf(fileA, fileB, fileC))
    vm.setFilter("B") // 只 fileB 可见
    vm.onSelectAll(true)
    advanceUntilIdle()
    assertTrue(fileB in vm.state.value.selectedIds)
    assertFalse(fileA in vm.state.value.selectedIds)
    assertFalse(fileC in vm.state.value.selectedIds)
}
```

- [ ] **Step 2: 改 FileViewModel**

把 `isAllSelected` 计算属性与 `onSelectAll` 实现改用 `visibleFiles` (即应用 filter 后的列表) 而非 `files`。

- [ ] **Step 3: 跑测试确认失败 → 改 → 通过**

- [ ] **Step 4: 跑全量验证 + 模拟器验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:输入过滤词 → 全选 → 仅勾选过滤后可见文件。

```bash
git add app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt
git commit -m "fix(file): isAllSelected + selectAll use visibleFiles (filter-aware)"
```

---

### Task 22: P2-14 ActiveTransferStatuses 去重

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferStatus.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt`

- [ ] **Step 1: 跑 gitnexus_impact**

```
用 mcp__gitnexus__impact,target=ActiveTransferStatuses,direction=upstream
```

- [ ] **Step 2: 在 TransferStatus.kt 加 activeStatuses**

```kotlin
companion object {
    val activeStatuses: Set<TransferStatus> = setOf(Pending, Running, Retrying)
}
```

- [ ] **Step 3: 改 TransferScreen.kt 删本地常量**

删 `private val ActiveTransferStatuses = setOf(...)`,改用 `TransferStatus.activeStatuses`。

- [ ] **Step 4: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
grep -rn 'ActiveTransferStatuses' app/src
git add app/src/main/java/com/textvision/alistclient/transfer/TransferStatus.kt app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt
git commit -m "refactor(transfer): TransferStatus.activeStatuses single source of truth"
```

---

### Task 23: P2-15 AppDestination.mime 重命名

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppDestination.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`

- [ ] **Step 1: 跑 gitnexus_impact**

```
用 mcp__gitnexus__impact,target=PreviewDestArgs,direction=upstream
```

- [ ] **Step 2: 改 AppDestination.kt 字段名**

```kotlin
// 原: data class PreviewDestArgs(val name: String, val path: String, val mime: String, val downloadUrl: String, val size: Long)
// 改:
@Serializable
data class PreviewDestArgs(
    val name: String,
    val path: String,
    val fileTypeName: String,  // 原 mime
    val downloadUrl: String,
    val size: Long,
)
```

- [ ] **Step 3: 改 AppNavHost.kt**

- 路由构造处:`PreviewDestArgs(mime = item.type.name, ...)` → `PreviewDestArgs(fileTypeName = item.type.name, ...)`
- 解析处:`FileType.valueOf(args.mime)` → `FileType.valueOf(args.fileTypeName)`

- [ ] **Step 4: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/navigation/AppDestination.kt app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
git commit -m "refactor(nav): PreviewDestArgs.mime → fileTypeName"
```

---

### Task 24: P2-16 PreviewViewModel.textRepository 私有化

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewViewModel.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/feature/preview/PreviewViewModelTest.kt`

- [ ] **Step 1: 改 PreviewViewModel**

```kotlin
@HiltViewModel
class PreviewViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val textRepository: PreviewTextRepository,  // 改 private
) : ViewModel() { ... }

// 工厂方法供测试:
companion object {
    fun forTest(
        textRepository: PreviewTextRepository,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    ): PreviewViewModel = PreviewViewModel(ioDispatcher, textRepository)
}
```

- [ ] **Step 2: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewViewModel.kt
git commit -m "refactor(preview): PreviewViewModel.textRepository private + forTest factory"
```

---

### Task 25: P2-17 ModifierParameter / ObsoleteSdkInt 清理

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferNotificationController.kt`
- Modify: 各含 `ModifierParameter` warning 的 Composable 文件
- Modify: `app/lint-baseline.xml` (清对应行)

- [ ] **Step 1: 跑 lint 看当前 warning 列表**

```bash
./gradlew :app:lintDebug
grep -n 'ModifierParameter' app/lint-baseline.xml
```

- [ ] **Step 2: 改 TransferNotificationController.kt 删 SDK 检查块**

```kotlin
// 原:
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { ... }
// 删整段 (minSdk=26, O=26)
```

- [ ] **Step 3: 对每条 ModifierParameter 决定 @Suppress 或默认 modifier**

两种修法:
- (a) `@Suppress("ModifierParameter") fun Foo(modifier: Modifier)`
- (b) `fun Foo(modifier: Modifier = Modifier)`

优先 (b),仅当 (b) 影响 API 时用 (a)。

- [ ] **Step 4: 跑 lint + 清 baseline**

```bash
./gradlew :app:lintDebug
# 删 baseline.xml 中已修的对应行
wc -l app/lint-baseline.xml
```

期望:baseline 缩至少 5 行,lint 无新 warning。

- [ ] **Step 5: commit**

```bash
git add app/src/main/java/com/textvision/alistclient/transfer/TransferNotificationController.kt app/lint-baseline.xml <其他含 ModifierParameter 的文件>
git commit -m "chore(lint): clear ModifierParameter + ObsoleteSdkInt warnings"
```

---

### Task 26: P2-18 TransferManager.kt:333 NewApi 处理

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt`

- [ ] **Step 1: 读 TransferManager.kt:333 周边代码**

确认 `@RequiresApi(Build.VERSION_CODES.O)` 注解位置与上下文。

- [ ] **Step 2: 删 @RequiresApi 注解 + 检查代码块**

- 删 `@RequiresApi(Build.VERSION_CODES.O)`
- 确认 `if (SDK_INT >= O)` 分支与 else 分支 (若代码块在 minSdk=26 下永远走主分支,删 else)

- [ ] **Step 3: 跑全量验证 + commit**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
git add app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt
git commit -m "chore(transfer): remove obsolete @RequiresApi(O) on minSdk=26"
```

---

### Task 27: Final 全量验证 + progress.md 收尾

- [ ] **Step 1: 跑全量验证 + 模拟器 + 视觉回归**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
./gradlew :app:verifyRoborazziDebug
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

期望:全绿 + 15+ Roborazzi baseline 通过 + 5 屏 (Login/Files/Transfers/Settings/Home) 视觉无回归。

- [ ] **Step 2: 更新 progress.md 收尾**

在 `.superpowers/sdd/progress.md` 追加 Final-REVIEW 块:

```
## Phase 5 — UI Expressive 后续收尾
Task 33: complete (...) P0-1 Snackbar+BottomBar 重叠修复
Task 34: complete (...) P0-2 FileScreen lost features
Task 35: complete (...) P0-3 字节数 humanize
Task 36: complete (...) P0-4 25+ @Preview
Task 37: complete (...) P1-5 AppError 链路
Task 38: complete (...) P1-6 Cloud* 替换
Task 39: complete (...) P1-7 PreviewScreen AppScaffold
Task 40: complete (...) P1-8 Roborazzi 15+ baseline
Task 41: complete (...) P1-9 Spring 动效扩展
Task 42: complete (...) P1-10 测试恢复
Task 43: complete (...) P2-11 HomeScreen 包迁移
Task 44: complete (...) P2-12 VM 状态架构统一
Task 45: complete (...) P2-13 isAllSelected visibleFiles
Task 46: complete (...) P2-14 ActiveTransferStatuses 去重
Task 47: complete (...) P2-15 AppDestination.mime 重命名
Task 48: complete (...) P2-16 PreviewViewModel.textRepository 私有化
Task 49: complete (...) P2-17 ModifierParameter/ObsoleteSdkInt
Task 50: complete (...) P2-18 TransferManager NewApi

## Plan 完成 - 19 Tasks (含 6a/6b 拆)
## 18 项遗留问题清零
```

- [ ] **Step 3: commit progress.md**

```bash
git add .superpowers/sdd/progress.md
git commit -m "docs(progress): Phase 5 followups complete (19 tasks, 18 items cleared)"
```

---

## Self-Review Checklist

- [x] **Spec coverage**: §1-§18 各对应至少 1 个 task
- [x] **P1-6 拆 6a/6b**:Task 12/13/14 (6a 替换) + Task 15 (6b 删除) — 4 个 task 覆盖 §6
- [x] **Placeholder scan**: 全部 step 含实际代码/命令,无 TBD
- [x] **Type consistency**:
  - `SnackbarHostState` (Task 1) 非空,MainActivity 调用一致
  - `ListItemRow.onMoreClick` (Task 2) 可空参数,Task 4 调用一致
  - `SessionGate.navEvent` (Task 11) 与 `AppRoute.Login` 引用一致
  - `TransferStatus.activeStatuses` (Task 22) 引用一致
  - `PreviewDestArgs.fileTypeName` (Task 23) 改后引用一致
- [x] **TDD**: 每个 task 都是"失败测试 → 最小实现 → 通过" 模式
- [x] **Frequent commits**: 27 个 commit 节点(19 task + 8 progress 节点)
- [x] **Global constraints**: 每 task 隐含 minSdk 26 / ≤400 行 / `collectAsStateWithLifecycle` / gitnexus impact
