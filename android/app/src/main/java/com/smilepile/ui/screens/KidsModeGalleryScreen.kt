package com.smilepile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import com.smilepile.ui.components.gallery.CategoryFilterComponentKidsMode
import com.smilepile.ui.toast.CategoryToastUI

/**
 * Simplified gallery screen for Kids Mode
 * - No deletion capabilities
 * - No settings access
 * - Simple category navigation
 * - Mode toggle FAB (with PIN protection)
 */
@Composable
fun KidsModeGalleryScreen(
    onPhotoClick: (Photo, Int) -> Unit,
    onNavigateToParentalLock: () -> Unit = {},
    modifier: Modifier = Modifier,
    galleryViewModel: PhotoGalleryViewModel = hiltViewModel(),
    modeViewModel: AppModeViewModel = hiltViewModel(),
    toastState: com.smilepile.ui.toast.ToastState? = null
) {
    val galleryState by galleryViewModel.uiState.collectAsState()
    val categories by galleryViewModel.categories.collectAsState()
    val selectedCategoryId by galleryViewModel.selectedCategoryId.collectAsState()
    val allPhotos by galleryViewModel.allPhotos.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()

    var zoomedPhoto by remember { mutableStateOf<Photo?>(null) }
    var maintainZoom by remember { mutableStateOf(false) }

    // Handle back button in Kids Mode - request mode toggle (will handle PIN internally)
    BackHandler {
        // Don't exit on back if in fullscreen
        if (zoomedPhoto == null) {
            modeViewModel.requestModeToggle()
        }
    }

    // Initialize with first category if none selected
    LaunchedEffect(categories, selectedCategoryId) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            galleryViewModel.selectCategory(categories.first().id)
        }
    }

    // Update fullscreen state when zooming changes
    LaunchedEffect(zoomedPhoto) {
        modeViewModel.setKidsFullscreen(zoomedPhoto != null)
    }

    // Filter photos by selected category (works with all photos or category-specific photos)
    val displayedPhotos = remember(allPhotos, selectedCategoryId) {
        if (selectedCategoryId == null || allPhotos.isEmpty()) {
            allPhotos
        } else {
            allPhotos.filter { it.categoryId == selectedCategoryId }
        }
    }

    // When category changes while zoomed, update to first photo of new category
    LaunchedEffect(selectedCategoryId, displayedPhotos) {
        if (maintainZoom && displayedPhotos.isNotEmpty()) {
            zoomedPhoto = displayedPhotos.first()
            maintainZoom = false
        }
    }

    // Note: Removed photo count tracking as it was incorrectly triggering when switching categories
    // The photo count changes when filtering by category, not just when new photos are added

    // Track category index for swipe navigation (only actual categories, no 'All')
    val categoryIds = categories.map { it.id }
    val currentCategoryIndex = categoryIds.indexOf(selectedCategoryId).takeIf { it >= 0 } ?: 0

    // Ensure selected category is valid when categories change
    LaunchedEffect(categories) {
        if (selectedCategoryId != null && categories.isNotEmpty()) {
            val stillExists = categories.any { it.id == selectedCategoryId }
            if (!stillExists) {
                galleryViewModel.selectCategory(categories.first().id)
            }
        }
    }

    // Photos are filtered by selected category above
    // Debug log
    println("SmilePile Debug: Displaying ${displayedPhotos.size} photos for category: $selectedCategoryId")

    // LazyColumn state for scrolling to bottom
    val listState = rememberLazyListState()

    // Scroll to top when photos change or category switches
    LaunchedEffect(displayedPhotos) {
        if (displayedPhotos.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Handle horizontal swipe gestures for category navigation with debouncing
    val swipeThreshold = 100f
    var horizontalDragOffset by remember { mutableStateOf(0f) }
    var lastSwipeTime by remember { mutableStateOf(0L) }
    val swipeDebounceMs = 300L // Minimum time between swipes

    // Main layout with category filters at top and photo grid below
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Removed double-tap for "All Photos" - categories are mandatory
            .pointerInput(categories, selectedCategoryId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val currentTime = System.currentTimeMillis()
                        // Debounce rapid swipes
                        if (currentTime - lastSwipeTime < swipeDebounceMs) {
                            horizontalDragOffset = 0f
                            return@detectHorizontalDragGestures
                        }

                        println("SmilePile Debug: Swipe detected. Current index: $currentCategoryIndex, CategoryIds: $categoryIds, Selected: $selectedCategoryId")

                        when {
                            // Swipe left - next category (cycle through categories only)
                            horizontalDragOffset < -swipeThreshold && categoryIds.isNotEmpty() -> {
                                val nextIndex = (currentCategoryIndex + 1) % categoryIds.size
                                val nextCategoryId = categoryIds[nextIndex]
                                println("SmilePile Debug: Swipe LEFT - Moving from index $currentCategoryIndex to $nextIndex (category $nextCategoryId)")
                                galleryViewModel.selectCategory(nextCategoryId)
                                lastSwipeTime = currentTime
                                // Toast removed - only show in fullscreen mode
                            }
                            // Swipe right - previous category (cycle through categories only)
                            horizontalDragOffset > swipeThreshold && categoryIds.isNotEmpty() -> {
                                val prevIndex = if (currentCategoryIndex == 0) {
                                    categoryIds.size - 1
                                } else {
                                    currentCategoryIndex - 1
                                }
                                val prevCategoryId = categoryIds[prevIndex]
                                println("SmilePile Debug: Swipe RIGHT - Moving from index $currentCategoryIndex to $prevIndex (category $prevCategoryId)")
                                galleryViewModel.selectCategory(prevCategoryId)
                                lastSwipeTime = currentTime
                                // Toast removed - only show in fullscreen mode
                            }
                        }
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    }
                )
            }
    ) {
        // Category filter chips at top - floating bar
        if (categories.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                CategoryFilterComponentKidsMode(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { categoryId ->
                        galleryViewModel.selectCategory(categoryId)
                        // Toast removed - only show in fullscreen mode
                    }
                )
            }
        }
        // Photo grid
        if (displayedPhotos.isEmpty()) {
            EmptyKidsGallery()
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp // Reduced bottom padding since filters are now at top
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false // Keep normal layout, photos start at top
            ) {
                items(displayedPhotos) { photo ->
                    KidsPhotoStackItem(
                        photo = photo,
                        onClick = {
                            zoomedPhoto = photo
                            modeViewModel.setKidsFullscreen(true)
                        }
                    )
                }
            }
        }
    }

    // Zoomed photo overlay with swipe navigation
    zoomedPhoto?.let { photo ->
        // Find the photo index in the DISPLAYED photos (category-filtered), not all photos
        val actualPhotoIndex = displayedPhotos.indexOfFirst { it.id == photo.id }
        // If photo not in current category, use first photo instead of hiding overlay
        val safePhotoIndex = if (actualPhotoIndex >= 0) actualPhotoIndex else 0

        ZoomedPhotoOverlay(
            allPhotos = displayedPhotos, // FIXED: Pass the filtered list, not all photos
            categories = categories,
            currentCategoryId = selectedCategoryId ?: categories.firstOrNull()?.id ?: 0L,
            initialPhotoIndex = safePhotoIndex,
            onDismiss = {
                zoomedPhoto = null
                modeViewModel.setKidsFullscreen(false)
            },
            onCategoryChange = { newCategoryId ->
                // Update the selected category via ViewModel
                maintainZoom = true
                galleryViewModel.selectCategory(newCategoryId)
                val categoryName = categories.find { it.id == newCategoryId }?.displayName ?: "Category"
                toastState?.showCategory(categoryName)
                // Keep fullscreen mode active
            }
        )
    }

    // Show category toast overlay when in fullscreen
    if (zoomedPhoto != null) {
        val currentCategory = categories.find { it.id == selectedCategoryId }
        toastState?.let { toast ->
            CategoryToastUI(
                toastState = toast,
                categoryColorHex = currentCategory?.colorHex
            )
        }
    }

    // Navigate to ParentalLockScreen for biometric/PIN authentication
    LaunchedEffect(modeState.requiresPinAuth) {
        if (modeState.requiresPinAuth) {
            // Reset the requiresPinAuth state and navigate to ParentalLockScreen
            modeViewModel.cancelPinAuth()
            onNavigateToParentalLock()
        }
    }

}


@Composable
private fun KidsPhotoStackItem(
    photo: Photo,
    onClick: () -> Unit
) {
    // Calculate dynamic height based on image aspect ratio
    // Default to 4:3 aspect ratio if no specific ratio is available
    val defaultAspectRatio = 4f / 3f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(defaultAspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.path)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EmptyKidsGallery() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "No photos",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No photos yet!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ask a parent to add some photos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomedPhotoOverlay(
    allPhotos: List<Photo>,
    categories: List<Category>,
    currentCategoryId: Long,
    initialPhotoIndex: Int,
    onDismiss: () -> Unit,
    onCategoryChange: (Long) -> Unit
) {
    // Handle back button - should just dismiss fullscreen, not prompt for PIN
    BackHandler {
        onDismiss()
    }
    // Use ALL categories for consistent navigation (don't filter by photos)
    val categoryIds = remember(categories) {
        categories.map { it.id }
    }

    // Use the currentCategoryId directly instead of maintaining separate state
    val currentCategoryIndex = remember(currentCategoryId, categoryIds) {
        categoryIds.indexOf(currentCategoryId).coerceAtLeast(0)
    }

    // FIXED: Use the photos as passed - they're already filtered by the parent
    val displayedPhotos = allPhotos

    // Track if we've changed categories (to reset to first photo)
    var hasChangedCategory by remember { mutableStateOf(false) }

    // Animate the appearance
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "zoom"
    )

    // Use the passed index directly - it's already correct!
    // The parent already calculated the right index in the filtered list
    val initialFilteredIndex = initialPhotoIndex.coerceIn(0, (displayedPhotos.size - 1).coerceAtLeast(0))

    // Pager state for vertical scrolling through photos
    val pagerState = rememberPagerState(
        initialPage = initialFilteredIndex,
        pageCount = { displayedPhotos.size.coerceAtLeast(1) }
    )

    // Reset pager to first photo only when category actually changes (not on initial load)
    LaunchedEffect(currentCategoryId) {
        if (displayedPhotos.isNotEmpty() && hasChangedCategory) {
            // Only go to first photo when category changes, not on initial load
            pagerState.scrollToPage(0)
        }
        hasChangedCategory = true // Mark that we've loaded, so future changes will reset
    }

    // Handle horizontal swipe for categories with debouncing
    val swipeThreshold = 100f
    var horizontalDragOffset by remember { mutableStateOf(0f) }
    var lastSwipeTime by remember { mutableStateOf(0L) }
    val swipeDebounceMs = 300L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(categories, currentCategoryId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val currentTime = System.currentTimeMillis()
                        // Debounce rapid swipes
                        if (currentTime - lastSwipeTime < swipeDebounceMs) {
                            horizontalDragOffset = 0f
                            return@detectHorizontalDragGestures
                        }

                        when {
                            // Swipe left - next category (cycle through all)
                            horizontalDragOffset < -swipeThreshold && categoryIds.isNotEmpty() -> {
                                val nextIndex = (currentCategoryIndex + 1) % categoryIds.size
                                onCategoryChange(categoryIds[nextIndex])
                                lastSwipeTime = currentTime
                            }
                            // Swipe right - previous category (cycle through all)
                            horizontalDragOffset > swipeThreshold && categoryIds.isNotEmpty() -> {
                                val prevIndex = if (currentCategoryIndex == 0) {
                                    categoryIds.size - 1
                                } else {
                                    currentCategoryIndex - 1
                                }
                                onCategoryChange(categoryIds[prevIndex])
                                lastSwipeTime = currentTime
                            }
                        }
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    }
                )
            }
    ) {
        if (displayedPhotos.isNotEmpty()) {
            // Vertical pager for scrolling through photos (up/down)
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onDismiss() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(displayedPhotos[page].path)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animationProgress
                                scaleY = animationProgress
                                alpha = animationProgress
                            }
                    )
                }
            }

            // Page indicator (vertical dots on the right)
            if (displayedPhotos.size > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(minOf(displayedPhotos.size, 10)) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        index == pagerState.currentPage -> Color.White
                                        index < 10 -> Color.White.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }

            // Removed permanent category indicator - toast provides sufficient feedback
        } else {
            // Loading or transitioning - show loading indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}