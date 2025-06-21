package android.bignerdranch.reportsapp

import android.Manifest
import android.bignerdranch.reportsapp.auth.domain.AuthService
import android.bignerdranch.reportsapp.auth.domain.UserManager
import android.bignerdranch.reportsapp.map.YandexMapView
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.reports.presentation.reportscreen.CreateReportScreen
import android.bignerdranch.reportsapp.reports.presentation.reportscreen.ReportViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.bignerdranch.reportsapp.ui.theme.ReportsAppTheme
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location

private const val REQUEST_IMAGE_PICK = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val YANDEX_API_KEY = "af8e739d-55c6-4d5d-931c-61aebb058cca"


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000
    ).build()

    init {
        // Ранняя инициализация MapKit (до onCreate)
        initYandexMapKit()
    }

    private fun initYandexMapKit() {
        try {
            MapKitFactory.setApiKey(YANDEX_API_KEY)
            MapKitFactory.initialize(this)
            Log.d("MapKit", "Successfully initialized")
        } catch (e: Exception) {
            Log.e("MapKit", "Initialization failed", e)
            // Можно сохранить флаг ошибки для отображения в UI
        }
    }

    private val appContainer get() = (application as? MyApp)?.container
        ?: throw IllegalStateException("Invalid Application class")


    //Проверка разрешения на геопозицию
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startLocationUpdates(callback: (com.yandex.mapkit.geometry.Point) -> Unit) {
        if (checkLocationPermission()) {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { androidLocation ->
                        // Преобразуем Android Location в Yandex Point
                        val yandexPoint = com.yandex.mapkit.geometry.Point(
                            androidLocation.latitude,
                            androidLocation.longitude
                        )
                        callback(yandexPoint)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        // Инициализация аутентификации при запуске
        val authService = appContainer.authService
        val authPreferences = appContainer.authPreferences
        val userManager = UserManager(authService, authPreferences)

        // Выполняем авторизацию
        userManager.getUserId { userId ->
            runOnUiThread {
                userManager.activateAdminMode(userId)
                Log.d("AUTH", "User ID: $userId")
                // Можно сохранить userId в ViewModel или обработать здесь
            }
        }

        setContent {
            ReportsAppTheme {
                var showReportScreen by remember { mutableStateOf(false) }
                var currentLocation by remember { mutableStateOf<Point?>(null) }
                var reports by remember { mutableStateOf<List<Report>>(emptyList()) }

                // Эффект для получения локации
                LaunchedEffect(Unit) {
                    if (checkLocationPermission()) {
                        startLocationUpdates { location ->
                            currentLocation = Point(location.latitude, location.longitude)
                        }
                        Log.d("LE_CHECK", "Локация прогрузилась...")
                    }

                    // Загрузка отчетов
                    Log.d("LE_CHECK", "Отчёты прогружаются...")
                    reports = appContainer.reportRepository.getReports()
                    Log.d("LE_CHECK", "Количество отчётов: ${reports.size}...")

                }

                // Дефолтная локация (Москва)
                val defaultLocation = remember { Point(55.751574, 37.573856) }
                val actualLocation = currentLocation ?: defaultLocation

                Scaffold(
                    floatingActionButton = {
                        if (!showReportScreen) {
                            FloatingActionButton(
                                onClick = { Log.d("NAVIGATION", "FAB clicked") // Добавляем лог
                                showReportScreen = true }
                            ) {
                                Icon(Icons.Default.Add, "Создать отчёт")
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        // 4. Условный рендеринг экранов
                        when {
                            showReportScreen -> {
                                Log.d("RS", "1. Before container access")
                                val authService = appContainer.authService.also {
                                    Log.d("RS", "2. AuthService obtained")
                                }
                                val authPreferences = appContainer.authPreferences.also {
                                    Log.d("RS", "3. AuthPreferences obtained")
                                }
                                Log.d("RS", "4. Before UserManager creation")
                                val userManager = UserManager(authService, authPreferences).also {
                                    Log.d("RS", "5. UserManager created")
                                }
                                val reportRepository = appContainer.reportRepository
                                Log.d("RS", "6. Before CreateReportScreen")
                                CreateReportScreen(
                                    userManager = userManager,
                                    onBack = { showReportScreen = false },
                                    currentLocation,
                                    reportRepository
                                ).also {
                                    Log.d("RS", "7. CreateReportScreen composed")
                                }
                            }

                            else -> {
                                Log.d("LOCATION", "current location ${currentLocation?.latitude}, ${currentLocation?.longitude}")
                                Log.d("REPORTS", "${reports}")
                                YandexMapView(currentLocation, reports?: emptyList(), userManager.isAdmin)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReportsAppTheme {
        Greeting("Android")
    }
}