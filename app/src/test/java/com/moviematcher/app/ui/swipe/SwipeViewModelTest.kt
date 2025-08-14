package com.moviematcher.app.ui.swipe

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.repository.SwipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var swipeRepository: SwipeRepository
    private lateinit var viewModel: SwipeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        swipeRepository = mockk()
        viewModel = SwipeViewModel(swipeRepository)
    }

    @Test
    fun `initializeSwipeSession should start observing partner swipes`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val partnerSwipe = Swipe(
            titleId = 12345L,
            userId = partnerId,
            decision = SwipeDecision.LIKE,
            timestamp = System.currentTimeMillis()
        )

        every { swipeRepository.observePartnerSwipes(roomId, partnerId) } returns flowOf(partnerSwipe)

        // Act
        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        advanceUntilIdle()

        // Assert
        val partnerSwipes = viewModel.partnerSwipes.value
        assertEquals(1, partnerSwipes.size)
        assertEquals(partnerSwipe, partnerSwipes.first())
        assertEquals(partnerSwipe, viewModel.uiState.value.lastPartnerSwipe)
    }

    @Test
    fun `recordSwipe should save swipe and update UI state`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L
        val decision = SwipeDecision.LIKE

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit

        viewModel.initializeSwipeSession(roomId, userId, partnerId)

        // Act
        viewModel.recordSwipe(titleId, decision)
        advanceUntilIdle()

        // Assert
        coVerify { swipeRepository.recordSwipe(roomId, any()) }
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertNotNull(uiState.lastSwipe)
        assertEquals(titleId, uiState.lastSwipe?.titleId)
        assertEquals(userId, uiState.lastSwipe?.userId)
        assertEquals(decision, uiState.lastSwipe?.decision)
    }

    @Test
    fun `recordSwipe should handle errors gracefully`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L
        val decision = SwipeDecision.LIKE
        val errorMessage = "Network error"

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } throws Exception(errorMessage)

        viewModel.initializeSwipeSession(roomId, userId, partnerId)

        // Act
        viewModel.recordSwipe(titleId, decision)
        advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.error?.contains(errorMessage) == true)
        assertNull(uiState.lastSwipe)
    }

    @Test
    fun `undoLastSwipe should remove last swipe and update UI state`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L
        val decision = SwipeDecision.LIKE

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit
        coEvery { swipeRepository.undoLastSwipe(any(), any()) } returns Unit

        viewModel.initializeSwipeSession(roomId, userId, partnerId)

        // First record a swipe
        viewModel.recordSwipe(titleId, decision)
        advanceUntilIdle()

        // Verify swipe was recorded
        assertNotNull(viewModel.uiState.value.lastSwipe)

        // Act - undo the swipe
        viewModel.undoLastSwipe()
        advanceUntilIdle()

        // Assert
        coVerify { swipeRepository.undoLastSwipe(roomId, userId) }
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertNull(uiState.lastSwipe)
    }

    @Test
    fun `undoLastSwipe should handle errors gracefully`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L
        val decision = SwipeDecision.LIKE
        val errorMessage = "Undo failed"

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit
        coEvery { swipeRepository.undoLastSwipe(any(), any()) } throws Exception(errorMessage)

        viewModel.initializeSwipeSession(roomId, userId, partnerId)

        // First record a swipe
        viewModel.recordSwipe(titleId, decision)
        advanceUntilIdle()

        // Act - try to undo
        viewModel.undoLastSwipe()
        advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.error?.contains(errorMessage) == true)
        // Last swipe should still be there since undo failed
        assertNotNull(uiState.lastSwipe)
    }

    @Test
    fun `canUndo should return true when last swipe exists and not loading`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val titleId = 12345L
        val decision = SwipeDecision.LIKE

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } returns Unit

        viewModel.initializeSwipeSession(roomId, userId, partnerId)

        // Act
        viewModel.recordSwipe(titleId, decision)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.canUndo())
    }

    @Test
    fun `canUndo should return false when no last swipe exists`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()

        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.canUndo())
    }

    @Test
    fun `clearError should remove error from UI state`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"

        every { swipeRepository.observePartnerSwipes(any(), any()) } returns flowOf()
        coEvery { swipeRepository.recordSwipe(any(), any()) } throws Exception("Test error")

        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        viewModel.recordSwipe(12345L, SwipeDecision.LIKE)
        advanceUntilIdle()

        // Verify error exists
        assertNotNull(viewModel.uiState.value.error)

        // Act
        viewModel.clearError()

        // Assert
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `observePartnerSwipes should not add duplicate swipes`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        val partnerId = "partner456"
        val partnerSwipe = Swipe(
            titleId = 12345L,
            userId = partnerId,
            decision = SwipeDecision.LIKE,
            timestamp = System.currentTimeMillis()
        )

        // Simulate receiving the same swipe twice
        every { swipeRepository.observePartnerSwipes(roomId, partnerId) } returns 
            flowOf(partnerSwipe, partnerSwipe)

        // Act
        viewModel.initializeSwipeSession(roomId, userId, partnerId)
        advanceUntilIdle()

        // Assert - should only have one swipe, not two
        val partnerSwipes = viewModel.partnerSwipes.value
        assertEquals(1, partnerSwipes.size)
        assertEquals(partnerSwipe, partnerSwipes.first())
    }
}