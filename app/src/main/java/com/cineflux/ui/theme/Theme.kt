package com.cineflux.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CineFluxColorScheme = darkColorScheme(
    primary = CineFluxRed,
    onPrimary = DarkOnBackground,
    primaryContainer = CineFluxRedDark,
    secondary = CineFluxGold,
    onSecondary = DarkBackground,
    secondaryContainer = CineFluxGoldDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = CineFluxRed
)

@Composable
fun CineFluxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CineFluxColorScheme,
        typography = CineFluxTypography,
        content = content
    )
}
