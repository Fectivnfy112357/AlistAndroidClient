package com.textvision.alistclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.textvision.alistclient.transfer.data.TransferDao
import com.textvision.alistclient.transfer.data.TransferEntity

@Database(
    entities = [SmokeEntity::class, TransferEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
    abstract fun transferDao(): TransferDao
}
