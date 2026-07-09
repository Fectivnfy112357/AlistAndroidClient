# AGENTS.md

Compact guidance for OpenCode sessions in this repo. For depth, read `CLAUDE.md` first.

## 会话约定
- 用中文与用户对话。
- 探索代码库优先使用 gitnexus（impact / context / query 等 MCP 工具）。
- Alist 测试地址：`http://textvision.top:5244`
- 测试账密：`fectivnfy` / `Yishengaini12345`

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
**黑屏诊断**：应用在前台且无 logcat FATAL 但窗口全黑 → 模拟器宿主 GPU 渲染失败，非应用 bug，用 `-gpu swiftshader_indirect` 重启。
**截图路径**：Git Bash 下 `/sdcard` 会被转换成本地路径，必须加 `MSYS_NO_PATHCONV=1` 前缀：
```bash
MSYS_NO_PATHCONV=1 adb shell screencap -p //sdcard/scr.png
MSYS_NO_PATHCONV=1 adb pull //sdcard/scr.png ./scr.png
```

## What this is
Single-module (**`:app`**) native Android client (Kotlin + Jetpack Compose + Material 3) for Alist v3. Package `com.textvision.alistclient`. No multi-module split; everything lives under `app/`.

## Commands (from repo root, via `gradlew.bat` on Windows)
- Build debug APK → `app/build/outputs/apk/debug/app-debug.apk`:
  `./gradlew :app:assembleDebug`
- Full validation (build + lint + unit tests):
  `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`
- On-device instrumented tests (needs connected device/emulator):
  `./gradlew :app:connectedDebugAndroidTest`
- Single unit test class: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest`
- Single test method: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileViewModelTest.someMethod"`
- Lint only: `./gradlew :app:lintDebug`. There is **no ktlint/detekt** — Android Lint is the only static check.

## Hard-won gotchas
- `dependencyResolutionManagement` uses `FAIL_ON_PROJECT_REPOS` — never add `repositories {}` blocks to module build files. Add all dependencies centrally in `gradle/libs.versions.toml`.
- All dependency versions are centralized in `gradle/libs.versions.toml`. JVM target is 17.
- Emulator will show a **black window** if launched with default GPU; use `emulator -avd test_avd -no-snapshot -gpu swiftshader_indirect`. A black screen with no FATAL in logcat is a GPU issue, not an app bug.
- `adb` screenshots from Git Bash need `MSYS_NO_PATHCONV=1` (e.g. `MSYS_NO_PATHCONV=1 adb shell screencap -p //sdcard/scr.png`).
- API base URL defaults to `http://127.0.0.1:5244/`, configurable at login (see `di/AppModule.kt:NetworkModule`).
- Known MVP limitations (no background reliable transfers, no resume, portrait-only, no multi-account, no self-signed HTTPS): see `docs/testing/known-limitations.md`.

## Architecture entry points (app/src/main/java/com/textvision/alistclient/)
- `AlistClientApp.kt` — `@HiltAndroidApp`; installs `SafeCrashHandler`.
- `MainActivity.kt` — only `@AndroidEntryPoint` activity; hosts `AppNavHost`, inits `TransferManager`.
- `di/AppModule.kt` — all Hilt bindings + `@IoDispatcher`.
- `navigation/AppNavHost.kt` + `AppRoute.kt` — sealed `AppRoute` graph; bottom bar only on Files/Transfers/Settings.
- `common/result/ApiResult.kt` — sealed `Success/Failure/NetworkError` wrapper used by every API call.
- `transfer/TransferManager.kt` — upload/download coordinator, bound in `MainActivity`.

## Testing conventions
- Unit tests mirror packages under `app/src/test/java/com/textvision/alistclient/`, named `<Subject>Test.kt`.
- Stack: JUnit 4 + MockK + Turbine + MockWebServer + Coroutines Test (`StandardTestDispatcher`, explicit `advanceUntilIdle()`). Flows via `flow.test { … }`.

## Before changing code
This repo is indexed by GitNexus. Run impact analysis (e.g. `gitnexus_impact`) before editing a symbol, and `gitnexus_detect_changes()` before committing. See `CLAUDE.md` for the full GitNexus workflow.

## Specs workflow
Non-trivial features start from specs/plans in `docs/superpowers/specs/` + `docs/superpowers/plans/` — check those first.
