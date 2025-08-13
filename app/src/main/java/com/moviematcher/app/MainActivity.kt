package com.moviematcher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.moviematcher.app.ui.auth.AuthScreen
import com.moviematcher.app.ui.auth.AuthViewModel
import com.moviematcher.app.ui.auth.AuthState
import com.moviematcher.app.ui.pairing.PairingScreen
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
                    MovieMatcherApp()
                }
            }
        }
    }
}

@Composable
fun MovieMatcherApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // Debug logging
    LaunchedEffect(authState) {
        println("Auth state changed: $authState")
    }
    
    when (authState) {
        is AuthState.Authenticated -> {
            val authenticatedState = authState as AuthState.Authenticated
            println("Showing pairing screen for user: ${authenticatedState.user.displayName}")
            // User is authenticated, show pairing screen
            PairingScreen(
                userId = authenticatedState.user.id,
                onPairingComplete = { roomId ->
                    // Handle successful pairing - navigate to main app
                    println("Pairing complete! Room ID: $roomId")
                }
            )
        }
        is AuthState.Loading -> {
            // Show loading state
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        else -> {
            println("Showing auth screen, current state: $authState")
            // User not authenticated, show auth screen
            AuthScreen(
                onAuthSuccess = {
                    println("Auth success callback triggered")
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MovieMatcherTheme {
        Greeting("Movie Matcher")
    }
}