# SMILE-002: SmilePile Mode Enhancement

## Story Overview
**Type:** Feature Enhancement  
**Priority:** High  
**Status:** In Development  
**Wave:** Post-Wave 4 Enhancement  

## Description
Enhance Kids Mode to become "SmilePile Mode" - a truly immersive photo viewing experience with hidden UI chrome, where the menu bar and FAB are removed, and navigation is controlled through gestures and PIN-protected exits.

## User Story
As a parent, I want my children to have a distraction-free photo viewing experience where they can't accidentally navigate to settings or delete photos, but can still browse and view photos safely.

## Acceptance Criteria
1. ✅ Kids Mode hides all UI chrome (top bar, FAB)
2. ✅ Photos display in full-screen grid
3. ✅ Clicking a photo opens it in fullscreen viewer
4. ✅ Back button triggers PIN authentication to exit
5. ✅ Status bar color matches theme (white in light mode)
6. ✅ No accidental mode switching possible

## Technical Requirements

### 1. UI Changes
- Hide TopAppBar in Kids Mode
- Hide FloatingActionButton in Kids Mode
- Full-screen photo grid with minimal padding
- Immersive system UI experience

### 2. Navigation
- Photo clicks must properly navigate to PhotoViewerScreen
- Back button intercepts with PIN dialog
- Successful PIN entry switches to Parent Mode
- Failed/cancelled PIN keeps in Kids Mode

### 3. Theme Integration
- Status bar color matches current theme
- Light mode: White status bar with dark icons
- Dark mode: Dark status bar with light icons
- Proper WindowInsets handling

### 4. Security
- PIN required to exit SmilePile mode
- No UI elements for mode switching visible
- Child-safe interaction patterns

## Implementation Plan

### Phase 1: Research (Atlas Research Agent)
- Analyze current KidsModeGalleryScreen
- Understand photo click navigation flow
- Review back button handling
- Check theme/status bar implementation

### Phase 2: Design (Atlas UI/UX Agent)
- Design minimal UI for SmilePile mode
- Plan gesture interactions
- Define PIN dialog flow
- Status bar theming approach

### Phase 3: Implementation (Atlas Developer Agent)
- Modify KidsModeGalleryScreen
- Update MainActivity for back handling
- Fix photo navigation
- Implement theme-aware status bar

### Phase 4: Testing (Atlas QA Agent)
- Test immersive experience
- Verify PIN protection
- Validate photo navigation
- Check theme consistency

## Files to Modify
1. `KidsModeGalleryScreen.kt` - Remove UI chrome
2. `MainActivity.kt` - Back button handling, status bar
3. `AppNavigation.kt` - Fix photo click navigation
4. `AppModeViewModel.kt` - PIN exit logic

## Risk Assessment
- **Low Risk:** UI hiding is straightforward
- **Medium Risk:** Back button interception needs careful handling
- **Low Risk:** Status bar theming is standard Android

## Success Metrics
- Zero UI chrome visible in Kids Mode
- 100% photo clicks open viewer
- PIN required for all exit attempts
- Status bar properly themed

## Notes
- SmilePile mode is an enhancement of Kids Mode, not a separate mode
- Focus on simplicity and child safety
- Maintain existing photo viewing functionality

---
*Created: $(date)*  
*Atlas Story: Post-Wave 4 Enhancement*