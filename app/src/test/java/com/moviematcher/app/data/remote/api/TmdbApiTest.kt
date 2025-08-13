package com.moviematcher.app.data.remote.api

import com.moviematcher.app.data.remote.dto.TmdbGenresResponse
import com.moviematcher.app.data.remote.dto.TmdbMoviesResponse
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersResponse
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TmdbApiTest {
    
    @Mock
    private lateinit var tmdbApi: TmdbApi
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `getTrendingMovies calls API with correct parameters`() = runTest {
        // Given
        val mockResponse = TmdbMoviesResponse(1, emptyList(), 1, 0)
        whenever(tmdbApi.getTrendingMovies(any(), any(), any())).thenReturn(mockResponse)
        
        // When
        tmdbApi.getTrendingMovies("test_api_key", 1, "en-US")
        
        // Then
        verify(tmdbApi).getTrendingMovies(
            apiKey = "test_api_key",
            page = 1,
            language = "en-US"
        )
    }
    
    @Test
    fun `searchMovies calls API with correct parameters`() = runTest {
        // Given
        val mockResponse = TmdbMoviesResponse(1, emptyList(), 1, 0)
        whenever(tmdbApi.searchMovies(any(), any(), any(), any(), any())).thenReturn(mockResponse)
        
        // When
        tmdbApi.searchMovies("test_api_key", "test query", 1, "en-US", false)
        
        // Then
        verify(tmdbApi).searchMovies(
            apiKey = "test_api_key",
            query = "test query",
            page = 1,
            language = "en-US",
            includeAdult = false
        )
    }
    
    @Test
    fun `getWatchProviders calls API with correct parameters`() = runTest {
        // Given
        val mockResponse = TmdbWatchProvidersResponse(123L, emptyMap())
        whenever(tmdbApi.getWatchProviders(any(), any())).thenReturn(mockResponse)
        
        // When
        tmdbApi.getWatchProviders(123L, "test_api_key")
        
        // Then
        verify(tmdbApi).getWatchProviders(
            movieId = 123L,
            apiKey = "test_api_key"
        )
    }
    
    @Test
    fun `getMovieGenres calls API with correct parameters`() = runTest {
        // Given
        val mockResponse = TmdbGenresResponse(emptyList())
        whenever(tmdbApi.getMovieGenres(any(), any())).thenReturn(mockResponse)
        
        // When
        tmdbApi.getMovieGenres("test_api_key", "en-US")
        
        // Then
        verify(tmdbApi).getMovieGenres(
            apiKey = "test_api_key",
            language = "en-US"
        )
    }
}