package com.textvision.alistclient.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_artist")
data class ArtistEntity(
    @PrimaryKey val name: String,
    val path: String,
    val albumCount: Int,
    val songCount: Int,
)

@Entity(tableName = "music_album")
data class AlbumEntity(
    val artist: String,
    val name: String,
    val path: String,
    val coverPath: String?,
    val songCount: Int,
) {
    @PrimaryKey
    var id: String = (artist + "" + name).hashCode().toString()
}

@Entity(tableName = "music_song")
data class SongEntity(
    @PrimaryKey val path: String,
    val trackNo: String,
    val trackNoInt: Int?,
    val artist: String,
    val album: String,
    val title: String,
    val lrcPath: String?,
    val coverPath: String?,
    val sizeBytes: Long,
)
