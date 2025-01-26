package fcul.mei.cm.app.screens.alliances

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.R
import fcul.mei.cm.app.domain.Message
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.AlliancesViewModel



@Composable
fun ChatTemplate(
    viewModel: AlliancesViewModel,
    modifier: Modifier = Modifier,
    chatName: String
) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var isMemberPanelVisible by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentAllianceId by remember { mutableStateOf("1") }
    val context = LocalContext.current

    val userId = UserSharedPreferences(context).getUserId()

    if (userId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "You need to register first",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Gray
            )
        }
        return
    }

    // Fetch messages and users when the allianceId or chatName changes
    LaunchedEffect(currentAllianceId, chatName) {
        viewModel.getUsersFromAlliance(chatName) { allUsers ->
            members = allUsers
        }
        viewModel.getMessagesFromAlliance(chatName) { allianceMessages ->
            messages = allianceMessages
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { isMemberPanelVisible = true }) {
                    Text("Members")
                }
            }
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.start_talking), color = Color.Gray)
                }
            } else {

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true
                ) {
                    items(messages.sortedByDescending { it.timestamp }) { message ->
                        if (userId == message.sender) {
                            MessageBubbleCurrentUser(
                                sender = members.firstOrNull { member ->
                                    member.id == message.sender
                                }?.name ?: stringResource(R.string.unknown_user),
                                message = message.text
                            )
                        } else {
                            MessageBubbleOtherUser(
                                sender = members.firstOrNull { member ->
                                    member.id == message.sender
                                }?.name ?: stringResource(R.string.unknown_user),
                                message = message.text
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        if (inputText.text.isEmpty()) {
                            Text(stringResource(R.string.type_message), color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = userId != null,
                    onClick = {
                        if (inputText.text.isNotBlank()) {
                            viewModel.sendMessageToFireStore(
                                chatName,
                                inputText.text,
                                userId.toString()
                            )
                            inputText = TextFieldValue("") // Reset input field after sending

                            // Refresh messages after sending a message
                            viewModel.getMessagesFromAlliance(chatName) { updatedMessages ->
                                messages = updatedMessages
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.send_message))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


        }

        AnimatedVisibility(
            visible = isMemberPanelVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(
                        Color.LightGray,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .align(Alignment.CenterEnd)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isMemberPanelVisible = false
                            },
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Text("X")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn {
                        items(members) { member ->
                            UsersCard(member)
                        }
                    }
                }
            }
        }
    }
}