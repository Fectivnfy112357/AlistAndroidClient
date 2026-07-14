package com.textvision.alistclient.ui.feature.music

import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.MusicIndexState
import com.textvision.alistclient.music.data.model.Artist
import com.textvision.alistclient.music.playback.PlaybackController
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

        val vm = MusicLibraryViewModel(repo, playback)
        val s = vm.state.first()
        assertEquals(UiIndexState.Ready, s.indexState)
        assertEquals(1, s.artists.size)
    }

    @Test
    fun visibleItemCount_growsInFixedPages_withoutExceedingTotal() {
        assertEquals(40, visibleItemCount(total = 95, requested = 40))
        assertEquals(80, visibleItemCount(total = 95, requested = 80))
        assertEquals(95, visibleItemCount(total = 95, requested = 120))
    }
}
