package com.moviematcher.app.ui.matches

import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.Match
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.repository.MatchRepository
import com.moviematcher.app.data.repository.MovieRepository
import com.moviematcher.app.data.repository.PreferencesRepository
import com.moviematcher.app.domain.SuggestionAlgorithm
import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModelTest {

    private lateinit var matchRepository: MatchRepository
    private lateinit var movieRepository: MovieRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var suggestionAlgorithm: SuggestionAlgorithm
    private lateinit var viewModel: MatchViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testMovie = Movie(
        id = 1L,
        title = "Test Movie",
        overview = "A test movie",
        posterPath = "/test.jpg",
        releaseDate = "2023-01-01",
        voteAverage = 8.5,
        genres = listOf(Genre(1, "Action")),
        runtime = 120
    )

    private val testMatch = Match(
        titleId = 1L,
        timestamp = System.currentTimeMillis(),
        watched = false,
        notes = ""
    )

    private val testProvider = StreamingProvider(
        id = 1,
        name = "Netflix",
        logoPath = "/netflix.jpg",
        deepLinkUrl = "https://netflix.com/title/123"
    )

    private val testPreferences = UserPreferences(
        selectedGenres = emptySet(),
        yearRange = 1990..2024,
        minRating = 6.0,
        selectedProviders = emptySet(),
        availabilityStrict = false,
        contentType = ContentType.MOVIE
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        matchRepository = mockk()
        movieRepository = mockk()
        preferencesRepository = mockk()
        suggestionAlgorithm = mockk()
        viewModel = MatchViewModel(matchRepository, movieRepository, preferencesRepository, suggestionAlgorithm)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initializeMatches should load matches and enrich them with movie details`() = runTest {
        // Given
        val roomId = "test-room"
        val matches = listOf(testMatch)
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(matches)
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { movieRepository.getMovieDetails(1L) } returns testMovie
        coEvery { movieRepository.getStreamingProviders(1L) } returns listOf(testProvider)
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns null

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()

        // Then
        val enrichedMatches = viewModel.enrichedMatches.value
        assertEquals(1, enrichedMatches.size)
        assertEquals(testMovie, enrichedMatches[0].movieDetails)
        assertEquals(listOf(testProvider), enrichedMatches[0].streamingProviders)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `initializeMatches should handle movie details fetch error gracefully`() = runTest {
        // Given
        val roomId = "test-room"
        val matches = listOf(testMatch)
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(matches)
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { movieRepository.getMovieDetails(1L) } throws Exception("API Error")
        coEvery { movieRepository.getStreamingProviders(1L) } returns emptyList()
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns null

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()

        // Then
        val enrichedMatches = viewModel.enrichedMatches.value
        assertEquals(1, enrichedMatches.size)
        assertNull(enrichedMatches[0].movieDetails)
        assertTrue(enrichedMatches[0].streamingProviders.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error) // Should not error on individual movie fetch failure
    }

    @Test
    fun `initializeMatches should handle repository error`() = runTest {
        // Given
        val roomId = "test-room"
        val error = Exception("Repository error")
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(emptyList())
        every { preferencesRepository.observePreferences(roomId) } returns kotlinx.coroutines.flow.flow {
            throw error
        }

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.error?.contains("Repository error") == true)
    }

    @Test
    fun `markAsWatched should call repository and handle success`() = runTest {
        // Given
        val roomId = "test-room"
        val movieId = 1L
        val notes = "Great movie!"
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(emptyList())
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { matchRepository.markAsWatched(roomId, movieId, notes) } returns Unit
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns null

        // When
        viewModel.initializeMatches(roomId)
        viewModel.markAsWatched(movieId, notes)
        advanceUntilIdle()

        // Then
        coVerify { matchRepository.markAsWatched(roomId, movieId, notes) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `markAsWatched should handle repository error`() = runTest {
        // Given
        val roomId = "test-room"
        val movieId = 1L
        val notes = "Great movie!"
        val error = Exception("Update failed")
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(emptyList())
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { matchRepository.markAsWatched(roomId, movieId, notes) } throws error
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns null

        // When
        viewModel.initializeMatches(roomId)
        viewModel.markAsWatched(movieId, notes)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Failed to mark as watched: Update failed", viewModel.uiState.value.error)
    }

    @Test
    fun `openStreamingProvider should return deep link when available`() {
        // Given
        val provider = testProvider
        val movieTitle = "Test Movie"

        // When
        val result = viewModel.openStreamingProvider(provider, movieTitle)

        // Then
        assertEquals("https://netflix.com/title/123", result)
    }

    @Test
    fun `openStreamingProvider should return web fallback when deep link unavailable`() {
        // Given
        val provider = StreamingProvider(1, "Netflix", "/logo.jpg", null)
        val movieTitle = "Test Movie"

        // When
        val result = viewModel.openStreamingProvider(provider, movieTitle)

        // Then
        assertEquals("https://www.netflix.com/search?q=Test%20Movie", result)
    }

    @Test
    fun `openStreamingProvider should handle various provider names for web fallback`() {
        val testCases = mapOf(
            "Amazon Prime Video" to "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=Test%20Movie",
            "Disney Plus" to "https://www.disneyplus.com/search?q=Test%20Movie",
            "Hulu" to "https://www.hulu.com/search?q=Test%20Movie",
            "HBO Max" to "https://www.max.com/search?q=Test%20Movie",
            "Apple TV Plus" to "https://tv.apple.com/search?term=Test%20Movie",
            "Unknown Provider" to "https://www.google.com/search?q=Test%20Movie+Unknown%20Provider+watch+online"
        )

        testCases.forEach { (providerName, expectedUrl) ->
            // Given
            val provider = StreamingProvider(1, providerName, "/logo.jpg", null)
            val movieTitle = "Test Movie"

            // When
            val result = viewModel.openStreamingProvider(provider, movieTitle)

            // Then
            assertEquals("Failed for provider: $providerName", expectedUrl, result)
        }
    }

    @Test
    fun `tonightsPick should be updated when matches change`() = runTest {
        // Given
        val roomId = "test-room"
        val matches = listOf(testMatch)
        val enrichedMatch = EnrichedMatch(
            match = testMatch,
            movieDetails = testMovie,
            streamingProviders = listOf(testProvider)
        )
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(matches)
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { movieRepository.getMovieDetails(1L) } returns testMovie
        coEvery { movieRepository.getStreamingProviders(1L) } returns listOf(testProvider)
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns enrichedMatch

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()

        // Then
        assertEquals(enrichedMatch, viewModel.tonightsPick.value)
    }

    @Test
    fun `refreshTonightsPick should update suggestion`() = runTest {
        // Given
        val roomId = "test-room"
        val matches = listOf(testMatch)
        val enrichedMatch = EnrichedMatch(
            match = testMatch,
            movieDetails = testMovie,
            streamingProviders = listOf(testProvider)
        )
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(matches)
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { movieRepository.getMovieDetails(1L) } returns testMovie
        coEvery { movieRepository.getStreamingProviders(1L) } returns listOf(testProvider)
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns enrichedMatch

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()
        viewModel.refreshTonightsPick()

        // Then
        io.mockk.verify(atLeast = 2) { suggestionAlgorithm.suggestTonightsPick(any(), any()) }
    }

    @Test
    fun `getRankedSuggestions should return suggestions from algorithm`() = runTest {
        // Given
        val roomId = "test-room"
        val matches = listOf(testMatch)
        val enrichedMatch = EnrichedMatch(
            match = testMatch,
            movieDetails = testMovie,
            streamingProviders = listOf(testProvider)
        )
        val expectedSuggestions = listOf(enrichedMatch)
        
        every { matchRepository.observeMatches(roomId) } returns flowOf(matches)
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        coEvery { movieRepository.getMovieDetails(1L) } returns testMovie
        coEvery { movieRepository.getStreamingProviders(1L) } returns listOf(testProvider)
        every { suggestionAlgorithm.getSuggestionsRanked(any(), any(), any()) } returns expectedSuggestions

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()
        val suggestions = viewModel.getRankedSuggestions(5)

        // Then
        assertEquals(expectedSuggestions, suggestions)
        io.mockk.verify { suggestionAlgorithm.getSuggestionsRanked(any(), testPreferences, 5) }
    }

    @Test
    fun `clearError should reset error state`() {
        // Given
        viewModel.initializeMatches("invalid-room")
        // Assume error state is set

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `should not reinitialize matches for same room`() = runTest {
        // Given
        val roomId = "test-room"
        every { matchRepository.observeMatches(roomId) } returns flowOf(emptyList())
        every { preferencesRepository.observePreferences(roomId) } returns flowOf(testPreferences)
        every { suggestionAlgorithm.suggestTonightsPick(any(), any()) } returns null

        // When
        viewModel.initializeMatches(roomId)
        advanceUntilIdle()
        viewModel.initializeMatches(roomId) // Second call with same room
        advanceUntilIdle()

        // Then
        io.mockk.verify(exactly = 1) { matchRepository.observeMatches(roomId) }
    }
}