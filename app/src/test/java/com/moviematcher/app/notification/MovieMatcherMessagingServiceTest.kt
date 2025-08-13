package com.moviematcher.app.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.RemoteMessage
import com.moviematcher.app.data.repository.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class MovieMatcherMessagingServiceTest {
    
    @Mock
    private lateinit var userRepository: UserRepository
    
    @Mock
    private lateinit var firebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var firebaseUser: FirebaseUser
    
    @Mock
    private lateinit var remoteMessage: RemoteMessage
    
    private lateinit var messagingService: MovieMatcherMessagingService
    
    @Before
    fun setup() {
        messagingService = MovieMatcherMessagingService()
        messagingService.userRepository = userRepository
        messagingService.firebaseAuth = firebaseAuth
    }
    
    @Test
    fun `onNewToken updates user FCM token when user is signed in`() = runTest {
        // Given
        val userId = "test-user-id"
        val fcmToken = "new-fcm-token"
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(userId)
        
        // When
        messagingService.onNewToken(fcmToken)
        
        // Give coroutine time to execute
        Thread.sleep(100)
        
        // Then
        verify(userRepository).updateFcmToken(userId, fcmToken)
    }
    
    @Test
    fun `onNewToken does nothing when user is not signed in`() = runTest {
        // Given
        val fcmToken = "new-fcm-token"
        whenever(firebaseAuth.currentUser).thenReturn(null)
        
        // When
        messagingService.onNewToken(fcmToken)
        
        // Give coroutine time to execute
        Thread.sleep(100)
        
        // Then
        verify(userRepository, never()).updateFcmToken(any(), any())
    }
    
    @Test
    fun `onMessageReceived handles message without crashing`() {
        // When - should not throw exception
        messagingService.onMessageReceived(remoteMessage)
        
        // Then - test passes if no exception is thrown
    }
}