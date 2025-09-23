package com.smilepile.ui.orchestrators

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.smilepile.data.models.Photo
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import com.smilepile.ui.viewmodels.PhotoImportViewModel
import com.smilepile.ui.viewmodels.SearchViewModel
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.utils.PermissionHandler

/**
 * PhotoGalleryOrchestrator - Centralized coordination component for photo gallery functionality.
 *
 * This orchestrator implements the StackMap team's orchestration pattern by:
 * - Coordinating between 4 ViewModels (PhotoGallery, Import, Search, AppMode)
 * - Managing shared state and cross-cutting concerns
 * - Handling mode switching (Kids/Parent) with appropriate UI adaptations
 * - Centralizing event handling to reduce prop drilling
 * - Managing dialogs and complex UI state transitions
 *
 * Key Responsibilities:
 * - Photo selection and batch operations coordination
 * - Permission management for photo imports
 * - Search state synchronization with gallery display
 * - Mode-aware rendering decisions
 * - Dialog state management and orchestration
 * - Event routing between components and ViewModels
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoGalleryOrchestrator(
    onPhotoClick: (Photo, List<Photo>) -> Unit,
    snackbarHostState: SnackbarHostState,
    galleryViewModel: PhotoGalleryViewModel = hiltViewModel(),
    importViewModel: PhotoImportViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    modeViewModel: AppModeViewModel = hiltViewModel(),
    content: @Composable (PhotoGalleryOrchestratorState) -> Unit
) {
    val context = LocalContext.current

    // Collect UI states from all ViewModels
    val galleryState by galleryViewModel.uiState.collectAsState()
    val importState by importViewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()

    // Local dialog states
    var showImportOptions by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var searchBarActive by remember { mutableStateOf(false) }

    // New state for category selection dialog
    var pendingImportUris by remember { mutableStateOf<List<Uri>?>(null) }
    var showCategorySelection by remember { mutableStateOf(false) }

    // Photo picker launchers
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUris = listOf(it)
            showCategorySelection = true
        }
    }

    val multiplePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
            showCategorySelection = true
        }
    }

    // Permission handling
    val storagePermission = rememberPermissionState(PermissionHandler.storagePermission) { isGranted ->
        if (isGranted) {
            multiplePhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            showPermissionDialog = true
        }
    }

    // Handle messages and errors from ViewModels
    LaunchedEffect(importState.successMessage) {
        importState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            importViewModel.clearMessages()
        }
    }

    LaunchedEffect(importState.error) {
        importState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            importViewModel.clearMessages()
        }
    }

    LaunchedEffect(galleryState.error) {
        galleryState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            galleryViewModel.clearError()
        }
    }

    // Create orchestrator state that encapsulates all coordinated state
    val orchestratorState = PhotoGalleryOrchestratorState(
        // Gallery state
        galleryState = galleryState,
        importState = importState,
        searchState = searchState,
        modeState = modeState,

        // Dialog states
        showImportOptions = showImportOptions,
        showPermissionDialog = showPermissionDialog,
        showBatchDeleteDialog = showBatchDeleteDialog,
        showBatchMoveDialog = showBatchMoveDialog,
        showDateRangePicker = showDateRangePicker,
        searchBarActive = searchBarActive,
        showCategorySelection = showCategorySelection,

        // Photo operations
        onPhotoClick = { photo ->
            val photoList = if (searchState.isSearchActive) searchState.searchResults else galleryState.photos
            if (galleryState.isSelectionMode) {
                galleryViewModel.togglePhotoSelection(photo.id)
            } else {
                onPhotoClick(photo, photoList)
            }
        },
        onPhotoLongClick = { photo ->
            if (!galleryState.isSelectionMode) {
                galleryViewModel.enterSelectionMode()
                galleryViewModel.togglePhotoSelection(photo.id)
            }
        },
        onFavoriteToggle = galleryViewModel::toggleFavorite,

        // Selection operations
        onEnterSelectionMode = galleryViewModel::enterSelectionMode,
        onExitSelectionMode = galleryViewModel::exitSelectionMode,
        onSelectAllPhotos = galleryViewModel::selectAllPhotos,
        onDeselectAllPhotos = galleryViewModel::deselectAllPhotos,
        onTogglePhotoSelection = galleryViewModel::togglePhotoSelection,

        // Batch operations
        onDeleteSelectedPhotos = {
            galleryViewModel.removeSelectedPhotosFromLibrary()
            showBatchDeleteDialog = false
        },
        onMoveSelectedPhotos = { categoryId ->
            galleryViewModel.moveSelectedPhotosToCategory(categoryId)
            showBatchMoveDialog = false
        },
        onSetSelectedPhotosFavorite = galleryViewModel::setSelectedPhotosFavorite,

        // Category operations
        onCategorySelected = galleryViewModel::selectCategory,

        // Search operations
        onSearchQueryChange = searchViewModel::updateSearchQuery,
        onSearchHistoryItemClick = searchViewModel::selectSearchHistoryItem,
        onRemoveFromHistory = searchViewModel::removeFromSearchHistory,
        onClearHistory = searchViewModel::clearSearchHistory,
        onToggleFilters = searchViewModel::toggleFilters,
        onClearAllFilters = searchViewModel::clearAllFilters,
        onSetDateRange = searchViewModel::setDateRange,
        onSetFavoritesOnly = searchViewModel::setFavoritesOnly,
        onSetCategoryFilter = searchViewModel::setCategoryFilter,

        // Import operations
        onImportSinglePhoto = {
            showImportOptions = false
            singlePhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onImportMultiplePhotos = {
            showImportOptions = false
            multiplePhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onAddPhotoClick = {
            if (PermissionHandler.isStoragePermissionGranted(context)) {
                multiplePhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                storagePermission.launchPermissionRequest()
            }
        },

        // Mode operations
        onSwitchToKidsMode = modeViewModel::requestModeToggle,
        onValidatePinAndToggle = modeViewModel::validatePinAndToggle,
        onCancelPinAuth = modeViewModel::cancelPinAuth,

        // Dialog operations
        onShowImportOptions = { showImportOptions = it },
        onShowPermissionDialog = { showPermissionDialog = it },
        onShowBatchDeleteDialog = { showBatchDeleteDialog = it },
        onShowBatchMoveDialog = { showBatchMoveDialog = it },
        onShowDateRangePicker = { showDateRangePicker = it },
        onSetSearchBarActive = { searchBarActive = it },
        onShowCategorySelection = { showCategorySelection = it },
        onCategorySelectedForImport = { categoryId ->
            pendingImportUris?.let { uris ->
                if (uris.size == 1) {
                    importViewModel.importPhoto(uris.first(), categoryId)
                } else {
                    importViewModel.importPhotos(uris, categoryId)
                }
                pendingImportUris = null
                showCategorySelection = false
            }
        },

        // Permission operations
        onRequestStoragePermission = { storagePermission.launchPermissionRequest() },
        onOpenAppSettings = { PermissionHandler.openAppSettings(context) }
    )

    // Provide orchestrated state to content
    content(orchestratorState)
}

/**
 * Consolidated state object that encapsulates all coordinated state and operations.
 * This reduces prop drilling by providing a single state object to child components.
 */
data class PhotoGalleryOrchestratorState(
    // ViewModel states
    val galleryState: com.smilepile.ui.viewmodels.PhotoGalleryUiState,
    val importState: com.smilepile.ui.viewmodels.PhotoImportUiState,
    val searchState: com.smilepile.ui.viewmodels.SearchUiState,
    val modeState: com.smilepile.ui.viewmodels.AppModeUiState,

    // Dialog states
    val showImportOptions: Boolean,
    val showPermissionDialog: Boolean,
    val showBatchDeleteDialog: Boolean,
    val showBatchMoveDialog: Boolean,
    val showDateRangePicker: Boolean,
    val searchBarActive: Boolean,
    val showCategorySelection: Boolean,

    // Photo operations
    val onPhotoClick: (Photo) -> Unit,
    val onPhotoLongClick: (Photo) -> Unit,
    val onFavoriteToggle: (Photo) -> Unit,

    // Selection operations
    val onEnterSelectionMode: () -> Unit,
    val onExitSelectionMode: () -> Unit,
    val onSelectAllPhotos: () -> Unit,
    val onDeselectAllPhotos: () -> Unit,
    val onTogglePhotoSelection: (Long) -> Unit,

    // Batch operations
    val onDeleteSelectedPhotos: () -> Unit,
    val onMoveSelectedPhotos: (Long) -> Unit,
    val onSetSelectedPhotosFavorite: (Boolean) -> Unit,

    // Category operations
    val onCategorySelected: (Long?) -> Unit,

    // Search operations
    val onSearchQueryChange: (String) -> Unit,
    val onSearchHistoryItemClick: (String) -> Unit,
    val onRemoveFromHistory: (String) -> Unit,
    val onClearHistory: () -> Unit,
    val onToggleFilters: () -> Unit,
    val onClearAllFilters: () -> Unit,
    val onSetDateRange: (com.smilepile.ui.viewmodels.DateRange?) -> Unit,
    val onSetFavoritesOnly: (Boolean) -> Unit,
    val onSetCategoryFilter: (Long?) -> Unit,

    // Import operations
    val onImportSinglePhoto: () -> Unit,
    val onImportMultiplePhotos: () -> Unit,
    val onAddPhotoClick: () -> Unit,

    // Mode operations
    val onSwitchToKidsMode: () -> Unit,
    val onValidatePinAndToggle: (String) -> Boolean,
    val onCancelPinAuth: () -> Unit,

    // Dialog operations
    val onShowImportOptions: (Boolean) -> Unit,
    val onShowPermissionDialog: (Boolean) -> Unit,
    val onShowBatchDeleteDialog: (Boolean) -> Unit,
    val onShowBatchMoveDialog: (Boolean) -> Unit,
    val onShowDateRangePicker: (Boolean) -> Unit,
    val onSetSearchBarActive: (Boolean) -> Unit,
    val onShowCategorySelection: (Boolean) -> Unit,
    val onCategorySelectedForImport: (Long) -> Unit,

    // Permission operations
    val onRequestStoragePermission: () -> Unit,
    val onOpenAppSettings: () -> Unit
) {
    // Computed properties for mode-aware rendering decisions
    val isKidsMode: Boolean get() = modeState.currentMode == AppMode.KIDS
    val isParentMode: Boolean get() = modeState.currentMode == AppMode.PARENT

    // Computed properties for state coordination
    val displayPhotos: List<Photo> get() = if (searchState.isSearchActive) searchState.searchResults else galleryState.photos
    val showEmptyState: Boolean get() = !searchState.isSearchActive && galleryState.photos.isEmpty() && !galleryState.isLoading
    val showSearchEmptyState: Boolean get() = searchState.showEmptyState
    val showSearchResults: Boolean get() = searchState.showSearchResults
    val isSearching: Boolean get() = searchState.isSearching
    val isLoading: Boolean get() = galleryState.isLoading && !searchState.isSearchActive

    // Batch operation availability
    val canPerformBatchOperations: Boolean get() = galleryState.isSelectionMode && galleryState.hasSelectedPhotos
}