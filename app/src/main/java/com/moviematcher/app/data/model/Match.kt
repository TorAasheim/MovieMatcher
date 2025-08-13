package com.moviematcher.app.data.model

/**
 * Represents a match when both users like the same movie
 */
data class Match(
    val titleId: Long,
    val timestamp: Long,
    val watched: Boolean = false,
    val notes: String = "",
    val movieDetails: Movie? = null,
    val streamingProviders: List<StreamingProvider> = emptyList()
)