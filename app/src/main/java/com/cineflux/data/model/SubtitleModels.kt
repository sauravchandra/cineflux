package com.cineflux.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSubtitleResult(
    @SerialName("IDSubtitleFile") val id: String = "",
    @SerialName("SubDownloadLink") val downloadLink: String = "",
    @SerialName("SubFileName") val fileName: String = "",
    @SerialName("SubFormat") val format: String = "srt",
    @SerialName("SubLanguageID") val language: String = "eng",
    @SerialName("MovieReleaseName") val releaseName: String = "",
    @SerialName("SubDownloadsCnt") val downloads: String = "0",
    @SerialName("Score") val score: Double = 0.0
)
