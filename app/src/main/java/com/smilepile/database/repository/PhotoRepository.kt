package com.smilepile.database.repository

import com.smilepile.database.dao.PhotoDao
import com.smilepile.database.entities.Photo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Photo operations
 *
 * Provides a clean API for photo management with pagination and performance optimization
 */
@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao
) {

    /**
     * Get all photos in a category as Flow
     */
    fun getPhotosInCategory(categoryId: Long): Flow<List<Photo>> =
        photoDao.getPhotosInCategory(categoryId)

    /**
     * Get photos with pagination for performance
     */
    suspend fun getPhotosInCategoryPaged(
        categoryId: Long,
        page: Int = 0,
        pageSize: Int = 20
    ): Result<List<Photo>> = runCatching {
        val offset = page * pageSize
        photoDao.getPhotosInCategoryPaged(categoryId, pageSize, offset)
    }

    /**
     * Get photo by ID
     */
    suspend fun getPhotoById(photoId: Long): Result<Photo?> = runCatching {
        photoDao.getPhotoById(photoId)
    }

    /**
     * Get photo by file path
     */
    suspend fun getPhotoByPath(filePath: String): Result<Photo?> = runCatching {
        photoDao.getPhotoByPath(filePath)
    }

    /**
     * Add new photo to category
     */
    suspend fun addPhoto(
        categoryId: Long,
        filePath: String,
        metadata: String? = null
    ): Result<Long> = runCatching {
        // Check if file already exists
        if (photoDao.isFilePathExists(filePath) > 0) {
            throw IllegalArgumentException("Photo already exists: $filePath")
        }

        val displayOrder = photoDao.getNextDisplayOrderInCategory(categoryId)
        val photo = Photo.create(
            categoryId = categoryId,
            filePath = filePath,
            displayOrder = displayOrder,
            metadata = metadata
        )
        photoDao.insertPhoto(photo)
    }

    /**
     * Add multiple photos efficiently
     */
    suspend fun addPhotos(
        categoryId: Long,
        filePaths: List<String>
    ): Result<List<Long>> = runCatching {
        val existingPaths = photoDao.getAllFilePaths().toSet()
        val newPaths = filePaths.filterNot { it in existingPaths }

        if (newPaths.isEmpty()) {
            throw IllegalArgumentException("All photos already exist")
        }

        val startOrder = photoDao.getNextDisplayOrderInCategory(categoryId)
        val photos = newPaths.mapIndexed { index, path ->
            Photo.create(
                categoryId = categoryId,
                filePath = path,
                displayOrder = startOrder + index
            )
        }
        photoDao.insertPhotos(photos)
    }

    /**
     * Update photo
     */
    suspend fun updatePhoto(photo: Photo): Result<Unit> = runCatching {
        photoDao.updatePhoto(photo)
    }

    /**
     * Update photo metadata
     */
    suspend fun updatePhotoMetadata(photoId: Long, metadata: String): Result<Unit> = runCatching {
        photoDao.updatePhotoMetadata(photoId, metadata)
    }

    /**
     * Update photo dimensions
     */
    suspend fun updatePhotoDimensions(
        photoId: Long,
        width: Int,
        height: Int
    ): Result<Unit> = runCatching {
        photoDao.updatePhotoDimensions(photoId, width, height)
    }

    /**
     * Delete photo
     */
    suspend fun deletePhoto(photoId: Long): Result<Unit> = runCatching {
        photoDao.deletePhotoById(photoId)
    }

    /**
     * Delete all photos in category
     */
    suspend fun deleteAllPhotosInCategory(categoryId: Long): Result<Unit> = runCatching {
        photoDao.deleteAllPhotosInCategory(categoryId)
    }

    /**
     * Get next photo for navigation
     */
    suspend fun getNextPhoto(categoryId: Long, currentPhotoId: Long): Result<Photo?> = runCatching {
        photoDao.getNextPhoto(categoryId, currentPhotoId)
    }

    /**
     * Get previous photo for navigation
     */
    suspend fun getPreviousPhoto(categoryId: Long, currentPhotoId: Long): Result<Photo?> = runCatching {
        photoDao.getPreviousPhoto(categoryId, currentPhotoId)
    }

    /**
     * Get first photo in category
     */
    suspend fun getFirstPhotoInCategory(categoryId: Long): Result<Photo?> = runCatching {
        photoDao.getFirstPhotoInCategory(categoryId)
    }

    /**
     * Get cover image for category
     */
    suspend fun getCoverImageForCategory(categoryId: Long): Result<Photo?> = runCatching {
        photoDao.getCoverImageForCategory(categoryId)
            ?: photoDao.getFirstPhotoInCategory(categoryId) // Fallback to first photo
    }

    /**
     * Set cover image for category
     */
    suspend fun setCoverImageForCategory(categoryId: Long, photoId: Long): Result<Unit> = runCatching {
        photoDao.setCoverImageForCategory(categoryId, photoId)
    }

    /**
     * Get photo count in category
     */
    suspend fun getPhotoCountInCategory(categoryId: Long): Result<Int> = runCatching {
        photoDao.getPhotoCountInCategory(categoryId)
    }

    /**
     * Get photo count in category as Flow
     */
    fun getPhotoCountInCategoryFlow(categoryId: Long): Flow<Int> =
        photoDao.getPhotoCountInCategoryFlow(categoryId)

    /**
     * Reorder photos in category
     */
    suspend fun reorderPhotosInCategory(
        photoOrders: List<Pair<Long, Int>>
    ): Result<Unit> = runCatching {
        photoDao.reorderPhotosInCategory(photoOrders)
    }

    /**
     * Search photos in category
     */
    suspend fun searchPhotosInCategory(
        categoryId: Long,
        searchTerm: String
    ): Result<List<Photo>> = runCatching {
        photoDao.searchPhotosInCategory(categoryId, searchTerm)
    }

    /**
     * Get recent photos in category
     */
    suspend fun getRecentPhotosInCategory(
        categoryId: Long,
        limit: Int = 20
    ): Result<List<Photo>> = runCatching {
        photoDao.getRecentPhotosInCategory(categoryId, limit)
    }

    /**
     * Check if file path exists
     */
    suspend fun isFilePathExists(filePath: String): Result<Boolean> = runCatching {
        photoDao.isFilePathExists(filePath) > 0
    }

    /**
     * Get all file paths (for cleanup)
     */
    suspend fun getAllFilePaths(): Result<List<String>> = runCatching {
        photoDao.getAllFilePaths()
    }

    /**
     * Performance: Get photo IDs for pagination calculations
     */
    suspend fun getPhotoIdsInCategory(categoryId: Long): Result<List<Long>> = runCatching {
        photoDao.getPhotoIdsInCategory(categoryId)
    }

    /**
     * Validate photo file
     */
    suspend fun validatePhotoFile(filePath: String): Result<Boolean> = runCatching {
        val photo = Photo.create(0, filePath)
        photo.isValidImageFile()
    }

    /**
     * Update photo file size
     */
    suspend fun updatePhotoFileSize(photoId: Long, fileSizeBytes: Long): Result<Unit> = runCatching {
        photoDao.updatePhotoFileSize(photoId, fileSizeBytes)
    }

    /**
     * Get photos with dimensions for optimization
     */
    suspend fun getPhotosWithDimensions(categoryId: Long): Result<List<Photo>> = runCatching {
        photoDao.getPhotosWithDimensions(categoryId)
    }
}