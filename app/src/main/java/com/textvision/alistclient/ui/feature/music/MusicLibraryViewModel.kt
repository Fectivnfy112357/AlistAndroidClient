package com.textvision.alistclient.ui.feature.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import com.textvision.alistclient.music.playback.PlaybackController
import com.textvision.alistclient.music.playback.PlaybackState
import com.textvision.alistclient.ui.feature.music.dto.UiIndexState
import com.textvision.alistclient.ui.feature.music.model.UiAlbum
import com.textvision.alistclient.ui.feature.music.model.UiArtist
import com.textvision.alistclient.ui.feature.music.model.UiSong
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicLibraryUiState(
    val indexState: UiIndexState = UiIndexState.NotIndexed,
    val artists: List<UiArtist> = emptyList(),
    val albums: List<UiAlbum> = emptyList(),
    val recentAlbums: List<UiAlbum> = emptyList(),
    val songs: List<UiSong> = emptyList(),
)

internal fun visibleItemCount(total: Int, requested: Int): Int =
    requested.coerceIn(0, total)

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val indexRepo: MusicIndexRepository,
    private val playbackController: PlaybackController,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    init {
        viewModelScope.launch { indexRepo.ensureIndexed() }
        // A2: pre-create the playback Service so the first `playQueue` click
        // doesn't pay the Service-cold-start + ExoPlayer-construction cost
        // (typically 200-500 ms on a real device, observed as "3-4 seconds of
        // silence" before the first track begins). Connecting the MediaController
        // boots the Service in the background; by the time the user taps a row
        // the player thread is alive and waiting on Intent.ACTION_PLAY_QUEUE.
        android.util.Log.i("MusicPerf", "LibraryVM warmUp begin ${System.nanoTime()}")
        playbackController.warmUp(appContext)
        android.util.Log.i("MusicPerf", "LibraryVM warmUp invoked (non-blocking) ${System.nanoTime()}")
    }

    val state: StateFlow<MusicLibraryUiState> = combine(
        indexRepo.state,
        indexRepo.artists().distinctUntilChanged(),
        indexRepo.allAlbums().distinctUntilChanged(),
        indexRepo.recentAlbums(8).distinctUntilChanged(),
        indexRepo.allSongs().distinctUntilChanged(),
    ) { indexState, artists, albums, recent, songs ->
        val albumArtwork = albums.associate { (it.artist to it.name) to it.artworkData }
        MusicLibraryUiState(
            indexState = UiIndexState.fromDomain(indexState),
            artists = artists.map(UiArtist::fromDomain),
            albums = albums.map(UiAlbum::fromDomain),
            recentAlbums = recent.map(UiAlbum::fromDomain),
            songs = songs.map { song -> UiSong.fromDomain(song, albumArtwork[song.artist to song.album]) },
        )
    }.flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MusicLibraryUiState(),
        )

    /**
     * Mini player source. We expose ONLY the fields the mini player reads
     * (current song + isPlaying), as a small immutable record. Previously this
     * forwarded the full PlaybackState — including `positionMs`, which ticks
     * every 250 ms and forced the entire library screen (including Lazy grids)
     * to recompose. The mini player doesn't show position, so skipping it
     * here removes the biggest churn source.
     */
    data class MiniPlayerState(val current: Song?, val isPlaying: Boolean)

    val playbackState: StateFlow<MiniPlayerState> = playbackController.state
        .map { MiniPlayerState(it.current, it.isPlaying) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MiniPlayerState(null, false),
        )

    fun onRescanClick() {
        viewModelScope.launch { indexRepo.rescan() }
    }

    fun onTogglePlayPause() = playbackController.togglePlayPause()

    fun onPlayQueueClick(context: Context, songs: List<UiSong>, index: Int) {
        val domainSongs = songs.map { s ->
            Song(
                path = s.path,
                trackNo = s.trackNo,
                trackNoInt = s.trackNoInt,
                artist = s.artist,
                album = s.album,
                title = s.title,
                lrcPath = s.lrcPath,
                coverPath = s.coverPath,
                sizeBytes = s.sizeBytes,
            )
        }
        playbackController.playQueue(context, domainSongs, index)
    }
}
