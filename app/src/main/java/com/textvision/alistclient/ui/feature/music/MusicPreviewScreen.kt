package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.music.playback.PlaybackProgress
import com.textvision.alistclient.ui.components.music.CoverLetter
import com.textvision.alistclient.ui.feature.music.components.LyricsView
import com.textvision.alistclient.ui.feature.music.components.PlayerControls
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.MusicMagenta
import com.textvision.alistclient.ui.theme.MusicViolet

/** Shared, stable gradient reference for the preview page artwork. */
private val PreviewCoverGradient: Brush = Brush.linearGradient(listOf(CandyPink, MusicMagenta, MusicViolet))

@Composable
fun MusicPreviewScreen(
    onBack: () -> Unit = {},
    viewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val chrome by viewModel.chromeState.collectAsStateWithLifecycle()
    val current = chrome.current
    val coverGradient = PreviewCoverGradient

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppTopBar(title = "正在播放", subtitle = current?.album.orEmpty(), onBack = onBack)
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.extraLarge),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(coverGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    ArtworkLayer(
                        title = current?.title ?: "音乐",
                        artworkData = chrome.artworkData,
                        gradient = coverGradient,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = current?.title ?: "尚未选择歌曲",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = listOfNotNull(current?.artist, current?.album).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            PlayerProgressSection(viewModel = viewModel, currentPath = current?.path)
            PlayerControls(
                isPlaying = chrome.isPlaying,
                shuffleEnabled = chrome.shuffle,
                repeatMode = chrome.repeatMode,
                preparing = chrome.preparing && !chrome.isPlaying,
                onPlayPause = viewModel::onTogglePlayPause,
                onPrev = viewModel::onPrev,
                onNext = viewModel::onNext,
                onToggleShuffle = viewModel::onToggleShuffle,
                onCycleRepeat = viewModel::onCycleRepeat,
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text("歌词", modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    PlayerLyricsSection(viewModel)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PlayerProgressSection(viewModel: MusicPlayerViewModel, currentPath: String?) {
    var draggingTo by remember(currentPath) { mutableStateOf<Float?>(null) }
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    SliderWithTimeRow(
        progress = progress,
        draggingTo = draggingTo,
        onValueChange = { draggingTo = it },
        onValueChangeFinished = { value -> value?.toLong()?.let(viewModel::onSeekTo) },
        onDraggingCleared = { draggingTo = null },
    )
}

@Composable
private fun PlayerLyricsSection(viewModel: MusicPlayerViewModel) {
    val lines by viewModel.lyrics.collectAsStateWithLifecycle()
    val currentLineIndex by viewModel.currentLineIndex.collectAsStateWithLifecycle()
    LyricsView(lines = lines, currentIndex = currentLineIndex, height = 280.dp)
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun produceArtworkBitmap(artworkData: ByteArray?): State<androidx.compose.ui.graphics.ImageBitmap?> {
    val state = remember(artworkData) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(artworkData) {
        state.value = if (artworkData == null) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size, bounds)
            val options = android.graphics.BitmapFactory.Options().apply {
                var sample = 1
                while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                inSampleSize = sample
            }
            android.graphics.BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size, options)
                ?.asImageBitmap()
        }
    }
    return state
}

/**
 * Slider + position/duration row — extracted so it owns its own recomposition
 * scope. PlaybackController publishes progress on a separate flow (1 Hz when
 * rounded to a second), so only this subtree rewrites per tick; the rest of
 * the page (controls, title, lyrics) stays stable.
 */
@Composable
private fun SliderWithTimeRow(
    progress: PlaybackProgress,
    draggingTo: Float?,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float?) -> Unit,
    onDraggingCleared: () -> Unit,
) {
    val duration = progress.durationMs.coerceAtLeast(1L).toFloat()
    val displayed = (draggingTo ?: progress.positionMs.toFloat()).coerceIn(0f, duration)
    Column {
        Slider(
            value = displayed,
            valueRange = 0f..duration,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                onValueChangeFinished(draggingTo)
                onDraggingCleared()
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(displayed.toLong()), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(progress.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArtworkLayer(
    title: String,
    artworkData: ByteArray?,
    gradient: Brush,
) {
    // Decoded off the UI thread; show the gradient placeholder until the bitmap
    // is ready. This stops a 256-px JPEG decode from hitching the preview page
    // (which is also where the user just tapped a song).
    val bitmap by produceArtworkBitmap(artworkData)
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "专辑封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        CoverLetter(
            name = title,
            gradient = gradient,
            size = 164.dp,
            modifier = Modifier.clip(RoundedCornerShape(36.dp)),
        )
    }
}
