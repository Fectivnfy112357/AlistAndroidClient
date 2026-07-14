package com.textvision.alistclient.music.playback

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicCacheTest {
    @Test
    fun size_isNonNegative_afterConstruct() = runTest {
        val cache = MusicCache(
            context = ApplicationProvider.getApplicationContext(),
            okHttp = OkHttpClient(),
        )
        val size = cache.sizeBytes
        assertTrue(size >= 0L)
    }
}
