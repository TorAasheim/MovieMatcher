package com.moviematcher.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CleanupRepository using Firebase Cloud Functions
 */
@Singleton
class CleanupRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : CleanupRepository {
    
    override suspend fun clearUserSwipes(roomId: String): Int {
        return try {
            val data = hashMapOf(
                "roomId" to roomId
            )
            
            val result = functions
                .getHttpsCallable("clearUserSwipes")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val swipesDeleted = resultData?.get("swipesDeleted") as? Number
            
            swipesDeleted?.toInt() ?: 0
            
        } catch (e: Exception) {
            throw CleanupException("Failed to clear swipes: ${e.message}", e)
        }
    }
}

/**
 * Exception thrown when cleanup operations fail
 */
class CleanupException(message: String, cause: Throwable? = null) : Exception(message, cause)