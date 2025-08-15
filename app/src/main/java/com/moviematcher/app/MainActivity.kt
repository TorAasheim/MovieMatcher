package com.moviematcher.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.moviematcher.app.navigation.MovieMatcherNavigation
import com.moviematcher.app.navigation.Screen
import com.moviematcher.app.ui.auth.AuthState
import com.moviematcher.app.ui.auth.AuthViewModel
import com.moviematcher.app.ui.theme.MovieMatcherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieMatcherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MovieMatcherApp(
                        intent = intent
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MovieMatcherApp(
    intent: Intent? = null,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // Handle deep links
    LaunchedEffect(intent, authState) {
        intent?.data?.let { uri ->
            handleDeepLink(uri, navController, authState)
        }
    }
    
    // Handle navigation based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                val currentRoute = navController.currentDestination?.route
                
                if (user.roomId != null && currentRoute != Screen.MainApp.route) {
                    // User has a room, navigate to main app
                    navController.navigate(Screen.MainApp.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } else if (user.roomId == null && currentRoute != Screen.Pairing.route) {
                    // User needs to pair, navigate to pairing
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Screen.Auth.route) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Loading -> {
                // Show loading state while auth is being determined
            }
            is AuthState.Error -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Screen.Auth.route) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
    
    when (authState) {
        is AuthState.Loading -> {
            // Show loading state while determining auth status
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            MovieMatcherNavigation(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}

/**
 * Handle deep links for room invites
 */
private fun handleDeepLink(
    uri: Uri,
    navController: androidx.navigation.NavHostController,
    authState: AuthState
) {
    when {
        uri.pathSegments.firstOrNull() == "join" -> {
            val inviteCode = uri.pathSegments.getOrNull(1)
            if (inviteCode != null && authState is AuthState.Authenticated) {
                navController.navigate("${Screen.JoinRoom.route}/$inviteCode")
            }
        }
    }
}

