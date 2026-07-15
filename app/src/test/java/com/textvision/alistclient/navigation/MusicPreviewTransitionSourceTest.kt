package com.textvision.alistclient.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MusicPreviewTransitionSourceTest {
    private val transitionsPath = "src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt"
    private val previewPath = "src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt"

    @Test
    fun musicPreviewUsesDedicatedOpaqueSlideInsteadOfCrossFade() {
        val transitions = File(transitionsPath).readText()
        val preview = File(previewPath).readText()

        assertTrue(
            "MusicPreviewDest needs a dedicated enter transition so every entry point uses one motion policy",
            transitions.contains("musicPreviewEnterTransition"),
        )
        assertTrue(
            "The dedicated enter transition must move an opaque page over the source page",
            transitions.contains("slideInHorizontally"),
        )
        assertFalse(
            "MusicPreviewDest must not fade its incoming layer over the previous page",
            transitions.contains("MusicPreviewDest::class)) ->\n            fadeIn"),
        )
        assertTrue(
            "Music preview must paint an opaque root background before async content arrives",
            preview.contains("background(MaterialTheme.colorScheme.background)"),
        )
    }
}
