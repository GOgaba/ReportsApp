package android.bignerdranch.reportsapp.storage

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri

@SuppressLint("MissingPermission")
fun downloadMediaFile(context: Context, url: String) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // Определяем тип файла
    val isImage = url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png")
    val isVideo = url.endsWith(".mp4") || url.endsWith(".mov")

    // Создаем запрос на скачивание
    val request = DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setTitle("Скачивание файла")
        .setDescription("Идёт загрузка медиафайла")
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    // Устанавливаем правильную директорию и делаем файл видимым для медиа-сканера
    when {
        isImage -> {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES,
                "ReportsApp/${System.currentTimeMillis()}.${url.substringAfterLast('.')}"
            )
        }
        isVideo -> {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "ReportsApp/${System.currentTimeMillis()}.${url.substringAfterLast('.')}"
            )
        }
        else -> {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "ReportsApp/${System.currentTimeMillis()}.${url.substringAfterLast('.')}"
            )
        }
    }

    // Делаем файл видимым в галерее
    if (isImage || isVideo) {
        request.allowScanningByMediaScanner()
    }

    try {
        downloadManager.enqueue(request)
        Toast.makeText(context, "Файл сохраняется в галерею", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}