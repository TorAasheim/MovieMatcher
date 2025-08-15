package com.moviematcher.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.moviematcher.app.data.model.Match
import com.moviematcher.app.data.model.SwipeDecision
import com.moviematcher.app.notification.NotificationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MatchRepository using Firebase Firestore
 */
@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService
) : MatchRepository {

    companion object {
        private const val ROOMS_COLLECTION = "rooms"
        private const val SWIPES_COLLECTION = "swipes"
        private const val MATCHES_COLLECTION = "matches"
        private const val FIELD_TITLE_ID = "titleId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_DECISION = "decision"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_WATCHED = "watched"
        private const val FIELD_NOTES = "notes"
    }

    override suspend fun createMatch(roomId: String, movieId: Long) {
        val roomRef = firestore.collection(ROOMS_COLLECTION).document(roomId)
        val matchRef = roomRef.collection(MATCHES_COLLECTION).document(movieId.toString())
        
        // Check if match already exists
        val existingMatch = matchRef.get().await()
        if (existingMatch.exists()) {
            return // Match already exists, no need to create
        }
        
        // Get all swipes for this movie in this room
        val swipeQuery = roomRef.collection(SWIPES_COLLECTION)
            .whereEqualTo(FIELD_TITLE_ID, movieId)
        val swipeSnapshot = swipeQuery.get().await()
        
        // Check if both users have liked this movie
        val likeSwipes = swipeSnapshot.documents.filter { document ->
            document.getString(FIELD_DECISION) == SwipeDecision.LIKE.name
        }
        
        if (likeSwipes.size >= 2) {
            // Both users liked the movie, create a match using transaction to prevent race conditions
            firestore.runTransaction { transaction ->
                // Double-check that match doesn't exist (race condition protection)
                val existingMatchCheck = transaction.get(matchRef)
                if (existingMatchCheck.exists()) {
                    return@runTransaction null
                }
                
                val matchData = mapOf(
                    FIELD_TITLE_ID to movieId,
                    FIELD_TIMESTAMP to System.currentTimeMillis(),
                    FIELD_WATCHED to false,
                    FIELD_NOTES to ""
                )
                
                transaction.set(matchRef, matchData)
                
                // Get room data to find user IDs for notifications
                val roomSnapshot = transaction.get(roomRef)
                val userIds = roomSnapshot.get("userIds") as? List<String> ?: emptyList()
                userIds
            }.await()?.let { userIds ->
                // Send notifications after successful transaction
                notificationService.sendMatchNotification(userIds as List<String>, movieId)
            }
        }
    }

    override fun observeMatches(roomId: String): Flow<List<Match>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(MATCHES_COLLECTION)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val matches = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Match(
                            titleId = document.getLong(FIELD_TITLE_ID) ?: 0L,
                            timestamp = document.getLong(FIELD_TIMESTAMP) ?: 0L,
                            watched = document.getBoolean(FIELD_WATCHED) ?: false,
                            notes = document.getString(FIELD_NOTES) ?: ""
                        )
                    } catch (e: Exception) {
                        null // Skip invalid documents
                    }
                } ?: emptyList()

                trySend(matches)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun markAsWatched(roomId: String, movieId: Long, notes: String) {
        val matchRef = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(MATCHES_COLLECTION)
            .document(movieId.toString())

        val updateData = mapOf(
            FIELD_WATCHED to true,
            FIELD_NOTES to notes
        )

        matchRef.update(updateData).await()
    }
}