package com.textvision.alistclient.music.data

import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.FsGetRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SignKind { THUMBNAIL, DOWNLOAD }

internal data class SignEntry(
    val sign: String,
    val expiresAtMs: Long,
)

@Singleton
class SignProvider @Inject constructor(
    private val api: AlistApi,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val cache = mutableMapOf<String, SignEntry>()
    private val mutex = Mutex()

    suspend fun get(path: String, kind: SignKind, baseUrl: String = ""): String? {
        val now = System.currentTimeMillis()
        mutex.withLock {
            val hit = cache[path]
            if (hit != null && hit.expiresAtMs > now + REFRESH_MARGIN_MS) {
                return hit.sign
            }
        }
        val resolved = try {
            val url = baseUrl.trimEnd('/') + "/api/fs/get"
            val resp = withContext(dispatcher) {
                api.fsGet(url, FsGetRequest(path))
            }
            if (resp.code == 200) resp.data?.sign else null
        } catch (t: Throwable) {
            null
        } ?: return null
        mutex.withLock { cache[path] = SignEntry(resolved, now + TTL_MS) }
        return resolved
    }

    suspend fun invalidate(path: String) {
        mutex.withLock { cache.remove(path) }
    }

    suspend fun primeThumbnails(
        paths: List<String>,
        baseUrl: String = "",
    ) {
        if (paths.isEmpty()) return
        coroutineScope {
            paths.map { path ->
                async {
                    runCatching { get(path, SignKind.THUMBNAIL, baseUrl) }
                    Unit
                }
            }.awaitAll()
        }
    }

    companion object {
        private const val TTL_MS = 30 * 60 * 1_000L
        private const val REFRESH_MARGIN_MS = 60 * 1_000L
    }
}
