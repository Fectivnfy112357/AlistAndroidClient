package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AppMotion
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMint

/**
 * Animated progress indicator with optional gradient brush.
 * Defaults to a mint→brand gradient (upload/download accent).
 */
@Composable
fun TransferProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    brush: Brush = Brush.horizontalGradient(listOf(CandyMint, Brand500)),
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.SpringFast,
        label = "transferProgress",
    )
    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(height)
                    .clip(shape)
                    .background(brush),
            )
        }
    }
}