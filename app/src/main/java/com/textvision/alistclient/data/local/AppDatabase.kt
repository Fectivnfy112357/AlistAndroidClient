package com.textvision.alistclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.textvision.alistclient.music.data.AlbumEntity
import com.textvision.alistclient.music.data.ArtistEntity
import com.textvision.alistclient.music.data.MusicDao
import com.textvision.alistclient.music.data.SongEntity
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity

@Database(
    entities = [
        SmokeEntity::class,
        TransferEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        SongEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
    abstract fun transferDao(): TransferDao
    abstract fun musicDao(): MusicDao
}
