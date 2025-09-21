package com.smilepile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Error state UI components with retry mechanisms and contextual messaging.
 * These components provide graceful error handling and recovery options.
 */

/**
 * Generic error state component with retry functionality.
 *
 * @param errorMessage The error message to display
 * @param onRetry Callback for retry action
 * @param modifier Modifier for styling
 * @param errorType Type of error for appropriate icon selection
 * @param showRetry Whether to show the retry button
 */
@Composable
fun ErrorState(
    errorMessage: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.GENERIC,
    showRetry: Boolean = true
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = errorType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = errorType.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                if (showRetry && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

/**
 * Compact error state for inline use (e.g., in lists or grids).
 */
@Composable
fun CompactErrorState(
    errorMessage: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        onRetry?.let {
            TextButton(onClick = it) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Error boundary wrapper that catches and displays errors gracefully.
 */
@Composable
fun ErrorBoundary(
    onError: (Throwable) -> Unit = {},
    fallback: @Composable (Throwable) -> Unit = { error ->
        ErrorState(
            errorMessage = "Something went wrong: ${error.message}",
            onRetry = null,
            modifier = Modifier.fillMaxSize()
        )
    },
    content: @Composable () -> Unit
) {
    // Error boundaries can't use try-catch with @Composable functions
    // This would need to be implemented at a higher level with proper error handling
    content()
}

/**
 * Image loading error state specifically for failed image loads.
 */
@Composable
fun ImageLoadError(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Failed to load image",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Failed to load",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            onRetry?.let {
                TextButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Error types with associated icons and titles.
 */
enum class ErrorType(
    val icon: ImageVector,
    val title: String
) {
    GENERIC(Icons.Default.Error, "Something went wrong"),
    NETWORK(Icons.Default.WifiOff, "Network Error"),
    PERMISSION(Icons.Default.Warning, "Permission Required"),
    STORAGE(Icons.Default.Warning, "Storage Error")
}