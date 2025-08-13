package com.moviematcher.app.data.model

/**
 * Represents a room where two users can swipe on movies together
 */
data class Room(
    val id: String,
    val userIds: List<String>,
    val createdAt: Long
)