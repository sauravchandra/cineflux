package com.cineflux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.cineflux.data.torrent.DownloadService
import com.cineflux.ui.navigation.CineFluxNavHost
import com.cineflux.ui.theme.CineFluxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, DownloadService::class.java))
        setContent {
            CineFluxTheme {
                CineFluxNavHost()
            }
        }
    }
}
