package com.textvision.alistclient.home

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : HomeRepositoryContract {

    override suspend fun loadDashboard(): ApiResult<HomeData> = withContext(dispatcher) {
        val saved = sessionManager.loadSavedSession()
            ?: return@withContext ApiResult.Failure(401, "No active session")
        val base = saved.serverUrl

        val adminResult = runAdminWithRefresh(base)
        if (adminResult is AdminResult.Ok) {
            ApiResult.Success(adminResult.toData())
        } else {
            fetchPublicFallback(base)
        }
    }

    /**
     * Attempts admin calls. On 401, refreshes the session token via login and retries.
     * On 403 or NetworkError, falls back directly without refresh.
     */
    private suspend fun runAdminWithRefresh(base: String): AdminResult {
        val first = runAdmin(base)
        if (first is AdminResult.Ok) return first

        // Only attempt refresh on 401, not 403 or network errors
        if (first is AdminResult.NotAdmin && first.code == 401) {
            val session = sessionManager.loadSavedSession() ?: return first
            val username = session.username ?: return first
            val password = session.password ?: return first
            when (val refreshed = authRepository.login(base, username, password)) {
                is ApiResult.Success -> {
                    val second = runAdmin(base)
                    if (second is AdminResult.Ok) return second
                    if (second is AdminResult.NotAdmin) return second
                }
                else -> Unit
            }
        }
        return first
    }

    private suspend fun fetchPublicFallback(base: String): ApiResult<HomeData> = try {
        val response = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
        if (response.code == 200 && response.data != null) {
            ApiResult.Success(publicToGuest(response.data))
        } else ApiResult.Failure(response.code, response.message)
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        ApiResult.NetworkError(t)
    }

    /**
     * Alist v3 admin endpoint surface used here: only `GET /api/admin/storage/list`.
     * (Earlier specs also called `POST /api/admin/info`, but that route does
     * not exist on v3.x — POSTing it returns the SPA HTML shell, which fails
     * to deserialize as `AlistResponse`.)
     */
    private suspend fun runAdmin(base: String): AdminResult {
        val storage = safeCallApi { api.listStorage("${base}api/admin/storage/list") }
        return combine(storage)
    }

    private suspend fun <T : Any> safeCallApi(call: suspend () -> AlistResponse<T>): ApiResult<AlistResponse<T>> {
        return try {
            ApiResult.Success(call())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            ApiResult.NetworkError(t)
        }
    }

    private fun combine(storage: ApiResult<AlistResponse<StorageList>>): AdminResult {
        if (storage is ApiResult.NetworkError) {
            return AdminResult.Network(storage.cause)
        }
        if (storage is ApiResult.Success) {
            val storageResp = storage.data
            if (storageResp.code == 200 && storageResp.data != null) {
                return AdminResult.Ok(storageResp.data)
            }
            if (storageResp.code in setOf(401, 403)) {
                return AdminResult.NotAdmin(storageResp.code, "Forbidden")
            }
        }
        val code = (storage as? ApiResult.Success)?.data?.code ?: 500
        return AdminResult.NotAdmin(code, "Unexpected response")
    }

    private fun publicToGuest(settings: PublicSettings) = HomeData.Guest(
        serverTitle = settings.title ?: "Alist",
        serverVersion = settings.version,
        publicSettings = settings,
    )

    private sealed interface AdminResult {
        data class Ok(val storage: StorageList) : AdminResult
        data class NotAdmin(val code: Int, val message: String) : AdminResult
        data class Network(val cause: Throwable) : AdminResult

        fun toData(): HomeData.Admin = when (this) {
            is Ok -> {
                // /api/admin/info does not exist on Alist v3; approximate system
                // usage by summing across the storage list.
                val usedBytes = storage.content.sumOf { it.usedBytes }
                val totalBytes = storage.content.sumOf { it.totalBytes }
                HomeData.Admin(
                    serverTitle = "Alist", // admin/storage/list does not return site title
                    serverVersion = null, // not exposed by /api/admin/storage/list
                    startTime = null, // not exposed by /api/admin/storage/list
                    usedBytes = usedBytes,
                    totalBytes = totalBytes,
                    storages = storage.content,
                )
            }
            else -> error("Cannot convert non-Ok AdminResult to Admin data")
        }
    }
}