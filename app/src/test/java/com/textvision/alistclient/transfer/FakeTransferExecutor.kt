package com.textvision.alistclient.transfer

import android.net.Uri

/**
 * Test double for [TransferExecutor]. Full cancel/IO-deadlock simulation lives
 * here in Task 2; this stub exists only to satisfy the compiler for Task 1.
 */
class FakeTransferExecutor : TransferExecutor {
    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = TransferOutcome.Success

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome = TransferOutcome.Success

    override fun cancel(id: String) = Unit
}
