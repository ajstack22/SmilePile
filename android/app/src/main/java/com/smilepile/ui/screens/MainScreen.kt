package com.smilepile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smilepile.R
import com.smilepile.mode.AppMode
import com.smilepile.navigation.AppNavHost
import com.smilepile.navigation.NavigationRoutes
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.ui.toast.ToastUI
import com.smilepile.ui.toast.rememberToastState
import com.smilepile.ui.toast.ToastManager
import javax.inject.Inject

/**
 * Data class representing a bottom navigation item
 */
data class BottomNavigationItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: Int,
    val hasNews: Boolean = false
)

/**
 * Main screen that contains the bottom navigation and hosts all main app screens
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    showKidsModeExitDialog: Boolean = false,
    onKidsModeExitDialogDismiss: () -> Unit = {},
    modeViewModel: AppModeViewModel = hiltViewModel(),
    toastManager: ToastManager? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val modeState by modeViewModel.uiState.collectAsState()
    val currentMode = modeState.currentMode

    // Toast state
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState(scope)

    // Listen for toast events from ToastManager if provided
    toastManager?.let { manager ->
        val toastEvent by manager.toastEvent.collectAsState()
        LaunchedEffect(toastEvent) {
            toastEvent?.let {
                toastState.showToast(it.data)
                manager.clearToast()
            }
        }
    }

    // Define bottom navigation items
    val bottomNavigationItems = listOf(
        BottomNavigationItem(
            route = NavigationRoutes.GALLERY,
            selectedIcon = Icons.Filled.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
            iconTextId = R.string.nav_gallery
        ),
        BottomNavigationItem(
            route = NavigationRoutes.CATEGORIES,
            selectedIcon = Icons.Filled.Layers,
            unselectedIcon = Icons.Outlined.Layers,
            iconTextId = R.string.nav_categories
        ),
        BottomNavigationItem(
            route = NavigationRoutes.SETTINGS,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            iconTextId = R.string.nav_settings
        )
    )

    // Check if current route should show bottom navigation
    val shouldShowBottomNavigation = when (currentDestination?.route) {
        NavigationRoutes.GALLERY,
        NavigationRoutes.CATEGORIES,
        NavigationRoutes.SETTINGS -> currentMode == AppMode.PARENT // Only show in Parent mode
        else -> false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
            if (shouldShowBottomNavigation) {
                SmilePileBottomNavigation(
                    items = bottomNavigationItems,
                    currentDestination = currentDestination,
                    onNavigateToDestination = { route ->
                        navigateWithSingleTop(navController, route)
                    }
                )
            }
        }
    ) { paddingValues ->
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize(),
                paddingValues = paddingValues,
                toastState = toastState
            )
        }

        // Toast UI overlay - Only show toast in Parent Mode
        // Kids Mode handles its own toast in KidsModeGalleryScreen (only in fullscreen)
        if (currentMode == AppMode.PARENT) {
            ToastUI(toastState = toastState)
        }
        // No toast for Kids Mode - handled by KidsModeGalleryScreen
    }

    // Navigate to ParentalLockScreen for Kids Mode Exit
    LaunchedEffect(showKidsModeExitDialog) {
        if (showKidsModeExitDialog) {
            // Dismiss the dialog state and navigate to ParentalLockScreen
            onKidsModeExitDialogDismiss()
            navController.navigate("parental_lock_exit_kids")
        }
    }
}

// MARK: - Helper Functions

private fun navigateWithSingleTop(navController: NavHostController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination to avoid building up large back stack
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

/**
 * Bottom navigation bar component for the SmilePile app
 */
@Composable
private fun SmilePileBottomNavigation(
    items: List<BottomNavigationItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigateToDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(86.dp), // Increased height for better aesthetics
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                label = "icon_scale"
            )

            NavigationBarItem(
                modifier = Modifier.padding(top = 6.dp), // Reduced padding with increased bar height
                icon = {
                    Icon(
                        imageVector = if (isSelected) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = stringResource(item.iconTextId),
                        modifier = Modifier
                            .size(28.dp)
                            .scale(iconScale)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.iconTextId),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    )
                },
                selected = isSelected,
                onClick = { onNavigateToDestination(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFE86082), // SmilePile pink
                    selectedTextColor = Color(0xFFE86082), // SmilePile pink
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent // No background glow
                )
            )
        }
    }
}

/**
 * Preview function for development
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    MaterialTheme {
        MainScreen()
    }
}