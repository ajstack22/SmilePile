package com.smilepile.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.data.database.entities.Album
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.data.database.entities.Photo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Database performance tests for SmilePile (F0006).
 * Tests that Room database queries complete within the required 50ms threshold.
 */
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {

    private lateinit var database: TestSmilePileDatabase
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var albumDao: AlbumDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestSmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        photoDao = database.photoDao()
        categoryDao = database.categoryDao()
        albumDao = database.albumDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCategoryInsertPerformance() = runBlocking {
        val category = createTestCategory("Test Category")

        val startTime = System.currentTimeMillis()
        val categoryId = categoryDao.insertCategory(category)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Category insert should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertTrue("Category should be inserted with valid ID", categoryId > 0)
    }

    @Test
    fun testCategoryQueryPerformance() = runBlocking {
        // Setup test data
        val categories = (1..10).map { createTestCategory("Category $it") }
        categories.forEach { categoryDao.insertCategory(it) }

        val startTime = System.currentTimeMillis()
        val retrievedCategories = categoryDao.getAllCategories()
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Category query should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should retrieve all inserted categories", categories.size, retrievedCategories.size)
    }

    @Test
    fun testPhotoInsertPerformance() = runBlocking {
        // Setup category first
        val category = createTestCategory("Photo Category")
        val categoryId = categoryDao.insertCategory(category)

        val photo = createTestPhoto(categoryId)

        val startTime = System.currentTimeMillis()
        val photoId = photoDao.insertPhoto(photo)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Photo insert should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertTrue("Photo should be inserted with valid ID", photoId > 0)
    }

    @Test
    fun testPhotoQueryByCategoryPerformance() = runBlocking {
        // Setup test data
        val category = createTestCategory("Performance Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = (1..20).map { createTestPhoto(categoryId, "photo_$it.jpg") }
        photoDao.insertPhotos(photos)

        val startTime = System.currentTimeMillis()
        val retrievedPhotos = photoDao.getPhotosByCategory(categoryId)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Photo category query should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should retrieve all photos for category", photos.size, retrievedPhotos.size)
    }

    @Test
    fun testBulkPhotoInsertPerformance() = runBlocking {
        // Setup category
        val category = createTestCategory("Bulk Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = (1..100).map { createTestPhoto(categoryId, "bulk_photo_$it.jpg") }

        val startTime = System.currentTimeMillis()
        val photoIds = photoDao.insertPhotos(photos)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Bulk photo insert should complete in reasonable time, took ${executionTime}ms", executionTime < 200)
        assertEquals("Should insert all photos", photos.size, photoIds.size)
        assertTrue("All photo IDs should be valid", photoIds.all { it > 0 })
    }

    @Test
    fun testPhotoCountQueryPerformance() = runBlocking {
        // Setup test data
        val category = createTestCategory("Count Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = (1..50).map { createTestPhoto(categoryId, "count_photo_$it.jpg") }
        photoDao.insertPhotos(photos)

        val startTime = System.currentTimeMillis()
        val count = photoDao.getPhotoCountByCategory(categoryId)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Photo count query should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should count all photos in category", photos.size, count)
    }

    @Test
    fun testComplexPhotoQueryPerformance() = runBlocking {
        // Setup test data with multiple categories
        val categories = (1..5).map {
            val category = createTestCategory("Category $it")
            categoryDao.insertCategory(category) to category
        }

        categories.forEach { (categoryId, _) ->
            val photos = (1..30).map { createTestPhoto(categoryId, "complex_photo_${categoryId}_$it.jpg") }
            photoDao.insertPhotos(photos)
        }

        val categoryIds = categories.map { it.first }

        val startTime = System.currentTimeMillis()
        val photos = photoDao.getPhotosByMultipleCategories(categoryIds)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Complex photo query should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should retrieve photos from all categories", 150, photos.size)
    }

    @Test
    fun testPhotoSearchPerformance() = runBlocking {
        // Setup test data
        val category = createTestCategory("Search Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = listOf(
            createTestPhoto(categoryId, "vacation_beach.jpg"),
            createTestPhoto(categoryId, "vacation_mountain.jpg"),
            createTestPhoto(categoryId, "work_meeting.jpg"),
            createTestPhoto(categoryId, "family_dinner.jpg"),
            createTestPhoto(categoryId, "vacation_sunset.jpg")
        )
        photoDao.insertPhotos(photos)

        val startTime = System.currentTimeMillis()
        val searchResults = photoDao.searchPhotosByName("%vacation%")
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Photo search should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should find 3 vacation photos", 3, searchResults.size)
    }

    @Test
    fun testAlbumQueryPerformance() = runBlocking {
        // Setup test data
        val albums = (1..15).map { createTestAlbum("Album $it") }
        albums.forEach { albumDao.insertAlbum(it) }

        val startTime = System.currentTimeMillis()
        val retrievedAlbums = albumDao.getAllAlbums()
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Album query should complete in <50ms, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should retrieve all albums", albums.size, retrievedAlbums.size)
    }

    @Test
    fun testConcurrentQueryPerformance() = runBlocking {
        // Setup test data
        val category = createTestCategory("Concurrent Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = (1..50).map { createTestPhoto(categoryId, "concurrent_photo_$it.jpg") }
        photoDao.insertPhotos(photos)

        val startTime = System.currentTimeMillis()

        // Simulate concurrent operations
        val photos1 = photoDao.getPhotosByCategory(categoryId)
        val count = photoDao.getPhotoCountByCategory(categoryId)
        val categories = categoryDao.getAllCategories()

        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Concurrent queries should complete efficiently, took ${executionTime}ms", executionTime < 100)
        assertEquals("Should retrieve correct photo count", photos.size, photos1.size)
        assertEquals("Should retrieve correct count", photos.size, count)
        assertFalse("Should retrieve categories", categories.isEmpty())
    }

    // Helper methods
    private fun createTestCategory(name: String): Category {
        return Category(
            name = name,
            description = "Test category for performance testing",
            dateCreated = Date(),
            photoCount = 0
        )
    }

    private fun createTestPhoto(categoryId: Long, fileName: String = "test_photo.jpg"): Photo {
        return Photo(
            filePath = "/test/path/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            fileSize = 1024 * 1024, // 1MB
            mimeType = "image/jpeg"
        )
    }

    private fun createTestAlbum(name: String): Album {
        return Album(
            name = name,
            description = "Test album for performance testing",
            dateCreated = Date(),
            photoCount = 0
        )
    }
}