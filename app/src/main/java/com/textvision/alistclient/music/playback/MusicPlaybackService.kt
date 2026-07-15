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
            serviceScope.launch { playQueue(paths, startIndex) }
        }
        return START_STICKY
    }

    private suspend fun playQueue(paths: List<String>, startIndex: Int) {
        if (paths.isEmpty()) return
        // Progressive queue strategy:
        //
        // - Build the *full* ExoPlayer queue (length = paths.size) immediately so
        //   indices match what the user expects: tapping the 30th song queues 30..end
        //   and the player plays them in order, not "preBuffer then tail".
        // - Only paths[startIndex] (current) and paths[startIndex+1] (next) get signed
        //   synchronously. ExoPlayer.prepare() then begins streaming the current and
        //   pre-buffers the next from local cache where present.
        // - Remaining slots are filled by the background `fillTailSlots` pass. Each
        //   slot is replaced in place via replaceMediaItem so indices stay aligned.
        //
        // Historically this method signed all N paths before any setMediaItems call,
        // which was the dominant cold-start cost for a queue of 100+ songs.
        val playIndex = startIndex.coerceIn(0, paths.lastIndex)
        val preBufferEnd = (playIndex + FASTER_BUFFER).coerceAtMost(paths.lastIndex)
        val preBufferRange = playIndex..preBufferEnd
        val preBufferPaths = preBufferRange.map { paths[it] }

        // Sign pre-buffer window + load path → Song rows for them.
        val sem = Semaphore(SIGN_CONCURRENCY)
        val (preBufferItems, preBufferSongs) = coroutineScope {
            val itemsDeferred = async {
                preBufferPaths.map { path ->
                    async {
                        sem.withPermit {
                            val signed = indexRepository.downloadUrl(path) ?: return@withPermit null
                            MediaItem.Builder().setUri(signed).setMediaId(path).build()
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            val songsDeferred = async { indexRepository.songMapForPaths(preBufferPaths) }
            itemsDeferred.await() to songsDeferred.await()
        }
        if (preBufferItems.isEmpty()) return
        val player = exoPlayer ?: return

        // Build full queue with placeholder uris ("about:pending") and stitch the
        // pre-buffer signed items into their proper slots. Tail signing pass
        // replaces the placeholders in place via replaceMediaItem.
        val stitched: List<MediaItem> = List(paths.size) { idx ->
            val path = paths[idx]
            if (idx in preBufferRange) {
                preBufferItems.getOrNull(idx - playIndex)
                    ?: MediaItem.Builder().setMediaId(path).setUri("about:pending").build()
            } else {
                MediaItem.Builder().setMediaId(path).setUri("about:pending").build()
            }
        }
        pathToSong = pathToSong + preBufferSongs.associateBy { it.path }

        player.setMediaItems(stitched, playIndex, 0L)
        player.prepare()
        player.playWhenReady = true

        // Background: fill the rest of the queue slots. earlier (paths before playIndex)
        // and later (paths after preBufferEnd) are signed concurrently with the same
        // semaphore throttling the player build used.
        val earlierPaths = if (playIndex > 0) paths.subList(0, playIndex).toList() else emptyList()
        val laterPaths = if (preBufferEnd + 1 < paths.size) paths.subList(preBufferEnd + 1, paths.size).toList() else emptyList()
        if (earlierPaths.isNotEmpty() || laterPaths.isNotEmpty()) {
            serviceScope.launch {
                fillTailSlots(earlierPaths, laterStart = preBufferEnd + 1, later = laterPaths)
            }
        }
    }

    private suspend fun fillTailSlots(earlier: List<String>, laterStart: Int, later: List<String>) {
        val sem = Semaphore(SIGN_CONCURRENCY)
        coroutineScope {
            earlier.forEachIndexed { offset, path ->
                launch {
                    val signed = sem.withPermit { indexRepository.downloadUrl(path) } ?: return@launch
                    applyTailMediaItem(path, signed, playerIndex = offset)
                }
            }
            later.forEachIndexed { offset, path ->
                launch {
                    val signed = sem.withPermit { indexRepository.downloadUrl(path) } ?: return@launch
                    applyTailMediaItem(path, signed, playerIndex = laterStart + offset)
                }
            }
        }
    }

    private fun applyTailMediaItem(path: String, signed: String, playerIndex: Int) {
        val player = exoPlayer ?: return
        if (playerIndex < 0 || playerIndex >= player.mediaItemCount) return
        if (player.currentMediaItemIndex == playerIndex) return
        val current = player.getMediaItemAt(playerIndex)
        if (current.localConfiguration?.uri.toString() != "about:pending") return
        val item = MediaItem.Builder().setUri(signed).setMediaId(path).build()
        runCatching { player.replaceMediaItem(playerIndex, item) }
        // Best-effort: refresh pathToSong lazily on transition (Player.Listener
        // already does song lookup on onMediaItemTransition by querying the
        // MusicIndexRepository, so we don't need to merge eagerly here).
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                val player = exoPlayer ?: break
                if (!player.isPlaying) break
                publishProgress(player)
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun publishProgress(player: Player) {
        playbackController.publishState(
            playbackProgressState(
                state = playbackController.state.value,
                positionMs = player.currentPosition,
                durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            ),
        )
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
            // D1: surface buffering state so the preview page can show a
            // "loading…" placeholder instead of appearing unresponsive. STATE_READY
            // means audio is actually playing; STATE_BUFFERING means the player has
            // a MediaItem loaded but is still fetching bytes.
            val preparing = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
            val bufferedPercent = if (player.duration > 0) {
                ((player.bufferedPosition.coerceAtLeast(0).toDouble() / player.duration) * 100.0)
                    .toInt().coerceIn(0, 100)
            } else 0
            playbackController.publishState(
                playbackController.state.value.copy(
                    preparing = preparing,
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
        const val PROGRESS_UPDATE_MS = 500L
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
