package com.moviematcher.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.UserPreferences
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreferencesRepositoryTest {
    
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference
    private lateinit var mockPreferencesCollection: CollectionReference
    private lateinit var mockPreferencesDocument: DocumentReference
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    private lateinit var repository: PreferencesRepository
    
    private val testRoomId = "test-room-123"
    
    @Before
    fun setup() {
        mockFirestore = mockk()
        mockCollection = mockk()
        mockDocument = mockk()
        mockPreferencesCollection = mockk()
        mockPreferencesDocument = mockk()
        mockDocumentSnapshot = mockk()
        
        // Setup Firestore mock chain
        every { mockFirestore.collection("rooms") } returns mockCollection
        every { mockCollection.document(testRoomId) } returns mockDocument
        every { mockDocument.collection("preferences") } returns mockPreferencesCollection
        every { mockPreferencesCollection.document("preferences") } returns mockPreferencesDocument
        
        repository = PreferencesRepository(mockFirestore)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `getPreferences returns default preferences when document does not exist`() = runTest {
        // Given
        val mockTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocumentSnapshot)
        every { mockPreferencesDocument.get() } returns mockTask
        every { mockDocumentSnapshot.exists() } returns false
        
        // When
        val result = repository.getPreferences(testRoomId)
        
        // Then
        assertEquals(emptySet<Int>(), result.selectedGenres)
        assertEquals(1990..2024, result.yearRange)
        assertEquals(6.0, result.minRating, 0.01)
        assertEquals(emptySet<Int>(), result.selectedProviders)
        assertFalse(result.availabilityStrict)
        assertEquals(ContentType.MOVIE, result.contentType)
    }
    
    @Test
    fun `getPreferences returns parsed preferences when document exists`() = runTest {
        // Given
        val preferencesData = mapOf(
            "selectedGenres" to listOf(28, 35, 18), // Action, Comedy, Drama
            "yearRange" to mapOf("min" to 2000, "max" to 2023),
            "minRating" to 7.5,
            "selectedProviders" to listOf(8, 9), // Netflix, Amazon Prime
            "availabilityStrict" to true,
            "contentType" to "BOTH"
        )
        
        val mockTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocumentSnapshot)
        every { mockPreferencesDocument.get() } returns mockTask
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.data } returns preferencesData
        
        // When
        val result = repository.getPreferences(testRoomId)
        
        // Then
        assertEquals(setOf(28, 35, 18), result.selectedGenres)
        assertEquals(2000..2023, result.yearRange)
        assertEquals(7.5, result.minRating, 0.01)
        assertEquals(setOf(8, 9), result.selectedProviders)
        assertTrue(result.availabilityStrict)
        assertEquals(ContentType.BOTH, result.contentType)
    }
    
    @Test
    fun `getPreferences handles invalid data gracefully`() = runTest {
        // Given
        val invalidData = mapOf(
            "selectedGenres" to "invalid", // Should be list
            "yearRange" to "invalid", // Should be map
            "minRating" to "invalid", // Should be number
            "selectedProviders" to "invalid", // Should be list
            "availabilityStrict" to "invalid", // Should be boolean
            "contentType" to "INVALID_TYPE" // Invalid enum value
        )
        
        val mockTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocumentSnapshot)
        every { mockPreferencesDocument.get() } returns mockTask
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.data } returns invalidData
        
        // When
        val result = repository.getPreferences(testRoomId)
        
        // Then - Should return default values for invalid data
        assertEquals(emptySet<Int>(), result.selectedGenres)
        assertEquals(1990..2024, result.yearRange)
        assertEquals(6.0, result.minRating, 0.01)
        assertEquals(emptySet<Int>(), result.selectedProviders)
        assertFalse(result.availabilityStrict)
        assertEquals(ContentType.MOVIE, result.contentType)
    }
    
    @Test
    fun `updatePreferences saves preferences correctly`() = runTest {
        // Given
        val preferences = UserPreferences(
            selectedGenres = setOf(28, 35),
            yearRange = 2000..2023,
            minRating = 7.5,
            selectedProviders = setOf(8, 9),
            availabilityStrict = true,
            contentType = ContentType.BOTH
        )
        
        val expectedData = mapOf(
            "selectedGenres" to listOf(28, 35),
            "yearRange" to mapOf("min" to 2000, "max" to 2023),
            "minRating" to 7.5,
            "selectedProviders" to listOf(8, 9),
            "availabilityStrict" to true,
            "contentType" to "BOTH"
        )
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.set(expectedData) } returns mockTask
        
        // When
        repository.updatePreferences(testRoomId, preferences)
        
        // Then
        verify { mockPreferencesDocument.set(expectedData) }
    }
    
    @Test
    fun `updateSelectedGenres updates only genres`() = runTest {
        // Given
        val selectedGenres = setOf(28, 35, 18)
        val expectedGenresList = listOf(28, 35, 18)
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("selectedGenres", expectedGenresList) } returns mockTask
        
        // When
        repository.updateSelectedGenres(testRoomId, selectedGenres)
        
        // Then
        verify { mockPreferencesDocument.update("selectedGenres", expectedGenresList) }
    }
    
    @Test
    fun `updateYearRange updates year range correctly`() = runTest {
        // Given
        val yearRange = 2010..2022
        val expectedYearRangeMap = mapOf("min" to 2010, "max" to 2022)
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("yearRange", expectedYearRangeMap) } returns mockTask
        
        // When
        repository.updateYearRange(testRoomId, yearRange)
        
        // Then
        verify { mockPreferencesDocument.update("yearRange", expectedYearRangeMap) }
    }
    
    @Test
    fun `updateMinRating updates rating correctly`() = runTest {
        // Given
        val minRating = 8.0
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("minRating", minRating) } returns mockTask
        
        // When
        repository.updateMinRating(testRoomId, minRating)
        
        // Then
        verify { mockPreferencesDocument.update("minRating", minRating) }
    }
    
    @Test
    fun `updateSelectedProviders updates providers correctly`() = runTest {
        // Given
        val selectedProviders = setOf(8, 9, 15)
        val expectedProvidersList = listOf(8, 9, 15)
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("selectedProviders", expectedProvidersList) } returns mockTask
        
        // When
        repository.updateSelectedProviders(testRoomId, selectedProviders)
        
        // Then
        verify { mockPreferencesDocument.update("selectedProviders", expectedProvidersList) }
    }
    
    @Test
    fun `updateAvailabilityStrict updates strict mode correctly`() = runTest {
        // Given
        val availabilityStrict = true
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("availabilityStrict", availabilityStrict) } returns mockTask
        
        // When
        repository.updateAvailabilityStrict(testRoomId, availabilityStrict)
        
        // Then
        verify { mockPreferencesDocument.update("availabilityStrict", availabilityStrict) }
    }
    
    @Test
    fun `updateContentType updates content type correctly`() = runTest {
        // Given
        val contentType = ContentType.TV
        
        val mockTask: Task<Void> = Tasks.forResult(null)
        every { mockPreferencesDocument.update("contentType", "TV") } returns mockTask
        
        // When
        repository.updateContentType(testRoomId, contentType)
        
        // Then
        verify { mockPreferencesDocument.update("contentType", "TV") }
    }
}