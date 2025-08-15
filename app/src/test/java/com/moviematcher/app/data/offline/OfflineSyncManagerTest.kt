package com.moviematcher.app.data.offline

import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.repository.SwipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for OfflineSyncManager
 */
class OfflineSyncManagerTest {

    private lateinit var connectionManager: ConnectionManager
    private lateinit var offlineSwipeQueue: OfflineSwipeQueue
    private lateinit var swipeRepository: SwipeRepository
    private lateinit var offlineSyncManager: OfflineSyncManager

    @Before
    fun setup() {
        connectionManager = mockk()
        offlineSwipeQueue = mockk()
        swipeRepository = mockk()
        
        // Default mock setup
        every { connectionManager.isConnected } returns flowOf(true)
        every { offlineSwipeQueue.queueSize } returns flowOf(0)
        
        offlineSyncManager = OfflineSyncManager(
            connectionManager,
            offlineSwipeQueue,
            swipeRepository
        )
    }

    @Test
    fun `syncOfflineData returns success when no swipes queued`() = runTest {
        // Given
        coEvery { offlineSwipeQueue.getQueuedSwipes() } returns emptyList()

        // When
        val result = offlineSyncManager.syncOfflineData()

        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(0, (result as SyncResult.Success).syncedCount)
    }

    @Test
    fun `syncOfflineData successfully syncs queued swipes`() = runTest {
        // Given
        val queuedSwipe = QueuedSwipe(
            roomId = "room123",
            swipe = Swipe(
                titleId = 456L,
                userId = "user123",
                decision = SwipeDecision.LIKE,
                timestamp = System.currentTimeMillis()
            ),
            timestamp = System.currentTimeMillis()
        )
        
        coEvery { offlineSwipeQueue.getQueuedSwipes() } returns listOf(queuedSwipe)
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit
        coEvery { offlineSwipeQueue.removeSwipes(any()) } returns Unit

        // When
        val result = offlineSyncManager.syncOfflineData()

        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).syncedCount)
        
        coVerify { swipeRepository.recordSwipe("room123", queuedSwipe.swipe) }
        coVerify { offlineSwipeQueue.removeSwipes(listOf(queuedSwipe)) }
    }

    @Test
    fun `syncOfflineData handles partial failures`() = runTest {
        // Given
        val successSwipe = QueuedSwipe(
            roomId = "room123",
            swipe = Swipe(456L, "user123", SwipeDecision.LIKE, System.currentTimeMillis()),
            timestamp = System.currentTimeMillis()
        )
        val failSwipe = QueuedSwipe(
            roomId = "room456",
            swipe = Swipe(789L, "user456", SwipeDecision.PASS, System.currentTimeMillis()),
            timestamp = System.currentTimeMillis()
        )
        
        coEvery { offlineSwipeQueue.getQueuedSwipes() } returns listOf(successSwipe, failSwipe)
        coEvery { swipeRepository.recordSwipe("room123", successSwipe.swipe) } returns Unit
        coEvery { swipeRepository.recordSwipe("room456", failSwipe.swipe) } throws Exception("Network error")
        coEvery { offlineSwipeQueue.removeSwipes(any()) } returns Unit

        // When
        val result = offlineSyncManager.syncOfflineData()

        // Then
        assertTrue(result is SyncResult.PartialFailure)
        val partialResult = result as SyncResult.PartialFailure
        assertEquals(1, partialResult.successCount)
        assertEquals(1, partialResult.failedCount)
        assertEquals(1, partialResult.errors.size)
        
        coVerify { offlineSwipeQueue.removeSwipes(listOf(successSwipe)) }
    }

    @Test
    fun `forceSyncOfflineData returns NoConnection when offline`() = runTest {
        // Given
        every { connectionManager.isCurrentlyConnected() } returns false

        // When
        val result = offlineSyncManager.forceSyncOfflineData()

        // Then
        assertTrue(result is SyncResult.NoConnection)
    }

    @Test
    fun `forceSyncOfflineData syncs when connected`() = runTest {
        // Given
        every { connectionManager.isCurrentlyConnected() } returns true
        coEvery { offlineSwipeQueue.getQueuedSwipes() } returns emptyList()

        // When
        val result = offlineSyncManager.forceSyncOfflineData()

        // Then
        assertTrue(result is SyncResult.Success)
    }
}