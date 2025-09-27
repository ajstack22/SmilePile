# Active TODOs in SmilePile Codebase

**Last Updated:** September 27, 2025 (Sprint 6 Clean-up)

## Sprint 6 Status Update

### Completed in Sprint 6 ✅
- **BackupManager.kt:1245** - ✅ COMPLETED - PIN/pattern backup encryption implemented with PBKDF2 (600,000 iterations) and AES-256-GCM
- Kids Mode time limits and rewards system implemented with security enhancements

### Sprint 6 Incomplete Items (Carried to Sprint 6.1)

## Android TODOs

### Backup System - P0 Priority
- **BackupManager.kt:1166** - Implement getPhotosModifiedAfter in PhotoRepository
- **BackupManager.kt:1184** - Track deletions for photos (PARTIALLY IMPLEMENTED - needs 30-day retention)
- **BackupManager.kt:1186** - Track deletions for categories (PARTIALLY IMPLEMENTED - needs 30-day retention)
- **NEW: Deletion Tracking** - Complete 30-day retention window implementation
- **NEW: Deletion Tracking** - Implement sync behavior across devices
- **NEW: Deletion Tracking** - Add user-viewable deletion history
- **NEW: Deletion Tracking** - Implement selective restore of deleted items

### Security - P1 Priority
- **NEW: Key Rotation** - Implement automatic key rotation mechanism
- **NEW: Audit Logging** - Add comprehensive security event logging

### Testing - P0 Priority
- **NEW: Integration Tests** - Add backup/restore integration tests
- **NEW: Security Tests** - Implement penetration testing
- **NEW: Cross-platform Tests** - Add sync testing between platforms

### View Models
- **SettingsViewModel.kt:445** - WON'T FIX - Android handles image cache clearing automatically

## iOS TODOs

### Backup System - P0 Priority (CRITICAL)
- **BackupViewModel.swift:362** - Get category name for photo.categoryId
- **NEW: STORY-6.3** - Implement complete iOS category resolution logic
- **NEW: STORY-6.3** - Add category hierarchy preservation
- **NEW: STORY-6.3** - Implement category-photo associations
- **NEW: STORY-6.3** - Add orphaned category handling
- **NEW: STORY-6.3** - Implement data integrity checks

### Kids Mode - P1 Priority
- **NEW: Time Limits UI** - Complete iOS UI for time limit settings
- **NEW: Rewards UI** - Complete sticker collection UI
- **NEW: Parent Controls** - Implement parent control panel

### Testing - P0 Priority
- **NEW: iOS Integration** - Full integration testing for backup encryption
- **NEW: Kids Mode Testing** - Complete integration testing

### Test Runner
- **.build/arm64-apple-macosx/debug/SmilePilePackageTests.derived/runner.swift:256** - Handle userInfo: [AnyHashable : Any]?

## Critical Issues from Sprint 6 Validation

### P0 - MUST FIX (Blocks Release)
1. **iOS Category Resolution Missing** - Backup/restore will corrupt category data
2. **Deletion Tracking Incomplete** - Data loss possible during restore
3. **Integration Tests Missing** - Unknown edge cases and failure modes
4. **Backup/Restore End-to-End** - Complete flow validation needed

### P1 - SHOULD FIX (Major Issues)
1. **Key Rotation Missing** - Long-term security vulnerability
2. **Audit Logging Incomplete** - Cannot detect/investigate breaches
3. **Kids Mode UI Gaps** - User confusion with partial features
4. **Cross-platform Sync Tests** - Sync issues likely

## Summary

**Total TODOs:** 25 (was 6)
- Android: 12 (was 4)
- iOS: 13 (was 2)
- **New from Sprint 6:** 19 items

**Sprint 6 Completion Rate:** 50% (3 of 6 stories)

## Priority Areas for Sprint 6.1
1. **iOS Category Resolution** - CRITICAL - Must complete before any deployment
2. **Deletion Tracking Completion** - HIGH - Data loss risk
3. **Integration Testing** - HIGH - Unknown failure modes
4. **Security Hardening** - MEDIUM - Key rotation and audit logging
5. **Kids Mode UI Completion** - MEDIUM - Feature completeness

## Risk Assessment
- **CRITICAL:** iOS backup/restore will fail without category resolution
- **HIGH:** Incomplete deletion tracking may cause data loss
- **MEDIUM:** Security improvements delivered but gaps remain