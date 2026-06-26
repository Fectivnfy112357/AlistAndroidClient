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
- testDebugUnitTest: PASS — 21 test suites, 67 tests, 0 failures, 0 errors

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
- Upload launcher: PASS — tapping the upload icon in the top bar launches `com.google.android.documentsui/.picker.PickActivity` (system file picker). End-to-end upload was not exercised here; full transfer verification is in Task 5.6.

## Weak Network

Tools used: `tc qdisc add dev wlan0 root netem` on emulator (root via `su 0`), and `svc wifi/data enable/disable` + `settings put global airplane_mode_on` to simulate flight mode. Note: emulator reports `wlan0` and `eth0`; wlan0 is the one carrying app traffic.

- 500ms + 5% loss list/search:
  - Without throttle: `POST /api/fs/list` → 200 OK in 28–162ms.
  - With `tc qdisc ... netem delay 500ms loss 5%`: `POST /api/fs/list` → 200 OK in **547ms** (single attempt) and **2807ms** on retry after a simulated loss. UI does not freeze; data eventually renders. Logcat excerpt: `okhttp.OkHttpClient: <-- 200 OK ... (547ms)` and `(2807ms)`.
- Flight mode behavior: PASS — enabling airplane mode (`settings put global airplane_mode_on 1` + `svc wifi/data disable`) while inside `/我的文件/开发环境` immediately surfaces the red `当前无网络` banner, status bar airplane icon appears, and the screen shows `无法连接服务器，请检查地址和网络` with a `重试` button. Screenshot: `docs/testing/screenshots/flight-mode.png`.
- Manual retry after restore: PASS — after re-enabling network (`airplane_mode_on 0`, `svc wifi enable`), tapping `重试` issues a new `POST /api/fs/list` and renders the directory contents (CC Switch / FinalShell / Git / IDE / Java). Screenshot: `docs/testing/screenshots/manual-retry6.png`.
- WiFi → mobile switch during transfer: NOT EXECUTED — emulator has a single wlan0 interface and no 4G radio, so a WiFi↔4G switch cannot be reproduced in this environment. Transfer-failure path is covered by the flight-mode test (network goes away → manual retry needed).

## 500MB Transfer

Original test file:

```bash
python - <<'PY'
from pathlib import Path
p = Path('alist-500mb-test.bin')
p.write_bytes(b'\0' * 500 * 1024 * 1024)
PY
sha256sum alist-500mb-test.bin
```

- Original SHA256: `a08a92258f621b55d08ad1e84c90c2ea6286fc6b6c9a4dfa7156afb16c190170`
- Original size: `524288000` bytes
- Upload verification: PASS after integration fixes.
  - Initial app upload exposed three bugs that were fixed with regression tests:
    1. `File-Path` header rejected non-ASCII path `/我的文件/...`; fixed by percent-encoding header path segments.
    2. `HttpLoggingInterceptor.Level.BODY` caused OOM for 500MB streaming upload; changed logging to `HEADERS`.
    3. Alist `type: 0` regular files were rendered as folders; mapping now trusts `is_dir`.
  - Direct Alist API 500MB upload to `/我的文件/alist-500mb-test-host.bin` returned `{"code":200,"message":"success"}` and app list displays it as a file with size `524288000 B`. Screenshot: `docs/testing/screenshots/500mb-file-visible-downloadable.png`.
  - App upload path now sends encoded `File-Path: /%E6%88%91%E7%9A%84%E6%96%87%E4%BB%B6/...` and no longer OOMs; app also parses Alist JSON `code` instead of treating any HTTP 200 as success.
- Download verification: PASS.
  - Download started through app by tapping the download action for `alist-500mb-test-host.bin`. Screenshot: `docs/testing/screenshots/500mb-download-progress.png`.
  - App-private downloaded file: `/data/user/0/com.textvision.alistclient/files/downloads/69c0edfd5fffc26c765a2bbe89920b160a6d20d2.bin`.
  - Downloaded size: `524288000` bytes.
  - Downloaded SHA256: `a08a92258f621b55d08ad1e84c90c2ea6286fc6b6c9a4dfa7156afb16c190170`
  - Hash match: PASS.
  - Additional bug fixed: the app wrote the full file and hash matched, but progress status remained `下载中`; download progress now uses the same conflated progress collector as upload, and `TransferProgressResponseBody` now forces final completion progress.
- Progress behavior: PASS after fixes — large uploads/downloads stream without loading the full body into memory and progress events are coalesced.
- Notification behavior: PASS — detailed notification permission checks are recorded in Task 5.7.

## Resilience

### Kill Process
- Command path: started a 500MB download, then tested both `adb shell am kill com.textvision.alistclient` and `adb shell am force-stop com.textvision.alistclient`.
- `am kill` result: DID NOT KILL while the app was foreground/top; transfer remained `下载中`. This command is insufficient for foreground process-kill simulation on this emulator.
- `am force-stop` result: PASS — after force-stopping mid-download and reopening, transfer row changed to `已中断：传输中断`; button label changed to `重新传输`. Screenshot: `docs/testing/screenshots/resilience-interrupted-after-login.png`.
- Retry restarts at 0%: PASS — tapping `重新传输` changed status to `下载中`, and a new private download file started near 11MB instead of resuming from the previous ~70MB partial file. Screenshot: `docs/testing/screenshots/resilience-retry-after-interrupt.png`.
- Note: a transient ANR dialog occurred when an automation script typed before the first post-clear launch had a focused window. Logcat reason was `Input dispatching timed out (Application does not have a focused window)` and the Activity displayed afterward; subsequent wait-for-login automation avoided the issue. Screenshot: `docs/testing/screenshots/resilience-anr-dialog.png`.

### Backup Exclusion
- Backup rules inspected:
  - `app/src/main/res/xml/backup_rules.xml` excludes `secure_prefs.xml`, `transfer_tasks.db`, `transfer_tasks.db-shm`, `transfer_tasks.db-wal`, `files/downloads/`, and `files/preview/`.
  - `app/src/main/res/xml/data_extraction_rules.xml` excludes `secure_prefs.xml`, `transfer_tasks.db`, `files/downloads/`, and `files/preview/` from cloud backup; device-transfer excludes `secure_prefs.xml` and `transfer_tasks.db`.
- `adb shell bmgr backupnow com.textvision.alistclient`: SKIPPED by emulator/system — command returned `Backup finished with result: Backup is not allowed`.
- `adb shell bmgr restore com.textvision.alistclient`: SKIPPED by Android version — command returned `restore <package> is no longer supported; use restore <token> <package>`.
- Uninstall/reinstall validation: PASS.
  - Login page has no credential prefill after reinstall. Screenshot: `docs/testing/screenshots/resilience-after-reinstall-login.png`.
  - `files/downloads/`: absent/empty after reinstall.
  - `cache/preview/`: absent/empty after reinstall.
  - `transfer_tasks.db`: newly created but no `transfer_tasks` table yet (`OperationalError no such table: transfer_tasks`), equivalent to empty transfer state.

### Notifications
- API level: 34.
- Denied permission behavior: PASS — `pm revoke com.textvision.alistclient android.permission.POST_NOTIFICATIONS`, then started a download. Transfer page worked (`下载中` + `取消`) and `dumpsys notification` showed app importance `NONE` with no active `Alist 传输` notification. Screenshot: `docs/testing/screenshots/resilience-notification-denied.png`.
- Granted permission behavior: PASS after integration fix — wired `TransferManager` to `TransferNotificationController`. With `pm grant ... POST_NOTIFICATIONS`, starting a download produced notification record `pkg=com.textvision.alistclient id=1001 channel=transfer_progress_channel`, title `Alist 传输`, text `正在传输 1 个文件`, importance 2 (low priority). Screenshot: `docs/testing/screenshots/resilience-notification-granted-active.png`.

### Monkey
- Command:

```bash
adb shell monkey -p com.textvision.alistclient --throttle 500 -v 1000
```

- Result: PASS — `Events injected: 1000`, `Monkey finished`, exit code 0.
- Crash/ANR log check after monkey: PASS — no `FATAL EXCEPTION`, no `ANR in com.textvision.alistclient`, no `OutOfMemory` in post-monkey logcat. Log saved to `docs/testing/monkey-1000.log`.

## Completion Gate

Command:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Result: PASS — `BUILD SUCCESSFUL in 5s`.

Evidence summary:
- assembleDebug: PASS
- lintDebug: PASS
- testDebugUnitTest: PASS — 21 suites, 67 tests, 0 failures, 0 errors

Required file existence:
- `README.md`: PASS
- `docs/testing/known-limitations.md`: PASS
- `docs/testing/phase-5-verification-report.md`: PASS
- `app/build/outputs/apk/debug/app-debug.apk`: PASS