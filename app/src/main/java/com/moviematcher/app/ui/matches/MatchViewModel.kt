package com.moviematcher.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviematcher.app.data.model.Match
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.MatchRepository
import com.moviematcher.app.data.repository.MovieRepository
import com.moviematcher.app.data.repository.PreferencesRepository
import com.moviematcher.app.domain.SuggestionAlgorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing matches screen state and operations
 */
@HiltViewModel
class MatchViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val movieRepository: MovieRepository,
    private val preferencesRepository: PreferencesRepository,
    private val suggestionAlgorithm: SuggestionAlgorithm
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    private val _enrichedMatches = MutableStateFlow<List<EnrichedMatch>>(emptyList())
    val enrichedMatches: StateFlow<List<EnrichedMatch>> = _enrichedMatches.asStateFlow()

    private val _tonightsPick = MutableStateFlow<EnrichedMatch?>(null)
    val tonightsPick: StateFlow<EnrichedMatch?> = _tonightsPick.asStateFlow()

    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences.asStateFlow()

    private var currentRoomId: String? = null

    /**
     * Initialize matches observation for a room
     */
    fun initializeMatches(roomId: String) {
        if (currentRoomId == roomId) return // Already initialized for this room
        
        currentRoomId = roomId
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Observe both matches and preferences
            combine(
                matchRepository.observeMatches(roomId),
                preferencesRepository.observePreferences(roomId)
            ) { matches, preferences ->
                _matches.value = matches
                _userPreferences.value = preferences
                enrichMatches(matches)
                updateTonightsPick()
            }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load matches: ${error.message}"
                    )
                }
                .collect { }
        }
    }

    /**
     * Enrich matches with movie details and streaming providers
     */
    private suspend fun enrichMatches(matches: List<Match>) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        try {
            val enrichedMatches = matches.map { match ->
                val movieDetails = if (match.movieDetails == null) {
                    try {
                        movieRepository.getMovieDetails(match.titleId)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    match.movieDetails
                }

                val streamingProviders = if (match.streamingProviders.isEmpty()) {
                    try {
                        movieRepository.getStreamingProviders(match.titleId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    match.streamingProviders
                }

                EnrichedMatch(
                    match = match,
                    movieDetails = movieDetails,
                    streamingProviders = streamingProviders
                )
            }

            _enrichedMatches.value = enrichedMatches
            _uiState.value = _uiState.value.copy(isLoading = false)
            updateTonightsPick()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load movie details: ${e.message}"
            )
        }
    }

    /**
     * Mark a match as watched with optional notes
     */
    fun markAsWatched(movieId: Long, notes: String = "") {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                matchRepository.markAsWatched(roomId, movieId, notes)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to mark as watched: ${e.message}"
                )
            }
        }
    }

    /**
     * Open streaming provider deep link or fallback to web
     */
    fun openStreamingProvider(provider: StreamingProvider, movieTitle: String): String? {
        return provider.deepLinkUrl ?: generateWebFallbackUrl(provider, movieTitle)
    }

    /**
     * Generate web fallback URL for streaming provider
     */
    private fun generateWebFallbackUrl(provider: StreamingProvider, movieTitle: String): String {
        return when (provider.name.lowercase()) {
            "netflix" -> "https://www.netflix.com/search?q=${movieTitle.replace(" ", "%20")}"
            "amazon prime video" -> "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=${movieTitle.replace(" ", "%20")}"
            "disney plus", "disney+" -> "https://www.disneyplus.com/search?q=${movieTitle.replace(" ", "%20")}"
            "hulu" -> "https://www.hulu.com/search?q=${movieTitle.replace(" ", "%20")}"
            "hbo max", "max" -> "https://www.max.com/search?q=${movieTitle.replace(" ", "%20")}"
            "apple tv plus", "apple tv+" -> "https://tv.apple.com/search?term=${movieTitle.replace(" ", "%20")}"
            "paramount plus", "paramount+" -> "https://www.paramountplus.com/search/?query=${movieTitle.replace(" ", "%20")}"
            "peacock" -> "https://www.peacocktv.com/search/${movieTitle.replace(" ", "-")}"
            else -> "https://www.google.com/search?q=${movieTitle.replace(" ", "%20")}+${provider.name.replace(" ", "%20")}+watch+online"
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Update tonight's pick suggestion based on current matches and preferences
     */
    private fun updateTonightsPick() {
        val preferences = _userPreferences.value
        if (preferences != null) {
            val suggestion = suggestionAlgorithm.suggestTonightsPick(
                matches = _enrichedMatches.value,
                preferences = preferences
            )
            _tonightsPick.value = suggestion
        }
    }

    /**
     * Get multiple suggestions ranked by score
     */
    fun getRankedSuggestions(limit: Int = 5): List<EnrichedMatch> {
        val preferences = _userPreferences.value ?: return emptyList()
        return suggestionAlgorithm.getSuggestionsRanked(
            matches = _enrichedMatches.value,
            preferences = preferences,
            limit = limit
        )
    }

    /**
     * Refresh tonight's pick suggestion
     */
    fun refreshTonightsPick() {
        updateTonightsPick()
    }
}

/**
 * UI state for matches screen
 */
data class MatchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Match enriched with movie details and streaming providers
 */
data class EnrichedMatch(
    val match: Match,
    val movieDetails: Movie?,
    val streamingProviders: List<StreamingProvider>
)