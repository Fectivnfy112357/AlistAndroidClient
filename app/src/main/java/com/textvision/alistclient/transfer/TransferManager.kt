package com.textvision.alistclient.transfer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.file.FileNameValidator
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
        activeCalls.remove(id)?.cancel()
        activeJobs.remove(id)?.cancel()
        scope.launch {
            // Only transitions Cancelled from active states; never clobbers Success/Failed/Interrupted/Cancelled.
            dao.cancelActiveTask(id, null, System.currentTimeMillis())
        }
    }

    fun delete(id: String) {
        cancelNotified[id] = true
        activeCalls.remove(id)?.cancel()
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
        activeCalls.forEach { (_, call) -> call.cancel() }
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeCalls.clear()
        activeJobs.clear()
        cancelNotified.clear()
        scope.launch { dao.deleteAll() }
    }

    private suspend fun runDownload(id: String, remotePath: String, displayName: String, generation: Long) {
        downloadSemaphore.withPermit {
            var call: Call? = null
            var uri: Uri? = null
            try {
                if (!isActive(generation, id)) return
                updateStatusUnlessCancelled(id, TransferStatus.Downloading, null, generation)
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
                    uri = createDownloadUri(displayName)
                    val output = requireNotNull(context.contentResolver.openOutputStream(uri)) { "无法创建下载文件" }
                    val body = response.body ?: error("响应体为空")
                    val progressBody = TransferProgressResponseBody(body) { done, total ->
                        progress.tryUpdate(done, total)
                    }
                    output.use { target -> progressBody.byteStream().use { input -> input.copyTo(target) } }
                    progress.flush()
                    publishDownloadUri(uri)
                    updateStatusUnlessCancelled(id, TransferStatus.Success, null, generation)
                }
            } catch (t: Throwable) {
                uri?.let { discardDownloadUri(it) }
                if (!isCancellation(t)) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, localizeDownloadFailure(t), generation)
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
                val request = uploadRequest(uploadPath, TransferProgressRequestBody(streamBody) { done, length -> progress.tryUpdate(done, length) })
                call = okHttpClient.newCall(request)
                activeCalls[id] = call
                call.execute().use { response ->
                    if (!isActive(generation, id)) return
                    when {
                        response.code == 401 -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传中断，请重试", generation)
                        response.isSuccessful -> {
                            val result = response.body?.string().orEmpty().toAlistUploadResult()
                            if (result != null && !result.isSuccess) {
                                updateStatusUnlessCancelled(id, TransferStatus.Failed, localizeUploadFailure(result.code, result.message), generation)
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
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, localizeDownloadFailure(t), generation)
                }
            } finally {
                activeCalls.remove(id, call)
            }
        }
    }

    private fun uploadRequest(uploadPath: String, body: RequestBody): Request = Request.Builder()
        .url(transferUrl("api/fs/put"))
        .put(body)
        .header("File-Path", uploadPath)
        .build()

    private fun transferUrl(path: String): String = transferUrl(path, null)

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

    private fun publishDownloadUri(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun discardDownloadUri(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

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

    /**
     * Map a raw alist upload error code + message to a user-friendly Chinese
     * explanation. The server's `message` is intentionally not echoed verbatim
     * because the source is English and not actionable.
     */
    internal fun localizeUploadFailure(code: Int, rawMessage: String?): String {
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

    /**
     * Map a download-side throwable to a user-friendly Chinese message. We
     * avoid surfacing the raw OkHttp/IOException text because it's English
     * network jargon ("Unable to resolve host ...", "Connect timed out").
     */
    internal fun localizeDownloadFailure(t: Throwable): String {
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
