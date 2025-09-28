package com.smilepile.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.smilepile.R
import com.smilepile.security.SecuritySummary

/**
 * Content controls section for child safety settings
 */
@Composable
fun ContentControlsSection(
    securitySummary: SecuritySummary?,
    onKidSafeModeToggle: () -> Unit,
    onDeleteProtectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSection(
        title = stringResource(R.string.child_safety_settings),
        modifier = modifier
    ) {
        securitySummary?.let { summary ->
            // Kid-Safe Mode
            SettingsToggleItem(
                title = stringResource(R.string.kid_safe_mode),
                subtitle = stringResource(R.string.kid_safe_mode_subtitle),
                icon = Icons.Default.ChildFriendly,
                checked = summary.kidSafeModeEnabled,
                onCheckedChange = { onKidSafeModeToggle() }
            )

            // Delete Protection
            SettingsToggleItem(
                title = stringResource(R.string.delete_protection),
                subtitle = stringResource(R.string.delete_protection_subtitle),
                icon = Icons.Default.Delete,
                checked = summary.deleteProtectionEnabled,
                onCheckedChange = { onDeleteProtectionToggle() }
            )
        }
    }
}