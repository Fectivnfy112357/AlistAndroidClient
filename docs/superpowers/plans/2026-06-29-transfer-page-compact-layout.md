# Transfer Page Compact Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the Transfer page into a compact, readable list layout where each transfer task is one coherent row with progress, status, and the correct action aligned cleanly.

**Architecture:** Keep transfer execution and persistence unchanged. Improve only `TransferScreen` presentation and derived UI helpers from `TransferEntity` / `TransferListUiState`, with tests proving summary/action/progress text behavior before UI changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt ViewModel, Room-backed `TransferEntity`, JUnit unit tests.

## Global Constraints

- Do not change transfer execution, upload/download networking, Room schema, or navigation behavior.
- Selected visual direction: compact list layout, not large card grouping.
- Keep existing theme tokens: `CloudPrimary`, `CloudErrorText`, `CloudTextSecondary`, `CloudShapes`, `CloudSurface` where appropriate.
- Use TDD: every production behavior change starts with a failing test.
- Project instruction: run GitNexus impact analysis before editing symbols and `gitnexus_detect_changes()` before committing.
- Do not commit unless the user explicitly asks.

---

## File Structure

- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`
  - Owns transfer list UI state helpers and Compose rendering.
  - Will gain compact summary/action helpers and a compact row layout.
- Modify: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`
  - Unit tests for derived UI text: summary, action labels, completed-state behavior, progress text.
- Optional create: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt`
  - Lightweight source-level guard that the compact row no longer uses `CloudListItem` for task rows. Use only if Compose UI behavior is hard to assert with existing test tools.

---

### Task 1: Add compact transfer summary and row-action helpers

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenTest.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`

**Interfaces:**
- Consumes: `TransferEntity.status`, `TransferStatus.Waiting`, `TransferStatus.Uploading`, `TransferStatus.Downloading`, `TransferStatus.Failed`, `TransferStatus.Interrupted`, `TransferStatus.Success`.
- Produces:
  - `TransferListUiState.summaryText: String`
  - `TransferEntity.primaryActionLabel: String?`
  - `TransferEntity.isComplete: Boolean`

- [ ] **Step 1: Write failing summary/action tests**

Add these tests to `TransferScreenTest.kt`:

```kotlin
@Test
fun transferListSummaryCountsActiveFailedAndCompletedTasks() {
    val now = 1L
    val transfers = listOf(
        TransferEntity("1", "upload.bin", "/upload.bin", null, null, 0, 100, TransferType.Upload, TransferStatus.Uploading, null, now, now),
        TransferEntity("2", "download.zip", "/download.zip", null, null, 0, 100, TransferType.Download, TransferStatus.Downloading, null, now, now),
        TransferEntity("3", "bad.pdf", "/bad.pdf", null, null, 0, 100, TransferType.Download, TransferStatus.Failed, "网络错误", now, now),
        TransferEntity("4", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now),
    )

    val state = TransferListUiState(transfers)

    assertEquals("2 个进行中 · 1 个失败 · 1 个完成", state.summaryText)
}

@Test
fun activeFailedAndCompletedTransfersExposeCompactActionState() {
    val now = 1L
    val uploading = TransferEntity("1", "upload.bin", "/upload.bin", null, null, 0, 100, TransferType.Upload, TransferStatus.Uploading, null, now, now)
    val failed = TransferEntity("2", "bad.pdf", "/bad.pdf", null, null, 0, 100, TransferType.Download, TransferStatus.Failed, "网络错误", now, now)
    val completed = TransferEntity("3", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now)

    assertEquals("取消", uploading.primaryActionLabel)
    assertEquals("重试", failed.primaryActionLabel)
    assertEquals(null, completed.primaryActionLabel)
    assertEquals(true, completed.isComplete)
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.TransferScreenTest.transferListSummaryCountsActiveFailedAndCompletedTasks" --tests "com.textvision.alistclient.ui.screens.TransferScreenTest.activeFailedAndCompletedTransfersExposeCompactActionState"
```

Expected: compile failure or assertion failure because `summaryText`, `primaryActionLabel`, and `isComplete` are not implemented.

- [ ] **Step 3: Implement minimal helpers**

In `TransferScreen.kt`, replace/extend the helper block near `TransferListUiState` and `TransferEntity` extensions with:

```kotlin
private const val EmptyTransferMessage = "暂无传输任务"

data class TransferListUiState(val transfers: List<TransferEntity>) {
    val emptyMessage: String = EmptyTransferMessage
    val shouldShowEmptyState: Boolean = transfers.isEmpty()
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

private val ActiveTransferStatuses = setOf(
    TransferStatus.Waiting,
    TransferStatus.Uploading,
    TransferStatus.Downloading,
)

val TransferEntity.statusText: String
    get() = status.displayName + (failureReason?.let { "：$it" } ?: "")

val TransferEntity.showRetry: Boolean
    get() = status.canRetry

val TransferEntity.retryButtonLabel: String
    get() = status.retryLabel ?: "重试"

val TransferEntity.primaryActionLabel: String?
    get() = when {
        status in ActiveTransferStatuses -> "取消"
        showRetry -> retryButtonLabel
        else -> null
    }

val TransferEntity.isComplete: Boolean
    get() = status == TransferStatus.Success
```

Keep the existing `progressText` and `formatBytes()` helpers below this block.

- [ ] **Step 4: Run tests and verify GREEN**

Run the same command from Step 2.

Expected: PASS.

---

### Task 2: Render compact task rows

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt` or create it if absent.
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`

**Interfaces:**
- Consumes: `TransferListUiState.summaryText`, `TransferEntity.statusText`, `TransferEntity.progressText`, `TransferEntity.primaryActionLabel`, `TransferEntity.isComplete` from Task 1.
- Produces: Compact `TransferRow(task, onCancel, onRetry)` layout with a single row/card per task.

- [ ] **Step 1: Write failing source layout guard**

Create `app/src/test/java/com/textvision/alistclient/ui/screens/TransferScreenSourceTest.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferScreenSourceTest {
    @Test
    fun transferRowsUseCompactCardLayoutInsteadOfCloudListItemPlusDetachedProgress() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt").readText()

        assertTrue(source.contains("private fun TransferRow"))
        assertTrue(source.contains("task.primaryActionLabel"))
        assertTrue(source.contains("task.progressText"))
        assertFalse(source.contains("CloudListItem("))
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.TransferScreenSourceTest.transferRowsUseCompactCardLayoutInsteadOfCloudListItemPlusDetachedProgress"
```

Expected: FAIL because current `TransferRow` still uses `CloudListItem(`.

- [ ] **Step 3: Update imports for compact layout**

In `TransferScreen.kt`, remove:

```kotlin
import com.textvision.alistclient.ui.components.CloudListItem
```

Add imports if missing:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
```

- [ ] **Step 4: Replace `TransferScreenContent` top bar subtitle**

Change:

```kotlin
CloudTopBar(title = "传输", subtitle = "上传与下载任务")
```

To:

```kotlin
CloudTopBar(title = "传输", subtitle = state.summaryText)
```

- [ ] **Step 5: Replace `TransferRow` implementation**

Replace the current `TransferRow` function with:

```kotlin
@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .clip(CloudShapes.Control)
            .background(CloudSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    color = CloudTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = task.statusText,
                    color = if (task.showRetry) CloudErrorText else CloudTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TransferAction(task, onCancel, onRetry)
        }

        Spacer(Modifier.height(8.dp))
        TransferProgress(task.bytesDone, task.totalBytes)
        Text(
            text = task.progressText,
            color = CloudPrimary,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
```

Add this helper below `TransferRow`:

```kotlin
@Composable
private fun TransferAction(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    val label = task.primaryActionLabel
    when {
        label == "取消" -> TextButton(
            onClick = onCancel,
            modifier = Modifier.widthIn(min = 72.dp),
        ) { Text(label, color = CloudErrorText) }
        label != null -> TextButton(
            onClick = onRetry,
            modifier = Modifier.widthIn(min = 72.dp),
        ) { Text(label, color = CloudPrimary) }
        task.isComplete -> Box(
            modifier = Modifier
                .clip(CloudShapes.Pill)
                .background(CloudPrimarySoft)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("完成", color = CloudPrimary)
        }
    }
}
```

- [ ] **Step 6: Run layout source test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.TransferScreenSourceTest.transferRowsUseCompactCardLayoutInsteadOfCloudListItemPlusDetachedProgress"
```

Expected: PASS.

---

### Task 3: Verify affected tests and install to emulator

**Files:**
- Test only; no expected production changes unless tests reveal an issue.

**Interfaces:**
- Consumes: all helpers/layout from Tasks 1-2.
- Produces: installed APK with compact Transfer page.

- [ ] **Step 1: Run affected unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.TransferScreenTest" --tests "com.textvision.alistclient.ui.screens.TransferScreenSourceTest" --tests "com.textvision.alistclient.transfer.TransferProgressRequestBodyTest" --tests "com.textvision.alistclient.transfer.TransferProgressResponseBodyTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Install debug build**

Run:

```powershell
.\gradlew.bat :app:installDebug
```

Expected: `Installed on 1 device.` and `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual visual check**

On emulator:

1. Open the app.
2. Go to `传输` tab.
3. Confirm top subtitle shows summary text such as `2 个进行中 · 1 个失败`.
4. Confirm each task is one compact unit.
5. Confirm progress text is visible below the progress bar.
6. Confirm active row shows `取消`, failed row shows `重试`, completed row shows `完成`.

Expected: the Transfer page matches layout direction B and no longer has detached progress/action blocks.

---

## Self-Review

**Spec coverage:**
- Compact list layout: Task 2.
- Summary subtitle: Task 1 + Task 2.
- Action alignment and labels: Task 1 + Task 2.
- Progress text remains visible: Task 2 and existing `progressText` tests.
- No transfer execution changes: Global Constraints and task scope.

**Placeholder scan:** No TBD/TODO/fill-in placeholders are present.

**Type consistency:** `summaryText`, `primaryActionLabel`, `isComplete`, and `progressText` are consistently defined and consumed.
