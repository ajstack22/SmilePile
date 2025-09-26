# Kids Mode - Android Implementation Documentation

## Overview

Kids Mode is the default and primary interface for SmilePile, designed as a safe photo viewer for children. It allows vertical swiping through photos within a category and horizontal swiping to jump between categories, with PIN protection preventing access to Parent Mode features.

## Mode Activation & Persistence

### Default State
- **Initial Mode**: Kids Mode is the default state when the app launches
- **Persistence**: Mode state is saved to SharedPreferences via ModeManager and persists across app restarts
- **Mode Storage**: Stored in "app_mode_prefs" with key "current_mode"

### Mode Switching Mechanism
1. **From Kids to Parent Mode**:
   - Triggered via tapping top right corner of the screen 3 times
   - Requires PIN validation if PIN is enabled in SecurePreferencesManager
   - If no PIN is set, switches directly to Parent Mode
   - Navigation to ParentalLockScreen for authentication

2. **From Parent to Kids Mode**:
   - No authentication required
   - Immediate switch via AppModeViewModel.forceKidsMode()

### PIN Protection Implementation
- **Purpose**: PIN locks access to Parent Mode features, not just app exit
- **PIN Check**: SecurePreferencesManager.isPINEnabled() determines if PIN is required
- **Protection Scope**: When PIN is enabled, users cannot access:
  - Settings and app configuration
  - Photo editing and management features
  - Category creation/deletion
  - Photo deletion/organization tools
- **Validation**: PIN validation occurs in ParentalLockScreen via ParentalControlsViewModel
- **Biometric Support**: Biometric authentication available if enabled and device supports it
- **Back Button Handling**: MainActivity.kidsBackPressedCallback handles back press in Kids Mode

## UI Differences

### System UI Behavior
- **Kids Mode**:
  - Status bar and navigation bar are HIDDEN via WindowInsetsControllerCompat
  - Uses BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE for immersive experience
  - Full-screen photo viewing experience

- **Parent Mode**:
  - System bars visible with transparent status bar
  - Theme-aware status bar icon colors
  - Standard navigation experience

### Bottom Navigation
- **Kids Mode**: Bottom navigation is completely hidden
- **Parent Mode**: Shows full navigation (Gallery, Categories, Settings)
- **Visibility Control**: MainScreen.shouldShowBottomNavigation enforces this rule

### Interface Components
- **Kids Mode**: Uses KidsModeGalleryScreen optimized as a photo viewer with:
  - Simplified gallery view for category browsing
  - Primary fullscreen viewer interface with swipe navigation
  - No management controls or editing capabilities
- **Parent Mode**: Uses PhotoGalleryScreen with full management capabilities

## Navigation Restrictions

### Blocked Features in Kids Mode
1. **Settings Access**:
   - Settings route redirects to gallery via LaunchedEffect in AppNavigation
   - No direct access to app configuration

2. **Category Management**:
   - Uses CategoryFilterComponentKidsMode (view-only)
   - Cannot create, edit, or delete categories
   - Can only switch between existing categories

3. **Photo Editing**:
   - No photo editing capabilities
   - Photo clicks only open fullscreen zoom mode
   - No access to PhotoEditScreen

4. **Photo Management**:
   - Cannot delete photos
   - Cannot add photos from gallery
   - Cannot manage photo metadata

### Available Features in Kids Mode
1. **Photo Viewing**:
   - Primarily a fullscreen photo viewer experience
   - Vertical swiping scrolls through photos within the current category
   - Horizontal swiping (left/right) jumps to the first photo in the previous/next category
   - Tap to dismiss fullscreen mode

2. **Category Navigation**:
   - Category filter chips at top of screen in gallery view
   - Horizontal swipe gestures for category switching in fullscreen mode
   - Category cycling with visual feedback via toast notifications

3. **Safe Interaction**:
   - All destructive actions disabled
   - Child-friendly empty states
   - Protective error handling

## Photo Display Implementation

### Layout Structure
- **Gallery View**: LazyColumn with single-column layout (NOT grid) for browsing
- **Primary Interface**: Fullscreen photo viewer accessed by tapping photos
- **Photo Items**: KidsPhotoStackItem components with:
  - 4:3 aspect ratio containers
  - Rounded corner styling (12.dp)
  - ContentScale.Crop for photo display
  - Click handlers for entering fullscreen viewer mode

### Category Filtering
- **Real Filtering**: Photos filtered by selectedCategoryId from allPhotos
- **Filter Logic**: `displayedPhotos = allPhotos.filter { it.categoryId == selectedCategoryId }`
- **Category Selection**: Managed via PhotoGalleryViewModel.selectCategory()

### Fullscreen Photo Experience (Primary Kids Mode Interface)
- **Overlay Component**: ZoomedPhotoOverlay with dual pager system:
  - HorizontalPager for category navigation (left/right swipes)
  - VerticalPager for photos within categories (up/down swipes)
- **Navigation Behavior**:
  - **Vertical swipes (up/down)**: Navigate through photos within the current category
  - **Horizontal swipes (left/right)**: Jump to the first photo of the previous/next category
  - **Tap**: Dismiss fullscreen mode and return to gallery view
- **State Management**: isKidsFullscreen tracks fullscreen state

## Core Navigation Pattern

Kids Mode implements a unique navigation pattern optimized for child-friendly photo viewing:

1. **Entry Point**: Tap any photo in the gallery view to enter fullscreen viewer mode
2. **Within Category Navigation**:
   - **Vertical swipes** move between photos in the current category
   - Swipe up/down to see the next/previous photo in the same category
3. **Between Category Navigation**:
   - **Horizontal swipes** jump to adjacent categories
   - Swipe left/right to go to the first photo of the previous/next category
   - Visual feedback shows the new category name via toast
4. **Exit**: Tap anywhere on the photo to return to gallery view

### Swipe Gesture Implementation
- **Fullscreen Mode Navigation**:
  - **Vertical swipes**: Navigate between photos within the current category
  - **Horizontal swipes**: Jump to first photo of adjacent categories
  - Threshold: 100f pixels
  - Debounced: 300ms minimum between swipes
- **Category Index Tracking**: Uses categoryIds list for proper cycling
- **Visual Feedback**: Category toast display shows category name when switching in fullscreen mode

## Security Features

### Child Safety Measures
1. **No Destructive Actions**: All deletion/editing capabilities disabled
2. **Navigation Boundaries**: Cannot access management screens
3. **PIN Protection**: Locks Parent Mode features behind authentication (if enabled)
4. **System UI Hiding**: Prevents accidental system interactions
5. **Simplified Interface**: Primarily a viewer mode that reduces cognitive load and confusion
6. **Viewer-First Design**: The interface is optimized for viewing photos rather than management

### Back Button Handling
- **Kids Mode**: Back button requires PIN authentication before exiting
- **Fullscreen Mode**: Back button dismisses fullscreen without PIN requirement
- **Back Press Callback**: MainActivity manages back button behavior per mode

## Toast System
- **Kids Mode**: Toasts only display in fullscreen photo mode
- **Category Changes**: Toast shows category name when switching in fullscreen
- **Color Coordination**: Toast uses category color from colorHex
- **Positioning**: CategoryToastUI overlay in KidsModeGalleryScreen

## Error Handling & Empty States

### Empty Gallery State
- **Component**: EmptyKidsGallery with camera icon
- **Message**: "No photos yet! Ask a parent to add some photos"
- **Child-Friendly**: Encourages parental involvement

### Error Prevention
- **Index Bounds**: Safe photo index calculation with fallbacks
- **Category Validation**: Ensures selected category exists in available list
- **State Consistency**: LaunchedEffect blocks maintain proper state

## Integration Points

### ViewModel Dependencies
- **AppModeViewModel**: Manages mode state and transitions
- **PhotoGalleryViewModel**: Handles photo data and category selection
- **ParentalControlsViewModel**: Manages PIN authentication

### Navigation Integration
- **AppNavigation**: Route-level mode enforcement
- **MainScreen**: Bottom navigation visibility control
- **NavigationRoutes**: Proper route definitions for mode-specific flows

### Theme Integration
- **ThemeViewModel**: Maintains theme state across mode changes
- **MaterialTheme**: Consistent theming in both modes
- **System Theme**: Automatic theme following system preferences

## Testing Considerations

### Critical Test Cases
1. **Mode Persistence**: Verify mode survives app restarts
2. **PIN Protection**: Test PIN locking of Parent Mode features (not just exit)
3. **Navigation Blocking**: Confirm restricted features are inaccessible in Kids Mode
4. **Photo Display**: Verify category filtering works correctly
5. **Swipe Navigation**: Test fullscreen viewer navigation:
   - Vertical swipes navigate within category
   - Horizontal swipes jump between categories
   - Gesture debouncing and category cycling
6. **System UI**: Confirm immersive mode in Kids Mode
7. **Back Button**: Test all back button scenarios
8. **Fullscreen Mode**: Verify proper entry/exit behavior and primary viewer interface
9. **Empty States**: Test behavior with no photos/categories
10. **Error Handling**: Test edge cases and recovery

### Edge Cases
- App backgrounding during mode switch
- PIN failure handling and cooldown
- Category deletion while in Kids Mode
- Photo deletion while in Kids Mode
- Device rotation during fullscreen viewing
- Network connectivity issues during image loading

## Performance Considerations

### Optimization Features
- **Image Loading**: Coil library with crossfade transitions
- **Gesture Debouncing**: Prevents rapid category switching
- **State Management**: Efficient StateFlow usage
- **Memory Management**: Proper lifecycle-aware components
- **Lazy Loading**: LazyColumn for efficient photo display

### Resource Management
- **Image Caching**: Coil handles image caching automatically
- **State Persistence**: Minimal SharedPreferences usage
- **Animation**: Efficient state-driven animations
- **Memory Cleanup**: Proper disposal of resources

This documentation represents the actual implementation as of the current codebase analysis and should be considered the authoritative reference for Kids Mode functionality.