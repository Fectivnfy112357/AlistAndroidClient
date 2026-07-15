package com.textvision.alistclient.ui.feature.music

import com.textvision.alistclient.music.data.model.LrcLine
import com.textvision.alistclient.music.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPlayerViewModelTest {
    @Test
    fun chromeProjectionIgnoresPositionAndDuration() {
        val first = MusicPlayerUiState(
            playback = PlaybackState(positionMs = 1_000L, durationMs = 180_000L),
        )
        val second = MusicPlayerUiState(
            playback = PlaybackState(positionMs = 2_000L, durationMs = 180_000L),
        )

        assertEquals(first.toPlayerChromeState(), second.toPlayerChromeState())
    }

    @Test
    fun currentLyricLineIndex_advancesWhenPlaybackProgressAdvances() {
        val lines = listOf(
            LrcLine(timeMs = 0L, text = "第一句"),
            LrcLine(timeMs = 1_000L, text = "第二句"),
            LrcLine(timeMs = 2_000L, text = "第三句"),
        )

        assertEquals(0, currentLyricLineIndex(lines, positionMs = 0L))
        assertEquals(1, currentLyricLineIndex(lines, positionMs = 1_500L))
    }
}
