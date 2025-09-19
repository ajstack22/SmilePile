package com.smilepile.app.repository

import android.content.Context
import android.util.Log
import com.smilepile.app.database.SmilePileDatabase
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.entities.PhotoEntity
import com.smilepile.app.managers.CategoryManager
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import com.smilepile.app.models.CategoryWithPhotos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Implementation of CategoryRepository that uses Room database with fallback to in-memory storage.
 * Ensures zero disruption by gracefully handling database failures.
 */
class CategoryRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CategoryRepository {

    companion object {
        private const val TAG = "CategoryRepository"
    }

    private val database by lazy { SmilePileDatabase.getDatabase(context, scope) }
    private val categoryDao by lazy { database.categoryDao() }
    private val photoDao by lazy { database.photoDao() }

    // Fallback manager for when database operations fail
    private val fallbackManager by lazy { CategoryManager() }

    // Thread-safe circuit breaker state
    private val fallbackState = AtomicBoolean(false)
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    private val maxFailures = 3
    private val recoveryTimeMs = 60_000L // 1 minute

    /**
     * Executes a database operation with fallback to in-memory storage using circuit breaker pattern
     */
    private suspend fun <T> withFallback(
        operation: suspend () -> T,
        fallbackOperation: () -> T,
        operationName: String
    ): T {
        // Check circuit breaker state
        if (shouldUseFallback()) {
            Log.d(TAG, "Circuit breaker open, using fallback for $operationName")
            return fallbackOperation()
        }

        return try {
            withContext(Dispatchers.IO) {
                operation().also {
                    // Reset failure count on success
                    if (failureCount.get() > 0) {
                        Log.d(TAG, "Database recovered, resetting failure count")
                        failureCount.set(0)
                        fallbackState.set(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Database operation '$operationName' failed", e)

            val failures = failureCount.incrementAndGet()
            lastFailureTime.set(System.currentTimeMillis())

            if (failures >= maxFailures) {
                fallbackState.set(true)
                Log.w(TAG, "Circuit breaker opened after $failures failures")
            }

            fallbackOperation()
        }
    }

    /**
     * Checks if the circuit breaker should use fallback
     */
    private fun shouldUseFallback(): Boolean {
        if (!fallbackState.get()) return false

        // Check if recovery time has passed
        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get()
        if (timeSinceLastFailure > recoveryTimeMs) {
            Log.d(TAG, "Attempting circuit breaker recovery after ${timeSinceLastFailure}ms")
            fallbackState.set(false)
            failureCount.set(0)
            return false
        }

        return true
    }

    /**
     * Returns the current circuit breaker status for monitoring
     */
    fun getCircuitBreakerStatus(): String {
        return when {
            fallbackState.get() -> "OPEN (using fallback)"
            failureCount.get() > 0 -> "HALF-OPEN (${failureCount.get()} failures)"
            else -> "CLOSED (normal operation)"
        }
    }

    override suspend fun getCategories(): List<Category> = withFallback(
        operation = {
            categoryDao.getAllCategories()
                .map { entities -> entities.map { it.toDomainModel() } }
                .first()
        },
        fallbackOperation = { fallbackManager.getCategories() },
        operationName = "getCategories"
    )

    override suspend fun getCategory(categoryId: String): Category? = withFallback(
        operation = {
            categoryDao.getCategoryById(categoryId)?.toDomainModel()
        },
        fallbackOperation = { fallbackManager.getCategory(categoryId) },
        operationName = "getCategory"
    )

    override suspend fun addCategory(category: Category): Boolean = withFallback(
        operation = {
            val entity = CategoryEntity.fromDomainModel(category)
            categoryDao.insertCategory(entity) > 0
        },
        fallbackOperation = { fallbackManager.addCategory(category) },
        operationName = "addCategory"
    )

    override suspend fun removeCategory(categoryId: String): Boolean = withFallback(
        operation = {
            val category = categoryDao.getCategoryById(categoryId)
            if (category != null) {
                categoryDao.deleteCategory(category) > 0
            } else {
                false
            }
        },
        fallbackOperation = { fallbackManager.removeCategory(categoryId) },
        operationName = "removeCategory"
    )

    override suspend fun getPhotosForCategory(categoryId: String): List<Photo> = withFallback(
        operation = {
            photoDao.getPhotosForCategory(categoryId)
                .map { entities -> entities.map { it.toDomainModel() } }
                .first()
        },
        fallbackOperation = { fallbackManager.getPhotosForCategory(categoryId) },
        operationName = "getPhotosForCategory"
    )

    override suspend fun addPhoto(photo: Photo): Boolean = withFallback(
        operation = {
            database.withTransaction {
                try {
                    val entity = PhotoEntity.fromDomainModel(photo)
                    val result = photoDao.insertPhoto(entity) > 0

                    // Update category photo count
                    if (result) {
                        val category = categoryDao.getCategoryById(photo.categoryId)
                        if (category != null) {
                            val photoCount = photoDao.getPhotoCountForCategory(photo.categoryId)
                            val updatedCategory = category.copy(photoCount = photoCount)
                            categoryDao.updateCategory(updatedCategory)
                        }
                    }
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "Transaction failed for addPhoto", e)
                    throw e // Propagate to trigger rollback
                }
            }
        },
        fallbackOperation = { fallbackManager.addPhoto(photo) },
        operationName = "addPhoto"
    )

    override suspend fun removePhoto(photoId: String): Boolean = withFallback(
        operation = {
            database.withTransaction {
                try {
                    val photo = photoDao.getPhotoById(photoId)
                    if (photo != null) {
                        val result = photoDao.deletePhoto(photo) > 0

                        // Update category photo count
                        if (result && photo.categoryId != null) {
                            val category = categoryDao.getCategoryById(photo.categoryId)
                            if (category != null) {
                                val photoCount = photoDao.getPhotoCountForCategory(photo.categoryId)
                                val updatedCategory = category.copy(photoCount = photoCount)
                                categoryDao.updateCategory(updatedCategory)
                            }
                        }
                        result
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Transaction failed for removePhoto", e)
                    throw e // Propagate to trigger rollback
                }
            }
        },
        fallbackOperation = { fallbackManager.removePhoto(photoId) },
        operationName = "removePhoto"
    )

    override suspend fun getAllPhotos(): List<Photo> = withFallback(
        operation = {
            photoDao.getAllPhotos()
                .map { entities -> entities.map { it.toDomainModel() } }
                .first()
        },
        fallbackOperation = { fallbackManager.getAllPhotos() },
        operationName = "getAllPhotos"
    )

    override suspend fun getAllPhotoPaths(): List<String> = withFallback(
        operation = {
            getAllPhotos().map { it.getAssetPath() }
        },
        fallbackOperation = { fallbackManager.getAllPhotoPaths() },
        operationName = "getAllPhotoPaths"
    )

    override suspend fun getCategoryWithPhotos(categoryId: String): CategoryWithPhotos? = withFallback(
        operation = {
            categoryDao.getCategoryWithPhotos(categoryId)?.toDomainModel()
        },
        fallbackOperation = { fallbackManager.getCategoryWithPhotos(categoryId) },
        operationName = "getCategoryWithPhotos"
    )

    override suspend fun getAllCategoriesWithPhotos(): List<CategoryWithPhotos> = withFallback(
        operation = {
            categoryDao.getAllCategoriesWithPhotos()
                .map { entities -> entities.map { it.toDomainModel() } }
                .first()
        },
        fallbackOperation = { fallbackManager.getAllCategoriesWithPhotos() },
        operationName = "getAllCategoriesWithPhotos"
    )

    override fun getCategoriesFlow(): Flow<List<Category>> {
        return if (shouldUseFallback()) {
            flowOf(fallbackManager.getCategories())
        } else {
            categoryDao.getAllCategories()
                .map { entities -> entities.map { it.toDomainModel() } }
                .catch { e ->
                    Log.w(TAG, "Database flow failed, falling back to in-memory", e)

                    val failures = failureCount.incrementAndGet()
                    lastFailureTime.set(System.currentTimeMillis())

                    if (failures >= maxFailures) {
                        fallbackState.set(true)
                        Log.w(TAG, "Circuit breaker opened after $failures failures")
                    }

                    emit(fallbackManager.getCategories())
                }
        }
    }

    override fun getAllCategoriesWithPhotosFlow(): Flow<List<CategoryWithPhotos>> {
        return if (shouldUseFallback()) {
            flowOf(fallbackManager.getAllCategoriesWithPhotos())
        } else {
            categoryDao.getAllCategoriesWithPhotos()
                .map { entities -> entities.map { it.toDomainModel() } }
                .catch { e ->
                    Log.w(TAG, "Database flow failed, falling back to in-memory", e)

                    val failures = failureCount.incrementAndGet()
                    lastFailureTime.set(System.currentTimeMillis())

                    if (failures >= maxFailures) {
                        fallbackState.set(true)
                        Log.w(TAG, "Circuit breaker opened after $failures failures")
                    }

                    emit(fallbackManager.getAllCategoriesWithPhotos())
                }
        }
    }

    override suspend fun initializeSampleData(): Boolean = withFallback(
        operation = {
            database.withTransaction {
                try {
                    val categoryCount = categoryDao.getCategoryCount()
                    if (categoryCount == 0) {
                        // Initialize sample data matching CategoryManager.initializeSampleData()
                        val sampleImages = listOf(
                            "sample_1.png", "sample_2.png", "sample_3.png",
                            "sample_4.png", "sample_5.png", "sample_6.png"
                        )

                        // Create 3 sample categories
                        val categories = listOf(
                            Category(
                                id = "animals",
                                name = "animals",
                                displayName = "Animals",
                                coverImagePath = null,
                                description = "Fun animal pictures",
                                position = 0
                            ),
                            Category(
                                id = "family",
                                name = "family",
                                displayName = "Family",
                                coverImagePath = null,
                                description = "Happy family moments",
                                position = 1
                            ),
                            Category(
                                id = "fun_times",
                                name = "fun_times",
                                displayName = "Fun Times",
                                coverImagePath = null,
                                description = "Good times and memories",
                                position = 2
                            )
                        )

                        // Insert categories
                        val categoryEntities = categories.map { CategoryEntity.fromDomainModel(it) }
                        categoryDao.insertCategories(categoryEntities)

                        // Create and insert photos
                        val photos = sampleImages.mapIndexed { index, imageName ->
                            val categoryId = when (index % 3) {
                                0 -> "animals"
                                1 -> "family"
                                else -> "fun_times"
                            }

                            Photo(
                                id = "photo_${index + 1}",
                                path = imageName,
                                name = imageName.substringBeforeLast('.'),
                                categoryId = categoryId,
                                position = index / 3,
                                isFromAssets = true
                            )
                        }

                        val photoEntities = photos.map { PhotoEntity.fromDomainModel(it) }
                        photoDao.insertPhotos(photoEntities)

                        // Update category photo counts
                        categories.forEach { category ->
                            val photoCount = photoDao.getPhotoCountForCategory(category.id)
                            val updatedCategory = CategoryEntity.fromDomainModel(category.copy(photoCount = photoCount))
                            categoryDao.updateCategory(updatedCategory)
                        }

                        Log.d(TAG, "Sample data initialized in database")
                        true
                    } else {
                        Log.d(TAG, "Database already has data, skipping initialization")
                        true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Transaction failed for initializeSampleData", e)
                    throw e // Propagate to trigger rollback
                }
            }
        },
        fallbackOperation = {
            // Fallback manager already initializes sample data in its constructor
            Log.d(TAG, "Using fallback manager sample data")
            true
        },
        operationName = "initializeSampleData"
    )

    override suspend fun isInitialized(): Boolean = withFallback(
        operation = {
            categoryDao.getCategoryCount() > 0
        },
        fallbackOperation = { fallbackManager.getCategories().isNotEmpty() },
        operationName = "isInitialized"
    )

    /**
     * Add a category with photos in a single transaction.
     * Ensures data consistency by either inserting all data or rolling back completely.
     */
    suspend fun addCategoryWithPhotos(category: Category, photos: List<Photo>): Boolean {
        return withFallback(
            operation = {
                database.withTransaction {
                    try {
                        // Insert category first
                        val categoryEntity = CategoryEntity.fromDomainModel(category)
                        val categoryResult = categoryDao.insertCategory(categoryEntity)
                        if (categoryResult <= 0) {
                            throw IllegalStateException("Failed to insert category")
                        }

                        // Insert photos if any
                        if (photos.isNotEmpty()) {
                            val photoEntities = photos.map { PhotoEntity.fromDomainModel(it) }
                            val photoResults = photoDao.insertPhotos(photoEntities)
                            if (photoResults.any { it <= 0 }) {
                                throw IllegalStateException("Failed to insert some photos")
                            }
                        }

                        // Update category photo count
                        val updatedCategory = categoryEntity.copy(photoCount = photos.size)
                        categoryDao.updateCategory(updatedCategory)

                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Transaction failed for addCategoryWithPhotos", e)
                        throw e // Propagate to trigger rollback
                    }
                }
            },
            fallbackOperation = {
                fallbackManager.addCategory(category) &&
                photos.all { fallbackManager.addPhoto(it) }
            },
            operationName = "addCategoryWithPhotos"
        )
    }

    /**
     * Move a photo to a different category with transaction support.
     * Updates photo counts for both source and destination categories atomically.
     */
    suspend fun movePhotoToCategory(photoId: String, newCategoryId: String): Boolean {
        return withFallback(
            operation = {
                database.withTransaction {
                    try {
                        val photo = photoDao.getPhotoById(photoId)
                            ?: throw IllegalArgumentException("Photo not found: $photoId")

                        val oldCategoryId = photo.categoryId

                        // Update photo category
                        val updatedPhoto = photo.copy(categoryId = newCategoryId)
                        photoDao.updatePhoto(updatedPhoto)

                        // Update photo counts for both categories
                        updateCategoryPhotoCounts(listOf(oldCategoryId, newCategoryId))

                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Transaction failed for movePhotoToCategory", e)
                        throw e // Propagate to trigger rollback
                    }
                }
            },
            fallbackOperation = {
                // Fallback implementation
                val photo = fallbackManager.getAllPhotos().find { it.id == photoId }
                photo?.let {
                    fallbackManager.removePhoto(photoId) &&
                    fallbackManager.addPhoto(it.copy(categoryId = newCategoryId))
                } ?: false
            },
            operationName = "movePhotoToCategory"
        )
    }

    /**
     * Helper method to update category photo counts for multiple categories
     */
    private suspend fun updateCategoryPhotoCounts(categoryIds: List<String?>) {
        categoryIds.filterNotNull().forEach { categoryId ->
            val category = categoryDao.getCategoryById(categoryId)
            if (category != null) {
                val photoCount = photoDao.getActivePhotoCountForCategory(categoryId)
                val updatedCategory = category.copy(photoCount = photoCount)
                categoryDao.updateCategory(updatedCategory)
            }
        }
    }

    /**
     * Add multiple photos to a category in a single transaction.
     * Ensures all photos are added or none are added.
     */
    suspend fun bulkAddPhotos(categoryId: String, photos: List<Photo>): Boolean {
        return withFallback(
            operation = {
                database.withTransaction {
                    try {
                        // Verify category exists
                        val category = categoryDao.getCategoryById(categoryId)
                            ?: throw IllegalArgumentException("Category not found: $categoryId")

                        // Insert all photos
                        val photoEntities = photos.map {
                            PhotoEntity.fromDomainModel(it.copy(categoryId = categoryId))
                        }
                        val results = photoDao.insertPhotos(photoEntities)

                        if (results.any { it <= 0 }) {
                            throw IllegalStateException("Failed to insert some photos")
                        }

                        // Update category photo count
                        val newCount = category.photoCount + photos.size
                        categoryDao.updateCategory(category.copy(photoCount = newCount))

                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Transaction failed for bulkAddPhotos", e)
                        throw e // Propagate to trigger rollback
                    }
                }
            },
            fallbackOperation = {
                photos.all { fallbackManager.addPhoto(it.copy(categoryId = categoryId)) }
            },
            operationName = "bulkAddPhotos"
        )
    }

    /**
     * Reorder photos within a category atomically.
     * Updates all photo positions in a single transaction.
     */
    suspend fun reorderPhotosInCategory(categoryId: String, photoIds: List<String>): Boolean {
        return withFallback(
            operation = {
                database.withTransaction {
                    try {
                        // Update positions for all photos
                        photoIds.forEachIndexed { index, photoId ->
                            val photo = photoDao.getPhotoById(photoId)
                            if (photo != null && photo.categoryId == categoryId) {
                                photoDao.updatePhoto(photo.copy(position = index))
                            }
                        }
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Transaction failed for reorderPhotosInCategory", e)
                        throw e // Propagate to trigger rollback
                    }
                }
            },
            fallbackOperation = {
                // Fallback doesn't support reordering
                true
            },
            operationName = "reorderPhotosInCategory"
        )
    }
}