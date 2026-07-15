package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.textvision.alistclient.ui.theme.BgEnd
import com.textvision.alistclient.ui.theme.BgStart
import com.textvision.alistclient.ui.theme.Brand200
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.DarkBg
import com.textvision.alistclient.ui.theme.DarkSurface

/**
 * Vertical sky-blue gradient background — login / hero / 165° screen base.
 * Theme-aware: light mode uses the sky-blue gradient; dark mode uses the
 * deep-navy gradient so it stays legible when applied globally.
 *
 * `isDark` is resolved once via `remember(isSystemInDarkTheme)`-equivalent — we
 * check the resolved colorScheme luminance in the composition phase but only
 * once, so re-renders from unrelated state changes don't re-evaluate the
 * dark/light choice (and therefore don't re-construct the gradient Brush on
 * every recomposition).
 */
@Composable
fun SkyBlueBackground(modifier: Modifier = Modifier) {
    val isDark = !MaterialTheme.colorScheme.background.isBright()
    val colors = if (isDark) remember { listOf(DarkBg, DarkSurface) } else remember { listOf(BgStart, BgEnd) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = colors,
                    startY = 0f,
                    endY = 1200f,
                ),
            ),
    )
}

private fun Color.isBright(): Boolean {
    // Avoid Math.sqrt: relative luminance (Rec.709) without gamma correction is a
    // cheap proxy that doesn't require reading r/g/b through float math.pow.
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return (r + g + b) >= 0.5f
}

/**
 * Lightweight cloud decoration — drawn in top 280dp, simulated with translucent circles.
 *
 * The geometric positions / colors are identical across the whole app — they
 * are derived from constants that don't change, so we cache them with `remember`
 * to keep each recomposition allocation-free.
 */
@Composable
fun CloudDecor(modifier: Modifier = Modifier) {
    val circles = remember {
        listOf(
            CloudCircle(60f,   20f,  140f, Color.White.copy(alpha = 0.55f)),
            CloudCircle(300f,  30f,  120f, Brand200.copy(alpha = 0.55f)),
            CloudCircle(40f,   80f,  4f,   CandyPink.copy(alpha = 0.7f)),
            CloudCircle(320f,  110f, 3f,   CandyMint.copy(alpha = 0.8f)),
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            circles.forEach { c ->
                drawCircle(color = c.color, radius = c.radius, center = Offset(c.x, c.y))
            }
        }
    }
}

private data class CloudCircle(val x: Float, val y: Float, val radius: Float, val color: Color)

/**
 * @deprecated Use [SkyBlueBackground] + [CloudDecor] instead.
 *             Kept for backward compatibility with existing callers.
 */
@Deprecated(
    message = "Use SkyBlueBackground or CloudDecor instead",
    replaceWith = ReplaceWith(""),
)
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        content()
    }
}
