package com.cineflux.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YtsResponse(
    val status: String,
    @SerialName("status_message") val statusMessage: String,
    val data: YtsData
)

@Serializable
data class YtsData(
    @SerialName("movie_count") val movieCount: Int = 0,
    val movies: List<YtsMovie>? = null
)

@Serializable
data class YtsMovie(
    val id: Int,
    val title: String,
    val year: Int,
    val rating: Double,
    val torrents: List<YtsTorrent> = emptyList()
)

@Serializable
data class YtsTorrent(
    val url: String = "",
    val hash: String,
    val quality: String,
    val type: String = "bluray",
    val seeds: Int = 0,
    val peers: Int = 0,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val size: String = ""
)
