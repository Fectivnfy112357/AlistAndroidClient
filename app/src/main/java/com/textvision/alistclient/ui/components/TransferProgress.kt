package com.textvision.alistclient.ui.components

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable

@Composable
fun TransferProgress(bytesDone: Long, totalBytes: Long) {
    if (totalBytes > 0 && totalBytes >= bytesDone) {
        val progress = (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progress })
    } else {
        LinearProgressIndicator()
    }
}
