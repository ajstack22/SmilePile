package com.smilepile

import android.content.res.Configuration
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
import com.smilepile.theme.ThemeManager
import com.smilepile.settings.SettingsManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import com.smilepile.onboarding.OnboardingActivity
import com.smilepile.data.repository.CategoryRepository
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : SecureActivity() {

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var categoryRepository: CategoryRepository

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

        // Check for first launch and show onboarding if needed
        lifecycleScope.launch {
            if (shouldShowOnboarding()) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish() // Close main activity until onboarding is complete
                return@launch
            }
        }

        // Initialize settings on first launch
        initializeSettings()

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

        // Set transparent status bar to allow our Surface to show through
        if (currentMode == AppMode.PARENT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = Color.Transparent.toArgb()

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update theme when system dark mode changes
        themeManager.onConfigurationChanged(newConfig)
    }

    private fun initializeSettings() {
        lifecycleScope.launch {
            // Check if this is the first launch
            settingsManager.isFirstLaunch().collect { isFirstLaunch ->
                if (isFirstLaunch) {
                    // Set up default settings for first launch
                    settingsManager.setFirstLaunch(false)
                    settingsManager.setKidsModeEnabled(true)
                    settingsManager.setNotificationsEnabled(true)
                    settingsManager.setPreserveMetadata(true)
                    settingsManager.setShowPhotoDates(true)
                }
            }

            // Migrate from SharedPreferences if needed
            val sharedPrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
            if (sharedPrefs.contains("theme_mode")) {
                settingsManager.migrateFromSharedPreferences(sharedPrefs)
                // Clear old preferences after migration
                sharedPrefs.edit().clear().apply()
            }
        }
    }

    private suspend fun shouldShowOnboarding(): Boolean {
        // Check if onboarding has been completed
        val hasCompletedOnboarding = settingsManager.hasCompletedOnboarding().first()

        if (!hasCompletedOnboarding) {
            // Check if we have existing data (migrating user)
            val categories = categoryRepository.getAllCategories()
            if (categories.isEmpty()) {
                // First time launch, show onboarding
                return true
            } else {
                // Has data but no onboarding flag - mark as complete (migrating user)
                settingsManager.setOnboardingCompleted(true)
            }
        }

        return false
    }
}