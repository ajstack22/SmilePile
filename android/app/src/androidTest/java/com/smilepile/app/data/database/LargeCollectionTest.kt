package com.smilepile.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Large collection tests for SmilePile.
 * Tests support for 100+ photos per category and large dataset handling.
 */
@RunWith(AndroidJUnit4::class)
class LargeCollectionTest {

    private lateinit var database: TestSmilePileDatabase
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestSmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        photoDao = database.photoDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSupport100PhotosPerCategory() = runBlocking {
        // Create a category
        val category = createTestCategory("Large Category")
        val categoryId = categoryDao.insertCategory(category)

        // Create exactly 100 photos
        val photos = (1..100).map {
            createTestPhoto(categoryId, "photo_$it.jpg")
        }

        // Insert all photos
        val photoIds = photoDao.insertPhotos(photos)
        assertEquals("Should insert all 100 photos", 100, photoIds.size)
        assertTrue("All photo IDs should be valid", photoIds.all { it > 0 })

        // Verify retrieval
        val retrievedPhotos = photoDao.getPhotosByCategory(categoryId)
        assertEquals("Should retrieve all 100 photos", 100, retrievedPhotos.size)

        // Verify count
        val count = photoDao.getPhotoCountByCategory(categoryId)
        assertEquals("Should count 100 photos", 100, count)
    }

    @Test
    fun testSupport500PhotosPerCategory() = runBlocking {
        // Test with 500 photos to ensure scalability beyond minimum requirement
        val category = createTestCategory("Very Large Category")
        val categoryId = categoryDao.insertCategory(category)

        // Create 500 photos in batches for better performance
        val batchSize = 50
        val totalPhotos = 500

        for (i in 0 until totalPhotos step batchSize) {
            val batch = (i until minOf(i + batchSize, totalPhotos)).map {
                createTestPhoto(categoryId, "batch_photo_$it.jpg")
            }
            photoDao.insertPhotos(batch)
        }

        // Verify total count
        val count = photoDao.getPhotoCountByCategory(categoryId)
        assertEquals("Should count all 500 photos", totalPhotos, count)

        // Test retrieval performance with large dataset
        val startTime = System.currentTimeMillis()
        val retrievedPhotos = photoDao.getPhotosByCategory(categoryId)
        val endTime = System.currentTimeMillis()

        assertEquals("Should retrieve all 500 photos", totalPhotos, retrievedPhotos.size)

        val executionTime = endTime - startTime
        assertTrue("Large collection query should complete reasonably fast, took ${executionTime}ms",
                  executionTime < 200)
    }

    @Test
    fun testMultipleLargeCategories() = runBlocking {
        // Test multiple categories with 100+ photos each
        val categoryCount = 5
        val photosPerCategory = 150

        val categoryIds = mutableListOf<Long>()

        // Create categories and photos
        for (i in 1..categoryCount) {
            val category = createTestCategory("Large Category $i")
            val categoryId = categoryDao.insertCategory(category)
            categoryIds.add(categoryId)

            val photos = (1..photosPerCategory).map {
                createTestPhoto(categoryId, "category${i}_photo_$it.jpg")
            }
            photoDao.insertPhotos(photos)
        }

        // Verify each category has the correct number of photos
        categoryIds.forEach { categoryId ->
            val count = photoDao.getPhotoCountByCategory(categoryId)
            assertEquals("Each category should have $photosPerCategory photos",
                        photosPerCategory, count)
        }

        // Test total photo count
        val totalCount = photoDao.getTotalPhotoCount()
        assertEquals("Total photos should be ${categoryCount * photosPerCategory}",
                    categoryCount * photosPerCategory, totalCount)

        // Test querying photos from multiple large categories
        val startTime = System.currentTimeMillis()
        val allPhotos = photoDao.getPhotosByMultipleCategories(categoryIds)
        val endTime = System.currentTimeMillis()

        assertEquals("Should retrieve all photos from all categories",
                    categoryCount * photosPerCategory, allPhotos.size)

        val executionTime = endTime - startTime
        assertTrue("Multi-category query should complete efficiently, took ${executionTime}ms",
                  executionTime < 300)
    }

    @Test
    fun testPaginationWithLargeCollection() = runBlocking {
        // Test pagination functionality with large dataset
        val category = createTestCategory("Paginated Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 250
        val pageSize = 20

        // Insert photos
        val photos = (1..totalPhotos).map {
            createTestPhoto(categoryId, "paginated_photo_$it.jpg")
        }
        photoDao.insertPhotos(photos)

        // Test pagination
        val retrievedPhotos = mutableListOf<Photo>()
        var offset = 0

        while (offset < totalPhotos) {
            val startTime = System.currentTimeMillis()
            val page = photoDao.getPhotosByCategoryPaged(categoryId, pageSize, offset)
            val endTime = System.currentTimeMillis()

            val executionTime = endTime - startTime
            assertTrue("Paginated query should be fast, took ${executionTime}ms", executionTime < 50)

            retrievedPhotos.addAll(page)
            offset += pageSize

            if (page.isEmpty()) break
        }

        assertEquals("Should retrieve all photos through pagination", totalPhotos, retrievedPhotos.size)

        // Verify no duplicates
        val uniqueIds = retrievedPhotos.map { it.id }.toSet()
        assertEquals("Should have no duplicate photos", retrievedPhotos.size, uniqueIds.size)
    }

    @Test
    fun testSearchPerformanceWithLargeCollection() = runBlocking {
        // Test search performance with large dataset
        val category = createTestCategory("Searchable Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 300

        // Create photos with different naming patterns
        val photos = (1..totalPhotos).map { i ->
            val fileName = when {
                i % 5 == 0 -> "vacation_photo_$i.jpg"
                i % 7 == 0 -> "family_photo_$i.jpg"
                i % 11 == 0 -> "work_photo_$i.jpg"
                else -> "regular_photo_$i.jpg"
            }
            createTestPhoto(categoryId, fileName)
        }
        photoDao.insertPhotos(photos)

        // Test search for vacation photos
        val startTime = System.currentTimeMillis()
        val vacationPhotos = photoDao.searchPhotosByName("%vacation%")
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("Search query should be fast even with large dataset, took ${executionTime}ms",
                  executionTime < 100)

        val expectedVacationCount = totalPhotos / 5 // Every 5th photo
        assertEquals("Should find correct number of vacation photos",
                    expectedVacationCount, vacationPhotos.size)
    }

    @Test
    fun testFileSizeQueryWithLargeCollection() = runBlocking {
        // Test file size range queries with large dataset
        val category = createTestCategory("File Size Category")
        val categoryId = categoryDao.insertCategory(category)

        // Create photos with varying file sizes
        val photos = (1..200).map { i ->
            val fileSize = when {
                i <= 50 -> 500_000L // Small files (500KB)
                i <= 100 -> 2_000_000L // Medium files (2MB)
                i <= 150 -> 8_000_000L // Large files (8MB)
                else -> 20_000_000L // Very large files (20MB)
            }
            createTestPhoto(categoryId, "sized_photo_$i.jpg", fileSize)
        }
        photoDao.insertPhotos(photos)

        // Test query for medium-sized files (1MB - 5MB)
        val startTime = System.currentTimeMillis()
        val mediumFiles = photoDao.getPhotosByFileSize(1_000_000L, 5_000_000L)
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime
        assertTrue("File size query should be fast, took ${executionTime}ms", executionTime < 50)
        assertEquals("Should find medium-sized files", 50, mediumFiles.size)
    }

    @Test
    fun testBulkOperationsWithLargeCollections() = runBlocking {
        // Test bulk operations performance
        val category = createTestCategory("Bulk Operations Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 400

        // Test bulk insert performance
        val photos = (1..totalPhotos).map {
            createTestPhoto(categoryId, "bulk_photo_$it.jpg")
        }

        val insertStartTime = System.currentTimeMillis()
        val photoIds = photoDao.insertPhotos(photos)
        val insertEndTime = System.currentTimeMillis()

        val insertTime = insertEndTime - insertStartTime
        assertTrue("Bulk insert should be efficient, took ${insertTime}ms", insertTime < 500)
        assertEquals("Should insert all photos", totalPhotos, photoIds.size)

        // Test bulk query performance
        val queryStartTime = System.currentTimeMillis()
        val retrievedPhotos = photoDao.getPhotosByCategory(categoryId)
        val queryEndTime = System.currentTimeMillis()

        val queryTime = queryEndTime - queryStartTime
        assertTrue("Bulk query should be efficient, took ${queryTime}ms", queryTime < 200)
        assertEquals("Should retrieve all photos", totalPhotos, retrievedPhotos.size)
    }

    // Helper methods
    private fun createTestCategory(name: String): Category {
        return Category(
            name = name,
            description = "Test category for large collection testing",
            dateCreated = Date(),
            photoCount = 0
        )
    }

    private fun createTestPhoto(categoryId: Long, fileName: String, fileSize: Long = 1024 * 1024): Photo {
        return Photo(
            filePath = "/test/path/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            fileSize = fileSize,
            mimeType = "image/jpeg"
        )
    }
}