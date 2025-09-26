package com.smilepile.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.storage.StorageManager
import com.smilepile.storage.StorageResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for handling photo import operations from device gallery.
 * Manages the import process including storage operations and database saves.
 * All imported photos are stored in internal storage only for privacy and security.
 */
@HiltViewModel
class PhotoImportViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val storageManager: StorageManager
) : ViewModel() {

    companion object {
        private const val TAG = "PhotoImportViewModel"
    }

    private val _uiState = MutableStateFlow(PhotoImportUiState())
    val uiState: StateFlow<PhotoImportUiState> = _uiState.asStateFlow()

    private var pendingCategoryId: Long = 1L // Default category for editor

    /**
     * Import a single photo from the device gallery
     */
    fun importPhoto(uri: Uri, categoryId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImporting = true,
                    importProgress = 0f,
                    error = null
                )

                // Check available space
                if (!storageManager.hasEnoughSpace()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "Not enough storage space available"
                    )
                    return@launch
                }

                // Import photo to app's internal storage (all photos stored internally for security)
                val storageResult = storageManager.importPhoto(uri)
                if (storageResult == null) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "Failed to import photo. Please try again."
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(importProgress = 0.5f)

                // Create photo entity and save to database
                val photo = createPhotoFromStorageResult(storageResult, categoryId)
                val photoId = photoRepository.insertPhoto(photo)

                if (photoId > 0) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importProgress = 1f,
                        lastImportedPhoto = photo.copy(id = photoId),
                        successMessage = "Photo imported successfully"
                    )
                    Log.d(TAG, "Successfully imported photo with ID: $photoId")
                } else {
                    // Import failed, clean up storage
                    storageManager.deletePhoto(storageResult.photoPath)
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "Failed to save photo to database"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing photo", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    /**
     * Import multiple photos from the device gallery
     */
    fun importPhotos(uris: List<Uri>, categoryId: Long) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImporting = true,
                    importProgress = 0f,
                    error = null,
                    batchImportTotal = uris.size,
                    batchImportCompleted = 0
                )

                // Check available space for batch import
                val estimatedTotalSize = uris.size * 10 * 1024 * 1024L // 10MB per photo estimate
                if (!storageManager.hasEnoughSpace(estimatedTotalSize)) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "Not enough storage space for ${uris.size} photos"
                    )
                    return@launch
                }

                val importedPhotos = mutableListOf<Photo>()
                val failedImports = mutableListOf<String>()

                uris.forEachIndexed { index, uri ->
                    try {
                        // Update progress
                        val progress = index.toFloat() / uris.size
                        _uiState.value = _uiState.value.copy(
                            importProgress = progress,
                            batchImportCompleted = index
                        )

                        // Import individual photo to internal storage
                        val storageResult = storageManager.importPhoto(uri)
                        if (storageResult != null) {
                            val photo = createPhotoFromStorageResult(storageResult, categoryId)
                            val photoId = photoRepository.insertPhoto(photo)

                            if (photoId > 0) {
                                importedPhotos.add(photo.copy(id = photoId))
                                Log.d(TAG, "Successfully imported photo ${index + 1}/${uris.size}")
                            } else {
                                // Clean up failed import
                                storageManager.deletePhoto(storageResult.photoPath)
                                failedImports.add("Photo ${index + 1}")
                            }
                        } else {
                            failedImports.add("Photo ${index + 1}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing photo ${index + 1}", e)
                        failedImports.add("Photo ${index + 1}: ${e.message}")
                    }
                }

                // Update final state
                val successCount = importedPhotos.size
                val failureCount = failedImports.size

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = 1f,
                    batchImportCompleted = uris.size,
                    successMessage = when {
                        successCount == uris.size -> "All $successCount photos imported successfully"
                        successCount > 0 -> "$successCount photos imported successfully"
                        else -> null
                    },
                    error = when {
                        failureCount == uris.size -> "Failed to import all photos"
                        failureCount > 0 -> "Failed to import $failureCount photos"
                        else -> null
                    }
                )

                Log.d(TAG, "Batch import completed: $successCount success, $failureCount failed")
            } catch (e: Exception) {
                Log.e(TAG, "Error in batch photo import", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Batch import failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear any error or success messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }

    /**
     * Set the category ID for photos being imported through the editor
     */
    fun setPendingCategoryId(categoryId: Long) {
        pendingCategoryId = categoryId
    }

    /**
     * Get the pending category ID for the editor
     */
    fun getPendingCategoryId(): Long = pendingCategoryId

    /**
     * Reset import state
     */
    fun resetImportState() {
        _uiState.value = PhotoImportUiState()
    }

    /**
     * Get storage usage information
     */
    fun refreshStorageInfo() {
        viewModelScope.launch {
            try {
                val usage = storageManager.calculateStorageUsage()
                val availableSpace = storageManager.getAvailableSpace()

                _uiState.value = _uiState.value.copy(
                    storageUsageMB = usage.getTotalMB(),
                    availableSpaceMB = availableSpace / (1024.0 * 1024.0),
                    photoCount = usage.photoCount
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing storage info", e)
            }
        }
    }

    private fun createPhotoFromStorageResult(
        storageResult: StorageResult,
        categoryId: Long
    ): Photo {
        return Photo(
            id = 0, // Will be set by database
            path = storageResult.photoPath, // Path is always internal storage
            name = storageResult.fileName,
            categoryId = categoryId, // Category is now required
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = storageResult.fileSize,
            width = 0, // Will be updated later if needed
            height = 0, // Will be updated later if needed
            isFavorite = false
        )
    }
}

/**
 * UI state for photo import operations
 */
data class PhotoImportUiState(
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val batchImportTotal: Int = 0,
    val batchImportCompleted: Int = 0,
    val lastImportedPhoto: Photo? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val storageUsageMB: Double = 0.0,
    val availableSpaceMB: Double = 0.0,
    val photoCount: Int = 0
) {
    val isInProgress: Boolean get() = isImporting
    val isBatchImport: Boolean get() = batchImportTotal > 1
    val batchProgressText: String get() = "$batchImportCompleted / $batchImportTotal"
}