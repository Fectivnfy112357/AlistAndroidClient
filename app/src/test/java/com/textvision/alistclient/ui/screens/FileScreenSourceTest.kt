package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileScreenSourceTest {
    @Test fun fileRowsNavigateToPreviewRouteOnClick() {
        val fileScreen = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()
        val navHost = File("src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt").readText()

        assertTrue(navHost.contains("AppRoute.Preview.create("))
        assertTrue(fileScreen.contains("onPreview = { onPreview(item) }"))
        assertTrue(fileScreen.contains("onClick = if (item.isDir) onOpenDir else onPreview"))
    }
}
