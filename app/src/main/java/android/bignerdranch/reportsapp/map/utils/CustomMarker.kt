package android.bignerdranch.reportsapp.map.utils

import android.bignerdranch.reportsapp.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yandex.mapkit.geometry.Point

@Composable
fun CustomMarker(
    position: Point,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = LocalMapView.current
    var screenPosition by remember { mutableStateOf(Offset.Zero) }

    // Конвертируем географические координаты в экранные
    LaunchedEffect(position) {
        screenPosition = withFrameMillis {
            mapView.mapWindow.worldToScreen(position).let {
                Offset(it!!.x.toFloat(), it.y.toFloat())
            }
        }
    }

    // Рендерим маркер поверх карты
    Box(
        modifier = Modifier
            .offset { IntOffset(screenPosition.x.toInt(), screenPosition.y.toInt()) }
            .size(48.dp) // Размер зоны нажатия
            .clickable { onClick() }
            .then(modifier)
    ) {
        Image(
            painter = painterResource(R.drawable.ymk_default_point),
            contentDescription = "Marker",
            modifier = Modifier.size(24.dp) // Видимый размер иконки
        )
    }
}