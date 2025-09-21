package com.smilepile.performance

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import java.io.File

/**
 * Performance optimization utilities for the SmilePile app
 */
object PerformanceOptimizer {

    /**
     * Create an optimized Coil ImageLoader with proper caching
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true) // Enable smooth crossfade
            .build()
    }

    /**
     * Image loading best practices
     */
    object ImageLoadingConfig {
        const val THUMBNAIL_SIZE = 200 // Size for thumbnails in grid
        const val FULL_IMAGE_SIZE = 1920 // Max size for full screen images
        const val CACHE_DURATION_DAYS = 7 // How long to cache images
    }

    /**
     * Performance monitoring flags (only in debug)
     */
    object Monitoring {
        const val LOG_IMAGE_LOADING = false
        const val LOG_DATABASE_QUERIES = false
        const val LOG_MEMORY_USAGE = false
    }
}