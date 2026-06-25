package com.textvision.alistclient.ui.screens

import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferScreenTest {
    @Test
    fun emptyTransferListShowsEmptyState() {
        val state = TransferListUiState(transfers = emptyList())

        assertEquals("暂无传输任务", state.emptyMessage)
        assertTrue(state.shouldShowEmptyState)
    }

    @Test
    fun failedTransferStatusTextIncludesReasonAndRetryLabel() {
        val task = TransferEntity(
            id = "1",
            fileName = "video.mp4",
            remotePath = "/video.mp4",
            localPath = null,
            sourceUri = null,
            bytesDone = 10,
            totalBytes = 100,
            type = TransferType.Download,
            status = TransferStatus.Failed,
            failureReason = "网络错误",
            createdAtMillis = 1,
            updatedAtMillis = 2,
        )

        assertEquals("失败：网络错误", task.statusText)
        assertTrue(task.showRetry)
        assertEquals("重试", task.retryButtonLabel)
    }
}
