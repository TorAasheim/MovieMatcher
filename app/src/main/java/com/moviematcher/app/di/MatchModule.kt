package com.moviematcher.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.moviematcher.app.data.repository.MatchRepository
import com.moviematcher.app.data.repository.MatchRepositoryImpl
import com.moviematcher.app.notification.NotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MatchModule {
    
    @Provides
    @Singleton
    fun provideMatchRepository(
        firestore: FirebaseFirestore,
        notificationService: NotificationService
    ): MatchRepository {
        return MatchRepositoryImpl(firestore, notificationService)
    }
}