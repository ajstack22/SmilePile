package com.smilepile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9800),  // SmilePile orange
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),  // Light orange
    onPrimaryContainer = Color(0xFF5D2E00),  // Dark orange
    secondary = Color(0xFF4CAF50),  // SmilePile green
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8E6C9),  // Light green
    onSecondaryContainer = Color(0xFF1B5E20),  // Dark green
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE0E0E0),  // Darker gray for headers/footers
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFFF4EFF4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = Color(0xFFD0BCFF),
    surfaceTint = Color(0xFF6750A4),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),  // Light orange for dark theme
    onPrimary = Color(0xFF5D2E00),
    primaryContainer = Color(0xFFE65100),  // Dark orange container
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFF81C784),  // Light green for dark theme
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),  // Dark green container
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseOnSurface = Color(0xFF1C1B1F),
    inverseSurface = Color(0xFFE6E1E5),
    inversePrimary = Color(0xFF6750A4),
    surfaceTint = Color(0xFFD0BCFF),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
)

@Composable
fun SmilePileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isKidsMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = if (isKidsMode) KidsModeTypography else Typography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}