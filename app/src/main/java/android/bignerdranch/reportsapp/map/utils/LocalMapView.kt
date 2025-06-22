package android.bignerdranch.reportsapp.map.utils

import android.bignerdranch.reportsapp.R
import android.util.Log
import android.bignerdranch.reportsapp.map.YandexMapView
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.ui.components.dialogs.ReportInfoDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView

val LocalMapView = staticCompositionLocalOf<MapView> {
    error("MapView not provided")
}

@Composable
fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            MapKitFactory.getInstance().onStart()
            this.onStart()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    return mapView
}

@Composable
fun MapWithMarkers(
    location: Point?,
    reports: List<Report>,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Правильное создание MapView
    val mapView = remember {
        MapView(context).apply {
            // Настройки карты можно добавить здесь
        }
    }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    Box(modifier = modifier) {
        // Используем существующий YandexMapView как основу
        YandexMapView(
            location = location,
            reports = reports, // Не показываем стандартные маркеры
            isAdmin = isAdmin
        )

        // Накладываем кастомные Compose-маркеры
        CompositionLocalProvider(LocalMapView provides mapView) {
            reports.forEach { report ->
                Log.d("MAP_MARKERS", "позиция отч1та ${report.location?.latitude}, ${report.location?.longitude}")
                report.location?.let { point ->
                    DynamicMarker(
                        position = point,
                        report = report,
                        isAdmin = isAdmin,
                        onClick = {selectedReport = report}
                    )
                }
            }
        }

        // Диалог при выборе маркера
        selectedReport?.let { report ->
            ReportInfoDialog(
                report = report,
                onDismiss = { selectedReport = null }
            )
        }
    }
}


@Composable
fun rememberCameraState(mapView: MapView): CameraPosition {
    var cameraState by remember { mutableStateOf(mapView.map.cameraPosition) }

    LaunchedEffect(Unit) {
        val listener = object : CameraListener {
            // Новый метод с 4 параметрами
            override fun onCameraPositionChanged(
                map: com.yandex.mapkit.map.Map,
                position: CameraPosition,
                reason: CameraUpdateReason,
                finished: Boolean
            ) {
                cameraState = position
            }
        }

        mapView.map.addCameraListener(listener)
    }

    return cameraState
}

@Composable
fun DynamicMarker(
    position: Point,
    report: Report,
    isAdmin: Boolean,
    onClick: () -> Unit
) {
    val mapView = LocalMapView.current
    var screenPosition by remember { mutableStateOf(Offset.Zero) }
    var isMapReady by remember { mutableStateOf(false) }

    // Ждём готовности карты
    LaunchedEffect(mapView) {
        mapView.map.addCameraListener(object : CameraListener {
            override fun onCameraPositionChanged(
                map: Map,
                cameraPosition: CameraPosition,
                reason: CameraUpdateReason,
                finished: Boolean
            ) {
                isMapReady = true
            }
        })
    }

    // Обновление позиции при изменении камеры
    LaunchedEffect(position, isMapReady, mapView.map.cameraPosition) {
        if (isMapReady) {
            val screenPoint = try {
                mapView.mapWindow.worldToScreen(position) ?: return@LaunchedEffect
            } catch (e: Exception) {
                Log.e("MAP_MARKERS", "Coordinate conversion error", e)
                return@LaunchedEffect
            }

            screenPosition = Offset(
                x = screenPoint.x.toFloat(),
                y = screenPoint.y.toFloat()
            ).also {
                Log.d("MAP_MARKERS", "Converted: $position → $it")
            }
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (screenPosition.x.toInt()),
                    (screenPosition.y.toInt())
                )
            }
            .size(48.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(
                R.drawable.ymk_default_point
            ),
            contentDescription = "Marker",
            tint = Color.Red,
            modifier = Modifier.size(24.dp)
        )
    }
}