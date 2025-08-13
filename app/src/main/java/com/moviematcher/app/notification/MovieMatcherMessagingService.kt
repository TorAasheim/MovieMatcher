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
        // TODO: Handle FCM messages (will be implemented in later tasks)
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