package com.moviematcher.app.data.repository

import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider

/**
 * Repository interface for movie data operations
 */
interface MovieRepository {
    /**
     * Get trending movies from TMDB
     */
    suspend fun getTrendingMovies(page: Int): List<Movie>
    
    /**
     * Search for movies by query
     */
    suspend fun searchMovies(query: String, page: Int): List<Movie>
    
    /**
     * Get detailed information for a specific movie
     */
    suspend fun getMovieDetails(id: Long): Movie
    
    /**
     * Get streaming providers for a specific movie
     */
    suspend fun getStreamingProviders(movieId: Long): List<StreamingProvider>
}