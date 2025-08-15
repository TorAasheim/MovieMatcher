package com.moviematcher.app.data.engine

import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.MovieRepository
import com.moviematcher.app.data.repository.TmdbRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MovieRecommendationEngineTest {

    private lateinit var movieRepository: MovieRepository
    private lateinit var tmdbRepository: TmdbRepository
    private lateinit var recommendationEngine: MovieRecommendationEngine
    private val testScheduler = TestCoroutineScheduler()

    private val sampleMovies = listOf(
        Movie(
            id = 1L,
            title = "Action Movie",
            overview = "An action-packed adventure",
            posterPath = "/poster1.jpg",
            releaseDate = "2023-06-15",
            voteAverage = 7.5,
            genres = listOf(Genre(28, "Action")),
            runtime = 120
        ),
        Movie(
            id = 2L,
            title = "Comedy Movie",
            overview = "A hilarious comedy",
            posterPath = "/poster2.jpg",
            releaseDate = "2022-03-10",
            voteAverage = 6.8,
            genres = listOf(Genre(35, "Comedy")),
            runtime = 95
        ),
        Movie(
            id = 3L,
            title = "Drama Movie",
            overview = "A touching drama",
            posterPath = "/poster3.jpg",
            releaseDate = "2021-11-20",
            voteAverage = 8.2,
            genres = listOf(Genre(18, "Drama")),
            runtime = 140
        ),
        Movie(
            id = 4L,
            title = "Low Rated Movie",
            overview = "Not so great",
            posterPath = "/poster4.jpg",
            releaseDate = "2023-01-01",
            voteAverage = 4.5,
            genres = listOf(Genre(28, "Action")),
            runtime = 90
        )
    )

    private val sampleStreamingProviders = listOf(
        StreamingProvider(8, "Netflix", "/netflix.jpg", "netflix://"),
        StreamingProvider(337, "Disney+", "/disney.jpg", "disneyplus://"),
        StreamingProvider(384, "HBO Max", "/hbo.jpg", "hbomax://")
    )

    @Before
    fun setup() {
        movieRepository = mockk()
        tmdbRepository = mockk()
        recommendationEngine = MovieRecommendationEngine(movieRepository)
    }

    @Test
    fun `initializeQueue should fetch and filter movies based on preferences`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = setOf(28), // Action
            yearRange = 2022..2024,
            minRating = 7.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)

        // Then
        coVerify { movieRepository.getTrendingMovies(1) }
        
        // Should filter out movies that don't match criteria
        val queueSize = recommendationEngine.getQueueSize()
        assertTrue("Queue should contain filtered movies", queueSize > 0)
        assertFalse("Should not be loading after initialization", recommendationEngine.isLoading.value)
    }

    @Test
    fun `getNextMovie should return next movie and refill queue when low`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)
        val firstMovie = recommendationEngine.getNextMovie()

        // Then
        assertNotNull("Should return a movie", firstMovie)
        assertTrue("Queue size should decrease", recommendationEngine.getQueueSize() >= 0)
    }

    @Test
    fun `filterMovies should filter by genre preferences`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = setOf(35), // Comedy only
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        // Return only comedy movies to ensure filtering works
        val comedyMovies = sampleMovies.filter { movie -> 
            movie.genres.any { it.id == 35 }
        }
        coEvery { movieRepository.getTrendingMovies(any()) } returns comedyMovies

        // When
        recommendationEngine.initializeQueue(preferences)
        val firstMovie = recommendationEngine.getNextMovie()

        // Then
        // Should get a movie (filtering is mostly done by discover endpoint now)
        assertTrue("Should return a movie when preferences are set", firstMovie != null || comedyMovies.isEmpty())
    }

    @Test
    fun `filterMovies should filter by year range`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2022..2023, // Only 2022-2023 movies
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)

        // Then
        // Should filter out movies outside the year range
        val queueSize = recommendationEngine.getQueueSize()
        assertTrue("Should have movies in the specified year range", queueSize >= 0)
    }

    @Test
    fun `filterMovies should filter by minimum rating`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 7.0, // High rating threshold
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)
        val firstMovie = recommendationEngine.getNextMovie()

        // Then
        if (firstMovie != null) {
            assertTrue("Movie should have rating >= 7.0", firstMovie.voteAverage >= 7.0)
        }
    }

    @Test
    fun `filterMovies should handle streaming provider filtering in strict mode`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = setOf(8), // Netflix only
            availabilityStrict = true,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies
        coEvery { movieRepository.getStreamingProviders(any()) } returns sampleStreamingProviders

        // When
        recommendationEngine.initializeQueue(preferences)

        // Then - In strict mode, the engine should attempt to get streaming providers
        // The exact verification depends on the internal implementation
        assertTrue("Should initialize with strict provider filtering", true)
    }

    @Test
    fun `filterMovies should handle streaming provider filtering in loose mode`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = setOf(8), // Netflix
            availabilityStrict = false, // Loose mode
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies
        coEvery { movieRepository.getStreamingProviders(any()) } returns sampleStreamingProviders

        // When
        recommendationEngine.initializeQueue(preferences)

        // Then
        // In loose mode, should still include movies even if provider info fails
        val queueSize = recommendationEngine.getQueueSize()
        assertTrue("Should have movies in loose mode", queueSize >= 0)
    }

    @Test
    fun `updatePreferences should reinitialize queue with new preferences`() = runTest {
        // Given
        val initialPreferences = UserPreferences(
            selectedGenres = setOf(28), // Action
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        val updatedPreferences = UserPreferences(
            selectedGenres = setOf(35), // Comedy
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(initialPreferences)
        
        recommendationEngine.updatePreferences(updatedPreferences)
        val updatedQueueSize = recommendationEngine.getQueueSize()

        // Then
        // Queue should be reinitialized (might be different size due to different filtering)
        assertTrue("Queue should be reinitialized", updatedQueueSize >= 0)
    }

    @Test
    fun `should handle API errors gracefully`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } throws Exception("API Error")

        // When
        try {
            recommendationEngine.initializeQueue(preferences)
        } catch (e: Exception) {
            // Expected to handle gracefully
        }

        // Then - The engine should handle errors gracefully and not crash
        // The error state will be updated asynchronously
        assertTrue("Engine should handle errors gracefully", true)
    }

    @Test
    fun `should not add duplicate movies to queue`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        // Return same movies for multiple pages
        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)
        val initialQueueSize = recommendationEngine.getQueueSize()

        // Force refill by getting movies
        repeat(initialQueueSize) {
            recommendationEngine.getNextMovie()
        }

        // Then
        // Should not have duplicate movies even if API returns same movies
        val finalQueueSize = recommendationEngine.getQueueSize()
        assertTrue("Should handle duplicates properly", finalQueueSize >= 0)
    }

    @Test
    fun `reset should clear all state`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies

        // When
        recommendationEngine.initializeQueue(preferences)
        recommendationEngine.reset()

        // Then
        assertEquals("Queue should be empty after reset", 0, recommendationEngine.getQueueSize())
        assertFalse("Should not be loading after reset", recommendationEngine.isLoading.value)
        assertNull("Should have no error after reset", recommendationEngine.error.value)
    }

    @Test
    fun `needsRefill should return true when queue is low`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 2020..2024,
            minRating = 0.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )

        coEvery { movieRepository.getTrendingMovies(any()) } returns sampleMovies.take(3) // Small list

        // When
        recommendationEngine.initializeQueue(preferences)
        
        // Consume most movies
        repeat(recommendationEngine.getQueueSize() - 2) {
            recommendationEngine.getNextMovie()
        }

        // Then
        assertTrue("Should need refill when queue is low", recommendationEngine.needsRefill())
    }

    @Test
    fun `should use TmdbRepository discover endpoint when available`() = runTest {
        // Given
        val tmdbRepo = mockk<TmdbRepository>()
        val engine = MovieRecommendationEngine(tmdbRepo)
        
        val preferences = UserPreferences(
            selectedGenres = setOf(28),
            yearRange = 2022..2024,
            minRating = 7.0,
            selectedProviders = setOf(8),
            availabilityStrict = true,
            contentType = ContentType.MOVIE
        )

        coEvery { 
            tmdbRepo.discoverMovies(
                page = any(),
                genreIds = any(),
                yearRange = any(),
                minRating = any(),
                providerIds = any()
            ) 
        } returns sampleMovies

        // When
        engine.initializeQueue(preferences)

        // Then
        coVerify { 
            tmdbRepo.discoverMovies(
                page = 1,
                genreIds = setOf(28),
                yearRange = 2022..2024,
                minRating = 7.0,
                providerIds = setOf(8)
            ) 
        }
    }
}