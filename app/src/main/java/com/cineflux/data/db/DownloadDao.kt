package com.cineflux.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE infoHash = :hash LIMIT 1")
    suspend fun getByInfoHash(hash: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN (0, 1)")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, totalBytes = :total, status = :status WHERE infoHash = :hash")
    suspend fun updateProgress(hash: String, bytes: Long, total: Long, status: Int)

    @Query("UPDATE downloads SET filePath = :path, status = :status WHERE infoHash = :hash")
    suspend fun markCompleted(hash: String, path: String, status: Int = DownloadEntity.STATUS_COMPLETED)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}
