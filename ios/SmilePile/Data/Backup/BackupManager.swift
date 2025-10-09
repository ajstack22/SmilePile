import Foundation
import CoreData

class BackupManager {
    static let shared = BackupManager()

    private let photoRepository: PhotoRepository
    private let categoryRepository: CategoryRepository
    private let settingsManager: SettingsManager
    private let keychainManager: KeychainManager
    private let fileManager = FileManager.default

    // Directory constants
    private let backupDirName = "SmilePileBackups"
    private let photosSubdir = "photos"
    private let metadataFilename = "metadata.json"

    init(
        photoRepository: PhotoRepository = PhotoRepositoryImpl(),
        categoryRepository: CategoryRepository = CategoryRepositoryImpl(),
        settingsManager: SettingsManager = SettingsManager.shared,
        keychainManager: KeychainManager = KeychainManager.shared
    ) {
        self.photoRepository = photoRepository
        self.categoryRepository = categoryRepository
        self.settingsManager = settingsManager
        self.keychainManager = keychainManager

        // Fix ADVERSARIAL-CRITICAL-5: Clean up orphaned temp files on launch
        Task {
            await cleanupOrphanedTempFiles()
        }
    }

    // MARK: - Directory Management

    func getBackupsDirectory() throws -> URL {
        let tempDir = fileManager.temporaryDirectory
        let backupDir = tempDir.appendingPathComponent(backupDirName, isDirectory: true)

        if !fileManager.fileExists(atPath: backupDir.path) {
            try fileManager.createDirectory(
                at: backupDir,
                withIntermediateDirectories: true,
                attributes: nil
            )
        }

        return backupDir
    }

    func createBackupWorkingDirectory() throws -> URL {
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let workingDirName = "backup_temp_\(timestamp)"
        let backupDir = try getBackupsDirectory()
        let workingDir = backupDir.appendingPathComponent(workingDirName, isDirectory: true)

        // Fix SECURITY-M3: Set restricted permissions (user-only access)
        let attributes: [FileAttributeKey: Any] = [
            .posixPermissions: 0o700  // User read/write/execute only
        ]

        try fileManager.createDirectory(
            at: workingDir,
            withIntermediateDirectories: true,
            attributes: attributes
        )

        return workingDir
    }

    func cleanupBackupWorkingDirectory(_ directory: URL) {
        try? fileManager.removeItem(at: directory)
    }

    // Fix ADVERSARIAL-CRITICAL-5: Clean up orphaned temp files from crashes
    func cleanupOrphanedTempFiles() async {
        guard let backupDir = try? getBackupsDirectory() else { return }

        guard let contents = try? fileManager.contentsOfDirectory(
            at: backupDir,
            includingPropertiesForKeys: [.creationDateKey],
            options: .skipsHiddenFiles
        ) else { return }

        let oneHourAgo = Date().addingTimeInterval(-3600) // 1 hour

        for item in contents {
            // Only delete temp directories (backup_temp_* or restore_temp_*)
            let filename = item.lastPathComponent
            if filename.contains("_temp_") {
                // Check file age
                if let attrs = try? fileManager.attributesOfItem(atPath: item.path),
                   let creationDate = attrs[.creationDate] as? Date,
                   creationDate < oneHourAgo {
                    // Delete old temp files
                    try? fileManager.removeItem(at: item)
                }
            }
        }
    }

    // MARK: - Data Collection

    func collectPhotos() async throws -> [Photo] {
        return try await photoRepository.getAllPhotos()
    }

    func collectCategories() async throws -> [Category] {
        return try await categoryRepository.getAllCategories()
    }

    func collectSettings() -> BackupSettings {
        // Fix CRITICAL-3: Remove security settings from metadata (SECURITY-M2)
        // Security settings should not be exported as they disclose security posture
        let isDarkMode = settingsManager.themeMode == .dark

        return BackupSettings(
            isDarkMode: isDarkMode
        )
    }

    // MARK: - Metadata Creation

    func createMetadataJSON(
        categories: [Category],
        photos: [Photo],
        settings: BackupSettings,
        photoManifest: [PhotoManifestEntry]
    ) throws -> Data {
        let backupCategories = categories.map { BackupCategory.fromCategory($0) }
        let backupPhotos = photos.map { BackupPhoto.fromPhoto($0) }

        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"

        let appBackup = AppBackup(
            version: CURRENT_BACKUP_VERSION,
            exportDate: Int64(Date().timeIntervalSince1970 * 1000),
            appVersion: appVersion,
            format: BackupFormat.zip.rawValue,
            categories: backupCategories,
            photos: backupPhotos,
            settings: settings,
            photoManifest: photoManifest
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .millisecondsSince1970

        return try encoder.encode(appBackup)
    }

    // MARK: - Photo File Operations

    func copyPhotosToBackupDirectory(
        _ photos: [Photo],
        to workingDir: URL,
        progressCallback: ((Int, Int) -> Void)? = nil
    ) async throws -> [PhotoManifestEntry] {
        let photosDir = workingDir.appendingPathComponent(photosSubdir, isDirectory: true)
        try fileManager.createDirectory(at: photosDir, withIntermediateDirectories: true, attributes: nil)

        var manifest: [PhotoManifestEntry] = []
        var processedCount = 0
        let totalCount = photos.count

        for photo in photos {
            let sourcePath = getDocumentsDirectory().appendingPathComponent(photo.path)

            // Skip if source doesn't exist
            guard fileManager.fileExists(atPath: sourcePath.path) else {
                print("Warning: Photo file not found: \(photo.path)")
                continue
            }

            let fileName = sourcePath.lastPathComponent
            let destinationPath = photosDir.appendingPathComponent(fileName)

            // Handle duplicate filenames by appending photo ID
            let finalDestination: URL
            if fileManager.fileExists(atPath: destinationPath.path) {
                let fileExt = sourcePath.pathExtension
                let baseName = sourcePath.deletingPathExtension().lastPathComponent
                let uniqueName = "\(baseName)_\(photo.id).\(fileExt)"
                finalDestination = photosDir.appendingPathComponent(uniqueName)
            } else {
                finalDestination = destinationPath
            }

            // Copy file
            try fileManager.copyItem(at: sourcePath, to: finalDestination)

            // Calculate checksum (MD5)
            let checksum = try calculateMD5(for: finalDestination)

            // Create manifest entry
            let entry = PhotoManifestEntry(
                photoId: photo.id,
                originalPath: photo.path,
                zipEntryName: "photos/\(finalDestination.lastPathComponent)",
                fileName: finalDestination.lastPathComponent,
                fileSize: photo.fileSize,
                checksum: checksum
            )
            manifest.append(entry)

            processedCount += 1
            progressCallback?(processedCount, totalCount)
        }

        return manifest
    }

    // MARK: - Backup Creation

    func createBackup(
        progressCallback: ((ExportProgress) -> Void)? = nil
    ) async throws -> URL {
        // Create working directory
        let workingDir = try createBackupWorkingDirectory()

        defer {
            // Cleanup working directory on success or failure
            cleanupBackupWorkingDirectory(workingDir)
        }

        // Fix CRITICAL-1: Calculate actual total items based on photo count
        // Step 1: Collect data to determine total items
        let categories = try await collectCategories()
        let photos = try await collectPhotos()

        // Calculate total progress items: categories + photos + metadata + ZIP
        let totalItems = categories.count + photos.count + 3

        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count,
            currentOperation: "Collecting categories...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count + photos.count,
            currentOperation: "Collecting photos...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let settings = collectSettings()

        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count + photos.count,
            currentOperation: "Collecting settings...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        // Step 2: Copy photos to working directory
        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count + photos.count,
            currentOperation: "Copying photos...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let manifest = try await copyPhotosToBackupDirectory(photos, to: workingDir) { current, total in
            let processedItems = categories.count + current
            progressCallback?(ExportProgress(
                totalItems: totalItems,
                processedItems: processedItems,
                currentOperation: "Copying photos (\(current)/\(total))...",
                currentFile: nil,
                bytesProcessed: 0,
                totalBytes: 0,
                errors: []
            ))
        }

        // Step 3: Create metadata.json
        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count + photos.count + 1,
            currentOperation: "Creating metadata...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let metadataJSON = try createMetadataJSON(
            categories: categories,
            photos: photos,
            settings: settings,
            photoManifest: manifest
        )

        let metadataPath = workingDir.appendingPathComponent(metadataFilename)
        try metadataJSON.write(to: metadataPath)

        // Step 4: Create ZIP file
        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: categories.count + photos.count + 2,
            currentOperation: "Creating ZIP archive...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let timestamp = DateFormatter.backupFilename.string(from: Date())
        let zipFilename = "SmilePileBackup_\(timestamp).zip"
        let backupDir = try getBackupsDirectory()
        let zipPath = backupDir.appendingPathComponent(zipFilename)

        try await ZipUtils.createZip(from: workingDir, to: zipPath) { progress in
            let zipProgress = categories.count + photos.count + 2
            progressCallback?(ExportProgress(
                totalItems: totalItems,
                processedItems: zipProgress,
                currentOperation: "Compressing...",
                currentFile: nil,
                bytesProcessed: 0,
                totalBytes: 0,
                errors: []
            ))
        }

        // Step 5: Done
        progressCallback?(ExportProgress(
            totalItems: totalItems,
            processedItems: totalItems,
            currentOperation: "Backup complete",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        return zipPath
    }

    // MARK: - Clear All Data

    /// Clears only data (photos, categories) without touching settings
    /// This prevents SwiftUI re-render issues while the Settings view is still active
    func clearDataOnly() async throws {
        // 1. Delete all photos from filesystem
        let photos = try await photoRepository.getAllPhotos()
        let documentsDir = getDocumentsDirectory()

        // Batch delete photo files in parallel
        await withTaskGroup(of: Void.self) { group in
            for photo in photos {
                group.addTask {
                    let photoPath = documentsDir.appendingPathComponent(photo.path)
                    if self.fileManager.fileExists(atPath: photoPath.path) {
                        try? self.fileManager.removeItem(at: photoPath)
                    }
                }
            }
        }

        // 2. Batch delete all categories from CoreData
        let context = PersistenceController.shared.container.viewContext
        let categoryRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "CategoryEntity")
        let categoryBatchDelete = NSBatchDeleteRequest(fetchRequest: categoryRequest)
        categoryBatchDelete.resultType = .resultTypeCount

        try await context.perform {
            _ = try context.execute(categoryBatchDelete)
            try context.save()
        }

        // 3. Batch delete all photos from CoreData
        let photoRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "PhotoEntity")
        let photoBatchDelete = NSBatchDeleteRequest(fetchRequest: photoRequest)
        photoBatchDelete.resultType = .resultTypeCount

        try await context.perform {
            _ = try context.execute(photoBatchDelete)
            try context.save()
        }

        // 4. Clear keychain data
        try? keychainManager.delete(for: "pin")
        try? keychainManager.delete(for: "biometric_enabled")
    }

    /// Complete clear including settings reset
    /// Should only be called after the Settings view has been dismissed
    func clearAllData() async throws {
        // First clear the data
        try await clearDataOnly()

        // Then reset settings (this will trigger @AppStorage updates)
        // This should only happen after the Settings view is gone
        settingsManager.resetToDefaults()
    }

    // MARK: - Utilities

    private func getDocumentsDirectory() -> URL {
        fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    private func calculateMD5(for fileURL: URL) throws -> String {
        let data = try Data(contentsOf: fileURL)
        let digest = Insecure.MD5.hash(data: data)
        return digest.map { String(format: "%02hhx", $0) }.joined()
    }
}

// MARK: - CryptoKit Import

import CryptoKit

// MARK: - Date Formatter Extensions

extension DateFormatter {
    static let backupFilename: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd_HHmmss"
        formatter.timeZone = TimeZone.current
        return formatter
    }()
}
