/**
 * Test script for Movie Matcher notification system
 * This script tests the Cloud Function by creating test data and triggering notifications
 * 
 * Usage:
 * node test-notifications.js
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// Make sure you have the service account key or are running in an authenticated environment
admin.initializeApp();

const db = admin.firestore();

// Test configuration
const TEST_CONFIG = {
  roomId: 'test-room-' + Date.now(),
  userIds: ['test-user-1', 'test-user-2'],
  movieId: 12345,
  // Replace these with actual FCM tokens for testing
  // You can get these from the Android app logs or Firebase console
  fcmTokens: [
    'test-token-1', // Replace with actual FCM token
    'test-token-2'  // Replace with actual FCM token
  ]
};

async function runNotificationTest() {
  console.log('🧪 Starting notification test...');
  console.log('Test configuration:', TEST_CONFIG);

  try {
    // Step 1: Create test users with FCM tokens
    console.log('\n📝 Creating test users...');
    await createTestUsers();

    // Step 2: Create test room
    console.log('\n🏠 Creating test room...');
    await createTestRoom();

    // Step 3: Create a match (this should trigger the Cloud Function)
    console.log('\n🎬 Creating test match (this will trigger the notification)...');
    await createTestMatch();

    console.log('\n✅ Test setup complete!');
    console.log('🔔 Check your devices for notifications');
    console.log('📊 Check Firebase Console > Functions > Logs for execution details');
    
    // Wait a bit before cleanup
    console.log('\n⏳ Waiting 10 seconds before cleanup...');
    await new Promise(resolve => setTimeout(resolve, 10000));

    // Step 4: Cleanup
    console.log('\n🧹 Cleaning up test data...');
    await cleanupTestData();

    console.log('\n🎉 Test completed successfully!');

  } catch (error) {
    console.error('❌ Test failed:', error);
    
    // Attempt cleanup even if test failed
    try {
      await cleanupTestData();
    } catch (cleanupError) {
      console.error('❌ Cleanup also failed:', cleanupError);
    }
  }
}

async function createTestUsers() {
  const userPromises = TEST_CONFIG.userIds.map((userId, index) => {
    const userData = {
      displayName: `Test User ${index + 1}`,
      photoUrl: null,
      fcmToken: TEST_CONFIG.fcmTokens[index],
      roomId: null,
      createdAt: admin.firestore.Timestamp.now()
    };

    return db.collection('users').doc(userId).set(userData);
  });

  await Promise.all(userPromises);
  console.log(`✅ Created ${TEST_CONFIG.userIds.length} test users`);
}

async function createTestRoom() {
  const roomData = {
    userIds: TEST_CONFIG.userIds,
    createdAt: admin.firestore.Timestamp.now()
  };

  await db.collection('rooms').doc(TEST_CONFIG.roomId).set(roomData);
  console.log(`✅ Created test room: ${TEST_CONFIG.roomId}`);
}

async function createTestMatch() {
  const matchData = {
    titleId: TEST_CONFIG.movieId,
    timestamp: admin.firestore.Timestamp.now(),
    watched: false,
    notes: ''
  };

  // This write operation should trigger the onMatchCreated Cloud Function
  await db.collection('rooms')
    .doc(TEST_CONFIG.roomId)
    .collection('matches')
    .add(matchData);
    
  console.log(`✅ Created test match for movie ${TEST_CONFIG.movieId}`);
}

async function cleanupTestData() {
  // Delete room and all subcollections
  const roomRef = db.collection('rooms').doc(TEST_CONFIG.roomId);
  
  // Delete matches
  const matchesSnapshot = await roomRef.collection('matches').get();
  const matchDeletePromises = matchesSnapshot.docs.map(doc => doc.ref.delete());
  
  await Promise.all(matchDeletePromises);
  
  // Delete room document
  await roomRef.delete();
  
  // Delete test users
  const userDeletePromises = TEST_CONFIG.userIds.map(userId => 
    db.collection('users').doc(userId).delete()
  );
  await Promise.all(userDeletePromises);
  
  console.log('✅ Cleaned up test data');
}

// Run the test
if (require.main === module) {
  runNotificationTest()
    .then(() => {
      console.log('\n🏁 Test script finished');
      process.exit(0);
    })
    .catch((error) => {
      console.error('\n💥 Test script failed:', error);
      process.exit(1);
    });
}

module.exports = {
  runNotificationTest,
  TEST_CONFIG
};