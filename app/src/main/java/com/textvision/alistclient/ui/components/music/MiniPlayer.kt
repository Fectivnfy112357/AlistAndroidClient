package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
 * 迷你播放器 — bottom docked bar: 38dp [CoverLetter] + name + artist + [WaveIndicator] +
 * play button. Global placeholder shown across the 5 main screens; taps do nothing.
 */
@Composable
fun MiniPlayer(
    name: String,
    artist: String,
    gradient: Brush,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverLetter(name = name, gradient = gradient, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
            WaveIndicator(isPlaying = isPlaying, barHeight = 14.dp)
            Spacer(Modifier.width(4.dp))
            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) AppIcons.pause else AppIcons.play,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "MiniPlayer playing")
@Composable
private fun MiniPlayerPlayingPreview() {
    MaterialTheme {
        MiniPlayer(
            name = "夏日海岸",
            artist = "云端乐团",
            gradient = Brush.linearGradient(listOf(CandyPink, Brand500)),
            isPlaying = true,
            onPlayPause = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "MiniPlayer paused")
@Composable
private fun MiniPlayerPausedPreview() {
    MaterialTheme {
        MiniPlayer(
            name = "午夜电波",
            artist = "霓虹计划",
            gradient = Brush.linearGradient(listOf(CandyPink, Brand500)),
            isPlaying = false,
            onPlayPause = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
