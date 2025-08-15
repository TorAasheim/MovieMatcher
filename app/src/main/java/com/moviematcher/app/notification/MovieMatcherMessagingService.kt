package com.moviematcher.app.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moviematcher.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MovieMatcherMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Handle different types of notifications
        val notificationType = remoteMessage.data["type"]
        when (notificationType) {
            "match" -> {
                val title = remoteMessage.data["title"] ?: "New Match!"
                val body = remoteMessage.data["body"] ?: "You have a new movie match!"
                val movieId = remoteMessage.data["movieId"]
                
                // Create and show notification
                showMatchNotification(title, body, movieId)
            }
            else -> {
                // Handle other notification types or show generic notification
                val title = remoteMessage.notification?.title ?: "Movie Matcher"
                val body = remoteMessage.notification?.body ?: "You have a new notification"
                showGenericNotification(title, body)
            }
        }
    }
    
    private fun showMatchNotification(title: String, body: String, movieId: String?) {
        // In a real implementation, this would create an Android notification
        // with proper intent to open the matches screen
        println("Match Notification: $title - $body (Movie ID: $movieId)")
    }
    
    private fun showGenericNotification(title: String, body: String) {
        // In a real implementation, this would create a generic Android notification
        println("Generic Notification: $title - $body")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Update the user's FCM token in Firestore
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRepository.updateFcmToken(currentUser.uid, token)
                } catch (e: Exception) {
                    // Log error but don't crash the service
                }
            }
        }
    }
}