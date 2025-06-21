package android.bignerdranch.reportsapp.ui.components.dialogs

import android.annotation.SuppressLint
import android.bignerdranch.reportsapp.R
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.reports.presentation.components.FullScreenImageDialog
import android.bignerdranch.reportsapp.storage.S3Config
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date


private fun getPublicFileUrl(fileKey: String): String {
    return "https://${S3Config.BUCKET_NAME}.hb.vkcs.cloud/$fileKey"
}

@SuppressLint("SimpleDateFormat")
@Composable
fun ReportInfoDialog(
    report: Report,
    onDismiss: () -> Unit
) {
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }

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
                            val imageUrl = remember(fileKey) {
                                getPublicFileUrl(fileKey) // Преобразуем fileKey в полный URL
                            }

                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(4.dp)
                                    .clickable {
                                        expandedImageUrl = imageUrl
                                    },
                                contentScale = ContentScale.Crop
                            )
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
}