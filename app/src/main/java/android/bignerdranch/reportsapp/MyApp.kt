package android.bignerdranch.reportsapp

import android.app.Application
import android.bignerdranch.reportsapp.auth.di.AppContainer
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    val container: AppContainer by lazy {
        AppContainer(applicationContext) // Передаём контекст
    }

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("AuthDebug", "Firebase init success")

            // Инициализируем лениво при первом обращении
            container.authService
        } catch (e: Exception) {
            Log.e("AuthDebug", "Init failed", e)
            // Можно добавить fallback-логику
        }
    }
}