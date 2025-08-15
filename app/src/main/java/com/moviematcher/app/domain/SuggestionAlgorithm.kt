package com.moviematcher.app.domain

import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.ui.matches.EnrichedMatch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Algorithm for suggesting tonight's pick from available matches
 */
@Singleton
class SuggestionAlgorithm @Inject constructor() {

    companion object {
        private const val RATING_WEIGHT = 0.7
        private const val RECENCY_WEIGHT = 0.3
        private const val MAX_RELEASE_YEAR = 2024
        private const val MIN_RELEASE_YEAR = 1900
    }

    /**
     * Suggest the best match for tonight based on user preferences
     * @param matches List of enriched matches to choose from
     * @param preferences User preferences for filtering
     * @return The suggested match or null if no suitable matches found
     */
    fun suggestTonightsPick(
        matches: List<EnrichedMatch>,
        preferences: UserPreferences
    ): EnrichedMatch? {
        // Filter out watched matches
        val unwatchedMatches = matches.filter { !it.match.watched }
        
        if (unwatchedMatches.isEmpty()) {
            return null
        }

        // Apply availability filtering if strict mode is enabled
        val filteredMatches = if (preferences.availabilityStrict && preferences.selectedProviders.isNotEmpty()) {
            unwatchedMatches.filter { match ->
                hasAvailableProvider(match.streamingProviders, preferences.selectedProviders)
            }
        } else {
            unwatchedMatches
        }

        if (filteredMatches.isEmpty()) {
            return null
        }

        // Calculate scores for each match
        val scoredMatches = filteredMatches.map { match ->
            ScoredMatch(
                match = match,
                score = calculateScore(match.movieDetails, preferences)
            )
        }

        // Group by score to handle ties
        val groupedByScore = scoredMatches.groupBy { it.score }
        val maxScore = groupedByScore.keys.maxOrNull() ?: return null
        val topMatches = groupedByScore[maxScore] ?: return null

        // Random tie-breaking for equal-rated movies
        return topMatches.randomOrNull()?.match
    }

    /**
     * Get multiple suggestions ranked by score
     * @param matches List of enriched matches to choose from
     * @param preferences User preferences for filtering
     * @param limit Maximum number of suggestions to return
     * @return List of suggested matches ranked by score
     */
    fun getSuggestionsRanked(
        matches: List<EnrichedMatch>,
        preferences: UserPreferences,
        limit: Int = 5
    ): List<EnrichedMatch> {
        // Filter out watched matches
        val unwatchedMatches = matches.filter { !it.match.watched }
        
        if (unwatchedMatches.isEmpty()) {
            return emptyList()
        }

        // Apply availability filtering if strict mode is enabled
        val filteredMatches = if (preferences.availabilityStrict && preferences.selectedProviders.isNotEmpty()) {
            unwatchedMatches.filter { match ->
                hasAvailableProvider(match.streamingProviders, preferences.selectedProviders)
            }
        } else {
            unwatchedMatches
        }

        if (filteredMatches.isEmpty()) {
            return emptyList()
        }

        // Calculate scores and sort by score descending
        return filteredMatches
            .map { match ->
                ScoredMatch(
                    match = match,
                    score = calculateScore(match.movieDetails, preferences)
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.match }
    }

    /**
     * Calculate score for a movie based on rating and release date
     * @param movie The movie to score
     * @param preferences User preferences (currently unused but available for future enhancements)
     * @return Calculated score between 0.0 and 1.0
     */
    private fun calculateScore(movie: Movie?, preferences: UserPreferences): Double {
        if (movie == null) return 0.0

        // Normalize rating (0-10 scale to 0-1 scale)
        val normalizedRating = (movie.voteAverage / 10.0).coerceIn(0.0, 1.0)

        // Calculate recency score based on release date
        val recencyScore = calculateRecencyScore(movie.releaseDate)

        // Weighted combination of rating and recency
        return (normalizedRating * RATING_WEIGHT) + (recencyScore * RECENCY_WEIGHT)
    }

    /**
     * Calculate recency score based on release date
     * More recent movies get higher scores
     * @param releaseDate Release date string in YYYY-MM-DD format
     * @return Recency score between 0.0 and 1.0
     */
    private fun calculateRecencyScore(releaseDate: String?): Double {
        if (releaseDate.isNullOrBlank()) return 0.0

        return try {
            val date = LocalDate.parse(releaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val releaseYear = date.year
            
            // Normalize year to 0-1 scale
            val yearRange = MAX_RELEASE_YEAR - MIN_RELEASE_YEAR
            val normalizedYear = (releaseYear - MIN_RELEASE_YEAR).toDouble() / yearRange
            
            normalizedYear.coerceIn(0.0, 1.0)
        } catch (e: Exception) {
            // If date parsing fails, try to extract year from string
            extractYearFromString(releaseDate)?.let { year ->
                val yearRange = MAX_RELEASE_YEAR - MIN_RELEASE_YEAR
                val normalizedYear = (year - MIN_RELEASE_YEAR).toDouble() / yearRange
                normalizedYear.coerceIn(0.0, 1.0)
            } ?: 0.0
        }
    }

    /**
     * Extract year from release date string
     * @param releaseDate Release date string
     * @return Extracted year or null if not found
     */
    private fun extractYearFromString(releaseDate: String): Int? {
        return try {
            val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
            yearRegex.find(releaseDate)?.value?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if any of the movie's streaming providers match user's selected providers
     * @param streamingProviders List of available streaming providers for the movie
     * @param selectedProviders Set of user's selected provider IDs
     * @return True if at least one provider matches
     */
    private fun hasAvailableProvider(
        streamingProviders: List<StreamingProvider>,
        selectedProviders: Set<Int>
    ): Boolean {
        return streamingProviders.any { provider ->
            selectedProviders.contains(provider.id)
        }
    }

    /**
     * Data class to hold a match with its calculated score
     */
    private data class ScoredMatch(
        val match: EnrichedMatch,
        val score: Double
    )
}