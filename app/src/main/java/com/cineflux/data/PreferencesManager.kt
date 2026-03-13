package com.cineflux.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("cineflux_prefs", Context.MODE_PRIVATE)

    var downloadPath: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH, null) ?: defaultDownloadPath
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_PATH, value).apply()

    val defaultDownloadPath: String
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "CineFlux"
            )
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath
        }

    fun getAvailableStorageLocations(): List<StorageLocation> {
        val locations = mutableListOf<StorageLocation>()
        val seenRoots = mutableSetOf<String>()

        val internalMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (internalMovies != null) {
            val cinefluxDir = File(internalMovies, "CineFlux")
            if (!cinefluxDir.exists()) cinefluxDir.mkdirs()
            seenRoots.add(Environment.getExternalStorageDirectory().absolutePath)
            locations.add(
                StorageLocation(
                    name = "Internal Storage",
                    path = cinefluxDir.absolutePath,
                    freeSpace = getFreeSpace(cinefluxDir.absolutePath)
                )
            )
        }

        context.getExternalFilesDirs(null)?.filterNotNull()?.forEach { appDir ->
            val volumeRoot = extractVolumeRoot(appDir.absolutePath) ?: return@forEach
            if (volumeRoot in seenRoots) return@forEach
            seenRoots.add(volumeRoot)

            val volumeName = File(volumeRoot).name
            val cinefluxDir = File(volumeRoot, "CineFlux")
            if (!cinefluxDir.exists()) cinefluxDir.mkdirs()

            if (cinefluxDir.canWrite()) {
                locations.add(
                    StorageLocation(
                        name = "USB/SD: $volumeName",
                        path = cinefluxDir.absolutePath,
                        freeSpace = getFreeSpace(cinefluxDir.absolutePath)
                    )
                )
            } else {
                val fallbackDir = File(appDir, "CineFlux")
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                locations.add(
                    StorageLocation(
                        name = "USB/SD: $volumeName",
                        path = fallbackDir.absolutePath,
                        freeSpace = getFreeSpace(appDir.absolutePath)
                    )
                )
            }
        }

        if (locations.isEmpty()) {
            locations.add(
                StorageLocation(
                    name = "Internal Storage",
                    path = defaultDownloadPath,
                    freeSpace = getFreeSpace(defaultDownloadPath)
                )
            )
        }

        return locations
    }

    private fun extractVolumeRoot(appDirPath: String): String? {
        val marker = "/Android/data/"
        val idx = appDirPath.indexOf(marker)
        if (idx <= 0) return null
        return appDirPath.substring(0, idx)
    }

    private fun getFreeSpace(path: String): Long {
        return try {
            val stat = android.os.StatFs(path)
            stat.availableBytes
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        private const val KEY_DOWNLOAD_PATH = "download_path"
    }
}

data class StorageLocation(
    val name: String,
    val path: String,
    val freeSpace: Long
) {
    val freeSpaceText: String
        get() = when {
            freeSpace >= 1_073_741_824 -> String.format("%.1f GB free", freeSpace / 1_073_741_824.0)
            freeSpace >= 1_048_576 -> String.format("%.0f MB free", freeSpace / 1_048_576.0)
            else -> "Unknown"
        }
}
