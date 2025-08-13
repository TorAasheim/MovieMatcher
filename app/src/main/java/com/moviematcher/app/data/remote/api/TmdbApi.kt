package com.moviematcher.app.data.remote.api

import com.moviematcher.app.data.remote.dto.TmdbGenresResponse
import com.moviematcher.app.data.remote.dto.TmdbMovieDto
import com.moviematcher.app.data.remote.dto.TmdbMoviesResponse
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB API interface for movie data
 */
interface TmdbApi {
    
    /**
     * Get trending movies
     */
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbMoviesResponse
    
    /**
     * Search for movies
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbMoviesResponse
    
    /**
     * Get movie details by ID
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDto
    
    /**
     * Get watch providers for a movie
     */
    @GET("movie/{movie_id}/watch/providers")
    suspend fun getWatchProviders(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String
    ): TmdbWatchProvidersResponse
    
    /**
     * Get list of movie genres
     */
    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbGenresResponse
}