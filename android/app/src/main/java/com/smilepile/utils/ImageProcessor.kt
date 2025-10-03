package com.smilepile.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for image processing operations.
 * Handles rotation, cropping, and EXIF orientation for photos.
 * Follows Atlas Lite principles: simple, pragmatic, under 250 lines.
 */
@Singleton
class ImageProcessor @Inject constructor() : IImageProcessor {

    companion object {
        private const val TAG = "ImageProcessor"
        private const val MIN_CROP_SIZE = 100 // Minimum 100x100px crop
        private const val JPEG_QUALITY = 90
        private const val PREVIEW_MAX_SIZE = 1024 // Preview images max 1024px
    }

    /**
     * Rotate a bitmap by the specified degrees (90° increments)
     */
    override fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        return if (degrees == 0f) {
            bitmap
        } else {
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    /**
     * Crop a bitmap to the specified rectangle
     * Ensures minimum size constraints are met
     */
    override fun cropBitmap(bitmap: Bitmap, cropRect: Rect): Bitmap {
        // Validate and adjust crop rectangle
        val validRect = validateCropRect(bitmap, cropRect)

        return Bitmap.createBitmap(
            bitmap,
            validRect.left,
            validRect.top,
            validRect.width(),
            validRect.height()
        )
    }

    /**
     * Apply both rotation and crop in a single operation for efficiency
     */
    override fun processImage(
        bitmap: Bitmap,
        rotationDegrees: Float,
        cropRect: Rect?
    ): Bitmap {
        var result = bitmap

        // Apply rotation first if needed
        if (rotationDegrees != 0f) {
            result = rotateBitmap(result, rotationDegrees)
        }

        // Apply crop if specified
        if (cropRect != null) {
            result = cropBitmap(result, cropRect)
        }

        return result
    }

    /**
     * Get EXIF orientation from image file
     */
    override suspend fun getExifRotation(imagePath: String): Int = withContext(Dispatchers.IO) {
        try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF data", e)
            0
        }
    }

    /**
     * Create a memory-efficient preview bitmap for editing UI
     */
    override fun createPreviewBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scale to fit within maxSize
        val maxDimension = max(width, height)
        if (maxDimension <= maxSize) {
            return bitmap // Already small enough
        }

        val scale = maxSize.toFloat() / maxDimension
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Save processed bitmap to file
     */
    override suspend fun saveBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            false
        }
    }

    /**
     * Calculate crop rectangle for aspect ratio presets
     */
    override fun calculateAspectRatioCrop(
        bitmapWidth: Int,
        bitmapHeight: Int,
        aspectRatio: IImageProcessor.AspectRatio
    ): Rect {
        val (targetWidth, targetHeight) = when (aspectRatio) {
            IImageProcessor.AspectRatio.FREE -> return Rect(0, 0, bitmapWidth, bitmapHeight)
            IImageProcessor.AspectRatio.SQUARE -> {
                val size = min(bitmapWidth, bitmapHeight)
                size to size
            }
            IImageProcessor.AspectRatio.RATIO_4_3 -> {
                val ratio = 4f / 3f
                if (bitmapWidth.toFloat() / bitmapHeight > ratio) {
                    val width = (bitmapHeight * ratio).toInt()
                    width to bitmapHeight
                } else {
                    val height = (bitmapWidth / ratio).toInt()
                    bitmapWidth to height
                }
            }
            IImageProcessor.AspectRatio.RATIO_16_9 -> {
                val ratio = 16f / 9f
                if (bitmapWidth.toFloat() / bitmapHeight > ratio) {
                    val width = (bitmapHeight * ratio).toInt()
                    width to bitmapHeight
                } else {
                    val height = (bitmapWidth / ratio).toInt()
                    bitmapWidth to height
                }
            }
        }

        // Center the crop
        val left = (bitmapWidth - targetWidth) / 2
        val top = (bitmapHeight - targetHeight) / 2

        return Rect(left, top, left + targetWidth, top + targetHeight)
    }

    /**
     * Validate and adjust crop rectangle to ensure it's within bounds
     */
    private fun validateCropRect(bitmap: Bitmap, cropRect: Rect): Rect {
        val validRect = Rect(cropRect)

        // Ensure minimum size
        if (validRect.width() < MIN_CROP_SIZE) {
            validRect.right = validRect.left + MIN_CROP_SIZE
        }
        if (validRect.height() < MIN_CROP_SIZE) {
            validRect.bottom = validRect.top + MIN_CROP_SIZE
        }

        // Ensure within bitmap bounds
        validRect.left = max(0, validRect.left)
        validRect.top = max(0, validRect.top)
        validRect.right = min(bitmap.width, validRect.right)
        validRect.bottom = min(bitmap.height, validRect.bottom)

        return validRect
    }
}