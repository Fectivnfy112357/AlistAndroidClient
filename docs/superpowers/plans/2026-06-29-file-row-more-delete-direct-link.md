# File Row More Menu Delete and Direct Link Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a clean file-row actions menu that keeps Download visible, moves Share into More, adds conditional Copy Direct Link, and supports confirmed file/folder deletion.

**Architecture:** Reuse the existing AList remove API through `FileRepository.delete(paths)`. Extend `FileRepositoryContract` so `FileViewModel` can delete a single `FileItem`, refresh on success, and avoid fake-removing on failure. Keep UI state local to `FileRow` for menu/dialog visibility and keep the row layout aligned with existing `CloudListItem` styling.

**Tech Stack:** Kotlin, Android Jetpack Compose Material3, Android ClipboardManager, Toast, existing FileViewModel unit tests and source contract tests.

## Global Constraints

- Use Chinese user-facing copy.
- Do not implement bulk deletion.
- Do not implement long-press menu.
- Do not implement recycle bin.
- Do not synthesize direct links when `downloadUrl` is missing; show Copy Direct Link only when `item.downloadUrl` is not null or blank.
- Run GitNexus impact analysis before editing symbols and `gitnexus_detect_changes()` before committing.

---

## File Structure

- Modify `app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt`
  - Add `delete(paths)` to `FileRepositoryContract`.
  - Add `FileViewModel.delete(item: FileItem)` for single item deletion and refresh.
- Modify `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`
  - Replace the file-row visible Share button with a More menu.
  - Keep Download visible for files.
  - Add menu items for Share, Copy Direct Link, and Delete.
  - Add delete confirmation dialog.
- Modify `app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt`
  - Add fake repository delete support and success/failure tests.
- Modify `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt`
  - Add source contract tests for More menu, conditional direct link, and delete confirmation.

Impact analysis already run while planning:

- `FileScreen`: LOW risk; no upstream callers beyond navigation composition.
- `FileViewModel`: MEDIUM risk; used by `FileScreen`, `MoveCopyTargetPickerScreen`, and several tests.

---

### Task 1: Add file deletion to FileViewModel

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt:29-32, 127-131`
- Test: `app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt`

**Interfaces:**
- Consumes: `FileRepository.delete(paths: List<String>): ApiResult<Unit>` already implemented on concrete repository.
- Produces:
  - `FileRepositoryContract.delete(paths: List<String>): ApiResult<Unit>`
  - `FileViewModel.delete(item: FileItem): Unit`

- [ ] **Step 1: Write failing tests for delete success and failure**

In `app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt`, update `FakeRepo` to track delete calls but do not update the interface yet:

```kotlin
private inner class FakeRepo : FileRepositoryContract {
    var listCalls = 0
    var deletedPaths = emptyList<String>()
    var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var listResult: ApiResult<List<FileItem>> = ApiResult.Success(
        listOf(item("b.txt"), item("docs", true), item("a.txt"))
    )
    override suspend fun list(path: String): ApiResult<List<FileItem>> {
        listCalls++
        return listResult
    }
    override suspend fun search(path: String, keyword: String) = ApiResult.Success(listOf(item("match.txt")))
    override suspend fun delete(paths: List<String>): ApiResult<Unit> {
        deletedPaths = paths
        return deleteResult
    }
}
```

Update `PausedRepo` with a delete stub:

```kotlin
override suspend fun delete(paths: List<String>): ApiResult<Unit> = ApiResult.Success(Unit)
```

Add these tests before the closing brace of `FileViewModelTest`:

```kotlin
@Test fun deleteSuccessCallsRepositoryAndRefreshesCurrentDirectory() = runTest {
    val repo = FakeRepo()
    val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
    vm.load("/")
    testScheduler.advanceUntilIdle()
    repo.listResult = ApiResult.Success(listOf(item("remaining.txt")))

    vm.delete(item("a.txt"))
    testScheduler.advanceUntilIdle()

    assertEquals(listOf("/a.txt"), repo.deletedPaths)
    assertEquals(2, repo.listCalls)
    assertEquals(listOf("remaining.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
}

@Test fun deleteFailureKeepsCurrentListVisible() = runTest {
    val repo = FakeRepo().apply { deleteResult = ApiResult.Failure(500, "remove failed") }
    val vm = FileViewModel(repo, newManager(), StandardTestDispatcher(testScheduler))
    vm.load("/")
    testScheduler.advanceUntilIdle()

    vm.delete(item("a.txt"))
    testScheduler.advanceUntilIdle()

    assertEquals(listOf("/a.txt"), repo.deletedPaths)
    assertEquals(1, repo.listCalls)
    assertEquals(listOf("docs", "a.txt", "b.txt"), (vm.uiState.value as FileUiState.Success).items.map { it.name })
}
```

- [ ] **Step 2: Run focused test and verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest
```

Expected: compilation fails because `FileRepositoryContract.delete` and `FileViewModel.delete` do not exist yet.

- [ ] **Step 3: Extend FileRepositoryContract**

In `app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt`, change the interface to:

```kotlin
interface FileRepositoryContract {
    suspend fun list(path: String): ApiResult<List<FileItem>>
    suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>>
    suspend fun delete(paths: List<String>): ApiResult<Unit>
}
```

`FileRepository` already has a compatible `suspend fun delete(paths: List<String>): ApiResult<Unit>`, so no production method body is needed there.

- [ ] **Step 4: Add FileViewModel.delete**

In `FileViewModel`, add this method after `enqueueUpload(uri: Uri)`:

```kotlin
fun delete(item: FileItem) {
    viewModelScope.launch(dispatcher) {
        when (repository.delete(listOf(item.path))) {
            is ApiResult.Success -> load(currentPath)
            is ApiResult.Failure -> Unit
            is ApiResult.NetworkError -> Unit
        }
    }
}
```

This intentionally preserves the current list on failure and refreshes on success.

- [ ] **Step 5: Run focused tests and verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Add FileScreen More menu, Copy Direct Link, and delete confirmation

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt:17-183`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt`

**Interfaces:**
- Consumes:
  - `FileViewModel.delete(item: FileItem)` from Task 1
  - `FileItem.downloadUrl: String?`
  - existing `PreviewRouter.shareLinkIntent(item.path)`
- Produces:
  - `FileRow(item, onOpenDir, onPreview, onDownload, onShare, onCopyDirectLink, onDelete)`
  - visible Download button for files
  - More menu with Share, Copy Direct Link when available, Delete
  - Delete confirmation dialog

- [ ] **Step 1: Write failing source contract test**

Open `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt` and add this test:

```kotlin
@Test
fun fileRowsUseDownloadPlusMoreMenuForShareDirectLinkAndDelete() {
    val source = java.io.File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()

    assertTrue(source.contains("DropdownMenu"))
    assertTrue(source.contains("DropdownMenuItem"))
    assertTrue(source.contains("Icons.Default.MoreVert"))
    assertTrue(source.contains("Text(\"分享链接\")"))
    assertTrue(source.contains("Text(\"复制直链\")"))
    assertTrue(source.contains("!item.downloadUrl.isNullOrBlank()"))
    assertTrue(source.contains("Text(\"删除\")"))
    assertTrue(source.contains("AlertDialog"))
    assertTrue(source.contains("确定删除「"))
    assertFalse(source.contains("IconButton(onClick = onShare, modifier = Modifier.testTag(\"share_button\"))"))
}
```

- [ ] **Step 2: Run source test and verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.FileScreenSourceTest.fileRowsUseDownloadPlusMoreMenuForShareDirectLinkAndDelete
```

Expected: FAIL because the More menu and delete dialog are not implemented yet.

- [ ] **Step 3: Add imports to FileScreen**

In `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`, add imports:

```kotlin
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.textvision.alistclient.ui.theme.CloudErrorText
```

Remove `import androidx.compose.material.icons.filled.Share` if it becomes unused.

- [ ] **Step 4: Wire callbacks from FileScreen to FileRow**

In the `FileRow(...)` call inside `items(s.items...)`, replace the existing callback block with:

```kotlin
FileRow(
    item = item,
    onOpenDir = { viewModel.load(item.path) },
    onPreview = { onPreview(item) },
    onDownload = { viewModel.enqueueDownload(item) },
    onShare = {
        val intent = PreviewRouter.shareLinkIntent(item.path)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    },
    onCopyDirectLink = item.downloadUrl?.takeIf { it.isNotBlank() }?.let { url ->
        {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("直链", url))
            Toast.makeText(context, "直链已复制", Toast.LENGTH_SHORT).show()
        }
    },
    onDelete = { viewModel.delete(item) },
)
```

- [ ] **Step 5: Update FileRow signature and state**

Replace the `FileRow` signature with:

```kotlin
private fun FileRow(
    item: FileItem,
    onOpenDir: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCopyDirectLink: (() -> Unit)?,
    onDelete: () -> Unit,
) {
```

At the start of `FileRow`, before `CloudListItem`, add:

```kotlin
var menuExpanded by remember { mutableStateOf(false) }
var showDeleteDialog by remember { mutableStateOf(false) }
if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text(if (item.isDir) "删除文件夹" else "删除文件") },
        text = { Text("确定删除「${item.name}」吗？此操作不可恢复。") },
        confirmButton = {
            TextButton(onClick = {
                showDeleteDialog = false
                onDelete()
            }) { Text("删除", color = CloudErrorText) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
        },
    )
}
```

- [ ] **Step 6: Replace trailing UI with download plus More menu**

Replace the current `trailing = { ... }` block in `CloudListItem` with:

```kotlin
trailing = {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!item.isDir) {
            IconButton(onClick = onDownload, modifier = Modifier.testTag("download_button")) {
                Icon(Icons.Default.Download, contentDescription = "下载", tint = CloudPrimary)
            }
        }
        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("more_button")) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = CloudPrimary)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (!item.isDir) {
                DropdownMenuItem(
                    text = { Text("分享链接") },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    },
                )
                if (!item.downloadUrl.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text("复制直链") },
                        onClick = {
                            menuExpanded = false
                            onCopyDirectLink?.invoke()
                        },
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("删除", color = CloudErrorText) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CloudErrorText) },
                onClick = {
                    menuExpanded = false
                    showDeleteDialog = true
                },
            )
        }
    }
}
```

This removes the directory chevron. Row body click still opens directories because `CloudListItem.onClick` remains `if (item.isDir) onOpenDir else onPreview`.

- [ ] **Step 7: Run focused source test and compile tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.ui.screens.FileScreenSourceTest --tests com.textvision.alistclient.file.FileViewModelTest
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 3: Final verification and emulator install

**Files:**
- Verify files touched in Tasks 1-2.

**Interfaces:**
- Consumes all prior task outputs.
- Produces verified APK installed on emulator.

- [ ] **Step 1: Run focused tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.textvision.alistclient.file.FileViewModelTest --tests com.textvision.alistclient.ui.screens.FileScreenSourceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build debug APK**

Run:

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Install and launch on emulator**

Run:

```bash
/d/programming/devtools/android/sdk/platform-tools/adb.exe devices
/d/programming/devtools/android/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/d/programming/devtools/android/sdk/platform-tools/adb.exe shell am start -W -n com.textvision.alistclient/.MainActivity
```

Expected:

- `adb devices` shows an online emulator.
- Install prints `Success`.
- Activity start prints `Status: ok`.

- [ ] **Step 4: Run GitNexus detect_changes**

Run GitNexus MCP:

```text
gitnexus_detect_changes({ repo: "alist", scope: "all" })
```

Expected: changed symbols include `FileScreen`, `FileViewModel`, `FileRepositoryContract`, and related tests. Review unexpected changed symbols before reporting.

---

## Self-Review

- Spec coverage: File row Download + More, Share in menu, Copy Direct Link only for nonblank `downloadUrl`, folder More/Delete, confirmation dialog, delete API wiring, refresh-on-success, no fake delete on failure, no bulk/long-press/recycle-bin all covered.
- Placeholder scan: No TBD/TODO placeholders remain. Commit steps omitted because the user did not ask for commits.
- Type consistency: `FileRepositoryContract.delete`, `FileViewModel.delete`, `onCopyDirectLink`, and `onDelete` names are consistent across tasks.
