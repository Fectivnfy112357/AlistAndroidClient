package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
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

    @Test fun previewScreenDownloadDelegatesToTransferManagerNotPopBackStack() {
        val screen = File("src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt").readText()
        val viewModel = File("src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt").readText()
        val navHost = File("src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt").readText()

        // The view model exposes enqueueDownload(path, name) wired to TransferManager.
        assertTrue(
            "PreviewViewModel must forward to TransferManager.enqueueDownload",
            viewModel.contains("transferManager.enqueueDownload(remotePath = path, fileName = name)")
        )

        // The Composable wires the toolbar download icon to the view model, not to popBackStack.
        assertTrue(
            "PreviewScreen should call viewModel.enqueueDownload for the download button",
            screen.contains("viewModel.enqueueDownload(path, name)")
        )

        // AppNavHost must not be the place that wires download anymore — no popBackStack in preview composable.
        assertFalse(
            "AppNavHost no longer wires onDownload = { navController.popBackStack() }",
            navHost.contains("onDownload = { navController.popBackStack() }")
        )
    }

    @Test fun previewScreenExternalOpenBuildsViewIntentWithDownloadUrl() {
        val screen = File("src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt").readText()

        assertTrue(
            "External open should build an Intent.ACTION_VIEW with the download url",
            screen.contains("Intent(Intent.ACTION_VIEW).apply {")
        )
        assertTrue(
            "External open should infer mime type from file name",
            screen.contains("MimeTypeResolver.infer(name)")
        )
    }
}
