# iOS Backup/Restore UI Wiring - Implementation Plan

**Date**: 2025-10-08
**Phase**: Planning (Atlas Phase 3)
**Status**: Ready for Implementation
**Estimated Effort**: 2-3 hours

---

## Executive Summary

This plan details the step-by-step integration of BackupViewModel into SettingsViewCustom. **No backend changes are needed** - all functionality already exists. This is purely UI wiring work.

**Key Facts**:
- BackupViewModel: 100% complete (251 lines, fully tested)
- BackupManager/RestoreManager: 100% complete
- SettingsViewCustom: Currently 272 lines (stub implementations at lines 165-205)
- Target: Keep SettingsViewCustom under 500 lines
- Strategy: Create separate dialog components, wire them up

---

## Part 1: Files to Modify

### 1.1 Primary File: SettingsViewCustom.swift

**Location**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/SettingsViewCustom.swift`

**Current State**:
- Lines 1-272: Settings view with stub export/import implementations
- Lines 11-17: State variables for export/import (currently unused)
- Lines 89-104: Action items with stub actions
- Lines 165-201: Stub export sheet with TODO comment
- Lines 202-205: Placeholder import sheet

**Changes Needed**:
1. Add BackupViewModel as @StateObject (after line 7)
2. Wire export action (line 93)
3. Wire import action (line 103)
4. Replace stub export sheet (lines 165-201)
5. Replace placeholder import sheet (lines 202-205)
6. Add ShareSheet integration (new)
7. Add import confirmation alert (new)
8. Add import progress sheet (new)
9. Add import success alert (new)
10. Add error alerts (new)

**Line Count Impact**:
- Remove: ~40 lines (stubs)
- Add: ~80 lines (wiring + alerts)
- Net: +40 lines → ~312 lines total (well under 500 limit)

---

## Part 2: Files to Create

### 2.1 ExportProgressDialog.swift

**Location**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/Components/ExportProgressDialog.swift`

**Purpose**: Reusable export progress dialog matching Android pattern

**Size**: ~40 lines

**Component Structure**:
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

**Key Features**:
- `.interactiveDismissDisabled()` prevents dismissal during export
- Uses BackupViewModel.exportMessage for operation text
- Shows percentage based on BackupViewModel.exportProgress
- Matches Android dialog pattern (circular progress + text)

---

### 2.2 ImportProgressDialog.swift

**Location**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/Components/ImportProgressDialog.swift`

**Purpose**: Reusable import progress dialog matching Android pattern

**Size**: ~35 lines

**Component Structure**:
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

**Key Features**:
- Modal during import (`.interactiveDismissDisabled()`)
- Shows operation text from BackupViewModel.importMessage
- Displays percentage progress
- Simpler than export dialog (no subtitle text)

---

## Part 3: Implementation Steps (In Order)

### Step 1: Create Progress Dialog Components

**Order**: Do this FIRST so dialogs exist before wiring

#### Step 1.1: Create ExportProgressDialog.swift
1. Create new file: `ios/SmilePile/Views/Components/ExportProgressDialog.swift`
2. Add imports:
   ```swift
   import SwiftUI
   ```
3. Copy ExportProgressDialog implementation from Section 2.1
4. Save file

**Verification**: File compiles without errors

#### Step 1.2: Create ImportProgressDialog.swift
1. Create new file: `ios/SmilePile/Views/Components/ImportProgressDialog.swift`
2. Add imports:
   ```swift
   import SwiftUI
   ```
3. Copy ImportProgressDialog implementation from Section 2.2
4. Save file

**Verification**: File compiles without errors

---

### Step 2: Add BackupViewModel to SettingsViewCustom

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After line 7 (after existing @StateObject declarations)

**Change**:
```swift
@StateObject private var backupViewModel = BackupViewModel()
```

**Line Numbers**:
- Insert after line 7
- This will push everything below down by 1 line

**Verification**: Build succeeds, no errors

---

### Step 3: Wire Export Action

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: Line 93 (will be line 94 after Step 2)

**Current Code** (line 93):
```swift
action: { showingExportSheet = true }
```

**New Code**:
```swift
action: { backupViewModel.exportData() }
```

**Explanation**:
- Remove dependency on `showingExportSheet` state variable
- Call BackupViewModel's `exportData()` method directly
- BackupViewModel will manage all export state internally

**Verification**:
- Build succeeds
- Tapping "Export Data" should trigger export (though progress dialog not yet visible)

---

### Step 4: Wire Import Action

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: Line 103 (will be line 104 after previous changes)

**Current Code** (line 103):
```swift
action: { showingImportPicker = true }
```

**New Code**:
```swift
action: { backupViewModel.showFilePicker() }
```

**Explanation**:
- Remove dependency on `showingImportPicker` state variable
- Call BackupViewModel's `showFilePicker()` method
- BackupViewModel sets `showDocumentPicker = true` internally

**Verification**: Build succeeds

---

### Step 5: Replace Export Sheet with Progress Dialog

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: Lines 165-201 (after previous changes, will be ~166-202)

**Current Code** (lines 165-201):
```swift
.sheet(isPresented: $showingExportSheet) {
    NavigationView {
        VStack(spacing: 20) {
            Text("Exporting backup...")
                .font(.headline)

            if isExporting {
                ProgressView(value: exportProgress)
                    .progressViewStyle(LinearProgressViewStyle())
                    .padding(.horizontal)

                Text("Preparing backup...")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Button("Export") {
                Task {
                    isExporting = true
                    // TODO: Implement export functionality
                    try? await Task.sleep(nanoseconds: 2_000_000_000)
                    isExporting = false
                    showingExportSheet = false
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isExporting)

            Spacer()
        }
        .padding()
        .navigationTitle("Export Backup")
        .navigationBarItems(trailing: Button("Cancel") {
            showingExportSheet = false
        })
    }
}
```

**New Code**:
```swift
.sheet(isPresented: $backupViewModel.isExporting) {
    ExportProgressDialog(viewModel: backupViewModel)
}
```

**Explanation**:
- Remove entire stub NavigationView sheet (36 lines)
- Replace with simple sheet showing ExportProgressDialog
- Binds to BackupViewModel's `isExporting` published property
- Dialog shows/hides automatically based on export state

**Verification**:
- Export starts when "Export Data" tapped
- Progress dialog appears
- Dialog shows progress messages from BackupManager

---

### Step 6: Replace Import Picker Placeholder

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: Lines 202-205 (after previous changes, will be ~168-171)

**Current Code** (lines 202-205):
```swift
.sheet(isPresented: $showingImportPicker) {
    Text("Import functionality coming soon")
        .padding()
}
```

**New Code**:
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

**Explanation**:
- Remove placeholder text (4 lines)
- Add DocumentPickerView (already exists in BackupViewModel.swift)
- Binds to BackupViewModel's `showDocumentPicker` published property
- Passes selected file URL to `handleSelectedFile()` which triggers validation

**Note**: DocumentPickerView already exists in BackupViewModel.swift (lines 211-248), no need to recreate it

**Verification**:
- Tapping "Import Data" shows file picker
- File picker filters to .zip files only
- Selecting file triggers validation

---

### Step 7: Add ShareSheet Integration

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After import picker sheet (after new line ~177)

**Add New Code**:
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

**Explanation**:
- Shows ShareSheet when `showShareSheet = true`
- Passes exported file URL to ShareSheet
- Calls `dismissShareSheet()` on dismiss to clean up temp file
- ShareSheet already exists in ShareManager.swift (lines 146-167)

**Verification**:
- After export completes, ShareSheet appears
- User can save to Files, AirDrop, etc.
- Dismissing ShareSheet cleans up temp file

---

### Step 8: Add Import Confirmation Alert

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After ShareSheet (after new line ~185)

**Add New Code**:
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

**Explanation**:
- Shows confirmation after backup validation succeeds
- Displays backup contents (photo count, category count)
- "Cancel" calls `cancelImport()` to clean up state
- "Restore" calls `confirmImport()` to start import

**Verification**:
- After selecting valid backup file, confirmation alert appears
- Shows correct photo/category counts
- Cancel returns to settings without import
- Restore starts import process

---

### Step 9: Add Import Progress Sheet

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After import confirmation alert (after new line ~198)

**Add New Code**:
```swift
.sheet(isPresented: $backupViewModel.isImporting) {
    ImportProgressDialog(viewModel: backupViewModel)
}
```

**Explanation**:
- Shows progress dialog during import
- Binds to BackupViewModel's `isImporting` published property
- Uses ImportProgressDialog component created in Step 1.2
- Modal dialog prevents dismissal during import

**Verification**:
- After confirming restore, progress dialog appears
- Shows operation text ("Restoring categories...", "Importing photos...")
- Shows progress percentage
- Dialog auto-dismisses when import completes

---

### Step 10: Add Import Success Alert

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After import progress sheet (after new line ~202)

**Add New Code**:
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

**Explanation**:
- Shows success message after import completes
- Displays number of photos imported
- "OK" button calls `dismissImportSuccess()` to clean up state
- Alert appears after progress dialog dismisses

**Verification**:
- After import completes, success alert appears
- Shows correct count of imported photos
- OK dismisses and returns to settings

---

### Step 11: Add Export Error Alert

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After import success alert (after new line ~211)

**Add New Code**:
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
```

**Explanation**:
- Shows alert when export error occurs
- Binds to presence of `exportError` (error != nil)
- Displays error message from error object
- "OK" button clears error to dismiss alert

**Verification**:
- Trigger export error (e.g., insufficient storage)
- Error alert appears with message
- OK clears error and returns to settings

---

### Step 12: Add Import Error Alert

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: After export error alert (after new line ~220)

**Add New Code**:
```swift
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

**Explanation**:
- Shows alert when import/validation error occurs
- Binds to presence of `importError` (error != nil)
- Displays error message (validation errors, file errors, etc.)
- "OK" button clears error to dismiss alert

**Verification**:
- Select invalid backup file (corrupted, wrong version, etc.)
- Error alert appears with specific message
- OK clears error and returns to settings

---

### Step 13: Clean Up Unused State Variables

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

**Location**: Lines 11-17 (now unused)

**Current Code**:
```swift
@State private var showingExportSheet = false
@State private var showingImportPicker = false
@State private var exportProgress: Double = 0.0
@State private var importProgress: Double = 0.0
@State private var isExporting = false
@State private var isImporting = false
```

**Action**: DELETE these lines

**Explanation**:
- All state now managed by BackupViewModel
- These variables are no longer referenced
- Removing them reduces complexity

**Verification**:
- Build succeeds
- No compiler warnings about unused variables

---

## Part 4: Integration Points

### 4.1 BackupViewModel → SettingsViewCustom

**Pattern**: `@StateObject` with Bindings

```swift
// In SettingsViewCustom
@StateObject private var backupViewModel = BackupViewModel()

// Sheet bindings
.sheet(isPresented: $backupViewModel.isExporting) { ... }
.sheet(isPresented: $backupViewModel.showShareSheet) { ... }
.sheet(isPresented: $backupViewModel.showDocumentPicker) { ... }
.sheet(isPresented: $backupViewModel.isImporting) { ... }

// Alert bindings
.alert("...", isPresented: $backupViewModel.showImportConfirmation) { ... }
.alert("...", isPresented: $backupViewModel.importSuccess) { ... }
.alert("...", isPresented: .constant(backupViewModel.exportError != nil)) { ... }
.alert("...", isPresented: .constant(backupViewModel.importError != nil)) { ... }
```

**Key Points**:
- Use `@StateObject` (not `@ObservedObject`) since SettingsViewCustom owns the lifecycle
- Use `$` prefix for binding to published properties
- Use `.constant()` wrapper for error alerts (computed binding from optional)

---

### 4.2 Progress Dialog → BackupViewModel

**Pattern**: `@ObservedObject` for child views

```swift
struct ExportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        Text(viewModel.exportMessage)  // Access published properties
        if viewModel.exportProgress > 0 { ... }
    }
}
```

**Key Points**:
- Use `@ObservedObject` (not `@StateObject`) in child views
- Child views observe changes, don't own the object
- No `$` needed for read-only access

---

### 4.3 State Flow Diagrams

#### Export Flow
```
User taps "Export Data"
    ↓
backupViewModel.exportData()
    ↓
backupViewModel.isExporting = true
    ↓
ExportProgressDialog appears
    ↓
BackupManager.createBackup() starts
    ↓
Progress callbacks update exportProgress, exportMessage
    ↓
ExportProgressDialog shows updates
    ↓
Export completes
    ↓
backupViewModel.isExporting = false
backupViewModel.showShareSheet = true
    ↓
ExportProgressDialog dismisses
ShareSheet appears
    ↓
User saves file
    ↓
ShareSheet dismisses
    ↓
backupViewModel.dismissShareSheet()
    ↓
Temp file cleaned up
    ↓
Done
```

#### Import Flow
```
User taps "Import Data"
    ↓
backupViewModel.showFilePicker()
    ↓
backupViewModel.showDocumentPicker = true
    ↓
DocumentPickerView appears
    ↓
User selects file
    ↓
backupViewModel.handleSelectedFile(url)
    ↓
backupViewModel.isImporting = true
    ↓
RestoreManager.validateBackup() starts
    ↓
Validation completes
    ↓
If valid:
    backupViewModel.showImportConfirmation = true
    ImportConfirmation alert appears
    ↓
User taps "Restore"
    ↓
backupViewModel.confirmImport()
    ↓
backupViewModel.showImportConfirmation = false
backupViewModel.isImporting = true
    ↓
ImportProgressDialog appears
    ↓
RestoreManager.restoreBackup() starts
    ↓
Progress callbacks update importProgress, importMessage
    ↓
ImportProgressDialog shows updates
    ↓
Import completes
    ↓
backupViewModel.isImporting = false
backupViewModel.importSuccess = true
    ↓
ImportProgressDialog dismisses
ImportSuccess alert appears
    ↓
User taps "OK"
    ↓
backupViewModel.dismissImportSuccess()
    ↓
Done

If invalid:
    backupViewModel.importError = error
    ImportError alert appears
    ↓
User taps "OK"
    ↓
backupViewModel.importError = nil
    ↓
Done
```

---

## Part 5: Testing Strategy

### 5.1 Unit Testing
**Status**: Not required for this task

**Rationale**:
- BackupViewModel already tested (backend integration)
- BackupManager/RestoreManager already tested
- This task is UI wiring only
- Manual testing is sufficient

---

### 5.2 Integration Testing

#### Test 1: Export Button Wiring
**Steps**:
1. Build project
2. Open Settings
3. Tap "Export Data"
4. Verify ExportProgressDialog appears
5. Verify progress updates show
6. Verify ShareSheet appears after export

**Expected**: All steps succeed without errors

---

#### Test 2: Import Button Wiring
**Steps**:
1. Build project
2. Open Settings
3. Tap "Import Data"
4. Verify DocumentPickerView appears
5. Verify only .zip files shown
6. Select valid backup
7. Verify confirmation alert appears

**Expected**: All steps succeed without errors

---

#### Test 3: Progress Dialog States
**Steps**:
1. Start export
2. Verify dialog is modal (cannot swipe to dismiss)
3. Verify progress message updates
4. Verify progress percentage shows
5. Verify dialog dismisses when complete

**Expected**: Progress UI works correctly

---

#### Test 4: Error Alert Wiring
**Steps**:
1. Create corrupted .zip file
2. Try to import it
3. Verify error alert appears
4. Verify error message is readable
5. Tap "OK"
6. Verify alert dismisses

**Expected**: Error handling works correctly

---

### 5.3 Manual Testing Scenarios

#### Scenario 1: Happy Path Export
**Setup**: iOS device with 10 photos in SmilePile

**Steps**:
1. Open Settings
2. Tap "Export Data"
3. Wait for progress dialog
4. When ShareSheet appears, tap "Save to Files"
5. Save to "On My iPhone" → "Downloads"
6. Verify file exists in Files app
7. Verify file is named `smilepile_backup_[timestamp].zip`

**Expected**: Export succeeds, file saved correctly

**Verification**:
- Check file size (should be > 0 bytes)
- Try unzipping file (should extract photos + metadata.json)

---

#### Scenario 2: Happy Path Import
**Setup**: Valid SmilePile backup file in Files app

**Steps**:
1. Open Settings
2. Tap "Import Data"
3. Select backup file
4. Wait for validation
5. Verify confirmation shows correct counts
6. Tap "Restore"
7. Wait for progress dialog
8. Verify success message appears
9. Tap "OK"
10. Return to photo gallery
11. Verify imported photos appear

**Expected**: Import succeeds, photos visible

**Verification**:
- Check photo count matches backup
- Check categories restored correctly
- Check photo-category assignments preserved

---

#### Scenario 3: Large Backup (100+ photos)
**Setup**: Backup file with 100+ photos

**Steps**:
1. Export 100 photos
2. Verify progress updates smoothly (no freezing)
3. Verify export completes in < 30 seconds
4. Import the same backup
5. Verify import progress updates smoothly
6. Verify import completes in < 60 seconds

**Expected**: Performance within targets

---

#### Scenario 4: Corrupted Backup File
**Setup**: Create invalid .zip file (e.g., rename .txt to .zip)

**Steps**:
1. Tap "Import Data"
2. Select corrupted file
3. Wait for validation
4. Verify error alert appears
5. Verify message says "Invalid backup file" or similar
6. Tap "OK"
7. Verify returns to settings without crash

**Expected**: Graceful error handling

---

#### Scenario 5: Cancelled Document Picker
**Steps**:
1. Tap "Import Data"
2. Document picker appears
3. Tap "Cancel"
4. Verify returns to settings without error
5. No alerts or crashes

**Expected**: Cancel handled gracefully

---

#### Scenario 6: Cancelled Import Confirmation
**Setup**: Valid backup file

**Steps**:
1. Tap "Import Data"
2. Select file
3. Confirmation alert appears
4. Tap "Cancel"
5. Verify returns to settings without import
6. No changes to photo library

**Expected**: Cancel prevents import

---

#### Scenario 7: Export While Backgrounded
**Steps**:
1. Start export
2. Immediately background app (swipe up)
3. Wait 5 seconds
4. Return to app
5. Check export status

**Expected**:
- Export may complete or may fail (iOS limitation)
- If failed, error alert appears
- User can retry

**Note**: iOS may suspend background tasks. This is acceptable.

---

#### Scenario 8: Insufficient Storage
**Setup**: Device with < 100MB free storage, try to export large backup

**Steps**:
1. Fill device storage
2. Try to export
3. Verify error alert appears
4. Verify message mentions storage issue

**Expected**: Clear error message about storage

---

#### Scenario 9: Cross-Platform Import (Android → iOS)
**Setup**: Backup created on Android

**Steps**:
1. Transfer Android backup to iOS device
2. Tap "Import Data"
3. Select Android backup
4. Verify validation succeeds
5. Complete import
6. Verify photos appear correctly
7. Verify categories restored

**Expected**: Android backups work on iOS

---

#### Scenario 10: ShareSheet Dismissal
**Steps**:
1. Start export
2. Wait for ShareSheet
3. Tap outside ShareSheet to dismiss (or tap "Cancel")
4. Check temp files

**Expected**:
- ShareSheet dismisses
- `dismissShareSheet()` called
- Temp file deleted
- No lingering files in app's temp directory

---

### 5.4 Verification Checklist

After implementation, verify:

- [ ] Export button triggers export
- [ ] Export progress dialog appears
- [ ] Progress messages update during export
- [ ] ShareSheet appears after export
- [ ] Temp file cleaned up after ShareSheet dismissal
- [ ] Import button shows file picker
- [ ] File picker filters to .zip files only
- [ ] Validation runs after file selection
- [ ] Confirmation alert shows backup details
- [ ] Cancel on confirmation prevents import
- [ ] Import progress dialog appears
- [ ] Progress messages update during import
- [ ] Success alert shows after import
- [ ] Error alert shows on validation failure
- [ ] Error alert shows on import failure
- [ ] All dialogs are modal during operations
- [ ] All dialogs dismiss properly when complete
- [ ] No compiler warnings
- [ ] No runtime errors
- [ ] No memory leaks
- [ ] SettingsViewCustom under 500 lines

---

## Part 6: Constraints & Compliance

### 6.1 Line Count Constraint

**Requirement**: Keep SettingsViewCustom under 500 lines

**Current**: 272 lines

**After Changes**:
- Remove: ~43 lines (stub code + unused state)
- Add: ~8 lines (viewModel + wiring + alerts)
- New total: ~237 lines

**Status**: ✅ Well under limit (237 vs 500)

**Strategy**: Progress dialogs extracted to separate components

---

### 6.2 No Backend Changes

**Requirement**: Use existing BackupViewModel without modifications

**Compliance**:
- ✅ BackupViewModel not modified
- ✅ BackupManager not modified
- ✅ RestoreManager not modified
- ✅ DocumentPickerView reused from BackupViewModel.swift
- ✅ ShareSheet reused from ShareManager.swift

**All changes are in UI layer only**

---

### 6.3 SwiftUI Best Practices

#### State Management
✅ Use `@StateObject` for view-owned objects
✅ Use `@ObservedObject` for passed-in objects
✅ Use `@State` for simple local state only
✅ Avoid redundant state (removed duplicate progress variables)

#### Binding Patterns
✅ Use `$` for two-way bindings
✅ Use `.constant()` for computed bindings
✅ Bind to published properties, not methods

#### View Composition
✅ Extract complex dialogs to separate components
✅ Keep SettingsViewCustom focused on layout
✅ Reuse existing components (ShareSheet, DocumentPickerView)

#### Modifiers
✅ Use `.sheet()` for modal presentations
✅ Use `.alert()` for confirmations and errors
✅ Use `.interactiveDismissDisabled()` for modal operations
✅ Use `.onDisappear()` for cleanup

---

### 6.4 iOS Native Patterns

#### File Operations
✅ Use ShareSheet for export (iOS standard)
✅ Use DocumentPicker for import (iOS standard)
✅ Use `asCopy: true` for security

#### Progress UI
✅ Use `ProgressView()` for indeterminate progress
✅ Show text updates for operations
✅ Show percentage for measurable progress
✅ Modal dialogs during operations

#### Error Handling
✅ Use `.alert()` for errors
✅ Show user-friendly messages
✅ Provide clear dismissal (OK button)
✅ Don't expose technical details

---

### 6.5 No Backend Modifications

**BackupViewModel.swift**:
- Status: DO NOT MODIFY
- Reason: Already 100% complete and tested
- Usage: Import as-is, use published properties

**BackupManager.swift**:
- Status: DO NOT MODIFY
- Reason: Backend fully implemented
- Usage: Used by BackupViewModel internally

**RestoreManager.swift**:
- Status: DO NOT MODIFY
- Reason: Backend fully implemented
- Usage: Used by BackupViewModel internally

**ShareManager.swift**:
- Status: DO NOT MODIFY
- Reason: ShareSheet already exists (lines 146-167)
- Usage: Import ShareSheet, use in .sheet()

---

## Part 7: Potential Issues & Solutions

### 7.1 ShareSheet Temp File Cleanup

**Issue**: If user dismisses ShareSheet without saving, temp file lingers

**Solution**: Already handled in BackupViewModel

```swift
// In BackupViewModel.swift (lines 72-79)
func dismissShareSheet() {
    showShareSheet = false
    if let url = exportedFileURL {
        try? FileManager.default.removeItem(at: url)
        exportedFileURL = nil
    }
}
```

**Implementation**: Add `.onDisappear()` to ShareSheet
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

**Verification**: Check app's tmp directory after ShareSheet dismissal - should be empty

---

### 7.2 Multiple Error Alerts

**Issue**: If multiple errors occur, alerts might conflict

**Solution**: Use separate bindings for each error type

```swift
// Export errors
.alert("Export Error", isPresented: .constant(backupViewModel.exportError != nil)) { ... }

// Import errors
.alert("Import Error", isPresented: .constant(backupViewModel.importError != nil)) { ... }
```

**Why This Works**:
- SwiftUI queues alerts if multiple appear
- Different titles distinguish error types
- Clearing error dismisses specific alert
- No conflicts between export/import errors

**Edge Case**: Both errors at once (very rare)
- First alert shows
- User dismisses
- Second alert shows
- Both eventually cleared

---

### 7.3 Progress Dialog Dismissal Timing

**Issue**: Progress dialog might dismiss before ShareSheet appears

**Solution**: Already handled in BackupViewModel

```swift
// In BackupViewModel.swift (lines 59-61)
try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
showShareSheet = true
```

**Why This Works**:
- Export sets `isExporting = false` first
- Progress dialog begins dismiss animation
- 0.5 second delay allows dismiss animation to complete
- ShareSheet then appears smoothly
- No visual glitch or overlap

**Alternative**: Use `.onDisappear()` callback (more complex, unnecessary)

---

### 7.4 File Picker Not Showing .zip Files

**Issue**: If .zip files aren't visible in DocumentPicker

**Root Cause**: File doesn't have .zip extension or wrong UTType

**Solution**: Already handled in DocumentPickerView

```swift
// In BackupViewModel.swift (lines 215-222)
let picker = UIDocumentPickerViewController(
    forOpeningContentTypes: [.zip],
    asCopy: true
)
```

**If Still Not Working**:
1. Check file has .zip extension
2. Check file is actually ZIP format (magic number PK\x03\x04)
3. Try *.* to see if picker appears at all
4. Check file isn't in restricted location

**Testing**: Use known-good .zip file from different source

---

### 7.5 Import Confirmation Showing Wrong Counts

**Issue**: Confirmation shows 0 photos or wrong numbers

**Root Cause**: Validation result not properly set

**Solution**: Check validation flow

```swift
// In BackupViewModel.swift (lines 87-96)
func handleSelectedFile(_ url: URL) {
    Task {
        let validationResult = try await restoreManager.validateBackup(at: url)
        backupValidationResult = validationResult  // Must set this

        if validationResult.isValid {
            showImportConfirmation = true
        }
    }
}
```

**Verification**:
- Set breakpoint in handleSelectedFile
- Check validationResult contents
- Check backupValidationResult is set
- Check alert message accesses correct properties

**Common Mistake**: Accessing wrong property (e.g., `.totalPhotos` instead of `.photosCount`)

---

### 7.6 Progress Not Updating

**Issue**: Progress dialog shows 0% or doesn't update

**Root Cause**: Progress callback not on main thread

**Solution**: Already handled in BackupViewModel

```swift
// In BackupViewModel.swift (lines 48-52)
let zipURL = try await backupManager.createBackup { progress in
    Task { @MainActor in  // Forces main thread
        self.exportProgress = Double(progress.processedItems) / 100.0
        self.exportMessage = progress.currentOperation
    }
}
```

**Key Point**:
- Progress callback happens on background thread
- `Task { @MainActor in }` forces UI updates on main thread
- Without this, UI won't update

**If Still Not Working**:
1. Check BackupManager is actually calling progress callback
2. Check progress values are non-zero
3. Add print statements in progress callback
4. Verify ExportProgressDialog is observing viewModel

---

### 7.7 Memory Leaks from Closures

**Issue**: Strong reference cycles in closure captures

**Solution**: Use `self` carefully in closures

**Safe Patterns**:
```swift
// ✅ Safe: Task captures self weakly by default in @MainActor context
Task { @MainActor in
    self.exportProgress = value
}

// ✅ Safe: Button actions don't create cycles
Button("OK") {
    backupViewModel.exportError = nil
}

// ✅ Safe: .onDisappear doesn't create cycles
.onDisappear {
    backupViewModel.dismissShareSheet()
}
```

**Unsafe Pattern** (not used in this implementation):
```swift
// ❌ Unsafe: Would create cycle if closure stored
let handler = {
    self.someProperty = value
}
```

**Verification**:
- Use Instruments → Leaks
- Run export/import 10 times
- Check memory doesn't grow unbounded

---

### 7.8 Build Errors After Changes

**Common Build Errors**:

#### Error 1: "Cannot find 'ExportProgressDialog' in scope"
**Cause**: File not added to Xcode project
**Fix**:
1. In Xcode, File → Add Files to "SmilePile"
2. Navigate to `ios/SmilePile/Views/Components/`
3. Select ExportProgressDialog.swift
4. Check "Add to targets: SmilePile"
5. Click Add

#### Error 2: "Cannot find 'ImportProgressDialog' in scope"
**Cause**: Same as Error 1
**Fix**: Same as Error 1, but for ImportProgressDialog.swift

#### Error 3: "Cannot find type 'ShareSheet' in scope"
**Cause**: Missing import
**Fix**: Add to SettingsViewCustom.swift:
```swift
import SwiftUI
// ShareSheet is in ShareManager.swift - should auto-import
```
If still failing, check ShareManager.swift is in project

#### Error 4: "Cannot find 'DocumentPickerView' in scope"
**Cause**: BackupViewModel.swift not in build
**Fix**: Check BackupViewModel.swift is in project target

#### Error 5: "'isExporting' is not a member of BackupViewModel"
**Cause**: Using wrong property name
**Fix**: Check exact property name in BackupViewModel.swift
- Should be `isExporting`, not `exporting`
- Case-sensitive

---

### 7.9 Runtime Crashes

**Common Crashes**:

#### Crash 1: "Unexpectedly found nil while unwrapping an Optional"
**Location**: ShareSheet presentation
**Cause**: `exportedFileURL` is nil
**Fix**: Already handled - only show ShareSheet when URL exists
```swift
.sheet(isPresented: $backupViewModel.showShareSheet) {
    if let url = backupViewModel.exportedFileURL {  // Unwrap safely
        ShareSheet(items: [url])
    }
}
```

#### Crash 2: "Simultaneous accesses to..."
**Cause**: State mutation during view update
**Fix**: Use `Task { @MainActor in }` for async updates
```swift
Task { @MainActor in
    backupViewModel.exportError = nil
}
```

#### Crash 3: Sheet presentation while another sheet is active
**Cause**: Multiple sheets triggered simultaneously
**Fix**: Already prevented by BackupViewModel state machine
- Only one operation at a time
- `isExporting` and `isImporting` mutually exclusive
- Sheets bound to different states

---

### 7.10 UX Issues

#### Issue 1: Dialog appears then immediately dismisses
**Cause**: State changes too fast
**Solution**: Already handled with 0.5s delay before ShareSheet

#### Issue 2: Progress stuck at 0%
**Cause**: BackupManager not reporting progress
**Debug**:
1. Check BackupManager.createBackup() calls progress callback
2. Add print statement in progress callback
3. Verify totalItems > 0

**Workaround**: If progress callback not working, show indeterminate spinner only (remove percentage)

#### Issue 3: Success alert appears before import finishes
**Cause**: `importSuccess` set too early
**Fix**: Check BackupViewModel sets `importSuccess = true` AFTER `isImporting = false`

---

## Part 8: Code Snippets Reference

### 8.1 Complete SettingsViewCustom Changes

**New imports** (add if needed):
```swift
import SwiftUI
// No additional imports needed - ShareSheet and DocumentPickerView auto-import
```

**Add after line 7**:
```swift
@StateObject private var backupViewModel = BackupViewModel()
```

**Replace line 93**:
```swift
action: { backupViewModel.exportData() }
```

**Replace line 103**:
```swift
action: { backupViewModel.showFilePicker() }
```

**Delete lines 11-17** (unused state variables)

**Replace lines 165-201** (export sheet):
```swift
.sheet(isPresented: $backupViewModel.isExporting) {
    ExportProgressDialog(viewModel: backupViewModel)
}
```

**Replace lines 202-205** (import picker):
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

**Add after import picker**:
```swift
.sheet(isPresented: $backupViewModel.showShareSheet) {
    if let url = backupViewModel.exportedFileURL {
        ShareSheet(items: [url])
            .onDisappear {
                backupViewModel.dismissShareSheet()
            }
    }
}
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
.sheet(isPresented: $backupViewModel.isImporting) {
    ImportProgressDialog(viewModel: backupViewModel)
}
.alert("Import Complete", isPresented: $backupViewModel.importSuccess) {
    Button("OK") {
        backupViewModel.dismissImportSuccess()
    }
} message: {
    if let result = backupViewModel.importResult {
        Text("\(result.photosImported) photos imported successfully")
    }
}
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

---

### 8.2 Complete ExportProgressDialog.swift

**Full file**:
```swift
import SwiftUI

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
                .padding(.horizontal)

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

---

### 8.3 Complete ImportProgressDialog.swift

**Full file**:
```swift
import SwiftUI

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

---

## Part 9: Final Checklist

### Pre-Implementation
- [ ] Read this plan completely
- [ ] Review BackupViewModel.swift to understand published properties
- [ ] Review SettingsViewCustom.swift current state
- [ ] Understand line number references (may shift as you edit)

### During Implementation
- [ ] Create ExportProgressDialog.swift
- [ ] Create ImportProgressDialog.swift
- [ ] Add BackupViewModel to SettingsViewCustom
- [ ] Wire export action
- [ ] Wire import action
- [ ] Replace export sheet
- [ ] Replace import picker
- [ ] Add ShareSheet integration
- [ ] Add import confirmation alert
- [ ] Add import progress sheet
- [ ] Add import success alert
- [ ] Add export error alert
- [ ] Add import error alert
- [ ] Remove unused state variables
- [ ] Build project (fix any compilation errors)

### Post-Implementation
- [ ] Run manual test: Export 10 photos
- [ ] Run manual test: Import valid backup
- [ ] Run manual test: Try invalid backup file
- [ ] Run manual test: Cancel document picker
- [ ] Run manual test: Cancel import confirmation
- [ ] Verify no compiler warnings
- [ ] Verify SettingsViewCustom < 500 lines
- [ ] Check for memory leaks (Instruments)
- [ ] Test on physical device (not just simulator)
- [ ] Test cross-platform: Android backup → iOS import
- [ ] Git commit with clear message

---

## Part 10: Success Criteria

### Implementation Complete When:

1. **Functional**:
   - ✅ Export button triggers export
   - ✅ Export progress dialog shows and updates
   - ✅ ShareSheet appears after export
   - ✅ Import button shows file picker
   - ✅ Import validation works
   - ✅ Import confirmation shows backup details
   - ✅ Import progress dialog shows and updates
   - ✅ Success alert appears after import
   - ✅ Error alerts show for failures
   - ✅ All operations can be cancelled
   - ✅ Temp files cleaned up properly

2. **Technical**:
   - ✅ No compiler warnings
   - ✅ No runtime errors
   - ✅ SettingsViewCustom under 500 lines
   - ✅ No backend modifications
   - ✅ Follows SwiftUI best practices
   - ✅ No memory leaks

3. **UX**:
   - ✅ Export completes in < 30 sec for 100 photos
   - ✅ Import completes in < 60 sec for 100 photos
   - ✅ Progress updates visible within 500ms
   - ✅ All dialogs are modal during operations
   - ✅ Error messages are user-friendly
   - ✅ Cancellation works without errors

4. **Cross-Platform**:
   - ✅ iOS backups work on Android
   - ✅ Android backups work on iOS
   - ✅ No data loss or corruption
   - ✅ Photo counts match after import

---

## Appendix A: Quick Reference

### Published Properties in BackupViewModel

**Export**:
- `isExporting: Bool` - True during export
- `exportProgress: Double` - 0.0 to 1.0
- `exportMessage: String` - Current operation text
- `exportError: Error?` - Error if export failed
- `exportedFileURL: URL?` - URL of created backup file
- `showShareSheet: Bool` - True to show ShareSheet

**Import**:
- `isImporting: Bool` - True during import/validation
- `importProgress: Double` - 0.0 to 1.0
- `importMessage: String` - Current operation text
- `importError: Error?` - Error if import/validation failed
- `showImportConfirmation: Bool` - True to show confirmation alert
- `backupValidationResult: BackupValidationResult?` - Validation details
- `showDocumentPicker: Bool` - True to show file picker
- `importSuccess: Bool` - True when import completes
- `importResult: ImportResult?` - Import completion details

### Methods in BackupViewModel

**Export**:
- `exportData()` - Start export process
- `dismissShareSheet()` - Clean up after ShareSheet dismissal

**Import**:
- `showFilePicker()` - Show document picker
- `handleSelectedFile(_ url: URL)` - Process selected file
- `confirmImport()` - Start import after confirmation
- `cancelImport()` - Cancel import confirmation
- `dismissImportSuccess()` - Dismiss success alert

---

## Appendix B: Line Number Map

**After all changes, approximate line numbers**:

```
SettingsViewCustom.swift (estimated ~320 lines)

Lines 1-7:   Existing @StateObject declarations
Line 8:      NEW: @StateObject private var backupViewModel = BackupViewModel()
Lines 9-140: Existing UI code (unchanged)
Line 93:     CHANGED: Export action calls backupViewModel.exportData()
Line 103:    CHANGED: Import action calls backupViewModel.showFilePicker()
Lines 141-164: Existing sheet/alert code (unchanged)
Lines 165-167: NEW: Export progress sheet
Lines 168-175: NEW: Import document picker sheet
Lines 176-183: NEW: ShareSheet integration
Lines 184-196: NEW: Import confirmation alert
Lines 197-199: NEW: Import progress sheet
Lines 200-208: NEW: Import success alert
Lines 209-217: NEW: Export error alert
Lines 218-226: NEW: Import error alert
Lines 227-320: Existing code (onAppear, ThemeSelector, etc.)
```

**Note**: Exact line numbers will vary based on formatting. Use code patterns to locate sections.

---

## Appendix C: Android Comparison

### What's Different (by design):

**Export Destination Selection**:
- Android: File picker FIRST (choose location), then export
- iOS: Export FIRST, then ShareSheet (choose destination)
- Reason: iOS pattern is more native, flexible (supports AirDrop, Mail, etc.)

**File Picker UI**:
- Android: Storage Access Framework (Material Design)
- iOS: UIDocumentPickerViewController (iOS native)
- Reason: Platform-native components

**Progress UI Styling**:
- Android: Material Design (AlertDialog with CircularProgressIndicator)
- iOS: SwiftUI (Custom VStack with ProgressView)
- Reason: Platform-native styling

### What's the Same:

- Import flow (picker → validation → confirmation → import)
- Progress callback updates
- Error handling approach
- MERGE strategy
- Backup file format (ZIP with metadata.json)
- Validation logic
- Cross-platform compatibility

---

## Appendix D: Troubleshooting

### Problem: "Build succeeded but nothing happens when I tap Export"

**Check**:
1. Is `backupViewModel.exportData()` actually called? (Add print statement)
2. Is `isExporting` changing to `true`? (Check in debugger)
3. Is ExportProgressDialog file added to Xcode project?
4. Is sheet binding correct? (`$backupViewModel.isExporting`)

### Problem: "ShareSheet doesn't appear after export"

**Check**:
1. Did export complete successfully? (No error?)
2. Is `showShareSheet` set to `true`? (Check in debugger)
3. Is `exportedFileURL` non-nil?
4. Is ShareSheet sheet binding present in view?

### Problem: "Document picker shows but no files appear"

**Check**:
1. Are there .zip files in the test location?
2. Is UTType.zip used in DocumentPickerView?
3. Try selecting "Browse" to see all locations
4. Check file actually has .zip extension

### Problem: "Import confirmation shows 0 photos"

**Check**:
1. Is backup file valid? (Try opening in Archive Utility)
2. Is metadata.json present in backup?
3. Is `backupValidationResult` set correctly?
4. Are property names correct? (`.photosCount`, not `.photoCount`)

### Problem: "Progress never updates"

**Check**:
1. Is BackupManager calling progress callback? (Add print)
2. Is `Task { @MainActor in }` used in callback?
3. Are progress values actually changing? (Print them)
4. Is ExportProgressDialog observing viewModel?

### Problem: "App crashes when dismissing ShareSheet"

**Check**:
1. Is `.onDisappear()` calling `dismissShareSheet()`?
2. Is file deletion wrapped in try? (`try?`)
3. Check crash log for actual error

---

## End of Implementation Plan

**Next Steps**:
1. Read this plan thoroughly
2. Set up test environment (simulator + test photos)
3. Create progress dialog files (Step 1)
4. Modify SettingsViewCustom (Steps 2-13)
5. Test each feature as you implement it
6. Run full manual test suite
7. Fix any issues found
8. Commit changes

**Questions?**
- Check BackupViewModel.swift for implementation details
- Check Android code for reference pattern
- Check research report for architecture decisions

**Good luck!** 🚀
