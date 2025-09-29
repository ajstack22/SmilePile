package com.smilepile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Unified header component for all parent mode screens
 * Shows SmilePile logo on left and View Mode eye icon on right
 */
@Composable
fun AppHeaderComponent(
    onViewModeClick: () -> Unit,
    showViewModeButton: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    // Check if we're in dark theme by comparing the background color
    // In dark theme, background is #1C1B1F (very dark), in light theme it's #FFFBFE (almost white)
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF1C1B1F)

    // Use appropriate background color based on theme
    val headerBackgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(0xFFFAFAFA) // Very light gray, softer than pure white
    }

    // Wrap everything in a column
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Header section with background that extends to safe area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBackgroundColor) // Background extends to top edge
        ) {
            // Inner box with padding for content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Only content is pushed down, not background
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // SmilePile logo on the left
                SmilePileLogo(
                    iconSize = 48.dp,
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // View Mode eye icon on the right
                if (showViewModeButton) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp) // Same size as logo
                            .clip(CircleShape)
                            .clickable { onViewModeClick() },
                        color = Color(0xFF4CAF50) // Solid green background
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveRedEye,
                                contentDescription = "Switch to View Mode",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White // White icon
                            )
                        }
                    }
                }
            }
        }


        // Add subtle bottom border for header separation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (isDarkTheme) {
                        Color.White.copy(alpha = 0.1f)
                    } else {
                        Color.Black.copy(alpha = 0.08f)
                    }
                )
        )

        // Additional content (like category filters) - outside header background
        // This will have the scroll area background color
        content()
    }
}