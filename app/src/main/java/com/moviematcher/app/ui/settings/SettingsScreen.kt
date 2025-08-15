package com.moviematcher.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moviematcher.app.data.model.ContentType
import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.model.UserPreferences

/**
 * Settings screen for managing user preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val availableProviders by viewModel.availableProviders.collectAsStateWithLifecycle()
    
    // Initialize for room when roomId changes
    LaunchedEffect(roomId) {
        viewModel.initializeForRoom(roomId)
    }
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you'd show a snackbar here
            // For now, we'll just clear the error after showing it
            viewModel.clearError()
        }
    }
    
    // Show cleanup result
    uiState.cleanupResult?.let { result ->
        LaunchedEffect(result) {
            // In a real app, you'd show a snackbar here
            // For now, we'll just clear the result after showing it
            viewModel.clearCleanupResult()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetToDefaults() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            preferences?.let { prefs ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Content Type Section
                    item {
                        ContentTypeSection(
                            selectedContentType = prefs.contentType,
                            onContentTypeSelected = viewModel::updateContentType,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Genres Section
                    item {
                        GenresSection(
                            availableGenres = availableGenres,
                            selectedGenres = prefs.selectedGenres,
                            onGenreToggled = viewModel::toggleGenre,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Year Range Section
                    item {
                        YearRangeSection(
                            yearRange = prefs.yearRange,
                            onYearRangeChanged = viewModel::updateYearRange,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Rating Section
                    item {
                        RatingSection(
                            minRating = prefs.minRating,
                            onRatingChanged = viewModel::updateMinRating,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Streaming Providers Section
                    item {
                        StreamingProvidersSection(
                            availableProviders = availableProviders,
                            selectedProviders = prefs.selectedProviders,
                            onProviderToggled = viewModel::toggleProvider,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Availability Strict Section
                    item {
                        AvailabilityStrictSection(
                            availabilityStrict = prefs.availabilityStrict,
                            onAvailabilityStrictChanged = viewModel::updateAvailabilityStrict,
                            enabled = !uiState.isSaving
                        )
                    }
                    
                    // Data Management Section
                    item {
                        DataManagementSection(
                            onClearSwipes = viewModel::clearMySwipes,
                            enabled = !uiState.isSaving
                        )
                    }
                }
            }
        }
        
        // Show loading overlay when saving
        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Saving preferences...")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentTypeSection(
    selectedContentType: ContentType,
    onContentTypeSelected: (ContentType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Content Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            ContentType.values().forEach { contentType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedContentType == contentType,
                            onClick = { onContentTypeSelected(contentType) },
                            role = Role.RadioButton,
                            enabled = enabled
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContentType == contentType,
                        onClick = null,
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (contentType) {
                            ContentType.MOVIE -> "Movies Only"
                            ContentType.TV -> "TV Shows Only"
                            ContentType.BOTH -> "Movies & TV Shows"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenresSection(
    availableGenres: List<Genre>,
    selectedGenres: Set<Int>,
    onGenreToggled: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select genres you're interested in (leave empty for all genres)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableGenres) { genre ->
                FilterChip(
                    onClick = { onGenreToggled(genre.id) },
                    label = { Text(genre.name) },
                    selected = selectedGenres.contains(genre.id),
                    enabled = enabled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearRangeSection(
    yearRange: IntRange,
    onYearRangeChanged: (IntRange) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var startYear by remember(yearRange) { mutableFloatStateOf(yearRange.first.toFloat()) }
    var endYear by remember(yearRange) { mutableFloatStateOf(yearRange.last.toFloat()) }
    
    Column(modifier = modifier) {
        Text(
            text = "Release Year Range",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${startYear.toInt()} - ${endYear.toInt()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Start Year Slider
        Text(
            text = "From: ${startYear.toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = startYear,
            onValueChange = { 
                startYear = it
                if (it <= endYear) {
                    onYearRangeChanged(it.toInt()..endYear.toInt())
                }
            },
            valueRange = 1900f..2024f,
            steps = 123, // 2024 - 1900 - 1
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // End Year Slider
        Text(
            text = "To: ${endYear.toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = endYear,
            onValueChange = { 
                endYear = it
                if (it >= startYear) {
                    onYearRangeChanged(startYear.toInt()..it.toInt())
                }
            },
            valueRange = 1900f..2024f,
            steps = 123, // 2024 - 1900 - 1
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingSection(
    minRating: Double,
    onRatingChanged: (Double) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Minimum Rating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${String.format("%.1f", minRating)}+ stars",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Slider(
            value = minRating.toFloat(),
            onValueChange = { onRatingChanged(it.toDouble()) },
            valueRange = 0f..10f,
            steps = 19, // 0.5 increments
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingProvidersSection(
    availableProviders: List<StreamingProvider>,
    selectedProviders: Set<Int>,
    onProviderToggled: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Streaming Providers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select your streaming services (leave empty to show all)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableProviders) { provider ->
                FilterChip(
                    onClick = { onProviderToggled(provider.id) },
                    label = { Text(provider.name) },
                    selected = selectedProviders.contains(provider.id),
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun AvailabilityStrictSection(
    availabilityStrict: Boolean,
    onAvailabilityStrictChanged: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Strict Availability",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (availabilityStrict) {
                        "Only show movies available on selected providers"
                    } else {
                        "Show all movies, prioritize selected providers"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = availabilityStrict,
                onCheckedChange = onAvailabilityStrictChanged,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun DataManagementSection(
    onClearSwipes: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manage your swipe history and data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = { showConfirmDialog = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear My Swipes")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This will delete all your swipe history in this room. Matches will be preserved unless your partner also clears their swipes. This action cannot be undone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Clear All Swipes?")
            },
            text = {
                Text(
                    "This will permanently delete all your swipe history in this room. " +
                    "Matches may be removed if your partner has also cleared their swipes. " +
                    "This action cannot be undone.\n\n" +
                    "Are you sure you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearSwipes()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Swipes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}