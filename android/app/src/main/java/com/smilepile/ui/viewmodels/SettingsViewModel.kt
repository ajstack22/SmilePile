package com.smilepile.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing settings screen state and operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeThemeChanges()
    }

    /**
     * Load current settings from preferences
     */
    private fun loadSettings() {
        // Initial load - set loading state briefly
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Observe theme changes from ThemeManager
     */
    private fun observeThemeChanges() {
        viewModelScope.launch {
            themeManager.isDarkMode.collect { isDarkMode ->
                _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
            }
        }
    }

    /**
     * Toggle dark mode setting
     */
    fun toggleDarkMode() {
        // Use ThemeManager to toggle theme
        themeManager.toggleTheme()
    }

    /**
     * Export app data
     */
    fun exportData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // TODO: Implement data export functionality
                // This would typically involve:
                // 1. Gathering all photos and categories data
                // 2. Creating a backup file (JSON/ZIP)
                // 3. Saving to external storage or sharing

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to export data",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Import app data
     */
    fun importData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // TODO: Implement data import functionality
                // This would typically involve:
                // 1. File picker to select backup file
                // 2. Parsing the backup data
                // 3. Importing photos and categories to database
                // 4. Handling conflicts and validation

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to import data",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Clear app cache
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // TODO: Implement cache clearing functionality
                // This would typically involve:
                // 1. Clearing image cache (Coil cache)
                // 2. Clearing temporary files
                // 3. Clearing any other cached data

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to clear cache",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}