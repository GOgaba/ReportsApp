package android.bignerdranch.reportsapp.storage

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.amazonaws.services.s3.model.PutObjectRequest
import java.io.File
import android.content.Context
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class StorageService() {

    val filesPath =  mutableListOf<String>()

    suspend fun uploadFile(
        inputStream: InputStream,
        mimeType: String,
        remotePath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("TAG", "Начало загрузки файла типа $mimeType")

            val metadata = ObjectMetadata().apply {
                contentType = mimeType
                contentLength = inputStream.available().toLong()
            }

            val request = PutObjectRequest(
                S3Config.BUCKET_NAME,
                remotePath,
                inputStream,
                metadata
            ).apply {
                withCannedAcl(CannedAccessControlList.PublicRead)
            }

            S3Config.s3Client.putObject(request)
            "${S3Config.ENDPOINT}/${S3Config.BUCKET_NAME}/$remotePath"
        } catch (e: Exception) {
            Log.e("TAG", "Ошибка загрузки", e)
            null
        } finally {
            inputStream.close()
        }
    }

    suspend fun uploadFiles(
        uris: List<Uri>,
        folder: String,
        contentResolver: ContentResolver
    ): List<String> = withContext(Dispatchers.IO) {
        filesPath.clear() // Очищаем перед новой загрузкой
        Log.d("TAG", "Начало загрузки ${uris.size} файлов в папку: $folder")
        uris.mapNotNull { uri ->
            try {
                // Получаем MIME-тип и расширение
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val extension = mimeType.substringAfterLast('/').takeIf { it.isNotEmpty() }
                    ?: uri.path?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
                    ?: "bin"

                // Генерируем уникальное имя файла
                val fileName = "report_${System.currentTimeMillis()}_${(0..9999).random()}.$extension"
                val remotePath = "$folder/$fileName".replace("//", "/")

                Log.d("TAG", "Загрузка: $uri → $remotePath")

                // Открываем поток и загружаем
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    uploadFile(
                        inputStream = inputStream,
                        mimeType = mimeType,
                        remotePath = remotePath
                    )?.also {
                        filesPath.add(remotePath)
                        Log.d("TAG", "Успешно: $it")
                    }
                }
            } catch (e: Exception) {
                Log.e("TAG", "Ошибка обработки $uri", e)
                null
            }
        }. also {
            Log.d("TESTRETURN", "${filesPath}")
        }

    }

    suspend fun deleteFile(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("TAG", "Попытка удаления файла: $remotePath")

            // Удаляем объект из бакета
            S3Config.s3Client.deleteObject(
                DeleteObjectRequest(S3Config.BUCKET_NAME, remotePath)
            )

            Log.d("TAG", "Файл успешно удален: $remotePath")
            true
        } catch (e: Exception) {
            Log.e("TAG", "Ошибка при удалении файла $remotePath", e)
            false
        }
    }
}