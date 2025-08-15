package com.moviematcher.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.UserPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user preferences in Firestore
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val ROOMS_COLLECTION = "rooms"
        private const val PREFERENCES_DOCUMENT = "preferences"
        
        // Default preferences
        private val DEFAULT_PREFERENCES = UserPreferences(
            selectedGenres = emptySet(),
            yearRange = 1990..2024,
            minRating = 6.0,
            selectedProviders = emptySet(),
            availabilityStrict = false,
            contentType = ContentType.MOVIE
        )
    }
    
    /**
     * Get user preferences for a room
     * @param roomId The room ID
     * @return UserPreferences object or default if not found
     */
    suspend fun getPreferences(roomId: String): UserPreferences {
        return try {
            val document = firestore.collection(ROOMS_COLLECTION)
                .document(roomId)
                .collection("preferences")
                .document(PREFERENCES_DOCUMENT)
                .get()
                .await()
            
            if (document.exists()) {
                val data = document.data ?: return DEFAULT_PREFERENCES
                
                UserPreferences(
                    selectedGenres = (data["selectedGenres"] as? List<*>)
                        ?.mapNotNull { (it as? Number)?.toInt() }
                        ?.toSet() ?: emptySet(),
                    yearRange = run {
                        val yearRangeMap = data["yearRange"] as? Map<*, *>
                        val min = (yearRangeMap?.get("min") as? Number)?.toInt() ?: 1990
                        val max = (yearRangeMap?.get("max") as? Number)?.toInt() ?: 2024
                        min..max
                    },
                    minRating = (data["minRating"] as? Number)?.toDouble() ?: 6.0,
                    selectedProviders = (data["selectedProviders"] as? List<*>)
                        ?.mapNotNull { (it as? Number)?.toInt() }
                        ?.toSet() ?: emptySet(),
                    availabilityStrict = data["availabilityStrict"] as? Boolean ?: false,
                    contentType = try {
                        ContentType.valueOf(data["contentType"] as? String ?: "MOVIE")
                    } catch (e: IllegalArgumentException) {
                        ContentType.MOVIE
                    }
                )
            } else {
                DEFAULT_PREFERENCES
            }
        } catch (e: Exception) {
            DEFAULT_PREFERENCES
        }
    }
    
    /**
     * Observe user preferences for a room
     * @param roomId The room ID
     * @return Flow of UserPreferences
     */
    fun observePreferences(roomId: String): Flow<UserPreferences> {
        return callbackFlow {
            val listener = firestore.collection(ROOMS_COLLECTION)
                .document(roomId)
                .collection("preferences")
                .document(PREFERENCES_DOCUMENT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val preferences = if (snapshot?.exists() == true) {
                        val data = snapshot.data ?: run {
                            trySend(DEFAULT_PREFERENCES)
                            return@addSnapshotListener
                        }
                        
                        UserPreferences(
                            selectedGenres = (data["selectedGenres"] as? List<*>)
                                ?.mapNotNull { (it as? Number)?.toInt() }
                                ?.toSet() ?: emptySet(),
                            yearRange = run {
                                val yearRangeMap = data["yearRange"] as? Map<*, *>
                                val min = (yearRangeMap?.get("min") as? Number)?.toInt() ?: 1990
                                val max = (yearRangeMap?.get("max") as? Number)?.toInt() ?: 2024
                                min..max
                            },
                            minRating = (data["minRating"] as? Number)?.toDouble() ?: 6.0,
                            selectedProviders = (data["selectedProviders"] as? List<*>)
                                ?.mapNotNull { (it as? Number)?.toInt() }
                                ?.toSet() ?: emptySet(),
                            availabilityStrict = data["availabilityStrict"] as? Boolean ?: false,
                            contentType = try {
                                ContentType.valueOf(data["contentType"] as? String ?: "MOVIE")
                            } catch (e: IllegalArgumentException) {
                                ContentType.MOVIE
                            }
                        )
                    } else {
                        DEFAULT_PREFERENCES
                    }
                    
                    trySend(preferences)
                }
            
            awaitClose { listener.remove() }
        }
    }
    
    /**
     * Update user preferences for a room
     * @param roomId The room ID
     * @param preferences The new preferences
     */
    suspend fun updatePreferences(roomId: String, preferences: UserPreferences) {
        val preferencesData = mapOf(
            "selectedGenres" to preferences.selectedGenres.toList(),
            "yearRange" to mapOf(
                "min" to preferences.yearRange.first,
                "max" to preferences.yearRange.last
            ),
            "minRating" to preferences.minRating,
            "selectedProviders" to preferences.selectedProviders.toList(),
            "availabilityStrict" to preferences.availabilityStrict,
            "contentType" to preferences.contentType.name
        )
        
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .set(preferencesData)
            .await()
    }
    
    /**
     * Update selected genres
     * @param roomId The room ID
     * @param selectedGenres Set of genre IDs
     */
    suspend fun updateSelectedGenres(roomId: String, selectedGenres: Set<Int>) {
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("selectedGenres", selectedGenres.toList())
            .await()
    }
    
    /**
     * Update year range
     * @param roomId The room ID
     * @param yearRange The year range
     */
    suspend fun updateYearRange(roomId: String, yearRange: IntRange) {
        val yearRangeMap = mapOf(
            "min" to yearRange.first,
            "max" to yearRange.last
        )
        
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("yearRange", yearRangeMap)
            .await()
    }
    
    /**
     * Update minimum rating
     * @param roomId The room ID
     * @param minRating The minimum rating
     */
    suspend fun updateMinRating(roomId: String, minRating: Double) {
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("minRating", minRating)
            .await()
    }
    
    /**
     * Update selected streaming providers
     * @param roomId The room ID
     * @param selectedProviders Set of provider IDs
     */
    suspend fun updateSelectedProviders(roomId: String, selectedProviders: Set<Int>) {
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("selectedProviders", selectedProviders.toList())
            .await()
    }
    
    /**
     * Update availability strict mode
     * @param roomId The room ID
     * @param availabilityStrict Whether to use strict availability filtering
     */
    suspend fun updateAvailabilityStrict(roomId: String, availabilityStrict: Boolean) {
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("availabilityStrict", availabilityStrict)
            .await()
    }
    
    /**
     * Update content type
     * @param roomId The room ID
     * @param contentType The content type
     */
    suspend fun updateContentType(roomId: String, contentType: ContentType) {
        firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection("preferences")
            .document(PREFERENCES_DOCUMENT)
            .update("contentType", contentType.name)
            .await()
    }
}