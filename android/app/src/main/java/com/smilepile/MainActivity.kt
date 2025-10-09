package com.smilepile

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
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
    private var sharedPhotoUris by mutableStateOf<List<Uri>?>(null)
    private var showSharedPhotoCategoryDialog by mutableStateOf(false)

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

    private var hasSetupUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Handle share intent first
        if (handleShareIntent(intent)) {
            return
        }

        // Check for first launch and show onboarding if needed
        lifecycleScope.launch {
            val needsOnboarding = shouldShowOnboarding()

            if (needsOnboarding) {
                val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                startActivity(intent)
                // Don't setup UI yet - will do it when returning from onboarding
                return@launch
            }

            // Only set up the UI after onboarding check
            if (!hasSetupUI) {
                setupMainUI()
                hasSetupUI = true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // If returning from onboarding, set up the UI
        if (!hasSetupUI) {
            lifecycleScope.launch {
                val needsOnboarding = shouldShowOnboarding()
                if (!needsOnboarding) {
                    setupMainUI()
                    hasSetupUI = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)

            Log.d("MainActivity", "onNewIntent called with action: ${it.action}")

            // Reset state before handling new share intent
            sharedPhotoUris = null
            showSharedPhotoCategoryDialog = false

            handleShareIntent(it)
        }
    }

    private fun handleShareIntent(intent: Intent): Boolean {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                    return true
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendMultipleImages(intent)
                    return true
                }
            }
        }
        return false
    }

    private fun handleSendImage(intent: Intent) {
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }

        imageUri?.let { uri ->
            if (isValidContentUri(uri)) {
                showCategorySelectionForSharedPhotos(listOf(uri))
            } else {
                Log.e("MainActivity", "Invalid shared URI: $uri")
            }
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        }

        imageUris?.let { uris ->
            val validUris = uris.filter { isValidContentUri(it) }
            if (validUris.isNotEmpty()) {
                val truncatedUris = if (validUris.size > 50) {
                    Log.w("MainActivity", "Truncating ${validUris.size} photos to 50")
                    validUris.take(50)
                } else {
                    validUris
                }
                showCategorySelectionForSharedPhotos(truncatedUris)
            }
        }
    }

    private fun isValidContentUri(uri: Uri): Boolean {
        return try {
            uri.scheme == "content" &&
            contentResolver.getType(uri)?.startsWith("image/") == true
        } catch (e: Exception) {
            Log.e("MainActivity", "Error validating URI: $uri", e)
            false
        }
    }

    private fun showCategorySelectionForSharedPhotos(uris: List<Uri>) {
        lifecycleScope.launch {
            Log.d("MainActivity", "showCategorySelectionForSharedPhotos: ${uris.size} URIs")

            val needsOnboarding = shouldShowOnboarding()
            if (needsOnboarding) {
                val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                startActivity(intent)
                return@launch
            }

            sharedPhotoUris = uris
            showSharedPhotoCategoryDialog = true

            Log.d("MainActivity", "Set showSharedPhotoCategoryDialog = true, sharedPhotoUris size = ${sharedPhotoUris?.size}")

            if (!hasSetupUI) {
                setupMainUI()
                hasSetupUI = true
            }
        }
    }

    private fun setupMainUI() {
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

                    // Show category selection dialog for shared photos
                    SharedPhotoHandler()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SharedPhotoHandler() {
        if (showSharedPhotoCategoryDialog && sharedPhotoUris != null) {
            val categoryViewModel: com.smilepile.ui.viewmodels.CategoryViewModel by viewModels()
            val importViewModel: com.smilepile.ui.viewmodels.PhotoImportViewModel by viewModels()

            // Clear any previous import state to prevent LaunchedEffects from firing with stale data
            androidx.compose.runtime.LaunchedEffect(sharedPhotoUris) {
                importViewModel.clearMessages()
            }

            val categories by categoryViewModel.categories.collectAsState()
            val importState by importViewModel.uiState.collectAsState()

            com.smilepile.ui.components.CategorySelectionDialog(
                categories = categories,
                selectedCategoryIds = emptySet(),
                multiSelectMode = false,
                title = "Add ${sharedPhotoUris!!.size} photo${if (sharedPhotoUris!!.size > 1) "s" else ""} to which pile?",
                onCategorySelected = { categoryIds ->
                    val categoryId = categoryIds.firstOrNull()
                    if (categoryId != null) {
                        importViewModel.importPhotos(sharedPhotoUris!!, categoryId)
                    }
                    showSharedPhotoCategoryDialog = false
                    sharedPhotoUris = null
                },
                onDismiss = {
                    showSharedPhotoCategoryDialog = false
                    sharedPhotoUris = null
                }
            )

            // Show import progress
            if (importState.isImporting) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    androidx.compose.material3.Card {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Text("Importing photos...")
                            if (importState.isBatchImport) {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.Text(
                                    importState.batchProgressText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Handle import completion
            androidx.compose.runtime.LaunchedEffect(importState.successMessage) {
                importState.successMessage?.let { message ->
                    android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                    sharedPhotoUris = null
                }
            }

            androidx.compose.runtime.LaunchedEffect(importState.error) {
                importState.error?.let { error ->
                    android.widget.Toast.makeText(this@MainActivity, error, android.widget.Toast.LENGTH_LONG).show()
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
        try {
            // Check if onboarding has been completed
            val hasCompletedOnboarding = settingsManager.hasCompletedOnboarding().first()

            if (!hasCompletedOnboarding) {
                // Check if we have existing data (migrating user)
                val categories = categoryRepository.getAllCategories()
                if (categories.isNotEmpty()) {
                    // Has data but no onboarding flag - mark as complete (migrating user)
                    settingsManager.setOnboardingCompleted(true)
                    return false
                }
                // First time launch with no categories, show onboarding
                return true
            }

            return false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking onboarding status", e)
            // On error, assume onboarding needed to be safe
            return true
        }
    }
}