package fcul.mei.cm.app.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import fcul.mei.cm.app.database.UserRepository
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.UserSharedPreferences

@SuppressLint("StaticFieldLeak")
class UserViewModel(
    private val context: Context,
) : ViewModel() {

    private val userRepository = UserRepository()

    private val userSharedPreferences = UserSharedPreferences(context)

    fun getUserId() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_USER_ID, null)

    fun addUser(district: Int, name: String) {
        val userId = userSharedPreferences.getUserId()
        if (userId != null) {
            Log.d(TAG, "User already added: $userId")
            return
        }

        val user = User(
            district = district,
            name = name,
        )

        userRepository.addUser(user) { userAdded ->
            if (userAdded) {
                userSharedPreferences.saveUserId(user.id)
                Log.d(TAG, "UserAlreadyAdded")
            }
        }
    }

    fun getAllUsers(callback: (List<User>) -> Unit) {
        return userRepository.getAllUsers { callback(it) }
    }

    fun getUsersFromDistrict(district: Int,  callback: (List<User>) -> Unit) {
        return userRepository.getUserFromSameDistrict(district) { members ->
            callback(members)
        }
    }

    fun getUserInfo(userId: String, callback: (User?) -> Unit) {
        return userRepository.getUser(userId) { callback(it) }
    }

    fun displaySameDistrict(district: Int): User? {
        var user: User? = null
        userRepository.getUserFromSameDistrict(district) { usersList ->
            user = usersList.find { it.district == district }
        }
        return user
    }


    fun deleteUser(){
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_USER_ID).apply()
    }

    companion object {


        private const val TAG = "--User_ViewModel"
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USER_ID = "user_id"
    }
}
