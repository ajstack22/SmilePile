package com.smilepile.ui.components.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
    allChipLabel: String = "All",
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding
    ) {
        // "All" filter chip
        item {
            FilterChip(
                onClick = { onCategorySelected(null) },
                label = { Text(allChipLabel) },
                selected = selectedCategoryId == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // Category filter chips
        items(categories) { category ->
            FilterChip(
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.displayName) },
                selected = selectedCategoryId == category.id,
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
 * Kids Mode version with toggle behavior - tap selected chip to deselect
 */
@Composable
fun CategoryFilterComponentKidsMode(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allChipLabel: String = "All Photos",
    enableToggle: Boolean = true
) {
    CategoryFilterComponent(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = { categoryId ->
            if (enableToggle && selectedCategoryId == categoryId) {
                // Toggle behavior: deselect if already selected
                onCategorySelected(null)
            } else {
                onCategorySelected(categoryId)
            }
        },
        modifier = modifier,
        allChipLabel = allChipLabel,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    )
}