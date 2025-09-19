package com.smilepile.app.repository

import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import com.smilepile.app.models.CategoryWithPhotos
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category and photo operations.
 * Provides a clean API that abstracts the data source (Room database, in-memory, etc.)
 */
interface CategoryRepository {

    // Category operations
    suspend fun getCategories(): List<Category>
    suspend fun getCategory(categoryId: String): Category?
    suspend fun addCategory(category: Category): Boolean
    suspend fun removeCategory(categoryId: String): Boolean

    // Photo operations
    suspend fun getPhotosForCategory(categoryId: String): List<Photo>
    suspend fun addPhoto(photo: Photo): Boolean
    suspend fun removePhoto(photoId: String): Boolean
    suspend fun getAllPhotos(): List<Photo>
    suspend fun getAllPhotoPaths(): List<String>

    // Combined operations
    suspend fun getCategoryWithPhotos(categoryId: String): CategoryWithPhotos?
    suspend fun getAllCategoriesWithPhotos(): List<CategoryWithPhotos>

    // Reactive operations (for UI updates)
    fun getCategoriesFlow(): Flow<List<Category>>
    fun getAllCategoriesWithPhotosFlow(): Flow<List<CategoryWithPhotos>>

    // Initialization
    suspend fun initializeSampleData(): Boolean
    suspend fun isInitialized(): Boolean
}