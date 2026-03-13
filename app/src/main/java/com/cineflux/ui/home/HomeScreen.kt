package com.cineflux.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import coil3.compose.AsyncImage
import com.cineflux.data.model.Movie
import com.cineflux.ui.components.ActionIconButton
import com.cineflux.ui.components.MovieCard
import androidx.compose.material3.CircularProgressIndicator
import com.cineflux.ui.components.ShimmerHero
import com.cineflux.ui.components.ShimmerMovieRow
import com.cineflux.ui.theme.CineFluxRed

@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 2 && total > 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) viewModel.loadMoreCategories()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item { TopBar(onSearchClick, onDownloadsClick, onSettingsClick) }

        if (state.isLoading) {
            item { ShimmerHero() }
            items(4) { ShimmerMovieRow() }
        } else {
            item {
                val heroMovies = state.categories.firstOrNull()?.movies?.take(5) ?: emptyList()
                if (heroMovies.isNotEmpty()) {
                    FeaturedCarousel(movies = heroMovies, onMovieClick = onMovieClick)
                }
            }

            state.categories.forEachIndexed { index, category ->
                item(key = category.title) {
                    MovieCategoryRow(
                        title = category.title,
                        movies = category.movies,
                        onMovieClick = onMovieClick,
                        onTitleClick = { onCategoryClick(index) },
                        isLoadingMore = category.loadingMore,
                        onLoadMore = { viewModel.loadMoreForCategory(index) }
                    )
                }
            }

            if (!state.allCategoriesLoaded) {
                item { ShimmerMovieRow() }
            }
        }
    }
}

@Composable
private fun TopBar(
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CINEFLUX",
            style = MaterialTheme.typography.headlineLarge,
            color = CineFluxRed
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionIconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            ActionIconButton(onClick = onDownloadsClick) {
                Icon(Icons.Default.Download, contentDescription = "Downloads")
            }
            ActionIconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun FeaturedCarousel(movies: List<Movie>, onMovieClick: (Int) -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(movies.size) {
        while (true) {
            delay(6000)
            currentIndex = (currentIndex + 1) % movies.size
        }
    }

    val movie = movies[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero"
        ) { idx ->
            val m = movies[idx]
            AsyncImage(
                model = m.backdropUrl ?: m.posterUrl,
                contentDescription = m.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0E0E0E)),
                                startY = size.height * 0.3f,
                                endY = size.height
                            )
                        )
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 40.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                movies.indices.forEach { i ->
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentIndex) 10.dp else 6.dp)
                                .background(
                                    if (i == currentIndex) CineFluxRed else Color.White.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCategoryRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onTitleClick: () -> Unit = {},
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    if (movies.isEmpty()) return

    Column(modifier = Modifier.padding(top = 24.dp)) {
        var isFocused by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 48.dp, bottom = 12.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                        (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                         event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                        onTitleClick()
                        true
                    } else false
                }
                .clickable { onTitleClick() }
        ) {
            Text(
                text = title,
                style = if (isFocused) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See all",
                tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(if (isFocused) 28.dp else 22.dp)
            )
        }
        val rowState = rememberLazyListState()
        val shouldLoadMore = remember {
            derivedStateOf {
                val lastVisible = rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= movies.size - 3 && movies.isNotEmpty()
            }
        }
        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) onLoadMore()
        }

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies, key = { it.tmdbId }, contentType = { "movie" }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.tmdbId) },
                    modifier = Modifier.width(160.dp)
                )
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.width(80.dp).height(234.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CineFluxRed,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
