package com.smilepile.ui.components.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smilepile.data.models.Category

/**
 * Reusable category filter component that displays a horizontal row of filter chips
 * for category selection. Works for both Kids Mode and Parent Mode contexts.
 */
@Composable
fun CategoryFilterComponent(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding
    ) {
        // Category filter chips only
        items(categories) { category ->
            FilterChip(
                onClick = { onCategorySelected(category.id) },
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                selected = selectedCategoryId == category.id,
                modifier = Modifier.height(48.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    )
}