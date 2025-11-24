package cl.samuel.barzarena.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "barzarena_session"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveSession(userId: Int) {
        prefs.edit {
            putInt(KEY_USER_ID, userId)
        }
    }

    fun getUserId(): Int? {
        val userId = prefs.getInt(KEY_USER_ID, -1)
        return if (userId != -1) userId else null
    }

    fun clearSession() {
        prefs.edit {
            remove(KEY_USER_ID)
        }
    }
}
