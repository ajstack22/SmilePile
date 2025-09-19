package com.smilepile.app.repository

import com.smilepile.app.managers.CategoryManager
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Test that verifies CategoryManager compatibility - tests the fallback behavior
 * since we can't easily test Room database in unit tests without Robolectric
 */
class CategoryRepositoryImplTest {

    private lateinit var categoryManager: CategoryManager

    @Before
    fun setUp() {
        categoryManager = CategoryManager()
    }

    @Test
    fun `test CategoryManager initializes with sample data`() {
        // Given: CategoryManager is initialized (happens in setUp)

        // When: Getting categories
        val categories = categoryManager.getCategories()

        // Then: Should have sample categories
        assertEquals(3, categories.size)

        val categoryNames = categories.map { it.displayName }.sorted()
        assertEquals(listOf("Animals", "Family", "Fun Times"), categoryNames)
    }

    @Test
    fun `test CategoryManager maintains backward compatibility`() {
        // Given: CategoryManager is initialized

        // When: Getting all categories with photos
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        // Then: Should match expected behavior
        assertEquals(3, categoriesWithPhotos.size)

        val totalPhotos = categoriesWithPhotos.sumOf { it.photos.size }
        assertEquals(6, totalPhotos) // 6 sample images distributed across 3 categories
    }

    @Test
    fun `test add and get category`() {
        // Given: CategoryManager is initialized

        // When: Adding a new category
        val newCategory = Category(
            id = "test_category",
            name = "test_category",
            displayName = "Test Category",
            coverImagePath = null,
            description = "Test category description",
            position = 3
        )

        val added = categoryManager.addCategory(newCategory)

        // Then: Category should be added successfully
        assertTrue(added)

        val retrievedCategory = categoryManager.getCategory("test_category")
        assertNotNull(retrievedCategory)
        assertEquals("Test Category", retrievedCategory?.displayName)
    }

    @Test
    fun `test add and get photo`() {
        // Given: CategoryManager is initialized

        // When: Adding a new photo to animals category
        val newPhoto = Photo(
            id = "test_photo",
            path = "test_image.png",
            name = "test_image",
            categoryId = "animals",
            position = 5,
            isFromAssets = true
        )

        val added = categoryManager.addPhoto(newPhoto)

        // Then: Photo should be added successfully
        assertTrue(added)

        val photosInCategory = categoryManager.getPhotosForCategory("animals")
        val addedPhoto = photosInCategory.find { it.id == "test_photo" }
        assertNotNull(addedPhoto)
        assertEquals("test_image.png", addedPhoto?.path)
    }

    @Test
    fun `test backward compatibility with photo paths`() {
        // Given: CategoryManager is initialized

        // When: Getting all photo paths
        val photoPaths = categoryManager.getAllPhotoPaths()

        // Then: Should have correct asset paths for backward compatibility
        assertEquals(6, photoPaths.size)

        // All paths should start with sample_images/ for assets
        assertTrue(photoPaths.all { it.startsWith("sample_images/") })
    }
}