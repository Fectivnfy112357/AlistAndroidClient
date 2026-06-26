package com.textvision.alistclient.file

import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.network.dto.AlistFileDto
import com.textvision.alistclient.network.dto.toFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileDtoMappingTest {
    @Test fun mapsFolderPathFromParentAndName() {
        val item = AlistFileDto(name = "docs", isDir = true, fileType = 0).toFileItem("/", "http://s/")
        assertEquals("/docs", item.path)
        assertTrue(item.isDir)
        assertEquals(FileType.Folder, item.type)
    }

    @Test fun mapsFileExtensionAndThumbnailUrl() {
        val item = AlistFileDto(name = "cat.JPG", size = 10, thumb = "abc", sign = "def").toFileItem("/photos", "http://s/")
        assertEquals("/photos/cat.JPG", item.path)
        assertFalse(item.isDir)
        assertEquals("jpg", item.extension)
        assertEquals(FileType.Image, item.type)
        assertEquals("http://s/p//photos/cat.JPG?sign=abc", item.thumbnailUrl)
        assertEquals("http://s/d//photos/cat.JPG?sign=def", item.downloadUrl)
    }

    @Test fun toleratesMissingDateAndSize() {
        val item = AlistFileDto(name = "unknown.bin").toFileItem("/", "http://s/")
        assertEquals(0L, item.size)
        assertNull(item.modifiedAt)
    }

    @Test fun mapsAlistTypeZeroFileAsFileWhenIsDirIsFalse() {
        val item = AlistFileDto(name = "alist-500mb-test-host.bin", size = 524288000, isDir = false, fileType = 0).toFileItem("/我的文件", "http://s/")

        assertFalse(item.isDir)
        assertEquals(FileType.Other, item.type)
        assertEquals(524288000L, item.size)
    }
}
