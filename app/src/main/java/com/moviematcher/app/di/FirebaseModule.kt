package com.moviematcher.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.moviematcher.app.notification.NotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Firebase dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        
        // Enable offline persistence for local data caching
        try {
            firestore.enableNetwork()
            // Note: Offline persistence is enabled by default in newer Firebase versions
            // If you need to configure cache size, use:
            // firestore.clearPersistence() // Only call when needed
        } catch (e: Exception) {
            // Persistence might already be enabled
        }
        
        return firestore
    }
    
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()
    
    @Provides
    @Singleton
    fun provideNotificationService(
        firebaseMessaging: FirebaseMessaging,
        firestore: FirebaseFirestore
    ): NotificationService {
        return NotificationService(firebaseMessaging, firestore)
    }
}