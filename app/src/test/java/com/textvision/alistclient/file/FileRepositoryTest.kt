package com.textvision.alistclient.file

import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistFileDto
import com.textvision.alistclient.network.dto.AlistFsList
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.CopyMovePathRequest
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRepositoryTest {
    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    private class RefreshingApi : AlistApi {
        val listRequests = mutableListOf<Pair<String, FsListRequest>>()
        val loginRequests = mutableListOf<Triple<String, String, LoginRequest>>()

        override suspend fun list(url: String, request: FsListRequest): AlistResponse<AlistFsList> {
            listRequests += url to request
            return if (listRequests.size == 1) {
                AlistResponse(401, "token expired", null)
            } else {
                AlistResponse(200, "success", AlistFsList(content = listOf(AlistFileDto(name = "ok.txt"))))
            }
        }

        override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> {
            loginRequests += Triple(url, skipAuthRetry, request)
            return AlistResponse(200, "success", AlistLoginData("new-token"))
        }

        override suspend fun search(url: String, request: FsSearchRequest): AlistResponse<AlistFsList> = throw UnsupportedOperationException("search is not used by this test")
        override suspend fun mkdir(url: String, request: MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException("mkdir is not used by this test")
        override suspend fun rename(url: String, request: RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException("rename is not used by this test")
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException("remove is not used by this test")
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("copy is not used by this test")
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("move is not used by this test")
        override suspend fun listStorage(url: String, page: Int, perPage: Int): AlistResponse<com.textvision.alistclient.network.dto.StorageList> = throw UnsupportedOperationException("listStorage is not used by this test")
        override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException("getPublicSettings is not used by this test")
    }

    @Test fun listRefreshesExpiredTokenAndRetriesOnce() = runTest {
        val api = RefreshingApi()
        val store = MemoryStore().apply {
            map[SessionManager.KEY_SERVER_URL] = "http://server/"
            map[SessionManager.KEY_USERNAME] = "admin"
            map[SessionManager.KEY_PASSWORD] = "pass"
            map[SessionManager.KEY_TOKEN] = "old-token"
        }
        val tokenProvider = AuthTokenProvider()
        val repository = FileRepository(api, SessionManager(store, tokenProvider))

        val result = repository.list("/")

        assertTrue(result is ApiResult.Success<*>)
        assertEquals(listOf("ok.txt"), (result as ApiResult.Success).data.map { it.name })
        assertEquals(2, api.listRequests.size)
        assertEquals(1, api.loginRequests.size)
        assertEquals("http://server/api/auth/login", api.loginRequests.single().first)
        assertEquals(SkipAuthRetry.HEADER, api.loginRequests.single().second)
        assertEquals(LoginRequest("admin", "pass"), api.loginRequests.single().third)
        assertEquals("new-token", store.map[SessionManager.KEY_TOKEN])
        assertEquals("new-token", tokenProvider.getToken())
    }
}
