package fcul.mei.cm.app.database

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import fcul.mei.cm.app.domain.MarkerData
import fcul.mei.cm.app.utils.CollectionPath

class MarkerRepository {
    val db = Firebase.firestore

    fun saveMarkerToDatabase(marker: MarkerData, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(CollectionPath.MARKERS)
            .add(marker)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}