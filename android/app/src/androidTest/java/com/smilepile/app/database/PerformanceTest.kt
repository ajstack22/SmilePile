package com.smilepile.app.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.entities.PhotoEntity
import com.smilepile.app.repository.CategoryRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Performance benchmark tests for Room database operations to ensure
 * query response times meet requirements (< 50ms) and operations are efficient.
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: SmilePileDatabase
    private lateinit var testScope: CoroutineScope

    @Before
    fun createDb() {
        testScope = CoroutineScope(Dispatchers.IO)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // Performance thresholds (in milliseconds)
    companion object {
        const val QUERY_THRESHOLD_MS = 50L
        const val INSERT_BATCH_THRESHOLD_MS = 200L
        const val LARGE_QUERY_THRESHOLD_MS = 100L
        const val TRANSACTION_THRESHOLD_MS = 150L
    }

    // Sample data generators
    private fun createSampleCategories(count: Int): List<CategoryEntity> {
        return (1..count).map { index ->
            CategoryEntity(
                id = "perf_cat_$index",
                name = "performance_category_$index",
                displayName = "Performance Category $index",
                coverImagePath = null,
                description = "Performance test category $index",
                photoCount = 0,
                position = index,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    private fun createSamplePhotos(categoryId: String, count: Int): List<PhotoEntity> {
        return (1..count).map { index ->
            PhotoEntity(
                id = "${categoryId}_photo_$index",
                path = "performance/photo_$index.png",
                name = "Performance Photo $index",
                categoryId = categoryId,
                position = index,
                dateAdded = System.currentTimeMillis(),
                isFromAssets = true
            )
        }
    }

    // ================== Query Performance Tests ==================

    @Test
    fun getCategoriesPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert test data
        val categoryDao = db.categoryDao()
        val testCategories = createSampleCategories(50)
        categoryDao.insertCategories(testCategories)

        // When: Measure query performance
        val queryTime = measureTimeMillis {
            val categories = categoryDao.getAllCategories().first()
            assertEquals("Should retrieve all categories", 50, categories.size)
        }

        // Then: Should meet performance threshold
        assertTrue("Query should complete within $QUERY_THRESHOLD_MS ms (actual: ${queryTime}ms)",
            queryTime < QUERY_THRESHOLD_MS)
    }

    @Test
    fun getPhotosForCategoryPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert category with many photos
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val category = createSampleCategories(1).first()
        categoryDao.insertCategory(category)

        val photos = createSamplePhotos(category.id, 100)
        photoDao.insertPhotos(photos)

        // When: Measure query performance for specific category
        val queryTime = measureTimeMillis {
            val categoryPhotos = photoDao.getPhotosForCategory(category.id).first()
            assertEquals("Should retrieve all photos for category", 100, categoryPhotos.size)
        }

        // Then: Should meet performance threshold
        assertTrue("Category photos query should complete within $QUERY_THRESHOLD_MS ms (actual: ${queryTime}ms)",
            queryTime < QUERY_THRESHOLD_MS)
    }

    @Test
    fun getCategoriesWithPhotosPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert categories with photos
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(10)
        categoryDao.insertCategories(categories)

        // Add 20 photos per category
        for (category in categories) {
            val photos = createSamplePhotos(category.id, 20)
            photoDao.insertPhotos(photos)
        }

        // When: Measure complex relationship query performance
        val queryTime = measureTimeMillis {
            val categoriesWithPhotos = categoryDao.getAllCategoriesWithPhotos().first()
            assertEquals("Should retrieve all categories with photos", 10, categoriesWithPhotos.size)

            // Verify each category has photos
            for (categoryWithPhotos in categoriesWithPhotos) {
                assertEquals("Each category should have 20 photos", 20, categoryWithPhotos.photos.size)
            }
        }

        // Then: Should meet performance threshold (slightly higher for complex queries)
        assertTrue("Complex relationship query should complete within $LARGE_QUERY_THRESHOLD_MS ms (actual: ${queryTime}ms)",
            queryTime < LARGE_QUERY_THRESHOLD_MS)
    }

    @Test
    fun searchPhotosPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert large dataset for search
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(5)
        categoryDao.insertCategories(categories)

        // Insert 1000 photos across categories
        for (category in categories) {
            val photos = createSamplePhotos(category.id, 200)
            photoDao.insertPhotos(photos)
        }

        // When: Measure search performance
        val searchTime = measureTimeMillis {
            val searchResults = photoDao.searchPhotosByName("Performance").first()
            assertTrue("Search should find matching photos", searchResults.isNotEmpty())
        }

        // Then: Should meet performance threshold
        assertTrue("Search query should complete within $QUERY_THRESHOLD_MS ms (actual: ${searchTime}ms)",
            searchTime < QUERY_THRESHOLD_MS)
    }

    // ================== Insert Performance Tests ==================

    @Test
    fun batchInsertCategoriesPerformanceShouldMeetThreshold() = runTest {
        // Given: Prepare large batch of categories
        val categoryDao = db.categoryDao()
        val categories = createSampleCategories(100)

        // When: Measure batch insert performance
        val insertTime = measureTimeMillis {
            val insertIds = categoryDao.insertCategories(categories)
            assertEquals("Should insert all categories", 100, insertIds.size)
        }

        // Then: Should meet performance threshold
        assertTrue("Batch insert should complete within $INSERT_BATCH_THRESHOLD_MS ms (actual: ${insertTime}ms)",
            insertTime < INSERT_BATCH_THRESHOLD_MS)
    }

    @Test
    fun batchInsertPhotosPerformanceShouldMeetThreshold() = runTest {
        // Given: Prepare category and large batch of photos
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val category = createSampleCategories(1).first()
        categoryDao.insertCategory(category)

        val photos = createSamplePhotos(category.id, 500)

        // When: Measure batch insert performance
        val insertTime = measureTimeMillis {
            val insertIds = photoDao.insertPhotos(photos)
            assertEquals("Should insert all photos", 500, insertIds.size)
        }

        // Then: Should meet performance threshold
        assertTrue("Photo batch insert should complete within $INSERT_BATCH_THRESHOLD_MS ms (actual: ${insertTime}ms)",
            insertTime < INSERT_BATCH_THRESHOLD_MS)
    }

    @Test
    fun individualInsertPerformanceShouldBeEfficient() = runTest {
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val category = createSampleCategories(1).first()
        categoryDao.insertCategory(category)

        // When: Measure individual insert performance (10 inserts)
        val totalTime = measureTimeMillis {
            repeat(10) { index ->
                val photo = createSamplePhotos(category.id, 1).first().copy(id = "individual_$index")
                val insertId = photoDao.insertPhoto(photo)
                assertTrue("Insert should succeed", insertId > 0)
            }
        }

        val avgTime = totalTime / 10.0

        // Then: Average individual insert should be efficient
        assertTrue("Average individual insert should be under 10ms (actual: ${avgTime}ms)", avgTime < 10.0)
    }

    // ================== Update Performance Tests ==================

    @Test
    fun updateOperationsPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert data to update
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(50)
        categoryDao.insertCategories(categories)

        val category = categories.first()
        val photos = createSamplePhotos(category.id, 100)
        photoDao.insertPhotos(photos)

        // When: Measure update performance
        val updateTime = measureTimeMillis {
            // Update category
            val updatedCategory = category.copy(description = "Updated description", photoCount = 100)
            val categoryUpdateCount = categoryDao.updateCategory(updatedCategory)
            assertEquals("Category update should affect 1 row", 1, categoryUpdateCount)

            // Update multiple photos
            val updatedPhotos = photos.take(10).map { it.copy(name = "Updated ${it.name}") }
            for (photo in updatedPhotos) {
                val photoUpdateCount = photoDao.updatePhoto(photo)
                assertEquals("Photo update should affect 1 row", 1, photoUpdateCount)
            }
        }

        // Then: Should meet performance threshold
        assertTrue("Update operations should complete within $QUERY_THRESHOLD_MS ms (actual: ${updateTime}ms)",
            updateTime < QUERY_THRESHOLD_MS)
    }

    @Test
    fun positionUpdatesPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert categories and photos
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(20)
        categoryDao.insertCategories(categories)

        val category = categories.first()
        val photos = createSamplePhotos(category.id, 50)
        photoDao.insertPhotos(photos)

        // When: Measure position update performance (simulating reordering)
        val reorderTime = measureTimeMillis {
            // Update category positions (reverse order)
            val reorderedCategories = categories.mapIndexed { index, cat ->
                cat.copy(position = categories.size - index - 1)
            }
            val categoryUpdateIds = categoryDao.updateCategoryPositions(reorderedCategories)
            assertEquals("Should update all category positions", 20, categoryUpdateIds.size)

            // Update photo positions within category
            val reorderedPhotos = photos.mapIndexed { index, photo ->
                photo.copy(position = photos.size - index - 1)
            }
            val photoUpdateIds = photoDao.updatePhotoPositions(reorderedPhotos)
            assertEquals("Should update all photo positions", 50, photoUpdateIds.size)
        }

        // Then: Should meet performance threshold
        assertTrue("Position updates should complete within $TRANSACTION_THRESHOLD_MS ms (actual: ${reorderTime}ms)",
            reorderTime < TRANSACTION_THRESHOLD_MS)
    }

    // ================== Delete Performance Tests ==================

    @Test
    fun deleteOperationsPerformanceShouldMeetThreshold() = runTest {
        // Given: Insert data to delete
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(20)
        categoryDao.insertCategories(categories)

        for (category in categories) {
            val photos = createSamplePhotos(category.id, 25)
            photoDao.insertPhotos(photos)
        }

        // When: Measure delete performance
        val deleteTime = measureTimeMillis {
            // Delete individual photos
            repeat(10) { index ->
                val photoId = "${categories[0].id}_photo_${index + 1}"
                val photo = photoDao.getPhotoById(photoId)
                if (photo != null) {
                    val deleteCount = photoDao.deletePhoto(photo)
                    assertEquals("Photo delete should affect 1 row", 1, deleteCount)
                }
            }

            // Delete photos for entire category
            val deleteCount = photoDao.deletePhotosForCategory(categories[1].id)
            assertEquals("Should delete all photos for category", 25, deleteCount)

            // Delete category (cascade should handle photos)
            val categoryDeleteCount = categoryDao.deleteCategory(categories[2])
            assertEquals("Category delete should affect 1 row", 1, categoryDeleteCount)
        }

        // Then: Should meet performance threshold
        assertTrue("Delete operations should complete within $QUERY_THRESHOLD_MS ms (actual: ${deleteTime}ms)",
            deleteTime < QUERY_THRESHOLD_MS)
    }

    // ================== Repository-Level Performance Tests ==================

    @Test
    fun repositoryOperationsPerformanceShouldMeetThreshold() = runTest {
        // Given: Create repository
        val repository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        // When: Measure repository initialization performance
        val initTime = measureTimeMillis {
            repository.initializeSampleData()
        }

        // Then: Initialization should be efficient
        assertTrue("Repository initialization should complete within 500ms (actual: ${initTime}ms)",
            initTime < 500)

        // When: Measure repository query performance
        val queryTime = measureTimeMillis {
            val categories = repository.getCategories()
            val photos = repository.getAllPhotos()
            val categoriesWithPhotos = repository.getAllCategoriesWithPhotos()

            assertTrue("Should have categories", categories.isNotEmpty())
            assertTrue("Should have photos", photos.isNotEmpty())
            assertTrue("Should have categories with photos", categoriesWithPhotos.isNotEmpty())
        }

        // Then: Repository queries should be efficient
        assertTrue("Repository queries should complete within $QUERY_THRESHOLD_MS ms (actual: ${queryTime}ms)",
            queryTime < QUERY_THRESHOLD_MS)
    }

    @Test
    fun repositoryFlowPerformanceShouldMeetThreshold() = runTest {
        // Given: Create repository with data
        val repository = CategoryRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )
        repository.initializeSampleData()

        // When: Measure flow query performance
        val flowTime = measureTimeMillis {
            val categoriesFlow = repository.getCategoriesFlow().first()
            val categoriesWithPhotosFlow = repository.getAllCategoriesWithPhotosFlow().first()

            assertTrue("Categories flow should have data", categoriesFlow.isNotEmpty())
            assertTrue("Categories with photos flow should have data", categoriesWithPhotosFlow.isNotEmpty())
        }

        // Then: Flow queries should be efficient
        assertTrue("Repository flow queries should complete within $QUERY_THRESHOLD_MS ms (actual: ${flowTime}ms)",
            flowTime < QUERY_THRESHOLD_MS)
    }

    // ================== Stress Tests ==================

    @Test
    fun largeDatasetPerformanceShouldRemainAcceptable() = runTest {
        // Given: Insert very large dataset
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(100)
        val insertCategoriesTime = measureTimeMillis {
            categoryDao.insertCategories(categories)
        }

        // Insert 50 photos per category (5000 total photos)
        val insertPhotosTime = measureTimeMillis {
            for (category in categories) {
                val photos = createSamplePhotos(category.id, 50)
                photoDao.insertPhotos(photos)
            }
        }

        // When: Measure query performance with large dataset
        val queryTime = measureTimeMillis {
            val allCategories = categoryDao.getAllCategories().first()
            val allPhotos = photoDao.getAllPhotos().first()

            assertEquals("Should have all categories", 100, allCategories.size)
            assertEquals("Should have all photos", 5000, allPhotos.size)
        }

        // Then: Performance should remain acceptable even with large dataset
        assertTrue("Large dataset category insert should complete within 1000ms (actual: ${insertCategoriesTime}ms)",
            insertCategoriesTime < 1000)
        assertTrue("Large dataset photo insert should complete within 5000ms (actual: ${insertPhotosTime}ms)",
            insertPhotosTime < 5000)
        assertTrue("Large dataset query should complete within 200ms (actual: ${queryTime}ms)",
            queryTime < 200)
    }

    @Test
    fun concurrentAccessPerformanceShouldMaintainThreshold() = runTest {
        // Given: Prepare concurrent operations
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(10)
        categoryDao.insertCategories(categories)

        // When: Measure concurrent access performance
        val concurrentTime = measureTimeMillis {
            val jobs = (1..5).map { jobIndex ->
                kotlinx.coroutines.async {
                    // Each job inserts photos for 2 categories
                    for (catIndex in 0..1) {
                        val categoryId = categories[jobIndex * 2 + catIndex].id
                        val photos = createSamplePhotos(categoryId, 20)
                        photoDao.insertPhotos(photos)
                    }
                }
            }

            // Wait for all concurrent operations
            jobs.forEach { it.await() }
        }

        // Verify data integrity after concurrent operations
        val totalPhotos = photoDao.getTotalPhotoCount()
        assertEquals("Should have inserted all photos concurrently", 200, totalPhotos)

        // Then: Concurrent operations should complete within reasonable time
        assertTrue("Concurrent operations should complete within 1000ms (actual: ${concurrentTime}ms)",
            concurrentTime < 1000)
    }

    // ================== Memory Usage Tests ==================

    @Test
    fun memoryUsageShouldRemainStableUnderLoad() = runTest {
        // Given: Track initial memory state
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        // When: Perform memory-intensive operations
        repeat(10) { iteration ->
            // Create and insert data
            val categories = createSampleCategories(50)
            categoryDao.insertCategories(categories)

            for (category in categories) {
                val photos = createSamplePhotos(category.id, 20)
                photoDao.insertPhotos(photos)
            }

            // Read all data
            val allCategories = categoryDao.getAllCategories().first()
            val allPhotos = photoDao.getAllPhotos().first()

            assertTrue("Should have data after iteration $iteration",
                allCategories.isNotEmpty() && allPhotos.isNotEmpty())

            // Clean up this iteration's data
            categoryDao.deleteAllCategories()
            photoDao.deleteAllPhotos()

            // Force garbage collection
            System.gc()
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Then: Memory usage should not increase significantly
        assertTrue("Memory increase should be reasonable (< 50MB), actual: ${memoryIncrease / 1024 / 1024}MB",
            memoryIncrease < 50 * 1024 * 1024) // 50MB threshold
    }

    // ================== Index Performance Tests ==================

    @Test
    fun indexedQueriesPerformanceShouldBeOptimal() = runTest {
        // Given: Insert large dataset to test index performance
        val categoryDao = db.categoryDao()
        val photoDao = db.photoDao()

        val categories = createSampleCategories(50)
        categoryDao.insertCategories(categories)

        for (category in categories) {
            val photos = createSamplePhotos(category.id, 100)
            photoDao.insertPhotos(photos)
        }

        // When: Measure indexed query performance
        val indexedQueryTime = measureTimeMillis {
            // Query by category ID (should use foreign key index)
            val categoryPhotos = photoDao.getPhotosForCategory(categories[25].id).first()
            assertEquals("Should find photos for specific category", 100, categoryPhotos.size)

            // Query by position (should use position index)
            val categoryWithPhotos = categoryDao.getAllCategoriesWithPhotos().first()
            assertTrue("Categories should be ordered by position",
                categoryWithPhotos.first().category.position <= categoryWithPhotos.last().category.position)

            // Search by name (should use text index efficiently)
            val searchResults = photoDao.searchPhotosByName("Performance").first()
            assertTrue("Search should find matching photos", searchResults.isNotEmpty())
        }

        // Then: Indexed queries should be very fast
        assertTrue("Indexed queries should complete within 30ms (actual: ${indexedQueryTime}ms)",
            indexedQueryTime < 30)
    }
}