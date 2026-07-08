package com.textvision.alistclient.ui.feature.file

import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUiStateTest {

    private fun file(name: String, path: String) = FileItem(
        name = name,
        path = path,
        isDir = false,
        size = 0L,
        modifiedAt = null,
        extension = null,
        type = FileType.Other,
        thumbnailUrl = null,
        downloadUrl = null,
    )

    @Test
    fun `isAllSelected true only when all visible files selected`() {
        val files = listOf(file("a.txt", "/a.txt"), file("b.txt", "/b.txt"))
        val state = FileUiState(
            files = files,
            selection = setOf("/a.txt", "/b.txt"),
        )
        assertTrue(state.isAllSelected)
    }

    @Test
    fun `isAllSelected false when some visible files unselected`() {
        val files = listOf(file("a.txt", "/a.txt"), file("b.txt", "/b.txt"))
        val state = FileUiState(
            files = files,
            selection = setOf("/a.txt"),
        )
        assertFalse(state.isAllSelected)
    }

    @Test
    fun `isAllSelected considers only filtered visible files`() {
        // query filters to only "a.txt"; selection contains just the visible one
        val files = listOf(file("a.txt", "/a.txt"), file("b.log", "/b.log"))
        val state = FileUiState(
            files = files,
            query = "a.txt",
            selection = setOf("/a.txt"),
        )
        assertTrue(state.isAllSelected)
    }

    @Test
    fun `isAllSelected false when no visible files`() {
        val files = listOf(file("a.txt", "/a.txt"))
        val state = FileUiState(
            files = files,
            query = "nomatch",
            selection = emptySet(),
        )
        assertFalse(state.isAllSelected)
    }
}
