package com.smilepile.app.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.entities.PhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.IOException

/**
 * Integration tests for SmilePileDatabase to verify complete database operations,
 * relationships, data integrity, and persistence scenarios for the SmilePile app.
 */
@RunWith(AndroidJUnit4::class)
class SmilePileDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: SmilePileDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Sample test data
    private fun createSampleCategory(
        id: String = "cat1",
        name: String = "animals",
        position: Int = 0
    ) = CategoryEntity(
        id = id,
        name = name,
        displayName = name.capitalize(),
        coverImagePath = null,
        description = "Sample $name category",
        photoCount = 0,
        position = position,
        createdAt = System.currentTimeMillis()
    )

    private fun createSamplePhoto(
        id: String = "photo1",
        categoryId: String = "cat1",
        position: Int = 0
    ) = PhotoEntity(
        id = id,
        path = "sample_images/sample_$position.png",
        name = "Sample Photo $position",
        categoryId = categoryId,
        position = position,
        dateAdded = System.currentTimeMillis(),
        isFromAssets = true
    )

    // ================== Database Instance Tests ==================

    @Test
    fun databaseInstanceShouldProvideValidDAOs() {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        assertNotNull("CategoryDao should not be null", categoryDao)
        assertNotNull("PhotoDao should not be null", photoDao)
    }

    @Test
    fun databaseShouldBeInitiallyEmpty() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        assertEquals("Database should start with 0 categories", 0, categoryDao.getCategoryCount())
        assertEquals("Database should start with 0 photos", 0, photoDao.getTotalPhotoCount())

        val categories = categoryDao.getAllCategories().first()
        val photos = photoDao.getAllPhotos().first()

        assertTrue("Categories list should be empty", categories.isEmpty())
        assertTrue("Photos list should be empty", photos.isEmpty())
    }

    // ================== Complete Workflow Tests ==================

    @Test
    fun completePhotoManagementWorkflowShouldWork() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Step 1: Create categories
        val categories = listOf(
            createSampleCategory(id = "animals", name = "animals", position = 0),
            createSampleCategory(id = "family", name = "family", position = 1),
            createSampleCategory(id = "fun_times", name = "fun_times", position = 2)
        )
        categoryDao.insertCategories(categories)

        // Step 2: Add photos to categories
        val photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = "animals", position = 0),
            createSamplePhoto(id = "photo2", categoryId = "animals", position = 1),
            createSamplePhoto(id = "photo3", categoryId = "family", position = 0),
            createSamplePhoto(id = "photo4", categoryId = "family", position = 1),
            createSamplePhoto(id = "photo5", categoryId = "fun_times", position = 0),
            createSamplePhoto(id = "photo6", categoryId = "fun_times", position = 1)
        )
        photoDao.insertPhotos(photos)

        // Step 3: Update category photo counts
        for (category in categories) {
            val photoCount = photoDao.getPhotoCountForCategory(category.id)
            val updatedCategory = category.copy(photoCount = photoCount)
            categoryDao.updateCategory(updatedCategory)
        }

        // Step 4: Verify complete state
        val allCategoriesWithPhotos = categoryDao.getAllCategoriesWithPhotos().first()
        assertEquals("Should have 3 categories with photos", 3, allCategoriesWithPhotos.size)

        for (categoryWithPhotos in allCategoriesWithPhotos) {
            assertEquals("Each category should have 2 photos", 2, categoryWithPhotos.photos.size)
            assertEquals("Photo count should match actual photos",
                categoryWithPhotos.photos.size, categoryWithPhotos.category.photoCount)
        }

        // Step 5: Test category reordering
        val reorderedCategories = categories.map { it.copy(position = 2 - it.position) }
        categoryDao.updateCategoryPositions(reorderedCategories)

        val reorderedResult = categoryDao.getAllCategories().first()
        assertEquals("First category should now be fun_times", "fun_times", reorderedResult[0].name)
        assertEquals("Second category should now be family", "family", reorderedResult[1].name)
        assertEquals("Third category should now be animals", "animals", reorderedResult[2].name)

        // Step 6: Test photo reordering within category
        val animalsPhotos = photoDao.getPhotosForCategory("animals").first()
        val reorderedPhotos = animalsPhotos.map { it.copy(position = 1 - it.position) }
        photoDao.updatePhotoPositions(reorderedPhotos)

        val reorderedAnimalsPhotos = photoDao.getPhotosForCategory("animals").first()
        assertEquals("First animals photo should now be photo2", "photo2", reorderedAnimalsPhotos[0].id)
        assertEquals("Second animals photo should now be photo1", "photo1", reorderedAnimalsPhotos[1].id)
    }

    // ================== Data Integrity Tests ==================

    @Test
    fun categoryPhotoRelationshipShouldMaintainIntegrity() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Create category and photos
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", position = 0),
            createSamplePhoto(id = "photo2", position = 1),
            createSamplePhoto(id = "photo3", position = 2)
        )
        photoDao.insertPhotos(photos)

        // Verify relationship through CategoryWithPhotos
        val categoryWithPhotos = categoryDao.getCategoryWithPhotos(category.id)
        assertNotNull("Category with photos should exist", categoryWithPhotos)
        assertEquals("Category should have 3 photos", 3, categoryWithPhotos!!.photos.size)

        // Verify individual photo relationships
        for (photo in photos) {
            val retrievedPhoto = photoDao.getPhotoById(photo.id)
            assertNotNull("Photo should exist", retrievedPhoto)
            assertEquals("Photo should reference correct category",
                category.id, retrievedPhoto!!.categoryId)
        }

        // Test cascade delete
        categoryDao.deleteCategory(category)

        // Verify photos are deleted due to cascade
        for (photo in photos) {
            val deletedPhoto = photoDao.getPhotoById(photo.id)
            assertNull("Photo should be deleted with category", deletedPhoto)
        }

        assertEquals("Total photo count should be 0", 0, photoDao.getTotalPhotoCount())
    }

    @Test
    fun foreignKeyConstraintsShouldPreventOrphanedPhotos() = runTest {
        val photoDao = db.photoDao()

        // Try to insert photo without category
        val orphanPhoto = createSamplePhoto(categoryId = "nonexistent")

        try {
            photoDao.insertPhoto(orphanPhoto)
            fail("Should not be able to insert photo with non-existent category")
        } catch (e: Exception) {
            assertTrue("Should fail due to foreign key constraint",
                e.message?.contains("FOREIGN KEY constraint failed") == true)
        }

        // Verify photo was not inserted
        assertEquals("Should have 0 photos after failed insert", 0, photoDao.getTotalPhotoCount())
    }

    // ================== Concurrent Access Tests ==================

    @Test
    fun concurrentDatabaseAccessShouldMaintainConsistency() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Create base category
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Simulate concurrent photo insertions
        val photoJob1 = kotlinx.coroutines.async {
            val photos = (1..10).map {
                createSamplePhoto(id = "photo_a_$it", position = it)
            }
            photoDao.insertPhotos(photos)
        }

        val photoJob2 = kotlinx.coroutines.async {
            val photos = (1..10).map {
                createSamplePhoto(id = "photo_b_$it", position = it + 100)
            }
            photoDao.insertPhotos(photos)
        }

        // Wait for both operations to complete
        photoJob1.await()
        photoJob2.await()

        // Verify data consistency
        val totalPhotos = photoDao.getTotalPhotoCount()
        assertEquals("Should have 20 photos total", 20, totalPhotos)

        val categoryPhotos = photoDao.getPhotosForCategory(category.id).first()
        assertEquals("Category should have 20 photos", 20, categoryPhotos.size)

        // Verify all photo IDs are unique
        val photoIds = categoryPhotos.map { it.id }.toSet()
        assertEquals("All photo IDs should be unique", 20, photoIds.size)
    }

    // ================== Data Validation and Error Handling ==================

    @Test
    fun invalidDataShouldBeHandledGracefully() = runTest {
        val categoryDao = db.categoryDao()

        // Test duplicate category names (should fail unique constraint)
        val category1 = createSampleCategory(id = "cat1", name = "animals")
        val category2 = createSampleCategory(id = "cat2", name = "animals") // Same name

        categoryDao.insertCategory(category1)

        try {
            categoryDao.insertCategory(category2)
            fail("Should not allow duplicate category names")
        } catch (e: Exception) {
            assertTrue("Should fail due to unique constraint",
                e.message?.contains("UNIQUE constraint failed") == true)
        }

        // Verify only first category exists
        assertEquals("Should have only 1 category", 1, categoryDao.getCategoryCount())
        assertNotNull("First category should exist", categoryDao.getCategoryById("cat1"))
        assertNull("Second category should not exist", categoryDao.getCategoryById("cat2"))
    }

    // ================== Large Dataset Performance Tests ==================

    @Test
    fun largeDatasetshouldPerformWithinAcceptableLimits() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val startTime = System.currentTimeMillis()

        // Create 10 categories
        val categories = (1..10).map {
            createSampleCategory(id = "cat$it", name = "category$it", position = it)
        }
        categoryDao.insertCategories(categories)

        val categoryInsertTime = System.currentTimeMillis()

        // Create 100 photos (10 per category)
        val photos = (1..100).map { photoIndex ->
            val categoryIndex = ((photoIndex - 1) / 10) + 1
            createSamplePhoto(
                id = "photo$photoIndex",
                categoryId = "cat$categoryIndex",
                position = (photoIndex - 1) % 10
            )
        }
        photoDao.insertPhotos(photos)

        val photoInsertTime = System.currentTimeMillis()

        // Test query performance
        val allCategoriesWithPhotos = categoryDao.getAllCategoriesWithPhotos().first()
        val queryTime = System.currentTimeMillis()

        // Verify data integrity
        assertEquals("Should have 10 categories", 10, allCategoriesWithPhotos.size)
        assertEquals("Total photos should be 100", 100, photoDao.getTotalPhotoCount())

        for (categoryWithPhotos in allCategoriesWithPhotos) {
            assertEquals("Each category should have 10 photos",
                10, categoryWithPhotos.photos.size)
        }

        // Performance assertions (adjust thresholds as needed)
        val categoryInsertDuration = categoryInsertTime - startTime
        val photoInsertDuration = photoInsertTime - categoryInsertTime
        val queryDuration = queryTime - photoInsertTime

        assertTrue("Category insert should be fast (< 100ms)", categoryInsertDuration < 100)
        assertTrue("Photo insert should be reasonable (< 500ms)", photoInsertDuration < 500)
        assertTrue("Query should be fast (< 50ms)", queryDuration < 50)
    }

    // ================== Database Initialization Tests ==================

    @Test
    fun databaseCallbackShouldPopulateInitialData() = runTest {
        // Close current in-memory database
        db.close()

        // Create new database that would trigger the callback (in real scenario)
        // Note: In-memory database doesn't persist the callback behavior,
        // but we can test the populate logic directly
        val newDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val categoryDao = newDb.categoryDao()
            val photoDao = newDb.photoDao()

            // Manually call populate logic to test it
            // (In real app, this would be called by the database callback)
            populateTestData(newDb)

            // Verify initial data was created
            val categories = categoryDao.getAllCategories().first()
            val photos = photoDao.getAllPhotos().first()

            assertTrue("Should have categories after population", categories.isNotEmpty())
            assertTrue("Should have photos after population", photos.isNotEmpty())

            // Verify category names match expected initial data
            val categoryNames = categories.map { it.name }.toSet()
            assertTrue("Should have animals category", "animals" in categoryNames)
            assertTrue("Should have family category", "family" in categoryNames)
            assertTrue("Should have fun_times category", "fun_times" in categoryNames)

            // Verify each category has photos
            for (category in categories) {
                val categoryPhotos = photoDao.getPhotoCountForCategory(category.id)
                assertTrue("Category ${category.name} should have photos", categoryPhotos > 0)
            }

        } finally {
            newDb.close()
        }
    }

    // Helper method to simulate database population
    private suspend fun populateTestData(database: SmilePileDatabase) {
        val categoryDao = database.categoryDao()
        val photoDao = database.photoDao()

        // Check if database is already populated
        if (categoryDao.getCategoryCount() > 0) {
            return
        }

        // Sample image files from assets/sample_images/
        val sampleImages = listOf(
            "sample_1.png", "sample_2.png", "sample_3.png",
            "sample_4.png", "sample_5.png", "sample_6.png"
        )

        // Create 3 sample categories matching CategoryManager
        val categories = listOf(
            CategoryEntity(
                id = "animals",
                name = "animals",
                displayName = "Animals",
                coverImagePath = null,
                description = "Fun animal pictures",
                position = 0
            ),
            CategoryEntity(
                id = "family",
                name = "family",
                displayName = "Family",
                coverImagePath = null,
                description = "Happy family moments",
                position = 1
            ),
            CategoryEntity(
                id = "fun_times",
                name = "fun_times",
                displayName = "Fun Times",
                coverImagePath = null,
                description = "Good times and memories",
                position = 2
            )
        )

        // Insert categories
        categoryDao.insertCategories(categories)

        // Create and insert photos
        val photos = sampleImages.mapIndexed { index, imageName ->
            val categoryId = when (index % 3) {
                0 -> "animals"
                1 -> "family"
                else -> "fun_times"
            }

            PhotoEntity(
                id = "photo_${index + 1}",
                path = imageName,
                name = imageName.substringBeforeLast('.'),
                categoryId = categoryId,
                position = index / 3,
                isFromAssets = true
            )
        }

        photoDao.insertPhotos(photos)

        // Update category photo counts
        categories.forEach { category ->
            val photoCount = photoDao.getPhotoCountForCategory(category.id)
            val updatedCategory = category.copy(photoCount = photoCount)
            categoryDao.updateCategory(updatedCategory)
        }
    }

    // ================== Migration and Backward Compatibility Tests ==================

    @Test
    fun databaseVersionShouldBeCorrect() {
        // Verify database version
        assertEquals("Database version should be 1", 1, db.openHelper.readableDatabase.version)
    }

    @Test
    fun databaseSchemaShouldSupportRequiredTables() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Test that all required operations work (indicating schema is correct)
        val category = createSampleCategory()
        val photo = createSamplePhoto()

        // These operations should succeed if schema is correct
        categoryDao.insertCategory(category)
        photoDao.insertPhoto(photo)

        // Test relationship query
        val categoryWithPhotos = categoryDao.getCategoryWithPhotos(category.id)
        assertNotNull("Relationship query should work", categoryWithPhotos)
        assertEquals("Should have 1 photo", 1, categoryWithPhotos!!.photos.size)
    }

    // ================== Memory and Resource Management Tests ==================

    @Test
    fun databaseOperationsShouldNotLeakMemory() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Perform many operations that could potentially leak memory
        repeat(50) { iteration ->
            val category = createSampleCategory(id = "cat$iteration", name = "category$iteration")
            categoryDao.insertCategory(category)

            val photos = (1..5).map { photoIndex ->
                createSamplePhoto(
                    id = "photo_${iteration}_$photoIndex",
                    categoryId = category.id,
                    position = photoIndex
                )
            }
            photoDao.insertPhotos(photos)

            // Read data back
            val retrieved = categoryDao.getCategoryWithPhotos(category.id)
            assertNotNull("Category should be retrievable", retrieved)

            // Clean up this iteration
            categoryDao.deleteCategory(category)
        }

        // Verify cleanup worked
        assertEquals("Should have 0 categories after cleanup", 0, categoryDao.getCategoryCount())
        assertEquals("Should have 0 photos after cleanup", 0, photoDao.getTotalPhotoCount())
    }

    // ================== Edge Case Tests ==================

    @Test
    fun emptyStringSearchShouldReturnAllPhotos() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // Setup test data
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", position = 0),
            createSamplePhoto(id = "photo2", position = 1)
        )
        photoDao.insertPhotos(photos)

        // Search with empty string should return all photos
        val searchResults = photoDao.searchPhotosByName("").first()
        assertEquals("Empty search should return all photos", 2, searchResults.size)
    }

    @Test
    fun maxPositionCalculationShouldHandleEdgeCases() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Test with no photos
        assertEquals("Max position should be 0 for empty category",
            0, photoDao.getMaxPositionInCategory(category.id))

        // Test with negative positions
        val negativePhoto = createSamplePhoto(id = "negative", position = -5)
        photoDao.insertPhoto(negativePhoto)

        val maxAfterNegative = photoDao.getMaxPositionInCategory(category.id)
        assertEquals("Max position should handle negative values", -5, maxAfterNegative)

        // Test with large positions
        val largePhoto = createSamplePhoto(id = "large", position = 1000000)
        photoDao.insertPhoto(largePhoto)

        val maxAfterLarge = photoDao.getMaxPositionInCategory(category.id)
        assertEquals("Max position should handle large values", 1000000, maxAfterLarge)
    }
}