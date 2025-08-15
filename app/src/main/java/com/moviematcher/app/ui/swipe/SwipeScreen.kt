package com.moviematcher.app.ui.swipe

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moviematcher.app.data.model.SwipeDecision

/**
 * Screen for swiping through movie recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    userId: String,
    roomId: String,
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentMovie by viewModel.currentMovie.collectAsStateWithLifecycle()
    val isLoadingRecommendations by viewModel.isLoadingRecommendations.collectAsStateWithLifecycle()
    val recommendationError by viewModel.recommendationError.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle(initialValue = true)
    val pendingSwipesCount by viewModel.pendingSwipesCount.collectAsStateWithLifecycle(initialValue = 0)
    
    // For now, we'll create a simple placeholder screen since we need user preferences
    // and partner information to properly initialize the SwipeViewModel
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Swipe Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Room ID: $roomId",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = "User ID: $userId",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Offline status indicator
        if (!isConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 Offline Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (pendingSwipesCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "($pendingSwipesCount pending)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else if (pendingSwipesCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔄 Syncing $pendingSwipesCount swipes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.forceSyncOfflineData() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (currentMovie != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = currentMovie!!.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentMovie!!.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { 
                        currentMovie?.let { movie ->
                            viewModel.recordSwipe(movie.id, SwipeDecision.PASS)
                        }
                    }
                ) {
                    Text("Pass")
                }
                
                Button(
                    onClick = { 
                        currentMovie?.let { movie ->
                            viewModel.recordSwipe(movie.id, SwipeDecision.LIKE)
                        }
                    }
                ) {
                    Text("Like")
                }
            }
            
            if (viewModel.canUndo()) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { viewModel.undoLastSwipe() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Undo")
                }
            }
        } else {
            Text(
                text = "No movie loaded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.refreshRecommendations() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Movies")
            }
        }
        
        if (isLoadingRecommendations) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        
        recommendationError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}