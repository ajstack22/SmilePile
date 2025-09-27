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
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of PhotoRepository that uses Room database through PhotoDao
 * Handles data mapping between domain models (Photo) and database entities (PhotoEntity)
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoRepository {
    // Cache to reduce database queries and improve performance
    private val photoCacheById = ConcurrentHashMap<Long, Photo>()

    /**
     * Maps PhotoEntity to Photo domain model
     * Uses a stable hash of the URI to generate a consistent Long ID
     */
    private fun PhotoEntity.toPhoto(): Photo {
        return Photo(
            id = generateStableIdFromUri(this.uri),
            path = this.uri,
            categoryId = this.categoryId,
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
     * Generates a stable Long ID from a URI string
     * Uses a combination of URI hash and length to minimize collisions
     */
    private fun generateStableIdFromUri(uri: String): Long {
        // Use a more stable hash calculation to minimize collisions
        // Combine the absolute value of hashCode with string length for better distribution
        val hash = uri.hashCode()
        val length = uri.length
        // Create a composite ID using both hash and length, ensuring it's always positive
        return (kotlin.math.abs(hash).toLong() shl 16) or (length.toLong() and 0xFFFF)
    }

    /**
     * Maps Photo domain model to PhotoEntity
     * For new photos (id == 0L), generates a new UUID
     * For existing photos, we need to find the actual entity by URI since we can't reliably map Long ID back to UUID
     */
    private suspend fun Photo.toPhotoEntity(): PhotoEntity {
        return if (this.id == 0L) {
            // New photo - generate new UUID
            PhotoEntity(
                id = java.util.UUID.randomUUID().toString(),
                uri = this.path,
                categoryId = this.categoryId,
                timestamp = this.createdAt,
                isFavorite = this.isFavorite
            )
        } else {
            // Existing photo - find by URI to get the actual UUID
            val existingEntity = photoDao.getByUri(this.path)
            if (existingEntity != null) {
                // Update existing entity
                existingEntity.copy(
                    categoryId = this.categoryId,
                    timestamp = this.createdAt,
                    isFavorite = this.isFavorite
                )
            } else {
                // Photo not found by URI, create new one with deterministic UUID
                PhotoEntity(
                    id = generateDeterministicUuidFromUri(this.path),
                    uri = this.path,
                    categoryId = this.categoryId,
                    timestamp = this.createdAt,
                    isFavorite = this.isFavorite
                )
            }
        }
    }

    /**
     * Generates a deterministic UUID string from a URI
     * This ensures we can always map the same URI to the same UUID
     */
    private fun generateDeterministicUuidFromUri(uri: String): String {
        // Create a deterministic UUID based on the URI
        // This approach ensures we can always map the same URI to the same UUID
        val hash = uri.hashCode()
        val uuidString = String.format(
            "%08x-%04x-%04x-%04x-%012x",
            hash,
            (hash shr 16) and 0xFFFF,
            (hash shr 8) and 0xFFFF,
            hash and 0xFFFF,
            uri.length.toLong()
        )
        return uuidString
    }

    override suspend fun insertPhoto(photo: Photo): Long = withContext(ioDispatcher) {
        try {
            // Ensure photo has a valid category (mandatory)
            if (photo.categoryId <= 0) {
                throw PhotoRepositoryException("Photo must have a valid category. Category ID: ${photo.categoryId}")
            }
            val photoEntity = photo.toPhotoEntity()
            val rowId = photoDao.insert(photoEntity)
            rowId
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to insert photo: ${e.message}", e)
        }
    }

    override suspend fun insertPhotos(photos: List<Photo>): Unit = withContext(ioDispatcher) {
        try {
            // Ensure all photos have valid categories (mandatory)
            val invalidPhotos = photos.filter { it.categoryId <= 0 }
            if (invalidPhotos.isNotEmpty()) {
                throw PhotoRepositoryException("All photos must have valid categories. ${invalidPhotos.size} photos have invalid category IDs.")
            }
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
            // Since we can't reliably map Long ID back to UUID, we need to find by generated ID
            // This is a limitation of the current approach - we'll need to search all photos
            val allPhotos = photoDao.getAll().first()
            val targetPhoto = allPhotos.find { generateStableIdFromUri(it.uri) == photoId }

            if (targetPhoto != null) {
                val rowsAffected = photoDao.deleteById(targetPhoto.id)
                if (rowsAffected == 0) {
                    throw PhotoRepositoryException("Photo not found for deletion: $photoId")
                }
            } else {
                throw PhotoRepositoryException("Photo not found for deletion: $photoId")
            }
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to delete photo by ID: ${e.message}", e)
        }
    }

    override suspend fun getPhotoById(photoId: Long): Photo? = withContext(ioDispatcher) {
        try {
            // Since we can't reliably map Long ID back to UUID, we need to find by generated ID
            val allPhotos = photoDao.getAll().first()
            allPhotos.find { generateStableIdFromUri(it.uri) == photoId }?.toPhoto()
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo by ID: ${e.message}", e)
        }
    }

    override suspend fun getPhotosByCategory(categoryId: Long): List<Photo> = withContext(ioDispatcher) {
        try {
            // Since PhotoDao.getByCategory returns Flow, we need to get the first emission
            photoDao.getByCategory(categoryId).first().map { it.toPhoto() }
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photos by category: ${e.message}", e)
        }
    }

    override fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>> {
        return photoDao.getByCategory(categoryId).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override suspend fun getPhotosInCategories(categoryIds: List<Long>): List<Photo> = withContext(ioDispatcher) {
        try {
            if (categoryIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Get photos from all specified categories (OR operation)
            val allPhotos = mutableSetOf<PhotoEntity>()
            categoryIds.forEach { categoryId ->
                allPhotos.addAll(photoDao.getByCategory(categoryId).first())
            }
            allPhotos.map { it.toPhoto() }
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photos in categories: ${e.message}", e)
        }
    }

    override fun getPhotosInCategoriesFlow(categoryIds: List<Long>): Flow<List<Photo>> {
        return if (categoryIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            // Combine flows from multiple categories
            kotlinx.coroutines.flow.combine(
                categoryIds.map { categoryId ->
                    photoDao.getByCategory(categoryId)
                }
            ) { arrays ->
                // Combine all photos and remove duplicates
                arrays.flatMap { it.toList() }
                    .distinctBy { it.id }
                    .map { it.toPhoto() }
            }
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
            photoDao.deleteByCategory(categoryId)
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
            photoDao.getPhotoCountByCategory(categoryId)
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo count for category: ${e.message}", e)
        }
    }

    override suspend fun getPhotoByPath(path: String): Photo? = withContext(ioDispatcher) {
        try {
            // Find photo entity by URI/path
            val photoEntity = photoDao.getByUri(path)
            photoEntity?.toPhoto()
        } catch (e: Exception) {
            throw PhotoRepositoryException("Failed to get photo by path: ${e.message}", e)
        }
    }

    // Search and filter methods implementation
    override fun searchPhotos(searchQuery: String): Flow<List<Photo>> {
        return photoDao.searchPhotos(searchQuery).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<Photo>> {
        return photoDao.searchPhotosInCategory(searchQuery, categoryId).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<Photo>> {
        return photoDao.getPhotosByDateRange(startDate, endDate).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    override fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<Photo>> {
        return photoDao.getPhotosByDateRangeAndCategory(startDate, endDate, categoryId).map { photoEntities ->
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
            categoryId = categoryId ?: 0L
        ).map { photoEntities ->
            photoEntities.map { it.toPhoto() }
        }
    }

    // Remove from library methods - only delete from app database, NOT device storage
    override suspend fun removeFromLibrary(photo: Photo): Unit = withContext(ioDispatcher) {
        try {
            android.util.Log.d("PhotoRepository", "Removing photo from library (app only): ${photo.id}, path: ${photo.path}")
            // Use safer deleteByUri method that's atomic and avoids ID collision issues
            val rowsAffected = photoDao.deleteByUri(photo.path)
            if (rowsAffected == 0) {
                throw PhotoRepositoryException("Photo not found for removal from library: ${photo.path}")
            }
            // Clear cache entry if it exists
            photoCacheById.remove(photo.id)
            android.util.Log.d("PhotoRepository", "Successfully removed photo from library: ${photo.id}")
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to remove photo from library: ${e.message}", e)
        }
    }

    override suspend fun removeFromLibraryById(photoId: Long): Unit = withContext(ioDispatcher) {
        try {
            android.util.Log.d("PhotoRepository", "Removing photo from library by ID (app only): $photoId")

            // Since we can't reliably map Long ID back to UUID, we need to find by generated ID
            val allPhotos = photoDao.getAll().first()
            val targetPhoto = allPhotos.find { generateStableIdFromUri(it.uri) == photoId }

            if (targetPhoto != null) {
                val rowsAffected = photoDao.deleteById(targetPhoto.id)
                if (rowsAffected == 0) {
                    throw PhotoRepositoryException("Photo not found for removal from library: $photoId")
                }
                android.util.Log.d("PhotoRepository", "Successfully removed photo from library by ID: $photoId")
            } else {
                throw PhotoRepositoryException("Photo not found for removal from library: $photoId")
            }
        } catch (e: Exception) {
            if (e is PhotoRepositoryException) throw e
            throw PhotoRepositoryException("Failed to remove photo from library by ID: ${e.message}", e)
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