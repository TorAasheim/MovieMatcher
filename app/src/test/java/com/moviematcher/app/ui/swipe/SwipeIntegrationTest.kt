package com.moviematcher.app.ui.swipe

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.repository.SwipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test for SwipeViewModel with a fake repository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwipeIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fakeSwipeRepository: FakeSwipeRepository
    private lateinit var viewModel: SwipeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSwipeRepository = FakeSwipeRepository()
        viewModel = SwipeViewModel(fakeSwipeRepository)
    }

    @Test
    fun `complete swipe flow should work correctly`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L

        // Act - Initialize session
        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        advanceUntilIdle()

        // Act - Record a swipe
        viewModel.recordSwipe(titleId, SwipeDecision.LIKE)
        advanceUntilIdle()

        // Assert - Swipe was recorded
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertNotNull(uiState.lastSwipe)
        assertEquals(titleId, uiState.lastSwipe?.titleId)
        assertEquals(SwipeDecision.LIKE, uiState.lastSwipe?.decision)
        assertTrue(viewModel.canUndo())

        // Verify repository received the swipe
        assertEquals(1, fakeSwipeRepository.recordedSwipes.size)
        val recordedSwipe = fakeSwipeRepository.recordedSwipes.first()
        assertEquals(titleId, recordedSwipe.titleId)
        assertEquals(userId, recordedSwipe.userId)
        assertEquals(SwipeDecision.LIKE, recordedSwipe.decision)
    }

    @Test
    fun `partner swipe observation should work correctly`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val partnerSwipe = Swipe(
            titleId = 67890L,
            userId = partnerId,
            decision = SwipeDecision.PASS,
            timestamp = System.currentTimeMillis()
        )

        // Act - Initialize session first
        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        advanceUntilIdle()

        // Act - Simulate partner swipe after initialization
        fakeSwipeRepository.emitPartnerSwipe(partnerSwipe)
        advanceUntilIdle()

        // Assert - Partner swipe was received
        val partnerSwipes = viewModel.partnerSwipes.value
        assertEquals(1, partnerSwipes.size)
        assertEquals(partnerSwipe, partnerSwipes.first())
        assertEquals(partnerSwipe, viewModel.uiState.value.lastPartnerSwipe)
    }

    @Test
    fun `undo functionality should work correctly`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L

        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        viewModel.recordSwipe(titleId, SwipeDecision.LIKE)
        advanceUntilIdle()

        // Verify swipe was recorded
        assertTrue(viewModel.canUndo())
        assertEquals(1, fakeSwipeRepository.recordedSwipes.size)

        // Act - Undo the swipe
        viewModel.undoLastSwipe()
        advanceUntilIdle()

        // Assert - Swipe was undone
        assertFalse(viewModel.canUndo())
        assertNull(viewModel.uiState.value.lastSwipe)
        assertEquals(1, fakeSwipeRepository.undoCallCount)
    }

    /**
     * Fake implementation of SwipeRepository for testing
     */
    private class FakeSwipeRepository : SwipeRepository {
        val recordedSwipes = mutableListOf<Swipe>()
        var undoCallCount = 0
        private val partnerSwipeFlow = MutableSharedFlow<Swipe>(replay = 1)

        override suspend fun recordSwipe(roomId: String, swipe: Swipe) {
            recordedSwipes.add(swipe)
        }

        override fun observePartnerSwipes(roomId: String, partnerId: String): Flow<Swipe> {
            return partnerSwipeFlow
        }

        override suspend fun undoLastSwipe(roomId: String, userId: String) {
            undoCallCount++
            // Remove the last swipe for this user
            recordedSwipes.removeLastOrNull()
        }

        fun emitPartnerSwipe(swipe: Swipe) {
            partnerSwipeFlow.tryEmit(swipe)
        }
    }
}