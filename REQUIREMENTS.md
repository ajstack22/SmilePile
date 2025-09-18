# SmilePile - Photo Gallery for Children

## Project Overview
Android photo gallery app designed for children's emotional self-regulation through curated photos. Built for tablets (10+ inch) with gesture-first navigation and visible category names to support early literacy.

## Core User Journey
1. Child opens app → Sees categories with names
2. Swipes between categories horizontally
3. Swipes up (or taps) to enter a category
4. Swipes through full-screen photos
5. Returns to category view

## ITERATIVE DEVELOPMENT PLAN

### Iteration 0: Minimal Working System (MWS)
**Goal**: Display one hardcoded image fullscreen
- [ ] Create Android project
- [ ] Display single image
- [ ] One basic test
- [ ] Must compile and run

### Iteration 1: Basic Swipe Navigation
**Goal**: Swipe between 3 hardcoded images
- [ ] Add ViewPager2
- [ ] Load 3 hardcoded images
- [ ] Implement swipe gestures
- [ ] Test swipe navigation

### Iteration 2: Load Images from Folder
**Goal**: Display all images from a folder
- [ ] Create sample images folder
- [ ] Dynamic image loading
- [ ] Handle empty folder case
- [ ] Test image loading

### Iteration 3: Category Concept
**Goal**: Group images into categories
- [ ] Category data structure
- [ ] Category view with titles
- [ ] Navigation into category
- [ ] Test category switching

### Iteration 4: Database Integration
**Goal**: Persist categories and photo metadata
- [ ] Room database setup
- [ ] Category and Photo entities
- [ ] Basic CRUD operations
- [ ] Test data persistence

### Iteration 5: Photo Import
**Goal**: Import photos from device storage
- [ ] File picker integration
- [ ] Permission handling
- [ ] Add photos to categories
- [ ] Test import flow

### Iteration 6: Parent Management UI
**Goal**: Category management for parents
- [ ] Create/edit/delete categories
- [ ] Reorder photos
- [ ] Set cover images
- [ ] Test management features

### Iteration 7: Polish & Performance
**Goal**: Production-ready app
- [ ] Image caching (Coil)
- [ ] Smooth animations
- [ ] Error handling
- [ ] Performance optimization

## Technical Requirements

### Platform
- Android 8.0+ (SDK 26+)
- Kotlin
- Tablet-optimized (10+ inch)

### Key Libraries (add as needed per iteration)
- ViewPager2 (swipe navigation)
- Room (database)
- Coil (image loading)
- Hilt (dependency injection - if needed)

### Performance Targets
- App launch: <2 seconds
- Image load: <500ms
- Swipe response: <100ms
- Database queries: <50ms

## Design Principles

### For Child User
- **Visible category names** (learning to read)
- **Minimal text** elsewhere
- **Gesture-first** (swipes primary, taps secondary)
- **Visual-heavy** (photos are the focus)
- **Works offline** (all local storage)
- **Child-safe** (no external sharing)

### For Parent User
- Text-based UI is fine for management features
- Clear organization tools
- Easy photo import
- Quick category setup

## Success Criteria
- ✅ Child can navigate without parent help
- ✅ Category names are readable and prominent
- ✅ Swipes respond smoothly
- ✅ No crashes during extended use
- ✅ Each iteration produces working software

## Development Rules

### CRITICAL: Iterative Development
1. **Always have working software** - Each iteration must compile and run
2. **Test as you go** - Each iteration adds tests
3. **Document as you build** - Update docs with each iteration
4. **Integrate immediately** - Don't build in isolation
5. **Validate before proceeding** - Must pass all checks

### Validation Gates (per iteration)
- [ ] Code compiles
- [ ] Tests pass
- [ ] Coverage adequate for iteration
- [ ] Documentation updated
- [ ] Git committed
- [ ] Can demo the feature

## Version Strategy
- Dev builds: YYYY.MM.DD.VVV (e.g., 2024.09.18.001)
- Display subtly in app (bottom-right, 50% opacity)

## Current Status
**Starting fresh with Iteration 0** - Previous attempt built components in isolation that didn't integrate. Now following strict iterative approach where each step builds on working code.