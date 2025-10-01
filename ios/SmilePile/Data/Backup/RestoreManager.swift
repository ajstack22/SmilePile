import Foundation
import CryptoKit

class RestoreManager {
    static let shared = RestoreManager()

    private let photoRepository: PhotoRepository
    private let categoryRepository: CategoryRepository
    private let settingsManager: SettingsManager
    private let keychainManager: KeychainManager
    private let fileManager = FileManager.default

    // Version support
    private let MIN_SUPPORTED_VERSION = 1
    private let MAX_SUPPORTED_VERSION = CURRENT_BACKUP_VERSION

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

    // MARK: - Validation

    func validateBackup(
        at zipPath: URL,
        checkIntegrity: Bool = true
    ) async throws -> BackupValidationResult {
        let tempDir = createTempValidationDirectory()
        defer { cleanupTempDirectory(tempDir) }

        guard let backupData = try await extractAndParseBackup(zipPath: zipPath, tempDir: tempDir) else {
            return createFailureResult(error: "Failed to extract or parse backup")
        }

        return try await performValidation(
            backupData: backupData,
            tempDir: tempDir,
            checkIntegrity: checkIntegrity
        )
    }

    private func createTempValidationDirectory() -> URL {
        return fileManager.temporaryDirectory.appendingPathComponent("validate_temp_\(UUID().uuidString)")
    }

    private func cleanupTempDirectory(_ directory: URL) {
        try? fileManager.removeItem(at: directory)
    }

    private func extractAndParseBackup(zipPath: URL, tempDir: URL) async throws -> AppBackup? {
        do {
            try await ZipUtils.extractZip(from: zipPath, to: tempDir)
        } catch {
            return nil
        }

        let metadataPath = tempDir.appendingPathComponent(ZipUtils.METADATA_FILE)
        guard fileManager.fileExists(atPath: metadataPath.path) else {
            return nil
        }

        return try? parseBackupMetadata(from: metadataPath)
    }

    private func parseBackupMetadata(from path: URL) throws -> AppBackup {
        let metadataData = try Data(contentsOf: path)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .millisecondsSince1970
        return try decoder.decode(AppBackup.self, from: metadataData)
    }

    private func performValidation(
        backupData: AppBackup,
        tempDir: URL,
        checkIntegrity: Bool
    ) async throws -> BackupValidationResult {
        var errors: [String] = []
        var warnings: [String] = []

        validateVersion(backupData: backupData, errors: &errors)

        let photosDir = tempDir.appendingPathComponent("photos")
        let hasPhotos = fileManager.fileExists(atPath: photosDir.path)

        let integrityPassed = try await checkPhotoIntegrity(
            backupData: backupData,
            photosDir: photosDir,
            shouldCheck: checkIntegrity,
            warnings: &warnings
        )

        return createValidationResult(
            backupData: backupData,
            hasPhotos: hasPhotos,
            integrityPassed: integrityPassed,
            errors: errors,
            warnings: warnings
        )
    }

    private func validateVersion(backupData: AppBackup, errors: inout [String]) {
        if backupData.version < MIN_SUPPORTED_VERSION || backupData.version > MAX_SUPPORTED_VERSION {
            errors.append("Unsupported backup version: \(backupData.version)")
        }
    }

    private func checkPhotoIntegrity(
        backupData: AppBackup,
        photosDir: URL,
        shouldCheck: Bool,
        warnings: inout [String]
    ) async throws -> Bool {
        guard shouldCheck && !backupData.photoManifest.isEmpty else {
            return true
        }

        var integrityPassed = true
        for manifestEntry in backupData.photoManifest {
            let photoFile = photosDir.appendingPathComponent(manifestEntry.fileName)

            if fileManager.fileExists(atPath: photoFile.path) {
                if let expectedChecksum = manifestEntry.checksum {
                    let actualChecksum = try calculateMD5(for: photoFile)
                    if actualChecksum != expectedChecksum {
                        warnings.append("Checksum mismatch for \(manifestEntry.fileName)")
                        integrityPassed = false
                    }
                }
            } else {
                warnings.append("Missing photo file: \(manifestEntry.fileName)")
            }
        }

        return integrityPassed
    }

    private func createFailureResult(error: String) -> BackupValidationResult {
        return BackupValidationResult(isValid: false, errors: [error])
    }

    private func createValidationResult(
        backupData: AppBackup,
        hasPhotos: Bool,
        integrityPassed: Bool,
        errors: [String],
        warnings: [String]
    ) -> BackupValidationResult {
        return BackupValidationResult(
            isValid: errors.isEmpty,
            version: backupData.version,
            format: .zip,
            hasMetadata: true,
            hasPhotos: hasPhotos,
            photosCount: backupData.photos.count,
            categoriesCount: backupData.categories.count,
            integrityCheckPassed: integrityPassed,
            errors: errors,
            warnings: warnings
        )
    }

    // MARK: - Restore

    func restoreBackup(
        from zipPath: URL,
        options: RestoreOptions = RestoreOptions(),
        progressCallback: ((ImportProgress) -> Void)? = nil
    ) async throws -> ImportResult {
        // Create temp extraction directory
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent("restore_temp_\(UUID().uuidString)")

        defer {
            try? fileManager.removeItem(at: tempDir)
        }

        // Step 1: Extract ZIP
        progressCallback?(ImportProgress(
            totalItems: 100,
            processedItems: 10,
            currentOperation: "Extracting backup...",
            errors: []
        ))

        try await ZipUtils.extractZip(from: zipPath, to: tempDir)

        // Step 2: Parse metadata
        progressCallback?(ImportProgress(
            totalItems: 100,
            processedItems: 20,
            currentOperation: "Reading metadata...",
            errors: []
        ))

        let metadataPath = tempDir.appendingPathComponent(ZipUtils.METADATA_FILE)
        let metadataData = try Data(contentsOf: metadataPath)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .millisecondsSince1970
        let backupData = try decoder.decode(AppBackup.self, from: metadataData)

        var categoriesImported = 0
        var photosImported = 0
        var photosSkipped = 0
        var photoFilesRestored = 0
        var errors: [String] = []

        // Step 3: Restore categories
        progressCallback?(ImportProgress(
            totalItems: 100,
            processedItems: 30,
            currentOperation: "Restoring categories...",
            errors: []
        ))

        if options.strategy == .replace {
            // Clear existing categories (except defaults if needed)
            let existingCategories = try await categoryRepository.getAllCategories()
            for category in existingCategories {
                if !category.isDefault {
                    try await categoryRepository.deleteCategory(category)
                }
            }
        }

        for backupCategory in backupData.categories {
            do {
                let category = backupCategory.toCategory()

                if options.strategy == .merge {
                    // Check if category exists
                    if let existing = try await categoryRepository.getCategoryById(category.id) {
                        // Update existing
                        try await categoryRepository.updateCategory(category)
                    } else {
                        // Insert new
                        _ = try await categoryRepository.insertCategory(category)
                    }
                } else {
                    // Replace mode
                    _ = try await categoryRepository.insertCategory(category)
                }

                categoriesImported += 1
            } catch {
                errors.append("Failed to restore category \(backupCategory.displayName): \(error.localizedDescription)")
            }
        }

        // Step 4: Restore photos
        progressCallback?(ImportProgress(
            totalItems: 100,
            processedItems: 40,
            currentOperation: "Restoring photos...",
            errors: []
        ))

        let photosDir = tempDir.appendingPathComponent("photos")
        let documentsDir = getDocumentsDirectory()
        let totalPhotos = backupData.photos.count
        var processedPhotos = 0

        for backupPhoto in backupData.photos {
            // Find corresponding manifest entry
            let manifestEntry = backupData.photoManifest.first { $0.photoId == backupPhoto.id }

            do {
                // Copy photo file
                if let manifest = manifestEntry {
                    let sourceFile = photosDir.appendingPathComponent(manifest.fileName)

                    if fileManager.fileExists(atPath: sourceFile.path) {
                        // Determine destination path
                        let destPath = documentsDir.appendingPathComponent(backupPhoto.path)

                        // Create parent directory if needed
                        let parentDir = destPath.deletingLastPathComponent()
                        try fileManager.createDirectory(at: parentDir, withIntermediateDirectories: true, attributes: nil)

                        // Handle duplicates based on resolution strategy
                        if fileManager.fileExists(atPath: destPath.path) {
                            switch options.duplicateResolution {
                            case .skip:
                                photosSkipped += 1
                                continue
                            case .replace:
                                try fileManager.removeItem(at: destPath)
                            case .rename:
                                // Already handled by unique photo ID in path
                                break
                            case .askUser:
                                // Not implemented in automated restore
                                photosSkipped += 1
                                continue
                            }
                        }

                        // Copy file
                        try fileManager.copyItem(at: sourceFile, to: destPath)
                        photoFilesRestored += 1
                    }
                }

                // Insert photo record
                let photo = backupPhoto.toPhoto()

                if options.strategy == .merge {
                    if let existing = try await photoRepository.getPhotoById(photo.id) {
                        try await photoRepository.updatePhoto(photo)
                    } else {
                        _ = try await photoRepository.insertPhoto(photo)
                    }
                } else {
                    _ = try await photoRepository.insertPhoto(photo)
                }

                photosImported += 1
                processedPhotos += 1

                let progress = 40 + Int((Double(processedPhotos) / Double(totalPhotos)) * 50)
                progressCallback?(ImportProgress(
                    totalItems: 100,
                    processedItems: progress,
                    currentOperation: "Restoring photos (\(processedPhotos)/\(totalPhotos))...",
                    errors: errors
                ))
            } catch {
                errors.append("Failed to restore photo \(backupPhoto.name): \(error.localizedDescription)")
                photosSkipped += 1
            }
        }

        // Step 5: Restore settings
        if options.restoreSettings {
            progressCallback?(ImportProgress(
                totalItems: 100,
                processedItems: 95,
                currentOperation: "Restoring settings...",
                errors: errors
            ))

            restoreSettings(backupData.settings)
        }

        // Step 6: Done
        progressCallback?(ImportProgress(
            totalItems: 100,
            processedItems: 100,
            currentOperation: "Restore complete",
            errors: errors
        ))

        return ImportResult(
            success: errors.isEmpty,
            categoriesImported: categoriesImported,
            photosImported: photosImported,
            photosSkipped: photosSkipped,
            photoFilesRestored: photoFilesRestored,
            errors: errors
        )
    }

    // MARK: - Settings Restore

    private func restoreSettings(_ settings: BackupSettings) {
        // Restore theme
        if settings.isDarkMode {
            settingsManager.themeMode = .dark
        } else {
            settingsManager.themeMode = .light
        }

        // Restore security settings
        settingsManager.kidsModeEnabled = settings.securitySettings.kidSafeModeEnabled

        // Note: PINs are not restored for security reasons
        // User must set up PIN again if needed
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
