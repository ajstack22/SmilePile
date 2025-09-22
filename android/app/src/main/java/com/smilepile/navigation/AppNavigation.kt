package com.smilepile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

/**
 * Navigation routes for the SmilePile app
 */
object NavigationRoutes {
    const val GALLERY = "gallery"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val PHOTO_VIEWER = "photo_viewer"
    const val CAMERA = "camera"
    const val PARENTAL_LOCK = "parental_lock"
    const val PARENTAL_SETTINGS = "parental_settings"

    // Route with arguments
    const val PHOTO_VIEWER_WITH_ARGS = "photo_viewer/{photoId}/{photoIndex}"
    const val CAMERA_WITH_CATEGORY = "camera/{categoryId}"

    fun photoViewerRoute(photoId: Long, photoIndex: Int): String {
        return "photo_viewer/$photoId/$photoIndex"
    }

    fun cameraRoute(categoryId: Long = 1L): String {
        return "camera/$categoryId"
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
    object Camera : NavigationDestination(NavigationRoutes.CAMERA, "Camera")
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
    modifier: Modifier = Modifier
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
                    onPhotoClick = { photo ->
                        // Kids mode - simple photo viewing
                        navController.navigate(
                            NavigationRoutes.photoViewerRoute(photo.id, 0)
                        )
                    }
                )
            } else {
                PhotoGalleryScreen(
                    onPhotoClick = { photo, photos ->
                    // Find the index of the clicked photo in the list
                    val photoIndex = photos.indexOfFirst { it.id == photo.id }
                    if (photoIndex != -1) {
                        navController.navigate(
                            NavigationRoutes.photoViewerRoute(photo.id, photoIndex)
                        )
                    }
                }
                )
            }
        }

        // Categories Screen - Category management interface
        composable(NavigationRoutes.CATEGORIES) {
            CategoryManagementScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Settings Screen - App configuration and preferences (Parent Mode only)
        composable(NavigationRoutes.SETTINGS) {
            if (currentMode == AppMode.PARENT) {
                SettingsScreen(
                    onNavigateUp = {
                        navController.navigateUp()
                    }
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

            // Get the PhotoGalleryViewModel to access photo data
            val photoGalleryViewModel: com.smilepile.ui.viewmodels.PhotoGalleryViewModel = hiltViewModel()
            val galleryUiState by photoGalleryViewModel.uiState.collectAsState()
            val photos = galleryUiState.photos

            // Find the current photo by ID
            val currentPhoto = photos.find { it.id == photoId }

            if (currentPhoto != null) {
                PhotoViewerScreen(
                    photo = currentPhoto,
                    photos = photos,
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onSharePhoto = { photo ->
                        // TODO: Implement share functionality using PhotoOperationsManager
                    },
                    onDeletePhoto = { photo ->
                        // Use the new removeFromLibrary method for safe deletion
                        photoGalleryViewModel.removePhotoFromLibrary(photo)
                        navController.navigateUp()
                    }
                )
            } else {
                // Photo not found - navigate back
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
     * Navigate to camera
     */
    fun navigateToCamera(navController: NavHostController, categoryId: Long = 1L) {
        navController.navigate(NavigationRoutes.cameraRoute(categoryId)) {
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