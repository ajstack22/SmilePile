package com.smilepile.operations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoOperationsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository
) {

    /**
     * Deletes a photo from both storage and database
     */
    suspend fun deletePhoto(photo: Photo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Only delete file if it's not from assets
                if (!photo.isFromAssets) {
                    val file = File(photo.path)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                // Remove from database
                photoRepository.deletePhoto(photo)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Deletes multiple photos in batch
     */
    suspend fun deletePhotos(photos: List<Photo>): BatchOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failureCount = 0
            val failedPhotos = mutableListOf<Photo>()

            photos.forEach { photo ->
                try {
                    // Only delete file if it's not from assets
                    if (!photo.isFromAssets) {
                        val file = File(photo.path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }

                    // Remove from database
                    photoRepository.deletePhoto(photo)
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failureCount++
                    failedPhotos.add(photo)
                }
            }

            BatchOperationResult(
                successCount = successCount,
                failureCount = failureCount,
                failedItems = failedPhotos
            )
        }
    }

    /**
     * Creates a share intent for a photo
     */
    fun sharePhoto(photo: Photo): Intent? {
        return try {
            val file = File(photo.path)
            if (!file.exists() && !photo.isFromAssets) {
                return null
            }

            val uri = if (photo.isFromAssets) {
                // For asset photos, we need to copy to cache first
                copyAssetToCache(photo)
            } else {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, "Shared from SmilePile")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares multiple photos
     */
    fun sharePhotos(photos: List<Photo>): Intent? {
        return try {
            val uris = mutableListOf<Uri>()

            photos.forEach { photo ->
                val file = File(photo.path)
                if (file.exists() || photo.isFromAssets) {
                    val uri = if (photo.isFromAssets) {
                        copyAssetToCache(photo)
                    } else {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }
                    uri?.let { uris.add(it) }
                }
            }

            if (uris.isEmpty()) return null

            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, "Shared from SmilePile")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Moves a photo to a different category
     */
    suspend fun movePhotoToCategory(photo: Photo, newCategoryId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updatedPhoto = photo.copy(categoryId = newCategoryId)
                photoRepository.updatePhoto(updatedPhoto)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Moves multiple photos to a different category
     */
    suspend fun movePhotosToCategory(photos: List<Photo>, newCategoryId: Long): BatchOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failureCount = 0
            val failedPhotos = mutableListOf<Photo>()

            photos.forEach { photo ->
                try {
                    val updatedPhoto = photo.copy(categoryId = newCategoryId)
                    photoRepository.updatePhoto(updatedPhoto)
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failureCount++
                    failedPhotos.add(photo)
                }
            }

            BatchOperationResult(
                successCount = successCount,
                failureCount = failureCount,
                failedItems = failedPhotos
            )
        }
    }

    /**
     * Updates favorite status for multiple photos
     */
    suspend fun updateFavoriteStatus(photos: List<Photo>, isFavorite: Boolean): BatchOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failureCount = 0
            val failedPhotos = mutableListOf<Photo>()

            photos.forEach { photo ->
                try {
                    val updatedPhoto = photo.copy(isFavorite = isFavorite)
                    photoRepository.updatePhoto(updatedPhoto)
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failureCount++
                    failedPhotos.add(photo)
                }
            }

            BatchOperationResult(
                successCount = successCount,
                failureCount = failureCount,
                failedItems = failedPhotos
            )
        }
    }

    /**
     * Copies an asset file to cache directory for sharing
     */
    private fun copyAssetToCache(photo: Photo): Uri? {
        return try {
            val inputStream = context.assets.open(photo.path)
            val cacheFile = File(context.cacheDir, "shared_${photo.displayName}")

            inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Result of batch operations
 */
data class BatchOperationResult(
    val successCount: Int,
    val failureCount: Int,
    val failedItems: List<Photo>
) {
    val isCompleteSuccess: Boolean
        get() = failureCount == 0

    val hasFailures: Boolean
        get() = failureCount > 0

    val totalCount: Int
        get() = successCount + failureCount
}