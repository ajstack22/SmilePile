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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B6B).copy(alpha = 0.1f),
                        Color(0xFF4ECDC4).copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress bar (except for welcome and complete screens)
            if (uiState.currentStep != OnboardingStep.WELCOME &&
                uiState.currentStep != OnboardingStep.COMPLETE) {
                LinearProgressIndicator(
                    progress = when (uiState.currentStep) {
                        OnboardingStep.CATEGORIES -> 0.25f
                        OnboardingStep.PHOTO_IMPORT -> 0.5f
                        OnboardingStep.PIN_SETUP -> 0.75f
                        else -> 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFFFF6B6B)
                )
            }

            // Top bar with navigation
            if (uiState.currentStep != OnboardingStep.WELCOME &&
                uiState.currentStep != OnboardingStep.COMPLETE) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentStep) {
                                OnboardingStep.CATEGORIES -> "Create Categories"
                                OnboardingStep.PHOTO_IMPORT -> "Add Photos"
                                OnboardingStep.PIN_SETUP -> "Security Setup"
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
                    actions = {
                        if (uiState.currentStep.canSkip()) {
                            TextButton(onClick = onSkip) {
                                Text("Skip")
                            }
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
                    OnboardingStep.PHOTO_IMPORT -> {
                        PhotoImportScreen(
                            categories = uiState.categories,
                            importedPhotos = uiState.importedPhotos,
                            onPhotosSelected = onPhotosSelected,
                            onContinue = onNavigateNext,
                            onSkip = onSkip
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
                            photosImported = uiState.importedPhotos.size,
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
    return this == OnboardingStep.PHOTO_IMPORT || this == OnboardingStep.PIN_SETUP
}