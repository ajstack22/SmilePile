package com.smilepile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.R
import com.smilepile.ui.components.settings.SecuritySetupSection
import com.smilepile.ui.components.settings.ContentControlsSection
import com.smilepile.ui.components.settings.PinSetupDialog
import com.smilepile.ui.components.settings.PatternSetupDialog
import com.smilepile.ui.components.settings.InitialSecuritySetupDialog
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
                SecuritySetupSection(
                    securitySummary = uiState.securitySummary,
                    onPinSetupClick = { showPinChangeDialog = true },
                    onPatternSetupClick = { showPatternChangeDialog = true }
                )
            }

            // Child Safety Settings Section
            item {
                ContentControlsSection(
                    securitySummary = uiState.securitySummary,
                    onKidSafeModeToggle = { viewModel.toggleKidSafeMode() },
                    onDeleteProtectionToggle = { viewModel.toggleDeleteProtection() }
                )
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

