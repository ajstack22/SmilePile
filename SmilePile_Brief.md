# SmilePile - Quick Brief

## What
Android photo gallery app for children's self-regulation through curated calming photos. Designed for a child learning to read, with visible category names to support literacy development.

## Core User Flow
Child swipes between categories (sees category names) → Swipes up to enter → Swipes through photos → Returns to categories

## Key Design Principles
- **Category titles visible** (child is learning to read)
- **Minimal other text** (except import/management features for parent)
- **Gesture-first navigation** (swipes primary, taps secondary)
- **Visual-heavy interface** (photos are the focus)
- **Works offline** (all local storage)
- **Child-safe** (no external sharing/internet)
- **Tablet-first** (10+ inch screens)

## POC Must-Haves
1. Category browsing with visible titles (horizontal swipe)
2. Enter category (swipe up or tap)
3. Photo viewing (full-screen swipe navigation)
4. Basic import from device storage
5. Category name display (large, readable font)

## Tech Stack
- Android Native (Kotlin)
- Min SDK 26 (Android 8.0)
- ViewPager2 for swipe navigation
- Room database for metadata
- Coil/Glide for image loading
- Local storage only

## MVP Scope (Phase 1)
- ✅ Category management with cover images and titles
- ✅ Photo import via file picker
- ✅ Swipe navigation between categories
- ✅ Full-screen photo viewing
- ✅ Return to category navigation

## Success Criteria
- Child can navigate without parent help
- Category names are readable and helpful
- Swipes respond smoothly (<100ms)
- Photos load quickly (<500ms)
- App launches fast (<2 seconds)
- No crashes during 30+ minute sessions

## Parent Features (Text-Heavy OK)
- Create/edit/delete categories
- Import photos from device
- Organize photos into categories
- Set cover images
- Manage storage

## Key Requirements Reference
- REQ-001: Full-screen category covers
- REQ-002: Horizontal swipe navigation
- REQ-003: Swipe-up to enter category
- REQ-004: Custom category names (VISIBLE)
- REQ-011: Full-screen photo viewing
- REQ-016: Large, clear visual targets
- REQ-031: Core features work without reading (except category names)