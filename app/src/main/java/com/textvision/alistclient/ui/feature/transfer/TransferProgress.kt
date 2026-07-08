package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudMotion

@Composable
fun TransferProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = CloudMotion.SpringFast,
        label = "transferProgress",
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.fillMaxWidth().height(4.dp),
    )
}
