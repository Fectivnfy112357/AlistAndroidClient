package com.textvision.alistclient.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabBackgroundSourceTest {
    @Test
    fun mainTabsUseOnlyTheNavigationHostsSharedSkyBackground() {
        val navHost = File("src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt").readText()
        val scaffold = File("src/main/java/com/textvision/alistclient/ui/foundation/Scaffold.kt").readText()
        val home = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt").readText()
        val files = File("src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt").readText()
        val transfers = File("src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt").readText()
        val settings = File("src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsContent.kt").readText()
        val music = File("src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt").readText()

        assertTrue(navHost.contains("SkyBlueBackground()"))
        assertTrue(navHost.contains("CloudDecor()"))
        assertTrue(navHost.contains("containerColor = Color.Transparent"))
        assertTrue(scaffold.contains("transparentBase"))
        assertTrue(home.contains("transparentBase = true"))
        assertTrue(home.contains("background = {}"))
        assertTrue(files.contains("transparentBase = true"))
        assertTrue(files.contains("background = {}"))
        assertTrue(transfers.contains("transparentBase = true"))
        assertTrue(transfers.contains("background = {}"))
        assertTrue(settings.contains("AppScaffold("))
        assertTrue(settings.contains("transparentBase = true"))
        assertTrue(settings.contains("background = {}"))
        assertTrue(music.contains("transparentBase = true"))
        assertTrue(music.contains("background = {}"))
    }
}
