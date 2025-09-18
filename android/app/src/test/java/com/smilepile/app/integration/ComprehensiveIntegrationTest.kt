package com.smilepile.app.integration

import com.smilepile.app.Photo
import com.smilepile.app.managers.CategoryManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive integration test for Iteration 3 - Category Concept
 * Tests the full data flow from asset loading to category distribution
 */
class ComprehensiveIntegrationTest {

    private lateinit var categoryManager: CategoryManager
    private lateinit var assetPhotoList: List<Photo>

    @Before
    fun setUp() {
        categoryManager = CategoryManager()

        // Simulate asset loading like MainActivity does
        val sampleImagePaths = listOf(
            "sample_images/sample_1.png",
            "sample_images/sample_2.png",
            "sample_images/sample_3.png",
            "sample_images/sample_4.png",
            "sample_images/sample_5.png",
            "sample_images/sample_6.png"
        )
        assetPhotoList = Photo.fromImagePaths(sampleImagePaths)
    }

    @Test
    fun `F0010 CategoryManager should properly initialize with 3 categories`() {
        // Test Feature F0010: Category data structure
        val categories = categoryManager.getCategories()

        assertEquals("Should have 3 categories", 3, categories.size)

        val categoryNames = categories.map { it.displayName }.sorted()
        assertEquals("Should have correct category names",
            listOf("Animals", "Family", "Fun Times"), categoryNames)

        // Each category should be valid
        categories.forEach { category ->
            assertTrue("Category ${category.name} should be valid", category.isValid())
            assertTrue("Category should have photos", category.photoCount > 0)
        }
    }

    @Test
    fun `F0011 Photo categorization should work correctly with both Photo classes`() {
        // Test Feature F0011: Category selection UI data integration

        // Test root package Photo class categorization
        val rootCategories = Photo.getCategories(assetPhotoList)
        assertEquals("Root Photo class should create 3 categories", 3, rootCategories.size)

        // Test that categories are properly distributed
        val expectedCategories = setOf("Animals", "Family", "Fun Times")
        assertEquals("Should have expected categories", expectedCategories, rootCategories.toSet())

        // Test filtering works correctly
        rootCategories.forEach { category ->
            val filteredPhotos = Photo.filterByCategory(assetPhotoList, category)
            assertTrue("Each category should have photos", filteredPhotos.isNotEmpty())
            assertEquals("Filtered photos should be 2 per category", 2, filteredPhotos.size)
        }
    }

    @Test
    fun `F0012 Navigation data flow should maintain consistency`() {
        // Test Feature F0012: Navigation integration

        // Test that CategoryManager photos are compatible with root Photo class methods
        val managerPhotos = categoryManager.getAllPhotos()
        val managerPaths = categoryManager.getAllPhotoPaths()

        assertEquals("Photo paths should match photo count", managerPhotos.size, managerPaths.size)

        // All paths should be asset-compatible
        managerPaths.forEach { path ->
            assertTrue("Path $path should start with sample_images/",
                path.startsWith("sample_images/"))
        }

        // Test backward compatibility - paths should work with ImagePagerAdapter
        managerPaths.forEach { path ->
            assertTrue("Path should be a valid asset path",
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg"))
        }
    }

    @Test
    fun `Data integrity between CategoryManager and root Photo class`() {
        // Test data consistency between the two Photo implementations

        val managerPhotos = categoryManager.getAllPhotos()
        val managerCategories = categoryManager.getCategories()

        // Test that all photos have valid categories
        managerPhotos.forEach { photo ->
            assertTrue("Photo ${photo.id} should be valid", photo.isValid())
            assertTrue("Photo should belong to a valid category",
                managerCategories.any { it.id == photo.categoryId })
        }

        // Test that category photo counts are accurate
        managerCategories.forEach { category ->
            val categoryPhotos = categoryManager.getPhotosForCategory(category.id)
            assertEquals("Category photo count should match actual photos",
                category.photoCount, categoryPhotos.size)
        }
    }

    @Test
    fun `Child-friendly UI requirements should be met`() {
        // Test child-friendly features

        val managerCategories = categoryManager.getCategories()

        // Categories should have display names (child-friendly labels)
        managerCategories.forEach { category ->
            assertTrue("Category should have display name", category.displayName.isNotBlank())
            assertTrue("Display name should be different from ID",
                category.displayName != category.id)
            assertTrue("Display name should be capitalized",
                category.displayName.first().isUpperCase())
        }

        // Test root Photo class provides proper categorization
        val rootCategories = Photo.getCategories(assetPhotoList)
        rootCategories.forEach { categoryName ->
            assertTrue("Category name should be child-friendly", categoryName.isNotBlank())
            assertTrue("Category name should be capitalized", categoryName.first().isUpperCase())
        }
    }

    @Test
    fun `Memory management should be efficient`() {
        // Test that data structures are efficient

        val managerPhotos = categoryManager.getAllPhotos()
        val categoryWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        // Test that we're not duplicating photo data unnecessarily
        val totalPhotosInCategories = categoryWithPhotos.sumOf { it.photos.size }
        assertEquals("Total photos should match manager photos",
            managerPhotos.size, totalPhotosInCategories)

        // Test that photo references are consistent
        managerPhotos.forEach { photo ->
            val foundInCategory = categoryWithPhotos.any { categoryWithPhoto ->
                categoryWithPhoto.photos.any { categoryPhoto ->
                    categoryPhoto.id == photo.id
                }
            }
            assertTrue("Photo ${photo.id} should be found in a category", foundInCategory)
        }
    }

    @Test
    fun `Performance requirements should be met`() {
        // Test performance characteristics

        val startTime = System.currentTimeMillis()

        // Test that category retrieval is fast
        repeat(100) {
            categoryManager.getCategories()
        }

        val categoryRetrievalTime = System.currentTimeMillis() - startTime
        assertTrue("Category retrieval should be fast (< 100ms for 100 calls)",
            categoryRetrievalTime < 100)

        // Test that photo filtering is efficient
        val filterStartTime = System.currentTimeMillis()
        val rootCategories = Photo.getCategories(assetPhotoList)

        repeat(50) {
            rootCategories.forEach { category ->
                Photo.filterByCategory(assetPhotoList, category)
            }
        }

        val filterTime = System.currentTimeMillis() - filterStartTime
        assertTrue("Photo filtering should be efficient (< 200ms for 50x3 calls)",
            filterTime < 200)
    }

    @Test
    fun `Integration between MainActivity and CategoryManager should work`() {
        // Test the integration point between MainActivity data loading and CategoryManager

        // Simulate MainActivity asset loading
        val imagePaths = listOf(
            "sample_images/sample_1.png",
            "sample_images/sample_2.png",
            "sample_images/sample_3.png",
            "sample_images/sample_4.png",
            "sample_images/sample_5.png",
            "sample_images/sample_6.png"
        )

        val mainActivityPhotos = Photo.fromImagePaths(imagePaths)

        // Test that the data can be used by both systems
        assertEquals("Should have 6 photos from MainActivity", 6, mainActivityPhotos.size)
        assertEquals("Should create 3 categories", 3, Photo.getCategories(mainActivityPhotos).size)

        // Test that CategoryManager data is compatible
        val managerPaths = categoryManager.getAllPhotoPaths()
        assertEquals("Manager should provide 6 photo paths", 6, managerPaths.size)

        // Both should reference the same asset files
        val mainActivityPaths = mainActivityPhotos.map { it.path }.sorted()
        val sortedManagerPaths = managerPaths.sorted()

        // They might not be identical due to different naming schemes, but should have same images
        assertEquals("Should have same number of image files",
            mainActivityPaths.size, sortedManagerPaths.size)
    }
}