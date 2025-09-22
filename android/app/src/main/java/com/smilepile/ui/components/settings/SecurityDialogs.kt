package com.smilepile.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smilepile.R
import com.smilepile.ui.components.dialogs.ActionConfig
import com.smilepile.ui.components.dialogs.ActionStyle
import com.smilepile.ui.components.dialogs.DialogBuilder
import com.smilepile.ui.components.dialogs.DialogConfig
import com.smilepile.ui.components.dialogs.DialogType
import com.smilepile.ui.components.dialogs.UniversalCrudDialog

/**
 * PIN setup dialog using UniversalCrudDialog
 */
@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isChange: Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val dialogConfig = DialogConfig(
        type = DialogType.CUSTOM,
        title = if (isChange) {
            stringResource(R.string.change_pin)
        } else {
            stringResource(R.string.setup_pin_title)
        },
        icon = Icons.Default.Pin,
        primaryAction = ActionConfig(
            text = if (showConfirm) stringResource(R.string.save) else stringResource(R.string.continue_button),
            style = ActionStyle.FILLED,
            enabled = if (!showConfirm) pin.isNotEmpty() else confirmPin.isNotEmpty(),
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
            }
        ),
        dismissAction = ActionConfig(
            text = stringResource(R.string.cancel),
            style = ActionStyle.TEXT,
            onClick = onDismiss
        ),
        content = {
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
        }
    )

    UniversalCrudDialog(
        config = dialogConfig,
        onDismiss = onDismiss
    )
}

/**
 * Pattern setup dialog using UniversalCrudDialog
 */
@Composable
fun PatternSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    isChange: Boolean
) {
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var confirmPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val dialogConfig = DialogConfig(
        type = DialogType.CUSTOM,
        title = if (isChange) {
            stringResource(R.string.change_pattern)
        } else {
            stringResource(R.string.setup_pattern_title)
        },
        icon = Icons.Default.Pattern,
        primaryAction = ActionConfig(
            text = if (showConfirm) stringResource(R.string.save) else stringResource(R.string.continue_button),
            style = ActionStyle.FILLED,
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
        ),
        dismissAction = ActionConfig(
            text = stringResource(R.string.cancel),
            style = ActionStyle.TEXT,
            onClick = onDismiss
        ),
        content = {
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
        }
    )

    UniversalCrudDialog(
        config = dialogConfig,
        onDismiss = onDismiss
    )
}

/**
 * Initial security setup dialog using UniversalCrudDialog
 */
@Composable
fun InitialSecuritySetupDialog(
    onDismiss: () -> Unit,
    onSetupPin: () -> Unit,
    onSetupPattern: () -> Unit,
    onSkip: () -> Unit
) {
    val dialogConfig = DialogConfig(
        type = DialogType.CUSTOM,
        title = stringResource(R.string.initial_setup),
        message = stringResource(R.string.setup_security_subtitle),
        icon = Icons.Default.Security,
        primaryAction = ActionConfig(
            text = stringResource(R.string.skip),
            style = ActionStyle.TEXT,
            onClick = onSkip
        ),
        dismissAction = ActionConfig(
            text = stringResource(R.string.cancel),
            style = ActionStyle.TEXT,
            onClick = onDismiss
        ),
        content = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
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
        }
    )

    UniversalCrudDialog(
        config = dialogConfig,
        onDismiss = onDismiss
    )
}