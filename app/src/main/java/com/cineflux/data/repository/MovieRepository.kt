package com.cineflux.data.repository

import com.cineflux.BuildConfig
import android.util.Log
import com.cineflux.data.api.Leet1337xScraper
import com.cineflux.data.api.LetterboxdScraper
import com.cineflux.data.api.PirateBayApi
import com.cineflux.data.api.TmdbApi
import com.cineflux.data.api.YtsApi
import com.cineflux.data.model.Movie
import com.cineflux.data.model.PirateBayResult
import com.cineflux.data.model.TmdbMovie
import com.cineflux.data.model.TmdbMovieDetailResponse
import com.cineflux.data.model.TorrentInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val ytsApi: YtsApi,
    private val pirateBayApi: PirateBayApi,
    private val letterboxdScraper: LetterboxdScraper,
    private val leet1337xScraper: Leet1337xScraper
) {
    private val apiKey = BuildConfig.TMDB_API_KEY

    suspend fun getTrending(page: Int = 1): List<Movie> {
        val response = tmdbApi.getTrending(apiKey, page)
        return response.results.map { it.toDomain() }
    }

    suspend fun getPopular(page: Int = 1): List<Movie> {
        val response = tmdbApi.getPopular(apiKey, page)
        return response.results.map { it.toDomain() }
    }

    suspend fun getTopRated(page: Int = 1): List<Movie> {
        val response = tmdbApi.getTopRated(apiKey, page)
        return response.results.map { it.toDomain() }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        if (query.isBlank()) return emptyList()
        val response = tmdbApi.searchMovies(apiKey, query)
        return response.results.map { it.toDomain() }
    }

    suspend fun getRecommendations(movieId: Int): List<Movie> {
        val response = tmdbApi.getRecommendations(movieId, apiKey)
        return response.results.map { it.toDomain() }
    }

    suspend fun getSimilar(movieId: Int): List<Movie> {
        val response = tmdbApi.getSimilar(movieId, apiKey)
        return response.results.map { it.toDomain() }
    }

    suspend fun getNowPlaying(): List<Movie> {
        val response = tmdbApi.getNowPlaying(apiKey)
        return response.results.map { it.toDomain() }
    }

    suspend fun getUpcoming(): List<Movie> {
        val response = tmdbApi.getUpcoming(apiKey)
        return response.results.map { it.toDomain() }
    }

    suspend fun getByGenre(genreId: Int): List<Movie> {
        val response = tmdbApi.discoverByGenre(apiKey, genreId)
        return response.results.map { it.toDomain() }
    }

    suspend fun loadMoreLetterboxd(path: String, page: Int): List<Movie> = coroutineScope {
        val films = if (path.contains("/list/")) {
            letterboxdScraper.scrapeList(path, page)
        } else {
            letterboxdScraper.scrapeCategory(path, page)
        }
        if (films.isEmpty()) return@coroutineScope emptyList()
        films.map { film ->
            async {
                try {
                    val response = tmdbApi.searchMovies(apiKey, film.title, 1)
                    val tmdb = response.results.firstOrNull() ?: return@async null
                    val movie = tmdb.toDomain()
                    if (film.rating > 0) movie.copy(rating = film.rating) else movie
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getLetterboxdList(fetcher: suspend () -> List<com.cineflux.data.api.LetterboxdFilm>): List<Movie> = coroutineScope {
        val films = fetcher()
        if (films.isEmpty()) return@coroutineScope emptyList()
        films.map { film ->
            async {
                try {
                    val response = tmdbApi.searchMovies(apiKey, film.title, 1)
                    val tmdb = response.results.firstOrNull() ?: return@async null
                    val movie = tmdb.toDomain()
                    if (film.rating > 0) movie.copy(rating = film.rating) else movie
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getMovieDetails(movieId: Int): Movie {
        val detail = tmdbApi.getMovieDetails(movieId, apiKey)
        return detail.toDomain()
    }

    suspend fun getMovieWithTorrents(movieId: Int): Movie = coroutineScope {
        val detailDeferred = async { tmdbApi.getMovieDetails(movieId, apiKey) }
        val detail = detailDeferred.await()

        val year = detail.releaseDate.take(4)

        var torrents = try {
            Log.i("MovieRepo", "Trying 1337x for '${detail.title}'")
            leet1337xScraper.search(detail.title, year)
        } catch (e: Exception) {
            Log.w("MovieRepo", "1337x failed: ${e.message}")
            emptyList()
        }

        if (torrents.isEmpty()) {
            torrents = try {
                Log.i("MovieRepo", "Trying TPB for '${detail.title}'")
                val query = "${detail.title} $year"
                val results = pirateBayApi.search(query)
                    .filter { it.seedCount > 0 && it.info_hash.length == 40 }
                    .sortedByDescending { it.seedCount }
                    .take(6)
                results.map { it.toDomain() }
            } catch (e: Exception) {
                Log.w("MovieRepo", "TPB failed: ${e.message}")
                emptyList()
            }
        }

        if (torrents.isEmpty()) {
            torrents = try {
                Log.i("MovieRepo", "Trying YTS for '${detail.title}'")
                val ytsResponse = ytsApi.searchMovies(detail.title)
                val ytsMovie = ytsResponse.data.movies
                    ?.firstOrNull {
                        it.title.equals(detail.title, ignoreCase = true) ||
                        it.title.contains(detail.title, ignoreCase = true) ||
                        detail.title.contains(it.title, ignoreCase = true)
                    }
                    ?: ytsResponse.data.movies?.firstOrNull()
                ytsMovie?.torrents?.map { it.toDomain() } ?: emptyList()
            } catch (e: Exception) {
                Log.w("MovieRepo", "YTS failed: ${e.message}")
                emptyList()
            }
        }

        detail.toDomain(dedup(torrents))
    }

    private fun dedup(torrents: List<TorrentInfo>): List<TorrentInfo> {
        return torrents
            .filter { normalizeQuality(it.quality) != it.quality || it.quality in listOf("720p","1080p","2160p","4K","480p") }
            .groupBy { normalizeQuality(it.quality) }
            .mapValues { (_, v) -> v.maxByOrNull { it.seeds }!! }
            .values
            .sortedByDescending { qualityRank(it.quality) }
            .toList()
    }

    private fun normalizeQuality(q: String): String = when {
        q.contains("2160", ignoreCase = true) || q.contains("4K", ignoreCase = true) -> "2160p"
        q.contains("1080", ignoreCase = true) -> "1080p"
        q.contains("720", ignoreCase = true) -> "720p"
        q.contains("480", ignoreCase = true) -> "480p"
        else -> q
    }

    private fun qualityRank(q: String): Int = when (normalizeQuality(q)) {
        "2160p" -> 3
        "1080p" -> 2
        "720p" -> 1
        else -> 0
    }

    private fun TmdbMovie.toDomain() = Movie(
        tmdbId = id,
        title = title,
        overview = overview,
        posterUrl = TmdbApi.posterUrl(posterPath),
        backdropUrl = TmdbApi.backdropUrl(backdropPath),
        releaseDate = releaseDate,
        rating = voteAverage
    )

    private fun TmdbMovieDetailResponse.toDomain(torrents: List<TorrentInfo> = emptyList()) = Movie(
        tmdbId = id,
        title = title,
        overview = overview,
        posterUrl = TmdbApi.posterUrl(posterPath),
        backdropUrl = TmdbApi.backdropUrl(backdropPath),
        releaseDate = releaseDate,
        rating = voteAverage,
        runtime = runtime,
        genres = genres.map { it.name },
        tagline = tagline,
        torrents = torrents
    )

    private fun com.cineflux.data.model.YtsTorrent.toDomain() = TorrentInfo(
        hash = hash,
        quality = quality,
        type = type,
        seeds = seeds,
        peers = peers,
        size = size,
        sizeBytes = sizeBytes
    )

    private fun PirateBayResult.toDomain() = TorrentInfo(
        hash = info_hash,
        quality = qualityGuess,
        type = name,
        seeds = seedCount,
        peers = peerCount,
        size = sizeText,
        sizeBytes = sizeBytes,
        source = com.cineflux.data.model.TorrentSource.TPB
    )
}
