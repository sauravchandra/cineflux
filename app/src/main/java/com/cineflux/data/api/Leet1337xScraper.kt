package com.cineflux.data.api

import android.util.Log
import com.cineflux.data.model.TorrentInfo
import com.cineflux.data.model.TorrentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Leet1337xScraper @Inject constructor(
    baseClient: OkHttpClient
) {
    private val client = baseClient.newBuilder().followRedirects(true).build()

    private val linkRegex = Regex("""href="/torrent/(\d+)/([^/"]+)/"[^>]*>([^<]+)</a>""")
    private val seedsInRowRegex = Regex("""coll-2 seeds">(\d+)<""")
    private val leechInRowRegex = Regex("""coll-3 leeches">(\d+)<""")
    private val sizeInRowRegex = Regex("""coll-4 size[^"]*">([^<]+)<""")
    private val magnetRegex = Regex("""href="(magnet:\?[^"]+)"""")
    private val hashRegex = Regex("""btih:([a-fA-F0-9]{40})""", RegexOption.IGNORE_CASE)
    private val qualityRegex = Regex("""(2160p|4K|1080p|720p|480p)""", RegexOption.IGNORE_CASE)

    suspend fun search(movieTitle: String, year: String = ""): List<TorrentInfo> = withContext(Dispatchers.IO) {
        try {
            val query = movieTitle.trim().replace(" ", "+")
            val html = fetch("$BASE_URL/search/$query/1/") ?: return@withContext emptyList()

            val rows = html.split("<tr>").drop(1)
            Log.d(TAG, "Split into ${rows.size} rows, first row snippet: ${rows.firstOrNull()?.take(200)}")
            val searchResults = rows.mapNotNull { row ->
                val link = linkRegex.find(row) ?: return@mapNotNull null
                val seeds = seedsInRowRegex.find(row)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val leechers = leechInRowRegex.find(row)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val size = sizeInRowRegex.find(row)?.groupValues?.get(1)?.trim() ?: "?"
                SearchResult(
                    slug = link.groupValues[2],
                    name = link.groupValues[3].trim(),
                    seeds = seeds,
                    leechers = leechers,
                    size = size,
                    torrentId = link.groupValues[1]
                )
            }

            val titleWords = movieTitle.lowercase().split(" ").filter { it.length > 1 }
            val filtered = searchResults
                .filter { r ->
                    val nameLower = r.name.lowercase().replace(".", " ").replace("-", " ")
                    val matchCount = titleWords.count { nameLower.contains(it) }
                    val hasQuality = qualityRegex.containsMatchIn(r.name)
                    matchCount >= (titleWords.size / 2).coerceAtLeast(1) && hasQuality
                }
                .sortedByDescending { it.seeds }
                .take(6)

            searchResults.take(5).forEach { r ->
                Log.d(TAG, "  Result: '${r.name}' ${r.seeds}s (slug=${r.slug.take(40)})")
            }
            Log.i(TAG, "Found ${searchResults.size} results, ${filtered.size} matched '$movieTitle'")

            filtered.mapNotNull { result ->
                try {
                    val page = fetch("$BASE_URL/torrent/${result.torrentId}/${result.slug}/") ?: return@mapNotNull null
                    val magnet = magnetRegex.find(page)?.groupValues?.get(1) ?: return@mapNotNull null
                    val hash = hashRegex.find(magnet)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
                    val quality = qualityRegex.find(result.name)?.groupValues?.get(1) ?: "Unknown"

                    TorrentInfo(
                        hash = hash,
                        quality = quality,
                        type = result.name,
                        seeds = result.seeds,
                        peers = result.leechers,
                        size = result.size,
                        sizeBytes = parseSizeToBytes(result.size),
                        source = TorrentSource.LEET,
                        originalMagnet = magnet
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Detail fetch failed: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search failed: ${e.message}")
            emptyList()
        }
    }

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html")
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) response.body?.string() else null
        } catch (_: Exception) { null }
    }

    private fun parseSizeToBytes(size: String): Long {
        val num = size.replace(",", "").trim().split(" ").firstOrNull()?.toDoubleOrNull() ?: return 0
        return when {
            size.contains("GB", ignoreCase = true) -> (num * 1_073_741_824).toLong()
            size.contains("MB", ignoreCase = true) -> (num * 1_048_576).toLong()
            size.contains("KB", ignoreCase = true) -> (num * 1024).toLong()
            else -> 0
        }
    }

    private data class SearchResult(
        val slug: String, val name: String,
        val seeds: Int, val leechers: Int, val size: String,
        val torrentId: String = ""
    )

    companion object {
        private const val TAG = "1337xScraper"
        private const val BASE_URL = "https://www.1377x.to"
        private const val UA = "Mozilla/5.0 (Linux; Android 12; SmartTV) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    }
}
