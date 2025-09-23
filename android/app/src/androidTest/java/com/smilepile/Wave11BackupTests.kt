package com.smilepile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.BackupFormat
import com.smilepile.data.backup.ImportStrategy
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepositoryImpl
import com.smilepile.data.repository.PhotoRepositoryImpl
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.storage.ZipUtils
import com.smilepile.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Comprehensive tests for Wave 11 ZIP backup functionality
 * Tests the complete export/import cycle, device transfer simulation,
 * edge cases, and ZipUtils functionality
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class Wave11BackupTests {

    private lateinit var context: Context
    private lateinit var database: SmilePileDatabase
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var photoRepository: PhotoRepositoryImpl
    private lateinit var backupManager: BackupManager
    private lateinit var themeManager: ThemeManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        categoryRepository = CategoryRepositoryImpl(
            categoryDao = database.categoryDao(),
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO
        )
        photoRepository = PhotoRepositoryImpl(
            photoDao = database.photoDao(),
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO
        )

        // Mock dependencies
        themeManager = ThemeManager(context)
        val secureStorageManager = com.smilepile.security.SecureStorageManager(context)
        securePreferencesManager = SecurePreferencesManager(context, secureStorageManager)

        backupManager = BackupManager(
            context = context,
            categoryRepository = categoryRepository,
            photoRepository = photoRepository,
            themeManager = themeManager,
            securePreferencesManager = securePreferencesManager
        )

        // Create test directory
        testDir = File(context.cacheDir, "wave11_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun tearDown() {
        database.close()
        testDir.deleteRecursively()
    }

    /**
     * Test Scenario 1: Basic Export/Import Cycle
     */
    @Test
    fun testBasicExportImportCycle() = runBlocking {
        // Setup test data
        val testCategory = Category(
            id = 1,
            name = "test_category",
            displayName = "Test Category",
            position = 1,
            iconResource = "ic_test",
            colorHex = "#FF5722",
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )

        // Create a test photo file
        val testPhotoFile = File(testDir, "test_photo.jpg")
        createTestImageFile(testPhotoFile)

        val testPhoto = Photo(
            id = 1,
            path = testPhotoFile.absolutePath,
            categoryId = 1,
            name = "Test Photo",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = testPhotoFile.length(),
            width = 800,
            height = 600,
            isFavorite = false
        )

        // Insert test data
        categoryRepository.insertCategory(testCategory)
        photoRepository.insertPhoto(testPhoto)

        // Test export to ZIP
        val exportResult = backupManager.exportToZip(tempDir = testDir)
        assertTrue("Export should succeed", exportResult.isSuccess)

        val zipFile = exportResult.getOrThrow()
        assertTrue("ZIP file should exist", zipFile.exists())
        assertTrue("ZIP file should not be empty", zipFile.length() > 0)

        // Validate ZIP structure
        val structureResult = ZipUtils.validateZipStructure(zipFile)
        assertTrue("ZIP structure should be valid", structureResult.isSuccess)

        val structure = structureResult.getOrThrow()
        assertTrue("Should have metadata", structure.hasMetadata)
        assertTrue("Should have photos directory", structure.hasPhotosDirectory)
        assertEquals("Should have one photo", 1, structure.photoCount)

        // Clear existing data
        photoRepository.deletePhoto(testPhoto)
        categoryRepository.deleteCategory(testCategory)

        // Verify data is cleared
        assertEquals("Should have no categories", 0, categoryRepository.getAllCategories().size)
        assertEquals("Should have no photos", 0, photoRepository.getAllPhotos().size)

        // Test import from ZIP
        val importFlow = backupManager.importFromZip(
            zipFile = zipFile,
            strategy = ImportStrategy.REPLACE
        )

        val importResults = importFlow.toList()
        val finalResult = importResults.last()

        assertTrue("Import should complete", finalResult.currentOperation.contains("completed"))
        assertEquals("Should have processed all items", finalResult.totalItems, finalResult.processedItems)

        // Verify imported data
        val importedCategories = categoryRepository.getAllCategories()
        val importedPhotos = photoRepository.getAllPhotos()

        assertEquals("Should have restored category", 1, importedCategories.size)
        assertEquals("Should have restored photo", 1, importedPhotos.size)

        val restoredCategory = importedCategories.first()
        assertEquals("Category name should match", testCategory.name, restoredCategory.name)
        assertEquals("Category display name should match", testCategory.displayName, restoredCategory.displayName)

        val restoredPhoto = importedPhotos.first()
        assertEquals("Photo name should match", testPhoto.name, restoredPhoto.name)
        assertEquals("Photo category should match", testPhoto.categoryId, restoredPhoto.categoryId)

        // Verify photo file was restored
        val restoredPhotoFile = File(restoredPhoto.path)
        assertTrue("Restored photo file should exist", restoredPhotoFile.exists())
        assertTrue("Restored photo file should not be empty", restoredPhotoFile.length() > 0)
    }

    /**
     * Test Scenario 2: Device Transfer Simulation
     */
    @Test
    fun testDeviceTransferSimulation() = runBlocking {
        // Create comprehensive test data set
        val categories = listOf(
            Category(1, "family", "Family", 1, "ic_family", "#2196F3", false, System.currentTimeMillis()),
            Category(2, "pets", "Pets", 2, "ic_pets", "#4CAF50", false, System.currentTimeMillis()),
            Category(3, "vacation", "Vacation", 3, "ic_vacation", "#FF9800", false, System.currentTimeMillis())
        )

        val photoFiles = mutableListOf<File>()
        val photos = categories.mapIndexed { index, category ->
            val photoFile = File(testDir, "photo_${category.name}_$index.jpg")
            createTestImageFile(photoFile)
            photoFiles.add(photoFile)

            Photo(
                id = index.toLong() + 1,
                path = photoFile.absolutePath,
                categoryId = category.id,
                name = "${category.displayName} Photo ${index + 1}",
                isFromAssets = false,
                createdAt = System.currentTimeMillis() - (index * 1000),
                fileSize = photoFile.length(),
                width = 1024,
                height = 768,
                isFavorite = index % 2 == 0
            )
        }

        // Insert test data
        categories.forEach { categoryRepository.insertCategory(it) }
        photos.forEach { photoRepository.insertPhoto(it) }

        // Export to ZIP with progress tracking
        var lastProgress = 0
        val exportResult = backupManager.exportToZip(testDir) { current, total, operation ->
            assertTrue("Progress should not go backwards", current >= lastProgress)
            assertTrue("Current should not exceed total", current <= total)
            assertNotNull("Operation should not be null", operation)
            lastProgress = current
        }

        assertTrue("Export should succeed", exportResult.isSuccess)
        val backupZip = exportResult.getOrThrow()

        // Get backup stats for validation
        val backupStats = backupManager.getBackupStats()
        assertTrue("Backup stats should be successful", backupStats.success)
        assertEquals("Should match category count", categories.size, backupStats.categoryCount)
        assertEquals("Should match photo count", photos.size, backupStats.photoCount)

        // Simulate device transfer: clear all app data
        photos.forEach { photoRepository.deletePhoto(it) }
        categories.forEach { categoryRepository.deleteCategory(it) }

        // Delete original photo files to simulate new device
        photoFiles.forEach { it.delete() }

        // Verify complete data wipe
        assertEquals("All categories should be deleted", 0, categoryRepository.getAllCategories().size)
        assertEquals("All photos should be deleted", 0, photoRepository.getAllPhotos().size)

        // Import on "new device"
        val importFlow = backupManager.importFromZip(
            zipFile = backupZip,
            strategy = ImportStrategy.REPLACE
        ) { current, total, operation ->
            assertTrue("Import progress should be valid", current <= total)
            assertNotNull("Import operation should not be null", operation)
        }

        val importResults = importFlow.toList()
        val finalImportResult = importResults.last()

        // Verify import completion
        assertTrue("Import should complete successfully",
            finalImportResult.currentOperation.contains("completed"))
        assertTrue("Import should have no errors", finalImportResult.errors.isEmpty())

        // Verify all data was restored
        val restoredCategories = categoryRepository.getAllCategories()
        val restoredPhotos = photoRepository.getAllPhotos()

        assertEquals("All categories should be restored", categories.size, restoredCategories.size)
        assertEquals("All photos should be restored", photos.size, restoredPhotos.size)

        // Verify data integrity
        categories.forEach { originalCategory ->
            val restored = restoredCategories.find { it.name == originalCategory.name }
            assertNotNull("Category ${originalCategory.name} should be restored", restored)
            assertEquals("Display name should match", originalCategory.displayName, restored!!.displayName)
            assertEquals("Icon should match", originalCategory.iconResource, restored.iconResource)
            assertEquals("Color should match", originalCategory.colorHex, restored.colorHex)
        }

        // Verify photo files were restored to internal storage
        restoredPhotos.forEach { photo ->
            val photoFile = File(photo.path)
            assertTrue("Photo file should exist: ${photo.name}", photoFile.exists())
            assertTrue("Photo file should not be empty: ${photo.name}", photoFile.length() > 0)
            assertTrue("Photo should be in internal storage",
                photo.path.contains(context.filesDir.absolutePath))
        }
    }

    /**
     * Test Scenario 3: Edge Cases and Error Handling
     */
    @Test
    fun testEdgeCasesAndErrorHandling() = runBlocking {
        // Test 1: Export with no data
        val emptyExportResult = backupManager.exportToZip(testDir)
        assertTrue("Empty export should still succeed", emptyExportResult.isSuccess)

        val emptyZipFile = emptyExportResult.getOrThrow()
        val emptyStructure = ZipUtils.validateZipStructure(emptyZipFile).getOrThrow()
        assertTrue("Empty backup should have metadata", emptyStructure.hasMetadata)
        assertEquals("Empty backup should have no photos", 0, emptyStructure.photoCount)

        // Test 2: Special characters in filenames
        val specialCategory = Category(
            id = 1,
            name = "special_chars",
            displayName = "Special Characters !@#$%",
            position = 1,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )

        val specialPhotoFile = File(testDir, "photo with spaces & symbols!@#.jpg")
        createTestImageFile(specialPhotoFile)

        val specialPhoto = Photo(
            id = 1,
            path = specialPhotoFile.absolutePath,
            categoryId = 1,
            name = "Photo with !@#$% special chars",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = specialPhotoFile.length()
        )

        categoryRepository.insertCategory(specialCategory)
        photoRepository.insertPhoto(specialPhoto)

        val specialExportResult = backupManager.exportToZip(testDir)
        assertTrue("Export with special characters should succeed", specialExportResult.isSuccess)

        // Test 3: Large photo set (create multiple photos)
        val largePhotoSet = (1..20).map { index ->
            val photoFile = File(testDir, "large_set_photo_$index.jpg")
            createTestImageFile(photoFile, size = 1024 * 10) // 10KB files

            Photo(
                id = index.toLong() + 10,
                path = photoFile.absolutePath,
                categoryId = 1,
                name = "Large Set Photo $index",
                isFromAssets = false,
                createdAt = System.currentTimeMillis(),
                fileSize = photoFile.length()
            )
        }

        largePhotoSet.forEach { photoRepository.insertPhoto(it) }

        val largeExportResult = backupManager.exportToZip(testDir)
        assertTrue("Large photo set export should succeed", largeExportResult.isSuccess)

        val largeZipFile = largeExportResult.getOrThrow()
        val largeStructure = ZipUtils.validateZipStructure(largeZipFile).getOrThrow()
        assertEquals("Should have correct photo count", 21, largeStructure.photoCount) // 1 + 20

        // Test 4: Corrupted ZIP handling
        val corruptedZip = File(testDir, "corrupted.zip")
        corruptedZip.writeText("This is not a valid ZIP file")

        val corruptedImportFlow = backupManager.importFromZip(corruptedZip)
        val corruptedResults = corruptedImportFlow.toList()
        val corruptedFinalResult = corruptedResults.last()

        assertTrue("Corrupted ZIP import should fail gracefully",
            corruptedFinalResult.errors.isNotEmpty())
        assertTrue("Should report import failure",
            corruptedFinalResult.currentOperation.contains("failed"))

        // Test 5: Missing photos in ZIP
        val incompleteZip = createIncompleteZip()
        val incompleteImportFlow = backupManager.importFromZip(incompleteZip)
        val incompleteResults = incompleteImportFlow.toList()
        // Should complete but with warnings about missing files

        // Test 6: Version compatibility
        val futurVersionZip = createFutureVersionZip()
        val futureVersionValidation = backupManager.validateBackupFile(futurVersionZip)
        assertTrue("Future version should be rejected", futureVersionValidation.isFailure)
    }

    /**
     * Test ZipUtils functionality in isolation
     */
    @Test
    fun testZipUtilsFunctionality() = runBlocking {
        // Test 1: Create ZIP from directory
        val sourceDir = File(testDir, "zip_source")
        sourceDir.mkdirs()

        // Create metadata file
        val metadataFile = File(sourceDir, "metadata.json")
        metadataFile.writeText("""{"version": 2, "format": "ZIP"}""")

        // Create photos directory with test files
        val photosDir = File(sourceDir, "photos")
        photosDir.mkdirs()

        val testPhotos = (1..5).map { index ->
            val photoFile = File(photosDir, "test_photo_$index.jpg")
            createTestImageFile(photoFile)
            photoFile
        }

        val outputZip = File(testDir, "zip_utils_test.zip")

        var progressCalls = 0
        val createResult = ZipUtils.createZipFromDirectory(
            sourceDir = sourceDir,
            outputFile = outputZip
        ) { current, total ->
            progressCalls++
            assertTrue("Progress should be valid", current <= total)
        }

        assertTrue("ZIP creation should succeed", createResult.isSuccess)
        assertTrue("ZIP file should exist", outputZip.exists())
        assertTrue("Progress callback should be called", progressCalls > 0)

        // Test 2: Validate ZIP structure
        val structureResult = ZipUtils.validateZipStructure(outputZip)
        assertTrue("Structure validation should succeed", structureResult.isSuccess)

        val structure = structureResult.getOrThrow()
        assertTrue("Should have metadata", structure.hasMetadata)
        assertTrue("Should have photos directory", structure.hasPhotosDirectory)
        assertEquals("Should have correct photo count", 5, structure.photoCount)

        // Test 3: Extract ZIP
        val extractDir = File(testDir, "extracted")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(outputZip, extractDir)
        assertTrue("ZIP extraction should succeed", extractResult.isSuccess)

        val extractedFiles = extractResult.getOrThrow()
        assertTrue("Should extract metadata file",
            extractedFiles.any { it.name == "metadata.json" })
        assertEquals("Should extract correct number of photo files",
            5, extractedFiles.count { it.name.endsWith(".jpg") })

        // Test 4: Get ZIP info
        val zipInfoResult = ZipUtils.getZipInfo(outputZip)
        assertTrue("ZIP info should be retrievable", zipInfoResult.isSuccess)

        val zipInfo = zipInfoResult.getOrThrow()
        assertEquals("Should have correct entry count", 6, zipInfo.entryCount) // 1 metadata + 5 photos
        assertTrue("Should have compression ratio", zipInfo.compressionRatio > 0)

        // Test 5: Security validation - ZIP bomb detection
        val zipBombFile = createZipBombFile()
        val bombStructureResult = ZipUtils.validateZipStructure(zipBombFile)
        // Should detect and reject ZIP bomb
        assertTrue("ZIP bomb should be detected", bombStructureResult.isFailure)
    }

    /**
     * Test storage consolidation features
     */
    @Test
    fun testStorageConsolidation() = runBlocking {
        // Test moving photos from external to internal storage during import
        val externalPhotoFile = File(testDir, "external_photo.jpg")
        createTestImageFile(externalPhotoFile)

        val category = Category(1, "test", "Test", 1, createdAt = System.currentTimeMillis())
        val externalPhoto = Photo(
            id = 1,
            path = externalPhotoFile.absolutePath,
            categoryId = 1,
            name = "External Photo",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = externalPhotoFile.length()
        )

        categoryRepository.insertCategory(category)
        photoRepository.insertPhoto(externalPhoto)

        // Export and then import to trigger consolidation
        val exportResult = backupManager.exportToZip(testDir)
        assertTrue("Export should succeed", exportResult.isSuccess)

        val zipFile = exportResult.getOrThrow()

        // Clear data
        photoRepository.deletePhoto(externalPhoto)
        categoryRepository.deleteCategory(category)
        externalPhotoFile.delete()

        // Import should consolidate photos to internal storage
        val importFlow = backupManager.importFromZip(zipFile, ImportStrategy.REPLACE)
        val importResults = importFlow.toList()
        val finalResult = importResults.last()

        assertTrue("Import should complete", finalResult.currentOperation.contains("completed"))

        val restoredPhotos = photoRepository.getAllPhotos()
        assertEquals("Should have one restored photo", 1, restoredPhotos.size)

        val restoredPhoto = restoredPhotos.first()
        assertTrue("Photo should be in internal storage",
            restoredPhoto.path.contains(context.filesDir.absolutePath))

        val restoredFile = File(restoredPhoto.path)
        assertTrue("Consolidated photo file should exist", restoredFile.exists())
    }

    /**
     * Test merge vs replace import strategies
     */
    @Test
    fun testImportStrategies() = runBlocking {
        // Setup initial data
        val existingCategory = Category(1, "existing", "Existing", 1,
            createdAt = System.currentTimeMillis())
        val existingPhotoFile = File(testDir, "existing.jpg")
        createTestImageFile(existingPhotoFile)
        val existingPhoto = Photo(1, existingPhotoFile.absolutePath, 1, "Existing",
            false, System.currentTimeMillis(), existingPhotoFile.length())

        categoryRepository.insertCategory(existingCategory)
        photoRepository.insertPhoto(existingPhoto)

        // Create backup with new data
        val newCategory = Category(2, "new", "New Category", 2,
            createdAt = System.currentTimeMillis())
        val newPhotoFile = File(testDir, "new.jpg")
        createTestImageFile(newPhotoFile)
        val newPhoto = Photo(2, newPhotoFile.absolutePath, 2, "New Photo",
            false, System.currentTimeMillis(), newPhotoFile.length())

        categoryRepository.insertCategory(newCategory)
        photoRepository.insertPhoto(newPhoto)

        val backupResult = backupManager.exportToZip(testDir)
        val backupZip = backupResult.getOrThrow()

        // Clear and restore only existing data
        photoRepository.deletePhoto(newPhoto)
        categoryRepository.deleteCategory(newCategory)

        // Test MERGE strategy
        val mergeFlow = backupManager.importFromZip(backupZip, ImportStrategy.MERGE)
        val mergeResults = mergeFlow.toList()

        val allCategoriesAfterMerge = categoryRepository.getAllCategories()
        val allPhotosAfterMerge = photoRepository.getAllPhotos()

        assertEquals("Merge should have both categories", 2, allCategoriesAfterMerge.size)
        assertEquals("Merge should have both photos", 2, allPhotosAfterMerge.size)

        // Test REPLACE strategy
        val replaceFlow = backupManager.importFromZip(backupZip, ImportStrategy.REPLACE)
        val replaceResults = replaceFlow.toList()

        val allCategoriesAfterReplace = categoryRepository.getAllCategories()
        val allPhotosAfterReplace = photoRepository.getAllPhotos()

        assertEquals("Replace should have backup categories", 2, allCategoriesAfterReplace.size)
        assertEquals("Replace should have backup photos", 2, allPhotosAfterReplace.size)
    }

    // Helper methods

    private fun createTestImageFile(file: File, size: Int = 1024) {
        val data = ByteArray(size) { (it % 256).toByte() }
        file.writeBytes(data)
    }

    private fun createIncompleteZip(): File {
        val incompleteZip = File(testDir, "incomplete.zip")
        ZipOutputStream(FileOutputStream(incompleteZip)).use { zip ->
            // Add metadata but no photos
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write("""{"version": 2, "photos": [{"id": 1, "name": "missing"}]}""".toByteArray())
            zip.closeEntry()
        }
        return incompleteZip
    }

    private fun createFutureVersionZip(): File {
        val futureZip = File(testDir, "future_version.zip")
        ZipOutputStream(FileOutputStream(futureZip)).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write("""{"version": 999, "format": "FUTURE"}""".toByteArray())
            zip.closeEntry()
        }
        return futureZip
    }

    private fun createZipBombFile(): File {
        val bombZip = File(testDir, "bomb.zip")
        ZipOutputStream(FileOutputStream(bombZip)).use { zip ->
            // Create entry with very high compression ratio
            val entry = ZipEntry("bomb.txt").apply {
                size = 1024L * 1024L * 100L // 100MB uncompressed
                compressedSize = 1024L // 1KB compressed
            }
            zip.putNextEntry(entry)
            zip.write("bomb".toByteArray())
            zip.closeEntry()
        }
        return bombZip
    }
}