import Foundation
import CoreData
import UIKit
import os.log
import Combine

// MARK: - Restore Manager
@MainActor
final class RestoreManager: ObservableObject {

    // MARK: - Singleton
    static let shared = RestoreManager()

    // MARK: - Published Properties
    @Published var isImporting = false
    @Published var importProgress: ImportProgress?
    @Published var validationResult: BackupValidationResult?

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "RestoreManager")
    private let fileManager = FileManager.default
    private let jsonDecoder = JSONDecoder()

    // MARK: - Dependencies
    private let categoryRepository: CategoryRepository
    private let photoRepository: PhotoRepository
    private let storageManager: StorageManager
    private let settingsManager: SettingsManager

    // MARK: - Constants
    private let tempDirectory: URL

    // MARK: - Initialization
    private init() {
        self.categoryRepository = CategoryRepository.shared
        self.photoRepository = PhotoRepository.shared
        self.storageManager = StorageManager.shared
        self.settingsManager = SettingsManager.shared

        // Setup temp directory
        self.tempDirectory = fileManager.temporaryDirectory.appendingPathComponent("RestoreTemp", isDirectory: true)
        try? fileManager.createDirectory(at: tempDirectory, withIntermediateDirectories: true, attributes: nil)
    }

    // MARK: - Import from ZIP

    func importFromZip(
        zipFile: URL,
        strategy: ImportStrategy = .merge,
        progressCallback: ((Int, Int, String) -> Void)? = nil
    ) async throws -> ImportResult {

        logger.info("Starting ZIP import with strategy: \(String(describing: strategy))")
        isImporting = true
        defer { isImporting = false }

        var errors: [String] = []
        var warnings: [String] = []
        var categoriesImported = 0
        var photosImported = 0
        var photosSkipped = 0
        var photoFilesRestored = 0

        // Clean temp directory
        cleanTempDirectory()

        progressCallback?(0, 100, "Validating ZIP structure")
        updateProgress(1, 0, "Validating ZIP structure", errors)

        // Validate ZIP structure
        let validationResult = await ZipUtils.validateZipStructure(zipFile)
        switch validationResult {
        case .failure(let error):
            throw BackupError.invalidFormat
        case .success(let result):
            if !result.isValid {
                throw BackupError.invalidFormat
            }
            self.validationResult = result
        }

        progressCallback?(10, 100, "Extracting ZIP archive")
        updateProgress(1, 0, "Extracting ZIP archive", errors)

        // Extract ZIP to temp directory
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let workDir = tempDirectory.appendingPathComponent("import_\(timestamp)", isDirectory: true)
        try fileManager.createDirectory(at: workDir, withIntermediateDirectories: true, attributes: nil)

        try await ZipUtils.extractZip(
            zipFile: zipFile,
            destinationDir: workDir
        ) { current, total in
            let progress = 10 + (current * 20 / total)
            progressCallback?(progress, 100, "Extracting files (\(current)/\(total))")
        }

        progressCallback?(30, 100, "Reading backup metadata")
        updateProgress(1, 0, "Reading backup metadata", errors)

        // Read metadata.json
        let metadataFile = workDir.appendingPathComponent(ZipUtils.METADATA_FILE)
        guard fileManager.fileExists(atPath: metadataFile.path) else {
            throw BackupError.fileNotFound("metadata.json not found in ZIP")
        }

        let metadataData = try Data(contentsOf: metadataFile)
        let appBackup = try jsonDecoder.decode(AppBackup.self, from: metadataData)

        // Validate backup version
        guard appBackup.version >= MIN_SUPPORTED_VERSION && appBackup.version <= MAX_SUPPORTED_VERSION else {
            throw BackupError.unsupportedVersion(appBackup.version)
        }

        let totalItems = appBackup.categories.count + appBackup.photos.count
        var processedItems = 0

        progressCallback?(40, 100, "Starting import")
        updateProgress(totalItems, processedItems, "Starting import", errors)

        // Handle strategy
        if strategy == .replace {
            progressCallback?(45, 100, "Clearing existing data")
            updateProgress(totalItems, processedItems, "Clearing existing data", errors)
            try await clearAllData()
        }

        // Import categories first
        progressCallback?(50, 100, "Importing categories")
        updateProgress(totalItems, processedItems, "Importing categories", errors)

        for (index, backupCategory) in appBackup.categories.enumerated() {
            do {
                let existingCategory = try await categoryRepository.getCategoryByName(backupCategory.name)

                if strategy == .merge && existingCategory != nil {
                    // Update existing category
                    var updatedCategory = backupCategory.toCategory()
                    updatedCategory.id = existingCategory!.id
                    try await categoryRepository.updateCategory(updatedCategory)
                    warnings.append("Updated existing category: \(backupCategory.displayName)")
                } else {
                    // Insert new category
                    var categoryToInsert = backupCategory.toCategory()
                    if strategy == .merge {
                        categoryToInsert.id = 0 // Let CoreData auto-generate ID
                    }
                    try await categoryRepository.insertCategory(categoryToInsert)
                    categoriesImported += 1
                }
            } catch {
                errors.append("Failed to import category '\(backupCategory.displayName)': \(error.localizedDescription)")
                logger.error("Error importing category: \(backupCategory.displayName) - \(error.localizedDescription)")
            }

            processedItems += 1
            let progress = 50 + ((index + 1) * 20 / appBackup.categories.count)
            progressCallback?(progress, 100, "Importing categories (\(index + 1)/\(appBackup.categories.count))")
            updateProgress(totalItems, processedItems, "Importing categories", errors)
        }

        // Import photos and restore photo files
        progressCallback?(70, 100, "Importing photos")
        updateProgress(totalItems, processedItems, "Importing photos", errors)

        let photosDir = workDir.appendingPathComponent("photos", isDirectory: true)
        let internalPhotosDir = storageManager.photosDirectory

        for (index, backupPhoto) in appBackup.photos.enumerated() {
            autoreleasepool {
                do {
                    // Find corresponding file in extracted photos
                    let manifestEntry = appBackup.photoManifest.first { $0.photoId == backupPhoto.id }
                    var newPhotoPath = backupPhoto.path

                    if let manifestEntry = manifestEntry {
                        // Restore photo file from ZIP
                        let sourceFile = photosDir.appendingPathComponent(manifestEntry.fileName)
                        if fileManager.fileExists(atPath: sourceFile.path) {
                            // Verify checksum if available
                            if let expectedChecksum = manifestEntry.checksum {
                                let actualChecksum = try ZipUtils.calculateMD5(sourceFile)
                                if actualChecksum != expectedChecksum {
                                    warnings.append("Checksum mismatch for photo: \(manifestEntry.fileName)")
                                }
                            }

                            let destFile = internalPhotosDir.appendingPathComponent(manifestEntry.fileName)
                            try fileManager.copyItem(at: sourceFile, to: destFile)
                            newPhotoPath = destFile.path
                            photoFilesRestored += 1
                        } else {
                            warnings.append("Photo file not found in ZIP: \(manifestEntry.fileName)")
                        }
                    }

                    // Check for duplicates in merge mode
                    if strategy == .merge {
                        let existingPhotos = try await photoRepository.getAllPhotos()
                        let isDuplicate = existingPhotos.contains { $0.path == newPhotoPath }

                        if isDuplicate {
                            photosSkipped += 1
                            warnings.append("Skipped duplicate photo: \(backupPhoto.name)")
                            processedItems += 1
                            return
                        }
                    }

                    // Get the actual category ID for merge mode
                    let actualCategoryId: Int64
                    if strategy == .replace {
                        actualCategoryId = backupPhoto.categoryId
                    } else {
                        let categoryBackupForPhoto = appBackup.categories.first { $0.id == backupPhoto.categoryId }
                        if let categoryBackupForPhoto = categoryBackupForPhoto,
                           let existingCategory = try await categoryRepository.getCategoryByName(categoryBackupForPhoto.name) {
                            actualCategoryId = existingCategory.id
                        } else {
                            actualCategoryId = backupPhoto.categoryId
                        }
                    }

                    // Insert photo with updated path
                    var photoToInsert = backupPhoto.toPhoto()
                    photoToInsert.path = newPhotoPath
                    if strategy == .merge {
                        photoToInsert.id = 0 // Let CoreData auto-generate ID
                    }
                    photoToInsert.categoryId = actualCategoryId

                    try await photoRepository.insertPhoto(photoToInsert)
                    photosImported += 1

                } catch {
                    errors.append("Failed to import photo '\(backupPhoto.name)': \(error.localizedDescription)")
                    logger.error("Error importing photo: \(backupPhoto.name) - \(error.localizedDescription)")
                }

                processedItems += 1
                let progress = 70 + ((index + 1) * 25 / appBackup.photos.count)
                progressCallback?(progress, 100, "Importing photos (\(index + 1)/\(appBackup.photos.count))")
                updateProgress(totalItems, processedItems, "Importing photos", errors)
            }
        }

        // Restore settings if available
        if appBackup.settings.isDarkMode != await settingsManager.isDarkMode {
            await settingsManager.setDarkMode(appBackup.settings.isDarkMode)
        }

        // Clean up temp directory
        try? fileManager.removeItem(at: workDir)

        progressCallback?(100, 100, "Import completed")
        updateProgress(totalItems, processedItems, "Import completed", errors)

        logger.info("ZIP import completed: \(categoriesImported) categories, \(photosImported) photos, \(photoFilesRestored) files restored")

        return ImportResult(
            success: errors.isEmpty,
            categoriesImported: categoriesImported,
            photosImported: photosImported,
            photosSkipped: photosSkipped,
            photoFilesRestored: photoFilesRestored,
            errors: errors,
            warnings: warnings
        )
    }

    // MARK: - Import from JSON

    func importFromJson(
        jsonString: String,
        strategy: ImportStrategy = .merge
    ) async throws -> ImportResult {

        logger.info("Starting JSON import with strategy: \(String(describing: strategy))")
        isImporting = true
        defer { isImporting = false }

        var errors: [String] = []
        var warnings: [String] = []
        var categoriesImported = 0
        var photosImported = 0
        var photosSkipped = 0

        updateProgress(1, 0, "Parsing JSON data", errors)

        // Parse JSON
        guard let jsonData = jsonString.data(using: .utf8) else {
            throw BackupError.invalidFormat
        }

        let appBackup = try jsonDecoder.decode(AppBackup.self, from: jsonData)

        // Validate backup version
        guard appBackup.version >= MIN_SUPPORTED_VERSION && appBackup.version <= MAX_SUPPORTED_VERSION else {
            throw BackupError.unsupportedVersion(appBackup.version)
        }

        let totalItems = appBackup.categories.count + appBackup.photos.count
        var processedItems = 0

        updateProgress(totalItems, processedItems, "Starting import", errors)

        // Handle strategy
        if strategy == .replace {
            updateProgress(totalItems, processedItems, "Clearing existing data", errors)
            try await clearAllData()
        }

        // Import categories
        updateProgress(totalItems, processedItems, "Importing categories", errors)

        for backupCategory in appBackup.categories {
            do {
                let existingCategory = try await categoryRepository.getCategoryByName(backupCategory.name)

                if strategy == .merge && existingCategory != nil {
                    // Update existing category
                    var updatedCategory = backupCategory.toCategory()
                    updatedCategory.id = existingCategory!.id
                    try await categoryRepository.updateCategory(updatedCategory)
                    warnings.append("Updated existing category: \(backupCategory.displayName)")
                } else {
                    // Insert new category
                    var categoryToInsert = backupCategory.toCategory()
                    if strategy == .merge {
                        categoryToInsert.id = 0 // Let CoreData auto-generate ID
                    }
                    try await categoryRepository.insertCategory(categoryToInsert)
                    categoriesImported += 1
                }
            } catch {
                errors.append("Failed to import category '\(backupCategory.displayName)': \(error.localizedDescription)")
            }

            processedItems += 1
            updateProgress(totalItems, processedItems, "Importing categories", errors)
        }

        // Import photos (metadata only for JSON)
        updateProgress(totalItems, processedItems, "Importing photos", errors)

        for backupPhoto in appBackup.photos {
            do {
                // Validate photo file exists
                if !backupPhoto.isFromAssets {
                    let photoURL = URL(fileURLWithPath: backupPhoto.path)
                    if !fileManager.fileExists(atPath: photoURL.path) {
                        photosSkipped += 1
                        warnings.append("Skipped missing photo: \(backupPhoto.name)")
                        processedItems += 1
                        continue
                    }
                }

                // Check for duplicates in merge mode
                if strategy == .merge {
                    let existingPhotos = try await photoRepository.getAllPhotos()
                    let isDuplicate = existingPhotos.contains { $0.path == backupPhoto.path }

                    if isDuplicate {
                        photosSkipped += 1
                        warnings.append("Skipped duplicate photo: \(backupPhoto.name)")
                        processedItems += 1
                        continue
                    }
                }

                // Get the actual category ID for merge mode
                let actualCategoryId: Int64
                if strategy == .replace {
                    actualCategoryId = backupPhoto.categoryId
                } else {
                    let categoryBackupForPhoto = appBackup.categories.first { $0.id == backupPhoto.categoryId }
                    if let categoryBackupForPhoto = categoryBackupForPhoto,
                       let existingCategory = try await categoryRepository.getCategoryByName(categoryBackupForPhoto.name) {
                        actualCategoryId = existingCategory.id
                    } else {
                        actualCategoryId = backupPhoto.categoryId
                    }
                }

                // Insert photo
                var photoToInsert = backupPhoto.toPhoto()
                if strategy == .merge {
                    photoToInsert.id = 0 // Let CoreData auto-generate ID
                }
                photoToInsert.categoryId = actualCategoryId

                try await photoRepository.insertPhoto(photoToInsert)
                photosImported += 1

            } catch {
                errors.append("Failed to import photo '\(backupPhoto.name)': \(error.localizedDescription)")
            }

            processedItems += 1
            updateProgress(totalItems, processedItems, "Importing photos", errors)
        }

        updateProgress(totalItems, processedItems, "Import completed", errors)

        logger.info("JSON import completed: \(categoriesImported) categories, \(photosImported) photos imported, \(photosSkipped) photos skipped")

        return ImportResult(
            success: errors.isEmpty,
            categoriesImported: categoriesImported,
            photosImported: photosImported,
            photosSkipped: photosSkipped,
            photoFilesRestored: 0,
            errors: errors,
            warnings: warnings
        )
    }

    // MARK: - Validate Backup File

    func validateBackupFile(_ fileURL: URL) async -> Result<BackupPreview, Error> {
        do {
            guard fileManager.fileExists(atPath: fileURL.path) else {
                return .failure(BackupError.fileNotFound(fileURL.path))
            }

            let fileName = fileURL.lastPathComponent.lowercased()

            // Check file extension
            if fileName.hasSuffix(".zip") || fileName.hasSuffix(".smilepile") {
                return await validateZipBackup(fileURL)
            } else if fileName.hasSuffix(".json") {
                return await validateJsonBackup(fileURL)
            } else {
                return .failure(BackupError.invalidFormat)
            }
        } catch {
            return .failure(error)
        }
    }

    // MARK: - Get Backup Preview

    func getBackupPreview(_ fileURL: URL) async -> Result<BackupPreview, Error> {
        let validationResult = await validateBackupFile(fileURL)

        switch validationResult {
        case .success(let preview):
            return .success(preview)
        case .failure(let error):
            return .failure(error)
        }
    }

    // MARK: - Private Methods

    private func validateZipBackup(_ zipFile: URL) async -> Result<BackupPreview, Error> {
        do {
            // Validate ZIP structure
            let structureResult = await ZipUtils.validateZipStructure(zipFile)
            guard case .success(let validation) = structureResult else {
                return .failure(BackupError.invalidFormat)
            }

            guard validation.isValid else {
                return .failure(BackupError.invalidFormat)
            }

            // Extract and read metadata
            let tempDir = tempDirectory.appendingPathComponent("validate_\(UUID().uuidString)", isDirectory: true)
            try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true, attributes: nil)
            defer { try? fileManager.removeItem(at: tempDir) }

            try await ZipUtils.extractZip(zipFile: zipFile, destinationDir: tempDir)

            let metadataFile = tempDir.appendingPathComponent(ZipUtils.METADATA_FILE)
            let metadataData = try Data(contentsOf: metadataFile)
            let appBackup = try jsonDecoder.decode(AppBackup.self, from: metadataData)

            // Check for missing photos
            var missingPhotos: [String] = []
            for photo in appBackup.photos {
                if !photo.isFromAssets {
                    let manifestEntry = appBackup.photoManifest.first { $0.photoId == photo.id }
                    if manifestEntry == nil {
                        missingPhotos.append("\(photo.name) (\(photo.path))")
                    }
                }
            }

            let preview = BackupPreview(
                version: appBackup.version,
                exportDate: appBackup.exportDate,
                appVersion: appBackup.appVersion,
                categoriesCount: appBackup.categories.count,
                photosCount: appBackup.photos.count,
                missingPhotosCount: missingPhotos.count,
                missingPhotos: missingPhotos,
                isZipFormat: true
            )

            return .success(preview)

        } catch {
            return .failure(error)
        }
    }

    private func validateJsonBackup(_ jsonFile: URL) async -> Result<BackupPreview, Error> {
        do {
            let jsonData = try Data(contentsOf: jsonFile)
            let appBackup = try jsonDecoder.decode(AppBackup.self, from: jsonData)

            // Check which photos are missing
            var missingPhotos: [String] = []
            for photo in appBackup.photos {
                if !photo.isFromAssets {
                    let photoURL = URL(fileURLWithPath: photo.path)
                    if !fileManager.fileExists(atPath: photoURL.path) {
                        missingPhotos.append("\(photo.name) (\(photo.path))")
                    }
                }
            }

            let preview = BackupPreview(
                version: appBackup.version,
                exportDate: appBackup.exportDate,
                appVersion: appBackup.appVersion,
                categoriesCount: appBackup.categories.count,
                photosCount: appBackup.photos.count,
                missingPhotosCount: missingPhotos.count,
                missingPhotos: missingPhotos,
                isZipFormat: false
            )

            return .success(preview)

        } catch {
            return .failure(error)
        }
    }

    private func clearAllData() async throws {
        // Delete all photos first (due to foreign key constraints)
        let allPhotos = try await photoRepository.getAllPhotos()
        for photo in allPhotos {
            try await photoRepository.deletePhoto(photo)
        }

        // Delete all non-default categories
        let allCategories = try await categoryRepository.getAllCategories()
        for category in allCategories {
            if !category.isDefault {
                try await categoryRepository.deleteCategory(category)
            }
        }

        logger.info("Cleared all existing data")
    }

    private func updateProgress(_ total: Int, _ processed: Int, _ operation: String, _ errors: [String]) {
        DispatchQueue.main.async {
            self.importProgress = ImportProgress(
                totalItems: total,
                processedItems: processed,
                currentOperation: operation,
                errors: errors
            )
        }
    }

    private func cleanTempDirectory() {
        do {
            let tempFiles = try fileManager.contentsOfDirectory(at: tempDirectory, includingPropertiesForKeys: nil)
            for tempFile in tempFiles {
                try? fileManager.removeItem(at: tempFile)
            }
        } catch {
            logger.warning("Failed to clean temp directory: \(error.localizedDescription)")
        }
    }

    // MARK: - Duplicate Resolution

    func resolveDuplicates(
        photos: [Photo],
        resolution: DuplicateResolution = .skip
    ) async throws -> [Photo] {

        var resolvedPhotos: [Photo] = []
        let existingPhotos = try await photoRepository.getAllPhotos()
        let existingPaths = Set(existingPhotos.map { $0.path })

        for photo in photos {
            if existingPaths.contains(photo.path) {
                switch resolution {
                case .skip:
                    continue
                case .replace:
                    // Delete existing photo and add new one
                    if let existing = existingPhotos.first(where: { $0.path == photo.path }) {
                        try await photoRepository.deletePhoto(existing)
                    }
                    resolvedPhotos.append(photo)
                case .rename:
                    // Generate new name for the photo
                    var newPhoto = photo
                    let url = URL(fileURLWithPath: photo.path)
                    let name = url.deletingPathExtension().lastPathComponent
                    let ext = url.pathExtension
                    let timestamp = Int(Date().timeIntervalSince1970)
                    newPhoto.name = "\(name)_\(timestamp)"
                    newPhoto.path = url.deletingLastPathComponent()
                        .appendingPathComponent("\(name)_\(timestamp).\(ext)")
                        .path
                    resolvedPhotos.append(newPhoto)
                case .askUser:
                    // This would require UI interaction
                    // For now, default to skip
                    continue
                }
            } else {
                resolvedPhotos.append(photo)
            }
        }

        return resolvedPhotos
    }
}

// MARK: - Duplicate Resolution
enum DuplicateResolution {
    case skip           // Skip duplicate photos
    case replace        // Replace with imported version
    case rename         // Rename imported photo
    case askUser        // Ask user for each duplicate
}