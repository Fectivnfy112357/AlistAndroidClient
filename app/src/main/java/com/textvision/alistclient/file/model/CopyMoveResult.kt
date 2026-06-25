package com.textvision.alistclient.file.model

data class CopyMoveResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val firstFailure: Pair<String, String>? = null,
    val stopped: Boolean,
)
