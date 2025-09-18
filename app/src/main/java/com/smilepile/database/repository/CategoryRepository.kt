package com.smilepile.database.repository

import com.smilepile.database.dao.CategoryDao
import com.smilepile.database.dao.CategoryWithPhotoCount
import com.smilepile.database.entities.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Category operations
 *
 * Provides a clean API for category management with proper error handling
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    /**
     * Get all active categories as Flow for reactive UI
     */
    fun getAllActiveCategories(): Flow<List<Category>> =
        categoryDao.getAllActiveCategories()

    /**
     * Get all categories including inactive ones
     */
    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: Long): Result<Category?> = runCatching {
        categoryDao.getCategoryById(categoryId)
    }

    /**
     * Get category by ID as Flow
     */
    fun getCategoryByIdFlow(categoryId: Long): Flow<Category?> =
        categoryDao.getCategoryByIdFlow(categoryId)

    /**
     * Create a new category
     */
    suspend fun createCategory(
        name: String,
        coverImagePath: String? = null
    ): Result<Long> = runCatching {
        val displayOrder = categoryDao.getNextDisplayOrder()
        val category = Category.create(
            name = name,
            coverImagePath = coverImagePath,
            displayOrder = displayOrder
        )
        categoryDao.insertCategory(category)
    }

    /**
     * Update category
     */
    suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        categoryDao.updateCategory(category)
    }

    /**
     * Update category name
     */
    suspend fun updateCategoryName(categoryId: Long, name: String): Result<Unit> = runCatching {
        val category = categoryDao.getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category not found")
        categoryDao.updateCategory(category.copy(name = name))
    }

    /**
     * Update category cover image
     */
    suspend fun updateCategoryCoverImage(
        categoryId: Long,
        coverImagePath: String?
    ): Result<Unit> = runCatching {
        categoryDao.updateCategoryCoverImage(categoryId, coverImagePath)
    }

    /**
     * Soft delete category (mark as inactive)
     */
    suspend fun softDeleteCategory(categoryId: Long): Result<Unit> = runCatching {
        categoryDao.softDeleteCategory(categoryId)
    }

    /**
     * Permanently delete category
     */
    suspend fun deleteCategory(categoryId: Long): Result<Unit> = runCatching {
        categoryDao.deleteCategoryById(categoryId)
    }

    /**
     * Reactivate soft-deleted category
     */
    suspend fun reactivateCategory(categoryId: Long): Result<Unit> = runCatching {
        categoryDao.reactivateCategory(categoryId)
    }

    /**
     * Reorder categories
     */
    suspend fun reorderCategories(categoryOrders: List<Pair<Long, Int>>): Result<Unit> = runCatching {
        categoryDao.reorderCategories(categoryOrders)
    }

    /**
     * Get photo count for category
     */
    suspend fun getPhotoCount(categoryId: Long): Result<Int> = runCatching {
        categoryDao.getPhotoCount(categoryId)
    }

    /**
     * Get photo count for category as Flow
     */
    fun getPhotoCountFlow(categoryId: Long): Flow<Int> =
        categoryDao.getPhotoCountFlow(categoryId)

    /**
     * Get categories with photo counts
     */
    fun getCategoriesWithPhotoCounts(): Flow<List<CategoryWithPhotoCount>> =
        categoryDao.getCategoriesWithPhotoCounts()

    /**
     * Validate category name
     */
    suspend fun validateCategoryName(name: String, excludeId: Long = -1): Result<Boolean> = runCatching {
        when {
            name.isBlank() -> false
            name.length > 50 -> false // Reasonable limit for UI
            categoryDao.isCategoryNameTaken(name, excludeId) > 0 -> false
            else -> true
        }
    }

    /**
     * Check if category exists
     */
    suspend fun categoryExists(categoryId: Long): Result<Boolean> = runCatching {
        categoryDao.getCategoryById(categoryId) != null
    }

    /**
     * Bulk create categories (for initial setup or import)
     */
    suspend fun createCategories(categoryNames: List<String>): Result<List<Long>> = runCatching {
        val categories = categoryNames.mapIndexed { index, name ->
            Category.create(
                name = name,
                displayOrder = index
            )
        }
        categoryDao.insertCategories(categories)
    }
}