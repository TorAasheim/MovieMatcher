package com.moviematcher.app.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviematcher.app.data.engine.MovieRecommendationEngine
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.SwipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing swipe decisions and partner synchronization
 */
@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val swipeRepository: SwipeRepository,
    private val recommendationEngine: MovieRecommendationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    private val _partnerSwipes = MutableStateFlow<List<Swipe>>(emptyList())
    val partnerSwipes: StateFlow<List<Swipe>> = _partnerSwipes.asStateFlow()

    private val _currentMovie = MutableStateFlow<Movie?>(null)
    val currentMovie: StateFlow<Movie?> = _currentMovie.asStateFlow()

    // Expose recommendation engine states
    val movieQueue = recommendationEngine.movieQueue
    val isLoadingRecommendations = recommendationEngine.isLoading
    val recommendationError = recommendationEngine.error

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var partnerId: String? = null

    /**
     * Initialize the swipe session with room and user information
     */
    fun initializeSwipeSession(roomId: String, userId: String, partnerUserId: String, preferences: UserPreferences) {
        currentRoomId = roomId
        currentUserId = userId
        partnerId = partnerUserId
        
        // Initialize recommendation engine with preferences
        viewModelScope.launch {
            try {
                recommendationEngine.initializeQueue(preferences)
                loadNextMovie()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to initialize recommendations: ${e.message}"
                )
            }
        }
        
        // Start observing partner swipes
        observePartnerSwipes()
    }

    /**
     * Record a swipe decision for the current user
     */
    fun recordSwipe(titleId: Long, decision: SwipeDecision) {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val swipe = Swipe(
                    titleId = titleId,
                    userId = userId,
                    decision = decision,
                    timestamp = System.currentTimeMillis()
                )

                swipeRepository.recordSwipe(roomId, swipe)
                
                // Update last swipe for undo functionality and load next movie
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSwipe = swipe
                )
                
                // Load the next movie from the recommendation queue
                loadNextMovie()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to record swipe: ${e.message}"
                )
            }
        }
    }

    /**
     * Undo the last swipe decision
     */
    fun undoLastSwipe() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        val lastSwipe = _uiState.value.lastSwipe ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                swipeRepository.undoLastSwipe(roomId, userId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSwipe = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to undo swipe: ${e.message}"
                )
            }
        }
    }

    /**
     * Start observing partner's swipe decisions in real-time
     */
    private fun observePartnerSwipes() {
        val roomId = currentRoomId ?: return
        val partnerUserId = partnerId ?: return

        viewModelScope.launch {
            swipeRepository.observePartnerSwipes(roomId, partnerUserId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to observe partner swipes: ${error.message}"
                    )
                }
                .collect { partnerSwipe ->
                    // Add new partner swipe to the list
                    val currentSwipes = _partnerSwipes.value.toMutableList()
                    
                    // Check if this swipe already exists (to avoid duplicates)
                    val existingSwipeIndex = currentSwipes.indexOfFirst { 
                        it.titleId == partnerSwipe.titleId && it.userId == partnerSwipe.userId 
                    }
                    
                    if (existingSwipeIndex == -1) {
                        // Add new swipe at the beginning (most recent first)
                        currentSwipes.add(0, partnerSwipe)
                        _partnerSwipes.value = currentSwipes
                        
                        // Update UI state to show partner activity
                        _uiState.value = _uiState.value.copy(
                            lastPartnerSwipe = partnerSwipe
                        )
                    }
                }
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = _uiState.value.lastSwipe != null && !_uiState.value.isLoading

    /**
     * Load the next movie from the recommendation queue
     */
    private suspend fun loadNextMovie() {
        try {
            val nextMovie = recommendationEngine.getNextMovie()
            _currentMovie.value = nextMovie
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load next movie: ${e.message}"
            )
        }
    }

    /**
     * Update user preferences and refresh recommendations
     */
    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            try {
                recommendationEngine.updatePreferences(preferences)
                loadNextMovie()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update preferences: ${e.message}"
                )
            }
        }
    }

    /**
     * Manually refresh the recommendation queue
     */
    fun refreshRecommendations() {
        viewModelScope.launch {
            try {
                loadNextMovie()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to refresh recommendations: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear recommendation engine errors
     */
    fun clearRecommendationError() {
        recommendationEngine.clearError()
    }
}

/**
 * UI state for the swipe screen
 */
data class SwipeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastSwipe: Swipe? = null,
    val lastPartnerSwipe: Swipe? = null
)