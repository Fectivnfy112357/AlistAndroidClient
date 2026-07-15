package com.textvision.alistclient.music.playback

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.music.data.MusicIndexRepository
import com.textvision.alistclient.music.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var indexRepository: MusicIndexRepository
    @Inject lateinit var musicCache: MusicCache
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var sessionManager: SessionManager

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // IO scope is for signing/IO-bound work only — never call ExoPlayer API from
    // here, since ExoPlayer mutation must happen on the looper it was constructed
    // on (the main looper in this Service). Using IO for signing keeps the main
    // thread free during cold start so the Compose CircularProgressIndicator
    // doesn't freeze in place — the UI shows a working spinner instead of a
    // stuck dot.
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pathToSong: Map<String, Song> = emptyMap()
    private var progressJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(musicCache.cacheDataSourceFactory),
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)
        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        progressJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == PlaybackIntents.ACTION_PLAY_QUEUE) {
            val paths = intent.getStringArrayListExtra(PlaybackIntents.EXTRA_SONG_PATHS) ?: return START_NOT_STICKY
            val startIndex = intent.getIntExtra(PlaybackIntents.EXTRA_START_INDEX, 0)
            android.util.Log.i("MusicPerf", "onStartCommand PLAY_QUEUE n=${paths.size} start=$startIndex ${System.nanoTime()}")
            serviceScope.launch { playQueue(paths, startIndex) }
        }
        return START_NOT_STICKY
    }

    private suspend fun playQueue(paths: List<String>, startIndex: Int) {
        if (paths.isEmpty()) return
        val tStart = System.nanoTime()
        android.util.Log.i("MusicPerf", "playQueue start n=${paths.size} idx=$startIndex")
        // Two-stage start:
        //
        // Stage 1 (synchronous, this method): sign only the first song + sign+queue
        // any extra "fast lane" item so the user hears audio within a couple of
        // hundred ms even on a cold Service start. setMediaItems gets a single
        // MediaItem (or two) — ExoPlayer prepares immediately and starts streaming.
        //
        // Stage 2 (background): sign and `addMediaItem` the rest of the queue in
        // `serviceScope` at SIGN_CONCURRENCY parallelism. Each append is cheap;
        // ExoPlayer transparently transitions to the next item when the current
        // one ends. We do NOT use placeholder MediaItems with "about:pending"
        // uris because ExoPlayer probes those on `setMediaItems` and ends up in
        // STATE_BUFFERING while waiting on an invalid uri — making the UI look
        // stuck on "缓冲中" even though the first track is actually playing.
        //
        // Signing runs on ioScope so the main thread stays free while awaiting
        // downloadUrl / songMapForPaths — that way the Compose spinner keeps
        // animating instead of freezing for the duration of network IO.
        val playIndex = startIndex.coerceIn(0, paths.lastIndex)
        val fastLaneEnd = (playIndex + FASTER_BUFFER).coerceAtMost(paths.lastIndex)
        val firstSlice = (playIndex..fastLaneEnd).map { paths[it] }
        val tailStart = fastLaneEnd + 1
        val tailPaths: List<String> = if (tailStart < paths.size) paths.subList(tailStart, paths.size).toList() else emptyList()
        val earlierPaths: List<String> = if (playIndex > 0) paths.subList(0, playIndex).toList() else emptyList()

        val sem = Semaphore(SIGN_CONCURRENCY)
        val (firstItems, firstSongs) = coroutineScope {
            val itemsDeferred = async(ioScope.coroutineContext) {
                firstSlice.map { path ->
                    async {
                        sem.withPermit {
                            val signed = indexRepository.downloadUrl(path) ?: return@withPermit null
                            MediaItem.Builder().setUri(signed).setMediaId(path).build()
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            val songsDeferred = async(ioScope.coroutineContext) {
                indexRepository.songMapForPaths(firstSlice)
            }
            itemsDeferred.await() to songsDeferred.await()
        }
        if (firstItems.isEmpty()) return
        val player = exoPlayer ?: return
        pathToSong = pathToSong + firstSongs.associateBy { it.path }

        player.setMediaItems(firstItems, 0, 0L)
        player.prepare()
        player.playWhenReady = true
        android.util.Log.i("MusicPerf", "playQueue setMediaItems+prepare done elapsed=${(System.nanoTime()-tStart)/1_000_000}ms")

        // Stage 2: lazy-load the rest of the queue. Earlier songs go in front
        // (used by repeat-all wrap-around); later songs get appended to the
        // tail. We process them in a single pass and let ExoPlayer handle
        // whatever playback order the user has configured (shuffle, repeat).
        if (earlierPaths.isNotEmpty() || tailPaths.isNotEmpty()) {
            serviceScope.launch { enqueueTail(sem, earlierPaths, tailPaths) }
        }
    }

    private suspend fun enqueueTail(sem: Semaphore, earlier: List<String>, later: List<String>) {
        // Signing happens on ioScope; the ExoPlayer.addMediaItem step must stay
        // on main (via serviceScope), which is what serviceScope.launch wraps.
        coroutineScope {
            earlier.forEach { path ->
                launch(ioScope.coroutineContext) {
                    val signed = sem.withPermit { indexRepository.downloadUrl(path) } ?: return@launch
                    val item = MediaItem.Builder().setUri(signed).setMediaId(path).build()
                    withContext(Dispatchers.Main) { exoPlayer?.addMediaItem(0, item) }
                    val songs = indexRepository.songMapForPaths(listOf(path))
                    songs.firstOrNull()?.let { pathToSong = pathToSong + (path to it) }
                }
            }
            later.forEach { path ->
                launch(ioScope.coroutineContext) {
                    val signed = sem.withPermit { indexRepository.downloadUrl(path) } ?: return@launch
                    val item = MediaItem.Builder().setUri(signed).setMediaId(path).build()
                    withContext(Dispatchers.Main) { exoPlayer?.addMediaItem(item) }
                    val songs = indexRepository.songMapForPaths(listOf(path))
                    songs.firstOrNull()?.let { pathToSong = pathToSong + (path to it) }
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            // Throttle to 250 ms; the PlaybackController.progress flow dedupes
            // identical-second ticks so callers see at most 1 emission / sec.
            while (isActive) {
                val player = exoPlayer ?: break
                if (!player.isPlaying) break
                publishProgress(player)
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun publishProgress(player: Player) {
        val pos = player.currentPosition
        val dur = player.duration.takeIf { it > 0 } ?: 0L
        // ── perf fix ─────────────────────────────────────────────────────
        // Only patch the PlaybackState when the duration actually changed;
        // position is consumed by the side-channel flow (PlaybackController.progress)
        // so we don't churn the canonical state and don't force every observer
        // to recompose on each tick.
        val current = playbackController.state.value
        if (current.durationMs != dur) {
            playbackController.publishState(current.copy(durationMs = dur))
        }
        playbackController.publishProgress(pos, dur)
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val path = mediaItem?.mediaId.orEmpty()
            playbackController.publishState(
                playbackController.state.value.copy(
                    current = pathToSong[path],
                    durationMs = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L,
                ),
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playbackController.publishState(playbackController.state.value.copy(isPlaying = isPlaying))
            if (isPlaying) startProgressUpdates() else progressJob?.cancel()
        }

        override fun onPlaybackStateChanged(state: Int) {
            val player = exoPlayer ?: return
            publishProgress(player)
            // D1: surface buffering state so the preview page can replace the
            // play glyph with a spinner while the first track is being fetched.
            //
            // We treat "preparing" as: a track has been queued (current != null)
            // but audio hasn't actually started, and we can't yet tell the user
            // the duration. That catches both ExoPlayer's STATE_BUFFERING
            // windows and the brief gap between `setMediaItems`/`prepare` and
            // `onIsPlayingChanged(true)`, which is when the user most needs a
            // visual cue that the app hasn't frozen.
            val durationMs = player.duration.takeIf { it > 0 } ?: 0L
            val bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
            val bufferedPercent = if (durationMs > 0) {
                ((bufferedMs.toDouble() / durationMs) * 100.0).toInt().coerceIn(0, 100)
            } else 0
            val hasCurrent = player.currentMediaItem != null
            val isPlaying = player.isPlaying
            val actuallyBuffering = hasCurrent && !isPlaying && (
                state == Player.STATE_BUFFERING ||
                    state == Player.STATE_IDLE ||
                    durationMs == 0L
                )
            playbackController.publishState(
                playbackController.state.value.copy(
                    preparing = actuallyBuffering,
                    bufferedPercent = bufferedPercent,
                ),
            )
        }

        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            playbackController.publishState(
                playbackController.state.value.copy(artworkData = mediaMetadata.artworkData),
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            // D1: an error means we are no longer "preparing"; clear the flag so
            // the preview UI can show its own error state instead of a stuck spinner.
            playbackController.publishState(
                playbackController.state.value.copy(preparing = false),
            )
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            playbackController.publishState(playbackController.state.value.copy(repeatMode = mapped))
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            playbackController.publishState(
                playbackController.state.value.copy(shuffle = shuffleModeEnabled),
            )
        }
    }

    private companion object {
        // 250 ms → 4 Hz. Smooth enough for the Slider / progress text, doesn't
        // flood the Compose snapshot system with state replacements.
        const val PROGRESS_UPDATE_MS = 250L
        // Cap concurrent signed-URL fetches so a large queue (e.g. 200 songs) doesn't
        // open 200 parallel HTTP requests against the Alist server. SignProvider already
        // memoises results for TTL_MS, so the steady-state cost is small.
        const val SIGN_CONCURRENCY = 4
        // How many extra songs (beyond the start song) are signed synchronously so
        // ExoPlayer can pre-buffer the next track. Keeps the cold-start synchronous
        // cost bounded (≤ 2 sign calls when cached) while ensuring gapless transition.
        const val FASTER_BUFFER = 1
    }
}
