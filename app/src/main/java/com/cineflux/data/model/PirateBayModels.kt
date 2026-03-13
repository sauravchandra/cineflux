package com.cineflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PirateBayResult(
    val id: String = "",
    val name: String = "",
    val info_hash: String = "",
    val leechers: String = "0",
    val seeders: String = "0",
    val num_files: String = "0",
    val size: String = "0",
    val username: String = "",
    val added: String = "",
    val category: String = ""
) {
    val seedCount: Int get() = seeders.toIntOrNull() ?: 0
    val peerCount: Int get() = leechers.toIntOrNull() ?: 0
    val sizeBytes: Long get() = size.toLongOrNull() ?: 0

    val sizeText: String
        get() {
            val bytes = sizeBytes
            return when {
                bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576 -> String.format("%.0f MB", bytes / 1_048_576.0)
                else -> String.format("%.0f KB", bytes / 1024.0)
            }
        }

    val qualityGuess: String
        get() = when {
            name.contains("2160p", ignoreCase = true) || name.contains("4K", ignoreCase = true) -> "2160p"
            name.contains("1080p", ignoreCase = true) -> "1080p"
            name.contains("720p", ignoreCase = true) -> "720p"
            name.contains("480p", ignoreCase = true) -> "480p"
            else -> "Unknown"
        }
}
