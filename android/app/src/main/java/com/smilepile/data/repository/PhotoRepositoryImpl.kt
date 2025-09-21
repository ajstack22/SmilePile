package com.smilepile.data.repository

import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.models.Photo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.smilepile.di.IoDispatcher

/**
 * Implementation of PhotoRepository that uses Room database through PhotoDao
 * Handles data mapping between domain models (Photo) and database entities (PhotoEntity)
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoRepository {

    /**
     * Maps PhotoEntity to Photo domain model
     */
    private fun PhotoEntity.toPhoto(): Photo {
        return Photo(
            id = this.id.hashCode().toLong(), // Convert UUID string to Long for compatibility
            path = this.uri,
            categoryId = this.categoryId.toLongOrNull() ?: 0L,
            name = this.uri.substringAfterLast("/").substringBeforeLast("."),
            isFromAssets = false,
            createdAt = this.timestamp,
            fileSize = 0L, // PhotoEntity doesn't store file size
            width = 0, // PhotoEntity doesn't store dimensions
            height = 0, // PhotoEntity doesn't store dimensions
            isFavorite = this.isFavorite
        )
    }

    /**
     * Maps Photo domain model to PhotoEntity
     */
    private fun Photo.toPhotoEntity(): PhotoEntity {
        return PhotoEntity(
            id = if (this.id == 0L) java.util.UUID.randomUUID().toString() else this.id.toString(),
            uri = this.path,
            categoryId = this.categoryId.toString(),
            timestamp = this.createdAt,
            isFavorite = this.isFavorite
        )
    }

    override suspend fun insertPhoto(photo: Photo): Long = withContext(ioDispatcher) {
        try {
            val photoEntity = photo.toPhotoEntity()
            val rowId = photoDao.insert(photoEntity)
            rowId
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to insert photo: ${e.message}", e)
        }
    }

    override suspend fun insertPhotos(photos: List<Photo>): Unit = withContext(ioDispatcher) {
        try {
            val photoEntities = photos.map { it.toPhotoEntity() }
            photoDao.insertAll(photoEntities)
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to insert photos: ${e.message}", e)
        }
    }

    override suspend fun updatePhoto(photo: Photo): Unit = withContext(ioDispatcher) {
        try {
            val photoEntity = photo.toPhotoEntity()
            val rowsAffected = photoDao.update(photoEntity)
            if (rowsAffected == 0) {
                throw PhotoRepositoryException("Photo not found for update: ${photo.id}")
            }
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to update photo: ${e.message}", e)
        }
    }

    override suspend fun deletePhoto(photo: Photo): Unit = withContext(ioDispatcher) {
        try {
            val photoEntity = photo.toPhotoEntity()
            val rowsAffected = photoDao.delete(photoEntity)
            if (rowsAffected == 0) {
                throw PhotoRepositoryException("Photo not found for deletion: ${photo.id}")
            }
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to delete photo: ${e.message}", e)
        }
    }

    override suspend fun deletePhotoById(photoId: Long): Unit = withContext(ioDispatcher) {
        try {
            val rowsAffected = photoDao.deleteById(photoId.toString())
            if (rowsAffected == 0) {
                throw PhotoRepositoryException("Photo not found for deletion: $photoId")
            }
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to delete photo by ID: ${e.message}", e)
        }
    }

    override suspend fun getPhotoById(photoId: Long): Photo? = withContext(ioDispatcher) {
        try {
            photoDao.getById(photoId.toString())?.toPhoto()
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo by ID: ${e.message}", e)
        }
    }

    override suspend fun getPhotosByCategory(categoryId: Long): List<Photo> = withContext(ioDispatcher) {
        try {
            // Since PhotoDao.getByCategory returns Flow, we need to get the first emission
            photoDao.getByCategory(categoryId.toString()).first().map { it.toPhoto() }
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photos by category: ${e.message}", e)
        }
    }

    override fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>> {
        return photoDao.getByCategory(categoryId.toString()).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override suspend fun getAllPhotos(): List<Photo> = withContext(ioDispatcher) {
        try {
            // Since PhotoDao.getAll returns Flow, we need to get the first emission
            photoDao.getAll().first().map { it.toPhoto() }
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get all photos: ${e.message}", e)
        }
    }

    override fun getAllPhotosFlow(): Flow<List<Photo>> {
        return photoDao.getAll().map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override suspend fun deletePhotosByCategory(categoryId: Long): Unit = withContext(ioDispatcher) {
        try {
            photoDao.deleteByCategory(categoryId.toString())
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to delete photos by category: ${e.message}", e)
        }
    }

    override suspend fun getPhotoCount(): Int = withContext(ioDispatcher) {
        try {
            // PhotoDao doesn't have a total count method, so we'll get all and count
            photoDao.getAll().first().size
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo count: ${e.message}", e)
        }
    }

    override suspend fun getPhotoCategoryCount(categoryId: Long): Int = withContext(ioDispatcher) {
        try {
            photoDao.getPhotoCountByCategory(categoryId.toString())
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo count for category: ${e.message}", e)
        }
    }

    // Search and filter methods implementation
    override fun searchPhotos(searchQuery: String): Flow<List<Photo>> {
        return photoDao.searchPhotos(searchQuery).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<Photo>> {
        return photoDao.searchPhotosInCategory(searchQuery, categoryId.toString()).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<Photo>> {
        return photoDao.getPhotosByDateRange(startDate, endDate).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<Photo>> {
        return photoDao.getPhotosByDateRangeAndCategory(startDate, endDate, categoryId.toString()).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun searchPhotosWithFilters(
        searchQuery: String,
        startDate: Long,
        endDate: Long,
        favoritesOnly: Boolean?,
        categoryId: Long?
    ): Flow<List<Photo>> {
        return photoDao.searchPhotosWithFilters(
            searchQuery = searchQuery,
            startDate = startDate,
            endDate = endDate,
            favoritesOnly = favoritesOnly,
            categoryId = categoryId?.toString() ?: ""
        ).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }
}

/**
 * Custom exception for PhotoRepository operations
 */
class PhotoRepositoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)