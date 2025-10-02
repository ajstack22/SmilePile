package com.smilepile.ui.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.storage.StorageManager
import com.smilepile.utils.IImageProcessor
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
    private val categoryRepository: CategoryRepository,
    private val imageProcessor: IImageProcessor
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
            val bitmap = loadBitmapFromItem(currentItem)

            bitmap?.let {
                val correctedBitmap = applyExifRotation(it, currentItem.path)
                val preview = imageProcessor.createPreviewBitmap(correctedBitmap)
                updateStateWithLoadedPhoto(correctedBitmap, preview)
            }
        } catch (e: Exception) {
            handleLoadPhotoError(e)
        }
    }

    private suspend fun loadBitmapFromItem(item: PhotoEditItem): android.graphics.Bitmap? {
        return when {
            item.uri != null -> loadBitmapFromUri(item.uri)
            item.path != null -> loadBitmapFromPath(item.path)
            else -> null
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): android.graphics.Bitmap? {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
    }

    private suspend fun loadBitmapFromPath(path: String): android.graphics.Bitmap? {
        return withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)
        }
    }

    private suspend fun applyExifRotation(bitmap: android.graphics.Bitmap, path: String?): android.graphics.Bitmap {
        val exifRotation = path?.let { imageProcessor.getExifRotation(it) } ?: 0
        return if (exifRotation != 0) {
            imageProcessor.rotateBitmap(bitmap, exifRotation.toFloat())
        } else {
            bitmap
        }
    }

    private fun updateStateWithLoadedPhoto(bitmap: android.graphics.Bitmap, preview: android.graphics.Bitmap) {
        _uiState.value = _uiState.value.copy(
            currentBitmap = bitmap,
            previewBitmap = preview,
            currentRotation = 0f,
            isLoading = false
        )
    }

    private fun handleLoadPhotoError(e: Exception) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "Failed to load photo: ${e.message}"
        )
    }

    /**
     * Rotate current photo by 90 degrees clockwise
     */
    fun rotatePhoto() {
        val currentBitmap = _uiState.value.currentBitmap ?: return
        val currentRotation = _uiState.value.currentRotation
        val newRotation = (currentRotation + 90) % 360

        // Apply rotation to the preview bitmap
        val rotatedBitmap = imageProcessor.rotateBitmap(currentBitmap, 90f)

        _uiState.value = _uiState.value.copy(
            currentRotation = newRotation,
            currentBitmap = rotatedBitmap,
            previewBitmap = imageProcessor.createPreviewBitmap(rotatedBitmap)
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
    fun applyAspectRatio(aspectRatio: IImageProcessor.AspectRatio) {
        val bitmap = _uiState.value.currentBitmap ?: return

        val cropRect = imageProcessor.calculateAspectRatioCrop(
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
                imageProcessor.cropBitmap(bitmap, cropRect)
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
            val savedPhoto = when {
                shouldSaveEditedPhoto(item) && editMode == EditMode.GALLERY ->
                    saveEditedGalleryPhoto(item)
                shouldSaveEditedPhoto(item) && editMode == EditMode.IMPORT ->
                    saveNewImportedPhoto(item, savedPhotos.size)
                shouldSaveUneditedImport(item) ->
                    saveUneditedImport(item, savedPhotos.size)
                shouldUpdateCategoryOnly(item) ->
                    updatePhotoCategory(item)
                else -> null
            }

            savedPhoto?.let { photo ->
                savedPhotos.add(photo)
                updateQueueItemPath(item, photo.path)
            }
        }

        return savedPhotos
    }

    private fun shouldSaveEditedPhoto(item: PhotoEditItem): Boolean {
        return item.isProcessed && item.wasEdited && item.processedBitmap != null
    }

    private fun shouldSaveUneditedImport(item: PhotoEditItem): Boolean {
        return item.isProcessed && !item.wasEdited && editMode == EditMode.IMPORT && item.uri != null
    }

    private fun shouldUpdateCategoryOnly(item: PhotoEditItem): Boolean {
        return item.isProcessed && !item.wasEdited && editMode == EditMode.GALLERY && item.path != null
    }

    private suspend fun saveEditedGalleryPhoto(item: PhotoEditItem): Photo? {
        return try {
            val existingFile = File(item.path!!)
            val savedFile = storageManager.savePhotoToInternalStorage(
                bitmap = item.processedBitmap!!,
                filename = existingFile.name
            ) ?: return null

            val existingPhoto = photoRepository.getPhotoByPath(item.path) ?: return null

            val updatedPhoto = existingPhoto.copy(
                categoryId = pendingCategoryId,
                width = item.processedBitmap.width,
                height = item.processedBitmap.height,
                fileSize = savedFile.length(),
                createdAt = System.currentTimeMillis()
            )
            photoRepository.updatePhoto(updatedPhoto)
            updatedPhoto
        } catch (e: Exception) {
            android.util.Log.e("SmilePile", "Failed to save edited photo: ${e.message}")
            null
        }
    }

    private suspend fun saveNewImportedPhoto(item: PhotoEditItem, index: Int): Photo? {
        return try {
            val filename = "edited_${System.currentTimeMillis()}_$index.jpg"
            val savedFile = storageManager.savePhotoToInternalStorage(
                bitmap = item.processedBitmap!!,
                filename = filename
            ) ?: return null

            val photo = createPhotoEntity(
                filename = filename,
                path = savedFile.absolutePath,
                width = item.processedBitmap.width,
                height = item.processedBitmap.height,
                fileSize = savedFile.length()
            )

            val photoId = photoRepository.insertPhoto(photo)
            photo.copy(id = photoId)
        } catch (e: Exception) {
            android.util.Log.e("SmilePile", "Failed to save edited photo: ${e.message}")
            null
        }
    }

    private suspend fun saveUneditedImport(item: PhotoEditItem, index: Int): Photo? {
        return try {
            val filename = "import_${System.currentTimeMillis()}_$index.jpg"

            val inputStream = context.contentResolver.openInputStream(item.uri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val savedFile = storageManager.savePhotoToInternalStorage(bitmap, filename) ?: return null

            val photo = createPhotoEntity(
                filename = filename,
                path = savedFile.absolutePath,
                width = bitmap.width,
                height = bitmap.height,
                fileSize = savedFile.length()
            )

            val photoId = photoRepository.insertPhoto(photo)
            photo.copy(id = photoId)
        } catch (e: Exception) {
            android.util.Log.e("SmilePile", "Failed to save unedited import: ${e.message}")
            null
        }
    }

    private suspend fun updatePhotoCategory(item: PhotoEditItem): Photo? {
        return try {
            val existingPhoto = photoRepository.getPhotoByPath(item.path!!) ?: return null

            if (existingPhoto.categoryId != pendingCategoryId) {
                val updatedPhoto = existingPhoto.copy(categoryId = pendingCategoryId)
                photoRepository.updatePhoto(updatedPhoto)
                updatedPhoto
            } else {
                existingPhoto
            }
        } catch (e: Exception) {
            android.util.Log.e("SmilePile", "Failed to update category for skipped photo: ${e.message}")
            null
        }
    }

    private fun createPhotoEntity(filename: String, path: String, width: Int, height: Int, fileSize: Long): Photo {
        return Photo(
            id = 0,
            name = filename,
            path = path,
            categoryId = pendingCategoryId,
            createdAt = System.currentTimeMillis(),
            width = width,
            height = height,
            fileSize = fileSize,
            isFromAssets = false
        )
    }

    private fun updateQueueItemPath(item: PhotoEditItem, savedPath: String) {
        val index = _uiState.value.editQueue.indexOf(item)
        if (index >= 0) {
            val updatedQueue = _uiState.value.editQueue.toMutableList()
            updatedQueue[index] = item.copy(savedPath = savedPath)
            _uiState.value = _uiState.value.copy(editQueue = updatedQueue)
        }
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