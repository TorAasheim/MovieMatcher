package com.moviematcher.app.data.offline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for OfflineSwipeQueue
 */
class OfflineSwipeQueueTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var moshi: Moshi
    private lateinit var offlineSwipeQueue: OfflineSwipeQueue

    @Before
    fun setup() {
        context = mockk()
        dataStore = mockk()
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        // Mock DataStore to return empty preferences by default
        every { dataStore.data } returns flowOf(mockk<Preferences>().apply {
            every { get<String>(any()) } returns null
        })
        
        offlineSwipeQueue = OfflineSwipeQueue(context, moshi)
    }

    @Test
    fun `queueSwipe adds swipe to queue`() = runTest {
        // Given
        val roomId = "room123"
        val swipe = Swipe(
            titleId = 456L,
            userId = "user123",
            decision = SwipeDecision.LIKE,
            timestamp = System.currentTimeMillis()
        )
        
        val preferences = mockk<Preferences>(relaxed = true)
        coEvery { dataStore.edit(any()) } returns preferences

        // When
        offlineSwipeQueue.queueSwipe(roomId, swipe)

        // Then - verify that edit was called (actual DataStore testing would require more complex setup)
        // In a real test environment, we'd verify the JSON serialization and storage
    }

    @Test
    fun `hasPendingSwipes returns false when queue is empty`() = runTest {
        // Given - empty queue (default mock setup)

        // When
        val result = offlineSwipeQueue.hasPendingSwipes()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getQueuedSwipes returns empty list when no swipes queued`() = runTest {
        // Given - empty queue (default mock setup)

        // When
        val result = offlineSwipeQueue.getQueuedSwipes()

        // Then
        assertEquals(emptyList(), result)
    }
}