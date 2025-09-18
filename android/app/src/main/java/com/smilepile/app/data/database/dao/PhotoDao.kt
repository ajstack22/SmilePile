package com.smilepile.app.data.database.dao

import androidx.room.*
import com.smilepile.app.data.database.entities.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Photo entity.
 * Contains all database queries for photos used in performance testing.
 */
@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE isDeleted = 0")
    suspend fun getAllPhotos(): List<Photo>

    @Query("SELECT * FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getPhotosByCategory(categoryId: Long): List<Photo>

    @Query("SELECT * FROM photos WHERE categoryId = :categoryId AND isDeleted = 0 LIMIT :limit OFFSET :offset")
    suspend fun getPhotosByCategoryPaged(categoryId: Long, limit: Int, offset: Int): List<Photo>

    @Query("SELECT * FROM photos WHERE albumId = :albumId AND isDeleted = 0")
    suspend fun getPhotosByAlbum(albumId: Long): List<Photo>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 AND isDeleted = 0")
    suspend fun getFavoritePhotos(): List<Photo>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): Photo?

    @Query("SELECT COUNT(*) FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun getPhotoCountByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 0")
    suspend fun getTotalPhotoCount(): Int

    @Insert
    suspend fun insertPhoto(photo: Photo): Long

    @Insert
    suspend fun insertPhotos(photos: List<Photo>): List<Long>

    @Update
    suspend fun updatePhoto(photo: Photo)

    @Delete
    suspend fun deletePhoto(photo: Photo)

    @Query("UPDATE photos SET isDeleted = 1 WHERE id = :id")
    suspend fun markPhotoAsDeleted(id: Long)

    @Query("DELETE FROM photos WHERE isDeleted = 1")
    suspend fun permanentlyDeleteMarkedPhotos()

    // Flow-based queries for reactive UI
    @Query("SELECT * FROM photos WHERE categoryId = :categoryId AND isDeleted = 0")
    fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY dateCreated DESC")
    fun getAllPhotosFlow(): Flow<List<Photo>>

    // Performance testing queries
    @Query("SELECT * FROM photos WHERE categoryId IN (:categoryIds) AND isDeleted = 0")
    suspend fun getPhotosByMultipleCategories(categoryIds: List<Long>): List<Photo>

    @Query("SELECT * FROM photos WHERE fileName LIKE :searchTerm AND isDeleted = 0")
    suspend fun searchPhotosByName(searchTerm: String): List<Photo>

    @Query("SELECT * FROM photos WHERE fileSize > :minSize AND fileSize < :maxSize AND isDeleted = 0")
    suspend fun getPhotosByFileSize(minSize: Long, maxSize: Long): List<Photo>
}