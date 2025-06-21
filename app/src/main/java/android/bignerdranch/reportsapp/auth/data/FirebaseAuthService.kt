package android.bignerdranch.reportsapp.auth.data

import android.bignerdranch.reportsapp.auth.domain.AuthService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// auth/data/FirebaseAuthService.kt
class FirebaseAuthService : AuthService {
    override suspend fun getUserId(): String {
        val currentUser = Firebase.auth.currentUser
        return currentUser?.uid ?: signInAnonymously()
    }

    private suspend fun signInAnonymously(): String {
        return try {
            Firebase.auth.signInAnonymously().await().user?.uid
                ?: throw Exception("Null user after auth")
        } catch (e: Exception) {
            throw Exception("Firebase auth failed: ${e.message}")
        }
    }

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }
}