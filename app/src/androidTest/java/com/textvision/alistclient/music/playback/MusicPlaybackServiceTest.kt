package com.textvision.alistclient.music.playback

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MusicPlaybackServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceStarts_andStopsCleanly() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MusicPlaybackService::class.java,
        )
        val binder = serviceRule.startService(intent)
        assert(binder.isBinderAlive)
        serviceRule.unbindService()
    }
}
