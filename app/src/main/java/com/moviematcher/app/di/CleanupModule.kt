package com.moviematcher.app.di

import com.moviematcher.app.data.repository.CleanupRepository
import com.moviematcher.app.data.repository.CleanupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for cleanup-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CleanupModule {
    
    @Binds
    @Singleton
    abstract fun bindCleanupRepository(
        cleanupRepositoryImpl: CleanupRepositoryImpl
    ): CleanupRepository
}