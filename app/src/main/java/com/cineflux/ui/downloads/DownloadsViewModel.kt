package com.cineflux.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflux.data.db.DownloadEntity
import com.cineflux.data.torrent.DownloadProgress
import com.cineflux.data.torrent.TorrentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DownloadItem(
    val entity: DownloadEntity,
    val liveProgress: DownloadProgress?,
    val localPaused: Boolean = false
) {
    val displayProgress: Float
        get() = liveProgress?.progress ?: run {
            if (entity.totalBytes > 0) entity.downloadedBytes.toFloat() / entity.totalBytes
            else 0f
        }

    val isCompleted: Boolean
        get() = entity.status == DownloadEntity.STATUS_COMPLETED

    val isPaused: Boolean
        get() = localPaused || entity.status == DownloadEntity.STATUS_PAUSED

    val speedText: String
        get() {
            if (localPaused) return "Paused"
            val rate = liveProgress?.downloadRate ?: return ""
            return when {
                rate > 1_048_576 -> String.format("%.1f MB/s", rate / 1_048_576.0)
                rate > 1024 -> String.format("%.0f KB/s", rate / 1024.0)
                else -> "$rate B/s"
            }
        }
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val torrentRepository: TorrentRepository
) : ViewModel() {

    private val pausedHashes = MutableStateFlow<Set<String>>(emptySet())

    val downloads: StateFlow<List<DownloadItem>> =
        combine(
            torrentRepository.allDownloads,
            torrentRepository.liveProgress,
            pausedHashes
        ) { entities, progressMap, paused ->
            entities.map { entity ->
                DownloadItem(
                    entity = entity,
                    liveProgress = progressMap[entity.infoHash],
                    localPaused = entity.infoHash in paused
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pause(infoHash: String) {
        pausedHashes.value = pausedHashes.value + infoHash
        torrentRepository.pauseDownload(infoHash)
    }

    fun resume(infoHash: String) {
        pausedHashes.value = pausedHashes.value - infoHash
        torrentRepository.resumeDownload(infoHash)
    }

    fun remove(infoHash: String) {
        pausedHashes.value = pausedHashes.value - infoHash
        torrentRepository.removeDownload(infoHash)
    }
}
