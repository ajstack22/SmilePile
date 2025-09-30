package com.smilepile.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smilepile.onboarding.TempCategory

@Composable
fun CategorySetupScreen(
    categories: List<TempCategory>,
    onCategoryAdded: (TempCategory) -> Unit,
    onCategoryRemoved: (TempCategory) -> Unit,
    onContinue: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var showColorPicker by remember { mutableStateOf(false) }

    val suggestedCategories = listOf(
        Triple("Family", "#FF6B6B", "👨‍👩‍👧‍👦"),
        Triple("Friends", "#4ECDC4", "👫"),
        Triple("Vacation", "#45B7D1", "🏖️"),
        Triple("Pets", "#96CEB4", "🐾"),
        Triple("Fun", "#FFEAA7", "🎉"),
        Triple("School", "#DDA0DD", "🎒")
    )

    val colorOptions = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
    )

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
                text = "Create Categories",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Organize your photos into colorful categories",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quick add suggestions
            if (suggestedCategories.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Quick Add",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(suggestedCategories) { (name, colorHex, icon) ->
                                if (!categories.any { it.name == name }) {
                                    SuggestedCategoryCard(
                                        name = name,
                                        colorHex = colorHex,
                                        icon = icon,
                                        onAdd = {
                                            onCategoryAdded(
                                                TempCategory(
                                                    name = name,
                                                    colorHex = colorHex,
                                                    icon = icon
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom category creation
            item {
                Column {
                    Text(
                        text = "Create Custom",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Category name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(selectedColor)))
                                        .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                        .clickable { showColorPicker = !showColorPicker }
                                )
                            }

                            // Color palette
                            if (showColorPicker) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(6),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(120.dp)
                                ) {
                                    items(colorOptions) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(color)))
                                                .border(
                                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    selectedColor = color
                                                    showColorPicker = false
                                                }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        onCategoryAdded(
                                            TempCategory(
                                                name = newCategoryName,
                                                colorHex = selectedColor
                                            )
                                        )
                                        newCategoryName = ""
                                        showColorPicker = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newCategoryName.isNotBlank() && categories.size < 5,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B6B)
                                )
                            ) {
                                Text("Add Category")
                            }
                        }
                    }
                }
            }

            // Created categories
            if (categories.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Categories",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "${categories.size}/5",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { category ->
                                CreatedCategoryRow(
                                    category = category,
                                    onRemove = { onCategoryRemoved(category) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue button
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (categories.isEmpty()) {
                Text(
                    text = "Add at least one category to continue",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = categories.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SuggestedCategoryCard(
    name: String,
    colorHex: String,
    icon: String,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )

            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = Color(android.graphics.Color.parseColor(colorHex))
                )
            }
        }
    }
}

@Composable
private fun CreatedCategoryRow(
    category: TempCategory,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.colorHex)))
            )

            category.icon?.let { icon ->
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }

            Text(
                text = category.name,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Remove",
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}