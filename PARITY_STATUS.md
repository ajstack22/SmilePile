# iOS vs Android Feature Parity Status

**Last Updated**: 2025-09-30

## ✅ COMPLETED: Onboarding Parity (100%)

All 24 onboarding chunks complete:
- Color constants matching Android
- Nunito font support (crisp rendering with static fonts)
- Multicolor SmilePile logo
- Welcome → Categories → PIN → Completion flow
- "Piles" terminology throughout
- Pixel-perfect matching with Android spec

## 🚧 IN PROGRESS: Main App Parity

### Current Status
- iOS onboarding: ✅ Complete
- Main app features: ⚠️ Needs audit

---

## 🗃️ PARKING LOT: Cleanup Items

### 1. Remove Parental Controls Screen (Both Platforms)

**Issue**: Both iOS and Android have a "Parental Controls" screen with Kid-Safe Mode and Delete Protection toggles that aren't actually needed.

**Android Files to Remove/Simplify**:
- `/android/app/src/main/java/com/smilepile/ui/screens/ParentalSettingsScreen.kt` - Entire screen
- `/android/app/src/main/java/com/smilepile/ui/components/settings/ContentControlsSection.kt` - Kid-Safe/Delete Protection toggles
- `/android/app/src/main/java/com/smilepile/ui/viewmodels/ParentalControlsViewModel.kt` - Related ViewModel
- Settings → Security: Remove "Parental Controls" menu item (line 226-231 in SettingsScreen.kt)
- Remove `kidSafeModeEnabled` and `deleteProtectionEnabled` from SecurePreferencesManager

**iOS Files to Remove/Simplify**:
- `/ios/SmilePile/ViewModels/ParentalControlsViewModel.swift` - Delete (just created, not in Xcode project yet)
- `/ios/SmilePile/Views/Settings/ParentalSettingsView.swift` - Delete (just created, not in Xcode project yet)
- `/ios/SmilePile/Views/Settings/ParentalControlsView.swift` - Already deprecated, can delete
- Remove from SettingsViewCustom.swift:
  - Line 14: `@State private var showingParentalSettings`
  - Lines 63-70: "Parental Controls" menu item
  - Lines 198-200: `.sheet(isPresented: $showingParentalSettings)`
- Remove from SettingsManager.swift:
  - Lines 62-63: kidSafeModeEnabled/deleteProtectionEnabled keys
  - Lines 176-177: @AppStorage properties
  - Lines 345-346: Default values

**What to Keep**:
- Settings → Security → "Set PIN" / "Change PIN" / "Remove PIN" (basic PIN management)
- Kids Mode toggle (already exists in Settings → Appearance)
- Biometric authentication toggle

**Rationale**: Kids Mode already prevents inadvertent changes by being read-only. No need for additional "Delete Protection" or separate "Kid-Safe Mode" toggle.

**Estimate**: 30 minutes per platform (1 hour total)

---

### 2. Backup/Restore System (iOS Only)

**Issue**: iOS backup/restore files are currently stubbed out to enable builds.

**Stubbed iOS Files**:
- BackupManager.swift
- BackupModels.swift
- ExportManager.swift
- RestoreManager.swift
- ZipUtils.swift
- PhotoImportManager.swift

**Action Required**: Re-implement from Android spec OR remove export/import menu items entirely if not needed.

**Android Reference**: `/android/app/src/main/java/com/smilepile/data/backup/`

**Estimate**: 8-12 hours (or 15 minutes to remove if not needed)

---

### 3. Terminology Audit (iOS Main App)

**Issue**: Need to verify "Piles" terminology is used everywhere in main app, not just onboarding.

**Files to Audit**:
- CategoryManagementView.swift
- PhotoGalleryView.swift
- OptimizedPhotoGalleryView.swift
- KidsModeGalleryView.swift
- All ViewModels referencing categories

**Action**: Global search for user-facing "Category" strings and replace with "Pile".

**Estimate**: 2-3 hours

---

## 📊 Priority Assessment

### P0 - NONE (No blockers!)
All critical features working.

### P1 - Low Priority Cleanup
1. Remove Parental Controls screen (both platforms) - 1 hour
2. Terminology audit (iOS) - 2-3 hours

### P2 - Can Defer
3. Backup/Restore (decide if needed) - 8-12 hours or remove

---

## 🎯 Recommendation

**Option A: Quick Cleanup (2-4 hours)**
1. Remove Parental Controls screen from both platforms (1 hour)
2. Terminology audit for iOS (2-3 hours)
3. Deploy to qual

**Option B: Skip for Now**
- Current state is functional
- Parking lot items are non-blocking
- Can address in future sprint if needed

**Option C: Focus on New Features**
- iOS and Android are now at parity for core features
- Onboarding pixel-perfect
- Move on to new feature development
