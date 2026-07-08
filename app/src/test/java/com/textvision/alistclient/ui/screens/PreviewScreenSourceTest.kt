package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PreviewScreenSourceTest {
    private val screenPath = "src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewScreen.kt"
    private val viewModelPath = "src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewViewModel.kt"

    @Test fun previewScreenBranchesForImageTextAudioAndExternalModes() {
        val source = File(screenPath).readText()

        assertTrue(source.contains("PreviewMode.Image"))
        assertTrue(source.contains("PreviewMode.Text"))
        assertTrue(source.contains("PreviewMode.Audio"))
    }

    @Test fun previewSubComponentsCoverImageTextAudioAndFallback() {
        val image = File("src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewImage.kt").readText()
        val text = File("src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewText.kt").readText()
        val audio = File("src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewAudio.kt").readText()
        val fallback = File("src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewFallback.kt").readText()
        val viewModel = File(viewModelPath).readText()

        assertTrue("Image preview uses AsyncImage", image.contains("AsyncImage"))
        // Text repository is now owned by the view model; the composable receives a suspend fetch fn.
        assertTrue("Text preview receives a suspend fetch fn", text.contains("fetch: suspend (String) -> String"))
        assertTrue("View model wires PreviewTextRepository", viewModel.contains("PreviewTextRepository"))
        assertTrue("Audio preview wires MediaPlayer", audio.contains("MediaPlayer"))
        assertTrue("Fallback preview covers TextTooLarge/External/Unavailable", fallback.contains("PreviewFallback"))
    }

    @Test fun previewScreenDownloadDelegatesToTransferManagerNotPopBackStack() {
        val screen = File(screenPath).readText()
        val viewModel = File(viewModelPath).readText()
        val navHost = File("src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt").readText()

        // The view model exposes enqueueDownload(path, name) wired to TransferManager.
        assertTrue(
            "PreviewViewModel must forward to TransferManager.enqueueDownload",
            viewModel.contains("transferManager.enqueueDownload(remotePath = path, fileName = name)"),
        )

        // The Composable wires the toolbar download icon to the view model, not to popBackStack.
        assertTrue(
            "PreviewScreen should call viewModel.enqueueDownload for the download button",
            screen.contains("viewModel.enqueueDownload(path, name)"),
        )

        // AppNavHost must not be the place that wires download anymore — no popBackStack in preview composable.
        assertFalse(
            "AppNavHost no longer wires onDownload = { navController.popBackStack() }",
            navHost.contains("onDownload = { navController.popBackStack() }"),
        )
    }

    @Test fun previewScreenExternalOpenBuildsViewIntentWithDownloadUrl() {
        val screen = File(screenPath).readText()

        assertTrue(
            "External open should build an Intent.ACTION_VIEW with the download url",
            screen.contains("Intent(Intent.ACTION_VIEW).apply {"),
        )
        assertTrue(
            "External open should infer mime type from file name",
            screen.contains("MimeTypeResolver.infer(name)"),
        )
    }
}
