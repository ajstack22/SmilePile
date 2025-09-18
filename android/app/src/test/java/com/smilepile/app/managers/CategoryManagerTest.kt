package com.smilepile.app.managers

import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CategoryManagerTest {

    private lateinit var categoryManager: CategoryManager

    @Before
    fun setUp() {
        categoryManager = CategoryManager()
    }

    @Test
    fun `initialization should create sample data`() {
        val categories = categoryManager.getCategories()
        val photos = categoryManager.getAllPhotos()

        // Should have 3 sample categories
        assertEquals(3, categories.size)

        // Should have 6 sample photos
        assertEquals(6, photos.size)

        // Check category names
        val categoryNames = categories.map { it.displayName }.sorted()
        assertEquals(listOf("Animals", "Family", "Fun Times"), categoryNames)
    }

    @Test
    fun `getCategories should return categories sorted by position`() {
        val categories = categoryManager.getCategories()

        assertEquals(3, categories.size)
        assertEquals(0, categories[0].position)
        assertEquals(1, categories[1].position)
        assertEquals(2, categories[2].position)
    }

    @Test
    fun `getCategory should return correct category by id`() {
        val category = categoryManager.getCategory("animals")

        assertNotNull(category)
        assertEquals("animals", category?.id)
        assertEquals("Animals", category?.displayName)
    }

    @Test
    fun `getCategory should return null for non-existent id`() {
        val category = categoryManager.getCategory("non_existent")

        assertNull(category)
    }

    @Test
    fun `getPhotosForCategory should return photos for specific category`() {
        val animalPhotos = categoryManager.getPhotosForCategory("animals")
        val familyPhotos = categoryManager.getPhotosForCategory("family")
        val funTimesPhotos = categoryManager.getPhotosForCategory("fun_times")

        // Each category should have 2 photos (6 photos distributed across 3 categories)
        assertEquals(2, animalPhotos.size)
        assertEquals(2, familyPhotos.size)
        assertEquals(2, funTimesPhotos.size)

        // All photos should belong to the correct category
        animalPhotos.forEach { assertEquals("animals", it.categoryId) }
        familyPhotos.forEach { assertEquals("family", it.categoryId) }
        funTimesPhotos.forEach { assertEquals("fun_times", it.categoryId) }
    }

    @Test
    fun `getPhotosForCategory should return empty list for non-existent category`() {
        val photos = categoryManager.getPhotosForCategory("non_existent")

        assertTrue(photos.isEmpty())
    }

    @Test
    fun `getCategoryWithPhotos should return category with its photos`() {
        val categoryWithPhotos = categoryManager.getCategoryWithPhotos("animals")

        assertNotNull(categoryWithPhotos)
        assertEquals("animals", categoryWithPhotos?.category?.id)
        assertEquals(2, categoryWithPhotos?.photos?.size)
        assertEquals(2, categoryWithPhotos?.photoCount)
    }

    @Test
    fun `getCategoryWithPhotos should return null for non-existent category`() {
        val categoryWithPhotos = categoryManager.getCategoryWithPhotos("non_existent")

        assertNull(categoryWithPhotos)
    }

    @Test
    fun `getAllCategoriesWithPhotos should return all categories with their photos`() {
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()

        assertEquals(3, categoriesWithPhotos.size)
        categoriesWithPhotos.forEach {
            assertTrue(it.photoCount > 0)
            assertEquals(2, it.photoCount) // Each category should have 2 photos
        }
    }

    @Test
    fun `getAllPhotoPaths should return all photo paths for backward compatibility`() {
        val photoPaths = categoryManager.getAllPhotoPaths()

        assertEquals(6, photoPaths.size)
        photoPaths.forEach {
            assertTrue(it.startsWith("sample_images/"))
        }
    }

    @Test
    fun `addCategory should add valid category successfully`() {
        val newCategory = Category(
            id = "new_category",
            name = "new",
            displayName = "New Category",
            coverImagePath = null
        )

        val result = categoryManager.addCategory(newCategory)

        assertTrue(result)
        assertEquals(4, categoryManager.getCategories().size)
        assertNotNull(categoryManager.getCategory("new_category"))
    }

    @Test
    fun `addCategory should fail for invalid category`() {
        val invalidCategory = Category(
            id = "",
            name = "new",
            displayName = "New Category",
            coverImagePath = null
        )

        val result = categoryManager.addCategory(invalidCategory)

        assertFalse(result)
        assertEquals(3, categoryManager.getCategories().size)
    }

    @Test
    fun `addCategory should fail for duplicate category id`() {
        val duplicateCategory = Category(
            id = "animals", // Already exists
            name = "duplicate",
            displayName = "Duplicate Category",
            coverImagePath = null
        )

        val result = categoryManager.addCategory(duplicateCategory)

        assertFalse(result)
        assertEquals(3, categoryManager.getCategories().size)
    }

    @Test
    fun `addPhoto should add valid photo successfully`() {
        val newPhoto = Photo(
            id = "new_photo",
            path = "new_image.jpg",
            name = "New Photo",
            categoryId = "animals"
        )

        val result = categoryManager.addPhoto(newPhoto)

        assertTrue(result)
        assertEquals(7, categoryManager.getAllPhotos().size)
        assertEquals(3, categoryManager.getPhotosForCategory("animals").size)
    }

    @Test
    fun `addPhoto should fail for invalid photo`() {
        val invalidPhoto = Photo(
            id = "",
            path = "new_image.jpg",
            name = "New Photo",
            categoryId = "animals"
        )

        val result = categoryManager.addPhoto(invalidPhoto)

        assertFalse(result)
        assertEquals(6, categoryManager.getAllPhotos().size)
    }

    @Test
    fun `addPhoto should fail for non-existent category`() {
        val photoWithInvalidCategory = Photo(
            id = "new_photo",
            path = "new_image.jpg",
            name = "New Photo",
            categoryId = "non_existent"
        )

        val result = categoryManager.addPhoto(photoWithInvalidCategory)

        assertFalse(result)
        assertEquals(6, categoryManager.getAllPhotos().size)
    }

    @Test
    fun `removeCategory should remove category and all its photos`() {
        val initialPhotoCount = categoryManager.getAllPhotos().size
        val animalPhotosCount = categoryManager.getPhotosForCategory("animals").size

        val result = categoryManager.removeCategory("animals")

        assertTrue(result)
        assertEquals(2, categoryManager.getCategories().size)
        assertNull(categoryManager.getCategory("animals"))
        assertEquals(initialPhotoCount - animalPhotosCount, categoryManager.getAllPhotos().size)
        assertTrue(categoryManager.getPhotosForCategory("animals").isEmpty())
    }

    @Test
    fun `removeCategory should fail for non-existent category`() {
        val result = categoryManager.removeCategory("non_existent")

        assertFalse(result)
        assertEquals(3, categoryManager.getCategories().size)
    }

    @Test
    fun `removePhoto should remove photo from category`() {
        val animalPhotos = categoryManager.getPhotosForCategory("animals")
        val photoToRemove = animalPhotos.first()
        val initialPhotoCount = categoryManager.getAllPhotos().size

        val result = categoryManager.removePhoto(photoToRemove.id)

        assertTrue(result)
        assertEquals(initialPhotoCount - 1, categoryManager.getAllPhotos().size)
        assertEquals(animalPhotos.size - 1, categoryManager.getPhotosForCategory("animals").size)
    }

    @Test
    fun `removePhoto should fail for non-existent photo`() {
        val result = categoryManager.removePhoto("non_existent")

        assertFalse(result)
        assertEquals(6, categoryManager.getAllPhotos().size)
    }
}