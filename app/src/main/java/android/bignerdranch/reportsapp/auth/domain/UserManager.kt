package android.bignerdranch.reportsapp.auth.domain

import android.bignerdranch.reportsapp.auth.data.AuthPreferences
import android.bignerdranch.reportsapp.auth.data.FirebaseAuthService
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

// Упрощенный UserManager
class UserManager(
    private val authService: FirebaseAuthService,
    private val authPreferences: AuthPreferences
) {
    private val ADMIN_USER_ID = "WoUBvP31jRg6ZCf3L6mRflK4bdH3"

    val isAdmin: Boolean
        get() = authPreferences.getBoolean("is_admin")

    fun activateAdminMode(code: String): Boolean {
        val isCorrectCode = code == "WoUBvP31jRg6ZCf3L6mRflK4bdH3" // Проверяем код
        if (isCorrectCode) {
            authPreferences.saveBoolean("is_admin", true)
            Log.d("ADMIN", "Activate admin mode")
        }
        return isCorrectCode
    }

    fun deactivateAdminMode() {
        authPreferences.saveBoolean("is_admin", false)
    }

    suspend fun getUserId(): String {
        return try {
            val userId = authService.getUserId()
            Log.d("AUTH", "set Firebase id ")
            userId

        } catch (e: Exception) {
            val userId = authPreferences.getBackupUserId() // Fallback
            Log.d("AUTH", "set Preference id ")
            userId
        }
    }

    // Callback-версия (для Java или вызовов без suspend)
    fun getUserId(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userId = getUserId() // Вызываем suspend-версию
                callback(userId)
            } catch (e: Exception) {
                // Можно передать ошибку в callback, если нужно
                callback("") // или throw в зависимости от логики
            }
        }
    }

}