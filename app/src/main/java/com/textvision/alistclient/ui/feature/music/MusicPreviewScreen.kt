package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.feature.music.components.LyricsView
import com.textvision.alistclient.ui.feature.music.components.PlayerControls
import com.textvision.alistclient.ui.foundation.AppTopBar

@Composable
fun MusicPreviewScreen(
    onBack: () -> Unit = {},
    viewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val current = ui.playback.current

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppTopBar(title = "正在播放", subtitle = current?.album.orEmpty(), onBack = onBack)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Cover placeholder — Coil load is a future enhancement once the cover URL
                // provider (Task 8) is plumbed into this screen.
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                ) {
                    Text(
                        text = current?.title?.take(1) ?: "♪",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = current?.title ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = current?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                var draggingTo by remember(current?.path) { mutableStateOf<Float?>(null) }
                val duration = ui.playback.durationMs.coerceAtLeast(1L).toFloat()
                val displayed = (draggingTo ?: ui.playback.positionMs.toFloat())
                    .coerceIn(0f, duration)
                Slider(
                    value = displayed,
                    valueRange = 0f..duration,
                    onValueChange = { draggingTo = it },
                    onValueChangeFinished = {
                        draggingTo?.toLong()?.let(viewModel::onSeekTo)
                        draggingTo = null
                    },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(formatTime(displayed.toLong()), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatTime(ui.playback.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PlayerControls(
                    isPlaying = ui.playback.isPlaying,
                    shuffleEnabled = ui.playback.shuffle,
                    repeatMode = ui.playback.repeatMode,
                    onPlayPause = viewModel::onTogglePlayPause,
                    onPrev = viewModel::onPrev,
                    onNext = viewModel::onNext,
                    onToggleShuffle = viewModel::onToggleShuffle,
                    onCycleRepeat = viewModel::onCycleRepeat,
                )
                Spacer(Modifier.height(24.dp))
                LyricsView(lines = ui.lyrics, currentIndex = ui.currentLineIndex)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
