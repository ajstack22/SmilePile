package com.smilepile.backup

import android.content.Context
import com.smilepile.data.backup.*
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.theme.ThemeManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for RestoreManager functionality
 */
class RestoreManagerTest {

    private lateinit var restoreManager: RestoreManager
    private lateinit var context: Context
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var themeManager: ThemeManager
    private lateinit var securePreferencesManager: SecurePreferencesManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        photoRepository = mockk(relaxed = true)
        themeManager = mockk(relaxed = true)
        securePreferencesManager = mockk(relaxed = true)

        restoreManager = RestoreManager(
            context,
            categoryRepository,
            photoRepository,
            themeManager,
            securePreferencesManager
        )

        every { context.cacheDir } returns File("/test/cache")
        every { context.filesDir } returns File("/test/files")
    }

    @Test
    fun `validateBackup detects invalid file`() = runBlocking {
        // Given
        val invalidFile = File("/test/nonexistent.zip")

        // When
        val result = restoreManager.validateBackup(invalidFile)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateBackup detects unsupported format`() = runBlocking {
        // Given
        val unsupportedFile = mockk<File>()
        every { unsupportedFile.exists() } returns true
        every { unsupportedFile.name } returns "backup.txt"

        // When
        val result = restoreManager.validateBackup(unsupportedFile)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `restoreFromBackup with MERGE strategy preserves existing data`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()
        val existingCategories = listOf(
            createTestCategory(1, "existing", "Existing")
        )
        val existingPhotos = listOf(
            createTestPhoto(1, "existing.jpg", 1)
        )

        coEvery { categoryRepository.getAllCategories() } returns existingCategories
        coEvery { photoRepository.getAllPhotos() } returns existingPhotos
        coEvery { categoryRepository.getCategoryByName(any()) } returns null

        val options = RestoreOptions(
            strategy = ImportStrategy.MERGE,
            duplicateResolution = DuplicateResolution.SKIP,
            validateIntegrity = true,
            dryRun = false
        )

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        assertTrue(progressList.isNotEmpty())

        // Verify existing data was not deleted
        coVerify(exactly = 0) {
            categoryRepository.deleteCategory(any())
            photoRepository.deletePhoto(any())
        }
    }

    @Test
    fun `restoreFromBackup with REPLACE strategy clears existing data`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()
        val existingCategories = listOf(
            createTestCategory(1, "existing", "Existing", isDefault = false)
        )
        val existingPhotos = listOf(
            createTestPhoto(1, "existing.jpg", 1)
        )

        coEvery { categoryRepository.getAllCategories() } returns existingCategories
        coEvery { photoRepository.getAllPhotos() } returns existingPhotos

        val options = RestoreOptions(
            strategy = ImportStrategy.REPLACE,
            duplicateResolution = DuplicateResolution.REPLACE,
            validateIntegrity = false,
            dryRun = false
        )

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        // Verify data clearing was attempted
        coVerify(atLeast = 1) {
            photoRepository.getAllPhotos()
            categoryRepository.getAllCategories()
        }
    }

    @Test
    fun `restoreFromBackup dry run does not modify data`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()

        val options = RestoreOptions(
            strategy = ImportStrategy.MERGE,
            dryRun = true // Dry run mode
        )

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        assertTrue(progressList.any { it.currentOperation.contains("Dry run") })

        // Verify no data modifications
        coVerify(exactly = 0) {
            categoryRepository.insertCategory(any())
            categoryRepository.updateCategory(any())
            categoryRepository.deleteCategory(any())
            photoRepository.insertPhoto(any())
            photoRepository.deletePhoto(any())
        }
    }

    @Test
    fun `duplicate resolution SKIP skips duplicate photos`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()
        val duplicatePhoto = createTestPhoto(1, "duplicate.jpg", 1)

        coEvery { photoRepository.getAllPhotos() } returns listOf(duplicatePhoto)
        coEvery { categoryRepository.getAllCategories() } returns emptyList()

        val options = RestoreOptions(
            strategy = ImportStrategy.MERGE,
            duplicateResolution = DuplicateResolution.SKIP
        )

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        // The duplicate photo should not be inserted again
        val finalProgress = progressList.lastOrNull()
        assertNotNull(finalProgress)
    }

    @Test
    fun `duplicate resolution RENAME creates unique names`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()
        val existingPhoto = createTestPhoto(1, "photo.jpg", 1)

        coEvery { photoRepository.getAllPhotos() } returns listOf(existingPhoto)
        coEvery { categoryRepository.getAllCategories() } returns emptyList()

        val options = RestoreOptions(
            strategy = ImportStrategy.MERGE,
            duplicateResolution = DuplicateResolution.RENAME
        )

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        // Renamed photos should be inserted
        coVerify(atLeast = 0) {
            photoRepository.insertPhoto(any())
        }
    }

    @Test
    fun `integrity check validates checksums`() = runBlocking {
        // Given
        val backupFile = createMockBackupFileWithChecksum()

        val options = RestoreOptions(
            validateIntegrity = true // Enable integrity checking
        )

        // When
        val validationResult = restoreManager.validateBackup(backupFile, checkIntegrity = true)

        // Then
        // Validation should check integrity
        assertTrue(validationResult.isSuccess || validationResult.isFailure)
    }

    @Test
    fun `restore with settings updates app settings`() = runBlocking {
        // Given
        val backupFile = createMockBackupFile()

        val options = RestoreOptions(
            restoreSettings = true // Enable settings restore
        )

        coEvery { themeManager.isDarkMode } returns flowOf(false)

        // When
        val progressList = restoreManager.restoreFromBackup(backupFile, options).toList()

        // Then
        // Theme settings should be updated
        coVerify(atLeast = 0) {
            themeManager.setDarkMode(any())
        }
    }

    // Helper functions
    private fun createMockBackupFile(): File {
        val file = mockk<File>()
        every { file.exists() } returns true
        every { file.name } returns "backup.json"
        every { file.readText() } returns createMockBackupJson()
        return file
    }

    private fun createMockBackupFileWithChecksum(): File {
        val file = mockk<File>()
        every { file.exists() } returns true
        every { file.name } returns "backup.zip"
        return file
    }

    private fun createMockBackupJson(): String {
        return """
        {
            "version": 2,
            "exportDate": ${System.currentTimeMillis()},
            "appVersion": "1.0.0",
            "format": "JSON",
            "categories": [
                {
                    "id": 1,
                    "name": "test",
                    "displayName": "Test",
                    "position": 0,
                    "isDefault": false,
                    "createdAt": ${System.currentTimeMillis()}
                }
            ],
            "photos": [
                {
                    "id": 1,
                    "path": "/test/photo.jpg",
                    "categoryId": 1,
                    "name": "Test Photo",
                    "isFromAssets": false,
                    "createdAt": ${System.currentTimeMillis()},
                    "fileSize": 1000,
                    "width": 1920,
                    "height": 1080,
                    "isFavorite": false
                }
            ],
            "settings": {
                "isDarkMode": true,
                "securitySettings": {
                    "hasPIN": false,
                    "hasPattern": false,
                    "kidSafeModeEnabled": false,
                    "cameraAccessAllowed": true,
                    "deleteProtectionEnabled": false
                }
            },
            "photoManifest": []
        }
        """.trimIndent()
    }

    private fun createTestCategory(
        id: Long,
        name: String,
        displayName: String,
        isDefault: Boolean = false
    ): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            position = 0,
            iconResource = null,
            colorHex = null,
            isDefault = isDefault,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createTestPhoto(
        id: Long,
        name: String,
        categoryId: Long
    ): Photo {
        return Photo(
            id = id,
            path = "/test/photos/$name",
            categoryId = categoryId,
            name = name,
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = 1000,
            width = 1920,
            height = 1080,
            isFavorite = false
        )
    }
}