package com.smilepile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.Alignment
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
import com.smilepile.ui.components.CustomFloatingActionButton
import com.smilepile.ui.viewmodels.DemoModeViewModel
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
    demoModeViewModel: DemoModeViewModel = hiltViewModel(),
    toastManager: ToastManager? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val modeState by modeViewModel.uiState.collectAsState()
    val currentMode = modeState.currentMode
    val demoState by demoModeViewModel.uiState.collectAsState()
    var showExitDemoDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
        },
        contentWindowInsets = WindowInsets(0.dp) // Match nested screens
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize(),
                paddingValues = paddingValues,
                toastState = toastState,
                demoState = demoState,
                onShowExitDemoDialog = { showExitDemoDialog = true }
            )

            // Toast UI overlay - Only show toast in Parent Mode
            // Kids Mode handles its own toast in KidsModeGalleryScreen (only in fullscreen)
            if (currentMode == AppMode.PARENT) {
                ToastUI(toastState = toastState)
            }
            // No toast for Kids Mode - handled by KidsModeGalleryScreen
        }
    }

    // Exit Demo confirmation dialog
    if (showExitDemoDialog) {
        AlertDialog(
            onDismissRequest = { if (!demoState.isExiting) showExitDemoDialog = false },
            title = { Text("Exit Demo Mode?") },
            text = { Text("This will remove all demo photos and categories. You can try demo mode again later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        demoModeViewModel.exitDemoMode()
                        showExitDemoDialog = false
                    },
                    enabled = !demoState.isExiting,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Exit Demo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDemoDialog = false },
                    enabled = !demoState.isExiting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error dialog
    demoState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { demoModeViewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { demoModeViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
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
                    // Determine color based on route and selection state
                    val iconColor = if (isSelected) {
                        when (item.route) {
                            NavigationRoutes.GALLERY -> Color(0xFF2196F3) // SmilePile blue
                            NavigationRoutes.CATEGORIES -> Color(0xFFFF9800) // SmilePile orange
                            else -> Color(0xFFE86082) // SmilePile pink (Settings)
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = if (isSelected) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = stringResource(item.iconTextId),
                        tint = iconColor,
                        modifier = Modifier
                            .size(22.4.dp) // Reduced by 20% from 28.dp
                            .scale(iconScale)
                    )
                },
                label = {
                    // Determine color based on route and selection state
                    val textColor = if (isSelected) {
                        when (item.route) {
                            NavigationRoutes.GALLERY -> Color(0xFF2196F3) // SmilePile blue
                            NavigationRoutes.CATEGORIES -> Color(0xFFFF9800) // SmilePile orange
                            else -> Color(0xFFE86082) // SmilePile pink (Settings)
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Text(
                        text = stringResource(item.iconTextId),
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    )
                },
                selected = isSelected,
                onClick = { onNavigateToDestination(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Transparent, // Handled manually above
                    selectedTextColor = Color.Transparent, // Handled manually above
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