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
}