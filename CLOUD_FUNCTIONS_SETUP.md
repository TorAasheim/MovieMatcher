# Cloud Functions Setup and Testing Guide

This guide explains how to set up, deploy, and test the Firebase Cloud Functions for Movie Matcher notifications.

## Overview

The Cloud Functions handle push notifications when matches are created between users. When both users in a room like the same movie, a match document is created in Firestore, which triggers the `onMatchCreated` function to send push notifications to both users.

## Prerequisites

1. **Firebase CLI**: Install globally with `npm install -g firebase-tools`
2. **Node.js**: Version 18 or later
3. **Firebase Project**: With Firestore, Authentication, and Cloud Messaging enabled

## Setup Steps

### 1. Install Dependencies
```bash
cd functions
npm install
```

### 2. Build Functions
```bash
npm run build
```

### 3. Deploy to Firebase
```bash
# From project root
firebase deploy --only functions

# Or use the deployment script
./deploy-functions.ps1  # Windows
./deploy-functions.sh   # Linux/Mac
```

## Function Details

### onMatchCreated
- **Trigger**: Document creation at `rooms/{roomId}/matches/{matchId}`
- **Purpose**: Send push notifications when matches occur
- **Process**:
  1. Validates room exists and has exactly 2 users
  2. Retrieves FCM tokens for both users
  3. Sends customized push notifications
  4. Handles invalid tokens by removing them
  5. Logs all operations for monitoring

## Testing

### Method 1: Through Android App
1. Set up two devices with the app
2. Create a room and have both users join
3. Swipe on the same movie (both like it)
4. Check that both devices receive notifications

### Method 2: Manual Firestore Trigger
1. Ensure you have a room with 2 users in Firestore
2. Ensure both users have valid FCM tokens
3. Create a match document manually:
   ```javascript
   // In Firestore console, create document at:
   // rooms/your-room-id/matches/test-match-id
   {
     "titleId": 12345,
     "timestamp": new Date(),
     "watched": false,
     "notes": ""
   }
   ```
4. Check function logs for execution

### Method 3: Test Script
1. Update FCM tokens in `functions/test-notifications.js`
2. Run the test script:
   ```bash
   cd functions
   node test-notifications.js
   ```

## Monitoring

### View Logs
```bash
# Real-time logs
firebase functions:log

# Specific function logs
firebase functions:log --only onMatchCreated
```

### Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to Functions section
4. Click on function name to view metrics and logs

## Troubleshooting

### Common Issues

1. **Function not triggering**
   - Check Firestore security rules allow the write
   - Verify the document path matches exactly: `rooms/{roomId}/matches/{matchId}`
   - Check function deployment was successful

2. **Notifications not received**
   - Verify FCM tokens are valid and current
   - Check that FCM is properly configured in the Android app
   - Ensure devices have network connectivity
   - Check notification permissions are granted

3. **Invalid FCM tokens**
   - The function automatically removes invalid tokens
   - Check logs for token removal messages
   - Ensure the Android app updates tokens when they change

### Error Handling

The function includes comprehensive error handling:
- Invalid rooms or missing users are logged and skipped
- Network failures are retried automatically by Firebase
- Invalid FCM tokens are removed from user documents
- All errors include structured logging for debugging

## Security

### Firestore Rules
The included `firestore.rules` file ensures:
- Users can only access rooms they're members of
- Match creation is properly authenticated
- Data integrity is maintained

### Function Security
- Functions run with admin privileges but validate all data
- User tokens are handled securely
- No sensitive data is logged

## Performance

### Optimization Features
- Batch processing of multiple user notifications
- Automatic cleanup of invalid FCM tokens
- Structured logging for efficient monitoring
- Error recovery and graceful degradation

### Monitoring Metrics
- Function execution time
- Success/failure rates
- FCM delivery rates
- Error patterns

## Cost Considerations

- Functions are billed per invocation and execution time
- FCM notifications are free up to generous limits
- Firestore reads for user/room data count toward quotas
- Consider implementing rate limiting for high-volume scenarios