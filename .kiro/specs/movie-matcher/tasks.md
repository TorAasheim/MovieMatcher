# Implementation Plan

- [x] 1. Set up project structure and dependencies
  - Create Android project with Kotlin and Jetpack Compose
  - Configure build.gradle with Firebase BOM, Auth, Firestore, FCM dependencies
  - Add TMDB API, Retrofit, Moshi, Coil, and Navigation Compose dependencies
  - Set up BuildConfig for TMDB API key from local.properties
  - _Requirements: 10.1, 10.2_

- [x] 2. Implement core data models and interfaces
  - Create data classes for User, Room, Movie, Swipe, Match, and StreamingProvider
  - Define repository interfaces for MovieRepository, SwipeRepository, MatchRepository
  - Implement SwipeDecision enum and ContentType enum
  - Create UserPreferences data class with filtering options
  - _Requirements: 4.1, 4.2, 2.2, 2.3, 3.3_

- [x] 3. Set up Firebase configuration and authentication
  - Add google-services.json to project
  - Implement GoogleAuthManager for Google Sign-In integration
  - Create UserRepository with Firestore integration for user profiles
  - Implement FCM token registration and storage
  - Write unit tests for authentication flow
  - _Requirements: 10.1, 10.2, 10.3_

- [ ] 4. Implement TMDB API client and movie repository


  - Create TmdbApi interface with Retrofit annotations for trending, search, details, and providers endpoints
  - Implement TmdbRepository with movie data fetching and caching
  - Create DTOs for TMDB API responses with Moshi annotations
  - Add image URL helper utilities for poster loading
  - Write unit tests for API client and repository
  - _Requirements: 2.1, 6.1, 6.2_

- [ ] 5. Create room management and pairing system
  - Implement RoomRepository with Firestore integration for room creation and joining
  - Create InviteCodeGenerator for unique room codes
  - Implement PairingViewModel for room creation and joining flow
  - Add room membership validation (max 2 users)
  - Write unit tests for room management logic
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 6. Build swipe functionality and real-time synchronization
  - Implement SwipeRepository with Firestore real-time listeners
  - Create SwipeViewModel for managing swipe decisions and partner synchronization
  - Add swipe decision recording with timestamp
  - Implement real-time partner swipe observation using Firestore listeners
  - Write unit tests for swipe logic and synchronization
  - _Requirements: 2.2, 2.3, 3.1, 3.2_

- [ ] 7. Implement matching system with notifications
  - Create MatchRepository for match creation and management
  - Implement match detection logic when both users like the same movie
  - Add Firestore transaction-based match creation to prevent duplicates
  - Create NotificationService for FCM push notification handling
  - Write unit tests for matching logic
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 8. Create swipe UI with Tinder-style cards
  - Implement SwipeCard Composable with movie poster, title, year, rating display
  - Add drag gesture detection for left/right swipe actions
  - Create card animation with rotation and translation effects
  - Implement like/pass buttons as alternative to gestures
  - Add streaming provider badges display on cards
  - _Requirements: 2.1, 2.4, 6.1_

- [ ] 9. Build movie recommendation queue system
  - Create MovieRecommendationEngine for managing recommendation queue
  - Implement automatic queue refilling when running low on cards
  - Add preference-based filtering for genres, year range, and rating
  - Implement streaming provider filtering with strict/loose modes
  - Write unit tests for recommendation logic and filtering
  - _Requirements: 2.5, 4.1, 4.2, 4.3_

- [ ] 10. Implement undo functionality
  - Add last swipe tracking in SwipeViewModel
  - Create undo method that deletes swipe record from Firestore
  - Implement match removal when undo affects existing match
  - Add UI button for undo with proper state management
  - Write unit tests for undo logic and edge cases
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 11. Create matches screen and management
  - Implement MatchViewModel for displaying and managing matches
  - Create matches list UI showing movie details and streaming providers
  - Add mark as watched functionality with notes support
  - Implement streaming provider deep links with fallback to web
  - Write unit tests for match management
  - _Requirements: 3.4, 6.3, 6.4, 8.1, 8.2, 8.3, 8.4_

- [ ] 12. Build preferences and settings system
  - Create PreferencesRepository for storing user filtering preferences
  - Implement SettingsViewModel for preference management
  - Build settings UI for genre selection, year range, rating, and providers
  - Add availability strict toggle functionality
  - Write unit tests for preferences logic
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 13. Implement tonight's pick suggestion algorithm
  - Create suggestion algorithm that ranks matches by rating and release date
  - Add availability filtering based on selected providers and strict mode
  - Implement random tie-breaking for equal-rated movies
  - Create UI for displaying suggested pick with provider links
  - Write unit tests for suggestion algorithm
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 14. Set up Firebase Cloud Functions for notifications
  - Create Cloud Function triggered on match creation
  - Implement FCM message sending to both users when match occurs
  - Add proper error handling and logging for notification delivery
  - Configure function deployment and testing
  - _Requirements: 3.2_

- [ ] 15. Implement data retention and cleanup
  - Create scheduled Cloud Function for 90-day data cleanup
  - Add manual "clear my swipes" functionality in settings
  - Implement cleanup logic that preserves watched matches with notes
  - Add proper error handling and logging for cleanup operations
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] 16. Create navigation and main app structure
  - Set up Navigation Compose with authentication, pairing, swipe, matches, and settings screens
  - Implement main activity with proper theme and navigation handling
  - Create bottom navigation or tab structure for main screens
  - Add deep linking support for room invites
  - _Requirements: 1.2, 10.4_

- [ ] 17. Implement offline support and caching
  - Enable Firestore offline persistence for local data caching
  - Add image caching configuration with Coil
  - Implement swipe queuing for offline scenarios
  - Add proper sync handling when connection is restored
  - Write tests for offline functionality
  - _Requirements: 2.5, 3.1_

- [ ] 18. Add comprehensive error handling
  - Implement AppError sealed class and ErrorHandler
  - Add retry logic for network failures with exponential backoff
  - Create user-friendly error messages and recovery options
  - Add proper error logging and crash reporting
  - Write unit tests for error handling scenarios
  - _Requirements: 2.5, 4.4, 6.4, 7.4_

- [ ] 19. Create Material 3 theme with orange primary color
  - Implement WatchPartyTheme with orange primary color and dark mode
  - Configure proper color schemes for light and dark themes
  - Add consistent typography and component styling
  - Ensure accessibility compliance with proper contrast ratios
  - _Requirements: 2.1, 6.1_

- [ ] 20. Write integration tests and end-to-end scenarios
  - Create integration tests for Firebase real-time synchronization
  - Add two-device testing scenarios for complete matching flow
  - Implement tests for push notification delivery and handling
  - Create performance tests for image loading and database queries
  - Add UI tests for swipe gestures and navigation flows
  - _Requirements: 1.1, 1.2, 2.2, 2.3, 3.1, 3.2_