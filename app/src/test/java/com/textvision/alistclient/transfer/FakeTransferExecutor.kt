package com.textvision.alistclient.transfer

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred

/**
 * Test double for [TransferExecutor] that simulates a long-running IO operation
 * which completes only when [cancel] is invoked. Replaces the original
 * `BlockingOkHttpClient.execute()` busy-spin that caused order-dependent
 * failures in the full test suite.
 */
class FakeTransferExecutor : TransferExecutor {
    private val cancelGate = CompletableDeferred<Unit>()

    /** How many times runDownload was invoked. */
    var downloadCount: Int = 0
        private set
    /** How many times runUpload was invoked. */
    var uploadCount: Int = 0
        private set
    /** Last displayName passed to runDownload — for assertion. */
    var lastDownloadDisplayName: String? = null
        private set

    /** Releases the pending download/upload to return [TransferOutcome.Cancelled]. */
    fun cancel() {
        cancelGate.complete(Unit)
    }

    override suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome {
        downloadCount++
        lastDownloadDisplayName = displayName
        cancelGate.await()
        return if (isActive()) TransferOutcome.Success else TransferOutcome.Cancelled
    }

    override suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend () -> Boolean,
    ): TransferOutcome {
        uploadCount++
        cancelGate.await()
        return if (isActive()) TransferOutcome.Success else TransferOutcome.Cancelled
    }

    override fun cancel(id: String) {
        cancelGate.complete(Unit)
    }

    override fun cancelAll() {
        cancelGate.complete(Unit)
    }

    override fun activeCallCount(): Int = if (cancelGate.isCompleted) 0 else 1
}