# SmilePile TODO List

**Last Updated**: 2025-09-30
**Project Status**: iOS onboarding complete, Android/iOS at parity

---

## 🎯 P0 - NONE (Critical items blocking users)

**All critical features are working!** ✅

---

## 📋 P1 - High Priority (Improve Quality)

### 1. Backup/Restore System (iOS)
**Status**: Stubbed out
**Issue**: iOS backup/export/import files are currently stubbed to enable builds.

**Stubbed Files**:
- `/ios/SmilePile/Data/Backup/BackupManager.swift` - "TODO: Port Android's working BackupManager.kt to iOS"
- `/ios/SmilePile/Data/Backup/BackupModels.swift` - Empty stub
- `/ios/SmilePile/Data/Backup/RestoreManager.swift` - "TODO: Port Android's working restore functionality to iOS"
- `/ios/SmilePile/Data/Backup/ZipUtils.swift` - "TODO: Fix Archive framework generic parameter inference"
- `/ios/SmilePile/ViewModels/BackupViewModel.swift` - "TODO: Create proper SettingsViewModel with export/import UI"

**Options**:
- **A) Implement**: Port from `/android/app/src/main/java/com/smilepile/data/backup/` (8-12 hours)
- **B) Remove**: Delete export/import menu items from Settings if not needed (15 minutes)

**Recommendation**: Ask user if backup/restore is needed. If not, remove the stubbed code and menu items.

---

### 2. Photo Import System (iOS)
**Status**: Stubbed out
**Issue**: Enhanced photo import with coordinators is stubbed.

**Stubbed Files**:
- `/ios/SmilePile/Data/Storage/PhotoImportManager.swift` - "TODO: Implement proper photo import with batch limits and duplicate detection"
- `/ios/SmilePile/Data/Storage/PhotoImportCoordinator.swift` - "TODO: Implement import session coordination and resumption" ✅ **REMOVED from PhotoGalleryView**
- `/ios/SmilePile/Data/Storage/EnhancedPhotoImportCoordinator.swift` - "TODO: Implement enhanced photo import with multi-size thumbnails"
- `/ios/SmilePile/Views/EnhancedPhotoImportView.swift` - "TODO: Fix PhotoImportManager integration"

**Current Status**: Photo import WORKS via `EnhancedPhotoPickerView` - these enhanced coordinators are optional improvements.

**Options**:
- **A) Implement**: Add batch limits, duplicate detection, multi-size thumbnails (6-8 hours)
- **B) Leave as-is**: Current photo import works fine, these are nice-to-haves

**Recommendation**: Leave as-is (current import works), or delete stubbed files if not planning to implement.

---

### 3. Terminology Audit - "Categories" → "Piles" (iOS Main App)
**Status**: Onboarding complete, main app needs audit
**Issue**: Need to verify "Piles" terminology used everywhere in main app, not just onboarding.

**Files to Audit**:
- `/ios/SmilePile/Views/CategoryManagementView.swift`
- `/ios/SmilePile/Views/OptimizedPhotoGalleryView.swift`
- `/ios/SmilePile/Views/KidsMode/KidsModeGalleryView.swift`
- All ViewModels referencing categories

**Action**: Global search for user-facing "Category" strings and replace with "Pile" (keep code-level variable names as "category").

**Estimate**: 2-3 hours

---

## 📝 P2 - Medium Priority (Code Quality)

### 4. CategoryManagementViewEnhanced.swift
**Status**: Stubbed out
**File**: `/ios/SmilePile/Views/CategoryManagementViewEnhanced.swift`
**Issue**: "TODO: Fix SwiftUI.EditMode vs custom EditMode enum usage"

**Options**:
- **A) Fix**: Resolve EditMode enum conflict
- **B) Remove**: Delete if not used (check if referenced)

---

### 5. Export/Import UI Placeholders (iOS Settings)
**Status**: Placeholder implementations
**Files**:
- `/ios/SmilePile/Views/SettingsViewNative.swift:186` - "// TODO: Implement export functionality"
- `/ios/SmilePile/Views/SettingsViewCustom.swift:164` - "// TODO: Implement export functionality"

**Current**: Shows "TODO: Implement export functionality" placeholder

**Action**: Either implement (see P1 #1) or remove export/import menu items if not needed.

---

## 🧹 P3 - Low Priority (Cleanup & Polish)

### 6. Compiler Warnings (iOS)
**Status**: 70+ warnings (non-blocking)

**Major Categories**:
1. **Non-sendable type capture in @Sendable closures** (SafeThumbnailGenerator.swift, PhotoOptimizer.swift, etc.)
   - Not urgent, but good practice for Swift 6 compatibility

2. **Left side of `??` has non-optional type** (many Color-related files)
   - Cosmetic, can remove unnecessary `?? .fallback` code

3. **Unused variables** (PhotoIDMigration.swift, CoreDataPublisher.swift, etc.)
   - Change `var` to `let` or remove if truly unused

4. **Deprecated APIs** (UIWindowScene.windows, etc.)
   - Update to modern iOS APIs when time allows

**Estimate**: 3-4 hours to fix all warnings

---

### 7. Duplicate Font Files
**Status**: Fonts exist in two locations
**Issue**: Nunito fonts duplicated:
- `/ios/SmilePile/*.ttf` (3 files)
- `/ios/SmilePile/Fonts/*.ttf` (3 files)

**Action**: Remove duplicates from root directory, keep in `/Fonts/` folder (5 minutes)

---

### 8. Broken/Backup Files
**Status**: Multiple `.broken`, `.backup`, `.broken2` files in codebase

**Files to Delete**:
- `/ios/SmilePile/Data/Storage/PhotoImportCoordinator.swift.broken`
- `/ios/SmilePile/Data/Storage/EnhancedPhotoImportCoordinator.swift.backup`
- `/ios/SmilePile/Data/Storage/EnhancedPhotoImportCoordinator.swift.broken2`
- `/ios/SmilePile/Data/Backup/BackupManager.swift.backup`
- `/ios/SmilePile/Data/Backup/RestoreManager.swift.backup`
- And more...

**Action**: Delete all `*.broken`, `*.backup`, `*.broken2` files (5 minutes)

---

## ✅ COMPLETED

### Onboarding (iOS)
- ✅ All 24 chunks complete
- ✅ Pixel-perfect matching with Android
- ✅ Crisp font rendering (Variable → Static fonts)
- ✅ CategorySetupScreen completely rebuilt (inline + button, horizontal color picker)
- ✅ "Piles" terminology throughout onboarding
- ✅ Removed PhotoImport from onboarding flow

### Parental Controls Cleanup
- ✅ Removed unnecessary Parental Controls screen (iOS & Android)
- ✅ Removed Kid-Safe Mode and Delete Protection toggles (not needed)
- ✅ Simplified to basic PIN management only

### PhotoImportCoordinator Fix
- ✅ Removed unused PhotoImportCoordinator from PhotoGalleryView.swift
- ✅ iOS now builds successfully

---

## 🎯 RECOMMENDED NEXT STEPS

### Option A: Quick Cleanup (4-5 hours)
1. ✅ **Decide on Backup/Restore**: Implement or remove? (User decision needed)
2. **Terminology Audit**: "Categories" → "Piles" in main app (2-3 hours)
3. **Delete broken/backup files** (5 min)
4. **Remove duplicate font files** (5 min)

### Option B: Focus on New Features
- iOS and Android are at parity for core features
- All P0/P1 items are either complete or awaiting user decision
- Can move on to new feature development

### Option C: Polish Pass (Full Day)
1. Fix all compiler warnings (3-4 hours)
2. Terminology audit (2-3 hours)
3. Delete broken/backup files (5 min)
4. Remove duplicate fonts (5 min)
5. Decide on backup/restore (implement or remove)

---

## 📊 SUMMARY

**Total TODO Items**: 8
**P0 (Critical)**: 0 ✅
**P1 (High)**: 3 (all awaiting decisions)
**P2 (Medium)**: 2
**P3 (Low)**: 3

**Key Decisions Needed**:
1. Should we implement or remove Backup/Restore? (P1 #1)
2. Should we implement or remove Enhanced Photo Import? (P1 #2)

**Blockers**: None! App is fully functional.

**Recommendation**: Ask user about backup/restore decision, then proceed with quick cleanup (Option A) or move to new features (Option B).
