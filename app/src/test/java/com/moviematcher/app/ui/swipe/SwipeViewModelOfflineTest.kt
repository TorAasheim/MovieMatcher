package com.moviematcher.app.ui.swipe

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.moviematcher.app.data.engine.MovieRecommendationEngine
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.offline.ConnectionManager
import com.moviematcher.app.data.offline.OfflineSwipeQueue
import com.moviematcher.app.data.offline.OfflineSyncManager
import com.moviematcher.app.data.offline.SyncResult
import com.moviematcher.app.data.repository.SwipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for SwipeViewModel offline functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelOfflineTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var swipeRepository: SwipeRepository
    private lateinit var recommendationEngine: MovieRecommendationEngine
    private lateinit var connectionManager: ConnectionManager
    private lateinit var offlineSwipeQueue: OfflineSwipeQueue
    private lateinit var offlineSyncManager: OfflineSyncManager
    private lateinit var viewModel: SwipeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        swipeRepository = mockk()
        recommendationEngine = mockk()
        connectionManager = mockk()
        offlineSwipeQueue = mockk()
        offlineSyncManager = mockk()

        // Default mock setup
        every { connectionManager.isConnected } returns flowOf(true)
        every { connectionManager.isCurrentlyConnected() } returns true
        every { offlineSwipeQueue.queueSize } returns flowOf(0)
        every { recommendationEngine.movieQueue } returns flowOf(emptyList())
        every { recommendationEngine.isLoading } returns flowOf(false)
        every { recommendationEngine.error } returns flowOf(null)
        coEvery { recommendationEngine.getNextMovie() } returns mockk<Movie>()

        viewModel = SwipeViewModel(
            swipeRepository,
            recommendationEngine,
            connectionManager,
            offlineSwipeQueue,
            offlineSyncManager
        )
    }

    @Test
    fun `recordSwipe queues swipe when offline`() = runTest {
        // Given
        every { connectionManager.isConnected } returns flowOf(false)
        every { connectionManager.isCurrentlyConnected() } returns false
        coEvery { swipeRepository.recordSwipe(any(), any()) } throws Exception("No connection")

        // When
        viewModel.recordSwipe(123L, SwipeDecision.LIKE)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("offline"))
        assertNotNull(uiState.lastSwipe)
        assertEquals(123L, uiState.lastSwipe!!.titleId)
        assertEquals(SwipeDecision.LIKE, uiState.lastSwipe!!.decision)
    }

    @Test
    fun `recordSwipe works normally when online`() = runTest {
        // Given
        every { connectionManager.isConnected } returns flowOf(true)
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit

        // When
        viewModel.recordSwipe(123L, SwipeDecision.LIKE)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { swipeRepository.recordSwipe(any(), any()) }
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.lastSwipe)
        assertEquals(123L, uiState.lastSwipe!!.titleId)
    }

    @Test
    fun `forceSyncOfflineData calls sync manager`() = runTest {
        // Given
        coEvery { offlineSyncManager.forceSyncOfflineData() } returns SyncResult.Success(2)

        // When
        viewModel.forceSyncOfflineData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { offlineSyncManager.forceSyncOfflineData() }
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("Synced 2"))
    }

    @Test
    fun `forceSyncOfflineData handles no connection`() = runTest {
        // Given
        coEvery { offlineSyncManager.forceSyncOfflineData() } returns SyncResult.NoConnection

        // When
        viewModel.forceSyncOfflineData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("No connection"))
    }

    @Test
    fun `forceSyncOfflineData handles partial failure`() = runTest {
        // Given
        coEvery { offlineSyncManager.forceSyncOfflineData() } returns SyncResult.PartialFailure(
            successCount = 2,
            failedCount = 1,
            errors = listOf(Exception("Test error"))
        )

        // When
        viewModel.forceSyncOfflineData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("Synced 2"))
        assertTrue(uiState.error!!.contains("1 failed"))
    }
}