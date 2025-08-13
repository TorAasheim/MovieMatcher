package com.moviematcher.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.moviematcher.app.R
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class GoogleAuthManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var firebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var firebaseUser: FirebaseUser
    
    @Mock
    private lateinit var authResult: AuthResult
    
    private lateinit var googleAuthManager: GoogleAuthManager
    
    @Before
    fun setup() {
        whenever(context.getString(R.string.default_web_client_id))
            .thenReturn("test-web-client-id")
        
        googleAuthManager = GoogleAuthManager(context, firebaseAuth)
    }
    
    @Test
    fun `currentUser returns Firebase user when signed in`() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        
        // When
        val result = googleAuthManager.currentUser
        
        // Then
        assertEquals(firebaseUser, result)
    }
    
    @Test
    fun `currentUser returns null when not signed in`() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)
        
        // When
        val result = googleAuthManager.currentUser
        
        // Then
        assertEquals(null, result)
    }
    
    @Test
    fun `isSignedIn returns true when user is signed in`() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        
        // When
        val result = googleAuthManager.isSignedIn
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isSignedIn returns false when user is not signed in`() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)
        
        // When
        val result = googleAuthManager.isSignedIn
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getSignInIntent returns valid intent`() {
        // When
        val result = googleAuthManager.getSignInIntent()
        
        // Then
        assertNotNull(result)
        assertTrue(result is Intent)
    }
}