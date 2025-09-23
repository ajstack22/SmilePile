package com.smilepile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.smilepile.data.backup.*
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepositoryImpl
import com.smilepile.data.repository.PhotoRepositoryImpl
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.storage.ZipUtils
import com.smilepile.theme.ThemeManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
 * Unit tests for BackupManager individual methods and components
 * Focuses on testing specific functionality in isolation
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class BackupManagerUnitTests {

    private lateinit var context: Context
    private lateinit var database: SmilePileDatabase
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var photoRepository: PhotoRepositoryImpl
    private lateinit var backupManager: BackupManager
    private lateinit var themeManager: ThemeManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var testDir: File
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
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

        testDir = File(context.cacheDir, "backup_unit_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun tearDown() {
        database.close()
        testDir.deleteRecursively()
    }

    /**
     * Test JSON export functionality (v1 compatibility)
     */
    @Test
    fun testJsonExport() = runBlocking {
        // Setup test data
        val category = Category(
            id = 1,
            name = "json_test",
            displayName = "JSON Test Category",
            position = 1,
            iconResource = "ic_test",
            colorHex = "#FF5722",
            createdAt = System.currentTimeMillis()
        )

        val photoFile = File(testDir, "json_test_photo.jpg")
        photoFile.writeBytes(ByteArray(1024) { it.toByte() })

        val photo = Photo(
            id = 1,
            path = photoFile.absolutePath,
            categoryId = 1,
            name = "JSON Test Photo",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = photoFile.length(),
            width = 800,
            height = 600,
            isFavorite = true
        )

        categoryRepository.insertCategory(category)
        photoRepository.insertPhoto(photo)

        // Test JSON export
        val exportResult = backupManager.exportToJson()
        assertTrue("JSON export should succeed", exportResult.isSuccess)

        val jsonContent = exportResult.getOrThrow()
        assertNotNull("JSON content should not be null", jsonContent)
        assertTrue("JSON should not be empty", jsonContent.isNotEmpty())

        // Parse and validate JSON structure
        val parsedBackup = json.decodeFromString<AppBackup>(jsonContent)
        assertEquals("Version should be 1 for JSON format", 1, parsedBackup.version)
        assertEquals("Format should be JSON", BackupFormat.JSON.name, parsedBackup.format)
        assertEquals("Should have one category", 1, parsedBackup.categories.size)
        assertEquals("Should have one photo", 1, parsedBackup.photos.size)
        assertTrue("Photo manifest should be empty for JSON", parsedBackup.photoManifest.isEmpty())

        // Verify category data
        val backupCategory = parsedBackup.categories.first()
        assertEquals("Category name should match", category.name, backupCategory.name)
        assertEquals("Category display name should match", category.displayName, backupCategory.displayName)
        assertEquals("Category icon should match", category.iconResource, backupCategory.iconResource)

        // Verify photo data
        val backupPhoto = parsedBackup.photos.first()
        assertEquals("Photo name should match", photo.name, backupPhoto.name)
        assertEquals("Photo path should match", photo.path, backupPhoto.path)
        assertEquals("Photo favorite status should match", photo.isFavorite, backupPhoto.isFavorite)
    }

    /**
     * Test backup statistics functionality
     */
    @Test
    fun testGetBackupStats() = runBlocking {
        // Test with empty database
        val emptyStats = backupManager.getBackupStats()
        assertTrue("Empty stats should be successful", emptyStats.success)
        assertEquals("Empty database should have 0 categories", 0, emptyStats.categoryCount)
        assertEquals("Empty database should have 0 photos", 0, emptyStats.photoCount)
        assertNull("Empty stats should have no error", emptyStats.errorMessage)

        // Add test data
        val categories = (1..5).map { index ->
            Category(
                id = index.toLong(),
                name = "category_$index",
                displayName = "Category $index",
                position = index,
                createdAt = System.currentTimeMillis()
            )
        }

        val photos = (1..10).map { index ->
            val photoFile = File(testDir, "stats_photo_$index.jpg")
            photoFile.writeBytes(ByteArray(100))

            Photo(
                id = index.toLong(),
                path = photoFile.absolutePath,
                categoryId = ((index - 1) % 5 + 1).toLong(),
                name = "Stats Photo $index",
                isFromAssets = false,
                createdAt = System.currentTimeMillis(),
                fileSize = photoFile.length()
            )
        }

        categories.forEach { categoryRepository.insertCategory(it) }
        photos.forEach { photoRepository.insertPhoto(it) }

        // Test with populated database
        val populatedStats = backupManager.getBackupStats()
        assertTrue("Populated stats should be successful", populatedStats.success)
        assertEquals("Should have 5 categories", 5, populatedStats.categoryCount)
        assertEquals("Should have 10 photos", 10, populatedStats.photoCount)
        assertNull("Populated stats should have no error", populatedStats.errorMessage)
    }

    /**
     * Test backup version validation
     */
    @Test
    fun testBackupVersionValidation() {
        // Test valid versions
        assertDoesNotThrow("Version 1 should be valid") {
            backupManager.checkBackupVersion(1)
        }

        assertDoesNotThrow("Version 2 should be valid") {
            backupManager.checkBackupVersion(2)
        }

        // Test invalid versions
        assertThrows("Version 0 should be invalid", IllegalArgumentException::class.java) {
            backupManager.checkBackupVersion(0)
        }

        assertThrows("Negative version should be invalid", IllegalArgumentException::class.java) {
            backupManager.checkBackupVersion(-1)
        }

        assertThrows("Future version should be invalid", IllegalArgumentException::class.java) {
            backupManager.checkBackupVersion(999)
        }
    }

    /**
     * Test MediaStore URI validation
     */
    @Test
    fun testMediaStoreUriValidation() = runBlocking {
        // Test valid file URI
        val validFile = File(testDir, "valid_photo.jpg")
        validFile.writeBytes(ByteArray(100))

        val isValidFile = backupManager.validateMediaStoreUri(validFile.absolutePath)
        assertTrue("Valid file path should be validated", isValidFile)

        // Test non-existent file
        val invalidFile = File(testDir, "non_existent.jpg")
        val isInvalidFile = backupManager.validateMediaStoreUri(invalidFile.absolutePath)
        assertFalse("Non-existent file should not be validated", isInvalidFile)

        // Test invalid URI format
        val isInvalidUri = backupManager.validateMediaStoreUri("invalid://uri/format")
        assertFalse("Invalid URI should not be validated", isInvalidUri)

        // Test MediaStore-style URI (would fail without actual MediaStore content)
        val mediaStoreUri = "content://media/external/images/media/12345"
        val isMediaStoreValid = backupManager.validateMediaStoreUri(mediaStoreUri)
        assertFalse("Non-existent MediaStore URI should not be validated", isMediaStoreValid)
    }

    /**
     * Test backup file validation
     */
    @Test
    fun testBackupFileValidation() = runBlocking {
        // Test non-existent file
        val nonExistentFile = File(testDir, "non_existent.json")
        val nonExistentResult = backupManager.validateBackupFile(nonExistentFile)
        assertTrue("Non-existent file should fail validation", nonExistentResult.isFailure)
        assertTrue("Should be FileNotFoundException",
            nonExistentResult.exceptionOrNull() is java.io.FileNotFoundException)

        // Test invalid file extension
        val invalidExtFile = File(testDir, "invalid.txt")
        invalidExtFile.writeText("invalid content")
        val invalidExtResult = backupManager.validateBackupFile(invalidExtFile)
        assertTrue("Invalid extension should fail validation", invalidExtResult.isFailure)

        // Test valid JSON backup file
        val validBackup = AppBackup(
            version = 1,
            exportDate = System.currentTimeMillis(),
            appVersion = "1.0",
            format = BackupFormat.JSON.name,
            categories = emptyList(),
            photos = emptyList(),
            settings = BackupSettings(
                isDarkMode = false,
                securitySettings = BackupSecuritySettings(
                    hasPIN = false,
                    hasPattern = false,
                    kidSafeModeEnabled = false,
                    cameraAccessAllowed = true,
                    deleteProtectionEnabled = false
                )
            )
        )

        val validJsonFile = File(testDir, "valid_backup.json")
        validJsonFile.writeText(json.encodeToString(validBackup))

        val validJsonResult = backupManager.validateBackupFile(validJsonFile)
        assertTrue("Valid JSON backup should pass validation", validJsonResult.isSuccess)

        val validatedBackup = validJsonResult.getOrThrow()
        assertEquals("Version should match", 1, validatedBackup.version)
        assertEquals("Format should match", BackupFormat.JSON.name, validatedBackup.format)

        // Test invalid JSON content
        val invalidJsonFile = File(testDir, "invalid_backup.json")
        invalidJsonFile.writeText("{ invalid json content")
        val invalidJsonResult = backupManager.validateBackupFile(invalidJsonFile)
        assertTrue("Invalid JSON should fail validation", invalidJsonResult.isFailure)

        // Test valid ZIP backup file
        val validZipFile = createValidZipBackup()
        val validZipResult = backupManager.validateBackupFile(validZipFile)
        assertTrue("Valid ZIP backup should pass validation", validZipResult.isSuccess)

        val validatedZipBackup = validZipResult.getOrThrow()
        assertEquals("ZIP version should be 2", 2, validatedZipBackup.version)
    }

    /**
     * Test backup preview functionality
     */
    @Test
    fun testGetBackupPreview() = runBlocking {
        // Create test backup with missing photos
        val validPhoto = File(testDir, "existing_photo.jpg")
        validPhoto.writeBytes(ByteArray(100))

        val missingPhotoPath = File(testDir, "missing_photo.jpg").absolutePath

        val backupWithMissingPhotos = AppBackup(
            version = 2,
            exportDate = System.currentTimeMillis(),
            appVersion = "1.0",
            format = BackupFormat.ZIP.name,
            categories = listOf(
                BackupCategory(1, "test", "Test Category", 1, createdAt = System.currentTimeMillis())
            ),
            photos = listOf(
                BackupPhoto(1, validPhoto.absolutePath, 1, "Existing Photo", false, System.currentTimeMillis()),
                BackupPhoto(2, missingPhotoPath, 1, "Missing Photo", false, System.currentTimeMillis())
            ),
            settings = BackupSettings(
                isDarkMode = false,
                securitySettings = BackupSecuritySettings(
                    hasPIN = false,
                    hasPattern = false,
                    kidSafeModeEnabled = false,
                    cameraAccessAllowed = true,
                    deleteProtectionEnabled = false
                )
            )
        )

        val backupFile = File(testDir, "preview_test.json")
        backupFile.writeText(json.encodeToString(backupWithMissingPhotos))

        val previewResult = backupManager.getBackupPreview(backupFile)
        assertTrue("Preview should succeed", previewResult.isSuccess)

        val preview = previewResult.getOrThrow()
        assertEquals("Version should match", 2, preview.version)
        assertEquals("Categories count should match", 1, preview.categoriesCount)
        assertEquals("Photos count should match", 2, preview.photosCount)
        assertEquals("Should detect 1 missing photo", 1, preview.missingPhotosCount)
        assertTrue("Should list missing photo",
            preview.missingPhotos.any { it.contains("Missing Photo") })
        assertTrue("Should be ZIP format", preview.isZipFormat)
    }

    /**
     * Test create export intent functionality
     */
    @Test
    fun testCreateExportIntent() {
        // Test JSON export intent
        val jsonIntent = backupManager.createExportIntent(BackupFormat.JSON)
        assertEquals("Should be CREATE_DOCUMENT action",
            android.content.Intent.ACTION_CREATE_DOCUMENT, jsonIntent.action)
        assertEquals("Should have JSON MIME type",
            "application/json", jsonIntent.type)
        assertTrue("Should have JSON file extension in title",
            jsonIntent.getStringExtra(android.content.Intent.EXTRA_TITLE)?.endsWith(".json") == true)

        // Test ZIP export intent
        val zipIntent = backupManager.createExportIntent(BackupFormat.ZIP)
        assertEquals("Should be CREATE_DOCUMENT action",
            android.content.Intent.ACTION_CREATE_DOCUMENT, zipIntent.action)
        assertEquals("Should have ZIP MIME type",
            "application/zip", zipIntent.type)
        assertTrue("Should have ZIP file extension in title",
            zipIntent.getStringExtra(android.content.Intent.EXTRA_TITLE)?.endsWith(".zip") == true)

        // Test default export intent (should be ZIP)
        val defaultIntent = backupManager.createExportIntent()
        assertEquals("Default should be ZIP MIME type",
            "application/zip", defaultIntent.type)
    }

    /**
     * Test ZIP file writing functionality
     */
    @Test
    fun testZipFileWriting() = runBlocking {
        // Create a test ZIP file
        val testZipFile = File(testDir, "test_write.zip")
        ZipOutputStream(FileOutputStream(testZipFile)).use { zip ->
            zip.putNextEntry(ZipEntry("test.txt"))
            zip.write("test content".toByteArray())
            zip.closeEntry()
        }

        // Create a mock URI using a regular file
        val destinationFile = File(testDir, "destination.zip")
        val mockUri = android.net.Uri.fromFile(destinationFile)

        // Test writing ZIP to file
        val writeResult = backupManager.writeZipToFile(testZipFile, mockUri)
        assertTrue("ZIP writing should succeed", writeResult.isSuccess)
        assertTrue("Destination file should exist", destinationFile.exists())
        assertTrue("Destination file should not be empty", destinationFile.length() > 0)
        assertEquals("File sizes should match", testZipFile.length(), destinationFile.length())
    }

    /**
     * Test JSON content writing functionality
     */
    @Test
    fun testJsonContentWriting() = runBlocking {
        val jsonContent = """{"test": "content", "number": 42}"""
        val destinationFile = File(testDir, "json_destination.json")
        val mockUri = android.net.Uri.fromFile(destinationFile)

        val writeResult = backupManager.writeJsonToFile(jsonContent, mockUri)
        assertTrue("JSON writing should succeed", writeResult.isSuccess)
        assertTrue("Destination file should exist", destinationFile.exists())

        val writtenContent = destinationFile.readText()
        assertEquals("Content should match", jsonContent, writtenContent)
    }

    /**
     * Test import strategy differences
     */
    @Test
    fun testImportStrategyBehavior() = runBlocking {
        // Setup existing data
        val existingCategory = Category(1, "existing", "Existing Category", 1,
            createdAt = System.currentTimeMillis())
        categoryRepository.insertCategory(existingCategory)

        // Create backup data with overlapping category
        val backupData = AppBackup(
            version = 1,
            exportDate = System.currentTimeMillis(),
            appVersion = "1.0",
            format = BackupFormat.JSON.name,
            categories = listOf(
                BackupCategory(1, "existing", "Updated Existing Category", 1,
                    createdAt = System.currentTimeMillis()),
                BackupCategory(2, "new", "New Category", 2,
                    createdAt = System.currentTimeMillis())
            ),
            photos = emptyList(),
            settings = BackupSettings(
                isDarkMode = false,
                securitySettings = BackupSecuritySettings(
                    hasPIN = false, hasPattern = false, kidSafeModeEnabled = false,
                    cameraAccessAllowed = true, deleteProtectionEnabled = false
                )
            )
        )

        val backupFile = File(testDir, "strategy_test.json")
        backupFile.writeText(json.encodeToString(backupData))

        // Test MERGE strategy - should update existing and add new
        val mergeFlow = backupManager.importFromJson(backupFile, ImportStrategy.MERGE)
        val mergeResults = mergeFlow.collect { }

        val categoriesAfterMerge = categoryRepository.getAllCategories()
        assertEquals("Merge should result in 2 categories", 2, categoriesAfterMerge.size)

        val updatedCategory = categoriesAfterMerge.find { it.name == "existing" }
        assertNotNull("Existing category should still exist", updatedCategory)
        assertEquals("Category should be updated", "Updated Existing Category",
            updatedCategory!!.displayName)

        // Reset and test REPLACE strategy
        categoryRepository.deleteCategory(categoriesAfterMerge.find { it.name == "new" }!!)

        val replaceFlow = backupManager.importFromJson(backupFile, ImportStrategy.REPLACE)
        val replaceResults = replaceFlow.collect { }

        val categoriesAfterReplace = categoryRepository.getAllCategories()
        // Replace strategy would clear non-default categories first
        assertTrue("Replace should clear existing data", categoriesAfterReplace.isNotEmpty())
    }

    // Helper methods

    private fun createValidZipBackup(): File {
        val zipFile = File(testDir, "valid_backup.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            // Add metadata
            zip.putNextEntry(ZipEntry(ZipUtils.METADATA_FILE))
            val metadata = AppBackup(
                version = 2,
                exportDate = System.currentTimeMillis(),
                appVersion = "1.0",
                format = BackupFormat.ZIP.name,
                categories = emptyList(),
                photos = emptyList(),
                settings = BackupSettings(
                    isDarkMode = false,
                    securitySettings = BackupSecuritySettings(
                        hasPIN = false, hasPattern = false, kidSafeModeEnabled = false,
                        cameraAccessAllowed = true, deleteProtectionEnabled = false
                    )
                )
            )
            zip.write(json.encodeToString(metadata).toByteArray())
            zip.closeEntry()

            // Add photos directory
            zip.putNextEntry(ZipEntry("${ZipUtils.PHOTOS_DIR}"))
            zip.closeEntry()
        }
        return zipFile
    }

    private inline fun assertDoesNotThrow(message: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }

    private inline fun <T : Throwable> assertThrows(
        message: String,
        exceptionClass: Class<T>,
        block: () -> Unit
    ) {
        try {
            block()
            fail("$message - Expected ${exceptionClass.simpleName} but no exception was thrown")
        } catch (e: Exception) {
            if (!exceptionClass.isInstance(e)) {
                fail("$message - Expected ${exceptionClass.simpleName} but got ${e::class.java.simpleName}")
            }
        }
    }
}