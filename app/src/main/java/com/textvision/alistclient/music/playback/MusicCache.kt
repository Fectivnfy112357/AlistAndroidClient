package com.textvision.alistclient.music.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
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

    companion object {
        private const val CACHE_BYTES: Long = 512L * 1024L * 1024L
    }
}
