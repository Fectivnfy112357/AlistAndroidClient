package com.textvision.alistclient.home

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
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
     * Serialized admin calls — safe vs race conditions in MockWebServer
     * (parallel async both consume first enqueued response, wrong DTO = crash).
     * Trade-off: ~2x round trips under load; acceptable for cheap admin endpoints.
     */
    private suspend fun runAdmin(base: String): AdminResult {
        val info = safeCallApi { api.adminInfo("${base}api/admin/info", SkipAuthRetry.HEADER) }
        val storage = safeCallApi { api.listStorage("${base}api/admin/storage/list", SkipAuthRetry.HEADER) }
        return combine(info, storage)
    }

    private suspend fun <T : Any> safeCallApi(call: suspend () -> AlistResponse<T>): ApiResult<AlistResponse<T>> {
        return try {
            ApiResult.Success(call())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            ApiResult.NetworkError(t)
        }
    }

    private fun combine(
        info: ApiResult<AlistResponse<AdminInfo>>,
        storage: ApiResult<AlistResponse<StorageList>>
    ): AdminResult {
        if (info is ApiResult.NetworkError && storage is ApiResult.NetworkError) {
            return AdminResult.Network(info.cause)
        }
        if (info is ApiResult.Success && storage is ApiResult.Success) {
            val infoResp = info.data
            val storageResp = storage.data
            if (infoResp.code == 200 && storageResp.code == 200 && infoResp.data != null && storageResp.data != null) {
                return AdminResult.Ok(infoResp.data, storageResp.data)
            }
            if (infoResp.code in setOf(401, 403) || storageResp.code in setOf(401, 403)) {
                val code = if (infoResp.code in setOf(401, 403)) infoResp.code else storageResp.code
                return AdminResult.NotAdmin(code, "Forbidden")
            }
        }
        val firstFailure = listOf(info, storage)
            .filterIsInstance<ApiResult.Success<*>>()
            .map { (it.data as AlistResponse<*>).code }
            .firstOrNull { it != 200 } ?: 500
        return AdminResult.NotAdmin(firstFailure, "Unexpected response")
    }

    private fun publicToGuest(settings: PublicSettings) = HomeData.Guest(
        serverTitle = settings.title ?: "Alist",
        serverVersion = settings.version,
        publicSettings = settings,
    )

    private sealed interface AdminResult {
        data class Ok(val info: AdminInfo, val storage: StorageList) : AdminResult
        data class NotAdmin(val code: Int, val message: String) : AdminResult
        data class Network(val cause: Throwable) : AdminResult

        fun toData(): HomeData.Admin = when (this) {
            is Ok -> HomeData.Admin(
                serverTitle = "Alist", // admin/info does not return site title
                serverVersion = info.version,
                startTime = info.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() },
                usedBytes = info.usedBytes,
                totalBytes = info.totalBytes,
                storages = storage.content,
            )
            else -> error("Cannot convert non-Ok AdminResult to Admin data")
        }
    }
}