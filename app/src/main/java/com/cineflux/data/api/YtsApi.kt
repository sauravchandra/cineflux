package com.cineflux.data.api

import com.cineflux.data.model.YtsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YtsApi {

    @GET("list_movies.json")
    suspend fun searchMovies(
        @Query("query_term") query: String,
        @Query("limit") limit: Int = 20,
        @Query("sort_by") sortBy: String = "seeds"
    ): YtsResponse

    companion object {
        const val BASE_URL = "https://yts.bz/api/v2/"

        private val TRACKERS = listOf(
            "udp://open.demonii.com:1337/announce",
            "udp://tracker.openbittorrent.com:80",
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://p4p.arenabg.com:1337",
            "udp://tracker.internetwarriors.net:1337/announce",
            "udp://9.rarbg.me:2710/announce",
            "udp://exodus.desync.com:6969/announce"
        )

        fun buildMagnetUrl(hash: String, name: String): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            val trackers = TRACKERS.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
            return "magnet:?xt=urn:btih:$hash&dn=$encodedName$trackers"
        }
    }
}
