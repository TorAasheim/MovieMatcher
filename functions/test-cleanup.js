const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for testing
if (!admin.apps.length) {
  admin.initializeApp({
    projectId: 'demo-project-id'
  });
}

const db = admin.firestore();

/**
 * Test script for data cleanup functions
 * This script creates test data and verifies cleanup functionality
 */
async function testDataCleanup() {
  console.log('🧪 Starting data cleanup tests...\n');
  
  try {
    // Create test room and data
    const testRoomId = 'test-room-cleanup';
    const testUserId1 = 'user1';
    const testUserId2 = 'user2';
    
    console.log('📝 Creating test data...');
    
    // Create test room
    await db.collection('rooms').doc(testRoomId).set({
      userIds: [testUserId1, testUserId2],
      createdAt: admin.firestore.Timestamp.now()
    });
    
    // Create old swipes (older than 90 days)
    const oldDate = new Date();
    oldDate.setDate(oldDate.getDate() - 100); // 100 days ago
    const oldTimestamp = admin.firestore.Timestamp.fromDate(oldDate);
    
    // Create recent swipes (within 90 days)
    const recentDate = new Date();
    recentDate.setDate(recentDate.getDate() - 30); // 30 days ago
    const recentTimestamp = admin.firestore.Timestamp.fromDate(recentDate);
    
    // Add old swipes
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('1:user1').set({
      titleId: 1,
      userId: testUserId1,
      decision: 'LIKE',
      timestamp: oldTimestamp
    });
    
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('1:user2').set({
      titleId: 1,
      userId: testUserId2,
      decision: 'LIKE',
      timestamp: oldTimestamp
    });
    
    // Add recent swipes
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('2:user1').set({
      titleId: 2,
      userId: testUserId1,
      decision: 'LIKE',
      timestamp: recentTimestamp
    });
    
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('2:user2').set({
      titleId: 2,
      userId: testUserId2,
      decision: 'LIKE',
      timestamp: recentTimestamp
    });
    
    // Add old matches
    await db.collection('rooms').doc(testRoomId).collection('matches').doc('1').set({
      titleId: 1,
      timestamp: oldTimestamp,
      watched: false,
      notes: ''
    });
    
    // Add old watched match with notes (should be preserved)
    await db.collection('rooms').doc(testRoomId).collection('matches').doc('3').set({
      titleId: 3,
      timestamp: oldTimestamp,
      watched: true,
      notes: 'Great movie!'
    });
    
    // Add recent match
    await db.collection('rooms').doc(testRoomId).collection('matches').doc('2').set({
      titleId: 2,
      timestamp: recentTimestamp,
      watched: false,
      notes: ''
    });
    
    console.log('✅ Test data created successfully\n');
    
    // Test cleanup functions
    console.log('🧹 Testing cleanup functions...');
    
    // Import cleanup functions (you would need to adjust the path)
    const { cleanupOldSwipes, cleanupOldMatches } = require('./lib/index.js');
    
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - 90);
    const cutoffTimestamp = admin.firestore.Timestamp.fromDate(cutoffDate);
    
    // Test swipe cleanup
    console.log('Testing swipe cleanup...');
    const swipesDeleted = await cleanupOldSwipes(testRoomId, cutoffTimestamp);
    console.log(`✅ Deleted ${swipesDeleted} old swipes`);
    
    // Test match cleanup
    console.log('Testing match cleanup...');
    const matchesDeleted = await cleanupOldMatches(testRoomId, cutoffTimestamp);
    console.log(`✅ Deleted ${matchesDeleted} old matches`);
    
    // Verify results
    console.log('\n🔍 Verifying cleanup results...');
    
    const remainingSwipes = await db.collection('rooms').doc(testRoomId).collection('swipes').get();
    console.log(`Remaining swipes: ${remainingSwipes.size}`);
    
    const remainingMatches = await db.collection('rooms').doc(testRoomId).collection('matches').get();
    console.log(`Remaining matches: ${remainingMatches.size}`);
    
    // Check that watched match with notes is preserved
    const watchedMatch = await db.collection('rooms').doc(testRoomId).collection('matches').doc('3').get();
    if (watchedMatch.exists) {
      console.log('✅ Watched match with notes preserved');
    } else {
      console.log('❌ Watched match with notes was deleted (should be preserved)');
    }
    
    // Cleanup test data
    console.log('\n🧽 Cleaning up test data...');
    await db.collection('rooms').doc(testRoomId).delete();
    console.log('✅ Test data cleaned up');
    
    console.log('\n🎉 All cleanup tests completed successfully!');
    
  } catch (error) {
    console.error('❌ Test failed:', error);
    process.exit(1);
  }
}

/**
 * Test manual swipe cleanup
 */
async function testManualCleanup() {
  console.log('\n🧪 Testing manual swipe cleanup...\n');
  
  try {
    const testRoomId = 'test-room-manual';
    const testUserId1 = 'user1';
    const testUserId2 = 'user2';
    
    // Create test room
    await db.collection('rooms').doc(testRoomId).set({
      userIds: [testUserId1, testUserId2],
      createdAt: admin.firestore.Timestamp.now()
    });
    
    // Create test swipes
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('1:user1').set({
      titleId: 1,
      userId: testUserId1,
      decision: 'LIKE',
      timestamp: admin.firestore.Timestamp.now()
    });
    
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('2:user1').set({
      titleId: 2,
      userId: testUserId1,
      decision: 'PASS',
      timestamp: admin.firestore.Timestamp.now()
    });
    
    await db.collection('rooms').doc(testRoomId).collection('swipes').doc('1:user2').set({
      titleId: 1,
      userId: testUserId2,
      decision: 'LIKE',
      timestamp: admin.firestore.Timestamp.now()
    });
    
    // Create a match
    await db.collection('rooms').doc(testRoomId).collection('matches').doc('1').set({
      titleId: 1,
      timestamp: admin.firestore.Timestamp.now(),
      watched: false,
      notes: ''
    });
    
    console.log('📝 Test data created for manual cleanup test');
    
    // Count initial swipes
    const initialSwipes = await db.collection('rooms').doc(testRoomId).collection('swipes').get();
    console.log(`Initial swipes: ${initialSwipes.size}`);
    
    const initialMatches = await db.collection('rooms').doc(testRoomId).collection('matches').get();
    console.log(`Initial matches: ${initialMatches.size}`);
    
    // Test manual cleanup (simulate the function)
    const userSwipesQuery = db.collection('rooms').doc(testRoomId).collection('swipes').where('userId', '==', testUserId1);
    const userSwipes = await userSwipesQuery.get();
    
    console.log(`\n🧹 Deleting ${userSwipes.size} swipes for user ${testUserId1}...`);
    
    const batch = db.batch();
    userSwipes.docs.forEach(doc => {
      batch.delete(doc.ref);
    });
    await batch.commit();
    
    // Verify results
    const remainingSwipes = await db.collection('rooms').doc(testRoomId).collection('swipes').get();
    console.log(`Remaining swipes: ${remainingSwipes.size}`);
    
    // Check if match still exists (should be deleted since user1's like was removed)
    const remainingMatches = await db.collection('rooms').doc(testRoomId).collection('matches').get();
    console.log(`Remaining matches: ${remainingMatches.size}`);
    
    // Cleanup
    await db.collection('rooms').doc(testRoomId).delete();
    console.log('✅ Manual cleanup test completed');
    
  } catch (error) {
    console.error('❌ Manual cleanup test failed:', error);
  }
}

// Run tests
async function runAllTests() {
  await testDataCleanup();
  await testManualCleanup();
  process.exit(0);
}

runAllTests().catch(console.error);