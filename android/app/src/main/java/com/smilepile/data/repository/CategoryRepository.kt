package com.smilepile.data.repository

import com.smilepile.data.models.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insertCategory(category: Category): Long
    suspend fun insertCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryById(categoryId: Long): Category?
    suspend fun getAllCategories(): List<Category>
    fun getAllCategoriesFlow(): Flow<List<Category>>
    suspend fun getCategoryByName(name: String): Category?
    suspend fun initializeDefaultCategories()
    suspend fun getCategoryCount(): Int

    // Batch operations for photo-category associations
    suspend fun assignPhotosToCategory(photoIds: List<String>, categoryId: Long)
    suspend fun removePhotosFromCategory(photoIds: List<String>, categoryId: Long)
    suspend fun assignPhotoToCategories(photoId: String, categoryIds: List<Long>)
    suspend fun getCategoriesForPhoto(photoId: String): List<Category>
    fun getCategoriesForPhotoFlow(photoId: String): Flow<List<Category>>
    suspend fun getPhotoCountInCategory(categoryId: Long): Int
}