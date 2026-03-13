package com.cineflux.data.api

import com.cineflux.data.model.OpenSubtitleResult
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface SubtitleApi {

    @GET("search/imdbid-{imdbId}/sublanguageid-eng")
    suspend fun searchByImdb(
        @Path("imdbId") imdbId: String,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): List<OpenSubtitleResult>

    @GET("search/query-{query}/sublanguageid-eng")
    suspend fun searchByName(
        @Path("query", encoded = true) query: String,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): List<OpenSubtitleResult>

    companion object {
        const val BASE_URL = "https://rest.opensubtitles.org/"
        const val USER_AGENT = "CineFlux v1.0"
    }
}
