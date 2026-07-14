package com.textvision.alistclient.music.playback

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackControllerTest {
    private val controller = PlaybackController(
        context = ApplicationProvider.getApplicationContext(),
        indexRepo = mockk<MusicIndexRepository>(relaxed = true),
    )

    @Test
    fun initialState_isEmpty() = runTest {
        val s = controller.state.first()
        assertEquals(null, s.current)
        assertEquals(false, s.isPlaying)
        assertEquals(RepeatMode.OFF, s.repeatMode)
    }

    @Test
    fun publishState_updatesFlow() = runTest {
        val song = Song(
            path = "/x.mp3", trackNo = "01", trackNoInt = 1,
            artist = "a", album = "al", title = "t",
            lrcPath = null, coverPath = null, sizeBytes = 100L,
        )
        controller.publishState(
            PlaybackState(current = song, isPlaying = true, positionMs = 1000, durationMs = 2000),
        )
        val s = controller.state.first()
        assertEquals(song, s.current)
        assertEquals(true, s.isPlaying)
        assertEquals(1000L, s.positionMs)
    }

    @Test
    fun playbackStartState_selectsRequestedSong_beforeServiceIsReady() {
        val songs = listOf(
            Song("/a.mp3", "1", 1, "x", "y", "first", null, null, 0L),
            Song("/b.mp3", "2", 2, "x", "y", "second", null, null, 0L),
        )

        assertEquals(songs[1], playbackStartState(songs, 1).current)
    }

    @Test
    fun playQueue_intentHasCorrectExtras() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val songs = listOf(
            Song("/a.mp3", "1", 1, "x", "y", "t1", null, null, 0L),
            Song("/b.mp3", "2", 2, "x", "y", "t2", null, null, 0L),
        )
        val intent = android.content.Intent(ctx, MusicPlaybackService::class.java).apply {
            action = PlaybackIntents.ACTION_PLAY_QUEUE
            putStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS, ArrayList(songs.map { it.path }))
            putExtra(PlaybackIntents.EXTRA_START_INDEX, 1)
        }
        assertEquals(PlaybackIntents.ACTION_PLAY_QUEUE, intent.action)
        assertEquals(listOf("/a.mp3", "/b.mp3"), intent.getStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS))
        assertEquals(1, intent.getIntExtra(PlaybackIntents.EXTRA_START_INDEX, -1))
    }
}
