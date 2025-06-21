package android.bignerdranch.reportsapp.reports.presentation.reportscreen

import android.bignerdranch.reportsapp.auth.domain.AuthService
import android.bignerdranch.reportsapp.auth.domain.UserManager
import android.bignerdranch.reportsapp.reports.data.Report
import android.bignerdranch.reportsapp.reports.data.ReportRepository
import android.bignerdranch.reportsapp.storage.StorageService
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import java.util.UUID

class ReportViewModel(
    private val storageService: StorageService,
    private val userManager: UserManager,
    private val reportRepository: ReportRepository
) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    val mediaUris = mutableStateListOf<Uri>()
    val canSubmit: Boolean
        get() = title.isNotBlank() && mediaUris.isNotEmpty()

    // Состояние UI
    private val _uiState = mutableStateOf<ReportUiState>(ReportUiState.Idle)
    val uiState: State<ReportUiState> get() = _uiState

    // Список загруженных отчетов
    private val _reports = mutableStateOf<List<Report>>(emptyList())
    val reports: State<List<Report>> get() = _reports

    // Загрузка отчетов
    fun loadReports() {
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                val loadedReports = reportRepository.getReports()
                _reports.value = loadedReports
                _uiState.value = ReportUiState.Success(loadedReports)
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(e.message ?: "Unknown error")
                _reports.value = emptyList()
            }
        }
    }



    fun addMedia(uris: List<Uri>) {
        mediaUris.addAll(uris)
    }

    fun removeMedia(uri: Uri) {
        mediaUris.remove(uri)
    }

    fun submitReport(mediaUris: List<Uri>, contentResolver: ContentResolver, location: Point?) {
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                val userId = userManager.getUserId() ?: throw Exception("User not authenticated")
                // Загрузка медиа
                val uploadedUrls = storageService.apply {
                    uploadFiles(
                    mediaUris,
                    "reports/${userManager.getUserId()}/",
                    contentResolver
                )}.filesPath
                Log.d("TESTRETURN", "${uploadedUrls}")
                // Создание отчета
                val report = Report(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    description = description,
                    mediaUrls = uploadedUrls,
                    location = location,
                    createdAt = System.currentTimeMillis()
                )
                Log.d("TESTRETURN", "${report.mediaUrls}")
                // Сохранение через репозиторий
                reportRepository.saveReport(
                    report = report,
                    onSuccess = { reportId ->
                        // Обновляем список отчетов
                        _reports.value = _reports.value + report  // Добавляем новый отчет
                        // Обновляем состояние UI
                        _uiState.value = ReportUiState.Success(_reports.value)
                    },
                    onError = { e ->
                        _uiState.value = ReportUiState.Error(e.message ?: "Report save failed")
                    }
                )
                Log.d("SubmitReport", "Отчёт отправлен")
            } catch (e: Exception) {
                "Ошибка отправки: ${e.message}"

            }
        }
    }
}

// Состояния UI
sealed class ReportUiState {
    object Idle : ReportUiState()        // Начальное состояние
    object Loading : ReportUiState()     // Загрузка данных
    data class Success(val reports: List<Report>) : ReportUiState()  // Успех с данными
    data class Error(val message: String) : ReportUiState()  // Ошибка с сообщением
}