package com.moviematcher.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.moviematcher.app.ui.auth.AuthScreen
import com.moviematcher.app.ui.auth.AuthState
import com.moviematcher.app.ui.auth.AuthViewModel
import com.moviematcher.app.ui.pairing.PairingScreen

/**
 * Main navigation component for the Movie Matcher app
 */
@Composable
fun MovieMatcherNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    NavHost(
        navController = navController,
        startDestination = when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                if (user.roomId != null) {
                    Screen.MainApp.route
                } else {
                    Screen.Pairing.route
                }
            }
            else -> Screen.Auth.route
        }
    ) {
        // Authentication screen
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    // Navigation is handled by observing auth state
                }
            )
        }
        
        // Pairing screen
        composable(Screen.Pairing.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            if (user != null) {
                PairingScreen(
                    userId = user.id,
                    onPairingComplete = { roomId ->
                        navController.navigate(Screen.MainApp.route) {
                            popUpTo(Screen.Pairing.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        // Main app with bottom navigation
        composable(Screen.MainApp.route) {
            val user = (authState as? AuthState.Authenticated)?.user
            if (user != null && user.roomId != null) {
                MainAppScreen(
                    userId = user.id,
                    roomId = user.roomId
                )
            }
        }
        
        // Deep link for room invites
        composable(
            route = "${Screen.JoinRoom.route}/{inviteCode}",
            arguments = Screen.JoinRoom.arguments
        ) { backStackEntry ->
            val inviteCode = backStackEntry.arguments?.getString("inviteCode") ?: ""
            val user = (authState as? AuthState.Authenticated)?.user
            
            if (user != null) {
                PairingScreen(
                    userId = user.id,
                    onPairingComplete = { roomId ->
                        navController.navigate(Screen.MainApp.route) {
                            popUpTo(Screen.JoinRoom.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // User not authenticated, redirect to auth
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.JoinRoom.route) { inclusive = true }
                }
            }
        }
    }
}