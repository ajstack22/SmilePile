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
 * Comprehensive tests for CategoryDao to verify Room database persistence
 * and backward compatibility for the SmilePile app.
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var categoryDao: CategoryDao
    private lateinit var photoDao: PhotoDao
    private lateinit var db: SmilePileDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        categoryDao = db.categoryDao()
        photoDao = db.photoDao()
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
        displayName: String = "Animals",
        position: Int = 0
    ) = CategoryEntity(
        id = id,
        name = name,
        displayName = displayName,
        coverImagePath = "animals/cat1.jpg",
        description = "Cute animals for kids",
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
        path = "animals/cat1.jpg",
        name = "Cute Cat",
        categoryId = categoryId,
        position = position,
        dateAdded = System.currentTimeMillis(),
        isFromAssets = true
    )

    // ================== CRUD Operation Tests ==================

    @Test
    fun insertAndRetrieveCategoryShouldPersistData() = runTest {
        val category = createSampleCategory()

        // Insert category
        val insertId = categoryDao.insertCategory(category)
        assertTrue("Insert should return positive ID", insertId > 0)

        // Retrieve category
        val retrieved = categoryDao.getCategoryById(category.id)
        assertNotNull("Category should be retrievable after insert", retrieved)
        assertEquals("Category ID should match", category.id, retrieved!!.id)
        assertEquals("Category name should match", category.name, retrieved.name)
        assertEquals("Category display name should match", category.displayName, retrieved.displayName)
        assertEquals("Category position should match", category.position, retrieved.position)
    }

    @Test
    fun insertCategoryWithDuplicateIdShouldReplace() = runTest {
        val originalCategory = createSampleCategory(id = "test", name = "original")
        val updatedCategory = createSampleCategory(id = "test", name = "updated")

        // Insert original
        categoryDao.insertCategory(originalCategory)

        // Insert with same ID (should replace due to REPLACE strategy)
        categoryDao.insertCategory(updatedCategory)

        // Verify replacement
        val retrieved = categoryDao.getCategoryById("test")
        assertNotNull("Category should exist", retrieved)
        assertEquals("Category name should be updated", "updated", retrieved!!.name)
    }

    @Test
    fun updateCategoryShouldModifyExistingData() = runTest {
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Update the category
        val updatedCategory = category.copy(
            displayName = "Updated Animals",
            description = "Updated description",
            photoCount = 5
        )

        val updateCount = categoryDao.updateCategory(updatedCategory)
        assertEquals("Update should affect one row", 1, updateCount)

        // Verify update
        val retrieved = categoryDao.getCategoryById(category.id)
        assertNotNull("Updated category should exist", retrieved)
        assertEquals("Display name should be updated", "Updated Animals", retrieved!!.displayName)
        assertEquals("Description should be updated", "Updated description", retrieved.description)
        assertEquals("Photo count should be updated", 5, retrieved.photoCount)
    }

    @Test
    fun deleteCategoryShouldRemoveData() = runTest {
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Verify insertion
        assertNotNull("Category should exist before delete", categoryDao.getCategoryById(category.id))

        // Delete category
        val deleteCount = categoryDao.deleteCategory(category)
        assertEquals("Delete should affect one row", 1, deleteCount)

        // Verify deletion
        assertNull("Category should not exist after delete", categoryDao.getCategoryById(category.id))
    }

    // ================== Batch Operation Tests ==================

    @Test
    fun insertMultipleCategoriesShouldPersistAll() = runTest {
        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals", position = 0),
            createSampleCategory(id = "cat2", name = "vehicles", position = 1),
            createSampleCategory(id = "cat3", name = "food", position = 2)
        )

        // Batch insert
        val insertIds = categoryDao.insertCategories(categories)
        assertEquals("Should return ID for each inserted category", 3, insertIds.size)
        assertTrue("All insert IDs should be positive", insertIds.all { it > 0 })

        // Verify all categories exist
        for (category in categories) {
            val retrieved = categoryDao.getCategoryById(category.id)
            assertNotNull("Category ${category.id} should exist", retrieved)
            assertEquals("Category name should match", category.name, retrieved!!.name)
        }
    }

    @Test
    fun getAllCategoriesShouldReturnOrderedByPosition() = runTest {
        val categories = listOf(
            createSampleCategory(id = "cat3", name = "food", position = 2),
            createSampleCategory(id = "cat1", name = "animals", position = 0),
            createSampleCategory(id = "cat2", name = "vehicles", position = 1)
        )

        categoryDao.insertCategories(categories)

        // Get all categories via Flow
        val retrievedCategories = categoryDao.getAllCategories().first()

        assertEquals("Should retrieve all categories", 3, retrievedCategories.size)

        // Verify ordering by position
        assertEquals("First category should be animals (position 0)", "animals", retrievedCategories[0].name)
        assertEquals("Second category should be vehicles (position 1)", "vehicles", retrievedCategories[1].name)
        assertEquals("Third category should be food (position 2)", "food", retrievedCategories[2].name)
    }

    // ================== Relationship Tests ==================

    @Test
    fun getCategoryWithPhotosShouldIncludeRelatedPhotos() = runTest {
        // Insert category
        val category = createSampleCategory()
        categoryDao.insertCategory(category)

        // Insert photos for the category
        val photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = category.id, position = 0),
            createSamplePhoto(id = "photo2", categoryId = category.id, position = 1)
        )
        photoDao.insertPhotos(photos)

        // Get category with photos
        val categoryWithPhotos = categoryDao.getCategoryWithPhotos(category.id)

        assertNotNull("Category with photos should exist", categoryWithPhotos)
        assertEquals("Category ID should match", category.id, categoryWithPhotos!!.category.id)
        assertEquals("Should have 2 photos", 2, categoryWithPhotos.photos.size)

        // Verify photo ordering
        assertEquals("First photo should be photo1", "photo1", categoryWithPhotos.photos[0].id)
        assertEquals("Second photo should be photo2", "photo2", categoryWithPhotos.photos[1].id)
    }

    @Test
    fun getAllCategoriesWithPhotosShouldIncludeAllRelationships() = runTest {
        // Insert categories
        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals", position = 0),
            createSampleCategory(id = "cat2", name = "vehicles", position = 1)
        )
        categoryDao.insertCategories(categories)

        // Insert photos for both categories
        val photos = listOf(
            createSamplePhoto(id = "photo1", categoryId = "cat1", position = 0),
            createSamplePhoto(id = "photo2", categoryId = "cat1", position = 1),
            createSamplePhoto(id = "photo3", categoryId = "cat2", position = 0)
        )
        photoDao.insertPhotos(photos)

        // Get all categories with photos
        val categoriesWithPhotos = categoryDao.getAllCategoriesWithPhotos().first()

        assertEquals("Should have 2 categories", 2, categoriesWithPhotos.size)

        // Verify first category (animals)
        val animalsCategory = categoriesWithPhotos.find { it.category.name == "animals" }
        assertNotNull("Animals category should exist", animalsCategory)
        assertEquals("Animals should have 2 photos", 2, animalsCategory!!.photos.size)

        // Verify second category (vehicles)
        val vehiclesCategory = categoriesWithPhotos.find { it.category.name == "vehicles" }
        assertNotNull("Vehicles category should exist", vehiclesCategory)
        assertEquals("Vehicles should have 1 photo", 1, vehiclesCategory!!.photos.size)
    }

    // ================== Constraint and Validation Tests ==================

    @Test
    fun insertCategoryWithDuplicateNameShouldFailUniqueConstraint() = runTest {
        val category1 = createSampleCategory(id = "cat1", name = "animals")
        val category2 = createSampleCategory(id = "cat2", name = "animals") // Same name

        // Insert first category
        categoryDao.insertCategory(category1)

        // Try to insert second category with same name - should fail due to unique constraint
        try {
            categoryDao.insertCategory(category2)
            fail("Should have thrown an exception due to unique name constraint")
        } catch (e: Exception) {
            // Expected - unique constraint violation
            assertTrue("Exception should be about constraint violation",
                e.message?.contains("UNIQUE constraint failed") == true)
        }
    }

    @Test
    fun categoryExistsByNameShouldReturnCorrectStatus() = runTest {
        val category = createSampleCategory(name = "animals")

        // Check non-existent category
        assertFalse("Non-existent category should return false",
            categoryDao.categoryExistsByName("animals"))

        // Insert category
        categoryDao.insertCategory(category)

        // Check existing category
        assertTrue("Existing category should return true",
            categoryDao.categoryExistsByName("animals"))
    }

    // ================== Utility Function Tests ==================

    @Test
    fun getCategoryCountShouldReturnCorrectNumber() = runTest {
        // Initial count should be 0
        assertEquals("Initial count should be 0", 0, categoryDao.getCategoryCount())

        // Insert categories
        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals"),
            createSampleCategory(id = "cat2", name = "vehicles"),
            createSampleCategory(id = "cat3", name = "food")
        )
        categoryDao.insertCategories(categories)

        // Count should be 3
        assertEquals("Count should be 3 after inserting 3 categories", 3, categoryDao.getCategoryCount())

        // Delete one category
        categoryDao.deleteCategory(categories[0])

        // Count should be 2
        assertEquals("Count should be 2 after deleting 1 category", 2, categoryDao.getCategoryCount())
    }

    @Test
    fun getMaxPositionShouldReturnCorrectValue() = runTest {
        // Initial max position should be 0
        assertEquals("Initial max position should be 0", 0, categoryDao.getMaxPosition())

        // Insert categories with different positions
        val categories = listOf(
            createSampleCategory(id = "cat1", position = 5),
            createSampleCategory(id = "cat2", position = 2),
            createSampleCategory(id = "cat3", position = 8)
        )
        categoryDao.insertCategories(categories)

        // Max position should be 8
        assertEquals("Max position should be 8", 8, categoryDao.getMaxPosition())
    }

    @Test
    fun deleteAllCategoriesShouldClearTable() = runTest {
        // Insert some categories
        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals"),
            createSampleCategory(id = "cat2", name = "vehicles"),
            createSampleCategory(id = "cat3", name = "food")
        )
        categoryDao.insertCategories(categories)

        // Verify categories exist
        assertEquals("Should have 3 categories", 3, categoryDao.getCategoryCount())

        // Delete all categories
        val deleteCount = categoryDao.deleteAllCategories()
        assertEquals("Should delete 3 categories", 3, deleteCount)

        // Verify table is empty
        assertEquals("Should have 0 categories after delete all", 0, categoryDao.getCategoryCount())
        val allCategories = categoryDao.getAllCategories().first()
        assertTrue("Categories list should be empty", allCategories.isEmpty())
    }

    // ================== Position Management Tests ==================

    @Test
    fun updateCategoryPositionsShouldReorderCategories() = runTest {
        // Insert categories with initial positions
        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals", position = 0),
            createSampleCategory(id = "cat2", name = "vehicles", position = 1),
            createSampleCategory(id = "cat3", name = "food", position = 2)
        )
        categoryDao.insertCategories(categories)

        // Update positions (reverse order)
        val updatedCategories = listOf(
            categories[0].copy(position = 2),
            categories[1].copy(position = 1),
            categories[2].copy(position = 0)
        )

        val updateIds = categoryDao.updateCategoryPositions(updatedCategories)
        assertEquals("Should update 3 categories", 3, updateIds.size)

        // Verify new ordering
        val reorderedCategories = categoryDao.getAllCategories().first()
        assertEquals("First category should now be food", "food", reorderedCategories[0].name)
        assertEquals("Second category should still be vehicles", "vehicles", reorderedCategories[1].name)
        assertEquals("Third category should now be animals", "animals", reorderedCategories[2].name)
    }

    // ================== Data Validation Tests ==================

    @Test
    fun categoryEntityValidationShouldWork() {
        // Valid category
        val validCategory = createSampleCategory()
        assertTrue("Valid category should pass validation", validCategory.isValid())

        // Invalid categories
        val invalidId = validCategory.copy(id = "")
        assertFalse("Category with empty ID should fail validation", invalidId.isValid())

        val invalidName = validCategory.copy(name = "")
        assertFalse("Category with empty name should fail validation", invalidName.isValid())

        val invalidDisplayName = validCategory.copy(displayName = "")
        assertFalse("Category with empty display name should fail validation", invalidDisplayName.isValid())
    }

    // ================== Domain Model Conversion Tests ==================

    @Test
    fun categoryEntityToDomainModelConversionShouldPreserveData() {
        val entity = createSampleCategory()
        val domainModel = entity.toDomainModel()

        assertEquals("ID should match", entity.id, domainModel.id)
        assertEquals("Name should match", entity.name, domainModel.name)
        assertEquals("Display name should match", entity.displayName, domainModel.displayName)
        assertEquals("Cover image path should match", entity.coverImagePath, domainModel.coverImagePath)
        assertEquals("Description should match", entity.description, domainModel.description)
        assertEquals("Photo count should match", entity.photoCount, domainModel.photoCount)
        assertEquals("Position should match", entity.position, domainModel.position)
        assertEquals("Created at should match", entity.createdAt, domainModel.createdAt)
    }

    @Test
    fun categoryEntityFromDomainModelConversionShouldPreserveData() {
        val entity = createSampleCategory()
        val domainModel = entity.toDomainModel()
        val convertedEntity = CategoryEntity.fromDomainModel(domainModel)

        assertEquals("Converted entity should match original", entity, convertedEntity)
    }
}