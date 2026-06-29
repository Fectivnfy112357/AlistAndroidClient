package com.textvision.alistclient.transfer

import org.robolectric.RuntimeEnvironment
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class TransferManagerTest {
    @Test
    fun downloadUsesRootDPathWhenSavedServerUrlContainsPathPrefix() {
        val client = CapturingOkHttpClient()
        val sessionManager = savedSessionManager("http://example.com/alist/")
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), client, sessionManager)

        manager.enqueueDownload("/folder/file.txt", "file.txt")
        client.awaitRequest()

        assertEquals("http://example.com/alist/d/folder/file.txt", client.request!!.url.toString())
    }

    @Test
    fun uploadUsesRootApiFsPutWhenSavedServerUrlContainsPathPrefix() {
        val client = CapturingOkHttpClient()
        val sessionManager = savedSessionManager("http://example.com/alist/")
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), client, sessionManager)

        val url = TransferManager::class.java.getDeclaredMethod("transferUrl", String::class.java).apply { isAccessible = true }
            .invoke(manager, "api/fs/put")

        assertEquals("http://example.com/alist/api/fs/put", url)
    }

    @Test
    fun downloadEncodesRawPathSegmentsWithoutDoubleEncoding() {
        val client = CapturingOkHttpClient()
        val sessionManager = savedSessionManager("http://example.com/alist/")
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), client, sessionManager)

        val url = TransferManager::class.java.getDeclaredMethod("transferUrl", String::class.java, String::class.java).apply { isAccessible = true }
            .invoke(manager, "d", "/space name/hash#name/percent%/a%2Fb.txt/雪.txt")

        assertEquals("http://example.com/alist/d/space%20name/hash%23name/percent%25/a%252Fb.txt/%E9%9B%AA.txt", url)
    }


    @Test
    fun uploadRequestAllowsAuthInterceptorToAttachToken() {
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), CapturingOkHttpClient(), savedSessionManager("http://example.com/alist/"))
        val uploadRequest = TransferManager::class.java.getDeclaredMethod("uploadRequest", String::class.java, okhttp3.RequestBody::class.java).apply { isAccessible = true }

        val request = uploadRequest.invoke(manager, "/target/file.txt", "ok".toRequestBody()) as Request

        assertEquals(null, request.header(SkipAuthRetry.HEADER))
        assertEquals(false, SkipAuthRetry.shouldSkip(request))
    }

    @Test
    fun markInterruptedOnStartupMarksActiveTasks() = kotlinx.coroutines.runBlocking {
        val dao = MemoryTransferDao()
        val now = System.currentTimeMillis()
        dao.upsert(TransferEntity("waiting", "a", "/a", null, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
        dao.upsert(TransferEntity("failed", "b", "/b", null, null, 0, 0, TransferType.Download, TransferStatus.Failed, "x", now, now))
        val manager = TransferManager(RuntimeEnvironment.getApplication(), dao, CapturingOkHttpClient(), savedSessionManager("http://example.com/"))

        manager.markInterruptedOnStartup()

        assertEquals(TransferStatus.Interrupted, dao.find("waiting")!!.status)
        assertEquals(TransferStatus.Failed, dao.find("failed")!!.status)
    }

    @Test
    fun sanitizeUploadPathRejectsFileNamesInvalidInFileBrowser() {
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), CapturingOkHttpClient(), savedSessionManager("http://example.com/"))
        val sanitize = TransferManager::class.java.getDeclaredMethod("sanitizeUploadPath", String::class.java, String::class.java).apply { isAccessible = true }

        assertEquals(null, sanitize.invoke(manager, "/target", "."))
        assertEquals(null, sanitize.invoke(manager, "/target", ".."))
        assertEquals(null, sanitize.invoke(manager, "/target", "a/b.txt"))
        assertEquals(null, sanitize.invoke(manager, "/target", "a\b.txt"))
        assertEquals(null, sanitize.invoke(manager, "/target", "ab.txt"))
        assertEquals(null, sanitize.invoke(manager, "/target", "a".repeat(256)))
        assertEquals("/target/good.txt", sanitize.invoke(manager, "/target", "good.txt"))
    }

    @Test
    fun sanitizeUploadPathEncodesNonAsciiSegmentsForFilePathHeader() {
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), CapturingOkHttpClient(), savedSessionManager("http://example.com/"))
        val sanitize = TransferManager::class.java.getDeclaredMethod("sanitizeUploadPath", String::class.java, String::class.java).apply { isAccessible = true }

        assertEquals("/%E6%88%91%E7%9A%84%E6%96%87%E4%BB%B6/alist-500mb-test.bin", sanitize.invoke(manager, "/我的文件", "alist-500mb-test.bin"))
    }

    @Test
    fun alistUploadResultTreatsNon200JsonCodeAsFailure() {
        val manager = TransferManager(RuntimeEnvironment.getApplication(), MemoryTransferDao(), CapturingOkHttpClient(), savedSessionManager("http://example.com/"))
        val parser = TransferManager::class.java.getDeclaredMethod("toAlistUploadResult", String::class.java).apply { isAccessible = true }
        val result = parser.invoke(manager, """{"code":500,"message":"object not found","data":null}""")
        val isSuccess = result!!::class.java.getDeclaredMethod("isSuccess").apply { isAccessible = true }
        val message = result::class.java.getDeclaredField("message").apply { isAccessible = true }

        assertEquals(false, isSuccess.invoke(result))
        assertEquals("object not found", message.get(result))
    }

    private fun savedSessionManager(serverUrl: String): SessionManager {
        val store = MemoryStore()
        val manager = SessionManager(store, AuthTokenProvider())
        manager.saveSession(SavedSession(serverUrl, "user", "pass", "token"))
        return manager
    }

    private class MemoryStore : CredentialStore {
        private val map = mutableMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    private class MemoryTransferDao : TransferDao {
        private val entities = mutableMapOf<String, TransferEntity>()
        override fun observeAll(): Flow<List<TransferEntity>> = flowOf(entities.values.toList())
        override suspend fun find(id: String): TransferEntity? = entities[id]
        override suspend fun upsert(entity: TransferEntity) { entities[entity.id] = entity }
        override suspend fun updateStatus(id: String, status: TransferStatus, reason: String?, updatedAtMillis: Long) {
            entities[id]?.let { entities[id] = it.copy(status = status, failureReason = reason, updatedAtMillis = updatedAtMillis) }
        }
        override suspend fun cancelActiveTask(id: String, reason: String?, updatedAtMillis: Long): Int {
            val current = entities[id] ?: return 0
            if (current.status !in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) return 0
            entities[id] = current.copy(status = TransferStatus.Cancelled, failureReason = reason, updatedAtMillis = updatedAtMillis)
            return 1
        }
        override suspend fun updateProgress(id: String, bytesDone: Long, totalBytes: Long, updatedAtMillis: Long) {
            entities[id]?.let { entities[id] = it.copy(bytesDone = bytesDone, totalBytes = totalBytes, updatedAtMillis = updatedAtMillis) }
        }
        override suspend fun markActiveTasksInterrupted(updatedAtMillis: Long) {
            entities.replaceAll { _, entity ->
                if (entity.status in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) {
                    entity.copy(status = TransferStatus.Interrupted, failureReason = "传输中断", updatedAtMillis = updatedAtMillis)
                } else {
                    entity
                }
            }
        }
        override suspend fun deleteAll() { entities.clear() }
    }

    private class CapturingOkHttpClient(
        private val responseBody: String = "ok",
    ) : OkHttpClient() {
        @Volatile var request: Request? = null
        fun awaitRequest(): Request {
            repeat(100) {
                request?.let { return it }
                Thread.sleep(10)
            }
            throw AssertionError("Expected request")
        }
        override fun newCall(request: Request): Call {
            this.request = request
            return object : Call {
                override fun request(): Request = request
                override fun execute(): Response = Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody())
                    .build()
                override fun enqueue(responseCallback: okhttp3.Callback) = throw UnsupportedOperationException()
                override fun cancel() = Unit
                override fun isExecuted(): Boolean = false
                override fun isCanceled(): Boolean = false
                override fun timeout(): okio.Timeout = okio.Timeout.NONE
                override fun clone(): Call = this
            }
        }
    }
}
