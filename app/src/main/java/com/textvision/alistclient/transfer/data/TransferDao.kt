package com.textvision.alistclient.transfer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.textvision.alistclient.transfer.model.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_tasks ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfer_tasks WHERE id = :id")
    suspend fun find(id: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferEntity)

    @Query("UPDATE transfer_tasks SET status = :status, failureReason = :reason, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateStatus(id: String, status: TransferStatus, reason: String?, updatedAtMillis: Long)

    @Query("UPDATE transfer_tasks SET bytesDone = :bytesDone, totalBytes = :totalBytes, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateProgress(id: String, bytesDone: Long, totalBytes: Long, updatedAtMillis: Long)

    @Query("UPDATE transfer_tasks SET status = 'Cancelled', failureReason = :reason, updatedAtMillis = :updatedAtMillis WHERE id = :id AND status IN ('Waiting', 'Uploading', 'Downloading')")
    suspend fun cancelActiveTask(id: String, reason: String?, updatedAtMillis: Long): Int

    @Query("UPDATE transfer_tasks SET status = 'Interrupted', failureReason = '传输中断', updatedAtMillis = :updatedAtMillis WHERE status IN ('Waiting', 'Uploading', 'Downloading')")
    suspend fun markActiveTasksInterrupted(updatedAtMillis: Long)

    @Query("DELETE FROM transfer_tasks")
    suspend fun deleteAll()
}
