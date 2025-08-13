package com.moviematcher.app.data.model

/**
 * Represents a streaming provider where a movie is available
 */
data class StreamingProvider(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val deepLinkUrl: String?
)