package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.security.SecuritySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for parental controls
 */
data class ParentalControlsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val authenticationMode: AuthenticationMode = AuthenticationMode.PIN,
    val securitySummary: SecuritySummary? = null,
    val showInitialSetup: Boolean = false,
    val pinInput: String = "",
    val patternInput: List<Int> = emptyList(),
    val isInCooldown: Boolean = false,
    val cooldownTimeRemaining: Long = 0L,
    val failedAttempts: Int = 0
)

/**
 * Authentication modes available for parental controls
 */
enum class AuthenticationMode {
    PIN,
    PATTERN
}

/**
 * UI state for parental lock screen
 */
data class ParentalLockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val authenticationMode: AuthenticationMode = AuthenticationMode.PIN,
    val pinInput: String = "",
    val patternInput: List<Int> = emptyList(),
    val isInCooldown: Boolean = false,
    val cooldownTimeRemaining: Long = 0L,
    val failedAttempts: Int = 0,
    val maxAttempts: Int = 5,
    val showKidFriendlyMessage: Boolean = false
)

/**
 * ViewModel for managing parental controls authentication and settings
 */
@HiltViewModel
class ParentalControlsViewModel @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentalControlsUiState())
    val uiState: StateFlow<ParentalControlsUiState> = _uiState.asStateFlow()

    private val _lockUiState = MutableStateFlow(ParentalLockUiState())
    val lockUiState: StateFlow<ParentalLockUiState> = _lockUiState.asStateFlow()

    init {
        loadInitialState()
        startCooldownTimer()
    }

    /**
     * Load initial state and check if setup is needed
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val securitySummary = securePreferencesManager.getSecuritySummary()
                val hasParentalLock = securePreferencesManager.hasParentalLock()
                val showInitialSetup = !hasParentalLock

                // Determine default authentication mode
                val authMode = when {
                    securitySummary.hasPIN -> AuthenticationMode.PIN
                    securitySummary.hasPattern -> AuthenticationMode.PATTERN
                    else -> AuthenticationMode.PIN // Default for setup
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    securitySummary = securitySummary,
                    showInitialSetup = showInitialSetup,
                    authenticationMode = authMode,
                    isInCooldown = securitySummary.isInCooldown,
                    failedAttempts = securitySummary.failedAttempts
                )

                _lockUiState.value = _lockUiState.value.copy(
                    authenticationMode = authMode,
                    isInCooldown = securitySummary.isInCooldown,
                    failedAttempts = securitySummary.failedAttempts,
                    showKidFriendlyMessage = securitySummary.isInCooldown
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load parental controls"
                )
            }
        }
    }

    /**
     * Start cooldown timer to update remaining time
     */
    private fun startCooldownTimer() {
        viewModelScope.launch {
            combine(
                securePreferencesManager.kidSafeModeEnabled,
                securePreferencesManager.cameraAccessAllowed,
                securePreferencesManager.deleteProtectionEnabled
            ) { kidSafe, camera, delete ->
                // Update states when preferences change
                loadInitialState()
            }
        }
    }

    /**
     * PIN Authentication
     */
    fun updatePinInput(pin: String) {
        if (pin.length <= 6) { // Limit to 6 digits
            _uiState.value = _uiState.value.copy(pinInput = pin)
            _lockUiState.value = _lockUiState.value.copy(pinInput = pin)
        }
    }

    fun clearPinInput() {
        _uiState.value = _uiState.value.copy(pinInput = "")
        _lockUiState.value = _lockUiState.value.copy(pinInput = "")
    }

    fun validatePin(): Boolean {
        return validateCurrentPin(_uiState.value.pinInput)
    }

    private fun validateCurrentPin(pin: String): Boolean {
        if (pin.length < 4) return false

        return if (securePreferencesManager.validatePIN(pin)) {
            handleSuccessfulAuthentication()
            true
        } else {
            handleFailedAuthentication()
            false
        }
    }

    /**
     * Pattern Authentication
     */
    fun updatePatternInput(pattern: List<Int>) {
        if (pattern.size <= 9) { // Limit to 3x3 grid
            _uiState.value = _uiState.value.copy(patternInput = pattern)
            _lockUiState.value = _lockUiState.value.copy(patternInput = pattern)
        }
    }

    fun clearPatternInput() {
        _uiState.value = _uiState.value.copy(patternInput = emptyList())
        _lockUiState.value = _lockUiState.value.copy(patternInput = emptyList())
    }

    fun validatePattern(): Boolean {
        return validateCurrentPattern(_uiState.value.patternInput)
    }

    private fun validateCurrentPattern(pattern: List<Int>): Boolean {
        if (pattern.size < 4) return false

        return if (securePreferencesManager.validatePattern(pattern)) {
            handleSuccessfulAuthentication()
            true
        } else {
            handleFailedAuthentication()
            false
        }
    }

    /**
     * Authentication Mode Management
     */
    fun switchAuthenticationMode() {
        val newMode = when (_uiState.value.authenticationMode) {
            AuthenticationMode.PIN -> {
                if (securePreferencesManager.isPatternEnabled()) {
                    AuthenticationMode.PATTERN
                } else {
                    AuthenticationMode.PIN
                }
            }
            AuthenticationMode.PATTERN -> {
                if (securePreferencesManager.isPINEnabled()) {
                    AuthenticationMode.PIN
                } else {
                    AuthenticationMode.PATTERN
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            authenticationMode = newMode,
            pinInput = "",
            patternInput = emptyList()
        )

        _lockUiState.value = _lockUiState.value.copy(
            authenticationMode = newMode,
            pinInput = "",
            patternInput = emptyList()
        )
    }

    /**
     * Setup New PIN/Pattern
     */
    fun setupPIN(pin: String) {
        viewModelScope.launch {
            try {
                if (pin.length < 4 || pin.length > 6) {
                    _uiState.value = _uiState.value.copy(
                        error = "PIN must be between 4 and 6 digits"
                    )
                    return@launch
                }

                securePreferencesManager.setPIN(pin)
                loadInitialState()

                _uiState.value = _uiState.value.copy(
                    pinInput = "",
                    showInitialSetup = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to setup PIN"
                )
            }
        }
    }

    fun setupPattern(pattern: List<Int>) {
        viewModelScope.launch {
            try {
                if (pattern.size < 4) {
                    _uiState.value = _uiState.value.copy(
                        error = "Pattern must connect at least 4 dots"
                    )
                    return@launch
                }

                securePreferencesManager.setPattern(pattern)
                loadInitialState()

                _uiState.value = _uiState.value.copy(
                    patternInput = emptyList(),
                    showInitialSetup = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to setup pattern"
                )
            }
        }
    }

    /**
     * Settings Management
     */
    fun toggleKidSafeMode() {
        viewModelScope.launch {
            val current = securePreferencesManager.getKidSafeModeEnabled()
            securePreferencesManager.setKidSafeModeEnabled(!current)
            loadInitialState()
        }
    }

    fun toggleCameraAccess() {
        viewModelScope.launch {
            val current = securePreferencesManager.getCameraAccessAllowed()
            securePreferencesManager.setCameraAccessAllowed(!current)
            loadInitialState()
        }
    }

    fun toggleDeleteProtection() {
        viewModelScope.launch {
            val current = securePreferencesManager.getDeleteProtectionEnabled()
            securePreferencesManager.setDeleteProtectionEnabled(!current)
            loadInitialState()
        }
    }

    fun changePIN(newPin: String) {
        viewModelScope.launch {
            try {
                if (newPin.length < 4 || newPin.length > 6) {
                    _uiState.value = _uiState.value.copy(
                        error = "PIN must be between 4 and 6 digits"
                    )
                    return@launch
                }

                securePreferencesManager.setPIN(newPin)
                loadInitialState()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to change PIN"
                )
            }
        }
    }

    fun changePattern(newPattern: List<Int>) {
        viewModelScope.launch {
            try {
                if (newPattern.size < 4) {
                    _uiState.value = _uiState.value.copy(
                        error = "Pattern must connect at least 4 dots"
                    )
                    return@launch
                }

                securePreferencesManager.setPattern(newPattern)
                loadInitialState()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to change pattern"
                )
            }
        }
    }

    /**
     * Authentication Result Handling
     */
    private fun handleSuccessfulAuthentication() {
        securePreferencesManager.resetFailedAttempts()
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            error = null,
            pinInput = "",
            patternInput = emptyList(),
            failedAttempts = 0,
            isInCooldown = false
        )
        loadInitialState()
    }

    private fun handleFailedAuthentication() {
        securePreferencesManager.recordFailedAttempt()
        val attempts = securePreferencesManager.getFailedAttempts()
        val isInCooldown = securePreferencesManager.isInCooldown()

        _uiState.value = _uiState.value.copy(
            error = "Incorrect ${_uiState.value.authenticationMode.name.lowercase()}. Try again.",
            pinInput = "",
            patternInput = emptyList(),
            failedAttempts = attempts,
            isInCooldown = isInCooldown
        )

        _lockUiState.value = _lockUiState.value.copy(
            error = "Try again!",
            pinInput = "",
            patternInput = emptyList(),
            failedAttempts = attempts,
            isInCooldown = isInCooldown,
            showKidFriendlyMessage = isInCooldown
        )
    }

    /**
     * Utility Methods
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _lockUiState.value = _lockUiState.value.copy(error = null)
    }

    fun refreshCooldownTime() {
        viewModelScope.launch {
            val remainingTime = securePreferencesManager.getRemainingCooldownTime()
            val isInCooldown = securePreferencesManager.isInCooldown()

            _uiState.value = _uiState.value.copy(
                cooldownTimeRemaining = remainingTime,
                isInCooldown = isInCooldown
            )

            _lockUiState.value = _lockUiState.value.copy(
                cooldownTimeRemaining = remainingTime,
                isInCooldown = isInCooldown,
                showKidFriendlyMessage = isInCooldown
            )
        }
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = false,
            pinInput = "",
            patternInput = emptyList()
        )
    }

    /**
     * For development/testing - clear all settings
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            securePreferencesManager.clearAllSettings()
            loadInitialState()
        }
    }
}