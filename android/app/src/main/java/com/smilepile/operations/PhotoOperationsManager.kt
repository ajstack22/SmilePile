package com.smilepile.operations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoOperationsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository
) {

    /**
     * Deletes a photo from both internal storage and database
     * Only works with photos in internal storage (all imported photos)
     */
    suspend fun deletePhoto(photo: Photo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Only delete file if it's not from assets and is in internal storage
                if (!photo.isFromAssets) {
                    val file = File(photo.path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (!deleted) {
                            Log.w("PhotoOps", "Failed to delete photo file: ${photo.path}")
                            // Continue with database removal even if file deletion fails
                        }
                    }
                }

                // Remove from database
                photoRepository.deletePhoto(photo)
                true
            } catch (e: Exception) {
                Log.e("PhotoOps", "Error deleting photo: ${photo.path}", e)
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
                if (deleteSinglePhoto(photo)) {
                    successCount++
                } else {
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

    private suspend fun deleteSinglePhoto(photo: Photo): Boolean {
        return try {
            deletePhotoFile(photo)
            photoRepository.deletePhoto(photo)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deletePhotoFile(photo: Photo) {
        if (!photo.isFromAssets) {
            val file = File(photo.path)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Log.w("PhotoOps", "Failed to delete photo file: ${photo.path}")
                }
            }
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
     * Removes a photo from the app library only (NOT from device storage)
     * This is the safe removal method that preserves photos on the device
     */
    suspend fun removeFromLibrary(photo: Photo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("PhotoOperations", "Removing photo from library only: ${photo.id}")
                photoRepository.removeFromLibrary(photo)
                true
            } catch (e: Exception) {
                android.util.Log.e("PhotoOperations", "Failed to remove photo from library: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Removes multiple photos from the app library only (NOT from device storage)
     * This is the safe batch removal method that preserves photos on the device
     */
    suspend fun removeFromLibrary(photos: List<Photo>): BatchOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failureCount = 0
            val failedPhotos = mutableListOf<Photo>()

            android.util.Log.d("PhotoOperations", "Removing ${photos.size} photos from library only")

            photos.forEach { photo ->
                try {
                    photoRepository.removeFromLibrary(photo)
                    successCount++
                } catch (e: Exception) {
                    android.util.Log.e("PhotoOperations", "Failed to remove photo ${photo.id} from library: ${e.message}", e)
                    failureCount++
                    failedPhotos.add(photo)
                }
            }

            android.util.Log.d("PhotoOperations", "Batch library removal complete: $successCount success, $failureCount failed")

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