package com.smilepile.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Utility class for handling storage permissions for photo import functionality.
 * Handles different permission requirements for Android 13+ (READ_MEDIA_IMAGES)
 * and older versions (READ_EXTERNAL_STORAGE).
 */
object PermissionHandler {

    /**
     * Get the appropriate storage permission based on Android version
     */
    val storagePermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * Check if storage permission is granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            storagePermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get user-friendly permission name for display
     */
    fun getPermissionDisplayName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "Photos and media"
        } else {
            "Storage"
        }
    }

    /**
     * Open app settings to allow user to manually grant permissions
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Data class representing the state of storage permission
 */
data class StoragePermissionState(
    val isGranted: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val permissionName: String = PermissionHandler.getPermissionDisplayName()
)

/**
 * Composable function to handle storage permission with rationale dialogs
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberStoragePermissionState(
    onPermissionResult: (StoragePermissionState) -> Unit = {}
): StoragePermissionState {
    val context = LocalContext.current
    val permission = PermissionHandler.storagePermission

    val permissionState = rememberPermissionState(permission)
    var hasRequestedOnce by remember { mutableStateOf(false) }

    // Track permission state changes
    LaunchedEffect(permissionState.status) {
        val state = StoragePermissionState(
            isGranted = permissionState.status.isGranted,
            shouldShowRationale = permissionState.status.shouldShowRationale,
            isPermanentlyDenied = hasRequestedOnce &&
                !permissionState.status.isGranted &&
                !permissionState.status.shouldShowRationale,
            permissionName = PermissionHandler.getPermissionDisplayName()
        )
        onPermissionResult(state)
    }

    return StoragePermissionState(
        isGranted = permissionState.status.isGranted,
        shouldShowRationale = permissionState.status.shouldShowRationale,
        isPermanentlyDenied = hasRequestedOnce &&
            !permissionState.status.isGranted &&
            !permissionState.status.shouldShowRationale,
        permissionName = PermissionHandler.getPermissionDisplayName()
    )
}

/**
 * Extension function to request permission with tracking
 */
@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.requestPermissionWithTracking(
    onRequested: () -> Unit = {}
) {
    launchPermissionRequest()
    onRequested()
}

/**
 * Composable function to get a permission launcher for manual control
 */
@Composable
fun rememberStoragePermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
}

/**
 * Permission rationale messages
 */
object PermissionRationale {
    const val STORAGE_PERMISSION_TITLE = "Photos Access Required"

    val STORAGE_PERMISSION_MESSAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "SmilePile needs access to your photos to import them into your gallery. " +
        "This permission allows you to select and organize your favorite photos."
    } else {
        "SmilePile needs storage access to import photos from your device. " +
        "This permission allows you to select and organize your favorite photos."
    }

    const val PERMISSION_DENIED_TITLE = "Permission Required"
    const val PERMISSION_DENIED_MESSAGE =
        "To import photos, please grant the required permission in Settings."

    const val SETTINGS_BUTTON_TEXT = "Open Settings"
    const val CANCEL_BUTTON_TEXT = "Cancel"
    const val GRANT_BUTTON_TEXT = "Grant Permission"
}