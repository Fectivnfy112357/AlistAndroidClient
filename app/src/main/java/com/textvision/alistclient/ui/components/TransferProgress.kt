package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft

@Composable
fun TransferProgress(bytesDone: Long, totalBytes: Long) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
    if (totalBytes > 0 && totalBytes >= bytesDone) {
        val progress = (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = CloudPrimary,
            trackColor = CloudPrimarySoft,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = CloudPrimary,
            trackColor = CloudPrimarySoft,
        )
    }
}
