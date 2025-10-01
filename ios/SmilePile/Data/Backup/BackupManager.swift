import Foundation

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

        try fileManager.createDirectory(
            at: workingDir,
            withIntermediateDirectories: true,
            attributes: nil
        )

        return workingDir
    }

    func cleanupBackupWorkingDirectory(_ directory: URL) {
        try? fileManager.removeItem(at: directory)
    }

    // MARK: - Data Collection

    func collectPhotos() async throws -> [Photo] {
        return try await photoRepository.getAllPhotos()
    }

    func collectCategories() async throws -> [Category] {
        return try await categoryRepository.getAllCategories()
    }

    func collectSettings() -> BackupSettings {
        // Check if PIN exists using PINManager
        let hasPIN = PINManager.shared.isPINEnabled()

        let securitySettings = BackupSecuritySettings(
            hasPIN: hasPIN,
            hasPattern: false, // iOS doesn't support pattern lock
            kidSafeModeEnabled: settingsManager.kidsModeEnabled,
            deleteProtectionEnabled: false // Not implemented yet
        )

        let isDarkMode = settingsManager.themeMode == .dark

        return BackupSettings(
            isDarkMode: isDarkMode,
            securitySettings: securitySettings
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

        // Step 1: Collect data
        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 10,
            currentOperation: "Collecting categories...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let categories = try await collectCategories()

        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 20,
            currentOperation: "Collecting photos...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let photos = try await collectPhotos()

        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 30,
            currentOperation: "Collecting settings...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let settings = collectSettings()

        // Step 2: Copy photos to working directory
        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 40,
            currentOperation: "Copying photos...",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        let manifest = try await copyPhotosToBackupDirectory(photos, to: workingDir) { current, total in
            let progress = 40 + Int((Double(current) / Double(total)) * 40)
            progressCallback?(ExportProgress(
                totalItems: 100,
                processedItems: progress,
                currentOperation: "Copying photos (\(current)/\(total))...",
                currentFile: nil,
                bytesProcessed: 0,
                totalBytes: 0,
                errors: []
            ))
        }

        // Step 3: Create metadata.json
        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 80,
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
            totalItems: 100,
            processedItems: 90,
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
            let totalProgress = 90 + Int(progress * 10)
            progressCallback?(ExportProgress(
                totalItems: 100,
                processedItems: totalProgress,
                currentOperation: "Compressing...",
                currentFile: nil,
                bytesProcessed: 0,
                totalBytes: 0,
                errors: []
            ))
        }

        // Step 5: Done
        progressCallback?(ExportProgress(
            totalItems: 100,
            processedItems: 100,
            currentOperation: "Backup complete",
            currentFile: nil,
            bytesProcessed: 0,
            totalBytes: 0,
            errors: []
        ))

        return zipPath
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
