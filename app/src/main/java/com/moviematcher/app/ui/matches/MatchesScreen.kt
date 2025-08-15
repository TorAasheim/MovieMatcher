package com.moviematcher.app.ui.matches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moviematcher.app.data.model.StreamingProvider
import java.text.SimpleDateFormat
import java.util.*

/**
 * Matches screen showing all matched movies with management options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    roomId: String,
    modifier: Modifier = Modifier,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val enrichedMatches by viewModel.enrichedMatches.collectAsStateWithLifecycle()
    val tonightsPick by viewModel.tonightsPick.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize matches when screen loads
    LaunchedEffect(roomId) {
        viewModel.initializeMatches(roomId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Your Matches",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tonight's Pick Section
        TonightsPickCard(
            enrichedMatch = tonightsPick,
            onRefreshClick = { viewModel.refreshTonightsPick() },
            onProviderClick = { provider, movieTitle ->
                val url = viewModel.openStreamingProvider(provider, movieTitle)
                url?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Error handling
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Matches list
        if (enrichedMatches.isEmpty() && !uiState.isLoading) {
            EmptyMatchesState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(enrichedMatches) { enrichedMatch ->
                    MatchCard(
                        enrichedMatch = enrichedMatch,
                        onProviderClick = { provider ->
                            val url = viewModel.openStreamingProvider(provider, enrichedMatch.movieDetails?.title ?: "")
                            url?.let {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                context.startActivity(intent)
                            }
                        },
                        onMarkWatched = { notes ->
                            viewModel.markAsWatched(enrichedMatch.match.titleId, notes)
                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun MatchCard(
    enrichedMatch: EnrichedMatch,
    onProviderClick: (StreamingProvider) -> Unit,
    onMarkWatched: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        MatchContent(
            enrichedMatch = enrichedMatch,
            onProviderClick = onProviderClick,
            onMarkWatched = onMarkWatched,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun MatchContent(
    enrichedMatch: EnrichedMatch,
    onProviderClick: (StreamingProvider) -> Unit,
    onMarkWatched: (String) -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val movie = enrichedMatch.movieDetails
    val match = enrichedMatch.match
    var showWatchedDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Movie poster
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://image.tmdb.org/t/p/w342${movie?.posterPath}")
                .crossfade(true)
                .build(),
            contentDescription = "${movie?.title} poster",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(80.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Movie details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Title and year
            Text(
                text = movie?.title ?: "Unknown Movie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            movie?.releaseDate?.let { releaseDate ->
                Text(
                    text = releaseDate.take(4), // Extract year
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rating
            movie?.voteAverage?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700), // Gold color
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", rating),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Streaming providers
            if (enrichedMatch.streamingProviders.isNotEmpty()) {
                Text(
                    text = "Available on:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(enrichedMatch.streamingProviders) { provider ->
                        ProviderChip(
                            provider = provider,
                            onClick = { onProviderClick(provider) }
                        )
                    }
                }
            }

            // Watched status and notes
            if (match.watched) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Watched",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (match.notes.isNotEmpty()) {
                    Text(
                        text = match.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // Mark as watched button
                OutlinedButton(
                    onClick = { showWatchedDialog = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Mark as Watched")
                }
            }

            // Match timestamp
            Text(
                text = "Matched ${formatTimestamp(match.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Mark as watched dialog
    if (showWatchedDialog) {
        MarkAsWatchedDialog(
            movieTitle = movie?.title ?: "Unknown Movie",
            onConfirm = { notes ->
                onMarkWatched(notes)
                showWatchedDialog = false
            },
            onDismiss = { showWatchedDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderChip(
    provider: StreamingProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = { Text(provider.name) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkAsWatchedDialog(
    movieTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Watched") },
        text = {
            Column {
                Text("Mark \"$movieTitle\" as watched?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("What did you think?") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes) }) {
                Text("Mark Watched")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyMatchesState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎬",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No matches yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Start swiping to find movies you both love!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

    return when {
        diffInDays == 0L -> "today"
        diffInDays == 1L -> "yesterday"
        diffInDays < 7 -> "$diffInDays days ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}