# Design Document

## Overview

The Movie Matcher app is an Android application built with Kotlin and Jetpack Compose that provides a Tinder-style interface for couples to discover movies they both want to watch. The app uses Firebase for authentication, real-time data synchronization, and push notifications, while integrating with The Movie Database (TMDB) API for movie data and streaming provider information.

## Architecture

### High-Level Architecture

The app follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │     Domain      │    │      Data       │
│                 │    │                 │    │                 │
│ • UI Components │◄──►│ • Use Cases     │◄──►│ • Repositories  │
│ • ViewModels    │    │ • Models        │    │ • Data Sources  │
│ • Navigation    │    │ • Interfaces    │    │ • API Clients   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Technology Stack

- **Frontend**: Kotlin, Jetpack Compose, Material 3
- **Backend Services**: Firebase (Auth, Firestore, Cloud Messaging, Functions)
- **External APIs**: TMDB API v3
- **Image Loading**: Coil
- **Networking**: Retrofit + Moshi
- **Async**: Kotlin Coroutines + Flow## Com
ponents and Interfaces

### Core Components

#### 1. Authentication Module
- **GoogleAuthManager**: Handles Google Sign-In integration
- **UserRepository**: Manages user profile data and FCM tokens
- **AuthViewModel**: Coordinates authentication flow

#### 2. Room Management Module
- **RoomRepository**: Handles room creation, joining, and membership
- **PairingViewModel**: Manages room creation and invite code sharing
- **InviteCodeGenerator**: Creates unique, user-friendly room codes

#### 3. Movie Discovery Module
- **TmdbRepository**: Interfaces with TMDB API for movie data
- **MovieRecommendationEngine**: Applies filters and manages recommendation queue
- **ProviderRepository**: Handles streaming provider data and deep links

#### 4. Swiping Module
- **SwipeRepository**: Manages swipe decisions and real-time synchronization
- **SwipeViewModel**: Coordinates swiping logic and UI state
- **SwipeCard**: Composable component for movie cards with gesture handling

#### 5. Matching Module
- **MatchRepository**: Handles match creation and management
- **MatchViewModel**: Manages match list and suggestions
- **NotificationService**: Handles FCM push notifications

#### 6. Preferences Module
- **PreferencesRepository**: Manages user filtering preferences
- **SettingsViewModel**: Handles preference updates and validation

### Key Interfaces

```kotlin
interface MovieRepository {
    suspend fun getTrendingMovies(page: Int): List<Movie>
    suspend fun searchMovies(query: String, page: Int): List<Movie>
    suspend fun getMovieDetails(id: Long): MovieDetails
    suspend fun getStreamingProviders(movieId: Long): List<StreamingProvider>
}

interface SwipeRepository {
    suspend fun recordSwipe(roomId: String, swipe: Swipe)
    fun observePartnerSwipes(roomId: String, partnerId: String): Flow<Swipe>
    suspend fun undoLastSwipe(roomId: String, userId: String)
}

interface MatchRepository {
    suspend fun createMatch(roomId: String, movieId: Long)
    fun observeMatches(roomId: String): Flow<List<Match>>
    suspend fun markAsWatched(roomId: String, movieId: Long, notes: String)
}
```## Data 
Models

### Core Data Models

```kotlin
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val fcmToken: String?,
    val roomId: String?,
    val createdAt: Long
)

data class Room(
    val id: String,
    val userIds: List<String>,
    val createdAt: Long
)

data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val genres: List<Genre>,
    val runtime: Int?
)

data class Swipe(
    val titleId: Long,
    val userId: String,
    val decision: SwipeDecision,
    val timestamp: Long
)

enum class SwipeDecision { LIKE, PASS }

data class Match(
    val titleId: Long,
    val timestamp: Long,
    val watched: Boolean = false,
    val notes: String = "",
    val movieDetails: Movie? = null,
    val streamingProviders: List<StreamingProvider> = emptyList()
)

data class StreamingProvider(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val deepLinkUrl: String?
)

data class UserPreferences(
    val selectedGenres: Set<Int>,
    val yearRange: IntRange,
    val minRating: Double,
    val selectedProviders: Set<Int>,
    val availabilityStrict: Boolean,
    val contentType: ContentType
)

enum class ContentType { MOVIE, TV, BOTH }
```

### Firestore Data Structure

```
users/{userId}
├── displayName: String
├── photoUrl: String?
├── fcmToken: String?
├── roomId: String?
└── createdAt: Timestamp

rooms/{roomId}
├── userIds: Array[String]
├── createdAt: Timestamp
├── preferences/
│   ├── selectedGenres: Array[Number]
│   ├── yearRange: Map{min: Number, max: Number}
│   ├── minRating: Number
│   ├── selectedProviders: Array[Number]
│   ├── availabilityStrict: Boolean
│   └── contentType: String
├── swipes/{titleId}:{userId}
│   ├── titleId: Number
│   ├── userId: String
│   ├── decision: String
│   └── timestamp: Timestamp
└── matches/{titleId}
    ├── titleId: Number
    ├── timestamp: Timestamp
    ├── watched: Boolean
    └── notes: String
```## 
Error Handling

### Error Categories and Strategies

#### 1. Network Errors
- **TMDB API failures**: Implement retry logic with exponential backoff
- **Firestore connection issues**: Use offline persistence and queue operations
- **FCM delivery failures**: Graceful degradation with in-app notifications

#### 2. Authentication Errors
- **Google Sign-In failures**: Clear error messages and retry options
- **Token expiration**: Automatic token refresh with fallback to re-authentication
- **Account linking issues**: Guided resolution flow

#### 3. Data Consistency Errors
- **Concurrent swipe conflicts**: Use Firestore transactions for match creation
- **Room membership violations**: Server-side validation in security rules
- **Stale data issues**: Implement proper cache invalidation strategies

#### 4. User Experience Errors
- **Empty recommendation queue**: Fallback to popular movies and clear messaging
- **No matches found**: Suggest preference adjustments
- **Provider unavailability**: Show alternative providers or web fallbacks

### Error Recovery Mechanisms

```kotlin
sealed class AppError {
    object NetworkUnavailable : AppError()
    object AuthenticationFailed : AppError()
    data class ApiError(val code: Int, val message: String) : AppError()
    data class DataError(val cause: Throwable) : AppError()
}

class ErrorHandler {
    fun handleError(error: AppError): ErrorAction {
        return when (error) {
            is AppError.NetworkUnavailable -> ErrorAction.ShowRetry
            is AppError.AuthenticationFailed -> ErrorAction.RequireReauth
            is AppError.ApiError -> when (error.code) {
                429 -> ErrorAction.BackoffRetry
                404 -> ErrorAction.ShowNotFound
                else -> ErrorAction.ShowGenericError
            }
            is AppError.DataError -> ErrorAction.LogAndContinue
        }
    }
}
```## Testin
g Strategy

### Unit Testing
- **Repository Layer**: Mock external dependencies (Firebase, TMDB API)
- **Use Cases**: Test business logic with fake repositories
- **ViewModels**: Test state management and user interactions
- **Utilities**: Test recommendation algorithms and filtering logic

### Integration Testing
- **Firebase Integration**: Test real-time synchronization between devices
- **TMDB API Integration**: Verify data parsing and error handling
- **Authentication Flow**: End-to-end Google Sign-In testing

### UI Testing
- **Swipe Gestures**: Verify card animations and decision recording
- **Navigation**: Test screen transitions and deep linking
- **Real-time Updates**: Verify match notifications and UI updates

### End-to-End Testing
- **Two-Device Scenarios**: Test complete matching flow between paired devices
- **Offline Scenarios**: Verify queue persistence and sync on reconnection
- **Push Notifications**: Test FCM delivery and handling

### Performance Testing
- **Image Loading**: Test poster loading performance and caching
- **Database Queries**: Verify Firestore query efficiency
- **Memory Usage**: Monitor for leaks during extended swiping sessions

### Test Data Management
- **Mock TMDB Responses**: Consistent test data for movie information
- **Firebase Emulator**: Local testing environment for Firestore operations
- **Test User Accounts**: Dedicated Google accounts for authentication testing

## Security Considerations

### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Room access limited to members
    match /rooms/{roomId} {
      allow read, write: if request.auth != null && 
        request.auth.uid in resource.data.userIds;
      
      // Swipes and matches inherit room permissions
      match /{collection}/{document} {
        allow read, write: if request.auth != null && 
          request.auth.uid in get(/databases/$(database)/documents/rooms/$(roomId)).data.userIds;
      }
    }
  }
}
```

### Data Privacy
- **Minimal Data Collection**: Only store necessary user information
- **Automatic Cleanup**: 90-day retention policy for swipes and matches
- **Secure Transmission**: All API calls use HTTPS
- **Token Management**: Secure FCM token storage and rotation

## Performance Optimizations

### Image Loading
- **Coil Configuration**: Disk and memory caching for movie posters
- **Lazy Loading**: Load images only when cards are visible
- **Size Optimization**: Request appropriate image sizes from TMDB

### Database Optimization
- **Firestore Indexing**: Optimize queries for swipes and matches
- **Offline Persistence**: Enable local caching for better performance
- **Batch Operations**: Group related writes to reduce latency

### UI Performance
- **Compose Optimization**: Minimize recomposition with stable keys
- **Animation Performance**: Use hardware acceleration for swipe gestures
- **Memory Management**: Proper lifecycle handling for ViewModels