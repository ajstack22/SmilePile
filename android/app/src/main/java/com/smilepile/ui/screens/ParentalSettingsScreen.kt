package com.smilepile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.R
import com.smilepile.ui.viewmodels.ParentalControlsViewModel

/**
 * Parental settings screen for managing child safety controls
 * Accessible only after successful authentication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalSettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToLock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentalControlsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showPatternChangeDialog by remember { mutableStateOf(false) }
    var showInitialSetupDialog by remember { mutableStateOf(false) }

    // Show initial setup if no security is configured
    if (uiState.showInitialSetup && !showInitialSetupDialog) {
        showInitialSetupDialog = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.parental_controls),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Lock button to exit settings
                    TextButton(
                        onClick = {
                            viewModel.logout()
                            onNavigateUp()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.lock)
                        )
                        Text(
                            text = stringResource(R.string.lock),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Security Settings Section
            item {
                SettingsSection(
                    title = stringResource(R.string.setup_security),
                    icon = Icons.Default.Security
                ) {
                    uiState.securitySummary?.let { summary ->
                        // PIN Settings
                        SettingsActionItem(
                            title = if (summary.hasPIN) {
                                stringResource(R.string.change_pin)
                            } else {
                                stringResource(R.string.setup_pin_title)
                            },
                            subtitle = if (summary.hasPIN) {
                                "PIN is currently active"
                            } else {
                                stringResource(R.string.setup_pin_subtitle)
                            },
                            icon = Icons.Default.Pin,
                            onClick = { showPinChangeDialog = true },
                            trailingContent = {
                                if (summary.hasPIN) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        // Pattern Settings
                        SettingsActionItem(
                            title = if (summary.hasPattern) {
                                stringResource(R.string.change_pattern)
                            } else {
                                stringResource(R.string.setup_pattern_title)
                            },
                            subtitle = if (summary.hasPattern) {
                                "Pattern is currently active"
                            } else {
                                stringResource(R.string.setup_pattern_subtitle)
                            },
                            icon = Icons.Default.Pattern,
                            onClick = { showPatternChangeDialog = true },
                            trailingContent = {
                                if (summary.hasPattern) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        // Security Status
                        if (!summary.hasPIN && !summary.hasPattern) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_security_warning),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Child Safety Settings Section
            item {
                SettingsSection(
                    title = stringResource(R.string.child_safety_settings),
                    icon = Icons.Default.ChildFriendly
                ) {
                    uiState.securitySummary?.let { summary ->
                        // Kid-Safe Mode
                        SettingsToggleItem(
                            title = stringResource(R.string.kid_safe_mode),
                            subtitle = stringResource(R.string.kid_safe_mode_subtitle),
                            icon = Icons.Default.ChildFriendly,
                            checked = summary.kidSafeModeEnabled,
                            onCheckedChange = { viewModel.toggleKidSafeMode() }
                        )


                        // Delete Protection
                        SettingsToggleItem(
                            title = stringResource(R.string.delete_protection),
                            subtitle = stringResource(R.string.delete_protection_subtitle),
                            icon = Icons.Default.Delete,
                            checked = summary.deleteProtectionEnabled,
                            onCheckedChange = { viewModel.toggleDeleteProtection() }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // PIN Change Dialog
    if (showPinChangeDialog) {
        PinSetupDialog(
            onDismiss = { showPinChangeDialog = false },
            onConfirm = { pin ->
                // If this is initial setup (no PIN exists), use setupPIN instead of changePIN
                if (uiState.securitySummary?.hasPIN == false) {
                    viewModel.setupPIN(pin)
                } else {
                    viewModel.changePIN(pin)
                }
                showPinChangeDialog = false
            },
            isChange = uiState.securitySummary?.hasPIN == true
        )
    }

    // Pattern Change Dialog
    if (showPatternChangeDialog) {
        PatternSetupDialog(
            onDismiss = { showPatternChangeDialog = false },
            onConfirm = { pattern ->
                // If this is initial setup (no pattern exists), use setupPattern instead of changePattern
                if (uiState.securitySummary?.hasPattern == false) {
                    viewModel.setupPattern(pattern)
                } else {
                    viewModel.changePattern(pattern)
                }
                showPatternChangeDialog = false
            },
            isChange = uiState.securitySummary?.hasPattern == true
        )
    }

    // Initial Setup Dialog
    if (showInitialSetupDialog) {
        InitialSecuritySetupDialog(
            onDismiss = { showInitialSetupDialog = false },
            onSetupPin = {
                // Open the PIN setup dialog instead of setting hardcoded PIN
                showInitialSetupDialog = false
                showPinChangeDialog = true
            },
            onSetupPattern = {
                // Open the pattern setup dialog instead of setting hardcoded pattern
                showInitialSetupDialog = false
                showPatternChangeDialog = true
            },
            onSkip = {
                showInitialSetupDialog = false
            }
        )
    }
}

/**
 * Settings section container with title and icon
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            content()
        }
    }
}

/**
 * Settings item with toggle switch
 */
@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

/**
 * Settings item with action button
 */
@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * PIN setup/change dialog
 */
@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isChange: Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isChange) {
                    stringResource(R.string.change_pin)
                } else {
                    stringResource(R.string.setup_pin_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showConfirm) {
                    Text(
                        text = stringResource(R.string.setup_pin_subtitle),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN input field
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value ->
                            // Only allow digits and limit to 6 characters
                            if (value.all { it.isDigit() } && value.length <= 6) {
                                pin = value
                                error = null
                            }
                        },
                        label = { Text("Enter PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.confirm_pin),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm PIN input field
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { value ->
                            // Only allow digits and limit to 6 characters
                            if (value.all { it.isDigit() } && value.length <= 6) {
                                confirmPin = value
                                error = null
                            }
                        },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pin == confirmPin) {
                                    onConfirm(pin)
                                } else {
                                    error = "PINs do not match"
                                }
                            }
                        ),
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!showConfirm) {
                        if (pin.length in 4..6) {
                            showConfirm = true
                            confirmPin = ""
                            error = null
                        } else {
                            error = "PIN must be 4-6 digits"
                        }
                    } else {
                        if (pin == confirmPin) {
                            onConfirm(pin)
                        } else {
                            error = "PINs do not match"
                        }
                    }
                },
                enabled = if (!showConfirm) pin.isNotEmpty() else confirmPin.isNotEmpty()
            ) {
                Text(if (showConfirm) stringResource(R.string.save) else stringResource(R.string.continue_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Pattern setup/change dialog
 */
@Composable
private fun PatternSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    isChange: Boolean
) {
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var confirmPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isChange) {
                    stringResource(R.string.change_pattern)
                } else {
                    stringResource(R.string.setup_pattern_title)
                }
            )
        },
        text = {
            Column {
                if (!showConfirm) {
                    Text(stringResource(R.string.setup_pattern_subtitle))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pattern: ${pattern.size} dots connected",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(stringResource(R.string.confirm_pattern))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Confirm: ${confirmPattern.size} dots connected",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!showConfirm) {
                        if (pattern.size >= 4) {
                            showConfirm = true
                            error = null
                        } else {
                            error = "Pattern must connect at least 4 dots"
                        }
                    } else {
                        if (pattern == confirmPattern) {
                            onConfirm(pattern)
                        } else {
                            error = "Patterns do not match"
                        }
                    }
                }
            ) {
                Text(if (showConfirm) stringResource(R.string.save) else stringResource(R.string.continue_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Initial security setup dialog
 */
@Composable
private fun InitialSecuritySetupDialog(
    onDismiss: () -> Unit,
    onSetupPin: () -> Unit,
    onSetupPattern: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.initial_setup))
        },
        text = {
            Column {
                Text(stringResource(R.string.setup_security_subtitle))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onSetupPin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Pin, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setup PIN")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSetupPattern,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Pattern, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setup Pattern")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}