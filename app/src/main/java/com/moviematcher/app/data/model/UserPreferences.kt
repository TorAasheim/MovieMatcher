package com.moviematcher.app.data.model

/**
 * Represents user preferences for filtering movie recommendations
 */
data class UserPreferences(
    val selectedGenres: Set<Int>,
    val yearRange: IntRange,
    val minRating: Double,
    val selectedProviders: Set<Int>,
    val availabilityStrict: Boolean,
    val contentType: ContentType
)