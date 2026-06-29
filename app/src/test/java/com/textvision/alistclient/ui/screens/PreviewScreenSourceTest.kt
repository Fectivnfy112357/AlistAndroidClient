package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PreviewScreenSourceTest {
    @Test fun previewScreenBranchesForImageTextAudioAndExternalModes() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt").readText()

        assertTrue(source.contains("PreviewMode.Image"))
        assertTrue(source.contains("PreviewMode.Text"))
        assertTrue(source.contains("PreviewMode.Audio"))
        assertTrue(source.contains("AsyncImage"))
        assertTrue(source.contains("MediaPlayer"))
        assertTrue(source.contains("PreviewTextRepository"))
    }
}
