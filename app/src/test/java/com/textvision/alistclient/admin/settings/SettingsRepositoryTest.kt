package com.textvision.alistclient.admin.settings

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
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
class SettingsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: SettingsRepository

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
        val adminRepo = AdminRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
        repo = SettingsRepository(api, adminRepo, UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun listGroupsByKeyAndFiltersEmptyFormItems() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"code":200,"message":"success","data":{
              "content":[
                {"key":"site_title","value":"My Alist","group":"site",
                 "form_items":[{"name":"site_title","label":"站点标题","type":"string","required":true}]},
                {"key":"raw_token","value":"xyz","group":"aria2","form_items":null}
              ],
              "total":2
            }}
        """.trimIndent()))
        val r = repo.list(server.url("/").toString())
        assertTrue(r is AdminResult.Ok)
        val groups = (r as AdminResult.Ok).data!!
        assertEquals(1, groups.size)
        assertEquals("site", groups[0].key)
        assertEquals(1, groups[0].items.size)
        assertEquals("site_title", groups[0].items[0].key)
    }

    @Test fun savePostsPatchesAsItems() = runTest {
        var capturedBody: String? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                capturedBody = request.body.readUtf8()
                return MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
            }
        }
        val r = repo.save(server.url("/").toString(), listOf("site_title" to "New Title"))
        assertTrue(r is AdminResult.Ok)
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"key\":\"site_title\""))
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"value\":\"New Title\""))
    }
}
