package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferScreenSourceTest {
    private val source = File("src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt").readText()

    @Test
    fun transferRowsUseCompactCardLayoutInsteadOfCloudListItemPlusDetachedProgress() {
        assertTrue(source.contains("private fun TransferRow"))
        assertTrue(source.contains("task.primaryActionLabel"))
        assertTrue(source.contains("task.progressText"))
        assertFalse(source.contains("CloudListItem("))
        assertFalse(source.contains("CloudCard {\n                LazyColumn"))
        assertFalse(source.contains("CloudCard {\r\n                LazyColumn"))
    }

    @Test
    fun transferScreenUsesTabsAndVisibleTransfers() {
        assertTrue(source.contains("ScrollableTabRow") || source.contains("TabRow"))
        assertTrue(source.contains("TransferTab.entries"))
        assertTrue(source.contains("state.visibleTransfers"))
        assertTrue(source.contains("selectedTab"))
    }

    @Test
    fun transferScreenShowsDeleteActionWithConfirmation() {
        assertTrue(source.contains("fun delete(id: String) = manager.delete(id)"))
        assertTrue(source.contains("AlertDialog"))
        assertTrue(source.contains("删除后会取消当前传输，并永久删除这条记录。"))
        assertTrue(source.contains("将永久删除这条传输记录。"))
        assertTrue(source.contains("Text(\"删除\", color = CloudErrorText)"))
    }
}
