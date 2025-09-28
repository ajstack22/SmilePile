package com.smilepile.ui.components

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Header bar with logo and view mode button - with background
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

        // Add minimal spacing between branding and categories
        Spacer(modifier = Modifier.height(4.dp))

        // Additional content (like category filters) - OUTSIDE the Surface background
        content()
    }
}