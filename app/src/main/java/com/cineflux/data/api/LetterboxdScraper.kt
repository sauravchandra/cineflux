package com.cineflux.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class LetterboxdFilm(
    val title: String,
    val rating: Double
)

@Singleton
class LetterboxdScraper @Inject constructor(
    baseClient: OkHttpClient
) {
    private val client = baseClient.newBuilder()
        .followRedirects(true)
        .build()

    private val titleRegex = Regex("""alt="([^"]{2,80})"""")
    private val titleRatingRegex = Regex("""alt="([^"]{2,80})"[^>]*>.*?data-average-rating="([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
    private val pairRegex = Regex("""data-film-slug="([^"]+)"[^>]*data-average-rating="([^"]+)"""")

    private val allEndpoints = listOf(
        // Most loved
        "Most Loved Films" to "/films/ajax/by/member/page/1/",
        "Popular All Time" to "/films/ajax/popular/page/1/",

        // Top rated by genre
        "Top Drama" to "/films/ajax/by/rating/genre/drama/page/1/",
        "Top Comedy" to "/films/ajax/by/rating/genre/comedy/page/1/",
        "Top Animation" to "/films/ajax/by/rating/genre/animation/page/1/",
        "Top Romance" to "/films/ajax/by/rating/genre/romance/page/1/",
        "Top Thriller" to "/films/ajax/by/rating/genre/thriller/page/1/",
        "Top Crime" to "/films/ajax/by/rating/genre/crime/page/1/",
        "Top Sci-Fi" to "/films/ajax/by/rating/genre/science-fiction/page/1/",
        "Top Horror" to "/films/ajax/by/rating/genre/horror/page/1/",
        "Top War" to "/films/ajax/by/rating/genre/war/page/1/",
        "Top Documentary" to "/films/ajax/by/rating/genre/documentary/page/1/",
        "Top Mystery" to "/films/ajax/by/rating/genre/mystery/page/1/",
        "Top Music" to "/films/ajax/by/rating/genre/music/page/1/",

        // Official Letterboxd lists
        "Letterboxd Top 500" to "/official/list/letterboxds-top-500-films/page/1/",
        "Most Fans Top 250" to "/official/list/top-250-films-with-the-most-fans/page/1/",
        "Top 250 Animated" to "/official/list/top-250-animated-films/page/1/",
    )

    fun getAvailableCategories(): List<Pair<String, String>> = allEndpoints

    suspend fun scrapeCategory(path: String, page: Int = 1): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        val pagedPath = path.replace("/page/1/", "/page/$page/")
        scrapeAjax("$BASE_URL$pagedPath")
    }

    suspend fun getPopularThisWeek(): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        scrapeAjax("$BASE_URL/films/ajax/popular/this/week/page/1/")
    }

    suspend fun getPopularLists(pages: Int = 1): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val hrefRegex = Regex("""href="(/([^/]+)/list/([^/"]+)/)"[^>]*>([^<]+)""")
        val allResults = mutableListOf<Pair<String, String>>()

        for (page in 1..pages) {
            val suffix = if (page == 1) "" else "page/$page/"
            val urls = listOf(
                "$BASE_URL/lists/popular/this/week/$suffix",
                "$BASE_URL/lists/featured/$suffix"
            )
            for (url in urls) {
                try {
                    val request = Request.Builder().url(url).header("User-Agent", UA).build()
                    val body = client.newCall(request).execute().body?.string() ?: continue
                val results = hrefRegex.findAll(body).mapNotNull { m ->
                    val name = m.groupValues[4].trim().let {
                        if (it.length > 50) it.take(47) + "..." else it
                    }
                    if (name.isBlank()) return@mapNotNull null
                    val path = "${m.groupValues[1]}page/1/"
                    name to path
                }.toList()
                    allResults.addAll(results)
                } catch (e: Exception) {
                    Log.w(TAG, "Lists fetch failed for $url: ${e.message}")
                }
            }
        }

        val deduplicated = allResults.distinctBy { it.second }
        Log.i(TAG, "Found ${deduplicated.size} popular lists (from ${allResults.size} total, $pages pages)")
        deduplicated
    }

    suspend fun scrapeList(path: String, page: Int = 1): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        val sortedPath = if (!path.contains("/by/")) path.replace("/page/", "/by/rating/page/") else path
        val pagedPath = sortedPath.replace("/page/1/", "/page/$page/")
        scrapeAjax("$BASE_URL$pagedPath")
    }

    private fun scrapeAjax(url: String): List<LetterboxdFilm> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "text/html, */*; q=0.01")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()

            val films = mutableListOf<LetterboxdFilm>()
            val slugRatings = pairRegex.findAll(body).associate {
                it.groupValues[1] to it.groupValues[2].toDoubleOrNull()
            }

            val titles = titleRegex.findAll(body)
                .map { it.groupValues[1].trim() }
                .filter { it.length > 2 && it != "Letterboxd" }
                .distinct()
                .toList()

            for (title in titles.take(15)) {
                val matchingSlug = slugRatings.entries.firstOrNull { (slug, _) ->
                    slug.replace("-", " ").contains(title.take(10).lowercase().replace(" ", ""))
                }
                val lbRating = matchingSlug?.value ?: 0.0
                films.add(LetterboxdFilm(title, lbRating * 2.0))
            }

            Log.i(TAG, "Scraped ${films.size} films from $url")
            films
        } catch (e: Exception) {
            Log.w(TAG, "Scrape failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "LetterboxdScraper"
        private const val BASE_URL = "https://letterboxd.com"
        private const val UA = "Mozilla/5.0 (Linux; Android 12; SmartTV) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    }
}
