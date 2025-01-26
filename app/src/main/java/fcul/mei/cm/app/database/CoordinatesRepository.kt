package fcul.mei.cm.app.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import fcul.mei.cm.app.domain.Coordinates
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.FirebaseConfig
import fcul.mei.cm.app.utils.ReferencePath

class CoordinatesRepository {

    private val db = Firebase.database(FirebaseConfig.DB_URL)

    private val myRef = db.getReference(ReferencePath.COORDINATES)

    private val userRepository = UserRepository()

    fun saveCoordinates(userId: String, coordinates: Coordinates){
        Log.d(TAG, "Value is: $coordinates")

        myRef.child(userId).child(ReferencePath.COORDINATES).setValue(coordinates)

    }

    fun getCoordinates(districtNumber: Int, callback: (Map<User, Coordinates?>) -> Unit) {
        userRepository.getUserFromSameDistrict(districtNumber) { users ->
            if (users.isEmpty()) {
                callback(emptyMap())
                return@getUserFromSameDistrict
            }
            Log.d("DistritoFirebase", users.toString())
            val coordinatesMap = mutableMapOf<User, Coordinates?>()
            users.forEach { user ->
                myRef.child(user.id)
                    .child(ReferencePath.COORDINATES)
                    .addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val coordinates = snapshot.getValue(Coordinates::class.java)
                            coordinatesMap[user] = coordinates
                            Log.d("DistritoFirebase", users.toString())
                            callback(coordinatesMap)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w(TAG, "Failed to read coordinates for ${user.id}.", error.toException())
                            coordinatesMap[user] = null
                            callback(coordinatesMap)
                        }
                    })
            }
        }
    }



    companion object {
        private const val TAG = "Coordinates"
    }
}