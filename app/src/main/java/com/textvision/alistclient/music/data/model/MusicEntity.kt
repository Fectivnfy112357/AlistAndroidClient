package com.textvision.alistclient.music.data.model

import com.textvision.alistclient.music.data.AlbumEntity
import com.textvision.alistclient.music.data.ArtistEntity
import com.textvision.alistclient.music.data.SongEntity

data class Artist(
    val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
    val artworkData: ByteArray? = null,
) {
    companion object {
        fun fromEntity(e: ArtistEntity) = Artist(e.name, e.path, e.albumCount, e.songCount, e.artworkData)
    }
}

data class Album(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
    val artworkData: ByteArray? = null,
) {
    companion object {
        fun fromEntity(e: AlbumEntity) = Album(e.artist, e.name, e.path, e.coverPath, e.songCount, e.artworkData)
    }
}

data class Song(
    val path: String,
    val trackNo: String,
    val trackNoInt: Int?,
    val artist: String,
    val album: String,
    val title: String,
    val lrcPath: String?,
    val coverPath: String?,
    val sizeBytes: Long,
) {
    companion object {
        fun fromEntity(e: SongEntity) = Song(
            e.path, e.trackNo, e.trackNoInt, e.artist, e.album,
            e.title, e.lrcPath, e.coverPath, e.sizeBytes,
        )
    }
}
