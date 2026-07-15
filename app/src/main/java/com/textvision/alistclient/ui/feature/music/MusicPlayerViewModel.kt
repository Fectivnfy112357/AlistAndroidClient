package com.textvision.alistclient.ui.feature.music

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.music.data.LrcParser
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.LrcLine
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicPlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val lyrics: List<LrcLine> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val indexRepo: MusicIndexRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val rawLyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    private val rawArtwork = MutableStateFlow<ByteArray?>(null)

    init {
        Log.i("MusicPerf", "MusicPlayerViewModel.init START ${System.nanoTime()}")
        viewModelScope.launch {
            // Reload LRC only when the LRC path actually changes — `positionMs` ticks every
            // 100ms would otherwise trigger a download per frame.
            playbackController.state
                .map { it.current?.lrcPath }
                .distinctUntilChanged()
                .collect { lrcPath ->
                    val t0 = System.nanoTime()
                    rawLyrics.value = if (lrcPath != null) {
                        val text = indexRepo.loadLrcText(lrcPath).orEmpty()
                        val parsed = LrcParser.parse(text)
                        Log.i("MusicPerf", "loadLrcText ${lrcPath} took ${(System.nanoTime()-t0)/1_000_000}ms lines=${parsed.size}")
                        parsed
                    } else {
                        emptyList()
                    }
                }
        }
        viewModelScope.launch {
            playbackController.state.map { it.current?.path }.distinctUntilChanged().collect { path ->
                val t0 = System.nanoTime()
                rawArtwork.value = if (path != null) indexRepo.artworkForSong(path) else null
                val bytes = rawArtwork.value?.size ?: 0
                Log.i("MusicPerf", "artworkForSong ${path} took ${(System.nanoTime()-t0)/1_000_000}ms bytes=$bytes")
            }
        }
        Log.i("MusicPerf", "MusicPlayerViewModel.init DONE ${System.nanoTime()}")
    }

    val state: StateFlow<MusicPlayerUiState> = combine(
        playbackController.state,
        rawLyrics,
        rawArtwork,
    ) { playback, lyrics, artwork ->
        MusicPlayerUiState(playback.copy(artworkData = artwork ?: playback.artworkData), lyrics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicPlayerUiState(),
    )

    /**
     * Side-channel position/duration flow. Composables that only need the
     * slider/position text should read this instead of `state` so the rest of
     * the page (title, controls, lyrics) doesn't recompose on each tick.
     */
    val progress: StateFlow<com.textvision.alistclient.music.playback.PlaybackProgress> =
        playbackController.progress

    val currentLineIndex: StateFlow<Int> = combine(rawLyrics, progress) { lyrics, playbackProgress ->
        currentLyricLineIndex(lyrics, playbackProgress.positionMs)
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = -1,
    )

    fun onTogglePlayPause() = playbackController.togglePlayPause()
    fun onPrev() = playbackController.prev()
    fun onNext() = playbackController.next()
    fun onSeekTo(ms: Long) = playbackController.seekTo(ms)
    fun onToggleShuffle() = playbackController.toggleShuffle()
    fun onCycleRepeat() = playbackController.cycleRepeat()
}

internal fun currentLyricLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var result = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].timeMs <= positionMs) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}
