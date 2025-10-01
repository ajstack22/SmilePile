# RESEARCH-BACKUP-RESTORE.md

**Research Phase for STORY-001: Backup & Restore iOS Implementation**
**Date**: 2025-10-01
**Researcher**: Claude (Phase 0 - Research & Validation)
**Status**: Complete - Ready for PM Approval

---

## Executive Summary

This document provides comprehensive research findings for implementing iOS Backup & Restore functionality to achieve parity with the existing Android implementation. The research validates that the implementation is **FEASIBLE** with no critical blockers identified.

### Key Findings

1. **ZIP Library Recommendation**: Use native NSFileCoordinator approach for iOS 14+ (no third-party dependencies needed)
2. **Android Code Analysis**: Thoroughly documented all flows and patterns
3. **iOS Equivalents**: All Android patterns have direct iOS equivalents
4. **No Critical Blockers**: Implementation can proceed with confidence

---

## Table of Contents

1. [iOS API Research](#ios-api-research)
2. [Android Code Analysis](#android-code-analysis)
3. [Android-to-iOS Mapping](#android-to-ios-mapping)
4. [Library Recommendations](#library-recommendations)
5. [iOS-Specific Considerations](#ios-specific-considerations)
6. [Implementation Recommendations](#implementation-recommendations)
7. [Potential Risks & Mitigations](#potential-risks--mitigations)

---

## iOS API Research

### Task 0.1: ZIP Handling in iOS 14+

#### Native iOS ZIP Solution (RECOMMENDED)

**Discovery**: iOS provides native ZIP creation through `NSFileCoordinator` with `.forUploading` option.

**Implementation Pattern**:
```swift
let coordinator = NSFileCoordinator()
var error: NSError?
coordinator.coordinate(readingItemAt: sourceURL, options: [.forUploading], error: &error) { zipURL in
    // zipURL contains the automatically created ZIP file
    try? FileManager.default.copyItem(at: zipURL, to: destinationURL)
}
```

**Advantages**:
- No third-party dependencies
- Native iOS API since iOS 8
- Automatic compression
- Built-in security (sandbox compliance)
- Zero maintenance burden

**Disadvantages**:
- Less granular control over compression level
- No built-in progress callbacks
- May create temporary files

#### Alternative: ZIPFoundation (NOT RECOMMENDED for this project)

**Research Findings**:
- ZIPFoundation is well-maintained and supports iOS 14.0+
- Provides fine-grained control (compression levels, progress tracking, in-memory archives)
- Adds dependency management complexity
- Requires adding Swift Package Manager dependency

**Recommendation**: **Avoid third-party dependency** unless native solution proves insufficient during implementation.

---

### Task 0.1: FileManager API for iOS 14+

**Relevant APIs**:
- `FileManager.default.createDirectory(at:withIntermediateDirectories:)`
- `FileManager.default.copyItem(at:to:)`
- `FileManager.default.removeItem(at:)`
- `FileManager.default.contentsOfDirectory(at:includingPropertiesForKeys:)`
- `FileManager.default.attributesOfItem(atPath:)` - for file size

**iOS 14+ Changes**: No breaking changes affecting our use case.

**iOS Sandbox Compliance**:
- Documents directory: Full read/write access
- Temporary directory: Full read/write access, automatic cleanup
- Cache directory: Full read/write access, may be purged by system

**Recommended Paths**:
```swift
// Working directory for backup staging
let backupTempDir = FileManager.default.temporaryDirectory
    .appendingPathComponent("backup_\(UUID().uuidString)")

// Final backup output
let documentsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
let backupZip = documentsDir.appendingPathComponent("SmilePile_Backup_\(timestamp).zip")
```

---

### Task 0.1: UIActivityViewController / UIDocumentPickerViewController

#### UIActivityViewController (Export/Share)

**Usage for Backup Export**:
```swift
let activityVC = UIActivityViewController(
    activityItems: [backupZipURL],
    applicationActivities: nil
)
activityVC.completionWithItemsHandler = { activityType, completed, returnedItems, error in
    if completed {
        // User completed share action (saved to Files, AirDropped, etc.)
        cleanupTemporaryFiles()
    }
}
present(activityVC, animated: true)
```

**Best Practices**:
- Works for sharing files to Files app, AirDrop, email, cloud storage
- Automatically handles file security-scoped resources
- Must clean up temporary files in completion handler
- iPad: Must present as popover with source rect

#### UIDocumentPickerViewController (Import)

**iOS 14+ Implementation**:
```swift
import UniformTypeIdentifiers

let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.zip], asCopy: true)
picker.delegate = self
picker.allowsMultipleSelection = false
present(picker, animated: true)

// Delegate method
func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
    guard let zipURL = urls.first else { return }

    // CRITICAL: Start accessing security-scoped resource
    guard zipURL.startAccessingSecurityScopedResource() else {
        return
    }

    defer {
        zipURL.stopAccessingSecurityScopedResource()
    }

    // Process backup file
    processBackupFile(at: zipURL)
}
```

**iOS 14+ Changes**:
- Old: `UIDocumentPickerViewController(documentTypes: [String], in: .import)` (DEPRECATED)
- New: `UIDocumentPickerViewController(forOpeningContentTypes: [UTType], asCopy: Bool)`
- Must use `UniformTypeIdentifiers` framework
- **CRITICAL**: Must call `startAccessingSecurityScopedResource()` and `stopAccessingSecurityScopedResource()`

**Security Best Practice**:
```swift
// Wrap in defer to ensure cleanup
defer { zipURL.stopAccessingSecurityScopedResource() }
```

---

### Task 0.1: Swift Concurrency Patterns (async/await)

**Memory-Efficient File Operations**:

**Problem**: Loading entire files into memory is dangerous for large photo collections.

**Solution**: Use `AsyncSequence` with chunked processing.

**Pattern for Large File Copy**:
```swift
func copyPhotoWithProgress(from source: URL, to destination: URL) async throws {
    let handle = try FileHandle(forReadingFrom: source)
    defer { try? handle.close() }

    let writeHandle = try FileHandle(forWritingTo: destination)
    defer { try? writeHandle.close() }

    let chunkSize = 8192 // 8KB chunks
    var totalBytesRead: UInt64 = 0

    while let chunk = try handle.read(upToCount: chunkSize) {
        try writeHandle.write(contentsOf: chunk)
        totalBytesRead += UInt64(chunk.count)

        // Yield to allow UI updates
        await Task.yield()
    }
}
```

**Best Practices**:
1. **Always use async/await** for file I/O (avoid blocking main thread)
2. **Process in chunks** (never load entire file into memory)
3. **Use Task.yield()** periodically to allow UI updates
4. **Use defer** to ensure file handle cleanup
5. **Avoid nested async operations** (creates context switching overhead)

**Performance Considerations**:
- Foundation's `FileManager.copyItem()` is synchronous and blocks the thread
- For large files (>10MB), use chunked async copying
- For small files (<1MB), synchronous copy is acceptable if wrapped in `Task { }`

---

### Task 0.1: Memory Management for Large File Operations

**Research Findings**:

**Problem**: Backup operations can involve hundreds of photos (100MB+ each).

**iOS Memory Limits**:
- Background tasks: ~30MB peak memory
- Foreground tasks: ~200MB recommended peak
- System will terminate app if memory exceeds ~500MB

**Best Practices**:

1. **Batch Processing**:
```swift
let batchSize = 10
for batch in photos.chunked(into: batchSize) {
    for photo in batch {
        try await processPhoto(photo)
    }
    // Allow memory to be released between batches
    await Task.yield()
}
```

2. **Autoreleasepool for Image Processing**:
```swift
for photo in photos {
    try await withAutoreleasePool {
        let imageData = try Data(contentsOf: photo.path)
        try imageData.write(to: destinationPath)
    }
}
```

3. **Progress Tracking Without Retaining All Objects**:
```swift
// BAD: Retains all photo objects in memory
let allPhotos = try await photoRepository.getAllPhotos()

// GOOD: Stream photos on-demand
let photoCount = try await photoRepository.getPhotoCount()
for i in 0..<photoCount {
    let photo = try await photoRepository.getPhotoByIndex(i)
    try await processPhoto(photo)
}
```

**Recommendation**: Implement batch processing with max 10-20 photos per batch.

---

## Android Code Analysis

### Task 0.2: Android Backup Flow (BackupManager.kt)

**High-Level Flow**:
```
1. exportToZip()
   ├── Create temp directory
   ├── Gather data from repositories (categories, photos, settings)
   ├── Create photoManifest (tracks each photo file)
   ├── Copy photos to staging directory
   ├── Generate metadata.json
   ├── Create ZIP from staging directory
   ├── Return ZIP file path
   └── Clean up temp directory
```

**Detailed Step-by-Step**:

#### Step 1: Create Working Directory
```kotlin
val workDir = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
workDir.mkdirs()
val photosDir = File(workDir, "photos")
photosDir.mkdirs()
```

**iOS Equivalent**:
```swift
let workDir = FileManager.default.temporaryDirectory
    .appendingPathComponent("backup_temp_\(Date().timeIntervalSince1970)")
try FileManager.default.createDirectory(at: workDir, withIntermediateDirectories: true)
let photosDir = workDir.appendingPathComponent("photos")
try FileManager.default.createDirectory(at: photosDir, withIntermediateDirectories: true)
```

#### Step 2: Gather Data from Repositories
```kotlin
val categories = categoryRepository.getAllCategories()
val photos = photoRepository.getAllPhotos()
val isDarkMode = themeManager.isDarkMode.first()
val securitySummary = securePreferencesManager.getSecuritySummary()
```

**iOS Equivalent**:
```swift
let categories = try await categoryRepository.getAllCategories()
let photos = try await photoRepository.getAllPhotos()
let isDarkMode = (SettingsManager.shared.themeMode == .dark)
let hasPIN = PINManager.shared.hasPIN()
```

#### Step 3: Copy Photos to Staging Directory
```kotlin
photos.forEachIndexed { index, photo ->
    if (!photo.isFromAssets) {
        val sourceFile = File(photo.path)
        if (sourceFile.exists()) {
            val fileName = "${photo.id}_${sourceFile.name}"
            val destFile = File(photosDir, fileName)
            sourceFile.copyTo(destFile, overwrite: true)

            val checksum = calculateMD5(destFile)

            photoManifest.add(
                PhotoManifestEntry(
                    photoId = photo.id,
                    originalPath = photo.path,
                    zipEntryName = "photos/$fileName",
                    fileName = fileName,
                    fileSize = destFile.length(),
                    checksum = checksum
                )
            )
        }
    }
    progressCallback?.invoke(30 + ((index + 1) * 40 / photos.size), 100, "Copying photos")
}
```

**iOS Equivalent**:
```swift
var photoManifest: [PhotoManifestEntry] = []

for (index, photo) in photos.enumerated() {
    if !photo.isFromAssets {
        let sourceURL = URL(fileURLWithPath: photo.path)
        if FileManager.default.fileExists(atPath: photo.path) {
            let fileName = "\(photo.id)_\(sourceURL.lastPathComponent)"
            let destURL = photosDir.appendingPathComponent(fileName)
            try FileManager.default.copyItem(at: sourceURL, to: destURL)

            let checksum = try calculateMD5(of: destURL)
            let fileSize = try FileManager.default.attributesOfItem(atPath: destURL.path)[.size] as! Int64

            photoManifest.append(
                PhotoManifestEntry(
                    photoId: photo.id,
                    originalPath: photo.path,
                    zipEntryName: "photos/\(fileName)",
                    fileName: fileName,
                    fileSize: fileSize,
                    checksum: checksum
                )
            )
        }
    }
    let progress = 30 + ((index + 1) * 40 / photos.count)
    progressCallback?(progress, 100, "Copying photos (\(index + 1)/\(photos.count))")
}
```

#### Step 4: Create metadata.json
```kotlin
val appBackup = AppBackup(
    version = CURRENT_BACKUP_VERSION,
    exportDate = System.currentTimeMillis(),
    appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
    format = BackupFormat.ZIP.name,
    categories = backupCategories,
    photos = backupPhotos,
    settings = backupSettings,
    photoManifest = photoManifest
)

val metadataFile = File(workDir, "metadata.json")
val jsonString = json.encodeToString(appBackup)
metadataFile.writeText(jsonString)
```

**iOS Equivalent**:
```swift
let appBackup = AppBackup(
    version: CURRENT_BACKUP_VERSION,
    exportDate: Int64(Date().timeIntervalSince1970 * 1000),
    appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
    format: "ZIP",
    categories: backupCategories,
    photos: backupPhotos,
    settings: backupSettings,
    photoManifest: photoManifest
)

let encoder = JSONEncoder()
encoder.outputFormatting = .prettyPrinted
let jsonData = try encoder.encode(appBackup)

let metadataURL = workDir.appendingPathComponent("metadata.json")
try jsonData.write(to: metadataURL)
```

#### Step 5: Create ZIP Archive
```kotlin
val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
val zipFile = File(context.cacheDir, "SmilePile_Backup_${timestamp}.zip")

val zipResult = ZipUtils.createZipFromDirectory(
    sourceDir = workDir,
    outputFile = zipFile,
    compressionLevel = Deflater.DEFAULT_COMPRESSION
) { current, total ->
    val progress = 80 + (current * 20 / total)
    progressCallback?.invoke(progress, 100, "Archiving files ($current/$total)")
}
```

**iOS Equivalent**:
```swift
let dateFormatter = DateFormatter()
dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
let timestamp = dateFormatter.string(from: Date())
let zipFileName = "SmilePile_Backup_\(timestamp).zip"
let zipURL = FileManager.default.temporaryDirectory.appendingPathComponent(zipFileName)

// Use NSFileCoordinator to create ZIP
let coordinator = NSFileCoordinator()
var coordinatorError: NSError?
coordinator.coordinate(readingItemAt: workDir, options: [.forUploading], error: &coordinatorError) { zippedURL in
    try? FileManager.default.copyItem(at: zippedURL, to: zipURL)
}

if let error = coordinatorError {
    throw error
}
```

#### Step 6: Cleanup and Return
```kotlin
workDir.deleteRecursively()
progressCallback?.invoke(100, 100, "Export completed")
Result.success(zipFile)
```

**iOS Equivalent**:
```swift
try FileManager.default.removeItem(at: workDir)
progressCallback?(100, 100, "Export completed")
return zipURL
```

---

### Task 0.2: Android Restore Flow (RestoreManager.kt)

**High-Level Flow**:
```
1. restoreFromBackup()
   ├── Validate backup file (ZIP structure, version, integrity)
   ├── Create rollback snapshot (if strategy == REPLACE)
   ├── Extract ZIP to temp directory
   ├── Read metadata.json
   ├── Clear existing data (if strategy == REPLACE)
   ├── Restore categories
   ├── Restore photos (copy files + insert DB records)
   ├── Restore settings
   ├── Clean up temp directory
   └── Return import result
```

**Detailed Step-by-Step**:

#### Step 1: Validate Backup
```kotlin
val validationResult = validateBackup(backupFile, options.validateIntegrity).getOrThrow()
if (!validationResult.isValid) {
    errors.addAll(validationResult.errors)
    emit(ImportProgress(0, 0, "Validation failed", errors))
    return@flow
}
```

**iOS Equivalent**:
```swift
let validationResult = try await validateBackup(backupURL, checkIntegrity: options.validateIntegrity)
guard validationResult.isValid else {
    throw RestoreError.validationFailed(validationResult.errors)
}
```

#### Step 2: Create Rollback Snapshot
```kotlin
if (options.strategy == ImportStrategy.REPLACE) {
    rollbackData = createRollbackSnapshot()
}

// createRollbackSnapshot() implementation:
private suspend fun createRollbackSnapshot(): RollbackData {
    val categories = categoryRepository.getAllCategories()
    val photos = photoRepository.getAllPhotos()
    val isDarkMode = themeManager.isDarkMode.value

    return RollbackData(
        categories = categories,
        photos = photos,
        isDarkMode = isDarkMode,
        rollbackDir = File(context.cacheDir, "rollback_${System.currentTimeMillis()}")
    )
}
```

**iOS Equivalent**:
```swift
var rollbackData: RollbackData?
if options.strategy == .replace {
    rollbackData = try await createRollbackSnapshot()
}

// createRollbackSnapshot() implementation:
private func createRollbackSnapshot() async throws -> RollbackData {
    let categories = try await categoryRepository.getAllCategories()
    let photos = try await photoRepository.getAllPhotos()
    let isDarkMode = SettingsManager.shared.themeMode == .dark

    return RollbackData(
        categories: categories,
        photos: photos,
        isDarkMode: isDarkMode,
        rollbackDir: FileManager.default.temporaryDirectory
            .appendingPathComponent("rollback_\(Date().timeIntervalSince1970)")
    )
}
```

#### Step 3: Extract ZIP
```kotlin
val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
tempDir.mkdirs()

val extractResult = ZipUtils.extractZip(zipFile, tempDir)
if (extractResult.isFailure) {
    throw Exception("Failed to extract backup: ${extractResult.exceptionOrNull()?.message}")
}
```

**iOS Equivalent**:
```swift
let tempDir = FileManager.default.temporaryDirectory
    .appendingPathComponent("restore_temp_\(Date().timeIntervalSince1970)")
try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)

try await extractZip(from: backupURL, to: tempDir)
```

#### Step 4: Read metadata.json
```kotlin
val metadataFile = File(tempDir, "metadata.json")
val backupData = json.decodeFromString<AppBackup>(metadataFile.readText())
```

**iOS Equivalent**:
```swift
let metadataURL = tempDir.appendingPathComponent("metadata.json")
let jsonData = try Data(contentsOf: metadataURL)
let decoder = JSONDecoder()
let backupData = try decoder.decode(AppBackup.self, from: jsonData)
```

#### Step 5: Clear Existing Data (if REPLACE)
```kotlin
if (options.strategy == ImportStrategy.REPLACE) {
    clearAllData()
}

// clearAllData() implementation:
private suspend fun clearAllData() {
    // Delete photos first (foreign key constraints)
    val allPhotos = photoRepository.getAllPhotos()
    allPhotos.forEach { photo ->
        photoRepository.deletePhoto(photo)
    }

    // Delete categories
    val allCategories = categoryRepository.getAllCategories()
    allCategories.forEach { category ->
        if (!category.isDefault) {
            categoryRepository.deleteCategory(category)
        }
    }
}
```

**iOS Equivalent**:
```swift
if options.strategy == .replace {
    try await clearAllData()
}

// clearAllData() implementation:
private func clearAllData() async throws {
    // Delete photos first (foreign key constraints)
    let allPhotos = try await photoRepository.getAllPhotos()
    for photo in allPhotos {
        try await photoRepository.deletePhoto(photo)
    }

    // Delete non-default categories
    let allCategories = try await categoryRepository.getAllCategories()
    for category in allCategories where !category.isDefault {
        try await categoryRepository.deleteCategory(category)
    }
}
```

#### Step 6: Restore Categories
```kotlin
for (categoryBackup in backupData.categories) {
    val existingCategory = categoryRepository.getCategoryByName(categoryBackup.name)

    if (strategy == ImportStrategy.MERGE && existingCategory != null) {
        val updatedCategory = categoryBackup.toCategory().copy(id = existingCategory.id)
        categoryRepository.updateCategory(updatedCategory)
        warnings.add("Updated existing category: ${categoryBackup.displayName}")
    } else {
        val categoryToInsert = if (strategy == ImportStrategy.REPLACE) {
            categoryBackup.toCategory()
        } else {
            categoryBackup.toCategory().copy(id = 0) // Auto-generate ID
        }
        categoryRepository.insertCategory(categoryToInsert)
        categoriesImported++
    }
}
```

**iOS Equivalent**:
```swift
for categoryBackup in backupData.categories {
    let existingCategory = try? await categoryRepository.getCategoryByName(categoryBackup.name)

    if options.strategy == .merge, let existing = existingCategory {
        // Update existing
        var updated = categoryBackup.toCategory()
        updated.id = existing.id
        try await categoryRepository.updateCategory(updated)
        warnings.append("Updated existing category: \(categoryBackup.displayName)")
    } else {
        // Insert new
        var categoryToInsert = categoryBackup.toCategory()
        if options.strategy != .replace {
            categoryToInsert.id = 0 // Let CoreData auto-generate
        }
        try await categoryRepository.insertCategory(categoryToInsert)
        categoriesImported += 1
    }
}
```

#### Step 7: Restore Photos
```kotlin
val photosDir = File(tempDir, "photos")
val internalPhotosDir = File(context.filesDir, "photos")
internalPhotosDir.mkdirs()

for (photoBackup in backupData.photos) {
    val manifestEntry = backupData.photoManifest.find { it.photoId == photoBackup.id }
    var newPhotoPath = photoBackup.path

    if (manifestEntry != null) {
        // Restore photo file
        val sourceFile = File(photosDir, manifestEntry.fileName)
        if (sourceFile.exists()) {
            val destFile = File(internalPhotosDir, manifestEntry.fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            newPhotoPath = destFile.absolutePath
            photoFilesRestored++
        }
    }

    // Check for duplicates
    if (strategy == ImportStrategy.MERGE) {
        val existingPhotos = photoRepository.getAllPhotos()
        val isDuplicate = existingPhotos.any { it.path == newPhotoPath }

        if (isDuplicate) {
            photosSkipped++
            continue
        }
    }

    // Insert photo
    val photoToInsert = photoBackup.toPhoto().copy(path = newPhotoPath)
    photoRepository.insertPhoto(photoToInsert)
    photosImported++
}
```

**iOS Equivalent**:
```swift
let photosDir = tempDir.appendingPathComponent("photos")
let internalPhotosDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    .appendingPathComponent("photos")
try FileManager.default.createDirectory(at: internalPhotosDir, withIntermediateDirectories: true)

for photoBackup in backupData.photos {
    let manifestEntry = backupData.photoManifest.first { $0.photoId == photoBackup.id }
    var newPhotoPath = photoBackup.path
    var fileRestored = false

    if let manifestEntry = manifestEntry {
        // Restore photo file
        let sourceURL = photosDir.appendingPathComponent(manifestEntry.fileName)
        if FileManager.default.fileExists(atPath: sourceURL.path) {
            let destURL = internalPhotosDir.appendingPathComponent(manifestEntry.fileName)
            try FileManager.default.copyItem(at: sourceURL, to: destURL)
            newPhotoPath = destURL.path
            fileRestored = true
        }
    }

    // Check for duplicates
    if options.strategy == .merge {
        let existingPhotos = try await photoRepository.getAllPhotos()
        let isDuplicate = existingPhotos.contains { $0.path == newPhotoPath }

        if isDuplicate {
            photosSkipped += 1
            continue
        }
    }

    // Insert photo
    var photoToInsert = photoBackup.toPhoto()
    photoToInsert.path = newPhotoPath
    try await photoRepository.insertPhoto(photoToInsert)
    photosImported += 1
}
```

#### Step 8: Restore Settings
```kotlin
if (options.restoreSettings && backupData.settings != null) {
    themeManager.setThemeMode(
        if (settings.isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
    )

    // Note: Security settings like PIN/pattern are NOT restored for security reasons
}
```

**iOS Equivalent**:
```swift
if options.restoreSettings, let settings = backupData.settings {
    SettingsManager.shared.themeMode = settings.isDarkMode ? .dark : .light

    // Note: PINs are NOT restored for security reasons
}
```

#### Step 9: Cleanup
```kotlin
tempDir.deleteRecursively()
progressCallback?.invoke(100, 100, "Restore completed")
emit(ImportProgress(totalItems, processedItems, "Restore completed", errors))
```

**iOS Equivalent**:
```swift
try FileManager.default.removeItem(at: tempDir)
progressCallback?(100, 100, "Restore completed")
return ImportResult(success: true, categoriesImported: categoriesImported, photosImported: photosImported, errors: errors)
```

---

### Task 0.2: Android Data Models (BackupModels.kt)

**Key Data Structures**:

```kotlin
data class AppBackup(
    val version: Int = CURRENT_BACKUP_VERSION,
    val exportDate: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val format: String = BackupFormat.ZIP.name,
    val categories: List<BackupCategory>,
    val photos: List<BackupPhoto>,
    val settings: BackupSettings,
    val photoManifest: List<PhotoManifestEntry> = emptyList()
)

data class BackupCategory(
    val id: Long,
    val name: String,
    val displayName: String,
    val position: Int,
    val iconResource: String? = null,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long
)

data class BackupPhoto(
    val id: Long,
    val path: String,
    val categoryId: Long,
    val name: String,
    val isFromAssets: Boolean = false,
    val createdAt: Long,
    val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0
)

data class BackupSettings(
    val isDarkMode: Boolean,
    val securitySettings: BackupSecuritySettings
)

data class BackupSecuritySettings(
    val hasPIN: Boolean,
    val hasPattern: Boolean,
    val kidSafeModeEnabled: Boolean,
    val deleteProtectionEnabled: Boolean
)

data class PhotoManifestEntry(
    val photoId: Long,
    val originalPath: String,
    val zipEntryName: String,
    val fileName: String,
    val fileSize: Long,
    val checksum: String? = null
)

enum class ImportStrategy {
    REPLACE,  // Delete all existing data, then import
    MERGE,    // Merge imported data with existing
    SKIP      // Skip duplicates
}
```

**iOS Equivalent**:
```swift
struct AppBackup: Codable {
    let version: Int
    let exportDate: Int64
    let appVersion: String
    let format: String
    let categories: [BackupCategory]
    let photos: [BackupPhoto]
    let settings: BackupSettings
    let photoManifest: [PhotoManifestEntry]
}

struct BackupCategory: Codable {
    let id: Int64
    let name: String
    let displayName: String
    let position: Int
    let iconResource: String?
    let colorHex: String?
    let isDefault: Bool
    let createdAt: Int64

    func toCategory() -> Category {
        return Category(
            id: id,
            name: name,
            displayName: displayName,
            position: position,
            iconResource: iconResource,
            colorHex: colorHex,
            isDefault: isDefault,
            createdAt: createdAt
        )
    }
}

struct BackupPhoto: Codable {
    let id: Int64
    let path: String
    let categoryId: Int64
    let name: String
    let isFromAssets: Bool
    let createdAt: Int64
    let fileSize: Int64
    let width: Int
    let height: Int

    func toPhoto() -> Photo {
        return Photo(
            id: id,
            path: path,
            categoryId: categoryId,
            name: name,
            isFromAssets: isFromAssets,
            createdAt: createdAt,
            fileSize: fileSize,
            width: width,
            height: height
        )
    }
}

struct BackupSettings: Codable {
    let isDarkMode: Bool
    let securitySettings: BackupSecuritySettings
}

struct BackupSecuritySettings: Codable {
    let hasPIN: Bool
    let hasPattern: Bool
    let kidSafeModeEnabled: Bool
    let deleteProtectionEnabled: Bool
}

struct PhotoManifestEntry: Codable {
    let photoId: Int64
    let originalPath: String
    let zipEntryName: String
    let fileName: String
    let fileSize: Int64
    let checksum: String?
}

enum ImportStrategy {
    case replace  // Delete all existing data, then import
    case merge    // Merge imported data with existing
    case skip     // Skip duplicates
}
```

---

## Android-to-iOS Mapping

### Concurrency Patterns

| Android (Kotlin Coroutines) | iOS (Swift async/await) | Notes |
|------------------------------|-------------------------|-------|
| `suspend fun foo()` | `func foo() async throws` | Both are non-blocking |
| `withContext(Dispatchers.IO)` | `Task.detached(priority: .background)` | Background execution |
| `kotlinx.coroutines.flow.Flow` | `AsyncStream` or `AsyncSequence` | Async data streams |
| `flow.first()` | `for await value in stream { return value }` | Get first element |
| `flow.collect { }` | `for await value in stream { }` | Iterate over stream |
| `launch { }` | `Task { }` | Fire-and-forget |
| `async { }.await()` | `async let result = ...` then `await result` | Concurrent execution |

### File System Operations

| Android | iOS | Notes |
|---------|-----|-------|
| `File(path)` | `URL(fileURLWithPath: path)` | File reference |
| `file.exists()` | `FileManager.default.fileExists(atPath:)` | Check existence |
| `file.mkdirs()` | `FileManager.default.createDirectory(at:withIntermediateDirectories:)` | Create dirs |
| `sourceFile.copyTo(destFile)` | `FileManager.default.copyItem(at:to:)` | Copy file |
| `file.delete()` | `FileManager.default.removeItem(at:)` | Delete file |
| `file.deleteRecursively()` | `FileManager.default.removeItem(at:)` | Delete dir tree |
| `file.listFiles()` | `FileManager.default.contentsOfDirectory(at:)` | List dir |
| `file.length()` | `FileManager.default.attributesOfItem(atPath:)[.size]` | File size |
| `file.lastModified()` | `FileManager.default.attributesOfItem(atPath:)[.modificationDate]` | Mod date |

### JSON Serialization

| Android (kotlinx.serialization) | iOS (Codable) | Notes |
|---------------------------------|---------------|-------|
| `@Serializable data class Foo` | `struct Foo: Codable` | Auto-generated encoding |
| `Json.encodeToString(obj)` | `try JSONEncoder().encode(obj)` | Encode to JSON |
| `Json.decodeFromString<T>(str)` | `try JSONDecoder().decode(T.self, from:)` | Decode from JSON |
| `json { prettyPrint = true }` | `encoder.outputFormatting = .prettyPrinted` | Formatting |
| `json { ignoreUnknownKeys = true }` | (default behavior) | Ignore extra keys |

### Repository Pattern

| Android | iOS | Notes |
|---------|-----|-------|
| `categoryRepository.getAllCategories()` | `try await categoryRepository.getAllCategories()` | Async call |
| `photoRepository.insertPhoto(photo)` | `try await photoRepository.insertPhoto(photo)` | Insert record |
| `photoRepository.deletePhoto(photo)` | `try await photoRepository.deletePhoto(photo)` | Delete record |
| `Repository.shared` (Singleton) | `Repository.shared` (Singleton) | Same pattern |

### Progress Callbacks

| Android | iOS | Notes |
|---------|-----|-------|
| `progressCallback: ((Int, Int, String) -> Unit)?` | `progressCallback: ((Int, Int, String) -> Void)?` | Nullable closure |
| `progressCallback?.invoke(50, 100, "Processing")` | `progressCallback?(50, 100, "Processing")` | Optional call |

### Settings/Preferences

| Android | iOS | Notes |
|---------|-----|-------|
| `SharedPreferences` | `UserDefaults` | Key-value storage |
| `prefs.getBoolean(key, default)` | `UserDefaults.standard.bool(forKey:)` | Get bool |
| `prefs.edit().putBoolean(key, value).apply()` | `UserDefaults.standard.set(value, forKey:)` | Set bool |
| `SecurePreferences` (encrypted) | `Keychain` (via KeychainManager) | Secure storage |

### Date/Time

| Android | iOS | Notes |
|---------|-----|-------|
| `System.currentTimeMillis()` | `Int64(Date().timeIntervalSince1970 * 1000)` | Milliseconds since epoch |
| `Date()` | `Date()` | Current date |
| `SimpleDateFormat("yyyyMMdd_HHmmss")` | `DateFormatter()` with `dateFormat` | Date formatting |

### ZIP Operations

| Android | iOS | Notes |
|---------|-----|-------|
| `ZipOutputStream` | `NSFileCoordinator` with `.forUploading` | Create ZIP |
| `ZipInputStream` | Custom extraction or ZIPFoundation | Extract ZIP |
| `ZipEntry` | N/A (handled by NSFileCoordinator) | Entry metadata |
| `Deflater.DEFAULT_COMPRESSION` | (automatic) | Compression level |

### Share/Export Intent

| Android | iOS | Notes |
|---------|-----|-------|
| `Intent(Intent.ACTION_CREATE_DOCUMENT)` | `UIDocumentPickerViewController(forExportingURLs:)` | Save file picker |
| `Intent.ACTION_SEND` with ZIP file | `UIActivityViewController(activityItems: [zipURL])` | Share sheet |
| `startActivityForResult()` | Present picker with delegate | Get result |

### Import Intent

| Android | iOS | Notes |
|---------|-----|-------|
| `Intent(Intent.ACTION_OPEN_DOCUMENT)` | `UIDocumentPickerViewController(forOpeningContentTypes:)` | Open file picker |
| `contentResolver.openInputStream(uri)` | `startAccessingSecurityScopedResource()` | Access file |
| | `stopAccessingSecurityScopedResource()` | Release access |

---

## Library Recommendations

### ZIP Handling: Native iOS (RECOMMENDED)

**Library**: None (use native NSFileCoordinator)

**Rationale**:
1. **No Dependencies**: Reduces project complexity and maintenance burden
2. **Native iOS API**: Guaranteed compatibility with all iOS versions
3. **Sandbox Compliant**: Built-in security and sandbox compliance
4. **Sufficient for Requirements**: All requirements can be met without third-party libs

**Implementation Strategy**:

**For ZIP Creation**:
```swift
func createZip(from sourceDir: URL, to destinationZip: URL) throws {
    let coordinator = NSFileCoordinator()
    var coordinatorError: NSError?

    coordinator.coordinate(readingItemAt: sourceDir, options: [.forUploading], error: &coordinatorError) { zippedURL in
        do {
            try FileManager.default.copyItem(at: zippedURL, to: destinationZip)
        } catch {
            coordinatorError = error as NSError
        }
    }

    if let error = coordinatorError {
        throw error
    }
}
```

**For ZIP Extraction**:
```swift
import Compression

func extractZip(from zipURL: URL, to destinationDir: URL) throws {
    // Option 1: Use unzip command (fastest, but less control)
    let process = Process()
    process.executableURL = URL(fileURLWithPath: "/usr/bin/unzip")
    process.arguments = ["-o", zipURL.path, "-d", destinationDir.path]
    try process.run()
    process.waitUntilExit()

    guard process.terminationStatus == 0 else {
        throw RestoreError.extractionFailed
    }
}
```

**Alternative**: If extraction proves problematic, fallback to ZIPFoundation for extraction only.

---

### MD5 Checksum Calculation

**Library**: CryptoKit (native iOS 13+)

**Implementation**:
```swift
import CryptoKit

func calculateMD5(of fileURL: URL) throws -> String {
    let data = try Data(contentsOf: fileURL)
    let hash = Insecure.MD5.hash(data: data)
    return hash.map { String(format: "%02hhx", $0) }.joined()
}
```

**Note**: CryptoKit's `Insecure.MD5` is appropriate for file integrity checks (not cryptographic security).

---

## iOS-Specific Considerations

### 1. Sandbox Limitations

**Issue**: iOS apps run in a sandbox with restricted file access.

**Allowed Directories**:
- `Documents/` - Persistent, user-visible, backed up by iTunes/iCloud
- `Library/Caches/` - Persistent, not backed up, can be purged by system
- `tmp/` - Temporary, deleted by system periodically

**Recommended Paths**:
```swift
// Staging directory for backup creation (will be deleted after share)
let backupTempDir = FileManager.default.temporaryDirectory
    .appendingPathComponent("backup_\(UUID().uuidString)")

// Photo storage (persistent)
let photosDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    .appendingPathComponent("photos")
```

**Security-Scoped Resources**:
- Files from UIDocumentPickerViewController require `startAccessingSecurityScopedResource()`
- Must be balanced with `stopAccessingSecurityScopedResource()`
- Use `defer` to ensure cleanup

---

### 2. Background Task Limitations

**Issue**: Long-running backups/restores may be interrupted if app backgrounded.

**Mitigation**:
1. Use `UIApplication.beginBackgroundTask()` to request extra time
2. Limit background time to ~30 seconds max
3. Show progress UI to keep user engaged (prevents backgrounding)

**Implementation**:
```swift
var backgroundTaskID: UIBackgroundTaskIdentifier = .invalid

func startBackupTask() {
    backgroundTaskID = UIApplication.shared.beginBackgroundTask { [weak self] in
        // Time expired, cleanup
        self?.cleanupBackupOperation()
        UIApplication.shared.endBackgroundTask(self!.backgroundTaskID)
        self?.backgroundTaskID = .invalid
    }

    Task {
        await performBackup()
        UIApplication.shared.endBackgroundTask(backgroundTaskID)
        backgroundTaskID = .invalid
    }
}
```

**Recommendation**: For large backups (>100 photos), keep app in foreground with progress UI.

---

### 3. Memory Constraints

**Issue**: iOS has stricter memory limits than Android (especially on older devices).

**Limits**:
- iPhone 12 and newer: ~2GB available
- iPhone 8-11: ~1GB available
- iPad: ~3GB available

**Best Practices**:
1. **Process photos in batches** (max 10-20 at a time)
2. **Use autoreleasepool** for image processing
3. **Monitor memory with weak self references**
4. **Release references immediately after use**

**Implementation**:
```swift
func copyPhotos(_ photos: [Photo]) async throws {
    let batchSize = 10
    for batch in photos.chunked(into: batchSize) {
        try await withThrowingTaskGroup(of: Void.self) { group in
            for photo in batch {
                group.addTask {
                    try await self.copyPhoto(photo)
                }
            }
            try await group.waitForAll()
        }
        // Allow memory to be released between batches
        try await Task.sleep(nanoseconds: 100_000_000) // 0.1s
    }
}
```

---

### 4. Photo Library Permissions

**Issue**: iOS requires explicit permission to access photo library.

**Mitigation**:
- Permission should already be granted (app imports photos)
- No additional permissions needed for backup (photos stored in Documents/)

**Info.plist Keys** (already present):
```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>SmilePile needs access to your photo library to let you select and organize photos for your child.</string>
```

---

### 5. UIActivityViewController iPad Behavior

**Issue**: On iPad, UIActivityViewController must be presented as popover.

**Implementation**:
```swift
func presentShareSheet(for backupURL: URL, from viewController: UIViewController, sourceView: UIView) {
    let activityVC = UIActivityViewController(
        activityItems: [backupURL],
        applicationActivities: nil
    )

    // iPad: Present as popover
    if let popover = activityVC.popoverPresentationController {
        popover.sourceView = sourceView
        popover.sourceRect = sourceView.bounds
    }

    activityVC.completionWithItemsHandler = { _, completed, _, error in
        if completed {
            self.cleanupTemporaryFiles()
        }
    }

    viewController.present(activityVC, animated: true)
}
```

---

### 6. Swift Concurrency and Main Thread

**Issue**: UI updates must happen on main thread.

**Implementation**:
```swift
Task {
    // Background work
    let result = await performBackup()

    // Update UI on main thread
    await MainActor.run {
        self.isExporting = false
        self.showSuccessMessage(result)
    }
}
```

**Alternative with @MainActor**:
```swift
@MainActor
class BackupViewModel: ObservableObject {
    @Published var isExporting = false
    @Published var progress: Double = 0.0

    func exportData() async {
        isExporting = true  // Already on main thread

        await Task.detached {
            // Background work
            return await self.backupManager.createBackup()
        }.value

        isExporting = false  // Already on main thread
    }
}
```

---

## Implementation Recommendations

### 1. Use Native iOS ZIP Solution

**Recommendation**: Use `NSFileCoordinator` with `.forUploading` for ZIP creation.

**Rationale**:
- No third-party dependencies
- Native iOS API since iOS 8
- Sufficient for requirements
- Reduces maintenance burden

**Fallback Plan**: If native extraction proves problematic, use ZIPFoundation for extraction only.

---

### 2. Mirror Android Data Models Exactly

**Recommendation**: iOS `BackupModels.swift` should match Android `BackupModels.kt` field-for-field.

**Rationale**:
- Ensures cross-platform compatibility (if needed in future)
- Simplifies testing (can validate against Android backups)
- Reduces bugs from structural mismatches

**Example**:
```swift
// iOS
struct AppBackup: Codable {
    let version: Int
    let exportDate: Int64
    let appVersion: String
    let format: String
    let categories: [BackupCategory]
    let photos: [BackupPhoto]
    let settings: BackupSettings
    let photoManifest: [PhotoManifestEntry]
}
```

---

### 3. Implement Batch Processing

**Recommendation**: Process photos in batches of 10-20 to avoid memory issues.

**Implementation**:
```swift
let batchSize = 10
for batch in photos.chunked(into: batchSize) {
    for photo in batch {
        try await processPhoto(photo)
    }
    await Task.yield() // Allow memory cleanup
}
```

---

### 4. Use Progress Callbacks for UI Responsiveness

**Recommendation**: All long-running operations should provide progress callbacks.

**Implementation**:
```swift
func createBackup(
    progressCallback: ((Int, Int, String) -> Void)?
) async throws -> URL {
    progressCallback?(0, 100, "Starting backup")

    // Step 1
    progressCallback?(20, 100, "Gathering photos")
    let photos = try await photoRepository.getAllPhotos()

    // Step 2
    progressCallback?(50, 100, "Copying photos")
    try await copyPhotos(photos, progressCallback: progressCallback)

    // Step 3
    progressCallback?(80, 100, "Creating ZIP")
    let zipURL = try await createZip()

    progressCallback?(100, 100, "Backup complete")
    return zipURL
}
```

---

### 5. Implement Rollback for REPLACE Strategy

**Recommendation**: Create snapshot before destructive operations, restore on failure.

**Implementation**:
```swift
func restoreFromBackup(options: RestoreOptions) async throws {
    var rollbackData: RollbackData?

    if options.strategy == .replace {
        rollbackData = try await createRollbackSnapshot()
    }

    do {
        try await performRestore(options: options)
    } catch {
        if let rollback = rollbackData {
            try await performRollback(rollback)
        }
        throw error
    }
}
```

---

### 6. Clean Up Temporary Files

**Recommendation**: Always clean up temp files, even on error.

**Implementation**:
```swift
func createBackup() async throws -> URL {
    let tempDir = FileManager.default.temporaryDirectory
        .appendingPathComponent("backup_\(UUID().uuidString)")

    defer {
        try? FileManager.default.removeItem(at: tempDir)
    }

    // Backup logic here
    try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
    // ...

    return finalZipURL
}
```

---

## Potential Risks & Mitigations

### Risk 1: Native ZIP Extraction Fails

**Probability**: Low
**Impact**: High
**Mitigation**: Fallback to ZIPFoundation library for extraction only
**Validation**: Test with Android-generated ZIP files during Phase 1

---

### Risk 2: Memory Issues with Large Photo Collections

**Probability**: Medium (for 200+ photos)
**Impact**: High (app crash)
**Mitigation**: Implement batch processing (10-20 photos per batch)
**Validation**: Test with 500 photos on iPhone 8 (lowest spec device)

---

### Risk 3: Background Task Timeout

**Probability**: Medium (for large backups)
**Impact**: Medium (incomplete backup)
**Mitigation**: Keep app in foreground with progress UI, use UIApplication.beginBackgroundTask()
**Validation**: Test backup/restore with app backgrounded

---

### Risk 4: Disk Space Exhaustion

**Probability**: Low
**Impact**: High (backup failure)
**Mitigation**: Check available disk space before backup, require 2x photo collection size
**Validation**: Test on device with <500MB free space

---

### Risk 5: Security-Scoped Resource Leaks

**Probability**: Low
**Impact**: Medium (memory leak)
**Mitigation**: Use `defer { url.stopAccessingSecurityScopedResource() }` pattern
**Validation**: Test import multiple times, monitor memory with Instruments

---

### Risk 6: JSON Encoding/Decoding Failures

**Probability**: Low
**Impact**: High (data loss)
**Mitigation**: Extensive testing with edge cases (empty categories, missing fields)
**Validation**: Test round-trip (export → import → export) and compare JSON

---

## Conclusion

### Go/No-Go Decision

**Recommendation**: ✅ **GO** - Implementation is feasible with no critical blockers.

### Summary of Findings

1. **ZIP Library**: Native iOS `NSFileCoordinator` is sufficient (no third-party deps needed)
2. **Android Equivalents**: All Android patterns have direct iOS equivalents
3. **iOS Models**: Can directly mirror Android models using `Codable`
4. **Repositories**: Existing iOS repositories support all required operations
5. **No Critical Blockers**: All requirements can be implemented with standard iOS APIs

### Recommended Next Steps

1. **Product Manager Review**: Approve this research document
2. **Begin Phase 1**: Implement BackupModels.swift and ZipUtils.swift
3. **Validate ZIP Operations**: Test native ZIP creation/extraction with sample files
4. **Proceed to Phase 2**: Implement BackupManager based on Android patterns

---

## Appendix: Code Examples

### Example: Complete Backup Flow

```swift
class BackupManager {
    static let shared = BackupManager()

    private let photoRepository: PhotoRepository
    private let categoryRepository: CategoryRepository

    func createBackup(
        progressCallback: ((Int, Int, String) -> Void)? = nil
    ) async throws -> URL {
        // Step 1: Create working directory
        let workDir = FileManager.default.temporaryDirectory
            .appendingPathComponent("backup_\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: workDir, withIntermediateDirectories: true)

        defer {
            try? FileManager.default.removeItem(at: workDir)
        }

        progressCallback?(10, 100, "Gathering data")

        // Step 2: Gather data
        let categories = try await categoryRepository.getAllCategories()
        let photos = try await photoRepository.getAllPhotos()
        let isDarkMode = SettingsManager.shared.themeMode == .dark
        let hasPIN = PINManager.shared.hasPIN()

        progressCallback?(30, 100, "Copying photos")

        // Step 3: Copy photos to staging
        let photosDir = workDir.appendingPathComponent("photos")
        try FileManager.default.createDirectory(at: photosDir, withIntermediateDirectories: true)

        var photoManifest: [PhotoManifestEntry] = []

        for (index, photo) in photos.enumerated() {
            if !photo.isFromAssets {
                let sourceURL = URL(fileURLWithPath: photo.path)
                if FileManager.default.fileExists(atPath: photo.path) {
                    let fileName = "\(photo.id)_\(sourceURL.lastPathComponent)"
                    let destURL = photosDir.appendingPathComponent(fileName)
                    try FileManager.default.copyItem(at: sourceURL, to: destURL)

                    let checksum = try calculateMD5(of: destURL)
                    let attrs = try FileManager.default.attributesOfItem(atPath: destURL.path)
                    let fileSize = attrs[.size] as! Int64

                    photoManifest.append(
                        PhotoManifestEntry(
                            photoId: photo.id,
                            originalPath: photo.path,
                            zipEntryName: "photos/\(fileName)",
                            fileName: fileName,
                            fileSize: fileSize,
                            checksum: checksum
                        )
                    )
                }
            }

            let progress = 30 + ((index + 1) * 40 / photos.count)
            progressCallback?(progress, 100, "Copying photos (\(index + 1)/\(photos.count))")
        }

        progressCallback?(70, 100, "Creating metadata")

        // Step 4: Create metadata.json
        let backupCategories = categories.map { BackupCategory(from: $0) }
        let backupPhotos = photos.map { BackupPhoto(from: $0) }

        let appBackup = AppBackup(
            version: CURRENT_BACKUP_VERSION,
            exportDate: Int64(Date().timeIntervalSince1970 * 1000),
            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
            format: "ZIP",
            categories: backupCategories,
            photos: backupPhotos,
            settings: BackupSettings(
                isDarkMode: isDarkMode,
                securitySettings: BackupSecuritySettings(
                    hasPIN: hasPIN,
                    hasPattern: false,
                    kidSafeModeEnabled: SettingsManager.shared.kidsModeEnabled,
                    deleteProtectionEnabled: false
                )
            ),
            photoManifest: photoManifest
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        let jsonData = try encoder.encode(appBackup)

        let metadataURL = workDir.appendingPathComponent("metadata.json")
        try jsonData.write(to: metadataURL)

        progressCallback?(80, 100, "Creating ZIP archive")

        // Step 5: Create ZIP
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: Date())
        let zipFileName = "SmilePile_Backup_\(timestamp).zip"
        let zipURL = FileManager.default.temporaryDirectory.appendingPathComponent(zipFileName)

        try createZip(from: workDir, to: zipURL)

        progressCallback?(100, 100, "Backup complete")

        return zipURL
    }

    private func createZip(from sourceDir: URL, to destinationZip: URL) throws {
        let coordinator = NSFileCoordinator()
        var coordinatorError: NSError?

        coordinator.coordinate(readingItemAt: sourceDir, options: [.forUploading], error: &coordinatorError) { zippedURL in
            do {
                try FileManager.default.copyItem(at: zippedURL, to: destinationZip)
            } catch {
                coordinatorError = error as NSError
            }
        }

        if let error = coordinatorError {
            throw error
        }
    }

    private func calculateMD5(of fileURL: URL) throws -> String {
        let data = try Data(contentsOf: fileURL)
        let hash = Insecure.MD5.hash(data: data)
        return hash.map { String(format: "%02hhx", $0) }.joined()
    }
}
```

---

**End of Research Document**
