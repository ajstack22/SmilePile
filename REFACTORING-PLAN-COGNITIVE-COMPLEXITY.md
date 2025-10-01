# Refactoring Plan: Cognitive Complexity Reduction

## Overview
This document outlines the detailed refactoring strategy for reducing cognitive complexity in 6 critical methods with complexity > 50.

## Method 1: PinSetupScreen.kt:21 (Complexity 96 → Target < 15)

### Current Method
`fun PinSetupScreen()` - A large Composable function with deeply nested UI logic, state management, and event handling.

### Complexity Sources
- Mixed concerns: UI rendering + PIN validation + state management
- Deep nesting in number pad rendering (nested loops and conditionals)
- Complex conditional logic for PIN confirmation flow
- Event handling embedded within UI composition

### Refactoring Strategy

#### Extract Helper Composables
1. `@Composable private fun PinHeader(isConfirming: Boolean, showError: Boolean, errorMessage: String)`
   - Renders icon, title, subtitle, and error message

2. `@Composable private fun PinDotsIndicator(pinLength: Int, currentPin: String)`
   - Renders the PIN dots display

3. `@Composable private fun NumberPad(onNumberClick: (String) -> Unit, onClearClick: () -> Unit, onBackspaceClick: () -> Unit)`
   - Renders the entire number pad grid

4. `@Composable private fun NumberButton(number: String, onClick: () -> Unit)`
   - Already exists, ensure it's used consistently

#### Extract State Management
5. `private fun validateAndProcessPin(pinCode: String, confirmPinCode: String, onPinSet: (String) -> Unit): PinValidationResult`
   - Validates PIN rules and handles confirmation logic
   - Returns sealed class result (Success, Error, NeedConfirmation)

6. `private fun addDigit(digit: String, currentPin: String, maxLength: Int): String?`
   - Already exists, keep it

#### Resulting Structure
```kotlin
@Composable
fun PinSetupScreen(onPinSet: (String) -> Unit, onSkip: () -> Unit) {
    val pinState = rememberPinSetupState()

    Column {
        PinHeader(pinState.isConfirming, pinState.showError, pinState.errorMessage)
        PinDotsIndicator(PIN_LENGTH, pinState.currentPin)
        NumberPad(
            onNumberClick = { pinState.addDigit(it) },
            onClearClick = { pinState.clear() },
            onBackspaceClick = { pinState.backspace() }
        )
    }
}
```

### Expected Complexity Reduction: 96 → ~12

---

## Method 2: PhotoGalleryScreen.kt:82 (Complexity 72 → Target < 15)

### Current Method
`fun PhotoGalleryScreen()` - Large Composable with swipe gesture detection, selection mode, and multiple UI states.

### Complexity Sources
- Swipe gesture logic embedded in UI
- Multiple conditional renderings based on orchestrator state
- Deep nesting in Scaffold components
- Complex gesture calculations

### Refactoring Strategy

#### Extract Helper Composables
1. `@Composable private fun PhotoGalleryContent(orchestratorState, paddingValues)`
   - Main content area with header and photo grid

2. `@Composable private fun GalleryHeader(orchestratorState)`
   - Header with category filter (only shown when not in selection mode)

3. `@Composable private fun ImportProgressSection(importState)`
   - Import progress indicator section

4. `@Composable private fun PhotoGridWithGestures(orchestratorState, onSwipe: (SwipeDirection) -> Unit)`
   - Photo grid with gesture detection separated

#### Extract Gesture Logic
5. `private fun createHorizontalSwipeGesture(categories, selectedCategoryId, onCategorySwitch): PointerInputScope.() -> Unit`
   - Returns configured gesture handler
   - Encapsulates swipe threshold and direction logic

6. `private fun calculateNextCategory(categories, currentId, swipeDirection): String?`
   - Pure function to calculate category transitions

### Expected Complexity Reduction: 72 → ~14

---

## Method 3: ZipUtils.kt:174 (Complexity 65 → Target < 15)

### Current Method
`suspend fun extractZip()` - Two-pass ZIP extraction with extensive security validation.

### Complexity Sources
- Two complete ZIP passes (validation + extraction)
- Multiple security checks (path traversal, size limits, compression ratio)
- Nested loops with multiple early returns
- Mixed concerns: validation + extraction + progress reporting

### Refactoring Strategy

#### Extract Validation Methods
1. `private suspend fun validateZipSecurityFirstPass(zipFile: File): Result<ValidationResult>`
   - First pass: count entries, check sizes, detect ZIP bombs
   - Returns ValidationResult with entryCount and totalSize

2. `private fun validateZipEntry(entry: ZipEntry, totalUncompressedSize: Long, entryCount: Int): Result<Unit>`
   - Validates single entry for security issues
   - Checks: path traversal, size limits, compression ratio

3. `private fun checkCompressionRatio(entry: ZipEntry): Boolean`
   - Isolated compression ratio validation

#### Extract Extraction Methods
4. `private suspend fun extractZipSecondPass(zipFile: File, destDir: File, entryCount: Int, progressCallback): Result<List<File>>`
   - Second pass: actual file extraction
   - Progress reporting

5. `private fun extractZipEntry(zipIn: ZipInputStream, destDir: File, entry: ZipEntry): File?`
   - Extract single entry (file or directory)
   - Returns extracted File or null for directories

#### Resulting Structure
```kotlin
suspend fun extractZip(zipFile: File, destDir: File, progressCallback): Result<List<File>> {
    if (!validateZipFileExists(zipFile)) return failure()

    val validation = validateZipSecurityFirstPass(zipFile).getOrElse { return failure(it) }
    validateZipStructure(zipFile).getOrElse { return failure(it) }

    return extractZipSecondPass(zipFile, destDir, validation.entryCount, progressCallback)
}
```

### Expected Complexity Reduction: 65 → ~10

---

## Method 4: BackupManager.kt:575 (Complexity 63 → Target < 15)

### Current Method
`suspend fun importFromJson()` - Imports categories and photos from JSON backup with strategy handling.

### Complexity Sources
- Two sequential import phases (categories, then photos)
- Strategy branching (MERGE vs REPLACE)
- Duplicate detection logic
- Category ID mapping between backup and current DB
- Extensive error handling

### Refactoring Strategy

#### Extract Import Phases
1. `private suspend fun importCategories(backupData: AppBackup, strategy: ImportStrategy): CategoryImportResult`
   - Imports all categories
   - Returns result with imported count, warnings, errors

2. `private suspend fun importCategory(categoryBackup: CategoryBackup, strategy: ImportStrategy): Result<Unit>`
   - Imports single category with strategy handling

3. `private suspend fun importPhotos(backupData: AppBackup, strategy: ImportStrategy): PhotoImportResult`
   - Imports all photos
   - Returns result with imported/skipped counts

4. `private suspend fun importPhoto(photoBackup: PhotoBackup, backupData: AppBackup, strategy: ImportStrategy): PhotoImportStatus`
   - Imports single photo
   - Returns sealed class: Imported, Skipped(reason), Failed(error)

#### Extract Validation Methods
5. `private suspend fun validatePhoto(photoBackup: PhotoBackup, backupData: AppBackup, strategy: ImportStrategy): PhotoValidationResult`
   - Validates MediaStore URI, checks duplicates, verifies category exists
   - Returns validation result with mapped category ID

6. `private suspend fun resolveCategoryIdForPhoto(photoBackup: PhotoBackup, backupData: AppBackup, strategy: ImportStrategy): Long?`
   - Maps backup category ID to actual DB category ID

### Expected Complexity Reduction: 63 → ~12

---

## Method 5: BackupManager.kt:380 (Complexity 55 → Target < 15)

### Current Method
`suspend fun importFromZip()` - Similar to importFromJson but with ZIP extraction first.

### Complexity Sources
- ZIP extraction and validation
- Category and photo import logic (duplicated from importFromJson)
- Photo file restoration from ZIP
- Progress reporting at multiple levels

### Refactoring Strategy

#### Reuse importFromJson Logic
1. Extract common import logic from both methods into shared helpers (from Method 4)

2. `private suspend fun extractAndReadZipBackup(zipFile: File): Result<ZipBackupData>`
   - Validates ZIP structure
   - Extracts to temp directory
   - Reads metadata.json
   - Returns ZipBackupData(backupData, tempDir, photoFiles)

3. `private suspend fun restorePhotoFilesFromZip(photos: List<Photo>, tempDir: File): RestoreResult`
   - Copies photo files from extracted ZIP to internal storage
   - Returns count of restored files

#### Resulting Structure
```kotlin
suspend fun importFromZip(zipFile: File, strategy: ImportStrategy, progressCallback): Flow<ImportProgress> = flow {
    val zipData = extractAndReadZipBackup(zipFile).getOrElse { error ->
        emit(error progress)
        return@flow
    }

    // Reuse shared import logic
    val categoryResult = importCategories(zipData.backupData, strategy)
    emit(categoryResult.toProgress())

    val photoResult = importPhotos(zipData.backupData, strategy)
    emit(photoResult.toProgress())

    val restoreResult = restorePhotoFilesFromZip(photoResult.importedPhotos, zipData.tempDir)
    emit(restoreResult.toProgress())
}
```

### Expected Complexity Reduction: 55 → ~11

---

## Method 6: PhotoEditViewModel.kt:382 (Complexity 52 → Target < 15)

### Current Method
`suspend fun saveAllProcessedPhotos()` - Saves edited/imported photos with different logic for GALLERY vs IMPORT modes.

### Complexity Sources
- Mode branching (EditMode.GALLERY vs IMPORT)
- Conditional processing based on edit flags
- Different save paths for edited vs non-edited imports
- In-place updates mixed with new inserts
- Queue state updates

### Refactoring Strategy

#### Extract Save Operations
1. `private suspend fun saveEditedGalleryPhoto(item: EditQueueItem, categoryId: Long): Photo?`
   - Overwrites existing photo file
   - Updates database entry
   - Returns updated Photo

2. `private suspend fun saveNewImportedPhoto(item: EditQueueItem, categoryId: Long, index: Int): Photo?`
   - Creates new file
   - Inserts new database entry
   - Returns new Photo

3. `private suspend fun saveUneditedImport(item: EditQueueItem, categoryId: Long, index: Int): Photo?`
   - Saves imported photo that wasn't edited
   - Creates new file from URI

#### Extract Helper Methods
4. `private fun shouldSavePhoto(item: EditQueueItem): Boolean`
   - Determines if photo needs saving based on flags

5. `private fun updateQueueWithSavedPath(item: EditQueueItem, savedPath: String)`
   - Updates queue item with saved file path

#### Resulting Structure
```kotlin
suspend fun saveAllProcessedPhotos(): List<Photo> {
    val savedPhotos = mutableListOf<Photo>()

    _uiState.value.editQueue.forEachIndexed { index, item ->
        val savedPhoto = when {
            shouldSaveEditedPhoto(item) && editMode == EditMode.GALLERY ->
                saveEditedGalleryPhoto(item, pendingCategoryId)
            shouldSaveEditedPhoto(item) && editMode == EditMode.IMPORT ->
                saveNewImportedPhoto(item, pendingCategoryId, index)
            shouldSaveUneditedImport(item) ->
                saveUneditedImport(item, pendingCategoryId, index)
            else -> null
        }

        savedPhoto?.let { photo ->
            savedPhotos.add(photo)
            updateQueueWithSavedPath(item, photo.path)
        }
    }

    return savedPhotos
}
```

### Expected Complexity Reduction: 52 → ~9

---

## Implementation Order

### Phase 1: Independent Refactorings (Parallel Safe)
1. PinSetupScreen.kt - UI only, no business logic dependencies
2. PhotoGalleryScreen.kt - UI only, uses orchestrator
3. PhotoEditViewModel.kt - ViewModel, no dependencies on other complex methods

### Phase 2: Shared Infrastructure
4. ZipUtils.kt - Used by BackupManager, refactor first

### Phase 3: Dependent Refactorings
5. BackupManager.kt:575 (importFromJson) - Extract shared helpers first
6. BackupManager.kt:380 (importFromZip) - Reuses shared helpers from step 5

## Testing Strategy

### Per-Method Testing
- Run Android build after each method refactoring
- Verify no compilation errors
- Manual smoke test of affected feature

### Full Test Suite
- Run all Android tests after completing all refactorings
- `./android/gradlew test`
- `./android/gradlew connectedAndroidTest` (if available)

### SonarCloud Verification
- Run sonar-scanner after all changes
- Verify all 6 methods show complexity < 15
- Confirm no new code smells introduced

## Risks and Mitigations

### Risk 1: Breaking Existing Functionality
**Mitigation**: Extract methods as private helpers in same file, don't change public API

### Risk 2: Test Failures
**Mitigation**: Run tests after each refactoring, fix immediately

### Risk 3: Introducing New Complexity
**Mitigation**: Keep helper methods focused, single responsibility, no more than 2 levels of nesting

### Risk 4: Security Impact (ZipUtils)
**Mitigation**: Security review phase will specifically validate ZIP extraction changes

## Success Criteria

- [ ] All 6 methods reduced to complexity < 15
- [ ] Android build passes without errors or warnings
- [ ] All existing tests pass
- [ ] SonarCloud shows reduction in cognitive complexity issues
- [ ] No new security vulnerabilities introduced
- [ ] Git diff shows clean, focused changes