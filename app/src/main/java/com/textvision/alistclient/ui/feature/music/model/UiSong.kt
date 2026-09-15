package com.textvision.alistclient.ui.feature.music.model

import androidx.compose.runtime.Stable
import com.textvision.alistclient.music.data.model.Album as DomainAlbum
import com.textvision.alistclient.music.data.model.Artist as DomainArtist
import com.textvision.alistclient.music.data.model.Song

// ByteArray-bearing types are marked @Stable (not @Immutable) — Compose may
// still skip recomposition when other fields are equal, and we trust the
// generated `equals` (which compares ByteArray contents) for correctness.
@Stable
data class UiSong(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val trackNo: String,
    val trackNoInt: Int?,
    val coverPath: String?,
    val lrcPath: String?,
    val sizeBytes: Long,
    val artworkData: ByteArray? = null,
) {
    companion object {
        fun fromDomain(s: Song, artworkData: ByteArray? = null) = UiSong(
            path = s.path,
            title = s.title,
            artist = s.artist,
            album = s.album,
            trackNo = s.trackNo,
            trackNoInt = s.trackNoInt,
            coverPath = s.coverPath,
            lrcPath = s.lrcPath,
            sizeBytes = s.sizeBytes,
            artworkData = artworkData,
        )
    }
}

@Stable
data class UiAlbum(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
    val artworkData: ByteArray? = null,
) {
    companion object {
        fun fromDomain(a: DomainAlbum) = UiAlbum(
            a.artist, a.name, a.path, a.coverPath, a.songCount, a.artworkData,
        )
    }
}

@Stable
data class UiArtist(
    val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
    val artworkData: ByteArray? = null,
) {
    companion object {
        fun fromDomain(a: DomainArtist) = UiArtist(
            a.name, a.path, a.albumCount, a.songCount, a.artworkData,
        )
    }
}
