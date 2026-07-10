package com.textvision.alistclient.ui.feature.preview

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.cloudClickable
import kotlinx.coroutines.delay

/**
 * Audio preview — MediaPlayer-backed playback control surface. Renders a disc,
 * playback status, seek slider, time labels, and skip/play controls. Uses
 * AppIcons for control glyphs and theme tokens for all colors.
 */
@Composable
internal fun AudioPreview(url: String) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var isPrepared by remember(url) { mutableStateOf(false) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var durationMs by remember(url) { mutableIntStateOf(0) }
    var positionMs by remember(url) { mutableIntStateOf(0) }
    var isSeeking by remember(url) { mutableStateOf(false) }
    var seekTarget by remember(url) { mutableFloatStateOf(0f) }

    val player = remember(url) { MediaPlayer() }
    DisposableEffect(url) {
        runCatching {
            player.setOnPreparedListener {
                durationMs = it.duration
                isPrepared = true
            }
            player.setOnCompletionListener {
                isPlaying = false
                positionMs = durationMs
            }
            player.setOnErrorListener { _, _, _ ->
                error = "播放失败"
                true
            }
            player.setDataSource(url)
            player.prepareAsync()
        }.onFailure { error = "播放失败" }
        onDispose { player.release() }
    }

    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            runCatching { positionMs = player.currentPosition }
            delay(250)
        }
    }

    AudioPreviewContent(
        isPlaying = isPlaying,
        isPrepared = isPrepared,
        error = error,
        durationMs = durationMs,
        positionMs = positionMs,
        isSeeking = isSeeking,
        seekTarget = seekTarget,
        onSeekChange = {
            isSeeking = true
            seekTarget = it
        },
        onSeekFinished = {
            runCatching {
                player.seekTo(seekTarget.toInt())
                positionMs = seekTarget.toInt()
            }
            isSeeking = false
        },
        onPlayPauseToggle = {
            runCatching {
                if (isPlaying) player.pause() else player.start()
                isPlaying = !isPlaying
            }.onFailure { error = "播放失败" }
        },
        onSkip = { deltaMs ->
            runCatching {
                val current = player.currentPosition
                val target = (current + deltaMs).coerceIn(0, durationMs)
                player.seekTo(target)
                positionMs = target
            }
        },
    )
}

/**
 * Stateless variant — accepts all playback state + callbacks. Extracted for
 * preview / unit testability without instantiating MediaPlayer.
 */
@Composable
internal fun AudioPreviewContent(
    isPlaying: Boolean,
    isPrepared: Boolean,
    error: String?,
    durationMs: Int,
    positionMs: Int,
    isSeeking: Boolean,
    seekTarget: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSkip: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.musicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = error ?: if (!isPrepared) "加载中…" else if (isPlaying) "正在播放" else "已暂停",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        val displayPos = if (isSeeking) seekTarget.toInt() else positionMs
        Slider(
            value = if (durationMs > 0) displayPos.coerceIn(0, durationMs).toFloat() else 0f,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat()),
            enabled = isPrepared && error == null,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(displayPos), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AudioControlButton(
                icon = AppIcons.skipPrev,
                contentDescription = "后退10秒",
                enabled = isPrepared && error == null,
                onClick = { onSkip(-10_000) },
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .cloudClickable(enabled = isPrepared && error == null) {
                        onPlayPauseToggle()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) AppIcons.pause else AppIcons.play,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            AudioControlButton(
                icon = AppIcons.skipNext,
                contentDescription = "前进10秒",
                enabled = isPrepared && error == null,
                onClick = { onSkip(10_000) },
            )
        }
    }
}

@Composable
private fun AudioControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .cloudClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview(name = "AudioPreview Loaded")
@Composable
private fun AudioPreviewLoadedPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        AudioPreviewContent(
            isPlaying = false,
            isPrepared = true,
            error = null,
            durationMs = 215_000,
            positionMs = 42_000,
            isSeeking = false,
            seekTarget = 42_000f,
            onSeekChange = {},
            onSeekFinished = {},
            onPlayPauseToggle = {},
            onSkip = {},
        )
    }
}

@Preview(name = "AudioPreview Playing")
@Composable
private fun AudioPreviewPlayingPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        AudioPreviewContent(
            isPlaying = true,
            isPrepared = true,
            error = null,
            durationMs = 215_000,
            positionMs = 88_000,
            isSeeking = false,
            seekTarget = 88_000f,
            onSeekChange = {},
            onSeekFinished = {},
            onPlayPauseToggle = {},
            onSkip = {},
        )
    }
}

@Preview(name = "AudioPreview Dark")
@Composable
private fun AudioPreviewDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        AudioPreviewContent(
            isPlaying = false,
            isPrepared = false,
            error = null,
            durationMs = 0,
            positionMs = 0,
            isSeeking = false,
            seekTarget = 0f,
            onSeekChange = {},
            onSeekFinished = {},
            onPlayPauseToggle = {},
            onSkip = {},
        )
    }
}