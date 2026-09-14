package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AppMotion
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMint

/** Default mint→brand gradient for the active transfer progress. */
val TransferProgressDefaultBrush: Brush =
    Brush.horizontalGradient(listOf(CandyMint, Brand500))

/**
 * Animated progress indicator with optional gradient brush.
 * Defaults to a mint→brand gradient (upload/download accent).
 *
 * P0 fix: the previous implementation layered two `Box`s where the inner one
 * used `Modifier.fillMaxWidth(animated)` to size the fill. That re-ran the
 * measure pass on every animation frame because the width depends on a
 * `MutableFloatState`. We now do the fill with [Modifier.drawBehind] (draw
 * phase, no measure) and only redraw when the animated value, brush, or
 * track color changes.
 */
@Composable
fun TransferProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    brush: Brush = TransferProgressDefaultBrush,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.SpringFast,
        label = "transferProgress",
    )
    val shape = remember(height) { RoundedCornerShape(height / 2) }
    val track = trackColor
    val fill = brush

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .drawBehind {
                drawTrackAndFill(track, fill, animated)
            },
    )
}

/** Pure draw block — extracted so the lambda is inlinable and the closure
 *  doesn't re-capture unrelated state on every frame. */
private fun DrawScope.drawTrackAndFill(track: Color, fill: Brush, fraction: Float) {
    drawRect(color = track, size = size)
    if (fraction > 0f) {
        drawRect(
            brush = fill,
            size = Size(size.width * fraction, size.height),
        )
    }
}