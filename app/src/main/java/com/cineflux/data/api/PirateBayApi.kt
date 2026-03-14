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

        val TRACKERS = listOf(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.openbittorrent.com:80/announce",
            "udp://open.demonii.com:1337/announce",
            "udp://exodus.desync.com:6969/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://tracker.dler.com:6969/announce",
            "udp://tracker.srv00.com:6969/announce",
            "udp://tracker.opentorrent.top:6969/announce",
            "udp://tracker.qu.ax:6969/announce",
            "udp://tracker.breizh.pm:6969/announce",
            "udp://p4p.arenabg.com:1337/announce",
            "udp://tracker.moeking.me:6969/announce",
            "udp://explodie.org:6969/announce",
            "udp://tracker.tiny-vps.com:6969/announce",
            "http://tracker.bt4g.com:2095/announce"
        )

        fun buildMagnetUrl(hash: String, name: String): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            val trackers = TRACKERS.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
            return "magnet:?xt=urn:btih:$hash&dn=$encodedName$trackers"
        }
    }
}
