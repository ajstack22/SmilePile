package com.smilepile.app.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.entities.CategoryWithPhotosEntity

/**
 * Data Access Object for Category operations.
 * Provides CRUD operations and relationship queries for categories.
 */
@Dao
interface CategoryDao {

    /**
     * Get all categories as a Flow for reactive updates.
     * Categories are ordered by their position.
     */
    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /**
     * Get a single category by its ID.
     * Returns null if category doesn't exist.
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    /**
     * Get all categories with their associated photos.
     * Uses @Transaction to ensure data consistency.
     */
    @Transaction
    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun getAllCategoriesWithPhotos(): Flow<List<CategoryWithPhotosEntity>>

    /**
     * Get a specific category with its associated photos.
     * Uses @Transaction to ensure data consistency.
     */
    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryWithPhotos(id: String): CategoryWithPhotosEntity?

    /**
     * Insert a single category.
     * Uses REPLACE strategy to handle conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    /**
     * Insert multiple categories in a batch operation.
     * Uses REPLACE strategy to handle conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    /**
     * Update an existing category.
     * Returns the number of rows affected.
     */
    @Update
    suspend fun updateCategory(category: CategoryEntity): Int

    /**
     * Delete a specific category.
     * Returns the number of rows affected.
     */
    @Delete
    suspend fun deleteCategory(category: CategoryEntity): Int

    /**
     * Delete all categories from the database.
     * Use with caution as this removes all category data.
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories(): Int

    /**
     * Get the count of categories in the database.
     * Useful for UI state management.
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    /**
     * Check if a category exists by name.
     * Useful for validation before insertion.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name)")
    suspend fun categoryExistsByName(name: String): Boolean

    /**
     * Get the maximum position value for ordering new categories.
     * Returns 0 if no categories exist.
     */
    @Query("SELECT COALESCE(MAX(position), 0) FROM categories")
    suspend fun getMaxPosition(): Int

    /**
     * Update category positions for reordering.
     * This is an upsert operation using @Insert with REPLACE.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCategoryPositions(categories: List<CategoryEntity>): List<Long>

    // ========== SAFE DELETE OPERATIONS ==========

    /**
     * Check if category has active (non-deleted) photos before deletion.
     * Returns the count of active photos in the category.
     * Used to prevent accidental deletion of categories with photos.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getActivePhotoCount(categoryId: String): Int

    /**
     * Safe delete that checks for photos before deletion.
     * Returns true if category was deleted, false if it has photos.
     * This prevents accidental data loss by refusing to delete categories with photos.
     */
    @Transaction
    suspend fun safeDeleteCategory(category: CategoryEntity): Boolean {
        val photoCount = getActivePhotoCount(category.id)
        return if (photoCount == 0) {
            deleteCategory(category) > 0
        } else {
            false  // Refuse to delete if photos exist
        }
    }

    /**
     * Force delete category and soft-delete all its photos.
     * Use with caution - this will make photos inaccessible but not permanently delete them.
     * Photos can be recovered using PhotoDao.restorePhotosForCategory().
     */
    @Transaction
    suspend fun forceDeleteCategoryWithPhotos(category: CategoryEntity, photoDao: com.smilepile.app.database.dao.PhotoDao): Boolean {
        val timestamp = System.currentTimeMillis()

        // First soft-delete all photos in the category
        photoDao.softDeletePhotosForCategory(category.id, timestamp)

        // Then delete the category
        return deleteCategory(category) > 0
    }

    /**
     * Safe delete with cleanup - performs safe delete and cleans up if successful.
     * Uses @Transaction to ensure atomicity.
     */
    @Transaction
    suspend fun deleteWithCleanup(category: CategoryEntity, photoDao: com.smilepile.app.database.dao.PhotoDao): Int {
        // First soft-delete all photos
        photoDao.softDeletePhotosForCategory(category.id)
        // Then delete the category
        return deleteCategory(category)
    }

    /**
     * Get categories that have no active photos.
     * Useful for cleanup operations or identifying empty categories.
     */
    @Query("""
        SELECT c.* FROM categories c
        LEFT JOIN photos p ON c.id = p.categoryId AND p.isDeleted = 0
        GROUP BY c.id, c.name, c.displayName, c.coverImagePath, c.description, c.position, c.photoCount
        HAVING COUNT(p.id) = 0
        ORDER BY c.position ASC
    """)
    suspend fun getEmptyCategories(): List<CategoryEntity>

    /**
     * Get categories with their actual photo counts (excluding soft-deleted photos).
     * Useful for updating category photoCount fields.
     */
    @Query("""
        SELECT c.id, c.name, c.displayName, c.coverImagePath, c.description, c.position, c.photoCount, COUNT(p.id) as actualPhotoCount
        FROM categories c
        LEFT JOIN photos p ON c.id = p.categoryId AND p.isDeleted = 0
        GROUP BY c.id, c.name, c.displayName, c.coverImagePath, c.description, c.position, c.photoCount
        ORDER BY c.position ASC
    """)
    suspend fun getCategoriesWithActualPhotoCounts(): List<CategoryWithPhotoCount>

    /**
     * Data class for categories with their actual photo counts
     */
    data class CategoryWithPhotoCount(
        val id: String,
        val name: String,
        val displayName: String,
        val coverImagePath: String?,
        val description: String,
        val position: Int,
        val photoCount: Int,
        val actualPhotoCount: Int
    )
}