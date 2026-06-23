package com.textvision.alistclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SmokeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
}
