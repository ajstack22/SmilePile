# SmilePile iOS Product Roadmap

## Executive Summary
SmilePile iOS has successfully implemented Kids Mode with core features working. The app now needs to transition from mock data to real photo management capabilities to become a viable product for families.

## Current State Assessment

### What's Working
- Kids Mode toggle with PIN protection
- Fullscreen photo viewer with gestures
- Category-based photo organization
- Toast notifications for category changes
- Security (PIN) system
- SmilePile branding and color scheme
- Edge-to-edge display support

### Critical Gaps
- **No real photos** - App uses mock data only
- **No photo import** - Users cannot add their photos
- **No persistence** - Categories and settings don't save
- **No photo library access** - Can't import from device
- **Limited testing** - Only on emulators, not real devices

## Prioritized Feature Roadmap

### Priority 1: Photo Import & Management
**Effort:** Large (3-4 days)
**Justification:** Without real photos, the app has no value to users. This is the foundation for everything else.

**Tasks:**
- Implement photo library permissions and access
- Create photo picker UI (PhotosUI framework)
- Connect StorageManager to actually save photos
- Wire up PhotoRepository with CoreData persistence
- Create thumbnail generation for performance
- Add basic photo metadata (date, size, etc.)

**Success Metrics:**
- User can import 10+ photos from library
- Photos persist between app launches
- Thumbnails load within 100ms
- Import success rate > 95%

**Dependencies:** None - this is foundational

---

### Priority 2: Category Management & Assignment
**Effort:** Medium (2 days)
**Justification:** Parents need to organize photos into categories for Kids Mode to be meaningful.

**Tasks:**
- Create category CRUD operations in CoreData
- Build category assignment UI in Parent Mode
- Implement batch photo categorization
- Add default categories on first launch
- Create category color customization

**Success Metrics:**
- Parents can create/edit/delete categories
- Assign photos to categories in < 3 taps
- Categories persist across sessions
- Kids see only categorized photos

**Dependencies:** Priority 1 (need real photos to categorize)

---

### Priority 3: Settings & Preferences Persistence
**Effort:** Small (1 day)
**Justification:** User settings must persist for the app to feel polished and reliable.

**Tasks:**
- Implement UserDefaults for app preferences
- Save PIN securely in Keychain
- Persist selected categories
- Remember Kids Mode state
- Store photo sort preferences

**Success Metrics:**
- All settings persist after app restart
- PIN remains secure in Keychain
- Last used category auto-selects
- Preferences load in < 50ms

**Dependencies:** None (can parallelize with Priority 2)

---

### Priority 4: Performance Optimization
**Effort:** Medium (2 days)
**Justification:** With real photos, performance becomes critical for user experience.

**Tasks:**
- Implement lazy loading for photo grid
- Add image caching strategy
- Optimize CoreData queries
- Implement background thumbnail generation
- Add loading states and placeholders

**Success Metrics:**
- Gallery scrolls at 60fps with 1000+ photos
- Photos load within 200ms
- Memory usage < 150MB for typical use
- No UI freezes during photo import

**Dependencies:** Priority 1 (need real photos to optimize)

---

### Priority 5: Backup & Export
**Effort:** Medium (2-3 days)
**Justification:** Parents need data safety and portability for their memories.

**Tasks:**
- Implement ZIP-based backup format
- Create export UI in Settings
- Add import/restore functionality
- Include categories and metadata in backup
- Add progress indicators

**Success Metrics:**
- Backup 1000 photos in < 30 seconds
- Restore maintains all categories
- Export file shareable via standard iOS share sheet
- 100% data integrity on restore

**Dependencies:** Priorities 1 & 2 (need data to backup)

---

### Priority 6: Device Testing & Polish
**Effort:** Medium (2 days)
**Justification:** Real device testing reveals issues emulators don't catch.

**Tasks:**
- Test on various iPhone models (SE to Pro Max)
- Verify camera notch/Dynamic Island handling
- Test with large photo libraries (5000+ photos)
- Optimize for different screen sizes
- Fix any gesture conflicts

**Success Metrics:**
- Works on all iOS 17+ devices
- No UI clipping on any screen size
- Gestures work reliably on all devices
- Performance acceptable on iPhone SE

**Dependencies:** Priorities 1-4 (need full functionality to test)

---

### Priority 7: Sharing & Collaboration (Future)
**Effort:** Large (4-5 days)
**Justification:** Families often want to share photo collections between devices.

**Tasks:**
- Design sharing architecture (iCloud/custom)
- Implement family sharing setup
- Add selective photo sharing
- Create receive/import flow
- Add conflict resolution

**Success Metrics:**
- Share album in < 30 seconds
- Multiple devices stay in sync
- Clear permission model
- No data loss during sync

**Dependencies:** All previous priorities

---

## Risk Mitigation

### Technical Risks
1. **Photo Library Performance** - Large libraries (10,000+ photos) may cause performance issues
   - Mitigation: Implement pagination and virtual scrolling early

2. **Storage Management** - Users may run out of device storage
   - Mitigation: Add storage warnings and cleanup tools

3. **CoreData Migration** - Schema changes could lose user data
   - Mitigation: Implement versioned migration strategy from start

### User Experience Risks
1. **Complex Setup** - Parents may find initial setup overwhelming
   - Mitigation: Add guided onboarding flow

2. **Accidental Deletion** - Parents might delete photos unintentionally
   - Mitigation: Add trash/recovery feature

3. **Kids Mode Escape** - Kids might find ways around PIN
   - Mitigation: Test with actual children, iterate on security

## Next Immediate Actions

1. **Today**: Start implementing photo library access and import
2. **Tomorrow**: Wire up StorageManager with real photo data
3. **Day 3**: Connect PhotoRepository to CoreData
4. **Day 4**: Test with real photos on physical device
5. **Day 5**: Begin category persistence work

## Success Criteria for MVP

- Parents can import photos from their library
- Photos can be organized into categories
- Kids Mode shows only appropriate photos
- All data persists between sessions
- App performs well with 500+ photos
- Works reliably on iPhone 12 and newer

## Timeline Estimate

**MVP Ready**: 7-10 days of focused development
**App Store Ready**: Additional 5-7 days for polish and testing
**Total**: 2-3 weeks to market-ready product

---

*Note: This roadmap prioritizes core functionality over advanced features. The focus is on delivering a reliable, useful product that families can start using immediately.*