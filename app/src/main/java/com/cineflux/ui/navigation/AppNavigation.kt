package com.cineflux.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cineflux.ui.detail.DetailScreen
import com.cineflux.ui.downloads.DownloadsScreen
import com.cineflux.ui.home.CategoryScreen
import com.cineflux.ui.home.HomeScreen
import com.cineflux.ui.home.HomeViewModel
import com.cineflux.ui.player.PlayerScreen
import com.cineflux.ui.search.SearchScreen
import com.cineflux.ui.settings.SettingsScreen

@Composable
fun CineFluxNavHost() {
    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onCategoryClick = { index ->
                    navController.navigate(Screen.Category.createRoute(index))
                },
                viewModel = homeViewModel
            )
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            val state by homeViewModel.uiState.collectAsStateWithLifecycle()
            val category = state.categories.getOrNull(index)
            if (category != null) {
                CategoryScreen(
                    title = category.title,
                    movies = category.movies,
                    isLoadingMore = category.loadingMore,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.Detail.createRoute(movieId))
                    },
                    onBack = { navController.popBackStack() },
                    onLoadMore = { homeViewModel.loadMoreForCategory(index) }
                )
            }
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onPlayClick = { downloadId ->
                    navController.navigate(Screen.Player.createRoute(downloadId))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onPlayClick = { downloadId ->
                    navController.navigate(Screen.Player.createRoute(downloadId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("downloadId") { type = NavType.LongType })
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
