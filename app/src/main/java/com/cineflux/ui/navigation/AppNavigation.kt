package com.cineflux.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.rememberDrawerState
import com.cineflux.ui.detail.DetailScreen
import com.cineflux.ui.downloads.DownloadsScreen
import com.cineflux.ui.home.CategoryScreen
import com.cineflux.ui.home.HomeScreen
import com.cineflux.ui.home.HomeViewModel
import com.cineflux.ui.player.PlayerScreen
import com.cineflux.ui.search.SearchScreen
import com.cineflux.ui.settings.SettingsScreen
import com.cineflux.ui.theme.CineFluxRed
import com.cineflux.ui.theme.DarkSurface

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

private val navItems = listOf(
    NavItem("Home", Icons.Default.Home, Screen.Home.route),
    NavItem("Search", Icons.Default.Search, Screen.Search.route),
    NavItem("Downloads", Icons.Default.Download, Screen.Downloads.route),
    NavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CineFluxNavHost() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isPlayerScreen = currentRoute?.startsWith("player") == true
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val contentFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    if (isPlayerScreen) {
        NavContent(navController, homeViewModel)
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Column(
                    Modifier
                        .background(Color.Black)
                        .fillMaxHeight()
                        .padding(12.dp)
                        .selectableGroup(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                scope.launch {
                                    drawerState.setValue(DrawerValue.Closed)
                                    contentFocusRequester.requestFocus()
                                }
                            },
                            leadingContent = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) CineFluxRed else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) CineFluxRed else Color.White
                            )
                        }
                    }
                }
            },
            scrimBrush = SolidColor(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 56.dp)
                    .focusRequester(contentFocusRequester)
            ) {
                NavContent(navController, homeViewModel)
            }
        }
    }
}

@Composable
private fun NavContent(
    navController: androidx.navigation.NavHostController,
    homeViewModel: HomeViewModel
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
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
