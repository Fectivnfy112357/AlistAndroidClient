package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Corner
import com.textvision.alistclient.ui.theme.MusicLilac
import com.textvision.alistclient.ui.theme.MusicPink
import com.textvision.alistclient.ui.theme.MusicViolet

/** Pink → purple → violet artistic gradient for the music hero banner. */
private val HeroGradient = Brush.linearGradient(
    listOf(MusicPink, MusicLilac, MusicViolet),
)

/**
 * 音乐横幅 — 180dp gradient banner: "刚刚播放" label + [WaveIndicator] + Fredoka title +
 * round play/favorite/queue buttons. Visual placeholder (no playback).
 */
@Composable
fun MusicHeroCard(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onFavorite: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Corner.ExtraLarge))
            .background(HeroGradient)
            .padding(20.dp),
    ) {
        Column(Modifier.align(Alignment.TopStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "刚刚播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.width(10.dp))
                WaveIndicator(isPlaying = isPlaying, color = Color.White, barHeight = 14.dp)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundControl(
                    icon = if (isPlaying) AppIcons.pause else AppIcons.play,
                    onClick = onPlayPause,
                    filled = true,
                )
                RoundControl(icon = AppIcons.heartFill, onClick = onFavorite)
                RoundControl(icon = AppIcons.queue, onClick = onQueue)
            }
        }
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    val bg = if (filled) Color.White else Color.White.copy(alpha = 0.22f)
    val tint = if (filled) MusicViolet else Color.White
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(name = "MusicHeroCard playing")
@Composable
private fun MusicHeroCardPlayingPreview() {
    MaterialTheme {
        MusicHeroCard(
            title = "夏日海岸",
            subtitle = "云端乐团 · 夏日精选 · 3:42",
            isPlaying = true,
            onPlayPause = {}, onFavorite = {}, onQueue = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "MusicHeroCard paused")
@Composable
private fun MusicHeroCardPausedPreview() {
    MaterialTheme {
        MusicHeroCard(
            title = "午夜电波",
            subtitle = "霓虹计划 · 午夜专辑 · 4:08",
            isPlaying = false,
            onPlayPause = {}, onFavorite = {}, onQueue = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
