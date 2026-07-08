package com.textvision.alistclient.admin.storage

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: StorageRepository

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
        val adminRepo = AdminRepository(api, session, AuthRepository(api, session), com.textvision.alistclient.auth.SessionEventBus(), UnconfinedTestDispatcher())
        repo = StorageRepository(api, adminRepo, UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun listParsesStorages() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"code":200,"message":"success","data":{"content":[{"id":1,"mount_path":"/local","driver":"Local","status":"work"}],"total":1}}"""
        ))
        val r = repo.list(server.url("/").toString())
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        val data = (r as com.textvision.alistclient.admin.AdminResult.Ok).data as StorageList
        assertEquals(1, data.content.size)
        assertEquals("/local", data.content[0].mountPath)
    }

    @Test fun updatePostsPatch() = runTest {
        var capturedPath: String? = null
        var capturedBody: String? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                capturedPath = request.path
                capturedBody = request.body.readUtf8()
                return MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
            }
        }
        val patch = StoragePatch(id = 1, mountPath = "/local", driver = "Local", disabled = true)
        val r = repo.update(server.url("/").toString(), patch)
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        assertTrue("path: $capturedPath", capturedPath!!.contains("/api/admin/storage/update"))
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"disabled\":true"))
    }

    @Test fun listDriversReturnsDriverInfo() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"code":200,"message":"success","data":{"Local":{"name":"Local","label":"本地存储","common":[],"additional":[]}}}"""
        ))
        val r = repo.listDrivers(server.url("/").toString())
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        val drivers = (r as com.textvision.alistclient.admin.AdminResult.Ok).data!!
        assertEquals(1, drivers.size)
        assertNotNull(drivers["Local"])
    }
}
