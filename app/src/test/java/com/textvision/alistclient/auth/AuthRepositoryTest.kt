package com.textvision.alistclient.auth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.CopyMovePathRequest
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    private class FakeApi(private val response: AlistResponse<AlistLoginData>) : AlistApi {
        override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> = response
        override suspend fun list(url: String, request: FsListRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsList> = throw UnsupportedOperationException("list is not used by this test")
        override suspend fun search(url: String, request: com.textvision.alistclient.network.dto.FsSearchRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsList> = throw UnsupportedOperationException("search is not used by this test")
        override suspend fun mkdir(url: String, request: com.textvision.alistclient.network.dto.MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException("mkdir is not used by this test")
        override suspend fun rename(url: String, request: com.textvision.alistclient.network.dto.RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException("rename is not used by this test")
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException("remove is not used by this test")
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("copy is not used by this test")
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("move is not used by this test")
    }

    private class CancellingApi : AlistApi {
        override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> {
            throw CancellationException("cancelled")
        }
        override suspend fun list(url: String, request: FsListRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsList> = throw UnsupportedOperationException("list is not used by this test")
        override suspend fun search(url: String, request: com.textvision.alistclient.network.dto.FsSearchRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsList> = throw UnsupportedOperationException("search is not used by this test")
        override suspend fun mkdir(url: String, request: com.textvision.alistclient.network.dto.MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException("mkdir is not used by this test")
        override suspend fun rename(url: String, request: com.textvision.alistclient.network.dto.RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException("rename is not used by this test")
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException("remove is not used by this test")
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("copy is not used by this test")
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("move is not used by this test")
    }

    @Test fun loginRethrowsCancellationException() = runTest {
        val repo = AuthRepository(CancellingApi(), SessionManager(MemoryStore(), AuthTokenProvider()))

        try {
            repo.login("http://server/", "admin", "pass")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
            return@runTest
        }

        throw AssertionError("Expected CancellationException")
    }

    @Test fun loginUsesSelectedServerUrlForApiRequest() = runTest {
        val defaultServer = MockWebServer()
        val selectedServer = MockWebServer()
        defaultServer.start()
        selectedServer.start()
        try {
            selectedServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":{"token":"tok"}}"""))
            val api = Retrofit.Builder()
                .baseUrl(defaultServer.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AlistApi::class.java)
            val repo = AuthRepository(api, SessionManager(MemoryStore(), AuthTokenProvider()))

            val result = repo.login(selectedServer.url("/").toString(), "admin", "pass")

            assertTrue(result is ApiResult.Success<*>)
            assertEquals("/api/auth/login", selectedServer.takeRequest().path)
            assertEquals(0, defaultServer.requestCount)
        } finally {
            defaultServer.shutdown()
            selectedServer.shutdown()
        }
    }

    @Test fun loginSkipsAuthHeaderInjectionAndDoesNotSendSkipMarkerHeader() = runTest {
        val defaultServer = MockWebServer()
        val selectedServer = MockWebServer()
        val tokenProvider = AuthTokenProvider().apply { setToken("old-token") }
        defaultServer.start()
        selectedServer.start()
        try {
            selectedServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":{"token":"new-token"}}"""))
            val api = Retrofit.Builder()
                .baseUrl(defaultServer.url("/"))
                .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build())
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AlistApi::class.java)
            val repo = AuthRepository(api, SessionManager(MemoryStore(), tokenProvider))

            val result = repo.login(selectedServer.url("/").toString(), "admin", "pass")

            assertTrue(result is ApiResult.Success<*>)
            val loginRequest = selectedServer.takeRequest()
            assertEquals("/api/auth/login", loginRequest.path)
            assertEquals(null, loginRequest.getHeader("Authorization"))
            assertEquals(null, loginRequest.getHeader(SkipAuthRetry.HEADER))
        } finally {
            defaultServer.shutdown()
            selectedServer.shutdown()
        }
    }

    @Test fun loginSuccessPersistsSessionAndToken() = runTest {
        val store = MemoryStore()
        val tokenProvider = AuthTokenProvider()
        val manager = SessionManager(store, tokenProvider)
        val repo = AuthRepository(FakeApi(AlistResponse(200, "success", AlistLoginData("tok"))), manager)

        val result = repo.login("http://server/", "admin", "pass")

        assertTrue(result is ApiResult.Success<*>)
        assertEquals("tok", store.map[SessionManager.KEY_TOKEN])
        assertEquals("pass", store.map[SessionManager.KEY_PASSWORD])
        assertEquals("tok", tokenProvider.getToken())
    }

    @Test fun loginBusinessFailureDoesNotPersistPassword() = runTest {
        val store = MemoryStore()
        val manager = SessionManager(store, AuthTokenProvider())
        val repo = AuthRepository(FakeApi(AlistResponse(401, "unauthorized", null)), manager)

        val result = repo.login("http://server/", "admin", "bad")

        assertTrue(result is ApiResult.Failure)
        assertTrue(store.map.isEmpty())
    }
}