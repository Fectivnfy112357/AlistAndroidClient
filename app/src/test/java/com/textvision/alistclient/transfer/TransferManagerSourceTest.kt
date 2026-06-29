package com.textvision.alistclient.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferManagerSourceTest {
    @Test
    fun downloadsUsePublicMediaStoreDownloadsInsteadOfPrivateFilesDir() {
        val source = File("src/main/java/com/textvision/alistclient/transfer/TransferManager.kt").readText()

        assertTrue(source.contains("MediaStore.Downloads.EXTERNAL_CONTENT_URI"))
        assertTrue(source.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
        assertFalse(source.contains("File(context.filesDir, \"downloads\")"))
    }
}
