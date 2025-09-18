package com.smilepile.database.dao

import androidx.room.*
import com.smilepile.database.entities.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Photo operations
 *
 * Optimized for fast photo loading with pagination support for smooth browsing
 */
@Dao
interface PhotoDao {

    /**
     * Get all photos in a category ordered by display order
     * Optimized query with proper indexing for <50ms performance
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        ORDER BY display_order ASC, created_at ASC
    """)
    fun getPhotosInCategory(categoryId: Long): Flow<List<Photo>>

    /**
     * Get photos in category with pagination for performance
     * Essential for categories with 100+ photos
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        ORDER BY display_order ASC, created_at ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPhotosInCategoryPaged(
        categoryId: Long,
        limit: Int,
        offset: Int
    ): List<Photo>

    /**
     * Get photo by ID
     */
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): Photo?

    /**
     * Get photo by file path
     */
    @Query("SELECT * FROM photos WHERE file_path = :filePath LIMIT 1")
    suspend fun getPhotoByPath(filePath: String): Photo?

    /**
     * Get next photo in category for navigation
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        AND display_order > (
            SELECT display_order FROM photos WHERE id = :currentPhotoId
        )
        ORDER BY display_order ASC
        LIMIT 1
    """)
    suspend fun getNextPhoto(categoryId: Long, currentPhotoId: Long): Photo?

    /**
     * Get previous photo in category for navigation
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        AND display_order < (
            SELECT display_order FROM photos WHERE id = :currentPhotoId
        )
        ORDER BY display_order DESC
        LIMIT 1
    """)
    suspend fun getPreviousPhoto(categoryId: Long, currentPhotoId: Long): Photo?

    /**
     * Get first photo in category (for cover image fallback)
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        ORDER BY display_order ASC, created_at ASC
        LIMIT 1
    """)
    suspend fun getFirstPhotoInCategory(categoryId: Long): Photo?

    /**
     * Get cover image for category
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId AND is_cover_image = 1
        LIMIT 1
    """)
    suspend fun getCoverImageForCategory(categoryId: Long): Photo?

    /**
     * Insert a new photo
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhoto(photo: Photo): Long

    /**
     * Insert multiple photos efficiently
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhotos(photos: List<Photo>): List<Long>

    /**
     * Update photo
     */
    @Update
    suspend fun updatePhoto(photo: Photo)

    /**
     * Update multiple photos
     */
    @Update
    suspend fun updatePhotos(photos: List<Photo>)

    /**
     * Delete photo
     */
    @Delete
    suspend fun deletePhoto(photo: Photo)

    /**
     * Delete photo by ID
     */
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)

    /**
     * Delete all photos in a category
     */
    @Query("DELETE FROM photos WHERE category_id = :categoryId")
    suspend fun deleteAllPhotosInCategory(categoryId: Long)

    /**
     * Update photo display order
     */
    @Query("UPDATE photos SET display_order = :displayOrder WHERE id = :photoId")
    suspend fun updatePhotoDisplayOrder(photoId: Long, displayOrder: Int)

    /**
     * Update photo metadata
     */
    @Query("UPDATE photos SET metadata = :metadata WHERE id = :photoId")
    suspend fun updatePhotoMetadata(photoId: Long, metadata: String)

    /**
     * Get next display order for new photos in category
     */
    @Query("""
        SELECT COALESCE(MAX(display_order), 0) + 1
        FROM photos
        WHERE category_id = :categoryId
    """)
    suspend fun getNextDisplayOrderInCategory(categoryId: Long): Int

    /**
     * Get photo count in category
     */
    @Query("SELECT COUNT(*) FROM photos WHERE category_id = :categoryId")
    suspend fun getPhotoCountInCategory(categoryId: Long): Int

    /**
     * Get photo count in category as Flow
     */
    @Query("SELECT COUNT(*) FROM photos WHERE category_id = :categoryId")
    fun getPhotoCountInCategoryFlow(categoryId: Long): Flow<Int>

    /**
     * Set cover image for category (unset previous cover image)
     */
    @Transaction
    suspend fun setCoverImageForCategory(categoryId: Long, photoId: Long) {
        // Unset previous cover image
        unsetCoverImageForCategory(categoryId)
        // Set new cover image
        setCoverImage(photoId)
    }

    /**
     * Unset cover image for category
     */
    @Query("UPDATE photos SET is_cover_image = 0 WHERE category_id = :categoryId")
    suspend fun unsetCoverImageForCategory(categoryId: Long)

    /**
     * Set photo as cover image
     */
    @Query("UPDATE photos SET is_cover_image = 1 WHERE id = :photoId")
    suspend fun setCoverImage(photoId: Long)

    /**
     * Get photos for category ordered by creation date (for import order)
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentPhotosInCategory(categoryId: Long, limit: Int = 20): List<Photo>

    /**
     * Search photos by file name pattern
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        AND file_path LIKE '%' || :searchTerm || '%'
        ORDER BY display_order ASC
    """)
    suspend fun searchPhotosInCategory(categoryId: Long, searchTerm: String): List<Photo>

    /**
     * Get photos with specific dimensions (for optimization)
     */
    @Query("""
        SELECT * FROM photos
        WHERE category_id = :categoryId
        AND width IS NOT NULL AND height IS NOT NULL
        ORDER BY display_order ASC
    """)
    suspend fun getPhotosWithDimensions(categoryId: Long): List<Photo>

    /**
     * Transaction to safely reorder photos in a category
     */
    @Transaction
    suspend fun reorderPhotosInCategory(photoOrders: List<Pair<Long, Int>>) {
        photoOrders.forEach { (photoId, order) ->
            updatePhotoDisplayOrder(photoId, order)
        }
    }

    /**
     * Batch operation to update photo file sizes
     */
    @Query("UPDATE photos SET file_size_bytes = :fileSizeBytes WHERE id = :photoId")
    suspend fun updatePhotoFileSize(photoId: Long, fileSizeBytes: Long)

    /**
     * Update photo dimensions
     */
    @Query("UPDATE photos SET width = :width, height = :height WHERE id = :photoId")
    suspend fun updatePhotoDimensions(photoId: Long, width: Int, height: Int)

    /**
     * Check if file path already exists (prevent duplicates)
     */
    @Query("SELECT COUNT(*) FROM photos WHERE file_path = :filePath")
    suspend fun isFilePathExists(filePath: String): Int

    /**
     * Get all unique file paths (for cleanup operations)
     */
    @Query("SELECT DISTINCT file_path FROM photos ORDER BY file_path")
    suspend fun getAllFilePaths(): List<String>

    /**
     * Performance: Get photo IDs only for pagination calculations
     */
    @Query("""
        SELECT id FROM photos
        WHERE category_id = :categoryId
        ORDER BY display_order ASC, created_at ASC
    """)
    suspend fun getPhotoIdsInCategory(categoryId: Long): List<Long>
}