# Movie Matcher

A Tinder-style Android app for couples to discover movies they both want to watch together.

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Google account for Firebase setup
- TMDB API account

### Configuration

1. **TMDB API Setup**
   - Create an account at [The Movie Database](https://www.themoviedb.org/)
   - Go to Settings > API and request an API key
   - Add your API key to `local.properties`:
     ```
     TMDB_API_KEY=your_actual_api_key_here
     ```

2. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication with Google Sign-In
   - Enable Firestore Database
   - Enable Cloud Messaging
   - Download the `google-services.json` file and replace the placeholder in `app/google-services.json`

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

## Project Structure

```
app/
├── src/main/java/com/moviematcher/app/
│   ├── MainActivity.kt                    # Main activity
│   ├── MovieMatcherApplication.kt         # Application class with Hilt
│   ├── ui/theme/                         # Material 3 theme with orange primary
│   └── notification/                     # FCM messaging service
├── build.gradle.kts                     # App-level dependencies
└── google-services.json                 # Firebase configuration
```

## Dependencies

- **Firebase**: Authentication, Firestore, Cloud Messaging
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system with orange primary color
- **Hilt**: Dependency injection
- **Retrofit + Moshi**: TMDB API client
- **Coil**: Image loading
- **Navigation Compose**: Screen navigation

## Next Steps

This is the basic project structure. The next tasks will implement:
1. Core data models and interfaces
2. Firebase authentication
3. TMDB API integration
4. Room management system
5. Swipe functionality
6. And more...

## Notes

- The `google-services.json` file contains placeholder values and must be replaced with your actual Firebase configuration
- The `local.properties` file is gitignored and contains your TMDB API key
- All Firebase dependencies are managed by the Firebase BOM for version consistency