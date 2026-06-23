package com.textvision.alistclient.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmokeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SmokeEntity)

    @Query("SELECT * FROM smoke WHERE id = :id")
    suspend fun find(id: String): SmokeEntity?
}
