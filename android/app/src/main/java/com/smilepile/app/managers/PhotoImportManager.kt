package com.smilepile.app.managers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

class PhotoImportManager(
    private val context: Context,
    private val fragment: Fragment
) {
    companion object {
        private const val MAX_PHOTOS = 50 // Child-friendly limit
        private const val TAG = "PhotoImportManager"
    }

    private val permissionManager = PermissionManager(context, fragment)
    private val isPhotoPickerAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var onPhotosSelected: ((List<Uri>) -> Unit)? = null
    var onPhotosCaptured: ((Uri) -> Unit)? = null

    // Modern Photo Picker (Android 13+)
    private val modernPicker = fragment.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            processSelectedPhotos(uris)
        }
    }

    // Legacy picker for older devices
    private val legacyPicker = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleLegacyResult(result.data)
        }
    }

    // Camera capture launcher
    private val cameraLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            showCaptureSuccessMessage()
        }
    }

    fun selectPhotos() {
        // Check permissions first
        permissionManager.requestPhotoPickerPermissions { granted ->
            if (granted) {
                if (isPhotoPickerAvailable) {
                    launchModernPicker()
                } else {
                    launchLegacyPicker()
                }
            } else {
                showPermissionDeniedMessage()
            }
        }
    }

    fun capturePhotoFromCamera(callback: (Uri) -> Unit) {
        onPhotosCaptured = callback

        permissionManager.requestCameraPermission { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                showCameraPermissionDeniedMessage()
            }
        }
    }

    private fun launchModernPicker() {
        modernPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun launchLegacyPicker() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        legacyPicker.launch(Intent.createChooser(intent, "Select Photos"))
    }

    private fun handleLegacyResult(data: Intent?) {
        val uris = mutableListOf<Uri>()

        data?.let {
            // Handle multiple selection
            it.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount.coerceAtMost(MAX_PHOTOS)) {
                    clipData.getItemAt(i).uri?.let { uri ->
                        uris.add(uri)
                    }
                }
            } ?: it.data?.let { uri ->
                // Single selection
                uris.add(uri)
            }
        }

        if (uris.isNotEmpty()) {
            processSelectedPhotos(uris)
        }
    }

    private fun processSelectedPhotos(uris: List<Uri>) {
        // Validate and filter URIs
        val validUris = uris.filter { isValidImageUri(it) }
        onPhotosSelected?.invoke(validUris)
    }

    private fun isValidImageUri(uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchCameraCapture() {
        try {
            val photoFile = createTempPhotoFile()
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            showCameraErrorMessage()
        }
    }

    private fun createTempPhotoFile(): File {
        val storageDir = File(context.cacheDir, "temp_photos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            "captured_photo_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
    }

    fun getPhotoPickerStatus(): String {
        return if (isPhotoPickerAvailable) {
            "✓ Photo selection: No permissions needed (Android 13+)"
        } else {
            val hasPermission = permissionManager.checkPhotoPickerPermissions()
            if (hasPermission) {
                "✓ Photo selection: Permission granted"
            } else {
                "○ Photo selection: Permission needed"
            }
        }
    }

    fun getCameraStatus(): String {
        return if (permissionManager.checkCameraPermission()) {
            "✓ Camera: Permission granted"
        } else {
            "○ Camera: Permission needed"
        }
    }

    private fun showPermissionDeniedMessage() {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "To select photos, please allow access to images in your device settings. Your photos stay private on your device."
        } else {
            "To select photos, please allow storage permission in your device settings. Your photos stay private on your device."
        }

        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showCameraPermissionDeniedMessage() {
        Toast.makeText(
            context,
            "To take photos, please allow camera permission in your device settings. Photos are stored privately on your device.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showCameraErrorMessage() {
        Toast.makeText(
            context,
            "Cannot open camera",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showCaptureSuccessMessage() {
        Toast.makeText(
            context,
            "Photo captured successfully",
            Toast.LENGTH_SHORT
        ).show()
    }

    suspend fun copyPhotoToInternalStorage(
        sourceUri: Uri,
        categoryId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val categoryDir = File(context.filesDir, "photos/$categoryId")
            categoryDir.mkdirs()

            val destFile = File(categoryDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}