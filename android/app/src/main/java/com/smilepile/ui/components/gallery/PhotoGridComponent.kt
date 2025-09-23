package com.smilepile.ui.components.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smilepile.R
import com.smilepile.data.models.Photo
import java.io.File

/**
 * PhotoGridComponent - Reusable photo grid display component
 *
 * A self-contained component for displaying photos in a grid layout with support for:
 * - Photo grid display (LazyVerticalGrid)
 * - Photo item rendering with Material3 design
 * - Click/long-click handling
 * - Favorite toggle functionality
 * - Selection state display
 * - Empty state handling
 *
 * This component accepts photos as input and handles interactions via callbacks,
 * making it completely independent from ViewModels.
 */
@Composable
fun PhotoGridComponent(
    photos: List<Photo>,
    selectedPhotos: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit = {},
    onFavoriteToggle: ((Photo) -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Int = 8
) {
    if (photos.isEmpty()) {
        EmptyPhotoGridState(modifier = modifier)
    } else {
        PhotoGrid(
            photos = photos,
            selectedPhotos = selectedPhotos,
            isSelectionMode = isSelectionMode,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = modifier,
            columns = columns,
            contentPadding = contentPadding,
            spacing = spacing
        )
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotos: Set<Long>,
    isSelectionMode: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onFavoriteToggle: ((Photo) -> Unit)?,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Int = 8
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        items(photos) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.id),
                isSelectionMode = isSelectionMode,
                onPhotoClick = { onPhotoClick(photo) },
                onPhotoLongClick = { onPhotoLongClick(photo) },
                onFavoriteToggle = { onFavoriteToggle?.invoke(photo) }
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
            // Photo image
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
            }

            // Selection overlay with check icon
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
private fun EmptyPhotoGridState(
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
                text = "No photos to display",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Photos will appear here when available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}