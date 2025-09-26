package com.smilepile.ui.components.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smilepile.R
import com.smilepile.data.models.Photo
import java.io.File

/**
 * PhotoStackComponent - Unified stacked photo display for both edit and kids modes
 *
 * Displays photos in a vertical stack layout similar to kids mode
 * with optional edit/delete action buttons for parent mode
 */
@Composable
fun PhotoStackComponent(
    photos: List<Photo>,
    selectedPhotos: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    showEditActions: Boolean = false, // Show edit/delete buttons
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit = {},
    onEditClick: ((Photo) -> Unit)? = null,
    onDeleteClick: ((Photo) -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    if (photos.isEmpty()) {
        EmptyPhotoStackState(modifier = modifier)
    } else {
        PhotoStack(
            photos = photos,
            selectedPhotos = selectedPhotos,
            isSelectionMode = isSelectionMode,
            showEditActions = showEditActions,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            modifier = modifier,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun PhotoStack(
    photos: List<Photo>,
    selectedPhotos: Set<Long>,
    isSelectionMode: Boolean,
    showEditActions: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onEditClick: ((Photo) -> Unit)?,
    onDeleteClick: ((Photo) -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(photos) { photo ->
            PhotoStackItem(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.id),
                isSelectionMode = isSelectionMode,
                showEditActions = showEditActions,
                onPhotoClick = { onPhotoClick(photo) },
                onPhotoLongClick = { onPhotoLongClick(photo) },
                onEditClick = { onEditClick?.invoke(photo) },
                onDeleteClick = { onDeleteClick?.invoke(photo) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoStackItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    showEditActions: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onPhotoClick,
                onLongClick = onPhotoLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box {
            // Photo with 4:3 aspect ratio
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photo.path))
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
            )

            // Selection checkbox in top-left
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onPhotoClick() },
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyPhotoStackState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = "No photos",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No photos yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add photos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}