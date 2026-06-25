package com.textvision.alistclient.transfer.model

enum class TransferStatus(val canRetry: Boolean, val retryLabel: String?) {
    Waiting(canRetry = false, retryLabel = null),
    Uploading(canRetry = false, retryLabel = null),
    Downloading(canRetry = false, retryLabel = null),
    Success(canRetry = false, retryLabel = null),
    Failed(canRetry = true, retryLabel = "重试"),
    Cancelled(canRetry = false, retryLabel = null),
    Interrupted(canRetry = true, retryLabel = "重新传输"),
}
