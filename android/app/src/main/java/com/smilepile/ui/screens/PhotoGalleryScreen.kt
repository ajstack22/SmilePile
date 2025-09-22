package com.smilepile.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smilepile.R
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.ui.components.SearchBar
import com.smilepile.ui.components.SearchFiltersRow
import com.smilepile.ui.components.SearchResultsHeader
import com.smilepile.ui.components.EmptySearchState
import com.smilepile.ui.components.DateRangePickerDialog
import com.smilepile.ui.components.gallery.CategoryFilterComponent
import com.smilepile.ui.components.gallery.PhotoGridComponent
import com.smilepile.ui.components.gallery.SelectionToolbarComponent
import com.smilepile.ui.components.dialogs.UniversalCrudDialog
import com.smilepile.ui.components.dialogs.DialogBuilder
import com.smilepile.ui.orchestrators.PhotoGalleryOrchestrator
import com.smilepile.utils.PermissionRationale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    onPhotoClick: (Photo, List<Photo>) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    PhotoGalleryOrchestrator(
        onPhotoClick = onPhotoClick,
        snackbarHostState = snackbarHostState
    ) { orchestratorState ->

        Scaffold(
            modifier = modifier,
            topBar = {
                if (orchestratorState.galleryState.isSelectionMode) {
                    SelectionToolbarComponent(
                        selectedCount = orchestratorState.galleryState.selectedPhotosCount,
                        isAllSelected = orchestratorState.galleryState.isAllPhotosSelected,
                        onExitSelectionMode = orchestratorState.onExitSelectionMode,
                        onSelectAll = orchestratorState.onSelectAllPhotos,
                        onDeselectAll = orchestratorState.onDeselectAllPhotos
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.photo_gallery),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text("Edit Mode")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            floatingActionButton = {
                ParentModeFABs(
                    onAddPhotoClick = orchestratorState.onAddPhotoClick,
                    onSwitchToKidsMode = orchestratorState.onSwitchToKidsMode
                )
            },
            bottomBar = {
                if (orchestratorState.canPerformBatchOperations) {
                    BatchOperationsBottomBar(
                        selectedCount = orchestratorState.galleryState.selectedPhotosCount,
                        onDeleteClick = { orchestratorState.onShowBatchDeleteDialog(true) },
                        onMoveClick = { orchestratorState.onShowBatchMoveDialog(true) },
                        onFavoriteClick = { orchestratorState.onSetSelectedPhotosFavorite(true) },
                        onUnfavoriteClick = { orchestratorState.onSetSelectedPhotosFavorite(false) },
                        onShareClick = {
                            // TODO: Implement batch share functionality
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                SearchBar(
                    searchQuery = orchestratorState.searchState.searchQuery,
                    onSearchQueryChange = orchestratorState.onSearchQueryChange,
                    searchHistory = orchestratorState.searchState.searchHistory,
                    onSearchHistoryItemClick = orchestratorState.onSearchHistoryItemClick,
                    onRemoveFromHistory = orchestratorState.onRemoveFromHistory,
                    onClearHistory = orchestratorState.onClearHistory,
                    active = orchestratorState.searchBarActive,
                    onActiveChange = { active ->
                        orchestratorState.onSetSearchBarActive(active)
                        if (!active) {
                            orchestratorState.onToggleFilters()
                        }
                    },
                    onSearch = { orchestratorState.onSetSearchBarActive(false) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Search filters
                SearchFiltersRow(
                    selectedDateRange = orchestratorState.searchState.selectedDateRange,
                    onDateRangeClick = { orchestratorState.onShowDateRangePicker(true) },
                    favoritesOnly = orchestratorState.searchState.favoritesOnly,
                    onFavoritesToggle = { orchestratorState.onSetFavoritesOnly(!orchestratorState.searchState.favoritesOnly) },
                    selectedCategoryId = orchestratorState.searchState.selectedCategoryId,
                    categories = orchestratorState.searchState.categories,
                    onCategorySelect = orchestratorState.onSetCategoryFilter,
                    showFilters = orchestratorState.searchState.showFilters,
                    onToggleFilters = orchestratorState.onToggleFilters,
                    onClearAllFilters = orchestratorState.onClearAllFilters
                )

                // Import progress indicator
                if (orchestratorState.importState.isImporting) {
                    ImportProgressIndicator(
                        progress = orchestratorState.importState.importProgress,
                        isBatchImport = orchestratorState.importState.isBatchImport,
                        progressText = if (orchestratorState.importState.isBatchImport) {
                            orchestratorState.importState.batchProgressText
                        } else {
                            "Importing photo..."
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Category filter chips (only show when not searching)
                if (!orchestratorState.searchState.isSearchActive) {
                    CategoryFilterComponent(
                        categories = orchestratorState.galleryState.categories,
                        selectedCategoryId = orchestratorState.galleryState.selectedCategoryId,
                        onCategorySelected = orchestratorState.onCategorySelected
                    )
                }

                // Photos grid - show search results when searching, otherwise show regular gallery
                when {
                    orchestratorState.isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    orchestratorState.showSearchEmptyState -> {
                        EmptySearchState(
                            searchQuery = orchestratorState.searchState.searchQuery,
                            hasFilters = orchestratorState.searchState.hasFilters,
                            onClearFilters = orchestratorState.onClearAllFilters,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        )
                    }
                    orchestratorState.showSearchResults -> {
                        Column {
                            // Search results header
                            SearchResultsHeader(
                                resultsCount = orchestratorState.searchState.resultsCount,
                                searchQuery = orchestratorState.searchState.searchQuery,
                                hasFilters = orchestratorState.searchState.hasFilters
                            )

                            // Search results grid
                            PhotoGridComponent(
                                photos = orchestratorState.searchState.searchResults,
                                selectedPhotos = orchestratorState.galleryState.selectedPhotos,
                                isSelectionMode = orchestratorState.galleryState.isSelectionMode,
                                onPhotoClick = orchestratorState.onPhotoClick,
                                onPhotoLongClick = orchestratorState.onPhotoLongClick,
                                onFavoriteToggle = orchestratorState.onFavoriteToggle,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    orchestratorState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    orchestratorState.showEmptyState -> {
                        EmptyState(
                            onImportClick = orchestratorState.onAddPhotoClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        PhotoGridComponent(
                            photos = orchestratorState.galleryState.photos,
                            selectedPhotos = orchestratorState.galleryState.selectedPhotos,
                            isSelectionMode = orchestratorState.galleryState.isSelectionMode,
                            onPhotoClick = orchestratorState.onPhotoClick,
                            onPhotoLongClick = orchestratorState.onPhotoLongClick,
                            onFavoriteToggle = orchestratorState.onFavoriteToggle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Import options dialog
            if (orchestratorState.showImportOptions) {
                UniversalCrudDialog(
                    config = DialogBuilder.custom(
                        title = "Add Photos",
                        message = "Choose how you'd like to add photos from your gallery:",
                        primaryText = "Import Multiple",
                        secondaryText = "Import One",
                        cancelText = "Cancel",
                        icon = Icons.Default.Add,
                        onPrimary = orchestratorState.onImportMultiplePhotos,
                        onSecondary = orchestratorState.onImportSinglePhoto,
                        onCancel = { orchestratorState.onShowImportOptions(false) }
                    ),
                    onDismiss = { orchestratorState.onShowImportOptions(false) }
                )
            }

            // Permission rationale dialog
            if (orchestratorState.showPermissionDialog) {
                UniversalCrudDialog(
                    config = DialogBuilder.custom(
                        title = PermissionRationale.STORAGE_PERMISSION_TITLE,
                        message = PermissionRationale.STORAGE_PERMISSION_MESSAGE,
                        primaryText = PermissionRationale.GRANT_BUTTON_TEXT,
                        secondaryText = "Settings",
                        cancelText = PermissionRationale.CANCEL_BUTTON_TEXT,
                        icon = Icons.Default.Warning,
                        onPrimary = {
                            orchestratorState.onShowPermissionDialog(false)
                            orchestratorState.onRequestStoragePermission()
                        },
                        onSecondary = {
                            orchestratorState.onShowPermissionDialog(false)
                            orchestratorState.onOpenAppSettings()
                        },
                        onCancel = { orchestratorState.onShowPermissionDialog(false) }
                    ),
                    onDismiss = { orchestratorState.onShowPermissionDialog(false) }
                )
            }

            // Batch delete confirmation dialog
            if (orchestratorState.showBatchDeleteDialog) {
                UniversalCrudDialog(
                    config = DialogBuilder.confirmation(
                        title = "Remove Photos from Library",
                        message = "Are you sure you want to remove ${orchestratorState.galleryState.selectedPhotosCount} selected photos from your SmilePile library? The photos will remain on your device but won't appear in the app.",
                        confirmText = "Remove",
                        cancelText = "Cancel",
                        isDestructive = false,
                        icon = Icons.Default.RemoveCircleOutline,
                        onConfirm = orchestratorState.onDeleteSelectedPhotos,
                        onCancel = { orchestratorState.onShowBatchDeleteDialog(false) }
                    ),
                    onDismiss = { orchestratorState.onShowBatchDeleteDialog(false) }
                )
            }

            // Batch move dialog
            if (orchestratorState.showBatchMoveDialog) {
                BatchMoveToCategoryDialog(
                    categories = orchestratorState.galleryState.categories,
                    selectedCount = orchestratorState.galleryState.selectedPhotosCount,
                    onCategorySelected = orchestratorState.onMoveSelectedPhotos,
                    onDismiss = { orchestratorState.onShowBatchMoveDialog(false) }
                )
            }

            // Date range picker dialog
            if (orchestratorState.showDateRangePicker) {
                DateRangePickerDialog(
                    currentDateRange = orchestratorState.searchState.selectedDateRange,
                    onDateRangeSelected = { dateRange ->
                        orchestratorState.onSetDateRange(dateRange)
                        orchestratorState.onShowDateRangePicker(false)
                    },
                    onDismiss = { orchestratorState.onShowDateRangePicker(false) }
                )
            }
        }
    }
}



@Composable
private fun EmptyState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No photos yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Import photos from your gallery to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onImportClick
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Import Photos",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportProgressIndicator(
    progress: Float,
    isBatchImport: Boolean,
    progressText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isBatchImport) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isBatchImport) "Importing Photos" else "Importing Photo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isBatchImport) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (isBatchImport) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BatchOperationsBottomBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit,
    onMoveClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onUnfavoriteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share selected"
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Add to favorites"
                )
            }

            IconButton(onClick = onUnfavoriteClick) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Remove from favorites"
                )
            }

            IconButton(onClick = onMoveClick) {
                Icon(
                    imageVector = Icons.Default.DriveFileMove,
                    contentDescription = "Move to category"
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from Library",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BatchMoveToCategoryDialog(
    categories: List<Category>,
    selectedCount: Int,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    UniversalCrudDialog(
        config = DialogBuilder.custom(
            title = "Move Photos",
            message = "Move $selectedCount selected photos to:",
            primaryText = "Move",
            cancelText = "Cancel",
            icon = Icons.Default.DriveFileMove,
            content = {
                Spacer(modifier = Modifier.height(16.dp))
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategoryId = category.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            onPrimary = {
                selectedCategoryId?.let { onCategorySelected(it) }
            },
            onCancel = onDismiss
        ),
        onDismiss = onDismiss
    )
}

/**
 * Dual FAB system for Parent Mode
 * Primary: Switch to SmilePile (Kids Mode)
 * Secondary: Add Photos
 */
@Composable
private fun ParentModeFABs(
    onAddPhotoClick: () -> Unit,
    onSwitchToKidsMode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Secondary FAB - Add Photos
        SmallFloatingActionButton(
            onClick = onAddPhotoClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Photos"
            )
        }

        // Primary FAB - Switch to SmilePile (Kids Mode)
        ExtendedFloatingActionButton(
            onClick = onSwitchToKidsMode,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Switch to SmilePile"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("SmilePile")
        }
    }
}
