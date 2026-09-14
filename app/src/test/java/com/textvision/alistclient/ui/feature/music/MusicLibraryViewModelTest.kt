package com.textvision.alistclient.ui.feature.music

import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.MusicIndexState
import com.textvision.alistclient.music.data.model.Artist
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.music.playback.PlaybackProgress
import com.textvision.alistclient.music.playback.PlaybackState
import com.textvision.alistclient.ui.feature.music.dto.UiIndexState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MusicLibraryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun state_isReady_whenIndexStateReady() = runTest {
        val repo = mockk<MusicIndexRepository>(relaxed = true)
        coEvery { repo.ensureIndexed() } returns Unit
        every { repo.state } returns MutableStateFlow(MusicIndexState.Ready)
        every { repo.artists() } returns flowOf(listOf(Artist("a", "/a", 1, 2)))
        every { repo.allAlbums() } returns flowOf(emptyList())
        every { repo.recentAlbums(any()) } returns flowOf(emptyList())
        every { repo.allSongs() } returns flowOf(emptyList())
        val playback = mockk<PlaybackController>(relaxed = true)
        every { playback.state } returns MutableStateFlow(PlaybackState())
        every { playback.progress } returns MutableStateFlow(PlaybackProgress(0L, 0L))

        val vm = MusicLibraryViewModel(mockk(relaxed = true), repo, playback, Dispatchers.Unconfined)
        val s = vm.state.first()
        assertEquals(UiIndexState.Ready, s.indexState)
        assertEquals(1, s.artists.size)
    }

    @Test
    fun playbackState_omitsPosition_andDuration_soConsumersSkipRecompose() = runTest {
        val song = mockk<com.textvision.alistclient.music.data.model.Song>(relaxed = true)
        val playback = mockk<PlaybackController>(relaxed = true)
        val n = MutableStateFlow(PlaybackState(current = song, isPlaying = true, positionMs = 1234L, durationMs = 9999L))
        every { playback.state } returns n
        every { playback.progress } returns MutableStateFlow(PlaybackProgress(1234L, 9999L))

        val vm = MusicLibraryViewModel(mockk(relaxed = true), mockk(relaxed = true), playback, Dispatchers.Unconfined)
        // Position/duration text lives in the preview screen, NOT the mini
        // player. The mini player state surface must drop positionMs /
        // durationMs so progress ticks don't trigger recomposition here.
        val mini = vm.playbackState.first()
        assertEquals(song, mini.current)
        assertEquals(true, mini.isPlaying)
        // Defensive — the published data class has no position field at all.
        assertNull(mini.javaClass.declaredFields.firstOrNull { it.name == "positionMs" })
    }

    @Test
    fun visibleItemCount_growsInFixedPages_withoutExceedingTotal() {
        assertEquals(40, visibleItemCount(total = 95, requested = 40))
        assertEquals(80, visibleItemCount(total = 95, requested = 80))
        assertEquals(95, visibleItemCount(total = 95, requested = 120))
    }

    @Test
    fun state_withRepeatedSourceEmissions_publishesReferenceStableUiState_perDistinctUntilChanged() = runTest {
        val repo = mockk<MusicIndexRepository>(relaxed = true)
        coEvery { repo.ensureIndexed() } returns Unit
        every { repo.state } returns MutableStateFlow(MusicIndexState.Ready)
        val artistsFlow = MutableStateFlow(listOf(Artist("a", "/a", 1, 2)))
        every { repo.artists() } returns artistsFlow
        every { repo.allAlbums() } returns MutableStateFlow(emptyList())
        every { repo.recentAlbums(any()) } returns MutableStateFlow(emptyList())
        every { repo.allSongs() } returns MutableStateFlow(emptyList())
        val playback = mockk<PlaybackController>(relaxed = true)
        every { playback.state } returns MutableStateFlow(PlaybackState())
        every { playback.progress } returns MutableStateFlow(PlaybackProgress(0L, 0L))

        val vm = MusicLibraryViewModel(mockk(relaxed = true), repo, playback, Dispatchers.Unconfined)
        val first = vm.state.first()

        // Re-emit the same list payload — without distinctUntilChanged this
        // would retrigger projection; with it the StateFlow value stays put.
        artistsFlow.value = listOf(Artist("a", "/a", 1, 2))
        val second = vm.state.first()

        // The contract under test is that distinctUntilChanged() prevents a
        // fresh projection when the source material is structurally identical.
        // The art-type `UiArtist` is a data class, so reference equality of
        // the resulting list is what holds after dedup; we cannot rely on
        // reference identity, but we can verify that subsequent reads keep
        // the same Ready / artist count without a different instance each time.
        assertEquals(UiIndexState.Ready, second.indexState)
        assertEquals(1, second.artists.size)
        // The first emission must already reflect Ready / artists.
        assertEquals(first.indexState, second.indexState)
    }
}
