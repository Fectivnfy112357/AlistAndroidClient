# Alist Android Client — UI Expressive 全面重做

- **日期**: 2026-07-08
- **作者**: 贾晓源
- **状态**: 设计已确认，待用户复核
- **前置**: `2026-06-26-alist-hyperos-ui-redesign-design.md`（v1 HyperOS 设计，本 spec 为 v2 视觉刷新）
- **目标**: Material 3 Expressive 全面视觉重做、动效、流畅度、配色、排版、可访问性、深色模式、视觉回归测试

---

## 1. 目标与定位

### 1.1 解决的问题

| # | 现状 | 影响 |
|---|---|---|
| 1 | SettingsScreen 600+ 行 / PreviewScreen 1295 行 / TransferScreen 730+ 行 | 维护难、测试难、合并冲突频繁 |
| 2 | 部分 Composable 用 `collectAsState` 而非 `collectAsStateWithLifecycle` | 屏幕关闭时仍消费数据，浪费电 |
| 3 | 自定义 `CloudScaffold` / `CloudBottomBar` 偏离 M3 | BottomBar 视觉不达 Material 3 标准 |
| 4 | 颜色和排版 token 不完整 | 一致性靠人工保证，新增屏幕容易风格漂移 |
| 5 | 动效仅有 `LinearEasing`，无 Spring | 交互生硬，没有 iOS / 高端 Android 的"物理感" |
| 6 | 错误展示分散（Banner / Dialog / Snackbar） | 用户认知成本高 |
| 7 | 暗黑模式覆盖逻辑缺失 | 用户无法手动切换 |
| 8 | 无视觉回归测试 | Token 一改全屏震动无感知 |

### 1.2 不做（YAGNI）

- 大屏双栏 / WindowSizeClass 适配
- 折叠屏
- 自定义字体（保持系统默认）
- 国际化文案整理（仅做 token 准备，文案留后续）
- Live Edit / Studio Bot 集成
- 手势导航之外的复杂手势

### 1.3 成功标准

1. 所有 7 个主 Screen（Login/File/Transfer/Settings/Preview/MoveCopyPicker/Home）在 1080p × 60Hz / 120Hz / 144Hz 三档下均流畅（jank < 1%）
2. 暗黑模式：跟随系统 / 浅色 / 深色 三档可手动切换并持久化
3. 每个 Screen 至少 3 个 `@Preview`（Light / Dark / 大字）+ 5-10 个 Unit Test + 关键流程 Compose UI Test
4. Roborazzi 视觉回归覆盖全部 7 个 Screen + 8 个组件，CI 必跑
5. 无 P0 / P1 bug，崩溃率 < 0.1%
6. 任意单文件 ≤ 400 行（强制约束）

---

## 2. 目录重组

```
ui/
  theme/                # token 全集
    Color.kt
    Type.kt
    Shape.kt
    Motion.kt
    Theme.kt
    ThemeRepository.kt  # DataStore 持久化 dark mode 覆盖
  foundation/           # 跨页通用基础设施
    Scaffold.kt         # 包装 M3 Scaffold + 系统栏适配
    AppBars.kt          # TopAppBar / BottomAppBar (M3)
    Backgrounds.kt
  components/           # 通用组件
    EmptyState.kt
    ErrorState.kt
    LoadingState.kt
    StatusBanner.kt
    SearchField.kt
    ActionButton.kt
    ListItemRow.kt
    FileTypeIcon.kt     # mime 映射 + Material Icons Extended
    Breadcrumb.kt
    AppAlertDialog.kt
  navigation/
    AppNavHost.kt
    AppRoute.kt         # @Serializable
    BottomNavBar.kt     # M3 NavigationBar
    NavTransitions.kt
  feature/              # 按业务分（替换 ui/screens/）
    auth/
      LoginScreen.kt
      LoginViewModel.kt
    home/
      HomeScreen.kt
      HomeViewModel.kt
    file/
      FileScreen.kt          # < 300 行
      FileListContent.kt
      FileDetailPane.kt
      FileMultiSelectBar.kt
      FileViewModel.kt
    preview/
      PreviewScreen.kt       # 入口 < 300 行
      PreviewImage.kt
      PreviewText.kt
      PreviewAudio.kt
      PreviewVideo.kt
      PreviewPdf.kt
      PreviewTooLarge.kt
      PreviewUnavailable.kt
      PreviewViewModel.kt
    transfer/
      TransferScreen.kt
      TransferListContent.kt
      TransferRow.kt
      TransferViewModel.kt
    settings/
      SettingsScreen.kt
      SettingsViewModel.kt
    storage/                 # 管理后台存储相关
      StorageEditScreen.kt
      ...
    picker/                  # MoveCopyPicker
      MoveCopyTargetPickerScreen.kt
```

**强制约束**：单文件 ≤ 400 行；超 400 行必须拆分（CI lint 检查）。

---

## 3. 设计 Token

### 3.1 Color — Material 3 Expressive 浅色 + 深色

**主色**：Indigo Blue `#1F6FEB`

**浅色**：

| Slot | Hex | 用途 |
|---|---|---|
| primary | #1F6FEB | 强调按钮 / FAB |
| onPrimary | #FFFFFF | 主色上的文字 |
| primaryContainer | #D8E4FE | 主色弱背景 |
| onPrimaryContainer | #001A41 | 主色弱背景上的文字 |
| secondary | #5A6478 | 次要强调 |
| secondaryContainer | #DEE3F2 | 次要容器 |
| tertiary | #7C5800 | 第三强调（金色） |
| tertiaryContainer | #FFDFA1 | 第三容器 |
| error | #BA1A1A | 错误 |
| errorContainer | #FFDAD6 | 错误容器 |
| background | #FEFBFF | 全屏背景 |
| onBackground | #1B1B1F | 背景上的文字 |
| surface | #FEFBFF | 表面 |
| onSurface | #1B1B1F | 表面文字 |
| surfaceVariant | #E1E2EC | 表面变体 |
| surfaceContainerLowest | #FFFFFF | |
| surfaceContainerLow | #F8F4FB | |
| surfaceContainer | #F2EFF4 | |
| surfaceContainerHigh | #ECE9EE | |
| surfaceContainerHighest | #E6E3E9 | |
| outline | #74777F | 描边 |
| outlineVariant | #C4C6D0 | 弱描边 |

**深色**：镜像浅色调整亮度（M3 规范自动推导）。

**Dynamic Color**：
- Android 12+：`dynamicLightColorScheme(LocalContext.current)` / `dynamicDarkColorScheme(...)`
- 低版本：回退静态 token
- 提供"使用动态取色"开关（默认开启，Settings 可关）

### 3.2 Type Scale — M3 Expressive 15 样式

字体：系统默认（Roboto / 中文系统字体）

| Role | Size/LineHeight | Weight | LetterSpacing |
|---|---|---|---|
| displayLarge | 57sp/64 | 400 | -0.25 |
| displayMedium | 45/52 | 400 | 0 |
| displaySmall | 36/44 | 400 | 0 |
| headlineLarge | 32/40 | 400 | 0 |
| headlineMedium | 28/36 | 400 | 0 |
| headlineSmall | 24/32 | 400 | 0 |
| titleLarge | 22/28 | 500 | 0 |
| titleMedium | 16/24 | 600 | 0.15 |
| titleSmall | 14/20 | 600 | 0.1 |
| bodyLarge | 16/24 | 400 | 0.5 |
| bodyMedium | 14/20 | 400 | 0.25 |
| bodySmall | 12/16 | 400 | 0.4 |
| labelLarge | 14/20 | 600 | 0.1 |
| labelMedium | 12/16 | 600 | 0.5 |
| labelSmall | 11/16 | 600 | 0.5 |

### 3.3 Shape

| Token | Value | 用途 |
|---|---|---|
| Corner.ExtraSmall | 4dp | Chip / 小标签 |
| Corner.Small | 8dp | Button / TextField |
| Corner.Medium | 12dp | Card / Dialog |
| Corner.Large | 16dp | 大 Card / Sheet |
| Corner.ExtraLarge | 28dp | TopAppBar / 大 Panel |

对应 `RoundedCornerShape(4.dp)` / `(8.dp)` / `(12.dp)` / `(16.dp)` / `(28.dp)`。

### 3.4 Motion

```kotlin
object CloudMotion {
  // Spring specs
  val SpringFast   = Spring(dampingRatio = 0.9f, stiffness = 1200f)  // 按钮反馈
  val SpringMedium = Spring(dampingRatio = 0.85f, stiffness = 600f)  // Tab 切换
  val SpringSlow   = Spring(dampingRatio = 0.8f, stiffness = 300f)   // 页面转场

  // Tween specs
  val TweenShort  = tween<Float>(120, easing = FastOutSlowInEasing)
  val TweenMedium = tween<Float>(240, easing = FastOutSlowInEasing)
  val TweenLong   = tween<Float>(400, easing = FastOutSlowInEasing)

  // 检测
  @Composable
  fun reducedMotion(): Boolean { ... }
}
```

**使用规则**：
- 列表 item 进入用 `AnimatedItemPlacement` + `SpringMedium`
- 状态切换用 `AnimatedContent` + `SpringMedium`
- 进度条用 `animateFloatAsState` + `SpringFast`
- 页面转场用 `TweenMedium`
- `Settings → 减少动效` 时全部退化为 `TweenShort`（可选，Phase 4 不做）

### 3.5 主题切换

`ThemeRepository`（DataStore 持久化）：
```kotlin
enum class DarkMode { SYSTEM, LIGHT, DARK }
```

- 默认 `SYSTEM`，跟随 `isSystemInDarkTheme()`
- 手动覆盖：`Settings → 主题 → 跟随系统 / 浅色 / 深色`
- Settings 切换即时生效，无需重启

---

## 4. 组件库

### 4.1 Foundation

| 组件 | 实现 | 备注 |
|---|---|---|
| `AppScaffold` | M3 `Scaffold` 包装 | 处理状态栏 / 导航栏 padding + WindowInsets |
| `AppTopBar` | M3 `TopAppBar` + `colors` | title / subtitle / navigationIcon / actions slot |
| `AppBottomBar` | M3 `NavigationBar` | 4 tab（Home / Files / Transfers / Settings），原 M3 indicator |
| `AppBackground` | Surface + 可选渐变 | Surface tint 基础 |

### 4.2 Components

| 组件 | API | 关键能力 |
|---|---|---|
| `EmptyState` | `(icon, title, message, action?)` | 居中，48dp icon，action 用 `TextButton` |
| `ErrorState` | `(message, onRetry?)` | 错误图标 + 重试按钮 |
| `LoadingState` | `()` | `CircularProgressIndicator` + 文案 |
| `StatusBanner` | `(kind, message, onAction?)` | kind: Info / Warning / Error / Success |
| `SearchField` | `(value, onChange, placeholder)` | M3 `SearchBar` 简化版 |
| `ActionButton` | Filled / Tonal / Outlined / Text 四种 | 高度 40dp，圆角 20dp |
| `ListItemRow` | `(leading, title, subtitle, trailing, onClick?)` | 替换 CloudListItem |
| `FileTypeIcon` | `(mime, size)` | Material Icons Extended 按 mime 映射 |
| `Breadcrumb` | `(path, onNavigate)` | 横滚 + 截断 |
| `AppAlertDialog` | `(title, message, confirm/dismiss)` | M3 `AlertDialog` 封装 |

**强制规则**：
- 全部使用 `MaterialTheme.colorScheme.*` 和 `MaterialTheme.typography.*`
- 圆角用 `MaterialTheme.shapes.*`
- 动效用 `CloudMotion.*`
- 不再保留 `Cloud*` 前缀私有组件，全部走 M3 slot
- 每个组件 ≥ 1 个 `@Preview`（Light + Dark + 大字）

### 4.3 FileTypeIcon 映射

| 类别 | 扩展名 | Icon | 容器色 |
|---|---|---|---|
| Folder | — | `Icons.Outlined.Folder` | `tertiaryContainer` |
| Image | jpg/png/gif/webp | `Icons.Outlined.Image` | `secondaryContainer` |
| Video | mp4/mkv/mov | `Icons.Outlined.Movie` | `primaryContainer` |
| Audio | mp3/flac/wav | `Icons.Outlined.MusicNote` | `secondaryContainer` |
| Text | txt/md | `Icons.Outlined.Description` | `surfaceContainerHigh` |
| Code | kt/java/py/js/ts | `Icons.Outlined.Code` | `surfaceContainerHigh` |
| Archive | zip/rar/7z/tar | `Icons.Outlined.Archive` | `tertiaryContainer` |
| PDF | pdf | `Icons.Outlined.PictureAsPdf` | `errorContainer` |
| Document | doc/docx | `Icons.Outlined.Article` | `surfaceContainerHigh` |
| Other | — | `Icons.Outlined.InsertDriveFile` | `surfaceContainerHigh` |

---

## 5. 状态管理 & 数据流

### 5.1 MVVM + UDF

```kotlin
@HiltViewModel
class FileViewModel @Inject constructor(
  private val repo: FileRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(FileUiState())
  val state: StateFlow<FileUiState> = _state.asStateFlow()

  fun onIntent(intent: FileIntent) = when (intent) {
    is FileIntent.Load -> load(intent.path)
    is FileIntent.Search -> search(intent.query)
    is FileIntent.MultiSelectToggle -> toggleSelect(intent.path)
    is FileIntent.MultiSelectClear -> clearSelection()
    is FileIntent.MultiSelectDelete -> deleteSelected()
    is FileIntent.MultiSelectDownload -> downloadSelected()
  }
}

@Composable
fun FileScreen(vm: FileViewModel = hiltViewModel()) {
  val state by vm.state.collectAsStateWithLifecycle()
  FileContent(state, onIntent = vm::onIntent)
}
```

### 5.2 强制规则

- Screen 永远 `collectAsStateWithLifecycle()`，不混用 `collectAsState`
- 不在 Composable 里直接调 `viewModel::method`，统一走 `onIntent`
- UiState 标 `@Stable` 或 `@Immutable`
- 计算属性用 `derivedStateOf`（如 `visibleFiles`）
- 多选状态进入 FileViewModel，不在 Composable 里 `remember`

### 5.3 UiState 模板

```kotlin
@Immutable
data class FileUiState(
  val path: String = "/",
  val files: List<FileItem> = emptyList(),
  val isLoading: Boolean = false,
  val error: AppError? = null,
  val query: String = "",
  val selection: Set<String> = emptySet(),
  val isMultiSelectMode: Boolean = false,
) {
  val visibleFiles: List<FileItem> get() = if (query.isBlank()) files else files.filter { ... }
  val isSelectionEmpty: Boolean get() = selection.isEmpty()
  val isAllSelected: Boolean get() = selection.size == visibleFiles.size
}
```

### 5.4 多选/手势

- 长按进入多选 → `FileViewModel.isMultiSelectMode = true`
- 顶部出现 `MultiSelectTopBar`（全选 / 反选 / 下载 / 删除 / 取消）
- 搜索结果嵌在多选模式内仍生效
- **不做**：Swipe 手势、拖拽排序

### 5.5 PreviewScreen 拆分

- 入口 `PreviewScreen` < 300 行
- 子组件各文件：`PreviewImage / PreviewText / PreviewAudio / PreviewVideo / PreviewPdf / PreviewTooLarge / PreviewUnavailable`
- 共享 `PreviewViewModel` 管理文件元数据 + 操作

### 5.6 TransferScreen 拆分

- `TransferScreen` 顶层：tab 切换 + 内容容器
- `TransferListContent(Upload | Download)` 渲染 LazyColumn
- `TransferRow` 单条记录（独立可测）
- 状态由 `TransferViewModel` 管（已存在，重构暴露单一 state）

---

## 6. 错误处理 & 加载态

### 6.1 统一错误模型

```kotlin
sealed interface AppError {
  data class Network(val cause: String?) : AppError
  data class Server(val code: Int, val message: String) : AppError
  data class Unauthorized(val reason: String) : AppError
  data class Unknown(val message: String) : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
  is IOException -> AppError.Network(cause = message)
  is HttpException -> when (code()) {
    401, 403 -> AppError.Unauthorized("请重新登录")
    else -> AppError.Server(code(), message())
  }
  else -> AppError.Unknown(message ?: "未知错误")
}
```

### 6.2 3 层错误展示

| 层 | 触发 | UI | 时机 |
|---|---|---|---|
| 1 Snackbar | 表单校验、瞬时失败 | `SnackbarHost` | 3-4s 自动消失 |
| 2 Banner | 列表顶部提示（不可恢复但可重试） | `StatusBanner(kind = Warning)` | 持久直到用户操作 |
| 3 全屏 | 网络断开 / 未授权 | `ErrorState` 整页 | 必须重试或返回 |

### 6.3 Loading 策略

| 场景 | 组件 | 细节 |
|---|---|---|
| 全屏初次加载 | `LoadingState` | 中心 spinner + 文案 |
| 下拉刷新 | `PullToRefreshBox` | M3 原生，顶部下拉触发 |
| 列表追加 | 底部 shimmer | 3 个骨架行 |
| 按钮提交 | `ActionButton(loading = true)` | spinner 替换文字 |
| 进度条 | `LinearProgressIndicator` | Spring 动画过渡 |

### 6.4 重试模式

- `ErrorState` 提供"重试"按钮 → 重发最近 Intent
- ViewModel 维护 `lastFailedIntent: Intent?` 便于重放
- Snackbar 错误自动加入"重试" action

### 6.5 未授权恢复

- 401 / 403 → 清空 `SessionManager` + 跳回登录
- 通过 `SessionEvent` Flow 在 `MainActivity` 层统一处理（不污染 ViewModel）

### 6.6 离线感知

- `NetworkMonitor`（已有） → `isOnline: StateFlow<Boolean>`
- 离线时：列表顶部常驻 `StatusBanner(kind = Warning, "当前离线")`
- 离线时禁用上传 / 下载按钮

---

## 7. 导航

### 7.1 Routes

```kotlin
@Serializable data object Login
@Serializable data class Files(val path: String = "/")
@Serializable data object Transfers
@Serializable data object Settings
@Serializable data object Home
@Serializable data class Preview(val payload: PreviewArgs)
@Serializable data class MoveCopyPicker(val op: String, val path: String)
@Serializable data class StorageEdit(val id: Int)
@Serializable data object AdminSiteSettings
```

### 7.2 NavHost

- 根 `NavHost(startDestination = if (loggedIn) Home else Login)`
- BottomBar 仅在 `Home / Files / Transfers / Settings` 路由显示
- 转场统一通过 `AppNavTransitions` 封装

### 7.3 转场

| From → To | 转场 | 动效 |
|---|---|---|
| Login → Main | 垂直 slide-up + fade | `TweenMedium` |
| Tab → Tab | 水平 slide | `SpringMedium` |
| Main → Detail | 水平 slide | `SpringMedium` |

---

## 8. 测试 & 预览

### 8.1 @Preview 规范

每个 Screen 至少 3 个 `@Preview`：

```kotlin
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", fontScale = 1.3f)
@Composable
private fun FileScreenPreview() {
  AlistClientTheme { FileContent(previewState(), onIntent = {}) }
}

private fun previewState() = FileUiState(
  path = "/",
  files = listOf(
    FileItem("Documents", "/Documents", isDir = true, size = 0),
    FileItem("photo.jpg", "/photo.jpg", isDir = false, size = 1024 * 1024),
  ),
  isLoading = false,
)
```

每个 Screen 至少 3 个状态：Loading / Empty / Success（数据）。
每个组件至少 2 个：Default / Pressed。

### 8.2 Compose UI Test

```kotlin
class FileScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test fun emptyState_showsEmptyMessage() {
    composeTestRule.setContent { AlistClientTheme { FileScreen() } }
    composeTestRule.onNodeWithText("文件夹为空").assertIsDisplayed()
  }

  @Test fun longPress_entersMultiSelectMode() {
    composeTestRule.onNodeWithText("file1.txt").performLongClick()
    composeTestRule.onNodeWithTag("multi_select_bar").assertIsDisplayed()
  }
}
```

### 8.3 测试覆盖范围

| 类型 | 目标 | 工具 |
|---|---|---|
| Unit | ViewModel intent → state | JUnit + Turbine + MockK |
| Compose UI | 关键流程 | Compose UI Test |
| Snapshot | 视觉回归 | Roborazzi |

### 8.4 视觉回归（Roborazzi）

- 引入 Roborazzi 作为首次引入
- 覆盖全部 7 个 Screen + 8 个组件 ≈ 15 张 baseline
- 失败时 PR 直接附图对比
- 容忍度 0.1%（容忍抗锯齿微小差异）
- CI 必跑 `./gradlew :app:verifyRoborazziDebug`
- 手动更新：`./gradlew :app:recordRoborazziDebug`

### 8.5 CI 集成

```bash
./gradlew :app:assembleDebug            # 编译
./gradlew :app:testDebugUnitTest        # 单元测试
./gradlew :app:lintDebug                # Lint
./gradlew :app:verifyRoborazziDebug     # 视觉回归
```

---

## 9. 深色模式 & 高帧适配

### 9.1 深色模式

- `ThemeRepository` 暴露 `Flow<DarkMode>`
- `AlistClientTheme` 根据值决定 `darkTheme` 参数
- 三档：`SYSTEM` / `LIGHT` / `DARK`
- Settings 提供切换入口
- 切换即时生效，不需重启

### 9.2 高帧适配

- 目标屏幕：60Hz / 90Hz / 120Hz / 144Hz
- 所有动画必须基于 `withFrameNanos` 或 Spring 框架（自动适配）
- 列表滚动 / LazyColumn 用 stable key + contentType 避免重组
- 进度条 / 进度环避免每帧 setState，使用 `animateFloatAsState`

### 9.3 流畅度指标

- 启动到首屏 < 1.5s（冷启动）
- 列表滚动 jank < 1%
- 转场动画不掉帧
- 监控：接入 Android Profiler / Macrobenchmark（Phase 4 评估）

---

## 10. 实现路径（4 Phase）

### Phase 1 — Token & Theme（3-4 天）

- `Color.kt`：浅 / 深 / Dynamic Color 全套
- `Type.kt`：15 个 M3 Expressive 文字样式
- `Shape.kt`：Corner.ExtraSmall / Small / Medium / Large / ExtraLarge
- `Motion.kt`：Spring + Tween + reducedMotion 检测
- `Theme.kt`：包 `AlistClientTheme` + `ThemeRepository`（DataStore）
- `ThemeRepository` 单测
- Roborazzi 集成 + 5 张 token baseline
- `git commit`：`feat(theme): m3 expressive tokens + dynamic color`

### Phase 2 — Foundation & Components（5-7 天）

- `foundation/`：`AppScaffold` / `AppTopBar` / `AppBottomBar` / `AppBackground`
- `components/`：8 个共享组件全部重写
- 每个组件 `@Preview`（Light / Dark / 大字）
- Roborazzi baseline 增至 15 张
- 旧 `Cloud*` 组件仍保留（Phase 4 收尾删）
- 各组件单测
- `git commit`：`feat(ui): foundation + components v2`

### Phase 3 — Screen 重做（7-10 天）

- 按 `feature/<name>/` 重构 6 个 Screen
- 拆分 `PreviewScreen` 为 7 个子组件
- 拆分 `TransferScreen` 为顶层 + 内容 + 行
- `AppError` + `toAppError` 统一错误模型
- `FileScreen` 加长按多选
- 每个 Screen UiState / Intent 命名规范
- ViewModel 单元测试覆盖（每个 Screen 5-10 个测试）
- Compose UI Test 关键流程
- Roborazzi baseline 增至 30 张
- `git commit`（按 Screen 拆分 6 次）：`refactor(<screen>): m3 expressive screen`

### Phase 4 — 体验增强 & 收尾（3-5 天）

- `NetworkMonitor` 接入离线 Banner
- `SnackbarHost` 全局化
- `ThemeRepository.darkModeOverride` 接入 Settings
- 下拉刷新 `PullToRefreshBox` 全 Screen
- 进度条 Spring 过渡
- 删除旧 `Cloud*` 组件
- 全部 Screen Preview 覆盖
- Roborazzi baseline 完整化
- CI 接入测试任务
- `git commit`：`feat(ui): polish + theme override + offline banner`

**总工期**：18-26 工作日。

---

## 11. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| Token 改动视觉大震动 | P2 组件全要调 | P2 起步先单独 Preview 验证 |
| `PreviewScreen` 拆错 | 回归风险 | 抽 `PreviewViewModel`，行为先单测再拆 |
| Roborazzi 误报 | CI 噪声 | 设 0.1% 容忍度 + 手动 `recordRoborazziDebug` |
| 拆分巨型文件引入 bug | 业务回归 | 拆分前先补单测，行为锁定 |
| Dynamic Color 与品牌色冲突 | 视觉漂移 | 默认 Indigo Blue，Dynamic 可关 |
| Material Icons Extended APK 体积 | +400KB | 可接受，监测 |

---

## 12. 验收标准

1. 所有 Screen 在浅 / 深 / Dynamic Color 三模式下截图一致通过
2. Roborazzi baseline 30+ 张，CI 全绿
3. 单文件 ≤ 400 行（CI 检查）
4. 每个 Screen 单元测试覆盖率 ≥ 70%
5. Compose UI Test 覆盖关键流程（多选 / 错误重试 / 暗黑切换）
6. 暗黑模式三档手动切换即时生效且持久化
7. 启动到首屏 < 1.5s
8. 列表滚动 jank < 1%
9. 无 P0 / P1 bug

---

## 13. 不在本 spec 范围

- 大屏双栏 / 折叠屏 / WindowSizeClass
- 自定义字体
- 国际化文案整理（仅准备 token 接入点）
- 性能监控基建接入（仅说明目标）
- 第三方设计稿 / Figma 同步