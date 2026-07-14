package com.textvision.alistclient.ui.feature.music

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
    val currentLineIndex: Int = -1,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val indexRepo: MusicIndexRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val rawLyrics = MutableStateFlow<List<LrcLine>>(emptyList())

    init {
        viewModelScope.launch {
            // Reload LRC only when the LRC path actually changes — `positionMs` ticks every
            // 100ms would otherwise trigger a download per frame.
            playbackController.state
                .map { it.current?.lrcPath }
                .distinctUntilChanged()
                .collect { lrcPath ->
                    rawLyrics.value = if (lrcPath != null) {
                        LrcParser.parse(indexRepo.loadLrcText(lrcPath).orEmpty())
                    } else {
                        emptyList()
                    }
                }
        }
    }

    val state: StateFlow<MusicPlayerUiState> = combine(
        playbackController.state,
        rawLyrics,
    ) { playback, lyrics ->
        val index = binarySearchCurrentLine(lyrics, playback.positionMs)
        MusicPlayerUiState(playback, lyrics, index)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicPlayerUiState(),
    )

    private fun binarySearchCurrentLine(lines: List<LrcLine>, positionMs: Long): Int {
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

    fun onTogglePlayPause() = playbackController.togglePlayPause()
    fun onPrev() = playbackController.prev()
    fun onNext() = playbackController.next()
    fun onSeekTo(ms: Long) = playbackController.seekTo(ms)
    fun onToggleShuffle() = playbackController.toggleShuffle()
    fun onCycleRepeat() = playbackController.cycleRepeat()
}
