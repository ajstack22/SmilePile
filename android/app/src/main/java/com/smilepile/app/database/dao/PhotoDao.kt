package com.smilepile.app.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.smilepile.app.database.entities.PhotoEntity

/**
 * Data Access Object for Photo operations.
 * Provides CRUD operations and category-specific queries for photos.
 */
@Dao
interface PhotoDao {

    /**
     * Get all photos for a specific category as a Flow for reactive updates.
     * Photos are ordered by their position within the category.
     * Only returns non-deleted photos.
     */
    @Query("SELECT * FROM photos WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY position ASC")
    fun getPhotosForCategory(categoryId: String): Flow<List<PhotoEntity>>

    /**
     * Get all photos across all categories as a Flow for reactive updates.
     * Photos are ordered by category ID and then by position.
     * Only returns non-deleted photos.
     */
    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY categoryId ASC, position ASC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    /**
     * Get a single photo by its ID.
     * Returns null if photo doesn't exist.
     */
    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: String): PhotoEntity?

    /**
     * Insert a single photo.
     * Uses REPLACE strategy to handle conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    /**
     * Insert multiple photos in a batch operation.
     * Uses REPLACE strategy to handle conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>): List<Long>

    /**
     * Update an existing photo.
     * Returns the number of rows affected.
     */
    @Update
    suspend fun updatePhoto(photo: PhotoEntity): Int

    /**
     * Delete a specific photo.
     * Returns the number of rows affected.
     */
    @Delete
    suspend fun deletePhoto(photo: PhotoEntity): Int

    /**
     * Delete all photos for a specific category.
     * Useful when deleting a category or clearing category contents.
     */
    @Query("DELETE FROM photos WHERE categoryId = :categoryId")
    suspend fun deletePhotosForCategory(categoryId: String): Int

    /**
     * Get the count of photos for a specific category.
     * Useful for UI state management and validation.
     * Only counts non-deleted photos.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getPhotoCountForCategory(categoryId: String): Int

    /**
     * Get the total count of all photos in the database.
     * Useful for overall UI state management.
     * Only counts non-deleted photos.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 0")
    suspend fun getTotalPhotoCount(): Int

    /**
     * Check if a photo exists by name within a category.
     * Useful for validation before insertion to prevent duplicates.
     * Only checks non-deleted photos.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE name = :name AND categoryId = :categoryId AND isDeleted = 0)")
    suspend fun photoExistsByName(name: String, categoryId: String): Boolean

    /**
     * Get the maximum position value within a category for ordering new photos.
     * Returns 0 if no photos exist in the category.
     * Only considers non-deleted photos.
     */
    @Query("SELECT COALESCE(MAX(position), 0) FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getMaxPositionInCategory(categoryId: String): Int

    /**
     * Update photo positions within a category for reordering.
     * This is an upsert operation using @Insert with REPLACE.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePhotoPositions(photos: List<PhotoEntity>): List<Long>

    /**
     * Get photos by name pattern (for search functionality).
     * Uses LIKE operator with case-insensitive matching.
     */
    @Query("SELECT * FROM photos WHERE name LIKE '%' || :pattern || '%' COLLATE NOCASE ORDER BY categoryId ASC, position ASC")
    fun searchPhotosByName(pattern: String): Flow<List<PhotoEntity>>

    /**
     * Get random photos for display purposes (like featured photos).
     * Limited to a specified count for performance.
     */
    @Query("SELECT * FROM photos ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomPhotos(limit: Int): List<PhotoEntity>

    /**
     * Delete all photos from the database.
     * Use with caution as this removes all photo data.
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos(): Int

    /**
     * Get photos from assets (default source).
     * Useful for filtering by source type.
     * Only returns non-deleted photos.
     */
    @Query("SELECT * FROM photos WHERE isFromAssets = 1 AND isDeleted = 0 ORDER BY categoryId ASC, position ASC")
    fun getPhotosFromAssets(): Flow<List<PhotoEntity>>

    /**
     * Update asset status for a photo.
     * Useful when photos are moved between asset and external storage.
     */
    @Query("UPDATE photos SET isFromAssets = :isFromAssets WHERE id = :photoId")
    suspend fun updatePhotoAssetStatus(photoId: String, isFromAssets: Boolean): Int

    /**
     * Batch update asset status for multiple photos.
     * More efficient than individual updates.
     */
    @Query("UPDATE photos SET isFromAssets = :isFromAssets WHERE id IN (:photoIds)")
    suspend fun updatePhotosAssetStatus(photoIds: List<String>, isFromAssets: Boolean): Int

    // ========== SOFT DELETE OPERATIONS ==========

    /**
     * Soft delete photos for a category when category is deleted.
     * Photos are marked as deleted but not physically removed from database.
     * This prevents data loss and allows for potential recovery.
     */
    @Query("UPDATE photos SET isDeleted = 1, deletedAt = :timestamp WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun softDeletePhotosForCategory(categoryId: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Restore soft-deleted photos for a category.
     * Used when undoing category deletion or reassigning photos to new category.
     */
    @Query("UPDATE photos SET isDeleted = 0, deletedAt = null WHERE categoryId = :categoryId AND isDeleted = 1")
    suspend fun restorePhotosForCategory(categoryId: String): Int

    /**
     * Soft delete a single photo.
     * Marks photo as deleted without removing from database.
     */
    @Query("UPDATE photos SET isDeleted = 1, deletedAt = :timestamp WHERE id = :photoId")
    suspend fun softDeletePhoto(photoId: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Restore a soft-deleted photo.
     * Marks photo as active again.
     */
    @Query("UPDATE photos SET isDeleted = 0, deletedAt = null WHERE id = :photoId")
    suspend fun restorePhoto(photoId: String): Int

    /**
     * Get count of non-deleted photos for category validation.
     * Used to check if category can be safely deleted.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getActivePhotoCountForCategory(categoryId: String): Int

    /**
     * Get all soft-deleted photos for recovery purposes.
     * Returns photos that were deleted but not permanently removed.
     */
    @Query("SELECT * FROM photos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getSoftDeletedPhotos(): List<PhotoEntity>

    /**
     * Permanently delete old soft-deleted photos (housekeeping).
     * Used for cleanup - removes photos that have been soft-deleted for too long.
     * @param cutoffTime Photos deleted before this timestamp will be permanently removed
     */
    @Query("DELETE FROM photos WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun cleanupDeletedPhotos(cutoffTime: Long): Int

    /**
     * Get photos that have null categoryId (orphaned after category deletion).
     * These photos need special handling in the UI.
     */
    @Query("SELECT * FROM photos WHERE categoryId IS NULL AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getOrphanedPhotos(): Flow<List<PhotoEntity>>

    /**
     * Count orphaned photos (photos with null categoryId).
     * Useful for UI notifications about photos needing category assignment.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE categoryId IS NULL AND isDeleted = 0")
    suspend fun getOrphanedPhotoCount(): Int

    // ========== TRANSACTION-BASED BATCH OPERATIONS ==========

    /**
     * Update photo position with transaction support.
     * Used for reordering operations to ensure consistency.
     */
    @Transaction
    suspend fun updatePhotoPosition(photoId: String, newPosition: Int) {
        val photo = getPhotoById(photoId)
        photo?.let {
            updatePhoto(it.copy(position = newPosition))
        }
    }

    /**
     * Swap positions of two photos atomically.
     * Ensures both photos are updated or neither is updated.
     */
    @Transaction
    suspend fun swapPhotoPositions(photoId1: String, photoId2: String) {
        val photo1 = getPhotoById(photoId1)
        val photo2 = getPhotoById(photoId2)

        if (photo1 != null && photo2 != null) {
            val tempPosition = photo1.position
            updatePhoto(photo1.copy(position = photo2.position))
            updatePhoto(photo2.copy(position = tempPosition))
        }
    }

    /**
     * Batch update photo categories with transaction support.
     * Moves multiple photos to a new category atomically.
     */
    @Transaction
    suspend fun batchUpdatePhotoCategories(photoIds: List<String>, newCategoryId: String) {
        photoIds.forEach { photoId ->
            val photo = getPhotoById(photoId)
            photo?.let {
                updatePhoto(it.copy(categoryId = newCategoryId))
            }
        }
    }

    /**
     * Reorder all photos in a category with transaction support.
     * Updates positions for all photos in the specified order.
     */
    @Transaction
    suspend fun reorderPhotosInCategory(categoryId: String, orderedPhotoIds: List<String>) {
        orderedPhotoIds.forEachIndexed { index, photoId ->
            val photo = getPhotoById(photoId)
            if (photo?.categoryId == categoryId) {
                updatePhoto(photo.copy(position = index))
            }
        }
    }

    /**
     * Batch soft delete photos with transaction support.
     * Ensures all photos are deleted atomically or none are deleted.
     */
    @Transaction
    suspend fun batchSoftDeletePhotos(photoIds: List<String>): Int {
        val timestamp = System.currentTimeMillis()
        var deletedCount = 0

        photoIds.forEach { photoId ->
            val result = softDeletePhoto(photoId, timestamp)
            deletedCount += result
        }

        return deletedCount
    }

    /**
     * Batch restore soft-deleted photos with transaction support.
     * Ensures all photos are restored atomically or none are restored.
     */
    @Transaction
    suspend fun batchRestorePhotos(photoIds: List<String>): Int {
        var restoredCount = 0

        photoIds.forEach { photoId ->
            val result = restorePhoto(photoId)
            restoredCount += result
        }

        return restoredCount
    }
}