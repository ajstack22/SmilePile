package com.smilepile.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central configuration for performance optimizations across the photo gallery.
 * This provides consistent settings for grid layouts, caching, and animations.
 */
object PerformanceConfig {

    /**
     * Image loading performance settings.
     */
    object ImageLoading {
        const val CROSSFADE_DURATION_MS = 300
        const val MEMORY_CACHE_PERCENTAGE = 0.25 // 25% of available memory
        const val DISK_CACHE_PERCENTAGE = 0.02 // 2% of available disk space
        const val PRELOAD_DISTANCE = 3 // Items ahead to preload

        @Composable
        fun getOptimalImageSize(): Int {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            val screenWidth = configuration.screenWidthDp.dp
            val columns = GridLayout.getColumnCount(screenWidth)
            val spacing = GridLayout.getItemSpacing(screenWidth)
            val padding = GridLayout.getContentPadding(screenWidth).calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)

            val itemWidth = (screenWidth - (padding * 2) - (spacing * (columns - 1))) / columns

            // Convert to pixels and apply density
            return with(density) { itemWidth.toPx().toInt() }
        }
    }

    /**
     * Grid layout performance settings.
     */
    object GridLayout {
        @Composable
        fun getColumnCount(screenWidth: Dp = LocalConfiguration.current.screenWidthDp.dp): Int {
            return when {
                screenWidth < 400.dp -> 2
                screenWidth < 600.dp -> 3
                screenWidth < 900.dp -> 4
                screenWidth < 1200.dp -> 5
                else -> 6
            }
        }

        @Composable
        fun getItemSpacing(screenWidth: Dp = LocalConfiguration.current.screenWidthDp.dp): Dp {
            return when {
                screenWidth < 400.dp -> 6.dp
                screenWidth < 600.dp -> 8.dp
                screenWidth < 900.dp -> 10.dp
                else -> 12.dp
            }
        }

        @Composable
        fun getContentPadding(screenWidth: Dp = LocalConfiguration.current.screenWidthDp.dp): PaddingValues {
            val padding = when {
                screenWidth < 400.dp -> 12.dp
                screenWidth < 600.dp -> 16.dp
                screenWidth < 900.dp -> 20.dp
                else -> 24.dp
            }
            return PaddingValues(padding)
        }

        @Composable
        fun rememberOptimalGridState(): LazyGridState {
            return rememberLazyGridState()
        }

        @Composable
        fun getGridCells(): GridCells {
            val columnCount = getColumnCount()
            return GridCells.Fixed(columnCount)
        }
    }

    /**
     * Animation performance settings.
     */
    object Animations {
        const val FAST_DURATION = 150
        const val MEDIUM_DURATION = 300
        const val SLOW_DURATION = 500
        const val STAGGER_DELAY = 30L

        const val SCALE_FACTOR_PRESSED = 0.95f
        const val SCALE_FACTOR_SELECTED = 1.02f

        const val SPRING_DAMPING = 0.8f
        const val SPRING_STIFFNESS = 400f

        const val CROSSFADE_DURATION = 300
        const val FADE_DURATION = 200
        const val SLIDE_DURATION = 250
    }

    /**
     * Memory management settings.
     */
    object Memory {
        const val ITEM_CACHE_SIZE = 50
        const val PRELOAD_BUFFER = 5
        const val MAX_CACHED_IMAGES = 100

        @Composable
        fun shouldUseMemoryOptimization(): Boolean {
            val configuration = LocalConfiguration.current
            // Use memory optimization on smaller screens or older devices
            return configuration.screenWidthDp < 600
        }
    }

    /**
     * Pagination settings.
     */
    object Pagination {
        const val PAGE_SIZE = 20
        const val LOAD_MORE_THRESHOLD = 5
        const val INITIAL_LOAD_SIZE = 30

        @Composable
        fun getLoadingSkeletonCount(): Int {
            val columnCount = GridLayout.getColumnCount()
            return columnCount * 2 // Two rows of skeleton items
        }
    }

    /**
     * Error handling configuration.
     */
    object ErrorHandling {
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1000L
        const val ERROR_DISPLAY_DURATION = 3000L

        enum class ErrorType {
            NETWORK,
            STORAGE,
            PERMISSION,
            GENERIC
        }
    }

    /**
     * Search performance settings.
     */
    object Search {
        const val DEBOUNCE_DELAY_MS = 300L
        const val MIN_SEARCH_LENGTH = 2
        const val MAX_HISTORY_ITEMS = 10
        const val SEARCH_TIMEOUT_MS = 5000L
    }
}

/**
 * Helper functions for applying performance configurations.
 */
@Composable
fun rememberPerformanceOptimizedGridConfig(): Any {
    val columns = PerformanceConfig.GridLayout.getGridCells()
    val contentPadding = PerformanceConfig.GridLayout.getContentPadding()
    val itemSpacing = PerformanceConfig.GridLayout.getItemSpacing()
    val state = PerformanceConfig.GridLayout.rememberOptimalGridState()

    return remember {
        object {
            val columns = columns
            val contentPadding = contentPadding
            val itemSpacing = itemSpacing
            val state = state
        }
    }
}

/**
 * Performance monitoring utilities (for debug builds).
 */
object PerformanceMonitor {
    private var scrollPerformanceEnabled = false
    private var imageLoadingMetricsEnabled = false

    fun enableScrollPerformanceMonitoring(enabled: Boolean) {
        scrollPerformanceEnabled = enabled && com.smilepile.BuildConfig.DEBUG
    }

    fun enableImageLoadingMetrics(enabled: Boolean) {
        imageLoadingMetricsEnabled = enabled && com.smilepile.BuildConfig.DEBUG
    }

    fun logScrollPerformance(
        scrollState: LazyGridState,
        visibleItemCount: Int,
        totalItemCount: Int
    ) {
        if (scrollPerformanceEnabled) {
            println("Scroll Performance - Visible: $visibleItemCount, Total: $totalItemCount, First: ${scrollState.firstVisibleItemIndex}")
        }
    }

    fun logImageLoadTime(imageId: Long, loadTimeMs: Long) {
        if (imageLoadingMetricsEnabled) {
            println("Image Load Performance - ID: $imageId, Time: ${loadTimeMs}ms")
        }
    }
}