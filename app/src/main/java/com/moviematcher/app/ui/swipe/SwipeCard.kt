package com.moviematcher.app.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviematcher.app.R
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.SwipeDecision
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tinder-style swipe card for movies with drag gestures and animations
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeCard(
    movie: Movie,
    streamingProviders: List<StreamingProvider> = emptyList(),
    onSwipe: (SwipeDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidth * 0.3f
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Calculate rotation based on horizontal offset
    val rotationAngle = (offsetX / screenWidth) * 30f
    
    // Calculate alpha for overlay indicators
    val swipeProgress = abs(offsetX) / swipeThreshold
    val overlayAlpha = (swipeProgress * 0.8f).coerceAtMost(0.8f)
    
    LaunchedEffect(offsetX) {
        rotation.animateTo(rotationAngle, animationSpec = tween(100))
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .rotate(rotation.value)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    offsetX > swipeThreshold -> {
                                        // Swipe right - Like
                                        onSwipe(SwipeDecision.LIKE)
                                    }
                                    offsetX < -swipeThreshold -> {
                                        // Swipe left - Pass
                                        onSwipe(SwipeDecision.PASS)
                                    }
                                    else -> {
                                        // Snap back to center
                                        offsetX = 0f
                                        offsetY = 0f
                                        rotation.animateTo(0f, animationSpec = tween(300))
                                        scale.animateTo(1f, animationSpec = tween(300))
                                    }
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        
                        // Add slight scale effect when dragging
                        coroutineScope.launch {
                            val scaleValue = 1f - (abs(offsetX) / screenWidth) * 0.1f
                            scale.animateTo(scaleValue.coerceAtLeast(0.9f), animationSpec = tween(100))
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Movie poster background
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                    contentDescription = "${movie.title} poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_movie_placeholder),
                    placeholder = painterResource(id = R.drawable.ic_movie_placeholder)
                )
                
                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 0.5f
                            )
                        )
                )
                
                // Like/Pass overlay indicators
                if (abs(offsetX) > swipeThreshold * 0.3f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (offsetX > 0) Color.Green.copy(alpha = overlayAlpha * 0.3f)
                                else Color.Red.copy(alpha = overlayAlpha * 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = if (offsetX > 0) Color.Green.copy(alpha = overlayAlpha)
                                   else Color.Red.copy(alpha = overlayAlpha)
                        ) {
                            Icon(
                                imageVector = if (offsetX > 0) Icons.Default.Favorite else Icons.Default.Close,
                                contentDescription = if (offsetX > 0) "Like" else "Pass",
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Movie information at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Streaming providers
                    if (streamingProviders.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            streamingProviders.take(4).forEach { provider ->
                                StreamingProviderBadge(provider = provider)
                            }
                            if (streamingProviders.size > 4) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "+${streamingProviders.size - 4}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Movie title
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Year and rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Release year
                        movie.releaseDate?.let { releaseDate ->
                            val year = releaseDate.substring(0, 4)
                            Text(
                                text = year,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        if (movie.releaseDate != null && movie.voteAverage > 0) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Rating
                        if (movie.voteAverage > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⭐",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", movie.voteAverage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                    
                    // Runtime if available
                    movie.runtime?.let { runtime ->
                        if (runtime > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${runtime}min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        // Action buttons at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pass button
            FloatingActionButton(
                onClick = { onSwipe(SwipeDecision.PASS) },
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Pass",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Like button
            FloatingActionButton(
                onClick = { onSwipe(SwipeDecision.LIKE) },
                modifier = Modifier.size(56.dp),
                containerColor = Color(0xFF4CAF50), // Green
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Badge component for displaying streaming providers
 */
@Composable
private fun StreamingProviderBadge(
    provider: StreamingProvider,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider logo (if available)
            provider.logoPath?.let { logoPath ->
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w92$logoPath",
                    contentDescription = "${provider.name} logo",
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            // Provider name
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
/**
 *
 Preview composable for SwipeCard
 */
@Composable
fun SwipeCardPreview() {
    val sampleMovie = Movie(
        id = 1L,
        title = "The Matrix",
        overview = "A computer programmer is led to fight an underground war against powerful computers who have constructed his entire reality with a system called the Matrix.",
        posterPath = "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
        releaseDate = "1999-03-30",
        voteAverage = 8.7,
        genres = emptyList(),
        runtime = 136
    )
    
    val sampleProviders = listOf(
        StreamingProvider(8, "Netflix", "/t2yyOv40HZeVlLjYsCsPHnWLk4W.jpg", "netflix://"),
        StreamingProvider(337, "Disney+", "/7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg", "disneyplus://"),
        StreamingProvider(384, "HBO Max", "/Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg", "hbomax://")
    )
    
    MaterialTheme {
        SwipeCard(
            movie = sampleMovie,
            streamingProviders = sampleProviders,
            onSwipe = { decision ->
                // Preview - no action
            }
        )
    }
}