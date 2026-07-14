package com.textvision.alistclient.ui.components.music

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun coverLabel(name: String): String =
    name.firstOrNull { it.isLetter() }?.toString()?.uppercase() ?: "♪"

/**
 * 居中首字封面 — gradient background + first Unicode character of `name` rendered large.
 */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
) {
    val firstChar = coverLabel(name)
    val fontSize = (size.value * 0.52f).sp
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}

@Composable
fun ArtworkCover(
    name: String,
    artworkData: ByteArray?,
    gradient: Brush,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val artwork = remember(artworkData) {
        artworkData?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    if (artwork == null) {
        CoverLetter(name, gradient, size, modifier)
    } else {
        Image(
            bitmap = artwork,
            contentDescription = "$name 封面",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(MaterialTheme.shapes.small),
        )
    }
}
