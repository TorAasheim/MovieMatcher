# Movie Matcher Cloud Functions

This directory contains Firebase Cloud Functions for the Movie Matcher app.

## Functions

### onMatchCreated
- **Trigger**: Firestore document creation at `rooms/{roomId}/matches/{matchId}`
- **Purpose**: Sends push notifications to both users when a match is created
- **Features**:
  - Retrieves room data to find user IDs
  - Gets FCM tokens for both users
  - Sends customized push notifications
  - Handles invalid tokens by removing them
  - Comprehensive error handling and logging

## Setup

1. **Install dependencies**:
   ```bash
   cd functions
   npm install
   ```

2. **Build the functions**:
   ```bash
   npm run build
   ```

3. **Test locally with Firebase Emulator**:
   ```bash
   # From project root
   firebase emulators:start --only functions,firestore
   ```

4. **Deploy to Firebase**:
   ```bash
   # From project root
   firebase deploy --only functions
   ```

## Development

### Local Testing
```bash
# Start emulator suite
firebase emulators:start

# In another terminal, trigger a test match creation
# This can be done through the Android app or by directly writing to Firestore
```

### Viewing Logs
```bash
# Local logs (when using emulator)
# Check the emulator UI at http://localhost:4000

# Production logs
firebase functions:log
```

### Environment Variables
The functions use Firebase Admin SDK which automatically uses the project's service account when deployed. For local development, you may need to set up authentication:

```bash
# Set up local authentication (if needed)
export GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account-key.json"
```

## Error Handling

The function includes comprehensive error handling:
- Invalid FCM tokens are automatically removed from user documents
- Network failures are logged with full context
- Room validation ensures proper data structure
- All errors are logged with structured data for debugging

## Monitoring

Monitor function performance and errors in the Firebase Console:
1. Go to Firebase Console > Functions
2. Click on the function name to view metrics
3. Check logs for any errors or warnings

## Testing

To test the notification function:

1. **Create a test match** through the Android app or directly in Firestore
2. **Check the function logs** to ensure it executed successfully
3. **Verify notifications** are received on both devices
4. **Test error scenarios** like invalid FCM tokens

### Manual Testing via Firestore
You can manually trigger the function by creating a document in Firestore:

```javascript
// In Firestore console, create a document at:
// rooms/test-room-id/matches/test-match-id
{
  "titleId": 12345,
  "timestamp": new Date(),
  "watched": false,
  "notes": ""
}
```

Make sure the room document exists with valid user IDs and that the users have FCM tokens.