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
