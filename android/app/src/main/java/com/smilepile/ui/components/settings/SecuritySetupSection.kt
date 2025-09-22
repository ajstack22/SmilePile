package com.smilepile.ui.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smilepile.R
import com.smilepile.security.SecuritySummary

/**
 * Security setup section for PIN and pattern management
 */
@Composable
fun SecuritySetupSection(
    securitySummary: SecuritySummary?,
    onPinSetupClick: () -> Unit,
    onPatternSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSection(
        title = stringResource(R.string.setup_security),
        icon = Icons.Default.Security,
        modifier = modifier
    ) {
        securitySummary?.let { summary ->
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
                onClick = onPinSetupClick,
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
                onClick = onPatternSetupClick,
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

            // Security Status Warning
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