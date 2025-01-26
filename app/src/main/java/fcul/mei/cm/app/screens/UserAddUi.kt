package fcul.mei.cm.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.UserViewModel

@Composable
fun AddUserScreen(
    userViewModel: UserViewModel,
    onDone: () -> Unit,
) {
    var district by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }

    val districts = (1..13).map { it.toString() }
    val user = UserSharedPreferences(LocalContext.current)
    if (user.getUserId() == null) {
        if (showConfirmation) {
            Text(
                text = "User added successfully!",
                modifier = Modifier.padding(16.dp),
                color = Color.Green,
                fontSize = 18.sp
            )
            onDone()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (district.isEmpty()) "Select District" else "District: $district")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    districts.forEach { number ->
                        DropdownMenuItem(
                            onClick = {
                                district = number
                                expanded = false
                            },
                            text = { Text(number) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Button(
                onClick = {
                    if (district.isNotEmpty() && name.isNotEmpty()) {
                        userViewModel.addUser(district.toInt(), name)
                        showConfirmation = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Add User")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tribute already registered")
            Spacer(Modifier.size(64.dp))
            Button(
                onClick = {
                    userViewModel.deleteUser()
                    onDone()
                }
            ) {
                Text("SIGN OUT")
            }
        }
    }
}
