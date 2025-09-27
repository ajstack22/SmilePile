package com.smilepile.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.camera.CameraManager
import com.smilepile.camera.FlashMode
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * ViewModel for camera functionality
 * Manages camera state and photo capture operations
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // Camera initialization state
    private val _isCameraInitialized = MutableStateFlow(false)
    val isCameraInitialized: StateFlow<Boolean> = _isCameraInitialized.asStateFlow()

    // Flash mode state
    private val _flashMode = MutableStateFlow(FlashMode.AUTO)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    // Front camera availability
    private val _hasFrontCamera = MutableStateFlow(false)
    val hasFrontCamera: StateFlow<Boolean> = _hasFrontCamera.asStateFlow()

    // Current camera (front/back)
    private val _isUsingFrontCamera = MutableStateFlow(false)
    val isUsingFrontCamera: StateFlow<Boolean> = _isUsingFrontCamera.asStateFlow()

    /**
     * Initialize camera with preview
     */
    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                cameraManager.initializeCamera(context, lifecycleOwner, previewView)

                // Update camera capabilities
                _hasFrontCamera.value = cameraManager.hasFrontCamera()
                _isUsingFrontCamera.value = cameraManager.isUsingFrontCamera()
                _flashMode.value = cameraManager.getCurrentFlashMode()

                _isCameraInitialized.value = true
                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize camera: ${exception.message}"
                )
            }
        }
    }

    /**
     * Capture a photo and save it to the database
     */
    fun capturePhoto(context: Context, categoryId: Long) {
        if (!_isCameraInitialized.value) {
            _uiState.value = _uiState.value.copy(error = "Camera not initialized")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isCapturing = true, error = null)

                // Capture photo
                val photoUri = cameraManager.capturePhoto(context)
                val photoFile = File(photoUri.path ?: "")

                if (!photoFile.exists()) {
                    throw Exception("Photo file not found")
                }

                // Get image dimensions and file size
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val width = bitmap?.width ?: 0
                val height = bitmap?.height ?: 0
                val fileSize = photoFile.length()

                // Generate thumbnail
                val thumbnailFile = generateThumbnail(context, bitmap, photoFile.nameWithoutExtension)

                // Create photo object
                val photo = Photo(
                    path = photoFile.absolutePath,
                    categoryId = categoryId,
                    name = photoFile.nameWithoutExtension,
                    isFromAssets = false,
                    createdAt = System.currentTimeMillis(),
                    fileSize = fileSize,
                    width = width,
                    height = height
                )

                // Save to database
                val photoId = photoRepository.insertPhoto(photo)

                // Update UI state with success
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    lastCapturedPhotoId = photoId,
                    showCaptureSuccess = true
                )

                // Clean up bitmap
                bitmap?.recycle()

            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    error = "Failed to capture photo: ${exception.message}"
                )
            }
        }
    }

    /**
     * Switch between front and back camera
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner) {
        if (!_isCameraInitialized.value || !_hasFrontCamera.value) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                cameraManager.switchCamera(lifecycleOwner)
                _isUsingFrontCamera.value = cameraManager.isUsingFrontCamera()

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to switch camera: ${exception.message}"
                )
            }
        }
    }

    /**
     * Toggle flash mode
     */
    fun toggleFlashMode() {
        if (!_isCameraInitialized.value) return

        val newFlashMode = cameraManager.toggleFlashMode()
        _flashMode.value = newFlashMode
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear capture success message
     */
    fun clearCaptureSuccess() {
        _uiState.value = _uiState.value.copy(showCaptureSuccess = false)
    }

    /**
     * Generate thumbnail for the captured photo
     */
    private suspend fun generateThumbnail(
        context: Context,
        originalBitmap: Bitmap?,
        photoName: String
    ): File? {
        if (originalBitmap == null) return null

        return try {
            // Create thumbnail directory
            val thumbnailDir = File(context.filesDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }

            // Calculate thumbnail size (maintaining aspect ratio)
            val thumbnailSize = 200
            val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val thumbnailWidth: Int
            val thumbnailHeight: Int

            if (aspectRatio > 1) {
                thumbnailWidth = thumbnailSize
                thumbnailHeight = (thumbnailSize / aspectRatio).toInt()
            } else {
                thumbnailWidth = (thumbnailSize * aspectRatio).toInt()
                thumbnailHeight = thumbnailSize
            }

            // Create thumbnail bitmap
            val thumbnailBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                thumbnailWidth,
                thumbnailHeight,
                true
            )

            // Save thumbnail
            val thumbnailFile = File(thumbnailDir, "${photoName}_thumb.jpg")
            FileOutputStream(thumbnailFile).use { outputStream ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            // Clean up
            thumbnailBitmap.recycle()

            thumbnailFile

        } catch (exception: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}

/**
 * UI State for the camera screen
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null,
    val lastCapturedPhotoId: Long? = null,
    val showCaptureSuccess: Boolean = false
)