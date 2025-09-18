package com.smilepile.app.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.data.database.TestSmilePileDatabase
import com.smilepile.app.data.database.dao.CategoryDao
import com.smilepile.app.data.database.dao.PhotoDao
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.data.database.entities.Photo
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Extended session stability tests for SmilePile.
 * Tests app stability and performance during extended usage sessions (30+ minutes).
 * These tests simulate long-running app usage patterns.
 */
@RunWith(AndroidJUnit4::class)
class SessionStabilityTest {

    private lateinit var database: TestSmilePileDatabase
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao

    companion object {
        private const val EXTENDED_SESSION_MINUTES = 1 // Reduced for unit tests, represents 30+ min session
        private const val SHORT_SESSION_SECONDS = 30 // Quick stability test
        private const val OPERATIONS_PER_MINUTE = 60 // Realistic user interaction rate
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
    fun testBasicSessionStability() = runBlocking {
        // Test basic operations over a shorter period to validate stability
        val sessionDurationMs = SHORT_SESSION_SECONDS * 1000L
        val operationsCount = AtomicInteger(0)
        val errorsCount = AtomicInteger(0)

        // Setup initial data
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val startTime = System.currentTimeMillis()

        // Simulate continuous user activity
        val job = launch {
            while (System.currentTimeMillis() - startTime < sessionDurationMs) {
                try {
                    performRandomOperation(categoryIds)
                    operationsCount.incrementAndGet()
                    delay(100) // Simulate user interaction interval
                } catch (e: Exception) {
                    errorsCount.incrementAndGet()
                    println("Error during session: ${e.message}")
                }
            }
        }

        job.join()

        val totalOperations = operationsCount.get()
        val totalErrors = errorsCount.get()

        assertTrue("Should perform multiple operations during session", totalOperations > 100)
        assertTrue("Error rate should be minimal (<5%)", totalErrors < totalOperations * 0.05)

        println("Session completed: $totalOperations operations, $totalErrors errors")
    }

    @Test
    fun testConcurrentOperationsStability() = runBlocking {
        // Test stability under concurrent operations
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val concurrentJobs = 5
        val operationsPerJob = 50
        val totalOperations = AtomicInteger(0)
        val errors = AtomicInteger(0)

        val jobs = (1..concurrentJobs).map { jobId ->
            launch {
                repeat(operationsPerJob) { operationId ->
                    try {
                        performRandomOperation(categoryIds, jobId)
                        totalOperations.incrementAndGet()
                        delay(Random.nextLong(10, 50)) // Simulate random user timing
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                        println("Error in job $jobId, operation $operationId: ${e.message}")
                    }
                }
            }
        }

        jobs.joinAll()

        assertEquals("Should complete all operations", concurrentJobs * operationsPerJob, totalOperations.get())
        assertTrue("Should have minimal errors", errors.get() < totalOperations.get() * 0.05)
    }

    @Test
    fun testMemoryStabilityDuringLongSession() = runBlocking {
        // Test memory stability over extended operations
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val operationsCount = 500 // Simulate extended usage
        val memorySnapshots = mutableListOf<Long>()

        // Get initial memory usage
        System.gc()
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        memorySnapshots.add(initialMemory)

        repeat(operationsCount) { i ->
            performRandomOperation(categoryIds)

            // Take memory snapshots periodically
            if (i % 50 == 0) {
                System.gc()
                delay(10) // Give GC time to work
                val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                memorySnapshots.add(currentMemory)
            }
        }

        // Analyze memory usage trend
        val finalMemory = memorySnapshots.last()
        val memoryIncrease = finalMemory - initialMemory
        val maxMemoryIncrease = initialMemory * 2 // Allow up to 2x initial memory

        assertTrue("Memory usage should remain stable, increase: ${memoryIncrease / 1024 / 1024}MB",
                  memoryIncrease < maxMemoryIncrease)

        println("Memory usage - Initial: ${initialMemory / 1024 / 1024}MB, Final: ${finalMemory / 1024 / 1024}MB")
    }

    @Test
    fun testDatabaseConnectionStability() = runBlocking {
        // Test database connection stability over extended period
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val testDurationMs = 30_000L // 30 seconds
        val startTime = System.currentTimeMillis()
        val operationsCount = AtomicInteger(0)
        val connectionErrors = AtomicInteger(0)

        // Continuously perform database operations
        val job = launch {
            while (System.currentTimeMillis() - startTime < testDurationMs) {
                try {
                    // Test various database operations
                    val randomCategoryId = categoryIds.random()

                    when (Random.nextInt(4)) {
                        0 -> photoDao.getPhotosByCategory(randomCategoryId)
                        1 -> photoDao.getPhotoCountByCategory(randomCategoryId)
                        2 -> categoryDao.getAllCategories()
                        3 -> photoDao.getTotalPhotoCount()
                    }

                    operationsCount.incrementAndGet()
                    delay(50) // Realistic operation frequency
                } catch (e: Exception) {
                    connectionErrors.incrementAndGet()
                    println("Database connection error: ${e.message}")
                }
            }
        }

        job.join()

        val totalOps = operationsCount.get()
        val totalErrors = connectionErrors.get()

        assertTrue("Should perform many database operations", totalOps > 300)
        assertTrue("Database connection should be stable", totalErrors < totalOps * 0.02)

        println("Database stability test: $totalOps operations, $totalErrors connection errors")
    }

    @Test
    fun testPerformanceDegradationOverTime() = runBlocking {
        // Test that performance doesn't degrade significantly over time
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val performanceMeasurements = mutableListOf<Long>()
        val batchSize = 50

        // Measure performance over multiple batches
        repeat(10) { batchIndex ->
            val batchTime = measureTimeMillis {
                repeat(batchSize) {
                    performRandomOperation(categoryIds)
                }
            }
            performanceMeasurements.add(batchTime)
            delay(100) // Brief pause between batches
        }

        // Analyze performance trend
        val firstBatchTime = performanceMeasurements.first()
        val lastBatchTime = performanceMeasurements.last()
        val averageTime = performanceMeasurements.average()

        // Performance shouldn't degrade by more than 50%
        val maxAllowedDegradation = firstBatchTime * 1.5

        assertTrue("Performance shouldn't degrade significantly over time. " +
                  "First batch: ${firstBatchTime}ms, Last batch: ${lastBatchTime}ms",
                  lastBatchTime < maxAllowedDegradation)

        println("Performance over time - Average: ${averageTime.toInt()}ms, " +
                "First: ${firstBatchTime}ms, Last: ${lastBatchTime}ms")
    }

    @Test
    fun testDataIntegrityDuringLongSession() = runBlocking {
        // Test data integrity during extended session
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        // Record initial state
        val initialPhotoCount = photoDao.getTotalPhotoCount()
        val initialCategoryCount = categoryDao.getCategoryCount()

        // Perform operations that might affect data integrity
        repeat(200) {
            performDataModificationOperation(categoryIds)
        }

        // Verify data integrity
        val finalPhotoCount = photoDao.getTotalPhotoCount()
        val finalCategoryCount = categoryDao.getCategoryCount()

        assertTrue("Photo count should be reasonable", finalPhotoCount >= initialPhotoCount)
        assertEquals("Category count should remain stable", initialCategoryCount, finalCategoryCount)

        // Verify no data corruption
        categoryIds.forEach { categoryId ->
            val photos = photoDao.getPhotosByCategory(categoryId)
            val count = photoDao.getPhotoCountByCategory(categoryId)
            assertEquals("Photo count should match actual photos", photos.size, count)
        }
    }

    @Test
    fun testResourceCleanupStability() = runBlocking {
        // Test that resources are properly cleaned up during extended usage
        val categories = setupTestData()
        val categoryIds = categories.map { it.first }

        val iterations = 100
        val photosPerIteration = 10

        repeat(iterations) { iteration ->
            // Add photos
            val photos = (1..photosPerIteration).map {
                createTestPhoto(categoryIds.random(), "temp_photo_${iteration}_$it.jpg")
            }
            val photoIds = photoDao.insertPhotos(photos)

            // Use photos (simulate viewing, searching, etc.)
            photoDao.getPhotosByCategory(categoryIds.random())
            photoDao.searchPhotosByName("%temp%")

            // Clean up photos (simulate deletion)
            photoIds.forEach { photoId ->
                photoDao.markPhotoAsDeleted(photoId)
            }

            // Periodic cleanup
            if (iteration % 10 == 0) {
                photoDao.permanentlyDeleteMarkedPhotos()
                System.gc() // Force garbage collection
            }
        }

        // Final cleanup
        photoDao.permanentlyDeleteMarkedPhotos()

        // Verify cleanup was effective
        val remainingPhotos = photoDao.searchPhotosByName("%temp%")
        assertEquals("Temporary photos should be cleaned up", 0, remainingPhotos.size)
    }

    // Helper methods
    private suspend fun setupTestData(): List<Pair<Long, Category>> {
        val categories = (1..5).map { i ->
            val category = createTestCategory("Session Category $i")
            val categoryId = categoryDao.insertCategory(category)

            // Add initial photos to each category
            val photos = (1..20).map { j ->
                createTestPhoto(categoryId, "initial_photo_${i}_$j.jpg")
            }
            photoDao.insertPhotos(photos)

            categoryId to category
        }
        return categories
    }

    private suspend fun performRandomOperation(categoryIds: List<Long>, jobId: Int = 0) {
        val randomCategoryId = categoryIds.random()

        when (Random.nextInt(6)) {
            0 -> {
                // Query photos by category
                photoDao.getPhotosByCategory(randomCategoryId)
            }
            1 -> {
                // Search photos
                val searchTerms = listOf("photo", "initial", "test", "session")
                photoDao.searchPhotosByName("%${searchTerms.random()}%")
            }
            2 -> {
                // Get photo count
                photoDao.getPhotoCountByCategory(randomCategoryId)
            }
            3 -> {
                // Get all categories
                categoryDao.getAllCategories()
            }
            4 -> {
                // Get favorite photos
                photoDao.getFavoritePhotos()
            }
            5 -> {
                // Add a photo (simulate photo taking)
                val photo = createTestPhoto(randomCategoryId, "session_photo_${jobId}_${Random.nextInt()}.jpg")
                photoDao.insertPhoto(photo)
            }
        }
    }

    private suspend fun performDataModificationOperation(categoryIds: List<Long>) {
        val randomCategoryId = categoryIds.random()

        when (Random.nextInt(3)) {
            0 -> {
                // Add a photo
                val photo = createTestPhoto(randomCategoryId, "data_photo_${Random.nextInt()}.jpg")
                photoDao.insertPhoto(photo)
            }
            1 -> {
                // Mark a photo as favorite
                val photos = photoDao.getPhotosByCategory(randomCategoryId)
                if (photos.isNotEmpty()) {
                    val randomPhoto = photos.random()
                    val updatedPhoto = randomPhoto.copy(isFavorite = !randomPhoto.isFavorite)
                    photoDao.updatePhoto(updatedPhoto)
                }
            }
            2 -> {
                // Update category photo count (simulate maintenance operation)
                val actualCount = photoDao.getPhotoCountByCategory(randomCategoryId)
                categoryDao.updatePhotoCount(randomCategoryId, actualCount)
            }
        }
    }

    private fun createTestCategory(name: String): Category {
        return Category(
            name = name,
            description = "Test category for session stability testing",
            dateCreated = Date(),
            photoCount = 0
        )
    }

    private fun createTestPhoto(categoryId: Long, fileName: String): Photo {
        return Photo(
            filePath = "/test/session/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            fileSize = Random.nextLong(100_000, 5_000_000),
            mimeType = "image/jpeg",
            isFavorite = Random.nextBoolean()
        )
    }
}