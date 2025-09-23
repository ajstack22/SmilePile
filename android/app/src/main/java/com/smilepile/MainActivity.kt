package com.smilepile

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.smilepile.mode.AppMode
import com.smilepile.security.SecureActivity
import com.smilepile.security.SecurePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.smilepile.ui.screens.MainScreen
import com.smilepile.ui.theme.SmilePileTheme
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.ui.viewmodels.ThemeViewModel

@AndroidEntryPoint
class MainActivity : SecureActivity() {

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    private val themeViewModel: ThemeViewModel by viewModels()
    private val modeViewModel: AppModeViewModel by viewModels()

    private var showKidsModeExitDialog by mutableStateOf(false)

    private val kidsBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Check if PIN is enabled
            if (securePreferencesManager.isPINEnabled()) {
                // Show PIN dialog if PIN is set
                showKidsModeExitDialog = true
            } else {
                // No PIN set, switch directly to Parent Mode
                modeViewModel.forceParentMode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Add callback for Kids Mode back press handling
        onBackPressedDispatcher.addCallback(this, kidsBackPressedCallback)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val modeState by modeViewModel.uiState.collectAsState()
            val currentMode = modeState.currentMode

            // Update back callback enabled state based on mode and fullscreen state
            // Only enable back handling in Kids Mode when NOT in fullscreen
            kidsBackPressedCallback.isEnabled = (currentMode == AppMode.KIDS && !modeState.isKidsFullscreen)

            // Configure system UI for current mode and theme
            setupSystemUI(isDarkMode, currentMode)

            SmilePileTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainScreen(
                        navController = navController,
                        showKidsModeExitDialog = showKidsModeExitDialog,
                        onKidsModeExitDialogDismiss = { showKidsModeExitDialog = false },
                        modeViewModel = modeViewModel
                    )
                }
            }
        }
    }

    private fun setupSystemUI(isDarkMode: Boolean, currentMode: AppMode) {
        // Configure window for edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set status bar colors based on theme and mode (only in Parent Mode)
        if (currentMode == AppMode.PARENT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val statusBarColor = if (isDarkMode) {
                Color(0xFF121212).toArgb() // Dark theme status bar
            } else {
                Color(0xFFFFFBFE).toArgb() // Light theme status bar
            }
            window.statusBarColor = statusBarColor

            // Set light status bar icons for light theme
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
        }

        // Make the app more immersive for children in Kids Mode
        if (currentMode == AppMode.KIDS) {
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.apply {
                // Configure system bars behavior for immersive experience
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both status bar and navigation bar in Kids Mode for full immersion
                hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            // Show all system bars in Parent Mode
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-setup system UI when focus is regained
            val isDarkMode = themeViewModel.isDarkMode.value
            val currentMode = modeViewModel.uiState.value.currentMode
            setupSystemUI(isDarkMode, currentMode)
        }
    }

}