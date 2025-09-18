package com.smilepile.app.models

import org.junit.Test
import org.junit.Assert.*

class CategoryWithPhotosTest {

    private val testCategory = Category(
        id = "test_category",
        name = "test",
        displayName = "Test Category",
        coverImagePath = "cover.jpg"
    )

    private val testPhotos = listOf(
        Photo(
            id = "photo1",
            path = "image1.jpg",
            name = "Photo 1",
            categoryId = "test_category",
            position = 2
        ),
        Photo(
            id = "photo2",
            path = "image2.jpg",
            name = "Photo 2",
            categoryId = "test_category",
            position = 1
        ),
        Photo(
            id = "photo3",
            path = "image3.jpg",
            name = "Photo 3",
            categoryId = "test_category",
            position = 3
        )
    )

    @Test
    fun `categoryWithPhotos creation should work correctly`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)

        assertEquals(testCategory, categoryWithPhotos.category)
        assertEquals(testPhotos, categoryWithPhotos.photos)
    }

    @Test
    fun `photoCount should return correct number of photos`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)

        assertEquals(3, categoryWithPhotos.photoCount)
    }

    @Test
    fun `photoCount should return 0 for empty photos list`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, emptyList())

        assertEquals(0, categoryWithPhotos.photoCount)
    }

    @Test
    fun `coverImage should return first photo path when photos exist`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)

        assertEquals("image1.jpg", categoryWithPhotos.coverImage)
    }

    @Test
    fun `coverImage should return category coverImagePath when no photos`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, emptyList())

        assertEquals("cover.jpg", categoryWithPhotos.coverImage)
    }

    @Test
    fun `coverImage should return null when no photos and no category cover`() {
        val categoryWithoutCover = testCategory.copy(coverImagePath = null)
        val categoryWithPhotos = CategoryWithPhotos(categoryWithoutCover, emptyList())

        assertNull(categoryWithPhotos.coverImage)
    }

    @Test
    fun `sortedPhotos should return photos sorted by position`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)
        val sortedPhotos = categoryWithPhotos.sortedPhotos

        assertEquals(3, sortedPhotos.size)
        assertEquals("photo2", sortedPhotos[0].id) // position 1
        assertEquals("photo1", sortedPhotos[1].id) // position 2
        assertEquals("photo3", sortedPhotos[2].id) // position 3
    }

    @Test
    fun `isValid should return true when category and all photos are valid and match categoryId`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)

        assertTrue(categoryWithPhotos.isValid())
    }

    @Test
    fun `isValid should return false when category is invalid`() {
        val invalidCategory = testCategory.copy(id = "")
        val categoryWithPhotos = CategoryWithPhotos(invalidCategory, testPhotos)

        assertFalse(categoryWithPhotos.isValid())
    }

    @Test
    fun `isValid should return false when any photo is invalid`() {
        val invalidPhotos = testPhotos + Photo(
            id = "",
            path = "image4.jpg",
            name = "Photo 4",
            categoryId = "test_category"
        )
        val categoryWithPhotos = CategoryWithPhotos(testCategory, invalidPhotos)

        assertFalse(categoryWithPhotos.isValid())
    }

    @Test
    fun `isValid should return false when photo categoryId doesn't match category id`() {
        val mismatchedPhotos = testPhotos + Photo(
            id = "photo4",
            path = "image4.jpg",
            name = "Photo 4",
            categoryId = "different_category"
        )
        val categoryWithPhotos = CategoryWithPhotos(testCategory, mismatchedPhotos)

        assertFalse(categoryWithPhotos.isValid())
    }

    @Test
    fun `getPhotoPaths should return sorted photo asset paths`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, testPhotos)
        val photoPaths = categoryWithPhotos.getPhotoPaths()

        assertEquals(3, photoPaths.size)
        assertEquals("sample_images/image2.jpg", photoPaths[0]) // position 1
        assertEquals("sample_images/image1.jpg", photoPaths[1]) // position 2
        assertEquals("sample_images/image3.jpg", photoPaths[2]) // position 3
    }

    @Test
    fun `getPhotoPaths should return empty list when no photos`() {
        val categoryWithPhotos = CategoryWithPhotos(testCategory, emptyList())
        val photoPaths = categoryWithPhotos.getPhotoPaths()

        assertTrue(photoPaths.isEmpty())
    }
}