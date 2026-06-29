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

    @Test
    fun transferProgressTextShowsPercentAndTransferredSize() {
        val task = TransferEntity(
            id = "1",
            fileName = "alist-500mb-test.bin",
            remotePath = "/alist-500mb-test.bin",
            localPath = null,
            sourceUri = null,
            bytesDone = 2_424_832,
            totalBytes = 524_288_000,
            type = TransferType.Upload,
            status = TransferStatus.Uploading,
            failureReason = null,
            createdAtMillis = 1,
            updatedAtMillis = 2,
        )

        assertEquals("0.5% · 2.3 MB / 500.0 MB", task.progressText)
    }

    @Test
    fun transferListSummaryCountsActiveFailedAndCompletedTasks() {
        val now = 1L
        val transfers = listOf(
            TransferEntity("1", "upload.bin", "/upload.bin", null, null, 0, 100, TransferType.Upload, TransferStatus.Uploading, null, now, now),
            TransferEntity("2", "download.zip", "/download.zip", null, null, 0, 100, TransferType.Download, TransferStatus.Downloading, null, now, now),
            TransferEntity("3", "bad.pdf", "/bad.pdf", null, null, 0, 100, TransferType.Download, TransferStatus.Failed, "网络错误", now, now),
            TransferEntity("4", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now),
        )

        val state = TransferListUiState(transfers)

        assertEquals("2 个进行中 · 1 个失败 · 1 个完成", state.summaryText)
    }

    @Test
    fun activeFailedAndCompletedTransfersExposeCompactActionState() {
        val now = 1L
        val uploading = TransferEntity("1", "upload.bin", "/upload.bin", null, null, 0, 100, TransferType.Upload, TransferStatus.Uploading, null, now, now)
        val failed = TransferEntity("2", "bad.pdf", "/bad.pdf", null, null, 0, 100, TransferType.Download, TransferStatus.Failed, "网络错误", now, now)
        val completed = TransferEntity("3", "done.jpg", "/done.jpg", null, null, 100, 100, TransferType.Download, TransferStatus.Success, null, now, now)

        assertEquals("取消", uploading.primaryActionLabel)
        assertEquals("重试", failed.primaryActionLabel)
        assertEquals(null, completed.primaryActionLabel)
        assertEquals(true, completed.isComplete)
    }
}