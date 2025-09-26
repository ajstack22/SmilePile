package com.smilepile.ui.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.storage.StorageManager
import com.smilepile.utils.ImageProcessor
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.models.Photo
import com.smilepile.data.models.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for photo editing operations.
 * Manages rotation, cropping, and batch processing.
 * Atlas Lite: pragmatic, under 250 lines.
 */
@HiltViewModel
class PhotoEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager,
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoEditUiState())
    val uiState: StateFlow<PhotoEditUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var pendingCategoryId: Long = 1L // Default category
    private var editMode: EditMode = EditMode.IMPORT

    /**
     * Initialize editor with photos to edit
     * @param photoUris URIs for new imports
     * @param photoPaths Paths for existing photos from gallery
     * @param categoryId Category to save imported photos to
     */
    fun initializeEditor(
        photoUris: List<Uri>? = null,
        photoPaths: List<String>? = null,
        categoryId: Long = 1L
    ) {
        pendingCategoryId = categoryId
        editMode = if (photoUris != null) EditMode.IMPORT else EditMode.GALLERY
        viewModelScope.launch {
            val editQueue = mutableListOf<PhotoEditItem>()

            // Add URIs if provided (new imports)
            photoUris?.forEach { uri ->
                editQueue.add(PhotoEditItem(uri = uri))
            }

            // Add paths if provided (existing photos)
            photoPaths?.forEach { path ->
                editQueue.add(PhotoEditItem(path = path))
            }

            // For gallery mode with single photo, use its existing category
            if (editMode == EditMode.GALLERY && photoPaths?.size == 1) {
                photoPaths.firstOrNull()?.let { path ->
                    val existingPhoto = photoRepository.getPhotoByPath(path)
                    existingPhoto?.let {
                        pendingCategoryId = it.categoryId
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                editQueue = editQueue,
                totalPhotos = editQueue.size,
                currentIndex = if (editQueue.isNotEmpty()) 0 else -1
            )

            // Load first photo if available
            if (editQueue.isNotEmpty()) {
                loadCurrentPhoto()
            }
        }
    }

    /**
     * Load the current photo for editing
     */
    private suspend fun loadCurrentPhoto() {
        val currentItem = getCurrentEditItem() ?: return

        try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load bitmap based on source
            val bitmap = when {
                currentItem.uri != null -> {
                    // Load from URI (new import)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(currentItem.uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                }
                currentItem.path != null -> {
                    // Load from path (existing photo)
                    withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(currentItem.path)
                    }
                }
                else -> null
            }

            bitmap?.let {
                // Check for EXIF rotation and apply if needed
                val exifRotation = currentItem.path?.let { path ->
                    ImageProcessor.getExifRotation(path)
                } ?: 0

                // Apply EXIF rotation to the bitmap if needed
                val correctedBitmap = if (exifRotation != 0) {
                    ImageProcessor.rotateBitmap(it, exifRotation.toFloat())
                } else {
                    it
                }

                // Create preview for UI (memory efficient)
                val preview = ImageProcessor.createPreviewBitmap(correctedBitmap)

                _uiState.value = _uiState.value.copy(
                    currentBitmap = correctedBitmap,
                    previewBitmap = preview,
                    currentRotation = 0f, // Reset rotation since EXIF is already applied
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load photo: ${e.message}"
            )
        }
    }

    /**
     * Rotate current photo by 90 degrees clockwise
     */
    fun rotatePhoto() {
        val currentBitmap = _uiState.value.currentBitmap ?: return
        val currentRotation = _uiState.value.currentRotation
        val newRotation = (currentRotation + 90) % 360

        // Apply rotation to the preview bitmap
        val rotatedBitmap = ImageProcessor.rotateBitmap(currentBitmap, 90f)

        _uiState.value = _uiState.value.copy(
            currentRotation = newRotation,
            currentBitmap = rotatedBitmap,
            previewBitmap = ImageProcessor.createPreviewBitmap(rotatedBitmap)
        )
    }

    /**
     * Update crop rectangle
     */
    fun updateCropRect(rect: androidx.compose.ui.geometry.Rect) {
        val androidRect = Rect(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt()
        )
        _uiState.value = _uiState.value.copy(currentCropRect = androidRect)
    }

    /**
     * Apply aspect ratio preset
     */
    fun applyAspectRatio(aspectRatio: ImageProcessor.AspectRatio) {
        val bitmap = _uiState.value.currentBitmap ?: return

        val cropRect = ImageProcessor.calculateAspectRatioCrop(
            bitmap.width,
            bitmap.height,
            aspectRatio
        )

        _uiState.value = _uiState.value.copy(currentCropRect = cropRect)
        updatePreview()
    }

    /**
     * Skip current photo without editing
     */
    fun skipCurrentPhoto() {
        markCurrentAsProcessed(edited = false)
        moveToNextPhoto()
    }

    /**
     * Apply edits to current photo and move to next
     */
    fun applyCurrentPhoto() {
        viewModelScope.launch {
            val bitmap = _uiState.value.currentBitmap ?: return@launch
            val cropRect = _uiState.value.currentCropRect

            // Process the image - rotation is already applied to currentBitmap
            // so we only need to apply crop if present
            val processedBitmap = if (cropRect != null) {
                ImageProcessor.cropBitmap(bitmap, cropRect)
            } else {
                bitmap
            }

            // Mark as processed with edits
            markCurrentAsProcessed(
                edited = true,
                processedBitmap = processedBitmap
            )

            moveToNextPhoto()
        }
    }

    /**
     * Apply same edits to all remaining photos
     */
    fun applyToAll() {
        val rotation = _uiState.value.currentRotation
        val shouldApplyToAll = rotation != 0f // Only rotation can be applied to all

        if (shouldApplyToAll) {
            _uiState.value = _uiState.value.copy(
                applyRotationToAll = true,
                batchRotation = rotation
            )
        }

        applyCurrentPhoto()
    }

    /**
     * Delete the current photo from the edit queue
     */
    fun deleteCurrentPhoto() {
        val currentIndex = _uiState.value.currentIndex
        val editQueue = _uiState.value.editQueue.toMutableList()

        if (currentIndex >= 0 && currentIndex < editQueue.size) {
            val item = editQueue[currentIndex]

            // If editing existing photo, delete from storage and database
            viewModelScope.launch {
                item.path?.let { path ->
                    try {
                        // Delete from storage
                        storageManager.deletePhoto(path)
                        // Remove from database
                        photoRepository.getPhotoByPath(path)?.let { photo ->
                            photoRepository.deletePhoto(photo)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmilePile", "Failed to delete photo: ${e.message}")
                    }
                }
            }

            // Remove from edit queue
            editQueue.removeAt(currentIndex)

            // Update state
            val newTotalPhotos = editQueue.size
            val newCurrentIndex = if (currentIndex >= newTotalPhotos) {
                newTotalPhotos - 1
            } else {
                currentIndex
            }

            _uiState.value = _uiState.value.copy(
                editQueue = editQueue,
                totalPhotos = newTotalPhotos,
                currentIndex = newCurrentIndex
            )

            // Load next photo or complete if no more
            if (newTotalPhotos > 0 && newCurrentIndex >= 0) {
                viewModelScope.launch {
                    loadCurrentPhoto()
                }
            } else {
                _uiState.value = _uiState.value.copy(isComplete = true)
            }
        }
    }

    /**
     * Update the pending category for saving photos
     */
    fun updatePendingCategory(categoryId: Long) {
        pendingCategoryId = categoryId
    }

    /**
     * Get the current pending category ID
     */
    fun getPendingCategoryId(): Long = pendingCategoryId

    private fun markCurrentAsProcessed(edited: Boolean, processedBitmap: Bitmap? = null) {
        val currentIndex = _uiState.value.currentIndex
        if (currentIndex >= 0) {
            val updatedQueue = _uiState.value.editQueue.toMutableList()
            updatedQueue[currentIndex] = updatedQueue[currentIndex].copy(
                isProcessed = true,
                wasEdited = edited,
                processedBitmap = processedBitmap
            )
            _uiState.value = _uiState.value.copy(editQueue = updatedQueue)
        }
    }

    private fun moveToNextPhoto() {
        val nextIndex = _uiState.value.currentIndex + 1

        if (nextIndex < _uiState.value.totalPhotos) {
            _uiState.value = _uiState.value.copy(currentIndex = nextIndex)
            viewModelScope.launch {
                loadCurrentPhoto()
            }
        } else {
            // All photos processed
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    private fun updatePreview() {
        // Update preview bitmap with current edits
        // This would apply rotation and crop to the preview
    }

    private fun getCurrentEditItem(): PhotoEditItem? {
        val index = _uiState.value.currentIndex
        return if (index >= 0 && index < _uiState.value.editQueue.size) {
            _uiState.value.editQueue[index]
        } else null
    }

    fun getProcessedResults(): List<PhotoEditResult> {
        return _uiState.value.editQueue.mapNotNull { item ->
            if (item.isProcessed) {
                PhotoEditResult(
                    originalUri = item.uri,
                    originalPath = item.path,
                    wasEdited = item.wasEdited,
                    processedBitmap = item.processedBitmap,
                    savedPath = item.savedPath
                )
            } else null
        }
    }

    /**
     * Save all processed photos to storage and database
     */
    suspend fun saveAllProcessedPhotos(): List<Photo> {
        val savedPhotos = mutableListOf<Photo>()

        _uiState.value.editQueue.forEach { item ->
            if (item.isProcessed && item.wasEdited && item.processedBitmap != null) {
                try {
                    // Check if we're editing an existing photo or importing a new one
                    if (item.path != null && editMode == EditMode.GALLERY) {
                        // Editing existing photo - overwrite the original file
                        val existingFile = File(item.path)
                        val filename = existingFile.name

                        // Overwrite the existing file
                        val savedFile = storageManager.savePhotoToInternalStorage(
                            bitmap = item.processedBitmap,
                            filename = filename
                        )

                        if (savedFile != null) {
                            // Find and update the existing photo in database
                            val existingPhoto = photoRepository.getPhotoByPath(item.path)
                            if (existingPhoto != null) {
                                val updatedPhoto = existingPhoto.copy(
                                    categoryId = pendingCategoryId, // Update category
                                    width = item.processedBitmap.width,
                                    height = item.processedBitmap.height,
                                    fileSize = savedFile.length(),
                                    createdAt = System.currentTimeMillis() // Update timestamp
                                )
                                photoRepository.updatePhoto(updatedPhoto)
                                savedPhotos.add(updatedPhoto)
                            }

                            // Update item with saved path
                            val index = _uiState.value.editQueue.indexOf(item)
                            if (index >= 0) {
                                val updatedQueue = _uiState.value.editQueue.toMutableList()
                                updatedQueue[index] = item.copy(savedPath = savedFile.absolutePath)
                                _uiState.value = _uiState.value.copy(editQueue = updatedQueue)
                            }
                        }
                    } else {
                        // New import - create a new file
                        val filename = "edited_${System.currentTimeMillis()}_${savedPhotos.size}.jpg"

                        // Save to internal storage
                        val savedFile = storageManager.savePhotoToInternalStorage(
                            bitmap = item.processedBitmap,
                            filename = filename
                        )

                        if (savedFile != null) {
                            // Create photo entry
                            val photo = Photo(
                                id = 0,
                                name = filename,
                                path = savedFile.absolutePath,
                                categoryId = pendingCategoryId,
                                isFavorite = false,
                                createdAt = System.currentTimeMillis(),
                                width = item.processedBitmap.width,
                                height = item.processedBitmap.height,
                                fileSize = savedFile.length(),
                                isFromAssets = false
                            )

                            // Save to database
                            val photoId = photoRepository.insertPhoto(photo)
                            savedPhotos.add(photo.copy(id = photoId))

                            // Update item with saved path
                            val index = _uiState.value.editQueue.indexOf(item)
                            if (index >= 0) {
                                val updatedQueue = _uiState.value.editQueue.toMutableList()
                                updatedQueue[index] = item.copy(savedPath = savedFile.absolutePath)
                                _uiState.value = _uiState.value.copy(editQueue = updatedQueue)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SmilePile", "Failed to save edited photo: ${e.message}")
                }
            } else if (item.isProcessed && !item.wasEdited && editMode == EditMode.IMPORT) {
                // For imports that weren't edited, still save them
                item.uri?.let { uri ->
                    try {
                        val filename = "import_${System.currentTimeMillis()}_${savedPhotos.size}.jpg"

                        // Copy from URI to internal storage
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        val savedFile = storageManager.savePhotoToInternalStorage(bitmap, filename)

                        savedFile?.let { file ->
                            val photo = Photo(
                                id = 0,
                                name = filename,
                                path = savedFile.absolutePath,
                                categoryId = pendingCategoryId,
                                isFavorite = false,
                                createdAt = System.currentTimeMillis(),
                                width = bitmap.width,
                                height = bitmap.height,
                                fileSize = savedFile.length(),
                                isFromAssets = false
                            )

                            val photoId = photoRepository.insertPhoto(photo)
                            savedPhotos.add(photo.copy(id = photoId))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmilePile", "Failed to save unedited import: ${e.message}")
                    }
                }
            } else if (item.isProcessed && !item.wasEdited && editMode == EditMode.GALLERY) {
                // For gallery photos that were skipped but category may have changed
                item.path?.let { path ->
                    try {
                        val existingPhoto = photoRepository.getPhotoByPath(path)
                        existingPhoto?.let { photo ->
                            if (photo.categoryId != pendingCategoryId) {
                                // Only update if category changed
                                val updatedPhoto = photo.copy(categoryId = pendingCategoryId)
                                photoRepository.updatePhoto(updatedPhoto)
                                savedPhotos.add(updatedPhoto)
                            } else {
                                // Photo exists but category hasn't changed, still add to saved photos
                                savedPhotos.add(photo)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmilePile", "Failed to update category for skipped photo: ${e.message}")
                    }
                }
            }
        }

        return savedPhotos
    }
}

data class PhotoEditUiState(
    val editQueue: List<PhotoEditItem> = emptyList(),
    val currentIndex: Int = -1,
    val totalPhotos: Int = 0,
    val currentBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val currentRotation: Float = 0f,
    val currentCropRect: Rect? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
    val applyRotationToAll: Boolean = false,
    val batchRotation: Float = 0f
) {
    val progressText: String get() = "${currentIndex + 1} / $totalPhotos"
    val canApplyToAll: Boolean get() = currentRotation != 0f && currentIndex < totalPhotos - 1
}

data class PhotoEditItem(
    val uri: Uri? = null,
    val path: String? = null,
    val isProcessed: Boolean = false,
    val wasEdited: Boolean = false,
    val processedBitmap: Bitmap? = null,
    val savedPath: String? = null
)

data class PhotoEditResult(
    val originalUri: Uri?,
    val originalPath: String?,
    val wasEdited: Boolean,
    val processedBitmap: Bitmap?,
    val savedPath: String? = null
)

enum class EditMode {
    IMPORT,  // Editing photos being imported
    GALLERY  // Editing existing photos from gallery
}