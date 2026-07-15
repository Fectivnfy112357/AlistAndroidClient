package com.textvision.alistclient.ui.feature.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {

    init {
        viewModelScope.launch { indexRepo.ensureIndexed() }
        // A2: pre-create the playback Service so the first `playQueue` click
        // doesn't pay the Service-cold-start + ExoPlayer-construction cost
        // (typically 200-500 ms on a real device, observed as "3-4 seconds of
        // silence" before the first track begins). Connecting the MediaController
        // boots the Service in the background; by the time the user taps a row
        // the player thread is alive and waiting on Intent.ACTION_PLAY_QUEUE.
        playbackController.warmUp(appContext)
    }

    val state: StateFlow<MusicLibraryUiState> = combine(
        indexRepo.state,
        indexRepo.artists(),
        indexRepo.allAlbums(),
        indexRepo.recentAlbums(8),
        indexRepo.allSongs(),
    ) { indexState, artists, albums, recent, songs ->
        val albumArtwork = albums.associate { (it.artist to it.name) to it.artworkData }
        MusicLibraryUiState(
            indexState = UiIndexState.fromDomain(indexState),
            artists = artists.map(UiArtist::fromDomain),
            albums = albums.map(UiAlbum::fromDomain),
            recentAlbums = recent.map(UiAlbum::fromDomain),
            songs = songs.map { song -> UiSong.fromDomain(song, albumArtwork[song.artist to song.album]) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicLibraryUiState(),
    )

    /** Sticky bottom MiniPlayer source — playback state, surfaced for the library screen. */
    val playbackState: StateFlow<PlaybackState> = playbackController.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaybackState(),
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
