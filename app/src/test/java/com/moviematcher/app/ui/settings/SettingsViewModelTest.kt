package com.moviematcher.app.ui.settings

import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences
import com.moviematcher.app.data.repository.CleanupRepository
import com.moviematcher.app.data.repository.PreferencesRepository
import com.moviematcher.app.data.repository.TmdbRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    private lateinit var mockPreferencesRepository: PreferencesRepository
    private lateinit var mockTmdbRepository: TmdbRepository
    private lateinit var mockCleanupRepository: CleanupRepository
    private lateinit var viewModel: SettingsViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    private val testRoomId = "test-room-123"
    
    private val testGenres = listOf(
        Genre(28, "Action"),
        Genre(35, "Comedy"),
        Genre(18, "Drama")
    )
    
    private val testProviders = listOf(
        StreamingProvider(8, "Netflix", null, null),
        StreamingProvider(9, "Amazon Prime Video", null, null),
        StreamingProvider(15, "Hulu", null, null)
    )
    
    private val testPreferences = UserPreferences(
        selectedGenres = setOf(28, 35),
        yearRange = 2000..2023,
        minRating = 7.5,
        selectedProviders = setOf(8, 9),
        availabilityStrict = true,
        contentType = ContentType.BOTH
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockPreferencesRepository = mockk()
        mockTmdbRepository = mockk()
        mockCleanupRepository = mockk()
        
        // Setup default mocks
        coEvery { mockTmdbRepository.getGenres() } returns testGenres
        coEvery { mockTmdbRepository.getPopularStreamingProviders() } returns testProviders
        every { mockPreferencesRepository.observePreferences(any()) } returns flowOf(testPreferences)
        
        viewModel = SettingsViewModel(mockPreferencesRepository, mockTmdbRepository, mockCleanupRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `initializeForRoom loads preferences and updates state`() = runTest {
        // When
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(testPreferences, viewModel.preferences.value)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        
        verify { mockPreferencesRepository.observePreferences(testRoomId) }
    }
    
    @Test
    fun `loadAvailableOptions loads genres and providers`() = runTest {
        // When
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(testGenres, viewModel.availableGenres.value)
        assertEquals(testProviders, viewModel.availableProviders.value)
        
        coVerify { mockTmdbRepository.getGenres() }
        coVerify { mockTmdbRepository.getPopularStreamingProviders() }
    }
    
    @Test
    fun `updateSelectedGenres calls repository with correct data`() = runTest {
        // Given
        val selectedGenres = setOf(28, 35, 18)
        coEvery { mockPreferencesRepository.updateSelectedGenres(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateSelectedGenres(selectedGenres)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateSelectedGenres(testRoomId, selectedGenres) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `updateYearRange calls repository with correct data`() = runTest {
        // Given
        val yearRange = 2010..2022
        coEvery { mockPreferencesRepository.updateYearRange(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateYearRange(yearRange)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateYearRange(testRoomId, yearRange) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `updateMinRating validates and calls repository`() = runTest {
        // Given
        val minRating = 8.5
        coEvery { mockPreferencesRepository.updateMinRating(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateMinRating(minRating)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateMinRating(testRoomId, minRating) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `updateMinRating clamps invalid values`() = runTest {
        // Given
        val invalidRating = 15.0 // Above max of 10.0
        coEvery { mockPreferencesRepository.updateMinRating(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateMinRating(invalidRating)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateMinRating(testRoomId, 10.0) }
    }
    
    @Test
    fun `updateSelectedProviders calls repository with correct data`() = runTest {
        // Given
        val selectedProviders = setOf(8, 9, 15)
        coEvery { mockPreferencesRepository.updateSelectedProviders(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateSelectedProviders(selectedProviders)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateSelectedProviders(testRoomId, selectedProviders) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `updateAvailabilityStrict calls repository with correct data`() = runTest {
        // Given
        val availabilityStrict = false
        coEvery { mockPreferencesRepository.updateAvailabilityStrict(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateAvailabilityStrict(availabilityStrict)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateAvailabilityStrict(testRoomId, availabilityStrict) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `updateContentType calls repository with correct data`() = runTest {
        // Given
        val contentType = ContentType.TV
        coEvery { mockPreferencesRepository.updateContentType(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateContentType(contentType)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockPreferencesRepository.updateContentType(testRoomId, contentType) }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `toggleGenre adds genre when not selected`() = runTest {
        // Given
        val genreId = 18 // Drama, not in current selection
        coEvery { mockPreferencesRepository.updateSelectedGenres(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleGenre(genreId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val expectedGenres = setOf(28, 35, 18) // Added 18
        coVerify { mockPreferencesRepository.updateSelectedGenres(testRoomId, expectedGenres) }
    }
    
    @Test
    fun `toggleGenre removes genre when already selected`() = runTest {
        // Given
        val genreId = 28 // Action, already in current selection
        coEvery { mockPreferencesRepository.updateSelectedGenres(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleGenre(genreId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val expectedGenres = setOf(35) // Removed 28
        coVerify { mockPreferencesRepository.updateSelectedGenres(testRoomId, expectedGenres) }
    }
    
    @Test
    fun `toggleProvider adds provider when not selected`() = runTest {
        // Given
        val providerId = 15 // Hulu, not in current selection
        coEvery { mockPreferencesRepository.updateSelectedProviders(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleProvider(providerId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val expectedProviders = setOf(8, 9, 15) // Added 15
        coVerify { mockPreferencesRepository.updateSelectedProviders(testRoomId, expectedProviders) }
    }
    
    @Test
    fun `toggleProvider removes provider when already selected`() = runTest {
        // Given
        val providerId = 8 // Netflix, already in current selection
        coEvery { mockPreferencesRepository.updateSelectedProviders(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.toggleProvider(providerId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val expectedProviders = setOf(9) // Removed 8
        coVerify { mockPreferencesRepository.updateSelectedProviders(testRoomId, expectedProviders) }
    }
    
    @Test
    fun `resetToDefaults calls repository with default preferences`() = runTest {
        // Given
        coEvery { mockPreferencesRepository.updatePreferences(any(), any()) } just Runs
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.resetToDefaults()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { 
            mockPreferencesRepository.updatePreferences(
                testRoomId, 
                match { preferences ->
                    preferences.selectedGenres.isEmpty() &&
                    preferences.yearRange == 1990..2024 &&
                    preferences.minRating == 6.0 &&
                    preferences.selectedProviders.isEmpty() &&
                    !preferences.availabilityStrict &&
                    preferences.contentType == ContentType.MOVIE
                }
            )
        }
        assertFalse(viewModel.uiState.value.isSaving)
    }
    
    @Test
    fun `error handling sets error state correctly`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { mockPreferencesRepository.updateSelectedGenres(any(), any()) } throws Exception(errorMessage)
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateSelectedGenres(setOf(28))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.error?.contains("Failed to update genres") == true)
    }
    
    @Test
    fun `clearError removes error from state`() = runTest {
        // Given - Set an error state first
        coEvery { mockPreferencesRepository.updateSelectedGenres(any(), any()) } throws Exception("Test error")
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.updateSelectedGenres(setOf(28))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error is set
        assertNotNull(viewModel.uiState.value.error)
        
        // When
        viewModel.clearError()
        
        // Then
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `getYearRangeString formats correctly`() {
        // Given
        val yearRange = 2000..2023
        
        // When
        val result = viewModel.getYearRangeString(yearRange)
        
        // Then
        assertEquals("2000 - 2023", result)
    }
    
    @Test
    fun `getRatingString formats correctly`() {
        // Given
        val rating = 7.5
        
        // When
        val result = viewModel.getRatingString(rating)
        
        // Then
        assertEquals("7.5+", result)
    }
    
    @Test
    fun `clearMySwipes calls cleanup repository and updates state with success message`() = runTest {
        // Given
        val swipesDeleted = 5
        coEvery { mockCleanupRepository.clearUserSwipes(any()) } returns swipesDeleted
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearMySwipes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockCleanupRepository.clearUserSwipes(testRoomId) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Successfully cleared 5 swipes", viewModel.uiState.value.cleanupResult)
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `clearMySwipes handles zero swipes deleted`() = runTest {
        // Given
        val swipesDeleted = 0
        coEvery { mockCleanupRepository.clearUserSwipes(any()) } returns swipesDeleted
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearMySwipes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockCleanupRepository.clearUserSwipes(testRoomId) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Successfully cleared 0 swipes", viewModel.uiState.value.cleanupResult)
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `clearMySwipes handles error and sets error state`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { mockCleanupRepository.clearUserSwipes(any()) } throws Exception(errorMessage)
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearMySwipes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockCleanupRepository.clearUserSwipes(testRoomId) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.cleanupResult)
        assertTrue(viewModel.uiState.value.error?.contains("Failed to clear swipes") == true)
        assertTrue(viewModel.uiState.value.error?.contains(errorMessage) == true)
    }
    
    @Test
    fun `clearMySwipes does nothing when no room is initialized`() = runTest {
        // Given - Don't initialize room
        
        // When
        viewModel.clearMySwipes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify(exactly = 0) { mockCleanupRepository.clearUserSwipes(any()) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.cleanupResult)
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `clearCleanupResult removes cleanup result from state`() = runTest {
        // Given - Set a cleanup result first
        coEvery { mockCleanupRepository.clearUserSwipes(any()) } returns 3
        
        viewModel.initializeForRoom(testRoomId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.clearMySwipes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify cleanup result is set
        assertNotNull(viewModel.uiState.value.cleanupResult)
        
        // When
        viewModel.clearCleanupResult()
        
        // Then
        assertNull(viewModel.uiState.value.cleanupResult)
    }
}