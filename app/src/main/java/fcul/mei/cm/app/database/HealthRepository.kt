package fcul.mei.cm.app.database

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.domain.Health
import fcul.mei.cm.app.utils.CollectionPath

class HealthRepository {

    private val db = Firebase.firestore
    private val userRepository = UserRepository()
    private val alliancesRepository = AlliancesRepository()

    fun saveHealthInformation(health: Health) {
        db.collection(CollectionPath.HEALTH)
            .document(health.userId)
            .set(health)
                .addOnSuccessListener {
                    println("Participant added successfully")
                }
            .addOnFailureListener { e ->
                println("Error adding participant: ${e.message}")
        }
    }

     fun getHealthInformationByUser(userId: String, callback: (Health?) -> Unit) {
        userRepository.getUser(userId) { user ->
            if (user?.allianceName != null){

                db.collection(CollectionPath.HEALTH)
                    .get()
                    .addOnSuccessListener { result ->
                        val userHealth = result.map { document ->
                            document.toObject(Health::class.java).copy(userId = document.id)
                        }
                        userHealth.find { it.userId == userId}?.let { callback(it) }
                    }
                    .addOnFailureListener {
                        callback(null)
                    }
            }
        }
    }
}