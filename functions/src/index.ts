import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Cloud Function triggered when a new match is created
 * Sends push notifications to both users in the room
 */
export const onMatchCreated = functions.firestore
  .document('rooms/{roomId}/matches/{matchId}')
  .onCreate(async (snapshot, context) => {
    const { roomId, matchId } = context.params;
    const matchData = snapshot.data();
    
    try {
      functions.logger.info(`Match created in room ${roomId}: ${matchId}`, {
        roomId,
        matchId,
        titleId: matchData.titleId
      });

      // Get room data to find user IDs
      const roomDoc = await db.collection('rooms').doc(roomId).get();
      if (!roomDoc.exists) {
        functions.logger.error(`Room not found: ${roomId}`);
        return;
      }

      const roomData = roomDoc.data();
      const userIds = roomData?.userIds as string[];
      
      if (!userIds || userIds.length !== 2) {
        functions.logger.error(`Invalid room data - expected 2 users, got ${userIds?.length}`, {
          roomId,
          userIds
        });
        return;
      }

      // Get movie details for the notification
      const movieTitle = await getMovieTitle(matchData.titleId);
      
      // Get FCM tokens for both users
      const userTokens = await getUserFcmTokens(userIds);
      
      if (userTokens.length === 0) {
        functions.logger.warn(`No FCM tokens found for users in room ${roomId}`, {
          roomId,
          userIds
        });
        return;
      }

      // Send notifications to all users with valid tokens
      const notificationPromises = userTokens.map(async ({ userId, token }) => {
        try {
          const message = {
            token,
            notification: {
              title: "🎬 It's a Match!",
              body: `You both liked "${movieTitle}"! Check your matches to start watching.`
            },
            data: {
              type: 'match_created',
              roomId,
              titleId: matchData.titleId.toString(),
              movieTitle: movieTitle || 'Unknown Movie'
            },
            android: {
              notification: {
                icon: 'ic_notification',
                color: '#FF6B35',
                channelId: 'matches'
              }
            }
          };

          const response = await messaging.send(message);
          functions.logger.info(`Notification sent successfully to user ${userId}`, {
            userId,
            messageId: response,
            roomId,
            titleId: matchData.titleId
          });
          
          return { userId, success: true, messageId: response };
        } catch (error) {
          functions.logger.error(`Failed to send notification to user ${userId}`, {
            userId,
            error: error instanceof Error ? error.message : String(error),
            roomId,
            titleId: matchData.titleId
          });
          
          // If token is invalid, remove it from user document
          if (error instanceof Error && 
              (error.message.includes('registration-token-not-registered') ||
               error.message.includes('invalid-registration-token'))) {
            await removeInvalidFcmToken(userId);
          }
          
          return { userId, success: false, error: error instanceof Error ? error.message : String(error) };
        }
      });

      const results = await Promise.all(notificationPromises);
      const successCount = results.filter(r => r.success).length;
      const failureCount = results.filter(r => !r.success).length;

      functions.logger.info(`Match notification summary for room ${roomId}`, {
        roomId,
        titleId: matchData.titleId,
        movieTitle,
        totalUsers: userIds.length,
        tokensFound: userTokens.length,
        notificationsSent: successCount,
        notificationsFailed: failureCount,
        results
      });

    } catch (error) {
      functions.logger.error(`Error processing match creation for room ${roomId}`, {
        roomId,
        matchId,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined
      });
      
      // Re-throw to ensure the function is marked as failed
      throw error;
    }
  });

/**
 * Helper function to get movie title from cached data or external API
 * For now, returns a placeholder since we don't have TMDB integration in functions
 */
async function getMovieTitle(titleId: number): Promise<string> {
  try {
    // In a real implementation, you might:
    // 1. Check if movie data is cached in Firestore
    // 2. Make a call to TMDB API if not cached
    // 3. Cache the result for future use
    
    // For now, return a generic title with the ID
    return `Movie #${titleId}`;
  } catch (error) {
    functions.logger.warn(`Failed to get movie title for ${titleId}`, {
      titleId,
      error: error instanceof Error ? error.message : String(error)
    });
    return `Movie #${titleId}`;
  }
}

/**
 * Helper function to get FCM tokens for a list of user IDs
 */
async function getUserFcmTokens(userIds: string[]): Promise<Array<{ userId: string; token: string }>> {
  const tokenPromises = userIds.map(async (userId) => {
    try {
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        functions.logger.warn(`User document not found: ${userId}`);
        return null;
      }
      
      const userData = userDoc.data();
      const fcmToken = userData?.fcmToken;
      
      if (!fcmToken) {
        functions.logger.warn(`No FCM token found for user: ${userId}`);
        return null;
      }
      
      return { userId, token: fcmToken };
    } catch (error) {
      functions.logger.error(`Error getting FCM token for user ${userId}`, {
        userId,
        error: error instanceof Error ? error.message : String(error)
      });
      return null;
    }
  });
  
  const tokens = await Promise.all(tokenPromises);
  return tokens.filter((token): token is { userId: string; token: string } => token !== null);
}

/**
 * Helper function to remove invalid FCM token from user document
 */
async function removeInvalidFcmToken(userId: string): Promise<void> {
  try {
    await db.collection('users').doc(userId).update({
      fcmToken: admin.firestore.FieldValue.delete()
    });
    
    functions.logger.info(`Removed invalid FCM token for user ${userId}`, { userId });
  } catch (error) {
    functions.logger.error(`Failed to remove invalid FCM token for user ${userId}`, {
      userId,
      error: error instanceof Error ? error.message : String(error)
    });
  }
}

/**
 * Scheduled Cloud Function for 90-day data cleanup
 * Runs daily at 2 AM UTC to clean up old swipes and matches
 */
export const scheduledDataCleanup = functions.pubsub
  .schedule('0 2 * * *') // Daily at 2 AM UTC
  .timeZone('UTC')
  .onRun(async (context) => {
    const startTime = Date.now();
    functions.logger.info('Starting scheduled data cleanup');
    
    try {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - 90); // 90 days ago
      const cutoffTimestamp = admin.firestore.Timestamp.fromDate(cutoffDate);
      
      let totalSwipesDeleted = 0;
      let totalMatchesDeleted = 0;
      let roomsProcessed = 0;
      let errors = 0;
      
      // Get all rooms to process
      const roomsSnapshot = await db.collection('rooms').get();
      
      for (const roomDoc of roomsSnapshot.docs) {
        const roomId = roomDoc.id;
        
        try {
          // Clean up old swipes
          const swipesDeleted = await cleanupOldSwipes(roomId, cutoffTimestamp);
          totalSwipesDeleted += swipesDeleted;
          
          // Clean up old matches (preserve watched matches with notes)
          const matchesDeleted = await cleanupOldMatches(roomId, cutoffTimestamp);
          totalMatchesDeleted += matchesDeleted;
          
          roomsProcessed++;
          
          functions.logger.info(`Cleaned up room ${roomId}`, {
            roomId,
            swipesDeleted,
            matchesDeleted
          });
          
        } catch (error) {
          errors++;
          functions.logger.error(`Error cleaning up room ${roomId}`, {
            roomId,
            error: error instanceof Error ? error.message : String(error)
          });
        }
      }
      
      const duration = Date.now() - startTime;
      
      functions.logger.info('Scheduled data cleanup completed', {
        duration: `${duration}ms`,
        roomsProcessed,
        totalSwipesDeleted,
        totalMatchesDeleted,
        errors,
        cutoffDate: cutoffDate.toISOString()
      });
      
    } catch (error) {
      functions.logger.error('Scheduled data cleanup failed', {
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined
      });
      throw error;
    }
  });

/**
 * Clean up old swipes for a room
 * @param roomId The room ID
 * @param cutoffTimestamp Timestamp before which to delete swipes
 * @returns Number of swipes deleted
 */
async function cleanupOldSwipes(roomId: string, cutoffTimestamp: admin.firestore.Timestamp): Promise<number> {
  const swipesRef = db.collection('rooms').doc(roomId).collection('swipes');
  const oldSwipesQuery = swipesRef.where('timestamp', '<', cutoffTimestamp);
  
  const snapshot = await oldSwipesQuery.get();
  
  if (snapshot.empty) {
    return 0;
  }
  
  // Delete in batches to avoid timeout
  const batch = db.batch();
  let count = 0;
  
  snapshot.docs.forEach(doc => {
    batch.delete(doc.ref);
    count++;
  });
  
  await batch.commit();
  return count;
}

/**
 * Clean up old matches for a room, preserving watched matches with notes
 * @param roomId The room ID
 * @param cutoffTimestamp Timestamp before which to delete matches
 * @returns Number of matches deleted
 */
async function cleanupOldMatches(roomId: string, cutoffTimestamp: admin.firestore.Timestamp): Promise<number> {
  const matchesRef = db.collection('rooms').doc(roomId).collection('matches');
  const oldMatchesQuery = matchesRef.where('timestamp', '<', cutoffTimestamp);
  
  const snapshot = await oldMatchesQuery.get();
  
  if (snapshot.empty) {
    return 0;
  }
  
  // Filter out watched matches with notes
  const batch = db.batch();
  let count = 0;
  
  snapshot.docs.forEach(doc => {
    const matchData = doc.data();
    const isWatched = matchData.watched === true;
    const hasNotes = matchData.notes && matchData.notes.trim().length > 0;
    
    // Only delete if not watched or has no notes
    if (!isWatched || !hasNotes) {
      batch.delete(doc.ref);
      count++;
    }
  });
  
  if (count > 0) {
    await batch.commit();
  }
  
  return count;
}

/**
 * HTTP Cloud Function for manual swipe cleanup
 * Allows users to clear their swipe history in a room
 */
export const clearUserSwipes = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  
  const { roomId } = data;
  const userId = context.auth.uid;
  
  if (!roomId || typeof roomId !== 'string') {
    throw new functions.https.HttpsError('invalid-argument', 'roomId is required and must be a string');
  }
  
  try {
    functions.logger.info(`User ${userId} requesting swipe cleanup for room ${roomId}`, {
      userId,
      roomId
    });
    
    // Verify user is a member of the room
    const roomDoc = await db.collection('rooms').doc(roomId).get();
    if (!roomDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Room not found');
    }
    
    const roomData = roomDoc.data();
    const userIds = roomData?.userIds as string[];
    
    if (!userIds || !userIds.includes(userId)) {
      throw new functions.https.HttpsError('permission-denied', 'User is not a member of this room');
    }
    
    // Delete all swipes by this user in this room
    const swipesRef = db.collection('rooms').doc(roomId).collection('swipes');
    const userSwipesQuery = swipesRef.where('userId', '==', userId);
    
    const snapshot = await userSwipesQuery.get();
    
    if (snapshot.empty) {
      functions.logger.info(`No swipes found for user ${userId} in room ${roomId}`, {
        userId,
        roomId
      });
      return { swipesDeleted: 0 };
    }
    
    // Delete swipes in batches
    const batch = db.batch();
    let swipesDeleted = 0;
    
    snapshot.docs.forEach(doc => {
      batch.delete(doc.ref);
      swipesDeleted++;
    });
    
    await batch.commit();
    
    // Also need to clean up any matches that were created by this user's swipes
    // This is more complex as we need to check if the partner also swiped
    await cleanupOrphanedMatches(roomId, userId);
    
    functions.logger.info(`Cleared ${swipesDeleted} swipes for user ${userId} in room ${roomId}`, {
      userId,
      roomId,
      swipesDeleted
    });
    
    return { swipesDeleted };
    
  } catch (error) {
    functions.logger.error(`Error clearing swipes for user ${userId} in room ${roomId}`, {
      userId,
      roomId,
      error: error instanceof Error ? error.message : String(error)
    });
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    
    throw new functions.https.HttpsError('internal', 'Failed to clear swipes');
  }
});

/**
 * Clean up matches that no longer have both users' swipes
 * @param roomId The room ID
 * @param userId The user whose swipes were deleted
 */
async function cleanupOrphanedMatches(roomId: string, userId: string): Promise<void> {
  const matchesRef = db.collection('rooms').doc(roomId).collection('matches');
  const swipesRef = db.collection('rooms').doc(roomId).collection('swipes');
  
  // Get all matches
  const matchesSnapshot = await matchesRef.get();
  
  if (matchesSnapshot.empty) {
    return;
  }
  
  const batch = db.batch();
  let matchesDeleted = 0;
  
  for (const matchDoc of matchesSnapshot.docs) {
    const matchData = matchDoc.data();
    const titleId = matchData.titleId;
    
    // Don't delete watched matches with notes
    const isWatched = matchData.watched === true;
    const hasNotes = matchData.notes && matchData.notes.trim().length > 0;
    
    if (isWatched && hasNotes) {
      continue;
    }
    
    // Check if both users still have swipes for this title
    const swipesForTitle = await swipesRef
      .where('titleId', '==', titleId)
      .where('decision', '==', 'LIKE')
      .get();
    
    const userIds = new Set(swipesForTitle.docs.map(doc => doc.data().userId));
    
    // If we don't have 2 users with likes for this title, delete the match
    if (userIds.size < 2) {
      batch.delete(matchDoc.ref);
      matchesDeleted++;
    }
  }
  
  if (matchesDeleted > 0) {
    await batch.commit();
    functions.logger.info(`Cleaned up ${matchesDeleted} orphaned matches in room ${roomId}`, {
      roomId,
      matchesDeleted
    });
  }
}