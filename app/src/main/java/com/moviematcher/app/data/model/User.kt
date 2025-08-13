package com.moviematcher.app.data.model

/**
 * Represents a user in the Movie Matcher app
 */
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val fcmToken: String?,
    val roomId: String?,
    val createdAt: Long
)