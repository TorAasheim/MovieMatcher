package com.moviematcher.app.ui.auth

import android.content.Intent
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.moviematcher.app.auth.AuthException
import com.moviematcher.app.auth.GoogleAuthManager
import com.moviematcher.app.data.model.User
import com.moviematcher.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AuthViewModelTest {
    
    @Mock
    private lateinit var googleAuthManager: GoogleAuthManager
    
    @Mock
    private lateinit var userRepository: UserRepository
    
    @Mock
    private lateinit var firebaseUser: FirebaseUser
    
    @Mock
    private lateinit var authResult: AuthResult
    
    @Mock
    private lateinit var intent: Intent
    
    private lateinit var authViewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val testUser = User(
        id = "test-user-id",
        displayName = "Test User",
        photoUrl = "https://example.com/photo.jpg",
        fcmToken = "test-fcm-token",
        roomId = null,
        createdAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        whenever(googleAuthManager.currentUser).thenReturn(null)
        
        authViewModel = AuthViewModel(googleAuthManager, userRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Unauthenticated when no user signed in`() = runTest {
        // Given - setup already done in @Before
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Unauthenticated)
    }
    
    @Test
    fun `initial state is Authenticated when user is signed in`() = runTest {
        // Given
        whenever(googleAuthManager.currentUser).thenReturn(firebaseUser)
        whenever(userRepository.createOrUpdateUser(firebaseUser)).thenReturn(testUser)
        
        // When
        val viewModel = AuthViewModel(googleAuthManager, userRepository)
        
        // Then
        assertTrue(viewModel.authState.value is AuthState.Authenticated)
        assertEquals(testUser, (viewModel.authState.value as AuthState.Authenticated).user)
        assertEquals(testUser, viewModel.currentUser.value)
    }
    
    @Test
    fun `getSignInIntent returns intent from GoogleAuthManager`() {
        // Given
        whenever(googleAuthManager.getSignInIntent()).thenReturn(intent)
        
        // When
        val result = authViewModel.getSignInIntent()
        
        // Then
        assertEquals(intent, result)
    }
    
    @Test
    fun `handleSignInResult succeeds with valid data`() = runTest {
        // Given
        whenever(googleAuthManager.handleSignInResult(intent)).thenReturn(authResult)
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(userRepository.createOrUpdateUser(firebaseUser)).thenReturn(testUser)
        
        // When
        authViewModel.handleSignInResult(intent)
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Authenticated)
        assertEquals(testUser, (authViewModel.authState.value as AuthState.Authenticated).user)
        assertEquals(testUser, authViewModel.currentUser.value)
    }
    
    @Test
    fun `handleSignInResult fails with AuthException`() = runTest {
        // Given
        val errorMessage = "Authentication failed"
        whenever(googleAuthManager.handleSignInResult(intent))
            .thenThrow(AuthException(errorMessage))
        
        // When
        authViewModel.handleSignInResult(intent)
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Error)
        assertEquals(errorMessage, (authViewModel.authState.value as AuthState.Error).message)
    }
    
    @Test
    fun `handleSignInResult fails when no user returned`() = runTest {
        // Given
        whenever(googleAuthManager.handleSignInResult(intent)).thenReturn(authResult)
        whenever(authResult.user).thenReturn(null)
        
        // When
        authViewModel.handleSignInResult(intent)
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Error)
        assertTrue((authViewModel.authState.value as AuthState.Error).message.contains("No user returned"))
    }
    
    @Test
    fun `signOut clears user and sets state to Unauthenticated`() = runTest {
        // Given - start with authenticated state
        whenever(googleAuthManager.currentUser).thenReturn(firebaseUser)
        whenever(userRepository.createOrUpdateUser(firebaseUser)).thenReturn(testUser)
        val viewModel = AuthViewModel(googleAuthManager, userRepository)
        
        // When
        viewModel.signOut()
        
        // Then
        assertTrue(viewModel.authState.value is AuthState.Unauthenticated)
        assertEquals(null, viewModel.currentUser.value)
        verify(googleAuthManager).signOut()
    }
    
    @Test
    fun `signOut handles exception gracefully`() = runTest {
        // Given
        val errorMessage = "Sign out failed"
        whenever(googleAuthManager.signOut()).thenThrow(Exception(errorMessage))
        
        // When
        authViewModel.signOut()
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Error)
        assertTrue((authViewModel.authState.value as AuthState.Error).message.contains(errorMessage))
    }
    
    @Test
    fun `refreshFcmToken calls repository when user exists`() = runTest {
        // Given - start with authenticated state
        whenever(googleAuthManager.currentUser).thenReturn(firebaseUser)
        whenever(userRepository.createOrUpdateUser(firebaseUser)).thenReturn(testUser)
        val viewModel = AuthViewModel(googleAuthManager, userRepository)
        
        // When
        viewModel.refreshFcmToken()
        
        // Then
        verify(userRepository).refreshFcmToken(testUser.id)
    }
    
    @Test
    fun `clearError resets to appropriate state`() = runTest {
        // Given - start with error state
        whenever(googleAuthManager.handleSignInResult(intent))
            .thenThrow(AuthException("Test error"))
        authViewModel.handleSignInResult(intent)
        
        // When
        authViewModel.clearError()
        
        // Then
        assertTrue(authViewModel.authState.value is AuthState.Unauthenticated)
    }
}