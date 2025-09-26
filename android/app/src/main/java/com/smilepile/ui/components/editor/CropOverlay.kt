package com.smilepile.ui.components.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Crop overlay component for photo editing.
 * Atlas Lite: simple, pragmatic, under 250 lines.
 */
@Composable
fun CropOverlay(
    imageWidth: Float,
    imageHeight: Float,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    initialCropRect: Rect? = null
) {
    val density = LocalDensity.current

    // State for the crop rectangle (in normalized 0-1 coordinates)
    var normalizedCropRect by remember(initialCropRect, imageWidth, imageHeight) {
        mutableStateOf(
            initialCropRect?.let { rect ->
                Rect(
                    offset = Offset(rect.left / imageWidth, rect.top / imageHeight),
                    size = Size(rect.width / imageWidth, rect.height / imageHeight)
                )
            } ?: Rect(
                offset = Offset(0f, 0f),  // Default to full image
                size = Size(1f, 1f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate display dimensions maintaining aspect ratio
            val imageAspectRatio = imageWidth / imageHeight
            val canvasAspectRatio = canvasWidth / canvasHeight

            val (displayWidth, displayHeight) = if (imageAspectRatio > canvasAspectRatio) {
                canvasWidth to (canvasWidth / imageAspectRatio)
            } else {
                (canvasHeight * imageAspectRatio) to canvasHeight
            }

            // Calculate offsets to center the image
            val offsetX = (canvasWidth - displayWidth) / 2f
            val offsetY = (canvasHeight - displayHeight) / 2f

            // Convert normalized crop rect to display coordinates
            val cropRect = Rect(
                offset = Offset(
                    offsetX + normalizedCropRect.left * displayWidth,
                    offsetY + normalizedCropRect.top * displayHeight
                ),
                size = Size(
                    normalizedCropRect.width * displayWidth,
                    normalizedCropRect.height * displayHeight
                )
            )

            // Log for debugging
            android.util.Log.d("SmilePile", "Crop: canvas=${canvasWidth}x${canvasHeight}, display=${displayWidth}x${displayHeight}, offset=${offsetX}x${offsetY}")

            // Draw darkened overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset.Zero,
                size = size
            )

            // Clear the crop area
            clipRect(
                left = cropRect.left,
                top = cropRect.top,
                right = cropRect.right,
                bottom = cropRect.bottom
            ) {
                drawRect(
                    color = Color.Black.copy(alpha = 0f),
                    topLeft = cropRect.topLeft,
                    size = cropRect.size
                )
            }

            // Draw crop border
            drawRect(
                color = Color.White,
                topLeft = cropRect.topLeft,
                size = cropRect.size,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw grid lines
            val thirdWidth = cropRect.width / 3f
            val thirdHeight = cropRect.height / 3f

            for (i in 1..2) {
                // Vertical lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cropRect.left + thirdWidth * i, cropRect.top),
                    end = Offset(cropRect.left + thirdWidth * i, cropRect.bottom),
                    strokeWidth = 1.dp.toPx()
                )
                // Horizontal lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cropRect.left, cropRect.top + thirdHeight * i),
                    end = Offset(cropRect.right, cropRect.top + thirdHeight * i),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw corner handles
            val handleRadius = 24.dp.toPx()
            val handleColor = Color.White

            // Top-left
            drawCircle(
                color = handleColor,
                radius = handleRadius / 2,
                center = cropRect.topLeft
            )

            // Top-right
            drawCircle(
                color = handleColor,
                radius = handleRadius / 2,
                center = Offset(cropRect.right, cropRect.top)
            )

            // Bottom-left
            drawCircle(
                color = handleColor,
                radius = handleRadius / 2,
                center = Offset(cropRect.left, cropRect.bottom)
            )

            // Bottom-right
            drawCircle(
                color = handleColor,
                radius = handleRadius / 2,
                center = Offset(cropRect.right, cropRect.bottom)
            )
        }

        // Drag gesture handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()

                        // Calculate display dimensions
                        val imageAspectRatio = imageWidth / imageHeight
                        val canvasAspectRatio = canvasWidth / canvasHeight

                        val (displayWidth, displayHeight) = if (imageAspectRatio > canvasAspectRatio) {
                            canvasWidth to (canvasWidth / imageAspectRatio)
                        } else {
                            (canvasHeight * imageAspectRatio) to canvasHeight
                        }

                        val offsetX = (canvasWidth - displayWidth) / 2f
                        val offsetY = (canvasHeight - displayHeight) / 2f

                        // Convert touch position to normalized coordinates
                        val normalizedX = (change.position.x - offsetX) / displayWidth
                        val normalizedY = (change.position.y - offsetY) / displayHeight

                        // Determine which handle is being dragged (simplified - just check corners)
                        val threshold = 0.1f // 10% threshold for corner detection

                        val newRect = when {
                            // Top-left corner
                            normalizedX < normalizedCropRect.left + threshold &&
                            normalizedY < normalizedCropRect.top + threshold -> {
                                val newLeft = max(0f, min(normalizedX, normalizedCropRect.right - 0.1f))
                                val newTop = max(0f, min(normalizedY, normalizedCropRect.bottom - 0.1f))
                                Rect(
                                    offset = Offset(newLeft, newTop),
                                    size = Size(
                                        normalizedCropRect.right - newLeft,
                                        normalizedCropRect.bottom - newTop
                                    )
                                )
                            }
                            // Top-right corner
                            normalizedX > normalizedCropRect.right - threshold &&
                            normalizedY < normalizedCropRect.top + threshold -> {
                                val newRight = min(1f, max(normalizedX, normalizedCropRect.left + 0.1f))
                                val newTop = max(0f, min(normalizedY, normalizedCropRect.bottom - 0.1f))
                                Rect(
                                    offset = Offset(normalizedCropRect.left, newTop),
                                    size = Size(
                                        newRight - normalizedCropRect.left,
                                        normalizedCropRect.bottom - newTop
                                    )
                                )
                            }
                            // Bottom-left corner
                            normalizedX < normalizedCropRect.left + threshold &&
                            normalizedY > normalizedCropRect.bottom - threshold -> {
                                val newLeft = max(0f, min(normalizedX, normalizedCropRect.right - 0.1f))
                                val newBottom = min(1f, max(normalizedY, normalizedCropRect.top + 0.1f))
                                Rect(
                                    offset = Offset(newLeft, normalizedCropRect.top),
                                    size = Size(
                                        normalizedCropRect.right - newLeft,
                                        newBottom - normalizedCropRect.top
                                    )
                                )
                            }
                            // Bottom-right corner
                            normalizedX > normalizedCropRect.right - threshold &&
                            normalizedY > normalizedCropRect.bottom - threshold -> {
                                val newRight = min(1f, max(normalizedX, normalizedCropRect.left + 0.1f))
                                val newBottom = min(1f, max(normalizedY, normalizedCropRect.top + 0.1f))
                                Rect(
                                    offset = Offset(normalizedCropRect.left, normalizedCropRect.top),
                                    size = Size(
                                        newRight - normalizedCropRect.left,
                                        newBottom - normalizedCropRect.top
                                    )
                                )
                            }
                            else -> normalizedCropRect
                        }

                        normalizedCropRect = newRect

                        // Convert back to image coordinates and report
                        val imageCropRect = Rect(
                            offset = Offset(
                                newRect.left * imageWidth,
                                newRect.top * imageHeight
                            ),
                            size = Size(
                                newRect.width * imageWidth,
                                newRect.height * imageHeight
                            )
                        )
                        onCropRectChange(imageCropRect)
                    }
                }
        )
    }
}