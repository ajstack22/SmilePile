package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.mode.AppMode
import com.smilepile.mode.ModeManager
import com.smilepile.security.SecurePreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppModeUiState(
    val currentMode: AppMode = AppMode.KIDS,
    val isTransitioning: Boolean = false,
    val requiresPinAuth: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppModeViewModel @Inject constructor(
    private val modeManager: ModeManager,
    private val securePreferences: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppModeUiState())
    val uiState: StateFlow<AppModeUiState> = _uiState.asStateFlow()

    init {
        observeModeChanges()
    }

    private fun observeModeChanges() {
        viewModelScope.launch {
            modeManager.currentMode.collect { mode ->
                _uiState.value = _uiState.value.copy(currentMode = mode)
            }
        }
    }

    fun requestModeToggle() {
        val currentMode = _uiState.value.currentMode

        if (currentMode == AppMode.KIDS && securePreferences.isPINEnabled()) {
            // Require PIN to enter parent mode
            _uiState.value = _uiState.value.copy(requiresPinAuth = true)
        } else {
            // No PIN required or switching to kids mode
            performModeToggle()
        }
    }

    fun validatePinAndToggle(pin: String): Boolean {
        if (securePreferences.validatePIN(pin)) {
            performModeToggle()
            _uiState.value = _uiState.value.copy(requiresPinAuth = false)
            return true
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Incorrect PIN"
            )
            return false
        }
    }

    fun cancelPinAuth() {
        _uiState.value = _uiState.value.copy(
            requiresPinAuth = false,
            error = null
        )
    }

    private fun performModeToggle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTransitioning = true)
            modeManager.toggleMode()
            _uiState.value = _uiState.value.copy(isTransitioning = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
