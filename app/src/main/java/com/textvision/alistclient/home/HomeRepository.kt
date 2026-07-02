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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

        val first = runAdmin(base)
        if (first is AdminResult.Ok) {
            ApiResult.Success(first.toData())
        } else {
            fetchPublicFallback(base)
        }
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

    private suspend fun runAdmin(base: String): AdminResult = coroutineScope {
        val infoDeferred = async {
            try {
                ApiResult.Success(api.adminInfo("${base}api/admin/info", SkipAuthRetry.HEADER))
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                ApiResult.NetworkError(t) as ApiResult<AlistResponse<AdminInfo>>
            }
        }
        val storageDeferred = async {
            try {
                ApiResult.Success(api.listStorage("${base}api/admin/storage/list", SkipAuthRetry.HEADER))
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                ApiResult.NetworkError(t) as ApiResult<AlistResponse<StorageList>>
            }
        }
        val info = infoDeferred.await()
        val storage = storageDeferred.await()
        combine(info, storage)
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