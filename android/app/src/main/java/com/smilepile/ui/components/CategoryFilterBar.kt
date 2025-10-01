package com.smilepile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.smilepile.data.models.Category

/**
 * Horizontal scrollable filter bar for category selection in gallery view
 * Allows quick filtering of photos by categories
 */
@Composable
fun CategoryFilterBar(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategoryToggle: (Long) -> Unit,
    onClearAll: () -> Unit,
    onSelectAll: () -> Unit,
    showAllOption: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterMenuButton(
                selectedCategoryIds = selectedCategoryIds,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onSelectAll = onSelectAll,
                onClearAll = onClearAll
            )

            CategoryChipsRow(
                categories = categories,
                selectedCategoryIds = selectedCategoryIds,
                showAllOption = showAllOption,
                scrollState = scrollState,
                onClearAll = onClearAll,
                onCategoryToggle = onCategoryToggle
            )

            ClearFiltersButton(
                visible = selectedCategoryIds.isNotEmpty(),
                onClearAll = onClearAll
            )
        }

        ActiveFilterSummary(
            categories = categories,
            selectedCategoryIds = selectedCategoryIds
        )

        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex ?: "#808080"))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                category.displayName,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = categoryColor.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = categoryColor,
                selectedBorderColor = categoryColor,
                borderWidth = 2.dp,
                selectedBorderWidth = 2.dp
            )
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    )
}

/**
 * Compact version of CategoryFilterBar for smaller screens
 */
@Composable
fun CompactCategoryFilterBar(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onShowFullFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowFullFilter() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Categories",
                    tint = if (selectedCategoryIds.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when {
                        selectedCategoryIds.isEmpty() -> "All Categories"
                        selectedCategoryIds.size == 1 -> {
                            categories.find { it.id == selectedCategoryIds.first() }?.displayName
                                ?: "1 Category"
                        }
                        else -> "${selectedCategoryIds.size} Categories"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selectedCategoryIds.isNotEmpty()) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Normal
                    }
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// MARK: - Helper Composables for Complexity Reduction

@Composable
private fun FilterMenuButton(
    selectedCategoryIds: Set<Long>,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
) {
    Box {
        IconButton(
            onClick = onShowMenu,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Badge(
                containerColor = if (selectedCategoryIds.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            ) {
                if (selectedCategoryIds.isNotEmpty()) {
                    Text(
                        text = selectedCategoryIds.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter options",
                tint = if (selectedCategoryIds.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        FilterDropdownMenu(
            showMenu = showMenu,
            onDismissMenu = onDismissMenu,
            onSelectAll = onSelectAll,
            onClearAll = onClearAll
        )
    }
}

@Composable
private fun FilterDropdownMenu(
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissMenu
    ) {
        DropdownMenuItem(
            text = { Text("Select All") },
            onClick = {
                onSelectAll()
                onDismissMenu()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.SelectAll,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Clear All") },
            onClick = {
                onClearAll()
                onDismissMenu()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun RowScope.CategoryChipsRow(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    showAllOption: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onClearAll: () -> Unit,
    onCategoryToggle: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showAllOption) {
            AllCategoriesChip(
                isSelected = selectedCategoryIds.isEmpty(),
                onClick = onClearAll
            )
        }

        categories.forEach { category ->
            CategoryFilterChip(
                category = category,
                isSelected = selectedCategoryIds.contains(category.id),
                onClick = { onCategoryToggle(category.id) }
            )
        }
    }
}

@Composable
private fun AllCategoriesChip(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                "All",
                fontWeight = if (isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun ClearFiltersButton(
    visible: Boolean,
    onClearAll: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        IconButton(
            onClick = onClearAll,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear filters",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActiveFilterSummary(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>
) {
    if (selectedCategoryIds.isNotEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(4.dp)
        ) {
            val selectedCategoryNames = categories
                .filter { selectedCategoryIds.contains(it.id) }
                .map { it.displayName }
                .joinToString(", ")

            Text(
                text = "Filtering by: $selectedCategoryNames",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}