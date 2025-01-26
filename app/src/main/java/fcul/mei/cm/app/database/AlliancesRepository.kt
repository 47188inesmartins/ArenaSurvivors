package fcul.mei.cm.app.database

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.R
import fcul.mei.cm.app.domain.Alliances
import fcul.mei.cm.app.domain.Message
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.CollectionPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AlliancesRepository {
    private val db = Firebase.firestore

    fun createChat(
        chatName: String,
        ownerId: String,
        description: String,
        onComplete: (Boolean) -> Unit
    ) {
        val chatRef = db.collection(CollectionPath.CHATS).document(chatName)

        // Step 1: Check if a chat with the same name already exists
        chatRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Chat with the same name already exists
                    println("Chat with name $chatName already exists")
                    onComplete(false)
                } else {
                    // Proceed to create the chat if it doesn't exist
                    val chatData = hashMapOf(
                        "chatName" to chatName,
                        "ownerId" to ownerId,
                        "description" to description,
                        "creationTime" to System.currentTimeMillis(),
                        "members" to listOf(ownerId)
                    )

                    // Step 2: Create the chat document in the "CHATS" collection
                    chatRef.set(chatData)
                        .addOnSuccessListener {
                            // Step 3: Update the user's "alliances" field to include the new chat
                            val userRef = db.collection(CollectionPath.USERS).document(ownerId)

                            userRef.update("alliances", FieldValue.arrayUnion(chatName))
                                .addOnSuccessListener {
                                    println("User's alliances updated successfully")
                                    onComplete(true)
                                }
                                .addOnFailureListener { e ->
                                    println("Error updating user's alliances: ${e.message}")
                                    onComplete(false)
                                }
                        }
                        .addOnFailureListener { e ->
                            println("Error creating chat: ${e.message}")
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                println("Error checking for existing chat: ${e.message}")
                onComplete(false)
            }
    }



    fun getAllChats(): Flow<List<Alliances>> = flow {
        try {
            val initialList = db.collection(CollectionPath.CHATS)
                .get()
                .await()
                .map { document -> document.toObject(Alliances::class.java) }
            emit(initialList)
        } catch (e: Exception) {
            emit(emptyList())
            e.printStackTrace()
        }
    }

    fun getAllChatsExcludingUser(userId: String): Flow<List<Alliances>> = flow {
        try {
            val filteredList = db.collection(CollectionPath.CHATS)
                .get()
                .await()
                .map { document -> document.toObject(Alliances::class.java) }
                .filter { chat ->
                    !chat.members.contains(userId) && // Exclude chats where userId is a member
                            !chat.chatName.matches(Regex("^district (1[0-3]|[1-9])$")) // Exclude names "district 1" through "district 13"
                }
            emit(filteredList)
        } catch (e: Exception) {
            emit(emptyList()) // Emit an empty list in case of an error
            e.printStackTrace()
        }
    }

    fun getAllMessagesFromAlliance(allianceId: String, callback: (List<Message>) -> Unit) {
        db.collection(CollectionPath.ALLIANCES)
            .document(allianceId)
            .collection(CollectionPath.CHAT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FireStore", "ERROR: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { document ->
                        document.toObject(Message::class.java)
                    }
                    callback(messages)
                }
            }
    }

    fun getMessagesFromChat(chatName: String, callback: (List<Message>) -> Unit) {
        db.collection(CollectionPath.CHATS)
            .whereEqualTo("chatName", chatName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.documents.isNotEmpty()) {
                    // Get the first document that matches the chatName
                    val chatDocument = snapshot.documents[0]

                    // Extract the "messages" field, which is an array
                    val messagesData = chatDocument.get("messages") as? List<Map<String, Any>>

                    if (messagesData != null) {
                        // Map each entry in the "messages" array to the Message class
                        val messages = messagesData.mapNotNull { messageData ->
                            try {
                                // Convert each message data to a Message object
                                Message(
                                    id = messageData["id"] as? String ?: "",
                                    sender = messageData["sender"] as? String ?: "",
                                    text = messageData["text"] as? String ?: "",
                                    timestamp = messageData["timestamp"] as? Long ?: 0L
                                )
                            } catch (e: Exception) {
                                Log.e("getMessagesFromChat", "Error parsing message: ${e.message}")
                                null
                            }
                        }
                        callback(messages) // Return the list of messages
                    } else {
                        Log.w("Firestore", "No messages found for chat: $chatName")
                        callback(emptyList())
                    }
                } else {
                    Log.w("Firestore", "No chat found with name: $chatName")
                    callback(emptyList())
                }
            }
            .addOnFailureListener { error ->
                Log.e("Firestore", "Error fetching chat messages: ${error.message}")
                callback(emptyList())
            }
    }

    fun getUsersByIds(memberIds: List<String>, callback: (List<User>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usersList = mutableListOf<User>()

        if (memberIds.isEmpty()) {
            callback(emptyList()) // Return empty if no member IDs
            return
        }

        // Fetch each user by ID
        memberIds.forEach { userId ->
            db.collection(CollectionPath.USERS).document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Map the document to the User object
                        val user = document.toObject(User::class.java)
                        user?.let { usersList.add(it) }

                        // When all users are fetched, call the callback
                        if (usersList.size == memberIds.size) {
                            callback(usersList)
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("Firestore", "Error fetching user data: ${error.message}")
                }
        }

        // Handle case where memberIds is empty
        if (memberIds.isEmpty()) {
            callback(emptyList())
        }
    }

    fun getMemberNamesFromChatByChatName(chatName: String, callback: (List<String>) -> Unit) {
        // Step 1: Fetch member IDs from the chat document
        getMembersFromChat(chatName) { memberIds ->
            // Step 2: Fetch member names using the member IDs
            getMemberNamesFromChat(memberIds) { memberNames ->
                callback(memberNames)
            }
        }
    }

    fun getMembersFromChat(chatName: String, callback: (List<String>) -> Unit) {
        db.collection(CollectionPath.CHATS)
            .whereEqualTo("chatName", chatName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null) {
                    // Get the first document that matches the chatName
                    val chatDocument = querySnapshot.documents[0]
                    // Extract the "members" field
                    val members = chatDocument.get("members") as List<String>

                    callback(members)
                }
            }
            .addOnFailureListener { error ->
                Log.e("FireStore", "Error fetching chat members: ${error.message}")
                callback(emptyList())
            }
    }

    fun getMemberNamesFromChat(memberIds: List<String>, callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userNames = mutableListOf<String>()

        if (memberIds.isEmpty()) {
            callback(emptyList())
            return
        }

        // Fetch each user by ID
        memberIds.forEach { userId ->
            db.collection(CollectionPath.USERS).document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("name") ?: "Unknown"
                        userNames.add(userName)

                        // When all members are fetched, call the callback
                        if (userNames.size == memberIds.size) {
                            callback(userNames)
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("Firestore", "Error fetching user data: ${error.message}")
                }
        }
    }

    fun getAllAllianceMembers(allianceId: String) = flow {
        try {
            val members = db.collection(CollectionPath.ALLIANCES)
                .document(allianceId)
                .collection(CollectionPath.PARTICIPANTS)
                .get()
                .await()
                .map { document -> document.toObject(User::class.java) }
            emit(members)
        } catch (e: Exception) {
            emit(emptyList())
            e.printStackTrace()
        }
    }

    fun getAllMembersHealthUse(chatName: String) = flow {
        try {
            val members = db.collection(CollectionPath.CHATS).document(chatName)
                .collection(CollectionPath.PARTICIPANTS)
                .get()
                .await()
                .map { document -> document.toObject(User::class.java) }
            emit(members)
        } catch (e: Exception) {
            emit(emptyList())
            e.printStackTrace()
        }
    }

    fun addMember(
        chatName: String,
        memberId: String,
        memberName: String,
        district: Int,
        status: String = "pending",
        onComplete: (Boolean) -> Unit
    ) {
        val memberData = hashMapOf(
            "id" to memberId,
            "district" to district,
            "role" to "member",
            "name" to memberName,
            "status" to status,
            "joinedAt" to System.currentTimeMillis()
        )

        db.collection(CollectionPath.CHATS).document(chatName)
            .collection(CollectionPath.PARTICIPANTS).document(memberId)
            .set(memberData)
            .addOnSuccessListener {
                Log.d("ALLIANCES", "Member added successfully with status: $status")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.d("ALLIANCES", "Error adding member: ${e.message}")
                onComplete(false)
            }
    }

    fun requestToJoinAlliance(chatName: String, userId: String, onComplete: (Boolean) -> Unit) {
        val requestField = "memberRequest"
        db.collection(CollectionPath.CHATS).document(chatName)
            .update(requestField, FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Log.d("ALLIANCES", "User $userId added to requestedMembers list successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.d("ALLIANCES", "Error adding user to requestedMembers: ${e.message}")
                onComplete(false)
            }
    }

    fun listenForOwnedChats(userId: String, context: Context) {
        val db = FirebaseFirestore.getInstance()

        // Query chats where the user is the owner
        db.collection(CollectionPath.CHATS)
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ALLIANCES", "Error fetching owned chats: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (document in snapshot.documents) {
                        val chatName = document.getString("chatName") ?: continue
                        listenForJoinRequests(chatName, context)
                    }
                }
            }
    }

    private val localRequestedMembersCache = mutableMapOf<String, List<String>>()

    fun listenForJoinRequests(chatName: String, context: Context) {
        val db = FirebaseFirestore.getInstance()

        // Ensure the cache is initialized for this chat
        if (!localRequestedMembersCache.containsKey(chatName)) {
            localRequestedMembersCache[chatName] = emptyList()
        }

        db.collection(CollectionPath.CHATS).document(chatName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ALLIANCES", "Error listening for join requests: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val requestedMembers =
                        snapshot.get("memberRequest") as? List<String> ?: emptyList()

                    // Get the current cache for this chat
                    val localRequestedMembers = localRequestedMembersCache[chatName] ?: emptyList()

                    // Find newly added members
                    val newRequests = requestedMembers - localRequestedMembers

                    if (newRequests.isNotEmpty()) {
                        // Trigger the notification
                        showNotification(
                            context,
                            "New join request for $chatName",
                            "There are users waiting to join your alliance."
                        )

                        // Update the cache with the current state of requestedMembers
                        localRequestedMembersCache[chatName] = requestedMembers
                    }
                }
            }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "join_request_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Join Requests",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.notif_icon_old)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun fetchAlliances(userId: String?, context: Context, callback: (List<Alliances>) -> Unit) {
        if (userId != null) {
            fcul.mei.cm.app.screens.alliances.db.collection("chats")
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
                    Toast.makeText(
                        context,
                        "Failed to fetch alliances: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
    fun fetchMembersFromAllAlliances(
        userId: String?,
        context: Context,
        callback: (List<Map<String, Any>>) -> Unit
    ) {
        if (userId == null) {
            Toast.makeText(context, "User ID is null", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch all alliances for the user
        fetchAlliances(userId, context) { alliances ->
            val db = FirebaseFirestore.getInstance()
            val allMembersDetails = mutableListOf<Map<String, Any>>()
            var remainingCalls = alliances.size

            if (alliances.isEmpty()) {
                callback(emptyList())
                return@fetchAlliances
            }

            // For each alliance, fetch the members
            alliances.forEach { alliance ->
                db.collection(CollectionPath.CHATS).document(alliance.chatName)
                    .get()
                    .addOnSuccessListener { chatDocument ->
                        val memberIds = chatDocument.get("members") as? List<String> ?: emptyList()

                        if (memberIds.isEmpty()) {
                            // If no members, check if all alliances are processed
                            if (--remainingCalls == 0) {
                                callback(allMembersDetails)
                            }
                            return@addOnSuccessListener
                        }

                        // Fetch details for each member
                        memberIds.forEach { memberId ->
                            db.collection(CollectionPath.USERS).document(memberId)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument.exists()) {
                                        val userDetails = userDocument.data ?: emptyMap()
                                        allMembersDetails.add(userDetails)

                                        // If all members of all alliances are fetched, call the callback
                                        if (allMembersDetails.size == memberIds.size * alliances.size) {
                                            callback(allMembersDetails)
                                        }
                                    }
                                }
                                .addOnFailureListener { error ->
                                    Log.e("Firestore", "Error fetching user details: ${error.message}")
                                }
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e("Firestore", "Error fetching chat document: ${error.message}")
                    }
            }
        }
    }


    fun isUserInMemberRequests(chatName: String, userId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("chats").document(chatName)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val memberRequests = document.get("memberRequest") as? List<String> ?: emptyList()
                    callback(memberRequests.contains(userId))
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Failed to fetch member requests: ${exception.message}")
                callback(false)
            }
    }
}