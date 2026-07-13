package com.textvision.alistclient.ui.foundation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBottomBarSourceTest {
    @Test
    fun bottomTabsDisableTouchRipple() {
        val source = File("src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt").readText()

        assertTrue(source.contains("indication = null"))
        assertTrue(source.contains("MutableInteractionSource"))
    }
}
