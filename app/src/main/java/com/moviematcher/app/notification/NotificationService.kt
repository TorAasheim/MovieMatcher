package com.moviematcher.app.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling push notifications via Firebase Cloud Messaging
 */
@Singleton
class NotificationService @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FIELD_FCM_TOKEN = "fcmToken"
        private const val NOTIFICATION_TOPIC_MATCH = "match"
        private const val NOTIFICATION_TITLE_MATCH = "It's a Match! 🎬"
    }

    /**
     * Send match notification to both users when a match occurs
     */
    suspend fun sendMatchNotification(userIds: List<String>, movieId: Long) {
        try {
            // Get FCM tokens for both users
            val tokens = mutableListOf<String>()
            
            for (userId in userIds) {
                val userDoc = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .get()
                    .await()
                
                userDoc.getString(FIELD_FCM_TOKEN)?.let { token ->
                    tokens.add(token)
                }
            }
            
            if (tokens.isNotEmpty()) {
                // Create notification payload
                val notificationData = mapOf(
                    "type" to NOTIFICATION_TOPIC_MATCH,
                    "movieId" to movieId.toString(),
                    "title" to NOTIFICATION_TITLE_MATCH,
                    "body" to "You both liked the same movie! Check your matches to see what to watch tonight."
                )
                
                // Send to each token individually
                // Note: In a production app, you'd typically use Firebase Cloud Functions
                // to send notifications from the server side for better security
                for (token in tokens) {
                    sendNotificationToToken(token, notificationData)
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw - notification failures shouldn't break the match creation
            // In a real app, you'd use proper logging framework
            println("Failed to send match notification: ${e.message}")
        }
    }
    
    /**
     * Send notification to a specific FCM token
     * Note: In a real implementation, this would be done via Firebase Cloud Functions
     * since client SDKs cannot send messages to other devices directly
     */
    private suspend fun sendNotificationToToken(token: String, data: Map<String, String>) {
        try {
            // Note: FirebaseMessaging.send() is not available in client SDK
            // This would typically be done via Firebase Cloud Functions
            // For now, we'll use a local notification approach
            showLocalNotification(data["title"] ?: "", data["body"] ?: "")
            
        } catch (e: Exception) {
            println("Failed to send notification to token $token: ${e.message}")
        }
    }
    
    /**
     * Show local notification as fallback
     * In a real implementation, this would create an Android notification
     */
    private fun showLocalNotification(title: String, body: String) {
        // This is a placeholder - in a real app you'd use NotificationManager
        // to create and display Android notifications
        println("Local Notification: $title - $body")
    }
}