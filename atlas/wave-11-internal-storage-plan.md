# ATLAS Wave 11: Internal Photo Storage with ZIP Export/Import

## ATLAS Methodology Alignment

### Core Principles Applied
1. **Quality Over Speed**: Ensure data integrity during migration, no photo loss
2. **Evidence-Based Development**: Test with real devices, verify transfers
3. **Elimination Over Addition**: Replace JSON backup with ZIP, remove external dependencies
4. **Prevention Over Correction**: Validate ZIP structure before import
5. **Clarity Over Cleverness**: Simple ZIP format (metadata.json + photos/)

## Phase Structure (Following ATLAS Wave Pattern)

### **Phase 1: Analysis & Research (30 mins)**
**Agent Type**: Security Researcher + Backend Analyst
**Parallel Tasks**:
1. Audit current storage paths and photo references
2. Research Android ZIP APIs and best practices
3. Analyze backup/restore data flow

**Evidence Required**:
- List of all photo storage locations
- ZIP library compatibility report
- Data migration risk assessment

### **Phase 2: Core Implementation (2 hours)**
**Agent Type**: Backend Developer (Primary)

#### Task 2.1: Create ZipUtils.kt
```kotlin
// New file in storage package
- createZipFromDirectory(sourceDir, outputFile)
- extractZipToDirectory(zipFile, targetDir)
- addFileToZip(zipStream, file, entryName)
- validateZipStructure(zipFile): Boolean
```

#### Task 2.2: Update BackupManager
- Convert `exportToJson()` → `exportToZip()`
  - Bundle metadata.json + photos/ directory
  - Use application/zip MIME type
  - Extension: .smilepile.zip
- Convert `importFromJson()` → `importFromZip()`
  - Extract to temp, validate structure
  - Copy photos to internal storage
  - Update database paths

#### Task 2.3: Update BackupModels
- Increment CURRENT_BACKUP_VERSION to 2
- Add migration handling for v1 → v2

**Evidence Required**:
- ZipUtils unit tests passing
- Backup manager compilation success
- Version migration test

### **Phase 3: UI Integration (1 hour)**
**Agent Type**: UI Developer

#### Task 3.1: Update SettingsViewModel
- Handle ZIP file creation/selection
- Show bundling progress (photos + metadata)
- Update file extensions and MIME types

#### Task 3.2: Update Import Flow
- Accept .zip files in file picker
- Show extraction progress
- Display photo count during import

**Evidence Required**:
- Screenshot of export dialog
- Screenshot of import progress
- Video of complete export/import cycle

### **Phase 4: Storage Consolidation (30 mins)**
**Agent Type**: Backend Developer

#### Task 4.1: Update StorageManager
- Add `getAllInternalPhotos()` method
- Remove external storage references
- Ensure consistent internal paths

#### Task 4.2: Update PhotoImportViewModel
- Store only internal paths in database
- Remove MediaStore URI validation on import

**Evidence Required**:
- All photos using internal storage paths
- No external storage dependencies remain

### **Phase 5: Testing & Validation (1 hour)**
**Agent Type**: QA Specialist + Performance Reviewer

#### Test Scenarios:
1. **Basic Export/Import**:
   - Import 10 photos
   - Export as ZIP
   - Clear app data
   - Import ZIP
   - Verify all photos restored

2. **Device Transfer**:
   - Export from emulator
   - Transfer ZIP to device
   - Import on device
   - Verify photos and categories

3. **Edge Cases**:
   - Large photo sets (100+ photos)
   - Special characters in filenames
   - Corrupted ZIP handling

**Evidence Required**:
- Test execution logs
- Performance metrics (ZIP creation time)
- Storage usage before/after
- Screenshots from both devices

## File Changes Summary

### New Files:
1. `android/app/src/main/java/com/smilepile/storage/ZipUtils.kt`

### Modified Files:
1. `BackupManager.kt` - ZIP export/import
2. `BackupModels.kt` - Version 2
3. `SettingsViewModel.kt` - ZIP handling
4. `StorageManager.kt` - Internal storage methods
5. `PhotoImportViewModel.kt` - Internal paths only
6. `ImportStrategy.kt` - Update for ZIP format

## Validation Checkpoints

### Checkpoint 1: Core Implementation
- ZipUtils creates valid ZIP files
- BackupManager exports/imports successfully
- Version migration handled

### Checkpoint 2: UI Integration
- Export creates .smilepile.zip file
- Import accepts ZIP files
- Progress indicators work

### Checkpoint 3: Storage Consolidation
- All photos in internal storage
- No external dependencies
- Paths consistent in database

### Checkpoint 4: End-to-End Testing
- Device transfer successful
- No data loss
- Performance acceptable

## Success Criteria
✅ Photos stored internally only
✅ ZIP export includes all photos + metadata
✅ ZIP import restores complete app state
✅ Device-to-device transfer works
✅ Backward compatibility maintained
✅ < 30 second export for 100 photos

## Risk Mitigation
- **Data Loss**: Test with copies, validate ZIP before clearing
- **Large Files**: Chunked processing, progress indicators
- **Compatibility**: Support both v1 (JSON) and v2 (ZIP) formats
- **Storage Space**: Check available space before operations

## Orchestration Script Structure
```bash
#!/bin/bash
# Wave 11: Internal Photo Storage with ZIP Export/Import

init_wave()
security_analysis()
core_implementation()
ui_integration()
storage_consolidation()
testing_validation()
generate_evidence()
```

## Implementation Steps

### Step 1: Create ZipUtils.kt
```kotlin
package com.smilepile.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipUtils {
    fun createZipFromDirectory(sourceDir: File, outputZip: File): Result<Unit>
    fun extractZipToDirectory(zipFile: File, targetDir: File): Result<Unit>
    fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String)
    fun validateZipStructure(zipFile: File): Boolean
}
```

### Step 2: Update BackupManager Export
```kotlin
suspend fun exportToZip(): Result<File> {
    // 1. Create temp directory
    val tempDir = File(context.cacheDir, "export_${System.currentTimeMillis()}")

    // 2. Write metadata.json
    val metadataFile = File(tempDir, "metadata.json")
    val backupData = createBackupData()
    metadataFile.writeText(json.encodeToString(backupData))

    // 3. Copy all photos to temp/photos/
    val photosDir = File(tempDir, "photos")
    photosDir.mkdirs()
    copyInternalPhotosToDirectory(photosDir)

    // 4. Create ZIP
    val zipFile = File(context.cacheDir, "smilepile_backup.zip")
    ZipUtils().createZipFromDirectory(tempDir, zipFile)

    // 5. Clean up temp
    tempDir.deleteRecursively()

    return Result.success(zipFile)
}
```

### Step 3: Update BackupManager Import
```kotlin
suspend fun importFromZip(zipFile: File): Flow<ImportProgress> {
    // 1. Extract to temp
    val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
    ZipUtils().extractZipToDirectory(zipFile, tempDir)

    // 2. Read metadata
    val metadataFile = File(tempDir, "metadata.json")
    val backupData = json.decodeFromString<AppBackup>(metadataFile.readText())

    // 3. Copy photos to internal storage
    val photosDir = File(tempDir, "photos")
    val internalPhotosDir = File(context.filesDir, "photos")
    photosDir.listFiles()?.forEach { photo ->
        photo.copyTo(File(internalPhotosDir, photo.name), overwrite = true)
    }

    // 4. Import metadata with updated paths
    importBackupData(backupData, internalPhotosDir)

    // 5. Clean up
    tempDir.deleteRecursively()
}
```

## Total Timeline: ~5 hours with parallel execution

## Next Steps
1. Review and approve this plan
2. Exit plan mode
3. Begin Phase 1: Analysis & Research
4. Execute implementation phases in sequence
5. Validate with comprehensive testing
6. Generate ATLAS evidence documentation