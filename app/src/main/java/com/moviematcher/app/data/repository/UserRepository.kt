package com.moviematcher.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.moviematcher.app.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user data in Firestore
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
    }
    
    /**
     * Create or update user profile in Firestore
     * @param firebaseUser The authenticated Firebase user
     * @return User object with updated information
     */
    suspend fun createOrUpdateUser(firebaseUser: FirebaseUser): User {
        val fcmToken = try {
            messaging.token.await()
        } catch (e: Exception) {
            null // FCM token retrieval failed, continue without it
        }
        
        val user = User(
            id = firebaseUser.uid,
            displayName = firebaseUser.displayName ?: "Unknown User",
            photoUrl = firebaseUser.photoUrl?.toString(),
            fcmToken = fcmToken,
            roomId = null, // Will be set when user joins a room
            createdAt = System.currentTimeMillis()
        )
        
        // Save to Firestore
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user)
            .await()
        
        return user
    }
    
    /**
     * Get user by ID from Firestore
     * @param userId The user ID to retrieve
     * @return User object or null if not found
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Update user's FCM token
     * @param userId The user ID
     * @param fcmToken The new FCM token
     */
    suspend fun updateFcmToken(userId: String, fcmToken: String) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", fcmToken)
                .await()
        } catch (e: Exception) {
            // Log error but don't throw - FCM token update is not critical
        }
    }
    
    /**
     * Update user's room ID
     * @param userId The user ID
     * @param roomId The room ID to associate with the user
     */
    suspend fun updateUserRoom(userId: String, roomId: String?) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update("roomId", roomId)
            .await()
    }
    
    /**
     * Get current FCM token and update user record
     * @param userId The user ID to update
     */
    suspend fun refreshFcmToken(userId: String) {
        try {
            val token = messaging.token.await()
            updateFcmToken(userId, token)
        } catch (e: Exception) {
            // FCM token refresh failed, continue silently
        }
    }
    
    /**
     * Delete user data (for cleanup/privacy)
     * @param userId The user ID to delete
     */
    suspend fun deleteUser(userId: String) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .delete()
            .await()
    }
}