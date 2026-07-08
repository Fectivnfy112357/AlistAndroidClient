package com.textvision.alistclient.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeIconTest {

    @Test
    fun image_mime_returns_image_category() {
        assertEquals(FileCategory.IMAGE, fileCategoryFromMime("image/png", "photo.png"))
        assertEquals(FileCategory.IMAGE, fileCategoryFromMime("image/jpeg", "wallpaper.jpg"))
    }

    @Test
    fun video_mime_returns_video_category() {
        assertEquals(FileCategory.VIDEO, fileCategoryFromMime("video/mp4", "movie.mp4"))
        assertEquals(FileCategory.VIDEO, fileCategoryFromMime("video/webm", "clip.webm"))
    }

    @Test
    fun pdf_mime_returns_pdf_category() {
        assertEquals(FileCategory.PDF, fileCategoryFromMime("application/pdf", "report.pdf"))
    }

    @Test
    fun code_extension_returns_code_category() {
        // null mime, fall back to extension
        assertEquals(FileCategory.CODE, fileCategoryFromMime(null, "MainActivity.kt"))
        assertEquals(FileCategory.CODE, fileCategoryFromMime(null, "script.py"))
    }

    @Test
    fun archive_extension_returns_archive_category() {
        assertEquals(FileCategory.ARCHIVE, fileCategoryFromMime(null, "bundle.zip"))
        assertEquals(FileCategory.ARCHIVE, fileCategoryFromMime(null, "backup.tar.gz"))
    }

    @Test
    fun text_mime_returns_text_category() {
        assertEquals(FileCategory.TEXT, fileCategoryFromMime("text/plain", "notes.txt"))
        assertEquals(FileCategory.TEXT, fileCategoryFromMime("text/markdown", "README.md"))
    }

    @Test
    fun unknown_returns_other_category() {
        assertEquals(FileCategory.OTHER, fileCategoryFromMime(null, null))
        assertEquals(FileCategory.OTHER, fileCategoryFromMime("application/x-unknown", "mystery.unknownext"))
    }
}
