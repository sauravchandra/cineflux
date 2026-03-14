package com.cineflux.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val quality: String,
    val magnetUrl: String,
    val torrentUrl: String? = null,
    val infoHash: String,
    val filePath: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: Int = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_FAILED = 4
    }
}
