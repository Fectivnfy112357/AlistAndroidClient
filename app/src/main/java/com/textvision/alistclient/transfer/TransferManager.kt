package com.textvision.alistclient.transfer

import android.content.Context
import android.net.Uri
import com.textvision.alistclient.auth.SessionManager
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TransferDao,
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadSemaphore = Semaphore(2)
    private val downloadSemaphore = Semaphore(3)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()
    @Volatile private var clearGeneration = 0L

    fun observeTransfers(): kotlinx.coroutines.flow.Flow<List<TransferEntity>> = dao.observeAll()

    suspend fun markInterruptedOnStartup() {
        dao.markActiveTasksInterrupted(System.currentTimeMillis())
    }

    fun enqueueDownload(remotePath: String, fileName: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val local = File(File(context.filesDir, "downloads"), LocalDownloadNamer.fileNameFor(remotePath))
        val generation = clearGeneration
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            if (generation != clearGeneration) return@launch
            dao.upsert(TransferEntity(id, fileName, remotePath, local.absolutePath, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
            runDownload(id, remotePath, local, generation)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
        return id
    }

    fun enqueueUpload(uri: Uri, targetPath: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val fileName = UriDisplayNameResolver.resolve(context.contentResolver, uri, now)
        val generation = clearGeneration
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            if (generation != clearGeneration) return@launch
            dao.upsert(TransferEntity(id, fileName, targetPath, null, uri.toString(), 0, 0, TransferType.Upload, TransferStatus.Waiting, null, now, now))
            runUpload(id, uri, targetPath, fileName, generation)
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
        return id
    }

    fun cancel(id: String) {
        activeCalls.remove(id)?.cancel()
        activeJobs.remove(id)?.cancel()
        scope.launch { dao.updateStatus(id, TransferStatus.Cancelled, null, System.currentTimeMillis()) }
    }

    fun retry(id: String) {
        val generation = clearGeneration
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val task = dao.find(id) ?: return@launch
            if (generation != clearGeneration || !task.status.canRetry) return@launch
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
        clearGeneration++
        activeCalls.forEach { (_, call) -> call.cancel() }
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeCalls.clear()
        activeJobs.clear()
        scope.launch { dao.deleteAll() }
    }

    private suspend fun runDownload(id: String, remotePath: String, localFile: File, generation: Long) {
        downloadSemaphore.withPermit {
            var call: Call? = null
            try {
                if (generation != clearGeneration || isCancelled(id)) return
                dao.updateStatus(id, TransferStatus.Downloading, null, System.currentTimeMillis())
                localFile.parentFile?.mkdirs()
                val request = Request.Builder().url(transferUrl("d", remotePath)).get().build()
                call = okHttpClient.newCall(request)
                activeCalls[id] = call
                call.execute().use { response ->
                    if (generation != clearGeneration || isCancelled(id)) return
                    if (!response.isSuccessful) {
                        updateStatusUnlessCancelled(id, TransferStatus.Failed, "下载失败：HTTP ${response.code}")
                        return
                    }
                    val body = response.body ?: error("响应体为空")
                    val progressBody = TransferProgressResponseBody(body) { done, total ->
                        scope.launch { updateProgressUnlessCancelled(id, done, total) }
                    }
                    localFile.outputStream().use { output -> progressBody.byteStream().use { input -> input.copyTo(output) } }
                    updateStatusUnlessCancelled(id, TransferStatus.Success, null)
                }
            } catch (t: Throwable) {
                if (!isCancellation(t) && !isCancelled(id)) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, t.message ?: "下载失败")
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
                if (generation != clearGeneration || isCancelled(id)) return
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
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传路径无效，请重命名后重试")
                    return
                }
                val request = Request.Builder()
                    .url(transferUrl("api/fs/put"))
                    .put(TransferProgressRequestBody(streamBody) { done, length -> scope.launch { updateProgressUnlessCancelled(id, done, length) } })
                    .header("File-Path", uploadPath)
                    .also { SkipAuthRetry.mark(it) }
                    .build()
                call = okHttpClient.newCall(request)
                activeCalls[id] = call
                call.execute().use { response ->
                    if (generation != clearGeneration || isCancelled(id)) return
                    when {
                        response.code == 401 -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传中断，请重试")
                        response.isSuccessful -> updateStatusUnlessCancelled(id, TransferStatus.Success, null)
                        response.code == 409 -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "文件已存在，请重命名后重试")
                        else -> updateStatusUnlessCancelled(id, TransferStatus.Failed, "上传失败：HTTP ${response.code}")
                    }
                }
            } catch (t: Throwable) {
                if (!isCancellation(t) && !isCancelled(id)) {
                    updateStatusUnlessCancelled(id, TransferStatus.Failed, t.message ?: "上传失败")
                }
            } finally {
                activeCalls.remove(id, call)
            }
        }
    }

    private fun transferUrl(path: String): String = transferUrl(path, null)

    private fun transferUrl(rootPath: String, remotePath: String?): String {
        val session = requireNotNull(sessionManager.loadSavedSession()) { "未登录" }
        val builder = session.serverUrl.toHttpUrl().newBuilder()
            .query(null)
            .fragment(null)
        rootPath.trim('/').split('/').filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        remotePath?.trim('/')?.split('/')?.filter { it.isNotBlank() }?.forEach { builder.addEncodedPathSegment(it.replace("%25", "%")) }
        return builder.build().toString()
    }

    private fun sanitizeUploadPath(targetPath: String, fileName: String): String? {
        val cleanName = fileName.trim().replace(Regex("[\\r\\n\\u0000]"), "")
        if (cleanName.isBlank() || cleanName.contains('/')) return null
        val cleanTarget = targetPath.trim().replace(Regex("[\\r\\n\\u0000]"), "").trimEnd('/')
        val fullPath = if (cleanTarget.isBlank()) "/$cleanName" else "$cleanTarget/$cleanName"
        return if ("http://localhost".toHttpUrlOrNull()?.newBuilder()?.addHeaderPath(fullPath)?.build() == null) null else fullPath
    }

    private fun okhttp3.HttpUrl.Builder.addHeaderPath(path: String): okhttp3.HttpUrl.Builder = apply {
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach { addPathSegment(it) }
    }

    private suspend fun updateStatusUnlessCancelled(id: String, status: TransferStatus, reason: String?) {
        if (!isCancelled(id)) {
            dao.updateStatus(id, status, reason, System.currentTimeMillis())
        }
    }

    private suspend fun updateProgressUnlessCancelled(id: String, bytesDone: Long, totalBytes: Long) {
        if (!isCancelled(id)) {
            dao.updateProgress(id, bytesDone, totalBytes, System.currentTimeMillis())
        }
    }

    private suspend fun isCancelled(id: String): Boolean = dao.find(id)?.status == TransferStatus.Cancelled

    private fun isCancellation(t: Throwable): Boolean = t is kotlinx.coroutines.CancellationException || t is IOException && t.message == "Canceled"
}
