package com.textvision.alistclient.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransferManagerSourceTest {
    @Test
    fun downloadsUsePublicMediaStoreDownloadsInsteadOfPrivateFilesDir() {
        // Post-refactor: MediaStore.IO lives in RealTransferExecutor, not TransferManager.
        val managerSource = File("src/main/java/com/textvision/alistclient/transfer/TransferManager.kt").readText()
        val executorSource = File("src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt").readText()

        assertFalse(managerSource.contains("File(context.filesDir, \"downloads\")"))
        assertTrue(executorSource.contains("MediaStore.Downloads.EXTERNAL_CONTENT_URI"))
        assertTrue(executorSource.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
    }
}
