package com.smilepile.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages CameraX functionality for the SmilePile app
 * Handles camera lifecycle, preview, and image capture
 */
@Singleton
class CameraManager @Inject constructor() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    // Current camera selector (front/back)
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Flash mode state
    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO

    /**
     * Initialize the camera with preview
     */
    suspend fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = withContext(Dispatchers.Main) {
        try {
            // Get camera provider
            cameraProvider = getCameraProvider(context)

            // Setup preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Setup image capture
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setFlashMode(flashMode)
                .build()

            // Bind use cases to camera
            bindCameraUseCases(lifecycleOwner)

        } catch (exception: Exception) {
            throw CameraException("Failed to initialize camera: ${exception.message}", exception)
        }
    }

    /**
     * Capture a photo and save it to internal storage
     */
    suspend fun capturePhoto(context: Context): Uri = withContext(Dispatchers.IO) {
        val imageCapture = imageCapture ?: throw CameraException("Camera not initialized")

        try {
            // Create output file in app's internal storage
            val photoFile = createPhotoFile(context)

            // Create output file options
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Capture image
            suspendCancellableCoroutine { continuation ->
                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            continuation.resume(Uri.fromFile(photoFile))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            continuation.resumeWithException(
                                CameraException("Failed to capture photo: ${exception.message}", exception)
                            )
                        }
                    }
                )
            }
        } catch (exception: Exception) {
            throw CameraException("Failed to capture photo: ${exception.message}", exception)
        }
    }

    /**
     * Switch between front and back camera
     */
    suspend fun switchCamera(lifecycleOwner: LifecycleOwner) = withContext(Dispatchers.Main) {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Rebind camera with new selector
        bindCameraUseCases(lifecycleOwner)
    }

    /**
     * Toggle flash mode (off -> auto -> on -> off)
     */
    fun toggleFlashMode(): FlashMode {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }

        // Update image capture configuration
        imageCapture?.flashMode = flashMode

        return when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> FlashMode.OFF
            ImageCapture.FLASH_MODE_AUTO -> FlashMode.AUTO
            ImageCapture.FLASH_MODE_ON -> FlashMode.ON
            else -> FlashMode.AUTO
        }
    }

    /**
     * Check if device has front camera
     */
    fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Check if device has back camera
     */
    fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * Check if currently using front camera
     */
    fun isUsingFrontCamera(): Boolean {
        return cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }

    /**
     * Get current flash mode
     */
    fun getCurrentFlashMode(): FlashMode {
        return when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> FlashMode.OFF
            ImageCapture.FLASH_MODE_AUTO -> FlashMode.AUTO
            ImageCapture.FLASH_MODE_ON -> FlashMode.ON
            else -> FlashMode.AUTO
        }
    }

    /**
     * Release camera resources
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        preview = null
        imageCapture = null
        camera = null
    }

    // Private helper methods

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exception: Exception) {
            throw CameraException("Failed to bind camera use cases: ${exception.message}", exception)
        }
    }

    private fun createPhotoFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())
        val filename = "SmilePile_$timestamp.jpg"

        // Save to app's internal storage in a photos directory
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }

        return File(photosDir, filename)
    }
}

/**
 * Flash mode enum for UI
 */
enum class FlashMode {
    OFF, AUTO, ON
}

/**
 * Custom exception for camera-related errors
 */
class CameraException(message: String, cause: Throwable? = null) : Exception(message, cause)