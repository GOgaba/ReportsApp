package android.bignerdranch.reportsapp.reports.data

import android.util.Log
import com.yandex.mapkit.geometry.Point
import java.util.UUID
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Report(
    var id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val mediaUrls: List<String> = emptyList(), // Ссылки на фото/видео
    val location: Point? = null, // Координаты с карты
    val createdAt: Long = System.currentTimeMillis()
)

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getReports(): List<Report> = try {
        db.collection("reports")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                try {
                    document.toObject(Report::class.java)?.also {
                        // Можно добавить логгирование для отладки
                        Log.d("Firestore", "Successfully converted document ${document.id} to Report")
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error converting document ${document.id} to Report", e)
                    null
                }
            }.also {
                Log.d("Firestore", "Total reports fetched: ${it.size}")
            }
    } catch (e: Exception) {
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