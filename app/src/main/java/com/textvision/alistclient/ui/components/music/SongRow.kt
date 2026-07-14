package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyPink

/**
 * 歌曲行 — compact row: 42dp [CoverLetter] + name + artist + duration + more icon.
 * When [isPlaying] the row uses a primaryContainer background and the name uses the
 * primary color. Visual placeholder (no playback).
 */
@Composable
fun SongRow(
    name: String,
    artist: String,
    duration: String,
    gradient: Brush,
    artworkData: ByteArray? = null,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val bg = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtworkCover(name, artworkData, gradient, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { onMore?.invoke() }) {
                Icon(
                    imageVector = AppIcons.more,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(name = "SongRow default")
@Composable
private fun SongRowPreview() {
    MaterialTheme {
        SongRow(
            name = "夏日海岸",
            artist = "云端乐团",
            duration = "3:42",
            gradient = Brush.linearGradient(listOf(CandyPink, Brand500)),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "SongRow playing")
@Composable
private fun SongRowPlayingPreview() {
    MaterialTheme {
        SongRow(
            name = "午夜电波",
            artist = "霓虹计划",
            duration = "4:08",
            gradient = Brush.linearGradient(listOf(CandyPink, Brand500)),
            isPlaying = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
