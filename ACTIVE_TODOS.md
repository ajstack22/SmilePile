# Active TODOs in SmilePile Codebase

## Android TODOs

### Backup System
- **BackupManager.kt:1166** - Implement getPhotosModifiedAfter in PhotoRepository
- **BackupManager.kt:1184** - Track deletions for photos
- **BackupManager.kt:1186** - Track deletions for categories
- **BackupManager.kt:1245** - Implement proper encryption when PIN/pattern backup is needed

### View Models
- **SettingsViewModel.kt:445** - WON'T FIX - Android handles image cache clearing automatically

### Tests
- (No active TODOs)

## iOS TODOs

### Backup System
- **BackupViewModel.swift:362** - Get category name for photo.categoryId

### Test Runner
- **.build/arm64-apple-macosx/debug/SmilePilePackageTests.derived/runner.swift:256** - Handle userInfo: [AnyHashable : Any]?

## Summary

**Total TODOs:** 6
- Android: 4
- iOS: 2

## Priority Areas
1. **Backup/Sync Features** - Both platforms need completion of deletion tracking and category handling
2. **Encryption** - PIN/pattern backup encryption needs implementation on Android