package com.smilepile.data.repository

import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.models.PhotoMetadata
import com.smilepile.security.MetadataEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for secure photo operations with metadata encryption
 * Handles encrypted storage and retrieval of sensitive child data
 */
@Singleton
class SecurePhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val metadataEncryption: MetadataEncryption
) {

    /**
     * Inserts a photo with encrypted metadata
     * @param photoMetadata Photo metadata with sensitive fields
     * @return ID of inserted photo
     */
    suspend fun insertSecurePhoto(photoMetadata: PhotoMetadata): String = withContext(Dispatchers.IO) {
        val entity = if (photoMetadata.hasSensitiveData()) {
            photoMetadata.toEntity(metadataEncryption)
        } else {
            photoMetadata.toEntityBasic()
        }

        photoDao.insertPhoto(entity)
        entity.id
    }

    /**
     * Updates a photo with encrypted metadata
     * @param photoMetadata Updated photo metadata
     */
    suspend fun updateSecurePhoto(photoMetadata: PhotoMetadata) = withContext(Dispatchers.IO) {
        val entity = if (photoMetadata.hasSensitiveData()) {
            photoMetadata.toEntity(metadataEncryption)
        } else {
            photoMetadata.toEntityBasic()
        }

        photoDao.updatePhoto(entity)
    }

    /**
     * Gets a photo by ID with decrypted metadata
     * @param id Photo ID
     * @return PhotoMetadata with decrypted sensitive fields, or null if not found
     */
    suspend fun getSecurePhotoById(id: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        val entity = photoDao.getPhotoById(id)
        entity?.let { PhotoMetadata.fromEntity(it, metadataEncryption) }
    }

    /**
     * Gets a photo by ID without decrypting (for performance when sensitive data not needed)
     * @param id Photo ID
     * @return PhotoMetadata with only basic fields, or null if not found
     */
    suspend fun getBasicPhotoById(id: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        val entity = photoDao.getPhotoById(id)
        entity?.let { PhotoMetadata.fromEntityBasic(it) }
    }

    /**
     * Gets all photos in a category with decrypted metadata
     * Note: This may be slower for large collections due to decryption overhead
     * @param categoryId Category ID
     * @return Flow of PhotoMetadata list with decrypted sensitive fields
     */
    fun getSecurePhotosByCategory(categoryId: Long): Flow<List<PhotoMetadata>> {
        return photoDao.getPhotosByCategory(categoryId).map { entities ->
            entities.map { entity ->
                PhotoMetadata.fromEntity(entity, metadataEncryption)
            }
        }
    }

    /**
     * Gets all photos in a category without decrypting (for gallery display)
     * @param categoryId Category ID
     * @return Flow of PhotoMetadata list with only basic fields
     */
    fun getBasicPhotosByCategory(categoryId: Long): Flow<List<PhotoMetadata>> {
        return photoDao.getPhotosByCategory(categoryId).map { entities ->
            entities.map { entity ->
                PhotoMetadata.fromEntityBasic(entity)
            }
        }
    }

    /**
     * Gets all photos with decrypted metadata
     * Note: This may be slower for large collections due to decryption overhead
     * @return Flow of PhotoMetadata list with decrypted sensitive fields
     */
    fun getAllSecurePhotos(): Flow<List<PhotoMetadata>> {
        return photoDao.getAllPhotos().map { entities ->
            entities.map { entity ->
                PhotoMetadata.fromEntity(entity, metadataEncryption)
            }
        }
    }

    /**
     * Gets all photos without decrypting (for gallery display)
     * @return Flow of PhotoMetadata list with only basic fields
     */
    fun getAllBasicPhotos(): Flow<List<PhotoMetadata>> {
        return photoDao.getAllPhotos().map { entities ->
            entities.map { entity ->
                PhotoMetadata.fromEntityBasic(entity)
            }
        }
    }

    /**
     * Deletes a photo by ID
     * @param id Photo ID
     */
    suspend fun deletePhoto(id: String) = withContext(Dispatchers.IO) {
        photoDao.deletePhotoById(id)
    }

    /**
     * Deletes all photos in a category
     * @param categoryId Category ID
     */
    suspend fun deletePhotosByCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        photoDao.deletePhotosByCategory(categoryId)
    }

    /**
     * Adds or updates a child's name for a photo
     * @param photoId Photo ID
     * @param childName Child's name (will be encrypted)
     */
    suspend fun updateChildName(photoId: String, childName: String?) = withContext(Dispatchers.IO) {
        val entity = photoDao.getPhotoById(photoId) ?: return@withContext
        val metadata = PhotoMetadata.fromEntity(entity, metadataEncryption)
        val updatedMetadata = metadata.copy(childName = childName)
        updateSecurePhoto(updatedMetadata)
    }

    /**
     * Adds or updates notes for a photo
     * @param photoId Photo ID
     * @param notes Notes (will be encrypted)
     */
    suspend fun updateNotes(photoId: String, notes: String?) = withContext(Dispatchers.IO) {
        val entity = photoDao.getPhotoById(photoId) ?: return@withContext
        val metadata = PhotoMetadata.fromEntity(entity, metadataEncryption)
        val updatedMetadata = metadata.copy(notes = notes)
        updateSecurePhoto(updatedMetadata)
    }

    /**
     * Adds or updates tags for a photo
     * @param photoId Photo ID
     * @param tags List of tags (will be encrypted)
     */
    suspend fun updateTags(photoId: String, tags: List<String>) = withContext(Dispatchers.IO) {
        val entity = photoDao.getPhotoById(photoId) ?: return@withContext
        val metadata = PhotoMetadata.fromEntity(entity, metadataEncryption)
        val updatedMetadata = metadata.copy(tags = tags)
        updateSecurePhoto(updatedMetadata)
    }

    /**
     * Updates favorite status (unencrypted field)
     * @param photoId Photo ID
     * @param isFavorite Favorite status
     */
    suspend fun updateFavoriteStatus(photoId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        photoDao.updateFavoriteStatus(photoId, isFavorite)
    }

    /**
     * Gets photos that have encrypted child data
     * @return Flow of PhotoMetadata list for photos with child information
     */
    fun getPhotosWithChildData(): Flow<List<PhotoMetadata>> {
        return photoDao.getPhotosWithEncryptedData().map { entities ->
            entities.map { entity ->
                PhotoMetadata.fromEntity(entity, metadataEncryption)
            }
        }
    }

    /**
     * Searches photos by child name (requires decryption)
     * Note: This is relatively expensive as it requires decrypting all photos
     * @param childName Name to search for
     * @return List of matching PhotoMetadata
     */
    suspend fun searchByChildName(childName: String): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val allPhotos = photoDao.getAllPhotosSnapshot()
        val searchTerm = childName.lowercase()

        allPhotos.mapNotNull { entity ->
            try {
                val metadata = PhotoMetadata.fromEntity(entity, metadataEncryption)
                if (metadata.childName?.lowercase()?.contains(searchTerm) == true) {
                    metadata
                } else null
            } catch (e: Exception) {
                null // Skip photos that can't be decrypted
            }
        }
    }

    /**
     * Validates that the encryption system is working properly
     * @return true if encryption validation passes
     */
    suspend fun validateEncryption(): Boolean = withContext(Dispatchers.IO) {
        metadataEncryption.validateEncryption()
    }

    /**
     * Gets count of photos with encrypted metadata
     * @return Number of photos that have sensitive data
     */
    suspend fun getSecurePhotoCount(): Int = withContext(Dispatchers.IO) {
        photoDao.getPhotosWithEncryptedDataCount()
    }
}