# Backup & Export Feature Documentation

## Overview
The SmilePile app provides comprehensive backup and export functionality through ZIP-based archives that include both photo files and metadata. This system replaced the previous JSON-only backup option to provide complete data portability.

## Export Functionality

### Available Export Options
- **ZIP Export Only**: As of commit `edc22c0`, JSON backup option was removed. Only ZIP format is now supported.
- **Complete Backup**: Includes all photos, categories, settings, and metadata
- **Photo File Inclusion**: Unlike previous JSON format, ZIP exports include actual photo files

### Export Process
1. **User Access**: Settings Screen → "Backup & Restore" section → "Export Data" button
2. **File Generation**: Creates timestamped ZIP file: `smilepile_backup_YYYYMMDD_HHMMSS.zip`
3. **Storage Access Framework**: Uses Android SAF for user to select save location
4. **Progress Tracking**: Real-time progress display with operation details

### Export Implementation Details

#### SettingsScreen.kt Integration
```kotlin
// Export launcher (lines 95-101)
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip")
) { uri ->
    uri?.let {
        viewModel.completeExport(it)
    }
}

// Export button (lines 333-342)
SettingsActionItem(
    title = "Export Data",
    subtitle = "Create a complete backup file (includes photos)",
    icon = Icons.Default.Archive,
    iconColor = Color(0xFF2196F3), // Blue
    onClick = {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        exportLauncher.launch("smilepile_backup_$timestamp.zip")
    }
)
```

#### SettingsViewModel.kt Export Methods
- `completeExport(uri: Uri)`: Handles the actual export process (lines 225-274)
- `prepareExport()`: Prepares export data (lines 180-219) - **UNUSED IN CURRENT UI**
- Progress tracking via `exportProgress` state

#### BackupManager.kt Core Export Logic
- `exportToZip()`: Main export function (lines 65-189)
- Creates temporary staging directory
- Copies photo files with integrity checksums
- Generates metadata.json with complete app state
- Creates ZIP archive using ZipUtils

## Import Functionality

### Import Capabilities
- **Automatic Format Detection**: Detects ZIP or JSON format automatically
- **Import Strategies**:
  - `MERGE`: Adds new data, updates existing categories, skips duplicate photos
  - `REPLACE`: Clears all data and imports fresh (preserves default categories)
- **Progress Tracking**: Real-time import progress with error reporting

### Import Process
1. **User Access**: Settings Screen → "Backup & Restore" section → "Import Data" button
2. **File Selection**: Uses Android SAF to select backup file
3. **Format Detection**: Automatically detects ZIP vs JSON format
4. **Strategy Selection**: Currently defaults to MERGE strategy
5. **Progress Display**: Shows current operation and progress

### Import Implementation Details

#### SettingsScreen.kt Integration
```kotlin
// Import launcher (lines 103-110)
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let {
        viewModel.importFromUri(it)
    }
}

// Import button (lines 344-352)
SettingsActionItem(
    title = "Import Data",
    subtitle = "Restore from a backup file",
    icon = Icons.Default.CloudDownload,
    iconColor = Color(0xFF2196F3), // Blue
    onClick = {
        importLauncher.launch(arrayOf("application/zip", "*/*"))
    }
)
```

#### Import Strategy Behavior (ImportStrategy.kt)
- **MERGE**: Preserves existing data, adds new items, updates categories by name
- **REPLACE**: Deletes all existing data (except default categories), imports fresh

## File Structure

### ZIP Archive Structure
```
smilepile_backup_YYYYMMDD_HHMMSS.zip
├── metadata.json          # Complete app backup data
└── photos/                # Directory containing photo files
    ├── {photoId}_filename1.jpg
    ├── {photoId}_filename2.png
    └── ...
```

### metadata.json Content
- **App metadata**: Version, export date, app version
- **Categories**: All category data with IDs, names, colors, positions
- **Photos**: Photo metadata with database IDs, paths, categories
- **Settings**: Theme settings, security settings summary (no actual PINs)
- **Photo Manifest**: Maps database photos to ZIP file entries with checksums

### Photo File Handling
- **File Naming**: `{databaseId}_{originalFileName}`
- **Integrity Checking**: MD5 checksums calculated for each photo
- **Path Resolution**: Internal app storage paths updated during import
- **Asset Photos**: Photos from app assets are excluded from file copying

## Integration Points

### Settings Screen Access
- **Location**: Dedicated "Backup & Restore" section with blue accent color
- **Statistics Display**: Shows current library contents (X photos in Y categories)
- **Progress Dialogs**: Real-time feedback for both export and import operations

### Progress Tracking
- **Export Progress**: Shows file copying, archive creation stages
- **Import Progress**: Shows validation, extraction, data import stages
- **Error Handling**: Displays errors and warnings during operations

## Security & Data Handling

### Security Settings
- **PIN/Pattern Exclusion**: Actual PIN values never exported for security
- **Settings Summary**: Only boolean flags (hasPIN, biometricEnabled, etc.)
- **User Control**: All operations require explicit user consent via SAF

### File Management
- **Temporary Files**: Uses app cache directory for staging
- **Cleanup**: Automatic cleanup of temporary files after operations
- **Permissions**: Uses Storage Access Framework, no special permissions required

## Error Handling

### Common Error Scenarios
- **Missing Photos**: Gracefully handles when original photos no longer exist
- **Invalid Backups**: Version compatibility checking
- **Storage Issues**: Proper error messages for write/read failures
- **Corruption**: ZIP structure validation before import

### User Feedback
- **Progress Updates**: Real-time operation status
- **Error Messages**: Clear error descriptions
- **Warnings**: Non-fatal issues (missing photos, duplicates)
- **Completion Status**: Success/failure notification

## Version Compatibility

### Backup Versions
- **Version 1**: JSON format (legacy, still supported for import)
- **Version 2**: ZIP format with photo files (current standard)
- **Compatibility Range**: Versions 1-2 supported for import

### Migration Path
- JSON backups can still be imported
- All new exports use ZIP format
- No automatic conversion from JSON to ZIP

## Performance Considerations

### Export Performance
- **Streaming**: Files processed individually to manage memory
- **Progress Callbacks**: Regular updates without blocking UI
- **Background Processing**: All I/O operations on background threads

### Import Performance
- **Validation First**: ZIP structure validated before extraction
- **Batch Processing**: Categories imported before photos
- **Memory Management**: Large files handled with streaming

## Testing Coverage

### Available Tests
- **Wave11BackupTests.kt**: Comprehensive backup/export testing
- **BackupManagerUnitTests.kt**: Unit tests for BackupManager
- **ZipUtilsSecurityTests.kt**: Security validation for ZIP operations
- **Wave3FeatureTests.kt**: End-to-end backup functionality tests

## Known Limitations

### Current Restrictions
1. **No Cloud Storage**: Local device backups only
2. **Manual Process**: No automatic backup scheduling
3. **Single Strategy**: Import defaults to MERGE (no UI strategy selection)
4. **Asset Handling**: App asset photos not included in backups

### Future Considerations
- Cloud storage integration
- Automated backup schedules
- Import strategy selection UI
- Backup encryption options

## Implementation Status: ✅ COMPLETE

**Last Updated**: Wave 11 implementation
**JSON Removal**: Commit edc22c0 (refactor: Remove JSON backup option, use ZIP only)
**Current Status**: Fully functional ZIP-based backup system with comprehensive photo support