package com.moviematcher.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.moviematcher.app.data.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    
    @Provides
    @Singleton
    fun providePreferencesRepository(
        firestore: FirebaseFirestore
    ): PreferencesRepository {
        return PreferencesRepository(firestore)
    }
}