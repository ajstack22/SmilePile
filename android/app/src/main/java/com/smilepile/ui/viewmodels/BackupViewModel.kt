package com.smilepile.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.backup.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing backup and restore operations
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val exportManager: ExportManager
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // Backup progress
    private val _backupProgress = MutableStateFlow<BackupProgress?>(null)
    val backupProgress: StateFlow<BackupProgress?> = _backupProgress.asStateFlow()

    // Restore progress
    private val _restoreProgress = MutableStateFlow<ImportProgress?>(null)
    val restoreProgress: StateFlow<ImportProgress?> = _restoreProgress.asStateFlow()

    // Export progress
    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    init {
        loadBackupHistory()
        loadBackupSchedule()
    }

    /**
     * Create a full backup with options
     */
    fun createBackup(
        options: BackupOptions = BackupOptions(),
        destinationUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true, lastError = null) }
            _backupProgress.value = BackupProgress(0, 100, "Starting backup...")

            try {
                // Create backup
                val result = backupManager.exportToZip(
                    options = options,
                    progressCallback = { current, total, operation ->
                        _backupProgress.value = BackupProgress(current, total, operation)
                    }
                )

                if (result.isSuccess) {
                    val backupFile = result.getOrThrow()

                    // Save to user-selected location if provided
                    if (destinationUri != null) {
                        val writeResult = backupManager.writeZipToFile(backupFile, destinationUri)
                        if (writeResult.isFailure) {
                            throw writeResult.exceptionOrNull() ?: Exception("Failed to save backup")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isBackupInProgress = false,
                            lastBackupTime = System.currentTimeMillis(),
                            lastBackupFile = backupFile.absolutePath,
                            backupSuccess = true
                        )
                    }

                    // Reload backup history
                    loadBackupHistory()
                } else {
                    throw result.exceptionOrNull() ?: Exception("Backup failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackupInProgress = false,
                        lastError = e.message,
                        backupSuccess = false
                    )
                }
            } finally {
                _backupProgress.value = null
            }
        }
    }

    /**
     * Create incremental backup
     */
    fun createIncrementalBackup(baseBackupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true, lastError = null) }
            _backupProgress.value = BackupProgress(0, 100, "Analyzing changes...")

            try {
                val result = backupManager.performIncrementalBackup(
                    baseBackupId = baseBackupId,
                    progressCallback = { current, total, operation ->
                        _backupProgress.value = BackupProgress(current, total, operation)
                    }
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isBackupInProgress = false,
                            lastBackupTime = System.currentTimeMillis(),
                            lastBackupFile = result.getOrThrow().absolutePath,
                            backupSuccess = true
                        )
                    }
                    loadBackupHistory()
                } else {
                    throw result.exceptionOrNull() ?: Exception("Incremental backup failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackupInProgress = false,
                        lastError = e.message,
                        backupSuccess = false
                    )
                }
            } finally {
                _backupProgress.value = null
            }
        }
    }

    /**
     * Validate backup file
     */
    fun validateBackup(backupFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, validationResult = null) }

            try {
                val result = restoreManager.validateBackup(backupFile)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            validationResult = result.getOrThrow()
                        )
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Validation failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        lastError = e.message
                    )
                }
            }
        }
    }

    /**
     * Restore from backup
     */
    fun restoreBackup(
        backupFile: File,
        options: RestoreOptions = RestoreOptions()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true, lastError = null) }

            try {
                restoreManager.restoreFromBackup(
                    backupFile = backupFile,
                    options = options,
                    progressCallback = { current, total, operation ->
                        _restoreProgress.value = ImportProgress(total, current, operation)
                    }
                ).collect { progress ->
                    _restoreProgress.value = progress

                    // Check if restore completed
                    if (progress.currentOperation.contains("completed", ignoreCase = true)) {
                        _uiState.update {
                            it.copy(
                                isRestoreInProgress = false,
                                restoreSuccess = true,
                                lastRestoreTime = System.currentTimeMillis()
                            )
                        }
                    } else if (progress.currentOperation.contains("failed", ignoreCase = true)) {
                        _uiState.update {
                            it.copy(
                                isRestoreInProgress = false,
                                restoreSuccess = false,
                                lastError = progress.errors.firstOrNull()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoreInProgress = false,
                        lastError = e.message,
                        restoreSuccess = false
                    )
                }
            } finally {
                _restoreProgress.value = null
            }
        }
    }

    /**
     * Export data in specified format
     */
    fun exportData(
        format: ExportFormat,
        options: ExportOptions = ExportOptions(),
        destinationUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportInProgress = true, lastError = null) }

            try {
                val result = exportManager.export(
                    format = format,
                    options = options,
                    progressCallback = { progress ->
                        _exportProgress.value = progress
                    }
                )

                if (result.isSuccess) {
                    val exportFile = result.getOrThrow()

                    // Save to user-selected location if provided
                    if (destinationUri != null) {
                        // Copy export file to destination
                        // Implementation would depend on the specific requirements
                    }

                    _uiState.update {
                        it.copy(
                            isExportInProgress = false,
                            lastExportTime = System.currentTimeMillis(),
                            lastExportFile = exportFile.absolutePath,
                            exportSuccess = true
                        )
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Export failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportInProgress = false,
                        lastError = e.message,
                        exportSuccess = false
                    )
                }
            } finally {
                _exportProgress.value = null
            }
        }
    }

    /**
     * Schedule automatic backup
     */
    fun scheduleBackup(schedule: BackupSchedule) {
        viewModelScope.launch {
            try {
                backupManager.scheduleBackup(schedule)
                _uiState.update {
                    it.copy(backupSchedule = schedule)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(lastError = e.message)
                }
            }
        }
    }

    /**
     * Cancel scheduled backup
     */
    fun cancelScheduledBackup() {
        viewModelScope.launch {
            try {
                val disabledSchedule = BackupSchedule(enabled = false)
                backupManager.scheduleBackup(disabledSchedule)
                _uiState.update {
                    it.copy(backupSchedule = disabledSchedule)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(lastError = e.message)
                }
            }
        }
    }

    /**
     * Load backup history
     */
    private fun loadBackupHistory() {
        viewModelScope.launch {
            try {
                val history = backupManager.getBackupHistory()
                _uiState.update {
                    it.copy(backupHistory = history)
                }
            } catch (e: Exception) {
                // Silently fail for history loading
            }
        }
    }

    /**
     * Load backup schedule
     */
    private fun loadBackupSchedule() {
        viewModelScope.launch {
            try {
                val schedule = backupManager.getBackupSchedule()
                if (schedule != null) {
                    _uiState.update {
                        it.copy(backupSchedule = schedule)
                    }
                }
            } catch (e: Exception) {
                // Silently fail for schedule loading
            }
        }
    }

    /**
     * Get backup statistics
     */
    fun loadBackupStats() {
        viewModelScope.launch {
            try {
                val stats = backupManager.getBackupStats()
                _uiState.update {
                    it.copy(
                        categoryCount = stats.categoryCount,
                        photoCount = stats.photoCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(lastError = e.message)
                }
            }
        }
    }

    /**
     * Delete backup from history
     */
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                // Remove from history
                val currentHistory = _uiState.value.backupHistory
                val updatedHistory = currentHistory.filter { it.id != backupId }

                // Save updated history
                // This would need implementation in BackupManager

                _uiState.update {
                    it.copy(backupHistory = updatedHistory)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(lastError = e.message)
                }
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(lastError = null) }
    }

    /**
     * Reset success states
     */
    fun resetSuccessStates() {
        _uiState.update {
            it.copy(
                backupSuccess = null,
                restoreSuccess = null,
                exportSuccess = null
            )
        }
    }
}

/**
 * UI State for backup screen
 */
data class BackupUiState(
    // Backup state
    val isBackupInProgress: Boolean = false,
    val backupSuccess: Boolean? = null,
    val lastBackupTime: Long? = null,
    val lastBackupFile: String? = null,

    // Restore state
    val isRestoreInProgress: Boolean = false,
    val restoreSuccess: Boolean? = null,
    val lastRestoreTime: Long? = null,

    // Export state
    val isExportInProgress: Boolean = false,
    val exportSuccess: Boolean? = null,
    val lastExportTime: Long? = null,
    val lastExportFile: String? = null,

    // Validation state
    val isValidating: Boolean = false,
    val validationResult: BackupValidationResult? = null,

    // Schedule state
    val backupSchedule: BackupSchedule? = null,

    // History
    val backupHistory: List<BackupHistoryEntry> = emptyList(),

    // Statistics
    val categoryCount: Int = 0,
    val photoCount: Int = 0,

    // Error state
    val lastError: String? = null
)

/**
 * Backup progress data
 */
data class BackupProgress(
    val current: Int,
    val total: Int,
    val operation: String
) {
    val percentage: Int
        get() = if (total > 0) (current * 100) / total else 0
}