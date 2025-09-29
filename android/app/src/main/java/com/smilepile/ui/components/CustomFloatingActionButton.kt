package com.smilepile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Custom Floating Action Button that matches iOS implementation exactly
 * - Size: 56dp x 56dp
 * - Shape: Rounded rectangle with 12dp corners
 * - Icon size: 24dp
 * - Pulse animation: 1.0 -> 1.1 scale over 1 second
 */
@Composable
fun CustomFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    isPulsing: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Pulse animation using InfiniteTransition for proper infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "FAB pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPulsing) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse scale"
    )

    // Press animation scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "press scale"
    )

    // Combine scales: apply pulse only when isPulsing is true
    val finalScale = if (isPulsing) pulseScale * pressScale else pressScale

    Box(
        modifier = modifier
            .size(56.dp) // Fixed size matching iOS
            .scale(finalScale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(12.dp)) // Rounded rectangle shape matching iOS
            .background(
                color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = Color.White),
                enabled = enabled,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp), // Fixed icon size matching iOS
            tint = Color.White
        )
    }
}