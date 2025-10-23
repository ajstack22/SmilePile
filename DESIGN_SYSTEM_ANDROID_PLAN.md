# Android Design System Implementation Plan
**Platform**: Android (Kotlin/Jetpack Compose)
**Date**: January 2025
**Scope**: Phase 3B - Android Technical Implementation
**Context**: [DESIGN_SYSTEM.md](/Users/adamstack/SmilePile/website/DESIGN_SYSTEM.md) | [Research Report](/Users/adamstack/SmilePile/DESIGN_SYSTEM_IMPLEMENTATION_RESEARCH.md) | [User Stories](/Users/adamstack/SmilePile/DESIGN_SYSTEM_USER_STORIES.md)

---

## Executive Summary

This document provides a **step-by-step technical implementation plan** for migrating SmilePile's Android app to the new design system. The plan transforms the current bright, playful aesthetic (Nunito font, vibrant orange/green colors) into a calming, scientifically-backed design system (Atkinson Hyperlegible font, soft muted tones) specifically tailored for families with special needs children.

**Key Changes**:
- **Font**: Nunito → Atkinson Hyperlegible
- **Colors**: Bright Orange (#FF9800), Green (#4CAF50) → Soft Blue (#7FB3D5), Sage Green (#A8D8B9)
- **Accessibility**: Material3 defaults → WCAG 2.2 AA compliant
- **Typography Scale**: Material Design 3 → Design System scale
- **Components**: Material3 themed → Custom design system composables

**Estimated Effort**: 68 hours (~8.5 working days)
**Files Modified**: 109+ files
**Files Created**: 12+ new component files

---

## Table of Contents

1. [File Inventory](#1-file-inventory)
2. [Font Implementation](#2-font-implementation)
3. [Color System](#3-color-system)
4. [Component Updates](#4-component-updates)
5. [Accessibility Implementation](#5-accessibility-implementation)
6. [Testing Strategy](#6-testing-strategy)
7. [Material3 Customization](#7-material3-customization)
8. [Build Configuration](#8-build-configuration)
9. [Implementation Sequence](#9-implementation-sequence)

---

## 1. File Inventory

### 1.1 Core Theme Files (Must Modify First)

| File | Current State | Changes Required | Priority |
|------|--------------|------------------|----------|
| `/android/app/src/main/java/com/smilepile/ui/theme/Type.kt` | Uses Nunito via Google Fonts | Replace with Atkinson Hyperlegible | P0 |
| `/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt` | Orange/Green Material3 theme | Replace with design system colors | P0 |

### 1.2 Screen Files (16+ files)

**High Priority** (main user flows):
- `/android/app/src/main/java/com/smilepile/ui/screens/MainScreen.kt` - Main gallery
- `/android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt` - Settings
- `/android/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt` - Photo viewing
- `/android/app/src/main/java/com/smilepile/ui/screens/CategoryManagementScreen.kt` - Categories

**Medium Priority** (onboarding):
- `/android/app/src/main/java/com/smilepile/onboarding/OnboardingScreen.kt`
- `/android/app/src/main/java/com/smilepile/onboarding/screens/WelcomeScreen.kt`
- `/android/app/src/main/java/com/smilepile/onboarding/screens/CompletionScreen.kt`

**Low Priority** (secondary features):
- `/android/app/src/main/java/com/smilepile/ui/screens/PhotoEditScreen.kt`
- Kids Mode screens
- Security screens

### 1.3 Component Files (20+ files)

- `/android/app/src/main/java/com/smilepile/ui/components/ErrorStateComponents.kt` - Error handling
- `/android/app/src/main/java/com/smilepile/ui/components/gallery/PhotoStackComponent.kt` - Photo stacks
- Custom buttons throughout screens
- Card components
- Form components

### 1.4 Files to Create

**New Component Directory**:
- `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Buttons.kt`
- `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Cards.kt`
- `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Forms.kt`
- `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Modifiers.kt`

**New Theme Files**:
- `/android/app/src/main/java/com/smilepile/ui/theme/Color.kt` - Centralized color definitions

---

## 2. Font Implementation

### 2.1 Update Type.kt - Replace Nunito with Atkinson Hyperlegible

**File**: `/android/app/src/main/java/com/smilepile/ui/theme/Type.kt`

**Replace Entire File**:
```kotlin
package com.smilepile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.smilepile.R

// Google Fonts provider for Atkinson Hyperlegible
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Atkinson Hyperlegible font family
// Designed by Braille Institute for enhanced readability
private val atkinsonFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Atkinson Hyperlegible"),
        fontProvider = provider,
        weight = FontWeight.Normal // 400
    ),
    Font(
        googleFont = GoogleFont("Atkinson Hyperlegible"),
        fontProvider = provider,
        weight = FontWeight.Bold // 700
    )
)

// MARK: - Design System Typography
// Based on DESIGN_SYSTEM.md specification
// H1: 48sp, H2: 36sp, H3: 28sp, H4: 22sp, H5: 18sp
// Body: 16sp, BodySmall: 14sp, Button: 16sp

val Typography = Typography(
    // H1: 48sp, Bold, Line Height 1.1
    displayLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp, // 48 * 1.1
        letterSpacing = 0.sp
    ),

    // H2: 36sp, Bold, Line Height 1.2
    displayMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 43.2.sp, // 36 * 1.2
        letterSpacing = 0.sp
    ),

    // H3: 28sp, Bold, Line Height 1.3
    displaySmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.4.sp, // 28 * 1.3
        letterSpacing = 0.sp
    ),

    // H4: 22sp, Bold, Line Height 1.4
    headlineLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.8.sp, // 22 * 1.4
        letterSpacing = 0.sp
    ),

    // H5: 18sp, Bold, Line Height 1.4
    headlineMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 25.2.sp, // 18 * 1.4
        letterSpacing = 0.sp
    ),

    // Subheading: 18sp, Regular
    headlineSmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.8.sp, // 18 * 1.6
        letterSpacing = 0.sp
    ),

    // Title: 16sp, Bold
    titleLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 25.6.sp, // 16 * 1.6
        letterSpacing = 0.sp
    ),

    // Body Large: 18sp, Regular, Line Height 1.6
    titleMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.8.sp, // 18 * 1.6
        letterSpacing = 0.sp
    ),

    // Body: 16sp, Regular, Line Height 1.6 (DEFAULT)
    bodyLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp, // 16 * 1.6
        letterSpacing = 0.sp
    ),

    // Body Medium: 16sp (same as bodyLarge for consistency)
    bodyMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp,
        letterSpacing = 0.sp
    ),

    // Body Small: 14sp, Regular, Line Height 1.5
    bodySmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp, // 14 * 1.5
        letterSpacing = 0.sp
    ),

    // Button: 16sp, Bold, Line Height 1.0
    labelLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 16.sp, // 16 * 1.0
        letterSpacing = 0.sp
    ),

    // Label: 14sp, Bold, Line Height 1.2
    labelMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 16.8.sp, // 14 * 1.2
        letterSpacing = 0.sp
    ),

    // Small Label: 12sp, Bold
    labelSmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.4.sp, // 12 * 1.2
        letterSpacing = 0.sp
    )
)

// MARK: - Kids Mode Typography (20% larger)
val KidsModeTypography = Typography(
    // H1 Kids: 58sp (48 * 1.2)
    displayLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 63.8.sp,
        letterSpacing = 0.sp
    ),

    // H2 Kids: 43sp (36 * 1.2)
    displayMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 43.sp,
        lineHeight = 51.6.sp,
        letterSpacing = 0.sp
    ),

    // H3 Kids: 34sp (28 * 1.2)
    displaySmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 44.2.sp,
        letterSpacing = 0.sp
    ),

    // H4 Kids: 26sp (22 * 1.2)
    headlineLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.4.sp,
        letterSpacing = 0.sp
    ),

    // Body Large Kids: 22sp (18 * 1.2)
    headlineMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 35.2.sp,
        letterSpacing = 0.sp
    ),

    // Body Kids: 19sp (16 * 1.2)
    bodyLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 30.4.sp,
        letterSpacing = 0.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 30.4.sp,
        letterSpacing = 0.sp
    ),

    // Button Kids: 19sp (16 * 1.2)
    labelLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),

    // Keep smaller elements same size
    bodySmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),

    labelMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.sp
    ),

    labelSmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.4.sp,
        letterSpacing = 0.sp
    ),

    // Copy remaining from standard typography
    headlineSmall = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.8.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 25.6.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.8.sp,
        letterSpacing = 0.sp
    )
)
```

**Key Changes**:
1. Replaced "Nunito" with "Atkinson Hyperlegible"
2. Reduced to 2 font weights (Regular 400, Bold 700)
3. Updated to design system typography scale (48sp, 36sp, 28sp, 22sp, 18sp, 16sp, 14sp)
4. Applied correct line heights (1.1 to 1.6 based on element type)
5. Maintained Kids Mode variant (20% larger)

### 2.2 Migration Strategy

**Find & Replace** across all files:
```kotlin
// OLD → NEW (Material3 mappings to Design System)
MaterialTheme.typography.displayLarge → MaterialTheme.typography.displayLarge (H1)
MaterialTheme.typography.headlineLarge → MaterialTheme.typography.headlineLarge (H4)
MaterialTheme.typography.bodyLarge → MaterialTheme.typography.bodyLarge (Body 16sp)
MaterialTheme.typography.labelLarge → MaterialTheme.typography.labelLarge (Button)
```

**Verification**:
```bash
# Search for remaining Nunito references
grep -r "Nunito" android/app/src/main --include="*.kt"
# Should return 0 results
```

---

## 3. Color System

### 3.1 Create Color.kt - Centralized Color Definitions

**Create New File**: `/android/app/src/main/java/com/smilepile/ui/theme/Color.kt`

```kotlin
package com.smilepile.ui.theme

import androidx.compose.ui.graphics.Color

// MARK: - Design System Colors
// Based on DESIGN_SYSTEM.md - scientifically-backed palette
// designed to reduce anxiety and sensory overload

/**
 * Soft Blue (#7FB3D5) - Primary brand color
 * Usage: Primary buttons, headers, key CTAs, links
 * Psychology: Trust, empathy, calm, stability
 * Contrast: 4.6:1 against Soft Charcoal (WCAG AA)
 */
val SoftBlue = Color(0xFF7FB3D5)

/**
 * Sage Green (#A8D8B9) - Secondary color
 * Usage: Success states, growth indicators, secondary elements
 * Psychology: Refreshing, health-promoting, concentration
 * Contrast: 5.2:1 against Soft Charcoal (WCAG AA)
 */
val SageGreen = Color(0xFFA8D8B9)

/**
 * Lavender (#C9B3D6) - Accent color
 * Usage: Highlights, special features, gentle accents
 * Psychology: Calming to nervous system, relaxation, creativity
 * Contrast: 4.9:1 against Soft Charcoal (WCAG AA)
 */
val Lavender = Color(0xFFC9B3D6)

/**
 * Warm Cream (#F8F3ED) - Neutral light
 * Usage: Backgrounds, cards, containers
 * Psychology: Comforting, approachable, warm
 * Note: Avoid pure white (#FFFFFF) - too harsh
 */
val WarmCream = Color(0xFFF8F3ED)

/**
 * Soft Charcoal (#3A3A3A) - Neutral dark
 * Usage: Body text, dark UI elements
 * Psychology: Professional, readable, grounded
 * Contrast: 12.8:1 against Warm Cream (WCAG AAA)
 */
val SoftCharcoal = Color(0xFF3A3A3A)

// MARK: - Supporting Colors

/**
 * Gentle Gold (#F5DA81) - Celebrations
 * Usage: Achievements, warmth (use sparingly)
 */
val GentleGold = Color(0xFFF5DA81)

/**
 * Soft Coral (#F4A6A3) - Gentle alerts
 * Usage: Love/family themes, gentle alerts
 * Warning: Not for errors - too gentle
 */
val SoftCoral = Color(0xFFF4A6A3)

/**
 * Pale Mint (#B8DCD6) - Fresh sections
 * Usage: New content indicators, fresh sections
 */
val PaleMint = Color(0xFFB8DCD6)

// MARK: - Dark Mode Palette

/**
 * Soft Black (#1E1E1E) - Dark mode background
 */
val SoftBlack = Color(0xFF1E1E1E)

/**
 * Elevated Surface (#2A2A2A) - Dark mode surface
 */
val ElevatedSurface = Color(0xFF2A2A2A)

/**
 * Brighter Soft Blue (#A8CEEA) - Dark mode primary
 */
val SoftBlueDark = Color(0xFFA8CEEA)

/**
 * Brighter Sage Green (#C1E8CF) - Dark mode secondary
 */
val SageGreenDark = Color(0xFFC1E8CF)

/**
 * Brighter Lavender (#D8C5E5) - Dark mode accent
 */
val LavenderDark = Color(0xFFD8C5E5)

// MARK: - Category/Pile Colors (Soft Variants)
val CategoryColors = listOf(
    SageGreen,      // Sage Green
    SoftBlue,       // Soft Blue
    Lavender,       // Lavender
    PaleMint,       // Pale Mint
    GentleGold,     // Gentle Gold
    SoftCoral,      // Soft Coral
    Color(0xFFE5C9A0), // Soft Tan
    Color(0xFFD4A5C9)  // Soft Mauve
)

// MARK: - Semantic Colors

/**
 * Error color (softer than traditional red)
 */
val ErrorColor = Color(0xFFDC8686) // Muted red

/**
 * Focus indicator color (WCAG 2.2 AA)
 * 3dp solid outline, 2dp offset
 */
val FocusIndicator = SoftBlue
```

### 3.2 Update ComposeTheme.kt

**File**: `/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`

**Replace Color Schemes**:
```kotlin
package com.smilepile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Color Scheme - Design System
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = SoftBlue,
    onPrimary = WarmCream,
    primaryContainer = SoftBlue.copy(alpha = 0.1f),
    onPrimaryContainer = SoftCharcoal,

    // Secondary colors
    secondary = SageGreen,
    onSecondary = SoftCharcoal,
    secondaryContainer = SageGreen.copy(alpha = 0.1f),
    onSecondaryContainer = SoftCharcoal,

    // Tertiary colors
    tertiary = Lavender,
    onTertiary = SoftCharcoal,
    tertiaryContainer = Lavender.copy(alpha = 0.1f),
    onTertiaryContainer = SoftCharcoal,

    // Error colors
    error = ErrorColor,
    errorContainer = ErrorColor.copy(alpha = 0.1f),
    onError = Color.White,
    onErrorContainer = SoftCharcoal,

    // Background colors
    background = WarmCream,
    onBackground = SoftCharcoal,

    // Surface colors
    surface = Color.White,
    onSurface = SoftCharcoal,
    surfaceVariant = Color(0xFFF0F0F0), // Light gray for elevated surfaces
    onSurfaceVariant = SoftCharcoal.copy(alpha = 0.7f),

    // Outline colors
    outline = Color.Gray.copy(alpha = 0.3f),
    outlineVariant = Color.Gray.copy(alpha = 0.1f),

    // Inverse colors
    inverseSurface = SoftCharcoal,
    inverseOnSurface = WarmCream,
    inversePrimary = SoftBlueDark,

    // Other
    surfaceTint = SoftBlue,
    scrim = Color.Black.copy(alpha = 0.5f)
)

// Dark Color Scheme - Design System
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = SoftBlueDark,
    onPrimary = SoftBlack,
    primaryContainer = SoftBlueDark.copy(alpha = 0.2f),
    onPrimaryContainer = WarmCream,

    // Secondary colors
    secondary = SageGreenDark,
    onSecondary = SoftBlack,
    secondaryContainer = SageGreenDark.copy(alpha = 0.2f),
    onSecondaryContainer = WarmCream,

    // Tertiary colors
    tertiary = LavenderDark,
    onTertiary = SoftBlack,
    tertiaryContainer = LavenderDark.copy(alpha = 0.2f),
    onTertiaryContainer = WarmCream,

    // Error colors
    error = ErrorColor,
    errorContainer = ErrorColor.copy(alpha = 0.2f),
    onError = SoftBlack,
    onErrorContainer = WarmCream,

    // Background colors
    background = SoftBlack,
    onBackground = WarmCream,

    // Surface colors
    surface = ElevatedSurface,
    onSurface = WarmCream,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = WarmCream.copy(alpha = 0.7f),

    // Outline colors
    outline = Color.Gray.copy(alpha = 0.5f),
    outlineVariant = Color.Gray.copy(alpha = 0.2f),

    // Inverse colors
    inverseSurface = WarmCream,
    inverseOnSurface = SoftBlack,
    inversePrimary = SoftBlue,

    // Other
    surfaceTint = SoftBlueDark,
    scrim = Color.Black.copy(alpha = 0.7f)
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
```

**Key Changes**:
1. Primary: Orange (#FF9800) → Soft Blue (#7FB3D5)
2. Secondary: Green (#4CAF50) → Sage Green (#A8D8B9)
3. Tertiary: Purple → Lavender (#C9B3D6)
4. Background: Pure white → Warm Cream (#F8F3ED)
5. Text: Dark gray → Soft Charcoal (#3A3A3A)
6. Dark mode palette updated with brighter variants

### 3.3 Color Migration Map

| Old Color | Old Hex | New Color | New Hex | Material3 Slot |
|-----------|---------|-----------|---------|----------------|
| Orange | 0xFFFF9800 | Soft Blue | 0xFF7FB3D5 | `primary` |
| Green | 0xFF4CAF50 | Sage Green | 0xFFA8D8B9 | `secondary` |
| Purple | 0xFF7D5260 | Lavender | 0xFFC9B3D6 | `tertiary` |
| White | 0xFFFFFBFE | Warm Cream | 0xFFF8F3ED | `background` |
| Dark Gray | 0xFF1C1B1F | Soft Charcoal | 0xFF3A3A3A | `onBackground` |

---

## 4. Component Updates

### 4.1 Create Design System Button Components

**Create**: `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Buttons.kt`

```kotlin
package com.smilepile.ui.components.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smilepile.ui.theme.*

/**
 * Primary button following design system
 * Background: Soft Blue, Text: Warm Cream, Min Height: 48dp
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth(),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Secondary button following design system
 * Border: Soft Blue, Background: Transparent, Text: Soft Blue
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Text button (tertiary)
 * No border, no background, text color
 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// MARK: - Previews
@Preview(showBackground = true)
@Composable
fun ButtonsPreview() {
    SmilePileTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryButton(text = "Download Now", onClick = {})
            PrimaryButton(text = "Loading...", onClick = {}, isLoading = true)
            PrimaryButton(text = "Disabled", onClick = {}, enabled = false)

            SecondaryButton(text = "Learn More", onClick = {})
            SecondaryButton(text = "Disabled", onClick = {}, enabled = false)

            TextButton(text = "Cancel", onClick = {})
            TextButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
```

### 4.2 Create Card Components

**Create**: `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Cards.kt`

```kotlin
package com.smilepile.ui.components.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smilepile.ui.theme.SmilePileTheme

/**
 * Standard card component following design system
 * Background: White, Border: 1dp gray, Radius: 12dp, Shadow
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

// MARK: - Preview
@Preview(showBackground = true)
@Composable
fun StandardCardPreview() {
    SmilePileTheme {
        StandardCard(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Card Title",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Card description text goes here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
```

### 4.3 Create Form Input Component

**Create**: `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Forms.kt`

```kotlin
package com.smilepile.ui.components.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smilepile.ui.theme.*

/**
 * Form input component following design system
 * Min Height: 48dp, Focus: Soft Blue border
 */
@Composable
fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label with required indicator
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isRequired) {
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Input field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            enabled = enabled,
            isError = errorMessage != null,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusIndicator,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        // Error message
        if (errorMessage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// MARK: - Preview
@Preview(showBackground = true)
@Composable
fun FormInputPreview() {
    SmilePileTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FormInput(
                value = "",
                onValueChange = {},
                label = "Full Name",
                placeholder = "John Doe"
            )

            FormInput(
                value = "",
                onValueChange = {},
                label = "Email",
                isRequired = true
            )

            FormInput(
                value = "invalid",
                onValueChange = {},
                label = "Email",
                errorMessage = "Please enter a valid email address"
            )
        }
    }
}
```

---

## 5. Accessibility Implementation

### 5.1 Touch Target Enforcement

**Create**: `/android/app/src/main/java/com/smilepile/ui/components/designsystem/Modifiers.kt`

```kotlin
package com.smilepile.ui.components.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Enforces minimum 48dp touch target (WCAG 2.5.8)
 */
fun Modifier.touchTarget(minSize: Int = 48) = this.size(minSize.dp)

/**
 * Clickable modifier with accessibility support
 * Ensures 48dp minimum touch target
 */
fun Modifier.accessibleClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        indication = rememberRipple(),
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    ).touchTarget()
}
```

### 5.2 Reduced Motion Support

Add to Modifiers.kt:
```kotlin
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * Check if user has reduced motion preference enabled
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale == 0f
}

/**
 * Animation spec that respects reduced motion preference
 */
@Composable
fun <T> reducedMotionAnimation(
    default: AnimationSpec<T>,
    reduced: AnimationSpec<T> = tween(durationMillis = 1)
): AnimationSpec<T> {
    return if (isReducedMotionEnabled()) reduced else default
}
```

### 5.3 TalkBack Support Checklist

**Audit all composables**:
```kotlin
// ✅ All images have contentDescription
Image(
    painter = painterResource(R.drawable.photo),
    contentDescription = "Family photo at birthday party"
)

// ✅ Icon buttons have contentDescription
IconButton(
    onClick = {},
    modifier = Modifier.semantics { contentDescription = "Add photo" }
) {
    Icon(Icons.Default.Add, contentDescription = null)
}

// ✅ Form inputs have labels (not placeholder-only)
FormInput(
    value = email,
    onValueChange = { email = it },
    label = "Email" // ✅ Associated label
)

// ✅ Error messages announced
if (errorMessage != null) {
    Text(
        text = errorMessage,
        modifier = Modifier.semantics {
            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
        }
    )
}
```

---

## 6. Testing Strategy

### 6.1 Build Commands

**Clean Build**:
```bash
cd android
./gradlew clean
```

**Build Debug (QUAL Tier)**:
```bash
./gradlew assembleQualDebug
```

**Build and Install**:
```bash
./gradlew installQualDebug
```

**Run on Emulator**:
```bash
adb install -r app/build/outputs/apk/qual/debug/app-qual-debug.apk
adb shell am start -n com.smilepile.qual/.MainActivity
```

### 6.2 TalkBack Testing

**Enable TalkBack**:
1. Settings > Accessibility > TalkBack
2. Turn on TalkBack
3. Use gestures:
   - Swipe right: Next element
   - Swipe left: Previous element
   - Double-tap: Activate

**Verification Checklist**:
- [ ] All buttons announced as "Button name, Button"
- [ ] All images announced with contentDescription
- [ ] Form labels announced before input
- [ ] Error messages announced when shown
- [ ] Navigation follows logical order

### 6.3 Contrast Validation

**Automated Testing** (using Accessibility Scanner):
```bash
# Install Accessibility Scanner from Play Store
# Run app, capture screen, analyze
```

**Manual Verification**:
| Background | Foreground | Ratio | Pass |
|------------|------------|-------|------|
| Warm Cream | Soft Charcoal | 12.8:1 | ✅ AAA |
| Soft Blue | Soft Charcoal | 4.6:1 | ✅ AA |
| Sage Green | Soft Charcoal | 5.2:1 | ✅ AA |

### 6.4 Manual Testing Checklist

**Visual Regression**:
- [ ] All screens rendered with Atkinson Hyperlegible
- [ ] All buttons use Soft Blue (not orange)
- [ ] Background is Warm Cream (not pure white)
- [ ] No bright colors visible
- [ ] Dark mode works correctly

**Accessibility**:
- [ ] All touch targets minimum 48dp
- [ ] Focus indicators visible
- [ ] TalkBack announces correctly
- [ ] Reduced motion respected
- [ ] Contrast ratios pass

**Cross-Device**:
- [ ] Pixel 6 (standard)
- [ ] Tablet (large screen)
- [ ] Light mode
- [ ] Dark mode

---

## 7. Material3 Customization

### 7.1 Override Material Defaults

Material3 provides excellent defaults, but we need to customize colors and shapes to match the design system.

**Shape Customization** (Optional):
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),   // Buttons, chips
    medium = RoundedCornerShape(12.dp), // Cards
    large = RoundedCornerShape(16.dp)   // Bottom sheets, dialogs
)

// Update SmilePileTheme
MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = Shapes, // Add this
    content = content
)
```

### 7.2 Custom Component Defaults

Use Material3 components but override colors:
```kotlin
// Example: Custom Button colors
Button(
    onClick = {},
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary, // Soft Blue
        contentColor = MaterialTheme.colorScheme.onPrimary  // Warm Cream
    )
)

// Example: Custom Card colors
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface, // White
        contentColor = MaterialTheme.colorScheme.onSurface  // Soft Charcoal
    )
)
```

---

## 8. Build Configuration

### 8.1 Gradle Dependencies

**Verify** in `/android/app/build.gradle.kts`:
```kotlin
dependencies {
    // Material3
    implementation("androidx.compose.material3:material3:1.2.0")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    // Compose UI
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
}
```

**No changes needed** - existing dependencies support design system.

### 8.2 Font Certificates

**File**: `/android/app/src/main/res/values/font_certs.xml`

Already exists for Google Fonts. No changes needed.

### 8.3 Build Variants

**QUAL Tier** (for testing):
```bash
./gradlew assembleQualDebug
```

**PROD Tier** (production):
```bash
./gradlew assembleProdRelease
```

---

## 9. Implementation Sequence

### Phase 1: Foundation (Day 1-2, 12 hours)

**Day 1 Morning (4 hours)**:
1. Create Color.kt with all design system colors
2. Update ComposeTheme.kt with new color schemes
3. Build and verify compilation

**Day 1 Afternoon (4 hours)**:
4. Update Type.kt - replace Nunito with Atkinson Hyperlegible
5. Update typography scale to design system values
6. Build and verify font loads

**Day 2 (4 hours)**:
7. Test app with new fonts and colors
8. Fix any immediate visual issues
9. Verify light and dark modes

**Deliverable**: Font and colors working, no Nunito references

---

### Phase 2: Components (Day 3-4, 16 hours)

**Day 3 (8 hours)**:
1. Create Buttons.kt (Primary, Secondary, Text)
2. Create Cards.kt (StandardCard)
3. Create Forms.kt (FormInput)
4. Create Modifiers.kt (touchTarget, accessibleClickable)

**Day 4 (8 hours)**:
5. Migrate 10+ screen files to use new components
6. Update MainScreen.kt, SettingsScreen.kt
7. Update onboarding screens
8. Visual QA

**Deliverable**: Component library created, key screens migrated

---

### Phase 3: Accessibility (Day 5-6, 16 hours)

**Day 5 (8 hours)**:
1. Add contentDescription to all images
2. Add semantic labels to all buttons
3. Ensure 48dp touch targets
4. Implement reduced motion support

**Day 6 (8 hours)**:
5. TalkBack testing on all screens
6. Fix announced text issues
7. Validate contrast ratios
8. Fix failing combinations

**Deliverable**: WCAG 2.2 AA compliant, TalkBack tested

---

### Phase 4: Remaining Screens (Day 7, 8 hours)

1. Update remaining 30+ screen files
2. Migrate Kids Mode screens
3. Update photo editor screens
4. Visual QA all screens

**Deliverable**: All screens using design system

---

### Phase 5: Testing & QA (Day 8, 8 hours)

1. Manual testing on Pixel 6, Tablet
2. TalkBack full app walkthrough
3. Reduced motion testing
4. Performance testing

**Deliverable**: Production-ready

---

### Phase 6: Deployment (Day 9, 8 hours)

1. Final code review
2. Update release notes
3. Build QUAL tier
4. Deploy and monitor

**Deliverable**: Design system live in QUAL

---

## Validation Checklist

### Code Validation
- [ ] No "Nunito" references (`grep -r "Nunito" android/`)
- [ ] No orange (#FF9800) or green (#4CAF50) references
- [ ] Build succeeds: `./gradlew assembleQualDebug`
- [ ] 0 errors, 0 warnings

### Visual Validation
- [ ] Atkinson Hyperlegible renders correctly
- [ ] Colors match design system (Soft Blue, Sage Green, etc.)
- [ ] Dark mode contrast ratios pass
- [ ] No jarring visual changes

### Accessibility Validation
- [ ] All touch targets 48dp minimum
- [ ] TalkBack announces correctly
- [ ] Reduced motion respected
- [ ] Contrast ratios >= 4.5:1

### Functional Validation
- [ ] All user flows work
- [ ] Kids Mode maintains larger typography
- [ ] Forms validate properly
- [ ] Buttons respond correctly

---

## Success Metrics

**Quantitative**:
- [ ] 0 Nunito references
- [ ] 100% color migration
- [ ] 0 build errors
- [ ] 100% WCAG AA compliance

**Qualitative**:
- [ ] Design system visually cohesive
- [ ] TalkBack experience improved
- [ ] Developer velocity improved with components

---

## Risk Mitigation

### High Risk: Font Loading Performance
**Mitigation**:
- Google Fonts provider handles caching
- Monitor app launch time
- Fallback to system font if needed

### Medium Risk: Material3 Component Conflicts
**Mitigation**:
- Test all Material3 components
- Override colors as needed
- Document customizations

### Low Risk: User Confusion
**Mitigation**:
- Gradual rollout (QUAL → PROD)
- "What's New" screen
- Keep navigation identical

---

## Next Steps

After Android implementation:
1. **Website Implementation** (Phase 3C)
2. **Cross-Platform QA**
3. **User Testing**
4. **Production Rollout**

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Author**: Atlas Developer Agent
**Review Status**: Ready for Implementation
