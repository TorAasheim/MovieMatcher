package com.moviematcher.app.data.remote.mapper

import com.moviematcher.app.data.remote.dto.TmdbGenreDto
import com.moviematcher.app.data.remote.dto.TmdbMovieDto
import com.moviematcher.app.data.remote.dto.TmdbProviderDto
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersRegion
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmdbMapperTest {
    
    @Test
    fun `mapToMovie converts DTO with genres correctly`() {
        val genreDto = TmdbGenreDto(id = 28, name = "Action")
        val movieDto = TmdbMovieDto(
            id = 123L,
            title = "Test Movie",
            overview = "Test overview",
            posterPath = "/test.jpg",
            releaseDate = "2023-01-01",
            voteAverage = 7.5,
            genreIds = null,
            genres = listOf(genreDto),
            runtime = 120
        )
        
        val result = TmdbMapper.mapToMovie(movieDto)
        
        assertEquals(123L, result.id)
        assertEquals("Test Movie", result.title)
        assertEquals("Test overview", result.overview)
        assertEquals("/test.jpg", result.posterPath)
        assertEquals("2023-01-01", result.releaseDate)
        assertEquals(7.5, result.voteAverage)
        assertEquals(120, result.runtime)
        assertEquals(1, result.genres.size)
        assertEquals(28, result.genres[0].id)
        assertEquals("Action", result.genres[0].name)
    }
    
    @Test
    fun `mapToMovie converts DTO with genre IDs correctly`() {
        val movieDto = TmdbMovieDto(
            id = 123L,
            title = "Test Movie",
            overview = "Test overview",
            posterPath = "/test.jpg",
            releaseDate = "2023-01-01",
            voteAverage = 7.5,
            genreIds = listOf(28, 35),
            genres = null,
            runtime = 120
        )
        val genreMap = mapOf(28 to "Action", 35 to "Comedy")
        
        val result = TmdbMapper.mapToMovie(movieDto, genreMap)
        
        assertEquals(2, result.genres.size)
        assertEquals("Action", result.genres.find { it.id == 28 }?.name)
        assertEquals("Comedy", result.genres.find { it.id == 35 }?.name)
    }
    
    @Test
    fun `mapToMovie handles missing genres gracefully`() {
        val movieDto = TmdbMovieDto(
            id = 123L,
            title = "Test Movie",
            overview = "Test overview",
            posterPath = null,
            releaseDate = null,
            voteAverage = 7.5,
            genreIds = null,
            genres = null,
            runtime = null
        )
        
        val result = TmdbMapper.mapToMovie(movieDto)
        
        assertEquals(123L, result.id)
        assertEquals("Test Movie", result.title)
        assertNull(result.posterPath)
        assertNull(result.releaseDate)
        assertNull(result.runtime)
        assertEquals(0, result.genres.size)
    }
    
    @Test
    fun `mapToGenre converts DTO correctly`() {
        val genreDto = TmdbGenreDto(id = 28, name = "Action")
        
        val result = TmdbMapper.mapToGenre(genreDto)
        
        assertEquals(28, result.id)
        assertEquals("Action", result.name)
    }
    
    @Test
    fun `mapToStreamingProvider converts DTO correctly`() {
        val providerDto = TmdbProviderDto(
            providerId = 8,
            providerName = "Netflix",
            logoPath = "/netflix.jpg"
        )
        val deepLinkUrl = "https://www.netflix.com/title/123"
        
        val result = TmdbMapper.mapToStreamingProvider(providerDto, deepLinkUrl)
        
        assertEquals(8, result.id)
        assertEquals("Netflix", result.name)
        assertEquals("https://image.tmdb.org/t/p/w154/netflix.jpg", result.logoPath)
        assertEquals(deepLinkUrl, result.deepLinkUrl)
    }
    
    @Test
    fun `extractStreamingProviders combines all provider types`() {
        val flatrateProvider = TmdbProviderDto(8, "Netflix", "/netflix.jpg")
        val rentProvider = TmdbProviderDto(3, "Google Play", "/google.jpg")
        val buyProvider = TmdbProviderDto(2, "Apple TV", "/apple.jpg")
        
        val region = TmdbWatchProvidersRegion(
            link = "https://www.themoviedb.org/movie/123/watch",
            flatrate = listOf(flatrateProvider),
            rent = listOf(rentProvider),
            buy = listOf(buyProvider)
        )
        
        val result = TmdbMapper.extractStreamingProviders(region)
        
        assertEquals(3, result.size)
        assertEquals("Netflix", result.find { it.id == 8 }?.name)
        assertEquals("Google Play", result.find { it.id == 3 }?.name)
        assertEquals("Apple TV", result.find { it.id == 2 }?.name)
    }
    
    @Test
    fun `extractStreamingProviders removes duplicates`() {
        val provider = TmdbProviderDto(8, "Netflix", "/netflix.jpg")
        
        val region = TmdbWatchProvidersRegion(
            link = "https://www.themoviedb.org/movie/123/watch",
            flatrate = listOf(provider),
            rent = listOf(provider), // Same provider in multiple categories
            buy = null
        )
        
        val result = TmdbMapper.extractStreamingProviders(region)
        
        assertEquals(1, result.size) // Should only have one Netflix entry
        assertEquals("Netflix", result[0].name)
    }
}