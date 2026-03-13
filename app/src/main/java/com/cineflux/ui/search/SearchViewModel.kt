package com.cineflux.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflux.data.model.Movie
import com.cineflux.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val results: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = query
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                if (q.length < 2) {
                    emit(SearchUiState())
                    return@flow
                }
                emit(SearchUiState(isLoading = true, hasSearched = true))
                try {
                    val results = movieRepository.searchMovies(q)
                    emit(SearchUiState(results = results, hasSearched = true))
                } catch (e: Exception) {
                    emit(SearchUiState(error = e.message, hasSearched = true))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }
}
