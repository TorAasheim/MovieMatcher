package com.moviematcher.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.moviematcher.app.data.model.Swipe
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.data.offline.ConnectionManager
import com.moviematcher.app.data.offline.OfflineSwipeQueue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SwipeRepository using Firebase Firestore with offline support
 */
@Singleton
class SwipeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val matchRepository: MatchRepository,
    private val connectionManager: ConnectionManager,
    private val offlineSwipeQueue: OfflineSwipeQueue
) : SwipeRepository {

    companion object {
        private const val ROOMS_COLLECTION = "rooms"
        private const val SWIPES_COLLECTION = "swipes"
        private const val FIELD_TITLE_ID = "titleId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_DECISION = "decision"
        private const val FIELD_TIMESTAMP = "timestamp"
    }

    override suspend fun recordSwipe(roomId: String, swipe: Swipe) {
        // Check if we're online
        val isConnected = connectionManager.isConnected.first()
        
        if (isConnected) {
            try {
                // Try to record swipe online
                recordSwipeOnline(roomId, swipe)
            } catch (e: Exception) {
                // If online recording fails, queue for offline sync
                offlineSwipeQueue.queueSwipe(roomId, swipe)
                throw e
            }
        } else {
            // Queue swipe for offline sync
            offlineSwipeQueue.queueSwipe(roomId, swipe)
        }
    }
    
    /**
     * Record swipe directly to Firestore (online operation)
     */
    private suspend fun recordSwipeOnline(roomId: String, swipe: Swipe) {
        val swipeData = mapOf(
            FIELD_TITLE_ID to swipe.titleId,
            FIELD_USER_ID to swipe.userId,
            FIELD_DECISION to swipe.decision.name,
            FIELD_TIMESTAMP to swipe.timestamp
        )

        // Use composite key: titleId:userId to ensure uniqueness
        val documentId = "${swipe.titleId}:${swipe.userId}"
        
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(SWIPES_COLLECTION)
            .document(documentId)
            .set(swipeData)
            .await()
        
        // Check for potential match if this was a LIKE
        if (swipe.decision == SwipeDecision.LIKE) {
            try {
                matchRepository.createMatch(roomId, swipe.titleId)
            } catch (e: Exception) {
                // Log error but don't fail the swipe operation
                // In a real app, you'd use proper logging
                println("Failed to check for match: ${e.message}")
            }
        }
    }

    override fun observePartnerSwipes(roomId: String, partnerId: String): Flow<Swipe> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(SWIPES_COLLECTION)
            .whereEqualTo(FIELD_USER_ID, partnerId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val document = change.document
                        try {
                            val swipe = Swipe(
                                titleId = document.getLong(FIELD_TITLE_ID) ?: 0L,
                                userId = document.getString(FIELD_USER_ID) ?: "",
                                decision = SwipeDecision.valueOf(
                                    document.getString(FIELD_DECISION) ?: SwipeDecision.PASS.name
                                ),
                                timestamp = document.getLong(FIELD_TIMESTAMP) ?: 0L
                            )
                            trySend(swipe)
                        } catch (e: Exception) {
                            // Log error but don't close the flow
                            // In a real app, you'd use proper logging
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun undoLastSwipe(roomId: String, userId: String) {
        // Get the most recent swipe for this user
        val querySnapshot = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(SWIPES_COLLECTION)
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        // Delete the most recent swipe if it exists and handle match removal
        querySnapshot.documents.firstOrNull()?.let { document ->
            val titleId = document.getLong(FIELD_TITLE_ID) ?: return
            val decision = document.getString(FIELD_DECISION)
            
            // Delete the swipe
            document.reference.delete().await()
            
            // If this was a LIKE swipe, check if we need to remove a match
            if (decision == SwipeDecision.LIKE.name) {
                try {
                    matchRepository.removeMatch(roomId, titleId)
                } catch (e: Exception) {
                    // Log error but don't fail the undo operation
                    // In a real app, you'd use proper logging
                    println("Failed to remove match during undo: ${e.message}")
                }
            }
        }
    }
}