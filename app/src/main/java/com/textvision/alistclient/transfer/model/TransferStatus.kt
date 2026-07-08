package com.textvision.alistclient.transfer.model

enum class TransferStatus(
    val canRetry: Boolean,
    val retryLabel: String?,
    val displayName: String,
) {
    Waiting(canRetry = false, retryLabel = null, displayName = "等待中"),
    Uploading(canRetry = false, retryLabel = null, displayName = "上传中"),
    Downloading(canRetry = false, retryLabel = null, displayName = "下载中"),
    Success(canRetry = false, retryLabel = null, displayName = "已完成"),
    Failed(canRetry = true, retryLabel = "重试", displayName = "失败"),
    Cancelled(canRetry = false, retryLabel = null, displayName = "已取消"),
    Interrupted(canRetry = true, retryLabel = "重新传输", displayName = "已中断"),
    ;

    companion object {
        val activeStatuses: Set<TransferStatus> = setOf(Waiting, Uploading, Downloading)
    }
}
