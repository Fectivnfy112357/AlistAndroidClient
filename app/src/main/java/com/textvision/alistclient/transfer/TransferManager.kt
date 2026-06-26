package com.textvision.alistclient.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.file.FileNameValidator
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TransferDao,
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    private val notificationController: TransferNotificationController = TransferNotificationController(context),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadSemaphore = Semaphore(2)
    private val downloadSemaphore = Semaphore(3)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val clearGeneration = AtomicLong(0L)
    /**
     * Local cancel-notification cache keyed by transfer id. Populated when the
     * UI requests cancellation so subsequent status/progress writes can avoid a
     * `dao.find(id)` per write; falls back to the DB read on miss.
     */
    private val cancelNotified = ConcurrentHashMap<String, Boolean>()

    /**
     * Throttled progress writer per transfer ID.
     * Flushes at most once per 250ms or 64KB of progress to avoid per-chunk Room writes.
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
        val local = File(File(context.filesDir, "downloads"), LocalDownloadNamer.fileNameFor(remotePath))
        val generation = clearGeneration.get()
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            if (generation != clearGeneration.get()) return@launch
            dao.upsert(TransferEntity(id, fileName, remotePath, local.absolutePath, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
            runDownload(id, remotePath, local, generation)
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
        activeCalls.remove(id)?.cancel()
        activeJobs.remove(id)?.cancel()
        scope.launch {
            // Only transitions Cancelled from active states; never clobbers Success/Failed/Interrupted/Cancelled.
            dao.cancelActiveTask(id, null, System.currentTimeMillis())
        }
    }

    fun retry(id: String) {
        val generation = clearGeneration.get()
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val task = dao.find(id) ?: return@launch
            if (generation != clearGeneration.get() || !task.status.canRetry) return@launch
            updateStatusUnlessCancelled(id, TransferStatus.Waiting, null, generation)
            when (task.type) {
                TransferType.Download -> runDownload(id, task.remotePath, File(requireNotNull(task.localPath)), generation)
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
        activeCalls.forEach { (_, call) -> call.cancel() }
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeCalls.clear()
        activeJobs.clear()
        cancelNotified.clear()
        scope.launch { dao.deleteAll() }
    }

    private suspend fun runDownload(id: String, remotePath: String, localFile: File, generation: Long) {
        downloadSemaphore.withPermit {
            var call: Call? = null
            try {
                if (!isActive(generation, id)) return
                updateStatusUnlessCancelled(id, TransferStatus.Downloading, null, generation)
                localFile.parentFile?.mkdirs()
                val progress = ProgressCollector(id, generation)
                val request = Request.Builder().url(transferUrl("d", remotePath)).get().build()
                call = okHttpClient.newCall(request)
                activeCalls[id] = call
                call.execute().use { response ->
                    if (!isActive(generation, id)) return
                    if (!response.isSuccessful) {
                        updateStatusUnlessCancelled(id, TransferStatus.Failed, "下载失败：HTTP ${response.code}", generation)
                        return
                    }
                    val body = response.body ?: error("响应体为空")
                    val progressBody = TransferProgressResponseBody(body) { done, total ->
                        progress.tryUpdate(done, total)
                    }
                    localFile.outputStream().use { output -> progressBody.byteStream().use { input -> input.copyTo(output) } }
                    progress.flush()
                    updateStatusUnlessCancelled(id, TransferStatus.Success, null, generation)
                }
            } catch (t: Throwable) {
                if (!isCancellation(t)) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, t.message ?: "下载失败", generation)
                }
            } finally {
                activeCalls.remove(id, call)
            }
        }
    }

    private suspend fun runUpload(id: String, uri: Uri, targetPath: String, fileName: String, generation: Long) {
        uploadSemaphore.withPermit {
            var call: Call? = null
            try {
                if (FileNameValidator.errorMessage(fileName) != null) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传路径无效，请重命名后重试", generation)
                    return
                }
                if (!isActive(generation, id)) return
                updateStatusUnlessCancelled(id, TransferStatus.Uploading, null, generation)
                val progress = ProgressCollector(id, generation)
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
                val uploadPath = sanitizeUploadPath(targetPath, fileName)
                if (uploadPath == null) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传路径无效，请重命名后重试", generation)
                    return
                }
                val request = Request.Builder()
                    .url(transferUrl("api/fs/put"))
                    .put(TransferProgressRequestBody(streamBody) { done, length -> progress.tryUpdate(done, length) })
                    .header("File-Path", uploadPath)
                    .also { SkipAuthRetry.mark(it) }
                    .build()
                call = okHttpClient.newCall(request)
                activeCalls[id] = call
                call.execute().use { response ->
                    if (!isActive(generation, id)) return
                    when {
                        response.code == 401 -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传中断，请重试", generation)
                        response.isSuccessful -> {
                            val result = response.body?.string().orEmpty().toAlistUploadResult()
                            if (result != null && !result.isSuccess) {
                                updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传失败：${result.message ?: "服务器返回失败"}", generation)
                                return
                            }
                            progress.flush()
                            updateStatusUnlessCancelled(id, TransferStatus.Success, null, generation)
                        }
                        response.code == 409 -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "文件已存在，请重命名后重试", generation)
                        else -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传失败：HTTP ${response.code}", generation)
                    }
                }
            } catch (t: Throwable) {
                if (!isCancellation(t)) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, t.message ?: "上传失败", generation)
                }
            } finally {
                activeCalls.remove(id, call)
            }
        }
    }

    private fun transferUrl(path: String): String = transferUrl(path, null)

    private fun persistUploadUriPermission(uri: Uri) {
        if (uri.scheme != "content") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
        val cleanTarget = targetPath.trim().replace(Regex("[\r\n\u0000]"), "").trimEnd('/')
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

    private fun okhttp3.HttpUrl.Builder.addHeaderPath(path: String): okhttp3.HttpUrl.Builder = apply {
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach { addPathSegment(it) }
    }

    private suspend fun updateStatusUnlessCancelled(id: String, status: TransferStatus, reason: String?, generation: Long) {
        if (isActive(generation, id)) {
            dao.updateStatus(id, status, reason, System.currentTimeMillis())
            when (status) {
                TransferStatus.Uploading, TransferStatus.Downloading -> notificationController.showProgressSummary(activeCalls.size.coerceAtLeast(1), null)
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

    private fun isCancellation(t: Throwable): Boolean {
        if (t is kotlinx.coroutines.CancellationException) return true
        if (t is IOException && (t.message == "Canceled" || t.message == "Cancelled")) return true
        // Some providers wrap the cancellation message; fall back to a substring check.
        if (t is IOException && t.message?.contains("cancel", ignoreCase = true) == true) return true
        return false
    }
}
