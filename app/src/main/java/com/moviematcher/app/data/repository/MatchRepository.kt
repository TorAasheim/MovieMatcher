package com.moviematcher.app.data.repository

import com.moviematcher.app.data.model.Match
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for match data operations
 */
interface MatchRepository {
    /**
     * Create a match when both users like the same movie
     */
    suspend fun createMatch(roomId: String, movieId: Long)
    
    /**
     * Observe matches in a room in real-time
     */
    fun observeMatches(roomId: String): Flow<List<Match>>
    
    /**
     * Mark a match as watched with optional notes
     */
    suspend fun markAsWatched(roomId: String, movieId: Long, notes: String)
}