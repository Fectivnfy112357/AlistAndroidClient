package com.textvision.alistclient.file

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.SessionEvent
import com.textvision.alistclient.auth.SessionEventBus
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.CopyMovePathRequest
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import com.textvision.alistclient.network.dto.toFileItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val adminRepository: AdminRepository,
    private val sessionEventBus: SessionEventBus,
) : FileRepositoryContract, FileOperationRepositoryContract {

    // ── Warm cache (see FileRepositoryContract + AppStartupWarmer) ────────
    // Per-path so we can later pre-warm subdirectories the user is likely
    // to drill into. Today only "/" is warmed at app startup, but the
    // storage keeps a timestamp per path so the staleness check is exact.
    private val _warmCache = MutableStateFlow<Map<String, List<FileItem>>>(emptyMap())
    override val warmCache: StateFlow<Map<String, List<FileItem>>> = _warmCache.asStateFlow()
    private val warmTimestamps = HashMap<String, Long>()
    private val warmMutex = Mutex()

    override suspend fun warmUp(path: String) {
        warmMutex.withLock {
            val ts = warmTimestamps[path] ?: 0L
            if (ts > 0L && System.currentTimeMillis() - ts < WARM_MIN_INTERVAL_MS) return
            when (val result = list(path)) {
                is ApiResult.Success -> {
                    _warmCache.value = _warmCache.value + (path to result.data)
                    warmTimestamps[path] = System.currentTimeMillis()
                }
                else -> { /* keep prior cache; lazy load retries on demand */ }
            }
        }
    }

    override fun loadIfCached(path: String, maxAgeMs: Long): List<FileItem>? {
        val ts = warmTimestamps[path] ?: return null
        if (System.currentTimeMillis() - ts > maxAgeMs) return null
        return _warmCache.value[path]
    }

    private fun baseUrl(): String =
        sessionManager.loadSavedSession()?.serverUrl ?: error("No active session — cannot resolve server URL")

    override suspend fun list(path: String): ApiResult<List<FileItem>> = runAlistWithRefresh(
        request = {
            val base = baseUrl()
            val response = api.list("${base}api/fs/list", FsListRequest(path = path))
            if (response.code == 200) {
                val disabledMounts = disabledMountPaths(base)
                response.data?.content.orEmpty()
                    .map { it.toFileItem(path, base) }
                    .filter { it.name !in disabledMounts }
                    .sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
                    .let { ApiResult.Success(it) }
            } else ApiResult.Failure(response.code, response.message)
        }
    )

    /**
     * Fetch the set of disabled mount paths from the admin storage list. Returns an
     * empty set for non-admin callers so file listing is unaffected for them.
     */
    private suspend fun disabledMountPaths(base: String): Set<String> =
        when (val r = adminRepository.runAdmin(base) { api.listStorage("${base}api/admin/storage/list") }) {
            is AdminResult.Ok -> r.data?.content?.filter { it.disabled }?.map { it.mountPath.trim('/') }?.toSet().orEmpty()
            else -> emptySet()
        }

    override suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>> = runAlist {
        val base = baseUrl()
        val response = api.search("${base}api/fs/search", FsSearchRequest(path = path, keywords = keyword))
        if (response.code == 200) ApiResult.Success(response.data?.content.orEmpty().map { it.toFileItem(path, base) })
        else ApiResult.Failure(response.code, response.message)
    }

    suspend fun mkdir(path: String): ApiResult<Unit> = runUnit { api.mkdir("${baseUrl()}api/fs/mkdir", MkdirRequest(path)) }
    suspend fun rename(path: String, name: String): ApiResult<Unit> = runUnit { api.rename("${baseUrl()}api/fs/rename", RenameRequest(path, name)) }

    override suspend fun delete(paths: List<String>): ApiResult<Unit> {
        if (paths.isEmpty()) return ApiResult.Success(Unit)
        val dir = paths.first().substringBeforeLast('/', missingDelimiterValue = "/").ifBlank { "/" }
        val names = paths.map { it.substringAfterLast('/') }
        return runUnit { api.remove("${baseUrl()}api/fs/remove", RemoveRequest(dir, names)) }
    }

    override suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit> =
        runUnit { api.copy("${baseUrl()}api/fs/copy", CopyMovePathRequest(srcPath = srcPath, dstPath = dstDir)) }

    override suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit> =
        runUnit { api.move("${baseUrl()}api/fs/move", CopyMovePathRequest(srcPath = srcPath, dstPath = dstDir)) }

    private suspend fun runUnit(block: suspend () -> com.textvision.alistclient.network.dto.AlistResponse<Unit>): ApiResult<Unit> = runAlist {
        val response = block()
        if (response.code == 200) ApiResult.Success(Unit) else ApiResult.Failure(response.code, response.message)
    }

    private suspend fun <T> runAlistWithRefresh(request: suspend () -> ApiResult<T>): ApiResult<T> {
        val first = runAlist(request)
        if (first !is ApiResult.Failure || first.code != 401) return first
        return when (val refreshed = refreshSession()) {
            is ApiResult.Success -> {
                val retried = runAlist(request)
                if (retried is ApiResult.Failure && retried.code == 401) {
                    sessionEventBus.emit(SessionEvent.Unauthorized)
                }
                retried
            }
            is ApiResult.Failure -> {
                sessionEventBus.emit(SessionEvent.Unauthorized)
                refreshed
            }
            is ApiResult.NetworkError -> refreshed
        }
    }

    private suspend fun refreshSession(): ApiResult<SavedSession> = runAlist {
        val saved = sessionManager.loadSavedSession() ?: return@runAlist ApiResult.Failure(401, "No active session")
        val response = api.login(
            "${saved.serverUrl}api/auth/login",
            SkipAuthRetry.HEADER,
            LoginRequest(saved.username, saved.password),
        )
        if (response.code == 200 && response.data?.token?.isNotBlank() == true) {
            val refreshed = saved.copy(token = response.data.token)
            sessionManager.saveSession(refreshed)
            ApiResult.Success(refreshed)
        } else {
            ApiResult.Failure(response.code, response.message)
        }
    }

    private suspend fun <T> runAlist(block: suspend () -> ApiResult<T>): ApiResult<T> = try { block() } catch (t: CancellationException) { throw t } catch (t: Throwable) { ApiResult.NetworkError(t) }
}

/** Cache TTL: VM treats pre-fetched listings as fresh for this long. */
internal const val FILE_WARM_TTL_MS = 60_000L

/** Cooldown between warm-up triggers for the same path. */
private const val WARM_MIN_INTERVAL_MS = 30_000L
