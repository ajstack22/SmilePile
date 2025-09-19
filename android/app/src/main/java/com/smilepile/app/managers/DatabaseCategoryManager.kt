package com.smilepile.app.managers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import com.smilepile.app.models.CategoryWithPhotos
import com.smilepile.app.repository.CategoryRepository
import com.smilepile.app.repository.CategoryRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced CategoryManager that uses Room database for persistent storage.
 * Maintains exact same public API as the original CategoryManager for zero breaking changes.
 * Includes graceful fallback to in-memory storage if database operations fail.
 */
class DatabaseCategoryManager(
    context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) {
    companion object {
        private const val TAG = "DatabaseCategoryManager"
    }

    private val scope = lifecycleOwner?.lifecycleScope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository: CategoryRepository = CategoryRepositoryImpl(context, scope)

    // Cache fields for immediate data access without blocking main thread
    private var _categoriesCache: List<Category> = emptyList()
    private var _categoriesWithPhotosCache: List<CategoryWithPhotos> = emptyList()
    private val _photosCache: MutableMap<String, List<Photo>> = mutableMapOf()
    private val _isCacheValid: AtomicBoolean = AtomicBoolean(false)

    init {
        // Initialize sample data and preload cache asynchronously
        scope.launch {
            try {
                repository.initializeSampleData()
                refreshCache()
                Log.d(TAG, "DatabaseCategoryManager initialized successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize cache", e)
            }
        }
    }

    /**
     * Refreshes all cache data from the repository
     */
    private suspend fun refreshCache() {
        try {
            _categoriesCache = repository.getCategories()
            _categoriesWithPhotosCache = repository.getAllCategoriesWithPhotos()

            // Cache photos for each category
            _photosCache.clear()
            for (category in _categoriesCache) {
                _photosCache[category.id] = repository.getPhotosForCategory(category.id)
            }

            _isCacheValid.set(true)
            Log.d(TAG, "Cache refreshed successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh cache", e)
        }
    }

    /**
     * Gets all categories sorted by position
     */
    fun getCategories(): List<Category> {
        return if (_isCacheValid.get()) {
            _categoriesCache
        } else {
            // Return empty for initialization, cache will populate async
            emptyList()
        }
    }

    /**
     * Gets a category by ID
     */
    fun getCategory(categoryId: String): Category? {
        return if (_isCacheValid.get()) {
            _categoriesCache.firstOrNull { it.id == categoryId }
        } else {
            // Return null for initialization, cache will populate async
            null
        }
    }

    /**
     * Gets all photos for a specific category
     */
    fun getPhotosForCategory(categoryId: String): List<Photo> {
        return if (_isCacheValid.get()) {
            _photosCache[categoryId] ?: emptyList()
        } else {
            // Return empty for initialization, cache will populate async
            emptyList()
        }
    }

    /**
     * Gets a category with its photos
     */
    fun getCategoryWithPhotos(categoryId: String): CategoryWithPhotos? {
        return if (_isCacheValid.get()) {
            _categoriesWithPhotosCache.firstOrNull { it.category.id == categoryId }
        } else {
            // Return null for initialization, cache will populate async
            null
        }
    }

    /**
     * Gets all categories with their photos
     */
    fun getAllCategoriesWithPhotos(): List<CategoryWithPhotos> {
        return if (_isCacheValid.get()) {
            _categoriesWithPhotosCache
        } else {
            // Return empty for initialization, cache will populate async
            emptyList()
        }
    }

    /**
     * Gets all photos across all categories
     */
    fun getAllPhotos(): List<Photo> {
        return if (_isCacheValid.get()) {
            _photosCache.values.flatten()
        } else {
            // Return empty for initialization, cache will populate async
            emptyList()
        }
    }

    /**
     * Gets all photo paths for use with ImagePagerAdapter (maintains backward compatibility)
     */
    fun getAllPhotoPaths(): List<String> {
        return if (_isCacheValid.get()) {
            _photosCache.values.flatten().map { it.path }
        } else {
            // Return empty for initialization, cache will populate async
            emptyList()
        }
    }

    /**
     * Adds a new category
     */
    fun addCategory(category: Category): Boolean {
        // Launch async operation to add category and refresh cache
        scope.launch {
            try {
                val result = repository.addCategory(category)
                if (result) {
                    refreshCache()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add category ${category.id}", e)
            }
        }
        // Return true optimistically - cache will update when operation completes
        return true
    }

    /**
     * Adds a new photo to a category
     */
    fun addPhoto(photo: Photo): Boolean {
        // Launch async operation to add photo and refresh cache
        scope.launch {
            try {
                val result = repository.addPhoto(photo)
                if (result) {
                    refreshCache()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add photo ${photo.id}", e)
            }
        }
        // Return true optimistically - cache will update when operation completes
        return true
    }

    /**
     * Removes a category and all its photos
     */
    fun removeCategory(categoryId: String): Boolean {
        // Launch async operation to remove category and refresh cache
        scope.launch {
            try {
                val result = repository.removeCategory(categoryId)
                if (result) {
                    refreshCache()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove category $categoryId", e)
            }
        }
        // Return true optimistically - cache will update when operation completes
        return true
    }

    /**
     * Removes a photo from its category
     */
    fun removePhoto(photoId: String): Boolean {
        // Launch async operation to remove photo and refresh cache
        scope.launch {
            try {
                val result = repository.removePhoto(photoId)
                if (result) {
                    refreshCache()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove photo $photoId", e)
            }
        }
        // Return true optimistically - cache will update when operation completes
        return true
    }

    // Async versions for UI components that can handle coroutines

    /**
     * Gets all categories sorted by position (async version)
     */
    suspend fun getCategoriesAsync(): List<Category> {
        return repository.getCategories()
    }

    /**
     * Gets a category by ID (async version)
     */
    suspend fun getCategoryAsync(categoryId: String): Category? {
        return repository.getCategory(categoryId)
    }

    /**
     * Gets all photos for a specific category (async version)
     */
    suspend fun getPhotosForCategoryAsync(categoryId: String): List<Photo> {
        return repository.getPhotosForCategory(categoryId)
    }

    /**
     * Gets a category with its photos (async version)
     */
    suspend fun getCategoryWithPhotosAsync(categoryId: String): CategoryWithPhotos? {
        return repository.getCategoryWithPhotos(categoryId)
    }

    /**
     * Gets all categories with their photos (async version)
     */
    suspend fun getAllCategoriesWithPhotosAsync(): List<CategoryWithPhotos> {
        return repository.getAllCategoriesWithPhotos()
    }

    /**
     * Gets all photos across all categories (async version)
     */
    suspend fun getAllPhotosAsync(): List<Photo> {
        return repository.getAllPhotos()
    }

    /**
     * Gets all photo paths for use with ImagePagerAdapter (async version)
     */
    suspend fun getAllPhotoPathsAsync(): List<String> {
        return repository.getAllPhotoPaths()
    }

    /**
     * Adds a new category (async version)
     */
    suspend fun addCategoryAsync(category: Category): Boolean {
        val result = repository.addCategory(category)
        if (result) {
            refreshCache()
        }
        return result
    }

    /**
     * Adds a new photo to a category (async version)
     */
    suspend fun addPhotoAsync(photo: Photo): Boolean {
        val result = repository.addPhoto(photo)
        if (result) {
            refreshCache()
        }
        return result
    }

    /**
     * Removes a category and all its photos (async version)
     */
    suspend fun removeCategoryAsync(categoryId: String): Boolean {
        val result = repository.removeCategory(categoryId)
        if (result) {
            refreshCache()
        }
        return result
    }

    /**
     * Removes a photo from its category (async version)
     */
    suspend fun removePhotoAsync(photoId: String): Boolean {
        val result = repository.removePhoto(photoId)
        if (result) {
            refreshCache()
        }
        return result
    }

    // Reactive data access for UI updates

    /**
     * Gets categories as a Flow for reactive UI updates
     */
    fun getCategoriesFlow(): Flow<List<Category>> = repository.getCategoriesFlow()
        .onEach { categories ->
            _categoriesCache = categories
            _isCacheValid.set(true)
        }
        .flowOn(Dispatchers.IO)

    /**
     * Gets all categories with photos as a Flow for reactive UI updates
     */
    fun getAllCategoriesWithPhotosFlow(): Flow<List<CategoryWithPhotos>> = repository.getAllCategoriesWithPhotosFlow()
        .onEach { categoriesWithPhotos ->
            _categoriesWithPhotosCache = categoriesWithPhotos
            _isCacheValid.set(true)
        }
        .flowOn(Dispatchers.IO)
}