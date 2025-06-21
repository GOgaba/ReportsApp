package android.bignerdranch.reportsapp.auth.domain

interface AuthService {
    suspend fun getUserId(): String
    suspend fun signOut()
}