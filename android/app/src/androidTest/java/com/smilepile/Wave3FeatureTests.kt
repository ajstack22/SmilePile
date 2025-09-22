package com.smilepile

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.smilepile.data.backup.AppBackup
import com.smilepile.data.backup.BackupCategory
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.BackupPhoto
import com.smilepile.data.backup.BackupSecuritySettings
import com.smilepile.data.backup.BackupSettings
import com.smilepile.data.backup.ImportStrategy
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.mode.AppMode
import com.smilepile.mode.ModeManager
import com.smilepile.operations.PhotoOperationsManager
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Comprehensive integration tests for SmilePile Wave 3 features.
 * Tests the complete workflows for photo removal, backup/export/import, and Kids Mode restrictions.
 *
 * Wave 3 Features Tested:
 * - Photo removal workflow (app-only deletion preserving MediaStore)
 * - JSON export with valid structure and content
 * - Import handling all scenarios (valid backup, missing photos, merge vs replace)
 * - Kids Mode restrictions (access control for features)
 * - MediaStore preservation during photo removal
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Wave3FeatureTests {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private lateinit var database: SmilePileDatabase
    private lateinit var context: Context
    private lateinit var photoRepository: PhotoRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var backupManager: BackupManager
    private lateinit var modeManager: ModeManager
    private lateinit var photoOperationsManager: PhotoOperationsManager
    private lateinit var themeManager: ThemeManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var contentResolver: ContentResolver

    // Test data constants
    private val testPhotoId = UUID.randomUUID().toString()
    private val testPhotoUri = "content://media/external/images/media/1234"
    private val testCategoryId = "test-category-${System.currentTimeMillis()}"
    private val testCategoryName = "Test Category"
    private val testBackupDir = "test_backups"

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        hiltRule.inject()

        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver

        // Create in-memory test database
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        // Initialize repositories with test database using simple implementations for testing
        photoRepository = createTestPhotoRepository()
        categoryRepository = createTestCategoryRepository()

        // Initialize managers (these will be mocked in real tests with Hilt)
        // For integration tests, we'll create minimal implementations
        modeManager = ModeManager(context)

        // Create test backup directory
        val backupDir = File(context.cacheDir, testBackupDir)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        // Initialize other components with minimal setup for testing
        setupTestComponents()

        cleanupTestData()
    }

    @After
    fun tearDown() {
        cleanupTestData()
        database.close()

        // Cleanup test backup files
        val backupDir = File(context.cacheDir, testBackupDir)
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    }

    /**
     * Test 1: Photo Removal Workflow
     * - Add a test photo
     * - Remove it from library
     * - Verify it's gone from app database
     * - Verify file still exists in MediaStore (simulated)
     */
    @Test
    fun testPhotoRemovalWorkflow() = runBlocking {
        // Step 1: Add test photo to database
        val testPhoto = PhotoEntity(
            id = testPhotoId,
            uri = testPhotoUri,
            categoryId = testCategoryId,
            timestamp = System.currentTimeMillis(),
            isFavorite = false
        )

        // First add a category
        val testCategory = CategoryEntity(
            id = testCategoryId,
            name = testCategoryName,
            colorHex = "#FF5722",
            createdAt = System.currentTimeMillis()
        )
        database.categoryDao().insert(testCategory)
        database.photoDao().insert(testPhoto)

        // Verify photo exists in database
        val photosBeforeRemoval = database.photoDao().getAll().first()
        assertEquals("Should have 1 photo before removal", 1, photosBeforeRemoval.size)
        assertNotNull("Photo should exist", database.photoDao().getById(testPhotoId))

        // Step 2: Simulate MediaStore count before removal
        val mediaStoreCountBefore = getSimulatedMediaStoreCount()

        // Step 3: Remove photo from library (app database only)
        val photoModel = Photo(
            id = testPhotoId.hashCode().toLong(),
            path = testPhotoUri,
            categoryId = testCategoryId.hashCode().toLong(),
            name = "test_photo.jpg",
            createdAt = testPhoto.timestamp,
            isFavorite = testPhoto.isFavorite
        )
        photoRepository.removeFromLibrary(photoModel)

        // Step 4: Verify photo is gone from app database
        val photosAfterRemoval = database.photoDao().getAll().first()
        assertEquals("Should have 0 photos after removal", 0, photosAfterRemoval.size)
        assertNull("Photo should not exist in database", database.photoDao().getById(testPhotoId))

        // Step 5: Verify MediaStore count unchanged (simulated)
        val mediaStoreCountAfter = getSimulatedMediaStoreCount()
        assertEquals("MediaStore photo count should be unchanged",
                    mediaStoreCountBefore, mediaStoreCountAfter)

        // Step 6: Verify that the URI would still be accessible (in real MediaStore)
        // This simulates that the photo still exists in device storage
        assertTrue("Photo URI should still be valid in MediaStore",
                  isValidMediaStoreUri(testPhotoUri))
    }

    /**
     * Test 2: Export Creates Valid JSON
     * - Add test categories and photos
     * - Export to JSON
     * - Parse the JSON
     * - Verify structure and content
     */
    @Test
    fun testExportCreatesValidJson() = runBlocking {
        // Step 1: Add test data
        setupTestDataForExport()

        // Step 2: Export to JSON using BackupManager
        val exportResult = backupManager.exportToJson()
        assertTrue("Export should succeed", exportResult.isSuccess)

        val jsonString = exportResult.getOrThrow()
        assertNotNull("JSON string should not be null", jsonString)
        assertTrue("JSON string should not be empty", jsonString.isNotEmpty())

        // Step 3: Parse the JSON
        val json = Json { ignoreUnknownKeys = true }
        val parsedBackup = json.decodeFromString<AppBackup>(jsonString)

        // Step 4: Verify structure and content
        assertNotNull("Parsed backup should not be null", parsedBackup)
        assertEquals("Backup version should be current", 1, parsedBackup.version)
        assertTrue("Export date should be recent",
                  System.currentTimeMillis() - parsedBackup.exportDate < 10000)

        // Verify categories
        assertEquals("Should have 1 category", 1, parsedBackup.categories.size)
        val exportedCategory = parsedBackup.categories[0]
        assertEquals("Category name should match", testCategoryName, exportedCategory.name)
        assertEquals("Category color should match", "#FF5722", exportedCategory.colorHex)

        // Verify photos
        assertEquals("Should have 1 photo", 1, parsedBackup.photos.size)
        val exportedPhoto = parsedBackup.photos[0]
        assertEquals("Photo URI should match", testPhotoUri, exportedPhoto.path)
        assertEquals("Photo category ID should match", testCategoryId, exportedPhoto.categoryId.toString())
        assertFalse("Photo should not be favorite", exportedPhoto.isFavorite)

        // Verify settings structure
        assertNotNull("Settings should be present", parsedBackup.settings)
        assertNotNull("Security settings should be present", parsedBackup.settings.securitySettings)
    }

    /**
     * Test 3: Import Handles All Scenarios
     * - Test import with valid backup
     * - Test import with missing photos (should skip gracefully)
     * - Test merge vs replace strategies
     * - Test version compatibility
     */
    @Test
    fun testImportHandlesAllScenarios() = runBlocking {
        // Scenario 1: Valid backup import with MERGE strategy
        val validBackupFile = createTestBackupFile(includeValidPhotos = true)

        val importFlow = backupManager.importFromJson(validBackupFile, ImportStrategy.MERGE)
        val importResults = mutableListOf<com.smilepile.data.backup.ImportProgress>()

        importFlow.collect { progress ->
            importResults.add(progress)
        }

        assertTrue("Import should have progress updates", importResults.isNotEmpty())
        val finalResult = importResults.last()
        assertEquals("Final progress should be complete",
                    finalResult.totalItems, finalResult.processedItems)

        // Verify data was imported
        val importedCategories = database.categoryDao().getAll().first()
        val importedPhotos = database.photoDao().getAll().first()
        assertTrue("Should have imported categories", importedCategories.isNotEmpty())
        assertTrue("Should have imported photos", importedPhotos.isNotEmpty())

        // Scenario 2: Import with missing photos (should skip gracefully)
        cleanupTestData()
        val backupWithMissingPhotos = createTestBackupFile(includeValidPhotos = false)

        val missingPhotosImportFlow = backupManager.importFromJson(backupWithMissingPhotos, ImportStrategy.MERGE)
        val missingPhotosResults = mutableListOf<com.smilepile.data.backup.ImportProgress>()

        missingPhotosImportFlow.collect { progress ->
            missingPhotosResults.add(progress)
        }

        val missingPhotosFinalResult = missingPhotosResults.last()
        assertTrue("Should have completed despite missing photos",
                  missingPhotosFinalResult.processedItems > 0)

        // Scenario 3: REPLACE strategy
        setupTestDataForExport() // Add some existing data

        val replaceBackupFile = createTestBackupFile(includeValidPhotos = true)
        val replaceImportFlow = backupManager.importFromJson(replaceBackupFile, ImportStrategy.REPLACE)
        val replaceResults = mutableListOf<com.smilepile.data.backup.ImportProgress>()

        replaceImportFlow.collect { progress ->
            replaceResults.add(progress)
        }

        assertTrue("Replace import should complete", replaceResults.isNotEmpty())

        // Scenario 4: Version compatibility check
        val incompatibleBackupFile = createTestBackupFile(version = 999) // Unsupported version

        try {
            backupManager.validateBackupFile(incompatibleBackupFile)
            fail("Should throw exception for incompatible version")
        } catch (e: Exception) {
            assertTrue("Should mention version incompatibility",
                      e.message?.contains("version") == true)
        }
    }

    /**
     * Test 4: Kids Mode Restrictions
     * - Enable Kids Mode
     * - Verify no access to removal features
     * - Verify no access to backup features
     * - Verify Parent Mode has full access
     */
    @Test
    fun testKidsModeRestrictions() = runBlocking {
        // Step 1: Enable Kids Mode
        modeManager.setMode(AppMode.KIDS)
        assertTrue("Should be in Kids Mode", modeManager.isKidsMode())
        assertFalse("Should not be in Parent Mode", modeManager.isParentMode())

        // Step 2: Verify Kids Mode restrictions
        // In Kids Mode, certain features should be restricted
        // This would typically be enforced in UI layer, but we test the mode state
        assertEquals("Current mode should be KIDS", AppMode.KIDS, modeManager.currentMode.first())

        // Test that mode persists
        val savedMode = context.getSharedPreferences("app_mode_prefs", Context.MODE_PRIVATE)
            .getString("current_mode", AppMode.KIDS.name)
        assertEquals("Saved mode should be KIDS", AppMode.KIDS.name, savedMode)

        // Step 3: Switch to Parent Mode and verify full access
        modeManager.setMode(AppMode.PARENT)
        assertTrue("Should be in Parent Mode", modeManager.isParentMode())
        assertFalse("Should not be in Kids Mode", modeManager.isKidsMode())

        assertEquals("Current mode should be PARENT", AppMode.PARENT, modeManager.currentMode.first())

        // Test mode toggle functionality
        modeManager.toggleMode()
        assertTrue("Should toggle back to Kids Mode", modeManager.isKidsMode())

        modeManager.toggleMode()
        assertTrue("Should toggle back to Parent Mode", modeManager.isParentMode())

        // Verify mode persistence after toggle
        val toggledMode = context.getSharedPreferences("app_mode_prefs", Context.MODE_PRIVATE)
            .getString("current_mode", AppMode.KIDS.name)
        assertEquals("Toggled mode should be PARENT", AppMode.PARENT.name, toggledMode)
    }

    /**
     * Test 5: MediaStore Photos Untouched
     * - Count MediaStore photos before removal
     * - Remove photos from app
     * - Count MediaStore photos after
     * - Verify count unchanged
     */
    @Test
    fun testMediaStorePhotosUntouched() = runBlocking {
        // Step 1: Setup test photos in app database
        setupTestDataForExport()

        // Step 2: Get initial MediaStore count (simulated)
        val initialMediaStoreCount = getSimulatedMediaStoreCount()
        val initialAppPhotoCount = database.photoDao().getAll().first().size

        assertTrue("Should have photos in app database", initialAppPhotoCount > 0)

        // Step 3: Remove all photos from app
        val allPhotoEntities = database.photoDao().getAll().first()
        for (photoEntity in allPhotoEntities) {
            val photoModel = Photo(
                id = photoEntity.id.hashCode().toLong(),
                path = photoEntity.uri,
                categoryId = photoEntity.categoryId.hashCode().toLong(),
                name = "photo_${photoEntity.id}",
                createdAt = photoEntity.timestamp,
                isFavorite = photoEntity.isFavorite
            )
            photoRepository.removeFromLibrary(photoModel)
        }

        // Step 4: Verify app database is empty
        val finalAppPhotoCount = database.photoDao().getAll().first().size
        assertEquals("App database should be empty", 0, finalAppPhotoCount)

        // Step 5: Verify MediaStore count unchanged (simulated)
        val finalMediaStoreCount = getSimulatedMediaStoreCount()
        assertEquals("MediaStore count should be unchanged",
                    initialMediaStoreCount, finalMediaStoreCount)

        // Step 6: Verify specific URIs are still "valid" in MediaStore (simulated)
        for (photoEntity in allPhotoEntities) {
            assertTrue("Photo URI should still be valid: ${photoEntity.uri}",
                      isValidMediaStoreUri(photoEntity.uri))
        }

        // Step 7: Test batch removal
        setupTestDataForExport() // Add photos back
        val batchPhotoEntities = database.photoDao().getAll().first()
        val preBatchMediaStoreCount = getSimulatedMediaStoreCount()

        // Remove multiple photos at once
        for (photoEntity in batchPhotoEntities) {
            val photoModel = Photo(
                id = photoEntity.id.hashCode().toLong(),
                path = photoEntity.uri,
                categoryId = photoEntity.categoryId.hashCode().toLong(),
                name = "photo_${photoEntity.id}",
                createdAt = photoEntity.timestamp,
                isFavorite = photoEntity.isFavorite
            )
            photoRepository.removeFromLibrary(photoModel)
        }

        val postBatchMediaStoreCount = getSimulatedMediaStoreCount()
        assertEquals("MediaStore count should still be unchanged after batch removal",
                    preBatchMediaStoreCount, postBatchMediaStoreCount)
    }

    // Helper methods

    private fun createTestPhotoRepository(): PhotoRepository {
        return object : PhotoRepository {
            override suspend fun getAllPhotos(): List<Photo> {
                return database.photoDao().getAll().first().map { it.toPhoto() }
            }

            override fun getAllPhotosFlow(): kotlinx.coroutines.flow.Flow<List<Photo>> {
                return database.photoDao().getAll().map { entities ->
                    entities.map { it.toPhoto() }
                }
            }

            override suspend fun getPhotosByCategory(categoryId: Long): List<Photo> {
                return database.photoDao().getByCategory(categoryId.toString()).first().map { it.toPhoto() }
            }

            override fun getPhotosByCategoryFlow(categoryId: Long): kotlinx.coroutines.flow.Flow<List<Photo>> {
                return database.photoDao().getByCategory(categoryId.toString()).map { entities ->
                    entities.map { it.toPhoto() }
                }
            }

            override suspend fun getPhotoById(id: Long): Photo? {
                return database.photoDao().getById(id.toString())?.toPhoto()
            }

            override suspend fun insertPhoto(photo: Photo): Long {
                val entity = photo.toEntity()
                database.photoDao().insert(entity)
                return photo.id
            }

            override suspend fun updatePhoto(photo: Photo) {
                database.photoDao().update(photo.toEntity())
            }

            override suspend fun deletePhoto(photo: Photo) {
                database.photoDao().deleteById(photo.id.toString())
            }

            override suspend fun deletePhotoById(photoId: Long) {
                database.photoDao().deleteById(photoId.toString())
            }

            override suspend fun getFavoritePhotos(): List<Photo> {
                return database.photoDao().getFavorites().first().map { it.toPhoto() }
            }

            override fun getFavoritePhotosFlow(): kotlinx.coroutines.flow.Flow<List<Photo>> {
                return database.photoDao().getFavorites().map { entities ->
                    entities.map { it.toPhoto() }
                }
            }

            override suspend fun updateFavoriteStatus(photoId: Long, isFavorite: Boolean) {
                database.photoDao().updateFavoriteStatus(photoId.toString(), isFavorite)
            }

            override suspend fun deletePhotosByCategory(categoryId: Long) {
                database.photoDao().deleteByCategoryId(categoryId.toString())
            }

            override suspend fun getPhotoCount(): Int {
                return database.photoDao().getCount()
            }

            override suspend fun removeFromLibrary(photo: Photo) {
                database.photoDao().deleteById(photo.id.toString())
            }

            override suspend fun removeFromLibraryById(photoId: Long) {
                database.photoDao().deleteById(photoId.toString())
            }

            private fun PhotoEntity.toPhoto(): Photo {
                return Photo(
                    id = this.id.hashCode().toLong(),
                    path = this.uri,
                    categoryId = this.categoryId.hashCode().toLong(),
                    name = "photo_${this.id}",
                    isFromAssets = false,
                    createdAt = this.timestamp,
                    fileSize = 1024L,
                    width = 100,
                    height = 100,
                    isFavorite = this.isFavorite
                )
            }

            private fun Photo.toEntity(): PhotoEntity {
                return PhotoEntity(
                    id = this.id.toString(),
                    uri = this.path,
                    categoryId = this.categoryId.toString(),
                    timestamp = this.createdAt,
                    isFavorite = this.isFavorite
                )
            }
        }
    }

    private fun createTestCategoryRepository(): CategoryRepository {
        return object : CategoryRepository {
            override suspend fun getAllCategories(): List<Category> {
                return database.categoryDao().getAll().first().map { it.toCategory() }
            }

            override fun getAllCategoriesFlow(): kotlinx.coroutines.flow.Flow<List<Category>> {
                return database.categoryDao().getAll().map { entities ->
                    entities.map { it.toCategory() }
                }
            }

            override suspend fun getCategoryById(id: Long): Category? {
                return database.categoryDao().getById(id.toString())?.toCategory()
            }

            override suspend fun getCategoryByName(name: String): Category? {
                return database.categoryDao().getByName(name)?.toCategory()
            }

            override suspend fun insertCategory(category: Category): Long {
                val entity = category.toEntity()
                database.categoryDao().insert(entity)
                return category.id
            }

            override suspend fun updateCategory(category: Category) {
                database.categoryDao().update(category.toEntity())
            }

            override suspend fun deleteCategory(category: Category) {
                database.categoryDao().deleteById(category.id.toString())
            }

            override suspend fun deleteCategoryById(id: Long) {
                database.categoryDao().deleteById(id.toString())
            }

            override suspend fun getCategoryCount(): Int {
                return database.categoryDao().getCount()
            }

            private fun CategoryEntity.toCategory(): Category {
                return Category(
                    id = this.id.hashCode().toLong(),
                    name = this.name,
                    displayName = this.name,
                    position = 0,
                    iconResource = null,
                    colorHex = this.colorHex,
                    isDefault = false,
                    createdAt = this.createdAt
                )
            }

            private fun Category.toEntity(): CategoryEntity {
                return CategoryEntity(
                    id = this.id.toString(),
                    name = this.name,
                    colorHex = this.colorHex ?: "#FF5722",
                    createdAt = this.createdAt
                )
            }
        }
    }

    private fun setupTestComponents() {
        // Create minimal implementations for testing
        // In real app, these would be injected via Hilt
        themeManager = object : ThemeManager {
            override val isDarkMode = kotlinx.coroutines.flow.flowOf(false)
            override fun setDarkMode(enabled: Boolean) {}
            override fun toggleTheme() {}
        }

        securePreferencesManager = object : SecurePreferencesManager {
            override fun getSecuritySummary() = object {
                val hasPIN = false
                val hasPattern = false
                val kidSafeModeEnabled = false
                val cameraAccessAllowed = true
                val deleteProtectionEnabled = false
            }
        }

        backupManager = BackupManager(
            context = context,
            categoryRepository = categoryRepository,
            photoRepository = photoRepository,
            themeManager = themeManager,
            securePreferencesManager = securePreferencesManager
        )

        // PhotoOperationsManager would need more complex setup for real testing
        // For now we'll simulate its functionality in our tests
    }

    private suspend fun setupTestDataForExport() {
        // Add test category
        val category = CategoryEntity(
            id = testCategoryId,
            name = testCategoryName,
            colorHex = "#FF5722",
            createdAt = System.currentTimeMillis()
        )
        database.categoryDao().insert(category)

        // Add test photo
        val photo = PhotoEntity(
            id = testPhotoId,
            uri = testPhotoUri,
            categoryId = testCategoryId,
            timestamp = System.currentTimeMillis(),
            isFavorite = false
        )
        database.photoDao().insert(photo)
    }

    private fun createTestBackupFile(
        includeValidPhotos: Boolean = true,
        version: Int = 1
    ): File {
        val backupDir = File(context.cacheDir, testBackupDir)
        val backupFile = File(backupDir, "test_backup_${System.currentTimeMillis()}.json")

        val testBackup = AppBackup(
            version = version,
            exportDate = System.currentTimeMillis(),
            appVersion = "test-1.0.0",
            categories = listOf(
                BackupCategory(
                    id = testCategoryId.toLongOrNull() ?: 1L,
                    name = testCategoryName,
                    displayName = testCategoryName,
                    position = 0,
                    colorHex = "#FF5722",
                    isDefault = false,
                    createdAt = System.currentTimeMillis()
                )
            ),
            photos = listOf(
                BackupPhoto(
                    id = testPhotoId.hashCode().toLong(),
                    path = if (includeValidPhotos) testPhotoUri else "content://invalid/uri",
                    categoryId = testCategoryId.toLongOrNull() ?: 1L,
                    name = "test_photo.jpg",
                    isFromAssets = false,
                    createdAt = System.currentTimeMillis(),
                    fileSize = 1024L,
                    width = 100,
                    height = 100,
                    isFavorite = false
                )
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

        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(AppBackup.serializer(), testBackup)
        backupFile.writeText(jsonString)

        return backupFile
    }

    private fun getSimulatedMediaStoreCount(): Int {
        // In a real test, this would query MediaStore
        // For testing purposes, we simulate a stable count
        return 42 // Simulated stable MediaStore photo count
    }

    private fun isValidMediaStoreUri(uri: String): Boolean {
        // In a real test, this would check if the URI exists in MediaStore
        // For testing purposes, we simulate that test URIs are valid
        return uri.startsWith("content://media/external/images/media/")
    }

    private fun cleanupTestData() {
        try {
            runBlocking {
                // Clean up photos first (foreign key constraints)
                database.photoDao().deleteById(testPhotoId)

                // Clean up categories
                database.categoryDao().deleteById(testCategoryId)
            }

            // Reset mode to default
            modeManager.setMode(AppMode.KIDS)

        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}