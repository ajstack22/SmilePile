package com.smilepile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.smilepile.R
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import com.smilepile.ui.viewmodels.PhotoImportViewModel
import com.smilepile.ui.viewmodels.SearchViewModel
import com.smilepile.ui.components.SearchBar
import com.smilepile.ui.components.SearchFiltersRow
import com.smilepile.ui.components.SearchResultsHeader
import com.smilepile.ui.components.EmptySearchState
import com.smilepile.ui.components.DateRangePickerDialog
import com.smilepile.utils.PermissionHandler
import com.smilepile.utils.PermissionRationale
import com.smilepile.utils.StoragePermissionState
import com.smilepile.utils.rememberStoragePermissionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoGalleryScreen(
    onPhotoClick: (Photo, List<Photo>) -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoGalleryViewModel = hiltViewModel(),
    importViewModel: PhotoImportViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val importUiState by importViewModel.uiState.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Permission and import state
    var showImportOptions by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionState by remember { mutableStateOf(StoragePermissionState()) }

    // Batch operation dialogs
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }

    // Search state
    var searchBarActive by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Storage permission handling
    val storagePermission = rememberPermissionState(PermissionHandler.storagePermission) { isGranted ->
        if (isGranted) {
            showImportOptions = true
        } else {
            showPermissionDialog = true
        }
    }

    // Photo picker launchers
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            importViewModel.importPhoto(it, uiState.selectedCategoryId)
        }
    }

    val multiplePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            importViewModel.importPhotos(uris, uiState.selectedCategoryId)
        }
    }

    // Handle import results
    LaunchedEffect(importUiState.successMessage) {
        importUiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            importViewModel.clearMessages()
        }
    }

    LaunchedEffect(importUiState.error) {
        importUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            importViewModel.clearMessages()
        }
    }

    // Show gallery error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = uiState.selectedPhotosCount,
                    isAllSelected = uiState.isAllPhotosSelected,
                    onExitSelectionMode = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAllPhotos() },
                    onDeselectAll = { viewModel.deselectAllPhotos() }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.photo_gallery),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (PermissionHandler.isStoragePermissionGranted(context)) {
                        showImportOptions = true
                    } else {
                        storagePermission.launchPermissionRequest()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Import Photos"
                )
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.hasSelectedPhotos) {
                BatchOperationsBottomBar(
                    selectedCount = uiState.selectedPhotosCount,
                    onDeleteClick = { showBatchDeleteDialog = true },
                    onMoveClick = { showBatchMoveDialog = true },
                    onFavoriteClick = { viewModel.setSelectedPhotosFavorite(true) },
                    onUnfavoriteClick = { viewModel.setSelectedPhotosFavorite(false) },
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
                searchQuery = searchUiState.searchQuery,
                onSearchQueryChange = searchViewModel::updateSearchQuery,
                searchHistory = searchUiState.searchHistory,
                onSearchHistoryItemClick = searchViewModel::selectSearchHistoryItem,
                onRemoveFromHistory = searchViewModel::removeFromSearchHistory,
                onClearHistory = searchViewModel::clearSearchHistory,
                active = searchBarActive,
                onActiveChange = {
                    searchBarActive = it
                    if (!it) {
                        searchViewModel.hideFilters()
                    }
                },
                onSearch = { searchBarActive = false },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search filters
            SearchFiltersRow(
                selectedDateRange = searchUiState.selectedDateRange,
                onDateRangeClick = { showDateRangePicker = true },
                favoritesOnly = searchUiState.favoritesOnly,
                onFavoritesToggle = { searchViewModel.setFavoritesOnly(!searchUiState.favoritesOnly) },
                selectedCategoryId = searchUiState.selectedCategoryId,
                categories = searchUiState.categories,
                onCategorySelect = searchViewModel::setCategoryFilter,
                showFilters = searchUiState.showFilters,
                onToggleFilters = searchViewModel::toggleFilters,
                onClearAllFilters = searchViewModel::clearAllFilters
            )

            // Import progress indicator
            if (importUiState.isImporting) {
                ImportProgressIndicator(
                    progress = importUiState.importProgress,
                    isBatchImport = importUiState.isBatchImport,
                    progressText = if (importUiState.isBatchImport) {
                        importUiState.batchProgressText
                    } else {
                        "Importing photo..."
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Category filter chips (only show when not searching)
            if (!searchUiState.isSearchActive) {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = viewModel::selectCategory,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Photos grid - show search results when searching, otherwise show regular gallery
            when {
                searchUiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                searchUiState.showEmptyState -> {
                    EmptySearchState(
                        searchQuery = searchUiState.searchQuery,
                        hasFilters = searchUiState.hasFilters,
                        onClearFilters = searchViewModel::clearAllFilters,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    )
                }
                searchUiState.showSearchResults -> {
                    Column {
                        // Search results header
                        SearchResultsHeader(
                            resultsCount = searchUiState.resultsCount,
                            searchQuery = searchUiState.searchQuery,
                            hasFilters = searchUiState.hasFilters
                        )

                        // Search results grid
                        PhotoGrid(
                            photos = searchUiState.searchResults,
                            selectedPhotos = uiState.selectedPhotos,
                            isSelectionMode = uiState.isSelectionMode,
                            onPhotoClick = { photo ->
                                if (uiState.isSelectionMode) {
                                    viewModel.togglePhotoSelection(photo.id)
                                } else {
                                    onPhotoClick(photo, searchUiState.searchResults)
                                }
                            },
                            onPhotoLongClick = { photo ->
                                if (!uiState.isSelectionMode) {
                                    viewModel.enterSelectionMode()
                                    viewModel.togglePhotoSelection(photo.id)
                                }
                            },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.photos.isEmpty() -> {
                    EmptyState(
                        onImportClick = {
                            if (PermissionHandler.isStoragePermissionGranted(context)) {
                                showImportOptions = true
                            } else {
                                storagePermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    PhotoGrid(
                        photos = uiState.photos,
                        selectedPhotos = uiState.selectedPhotos,
                        isSelectionMode = uiState.isSelectionMode,
                        onPhotoClick = { photo ->
                            if (uiState.isSelectionMode) {
                                viewModel.togglePhotoSelection(photo.id)
                            } else {
                                onPhotoClick(photo, uiState.photos)
                            }
                        },
                        onPhotoLongClick = { photo ->
                            if (!uiState.isSelectionMode) {
                                viewModel.enterSelectionMode()
                                viewModel.togglePhotoSelection(photo.id)
                            }
                        },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Import options dropdown
        if (showImportOptions) {
            ImportOptionsDialog(
                onDismiss = { showImportOptions = false },
                onSinglePhotoClick = {
                    showImportOptions = false
                    singlePhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onMultiplePhotosClick = {
                    showImportOptions = false
                    multiplePhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
        }

        // Permission rationale dialog
        if (showPermissionDialog) {
            PermissionRationaleDialog(
                onDismiss = { showPermissionDialog = false },
                onGrantClick = {
                    showPermissionDialog = false
                    storagePermission.launchPermissionRequest()
                },
                onSettingsClick = {
                    showPermissionDialog = false
                    PermissionHandler.openAppSettings(context)
                }
            )
        }

        // Batch delete confirmation dialog
        if (showBatchDeleteDialog) {
            BatchDeleteConfirmationDialog(
                selectedCount = uiState.selectedPhotosCount,
                onConfirm = {
                    viewModel.deleteSelectedPhotos()
                    showBatchDeleteDialog = false
                },
                onDismiss = { showBatchDeleteDialog = false }
            )
        }

        // Batch move dialog
        if (showBatchMoveDialog) {
            BatchMoveToCategoryDialog(
                categories = uiState.categories,
                selectedCount = uiState.selectedPhotosCount,
                onCategorySelected = { categoryId ->
                    viewModel.moveSelectedPhotosToCategory(categoryId)
                    showBatchMoveDialog = false
                },
                onDismiss = { showBatchMoveDialog = false }
            )
        }

        // Date range picker dialog
        if (showDateRangePicker) {
            DateRangePickerDialog(
                currentDateRange = searchUiState.selectedDateRange,
                onDateRangeSelected = { dateRange ->
                    searchViewModel.setDateRange(dateRange)
                    showDateRangePicker = false
                },
                onDismiss = { showDateRangePicker = false }
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        // "All" filter chip
        item {
            FilterChip(
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                selected = selectedCategoryId == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        // Category filter chips
        items(categories) { category ->
            FilterChip(
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.displayName) },
                selected = selectedCategoryId == category.id,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotos: Set<Long>,
    isSelectionMode: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onFavoriteToggle: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.id),
                isSelectionMode = isSelectionMode,
                onPhotoClick = { onPhotoClick(photo) },
                onPhotoLongClick = { onPhotoLongClick(photo) },
                onFavoriteToggle = { onFavoriteToggle(photo) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onPhotoClick,
                onLongClick = onPhotoLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (photo.isFromAssets) {
                            "file:///android_asset/${photo.path}"
                        } else {
                            File(photo.path)
                        }
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Selection checkbox overlay (when in selection mode)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onPhotoClick() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                )
            } else {
                // Favorite button overlay
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (photo.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = if (photo.isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (photo.isFavorite) {
                            Color.Red
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Selection overlay
            if (isSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
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
private fun ImportOptionsDialog(
    onDismiss: () -> Unit,
    onSinglePhotoClick: () -> Unit,
    onMultiplePhotosClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photos") },
        text = {
            Column {
                Text("Choose how you'd like to add photos from your gallery:")
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onSinglePhotoClick) {
                    Text("Import One")
                }
                TextButton(onClick = onMultiplePhotosClick) {
                    Text("Import Multiple")
                }
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
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(PermissionRationale.STORAGE_PERMISSION_TITLE) },
        text = { Text(PermissionRationale.STORAGE_PERMISSION_MESSAGE) },
        confirmButton = {
            TextButton(onClick = onGrantClick) {
                Text(PermissionRationale.GRANT_BUTTON_TEXT)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(PermissionRationale.CANCEL_BUTTON_TEXT)
            }
        }
    )
}

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
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onExitSelectionMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit selection mode"
                )
            }
        },
        actions = {
            IconButton(
                onClick = if (isAllSelected) onDeselectAll else onSelectAll
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = if (isAllSelected) "Deselect all" else "Select all"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
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
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BatchDeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Photos") },
        text = {
            Text("Are you sure you want to delete $selectedCount selected photos? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onError)
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
private fun BatchMoveToCategoryDialog(
    categories: List<Category>,
    selectedCount: Int,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Photos") },
        text = {
            Column {
                Text("Move $selectedCount selected photos to:")
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategoryId?.let { onCategorySelected(it) }
                },
                enabled = selectedCategoryId != null
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}