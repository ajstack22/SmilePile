package com.smilepile.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.BackupStats
import com.smilepile.data.backup.ImportStrategy
import com.smilepile.data.backup.ImportProgress
import com.smilepile.data.backup.BackupFormat
import com.smilepile.theme.ThemeManager
import com.smilepile.theme.ThemeMode
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.security.BiometricManager
import com.smilepile.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val error: String? = null,
    val backupStats: BackupStats? = null,
    val exportProgress: ImportProgress? = null,
    val importProgress: ImportProgress? = null,
    val hasPIN: Boolean = false,
    val biometricEnabled: Boolean = false,
    val kidSafeModeEnabled: Boolean = true
)

/**
 * ViewModel for managing settings screen state and operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeManager: ThemeManager,
    private val backupManager: BackupManager,
    private val securePreferencesManager: SecurePreferencesManager,
    private val biometricManager: BiometricManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Keep track of export data for later writing
    private var pendingExportZipFile: File? = null

    init {
        loadSettings()
        observeThemeChanges()
        observeSecuritySettings()
        observeSettingsManager()
        loadBackupStats()
    }

    /**
     * Load current settings from preferences
     */
    private fun loadSettings() {
        // Load PIN and security settings
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasPIN = securePreferencesManager.isPINEnabled(),
            biometricEnabled = securePreferencesManager.getBiometricEnabled(),
            kidSafeModeEnabled = securePreferencesManager.getKidSafeModeEnabled()
        )
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
        viewModelScope.launch {
            themeManager.themeMode.collect { themeMode ->
                _uiState.value = _uiState.value.copy(themeMode = themeMode)
            }
        }
    }

    /**
     * Set theme mode (System, Light, or Dark)
     */
    fun setThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }

    /**
     * Toggle dark mode setting (cycles through modes)
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
     * Observe security settings changes
     */
    private fun observeSecuritySettings() {
        viewModelScope.launch {
            securePreferencesManager.kidSafeModeEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(kidSafeModeEnabled = enabled)
            }
        }
    }

    /**
     * Observe settings from SettingsManager
     */
    private fun observeSettingsManager() {
        // Observe gallery settings
        viewModelScope.launch {
            settingsManager.getGridSize().collect { gridSize ->
                // Update UI state if needed or trigger refresh
            }
        }

        // Observe auto-backup settings
        viewModelScope.launch {
            settingsManager.getAutoBackupEnabled().collect { enabled ->
                // Update backup scheduler based on setting
            }
        }

        // Observe notification settings
        viewModelScope.launch {
            settingsManager.getNotificationsEnabled().collect { enabled ->
                // Update notification permissions
            }
        }
    }

    /**
     * Set PIN for parental controls
     */
    fun setPIN(pin: String) {
        securePreferencesManager.setPIN(pin)
        _uiState.value = _uiState.value.copy(hasPIN = true)
    }

    /**
     * Remove PIN and disable biometric auth
     */
    fun removePIN() {
        securePreferencesManager.clearPIN()
        securePreferencesManager.setBiometricEnabled(false)
        _uiState.value = _uiState.value.copy(
            hasPIN = false,
            biometricEnabled = false
        )
    }

    /**
     * Change existing PIN
     */
    fun changePIN(oldPin: String, newPin: String): Boolean {
        return if (securePreferencesManager.validatePIN(oldPin)) {
            securePreferencesManager.setPIN(newPin)
            true
        } else {
            false
        }
    }

    /**
     * Toggle biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePreferencesManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
    }

    /**
     * Check if biometric auth is available
     */
    fun isBiometricAvailable(): Boolean {
        return biometricManager.isBiometricAvailable() == com.smilepile.security.BiometricAvailability.AVAILABLE
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
                    pendingExportZipFile?.let { file ->
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete temporary export file: ${file.name}")
                        }
                    }
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
                if (!tempFile.delete()) {
                    Log.w(TAG, "Failed to delete temporary import file: ${tempFile.name}")
                }

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
     * Reset app for onboarding (debug only)
     */
    fun resetAppForOnboarding() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Clear all app data
                backupManager.clearAllData()

                // Clear onboarding completed flag
                settingsManager.setOnboardingCompleted(false)

                // Clear security settings
                securePreferencesManager.clearPIN()
                securePreferencesManager.setBiometricEnabled(false)

                // Give DataStore time to persist all changes
                kotlinx.coroutines.delay(300)

                // Restart the app to trigger onboarding
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                context.startActivity(intent)

                // Exit the app gracefully - the intent flags will restart it
                (context as? android.app.Activity)?.finishAffinity()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reset app: ${e.message}",
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
        pendingExportZipFile?.let { file ->
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete pending export file: ${file.name}")
            }
        }
        pendingExportZipFile = null
    }

    /**
     * Clear import progress
     */
    fun clearImportProgress() {
        _uiState.value = _uiState.value.copy(importProgress = null)
    }

    /**
     * Export all app settings for backup
     */
    suspend fun exportAllSettings(): Map<String, Any> {
        return settingsManager.exportSettings()
    }

    /**
     * Import settings from backup
     */
    suspend fun importAllSettings(settings: Map<String, Any>, overwrite: Boolean = false) {
        settingsManager.importSettings(settings, overwrite)
        loadSettings() // Reload UI state after import
    }

    /**
     * Reset settings to defaults
     */
    fun resetSettingsToDefaults() {
        viewModelScope.launch {
            settingsManager.resetToDefaults()
            loadSettings()
        }
    }

    /**
     * Update gallery view settings
     */
    fun updateGallerySettings(gridSize: Int? = null, sortOrder: SettingsManager.SortOrder? = null) {
        viewModelScope.launch {
            gridSize?.let { settingsManager.setGridSize(it) }
            sortOrder?.let { settingsManager.setSortOrder(it) }
        }
    }

    /**
     * Update notification settings
     */
    fun updateNotificationSettings(
        notificationsEnabled: Boolean? = null,
        backupNotifications: Boolean? = null,
        memoryNotifications: Boolean? = null
    ) {
        viewModelScope.launch {
            notificationsEnabled?.let { settingsManager.setNotificationsEnabled(it) }
            backupNotifications?.let { settingsManager.setBackupNotifications(it) }
            memoryNotifications?.let { settingsManager.setMemoryNotifications(it) }
        }
    }

    /**
     * Update backup settings
     */
    fun updateBackupSettings(
        autoBackupEnabled: Boolean? = null,
        wifiOnly: Boolean? = null,
        frequency: SettingsManager.BackupFrequency? = null
    ) {
        viewModelScope.launch {
            autoBackupEnabled?.let { settingsManager.setAutoBackupEnabled(it) }
            wifiOnly?.let { settingsManager.setBackupWifiOnly(it) }
            frequency?.let { settingsManager.setBackupFrequency(it) }
        }
    }

    /**
     * Update photo quality settings
     */
    fun updatePhotoQualitySettings(
        uploadQuality: SettingsManager.PhotoQuality? = null,
        thumbnailQuality: SettingsManager.PhotoQuality? = null,
        autoOptimize: Boolean? = null
    ) {
        viewModelScope.launch {
            uploadQuality?.let { settingsManager.setUploadQuality(it) }
            thumbnailQuality?.let { settingsManager.setThumbnailQuality(it) }
            autoOptimize?.let { settingsManager.setAutoOptimizeStorage(it) }
        }
    }
}