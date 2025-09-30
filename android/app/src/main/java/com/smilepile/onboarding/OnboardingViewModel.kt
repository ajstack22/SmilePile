package com.smilepile.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI Note for LLM Developers: "Pile" in user-facing text = "Category" in code/database
 */

enum class OnboardingStep {
    WELCOME,
    CATEGORIES,
    PIN_SETUP,
    COMPLETE
}

data class TempCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val icon: String? = null
)

data class ImportedPhotoData(
    val uri: Uri,
    val categoryId: String? = null
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val navigationHistory: List<OnboardingStep> = emptyList(),
    val categories: List<TempCategory> = emptyList(),
    val importedPhotos: List<ImportedPhotoData> = emptyList(),
    val pinCode: String? = null,
    val skipPin: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository,
    private val securePreferencesManager: SecurePreferencesManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val progress: Float
        get() = when (_uiState.value.currentStep) {
            OnboardingStep.WELCOME -> 0f
            OnboardingStep.CATEGORIES -> 0.33f
            OnboardingStep.PIN_SETUP -> 0.67f
            OnboardingStep.COMPLETE -> 1f
        }

    fun navigateNext() {
        val currentState = _uiState.value

        // Validate current step
        if (!validateCurrentStep()) return

        // Add current step to history
        val newHistory = currentState.navigationHistory + currentState.currentStep

        // Determine next step
        val nextStep = when (currentState.currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.CATEGORIES
            OnboardingStep.CATEGORIES -> OnboardingStep.PIN_SETUP
            OnboardingStep.PIN_SETUP -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }

        _uiState.update { it.copy(
            currentStep = nextStep,
            navigationHistory = newHistory
        ) }
    }

    fun navigateBack() {
        val currentState = _uiState.value
        if (currentState.navigationHistory.isEmpty()) return

        val newHistory = currentState.navigationHistory.dropLast(1)
        val previousStep = currentState.navigationHistory.last()

        _uiState.update { it.copy(
            currentStep = previousStep,
            navigationHistory = newHistory
        ) }
    }

    fun skip() {
        val currentState = _uiState.value

        when (currentState.currentStep) {
            OnboardingStep.PIN_SETUP -> {
                _uiState.update { it.copy(
                    skipPin = true,
                    currentStep = OnboardingStep.COMPLETE
                ) }
            }
            else -> {}
        }
    }

    fun addCategory(category: TempCategory) {
        val currentCategories = _uiState.value.categories
        if (currentCategories.size < 5 && !currentCategories.any { it.name == category.name }) {
            _uiState.update { it.copy(categories = it.categories + category) }
        }
    }

    fun removeCategory(category: TempCategory) {
        _uiState.update { it.copy(
            categories = it.categories.filter { it.id != category.id }
        ) }
    }

    fun setImportedPhotos(photos: List<ImportedPhotoData>) {
        _uiState.update { it.copy(importedPhotos = photos) }
    }

    fun setPinCode(pin: String) {
        _uiState.update { it.copy(pinCode = pin, skipPin = false) }
    }

    private fun validateCurrentStep(): Boolean {
        return when (_uiState.value.currentStep) {
            OnboardingStep.CATEGORIES -> {
                if (_uiState.value.categories.isEmpty()) {
                    _uiState.update { it.copy(error = "Please create at least one category") }
                    return false
                }
                true
            }
            OnboardingStep.PIN_SETUP -> {
                val state = _uiState.value
                if (!state.skipPin && state.pinCode.isNullOrEmpty()) {
                    _uiState.update { it.copy(error = "Please enter a PIN or skip this step") }
                    return false
                }
                true
            }
            else -> true
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value

                // Save categories
                val categoryIdMap = mutableMapOf<String, Long>()
                state.categories.forEachIndexed { index, tempCategory ->
                    val category = Category(
                        id = 0, // Let Room auto-generate the ID
                        name = tempCategory.name.lowercase().replace(" ", "_"),
                        displayName = tempCategory.name,
                        position = index,
                        colorHex = tempCategory.colorHex,
                        iconResource = tempCategory.icon,
                        isDefault = false,
                        createdAt = System.currentTimeMillis()
                    )
                    val newCategoryId = categoryRepository.insertCategory(category)
                    categoryIdMap[tempCategory.id] = newCategoryId
                }

                // Import photos if any
                state.importedPhotos.forEach { photoData ->
                    // In a real implementation, you would copy the photo to app storage
                    // and create proper Photo objects. This is a simplified version.
                    photoData.categoryId?.let { tempCategoryId ->
                        categoryIdMap[tempCategoryId]?.let { actualCategoryId ->
                            val photo = Photo(
                                id = 0, // Let Room auto-generate the ID
                                path = photoData.uri.toString(),
                                categoryId = actualCategoryId,
                                name = "Imported Photo",
                                isFromAssets = false,
                                createdAt = System.currentTimeMillis(),
                                fileSize = 0,
                                width = 0,
                                height = 0
                            )
                            photoRepository.insertPhoto(photo)
                        }
                    }
                }

                // Set up PIN if provided
                state.pinCode?.let { pin ->
                    securePreferencesManager.setPIN(pin)
                }

                // Mark onboarding as complete
                settingsManager.setOnboardingCompleted(true)

                _uiState.update { it.copy(
                    isLoading = false,
                    currentStep = OnboardingStep.COMPLETE
                ) }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to save onboarding data: ${e.message}"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}