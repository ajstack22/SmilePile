# Backup/Restore Implementation Research Report

**Date**: 2025-10-08
**Purpose**: Document exact Android implementation to guide iOS parity implementation
**Status**: Research Complete - Ready for Story Creation

---

## A. Android Export Flow

### Step-by-Step Export Process

**File**: `android/app/src/main/java/com/smilepile/ui/viewmodels/SettingsViewModel.kt`

#### 1. User Initiates Export
**Location**: `SettingsScreen.kt` lines 239-242
```kotlin
onExport = {
    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
    launchers.exportLauncher.launch("smilepile_backup_$timestamp.zip")
}
```

**File Picker Setup**: Lines 153-156
```kotlin
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip")
) { uri ->
    uri?.let { viewModel.completeExport(it) }
}
```

#### 2. ViewModel Prepares Export
**Location**: `SettingsViewModel.kt::prepareExport()` (lines 222-261)

**Process**:
1. Set `isLoading = true, error = null`
2. Call `backupManager.exportToZip()` with progress callback
3. Store result in `pendingExportZipFile`
4. Create and return `createExportIntent(BackupFormat.ZIP)`

**Progress Tracking**:
```kotlin
val result = backupManager.exportToZip { current, total, operation ->
    val progress = ImportProgress(
        totalItems = total,
        processedItems = current,
        currentOperation = operation,
        errors = emptyList()
    )
    _uiState.value = _uiState.value.copy(exportProgress = progress)
}
```

#### 3. User Selects Destination
**File Picker Type**: Storage Access Framework (SAF)
- Uses `CreateDocument` contract
- Suggested filename: `smilepile_backup_${timestamp}.zip`
- MIME type: `application/zip`

#### 4. Complete Export
**Location**: `SettingsViewModel.kt::completeExport()` (lines 267-325)

**Process**:
1. Set `isLoading = true`
2. Use `pendingExportZipFile` or create fresh export
3. Call `backupManager.writeZipToFile(zipFile, uri)`
4. Clean up temp file on success
5. Set `isLoading = false, exportProgress = null`

**Error Handling**:
```kotlin
if (writeResult.isSuccess) {
    _uiState.value = _uiState.value.copy(
        isLoading = false,
        error = null,
        exportProgress = null
    )
    pendingExportZipFile?.delete()
} else {
    _uiState.value = _uiState.value.copy(
        error = writeResult.exceptionOrNull()?.message ?: "Failed to save backup file",
        isLoading = false,
        exportProgress = null
    )
}
```

### Progress UI Implementation

**Dialog**: `SettingsScreen.kt::ExportProgressDialog()` (lines 1083-1126)

**Features**:
- Modal dialog (cannot dismiss while exporting)
- Circular progress indicator
- Current operation text
- Progress counter: "Progress: X/Y"
- Text: "Creating backup with photos. This may take a moment..."

---

## B. Android Import Flow

### Step-by-Step Import Process

**File**: `android/app/src/main/java/com/smilepile/ui/viewmodels/SettingsViewModel.kt`

#### 1. User Initiates Import
**Location**: `SettingsScreen.kt` lines 243-245
```kotlin
onImport = {
    launchers.importLauncher.launch(arrayOf("application/zip", "*/*"))
}
```

**File Picker Setup**: Lines 159-163
```kotlin
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.importFromUri(it) }
}
```

#### 2. ViewModel Processes Import
**Location**: `SettingsViewModel.kt::importFromUri()` (lines 331-349)

**Process**:
1. Set `isLoading = true, error = null, importProgress = null`
2. Copy URI to temp file
3. Detect if ZIP or JSON format
4. Execute appropriate import
5. Clean up temp file

**Format Detection**:
```kotlin
private fun detectZipFormat(tempFile: File): Boolean {
    return tempFile.name.endsWith(".zip") ||
           (tempFile.length() > 4 &&
            tempFile.inputStream().use { stream ->
                val header = ByteArray(4)
                stream.read(header)
                // ZIP magic number: PK\x03\x04
                header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
                header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            })
}
```

#### 3. ZIP Import Execution
**Location**: `SettingsViewModel.kt::executeZipImport()` (lines 393-408)

**Process**:
```kotlin
backupManager.importFromZip(
    zipFile = tempFile,
    strategy = ImportStrategy.MERGE
) { current, total, operation ->
    val progress = ImportProgress(
        totalItems = total,
        processedItems = current,
        currentOperation = operation,
        errors = emptyList()
    )
    _uiState.value = _uiState.value.copy(importProgress = progress)
}.collect { progress ->
    handleImportProgress(progress)
}
```

#### 4. Progress Handling
**Location**: `SettingsViewModel.kt::handleImportProgress()` (lines 419-439)

**Completion Detection**:
```kotlin
when {
    progress.errors.isNotEmpty() -> {
        _uiState.value = _uiState.value.copy(
            error = "Import completed with errors: ${progress.errors.firstOrNull()}",
            isLoading = false,
            importProgress = null
        )
    }
    progress.currentOperation.contains("completed", ignoreCase = true) -> {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            importProgress = null
        )
        loadBackupStats()
    }
}
```

### Progress UI Implementation

**Dialog**: `SettingsScreen.kt::ImportProgressDialog()` (lines 1017-1080)

**Features**:
- Modal dialog (dismissible only when complete or error)
- Circular progress indicator (while importing)
- Current operation text
- Progress counter: "Progress: X/Y"
- Error count display if errors exist
- "OK" button only when complete

---

## C. iOS Backend Capabilities

### BackupManager.swift

**File**: `ios/SmilePile/Data/Backup/BackupManager.swift`

#### Current Capabilities

**Method**: `createBackup(progressCallback:)` (lines 188-322)

**Features**:
✅ **Full Implementation Available**
- Returns `URL` to created ZIP file
- Progress callback with `ExportProgress` struct
- Complete workflow:
  1. Create working directory
  2. Collect categories, photos, settings
  3. Copy photos to working directory
  4. Create metadata.json
  5. Create ZIP archive
  6. Clean up working directory

**Progress Support**:
```swift
progressCallback?(ExportProgress(
    totalItems: 100,
    processedItems: 10,
    currentOperation: "Collecting categories...",
    currentFile: nil,
    bytesProcessed: 0,
    totalBytes: 0,
    errors: []
))
```

**Error Handling**:
- Throws errors (Swift async/await pattern)
- Cleanup in `defer` block ensures temp directory removal
- Returns file URL on success

### RestoreManager.swift

**File**: `ios/SmilePile/Data/Backup/RestoreManager.swift`

#### Current Capabilities

**Method**: `restoreBackup(from:options:progressCallback:)` (lines 171-366)

**Features**:
✅ **Full Implementation Available**
- Accepts file URL
- Progress callback with `ImportProgress` struct
- Complete workflow:
  1. Extract ZIP to temp directory
  2. Parse metadata.json
  3. Restore categories
  4. Restore photos (with file restoration)
  5. Restore settings (optional)
  6. Clean up temp directory

**Progress Support**:
```swift
progressCallback?(ImportProgress(
    totalItems: 100,
    processedItems: 20,
    currentOperation: "Reading metadata...",
    errors: []
))
```

**Validation Method**: `validateBackup(at:checkIntegrity:)` (lines 31-107)
- Validates backup file
- Checks version compatibility
- Verifies photo integrity (optional)
- Returns `BackupValidationResult`

---

## D. iOS UI Current State

### SettingsViewCustom.swift

**File**: `ios/SmilePile/Views/SettingsViewCustom.swift`

#### Export Implementation

**Current State**: Lines 165-200
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
```

**Status**: 🔴 **STUB IMPLEMENTATION**
- Placeholder UI exists
- TODO comment on line 184
- Sleep instead of actual export
- No BackupManager integration
- No file picker integration
- Progress is fake

#### Import Implementation

**Current State**: Lines 202-205
```swift
.sheet(isPresented: $showingImportPicker) {
    Text("Import functionality coming soon")
        .padding()
}
```

**Status**: 🔴 **PLACEHOLDER TEXT ONLY**
- No file picker
- No RestoreManager integration
- No progress UI
- No error handling

### BackupViewModel.swift

**File**: `ios/SmilePile/ViewModels/BackupViewModel.swift`

#### Export Capabilities

**Current Implementation**: Lines 40-79

**Features**:
✅ **FULLY IMPLEMENTED**
- Uses `BackupManager.shared.createBackup()`
- Progress tracking via callback
- Error handling
- Creates `exportedFileURL`
- Shows `ShareSheet` on completion

**Pattern**:
```swift
func exportData() {
    Task {
        let zipURL = try await backupManager.createBackup { progress in
            Task { @MainActor in
                self.exportProgress = Double(progress.processedItems) / 100.0
                self.exportMessage = progress.currentOperation
            }
        }
        exportedFileURL = zipURL
        showShareSheet = true
    }
}
```

#### Import Capabilities

**Current Implementation**: Lines 83-132

**Features**:
✅ **FULLY IMPLEMENTED**
- Uses `DocumentPickerView` (UIDocumentPickerViewController)
- Validates backup before import
- Shows confirmation dialog
- Uses `RestoreManager.shared.restoreBackup()`
- Progress tracking
- Error handling

**Pattern**:
```swift
func handleSelectedFile(_ url: URL) {
    Task {
        let validationResult = try await restoreManager.validateBackup(at: url)
        backupValidationResult = validationResult

        if validationResult.isValid {
            showImportConfirmation = true
        } else {
            importError = createValidationError(from: validationResult.errors)
        }
    }
}
```

### DocumentPickerView

**File**: `ios/SmilePile/ViewModels/BackupViewModel.swift` (lines 211-248)

**Features**:
✅ **FULLY IMPLEMENTED**
- `UIViewControllerRepresentable` wrapper
- Accepts `.zip` files only
- Copy mode (`asCopy: true`)
- Delegate pattern for file selection
- Cancellation handling

```swift
struct DocumentPickerView: UIViewControllerRepresentable {
    @Binding var selectedURL: URL?
    let onSelect: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: [.zip],
            asCopy: true
        )
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }
}
```

### ShareSheet (for Export)

**File**: `ios/SmilePile/Managers/ShareManager.swift` (lines 146-192)

**Features**:
✅ **FULLY IMPLEMENTED**
- `UIActivityViewController` wrapper
- Accepts any items (works with file URLs)
- Excludes irrelevant activities
- SwiftUI integration via `.sheet()`

**Usage Pattern**:
```swift
.sheet(isPresented: $showShareSheet) {
    if let url = exportedFileURL {
        ShareSheet(items: [url])
    }
}
```

---

## E. Gap Analysis

### Export Flow Gaps

#### Android Pattern:
1. User taps "Export Data"
2. **SAF CreateDocument** picker appears immediately
3. User selects destination
4. Export process starts
5. Progress dialog shows
6. File written to selected location
7. Success (file saved to user's chosen location)

#### iOS Current State:
1. User taps "Export Data" → ✅
2. **Modal sheet** appears (not file picker) → ❌
3. "Export" button in sheet → ❌
4. Export process (TODO) → ❌
5. **ShareSheet** appears → ✅
6. User chooses destination from share options → ✅
7. Success → ✅

#### Missing in iOS UI:

**🔴 Critical**: No direct integration between SettingsViewCustom and BackupViewModel
- `SettingsViewCustom` has stub code (lines 165-200)
- `BackupViewModel` has working code but is not used
- Need to wire up SettingsViewCustom to use BackupViewModel

**🔴 Critical**: Different UX pattern than Android
- Android: File picker FIRST (choose destination), then export
- iOS Current: Export FIRST, then ShareSheet (choose destination)
- iOS pattern is actually better (simpler), but different from Android

**Decision Needed**: Match Android's UX exactly or keep iOS pattern?
- **Option A**: Keep iOS pattern (simpler, more iOS-native)
  - Pro: Already implemented in BackupViewModel
  - Pro: More iOS-like (ShareSheet is standard)
  - Con: Different from Android

- **Option B**: Match Android exactly
  - Pro: Cross-platform consistency
  - Pro: User explicitly chooses save location
  - Con: More complex implementation
  - Con: Less iOS-like

### Import Flow Gaps

#### Android Pattern:
1. User taps "Import Data"
2. **OpenDocument** picker appears
3. User selects backup file
4. Import process starts immediately
5. Progress dialog shows
6. Import completes
7. Success message

#### iOS Current State (BackupViewModel):
1. User taps "Import Data" → ✅
2. **DocumentPicker** appears → ✅
3. User selects file → ✅
4. **Validation** happens → ✅ (extra step, good!)
5. **Confirmation dialog** appears → ✅ (extra step, good!)
6. User confirms → ✅
7. Import process starts → ✅
8. Progress updates → ✅
9. Success → ✅

#### Missing in iOS UI:

**🔴 Critical**: No integration in SettingsViewCustom
- Line 202-205: Just placeholder text
- BackupViewModel has full working implementation
- Need to wire up SettingsViewCustom to use BackupViewModel

**🟢 Better than Android**: iOS has validation step
- Android imports immediately
- iOS validates first, shows confirmation
- This is actually better UX

### Progress UI Gaps

#### Android Progress Dialog (Export):
```kotlin
AlertDialog {
    Row {
        CircularProgressIndicator()
        Text("Exporting Data")
    }
    Text("Creating backup with photos. This may take a moment...")
    if (progress != null) {
        Text(progress.currentOperation)
        Text("Progress: ${progress.processedItems}/${progress.totalItems}")
    }
}
```

#### iOS Progress UI (Export):
```swift
// In SettingsViewCustom - STUB
ProgressView(value: exportProgress)
Text("Preparing backup...")

// In BackupViewModel - NOT USED
// Published variables exist but no UI
@Published var exportProgress: Double = 0
@Published var exportMessage: String = ""
```

**Gap**: SettingsViewCustom needs to use BackupViewModel's progress

#### Android Progress Dialog (Import):
```kotlin
AlertDialog {
    Row {
        if (!canDismiss) {
            CircularProgressIndicator()
        }
        Text("Importing Data")
    }
    Text(progress.currentOperation)
    Text("Progress: ${progress.processedItems}/${progress.totalItems}")
    if (progress.errors.isNotEmpty()) {
        Text("Errors: ${progress.errors.size}")
    }
    if (canDismiss) {
        Button("OK")
    }
}
```

#### iOS Progress UI (Import):
**Does not exist in SettingsViewCustom at all**

**Gap**: Need to create import progress dialog in SettingsViewCustom

### Error Handling Gaps

#### Android:
```kotlin
_uiState.value = _uiState.value.copy(
    error = e.message ?: "Failed to export data",
    isLoading = false,
    exportProgress = null
)
```

Errors shown via alert dialogs triggered by `uiState.error != null`

#### iOS:
```swift
// BackupViewModel has it
@Published var exportError: Error?
@Published var importError: Error?

// SettingsViewCustom doesn't use it
```

**Gap**: SettingsViewCustom needs error alert integration

---

## F. Concrete Implementation Gaps

### Gap 1: SettingsViewCustom Export Section

**Current**: Lines 89-104
```swift
SettingsActionItem(
    title: "Export Data",
    subtitle: "Save your photos and categories",
    icon: "square.and.arrow.up",
    action: { showingExportSheet = true }  // ❌ Shows stub sheet
)
```

**Needed**:
```swift
// Add BackupViewModel
@StateObject private var backupViewModel = BackupViewModel()

SettingsActionItem(
    title: "Export Data",
    subtitle: "Save your photos and categories",
    icon: "square.and.arrow.up",
    action: { backupViewModel.exportData() }  // ✅ Use real export
)
```

### Gap 2: Export Progress Dialog

**Current**: Lines 165-200 (stub sheet)

**Needed**: New progress dialog matching Android pattern
```swift
.sheet(isPresented: $backupViewModel.isExporting) {
    VStack {
        if backupViewModel.isExporting {
            ProgressView()
            Text("Exporting Data")
            Text(backupViewModel.exportMessage)

            if backupViewModel.exportProgress > 0 {
                Text("Progress: \(Int(backupViewModel.exportProgress * 100))%")
            }
        }
    }
}
```

### Gap 3: Export ShareSheet Integration

**Current**: Not present in SettingsViewCustom

**Needed**: Add to view
```swift
.sheet(isPresented: $backupViewModel.showShareSheet) {
    if let url = backupViewModel.exportedFileURL {
        ShareSheet(items: [url])
    }
}
```

### Gap 4: Import File Picker

**Current**: Lines 202-205 (placeholder)

**Needed**:
```swift
.sheet(isPresented: $showingImportPicker) {
    DocumentPickerView(
        selectedURL: $backupViewModel.selectedImportURL,
        onSelect: { url in
            backupViewModel.handleSelectedFile(url)
        }
    )
}
```

### Gap 5: Import Confirmation Dialog

**Current**: Not present

**Needed**: Match BackupViewModel's confirmation pattern
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

### Gap 6: Import Progress Dialog

**Current**: Not present

**Needed**: New dialog matching Android pattern
```swift
.sheet(isPresented: $backupViewModel.isImporting) {
    VStack {
        ProgressView()
        Text("Importing Data")
        Text(backupViewModel.importMessage)

        if backupViewModel.importProgress > 0 {
            Text("Progress: \(Int(backupViewModel.importProgress * 100))%")
        }
    }
}
```

### Gap 7: Import Success Dialog

**Current**: Not present

**Needed**:
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

### Gap 8: Error Handling

**Current**: Not present

**Needed**:
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

---

## G. Summary

### Android Implementation Status
✅ **100% Complete and Working**
- Export: Full SAF integration with progress tracking
- Import: Full file picker with auto-detection (ZIP/JSON)
- Progress: Modal dialogs with detailed progress
- Error handling: Comprehensive error states

### iOS Backend Status
✅ **100% Complete and Working**
- BackupManager: Full export with progress
- RestoreManager: Full import with validation
- BackupViewModel: Complete UI logic ready
- File pickers: DocumentPickerView implemented
- ShareSheet: Export destination selector ready

### iOS UI Status
🔴 **0% Integrated in SettingsViewCustom**
- Export button: Wired to stub code
- Import button: Shows placeholder text
- All working code exists in BackupViewModel
- Just needs to be connected

### Implementation Strategy

**The fix is simple**: Wire SettingsViewCustom to BackupViewModel

1. Add `@StateObject private var backupViewModel = BackupViewModel()`
2. Change export action to call `backupViewModel.exportData()`
3. Change import action to call `backupViewModel.showFilePicker()`
4. Add sheet/alert bindings for all BackupViewModel published states:
   - `isExporting` → progress sheet
   - `showShareSheet` → ShareSheet
   - `showDocumentPicker` → DocumentPickerView
   - `showImportConfirmation` → confirmation alert
   - `isImporting` → progress sheet
   - `importSuccess` → success alert
   - `exportError` / `importError` → error alerts

**Estimated Effort**: 1-2 hours of straightforward UI wiring

**No new code needed**: Everything already exists, just needs connection

---

## H. File Picker Patterns Reference

### iOS DocumentPicker (for Import)
```swift
UIDocumentPickerViewController(
    forOpeningContentTypes: [.zip],
    asCopy: true
)
```

### iOS ShareSheet (for Export)
```swift
UIActivityViewController(
    activityItems: [fileURL],
    applicationActivities: nil
)
```

### Android SAF CreateDocument (for Export)
```kotlin
ActivityResultContracts.CreateDocument("application/zip")
```

### Android SAF OpenDocument (for Import)
```kotlin
ActivityResultContracts.OpenDocument()
// with: arrayOf("application/zip", "*/*")
```

---

## Appendix: Key File Locations

### Android
- **ViewModel**: `android/app/src/main/java/com/smilepile/ui/viewmodels/SettingsViewModel.kt`
- **UI**: `android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt`
- **Backend**: `android/app/src/main/java/com/smilepile/data/backup/BackupManager.kt`

### iOS
- **UI (needs work)**: `ios/SmilePile/Views/SettingsViewCustom.swift`
- **ViewModel (ready)**: `ios/SmilePile/ViewModels/BackupViewModel.swift`
- **Backend**: `ios/SmilePile/Data/Backup/BackupManager.swift`, `RestoreManager.swift`
- **Pickers**: Embedded in BackupViewModel.swift
- **Share**: `ios/SmilePile/Managers/ShareManager.swift`
