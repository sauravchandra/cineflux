package com.cineflux.data.model

import com.cineflux.data.api.PirateBayApi
import com.cineflux.data.api.YtsApi

data class Movie(
    val tmdbId: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val rating: Double,
    val runtime: Int? = null,
    val genres: List<String> = emptyList(),
    val tagline: String = "",
    val torrents: List<TorrentInfo> = emptyList()
)

data class TorrentInfo(
    val hash: String,
    val quality: String,
    val type: String,
    val seeds: Int,
    val peers: Int,
    val size: String,
    val sizeBytes: Long,
    val source: TorrentSource = TorrentSource.YTS,
    val originalMagnet: String? = null
) {
    fun magnetUrl(title: String): String {
        if (originalMagnet != null) return originalMagnet
        return when (source) {
            TorrentSource.YTS -> YtsApi.buildMagnetUrl(hash, title)
            TorrentSource.TPB -> PirateBayApi.buildMagnetUrl(hash, type.ifBlank { title })
            TorrentSource.LEET -> PirateBayApi.buildMagnetUrl(hash, type.ifBlank { title })
        }
    }
}

enum class TorrentSource { YTS, TPB, LEET }
