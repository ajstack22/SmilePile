package com.smilepile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smilepile.data.models.Category
import com.smilepile.ui.theme.SmilePileTheme
import com.smilepile.ui.viewmodels.CategoryViewModel

@Composable
fun AddCategoryDialog(
    isVisible: Boolean,
    editingCategory: Category? = null,
    predefinedColors: List<String> = CategoryViewModel.PREDEFINED_COLORS,
    onDismiss: () -> Unit,
    onSave: (displayName: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var displayNameText by remember(editingCategory) {
        mutableStateOf(TextFieldValue(editingCategory?.displayName ?: ""))
    }
    var selectedColorHex by remember(editingCategory) {
        mutableStateOf(editingCategory?.colorHex ?: predefinedColors.first())
    }

    val isEditing = editingCategory != null
    val title = if (isEditing) "Edit Category" else "Add Category"
    val saveButtonText = if (isEditing) "Update" else "Add"

    // Validation
    val isDisplayNameValid = displayNameText.text.isNotBlank()
    val isSaveEnabled = isDisplayNameValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Category name field
                OutlinedTextField(
                    value = displayNameText,
                    onValueChange = { displayNameText = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Animals, Nature, Fun") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isDisplayNameValid && displayNameText.text.isNotEmpty()
                )

                // Color selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Category Color",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(predefinedColors) { colorHex ->
                            ColorSelectionItem(
                                colorHex = colorHex,
                                isSelected = selectedColorHex == colorHex,
                                onClick = { selectedColorHex = colorHex }
                            )
                        }
                    }
                }

                // Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Preview:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (displayNameText.text.isNotBlank()) {
                            CategoryColorIndicator(
                                colorHex = selectedColorHex,
                                size = 14.dp
                            )
                            Text(
                                text = displayNameText.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Enter display name to see preview",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSave(
                                displayNameText.text.trim(),
                                selectedColorHex
                            )
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text(saveButtonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSelectionItem(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CategoryValidationDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(text = message)
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCategoryDialogPreview() {
    SmilePileTheme {
        AddCategoryDialog(
            isVisible = true,
            onDismiss = {},
            onSave = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditCategoryDialogPreview() {
    SmilePileTheme {
        val sampleCategory = Category(
            id = 1,
            name = "animals",
            displayName = "Animals",
            position = 0,
            colorHex = "#4CAF50",
            isDefault = true
        )

        AddCategoryDialog(
            isVisible = true,
            editingCategory = sampleCategory,
            onDismiss = {},
            onSave = { _, _ -> }
        )
    }
}