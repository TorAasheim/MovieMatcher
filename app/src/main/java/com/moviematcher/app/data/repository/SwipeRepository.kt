package com.moviematcher.app.data.repository

import com.moviematcher.app.data.model.Swipe
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for swipe data operations
 */
interface SwipeRepository {
    /**
     * Record a swipe decision for a user in a room
     */
    suspend fun recordSwipe(roomId: String, swipe: Swipe)
    
    /**
     * Observe partner's swipe decisions in real-time
     */
    fun observePartnerSwipes(roomId: String, partnerId: String): Flow<Swipe>
    
    /**
     * Undo the last swipe decision for a user in a room
     */
    suspend fun undoLastSwipe(roomId: String, userId: String)
}