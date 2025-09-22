package com.smilepile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.smilepile.data.models.Category
import com.smilepile.ui.theme.SmilePileTheme

/**
 * Reusable category chip component for displaying and selecting categories.
 *
 * Features:
 * - Visual feedback for selected state
 * - Color-coded category display
 * - Material3 design compliance
 * - Clickable interaction
 *
 * @param category The category to display
 * @param isSelected Whether this chip is currently selected
 * @param onClick Callback when chip is clicked, null for non-interactive chips
 * @param modifier Modifier for customization
 */
@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean = false,
    onClick: ((Category) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .clickable { onClick?.invoke(category) }
            .clip(RoundedCornerShape(16.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator circle
            CategoryColorIndicator(
                colorHex = category.colorHex,
                size = 12.dp
            )

            // Category name
            Text(
                text = category.displayName,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CategoryColorIndicator(
    colorHex: String?,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (colorHex != null && colorHex.startsWith("#") && colorHex.length == 7) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(indicatorColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
    )
}

@Composable
fun SelectableCategoryChip(
    category: Category,
    isSelected: Boolean = false,
    photoCount: Int = 0,
    onClick: ((Category) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier
            .clickable { onClick?.invoke(category) }
            .clip(RoundedCornerShape(20.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator circle
            CategoryColorIndicator(
                colorHex = category.colorHex,
                size = 14.dp
            )

            // Category name
            Text(
                text = category.displayName,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )

            // Photo count badge
            if (photoCount > 0) {
                Text(
                    text = photoCount.toString(),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .background(
                            color = textColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipPreview() {
    SmilePileTheme {
        val sampleCategory = Category(
            id = 1,
            name = "animals",
            displayName = "Animals",
            position = 0,
            colorHex = "#4CAF50",
            isDefault = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CategoryChip(
                category = sampleCategory,
                isSelected = false
            )

            CategoryChip(
                category = sampleCategory,
                isSelected = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectableCategoryChipPreview() {
    SmilePileTheme {
        val sampleCategory = Category(
            id = 1,
            name = "animals",
            displayName = "Animals",
            position = 0,
            colorHex = "#4CAF50",
            isDefault = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            SelectableCategoryChip(
                category = sampleCategory,
                isSelected = false,
                photoCount = 12
            )

            SelectableCategoryChip(
                category = sampleCategory,
                isSelected = true,
                photoCount = 5
            )
        }
    }
}