package com.smilepile.ui.components.gallery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.smilepile.data.models.Category
import com.smilepile.ui.components.CategoryColorIndicator

/**
 * Custom category chip that matches the app's design language
 */
@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Check if we're in dark theme by checking background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Use black for dark mode, surfaceVariant for light mode
    val unselectedBackground = if (isDarkTheme) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) categoryColor else unselectedBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 1.dp else 0.dp
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) Color.Transparent else categoryColor
        ),
        shape = RoundedCornerShape(50) // Full pill shape
    ) {
        // Just text, no row needed
        Text(
            text = category.displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isDarkTheme) {
                    androidx.compose.ui.text.font.FontWeight.ExtraBold
                } else if (isSelected) {
                    androidx.compose.ui.text.font.FontWeight.Bold
                } else {
                    androidx.compose.ui.text.font.FontWeight.Normal
                }
            ),
            color = if (isSelected || isDarkTheme) Color.White else categoryColor
        )
    }
}

/**
 * Reusable category filter component that displays a horizontal row of custom category chips
 * for category selection. Works for both Kids Mode and Parent Mode contexts.
 */
@Composable
fun CategoryFilterComponent(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding
    ) {
        // Category filter chips
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

/**
 * Kids Mode version - no toggle behavior, always shows a selected category
 * A category must always be selected - defaults to first category
 */
@Composable
fun CategoryFilterComponentKidsMode(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    enableToggle: Boolean = false  // Disabled toggle to prevent "All Photos" state
) {
    CategoryFilterComponent(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = { categoryId ->
            // Always select a category, never allow null
            if (categoryId != null) {
                onCategorySelected(categoryId)
            }
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    )
}