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
    fun transferScreenUsesCustomPillTabSwitcherAndVisibleTransfers() {
        assertTrue(source.contains("private fun TransferTabSwitcher"))
        assertTrue(source.contains("TransferTab.entries"))
        assertTrue(source.contains("state.visibleTransfers"))
        assertTrue(source.contains("selectedTab"))
        assertTrue(source.contains("CloudSurfaceMuted"))
        assertTrue(source.contains("CloudShapes.Control"))
        assertTrue(source.contains("cloudClickable"))
        assertFalse(source.contains("TabRow("))
        assertFalse(source.contains("import androidx.compose.material3.Tab"))
    }

    @Test
    fun transferScreenShowsDeleteActionWithConfirmation() {
        assertTrue(source.contains("fun delete(id: String) = manager.delete(id)"))
        assertTrue(source.contains("AlertDialog"))
        assertTrue(source.contains("删除后会取消当前传输，并永久删除这条记录。"))
        assertTrue(source.contains("将永久删除这条传输记录。"))
        assertTrue(source.contains("Text(\"删除\", color = CloudErrorText)"))
    }

    @Test
    fun transferRowHidesRedundantStatusTextForCompletedTasks() {
        // The completion status is rendered as a pill chip on the right edge; the
        // statusText slot must collapse for completed rows so we don't show "已完成"
        // alongside the "完成" chip.
        val completedRow = source.substringAfter("private fun TransferRow").substringBefore("private fun TransferAction")
        assertTrue(
            "Completed rows must skip the statusText label so it does not duplicate the 完成 chip.",
            completedRow.contains("!task.isComplete") || completedRow.contains("task.isComplete.not()") || completedRow.contains("task.status != TransferStatus.Success")
        )
        assertTrue(completedRow.contains("task.statusText"))
    }
}
