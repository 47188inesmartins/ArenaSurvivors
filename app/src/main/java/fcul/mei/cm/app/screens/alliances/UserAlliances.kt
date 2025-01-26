package fcul.mei.cm.app.screens.alliances

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import fcul.mei.cm.app.domain.Alliances
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

val db = FirebaseFirestore.getInstance()

@Composable
fun AlliancesList(userId: String?, context: Context, onNavigateToChat: (String) -> Unit) {
    val alliances = remember { mutableStateListOf<Alliances>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        fetchAlliances(userId, context) { fetchedAlliances ->
            alliances.clear()
            alliances.addAll(fetchedAlliances)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (alliances.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No alliances found.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(alliances) { alliance ->
                AllianceItem(alliance = alliance, onNavigateToChat = onNavigateToChat)
            }
        }
    }
}


@Composable
fun AllianceItem(alliance: Alliances, onNavigateToChat: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = alliance.chatName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alliance.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Members: ${alliance.members.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onNavigateToChat(alliance.chatName) }) {
                Text(text = "Go to Chat")
            }
        }
    }
}

private fun fetchAlliances(userId: String?, context: Context, callback: (List<Alliances>) -> Unit) {
    if (userId != null) {
        db.collection("chats")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { result ->
                val alliances = ArrayList<Alliances>()
                for (document in result) {
                    val alliance = document.toObject(Alliances::class.java)
                    alliances.add(alliance)
                }
                callback(alliances)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to fetch alliances: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

