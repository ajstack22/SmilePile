package com.smilepile.app.managers

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.app.AlertDialog
import android.util.Log

class PermissionManager(
    private val context: Context,
    private val fragment: Fragment
) {
    companion object {
        private const val TAG = "PermissionManager"
        private const val PREFS_NAME = "permission_prefs"
        private const val KEY_PERMISSION_DENIED_COUNT = "permission_denied_count_"
        private const val KEY_PERMISSION_PERMANENTLY_DENIED = "permission_permanently_denied_"
        private const val MAX_DENIAL_COUNT = 2
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // Permission launcher for camera
    private val cameraPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    // Permission launcher for storage (legacy devices only)
    private val storagePermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(getStoragePermission(), isGranted)
        onPermissionResult?.invoke(isGranted)
    }

    // Permission launcher for READ_MEDIA_IMAGES (Android 13+)
    private val mediaImagesPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(Manifest.permission.READ_MEDIA_IMAGES, isGranted)
        onPermissionResult?.invoke(isGranted)
    }

    fun checkPhotoPickerPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13+, check if we need READ_MEDIA_IMAGES for file access
                // Note: Photo Picker itself doesn't need permissions, but accessing selected files might
                checkMediaImagesPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // For legacy devices, check if we have storage permission
                checkStoragePermission()
            }
            else -> {
                // No runtime permissions on Android 5 and below
                true
            }
        }
    }

    fun requestPhotoPickerPermissions(callback: (Boolean) -> Unit) {
        onPermissionResult = callback

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13+, request READ_MEDIA_IMAGES if needed
                if (checkMediaImagesPermission()) {
                    callback(true)
                } else {
                    requestMediaImagesPermission()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Request storage permission for legacy devices
                if (checkStoragePermission()) {
                    callback(true)
                } else {
                    requestStoragePermission()
                }
            }
            else -> {
                // No runtime permissions on Android 5 and below
                callback(true)
            }
        }
    }

    fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(callback: (Boolean) -> Unit) {
        onPermissionResult = callback

        if (checkCameraPermission()) {
            callback(true)
        } else {
            if (isPermissionPermanentlyDenied(Manifest.permission.CAMERA)) {
                showPermanentlyDeniedDialog("Camera") {
                    // Navigate to app settings
                    // This would be implemented in the calling fragment
                }
                callback(false)
            } else if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showCameraRationale {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On Android 13+, check READ_MEDIA_IMAGES instead
            checkMediaImagesPermission()
        }
    }

    private fun checkMediaImagesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older versions, fall back to READ external storage
            checkStoragePermission()
        }
    }

    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun requestStoragePermission() {
        val permission = getStoragePermission()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestMediaImagesPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (isPermissionPermanentlyDenied(permission)) {
                    showPermanentlyDeniedDialog("Photo Access") {
                        // Navigate to app settings
                        // This would be implemented in the calling fragment
                    }
                    onPermissionResult?.invoke(false)
                } else if (fragment.shouldShowRequestPermissionRationale(permission)) {
                    showStorageRationale {
                        storagePermissionLauncher.launch(permission)
                    }
                } else {
                    storagePermissionLauncher.launch(permission)
                }
            }
        }
    }

    private fun requestMediaImagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.READ_MEDIA_IMAGES

            if (isPermissionPermanentlyDenied(permission)) {
                showPermanentlyDeniedDialog("Photo Access") {
                    // Navigate to app settings
                    // This would be implemented in the calling fragment
                }
                onPermissionResult?.invoke(false)
            } else if (fragment.shouldShowRequestPermissionRationale(permission)) {
                showStorageRationale {
                    mediaImagesPermissionLauncher.launch(permission)
                }
            } else {
                mediaImagesPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showCameraRationale(onAccept: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Camera Permission Needed")
            .setMessage("To take new photos for your collection, SmilePile needs access to your camera. Your photos will stay private on your device and won't be shared online.")
            .setPositiveButton("Allow Camera") { _, _ -> onAccept() }
            .setNegativeButton("Maybe Later") { _, _ -> onPermissionResult?.invoke(false) }
            .setCancelable(false)
            .show()
    }

    private fun showStorageRationale(onAccept: () -> Unit) {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "To help you select photos from your gallery, SmilePile needs permission to access your images. We only access photos you specifically choose, and they stay private on your device."
        } else {
            "To help you select photos from your gallery, SmilePile needs storage permission. We only access photos you specifically choose, and they stay private on your device."
        }

        AlertDialog.Builder(context)
            .setTitle("Photo Access Permission")
            .setMessage(message)
            .setPositiveButton("Allow Access") { _, _ -> onAccept() }
            .setNegativeButton("Maybe Later") { _, _ -> onPermissionResult?.invoke(false) }
            .setCancelable(false)
            .show()
    }

    private fun showPermanentlyDeniedDialog(permissionName: String, onSettingsClick: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("$permissionName Permission Required")
            .setMessage("$permissionName permission has been permanently denied. To use this feature, please enable the permission in your device settings.")
            .setPositiveButton("Open Settings") { _, _ -> onSettingsClick() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handlePermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted) {
            incrementDenialCount(permission)
            Log.d(TAG, "Permission $permission denied. Count: ${getDenialCount(permission)}")
        } else {
            resetDenialCount(permission)
            Log.d(TAG, "Permission $permission granted")
        }
    }

    private fun incrementDenialCount(permission: String) {
        val currentCount = getDenialCount(permission)
        val newCount = currentCount + 1

        sharedPrefs.edit()
            .putInt(KEY_PERMISSION_DENIED_COUNT + permission, newCount)
            .apply()

        if (newCount >= MAX_DENIAL_COUNT) {
            markPermissionPermanentlyDenied(permission)
        }
    }

    private fun getDenialCount(permission: String): Int {
        return sharedPrefs.getInt(KEY_PERMISSION_DENIED_COUNT + permission, 0)
    }

    private fun resetDenialCount(permission: String) {
        sharedPrefs.edit()
            .remove(KEY_PERMISSION_DENIED_COUNT + permission)
            .remove(KEY_PERMISSION_PERMANENTLY_DENIED + permission)
            .apply()
    }

    private fun markPermissionPermanentlyDenied(permission: String) {
        sharedPrefs.edit()
            .putBoolean(KEY_PERMISSION_PERMANENTLY_DENIED + permission, true)
            .apply()
    }

    private fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return sharedPrefs.getBoolean(KEY_PERMISSION_PERMANENTLY_DENIED + permission, false)
    }

    /**
     * Get detailed permission status for UI display
     */
    fun getPermissionStatus(): PermissionStatus {
        val photoPickerGranted = checkPhotoPickerPermissions()
        val cameraGranted = checkCameraPermission()

        return PermissionStatus(
            photoPickerPermission = if (photoPickerGranted) PermissionState.GRANTED else PermissionState.DENIED,
            cameraPermission = if (cameraGranted) PermissionState.GRANTED else PermissionState.DENIED,
            androidVersion = Build.VERSION.SDK_INT,
            usingModernPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }

    data class PermissionStatus(
        val photoPickerPermission: PermissionState,
        val cameraPermission: PermissionState,
        val androidVersion: Int,
        val usingModernPermissions: Boolean
    )

    enum class PermissionState {
        GRANTED,
        DENIED,
        PERMANENTLY_DENIED
    }
}