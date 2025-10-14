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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.smilepile.utils.BrowserHelper
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
    @Suppress("UNUSED_PARAMETER") onNavigateUp: () -> Unit,
    onNavigateToKidsMode: () -> Unit = {},
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogState = rememberDialogState()

    val launchers = rememberBackupLaunchers(viewModel)
    val pinDialogHandlers = rememberPinDialogHandlers(viewModel, dialogState)

    Scaffold(
        modifier = modifier,
        topBar = {
            AppHeaderComponent(
                onViewModeClick = onNavigateToKidsMode,
                showViewModeButton = true
            )
        }
    ) { scaffoldPaddingValues ->
        SettingsContent(
            scaffoldPaddingValues = scaffoldPaddingValues,
            paddingValues = paddingValues,
            uiState = uiState,
            viewModel = viewModel,
            launchers = launchers,
            onShowAboutDialog = { dialogState.showAbout.value = true },
            onShowPinDialog = { dialogState.showPinSetup.value = true },
            onShowChangePinDialog = { dialogState.showChangePinDialog.value = true }
        )
    }

    SettingsDialogs(
        dialogState = dialogState,
        uiState = uiState,
        pinDialogHandlers = pinDialogHandlers,
        viewModel = viewModel
    )
}

// MARK: - Helper Data Classes and Composables

@Composable
private fun rememberDialogState(): DialogState {
    return remember {
        DialogState(
            showAbout = mutableStateOf(false),
            showPinSetup = mutableStateOf(false),
            showChangePinDialog = mutableStateOf(false)
        )
    }
}

@Composable
private fun rememberBackupLaunchers(viewModel: SettingsViewModel): BackupLaunchers {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.completeExport(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromUri(it) }
    }

    return remember { BackupLaunchers(exportLauncher, importLauncher) }
}

@Composable
private fun rememberPinDialogHandlers(
    viewModel: SettingsViewModel,
    dialogState: DialogState
): PinDialogHandlers {
    var changePinError by remember { mutableStateOf<String?>(null) }

    return remember(viewModel, dialogState) {
        PinDialogHandlers(
            onSetPinConfirm = { pin ->
                viewModel.setPIN(pin)
                dialogState.showPinSetup.value = false
            },
            onChangePinConfirm = { oldPin, newPin ->
                if (viewModel.changePIN(oldPin, newPin)) {
                    dialogState.showChangePinDialog.value = false
                    changePinError = null
                    true
                } else {
                    changePinError = "Current PIN is incorrect"
                    false
                }
            },
            changePinError = changePinError,
            onClearError = { changePinError = null }
        )
    }
}

@Composable
private fun SettingsContent(
    scaffoldPaddingValues: PaddingValues,
    paddingValues: PaddingValues,
    uiState: com.smilepile.ui.viewmodels.SettingsUiState,
    viewModel: SettingsViewModel,
    launchers: BackupLaunchers,
    onShowAboutDialog: () -> Unit,
    onShowPinDialog: () -> Unit,
    onShowChangePinDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPaddingValues)
            .padding(bottom = paddingValues.calculateBottomPadding())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppearanceSection(
                themeMode = uiState.themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) }
            )
        }

        item {
            SecuritySection(
                hasPIN = uiState.hasPIN,
                biometricEnabled = uiState.biometricEnabled,
                isBiometricAvailable = viewModel.isBiometricAvailable(),
                onSetPIN = onShowPinDialog,
                onChangePIN = onShowChangePinDialog,
                onRemovePIN = { viewModel.removePIN() },
                onBiometricToggle = { viewModel.setBiometricEnabled(it) }
            )
        }

        item {
            DataSection(
                onExport = {
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    launchers.exportLauncher.launch("smilepile_backup_$timestamp.zip")
                },
                onImport = {
                    launchers.importLauncher.launch(arrayOf("application/zip", "*/*"))
                },
                onClearAllData = { viewModel.resetAppForOnboarding() },
                hasPIN = uiState.hasPIN,
                viewModel = viewModel
            )
        }

        item {
            AboutSection(onAboutClick = onShowAboutDialog)
        }
    }
}

@Composable
private fun SettingsDialogs(
    dialogState: DialogState,
    uiState: com.smilepile.ui.viewmodels.SettingsUiState,
    pinDialogHandlers: PinDialogHandlers,
    viewModel: SettingsViewModel
) {
    if (dialogState.showAbout.value) {
        AboutDialog(onDismiss = { dialogState.showAbout.value = false })
    }

    if (dialogState.showPinSetup.value) {
        PinSetupDialog(
            onDismiss = { dialogState.showPinSetup.value = false },
            onConfirm = pinDialogHandlers.onSetPinConfirm
        )
    }

    if (dialogState.showChangePinDialog.value) {
        ChangePinDialog(
            onDismiss = {
                dialogState.showChangePinDialog.value = false
                pinDialogHandlers.onClearError()
            },
            onConfirm = { oldPin, newPin ->
                pinDialogHandlers.onChangePinConfirm(oldPin, newPin)
            },
            error = pinDialogHandlers.changePinError
        )
    }

    if (uiState.isLoading || uiState.exportProgress != null) {
        ExportProgressDialog(
            progress = uiState.exportProgress,
            onDismiss = { /* Can't dismiss while exporting */ }
        )
    }

    uiState.importProgress?.let { progress ->
        ImportProgressDialog(
            progress = progress,
            onDismiss = { viewModel.clearImportProgress() }
        )
    }
}

// MARK: - Data Classes

private data class DialogState(
    val showAbout: androidx.compose.runtime.MutableState<Boolean>,
    val showPinSetup: androidx.compose.runtime.MutableState<Boolean>,
    val showChangePinDialog: androidx.compose.runtime.MutableState<Boolean>
)

private data class BackupLaunchers(
    val exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    val importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
)

private data class PinDialogHandlers(
    val onSetPinConfirm: (String) -> Unit,
    val onChangePinConfirm: (String, String) -> Boolean,
    val changePinError: String?,
    val onClearError: () -> Unit
)

// MARK: - Settings Sections

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.settings_appearance),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Column {
            RadioButtonRow(
                isSelected = themeMode == ThemeMode.SYSTEM,
                icon = Icons.Default.PhoneAndroid,
                title = "System",
                subtitle = "Automatic",
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
            )
            SectionDivider()

            RadioButtonRow(
                isSelected = themeMode == ThemeMode.LIGHT,
                icon = Icons.Default.LightMode,
                title = "Light",
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            SectionDivider()

            RadioButtonRow(
                isSelected = themeMode == ThemeMode.DARK,
                icon = Icons.Default.DarkMode,
                title = "Dark",
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun SecuritySection(
    hasPIN: Boolean,
    biometricEnabled: Boolean,
    isBiometricAvailable: Boolean,
    onSetPIN: () -> Unit,
    onChangePIN: () -> Unit,
    onRemovePIN: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit
) {
    SettingsSection(title = "Security") {
        Column {
            PINSettingsItem(
                hasPIN = hasPIN,
                onSetPIN = onSetPIN,
                onChangePIN = onChangePIN
            )

            if (hasPIN && isBiometricAvailable) {
                SectionDivider()
                SettingsToggleItem(
                    title = "Use Biometrics",
                    subtitle = "Quick access with fingerprint",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricToggle
                )
            }

            if (hasPIN) {
                SectionDivider()
                SettingsActionItem(
                    title = "Remove PIN",
                    subtitle = "Disable PIN protection",
                    icon = Icons.Default.LockOpen,
                    onClick = onRemovePIN
                )
            }
        }
    }
}

@Composable
private fun PINSettingsItem(
    hasPIN: Boolean,
    onSetPIN: () -> Unit,
    onChangePIN: () -> Unit
) {
    SettingsActionItem(
        title = if (hasPIN) "Change PIN" else "Set PIN",
        subtitle = if (hasPIN) {
            "PIN protection is enabled for Parent Mode"
        } else {
            "Set a PIN to protect Parent Mode access"
        },
        icon = Icons.Default.Lock,
        onClick = if (hasPIN) onChangePIN else onSetPIN
    )
}

@Composable
private fun DataSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAllData: () -> Unit,
    hasPIN: Boolean,
    viewModel: SettingsViewModel
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showPinVerification by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    SettingsSection(title = "Data") {
        Column {
            SettingsActionItem(
                title = "Export",
                subtitle = "Save your photos and categories",
                icon = Icons.Default.Upload,
                onClick = onExport
            )

            SectionDivider()

            SettingsActionItem(
                title = "Import",
                subtitle = "Restore from backup",
                icon = Icons.Default.Download,
                onClick = onImport
            )

            SectionDivider()

            SettingsActionItem(
                title = "Clear All Data",
                subtitle = "Permanently delete all photos, categories, and settings",
                icon = Icons.Default.Delete,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    if (hasPIN) {
                        showPinVerification = true
                    } else {
                        showResetConfirmation = true
                    }
                },
                enabled = !isResetting
            )
        }
    }

    if (showPinVerification) {
        PinVerificationDialog(
            onDismiss = { showPinVerification = false },
            onSuccess = {
                showPinVerification = false
                showResetConfirmation = true
            },
            viewModel = viewModel
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetConfirmation = false },
            title = {
                Text(
                    "Clear All Data?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "This will permanently delete all photos, categories, settings, and PIN. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isResetting = true
                        onClearAllData()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isResetting
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false },
                    enabled = !isResetting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AboutSection(
    onAboutClick: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_about)) {
        SettingsActionItem(
            title = "SmilePile",
            subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            icon = Icons.Default.Info,
            onClick = onAboutClick
        )
    }
}


@Composable
private fun PinVerificationDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SettingsViewModel
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter your PIN to continue")

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
                    // Use proper PIN validation method instead of changePIN
                    if (viewModel.validatePIN(pin)) {
                        onSuccess()
                    } else {
                        error = "Incorrect PIN"
                    }
                }
            ) {
                Text("Verify")
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
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

// MARK: - Dialogs

/**
 * About app dialog
 */
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    val linkHandler = rememberAboutLinkHandler()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AboutDialogTitle() },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutDescriptionSection()
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutLinksSection(linkHandler)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )

    AboutErrorDialog(linkHandler)
}

@Composable
private fun AboutDialogTitle() {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
private fun AboutDescriptionSection() {
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

@Composable
private fun AboutLinksSection(linkHandler: AboutLinkHandler) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AboutLinkItem(
            label = stringResource(R.string.privacy_policy_label),
            onClickLabel = "Open privacy policy in browser",
            onClick = { linkHandler.openPrivacyPolicy() },
            enabled = !linkHandler.isProcessing
        )
        AboutLinkItem(
            label = stringResource(R.string.terms_of_service_label),
            onClickLabel = "Open terms of service in browser",
            onClick = { linkHandler.openTermsOfService() },
            enabled = !linkHandler.isProcessing
        )
        AboutLinkItem(
            label = stringResource(R.string.support_label),
            onClickLabel = "Send support email",
            onClick = { linkHandler.openSupport() },
            enabled = !linkHandler.isProcessing
        )
    }
}

@Composable
private fun AboutLinkItem(
    label: String,
    onClickLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AboutErrorDialog(linkHandler: AboutLinkHandler) {
    if (linkHandler.showError) {
        AlertDialog(
            onDismissRequest = { linkHandler.dismissError() },
            title = { Text("Error") },
            text = { Text(linkHandler.errorMessage) },
            confirmButton = {
                TextButton(onClick = { linkHandler.dismissError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun rememberAboutLinkHandler(): AboutLinkHandler {
    val context = LocalContext.current
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    return remember(showError, errorMessage, isProcessing) {
        AboutLinkHandler(
            context = context,
            showError = showError,
            errorMessage = errorMessage,
            isProcessing = isProcessing,
            onShowError = { message ->
                errorMessage = message
                showError = true
            },
            onDismissError = { showError = false },
            onStartProcessing = { isProcessing = true },
            onStopProcessing = { isProcessing = false }
        )
    }
}

private class AboutLinkHandler(
    private val context: android.content.Context,
    val showError: Boolean,
    val errorMessage: String,
    val isProcessing: Boolean,
    private val onShowError: (String) -> Unit,
    private val onDismissError: () -> Unit,
    private val onStartProcessing: () -> Unit,
    private val onStopProcessing: () -> Unit
) {
    private fun handleLink(action: () -> Boolean, errorMsg: String) {
        if (isProcessing) return

        onStartProcessing()
        if (!action()) {
            onShowError(errorMsg)
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onStopProcessing()
        }, 500)
    }

    fun openPrivacyPolicy() {
        handleLink(
            action = {
                BrowserHelper.openUrl(context, context.getString(R.string.privacy_policy_url))
            },
            errorMsg = context.getString(R.string.error_browser_unavailable)
        )
    }

    fun openTermsOfService() {
        handleLink(
            action = {
                BrowserHelper.openUrl(context, context.getString(R.string.terms_of_service_url))
            },
            errorMsg = context.getString(R.string.error_browser_unavailable)
        )
    }

    fun openSupport() {
        handleLink(
            action = {
                BrowserHelper.openEmailClient(context, context.getString(R.string.support_email))
            },
            errorMsg = context.getString(R.string.error_email_unavailable, context.getString(R.string.support_email))
        )
    }

    fun dismissError() {
        onDismissError()
    }
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

