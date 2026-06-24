package com.textvision.alistclient.auth

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.LoginRequest
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
        override suspend fun login(request: LoginRequest): AlistResponse<AlistLoginData> = response
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
