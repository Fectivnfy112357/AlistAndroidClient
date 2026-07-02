package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
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

    @Test
    fun fileRowsUseDownloadPlusMoreMenuForShareDirectLinkAndDelete() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()

        assertTrue(source.contains("if (item.isDir) {"))
        assertTrue(source.contains("Text(\"›\""))
        assertTrue(source.contains("else {"))
        assertTrue(source.contains("Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically)"))
        assertTrue(source.contains("DropdownMenu"))
        assertTrue(source.contains("DropdownMenuItem"))
        assertTrue(source.contains("Icons.Default.MoreVert"))
        assertTrue(source.contains("Text(\"分享链接\")"))
        assertTrue(source.contains("Text(\"复制直链\")"))
        assertTrue(source.contains("!item.downloadUrl.isNullOrBlank()"))
        assertTrue(source.contains("Text(\"删除\", color = CloudErrorText)"))
        assertTrue(source.contains("AlertDialog"))
        assertTrue(source.contains("确定删除「"))
        assertFalse(source.contains("IconButton(onClick = onShare, modifier = Modifier.testTag(\"share_button\"))"))
    }

    @Test
    fun fileScreenLaunchesLoadIfNeededWithProvidedInitialPath() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()
        assertTrue(source.contains("fun FileScreen("))
        assertTrue(source.contains("initialPath: String = \"/\""))
        assertTrue(source.contains("LaunchedEffect(initialPath) { viewModel.loadIfNeeded(initialPath) }"))
    }
}
