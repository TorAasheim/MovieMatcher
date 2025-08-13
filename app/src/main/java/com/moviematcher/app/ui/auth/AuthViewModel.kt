package com.moviematcher.app.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviematcher.app.auth.AuthException
import com.moviematcher.app.auth.GoogleAuthManager
import com.moviematcher.app.data.model.User
import com.moviematcher.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state and operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Check current authentication state
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val firebaseUser = googleAuthManager.currentUser
                if (firebaseUser != null) {
                    // User is signed in, get or create user profile
                    val user = userRepository.createOrUpdateUser(firebaseUser)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    // User is not signed in
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication check failed")
            }
        }
    }
    
    /**
     * Get the Google Sign-In intent
     */
    fun getSignInIntent(): Intent {
        return googleAuthManager.getSignInIntent()
    }
    
    /**
     * Handle the result from Google Sign-In activity
     */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                val authResult = googleAuthManager.handleSignInResult(data)
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Create or update user profile in Firestore
                    val user = userRepository.createOrUpdateUser(firebaseUser)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error("Sign-in failed: No user returned")
                }
            } catch (e: AuthException) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Unexpected error: ${e.message}")
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                googleAuthManager.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign out failed: ${e.message}")
            }
        }
    }
    
    /**
     * Refresh FCM token for the current user
     */
    fun refreshFcmToken() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                userRepository.refreshFcmToken(user.id)
            }
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            val user = _currentUser.value
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
    }
}

/**
 * Represents different authentication states
 */
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}