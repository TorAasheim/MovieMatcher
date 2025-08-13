package com.moviematcher.app.ui.pairing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for creating or joining a room with a partner
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    userId: String,
    onPairingComplete: (roomId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle pairing completion
    LaunchedEffect(uiState.room) {
        uiState.room?.let { room ->
            if (room.userIds.size == 2) {
                onPairingComplete(room.id)
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState.pairingStep) {
            PairingStep.CHOOSE_ACTION -> {
                ChooseActionContent(
                    onCreateRoom = { viewModel.setPairingStep(PairingStep.CREATE_ROOM) },
                    onJoinRoom = { viewModel.setPairingStep(PairingStep.JOIN_ROOM) }
                )
            }
            
            PairingStep.CREATE_ROOM -> {
                CreateRoomContent(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onCreateRoom = { viewModel.createRoom(userId) },
                    onBack = { viewModel.setPairingStep(PairingStep.CHOOSE_ACTION) }
                )
            }
            
            PairingStep.JOIN_ROOM -> {
                JoinRoomContent(
                    inviteCodeInput = uiState.inviteCodeInput,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onInviteCodeChange = viewModel::updateInviteCodeInput,
                    onJoinRoom = { viewModel.joinRoom(userId, uiState.inviteCodeInput) },
                    onBack = { viewModel.setPairingStep(PairingStep.CHOOSE_ACTION) }
                )
            }
            
            PairingStep.ROOM_CREATED -> {
                RoomCreatedContent(
                    inviteCode = uiState.inviteCode ?: "",
                    roomUserCount = uiState.room?.userIds?.size ?: 0
                )
            }
            
            PairingStep.ROOM_JOINED -> {
                RoomJoinedContent(
                    roomUserCount = uiState.room?.userIds?.size ?: 0
                )
            }
            
            PairingStep.PAIRING_COMPLETE -> {
                // This state is handled by LaunchedEffect above
            }
        }
    }
}

@Composable
private fun ChooseActionContent(
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Text(
        text = "Let's find movies together!",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 32.dp)
    )
    
    Text(
        text = "Choose how you'd like to start:",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    
    Button(
        onClick = onCreateRoom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text("Create a Room")
    }
    
    OutlinedButton(
        onClick = onJoinRoom,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Join a Room")
    }
}

@Composable
private fun CreateRoomContent(
    isLoading: Boolean,
    error: String?,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        text = "Create a Room",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Text(
        text = "Create a room and share the invite code with your partner.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    
    if (error != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
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
    
    Button(
        onClick = onCreateRoom,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Create Room")
        }
    }
    
    TextButton(onClick = onBack) {
        Text("Back")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomContent(
    inviteCodeInput: String,
    isLoading: Boolean,
    error: String?,
    onInviteCodeChange: (String) -> Unit,
    onJoinRoom: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        text = "Join a Room",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Text(
        text = "Enter the invite code your partner shared with you.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    
    OutlinedTextField(
        value = inviteCodeInput,
        onValueChange = onInviteCodeChange,
        label = { Text("Invite Code") },
        placeholder = { Text("BAR-TOK") },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )
    
    if (error != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
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
    
    Button(
        onClick = onJoinRoom,
        enabled = !isLoading && inviteCodeInput.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Join Room")
        }
    }
    
    TextButton(onClick = onBack) {
        Text("Back")
    }
}

@Composable
private fun RoomCreatedContent(
    inviteCode: String,
    roomUserCount: Int
) {
    Text(
        text = "Room Created!",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Text(
        text = "Share this invite code with your partner:",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = inviteCode,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
    
    if (roomUserCount < 2) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Waiting for your partner to join...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        Text(
            text = "Your partner has joined! Starting the app...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoomJoinedContent(
    roomUserCount: Int
) {
    Text(
        text = "Joined Room!",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    if (roomUserCount < 2) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Waiting for the room creator...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        Text(
            text = "Both users are ready! Starting the app...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}