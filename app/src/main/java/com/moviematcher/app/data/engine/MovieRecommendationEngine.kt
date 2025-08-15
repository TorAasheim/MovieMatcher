package com.moviematcher.app.data.engine

import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for managing movie recommendation queue with preference-based filtering
 * and automatic queue refilling
 */
@Singleton
class MovieRecommendationEngine @Inject constructor(
    private val movieRepository: MovieRepository
) {
    companion object {
        private const val MIN_QUEUE_SIZE = 5
        private const val TARGET_QUEUE_SIZE = 20
        private const val MAX_PAGES_TO_FETCH = 10
    }

    private val _movieQueue = MutableStateFlow<List<Movie>>(emptyList())
    val movieQueue: StateFlow<List<Movie>> = _movieQueue.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val queueMutex = Mutex()
    private var currentPage = 1
    private var currentPreferences: UserPreferences? = null
    private val seenMovieIds = mutableSetOf<Long>()

    /**
     * Initialize the recommendation queue with user preferences
     */
    suspend fun initializeQueue(preferences: UserPreferences) {
        queueMutex.withLock {
            currentPreferences = preferences
            currentPage = 1
            seenMovieIds.clear()
            _movieQueue.value = emptyList()
            _error.value = null
        }
        
        refillQueue()
    }

    /**
     * Get the next movie from the queue and automatically refill if needed
     */
    suspend fun getNextMovie(): Movie? {
        val currentQueue = _movieQueue.value
        
        if (currentQueue.isEmpty()) {
            refillQueue()
            return _movieQueue.value.firstOrNull()
        }

        val nextMovie = currentQueue.first()
        
        queueMutex.withLock {
            _movieQueue.value = currentQueue.drop(1)
        }

        // Refill queue if running low
        if (_movieQueue.value.size <= MIN_QUEUE_SIZE) {
            refillQueue()
        }

        return nextMovie
    }

    /**
     * Update preferences and refresh the queue
     */
    suspend fun updatePreferences(preferences: UserPreferences) {
        if (currentPreferences != preferences) {
            initializeQueue(preferences)
        }
    }

    /**
     * Refill the movie queue with filtered recommendations
     */
    private suspend fun refillQueue() {
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        try {
            val preferences = currentPreferences ?: return
            val newMovies = mutableListOf<Movie>()
            var pagesChecked = 0

            while (newMovies.size < TARGET_QUEUE_SIZE && pagesChecked < MAX_PAGES_TO_FETCH) {
                val movies = fetchMoviesPage(currentPage)
                val filteredMovies = filterMovies(movies, preferences)
                
                // Add movies that haven't been seen before
                val unseenMovies = filteredMovies.filter { movie ->
                    !seenMovieIds.contains(movie.id)
                }
                
                newMovies.addAll(unseenMovies)
                seenMovieIds.addAll(unseenMovies.map { it.id })
                
                currentPage++
                pagesChecked++
            }

            queueMutex.withLock {
                val currentQueue = _movieQueue.value.toMutableList()
                currentQueue.addAll(newMovies)
                _movieQueue.value = currentQueue
            }

        } catch (e: Exception) {
            _error.value = "Failed to load movie recommendations: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Fetch a page of movies from the repository
     */
    private suspend fun fetchMoviesPage(page: Int): List<Movie> {
        return try {
            val preferences = currentPreferences
            if (preferences != null && movieRepository is com.moviematcher.app.data.repository.TmdbRepository) {
                // Use discover endpoint with filters for better results
                val tmdbRepo = movieRepository
                tmdbRepo.discoverMovies(
                    page = page,
                    genreIds = preferences.selectedGenres.takeIf { it.isNotEmpty() },
                    yearRange = preferences.yearRange,
                    minRating = preferences.minRating.takeIf { it > 0.0 },
                    providerIds = preferences.selectedProviders.takeIf { it.isNotEmpty() && preferences.availabilityStrict }
                )
            } else {
                // Fallback to trending movies
                movieRepository.getTrendingMovies(page)
            }
        } catch (e: Exception) {
            // If discover fails, fallback to trending movies
            try {
                movieRepository.getTrendingMovies(page)
            } catch (fallbackException: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Filter movies based on user preferences
     * Note: Most filtering is done by the discover endpoint, this handles edge cases
     */
    private suspend fun filterMovies(movies: List<Movie>, preferences: UserPreferences): List<Movie> {
        return movies.filter { movie ->
            // Additional client-side filtering for edge cases
            // Most filtering is already done by the discover endpoint
            
            // Ensure minimum rating (in case API doesn't filter precisely)
            if (movie.voteAverage < preferences.minRating) {
                return@filter false
            }

            // Ensure year range (in case API doesn't filter precisely)
            val releaseYear = extractYearFromDate(movie.releaseDate)
            if (releaseYear != null && !preferences.yearRange.contains(releaseYear)) {
                return@filter false
            }

            true
        }.let { filteredMovies ->
            // Apply streaming provider filtering for loose mode
            if (preferences.selectedProviders.isNotEmpty() && !preferences.availabilityStrict) {
                filterByStreamingProviders(filteredMovies, preferences)
            } else {
                filteredMovies
            }
        }
    }

    /**
     * Filter movies by streaming provider availability
     */
    private suspend fun filterByStreamingProviders(
        movies: List<Movie>, 
        preferences: UserPreferences
    ): List<Movie> {
        if (!preferences.availabilityStrict && preferences.selectedProviders.isEmpty()) {
            return movies
        }

        return movies.filter { movie ->
            try {
                val providers = movieRepository.getStreamingProviders(movie.id)
                val providerIds = providers.map { it.id }.toSet()
                
                if (preferences.availabilityStrict) {
                    // Strict mode: movie must be available on at least one selected provider
                    providerIds.intersect(preferences.selectedProviders).isNotEmpty()
                } else {
                    // Loose mode: prioritize movies on selected providers but don't exclude others
                    true
                }
            } catch (e: Exception) {
                // If we can't get provider info, include the movie in loose mode, exclude in strict mode
                !preferences.availabilityStrict
            }
        }
    }

    /**
     * Extract year from release date string (YYYY-MM-DD format)
     */
    private fun extractYearFromDate(dateString: String?): Int? {
        return try {
            dateString?.substring(0, 4)?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset the engine state
     */
    suspend fun reset() {
        queueMutex.withLock {
            _movieQueue.value = emptyList()
            currentPage = 1
            currentPreferences = null
            seenMovieIds.clear()
            _error.value = null
            _isLoading.value = false
        }
    }

    /**
     * Get current queue size
     */
    fun getQueueSize(): Int = _movieQueue.value.size

    /**
     * Check if queue needs refilling
     */
    fun needsRefill(): Boolean = _movieQueue.value.size <= MIN_QUEUE_SIZE
}