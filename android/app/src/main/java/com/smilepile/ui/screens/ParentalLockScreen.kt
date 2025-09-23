package com.smilepile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.fragment.app.FragmentActivity
import com.smilepile.R
import com.smilepile.ui.viewmodels.AuthenticationMode
import com.smilepile.ui.viewmodels.ParentalControlsViewModel
import kotlinx.coroutines.delay

/**
 * Parental lock screen with PIN entry and pattern lock alternatives
 * Features kid-friendly design with retry limits and cooldown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalLockScreen(
    onUnlocked: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentalControlsViewModel = hiltViewModel()
) {
    val uiState by viewModel.lockUiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // If no PIN/Pattern is set up, go directly to settings
    LaunchedEffect(uiState) {
        val securitySummary = viewModel.uiState.value.securitySummary
        if (securitySummary != null && !securitySummary.hasPIN && !securitySummary.hasPattern) {
            onUnlocked() // This will navigate to ParentalSettingsScreen
        }
    }

    // Handle biometric authentication prompt
    LaunchedEffect(uiState.showBiometricPrompt) {
        if (uiState.showBiometricPrompt && activity != null) {
            viewModel.authenticateWithBiometrics(activity)
        }
    }

    // Auto-refresh cooldown timer
    LaunchedEffect(uiState.isInCooldown) {
        if (uiState.isInCooldown) {
            while (uiState.cooldownTimeRemaining > 0) {
                delay(1000)
                viewModel.refreshCooldownTime()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.parental_lock_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Lock Icon and Title
                Card(
                    modifier = Modifier.size(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.cd_parental_lock),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Title and Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.parental_lock_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.parental_lock_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Kid-friendly message or cooldown
                AnimatedVisibility(
                    visible = uiState.showKidFriendlyMessage || uiState.isInCooldown,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isInCooldown) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.isInCooldown) {
                                Text(
                                    text = stringResource(
                                        R.string.parental_lock_cooldown_message,
                                        (uiState.cooldownTimeRemaining / 1000).toInt()
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.parental_lock_kid_message),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Authentication Interface
                if (!uiState.isInCooldown) {
                    when (uiState.authenticationMode) {
                        AuthenticationMode.BIOMETRIC -> {
                            BiometricAuthenticationInterface(
                                onRetryClick = {
                                    activity?.let { viewModel.authenticateWithBiometrics(it) }
                                },
                                onUsePinClick = { viewModel.dismissBiometricPrompt() },
                                isLoading = uiState.isLoading
                            )
                        }
                        AuthenticationMode.PIN -> {
                            PINEntryInterface(
                                pinInput = uiState.pinInput,
                                onPinChange = viewModel::updatePinInput,
                                onSubmit = {
                                    if (viewModel.validatePin()) {
                                        onUnlocked()
                                    }
                                },
                                error = uiState.error,
                                failedAttempts = uiState.failedAttempts,
                                maxAttempts = uiState.maxAttempts
                            )
                        }
                        AuthenticationMode.PATTERN -> {
                            PatternEntryInterface(
                                patternInput = uiState.patternInput,
                                onPatternChange = viewModel::updatePatternInput,
                                onSubmit = {
                                    if (viewModel.validatePattern()) {
                                        onUnlocked()
                                    }
                                },
                                error = uiState.error,
                                failedAttempts = uiState.failedAttempts,
                                maxAttempts = uiState.maxAttempts
                            )
                        }
                    }

                    // Mode Switch Button
                    TextButton(
                        onClick = { viewModel.switchAuthenticationMode() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        val switchText = when (uiState.authenticationMode) {
                            AuthenticationMode.BIOMETRIC -> if (viewModel.uiState.value.securitySummary?.hasPIN == true) {
                                "Use PIN Instead"
                            } else {
                                "Use Pattern Instead"
                            }
                            AuthenticationMode.PIN -> if (viewModel.uiState.value.securitySummary?.hasPattern == true) {
                                stringResource(R.string.switch_to_pattern)
                            } else if (uiState.biometricEnabled) {
                                "Use Biometric"
                            } else {
                                stringResource(R.string.switch_to_pattern)
                            }
                            AuthenticationMode.PATTERN -> if (uiState.biometricEnabled) {
                                "Use Biometric"
                            } else {
                                stringResource(R.string.switch_to_pin)
                            }
                        }
                        Text(
                            text = switchText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Back Button
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        }
    }
}

/**
 * PIN entry interface with numeric keypad
 */
@Composable
private fun PINEntryInterface(
    pinInput: String,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    failedAttempts: Int,
    maxAttempts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PIN Display
        PINDisplay(
            pin = pinInput,
            maxLength = 6
        )

        // Error Message
        AnimatedVisibility(visible = error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Failed Attempts Display
        if (failedAttempts > 0) {
            Text(
                text = stringResource(R.string.failed_attempts, failedAttempts, maxAttempts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Numeric Keypad
        NumericKeypad(
            onNumberClick = { digit ->
                if (pinInput.length < 6) {
                    onPinChange(pinInput + digit)
                }
            },
            onBackspaceClick = {
                if (pinInput.isNotEmpty()) {
                    onPinChange(pinInput.dropLast(1))
                }
            },
            onSubmitClick = {
                if (pinInput.length >= 4) {
                    onSubmit()
                }
            },
            enabled = pinInput.length >= 4
        )
    }
}

/**
 * Pattern entry interface with 3x3 grid
 */
@Composable
private fun PatternEntryInterface(
    patternInput: List<Int>,
    onPatternChange: (List<Int>) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    failedAttempts: Int,
    maxAttempts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pattern Display
        Text(
            text = "${stringResource(R.string.enter_pattern)} (${patternInput.size} dots)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Error Message
        AnimatedVisibility(visible = error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Failed Attempts Display
        if (failedAttempts > 0) {
            Text(
                text = stringResource(R.string.failed_attempts, failedAttempts, maxAttempts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Pattern Grid
        PatternGrid(
            selectedDots = patternInput,
            onPatternChange = onPatternChange
        )

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onPatternChange(emptyList()) },
                enabled = patternInput.isNotEmpty()
            ) {
                Text(stringResource(R.string.clear))
            }

            Button(
                onClick = onSubmit,
                enabled = patternInput.size >= 4
            ) {
                Text(stringResource(R.string.unlock))
            }
        }
    }
}

/**
 * PIN display component with masked circles
 */
@Composable
private fun PINDisplay(
    pin: String,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Only show dots for entered digits, not empty placeholders
        repeat(pin.length) { index ->
            val scale by animateFloatAsState(
                targetValue = 1.2f,
                label = "pin_scale"
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Numeric keypad for PIN entry
 */
@Composable
private fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rows 1-3
        repeat(3) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
            ) {
                repeat(3) { col ->
                    val number = (row * 3 + col + 1).toString()
                    KeypadButton(
                        text = number,
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }

        // Bottom row with 0, backspace, and submit
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KeypadButton(
                text = "",
                onClick = { },
                enabled = false,
                modifier = Modifier.alpha(0f)
            )

            KeypadButton(
                text = "0",
                onClick = { onNumberClick("0") }
            )

            KeypadButton(
                icon = Icons.Default.Backspace,
                onClick = onBackspaceClick,
                contentDescription = stringResource(R.string.cd_delete_digit)
            )
        }

        // Submit Button
        Button(
            onClick = onSubmitClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.unlock),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Individual keypad button
 */
@Composable
private fun KeypadButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 3x3 pattern grid for pattern lock
 */
@Composable
private fun PatternGrid(
    selectedDots: List<Int>,
    onPatternChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) { col ->
                    val dotIndex = row * 3 + col
                    val isSelected = selectedDots.contains(dotIndex)

                    PatternDot(
                        isSelected = isSelected,
                        onClick = {
                            if (!selectedDots.contains(dotIndex)) {
                                onPatternChange(selectedDots + dotIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual pattern dot
 */
@Composable
private fun PatternDot(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1f,
        label = "dot_scale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            )
            .border(
                3.dp,
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

/**
 * Biometric authentication interface
 */
@Composable
private fun BiometricAuthenticationInterface(
    onRetryClick: () -> Unit,
    onUsePinClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Biometric Icon
        Card(
            modifier = Modifier.size(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Authentication",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Instructions
        Text(
            text = "Touch the fingerprint sensor or look at the camera",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Action Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRetryClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    Text("Authenticating...")
                } else {
                    Text("Try Again")
                }
            }

            OutlinedButton(
                onClick = onUsePinClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use PIN Instead")
            }
        }
    }
}