package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMint

/**
 * 艺人卡 — 84dp circular [CoverLetter] + artist name + song count.
 * Visual placeholder (no playback).
 */
@Composable
fun ArtistCard(
    name: String,
    count: Int,
    gradient: Brush,
    artworkData: ByteArray? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArtworkCover(
            name = name,
            artworkData = artworkData,
            gradient = gradient,
            size = 84.dp,
            shape = CircleShape,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "$count 首",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "ArtistCard")
@Composable
private fun ArtistCardPreview() {
    MaterialTheme {
        ArtistCard(
            name = "云端乐团",
            count = 42,
            gradient = Brush.linearGradient(listOf(CandyMint, Brand500)),
            modifier = Modifier.padding(16.dp),
        )
    }
}
