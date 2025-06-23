package android.bignerdranch.reportsapp.ui.components.dialogs

import android.annotation.SuppressLint
import android.bignerdranch.reportsapp.R
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.reports.presentation.components.FullScreenImageDialog
import android.bignerdranch.reportsapp.reports.presentation.components.FullScreenVideoDialog
import android.bignerdranch.reportsapp.storage.S3Config
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date


private fun getPublicFileUrl(fileKey: String): String {
    Log.d("IMAGE_URL", "URL: ${"https://${S3Config.BUCKET_NAME}.hb.vkcs.cloud/$fileKey"}")
    return "https://${S3Config.BUCKET_NAME}.hb.vkcs.cloud/$fileKey"
}

@SuppressLint("SimpleDateFormat")
@Composable
fun ReportInfoDialog(
    report: Report,
    onDismiss: () -> Unit
) {
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedVideoUrl by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(report.title) },
        text = {
            Column {
                Text(report.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(report.createdAt))}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Координаты: ${report.location?.latitude}, ${report.location?.longitude}")

                // Превью медиа (если есть)
                if (report.mediaUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Прикрепленные медиа:")

                    LazyRow {
                        items(report.mediaUrls) { fileKey ->
                            Log.d("IMAGE_URL", "$fileKey")
                            val isVideo = fileKey.endsWith(".mp4") // или другие видеоформаты
                            val mediaUrl = remember(fileKey) { getPublicFileUrl(fileKey) }

                            if (isVideo) {
                                VideoThumbnail(
                                    videoUrl = mediaUrl,
                                    onClick = {
                                        Log.d("ReportInfoDialog", "Opening video: $mediaUrl")
                                        expandedVideoUrl = mediaUrl } // Новое состояние для видео
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(mediaUrl)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable { expandedImageUrl = mediaUrl }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
    // Блок для отображения изображения в полном размере
    expandedImageUrl?.let { url ->
        FullScreenImageDialog(
            imageUrl = url,
            onDismiss = { expandedImageUrl = null }
        )
    }

    // Проверьте, что диалог добавлен в конце:
    expandedVideoUrl?.let { url ->
        Log.d("ReportInfoDialog", "Launching FullScreenVideoDialog")
        FullScreenVideoDialog(url, onDismiss = { expandedVideoUrl = null })
    }
}