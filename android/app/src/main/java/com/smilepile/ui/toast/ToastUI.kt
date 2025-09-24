package com.smilepile.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Toast UI Component
 * Displays toast notifications at the bottom of the screen
 */
@Composable
fun ToastUI(
    toastState: ToastState,
    modifier: Modifier = Modifier,
    bottomPadding: Int = 100 // Default padding from bottom
) {
    val toast = toastState.currentToast
    val isVisible = toastState.isVisible

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(99999f) // Above all other content
    ) {
        AnimatedVisibility(
            visible = isVisible && toast != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            toast?.let { data ->
                ToastContent(
                    toastData = data,
                    onDismiss = { toastState.hideToast() },
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}

@Composable
private fun ToastContent(
    toastData: ToastData,
    onDismiss: () -> Unit,
    bottomPadding: Int
) {
    val backgroundColor = toastData.backgroundColor ?: getToastColor(toastData.type)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding.dp)
            .clickable { onDismiss() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toast message
            Text(
                text = toastData.message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Optional action button
            toastData.action?.let { action ->
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    onClick = {
                        action.onPress()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = action.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Get default color for toast type
 */
@Composable
private fun getToastColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> Color(0xFF4CAF50) // Green
        ToastType.ERROR -> Color(0xFFF44336) // Red
        ToastType.WARNING -> Color(0xFFFF9800) // Orange
        ToastType.INFO -> Color(0xFF2196F3) // Blue
        ToastType.DEFAULT -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Toast UI for Kids Mode with larger text and simpler styling
 */
@Composable
fun KidsModeToastUI(
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    val toast = toastState.currentToast
    val isVisible = toastState.isVisible

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(99999f)
    ) {
        AnimatedVisibility(
            visible = isVisible && toast != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            toast?.let { data ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 100.dp) // Adjusted for better placement
                        .clickable { toastState.hideToast() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f), // Modern glass effect
                    shadowElevation = 12.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.message,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 18.sp, // Larger for kids
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category-specific toast UI that matches the category chip styling
 * Appears at 25% from top with fade in/out animation
 */
@Composable
fun CategoryToastUI(
    toastState: ToastState,
    categoryColorHex: String?,
    modifier: Modifier = Modifier
) {
    val toast = toastState.currentToast
    val isVisible = toastState.isVisible

    // Parse the category color, fallback to primary if invalid
    val categoryColor = categoryColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    // Calculate if we need light or dark text based on background brightness
    // Using simple luminance formula: 0.299*R + 0.587*G + 0.114*B
    val luminance = (0.299f * categoryColor.red + 0.587f * categoryColor.green + 0.114f * categoryColor.blue)
    val textColor = if (luminance > 0.5f) {
        Color.Black
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(99999f)
    ) {
        AnimatedVisibility(
            visible = isVisible && toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp) // Position at ~10% from top for better visibility
        ) {
            toast?.let { data ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Match the CategoryChip styling exactly, just slightly larger
                    Card(
                        modifier = Modifier
                            .clickable { toastState.hideToast() },
                        colors = CardDefaults.cardColors(
                            containerColor = categoryColor.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = data.message,
                                color = textColor,
                                fontSize = 18.sp, // Slightly larger than category chip
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}