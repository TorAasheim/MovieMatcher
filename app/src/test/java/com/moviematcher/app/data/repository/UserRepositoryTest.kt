package com.moviematcher.app.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.moviematcher.app.data.model.User
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {
    
    @Mock
    private lateinit var firestore: FirebaseFirestore
    
    @Mock
    private lateinit var messaging: FirebaseMessaging
    
    @Mock
    private lateinit var firebaseUser: FirebaseUser
    
    @Mock
    private lateinit var collectionReference: CollectionReference
    
    @Mock
    private lateinit var documentReference: DocumentReference
    
    @Mock
    private lateinit var documentSnapshot: DocumentSnapshot
    
    @Mock
    private lateinit var fcmTokenTask: Task<String>
    
    @Mock
    private lateinit var firestoreTask: Task<Void>
    
    @Mock
    private lateinit var getTask: Task<DocumentSnapshot>
    
    private lateinit var userRepository: UserRepository
    
    @Before
    fun setup() {
        userRepository = UserRepository(firestore, messaging)
        
        // Setup Firestore mocks
        whenever(firestore.collection("users")).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(documentReference.set(any())).thenReturn(firestoreTask)
        whenever(documentReference.update(any<String>(), any())).thenReturn(firestoreTask)
        whenever(documentReference.delete()).thenReturn(firestoreTask)
        whenever(documentReference.get()).thenReturn(getTask)
        
        // Setup successful task completion
        whenever(firestoreTask.isComplete).thenReturn(true)
        whenever(firestoreTask.isSuccessful).thenReturn(true)
        whenever(getTask.isComplete).thenReturn(true)
        whenever(getTask.isSuccessful).thenReturn(true)
        whenever(getTask.result).thenReturn(documentSnapshot)
    }
    
    @Test
    fun `createOrUpdateUser creates user with FCM token`() = runTest {
        // Given
        val userId = "test-user-id"
        val displayName = "Test User"
        val photoUrl = "https://example.com/photo.jpg"
        val fcmToken = "test-fcm-token"
        
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn(displayName)
        whenever(firebaseUser.photoUrl).thenReturn(Uri.parse(photoUrl))
        whenever(messaging.token).thenReturn(Tasks.forResult(fcmToken))
        
        // When
        val result = userRepository.createOrUpdateUser(firebaseUser)
        
        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals(displayName, result.displayName)
        assertEquals(photoUrl, result.photoUrl)
        assertEquals(fcmToken, result.fcmToken)
        assertNull(result.roomId)
        
        verify(documentReference).set(any<User>())
    }
    
    @Test
    fun `createOrUpdateUser handles FCM token failure gracefully`() = runTest {
        // Given
        val userId = "test-user-id"
        val displayName = "Test User"
        
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn(displayName)
        whenever(firebaseUser.photoUrl).thenReturn(null)
        whenever(messaging.token).thenReturn(Tasks.forException(Exception("FCM failed")))
        
        // When
        val result = userRepository.createOrUpdateUser(firebaseUser)
        
        // Then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals(displayName, result.displayName)
        assertNull(result.photoUrl)
        assertNull(result.fcmToken) // Should be null when FCM fails
        
        verify(documentReference).set(any<User>())
    }
    
    @Test
    fun `getUser returns user when document exists`() = runTest {
        // Given
        val userId = "test-user-id"
        val user = User(
            id = userId,
            displayName = "Test User",
            photoUrl = null,
            fcmToken = "token",
            roomId = null,
            createdAt = System.currentTimeMillis()
        )
        
        whenever(documentSnapshot.toObject(User::class.java)).thenReturn(user)
        
        // When
        val result = userRepository.getUser(userId)
        
        // Then
        assertEquals(user, result)
        verify(collectionReference).document(userId)
    }
    
    @Test
    fun `getUser returns null when document does not exist`() = runTest {
        // Given
        val userId = "test-user-id"
        whenever(documentSnapshot.toObject(User::class.java)).thenReturn(null)
        
        // When
        val result = userRepository.getUser(userId)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `updateFcmToken updates token successfully`() = runTest {
        // Given
        val userId = "test-user-id"
        val fcmToken = "new-fcm-token"
        
        // When
        userRepository.updateFcmToken(userId, fcmToken)
        
        // Then
        verify(documentReference).update("fcmToken", fcmToken)
    }
    
    @Test
    fun `updateUserRoom updates room ID successfully`() = runTest {
        // Given
        val userId = "test-user-id"
        val roomId = "test-room-id"
        
        // When
        userRepository.updateUserRoom(userId, roomId)
        
        // Then
        verify(documentReference).update("roomId", roomId)
    }
    
    @Test
    fun `deleteUser deletes document successfully`() = runTest {
        // Given
        val userId = "test-user-id"
        
        // When
        userRepository.deleteUser(userId)
        
        // Then
        verify(documentReference).delete()
    }
}