# Transfer Tabs and Permanent Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the Transfer page into Upload and Download tabs and add per-record permanent deletion that cancels active transfers before deleting their database row.

**Architecture:** Keep the feature local to the existing transfer stack. `TransferDao` owns database deletion, `TransferManager` owns cancellation plus deletion semantics, and `TransferScreen` owns tab filtering plus confirmation UI. No new database table or migration is needed because the schema does not change.

**Tech Stack:** Kotlin, Android Jetpack Compose Material3, Room DAO, Hilt ViewModel, kotlinx.coroutines, existing Gradle unit tests.

## Global Constraints

- Use Chinese user-facing copy.
- Do not implement bulk deletion.
- Do not implement long-press deletion.
- Do not change existing upload, download, retry, or cancel semantics.
- Do not delete local downloaded files or remote files; only delete transfer records.
- Run GitNexus impact analysis before editing any symbol and `gitnexus_detect_changes()` before committing.

---

## File Structure

- Modify `app/src/main/java/com/textvision/alistclient/transfer/data/TransferDao.kt`
  - Add `deleteById(id: String)` for single-row permanent deletion.
- Modify `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt`
  - Add `delete(id: String)` that cancels active call/job and deletes the row.
- Modify `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`
  - Add tab state, filtering helpers, tab-specific empty text, delete callback, and delete confirmation dialog.
- Modify `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt`
  - Add DAO helper support and manager-level deletion tests.
- Modify `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`
  - Add pure state/filtering tests for Upload/Download tabs and summary behavior.

Impact analysis already run while writing this plan:

- `TransferScreenContent`: LOW risk; directly impacts `TransferScreen`.
- `TransferManager`: MEDIUM risk; imported by MainActivity, FileViewModel, TransferScreen, SettingsViewModel, and tests.
- `TransferDao`: LOW risk; imported by TransferManager, AppDatabase, AppModule, and tests.

---

### Task 1: Add transfer tab state and filtering helpers

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt:50-67`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`

**Interfaces:**
- Consumes: `TransferEntity.type: TransferType`, `TransferType.Upload`, `TransferType.Download`
- Produces:
  - `enum class TransferTab(val type: TransferType, val title: String, val emptyMessage: String)`
  - `TransferListUiState.visibleTransfers: List<TransferEntity>`
  - `TransferListUiState.shouldShowEmptyState: Boolean`
  - `TransferListUiState.emptyMessage: String`
  - `TransferListUiState.summaryText: String`

- [ ] **Step 1: Write failing tests for tab filtering and tab-specific empty text**

Add these tests to `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt` after `emptyTransferListShowsEmptyState`:

```kotlin
@Test
fun transferListStateFiltersRecordsBySelectedTab() {
    val now = 1L
    val transfers = listOf(
        TransferEntity("upload", "upload.bin", "/upload.bin", null, null, 0, 100, TransferType.Upload, TransferStatus.Uploading, null, now, now),
        TransferEntity("download", "download.zip", "/download.zip", null, null, 0, 100, TransferType.Download, TransferStatus.Downloading, null, now, now),
    )

    val uploadState = TransferListUiState(transfers, selectedTab = TransferTab.Upload)
    val downloadState = TransferListUiState(transfers, selectedTab = TransferTab.Download)

    assertEquals(listOf("upload"), uploadState.visibleTransfers.map { it.id })
    assertEquals(listOf("download"), downloadState.visibleTransfers.map { it.id })
}

@Test
fun transferListEmptyMessageMatchesSelectedTab() {
    val uploadState = TransferListUiState(emptyList(), selectedTab = TransferTab.Upload)
    val downloadState = TransferListUiState(emptyList(), selectedTab = TransferTab.Download)

    assertEquals("暂无上传任务", uploadState.emptyMessage)
    assertEquals("暂无下载任务", downloadState.emptyMessage)
    assertTrue(uploadState.shouldShowEmptyState)
    assertTrue(downloadState.shouldShowEmptyState)
}
```

Update the existing `emptyTransferListShowsEmptyState` test to pass the new constructor parameter:

```kotlin
@Test
fun emptyTransferListShowsEmptyState() {
    val state = TransferListUiState(transfers = emptyList(), selectedTab = TransferTab.Upload)

    assertEquals("暂无上传任务", state.emptyMessage)
    assertTrue(state.shouldShowEmptyState)
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenTest
```

Expected: compilation fails because `TransferTab` and the new `TransferListUiState` constructor/properties do not exist yet.

- [ ] **Step 3: Implement minimal tab state and filtering**

In `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`, add this import near the existing transfer model imports:

```kotlin
import com.textvision.alistclient.transfer.model.TransferType
```

Replace the existing `EmptyTransferMessage` constant and `TransferListUiState` definition with:

```kotlin
enum class TransferTab(
    val type: TransferType,
    val title: String,
    val emptyMessage: String,
) {
    Upload(TransferType.Upload, "上传", "暂无上传任务"),
    Download(TransferType.Download, "下载", "暂无下载任务"),
}

data class TransferListUiState(
    val transfers: List<TransferEntity>,
    val selectedTab: TransferTab,
) {
    val visibleTransfers: List<TransferEntity> = transfers.filter { it.type == selectedTab.type }
    val emptyMessage: String = selectedTab.emptyMessage
    val shouldShowEmptyState: Boolean = visibleTransfers.isEmpty()
    val summaryText: String
        get() {
            if (transfers.isEmpty()) return "上传和下载任务"
            val active = transfers.count { it.status in ActiveTransferStatuses }
            val failed = transfers.count { it.showRetry }
            val completed = transfers.count { it.status == TransferStatus.Success }
            return listOfNotNull(
                active.takeIf { it > 0 }?.let { "$it 个进行中" },
                failed.takeIf { it > 0 }?.let { "$it 个失败" },
                completed.takeIf { it > 0 }?.let { "$it 个完成" },
            ).joinToString(" · ").ifBlank { "暂无进行中的任务" }
        }
}
```

- [ ] **Step 4: Update existing tests to use selectedTab**

In `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`, update the existing summary test state creation:

```kotlin
val state = TransferListUiState(transfers, selectedTab = TransferTab.Upload)
```

Do not change the expected summary text; it must remain:

```kotlin
assertEquals("2 个进行中 · 1 个失败 · 1 个完成", state.summaryText)
```

This verifies the summary still counts all transfers, not just the selected tab.

- [ ] **Step 5: Run tests and verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit Task 1**

Only commit if the user has explicitly authorized commits in this session. If committing is authorized, run:

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt
git commit -m "feat(transfer): add transfer tab filtering state"
```

---

### Task 2: Add permanent single-record deletion to the transfer data layer

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/data/TransferDao.kt:30-35`
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt:167-176`
- Test: `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt`

**Interfaces:**
- Consumes: `TransferDao.find(id: String): TransferEntity?`, `TransferDao.deleteAll()`
- Produces:
  - `TransferDao.deleteById(id: String): Unit`
  - `TransferManager.delete(id: String): Unit`

- [ ] **Step 1: Write failing tests for permanent deletion**

In `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt`, add these imports if they are not already present:

```kotlin
import java.util.concurrent.atomic.AtomicBoolean
```

Add these tests after `markInterruptedOnStartupMarksActiveTasks`:

```kotlin
@Test
fun deleteRemovesCompletedTransferRecord() = runBlocking {
    val dao = MemoryTransferDao()
    val now = System.currentTimeMillis()
    val manager = TransferManager(RuntimeEnvironment.getApplication(), dao, CapturingOkHttpClient(), savedSessionManager("http://example.com/"))
    dao.upsert(TransferEntity("done", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now))

    manager.delete("done")
    dao.awaitMissing("done")

    assertEquals(null, dao.find("done"))
}

@Test
fun deleteCancelsActiveTransferAndRemovesRecord() = runBlocking {
    val dao = MemoryTransferDao()
    val client = BlockingOkHttpClient()
    val manager = TransferManager(RuntimeEnvironment.getApplication(), dao, client, savedSessionManager("http://example.com/"))

    val id = manager.enqueueDownload("/folder/file.txt", "file.txt")
    client.awaitRequest()

    manager.delete(id)
    dao.awaitMissing(id)

    assertEquals(true, client.cancelled.get())
    assertEquals(null, dao.find(id))
}
```

- [ ] **Step 2: Add test helpers that will fail until production interfaces exist**

In the `MemoryTransferDao` test class inside `TransferManagerTest.kt`, add this method:

```kotlin
override suspend fun deleteById(id: String) { entities.remove(id) }

fun awaitMissing(id: String) {
    repeat(100) {
        if (!entities.containsKey(id)) return
        Thread.sleep(10)
    }
    throw AssertionError("Expected transfer $id to be deleted")
}
```

Add this fake client class after `CapturingOkHttpClient`:

```kotlin
private class BlockingOkHttpClient : OkHttpClient() {
    @Volatile private var requestSeen = false
    val cancelled = AtomicBoolean(false)

    fun awaitRequest() {
        repeat(100) {
            if (requestSeen) return
            Thread.sleep(10)
        }
        throw AssertionError("Expected request")
    }

    override fun newCall(request: Request): Call {
        requestSeen = true
        return object : Call {
            override fun request(): Request = request
            override fun execute(): Response {
                while (!cancelled.get()) {
                    Thread.sleep(10)
                }
                throw IOException("Canceled")
            }
            override fun enqueue(responseCallback: okhttp3.Callback) = throw UnsupportedOperationException()
            override fun cancel() { cancelled.set(true) }
            override fun isExecuted(): Boolean = false
            override fun isCanceled(): Boolean = cancelled.get()
            override fun timeout(): okio.Timeout = okio.Timeout.NONE
            override fun clone(): Call = this
        }
    }
}
```

Also add this import if missing:

```kotlin
import java.io.IOException
```

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.transfer.TransferManagerTest
```

Expected: compilation fails because `TransferManager.delete(id)` and `TransferDao.deleteById(id)` do not exist yet.

- [ ] **Step 4: Add DAO single-row delete**

In `app/src/main/java/com/textvision/alistclient/transfer/data/TransferDao.kt`, add this method before `deleteAll()`:

```kotlin
@Query("DELETE FROM transfer_tasks WHERE id = :id")
suspend fun deleteById(id: String)
```

The end of the interface should look like:

```kotlin
@Query("UPDATE transfer_tasks SET status = 'Interrupted', failureReason = '传输中断', updatedAtMillis = :updatedAtMillis WHERE status IN ('Waiting', 'Uploading', 'Downloading')")
suspend fun markActiveTasksInterrupted(updatedAtMillis: Long)

@Query("DELETE FROM transfer_tasks WHERE id = :id")
suspend fun deleteById(id: String)

@Query("DELETE FROM transfer_tasks")
suspend fun deleteAll()
```

- [ ] **Step 5: Add TransferManager.delete**

In `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt`, add this public method after `cancel(id: String)`:

```kotlin
fun delete(id: String) {
    cancelNotified[id] = true
    activeCalls.remove(id)?.cancel()
    activeJobs.remove(id)?.cancel()
    scope.launch {
        dao.deleteById(id)
        cancelNotified.remove(id)
    }
}
```

Do not call `cancel(id)` here because `cancel(id)` writes a `Cancelled` status row, while `delete(id)` must permanently remove the row.

- [ ] **Step 6: Run focused tests and verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.transfer.TransferManagerTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit Task 2**

Only commit if the user has explicitly authorized commits in this session. If committing is authorized, run:

```bash
git add app/src/main/java/com/textvision/alistclient/transfer/data/TransferDao.kt app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt
git commit -m "feat(transfer): permanently delete transfer records"
```

---

### Task 3: Wire Upload/Download tabs into the Transfer UI

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt:118-148`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`

**Interfaces:**
- Consumes:
  - `TransferTab.entries`
  - `TransferListUiState.visibleTransfers`
  - `TransferListUiState.emptyMessage`
- Produces:
  - `TransferScreenContent(transfers, onCancel, onRetry, onDelete)` UI that displays tabs and only current-tab records.

- [ ] **Step 1: Write a source-level failing test for tab UI contract**

Create or update `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt` with this test class if it does not already exist:

```kotlin
package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferScreenSourceTest {
    private val source = File("src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt").readText()

    @Test
    fun transferScreenUsesTabsAndVisibleTransfers() {
        assertTrue(source.contains("ScrollableTabRow") || source.contains("TabRow"))
        assertTrue(source.contains("TransferTab.entries"))
        assertTrue(source.contains("state.visibleTransfers"))
        assertTrue(source.contains("selectedTab"))
    }
}
```

If `TransferScreenSourceTest` already exists, add only the `transferScreenUsesTabsAndVisibleTransfers` test and reuse its existing `source` property.

- [ ] **Step 2: Run source test and verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenSourceTest.transferScreenUsesTabsAndVisibleTransfers
```

Expected: FAIL because the screen does not yet use `TabRow`/`TransferTab.entries`/`state.visibleTransfers`.

- [ ] **Step 3: Add Compose tab imports and selected tab state**

In `TransferScreen.kt`, add these imports:

```kotlin
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
```

`getValue` is already imported. If `setValue` is already present after previous work, do not duplicate it.

Inside `TransferScreenContent`, replace:

```kotlin
val state = TransferListUiState(transfers)
```

with:

```kotlin
var selectedTab by remember { mutableStateOf(TransferTab.Upload) }
val state = TransferListUiState(transfers, selectedTab)
```

- [ ] **Step 4: Add TabRow under the top bar**

In `TransferScreenContent`, immediately after:

```kotlin
CloudTopBar(title = "传输", subtitle = state.summaryText)
```

add:

```kotlin
TabRow(selectedTabIndex = TransferTab.entries.indexOf(selectedTab)) {
    TransferTab.entries.forEach { tab ->
        Tab(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            text = { Text(tab.title) },
        )
    }
}
Spacer(Modifier.height(10.dp))
```

- [ ] **Step 5: Render only selected-tab records and tab-specific empty copy**

In `TransferScreenContent`, replace the empty/list block with:

```kotlin
if (state.shouldShowEmptyState) {
    CloudCard {
        CloudEmptyState(title = state.emptyMessage, message = "对应类型的传输任务会显示在这里")
    }
} else {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.visibleTransfers, key = { it.id }) { task ->
            TransferRow(
                task,
                onCancel = { onCancel(task.id) },
                onRetry = { onRetry(task.id) },
                onDelete = { onDelete(task.id) },
            )
        }
    }
}
```

This step refers to `onDelete` and `TransferRow(..., onDelete)` that will not compile until Task 4. To keep Task 3 independently green, temporarily pass a no-op callback and update the signatures in this task as follows:

- Add `onDelete: (String) -> Unit = {}` to `TransferScreenContent` parameters.
- Add `onDelete: () -> Unit = {}` to `TransferRow` parameters.
- Do not show the delete button yet.

The final `items` block for this task should compile with:

```kotlin
TransferRow(
    task,
    onCancel = { onCancel(task.id) },
    onRetry = { onRetry(task.id) },
    onDelete = { onDelete(task.id) },
)
```

- [ ] **Step 6: Run focused UI state/source tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenTest --tests com.textvision.alistclient.ui.screens.TransferScreenSourceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit Task 3**

Only commit if the user has explicitly authorized commits in this session. If committing is authorized, run:

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt
git commit -m "feat(transfer): split transfer page into upload and download tabs"
```

---

### Task 4: Add per-record delete action and confirmation dialog

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt:109-217`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt`

**Interfaces:**
- Consumes:
  - `TransferManager.delete(id: String)` from Task 2
  - `TransferScreenContent(..., onDelete: (String) -> Unit)` from Task 3
- Produces:
  - `TransferViewModel.delete(id: String)`
  - `TransferRow(task, onCancel, onRetry, onDelete)` with visible delete action
  - `DeleteTransferDialog(task, onConfirm, onDismiss)` confirmation UI

- [ ] **Step 1: Write a failing source-level test for delete UI contract**

In `TransferScreenSourceTest.kt`, add:

```kotlin
@Test
fun transferScreenShowsDeleteActionWithConfirmation() {
    assertTrue(source.contains("fun delete(id: String) = manager.delete(id)"))
    assertTrue(source.contains("AlertDialog"))
    assertTrue(source.contains("删除后会取消当前传输，并永久删除这条记录。"))
    assertTrue(source.contains("将永久删除这条传输记录。"))
    assertTrue(source.contains("TextButton(onClick = onDelete"))
}
```

- [ ] **Step 2: Run source test and verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenSourceTest.transferScreenShowsDeleteActionWithConfirmation
```

Expected: FAIL because delete UI and ViewModel delete method do not exist yet.

- [ ] **Step 3: Add dialog imports**

In `TransferScreen.kt`, add:

```kotlin
import androidx.compose.material3.AlertDialog
```

If not already present from Task 3, ensure these imports exist:

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
```

- [ ] **Step 4: Add delete to TransferViewModel and top-level TransferScreen**

In `TransferViewModel`, add:

```kotlin
fun delete(id: String) = manager.delete(id)
```

In `TransferScreen`, update `TransferScreenContent` call to:

```kotlin
TransferScreenContent(
    transfers = transfers,
    onCancel = viewModel::cancel,
    onRetry = viewModel::retry,
    onDelete = viewModel::delete,
)
```

- [ ] **Step 5: Add delete state and button to TransferRow**

Replace `TransferRow` signature with:

```kotlin
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit, onDelete: () -> Unit) {
```

At the top of `TransferRow`, before `Column(...)`, add:

```kotlin
var showDeleteDialog by remember { mutableStateOf(false) }
if (showDeleteDialog) {
    DeleteTransferDialog(
        task = task,
        onConfirm = {
            showDeleteDialog = false
            onDelete()
        },
        onDismiss = { showDeleteDialog = false },
    )
}
```

In the row action area, replace:

```kotlin
TransferAction(task, onCancel, onRetry)
```

with:

```kotlin
Column(horizontalAlignment = Alignment.End) {
    TransferAction(task, onCancel, onRetry)
    TextButton(onClick = onDelete) {
        Text("删除", color = CloudErrorText)
    }
}
```

Then immediately correct the delete button to open the dialog instead of deleting directly:

```kotlin
Column(horizontalAlignment = Alignment.End) {
    TransferAction(task, onCancel, onRetry)
    TextButton(onClick = { showDeleteDialog = true }) {
        Text("删除", color = CloudErrorText)
    }
}
```

The source-level test in Step 1 expects the text `TextButton(onClick = onDelete`. Update that test assertion to the safer visible contract:

```kotlin
assertTrue(source.contains("Text(\"删除\", color = CloudErrorText)"))
```

This keeps the UI contract accurate: clicking visible Delete opens a confirmation dialog, not immediate deletion.

- [ ] **Step 6: Add DeleteTransferDialog composable**

Add this function below `TransferRow` and above `TransferAction`:

```kotlin
@Composable
private fun DeleteTransferDialog(task: TransferEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val isActive = task.status in ActiveTransferStatuses
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除传输记录") },
        text = {
            Text(
                if (isActive) {
                    "删除后会取消当前传输，并永久删除这条记录。"
                } else {
                    "将永久删除这条传输记录。"
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = CloudErrorText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
```

- [ ] **Step 7: Run focused tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.TransferScreenSourceTest --tests com.textvision.alistclient.ui.screens.TransferScreenTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit Task 4**

Only commit if the user has explicitly authorized commits in this session. If committing is authorized, run:

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt
git commit -m "feat(transfer): add permanent delete confirmation"
```

---

### Task 5: Final verification and app smoke test

**Files:**
- Verify: all files touched by Tasks 1-4

**Interfaces:**
- Consumes all prior task outputs.
- Produces verified working feature and GitNexus change report.

- [ ] **Step 1: Run all transfer-related unit tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.transfer.TransferManagerTest --tests com.textvision.alistclient.ui.screens.TransferScreenTest --tests com.textvision.alistclient.ui.screens.TransferScreenSourceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build debug APK**

Run:

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` and APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Install and launch on emulator**

Run:

```bash
/d/programming/devtools/android/sdk/platform-tools/adb.exe devices
/d/programming/devtools/android/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/d/programming/devtools/android/sdk/platform-tools/adb.exe shell am start -n com.textvision.alistclient/.MainActivity
```

Expected:

- `adb devices` shows `emulator-5554 device` or another online emulator.
- Install prints `Success`.
- Activity start prints `Status: ok` or opens the app without a fatal error.

- [ ] **Step 4: Manual UI smoke**

On the emulator:

1. Open `传输` tab from the bottom navigation.
2. Verify top tabs show `上传` and `下载`.
3. Tap `上传`; only upload records appear, or empty state says `暂无上传任务`.
4. Tap `下载`; only download records appear, or empty state says `暂无下载任务`.
5. Tap `删除` on an ended record; verify confirmation says `将永久删除这条传输记录。`.
6. Cancel the dialog and verify the record remains.
7. Tap `删除` again, confirm, and verify the record disappears.
8. If an active transfer is available, tap `删除`; verify confirmation says `删除后会取消当前传输，并永久删除这条记录。` and the record disappears after confirmation.

- [ ] **Step 5: Run GitNexus change detection**

Run GitNexus MCP:

```text
gitnexus_detect_changes({ repo: "alist", scope: "all" })
```

Expected: changed symbols include `TransferScreen`, `TransferManager`, `TransferDao`, and their tests. Review unexpected symbols before reporting completion.

- [ ] **Step 6: Report verification evidence**

In the final report, include:

- Unit test command and result.
- Build command and result.
- Emulator install/launch result, if run.
- GitNexus `detect_changes` risk summary.
- Note whether commits were skipped because the user did not explicitly authorize commits.

---

## Self-Review

- Spec coverage: Tabs, per-tab filtering, tab empty messages, per-record delete, active-transfer cancel-before-delete, no local/remote file deletion, and tests are covered by Tasks 1-5.
- Placeholder scan: No TBD/TODO placeholders remain. Commit steps are conditional because the user has not authorized commits in this session.
- Type consistency: `TransferTab`, `TransferListUiState.visibleTransfers`, `TransferDao.deleteById`, `TransferManager.delete`, and `TransferViewModel.delete` names are consistent across tasks.
