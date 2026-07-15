package com.textvision.alistclient.music.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.DataSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Singleton
class MusicCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient,
) {
    private val cacheDir: File = File(context.filesDir, "music_cache").apply { mkdirs() }
    val cache: SimpleCache = SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
        StandaloneDatabaseProvider(context),
    )

    val cacheDataSourceFactory: DataSource.Factory
        get() = CacheDataSource.Factory()
            .setCache(currentCache)
            // C1: stable cache key. By default ExoPlayer's CacheDataSource uses the
            // full request URI as the cache key — but our URIs include a rotating
            // `?sign=...` query parameter that changes every ~30 minutes. That
            // makes the on-disk cache effectively write-only: re-requesting a song
            // minutes later gives a different URI and bypasses the cache entirely.
            //
            // CacheKeyPolicy.getCacheKey is invoked on every cache read/write; by
            // stripping `?...` we collapse all variants of the same file onto a
            // single key so re-listening to a song you just played reads from
            // local bytes (path is the only thing that varies by source identity).
            //
            // The upstream OkHttpDataSource still receives the full signed URI so
            // networking works — only the cache lookup is key-stabilised.
            .setCacheKeyFactory(PathOnlyCacheKeyFactory)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(okHttp),
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private var currentCache: SimpleCache = cache

    val sizeBytes: Long
        get() = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Evict all cached media by releasing the active [SimpleCache] instance. */
    suspend fun clear() {
        currentCache.release()
        currentCache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    private companion object {
        const val CACHE_BYTES: Long = 512L * 1024L * 1024L
    }
}

/**
 * CacheKeyFactory that strips query parameters from the request URI before computing
 * the cache key. Used by [MusicCache] so that signed Alist URLs (`?sign=...`) read
 * from disk even after the signature has rotated. The upstream network source still
 * receives the full signed URI — only the cache lookup is rebased to the file path.
 */
@UnstableApi
private object PathOnlyCacheKeyFactory : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val raw = dataSpec.uri.toString()
        val queryStart = raw.indexOf('?')
        return if (queryStart >= 0) raw.substring(0, queryStart) else raw
    }
}
