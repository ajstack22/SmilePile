package com.smilepile.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages photo storage operations for the SmilePile app.
 * Handles copying imported photos to app's private storage,
 * generating thumbnails, and managing storage directories.
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StorageManager"
        private const val PHOTOS_DIR = "photos"
        private const val THUMBNAILS_DIR = "thumbnails"
        private const val THUMBNAIL_SIZE = 300 // px
        private const val THUMBNAIL_QUALITY = 85 // JPEG quality
        private const val MAX_PHOTO_SIZE = 2048 // px for width/height
        private const val PHOTO_QUALITY = 90 // JPEG quality
    }

    // Directory for storing original photos
    private val photosDir: File by lazy {
        File(context.filesDir, PHOTOS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    // Directory for storing thumbnails
    private val thumbnailsDir: File by lazy {
        File(context.filesDir, THUMBNAILS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Import a photo from any source (gallery, camera, etc.) to app's private internal storage
     * All photos are stored in internal storage only for security and privacy
     * @param sourceUri The URI of the source photo
     * @return StorageResult with photo path, thumbnail path, etc. or null if failed
     */
    suspend fun importPhoto(sourceUri: Uri): StorageResult? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val uniqueId = UUID.randomUUID().toString().take(8)
            val fileName = "IMG_${timestamp}_${uniqueId}.jpg"

            val photoFile = File(photosDir, fileName)
            val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")

            // Copy and resize the original photo
            val photoSuccess = copyAndResizePhoto(sourceUri, photoFile)
            if (!photoSuccess) {
                Log.e(TAG, "Failed to copy photo from $sourceUri")
                return@withContext null
            }

            // Generate thumbnail
            val thumbnailSuccess = generateThumbnail(photoFile, thumbnailFile)
            if (!thumbnailSuccess) {
                Log.w(TAG, "Failed to generate thumbnail for $fileName")
                // Don't fail the import if thumbnail generation fails
            }

            Log.d(TAG, "Successfully imported photo: $fileName")
            StorageResult(
                photoPath = photoFile.absolutePath,
                thumbnailPath = if (thumbnailSuccess) thumbnailFile.absolutePath else null,
                fileName = fileName,
                fileSize = photoFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing photo from $sourceUri", e)
            null
        }
    }

    /**
     * Save a bitmap directly to internal storage
     * @param bitmap The bitmap to save
     * @param filename The filename to save as
     * @return The saved file or null if failed
     */
    suspend fun savePhotoToInternalStorage(bitmap: Bitmap, filename: String): File? = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(photosDir, filename)

            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
            }

            Log.d(TAG, "Successfully saved bitmap to: $filename")
            photoFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: $filename", e)
            null
        }
    }

    /**
     * Import multiple photos in batch
     * @param sourceUris List of photo URIs to import
     * @return List of successful import results
     */
    suspend fun importPhotos(sourceUris: List<Uri>): List<StorageResult> = withContext(Dispatchers.IO) {
        sourceUris.mapNotNull { uri ->
            try {
                importPhoto(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import photo from $uri", e)
                null
            }
        }
    }

    /**
     * Delete a photo and its thumbnail
     * @param photoPath Path to the photo file
     * @return true if successfully deleted
     */
    suspend fun deletePhoto(photoPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(photoPath)
            val fileName = photoFile.name
            val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")

            var success = true

            if (photoFile.exists()) {
                success = photoFile.delete()
                if (!success) {
                    Log.e(TAG, "Failed to delete photo file: $photoPath")
                }
            }

            if (thumbnailFile.exists()) {
                val thumbnailDeleted = thumbnailFile.delete()
                if (!thumbnailDeleted) {
                    Log.w(TAG, "Failed to delete thumbnail file: ${thumbnailFile.absolutePath}")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo: $photoPath", e)
            false
        }
    }

    /**
     * Get the thumbnail path for a given photo path
     */
    fun getThumbnailPath(photoPath: String): String? {
        val photoFile = File(photoPath)
        val fileName = photoFile.name
        val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")
        return if (thumbnailFile.exists()) thumbnailFile.absolutePath else null
    }

    /**
     * Calculate total storage usage of photos and thumbnails
     * @return Storage usage in bytes
     */
    suspend fun calculateStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        try {
            val photoSize = photosDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }

            val thumbnailSize = thumbnailsDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }

            val photoCount = photosDir.listFiles()?.size ?: 0

            StorageUsage(
                totalBytes = photoSize + thumbnailSize,
                photoBytes = photoSize,
                thumbnailBytes = thumbnailSize,
                photoCount = photoCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage usage", e)
            StorageUsage()
        }
    }

    /**
     * Clean up orphaned thumbnails (thumbnails without corresponding photos)
     */
    suspend fun cleanupOrphanedThumbnails(): Int = withContext(Dispatchers.IO) {
        try {
            val photoFiles = photosDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
            val thumbnailFiles = thumbnailsDir.listFiles() ?: emptyArray()

            var deletedCount = 0
            thumbnailFiles.forEach { thumbnailFile ->
                val thumbnailName = thumbnailFile.name
                if (thumbnailName.startsWith("thumb_")) {
                    val originalName = thumbnailName.removePrefix("thumb_")
                    if (originalName !in photoFiles) {
                        if (thumbnailFile.delete()) {
                            deletedCount++
                            Log.d(TAG, "Deleted orphaned thumbnail: $thumbnailName")
                        }
                    }
                }
            }

            Log.d(TAG, "Cleaned up $deletedCount orphaned thumbnails")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up orphaned thumbnails", e)
            0
        }
    }

    /**
     * Get available free space in internal storage
     */
    fun getAvailableSpace(): Long {
        return context.filesDir.freeSpace
    }

    /**
     * Check if there's enough space for a photo import
     * @param estimatedSize Estimated size in bytes (use 10MB as default)
     */
    fun hasEnoughSpace(estimatedSize: Long = 10 * 1024 * 1024): Boolean {
        return getAvailableSpace() > estimatedSize
    }

    /**
     * Get all photos stored in internal storage
     * @return List of photo file paths in internal storage
     */
    suspend fun getAllInternalPhotos(): List<String> = withContext(Dispatchers.IO) {
        try {
            photosDir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
                ?.map { it.absolutePath }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting internal photos", e)
            emptyList()
        }
    }

    /**
     * Copy a photo file to internal storage (used for migration)
     * This method handles copying existing photos from any location to internal storage
     * @param sourceFile The source file to copy
     * @return StorageResult or null if failed
     */
    suspend fun copyPhotoToInternalStorage(sourceFile: java.io.File): StorageResult? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file does not exist: ${sourceFile.absolutePath}")
                return@withContext null
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val uniqueId = UUID.randomUUID().toString().take(8)
            val fileName = "IMG_${timestamp}_${uniqueId}.jpg"

            val photoFile = File(photosDir, fileName)
            val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")

            // Copy and resize the photo
            val photoSuccess = copyAndResizePhotoFromFile(sourceFile, photoFile)
            if (!photoSuccess) {
                Log.e(TAG, "Failed to copy photo from ${sourceFile.absolutePath}")
                return@withContext null
            }

            // Generate thumbnail
            val thumbnailSuccess = generateThumbnail(photoFile, thumbnailFile)
            if (!thumbnailSuccess) {
                Log.w(TAG, "Failed to generate thumbnail for $fileName")
            }

            Log.d(TAG, "Successfully copied photo to internal storage: $fileName")
            StorageResult(
                photoPath = photoFile.absolutePath,
                thumbnailPath = if (thumbnailSuccess) thumbnailFile.absolutePath else null,
                fileName = fileName,
                fileSize = photoFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error copying photo to internal storage from ${sourceFile.absolutePath}", e)
            null
        }
    }

    /**
     * Migrate external photos to internal storage
     * This method helps consolidate storage to internal-only
     * @param externalPhotoPaths List of external photo paths to migrate
     * @return List of successful migration results
     */
    suspend fun migrateExternalPhotosToInternal(externalPhotoPaths: List<String>): List<StorageResult> = withContext(Dispatchers.IO) {
        externalPhotoPaths.mapNotNull { externalPath ->
            try {
                val sourceFile = File(externalPath)
                if (sourceFile.exists()) {
                    copyPhotoToInternalStorage(sourceFile)
                } else {
                    Log.w(TAG, "External photo not found for migration: $externalPath")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate photo from $externalPath", e)
                null
            }
        }
    }

    /**
     * Check if a photo path is already in internal storage
     * @param photoPath The photo path to check
     * @return true if the photo is in internal storage
     */
    fun isInternalStoragePath(photoPath: String): Boolean {
        return try {
            val file = File(photoPath)
            file.absolutePath.startsWith(photosDir.absolutePath)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy and resize photo from file (for migration purposes)
     */
    private suspend fun copyAndResizePhotoFromFile(sourceFile: File, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { inputStream ->
                // First, decode bounds to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate sample size to resize if needed
                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_PHOTO_SIZE)

                // Reset stream and decode with sample size
                FileInputStream(sourceFile).use { resetStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }

                    val bitmap = BitmapFactory.decodeStream(resetStream, null, decodeOptions)
                    bitmap?.let { bmp ->
                        FileOutputStream(targetFile).use { outputStream ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, outputStream)
                        }
                        bmp.recycle()
                        true
                    } ?: false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying and resizing photo from file", e)
            false
        }
    }

    private suspend fun copyAndResizePhoto(sourceUri: Uri, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // First, decode bounds to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate sample size to resize if needed
                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_PHOTO_SIZE)

                // Reset stream and decode with sample size
                context.contentResolver.openInputStream(sourceUri)?.use { resetStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }

                    val bitmap = BitmapFactory.decodeStream(resetStream, null, decodeOptions)
                    bitmap?.let { bmp ->
                        FileOutputStream(targetFile).use { outputStream ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, outputStream)
                        }
                        bmp.recycle()
                        true
                    } ?: false
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error copying and resizing photo", e)
            false
        }
    }

    private suspend fun generateThumbnail(sourceFile: File, thumbnailFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // Decode bounds first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            // Calculate sample size for thumbnail
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, THUMBNAIL_SIZE)

            // Decode and create thumbnail
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            bitmap?.let { bmp ->
                // Create square thumbnail
                val size = minOf(bmp.width, bmp.height)
                val x = (bmp.width - size) / 2
                val y = (bmp.height - size) / 2

                val squareBitmap = Bitmap.createBitmap(bmp, x, y, size, size)
                val thumbnail = Bitmap.createScaledBitmap(squareBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)

                FileOutputStream(thumbnailFile).use { outputStream ->
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
                }

                thumbnail.recycle()
                squareBitmap.recycle()
                bmp.recycle()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail", e)
            false
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)

        while (maxDimension / sampleSize > maxSize) {
            sampleSize *= 2
        }

        return sampleSize
    }
}

/**
 * Result of a photo import operation
 */
data class StorageResult(
    val photoPath: String,
    val thumbnailPath: String?,
    val fileName: String,
    val fileSize: Long
)

/**
 * Storage usage information
 */
data class StorageUsage(
    val totalBytes: Long = 0,
    val photoBytes: Long = 0,
    val thumbnailBytes: Long = 0,
    val photoCount: Int = 0
) {
    fun getTotalMB(): Double = totalBytes / (1024.0 * 1024.0)
    fun getPhotoMB(): Double = photoBytes / (1024.0 * 1024.0)
    fun getThumbnailMB(): Double = thumbnailBytes / (1024.0 * 1024.0)
}