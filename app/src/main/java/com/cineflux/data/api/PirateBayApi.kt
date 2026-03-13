package com.cineflux.data.api

import com.cineflux.data.model.PirateBayResult
import retrofit2.http.GET
import retrofit2.http.Query

interface PirateBayApi {

    @GET("q.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("cat") category: String = "207"
    ): List<PirateBayResult>

    companion object {
        const val BASE_URL = "https://apibay.org/"

        private val TRACKERS = listOf(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.demonii.com:1337/announce",
            "udp://tracker.openbittorrent.com:80",
            "udp://p4p.arenabg.com:1337",
            "udp://exodus.desync.com:6969/announce"
        )

        fun buildMagnetUrl(hash: String, name: String): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            val trackers = TRACKERS.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
            return "magnet:?xt=urn:btih:$hash&dn=$encodedName$trackers"
        }
    }
}
