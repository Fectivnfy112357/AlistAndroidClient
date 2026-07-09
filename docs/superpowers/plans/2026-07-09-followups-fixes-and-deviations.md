# Followups Fixes & Deviations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate 2 pre-existing flaky tests + complete 4 documentation tasks for the deviations recorded in `.superpowers/sdd/progress.md` lines 162-170.

**Architecture:** Two-pronged fix:
- **Code (Tasks 1-4):** Extract `TransferExecutor` interface so the test can substitute a `FakeExecutor` driven by `TestScope` instead of `BlockingOkHttpClient.execute()` IO deadlock. Replace `HomeRepositoryTest` `requestCount` assertions with `MockWebServer.takeRequest(timeout)` path matching.
- **Docs (Tasks 5-8):** Replace remaining `Cloud*` token production references with `App*`, delete `CloudTypography`, add `TransferManager.runDownload` SDK<Q guard rationale KDoc, expand `progress.md` P1-5 entry, create `_deviation-template.md` and backfill 4 known deviations.

**Tech Stack:** Kotlin 2.0.21, Coroutines 1.8+, Robolectric, MockWebServer, JUnit 4, Hilt 2.52.

**Spec:** `docs/superpowers/specs/2026-07-09-followups-fixes-and-deviations-design.md`

## Global Constraints

- minSdk 26 (Android 8.0); targetSdk 34
- Kotlin JVM target 17
- Single-module project (`app/`)
- Test framework: JUnit 4 + MockK + Turbine + Robolectric + MockWebServer; coroutine tests on `StandardTestDispatcher` with `advanceUntilIdle()` or explicit `runTest(UnconfinedTestDispatcher)`
- File line cap: 400 (per project convention)
- No ktlint/detekt; only standard Android Lint via `:app:lintDebug`
- DI bindings live in `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`
- `@ApplicationScope` qualifier already exists in `AppModule.kt:140` — reuse, do not recreate
- `@IoDispatcher` qualifier already exists in `AppModule.kt:47` — reuse, do not recreate
- All new public types get a one-line KDoc; new Composable functions get a `@Preview` (project convention)
- No `TBD`/`TODO` placeholders, no `// implement later` comments
- Each task ends with an independently-testable deliverable and a single git commit

## File Structure

### New Files
| Path | Purpose |
|------|---------|
| `app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt` | Interface separating IO from state-machine; `RealTransferExecutor` (production) + `TransferOutcome` sealed type |
| `app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt` | Test double for `TransferExecutor` (lives in test source set — Robolectric classpath only) |
| `docs/superpowers/plans/_deviation-template.md` | Deviation log template + 4 backfilled records |

### Modified Files
| Path | Reason |
|------|--------|
| `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt` | Replace direct `runDownload`/`runUpload` bodies with `executor.run*` calls; inject `@ApplicationScope`; add SDK<Q KDoc |
| `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt:333` | KDoc above `@RequiresApi(Q) createDownloadUri` referencing runDownload guard |
| `app/src/main/java/com/textvision/alistclient/di/AppModule.kt` | Add `provideRealTransferExecutor` Hilt binding for `TransferExecutor` |
| `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt` | Replace `BlockingOkHttpClient` with `FakeTransferExecutor` + `TestScope`; rewrite `deleteCancelsActiveTransferAndRemovesRecord` |
| `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt` | Rewrite `retrySectionRefetchesOnlyThatSection` using `takeRequest(timeout)` |
| `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt` | Replace 4× `CloudShapes.Control` → inline `RoundedCornerShape(18.dp)` (or new `AppShapes.Control` if added) |
| `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt` | Replace 3× `CloudMotion.{DurationMediumMillis, FloatTween, OffsetTween}` → `AppMotion.*` |
| `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt` | Replace 1× `CloudMotion.SpringFast` → `AppMotion.SpringFast` |
| `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` | Add `object AppMotion` with same constants; keep `CloudMotion` as `@Deprecated` forwarding object; rewrite `cloudClickable` to use `AppMotion` |
| `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt` | Remove `object CloudShapes` (0 production references after replacement); production sites use inline `RoundedCornerShape` directly |
| `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt` | Delete `val CloudTypography = AppTypography` (line 121) and the backward-compat comment (line 11) |
| `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt` | Update demo text + references: `CloudShapes Card/Panel/Pill` → `AppShapes Card/Panel/Pill` (only `ThemePreviews.kt` may reference `CloudShapes`/`CloudMotion` after Task 5) |
| `.superpowers/sdd/progress.md` line 130 (P1-5 entry) | Expand with fix commit + bug description + test pointers |
| `.superpowers/sdd/progress.md` line 172 | Add link to `_deviation-template.md` |

### Verification Commands (all tasks)
```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug

# Build
./gradlew :app:assembleDebug

# Flaky counter-test (Task 4 only)
for i in {1..100}; do
  ./gradlew :app:testDebugUnitTest \
    --tests "com.textvision.alistclient.transfer.TransferManagerTest.deleteCancelsActiveTransferAndRemovesRecord" \
    --tests "com.textvision.alistclient.ui.feature.home.HomeRepositoryTest.retrySectionRefetchesOnlyThatSection" \
    --rerun-tasks
  if [ $? -ne 0 ]; then echo "FAIL at iteration $i"; exit 1; fi
done
echo "100 iterations OK"

# Grep checks (Task 5)
grep -rn "CloudShapes\.\|CloudMotion\.\|CloudTypography\." app/src/main | grep -v "Motion.kt:.*@Deprecated\|Motion.kt:.*object CloudMotion\|Motion.kt:.*get()\|Motion.kt:.*= AppMotion"
# Expected: no output
```

---

## Task 1: Extract TransferExecutor interface + scope injection

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt:43-50` (constructor + scope)
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt` (add Hilt binding)
- Test: `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt` (compilation check)

**Interfaces:**
- Consumes: `TransferDao`, `OkHttpClient`, `Context`, `SessionManager`, `TransferNotificationController`
- Produces: `TransferExecutor` interface with two suspend methods + `TransferOutcome` sealed type (Task 2 uses these for Fake)

**Why this exists:** `TransferManager.scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` is hardcoded at line 50, so tests cannot substitute `TestScope`. Extracting IO into `TransferExecutor` lets the test inject `FakeTransferExecutor` + `TestScope`, eliminating the `BlockingOkHttpClient.execute()` deadlock that drives the flaky test.

- [ ] **Step 1: Write the failing compile test (Hilt binding exists check)**

Add to `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt` (top of class, before any `@Test`):

```kotlin
@Test
fun transferManagerAcceptsInjectedExecutorAndScope() {
    // Compile-time check: TransferManager constructor accepts (TransferExecutor, CoroutineScope)
    // Runtime check: a no-op FakeTransferExecutor + TestScope boots without crashing
    val executor = FakeTransferExecutor()
    val scope = TestScope()
    val manager = TransferManager(
        dao = MemoryTransferDao(),
        executor = executor,
        scope = scope,
        notificationController = TransferNotificationController(RuntimeEnvironment.getApplication()),
    )
    // Smoke: scope is reachable; no crash
    assertEquals(executor, manager.executorForTest)
}
```

This requires adding `executorForTest` accessor to `TransferManager` and a `FakeTransferExecutor` stub (write the stub first, full impl in Task 2).

- [ ] **Step 2: Run test to verify it fails to compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferManagerTest.transferManagerAcceptsInjectedExecutorAndScope"`
Expected: COMPILATION FAILURE (FakeTransferExecutor not defined, TransferManager constructor mismatch)

- [ ] **Step 3: Create TransferExecutor.kt with interface + outcome type**

Create `app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.net.Uri

/**
 * Pluggable IO backend for [TransferManager]. The production implementation
 * performs HTTP + MediaStore writes; tests substitute a fake driven by a
 * TestScope to avoid Dispatchers.IO deadlock and Robolectric shared-state
 * races. Implementations MUST honor [isActive] before each progress/status
 * write — TransferManager delegates cancellation, status, and notification
 * concerns to itself.
 */
interface TransferExecutor {
    suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome

    suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome
}

/** Outcome surfaced by [TransferExecutor.runDownload]/[runUpload] to TransferManager. */
sealed interface TransferOutcome {
    data object Success : TransferOutcome
    data class Failed(val reason: String) : TransferOutcome
    data object Cancelled : TransferOutcome
}
```

- [ ] **Step 4: Create minimal FakeTransferExecutor stub for compile**

Create `app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.net.Uri

/**
 * Test double for [TransferExecutor]. Full cancel/IO-deadlock simulation lives
 * here in Task 2; this stub exists only to satisfy the compiler for Task 1.
 */
class FakeTransferExecutor : TransferExecutor {
    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = TransferOutcome.Success

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = TransferOutcome.Success
}
```

- [ ] **Step 5: Refactor TransferManager to inject executor + scope**

Replace `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt` lines 43-149 (constructor + `enqueueDownload` + `enqueueUpload`) with:

```kotlin
@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TransferDao,
    private val executor: TransferExecutor,
    @ApplicationScope private val scope: CoroutineScope,
    private val notificationController: TransferNotificationController = TransferNotificationController(context),
) {
    private val uploadSemaphore = Semaphore(2)
    private val downloadSemaphore = Semaphore(3)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val clearGeneration = AtomicLong(0L)
    private val cancelNotified = ConcurrentHashMap<String, Boolean>()

    /** Test-only accessor used by TransferManagerTest.transferManagerAcceptsInjectedExecutorAndScope. */
    internal val executorForTest: TransferExecutor get() = executor

    fun observeTransfers(): kotlinx.coroutines.flow.Flow<List<TransferEntity>> = dao.observeAll()

    suspend fun markInterruptedOnStartup() {
        dao.markActiveTasksInterrupted(System.currentTimeMillis())
    }

    fun initialize() {
        scope.launch { markInterruptedOnStartup() }
    }

    fun enqueueDownload(remotePath: String, fileName: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val displayName = remotePath.substringAfterLast('/').takeIf { it.isNotBlank() } ?: fileName
        val local = "${LocalDownloadNamer.publicDownloadsRelativePath}/$displayName"
        val generation = clearGeneration.get()
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            if (generation != clearGeneration.get()) return@launch
            dao.upsert(TransferEntity(id, fileName, remotePath, local, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
            runDownload(id, remotePath, displayName, generation)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
        return id
    }

    fun enqueueUpload(uri: Uri, targetPath: String): String {
        persistUploadUriPermission(uri)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val fileName = UriDisplayNameResolver.resolve(context.contentResolver, uri, now)
        val generation = clearGeneration.get()
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            if (generation != clearGeneration.get()) return@launch
            dao.upsert(TransferEntity(id, fileName, targetPath, null, uri.toString(), 0, 0, TransferType.Upload, TransferStatus.Waiting, null, now, now))
            runUpload(id, uri, targetPath, fileName, generation)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
        return id
    }

    fun cancel(id: String) {
        cancelNotified[id] = true
        activeJobs.remove(id)?.cancel()
        scope.launch {
            dao.cancelActiveTask(id, null, System.currentTimeMillis())
        }
    }

    fun delete(id: String) {
        cancelNotified[id] = true
        activeJobs.remove(id)?.cancel()
        scope.launch {
            dao.deleteById(id)
            cancelNotified.remove(id)
        }
    }

    fun retry(id: String) {
        val generation = clearGeneration.get()
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val task = dao.find(id) ?: return@launch
            if (generation != clearGeneration.get() || !task.status.canRetry) return@launch
            updateStatusUnlessCancelled(id, TransferStatus.Waiting, null, generation)
            when (task.type) {
                TransferType.Download -> runDownload(id, task.remotePath, task.fileName, generation)
                TransferType.Upload -> runUpload(id, Uri.parse(requireNotNull(task.sourceUri)), task.remotePath, task.fileName, generation)
            }
        }
        if (activeJobs.putIfAbsent(id, job) != null) {
            job.cancel()
            return
        }
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
    }

    fun clearAllTasks() {
        clearGeneration.incrementAndGet()
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
        cancelNotified.clear()
        scope.launch { dao.deleteAll() }
    }

    private suspend fun runDownload(id: String, remotePath: String, displayName: String, generation: Long) {
        downloadSemaphore.withPermit {
            if (!isActive(generation, id)) return
            updateStatusUnlessCancelled(id, TransferStatus.Downloading, null, generation)
            val progress = ProgressCollector(id, generation)
            val outcome = executor.runDownload(
                id = id,
                remotePath = remotePath,
                displayName = displayName,
                onProgress = { done, total -> progress.tryUpdate(done, total) },
                isActive = { isActive(generation, id) },
            )
            progress.flush()
            when (outcome) {
                TransferOutcome.Success -> updateStatusUnlessCancelled(id, TransferStatus.Success, null, generation)
                is TransferOutcome.Failed -> updateStatusUnlessCancelled(id, TransferStatus.Failed, outcome.reason, generation)
                TransferOutcome.Cancelled -> Unit
            }
        }
    }

    private suspend fun runUpload(id: String, uri: Uri, targetPath: String, fileName: String, generation: Long) {
        uploadSemaphore.withPermit {
            if (FileNameValidator.errorMessage(fileName) != null) {
                updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传路径无效，请重命名后重试", generation)
                return
            }
            if (!isActive(generation, id)) return
            updateStatusUnlessCancelled(id, TransferStatus.Uploading, null, generation)
            val progress = ProgressCollector(id, generation)
            val uploadPath = sanitizeUploadPath(targetPath, fileName)
            if (uploadPath == null) {
                updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传路径无效，请重命名后重试", generation)
                return
            }
            val outcome = executor.runUpload(
                id = id,
                uri = uri,
                targetPath = uploadPath,
                fileName = fileName,
                onProgress = { done, total -> progress.tryUpdate(done, total) },
                isActive = { isActive(generation, id) },
            )
            progress.flush()
            when (outcome) {
                TransferOutcome.Success -> updateStatusUnlessCancelled(id, TransferStatus.Success, null, generation)
                is TransferOutcome.Failed -> updateStatusUnlessCancelled(id, TransferStatus.Failed, outcome.reason, generation)
                TransferOutcome.Cancelled -> Unit
            }
        }
    }

    private inner class ProgressCollector(id: String, generation: Long) {
        private val progress = ThrottledProgress(id, generation)
        private val channel = kotlinx.coroutines.channels.Channel<Pair<Long, Long>>(kotlinx.coroutines.channels.Channel.CONFLATED)
        private val job = scope.launch {
            for ((bytesDone, totalBytes) in channel) {
                progress.update(bytesDone, totalBytes)
            }
        }

        fun tryUpdate(bytesDone: Long, totalBytes: Long) {
            channel.trySend(bytesDone to totalBytes)
        }

        suspend fun flush() {
            channel.close()
            job.join()
            progress.flush()
        }
    }

    private inner class ThrottledProgress(private val id: String, private val generation: Long) {
        private var lastWrittenBytes = 0L
        private var lastWriteMillis = 0L
        private var latestBytes = 0L
        private var latestTotal = 0L
        private val debounceIntervalMs = 250L
        private val debounceByteThreshold = 65_536L

        suspend fun update(bytesDone: Long, totalBytes: Long, force: Boolean = false) {
            if (!isActive(generation, id)) return
            latestBytes = bytesDone
            latestTotal = totalBytes
            val now = System.currentTimeMillis()
            val deltaBytes = bytesDone - lastWrittenBytes
            val deltaTime = now - lastWriteMillis
            if (!force && deltaBytes < debounceByteThreshold && deltaTime < debounceIntervalMs) return
            dao.updateProgress(id, latestBytes, latestTotal, now)
            lastWrittenBytes = latestBytes
            lastWriteMillis = now
        }

        suspend fun flush() { update(latestBytes, latestTotal, force = true) }
    }

    private suspend fun updateStatusUnlessCancelled(id: String, status: TransferStatus, reason: String?, generation: Long) {
        if (isActive(generation, id)) {
            dao.updateStatus(id, status, reason, System.currentTimeMillis())
            when (status) {
                TransferStatus.Uploading, TransferStatus.Downloading -> notificationController.showProgressSummary(1, null)
                TransferStatus.Success, TransferStatus.Failed, TransferStatus.Cancelled, TransferStatus.Interrupted -> notificationController.clearProgress()
                TransferStatus.Waiting -> Unit
            }
        }
    }

    private suspend fun isActive(generation: Long, id: String): Boolean =
        generation == clearGeneration.get() && !isCancelled(id)

    private suspend fun isCancelled(id: String): Boolean {
        cancelNotified[id]?.let { if (it) return true }
        val cancelled = dao.find(id)?.status == TransferStatus.Cancelled
        if (cancelled) cancelNotified[id] = true
        return cancelled
    }
}
```

Also remove the old `runDownload` (lines 219-260), `runUpload` (lines 262-323), `createDownloadUri` (lines 333-343), `publishDownloadUri` (lines 345-349), `discardDownloadUri` (lines 351-353), `persistUploadUriPermission` (lines 355-360), `localizeUploadFailure` (lines 401-414), `localizeDownloadFailure` (lines 421-432) — they all migrate into `RealTransferExecutor` in Step 6.

Keep `transferUrl` (lines 362-370), `sanitizeUploadPath` (lines 372-378), `encodeHeaderPath` (lines 380-384), `toAlistUploadResult` (lines 386-394) — these are URL/path helpers consumed by `RealTransferExecutor`.

- [ ] **Step 6: Create RealTransferExecutor with migrated IO logic**

Append to `app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt`:

```kotlin
package com.textvision.alistclient.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.file.FileNameValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [TransferExecutor] backed by OkHttp + ContentResolver. Behavior is
 * the verbatim migration of the original TransferManager.runDownload/runUpload
 * bodies — no semantic change beyond the extraction.
 */
@Singleton
class RealTransferExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val okHttpClient: OkHttpClient,
) : TransferExecutor {
    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = withContext(kotlinx.coroutines.Dispatchers.IO) {
        var call: Call? = null
        var uri: Uri? = null
        try {
            if (!isActive()) return@withContext TransferOutcome.Cancelled
            val request = Request.Builder().url(transferUrl("d", remotePath)).get().build()
            call = okHttpClient.newCall(request)
            call.execute().use { response ->
                if (!isActive()) return@withContext TransferOutcome.Cancelled
                if (!response.isSuccessful) {
                    return@withContext TransferOutcome.Failed("下载失败：HTTP ${response.code}")
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    return@withContext TransferOutcome.Failed("需要 Android 10 或更高版本以保存到下载目录")
                }
                uri = createDownloadUri(displayName)
                val output = requireNotNull(context.contentResolver.openOutputStream(uri)) { "无法创建下载文件" }
                val body = response.body ?: error("响应体为空")
                val progressBody = TransferProgressResponseBody(body, onProgress)
                output.use { target -> progressBody.byteStream().use { input -> input.copyTo(target) } }
                publishDownloadUri(uri!!)
                TransferOutcome.Success
            }
        } catch (t: Throwable) {
            uri?.let { discardDownloadUri(it) }
            when {
                isCancellation(t) -> TransferOutcome.Cancelled
                else -> TransferOutcome.Failed(localizeDownloadFailure(t))
            }
        }
    }

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (FileNameValidator.errorMessage(fileName) != null) {
            return@withContext TransferOutcome.Failed("上传路径无效，请重命名后重试")
        }
        var call: Call? = null
        try {
            if (!isActive()) return@withContext TransferOutcome.Cancelled
            val resolver = context.contentResolver
            val total = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            val streamBody = object : RequestBody() {
                override fun contentType() = resolver.getType(uri)?.toMediaTypeOrNull()
                override fun contentLength() = total
                override fun writeTo(sink: BufferedSink) {
                    resolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "无法读取选择的文件" }
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            sink.write(buffer, 0, read)
                        }
                    }
                }
            }
            val request = uploadRequest(targetPath, TransferProgressRequestBody(streamBody, onProgress))
            call = okHttpClient.newCall(request)
            call.execute().use { response ->
                if (!isActive()) return@withContext TransferOutcome.Cancelled
                when {
                    response.code == 401 -> TransferOutcome.Failed("上传中断，请重试")
                    response.isSuccessful -> {
                        val result = response.body?.string().orEmpty().toAlistUploadResult()
                        if (result != null && !result.isSuccess) {
                            TransferOutcome.Failed(localizeUploadFailure(result.code, result.message))
                        } else {
                            TransferOutcome.Success
                        }
                    }
                    response.code == 409 -> TransferOutcome.Failed("文件已存在，请重命名后重试")
                    else -> TransferOutcome.Failed("上传失败：HTTP ${response.code}")
                }
            }
        } catch (t: Throwable) {
            when {
                isCancellation(t) -> TransferOutcome.Cancelled
                else -> TransferOutcome.Failed(localizeDownloadFailure(t))
            }
        }
    }

    private fun uploadRequest(uploadPath: String, body: RequestBody): Request = Request.Builder()
        .url(transferUrl("api/fs/put"))
        .put(body)
        .header("File-Path", uploadPath)
        .build()

    private fun transferUrl(path: String): String = transferUrl(path, null)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createDownloadUri(displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, LocalDownloadNamer.publicDownloadsRelativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "无法创建下载文件"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishDownloadUri(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun discardDownloadUri(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    private fun transferUrl(rootPath: String, remotePath: String?): String {
        val session = requireNotNull(sessionManager.loadSavedSession()) { "未登录" }
        val builder = session.serverUrl.toHttpUrl().newBuilder()
            .query(null)
            .fragment(null)
        rootPath.trim('/').split('/').filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        remotePath?.trim('/')?.split('/')?.filter { it.isNotBlank() }?.forEach { builder.addPathSegment(it) }
        return builder.build().toString()
    }

    private fun sanitizeUploadPath(targetPath: String, fileName: String): String? {
        val cleanName = fileName.trim()
        if (FileNameValidator.errorMessage(cleanName) != null) return null
        val cleanTarget = targetPath.trim().replace(Regex("[\r\n ]"), "").trimEnd('/')
        val fullPath = if (cleanTarget.isBlank()) "/$cleanName" else "$cleanTarget/$cleanName"
        return if ("http://localhost".toHttpUrlOrNull()?.newBuilder()?.addHeaderPath(fullPath)?.build() == null) null else encodeHeaderPath(fullPath)
    }

    private fun encodeHeaderPath(path: String): String =
        path.trim('/').split('/').filter { it.isNotBlank() }
            .joinToString(separator = "/", prefix = "/") { segment ->
                URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
            }

    private data class AlistUploadResult(val code: Int, val message: String?) {
        val isSuccess: Boolean get() = code == 200
    }

    private fun String.toAlistUploadResult(): AlistUploadResult? {
        val code = Regex("\"code\"\\s*:\\s*(-?\\d+)").find(this)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(this)?.groupValues?.get(1)
        return AlistUploadResult(code, message)
    }

    /**
     * Map a raw alist upload error code + message to a user-friendly Chinese
     * explanation. The server's `message` is intentionally not echoed verbatim
     * because the source is English and not actionable.
     */
    private fun localizeUploadFailure(code: Int, rawMessage: String?): String {
        val lower = rawMessage?.lowercase().orEmpty()
        return when {
            code == 500 || lower.contains("storage not found") -> "存储未挂载，请先在 Alist 后台挂载存储"
            code == 401 || lower.contains("unauthorized") || lower.contains("token") -> "登录已失效，请重新登录"
            code == 403 || lower.contains("permission") -> "没有上传权限"
            code == 404 || lower.contains("not found") -> "目标路径不存在"
            code == 409 || lower.contains("already exists") -> "文件已存在，请重命名后重试"
            code == 50051 || lower.contains("failed get objs") -> "服务器拒绝访问，请检查登录状态"
            code in 500..599 -> "服务器错误（$code），请稍后重试"
            code in 400..499 -> "请求被拒绝（$code），请重试"
            else -> "上传失败，请稍后重试"
        }
    }

    private fun localizeDownloadFailure(t: Throwable): String {
        val msg = t.message.orEmpty().lowercase()
        return when {
            msg.contains("unable to resolve host") || t is java.net.UnknownHostException -> "无法解析服务器地址，请检查网络"
            t is java.net.SocketTimeoutException -> "连接超时，请重试"
            msg.contains("connect") && msg.contains("refused") -> "服务器拒绝连接，请确认服务在线"
            msg.contains("unexpected end of stream") || t is java.io.EOFException -> "下载中断，请重试"
            msg.contains("failed to connect") -> "无法连接服务器"
            msg.contains("ssl") || t is javax.net.ssl.SSLException -> "TLS 握手失败，服务器证书可能不受信任"
            else -> "下载失败，请稍后重试"
        }
    }

    private fun okhttp3.HttpUrl.Builder.addHeaderPath(path: String): okhttp3.HttpUrl.Builder = apply {
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach { addPathSegment(it) }
    }

    private fun isCancellation(t: Throwable): Boolean {
        if (t is kotlinx.coroutines.CancellationException) return true
        if (t is IOException && (t.message == "Canceled" || t.message == "Cancelled")) return true
        if (t is IOException && t.message?.contains("cancel", ignoreCase = true) == true) return true
        return false
    }
}
```

- [ ] **Step 7: Add Hilt binding for TransferExecutor**

In `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`, add a new `@Provides` method inside the existing `abstract class` or `@Module` (whichever matches current style — read the file first to confirm pattern, then insert):

```kotlin
@Provides
@Singleton
fun provideTransferExecutor(impl: RealTransferExecutor): TransferExecutor = impl
```

Place this near other `@Singleton` bindings (after `provideApplicationScope` at line 141).

- [ ] **Step 8: Run the compile test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferManagerTest.transferManagerAcceptsInjectedExecutorAndScope"`
Expected: PASS

- [ ] **Step 9: Run full unit tests + lint to confirm no regression**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```
Expected: All pre-existing tests pass. 11 existing `TransferManagerTest` tests should pass without modification (they exercise `transferUrl`/`sanitizeUploadPath`/`localizeUploadFailure` indirectly via reflection; if any fail, that test used the old `runDownload` body — see Step 10).

- [ ] **Step 10: Update reflection-based test accessors**

Tests in `TransferManagerTest.kt:80,89,151,165,172` use `TransferManager::class.java.declaredMethods.first { it.name.startsWith("localizeUploadFailure") }` and similar for `sanitizeUploadPath`/`toAlistUploadResult`. These methods now live in `RealTransferExecutor`, not `TransferManager`.

Update `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt` lines 78-94, 148-167, 170-180 as follows — change `TransferManager::class` to `RealTransferExecutor::class` for all reflection accessors. Add a field at the top of the test class:

```kotlin
private fun realExecutor(): RealTransferExecutor =
    RealTransferExecutor(RuntimeEnvironment.getApplication(), savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())
```

Then replace each `TransferManager::class.java.declaredMethods.first { it.name.startsWith("localizeUploadFailure") }` with `RealTransferExecutor::class.java.declaredMethods.first { it.name.startsWith("localizeUploadFailure") }` and `localize.invoke(manager, ...)` with `localize.invoke(realExecutor(), ...)`.

For `sanitizeUploadPath`/`toAlistUploadResult` (now private in RealTransferExecutor), they remain accessible via reflection with `isAccessible = true` — only the class changes.

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferManagerTest"`
Expected: All 12 tests pass (11 existing + 1 from Step 1).

- [ ] **Step 11: Commit**

```bash
git add \
  app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt \
  app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt \
  app/src/main/java/com/textvision/alistclient/di/AppModule.kt \
  app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt \
  app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt
git commit -m "refactor(transfer): extract TransferExecutor + inject @ApplicationScope"
```

---

## Task 2: Replace deleteCancelsActive with FakeExecutor + TestScope

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt:132-146`
- Modify: `app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt`

**Why this exists:** The original test uses `BlockingOkHttpClient.execute()` which spins on a `while (!cancelled.get()) Thread.sleep(10)` busy loop — a thread leak that races with subsequent tests in the same JVM. With `TransferExecutor` extracted (Task 1), the fake can simulate the IO deadlock via `awaitCancellation()` on the test's coroutine, fully under `TestScope` control.

- [ ] **Step 1: Rewrite FakeTransferExecutor with cancellation control**

Replace `app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt` with:

```kotlin
package com.textvision.alistclient.transfer

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred

/**
 * Test double for [TransferExecutor] that simulates a long-running IO operation
 * which completes only when [cancel] is invoked. Replaces the original
 * `BlockingOkHttpClient.execute()` busy-spin that caused order-dependent
 * failures in the full test suite.
 */
class FakeTransferExecutor : TransferExecutor {
    private val cancelGate = CompletableDeferred<Unit>()

    /** How many times runDownload was invoked. */
    var downloadCount: Int = 0
        private set
    /** How many times runUpload was invoked. */
    var uploadCount: Int = 0
        private set
    /** Last displayName passed to runDownload — for assertion. */
    var lastDownloadDisplayName: String? = null
        private set

    /** Releases the pending download/upload to return [TransferOutcome.Cancelled]. */
    fun cancel() {
        cancelGate.complete(Unit)
    }

    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome {
        downloadCount++
        lastDownloadDisplayName = displayName
        cancelGate.await()
        return if (isActive()) TransferOutcome.Success else TransferOutcome.Cancelled
    }

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome {
        uploadCount++
        cancelGate.await()
        return if (isActive()) TransferOutcome.Success else TransferOutcome.Cancelled
    }
}
```

- [ ] **Step 2: Rewrite `deleteCancelsActiveTransferAndRemovesRecord`**

Replace `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt:132-146` with:

```kotlin
@Test
fun deleteCancelsActiveTransferAndRemovesRecord() = runBlocking {
    val dao = MemoryTransferDao()
    val executor = FakeTransferExecutor()
    val scope = TestScope(StandardTestDispatcher())
    val manager = TransferManager(
        context = RuntimeEnvironment.getApplication(),
        dao = dao,
        executor = executor,
        scope = scope,
        notificationController = TransferNotificationController(RuntimeEnvironment.getApplication()),
    )

    val id = manager.enqueueDownload("/folder/file.txt", "file.txt")
    // Wait for the executor to be entered — replaces awaitRequest polling.
    scope.testScheduler.runCurrent()
    assertEquals(1, executor.downloadCount)

    manager.delete(id)
    dao.awaitMissing(id)

    assertEquals(0, dao.find(id)?.status?.let { 1 } ?: 0) // row gone
    // Executor was released (no leaked coroutine on the scope).
    scope.testScheduler.advanceUntilIdle()
}
```

Add imports at the top of the test file:

```kotlin
import com.textvision.alistclient.transfer.TransferExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
```

- [ ] **Step 3: Remove `BlockingOkHttpClient` private class**

Delete `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt:268-298` (the `private class BlockingOkHttpClient` block). Remove unused imports:

```kotlin
// Remove:
import okhttp3.Call
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
```

Keep `CapturingOkHttpClient` (still used by other tests) and its imports.

- [ ] **Step 4: Run the rewritten test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferManagerTest.deleteCancelsActiveTransferAndRemovesRecord"`
Expected: PASS

- [ ] **Step 5: Run all TransferManagerTest tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.transfer.TransferManagerTest"`
Expected: All 12 tests pass

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt \
        app/src/test/java/com/textvision/alistclient/transfer/FakeTransferExecutor.kt
git commit -m "test(transfer): rewrite deleteCancelsActive with FakeExecutor + TestScope"
```

---

## Task 3: Rewrite HomeRepositoryTest.retrySection with takeRequest

**Files:**
- Modify: `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt:150-166`

**Why this exists:** The original test counts `server.requestCount` after `enqueue`-ing 13 responses and assumes the dispatcher order matches `loadDashboard`'s internal call sequence. When `runTest(StandardTestDispatcher)` is replaced with `runTest(UnconfinedTestDispatcher)` or other tests in the same suite enqueue extra responses, the count assertion becomes fragile. `takeRequest(timeout)` matches the path explicitly, removing the count assumption.

- [ ] **Step 1: Rewrite the test body**

Replace `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt:150-166` with:

```kotlin
@Test fun retrySectionRefetchesOnlyThatSection() = runTest {
    installPathDispatcher()

    // First load: 6 admin sections + 7 task buckets = 13 requests.
    val r1 = repo.loadDashboard() as ApiResult.Success
    val initialStorage = r1.data.storageSection as SectionResult.Ok
    assertEquals("/local", initialStorage.data.storages.first().mountPath)

    // Drain the initial request queue so takeRequest only sees the retry.
    while (server.requestCount < 13) {
        server.takeRequest(2_000, TimeUnit.MILLISECONDS)
            ?: fail("Expected initial request within 2s; got ${server.requestCount}/13")
    }

    // Re-enqueue the storage list response for the retry.
    enqueue(storageOk())

    // Retry only the storage section.
    val r2 = repo.retrySection(r1.data, SectionKey.Storage)
    val storage = r2.storageSection as SectionResult.Ok
    assertEquals("/local", storage.data.storages.first().mountPath)

    // The retry MUST hit /api/admin/storage/list exactly once.
    val retryRequest = server.takeRequest(2_000, TimeUnit.MILLISECONDS)
        ?: fail("Expected retry request within 2s")
    assertEquals("GET", retryRequest.method)
    assertTrue(
        retryRequest.path?.contains("/api/admin/storage/list") == true,
        "Expected /api/admin/storage/list, got ${retryRequest.path}",
    )

    // No additional request after the retry (public section was NOT refetched).
    val extra = server.takeRequest(200, TimeUnit.MILLISECONDS)
    assertEquals(null, extra)

    // publicSection unchanged.
    assertEquals(
        (r1.data.publicSection as SectionResult.Ok).data.siteTitle,
        (r2.publicSection as SectionResult.Ok).data.siteTitle,
    )
}
```

Add imports:

```kotlin
import java.util.concurrent.TimeUnit
```

- [ ] **Step 2: Run the rewritten test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.home.HomeRepositoryTest.retrySectionRefetchesOnlyThatSection"`
Expected: PASS

- [ ] **Step 3: Run all HomeRepositoryTest tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.feature.home.HomeRepositoryTest"`
Expected: All 4 tests pass

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt
git commit -m "test(home): rewrite retrySection with takeRequest path matching"
```

---

## Task 4: Full validation — tests, lint, flaky counter-test

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass (file count and count same as before Task 1 — no regression)

- [ ] **Step 2: Lint**

Run: `./gradlew :app:lintDebug`
Expected: PASS, baseline ≤ current 615 lines

- [ ] **Step 3: Assemble debug**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 4: Run flaky counter-test for TransferManagerTest.deleteCancelsActive**

Run:
```bash
PASS=0
for i in {1..100}; do
  if ./gradlew :app:testDebugUnitTest \
       --tests "com.textvision.alistclient.transfer.TransferManagerTest.deleteCancelsActiveTransferAndRemovesRecord" \
       --rerun-tasks --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    PASS=$((PASS+1))
  else
    echo "FAIL at iteration $i"
    exit 1
  fi
done
echo "TransferManagerTest: $PASS/100 passed"
```
Expected: `TransferManagerTest: 100/100 passed`

- [ ] **Step 5: Run flaky counter-test for HomeRepositoryTest.retrySection**

Run:
```bash
PASS=0
for i in {1..100}; do
  if ./gradlew :app:testDebugUnitTest \
       --tests "com.textvision.alistclient.ui.feature.home.HomeRepositoryTest.retrySectionRefetchesOnlyThatSection" \
       --rerun-tasks --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    PASS=$((PASS+1))
  else
    echo "FAIL at iteration $i"
    exit 1
  fi
done
echo "HomeRepositoryTest: $PASS/100 passed"
```
Expected: `HomeRepositoryTest: 100/100 passed`

- [ ] **Step 6: Run combined flaky counter-test**

Run:
```bash
PASS=0
for i in {1..50}; do
  if ./gradlew :app:testDebugUnitTest \
       --tests "com.textvision.alistclient.transfer.TransferManagerTest.deleteCancelsActiveTransferAndRemovesRecord" \
       --tests "com.textvision.alistclient.ui.feature.home.HomeRepositoryTest.retrySectionRefetchesOnlyThatSection" \
       --rerun-tasks --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    PASS=$((PASS+1))
  else
    echo "FAIL at iteration $i"
    exit 1
  fi
done
echo "Combined: $PASS/50 passed"
```
Expected: `Combined: 50/50 passed`

- [ ] **Step 7: No commit (verification task)**

If any step failed, return to Task 2 or Task 3 and fix before continuing to Task 5.

---

## Task 5: Replace Cloud* token production references with App*

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt:23-30` (delete `CloudShapes`)
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt:25-91` (add `AppMotion`, deprecate `CloudMotion`, update `cloudClickable`)
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt:11,121` (delete `CloudTypography` + comment)
- Modify: `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt:32,77,148,195,202`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt:15,17,19,21`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt:11,20`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt:106-128`

**Why this exists:** Production code still references `CloudShapes.Control` and `CloudMotion.*` even though `AppShapes` (val of `Shapes`) and a new `AppMotion` are available. Replacing these eliminates the "0 引用但保留" half-state and lets `CloudShapes`/`CloudMotion`/`CloudTypography` be deleted (or kept as `@Deprecated` forwarding objects).

- [ ] **Step 1: Add `AppMotion` and deprecate `CloudMotion`**

Replace `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` lines 25-62 with:

```kotlin
object AppMotion {
    // Spring specs
    val SpringFast: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    val SpringMedium: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val SpringSlow: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    // Tween durations
    const val TweenShort: Int = 120
    const val TweenMedium: Int = 240
    const val TweenLong: Int = 400
    val Easing = FastOutSlowInEasing

    internal const val DurationShortMillis = 120
    internal const val DurationMediumMillis = 240
    internal const val DurationLongMillis = 300

    internal const val PressedScale = 0.94f
    internal const val RestScale = 1f

    internal val FloatTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )

    internal val OffsetTween: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )
}

/**
 * @deprecated Use [AppMotion]. Retained as a forwarding alias during the
 * UI Expressive followup migration; will be removed once no production code
 * references it. See `docs/superpowers/specs/2026-07-09-followups-fixes-and-deviations-design.md` §3.2.
 */
@Deprecated("Use AppMotion", ReplaceWith("AppMotion"))
object CloudMotion {
    val SpringFast: AnimationSpec<Float> get() = AppMotion.SpringFast
    val SpringMedium: AnimationSpec<Float> get() = AppMotion.SpringMedium
    val SpringSlow: AnimationSpec<Float> get() = AppMotion.SpringSlow
    const val TweenShort: Int = AppMotion.TweenShort
    const val TweenMedium: Int = AppMotion.TweenMedium
    const val TweenLong: Int = AppMotion.TweenLong
    val Easing get() = AppMotion.Easing
    internal const val DurationShortMillis: Int = AppMotion.DurationShortMillis
    internal const val DurationMediumMillis: Int = AppMotion.DurationMediumMillis
    internal const val DurationLongMillis: Int = AppMotion.DurationLongMillis
    internal const val PressedScale: Float = AppMotion.PressedScale
    internal const val RestScale: Float = AppMotion.RestScale
    internal val FloatTween: FiniteAnimationSpec<Float> get() = AppMotion.FloatTween
    internal val OffsetTween: FiniteAnimationSpec<IntOffset> get() = AppMotion.OffsetTween
}
```

Note: `const val` cannot delegate via getter syntax for top-level consts; use plain `val` if compiler complains, or use the `get()` form as shown (Kotlin allows `const val` to reference another `const val` directly).

Replace lines 74-76 (`cloudClickable` references) with:

```kotlin
targetValue = if (enabled && pressed) AppMotion.PressedScale else AppMotion.RestScale,
animationSpec = tween(
    durationMillis = AppMotion.DurationShortMillis,
    easing = LinearEasing,
),
```

- [ ] **Step 2: Delete `object CloudShapes` from Shape.kt**

Delete lines 23-30 of `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt`. Leave the file ending at line 21 (the `val AppShapes = Shapes(...)` definition). The git history records the removal; no source comment is added.

- [ ] **Step 3: Delete `CloudTypography` from Type.kt**

Delete `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt` line 121 (`val CloudTypography = AppTypography`) and update the comment at line 11 to remove the "Retains CloudTypography alias" clause:

```kotlin
// Material 3 Expressive Type Scale (spec §3.2)
// All styles use FontFamily.Default (system font) per global constraint.
```

- [ ] **Step 4: Replace CloudShapes.Control in DynamicFormField.kt**

In `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt`:

1. Remove `import com.textvision.alistclient.ui.theme.CloudShapes` (line 32)
2. Add `import androidx.compose.foundation.shape.RoundedCornerShape` and `import androidx.compose.ui.unit.dp`
3. Replace all 4 occurrences of `CloudShapes.Control` with `RoundedCornerShape(18.dp)` (lines 77, 148, 195, 202)

- [ ] **Step 5: Replace CloudMotion in AppNavTransitions.kt**

In `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt`:

1. Change line 15: `import com.textvision.alistclient.ui.theme.CloudMotion` → `import com.textvision.alistclient.ui.theme.AppMotion`
2. Replace line 17 `CloudMotion.DurationMediumMillis` → `AppMotion.DurationMediumMillis`
3. Replace line 19 `CloudMotion.FloatTween` → `AppMotion.FloatTween`
4. Replace line 21 `CloudMotion.OffsetTween` → `AppMotion.OffsetTween`

- [ ] **Step 6: Replace CloudMotion.SpringFast in TransferProgress.kt**

In `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt`:

1. Change line 11: `import com.textvision.alistclient.ui.theme.CloudMotion` → `import com.textvision.alistclient.ui.theme.AppMotion`
2. Replace line 20 `CloudMotion.SpringFast` → `AppMotion.SpringFast`

- [ ] **Step 7: Update ThemePreviews.kt demo text + use local RoundedCornerShape for Card/Panel/Pill**

The Material3 `AppShapes` is a `Shapes` val with `extraSmall/small/medium/large/extraLarge` slots, not a custom Card/Panel/Pill object. To preserve visual fidelity to the previous `CloudShapes.Card (24dp) / Panel (28dp) / Pill (999dp)`, define three local constants at the top of `ThemePreviews.kt`:

```kotlin
private val PreviewCardShape = RoundedCornerShape(24.dp)
private val PreviewPanelShape = RoundedCornerShape(28.dp)
private val PreviewPillShape = RoundedCornerShape(999.dp)
```

Then in `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt`:

1. Replace line 106 `Text("CloudShapes Card / Panel / Pill")` → `Text("AppShapes Card / Panel / Pill")`
2. Replace lines 108-110 `CloudShapes.Card / Panel / Pill` → `PreviewCardShape / PreviewPanelShape / PreviewPillShape`
3. Replace line 117 `@Preview(name = "CloudMotion SpringFast", ...)` → `@Preview(name = "AppMotion SpringFast", ...)`
4. Replace line 124 `Text("CloudMotion.SpringFast animated width")` → `Text("AppMotion.SpringFast animated width")`
5. Replace line 128 `animationSpec = CloudMotion.SpringFast` → `animationSpec = AppMotion.SpringFast`

Add imports at the top of `ThemePreviews.kt`:
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
```

- [ ] **Step 8: Verify no `CloudShapes` / `CloudMotion` / `CloudTypography` production references remain**

Run:
```bash
grep -rn "CloudShapes\.\|CloudMotion\.\|CloudTypography\." app/src/main
```
Expected output: no production references. Allowed locations (for completeness):
- `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` — the `@Deprecated object CloudMotion` forwarding object itself (definitions, not `.` accesses)

If `.`-accessed references to `CloudMotion.X` / `CloudShapes.X` / `CloudTypography.X` remain in any file other than `Motion.kt`, return to Step 4-7 and complete the replacement.

- [ ] **Step 9: Run full tests + lint**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```
Expected: All tests pass. Lint baseline may grow by ~5 lines (new `@Deprecated` warnings on `CloudMotion`); do not fail on lint baseline growth — it's expected.

- [ ] **Step 10: Commit**

```bash
git add \
  app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt \
  app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt \
  app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt \
  app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt \
  app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt \
  app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt \
  app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt
git commit -m "refactor(theme): replace production Cloud* token refs with App*"
```

---

## Task 6: Document SDK<Q guard rationale in TransferManager

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt` (KDoc near `runDownload`)

**Why this exists:** The SDK<Q guard in `RealTransferExecutor.runDownload` (Task 1) prevents silent failures on API 26-28. Without explicit KDoc explaining this is NOT a `@RequiresApi` annotation issue, future maintainers may delete the guard thinking it's a lint suppression.

- [ ] **Step 1: Add KDoc to runDownload in TransferManager**

The actual `runDownload` lives in `RealTransferExecutor` after Task 1 refactor. Add a KDoc at the top of `app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt` above `RealTransferExecutor`:

```kotlin
/**
 * Production [TransferExecutor] backed by OkHttp + ContentResolver.
 *
 * **API 29+ requirement for downloads:** `runDownload` returns
 * `TransferOutcome.Failed("需要 Android 10 或更高版本以保存到下载目录")` when
 * `Build.VERSION.SDK_INT < Q`. This is NOT a `@RequiresApi` lint suppression —
 * it guards a real silent-failure bug: `MediaStore.Downloads.EXTERNAL_CONTENT_URI`
 * with `IS_PENDING` returns null or stale URIs on API 26-28, leaving users with
 * a phantom "success" and no file. Do NOT delete this branch without first
 * either bumping minSdk to 29 or implementing a legacy
 * `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` fallback.
 * See `docs/superpowers/specs/2026-07-09-followups-fixes-and-deviations-design.md` §3.2.
 */
```

- [ ] **Step 2: Confirm `@RequiresApi(Q)` annotations are correct**

Verify `createDownloadUri` and `publishDownloadUri` in `RealTransferExecutor` (lines after Step 6 of Task 1) still carry `@RequiresApi(Build.VERSION_CODES.Q)`. They should — these methods are only called from the SDK<Q-guarded branch.

Run: `grep -n "@RequiresApi" app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt`
Expected: 2 matches, both `(Build.VERSION_CODES.Q)`.

- [ ] **Step 3: Verify lint baseline has no NewApi entry for TransferManager**

Run: `grep -n "MediaStore.Downloads\|createDownloadUri\|publishDownloadUri" app/lint-baseline.xml`
Expected: No matches (Task 21 already removed NewApi entries; verify still absent).

If matches exist, delete them:

```bash
# Delete each NewApi entry that mentions MediaStore.Downloads, createDownloadUri, or publishDownloadUri
```

- [ ] **Step 4: Run lint**

Run: `./gradlew :app:lintDebug`
Expected: PASS, no new lint errors

- [ ] **Step 5: Commit**

```bash
git add \
  app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt \
  app/lint-baseline.xml
git commit -m "docs(transfer): document SDK<Q guard rationale in TransferExecutor"
```

---

## Task 7: Expand progress.md P1-5 entry with fix details

**Files:**
- Modify: `.superpowers/sdd/progress.md:130`

**Why this exists:** The current one-line summary "fix 后 0 critical, ..." doesn't tell future maintainers *what* the bug was, *which test* guards the regression, or *which commit* fixed it.

- [ ] **Step 1: Locate the P1-5 line**

Run: `grep -n "P1-5\|SessionEventBus" .superpowers/sdd/progress.md | head -20`
Expected: Line ~130 contains `Task 9+10+11 (P1-5):` and references `SessionEventBus`.

- [ ] **Step 2: Expand the entry**

Replace line 130 (the line beginning with `Task 9+10+11 (P1-5):`) with the expanded version (preserve indentation and surrounding context):

```markdown
Task 9+10+11 (P1-5): complete (commits 1319de7..36c0f9b, 4 commit 含 1 fix, review APPROVED after fix, 0 critical/0 important)
  - **fix commit**: `36c0f9b` (AdminRepository: emit SessionEvent.Unauthorized only on refresh-failure, not on retry-still-Unauthorized)
  - **首版 bug**: 首版 AdminRepository.runAdmin 在第一个 401/403 即 emit `SessionEvent.Unauthorized`, 触发 SessionGate.logout → MainActivity 跳 LoginDest. 但 403 是权限不足 (admin-only 端点对非 admin 用户) 而非会话失效, 抢跑 refresh 重试会成功 (refresh 本身不需要 admin 权限), 然后 retry 仍 403 → 此时误判为"会话失效".
  - **修法**: AdminRepository.runAdmin 仅在 `refreshAndRetry()` 自身失败 (refresh 返回 false = credentials revoked 或网络失败) 时 emit; FileRepository.runAlistWithRefresh 因 `first.code != 401` 守卫短路 403, 无需改.
  - **回归测试**: `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt:111 admin401LeavesSectionsFailedAndPublicOk` (验证 admin 401 → 所有 admin section Failed 但 publicSection 仍 Ok, 且 SessionGate 不触发 logout).
  - **未覆盖 (留待 follow-up)**: FileRepository.fs/list 若服务端返 401 (非 403) 仍有同形 bug, refresh-success-still-401 路径未测试; 见 progress.md 行 173.
```

- [ ] **Step 3: Verify the edit**

Run: `grep -A 5 "fix commit" .superpowers/sdd/progress.md`
Expected: The "fix commit: 36c0f9b" line appears under Task 9+10+11 (P1-5) entry.

- [ ] **Step 4: Commit**

```bash
git add .superpowers/sdd/progress.md
git commit -m "docs(sdd): expand P1-5 progress.md entry with fix commit and bug description"
```

---

## Task 8: Create _deviation-template.md and backfill 4 known deviations

**Files:**
- Create: `docs/superpowers/plans/_deviation-template.md`
- Modify: `.superpowers/sdd/progress.md:172` (add link)

**Why this exists:** Plan-to-implementation drift (e.g., `HyperOsMotion` → `CloudMotion`) is a recurring pattern across this project. Without a durable deviation log, each new plan repeats the same guessing-implementer-correcting loop.

- [ ] **Step 1: Create the template file**

Create `docs/superpowers/plans/_deviation-template.md`:

```markdown
# Plan ↔ Implementer Deviation Log

> When a task review surfaces a divergence between the plan's stated name/signature/parameter and what the implementer actually wrote (e.g., plan named the constant `HyperOsMotion` but the codebase used `CloudMotion`), append one row to this table.
>
> Goal: prevent future plan authors from guessing the wrong symbol name. Real values accumulate here so each subsequent plan can grep them.

## Format

| task_id | plan original | implementer actual | reason |

## Known Deviations (Backfilled 2026-07-09)

| task_id | plan original | implementer actual | reason |
|---------|---------------|--------------------|--------|
| Task 19 (P2-15, followups plan §Task 19) | `PreviewDestArgs.mime: String` carries MIME type | `PreviewDestArgs.mime: String` carries `FileType.name` (later corrected by P2-15 task to real MIME) | Plan guessed `mime` parameter was a MIME string; codebase used enum-name bridge until proper MIME resolution was added in Task 25 |
| Task 8 (UI Expressive plan §Task 8) | Bottom nav item includes `onMoreClick` parameter | Bottom nav uses M3 `ListItemRow` `trailing` slot for the More menu | Plan guessed a callback hook; codebase already had a slot-based trailing widget pattern |
| Task 17 (UI Expressive plan §Task 17) | `HyperOsMotion.SpringFast` constant | `CloudMotion.SpringFast` constant (`HyperOsMotion` does not exist in codebase) | Plan author confused motion namespace; codebase uses `CloudMotion` (later renamed to `AppMotion` in followups Task 5) |
| Task 13 (UI Expressive plan §Task 13) | `fileCategoryFromMime(null, name)` helper | `toFileCategory()` extension on `FileItem` matching `FileScreen` usage | Plan guessed a nullable-MIME overload; codebase normalized to non-null `FileItem.toFileCategory()` for consistency |
```

- [ ] **Step 2: Add link in progress.md**

In `.superpowers/sdd/progress.md` at line 172 (the `## 关键偏离` section, or the line ending it), append:

```markdown
See also: [`_deviation-template.md`](../../docs/superpowers/plans/_deviation-template.md) for the durable plan↔implementer deviation log (4 backfilled entries as of 2026-07-09).
```

Verify the relative path resolves correctly: `docs/superpowers/plans/_deviation-template.md` from project root, and `.superpowers/sdd/progress.md` from project root. The relative path from `progress.md` to the template is `../../docs/superpowers/plans/_deviation-template.md`.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/plans/_deviation-template.md .superpowers/sdd/progress.md
git commit -m "docs(deviations): create _deviation-template.md + backfill 4 known deviations"
```

---

## Final Validation (run after all tasks)

- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- [ ] `grep -rn "CloudShapes\.\|CloudMotion\.\|CloudTypography\." app/src/main` → no production references (only `Motion.kt` `@Deprecated` forwarding object definitions acceptable)
- [ ] `grep -rn "BlockingOkHttpClient" app/src/test` → no output
- [ ] Flaky counter-test: both `deleteCancelsActive` and `retrySectionRefetchesOnlyThatSection` 100/100 (Tasks 4 reused)

If any check fails, return to the offending task and fix before declaring plan complete.