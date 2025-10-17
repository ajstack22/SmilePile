package com.smilepile.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.ISecurePreferencesManager
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

data class ImportStats(
    val categoriesRestored: Int = 0,
    val photosImported: Int = 0
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val navigationHistory: List<OnboardingStep> = emptyList(),
    val categories: List<TempCategory> = emptyList(),
    val importedPhotos: List<ImportedPhotoData> = emptyList(),
    val pinCode: String? = null,
    val skipPin: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val importMode: Boolean = false,
    val importStats: ImportStats? = null,
    val biometricEnabled: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository,
    private val securePreferencesManager: ISecurePreferencesManager,
    private val settingsManager: SettingsManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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
            OnboardingStep.WELCOME -> {
                // Skip categories if in import mode
                if (currentState.importMode) OnboardingStep.PIN_SETUP
                else OnboardingStep.CATEGORIES
            }
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
                // Allow proceeding even with no categories - we'll create defaults
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

    private fun createDefaultTempCategories(): List<TempCategory> {
        return listOf(
            TempCategory(name = "Family", colorHex = "#FF6B9D", icon = "family"),
            TempCategory(name = "Cars", colorHex = "#4A90E2", icon = "car"),
            TempCategory(name = "Games", colorHex = "#7ED321", icon = "game"),
            TempCategory(name = "Sports", colorHex = "#F5A623", icon = "sports")
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value

                // Only create/save categories in fresh setup mode (not import mode)
                if (!state.importMode) {
                    // If user hasn't created any categories, create default ones
                    val categoriesToSave = if (state.categories.isEmpty()) {
                        createDefaultTempCategories()
                    } else {
                        state.categories
                    }

                    val categoryIdMap = saveCategories(categoriesToSave)
                    importPhotos(state.importedPhotos, categoryIdMap)
                }

                savePinIfProvided(state.pinCode)

                // Save biometric preference if PIN was set and user enabled it
                if (!state.pinCode.isNullOrEmpty() && state.biometricEnabled) {
                    securePreferencesManager.setBiometricEnabled(true)
                }

                settingsManager.setOnboardingCompleted(true)

                _uiState.update { it.copy(
                    isLoading = false,
                    currentStep = OnboardingStep.COMPLETE
                ) }

            } catch (e: Exception) {
                handleOnboardingError(e)
            }
        }
    }

    private suspend fun saveCategories(categories: List<TempCategory>): Map<String, Long> {
        val categoryIdMap = mutableMapOf<String, Long>()
        android.util.Log.d("OnboardingVM", "📦 Saving ${categories.size} categories...")
        categories.forEachIndexed { index, tempCategory ->
            val category = createCategoryFromTemp(tempCategory, index)
            val newCategoryId = categoryRepository.insertCategory(category)
            categoryIdMap[tempCategory.id] = newCategoryId
            android.util.Log.d("OnboardingVM", "  ✓ Category '${tempCategory.name}' tempId=${tempCategory.id} → dbId=$newCategoryId")
        }
        android.util.Log.d("OnboardingVM", "📦 Category mapping complete: $categoryIdMap")
        return categoryIdMap
    }

    private fun createCategoryFromTemp(tempCategory: TempCategory, position: Int): Category {
        return Category(
            id = 0,
            name = tempCategory.name.lowercase().replace(" ", "_"),
            displayName = tempCategory.name,
            position = position,
            colorHex = tempCategory.colorHex,
            iconResource = tempCategory.icon,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private suspend fun importPhotos(
        importedPhotos: List<ImportedPhotoData>,
        categoryIdMap: Map<String, Long>
    ) {
        android.util.Log.d("OnboardingVM", "📸 Importing ${importedPhotos.size} photos...")
        importedPhotos.forEachIndexed { index, photoData ->
            android.util.Log.d("OnboardingVM", "  Photo $index: uri=${photoData.uri}, categoryId=${photoData.categoryId}")
            importSinglePhoto(photoData, categoryIdMap)
        }
    }

    private suspend fun importSinglePhoto(
        photoData: ImportedPhotoData,
        categoryIdMap: Map<String, Long>
    ) {
        // Determine the category ID: use mapped ID, or fall back to first category
        val actualCategoryId = photoData.categoryId?.let { tempCategoryId ->
            categoryIdMap[tempCategoryId]
        } ?: categoryIdMap.values.firstOrNull()

        if (actualCategoryId != null && actualCategoryId > 0) {
            val photo = createPhotoFromImport(photoData, actualCategoryId)
            photoRepository.insertPhoto(photo)
            android.util.Log.d("OnboardingVM", "✅ Imported photo to category $actualCategoryId")
        } else {
            android.util.Log.e("OnboardingVM", "❌ Failed to import photo: no valid category ID (photoData.categoryId=${photoData.categoryId}, mapped=$actualCategoryId)")
        }
    }

    private fun createPhotoFromImport(photoData: ImportedPhotoData, categoryId: Long): Photo {
        return Photo(
            id = 0,
            path = photoData.uri.toString(),
            categoryId = categoryId,
            name = "Imported Photo",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = 0,
            width = 0,
            height = 0
        )
    }

    private fun savePinIfProvided(pinCode: String?) {
        pinCode?.let { pin ->
            securePreferencesManager.setPIN(pin)
        }
    }

    private fun handleOnboardingError(e: Exception) {
        _uiState.update { it.copy(
            isLoading = false,
            error = "Failed to save onboarding data: ${e.message}"
        ) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun startImportFlow() {
        _uiState.update { it.copy(importMode = true) }
    }

    fun setImportStats(categoriesRestored: Int, photosImported: Int) {
        _uiState.update {
            it.copy(
                importStats = ImportStats(
                    categoriesRestored = categoriesRestored,
                    photosImported = photosImported
                )
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    // MARK: - Demo Mode

    fun enterDemoMode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Set demo mode flags
                settingsManager.setDemoMode(true)
                settingsManager.setDemoModeEntered(true)
                settingsManager.incrementDemoModeEntryCount()

                // Mark onboarding as complete
                settingsManager.setOnboardingCompleted(true)
                settingsManager.setFirstLaunch(false)

                // Load demo data if needed
                loadDemoDataIfNeeded()

                _uiState.update { it.copy(
                    isLoading = false,
                    currentStep = OnboardingStep.COMPLETE
                ) }

            } catch (e: Exception) {
                handleOnboardingError(e)
            }
        }
    }

    private suspend fun loadDemoDataIfNeeded() {
        // Check if demo data already exists
        val existingPhotos = photoRepository.getAllPhotos()
        val demoPhotos = existingPhotos.filter { it.isFromAssets }

        if (demoPhotos.isNotEmpty()) {
            android.util.Log.d("OnboardingVM", "Demo data already exists (${demoPhotos.size} photos), skipping load")
            return
        }

        android.util.Log.d("OnboardingVM", "Loading demo data...")

        // Load categories first
        val loadedCategories = mutableListOf<com.smilepile.data.models.Category>()
        com.smilepile.data.demo.DemoData.categories.forEach { categoryData ->
            val category = com.smilepile.data.models.Category(
                id = 0, // Auto-generate
                name = categoryData.name,
                displayName = categoryData.displayName,
                position = categoryData.position,
                iconResource = categoryData.icon,
                colorHex = categoryData.colorHex,
                isDefault = false,
                isDemoCategory = true,
                createdAt = System.currentTimeMillis()
            )

            val categoryId = categoryRepository.insertCategory(category)
            val insertedCategory = category.copy(id = categoryId)
            loadedCategories.add(insertedCategory)
            android.util.Log.d("OnboardingVM", "Created demo category: ${categoryData.displayName} (id: $categoryId)")
        }

        // Load first 10 photos immediately (high priority)
        val priorityPhotos = com.smilepile.data.demo.DemoData.photoMetadata.take(10)
        priorityPhotos.forEach { photoMeta ->
            loadDemoPhoto(photoMeta, loadedCategories)
        }

        // Load remaining photos in background
        val remainingPhotos = com.smilepile.data.demo.DemoData.photoMetadata.drop(10)
        if (remainingPhotos.isNotEmpty()) {
            viewModelScope.launch {
                remainingPhotos.forEach { photoMeta ->
                    try {
                        loadDemoPhoto(photoMeta, loadedCategories)
                    } catch (e: Exception) {
                        android.util.Log.w("OnboardingVM", "Failed to load demo photo ${photoMeta.assetName}: ${e.message}")
                    }
                }
                android.util.Log.d("OnboardingVM", "Background demo photo loading complete")
            }
        }

        android.util.Log.d("OnboardingVM", "Demo data loading initiated (10 photos loaded, ${remainingPhotos.size} loading in background)")
    }

    private suspend fun loadDemoPhoto(
        photoMeta: com.smilepile.data.demo.DemoData.PhotoMetadata,
        categories: List<com.smilepile.data.models.Category>
    ) {
        val categoryId = com.smilepile.data.demo.DemoData.getCategoryId(photoMeta.categoryName, categories)
        if (categoryId == null) {
            android.util.Log.w("OnboardingVM", "Category not found for ${photoMeta.categoryName}")
            return
        }

        // Copy from drawable to app filesDir
        try {
            val resourceId = getResourceId(photoMeta.assetName)
            if (resourceId == 0) {
                android.util.Log.w("OnboardingVM", "Resource not found: ${photoMeta.assetName}")
                return
            }

            // Create file in app storage
            val fileName = "${photoMeta.assetName}.jpg"
            val file = java.io.File(context.filesDir, fileName)

            // Copy resource to file
            copyResourceToFile(resourceId, file)

            // Create Photo object
            val photo = com.smilepile.data.models.Photo(
                id = 0,
                path = file.absolutePath,
                categoryId = categoryId,
                name = photoMeta.assetName,
                isFromAssets = true,
                createdAt = photoMeta.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                fileSize = file.length(),
                width = 0, // Will be calculated when needed
                height = 0  // Will be calculated when needed
            )

            photoRepository.insertPhoto(photo)
            android.util.Log.d("OnboardingVM", "Loaded demo photo: ${photoMeta.assetName} -> category $categoryId")

        } catch (e: Exception) {
            android.util.Log.w("OnboardingVM", "Failed to load demo photo ${photoMeta.assetName}: ${e.message}")
        }
    }

    private fun getResourceId(assetName: String): Int {
        // Get resource ID from drawable using reflection
        // Resource naming: demo_milestones_001 -> R.drawable.demo_milestones_001
        return try {
            context.resources.getIdentifier(assetName, "drawable", context.packageName)
        } catch (e: Exception) {
            android.util.Log.w("OnboardingVM", "Failed to get resource ID for $assetName: ${e.message}")
            0
        }
    }

    private fun copyResourceToFile(resourceId: Int, file: java.io.File) {
        context.resources.openRawResource(resourceId).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}