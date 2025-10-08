# Atlas Story: Fix Safe Area Insets in Kids Gallery Mode

## Problem
In kids gallery mode (full screen), the pile/category filter chips are rendering over the safe areas on both iOS and Android. This causes the chips to overlap with:
- Status bar
- Notch/dynamic island
- In-screen selfie camera cutouts

## User Story
**AS A** parent using SmilePile in kids gallery mode
**I WANT** the category chips to respect device safe areas
**SO THAT** they don't get obscured by the status bar or camera cutout

## Acceptance Criteria
- [ ] iOS: Category chips respect safe area insets (avoid notch/dynamic island)
- [ ] Android: Category chips respect safe area insets (avoid status bar/camera cutout)
- [ ] Chips remain fully visible and tappable in full screen mode
- [ ] Layout works correctly across different device types (iPhone SE, Pro, Pro Max, Android variants)
- [ ] No regression in non-full-screen mode

## Platform-Specific Guidance

### iOS (SwiftUI)
- Use `.safeAreaInset()` or `.padding(.top, geometry.safeAreaInsets.top)` with GeometryReader
- Check existing PhotoGalleryView/OptimizedPhotoGalleryView for safe area handling
- Ensure CategoryChip positioning accounts for top safe area

### Android (Jetpack Compose)
- Use `WindowInsets.safeDrawing` with `windowInsetsPadding()`
- Check PhotoGalleryScreen for current inset handling
- Apply appropriate padding to CategoryFilterComponent

## Technical Notes
- Kids gallery mode uses full screen immersive mode
- Issue likely exists in category filter chip positioning logic
- May need to adjust top padding/offset calculation

## Related Files to Investigate
- iOS: `OptimizedPhotoGalleryView.swift`, `CategoryChip.swift`
- Android: `PhotoGalleryScreen.kt`, `CategoryFilterComponent.kt`
