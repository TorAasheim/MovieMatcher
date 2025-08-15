import * as admin from 'firebase-admin';

/**
 * Test helper functions for Cloud Functions
 * These are utility functions to help test the notification system
 */

/**
 * Creates a test match document to trigger the onMatchCreated function
 * This is useful for testing the notification system
 */
export async function createTestMatch(
  roomId: string,
  titleId: number,
  db: admin.firestore.Firestore
): Promise<void> {
  const matchData = {
    titleId,
    timestamp: admin.firestore.Timestamp.now(),
    watched: false,
    notes: ''
  };

  await db.collection('rooms').doc(roomId).collection('matches').add(matchData);
  console.log(`Test match created for room ${roomId} with movie ${titleId}`);
}

/**
 * Creates a test room with two users for testing
 */
export async function createTestRoom(
  roomId: string,
  userIds: [string, string],
  db: admin.firestore.Firestore
): Promise<void> {
  const roomData = {
    userIds,
    createdAt: admin.firestore.Timestamp.now()
  };

  await db.collection('rooms').doc(roomId).set(roomData);
  console.log(`Test room created: ${roomId} with users: ${userIds.join(', ')}`);
}

/**
 * Creates test user documents with FCM tokens
 */
export async function createTestUsers(
  users: Array<{ id: string; fcmToken: string; displayName: string }>,
  db: admin.firestore.Firestore
): Promise<void> {
  const promises = users.map(user => {
    const userData = {
      displayName: user.displayName,
      photoUrl: null,
      fcmToken: user.fcmToken,
      roomId: null,
      createdAt: admin.firestore.Timestamp.now()
    };

    return db.collection('users').doc(user.id).set(userData);
  });

  await Promise.all(promises);
  console.log(`Created ${users.length} test users`);
}

/**
 * Cleans up test data
 */
export async function cleanupTestData(
  roomId: string,
  userIds: string[],
  db: admin.firestore.Firestore
): Promise<void> {
  // Delete room and all subcollections
  const roomRef = db.collection('rooms').doc(roomId);
  
  // Delete matches
  const matchesSnapshot = await roomRef.collection('matches').get();
  const matchDeletePromises = matchesSnapshot.docs.map(doc => doc.ref.delete());
  
  // Delete swipes
  const swipesSnapshot = await roomRef.collection('swipes').get();
  const swipeDeletePromises = swipesSnapshot.docs.map(doc => doc.ref.delete());
  
  // Delete preferences
  const preferencesSnapshot = await roomRef.collection('preferences').get();
  const prefDeletePromises = preferencesSnapshot.docs.map(doc => doc.ref.delete());
  
  await Promise.all([...matchDeletePromises, ...swipeDeletePromises, ...prefDeletePromises]);
  
  // Delete room document
  await roomRef.delete();
  
  // Delete test users
  const userDeletePromises = userIds.map(userId => 
    db.collection('users').doc(userId).delete()
  );
  await Promise.all(userDeletePromises);
  
  console.log(`Cleaned up test data for room ${roomId} and users ${userIds.join(', ')}`);
}