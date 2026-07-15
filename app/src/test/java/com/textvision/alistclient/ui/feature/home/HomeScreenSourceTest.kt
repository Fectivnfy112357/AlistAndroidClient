package com.textvision.alistclient.ui.feature.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenSourceTest {
    @Test
    fun dashboardDeclaresStableContentTypes() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt").readText()

        assertTrue(source.contains("key = \"hero\", contentType = \"hero\""))
        assertTrue(source.contains("key = { it.mountPath }"))
        assertTrue(source.contains("contentType = { \"storage\" }"))
    }
}
