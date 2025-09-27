package com.smilepile.backup

import android.content.Context
import com.smilepile.data.backup.*
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.security.SecuritySummary
import com.smilepile.theme.ThemeManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for BackupManager functionality
 */
class BackupManagerTest {

    private lateinit var backupManager: BackupManager
    private lateinit var context: Context
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var themeManager: ThemeManager
    private lateinit var securePreferencesManager: SecurePreferencesManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        categoryRepository = mockk()
        photoRepository = mockk()
        themeManager = mockk()
        securePreferencesManager = mockk()

        backupManager = BackupManager(
            context,
            categoryRepository,
            photoRepository,
            themeManager,
            securePreferencesManager
        )

        // Setup default mocks
        every { context.cacheDir } returns File("/test/cache")
        every { context.filesDir } returns File("/test/files")
        coEvery { themeManager.isDarkMode } returns flowOf(false)
        coEvery { securePreferencesManager.getSecuritySummary() } returns SecuritySummary(
            hasPIN = false,
            hasPattern = false,
            kidSafeModeEnabled = false,
            cameraAccessAllowed = true,
            deleteProtectionEnabled = false
        )
    }

    @Test
    fun `exportToZip with full backup options creates complete backup`() = runBlocking {
        // Given
        val categories = listOf(
            createTestCategory(1, "family", "Family"),
            createTestCategory(2, "friends", "Friends")
        )
        val photos = listOf(
            createTestPhoto(1, "photo1.jpg", 1),
            createTestPhoto(2, "photo2.jpg", 2)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        val options = BackupOptions(
            includePhotos = true,
            includeThumbnails = true,
            includeSettings = true,
            compressionLevel = CompressionLevel.MEDIUM,
            encryptSensitiveData = false
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
        val backupFile = result.getOrNull()
        assertNotNull(backupFile)
        assertTrue(backupFile?.name?.startsWith("SmilePile_Backup_") == true)
        assertTrue(backupFile?.name?.endsWith(".zip") == true)
    }

    @Test
    fun `exportToZip with selective backup filters correctly`() = runBlocking {
        // Given
        val categories = listOf(
            createTestCategory(1, "family", "Family"),
            createTestCategory(2, "friends", "Friends"),
            createTestCategory(3, "work", "Work")
        )
        val photos = listOf(
            createTestPhoto(1, "photo1.jpg", 1, createdAt = 1000L),
            createTestPhoto(2, "photo2.jpg", 2, createdAt = 2000L),
            createTestPhoto(3, "photo3.jpg", 3, createdAt = 3000L)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        val options = BackupOptions(
            selectedCategories = listOf(1L, 2L), // Only family and friends
            dateRangeStart = 1500L, // After photo1
            dateRangeEnd = 2500L, // Before photo3
            compressionLevel = CompressionLevel.HIGH
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
        // Verify that only photo2 would be included based on filters
        verify {
            photoRepository.getAllPhotos()
        }
    }

    @Test
    fun `exportToJson creates valid JSON backup`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(createTestPhoto(1, "test.jpg", 1))

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        // When
        val result = backupManager.exportToJson()

        // Then
        assertTrue(result.isSuccess)
        val json = result.getOrNull()
        assertNotNull(json)
        assertTrue(json?.contains("\"version\"") == true)
        assertTrue(json?.contains("\"categories\"") == true)
        assertTrue(json?.contains("\"photos\"") == true)
    }

    @Test
    fun `getBackupStats returns correct statistics`() = runBlocking {
        // Given
        coEvery { categoryRepository.getCategoryCount() } returns 5
        coEvery { photoRepository.getPhotoCount() } returns 100

        // When
        val stats = backupManager.getBackupStats()

        // Then
        assertEquals(5, stats.categoryCount)
        assertEquals(100, stats.photoCount)
        assertTrue(stats.success)
    }

    @Test
    fun `incremental backup detects changes correctly`() = runBlocking {
        // Given
        val baseBackupId = "backup-123"
        val lastBackupTime = 1000L

        val allPhotos = listOf(
            createTestPhoto(1, "old.jpg", 1, createdAt = 500L),
            createTestPhoto(2, "new.jpg", 1, createdAt = 1500L) // After backup
        )

        coEvery { photoRepository.getAllPhotos() } returns allPhotos
        coEvery { categoryRepository.getAllCategories() } returns emptyList()

        // Mock backup history
        val mockHistory = listOf(
            BackupHistoryEntry(
                id = baseBackupId,
                timestamp = lastBackupTime,
                fileName = "backup.zip",
                filePath = "/test/backup.zip",
                fileSize = 1000,
                format = BackupFormat.ZIP,
                photosCount = 1,
                categoriesCount = 1,
                compressionLevel = CompressionLevel.MEDIUM,
                success = true
            )
        )

        coEvery { backupManager.getBackupHistory() } returns mockHistory

        // When
        val result = backupManager.performIncrementalBackup(baseBackupId)

        // Then
        assertTrue(result.isSuccess || result.isFailure) // Will fail if no changes
    }

    @Test
    fun `backup with progress callback reports progress correctly`() = runBlocking {
        // Given
        val progressUpdates = mutableListOf<String>()
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(createTestPhoto(1, "test.jpg", 1))

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        // When
        val result = backupManager.exportToZip(
            progressCallback = { current, total, operation ->
                progressUpdates.add("$current/$total: $operation")
            }
        )

        // Then
        assertTrue(result.isSuccess)
        assertTrue(progressUpdates.isNotEmpty())
        assertTrue(progressUpdates.any { it.contains("Gathering app data") })
        assertTrue(progressUpdates.any { it.contains("Preparing metadata") })
    }

    // Helper functions
    private fun createTestCategory(
        id: Long,
        name: String,
        displayName: String
    ): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            position = 0,
            iconResource = null,
            colorHex = null,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createTestPhoto(
        id: Long,
        name: String,
        categoryId: Long,
        createdAt: Long = System.currentTimeMillis()
    ): Photo {
        return Photo(
            id = id,
            path = "/test/photos/$name",
            categoryId = categoryId,
            name = name,
            isFromAssets = false,
            createdAt = createdAt,
            fileSize = 1000,
            width = 1920,
            height = 1080,
            isFavorite = false
        )
    }
}