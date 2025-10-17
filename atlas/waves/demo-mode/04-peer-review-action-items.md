# Peer Review Action Items - Demo Mode
## Required Changes Before Implementation

### 🔴 CRITICAL - Must Fix

#### 1. Eliminate DemoModeManager Singleton
**Current Plan**: Creates new `DemoModeManager.shared` singleton
**Required Change**:
- Use existing `SettingsManager.isDemoMode` flag
- Let repositories handle filtering automatically
- No new manager classes

**Implementation**:
```swift
// Instead of:
DemoModeManager.shared.enterDemoMode()

// Use:
SettingsManager.shared.isDemoMode = true
// Repositories automatically filter by isFromAssets
```

#### 2. Reduce Bundle Size from 150MB to <30MB
**Current Plan**: 100 photos at 1280x720 = ~150MB
**Required Change**:
- Maximum 35 photos
- Resolution: 800x600
- Format: WebP for Android, HEIC for iOS where supported
- Expected size: ~25-30MB total

**Photo Distribution** (Revised):
- Milestones: 8 photos
- Birthdays: 5 photos
- Holidays: 6 photos
- Family: 4 photos
- Playtime: 4 photos
- Friends: 3 photos
- Creativity: 3 photos
- Adventures: 2 photos
- **Total: 35 photos**

#### 3. Simplify File Structure
**Current Plan**: 12 new files per platform
**Required Change**: Maximum 3 new files per platform

**iOS Files** (Revised):
1. `DemoData.swift` - Demo data definitions and photo metadata
2. `DemoModeBanner.swift` - Banner UI component
3. Demo photo assets in existing Assets.xcassets

**Android Files** (Revised):
1. `DemoData.kt` - Demo data definitions and photo metadata
2. `DemoModeBanner.kt` - Banner composable
3. Demo photo assets in existing drawable resources

---

### ⚠️ HIGH PRIORITY - Should Fix

#### 4. Fix Repository Pattern Usage
**Current Plan**: New methods `loadDemoPhotos()`, `getDemoPhotos()`
**Required Change**: Use existing repository methods with isFromAssets filtering

**Implementation**:
```swift
// iOS - PhotoRepositoryImpl.swift
func getAllPhotos() async throws -> [Photo] {
    let allPhotos = try await fetchPhotos()
    if SettingsManager.shared.isDemoMode {
        return allPhotos.filter { $0.isFromAssets }
    }
    return allPhotos.filter { !$0.isFromAssets }
}
```

```kotlin
// Android - PhotoRepositoryImpl.kt
override suspend fun getAllPhotos(): List<Photo> {
    val allPhotos = photoDao.getAll().first()
    return if (prefsManager.isDemoMode) {
        allPhotos.filter { it.isFromAssets }
    } else {
        allPhotos.filter { !it.isFromAssets }
    }
}
```

#### 5. Implement Progressive Loading
**Current Plan**: Load all 100 photos at once
**Required Change**: Load progressively

**Implementation**:
```swift
// Load first 10 immediately for UI
let firstBatch = demoPhotos.prefix(10)
await loadPhotos(firstBatch)

// Load remaining in background
Task.detached(priority: .background) {
    let remaining = Array(demoPhotos.dropFirst(10))
    await loadPhotos(remaining)
}
```

#### 6. Simplify Demo Mode Entry
**Current Plan**: Complex async state management
**Required Change**: Simple synchronous flag with async data load

**Implementation**:
```swift
// OnboardingCoordinator.swift
func enterDemoMode() {
    // 1. Set flag synchronously
    SettingsManager.shared.isDemoMode = true

    // 2. Skip to gallery
    isComplete = true

    // 3. Load demo data in background
    Task {
        await loadDemoDataIfNeeded()
    }
}

private func loadDemoDataIfNeeded() async {
    // Check if demo data already loaded
    let photoCount = try? await PhotoRepositoryImpl().getPhotoCount()
    if photoCount ?? 0 > 0 { return }

    // Load demo data from DemoData.swift
    for photo in DemoData.photos {
        // Insert with isFromAssets = true
    }
}
```

---

### 💡 RECOMMENDED - Nice to Have

#### 7. Add Performance Monitoring
```swift
// Track demo mode performance
let startTime = Date()
await loadDemoData()
let loadTime = Date().timeIntervalSince(startTime)
Analytics.track("demo_mode_load_time", value: loadTime)
```

#### 8. Add Storage Space Check
```swift
func canEnterDemoMode() -> Bool {
    let requiredSpace: Int64 = 30 * 1024 * 1024 // 30MB
    let availableSpace = getAvailableStorageSpace()
    return availableSpace > requiredSpace
}
```

#### 9. Implement State Machine for Transitions
```swift
enum DemoModeState {
    case idle
    case entering
    case active
    case exiting
}

// Prevent concurrent state changes
```

---

## Implementation Timeline (Revised)

### Day 1 (6 hours)
- ✅ Add isDemoMode flag to SettingsManager
- ✅ Create DemoData definitions
- ✅ Prepare 35 demo photos (resize, optimize)
- ✅ Modify repositories for isFromAssets filtering

### Day 2 (6 hours)
- ✅ Add "Try Demo" button to WelcomeScreen
- ✅ Create DemoModeBanner component
- ✅ Implement demo mode entry logic
- ✅ Implement demo mode exit logic

### Day 3 (4 hours)
- ✅ Add restrictions for edit actions
- ✅ Test both platforms
- ✅ Fix issues
- ✅ Verify platform parity

**Total: 16 hours** (vs 29 hours original estimate)

---

## Code Review Checklist

Before submitting for Phase 5 implementation:

- [ ] No new manager singletons created
- [ ] Maximum 3 new files per platform
- [ ] Demo photos ≤35 total
- [ ] Bundle size increase <30MB
- [ ] Using existing repository pattern
- [ ] Using existing settings pattern
- [ ] Progressive loading implemented
- [ ] Error handling in place
- [ ] Platform parity verified
- [ ] All tests passing

---

## Summary

The peer review identified significant overengineering in the technical plan. By following these action items, we can:

1. **Reduce implementation time** from 29 to 16 hours
2. **Reduce bundle size** from 150MB to <30MB
3. **Eliminate technical debt** before it's created
4. **Maintain codebase simplicity** per SmilePile standards
5. **Improve performance** through progressive loading

The key principle: **Extend existing systems, don't create parallel ones.**