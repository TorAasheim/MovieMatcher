package com.moviematcher.app.data.remote.util

/**
 * Utility class for handling TMDB image URLs
 */
object TmdbImageUtil {
    
    private const val BASE_IMAGE_URL = "https://image.tmdb.org/t/p/"
    
    // Poster sizes available from TMDB
    enum class PosterSize(val size: String) {
        W92("w92"),
        W154("w154"),
        W185("w185"),
        W342("w342"),
        W500("w500"),
        W780("w780"),
        ORIGINAL("original")
    }
    
    // Logo sizes available from TMDB
    enum class LogoSize(val size: String) {
        W45("w45"),
        W92("w92"),
        W154("w154"),
        W185("w185"),
        W300("w300"),
        W500("w500"),
        ORIGINAL("original")
    }
    
    /**
     * Build full poster URL from TMDB poster path
     * @param posterPath The poster path from TMDB API (e.g., "/abc123.jpg")
     * @param size The desired poster size
     * @return Full URL to the poster image, or null if posterPath is null
     */
    fun buildPosterUrl(posterPath: String?, size: PosterSize = PosterSize.W500): String? {
        return if (posterPath != null) {
            "$BASE_IMAGE_URL${size.size}$posterPath"
        } else {
            null
        }
    }
    
    /**
     * Build full logo URL from TMDB logo path
     * @param logoPath The logo path from TMDB API (e.g., "/abc123.jpg")
     * @param size The desired logo size
     * @return Full URL to the logo image, or null if logoPath is null
     */
    fun buildLogoUrl(logoPath: String?, size: LogoSize = LogoSize.W154): String? {
        return if (logoPath != null) {
            "$BASE_IMAGE_URL${size.size}$logoPath"
        } else {
            null
        }
    }
    
    /**
     * Get the optimal poster size based on screen width
     * @param screenWidthDp Screen width in dp
     * @return Appropriate poster size
     */
    fun getOptimalPosterSize(screenWidthDp: Int): PosterSize {
        return when {
            screenWidthDp <= 200 -> PosterSize.W185
            screenWidthDp <= 400 -> PosterSize.W342
            screenWidthDp <= 600 -> PosterSize.W500
            else -> PosterSize.W780
        }
    }
}