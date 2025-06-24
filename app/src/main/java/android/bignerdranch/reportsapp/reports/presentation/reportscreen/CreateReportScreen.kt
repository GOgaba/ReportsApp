package android.bignerdranch.reportsapp.reports.presentation.reportscreen

import android.bignerdranch.reportsapp.auth.di.ReportViewModelFactory
import android.bignerdranch.reportsapp.auth.domain.UserManager
import android.bignerdranch.reportsapp.map.YandexMapView
import android.bignerdranch.reportsapp.reports.data.ReportRepository
import android.bignerdranch.reportsapp.reports.presentation.components.MediaGrid
import android.bignerdranch.reportsapp.reports.presentation.components.MediaPicker
import android.bignerdranch.reportsapp.storage.StorageService
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    userManager: UserManager,
    onBack: () -> Unit,
    location: Point?,
    repository: ReportRepository
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    // Добавляем обработчик кнопки "назад"
    BackHandler {
        onBack() // Вызываем переданный колбэк
    }

    // Получаем ViewModel с фабрикой
    val viewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory(userManager, storageService = StorageService(), repository)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый отчёт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
                 },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.submitReport(viewModel.mediaUris, contentResolver, location)
                          },
                modifier = Modifier
                    .alpha(if (viewModel.canSubmit) 1f else 0.5f)  // Визуальное затемнение
            ) {
                Icon(Icons.Default.Send, "Отправить")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Поля формы
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Описание") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            // Медиа
            MediaPicker(
                modifier = Modifier.padding(vertical = 16.dp),
                onMediaSelected = { viewModel.addMedia(it) }
            )

            MediaGrid(
                mediaUris = viewModel.mediaUris,
                onRemove = { viewModel.removeMedia(it) }
            )
        }
    }

}