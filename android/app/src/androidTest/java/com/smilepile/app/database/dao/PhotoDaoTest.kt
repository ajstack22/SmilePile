package com.smilepile.app.database.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.database.SmilePileDatabase
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
 * Comprehensive tests for PhotoDao to verify Room database persistence,
 * relationship handling, and constraints for the SmilePile app.
 */
@RunWith(AndroidJUnit4::class)
class PhotoDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var db: SmilePileDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        photoDao = db.photoDao()
        categoryDao = db.categoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Sample test data
    private fun createSampleCategory(
        id: String = "cat1",
        name: String = "animals"
    ) = CategoryEntity(
        id = id,
        name = name,
        displayName = "Animals",
        coverImagePath = null,
        description = "Animal photos",
        photoCount = 0,
        position = 0,
        createdAt = System.currentTimeMillis()
    )

    private fun createSamplePhoto(
        id: String = "photo1",
        path: String = "animals/cat1.jpg",
        name: String = "Cute Cat",
        categoryId: String? = "cat1",
        position: Int = 0,
        isFromAssets: Boolean = true
    ) = PhotoEntity(
        id = id,
        path = path,
        name = name,
        categoryId = categoryId,
        position = position,
        dateAdded = System.currentTimeMillis(),
        isFromAssets = isFromAssets,
        isDeleted = false,
        deletedAt = null
    )

    private suspend fun setupCategoryAndPhotos(): Pair<CategoryEntity, List<PhotoEntity>> {
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", position = 0),
            createSamplePhoto(id = "photo2", path = "animals/dog1.jpg", name = "Happy Dog", position = 1),
            createSamplePhoto(id = "photo3", path = "animals/bird1.jpg", name = "Flying Bird", position = 2)
        )
        photoDao.insertPhotos(photos)

        return category to photos
    }

    // ================== CRUD Operation Tests ==================

    @Test
    fun insertAndRetrievePhotoShouldPersistData() = runTest {
        // Setup category first (required for foreign key)
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photo = createSamplePhoto()

        // Insert photo
        val insertId = photoDao.insertPhoto(photo)
        assertTrue("Insert should return positive ID", insertId > 0)

        // Retrieve photo
        val retrieved = photoDao.getPhotoById(photo.id)
        assertNotNull("Photo should be retrievable after insert", retrieved)
        assertEquals("Photo ID should match", photo.id, retrieved!!.id)
        assertEquals("Photo path should match", photo.path, retrieved.path)
        assertEquals("Photo name should match", photo.name, retrieved.name)
        assertEquals("Photo category ID should match", photo.categoryId, retrieved.categoryId)
        assertEquals("Photo position should match", photo.position, retrieved.position)
        assertEquals("Photo asset status should match", photo.isFromAssets, retrieved.isFromAssets)
    }

    @Test
    fun insertPhotoWithDuplicateIdShouldReplace() = runTest {
        // Setup category
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val originalPhoto = createSamplePhoto(id = "test", name = "Original")
        val updatedPhoto = createSamplePhoto(id = "test", name = "Updated")

        // Insert original
        photoDao.insertPhoto(originalPhoto)

        // Insert with same ID (should replace due to REPLACE strategy)
        photoDao.insertPhoto(updatedPhoto)

        // Verify replacement
        val retrieved = photoDao.getPhotoById("test")
        assertNotNull("Photo should exist", retrieved)
        assertEquals("Photo name should be updated", "Updated", retrieved!!.name)
    }

    @Test
    fun updatePhotoShouldModifyExistingData() = runTest {
        val (category, photos) = setupCategoryAndPhotos()
        val photo = photos[0]

        // Update the photo
        val updatedPhoto = photo.copy(
            name = "Updated Cat Photo",
            path = "animals/updated_cat.jpg",
            position = 5,
            isFromAssets = false
        )

        val updateCount = photoDao.updatePhoto(updatedPhoto)
        assertEquals("Update should affect one row", 1, updateCount)

        // Verify update
        val retrieved = photoDao.getPhotoById(photo.id)
        assertNotNull("Updated photo should exist", retrieved)
        assertEquals("Name should be updated", "Updated Cat Photo", retrieved!!.name)
        assertEquals("Path should be updated", "animals/updated_cat.jpg", retrieved.path)
        assertEquals("Position should be updated", 5, retrieved.position)
        assertEquals("Asset status should be updated", false, retrieved.isFromAssets)
    }

    @Test
    fun deletePhotoShouldRemoveData() = runTest {
        val (category, photos) = setupCategoryAndPhotos()
        val photo = photos[0]

        // Verify insertion
        assertNotNull("Photo should exist before delete", photoDao.getPhotoById(photo.id))

        // Delete photo
        val deleteCount = photoDao.deletePhoto(photo)
        assertEquals("Delete should affect one row", 1, deleteCount)

        // Verify deletion
        assertNull("Photo should not exist after delete", photoDao.getPhotoById(photo.id))
    }

    // ================== Foreign Key Constraint Tests ==================

    @Test
    fun insertPhotoWithInvalidCategoryIdShouldFail() = runTest {
        val photo = createSamplePhoto(categoryId = "nonexistent")

        // Try to insert photo with non-existent category ID
        try {
            photoDao.insertPhoto(photo)
            fail("Should have thrown an exception due to foreign key constraint")
        } catch (e: Exception) {
            // Expected - foreign key constraint violation
            assertTrue("Exception should be about foreign key constraint",
                e.message?.contains("FOREIGN KEY constraint failed") == true)
        }
    }

    @Test
    fun deleteCategoryShouldSetPhotoCategoryIdToNull() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Verify photos exist with category ID
        assertEquals("Should have 3 photos initially", 3, photoDao.getTotalPhotoCount())
        for (photo in photos) {
            val retrieved = photoDao.getPhotoById(photo.id)
            assertNotNull("Photo ${photo.id} should exist", retrieved)
            assertEquals("Photo should have category ID", category.id, retrieved!!.categoryId)
        }

        // Delete category (should set photo categoryId to null, not delete photos)
        categoryDao.deleteCategory(category)

        // Verify photos still exist but with null categoryId
        assertEquals("Should still have 3 photos after category deletion", 3, photoDao.getTotalPhotoCount())

        // Verify specific photos exist but are orphaned
        for (photo in photos) {
            val retrieved = photoDao.getPhotoById(photo.id)
            assertNotNull("Photo ${photo.id} should still exist", retrieved)
            assertNull("Photo ${photo.id} should have null categoryId", retrieved!!.categoryId)
        }

        // Verify photos no longer appear in category-specific queries
        val photosForDeletedCategory = photoDao.getPhotosForCategory(category.id).first()
        assertTrue("Should have no photos for deleted category", photosForDeletedCategory.isEmpty())
    }

    // ================== Batch Operation Tests ==================

    @Test
    fun insertMultiplePhotosShouldPersistAll() = runTest {
        // Setup category
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", name = "Cat", position = 0),
            createSamplePhoto(id = "photo2", name = "Dog", position = 1),
            createSamplePhoto(id = "photo3", name = "Bird", position = 2)
        )

        // Batch insert
        val insertIds = photoDao.insertPhotos(photos)
        assertEquals("Should return ID for each inserted photo", 3, insertIds.size)
        assertTrue("All insert IDs should be positive", insertIds.all { it > 0 })

        // Verify all photos exist
        for (photo in photos) {
            val retrieved = photoDao.getPhotoById(photo.id)
            assertNotNull("Photo ${photo.id} should exist", retrieved)
            assertEquals("Photo name should match", photo.name, retrieved!!.name)
        }
    }

    @Test
    fun getPhotosForCategoryShouldReturnOrderedByPosition() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Get photos for category via Flow
        val retrievedPhotos = photoDao.getPhotosForCategory(category.id).first()

        assertEquals("Should retrieve all photos for category", 3, retrievedPhotos.size)

        // Verify ordering by position
        assertEquals("First photo should be photo1 (position 0)", "photo1", retrievedPhotos[0].id)
        assertEquals("Second photo should be photo2 (position 1)", "photo2", retrievedPhotos[1].id)
        assertEquals("Third photo should be photo3 (position 2)", "photo3", retrievedPhotos[2].id)
    }

    @Test
    fun getAllPhotosShouldReturnOrderedByCategoryAndPosition() = runTest {
        // Setup multiple categories with photos
        val category1 = createSampleCategory(id = "cat1", name = "animals")
        val category2 = createSampleCategory(id = "cat2", name = "vehicles")
        categoryDao.insertCategories(listOf(category1, category2))

        val photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = "cat2", position = 0), // vehicles first
            createSamplePhoto(id = "photo2", categoryId = "cat1", position = 1), // animals second
            createSamplePhoto(id = "photo3", categoryId = "cat1", position = 0), // animals first
            createSamplePhoto(id = "photo4", categoryId = "cat2", position = 1)  // vehicles second
        )
        photoDao.insertPhotos(photos)

        // Get all photos via Flow
        val retrievedPhotos = photoDao.getAllPhotos().first()

        assertEquals("Should retrieve all photos", 4, retrievedPhotos.size)

        // Verify ordering: cat1 photos first (ordered by position), then cat2 photos
        assertEquals("First photo should be cat1/position 0", "photo3", retrievedPhotos[0].id)
        assertEquals("Second photo should be cat1/position 1", "photo2", retrievedPhotos[1].id)
        assertEquals("Third photo should be cat2/position 0", "photo1", retrievedPhotos[2].id)
        assertEquals("Fourth photo should be cat2/position 1", "photo4", retrievedPhotos[3].id)
    }

    // ================== Category-Specific Operations ==================

    @Test
    fun deletePhotosForCategoryShouldRemoveOnlySpecifiedCategoryPhotos() = runTest {
        // Setup two categories with photos
        val category1 = createSampleCategory(id = "cat1", name = "animals")
        val category2 = createSampleCategory(id = "cat2", name = "vehicles")
        categoryDao.insertCategories(listOf(category1, category2))

        val cat1Photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = "cat1"),
            createSamplePhoto(id = "photo2", categoryId = "cat1")
        )
        val cat2Photos = listOf(
            createSamplePhoto(id = "photo3", categoryId = "cat2"),
            createSamplePhoto(id = "photo4", categoryId = "cat2")
        )
        photoDao.insertPhotos(cat1Photos + cat2Photos)

        // Delete photos for category1 only
        val deleteCount = photoDao.deletePhotosForCategory("cat1")
        assertEquals("Should delete 2 photos for category1", 2, deleteCount)

        // Verify category1 photos are gone
        val cat1PhotosAfter = photoDao.getPhotosForCategory("cat1").first()
        assertTrue("Category1 should have no photos", cat1PhotosAfter.isEmpty())

        // Verify category2 photos still exist
        val cat2PhotosAfter = photoDao.getPhotosForCategory("cat2").first()
        assertEquals("Category2 should still have 2 photos", 2, cat2PhotosAfter.size)
    }

    @Test
    fun getPhotoCountForCategoryShouldReturnCorrectNumber() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        val photoCount = photoDao.getPhotoCountForCategory(category.id)
        assertEquals("Should count 3 photos for category", 3, photoCount)

        // Delete one photo
        photoDao.deletePhoto(photos[0])

        val photoCountAfterDelete = photoDao.getPhotoCountForCategory(category.id)
        assertEquals("Should count 2 photos after deletion", 2, photoCountAfterDelete)
    }

    @Test
    fun getTotalPhotoCountShouldCountAllPhotos() = runTest {
        // Setup multiple categories with photos
        val category1 = createSampleCategory(id = "cat1", name = "animals")
        val category2 = createSampleCategory(id = "cat2", name = "vehicles")
        categoryDao.insertCategories(listOf(category1, category2))

        val photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = "cat1"),
            createSamplePhoto(id = "photo2", categoryId = "cat1"),
            createSamplePhoto(id = "photo3", categoryId = "cat2")
        )
        photoDao.insertPhotos(photos)

        val totalCount = photoDao.getTotalPhotoCount()
        assertEquals("Should count all 3 photos", 3, totalCount)
    }

    // ================== Validation and Utility Tests ==================

    @Test
    fun photoExistsByNameShouldReturnCorrectStatus() = runTest {
        val (category, photos) = setupCategoryAndPhotos()
        val photo = photos[0]

        // Check existing photo
        assertTrue("Existing photo should return true",
            photoDao.photoExistsByName(photo.name, photo.categoryId))

        // Check non-existent photo in same category
        assertFalse("Non-existent photo should return false",
            photoDao.photoExistsByName("Non-existent", photo.categoryId))

        // Check same name in different category
        val category2 = createSampleCategory(id = "cat2", name = "vehicles")
        categoryDao.insertCategory(category2)

        assertFalse("Same name in different category should return false",
            photoDao.photoExistsByName(photo.name, category2.id))
    }

    @Test
    fun getMaxPositionInCategoryShouldReturnCorrectValue() = runTest {
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Initial max position should be 0 for empty category
        assertEquals("Initial max position should be 0", 0,
            photoDao.getMaxPositionInCategory(category.id))

        // Insert photos with different positions
        val photos = listOf(
            createSamplePhoto(id = "photo1", position = 3),
            createSamplePhoto(id = "photo2", position = 1),
            createSamplePhoto(id = "photo3", position = 7)
        )
        photoDao.insertPhotos(photos)

        // Max position should be 7
        assertEquals("Max position should be 7", 7,
            photoDao.getMaxPositionInCategory(category.id))
    }

    // ================== Search and Query Tests ==================

    @Test
    fun searchPhotosByNameShouldReturnMatchingPhotos() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Search for "cat" should find the cat photo
        val catResults = photoDao.searchPhotosByName("cat").first()
        assertEquals("Should find 1 photo matching 'cat'", 1, catResults.size)
        assertEquals("Should find the cat photo", "Cute Cat", catResults[0].name)

        // Search for "dog" should find the dog photo
        val dogResults = photoDao.searchPhotosByName("dog").first()
        assertEquals("Should find 1 photo matching 'dog'", 1, dogResults.size)
        assertEquals("Should find the dog photo", "Happy Dog", dogResults[0].name)

        // Search should be case insensitive
        val upperCaseResults = photoDao.searchPhotosByName("CAT").first()
        assertEquals("Case insensitive search should work", 1, upperCaseResults.size)

        // Search for non-existent term
        val noResults = photoDao.searchPhotosByName("elephant").first()
        assertTrue("Non-existent search should return empty", noResults.isEmpty())
    }

    @Test
    fun getRandomPhotosShouldReturnRandomSelection() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Get random photos
        val randomPhotos = photoDao.getRandomPhotos(2)
        assertEquals("Should return requested number of photos", 2, randomPhotos.size)

        // Verify returned photos are from our dataset
        val photoIds = photos.map { it.id }.toSet()
        assertTrue("Random photos should be from our dataset",
            randomPhotos.all { it.id in photoIds })

        // Test limit exceeding available photos
        val moreRandomPhotos = photoDao.getRandomPhotos(10)
        assertEquals("Should return all available photos when limit exceeds count",
            3, moreRandomPhotos.size)
    }

    // ================== Asset Management Tests ==================

    @Test
    fun getPhotosFromAssetsShouldFilterCorrectly() = runTest {
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", isFromAssets = true),
            createSamplePhoto(id = "photo2", isFromAssets = false),
            createSamplePhoto(id = "photo3", isFromAssets = true)
        )
        photoDao.insertPhotos(photos)

        val assetPhotos = photoDao.getPhotosFromAssets().first()
        assertEquals("Should find 2 asset photos", 2, assetPhotos.size)
        assertTrue("All returned photos should be from assets",
            assetPhotos.all { it.isFromAssets })
    }

    @Test
    fun updatePhotoAssetStatusShouldModifyStatus() = runTest {
        val (category, photos) = setupCategoryAndPhotos()
        val photo = photos[0]

        // Verify initial asset status
        assertTrue("Photo should initially be from assets", photo.isFromAssets)

        // Update asset status
        val updateCount = photoDao.updatePhotoAssetStatus(photo.id, false)
        assertEquals("Update should affect one row", 1, updateCount)

        // Verify update
        val updated = photoDao.getPhotoById(photo.id)
        assertNotNull("Photo should still exist", updated)
        assertFalse("Photo should no longer be from assets", updated!!.isFromAssets)
    }

    @Test
    fun updatePhotosAssetStatusShouldModifyMultiplePhotos() = runTest {
        val (category, photos) = setupCategoryAndPhotos()
        val photoIds = photos.take(2).map { it.id }

        // Update multiple photos' asset status
        val updateCount = photoDao.updatePhotosAssetStatus(photoIds, false)
        assertEquals("Update should affect 2 rows", 2, updateCount)

        // Verify updates
        for (photoId in photoIds) {
            val updated = photoDao.getPhotoById(photoId)
            assertNotNull("Photo $photoId should still exist", updated)
            assertFalse("Photo $photoId should no longer be from assets", updated!!.isFromAssets)
        }

        // Verify third photo unchanged
        val unchanged = photoDao.getPhotoById(photos[2].id)
        assertTrue("Third photo should still be from assets", unchanged!!.isFromAssets)
    }

    // ================== Position Management Tests ==================

    @Test
    fun updatePhotoPositionsShouldReorderPhotos() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Update positions (reverse order)
        val updatedPhotos = listOf(
            photos[0].copy(position = 2),
            photos[1].copy(position = 1),
            photos[2].copy(position = 0)
        )

        val updateIds = photoDao.updatePhotoPositions(updatedPhotos)
        assertEquals("Should update 3 photos", 3, updateIds.size)

        // Verify new ordering
        val reorderedPhotos = photoDao.getPhotosForCategory(category.id).first()
        assertEquals("First photo should now be photo3", "photo3", reorderedPhotos[0].id)
        assertEquals("Second photo should still be photo2", "photo2", reorderedPhotos[1].id)
        assertEquals("Third photo should now be photo1", "photo1", reorderedPhotos[2].id)
    }

    // ================== Data Validation Tests ==================

    @Test
    fun photoEntityValidationShouldWork() {
        val category = createSampleCategory()

        // Valid photo
        val validPhoto = createSamplePhoto(categoryId = category.id)
        assertTrue("Valid photo should pass validation", validPhoto.isValid())

        // Invalid photos
        val invalidId = validPhoto.copy(id = "")
        assertFalse("Photo with empty ID should fail validation", invalidId.isValid())

        val invalidPath = validPhoto.copy(path = "")
        assertFalse("Photo with empty path should fail validation", invalidPath.isValid())

        val invalidName = validPhoto.copy(name = "")
        assertFalse("Photo with empty name should fail validation", invalidName.isValid())

        val invalidCategoryId = validPhoto.copy(categoryId = "")
        assertFalse("Photo with empty category ID should fail validation", invalidCategoryId.isValid())

        // Test null categoryId (should be valid for orphaned photos)
        val orphanedPhoto = validPhoto.copy(categoryId = null)
        assertTrue("Photo with null category ID should be valid", orphanedPhoto.isValid())
    }

    // ================== Asset Path Tests ==================

    @Test
    fun getAssetPathShouldFormatCorrectly() {
        // Test asset photo without sample_images prefix
        val assetPhoto = createSamplePhoto(path = "animals/cat1.jpg", isFromAssets = true)
        assertEquals("Asset path should be prefixed with sample_images/",
            "sample_images/animals/cat1.jpg", assetPhoto.getAssetPath())

        // Test asset photo with sample_images prefix already
        val prefixedPhoto = createSamplePhoto(path = "sample_images/animals/cat1.jpg", isFromAssets = true)
        assertEquals("Asset path should not be double-prefixed",
            "sample_images/animals/cat1.jpg", prefixedPhoto.getAssetPath())

        // Test non-asset photo
        val nonAssetPhoto = createSamplePhoto(path = "/storage/photo.jpg", isFromAssets = false)
        assertEquals("Non-asset path should remain unchanged",
            "/storage/photo.jpg", nonAssetPhoto.getAssetPath())
    }

    // ================== Domain Model Conversion Tests ==================

    @Test
    fun photoEntityToDomainModelConversionShouldPreserveData() {
        val entity = createSamplePhoto()
        val domainModel = entity.toDomainModel()

        assertEquals("ID should match", entity.id, domainModel.id)
        assertEquals("Path should match", entity.path, domainModel.path)
        assertEquals("Name should match", entity.name, domainModel.name)
        assertEquals("Category ID should match", entity.categoryId, domainModel.categoryId)
        assertEquals("Position should match", entity.position, domainModel.position)
        assertEquals("Date added should match", entity.dateAdded, domainModel.dateAdded)
        assertEquals("Asset status should match", entity.isFromAssets, domainModel.isFromAssets)
    }

    @Test
    fun photoEntityFromDomainModelConversionShouldPreserveData() {
        val entity = createSamplePhoto()
        val domainModel = entity.toDomainModel()
        val convertedEntity = PhotoEntity.fromDomainModel(domainModel)

        assertEquals("Converted entity should match original", entity, convertedEntity)
    }

    // ================== Cleanup Tests ==================

    @Test
    fun deleteAllPhotosShouldClearTable() = runTest {
        val (category, photos) = setupCategoryAndPhotos()

        // Verify photos exist
        assertEquals("Should have 3 photos", 3, photoDao.getTotalPhotoCount())

        // Delete all photos
        val deleteCount = photoDao.deleteAllPhotos()
        assertEquals("Should delete 3 photos", 3, deleteCount)

        // Verify table is empty
        assertEquals("Should have 0 photos after delete all", 0, photoDao.getTotalPhotoCount())
        val allPhotos = photoDao.getAllPhotos().first()
        assertTrue("Photos list should be empty", allPhotos.isEmpty())
    }
}