package com.textvision.alistclient.home

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskBucket
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.RoleList
import com.textvision.alistclient.network.dto.SessionInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.TaskInfo
import com.textvision.alistclient.network.dto.UserList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

        // Public first: failure means whole-page error.
        val publicResult = runPublic(base)
        if (publicResult is PublicResult.Failed) {
            return@withContext ApiResult.Failure(publicResult.code, publicResult.message)
        }
        val publicSection = (publicResult as PublicResult.Ok).toSection()

        // Admin sections: independent failures are tolerated.
        val storage = runAdmin(base) { api.listStorage("${base}api/admin/storage/list") }.toStorageSection()
        val serverStats = fetchServerStats(base)
        val session = runAdmin(base) { api.listSessions("${base}api/admin/session/list") }.toSessionSection()
        val task = fetchTaskBuckets(base)

        ApiResult.Success(
            HomeData(
                publicSection = publicSection,
                storageSection = storage,
                serverStatsSection = serverStats,
                sessionSection = session,
                taskSection = task,
            )
        )
    }

    override suspend fun retrySection(data: HomeData, key: SectionKey): HomeData = withContext(dispatcher) {
        val base = sessionManager.loadSavedSession()?.serverUrl
            ?: return@withContext data
        when (key) {
            SectionKey.Public -> data.copy(publicSection = fetchPublic(base))
            SectionKey.Storage -> data.copy(storageSection = runAdmin(base) {
                api.listStorage("${base}api/admin/storage/list")
            }.toStorageSection())
            SectionKey.ServerStats -> data.copy(serverStatsSection = fetchServerStats(base))
            SectionKey.Session -> data.copy(sessionSection = runAdmin(base) {
                api.listSessions("${base}api/admin/session/list")
            }.toSessionSection())
            SectionKey.Task -> data.copy(taskSection = fetchTaskBuckets(base))
        }
    }

    // --- Public ---

    private suspend fun fetchPublic(base: String): SectionResult<PublicData> {
        return try {
            val resp = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
            if (resp.code == 200 && resp.data != null) {
                SectionResult.Ok(publicToData(resp.data))
            } else {
                SectionResult.Failed(SectionFailure.Server(resp.code))
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            SectionResult.Failed(SectionFailure.Network)
        }
    }

    private sealed interface PublicResult {
        data class Ok(val data: PublicData) : PublicResult
        data class Failed(val code: Int, val message: String) : PublicResult
    }

    private suspend fun runPublic(base: String): PublicResult {
        return try {
            val resp = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
            if (resp.code == 200 && resp.data != null) {
                PublicResult.Ok(publicToData(resp.data))
            } else {
                PublicResult.Failed(resp.code, resp.message)
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            PublicResult.Failed(0, t.message ?: "network error")
        }
    }

    private fun publicToData(s: PublicSettings): PublicData = PublicData(
        siteTitle = s.title ?: "Alist",
        siteVersion = s.version,
        announcement = null, // not exposed by /api/public/settings; null
        logo = s.logo,
        favicon = null,
        mainColor = null,
        allowRegister = false,
    )

    private fun PublicResult.Ok.toSection() = SectionResult.Ok(data)

    // --- Admin generic ---

    private suspend fun <T : Any> runAdmin(base: String, call: suspend () -> AlistResponse<T>): AdminResult<T> {
        val first = safeCall(call)
        if (first is AdminResult.Ok) return first
        if (first is AdminResult.Unauthorized) {
            val refreshed = refreshAndRetry(base)
            if (refreshed) {
                return safeCall(call)
            }
        }
        return first
    }

    private suspend fun refreshAndRetry(base: String): Boolean {
        val session = sessionManager.loadSavedSession() ?: return false
        val username = session.username ?: return false
        val password = session.password ?: return false
        return when (val r = authRepository.login(base, username, password)) {
            is ApiResult.Success -> true
            else -> false
        }
    }

    private suspend fun <T : Any> safeCall(call: suspend () -> AlistResponse<T>): AdminResult<T> {
        return try {
            val resp = call()
            when {
                resp.code == 200 && resp.data != null -> AdminResult.Ok(resp.data)
                resp.code == 401 || resp.code == 403 -> AdminResult.Unauthorized
                else -> AdminResult.ServerError(resp.code)
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            AdminResult.Network
        }
    }

    private sealed interface AdminResult<out T> {
        data class Ok<T>(val data: T) : AdminResult<T>
        data object Unauthorized : AdminResult<Nothing>
        data class ServerError(val code: Int) : AdminResult<Nothing>
        data object Network : AdminResult<Nothing>
    }

    private fun <T> AdminResult<T>.toSection(): SectionResult<T> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(data)
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }

    // --- Storage ---

    private fun AdminResult<StorageList>.toStorageSection(): SectionResult<StorageData> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(StorageData(data.content))
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }

    // --- ServerStats (user + role conjoined) ---

    private suspend fun fetchServerStats(base: String): SectionResult<ServerStatsData> {
        val userResult: AdminResult<UserList> = runAdmin(base) { api.listUsers("${base}api/admin/user/list") }
        if (userResult is AdminResult.Unauthorized) {
            return SectionResult.Failed(SectionFailure.Unauthorized)
        }
        if (userResult !is AdminResult.Ok) {
            // userResult is ServerError or Network here — both translate uniformly
            return when (userResult) {
                is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(userResult.code))
                AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
                else -> error("unreachable")
            }
        }
        val roleResult: AdminResult<RoleList> = runAdmin(base) { api.listRoles("${base}api/admin/role/list") }
        if (roleResult !is AdminResult.Ok) {
            return when (roleResult) {
                is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(roleResult.code))
                AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
                else -> SectionResult.Failed(SectionFailure.Unauthorized)
            }
        }
        return SectionResult.Ok(
            ServerStatsData(
                userCount = userResult.data.total,
                roleCount = roleResult.data.total,
                disabledUserCount = userResult.data.content.count { it.disabled },
            )
        )
    }

    // --- Task buckets (7 types) ---

    private val taskTypes = listOf(
        "upload", "copy", "offline_download", "offline_download_transfer",
        "s3_transition", "decompress", "decompress_upload",
    )

    private suspend fun fetchTaskBuckets(base: String): SectionResult<TaskData> = coroutineScope {
        val deferreds = taskTypes.map { type ->
            async {
                type to runAdmin(base) { api.taskUndone("${base}api/admin/task/${type}/undone") }
            }
        }
        val results = deferreds.map { it.await() }
        val failed = results.filter { it.second is AdminResult.Unauthorized }
        if (failed.isNotEmpty()) {
            return@coroutineScope SectionResult.Failed(SectionFailure.Unauthorized)
        }
        val buckets = results.map { (type, r) ->
            val count = if (r is AdminResult.Ok) r.data.size else 0
            TaskBucket(type, count)
        }
        val failedBucketIds = results.filter { it.second is AdminResult.Network }
            .map { it.first }
        SectionResult.Ok(
            TaskData(
                runningCount = buckets.sumOf { it.running },
                finishedCount = 0, // not exposed in v3 list view; populated by task/done if needed
                failedBucketIds = failedBucketIds,
                buckets = buckets,
            )
        )
    }

    // --- Session ---

    private fun AdminResult<List<SessionInfo>>.toSessionSection(): SectionResult<SessionData> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(
            SessionData(
                totalCount = data.size,
                activeCount = data.count { it.status == 0 },
            )
        )
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }
}