package com.smilepile.security

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.AppModeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base secure activity that provides security features
 * Screenshot prevention has been removed to allow sharing moments
 */
@AndroidEntryPoint
abstract class SecureActivity : ComponentActivity() {

    @Inject
    lateinit var securePreferences: SecurePreferencesManager

    private val appModeViewModel: AppModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe mode changes for security updates
        observeModeChanges()
    }

    override fun onResume() {
        super.onResume()
        // Activity resumed
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // User interaction detected
    }

    override fun onPause() {
        super.onPause()
        // Activity is pausing
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity destroyed
    }

    /**
     * Observe app mode changes to apply appropriate security measures
     */
    private fun observeModeChanges() {
        lifecycleScope.launch {
            appModeViewModel.uiState
                .distinctUntilChanged { old, new -> old.currentMode == new.currentMode }
                .collect { state ->
                    applySecurityForMode(state.currentMode)
                }
        }
    }

    /**
     * Apply security measures based on current app mode
     */
    private fun applySecurityForMode(mode: AppMode) {
        // Screenshot prevention removed - users can take screenshots in any mode
        when (mode) {
            AppMode.PARENT -> {
                // Parent mode active
            }
            AppMode.KIDS -> {
                // Kids mode active
            }
        }
    }

    // Screenshot prevention methods removed - no longer needed

    /**
     * Allow subclasses to customize security behavior
     */
    protected open fun onSecurityModeChanged(mode: AppMode) {
        // Override in subclasses if needed
    }

    /**
     * Allow subclasses to handle timeout events
     */
    protected open fun onInactivityTimeout() {
        // Override in subclasses if needed
    }

    /**
     * Get the current security summary
     */
    protected fun getSecuritySummary(): SecuritySummary {
        return securePreferences.getSecuritySummary()
    }
}