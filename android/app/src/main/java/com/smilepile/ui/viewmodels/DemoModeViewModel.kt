package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoModeUiState(
    val isDemoMode: Boolean = false,
    val isExiting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DemoModeViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

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

                // Set demo mode to false
                settingsManager.setDemoMode(false)

                _uiState.value = _uiState.value.copy(isExiting = false)

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
}
