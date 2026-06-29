package com.textvision.alistclient.transfer

import android.os.Environment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDownloadNamerTest {
    @Test fun sameFileNameDifferentPathDoesNotCollide() {
        val a = LocalDownloadNamer.fileNameFor("/a/file.zip")
        val b = LocalDownloadNamer.fileNameFor("/b/file.zip")
        assertNotEquals(a, b)
        assertTrue(a.endsWith(".zip"))
        assertTrue(b.endsWith(".zip"))
    }

    @Test fun publicDownloadsRelativePathUsesAlistFolder() {
        assertEquals(
            "${Environment.DIRECTORY_DOWNLOADS}/alist",
            LocalDownloadNamer.publicDownloadsRelativePath,
        )
    }
}
