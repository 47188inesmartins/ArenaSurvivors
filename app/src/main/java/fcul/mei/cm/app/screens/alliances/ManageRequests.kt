package fcul.mei.cm.app.screens.alliances

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fcul.mei.cm.app.domain.Alliances
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.viewmodel.AlliancesViewModel

@Composable
fun ManageJoinRequestsScreen(
    chatName: String,
    viewModel: AlliancesViewModel,
    onMemberUpdated: () -> Unit
) {
    val memberRequests by viewModel.memberRequests.collectAsState(initial = emptyList())
    val context = LocalContext.current

    LaunchedEffect(chatName) {
        viewModel.loadMemberRequests(chatName)
    }

    Log.d("requests", memberRequests.toString())



        if (memberRequests.isEmpty()) {
            // Display message when there are no requests
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "There are no requests to join this alliance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Requests to Join your Alliance $chatName",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Display the list of requests
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Iterate through the list
                items(memberRequests) { user ->
                    JoinRequestItem(
                        user = user,
                        onAccept = {
                            viewModel.addMemberToChat(chatName, user) {
                                Toast.makeText(
                                    context,
                                    "${user.name} added to members!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onMemberUpdated()
                            }
                        },
                        onReject = {
                            viewModel.removeFromRequests(chatName, user) {
                                Toast.makeText(
                                    context,
                                    "${user.name} removed from requests!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onMemberUpdated()
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun JoinRequestItem(
    user: User, // Replace `User` with your data model
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = user.name, // Display user name
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAccept) {
                Text("Accept")
            }
            OutlinedButton(onClick = onReject) {
                Text("Reject")
            }
        }
    }
}

@Composable
fun ManageAlliancesScreen(
    viewModel: AlliancesViewModel,
    userId: String,
    onAllianceSelected: (String) -> Unit // Callback when an alliance is selected
) {
    val alliances by viewModel.ownedAlliances.collectAsState(initial = emptyList())

    LaunchedEffect(userId) {
        viewModel.loadOwnedAlliances(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Alliances",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (alliances.isEmpty()) {
            Text(
                text = "You have no alliances.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                alliances.forEach { alliance ->
                    item {
                        AllianceItem(
                            alliance = alliance,
                            onClick = { onAllianceSelected(alliance.chatName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllianceItem(
    alliance: Alliances,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),

    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = alliance.chatName,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = alliance.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

