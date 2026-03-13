package com.cineflux.data.api

import com.cineflux.data.model.TmdbMovieDetailResponse
import com.cineflux.data.model.TmdbSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/movie/week")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/popular")
    suspend fun getPopular(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/top_rated")
    suspend fun getTopRated(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetailResponse

    @GET("movie/{movie_id}/recommendations")
    suspend fun getRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilar(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/now_playing")
    suspend fun getNowPlaying(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/upcoming")
    suspend fun getUpcoming(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("discover/movie")
    suspend fun discoverByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") minVotes: Int = 200,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE = "w500"
        const val BACKDROP_SIZE = "w1280"

        fun posterUrl(path: String?): String? =
            path?.let { "${IMAGE_BASE_URL}${POSTER_SIZE}$it" }

        fun backdropUrl(path: String?): String? =
            path?.let { "${IMAGE_BASE_URL}${BACKDROP_SIZE}$it" }
    }
}
