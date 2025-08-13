package com.moviematcher.app.ui.pairing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.moviematcher.app.data.model.Room
import com.moviematcher.app.data.repository.RoomCreationException
import com.moviematcher.app.data.repository.RoomJoinException
import com.moviematcher.app.data.repository.RoomRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var roomRepository: RoomRepository
    private lateinit var viewModel: PairingViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        roomRepository = mockk()
        viewModel = PairingViewModel(roomRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() {
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertNull(initialState.room)
        assertNull(initialState.inviteCode)
        assertEquals("", initialState.inviteCodeInput)
        assertEquals(PairingStep.CHOOSE_ACTION, initialState.pairingStep)
        assertNull(initialState.error)
    }
    
    @Test
    fun `createRoom successfully creates room and updates state`() = runTest {
        // Arrange
        val userId = "user123"
        val expectedRoom = Room("room123", listOf(userId), System.currentTimeMillis())
        val expectedInviteCode = "BAR-TOK"
        
        coEvery { roomRepository.createRoom(userId) } returns Pair(expectedRoom, expectedInviteCode)
        
        // Act
        viewModel.createRoom(userId)
        
        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expectedRoom, state.room)
        assertEquals(expectedInviteCode, state.inviteCode)
        assertEquals(PairingStep.ROOM_CREATED, state.pairingStep)
        assertNull(state.error)
        
        coVerify { roomRepository.createRoom(userId) }
    }
    
    @Test
    fun `createRoom shows loading state during operation`() = runTest {
        // Arrange
        val userId = "user123"
        val expectedRoom = Room("room123", listOf(userId), System.currentTimeMillis())
        val expectedInviteCode = "BAR-TOK"
        
        // Use a slow coroutine to test loading state
        coEvery { roomRepository.createRoom(userId) } coAnswers {
            delay(100)
            Pair(expectedRoom, expectedInviteCode)
        }
        
        // Act
        viewModel.createRoom(userId)
        
        // Assert loading state
        assertTrue(viewModel.uiState.value.isLoading)
        
        // Wait for completion
        advanceUntilIdle()
        
        // Assert final state
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expectedRoom, viewModel.uiState.value.room)
    }
    
    @Test
    fun `createRoom handles RoomCreationException`() = runTest {
        // Arrange
        val userId = "user123"
        val exception = RoomCreationException("Failed to create room")
        
        coEvery { roomRepository.createRoom(userId) } throws exception
        
        // Act
        viewModel.createRoom(userId)
        
        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.room)
        assertNull(state.inviteCode)
        assertEquals("Failed to create room: Failed to create room", state.error)
        assertEquals(PairingStep.CHOOSE_ACTION, state.pairingStep)
    }
    
    @Test
    fun `createRoom handles generic exception`() = runTest {
        // Arrange
        val userId = "user123"
        val exception = RuntimeException("Unexpected error")
        
        coEvery { roomRepository.createRoom(userId) } throws exception
        
        // Act
        viewModel.createRoom(userId)
        
        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("An unexpected error occurred", state.error)
    }
    
    @Test
    fun `createRoom ignores call when already loading`() = runTest {
        // Arrange
        val userId = "user123"
        
        // Set loading state
        viewModel.createRoom(userId)
        assertTrue(viewModel.uiState.value.isLoading)
        
        // Act - try to create room again while loading
        viewModel.createRoom(userId)
        
        // Assert - should only be called once
        coVerify(exactly = 1) { roomRepository.createRoom(userId) }
    }
    
    @Test
    fun `joinRoom successfully joins room and updates state`() = runTest {
        // Arrange
        val userId = "user456"
        val inviteCode = "BAR-TOK"
        val expectedRoom = Room("room123", listOf("user123", userId), System.currentTimeMillis())
        
        coEvery { roomRepository.joinRoom(userId, inviteCode) } returns expectedRoom
        
        // Act
        viewModel.joinRoom(userId, inviteCode)
        
        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expectedRoom, state.room)
        assertEquals(PairingStep.ROOM_JOINED, state.pairingStep)
        assertNull(state.error)
        
        coVerify { roomRepository.joinRoom(userId, inviteCode) }
    }
    
    @Test
    fun `joinRoom trims and uppercases invite code`() = runTest {
        // Arrange
        val userId = "user456"
        val inputCode = "  bar-tok  "
        val expectedCode = "BAR-TOK"
        val expectedRoom = Room("room123", listOf("user123", userId), System.currentTimeMillis())
        
        coEvery { roomRepository.joinRoom(userId, expectedCode) } returns expectedRoom
        
        // Act
        viewModel.joinRoom(userId, inputCode)
        
        // Assert
        coVerify { roomRepository.joinRoom(userId, expectedCode) }
    }
    
    @Test
    fun `joinRoom shows error for empty invite code`() = runTest {
        // Arrange
        val userId = "user456"
        val emptyCode = "   "
        
        // Act
        viewModel.joinRoom(userId, emptyCode)
        
        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Please enter an invite code", state.error)
        
        coVerify(exactly = 0) { roomRepository.joinRoom(any(), any()) }
    }
    
    @Test
    fun `joinRoom handles different RoomJoinException types`() = runTest {
        val userId = "user456"
        val inviteCode = "BAR-TOK"
        
        val testCases = listOf(
            "Invalid invite code format" to "Invalid invite code format. Please check and try again.",
            "Invalid invite code" to "This invite code doesn't exist. Please check and try again.",
            "Room is full" to "This room is already full. Only 2 people can be in a room.",
            "already in this room" to "You're already in this room.",
            "Room no longer exists" to "This room no longer exists.",
            "Some other error" to "Failed to join room: Some other error"
        )
        
        testCases.forEach { (exceptionMessage, expectedError) ->
            // Arrange
            coEvery { roomRepository.joinRoom(userId, inviteCode) } throws RoomJoinException(exceptionMessage)
            
            // Act
            viewModel.joinRoom(userId, inviteCode)
            
            // Assert
            assertEquals(expectedError, viewModel.uiState.value.error)
            
            // Reset for next test
            viewModel.clearError()
        }
    }
    
    @Test
    fun `joinRoom handles generic exception`() = runTest {
        // Arrange
        val userId = "user456"
        val inviteCode = "BAR-TOK"
        val exception = RuntimeException("Unexpected error")
        
        coEvery { roomRepository.joinRoom(userId, inviteCode) } throws exception
        
        // Act
        viewModel.joinRoom(userId, inviteCode)
        
        // Assert
        val state = viewModel.uiState.value
        assertEquals("An unexpected error occurred", state.error)
    }
    
    @Test
    fun `updateInviteCodeInput updates input and clears error`() {
        // Arrange
        viewModel.joinRoom("user123", "") // Set an error first
        assertEquals("Please enter an invite code", viewModel.uiState.value.error)
        
        // Act
        viewModel.updateInviteCodeInput("bar-tok")
        
        // Assert
        val state = viewModel.uiState.value
        assertEquals("BAR-TOK", state.inviteCodeInput)
        assertNull(state.error)
    }
    
    @Test
    fun `setPairingStep updates step and clears error`() {
        // Arrange
        viewModel.joinRoom("user123", "") // Set an error first
        assertEquals("Please enter an invite code", viewModel.uiState.value.error)
        
        // Act
        viewModel.setPairingStep(PairingStep.CREATE_ROOM)
        
        // Assert
        val state = viewModel.uiState.value
        assertEquals(PairingStep.CREATE_ROOM, state.pairingStep)
        assertNull(state.error)
    }
    
    @Test
    fun `clearError removes error message`() {
        // Arrange
        viewModel.joinRoom("user123", "") // Set an error first
        assertEquals("Please enter an invite code", viewModel.uiState.value.error)
        
        // Act
        viewModel.clearError()
        
        // Assert
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `reset returns state to initial values`() = runTest {
        // Arrange - modify state
        val userId = "user123"
        val room = Room("room123", listOf(userId), System.currentTimeMillis())
        coEvery { roomRepository.createRoom(userId) } returns Pair(room, "BAR-TOK")
        
        viewModel.createRoom(userId)
        viewModel.updateInviteCodeInput("test-input")
        
        // Verify state is modified
        val modifiedState = viewModel.uiState.value
        assertNotNull(modifiedState.room)
        assertNotNull(modifiedState.inviteCode)
        assertEquals("TEST-INPUT", modifiedState.inviteCodeInput)
        assertEquals(PairingStep.ROOM_CREATED, modifiedState.pairingStep)
        
        // Act
        viewModel.reset()
        
        // Assert - state is back to initial
        val resetState = viewModel.uiState.value
        assertFalse(resetState.isLoading)
        assertNull(resetState.room)
        assertNull(resetState.inviteCode)
        assertEquals("", resetState.inviteCodeInput)
        assertEquals(PairingStep.CHOOSE_ACTION, resetState.pairingStep)
        assertNull(resetState.error)
    }
}