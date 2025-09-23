# SMILE-003: Vertical Stack Gallery Layout

## Story Overview
**Type:** Feature Enhancement
**Priority:** High
**Status:** In Development
**Wave:** Wave 8

## Description
Transform the SmilePile Mode (Kids Mode) gallery from a grid layout to a modern vertical stack layout similar to social media feeds. Remove the "All" category option and relocate category filters to the bottom of the screen for better ergonomics.

## User Story
As a child using SmilePile Mode, I want to see photos in a large, easy-to-view vertical stack so that I can focus on one photo at a time while scrolling, and easily reach category filters with my thumbs at the bottom of the screen.

## Acceptance Criteria
1. [ ] Remove "All" category filter - only show actual categories
2. [ ] Move category filters from top to bottom of screen
3. [ ] Replace grid layout with vertical stack (single column)
4. [ ] Images display full width with dynamic height
5. [ ] Maintain tap-to-zoom functionality
6. [ ] Preserve swipe navigation in zoomed view
7. [ ] Category filtering still works correctly

## Technical Requirements

### 1. Layout Changes
- Replace `LazyVerticalGrid` with `LazyColumn`
- Images use `fillMaxWidth()` with dynamic height
- Maintain aspect ratio of original photos
- Consistent spacing between photos (8-12dp)

### 2. Category Filter Positioning
- Move filters to bottom using `Box` with `Alignment.BottomCenter`
- Add background/elevation for visibility
- Ensure filters don't overlap with photos
- Maintain horizontal scrolling for categories

### 3. Category Management
- Remove "All" option from filter chips
- Default to showing all photos when no category selected
- Visual indicator for selected category
- Tap to select, tap again to deselect (show all)

### 4. Performance Considerations
- Lazy loading for smooth scrolling
- Image caching with Coil
- Appropriate image sampling for large photos
- Memory-efficient for large galleries

## Implementation Plan

### Phase 1: Research (Atlas Research Agent)
- Analyze current grid implementation
- Study LazyColumn best practices
- Review Instagram/Pinterest layouts
- Check performance implications

### Phase 2: Design (Atlas UI/UX Agent)
- Design vertical stack layout
- Plan bottom navigation placement
- Define spacing and margins
- Create visual mockup

### Phase 3: Implementation (Atlas Developer Agent)
- Modify KidsModeGalleryScreen layout
- Update CategoryFilterComponent
- Implement vertical stack with LazyColumn
- Position filters at bottom

### Phase 4: Testing (Atlas QA Agent)
- Test scrolling performance
- Verify category filtering
- Validate zoom functionality
- Check memory usage with large galleries

## Files to Modify
1. `KidsModeGalleryScreen.kt` - Main layout changes
2. `CategoryFilterComponent.kt` - Remove "All" option
3. `PhotoGalleryScreen.kt` - Ensure parent mode unchanged
4. `SMILEPILE_MODE_HANDOFF.md` - Update documentation

## Design Specifications

### Photo Display
```kotlin
LazyColumn(
    contentPadding = PaddingValues(bottom = 80.dp), // Space for filters
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(photos) { photo ->
        PhotoCard(
            photo = photo,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

### Category Filters (Bottom)
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Photos in background
    LazyColumn { ... }

    // Filters at bottom
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        CategoryFilterRow(...)
    }
}
```

## Risk Assessment
- **Low Risk:** LazyColumn is standard Compose component
- **Medium Risk:** Performance with high-res images
- **Low Risk:** Category filter relocation is straightforward

## Success Metrics
- Smooth scrolling at 60 FPS
- All photos visible without cropping
- Category filters easily accessible
- Memory usage remains stable
- User engagement increases

## Future Enhancements
- Pull-to-refresh gesture
- Infinite scroll pagination
- Double-tap to zoom
- Share button on each photo
- Photo info overlay option

## Notes
- Maintain immersive experience of SmilePile Mode
- Keep interaction patterns simple for children
- Ensure changes don't affect Parent Mode gallery
- Consider tablet vs phone layouts

---
*Created: 2024-12-21*
*Atlas Story: Wave 8 Enhancement*
*Priority: High*