package com.textvision.alistclient.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.textvision.alistclient.di.ApplicationScope
import com.textvision.alistclient.file.FileNameValidator
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

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
    /**
     * Local cancel-notification cache keyed by transfer id. Populated when the
     * UI requests cancellation so subsequent status/progress writes can avoid a
     * `dao.find(id)` per write; falls back to the DB read on miss.
     */
    private val cancelNotified = ConcurrentHashMap<String, Boolean>()

    /** Test-only accessor used by TransferManagerTest.transferManagerAcceptsInjectedExecutorAndScope. */
    internal val executorForTest: TransferExecutor get() = executor

    /**
     * Throttled progress writer per transfer ID.
     *
     * Time-gated: caps Room writes at ~[debounceIntervalMs] per transfer even
     * on fast links. The previous `bytes < threshold || time < interval` gate
     * meant high-speed uploads could trigger a write every 64KB (≈16/s on a
     * 1MB/s stream × N concurrent transfers), invalidating every
     * `dao.observeAll()` consumer on each tick. The new gate is time alone
     * with a `deltaBytes > 0` sanity check to skip no-progress writes; the
     * 64KB threshold is kept only as a [bytesJustChangedForDoc] constant for
     * the public docstring and removed from the hot path.
     *
     * `force=true` (used by [flush] on terminal events) bypasses the gate.
     */
    private inner class ThrottledProgress(
        private val id: String,
        private val generation: Long,
    ) {
        private var lastWrittenBytes = 0L
        private var lastWriteMillis = 0L
        private var latestBytes = 0L
        private var latestTotal = 0L
        private val debounceIntervalMs = 250L

        suspend fun update(bytesDone: Long, totalBytes: Long, force: Boolean = false) {
            if (!isActive(generation, id)) return
            latestBytes = bytesDone
            latestTotal = totalBytes
            val now = System.currentTimeMillis()
            val deltaBytes = bytesDone - lastWrittenBytes
            val deltaTime = now - lastWriteMillis
            if (!force) {
                // Time-gate first: 250ms between writes regardless of byte volume.
                if (deltaTime < debounceIntervalMs) return
                // Skip no-progress writes (e.g. emitter spammed same bytesDone).
                if (deltaBytes <= 0L) return
            }
            dao.updateProgress(id, latestBytes, latestTotal, now)
            lastWrittenBytes = latestBytes
            lastWriteMillis = now
        }

        suspend fun flush() {
            update(latestBytes, latestTotal, force = true)
        }
    }

    /**
     * Serializes progress writes for one transfer. Producers use [tryUpdate], which
     * is non-blocking and conflated, so fast network/body callbacks cannot enqueue
     * unbounded coroutines or Room writes.
     */
    private inner class ProgressCollector(
        id: String,
        generation: Long,
    ) {
        private val progress = ThrottledProgress(id, generation)
        private val channel = Channel<Pair<Long, Long>>(Channel.CONFLATED)
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

    fun observeTransfers(): Flow<List<TransferEntity>> = dao.observeAll()

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
        executor.cancel(id)
        activeJobs.remove(id)?.cancel()
        scope.launch {
            // Only transitions Cancelled from active states; never clobbers Success/Failed/Interrupted/Cancelled.
            dao.cancelActiveTask(id, null, System.currentTimeMillis())
        }
    }

    fun delete(id: String) {
        cancelNotified[id] = true
        executor.cancel(id)
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
        // Atomic incrementAndGet: captures the new generation and publishes it
        // so any later reader sees a consistent value.
        clearGeneration.incrementAndGet()
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
        cancelNotified.clear()
        executor.cancelAll()
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
            val outcome = executor.runUpload(
                id = id,
                uri = uri,
                targetPath = targetPath,
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

    private fun persistUploadUriPermission(uri: Uri) {
        if (uri.scheme != "content") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private suspend fun updateStatusUnlessCancelled(id: String, status: TransferStatus, reason: String?, generation: Long) {
        if (isActive(generation, id)) {
            dao.updateStatus(id, status, reason, System.currentTimeMillis())
            when (status) {
                TransferStatus.Uploading, TransferStatus.Downloading -> notificationController.showProgressSummary(executor.activeCallCount().coerceAtLeast(1), null)
                TransferStatus.Success, TransferStatus.Failed, TransferStatus.Cancelled, TransferStatus.Interrupted -> notificationController.clearProgress()
                TransferStatus.Waiting -> Unit
            }
        }
    }

    /**
     * Single cancel-guard predicate used by every in-flight status and progress
     * write path. Returns true if the captured [generation] is still current
     * and the row is not already Cancelled.
     */
    private suspend fun isActive(generation: Long, id: String): Boolean =
        generation == clearGeneration.get() && !isCancelled(id)

    /**
     * Single source of truth for "is this row already cancelled?" for in-flight
     * status and progress writes. The local cache is populated by [cancel] and
     * is cheaper than a per-write `dao.find(id)`. On cache miss we still fall
     * back to the DB so external processes (e.g. retry from another path) are
     * honoured.
     */
    private suspend fun isCancelled(id: String): Boolean {
        cancelNotified[id]?.let { if (it) return true }
        val cancelled = dao.find(id)?.status == TransferStatus.Cancelled
        if (cancelled) cancelNotified[id] = true
        return cancelled
    }
}
