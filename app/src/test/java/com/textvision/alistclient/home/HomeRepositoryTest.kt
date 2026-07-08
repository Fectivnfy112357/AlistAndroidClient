package com.textvision.alistclient.home

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: HomeRepository

    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store = MemoryStore().apply {
            map[SessionManager.KEY_SERVER_URL] = server.url("/").toString()
            map[SessionManager.KEY_USERNAME] = "admin"
            map[SessionManager.KEY_PASSWORD] = "pass"
            map[SessionManager.KEY_TOKEN] = "tok"
        }
        tokenProvider = AuthTokenProvider()
        session = SessionManager(store, tokenProvider)
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build()
        api = retrofit2.Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AlistApi::class.java)
        val authRepo = AuthRepository(api, session)
        val adminRepo = AdminRepository(api, session, authRepo, UnconfinedTestDispatcher())
        repo = HomeRepository(api, session, authRepo, adminRepo, UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun publicOk() = """{"code":200,"message":"success","data":{"title":"My Alist","version":"v3.61.0"}}"""
    private fun storageOk() = """{"code":200,"message":"success","data":{"content":[{"mount_path":"/local","driver":"Local","status":"work"}],"total":1}}"""
    private fun userOk() = """{"code":200,"message":"success","data":{"content":[{"id":1,"username":"admin","disabled":false}],"total":1}}"""
    private fun roleOk() = """{"code":200,"message":"success","data":{"content":[{"id":2,"name":"admin"}],"total":1}}"""
    private fun sessionOk() = """{"code":200,"message":"success","data":[{"session_id":"s1","user_id":1,"last_active":1,"status":0,"ua":"u","ip":"1.1.1.1"}]}"""
    private fun taskOk() = """{"code":200,"message":"success","data":[]}"""

    /** Path-based dispatcher so concurrent admin fetches receive correct payloads. */
    private fun installPathDispatcher() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when {
                    request.path?.contains("/api/public/settings") == true -> publicOk()
                    request.path?.contains("/api/admin/storage/list") == true -> storageOk()
                    request.path?.contains("/api/admin/user/list") == true -> userOk()
                    request.path?.contains("/api/admin/role/list") == true -> roleOk()
                    request.path?.contains("/api/admin/session/list") == true -> sessionOk()
                    request.path?.matches(".*/api/admin/task/.*/undone.*".toRegex()) == true -> taskOk()
                    else -> """{"code":200,"message":"success","data":null}"""
                }
                return MockResponse().setResponseCode(200).setBody(body)
            }
        }
    }

    @Test fun allAdminOkReturnsAllSections() = runTest {
        installPathDispatcher()

        val r = repo.loadDashboard() as ApiResult.Success
        val d = r.data
        assertTrue(d.publicSection is SectionResult.Ok)
        assertTrue(d.storageSection is SectionResult.Ok)
        assertTrue(d.serverStatsSection is SectionResult.Ok)
        assertTrue(d.sessionSection is SectionResult.Ok)
        assertTrue(d.taskSection is SectionResult.Ok)
    }

    @Test fun admin401LeavesSectionsFailedAndPublicOk() = runTest {
        // Path-based dispatcher returning 401 for admin endpoints. The Alist body may parse
        // as Network (deserialization edge case) depending on endpoint DTO nullability, but
        // every admin section must end up Failed regardless of cause.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path?.contains("/api/public/settings") == true) {
                    MockResponse().setResponseCode(200).setBody(publicOk())
                } else {
                    MockResponse().setResponseCode(401).setBody("""{"code":401,"message":"x","data":null}""")
                }
            }
        }

        val r = repo.loadDashboard() as ApiResult.Success
        val d = r.data
        assertTrue(d.publicSection is SectionResult.Ok)
        assertTrue(d.storageSection is SectionResult.Failed)
        assertTrue(d.serverStatsSection is SectionResult.Failed)
        assertTrue(d.sessionSection is SectionResult.Failed)
        // Task section surfaces per-bucket Network failures but overall the data set is present;
        // assert the explicit Failed indicator via the bucket list being empty (failed-bucket ids
        // populated means the section is reporting failures consistently).
        val taskSection = d.taskSection
        if (taskSection is SectionResult.Ok) {
            // Conforming to the contract: failed bucket ids must reflect all 7 types.
            assertEquals(7, taskSection.data.failedBucketIds.size)
        } else {
            assertTrue(taskSection is SectionResult.Failed)
        }
    }

    @Test fun publicFailureReturnsApiResultFailure() = runTest {
        enqueue("""{"code":500,"message":"oops","data":null}""")
        val r = repo.loadDashboard()
        assertTrue(r is ApiResult.Failure)
        assertEquals(500, (r as ApiResult.Failure).code)
    }

    @Test fun retrySectionRefetchesOnlyThatSection() = runTest {
        // First load: public ok, storage ok, user ok, role ok, session ok, 7×task ok
        enqueue(publicOk()); enqueue(storageOk()); enqueue(userOk()); enqueue(roleOk()); enqueue(sessionOk())
        repeat(7) { enqueue(taskOk()) }
        val r1 = repo.loadDashboard() as ApiResult.Success
        val countAfterFirst = server.requestCount

        // retry storage
        enqueue("""{"code":200,"message":"success","data":{"content":[{"mount_path":"/new","driver":"Local","status":"work"}],"total":1}}""")
        val r2 = repo.retrySection(r1.data, SectionKey.Storage)
        val storage = r2.storageSection as SectionResult.Ok
        assertEquals("/new", storage.data.storages.first().mountPath)
        assertEquals(countAfterFirst + 1, server.requestCount)
        // public unchanged
        assertEquals((r1.data.publicSection as SectionResult.Ok).data.siteTitle,
            (r2.publicSection as SectionResult.Ok).data.siteTitle)
    }
}