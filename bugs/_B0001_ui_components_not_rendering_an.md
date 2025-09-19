# B0001: UI components not rendering and category dropdown non-functional on Android 16

## Type
Bug

## Severity
high

## Status
resolved

## Description
Multiple UI rendering failures in SmilePile Android app when running on Android 16 (API 35). Button and FloatingActionButton components defined in XML layouts are not appearing on screen. Additionally, the category selection dropdown in ParentModeFragment is non-responsive, preventing the photo import workflow from completing.

## Steps to Reproduce
1. Launch SmilePile app on Pixel 9 emulator with Android 16
2. Observe CategorySelectionFragment - no Import Photos button visible
3. Access ParentModeFragment
4. Tap on category dropdown spinner
5. Observe dropdown does not open or respond
6. Cannot proceed with photo import

## Expected Behavior
Import Photos button visible in top-right|FloatingActionButton in bottom-right|Long press triggers parent mode dialog|Category dropdown shows Animals, Family, Fun Times|Can select category and import photos

## Actual Behavior
No buttons or FAB visible despite proper XML|Long press has no effect|Category dropdown non-functional|Photo import blocked

## Environment
- Device: Pixel 9 Emulator
- Android Version: Android 16 (API Level 35)
- App Target SDK: 34
- Material Components: 1.12.0 (upgraded from 1.10.0)
- Kotlin: 1.9.0
- Gradle: 8.2
- Android Gradle Plugin: 8.2.0
- Layout: CoordinatorLayout (migrated from FrameLayout)

## Possible Root Cause
1. **SDK Version Mismatch**: App targets SDK 34 but running on Android 16 (API 35)
2. **Edge-to-Edge Enforcement**: Android 16 enforces edge-to-edge display which may hide UI components
3. **Material Components Compatibility**: Version 1.12.0 may have issues with Android 16
4. **Database Access Pattern**: Category dropdown not populated - possible main thread blocking
5. **View Inflation Issues**: CoordinatorLayout may not be properly inflating child views

## Proposed Fix
1. Update targetSdk and compileSdk to 35 in build.gradle.kts
2. Remove fitsSystemWindows="true" or handle insets programmatically
3. Fix category spinner population in ParentModeFragment.onViewCreated()
4. Add proper async loading for database categories
5. Consider reverting to FrameLayout or using ConstraintLayout
6. Update Kotlin to 2.0+ and Gradle to 8.13+ for better Android 16 support

## Created
2025-09-18T22:04:27.275078
