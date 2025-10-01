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
import androidx.compose.material.icons.outlined.Layers
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

    val suggestedCategories = remember {
        listOf(
            Pair("Family", "#FF6B6B"),
            Pair("Friends", "#4ECDC4"),
            Pair("Fun", "#FFEAA7")
        )
    }

    val colorOptions = remember {
        listOf(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
            "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
            "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CategorySetupHeader()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (categories.isNotEmpty()) {
                item {
                    CreatedCategoriesSection(
                        categories = categories,
                        onCategoryRemoved = onCategoryRemoved
                    )
                }
            }

            item {
                CustomCategorySection(
                    newCategoryName = newCategoryName,
                    onCategoryNameChange = { newCategoryName = it },
                    selectedColor = selectedColor,
                    onColorChange = { selectedColor = it },
                    colorOptions = colorOptions,
                    categories = categories,
                    onCategoryAdded = onCategoryAdded
                )
            }

            if (suggestedCategories.isNotEmpty()) {
                item {
                    SuggestedCategoriesSection(
                        suggestedCategories = suggestedCategories,
                        categories = categories,
                        onCategoryAdded = onCategoryAdded
                    )
                }
            }
        }

        ContinueButtonSection(
            categories = categories,
            onContinue = onContinue
        )
    }
}

@Composable
private fun CategorySetupHeader() {
    Icon(
        imageVector = Icons.Outlined.Layers,
        contentDescription = "Piles",
        modifier = Modifier
            .size(48.dp),
        tint = Color(0xFFFF6600)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Organize your photos into colorful piles",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    )
}

@Composable
private fun CreatedCategoriesSection(
    categories: List<TempCategory>,
    onCategoryRemoved: (TempCategory) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Piles",
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

@Composable
private fun CustomCategorySection(
    newCategoryName: String,
    onCategoryNameChange: (String) -> Unit,
    selectedColor: String,
    onColorChange: (String) -> Unit,
    colorOptions: List<String>,
    categories: List<TempCategory>,
    onCategoryAdded: (TempCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Create Your Own",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CategoryNameInput(
            newCategoryName = newCategoryName,
            onCategoryNameChange = onCategoryNameChange,
            selectedColor = selectedColor,
            categories = categories,
            onCategoryAdded = onCategoryAdded
        )

        ColorPickerRow(
            colorOptions = colorOptions,
            selectedColor = selectedColor,
            onColorChange = onColorChange
        )
    }
}

@Composable
private fun CategoryNameInput(
    newCategoryName: String,
    onCategoryNameChange: (String) -> Unit,
    selectedColor: String,
    categories: List<TempCategory>,
    onCategoryAdded: (TempCategory) -> Unit
) {
    OutlinedTextField(
        value = newCategoryName,
        onValueChange = onCategoryNameChange,
        label = { Text("Custom pile name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            AddCategoryButton(
                newCategoryName = newCategoryName,
                categories = categories,
                onClick = {
                    onCategoryAdded(
                        TempCategory(
                            name = newCategoryName,
                            colorHex = selectedColor
                        )
                    )
                    onCategoryNameChange("")
                }
            )
        }
    )
}

@Composable
private fun AddCategoryButton(
    newCategoryName: String,
    categories: List<TempCategory>,
    onClick: () -> Unit
) {
    val isEnabled = newCategoryName.isNotBlank() && categories.size < 5

    IconButton(
        onClick = { if (isEnabled) onClick() },
        enabled = isEnabled
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = if (isEnabled)
                Color(0xFF2196F3)
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ColorPickerRow(
    colorOptions: List<String>,
    selectedColor: String,
    onColorChange: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colorOptions) { color ->
            ColorOption(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorChange(color) }
            )
        }
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(color)))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SuggestedCategoriesSection(
    suggestedCategories: List<Pair<String, String>>,
    categories: List<TempCategory>,
    onCategoryAdded: (TempCategory) -> Unit
) {
    Column {
        Text(
            text = "Or Quick Add",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestedCategories
                .filter { (name, _) -> !categories.any { it.name == name } }
                .forEach { (name, colorHex) ->
                    SuggestedCategoryCard(
                        name = name,
                        colorHex = colorHex,
                        onAdd = {
                            onCategoryAdded(
                                TempCategory(
                                    name = name,
                                    colorHex = colorHex
                                )
                            )
                        }
                    )
                }
        }
    }
}

@Composable
private fun ContinueButtonSection(
    categories: List<TempCategory>,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (categories.isEmpty()) {
            Text(
                text = "Add at least one pile to continue",
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
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SuggestedCategoryCard(
    name: String,
    colorHex: String,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onAdd() },
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                )

                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = Color(android.graphics.Color.parseColor(colorHex)),
                modifier = Modifier.size(20.dp)
            )
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
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.colorHex)))
            )

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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}