package com.textvision.alistclient.file

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionEvent
import com.textvision.alistclient.auth.SessionEventBus
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
import com.textvision.alistclient.network.dto.RoleList
import com.textvision.alistclient.network.dto.SessionInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.TaskInfo
import com.textvision.alistclient.network.dto.UserList
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        override suspend fun fsGet(url: String, request: com.textvision.alistclient.network.dto.FsGetRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsGetData> = throw UnsupportedOperationException("fsGet is not used by this test")
        override suspend fun mkdir(url: String, request: MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException("mkdir is not used by this test")
        override suspend fun rename(url: String, request: RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException("rename is not used by this test")
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException("remove is not used by this test")
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("copy is not used by this test")
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException("move is not used by this test")
        override suspend fun listStorage(url: String, page: Int, perPage: Int): AlistResponse<StorageList> = throw UnsupportedOperationException("listStorage is not used by this test")
        override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException("getPublicSettings is not used by this test")
        override suspend fun listUsers(url: String, page: Int, perPage: Int): AlistResponse<UserList> = throw UnsupportedOperationException("listUsers is not used by this test")
        override suspend fun listRoles(url: String, page: Int, perPage: Int): AlistResponse<RoleList> = throw UnsupportedOperationException("listRoles is not used by this test")
        override suspend fun listSessions(url: String): AlistResponse<List<SessionInfo>> = throw UnsupportedOperationException("listSessions is not used by this test")
        override suspend fun taskUndone(url: String): AlistResponse<List<TaskInfo>> = throw UnsupportedOperationException("taskUndone is not used by this test")
        override suspend fun updateStorage(url: String, body: com.textvision.alistclient.network.dto.StoragePatch): AlistResponse<Unit> = throw UnsupportedOperationException("updateStorage is not used by this test")
        override suspend fun enableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException("enableStorage is not used by this test")
        override suspend fun disableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException("disableStorage is not used by this test")
        override suspend fun listDrivers(url: String, page: Int, perPage: Int): AlistResponse<Map<String, com.textvision.alistclient.network.dto.DriverInfo>> = throw UnsupportedOperationException("listDrivers is not used by this test")
        override suspend fun listSettings(url: String, page: Int, perPage: Int): AlistResponse<List<com.textvision.alistclient.network.dto.SettingItem>> = throw UnsupportedOperationException("listSettings is not used by this test")
        override suspend fun saveSettings(url: String, body: com.textvision.alistclient.network.dto.SettingSaveRequest): AlistResponse<Unit> = throw UnsupportedOperationException("saveSettings is not used by this test")
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
        val adminRepo: AdminRepository = mockk(relaxed = true)
        coEvery { adminRepo.runAdmin<StorageList>(any(), any()) } returns com.textvision.alistclient.admin.AdminResult.Ok(null)
        val bus = mockk<SessionEventBus>(relaxed = true)
        val repository = FileRepository(api, SessionManager(store, tokenProvider), adminRepo, bus)

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
        verify(exactly = 0) { bus.emit(any()) }
    }

    private class ThreeItemApi : AlistApi {
        override suspend fun list(url: String, request: FsListRequest): AlistResponse<AlistFsList> =
            AlistResponse(200, "success", AlistFsList(content = listOf(
                AlistFileDto(name = "我的文件"),
                AlistFileDto(name = "我的百度网盘"),
                AlistFileDto(name = "我的照片"),
            )))
        override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> = throw UnsupportedOperationException()
        override suspend fun search(url: String, request: FsSearchRequest): AlistResponse<AlistFsList> = throw UnsupportedOperationException()
        override suspend fun fsGet(url: String, request: com.textvision.alistclient.network.dto.FsGetRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsGetData> = throw UnsupportedOperationException()
        override suspend fun mkdir(url: String, request: MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun rename(url: String, request: RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun listStorage(url: String, page: Int, perPage: Int): AlistResponse<StorageList> = throw UnsupportedOperationException()
        override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException()
        override suspend fun listUsers(url: String, page: Int, perPage: Int): AlistResponse<UserList> = throw UnsupportedOperationException()
        override suspend fun listRoles(url: String, page: Int, perPage: Int): AlistResponse<RoleList> = throw UnsupportedOperationException()
        override suspend fun listSessions(url: String): AlistResponse<List<SessionInfo>> = throw UnsupportedOperationException()
        override suspend fun taskUndone(url: String): AlistResponse<List<TaskInfo>> = throw UnsupportedOperationException()
        override suspend fun updateStorage(url: String, body: com.textvision.alistclient.network.dto.StoragePatch): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun enableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun disableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun listDrivers(url: String, page: Int, perPage: Int): AlistResponse<Map<String, com.textvision.alistclient.network.dto.DriverInfo>> = throw UnsupportedOperationException()
        override suspend fun listSettings(url: String, page: Int, perPage: Int): AlistResponse<List<com.textvision.alistclient.network.dto.SettingItem>> = throw UnsupportedOperationException()
        override suspend fun saveSettings(url: String, body: com.textvision.alistclient.network.dto.SettingSaveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
    }

    @Test fun listFiltersDisabledMountPaths() = runTest {
        val api = ThreeItemApi()
        val store = MemoryStore().apply {
            map[SessionManager.KEY_SERVER_URL] = "http://server/"
            map[SessionManager.KEY_USERNAME] = "u"
            map[SessionManager.KEY_PASSWORD] = "p"
            map[SessionManager.KEY_TOKEN] = "t"
        }
        val adminRepo: AdminRepository = mockk(relaxed = true)
        coEvery { adminRepo.runAdmin<StorageList>(any(), any()) } returns com.textvision.alistclient.admin.AdminResult.Ok(
            StorageList(content = listOf(
                com.textvision.alistclient.network.dto.StorageInfo(id = 1, mountPath = "/我的文件", driver = "Local", disabled = false),
                com.textvision.alistclient.network.dto.StorageInfo(id = 2, mountPath = "/我的百度网盘", driver = "BaiduNetdisk", disabled = true),
                com.textvision.alistclient.network.dto.StorageInfo(id = 3, mountPath = "/我的照片", driver = "Local", disabled = false),
            ))
        )
        val repository = FileRepository(api, SessionManager(store, AuthTokenProvider()), adminRepo, mockk(relaxed = true))
        val result = repository.list("/")
        assertTrue(result is ApiResult.Success<*>)
        val names = (result as ApiResult.Success).data.map { it.name }
        assertEquals(listOf("我的文件", "我的照片"), names)
    }

    private class AlwaysUnauthorizedApi(private val loginCode: Int) : AlistApi {
        override suspend fun list(url: String, request: FsListRequest): AlistResponse<AlistFsList> =
            AlistResponse(401, "token expired", null)
        override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> =
            if (loginCode == 200) AlistResponse(200, "success", AlistLoginData("new-token"))
            else AlistResponse(loginCode, "unauthorized", null)
        override suspend fun search(url: String, request: FsSearchRequest): AlistResponse<AlistFsList> = throw UnsupportedOperationException()
        override suspend fun fsGet(url: String, request: com.textvision.alistclient.network.dto.FsGetRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsGetData> = throw UnsupportedOperationException()
        override suspend fun mkdir(url: String, request: MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun rename(url: String, request: RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun listStorage(url: String, page: Int, perPage: Int): AlistResponse<StorageList> = throw UnsupportedOperationException()
        override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException()
        override suspend fun listUsers(url: String, page: Int, perPage: Int): AlistResponse<UserList> = throw UnsupportedOperationException()
        override suspend fun listRoles(url: String, page: Int, perPage: Int): AlistResponse<RoleList> = throw UnsupportedOperationException()
        override suspend fun listSessions(url: String): AlistResponse<List<SessionInfo>> = throw UnsupportedOperationException()
        override suspend fun taskUndone(url: String): AlistResponse<List<TaskInfo>> = throw UnsupportedOperationException()
        override suspend fun updateStorage(url: String, body: com.textvision.alistclient.network.dto.StoragePatch): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun enableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun disableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun listDrivers(url: String, page: Int, perPage: Int): AlistResponse<Map<String, com.textvision.alistclient.network.dto.DriverInfo>> = throw UnsupportedOperationException()
        override suspend fun listSettings(url: String, page: Int, perPage: Int): AlistResponse<List<com.textvision.alistclient.network.dto.SettingItem>> = throw UnsupportedOperationException()
        override suspend fun saveSettings(url: String, body: com.textvision.alistclient.network.dto.SettingSaveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
    }

    private fun memoryStoreWithSession() = MemoryStore().apply {
        map[SessionManager.KEY_SERVER_URL] = "http://server/"
        map[SessionManager.KEY_USERNAME] = "admin"
        map[SessionManager.KEY_PASSWORD] = "pass"
        map[SessionManager.KEY_TOKEN] = "old-token"
    }

    @Test fun emitsUnauthorizedOnceWhenRetryStill401AfterRefresh() = runTest {
        val api = AlwaysUnauthorizedApi(loginCode = 200)
        val adminRepo: AdminRepository = mockk(relaxed = true)
        coEvery { adminRepo.runAdmin<StorageList>(any(), any()) } returns com.textvision.alistclient.admin.AdminResult.Ok(null)
        val bus = mockk<SessionEventBus>(relaxed = true)
        val repository = FileRepository(api, SessionManager(memoryStoreWithSession(), AuthTokenProvider()), adminRepo, bus)

        val result = repository.list("/")

        assertTrue(result is ApiResult.Failure)
        assertEquals(401, (result as ApiResult.Failure).code)
        verify(exactly = 1) { bus.emit(SessionEvent.Unauthorized) }
    }

    @Test fun emitsUnauthorizedOnceWhenRefreshItselfFails() = runTest {
        val api = AlwaysUnauthorizedApi(loginCode = 401)
        val adminRepo: AdminRepository = mockk(relaxed = true)
        coEvery { adminRepo.runAdmin<StorageList>(any(), any()) } returns com.textvision.alistclient.admin.AdminResult.Ok(null)
        val bus = mockk<SessionEventBus>(relaxed = true)
        val repository = FileRepository(api, SessionManager(memoryStoreWithSession(), AuthTokenProvider()), adminRepo, bus)

        val result = repository.list("/")

        assertTrue(result is ApiResult.Failure)
        verify(exactly = 1) { bus.emit(SessionEvent.Unauthorized) }
    }

    @Test fun nonAdminListDoesNotEmitWhenAdminProbeStillUnauthorized() = runTest {
        // Simulates non-admin user browsing files: fs/list succeeds, but listStorage (called via
        // disabledMountPaths) returns Unauthorized. The previous bug was that runAdmin emitted
        // SessionEvent.Unauthorized which kicked the user back to the login page.
        val api = object : AlistApi {
            override suspend fun list(url: String, request: FsListRequest): AlistResponse<AlistFsList> =
                AlistResponse(200, "success", AlistFsList(content = listOf(AlistFileDto(name = "a.txt"))))
            override suspend fun listStorage(url: String, page: Int, perPage: Int): AlistResponse<StorageList> =
                AlistResponse(403, "forbidden", null)
            override suspend fun login(url: String, skipAuthRetry: String, request: LoginRequest): AlistResponse<AlistLoginData> = throw UnsupportedOperationException()
            override suspend fun search(url: String, request: FsSearchRequest): AlistResponse<AlistFsList> = throw UnsupportedOperationException()
            override suspend fun fsGet(url: String, request: com.textvision.alistclient.network.dto.FsGetRequest): AlistResponse<com.textvision.alistclient.network.dto.AlistFsGetData> = throw UnsupportedOperationException()
            override suspend fun mkdir(url: String, request: MkdirRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun rename(url: String, request: RenameRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun remove(url: String, request: RemoveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun copy(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun move(url: String, request: CopyMovePathRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException()
            override suspend fun listUsers(url: String, page: Int, perPage: Int): AlistResponse<UserList> = throw UnsupportedOperationException()
            override suspend fun listRoles(url: String, page: Int, perPage: Int): AlistResponse<RoleList> = throw UnsupportedOperationException()
            override suspend fun listSessions(url: String): AlistResponse<List<SessionInfo>> = throw UnsupportedOperationException()
            override suspend fun taskUndone(url: String): AlistResponse<List<TaskInfo>> = throw UnsupportedOperationException()
            override suspend fun updateStorage(url: String, body: com.textvision.alistclient.network.dto.StoragePatch): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun enableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
        override suspend fun disableStorage(url: String, id: Long): AlistResponse<Unit> = throw UnsupportedOperationException()
            override suspend fun listDrivers(url: String, page: Int, perPage: Int): AlistResponse<Map<String, com.textvision.alistclient.network.dto.DriverInfo>> = throw UnsupportedOperationException()
            override suspend fun listSettings(url: String, page: Int, perPage: Int): AlistResponse<List<com.textvision.alistclient.network.dto.SettingItem>> = throw UnsupportedOperationException()
            override suspend fun saveSettings(url: String, body: com.textvision.alistclient.network.dto.SettingSaveRequest): AlistResponse<Unit> = throw UnsupportedOperationException()
        }
        val store = MemoryStore().apply {
            map[SessionManager.KEY_SERVER_URL] = "http://server/"
            map[SessionManager.KEY_USERNAME] = "user"
            map[SessionManager.KEY_PASSWORD] = "pass"
            map[SessionManager.KEY_TOKEN] = "tok"
        }
        val tokenProvider = AuthTokenProvider()
        // Use the real AdminRepository so the runAdmin path (refreshAndRetry + safeCall) is exercised.
        val adminApi = mockk<AlistApi>(relaxed = true)
        coEvery { adminApi.login(any(), any(), any()) } returns AlistResponse(200, "success", AlistLoginData("new-tok"))
        coEvery { adminApi.listStorage(any(), any(), any()) } returns AlistResponse(403, "forbidden", null)
        val bus = mockk<SessionEventBus>(relaxed = true)
        val sharedSession = SessionManager(store, tokenProvider)
        val adminRepo = AdminRepository(
            adminApi,
            sharedSession,
            AuthRepository(adminApi, sharedSession),
            bus,
            UnconfinedTestDispatcher(),
        )
        // FileRepository shares the SAME bus as AdminRepository so the verify below observes
        // any emit caused by the inner runAdmin path.
        val repository = FileRepository(api, sharedSession, adminRepo, bus)

        val result = repository.list("/")

        assertTrue("expected Success, got $result", result is ApiResult.Success<*>)
        verify(exactly = 0) { bus.emit(SessionEvent.Unauthorized) }
    }
}
