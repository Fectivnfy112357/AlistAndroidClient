package com.textvision.alistclient.home

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    @OptIn(ExperimentalCoroutinesApi::class)
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
        repo = HomeRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test fun adminSuccessReturnsAdminData() = runTest {
        enqueue("""{"code":200,"message":"success","data":{"version":"v3.25.0","start_time":"2026-07-01T00:00:00Z","used_bytes":100,"total_bytes":200}}""")
        enqueue("""{"code":200,"message":"success","data":{"content":[{"mount_path":"/local","driver":"Local","used_bytes":50,"total_bytes":100}],"total":1}}""")

        val result = repo.loadDashboard() as ApiResult.Success

        val data = result.data as HomeData.Admin
        assertEquals(false, data.isGuest)
        assertEquals(1, data.storages.size)
        assertEquals("/local", data.storages.first().mountPath)
        // public endpoint should NOT have been hit
        assertEquals(2, server.requestCount)
    }

    @Test fun admin401FallsBackToPublicAndMarksGuest() = runTest {
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        enqueue("""{"code":401,"message":"unauthorized","data":null}""") // login fails
        enqueue("""{"code":200,"message":"success","data":{"title":"My Alist","version":"v3.25.0"}}""")

        val result = repo.loadDashboard() as ApiResult.Success
        val data = result.data as HomeData.Guest

        assertEquals(true, data.isGuest)
        assertEquals("My Alist", data.serverTitle)
        assertEquals("v3.25.0", data.serverVersion)
        assertEquals(4, server.requestCount)
    }

    @Test fun adminNetworkErrorFallsBackToPublic() = runTest {
        server.shutdown() // public will be called on a fresh server below
        // emulate the public fallback path: open a new server for public
        val publicServer = MockWebServer().apply { start() }
        try {
            // point the stored URL at the new server
            store.map[SessionManager.KEY_SERVER_URL] = publicServer.url("/").toString()
            publicServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) // admin fails with network error
            // Re-enqueue the same socket error to the other admin endpoint
            publicServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            publicServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":{"title":"Public","version":"v3"}}"""))

            val result = repo.loadDashboard() as ApiResult.Success
            assertTrue(result.data is HomeData.Guest)
        } finally { publicServer.shutdown() }
    }

    @Test fun adminAndPublicAllFailReturnsFailure() = runTest {
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""") // login fails
        enqueue("""{"code":500,"message":"oops","data":null}""")

        val result = repo.loadDashboard()

        assertTrue(result is ApiResult.Failure)
        assertEquals(500, (result as ApiResult.Failure).code)
    }

    @Test fun refreshesTokenAndRetriesAdminOn401() = runTest {
        // First admin call returns 401
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        // Login with new token
        enqueue("""{"code":200,"message":"success","data":{"token":"new_tok"}}""")
        // Retry admin calls now succeed
        enqueue("""{"code":200,"message":"success","data":{"version":"v3.26.0","start_time":"2026-07-02T00:00:00Z","used_bytes":200,"total_bytes":400}}""")
        enqueue("""{"code":200,"message":"success","data":{"content":[{"mount_path":"/local2","driver":"Local","used_bytes":100,"total_bytes":200}],"total":1}}""")

        val result = repo.loadDashboard() as ApiResult.Success
        val data = result.data as HomeData.Admin

        assertEquals(false, data.isGuest)
        assertEquals(1, data.storages.size)
        assertEquals("/local2", data.storages.first().mountPath)
        // Should have made: 2 admin 401 + 1 login + 2 admin 200 = 5 requests
        assertEquals(5, server.requestCount)
        // Token should be refreshed
        assertEquals("new_tok", tokenProvider.getToken())
    }
}
