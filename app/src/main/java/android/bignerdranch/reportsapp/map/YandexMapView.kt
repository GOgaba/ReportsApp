@file:Suppress("DEPRECATION")

package android.bignerdranch.reportsapp.map

import android.Manifest
import android.bignerdranch.reportsapp.R
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.ui.components.dialogs.ReportInfoDialog
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.get
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.type.LatLng
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

@Composable
fun YandexMapView(location: Point?,
                  reports: List<Report> = emptyList(),
                  isAdmin: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }
    var initializationError by remember { mutableStateOf<String?>(null) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

        // Инициализация MapKit
    DisposableEffect(Unit) {

        try {
            // Проверяем инициализацию через try-catch
            MapKitFactory.getInstance()
            Log.d("YandexMap", "MapKit already initialized")
        } catch (e: IllegalStateException) {
            try {
                MapKitFactory.initialize(context)
                Log.d("YandexMap", "MapKit initialized successfully")
            } catch (e: Exception) {
                initializationError = "MapKit init error: ${e.message}"
                Log.e("YandexMap", "Initialization failed", e)
            }
        }

        onDispose {
            // Очистка ресурсов
        }
    }

    // Обработка жизненного цикла
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                    Log.d("YandexMap", "onStart called")
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                    Log.d("YandexMap", "onStop called")
                }
                else -> {}
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Отображение ошибки, если есть
    initializationError?.let { error ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Основное отображение карты
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            try {
                view.mapWindow.map.apply {
                    move(
                        CameraPosition(
                            Point(55.751574, 37.573856), // Москва
                            11.0f,  // Зум
                            0.0f,   // Азимут
                            0.0f    // Наклон
                        )
                    )
                    if (isAdmin) {
                        reports.forEach { report ->
                            report.location?.let { safeLocation -> // Пропускаем отчеты с null-локацией
                                val marker = mapView.map.mapObjects.addPlacemark(safeLocation).apply {
                                    setIcon(ImageProvider.fromResource(mapView.context, R.drawable.ymk_default_point))
                                    addTapListener { _ ->
                                        Log.d("REPORT_SELECT", "отчёт нажат")
                                        selectedReport = report
                                        true
                                    }
                                }
                            }
                        }
                    }
                    Log.d("YandexMap", "Map configured")
                }
            } catch (e: Exception) {
                Log.e("YandexMap", "Map configuration error", e)
            }
        }
    )

    // Показываем диалог, если selectedReport != null
    selectedReport?.let { report ->
        ReportInfoDialog(
            report = report,
            onDismiss = { selectedReport = null }
        )
    }
}