package com.cineflux.data.torrent

data class DownloadProgress(
    val infoHash: String,
    val name: String,
    val progress: Float,
    val downloadRate: Int,
    val uploadRate: Int,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val seeds: Int,
    val peers: Int,
    val state: TorrentState
)

enum class TorrentState {
    CHECKING,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    PAUSED,
    ERROR,
    UNKNOWN
}
