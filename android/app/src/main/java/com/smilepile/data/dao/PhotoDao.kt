package com.smilepile.data.dao

import androidx.room.*
import com.smilepile.data.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Photo operations
 * Provides CRUD operations for PhotoEntity with Room database
 */
@Dao
interface PhotoDao {

    /**
     * Insert a new photo into the database
     * @param photo The photo entity to insert
     * @return The row ID of the inserted photo
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    /**
     * Insert multiple photos into the database
     * @param photos List of photo entities to insert
     * @return List of row IDs of the inserted photos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>): List<Long>

    /**
     * Update an existing photo in the database
     * @param photo The photo entity to update
     * @return Number of rows affected
     */
    @Update
    suspend fun update(photo: PhotoEntity): Int

    /**
     * Delete a photo from the database
     * @param photo The photo entity to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun delete(photo: PhotoEntity): Int

    /**
     * Delete a photo by its ID
     * @param photoId The ID of the photo to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM photo_entities WHERE id = :photoId")
    suspend fun deleteById(photoId: String): Int

    /**
     * Get all photos from the database as a reactive Flow
     * @return Flow of list of all photos, ordered by timestamp descending
     */
    @Query("SELECT * FROM photo_entities ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PhotoEntity>>

    /**
     * Get a photo by its ID
     * @param photoId The ID of the photo to retrieve
     * @return The photo entity if found, null otherwise
     */
    @Query("SELECT * FROM photo_entities WHERE id = :photoId")
    suspend fun getById(photoId: String): PhotoEntity?

    /**
     * Get a photo by its ID as a reactive Flow
     * @param photoId The ID of the photo to retrieve
     * @return Flow of the photo entity
     */
    @Query("SELECT * FROM photo_entities WHERE id = :photoId")
    fun getByIdFlow(photoId: String): Flow<PhotoEntity?>

    /**
     * Get all photos in a specific category as a reactive Flow
     * @param categoryId The ID of the category
     * @return Flow of list of photos in the category, ordered by timestamp descending
     */
    @Query("SELECT * FROM photo_entities WHERE category_id = :categoryId ORDER BY timestamp DESC")
    fun getByCategory(categoryId: String): Flow<List<PhotoEntity>>

    /**
     * Get all favorite photos as a reactive Flow
     * @return Flow of list of favorite photos, ordered by timestamp descending
     */
    @Query("SELECT * FROM photo_entities WHERE is_favorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<PhotoEntity>>

    /**
     * Update the favorite status of a photo
     * @param photoId The ID of the photo to update
     * @param isFavorite The new favorite status
     * @return Number of rows affected
     */
    @Query("UPDATE photo_entities SET is_favorite = :isFavorite WHERE id = :photoId")
    suspend fun updateFavoriteStatus(photoId: String, isFavorite: Boolean): Int

    /**
     * Get the count of photos in a specific category
     * @param categoryId The ID of the category
     * @return Number of photos in the category
     */
    @Query("SELECT COUNT(*) FROM photo_entities WHERE category_id = :categoryId")
    suspend fun getPhotoCountByCategory(categoryId: String): Int

    /**
     * Get the count of photos in a specific category as a reactive Flow
     * @param categoryId The ID of the category
     * @return Flow of the number of photos in the category
     */
    @Query("SELECT COUNT(*) FROM photo_entities WHERE category_id = :categoryId")
    fun getPhotoCountByCategoryFlow(categoryId: String): Flow<Int>

    /**
     * Delete all photos in a specific category
     * @param categoryId The ID of the category
     * @return Number of rows affected
     */
    @Query("DELETE FROM photo_entities WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String): Int

    /**
     * Search photos by name/path with LIKE pattern
     * @param searchQuery The search query to match against photo names/paths
     * @return Flow of list of photos matching the search query
     */
    @Query("SELECT * FROM photo_entities WHERE uri LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchPhotos(searchQuery: String): Flow<List<PhotoEntity>>

    /**
     * Search photos by name/path within a specific category
     * @param searchQuery The search query to match against photo names/paths
     * @param categoryId The ID of the category to search within
     * @return Flow of list of photos matching the search query in the category
     */
    @Query("SELECT * FROM photo_entities WHERE uri LIKE '%' || :searchQuery || '%' AND category_id = :categoryId ORDER BY timestamp DESC")
    fun searchPhotosInCategory(searchQuery: String, categoryId: String): Flow<List<PhotoEntity>>

    /**
     * Get photos within a date range
     * @param startDate Start of the date range (timestamp)
     * @param endDate End of the date range (timestamp)
     * @return Flow of list of photos within the date range
     */
    @Query("SELECT * FROM photo_entities WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<PhotoEntity>>

    /**
     * Get photos within a date range and category
     * @param startDate Start of the date range (timestamp)
     * @param endDate End of the date range (timestamp)
     * @param categoryId The ID of the category
     * @return Flow of list of photos within the date range and category
     */
    @Query("SELECT * FROM photo_entities WHERE timestamp BETWEEN :startDate AND :endDate AND category_id = :categoryId ORDER BY timestamp DESC")
    fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: String): Flow<List<PhotoEntity>>

    /**
     * Search photos with multiple filters: text search, date range, favorites, and category
     * @param searchQuery The search query (empty string means no text filter)
     * @param startDate Start of the date range (0 means no start limit)
     * @param endDate End of the date range (Long.MAX_VALUE means no end limit)
     * @param favoritesOnly Whether to filter only favorites (null means all photos)
     * @param categoryId The category ID (empty string means all categories)
     * @return Flow of list of filtered photos
     */
    @Query("""
        SELECT * FROM photo_entities
        WHERE (:searchQuery = '' OR uri LIKE '%' || :searchQuery || '%')
        AND timestamp BETWEEN :startDate AND :endDate
        AND (:favoritesOnly IS NULL OR is_favorite = :favoritesOnly)
        AND (:categoryId = '' OR category_id = :categoryId)
        ORDER BY timestamp DESC
    """)
    fun searchPhotosWithFilters(
        searchQuery: String,
        startDate: Long,
        endDate: Long,
        favoritesOnly: Boolean?,
        categoryId: String
    ): Flow<List<PhotoEntity>>
}