package com.textvision.alistclient.transfer

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
}
