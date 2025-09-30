package com.smilepile.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.smilepile.ui.theme.SmilePileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            SmilePileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        uiState = uiState,
                        onNavigateNext = { viewModel.navigateNext() },
                        onNavigateBack = { viewModel.navigateBack() },
                        onSkip = { viewModel.skip() },
                        onCategoryAdded = { viewModel.addCategory(it) },
                        onCategoryRemoved = { viewModel.removeCategory(it) },
                        onPhotosSelected = { viewModel.setImportedPhotos(it) },
                        onPinSet = { viewModel.setPinCode(it) },
                        onComplete = {
                            viewModel.completeOnboarding()
                            finish() // Close onboarding and return to main activity
                        }
                    )
                }
            }
        }
    }
}