package com.textvision.alistclient.transfer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType

@Entity(tableName = "transfer_tasks")
data class TransferEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val remotePath: String,
    val localPath: String?,
    val sourceUri: String?,
    val bytesDone: Long,
    val totalBytes: Long,
    val type: TransferType,
    val status: TransferStatus,
    val failureReason: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
