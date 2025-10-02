package com.smilepile.utils

import android.graphics.Bitmap
import android.graphics.Rect
import java.io.File

/**
 * Interface for image processing operations.
 * Handles rotation, cropping, and EXIF orientation for photos.
 * This interface allows for dependency injection and testing.
 */
interface IImageProcessor {

    /**
     * Rotate a bitmap by the specified degrees (90° increments)
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap

    /**
     * Crop a bitmap to the specified rectangle
     * Ensures minimum size constraints are met
     */
    fun cropBitmap(bitmap: Bitmap, cropRect: Rect): Bitmap

    /**
     * Apply both rotation and crop in a single operation for efficiency
     */
    fun processImage(
        bitmap: Bitmap,
        rotationDegrees: Float = 0f,
        cropRect: Rect? = null
    ): Bitmap

    /**
     * Get EXIF orientation from image file
     */
    suspend fun getExifRotation(imagePath: String): Int

    /**
     * Create a memory-efficient preview bitmap for editing UI
     */
    fun createPreviewBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap

    /**
     * Save processed bitmap to file
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int = 90
    ): Boolean

    /**
     * Calculate crop rectangle for aspect ratio presets
     */
    fun calculateAspectRatioCrop(
        bitmapWidth: Int,
        bitmapHeight: Int,
        aspectRatio: AspectRatio
    ): Rect

    /**
     * Aspect ratio presets for cropping
     */
    enum class AspectRatio {
        FREE,       // No constraints
        SQUARE,     // 1:1
        RATIO_4_3,  // 4:3
        RATIO_16_9  // 16:9
    }
}