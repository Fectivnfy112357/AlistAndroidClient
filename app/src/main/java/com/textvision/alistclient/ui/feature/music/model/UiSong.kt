package com.textvision.alistclient.ui.feature.music.model

import com.textvision.alistclient.music.data.model.Album as DomainAlbum
import com.textvision.alistclient.music.data.model.Artist as DomainArtist
import com.textvision.alistclient.music.data.model.Song

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
) {
    companion object {
        fun fromDomain(s: Song) = UiSong(
            path = s.path,
            title = s.title,
            artist = s.artist,
            album = s.album,
            trackNo = s.trackNo,
            trackNoInt = s.trackNoInt,
            coverPath = s.coverPath,
            lrcPath = s.lrcPath,
            sizeBytes = s.sizeBytes,
        )
    }
}

data class UiAlbum(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
) {
    companion object {
        fun fromDomain(a: DomainAlbum) = UiAlbum(
            a.artist, a.name, a.path, a.coverPath, a.songCount,
        )
    }
}

data class UiArtist(
    val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
) {
    companion object {
        fun fromDomain(a: DomainArtist) = UiArtist(
            a.name, a.path, a.albumCount, a.songCount,
        )
    }
}
