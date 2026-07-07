package com.textvision.alistclient.admin

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
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
import retrofit2.http.GET
import retrofit2.http.Url

@OptIn(ExperimentalCoroutinesApi::class)
class AdminRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: AdminRepository

    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    interface ProbeApi {
        @GET suspend fun probe(@Url url: String): AlistResponse<Unit>
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
        repo = AdminRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun unauthorizedTriggersRefreshAndRetriesSuccessfully() = runTest {
        var probeCount = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path?.contains("/api/auth/login") == true) {
                    return MockResponse().setResponseCode(200).setBody(
                        """{"code":200,"message":"success","data":{"token":"new_tok"}}"""
                    )
                }
                if (request.path?.contains("/api/admin/probe") == true) {
                    probeCount++
                    return if (probeCount == 1) {
                        MockResponse().setResponseCode(401).setBody("""{"code":401,"message":"x","data":null}""")
                    } else {
                        MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }
        val probeApi = retrofit2.Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build())
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProbeApi::class.java)
        val r = repo.runAdmin(server.url("/").toString()) { probeApi.probe("api/admin/probe") }
        assertTrue("expected Ok after refresh, got $r", r is AdminResult.Ok)
        assertEquals(2, probeCount)
    }
}
