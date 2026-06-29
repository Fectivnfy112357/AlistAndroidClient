package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferScreenSourceTest {
    @Test
    fun transferRowsUseCompactCardLayoutInsteadOfCloudListItemPlusDetachedProgress() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt").readText()

        assertTrue(source.contains("private fun TransferRow"))
        assertTrue(source.contains("task.primaryActionLabel"))
        assertTrue(source.contains("task.progressText"))
        assertFalse(source.contains("CloudListItem("))
        assertFalse(source.contains("CloudCard {\n                LazyColumn"))
        assertFalse(source.contains("CloudCard {\r\n                LazyColumn"))
    }
}
