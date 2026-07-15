package com.textvision.alistclient.ui.feature.music

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicLibraryScreenStateIsolationTest {
    @Test
    fun playbackCollectionLivesOnlyInMiniPlayerBoundary() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt").readText()
        val root = source.substringBefore("private fun MusicLibraryMiniPlayer(")
        val boundary = source.substringAfter("private fun MusicLibraryMiniPlayer(")

        assertFalse(root.contains("viewModel.playbackState.collectAsStateWithLifecycle()"))
        assertTrue(boundary.contains("viewModel.playbackState.collectAsStateWithLifecycle()"))
    }
}
