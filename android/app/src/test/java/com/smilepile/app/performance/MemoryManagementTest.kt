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
import java.lang.ref.WeakReference
import java.util.*
import kotlin.random.Random

/**
 * Memory management tests for SmilePile.
 * Tests memory usage and leak prevention when handling large photo collections.
 */
@RunWith(AndroidJUnit4::class)
class MemoryManagementTest {

    private lateinit var database: TestSmilePileDatabase
    private lateinit var photoDao: PhotoDao
    private lateinit var categoryDao: CategoryDao

    companion object {
        private const val LARGE_COLLECTION_SIZE = 500
        private const val MEMORY_INCREASE_THRESHOLD = 0.5 // 50% increase threshold
        private const val GC_WAIT_TIME = 100L // Time to wait for GC
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestSmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        photoDao = database.photoDao()
        categoryDao = database.categoryDao()

        // Force initial garbage collection
        System.gc()
        Thread.sleep(GC_WAIT_TIME)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testMemoryUsageWithLargePhotoCollection() = runBlocking {
        // Measure baseline memory
        val baselineMemory = getCurrentMemoryUsage()

        // Create large photo collection
        val category = createTestCategory("Large Memory Category")
        val categoryId = categoryDao.insertCategory(category)

        val photos = (1..LARGE_COLLECTION_SIZE).map { i ->
            createTestPhoto(categoryId, "memory_photo_$i.jpg")
        }

        // Insert photos and measure memory
        photoDao.insertPhotos(photos)
        val afterInsertMemory = getCurrentMemoryUsage()

        // Query photos multiple times to test memory accumulation
        repeat(10) {
            photoDao.getPhotosByCategory(categoryId)
        }

        val afterQueriesMemory = getCurrentMemoryUsage()

        // Force garbage collection and re-measure
        forceGarbageCollection()
        val afterGCMemory = getCurrentMemoryUsage()

        // Analyze memory usage
        val insertIncreaseRatio = (afterInsertMemory - baselineMemory).toDouble() / baselineMemory
        val queriesIncreaseRatio = (afterQueriesMemory - afterInsertMemory).toDouble() / afterInsertMemory
        val gcEffectivenessRatio = (afterQueriesMemory - afterGCMemory).toDouble() / afterQueriesMemory

        assertTrue("Memory increase after insert should be reasonable (${insertIncreaseRatio * 100}%)",
                  insertIncreaseRatio < MEMORY_INCREASE_THRESHOLD)

        assertTrue("Memory increase after queries should be minimal (${queriesIncreaseRatio * 100}%)",
                  queriesIncreaseRatio < 0.2) // 20% threshold for query operations

        assertTrue("Garbage collection should be effective (${gcEffectivenessRatio * 100}% reclaimed)",
                  gcEffectivenessRatio > 0.1) // At least 10% should be reclaimed

        println("Memory analysis - Baseline: ${baselineMemory / 1024}KB, " +
                "After insert: ${afterInsertMemory / 1024}KB, " +
                "After queries: ${afterQueriesMemory / 1024}KB, " +
                "After GC: ${afterGCMemory / 1024}KB")
    }

    @Test
    fun testMemoryLeakPrevention() = runBlocking {
        val weakReferences = mutableListOf<WeakReference<List<Photo>>>()

        // Create and release multiple photo collections
        repeat(10) { iteration ->
            val category = createTestCategory("Leak Test Category $iteration")
            val categoryId = categoryDao.insertCategory(category)

            val photos = (1..100).map { i ->
                createTestPhoto(categoryId, "leak_test_photo_${iteration}_$i.jpg")
            }
            photoDao.insertPhotos(photos)

            // Query photos and store weak reference
            val queriedPhotos = photoDao.getPhotosByCategory(categoryId)
            weakReferences.add(WeakReference(queriedPhotos))

            // Clear the reference
            @Suppress("UNUSED_VALUE")
            val clearReference = null
        }

        // Force garbage collection
        forceGarbageCollection()

        // Check if objects were properly collected
        val stillReferencedCount = weakReferences.count { it.get() != null }
        val releasedCount = weakReferences.size - stillReferencedCount

        assertTrue("Most objects should be garbage collected, " +
                  "Released: $releasedCount, Still referenced: $stillReferencedCount",
                  releasedCount >= weakReferences.size * 0.7) // At least 70% should be collected
    }

    @Test
    fun testMemoryUsageWithConcurrentOperations() = runBlocking {
        val baselineMemory = getCurrentMemoryUsage()

        // Setup test data
        val categories = (1..5).map { i ->
            val category = createTestCategory("Concurrent Category $i")
            val categoryId = categoryDao.insertCategory(category)

            val photos = (1..100).map { j ->
                createTestPhoto(categoryId, "concurrent_photo_${i}_$j.jpg")
            }
            photoDao.insertPhotos(photos)
            categoryId
        }

        // Perform concurrent operations
        val jobs = (1..10).map { jobId ->
            launch {
                repeat(20) {
                    val randomCategory = categories.random()
                    when (Random.nextInt(3)) {
                        0 -> photoDao.getPhotosByCategory(randomCategory)
                        1 -> photoDao.getPhotoCountByCategory(randomCategory)
                        2 -> photoDao.searchPhotosByName("%photo%")
                    }
                    delay(10) // Small delay to simulate real usage
                }
            }
        }

        jobs.joinAll()

        val afterConcurrentMemory = getCurrentMemoryUsage()
        forceGarbageCollection()
        val afterGCMemory = getCurrentMemoryUsage()

        val memoryIncreaseRatio = (afterConcurrentMemory - baselineMemory).toDouble() / baselineMemory
        val gcEffectiveness = (afterConcurrentMemory - afterGCMemory).toDouble() / afterConcurrentMemory

        assertTrue("Memory increase should be reasonable during concurrent operations (${memoryIncreaseRatio * 100}%)",
                  memoryIncreaseRatio < 1.0) // Less than 100% increase

        assertTrue("Memory should be reclaimable after concurrent operations (${gcEffectiveness * 100}% reclaimed)",
                  gcEffectiveness > 0.05) // At least 5% should be reclaimed

        println("Concurrent operations memory - Baseline: ${baselineMemory / 1024}KB, " +
                "After operations: ${afterConcurrentMemory / 1024}KB, " +
                "After GC: ${afterGCMemory / 1024}KB")
    }

    @Test
    fun testMemoryUsageWithPagination() = runBlocking {
        // Test memory usage when using pagination vs loading all at once
        val category = createTestCategory("Pagination Category")
        val categoryId = categoryDao.insertCategory(category)

        val totalPhotos = 1000
        val pageSize = 50

        // Create large dataset
        val photos = (1..totalPhotos).map { i ->
            createTestPhoto(categoryId, "pagination_photo_$i.jpg")
        }
        photoDao.insertPhotos(photos)

        // Test 1: Load all photos at once
        val beforeLoadAllMemory = getCurrentMemoryUsage()
        val allPhotos = photoDao.getPhotosByCategory(categoryId)
        val afterLoadAllMemory = getCurrentMemoryUsage()

        // Test 2: Load photos using pagination
        forceGarbageCollection()
        val beforePaginationMemory = getCurrentMemoryUsage()

        var loadedPages = 0
        var offset = 0
        while (offset < totalPhotos) {
            val page = photoDao.getPhotosByCategoryPaged(categoryId, pageSize, offset)
            if (page.isEmpty()) break
            loadedPages++
            offset += pageSize
            // Simulate processing and releasing page data
            delay(1)
        }

        val afterPaginationMemory = getCurrentMemoryUsage()

        // Compare memory usage
        val loadAllIncrease = afterLoadAllMemory - beforeLoadAllMemory
        val paginationIncrease = afterPaginationMemory - beforePaginationMemory

        assertTrue("Pagination should use less memory than loading all at once. " +
                  "Load all: ${loadAllIncrease / 1024}KB, Pagination: ${paginationIncrease / 1024}KB",
                  paginationIncrease < loadAllIncrease * 0.8) // Pagination should use at least 20% less memory

        assertEquals("Should load all photos through pagination", totalPhotos, allPhotos.size)
        assertTrue("Should have loaded multiple pages", loadedPages > 1)
    }

    @Test
    fun testMemoryCleanupAfterBulkOperations() = runBlocking {
        val initialMemory = getCurrentMemoryUsage()

        // Perform multiple bulk operations
        repeat(5) { bulkIndex ->
            val category = createTestCategory("Bulk Category $bulkIndex")
            val categoryId = categoryDao.insertCategory(category)

            // Large bulk insert
            val photos = (1..200).map { i ->
                createTestPhoto(categoryId, "bulk_photo_${bulkIndex}_$i.jpg")
            }
            photoDao.insertPhotos(photos)

            // Bulk query operations
            repeat(10) {
                photoDao.getPhotosByCategory(categoryId)
                photoDao.getPhotoCountByCategory(categoryId)
            }

            // Simulate bulk deletion
            val insertedPhotos = photoDao.getPhotosByCategory(categoryId)
            insertedPhotos.forEach { photo ->
                photoDao.markPhotoAsDeleted(photo.id)
            }
            photoDao.permanentlyDeleteMarkedPhotos()
        }

        // Force cleanup
        forceGarbageCollection()
        val finalMemory = getCurrentMemoryUsage()

        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseRatio = memoryIncrease.toDouble() / initialMemory

        assertTrue("Memory should return close to baseline after bulk operations cleanup. " +
                  "Increase: ${memoryIncrease / 1024}KB (${memoryIncreaseRatio * 100}%)",
                  memoryIncreaseRatio < 0.3) // Less than 30% increase should remain

        println("Bulk operations cleanup - Initial: ${initialMemory / 1024}KB, " +
                "Final: ${finalMemory / 1024}KB, Increase: ${memoryIncrease / 1024}KB")
    }

    @Test
    fun testMemoryUsageWithLargeMetadata() = runBlocking {
        val baselineMemory = getCurrentMemoryUsage()

        // Create photos with large metadata (tags, descriptions, etc.)
        val category = createTestCategory("Large Metadata Category")
        val categoryId = categoryDao.insertCategory(category)

        val photosWithLargeMetadata = (1..200).map { i ->
            createTestPhotoWithLargeMetadata(categoryId, "metadata_photo_$i.jpg", i)
        }

        photoDao.insertPhotos(photosWithLargeMetadata)
        val afterInsertMemory = getCurrentMemoryUsage()

        // Query photos multiple times to test metadata memory usage
        repeat(20) {
            val photos = photoDao.getPhotosByCategory(categoryId)
            // Simulate processing metadata
            photos.forEach { photo ->
                photo.tags.forEach { tag ->
                    @Suppress("UNUSED_VARIABLE")
                    val processedTag = tag.uppercase()
                }
            }
        }

        val afterQueriesMemory = getCurrentMemoryUsage()
        forceGarbageCollection()
        val afterGCMemory = getCurrentMemoryUsage()

        val totalIncrease = afterQueriesMemory - baselineMemory
        val increaseRatio = totalIncrease.toDouble() / baselineMemory

        assertTrue("Memory usage with large metadata should be manageable (${increaseRatio * 100}%)",
                  increaseRatio < 1.0) // Less than 100% increase

        val gcEffectiveness = (afterQueriesMemory - afterGCMemory).toDouble() / afterQueriesMemory
        assertTrue("Metadata should be garbage collectible (${gcEffectiveness * 100}% reclaimed)",
                  gcEffectiveness > 0.1)

        println("Large metadata test - Baseline: ${baselineMemory / 1024}KB, " +
                "After queries: ${afterQueriesMemory / 1024}KB, " +
                "After GC: ${afterGCMemory / 1024}KB")
    }

    @Test
    fun testMemoryStabilityUnderStress() = runBlocking {
        val memorySnapshots = mutableListOf<Long>()
        val initialMemory = getCurrentMemoryUsage()
        memorySnapshots.add(initialMemory)

        // Stress test with rapid operations
        repeat(100) { iteration ->
            // Create temporary data
            val category = createTestCategory("Stress Category $iteration")
            val categoryId = categoryDao.insertCategory(category)

            val photos = (1..50).map { i ->
                createTestPhoto(categoryId, "stress_photo_${iteration}_$i.jpg")
            }
            photoDao.insertPhotos(photos)

            // Perform multiple operations
            repeat(10) {
                photoDao.getPhotosByCategory(categoryId)
                photoDao.getPhotoCountByCategory(categoryId)
                photoDao.searchPhotosByName("%stress%")
            }

            // Clean up
            val insertedPhotos = photoDao.getPhotosByCategory(categoryId)
            insertedPhotos.forEach { photo ->
                photoDao.markPhotoAsDeleted(photo.id)
            }
            photoDao.permanentlyDeleteMarkedPhotos()

            // Take memory snapshot every 10 iterations
            if (iteration % 10 == 0) {
                forceGarbageCollection()
                val currentMemory = getCurrentMemoryUsage()
                memorySnapshots.add(currentMemory)
            }
        }

        // Analyze memory stability
        val finalMemory = memorySnapshots.last()
        val maxMemory = memorySnapshots.maxOrNull() ?: 0L
        val avgMemory = memorySnapshots.average().toLong()

        val maxIncreaseRatio = (maxMemory - initialMemory).toDouble() / initialMemory
        val finalIncreaseRatio = (finalMemory - initialMemory).toDouble() / initialMemory

        assertTrue("Maximum memory increase should be bounded (${maxIncreaseRatio * 100}%)",
                  maxIncreaseRatio < 2.0) // Less than 200% increase

        assertTrue("Final memory should return close to baseline (${finalIncreaseRatio * 100}%)",
                  finalIncreaseRatio < 0.5) // Less than 50% increase remains

        println("Stress test memory - Initial: ${initialMemory / 1024}KB, " +
                "Max: ${maxMemory / 1024}KB, " +
                "Avg: ${avgMemory / 1024}KB, " +
                "Final: ${finalMemory / 1024}KB")
    }

    // Helper methods
    private fun getCurrentMemoryUsage(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    private fun forceGarbageCollection() {
        repeat(3) {
            System.gc()
            Thread.sleep(GC_WAIT_TIME)
        }
    }

    private fun createTestCategory(name: String): Category {
        return Category(
            name = name,
            description = "Test category for memory management testing",
            dateCreated = Date(),
            photoCount = 0
        )
    }

    private fun createTestPhoto(categoryId: Long, fileName: String): Photo {
        return Photo(
            filePath = "/test/memory/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            fileSize = Random.nextLong(100_000, 2_000_000),
            mimeType = "image/jpeg",
            tags = listOf("memory", "test"),
            isFavorite = Random.nextBoolean()
        )
    }

    private fun createTestPhotoWithLargeMetadata(categoryId: Long, fileName: String, index: Int): Photo {
        // Create large metadata to test memory usage
        val largeTags = (1..20).map { "tag_${index}_$it" }
        val largeDescription = "Large description for photo $index. ".repeat(10)

        return Photo(
            filePath = "/test/memory/large/$fileName",
            fileName = fileName,
            categoryId = categoryId,
            dateCreated = Date(),
            dateTaken = Date(System.currentTimeMillis() - (index * 86400000L)),
            fileSize = Random.nextLong(1_000_000, 10_000_000),
            width = 3840 + index,
            height = 2160 + index,
            mimeType = "image/jpeg",
            tags = largeTags,
            isFavorite = index % 3 == 0
        )
    }
}