package com.moviematcher.app.data.offline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_swipes")

/**
 * Manages offline swipe queue using DataStore for persistence
 */
@Singleton
class OfflineSwipeQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    
    private val queueKey = stringPreferencesKey("swipe_queue")
    
    private val swipeListAdapter: JsonAdapter<List<QueuedSwipe>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, QueuedSwipe::class.java)
    )
    
    /**
     * Add a swipe to the offline queue
     */
    suspend fun queueSwipe(roomId: String, swipe: Swipe) {
        val queuedSwipe = QueuedSwipe(
            roomId = roomId,
            swipe = swipe,
            timestamp = System.currentTimeMillis()
        )
        
        context.dataStore.edit { preferences ->
            val currentQueue = getQueueFromPreferences(preferences)
            val updatedQueue = currentQueue + queuedSwipe
            preferences[queueKey] = swipeListAdapter.toJson(updatedQueue)
        }
    }
    
    /**
     * Get all queued swipes
     */
    suspend fun getQueuedSwipes(): List<QueuedSwipe> {
        return context.dataStore.data.first().let { preferences ->
            getQueueFromPreferences(preferences)
        }
    }
    
    /**
     * Remove swipes from the queue after successful sync
     */
    suspend fun removeSwipes(swipesToRemove: List<QueuedSwipe>) {
        context.dataStore.edit { preferences ->
            val currentQueue = getQueueFromPreferences(preferences)
            val updatedQueue = currentQueue.filterNot { queued ->
                swipesToRemove.any { toRemove ->
                    queued.roomId == toRemove.roomId &&
                    queued.swipe.titleId == toRemove.swipe.titleId &&
                    queued.swipe.userId == toRemove.swipe.userId &&
                    queued.timestamp == toRemove.timestamp
                }
            }
            preferences[queueKey] = swipeListAdapter.toJson(updatedQueue)
        }
    }
    
    /**
     * Clear all queued swipes
     */
    suspend fun clearQueue() {
        context.dataStore.edit { preferences ->
            preferences.remove(queueKey)
        }
    }
    
    /**
     * Observe queue size changes
     */
    val queueSize: Flow<Int> = context.dataStore.data.map { preferences ->
        getQueueFromPreferences(preferences).size
    }
    
    /**
     * Check if queue has pending swipes
     */
    suspend fun hasPendingSwipes(): Boolean {
        return getQueuedSwipes().isNotEmpty()
    }
    
    private fun getQueueFromPreferences(preferences: Preferences): List<QueuedSwipe> {
        val queueJson = preferences[queueKey] ?: return emptyList()
        return try {
            swipeListAdapter.fromJson(queueJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Represents a swipe that's queued for offline sync
 */
data class QueuedSwipe(
    val roomId: String,
    val swipe: Swipe,
    val timestamp: Long
)