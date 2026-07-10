package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.textvision.alistclient.ui.theme.BgEnd
import com.textvision.alistclient.ui.theme.BgStart
import com.textvision.alistclient.ui.theme.Brand200
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink

/**
 * Vertical sky-blue gradient background — login / hero / 165° screen base.
 */
@Composable
fun SkyBlueBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgStart, BgEnd),
                    startY = 0f,
                    endY = 1200f,
                ),
            ),
    )
}

/**
 * Lightweight cloud decoration — drawn in top 280dp, simulated with translucent circles.
 */
@Composable
fun CloudDecor(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = 140f,
                center = Offset(60f, 20f),
            )
            drawCircle(
                color = Brand200.copy(alpha = 0.55f),
                radius = 120f,
                center = Offset(300f, 30f),
            )
            drawCircle(
                color = CandyPink.copy(alpha = 0.7f),
                radius = 4f,
                center = Offset(40f, 80f),
            )
            drawCircle(
                color = CandyMint.copy(alpha = 0.8f),
                radius = 3f,
                center = Offset(320f, 110f),
            )
        }
    }
}

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
