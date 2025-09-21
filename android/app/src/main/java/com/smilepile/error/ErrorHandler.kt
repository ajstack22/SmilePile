package com.smilepile.error

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global error handling for the SmilePile app
 */
object ErrorHandler {

    private const val TAG = "SmilePile"

    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    /**
     * Error states that can be displayed to users
     */
    sealed class ErrorState {
        data class NetworkError(val message: String) : ErrorState()
        data class StorageError(val message: String) : ErrorState()
        data class DatabaseError(val message: String) : ErrorState()
        data class PermissionError(val message: String) : ErrorState()
        data class GenericError(val message: String) : ErrorState()
    }

    /**
     * Handle different types of errors
     */
    fun handleError(throwable: Throwable, context: Context? = null) {
        Log.e(TAG, "Error occurred", throwable)

        val errorState = when (throwable) {
            is java.io.IOException -> ErrorState.NetworkError(
                "Network connection error. Please check your connection."
            )
            is android.database.sqlite.SQLiteException -> ErrorState.DatabaseError(
                "Database error occurred. Please restart the app."
            )
            is SecurityException -> ErrorState.PermissionError(
                "Permission required to perform this action."
            )
            is OutOfMemoryError -> ErrorState.StorageError(
                "Not enough memory. Please free up some space."
            )
            else -> ErrorState.GenericError(
                throwable.message ?: "An unexpected error occurred."
            )
        }

        _errorState.value = errorState

        // Show toast for immediate feedback if context is available
        context?.let {
            showErrorToast(it, errorState)
        }
    }

    /**
     * Clear current error state
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * Show error toast
     */
    private fun showErrorToast(context: Context, error: ErrorState) {
        val message = when (error) {
            is ErrorState.NetworkError -> error.message
            is ErrorState.StorageError -> error.message
            is ErrorState.DatabaseError -> error.message
            is ErrorState.PermissionError -> error.message
            is ErrorState.GenericError -> error.message
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Create a coroutine exception handler
     */
    fun createExceptionHandler(context: Context? = null): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleError(throwable, context)
        }
    }

    /**
     * Check if storage space is available
     */
    fun checkStorageSpace(context: Context): Boolean {
        val path = context.filesDir
        val stat = android.os.StatFs(path.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val minRequiredBytes = 50 * 1024 * 1024 // 50MB minimum

        if (availableBytes < minRequiredBytes) {
            handleError(
                OutOfMemoryError("Low storage space. Please free up some space."),
                context
            )
            return false
        }
        return true
    }

    /**
     * Handle database corruption
     */
    fun handleDatabaseCorruption(context: Context) {
        Log.e(TAG, "Database corruption detected")
        _errorState.value = ErrorState.DatabaseError(
            "Database error detected. The app will reset its data."
        )
        // In a real app, you might want to backup and recreate the database
    }

    /**
     * User-friendly error messages
     */
    object ErrorMessages {
        const val CAMERA_ERROR = "Unable to access camera. Please check permissions."
        const val PHOTO_LOAD_ERROR = "Unable to load photo. Please try again."
        const val SAVE_ERROR = "Unable to save changes. Please try again."
        const val DELETE_ERROR = "Unable to delete item. Please try again."
        const val IMPORT_ERROR = "Unable to import photos. Please try again."
        const val NETWORK_ERROR = "No internet connection available."
        const val GENERIC_ERROR = "Something went wrong. Please try again."
    }
}