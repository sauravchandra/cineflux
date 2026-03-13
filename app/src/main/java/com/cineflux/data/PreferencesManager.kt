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

        val externalDirs = arrayOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        )
        externalDirs.forEachIndexed { index, dir ->
            if (dir == null) return@forEachIndexed
            val cinefluxDir = File(dir, "CineFlux")
            if (!cinefluxDir.exists()) cinefluxDir.mkdirs()
            val name = if (index == 0) "Internal Storage" else "External Storage ${if (index > 1) index else ""}"
            locations.add(
                StorageLocation(
                    name = name.trim(),
                    path = cinefluxDir.absolutePath,
                    freeSpace = getFreeSpace(cinefluxDir.absolutePath)
                )
            )
        }

        try {
            val storageDir = File("/storage")
            storageDir.listFiles()?.filter {
                it.isDirectory && it.name != "emulated" && it.name != "self"
            }?.forEach { vol ->
                val cinefluxDir = File(vol, "CineFlux")
                if (locations.any { it.path.contains(vol.name) }) return@forEach
                try {
                    if (!cinefluxDir.exists()) cinefluxDir.mkdirs()
                    if (cinefluxDir.canWrite()) {
                        locations.add(
                            StorageLocation(
                                name = "USB/SD: ${vol.name}",
                                path = cinefluxDir.absolutePath,
                                freeSpace = getFreeSpace(cinefluxDir.absolutePath)
                            )
                        )
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }

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
