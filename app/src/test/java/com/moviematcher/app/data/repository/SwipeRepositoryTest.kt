package com.moviematcher.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SwipeRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var swipeRepository: SwipeRepositoryImpl
    private lateinit var roomsCollection: CollectionReference
    private lateinit var roomDocument: DocumentReference
    private lateinit var swipesCollection: CollectionReference
    private lateinit var swipeDocument: DocumentReference

    @Before
    fun setup() {
        firestore = mockk()
        roomsCollection = mockk()
        roomDocument = mockk()
        swipesCollection = mockk()
        swipeDocument = mockk()

        every { firestore.collection("rooms") } returns roomsCollection
        every { roomsCollection.document(any()) } returns roomDocument
        every { roomDocument.collection("swipes") } returns swipesCollection
        every { swipesCollection.document(any()) } returns swipeDocument

        swipeRepository = SwipeRepositoryImpl(firestore)
    }

    @Test
    fun `recordSwipe should save swipe data to Firestore with correct document ID`() = runTest {
        // Arrange
        val roomId = "room123"
        val swipe = Swipe(
            titleId = 12345L,
            userId = "user123",
            decision = SwipeDecision.LIKE,
            timestamp = 1234567890L
        )
        val task: Task<Void> = mockk(relaxed = true)
        every { swipeDocument.set(any()) } returns task

        val expectedDocumentId = "12345:user123"
        val dataSlot = slot<Map<String, Any>>()

        // Act
        swipeRepository.recordSwipe(roomId, swipe)

        // Assert
        verify { roomsCollection.document(roomId) }
        verify { roomDocument.collection("swipes") }
        verify { swipesCollection.document(expectedDocumentId) }
        verify { swipeDocument.set(capture(dataSlot)) }

        val capturedData = dataSlot.captured
        assertEquals(12345L, capturedData["titleId"])
        assertEquals("user123", capturedData["userId"])
        assertEquals("LIKE", capturedData["decision"])
        assertEquals(1234567890L, capturedData["timestamp"])
    }

    @Test
    fun `recordSwipe should handle PASS decision correctly`() = runTest {
        // Arrange
        val roomId = "room123"
        val swipe = Swipe(
            titleId = 67890L,
            userId = "user456",
            decision = SwipeDecision.PASS,
            timestamp = 9876543210L
        )
        val task: Task<Void> = mockk(relaxed = true)
        every { swipeDocument.set(any()) } returns task

        val dataSlot = slot<Map<String, Any>>()

        // Act
        swipeRepository.recordSwipe(roomId, swipe)

        // Assert
        verify { swipeDocument.set(capture(dataSlot)) }
        val capturedData = dataSlot.captured
        assertEquals("PASS", capturedData["decision"])
    }

    @Test
    fun `undoLastSwipe should delete most recent swipe for user`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        
        val query: Query = mockk()
        val querySnapshot: QuerySnapshot = mockk()
        val documentSnapshot: DocumentSnapshot = mockk()
        val documentReference: DocumentReference = mockk()
        val task: Task<QuerySnapshot> = mockk(relaxed = true)
        val deleteTask: Task<Void> = mockk(relaxed = true)

        every { swipesCollection.whereEqualTo("userId", userId) } returns query
        every { query.orderBy("timestamp", Query.Direction.DESCENDING) } returns query
        every { query.limit(1) } returns query
        every { query.get() } returns task
        every { querySnapshot.documents } returns listOf(documentSnapshot)
        every { documentSnapshot.reference } returns documentReference
        every { documentReference.delete() } returns deleteTask

        // Act
        swipeRepository.undoLastSwipe(roomId, userId)

        // Assert
        verify { swipesCollection.whereEqualTo("userId", userId) }
        verify { query.orderBy("timestamp", Query.Direction.DESCENDING) }
        verify { query.limit(1) }
        verify { documentReference.delete() }
    }

    @Test
    fun `undoLastSwipe should handle empty query result gracefully`() = runTest {
        // Arrange
        val roomId = "room123"
        val userId = "user123"
        
        val query: Query = mockk()
        val querySnapshot: QuerySnapshot = mockk()
        val task: Task<QuerySnapshot> = mockk(relaxed = true)

        every { swipesCollection.whereEqualTo("userId", userId) } returns query
        every { query.orderBy("timestamp", Query.Direction.DESCENDING) } returns query
        every { query.limit(1) } returns query
        every { query.get() } returns task
        every { querySnapshot.documents } returns emptyList()

        // Act
        swipeRepository.undoLastSwipe(roomId, userId)

        // Assert - should not throw exception
        verify { swipesCollection.whereEqualTo("userId", userId) }
        verify { query.orderBy("timestamp", Query.Direction.DESCENDING) }
        verify { query.limit(1) }
    }

    @Test
    fun `observePartnerSwipes should set up Firestore listener with correct query`() {
        // Arrange
        val roomId = "room123"
        val partnerId = "partner456"
        val query: Query = mockk()
        val listenerRegistration: ListenerRegistration = mockk()

        every { swipesCollection.whereEqualTo("userId", partnerId) } returns query
        every { query.orderBy("timestamp", Query.Direction.DESCENDING) } returns query
        every { query.addSnapshotListener(any()) } returns listenerRegistration

        // Act
        val flow = swipeRepository.observePartnerSwipes(roomId, partnerId)

        // Assert
        verify { roomsCollection.document(roomId) }
        verify { roomDocument.collection("swipes") }
        verify { swipesCollection.whereEqualTo("userId", partnerId) }
        verify { query.orderBy("timestamp", Query.Direction.DESCENDING) }
        verify { query.addSnapshotListener(any()) }
    }
}