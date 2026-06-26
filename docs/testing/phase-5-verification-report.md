# Phase 5 Verification Report

## Build / Lint / Unit Tests

Command:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Result: PASS

Evidence summary:
- assembleDebug: PASS — `app/build/outputs/apk/debug/app-debug.apk` (~19 MB)
- lintDebug: PASS — `0 errors, 54 warnings` (per `app/build/reports/lint-results-debug.txt`; warnings are library-style / non-blocking)
- testDebugUnitTest: PASS — 20 test suites, 61 tests, 0 failures, 0 errors

## Compatibility Matrix

Device/emulator in use: `emulator-5554` (Android `test_avd`).

| API | Device/Emulator | Install | Launch | Notes |
|-----|-----------------|---------|--------|-------|
| 26 | n/a | SKIPPED | SKIPPED | API 26 system image not installed in `$ANDROID_HOME/platforms`/`system-images`; no offline installer available in this environment. |
| 29 | n/a | SKIPPED | SKIPPED | API 29 system image not installed. |
| 31 | n/a | SKIPPED | SKIPPED | API 31 system image not installed. |
| 33 | n/a | SKIPPED | SKIPPED | API 33 system image not installed. |
| 34 | emulator-5554 (test_avd) | PASS | PASS | App launches to login screen with three input fields (服务器地址 / 用户名 / 密码) and 登录 button. Screenshot: `docs/testing/screenshots/api34-launch.png`. |