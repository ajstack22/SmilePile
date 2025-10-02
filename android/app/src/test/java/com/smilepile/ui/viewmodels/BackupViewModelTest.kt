package com.smilepile.ui.viewmodels

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smilepile.data.backup.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Unit tests for BackupViewModel
 */
@ExperimentalCoroutinesApi
class BackupViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var backupManager: BackupManager
    private lateinit var restoreManager: RestoreManager
    private lateinit var exportManager: ExportManager
    private lateinit var viewModel: BackupViewModel

    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val testBackupFile = File("/test/backup.zip")
    private val testBackupStats = BackupStats(
        categoryCount = 5,
        photoCount = 100,
        success = true,
        errorMessage = null
    )

    private val testBackupHistory = listOf(
        BackupHistoryEntry(
            id = "backup1",
            timestamp = System.currentTimeMillis() - 86400000,
            fileName = "backup1.zip",
            filePath = "/test/backup1.zip",
            fileSize = 1024000,
            format = BackupFormat.ZIP,
            photosCount = 100,
            categoriesCount = 5,
            compressionLevel = CompressionLevel.MEDIUM,
            success = true
        ),
        BackupHistoryEntry(
            id = "backup2",
            timestamp = System.currentTimeMillis(),
            fileName = "backup2.zip",
            filePath = "/test/backup2.zip",
            fileSize = 512000,
            format = BackupFormat.ZIP,
            photosCount = 50,
            categoriesCount = 5,
            compressionLevel = CompressionLevel.MEDIUM,
            success = true
        )
    )

    private val testBackupSchedule = BackupSchedule(
        enabled = true,
        frequency = BackupFrequency.DAILY,
        time = "02:00",
        wifiOnly = true
    )

    private val testValidationResult = BackupValidationResult(
        isValid = true,
        version = 2,
        format = BackupFormat.ZIP,
        hasMetadata = true,
        hasPhotos = true,
        photosCount = 100,
        categoriesCount = 5,
        integrityCheckPassed = true,
        errors = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        backupManager = mockk(relaxed = true)
        restoreManager = mockk(relaxed = true)
        exportManager = mockk(relaxed = true)

        // Setup default mock responses
        coEvery { backupManager.getBackupHistory() } returns testBackupHistory
        coEvery { backupManager.getBackupSchedule() } returns testBackupSchedule
        coEvery { backupManager.getBackupStats() } returns testBackupStats
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given & When
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBackupInProgress)
        assertFalse(uiState.isRestoreInProgress)
        assertFalse(uiState.isExportInProgress)
        assertFalse(uiState.isValidating)
        assertNull(uiState.lastError)
        assertNull(viewModel.backupProgress.value)
        assertNull(viewModel.restoreProgress.value)
        assertNull(viewModel.exportProgress.value)
    }

    @Test
    fun `loads backup history on initialization`() = runTest {
        // Given & When
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // Then
        val history = viewModel.uiState.value.backupHistory
        assertEquals(2, history.size)
        assertEquals("backup1", history[0].id)
        assertEquals("backup2", history[1].id)
    }

    @Test
    fun `loads backup schedule on initialization`() = runTest {
        // Given & When
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // Then
        val schedule = viewModel.uiState.value.backupSchedule
        assertNotNull(schedule)
        assertTrue(schedule!!.enabled)
        assertEquals(BackupFrequency.DAILY, schedule.frequency)
        assertEquals("02:00", schedule.time)
        assertTrue(schedule.wifiOnly)
    }

    @Test
    fun `creates backup successfully`() = runTest {
        // Given
        coEvery {
            backupManager.exportToZip(any(), any())
        } returns Result.success(testBackupFile)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.createBackup()
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBackupInProgress)
        assertTrue(uiState.backupSuccess == true)
        assertNotNull(uiState.lastBackupTime)
        assertEquals(testBackupFile.absolutePath, uiState.lastBackupFile)
        assertNull(viewModel.backupProgress.value)
    }

    @Test
    fun `handles backup failure`() = runTest {
        // Given
        val errorMessage = "Backup failed due to storage issue"
        coEvery {
            backupManager.exportToZip(any(), any())
        } returns Result.failure(RuntimeException(errorMessage))

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.createBackup()
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBackupInProgress)
        assertFalse(uiState.backupSuccess == true)
        assertEquals(errorMessage, uiState.lastError)
    }

    @Test
    fun `creates backup with destination URI`() = runTest {
        // Given
        val destinationUri = mockk<Uri>(relaxed = true)
        coEvery {
            backupManager.exportToZip(any(), any())
        } returns Result.success(testBackupFile)
        coEvery {
            backupManager.writeZipToFile(any(), any())
        } returns Result.success(Unit)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.createBackup(destinationUri = destinationUri)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.writeZipToFile(testBackupFile, destinationUri) }
        assertTrue(viewModel.uiState.value.backupSuccess == true)
    }

    @Test
    fun `creates incremental backup successfully`() = runTest {
        // Given
        coEvery {
            backupManager.performIncrementalBackup(any(), any())
        } returns Result.success(testBackupFile)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.createIncrementalBackup("backup1")
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isBackupInProgress)
        assertTrue(uiState.backupSuccess == true)
        assertEquals(testBackupFile.absolutePath, uiState.lastBackupFile)
    }

    @Test
    fun `validates backup file successfully`() = runTest {
        // Given
        coEvery {
            restoreManager.validateBackup(any())
        } returns Result.success(testValidationResult)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.validateBackup(testBackupFile)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isValidating)
        assertNotNull(uiState.validationResult)
        assertEquals(testValidationResult, uiState.validationResult)
    }

    @Test
    fun `handles validation failure`() = runTest {
        // Given
        val errorMessage = "Invalid backup file format"
        coEvery {
            restoreManager.validateBackup(any())
        } returns Result.failure(RuntimeException(errorMessage))

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.validateBackup(testBackupFile)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isValidating)
        assertNull(uiState.validationResult)
        assertEquals(errorMessage, uiState.lastError)
    }

    @Test
    fun `restores backup successfully`() = runTest {
        // Given
        val successProgress = ImportProgress(
            totalItems = 100,
            processedItems = 100,
            currentOperation = "Restore completed successfully",
            errors = emptyList()
        )
        coEvery {
            restoreManager.restoreFromBackup(any(), any(), any())
        } returns flowOf(successProgress)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.restoreBackup(testBackupFile)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isRestoreInProgress)
        assertTrue(uiState.restoreSuccess == true)
        assertNotNull(uiState.lastRestoreTime)
    }

    @Test
    fun `handles restore failure`() = runTest {
        // Given
        val failureProgress = ImportProgress(
            totalItems = 100,
            processedItems = 50,
            currentOperation = "Restore failed",
            errors = listOf("Corrupted data")
        )
        coEvery {
            restoreManager.restoreFromBackup(any(), any(), any())
        } returns flowOf(failureProgress)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.restoreBackup(testBackupFile)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isRestoreInProgress)
        assertFalse(uiState.restoreSuccess == true)
        assertEquals("Corrupted data", uiState.lastError)
    }

    @Test
    fun `exports data successfully`() = runTest {
        // Given
        val exportFile = File("/test/export.json")
        coEvery {
            exportManager.export(any(), any(), any())
        } returns Result.success(exportFile)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.exportData(ExportFormat.JSON)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isExportInProgress)
        assertTrue(uiState.exportSuccess == true)
        assertNotNull(uiState.lastExportTime)
        assertEquals(exportFile.absolutePath, uiState.lastExportFile)
    }

    @Test
    fun `schedules backup successfully`() = runTest {
        // Given
        val newSchedule = BackupSchedule(
            enabled = true,
            frequency = BackupFrequency.WEEKLY,
            time = "03:00",
            wifiOnly = false
        )
        coEvery { backupManager.scheduleBackup(any()) } just Runs

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.scheduleBackup(newSchedule)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.scheduleBackup(newSchedule) }
        assertEquals(newSchedule, viewModel.uiState.value.backupSchedule)
    }

    @Test
    fun `cancels scheduled backup`() = runTest {
        // Given
        coEvery { backupManager.scheduleBackup(any()) } just Runs

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.cancelScheduledBackup()
        testScheduler.advanceUntilIdle()

        // Then
        coVerify {
            backupManager.scheduleBackup(match {
                !it.enabled
            })
        }
        assertFalse(viewModel.uiState.value.backupSchedule?.enabled == true)
    }

    @Test
    fun `loads backup statistics`() = runTest {
        // Given
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)

        // When
        viewModel.loadBackupStats()
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(5, uiState.categoryCount)
        assertEquals(100, uiState.photoCount)
    }

    @Test
    fun `deletes backup from history`() = runTest {
        // Given
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.deleteBackup("backup1")
        testScheduler.advanceUntilIdle()

        // Then
        val history = viewModel.uiState.value.backupHistory
        assertEquals(1, history.size)
        assertFalse(history.any { it.id == "backup1" })
    }

    @Test
    fun `clears error state`() = runTest {
        // Given
        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)

        // Set an error
        coEvery {
            backupManager.exportToZip(any(), any())
        } returns Result.failure(RuntimeException("Test error"))
        viewModel.createBackup()
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testScheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.lastError)
    }

    @Test
    fun `resets success states`() = runTest {
        // Given
        coEvery {
            backupManager.exportToZip(any(), any())
        } returns Result.success(testBackupFile)

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        viewModel.createBackup()
        testScheduler.advanceUntilIdle()

        // When
        viewModel.resetSuccessStates()
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.backupSuccess)
        assertNull(uiState.restoreSuccess)
        assertNull(uiState.exportSuccess)
    }

    @Test
    fun `tracks backup progress`() = runTest {
        // Given
        var progressCalled = false
        coEvery {
            backupManager.exportToZip(any(), captureLambda())
        } answers {
            val progressCallback = lambda<(Int, Int, String) -> Unit>()
            progressCallback.invoke(50, 100, "Processing photos")
            progressCalled = true
            Result.success(testBackupFile)
        }

        viewModel = BackupViewModel(backupManager, restoreManager, exportManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.createBackup()
        testScheduler.advanceUntilIdle()

        // Then
        assertTrue(progressCalled)
    }
}