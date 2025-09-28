package com.smilepile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.BuildConfig
import com.smilepile.R
import com.smilepile.ui.viewmodels.SettingsViewModel
import com.smilepile.ui.components.AppHeaderComponent
import com.smilepile.ui.components.settings.SettingsSection
import com.smilepile.ui.components.settings.SettingsActionItem
import com.smilepile.ui.components.settings.SettingsToggleItem
import com.smilepile.ui.components.settings.RadioButtonRow
import com.smilepile.theme.ThemeMode

/**
 * Settings screen providing app configuration and management options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToKidsMode: () -> Unit = {},
    onNavigateToParentalControls: () -> Unit = {},
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    // Export launcher for Storage Access Framework
    val exportLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.completeExport(it)
        }
    }

    // Import launcher for Storage Access Framework
    val importLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFromUri(it)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppHeaderComponent(
                onViewModeClick = onNavigateToKidsMode,
                showViewModeButton = true
            )
        }
    ) { scaffoldPaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Appearance Section
                SettingsSection(
                    title = stringResource(R.string.settings_appearance),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Column {
                        // System theme option
                        RadioButtonRow(
                            isSelected = uiState.themeMode == ThemeMode.SYSTEM,
                            icon = Icons.Default.PhoneAndroid,
                            title = "System",
                            subtitle = "Automatic",
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                        )
                        Divider(
                            modifier = Modifier.padding(start = 48.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Light theme option
                        RadioButtonRow(
                            isSelected = uiState.themeMode == ThemeMode.LIGHT,
                            icon = Icons.Default.LightMode,
                            title = "Light",
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        Divider(
                            modifier = Modifier.padding(start = 48.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Dark theme option
                        RadioButtonRow(
                            isSelected = uiState.themeMode == ThemeMode.DARK,
                            icon = Icons.Default.DarkMode,
                            title = "Dark",
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }

            item {
                // Security Section
                SettingsSection(
                    title = "Security"
                ) {
                    Column {
                    SettingsActionItem(
                        title = if (uiState.hasPIN) "Change PIN" else "Set PIN",
                        subtitle = if (uiState.hasPIN) {
                            "PIN protection is enabled for Parent Mode"
                        } else {
                            "Set a PIN to protect Parent Mode access"
                        },
                        icon = Icons.Default.Lock,
                        onClick = {
                            if (uiState.hasPIN) {
                                showChangePinDialog = true
                            } else {
                                showPinDialog = true
                            }
                        }
                    )

                        // Biometric Authentication Toggle
                        if (uiState.hasPIN && viewModel.isBiometricAvailable()) {
                            Divider(
                                modifier = Modifier.padding(start = 48.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                            SettingsToggleItem(
                                title = "Use Biometrics",
                                subtitle = "Quick access with fingerprint",
                                icon = Icons.Default.Fingerprint,
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setBiometricEnabled(enabled)
                                }
                            )
                        }

                        // Parental Controls Access
                        if (uiState.hasPIN) {
                            Divider(
                                modifier = Modifier.padding(start = 48.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                            SettingsActionItem(
                                title = "Parental Controls",
                                subtitle = "Manage child safety settings",
                                icon = Icons.Default.ChildCare,
                                onClick = onNavigateToParentalControls
                            )
                        }

                        if (uiState.hasPIN) {
                            Divider(
                                modifier = Modifier.padding(start = 48.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                            SettingsActionItem(
                                title = "Remove PIN",
                                subtitle = "Disable PIN protection",
                                icon = Icons.Default.LockOpen,
                                onClick = {
                                    viewModel.removePIN()
                                }
                            )
                        }
                    }

                }
            }

            item {
                // Backup & Restore Section
                SettingsSection(
                    title = "Backup & Restore"
                ) {
                    Column {

                        SettingsActionItem(
                            title = "Export Data",
                            subtitle = "Save your photos and categories",
                            icon = Icons.Default.Upload,
                            onClick = {
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            exportLauncher.launch("smilepile_backup_$timestamp.zip")
                        }
                    )

                        Divider(
                            modifier = Modifier.padding(start = 48.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        SettingsActionItem(
                            title = "Import Data",
                            subtitle = "Restore from backup",
                            icon = Icons.Default.Download,
                            onClick = {
                            importLauncher.launch(arrayOf("application/zip", "*/*"))
                        }
                        )
                    }
                }
            }

            // Data Management section removed - clear cache button no longer needed

            item {
                // About Section
                SettingsSection(
                    title = stringResource(R.string.settings_about)
                ) {
                    SettingsActionItem(
                        title = "SmilePile",
                        subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // About App Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.setPIN(pin)
                showPinDialog = false
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var changePinError by remember { mutableStateOf<String?>(null) }

        ChangePinDialog(
            onDismiss = {
                showChangePinDialog = false
                changePinError = null
            },
            onConfirm = { oldPin, newPin ->
                if (viewModel.changePIN(oldPin, newPin)) {
                    showChangePinDialog = false
                    changePinError = null
                } else {
                    changePinError = "Current PIN is incorrect"
                }
            },
            error = changePinError
        )
    }

    // Export Progress Dialog
    if (uiState.isLoading || uiState.exportProgress != null) {
        ExportProgressDialog(
            progress = uiState.exportProgress,
            onDismiss = { /* Can't dismiss while exporting */ }
        )
    }

    // Import Progress Dialog
    uiState.importProgress?.let { progress ->
        ImportProgressDialog(
            progress = progress,
            onDismiss = { viewModel.clearImportProgress() }
        )
    }

}





/**
 * About app dialog
 */
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_about_child_safety),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter a 4-6 digit PIN to protect Parent Mode access")

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            error = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits"
                        pin != confirmPin -> error = "PINs do not match"
                        else -> onConfirm(pin)
                    }
                }
            ) {
                Text("Set PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    error: String? = null
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Display external error (wrong current PIN) or local validation error
    val displayError = error ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            oldPin = it
                            localError = null
                        }
                    },
                    label = { Text("Current PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = displayError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            localError = null
                        }
                    },
                    label = { Text("New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = displayError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmNewPin = it
                            localError = null
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = displayError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                displayError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPin.isEmpty() -> localError = "Enter current PIN"
                        newPin.length < 4 -> localError = "New PIN must be at least 4 digits"
                        newPin != confirmNewPin -> localError = "New PINs do not match"
                        else -> onConfirm(oldPin, newPin)
                    }
                }
            ) {
                Text("Change PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun ImportProgressDialog(
    progress: com.smilepile.data.backup.ImportProgress,
    onDismiss: () -> Unit
) {
    val canDismiss = progress.currentOperation.contains("completed", ignoreCase = true) ||
                    progress.errors.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!canDismiss) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                Text("Importing Data")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = progress.currentOperation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (progress.totalItems > 0) {
                    Text(
                        text = "Progress: ${progress.processedItems}/${progress.totalItems}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (progress.errors.isNotEmpty()) {
                    Text(
                        text = "Errors: ${progress.errors.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (canDismiss && progress.errors.isNotEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    )
}

@Composable
private fun ExportProgressDialog(
    progress: com.smilepile.data.backup.ImportProgress? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Exporting Data")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Creating backup with photos. This may take a moment..."
                )

                progress?.let { prog ->
                    Text(
                        text = prog.currentOperation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (prog.totalItems > 0) {
                        Text(
                            text = "Progress: ${prog.processedItems}/${prog.totalItems}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

