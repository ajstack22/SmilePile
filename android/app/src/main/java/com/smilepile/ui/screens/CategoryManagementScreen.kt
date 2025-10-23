package com.smilepile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smilepile.data.models.Category
import com.smilepile.managers.CategoryManager
import com.smilepile.ui.components.AppHeaderComponent
import com.smilepile.ui.components.CustomFloatingActionButton
import com.smilepile.ui.viewmodels.CategoryViewModel

/**
 * Full screen for comprehensive category management
 * Supports CRUD operations, reordering, and batch operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToKidsMode: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    demoState: com.smilepile.ui.viewmodels.DemoModeUiState = com.smilepile.ui.viewmodels.DemoModeUiState(),
    onShowExitDemoDialog: () -> Unit = {}
) {
    val categories by viewModel.categoriesWithCountsFlow.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }


    Box(modifier = Modifier.fillMaxSize()) {
        // Main scaffold
        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Empty bottom bar to match PhotoGalleryScreen structure
        },
        contentWindowInsets = WindowInsets(0.dp) // Same as PhotoGalleryScreen
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Add spacing for header
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp) // Height for header
            )
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (categories.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No piles yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Text("Create First Pile")
                        }
                    }
                }
            } else {
                // Category list with drag-to-reorder
                var draggedItem by remember { mutableStateOf<com.smilepile.ui.viewmodels.CategoryWithCount?>(null) }
                var draggedOverItem by remember { mutableStateOf<com.smilepile.ui.viewmodels.CategoryWithCount?>(null) }
                var dragOffset by remember { mutableStateOf(0f) }
                val listState = rememberLazyListState()
                val density = LocalDensity.current
                val itemHeightPx = with(density) { 88.dp.toPx() }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = categories,
                        key = { it.category.id }
                    ) { categoryWithCount ->
                        val isDragging = draggedItem?.category?.id == categoryWithCount.category.id
                        val isDraggedOver = draggedOverItem?.category?.id == categoryWithCount.category.id

                        CategoryManagementItem(
                            categoryWithCount = categoryWithCount,
                            onEdit = {
                                editingCategory = categoryWithCount.category
                                showAddDialog = true
                            },
                            onDelete = {
                                showDeleteDialog = categoryWithCount.category
                            },
                            isDragging = isDragging,
                            isDraggedOver = isDraggedOver,
                            onDragStart = {
                                draggedItem = categoryWithCount
                                dragOffset = 0f
                            },
                            onDrag = { delta ->
                                if (draggedItem != null) {
                                    dragOffset += delta.y

                                    // Calculate which item we're hovering over based on drag offset
                                    val draggedIndex = categories.indexOfFirst { it.category.id == draggedItem!!.category.id }
                                    if (draggedIndex != -1) {
                                        // Each item is approximately 88dp (card height + spacing)
                                        val offsetInItems = (dragOffset / itemHeightPx).toInt()
                                        val targetIndex = (draggedIndex + offsetInItems).coerceIn(0, categories.size - 1)

                                        if (targetIndex != draggedIndex) {
                                            draggedOverItem = categories[targetIndex]
                                        } else {
                                            draggedOverItem = null
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggedItem != null && draggedOverItem != null && draggedItem != draggedOverItem) {
                                    val fromIndex = categories.indexOfFirst { it.category.id == draggedItem!!.category.id }
                                    val toIndex = categories.indexOfFirst { it.category.id == draggedOverItem!!.category.id }

                                    if (fromIndex != -1 && toIndex != -1) {
                                        val reordered = categories.toMutableList()
                                        val item = reordered.removeAt(fromIndex)
                                        reordered.add(toIndex, item)
                                        viewModel.reorderCategories(reordered.map { it.category })
                                    }
                                }
                                draggedItem = null
                                draggedOverItem = null
                                dragOffset = 0f
                            }
                        )
                    }
                }
            }
        }
    }

    // Error handling
    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Add/Edit dialog
    if (showAddDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = {
                showAddDialog = false
                editingCategory = null
            },
            onSave = { displayName, colorHex, iconName ->
                if (editingCategory != null) {
                    viewModel.updateCategory(
                        editingCategory!!,
                        displayName,
                        colorHex
                    )
                } else {
                    viewModel.addCategory(displayName, colorHex)
                }
                showAddDialog = false
                editingCategory = null
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { category ->
        DeleteCategoryDialog(
            category = category,
            viewModel = viewModel,
            onDismiss = { showDeleteDialog = null },
            onConfirm = { deletePhotos ->
                viewModel.deleteCategory(category, deletePhotos)
                showDeleteDialog = null
            }
        )
    }

        // Add Pile FAB overlay (bottom-right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 102.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            CustomFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = Icons.Default.Add,
                contentDescription = "Add Pile",
                backgroundColor = Color(0xFFFF6600),
                isPulsing = true
            )
        }

        // Demo exit FAB overlay (bottom-left)
        if (demoState.isDemoMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, bottom = 102.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                CustomFloatingActionButton(
                    onClick = onShowExitDemoDialog,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit Demo Mode",
                    backgroundColor = Color(0xFF9C27B0),
                    isPulsing = false,
                    enabled = !demoState.isExiting
                )
            }
        }

        // Custom header overlay that extends into status bar
        AppHeaderComponent(
            onViewModeClick = onNavigateToKidsMode,
            showViewModeButton = true,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    } // End of Box
}

@Composable
fun CategoryManagementItem(
    categoryWithCount: com.smilepile.ui.viewmodels.CategoryWithCount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    isDraggedOver: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val category = categoryWithCount.category
    val photoCount = categoryWithCount.photoCount

    val categoryColor = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex ?: "#808080"))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = if (isDragging) 0.5f else 1f,
                scaleX = if (isDragging) 1.05f else 1f,
                scaleY = if (isDragging) 1.05f else 1f
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { _, dragAmount -> onDrag(dragAmount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDraggedOver)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Category color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Category info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "$photoCount photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Action buttons
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit pile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete pile",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(category?.displayName ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: CategoryManager.CATEGORY_COLORS.random()) }
    var selectedIcon by remember { mutableStateOf(category?.iconResource) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Top bar with title and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (category != null) "Edit Pile" else "Add Pile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        if (displayName.isNotBlank()) {
                            onSave(displayName, selectedColor, selectedIcon)
                        }
                    },
                    enabled = displayName.isNotBlank()
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name input
            Text(
                text = "Pile Name",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = { Text("Enter pile name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color selection
            Text(
                text = "Pile Color",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(100.dp) // 2 rows of colors
            ) {
                items(CategoryManager.CATEGORY_COLORS) { color ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .border(
                                width = if (color == selectedColor) 2.dp else 1.dp,
                                color = if (color == selectedColor) {
                                    MaterialTheme.colorScheme.primary
                                } else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == selectedColor) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live preview
            Text(
                text = "Preview",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (displayName.isNotBlank()) {
                com.smilepile.ui.components.gallery.CategoryChip(
                    category = Category(
                        id = 0,
                        name = displayName.lowercase().replace(" ", "_"),
                        displayName = displayName,
                        colorHex = selectedColor,
                        position = 0,
                        isDefault = false
                    ),
                    isSelected = true,
                    onClick = {}
                )
            } else {
                Text(
                    text = "Enter pile name to see preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeleteCategoryDialog(
    category: Category,
    viewModel: CategoryViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var deletePhotos by remember { mutableStateOf(false) }
    val (canDelete, reason) = viewModel.canDeleteCategory(category)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Pile") },
        text = {
            DeleteDialogContent(
                category = category,
                canDelete = canDelete,
                reason = reason,
                deletePhotos = deletePhotos,
                onDeletePhotosChange = { deletePhotos = it }
            )
        },
        confirmButton = {
            if (canDelete) {
                DeleteConfirmButton(
                    onConfirm = { onConfirm(deletePhotos) }
                )
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
private fun DeleteDialogContent(
    category: Category,
    canDelete: Boolean,
    reason: String?,
    deletePhotos: Boolean,
    onDeletePhotosChange: (Boolean) -> Unit
) {
    Column {
        Text("Delete '${category.displayName}'?")

        if (!canDelete) {
            DeleteErrorMessage(reason)
        } else {
            DeletePhotosCheckbox(
                checked = deletePhotos,
                onCheckedChange = onDeletePhotosChange
            )
        }
    }
}

@Composable
private fun DeleteErrorMessage(reason: String?) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = reason ?: "Cannot delete this pile",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DeletePhotosCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Also delete all photos in this pile")
    }
}

@Composable
private fun DeleteConfirmButton(onConfirm: () -> Unit) {
    TextButton(onClick = onConfirm) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
    }
}