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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.smilepile.data.models.Photo
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import com.smilepile.ui.viewmodels.PhotoImportViewModel
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.sharing.ShareManager
import com.smilepile.utils.PermissionHandler

/**
 * PhotoGalleryOrchestrator - Centralized coordination component for photo gallery functionality.
 *
 * This orchestrator implements the StackMap team's orchestration pattern by:
 * - Coordinating between 3 ViewModels (PhotoGallery, Import, AppMode)
 * - Managing shared state and cross-cutting concerns
 * - Handling mode switching (Kids/Parent) with appropriate UI adaptations
 * - Centralizing event handling to reduce prop drilling
 * - Managing dialogs and complex UI state transitions
 *
 * Key Responsibilities:
 * - Photo selection and batch operations coordination
 * - Permission management for photo imports
 * - Mode-aware rendering decisions
 * - Dialog state management and orchestration
 * - Event routing between components and ViewModels
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoGalleryOrchestrator(
    onPhotoClick: (Photo, List<Photo>) -> Unit,
    onNavigateToPhotoEditor: (List<String>) -> Unit = {},
    onNavigateToPhotoEditorWithUris: (List<Uri>) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    galleryViewModel: PhotoGalleryViewModel = hiltViewModel(),
    importViewModel: PhotoImportViewModel = hiltViewModel(),
    modeViewModel: AppModeViewModel = hiltViewModel(),
    content: @Composable (PhotoGalleryOrchestratorState) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Collect UI states from all ViewModels
    val galleryState by galleryViewModel.uiState.collectAsState()
    val importState by importViewModel.uiState.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()

    // Create ShareManager instance
    val shareManager = remember { ShareManager(context) }

    // Local dialog states
    var showImportOptions by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }

    // New state for category selection dialog
    var pendingImportUris by remember { mutableStateOf<List<Uri>?>(null) }
    var showCategorySelection by remember { mutableStateOf(false) }
    var isAddingPhotos by remember { mutableStateOf(false) }
    var selectedImportCategoryId by remember { mutableStateOf<Long?>(null) }

    // Photo picker launchers
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUris = listOf(it)
            // Navigate directly to editor with the pre-selected category
            selectedImportCategoryId?.let { categoryId ->
                onNavigateToPhotoEditorWithUris(listOf(uri))
                importViewModel.setPendingCategoryId(categoryId)
                // Reset states
                pendingImportUris = null
                selectedImportCategoryId = null
                isAddingPhotos = false
            }
        }
    }

    val multiplePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50) // Updated to support 50 photos
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
            // Navigate directly to editor with the pre-selected category
            selectedImportCategoryId?.let { categoryId ->
                onNavigateToPhotoEditorWithUris(uris)
                importViewModel.setPendingCategoryId(categoryId)
                // Reset states
                pendingImportUris = null
                selectedImportCategoryId = null
                isAddingPhotos = false
            }
        }
    }

    // Permission handling
    val storagePermission = rememberPermissionState(PermissionHandler.storagePermission) { isGranted ->
        handlePermissionResult(
            isGranted = isGranted,
            selectedCategoryId = selectedImportCategoryId,
            onLaunchPicker = {
                multiplePhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onShowDialog = { showPermissionDialog = true }
        )
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
        modeState = modeState,

        // Dialog states
        showImportOptions = showImportOptions,
        showPermissionDialog = showPermissionDialog,
        showBatchDeleteDialog = showBatchDeleteDialog,
        showBatchMoveDialog = showBatchMoveDialog,
        showCategorySelection = showCategorySelection,
        isAddingPhotos = isAddingPhotos,

        // Photo operations
        onPhotoClick = { photo ->
            handlePhotoClick(
                photo = photo,
                photoList = galleryState.photos,
                isSelectionMode = galleryState.isSelectionMode,
                onToggleSelection = { galleryViewModel.togglePhotoSelection(photo.id) },
                onNavigateToPhoto = { photoToView, list -> onPhotoClick(photoToView, list) }
            )
        },
        onPhotoLongClick = { photo ->
            if (!galleryState.isSelectionMode) {
                galleryViewModel.enterSelectionMode()
                galleryViewModel.togglePhotoSelection(photo.id)
            }
        },

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
        onShareSelectedPhotos = {
            // Get selected photos and share them
            val selectedPhotos = galleryState.photos.filter { photo ->
                galleryState.selectedPhotos.contains(photo.id)
            }
            if (selectedPhotos.isNotEmpty()) {
                shareManager.sharePhotos(context, selectedPhotos)
                galleryViewModel.exitSelectionMode()
            }
        },

        // Category operations
        onCategorySelected = galleryViewModel::selectCategory,


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
            // Prevent rapid taps by checking if flow is already in progress
            if (isAddingPhotos || showCategorySelection) {
                return@PhotoGalleryOrchestratorState
            }

            // Check for empty categories first
            if (galleryState.categories.isEmpty()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Please create a category first")
                }
                return@PhotoGalleryOrchestratorState
            }

            // Show category selection first, then photo picker
            isAddingPhotos = true
            showCategorySelection = true
        },
        onEditSelectedPhotos = {
            // Get selected photos and navigate to editor
            val selectedPhotos = galleryState.photos.filter { photo ->
                galleryState.selectedPhotos.contains(photo.id)
            }
            if (selectedPhotos.isNotEmpty() && selectedPhotos.size <= 5) {
                val photoPaths = selectedPhotos.map { it.path }
                onNavigateToPhotoEditor(photoPaths)
                galleryViewModel.exitSelectionMode()
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
        onShowCategorySelection = { show ->
            showCategorySelection = show
            // Reset add photos flow state when dialog is dismissed
            if (!show) {
                isAddingPhotos = false
                selectedImportCategoryId = null
            }
        },
        onCategorySelectedForImport = { categoryId ->
            handleCategorySelectionForImport(
                categoryId = categoryId,
                isAddingPhotos = isAddingPhotos,
                pendingImportUris = pendingImportUris,
                context = context,
                onStoreCategoryId = { selectedImportCategoryId = categoryId },
                onHideDialog = { showCategorySelection = false },
                onLaunchPicker = {
                    multiplePhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRequestPermission = { storagePermission.launchPermissionRequest() },
                onNavigateWithUris = { uris ->
                    onNavigateToPhotoEditorWithUris(uris)
                    importViewModel.setPendingCategoryId(categoryId)
                    pendingImportUris = null
                }
            )
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
    val modeState: com.smilepile.ui.viewmodels.AppModeUiState,

    // Dialog states
    val showImportOptions: Boolean,
    val showPermissionDialog: Boolean,
    val showBatchDeleteDialog: Boolean,
    val showBatchMoveDialog: Boolean,
    val showCategorySelection: Boolean,
    val isAddingPhotos: Boolean,

    // Photo operations
    val onPhotoClick: (Photo) -> Unit,
    val onPhotoLongClick: (Photo) -> Unit,

    // Selection operations
    val onEnterSelectionMode: () -> Unit,
    val onExitSelectionMode: () -> Unit,
    val onSelectAllPhotos: () -> Unit,
    val onDeselectAllPhotos: () -> Unit,
    val onTogglePhotoSelection: (Long) -> Unit,

    // Batch operations
    val onDeleteSelectedPhotos: () -> Unit,
    val onMoveSelectedPhotos: (Long) -> Unit,
    val onShareSelectedPhotos: () -> Unit,

    // Category operations
    val onCategorySelected: (Long?) -> Unit,


    // Import operations
    val onImportSinglePhoto: () -> Unit,
    val onImportMultiplePhotos: () -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onEditSelectedPhotos: () -> Unit,

    // Mode operations
    val onSwitchToKidsMode: () -> Unit,
    val onValidatePinAndToggle: (String) -> Boolean,
    val onCancelPinAuth: () -> Unit,

    // Dialog operations
    val onShowImportOptions: (Boolean) -> Unit,
    val onShowPermissionDialog: (Boolean) -> Unit,
    val onShowBatchDeleteDialog: (Boolean) -> Unit,
    val onShowBatchMoveDialog: (Boolean) -> Unit,
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
    val displayPhotos: List<Photo> get() = galleryState.photos
    val showEmptyState: Boolean get() = galleryState.photos.isEmpty() && !galleryState.isLoading
    val isLoading: Boolean get() = galleryState.isLoading

    // Batch operation availability
    val canPerformBatchOperations: Boolean get() = galleryState.isSelectionMode && galleryState.hasSelectedPhotos

    companion object {
        // Nuclear option: Store navigation state directly to bypass all navigation complexity
        var navigationPhotoList: List<Photo>? = null
        var navigationPhotoIndex: Int = -1
    }
}

private fun handlePermissionResult(
    isGranted: Boolean,
    selectedCategoryId: Long?,
    onLaunchPicker: () -> Unit,
    onShowDialog: () -> Unit
) {
    if (isGranted && selectedCategoryId != null) {
        onLaunchPicker()
    } else if (!isGranted) {
        onShowDialog()
    }
}

private fun handlePhotoClick(
    photo: Photo,
    photoList: List<Photo>,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onNavigateToPhoto: (Photo, List<Photo>) -> Unit
) {
    if (isSelectionMode) {
        onToggleSelection()
    } else {
        val actualIndex = photoList.indexOfFirst { it.id == photo.id }

        logPhotoNavigation(photo, photoList, actualIndex)

        PhotoGalleryOrchestratorState.navigationPhotoList = photoList
        PhotoGalleryOrchestratorState.navigationPhotoIndex = actualIndex

        onNavigateToPhoto(photo, photoList)
    }
}

private fun logPhotoNavigation(photo: Photo, photoList: List<Photo>, index: Int) {
    android.util.Log.e("SmilePile", "ORCHESTRATOR: Photo clicked - ID: ${photo.id}, Name: ${photo.displayName}")
    android.util.Log.e("SmilePile", "ORCHESTRATOR: Photo list size: ${photoList.size}")
    android.util.Log.e("SmilePile", "ORCHESTRATOR: First 3 in list: ${photoList.take(3).map { "${it.id}:${it.displayName}" }}")
    android.util.Log.e("SmilePile", "ORCHESTRATOR: ACTUAL INDEX CALCULATED HERE: $index")
    android.util.Log.e("SmilePile", "ORCHESTRATOR: STORED INDEX IN COMPANION: $index")
    println("SmilePile ORCHESTRATOR: Clicked ${photo.displayName} - Calculated index: $index of ${photoList.size} photos")
}

private fun handleCategorySelectionForImport(
    categoryId: Long,
    isAddingPhotos: Boolean,
    pendingImportUris: List<Uri>?,
    context: android.content.Context,
    onStoreCategoryId: () -> Unit,
    onHideDialog: () -> Unit,
    onLaunchPicker: () -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateWithUris: (List<Uri>) -> Unit
) {
    if (isAddingPhotos) {
        onStoreCategoryId()
        onHideDialog()

        if (PermissionHandler.isStoragePermissionGranted(context)) {
            onLaunchPicker()
        } else {
            onRequestPermission()
        }
    } else if (pendingImportUris != null) {
        onNavigateWithUris(pendingImportUris)
        onHideDialog()
    }
}