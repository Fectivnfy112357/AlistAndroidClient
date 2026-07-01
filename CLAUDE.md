# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 用中文与用户对话。

## 本地开发环境（Windows 11）

- **操作系统**: Windows 11 Pro
- **Shell**: Git Bash（POSIX 语法，路径用正斜杠，`/dev/null` 不是 `NUL`）
- **JDK**: 已检测到 `java -version` → Java 21 LTS（系统 PATH），`JAVA_HOME` 未设置。AGP 8.7.2 + Kotlin 2.0.21 在 JVM 17 目标下可直接用 system JDK 编译。
- **Android SDK**: `D:\programming\devtools\android\sdk`（`local.properties` 已指向）
  - Platforms: `android-34`（项目 compileSdk / targetSdk = 34）
  - Build-tools: `34.0.0`
  - cmdline-tools: `latest/`
  - platform-tools、emulator、system-images 已安装
- **Git 用户**: 贾晓源
- **备注**: 命令优先用 `./gradlew`（Git Bash 上可直接跑），Windows 原生命令行用 `gradlew.bat`。

## Project Overview

Native Android client (MVP) for [Alist v3](https://github.com/AlistGo/alist) — a multi-storage file management server. Single-module Kotlin app targeting Android 8.0+ (minSdk 26, targetSdk 34), built with Jetpack Compose and Material3.

- **Package / Application ID**: `com.textvision.alistclient`
- **Alist API base URL**: defaults to `http://127.0.0.1:5244/` (configurable via login). See `app/src/main/java/com/textvision/alistclient/di/AppModule.kt:NetworkModule`.
- **MVP scope & known limitations**: see `docs/testing/known-limitations.md` (no background reliable transfers, no resume, portrait-only, no multi-account, no self-signed HTTPS).

## Build & Test Commands

All commands run from the project root using the Gradle wrapper (`./gradlew` or `gradlew.bat` on Windows).

```bash
# Build debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Full validation: build + lint + unit tests
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest

# On-device instrumented tests (requires connected device)
./gradlew :app:connectedDebugAndroidTest

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest

# Run a single test method within a class
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileViewModelTest.someMethod"
```

Stack: Kotlin 2.0.21 + AGP 8.7.2 + Compose BOM 2024.09.03 + Hilt 2.52 + Retrofit 2.11 + OkHttp 4.12 + Room 2.6.1 + Coil 3.0.4 + Navigation Compose 2.8.3. JVM target 17. All versions centralized in `gradle/libs.versions.toml`.

No ktlint or detekt configured — only standard Android Lint (`./gradlew :app:lintDebug`).

## Architecture

Single-module MVVM Android app. Entry point is `MainActivity` which hosts a single `AppNavHost` Composable. State flows: View → ViewModel (Hilt-injected) → Repository → API/Room. Async work uses Coroutines + Flow throughout; HTTP via Retrofit/OkHttp; persistence via Room + EncryptedSharedPreferences for credentials.

### Key entry points
- `AlistClientApp.kt` — `@HiltAndroidApp` Application. Installs `SafeCrashHandler` as default uncaught exception handler.
- `MainActivity.kt` — Single `@AndroidEntryPoint` activity. Enables edge-to-edge, ensures notification channels, initializes `TransferManager`, sets Compose content with `AppNavHost`.
- `di/AppModule.kt` — All Hilt bindings live here (CredentialModule, DatabaseModule, NetworkModule) plus `@IoDispatcher` qualifier.
- `navigation/AppNavHost.kt` + `AppRoute.kt` — Compose Navigation graph (Login, Files, Transfers, Settings, MoveCopyPicker, Preview). Routes use a sealed `AppRoute` class with encoded payload helpers for preview. Bottom bar appears only on Files/Transfers/Settings.
- `common/result/ApiResult.kt` — Sealed `Success/Failure/NetworkError` wrapper used by every API call.
- `transfer/TransferManager.kt` — Coordinator for upload/download tasks (initialized once in `MainActivity` for lifecycle binding).

### Package layout (`app/src/main/java/com/textvision/alistclient/`)
- `auth/` — Login screen + AuthRepository + SessionManager + LoginViewModel.
- `common/` — Cross-cutting utilities: crash handler (`common/crash/`), error mapping (`common/error/`), `NetworkMonitor` (`common/network/`), `ApiResult` (`common/result/`).
- `data/local/` — Room `AppDatabase` + DAOs (DB name: `transfer_tasks.db`).
- `data/secure/` — `CredentialStore` interface + `EncryptedCredentialStore` impl (EncryptedSharedPreferences).
- `file/` — `FileRepository`, `FileViewModel`, `CopyMoveUseCase`, `FileNameValidator`, file models.
- `network/` — Retrofit `AlistApi`, `AuthInterceptor` + `AuthTokenProvider` + `SkipAuthRetry` annotation, DTOs.
- `preview/` — `PreviewRouter`, `MimeTypeResolver`, `PreviewFileStore`, `PreviewTextRepository`.
- `transfer/` — `TransferManager`, `TransferNotificationController`, progress request/response bodies, `TransferDao` + `TransferEntity`.
- `ui/components/` — Reusable Composables (CloudScaffold, CloudTopBar, CloudBottomBar, CloudListItem, BreadcrumbBar, DirectoryBrowser, FileTypeIcon, TransferProgress).
- `ui/screens/` — Top-level screen Composables + their ViewModels (LoginScreen, FileScreen, TransferScreen, SettingsScreen, PreviewScreen, MoveCopyTargetPickerScreen).
- `ui/theme/` — Material3 theme with HyperOS-style motion tokens.
- `util/` — `ServerUrlNormalizer`, helpers.

## Testing

**Frameworks**: JUnit 4, MockK, Turbine, Robolectric, MockWebServer, Coroutines Test, Room Testing, Compose UI Test, AndroidX Test, Espresso.

**Conventions**:
- Unit tests in `app/src/test/java/com/textvision/alistclient/<mirror-package-path>/`.
- Name test classes `<Subject>Test.kt`. Use MockK (`@MockK`, `mockk()`, `every { } / coEvery { } / verify { }`).
- Flow testing uses Turbine (`flow.test { … }`).
- HTTP layer uses MockWebServer. Coroutine tests run on `StandardTestDispatcher` with explicit `advanceUntilIdle()`.
- The tests cover ~30 classes across `auth/`, `common/`, `data/`, `di/`, `file/`, `navigation/`, `network/`, `preview/`, `transfer/`, `ui/screens/`, `util/`.

## GitNexus — Code Intelligence

This project is indexed by GitNexus as **alist** (1859 symbols, 4235 relationships, 136 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

### Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

### Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

### Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/alist/context` | Codebase overview, check index freshness |
| `gitnexus://repo/alist/clusters` | All functional areas |
| `gitnexus://repo/alist/processes` | All execution flows |
| `gitnexus://repo/alist/process/{name}` | Step-by-step execution trace |

### CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

## Development Workflow

The project follows a spec → plan → task → report workflow documented in `docs/superpowers/` and `.superpowers/sdd/`. Each feature is captured as a design spec (`docs/superpowers/specs/`) with a paired implementation plan (`docs/superpowers/plans/`). When picking up a non-trivial task, check those directories first — there's usually a spec to read.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **alist** (1993 symbols, 3957 relationships, 167 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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
