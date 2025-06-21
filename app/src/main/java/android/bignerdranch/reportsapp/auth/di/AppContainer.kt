package android.bignerdranch.reportsapp.auth.di

import android.bignerdranch.reportsapp.auth.data.AuthPreferences
import android.bignerdranch.reportsapp.auth.data.FirebaseAuthService
import android.bignerdranch.reportsapp.auth.domain.AuthService
import android.bignerdranch.reportsapp.auth.domain.UserManager
import android.bignerdranch.reportsapp.reports.data.ReportRepository
import android.bignerdranch.reportsapp.reports.presentation.reportscreen.ReportViewModel
import android.bignerdranch.reportsapp.storage.StorageService
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.yandex.mapkit.location.Location

class AppContainer(private val context: Context) {
    // Защита от null контекста
    init {
        require(true) { "Context cannot be null" }
    }

    val storageService = StorageService()

    // Repository
    val reportRepository by lazy {
        ReportRepository().also {
            Log.d("AppContainer", "ReportRepository initialized")
        }
    }

    // Ленивая инициализация с логированием
    val authPreferences by lazy {
        AuthPreferences(context.applicationContext).also {
            Log.d("AppContainer", "AuthPreferences initialized")
        }
    }

    val authService by lazy {
        try {
            FirebaseAuthService().also {
                Log.d("AppContainer", "FirebaseAuthService initialized")
            }
        } catch (e: Exception) {
            Log.e("AppContainer", "Firebase init failed", e)
            throw e
        }
    }

    val userManager by lazy {
        UserManager(authService, authPreferences).also {
            Log.d("AppContainer", "UserManager initialized")
        }
    }

    // Фабрика ViewModel с проверкой типа
    fun provideReportViewModel(owner: ViewModelStoreOwner): ReportViewModel {
        return ViewModelProvider(
            owner,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == ReportViewModel::class.java) {
                        "Unknown ViewModel class"
                    }
                    return ReportViewModel(storageService, userManager, reportRepository) as T
                }
            }
        )[ReportViewModel::class.java]
    }

    // Очистка ресурсов
    suspend fun clear() {
        (authService as? FirebaseAuthService)?.signOut()
    }
}