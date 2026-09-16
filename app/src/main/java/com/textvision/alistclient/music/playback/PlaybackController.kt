package com.textvision.alistclient.music.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.textvision.alistclient.debug.TraceMarkers
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode { OFF, ONE, ALL }

data class PlaybackState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val artworkData: ByteArray? = null,
    // D1: surface ExoPlayer's buffering state to the UI. `preparing == true`
    // means the user tapped a track and is waiting for the first audio buffer;
    // `bufferedPercent` is 0..100 of how much of the *current* item's content
    // has been fetched (ExoPlayer exposes this as bufferedPosition / duration).
    val preparing: Boolean = false,
    val bufferedPercent: Int = 0,
)

/**
 * Cheap, side-channel progress tick — emits the current `positionMs` (rounded to a
 * second) without re-allocating the full [PlaybackState]. Compose screens that only
 * need to update the Slider / position Text should subscribe to this so the rest of
 * the PlaybackState (current, isPlaying, repeatMode, shuffle, artworkData) stays
 * referentially stable and short-circuits its observers' recomposition.
 */
data class PlaybackProgress(val positionMs: Long, val durationMs: Long)

internal fun playbackStartState(songs: List<Song>, startIndex: Int): PlaybackState =
    PlaybackState(current = songs.getOrNull(startIndex))

internal fun playbackProgressState(
    state: PlaybackState,
    positionMs: Long,
    durationMs: Long,
): PlaybackState = state.copy(positionMs = positionMs, durationMs = durationMs)

@Singleton
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val indexRepo: MusicIndexRepository,
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress(0L, 0L))
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private var controller: MediaController? = null

    private fun ensureController(onReady: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null && existing.isConnected) {
            onReady(existing)
            return
        }
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                // alist: temporary jank instrumentation, see TraceMarkers.
                // This listener runs on whichever thread completes the binder
                // handshake, so it is an async section, not a B/E pair.
                val cookie = TraceMarkers.begin("playback:connected")
                try {
                    val c = future.get()
                    controller = c
                    onReady(c)
                } finally {
                    TraceMarkers.end("playback:connected", cookie)
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    /**
     * Pre-create the playback Service and ExoPlayer without actually queuing any
     * media. Called early (e.g. when entering the music library) so the first
     * user-initiated `playQueue` doesn't pay the 200-500 ms cold-start cost on
     * top of signing + prepare. Safe to invoke multiple times — once the Service
     * is alive, MediaController is bound and subsequent calls are no-ops.
     */
    fun warmUp(context: Context) {
        // alist: temporary jank instrumentation, see TraceMarkers.
        val cookie = TraceMarkers.begin("playback:warmUp")
        try {
            ensureController { /* no-op: just want the Service up */ }
        } finally {
            TraceMarkers.end("playback:warmUp", cookie)
        }
    }

    fun playQueue(context: Context, songs: List<Song>, startIndex: Int) {
        publishState(playbackStartState(songs, startIndex))
        val paths = songs.map { it.path }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = PlaybackIntents.ACTION_PLAY_QUEUE
            putStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS, ArrayList(paths))
            putExtra(PlaybackIntents.EXTRA_START_INDEX, startIndex)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun togglePlayPause() {
        ensureController { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun next() {
        ensureController { it.seekToNext() }
    }

    fun prev() {
        ensureController { it.seekToPrevious() }
    }

    fun seekTo(positionMs: Long) {
        ensureController { it.seekTo(positionMs) }
    }

    fun toggleShuffle() {
        ensureController { c ->
            // Sole writer strategy: Service.Player.Listener will publish updated shuffle
            // through MediaController state once ExoPlayer acknowledges the change.
            c.shuffleModeEnabled = !c.shuffleModeEnabled
        }
    }

    fun cycleRepeat() {
        ensureController { c ->
            val next = when (c.repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
            }
            c.repeatMode = next
        }
    }

    /**
     * Sole state-writer entrypoint, called by the Service's `Player.Listener` so
     * MediaController commands and playback events agree on a single source of truth.
     * Package-internal to keep callers honest.
     */
    internal fun publishState(update: PlaybackState) {
        _state.value = update
    }

    /**
     * Side-channel progress publisher: pushed by the Service on each tick. The
     * progress flow dedupes by `(positionMs / 1000)` so subscribers see at most
     * one emission per real-time second. Use this for Slider / position text;
     * keep using the main `state` flow for control-state (current, isPlaying,
     * repeat/shuffle, buffering flags) so they only recompose when they change.
     */
    internal fun publishProgress(positionMs: Long, durationMs: Long) {
        val currentSec = _progress.value.positionMs / 1000L
        if (positionMs / 1000L != currentSec) {
            _progress.value = PlaybackProgress(positionMs, durationMs)
        }
    }
}
