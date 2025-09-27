package com.smilepile.data.dao

import androidx.room.*
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoCategoryJoin
import com.smilepile.data.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing photo-category relationships
 * Handles the many-to-many relationship between photos and categories
 */
@Dao
interface PhotoCategoryDao {

    // ===== Join Table Operations =====

    /**
     * Add a photo to a category
     * @param join The PhotoCategoryJoin entity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoCategoryJoin(join: PhotoCategoryJoin)

    /**
     * Add multiple photo-category associations
     * @param joins List of PhotoCategoryJoin entities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoCategoryJoins(joins: List<PhotoCategoryJoin>)

    /**
     * Remove a photo from a category
     * @param photoId The photo ID
     * @param categoryId The category ID
     */
    @Query("DELETE FROM photo_category_join WHERE photo_id = :photoId AND category_id = :categoryId")
    suspend fun removePhotoFromCategory(photoId: String, categoryId: Long)

    /**
     * Remove a photo from all categories
     * @param photoId The photo ID
     */
    @Query("DELETE FROM photo_category_join WHERE photo_id = :photoId")
    suspend fun removePhotoFromAllCategories(photoId: String)

    /**
     * Remove all photos from a category
     * @param categoryId The category ID
     */
    @Query("DELETE FROM photo_category_join WHERE category_id = :categoryId")
    suspend fun removeAllPhotosFromCategory(categoryId: Long)

    // ===== Query Operations =====

    /**
     * Get all categories for a specific photo
     * @param photoId The photo ID
     * @return List of categories the photo belongs to
     */
    @Query("""
        SELECT c.* FROM category_entities c
        INNER JOIN photo_category_join pcj ON c.id = pcj.category_id
        WHERE pcj.photo_id = :photoId
        ORDER BY pcj.assigned_at DESC
    """)
    suspend fun getCategoriesForPhoto(photoId: String): List<CategoryEntity>

    /**
     * Get all categories for a specific photo as Flow
     * @param photoId The photo ID
     * @return Flow of categories the photo belongs to
     */
    @Query("""
        SELECT c.* FROM category_entities c
        INNER JOIN photo_category_join pcj ON c.id = pcj.category_id
        WHERE pcj.photo_id = :photoId
        ORDER BY pcj.assigned_at DESC
    """)
    fun getCategoriesForPhotoFlow(photoId: String): Flow<List<CategoryEntity>>

    /**
     * Get all photos in a specific category
     * @param categoryId The category ID
     * @return List of photos in the category
     */
    @Query("""
        SELECT p.* FROM photo_entities p
        INNER JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.category_id = :categoryId
        ORDER BY pcj.assigned_at DESC
    """)
    suspend fun getPhotosInCategory(categoryId: Long): List<PhotoEntity>

    /**
     * Get all photos in a specific category as Flow
     * @param categoryId The category ID
     * @return Flow of photos in the category
     */
    @Query("""
        SELECT p.* FROM photo_entities p
        INNER JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.category_id = :categoryId
        ORDER BY pcj.assigned_at DESC
    """)
    fun getPhotosInCategoryFlow(categoryId: Long): Flow<List<PhotoEntity>>

    /**
     * Get photos in multiple categories (OR operation)
     * @param categoryIds List of category IDs
     * @return List of photos in any of the specified categories
     */
    @Query("""
        SELECT DISTINCT p.* FROM photo_entities p
        INNER JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.category_id IN (:categoryIds)
        ORDER BY p.timestamp DESC
    """)
    suspend fun getPhotosInCategories(categoryIds: List<Long>): List<PhotoEntity>

    /**
     * Get photos in multiple categories as Flow (OR operation)
     * @param categoryIds List of category IDs
     * @return Flow of photos in any of the specified categories
     */
    @Query("""
        SELECT DISTINCT p.* FROM photo_entities p
        INNER JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.category_id IN (:categoryIds)
        ORDER BY p.timestamp DESC
    """)
    fun getPhotosInCategoriesFlow(categoryIds: List<Long>): Flow<List<PhotoEntity>>

    /**
     * Get photos that belong to ALL specified categories (AND operation)
     * @param categoryIds List of category IDs
     * @param categoryCount Number of categories (must match categoryIds.size)
     * @return List of photos that belong to all specified categories
     */
    @Query("""
        SELECT p.* FROM photo_entities p
        INNER JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.category_id IN (:categoryIds)
        GROUP BY p.id
        HAVING COUNT(DISTINCT pcj.category_id) = :categoryCount
        ORDER BY p.timestamp DESC
    """)
    suspend fun getPhotosInAllCategories(categoryIds: List<Long>, categoryCount: Int): List<PhotoEntity>

    /**
     * Get photos without any category
     * @return List of uncategorized photos
     */
    @Query("""
        SELECT p.* FROM photo_entities p
        LEFT JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.photo_id IS NULL
        ORDER BY p.timestamp DESC
    """)
    suspend fun getUncategorizedPhotos(): List<PhotoEntity>

    /**
     * Get photos without any category as Flow
     * @return Flow of uncategorized photos
     */
    @Query("""
        SELECT p.* FROM photo_entities p
        LEFT JOIN photo_category_join pcj ON p.id = pcj.photo_id
        WHERE pcj.photo_id IS NULL
        ORDER BY p.timestamp DESC
    """)
    fun getUncategorizedPhotosFlow(): Flow<List<PhotoEntity>>

    // ===== Count Operations =====

    /**
     * Get the number of photos in a category
     * @param categoryId The category ID
     * @return Number of photos in the category
     */
    @Query("SELECT COUNT(*) FROM photo_category_join WHERE category_id = :categoryId")
    suspend fun getPhotoCountInCategory(categoryId: Long): Int

    /**
     * Get the number of photos in a category as Flow
     * @param categoryId The category ID
     * @return Flow of photo count
     */
    @Query("SELECT COUNT(*) FROM photo_category_join WHERE category_id = :categoryId")
    fun getPhotoCountInCategoryFlow(categoryId: Long): Flow<Int>

    /**
     * Get the number of categories a photo belongs to
     * @param photoId The photo ID
     * @return Number of categories
     */
    @Query("SELECT COUNT(*) FROM photo_category_join WHERE photo_id = :photoId")
    suspend fun getCategoryCountForPhoto(photoId: String): Int

    /**
     * Check if a photo belongs to a category
     * @param photoId The photo ID
     * @param categoryId The category ID
     * @return True if the photo belongs to the category
     */
    @Query("SELECT COUNT(*) > 0 FROM photo_category_join WHERE photo_id = :photoId AND category_id = :categoryId")
    suspend fun isPhotoInCategory(photoId: String, categoryId: Long): Boolean

    // ===== Batch Operations =====

    /**
     * Assign multiple photos to a category
     * @param photoIds List of photo IDs
     * @param categoryId The category ID
     */
    @Transaction
    suspend fun assignPhotosToCategory(photoIds: List<String>, categoryId: Long) {
        val joins = photoIds.map { photoId ->
            PhotoCategoryJoin(
                photoId = photoId,
                categoryId = categoryId,
                assignedAt = System.currentTimeMillis()
            )
        }
        insertPhotoCategoryJoins(joins)
    }

    /**
     * Assign a photo to multiple categories
     * @param photoId The photo ID
     * @param categoryIds List of category IDs
     */
    @Transaction
    suspend fun assignPhotoToCategories(photoId: String, categoryIds: List<Long>) {
        val joins = categoryIds.map { categoryId ->
            PhotoCategoryJoin(
                photoId = photoId,
                categoryId = categoryId,
                assignedAt = System.currentTimeMillis()
            )
        }
        insertPhotoCategoryJoins(joins)
    }

    /**
     * Update categories for a photo (replace all existing)
     * @param photoId The photo ID
     * @param categoryIds New list of category IDs
     */
    @Transaction
    suspend fun updatePhotoCategories(photoId: String, categoryIds: List<Long>) {
        removePhotoFromAllCategories(photoId)
        if (categoryIds.isNotEmpty()) {
            assignPhotoToCategories(photoId, categoryIds)
        }
    }

    /**
     * Set primary category for a photo
     * @param photoId The photo ID
     * @param categoryId The category ID to set as primary
     */
    @Query("""
        UPDATE photo_category_join
        SET is_primary = CASE
            WHEN category_id = :categoryId THEN 1
            ELSE 0
        END
        WHERE photo_id = :photoId
    """)
    suspend fun setPrimaryCategory(photoId: String, categoryId: Long)

    /**
     * Get primary category for a photo
     * @param photoId The photo ID
     * @return Primary category or null if none
     */
    @Query("""
        SELECT c.* FROM category_entities c
        INNER JOIN photo_category_join pcj ON c.id = pcj.category_id
        WHERE pcj.photo_id = :photoId AND pcj.is_primary = 1
        LIMIT 1
    """)
    suspend fun getPrimaryCategory(photoId: String): CategoryEntity?
}