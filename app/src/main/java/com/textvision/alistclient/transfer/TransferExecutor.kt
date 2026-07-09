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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluggable IO backend for [TransferManager]. The production implementation
 * performs HTTP + MediaStore writes; tests substitute a fake driven by a
 * TestScope to avoid Dispatchers.IO deadlock and Robolectric shared-state
 * races. Implementations MUST honor [isActive] before each progress/status
 * write — TransferManager delegates cancellation, status, and notification
 * concerns to itself.
 *
 * Implementations MUST honor [cancel] so that TransferManager can interrupt
 * an in-flight OkHttp Call synchronously when the user requests cancellation;
 * coroutine cancellation alone cannot unblock a blocking [okhttp3.Call.execute].
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

    /**
     * Cancel any in-flight IO bound to [id]. No-op when [id] is unknown.
     * Invoked synchronously by TransferManager.cancel/delete so the blocking
     * OkHttp [okhttp3.Call.execute] unblocks immediately rather than waiting
     * for coroutine cancellation to propagate through Dispatchers.IO.
     */
    fun cancel(id: String)
}

/** Outcome surfaced by [TransferExecutor.runDownload]/[runUpload] to TransferManager. */
sealed interface TransferOutcome {
    data object Success : TransferOutcome
    data class Failed(val reason: String) : TransferOutcome
    data object Cancelled : TransferOutcome
}

/**
 * Production [TransferExecutor] backed by OkHttp + ContentResolver. Behavior is
 * the verbatim migration of the original TransferManager.runDownload/runUpload
 * bodies — no semantic change beyond the extraction.
 *
 * Cancellation: an OkHttp [Call] is registered in [activeCalls] for the
 * duration of the IO block, and the enclosing coroutine's [Job] is observed
 * via [invokeOnCompletion]. When the caller (TransferManager) cancels the
 * parent job, the call's [Call.cancel] fires so the blocking [Call.execute]
 * unblocks and surfaces the cancellation through the standard exception path.
 */
@Singleton
class RealTransferExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val okHttpClient: OkHttpClient,
) : TransferExecutor {
    private val activeCalls = ConcurrentHashMap<String, Call>()

    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = withContext(Dispatchers.IO) {
        var call: Call? = null
        var uri: Uri? = null
        try {
            if (!isActive()) return@withContext TransferOutcome.Cancelled
            val request = Request.Builder().url(transferUrl("d", remotePath)).get().build()
            call = okHttpClient.newCall(request)
            trackCall(id, call)
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
                val progressBody = TransferProgressResponseBody(body, onProgress = onProgress)
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
        } finally {
            call?.let { activeCalls.remove(id, it) }
        }
    }

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = withContext(Dispatchers.IO) {
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
            val request = uploadRequest(targetPath, TransferProgressRequestBody(streamBody, onProgress = onProgress))
            call = okHttpClient.newCall(request)
            trackCall(id, call)
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
        } finally {
            call?.let { activeCalls.remove(id, it) }
        }
    }

    /**
     * Register [call] under [id] and wire the enclosing coroutine's [Job] to
     * fire [Call.cancel] when the parent job completes (e.g. TransferManager
     * cancels an in-flight transfer). This is the hook that lets the new
     * executor-free TransferManager.cancel still interrupt a blocking
     * `call.execute()`.
     */
    private suspend fun trackCall(id: String, call: Call) {
        activeCalls[id] = call
        val job = currentCoroutineContext()[Job] ?: return
        job.invokeOnCompletion {
            activeCalls.remove(id, call)
        }
    }

    override fun cancel(id: String) {
        activeCalls.remove(id)?.cancel()
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
        if (t is CancellationException) return true
        if (t is IOException && (t.message == "Canceled" || t.message == "Cancelled")) return true
        // Some providers wrap the cancellation message; fall back to a substring check.
        if (t is IOException && t.message?.contains("cancel", ignoreCase = true) == true) return true
        return false
    }
}
