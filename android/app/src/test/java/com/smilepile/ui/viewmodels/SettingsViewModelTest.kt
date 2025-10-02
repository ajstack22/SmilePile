package com.smilepile.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.smilepile.data.backup.*
import com.smilepile.security.BiometricAvailability
import com.smilepile.security.BiometricManager
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.settings.SettingsManager
import com.smilepile.theme.ThemeManager
import com.smilepile.theme.ThemeMode
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Unit tests for SettingsViewModel
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var themeManager: ThemeManager
    private lateinit var backupManager: BackupManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var biometricManager: BiometricManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val testBackupStats = BackupStats(
        categoryCount = 5,
        photoCount = 100,
        success = true,
        errorMessage = null
    )

    private val testExportFile = File("/test/export.zip")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize context
        context = ApplicationProvider.getApplicationContext()

        // Initialize mocks
        themeManager = mockk(relaxed = true)
        backupManager = mockk(relaxed = true)
        securePreferencesManager = mockk(relaxed = true)
        biometricManager = mockk(relaxed = true)
        settingsManager = mockk(relaxed = true)

        // Setup default mock responses
        every { themeManager.isDarkMode } returns MutableStateFlow(false)
        every { themeManager.themeMode } returns MutableStateFlow(ThemeMode.SYSTEM)
        every { securePreferencesManager.isPINEnabled() } returns false
        every { securePreferencesManager.getBiometricEnabled() } returns false
        every { securePreferencesManager.getKidSafeModeEnabled() } returns true
        every { securePreferencesManager.kidSafeModeEnabled } returns flowOf(true)
        coEvery { backupManager.getBackupStats() } returns testBackupStats
        every { settingsManager.getGridSize() } returns flowOf(3)
        every { settingsManager.getAutoBackupEnabled() } returns flowOf(false)
        every { settingsManager.getNotificationsEnabled() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given & When
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isDarkMode)
        assertEquals(ThemeMode.SYSTEM, uiState.themeMode)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertFalse(uiState.hasPIN)
        assertFalse(uiState.biometricEnabled)
        assertTrue(uiState.kidSafeModeEnabled)
    }

    @Test
    fun `observes theme changes`() = runTest {
        // Given
        val darkModeFlow = MutableStateFlow(false)
        val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
        every { themeManager.isDarkMode } returns darkModeFlow
        every { themeManager.themeMode } returns themeModeFlow

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        darkModeFlow.value = true
        themeModeFlow.value = ThemeMode.DARK
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isDarkMode)
        assertEquals(ThemeMode.DARK, uiState.themeMode)
    }

    @Test
    fun `sets theme mode`() = runTest {
        // Given
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.LIGHT)

        // Then
        verify { themeManager.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `toggles dark mode`() = runTest {
        // Given
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.toggleDarkMode()

        // Then
        verify { themeManager.toggleTheme() }
    }

    @Test
    fun `sets PIN successfully`() = runTest {
        // Given
        every { securePreferencesManager.setPIN(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.setPIN("1234")
        testScheduler.advanceUntilIdle()

        // Then
        verify { securePreferencesManager.setPIN("1234") }
        assertTrue(viewModel.uiState.value.hasPIN)
    }

    @Test
    fun `removes PIN and disables biometric`() = runTest {
        // Given
        every { securePreferencesManager.clearPIN() } just Runs
        every { securePreferencesManager.setBiometricEnabled(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.removePIN()
        testScheduler.advanceUntilIdle()

        // Then
        verify { securePreferencesManager.clearPIN() }
        verify { securePreferencesManager.setBiometricEnabled(false) }
        assertFalse(viewModel.uiState.value.hasPIN)
        assertFalse(viewModel.uiState.value.biometricEnabled)
    }

    @Test
    fun `changes PIN with correct old PIN`() = runTest {
        // Given
        every { securePreferencesManager.validatePIN("1234") } returns true
        every { securePreferencesManager.setPIN("5678") } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)

        // When
        val result = viewModel.changePIN("1234", "5678")

        // Then
        assertTrue(result)
        verify { securePreferencesManager.setPIN("5678") }
    }

    @Test
    fun `rejects PIN change with incorrect old PIN`() = runTest {
        // Given
        every { securePreferencesManager.validatePIN("wrong") } returns false

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)

        // When
        val result = viewModel.changePIN("wrong", "5678")

        // Then
        assertFalse(result)
        verify(exactly = 0) { securePreferencesManager.setPIN("5678") }
    }

    @Test
    fun `enables biometric authentication`() = runTest {
        // Given
        every { securePreferencesManager.setBiometricEnabled(true) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.setBiometricEnabled(true)
        testScheduler.advanceUntilIdle()

        // Then
        verify { securePreferencesManager.setBiometricEnabled(true) }
        assertTrue(viewModel.uiState.value.biometricEnabled)
    }

    @Test
    fun `checks biometric availability`() = runTest {
        // Given
        every { biometricManager.isBiometricAvailable() } returns BiometricAvailability.AVAILABLE

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)

        // When
        val isAvailable = viewModel.isBiometricAvailable()

        // Then
        assertTrue(isAvailable)
    }

    @Test
    fun `prepares export successfully`() = runTest {
        // Given
        val mockIntent = mockk<Intent>()
        coEvery { backupManager.exportToZip(any()) } returns Result.success(testExportFile)
        coEvery { backupManager.createExportIntent(BackupFormat.ZIP) } returns mockIntent

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        val intent = viewModel.prepareExport()

        // Then
        assertNotNull(intent)
        assertEquals(mockIntent, intent)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `handles export preparation failure`() = runTest {
        // Given
        coEvery { backupManager.exportToZip(any()) } returns Result.failure(RuntimeException("Export failed"))

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        val intent = viewModel.prepareExport()

        // Then
        assertNull(intent)
        assertEquals("Export failed", viewModel.uiState.value.error)
    }

    @Test
    fun `completes export to URI`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery { backupManager.exportToZip(any()) } returns Result.success(testExportFile)
        coEvery { backupManager.writeZipToFile(any(), any()) } returns Result.success(Unit)

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.completeExport(uri)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.writeZipToFile(any(), uri) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `imports from ZIP file URI`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val contentResolver = mockk<android.content.ContentResolver>()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns "PK\u0003\u0004test".byteInputStream()

        val importProgress = ImportProgress(
            totalItems = 100,
            processedItems = 100,
            currentOperation = "Import completed",
            errors = emptyList()
        )
        coEvery { backupManager.importFromZip(any(), any(), any()) } returns flowOf(importProgress)

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.importFromUri(uri)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.importFromZip(any(), ImportStrategy.MERGE, any()) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `imports from JSON file URI`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val contentResolver = mockk<android.content.ContentResolver>()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns "{\"test\":\"data\"}".byteInputStream()

        val importProgress = ImportProgress(
            totalItems = 50,
            processedItems = 50,
            currentOperation = "Import completed",
            errors = emptyList()
        )
        coEvery { backupManager.importFromJson(any(), any()) } returns flowOf(importProgress)

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.importFromUri(uri)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.importFromJson(any(), ImportStrategy.MERGE) }
    }

    @Test
    fun `clears cache`() = runTest {
        // Given
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearCache()
        testScheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `resets app for onboarding`() = runTest {
        // Given
        coEvery { backupManager.clearAllData() } just Runs
        coEvery { settingsManager.setOnboardingCompleted(false) } just Runs
        every { securePreferencesManager.clearPIN() } just Runs
        every { securePreferencesManager.setBiometricEnabled(false) } just Runs
        every { context.packageManager } returns mockk(relaxed = true)
        every { context.packageName } returns "com.smilepile"

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.resetAppForOnboarding()
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { backupManager.clearAllData() }
        coVerify { settingsManager.setOnboardingCompleted(false) }
        verify { securePreferencesManager.clearPIN() }
        verify { securePreferencesManager.setBiometricEnabled(false) }
    }

    @Test
    fun `loads backup statistics`() = runTest {
        // Given
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // Then - Should be loaded on init
        val backupStats = viewModel.uiState.value.backupStats
        assertNotNull(backupStats)
        assertEquals(5, backupStats!!.categoryCount)
        assertEquals(100, backupStats.photoCount)
        assertTrue(backupStats.success)
    }

    @Test
    fun `clears error state`() = runTest {
        // Given
        coEvery { backupManager.exportToZip(any()) } returns Result.failure(RuntimeException("Test error"))
        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        viewModel.prepareExport()
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testScheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `exports all settings`() = runTest {
        // Given
        val testSettings = mapOf("key1" to "value1", "key2" to 42)
        coEvery { settingsManager.exportSettings() } returns testSettings

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)

        // When
        val settings = viewModel.exportAllSettings()

        // Then
        assertEquals(testSettings, settings)
    }

    @Test
    fun `imports all settings`() = runTest {
        // Given
        val testSettings = mapOf("key1" to "value1", "key2" to 42)
        coEvery { settingsManager.importSettings(any(), any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.importAllSettings(testSettings, overwrite = true)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.importSettings(testSettings, true) }
    }

    @Test
    fun `resets settings to defaults`() = runTest {
        // Given
        coEvery { settingsManager.resetToDefaults() } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.resetSettingsToDefaults()
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.resetToDefaults() }
    }

    @Test
    fun `updates gallery settings`() = runTest {
        // Given
        coEvery { settingsManager.setGridSize(any()) } just Runs
        coEvery { settingsManager.setSortOrder(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.updateGallerySettings(gridSize = 4, sortOrder = SettingsManager.SortOrder.DATE_NEWEST)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.setGridSize(4) }
        coVerify { settingsManager.setSortOrder(SettingsManager.SortOrder.DATE_NEWEST) }
    }

    @Test
    fun `updates notification settings`() = runTest {
        // Given
        coEvery { settingsManager.setNotificationsEnabled(any()) } just Runs
        coEvery { settingsManager.setBackupNotifications(any()) } just Runs
        coEvery { settingsManager.setMemoryNotifications(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.updateNotificationSettings(
            notificationsEnabled = true,
            backupNotifications = false,
            memoryNotifications = true
        )
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.setNotificationsEnabled(true) }
        coVerify { settingsManager.setBackupNotifications(false) }
        coVerify { settingsManager.setMemoryNotifications(true) }
    }

    @Test
    fun `updates backup settings`() = runTest {
        // Given
        coEvery { settingsManager.setAutoBackupEnabled(any()) } just Runs
        coEvery { settingsManager.setBackupWifiOnly(any()) } just Runs
        coEvery { settingsManager.setBackupFrequency(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.updateBackupSettings(
            autoBackupEnabled = true,
            wifiOnly = true,
            frequency = SettingsManager.BackupFrequency.WEEKLY
        )
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.setAutoBackupEnabled(true) }
        coVerify { settingsManager.setBackupWifiOnly(true) }
        coVerify { settingsManager.setBackupFrequency(SettingsManager.BackupFrequency.WEEKLY) }
    }

    @Test
    fun `updates photo quality settings`() = runTest {
        // Given
        coEvery { settingsManager.setUploadQuality(any()) } just Runs
        coEvery { settingsManager.setThumbnailQuality(any()) } just Runs
        coEvery { settingsManager.setAutoOptimizeStorage(any()) } just Runs

        viewModel = SettingsViewModel(context, themeManager, backupManager, securePreferencesManager, biometricManager, settingsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.updatePhotoQualitySettings(
            uploadQuality = SettingsManager.PhotoQuality.HIGH,
            thumbnailQuality = SettingsManager.PhotoQuality.MEDIUM,
            autoOptimize = true
        )
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.setUploadQuality(SettingsManager.PhotoQuality.HIGH) }
        coVerify { settingsManager.setThumbnailQuality(SettingsManager.PhotoQuality.MEDIUM) }
        coVerify { settingsManager.setAutoOptimizeStorage(true) }
    }
}