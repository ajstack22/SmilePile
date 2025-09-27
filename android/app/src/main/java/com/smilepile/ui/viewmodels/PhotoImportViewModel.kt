package com.smilepile.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.storage.StorageManager
import com.smilepile.storage.StorageResult
import com.smilepile.storage.PhotoImportManager
import com.smilepile.storage.ImportResult
import com.smilepile.storage.PhotoMetadata
import kotlinx.coroutines.flow.collect
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
 * Enhanced with PhotoImportManager for advanced features.
 */
@HiltViewModel
class PhotoImportViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val storageManager: StorageManager,
    private val photoImportManager: PhotoImportManager
) : ViewModel() {

    companion object {
        private const val TAG = "PhotoImportViewModel"
    }

    private val _uiState = MutableStateFlow(PhotoImportUiState())
    val uiState: StateFlow<PhotoImportUiState> = _uiState.asStateFlow()

    private var pendingCategoryId: Long = 1L // Default category for editor

    /**
     * Import a single photo from the device gallery using enhanced PhotoImportManager
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

                // Import photo using PhotoImportManager
                val result = photoImportManager.importPhoto(uri)

                when (result) {
                    is ImportResult.Success -> {
                        _uiState.value = _uiState.value.copy(importProgress = 0.5f)

                        // Create photo entity with metadata and save to database
                        val photo = createPhotoFromImportResult(result, categoryId)
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
                            storageManager.deletePhoto(result.photoPath)
                            _uiState.value = _uiState.value.copy(
                                isImporting = false,
                                error = "Failed to save photo to database"
                            )
                        }
                    }
                    is ImportResult.Duplicate -> {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            error = "This photo has already been imported"
                        )
                        Log.d(TAG, "Duplicate photo detected: ${result.hash}")
                    }
                    is ImportResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            error = result.message
                        )
                        Log.e(TAG, "Import error: ${result.message}")
                    }
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
     * Import multiple photos from the device gallery using enhanced PhotoImportManager
     */
    fun importPhotos(uris: List<Uri>, categoryId: Long) {
        if (uris.isEmpty()) return

        // Validate batch size
        if (uris.size > PhotoImportManager.MAX_BATCH_SIZE) {
            _uiState.value = _uiState.value.copy(
                error = "Cannot import more than ${PhotoImportManager.MAX_BATCH_SIZE} photos at once"
            )
            return
        }

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
                val duplicateCount = mutableListOf<String>()
                var processedCount = 0

                // Use PhotoImportManager with progress tracking
                photoImportManager.importPhotosWithProgress(
                    uris = uris,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(
                            importProgress = progress
                        )
                    }
                ).collect { result ->
                    processedCount++
                    _uiState.value = _uiState.value.copy(
                        batchImportCompleted = processedCount
                    )

                    when (result) {
                        is ImportResult.Success -> {
                            // Create photo entity with metadata
                            val photo = createPhotoFromImportResult(result, categoryId)
                            val photoId = photoRepository.insertPhoto(photo)
                            if (photoId > 0) {
                                importedPhotos.add(photo.copy(id = photoId))
                                Log.d(TAG, "Successfully imported photo ${processedCount}/${uris.size}")
                            } else {
                                failedImports.add("Photo ${processedCount}: Database save failed")
                            }
                        }
                        is ImportResult.Duplicate -> {
                            duplicateCount.add(result.hash)
                            Log.d(TAG, "Duplicate photo detected: ${result.hash}")
                        }
                        is ImportResult.Error -> {
                            failedImports.add("Photo ${processedCount}: ${result.message}")
                            Log.e(TAG, "Import error: ${result.message}")
                        }
                    }
                }

                // Update final state with enhanced statistics
                val successCount = importedPhotos.size
                val failureCount = failedImports.size
                val duplicatesFound = duplicateCount.size

                val successMessage = buildString {
                    if (successCount > 0) {
                        append("$successCount photos imported successfully")
                    }
                    if (duplicatesFound > 0) {
                        if (successCount > 0) append(". ")
                        append("$duplicatesFound duplicates skipped")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = 1f,
                    batchImportCompleted = uris.size,
                    successMessage = if (successMessage.isNotEmpty()) successMessage else null,
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
            height = 0 // Will be updated later if needed
        )
    }

    private fun createPhotoFromImportResult(
        importResult: ImportResult.Success,
        categoryId: Long
    ): Photo {
        return Photo(
            id = 0, // Will be set by database
            path = importResult.photoPath, // Path is always internal storage
            name = importResult.fileName,
            categoryId = categoryId, // Category is now required
            isFromAssets = false,
            createdAt = importResult.metadata?.dateTaken?.let {
                // Try to parse the date from metadata
                try {
                    java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.getDefault()).parse(it)?.time
                } catch (e: Exception) {
                    null
                }
            } ?: System.currentTimeMillis(),
            fileSize = importResult.fileSize,
            width = importResult.metadata?.width ?: 0,
            height = importResult.metadata?.height ?: 0
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