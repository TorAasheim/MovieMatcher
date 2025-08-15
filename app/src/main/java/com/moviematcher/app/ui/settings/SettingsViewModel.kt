package com.moviematcher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.CleanupRepository
import com.moviematcher.app.data.repository.PreferencesRepository
import com.moviematcher.app.data.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing user preferences and settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val tmdbRepository: TmdbRepository,
    private val cleanupRepository: CleanupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _preferences = MutableStateFlow<UserPreferences?>(null)
    val preferences: StateFlow<UserPreferences?> = _preferences.asStateFlow()
    
    private val _availableGenres = MutableStateFlow<List<Genre>>(emptyList())
    val availableGenres: StateFlow<List<Genre>> = _availableGenres.asStateFlow()
    
    private val _availableProviders = MutableStateFlow<List<StreamingProvider>>(emptyList())
    val availableProviders: StateFlow<List<StreamingProvider>> = _availableProviders.asStateFlow()
    
    private var currentRoomId: String? = null
    
    init {
        loadAvailableOptions()
    }
    
    /**
     * Initialize settings for a specific room
     * @param roomId The room ID to load preferences for
     */
    fun initializeForRoom(roomId: String) {
        currentRoomId = roomId
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Observe preferences changes
                preferencesRepository.observePreferences(roomId)
                    .collect { preferences ->
                        _preferences.value = preferences
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load preferences: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load available genres and streaming providers
     */
    private fun loadAvailableOptions() {
        viewModelScope.launch {
            try {
                // Load genres
                val genres = tmdbRepository.getGenres()
                _availableGenres.value = genres
                
                // Load popular streaming providers
                val providers = tmdbRepository.getPopularStreamingProviders()
                _availableProviders.value = providers
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load options: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update selected genres
     * @param selectedGenres Set of genre IDs
     */
    fun updateSelectedGenres(selectedGenres: Set<Int>) {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateSelectedGenres(roomId, selectedGenres)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update genres: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update year range
     * @param yearRange The year range
     */
    fun updateYearRange(yearRange: IntRange) {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateYearRange(roomId, yearRange)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update year range: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update minimum rating
     * @param minRating The minimum rating (0.0 to 10.0)
     */
    fun updateMinRating(minRating: Double) {
        val roomId = currentRoomId ?: return
        
        // Validate rating range
        val validRating = minRating.coerceIn(0.0, 10.0)
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateMinRating(roomId, validRating)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update rating: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update selected streaming providers
     * @param selectedProviders Set of provider IDs
     */
    fun updateSelectedProviders(selectedProviders: Set<Int>) {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateSelectedProviders(roomId, selectedProviders)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update providers: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update availability strict mode
     * @param availabilityStrict Whether to use strict availability filtering
     */
    fun updateAvailabilityStrict(availabilityStrict: Boolean) {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateAvailabilityStrict(roomId, availabilityStrict)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update availability mode: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update content type
     * @param contentType The content type
     */
    fun updateContentType(contentType: ContentType) {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updateContentType(roomId, contentType)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to update content type: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle a genre selection
     * @param genreId The genre ID to toggle
     */
    fun toggleGenre(genreId: Int) {
        val currentPreferences = _preferences.value ?: return
        val currentGenres = currentPreferences.selectedGenres.toMutableSet()
        
        if (currentGenres.contains(genreId)) {
            currentGenres.remove(genreId)
        } else {
            currentGenres.add(genreId)
        }
        
        updateSelectedGenres(currentGenres)
    }
    
    /**
     * Toggle a streaming provider selection
     * @param providerId The provider ID to toggle
     */
    fun toggleProvider(providerId: Int) {
        val currentPreferences = _preferences.value ?: return
        val currentProviders = currentPreferences.selectedProviders.toMutableSet()
        
        if (currentProviders.contains(providerId)) {
            currentProviders.remove(providerId)
        } else {
            currentProviders.add(providerId)
        }
        
        updateSelectedProviders(currentProviders)
    }
    
    /**
     * Reset preferences to defaults
     */
    fun resetToDefaults() {
        val roomId = currentRoomId ?: return
        
        val defaultPreferences = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 1990..2024,
            minRating = 6.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                preferencesRepository.updatePreferences(roomId, defaultPreferences)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to reset preferences: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Get formatted year range string
     */
    fun getYearRangeString(yearRange: IntRange): String {
        return "${yearRange.first} - ${yearRange.last}"
    }
    
    /**
     * Get formatted rating string
     */
    fun getRatingString(rating: Double): String {
        return String.format("%.1f+", rating)
    }
    
    /**
     * Clear all swipes for the current user in the current room
     */
    fun clearMySwipes() {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                
                val swipesDeleted = cleanupRepository.clearUserSwipes(roomId)
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    cleanupResult = "Successfully cleared $swipesDeleted swipes"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to clear swipes: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear cleanup result message
     */
    fun clearCleanupResult() {
        _uiState.value = _uiState.value.copy(cleanupResult = null)
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val cleanupResult: String? = null
)