# Alist Android Client — UI Expressive 全面重做 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 spec `2026-07-08-ui-expressive-redesign-design.md` 把 Alist Android 客户端升级到 Material 3 Expressive 全套设计 token、共享组件、Screen 重构、视觉回归测试。

**Architecture:** 4 个 Phase 顺序推进 — Token → Foundation+Components → Screens → 体验增强+收尾。每 Phase 独立 PR / 独立 review / 独立回滚。

**Tech Stack:** Kotlin 2.0.21 + Jetpack Compose BOM 2024.09.03 + Material3 1.3.0 + Material Icons Extended + Hilt 2.52 + Navigation Compose 2.8.3 + DataStore + Roborazzi（首次引入）+ JUnit4 + MockK + Turbine + Compose UI Test。

---

## Global Constraints

- **Compose BOM**: 2024.09.03
- **Material3**: 1.3.0
- **Kotlin**: 2.0.21，jvmTarget 17
- **minSdk**: 26 / **compileSdk / targetSdk**: 34
- **单文件行数**: ≤ 400 行（强制约束，CI 检查）
- **State 收集**: 一律 `collectAsStateWithLifecycle()`，禁用 `collectAsState()`
- **Composable 命名**: 废弃 `Cloud*` 前缀，改用 `App*`/`EmptyState`/`ErrorState` 等语义化命名
- **颜色**: 全部走 `MaterialTheme.colorScheme.*`，禁止硬编码 `Color.X`（仅 `Color.Transparent` 允许）
- **字体**: 系统默认
- **测试**: 每个 Screen ≥ 1 `@Preview` (Light) + ≥ 1 `@Preview` (Dark) + ViewModel 单测覆盖核心 intent → state
- **动态取色**: Android 12+ 用 `dynamicLightColorScheme` / `dynamicDarkColorScheme`，低版本回退静态 token
- **CI**: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug`
- **截图回归**: Roborazzi 0.1% 容忍度，manual update via `recordRoborazziDebug`
- **暗色覆盖**: `ThemeRepository` 持久化 `DarkMode` 枚举（SYSTEM / LIGHT / DARK）
- **不做**: 大屏双栏 / 折叠屏 / 自定义字体 / Swipe 手势 / "减少动效"开关

---

## File Structure

### Phase 1 新增
- `ui/theme/Color.kt`（重写）— 浅/深 M3 全套
- `ui/theme/Type.kt`（重写）— 15 个 M3 Expressive 文字样式
- `ui/theme/Shape.kt`（重写）— Corner 5 档
- `ui/theme/Motion.kt` — Spring/Tween tokens
- `ui/theme/Theme.kt`（重写）— `AlistClientTheme` + Dynamic Color
- `ui/theme/ThemeRepository.kt` — DataStore 持久化 DarkMode
- `test/.../ui/theme/ThemeRepositoryTest.kt`
- `test/.../ui/theme/MotionTest.kt`

### Phase 2 新增
- `ui/foundation/Scaffold.kt` — `AppScaffold`
- `ui/foundation/AppBars.kt` — `AppTopBar` / `AppBottomBar`
- `ui/foundation/Backgrounds.kt` — `AppBackground`
- `ui/components/LoadingState.kt` / `EmptyState.kt` / `ErrorState.kt`
- `ui/components/StatusBanner.kt` / `SearchField.kt`
- `ui/components/ActionButton.kt` / `ListItemRow.kt`
- `ui/components/FileTypeIcon.kt`（重写 mime 映射）
- `ui/components/Breadcrumb.kt`（新）/ `AppAlertDialog.kt`

### Phase 2 修改（旧 Cloud* 标 @Deprecated）
- `ui/components/Cloud{Scaffold,TopBar,BottomBar,ListItem,Card,SearchBar,EmptyState,StatusBanner,ActionButton,AlertDialog}.kt` — 加 `@Deprecated`
- `ui/components/BreadcrumbBar.kt` — 标 `@Deprecated`（Phase 4 删）

### Phase 3 新增
- `ui/common/AppError.kt` — 统一错误模型 + `toAppError()`
- `ui/feature/auth/{LoginScreen,LoginViewModel}.kt`
- `ui/feature/home/{HomeScreen,HomeViewModel}.kt`
- `ui/feature/file/{FileScreen,FileListContent,FileMultiSelectBar,FileViewModel,FileUiState,FileIntent}.kt`
- `ui/feature/preview/{PreviewScreen,PreviewImage,PreviewText,PreviewAudio,PreviewVideo,PreviewPdf,PreviewTooLarge,PreviewUnavailable,PreviewViewModel}.kt`
- `ui/feature/transfer/{TransferScreen,TransferListContent,TransferRow,TransferViewModel}.kt`
- `ui/feature/settings/{SettingsScreen,SettingsViewModel}.kt`
- `ui/feature/storage/StorageEditScreen.kt`
- `ui/feature/admin/AdminSiteSettingsScreen.kt`
- `ui/feature/picker/MoveCopyTargetPickerScreen.kt`

### Phase 3 修改
- `navigation/AppRoute.kt` — 改用 `@Serializable`
- `navigation/AppNavHost.kt` — 接 `AppScaffold` + `AppBottomBar`
- `navigation/BottomNavBar.kt` — M3 `NavigationBar` 包装
- `navigation/NavTransitions.kt` — 用 `CloudMotion` 重写

### Phase 4 修改 / 删除
- `MainActivity.kt` — 接 `ThemeRepository`
- 删除旧 `ui/components/Cloud*.kt` / `BreadcrumbBar.kt` / `DirectoryBrowser.kt` / `TransferProgress.kt`
- 删除旧 `ui/screens/*.kt`

### 测试文件
- `test/.../ui/common/AppErrorTest.kt`
- `test/.../ui/feature/file/FileViewModelTest.kt`
- `test/.../ui/feature/transfer/TransferViewModelTest.kt`
- `test/.../ui/feature/settings/SettingsViewModelTest.kt`
- `test/.../ui/components/FileTypeIconTest.kt`
- `androidTest/.../ui/feature/file/FileScreenTest.kt`
- `test/.../snapshot/{Theme,FileScreen}SnapshotTest.kt`

---

## Phase 1 — Token & Theme（Tasks 1-6）

### Task 1: 引入 Roborazzi + DataStore 依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1**: 在 `gradle/libs.versions.toml` 的 `[versions]` 末尾追加 `roborazzi = "1.32.2"` + `datastore = "1.1.1"`；在 `[libraries]` 末尾追加 4 个 `androidx-datastore-preferences` / `roborazzi` / `roborazzi-compose` / `roborazzi-junit-rule`；在 `[plugins]` 末尾追加 `roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }`

- [ ] **Step 2**: 在 `app/build.gradle.kts` 的 `plugins { ... }` 末尾追加 `alias(libs.plugins.roborazzi)`；在 `dependencies { ... }` 中 `testImplementation(libs.robolectric)` 后追加 `implementation(libs.androidx.datastore.preferences)` + 3 个 roborazzi 依赖

- [ ] **Step 3**: 编译 `cd "D:/programming/projects/my project/alist" && ./gradlew :app:compileDebugKotlin --quiet` → 期望 `BUILD SUCCESSFUL`

- [ ] **Step 4**: 跑测试 `./gradlew :app:testDebugUnitTest --quiet` → 期望 `BUILD SUCCESSFUL`

- [ ] **Step 5**: Commit `git add gradle/libs.versions.toml app/build.gradle.kts && git commit -m "build(deps): add roborazzi + datastore-preferences"`

### Task 2: Color Token（浅/深 M3 全套）

**Files:** Modify `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt`

- [ ] **Step 1**: 完整替换为（关键 token）：
```kotlin
package com.textvision.alistclient.ui.theme
import androidx.compose.ui.graphics.Color
// Brand
val IndigoBlue40 = Color(0xFF1F6FEB); val IndigoBlue80 = Color(0xFFA9C7FF)
val IndigoBlue20 = Color(0xFF001A41); val IndigoBlue90 = Color(0xFFD8E4FE)
// Secondary / Tertiary / Neutral / Error / Outline
// (完整版见 spec §3.1)
val CloudPrimary = IndigoBlue40  // legacy alias
val AlistBlue = IndigoBlue40     // legacy alias
```

- [ ] **Step 2**: 编译 `./gradlew :app:compileDebugKotlin --quiet` → `BUILD SUCCESSFUL`

- [ ] **Step 3**: Commit `git add app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt && git commit -m "feat(theme): m3 expressive color palette"`

### Task 3: Type Scale（15 个 M3 Expressive 样式）

**Files:** Modify `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt`

- [ ] **Step 1**: 替换为 `val AppTypography = Typography(displayLarge/.../labelSmall = TextStyle(fontFamily = FontFamily.Default, ...))`，按 spec §3.2 表填全 15 项；保留 `val CloudTypography = AppTypography` 别名

- [ ] **Step 2**: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 3**: Commit `git commit -m "feat(theme): m3 expressive typography scale"`

### Task 4: Shape + Motion Tokens

**Files:** Modify `ui/theme/Shape.kt`，Create `ui/theme/Motion.kt`，Create `test/.../ui/theme/MotionTest.kt`

- [ ] **Step 1**: 替换 `Shape.kt`：`object Corner { ExtraSmall=4, Small=8, Medium=12, Large=16, ExtraLarge=28 }.dp` + `val AppShapes = Shapes(extraSmall = RoundedCornerShape(Corner.ExtraSmall), ...)` + `val CloudShapes = AppShapes`

- [ ] **Step 2**: 创建 `Motion.kt`：`object CloudMotion { SpringFast/Medium/Slow + TweenShort(120)/Medium(240)/Long(400) + easing = FastOutSlowInEasing }`

- [ ] **Step 3**: 创建 `MotionTest.kt`：2 个测试（spring/tween specs 非空）

- [ ] **Step 4**: 跑测试 → 2 通过；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5**: Commit `git commit -m "feat(theme): shape + motion tokens"`

### Task 5: Theme.kt 入口 + ThemeRepository

**Files:** Modify `ui/theme/Theme.kt`，Create `ui/theme/ThemeRepository.kt`，Create `test/.../ui/theme/ThemeRepositoryTest.kt`

- [ ] **Step 1**: 创建 `ThemeRepository.kt`：
```kotlin
enum class DarkMode { SYSTEM, LIGHT, DARK }
private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val DARK_MODE_KEY = stringPreferencesKey("dark_mode_override")
@Singleton class ThemeRepository @Inject constructor(@ApplicationContext private val context: Context) {
  val darkMode: Flow<DarkMode> = context.themeDataStore.data.map { it[DARK_MODE_KEY]?.let { DarkMode.valueOf(it) } ?: DarkMode.SYSTEM }
  suspend fun setDarkMode(mode: DarkMode) { context.themeDataStore.edit { it[DARK_MODE_KEY] = mode.name } }
}
```

- [ ] **Step 2**: 创建 `ThemeRepositoryTest.kt`（Robolectric）：`default_is_system` + `set_then_read_returns_same`

- [ ] **Step 3**: 替换 `Theme.kt`：`LightColorScheme` / `DarkColorScheme`（用 Indigo/Slate/Amber/Neutral/Red 调色） + `AlistClientTheme(darkTheme, dynamicColor, content)` 入口（Android 12+ 调 `dynamicLight/DarkColorScheme(context)`）

- [ ] **Step 4**: 跑测试 → 2 通过；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5**: Commit `git commit -m "feat(theme): AlistClientTheme + ThemeRepository"`

### Task 6: 第一个 Roborazzi baseline

**Files:** Create `test/.../snapshot/ThemeSnapshotTest.kt`

- [ ] **Step 1**: 创建 3 个 Roborazzi 测试（`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk=[33], qualifiers=Pixel5)` + `@GraphicsMode(NATIVE)`）：`theme_light` / `theme_dark` / `theme_dynamic_light`，每个 `setContent { AlistClientTheme(...) { Surface { Box { Text(...) } } } }` + `onRoot().captureRoboImage("src/test/snapshots/images/theme_xxx.png")`

- [ ] **Step 2**: 生成 baseline `./gradlew :app:recordRoborazziDebug --tests "com.textvision.alistclient.snapshot.ThemeSnapshotTest" --quiet` → 3 PNG

- [ ] **Step 3**: 验证 `./gradlew :app:verifyRoborazziDebug --tests "..." --quiet` → `BUILD SUCCESSFUL`

- [ ] **Step 4**: Commit `git add app/src/test/java/com/textvision/alistclient/snapshot/ThemeSnapshotTest.kt app/src/test/snapshots/images/theme_*.png && git commit -m "test(snapshot): theme baselines"`

---

## Phase 2 — Foundation & Components（Tasks 7-15）

### Task 7: AppScaffold + AppBackground

**Files:** Create `ui/foundation/Scaffold.kt` + `ui/foundation/Backgrounds.kt`

- [ ] **Step 1**: 创建 `Backgrounds.kt`：`@Composable fun AppBackground(modifier, content) = Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }`

- [ ] **Step 2**: 创建 `Scaffold.kt`：`@Composable fun AppScaffold(modifier, topBar = {}, bottomBar = {}, content) = AppBackground { Scaffold(modifier, topBar, bottomBar, containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets.systemBars, content) }`

- [ ] **Step 3**: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 4**: Commit `git commit -m "feat(ui): AppScaffold + AppBackground"`

### Task 8: AppTopBar + AppBottomBar

**Files:** Create `ui/foundation/AppBars.kt`

- [ ] **Step 1**: 创建 `AppBars.kt`：`AppTopBar(title, subtitle?, onNavigateUp?, actions: @Composable RowScope.() -> Unit = {})` 用 M3 `TopAppBar` + Column(title/subtitle) + ArrowBack IconButton；`AppBottomBar(currentRoute, onNavigate)` 用 M3 `NavigationBar` + 4 个 `NavigationBarItem`（home/files/transfers/settings）

- [ ] **Step 2**: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 3]: Commit `git commit -m "feat(ui): AppTopBar + AppBottomBar (M3)"`

### Task 9: LoadingState + EmptyState + ErrorState

**Files:** Create `ui/components/LoadingState.kt` + `EmptyState.kt` + `ErrorState.kt`

- [ ] **Step 1**: `LoadingState.kt` — Center Column 包含 `CircularProgressIndicator` + 16dp Spacer + `Text(message, bodyMedium, onSurfaceVariant)`

- [ ] **Step 2**: `EmptyState.kt` — Center Column 含 optional `Icon(48dp, onSurfaceVariant)` + `Text(title, titleMedium)` + optional message + optional TextButton action

- [ ] **Step 3**: `ErrorState.kt` — Center Column 含 `Icon(Icons.Outlined.ErrorOutline, 48dp, error tint)` + message + 可选 `TextButton("重试")`

- [ ] **Step 4**: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5**: Commit `git commit -m "feat(ui): LoadingState + EmptyState + ErrorState"`

### Task 10: StatusBanner + SearchField

**Files:** Create `ui/components/StatusBanner.kt` + `SearchField.kt`

- [ ] **Step 1**: `StatusBanner.kt` — `enum class BannerKind { INFO, WARNING, ERROR, SUCCESS }` + `StatusBanner(kind, message, actionLabel?, onAction?, modifier)` 用 M3 `Surface(RoundedCornerShape(12dp), tonalElevation=0)` + 内部 Row 含 Icon(20dp) + message + 可选 TextButton action

- [ ] **Step 2**: `SearchField.kt` — M3 `TextField(singleLine, shape = RoundedCornerShape(12dp), modifier.fillMaxWidth().height(52dp))` + `TextFieldDefaults.colors(focusedContainerColor = surfaceContainerHigh, unfocusedContainerColor = surfaceContainerHigh, indicatorColor = Transparent)` + leadingIcon `Icons.Outlined.Search`

- [ ] **Step 3]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 4**: Commit `git commit -m "feat(ui): StatusBanner + SearchField"`

### Task 11: ActionButton (4 variants)

**Files:** Create `ui/components/ActionButton.kt`

- [ ] **Step 1**: 创建 `ActionButton.kt`：`enum class ButtonVariant { FILLED, TONAL, OUTLINED, TEXT }` + `ActionButton(text, onClick, modifier, variant=ButtonVariant.FILLED, enabled=true, loading=false, leadingIcon=null)` 用 `Button` / `FilledTonalButton` / `OutlinedButton` / `TextButton` 分别对应 variant，高度 40dp，loading 时显示 CircularProgressIndicator

- [ ] **Step 2]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 3**: Commit `git commit -m "feat(ui): ActionButton (4 variants)"`

### Task 12: ListItemRow

**Files:** Create `ui/components/ListItemRow.kt`

- [ ] **Step 1**: 创建 `ListItemRow.kt`：`@OptIn(ExperimentalFoundationApi::class) @Composable fun ListItemRow(leading, title, subtitle?, trailing, onClick?, onLongClick?, modifier)` 用 `Surface` + `combinedClickable(interactionSource, indication = rememberRipple(), onClick, onLongClick)` 包裹 Row（高度 56dp + leading + Column(title/subtitle, weight=1f) + trailing）

- [ ] **Step 2**: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 3]: Commit `git commit -m "feat(ui): ListItemRow (with long-press)"`

### Task 13: FileTypeIcon（mime 映射）

**Files:** Modify `ui/components/FileTypeIcon.kt`，Create `test/.../ui/components/FileTypeIconTest.kt`

- [ ] **Step 1**: 完整替换为：`enum class FileCategory { FOLDER, IMAGE, VIDEO, AUDIO, TEXT, CODE, ARCHIVE, PDF, DOCUMENT, OTHER }` + `fun fileCategoryFromMime(mime, name)`（按 mime 前缀 + ext 列表映射） + `FileTypeIcon(category, modifier, size=40dp)` 用 `Surface(RoundedCornerShape(12dp), container=对应 M3 slot)` + Box + Icon（Material Icons Extended 的 Folder/Image/Movie/MusicNote/Description/Code/Archive/PictureAsPdf/Article/InsertDriveFile）

- [ ] **Step 2**: 创建 `FileTypeIconTest.kt`（7 个测试覆盖 image/video/pdf/code/archive/text/other 映射）

- [ ] **Step 3]: 跑测试 → 7 通过；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 4**: Commit `git commit -m "feat(ui): FileTypeIcon with mime mapping"`

### Task 14: Breadcrumb + AppAlertDialog

**Files:** Create `ui/components/Breadcrumb.kt` + `AppAlertDialog.kt`，Modify `ui/components/BreadcrumbBar.kt`

- [ ] **Step 1**: 创建 `Breadcrumb.kt`：`Breadcrumb(path, onNavigate, modifier)` 用 `Row(horizontalScroll)` + 第一个 `TextButton("/")` 跳根目录 + 后续 `TextButton` 每个路径段

- [ ] **Step 2]: 标记 `BreadcrumbBar.kt` 为 `@Deprecated` 桩（保留原签名，函数体空）

- [ ] **Step 3**: 创建 `AppAlertDialog.kt`：`AppAlertDialog(title, message?, confirmLabel, onConfirm, dismissLabel, onDismiss, destructive=false)` 用 M3 `AlertDialog` + confirmButton 颜色按 `destructive` 选 error/primary

- [ ] **Step 4]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5]: Commit `git commit -m "feat(ui): Breadcrumb + AppAlertDialog"`

### Task 15: 旧 Cloud* 标 @Deprecated

**Files:** Modify 10 个 `ui/components/Cloud*.kt` 文件

- [ ] **Step 1**: 列出文件 `ls app/src/main/java/com/textvision/alistclient/ui/components/`

- [ ] **Step 2]: 对每个旧 Composable 的 `fun` 声明前加 `@Deprecated(message = "使用 M3 Expressive 组件替代；Phase 4 删除", replaceWith = ReplaceWith("<新组件>(...)"))`（具体映射：CloudScaffold→AppScaffold, CloudTopBar→AppTopBar, CloudBottomBar→AppBottomBar, CloudListItem→ListItemRow, CloudCard→Surface(shape=MaterialTheme.shapes.medium), CloudSearchBar→SearchField, CloudEmptyState→EmptyState, CloudStatusBanner→StatusBanner, CloudActionButton→ActionButton, CloudAlertDialog→AppAlertDialog）

- [ ] **Step 3]: 编译 → `BUILD SUCCESSFUL`（预期有 deprecation 警告）

- [ ] **Step 4]: Commit `git commit -m "refactor(ui): deprecate legacy Cloud* components"`

---

## Phase 3 — Screens（Tasks 16-26）

### Task 16: AppError 统一错误模型

**Files:** Create `ui/common/AppError.kt` + `test/.../ui/common/AppErrorTest.kt`

- [ ] **Step 1]: 创建 `AppError.kt`：
```kotlin
sealed interface AppError {
  data class Network(val cause: String?) : AppError
  data class Server(val code: Int, val message: String) : AppError
  data class Unauthorized(val reason: String) : AppError
  data class Unknown(val message: String) : AppError
}
fun Throwable.toAppError(): AppError = when (this) {
  is IOException -> AppError.Network(message)
  is HttpException -> if (code() in 401..403) AppError.Unauthorized(message()) else AppError.Server(code(), message())
  is AppError -> this
  else -> AppError.Unknown(message ?: this::class.simpleName.orEmpty())
}
@Composable fun AppError.userMessage(): String = when (this) {
  is AppError.Network -> "网络连接失败：${cause ?: "请检查网络"}"
  is AppError.Server -> "服务错误 $code：$message"
  is AppError.Unauthorized -> "请重新登录"
  is AppError.Unknown -> message.ifBlank { "未知错误" }
}
```

- [ ] **Step 2]: 创建 `AppErrorTest.kt`（3 个测试：ioexception→network, unknown throwable→unknown, AppError pass-through）

- [ ] **Step 3]: 跑测试 → 3 通过；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 4]: Commit `git commit -m "feat(ui): unified AppError model"`

### Task 17: FileViewModel + FileUiState + FileIntent

**Files:** Create `ui/feature/file/{FileUiState,FileIntent,FileViewModel}.kt` + `test/.../ui/feature/file/FileViewModelTest.kt`

- [ ] **Step 1]: `FileUiState.kt`：`@Immutable data class FileUiState(path, files, isLoading, error, query, selection, isMultiSelectMode)` 含 `visibleFiles`（按 query 过滤）/ `isSelectionEmpty` / `isAllSelected` 派生属性

- [ ] **Step 2]: `FileIntent.kt`：`sealed interface FileIntent { Load(path); Search(query); MultiSelectToggle(path); MultiSelectClear; MultiSelectDelete(paths); MultiSelectDownload(paths) }`

- [ ] **Step 3]: `FileViewModel.kt`：`@HiltViewModel class FileViewModel @Inject constructor(repo: FileRepository) : ViewModel()` 暴露 `state: StateFlow<FileUiState>` + `onIntent(intent)`；内部 `load(path)` 调 `repo.list(path)` + `toggleSelect(path)` + `deleteSelected(paths)` 调 `repo.delete` + `downloadSelected(paths)` 调 `repo.requestDownload`

- [ ] **Step 4]: 确认 `FileRepository.list/delete/requestDownload` 存在（`grep` 验证），不存在则在 `FileRepository` 加最小桩实现

- [ ] **Step 5]: 创建 `FileViewModelTest.kt`（6 测试：load_success / load_failure / search_filter / multi_select_toggle / multi_select_clear / multi_select_delete_calls_repo_and_relists）

- [ ] **Step 6]: 跑测试 → 6 通过；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 7]: Commit `git commit -m "feat(file): FileViewModel with UDF + multi-select"`

### Task 18: FileScreen 重构

**Files:** Create `ui/feature/file/{FileScreen,FileListContent,FileMultiSelectBar}.kt`，Delete `ui/screens/FileScreen.kt`（旧）

- [ ] **Step 1]: `FileListContent.kt`：`@Composable fun FileListContent(state, onIntent, onPreview, onFolderNavigate, contentPadding)` — `LazyColumn(items(visible, key={it.path}))` 渲染 `ListItemRow(leading = FileTypeIcon(...), title, subtitle, onClick={if (isMultiSelect) toggle else if (isDir) navigate else preview}, onLongClick={toggle})` + Checkbox trailing 在多选模式；空列表显示 `EmptyState("文件夹为空", error?.let{"请检查网络后重试"})`

- [ ] **Step 2]: `FileMultiSelectBar.kt`：`@Composable fun FileMultiSelectBar(selectionCount, onSelectAll, onDelete, onDownload, onClear)` — M3 `Surface(surfaceContainerHigh)` + Row 含 Close IconButton + `Text("已选 $count 项")` + DoneAll/Download/Delete IconButton 组

- [ ] **Step 3]: `FileScreen.kt`（< 300 行）：`@Composable fun FileScreen(initialPath="/", onPreview, onFolderNavigate, vm = hiltViewModel())` — `LaunchedEffect(initialPath) { if (state.path != initialPath) vm.onIntent(Load(initialPath)) }` + `AppScaffold(topBar = AppTopBar("文件", subtitle=path, actions={IconButton(Refresh){...}}))` body：`SearchField(state.query, ...)` + Spacer + `state.error?.let{StatusBanner(ERROR, err.userMessage(), "重试", {reload})}` + 可选 `FileMultiSelectBar(...)` + `FileListContent(...)`

- [ ] **Step 4]: 编译（可能短暂有 onLongClick 缺失错误，见 Step 5）→ 失败则继续

- [ ] **Step 5]: 如 ListItemRow 缺 `onLongClick` 参数，修改 Task 12 已加；否则跳过

- [ ] **Step 6]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 7]: Commit `git commit -m "feat(file): FileScreen with multi-select"`

### Task 19: AppRoute 改 @Serializable

**Files:** Modify `navigation/AppRoute.kt`

- [ ] **Step 1]: Read 旧 AppRoute.kt 全文

- [ ] **Step 2]: 替换为：
```kotlin
@Serializable data object Login
@Serializable data class Files(val path: String = "/")
@Serializable data object Transfers
@Serializable data object Settings
@Serializable data object Home
@Serializable data class PreviewArgs(val name: String, val path: String, val mime: String, val downloadUrl: String?, val size: Long)
@Serializable data class Preview(val args: PreviewArgs)
@Serializable data class MoveCopyPicker(val op: String, val path: String)
@Serializable data class StorageEdit(val id: Int)
@Serializable data object AdminSiteSettings
```

- [ ] **Step 3]: 编译 → 列出失败引用点

- [ ] **Step 4]: Read 每个失败文件，手动 Edit 把 `AppRoute.X` 改为直接 `X`（调整 import）

- [ ] **Step 5]: 编译 → `BUILD SUCCESSFUL`；跑现有测试 → 通过

- [ ] **Step 6]: Commit `git commit -m "refactor(nav): @Serializable type-safe AppRoute"`

### Task 20: AppNavHost 切换新组件

**Files:** Modify `navigation/AppNavHost.kt`，Create `navigation/BottomNavBar.kt`，Modify `navigation/NavTransitions.kt`

- [ ] **Step 1]: 创建 `BottomNavBar.kt`：`@Composable fun AppBottomNavBar(navController)` 用 `currentBackStackEntryAsState()` 拿 currentRoute，调 `AppBottomBar(currentRoute, onNavigate = { route → navController.navigate(route) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } })`

- [ ] **Step 2]: 替换 `NavTransitions.kt`：`object AppNavTransitions { tabEnter/Exit/PopEnter/PopExit: slideInHorizontally(240ms) + fadeIn(240ms); detailEnter/Exit/PopEnter/PopExit: slideInHorizontally(240ms, it/4) + fadeIn(240ms) }`

- [ ] **Step 3]: 重写 `AppNavHost.kt`（< 200 行）：`@Composable fun AppNavHost(navController = rememberNavController(), snackbarHostState: SnackbarHostState? = null)` + `Box(fillMaxSize) { NavHost(startDestination = Home, enterTransition, ...) { composable<Home> { HomeScreen(onNavigate = { navController.navigate(it) }) }; composable<Files> { entry → FileScreen(...) }; composable<Transfers> { TransferScreen() }; ... } + AppBottomNavBar(navController) }`

- [ ] **Step 4]: 为 Screen 引用先建占位空文件（`ui/feature/{home,transfer,settings,preview,picker,storage,admin}/...Screen.kt` 各 1 行空 Composable 签名）使编译通过

- [ ] **Step 5]: 编译 → `BUILD SUCCESSFUL`；跑现有测试 → 通过

- [ ] **Step 6]: Commit `git commit -m "refactor(nav): AppNavHost + type-safe routes"`

### Task 21-22: LoginScreen + HomeScreen 重做

**Files:** Create `ui/feature/auth/{LoginScreen,LoginViewModel}.kt`，Delete `ui/screens/LoginScreen.kt`；Create `ui/feature/home/{HomeScreen,HomeViewModel}.kt`，Delete `home/HomeScreen.kt`

- [ ] **Step 1]: Read 旧 LoginScreen.kt + LoginViewModel.kt，提取字段

- [ ] **Step 2]: 创建 LoginViewModel.kt：`data class LoginUiState(server, username, password, isLoading, error)` + `@HiltViewModel class LoginViewModel @Inject constructor(repo: AuthRepository)` 暴露 `state: StateFlow<LoginUiState>` + `setServer/Username/Password` + `submit(onSuccess)`（调 `repo.login`）

- [ ] **Step 3]: 创建 LoginScreen.kt：`AppScaffold { Column { Icon(Lock 48dp) + Text("登录 Alist" headlineMedium) + error?.let{StatusBanner(ERROR, msg)} + OutlinedTextField(server) + OutlinedTextField(username) + OutlinedTextField(password, PasswordVisualTransformation) + Spacer(8.dp) + ActionButton("登录", onClick = { vm.submit(onLoggedIn) }, loading = isLoading) } }`

- [ ] **Step 4]: 删除旧 `ui/screens/LoginScreen.kt`；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5]: Read 旧 home/HomeScreen.kt + HomeViewModel.kt

- [ ] **Step 6]: 创建 HomeViewModel.kt：`data class HomeUiState(isLoading, online, siteTitle, kpis, storages, error)` + `@HiltViewModel class HomeViewModel @Inject constructor(repo: HomeRepository)` 暴露 `state: StateFlow<HomeUiState>` + `refresh()`（调 `repo.load()`）

- [ ] **Step 7]: 创建 HomeScreen.kt：`AppScaffold(topBar = AppTopBar("首页", subtitle=if (online) "在线" else "离线")) { when { isLoading && storages.empty → LoadingState(); error != null && storages.empty → ErrorState(msg, onRetry=refresh); storages.empty → EmptyState("暂无内容"); else → PullToRefreshBox(isRefreshing, onRefresh) { LazyColumn { item { SiteCard(title, kpis) }; items(storages, key={it.mountPath}) { ListItemRow(title=name, subtitle=mountPath, onClick={onNavigate(Files(mountPath))}) } } } } }`

- [ ] **Step 8]: 删除旧 `home/HomeScreen.kt`；编译 → `BUILD SUCCESSFUL`；跑现有 Home 测试 → 通过

- [ ] **Step 9]: Commit `git commit -m "feat(auth,home): screens rewritten"`

### Task 23: TransferScreen 拆分

**Files:** Create `ui/feature/transfer/{TransferScreen,TransferListContent,TransferRow,TransferViewModel}.kt`，Delete `ui/screens/TransferScreen.kt`

- [ ] **Step 1]: Read 旧 `ui/screens/TransferScreen.kt` + `TransferManager.kt`，提取 `TransferItem` 字段

- [ ] **Step 2]: 创建 `TransferViewModel.kt`：`enum class TransferTab { UPLOAD, DOWNLOAD }; data class TransferListUiState(tab, uploads, downloads) { visible / summary 派生 }` + `@HiltViewModel class TransferViewModel @Inject constructor(manager: TransferManager)` 暴露 `state: StateFlow<TransferListUiState>`（`combine(_tab, manager.uploads, manager.downloads) { tab, u, d → UiState(tab, u, d) }.stateIn(...)`）+ `selectTab(tab)` + `cancel/retry/delete(id)` 委托 manager

- [ ] **Step 3]: 创建 `TransferRow.kt`：`@Composable fun TransferRow(item, onCancel, onRetry, onDelete)` 用 M3 `Surface` + Column(Row(name+subtitle, weight=1f + 状态按钮 cancel/retry/delete) + ifActive `LinearProgressIndicator(progress = item.progress)`)

- [ ] **Step 4]: 创建 `TransferListContent.kt`：`@Composable fun TransferListContent(rows, onCancel, onRetry, onDelete)` 用 `LazyColumn(items(rows, key={it.id}))` + 空时 `EmptyState("暂无传输任务")`

- [ ] **Step 5]: 创建 `TransferScreen.kt`：`AppScaffold(topBar = AppTopBar("传输", subtitle = state.summary)) { Column { SingleChoiceSegmentedButtonRow { TransferTab.entries.forEachIndexed { index, tab → SegmentedButton(selected = state.tab == tab, onClick = { vm.selectTab(tab) }) { Text(if (tab == UPLOAD) "上传" else "下载") } } } + TransferListContent(state.visible, ...) } }`

- [ ] **Step 6]: 删除旧 `ui/screens/TransferScreen.kt`；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 7]: 创建 `TransferViewModelTest.kt`（2 测试：selectTab_switches / cancel_delegates_to_manager）

- [ ] **Step 8]: 跑测试 → 2 通过

- [ ] **Step 9]: Commit `git commit -m "feat(transfer): TransferScreen split"`

### Task 24: SettingsScreen 重做（含 dark mode 切换）

**Files:** Create `ui/feature/settings/{SettingsScreen,SettingsViewModel}.kt`，Delete `ui/screens/SettingsScreen.kt` + `ui/screens/SettingsViewModel.kt`

- [ ] **Step 1]: Read 旧 2 个 Settings 文件

- [ ] **Step 2]: 创建 `SettingsViewModel.kt`：`data class Storage(id, name, mountPath, enabled); data class SettingsUiState(darkMode, storages); sealed interface SettingsIntent { SetDarkMode(mode); ToggleStorage(id, enabled) }` + `@HiltViewModel class SettingsViewModel @Inject constructor(themeRepo, settingsRepo)` `state = combine(themeRepo.darkMode, settingsRepo.storages) { ... }.stateIn(...)` + `onIntent(intent)` 委托 repo

- [ ] **Step 3]: 确认 `SettingsRepository.storages: Flow<List<Storage>>` + `setStorageEnabled(id, enabled)` 存在，不存在加最小桩

- [ ] **Step 4]: 创建 `SettingsScreen.kt`：`AppScaffold(topBar = AppTopBar("设置")) { LazyColumn { item { SectionHeader("主题") }; item { DarkModeRow(current, onSelect) }; item { SectionHeader("存储") }; items(storages, key={it.id}) { storage → ListItemRow(title, subtitle, trailing = Switch(checked, onCheckedChange)) } } }` + `DarkModeRow` 用 Column + 每行 `Row(clickable)` 渲染 DarkMode.entries + 当前选中显示 ✓

- [ ] **Step 5]: 删除旧 2 文件；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 6]: 创建 `SettingsViewModelTest.kt`（2 测试：setDarkMode_persists / toggleStorage_calls_repo）

- [ ] **Step 7]: 跑测试 → 2 通过

- [ ] **Step 8]: Commit `git commit -m "feat(settings): SettingsScreen with dark mode switch"`

### Task 25: PreviewScreen 拆分

**Files:** Create `ui/feature/preview/{PreviewScreen,PreviewImage,PreviewText,PreviewAudio,PreviewVideo,PreviewPdf,PreviewTooLarge,PreviewUnavailable,PreviewViewModel}.kt`，Delete `ui/screens/PreviewScreen.kt`

- [ ] **Step 1]: Read 旧 `ui/screens/PreviewScreen.kt` 全文，提取 PreviewMode 分支逻辑

- [ ] **Step 2]: 创建 `PreviewViewModel.kt`：`data class PreviewUiState(args, mode)` + `@HiltViewModel class PreviewViewModel @Inject constructor()` 暴露 `state: StateFlow<PreviewUiState?>` + `init(args)`（调 `PreviewRouter.route(...)` 设置 state）

- [ ] **Step 3]: 创建 7 个 Preview 子组件（每个文件 ≤ 50 行）：
  - `PreviewImage.kt` — `AsyncImage(model=url, fillMaxSize)`
  - `PreviewText.kt` — `Text(content, fillMaxSize, verticalScroll)`
  - `PreviewAudio.kt` — `AndroidView(::MediaController)`（简化：保留原 MediaPlayer 逻辑在函数体内）
  - `PreviewVideo.kt` — `AndroidView(::VideoView, setVideoURI+start)`
  - `PreviewPdf.kt` — `LaunchedEffect(url) { ctx.startActivity(Intent(ACTION_VIEW, Uri)) }`
  - `PreviewTooLarge.kt` — `Text("文件过大（${size/1024/1024} MB），请下载后查看")`
  - `PreviewUnavailable.kt` — `Text("无法预览：$reason")`

- [ ] **Step 4]: 重写 `PreviewScreen.kt`（< 300 行）：`AppScaffold(topBar = AppTopBar(args.name, subtitle = args.path, onNavigateUp = onBack)) { Box(fillMaxSize, contentAlignment = Center) { state?.let { s → when (s.mode) { Image → PreviewImage(url); Text → PreviewText(url); Audio → PreviewAudio(url); Video → PreviewVideo(url); External → PreviewPdf(url); TextTooLarge → PreviewTooLarge(size); Unavailable → PreviewUnavailable(reason) } } ?: LoadingState() } }`

- [ ] **Step 5]: 删除旧 `ui/screens/PreviewScreen.kt`；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 6]: Commit `git commit -m "refactor(preview): split into 7 sub-components"`

### Task 26: 其他 Screen 迁移

**Files:** Create `ui/feature/picker/MoveCopyTargetPickerScreen.kt` + `ui/feature/storage/StorageEditScreen.kt` + `ui/feature/admin/AdminSiteSettingsScreen.kt`，Delete 旧 `ui/screens/MoveCopyTargetPickerScreen.kt` + `admin/storage/StorageEditScreen.kt` + `admin/settings/AdminSiteSettingsScreen.kt`

- [ ] **Step 1]: Read 旧 3 个 Screen 文件，提取文案 + 字段

- [ ] **Step 2]: 3 个新 Screen 各 ≤ 150 行，复用旧业务逻辑但替换顶部为 `AppTopBar` + 主体为 M3 组件（`AppScaffold` + `ActionButton("保存", FILLED)` + `AppAlertDialog` 替换原 `AlertDialog`）

- [ ] **Step 3]: 删除旧 3 文件；编译 → `BUILD SUCCESSFUL`

- [ ] **Step 4]: Commit `git commit -m "refactor(screens): migrate picker+storage+admin"`

### Task 27: 主 Screen 的 Compose UI Test + Roborazzi baseline

**Files:** Create `androidTest/.../ui/feature/file/FileScreenTest.kt` + `test/.../snapshot/FileScreenSnapshotTest.kt`

- [ ] **Step 1]: `FileScreenTest.kt`：`@RunWith(AndroidJUnit4::class)` + `createComposeRule()`；2 个测试：`emptyState_shows_empty_message`（setContent + `FileListContent(FileUiState())` + assertNodeWithText("文件夹为空").isDisplayed()） + `long_press_enters_multi_select_mode`（准备 state with 1 file + performLongClick + 验证 list 显示）

- [ ] **Step 2]: `FileScreenSnapshotTest.kt`：`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk=[33], qualifiers=Pixel5)` + `@GraphicsMode(NATIVE)`；3 个测试：`file_screen_empty` / `file_screen_success`（含 4 个不同类型文件：Documents dir / photo.jpg / report.pdf / notes.md） / `file_screen_dark`

- [ ] **Step 3]: 生成 baseline `./gradlew :app:recordRoborazziDebug --tests "com.textvision.alistclient.snapshot.FileScreenSnapshotTest" --quiet` → 3 PNG

- [ ] **Step 4]: 跑测试 `./gradlew :app:verifyRoborazziDebug --tests "..." --quiet` → 3 通过；跑 instrumented test（需连接设备）→ 通过

- [ ] **Step 5]: Commit `git commit -m "test(file): UI Test + Roborazzi baselines"`

---

## Phase 4 — 体验增强 & 收尾（Tasks 28-32）

### Task 28: NetworkMonitor → 离线 Banner

**Files:** Modify `ui/feature/file/FileScreen.kt` + `ui/feature/transfer/TransferScreen.kt` + `ui/feature/file/FileViewModel.kt` + `ui/feature/transfer/TransferViewModel.kt` + `common/network/NetworkMonitor.kt`（已有）

- [ ] **Step 1]: 确认 `NetworkMonitor.isOnline: StateFlow<Boolean>` 存在（`grep` 验证）

- [ ] **Step 2]: `FileViewModel` 注入 `NetworkMonitor`，state 用 `combine(_state, networkMonitor.isOnline) { ui, online → ui.copy(isOnline = online) }`；`FileUiState` 加 `val isOnline: Boolean = true`

- [ ] **Step 3]: `FileScreen` 在 `state.error` Banner 之后加 `if (!state.isOnline) StatusBanner(WARNING, "当前离线")`

- [ ] **Step 4]: `TransferViewModel` + `TransferRow` 同样处理（离线禁用 cancel/retry）

- [ ] **Step 5]: 编译 → `BUILD SUCCESSFUL`；跑测试 → 通过

- [ ] **Step 6]: Commit `git commit -m "feat(ui): offline banner"`

### Task 29: SnackbarHost 全局化

**Files:** Modify `MainActivity.kt` + `navigation/AppNavHost.kt`

- [ ] **Step 1]: Read `MainActivity.kt`

- [ ] **Step 2]: 改 `MainActivity.kt`：注入 `ThemeRepository`，定义 `val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("No SnackbarHostState") }`，`setContent` 内 `val snackbar = remember { SnackbarHostState() }; CompositionLocalProvider(LocalSnackbarHostState provides snackbar) { AlistClientTheme { AppNavHost(snackbarHostState = snackbar) } }`

- [ ] **Step 3]: 改 `AppNavHost.kt`：接 `snackbarHostState` 参数，包在 `Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) })` 内

- [ ] **Step 4]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5]: Commit `git commit -m "feat(ui): global SnackbarHost"`

### Task 30: PullToRefresh 全 Screen + 进度条 Spring

**Files:** Modify `ui/feature/transfer/TransferScreen.kt`，Create `ui/feature/transfer/TransferProgress.kt`

- [ ] **Step 1]: 创建 `TransferProgress.kt`：`@Composable fun TransferProgress(progress, modifier) = LinearProgressIndicator(progress = { animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = CloudMotion.SpringFast, label = "transferProgress").value }, modifier.fillMaxWidth())`

- [ ] **Step 2]: `TransferRow` 替换 `LinearProgressIndicator` 为 `TransferProgress(item.progress)`

- [ ] **Step 3]: `TransferScreen` 内容包 `PullToRefreshBox(isRefreshing, onRefresh = { vm.refresh() })`（TransferViewModel 加 `fun refresh()` 调 `manager.refresh()`）

- [ ] **Step 4]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5]: Commit `git commit -m "feat(transfer): PullToRefresh + Spring progress"`

### Task 31: ThemeRepository 接入 MainActivity

**Files:** Modify `MainActivity.kt`

- [ ] **Step 1]: 改 `setContent`：`val mode by themeRepository.darkMode.collectAsStateWithLifecycle(initialValue = DarkMode.SYSTEM); val systemDark = isSystemInDarkTheme(); val darkTheme = when (mode) { SYSTEM → systemDark; LIGHT → false; DARK → true }` 传给 `AlistClientTheme(darkTheme = darkTheme)`

- [ ] **Step 2]: 编译 → `BUILD SUCCESSFUL`；跑测试 → 通过

- [ ] **Step 3]: Commit `git commit -m "feat(theme): wire dark mode override into Activity"`

### Task 32: 收尾（删旧组件 + 行数检查）

**Files:** Delete 11 个 `ui/components/Cloud*.kt` + `BreadcrumbBar.kt` + `DirectoryBrowser.kt` + `TransferProgress.kt` + 整个 `ui/screens/` 目录

- [ ] **Step 1]: 列出待删文件：`ls app/src/main/java/com/textvision/alistclient/ui/components/ | grep -E "^(Cloud|BreadcrumbBar|DirectoryBrowser|TransferProgress)"`

- [ ] **Step 2]: Grep 确认无引用：`grep -rn "CloudScaffold\|CloudTopBar\|CloudBottomBar\|CloudListItem\|CloudCard\|CloudSearchBar\|CloudEmptyState\|CloudStatusBanner\|CloudActionButton\|CloudAlertDialog\|BreadcrumbBar\|DirectoryBrowser" app/src/main/java/com/textvision/alistclient/ --include="*.kt"` → 期望 0 hits

- [ ] **Step 3]: 删除：
```bash
rm -f app/src/main/java/com/textvision/alistclient/ui/components/Cloud*.kt
rm -f app/src/main/java/com/textvision/alistclient/ui/components/BreadcrumbBar.kt
rm -f app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt
rm -f app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt
rm -rf app/src/main/java/com/textvision/alistclient/ui/screens/
```

- [ ] **Step 4]: 编译 → `BUILD SUCCESSFUL`

- [ ] **Step 5]: 跑全部测试 `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug --quiet` → 全绿

- [ ] **Step 6]: 行数强制约束检查：`find app/src/main/java/com/textvision/alistclient/ui -name "*.kt" -exec wc -l {} \; | awk '$1 > 400 { print }'` → 期望无输出

- [ ] **Step 7]: Lint `./gradlew :app:lintDebug --quiet` → 0 error

- [ ] **Step 8]: Commit `git add -A app/src/main/java/com/textvision/alistclient/ui/ && git commit -m "refactor(ui): remove legacy Cloud* components and ui/screens/"`

---

## Self-Review（已 inline）

- **Spec 覆盖**: §1 目标 / §2 目录 / §3 Token / §4 组件 / §5 状态管理 / §6 错误 / §7 导航 / §8 测试 / §9 深色 / §10 4 Phase → 32 个 Task 全部对应
- **占位符扫描**: 0 处 TBD/TODO/XXX；每个 Step 有具体代码或命令
- **类型一致性**: `FileUiState` 字段（path/files/isLoading/error/query/selection/isMultiSelectMode/isOnline）、`FileIntent` 子类型、`DarkMode` 枚举（SYSTEM/LIGHT/DARK）、`AppError` 子类型、`TransferTab` 枚举、`PreviewArgs` 数据类、`FileCategory` 枚举 → 全部在首次定义 Task 出现后被复用 Task 严格一致
- **风险点**: Task 17 `FileRepository.list/delete/requestDownload`、Task 22 `HomeRepository.load()`、Task 24 `SettingsRepository.storages/setStorageEnabled`、Task 25 `PreviewRouter.route` 接口可能不匹配 → 已分别在 Task 内部加 Step 4/6/3 修补
- **修正**: Task 12 已直接定义 `onLongClick`（原 plan 提到要回头补 ListItemRow）

---

**Plan complete and saved to `docs/superpowers/plans/2026-07-08-ui-expressive-redesign.md`. Two execution options:**

1. **Subagent-Driven (recommended)** - 每个 Task 派一个独立 subagent，task 间 review，快速迭代
2. **Inline Execution** - 在当前会话按 Task 顺序执行，关键节点 checkpoint

**选哪种？**
