# STORY-001: Backup & Restore - iOS Implementation

**Created**: 2025-10-01
**Status**: Ready for Implementation
**Estimated Effort**: 10-14 hours
**Agent Framework**: Product Manager → Developer → Peer Reviewer

---

## Executive Summary

Port the working Android Backup & Restore functionality to iOS to achieve platform parity. Users can export their entire SmilePile data (photos, categories, metadata) as a ZIP file and restore it later or on another device.

## User Story

**As a** SmilePile user
**I want to** export and import my entire photo collection with all categories and metadata
**So that** I can backup my data, migrate to a new device, or restore after reinstallation

## Business Context

- **Platform Parity**: Android has working Backup/Restore; iOS lacks this critical feature
- **User Trust**: Users expect data portability and backup options
- **Risk Mitigation**: Prevents data loss during app reinstallation or device migration
- **Competitive Feature**: Standard expectation for photo management apps

## Acceptance Criteria

### FR-1: Export Functionality
- [ ] User can tap "Export Data" in Settings → Data Management
- [ ] System creates ZIP file containing:
  - All photos from Documents directory
  - `backup_metadata.json` with categories, photo metadata, app settings
  - Manifest file with backup version and timestamp
- [ ] System presents iOS share sheet (UIActivityViewController)
- [ ] User can save to Files, AirDrop, email, or cloud storage
- [ ] Export completes successfully for up to 500 photos

### FR-2: Import Functionality
- [ ] User can tap "Import Data" in Settings → Data Management
- [ ] System presents file picker (UIDocumentPickerViewController)
- [ ] System validates ZIP file structure and metadata
- [ ] System shows confirmation dialog with backup details (date, photo count, category count)
- [ ] User confirms or cancels import
- [ ] System extracts photos to Documents directory
- [ ] System restores categories and metadata to CoreData
- [ ] System shows success message with import summary

### FR-3: Data Integrity
- [ ] All photos maintain original quality and EXIF data
- [ ] All categories restored with correct names, colors, and order
- [ ] All photo-category associations preserved
- [ ] Photo metadata (captions, dates, favorites) restored correctly
- [ ] App settings (PIN, Kids Mode preferences) restored

### FR-4: User Experience
- [ ] Clear progress indication during export/import
- [ ] Graceful error handling with user-friendly messages
- [ ] Warnings before overwriting existing data
- [ ] Option to cancel long-running operations
- [ ] No app crashes or data corruption under any scenario

### NFR-1: Performance
- [ ] Export completes in <30 seconds for 100 photos
- [ ] Import completes in <45 seconds for 100 photos
- [ ] Memory usage stays under 150MB during operations
- [ ] No UI freezing or ANR issues

### NFR-2: Compatibility
- [ ] Works on iOS 14.0+ (minimum deployment target)
- [ ] Backup format compatible with future iOS versions
- [ ] ZIP files readable by standard desktop tools

### NFR-3: Security
- [ ] No sensitive data exposed in ZIP file (PINs hashed, not plaintext)
- [ ] File operations respect iOS sandbox
- [ ] Proper cleanup of temporary files
- [ ] No data leakage through share sheet

---

## Technical Architecture

### Android Reference Implementation

**Files**:
- `BackupManager.kt` - Orchestrates backup creation
- `RestoreManager.kt` - Handles restore process
- `BackupModels.kt` - Data structures for metadata
- Native ZIP support via `java.util.zip`

**Flow**:
1. Collect photos from storage directory
2. Serialize metadata to JSON
3. Create ZIP with photos + metadata
4. Present share intent
5. Reverse process for restore with validation

### iOS Target Implementation

**Files to Create**:
- `ios/SmilePile/Data/Backup/BackupManager.swift`
- `ios/SmilePile/Data/Backup/RestoreManager.swift`
- `ios/SmilePile/Data/Backup/BackupModels.swift`
- `ios/SmilePile/Data/Backup/ZipUtils.swift`
- `ios/SmilePile/ViewModels/BackupViewModel.swift`

**Files to Modify**:
- `ios/SmilePile/Views/Settings/SettingsViewCustom.swift` (add Export/Import UI)
- `ios/SmilePile/Views/Settings/SettingsViewNative.swift` (add Export/Import UI)

**Dependencies**:
- ZIPFoundation (or native solution if available)
- FileManager (iOS SDK)
- CoreData (existing)
- UIActivityViewController (iOS SDK)
- UIDocumentPickerViewController (iOS SDK)

### Data Format Specification

**ZIP Structure**:
```
SmilePileBackup_2025-10-01_143022.zip
├── photos/
│   ├── IMG_001.jpg
│   ├── IMG_002.png
│   └── ...
├── backup_metadata.json
└── manifest.json
```

**backup_metadata.json**:
```json
{
  "version": "1.0",
  "timestamp": "2025-10-01T14:30:22Z",
  "categories": [
    {
      "id": 1,
      "displayName": "Family",
      "colorHex": "#FF5722",
      "orderIndex": 0,
      "isDefault": true
    }
  ],
  "photos": [
    {
      "id": 1,
      "path": "photos/IMG_001.jpg",
      "categoryId": 1,
      "caption": "Summer vacation",
      "timestamp": "2025-06-15T10:00:00Z",
      "isFavorite": false
    }
  ],
  "settings": {
    "hasPIN": true,
    "kidsModeEnabled": false
  }
}
```

**manifest.json**:
```json
{
  "backupVersion": "1.0",
  "appVersion": "1.0.0",
  "platform": "iOS",
  "timestamp": "2025-10-01T14:30:22Z",
  "photoCount": 150,
  "categoryCount": 5
}
```

---

## Implementation Plan

### Phase 0: Research & Validation (2-3 hours)

**Task 0.1: iOS API Research**
- [ ] Verify ZIPFoundation library status and best practices for iOS
- [ ] Check if Apple has native ZIP solution in iOS 14+
- [ ] Review FileManager API for any iOS 14+ changes
- [ ] Verify UIActivityViewController / UIDocumentPickerViewController current patterns
- [ ] Research iOS sandbox limitations for file operations
- [ ] Investigate Swift Concurrency patterns (async/await) for file I/O
- [ ] Check memory management best practices for large file operations
- [ ] Document any iOS-specific considerations

**Task 0.2: Android Code Analysis**
- [ ] Read `android/app/src/main/java/com/smilepile/data/backup/BackupManager.kt` thoroughly
- [ ] Read `android/app/src/main/java/com/smilepile/data/backup/RestoreManager.kt` thoroughly
- [ ] Read `android/app/src/main/java/com/smilepile/data/backup/BackupModels.kt` thoroughly
- [ ] Document Android backup flow step-by-step
- [ ] Document Android restore flow step-by-step
- [ ] Identify Android-specific code patterns that need iOS equivalents
- [ ] Map Kotlin Coroutines patterns to Swift async/await
- [ ] Map Android file system operations to iOS FileManager
- [ ] Map Android Intent system to iOS share sheet / document picker

**Task 0.3: Create Research Document**
- [ ] Document findings in `RESEARCH-BACKUP-RESTORE.md`
- [ ] List iOS-specific considerations and constraints
- [ ] Create Android-to-iOS equivalence mapping table
- [ ] Document recommended library choices (ZIPFoundation vs alternatives)
- [ ] Flag any potential blockers or unknowns
- [ ] Get Product Manager approval before proceeding to implementation

**Deliverable**: RESEARCH-BACKUP-RESTORE.md with iOS strategy and Android equivalence mapping

---

### Phase 1: Foundation (2-3 hours)

**Task 1.1: Create Data Models**
- [ ] Create `ios/SmilePile/Data/Backup/BackupModels.swift`
- [ ] Implement `BackupMetadata` struct (Codable)
- [ ] Implement `BackupCategory` struct matching Category model
- [ ] Implement `BackupPhoto` struct matching Photo model
- [ ] Implement `BackupSettings` struct for app preferences
- [ ] Implement `BackupManifest` struct
- [ ] Add JSON encoding/decoding tests in model file
- [ ] Verify Codable conformance compiles

**Task 1.2: Setup ZIP Utilities**
- [ ] Add ZIPFoundation dependency to project (or document alternative)
- [ ] Create `ios/SmilePile/Data/Backup/ZipUtils.swift`
- [ ] Implement `createZip(sourcePath:destinationPath:)` function
- [ ] Implement `extractZip(sourcePath:destinationPath:)` function
- [ ] Add error handling for disk space, permissions, corrupted files
- [ ] Add progress callback support for large operations
- [ ] Test with sample files (create, extract, verify)

**Task 1.3: Create Backup Directory Structure**
- [ ] Define constants for backup paths in BackupManager
- [ ] Implement `getBackupsDirectory()` function using FileManager
- [ ] Implement `createBackupWorkingDirectory()` function
- [ ] Implement `cleanupBackupWorkingDirectory()` function
- [ ] Ensure proper iOS sandbox compliance

**Grep Test**:
```bash
# Must find BackupModels.swift with all structs
rg "struct BackupMetadata" ios/SmilePile/Data/Backup/

# Must find ZipUtils.swift with create/extract functions
rg "func createZip" ios/SmilePile/Data/Backup/

# Must find BackupManager with directory functions
rg "func getBackupsDirectory" ios/SmilePile/Data/Backup/
```

---

### Phase 2: Export Implementation (3-4 hours)

**Task 2.1: Create BackupManager**
- [ ] Create `ios/SmilePile/Data/Backup/BackupManager.swift`
- [ ] Implement `createBackup()` async throws -> URL function
- [ ] Implement `collectPhotos()` async throws -> [Photo] (from PhotoRepository)
- [ ] Implement `collectCategories()` async throws -> [Category] (from CategoryRepository)
- [ ] Implement `collectSettings()` -> BackupSettings (from SettingsManager)
- [ ] Implement `createMetadataJSON()` throws -> Data
- [ ] Implement `createManifestJSON()` throws -> Data
- [ ] Implement `copyPhotosToBackupDirectory()` async throws with progress
- [ ] Implement `createBackupZip()` async throws -> URL
- [ ] Add comprehensive error handling and logging
- [ ] Add memory management (release photos after processing batches)

**Task 2.2: Create BackupViewModel**
- [ ] Create `ios/SmilePile/ViewModels/BackupViewModel.swift`
- [ ] Implement `@Published var isExporting: Bool`
- [ ] Implement `@Published var exportProgress: Double`
- [ ] Implement `@Published var exportMessage: String`
- [ ] Implement `@Published var exportError: Error?`
- [ ] Implement `exportData()` async function
- [ ] Call BackupManager.createBackup()
- [ ] Present UIActivityViewController with ZIP file
- [ ] Handle share sheet completion/cancellation
- [ ] Cleanup temporary files after share completes

**Task 2.3: Add Export UI**
- [ ] Modify `ios/SmilePile/Views/Settings/SettingsViewCustom.swift`
- [ ] Add "Data Management" section if not exists
- [ ] Add "Export Data" action item
- [ ] Add @StateObject for BackupViewModel
- [ ] Add loading overlay when isExporting == true
- [ ] Add progress indicator (exportProgress)
- [ ] Add error alert for exportError
- [ ] Add share sheet presentation
- [ ] Modify `ios/SmilePile/Views/Settings/SettingsViewNative.swift` (same changes)

**Grep Test**:
```bash
# Must find BackupManager with all core functions
rg "func createBackup" ios/SmilePile/Data/Backup/
rg "func collectPhotos" ios/SmilePile/Data/Backup/
rg "func createMetadataJSON" ios/SmilePile/Data/Backup/

# Must find BackupViewModel
rg "class BackupViewModel" ios/SmilePile/ViewModels/

# Must find Export UI in Settings
rg "Export Data" ios/SmilePile/Views/Settings/
```

---

### Phase 3: Import Implementation (3-4 hours)

**Task 3.1: Create RestoreManager**
- [ ] Create `ios/SmilePile/Data/Backup/RestoreManager.swift`
- [ ] Implement `restoreBackup(from: URL)` async throws function
- [ ] Implement `validateBackupZip()` async throws -> BackupManifest
- [ ] Implement `extractBackupZip()` async throws -> URL (temp directory)
- [ ] Implement `parseMetadataJSON()` throws -> BackupMetadata
- [ ] Implement `restoreCategories()` async throws
- [ ] Implement `restorePhotos()` async throws with progress
- [ ] Implement `restoreSettings()` throws
- [ ] Add validation for file structure, JSON schema, required fields
- [ ] Add conflict resolution (overwrite vs merge strategy)
- [ ] Add rollback mechanism if restore fails mid-process
- [ ] Add cleanup of temporary files

**Task 3.2: Update BackupViewModel for Import**
- [ ] Add `@Published var isImporting: Bool`
- [ ] Add `@Published var importProgress: Double`
- [ ] Add `@Published var importMessage: String`
- [ ] Add `@Published var importError: Error?`
- [ ] Add `@Published var showImportConfirmation: Bool`
- [ ] Add `@Published var backupManifest: BackupManifest?`
- [ ] Implement `importData()` async function
- [ ] Present UIDocumentPickerViewController
- [ ] Handle file selection
- [ ] Validate backup file
- [ ] Show confirmation dialog with manifest details
- [ ] Call RestoreManager.restoreBackup() on confirmation
- [ ] Handle success/error states
- [ ] Cleanup temporary files

**Task 3.3: Add Import UI**
- [ ] Modify `ios/SmilePile/Views/Settings/SettingsViewCustom.swift`
- [ ] Add "Import Data" action item in Data Management section
- [ ] Add loading overlay when isImporting == true
- [ ] Add progress indicator (importProgress)
- [ ] Add confirmation alert with backup details
- [ ] Add success alert
- [ ] Add error alert for importError
- [ ] Add document picker presentation
- [ ] Modify `ios/SmilePile/Views/Settings/SettingsViewNative.swift` (same changes)

**Grep Test**:
```bash
# Must find RestoreManager with all core functions
rg "func restoreBackup" ios/SmilePile/Data/Backup/
rg "func validateBackupZip" ios/SmilePile/Data/Backup/
rg "func restoreCategories" ios/SmilePile/Data/Backup/

# Must find Import additions in BackupViewModel
rg "var isImporting" ios/SmilePile/ViewModels/
rg "func importData" ios/SmilePile/ViewModels/

# Must find Import UI in Settings
rg "Import Data" ios/SmilePile/Views/Settings/
```

---

### Phase 4: Testing & Polish (2-3 hours)

**Task 4.1: Manual Testing**
- [ ] Test export with 0 photos (edge case)
- [ ] Test export with 1 photo
- [ ] Test export with 50 photos
- [ ] Test export with 200+ photos
- [ ] Verify ZIP file can be opened on macOS Finder
- [ ] Verify JSON files are valid (use jsonlint or jq)
- [ ] Test share sheet to Files app
- [ ] Test share sheet to AirDrop
- [ ] Test import of exported backup (round-trip)
- [ ] Test import with corrupted ZIP
- [ ] Test import with missing metadata.json
- [ ] Test import with invalid JSON
- [ ] Verify all photos restored with correct quality
- [ ] Verify all categories restored with correct colors/names
- [ ] Verify photo-category associations preserved
- [ ] Test canceling export mid-process
- [ ] Test canceling import mid-process
- [ ] Test with low disk space scenario
- [ ] Test with no photo library permissions

**Task 4.2: Performance Validation**
- [ ] Measure export time for 100 photos (must be <30s)
- [ ] Measure import time for 100 photos (must be <45s)
- [ ] Measure peak memory usage during export (must be <150MB)
- [ ] Measure peak memory usage during import (must be <150MB)
- [ ] Verify no memory leaks using Xcode Instruments
- [ ] Verify UI remains responsive during operations

**Task 4.3: Error Handling Review**
- [ ] Verify all error paths have user-friendly messages
- [ ] Verify no crashes on any error scenario
- [ ] Verify proper cleanup on all error paths
- [ ] Verify temporary files deleted even on cancellation/error
- [ ] Verify no data corruption on failed import

**Task 4.4: Build & Deploy**
- [ ] Run `xcodebuild` - must succeed with no errors
- [ ] Fix any compiler warnings introduced
- [ ] Run `./deploy/deploy_qual.sh ios` - must succeed
- [ ] Test on physical device if available

**Grep Test**:
```bash
# Must not find debug logs in production code
! rg "print\(\"DEBUG:" ios/SmilePile/Data/Backup/

# Must find error handling
rg "throws" ios/SmilePile/Data/Backup/BackupManager.swift
rg "catch" ios/SmilePile/Data/Backup/RestoreManager.swift
```

---

## Peer Review Checklist

### Code Quality
- [ ] All functions have clear, single responsibilities
- [ ] No code duplication between BackupManager and RestoreManager
- [ ] Error types are specific and actionable
- [ ] All async functions properly use await/async
- [ ] All file operations use FileManager correctly
- [ ] Memory is managed properly (no retain cycles, photos released after processing)
- [ ] No force unwraps (!) except where truly safe
- [ ] All optionals handled explicitly

### Data Integrity
- [ ] Exported photos match original quality (no compression artifacts)
- [ ] Exported JSON matches data models exactly
- [ ] Import restores all categories with correct properties
- [ ] Import restores all photos with correct associations
- [ ] Import restores settings correctly
- [ ] No data loss on round-trip (export → import)
- [ ] PINs are hashed, not plaintext in backup

### Error Handling
- [ ] All error paths tested and verified
- [ ] User sees helpful error messages (not technical jargon)
- [ ] No crashes on corrupted files
- [ ] No crashes on disk full
- [ ] No crashes on permission denial
- [ ] Temporary files cleaned up on all paths (success, error, cancel)
- [ ] Rollback works if import fails mid-process

### Performance
- [ ] Export completes in <30s for 100 photos (measured)
- [ ] Import completes in <45s for 100 photos (measured)
- [ ] Memory stays under 150MB (measured with Instruments)
- [ ] UI stays responsive (no ANR/freezing)
- [ ] Progress indicators update smoothly

### Security
- [ ] No sensitive data in plaintext (PINs hashed)
- [ ] File operations respect iOS sandbox
- [ ] Temporary files in secure location
- [ ] Proper file permissions set
- [ ] No data leakage through share sheet

### User Experience
- [ ] Export flow is intuitive (Settings → Data Management → Export)
- [ ] Import flow is intuitive (Settings → Data Management → Import)
- [ ] Progress indication is clear and accurate
- [ ] Success messages are encouraging
- [ ] Error messages are actionable
- [ ] Confirmation dialog shows meaningful info (photo count, date)
- [ ] Can cancel operations gracefully

### Build & Deployment
- [ ] `xcodebuild` passes with no errors
- [ ] No new compiler warnings
- [ ] `./deploy/deploy_qual.sh ios` succeeds
- [ ] Works on iOS 14.0+ (minimum target)
- [ ] Works on physical device (if tested)

### Documentation
- [ ] Code comments explain WHY not WHAT
- [ ] Complex algorithms have explanation comments
- [ ] Public functions have clear documentation
- [ ] Error types are documented

---

## Out of Scope

The following are explicitly NOT included in this story:

- Cross-platform backup (iOS ↔ Android compatibility)
- Selective backup (choosing specific categories)
- Incremental backup (only changed photos)
- Cloud backup integration (iCloud, Google Drive)
- Automatic scheduled backups
- Backup encryption (may add later)
- Backup history (multiple backup versions)
- Compression optimization beyond ZIP defaults
- Backup size limits or warnings

---

## Definition of Done

- [ ] All acceptance criteria (FR-1 through FR-4, NFR-1 through NFR-3) verified as PASS
- [ ] All Peer Review Checklist items verified as PASS
- [ ] All "Grep Test" commands pass
- [ ] Manual testing completed for all scenarios in Phase 4.1
- [ ] Performance validation completed (Phase 4.2) - all metrics met
- [ ] `xcodebuild` passes with no errors
- [ ] `./deploy/deploy_qual.sh ios` passes
- [ ] No new compiler warnings introduced
- [ ] RESEARCH-BACKUP-RESTORE.md completed and approved
- [ ] Product Manager validates feature in qual environment
- [ ] Peer Reviewer issues PASS verdict

---

## Agent Workflow

### Product Manager Responsibilities
- Define acceptance criteria (above)
- Review RESEARCH-BACKUP-RESTORE.md and approve iOS strategy
- Validate feature in qual environment after deployment
- Issue final GO/NO-GO for production

### Developer Responsibilities
- Complete Phase 0 (Research) and get PM approval before implementation
- Execute Phases 1-4 following implementation plan exactly
- Ensure all "Grep Tests" pass before marking phase complete
- Run xcodebuild and deploy_qual.sh before requesting peer review
- Pass peer review on first attempt (measure twice, cut once)

### Peer Reviewer Responsibilities
- Execute full Peer Review Checklist adversarially
- Verify all claims with evidence (run grep tests, check git diff, test manually)
- Issue verdict: 🔴 REJECTED / ⚠️ CONDITIONAL PASS / ✅ PASS
- If REJECTED: provide specific evidence and required fixes
- Do not accept claims without verification

---

## Notes

- This story follows the ATLAS agent-driven workflow
- Research phase (Phase 0) is MANDATORY before implementation due to iOS/Android platform differences
- Android implementation serves as reference but NOT direct translation
- iOS-specific patterns must be identified and documented in research phase
- All implementation decisions must be justified in RESEARCH-BACKUP-RESTORE.md
- No shortcuts - follow the plan phase-by-phase
