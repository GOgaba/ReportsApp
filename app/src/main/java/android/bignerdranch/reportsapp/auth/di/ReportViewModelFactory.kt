package android.bignerdranch.reportsapp.auth.di

import android.bignerdranch.reportsapp.auth.domain.AuthService
import android.bignerdranch.reportsapp.auth.domain.UserManager
import android.bignerdranch.reportsapp.reports.data.ReportRepository
import android.bignerdranch.reportsapp.reports.presentation.reportscreen.ReportViewModel
import android.bignerdranch.reportsapp.storage.StorageService
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ReportViewModelFactory(
    private val userManager: UserManager,  // Теперь принимает UserManager
    private val storageService: StorageService,
    private val reportRepository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ReportViewModel::class.java) -> {
                ReportViewModel(storageService, userManager, reportRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}