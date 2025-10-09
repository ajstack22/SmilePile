# iOS Backup/Restore User Story

**Date**: 2025-10-08
**Feature**: iOS Backup/Restore UI Integration
**Status**: Story Complete - Ready for Planning
**Related**: Research Report (01-research-report.md)

---

## User Story

**AS A** SmilePile iOS user
**I WANT TO** export my photos and categories to a backup file and restore them later
**SO THAT** I can preserve my memories, migrate to a new device, or recover from data loss

---

## Acceptance Criteria

### Export Functionality
- [ ] **AC-1**: User can tap "Export Data" button in Settings
- [ ] **AC-2**: Export progress dialog appears showing:
  - Current operation text (e.g., "Collecting categories...", "Copying photos...")
  - Progress indicator (circular spinner or percentage)
  - Progress counter (e.g., "Progress: 15/100")
  - Modal dialog that prevents dismissal during export
- [ ] **AC-3**: Export creates a timestamped ZIP file (e.g., `smilepile_backup_20251008_143052.zip`)
- [ ] **AC-4**: After export completes, iOS ShareSheet appears allowing user to:
  - Save to Files app
  - Share via AirDrop
  - Email the backup
  - Save to iCloud Drive
  - Use any other share destination
- [ ] **AC-5**: Export includes all data:
  - Photos (full resolution)
  - Thumbnails
  - Categories
  - Photo-category assignments
  - Settings (theme, security preferences)
  - Metadata (creation dates, categories)

### Import Functionality
- [ ] **AC-6**: User can tap "Import Data" button in Settings
- [ ] **AC-7**: iOS document picker appears filtered to show `.zip` files
- [ ] **AC-8**: After user selects file, validation occurs showing:
  - "Validating backup..." message
  - Progress indicator during validation
- [ ] **AC-9**: If backup is valid, confirmation dialog appears showing:
  - Title: "Restore Backup?"
  - Backup details: "X photos, Y categories"
  - "Cancel" button
  - "Restore" button
- [ ] **AC-10**: If backup is invalid, error alert appears with:
  - Clear error message (e.g., "Invalid backup file", "Unsupported version")
  - "OK" button to dismiss
- [ ] **AC-11**: After user confirms restore, import progress dialog appears showing:
  - Current operation text (e.g., "Restoring categories...", "Importing photos...")
  - Progress indicator
  - Progress counter (e.g., "Progress: 42/150")
  - Modal dialog during import
- [ ] **AC-12**: Import uses MERGE strategy:
  - New categories are added
  - Existing categories are preserved
  - Duplicate photos are skipped (not re-imported)
  - No data is deleted
- [ ] **AC-13**: After successful import, success dialog appears showing:
  - "Import Complete"
  - Summary: "X photos imported successfully"
  - "OK" button to dismiss

### Progress Feedback
- [ ] **AC-14**: Export progress updates at least every 5 photos
- [ ] **AC-15**: Import progress updates at least every 5 photos
- [ ] **AC-16**: Progress dialogs are modal (cannot be dismissed while in progress)
- [ ] **AC-17**: Progress messages are specific to current operation:
  - "Collecting categories..."
  - "Copying photos..."
  - "Creating backup archive..."
  - "Restoring categories..."
  - "Importing photos..."
  - "Processing metadata..."

### Error Handling
- [ ] **AC-18**: Export errors show alert with:
  - Title: "Export Error"
  - Error message (user-friendly, not technical)
  - "OK" button to dismiss
- [ ] **AC-19**: Import errors show alert with:
  - Title: "Import Error"
  - Error message
  - "OK" button to dismiss
- [ ] **AC-20**: Validation errors are specific:
  - "Invalid backup file" - not a valid ZIP
  - "Unsupported version" - backup from incompatible app version
  - "Corrupted backup" - ZIP is damaged
- [ ] **AC-21**: Storage errors are clear:
  - "Insufficient storage space"
  - "Cannot access file"
  - "Permission denied"
- [ ] **AC-22**: All errors allow user to retry the operation

### Cross-Platform Compatibility
- [ ] **AC-23**: iOS-created backups can be imported on Android
- [ ] **AC-24**: Android-created backups can be imported on iOS
- [ ] **AC-25**: Backup format is ZIP containing:
  - `metadata.json` (categories, settings, photo metadata)
  - Photo files in root directory (named by ID)
  - Thumbnail files in root directory (named by ID with `-thumb` suffix)
- [ ] **AC-26**: Backup validation checks:
  - File is valid ZIP format
  - Contains `metadata.json`
  - Metadata format is correct
  - Version compatibility

### User Experience Requirements
- [ ] **AC-27**: Export completes in under 30 seconds for 100 photos
- [ ] **AC-28**: Import completes in under 60 seconds for 100 photos
- [ ] **AC-29**: Progress feedback appears within 500ms of operation start
- [ ] **AC-30**: All dialogs have appropriate titles and are dismissible when safe
- [ ] **AC-31**: ShareSheet cleanup: Temporary export file is deleted after sharing completes
- [ ] **AC-32**: User can cancel document picker without error
- [ ] **AC-33**: User can cancel import confirmation without error

---

## Technical Requirements

### SettingsViewCustom Integration

**Current State**:
- Lines 89-104: Export/Import action items with stub implementations
- Lines 165-205: Placeholder sheets with TODO comments
- No BackupViewModel integration

**Required Changes**:

#### 1. Add BackupViewModel
```swift
@StateObject private var backupViewModel = BackupViewModel()
```

#### 2. Wire Export Action (Line 93)
```swift
action: { backupViewModel.exportData() }
```

#### 3. Wire Import Action (Line 103)
```swift
action: { backupViewModel.showFilePicker() }
```

#### 4. Replace Export Sheet (Lines 165-201)
Remove stub implementation and add progress dialog:
```swift
.sheet(isPresented: $backupViewModel.isExporting) {
    ExportProgressDialog(viewModel: backupViewModel)
}
```

#### 5. Replace Import Picker (Lines 202-205)
Replace placeholder with document picker:
```swift
.sheet(isPresented: $backupViewModel.showDocumentPicker) {
    DocumentPickerView(
        selectedURL: .constant(nil),
        onSelect: { url in
            backupViewModel.handleSelectedFile(url)
        }
    )
}
```

#### 6. Add ShareSheet Integration
```swift
.sheet(isPresented: $backupViewModel.showShareSheet) {
    if let url = backupViewModel.exportedFileURL {
        ShareSheet(items: [url])
            .onDisappear {
                backupViewModel.dismissShareSheet()
            }
    }
}
```

#### 7. Add Import Confirmation Dialog
```swift
.alert("Restore Backup?", isPresented: $backupViewModel.showImportConfirmation) {
    Button("Cancel", role: .cancel) {
        backupViewModel.cancelImport()
    }
    Button("Restore") {
        backupViewModel.confirmImport()
    }
} message: {
    if let result = backupViewModel.backupValidationResult {
        Text("\(result.photosCount) photos, \(result.categoriesCount) categories")
    }
}
```

#### 8. Add Import Progress Dialog
```swift
.sheet(isPresented: $backupViewModel.isImporting) {
    ImportProgressDialog(viewModel: backupViewModel)
}
```

#### 9. Add Import Success Dialog
```swift
.alert("Import Complete", isPresented: $backupViewModel.importSuccess) {
    Button("OK") {
        backupViewModel.dismissImportSuccess()
    }
} message: {
    if let result = backupViewModel.importResult {
        Text("\(result.photosImported) photos imported successfully")
    }
}
```

#### 10. Add Error Handling Dialogs
```swift
.alert("Export Error", isPresented: .constant(backupViewModel.exportError != nil)) {
    Button("OK") {
        backupViewModel.exportError = nil
    }
} message: {
    if let error = backupViewModel.exportError {
        Text(error.localizedDescription)
    }
}

.alert("Import Error", isPresented: .constant(backupViewModel.importError != nil)) {
    Button("OK") {
        backupViewModel.importError = nil
    }
} message: {
    if let error = backupViewModel.importError {
        Text(error.localizedDescription)
    }
}
```

### New UI Components Needed

#### ExportProgressDialog
```swift
struct ExportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Exporting Data")
                .font(.headline)

            Text("Creating backup with photos. This may take a moment...")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Text(viewModel.exportMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.exportProgress > 0 {
                Text("Progress: \(Int(viewModel.exportProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}
```

#### ImportProgressDialog
```swift
struct ImportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Importing Data")
                .font(.headline)

            Text(viewModel.importMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.importProgress > 0 {
                Text("Progress: \(Int(viewModel.importProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}
```

### BackupViewModel Integration
**No changes needed** - BackupViewModel is 100% ready:
- Export: `exportData()` method (lines 40-70)
- Import: `showFilePicker()`, `handleSelectedFile()`, `confirmImport()` methods (lines 83-132)
- Progress tracking: Published properties for UI binding
- Error handling: Published error properties
- File pickers: DocumentPickerView already implemented (lines 211-248)

### File Picker Requirements
**DocumentPickerView** (existing, lines 211-248):
- Already implements `UIDocumentPickerViewController`
- Already filters to `.zip` files only
- Already uses `asCopy: true` for security
- Already has coordinator/delegate pattern

**ShareSheet** (existing, ShareManager.swift lines 146-192):
- Already implements `UIActivityViewController`
- Already excludes irrelevant share activities
- Already integrates with SwiftUI via `.sheet()`

### Progress UI Requirements
- Use `ProgressView()` for indeterminate spinner during export/import
- Display `viewModel.exportMessage` / `viewModel.importMessage` for operation text
- Display `viewModel.exportProgress` / `viewModel.importProgress` as percentage
- Use `.interactiveDismissDisabled()` to prevent dismissal during operations

### Error Handling UI Requirements
- Use `.alert()` modifier for all errors
- Bind to `viewModel.exportError != nil` and `viewModel.importError != nil`
- Display `error.localizedDescription` in message
- Provide "OK" button to dismiss and clear error
- Errors should be clearable by setting error property to `nil`

---

## Edge Cases to Handle

### Large Backups (500+ Photos)
- **Scenario**: User exports/imports 500+ photos
- **Expected**: Progress updates smoothly without freezing UI
- **Backend**: BackupManager already handles this with async/await and progress callbacks
- **UI**: Progress dialog remains responsive, shows accurate percentage
- **Timeout**: Operations may take 2-3 minutes, but should complete
- **Memory**: BackupManager processes photos in batches to avoid memory issues

### Corrupted Backup Files
- **Scenario**: User selects a corrupted or invalid ZIP file
- **Detection**: RestoreManager validates ZIP format and structure
- **Expected**: Validation catches corruption before import starts
- **Error Message**: "Corrupted backup - file is damaged or incomplete"
- **User Action**: User can dismiss error and try different file
- **No Data Loss**: No changes made to existing data

### Insufficient Storage
- **Scenario**: Device has insufficient space for import or export
- **Detection**:
  - Export: BackupManager checks available space before creating ZIP
  - Import: RestoreManager checks space before extracting photos
- **Error Message**: "Insufficient storage space - need X MB more"
- **User Action**: User can free up space and retry
- **Cleanup**: Partial files are cleaned up on error

### Cancelled Operations
- **Scenario 1**: User cancels document picker
  - **Expected**: No error, returns to settings gracefully
  - **Backend**: DocumentPickerView handles cancellation (line 244-246)

- **Scenario 2**: User cancels import confirmation
  - **Expected**: No import occurs, returns to settings
  - **Backend**: `cancelImport()` method cleans up state

- **Scenario 3**: App backgrounded during export/import
  - **Expected**: Operation continues in background (iOS limitation may apply)
  - **Fallback**: If interrupted, user sees error and can retry
  - **Cleanup**: Temporary files are cleaned up on next app launch

### Permission Issues
- **Scenario 1**: User denies Files app access
  - **Detection**: Document picker won't appear or will show permission denied
  - **Error Message**: "Cannot access files - please grant permission in Settings"

- **Scenario 2**: User selects read-only location for export
  - **Detection**: ShareSheet handles this - user can choose different destination
  - **No Error**: iOS handles this natively

- **Scenario 3**: Photo library permission denied during import
  - **Detection**: RestoreManager checks permission before importing photos
  - **Error Message**: "Cannot import photos - please grant photo library access"
  - **Recovery**: User can grant permission and retry import

### Version Incompatibility
- **Scenario**: User imports backup from future app version
- **Detection**: RestoreManager validates backup version in metadata
- **Error Message**: "Unsupported backup version - please update the app"
- **User Action**: User updates app or uses compatible backup

### Duplicate Import Attempts
- **Scenario**: User imports same backup multiple times
- **Expected**: Second import skips duplicate photos (MERGE strategy)
- **Behavior**:
  - Categories: Existing categories preserved, new ones added
  - Photos: Duplicates detected by ID, skipped
  - Settings: User prompted whether to overwrite
- **Result**: No duplicate photos, no data loss

---

## Success Metrics

### Functional Parity
**Definition**: iOS functionality matches Android's core capabilities

**Measured By**:
1. **Export Feature Parity**:
   - [ ] iOS can create backup ZIP with same format as Android
   - [ ] iOS backup includes all data (photos, categories, settings)
   - [ ] iOS shows progress during export (operation text + percentage)
   - [ ] iOS user can choose save destination

2. **Import Feature Parity**:
   - [ ] iOS can import Android-created backups
   - [ ] iOS validates backup before import
   - [ ] iOS shows progress during import (operation text + percentage)
   - [ ] iOS uses MERGE strategy (no data deletion)

3. **Error Handling Parity**:
   - [ ] iOS handles corrupted files gracefully
   - [ ] iOS handles insufficient storage
   - [ ] iOS handles permission issues
   - [ ] iOS shows user-friendly error messages

### Cross-Platform Compatibility Validation
**Definition**: Backups can be transferred between iOS and Android

**Test Scenarios**:
1. **iOS → Android**:
   - Create backup on iOS with 50 photos, 5 categories
   - Transfer to Android device
   - Import on Android
   - Verify all photos and categories appear correctly

2. **Android → iOS**:
   - Create backup on Android with 50 photos, 5 categories
   - Transfer to iOS device
   - Import on iOS
   - Verify all photos and categories appear correctly

3. **Round-Trip**:
   - Create backup on iOS
   - Import on Android
   - Create backup on Android
   - Import on iOS
   - Verify data integrity maintained

**Success Criteria**:
- 100% of photos transfer correctly
- 100% of categories transfer correctly
- Photo-category assignments preserved
- No data corruption
- No missing metadata

### User Experience Validation
**Definition**: Feature is easy to use and performs well

**Measured By**:
1. **Performance**:
   - Export 100 photos: < 30 seconds
   - Import 100 photos: < 60 seconds
   - Progress feedback: < 500ms to appear
   - UI responsiveness: No freezing during operations

2. **Usability**:
   - User can complete export without documentation
   - User can complete import without documentation
   - Error messages are understandable
   - Recovery from errors is clear

3. **Reliability**:
   - Zero crashes during export/import
   - Zero data loss scenarios
   - 100% cleanup of temporary files
   - Graceful handling of edge cases

---

## UX Decision: iOS-Native vs Android-Matching Patterns

### Context
Android and iOS have different native patterns for file operations:

**Android Pattern (Current)**:
1. User taps "Export Data"
2. SAF CreateDocument picker appears (choose destination FIRST)
3. User selects save location
4. Export starts
5. Progress dialog shows
6. File saved to chosen location
7. Success

**iOS Pattern (BackupViewModel Implementation)**:
1. User taps "Export Data"
2. Export starts immediately
3. Progress dialog shows
4. Export completes
5. ShareSheet appears (choose destination AFTER)
6. User shares/saves to chosen location
7. Success

### Analysis

#### Option A: Keep iOS-Native Pattern (RECOMMENDED)
**Rationale**:
- ShareSheet is the standard iOS pattern for file export
- More familiar to iOS users
- Simpler implementation (already complete in BackupViewModel)
- Consistent with iOS system behavior
- Better security (file created in app's temporary directory first)
- More flexible (user can save, share, or airdrop)

**Pros**:
- Zero additional implementation needed
- Follows iOS Human Interface Guidelines
- Familiar to iOS users
- Already tested and working
- Supports more share destinations (email, messages, etc.)

**Cons**:
- Different from Android UX
- User doesn't choose exact save location first
- Temporary file created before user decision

**User Impact**:
- Positive: Familiar iOS experience
- Neutral: Different from Android but not confusing
- Risk: Low - iOS users expect ShareSheet pattern

#### Option B: Match Android Exactly
**Rationale**:
- Cross-platform consistency
- User has explicit control over save location
- Matches research documentation

**Pros**:
- Identical UX on both platforms
- User explicitly chooses save location
- Consistent documentation/tutorials

**Cons**:
- Requires new implementation (file picker for save)
- Less iOS-like (uncommon pattern)
- More complex (choose location, then create file there)
- iOS doesn't have direct "save to specific location" API like Android SAF
- Would need to use document picker in "move" mode (unusual)

**User Impact**:
- Negative: Unfamiliar pattern for iOS users
- Positive: Identical to Android
- Risk: Medium - may confuse iOS users expecting ShareSheet

### Recommendation: OPTION A (iOS-Native Pattern)

**Decision**: Keep the iOS-native ShareSheet pattern for export.

**Justification**:
1. **Platform Conventions**: iOS users expect ShareSheet for file operations. Using it provides better UX.
2. **Implementation Reality**: BackupViewModel already implements this correctly. It works.
3. **Security**: iOS sandboxing makes "choose destination first" pattern awkward and less secure.
4. **Flexibility**: ShareSheet supports more destinations than just Files app (AirDrop, Mail, Messages, etc.).
5. **Parity Definition**: Parity means "feature availability", not "identical UX". Both platforms can export/import - the interaction pattern can be platform-appropriate.

**Import Pattern**: Import uses document picker on BOTH platforms, so no decision needed - already identical.

**Documentation Note**: User-facing documentation should acknowledge platform differences:
- "iOS: Export creates a backup and lets you choose where to save/share it"
- "Android: Export lets you choose where to save, then creates the backup"

**Cross-Platform Testing**: Focus on file format compatibility, not UX matching.

---

## Implementation Notes

### What Already Exists (DO NOT RECREATE)
1. **BackupViewModel** (100% complete, lines 1-251)
   - All export logic
   - All import logic
   - Progress tracking
   - Error handling
   - File picker integration

2. **DocumentPickerView** (lines 211-248)
   - UIDocumentPickerViewController wrapper
   - Coordinator/delegate pattern
   - ZIP file filtering

3. **ShareSheet** (ShareManager.swift)
   - UIActivityViewController wrapper
   - Share/save functionality

4. **BackupManager** (BackupManager.swift)
   - createBackup() with progress callbacks
   - Complete ZIP creation logic

5. **RestoreManager** (RestoreManager.swift)
   - restoreBackup() with progress callbacks
   - validateBackup() for pre-import checks
   - Complete ZIP extraction logic

### What Needs to Be Created
1. **ExportProgressDialog** - New SwiftUI view for export progress
2. **ImportProgressDialog** - New SwiftUI view for import progress

### What Needs to Be Modified
1. **SettingsViewCustom.swift** (primary changes):
   - Add BackupViewModel as @StateObject
   - Wire export/import action buttons to viewModel methods
   - Replace stub sheets with real dialogs
   - Add alert bindings for errors, confirmation, success

### Testing Strategy
1. **Unit Testing**: Not required (backend already tested)
2. **Integration Testing**:
   - Test SettingsViewCustom → BackupViewModel wiring
   - Test all dialog flows
   - Test error scenarios
3. **Manual Testing**:
   - Export with 10, 50, 100 photos
   - Import valid backup
   - Import invalid backup
   - Cancel operations
   - Test all error scenarios

### Deployment Readiness
**Definition of Done**:
- [ ] All acceptance criteria met
- [ ] All dialogs implemented and wired
- [ ] All error cases handled
- [ ] Manual testing complete (export/import scenarios)
- [ ] Cross-platform compatibility validated (iOS ↔ Android)
- [ ] No console warnings or errors
- [ ] Build succeeds with xcodebuild
- [ ] Feature works in simulator and device
- [ ] Code review complete
- [ ] Documentation updated (if needed)

---

## Out of Scope

The following are explicitly NOT part of this story:

1. **Backend Changes**: BackupManager and RestoreManager are complete
2. **New Features**: No search, favorites, or other backup management features
3. **Settings Changes**: No new backup settings or preferences
4. **Analytics**: No tracking of backup/restore usage
5. **Cloud Sync**: No automatic cloud backup (only manual export/import)
6. **Scheduled Backups**: No automatic/scheduled backup functionality
7. **Incremental Backups**: Only full backups supported
8. **Backup Management UI**: No list of previous backups, no backup history
9. **Android Changes**: Android is 100% complete, no changes needed

---

## Dependencies

### Prerequisites
- BackupViewModel.swift (exists, no changes)
- BackupManager.swift (exists, no changes)
- RestoreManager.swift (exists, no changes)
- ShareManager.swift (exists, no changes)
- SettingsViewCustom.swift (exists, will modify)

### Platform Requirements
- iOS 16.0+ (for ShareSheet and DocumentPicker APIs)
- File system access permission (automatically handled by iOS)
- Photo library access permission (for importing photos)

### Third-Party Dependencies
- None (uses only iOS system frameworks)

---

## Risks & Mitigations

### Risk 1: ShareSheet Dismissal Handling
**Risk**: User dismisses ShareSheet without saving - temporary file lingers
**Likelihood**: Medium
**Impact**: Low (temp file cleanup on next launch)
**Mitigation**: BackupViewModel.dismissShareSheet() deletes temp file (lines 72-79)

### Risk 2: Large File Performance
**Risk**: Exporting 1000+ photos may be slow or time out
**Likelihood**: Low
**Impact**: Medium (poor user experience)
**Mitigation**:
- BackupManager already handles large sets efficiently
- Progress feedback keeps user informed
- Document performance limits in user guide

### Risk 3: iOS Background Limitations
**Risk**: App backgrounded during export/import may suspend operation
**Likelihood**: Medium
**Impact**: Medium (operation fails)
**Mitigation**:
- Modal dialogs discourage backgrounding
- Error handling allows retry
- Consider background task API in future iteration (out of scope)

### Risk 4: Version Compatibility
**Risk**: Future app updates may break backup compatibility
**Likelihood**: Low
**Impact**: High (user data inaccessible)
**Mitigation**:
- Validation checks version compatibility
- Error message directs user to update app
- Backup format versioned for forward compatibility

### Risk 5: Cross-Platform Format Drift
**Risk**: iOS and Android backup formats diverge over time
**Likelihood**: Low
**Impact**: High (cross-platform import fails)
**Mitigation**:
- Shared format specification in documentation
- Cross-platform testing in QA process
- Version compatibility checks

---

## Success Criteria Summary

This story is successful when:

1. **Feature Complete**: All 33 acceptance criteria pass
2. **Cross-Platform Works**: iOS ↔ Android backup transfer validated
3. **Performance Acceptable**: Export/import within time targets
4. **UX Smooth**: Users can export/import without errors or confusion
5. **Edge Cases Handled**: All edge cases tested and handled gracefully
6. **Zero Regressions**: Existing iOS functionality unaffected
7. **Code Quality**: Clean integration, no TODO comments, follows patterns

**Definition of Parity**: iOS has feature-equivalent backup/restore capability to Android, using platform-appropriate UX patterns.
