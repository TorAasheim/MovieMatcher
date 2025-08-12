package com.moviematcher.app.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MovieMatcherMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // TODO: Handle FCM messages (will be implemented in later tasks)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to server (will be implemented in later tasks)
    }
}