package com.textvision.alistclient.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smoke")
data class SmokeEntity(
    @PrimaryKey val id: String,
    val value: String,
)
