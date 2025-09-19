package com.smilepile.app.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.database.SmilePileDatabase
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Comprehensive tests for CategoryRepository to verify database persistence,
 * fallback mechanisms, backward compatibility, and API integration for the SmilePile app.
 */
@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: CategoryRepository
    private lateinit var testScope: CoroutineScope

    @Before
    fun createRepository() {
        testScope = CoroutineScope(Dispatchers.IO)
        repository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )
    }

    // Sample test data
    private fun createSampleCategory(
        id: String = "test_cat",
        name: String = "test_category",
        displayName: String = "Test Category",
        position: Int = 0
    ) = Category(
        id = id,
        name = name,
        displayName = displayName,
        coverImagePath = null,
        description = "Test category description",
        photoCount = 0,
        position = position,
        createdAt = System.currentTimeMillis()
    )

    private fun createSamplePhoto(
        id: String = "test_photo",
        categoryId: String = "test_cat",
        position: Int = 0
    ) = Photo(
        id = id,
        path = "test_images/sample.png",
        name = "Test Photo",
        categoryId = categoryId,
        position = position,
        dateAdded = System.currentTimeMillis(),
        isFromAssets = true
    )

    // ================== Initialization and Basic Operations ==================

    @Test
    fun repositoryInitializationShouldCreateSampleData() = runTest {
        // When: Initialize sample data
        val initialized = repository.initializeSampleData()

        // Then: Should initialize successfully
        assertTrue("Initialization should succeed", initialized)

        // Verify sample data exists
        val categories = repository.getCategories()
        assertTrue("Should have sample categories", categories.isNotEmpty())

        val photos = repository.getAllPhotos()
        assertTrue("Should have sample photos", photos.isNotEmpty())

        // Verify backward compatibility with original CategoryManager
        assertEquals("Should have 3 categories like CategoryManager", 3, categories.size)
        assertEquals("Should have 6 photos like CategoryManager", 6, photos.size)

        // Verify category names match expected values
        val categoryNames = categories.map { it.displayName }.toSet()
        assertTrue("Should have Animals category", "Animals" in categoryNames)
        assertTrue("Should have Family category", "Family" in categoryNames)
        assertTrue("Should have Fun Times category", "Fun Times" in categoryNames)
    }

    @Test
    fun isInitializedShouldReturnCorrectStatus() = runTest {
        // Initially should not be initialized
        assertFalse("Should not be initialized initially", repository.isInitialized())

        // After initialization should return true
        repository.initializeSampleData()
        assertTrue("Should be initialized after sample data creation", repository.isInitialized())
    }

    // ================== Category Operations with Persistence ==================

    @Test
    fun categoryOperationsShouldPersistAcrossRepositoryInstances() = runTest {
        // Given: Initialize data and add a custom category
        repository.initializeSampleData()
        val newCategory = createSampleCategory(id = "persistent_cat", displayName = "Persistent Category")

        val added = repository.addCategory(newCategory)
        assertTrue("Category should be added", added)

        // When: Create a new repository instance (simulates app restart)
        val newRepository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // Then: Category should still exist
        val retrievedCategory = newRepository.getCategory("persistent_cat")
        assertNotNull("Category should persist across repository instances", retrievedCategory)
        assertEquals("Category display name should match", "Persistent Category", retrievedCategory!!.displayName)

        // Verify in list of all categories
        val allCategories = newRepository.getCategories()
        val persistentCategory = allCategories.find { it.id == "persistent_cat" }
        assertNotNull("Category should be in all categories list", persistentCategory)
    }

    @Test
    fun categoryUpdatesShouldPersist() = runTest {
        // Given: Initialize data
        repository.initializeSampleData()

        // When: Add a category and then update photo count (simulates adding photos)
        val category = createSampleCategory(id = "update_test", photoCount = 0)
        repository.addCategory(category)

        // Verify initial state
        val initial = repository.getCategory("update_test")
        assertEquals("Initial photo count should be 0", 0, initial!!.photoCount)

        // Simulate adding photos by creating new repository and checking photo count updates
        val photo1 = createSamplePhoto(id = "photo1", categoryId = "update_test")
        val photo2 = createSamplePhoto(id = "photo2", categoryId = "update_test")

        repository.addPhoto(photo1)
        repository.addPhoto(photo2)

        // Create new repository instance to verify persistence
        val newRepository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // Then: Photo count should be updated and persist
        val photosInCategory = newRepository.getPhotosForCategory("update_test")
        assertEquals("Should have 2 photos in category", 2, photosInCategory.size)
    }

    @Test
    fun categoryRemovalShouldCascadeDeletePhotos() = runTest {
        // Given: Initialize data and add category with photos
        repository.initializeSampleData()
        val category = createSampleCategory(id = "cascade_test")
        repository.addCategory(category)

        val photo1 = createSamplePhoto(id = "photo1", categoryId = "cascade_test")
        val photo2 = createSamplePhoto(id = "photo2", categoryId = "cascade_test")
        repository.addPhoto(photo1)
        repository.addPhoto(photo2)

        // Verify photos exist
        val photosBeforeDelete = repository.getPhotosForCategory("cascade_test")
        assertEquals("Should have 2 photos before delete", 2, photosBeforeDelete.size)

        // When: Remove category
        val removed = repository.removeCategory("cascade_test")
        assertTrue("Category should be removed", removed)

        // Then: Photos should be cascade deleted
        val photosAfterDelete = repository.getPhotosForCategory("cascade_test")
        assertTrue("Photos should be cascade deleted", photosAfterDelete.isEmpty())

        // Verify category is gone
        val deletedCategory = repository.getCategory("cascade_test")
        assertNull("Category should be deleted", deletedCategory)
    }

    // ================== Photo Operations with Persistence ==================

    @Test
    fun photoOperationsShouldPersistAcrossRepositoryInstances() = runTest {
        // Given: Initialize data and add a custom photo
        repository.initializeSampleData()
        val newPhoto = createSamplePhoto(
            id = "persistent_photo",
            categoryId = "animals", // Use existing category
            position = 10
        )

        val added = repository.addPhoto(newPhoto)
        assertTrue("Photo should be added", added)

        // When: Create a new repository instance (simulates app restart)
        val newRepository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // Then: Photo should still exist
        val photosInCategory = newRepository.getPhotosForCategory("animals")
        val persistentPhoto = photosInCategory.find { it.id == "persistent_photo" }
        assertNotNull("Photo should persist across repository instances", persistentPhoto)
        assertEquals("Photo position should match", 10, persistentPhoto!!.position)

        // Verify in all photos list
        val allPhotos = newRepository.getAllPhotos()
        val foundPhoto = allPhotos.find { it.id == "persistent_photo" }
        assertNotNull("Photo should be in all photos list", foundPhoto)
    }

    @Test
    fun photoRemovalShouldPersist() = runTest {
        // Given: Initialize data
        repository.initializeSampleData()

        // Get initial photo to remove
        val allPhotos = repository.getAllPhotos()
        assertTrue("Should have photos to test removal", allPhotos.isNotEmpty())
        val photoToRemove = allPhotos.first()

        // When: Remove photo
        val removed = repository.removePhoto(photoToRemove.id)
        assertTrue("Photo should be removed", removed)

        // Create new repository instance to verify persistence
        val newRepository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // Then: Photo should still be gone
        val photosAfterRestart = newRepository.getAllPhotos()
        val deletedPhoto = photosAfterRestart.find { it.id == photoToRemove.id }
        assertNull("Photo should remain deleted after restart", deletedPhoto)

        // Verify photo count is correct
        assertEquals("Photo count should be reduced by 1",
            allPhotos.size - 1, photosAfterRestart.size)
    }

    // ================== Reactive Flow Operations ==================

    @Test
    fun categoriesFlowShouldUpdateReactively() = runTest {
        // Given: Initialize data
        repository.initializeSampleData()

        // When: Observe categories flow
        val initialCategories = repository.getCategoriesFlow().first()
        val initialCount = initialCategories.size

        // Add a new category
        val newCategory = createSampleCategory(id = "reactive_test", displayName = "Reactive Test")
        repository.addCategory(newCategory)

        // Then: Flow should emit updated list
        val updatedCategories = repository.getCategoriesFlow().first()
        assertEquals("Category count should increase", initialCount + 1, updatedCategories.size)

        val addedCategory = updatedCategories.find { it.id == "reactive_test" }
        assertNotNull("New category should be in flow emission", addedCategory)
        assertEquals("Category display name should match", "Reactive Test", addedCategory!!.displayName)
    }

    @Test
    fun categoriesWithPhotosFlowShouldUpdateReactively() = runTest {
        // Given: Initialize data
        repository.initializeSampleData()

        // When: Observe categories with photos flow
        val initialCategoriesWithPhotos = repository.getAllCategoriesWithPhotosFlow().first()

        // Add a category with photos
        val category = createSampleCategory(id = "flow_test", displayName = "Flow Test")
        repository.addCategory(category)

        val photo1 = createSamplePhoto(id = "flow_photo1", categoryId = "flow_test")
        val photo2 = createSamplePhoto(id = "flow_photo2", categoryId = "flow_test")
        repository.addPhoto(photo1)
        repository.addPhoto(photo2)

        // Then: Flow should emit updated list with new category and photos
        val updatedCategoriesWithPhotos = repository.getAllCategoriesWithPhotosFlow().first()

        val newCategoryWithPhotos = updatedCategoriesWithPhotos.find { it.category.id == "flow_test" }
        assertNotNull("New category should be in flow emission", newCategoryWithPhotos)
        assertEquals("Category should have 2 photos", 2, newCategoryWithPhotos!!.photos.size)
    }

    // ================== Backward Compatibility Tests ==================

    @Test
    fun repositoryShouldMaintainCategoryManagerCompatibility() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        // When: Get data through repository
        val categories = repository.getCategories()
        val allPhotos = repository.getAllPhotos()
        val categoriesWithPhotos = repository.getAllCategoriesWithPhotos()

        // Then: Should match CategoryManager behavior exactly
        assertEquals("Should have 3 categories like CategoryManager", 3, categories.size)
        assertEquals("Should have 6 photos like CategoryManager", 6, allPhotos.size)
        assertEquals("Should have 3 categories with photos", 3, categoriesWithPhotos.size)

        // Verify each category has photos
        for (categoryWithPhotos in categoriesWithPhotos) {
            assertTrue("Each category should have photos", categoryWithPhotos.photos.isNotEmpty())
            assertEquals("Photo count should match photos list size",
                categoryWithPhotos.photos.size, categoryWithPhotos.category.photoCount)
        }

        // Verify photo paths are correct for assets
        val photoPaths = repository.getAllPhotoPaths()
        assertEquals("Should have correct number of photo paths", 6, photoPaths.size)
        assertTrue("All photo paths should be valid", photoPaths.all { it.isNotBlank() })
    }

    @Test
    fun repositoryAPIsShouldMatchCategoryManagerAPIs() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        // Test getting specific category (existing API)
        val animalsCategory = repository.getCategory("animals")
        assertNotNull("Should be able to get animals category", animalsCategory)
        assertEquals("Animals category should have correct display name", "Animals", animalsCategory!!.displayName)

        // Test getting photos for category (existing API)
        val animalsPhotos = repository.getPhotosForCategory("animals")
        assertTrue("Animals category should have photos", animalsPhotos.isNotEmpty())

        // Test getting category with photos (existing API)
        val animalsCategoryWithPhotos = repository.getCategoryWithPhotos("animals")
        assertNotNull("Should be able to get animals category with photos", animalsCategoryWithPhotos)
        assertEquals("Photo count should match", animalsPhotos.size, animalsCategoryWithPhotos!!.photos.size)
    }

    // ================== Error Handling and Fallback Tests ==================

    @Test
    fun repositoryShouldHandleNonExistentCategoryGracefully() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        // When: Try to get non-existent category
        val nonExistentCategory = repository.getCategory("does_not_exist")

        // Then: Should return null gracefully
        assertNull("Non-existent category should return null", nonExistentCategory)

        // When: Try to get photos for non-existent category
        val nonExistentPhotos = repository.getPhotosForCategory("does_not_exist")

        // Then: Should return empty list
        assertTrue("Non-existent category photos should return empty list", nonExistentPhotos.isEmpty())

        // When: Try to get category with photos for non-existent category
        val nonExistentCategoryWithPhotos = repository.getCategoryWithPhotos("does_not_exist")

        // Then: Should return null gracefully
        assertNull("Non-existent category with photos should return null", nonExistentCategoryWithPhotos)
    }

    @Test
    fun repositoryShouldHandleInvalidOperationsGracefully() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        // When: Try to remove non-existent category
        val removedNonExistent = repository.removeCategory("does_not_exist")

        // Then: Should return false
        assertFalse("Removing non-existent category should return false", removedNonExistent)

        // When: Try to remove non-existent photo
        val removedNonExistentPhoto = repository.removePhoto("does_not_exist")

        // Then: Should return false
        assertFalse("Removing non-existent photo should return false", removedNonExistentPhoto)

        // When: Try to add photo to non-existent category
        val invalidPhoto = createSamplePhoto(categoryId = "does_not_exist")
        val addedInvalidPhoto = repository.addPhoto(invalidPhoto)

        // Then: Should handle gracefully (implementation dependent)
        // Note: This may return false or handle with fallback depending on implementation
    }

    // ================== Performance and Resource Management ==================

    @Test
    fun repositoryOperationsShouldBePerformant() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        val startTime = System.currentTimeMillis()

        // When: Perform various operations
        repeat(10) { iteration ->
            val category = createSampleCategory(
                id = "perf_cat_$iteration",
                displayName = "Performance Category $iteration"
            )
            repository.addCategory(category)

            val photo = createSamplePhoto(
                id = "perf_photo_$iteration",
                categoryId = "perf_cat_$iteration"
            )
            repository.addPhoto(photo)
        }

        // Get all data
        val categories = repository.getCategories()
        val photos = repository.getAllPhotos()
        val categoriesWithPhotos = repository.getAllCategoriesWithPhotos()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then: Operations should complete within reasonable time
        assertTrue("Operations should complete within 2 seconds", duration < 2000)
        assertTrue("Should have added categories", categories.size >= 13) // 3 initial + 10 added
        assertTrue("Should have added photos", photos.size >= 16) // 6 initial + 10 added
        assertEquals("Categories with photos should match categories", categories.size, categoriesWithPhotos.size)
    }

    // ================== Data Consistency Tests ==================

    @Test
    fun repositoryShouldMaintainDataConsistency() = runTest {
        // Given: Initialize repository
        repository.initializeSampleData()

        // When: Perform multiple operations
        val category1 = createSampleCategory(id = "consistency_cat1")
        val category2 = createSampleCategory(id = "consistency_cat2")

        repository.addCategory(category1)
        repository.addCategory(category2)

        val photo1 = createSamplePhoto(id = "consistency_photo1", categoryId = "consistency_cat1")
        val photo2 = createSamplePhoto(id = "consistency_photo2", categoryId = "consistency_cat1")
        val photo3 = createSamplePhoto(id = "consistency_photo3", categoryId = "consistency_cat2")

        repository.addPhoto(photo1)
        repository.addPhoto(photo2)
        repository.addPhoto(photo3)

        // Then: Data should be consistent across different access methods
        val allCategories = repository.getCategories()
        val allPhotos = repository.getAllPhotos()
        val categoriesWithPhotos = repository.getAllCategoriesWithPhotos()

        // Verify category consistency
        assertEquals("Categories count should be consistent", allCategories.size, categoriesWithPhotos.size)

        // Verify photo consistency
        val totalPhotosFromCategories = categoriesWithPhotos.sumOf { it.photos.size }
        assertEquals("Total photos should be consistent", allPhotos.size, totalPhotosFromCategories)

        // Verify specific category photo counts
        val cat1WithPhotos = categoriesWithPhotos.find { it.category.id == "consistency_cat1" }
        val cat2WithPhotos = categoriesWithPhotos.find { it.category.id == "consistency_cat2" }

        assertNotNull("Category 1 should exist", cat1WithPhotos)
        assertNotNull("Category 2 should exist", cat2WithPhotos)

        assertEquals("Category 1 should have 2 photos", 2, cat1WithPhotos!!.photos.size)
        assertEquals("Category 2 should have 1 photo", 1, cat2WithPhotos!!.photos.size)

        // Verify individual photo access matches
        val cat1Photos = repository.getPhotosForCategory("consistency_cat1")
        val cat2Photos = repository.getPhotosForCategory("consistency_cat2")

        assertEquals("Category 1 photos should match", cat1WithPhotos.photos.size, cat1Photos.size)
        assertEquals("Category 2 photos should match", cat2WithPhotos.photos.size, cat2Photos.size)
    }

    // ================== Multiple Repository Instance Tests ==================

    @Test
    fun multipleRepositoryInstancesShouldShareData() = runTest {
        // Given: Initialize first repository
        repository.initializeSampleData()

        val category = createSampleCategory(id = "shared_cat", displayName = "Shared Category")
        repository.addCategory(category)

        // When: Create second repository instance
        val repository2 = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // Then: Second repository should see data from first
        val sharedCategory = repository2.getCategory("shared_cat")
        assertNotNull("Second repository should see shared category", sharedCategory)
        assertEquals("Shared category display name should match", "Shared Category", sharedCategory!!.displayName)

        // When: Add data through second repository
        val photo = createSamplePhoto(id = "shared_photo", categoryId = "shared_cat")
        repository2.addPhoto(photo)

        // Then: First repository should see the new data
        val photosFromFirst = repository.getPhotosForCategory("shared_cat")
        val sharedPhoto = photosFromFirst.find { it.id == "shared_photo" }
        assertNotNull("First repository should see photo added by second", sharedPhoto)
    }
}