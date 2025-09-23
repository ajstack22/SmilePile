package com.smilepile.security

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * InactivityManager handles automatic timeout to Kids Mode after inactivity
 * Provides security by ensuring the app returns to safe mode when left unattended
 */
@Singleton
class InactivityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferencesManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "InactivityManager"
        private const val DEFAULT_TIMEOUT_MS = 300_000L // 5 minutes
        private const val MIN_TIMEOUT_MS = 60_000L // 1 minute
        private const val MAX_TIMEOUT_MS = 1_800_000L // 30 minutes
        private const val KEY_TIMEOUT_ENABLED = "inactivity_timeout_enabled"
        private const val KEY_TIMEOUT_DURATION = "inactivity_timeout_duration"
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timeoutJob: Job? = null
    private var lastActivityTime = System.currentTimeMillis()
    private var isAppInForeground = true

    // State for timeout events
    private val _timeoutTriggered = MutableStateFlow(false)
    val timeoutTriggered: StateFlow<Boolean> = _timeoutTriggered.asStateFlow()

    // State for remaining time (for UI indicators)
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    // Callback for when timeout occurs
    private var onTimeoutCallback: (() -> Unit)? = null

    private var isInitialized = false

    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            resetTimer()
        }
    }

    /**
     * Record user activity and reset the inactivity timer
     */
    fun recordActivity() {
        if (!isTimeoutEnabled()) return

        lastActivityTime = System.currentTimeMillis()
        resetTimer()
        Log.d(TAG, "User activity recorded, timer reset")
    }

    /**
     * Set the callback to execute when timeout occurs
     */
    fun setTimeoutCallback(callback: () -> Unit) {
        onTimeoutCallback = callback
    }

    // Direct preferences access for timeout settings
    private val prefs = context.getSharedPreferences("inactivity_prefs", Context.MODE_PRIVATE)

    /**
     * Enable or disable inactivity timeout
     */
    fun setTimeoutEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_TIMEOUT_ENABLED, enabled)
            .apply()

        if (enabled) {
            resetTimer()
        } else {
            cancelTimer()
        }
        Log.d(TAG, "Inactivity timeout ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if inactivity timeout is enabled
     */
    fun isTimeoutEnabled(): Boolean {
        return prefs.getBoolean(KEY_TIMEOUT_ENABLED, true) // Default enabled for security
    }

    /**
     * Set the timeout duration in milliseconds
     */
    fun setTimeoutDuration(durationMs: Long) {
        val clampedDuration = durationMs.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)
        prefs.edit()
            .putLong(KEY_TIMEOUT_DURATION, clampedDuration)
            .apply()

        if (isTimeoutEnabled()) {
            resetTimer()
        }
        Log.d(TAG, "Timeout duration set to ${clampedDuration}ms")
    }

    /**
     * Get the current timeout duration
     */
    fun getTimeoutDuration(): Long {
        return prefs.getLong(KEY_TIMEOUT_DURATION, DEFAULT_TIMEOUT_MS)
    }

    /**
     * Get formatted timeout duration for UI display
     */
    fun getTimeoutDurationFormatted(): String {
        val durationMs = getTimeoutDuration()
        val minutes = durationMs / 60_000
        return "${minutes} minutes"
    }

    /**
     * Reset the inactivity timer
     */
    private fun resetTimer() {
        if (!isTimeoutEnabled()) return

        cancelTimer()

        val timeoutDuration = getTimeoutDuration()
        timeoutJob = scope.launch {
            var remainingMs = timeoutDuration

            while (remainingMs > 0 && isAppInForeground) {
                _remainingTime.value = remainingMs
                delay(1000) // Update every second
                remainingMs -= 1000

                // Check if activity was recorded during countdown
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastActivityTime < 1000) {
                    // Recent activity detected, restart timer
                    remainingMs = timeoutDuration
                    lastActivityTime = currentTime
                }
            }

            // Timeout reached
            if (remainingMs <= 0 && isAppInForeground) {
                triggerTimeout()
            }
        }
    }

    /**
     * Cancel the current timer
     */
    private fun cancelTimer() {
        timeoutJob?.cancel()
        timeoutJob = null
        _remainingTime.value = 0L
    }

    /**
     * Trigger the timeout action
     */
    private fun triggerTimeout() {
        Log.i(TAG, "Inactivity timeout triggered, returning to Kids Mode")
        _timeoutTriggered.value = true
        onTimeoutCallback?.invoke()

        // Reset the state after a short delay
        scope.launch {
            delay(1000)
            _timeoutTriggered.value = false
        }
    }

    /**
     * Pause timeout when app goes to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        cancelTimer()
        Log.d(TAG, "App moved to background, pausing timeout")
    }

    /**
     * Resume timeout when app comes to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        recordActivity() // Reset timer when returning to foreground
        Log.d(TAG, "App moved to foreground, resuming timeout")
    }

    /**
     * Get available timeout duration options for settings
     */
    fun getTimeoutOptions(): List<Pair<String, Long>> {
        return listOf(
            "1 minute" to 60_000L,
            "2 minutes" to 120_000L,
            "5 minutes" to 300_000L,
            "10 minutes" to 600_000L,
            "15 minutes" to 900_000L,
            "30 minutes" to 1_800_000L
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        cancelTimer()
        onTimeoutCallback = null
        isInitialized = false
    }
}