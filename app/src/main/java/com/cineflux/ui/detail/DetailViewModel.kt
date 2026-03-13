package com.cineflux.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflux.data.model.Movie
import com.cineflux.data.model.TorrentInfo
import com.cineflux.data.repository.MovieRepository
import com.cineflux.data.torrent.TorrentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTorrent: TorrentInfo? = null,
    val downloadStarted: Boolean = false,
    val recommendations: List<Movie> = emptyList(),
    val torrentsLoading: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    private val torrentRepository: TorrentRepository
) : ViewModel() {

    private val movieId: Int = savedStateHandle["movieId"] ?: 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMovie()
    }

    private fun loadMovie() {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val detail = movieRepository.getMovieDetails(movieId)
                _uiState.value = DetailUiState(
                    movie = detail,
                    isLoading = false,
                    torrentsLoading = true
                )

                launch {
                    try {
                        val recs = movieRepository.getRecommendations(movieId)
                        _uiState.value = _uiState.value.copy(recommendations = recs)
                    } catch (_: Exception) { }
                }

                val movieWithTorrents = movieRepository.getMovieWithTorrents(movieId)
                _uiState.value = _uiState.value.copy(
                    movie = movieWithTorrents,
                    torrentsLoading = false,
                    selectedTorrent = movieWithTorrents.torrents.firstOrNull { it.quality.contains("1080") }
                        ?: movieWithTorrents.torrents.firstOrNull()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    torrentsLoading = false,
                    error = "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    fun selectTorrent(torrent: TorrentInfo) {
        _uiState.value = _uiState.value.copy(selectedTorrent = torrent)
    }

    fun getMagnetUrl(): String? {
        val movie = _uiState.value.movie ?: return null
        val torrent = _uiState.value.selectedTorrent ?: return null
        return torrent.magnetUrl(movie.title)
    }

    fun startBuiltInDownload(): Long {
        val movie = _uiState.value.movie ?: return -1
        val torrent = _uiState.value.selectedTorrent ?: return -1
        val downloadId = torrentRepository.startDownload(movie, torrent)
        _uiState.value = _uiState.value.copy(downloadStarted = true)
        return downloadId
    }
}
