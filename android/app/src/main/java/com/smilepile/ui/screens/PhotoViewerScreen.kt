package com.smilepile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import com.smilepile.ui.components.dialogs.UniversalCrudDialog
import com.smilepile.ui.components.dialogs.DialogBuilder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smilepile.R
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.mode.AppMode
import com.smilepile.ui.viewmodels.AppModeViewModel
import com.smilepile.ui.viewmodels.PhotoGalleryViewModel
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    photo: Photo,
    photos: List<Photo>,
    initialIndex: Int = 0,
    onNavigateBack: () -> Unit,
    onSharePhoto: (Photo) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onEditPhoto: (Photo) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PhotoGalleryViewModel = hiltViewModel(),
    modeViewModel: AppModeViewModel = hiltViewModel()
) {
    // Debug logging
    android.util.Log.e("SmilePile", "PhotoViewerScreen - initialIndex: $initialIndex, photos.size: ${photos.size}")
    android.util.Log.e("SmilePile", "PhotoViewerScreen - photo.id: ${photo.id}, photo.name: ${photo.displayName}")
    android.util.Log.e("SmilePile", "PhotoViewerScreen - First 3 photos: ${photos.take(3).map { "${it.id}:${it.displayName}" }}")

    // Always use the passed initialIndex - it should be correct
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = { photos.size }
    )

    val uiState by viewModel.uiState.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Check if we're in parent mode (edit controls available)
    val isParentMode = modeState.currentMode == AppMode.PARENT

    // Start with UI hidden in Kids Mode for immersive experience
    var isUIVisible by remember { mutableStateOf(isParentMode) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Photo pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentPhoto = photos[page]
            ZoomableImage(
                photo = currentPhoto,
                onImageTap = { isUIVisible = !isUIVisible },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top bar overlay
        if (isUIVisible) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = photos[pagerState.currentPage].displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${photos.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier.systemBarsPadding()
            )
        }

        // Bottom controls overlay
        if (isUIVisible) {
            PhotoControlsBar(
                photo = photos[pagerState.currentPage],
                isParentMode = isParentMode,
                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                onSharePhoto = { photo ->
                    // Create share intent for photo
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        val photoFile = File(photo.path)
                        if (photoFile.exists()) {
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            putExtra(Intent.EXTRA_STREAM, photoUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                },
                onEditPhoto = onEditPhoto,
                onDeletePhoto = { showDeleteDialog = true },
                onMovePhoto = { showMoveDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(16.dp)
            )
        }
    }

    // Remove from library confirmation dialog - only in parent mode
    if (showDeleteDialog && isParentMode) {
        RemoveFromLibraryConfirmationDialog(
            photoName = photos[pagerState.currentPage].displayName,
            onConfirm = {
                viewModel.removePhotoFromLibrary(photos[pagerState.currentPage])
                showDeleteDialog = false
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Move to category dialog - only in parent mode
    if (showMoveDialog && isParentMode) {
        MoveToCategoryDialog(
            categories = uiState.categories,
            currentCategoryId = photos[pagerState.currentPage].categoryId,
            onCategorySelected = { categoryId ->
                viewModel.movePhotoToCategory(photos[pagerState.currentPage], categoryId)
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }
}

@Composable
private fun ZoomableImage(
    photo: Photo,
    onImageTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        rotation += rotationChange
        offset += offsetChange
    }

    // Reset transform when photo changes
    LaunchedEffect(photo.id) {
        scale = 1f
        rotation = 0f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
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
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state),
            contentScale = ContentScale.Fit,
            onSuccess = { onImageTap() }
        )
    }
}

@Composable
private fun PhotoControlsBar(
    photo: Photo,
    isParentMode: Boolean,
    onFavoriteToggle: (Photo) -> Unit,
    onSharePhoto: (Photo) -> Unit,
    onEditPhoto: (Photo) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onMovePhoto: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    android.util.Log.e("SmilePile", "PhotoControlsBar rendered for photo: ${photo.displayName}, isParentMode: $isParentMode")
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo metadata
            PhotoMetadata(photo = photo)

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons - conditional based on mode
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite button - available in both modes
                if (isParentMode) {
                    IconButton(
                        onClick = { onFavoriteToggle(photo) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (photo.isFavorite) {
                                    Color.Red.copy(alpha = 0.2f)
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
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
                            tint = if (photo.isFavorite) Color.Red else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Share button - available in both modes
                IconButton(
                    onClick = { onSharePhoto(photo) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_photo),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Edit button - available in both modes
                IconButton(
                    onClick = {
                        android.util.Log.e("SmilePile", "Edit button CLICKED in PhotoControlsBar for photo: ${photo.displayName}")
                        onEditPhoto(photo)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit photo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Parent mode only controls
                if (isParentMode) {
                    // Move button
                    IconButton(
                        onClick = { onMovePhoto(photo) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Move to Category",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Remove from library button
                    IconButton(
                        onClick = { onDeletePhoto(photo) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Remove from Library",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoMetadata(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }
    val formattedDate = remember(photo.createdAt) {
        dateFormat.format(Date(photo.createdAt))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = photo.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )

        if (photo.width > 0 && photo.height > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${photo.width} × ${photo.height}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        if (photo.fileSize > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatFileSize(photo.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatFileSize(sizeInBytes: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        sizeInBytes >= gb -> String.format("%.1f GB", sizeInBytes.toDouble() / gb)
        sizeInBytes >= mb -> String.format("%.1f MB", sizeInBytes.toDouble() / mb)
        sizeInBytes >= kb -> String.format("%.1f KB", sizeInBytes.toDouble() / kb)
        else -> "$sizeInBytes B"
    }
}

@Composable
private fun RemoveFromLibraryConfirmationDialog(
    photoName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    UniversalCrudDialog(
        config = DialogBuilder.confirmation(
            title = "Remove from Library",
            message = "Are you sure you want to remove \"$photoName\" from your SmilePile library? The photo will remain on your device but won't appear in the app.",
            confirmText = "Remove",
            cancelText = "Cancel",
            isDestructive = false,
            icon = Icons.Default.RemoveCircleOutline,
            onConfirm = onConfirm,
            onCancel = onDismiss
        ),
        onDismiss = onDismiss
    )
}

@Composable
private fun MoveToCategoryDialog(
    categories: List<Category>,
    currentCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(currentCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Category") },
        text = {
            Column {
                Text("Select a category for this photo:")
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
                onClick = { onCategorySelected(selectedCategoryId) },
                enabled = selectedCategoryId != currentCategoryId
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