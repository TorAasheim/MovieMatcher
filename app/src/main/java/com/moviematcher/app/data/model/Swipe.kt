package com.moviematcher.app.data.model

/**
 * Represents a user's swipe decision on a movie
 */
data class Swipe(
    val titleId: Long,
    val userId: String,
    val decision: SwipeDecision,
    val timestamp: Long
)