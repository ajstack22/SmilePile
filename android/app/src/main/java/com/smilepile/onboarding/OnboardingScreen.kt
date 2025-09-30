package com.smilepile.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smilepile.onboarding.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
    onSkip: () -> Unit,
    onCategoryAdded: (TempCategory) -> Unit,
    onCategoryRemoved: (TempCategory) -> Unit,
    onPhotosSelected: (List<ImportedPhotoData>) -> Unit,
    onPinSet: (String) -> Unit,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with navigation
            if (uiState.currentStep != OnboardingStep.WELCOME &&
                uiState.currentStep != OnboardingStep.COMPLETE) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentStep) {
                                OnboardingStep.CATEGORIES -> "Create Piles"
                                OnboardingStep.PIN_SETUP -> "PIN Setup"
                                else -> ""
                            },
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }

            // Main content with animation
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "onboarding_content"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> {
                        WelcomeScreen(onGetStarted = onNavigateNext)
                    }
                    OnboardingStep.CATEGORIES -> {
                        CategorySetupScreen(
                            categories = uiState.categories,
                            onCategoryAdded = onCategoryAdded,
                            onCategoryRemoved = onCategoryRemoved,
                            onContinue = onNavigateNext
                        )
                    }
                    OnboardingStep.PIN_SETUP -> {
                        PinSetupScreen(
                            onPinSet = { pin ->
                                onPinSet(pin)
                                onNavigateNext()
                            },
                            onSkip = {
                                onSkip()
                            }
                        )
                    }
                    OnboardingStep.COMPLETE -> {
                        CompletionScreen(
                            categories = uiState.categories,
                            photosImported = 0,
                            pinEnabled = !uiState.skipPin && uiState.pinCode != null,
                            onComplete = onComplete
                        )
                    }
                }
            }
        }

        // Error dialog
        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { /* Clear error */ },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { /* Clear error */ }) {
                        Text("OK")
                    }
                }
            )
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun OnboardingStep.canSkip(): Boolean {
    return this == OnboardingStep.PIN_SETUP
}