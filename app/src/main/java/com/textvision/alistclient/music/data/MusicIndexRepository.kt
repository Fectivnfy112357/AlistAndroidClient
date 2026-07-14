package com.textvision.alistclient.music.data

import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.music.MusicLibraryRootStore
import com.textvision.alistclient.music.data.model.Album
import com.textvision.alistclient.music.data.model.Artist
import com.textvision.alistclient.music.data.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicIndexRepository @Inject constructor(
    private val dao: MusicDao,
    private val scanner: MusicScanner,
    private val rootStore: MusicLibraryRootStore,
    private val sessionManager: SessionManager,
    private val okHttp: OkHttpClient,
    private val signProvider: SignProvider,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val _state = MutableStateFlow<MusicIndexState>(MusicIndexState.NotIndexed)
    val state: StateFlow<MusicIndexState> = _state.asStateFlow()

    suspend fun ensureIndexed() {
        val count = withContext(dispatcher) { dao.songCount() }
        if (count > 0) {
            _state.value = MusicIndexState.Ready
        } else {
            rescan()
        }
    }

    suspend fun rescan() {
        val session = sessionManager.loadSavedSession()
        if (session == null) {
            _state.value = MusicIndexState.Failed("no active session")
            return
        }
        val root = rootStore.rootPath.first()
        try {
            val result = scanner.scan(root, session.serverUrl) { progress ->
                _state.value = MusicIndexState.Scanning(progress.artistsDone, progress.songsFound)
            }
            withContext(dispatcher) {
                dao.clearArtists()
                dao.clearAlbums()
                dao.clearSongs()
                if (result.artists.isNotEmpty()) dao.upsertArtists(result.artists)
                if (result.albums.isNotEmpty()) dao.upsertAlbums(result.albums)
                if (result.songs.isNotEmpty()) dao.upsertSongs(result.songs)
            }
            _state.value = MusicIndexState.Ready
        } catch (t: Throwable) {
            _state.value = MusicIndexState.Failed(t.message ?: "scan failed")
        }
    }

    fun artists(): Flow<List<Artist>> = dao.artists().map { list -> list.map(Artist::fromEntity) }
    fun albumsByArtist(artist: String) = dao.albumsByArtist(artist).map { list -> list.map(Album::fromEntity) }
    fun allAlbums(): Flow<List<Album>> = dao.allAlbums().map { list -> list.map(Album::fromEntity) }
    fun recentAlbums(limit: Int): Flow<List<Album>> = dao.recentAlbums(limit).map { list -> list.map(Album::fromEntity) }
    fun songsByAlbum(artist: String, album: String): Flow<List<Song>> =
        dao.songsByAlbum(artist, album).map { list -> list.map(Song::fromEntity) }
    fun allSongs(): Flow<List<Song>> = dao.allSongs().map { list -> list.map(Song::fromEntity) }

    suspend fun artworkForSong(songPath: String): ByteArray? = withContext(dispatcher) {
        dao.artworkForSong(songPath)
    }

    /**
     * Compose a fresh signed cover URL for [coverPath]. Returns null if the path is null
     * or if sign resolution fails (then the caller should fall back to a placeholder).
     */
    suspend fun coverUrl(coverPath: String?): String? {
        if (coverPath.isNullOrEmpty()) return null
        val session = sessionManager.loadSavedSession() ?: return null
        val sign = signProvider.get(coverPath, SignKind.THUMBNAIL, session.serverUrl)
            ?: return null
        val base = session.serverUrl.trimEnd('/')
        return "$base/p$coverPath?sign=$sign"
    }

    /**
     * Compose a fresh signed download URL for a song audio file.
     * Used by the player when we want to refresh sign before queueing.
     */
    suspend fun downloadUrl(songPath: String): String? {
        val session = sessionManager.loadSavedSession() ?: return null
        val sign = signProvider.get(songPath, SignKind.DOWNLOAD, session.serverUrl)
            ?: return null
        val base = session.serverUrl.trimEnd('/')
        return "$base/d$songPath?sign=$sign"
    }

    /**
     * Resolve a path → Song lookup table for a given list of paths. Used by the Service
     * to translate `currentMediaItem.mediaId` back into a Song for UI rendering.
     */
    suspend fun songMapForPaths(paths: List<String>): List<Song> = withContext(dispatcher) {
        if (paths.isEmpty()) return@withContext emptyList()
        val byPath = allSongs().first().associateBy { it.path }
        paths.mapNotNull { byPath[it] }
    }

    /**
     * Download the lrc body via the shared authenticated OkHttp client (no sign needed —
     * the OkHttpClient already attaches Authorization; the server resolves the path).
     * Returns null on null path, missing session, or HTTP failure.
     */
    suspend fun loadLrcText(lrcPath: String?): String? {
        if (lrcPath.isNullOrEmpty()) return null
        return withContext(dispatcher) {
            val session = sessionManager.loadSavedSession() ?: return@withContext null
            val base = session.serverUrl.trimEnd('/')
            val resp = runCatching {
                okHttp.newCall(
                    Request.Builder().url("$base/d/$lrcPath").build(),
                ).execute()
            }.getOrNull() ?: return@withContext null
            resp.use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
        }
    }
}
