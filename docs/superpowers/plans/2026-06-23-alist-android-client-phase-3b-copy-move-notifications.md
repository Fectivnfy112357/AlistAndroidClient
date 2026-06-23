# Phase 3b Copy Move and Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add service-side copy/move operations, target directory picker, serial multi-file execution, interrupted transfer restore, and transfer notifications.

**Architecture:** Copy/move remain in `FileRepository`; UI uses a reusable `DirectoryBrowser` composable for both file browsing and target picking. Notifications are owned by `transfer/TransferNotificationController`, not by screens.

**Tech Stack:** Kotlin, Compose, Hilt, Retrofit, Room, Android notifications.

## Global Constraints

- Copy/move must use Alist service-side API and must never download/re-upload through the phone.
- Multi-file copy/move is serial and stops on the first failure.
- Already completed copy/move items remain in place after a later item fails.
- Notification channels: progress = `IMPORTANCE_LOW`; result = `IMPORTANCE_DEFAULT`.
- Multiple active transfers prefer one summary notification.
- App startup marks `Waiting`, `Uploading`, and `Downloading` tasks as `Interrupted`.

---

## File Structure

Create:

```text
app/src/main/java/com/textvision/alistclient/file/model/CopyMoveResult.kt
app/src/main/java/com/textvision/alistclient/file/CopyMoveUseCase.kt
app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt
app/src/main/java/com/textvision/alistclient/ui/screens/MoveCopyTargetPickerScreen.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferNotificationController.kt
app/src/test/java/com/textvision/alistclient/file/CopyMoveUseCaseTest.kt
```

Modify:

```text
app/src/main/java/com/textvision/alistclient/network/dto/FileDtos.kt
app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
app/src/main/java/com/textvision/alistclient/file/FileRepository.kt
app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt
app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt
```

---

### Task 3b.1: Add copy/move API models and repository methods

**Files:**
- Modify: `network/dto/FileDtos.kt`
- Modify: `network/api/AlistApi.kt`
- Modify: `file/FileRepository.kt`

**Interfaces:**
- Produces: `FileRepository.copy(srcPath: String, dstDir: String): ApiResult<Unit>`.
- Produces: `FileRepository.move(srcPath: String, dstDir: String): ApiResult<Unit>`.
- Consumes: Alist API endpoint `/api/fs/copy` and `/api/fs/move`.

- [ ] **Step 1: Add request DTOs**

Append to `FileDtos.kt`:

```kotlin
@Serializable
data class CopyMovePathRequest(
    @SerialName("src_path") val srcPath: String,
    @SerialName("dst_path") val dstPath: String,
)
```

- [ ] **Step 2: Add API methods**

Append to `AlistApi.kt`:

```kotlin
@POST("api/fs/copy")
suspend fun copy(@Body request: CopyMovePathRequest): AlistResponse<Unit>

@POST("api/fs/move")
suspend fun move(@Body request: CopyMovePathRequest): AlistResponse<Unit>
```

- [ ] **Step 3: Add repository methods**

Append to `FileRepository`:

```kotlin
suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit> =
    runUnit { api.copy(CopyMovePathRequest(srcPath = srcPath, dstPath = dstDir)) }

suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit> =
    runUnit { api.move(CopyMovePathRequest(srcPath = srcPath, dstPath = dstDir)) }
```

Also import `CopyMovePathRequest`.

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/network app/src/main/java/com/textvision/alistclient/file/FileRepository.kt
git commit -m "feat: add service side copy move api"
```

---

### Task 3b.2: Implement serial copy/move use case

**Files:**
- Create: `file/model/CopyMoveResult.kt`
- Create: `file/CopyMoveUseCase.kt`
- Test: `file/CopyMoveUseCaseTest.kt`

**Interfaces:**
- Produces: `CopyMoveUseCase.copyMultiple(srcPaths, targetDir, onProgress)`.
- Produces: `CopyMoveUseCase.moveMultiple(srcPaths, targetDir, onProgress)`.
- Consumes: `FileOperationRepositoryContract`.

- [ ] **Step 1: Write failing tests**

```kotlin
package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyMoveUseCaseTest {
    private class FakeRepo : FileOperationRepositoryContract {
        val copied = mutableListOf<String>()
        var failOn: String? = null
        override suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit> {
            if (srcPath == failOn) return ApiResult.Failure(409, "文件已存在")
            copied += srcPath
            return ApiResult.Success(Unit)
        }
        override suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit> = copy(srcPath, dstDir)
    }

    @Test fun stopsOnFirstCopyFailureAndKeepsSuccessCount() = runTest {
        val repo = FakeRepo().apply { failOn = "/c" }
        val result = CopyMoveUseCase(repo).copyMultiple(listOf("/a", "/b", "/c", "/d"), "/target") { _, _ -> }
        assertEquals(2, result.success)
        assertEquals(2, result.failed)
        assertTrue(result.stopped)
        assertEquals(listOf("/a", "/b"), repo.copied)
        assertEquals("/c", result.firstFailure?.first)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.CopyMoveUseCaseTest"
```

Expected: FAIL because use case does not exist.

- [ ] **Step 3: Implement use case**

`CopyMoveResult.kt`:

```kotlin
package com.textvision.alistclient.file.model

data class CopyMoveResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val firstFailure: Pair<String, String>? = null,
    val stopped: Boolean,
)
```

`CopyMoveUseCase.kt`:

```kotlin
package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.CopyMoveResult
import javax.inject.Inject

interface FileOperationRepositoryContract {
    suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit>
    suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit>
}

class CopyMoveUseCase @Inject constructor(
    private val repository: FileOperationRepositoryContract,
) {
    suspend fun copyMultiple(srcPaths: List<String>, targetDir: String, onProgress: (Int, Int) -> Unit): CopyMoveResult =
        runSerial(srcPaths, targetDir, onProgress, repository::copy)

    suspend fun moveMultiple(srcPaths: List<String>, targetDir: String, onProgress: (Int, Int) -> Unit): CopyMoveResult =
        runSerial(srcPaths, targetDir, onProgress, repository::move)

    private suspend fun runSerial(
        srcPaths: List<String>,
        targetDir: String,
        onProgress: (Int, Int) -> Unit,
        operation: suspend (String, String) -> ApiResult<Unit>,
    ): CopyMoveResult {
        var success = 0
        for (src in srcPaths) {
            when (val result = operation(src, targetDir)) {
                is ApiResult.Success -> {
                    success++
                    onProgress(success, srcPaths.size)
                }
                is ApiResult.Failure -> return CopyMoveResult(srcPaths.size, success, srcPaths.size - success, src to result.message, stopped = true)
                is ApiResult.NetworkError -> return CopyMoveResult(srcPaths.size, success, srcPaths.size - success, src to (result.cause.message ?: "操作失败"), stopped = true)
            }
        }
        return CopyMoveResult(srcPaths.size, success, 0, stopped = false)
    }
}
```

Modify `FileRepository` class declaration to implement `FileOperationRepositoryContract`.

- [ ] **Step 4: Run test and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.CopyMoveUseCaseTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/file app/src/test/java/com/textvision/alistclient/file/CopyMoveUseCaseTest.kt
git commit -m "feat: add serial copy move use case"
```

---

### Task 3b.3: Add DirectoryBrowser and target picker route

**Files:**
- Create: `ui/components/DirectoryBrowser.kt`
- Create: `ui/screens/MoveCopyTargetPickerScreen.kt`
- Modify: `navigation/AppRoute.kt`
- Modify: `navigation/AppNavHost.kt`

**Interfaces:**
- Produces: `DirectoryBrowser(path, directories, onOpen, onSelectCurrent)`.
- Produces: route `copy_move_picker`.
- Consumes: `FileViewModel` list behavior.

- [ ] **Step 1: Add DirectoryBrowser**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.textvision.alistclient.file.model.FileItem

@Composable
fun DirectoryBrowser(
    path: String,
    directories: List<FileItem>,
    onOpen: (String) -> Unit,
    onSelectCurrent: (String) -> Unit,
) {
    Column {
        BreadcrumbBar(path, onOpen)
        Button(onClick = { onSelectCurrent(path) }) { Text("选择当前目录") }
        LazyColumn {
            items(directories, key = { it.path }) { dir ->
                ListItem(
                    headlineContent = { Text(dir.name) },
                    leadingContent = { FileTypeIcon(dir.type) },
                    modifier = Modifier.clickable { onOpen(dir.path) }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add picker screen**

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.DirectoryBrowser

@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }
    val success = state as? FileUiState.Success
    DirectoryBrowser(
        path = success?.path ?: "/",
        directories = success?.items.orEmpty().filter { it.isDir },
        onOpen = viewModel::load,
        onSelectCurrent = onTargetSelected,
    )
}
```

- [ ] **Step 3: Add route**

In `AppRoute.kt` add:

```kotlin
data object MoveCopyPicker : AppRoute("copy_move_picker")
```

In `AppNavHost.kt` add composable:

```kotlin
composable(AppRoute.MoveCopyPicker.route) {
    MoveCopyTargetPickerScreen(onTargetSelected = { navController.popBackStack() })
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui app/src/main/java/com/textvision/alistclient/navigation
git commit -m "feat: add copy move target picker"
```

---

### Task 3b.4: Mark interrupted tasks on startup and add notification controller

**Files:**
- Create: `transfer/TransferNotificationController.kt`
- Modify: `transfer/TransferManager.kt`
- Modify: `MainActivity.kt`

**Interfaces:**
- Produces: notification channels `transfer_progress_channel`, `transfer_result_channel`.
- Produces: startup call `markInterruptedOnStartup()`.
- Consumes: `TransferManager`, Android notification APIs.

- [ ] **Step 1: Add notification controller**

```kotlin
package com.textvision.alistclient.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.textvision.alistclient.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferNotificationController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(PROGRESS_CHANNEL, "传输进度", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(RESULT_CHANNEL, "传输结果", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun showProgressSummary(activeCount: Int, percent: Int?) {
        val text = if (percent == null) "正在传输 $activeCount 个文件" else "正在传输 $activeCount 个文件（总进度 $percent%）"
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Alist 传输")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(PROGRESS_ID, notification)
    }

    fun clearProgress() {
        NotificationManagerCompat.from(context).cancel(PROGRESS_ID)
    }

    companion object {
        const val PROGRESS_CHANNEL = "transfer_progress_channel"
        const val RESULT_CHANNEL = "transfer_result_channel"
        private const val PROGRESS_ID = 1001
    }
}
```

- [ ] **Step 2: Wire startup interrupted marking**

In `TransferManager`, add:

```kotlin
fun initialize() {
    scope.launch { markInterruptedOnStartup() }
}
```

In `MainActivity`, inject and initialize:

```kotlin
@Inject lateinit var transferManager: TransferManager
@Inject lateinit var notificationController: TransferNotificationController

// inside onCreate before setContent
notificationController.ensureChannels()
transferManager.initialize()
```

Ensure imports include `javax.inject.Inject`, `TransferManager`, `TransferNotificationController`.

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/transfer app/src/main/java/com/textvision/alistclient/MainActivity.kt
git commit -m "feat: add transfer notifications and startup recovery"
```

---

## Phase 3b Completion Gate

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Manual check:

1. Create or simulate an active transfer row, kill app with `adb shell am kill com.textvision.alistclient`, reopen.
2. Verify row becomes `Interrupted` and retry label is `重新传输`.
3. Verify notification channels exist in Android app notification settings.
