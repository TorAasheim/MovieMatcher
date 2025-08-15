package com.moviematcher.app.data.offline

import com.moviematcher.app.data.repository.SwipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization of offline data when connection is restored
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val offlineSwipeQueue: OfflineSwipeQueue,
    private val swipeRepository: SwipeRepository
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        startSyncObserver()
    }
    
    /**
     * Start observing connection changes and sync when connected
     */
    private fun startSyncObserver() {
        syncScope.launch {
            combine(
                connectionManager.isConnected,
                offlineSwipeQueue.queueSize
            ) { isConnected, queueSize ->
                isConnected && queueSize > 0
            }
            .distinctUntilChanged()
            .filter { shouldSync -> shouldSync }
            .collect {
                syncOfflineData()
            }
        }
    }
    
    /**
     * Sync all offline data to the server
     */
    suspend fun syncOfflineData(): SyncResult {
        val queuedSwipes = offlineSwipeQueue.getQueuedSwipes()
        if (queuedSwipes.isEmpty()) {
            return SyncResult.Success(0)
        }
        
        val successfulSwipes = mutableListOf<QueuedSwipe>()
        val failedSwipes = mutableListOf<Pair<QueuedSwipe, Exception>>()
        
        for (queuedSwipe in queuedSwipes) {
            try {
                swipeRepository.recordSwipe(queuedSwipe.roomId, queuedSwipe.swipe)
                successfulSwipes.add(queuedSwipe)
            } catch (e: Exception) {
                failedSwipes.add(queuedSwipe to e)
            }
        }
        
        // Remove successfully synced swipes from queue
        if (successfulSwipes.isNotEmpty()) {
            offlineSwipeQueue.removeSwipes(successfulSwipes)
        }
        
        return if (failedSwipes.isEmpty()) {
            SyncResult.Success(successfulSwipes.size)
        } else {
            SyncResult.PartialFailure(
                successCount = successfulSwipes.size,
                failedCount = failedSwipes.size,
                errors = failedSwipes.map { it.second }
            )
        }
    }
    
    /**
     * Force sync offline data (useful for manual retry)
     */
    suspend fun forceSyncOfflineData(): SyncResult {
        return if (connectionManager.isCurrentlyConnected()) {
            syncOfflineData()
        } else {
            SyncResult.NoConnection
        }
    }
}

/**
 * Result of sync operation
 */
sealed class SyncResult {
    data class Success(val syncedCount: Int) : SyncResult()
    data class PartialFailure(
        val successCount: Int,
        val failedCount: Int,
        val errors: List<Exception>
    ) : SyncResult()
    object NoConnection : SyncResult()
}