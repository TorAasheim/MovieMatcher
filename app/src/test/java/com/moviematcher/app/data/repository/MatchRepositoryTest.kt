package com.moviematcher.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.moviematcher.app.data.model.Match
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.notification.NotificationService
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MatchRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var notificationService: NotificationService
    private lateinit var matchRepository: MatchRepositoryImpl
    
    private lateinit var roomRef: DocumentReference
    private lateinit var matchRef: DocumentReference
    private lateinit var swipesRef: CollectionReference
    private lateinit var matchesRef: CollectionReference

    @Before
    fun setup() {
        firestore = mockk()
        notificationService = mockk()
        matchRepository = MatchRepositoryImpl(firestore, notificationService)
        
        roomRef = mockk()
        matchRef = mockk()
        swipesRef = mockk()
        matchesRef = mockk()
        
        every { firestore.collection("rooms") } returns mockk {
            every { document(any()) } returns roomRef
        }
        
        every { roomRef.collection("matches") } returns matchesRef
        every { roomRef.collection("swipes") } returns swipesRef
        every { matchesRef.document(any()) } returns matchRef
    }

    @Test
    fun `createMatch should create match when both users like the same movie`() = runTest {
        // Arrange
        val roomId = "room123"
        val movieId = 456L
        val userIds = listOf("user1", "user2")
        
        val existingMatchSnapshot = mockk<DocumentSnapshot>()
        val swipeQuery = mockk<Query>()
        val swipeSnapshot = mockk<QuerySnapshot>()
        val roomSnapshot = mockk<DocumentSnapshot>()
        val transaction = mockk<Transaction>()
        
        val swipeDoc1 = mockk<DocumentSnapshot>()
        val swipeDoc2 = mockk<DocumentSnapshot>()
        
        val matchTask = mockk<Task<DocumentSnapshot>>()
        val swipeTask = mockk<Task<QuerySnapshot>>()
        val transactionTask = mockk<Task<List<String>>>()
        
        // Mock existing match check
        every { matchRef.get() } returns matchTask
        every { matchTask.isComplete } returns true
        every { matchTask.exception } returns null
        every { matchTask.isCanceled } returns false
        every { matchTask.result } returns existingMatchSnapshot
        every { existingMatchSnapshot.exists() } returns false
        
        // Mock swipe query
        every { swipesRef.whereEqualTo("titleId", movieId) } returns swipeQuery
        every { swipeQuery.get() } returns swipeTask
        every { swipeTask.isComplete } returns true
        every { swipeTask.exception } returns null
        every { swipeTask.isCanceled } returns false
        every { swipeTask.result } returns swipeSnapshot
        every { swipeSnapshot.documents } returns listOf(swipeDoc1, swipeDoc2)
        
        every { swipeDoc1.getString("decision") } returns SwipeDecision.LIKE.name
        every { swipeDoc2.getString("decision") } returns SwipeDecision.LIKE.name
        
        // Mock transaction
        every { firestore.runTransaction<List<String>>(any()) } returns transactionTask
        every { transactionTask.isComplete } returns true
        every { transactionTask.exception } returns null
        every { transactionTask.isCanceled } returns false
        every { transactionTask.result } returns userIds
        
        every { transaction.get(matchRef) } returns existingMatchSnapshot
        every { transaction.get(roomRef) } returns roomSnapshot
        every { roomSnapshot.get("userIds") } returns userIds
        every { transaction.set(matchRef, any()) } returns transaction
        
        coEvery { notificationService.sendMatchNotification(userIds, movieId) } just Runs
        
        // Act
        matchRepository.createMatch(roomId, movieId)
        
        // Assert
        coVerify { notificationService.sendMatchNotification(userIds, movieId) }
    }

    @Test
    fun `createMatch should not create match when only one user likes the movie`() = runTest {
        // Arrange
        val roomId = "room123"
        val movieId = 456L
        
        val existingMatchSnapshot = mockk<DocumentSnapshot>()
        val swipeQuery = mockk<Query>()
        val swipeSnapshot = mockk<QuerySnapshot>()
        
        val swipeDoc1 = mockk<DocumentSnapshot>()
        
        val matchTask = mockk<Task<DocumentSnapshot>>()
        val swipeTask = mockk<Task<QuerySnapshot>>()
        
        // Mock existing match check
        every { matchRef.get() } returns matchTask
        every { matchTask.isComplete } returns true
        every { matchTask.exception } returns null
        every { matchTask.isCanceled } returns false
        every { matchTask.result } returns existingMatchSnapshot
        every { existingMatchSnapshot.exists() } returns false
        
        // Mock swipe query
        every { swipesRef.whereEqualTo("titleId", movieId) } returns swipeQuery
        every { swipeQuery.get() } returns swipeTask
        every { swipeTask.isComplete } returns true
        every { swipeTask.exception } returns null
        every { swipeTask.isCanceled } returns false
        every { swipeTask.result } returns swipeSnapshot
        every { swipeSnapshot.documents } returns listOf(swipeDoc1)
        
        every { swipeDoc1.getString("decision") } returns SwipeDecision.LIKE.name
        
        // Act
        matchRepository.createMatch(roomId, movieId)
        
        // Assert
        coVerify(exactly = 0) { notificationService.sendMatchNotification(any(), any()) }
    }

    @Test
    fun `createMatch should not create duplicate match`() = runTest {
        // Arrange
        val roomId = "room123"
        val movieId = 456L
        
        val existingMatchSnapshot = mockk<DocumentSnapshot>()
        val matchTask = mockk<Task<DocumentSnapshot>>()
        
        // Mock existing match check
        every { matchRef.get() } returns matchTask
        every { matchTask.isComplete } returns true
        every { matchTask.exception } returns null
        every { matchTask.isCanceled } returns false
        every { matchTask.result } returns existingMatchSnapshot
        every { existingMatchSnapshot.exists() } returns true
        
        // Act
        matchRepository.createMatch(roomId, movieId)
        
        // Assert
        coVerify(exactly = 0) { notificationService.sendMatchNotification(any(), any()) }
    }

    @Test
    fun `markAsWatched should update match with watched status and notes`() = runTest {
        // Arrange
        val roomId = "room123"
        val movieId = 456L
        val notes = "Great movie!"
        
        val updateTask = mockk<Task<Void>>()
        every { matchRef.update(any<Map<String, Any>>()) } returns updateTask
        every { updateTask.isComplete } returns true
        every { updateTask.exception } returns null
        every { updateTask.isCanceled } returns false
        every { updateTask.result } returns mockk()
        
        // Act
        matchRepository.markAsWatched(roomId, movieId, notes)
        
        // Assert
        verify { 
            matchRef.update(match<Map<String, Any>> { updateData ->
                updateData["watched"] == true && updateData["notes"] == notes
            })
        }
    }

    @Test
    fun `removeMatch should delete match document`() = runTest {
        // Arrange
        val roomId = "room123"
        val movieId = 456L
        
        val deleteTask = mockk<Task<Void>>()
        every { matchRef.delete() } returns deleteTask
        every { deleteTask.isComplete } returns true
        every { deleteTask.exception } returns null
        every { deleteTask.isCanceled } returns false
        every { deleteTask.result } returns mockk()
        
        // Act
        matchRepository.removeMatch(roomId, movieId)
        
        // Assert
        verify { matchRef.delete() }
    }

    @Test
    fun `observeMatches should return flow of matches`() = runTest {
        // Arrange
        val roomId = "room123"
        val query = mockk<Query>()
        val listenerRegistration = mockk<ListenerRegistration>()
        
        every { matchesRef.orderBy("timestamp", Query.Direction.DESCENDING) } returns query
        
        val snapshot = mockk<QuerySnapshot>()
        val document = mockk<DocumentSnapshot>()
        
        every { document.getLong("titleId") } returns 123L
        every { document.getLong("timestamp") } returns 1234567890L
        every { document.getBoolean("watched") } returns false
        every { document.getString("notes") } returns ""
        
        every { snapshot.documents } returns listOf(document)
        
        every { query.addSnapshotListener(any()) } answers {
            val listener = firstArg<EventListener<QuerySnapshot>>()
            listener.onEvent(snapshot, null)
            listenerRegistration
        }
        
        every { listenerRegistration.remove() } just Runs
        
        // Act
        val flow = matchRepository.observeMatches(roomId)
        val result = flow.first()
        
        // Assert
        assertEquals(1, result.size)
        assertEquals(123L, result[0].titleId)
        assertEquals(1234567890L, result[0].timestamp)
        assertEquals(false, result[0].watched)
        assertEquals("", result[0].notes)
    }
}