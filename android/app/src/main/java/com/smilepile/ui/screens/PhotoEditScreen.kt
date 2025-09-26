package com.smilepile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.ui.components.editor.CropOverlay
import com.smilepile.ui.viewmodels.PhotoEditViewModel
import com.smilepile.utils.ImageProcessor
import com.smilepile.data.models.Category
import kotlin.math.min

/**
 * Photo edit screen with rotate and crop functionality.
 * Atlas Lite: Simple, pragmatic, under 250 lines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    onComplete: (List<com.smilepile.ui.viewmodels.PhotoEditResult>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showCropOverlay by remember { mutableStateOf(false) }
    var selectedAspectRatio by remember { mutableStateOf(ImageProcessor.AspectRatio.FREE) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Key to force recreation of CropOverlay when aspect ratio changes
    var cropOverlayKey by remember { mutableStateOf(0) }

    // Debug logging
    LaunchedEffect(Unit) {
        android.util.Log.e("SmilePile", "PhotoEditScreen rendered!")
        android.util.Log.e("SmilePile", "Edit queue size: ${uiState.editQueue.size}")
        android.util.Log.e("SmilePile", "Current index: ${uiState.currentIndex}")
    }

    // Handle completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            // Save all edited photos
            val savedPhotos = viewModel.saveAllProcessedPhotos()
            android.util.Log.e("SmilePile", "Saved ${savedPhotos.size} edited photos")
            onComplete(viewModel.getProcessedResults())
        }
    }

    Scaffold(
        topBar = {
            EditTopBar(
                progressText = uiState.progressText,
                onCancel = {
                    android.util.Log.e("SmilePile", "Cancel/Back button clicked in editor")
                    onCancel()
                }
            )
        },
        bottomBar = {
            EditBottomBar(
                showCropOverlay = showCropOverlay,
                canApplyToAll = uiState.canApplyToAll,
                isSinglePhoto = uiState.totalPhotos == 1,
                onCategoryClick = { showCategoryDialog = true },
                onRotateClick = { viewModel.rotatePhoto() },
                onCropClick = { showCropOverlay = !showCropOverlay },
                onDeleteClick = { showDeleteDialog = true },
                onSkipClick = { viewModel.skipCurrentPhoto() },
                onApplyClick = { viewModel.applyCurrentPhoto() },
                onApplyToAllClick = { viewModel.applyToAll() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                uiState.error != null -> {
                    ErrorDisplay(
                        error = uiState.error ?: "Unknown error",
                        onSkip = { viewModel.skipCurrentPhoto() }
                    )
                }
                uiState.previewBitmap != null -> {
                    val previewBitmap = uiState.previewBitmap!!
                    // Photo display
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Photo to edit",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Crop overlay when active
                        if (showCropOverlay) {
                            // Calculate initial crop rect based on selected aspect ratio
                            val initialRect = when (selectedAspectRatio) {
                                    ImageProcessor.AspectRatio.FREE -> {
                                        // Full image by default
                                        androidx.compose.ui.geometry.Rect(
                                            offset = androidx.compose.ui.geometry.Offset.Zero,
                                            size = androidx.compose.ui.geometry.Size(
                                                previewBitmap.width.toFloat(),
                                                previewBitmap.height.toFloat()
                                            )
                                        )
                                    }
                                    ImageProcessor.AspectRatio.SQUARE -> {
                                        // Center square crop
                                        val size = min(previewBitmap.width, previewBitmap.height).toFloat()
                                        val offsetX = (previewBitmap.width - size) / 2f
                                        val offsetY = (previewBitmap.height - size) / 2f
                                        androidx.compose.ui.geometry.Rect(
                                            offset = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
                                            size = androidx.compose.ui.geometry.Size(size, size)
                                        )
                                    }
                                    ImageProcessor.AspectRatio.RATIO_4_3 -> {
                                        // 4:3 aspect ratio crop
                                        val targetRatio = 4f / 3f
                                        val imageRatio = previewBitmap.width.toFloat() / previewBitmap.height
                                        val (width, height) = if (imageRatio > targetRatio) {
                                            val h = previewBitmap.height.toFloat()
                                            val w = h * targetRatio
                                            w to h
                                        } else {
                                            val w = previewBitmap.width.toFloat()
                                            val h = w / targetRatio
                                            w to h
                                        }
                                        val offsetX = (previewBitmap.width - width) / 2f
                                        val offsetY = (previewBitmap.height - height) / 2f
                                        androidx.compose.ui.geometry.Rect(
                                            offset = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
                                            size = androidx.compose.ui.geometry.Size(width, height)
                                        )
                                    }
                                    ImageProcessor.AspectRatio.RATIO_16_9 -> {
                                        // 16:9 aspect ratio crop
                                        val targetRatio = 16f / 9f
                                        val imageRatio = previewBitmap.width.toFloat() / previewBitmap.height
                                        val (width, height) = if (imageRatio > targetRatio) {
                                            val h = previewBitmap.height.toFloat()
                                            val w = h * targetRatio
                                            w to h
                                        } else {
                                            val w = previewBitmap.width.toFloat()
                                            val h = w / targetRatio
                                            w to h
                                        }
                                        val offsetX = (previewBitmap.width - width) / 2f
                                        val offsetY = (previewBitmap.height - height) / 2f
                                        androidx.compose.ui.geometry.Rect(
                                            offset = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
                                            size = androidx.compose.ui.geometry.Size(width, height)
                                        )
                                    }
                                }

                            // Use key to force recreation when aspect ratio changes
                            key(cropOverlayKey) {
                                CropOverlay(
                                    imageWidth = previewBitmap.width.toFloat(),
                                    imageHeight = previewBitmap.height.toFloat(),
                                    onCropRectChange = { rect ->
                                        viewModel.updateCropRect(rect)
                                    },
                                    initialCropRect = initialRect
                                )
                            }

                            // Aspect ratio buttons
                            AspectRatioButtons(
                                selectedRatio = selectedAspectRatio,
                                onRatioSelected = { ratio ->
                                    selectedAspectRatio = ratio
                                    // Apply the aspect ratio to the view model
                                    viewModel.applyAspectRatio(ratio)
                                    // Force recreation of CropOverlay with new aspect ratio
                                    cropOverlayKey++
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Category selection dialog
    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            selectedCategoryId = viewModel.getPendingCategoryId(),
            onCategorySelected = { categoryId ->
                viewModel.updatePendingCategory(categoryId)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Photo?") },
            text = { Text("This photo will be removed from the gallery.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCurrentPhoto()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(
    progressText: String,
    onCancel: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Edit Photo • $progressText",
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.7f),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun EditBottomBar(
    showCropOverlay: Boolean,
    canApplyToAll: Boolean,
    isSinglePhoto: Boolean,
    onCategoryClick: () -> Unit,
    onRotateClick: () -> Unit,
    onCropClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSkipClick: () -> Unit,
    onApplyClick: () -> Unit,
    onApplyToAllClick: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Edit tools
            if (!showCropOverlay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Category button
                    IconButton(
                        onClick = onCategoryClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = "Category",
                                tint = Color(0xFF4A90E2) // Blue color matching footer menu
                            )
                            Text("Category", color = Color(0xFF4A90E2), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Rotate button
                    IconButton(
                        onClick = onRotateClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.RotateRight,
                                contentDescription = "Rotate",
                                tint = Color.White
                            )
                            Text("Rotate", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Crop button
                    IconButton(
                        onClick = onCropClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = Color.White
                            )
                            Text("Crop", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Skip/Cancel button
                OutlinedButton(
                    onClick = onSkipClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(if (isSinglePhoto) "Cancel" else "Skip")
                }

                // Apply button
                Button(
                    onClick = onApplyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply")
                }
            }

            // Apply to all option
            if (canApplyToAll) {
                TextButton(
                    onClick = onApplyToAllClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply rotation to all remaining photos", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun AspectRatioButtons(
    selectedRatio: ImageProcessor.AspectRatio,
    onRatioSelected: (ImageProcessor.AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AspectRatioChip("Free", ImageProcessor.AspectRatio.FREE, selectedRatio, onRatioSelected)
        AspectRatioChip("1:1", ImageProcessor.AspectRatio.SQUARE, selectedRatio, onRatioSelected)
        AspectRatioChip("4:3", ImageProcessor.AspectRatio.RATIO_4_3, selectedRatio, onRatioSelected)
        AspectRatioChip("16:9", ImageProcessor.AspectRatio.RATIO_16_9, selectedRatio, onRatioSelected)
    }
}

@Composable
private fun AspectRatioChip(
    label: String,
    ratio: ImageProcessor.AspectRatio,
    selectedRatio: ImageProcessor.AspectRatio,
    onSelect: (ImageProcessor.AspectRatio) -> Unit
) {
    FilterChip(
        selected = ratio == selectedRatio,
        onClick = { onSelect(ratio) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.White,
            selectedLabelColor = Color.Black,
            containerColor = Color.Transparent,
            labelColor = Color.White
        )
    )
}

@Composable
private fun ErrorDisplay(
    error: String,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = error,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSkip) {
            Text("Skip This Photo")
        }
    }
}

@Composable
private fun CategorySelectionDialog(
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedId by remember { mutableStateOf(selectedCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedId == category.id),
                                onClick = { selectedId = category.id }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedId == category.id),
                            onClick = { selectedId = category.id }
                        )
                        Spacer(Modifier.width(16.dp))
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
                    onCategorySelected(selectedId)
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}