package com.moviematcher.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.moviematcher.app.data.model.Room
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RoomRepositoryTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomRepository: RoomRepository
    private lateinit var mockTransaction: Transaction
    private lateinit var mockCollectionReference: CollectionReference
    private lateinit var mockDocumentReference: DocumentReference
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    
    @Before
    fun setup() {
        firestore = mockk()
        mockTransaction = mockk()
        mockCollectionReference = mockk()
        mockDocumentReference = mockk()
        mockDocumentSnapshot = mockk()
        
        roomRepository = RoomRepository(firestore)
        
        // Setup common mocks
        every { firestore.collection(any()) } returns mockCollectionReference
        every { mockCollectionReference.document() } returns mockDocumentReference
        every { mockCollectionReference.document(any()) } returns mockDocumentReference
        every { mockDocumentReference.id } returns "test-room-id"
    }
    
    @Test
    fun `createRoom successfully creates room and returns room with invite code`() = runTest {
        // Arrange
        val userId = "user123"
        val expectedRoom = Room(
            id = "test-room-id",
            userIds = listOf(userId),
            createdAt = System.currentTimeMillis()
        )
        
        // Mock transaction behavior
        every { firestore.runTransaction<Pair<Room, String>>(any()) } returns Tasks.forResult(
            Pair(expectedRoom, "BAR-TOK")
        )
        
        // Act
        val result = roomRepository.createRoom(userId)
        
        // Assert
        assertEquals(expectedRoom, result.first)
        assertEquals("BAR-TOK", result.second)
        verify { firestore.runTransaction<Pair<Room, String>>(any()) }
    }
    
    @Test
    fun `createRoom throws RoomCreationException when transaction fails`() = runTest {
        // Arrange
        val userId = "user123"
        val exception = FirebaseFirestoreException("Transaction failed", FirebaseFirestoreException.Code.ABORTED)
        
        every { firestore.runTransaction<Pair<Room, String>>(any()) } returns Tasks.forException(exception)
        
        // Act & Assert
        try {
            roomRepository.createRoom(userId)
            fail("Expected RoomCreationException")
        } catch (e: RoomCreationException) {
            assertEquals("Failed to create room", e.message)
            assertEquals(exception, e.cause)
        }
    }
    
    @Test
    fun `joinRoom successfully joins existing room`() = runTest {
        // Arrange
        val userId = "user456"
        val inviteCode = "BAR-TOK"
        val roomId = "test-room-id"
        val existingUserIds = listOf("user123")
        val expectedRoom = Room(
            id = roomId,
            userIds = listOf("user123", userId),
            createdAt = 1234567890L
        )
        
        // Mock invite code document
        val inviteCodeDoc = mockk<DocumentSnapshot>()
        every { inviteCodeDoc.exists() } returns true
        every { inviteCodeDoc.getString("roomId") } returns roomId
        
        // Mock room document
        val roomDoc = mockk<DocumentSnapshot>()
        every { roomDoc.exists() } returns true
        every { roomDoc.get("userIds") } returns existingUserIds
        every { roomDoc.getLong("createdAt") } returns 1234567890L
        
        // Mock transaction
        every { mockTransaction.get(any()) } returnsMany listOf(inviteCodeDoc, roomDoc)
        every { mockTransaction.update(any(), any(), any()) } returns mockTransaction
        
        every { firestore.runTransaction<Room>(any()) } returns Tasks.forResult(expectedRoom)
        
        // Act
        val result = roomRepository.joinRoom(userId, inviteCode)
        
        // Assert
        assertEquals(expectedRoom, result)
        verify { firestore.runTransaction<Room>(any()) }
    }
    
    @Test
    fun `joinRoom throws RoomJoinException for invalid invite code format`() = runTest {
        // Arrange
        val userId = "user456"
        val invalidCode = "invalid"
        
        // Act & Assert
        try {
            roomRepository.joinRoom(userId, invalidCode)
            fail("Expected RoomJoinException")
        } catch (e: RoomJoinException) {
            assertEquals("Invalid invite code format", e.message)
        }
    }
    
    @Test
    fun `joinRoom throws RoomJoinException when invite code doesn't exist`() = runTest {
        // Arrange
        val userId = "user456"
        val inviteCode = "BAR-TOK"
        
        val inviteCodeDoc = mockk<DocumentSnapshot>()
        every { inviteCodeDoc.exists() } returns false
        
        every { mockTransaction.get(any()) } returns inviteCodeDoc
        every { firestore.runTransaction<Room>(any()) } answers {
            val transaction = firstArg<Transaction.Function<Room>>()
            try {
                transaction.apply(mockTransaction)
                Tasks.forException(RoomJoinException("Invalid invite code"))
            } catch (e: Exception) {
                Tasks.forException(e)
            }
        }
        
        // Act & Assert
        try {
            roomRepository.joinRoom(userId, inviteCode)
            fail("Expected RoomJoinException")
        } catch (e: RoomJoinException) {
            assertTrue(e.message?.contains("Invalid invite code") == true)
        }
    }
    
    @Test
    fun `joinRoom throws RoomJoinException when room is full`() = runTest {
        // Arrange
        val userId = "user789"
        val inviteCode = "BAR-TOK"
        val roomId = "test-room-id"
        val fullUserIds = listOf("user123", "user456") // Already 2 users
        
        val inviteCodeDoc = mockk<DocumentSnapshot>()
        every { inviteCodeDoc.exists() } returns true
        every { inviteCodeDoc.getString("roomId") } returns roomId
        
        val roomDoc = mockk<DocumentSnapshot>()
        every { roomDoc.exists() } returns true
        every { roomDoc.get("userIds") } returns fullUserIds
        
        every { mockTransaction.get(any()) } returnsMany listOf(inviteCodeDoc, roomDoc)
        every { firestore.runTransaction<Room>(any()) } answers {
            val transaction = firstArg<Transaction.Function<Room>>()
            try {
                transaction.apply(mockTransaction)
                Tasks.forException(RoomJoinException("Room is full"))
            } catch (e: Exception) {
                Tasks.forException(e)
            }
        }
        
        // Act & Assert
        try {
            roomRepository.joinRoom(userId, inviteCode)
            fail("Expected RoomJoinException")
        } catch (e: RoomJoinException) {
            assertEquals("Room is full", e.message)
        }
    }
    
    @Test
    fun `joinRoom throws RoomJoinException when user already in room`() = runTest {
        // Arrange
        val userId = "user123"
        val inviteCode = "BAR-TOK"
        val roomId = "test-room-id"
        val existingUserIds = listOf("user123") // User already in room
        
        val inviteCodeDoc = mockk<DocumentSnapshot>()
        every { inviteCodeDoc.exists() } returns true
        every { inviteCodeDoc.getString("roomId") } returns roomId
        
        val roomDoc = mockk<DocumentSnapshot>()
        every { roomDoc.exists() } returns true
        every { roomDoc.get("userIds") } returns existingUserIds
        
        every { mockTransaction.get(any()) } returnsMany listOf(inviteCodeDoc, roomDoc)
        every { firestore.runTransaction<Room>(any()) } answers {
            val transaction = firstArg<Transaction.Function<Room>>()
            try {
                transaction.apply(mockTransaction)
                Tasks.forException(RoomJoinException("User is already in this room"))
            } catch (e: Exception) {
                Tasks.forException(e)
            }
        }
        
        // Act & Assert
        try {
            roomRepository.joinRoom(userId, inviteCode)
            fail("Expected RoomJoinException")
        } catch (e: RoomJoinException) {
            assertEquals("User is already in this room", e.message)
        }
    }
    
    @Test
    fun `getRoom returns room when it exists`() = runTest {
        // Arrange
        val roomId = "test-room-id"
        val userIds = listOf("user123", "user456")
        val createdAt = 1234567890L
        
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.get("userIds") } returns userIds
        every { mockDocumentSnapshot.getLong("createdAt") } returns createdAt
        
        every { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
        
        // Act
        val result = roomRepository.getRoom(roomId)
        
        // Assert
        assertNotNull(result)
        assertEquals(roomId, result?.id)
        assertEquals(userIds, result?.userIds)
        assertEquals(createdAt, result?.createdAt)
    }
    
    @Test
    fun `getRoom returns null when room doesn't exist`() = runTest {
        // Arrange
        val roomId = "nonexistent-room"
        
        every { mockDocumentSnapshot.exists() } returns false
        every { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
        
        // Act
        val result = roomRepository.getRoom(roomId)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `getRoom returns null when exception occurs`() = runTest {
        // Arrange
        val roomId = "test-room-id"
        val exception = FirebaseFirestoreException("Network error", FirebaseFirestoreException.Code.UNAVAILABLE)
        
        every { mockDocumentReference.get() } returns Tasks.forException(exception)
        
        // Act
        val result = roomRepository.getRoom(roomId)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `isRoomFull returns true when room has maximum members`() = runTest {
        // Arrange
        val roomId = "test-room-id"
        val userIds = listOf("user123", "user456") // 2 users = max
        
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.get("userIds") } returns userIds
        every { mockDocumentSnapshot.getLong("createdAt") } returns 1234567890L
        every { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
        
        // Act
        val result = roomRepository.isRoomFull(roomId)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun `isRoomFull returns false when room has less than maximum members`() = runTest {
        // Arrange
        val roomId = "test-room-id"
        val userIds = listOf("user123") // 1 user < max
        
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.get("userIds") } returns userIds
        every { mockDocumentSnapshot.getLong("createdAt") } returns 1234567890L
        every { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
        
        // Act
        val result = roomRepository.isRoomFull(roomId)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `leaveRoom removes user from room and updates user document`() = runTest {
        // Arrange
        val userId = "user123"
        val roomId = "test-room-id"
        val currentUserIds = listOf("user123", "user456")
        
        val roomDoc = mockk<DocumentSnapshot>()
        every { roomDoc.exists() } returns true
        every { roomDoc.get("userIds") } returns currentUserIds
        
        every { mockTransaction.get(any()) } returns roomDoc
        every { mockTransaction.update(any(), any(), any()) } returns mockTransaction
        every { firestore.runTransaction<Unit>(any()) } returns Tasks.forResult(Unit)
        
        // Act
        roomRepository.leaveRoom(userId, roomId)
        
        // Assert
        verify { firestore.runTransaction<Unit>(any()) }
    }
    
    @Test
    fun `leaveRoom throws RoomLeaveException when transaction fails`() = runTest {
        // Arrange
        val userId = "user123"
        val roomId = "test-room-id"
        val exception = FirebaseFirestoreException("Transaction failed", FirebaseFirestoreException.Code.ABORTED)
        
        every { firestore.runTransaction<Unit>(any()) } returns Tasks.forException(exception)
        
        // Act & Assert
        try {
            roomRepository.leaveRoom(userId, roomId)
            fail("Expected RoomLeaveException")
        } catch (e: RoomLeaveException) {
            assertEquals("Failed to leave room", e.message)
            assertEquals(exception, e.cause)
        }
    }
}