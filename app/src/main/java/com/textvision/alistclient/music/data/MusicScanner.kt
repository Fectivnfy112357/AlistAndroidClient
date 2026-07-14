package com.textvision.alistclient.music.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistFileDto
import com.textvision.alistclient.network.dto.FsListRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(
    val artists: List<ArtistEntity>,
    val albums: List<AlbumEntity>,
    val songs: List<SongEntity>,
)

data class ScanProgress(
    val artistsDone: Int,
    val songsFound: Int,
) {
    companion object {
        val Empty = ScanProgress(0, 0)
    }
}

@Singleton
class MusicScanner @Inject constructor(
    private val api: AlistApi,
    private val signProvider: SignProvider? = null,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    constructor(api: AlistApi, dispatcher: CoroutineDispatcher) : this(api, null, dispatcher)

    private val audioExtensions = setOf(
        "mp3", "flac", "m4a", "wav", "ogg", "aac", "ape", "wma",
    )
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    /**
     * Compose a stable path for an entry returned from `fs/list`. The Alist
     * `path` field is absent from the per-file record, so we rebuild it
     * deterministically from the parent directory + entry name.
     */
    private fun filePath(parent: String, entry: AlistFileDto): String {
        val normalizedParent = parent.trimEnd('/')
        return if (normalizedParent.isEmpty()) "/${entry.name}" else "$normalizedParent/${entry.name}"
    }

    suspend fun scan(
        rootPath: String,
        baseUrl: String,
        onProgress: suspend (ScanProgress) -> Unit = {},
    ): ScanResult = withContext(dispatcher) {
        val normalizedBase = baseUrl.trimEnd('/')
        val (artistsRaw, _) = listSafely(normalizedBase, rootPath)
        if (artistsRaw.isEmpty()) {
            return@withContext ScanResult(emptyList(), emptyList(), emptyList())
        }

        val sem = Semaphore(4)
        val albumRows = mutableListOf<AlbumEntity>()
        val songRows = mutableListOf<SongEntity>()
        var songsFound = 0
        var artistsDone = 0

        val artistsScanned = coroutineScope {
            artistsRaw.map { artistDir ->
                async {
                    sem.withPermit {
                        scanArtist(normalizedBase, rootPath, artistDir).also { partial ->
                            albumRows += partial.albums
                            songRows += partial.songs
                            artistsDone++
                            songsFound += partial.songs.size
                            onProgress(ScanProgress(artistsDone, songsFound))
                        }
                    }
                }
            }.awaitAll()
        }

        val artistMap = songRows.groupBy { it.artist }
            .mapValues { entry -> entry.value.size }
        val albumCountByArtist = albumRows.groupBy { it.artist }
            .mapValues { entry -> entry.value.size }
        val artistRows = artistsRaw.map { dir ->
            val artistArtwork = albumRows.firstOrNull {
                it.artist == dir.name && it.artworkData != null
            }?.artworkData
            ArtistEntity(
                name = dir.name,
                path = filePath(rootPath, dir),
                albumCount = albumCountByArtist[dir.name] ?: 0,
                songCount = artistMap[dir.name] ?: 0,
                artworkData = artistArtwork,
            )
        }
        ScanResult(artistRows, albumRows, songRows)
    }

    private data class ArtistScan(
        val albums: List<AlbumEntity>,
        val songs: List<SongEntity>,
    )

    private suspend fun scanArtist(
        base: String,
        rootPath: String,
        artistDir: AlistFileDto,
    ): ArtistScan {
        val artistPath = filePath(rootPath, artistDir)
        val (albumsRaw, _) = listSafely(base, artistPath)
        val albumsHere = mutableListOf<AlbumEntity>()
        val songsHere = mutableListOf<SongEntity>()
        albumsRaw.forEach { albumDir ->
            val albumPath = filePath(artistPath, albumDir)
            val (files, _) = listSafely(base, albumPath)
            val cover = files.firstOrNull { entry ->
                !entry.isDir &&
                    entry.name.substringBeforeLast('.', "").equals("cover", ignoreCase = true) &&
                    imageExtensions.contains(entry.extensionLower())
            }?.let { filePath(albumPath, it) }
            val lrcs = files
                .filter { !it.isDir && it.extensionLower() == "lrc" }
                .associate { it.name.substringBeforeLast('.') to filePath(albumPath, it) }
            val albumSongs = files.filter { !it.isDir && audioExtensions.contains(it.extensionLower()) }
            albumSongs.forEach { audioFile ->
                    val parsed = parseFileName(audioFile.name) ?: return@forEach
                    val lrcPath = lrcs[audioFile.name.substringBeforeLast('.')]
                    songsHere += SongEntity(
                        path = filePath(albumPath, audioFile),
                        trackNo = parsed.trackNo,
                        trackNoInt = parsed.trackNoInt,
                        artist = artistDir.name,
                        album = albumDir.name,
                        title = parsed.title,
                        lrcPath = lrcPath,
                        coverPath = cover,
                        sizeBytes = audioFile.size,
                    )
                }
            val artworkData = albumSongs.firstNotNullOfOrNull { audioFile ->
                extractArtwork(base, filePath(albumPath, audioFile))
            }
            albumsHere += AlbumEntity(
                artist = artistDir.name,
                name = albumDir.name,
                path = albumPath,
                coverPath = cover,
                songCount = songsHere.count { it.artist == artistDir.name && it.album == albumDir.name },
                artworkData = artworkData,
            )
        }
        return ArtistScan(albumsHere, songsHere)
    }

    private suspend fun listSafely(base: String, path: String): Pair<List<AlistFileDto>, Throwable?> {
        return try {
            val resp = api.list("$base/api/fs/list", FsListRequest(path = path))
            if (resp.code == 200 && resp.data != null) {
                resp.data.content to null
            } else {
                emptyList<AlistFileDto>() to IllegalStateException("code=${resp.code}")
            }
        } catch (t: Throwable) {
            emptyList<AlistFileDto>() to t
        }
    }

    private suspend fun extractArtwork(baseUrl: String, audioPath: String): ByteArray? {
        val sign = signProvider?.get(audioPath, SignKind.DOWNLOAD, baseUrl) ?: return null
        val source = "${baseUrl.trimEnd('/')}/d$audioPath?sign=$sign"
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(source, emptyMap())
            retriever.embeddedPicture?.let(::compressArtwork)
        } catch (_: Throwable) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun compressArtwork(bytes: ByteArray): ByteArray? {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scale = (MAX_ARTWORK_EDGE.toFloat() / maxOf(source.width, source.height)).coerceAtMost(1f)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        } else source
        return java.io.ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, output)
            if (bitmap !== source) bitmap.recycle()
            output.toByteArray()
        }
    }

    internal data class Parsed(val trackNo: String, val trackNoInt: Int?, val title: String)

    /**
     * Parse names in the form `01-周杰伦-晴天.mp3`. Split by '-' with limit=3
     * yields exactly 3 parts: trackNo, artist, title (extension stripped first).
     * Returns null if pattern does not match.
     */
    internal fun parseFileName(rawName: String): Parsed? {
        val stem = rawName.substringBeforeLast('.', missingDelimiterValue = rawName)
        val parts = stem.split("-", limit = 3)
        if (parts.size != 3) return null
        val trackNo = parts[0].trim()
        // artist = parts[1] is authoritative (matches directory name); title only = parts[2].
        val title = parts[2].trim()
        if (trackNo.isEmpty() || title.isEmpty()) return null
        val trackNoInt = trackNo.toIntOrNull()
        return Parsed(trackNo, trackNoInt, title)
    }

    private fun AlistFileDto.extensionLower(): String =
        name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    private companion object {
        const val MAX_ARTWORK_EDGE = 256
        const val ARTWORK_QUALITY = 82
    }
}
