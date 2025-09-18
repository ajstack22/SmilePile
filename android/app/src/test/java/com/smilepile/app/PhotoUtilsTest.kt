package com.smilepile.app

import org.junit.Test
import org.junit.Assert.*

class PhotoUtilsTest {

    @Test
    fun `Photo constructor should create photo with correct properties`() {
        val photo = Photo("photo_1", "test_path.jpg", "Test Category", "Test Photo")

        assertEquals("photo_1", photo.id)
        assertEquals("test_path.jpg", photo.path)
        assertEquals("Test Category", photo.category)
        assertEquals("Test Photo", photo.displayName)
    }

    @Test
    fun `Photo constructor should use default displayName`() {
        val photo = Photo("photo_1", "path/to/test_image.jpg", "Test Category")

        assertEquals("test_image", photo.displayName)
    }

    @Test
    fun `Photo filterByCategory should return photos for specified category`() {
        val photos = listOf(
            Photo("1", "photo1.jpg", "Category A"),
            Photo("2", "photo2.jpg", "Category B"),
            Photo("3", "photo3.jpg", "Category A"),
            Photo("4", "photo4.jpg", "Category C")
        )

        val categoryAPhotos = Photo.filterByCategory(photos, "Category A")

        assertEquals(2, categoryAPhotos.size)
        assertTrue(categoryAPhotos.all { it.category == "Category A" })
    }

    @Test
    fun `Photo filterByCategory should return empty list for non-existent category`() {
        val photos = listOf(
            Photo("1", "photo1.jpg", "Category A"),
            Photo("2", "photo2.jpg", "Category B")
        )

        val result = Photo.filterByCategory(photos, "Non-existent Category")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Photo filterByCategory should handle empty photo list`() {
        val result = Photo.filterByCategory(emptyList(), "Any Category")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Photo getCategories should return unique categories sorted`() {
        val photos = listOf(
            Photo("1", "photo1.jpg", "Category C"),
            Photo("2", "photo2.jpg", "Category A"),
            Photo("3", "photo3.jpg", "Category C"),
            Photo("4", "photo4.jpg", "Category B"),
            Photo("5", "photo5.jpg", "Category A")
        )

        val categories = Photo.getCategories(photos)

        assertEquals(3, categories.size)
        assertEquals(listOf("Category A", "Category B", "Category C"), categories)
    }

    @Test
    fun `Photo getCategories should handle empty photo list`() {
        val result = Photo.getCategories(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Photo fromImagePaths should create photos with correct categories`() {
        val imagePaths = listOf(
            "sample_images/sample_1.jpg",
            "sample_images/sample_3.jpg",
            "sample_images/sample_5.jpg",
            "sample_images/other.jpg"
        )

        val photos = Photo.fromImagePaths(imagePaths)

        assertEquals(4, photos.size)
        assertEquals("Animals", photos[0].category)
        assertEquals("Family", photos[1].category)
        assertEquals("Fun Times", photos[2].category)
        assertEquals("General", photos[3].category)
    }

    @Test
    fun `Photo fromImagePaths should generate correct IDs`() {
        val imagePaths = listOf("image1.jpg", "image2.jpg")

        val photos = Photo.fromImagePaths(imagePaths)

        assertEquals("photo_0", photos[0].id)
        assertEquals("photo_1", photos[1].id)
    }

    @Test
    fun `Photo data class should have correct equals behavior`() {
        val photo1 = Photo("1", "test.jpg", "Category", "Test")
        val photo2 = Photo("1", "test.jpg", "Category", "Test")
        val photo3 = Photo("2", "test.jpg", "Category", "Test")

        assertEquals(photo1, photo2)
        assertNotEquals(photo1, photo3)
    }

    @Test
    fun `Photo data class should have correct hashCode behavior`() {
        val photo1 = Photo("1", "test.jpg", "Category", "Test")
        val photo2 = Photo("1", "test.jpg", "Category", "Test")

        assertEquals(photo1.hashCode(), photo2.hashCode())
    }

    @Test
    fun `inferCategoryFromFileName should categorize sample files correctly`() {
        val imagePaths = listOf(
            "sample_images/sample_1.jpg",  // Should be Animals
            "sample_images/sample_2.jpg",  // Should be Animals
            "sample_images/sample_3.jpg",  // Should be Family
            "sample_images/sample_4.jpg",  // Should be Family
            "sample_images/sample_5.jpg",  // Should be Fun Times
            "sample_images/sample_6.jpg",  // Should be Fun Times
            "sample_images/random.jpg"     // Should be General
        )

        val photos = Photo.fromImagePaths(imagePaths)

        assertEquals("Animals", photos[0].category)
        assertEquals("Animals", photos[1].category)
        assertEquals("Family", photos[2].category)
        assertEquals("Family", photos[3].category)
        assertEquals("Fun Times", photos[4].category)
        assertEquals("Fun Times", photos[5].category)
        assertEquals("General", photos[6].category)
    }
}