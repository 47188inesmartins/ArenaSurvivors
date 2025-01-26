package fcul.mei.cm.app.utils

import android.content.Context

class UserSharedPreferences(
    private val context: Context,
) {

    fun getUserId() : String? {
        val user = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)
        return user
    }

    fun saveUserId(userId: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_USER_ID, userId)
            apply()
        }
    }


    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USER_ID = "user_id"
    }
}