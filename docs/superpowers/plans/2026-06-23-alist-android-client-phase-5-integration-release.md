# Phase 5 Integration and Release Verification Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify the MVP against real Alist v3 servers, Android compatibility targets, weak network conditions, kill-process behavior, backup exclusions, large files, notifications, and packaging readiness.

**Architecture:** Phase 5 should not add major features. It fixes integration bugs found by verification and produces release/test documentation.

**Tech Stack:** Gradle, Android Emulator/ADB, real Alist v3 server, Android Studio Profiler, bmgr, monkey, SHA256 tools.

## Global Constraints

- Do not claim release readiness unless every command in this phase has been run or explicitly skipped with user approval and documented reason.
- Test API 26, 29, 31, 33, 34.
- Do not test Android 15+ or MIUI/ColorOS/EMUI/HarmonyOS as MVP requirements.
- Large-file test size = 500MB; do not require 1GB+.
- Weak network does not auto-retry; manual retry must work.

---

## File Structure

Create:

```text
README.md
docs/testing/phase-5-verification-report.md
docs/testing/known-limitations.md
```

Modify:

```text
Any source file needed to fix integration bugs discovered during this phase.
```

---

### Task 5.1: Create README and known limitations documentation

**Files:**
- Create: `README.md`
- Create: `docs/testing/known-limitations.md`

**Interfaces:**
- Produces: user-facing setup and limitation docs.
- Consumes: spec section 12.

- [ ] **Step 1: Write README**

```markdown
# Alist Android Client

Android 原生 Alist v3 文件管理客户端 MVP。

## 功能

- Alist v3 账号密码登录
- 文件浏览、搜索、排序
- 上传、下载、删除、新建文件夹、重命名、复制、移动
- 上传/下载任务列表
- 图片/文本基础预览
- 系统分享和链接分享
- 单账号设置与退出登录

## 构建

```bash
./gradlew :app:assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 测试

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

连接设备后：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 已知限制

见 `docs/testing/known-limitations.md`。
```

- [ ] **Step 2: Write known limitations doc**

```markdown
# 已知限制

本应用为 MVP 第一版，以下功能明确不支持：

## 文件管理

- 后台可靠传输（切后台系统可能挂起）
- 断点续传（中断后需重传）
- 多账号切换
- 管理员面板
- 外部网盘挂载管理
- 离线目录缓存
- 回收站/撤销删除
- 复杂分享规则（有效期、密码、权限）

## 系统兼容

- 横屏/平板布局（仅竖屏手机）
- 自签 HTTPS 证书支持
- Alist v2 兼容
- 从其他 App 接收分享文件

## 性能边界

- 大文件 500MB 已测试；大于 1GB 未测试
- 同时并发：上传 2，下载 3，超出排队
- 缩略图缓存：内存 15-20% 堆，磁盘独立目录

## 安全边界

- HTTP 明文连接允许但会提示风险
- HTTPS 证书错误不允许绕过
- 凭据存储使用 EncryptedSharedPreferences + Keystore；已 root 设备无法保证安全
```

- [ ] **Step 3: Commit**

```bash
git add README.md docs/testing/known-limitations.md
git commit -m "docs: add user readme and known limitations"
```

---

### Task 5.2: Run build, lint, and unit test verification

**Files:**
- Create/Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: verification report with command output summaries.

- [ ] **Step 1: Run core verification**

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Record result**

Append to `docs/testing/phase-5-verification-report.md`:

```markdown
# Phase 5 Verification Report

## Build / Lint / Unit Tests

Command:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Result: PASS or FAIL

Evidence summary:
- assembleDebug:
- lintDebug:
- testDebugUnitTest:
```

Fill PASS/FAIL based on actual output. If FAIL, stop and fix before continuing.

- [ ] **Step 3: Commit report**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record core verification results"
```

---

### Task 5.3: Android compatibility matrix

**Files:**
- Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: compatibility result table for API 26/29/31/33/34.

- [ ] **Step 1: Run install and launch on each target API**

For each emulator/device API 26, 29, 31, 33, 34:

```bash
./gradlew :app:installDebug
adb shell monkey -p com.textvision.alistclient -c android.intent.category.LAUNCHER 1
```

Expected: app launches to login screen.

- [ ] **Step 2: Record matrix**

Append:

```markdown
## Compatibility Matrix

| API | Device/Emulator | Install | Launch | Notes |
|-----|-----------------|---------|--------|-------|
| 26 | | PASS/FAIL | PASS/FAIL | |
| 29 | | PASS/FAIL | PASS/FAIL | |
| 31 | | PASS/FAIL | PASS/FAIL | |
| 33 | | PASS/FAIL | PASS/FAIL | POST_NOTIFICATIONS runtime behavior |
| 34 | | PASS/FAIL | PASS/FAIL | targetSdk behavior |
```

- [ ] **Step 3: Commit**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record android compatibility matrix"
```

---

### Task 5.4: Real Alist login and file workflow verification

**Files:**
- Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: real-server smoke test results.

- [ ] **Step 1: Test login and file list**

Use a real Alist v3 server. Record server URL without credentials.

Checklist:

```text
[ ] Correct username/password logs in
[ ] Wrong password shows 用户名或密码错误
[ ] HTTP URL shows warning before login
[ ] Root directory loads
[ ] Child directory opens
[ ] Breadcrumb returns to root
[ ] Search returns results or falls back to current directory filter
```

- [ ] **Step 2: Test CRUD**

Checklist:

```text
[ ] New folder succeeds and refreshes
[ ] Rename succeeds and refreshes
[ ] Invalid names show specific errors
[ ] Delete requires confirmation
[ ] Delete failure shows server message
```

- [ ] **Step 3: Record results**

Append:

```markdown
## Real Alist Workflow

Server: `<redacted host or local test server>`

### Login/List
- Correct login:
- Wrong password:
- HTTP warning:
- Root list:
- Child navigation:
- Search:

### CRUD
- Mkdir:
- Rename:
- Invalid name validation:
- Delete confirmation:
- Delete failure message:
```

- [ ] **Step 4: Commit**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record real alist workflow results"
```

---

### Task 5.5: Weak network and network switching verification

**Files:**
- Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: weak network test evidence.

- [ ] **Step 1: Apply weak network on emulator**

```bash
adb shell tc qdisc add dev wlan0 root netem delay 500ms loss 5%
```

Run list, search, upload, and download.

Expected: operations complete or fail with clear retryable errors; UI does not freeze.

- [ ] **Step 2: Restore network**

```bash
adb shell tc qdisc del dev wlan0 root
```

- [ ] **Step 3: Test flight mode and switching**

Checklist:

```text
[ ] Flight mode: operation fails with 网络不可用
[ ] Flight mode -> restored: no auto retry; manual retry succeeds
[ ] WiFi -> 4G switch during transfer: task becomes Failed and user can retry
```

- [ ] **Step 4: Record results**

Append:

```markdown
## Weak Network

- 500ms + 5% loss list/search:
- 500ms + 5% loss upload/download:
- Flight mode behavior:
- Manual retry after restore:
- WiFi to mobile switch:
```

- [ ] **Step 5: Commit**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record weak network results"
```

---

### Task 5.6: 500MB large file verification

**Files:**
- Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: SHA256-backed transfer integrity evidence.

- [ ] **Step 1: Create 500MB test file on desktop**

Linux/macOS/Git Bash:

```bash
python - <<'PY'
from pathlib import Path
p = Path('alist-500mb-test.bin')
p.write_bytes(b'\0' * 500 * 1024 * 1024)
PY
sha256sum alist-500mb-test.bin
```

Record SHA256.

- [ ] **Step 2: Upload file through app**

Expected:

```text
[ ] Progress updates continuously
[ ] Speed displays in a plausible range
[ ] Notification progress matches app progress
[ ] Upload reaches Success
```

- [ ] **Step 3: Download file through app**

Pull downloaded private file using `run-as` if debug app allows:

```bash
adb shell run-as com.textvision.alistclient ls files/downloads
adb exec-out run-as com.textvision.alistclient cat files/downloads/<sha1-name>.bin > downloaded-500mb-test.bin
sha256sum downloaded-500mb-test.bin
```

Expected: SHA256 matches original.

- [ ] **Step 4: Record results**

Append:

```markdown
## 500MB Transfer

- Original SHA256:
- Uploaded successfully:
- Downloaded SHA256:
- Hash match:
- Progress behavior:
- Notification behavior:
```

- [ ] **Step 5: Commit**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record large file transfer results"
```

---

### Task 5.7: Kill-process, backup, notification, and monkey verification

**Files:**
- Modify: `docs/testing/phase-5-verification-report.md`

**Interfaces:**
- Produces: resilience verification evidence.

- [ ] **Step 1: Kill-process transfer test**

```text
1. Start downloading a file >= 100MB.
2. Wait until around 50%.
3. Run: adb shell am kill com.textvision.alistclient
4. Reopen app.
5. Verify task state = Interrupted.
6. Verify label = 传输中断.
7. Verify button = 重新传输.
8. Tap retry and verify it restarts at 0%.
```

- [ ] **Step 2: Backup exclusion test**

```bash
adb shell bmgr backupnow com.textvision.alistclient
adb shell bmgr restore com.textvision.alistclient
```

Then uninstall/reinstall and verify:

```text
[ ] Login page has no credential prefill
[ ] filesDir/downloads/ is empty after reinstall
[ ] Room transfer_tasks is empty
[ ] cacheDir/preview/ is empty
```

- [ ] **Step 3: Notification permission test**

On API 33+:

```text
[ ] Deny POST_NOTIFICATIONS
[ ] Start transfer
[ ] App transfer page works
[ ] No notification appears
[ ] Grant POST_NOTIFICATIONS
[ ] Start transfer
[ ] Low-priority progress notification appears
```

- [ ] **Step 4: Monkey test**

```bash
adb shell monkey -p com.textvision.alistclient --throttle 500 -v 1000
```

Expected: no ANR, no uncaught crash.

- [ ] **Step 5: Record results**

Append:

```markdown
## Resilience

### Kill Process
- Interrupted state:
- Retry restarts at 0%:

### Backup Exclusion
- Credentials excluded:
- Downloads excluded:
- Room excluded:
- Preview cache excluded:

### Notifications
- Denied permission behavior:
- Granted permission behavior:

### Monkey
- Command:
- Result:
```

- [ ] **Step 6: Commit**

```bash
git add docs/testing/phase-5-verification-report.md
git commit -m "test: record resilience verification results"
```

---

## Phase 5 Completion Gate

Run final verification:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Confirm these files exist:

```text
README.md
docs/testing/known-limitations.md
docs/testing/phase-5-verification-report.md
app/build/outputs/apk/debug/app-debug.apk
```

Do not state MVP is complete unless the report marks every required check PASS or explicitly documents a user-approved skip.
