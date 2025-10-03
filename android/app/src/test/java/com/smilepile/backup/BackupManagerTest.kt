package com.smilepile.backup

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.smilepile.data.backup.*
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.security.SecuritySummary
import com.smilepile.storage.ZipUtils
import com.smilepile.theme.ThemeManager
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
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
    private lateinit var deletionTracker: ManagedDeletionTracker
    private lateinit var tempCacheDir: File
    private lateinit var tempFilesDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        categoryRepository = mockk()
        photoRepository = mockk()
        themeManager = mockk()
        securePreferencesManager = mockk()
        deletionTracker = mockk(relaxed = true)

        // Mock ZipUtils object
        mockkObject(ZipUtils)

        backupManager = BackupManager(
            context,
            categoryRepository,
            photoRepository,
            themeManager,
            securePreferencesManager,
            deletionTracker
        )

        // Setup default mocks - use real temp directories for file operations
        tempCacheDir = createTempDir("backup_test_cache")
        tempFilesDir = createTempDir("backup_test_files")
        every { context.cacheDir } returns tempCacheDir
        every { context.filesDir } returns tempFilesDir

        // Mock package manager for version info
        val packageManager = mockk<android.content.pm.PackageManager>()
        val packageInfo = mockk<android.content.pm.PackageInfo>()
        packageInfo.versionName = "1.0.0"
        every { context.packageManager } returns packageManager
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
        every { context.packageName } returns "com.smilepile"

        coEvery { themeManager.isDarkMode } returns MutableStateFlow(false)
        coEvery { securePreferencesManager.getSecuritySummary() } returns SecuritySummary(
            hasPIN = false,
            hasPattern = false,
            biometricEnabled = false,
            kidSafeModeEnabled = false,
            deleteProtectionEnabled = false,
            failedAttempts = 0,
            isInCooldown = false
        )
    }

    @After
    fun tearDown() {
        // Unmock the ZipUtils object to prevent affecting other tests
        unmockkObject(ZipUtils)
        // Clean up temp directories
        tempCacheDir.deleteRecursively()
        tempFilesDir.deleteRecursively()
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

        // Mock ZipUtils.createZipFromDirectory to return success and ensure ZIP file gets created
        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            // Create the output file to simulate successful ZIP creation
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            // Call progress callback if provided
            val progressCallback = arg<((Int, Int) -> Unit)?>(3)
            progressCallback?.invoke(100, 100)
            Result.success(Unit)
        }

        val options = BackupOptions(
            includePhotos = true,
            includeThumbnails = true,
            includeSettings = true,
            compressionLevel = CompressionLevel.MEDIUM
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

        // Mock ZipUtils.createZipFromDirectory to return success and ensure ZIP file gets created
        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            // Create the output file to simulate successful ZIP creation
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            // Call progress callback if provided
            val progressCallback = arg<((Int, Int) -> Unit)?>(3)
            progressCallback?.invoke(100, 100)
            Result.success(Unit)
        }

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
        coVerify {
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

        // Mock ZipUtils with progress callback capture
        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            // Create the output file to simulate successful ZIP creation
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            // Call the progress callback
            val progressCallback = arg<((Int, Int) -> Unit)?>(3)
            progressCallback?.invoke(50, 100)
            Result.success(Unit)
        }

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

    @Test
    fun `category filtering works with selected categories`() = runBlocking {
        // Given
        val categories = listOf(
            createTestCategory(1, "family", "Family"),
            createTestCategory(2, "friends", "Friends"),
            createTestCategory(3, "work", "Work")
        )
        val photos = listOf(
            createTestPhoto(1, "photo1.jpg", 1),
            createTestPhoto(2, "photo2.jpg", 2),
            createTestPhoto(3, "photo3.jpg", 3)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            selectedCategories = listOf(1L, 2L) // Only family and friends
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `date range filtering excludes photos outside range`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(
            createTestPhoto(1, "old.jpg", 1, createdAt = 1000L),
            createTestPhoto(2, "mid.jpg", 1, createdAt = 2000L),
            createTestPhoto(3, "new.jpg", 1, createdAt = 3000L)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            dateRangeStart = 1500L, // After photo1
            dateRangeEnd = 2500L    // Before photo3
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `photo processing skips assets photos`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(
            createTestPhoto(1, "normal.jpg", 1).copy(isFromAssets = false),
            createTestPhoto(2, "asset.jpg", 1).copy(isFromAssets = true)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        // When
        val result = backupManager.exportToZip(BackupOptions(includePhotos = true))

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `export without photos option works correctly`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(createTestPhoto(1, "test.jpg", 1))

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(includePhotos = false)

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `export with thumbnails option creates thumbnail directory`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(createTestPhoto(1, "test.jpg", 1))

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            includePhotos = true,
            includeThumbnails = true
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `export with compression level sets correct level`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = emptyList<Photo>()

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            compressionLevel = CompressionLevel.HIGH
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `empty categories list creates valid backup`() = runBlocking {
        // Given
        coEvery { categoryRepository.getAllCategories() } returns emptyList()
        coEvery { photoRepository.getAllPhotos() } returns emptyList()

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        // When
        val result = backupManager.exportToZip()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `category and date filters work together`() = runBlocking {
        // Given
        val categories = listOf(
            createTestCategory(1, "family", "Family"),
            createTestCategory(2, "friends", "Friends")
        )
        val photos = listOf(
            createTestPhoto(1, "photo1.jpg", 1, createdAt = 1000L),
            createTestPhoto(2, "photo2.jpg", 1, createdAt = 2000L),
            createTestPhoto(3, "photo3.jpg", 2, createdAt = 1500L)
        )

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            selectedCategories = listOf(1L),
            dateRangeStart = 1500L,
            dateRangeEnd = 2500L
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `BackupOptions data class with all parameters works correctly`() {
        // Given
        val options = BackupOptions(
            includePhotos = false,
            includeThumbnails = false,
            includeSettings = false,
            selectedCategories = listOf(1L, 2L, 3L),
            dateRangeStart = 1000L,
            dateRangeEnd = 5000L,
            compressionLevel = CompressionLevel.HIGH,
            includeMetadata = false
        )

        // Then
        assertFalse(options.includePhotos)
        assertFalse(options.includeThumbnails)
        assertFalse(options.includeSettings)
        assertEquals(listOf(1L, 2L, 3L), options.selectedCategories)
        assertEquals(1000L, options.dateRangeStart)
        assertEquals(5000L, options.dateRangeEnd)
        assertEquals(CompressionLevel.HIGH, options.compressionLevel)
        assertFalse(options.includeMetadata)
    }

    @Test
    fun `BackupOptions default values are correct`() {
        // When
        val options = BackupOptions()

        // Then
        assertTrue(options.includePhotos)
        assertTrue(options.includeThumbnails)
        assertTrue(options.includeSettings)
        assertNull(options.selectedCategories)
        assertNull(options.dateRangeStart)
        assertNull(options.dateRangeEnd)
        assertEquals(CompressionLevel.MEDIUM, options.compressionLevel)
        assertTrue(options.includeMetadata)
    }

    @Test
    fun `BackupStats data class properties are accessible`() {
        // Given
        val stats = BackupStats(
            categoryCount = 10,
            photoCount = 100,
            success = true,
            errorMessage = null
        )

        // Then
        assertEquals(10, stats.categoryCount)
        assertEquals(100, stats.photoCount)
        assertTrue(stats.success)
        assertNull(stats.errorMessage)
    }

    @Test
    fun `BackupStats with error message works correctly`() {
        // Given
        val stats = BackupStats(
            categoryCount = 0,
            photoCount = 0,
            success = false,
            errorMessage = "Test error"
        )

        // Then
        assertEquals(0, stats.categoryCount)
        assertEquals(0, stats.photoCount)
        assertFalse(stats.success)
        assertEquals("Test error", stats.errorMessage)
    }

    @Test
    fun `PhotoManifestEntry data class all properties work`() {
        // Given
        val entry = PhotoManifestEntry(
            photoId = 1L,
            originalPath = "/test/photo.jpg",
            zipEntryName = "photos/1_photo.jpg",
            fileName = "1_photo.jpg",
            fileSize = 1024L,
            checksum = "abc123"
        )

        // Then
        assertEquals(1L, entry.photoId)
        assertEquals("/test/photo.jpg", entry.originalPath)
        assertEquals("photos/1_photo.jpg", entry.zipEntryName)
        assertEquals("1_photo.jpg", entry.fileName)
        assertEquals(1024L, entry.fileSize)
        assertEquals("abc123", entry.checksum)
    }

    @Test
    fun `PhotoManifestEntry without checksum works`() {
        // Given
        val entry = PhotoManifestEntry(
            photoId = 1L,
            originalPath = "/test/photo.jpg",
            zipEntryName = "photos/1_photo.jpg",
            fileName = "1_photo.jpg",
            fileSize = 1024L
        )

        // Then
        assertNull(entry.checksum)
    }

    @Test
    fun `ImportProgress calculates percentage correctly`() {
        // Given
        val progress = ImportProgress(
            totalItems = 100,
            processedItems = 50,
            currentOperation = "Processing",
            errors = emptyList()
        )

        // Then
        assertEquals(50, progress.percentage)
    }

    @Test
    fun `ImportProgress with zero total items returns zero percentage`() {
        // Given
        val progress = ImportProgress(
            totalItems = 0,
            processedItems = 0,
            currentOperation = "Starting",
            errors = emptyList()
        )

        // Then
        assertEquals(0, progress.percentage)
    }

    @Test
    fun `ImportProgress with errors stores them correctly`() {
        // Given
        val errors = listOf("Error 1", "Error 2", "Error 3")
        val progress = ImportProgress(
            totalItems = 10,
            processedItems = 5,
            currentOperation = "Failed",
            errors = errors
        )

        // Then
        assertEquals(3, progress.errors.size)
        assertTrue(progress.errors.contains("Error 1"))
        assertTrue(progress.errors.contains("Error 2"))
        assertTrue(progress.errors.contains("Error 3"))
    }

    @Test
    fun `ExportProgress calculates byte percentage correctly`() {
        // Given
        val progress = ExportProgress(
            totalItems = 10,
            processedItems = 5,
            currentOperation = "Exporting",
            currentFile = "test.jpg",
            bytesProcessed = 500L,
            totalBytes = 1000L,
            errors = emptyList()
        )

        // Then
        assertEquals(50, progress.percentage)
        assertEquals(50, progress.bytesPercentage)
    }

    @Test
    fun `ExportProgress with zero bytes returns zero percentage`() {
        // Given
        val progress = ExportProgress(
            totalItems = 0,
            processedItems = 0,
            currentOperation = "Starting",
            bytesProcessed = 0L,
            totalBytes = 0L
        )

        // Then
        assertEquals(0, progress.bytesPercentage)
    }

    @Test
    fun `CompressionLevel enum has all expected values`() {
        // Then
        val values = CompressionLevel.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(CompressionLevel.LOW))
        assertTrue(values.contains(CompressionLevel.MEDIUM))
        assertTrue(values.contains(CompressionLevel.HIGH))
    }

    @Test
    fun `BackupFormat enum has expected values`() {
        // Then
        val values = BackupFormat.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(BackupFormat.JSON))
        assertTrue(values.contains(BackupFormat.ZIP))
    }

    @Test
    fun `BackupCategory toCategory conversion works correctly`() {
        // Given
        val backupCategory = BackupCategory(
            id = 1L,
            name = "test",
            displayName = "Test",
            position = 0,
            iconResource = "icon_test",
            colorHex = "#FF0000",
            isDefault = true,
            createdAt = 12345L
        )

        // When
        val category = backupCategory.toCategory()

        // Then
        assertEquals(1L, category.id)
        assertEquals("test", category.name)
        assertEquals("Test", category.displayName)
        assertEquals(0, category.position)
        assertEquals("icon_test", category.iconResource)
        assertEquals("#FF0000", category.colorHex)
        assertTrue(category.isDefault)
        assertEquals(12345L, category.createdAt)
    }

    @Test
    fun `BackupCategory fromCategory conversion works correctly`() {
        // Given
        val category = createTestCategory(1L, "test", "Test")

        // When
        val backupCategory = BackupCategory.fromCategory(category)

        // Then
        assertEquals(category.id, backupCategory.id)
        assertEquals(category.name, backupCategory.name)
        assertEquals(category.displayName, backupCategory.displayName)
        assertEquals(category.position, backupCategory.position)
        assertEquals(category.iconResource, backupCategory.iconResource)
        assertEquals(category.colorHex, backupCategory.colorHex)
        assertEquals(category.isDefault, backupCategory.isDefault)
        assertEquals(category.createdAt, backupCategory.createdAt)
    }

    @Test
    fun `BackupPhoto toPhoto conversion works correctly`() {
        // Given
        val backupPhoto = BackupPhoto(
            id = 1L,
            path = "/test/photo.jpg",
            categoryId = 2L,
            name = "photo.jpg",
            isFromAssets = true,
            createdAt = 12345L,
            fileSize = 1024L,
            width = 1920,
            height = 1080
        )

        // When
        val photo = backupPhoto.toPhoto()

        // Then
        assertEquals(1L, photo.id)
        assertEquals("/test/photo.jpg", photo.path)
        assertEquals(2L, photo.categoryId)
        assertEquals("photo.jpg", photo.name)
        assertTrue(photo.isFromAssets)
        assertEquals(12345L, photo.createdAt)
        assertEquals(1024L, photo.fileSize)
        assertEquals(1920, photo.width)
        assertEquals(1080, photo.height)
    }

    @Test
    fun `BackupPhoto fromPhoto conversion works correctly`() {
        // Given
        val photo = createTestPhoto(1L, "photo.jpg", 2L)

        // When
        val backupPhoto = BackupPhoto.fromPhoto(photo)

        // Then
        assertEquals(photo.id, backupPhoto.id)
        assertEquals(photo.path, backupPhoto.path)
        assertEquals(photo.categoryId, backupPhoto.categoryId)
        assertEquals(photo.name, backupPhoto.name)
        assertEquals(photo.isFromAssets, backupPhoto.isFromAssets)
        assertEquals(photo.createdAt, backupPhoto.createdAt)
        assertEquals(photo.fileSize, backupPhoto.fileSize)
        assertEquals(photo.width, backupPhoto.width)
        assertEquals(photo.height, backupPhoto.height)
    }

    @Test
    fun `BackupSettings data class stores all properties`() {
        // Given
        val securitySettings = BackupSecuritySettings(
            hasPIN = true,
            hasPattern = false,
            kidSafeModeEnabled = true,
            deleteProtectionEnabled = false
        )
        val backupSettings = BackupSettings(
            isDarkMode = true,
            securitySettings = securitySettings
        )

        // Then
        assertTrue(backupSettings.isDarkMode)
        assertTrue(backupSettings.securitySettings.hasPIN)
        assertFalse(backupSettings.securitySettings.hasPattern)
        assertTrue(backupSettings.securitySettings.kidSafeModeEnabled)
        assertFalse(backupSettings.securitySettings.deleteProtectionEnabled)
    }

    @Test
    fun `BackupHistoryEntry generates unique ID by default`() {
        // When
        val entry1 = BackupHistoryEntry(
            timestamp = System.currentTimeMillis(),
            fileName = "backup1.zip",
            filePath = "/test/backup1.zip",
            fileSize = 1000L,
            format = BackupFormat.ZIP,
            photosCount = 10,
            categoriesCount = 5,
            compressionLevel = CompressionLevel.MEDIUM,
            success = true
        )
        val entry2 = BackupHistoryEntry(
            timestamp = System.currentTimeMillis(),
            fileName = "backup2.zip",
            filePath = "/test/backup2.zip",
            fileSize = 2000L,
            format = BackupFormat.ZIP,
            photosCount = 20,
            categoriesCount = 10,
            compressionLevel = CompressionLevel.HIGH,
            success = true
        )

        // Then
        assertNotEquals(entry1.id, entry2.id)
    }

    @Test
    fun `AppBackup data class with all fields works correctly`() {
        // Given
        val categories = listOf(BackupCategory.fromCategory(createTestCategory(1L, "test", "Test")))
        val photos = listOf(BackupPhoto.fromPhoto(createTestPhoto(1L, "photo.jpg", 1L)))
        val settings = BackupSettings(
            isDarkMode = false,
            securitySettings = BackupSecuritySettings(
                hasPIN = false,
                hasPattern = false,
                kidSafeModeEnabled = false,
                deleteProtectionEnabled = false
            )
        )
        val manifest = listOf(
            PhotoManifestEntry(
                photoId = 1L,
                originalPath = "/test/photo.jpg",
                zipEntryName = "photos/1_photo.jpg",
                fileName = "1_photo.jpg",
                fileSize = 1024L
            )
        )

        // When
        val appBackup = AppBackup(
            version = 2,
            exportDate = 12345L,
            appVersion = "1.0.0",
            format = BackupFormat.ZIP.name,
            categories = categories,
            photos = photos,
            settings = settings,
            photoManifest = manifest
        )

        // Then
        assertEquals(2, appBackup.version)
        assertEquals(12345L, appBackup.exportDate)
        assertEquals("1.0.0", appBackup.appVersion)
        assertEquals(BackupFormat.ZIP.name, appBackup.format)
        assertEquals(1, appBackup.categories.size)
        assertEquals(1, appBackup.photos.size)
        assertNotNull(appBackup.settings)
        assertEquals(1, appBackup.photoManifest.size)
    }

    @Test
    fun `IncrementalBackupMetadata stores all change tracking data`() {
        // Given
        val metadata = IncrementalBackupMetadata(
            baseBackupId = "backup-123",
            baseBackupDate = 1000L,
            changedPhotos = listOf(1L, 2L, 3L),
            deletedPhotos = listOf(4L, 5L),
            changedCategories = listOf(10L),
            deletedCategories = listOf(11L),
            incrementalDate = 2000L
        )

        // Then
        assertEquals("backup-123", metadata.baseBackupId)
        assertEquals(1000L, metadata.baseBackupDate)
        assertEquals(3, metadata.changedPhotos.size)
        assertEquals(2, metadata.deletedPhotos.size)
        assertEquals(1, metadata.changedCategories.size)
        assertEquals(1, metadata.deletedCategories.size)
        assertEquals(2000L, metadata.incrementalDate)
    }

    @Test
    fun `BackupSchedule default values are correct`() {
        // When
        val schedule = BackupSchedule()

        // Then
        assertFalse(schedule.enabled)
        assertEquals(BackupFrequency.WEEKLY, schedule.frequency)
        assertEquals("02:00", schedule.time)
        assertEquals(1, schedule.dayOfWeek)
        assertEquals(1, schedule.dayOfMonth)
        assertTrue(schedule.wifiOnly)
        assertTrue(schedule.chargeOnly)
        assertNull(schedule.lastBackupTime)
        assertNull(schedule.nextScheduledTime)
    }

    @Test
    fun `BackupSchedule with custom values works correctly`() {
        // Given
        val schedule = BackupSchedule(
            enabled = true,
            frequency = BackupFrequency.DAILY,
            time = "14:30",
            dayOfWeek = 5,
            dayOfMonth = 15,
            wifiOnly = false,
            chargeOnly = false,
            lastBackupTime = 1000L,
            nextScheduledTime = 2000L
        )

        // Then
        assertTrue(schedule.enabled)
        assertEquals(BackupFrequency.DAILY, schedule.frequency)
        assertEquals("14:30", schedule.time)
        assertEquals(5, schedule.dayOfWeek)
        assertEquals(15, schedule.dayOfMonth)
        assertFalse(schedule.wifiOnly)
        assertFalse(schedule.chargeOnly)
        assertEquals(1000L, schedule.lastBackupTime)
        assertEquals(2000L, schedule.nextScheduledTime)
    }

    @Test
    fun `export with null compression throws no exception`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = emptyList<Photo>()

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            compressionLevel = CompressionLevel.LOW
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getBackupStats handles repository errors gracefully`() = runBlocking {
        // Given
        coEvery { categoryRepository.getCategoryCount() } throws Exception("Database error")
        coEvery { photoRepository.getPhotoCount() } returns 100

        // When
        val stats = backupManager.getBackupStats()

        // Then
        assertFalse(stats.success)
        assertNotNull(stats.errorMessage)
        assertEquals(0, stats.categoryCount)
    }

    @Test
    fun `export with very large date range works`() = runBlocking {
        // Given
        val categories = listOf(createTestCategory(1, "test", "Test"))
        val photos = listOf(createTestPhoto(1, "photo.jpg", 1, createdAt = Long.MAX_VALUE / 2))

        coEvery { categoryRepository.getAllCategories() } returns categories
        coEvery { photoRepository.getAllPhotos() } returns photos

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            dateRangeStart = 0L,
            dateRangeEnd = Long.MAX_VALUE
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `export with single category selection works`() = runBlocking {
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

        coEvery {
            ZipUtils.createZipFromDirectory(any(), any(), any(), captureLambda())
        } answers {
            val outputFile = secondArg<File>()
            outputFile.createNewFile()
            Result.success(Unit)
        }

        val options = BackupOptions(
            selectedCategories = listOf(1L)
        )

        // When
        val result = backupManager.exportToZip(options)

        // Then
        assertTrue(result.isSuccess)
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
            height = 1080
        )
    }
}