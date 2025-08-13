package com.moviematcher.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviematcher.app.data.model.Room
import com.moviematcher.app.data.repository.RoomCreationException
import com.moviematcher.app.data.repository.RoomJoinException
import com.moviematcher.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing room creation and joining flow
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()
    
    /**
     * Creates a new room for the current user
     */
    fun createRoom(userId: String) {
        if (_uiState.value.isLoading) return
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        
        viewModelScope.launch {
            try {
                val (room, inviteCode) = roomRepository.createRoom(userId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    room = room,
                    inviteCode = inviteCode,
                    pairingStep = PairingStep.ROOM_CREATED
                )
            } catch (e: RoomCreationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to create room: ${e.message}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred"
                )
            }
        }
    }
    
    /**
     * Joins an existing room using an invite code
     */
    fun joinRoom(userId: String, inviteCode: String) {
        if (_uiState.value.isLoading) return
        
        val trimmedCode = inviteCode.trim().uppercase()
        if (trimmedCode.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter an invite code"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        
        viewModelScope.launch {
            try {
                val room = roomRepository.joinRoom(userId, trimmedCode)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    room = room,
                    pairingStep = PairingStep.ROOM_JOINED
                )
            } catch (e: RoomJoinException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("Invalid invite code format") == true -> 
                            "Invalid invite code format. Please check and try again."
                        e.message?.contains("Invalid invite code") == true -> 
                            "This invite code doesn't exist. Please check and try again."
                        e.message?.contains("Room is full") == true -> 
                            "This room is already full. Only 2 people can be in a room."
                        e.message?.contains("already in this room") == true -> 
                            "You're already in this room."
                        e.message?.contains("Room no longer exists") == true -> 
                            "This room no longer exists."
                        else -> "Failed to join room: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred"
                )
            }
        }
    }
    
    /**
     * Updates the invite code input
     */
    fun updateInviteCodeInput(code: String) {
        _uiState.value = _uiState.value.copy(
            inviteCodeInput = code.uppercase(),
            error = null
        )
    }
    
    /**
     * Sets the current pairing step
     */
    fun setPairingStep(step: PairingStep) {
        _uiState.value = _uiState.value.copy(
            pairingStep = step,
            error = null
        )
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Resets the pairing state
     */
    fun reset() {
        _uiState.value = PairingUiState()
    }
}

/**
 * UI state for the pairing screen
 */
data class PairingUiState(
    val isLoading: Boolean = false,
    val room: Room? = null,
    val inviteCode: String? = null,
    val inviteCodeInput: String = "",
    val pairingStep: PairingStep = PairingStep.CHOOSE_ACTION,
    val error: String? = null
)

/**
 * Steps in the pairing flow
 */
enum class PairingStep {
    CHOOSE_ACTION,      // Choose between creating or joining a room
    CREATE_ROOM,        // Creating a new room
    JOIN_ROOM,          // Joining an existing room
    ROOM_CREATED,       // Room successfully created, showing invite code
    ROOM_JOINED,        // Successfully joined a room
    PAIRING_COMPLETE    // Both users are in the room
}