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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
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
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.cloudClickable
import kotlinx.coroutines.delay

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

    // 定时刷新播放进度
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            runCatching { positionMs = player.currentPosition }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 封面圆盘
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
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
            onValueChange = {
                isSeeking = true
                seekTarget = it
            },
            onValueChangeFinished = {
                runCatching {
                    player.seekTo(seekTarget.toInt())
                    positionMs = seekTarget.toInt()
                }
                isSeeking = false
            },
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
                icon = Icons.Filled.Replay10,
                contentDescription = "后退10秒",
                enabled = isPrepared && error == null,
                onClick = {
                    runCatching {
                        val target = (player.currentPosition - 10_000).coerceAtLeast(0)
                        player.seekTo(target)
                        positionMs = target
                    }
                },
            )
            // 主播放/暂停按钮
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .cloudClickable(enabled = isPrepared && error == null) {
                        runCatching {
                            if (isPlaying) player.pause() else player.start()
                            isPlaying = !isPlaying
                        }.onFailure { error = "播放失败" }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
            AudioControlButton(
                icon = Icons.Filled.Forward10,
                contentDescription = "前进10秒",
                enabled = isPrepared && error == null,
                onClick = {
                    runCatching {
                        val target = (player.currentPosition + 10_000).coerceAtMost(durationMs)
                        player.seekTo(target)
                        positionMs = target
                    }
                },
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