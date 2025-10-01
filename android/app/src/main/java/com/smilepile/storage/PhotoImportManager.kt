package com.smilepile.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.smilepile.security.CircuitBreaker
import com.smilepile.security.CircuitBreakerException
import com.smilepile.security.CircuitBreakerOpenException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced photo import manager with advanced features including:
 * - Android Photo Picker integration support
 * - EXIF metadata extraction and preservation
 * - Photo optimization and compression
 * - Duplicate detection using SHA-256
 * - Circuit breaker pattern for resilient operations
 * - Batch import with progress callbacks
 */
@Singleton
class PhotoImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager
) {
    companion object {
        private const val TAG = "PhotoImportManager"

        // Import constraints
        const val MAX_BATCH_SIZE = 50
        const val MAX_PHOTO_DIMENSION = 2048
        const val JPEG_QUALITY = 90
        const val THUMBNAIL_SIZE = 300
        const val THUMBNAIL_QUALITY = 85

        // Supported formats
        val SUPPORTED_FORMATS = setOf("jpg", "jpeg", "png", "heif", "heic", "webp")

        // Circuit breaker configuration
        private const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5
        private const val CIRCUIT_BREAKER_RESET_TIMEOUT_MS = 60_000L
        private const val CIRCUIT_BREAKER_HALF_OPEN_ATTEMPTS = 2
    }

    // Circuit breaker for import operations
    private val importCircuitBreaker = CircuitBreaker(
        failureThreshold = CIRCUIT_BREAKER_FAILURE_THRESHOLD,
        resetTimeoutMs = CIRCUIT_BREAKER_RESET_TIMEOUT_MS,
        halfOpenMaxAttempts = CIRCUIT_BREAKER_HALF_OPEN_ATTEMPTS
    )

    // Metadata extractor
    private val metadataExtractor = PhotoMetadataExtractor()

    // Photo optimizer
    private val photoOptimizer = PhotoOptimizer()

    // Duplicate detector
    private val duplicateDetector = DuplicateDetector()

    // Cache for processed file hashes to avoid re-processing
    private val processedHashes = mutableSetOf<String>()

    /**
     * Import multiple photos with progress tracking
     * @param uris List of photo URIs from Android Photo Picker
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Flow of import results
     */
    fun importPhotosWithProgress(
        uris: List<Uri>,
        onProgress: (Float) -> Unit = {}
    ): Flow<ImportResult> = flow {
        if (uris.isEmpty()) {
            Log.w(TAG, "No URIs provided for import")
            return@flow
        }

        if (uris.size > MAX_BATCH_SIZE) {
            Log.w(TAG, "Batch size ${uris.size} exceeds maximum $MAX_BATCH_SIZE")
            emit(ImportResult.Error("Cannot import more than $MAX_BATCH_SIZE photos at once"))
            return@flow
        }

        val totalPhotos = uris.size
        var processedCount = 0

        Log.d(TAG, "Starting batch import of $totalPhotos photos")

        for (uri in uris) {
            try {
                // Update progress
                onProgress(processedCount.toFloat() / totalPhotos)

                // Process individual photo with circuit breaker
                val result = importCircuitBreaker.execute("importPhoto") {
                    processPhotoImport(uri)
                }

                emit(result)
                processedCount++

                // Update progress after successful import
                onProgress(processedCount.toFloat() / totalPhotos)

            } catch (e: CircuitBreakerOpenException) {
                Log.e(TAG, "Circuit breaker open: ${e.message}")
                emit(ImportResult.Error("Too many import failures. Please try again later."))
                break
            } catch (e: CircuitBreakerException) {
                Log.e(TAG, "Import failed with circuit breaker: ${e.message}", e.cause)
                emit(ImportResult.Error("Failed to import photo: ${e.cause?.message}"))
                processedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error importing photo", e)
                emit(ImportResult.Error("Unexpected error: ${e.message}"))
                processedCount++
            }
        }

        // Final progress update
        onProgress(1.0f)
        Log.d(TAG, "Batch import completed: $processedCount/$totalPhotos processed")
    }

    /**
     * Import a single photo
     * @param uri Photo URI from Android Photo Picker
     * @return Import result
     */
    suspend fun importPhoto(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            importCircuitBreaker.execute("importSinglePhoto") {
                processPhotoImport(uri)
            }
        } catch (e: CircuitBreakerOpenException) {
            Log.e(TAG, "Circuit breaker open: ${e.message}")
            ImportResult.Error("Import system temporarily unavailable")
        } catch (e: CircuitBreakerException) {
            Log.e(TAG, "Import failed: ${e.message}", e.cause)
            ImportResult.Error("Failed to import photo: ${e.cause?.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            ImportResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Process individual photo import with all features
     */
    private suspend fun processPhotoImport(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val format = validateFormat(uri) ?: return@withContext ImportResult.Error("Unsupported format")
            val photoData = readPhotoData(uri) ?: return@withContext ImportResult.Error("Cannot read photo data")
            val photoHash = duplicateDetector.calculateHash(photoData)

            if (duplicateDetector.isDuplicate(photoHash)) {
                Log.d(TAG, "Duplicate photo detected: $photoHash")
                return@withContext ImportResult.Duplicate(photoHash)
            }

            val metadata = extractMetadataFromData(photoData)
            val optimizedBitmap = optimizeBitmapFromData(photoData)
            val fileName = generateUniqueFileName()
            val (photoFile, thumbnailFile) = savePhotoAndThumbnail(optimizedBitmap, fileName)

            finalizeSaveWithMetadata(photoFile, metadata, photoHash)

            Log.d(TAG, "Successfully imported photo: $fileName")

            ImportResult.Success(
                photoPath = photoFile.absolutePath,
                thumbnailPath = thumbnailFile.absolutePath,
                fileName = fileName,
                fileSize = photoFile.length(),
                metadata = metadata,
                hash = photoHash
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing photo import", e)
            ImportResult.Error("Processing error: ${e.message}")
        }
    }

    private fun validateFormat(uri: Uri): String? {
        val format = getFileFormat(uri)
        return if (format != null && format.lowercase() in SUPPORTED_FORMATS) format else null
    }

    private suspend fun readPhotoData(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun extractMetadataFromData(photoData: ByteArray): PhotoMetadata? {
        return ByteArrayInputStream(photoData).use {
            metadataExtractor.extractMetadata(it).also { metadata ->
                Log.d(TAG, "Extracted metadata: $metadata")
            }
        }
    }

    private fun optimizeBitmapFromData(photoData: ByteArray): Bitmap {
        return ByteArrayInputStream(photoData).use {
            photoOptimizer.optimizePhoto(it, MAX_PHOTO_DIMENSION, JPEG_QUALITY)
        }
    }

    private fun generateUniqueFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueId = UUID.randomUUID().toString().take(8)
        return "IMG_${timestamp}_${uniqueId}.jpg"
    }

    private fun savePhotoAndThumbnail(bitmap: Bitmap, fileName: String): Pair<File, File> {
        val photoFile = savePhotoFile(bitmap, fileName)
        val thumbnailFile = saveThumbnailFile(bitmap, fileName)
        bitmap.recycle()
        return Pair(photoFile, thumbnailFile)
    }

    private fun savePhotoFile(bitmap: Bitmap, fileName: String): File {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val photoFile = File(photosDir, fileName)
        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return photoFile
    }

    private fun saveThumbnailFile(bitmap: Bitmap, fileName: String): File {
        val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")
        val thumbnail = photoOptimizer.generateThumbnail(bitmap, THUMBNAIL_SIZE)
        FileOutputStream(thumbnailFile).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
        }
        thumbnail.recycle()
        return thumbnailFile
    }

    private fun finalizeSaveWithMetadata(photoFile: File, metadata: PhotoMetadata?, photoHash: String) {
        metadata?.let {
            metadataExtractor.preserveMetadata(photoFile.absolutePath, it)
        }
        duplicateDetector.markAsProcessed(photoHash)
        processedHashes.add(photoHash)
    }

    /**
     * Get file format from URI
     */
    private fun getFileFormat(uri: Uri): String? {
        return context.contentResolver.getType(uri)?.substringAfter("/")
    }

    /**
     * Clear duplicate detection cache
     */
    fun clearDuplicateCache() {
        duplicateDetector.clearCache()
        processedHashes.clear()
    }

    /**
     * Get import statistics
     */
    fun getImportStatistics(): ImportStatistics {
        return ImportStatistics(
            totalImported = processedHashes.size,
            duplicatesDetected = duplicateDetector.getDuplicateCount(),
            circuitBreakerState = importCircuitBreaker.getState().name,
            failureCount = importCircuitBreaker.getFailureCount()
        )
    }
}

/**
 * Photo metadata extractor using ExifInterface
 */
class PhotoMetadataExtractor {

    fun extractMetadata(inputStream: InputStream): PhotoMetadata {
        return try {
            val exif = ExifInterface(inputStream)

            PhotoMetadata(
                dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                location = extractLocation(exif),
                cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL),
                width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
                height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0),
                focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
                aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED),
                exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            )
        } catch (e: Exception) {
            Log.e("PhotoMetadataExtractor", "Error extracting metadata", e)
            PhotoMetadata()
        }
    }

    fun preserveMetadata(photoPath: String, metadata: PhotoMetadata) {
        try {
            val exif = ExifInterface(photoPath)

            metadata.dateTaken?.let {
                exif.setAttribute(ExifInterface.TAG_DATETIME, it)
            }

            metadata.location?.let { loc ->
                exif.setLatLong(loc.latitude, loc.longitude)
            }

            metadata.cameraModel?.let {
                exif.setAttribute(ExifInterface.TAG_MODEL, it)
            }

            metadata.cameraMake?.let {
                exif.setAttribute(ExifInterface.TAG_MAKE, it)
            }

            exif.setAttribute(ExifInterface.TAG_ORIENTATION, metadata.orientation.toString())

            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e("PhotoMetadataExtractor", "Error preserving metadata", e)
        }
    }

    private fun extractLocation(exif: ExifInterface): PhotoLocation? {
        val latLong = exif.latLong ?: return null
        return PhotoLocation(latLong[0], latLong[1])
    }
}

/**
 * Photo optimizer for compression and resizing
 */
class PhotoOptimizer {

    fun optimizePhoto(inputStream: InputStream, maxDimension: Int, quality: Int): Bitmap {
        // Read all data once
        val bytes = inputStream.readBytes()

        // First decode bounds
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        // Calculate sample size
        val sampleSize = calculateSampleSize(
            options.outWidth,
            options.outHeight,
            maxDimension
        )

        // Decode with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inJustDecodeBounds = false
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalStateException("Failed to decode bitmap")
    }

    fun generateThumbnail(source: Bitmap, size: Int): Bitmap {
        val dimension = minOf(source.width, source.height)
        val x = (source.width - dimension) / 2
        val y = (source.height - dimension) / 2

        val squareBitmap = Bitmap.createBitmap(source, x, y, dimension, dimension)
        val thumbnail = Bitmap.createScaledBitmap(squareBitmap, size, size, true)

        if (squareBitmap != thumbnail) {
            squareBitmap.recycle()
        }

        return thumbnail
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
 * Duplicate detector using SHA-256 hashing
 */
class DuplicateDetector {

    private val processedHashes = mutableSetOf<String>()
    private var duplicateCount = 0

    fun calculateHash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isDuplicate(hash: String): Boolean {
        val isDupe = hash in processedHashes
        if (isDupe) duplicateCount++
        return isDupe
    }

    fun markAsProcessed(hash: String) {
        processedHashes.add(hash)
    }

    fun clearCache() {
        processedHashes.clear()
        duplicateCount = 0
    }

    fun getDuplicateCount(): Int = duplicateCount
}

/**
 * Data classes for import results
 */
sealed class ImportResult {
    data class Success(
        val photoPath: String,
        val thumbnailPath: String,
        val fileName: String,
        val fileSize: Long,
        val metadata: PhotoMetadata?,
        val hash: String
    ) : ImportResult()

    data class Duplicate(val hash: String) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

data class PhotoMetadata(
    val dateTaken: String? = null,
    val location: PhotoLocation? = null,
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val orientation: Int = 1,
    val width: Int = 0,
    val height: Int = 0,
    val focalLength: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null
)

data class PhotoLocation(
    val latitude: Double,
    val longitude: Double
)

data class ImportStatistics(
    val totalImported: Int,
    val duplicatesDetected: Int,
    val circuitBreakerState: String,
    val failureCount: Int
)