# ATLAS Wave 9: Critical Category Navigation Bug Fixes

## PROJECT CONTEXT
You are working on SmilePile, a child-friendly photo gallery Android app with parental controls. The app has two modes:
- **Parent Mode**: Full editing capabilities, category management, photo import/delete
- **Kids Mode**: View-only mode with swipe navigation between categories

## CRITICAL BUGS TO FIX

### Bug 1: App Defaults to "All Photos" on Launch
**Current Behavior**: When the app launches, it shows all photos (selectedCategoryId = null)
**Expected Behavior**: App should default to the FIRST category in the list, never show "All Photos"
**Files Affected**:
- `PhotoGalleryViewModel.kt`
- `KidsModeGalleryScreen.kt`
- `PhotoGalleryScreen.kt`

### Bug 2: Category Navigation Order is Broken
**Current Behavior**: Swiping between categories doesn't follow left-to-right/first-to-last order
**Expected Behavior**: Categories should navigate in the exact order they appear in the database
**Files Affected**:
- `KidsModeGalleryScreen.kt` (lines 171-199)

### Bug 3: Category State Not Synced Between Views
**Current Behavior**: Selected category is not synchronized between gallery view and fullscreen photo viewer
**Expected Behavior**: When user changes category in gallery, it should persist when entering/exiting fullscreen
**Files Affected**:
- `PhotoViewerScreen.kt`
- `PhotoGalleryViewModel.kt`
- `AppNavigation.kt`

## REQUIRED ATLAS WORKFLOW

### Phase 1: Initial Assessment
1. Run the app on emulator: `cd /Users/adamstack/SmilePile && ./android/gradlew :app:installDebug && adb shell am start -n com.smilepile/.MainActivity`
2. Verify all three bugs exist
3. Document current state with `adb shell screencap`

### Phase 2: Fix Implementation

#### Fix 1: Remove "All Photos" Default
```kotlin
// In PhotoGalleryViewModel.kt
// Change line 31 from:
private val _selectedCategoryId = MutableStateFlow<Long?>(null)
// To:
private val _selectedCategoryId = MutableStateFlow<Long?>(null)

// Add init block after line 29:
init {
    viewModelScope.launch {
        categoryRepository.getAllCategoriesFlow().collect { categoriesList ->
            if (_selectedCategoryId.value == null && categoriesList.isNotEmpty()) {
                _selectedCategoryId.value = categoriesList.first().id
            }
        }
    }
}
```

#### Fix 2: Category Navigation Order
```kotlin
// In KidsModeGalleryScreen.kt
// The category navigation logic (lines 171-199) needs to ensure:
// 1. Categories are accessed in order from the categories list
// 2. Index calculations properly wrap around
// 3. No "All Photos" option exists
```

#### Fix 3: Category Synchronization
```kotlin
// In PhotoViewerScreen.kt
// Add category tracking:
// 1. Pass selectedCategoryId from gallery to viewer
// 2. Update PhotoViewerScreen to filter photos by current category
// 3. Ensure navigation maintains category context
```

### Phase 3: Testing Protocol

1. **Launch Test**:
   - Start app fresh
   - Verify it opens to first category, not "All Photos"

2. **Navigation Test**:
   - Swipe left/right in Kids Mode
   - Verify categories change in order: Category1 → Category2 → Category3 → Category1 (loop)

3. **Sync Test**:
   - Select Category2 in gallery
   - Open photo fullscreen
   - Exit fullscreen
   - Verify still in Category2

### Phase 4: Validation

Run comprehensive tests:
```bash
# Build and deploy
cd /Users/adamstack/SmilePile
./android/gradlew clean assembleDebug
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Test category navigation
adb shell am start -n com.smilepile/.MainActivity

# Capture evidence
adb shell screencap /sdcard/category_fix_evidence.png
adb pull /sdcard/category_fix_evidence.png atlas/wave-9-evidence/
```

## IMPLEMENTATION CHECKLIST

- [ ] Remove null category state (no "All Photos")
- [ ] Fix category order in swipe navigation
- [ ] Implement category persistence across views
- [ ] Add proper category initialization on app launch
- [ ] Ensure Kids Mode starts with first category
- [ ] Sync category selection between gallery and fullscreen
- [ ] Test all navigation paths
- [ ] Document fixes with screenshots

## KEY FILES TO MODIFY

1. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoGalleryViewModel.kt`
2. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt`
3. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/PhotoViewerScreen.kt`
4. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt`
5. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/navigation/AppNavigation.kt`

## SUCCESS CRITERIA

1. App NEVER shows "All Photos" - always defaults to first category
2. Category navigation follows database order exactly
3. Selected category persists across all views and navigation
4. No category state is lost when switching between modes
5. Swipe navigation cycles through categories correctly

## NOTES FOR IMPLEMENTATION

- The app uses Hilt for dependency injection
- PhotoGalleryViewModel is shared between screens
- Categories come from `categoryRepository.getAllCategoriesFlow()`
- Kids Mode should ONLY show categories, never "All"
- Parent Mode should also default to first category, not "All"

## COMMAND TO START

Copy this entire prompt into a new Claude chat window and say:
"Please implement the Atlas Wave 9 category navigation fixes following the workflow described. Start with Phase 1: Initial Assessment."