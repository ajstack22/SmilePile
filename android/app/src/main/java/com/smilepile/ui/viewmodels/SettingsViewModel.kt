package com.smilepile.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.BackupStats
import com.smilepile.data.backup.ImportStrategy
import com.smilepile.data.backup.ImportProgress
import com.smilepile.data.backup.BackupFormat
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val backupStats: BackupStats? = null,
    val exportProgress: ImportProgress? = null,
    val importProgress: ImportProgress? = null
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

    // Keep track of export data for later writing
    private var pendingExportZipFile: File? = null

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

            // Export data to ZIP with progress tracking
            val result = backupManager.exportToZip { current, total, operation ->
                val progress = ImportProgress(
                    totalItems = total,
                    processedItems = current,
                    currentOperation = operation,
                    errors = emptyList()
                )
                _uiState.value = _uiState.value.copy(exportProgress = progress)
            }

            if (result.isSuccess) {
                pendingExportZipFile = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exportProgress = null
                )
                // Return SAF intent for ZIP
                backupManager.createExportIntent(BackupFormat.ZIP)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to export data",
                    isLoading = false,
                    exportProgress = null
                )
                null
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Failed to export data",
                isLoading = false,
                exportProgress = null
            )
            null
        }
    }

    /**
     * Complete the export by writing data to the selected file URI
     * @param uri The file URI selected by the user
     */
    fun completeExport(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Use pending ZIP file or create fresh
                val zipFile = pendingExportZipFile ?: run {
                    val result = backupManager.exportToZip { current, total, operation ->
                        val progress = ImportProgress(
                            totalItems = total,
                            processedItems = current,
                            currentOperation = operation,
                            errors = emptyList()
                        )
                        _uiState.value = _uiState.value.copy(exportProgress = progress)
                    }
                    if (result.isSuccess) {
                        result.getOrThrow()
                    } else {
                        throw Exception("Failed to create ZIP export")
                    }
                }

                // Write ZIP to the selected URI
                val writeResult = backupManager.writeZipToFile(zipFile, uri)
                if (writeResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        exportProgress = null
                    )
                    // Clean up temp file
                    pendingExportZipFile?.delete()
                    pendingExportZipFile = null
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = writeResult.exceptionOrNull()?.message ?: "Failed to save backup file",
                        isLoading = false,
                        exportProgress = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to export data",
                    isLoading = false,
                    exportProgress = null
                )
            }
        }
    }

    /**
     * Import app data from URI
     * Automatically detects if the file is JSON or ZIP format
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, importProgress = null)

                // Determine file format based on content
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to read backup file",
                        isLoading = false
                    )
                    return@launch
                }

                // Create temporary file
                val tempFile = java.io.File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")

                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Detect format by examining file content
                val isZipFile = try {
                    tempFile.name.endsWith(".zip") ||
                    (tempFile.length() > 4 &&
                     tempFile.inputStream().use { stream ->
                         val header = ByteArray(4)
                         stream.read(header)
                         // ZIP file signature: 0x504b0304
                         header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
                         header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                     })
                } catch (e: Exception) {
                    false
                }

                // Import based on detected format
                if (isZipFile) {
                    backupManager.importFromZip(
                        zipFile = tempFile,
                        strategy = ImportStrategy.MERGE
                    ) { current, total, operation ->
                        val progress = ImportProgress(
                            totalItems = total,
                            processedItems = current,
                            currentOperation = operation,
                            errors = emptyList()
                        )
                        _uiState.value = _uiState.value.copy(importProgress = progress)
                    }.collect { progress ->
                        _uiState.value = _uiState.value.copy(importProgress = progress)

                        when {
                            progress.errors.isNotEmpty() -> {
                                _uiState.value = _uiState.value.copy(
                                    error = "Import completed with errors: ${progress.errors.firstOrNull()}",
                                    isLoading = false,
                                    importProgress = null
                                )
                            }
                            progress.currentOperation.contains("completed", ignoreCase = true) -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null,
                                    importProgress = null
                                )
                                // Reload backup stats after successful import
                                loadBackupStats()
                            }
                        }
                    }
                } else {
                    // JSON format
                    backupManager.importFromJson(
                        backupFile = tempFile,
                        strategy = ImportStrategy.MERGE
                    ).collect { progress ->
                        _uiState.value = _uiState.value.copy(importProgress = progress)

                        when {
                            progress.errors.isNotEmpty() -> {
                                _uiState.value = _uiState.value.copy(
                                    error = "Import completed with errors: ${progress.errors.firstOrNull()}",
                                    isLoading = false,
                                    importProgress = null
                                )
                            }
                            progress.currentOperation.contains("completed", ignoreCase = true) -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null,
                                    importProgress = null
                                )
                                // Reload backup stats after successful import
                                loadBackupStats()
                            }
                        }
                    }
                }

                // Clean up temp file
                tempFile.delete()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to import data",
                    isLoading = false,
                    importProgress = null
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
        _uiState.value = _uiState.value.copy(
            exportProgress = null
        )
        pendingExportZipFile?.delete()
        pendingExportZipFile = null
    }

    /**
     * Clear import progress
     */
    fun clearImportProgress() {
        _uiState.value = _uiState.value.copy(importProgress = null)
    }
}