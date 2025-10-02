package com.smilepile.fakes

import android.graphics.Bitmap
import android.graphics.Rect
import com.smilepile.utils.IImageProcessor
import java.io.File

/**
 * Fake implementation of IImageProcessor for testing.
 * Provides simple in-memory implementations that return predictable results.
 */
class FakeImageProcessor : IImageProcessor {

    override fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        // For testing, just return the same bitmap without actual rotation
        return bitmap
    }

    override fun cropBitmap(bitmap: Bitmap, cropRect: Rect): Bitmap {
        // For testing, just return the same bitmap without actual cropping
        return bitmap
    }

    override fun processImage(
        bitmap: Bitmap,
        rotationDegrees: Float,
        cropRect: Rect?
    ): Bitmap {
        // For testing, just return the same bitmap without processing
        return bitmap
    }

    override suspend fun getExifRotation(imagePath: String): Int {
        // For testing, always return 0 (no rotation)
        return 0
    }

    override fun createPreviewBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        // For testing, just return the same bitmap without resizing
        return bitmap
    }

    override suspend fun saveBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int
    ): Boolean {
        // For testing, always return success
        return true
    }

    override fun calculateAspectRatioCrop(
        bitmapWidth: Int,
        bitmapHeight: Int,
        aspectRatio: IImageProcessor.AspectRatio
    ): Rect {
        // For testing, return a simple full-size rectangle
        return Rect(0, 0, bitmapWidth, bitmapHeight)
    }
}