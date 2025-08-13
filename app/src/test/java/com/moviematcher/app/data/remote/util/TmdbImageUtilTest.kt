package com.moviematcher.app.data.remote.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmdbImageUtilTest {
    
    @Test
    fun `buildPosterUrl returns correct URL with default size`() {
        val posterPath = "/abc123.jpg"
        val expectedUrl = "https://image.tmdb.org/t/p/w500/abc123.jpg"
        
        val result = TmdbImageUtil.buildPosterUrl(posterPath)
        
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `buildPosterUrl returns correct URL with custom size`() {
        val posterPath = "/abc123.jpg"
        val expectedUrl = "https://image.tmdb.org/t/p/w342/abc123.jpg"
        
        val result = TmdbImageUtil.buildPosterUrl(posterPath, TmdbImageUtil.PosterSize.W342)
        
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `buildPosterUrl returns null when posterPath is null`() {
        val result = TmdbImageUtil.buildPosterUrl(null)
        
        assertNull(result)
    }
    
    @Test
    fun `buildLogoUrl returns correct URL with default size`() {
        val logoPath = "/logo123.jpg"
        val expectedUrl = "https://image.tmdb.org/t/p/w154/logo123.jpg"
        
        val result = TmdbImageUtil.buildLogoUrl(logoPath)
        
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `buildLogoUrl returns correct URL with custom size`() {
        val logoPath = "/logo123.jpg"
        val expectedUrl = "https://image.tmdb.org/t/p/w300/logo123.jpg"
        
        val result = TmdbImageUtil.buildLogoUrl(logoPath, TmdbImageUtil.LogoSize.W300)
        
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `buildLogoUrl returns null when logoPath is null`() {
        val result = TmdbImageUtil.buildLogoUrl(null)
        
        assertNull(result)
    }
    
    @Test
    fun `getOptimalPosterSize returns correct size for different screen widths`() {
        assertEquals(TmdbImageUtil.PosterSize.W185, TmdbImageUtil.getOptimalPosterSize(150))
        assertEquals(TmdbImageUtil.PosterSize.W342, TmdbImageUtil.getOptimalPosterSize(300))
        assertEquals(TmdbImageUtil.PosterSize.W500, TmdbImageUtil.getOptimalPosterSize(500))
        assertEquals(TmdbImageUtil.PosterSize.W780, TmdbImageUtil.getOptimalPosterSize(800))
    }
}