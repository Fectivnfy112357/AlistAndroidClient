# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 用中文与用户对话。
> alist测试地址是 http://textvision.top:5244
> 账密是 fectivnfy/Yishengaini12345
> 探索代码库优先使用 gitnexus

## 本地开发环境（Windows 11）

- **操作系统**: Windows 11 Pro
- **Android SDK**: `D:\programming\devtools\android\sdk`（`local.properties` 已指向）
  - Platforms: `android-34`（项目 compileSdk / targetSdk = 34）
  - Build-tools: `34.0.0`
  - cmdline-tools: `latest/`
  - platform-tools、emulator、system-images 已安装

### 模拟器启动（test_avd）

```bash
# 默认启动（可能黑屏，窗口 GPU 渲染故障）
emulator -avd test_avd -no-snapshot

# 推荐：软件渲染，避免宿主 GPU 兼容问题导致模拟器窗口黑屏
emulator -avd test_avd -no-snapshot -gpu swiftshader_indirect
```

**黑屏诊断**：如果应用确认在前台（`adb shell dumpsys activity activities | grep ResumedActivity`）且 logcat 无 FATAL，但模拟器窗口全黑——是模拟器宿主 GPU 渲染失败，**不是应用 bug**。用 `-gpu swiftshader_indirect` 重启即可。

**截图路径**：Git Bash 下 `/sdcard` 会被转换成本地路径，必须加 `MSYS_NO_PATHCONV=1` 前缀：
```bash
MSYS_NO_PATHCONV=1 adb shell screencap -p //sdcard/scr.png
MSYS_NO_PATHCONV=1 adb pull //sdcard/scr.png ./scr.png
```

## Project Overview

Native Android client (MVP) for [Alist v3](https://github.com/AlistGo/alist) — a multi-storage file management server. Single-module Kotlin app targeting Android 8.0+ (minSdk 26, targetSdk 34), built with Jetpack Compose and Material3. 2026-07-08 完成 **UI Expressive 全面重做**（见 `docs/superpowers/specs/2026-07-08-ui-expressive-redesign-design.md`）—— 旧的 `Cloud*` 组件已删除，统一迁移到 M3 通用组件（`ActionButton` / `EmptyState` / `StatusBanner` / `AppAlertDialog` 等），屏幕按 `ui/feature/<name>/` 重组。

- **Package / Application ID**: `com.textvision.alistclient`
- **Alist API base URL**: 默认 `http://127.0.0.1:5244/`，登录可改。绑定见 `di/AppModule.kt:NetworkModule`。
- **Navigation 路由**：基于 Navigation Compose 2.8 + kotlinx.serialization 的类型安全目标，定义在 `navigation/AppDestination.kt`（`LoginDest` / `HomeDest` / `FilesDest` / `TransfersDest` / `SettingsDest` / `PreviewDest` / `MoveCopyPickerDest` / `StorageEditDest` / `AdminSiteSettingsDest`）。旧 `AppRoute` 字符串路由已废弃。
- **MVP 限制**：无后台可靠传输/断点续传、仅竖屏、单账号、不支持自签 HTTPS。详见 `docs/testing/known-limitations.md`。

## Build & Test Commands

所有命令从项目根目录执行，使用 Gradle wrapper（Windows 下 `gradlew.bat`）。

```bash
# 构建 Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# 完整校验：构建 + Lint + 单元测试
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest

# 设备端仪表测试（需连接设备/模拟器）
./gradlew :app:connectedDebugAndroidTest

# 仅 Lint
./gradlew :app:lintDebug

# 单测单个类
./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest

# 单测单个方法
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileViewModelTest.someMethod"
```

栈：Kotlin 2.0.21 · AGP 8.7.2 · Compose BOM 2024.09.03 · Hilt 2.52 · Retrofit 2.11 · OkHttp 4.12 · Room 2.6.1 · Coil 3.0.4 · Navigation Compose 2.8.3 · Material3 1.3.0 · JVM target 17。所有版本集中在 `gradle/libs.versions.toml`。

**没有 ktlint/detekt** —— 静态检查只跑 Android Lint。

### Gradle 陷阱

- `dependencyResolutionManagement` 使用 `FAIL_ON_PROJECT_REPOS` —— **禁止** 在子模块 `build.gradle.kts` 中写 `repositories {}` 块。
- 所有依赖版本集中在 `gradle/libs.versions.toml`，新增依赖也改这里。

## Architecture

单模块 MVVM Android 应用。`MainActivity` 是唯一的 `@AndroidEntryPoint` Activity，承载 `AppNavHost` Composable。状态流：View → ViewModel（Hilt 注入）→ Repository → API/Room。异步用 Coroutines + Flow；HTTP 用 Retrofit/OkHttp；持久化用 Room（`transfer_tasks.db`）+ EncryptedSharedPreferences 保存凭据。`TransferManager` 用 `@ApplicationScope` 协程作用域与可注入的 `TransferExecutor`（上传限流 2、下载限流 3，通过 `Semaphore`）。

### 关键入口（`app/src/main/java/com/textvision/alistclient/`）

- `AlistClientApp.kt` — `@HiltAndroidApp` Application，安装 `SafeCrashHandler`。
- `MainActivity.kt` — 唯一 Activity，启用 edge-to-edge、确保通知通道、用 `ThemeRepository` 注入主题，提供 `LocalSnackbarHostState` CompositionLocal。
- `di/AppModule.kt` — 所有 Hilt 绑定（`CredentialModule` / `DatabaseModule` / `NetworkModule`）+ `@IoDispatcher` / `@ApplicationScope` 限定符。
- `navigation/` — `AppDestination.kt`（类型安全 @Serializable 路由）、`AppNavHost.kt`、`BottomNavBar.kt`（M3 NavigationBar）、`AppNavTransitions.kt`（CloudMotion）。底部栏仅在 Home/Files/Transfers/Settings 显示。
- `common/result/ApiResult.kt` — 密封类 `Success/Failure/NetworkError`，所有 API 调用的统一包装。
- `transfer/TransferManager.kt` — 上传/下载协调器；`TransferExecutor`（接口）+ `RealTransferExecutor`（实现）已抽取为可注入依赖。

### UI 架构（重要 — 2026-07-08 重做后）

```
ui/
├── theme/        # Material3 主题（Color/Type/Shape/Motion CloudMotion/Theme）
├── foundation/   # Scaffold、AppBars、Backgrounds
├── components/   # 10 个 M3 共享组件：ActionButton、AppAlertDialog、Breadcrumb、EmptyState、ErrorState、FileTypeIcon、ListItemRow、LoadingState、SearchField、StatusBanner + ComponentPreviews
├── common/       # 跨特性 Composable 工具
└── feature/      # 按业务功能切分
    ├── auth/         # LoginScreen
    ├── home/         # HomeScreen + HomeRepository + HomeViewModel + dto/SectionKey/HomeSections/HomeStorageSection/HomeUiState/HomeRepositoryContract
    ├── file/         # FileScreen + FileListContent + FileMultiSelectBar + FileIntent/UiState/ViewModel
    ├── transfer/     # TransferScreen + TransferListContent/Progress/Row + TransferViewModel
    ├── settings/     # SettingsScreen + SettingsViewModel
    ├── preview/      # PreviewScreen + Audio/Image/Text/Fallback + PreviewViewModel
    ├── picker/       # MoveCopyTargetPicker
    ├── admin/        # AdminScreen + AdminViewModel
    ├── storage/      # StorageEditScreen
    └── auth + admin 下的 cookie/form/settings/storage 子模块
```

**已删除**：`ui/screens/` 目录、旧 `Cloud*` 前缀私有组件（`CloudScaffold` / `CloudTopBar` / `CloudBottomBar` / `CloudListItem` / `CloudCard` / `CloudSearchBar` / `CloudEmptyState` / `CloudStatusBanner` / `CloudActionButton` / `CloudAlertDialog`）、`BreadcrumbBar.kt`、`DirectoryBrowser.kt`、`TransferProgress.kt`。组件统一用 `MaterialTheme.colorScheme`/`typography`/`shapes`，圆角来自主题，动画来自 `CloudMotion`。每个组件至少一个 `@Preview`（亮/暗主题）。

### 业务模块包

- `auth/` — 登录流（AuthRepository / SessionManager / LoginViewModel / SessionGate）+ `model/`。
- `admin/` — Alist 后台管理（Settings/Storage/Cookie/Form 子模块，`AdminRepository` + `AdminResult`）。新增 `AdminSiteSettingsDest` / `StorageEditDest` 路由。
- `common/` — `crash/`（SafeCrashHandler）、`error/`（错误映射）、`network/`（NetworkMonitor）、`result/`（ApiResult）。
- `data/local/` — Room `AppDatabase` + DAO（库名 `transfer_tasks.db`）。
- `data/secure/` — `CredentialStore` 接口 + `EncryptedCredentialStore`（EncryptedSharedPreferences）。
- `file/` — FileRepository / FileViewModel / CopyMoveUseCase / FileNameValidator + `model/`。
- `network/` — `api/AlistApi`、`AuthInterceptor` + `AuthTokenProvider` + `SkipAuthRetry` 注解、`dto/`。
- `preview/` — PreviewRouter / MimeTypeResolver / PreviewFileStore / PreviewTextRepository。
- `transfer/` — `TransferManager` / `TransferNotificationController` / `TransferExecutor`(接口) / `RealTransferExecutor` / `TransferProgressRequestBody` / `TransferProgressResponseBody` + `data/`(TransferDao/Entity) + `model/`(TransferStatus/Type) + `LocalDownloadNamer` / `UriDisplayNameResolver`。
- `util/` — `ServerUrlNormalizer` 等。

## Testing

**框架**：JUnit 4 · MockK · Turbine · Robolectric · MockWebServer · Coroutines Test · Room Testing · Compose UI Test · AndroidX Test · Espresso · Roborazzi（视觉回归快照）。

**约定**：
- 单元测试在 `app/src/test/java/com/textvision/alistclient/<mirror-package-path>/`，命名 `<Subject>Test.kt`。MockK：`@MockK` / `mockk()` / `every { }` / `coEvery { }` / `verify { }`。
- Flow 用 Turbine（`flow.test { … }`）；协程测试用 `StandardTestDispatcher` + 显式 `advanceUntilIdle()`。
- HTTP 层用 MockWebServer。
- 覆盖 ~30 个类：auth / common / data / di / file / navigation / network / preview / transfer / ui/screens / util。
- UI 关键流程有 Compose UI 测试 + Roborazzi 亮/暗模式快照。

## GitNexus — Code Intelligence

项目已被 GitNexus 索引为 **alist**（3921 符号、8040 关系、300 执行流）。优先用 GitNexus MCP 工具理解代码、评估影响、安全导航。

> 索引可能陈旧：陈旧告警时在终端跑 `node .gitnexus/run.cjs analyze`（自动选择可用 runner）。无 `run.cjs` 用 `npx gitnexus analyze`（npm 11 崩溃时回退 `npm i -g gitnexus`；#1939）。

### 必做

- **修改任何符号前** 必须先跑 `impact({target: "symbolName", direction: "upstream"})` 并向用户报告爆炸半径（直接调用者、影响流程、风险等级）。
- **提交前** 必须跑 `detect_changes()` 验证改动只影响预期符号和执行流。回归审查对比默认分支：`detect_changes({scope: "compare", base_ref: "main"})`。
- `impact` 返回 **HIGH / CRITICAL** 风险时必须警告用户。
- 探索陌生代码用 `query({query: "concept"})` 找执行流，不要 grep。
- 查具体符号的调用方/被调方/参与流程用 `context({name: "symbolName"})`。

### 禁止

- 禁止不跑 `impact` 就修改任何函数/类/方法。
- 禁止忽略 `impact` 的 HIGH/CRITICAL 告警。
- 禁止 find-and-replace 重命名 —— 用 `rename`（理解调用图）。
- 禁止不跑 `detect_changes()` 就提交。

### 资源

| 资源 | 用途 |
|------|------|
| `gitnexus://repo/alist/context` | 代码库概览、索引新鲜度 |
| `gitnexus://repo/alist/clusters` | 全部功能簇 |
| `gitnexus://repo/alist/processes` | 全部执行流 |
| `gitnexus://repo/alist/process/{name}` | 单流程逐步追踪 |

### CLI

| 任务 | 读这个 skill 文件 |
|------|------------------|
| 理解架构 / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| 爆炸半径 / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| 追踪 bug / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| 重命名 / 抽取 / 拆分 / 重构 | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| 工具、资源、Schema 参考 | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| 索引、状态、清理、wiki CLI | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

## Development Workflow

遵循 `docs/superpowers/` + `.superpowers/sdd/` 文档化的 spec → plan → task → report 流程。每个非平凡特性在 `docs/superpowers/specs/` 有设计文档、配对 `docs/superpowers/plans/` 的实施计划。接手非平凡任务先看这两个目录，通常会有可读的 spec。**最新参考**：`2026-07-08-ui-expressive-redesign-design.md`（UI 重做设计）+ `2026-07-09-followups-fixes-and-deviations-design.md`（后续修复与偏差）。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **alist** (4054 symbols, 8261 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/alist/context` | Codebase overview, check index freshness |
| `gitnexus://repo/alist/clusters` | All functional areas |
| `gitnexus://repo/alist/processes` | All execution flows |
| `gitnexus://repo/alist/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
