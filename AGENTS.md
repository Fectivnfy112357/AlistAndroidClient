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
