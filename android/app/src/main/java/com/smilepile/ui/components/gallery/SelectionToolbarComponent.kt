package com.smilepile.ui.components.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.AppModeViewModel

/**
 * Selection mode top app bar component for photo gallery.
 *
 * Features:
 * - Displays selected count
 * - Exit selection mode action
 * - Select/deselect all toggle
 * - Mode-aware (hidden in Kids Mode)
 * - Material3 TopAppBar patterns
 *
 * This component handles only UI and delegates operations to callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionToolbarComponent(
    selectedCount: Int,
    isAllSelected: Boolean,
    onExitSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modeViewModel: AppModeViewModel = hiltViewModel()
) {
    val modeUiState by modeViewModel.uiState.collectAsState()

    // Hide in Kids Mode - selection tools are Parent Mode only
    if (modeUiState.currentMode == AppMode.KIDS) {
        return
    }

    SelectionTopAppBar(
        selectedCount = selectedCount,
        isAllSelected = isAllSelected,
        onExitSelectionMode = onExitSelectionMode,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll
    )
}

/**
 * Internal top app bar implementation for selection mode.
 * Follows Material3 TopAppBar patterns with selection-specific styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onExitSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = getSelectionCountText(selectedCount),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            ExitSelectionButton(onClick = onExitSelectionMode)
        },
        actions = {
            SelectAllToggleButton(
                isAllSelected = isAllSelected,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Exit selection mode button with close icon.
 */
@Composable
private fun ExitSelectionButton(
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Exit selection mode"
        )
    }
}

/**
 * Toggle button for select all/deselect all functionality.
 */
@Composable
private fun SelectAllToggleButton(
    isAllSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    IconButton(
        onClick = if (isAllSelected) onDeselectAll else onSelectAll
    ) {
        Icon(
            imageVector = Icons.Default.SelectAll,
            contentDescription = if (isAllSelected) "Deselect all" else "Select all"
        )
    }
}

/**
 * Formats the selection count for display in the toolbar title.
 */
private fun getSelectionCountText(count: Int): String {
    return when (count) {
        0 -> "No items selected"
        1 -> "1 item selected"
        else -> "$count items selected"
    }
}