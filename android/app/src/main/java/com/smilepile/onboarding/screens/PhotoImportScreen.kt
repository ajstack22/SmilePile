package com.smilepile.onboarding.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smilepile.onboarding.ImportedPhotoData
import com.smilepile.onboarding.TempCategory

@Composable
fun PhotoImportScreen(
    categories: List<TempCategory>,
    importedPhotos: List<ImportedPhotoData>,
    onPhotosSelected: (List<ImportedPhotoData>) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    var localPhotos by remember { mutableStateOf(importedPhotos) }
    val maxPhotos = 5

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newPhotos = uris.take(maxPhotos - localPhotos.size).map { uri ->
            ImportedPhotoData(
                uri = uri,
                categoryId = categories.firstOrNull()?.id
            )
        }
        localPhotos = localPhotos + newPhotos
        onPhotosSelected(localPhotos)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instructions
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Your First Photos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Select up to $maxPhotos photos to get started",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = "You can always add more later",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (localPhotos.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )

                    Text(
                        text = "No photos selected yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier
                            .width(200.dp)
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Select Photos")
                    }
                }
            } else {
                // Photos grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(localPhotos) { photo ->
                            PhotoThumbnail(
                                photo = photo,
                                categories = categories,
                                onCategoryChanged = { categoryId ->
                                    localPhotos = localPhotos.map {
                                        if (it.uri == photo.uri) {
                                            it.copy(categoryId = categoryId)
                                        } else it
                                    }
                                    onPhotosSelected(localPhotos)
                                },
                                onRemove = {
                                    localPhotos = localPhotos.filter { it.uri != photo.uri }
                                    onPhotosSelected(localPhotos)
                                }
                            )
                        }
                    }

                    // Add more photos button
                    if (localPhotos.size < maxPhotos) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Add More Photos (${localPhotos.size}/$maxPhotos)")
                        }
                    }

                    // Category assignment reminder
                    if (localPhotos.any { it.categoryId == null }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Tap photos to assign categories",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = if (localPhotos.isEmpty()) "Skip for Now" else "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: ImportedPhotoData,
    categories: List<TempCategory>,
    onCategoryChanged: (String?) -> Unit,
    onRemove: () -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    val assignedCategory = categories.find { it.id == photo.categoryId }

    Box {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 2.dp,
                    color = if (assignedCategory != null)
                        Color(android.graphics.Color.parseColor(assignedCategory.colorHex))
                    else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { showCategoryDialog = true }
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(2.dp)
                )
            }
        }

        // Category badge
        assignedCategory?.let { category ->
            Text(
                text = category.name,
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    // Category selection dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                Column {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCategoryChanged(category.id)
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(category.colorHex)),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(category.name)
                            if (photo.categoryId == category.id) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}