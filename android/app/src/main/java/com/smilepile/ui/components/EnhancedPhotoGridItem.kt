package com.smilepile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smilepile.data.models.Photo
import com.smilepile.ui.components.shared.shimmer

/**
 * Enhanced photo grid item with performance optimizations and smooth animations.
 * This component provides smooth interactions, efficient image loading, and visual feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedPhotoGridItem(
    photo: Photo,
    imageModel: Any,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var imageLoadError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Optimized image request with proper sizing and caching
    val imageRequest = ImageRequest.Builder(context)
        .data(imageModel)
        .crossfade(true)
        .crossfade(300)
        .memoryCacheKey("photo_${photo.id}")
        .diskCacheKey("photo_${photo.id}")
        .build()

    ScaleOnPressAnimation(pressed = isPressed) {
        Card(
            modifier = modifier
                .aspectRatio(1f)
                .combinedClickable(
                    onClick = {
                        onPhotoClick()
                        isPressed = false
                    },
                    onLongClick = {
                        onPhotoLongClick()
                        isPressed = false
                    }
                ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 4.dp
            ),
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
                if (imageLoadError) {
                    // Error state for failed image loads
                    ImageLoadError(
                        onRetry = {
                            imageLoadError = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = photo.displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        onError = {
                            imageLoadError = true
                        }
                    )
                }

                // Selection mode overlay
                if (isSelectionMode) {
                    SmoothVisibilityAnimation(
                        visible = true,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onPhotoClick() },
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                                .padding(2.dp)
                        )
                    }
                } else {
                    // Favorite button with smooth animation
                    SmoothVisibilityAnimation(
                        visible = !isSelectionMode,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        BounceAnimation(triggered = photo.isFavorite) {
                            IconButton(
                                onClick = onFavoriteToggle,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.4f),
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
                                        "Remove from favorites"
                                    } else {
                                        "Add to favorites"
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
                    }
                }

                // Selection overlay with smooth animation
                SmoothVisibilityAnimation(
                    visible = isSelectionMode && isSelected,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                    ) {
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

                // Shimmer loading state while image loads
                if (!imageLoadError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmer()
                    )
                }
            }
        }
    }
}