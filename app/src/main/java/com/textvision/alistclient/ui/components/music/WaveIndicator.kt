package com.textvision.alistclient.ui.components.music

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 波形指示器 — `barCount` vertical bars that pulse (scaleY) when [isPlaying].
 * When paused the bars sit at a low static height. Purely decorative.
 */
@Composable
fun WaveIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: androidx.compose.ui.unit.Dp = 3.dp,
    barHeight: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        modifier = modifier.height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600 + index * 120),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar-$index",
            )
            val barScale = if (isPlaying) scale else 0.4f
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight()
                    .scale(scaleX = 1f, scaleY = barScale)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Preview(name = "WaveIndicator playing")
@Composable
private fun WaveIndicatorPlayingPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) { WaveIndicator(isPlaying = true) }
    }
}

@Preview(name = "WaveIndicator paused")
@Composable
private fun WaveIndicatorPausedPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) { WaveIndicator(isPlaying = false) }
    }
}

@Preview(name = "WaveIndicator 6 bars")
@Composable
private fun WaveIndicatorSixPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) { WaveIndicator(isPlaying = true, barCount = 6) }
    }
}
