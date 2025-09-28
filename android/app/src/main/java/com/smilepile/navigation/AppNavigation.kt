package com.smilepile.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.data.models.Photo
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.ui.screens.CategoryManagementScreen
import com.smilepile.ui.screens.KidsModeGalleryScreen
import com.smilepile.ui.screens.PhotoGalleryScreen
import com.smilepile.ui.screens.PhotoViewerScreen
import com.smilepile.ui.screens.SettingsScreen
import com.smilepile.ui.screens.ParentalLockScreen
import com.smilepile.ui.screens.ParentalSettingsScreen
import com.smilepile.ui.screens.PhotoEditScreen
import com.smilepile.sharing.ShareManager
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simplified photo data class for passing through navigation
 */
@Parcelize
data class PhotoData(
    val id: Long,
    val name: String,
    val path: String,
    val categoryId: Long,
    val createdAt: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val isFromAssets: Boolean
) : Parcelable {
    fun toPhoto(): Photo = Photo(
        id = id,
        name = name,
        path = path,
        categoryId = categoryId,
        createdAt = createdAt,
        width = width,
        height = height,
        fileSize = fileSize,
        isFromAssets = isFromAssets
    )
}

/**
 * Navigation routes for the SmilePile app
 */
object NavigationRoutes {
    const val GALLERY = "gallery"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val PHOTO_VIEWER = "photo_viewer"
    const val PHOTO_EDITOR = "photo_editor"
    const val PARENTAL_LOCK = "parental_lock"
    const val PARENTAL_LOCK_EXIT_KIDS = "parental_lock_exit_kids"
    const val PARENTAL_SETTINGS = "parental_settings"

    // Route with arguments
    const val PHOTO_VIEWER_WITH_ARGS = "photo_viewer/{photoId}/{photoIndex}"
    const val PHOTO_EDITOR_WITH_MODE = "photo_editor/{mode}"

    fun photoViewerRoute(photoId: Long, photoIndex: Int): String {
        return "photo_viewer/$photoId/$photoIndex"
    }

    fun photoEditorRoute(mode: String): String {
        return "photo_editor/$mode"
    }
}

/**
 * Sealed class representing navigation destinations
 */
sealed class NavigationDestination(val route: String, val title: String) {
    object Gallery : NavigationDestination(NavigationRoutes.GALLERY, "Gallery")
    object Categories : NavigationDestination(NavigationRoutes.CATEGORIES, "Categories")
    object Settings : NavigationDestination(NavigationRoutes.SETTINGS, "Settings")
    object PhotoViewer : NavigationDestination(NavigationRoutes.PHOTO_VIEWER, "Photo Viewer")
    object PhotoEditor : NavigationDestination(NavigationRoutes.PHOTO_EDITOR, "Photo Editor")
    object ParentalLock : NavigationDestination(NavigationRoutes.PARENTAL_LOCK, "Parental Lock")
    object ParentalSettings : NavigationDestination(NavigationRoutes.PARENTAL_SETTINGS, "Parental Settings")
}

/**
 * Main navigation host for the SmilePile app
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = NavigationRoutes.GALLERY,
    modifier: Modifier = Modifier,
    paddingValues: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    toastState: com.smilepile.ui.toast.ToastState? = null
) {
    // Get mode state
    val modeViewModel: AppModeViewModel = hiltViewModel()
    val modeState by modeViewModel.uiState.collectAsState()
    val currentMode = modeState.currentMode
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Gallery Screen - Shows Kids or Parent mode based on current state
        composable(NavigationRoutes.GALLERY) {
            if (currentMode == AppMode.KIDS) {
                KidsModeGalleryScreen(
                    onPhotoClick = { photo, photoIndex ->
                        // Kids mode - photo zoom is now handled internally
                        // Navigation not needed for zoom-to-fit functionality
                    },
                    onNavigateToParentalLock = {
                        navController.navigate(NavigationRoutes.PARENTAL_LOCK_EXIT_KIDS)
                    },
                    toastState = toastState
                )
            } else {
                PhotoGalleryScreen(
                    onPhotoClick = { photo, photos ->
                    // Navigate directly to photo editor when clicking a photo
                    android.util.Log.e("SmilePile", "Photo clicked: ${photo.displayName}, navigating to editor")
                    navController.currentBackStackEntry?.savedStateHandle?.set("editPaths", listOf(photo.path))
                    navController.currentBackStackEntry?.savedStateHandle?.set("categoryId", photo.categoryId)
                    navController.navigate(NavigationRoutes.photoEditorRoute("gallery"))
                },
                onNavigateToPhotoEditor = { photoPaths ->
                    // Navigate to photo editor with selected photos
                    navController.currentBackStackEntry?.savedStateHandle?.set("editPaths", photoPaths)
                    navController.navigate(NavigationRoutes.photoEditorRoute("gallery"))
                },
                onNavigateToPhotoEditorWithUris = { uris ->
                    // Navigate to photo editor with imported URIs
                    navController.currentBackStackEntry?.savedStateHandle?.set("editUris", uris)
                    navController.navigate(NavigationRoutes.photoEditorRoute("import"))
                },
                paddingValues = paddingValues
                )
            }
        }

        // Categories Screen - Category management interface
        composable(NavigationRoutes.CATEGORIES) {
            val modeViewModel: AppModeViewModel = hiltViewModel()
            CategoryManagementScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToKidsMode = {
                    modeViewModel.forceKidsMode()
                    navController.navigate(NavigationRoutes.GALLERY) {
                        popUpTo(NavigationRoutes.CATEGORIES) { inclusive = true }
                    }
                },
                paddingValues = paddingValues
            )
        }

        // Settings Screen - App configuration and preferences (Parent Mode only)
        composable(NavigationRoutes.SETTINGS) {
            if (currentMode == AppMode.PARENT) {
                SettingsScreen(
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onNavigateToKidsMode = {
                        modeViewModel.forceKidsMode()
                        navController.navigate(NavigationRoutes.GALLERY) {
                            popUpTo(NavigationRoutes.SETTINGS) { inclusive = true }
                        }
                    },
                    onNavigateToParentalControls = {
                        navController.navigate(NavigationRoutes.PARENTAL_LOCK)
                    },
                    paddingValues = paddingValues
                )
            } else {
                // Settings not accessible in Kids Mode - redirect to gallery
                LaunchedEffect(Unit) {
                    navController.navigate(NavigationRoutes.GALLERY) {
                        popUpTo(NavigationRoutes.SETTINGS) { inclusive = true }
                    }
                }
            }
        }

        // Parental Lock Screen - Authentication required for parental controls
        composable(NavigationRoutes.PARENTAL_LOCK) {
            ParentalLockScreen(
                onUnlocked = {
                    navController.navigate(NavigationRoutes.PARENTAL_SETTINGS) {
                        popUpTo(NavigationRoutes.PARENTAL_LOCK) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Parental Lock Screen for Kids Mode Exit - Authentication required to exit Kids Mode
        composable(NavigationRoutes.PARENTAL_LOCK_EXIT_KIDS) {
            val modeViewModel: AppModeViewModel = hiltViewModel()
            ParentalLockScreen(
                onUnlocked = {
                    // Switch to Parent Mode after successful authentication
                    modeViewModel.forceParentMode()
                    navController.navigate(NavigationRoutes.GALLERY) {
                        popUpTo(NavigationRoutes.PARENTAL_LOCK_EXIT_KIDS) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Parental Settings Screen - Child safety configuration
        composable(NavigationRoutes.PARENTAL_SETTINGS) {
            ParentalSettingsScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToLock = {
                    navController.navigate(NavigationRoutes.PARENTAL_LOCK) {
                        popUpTo(NavigationRoutes.PARENTAL_SETTINGS) { inclusive = true }
                    }
                }
            )
        }

        // Camera functionality has been removed from the app

        // Photo Editor Screen - Edit photos with rotate and crop
        composable(
            route = NavigationRoutes.PHOTO_EDITOR_WITH_MODE,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "import"
            val photoImportViewModel: com.smilepile.ui.viewmodels.PhotoImportViewModel = hiltViewModel()
            val photoEditViewModel: com.smilepile.ui.viewmodels.PhotoEditViewModel = hiltViewModel()

            // Get saved URIs or paths based on mode
            val savedUris = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<android.net.Uri>>("editUris")
            val savedPaths = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<String>>("editPaths")

            LaunchedEffect(Unit) {
                when (mode) {
                    "import" -> {
                        val categoryId = photoImportViewModel.getPendingCategoryId()
                        photoEditViewModel.initializeEditor(
                            photoUris = savedUris,
                            categoryId = categoryId
                        )
                    }
                    "gallery" -> {
                        val categoryId = navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<Long>("categoryId") ?: 1L
                        photoEditViewModel.initializeEditor(
                            photoPaths = savedPaths,
                            categoryId = categoryId
                        )
                    }
                }
            }

            PhotoEditScreen(
                onComplete = { results ->
                    // Navigate back to preserve gallery state
                    navController.navigateUp()
                },
                onCancel = {
                    navController.navigateUp()
                }
            )
        }

        // Photo Viewer Screen - Full-screen photo viewing with navigation
        composable(
            route = NavigationRoutes.PHOTO_VIEWER_WITH_ARGS,
            arguments = listOf(
                navArgument("photoId") {
                    type = NavType.LongType
                },
                navArgument("photoIndex") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            val photoIndex = backStackEntry.arguments?.getInt("photoIndex") ?: 0

            android.util.Log.e("SmilePile", "==== PHOTO VIEWER ENTRY ====")
            android.util.Log.e("SmilePile", "PhotoViewer Nav - Photo ID: $photoId, Passed Index: $photoIndex")

            // NUCLEAR OPTION: Use the companion object state first
            val companionPhotos = com.smilepile.ui.orchestrators.PhotoGalleryOrchestratorState.navigationPhotoList
            val companionIndex = com.smilepile.ui.orchestrators.PhotoGalleryOrchestratorState.navigationPhotoIndex

            android.util.Log.e("SmilePile", "COMPANION INDEX: $companionIndex, COMPANION LIST SIZE: ${companionPhotos?.size ?: 0}")

            // Get the saved photo list from the previous navigation entry as backup
            val savedPhotoDataList = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<PhotoData>>("photoDataList")

            android.util.Log.e("SmilePile", "Retrieved ${savedPhotoDataList?.size ?: 0} photos from savedStateHandle")

            // Fallback to getting from ViewModel if saved list is not available
            val photoGalleryViewModel: com.smilepile.ui.viewmodels.PhotoGalleryViewModel = hiltViewModel()
            val galleryUiState by photoGalleryViewModel.uiState.collectAsState()

            // Get ShareManager from Hilt
            val shareManager: ShareManager = hiltViewModel<com.smilepile.ui.viewmodels.PhotoGalleryViewModel>().let {
                ShareManager(navController.context)
            }

            // NUCLEAR OPTION: Use companion object first, then fallbacks
            val photos = if (companionPhotos != null && companionPhotos.isNotEmpty()) {
                android.util.Log.e("SmilePile", "USING COMPANION OBJECT PHOTO LIST!")
                companionPhotos
            } else if (savedPhotoDataList != null && savedPhotoDataList.isNotEmpty()) {
                android.util.Log.e("SmilePile", "Using SAVED photo list from navigation")
                savedPhotoDataList.map { it.toPhoto() }
            } else {
                android.util.Log.e("SmilePile", "WARNING: Using VIEWMODEL fallback - this may cause index mismatch!")
                galleryUiState.photos
            }

            android.util.Log.e("SmilePile", "Photos list size: ${photos.size}")
            if (photos.isNotEmpty()) {
                android.util.Log.e("SmilePile", "First 3 photos: ${photos.take(3).map { "${it.id}:${it.displayName}" }}")
                android.util.Log.e("SmilePile", "Photo at index $photoIndex: ${photos.getOrNull(photoIndex)?.let { "${it.id}:${it.displayName}" } ?: "INDEX OUT OF BOUNDS"}")
            }

            // Find the current photo by ID
            val currentPhoto = photos.find { it.id == photoId }

            // NUCLEAR OPTION: Use companion index if available
            val actualIndex = if (companionIndex >= 0 && companionIndex < photos.size) {
                android.util.Log.e("SmilePile", "USING COMPANION INDEX: $companionIndex")
                companionIndex
            } else if (photoIndex >= 0 && photoIndex < photos.size) {
                photoIndex
            } else {
                android.util.Log.e("SmilePile", "ERROR: Index $photoIndex is out of bounds for list size ${photos.size}")
                photos.indexOfFirst { it.id == photoId }.takeIf { it != -1 } ?: 0
            }

            android.util.Log.e("SmilePile", "Current photo: ${currentPhoto?.displayName}, Using Index: $actualIndex")
            android.util.Log.e("SmilePile", "==== END PHOTO VIEWER ENTRY ====")

            if (currentPhoto != null) {
                PhotoViewerScreen(
                    photo = currentPhoto,
                    photos = photos,
                    initialIndex = actualIndex,
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onSharePhoto = { photo ->
                        // Use ShareManager to share the photo
                        shareManager.sharePhoto(navController.context, photo)
                    },
                    onDeletePhoto = { photo ->
                        // Use the new removeFromLibrary method for safe deletion
                        photoGalleryViewModel.removePhotoFromLibrary(photo)
                        navController.navigateUp()
                    },
                    onEditPhoto = { photo ->
                        // Navigate to photo editor with the current photo
                        android.util.Log.e("SmilePile", "Edit button clicked for photo: ${photo.displayName}")
                        android.util.Log.e("SmilePile", "Photo path: ${photo.path}")
                        navController.currentBackStackEntry?.savedStateHandle?.set("editPaths", listOf(photo.path))
                        val route = NavigationRoutes.photoEditorRoute("gallery")
                        android.util.Log.e("SmilePile", "Navigating to route: $route")
                        navController.navigate(route)
                    }
                )
            } else {
                // Photo not found - navigate back
                android.util.Log.e("SmilePile", "ERROR: Photo with ID $photoId not found in list!")
                LaunchedEffect(Unit) {
                    navController.navigateUp()
                }
            }
        }
    }
}

/**
 * Navigation helper functions
 */
object NavigationHelper {

    /**
     * Navigate to gallery and clear back stack
     */
    fun navigateToGallery(navController: NavHostController) {
        navController.navigate(NavigationRoutes.GALLERY) {
            popUpTo(NavigationRoutes.GALLERY) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * Navigate to categories
     */
    fun navigateToCategories(navController: NavHostController) {
        navController.navigate(NavigationRoutes.CATEGORIES) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to settings
     */
    fun navigateToSettings(navController: NavHostController) {
        navController.navigate(NavigationRoutes.SETTINGS) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to photo viewer
     */
    fun navigateToPhotoViewer(
        navController: NavHostController,
        photoId: Long,
        photoIndex: Int
    ) {
        navController.navigate(NavigationRoutes.photoViewerRoute(photoId, photoIndex))
    }

    /**
     * Handle system back button with child-safe navigation
     */
    fun handleBackNavigation(navController: NavHostController): Boolean {
        return when (navController.currentDestination?.route) {
            NavigationRoutes.GALLERY -> {
                // On gallery screen, don't allow back navigation (child safety)
                false
            }
            NavigationRoutes.CATEGORIES,
            NavigationRoutes.SETTINGS -> {
                // From main screens, go back to gallery
                navigateToGallery(navController)
                true
            }
            else -> {
                // For other screens, use default back navigation
                navController.navigateUp()
            }
        }
    }
}