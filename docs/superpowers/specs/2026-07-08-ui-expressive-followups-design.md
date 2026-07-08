# UI Expressive 后续收尾 — 设计 Spec

> 范围:消化 `.superpowers/sdd/progress.md` 中 18 项遗留问题 (P0×4 + P1×6 + P2×8)
> 目标:把 UI Expressive 重设计 (Task 1-32) 留下的 UX/架构/清理缺口一次性收口
> 交付:1 spec + 1 plan,18 项各占一节,串行逐项实施,每项 `assembleDebug + lintDebug + testDebugUnitTest` 全绿 + 模拟器手动验证

---

## 0. 范围与方法

### 18 项一览 (来自 progress.md)

| # | 优先级 | 类别 | 任务 |
|---|--------|------|------|
| 1 | P0 | UX Bug | SnackbarHost + AppBottomNavBar 重叠修复 |
| 2 | P0 | Lost Features | FileScreen 补 upload / per-row download / share / copy direct link |
| 3 | P0 | UX Bug | FileListContent 字节数 humanize |
| 4 | P0 | 规范 | 补 25+ @Preview |
| 5 | P1 | 架构 | AppError 落地 + §6.5 未授权恢复链路 |
| 6 | P1 | 主题统一 | Cloud* 11 + DirectoryBrowser 全面替换 |
| 7 | P1 | 主题统一 | PreviewScreen 切 AppScaffold |
| 8 | P1 | 测试 | Roborazzi 补到 15+ baseline |
| 9 | P1 | 动效 | Spring 动效扩展 (Tab / 列表 item / AnimatedContent) |
| 10 | P1 | 测试 | TransferViewModel / SettingsViewModel 测试恢复 |
| 11 | P2 | 清理 | HomeScreen 包路径迁移 |
| 12 | P2 | 架构 | ViewModel 状态架构统一到 combine + stateIn |
| 13 | P2 | 清理 | isAllSelected 用 visibleFiles |
| 14 | P2 | 清理 | ActiveTransferStatuses 去重 |
| 15 | P2 | 清理 | AppDestination.mime 重命名 |
| 16 | P2 | 清理 | PreviewViewModel.textRepository 私有化 |
| 17 | P2 | 清理 | ModifierParameter / ObsoleteSdkInt 清理 |
| 18 | P2 | 清理 | TransferManager.kt:333 NewApi 处理 |

### 实施原则

1. **每项独立 commit**,标题统一 `chore(<scope>): <title>` 或 `feat(<scope>): <title>`
2. **每项结束**:`./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` 全绿
3. **P0/P1 项**额外:在 `test_avd` 模拟器 (`.superpowers/specs` GPU 软件渲染) 手动验证
4. **依赖关系**:
   - P1-5 (AppError) 必须在 P0-1 (Snackbar) 之后:Snackbar 接入后才有错误展示消费者
   - P1-6 (Cloud* 替换) 在 P0-4 (@Preview) 之后:替换后再补 Preview 防止双重改动
   - P1-7 (PreviewScreen) 在 P1-6 之后:共用 Cloud* 替换路径
   - P2-12 (VM 统一) 在 P1-10 (测试恢复) 之后:先恢复测试基线再统一模式
5. **不重写**:每项只动必要代码,不趁机扩大改动 (例:P0-3 不改 FileListContent 整体结构)
6. **worktree**:不需要,18 项改动域小,main 直接逐项 commit

---

## 1. SnackbarHost + AppBottomNavBar 重叠修复 (P0)

### 问题
`navigation/AppNavHost.kt` 当前结构:
```kotlin
Box(Modifier.fillMaxSize()) {
    Scaffold(snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } }) { innerPadding ->
        NavHost(... modifier = Modifier.fillMaxSize().padding(innerPadding), ...)
    }
    AppBottomNavBar(navController, Modifier.align(Alignment.BottomCenter))
}
```
Scaffold 不知道 `AppBottomNavBar` 存在,Snackbar 默认显示在 Scaffold bottom,会被 BottomBar 视觉覆盖。Task 29 报告已知,Task 30/31 沿用待修。

### 设计
**采用方案 A**:把 `AppBottomNavBar` 移入 `Scaffold.bottomBar` slot。

```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = { AppBottomNavBar(navController) },
) { innerPadding ->
    NavHost(... modifier = Modifier.padding(innerPadding), ...)
}
```

- 删除外层 `Box`
- `AppBottomNavBar` 内部已用 `NavigationBar` 组件,无需改
- `MainActivity.kt` 仍保留 `LocalSnackbarHostState` CompositionLocal (供未来 ViewModel 注入)
- 文件:`navigation/AppNavHost.kt` (改),`MainActivity.kt` 不变

### 验收
- `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` 全绿
- 模拟器 `test_avd` 启动,登录后底部栏可见,Tab 切换正常
- 未来 Snackbar 消费者接入时不被 BottomBar 遮挡 (注:本任务不强制加消费者)

---

## 2. FileScreen Lost Features 回归 (P0)

### 问题
Task 18 重做 FileScreen 时丢失:① AppBar 上传入口 ② 每行下载/分享/复制直链 ③ 字节数未 humanize (本节先做 ①②,字节数在 §3)。

### 设计
**(a) Upload 入口**
- `FileScreen` TopBar 加 `IconButton(Icons.AutoMirrored.Filled.ArrowBackward) -> 选择文件` (实际为 `Icons.Default.Upload`)
- 点击后启动 `ActivityResultContracts.GetContent("*/*")`,用户选文件后:
  - 计算目标路径 = `currentPath + file.name`
  - 调 `TransferManager.enqueueUpload(uri, targetPath)` (沿用现有上传通道)
  - 成功 Snackbar "已加入传输队列" (暂不发,只通过 TransferManager 内部通知;后续 §5 接 SessionEvent 后统一)
- 错误时 Snackbar "上传失败"

**(b) 每行更多菜单**
- `ListItemRow` 已支持 `onMoreClick` 入口 (检查现有签名,若无则扩展)
- 每行点击 `more_vert` 弹出 `DropdownMenu`,3 项:
  - "下载" → `TransferManager.enqueueDownload(file)` + Snackbar "已加入下载队列"
  - "分享" → `Intent.ACTION_SEND` + `Intent.createChooser`,携带 `downloadUrl` 作为 `EXTRA_TEXT`
  - "复制直链" → `ClipboardManager.setPrimaryClip(ClipData.newPlainText("link", downloadUrl))` + Snackbar "已复制直链"

### 验收
- 测试:`FileScreenTest` 加 3 个用例:upload 触发、per-row download 触发、copy link 触发
- 模拟器:登录 → 文件列表 → 点上传 → 选择图片 → 确认加入传输 → 切到 Transfer Tab 可见任务
- 每行 `more_vert` → 弹出菜单 → 选复制直链 → 系统提示"已复制"

---

## 3. 字节数 Humanize (P0)

### 问题
`FileListContent` 直接显示原始字节数,如 `12345678 bytes`,人类不可读。

### 设计
新增 `util/FileSizeFormatter.kt`:
```kotlin
object FileSizeFormatter {
    fun humanize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
        else -> "%.2f GB".format(bytes / 1024.0 / 1024 / 1024)
    }
}
```
- 阈值与 Alist 官方客户端一致 (B / KB / MB / GB)
- `FileListContent` 替换 `bytes` 显示为 `FileSizeFormatter.humanize(file.size)`
- `PreviewScreen` 也用同一工具替换 (Task 25 拆分时显示原始字节数,需同步修)

### 验收
- 测试:`FileSizeFormatterTest` 5 个用例:0 / 512 / 2048 / 5242880 / 10737418240
- 模拟器:文件列表大小栏显示 "11.8 MB" 而非 "12345678"

---

## 4. 补 25+ @Preview (P0)

### 问题
整 main 源码零 `@Preview`,spec §8.1 强制要求。

### 设计
按"主题组件 → 通用组件 → 屏幕 shell"三档铺 25+ @Preview:

**主题 token (5 个)**:
- `theme/ColorPreview.kt` → LightColors / DarkColors / DynamicColors
- `theme/TypePreview.kt` → DisplayLarge / BodyMedium / LabelSmall
- `theme/ShapePreview.kt` → AppShapes (Card / Button / Dialog)
- `theme/MotionPreview.kt` → HyperOsMotion (SpringFast / SpringMedium / emphasized)

**通用组件 (12 个)**:
- AppScaffold / AppTopBar / AppBottomNavBar / StatusBanner (Info/Warning/Error/Success) / AppAlertDialog / EmptyState / ErrorState / LoadingState / ListItemRow / ActionButton (Primary/Secondary/Tonal) / SearchField / FileTypeIcon (6 文件类型各 1,共 6,选 Folder/Pdf/Image/Audio/Video/Other 6 个,合计 +6)

**屏幕 shell (5+ 个)**:
- LoginScreen (空状态) / FileScreen (空 + loading) / HomeScreen (loading) / SettingsScreen / TransferScreen (空)

每个 Preview 用 `AlistClientTheme { ... }` 包裹,提供 `darkTheme = false/true` 两个变体 (光暗各算 1 个时,数量 ×2 也可)。

### 验收
- 数量统计:`@Preview` 注解数 ≥ 25 (`grep -rn '@Preview' app/src/main/java | wc -l`)
- AS 中每张 Preview 可渲染,不报错
- 不引入新依赖 (Compose Preview runtime 已含)

---

## 5. AppError 落地 + §6.5 未授权恢复 (P1)

### 问题
Task 16 定义 `AppError` sealed model,但无人消费。`HomeViewModel` 在 401 时手动调 `authRepository.logout()`,各 VM 重复样板。

### 设计
**(a) SessionEvent 流**
- 新增 `auth/SessionEvent.kt`:
  ```kotlin
  sealed interface SessionEvent {
      data object Unauthorized : SessionEvent  // 401
      data object NetworkDown : SessionEvent   // 无网络
  }
  ```
- `AuthRepository` 暴露 `val sessionEvents: SharedFlow<SessionEvent> = MutableSharedFlow<SessionEvent>().asSharedFlow()`
- 提供 `suspend fun emit(event: SessionEvent)` (internal)
- 新增 `auth/SessionGate.kt` (`@Singleton`):
  - `init { sessionEvents.collect { when (it) { Unauthorized -> { clearSession(); _navEvent.emit(Login) }; NetworkDown -> _networkDown.update(true) } } }`
  - 暴露 `val navEvent: SharedFlow<AppRoute>` 与 `val isNetworkDown: StateFlow<Boolean>`

**(b) Retrofit 拦截**
- `AuthInterceptor` 检测到 401 (response.code == 401 或 body 含 "unauthorized") 时,调 `sessionRepository.emit(Unauthorized)` 后抛 `AppError.Unauthorized`
- `NetworkMonitor` 检测断网时 emit `NetworkDown`

**(c) 消费端**
- `MainActivity` 注入 `SessionGate`,在 `setContent` 内 `LaunchedEffect` 收集 `navEvent`,触发 `navController.navigate(LoginDest)`
- `AppError` 在 ViewModel 内 catch 后统一映射 `Result<AppError>` → 渲染到 `StatusBanner`

### 验收
- 测试:`AuthInterceptorTest` 加 `401_emits_unauthorized_event`,`SessionGateTest` 加 `unauthorized_clears_session_and_emits_nav`
- 模拟器:登录后用 token 失效模拟 401 → 自动跳回 Login 页

---

## 6. Cloud* 11 + DirectoryBrowser 全面替换 (P1)

### 问题
admin/storage,admin/cookie,admin/form,home,preview,DirectoryBrowser 仍 import Cloud*。Task 15 标 @Deprecated,Task 32 保守未删。

### 设计
**逐步替换路径**:
1. `admin/storage/StorageRowItem.kt`: `CloudListItem` → `ListItemRow`,`CloudPrimary` → `MaterialTheme.colorScheme.primary`
2. `admin/cookie/WebCookieDialog.kt`: `CloudBackground/Surface/TextPrimary/Primary` → `MaterialTheme.colorScheme.{background,surface,onSurface,primary}`
3. `admin/form/DynamicFormField.kt`: `CloudSurfaceMuted` → `colorScheme.surfaceContainerHigh`
4. `home/HomeScreen.kt`: `CloudScaffold/TopBar/...` → `AppScaffold/AppTopBar`
5. `preview/*`: 见 §7
6. `DirectoryBrowser.kt`: 重写为 `LazyColumn + ListItemRow + Breadcrumb`,删 `CloudScaffold/TopBar/ListItem` 引用
7. **删除 `Cloud*.kt` 11 个文件** (Card/ActionButton/AlertDialog/ListItem/Scaffold/SearchBar/StatusBanner/TopBar/EmptyState/RoundIconButton/PillButton)
8. **删除 `Color.kt` 5 个 @Deprecated alias** (CloudBackground/CloudPrimary/CloudSurface/CloudSurfaceMuted/CloudTextPrimary)

### 验收
- `grep -rn 'Cloud' app/src/main/java` = 0 (除注释/CHANGELOG)
- 全套测试 + lint 绿
- 模拟器:Storage/Admin/Preview/Picker 四屏视觉无回归

---

## 7. PreviewScreen 切 AppScaffold (P1)

### 问题
`preview/PreviewScreen.kt` 仍 import `CloudCard/Scaffold/TopBar`,主题色被锁死。

### 设计
- `CloudScaffold` → `AppScaffold`
- `CloudTopBar` → `AppTopBar(title=name, onBack=onBack, actions=...)`
- `CloudCard` → `Card(modifier, shape=AppShapes.Card, colors=CardDefaults.cardColors())` 或 `Surface`
- `PreviewAudio/PreviewFallback` 内的 `CloudPrimary/PrimarySoft/SurfaceMuted/TextPrimary` 全部走 `MaterialTheme.colorScheme`
- 删除 `PreviewScreen.kt` 顶部 `import com.textvision.alistclient.ui.components.Cloud*`

### 验收
- 模拟器:打开任意预览 (图片/PDF/视频/音频) → 顶栏主题色与 Settings 一致,暗黑模式切换无锁死
- 全套测试 + lint 绿

---

## 8. Roborazzi 补到 15+ baseline (P1)

### 问题
当前 6 张 baseline (FileScreen empty/success/dark + Home 3),spec §8.4 标 15-30+。

### 设计
新增 9+ baseline (FileScreenSnapshotTest 扩展,HomeScreenSnapshotTest 新建,SettingsScreenSnapshotTest 新建,TransferScreenSnapshotTest 新建):
- FileScreen loading / FileScreen error / FileScreen selection-mode (3 张)
- HomeScreen empty / HomeScreen loaded (2 张)
- SettingsScreen light / SettingsScreen dark (2 张)
- TransferScreen empty / TransferScreen with-tasks (2 张)
共 15+。

### 验收
- 跑 `./gradlew :app:recordRoborazziDebug` 后 15+ PNG 生成
- `app/src/test/snapshots/images/` 路径有对应基线
- 文件 ≤ 100KB

---

## 9. Spring 动效扩展 (P1)

### 问题
仅 `TransferProgress` 用 `SpringFast`,其余位置零 Spring。

### 设计
- `Tab` 切换 → `AnimatedContent` + `SpringMedium`
- `ListItemRow` 进入/退出 → `AnimatedVisibility` + `SpringFast`
- `AppBottomNavBar` 选中指示器 → `animateColorAsState(spring)`
- 统一从 `HyperOsMotion.SpringFast / SpringMedium / emphasized` 取,不写魔法数字

### 验收
- 模拟器:Tab 切换流畅无突变,ListItem 出现有滑动感
- 视觉无回归 (对比 progress.md commit `c05bd9d` 基线)

---

## 10. TransferViewModel / SettingsViewModel 测试恢复 (P1)

### 问题
Task 23/24 删除部分测试覆盖。

### 设计
**TransferViewModelTest** 新增:
- `refresh_triggers_reload` (mock `TransferDao.observeAll` emit → refresh → 验证 Dao.getSnapshot 调用)
- `filter_active_returns_only_running` (3 条混合状态 + filter=ACTIVE → 2 条)
- `retry_transitions_status` (FAILED → RETRYING → RUNNING)

**SettingsViewModelTest** 新增:
- `toggle_dark_mode_updates_repository`
- `loadAdminDataPopulates` (从 progress.md 恢复)
- `toggleStorageFailureSurfaces` (从 progress.md 恢复)

### 验收
- `testDebugUnitTest` 通过,新增 ≥ 6 测试
- 覆盖率回到 5+ 测试类

---

## 11. HomeScreen 包路径迁移 (P2)

### 问题
`home/HomeScreen.kt` 仍在顶层 `home/` 包,未迁 `ui/feature/home/`。

### 设计
- `mv app/src/main/java/.../home/*.kt app/src/main/java/.../ui/feature/home/`
- `package com.textvision.alistclient.home` → `package com.textvision.alistclient.ui.feature.home`
- `AppNavHost.kt` import 调整

### 验收
- `grep -rn 'com.textvision.alistclient.home' app/src` = 0 (除 git history)
- 全套测试 + lint 绿

---

## 12. ViewModel 状态架构统一到 combine + stateIn (P2)

### 问题
4 个 VM 用了 3 套不同模式:`combine+stateIn` (TransferViewModel/FileViewModel/HomeViewModel) / `MutableStateFlow+asStateFlow` / 无 StateFlow (PreviewViewModel)。

### 设计
统一规则:
1. **必须有 `val uiState: StateFlow<UiState>`**
2. 初始空状态由 `combine(...).stateIn(scope, SharingStarted.Eagerly, UiState.Empty)`
3. Intent 模式 (`fun onIntent(intent: Intent)`) 可选,但 ViewModel 内部状态变更走私有 MutableStateFlow
4. 派生属性 `derivedStateOf` 在 Composable 层,不在 VM

迁移清单:
- `SettingsViewModel`:`MutableStateFlow + asStateFlow` → `combine + stateIn`
- `PreviewViewModel`:Task 25 报告有意不强行 StateFlow,本任务**不强行改**,保持现状,在 spec 中标注"已知偏离"

### 验收
- 全套测试 + lint 绿
- `SettingsScreen` 行为无变化

---

## 13. isAllSelected 用 visibleFiles (P2)

### 问题
`FileViewModel.isAllSelected` 用 `files` 而非 `visibleFiles`,过滤时全选状态不准。

### 设计
- 改为 `visibleFiles`
- 同步 `onSelectAll` toggle 行为用 visibleFiles
- 加测试 `selectAll_toggle_with_filter_applied`

### 验收
- `FileViewModelTest` 新增 1 测试
- 模拟器:输入过滤词 → 全选 → 仅勾选过滤后可见文件

---

## 14. ActiveTransferStatuses 去重 (P2)

### 问题
`TransferScreen` 内 `ActiveTransferStatuses` 重复定义,与 `transfer/TransferStatus.kt` 同名常量重复。

### 设计
- 删 `TransferScreen` 内本地常量,改 import `TransferStatus.activeSet()`
- 提供 `TransferStatus.Companion.activeStatuses: Set<TransferStatus>` 单一来源

### 验收
- `grep -rn 'ActiveTransferStatuses' app/src` = 0
- 全套测试 + lint 绿

---

## 15. AppDestination.mime 重命名 (P2)

### 问题
`PreviewDestArgs.mime` 当前承载 `FileType.name` (Task 20 桥接),语义不准。

### 设计
- 字段 `mime: String` → `fileTypeName: String`
- `AppNavHost` 同步调整 (`args.mime` → `args.fileTypeName`)
- 这是 task 20 桥接 cleanup

### 验收
- 编译过 + 预览可打开

---

## 16. PreviewViewModel.textRepository 私有化 (P2)

### 问题
`PreviewViewModel` 的 `textRepository` 字段为 `internal`/public,应 private。

### 设计
- 改为 `private val textRepository: PreviewTextRepository`
- `enqueueDownload` 内部访问不变
- 加 `@VisibleForTesting` companion 工厂方法供测试访问

### 验收
- 全套测试 + lint 绿
- `PreviewViewModelTest` 不破

---

## 17. ModifierParameter / ObsoleteSdkInt 清理 (P2)

### 问题
9 条 `ModifierParameter` warning + 1 条 `ObsoleteSdkInt` (`TransferNotificationController.kt:21` `SDK_INT < O` 永远 false)

### 设计
- `TransferNotificationController.kt:21` 删 SDK 检查块 (minSdk=26,O=26)
- 9 条 ModifierParameter warning 由 lint baseline 移除:逐文件加 `@Suppress("ModifierParameter")` 仅在参数名无意义时,否则改 `modifier: Modifier = Modifier` 默认值
- 跑 `lintDebug` 后清 lint-baseline.xml 对应行

### 验收
- `lintDebug` 无新 warning
- `lint-baseline.xml` 缩至少 5 行

---

## 18. TransferManager.kt:333 NewApi 处理 (P2)

### 问题
`@RequiresApi(Build.VERSION_CODES.O)` 在 minSdk=26 下永远满足,标注是 pre-existing 噪声。

### 设计
- 删 `@RequiresApi(O)` 注解
- 若代码块在 API < O 路径不可达,确认后删整段 SDK 检查
- 不引入新 SDK 检查

### 验收
- 编译过 + TransferManager 单测全绿

---

## 19. 验收总览

每项结束命令:
```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

P0/P1 项额外手动:
```bash
MSYS_NO_PATHCONV=1 "D:/programming/devtools/android/sdk/platform-tools/adb.exe" shell am start -n com.textvision.alistclient/.MainActivity
```

进度追踪更新 `.superpowers/sdd/progress.md`,每完成一项追加一行 `Task N: complete (...)`。

---

## 20. 风险与回滚

| 风险 | 缓解 |
|------|------|
| P1-6 Cloud* 替换牵动 6 屏视觉 | 替换前保留原文件备份在 git;出问题时 `git revert` 单项 commit |
| P1-5 AppError 链路跨多个 VM | 先在 HomeViewModel 单点验证,再扩到 Settings/Transfer |
| P0-2 FileScreen upload 触发需要 SAF 权限 | 沿用现成 `ActivityResultContracts.GetContent`,不引新依赖 |
| 18 项总跨度大 | 每项独立 commit + 独立测试,任一项失败可单独回滚 |