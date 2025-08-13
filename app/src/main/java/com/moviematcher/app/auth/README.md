# Authentication Module

This module handles Firebase authentication with Google Sign-In integration.

## Components

### GoogleAuthManager
- Manages Google Sign-In flow
- Handles Firebase authentication with Google credentials
- Provides sign-out and access revocation functionality
- Includes proper error handling with custom AuthException

### UserRepository
- Creates and updates user profiles in Firestore
- Manages FCM token registration and updates
- Handles room membership updates
- Provides user data CRUD operations

### AuthViewModel
- Coordinates authentication flow
- Manages authentication state (Loading, Authenticated, Unauthenticated, Error)
- Handles sign-in result processing
- Provides FCM token refresh functionality

### MovieMatcherMessagingService
- Extends FirebaseMessagingService
- Automatically updates FCM tokens when they change
- Prepared for handling push notifications (to be implemented in later tasks)

## Usage

1. Inject AuthViewModel into your Composable
2. Use AuthScreen for the authentication UI
3. Handle authentication state changes with collectAsStateWithLifecycle
4. Navigate to main app when AuthState.Authenticated is received

## Dependencies

- Firebase Auth
- Firebase Firestore
- Firebase Cloud Messaging
- Google Play Services Auth
- Hilt for dependency injection

## Testing

Comprehensive unit tests are provided for all components:
- GoogleAuthManagerTest
- UserRepositoryTest
- AuthViewModelTest
- FirebaseModuleTest
- MovieMatcherMessagingServiceTest