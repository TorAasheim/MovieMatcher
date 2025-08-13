package com.moviematcher.app.data.repository

import com.moviematcher.app.BuildConfig
import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.remote.api.TmdbApi
import com.moviematcher.app.data.remote.mapper.TmdbMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MovieRepository using TMDB API
 */
@Singleton
class TmdbRepository @Inject constructor(
    private val tmdbApi: TmdbApi
) : MovieRepository {
    
    private val apiKey = BuildConfig.TMDB_API_KEY
    
    // Cache for genre mappings to avoid repeated API calls
    private var genreCache: Map<Int, String>? = null
    
    override suspend fun getTrendingMovies(page: Int): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTrendingMovies(apiKey, page)
            val genreMap = getGenreMap()
            
            response.results.map { movieDto ->
                TmdbMapper.mapToMovie(movieDto, genreMap)
            }
        } catch (e: Exception) {
            throw MovieRepositoryException("Failed to fetch trending movies", e)
        }
    }
    
    override suspend fun searchMovies(query: String, page: Int): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.searchMovies(apiKey, query, page)
            val genreMap = getGenreMap()
            
            response.results.map { movieDto ->
                TmdbMapper.mapToMovie(movieDto, genreMap)
            }
        } catch (e: Exception) {
            throw MovieRepositoryException("Failed to search movies", e)
        }
    }
    
    override suspend fun getMovieDetails(id: Long): Movie = withContext(Dispatchers.IO) {
        try {
            val movieDto = tmdbApi.getMovieDetails(id, apiKey)
            TmdbMapper.mapToMovie(movieDto)
        } catch (e: Exception) {
            throw MovieRepositoryException("Failed to fetch movie details for ID: $id", e)
        }
    }
    
    override suspend fun getStreamingProviders(movieId: Long): List<StreamingProvider> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getWatchProviders(movieId, apiKey)
            
            // For now, we'll focus on US providers, but this can be made configurable
            val usProviders = response.results["US"]
            
            if (usProviders != null) {
                TmdbMapper.extractStreamingProviders(usProviders)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            throw MovieRepositoryException("Failed to fetch streaming providers for movie ID: $movieId", e)
        }
    }
    
    /**
     * Get genre mapping from cache or fetch from API
     */
    private suspend fun getGenreMap(): Map<Int, String> {
        return genreCache ?: run {
            try {
                val response = tmdbApi.getMovieGenres(apiKey)
                val map = response.genres.associate { it.id to it.name }
                genreCache = map
                map
            } catch (e: Exception) {
                // If genre fetching fails, return empty map to avoid blocking movie fetching
                emptyMap()
            }
        }
    }
    
    /**
     * Clear the genre cache (useful for testing or if genres need to be refreshed)
     */
    fun clearGenreCache() {
        genreCache = null
    }
}

/**
 * Exception thrown when movie repository operations fail
 */
class MovieRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)