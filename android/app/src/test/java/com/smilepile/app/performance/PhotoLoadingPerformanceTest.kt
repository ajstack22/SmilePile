package com.smilepile.app.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.data.database.TestSmilePileDatabase
import com.smilepile.app.data.database.dao.CategoryDao
import com.smilepile.app.data.database.dao.PhotoDao
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.data.database.entities.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Photo loading performance tests for SmilePile (F0002).
 * Tests that photo loading operations complete within the required 500ms threshold.
 */
@RunWith(AndroidJUnit4::class)
class PhotoLoadingPerformanceTest {

    private lateinit var database: TestSmilePileDatabase
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao

    companion object {
        private const val PHOTO_LOADING_THRESHOLD_MS = 500L
        private const val SINGLE_PHOTO_THRESHOLD_MS = 100L
    }

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
    fun testSinglePhotoLoadingPerformance() = runBlocking {
        // Setup test data
        val category = createTestCategory("Single Photo Category")
        val categoryId = categoryDao.insertCategory(category)
        val photo = createTestPhoto(categoryId, "single_photo.jpg")
        val photoId = photoDao.insertPhoto(photo)

        // Test photo loading performance
        val loadingTime = measureTimeMillis {
            val loadedPhoto = photoDao.getPhotoById(photoId)
            assertNotNull("Photo should be loaded successfully", loadedPhoto)
            assertEquals("Loaded photo should match original", photo.fileName, loadedPhoto!!.fileName)
        }

        assertTrue("Single photo loading should complete in <${SINGLE_PHOTO_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < SINGLE_PHOTO_THRESHOLD_MS)
    }

    @Test
    fun testMultiplePhotoLoadingPerformance() = runBlocking {
        // Setup test data - typical gallery view scenario
        val category = createTestCategory("Multiple Photos Category")
        val categoryId = categoryDao.insertCategory(category)

        val photoCount = 20 // Typical gallery page size
        val photos = (1..photoCount).map {
            createTestPhoto(categoryId, "gallery_photo_$it.jpg")
        }
        photoDao.insertPhotos(photos)

        // Test multiple photo loading performance
        val loadingTime = measureTimeMillis {
            val loadedPhotos = photoDao.getPhotosByCategory(categoryId)
            assertEquals("Should load all photos", photoCount, loadedPhotos.size)
        }

        assertTrue("Multiple photo loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testPaginatedPhotoLoadingPerformance() = runBlocking {
        // Setup large dataset
        val category = createTestCategory("Paginated Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 100
        val pageSize = 20

        val photos = (1..totalPhotos).map {
            createTestPhoto(categoryId, "paginated_photo_$it.jpg")
        }
        photoDao.insertPhotos(photos)

        // Test paginated loading performance
        val loadingTime = measureTimeMillis {
            val firstPage = photoDao.getPhotosByCategoryPaged(categoryId, pageSize, 0)
            assertEquals("Should load first page", pageSize, firstPage.size)

            val secondPage = photoDao.getPhotosByCategoryPaged(categoryId, pageSize, pageSize)
            assertEquals("Should load second page", pageSize, secondPage.size)
        }

        assertTrue("Paginated photo loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testConcurrentPhotoLoadingPerformance() = runBlocking {
        // Setup test data for concurrent loading scenario
        val categories = (1..3).map {
            val category = createTestCategory("Concurrent Category $it")
            categoryDao.insertCategory(category) to category
        }

        categories.forEach { (categoryId, _) ->
            val photos = (1..15).map { i ->
                createTestPhoto(categoryId, "concurrent_photo_${categoryId}_$i.jpg")
            }
            photoDao.insertPhotos(photos)
        }

        // Test concurrent photo loading
        val loadingTime = measureTimeMillis {
            val categoryIds = categories.map { it.first }

            // Simulate concurrent loading from different categories
            val deferredResults = categoryIds.map { categoryId ->
                async {
                    photoDao.getPhotosByCategory(categoryId)
                }
            }

            val results = deferredResults.map { it.await() }
            assertEquals("Should load from all categories", 3, results.size)
            results.forEach { photoList ->
                assertEquals("Each category should have 15 photos", 15, photoList.size)
            }
        }

        assertTrue("Concurrent photo loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testFavoritePhotosLoadingPerformance() = runBlocking {
        // Setup test data with favorite photos
        val category = createTestCategory("Favorites Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 50
        val favoritePhotos = (1..totalPhotos).map { i ->
            createTestPhoto(categoryId, "favorite_photo_$i.jpg", isFavorite = i % 3 == 0)
        }
        photoDao.insertPhotos(favoritePhotos)

        // Test favorite photos loading performance
        val loadingTime = measureTimeMillis {
            val favorites = photoDao.getFavoritePhotos()
            val expectedFavoriteCount = totalPhotos / 3
            assertEquals("Should load correct number of favorites", expectedFavoriteCount, favorites.size)
            assertTrue("All loaded photos should be favorites", favorites.all { it.isFavorite })
        }

        assertTrue("Favorite photos loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testPhotoSearchLoadingPerformance() = runBlocking {
        // Setup test data for search scenarios
        val category = createTestCategory("Search Category")
        val categoryId = categoryDao.insertCategory(category)

        val searchablePhotos = listOf(
            "vacation_beach_2023.jpg", "vacation_mountain_2023.jpg", "work_meeting_jan.jpg",
            "family_christmas.jpg", "vacation_sunset.jpg", "work_conference.jpg",
            "family_birthday.jpg", "vacation_city_trip.jpg", "work_presentation.jpg",
            "family_reunion.jpg", "vacation_road_trip.jpg", "work_team_lunch.jpg"
        ).map { fileName ->
            createTestPhoto(categoryId, fileName)
        }
        photoDao.insertPhotos(searchablePhotos)

        // Test search loading performance
        val loadingTime = measureTimeMillis {
            val vacationResults = photoDao.searchPhotosByName("%vacation%")
            val workResults = photoDao.searchPhotosByName("%work%")
            val familyResults = photoDao.searchPhotosByName("%family%")

            assertEquals("Should find vacation photos", 5, vacationResults.size)
            assertEquals("Should find work photos", 4, workResults.size)
            assertEquals("Should find family photos", 3, familyResults.size)
        }

        assertTrue("Photo search loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testPhotoMetadataLoadingPerformance() = runBlocking {
        // Test loading photos with complex metadata
        val category = createTestCategory("Metadata Category")
        val categoryId = categoryDao.insertCategory(category)

        val photosWithMetadata = (1..30).map { i ->
            createTestPhotoWithMetadata(categoryId, "metadata_photo_$i.jpg", i)
        }
        photoDao.insertPhotos(photosWithMetadata)

        // Test metadata loading performance
        val loadingTime = measureTimeMillis {
            val loadedPhotos = photoDao.getPhotosByCategory(categoryId)
            assertEquals("Should load all photos with metadata", 30, loadedPhotos.size)

            // Verify metadata is properly loaded
            loadedPhotos.forEach { photo ->
                assertNotNull("Width should be loaded", photo.width)
                assertNotNull("Height should be loaded", photo.height)
                assertTrue("Tags should be loaded", photo.tags.isNotEmpty())
            }
        }

        assertTrue("Photo metadata loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testFileSizeBasedLoadingPerformance() = runBlocking {
        // Test loading photos of different file sizes
        val category = createTestCategory("File Size Category")
        val categoryId = categoryDao.insertCategory(category)

        val photosWithVariousSizes = listOf(
            createTestPhoto(categoryId, "small_photo.jpg", fileSize = 100_000L), // 100KB
            createTestPhoto(categoryId, "medium_photo.jpg", fileSize = 1_000_000L), // 1MB
            createTestPhoto(categoryId, "large_photo.jpg", fileSize = 5_000_000L), // 5MB
            createTestPhoto(categoryId, "xlarge_photo.jpg", fileSize = 15_000_000L), // 15MB
            createTestPhoto(categoryId, "huge_photo.jpg", fileSize = 25_000_000L) // 25MB
        )
        photoDao.insertPhotos(photosWithVariousSizes)

        // Test loading performance for different file size ranges
        val loadingTime = measureTimeMillis {
            val smallFiles = photoDao.getPhotosByFileSize(0L, 500_000L)
            val mediumFiles = photoDao.getPhotosByFileSize(500_000L, 2_000_000L)
            val largeFiles = photoDao.getPhotosByFileSize(2_000_000L, 10_000_000L)
            val xlargeFiles = photoDao.getPhotosByFileSize(10_000_000L, Long.MAX_VALUE)

            assertEquals("Should find small files", 1, smallFiles.size)
            assertEquals("Should find medium files", 1, mediumFiles.size)
            assertEquals("Should find large files", 1, largeFiles.size)
            assertEquals("Should find xlarge files", 2, xlargeFiles.size)
        }

        assertTrue("File size based loading should complete in <${PHOTO_LOADING_THRESHOLD_MS}ms, took ${loadingTime}ms",
                  loadingTime < PHOTO_LOADING_THRESHOLD_MS)
    }

    @Test
    fun testStressLoadingPerformance() = runBlocking {
        // Stress test with realistic app usage scenario
        val categories = (1..5).map {
            val category = createTestCategory("Stress Category $it")
            categoryDao.insertCategory(category) to category
        }

        // Create realistic dataset
        categories.forEach { (categoryId, _) ->
            val photos = (1..40).map { i ->
                createTestPhotoWithMetadata(categoryId, "stress_photo_${categoryId}_$i.jpg", i)
            }
            photoDao.insertPhotos(photos)
        }

        // Simulate realistic app usage with multiple operations
        val totalTime = measureTimeMillis {
            val categoryIds = categories.map { it.first }

            // Load photos from multiple categories
            val allPhotos = photoDao.getPhotosByMultipleCategories(categoryIds)
            assertEquals("Should load all photos", 200, allPhotos.size)

            // Search across all photos
            val searchResults = photoDao.searchPhotosByName("%photo%")
            assertEquals("Should find all photos in search", 200, searchResults.size)

            // Get counts for each category
            categoryIds.forEach { categoryId ->
                val count = photoDao.getPhotoCountByCategory(categoryId)
                assertEquals("Each category should have 40 photos", 40, count)
            }

            // Load favorites
            val favorites = photoDao.getFavoritePhotos()
            assertTrue("Should have some favorite photos", favorites.isNotEmpty())
        }

        assertTrue("Stress loading test should complete in reasonable time, took ${totalTime}ms",
                  totalTime < 1000L) // Allow more time for comprehensive stress test
    }

    // Helper methods
    private fun createTestCategory(name: String): Category {
        return Category(
            name = name,
            description = "Test category for photo loading performance",
            dateCreated = Date(),
            photoCount = 0
        )
    }

    private fun createTestPhoto(
        categoryId: Long,
        fileName: String,
        fileSize: Long = 1024 * 1024,
        isFavorite: Boolean = false
    ): Photo {
        return Photo(
            filePath = "/test/path/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            fileSize = fileSize,
            mimeType = "image/jpeg",
            isFavorite = isFavorite
        )
    }

    private fun createTestPhotoWithMetadata(
        categoryId: Long,
        fileName: String,
        index: Int
    ): Photo {
        return Photo(
            filePath = "/test/path/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            dateTaken = Date(System.currentTimeMillis() - (index * 86400000L)), // Different dates
            fileSize = (index * 100_000L) + 500_000L, // Varying file sizes
            width = 1920 + (index * 10),
            height = 1080 + (index * 5),
            mimeType = "image/jpeg",
            tags = listOf("tag1", "tag$index", "category${categoryId}"),
            isFavorite = index % 5 == 0 // Every 5th photo is a favorite
        )
    }
}