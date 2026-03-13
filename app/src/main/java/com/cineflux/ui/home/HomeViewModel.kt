package com.cineflux.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflux.data.api.LetterboxdScraper
import com.cineflux.data.model.Movie
import com.cineflux.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(
    val title: String,
    val movies: List<Movie>,
    val path: String = "",
    val page: Int = 1,
    val loadingMore: Boolean = false
)

data class HomeUiState(
    val featured: Movie? = null,
    val categories: List<CategoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val allCategoriesLoaded: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val letterboxdScraper: LetterboxdScraper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private val GENRE_MAP = mapOf(
            28 to "Action",
            35 to "Comedy",
            18 to "Drama",
            27 to "Horror",
            878 to "Sci-Fi",
            53 to "Thriller",
            16 to "Animation",
            10749 to "Romance",
            99 to "Documentary"
        )
    }

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)

            try {
                val trending = movieRepository.getTrending()
                _uiState.value = HomeUiState(
                    featured = trending.firstOrNull(),
                    categories = listOf(CategoryRow("Trending This Week", trending)),
                    isLoading = false
                )
            } catch (_: Exception) { }

            launch {
                try {
                    val np = movieRepository.getNowPlaying()
                    if (np.isNotEmpty()) addRow(CategoryRow("Now Playing", np))
                } catch (_: Exception) { }
            }

            val genreCategories = letterboxdScraper.getAvailableCategories().shuffled()
            val popularLists = try { letterboxdScraper.getPopularLists() } catch (_: Exception) { emptyList() }
            allLbCategories = genreCategories + popularLists.map { (name, path) -> "📋 $name" to path }
            loadedLbCount = 0
            loadMoreCategories(6)
        }
    }

    private var allLbCategories: List<Pair<String, String>> = emptyList()
    private var loadedLbCount = 0
    @Volatile private var loadingMoreCategories = false

    fun loadMoreCategories(count: Int = 5) {
        if (loadingMoreCategories) return
        if (loadedLbCount >= allLbCategories.size) {
            _uiState.value = _uiState.value.copy(allCategoriesLoaded = true)
            return
        }
        loadingMoreCategories = true

        val batch = allLbCategories.drop(loadedLbCount).take(count)
        loadedLbCount += batch.size

        viewModelScope.launch {
            val rows = batch.map { (title, path) ->
                async {
                    try {
                        val movies = movieRepository.getLetterboxdList {
                            if (path.contains("/list/")) letterboxdScraper.scrapeList(path)
                            else letterboxdScraper.scrapeCategory(path)
                        }
                        if (movies.isNotEmpty()) CategoryRow(title, movies, path = path) else null
                    } catch (_: Exception) { null }
                }
            }.awaitAll().filterNotNull()

            if (rows.isNotEmpty()) {
                val current = _uiState.value
                _uiState.value = current.copy(categories = current.categories + rows, isLoading = false)
            }
            loadingMoreCategories = false
        }
    }

    fun loadMoreForCategory(index: Int) {
        val current = _uiState.value
        val category = current.categories.getOrNull(index) ?: return
        if (category.path.isBlank() || category.loadingMore) return

        val nextPage = category.page + 1
        val updated = current.categories.toMutableList()
        updated[index] = category.copy(loadingMore = true)
        _uiState.value = current.copy(categories = updated)

        viewModelScope.launch {
            try {
                val more = movieRepository.loadMoreLetterboxd(category.path, nextPage)
                val currentState = _uiState.value
                val cats = currentState.categories.toMutableList()
                val existing = cats.getOrNull(index) ?: return@launch
                val existingIds = existing.movies.map { it.tmdbId }.toSet()
                val newMovies = more.filter { it.tmdbId !in existingIds }
                cats[index] = existing.copy(
                    movies = existing.movies + newMovies,
                    page = nextPage,
                    loadingMore = false
                )
                _uiState.value = currentState.copy(categories = cats)
            } catch (_: Exception) {
                val currentState = _uiState.value
                val cats = currentState.categories.toMutableList()
                cats.getOrNull(index)?.let { cats[index] = it.copy(loadingMore = false) }
                _uiState.value = currentState.copy(categories = cats)
            }
        }
    }

    private fun addRow(row: CategoryRow) {
        val current = _uiState.value
        if (current.categories.any { it.title == row.title }) return
        _uiState.value = current.copy(
            categories = current.categories + row,
            isLoading = false
        )
    }

    private suspend fun safeCall(block: suspend () -> List<Movie>): List<Movie> {
        return try { block() } catch (_: Exception) { emptyList() }
    }
}
