package com.moviematcher.app.data.repository

import com.moviematcher.app.data.remote.api.TmdbApi
import com.moviematcher.app.data.remote.dto.TmdbGenreDto
import com.moviematcher.app.data.remote.dto.TmdbGenresResponse
import com.moviematcher.app.data.remote.dto.TmdbMovieDto
import com.moviematcher.app.data.remote.dto.TmdbMoviesResponse
import com.moviematcher.app.data.remote.dto.TmdbProviderDto
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersRegion
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersResponse
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TmdbRepositoryTest {
    
    @Mock
    private lateinit var tmdbApi: TmdbApi
    
    private lateinit var repository: TmdbRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TmdbRepository(tmdbApi)
    }
    
    @Test
    fun `getTrendingMovies returns mapped movies successfully`() = runTest {
        // Given
        val movieDto = createTestMovieDto()
        val moviesResponse = TmdbMoviesResponse(
            page = 1,
            results = listOf(movieDto),
            totalPages = 1,
            totalResults = 1
        )
        val genresResponse = TmdbGenresResponse(
            genres = listOf(TmdbGenreDto(28, "Action"))
        )
        
        whenever(tmdbApi.getTrendingMovies(any(), eq(1))).thenReturn(moviesResponse)
        whenever(tmdbApi.getMovieGenres(any())).thenReturn(genresResponse)
        
        // When
        val result = repository.getTrendingMovies(1)
        
        // Then
        assertEquals(1, result.size)
        assertEquals(123L, result[0].id)
        assertEquals("Test Movie", result[0].title)
    }
    
    @Test
    fun `getTrendingMovies throws MovieRepositoryException on API failure`() = runTest {
        // Given
        whenever(tmdbApi.getTrendingMovies(any(), any())).thenThrow(RuntimeException("API Error"))
        
        // When & Then
        assertFailsWith<MovieRepositoryException> {
            repository.getTrendingMovies(1)
        }
    }
    
    @Test
    fun `searchMovies returns mapped movies successfully`() = runTest {
        // Given
        val movieDto = createTestMovieDto()
        val moviesResponse = TmdbMoviesResponse(
            page = 1,
            results = listOf(movieDto),
            totalPages = 1,
            totalResults = 1
        )
        val genresResponse = TmdbGenresResponse(
            genres = listOf(TmdbGenreDto(28, "Action"))
        )
        
        whenever(tmdbApi.searchMovies(any(), eq("test"), eq(1))).thenReturn(moviesResponse)
        whenever(tmdbApi.getMovieGenres(any())).thenReturn(genresResponse)
        
        // When
        val result = repository.searchMovies("test", 1)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("Test Movie", result[0].title)
    }
    
    @Test
    fun `searchMovies throws MovieRepositoryException on API failure`() = runTest {
        // Given
        whenever(tmdbApi.searchMovies(any(), any(), any())).thenThrow(RuntimeException("API Error"))
        
        // When & Then
        assertFailsWith<MovieRepositoryException> {
            repository.searchMovies("test", 1)
        }
    }
    
    @Test
    fun `getMovieDetails returns mapped movie successfully`() = runTest {
        // Given
        val movieDto = createTestMovieDto().copy(
            genres = listOf(TmdbGenreDto(28, "Action"))
        )
        
        whenever(tmdbApi.getMovieDetails(eq(123L), any())).thenReturn(movieDto)
        
        // When
        val result = repository.getMovieDetails(123L)
        
        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Movie", result.title)
        assertEquals(1, result.genres.size)
        assertEquals("Action", result.genres[0].name)
    }
    
    @Test
    fun `getMovieDetails throws MovieRepositoryException on API failure`() = runTest {
        // Given
        whenever(tmdbApi.getMovieDetails(any(), any())).thenThrow(RuntimeException("API Error"))
        
        // When & Then
        assertFailsWith<MovieRepositoryException> {
            repository.getMovieDetails(123L)
        }
    }
    
    @Test
    fun `getStreamingProviders returns mapped providers successfully`() = runTest {
        // Given
        val providerDto = TmdbProviderDto(8, "Netflix", "/netflix.jpg")
        val usRegion = TmdbWatchProvidersRegion(
            link = "https://www.themoviedb.org/movie/123/watch",
            flatrate = listOf(providerDto),
            rent = null,
            buy = null
        )
        val providersResponse = TmdbWatchProvidersResponse(
            id = 123L,
            results = mapOf("US" to usRegion)
        )
        
        whenever(tmdbApi.getWatchProviders(eq(123L), any())).thenReturn(providersResponse)
        
        // When
        val result = repository.getStreamingProviders(123L)
        
        // Then
        assertEquals(1, result.size)
        assertEquals(8, result[0].id)
        assertEquals("Netflix", result[0].name)
    }
    
    @Test
    fun `getStreamingProviders returns empty list when no US providers`() = runTest {
        // Given
        val providersResponse = TmdbWatchProvidersResponse(
            id = 123L,
            results = mapOf("UK" to TmdbWatchProvidersRegion(null, null, null, null))
        )
        
        whenever(tmdbApi.getWatchProviders(eq(123L), any())).thenReturn(providersResponse)
        
        // When
        val result = repository.getStreamingProviders(123L)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `getStreamingProviders throws MovieRepositoryException on API failure`() = runTest {
        // Given
        whenever(tmdbApi.getWatchProviders(any(), any())).thenThrow(RuntimeException("API Error"))
        
        // When & Then
        assertFailsWith<MovieRepositoryException> {
            repository.getStreamingProviders(123L)
        }
    }
    
    @Test
    fun `clearGenreCache clears the cache`() = runTest {
        // Given - First call to populate cache
        val genresResponse = TmdbGenresResponse(
            genres = listOf(TmdbGenreDto(28, "Action"))
        )
        whenever(tmdbApi.getMovieGenres(any())).thenReturn(genresResponse)
        
        val movieDto = createTestMovieDto()
        val moviesResponse = TmdbMoviesResponse(1, listOf(movieDto), 1, 1)
        whenever(tmdbApi.getTrendingMovies(any(), any())).thenReturn(moviesResponse)
        
        // Populate cache
        repository.getTrendingMovies(1)
        
        // When
        repository.clearGenreCache()
        
        // Then - Next call should fetch genres again
        repository.getTrendingMovies(1)
        
        // Verify genres API was called twice (once for initial cache, once after clear)
        org.mockito.kotlin.verify(tmdbApi, org.mockito.kotlin.times(2)).getMovieGenres(any())
    }
    
    private fun createTestMovieDto() = TmdbMovieDto(
        id = 123L,
        title = "Test Movie",
        overview = "Test overview",
        posterPath = "/test.jpg",
        releaseDate = "2023-01-01",
        voteAverage = 7.5,
        genreIds = listOf(28),
        genres = null,
        runtime = 120
    )
}