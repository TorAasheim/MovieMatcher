package com.moviematcher.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CleanupRepositoryTest {
    
    private lateinit var functions: FirebaseFunctions
    private lateinit var httpsCallable: HttpsCallableReference
    private lateinit var repository: CleanupRepositoryImpl
    
    @Before
    fun setup() {
        functions = mockk()
        httpsCallable = mockk()
        repository = CleanupRepositoryImpl(functions)
    }
    
    @Test
    fun `clearUserSwipes should return number of deleted swipes on success`() = runTest {
        // Given
        val roomId = "test-room-id"
        val expectedSwipesDeleted = 5
        val resultData = mapOf("swipesDeleted" to expectedSwipesDeleted)
        val result = mockk<HttpsCallableResult> {
            every { data } returns resultData
        }
        val task = Tasks.forResult(result)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When
        val actualSwipesDeleted = repository.clearUserSwipes(roomId)
        
        // Then
        assertEquals(expectedSwipesDeleted, actualSwipesDeleted)
        verify { httpsCallable.call(mapOf("roomId" to roomId)) }
    }
    
    @Test
    fun `clearUserSwipes should return 0 when result data is null`() = runTest {
        // Given
        val roomId = "test-room-id"
        val result = mockk<HttpsCallableResult> {
            every { data } returns null
        }
        val task = Tasks.forResult(result)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When
        val actualSwipesDeleted = repository.clearUserSwipes(roomId)
        
        // Then
        assertEquals(0, actualSwipesDeleted)
    }
    
    @Test
    fun `clearUserSwipes should return 0 when swipesDeleted is not a number`() = runTest {
        // Given
        val roomId = "test-room-id"
        val resultData = mapOf("swipesDeleted" to "not-a-number")
        val result = mockk<HttpsCallableResult> {
            every { data } returns resultData
        }
        val task = Tasks.forResult(result)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When
        val actualSwipesDeleted = repository.clearUserSwipes(roomId)
        
        // Then
        assertEquals(0, actualSwipesDeleted)
    }
    
    @Test
    fun `clearUserSwipes should throw CleanupException on function call failure`() = runTest {
        // Given
        val roomId = "test-room-id"
        val exception = RuntimeException("Function call failed")
        val task = Tasks.forException<HttpsCallableResult>(exception)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When & Then
        val thrownException = assertThrows(CleanupException::class.java) {
            repository.clearUserSwipes(roomId)
        }
        
        assertEquals("Failed to clear swipes: Function call failed", thrownException.message)
        assertEquals(exception, thrownException.cause)
    }
    
    @Test
    fun `clearUserSwipes should handle double values correctly`() = runTest {
        // Given
        val roomId = "test-room-id"
        val expectedSwipesDeleted = 3.0
        val resultData = mapOf("swipesDeleted" to expectedSwipesDeleted)
        val result = mockk<HttpsCallableResult> {
            every { data } returns resultData
        }
        val task = Tasks.forResult(result)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When
        val actualSwipesDeleted = repository.clearUserSwipes(roomId)
        
        // Then
        assertEquals(3, actualSwipesDeleted)
    }
    
    @Test
    fun `clearUserSwipes should handle long values correctly`() = runTest {
        // Given
        val roomId = "test-room-id"
        val expectedSwipesDeleted = 7L
        val resultData = mapOf("swipesDeleted" to expectedSwipesDeleted)
        val result = mockk<HttpsCallableResult> {
            every { data } returns resultData
        }
        val task = Tasks.forResult(result)
        
        every { functions.getHttpsCallable("clearUserSwipes") } returns httpsCallable
        every { httpsCallable.call(any()) } returns task
        
        // When
        val actualSwipesDeleted = repository.clearUserSwipes(roomId)
        
        // Then
        assertEquals(7, actualSwipesDeleted)
    }
}