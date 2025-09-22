package com.smilepile.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Universal CRUD Dialog - A configurable dialog component for common operations
 *
 * Replaces multiple specialized dialogs across the app with a single, flexible component.
 * Supports confirmation, input, selection, and custom content dialog types.
 */

enum class DialogType {
    CONFIRMATION,
    INPUT,
    SELECTION,
    CUSTOM,
    INFO
}

enum class ActionStyle {
    TEXT,        // TextButton
    OUTLINED,    // OutlinedButton
    FILLED,      // Button
    DESTRUCTIVE  // Button with error colors
}

data class ActionConfig(
    val text: String,
    val style: ActionStyle = ActionStyle.TEXT,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

data class DialogConfig(
    val type: DialogType,
    val title: String,
    val message: String? = null,
    val icon: ImageVector? = null,
    val primaryAction: ActionConfig,
    val secondaryAction: ActionConfig? = null,
    val dismissAction: ActionConfig? = null,
    val content: @Composable (() -> Unit)? = null
)

/**
 * Universal dialog component that handles common CRUD operations
 */
@Composable
fun UniversalCrudDialog(
    config: DialogConfig,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = config.icon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (config.type) {
                        DialogType.CONFIRMATION -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        title = {
            Text(
                text = config.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // Message text
                config.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Custom content
                config.content?.let { content ->
                    if (config.message != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    content()
                }
            }
        },
        confirmButton = {
            ActionButton(
                config = config.primaryAction,
                isConfirmButton = true
            )
        },
        dismissButton = {
            Row {
                // Secondary action (if provided)
                config.secondaryAction?.let { secondaryAction ->
                    ActionButton(
                        config = secondaryAction,
                        isConfirmButton = false
                    )

                    if (config.dismissAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Dismiss action (if provided, otherwise default cancel)
                val dismissConfig = config.dismissAction ?: ActionConfig(
                    text = "Cancel",
                    style = ActionStyle.TEXT,
                    onClick = onDismiss
                )

                ActionButton(
                    config = dismissConfig,
                    isConfirmButton = false
                )
            }
        }
    )
}

@Composable
private fun ActionButton(
    config: ActionConfig,
    isConfirmButton: Boolean,
    modifier: Modifier = Modifier
) {
    when (config.style) {
        ActionStyle.TEXT -> {
            TextButton(
                onClick = config.onClick,
                enabled = config.enabled,
                modifier = modifier
            ) {
                Text(config.text)
            }
        }

        ActionStyle.OUTLINED -> {
            OutlinedButton(
                onClick = config.onClick,
                enabled = config.enabled,
                modifier = modifier
            ) {
                Text(config.text)
            }
        }

        ActionStyle.FILLED -> {
            Button(
                onClick = config.onClick,
                enabled = config.enabled,
                modifier = modifier
            ) {
                Text(config.text)
            }
        }

        ActionStyle.DESTRUCTIVE -> {
            Button(
                onClick = config.onClick,
                enabled = config.enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = modifier
            ) {
                Text(config.text)
            }
        }
    }
}

// Convenience builders for common dialog types
object DialogBuilder {

    fun confirmation(
        title: String,
        message: String,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        isDestructive: Boolean = false,
        icon: ImageVector? = null,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ): DialogConfig {
        return DialogConfig(
            type = DialogType.CONFIRMATION,
            title = title,
            message = message,
            icon = icon,
            primaryAction = ActionConfig(
                text = confirmText,
                style = if (isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.FILLED,
                onClick = onConfirm
            ),
            dismissAction = ActionConfig(
                text = cancelText,
                style = ActionStyle.TEXT,
                onClick = onCancel
            )
        )
    }

    fun info(
        title: String,
        message: String,
        buttonText: String = "OK",
        icon: ImageVector? = null,
        onDismiss: () -> Unit
    ): DialogConfig {
        return DialogConfig(
            type = DialogType.INFO,
            title = title,
            message = message,
            icon = icon,
            primaryAction = ActionConfig(
                text = buttonText,
                style = ActionStyle.FILLED,
                onClick = onDismiss
            )
        )
    }

    fun custom(
        title: String,
        message: String? = null,
        primaryText: String,
        secondaryText: String? = null,
        cancelText: String = "Cancel",
        icon: ImageVector? = null,
        content: @Composable (() -> Unit)? = null,
        onPrimary: () -> Unit,
        onSecondary: (() -> Unit)? = null,
        onCancel: () -> Unit
    ): DialogConfig {
        return DialogConfig(
            type = DialogType.CUSTOM,
            title = title,
            message = message,
            icon = icon,
            content = content,
            primaryAction = ActionConfig(
                text = primaryText,
                style = ActionStyle.FILLED,
                onClick = onPrimary
            ),
            secondaryAction = secondaryText?.let { text ->
                ActionConfig(
                    text = text,
                    style = ActionStyle.OUTLINED,
                    onClick = onSecondary ?: {}
                )
            },
            dismissAction = ActionConfig(
                text = cancelText,
                style = ActionStyle.TEXT,
                onClick = onCancel
            )
        )
    }
}