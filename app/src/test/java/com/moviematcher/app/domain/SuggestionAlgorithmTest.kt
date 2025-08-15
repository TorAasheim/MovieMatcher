package com.moviematcher.app.domain

import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.Match
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.ui.matches.EnrichedMatch
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SuggestionAlgorithmTest {

    private lateinit var suggestionAlgorithm: SuggestionAlgorithm
    private lateinit var defaultPreferences: UserPreferences

    @Before
    fun setUp() {
        suggestionAlgorithm = SuggestionAlgorithm()
        defaultPreferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 1990..2024,
            minRating = 6.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )
    }

    @Test
    fun `suggestTonightsPick returns null when no matches available`() {
        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = emptyList(),
            preferences = defaultPreferences
        )

        assertNull(result)
    }

    @Test
    fun `suggestTonightsPick returns null when all matches are watched`() {
        val watchedMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Test Movie",
            rating = 8.0,
            releaseDate = "2023-01-01",
            watched = true
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(watchedMatch),
            preferences = defaultPreferences
        )

        assertNull(result)
    }

    @Test
    fun `suggestTonightsPick returns highest rated unwatched match`() {
        val lowRatedMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Low Rated Movie",
            rating = 6.0,
            releaseDate = "2023-01-01"
        )
        val highRatedMatch = createEnrichedMatch(
            movieId = 2L,
            title = "High Rated Movie",
            rating = 9.0,
            releaseDate = "2023-01-01"
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(lowRatedMatch, highRatedMatch),
            preferences = defaultPreferences
        )

        assertNotNull(result)
        assertEquals("High Rated Movie", result?.movieDetails?.title)
    }

    @Test
    fun `suggestTonightsPick prioritizes recent movies when ratings are equal`() {
        val olderMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Older Movie",
            rating = 8.0,
            releaseDate = "2020-01-01"
        )
        val newerMatch = createEnrichedMatch(
            movieId = 2L,
            title = "Newer Movie",
            rating = 8.0,
            releaseDate = "2023-01-01"
        )

        // Run multiple times to account for random tie-breaking
        var newerMovieSelected = 0
        val iterations = 100

        repeat(iterations) {
            val result = suggestionAlgorithm.suggestTonightsPick(
                matches = listOf(olderMatch, newerMatch),
                preferences = defaultPreferences
            )
            if (result?.movieDetails?.title == "Newer Movie") {
                newerMovieSelected++
            }
        }

        // Newer movie should be selected more often due to higher recency score
        assertTrue("Newer movie should be selected more often", newerMovieSelected > iterations / 2)
    }

    @Test
    fun `suggestTonightsPick filters by availability when strict mode enabled`() {
        val netflixProvider = StreamingProvider(id = 8, name = "Netflix", logoPath = null, deepLinkUrl = null)
        val huluProvider = StreamingProvider(id = 15, name = "Hulu", logoPath = null, deepLinkUrl = null)

        val netflixMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Netflix Movie",
            rating = 8.0,
            releaseDate = "2023-01-01",
            streamingProviders = listOf(netflixProvider)
        )
        val huluMatch = createEnrichedMatch(
            movieId = 2L,
            title = "Hulu Movie",
            rating = 9.0,
            releaseDate = "2023-01-01",
            streamingProviders = listOf(huluProvider)
        )

        val strictPreferences = defaultPreferences.copy(
            selectedProviders = setOf(8), // Only Netflix
            availabilityStrict = true
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(netflixMatch, huluMatch),
            preferences = strictPreferences
        )

        assertNotNull(result)
        assertEquals("Netflix Movie", result?.movieDetails?.title)
    }

    @Test
    fun `suggestTonightsPick returns null when strict mode enabled but no matches have selected providers`() {
        val amazonMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Amazon Movie",
            rating = 8.0,
            releaseDate = "2023-01-01",
            streamingProviders = listOf(
                StreamingProvider(id = 119, name = "Amazon Prime Video", logoPath = null, deepLinkUrl = null)
            )
        )

        val strictPreferences = defaultPreferences.copy(
            selectedProviders = setOf(8), // Only Netflix
            availabilityStrict = true
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(amazonMatch),
            preferences = strictPreferences
        )

        assertNull(result)
    }

    @Test
    fun `suggestTonightsPick ignores availability when strict mode disabled`() {
        val amazonMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Amazon Movie",
            rating = 8.0,
            releaseDate = "2023-01-01",
            streamingProviders = listOf(
                StreamingProvider(id = 119, name = "Amazon Prime Video", logoPath = null, deepLinkUrl = null)
            )
        )

        val nonStrictPreferences = defaultPreferences.copy(
            selectedProviders = setOf(8), // Only Netflix
            availabilityStrict = false
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(amazonMatch),
            preferences = nonStrictPreferences
        )

        assertNotNull(result)
        assertEquals("Amazon Movie", result?.movieDetails?.title)
    }

    @Test
    fun `getSuggestionsRanked returns matches sorted by score`() {
        val lowRatedMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Low Rated Movie",
            rating = 6.0,
            releaseDate = "2023-01-01"
        )
        val mediumRatedMatch = createEnrichedMatch(
            movieId = 2L,
            title = "Medium Rated Movie",
            rating = 7.5,
            releaseDate = "2023-01-01"
        )
        val highRatedMatch = createEnrichedMatch(
            movieId = 3L,
            title = "High Rated Movie",
            rating = 9.0,
            releaseDate = "2023-01-01"
        )

        val result = suggestionAlgorithm.getSuggestionsRanked(
            matches = listOf(lowRatedMatch, mediumRatedMatch, highRatedMatch),
            preferences = defaultPreferences,
            limit = 3
        )

        assertEquals(3, result.size)
        assertEquals("High Rated Movie", result[0].movieDetails?.title)
        assertEquals("Medium Rated Movie", result[1].movieDetails?.title)
        assertEquals("Low Rated Movie", result[2].movieDetails?.title)
    }

    @Test
    fun `getSuggestionsRanked respects limit parameter`() {
        val matches = (1..10).map { id ->
            createEnrichedMatch(
                movieId = id.toLong(),
                title = "Movie $id",
                rating = id.toDouble(),
                releaseDate = "2023-01-01"
            )
        }

        val result = suggestionAlgorithm.getSuggestionsRanked(
            matches = matches,
            preferences = defaultPreferences,
            limit = 3
        )

        assertEquals(3, result.size)
    }

    @Test
    fun `getSuggestionsRanked filters out watched matches`() {
        val unwatchedMatch = createEnrichedMatch(
            movieId = 1L,
            title = "Unwatched Movie",
            rating = 8.0,
            releaseDate = "2023-01-01",
            watched = false
        )
        val watchedMatch = createEnrichedMatch(
            movieId = 2L,
            title = "Watched Movie",
            rating = 9.0,
            releaseDate = "2023-01-01",
            watched = true
        )

        val result = suggestionAlgorithm.getSuggestionsRanked(
            matches = listOf(unwatchedMatch, watchedMatch),
            preferences = defaultPreferences,
            limit = 5
        )

        assertEquals(1, result.size)
        assertEquals("Unwatched Movie", result[0].movieDetails?.title)
    }

    @Test
    fun `score calculation handles null movie details gracefully`() {
        val matchWithNullMovie = EnrichedMatch(
            match = Match(
                titleId = 1L,
                timestamp = System.currentTimeMillis(),
                watched = false
            ),
            movieDetails = null,
            streamingProviders = emptyList()
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(matchWithNullMovie),
            preferences = defaultPreferences
        )

        // Should still return the match even with null movie details
        assertNotNull(result)
        assertEquals(1L, result?.match?.titleId)
    }

    @Test
    fun `score calculation handles invalid release dates gracefully`() {
        val matchWithInvalidDate = createEnrichedMatch(
            movieId = 1L,
            title = "Invalid Date Movie",
            rating = 8.0,
            releaseDate = "invalid-date"
        )

        val result = suggestionAlgorithm.suggestTonightsPick(
            matches = listOf(matchWithInvalidDate),
            preferences = defaultPreferences
        )

        assertNotNull(result)
        assertEquals("Invalid Date Movie", result?.movieDetails?.title)
    }

    @Test
    fun `score calculation extracts year from partial date strings`() {
        val match2023 = createEnrichedMatch(
            movieId = 1L,
            title = "2023 Movie",
            rating = 8.0,
            releaseDate = "2023"
        )
        val match2020 = createEnrichedMatch(
            movieId = 2L,
            title = "2020 Movie",
            rating = 8.0,
            releaseDate = "2020"
        )

        // Run multiple times to account for random tie-breaking
        var newerMovieSelected = 0
        val iterations = 100

        repeat(iterations) {
            val result = suggestionAlgorithm.suggestTonightsPick(
                matches = listOf(match2020, match2023),
                preferences = defaultPreferences
            )
            if (result?.movieDetails?.title == "2023 Movie") {
                newerMovieSelected++
            }
        }

        // 2023 movie should be selected more often due to higher recency score
        assertTrue("2023 movie should be selected more often", newerMovieSelected > iterations / 2)
    }

    private fun createEnrichedMatch(
        movieId: Long,
        title: String,
        rating: Double,
        releaseDate: String,
        watched: Boolean = false,
        streamingProviders: List<StreamingProvider> = emptyList()
    ): EnrichedMatch {
        return EnrichedMatch(
            match = Match(
                titleId = movieId,
                timestamp = System.currentTimeMillis(),
                watched = watched
            ),
            movieDetails = Movie(
                id = movieId,
                title = title,
                overview = "Test overview for $title",
                posterPath = "/test-poster.jpg",
                releaseDate = releaseDate,
                voteAverage = rating,
                genres = emptyList(),
                runtime = 120
            ),
            streamingProviders = streamingProviders
        )
    }
}