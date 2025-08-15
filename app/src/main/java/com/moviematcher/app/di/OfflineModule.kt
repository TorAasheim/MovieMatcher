package com.moviematcher.app.di

import android.content.Context
import com.moviematcher.app.data.offline.ConnectionManager
import com.moviematcher.app.data.offline.OfflineSwipeQueue
import com.moviematcher.app.data.offline.OfflineSyncManager
import com.moviematcher.app.data.repository.SwipeRepository
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for offline functionality dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {
    
    @Provides
    @Singleton
    fun provideConnectionManager(@ApplicationContext context: Context): ConnectionManager {
        return ConnectionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideOfflineSwipeQueue(
        @ApplicationContext context: Context,
        moshi: Moshi
    ): OfflineSwipeQueue {
        return OfflineSwipeQueue(context, moshi)
    }
    
    @Provides
    @Singleton
    fun provideOfflineSyncManager(
        connectionManager: ConnectionManager,
        offlineSwipeQueue: OfflineSwipeQueue,
        swipeRepository: SwipeRepository
    ): OfflineSyncManager {
        return OfflineSyncManager(connectionManager, offlineSwipeQueue, swipeRepository)
    }
}