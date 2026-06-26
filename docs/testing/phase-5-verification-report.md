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

## Real Alist Workflow

Server: `http://textvision.top:5244/` (public test instance). Credentials redacted; the account was used to validate flows, not stored.

### Login / List
- Correct login: PASS — `POST /api/auth/login` returns 200 + token, app navigates to file list. (Logcat: `okhttp.OkHttpClient: <-- 200 OK`)
- Wrong password: PASS — form error shows `用户名或密码错误`. Screenshot: `docs/testing/screenshots/wrong-password.png`.
- HTTP warning: PASS — red `⚠ 当前使用 HTTP 明文连接，账号密码可能被窃听` text appears once the URL starts with `http://`. Screenshot: `docs/testing/screenshots/after-url2.png`.
- Root list: PASS — 5 folders returned (我的夸克 / 我的文件 / 我的照片 / 我的百度网盘 / 我的音乐). Screenshot: `docs/testing/screenshots/file-list-root.png`.
- Child navigation: PASS — tapping `打开` on `我的文件` issues `POST /api/fs/list?path=/我的文件` (200 OK) and renders breadcrumb `/ 我的文件` + 3 sub-folders + 1 file. Screenshot: `docs/testing/screenshots/child-dir.png`.
- Search: PASS — typing `test` falls back to current-directory filter, shows `当前目录搜索结果 / 这里没有文件` (no matches in root). Screenshot: `docs/testing/screenshots/search.png`.
- Breadcrumb back to root: PASS — tapping the `/` in the breadcrumb re-loads the root. Screenshot: `docs/testing/screenshots/breadcrumb-root2.png`.
- Logout: PASS — tapping `退出登录` in Settings returns to login screen with all fields empty. Screenshot: `docs/testing/screenshots/after-logout.png`.

### CRUD
- Mkdir: N/A — UI for new folder is not implemented in MVP (only open / download / share actions on rows; consistent with `known-limitations.md`).
- Rename: N/A — same as above.
- Invalid name validation: N/A — no entry point in UI.
- Delete confirmation: N/A — no entry point in UI.
- Delete failure message: N/A — no entry point in UI.

### Upload
- Upload launcher: PASS — tapping the upload icon in the top bar launches `com.google.android.documentsui/.picker.PickActivity` (system file picker). End-to-end upload was not exercised here; full transfer verification is in Task 5.6. |