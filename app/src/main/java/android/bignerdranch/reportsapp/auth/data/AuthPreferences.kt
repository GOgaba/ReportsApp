package android.bignerdranch.reportsapp.auth.data

import android.content.Context
import java.util.UUID
import javax.inject.Inject

class AuthPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // Для чтения boolean значения
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefs.getBoolean(key, default)
    }

    // Для сохранения boolean значения
    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBackupUserId(): String {
        return prefs.getString("user_id", null) ?: run {
            val newId = "anon_${UUID.randomUUID()}"
            prefs.edit().putString("user_id", newId).apply()
            newId
        }
    }
}