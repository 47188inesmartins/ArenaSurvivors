package fcul.mei.cm.app.screens.alliances

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fcul.mei.cm.app.domain.Alliances
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.AlliancesViewModel
import fcul.mei.cm.app.viewmodel.UserViewModel
import fcul.mei.cm.app.database.AlliancesRepository
import fcul.mei.cm.app.screens.map.getUserNameById


@Composable
fun AlliancesScreen(
    modifier: Modifier = Modifier,
    viewModel: AlliancesViewModel,
    onCreateAllianceClick: () -> Unit,
    onManageAlliancesClick: () -> Unit
) {
    var alliances by remember { mutableStateOf<List<Alliances>>(emptyList()) }
    val context = LocalContext.current



    val userId = UserSharedPreferences(context).getUserId()

    LaunchedEffect(Unit) {
        if (userId != null) {
            viewModel.getAllChatsExcludingUser(userId).collect {
                alliances = it
            }
        }
    }
    if (userId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("You need to register first", style = MaterialTheme.typography.headlineMedium, color = Color.Gray)
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp), // Adjust the padding for the entire screen
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add consistent spacing
        ) {
            // Title
            Text(
                text = "Alliances",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Buttons for creating and managing alliances
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateAllianceClick
            ) {
                Text("Create Alliance")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onManageAlliancesClick
            ) {
                Text("Manage My Alliances")
            }

            // List of alliances
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(alliances) { group ->
                    GroupCard(group = group)
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: Alliances) {
    val context = LocalContext.current
    val userViewModel = UserViewModel(context)
    var ownerName by remember { mutableStateOf("Unknown") } // Remember state for owner name
    var isPending by remember { mutableStateOf(false) } // Track if the request is pending

    val chatRepository: AlliancesRepository = AlliancesRepository()
    val userId = userViewModel.getUserId()

    // Fetch the owner name asynchronously
    LaunchedEffect(group.ownerId) {
        getUserNameById(group.ownerId) { name ->
            if (name != null) {
                ownerName = name // Update the state
            } else {
                Log.e("UserName", "User name not found or error occurred")
            }
        }
    }

    // Check if the user is already in the memberRequests list
    LaunchedEffect(group.chatName, userId) {
        if (userId != null) {
            chatRepository.isUserInMemberRequests(group.chatName, userId) { result ->
                isPending = result
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Name: ${group.chatName}",
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Description: ${group.description}",
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Owner: ${ownerName}",
                    color = Color.Gray
                )
            }

            // Display either a button or "Pending" text
            if (isPending) {
                Text(
                    text = "Request Pending",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Button(
                    onClick = {
                        if (userId != null) {
                            chatRepository.requestToJoinAlliance(group.chatName, userId) { success ->
                                if (success) {
                                    isPending = true // Update the state to "Pending"
                                    Log.d("ALLIANCES", "Request sent successfully")
                                } else {
                                    Log.d("ALLIANCES", "Failed to send request")
                                }
                            }
                        }
                    }
                ) {
                    Text("Request to join")
                }
            }
        }
    }
}
