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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Stable
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
import com.smilepile.ui.viewmodels.AppModeUiState
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import com.smilepile.ui.components.gallery.CategoryFilterComponentKidsMode
import com.smilepile.ui.toast.CategoryToastUI
import kotlinx.coroutines.delay
import java.io.File
import kotlinx.coroutines.launch
import dagger.hilt.android.EntryPointAccessors
import com.smilepile.di.BiometricManagerEntryPoint
import android.os.SystemClock
import androidx.compose.runtime.saveable.rememberSaveable

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
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    // Get BiometricManager from Hilt
    val biometricManager = remember {
        val appContext = context.applicationContext as android.app.Application
        EntryPointAccessors.fromApplication(
            appContext,
            BiometricManagerEntryPoint::class.java
        ).biometricManager()
    }

    val galleryState by galleryViewModel.uiState.collectAsState()
    val categories by galleryViewModel.categories.collectAsState()
    val selectedCategoryId by galleryViewModel.selectedCategoryId.collectAsState()
    val allPhotos by galleryViewModel.allPhotos.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()

    var zoomedPhoto by remember { mutableStateOf<Photo?>(null) }
    var maintainZoom by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var lastLockClickTime by rememberSaveable { mutableStateOf(0L) }
    val lockDebounceMs = 500L

    // Initialize state and side effects
    KidsModeEffects(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        zoomedPhoto = zoomedPhoto,
        modeViewModel = modeViewModel,
        galleryViewModel = galleryViewModel,
        modeState = modeState,
        onNavigateToParentalLock = onNavigateToParentalLock
    )

    // Handle authentication when mode toggle is requested with protection
    LaunchedEffect(modeState.requiresPinAuth) {
        if (modeState.requiresPinAuth) {
            // Check if biometric is enabled and available
            if (biometricManager.shouldOfferBiometricFirst() && activity != null) {
                // Try biometric first
                coroutineScope.launch {
                    when (biometricManager.authenticateWithBiometrics(
                        activity = activity,
                        title = "Exit Kids Mode",
                        subtitle = "Use your fingerprint or face to return to Parent Mode",
                        description = "Biometric authentication protects parental settings"
                    )) {
                        com.smilepile.security.BiometricResult.SUCCESS -> {
                            modeViewModel.forceParentMode()
                        }
                        com.smilepile.security.BiometricResult.USER_CANCELED -> {
                            showPinDialog = true
                        }
                        else -> {
                            showPinDialog = true
                        }
                    }
                }
            } else {
                // No biometric available - show PIN dialog
                showPinDialog = true
            }
        }
    }

    // Handle back button
    BackHandler {
        if (zoomedPhoto == null) {
            modeViewModel.requestModeToggle()
        }
    }

    // Filter photos by selected category
    val displayedPhotos = remember(allPhotos, selectedCategoryId) {
        filterPhotosByCategory(allPhotos, selectedCategoryId)
    }

    // When category changes while zoomed, update to first photo
    LaunchedEffect(selectedCategoryId, displayedPhotos) {
        if (maintainZoom && displayedPhotos.isNotEmpty()) {
            zoomedPhoto = displayedPhotos.first()
            maintainZoom = false
        }
    }

    val listState = rememberLazyListState()

    // Scroll to top when photos change
    LaunchedEffect(displayedPhotos) {
        if (displayedPhotos.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Handle horizontal swipe gestures for category navigation with debouncing
    val categorySwipeHandler = rememberCategorySwipeHandler(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = galleryViewModel::selectCategory
    )

    // Main layout with category filters at top and photo grid below
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(categories, selectedCategoryId) {
                detectHorizontalDragGestures(
                    onDragEnd = { categorySwipeHandler.handleDragEnd() },
                    onHorizontalDrag = { _, dragAmount ->
                        categorySwipeHandler.horizontalDragOffset += dragAmount
                    }
                )
            }
    ) {
        if (categories.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Category filter chips (scrollable, stops before close button)
                    CategoryFilterComponentKidsMode(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { categoryId ->
                            galleryViewModel.selectCategory(categoryId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 56.dp) // Make room for close button
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                val currentTime = SystemClock.elapsedRealtime()
                                if (currentTime - lastLockClickTime >= lockDebounceMs) {
                                    lastLockClickTime = currentTime
                                    modeViewModel.requestModeToggle()
                                }
                            },
                        color = Color(0xFFE53935),
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Exit Kids Mode",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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

    // Show PIN verification dialog
    if (showPinDialog) {
        com.smilepile.ui.components.settings.PinVerificationDialog(
            onDismiss = {
                showPinDialog = false
                modeViewModel.cancelPinAuth()
            },
            onVerify = { pin ->
                modeViewModel.validatePinAndToggle(pin)
            },
            onSuccess = {
                showPinDialog = false
            }
        )
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
                .data(File(photo.path))
                .memoryCacheKey("${photo.path}_${photo.fileSize}")
                .diskCacheKey("${photo.path}_${photo.fileSize}")
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

    // Current category index
    val currentCategoryIndex = remember(currentCategoryId, categories) {
        categories.indexOfFirst { it.id == currentCategoryId }.coerceAtLeast(0)
    }

    // Horizontal pager state for category navigation
    val categoryPagerState = rememberPagerState(
        initialPage = currentCategoryIndex,
        pageCount = { categories.size }
    )

    // Track current category from pager and update parent
    LaunchedEffect(categoryPagerState.currentPage) {
        val newCategoryId = categories[categoryPagerState.currentPage].id
        if (newCategoryId != currentCategoryId) {
            onCategoryChange(newCategoryId)
        }
    }

    // Animate the appearance
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "zoom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal pager for categories
        HorizontalPager(
            state = categoryPagerState,
            modifier = Modifier.fillMaxSize()
        ) { categoryPage ->
            val category = categories[categoryPage]
            val categoryPhotos = allPhotos // This will be updated when category changes

            // Vertical pager for photos within the category
            if (categoryPhotos.isNotEmpty()) {
                val photoPagerState = rememberPagerState(
                    initialPage = if (categoryPage == currentCategoryIndex) initialPhotoIndex else 0,
                    pageCount = { categoryPhotos.size }
                )

                VerticalPager(
                    state = photoPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = animationProgress
                        }
                ) { photoPage ->
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
                                .data(File(categoryPhotos[photoPage].path))
                                .memoryCacheKey("${categoryPhotos[photoPage].path}_${categoryPhotos[photoPage].fileSize}")
                                .diskCacheKey("${categoryPhotos[photoPage].path}_${categoryPhotos[photoPage].fileSize}")
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
            } else {
                // Empty state for this category
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
                    Text(
                        text = "No photos in ${category.displayName}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// MARK: - Helper Functions

@Composable
private fun KidsModeEffects(
    categories: List<Category>,
    selectedCategoryId: Long?,
    zoomedPhoto: Photo?,
    modeViewModel: AppModeViewModel,
    galleryViewModel: PhotoGalleryViewModel,
    modeState: AppModeUiState,
    onNavigateToParentalLock: () -> Unit
) {
    LaunchedEffect(categories, selectedCategoryId) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            galleryViewModel.selectCategory(categories.first().id)
        }
    }

    LaunchedEffect(zoomedPhoto) {
        modeViewModel.setKidsFullscreen(zoomedPhoto != null)
    }

    LaunchedEffect(categories) {
        if (selectedCategoryId != null && categories.isNotEmpty()) {
            val stillExists = categories.any { it.id == selectedCategoryId }
            if (!stillExists) {
                galleryViewModel.selectCategory(categories.first().id)
            }
        }
    }

    LaunchedEffect(modeState.requiresPinAuth) {
        if (modeState.requiresPinAuth) {
            modeViewModel.cancelPinAuth()
            onNavigateToParentalLock()
        }
    }
}

private fun filterPhotosByCategory(
    allPhotos: List<Photo>,
    selectedCategoryId: Long?
): List<Photo> {
    return if (selectedCategoryId == null || allPhotos.isEmpty()) {
        allPhotos
    } else {
        allPhotos.filter { it.categoryId == selectedCategoryId }
    }
}

@Stable
private class CategorySwipeHandler(
    private val categories: List<Category>,
    private val selectedCategoryId: Long?,
    private val onCategorySelected: (Long) -> Unit
) {
    private val swipeThreshold = 100f
    private val swipeDebounceMs = 300L

    var horizontalDragOffset by mutableStateOf(0f)
    var lastSwipeTime by mutableStateOf(0L)

    fun handleDragEnd() {
        val currentTime = System.currentTimeMillis()

        if (isDebounced(currentTime)) {
            resetDrag()
            return
        }

        val categoryIds = categories.map { it.id }
        val currentCategoryIndex = categoryIds.indexOf(selectedCategoryId).takeIf { it >= 0 } ?: 0

        println("SmilePile Debug: Swipe detected. Current index: $currentCategoryIndex, CategoryIds: $categoryIds, Selected: $selectedCategoryId")

        when {
            isSwipeLeft() && categoryIds.isNotEmpty() -> {
                handleSwipeLeft(currentCategoryIndex, categoryIds, currentTime)
            }
            isSwipeRight() && categoryIds.isNotEmpty() -> {
                handleSwipeRight(currentCategoryIndex, categoryIds, currentTime)
            }
        }

        resetDrag()
    }

    private fun isDebounced(currentTime: Long): Boolean {
        return currentTime - lastSwipeTime < swipeDebounceMs
    }

    private fun isSwipeLeft(): Boolean {
        return horizontalDragOffset < -swipeThreshold
    }

    private fun isSwipeRight(): Boolean {
        return horizontalDragOffset > swipeThreshold
    }

    private fun handleSwipeLeft(currentIndex: Int, categoryIds: List<Long>, currentTime: Long) {
        val nextIndex = (currentIndex + 1) % categoryIds.size
        val nextCategoryId = categoryIds[nextIndex]
        println("SmilePile Debug: Swipe LEFT - Moving from index $currentIndex to $nextIndex (category $nextCategoryId)")
        onCategorySelected(nextCategoryId)
        lastSwipeTime = currentTime
    }

    private fun handleSwipeRight(currentIndex: Int, categoryIds: List<Long>, currentTime: Long) {
        val prevIndex = if (currentIndex == 0) {
            categoryIds.size - 1
        } else {
            currentIndex - 1
        }
        val prevCategoryId = categoryIds[prevIndex]
        println("SmilePile Debug: Swipe RIGHT - Moving from index $currentIndex to $prevIndex (category $prevCategoryId)")
        onCategorySelected(prevCategoryId)
        lastSwipeTime = currentTime
    }

    private fun resetDrag() {
        horizontalDragOffset = 0f
    }
}

@Composable
private fun rememberCategorySwipeHandler(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit
): CategorySwipeHandler {
    return remember(categories, selectedCategoryId) {
        CategorySwipeHandler(categories, selectedCategoryId, onCategorySelected)
    }
}
