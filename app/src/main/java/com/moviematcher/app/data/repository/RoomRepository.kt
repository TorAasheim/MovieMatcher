package com.moviematcher.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import com.moviematcher.app.data.model.Room
import com.moviematcher.app.data.util.InviteCodeGenerator
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing room creation, joining, and membership
 */
@Singleton
class RoomRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val ROOMS_COLLECTION = "rooms"
        private const val USERS_COLLECTION = "users"
        private const val INVITE_CODES_COLLECTION = "inviteCodes"
        private const val MAX_ROOM_MEMBERS = 2
        private const val MAX_CODE_GENERATION_ATTEMPTS = 10
    }
    
    /**
     * Creates a new room with the given user as the first member
     * @param userId The ID of the user creating the room
     * @return Pair of Room and invite code
     * @throws RoomCreationException if room creation fails
     */
    suspend fun createRoom(userId: String): Pair<Room, String> {
        return try {
            firestore.runTransaction { transaction ->
                val inviteCode = generateUniqueInviteCode(transaction)
                val roomId = firestore.collection(ROOMS_COLLECTION).document().id
                
                val room = Room(
                    id = roomId,
                    userIds = listOf(userId),
                    createdAt = System.currentTimeMillis()
                )
                
                // Create room document
                transaction.set(
                    firestore.collection(ROOMS_COLLECTION).document(roomId),
                    mapOf(
                        "userIds" to room.userIds,
                        "createdAt" to room.createdAt
                    )
                )
                
                // Create invite code mapping
                transaction.set(
                    firestore.collection(INVITE_CODES_COLLECTION).document(inviteCode),
                    mapOf(
                        "roomId" to roomId,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                
                // Update user's roomId
                transaction.update(
                    firestore.collection(USERS_COLLECTION).document(userId),
                    "roomId", roomId
                )
                
                Pair(room, inviteCode)
            }.await()
        } catch (e: Exception) {
            throw RoomCreationException("Failed to create room", e)
        }
    }
    
    /**
     * Joins an existing room using an invite code
     * @param userId The ID of the user joining the room
     * @param inviteCode The invite code for the room
     * @return The Room object if successfully joined
     * @throws RoomJoinException if joining fails
     */
    suspend fun joinRoom(userId: String, inviteCode: String): Room {
        if (!InviteCodeGenerator.isValidFormat(inviteCode)) {
            throw RoomJoinException("Invalid invite code format")
        }
        
        return try {
            firestore.runTransaction { transaction ->
                // Get room ID from invite code
                val inviteCodeDoc = transaction.get(
                    firestore.collection(INVITE_CODES_COLLECTION).document(inviteCode)
                )
                
                if (!inviteCodeDoc.exists()) {
                    throw RoomJoinException("Invalid invite code")
                }
                
                val roomId = inviteCodeDoc.getString("roomId")
                    ?: throw RoomJoinException("Invalid invite code data")
                
                // Get current room state
                val roomDoc = transaction.get(
                    firestore.collection(ROOMS_COLLECTION).document(roomId)
                )
                
                if (!roomDoc.exists()) {
                    throw RoomJoinException("Room no longer exists")
                }
                
                val currentUserIds = roomDoc.get("userIds") as? List<String>
                    ?: throw RoomJoinException("Invalid room data")
                
                // Validate room membership
                if (currentUserIds.contains(userId)) {
                    throw RoomJoinException("User is already in this room")
                }
                
                if (currentUserIds.size >= MAX_ROOM_MEMBERS) {
                    throw RoomJoinException("Room is full")
                }
                
                val updatedUserIds = currentUserIds + userId
                val createdAt = roomDoc.getLong("createdAt") ?: System.currentTimeMillis()
                
                // Update room with new member
                transaction.update(
                    firestore.collection(ROOMS_COLLECTION).document(roomId),
                    "userIds", updatedUserIds
                )
                
                // Update user's roomId
                transaction.update(
                    firestore.collection(USERS_COLLECTION).document(userId),
                    "roomId", roomId
                )
                
                Room(
                    id = roomId,
                    userIds = updatedUserIds,
                    createdAt = createdAt
                )
            }.await()
        } catch (e: RoomJoinException) {
            throw e
        } catch (e: Exception) {
            throw RoomJoinException("Failed to join room", e)
        }
    }
    
    /**
     * Gets room information by room ID
     * @param roomId The ID of the room to retrieve
     * @return Room object or null if not found
     */
    suspend fun getRoom(roomId: String): Room? {
        return try {
            val document = firestore.collection(ROOMS_COLLECTION)
                .document(roomId)
                .get()
                .await()
            
            if (!document.exists()) return null
            
            val userIds = document.get("userIds") as? List<String> ?: return null
            val createdAt = document.getLong("createdAt") ?: return null
            
            Room(
                id = roomId,
                userIds = userIds,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Leaves a room and updates user's roomId to null
     * @param userId The ID of the user leaving the room
     * @param roomId The ID of the room to leave
     */
    suspend fun leaveRoom(userId: String, roomId: String) {
        try {
            firestore.runTransaction { transaction ->
                val roomDoc = transaction.get(
                    firestore.collection(ROOMS_COLLECTION).document(roomId)
                )
                
                if (roomDoc.exists()) {
                    val currentUserIds = roomDoc.get("userIds") as? List<String> ?: emptyList()
                    val updatedUserIds = currentUserIds.filter { it != userId }
                    
                    if (updatedUserIds.isEmpty()) {
                        // Delete room if no users left
                        transaction.delete(
                            firestore.collection(ROOMS_COLLECTION).document(roomId)
                        )
                    } else {
                        // Update room with remaining users
                        transaction.update(
                            firestore.collection(ROOMS_COLLECTION).document(roomId),
                            "userIds", updatedUserIds
                        )
                    }
                }
                
                // Update user's roomId to null
                transaction.update(
                    firestore.collection(USERS_COLLECTION).document(userId),
                    "roomId", null
                )
            }.await()
        } catch (e: Exception) {
            throw RoomLeaveException("Failed to leave room", e)
        }
    }
    
    /**
     * Validates that a room has exactly the maximum number of members
     */
    suspend fun isRoomFull(roomId: String): Boolean {
        val room = getRoom(roomId)
        return room?.userIds?.size == MAX_ROOM_MEMBERS
    }
    
    /**
     * Generates a unique invite code that doesn't already exist
     */
    private suspend fun generateUniqueInviteCode(transaction: Transaction): String {
        repeat(MAX_CODE_GENERATION_ATTEMPTS) {
            val code = InviteCodeGenerator.generateCode()
            val existingDoc = transaction.get(
                firestore.collection(INVITE_CODES_COLLECTION).document(code)
            )
            
            if (!existingDoc.exists()) {
                return code
            }
        }
        throw RoomCreationException("Failed to generate unique invite code after $MAX_CODE_GENERATION_ATTEMPTS attempts")
    }
}

/**
 * Exception thrown when room creation fails
 */
class RoomCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when joining a room fails
 */
class RoomJoinException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when leaving a room fails
 */
class RoomLeaveException(message: String, cause: Throwable? = null) : Exception(message, cause)