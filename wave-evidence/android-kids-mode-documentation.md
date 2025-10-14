# Android Kids Gallery Mode Implementation - Comprehensive Documentation

**Date**: 2025-10-09
**Purpose**: Reference documentation for iOS kids mode implementation
**Author**: Research Agent (ATLAS Phase 1)

---

## Table of Contents

1. [Overview](#overview)
2. [Chip Color Implementation](#chip-color-implementation)
3. [Safe Area/Insets Handling](#safe-areainsets-handling)
4. [Lock Icon to Close Kids Mode](#lock-icon-to-close-kids-mode)
5. [Overall Kids Gallery Mode Structure](#overall-kids-gallery-mode-structure)
6. [Complete Code Reference](#complete-code-reference)

---

## Overview

The Android Kids Gallery Mode provides a simplified, child-friendly interface for browsing photos. The implementation uses Jetpack Compose with Material3 theming and consists of several key components:

- **Main Screen**: `KidsModeGalleryScreen.kt`
- **Category Filter Component**: `CategoryFilterComponent.kt` (shared with parent mode)
- **Kids Mode Variant**: `CategoryFilterComponentKidsMode` (wrapper for kids-specific behavior)
- **Theme Support**: Adapts to light/dark theme automatically

---

## Chip Color Implementation

### 1. Location and Files

**Primary Files**:
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/components/gallery/CategoryFilterComponent.kt`
  - Lines 119-210: `CategoryChip` composable
  - Lines 262-283: `CategoryFilterComponentKidsMode` wrapper

**Theme File**:
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`
  - Lines 10-72: Color scheme definitions

### 2. Chip Styling in Kids Mode vs Normal Mode

**Kids Mode and Parent Mode use the SAME chip component** (`CategoryChip`), but Kids Mode:
- Hides the "All Photos" chip (`showAllChip = false`)
- Prevents deselection (always keeps a category selected)
- Uses different padding in the wrapper

### 3. Exact Color Values and Implementation

#### Light Theme Colors (Default)

```kotlin
// From ComposeTheme.kt, lines 10-40
private val LightColorScheme = lightColorScheme(
    background = Color(0xFFFFFBFE),        // White-ish background
    onBackground = Color(0xFF1C1B1F),      // Dark text
    surface = Color(0xFFFFFBFE),           // White-ish surface
    onSurface = Color(0xFF1C1B1F),         // Dark text on surface
    surfaceVariant = Color(0xFFE0E0E0),    // Darker gray for headers/footers
    onSurfaceVariant = Color(0xFF49454F),  // Muted text
    primary = Color(0xFFFF9800),           // SmilePile orange
    onPrimary = Color(0xFFFFFFFF),         // White
)
```

#### Dark Theme Colors

```kotlin
// From ComposeTheme.kt, lines 42-72
private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF1C1B1F),        // Dark background
    onBackground = Color(0xFFE6E1E5),      // Light text
    surface = Color(0xFF1C1B1F),           // Dark surface
    onSurface = Color(0xFFE6E1E5),         // Light text on surface
    surfaceVariant = Color(0xFF49454F),    // Lighter gray for headers
    onSurfaceVariant = Color(0xFFCAC4D0),  // Muted light text
    primary = Color(0xFFFFB74D),           // Light orange for dark theme
    onPrimary = Color(0xFF5D2E00),         // Dark text
)
```

#### Chip Color Calculation Logic

**From CategoryFilterComponent.kt, lines 126-209:**

```kotlin
@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse category color from hex
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Check if we're in dark theme by checking background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Match iOS semantic colors: white in dark mode, black in light mode
    val selectionColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.1f) // White glow in dark mode
    } else {
        Color.Black.copy(alpha = 0.1f) // Gray shade in light mode
    }

    val borderColor = if (isDarkTheme) {
        if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
    } else {
        if (isSelected) Color.Black else Color.Black.copy(alpha = 0.3f)
    }
```

### 4. Chip Visual Breakdown

#### Selected Chip in Light Theme
- **Container Color**: `Color.Black.copy(alpha = 0.1f)` = Semi-transparent black (#000000 at 10% opacity)
- **Border Color**: `Color.Black` = Full black (#000000)
- **Border Width**: `1.dp`
- **Text Color**: `MaterialTheme.colorScheme.onSurface` = `Color(0xFF1C1B1F)` (dark gray/black)
- **Text Weight**: `FontWeight.Medium` when selected
- **Corner Radius**: `16.dp` (RoundedCornerShape)
- **Color Dot**: Category's custom color from `category.colorHex`
- **Dot Border**: `Color.Black.copy(alpha = 0.3f)` = Semi-transparent black at 30%

#### Unselected Chip in Light Theme
- **Container Color**: `Color.Transparent` (no background fill)
- **Border Color**: `Color.Black.copy(alpha = 0.3f)` = Semi-transparent black at 30%
- **Border Width**: `1.dp`
- **Text Color**: `MaterialTheme.colorScheme.onSurfaceVariant` = `Color(0xFF49454F)` (muted gray)
- **Text Weight**: `FontWeight.Normal`
- **Corner Radius**: `16.dp`
- **Color Dot**: Category's custom color from `category.colorHex`
- **Dot Border**: `Color.Black.copy(alpha = 0.3f)`

#### Selected Chip in Dark Theme
- **Container Color**: `Color.White.copy(alpha = 0.1f)` = Semi-transparent white (#FFFFFF at 10% opacity)
- **Border Color**: `Color.White` = Full white (#FFFFFF)
- **Border Width**: `1.dp`
- **Text Color**: `MaterialTheme.colorScheme.onSurface` = `Color(0xFFE6E1E5)` (light gray/white)
- **Text Weight**: `FontWeight.Bold` (ALL TEXT IS BOLD IN DARK MODE)
- **Corner Radius**: `16.dp`
- **Color Dot**: Category's custom color from `category.colorHex`
- **Dot Border**: `Color.White.copy(alpha = 0.3f)` = Semi-transparent white at 30%

#### Unselected Chip in Dark Theme
- **Container Color**: `Color.Transparent`
- **Border Color**: `Color.White.copy(alpha = 0.3f)` = Semi-transparent white at 30%
- **Border Width**: `1.dp`
- **Text Color**: `MaterialTheme.colorScheme.onSurfaceVariant` = `Color(0xFFCAC4D0)` (muted light gray)
- **Text Weight**: `FontWeight.Bold` (ALL TEXT IS BOLD IN DARK MODE)
- **Corner Radius**: `16.dp`
- **Color Dot**: Category's custom color from `category.colorHex`
- **Dot Border**: `Color.White.copy(alpha = 0.3f)`

### 5. Category Color Dot Specifications

**From CategoryFilterComponent.kt, lines 172-189:**

```kotlin
// Color dot indicator
Box(
    modifier = Modifier
        .size(12.dp)                      // Size: 12dp diameter circle
        .background(
            color = categoryColor,         // Category's custom color
            shape = CircleShape
        )
        .border(
            width = 1.dp,                  // Border: 1dp
            color = if (isDarkTheme) {
                Color.White.copy(alpha = 0.3f)  // White at 30% in dark mode
            } else {
                Color.Black.copy(alpha = 0.3f)  // Black at 30% in light mode
            },
            shape = CircleShape
        )
)
```

**Exact Specifications**:
- **Size**: 12.dp x 12.dp
- **Shape**: Perfect circle (CircleShape)
- **Fill**: Category's `colorHex` parsed to Color
- **Border Width**: 1.dp
- **Border Color**: Theme-adaptive (black 30% in light, white 30% in dark)
- **Position**: 8.dp spacing from text (horizontal arrangement)

### 6. Text Typography

**From CategoryFilterComponent.kt, lines 192-207:**

```kotlin
// Category text
Text(
    text = category.displayName,
    style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = when {
            isDarkTheme -> androidx.compose.ui.text.font.FontWeight.Bold  // Bold in dark mode
            isSelected -> androidx.compose.ui.text.font.FontWeight.Medium  // Medium when selected
            else -> androidx.compose.ui.text.font.FontWeight.Normal       // Regular otherwise
        }
    ),
    color = if (isSelected) {
        MaterialTheme.colorScheme.onSurface       // Full contrast text
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant // Muted text
    }
)
```

**Typography Breakdown**:
- **Font Size**: 14.sp (fixed)
- **Base Style**: `MaterialTheme.typography.bodyMedium`
- **Font Weight**:
  - Dark theme: **Always Bold** (all chips)
  - Light theme, selected: **Medium**
  - Light theme, unselected: **Normal/Regular**
- **Color**:
  - Selected: `onSurface` (high contrast)
  - Unselected: `onSurfaceVariant` (muted)

### 7. Chip Padding and Spacing

**From CategoryFilterComponent.kt, lines 167-168:**

```kotlin
Row(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // Color dot (12.dp circle)
    // 8.dp spacing (from horizontalArrangement)
    // Text
}
```

**Exact Measurements**:
- **Horizontal padding**: 12.dp (left and right inside chip)
- **Vertical padding**: 8.dp (top and bottom inside chip)
- **Dot-to-text spacing**: 8.dp
- **Chip-to-chip spacing**: 8.dp (set in parent LazyRow)

---

## Safe Area/Insets Handling

### 1. Current Implementation (as of research date)

**File**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt`

**Lines 196-243 (Category Filter Bar with Lock Icon):**

```kotlin
// Category filter chips at top - floating bar with close button
if (categories.isNotEmpty()) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp), // Reserve space for status bar/notch (even when hidden in Kids Mode)
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Category filter chips (scrollable, stops before close button)
            CategoryFilterComponentKidsMode(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    galleryViewModel.selectCategory(categoryId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 56.dp) // Make room for close button
            )

            // Close button (fixed on right side)
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { modeViewModel.requestModeToggle() },
                color = Color(0xFFE53935), // Red background
                shadowElevation = 2.dp
            ) {
                // Lock icon inside...
            }
        }
    }
}
```

### 2. Safe Area Strategy

**FIXED TOP PADDING APPROACH**:
- **Method**: `padding(top = 40.dp)` on the Surface wrapper
- **Purpose**: Reserve space for status bar/notch area
- **Note**: This is a HARDCODED value, not using `statusBarsPadding()` modifier

**Why 40.dp?**
- Provides clearance for most Android status bars
- Does NOT use dynamic WindowInsets
- Comment explicitly states: "Reserve space for status bar/notch (even when hidden in Kids Mode)"

### 3. Comparison with Parent Mode

**Parent Mode** (AppHeaderComponent) uses:
```kotlin
.statusBarsPadding() // Dynamic, adapts to actual status bar height
```

**Kids Mode** currently uses:
```kotlin
.padding(top = 40.dp) // Fixed, does not adapt
```

### 4. Known Issue (from STORY-12.2)

The current implementation has a documented bug:
- Fixed 40.dp padding may not be sufficient for all devices
- Does not dynamically adapt to different screen configurations
- Story STORY-12.2 recommends changing to `.statusBarsPadding()`

### 5. Content Padding Below Filter Bar

**Lines 248-259 (Photo Grid Padding):**

```kotlin
LazyColumn(
    state = listState,
    contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 8.dp,
        bottom = 16.dp // Reduced bottom padding since filters are now at top
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize(),
    reverseLayout = false // Keep normal layout, photos start at top
)
```

**Content Padding Breakdown**:
- **Left/Right**: 16.dp
- **Top** (below filter bar): 8.dp
- **Bottom**: 16.dp
- **Item spacing**: 12.dp between photo items

### 6. Screen Size Adaptations

**No explicit responsive logic** for different screen sizes in kids mode. The layout relies on:
- Compose's automatic measurement and layout
- Fixed padding values
- `fillMaxWidth()` and `fillMaxSize()` modifiers for flexibility

**Horizontal scrolling** is handled by `LazyRow` in CategoryFilterComponent:
```kotlin
// From CategoryFilterComponent.kt, line 226
LazyRow(
    modifier = modifier,
    horizontalArrangement = horizontalArrangement,
    contentPadding = contentPadding
)
```

---

## Lock Icon to Close Kids Mode

### 1. Location and Positioning

**File**: `KidsModeGalleryScreen.kt`, Lines 218-241

**Position in Layout Hierarchy**:
```
Surface (full width, at top of screen)
  └─ Box (full width container)
       ├─ CategoryFilterComponentKidsMode (scrollable, ends 56.dp from right)
       └─ Lock Icon Button (fixed on right side)
```

### 2. Exact Implementation

```kotlin
// Close button (fixed on right side)
Surface(
    modifier = Modifier
        .align(Alignment.CenterEnd)        // Aligned to right center of parent Box
        .padding(end = 8.dp)               // 8.dp from right edge
        .size(48.dp)                       // 48.dp x 48.dp square
        .clip(CircleShape)                 // Clipped to perfect circle
        .clickable { modeViewModel.requestModeToggle() },
    color = Color(0xFFE53935),             // Material Red 600 - Error/warning red
    shadowElevation = 2.dp                 // Subtle shadow for depth
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Exit Kids Mode",
            tint = Color.White,             // White icon on red background
            modifier = Modifier.size(24.dp) // Icon is 24.dp, inside 48.dp circle
        )
    }
}
```

### 3. Visual Specifications

**Container (Surface)**:
- **Size**: 48.dp x 48.dp (square before clipping)
- **Shape**: CircleShape (perfect circle)
- **Background Color**: `Color(0xFFE53935)` = Material Red 600 (error red)
  - RGB: rgb(229, 57, 53)
  - Hex: #E53935
- **Shadow**: 2.dp elevation
- **Position**: Right-aligned, 8.dp from right edge
- **Vertical alignment**: Centered vertically within the filter bar

**Icon**:
- **Icon**: `Icons.Default.Lock` (Material Design lock icon)
- **Size**: 24.dp x 24.dp
- **Color**: `Color.White` (#FFFFFF)
- **Position**: Centered inside the 48.dp circle
- **Content Description**: "Exit Kids Mode" (for accessibility)

**Spacing Relationship**:
- Filter chips have `padding(end = 56.dp)` to avoid overlap
- Lock button is 48.dp wide + 8.dp end padding = 56.dp total reserved space
- This ensures chips scroll under/stop before reaching the lock button

### 4. Click Behavior

**What happens when tapped**:

```kotlin
.clickable { modeViewModel.requestModeToggle() }
```

This triggers the mode toggle request flow:

1. **Request sent** to `AppModeViewModel.requestModeToggle()`
2. **State changes** to `requiresPinAuth = true`
3. **LaunchedEffect detects** the state change (lines 116-144)
4. **Biometric check**:
   - If biometric available and enabled → Show biometric prompt
   - If biometric succeeds → Exit kids mode immediately
   - If biometric fails/canceled → Show PIN dialog
5. **PIN verification** (if shown):
   - User enters PIN in dialog (lines 312-326)
   - If correct → Exit kids mode
   - If incorrect → Show error, stay in kids mode
6. **Mode change completes** → Navigate to parent mode

### 5. Authentication Flow Details

**From lines 116-144:**

```kotlin
LaunchedEffect(modeState.requiresPinAuth) {
    if (modeState.requiresPinAuth) {
        // Check if biometric is enabled and available
        if (biometricManager.shouldOfferBiometricFirst() && activity != null) {
            // Try biometric first
            coroutineScope.launch {
                when (biometricManager.authenticateWithBiometrics(
                    activity = activity,
                    title = "Exit Kids Mode",
                    subtitle = "Use your fingerprint or face to return to Parent Mode",
                    description = "Biometric authentication protects parental settings"
                )) {
                    com.smilepile.security.BiometricResult.SUCCESS -> {
                        modeViewModel.forceParentMode()
                    }
                    com.smilepile.security.BiometricResult.USER_CANCELED -> {
                        showPinDialog = true
                    }
                    else -> {
                        showPinDialog = true
                    }
                }
            }
        } else {
            // No biometric available - show PIN dialog
            showPinDialog = true
        }
    }
}
```

**Authentication Priority**:
1. **First**: Biometric (if available and enabled)
2. **Fallback**: PIN entry dialog
3. **Cancel**: Returns to kids mode (no change)

### 6. Back Button Behavior

**From line 147-151:**

```kotlin
BackHandler {
    if (zoomedPhoto == null) {
        modeViewModel.requestModeToggle()
    }
}
```

**What this means**:
- Pressing Android back button when NOT in fullscreen photo view → Same as tapping lock icon
- Pressing Android back button when IN fullscreen photo view → Exits fullscreen, stays in kids mode
- Back button essentially behaves like "lock" button when in gallery view

---

## Overall Kids Gallery Mode Structure

### 1. File Organization

**Main Implementation Files**:

1. **`/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt`**
   - Main screen composable (643 lines)
   - Photo grid display
   - Category filtering
   - Lock button
   - Authentication logic
   - Fullscreen photo viewer overlay

2. **`/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/components/gallery/CategoryFilterComponent.kt`**
   - Reusable category filter component (283 lines)
   - `CategoryChip` composable
   - `AllPhotosChip` composable
   - `CategoryFilterComponent` (parent mode)
   - `CategoryFilterComponentKidsMode` (kids mode wrapper)

3. **`/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`**
   - Theme definitions
   - Color schemes (light and dark)
   - Kids mode typography flag (line 77: `isKidsMode: Boolean = false`)

### 2. Screen Layout Structure

**Overall Hierarchy**:

```
KidsModeGalleryScreen
├─ Column (fills screen, handles gestures)
│   ├─ Surface (category filter bar + lock button)
│   │   └─ Box
│   │       ├─ CategoryFilterComponentKidsMode (scrollable chips)
│   │       └─ Lock Icon Button (fixed position)
│   │
│   └─ LazyColumn (photo grid)
│       └─ KidsPhotoStackItem (repeated for each photo)
│
└─ ZoomedPhotoOverlay (conditional, when photo tapped)
    └─ HorizontalPager (category swiping)
        └─ VerticalPager (photo swiping within category)
            └─ AsyncImage (full-screen photo)
```

### 3. Layout Measurements

**Screen Structure Measurements**:

```
┌─────────────────────────────────────────┐
│         Status Bar (system)             │ <- System-managed height
├─────────────────────────────────────────┤
│      40.dp padding (safe area)          │ <- Hardcoded top padding
├─────────────────────────────────────────┤
│  ┌─────────────────────────┬─────────┐  │
│  │  Category Filter Chips  │ [LOCK]  │  │ <- ~56.dp total height (with internal padding)
│  │  (horizontally scrolling) │ 48.dp │  │
│  └─────────────────────────┴─────────┘  │
├─────────────────────────────────────────┤
│             8.dp padding                │
├─────────────────────────────────────────┤
│                                         │
│         Photo Grid (LazyColumn)         │
│   ┌───────────────────────────────┐   │ <- 16.dp left/right padding
│   │        Photo Item (4:3)       │   │ <- 12.dp spacing between items
│   └───────────────────────────────┘   │
│   ┌───────────────────────────────┐   │
│   │        Photo Item (4:3)       │   │
│   └───────────────────────────────┘   │
│                 ...                     │
│                                         │
├─────────────────────────────────────────┤
│            16.dp bottom padding         │
└─────────────────────────────────────────┘
```

### 4. Photo Item Layout

**From lines 330-356:**

```kotlin
@Composable
private fun KidsPhotoStackItem(
    photo: Photo,
    onClick: () -> Unit
) {
    // Calculate dynamic height based on image aspect ratio
    // Default to 4:3 aspect ratio if no specific ratio is available
    val defaultAspectRatio = 4f / 3f

    Box(
        modifier = Modifier
            .fillMaxWidth()                  // Full width of parent
            .aspectRatio(defaultAspectRatio) // Maintains 4:3 ratio
            .clip(RoundedCornerShape(12.dp)) // Rounded corners
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.path)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Photo Item Specifications**:
- **Width**: Full width of screen minus 32.dp (16.dp padding each side)
- **Aspect Ratio**: 4:3 (default, no dynamic calculation despite comment)
- **Corner Radius**: 12.dp (RoundedCornerShape)
- **Image Scaling**: ContentScale.Crop (fills box, may crop)
- **Image Loading**: Coil library with crossfade animation
- **Spacing Between Items**: 12.dp (from LazyColumn verticalArrangement)

### 5. Fullscreen Photo Viewer Overlay

**From lines 273-298 (overlay trigger):**

When a photo is tapped:
```kotlin
zoomedPhoto?.let { photo ->
    val actualPhotoIndex = displayedPhotos.indexOfFirst { it.id == photo.id }
    val safePhotoIndex = if (actualPhotoIndex >= 0) actualPhotoIndex else 0

    ZoomedPhotoOverlay(
        allPhotos = displayedPhotos, // Filtered by current category
        categories = categories,
        currentCategoryId = selectedCategoryId ?: categories.firstOrNull()?.id ?: 0L,
        initialPhotoIndex = safePhotoIndex,
        onDismiss = {
            zoomedPhoto = null
            modeViewModel.setKidsFullscreen(false)
        },
        onCategoryChange = { newCategoryId ->
            maintainZoom = true
            galleryViewModel.selectCategory(newCategoryId)
            val categoryName = categories.find { it.id == newCategoryId }?.displayName ?: "Category"
            toastState?.showCategory(categoryName)
        }
    )
}
```

**Overlay Structure (lines 390-507):**

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomedPhotoOverlay(
    allPhotos: List<Photo>,
    categories: List<Category>,
    currentCategoryId: Long,
    initialPhotoIndex: Int,
    onDismiss: () -> Unit,
    onCategoryChange: (Long) -> Unit
) {
    // Two-dimensional paging:
    // - Horizontal paging for categories
    // - Vertical paging for photos within category

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)  // Full black background
    ) {
        HorizontalPager(
            state = categoryPagerState,
            modifier = Modifier.fillMaxSize()
        ) { categoryPage ->
            VerticalPager(
                state = photoPagerState,
                modifier = Modifier.fillMaxSize()
            ) { photoPage ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onDismiss() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(categoryPhotos[photoPage].path)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,  // Fit, not crop
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
```

**Fullscreen Viewer Features**:
- **Background**: Solid black (`Color.Black`)
- **Navigation**:
  - Swipe horizontally → Change category
  - Swipe vertically → Change photo within category
- **Dismiss**: Tap anywhere on photo
- **Back Button**: Handled separately, calls onDismiss
- **Animation**: Zoom-in animation (animateFloatAsState on alpha/scale)
- **Image Scaling**: ContentScale.Fit (shows full photo, no crop)
- **Toast**: Category name toast shown when category changes

### 6. State Management

**ViewModels Used**:

1. **PhotoGalleryViewModel** (`galleryViewModel`):
   - Manages photo list (`allPhotos`)
   - Manages categories list
   - Tracks selected category (`selectedCategoryId`)
   - Provides `selectCategory(id)` function

2. **AppModeViewModel** (`modeViewModel`):
   - Manages kids/parent mode state
   - Handles PIN authentication flow
   - Provides `requestModeToggle()`, `forceParentMode()`, etc.
   - Tracks fullscreen state (`setKidsFullscreen(boolean)`)

**State Flow**:

```kotlin
val galleryState by galleryViewModel.uiState.collectAsState()
val categories by galleryViewModel.categories.collectAsState()
val selectedCategoryId by galleryViewModel.selectedCategoryId.collectAsState()
val allPhotos by galleryViewModel.allPhotos.collectAsState()
val modeState by modeViewModel.uiState.collectAsState()
```

**Local State**:
- `zoomedPhoto` - Currently zoomed photo (or null)
- `maintainZoom` - Flag to keep fullscreen when category changes
- `showPinDialog` - Controls PIN dialog visibility

### 7. Special Features and Behaviors

#### Category Swipe Gestures (Non-Fullscreen)

**From lines 175-194:**

```kotlin
// Handle horizontal swipe gestures for category navigation with debouncing
val categorySwipeHandler = rememberCategorySwipeHandler(
    categories = categories,
    selectedCategoryId = selectedCategoryId,
    onCategorySelected = galleryViewModel::selectCategory
)

Column(
    modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .pointerInput(categories, selectedCategoryId) {
            detectHorizontalDragGestures(
                onDragEnd = { categorySwipeHandler.handleDragEnd() },
                onHorizontalDrag = { _, dragAmount ->
                    categorySwipeHandler.horizontalDragOffset += dragAmount
                }
            )
        }
)
```

**Swipe Behavior**:
- Swipe left → Next category
- Swipe right → Previous category
- Threshold: 100f pixels (from line 565)
- Debounce: 300ms between swipes (from line 566)
- Wraps around (last → first, first → last)

#### Empty State

**From lines 358-387:**

```kotlin
@Composable
private fun EmptyKidsGallery() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "No photos",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No photos yet!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ask a parent to add some photos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

**Empty State Design**:
- **Icon**: Camera icon, 72.dp size
- **Primary Text**: "No photos yet!" (headlineMedium)
- **Secondary Text**: "Ask a parent to add some photos" (bodyLarge)
- **Color**: All elements use `onSurfaceVariant` (muted color)
- **Spacing**: 16.dp between elements

### 8. Animation Details

**Zoom Animation (lines 424-429):**

```kotlin
val animationProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(300),
    label = "zoom"
)
```

Applied to fullscreen photo:
```kotlin
modifier = Modifier
    .fillMaxSize()
    .graphicsLayer {
        scaleX = animationProgress
        scaleY = animationProgress
        alpha = animationProgress
    }
```

**Animation Specs**:
- **Duration**: 300ms
- **Type**: Tween (linear interpolation)
- **Properties Animated**: scaleX, scaleY, alpha
- **Start State**: 0f (invisible, zero size)
- **End State**: 1f (visible, full size)

---

## Complete Code Reference

### Key Components Summary

#### CategoryFilterComponentKidsMode
**File**: `CategoryFilterComponent.kt`, Lines 262-283

```kotlin
@Composable
fun CategoryFilterComponentKidsMode(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    enableToggle: Boolean = false  // Disabled toggle to prevent "All Photos" state
) {
    CategoryFilterComponent(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = { categoryId ->
            // Always select a category, never allow null
            if (categoryId != null) {
                onCategorySelected(categoryId)
            }
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        showAllChip = false  // Hide "All" chip in Kids Mode
    )
}
```

**Key Differences from Parent Mode**:
1. `showAllChip = false` - No "All Photos" option
2. Null filtering in `onCategorySelected` - Prevents deselection
3. Same visual styling as parent mode (uses same CategoryChip)

#### Lock Icon Color

**Exact Color**: `Color(0xFFE53935)`
- **Name**: Material Design Red 600
- **Hex**: #E53935
- **RGB**: rgb(229, 57, 53)
- **Usage**: Error/warning color, high visibility
- **Contrast**: White icon on red background (excellent contrast ratio)

#### Safe Area Handling Pattern

**Current (Fixed Padding)**:
```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 40.dp), // Fixed padding
    // ...
)
```

**Recommended (Dynamic Padding)**:
```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding(), // Dynamic, adapts to device
    // ...
)
```

**Import Required**:
```kotlin
import androidx.compose.foundation.layout.statusBarsPadding
```

### Color Palette Quick Reference

| Element | Light Theme | Dark Theme |
|---------|-------------|------------|
| Background | #FFFBFE (white) | #1C1B1F (dark gray) |
| Surface | #FFFBFE (white) | #1C1B1F (dark gray) |
| Text (primary) | #1C1B1F (black) | #E6E1E5 (light) |
| Text (secondary) | #49454F (gray) | #CAC4D0 (light gray) |
| Selected chip bg | Black 10% | White 10% |
| Selected chip border | Black 100% | White 100% |
| Unselected chip border | Black 30% | White 30% |
| Lock button | #E53935 (red) | #E53935 (red) |
| Lock icon | #FFFFFF (white) | #FFFFFF (white) |

### Spacing and Sizing Quick Reference

| Element | Value | Notes |
|---------|-------|-------|
| Top safe area padding | 40.dp | Should be statusBarsPadding() |
| Filter bar shadow | 8.dp | Elevation |
| Filter chips horizontal padding | 12.dp | Inside each chip |
| Filter chips vertical padding | 8.dp | Inside each chip |
| Filter chips spacing | 8.dp | Between chips |
| Category color dot size | 12.dp | Circle diameter |
| Category dot border | 1.dp | Thickness |
| Chip border | 1.dp | Thickness |
| Chip corner radius | 16.dp | RoundedCornerShape |
| Lock button size | 48.dp | Circle diameter |
| Lock button end padding | 8.dp | From right edge |
| Lock button shadow | 2.dp | Elevation |
| Lock icon size | 24.dp | Inside 48.dp button |
| Photo grid left/right padding | 16.dp | Each side |
| Photo grid top padding | 8.dp | Below filter bar |
| Photo grid bottom padding | 16.dp | At bottom |
| Photo item spacing | 12.dp | Between items |
| Photo corner radius | 12.dp | RoundedCornerShape |
| Photo aspect ratio | 4:3 | Default ratio |

---

## iOS Implementation Guidance

Based on this Android implementation, here are key recommendations for iOS:

### 1. Chip Styling

**Colors to Match**:
- Use semantic colors that adapt to light/dark mode
- Light mode: Black with 10% opacity for selection, Black borders
- Dark mode: White with 10% opacity for selection, White borders
- Border width: ~1 point (equivalent to 1.dp)
- Corner radius: ~16 points (equivalent to 16.dp)

**Typography**:
- Font size: ~14 points (equivalent to 14.sp)
- Weight: Bold in dark mode (all chips), Medium when selected in light mode
- Color: Primary label color when selected, secondary label color when unselected

**Category Dot**:
- Size: ~12 points diameter
- Border: ~1 point, adapts to theme (black 30% or white 30%)
- Spacing from text: ~8 points

### 2. Safe Area Handling

**Recommendation**: Use `.padding(.top, 50)` to match Android's intent
- Android uses 40.dp fixed padding (but should use statusBarsPadding)
- iOS equivalently should use ~50 points to clear Dynamic Island
- This matches the pattern in iOS AppHeaderComponent

**DO NOT**:
- Ignore safe area completely (current bug)
- Use `.safeAreaInset` if parent view uses `.ignoresSafeArea()`

### 3. Lock Button

**Match Android Specs**:
- Size: ~48 points diameter
- Icon size: ~24 points (half of container)
- Color: SwiftUI Color.red (or custom #E53935 to exactly match)
- Icon: SF Symbol "lock.fill" in white
- Position: Right-aligned, ~8 points from edge
- Shadow: Small shadow for depth

### 4. Layout Structure

**Mirror Android's approach**:
- Filter bar at top with shadow
- Lock button overlays right side of filter bar
- Filter chips scroll but stop before lock button
- Photo grid below with appropriate spacing
- Full-screen overlay uses black background

### 5. Animations

**Match Android timing**:
- Zoom animation: 300ms (0.3 seconds)
- Use spring animation or easeInOut for smoothness
- Animate scale and opacity together

---

## Conclusion

This documentation provides a complete reference for the Android Kids Gallery Mode implementation. Key highlights:

1. **Chip colors**: Theme-adaptive with semantic colors (black in light, white in dark)
2. **Safe area**: Currently uses fixed 40.dp padding (should be dynamic)
3. **Lock icon**: Red (#E53935) circle, 48.dp, with white lock icon
4. **Layout**: Simple column with filter bar at top, photo grid below
5. **Code is clean and well-organized** with clear separation of concerns

All measurements, colors, and implementation details are documented with exact line numbers for reference.

---

**Research completed**: 2025-10-09
**Documented by**: Research Agent
**For use in**: iOS Kids Mode implementation (ATLAS Phase 1)
