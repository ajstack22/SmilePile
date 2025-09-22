package com.smilepile.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.BackupStats
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
    val error: String? = null,
    val backupStats: BackupStats? = null,
    val exportJsonContent: String? = null
)

/**
 * ViewModel for managing settings screen state and operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeManager: ThemeManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeThemeChanges()
        loadBackupStats()
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
     * Refresh settings state
     */
    fun refreshSettings() {
        loadSettings()
    }

    /**
     * Prepare export data and create intent for Storage Access Framework
     * @return Intent for file picker or null if export failed
     */
    suspend fun prepareExport(): Intent? {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Export data to JSON using BackupManager
            val result = backupManager.exportToJson()

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    exportJsonContent = result.getOrNull(),
                    isLoading = false
                )
                // Return SAF intent
                backupManager.createExportIntent()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to export data",
                    isLoading = false
                )
                null
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Failed to export data",
                isLoading = false
            )
            null
        }
    }

    /**
     * Complete the export by writing JSON to the selected file URI
     * @param uri The file URI selected by the user
     */
    fun completeExport(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonContent = _uiState.value.exportJsonContent
                if (jsonContent != null) {
                    val result = backupManager.writeJsonToFile(jsonContent, uri)
                    if (result.isSuccess) {
                        // Clear the export content after successful write
                        _uiState.value = _uiState.value.copy(
                            exportJsonContent = null,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = result.exceptionOrNull()?.message ?: "Failed to save backup file",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "No export data available. Please try exporting again.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save backup file",
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

                // TODO: WON'T FIX - Android handles image cache clearing automatically
                // Android and Coil manage image caching efficiently. Manual cache clearing
                // is not necessary and could negatively impact user experience by causing
                // unnecessary reloading of images. The OS will automatically clear cache
                // when memory is needed.

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
     * Load backup statistics
     */
    private fun loadBackupStats() {
        viewModelScope.launch {
            try {
                val stats = backupManager.getBackupStats()
                _uiState.value = _uiState.value.copy(backupStats = stats)
            } catch (e: Exception) {
                // Backup stats are not critical, so just log the error
                _uiState.value = _uiState.value.copy(
                    backupStats = BackupStats(
                        categoryCount = 0,
                        photoCount = 0,
                        success = false,
                        errorMessage = e.message
                    )
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

    /**
     * Clear export content
     */
    fun clearExportContent() {
        _uiState.value = _uiState.value.copy(exportJsonContent = null)
    }

    /**
     * Prepare export asynchronously and launch Storage Access Framework
     */
    fun prepareExportAsync() {
        viewModelScope.launch {
            val intent = prepareExport()
            intent?.let {
                // In a real app, this would be handled by the UI layer
                // For now, we'll just set the loading state
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}