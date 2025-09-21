# Wave 7: Kids Mode Redesign & PIN Simplification

## Story Overview
Transform SmilePile into a truly kid-safe photo viewing app by redesigning the Kids Mode as the primary interaction model. The app will have two distinct modes: Kids Mode (default, simplified) and Parent Mode (photo/category management). PIN protection moves to main settings for easier access, and Kids Mode becomes the prominent toggle via a primary FAB.

## Core Features (Parent Mode Only)
- **Photo Management**: Add photos from gallery, delete photos, assign categories
- **Category Management**: Create, edit (name/color), and delete categories
- **Settings**: PIN management, theme switching

## Removed Features (Not Prioritized)
- Import/Export functionality
- Favorites system
- Batch operations
- Sharing functionality

## User Story
**As a parent**, I want my young children to be able to safely browse family photos without accidentally deleting them or accessing settings, while I can easily toggle between kids and parent modes with proper PIN protection for managing photos and categories.

## Acceptance Criteria

### 1. Kids Mode as Primary Experience
- [ ] Kids Mode is enabled by default when app launches
- [ ] Primary FAB shows Kids Mode toggle (replaces current photo import FAB)
- [ ] When in Kids Mode, FAB shows lock icon to enter Parent Mode
- [ ] When in Parent Mode, FAB shows child icon to return to Kids Mode
- [ ] Entering Parent Mode requires PIN authentication

### 2. Kids Mode Interface
- [ ] Only shows photo grid and categories
- [ ] No Settings button visible
- [ ] No Gallery management options
- [ ] No delete functionality available
- [ ] No photo selection mode
- [ ] Simplified navigation - only viewing photos and switching categories
- [ ] All photos are automatically "protected" from deletion

### 3. Parent Mode Interface
- [ ] Core photo management features:
  - Add photos from gallery
  - Delete photos
  - Set photo categories
- [ ] Category management features:
  - Create new categories
  - Edit category names/colors
  - Delete categories
- [ ] Settings accessible from navigation
- [ ] Add Photo FAB becomes secondary action button

### 4. PIN Management Relocation
- [ ] Move PIN setup/change to main Settings screen
- [ ] Remove separate Parental Controls screen
- [ ] PIN settings include:
  - Set/Change PIN option
  - Reset PIN functionality
  - Toggle "Require PIN for Parent Mode"
- [ ] Remove pattern lock option (simplify to PIN only)

### 5. Visual & UX Changes
- [ ] Kids Mode FAB: Prominent, colorful, child-friendly icon
- [ ] Parent Mode indicator: Subtle badge or color change in app bar
- [ ] Smooth transition animations between modes
- [ ] Clear visual feedback when entering/exiting Kids Mode

## Technical Implementation Plan

### Phase 1: State Management Setup
**Files to modify:**
- Create `ModeManager.kt` - Singleton to manage app mode state
- Update `MainActivity.kt` - Observe mode changes
- Create `AppModeViewModel.kt` - Handle mode transitions

**Tasks:**
1. Create ModeManager with Kids/Parent mode state
2. Add mode observation to MainActivity
3. Implement mode persistence in SharedPreferences

### Phase 2: PIN Relocation
**Files to modify:**
- `SettingsScreen.kt` - Add PIN management section
- `SecurePreferencesManager.kt` - Simplify to PIN-only
- Remove `ParentalSettingsScreen.kt`
- Remove `ParentalLockScreen.kt` (repurpose as modal)

**Tasks:**
1. Add PIN settings to main Settings screen
2. Create PIN setup/change dialogs
3. Remove pattern lock functionality
4. Simplify SecurePreferencesManager

### Phase 3: Kids Mode Implementation
**Files to modify:**
- `PhotoGalleryScreen.kt` - Conditional UI based on mode
- `AppNavigation.kt` - Mode-aware navigation
- Create `KidsModeGalleryScreen.kt` - Simplified gallery

**Tasks:**
1. Create simplified Kids Mode gallery view
2. Hide settings/management features in Kids Mode
3. Implement deletion protection
4. Remove selection mode in Kids Mode

### Phase 4: FAB Redesign
**Files to modify:**
- `PhotoGalleryScreen.kt` - Dual FAB system
- Create `ModeSwitchFAB.kt` - Primary mode toggle
- Create `AddPhotoFAB.kt` - Secondary add photo button

**Tasks:**
1. Implement primary Kids Mode toggle FAB
2. Add secondary Add Photo FAB for Parent Mode
3. Create PIN authentication dialog for mode switch
4. Add transition animations

### Phase 5: Navigation Updates
**Files to modify:**
- `AppNavigation.kt` - Mode-based routing
- `NavigationRoutes.kt` - Add mode parameters
- Remove parental control routes

**Tasks:**
1. Implement mode-based navigation guards
2. Remove parental control navigation paths
3. Add mode transition routes
4. Update bottom navigation visibility

## Atlas Orchestration Structure

```kotlin
// atlas/wave-7-orchestration.kt
class Wave7Orchestration {
    // Parallel execution groups
    val phase1Tasks = listOf(
        Task("Create ModeManager", "infrastructure"),
        Task("Setup mode state flows", "infrastructure"),
        Task("Add mode persistence", "infrastructure")
    )

    val phase2Tasks = listOf(
        Task("Relocate PIN to Settings", "ui"),
        Task("Remove pattern lock", "cleanup"),
        Task("Simplify SecurePreferences", "refactor")
    )

    val phase3Tasks = listOf(
        Task("Create Kids Mode UI", "ui"),
        Task("Implement view restrictions", "ui"),
        Task("Add deletion protection", "security")
    )

    val phase4Tasks = listOf(
        Task("Create mode toggle FAB", "ui"),
        Task("Add PIN authentication", "security"),
        Task("Implement FAB animations", "ui")
    )
}
```

## Success Metrics
- Kids can safely browse photos without risk
- Parents can quickly toggle between modes
- PIN protection is easily accessible
- Mode switching is intuitive
- No accidental deletions in Kids Mode

## Testing Requirements
1. **Kids Mode Safety**
   - Verify no deletion possible
   - Confirm settings inaccessible
   - Test category navigation only

2. **Mode Switching**
   - PIN required for Parent Mode
   - Smooth transitions
   - State persistence on app restart

3. **Parent Mode**
   - Photo management works (add, delete, categorize)
   - Category management works (create, edit, delete)
   - Settings changes persist

4. **Edge Cases**
   - App backgrounding during mode switch
   - PIN failure handling
   - Mode state after app updates

## Rollback Plan
If issues arise:
1. Disable Kids Mode toggle via feature flag
2. Restore original FAB functionality
3. Re-enable Parental Settings screen
4. Maintain PIN in secure preferences

## Future Enhancements
- Age-appropriate content filtering
- Time limits for Kids Mode
- Multiple child profiles
- Educational photo activities
- Parental usage reports