package com.cineflux.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
    data object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: Int) = "detail/$movieId"
    }
    data object Player : Screen("player/{downloadId}") {
        fun createRoute(downloadId: Long) = "player/$downloadId"
    }
    data object Category : Screen("category/{index}") {
        fun createRoute(index: Int) = "category/$index"
    }
}
