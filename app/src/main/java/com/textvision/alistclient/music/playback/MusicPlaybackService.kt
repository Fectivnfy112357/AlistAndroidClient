package com.textvision.alistclient.music.playback

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        val items = paths.mapNotNull { path ->
            val signed = indexRepository.downloadUrl(path) ?: return@mapNotNull null
            // Keep the path as mediaId so Player.Listener.onMediaItemTransition
            // can resolve back into a Song via pathToSong.
            MediaItem.Builder().setUri(signed).setMediaId(path).build()
        }
        if (items.isEmpty()) return
        val player = exoPlayer ?: return
        val allSongs = indexRepository.songMapForPaths(paths)
        pathToSong = allSongs.associateBy { it.path }
        val firstIndex = startIndex.coerceIn(0, items.lastIndex)
        player.setMediaItems(items, firstIndex, 0L)
        player.prepare()
        player.playWhenReady = true
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
        }

        override fun onPlaybackStateChanged(state: Int) {
            val player = exoPlayer ?: return
            playbackController.publishState(
                playbackController.state.value.copy(
                    durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                    positionMs = player.currentPosition,
                ),
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
}
