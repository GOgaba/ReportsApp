package android.bignerdranch.reportsapp.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.times
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    // Правильное преобразование dp в px с учетом плотности экрана
    val screenWidthPx = remember(configuration) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    // Явно указываем типы для состояний
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Правильное создание transformable state с явными типами
    val transformState = rememberTransformableState { zoomChange: Float, panChange: Offset, _: Float ->
        val newScale = (scale * zoomChange).coerceIn(1f..5f)

        if (newScale > 1f) {
            val maxOffsetX = (newScale - 1f) * screenWidthPx / 2f
            val maxOffsetY = (newScale - 1f) * screenHeightPx / 2f

            // Явно создаем диапазоны как Float
            val xRange = -maxOffsetX..maxOffsetX
            val yRange = -maxOffsetY..maxOffsetY

            offset = Offset(
                x = (offset.x + panChange.x * newScale).coerceIn(xRange),
                y = (offset.y + panChange.y * newScale).coerceIn(yRange)
            )
        }
        scale = newScale
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Кнопка закрытия
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }

            // Контейнер для изображения с жестами
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale != 1f) 1f else 2f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .transformable(transformState)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .fillMaxSize()
                        .padding(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}