package com.smilepile.app.data.repository

import com.smilepile.app.data.database.dao.CategoryDao
import com.smilepile.app.data.database.entities.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date

/**
 * Repository for Category operations
 *
 * Provides a clean API for category management with proper error handling
 * Currently uses mock data until Room compiler is enabled
 */
class CategoryRepository(
    private val categoryDao: CategoryDao? = null
) {

    /**
     * Get all categories as Flow for reactive UI
     * Currently returns mock data for ViewPager2 testing
     */
    fun getAllCategoriesFlow(): Flow<List<Category>> {
        // Return mock data for now until Room compiler is enabled
        val mockCategories = listOf(
            Category(
                id = 1,
                name = "Family",
                description = "Family photos and memories",
                colorCode = "#FF6B6B",
                dateCreated = Date(),
                photoCount = 15,
                isDefault = true
            ),
            Category(
                id = 2,
                name = "Pets",
                description = "Our furry friends",
                colorCode = "#4ECDC4",
                dateCreated = Date(),
                photoCount = 8,
                isDefault = false
            ),
            Category(
                id = 3,
                name = "Travel",
                description = "Adventures and trips",
                colorCode = "#45B7D1",
                dateCreated = Date(),
                photoCount = 23,
                isDefault = false
            ),
            Category(
                id = 4,
                name = "Food",
                description = "Delicious meals",
                colorCode = "#F7DC6F",
                dateCreated = Date(),
                photoCount = 12,
                isDefault = false
            )
        )

        return flowOf(mockCategories)
    }

    /**
     * Get all categories (suspend function)
     */
    suspend fun getAllCategories(): List<Category> {
        return categoryDao?.getAllCategories() ?: emptyList()
    }

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: Long): Category? {
        return categoryDao?.getCategoryById(categoryId)
    }

    /**
     * Insert new category
     */
    suspend fun insertCategory(category: Category): Long {
        return categoryDao?.insertCategory(category) ?: -1
    }

    /**
     * Update category
     */
    suspend fun updateCategory(category: Category) {
        categoryDao?.updateCategory(category)
    }

    /**
     * Delete category
     */
    suspend fun deleteCategory(category: Category) {
        categoryDao?.deleteCategory(category)
    }

    /**
     * Update photo count for category
     */
    suspend fun updatePhotoCount(categoryId: Long, count: Int) {
        categoryDao?.updatePhotoCount(categoryId, count)
    }
}