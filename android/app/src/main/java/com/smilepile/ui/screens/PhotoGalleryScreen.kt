package com.smilepile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
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
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smilepile.R
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.ui.components.SmilePileLogo
import com.smilepile.ui.components.AppHeaderComponent
import com.smilepile.ui.components.CustomFloatingActionButton
import com.smilepile.ui.components.gallery.CategoryFilterComponent
import com.smilepile.ui.components.gallery.PhotoStackComponent
import com.smilepile.ui.components.gallery.SelectionToolbarComponent
import com.smilepile.ui.components.dialogs.UniversalCrudDialog
import com.smilepile.ui.components.dialogs.DialogBuilder
import com.smilepile.ui.orchestrators.PhotoGalleryOrchestrator
import com.smilepile.ui.orchestrators.PhotoGalleryOrchestratorState
import com.smilepile.utils.PermissionRationale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    onPhotoClick: (Photo, List<Photo>) -> Unit,
    onNavigateToPhotoEditor: (List<String>) -> Unit = {},
    onNavigateToPhotoEditorWithUris: (List<android.net.Uri>) -> Unit = {},
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val snackbarHostState = remember { SnackbarHostState() }

    PhotoGalleryOrchestrator(
        onPhotoClick = onPhotoClick,
        onNavigateToPhotoEditor = onNavigateToPhotoEditor,
        onNavigateToPhotoEditorWithUris = onNavigateToPhotoEditorWithUris,
        snackbarHostState = snackbarHostState
    ) { orchestratorState ->

        GalleryScaffold(
            modifier = modifier,
            orchestratorState = orchestratorState,
            snackbarHostState = snackbarHostState,
            paddingValues = paddingValues
        )

        GalleryDialogs(orchestratorState = orchestratorState)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Your photo collection awaits",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start building your SmilePile by adding photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isBatchImport) "Importing Photos" else "Importing Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isBatchImport) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Processing: EXIF extraction, duplicate check, optimization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = "Extracting metadata and optimizing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }

                // Show percentage
                if (isBatchImport) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isBatchImport) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.primary
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
    onShareClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Edit button (only show for 1-5 selected photos)
            if (selectedCount in 1..5) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit selected"
                    )
                }
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share selected"
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

// ParentModeFABs removed - now using single FAB for Add Photos and header eye icon for View Mode

@Composable
private fun CategorySelectionDialog(
    categories: List<Category>,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    UniversalCrudDialog(
        config = DialogBuilder.custom(
            title = "Select Category",
            message = "Choose a category for the imported photos:",
            primaryText = "Select",
            cancelText = "Cancel",
            icon = Icons.Default.PhotoLibrary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScaffold(
    modifier: Modifier,
    orchestratorState: PhotoGalleryOrchestratorState,
    snackbarHostState: SnackbarHostState,
    paddingValues: PaddingValues
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (orchestratorState.galleryState.isSelectionMode) {
                SelectionToolbarComponent(
                    selectedCount = orchestratorState.galleryState.selectedPhotosCount,
                    isAllSelected = orchestratorState.galleryState.isAllPhotosSelected,
                    onExitSelectionMode = orchestratorState.onExitSelectionMode,
                    onSelectAll = orchestratorState.onSelectAllPhotos,
                    onDeselectAll = orchestratorState.onDeselectAllPhotos
                )
            }
        },
        floatingActionButton = {
            CustomFloatingActionButton(
                onClick = {
                    if (!orchestratorState.isAddingPhotos) {
                        orchestratorState.onAddPhotoClick()
                    }
                },
                icon = Icons.Default.Add,
                contentDescription = "Add Photos",
                backgroundColor = Color(0xFF4A90E2),
                isPulsing = true,
                enabled = !orchestratorState.isAddingPhotos,
                modifier = Modifier.padding(end = 16.dp, bottom = 102.dp)
            )
        },
        bottomBar = {
            if (orchestratorState.canPerformBatchOperations) {
                BatchOperationsBottomBar(
                    selectedCount = orchestratorState.galleryState.selectedPhotosCount,
                    onDeleteClick = { orchestratorState.onShowBatchDeleteDialog(true) },
                    onMoveClick = { orchestratorState.onShowBatchMoveDialog(true) },
                    onShareClick = orchestratorState.onShareSelectedPhotos,
                    onEditClick = { orchestratorState.onEditSelectedPhotos() }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        GalleryContent(
            orchestratorState = orchestratorState,
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun GalleryContent(
    orchestratorState: PhotoGalleryOrchestratorState,
    paddingValues: PaddingValues
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!orchestratorState.galleryState.isSelectionMode) {
            AppHeaderComponent(
                onViewModeClick = orchestratorState.onSwitchToKidsMode,
                showViewModeButton = true
            ) {
                CategoryFilterComponent(
                    categories = orchestratorState.galleryState.categories,
                    selectedCategoryId = orchestratorState.galleryState.selectedCategoryId,
                    onCategorySelected = orchestratorState.onCategorySelected,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
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

            PhotoGridWithGestures(orchestratorState = orchestratorState)
        }
    }
}

@Composable
private fun PhotoGridWithGestures(
    orchestratorState: PhotoGalleryOrchestratorState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(
                orchestratorState.galleryState.categories,
                orchestratorState.galleryState.selectedCategoryId
            ) {
                handleCategorySwipeGestures(
                    categories = orchestratorState.galleryState.categories,
                    selectedCategoryId = orchestratorState.galleryState.selectedCategoryId,
                    onCategorySelected = orchestratorState.onCategorySelected
                )
            }
    ) {
        PhotoGridContent(orchestratorState = orchestratorState)
    }
}

private suspend fun PointerInputScope.handleCategorySwipeGestures(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit
) {
    val swipeThreshold = 150f
    var totalDrag = 0f

    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            if (categories.isEmpty()) return@detectHorizontalDragGestures

            val currentIndex = categories.indexOfFirst { it.id == selectedCategoryId }.takeIf { it >= 0 } ?: 0

            when {
                totalDrag < -swipeThreshold && currentIndex < categories.size - 1 -> {
                    onCategorySelected(categories[currentIndex + 1].id)
                }
                totalDrag > swipeThreshold && currentIndex > 0 -> {
                    onCategorySelected(categories[currentIndex - 1].id)
                }
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
    )
}

@Composable
private fun PhotoGridContent(
    orchestratorState: PhotoGalleryOrchestratorState
) {
    when {
        orchestratorState.galleryState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        orchestratorState.galleryState.photos.isEmpty() -> {
            EmptyState(
                onImportClick = orchestratorState.onAddPhotoClick,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            println("DEBUG: PhotoGrid displaying ${orchestratorState.galleryState.photos.size} photos")
            println("DEBUG: Selected Category: ${orchestratorState.galleryState.selectedCategoryId}")
            println("DEBUG: First 3 in grid: ${orchestratorState.galleryState.photos.take(3).map { photo -> "${photo.id}:${photo.displayName}" }}")
            PhotoStackComponent(
                photos = orchestratorState.galleryState.photos,
                selectedPhotos = orchestratorState.galleryState.selectedPhotos,
                isSelectionMode = orchestratorState.galleryState.isSelectionMode,
                showEditActions = false,
                onPhotoClick = orchestratorState.onPhotoClick,
                onPhotoLongClick = orchestratorState.onPhotoLongClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GalleryDialogs(
    orchestratorState: PhotoGalleryOrchestratorState
) {
    if (orchestratorState.showCategorySelection) {
        CategorySelectionDialog(
            categories = orchestratorState.galleryState.categories,
            onCategorySelected = orchestratorState.onCategorySelectedForImport,
            onDismiss = { orchestratorState.onShowCategorySelection(false) }
        )
    }

    if (orchestratorState.showPermissionDialog) {
        PermissionDialog(orchestratorState = orchestratorState)
    }

    if (orchestratorState.showBatchDeleteDialog) {
        BatchDeleteDialog(orchestratorState = orchestratorState)
    }

    if (orchestratorState.showBatchMoveDialog) {
        BatchMoveToCategoryDialog(
            categories = orchestratorState.galleryState.categories,
            selectedCount = orchestratorState.galleryState.selectedPhotosCount,
            onCategorySelected = orchestratorState.onMoveSelectedPhotos,
            onDismiss = { orchestratorState.onShowBatchMoveDialog(false) }
        )
    }
}

@Composable
private fun PermissionDialog(
    orchestratorState: PhotoGalleryOrchestratorState
) {
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

@Composable
private fun BatchDeleteDialog(
    orchestratorState: PhotoGalleryOrchestratorState
) {
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
