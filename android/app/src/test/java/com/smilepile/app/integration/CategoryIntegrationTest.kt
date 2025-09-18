package com.smilepile.app.integration

import com.smilepile.app.managers.CategoryManager
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CategoryIntegrationTest {

    private lateinit var categoryManager: CategoryManager

    @Before
    fun setUp() {
        categoryManager = CategoryManager()
    }

    @Test
    fun `CategoryManager should provide backward compatible photo paths`() {
        val allPhotoPaths = categoryManager.getAllPhotoPaths()

        // Should have 6 sample photos
        assertEquals(6, allPhotoPaths.size)

        // All paths should start with sample_images/
        allPhotoPaths.forEach { path ->
            assertTrue("Path $path should start with sample_images/", path.startsWith("sample_images/"))
        }
    }

    @Test
    fun `CategoryManager should distribute photos across 3 categories`() {
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        assertEquals(3, categoriesWithPhotos.size)

        // Check category names
        val categoryNames = categoriesWithPhotos.map { it.category.displayName }.sorted()
        assertEquals(listOf("Animals", "Family", "Fun Times"), categoryNames)

        // Each category should have 2 photos (6 photos / 3 categories)
        categoriesWithPhotos.forEach { categoryWithPhotos ->
            assertEquals(2, categoryWithPhotos.photoCount)
            assertEquals(2, categoryWithPhotos.photos.size)
        }
    }

    @Test
    fun `CategoryManager should maintain data integrity`() {
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        categoriesWithPhotos.forEach { categoryWithPhotos ->
            // Each category should be valid
            assertTrue(categoryWithPhotos.isValid())

            // All photos should belong to the correct category
            categoryWithPhotos.photos.forEach { photo ->
                assertEquals(categoryWithPhotos.category.id, photo.categoryId)
                assertTrue(photo.isValid())
            }
        }
    }

    @Test
    fun `CategoryManager should support adding new categories and photos`() {
        val initialCategoryCount = categoryManager.getCategories().size
        val initialPhotoCount = categoryManager.getAllPhotos().size

        // Add a new category
        val newCategory = Category(
            id = "sports",
            name = "sports",
            displayName = "Sports",
            coverImagePath = null,
            description = "Athletic activities"
        )

        assertTrue(categoryManager.addCategory(newCategory))
        assertEquals(initialCategoryCount + 1, categoryManager.getCategories().size)

        // Add a photo to the new category
        val newPhoto = Photo(
            id = "sports_photo_1",
            path = "sports1.jpg",
            name = "Soccer Game",
            categoryId = "sports"
        )

        assertTrue(categoryManager.addPhoto(newPhoto))
        assertEquals(initialPhotoCount + 1, categoryManager.getAllPhotos().size)

        // Verify the category now has the photo
        val sportsCategory = categoryManager.getCategoryWithPhotos("sports")
        assertNotNull(sportsCategory)
        assertEquals(1, sportsCategory!!.photoCount)
        assertEquals("Soccer Game", sportsCategory.photos.first().name)
    }

    @Test
    fun `CategoryManager should provide photo paths compatible with ImagePagerAdapter`() {
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        categoriesWithPhotos.forEach { categoryWithPhotos ->
            val photoPaths = categoryWithPhotos.getPhotoPaths()

            // All paths should be usable by ImagePagerAdapter (start with sample_images/)
            photoPaths.forEach { path ->
                assertTrue("Path $path should be asset-compatible", path.startsWith("sample_images/"))
            }
        }
    }
}