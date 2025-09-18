package com.smilepile.database.dao

import androidx.room.*
import com.smilepile.database.entities.Category
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations
 *
 * Optimized for fast category browsing with proper query performance
 */
@Dao
interface CategoryDao {

    /**
     * Get all active categories ordered by display order
     * Returns Flow for reactive UI updates
     */
    @Query("""
        SELECT * FROM categories
        WHERE is_active = 1
        ORDER BY display_order ASC, name ASC
    """)
    fun getAllActiveCategories(): Flow<List<Category>>

    /**
     * Get all categories including inactive ones
     */
    @Query("""
        SELECT * FROM categories
        ORDER BY display_order ASC, name ASC
    """)
    fun getAllCategories(): Flow<List<Category>>

    /**
     * Get a specific category by ID
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    /**
     * Get category by ID as Flow for reactive updates
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryByIdFlow(categoryId: Long): Flow<Category?>

    /**
     * Get category by name
     */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    /**
     * Insert a new category
     * @return the ID of the inserted category
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category): Long

    /**
     * Insert multiple categories
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(categories: List<Category>): List<Long>

    /**
     * Update an existing category
     */
    @Update
    suspend fun updateCategory(category: Category)

    /**
     * Update multiple categories
     */
    @Update
    suspend fun updateCategories(categories: List<Category>)

    /**
     * Delete a category (soft delete by setting isActive = false)
     */
    @Query("UPDATE categories SET is_active = 0 WHERE id = :categoryId")
    suspend fun softDeleteCategory(categoryId: Long)

    /**
     * Permanently delete a category (hard delete)
     * Note: This will cascade delete all photos in the category
     */
    @Delete
    suspend fun deleteCategory(category: Category)

    /**
     * Permanently delete a category by ID
     */
    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    /**
     * Reactivate a soft-deleted category
     */
    @Query("UPDATE categories SET is_active = 1 WHERE id = :categoryId")
    suspend fun reactivateCategory(categoryId: Long)

    /**
     * Update category display order
     */
    @Query("UPDATE categories SET display_order = :displayOrder WHERE id = :categoryId")
    suspend fun updateCategoryDisplayOrder(categoryId: Long, displayOrder: Int)

    /**
     * Update category cover image
     */
    @Query("UPDATE categories SET cover_image_path = :coverImagePath WHERE id = :categoryId")
    suspend fun updateCategoryCoverImage(categoryId: Long, coverImagePath: String?)

    /**
     * Get the next display order for new categories
     */
    @Query("SELECT COALESCE(MAX(display_order), 0) + 1 FROM categories")
    suspend fun getNextDisplayOrder(): Int

    /**
     * Get count of photos in a category
     */
    @Query("""
        SELECT COUNT(*)
        FROM photos
        WHERE category_id = :categoryId
    """)
    suspend fun getPhotoCount(categoryId: Long): Int

    /**
     * Get count of photos in a category as Flow
     */
    @Query("""
        SELECT COUNT(*)
        FROM photos
        WHERE category_id = :categoryId
    """)
    fun getPhotoCountFlow(categoryId: Long): Flow<Int>

    /**
     * Check if category name exists (for validation)
     */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND id != :excludeId")
    suspend fun isCategoryNameTaken(name: String, excludeId: Long = -1): Int

    /**
     * Get categories with photo counts for management UI
     */
    @Query("""
        SELECT c.*, COUNT(p.id) as photo_count
        FROM categories c
        LEFT JOIN photos p ON c.id = p.category_id
        WHERE c.is_active = 1
        GROUP BY c.id
        ORDER BY c.display_order ASC, c.name ASC
    """)
    fun getCategoriesWithPhotoCounts(): Flow<List<CategoryWithPhotoCount>>

    /**
     * Transaction to safely reorder categories
     */
    @Transaction
    suspend fun reorderCategories(categoryOrders: List<Pair<Long, Int>>) {
        categoryOrders.forEach { (categoryId, order) ->
            updateCategoryDisplayOrder(categoryId, order)
        }
    }
}

/**
 * Data class for categories with photo counts
 */
data class CategoryWithPhotoCount(
    @Embedded val category: Category,
    @ColumnInfo(name = "photo_count") val photoCount: Int
)