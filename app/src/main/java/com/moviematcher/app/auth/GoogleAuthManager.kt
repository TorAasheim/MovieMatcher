package com.moviematcher.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.moviematcher.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Sign-In authentication flow
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Get the current Firebase user
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser
    
    /**
     * Check if user is currently signed in
     */
    val isSignedIn: Boolean
        get() = currentUser != null
    
    /**
     * Get the sign-in intent for Google authentication
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    /**
     * Handle the result from Google Sign-In activity
     * @param data Intent data from the sign-in activity result
     * @return AuthResult if successful
     * @throws Exception if sign-in fails
     */
    suspend fun handleSignInResult(data: Intent?): AuthResult {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            throw AuthException("Google sign-in failed: ${e.message}", e)
        }
    }
    
    /**
     * Authenticate with Firebase using Google credentials
     */
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): AuthResult {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return try {
            firebaseAuth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            throw AuthException("Firebase authentication failed: ${e.message}", e)
        }
    }
    
    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            throw AuthException("Sign out failed: ${e.message}", e)
        }
    }
    
    /**
     * Revoke access and sign out
     */
    suspend fun revokeAccess() {
        try {
            firebaseAuth.signOut()
            googleSignInClient.revokeAccess().await()
        } catch (e: Exception) {
            throw AuthException("Revoke access failed: ${e.message}", e)
        }
    }
}

/**
 * Custom exception for authentication errors
 */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)