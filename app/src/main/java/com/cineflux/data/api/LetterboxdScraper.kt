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

        // Decades
        "Best of the 2020s" to "/films/ajax/popular/decade/2020s/page/1/",
        "Best of the 2010s" to "/films/ajax/popular/decade/2010s/page/1/",
        "Best of the 2000s" to "/films/ajax/popular/decade/2000s/page/1/",
        "Best of the 90s" to "/films/ajax/popular/decade/1990s/page/1/",
        "Best of the 80s" to "/films/ajax/popular/decade/1980s/page/1/",

        // Top rated by decade
        "Top Rated 2020s" to "/films/ajax/by/rating/decade/2020s/page/1/",
        "Top Rated 2010s" to "/films/ajax/by/rating/decade/2010s/page/1/",
        "Top Rated 2000s" to "/films/ajax/by/rating/decade/2000s/page/1/",
        "Top Rated 90s" to "/films/ajax/by/rating/decade/1990s/page/1/",

        // Popular by genre
        "Popular Sci-Fi" to "/films/ajax/popular/genre/science-fiction/page/1/",
        "Popular Thriller" to "/films/ajax/popular/genre/thriller/page/1/",
        "Popular Horror" to "/films/ajax/popular/genre/horror/page/1/",
        "Popular Romance" to "/films/ajax/popular/genre/romance/page/1/",
        "Popular Comedy" to "/films/ajax/popular/genre/comedy/page/1/",
        "Popular Drama" to "/films/ajax/popular/genre/drama/page/1/",
        "Popular Mystery" to "/films/ajax/popular/genre/mystery/page/1/",
        "Popular Crime" to "/films/ajax/popular/genre/crime/page/1/",
        "Popular Animation" to "/films/ajax/popular/genre/animation/page/1/",
        "Popular War" to "/films/ajax/popular/genre/war/page/1/",
        "Popular Music" to "/films/ajax/popular/genre/music/page/1/",
        "Popular Documentary" to "/films/ajax/popular/genre/documentary/page/1/",

        // Top rated by genre
        "Top Rated Drama" to "/films/ajax/by/rating/genre/drama/page/1/",
        "Top Rated Comedy" to "/films/ajax/by/rating/genre/comedy/page/1/",
        "Top Rated Animation" to "/films/ajax/by/rating/genre/animation/page/1/",
        "Top Rated Romance" to "/films/ajax/by/rating/genre/romance/page/1/",
        "Top Rated Thriller" to "/films/ajax/by/rating/genre/thriller/page/1/",
        "Top Rated Crime" to "/films/ajax/by/rating/genre/crime/page/1/",
        "Top Rated Sci-Fi" to "/films/ajax/by/rating/genre/science-fiction/page/1/",
        "Top Rated Horror" to "/films/ajax/by/rating/genre/horror/page/1/",
        "Top Rated War" to "/films/ajax/by/rating/genre/war/page/1/",
        "Top Rated Documentary" to "/films/ajax/by/rating/genre/documentary/page/1/",
        "Top Rated Mystery" to "/films/ajax/by/rating/genre/mystery/page/1/",
        "Top Rated Music" to "/films/ajax/by/rating/genre/music/page/1/",

        // Countries
        "Popular Korean" to "/films/ajax/popular/country/south-korea/page/1/",
        "Popular Japanese" to "/films/ajax/popular/country/japan/page/1/",
        "Popular Italian" to "/films/ajax/popular/country/italy/page/1/",
        "Popular Spanish" to "/films/ajax/popular/country/spain/page/1/",
        "Popular German" to "/films/ajax/popular/country/germany/page/1/",
        "Popular Chinese" to "/films/ajax/popular/country/china/page/1/",
        "Popular Iranian" to "/films/ajax/popular/country/iran/page/1/",
        "Popular French" to "/films/ajax/popular/country/france/page/1/",
        "Popular Indian" to "/films/ajax/popular/country/india/page/1/",
    )

    fun getAvailableCategories(): List<Pair<String, String>> = allEndpoints

    suspend fun scrapeCategory(path: String, page: Int = 1): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        val pagedPath = path.replace("/page/1/", "/page/$page/")
        scrapeAjax("$BASE_URL$pagedPath")
    }

    suspend fun getPopularThisWeek(): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        scrapeAjax("$BASE_URL/films/ajax/popular/this/week/page/1/")
    }

    suspend fun getPopularLists(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/lists/popular/this/week/")
                .header("User-Agent", UA)
                .build()
            val body = client.newCall(request).execute().body?.string() ?: return@withContext emptyList()

            val hrefRegex = Regex("""href="(/([^/]+)/list/([^/"]+)/)"[^>]*>([^<]+)""")
            val results = hrefRegex.findAll(body).map { m ->
                val name = m.groupValues[4].trim().let {
                    if (it.length > 50) it.take(47) + "..." else it
                }
                val path = "${m.groupValues[1]}page/1/"
                name to path
            }.toList().distinctBy { it.second }.take(20)

            Log.i(TAG, "Found ${results.size} popular lists")
            results
        } catch (e: Exception) {
            Log.w(TAG, "Lists fetch failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun scrapeList(path: String, page: Int = 1): List<LetterboxdFilm> = withContext(Dispatchers.IO) {
        val pagedPath = path.replace("/page/1/", "/page/$page/")
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
