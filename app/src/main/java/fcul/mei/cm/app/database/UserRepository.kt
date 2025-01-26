package fcul.mei.cm.app.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.domain.Alliances
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.CollectionPath

class UserRepository {
    private val db = Firebase.firestore
    private val alliancesRepository = AlliancesRepository()

    private fun addParticipantToChat(
        chatName: String,
        participantId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val participantData = hashMapOf(
            "id" to participantId,
            "role" to "member",
            "joinedAt" to System.currentTimeMillis()
        )

        db.collection(CollectionPath.CHATS).document(chatName)
            .collection(CollectionPath.PARTICIPANTS).document(participantId)
            .set(participantData)
            .addOnSuccessListener {
                println("Participant added successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                println("Error adding participant: ${e.message}")
                onComplete(false)
            }
    }

    fun removeParticipant(
        chatName: String,
        removerId: String,
        participantId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val participantRef = db.collection(CollectionPath.CHATS).document(chatName)
            .collection(CollectionPath.PARTICIPANTS).document(removerId)

        participantRef.get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getString("role") == "admin") {
                    removeParticipantFromChat(chatName, participantId, onComplete)
                } else {
                    println("Only admins can remove participants")
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                println("Error checking admin status: ${e.message}")
                onComplete(false)
            }
    }

    private fun removeParticipantFromChat(
        chatName: String,
        participantId: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection(CollectionPath.CHATS).document(chatName)
            .collection(CollectionPath.PARTICIPANTS).document(participantId)
            .delete()
            .addOnSuccessListener {
                println("Participant removed successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                println("Error removing participant: ${e.message}")
                onComplete(false)
            }
    }

    fun addUser(user: User, onComplete: (Boolean) -> Unit) {
        val userData = hashMapOf(
            "id" to user.id,
            "district" to user.district,
            "name" to user.name,
            "creationTime" to System.currentTimeMillis()
        )

        db.collection(CollectionPath.USERS).document(user.id)
            .set(userData)
            .addOnSuccessListener {
                println("User added successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                println("Fail to add a user: ${e.message}")
                onComplete(false)
            }
        
        val chatName = "district ${user.district}"
        db.collection(CollectionPath.CHATS).document(chatName)
            .get()
            .addOnSuccessListener { chatDocument ->
                if (chatDocument.exists()) {
                    // Chat already exists, just add the user to it
                    addUserToMembers(
                        chatName = chatName,
                        userId = user.id
                    ) {}
                } else {
                    // Chat does not exist, create a new one
                    alliancesRepository.createChat(
                        chatName = chatName,
                        ownerId = user.id,
                        description = "Same district"
                    ) {
                        // Add the user to the newly created chat
                        addParticipantToChat(
                            chatName = chatName,
                            participantId = user.id
                        ) {}
                    }
                }
            }
            .addOnFailureListener { e ->
                println("Error checking chat existence: ${e.message}")
            }
    }

    private fun addUserToMembers(chatName: String, userId: String, onComplete: (Boolean) -> Unit) {
        val chatDocumentRef = db.collection(CollectionPath.CHATS).document(chatName)

        // Update the 'members' array in the chat document
        chatDocumentRef.update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                println("User successfully added to members array.")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                println("Error adding user to members array: ${e.message}")
                onComplete(false)
            }
    }

    private fun verifyUser(userId: String, callback: (User?) -> Unit) {
        db.collection(CollectionPath.USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(id = document.id)
                    callback(user)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun getUser(userId: String, callback: (User?) -> Unit) {
        db.collection(CollectionPath.USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)?.copy(id = document.id)
                callback(user)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun getAllUsers(callback: (List<User>) -> Unit) {
        db.collection(CollectionPath.USERS)
            .get()
            .addOnSuccessListener { result ->
                val users = result.map { document ->
                    document.toObject(User::class.java).copy(id = document.id)
                }
                callback(users)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getUserFromSameDistrict(districtNumber: Int, callback: (List<User>) -> Unit){
        db.collection(CollectionPath.USERS)
            .get()
            .addOnSuccessListener { result ->
                val users = result.map { document ->
                    document.toObject(User::class.java).copy(id = document.id)
                }.filter { it.district == districtNumber }
                callback(users)
                Log.d("DISTRITO",users.toString())

            }
            .addOnFailureListener {
                callback(emptyList())
                Log.d("DISTRITO","erro")
            }
    }

    fun getUserAlliances(userId: String, callback: (List<Alliances>) -> Unit){
        getUser(userId){ user ->
            if(user?.allianceName != null){
                //alliancesRepository.getAlliancesById(user.allianceName!!){ alliance ->
                 //   callback(alliance)
                //}
            }
        }
    }

    fun updateStepCount(userId: String, stepCount: Int, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // Reference to the user's document in the USERS collection
        val userRef = db.collection(CollectionPath.USERS).document(userId)

        // Add or update the stepCount field
        userRef.set(
            mapOf("stepCount" to stepCount),
            SetOptions.merge() // Merge with existing fields
        )
            .addOnSuccessListener {
                println("Step count updated successfully")
                onComplete(true) // Notify the caller that the update was successful
            }
            .addOnFailureListener { e ->
                println("Failed to update step count: ${e.message}")
                onComplete(false) // Notify the caller that the update failed
            }
    }

}