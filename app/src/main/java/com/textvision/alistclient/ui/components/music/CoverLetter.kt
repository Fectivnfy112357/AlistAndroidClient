package com.textvision.alistclient.ui.components.music

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun coverLabel(name: String): String =
    name.firstOrNull { it.isLetter() }?.toString()?.uppercase() ?: "♪"

/** Default rounded shape for album/artist artwork — larger than shapes.small for a modern look. */
internal val CoverShape: Shape = RoundedCornerShape(14.dp)

/**
 * 居中首字封面 — gradient background + first Unicode character of `name` rendered large.
 * Sizing is controlled by the caller via [modifier] (fixed size or fill/aspectRatio); the
 * letter scales to the measured box so it looks right at any dimension.
 */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) {
    val firstChar = coverLabel(name)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        val edge = minOf(maxWidth, maxHeight)
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = (edge.value * 0.5f).sp,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}

/** Fixed-size convenience overload. */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) = CoverLetter(name, gradient, modifier.size(size), shape)

/**
 * Artwork cover sized by [modifier] (caller controls fixed size or fill/aspectRatio).
 * Falls back to a [CoverLetter] when [artworkData] is null or fails to decode.
 */
@Composable
fun ArtworkCover(
    name: String,
    artworkData: ByteArray?,
    gradient: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) {
    val artwork = remember(artworkData) {
        artworkData?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    if (artwork == null) {
        CoverLetter(name, gradient, modifier, shape)
    } else {
        Image(
            bitmap = artwork,
            contentDescription = "$name 封面",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    }
}

/** Fixed-size convenience overload. */
@Composable
fun ArtworkCover(
    name: String,
    artworkData: ByteArray?,
    gradient: Brush,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) = ArtworkCover(name, artworkData, gradient, modifier.size(size), shape)
