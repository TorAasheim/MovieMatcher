package com.moviematcher.app.data.repository

/**
 * Repository interface for data cleanup operations
 */
interface CleanupRepository {
    
    /**
     * Clear all swipes for the current user in the specified room
     * @param roomId The room ID to clear swipes from
     * @return Number of swipes that were deleted
     */
    suspend fun clearUserSwipes(roomId: String): Int
}