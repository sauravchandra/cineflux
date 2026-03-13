package com.cineflux

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
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
        requestStorageAccess()
        ContextCompat.startForegroundService(this, Intent(this, DownloadService::class.java))
        setContent {
            CineFluxTheme {
                CineFluxNavHost()
            }
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        if (hasExternalWriteAccess()) {
            Log.i(TAG, "External storage write access confirmed")
            return
        }
        Log.w(TAG, "No external storage write access, requesting...")

        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            },
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        )

        for (intent in intents) {
            if (intent.resolveActivity(packageManager) != null) {
                Log.i(TAG, "Launching ${intent.action}")
                startActivity(intent)
                return
            }
        }

        Log.w(TAG, "No settings UI available. Grant via: adb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
    }

    private fun hasExternalWriteAccess(): Boolean {
        return try {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val testDir = java.io.File(moviesDir, "CineFlux")
            if (!testDir.exists()) testDir.mkdirs()
            val testFile = java.io.File(testDir, ".write_test")
            val ok = testFile.createNewFile()
            testFile.delete()
            ok || testDir.canWrite()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "CineFlux"
    }
}
