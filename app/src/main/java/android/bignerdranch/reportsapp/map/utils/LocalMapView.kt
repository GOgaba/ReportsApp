package android.bignerdranch.reportsapp.map.utils

import android.bignerdranch.reportsapp.R
import android.util.Log
import android.bignerdranch.reportsapp.map.YandexMapView
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.reports.data.ReportRepository
import android.bignerdranch.reportsapp.ui.components.dialogs.ReportInfoDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
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
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.launch

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
    reportRepository: ReportRepository,
    location: Point?,
    reports: SnapshotStateList<Report>,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Правильное создание MapView
    val mapView = rememberMapView() // Используем единый экземпляр
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    // Диалог подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reportToDelete by remember { mutableStateOf<Report?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить отчёт?") },
            text = { Text("Это действие нельзя отменить") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                reportToDelete?.let { report ->
                                    // 1. Удаляем из репозитория (базы данных)
                                    reportRepository.deleteReport(report)

                                    // 2. Удаляем из локального списка (это вызовет перекомпозицию)
                                    reports.remove(report)

                                    // 3. Закрываем диалог
                                    showDeleteDialog = false

                                    // 4. Сбрасываем выбранный отчёт (если нужно)
                                    selectedReport = null
                                }
                                showDeleteDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) { Text("Отмена") }
            }
        )
    }

    Box(modifier = modifier) {
        // Передаём mapView в YandexMapView
        YandexMapView(
            mapView = mapView, // <-- Важно!
            location = location,
            reports = if (isAdmin) emptyList() else reports, // Админам не показываем стандартные маркеры
            isAdmin = isAdmin
        )

        // Накладываем кастомные Compose-маркеры
        CompositionLocalProvider(LocalMapView provides mapView) {
            Log.d("OUR_REPORTS", "Наши отчёты: $reports")
            reports.forEach { report ->
                report.location?.let { point ->
                    DynamicMarker(
                        key = report.id,
                        position = point,
                        report = report,
                        isAdmin = isAdmin,
                        onClick = {
                            coroutineScope.launch {
                                report.isViewedByAdmin = true  // <- ПРОСТО изменяем!
                                reportRepository.markReportAsViewed(report)  // Автоматически сохраняем
                            }
                            selectedReport = report
                        },
                        onLongClick = {
                            reportToDelete = report
                            showDeleteDialog = true
                        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicMarker(
    key: String,
    position: Point,
    report: Report,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val mapView = LocalMapView.current
    var screenPosition by remember { mutableStateOf(Offset.Zero) }
    var isVisible by remember { mutableStateOf(false) }

    // Оптимизация: используем DisposableEffect для подписки/отписки
    DisposableEffect(mapView) {
        val listener = object : CameraListener {
            override fun onCameraPositionChanged(
                map: Map,
                cameraPosition: CameraPosition,
                reason: CameraUpdateReason,
                finished: Boolean
            ) {
                val visibleRegion = map.visibleRegion
                val isInView = isPointInVisibleRegion(position, visibleRegion)

                if (isInView) {
                    val screenPoint = mapView.mapWindow.worldToScreen(position) ?: return
                    screenPosition = Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                }

                isVisible = isInView // Обновляем видимость
            }
        }

        // Подписываемся на изменения камеры
        mapView.map.addCameraListener(listener)

        // Удаляем listener при выходе из композиции
        onDispose {
            mapView.map.removeCameraListener(listener)
        }
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .offset { IntOffset(screenPosition.x.toInt(), screenPosition.y.toInt()) }
                .size(48.dp)
                .combinedClickable(  // Используем combinedClickable вместо clickable
                    onClick = onClick,
                    onLongClick = onLongClick,  // Обработчик долгого нажатия
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            if (report.isViewedByAdmin) {
                Icon(
                    painter = painterResource(R.drawable.ymk_default_point_viewed),
                    contentDescription = "Viewed marker",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center) // Выравниваем по центру Box
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ymk_default_point),
                    contentDescription = "Marker",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center) // Выравниваем по центру Box
                )
                }
        }
    }
}

fun isPointInVisibleRegion(point: Point, visibleRegion: VisibleRegion): Boolean {
    val minLat = minOf(
        visibleRegion.topLeft.latitude,
        visibleRegion.topRight.latitude,
        visibleRegion.bottomLeft.latitude,
        visibleRegion.bottomRight.latitude
    )
    val maxLat = maxOf(
        visibleRegion.topLeft.latitude,
        visibleRegion.topRight.latitude,
        visibleRegion.bottomLeft.latitude,
        visibleRegion.bottomRight.latitude
    )
    val minLon = minOf(
        visibleRegion.topLeft.longitude,
        visibleRegion.topRight.longitude,
        visibleRegion.bottomLeft.longitude,
        visibleRegion.bottomRight.longitude
    )
    val maxLon = maxOf(
        visibleRegion.topLeft.longitude,
        visibleRegion.topRight.longitude,
        visibleRegion.bottomLeft.longitude,
        visibleRegion.bottomRight.longitude
    )

    return point.latitude in minLat..maxLat &&
            point.longitude in minLon..maxLon
}