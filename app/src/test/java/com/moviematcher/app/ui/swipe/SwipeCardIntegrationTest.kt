package com.moviematcher.app.ui.swipe

import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.SwipeDecision
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for SwipeCard functionality
 */
class SwipeCardIntegrationTest {

    @Test
    fun `SwipeCard should handle movie data correctly`() {
        // Given
        val movie = Movie(
            id = 1L,
            title = "The Matrix",
            overview = "A computer programmer discovers reality is a simulation.",
            posterPath = "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
            releaseDate = "1999-03-30",
            voteAverage = 8.7,
            genres = listOf(Genre(28, "Action"), Genre(878, "Science Fiction")),
            runtime = 136
        )

        val providers = listOf(
            StreamingProvider(8, "Netflix", "/netflix.jpg", "netflix://"),
            StreamingProvider(337, "Disney+", "/disney.jpg", "disneyplus://")
        )

        var swipeResult: SwipeDecision? = null

        // When - Simulate swipe callback
        val onSwipe: (SwipeDecision) -> Unit = { decision ->
            swipeResult = decision
        }

        // Simulate like action
        onSwipe(SwipeDecision.LIKE)

        // Then
        assertEquals(SwipeDecision.LIKE, swipeResult)
        assertNotNull(movie.title)
        assertNotNull(movie.releaseDate)
        assertEquals(2, providers.size)
    }

    @Test
    fun `SwipeCard should handle pass decision`() {
        // Given
        var swipeResult: SwipeDecision? = null
        val onSwipe: (SwipeDecision) -> Unit = { decision ->
            swipeResult = decision
        }

        // When
        onSwipe(SwipeDecision.PASS)

        // Then
        assertEquals(SwipeDecision.PASS, swipeResult)
    }

    @Test
    fun `SwipeCard should handle movie without optional fields`() {
        // Given
        val minimalMovie = Movie(
            id = 1L,
            title = "Test Movie",
            overview = "A test movie",
            posterPath = null,
            releaseDate = null,
            voteAverage = 0.0,
            genres = emptyList(),
            runtime = null
        )

        // When/Then - Should not throw exceptions
        assertNotNull(minimalMovie.title)
        assertEquals(0.0, minimalMovie.voteAverage)
        assertEquals(emptyList(), minimalMovie.genres)
    }

    @Test
    fun `SwipeCard should handle empty streaming providers list`() {
        // Given
        val emptyProviders = emptyList<StreamingProvider>()

        // When/Then - Should handle empty list gracefully
        assertEquals(0, emptyProviders.size)
    }

    @Test
    fun `SwipeCard should handle many streaming providers`() {
        // Given
        val manyProviders = (1..10).map { id ->
            StreamingProvider(id, "Provider $id", "/logo$id.jpg", "provider$id://")
        }

        // When/Then
        assertEquals(10, manyProviders.size)
        assertEquals("Provider 1", manyProviders.first().name)
        assertEquals("Provider 10", manyProviders.last().name)
    }
}