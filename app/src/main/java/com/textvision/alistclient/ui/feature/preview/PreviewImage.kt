package com.textvision.alistclient.ui.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

/**
 * Image preview surface. Renders the bitmap with a bottom gradient overlay
 * containing title (file name) + subtitle (resolution · size). Wrapped in a
 * 18dp rounded card per prototype §5.2.4.
 */
@Composable
internal fun ImagePreview(
    url: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFFFFD1DD),
                        androidx.compose.ui.graphics.Color(0xFFFFE4ED),
                        androidx.compose.ui.graphics.Color(0xFFFFC4D6),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        ImageOverlay(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Bottom gradient strip — transparent → black@45%, white 11px title + 10px subtitle.
 * Tokens via theme: `Color.Black.copy(alpha = 0.45f)` is the only acceptable dark
 * surface color (overlay foreground is white per prototype §5.2.4).
 */
@Composable
private fun ImageOverlay(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.45f),
                    ),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Preview(name = "ImagePreview Light")
@Composable
private fun ImagePreviewLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        ImagePreview(
            url = "https://example.com/sample.jpg",
            title = "海岸线日落.jpg",
            subtitle = "4032 × 3024 · 2.4 MB",
            modifier = Modifier.padding(PaddingValues(16.dp)),
        )
    }
}

@Preview(name = "ImagePreview Dark")
@Composable
private fun ImagePreviewDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        ImagePreview(
            url = "https://example.com/sample.jpg",
            title = "海岸线日落.jpg",
            subtitle = "4032 × 3024 · 2.4 MB",
            modifier = Modifier.padding(PaddingValues(16.dp)),
        )
    }
}

@Preview(name = "ImagePreview Large Text")
@Composable
private fun ImagePreviewLargePreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        ImagePreview(
            url = "https://example.com/sample.jpg",
            title = "海岸线日落.jpg",
            subtitle = "4032 × 3024 · 2.4 MB",
            modifier = Modifier.padding(PaddingValues(16.dp)),
        )
    }
}