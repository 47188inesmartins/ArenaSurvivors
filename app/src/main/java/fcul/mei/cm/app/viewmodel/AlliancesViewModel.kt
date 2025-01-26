package fcul.mei.cm.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.database.AlliancesRepository
import fcul.mei.cm.app.domain.Alliances
import fcul.mei.cm.app.domain.Message
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.CollectionPath
import fcul.mei.cm.app.utils.CollectionPath.ALLIANCES
import fcul.mei.cm.app.utils.CollectionPath.CHAT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlliancesViewModel(
    val user: UserViewModel
) : ViewModel() {

    private val chatRepository: AlliancesRepository = AlliancesRepository()

    fun createChat(chatName: String, description: String, onComplete: (Boolean) -> Unit) {
        if (user.getUserId() != null) {
            user.getUserId()?.let { userId ->
                chatRepository.createChat(chatName, userId, description) { result ->
                    if (result) {
                        Log.d("Chat", "Chat was created!")
                    } else {
                        Log.w("Chat", "Chat was not created!")
                    }
                    onComplete(result) // Notify the caller of the result
                }
            }
        } else {
            onComplete(false) // Notify failure if userId is null
        }
    }

    fun getAllChats() = chatRepository.getAllChats()
    fun getAllChatsExcludingUser(userId: String) = chatRepository.getAllChatsExcludingUser(userId = userId)

    fun addMember(
        chatName: String,
        id: String,
        memberName: String,
        status: String,
    ){
        chatRepository.addMember(
            chatName = chatName,
            memberId = id,
            memberName = memberName,
            status = status,
            onComplete = {},
            district = 1
        )
    }

//    fun sendMessageToFireStore(allianceId: String, messageText: String, sender: String) {
//        val db = Firebase.firestore
//        val chatRef = db.collection(ALLIANCES)
//            .document(allianceId)
//            .collection(CHAT)
//
//        val message = Message(
//            id = chatRef.document().id,
//            sender = sender,
//            text = messageText,
//            timestamp = System.currentTimeMillis()
//        )
//
//        chatRef.add(message)
//            .addOnSuccessListener {
//                Log.d("FireStore", "MESSAGE SENT")
//            }
//            .addOnFailureListener { error ->
//                Log.e("FireStore", "FAILED TO SEND MESSAGE: ${error.message}")
//            }
//    }

    fun getDistrictMembers(district: Int, callback: (List<User>) -> Unit) {
        return user.getUsersFromDistrict(district) { members ->
            callback(members)
        }
    }

    fun sendMessageToFireStore(allianceId: String, messageText: String, sender: String) {
        val db = FirebaseFirestore.getInstance()

        // Reference to the chat document
        val chatRef = db.collection(CollectionPath.CHATS).document(allianceId)

        // Create a message map
        val message = mapOf(
            "id" to System.currentTimeMillis().toString(), // Generate a unique ID using timestamp
            "sender" to sender,
            "text" to messageText,
            "timestamp" to System.currentTimeMillis()
        )

        // Append the message to the "messages" field
        chatRef.update("messages", FieldValue.arrayUnion(message))
            .addOnSuccessListener {
                Log.d("FireStore", "Message added to the messages field successfully!")
            }
            .addOnFailureListener { error ->
                Log.e("FireStore", "Failed to add message to messages field: ${error.message}")
            }
    }


    fun getUsersFromAlliance(allianceId: String, callback: (List<User>) -> Unit) {
        // Step 1: Fetch member IDs from the chat document
        chatRepository.getMembersFromChat(allianceId) { memberIds ->
            // Step 2: Fetch user data using the member IDs
            chatRepository.getUsersByIds(memberIds) { allUsers ->
                // Return the full list of users (with all fields) to the callback
                callback(allUsers)
            }
        }
    }

    fun getMessagesFromAlliance(allianceId: String, callback: (List<Message>) -> Unit) {
        return chatRepository.getMessagesFromChat(allianceId) { message ->
            callback(message)
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val _memberRequests = MutableStateFlow<List<User>>(emptyList())
    val memberRequests: StateFlow<List<User>> = _memberRequests

    fun loadMemberRequests(chatName: String) {
        db.collection(CollectionPath.CHATS).document(chatName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error fetching requests: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val requestIds = snapshot.get("memberRequest") as? List<String> ?: emptyList()
                    Log.d("ids",requestIds.toString())
                    // Fetch user details for each ID
                    fetchUserDetails(requestIds)
                }
            }
    }

    private fun fetchUserDetails(userIds: List<String>) {
        val userDetails = mutableListOf<User>()

        userIds.forEach { userId ->
            db.collection(CollectionPath.USERS).document(userId)
                .get()
                .addOnSuccessListener { document ->
                    document?.toObject(User::class.java)?.let { user ->
                        userDetails.add(user)
                        if (userDetails.size == userIds.size) {
                            _memberRequests.value = userDetails
                            Log.d("users", userDetails.toString())
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("ChatViewModel", "Error fetching user details: ${error.message}")
                }
        }
        Log.d("users", userDetails.toString())
        _memberRequests.value = userDetails
    }

    fun addMemberToChat(chatName: String, user: User, onComplete: () -> Unit) {
        db.collection(CollectionPath.CHATS).document(chatName)
            .update("members", FieldValue.arrayUnion(user.id))
            .addOnSuccessListener {
                removeFromRequests(chatName, user, onComplete)
            }
            .addOnFailureListener { error ->
                Log.e("ChatViewModel", "Error adding member: ${error.message}")
            }
    }

    fun removeFromRequests(chatName: String, user: User, onComplete: () -> Unit) {
        db.collection(CollectionPath.CHATS).document(chatName)
            .update("memberRequest", FieldValue.arrayRemove(user.id))
            .addOnSuccessListener {  _memberRequests.value = _memberRequests.value.filter { it.id != user.id } }
            .addOnFailureListener { error ->
                Log.e("ChatViewModel", "Error removing request: ${error.message}")
            }
    }

    private val _ownedAlliances = MutableStateFlow<List<Alliances>>(emptyList())
    val ownedAlliances: StateFlow<List<Alliances>> = _ownedAlliances

    fun loadOwnedAlliances(userId: String) {
        db.collection(CollectionPath.CHATS)
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AlliancesViewModel", "Error fetching alliances: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alliances = snapshot.documents.mapNotNull { document ->
                        document.toObject(Alliances::class.java)?.copy(chatName = document.id)
                    }
                    _ownedAlliances.value = alliances
                }
            }
    }

    fun getAlliancesById(chatId: String, callback: (List<Alliances>) -> Unit) {
        db.collection(CollectionPath.CHATS)
            .get()
            .addOnSuccessListener { result ->
                val alliances = result.map { document ->
                    document.toObject(Alliances::class.java).copy(id = document.id)
                }.filter { it.chatName == chatId }
                callback(alliances)
                Log.d("Alliances",alliances.toString())
            }
            .addOnFailureListener {
                callback(emptyList())
                Log.d("Alliances","cant get")
            }
    }
}
