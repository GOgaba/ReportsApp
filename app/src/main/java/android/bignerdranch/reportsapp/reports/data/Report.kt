package android.bignerdranch.reportsapp.reports.data

import android.bignerdranch.reportsapp.storage.StorageService
import android.util.Log
import com.yandex.mapkit.geometry.Point
import java.util.UUID
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.time.LocalDateTime

data class Report(
    var id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val mediaUrls: List<String> = emptyList(), // Ссылки на фото/видео
    val location: Point? = null, // Координаты с карты
    val createdAt: Long = System.currentTimeMillis(),
    var isViewedByAdmin: Boolean = false
)

class ReportRepository(private val storageService: StorageService) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun markReportAsViewed(report: Report) {
        val updates = hashMapOf<String, Any>(
            "isViewedByAdmin" to true
        )
        try {
            db.collection("reports").document(report.id)
                .update(updates)  // Полная перезапись (или .update() для частичных изменений)
                .await()
        } catch (e: Exception) {
            // Обработка ошибок
        }
    }

    suspend fun deleteReport(report: Report) {
        try {
            // 1. Удаляем медиафайлы из VK Cloud (если есть)
            report.mediaUrls.forEach { url ->
                storageService.deleteFile(url)  // Ваш метод для удаления файла
            }

            // 2. Удаляем документ из Firestore
            db.collection("reports").document(report.id).delete().await()

        } catch (e: Exception) {
            throw IOException("Failed to delete report", e)
        }
    }

    suspend fun deleteViewedReports(): Boolean {
        return try {
            // Получаем все просмотренные репорты
            val viewedReports = db.collection("reports")
                .whereEqualTo("isViewedByAdmin", true)
                .get()
                .await()

            // Удаляем каждый репорт
            viewedReports.forEach { document ->
                val report = document.toObject(Report::class.java).copy(id = document.id)
                deleteReport(report) // Используем существующий метод удаления
            }

            true
        } catch (e: Exception) {
            Log.e("ReportRepository", "Error deleting viewed reports", e)
            false
        }
    }

    suspend fun getReports(): List<Report> = try {
        db.collection("reports")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                try {
                    // 1. Сначала получаем сырые данные из документа
                    val data = document.data ?: run {
                        Log.w("Firestore", "Document ${document.id} has no data")
                        return@mapNotNull null
                    }

                    // 2. Явно извлекаем все необходимые поля
                    Report(
                        id = document.id,
                        title = data["title"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Long) ?: 0L,
                        mediaUrls = (data["mediaUrls"] as? List<String>) ?: emptyList(),
                        location = (data["location"] as? Map<String, Double>)?.let {
                            Point(it["latitude"] ?: 0.0, it["longitude"] ?: 0.0)
                        },
                        isViewedByAdmin = data["isViewedByAdmin"] as? Boolean ?: false, // Явное извлечение
                        userId = data["userId"] as? String ?: ""
                    ).also {
                        Log.d("Firestore",
                            "Loaded report: ${it.id}\n" +
                                    "Title: ${it.title}\n" +
                                    "Viewed: ${it.isViewedByAdmin}\n" +
                                    "Location: ${it.location?.latitude},${it.location?.longitude}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error converting document ${document.id}", e)
                    null
                }
            }.also {
                Log.d("Firestore", "Total reports fetched: ${it.size}")
            }
    } catch (e: Exception) {
        Log.e("Firestore", "Error fetching reports", e)
        throw e
    }

    fun saveReport(report: Report, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        // Конвертируем Report в Map для Firestore
        val reportData = hashMapOf(
            "id" to report.id,
            "userId" to report.userId,
            "title" to report.title,
            "description" to report.description,
            "mediaUrls" to report.mediaUrls,
            "createdAt" to report.createdAt
        )

        // Добавляем location, если он есть
        report.location?.let {
            reportData["location"] = it
        }

        // Сохраняем в коллекцию "reports"
        db.collection("reports")
            .document(report.id) // Используем UUID как ID документа
            .set(reportData)
            .addOnSuccessListener {
                onSuccess(report.id)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}