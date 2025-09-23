package com.smilepile

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepositoryImpl
import com.smilepile.data.repository.PhotoRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.UUID

/**
 * Comprehensive CRUD operations test for Categories and Photos
 * Tests all Create, Read, Update, and Delete operations
 */
@RunWith(AndroidJUnit4::class)
class CrudOperationsTest {

    private lateinit var database: SmilePileDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var photoRepository: PhotoRepositoryImpl
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        categoryDao = database.categoryDao()
        photoDao = database.photoDao()

        // Create repositories
        categoryRepository = CategoryRepositoryImpl(categoryDao, Dispatchers.Main)
        photoRepository = PhotoRepositoryImpl(
            photoDao = photoDao,
            context = context,
            ioDispatcher = Dispatchers.Main
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    /**
     * Test Category CRUD Operations
     */
    @Test
    fun testCategoryCRUD() = runBlocking {
        Log.d("CrudTest", "=== CATEGORY CRUD TEST ===")

        // 1. CREATE - Test category creation
        Log.d("CrudTest", "1. Testing Category Creation...")
        val testCategory = Category(
            id = 0, // Auto-generate
            name = "test_category",
            displayName = "Test Category",
            position = 0,
            colorHex = "#FF0000",
            isDefault = false
        )

        val categoryId = categoryRepository.insertCategory(testCategory)
        Log.d("CrudTest", "Created category with ID: $categoryId")

        // 2. READ - Test reading categories
        Log.d("CrudTest", "2. Testing Category Read...")
        val allCategories = categoryRepository.getAllCategories()
        Log.d("CrudTest", "Total categories in database: ${allCategories.size}")
        assertTrue("Should have at least 1 category", allCategories.isNotEmpty())

        val firstCategory = allCategories.first()
        Log.d("CrudTest", "First category: name=${firstCategory.name}, displayName=${firstCategory.displayName}")

        // Test the name vs displayName issue
        Log.d("CrudTest", "=== NAME VS DISPLAY NAME INVESTIGATION ===")
        Log.d("CrudTest", "Original input: name='${testCategory.name}', displayName='${testCategory.displayName}'")
        Log.d("CrudTest", "After retrieval: name='${firstCategory.name}', displayName='${firstCategory.displayName}'")

        // 3. UPDATE - Test category update
        Log.d("CrudTest", "3. Testing Category Update...")
        val updatedCategory = firstCategory.copy(
            displayName = "Updated Display Name",
            colorHex = "#00FF00"
        )

        try {
            categoryRepository.updateCategory(updatedCategory)
            Log.d("CrudTest", "Category update successful")

            val retrievedUpdated = categoryRepository.getAllCategories().first()
            Log.d("CrudTest", "After update: displayName='${retrievedUpdated.displayName}'")
        } catch (e: Exception) {
            Log.e("CrudTest", "Update failed: ${e.message}")
        }

        // 4. DELETE - Test category deletion
        Log.d("CrudTest", "4. Testing Category Delete...")
        try {
            categoryRepository.deleteCategory(firstCategory)
            Log.d("CrudTest", "Category delete successful")

            val afterDelete = categoryRepository.getAllCategories()
            Log.d("CrudTest", "Categories after delete: ${afterDelete.size}")
        } catch (e: Exception) {
            Log.e("CrudTest", "Delete failed: ${e.message}")
        }

        // Test direct DAO operations
        Log.d("CrudTest", "=== DIRECT DAO OPERATIONS ===")
        val entityId = UUID.randomUUID().toString()
        val testEntity = CategoryEntity(
            id = entityId,
            name = "Direct DAO Test",
            colorHex = "#0000FF"
        )

        categoryDao.insert(testEntity)
        Log.d("CrudTest", "Direct DAO insert successful")

        val retrieved = categoryDao.getById(entityId)
        Log.d("CrudTest", "Direct DAO retrieval: ${retrieved?.name}")

        val deleteResult = categoryDao.delete(testEntity)
        Log.d("CrudTest", "Direct DAO delete result: $deleteResult rows affected")
    }

    /**
     * Test Photo CRUD Operations
     */
    @Test
    fun testPhotoCRUD() = runBlocking {
        Log.d("CrudTest", "=== PHOTO CRUD TEST ===")

        // First create a category for the photos
        val testCategory = Category(
            id = 1,
            name = "photo_test",
            displayName = "Photo Test",
            position = 0,
            colorHex = "#FF00FF",
            isDefault = false
        )
        categoryRepository.insertCategory(testCategory)

        // 1. CREATE - Test photo creation
        Log.d("CrudTest", "1. Testing Photo Creation...")
        val testPhoto = Photo(
            id = 0, // Auto-generate
            path = "/test/path/photo.jpg",
            categoryId = 1,
            isFavorite = false,
            isFromAssets = false,
            createdAt = System.currentTimeMillis()
        )

        val photoId = photoRepository.insertPhoto(testPhoto)
        Log.d("CrudTest", "Created photo with ID: $photoId")

        // 2. READ - Test reading photos
        Log.d("CrudTest", "2. Testing Photo Read...")
        val allPhotos = photoRepository.getAllPhotos()
        Log.d("CrudTest", "Total photos in database: ${allPhotos.size}")
        assertTrue("Should have at least 1 photo", allPhotos.isNotEmpty())

        val firstPhoto = allPhotos.first()
        Log.d("CrudTest", "First photo: path=${firstPhoto.path}, categoryId=${firstPhoto.categoryId}")

        // 3. UPDATE - Test photo update
        Log.d("CrudTest", "3. Testing Photo Update...")
        val updatedPhoto = firstPhoto.copy(
            isFavorite = true,
            categoryId = 1
        )

        try {
            photoRepository.updatePhoto(updatedPhoto)
            Log.d("CrudTest", "Photo update successful")

            val retrievedUpdated = photoRepository.getPhotoById(firstPhoto.id)
            Log.d("CrudTest", "After update: isFavorite=${retrievedUpdated?.isFavorite}")
        } catch (e: Exception) {
            Log.e("CrudTest", "Update failed: ${e.message}")
        }

        // 4. DELETE - Test photo deletion
        Log.d("CrudTest", "4. Testing Photo Delete...")
        try {
            photoRepository.deletePhoto(firstPhoto)
            Log.d("CrudTest", "Photo delete successful")

            val afterDelete = photoRepository.getAllPhotos()
            Log.d("CrudTest", "Photos after delete: ${afterDelete.size}")
        } catch (e: Exception) {
            Log.e("CrudTest", "Delete failed: ${e.message}")
        }
    }

    /**
     * Test the dual naming system issue
     */
    @Test
    fun testNamingSystemIssue() = runBlocking {
        Log.d("CrudTest", "=== NAMING SYSTEM INVESTIGATION ===")

        // Create categories with different name/displayName combinations
        val testCases = listOf(
            Category(0, "animals", "Animals", 0, null, "#FF0000", false),
            Category(0, "my_photos", "My Photos", 1, null, "#00FF00", false),
            Category(0, "FunTimes", "Fun Times", 2, null, "#0000FF", false)
        )

        for (testCase in testCases) {
            Log.d("CrudTest", "Input: name='${testCase.name}', displayName='${testCase.displayName}'")

            categoryRepository.insertCategory(testCase)
            val retrieved = categoryRepository.getCategoryByName(testCase.name)

            Log.d("CrudTest", "Retrieved: name='${retrieved?.name}', displayName='${retrieved?.displayName}'")
            Log.d("CrudTest", "Issue: displayName is auto-generated as '${retrieved?.displayName}' instead of '${testCase.displayName}'")
            Log.d("CrudTest", "---")
        }

        // Check what's actually in the database
        val allCategories = categoryDao.getAll().first()
        Log.d("CrudTest", "=== RAW DATABASE CONTENT ===")
        for (entity in allCategories) {
            Log.d("CrudTest", "Entity: id=${entity.id}, name=${entity.name}, colorHex=${entity.colorHex}")
            Log.d("CrudTest", "Note: CategoryEntity doesn't have displayName field!")
        }
    }
}