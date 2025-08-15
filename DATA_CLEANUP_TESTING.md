# Data Cleanup Testing Guide

This document explains how to test the data retention and cleanup functionality implemented in task 15.

## Overview

The data cleanup functionality consists of two main components:

1. **Scheduled Cloud Function**: Automatically cleans up data older than 90 days
2. **Manual Cleanup**: Allows users to clear their swipe history through the settings screen

## Components Implemented

### Firebase Cloud Functions

#### 1. Scheduled Data Cleanup (`scheduledDataCleanup`)
- **Trigger**: Runs daily at 2 AM UTC using Pub/Sub scheduler
- **Function**: Cleans up swipes and matches older than 90 days
- **Preservation**: Keeps watched matches that have notes
- **Location**: `functions/src/index.ts`

#### 2. Manual Swipe Cleanup (`clearUserSwipes`)
- **Trigger**: HTTP callable function triggered by user action
- **Function**: Deletes all swipes for a specific user in a room
- **Security**: Verifies user authentication and room membership
- **Cleanup**: Also removes orphaned matches when appropriate

### Android App Components

#### 1. CleanupRepository Interface
- **Location**: `app/src/main/java/com/moviematcher/app/data/repository/CleanupRepository.kt`
- **Purpose**: Defines the contract for cleanup operations

#### 2. CleanupRepositoryImpl
- **Location**: `app/src/main/java/com/moviematcher/app/data/repository/CleanupRepositoryImpl.kt`
- **Purpose**: Implements cleanup using Firebase Cloud Functions

#### 3. Settings Screen Integration
- **Location**: `app/src/main/java/com/moviematcher/app/ui/settings/SettingsScreen.kt`
- **Feature**: Added "Data Management" section with "Clear My Swipes" button
- **UX**: Includes confirmation dialog and proper error handling

#### 4. SettingsViewModel Updates
- **Location**: `app/src/main/java/com/moviematcher/app/ui/settings/SettingsViewModel.kt`
- **Features**: Added `clearMySwipes()` and `clearCleanupResult()` methods

## Testing the Implementation

### 1. Unit Tests

#### CleanupRepository Tests
```bash
./gradlew :app:testDebugUnitTest --tests "*CleanupRepositoryTest*"
```

Tests cover:
- Successful swipe deletion with various return types
- Error handling and exception mapping
- Edge cases (null data, invalid responses)

#### SettingsViewModel Tests
```bash
./gradlew :app:testDebugUnitTest --tests "*SettingsViewModelTest*"
```

Tests cover:
- Successful cleanup with result message
- Error handling and state management
- Edge cases (no room initialized, zero swipes)

### 2. Firebase Functions Testing

#### Manual Testing Script
A test script is provided at `functions/test-cleanup.js` that:
- Creates test data (old and recent swipes/matches)
- Tests the cleanup functions
- Verifies preservation of watched matches with notes
- Cleans up test data

#### Running the Test Script
```bash
cd functions
node test-cleanup.js
```

### 3. Integration Testing

#### Testing the Scheduled Function
1. Deploy the functions to Firebase
2. Manually trigger the scheduled function:
   ```bash
   firebase functions:shell
   scheduledDataCleanup({})
   ```

#### Testing Manual Cleanup
1. Build and run the Android app
2. Navigate to Settings screen
3. Scroll to "Data Management" section
4. Tap "Clear My Swipes" button
5. Confirm the action in the dialog
6. Verify success message appears

### 4. End-to-End Testing

#### Complete Workflow Test
1. Create test room with two users
2. Generate swipe data (some old, some recent)
3. Create matches (some watched with notes, some without)
4. Run scheduled cleanup
5. Verify:
   - Old swipes are deleted
   - Recent swipes are preserved
   - Watched matches with notes are preserved
   - Unwatched old matches are deleted

## Data Preservation Rules

### Scheduled Cleanup (90-day retention)
- ✅ **Preserves**: Watched matches with notes (regardless of age)
- ✅ **Preserves**: All data newer than 90 days
- ❌ **Deletes**: Swipes older than 90 days
- ❌ **Deletes**: Unwatched matches older than 90 days
- ❌ **Deletes**: Watched matches without notes older than 90 days

### Manual Cleanup
- ✅ **Preserves**: Partner's swipes
- ✅ **Preserves**: Watched matches with notes
- ❌ **Deletes**: All user's swipes in the room
- ❌ **Deletes**: Matches that become orphaned (no longer have both users' likes)

## Security Considerations

### Authentication & Authorization
- All cleanup functions verify user authentication
- Room membership is validated before allowing cleanup
- Firestore security rules prevent unauthorized access

### Data Integrity
- Batch operations ensure atomicity
- Proper error handling prevents partial cleanup
- Logging provides audit trail for cleanup operations

## Monitoring & Logging

### Cloud Functions Logs
- Scheduled cleanup logs summary statistics
- Manual cleanup logs user actions and results
- Error conditions are logged with full context

### Android App Feedback
- Success messages show number of swipes deleted
- Error messages provide user-friendly explanations
- Loading states prevent multiple simultaneous operations

## Requirements Verification

This implementation satisfies all requirements from task 15:

- ✅ **9.1**: Automatic 90-day data cleanup via scheduled Cloud Function
- ✅ **9.2**: Manual "clear my swipes" functionality in settings
- ✅ **9.3**: Preservation of user profiles, room info, and watched matches with notes
- ✅ **9.4**: Comprehensive error handling and logging throughout

## Deployment Notes

### Firebase Functions Deployment
```bash
cd functions
npm run build
firebase deploy --only functions
```

### Android App Build
```bash
./gradlew :app:assembleDebug
```

### Required Permissions
- Firebase Functions need Firestore read/write access
- Scheduled function needs Pub/Sub trigger permissions
- Android app needs internet permission for Cloud Functions calls