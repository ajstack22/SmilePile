package com.smilepile.ui.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.MainActivity
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoModeUiState(
    val isDemoMode: Boolean = false,
    val isExiting: Boolean = false,
    val error: String? = null,
    val shouldNavigateToOnboarding: Boolean = false
)

@HiltViewModel
class DemoModeViewModel @Inject constructor(
    application: Application,
    private val settingsManager: SettingsManager,
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DemoModeUiState())
    val uiState: StateFlow<DemoModeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsManager.isDemoMode().collect { isDemoMode ->
                _uiState.value = _uiState.value.copy(isDemoMode = isDemoMode)
            }
        }
    }

    fun exitDemoMode() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExiting = true, error = null)

                // Delete all photos where isFromAssets = true
                val allPhotos = photoRepository.getAllPhotos()
                val demoPhotos = allPhotos.filter { it.isFromAssets }

                demoPhotos.forEach { photo ->
                    // Delete photo file
                    val file = java.io.File(photo.path)
                    if (file.exists()) {
                        file.delete()
                    }

                    // Delete from database
                    photoRepository.deletePhoto(photo)
                }

                // Delete demo categories
                categoryRepository.deleteDemoCategories()

                // Set demo mode to false and reset onboarding to trigger wizard
                settingsManager.setDemoMode(false)
                settingsManager.setOnboardingCompleted(false)

                // Wait for DataStore to persist changes by checking the values
                var retries = 0
                while (retries < 20) {
                    kotlinx.coroutines.delay(100)
                    val hasCompleted = settingsManager.hasCompletedOnboarding().first()
                    val isDemoMode = settingsManager.isDemoMode().first()
                    if (!hasCompleted && !isDemoMode) {
                        // DataStore has persisted both changes
                        break
                    }
                    retries++
                }

                _uiState.value = _uiState.value.copy(isExiting = false, shouldNavigateToOnboarding = true)

                // Restart the app to trigger onboarding
                val context = getApplication<Application>()
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                context.startActivity(intent)

                // Force kill the process to ensure a clean restart
                android.os.Process.killProcess(android.os.Process.myPid())

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExiting = false,
                    error = "Failed to exit demo mode: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onNavigatedToOnboarding() {
        _uiState.value = _uiState.value.copy(shouldNavigateToOnboarding = false)
    }
}
