package com.smilepile.ui.toast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global Toast Manager
 * Singleton that manages toast notifications across the app
 */
@Singleton
class ToastManager @Inject constructor() {

    private val _toastEvent = MutableStateFlow<ToastEvent?>(null)
    val toastEvent: StateFlow<ToastEvent?> = _toastEvent.asStateFlow()

    /**
     * Show a toast notification
     */
    fun showToast(
        message: String,
        duration: Long = TOAST_DURATION_DEFAULT,
        type: ToastType = ToastType.DEFAULT,
        action: ToastAction? = null
    ) {
        _toastEvent.value = ToastEvent(
            id = System.currentTimeMillis(),
            data = ToastData(
                message = message,
                duration = duration,
                type = type,
                action = action
            )
        )
    }

    /**
     * Quick methods for common toasts
     */
    fun showSuccess(message: String) {
        showToast(message, type = ToastType.SUCCESS)
    }

    fun showError(message: String) {
        showToast(message, type = ToastType.ERROR, duration = TOAST_DURATION_LONG)
    }

    fun showInfo(message: String) {
        showToast(message, type = ToastType.INFO)
    }

    fun showWarning(message: String) {
        showToast(message, type = ToastType.WARNING)
    }

    /**
     * Clear the current toast event
     */
    fun clearToast() {
        _toastEvent.value = null
    }
}

/**
 * Toast event wrapper with unique ID for recomposition
 */
data class ToastEvent(
    val id: Long,
    val data: ToastData
)