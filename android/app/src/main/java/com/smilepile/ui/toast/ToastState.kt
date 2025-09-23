package com.smilepile.ui.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Toast configuration data class
 */
data class ToastData(
    val message: String,
    val duration: Long = TOAST_DURATION_DEFAULT,
    val backgroundColor: Color? = null,
    val action: ToastAction? = null,
    val type: ToastType = ToastType.DEFAULT
)

/**
 * Toast action for interactive toasts
 */
data class ToastAction(
    val label: String,
    val onPress: () -> Unit
)

/**
 * Toast types for different visual styles
 */
enum class ToastType {
    DEFAULT,
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Toast duration constants
 */
const val TOAST_DURATION_SHORT = 2000L
const val TOAST_DURATION_DEFAULT = 3000L
const val TOAST_DURATION_LONG = 5000L
const val TOAST_DURATION_PERSISTENT = 0L // Won't auto-hide

/**
 * State holder for toast notifications
 */
@Stable
class ToastState(
    private val scope: CoroutineScope
) {
    private val _currentToast = mutableStateOf<ToastData?>(null)
    val currentToast: ToastData? get() = _currentToast.value

    private val _isVisible = mutableStateOf(false)
    val isVisible: Boolean get() = _isVisible.value

    private var hideJob: Job? = null

    /**
     * Show a toast notification
     */
    fun showToast(
        message: String,
        duration: Long = TOAST_DURATION_DEFAULT,
        backgroundColor: Color? = null,
        action: ToastAction? = null,
        type: ToastType = ToastType.DEFAULT
    ) {
        showToast(
            ToastData(
                message = message,
                duration = duration,
                backgroundColor = backgroundColor,
                action = action,
                type = type
            )
        )
    }

    /**
     * Show a toast notification with ToastData
     */
    fun showToast(toastData: ToastData) {
        // Cancel any existing hide job
        hideJob?.cancel()

        // Update toast data
        _currentToast.value = toastData
        _isVisible.value = true

        // Schedule auto-hide if duration is not persistent
        if (toastData.duration != TOAST_DURATION_PERSISTENT) {
            hideJob = scope.launch {
                delay(toastData.duration)
                hideToast()
            }
        }
    }

    /**
     * Hide the current toast
     */
    fun hideToast() {
        hideJob?.cancel()
        _isVisible.value = false
        // Keep the toast data for animation out
        scope.launch {
            delay(300) // Animation duration
            _currentToast.value = null
        }
    }

    /**
     * Quick toast methods for common scenarios
     */
    fun showSuccess(message: String, action: ToastAction? = null) {
        showToast(
            message = message,
            type = ToastType.SUCCESS,
            action = action
        )
    }

    fun showError(message: String, duration: Long = TOAST_DURATION_LONG) {
        showToast(
            message = message,
            type = ToastType.ERROR,
            duration = duration
        )
    }

    fun showInfo(message: String) {
        showToast(
            message = message,
            type = ToastType.INFO
        )
    }

    fun showWarning(message: String) {
        showToast(
            message = message,
            type = ToastType.WARNING
        )
    }

    /**
     * Show loading toast (persistent until manually hidden)
     */
    fun showLoading(message: String) {
        showToast(
            message = message,
            duration = TOAST_DURATION_PERSISTENT,
            type = ToastType.INFO
        )
    }
}

/**
 * Remember toast state in composition
 */
@Composable
fun rememberToastState(
    scope: CoroutineScope
): ToastState {
    return remember { ToastState(scope) }
}