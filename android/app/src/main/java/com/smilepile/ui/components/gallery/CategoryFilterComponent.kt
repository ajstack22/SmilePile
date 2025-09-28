package com.smilepile.ui.components.gallery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smilepile.data.models.Category
import com.smilepile.ui.components.CategoryColorIndicator

/**
 * Custom category chip that matches the iOS design
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

    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Use primary color like iOS
            } else {
                Color.Transparent // Clear background for unselected
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // No elevation to match iOS
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary // Use primary color like iOS
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Use primary with opacity like iOS
            }
        ),
        shape = RoundedCornerShape(16.dp) // Match iOS corner radius
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = categoryColor,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), // Match iOS dot border
                        shape = CircleShape
                    )
            )

            // Category text
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = when {
                        isDarkTheme -> androidx.compose.ui.text.font.FontWeight.Bold // Bold in dark mode
                        isSelected -> androidx.compose.ui.text.font.FontWeight.Medium // Medium when selected
                        else -> androidx.compose.ui.text.font.FontWeight.Normal // Regular otherwise
                    }
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary // Primary color when selected
                } else {
                    MaterialTheme.colorScheme.secondary // Secondary color when unselected
                }
            )
        }
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