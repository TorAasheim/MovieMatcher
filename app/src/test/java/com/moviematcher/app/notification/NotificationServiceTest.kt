package com.moviematcher.app.notification

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class NotificationServiceTest {

    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var firestore: FirebaseFirestore
    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        firebaseMessaging = mockk()
        firestore = mockk()
        notificationService = NotificationService(firebaseMessaging, firestore)
    }

    @Test
    fun `sendMatchNotification should send notifications to users with FCM tokens`() = runTest {
        // Arrange
        val userIds = listOf("user1", "user2")
        val movieId = 123L
        val token1 = "fcm_token_1"
        val token2 = "fcm_token_2"
        
        val userDoc1 = mockk<DocumentSnapshot>()
        val userDoc2 = mockk<DocumentSnapshot>()
        val task1 = mockk<Task<DocumentSnapshot>>()
        val task2 = mockk<Task<DocumentSnapshot>>()
        
        every { firestore.collection("users") } returns mockk {
            every { document("user1") } returns mockk {
                every { get() } returns task1
            }
            every { document("user2") } returns mockk {
                every { get() } returns task2
            }
        }
        
        every { task1.isComplete } returns true
        every { task1.exception } returns null
        every { task1.isCanceled } returns false
        every { task1.result } returns userDoc1
        
        every { task2.isComplete } returns true
        every { task2.exception } returns null
        every { task2.isCanceled } returns false
        every { task2.result } returns userDoc2
        
        every { userDoc1.getString("fcmToken") } returns token1
        every { userDoc2.getString("fcmToken") } returns token2
        
        // Act
        notificationService.sendMatchNotification(userIds, movieId)
        
        // Assert
        verify { userDoc1.getString("fcmToken") }
        verify { userDoc2.getString("fcmToken") }
        // Note: In a real implementation, you'd verify the actual notification sending
        // but since we're using a placeholder implementation, we just verify token retrieval
    }

    @Test
    fun `sendMatchNotification should handle users without FCM tokens gracefully`() = runTest {
        // Arrange
        val userIds = listOf("user1", "user2")
        val movieId = 123L
        
        val userDoc1 = mockk<DocumentSnapshot>()
        val userDoc2 = mockk<DocumentSnapshot>()
        val task1 = mockk<Task<DocumentSnapshot>>()
        val task2 = mockk<Task<DocumentSnapshot>>()
        
        every { firestore.collection("users") } returns mockk {
            every { document("user1") } returns mockk {
                every { get() } returns task1
            }
            every { document("user2") } returns mockk {
                every { get() } returns task2
            }
        }
        
        every { task1.isComplete } returns true
        every { task1.exception } returns null
        every { task1.isCanceled } returns false
        every { task1.result } returns userDoc1
        
        every { task2.isComplete } returns true
        every { task2.exception } returns null
        every { task2.isCanceled } returns false
        every { task2.result } returns userDoc2
        
        every { userDoc1.getString("fcmToken") } returns null
        every { userDoc2.getString("fcmToken") } returns null
        
        // Act & Assert - should not throw exception
        notificationService.sendMatchNotification(userIds, movieId)
        
        verify { userDoc1.getString("fcmToken") }
        verify { userDoc2.getString("fcmToken") }
    }

    @Test
    fun `sendMatchNotification should handle Firestore errors gracefully`() = runTest {
        // Arrange
        val userIds = listOf("user1")
        val movieId = 123L
        
        val task = mockk<Task<DocumentSnapshot>>()
        
        every { firestore.collection("users") } returns mockk {
            every { document("user1") } returns mockk {
                every { get() } returns task
            }
        }
        
        every { task.isComplete } returns true
        every { task.exception } returns RuntimeException("Firestore error")
        every { task.isCanceled } returns false
        
        // Act & Assert - should not throw exception
        notificationService.sendMatchNotification(userIds, movieId)
        
        // The method should handle the exception gracefully
    }
}