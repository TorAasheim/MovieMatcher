package com.moviematcher.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.moviematcher.app.data.repository.SwipeRepository
import com.moviematcher.app.data.repository.SwipeRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SwipeModule {
    
    @Provides
    @Singleton
    fun provideSwipeRepository(
        firestore: FirebaseFirestore
    ): SwipeRepository {
        return SwipeRepositoryImpl(firestore)
    }
}