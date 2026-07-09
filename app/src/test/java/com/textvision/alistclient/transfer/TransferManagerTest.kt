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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class TransferManagerTest {
    @Test
    fun transferManagerAcceptsInjectedExecutorAndScope() {
        // Compile-time check: TransferManager constructor accepts (TransferExecutor, CoroutineScope)
        // Runtime check: a no-op FakeTransferExecutor + TestScope boots without crashing
        val executor = FakeTransferExecutor()
        val scope = kotlinx.coroutines.test.TestScope()
        val manager = TransferManager(
            context = RuntimeEnvironment.getApplication(),
            dao = MemoryTransferDao(),
            executor = executor,
            scope = scope,
            notificationController = TransferNotificationController(RuntimeEnvironment.getApplication()),
        )
        // Smoke: scope is reachable; no crash
        assertEquals(executor, manager.executorForTest)
    }

    @Test
    fun downloadUsesRootDPathWhenSavedServerUrlContainsPathPrefix() {
        val client = CapturingOkHttpClient()
        val manager = manager(client, "http://example.com/alist/")

        manager.enqueueDownload("/folder/file.txt", "file.txt")
        client.awaitRequest()

        assertEquals("http://example.com/alist/d/folder/file.txt", client.request!!.url.toString())
    }

    @Test
    fun uploadUsesRootApiFsPutWhenSavedServerUrlContainsPathPrefix() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())

        val url = RealTransferExecutor::class.java.getDeclaredMethod("transferUrl", String::class.java).apply { isAccessible = true }
            .invoke(executor, "api/fs/put")

        assertEquals("http://example.com/alist/api/fs/put", url)
    }

    @Test
    fun downloadEncodesRawPathSegmentsWithoutDoubleEncoding() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())

        val url = RealTransferExecutor::class.java.getDeclaredMethod("transferUrl", String::class.java, String::class.java).apply { isAccessible = true }
            .invoke(executor, "d", "/space name/hash#name/percent%/a%2Fb.txt/雪.txt")

        assertEquals("http://example.com/alist/d/space%20name/hash%23name/percent%25/a%252Fb.txt/%E9%9B%AA.txt", url)
    }


    @Test
    fun uploadRequestAllowsAuthInterceptorToAttachToken() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())
        val uploadRequest = RealTransferExecutor::class.java.getDeclaredMethod("uploadRequest", String::class.java, okhttp3.RequestBody::class.java).apply { isAccessible = true }

        val request = uploadRequest.invoke(executor, "/target/file.txt", "ok".toRequestBody()) as Request

        assertEquals(null, request.header(SkipAuthRetry.HEADER))
        assertEquals(false, SkipAuthRetry.shouldSkip(request))
    }

    @Test fun localizeUploadFailureMapsStorageNotFoundToChinese() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())
        val localize = RealTransferExecutor::class.java.declaredMethods.first { it.name.startsWith("localizeUploadFailure") }.apply { isAccessible = true }

        val mapped = localize.invoke(executor, 500, "failed get storage: storage not found; please add a storage first") as String

        assertEquals("存储未挂载，请先在 Alist 后台挂载存储", mapped)
    }

    @Test fun localizeUploadFailureMapsUnauthorizedToLoginPrompt() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())
        val localize = RealTransferExecutor::class.java.declaredMethods.first { it.name.startsWith("localizeUploadFailure") }.apply { isAccessible = true }

        val mapped = localize.invoke(executor, 401, "token invalid") as String

        assertEquals("登录已失效，请重新登录", mapped)
    }

    @Test fun localizeDownloadFailureMapsUnknownHostToFriendlyMessage() {
        val executor = realExecutor(savedSessionManager("http://example.com/alist/"), CapturingOkHttpClient())
        val localize = RealTransferExecutor::class.java.declaredMethods.first { it.name.startsWith("localizeDownloadFailure") }.apply { isAccessible = true }

        val mapped = localize.invoke(executor, java.net.UnknownHostException("Unable to resolve host \"x.test\"")) as String

        assertEquals("无法解析服务器地址，请检查网络", mapped)
    }

    @Test
    fun markInterruptedOnStartupMarksActiveTasks() = kotlinx.coroutines.runBlocking {
        val dao = MemoryTransferDao()
        val now = System.currentTimeMillis()
        dao.upsert(TransferEntity("waiting", "a", "/a", null, null, 0, 0, TransferType.Download, TransferStatus.Waiting, null, now, now))
        dao.upsert(TransferEntity("failed", "b", "/b", null, null, 0, 0, TransferType.Download, TransferStatus.Failed, "x", now, now))
        val manager = manager(dao, CapturingOkHttpClient(), "http://example.com/")

        manager.markInterruptedOnStartup()

        assertEquals(TransferStatus.Interrupted, dao.find("waiting")!!.status)
        assertEquals(TransferStatus.Failed, dao.find("failed")!!.status)
    }

    @Test
    fun deleteRemovesCompletedTransferRecord() = runBlocking {
        val dao = MemoryTransferDao()
        val now = System.currentTimeMillis()
        val manager = manager(dao, CapturingOkHttpClient(), "http://example.com/")
        dao.upsert(TransferEntity("done", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now))

        manager.delete("done")
        dao.awaitMissing("done")

        assertEquals(null, dao.find("done"))
    }

    @Test
    fun deleteCancelsActiveTransferAndRemovesRecord() = runBlocking {
        val dao = MemoryTransferDao()
        val client = BlockingOkHttpClient()
        val manager = manager(dao, client, "http://example.com/")

        val id = manager.enqueueDownload("/folder/file.txt", "file.txt")
        client.awaitRequest()

        manager.delete(id)
        dao.awaitMissing(id)

        assertEquals(true, client.cancelled.get())
        assertEquals(null, dao.find(id))
    }

    @Test
    fun sanitizeUploadPathRejectsFileNamesInvalidInFileBrowser() {
        val executor = realExecutor(savedSessionManager("http://example.com/"), CapturingOkHttpClient())
        val sanitize = RealTransferExecutor::class.java.getDeclaredMethod("sanitizeUploadPath", String::class.java, String::class.java).apply { isAccessible = true }

        assertEquals(null, sanitize.invoke(executor, "/target", "."))
        assertEquals(null, sanitize.invoke(executor, "/target", ".."))
        assertEquals(null, sanitize.invoke(executor, "/target", "a/b.txt"))
        assertEquals(null, sanitize.invoke(executor, "/target", "a\\b.txt"))
        assertEquals(null, sanitize.invoke(executor, "/target", "ab.txt"))
        assertEquals(null, sanitize.invoke(executor, "/target", "a".repeat(256)))
        assertEquals("/target/good.txt", sanitize.invoke(executor, "/target", "good.txt"))
    }

    @Test
    fun sanitizeUploadPathEncodesNonAsciiSegmentsForFilePathHeader() {
        val executor = realExecutor(savedSessionManager("http://example.com/"), CapturingOkHttpClient())
        val sanitize = RealTransferExecutor::class.java.getDeclaredMethod("sanitizeUploadPath", String::class.java, String::class.java).apply { isAccessible = true }

        assertEquals("/%E6%88%91%E7%9A%84%E6%96%87%E4%BB%B6/alist-500mb-test.bin", sanitize.invoke(executor, "/我的文件", "alist-500mb-test.bin"))
    }

    @Test
    fun alistUploadResultTreatsNon200JsonCodeAsFailure() {
        val executor = realExecutor(savedSessionManager("http://example.com/"), CapturingOkHttpClient())
        val parser = RealTransferExecutor::class.java.getDeclaredMethod("toAlistUploadResult", String::class.java).apply { isAccessible = true }
        val result = parser.invoke(executor, """{"code":500,"message":"object not found","data":null}""")
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

    private fun realExecutor(sessionManager: SessionManager, client: OkHttpClient): RealTransferExecutor =
        RealTransferExecutor(RuntimeEnvironment.getApplication(), sessionManager, client)

    private fun manager(client: OkHttpClient, serverUrl: String): TransferManager =
        manager(MemoryTransferDao(), client, serverUrl)

    private fun manager(dao: TransferDao, client: OkHttpClient, serverUrl: String): TransferManager =
        TransferManager(
            context = RuntimeEnvironment.getApplication(),
            dao = dao,
            executor = realExecutor(savedSessionManager(serverUrl), client),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            notificationController = TransferNotificationController(RuntimeEnvironment.getApplication()),
        )
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
        override suspend fun deleteById(id: String) { entities.remove(id) }

        fun awaitMissing(id: String) {
            repeat(100) {
                if (!entities.containsKey(id)) return
                Thread.sleep(10)
            }
            throw AssertionError("Expected transfer $id to be deleted")
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

    private class BlockingOkHttpClient : OkHttpClient() {
        @Volatile private var requestSeen = false
        val cancelled = AtomicBoolean(false)

        fun awaitRequest() {
            repeat(100) {
                if (requestSeen) return
                Thread.sleep(10)
            }
            throw AssertionError("Expected request")
        }

        override fun newCall(request: Request): Call {
            requestSeen = true
            return object : Call {
                override fun request(): Request = request
                override fun execute(): Response {
                    while (!cancelled.get()) {
                        Thread.sleep(10)
                    }
                    throw IOException("Canceled")
                }
                override fun enqueue(responseCallback: okhttp3.Callback) = throw UnsupportedOperationException()
                override fun cancel() { cancelled.set(true) }
                override fun isExecuted(): Boolean = false
                override fun isCanceled(): Boolean = cancelled.get()
                override fun timeout(): okio.Timeout = okio.Timeout.NONE
                override fun clone(): Call = this
            }
        }
    }
}
