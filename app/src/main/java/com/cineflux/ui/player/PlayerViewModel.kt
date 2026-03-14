package com.cineflux.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflux.data.PreferencesManager
import com.cineflux.data.repository.SubtitleRepository
import com.cineflux.data.torrent.TorrentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlayerState(
    val title: String = "",
    val filePath: String? = null,
    val subtitlePath: String? = null,
    val error: String? = null,
    val subtitleStatus: String = ""
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val torrentRepository: TorrentRepository,
    private val subtitleRepository: SubtitleRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val downloadId: Long = savedStateHandle["downloadId"] ?: 0L

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        loadDownload()
    }

    private fun loadDownload() {
        viewModelScope.launch {
            val download = torrentRepository.getDownload(downloadId)
            if (download == null) {
                _playerState.value = PlayerState(error = "Download not found")
                return@launch
            }

            var filePath = download.filePath
            if (filePath == null) {
                filePath = findVideoFile()
            }

            if (filePath == null) {
                _playerState.value = PlayerState(error = "Video file not found")
                return@launch
            }

            val existingSrt = findExistingSrt(filePath)
            if (existingSrt != null) {
                _playerState.value = PlayerState(
                    title = download.title,
                    filePath = filePath,
                    subtitlePath = existingSrt,
                    subtitleStatus = "Subtitles loaded"
                )
            } else {
                _playerState.value = PlayerState(
                    title = download.title,
                    filePath = filePath,
                    subtitleStatus = "Searching subtitles..."
                )
                fetchSubtitle(download.title, filePath)
            }
        }
    }

    private suspend fun findExistingSrt(videoPath: String): String? = withContext(Dispatchers.IO) {
        val video = java.io.File(videoPath)
        val srt = java.io.File(video.parent, video.nameWithoutExtension + ".srt")
        if (srt.exists()) srt.absolutePath else null
    }

    private suspend fun findVideoFile(): String? = withContext(Dispatchers.IO) {
        val videoExts = listOf(".mkv", ".mp4", ".avi", ".mov", ".wmv")
        val downloadDir = java.io.File(preferencesManager.downloadPath)
        if (!downloadDir.exists()) return@withContext null
        downloadDir.listFiles()
            ?.filter { file -> videoExts.any { file.name.endsWith(it, ignoreCase = true) } }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }

    private fun fetchSubtitle(title: String, videoPath: String) {
        viewModelScope.launch {
            val srtPath = subtitleRepository.findAndDownloadSubtitle(title, videoPath)
            _playerState.value = _playerState.value.copy(
                subtitlePath = srtPath,
                subtitleStatus = if (srtPath != null) "Subtitles loaded" else "No subtitles found"
            )
        }
    }
}
