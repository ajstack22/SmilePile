package com.smilepile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.smilepile.data.models.Photo
import com.smilepile.ui.components.shared.PhotoGridItemSkeleton
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Performance optimization utilities for LazyVerticalGrid and photo display.
 * These utilities ensure smooth scrolling and efficient memory usage.
 */

/**
 * Optimized photo grid with performance enhancements.
 */
@Composable
fun OptimizedPhotoGrid(
    photos: List<Photo>,
    selectedPhotos: Set<Long>,
    isSelectionMode: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onFavoriteToggle: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    onLoadMore: (() -> Unit)? = null,
    isLoading: Boolean = false,
    loadingItemCount: Int = 6
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Calculate optimal column count based on screen width
    val columnCount = remember(screenWidth) {
        when {
            screenWidth < 400.dp -> 2
            screenWidth < 600.dp -> 3
            screenWidth < 900.dp -> 4
            else -> 5
        }
    }

    // Detect when user is near the end for pagination
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= photos.size - 10 && !isLoading
        }
    }

    // Trigger load more when needed
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore?.invoke()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Photo items with stable keys for performance
        items(
            items = photos,
            key = { photo -> photo.id } // Stable key for recomposition optimization
        ) { photo ->
            PhotoGridItemEntrance(
                itemIndex = photos.indexOf(photo)
            ) {
                OptimizedPhotoGridItem(
                    photo = photo,
                    isSelected = selectedPhotos.contains(photo.id),
                    isSelectionMode = isSelectionMode,
                    onPhotoClick = { onPhotoClick(photo) },
                    onPhotoLongClick = { onPhotoLongClick(photo) },
                    onFavoriteToggle = { onFavoriteToggle(photo) }
                )
            }
        }

        // Loading indicators at the bottom
        if (isLoading) {
            items(loadingItemCount) { index ->
                PhotoGridItemSkeleton()
            }
        }
    }
}

/**
 * Optimized photo grid item with proper remember keys.
 */
@Composable
private fun OptimizedPhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember the image loading state to prevent recomposition
    val imageModel = remember(photo.id, photo.path) {
        if (photo.isFromAssets) {
            "file:///android_asset/${photo.path}"
        } else {
            java.io.File(photo.path)
        }
    }

    EnhancedPhotoGridItem(
        photo = photo,
        imageModel = imageModel,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        onPhotoClick = onPhotoClick,
        onPhotoLongClick = onPhotoLongClick,
        onFavoriteToggle = onFavoriteToggle,
        modifier = modifier
    )
}

/**
 * Grid configuration utilities for different screen sizes.
 */
object GridConfig {
    @Composable
    fun getOptimalColumns(): Int {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        return when {
            screenWidth < 400.dp -> 2
            screenWidth < 600.dp -> 3
            screenWidth < 900.dp -> 4
            else -> 5
        }
    }

    @Composable
    fun getOptimalItemSpacing(): Int {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        return when {
            screenWidth < 400.dp -> 6
            screenWidth < 600.dp -> 8
            else -> 12
        }
    }

    @Composable
    fun getOptimalContentPadding(): PaddingValues {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        val padding = when {
            screenWidth < 400.dp -> 12.dp
            screenWidth < 600.dp -> 16.dp
            else -> 20.dp
        }

        return PaddingValues(padding)
    }
}

/**
 * Scroll performance utilities.
 */
object ScrollPerformance {
    /**
     * Creates a scroll state that tracks performance metrics.
     */
    @Composable
    fun rememberPerformantScrollState(): LazyGridState {
        val scrollState = rememberLazyGridState()

        // Monitor scroll performance (optional debugging)
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    // Could log performance metrics here in debug builds
                    if (com.smilepile.BuildConfig.DEBUG) {
                        println("Grid scrolling: $isScrolling")
                    }
                }
        }

        return scrollState
    }

    /**
     * Smooth scroll to top with animation.
     */
    suspend fun smoothScrollToTop(scrollState: LazyGridState) {
        scrollState.animateScrollToItem(0)
    }

    /**
     * Check if user has scrolled significantly.
     */
    @Composable
    fun shouldShowScrollToTop(scrollState: LazyGridState): Boolean {
        return remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex > 10
            }
        }.value
    }
}

/**
 * Memory optimization utilities.
 */
object MemoryOptimization {
    /**
     * Calculate optimal image size based on grid item size.
     */
    @Composable
    fun getOptimalImageSize(): Int {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val columns = GridConfig.getOptimalColumns()
        val spacing = GridConfig.getOptimalItemSpacing()
        val padding = 32 // Total horizontal padding

        val itemWidth = (screenWidth - padding - (spacing * (columns - 1))) / columns

        // Return size in pixels (approximate)
        return (itemWidth * configuration.densityDpi / 160).toInt()
    }
}

/**
 * Pagination utilities for lazy loading.
 */
object PaginationUtils {
    /**
     * Determines if more content should be loaded based on scroll position.
     */
    @Composable
    fun shouldLoadMore(
        scrollState: LazyGridState,
        itemCount: Int,
        threshold: Int = 10
    ): Boolean {
        return remember {
            derivedStateOf {
                val lastVisibleIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= itemCount - threshold
            }
        }.value
    }

    /**
     * Optimized items function for lazy grids with pagination.
     */
    fun <T> LazyGridScope.optimizedItems(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        span: ((item: T) -> GridItemSpan)? = null,
        itemContent: @Composable (item: T) -> Unit
    ) {
        items(
            count = items.size,
            key = if (key != null) { index -> key(items[index]) } else null,
            span = if (span != null) { index -> span(items[index]) } else null
        ) { index ->
            itemContent(items[index])
        }
    }
}

/**
 * Recomposition optimization utilities.
 */
object RecompositionUtils {
    /**
     * Stable wrapper for photo data to prevent unnecessary recompositions.
     */
    @Composable
    fun rememberStablePhoto(photo: Photo): Photo {
        return remember(photo.id, photo.createdAt) { photo }
    }

    /**
     * Stable wrapper for selection state.
     */
    @Composable
    fun rememberStableSelectionState(
        selectedPhotos: Set<Long>,
        photoId: Long
    ): Boolean {
        return remember(selectedPhotos.size, photoId) {
            selectedPhotos.contains(photoId)
        }
    }
}