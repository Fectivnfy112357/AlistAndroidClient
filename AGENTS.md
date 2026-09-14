# AGENTS.md

Compact guidance for OpenCode sessions in this repo. For depth, read `CLAUDE.md` first.

## 会话约定
- 用中文与用户对话。
- Alist 测试地址：`http://textvision.top:5244`
- 测试账密：`fectivnfy` / `Yishengaini12345`

## 开发环境（Linux）
主机已装:
- OpenJDK 17 (apt, `/usr/lib/jvm/java-17-openjdk-amd64`) — **不**进 shell 全局 PATH,避免污染其他项目
- Android SDK (`$HOME/Android/Sdk`,含 `cmdline-tools/latest` / `platform-tools` / `emulator` / `platforms;android-34` / `build-tools;34.0.0` / `system-images;android-34;google_apis;x86_64`) — 已在 `~/.bashrc` 设了 `ANDROID_HOME` 和 PATH
- mihomo TUN 模式开启,curl 直通外网;apt 不走 TUN(`dl.google.com` DNS 解析不到),用 `http_proxy=http://127.0.0.1:7897 https_proxy=...` 走 mihomo mixed-port

**启动项目前**:
```bash
source tools/dev-env.sh
# 验证: java -version 应是 17, adb --version 可用
```

**为什么需要 dev-env.sh**: 系统 PATH 里 `~/.jdks/corretto-1.8.0_504/bin` 在 `/usr/bin/java` 之前,不显式把 `JAVA_HOME=17/bin` 提前到 PATH 最前,`java` 会被 1.8 抢到,导致 `gradle/AGP/sdkmanager` 报 class file 61.0 (JDK 17) 不支持。

```

## What this is
Single-module (**`:app`**) native Android client (Kotlin + Jetpack Compose + Material 3) for Alist v3. Package `com.textvision.alistclient`. No multi-module split; everything lives under `app/`.

## Commands (from repo root, via `./gradlew` on Linux/macOS; `gradlew.bat` on Windows)
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
- All dependency versions are centralized in `gradle/libs.versions.toml`. JVM target is 17. **Don't write `JAVA_HOME` to `~/.bashrc`** — other projects may need Java 8/25; use `source tools/dev-env.sh` per-session instead.
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
