# Phase 3a Transfer Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the minimum usable upload/download transfer system with Room persistence, private download storage, FileProvider open/share, progress, cancel, fail, interrupt, and retry states.

**Architecture:** `TransferManager` owns queueing and concurrency. Room persists transfer records. UI observes `TransferDao.observeAll()`. Uploads use `ActivityResultContracts.GetContent()` and URI streaming. Downloads write to `filesDir/downloads/{sha1(path)}.ext`.

**Tech Stack:** Kotlin coroutines, Room, OkHttp, Compose, Hilt, FileProvider, Android content resolver.

## Global Constraints

- Upload concurrency = 2; download concurrency = 3.
- Download location = `filesDir/downloads/{sha1(path)}.ext`.
- No reliable background transfer, no resumable transfer, no range resume.
- Upload 401 becomes Failed with message `上传中断，请重试`; do not trigger Authenticator retry for upload calls.
- `Cancelled` has no retry; `Failed` has `重试`; `Interrupted` has `重新传输`.
- Upload main endpoint is `PUT /api/fs/put`.
- Downloads use `/d/{path}` with Authorization header supplied by the shared OkHttp client.

---

## File Structure

Create:

```text
app/src/main/java/com/textvision/alistclient/transfer/model/TransferStatus.kt
app/src/main/java/com/textvision/alistclient/transfer/model/TransferType.kt
app/src/main/java/com/textvision/alistclient/transfer/data/TransferEntity.kt
app/src/main/java/com/textvision/alistclient/transfer/data/TransferDao.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferProgressRequestBody.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferProgressResponseBody.kt
app/src/main/java/com/textvision/alistclient/transfer/LocalDownloadNamer.kt
app/src/main/java/com/textvision/alistclient/transfer/UriDisplayNameResolver.kt
app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt
app/src/test/java/com/textvision/alistclient/transfer/LocalDownloadNamerTest.kt
app/src/test/java/com/textvision/alistclient/transfer/UriDisplayNameResolverTest.kt
app/src/test/java/com/textvision/alistclient/transfer/TransferStatusTest.kt
```

Modify:

```text
app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt
app/src/main/java/com/textvision/alistclient/di/AppModule.kt
app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt
app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt
```

---

### Task 3a.1: Transfer Room schema and status rules

**Files:**
- Create: `transfer/model/TransferStatus.kt`
- Create: `transfer/model/TransferType.kt`
- Create: `transfer/data/TransferEntity.kt`
- Create: `transfer/data/TransferDao.kt`
- Modify: `data/local/AppDatabase.kt`
- Test: `transfer/TransferStatusTest.kt`

**Interfaces:**
- Produces: `TransferStatus`, `TransferType`, `TransferEntity`, `TransferDao.observeAll()`.
- Consumes: Room database from Phase 0.

- [ ] **Step 1: Write failing status tests**

```kotlin
package com.textvision.alistclient.transfer

import com.textvision.alistclient.transfer.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferStatusTest {
    @Test fun retryRulesMatchSpec() {
        assertTrue(TransferStatus.Failed.canRetry)
        assertTrue(TransferStatus.Interrupted.canRetry)
        assertFalse(TransferStatus.Cancelled.canRetry)
        assertFalse(TransferStatus.Success.canRetry)
    }

    @Test fun interruptedAndFailedUseDifferentLabels() {
        assertEquals("重试", TransferStatus.Failed.retryLabel)
        assertEquals("重新传输", TransferStatus.Interrupted.retryLabel)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferStatusTest"
```

Expected: FAIL with unresolved reference for `TransferStatus`.

- [ ] **Step 3: Implement status and entity**

`TransferStatus.kt`:

```kotlin
package com.textvision.alistclient.transfer.model

enum class TransferStatus(val canRetry: Boolean, val retryLabel: String?) {
    Waiting(canRetry = false, retryLabel = null),
    Uploading(canRetry = false, retryLabel = null),
    Downloading(canRetry = false, retryLabel = null),
    Success(canRetry = false, retryLabel = null),
    Failed(canRetry = true, retryLabel = "重试"),
    Cancelled(canRetry = false, retryLabel = null),
    Interrupted(canRetry = true, retryLabel = "重新传输"),
}
```

`TransferType.kt`:

```kotlin
package com.textvision.alistclient.transfer.model

enum class TransferType { Upload, Download }
```

`TransferEntity.kt`:

```kotlin
package com.textvision.alistclient.transfer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType

@Entity(tableName = "transfer_tasks")
data class TransferEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val remotePath: String,
    val localPath: String?,
    val sourceUri: String?,
    val bytesDone: Long,
    val totalBytes: Long,
    val type: TransferType,
    val status: TransferStatus,
    val failureReason: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
```

`TransferDao.kt`:

```kotlin
package com.textvision.alistclient.transfer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.textvision.alistclient.transfer.model.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_tasks ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfer_tasks WHERE id = :id")
    suspend fun find(id: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferEntity)

    @Query("UPDATE transfer_tasks SET status = :status, failureReason = :reason, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateStatus(id: String, status: TransferStatus, reason: String?, updatedAtMillis: Long)

    @Query("UPDATE transfer_tasks SET bytesDone = :bytesDone, totalBytes = :totalBytes, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateProgress(id: String, bytesDone: Long, totalBytes: Long, updatedAtMillis: Long)

    @Query("UPDATE transfer_tasks SET status = 'Interrupted', failureReason = '传输中断', updatedAtMillis = :updatedAtMillis WHERE status IN ('Waiting', 'Uploading', 'Downloading')")
    suspend fun markActiveTasksInterrupted(updatedAtMillis: Long)

    @Query("DELETE FROM transfer_tasks")
    suspend fun deleteAll()
}
```

Modify `AppDatabase.kt`:

```kotlin
@Database(
    entities = [SmokeEntity::class, TransferEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
    abstract fun transferDao(): TransferDao
}
```

- [ ] **Step 4: Run tests and build**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferStatusTest" :app:assembleDebug
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/transfer app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt app/src/test/java/com/textvision/alistclient/transfer/TransferStatusTest.kt
git commit -m "feat: add transfer persistence model"
```

---

### Task 3a.2: Local download naming and URI display names

**Files:**
- Create: `transfer/LocalDownloadNamer.kt`
- Create: `transfer/UriDisplayNameResolver.kt`
- Test: `transfer/LocalDownloadNamerTest.kt`
- Test: `transfer/UriDisplayNameResolverTest.kt`

**Interfaces:**
- Produces: `LocalDownloadNamer.fileNameFor(remotePath: String): String`.
- Produces: `UriDisplayNameResolver.resolve(contentResolver, uri, nowMillis)`.
- Consumes: none.

- [ ] **Step 1: Write failing tests**

`LocalDownloadNamerTest.kt`:

```kotlin
package com.textvision.alistclient.transfer

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDownloadNamerTest {
    @Test fun sameFileNameDifferentPathDoesNotCollide() {
        val a = LocalDownloadNamer.fileNameFor("/a/file.zip")
        val b = LocalDownloadNamer.fileNameFor("/b/file.zip")
        assertNotEquals(a, b)
        assertTrue(a.endsWith(".zip"))
        assertTrue(b.endsWith(".zip"))
    }
}
```

`UriDisplayNameResolverTest.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class UriDisplayNameResolverTest {
    @Test fun fallbackUsesLastPathSegmentBeforeDefaultName() {
        val uri = Uri.parse("content://provider/tree/photo.jpg")
        assertEquals("photo.jpg", UriDisplayNameResolver.fallbackName(uri, 123L))
    }

    @Test fun fallbackUsesTimestampWhenNoSegment() {
        val uri = Uri.parse("content://provider")
        assertEquals("upload-123", UriDisplayNameResolver.fallbackName(uri, 123L))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.LocalDownloadNamerTest" --tests "com.textvision.alistclient.transfer.UriDisplayNameResolverTest"
```

Expected: FAIL with unresolved references for helper classes.

- [ ] **Step 3: Implement helpers**

`LocalDownloadNamer.kt`:

```kotlin
package com.textvision.alistclient.transfer

import java.security.MessageDigest

object LocalDownloadNamer {
    fun fileNameFor(remotePath: String): String {
        val extension = remotePath.substringAfterLast('.', missingDelimiterValue = "").takeIf { it.isNotBlank() && !it.contains('/') }
        val digest = MessageDigest.getInstance("SHA-1").digest(remotePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return if (extension == null) digest else "$digest.$extension"
    }
}
```

`UriDisplayNameResolver.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

object UriDisplayNameResolver {
    fun resolve(contentResolver: ContentResolver, uri: Uri, nowMillis: Long): String {
        val queried = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
        return queried ?: fallbackName(uri, nowMillis)
    }

    fun fallbackName(uri: Uri, nowMillis: Long): String = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "upload-$nowMillis"
}
```

- [ ] **Step 4: Run tests and verify pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.LocalDownloadNamerTest" --tests "com.textvision.alistclient.transfer.UriDisplayNameResolverTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/transfer/LocalDownloadNamer.kt app/src/main/java/com/textvision/alistclient/transfer/UriDisplayNameResolver.kt app/src/test/java/com/textvision/alistclient/transfer
git commit -m "feat: add transfer file naming helpers"
```

---

### Task 3a.3: Progress bodies and usable TransferManager

**Files:**
- Create: `transfer/TransferProgressRequestBody.kt`
- Create: `transfer/TransferProgressResponseBody.kt`
- Create: `transfer/TransferManager.kt`
- Modify: `di/AppModule.kt`

**Interfaces:**
- Produces: `TransferManager.observeTransfers()`, `enqueueDownload(remotePath, fileName)`, `enqueueUpload(uri, targetPath)`, `cancel(id)`, `retry(id)`, `markInterruptedOnStartup()`.
- Consumes: `TransferDao`, shared `OkHttpClient`, `ContentResolver`.

- [ ] **Step 1: Add progress bodies**

`TransferProgressRequestBody.kt`:

```kotlin
package com.textvision.alistclient.transfer

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

class TransferProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var written = 0L
        val forwarding = object : ForwardingSink(sink) {
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onProgress(written, total)
            }
        }
        val buffered = forwarding.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }
}
```

`TransferProgressResponseBody.kt`:

```kotlin
package com.textvision.alistclient.transfer

import okhttp3.ResponseBody
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

class TransferProgressResponseBody(
    private val delegate: ResponseBody,
    private val onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
) : ResponseBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun source(): BufferedSource {
        val total = contentLength()
        var readTotal = 0L
        return object : ForwardingSource(delegate.source()) {
            override fun read(sink: okio.Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) {
                    readTotal += read
                    onProgress(readTotal, total)
                }
                return read
            }
        }.buffer()
    }
}
```

- [ ] **Step 2: Add TransferManager**

`TransferManager.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.content.Context
import android.net.Uri
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TransferDao,
    private val okHttpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadSemaphore = Semaphore(2)
    private val downloadSemaphore = Semaphore(3)

    fun observeTransfers(): Flow<List<TransferEntity>> = dao.observeAll()

    suspend fun markInterruptedOnStartup() {
        dao.markActiveTasksInterrupted(System.currentTimeMillis())
    }

    fun enqueueDownload(remotePath: String, fileName: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val local = File(File(context.filesDir, "downloads"), LocalDownloadNamer.fileNameFor(remotePath))
        scope.launch {
            dao.upsert(TransferEntity(id, fileName, remotePath, local.absolutePath, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
            runDownload(id, remotePath, local)
        }
        return id
    }

    fun enqueueUpload(uri: Uri, targetPath: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val fileName = UriDisplayNameResolver.resolve(context.contentResolver, uri, now)
        scope.launch {
            dao.upsert(TransferEntity(id, fileName, targetPath, null, uri.toString(), 0, 0, TransferType.Upload, TransferStatus.Waiting, null, now, now))
            runUpload(id, uri, targetPath, fileName)
        }
        return id
    }

    fun cancel(id: String) {
        scope.launch { dao.updateStatus(id, TransferStatus.Cancelled, null, System.currentTimeMillis()) }
    }

    fun retry(id: String) {
        scope.launch {
            val task = dao.find(id) ?: return@launch
            when (task.type) {
                TransferType.Download -> runDownload(id, task.remotePath, File(requireNotNull(task.localPath)))
                TransferType.Upload -> runUpload(id, Uri.parse(requireNotNull(task.sourceUri)), task.remotePath, task.fileName)
            }
        }
    }

    fun clearAllTasks() {
        scope.launch { dao.deleteAll() }
    }

    private suspend fun runDownload(id: String, remotePath: String, localFile: File) {
        downloadSemaphore.withPermit {
            try {
                dao.updateStatus(id, TransferStatus.Downloading, null, System.currentTimeMillis())
                localFile.parentFile?.mkdirs()
                val encoded = remotePath.trimStart('/')
                val request = Request.Builder().url("d/$encoded").get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        dao.updateStatus(id, TransferStatus.Failed, "下载失败：HTTP ${response.code}", System.currentTimeMillis())
                        return
                    }
                    val body = response.body ?: error("响应体为空")
                    val progressBody = TransferProgressResponseBody(body) { done, total ->
                        scope.launch { dao.updateProgress(id, done, total, System.currentTimeMillis()) }
                    }
                    localFile.outputStream().use { output -> progressBody.byteStream().use { input -> input.copyTo(output) } }
                    dao.updateStatus(id, TransferStatus.Success, null, System.currentTimeMillis())
                }
            } catch (t: Throwable) {
                dao.updateStatus(id, TransferStatus.Failed, t.message ?: "下载失败", System.currentTimeMillis())
            }
        }
    }

    private suspend fun runUpload(id: String, uri: Uri, targetPath: String, fileName: String) {
        uploadSemaphore.withPermit {
            try {
                dao.updateStatus(id, TransferStatus.Uploading, null, System.currentTimeMillis())
                val resolver = context.contentResolver
                val total = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                val streamBody = object : RequestBody() {
                    override fun contentType() = resolver.getType(uri)?.toMediaTypeOrNull()
                    override fun contentLength() = total
                    override fun writeTo(sink: BufferedSink) {
                        resolver.openInputStream(uri).use { input ->
                            requireNotNull(input) { "无法读取选择的文件" }
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var written = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                sink.write(buffer, 0, read)
                                written += read
                                scope.launch { dao.updateProgress(id, written, total, System.currentTimeMillis()) }
                            }
                        }
                    }
                }
                val request = Request.Builder()
                    .url("api/fs/put")
                    .put(TransferProgressRequestBody(streamBody) { done, length -> scope.launch { dao.updateProgress(id, done, length, System.currentTimeMillis()) } })
                    .header("File-Path", targetPath.trimEnd('/') + "/" + fileName)
                    .header(SkipAuthRetry.HEADER, "true")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 -> dao.updateStatus(id, TransferStatus.Failed, "上传中断，请重试", System.currentTimeMillis())
                        response.isSuccessful -> dao.updateStatus(id, TransferStatus.Success, null, System.currentTimeMillis())
                        response.code == 409 -> dao.updateStatus(id, TransferStatus.Failed, "文件已存在，请重命名后重试", System.currentTimeMillis())
                        else -> dao.updateStatus(id, TransferStatus.Failed, "上传失败：HTTP ${response.code}", System.currentTimeMillis())
                    }
                }
            } catch (t: Throwable) {
                dao.updateStatus(id, TransferStatus.Failed, t.message ?: "上传失败", System.currentTimeMillis())
            }
        }
    }
}
```

- [ ] **Step 3: Provide TransferDao**

Add to `DatabaseModule` in `AppModule.kt`:

```kotlin
@Provides
fun provideTransferDao(database: AppDatabase): com.textvision.alistclient.transfer.data.TransferDao = database.transferDao()
```

- [ ] **Step 4: Build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/transfer app/src/main/java/com/textvision/alistclient/di/AppModule.kt
git commit -m "feat: add transfer manager"
```

---

### Task 3a.4: Transfer screen and progress UI

**Files:**
- Create: `ui/components/TransferProgress.kt`
- Modify: `ui/screens/TransferScreen.kt`

**Interfaces:**
- Produces: transfer list with status text, progress indicator, cancel/retry buttons.
- Consumes: `TransferManager.observeTransfers()`.

- [ ] **Step 1: Add progress component**

`TransferProgress.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable

@Composable
fun TransferProgress(bytesDone: Long, totalBytes: Long) {
    if (totalBytes > 0 && totalBytes >= bytesDone) {
        val progress = (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progress })
    } else {
        LinearProgressIndicator()
    }
}
```

- [ ] **Step 2: Replace TransferScreen**

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.ui.components.TransferProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val manager: TransferManager,
) : ViewModel() {
    val transfers = manager.observeTransfers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
}

@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("传输")
        if (transfers.isEmpty()) Text("暂无传输任务")
        LazyColumn {
            items(transfers, key = { it.id }) { task ->
                TransferRow(task, onCancel = { viewModel.cancel(task.id) }, onRetry = { viewModel.retry(task.id) })
            }
        }
    }
}

@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    ListItem(
        headlineContent = { Text(task.fileName) },
        supportingContent = {
            Column {
                Text(task.status.name + (task.failureReason?.let { "：$it" } ?: ""))
                TransferProgress(task.bytesDone, task.totalBytes)
                Row {
                    if (task.status in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) {
                        Button(onClick = onCancel) { Text("取消") }
                    }
                    if (task.status.canRetry) {
                        Button(onClick = onRetry) { Text(task.status.retryLabel ?: "重试") }
                    }
                }
            }
        }
    )
}
```

- [ ] **Step 3: Build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui app/src/main/java/com/textvision/alistclient/transfer
git commit -m "feat: add transfer list screen"
```

---

## Phase 3a Completion Gate

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Manual check:

1. Open Transfer tab.
2. Verify empty state says `暂无传输任务`.
3. Start a download or upload from File tab after its action menu is wired.
4. Verify Waiting/Downloading/Uploading/Failed/Cancelled/Interrupted labels render distinctly.
