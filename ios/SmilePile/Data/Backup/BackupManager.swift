import Foundation
import CoreData
import UIKit
import CryptoKit
import os.log
import Combine

// MARK: - Backup Manager
@MainActor
final class BackupManager: ObservableObject {

    // MARK: - Singleton
    static let shared = BackupManager()

    // MARK: - Published Properties
    @Published var isExporting = false
    @Published var exportProgress: ExportProgress?
    @Published var lastBackupDate: Date?
    @Published var backupHistory: [BackupHistoryEntry] = []

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "BackupManager")
    private let fileManager = FileManager.default
    private let jsonEncoder: JSONEncoder
    private let jsonDecoder: JSONDecoder
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Dependencies
    private let categoryRepository: CategoryRepository
    private let photoRepository: PhotoRepository
    private let settingsManager: SettingsManager
    private let storageManager: StorageManager

    // MARK: - Constants
    private let backupDirectory: URL
    private let tempDirectory: URL

    // MARK: - Initialization
    private init() {
        self.categoryRepository = CategoryRepository.shared
        self.photoRepository = PhotoRepository.shared
        self.settingsManager = SettingsManager.shared
        self.storageManager = StorageManager.shared

        // Configure JSON encoder/decoder
        self.jsonEncoder = JSONEncoder()
        self.jsonEncoder.outputFormatting = [.prettyPrinted, .sortedKeys]

        self.jsonDecoder = JSONDecoder()

        // Setup directories
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.backupDirectory = documentsDirectory.appendingPathComponent("Backups", isDirectory: true)
        self.tempDirectory = fileManager.temporaryDirectory.appendingPathComponent("BackupTemp", isDirectory: true)

        // Create directories if needed
        try? fileManager.createDirectory(at: backupDirectory, withIntermediateDirectories: true, attributes: nil)
        try? fileManager.createDirectory(at: tempDirectory, withIntermediateDirectories: true, attributes: nil)

        // Load backup history
        Task { await loadBackupHistory() }
    }

    // MARK: - Export to ZIP

    func exportToZip(
        options: BackupOptions = .default,
        progressCallback: ((Int, Int, String) -> Void)? = nil
    ) async throws -> URL {

        logger.info("Starting ZIP export with options")
        isExporting = true
        defer { isExporting = false }

        // Clean temp directory
        cleanTempDirectory()

        // Create working directory
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let workDir = tempDirectory.appendingPathComponent("backup_\(timestamp)", isDirectory: true)
        try fileManager.createDirectory(at: workDir, withIntermediateDirectories: true, attributes: nil)

        progressCallback?(0, 100, "Gathering app data")
        updateProgress(0, 100, "Gathering app data")

        // Gather data based on options
        let categories = try await gatherCategories(options: options)
        let photos = try await gatherPhotos(options: options, categories: categories)

        progressCallback?(20, 100, "Preparing metadata")
        updateProgress(20, 100, "Preparing metadata")

        // Create photo manifest and copy photos
        let photoManifest = try await processPhotos(
            photos: photos,
            workDir: workDir,
            options: options,
            progressCallback: progressCallback
        )

        progressCallback?(70, 100, "Creating backup metadata")
        updateProgress(70, 100, "Creating backup metadata")

        // Create backup data structure
        let appBackup = try await createAppBackup(
            categories: categories,
            photos: photos,
            photoManifest: photoManifest,
            options: options
        )

        // Write metadata.json
        let metadataFile = workDir.appendingPathComponent(ZipUtils.METADATA_FILE)
        let jsonData = try jsonEncoder.encode(appBackup)
        try jsonData.write(to: metadataFile)

        // Write categories.json for easier access
        let categoriesFile = workDir.appendingPathComponent("categories.json")
        let categoriesData = try jsonEncoder.encode(categories.map { BackupCategory.fromCategory($0) })
        try categoriesData.write(to: categoriesFile)

        progressCallback?(80, 100, "Creating ZIP archive")
        updateProgress(80, 100, "Creating ZIP archive")

        // Create ZIP file
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let dateString = dateFormatter.string(from: Date())
        let zipFileName = "SmilePile_Backup_\(dateString).zip"
        let zipFile = backupDirectory.appendingPathComponent(zipFileName)

        // Create ZIP using ZipUtils
        try await ZipUtils.createZipFromDirectory(
            sourceDir: workDir,
            outputFile: zipFile,
            compressionLevel: options.compressionLevel
        ) { current, total in
            let progress = 80 + (current * 20 / total)
            progressCallback?(progress, 100, "Archiving files (\(current)/\(total))")
            self.updateProgress(progress, 100, "Archiving files")
        }

        // Clean up temp directory
        try? fileManager.removeItem(at: workDir)

        progressCallback?(100, 100, "Export completed")
        updateProgress(100, 100, "Export completed")

        // Save to backup history
        await saveBackupHistory(
            fileName: zipFileName,
            filePath: zipFile.path,
            fileSize: try fileManager.attributesOfItem(atPath: zipFile.path)[.size] as? Int64 ?? 0,
            format: .zip,
            photosCount: photos.count,
            categoriesCount: categories.count,
            compressionLevel: options.compressionLevel
        )

        logger.info("ZIP export completed: \(zipFile.path)")
        lastBackupDate = Date()

        return zipFile
    }

    // MARK: - Export to JSON

    func exportToJson() async throws -> String {
        logger.info("Starting JSON export")

        // Gather all data
        let categories = try await categoryRepository.getAllCategories()
        let photos = try await photoRepository.getAllPhotos()

        // Create backup structure
        let appBackup = try await createAppBackup(
            categories: categories,
            photos: photos,
            photoManifest: [],
            options: BackupOptions(
                includePhotos: false, // JSON doesn't include actual photo files
                includeSettings: true,
                includeCategories: true,
                compressionLevel: .none,
                dateRange: nil,
                categoryFilter: nil
            )
        )

        // Convert to JSON
        let jsonData = try jsonEncoder.encode(appBackup)
        let jsonString = String(data: jsonData, encoding: .utf8)!

        logger.info("JSON export completed")
        return jsonString
    }

    // MARK: - Get Backup Statistics

    func getBackupStats() async -> BackupStats {
        do {
            let categories = try await categoryRepository.getAllCategories()
            let photos = try await photoRepository.getAllPhotos()

            var estimatedSize: Int64 = 0

            // Estimate photo sizes
            for photo in photos {
                if !photo.isFromAssets {
                    let photoURL = URL(fileURLWithPath: photo.path)
                    if fileManager.fileExists(atPath: photoURL.path) {
                        let attributes = try? fileManager.attributesOfItem(atPath: photoURL.path)
                        estimatedSize += attributes?[.size] as? Int64 ?? 0
                    }
                }
            }

            // Add estimated metadata size (roughly 1KB per item)
            estimatedSize += Int64((categories.count + photos.count) * 1024)

            return BackupStats(
                categoryCount: categories.count,
                photoCount: photos.count,
                estimatedSize: estimatedSize,
                success: true,
                errorMessage: nil
            )
        } catch {
            return BackupStats(
                categoryCount: 0,
                photoCount: 0,
                estimatedSize: 0,
                success: false,
                errorMessage: error.localizedDescription
            )
        }
    }

    // MARK: - Incremental Backup

    func performIncrementalBackup(
        baseBackupId: String,
        options: BackupOptions = .default,
        progressCallback: ((Int, Int, String) -> Void)? = nil
    ) async throws -> URL {

        guard let baseBackup = backupHistory.first(where: { $0.id == baseBackupId }) else {
            throw BackupError.fileNotFound("Base backup not found")
        }

        progressCallback?(0, 100, "Analyzing changes since last backup")

        // Get changes since last backup
        let baseDate = Date(timeIntervalSince1970: TimeInterval(baseBackup.timestamp / 1000))
        let changedPhotos = try await photoRepository.getPhotosModifiedAfter(baseDate)
        let changedCategories = try await categoryRepository.getCategoriesModifiedAfter(baseDate)

        if changedPhotos.isEmpty && changedCategories.isEmpty {
            throw BackupError.exportFailed("No changes since last backup")
        }

        progressCallback?(20, 100, "Creating incremental backup")

        // Create incremental options
        var incrementalOptions = options
        incrementalOptions.categoryFilter = changedCategories.map { $0.name }
        incrementalOptions.dateRange = BackupOptions.DateRange(
            start: baseDate,
            end: Date()
        )

        // Perform backup with incremental options
        return try await exportToZip(options: incrementalOptions, progressCallback: progressCallback)
    }

    // MARK: - Private Methods

    private func gatherCategories(options: BackupOptions) async throws -> [Category] {
        let allCategories = try await categoryRepository.getAllCategories()

        if let filter = options.categoryFilter {
            return allCategories.filter { filter.contains($0.name) }
        }

        return allCategories
    }

    private func gatherPhotos(options: BackupOptions, categories: [Category]) async throws -> [Photo] {
        let allPhotos = try await photoRepository.getAllPhotos()
        let categoryIds = Set(categories.map { $0.id })

        return allPhotos.filter { photo in
            // Filter by category
            guard categoryIds.contains(photo.categoryId) else { return false }

            // Filter by date range if specified
            if let dateRange = options.dateRange {
                let photoDate = photo.createdAt
                if photoDate < dateRange.start || photoDate > dateRange.end {
                    return false
                }
            }

            return true
        }
    }

    private func processPhotos(
        photos: [Photo],
        workDir: URL,
        options: BackupOptions,
        progressCallback: ((Int, Int, String) -> Void)?
    ) async throws -> [PhotoManifestEntry] {

        guard options.includePhotos else { return [] }

        var photoManifest: [PhotoManifestEntry] = []

        // Create photos directory
        let photosDir = workDir.appendingPathComponent("photos", isDirectory: true)
        try fileManager.createDirectory(at: photosDir, withIntermediateDirectories: true, attributes: nil)

        // Create thumbnails directory if needed
        let thumbnailsDir = workDir.appendingPathComponent("thumbnails", isDirectory: true)
        if options.includeSettings {
            try fileManager.createDirectory(at: thumbnailsDir, withIntermediateDirectories: true, attributes: nil)
        }

        var processedCount = 0
        let totalPhotos = photos.count

        for photo in photos {
            autoreleasepool {
                do {
                    if !photo.isFromAssets {
                        let sourceFile = URL(fileURLWithPath: photo.path)

                        if fileManager.fileExists(atPath: sourceFile.path) {
                            let fileName = "\(photo.id)_\(sourceFile.lastPathComponent)"
                            let destFile = photosDir.appendingPathComponent(fileName)

                            // Apply compression based on settings
                            switch options.compressionLevel {
                            case .maximum:
                                try compressPhoto(source: sourceFile, destination: destFile, quality: 0.7)
                            case .normal:
                                try compressPhoto(source: sourceFile, destination: destFile, quality: 0.85)
                            case .fast, .none:
                                try fileManager.copyItem(at: sourceFile, to: destFile)
                            }

                            // Generate thumbnail if needed
                            if options.includeSettings {
                                let thumbnailFile = thumbnailsDir.appendingPathComponent("thumb_\(fileName)")
                                try generateThumbnail(source: sourceFile, destination: thumbnailFile)
                            }

                            // Calculate checksum
                            let checksum = try ZipUtils.calculateMD5(destFile)

                            // Add to manifest
                            let manifestEntry = PhotoManifestEntry(
                                photoId: photo.id,
                                originalPath: photo.path,
                                zipEntryName: "photos/\(fileName)",
                                fileName: fileName,
                                fileSize: try fileManager.attributesOfItem(atPath: destFile.path)[.size] as? Int64 ?? 0,
                                checksum: checksum
                            )

                            photoManifest.append(manifestEntry)
                        }
                    }

                    processedCount += 1
                    let progress = 30 + (processedCount * 40 / totalPhotos)
                    progressCallback?(progress, 100, "Processing photos (\(processedCount)/\(totalPhotos))")
                    updateProgress(progress, 100, "Processing photos")

                } catch {
                    logger.warning("Failed to process photo: \(photo.path) - \(error.localizedDescription)")
                }
            }
        }

        return photoManifest
    }

    private func createAppBackup(
        categories: [Category],
        photos: [Photo],
        photoManifest: [PhotoManifestEntry],
        options: BackupOptions
    ) async throws -> AppBackup {

        // Convert to backup models
        let backupCategories = categories.map { BackupCategory.fromCategory($0) }
        let backupPhotos = photos.map { BackupPhoto.fromPhoto($0) }

        // Get settings
        let isDarkMode = await settingsManager.isDarkMode
        let securitySummary = await getSecuritySummary()

        let backupSettings = BackupSettings(
            isDarkMode: isDarkMode,
            securitySettings: securitySummary
        )

        return AppBackup(
            version: CURRENT_BACKUP_VERSION,
            exportDate: Int64(Date().timeIntervalSince1970 * 1000),
            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
            format: .zip,
            categories: backupCategories,
            photos: backupPhotos,
            settings: backupSettings,
            photoManifest: photoManifest
        )
    }

    private func getSecuritySummary() async -> BackupSecuritySettings {
        // Get security settings from UserDefaults or KeyChain
        let defaults = UserDefaults.standard

        return BackupSecuritySettings(
            hasPIN: defaults.bool(forKey: "hasPIN"),
            hasPattern: defaults.bool(forKey: "hasPattern"),
            kidSafeModeEnabled: defaults.bool(forKey: "kidSafeModeEnabled"),
            cameraAccessAllowed: defaults.bool(forKey: "cameraAccessAllowed"),
            deleteProtectionEnabled: defaults.bool(forKey: "deleteProtectionEnabled")
        )
    }

    private func compressPhoto(source: URL, destination: URL, quality: CGFloat) throws {
        guard let image = UIImage(contentsOfFile: source.path) else {
            throw BackupError.exportFailed("Failed to load image")
        }

        guard let jpegData = image.jpegData(compressionQuality: quality) else {
            throw BackupError.compressionFailed
        }

        try jpegData.write(to: destination)
    }

    private func generateThumbnail(source: URL, destination: URL) throws {
        guard let image = UIImage(contentsOfFile: source.path) else {
            throw BackupError.exportFailed("Failed to load image for thumbnail")
        }

        let thumbnailSize = CGSize(width: 200, height: 200)
        let renderer = UIGraphicsImageRenderer(size: thumbnailSize)

        let thumbnail = renderer.image { context in
            image.draw(in: CGRect(origin: .zero, size: thumbnailSize))
        }

        guard let jpegData = thumbnail.jpegData(compressionQuality: 0.85) else {
            throw BackupError.compressionFailed
        }

        try jpegData.write(to: destination)
    }

    private func updateProgress(_ current: Int, _ total: Int, _ operation: String) {
        DispatchQueue.main.async {
            self.exportProgress = ExportProgress(
                current: current,
                total: total,
                operation: operation
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

    // MARK: - Backup History

    private func saveBackupHistory(
        fileName: String,
        filePath: String,
        fileSize: Int64,
        format: BackupFormat,
        photosCount: Int,
        categoriesCount: Int,
        compressionLevel: BackupOptions.CompressionLevel
    ) async {

        let entry = BackupHistoryEntry(
            id: UUID().uuidString,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            fileName: fileName,
            filePath: filePath,
            fileSize: fileSize,
            format: format,
            photosCount: photosCount,
            categoriesCount: categoriesCount,
            compressionLevel: compressionLevel,
            success: true,
            errorMessage: nil,
            automatic: false
        )

        backupHistory.insert(entry, at: 0)

        // Keep only last 20 entries
        if backupHistory.count > 20 {
            backupHistory = Array(backupHistory.prefix(20))
        }

        // Save to UserDefaults
        if let encoded = try? jsonEncoder.encode(backupHistory) {
            UserDefaults.standard.set(encoded, forKey: "backupHistory")
        }
    }

    private func loadBackupHistory() async {
        guard let data = UserDefaults.standard.data(forKey: "backupHistory"),
              let history = try? jsonDecoder.decode([BackupHistoryEntry].self, from: data) else {
            return
        }

        await MainActor.run {
            self.backupHistory = history
            self.lastBackupDate = history.first.map {
                Date(timeIntervalSince1970: TimeInterval($0.timestamp / 1000))
            }
        }
    }

    // MARK: - Cleanup

    func cleanupOldBackups(keepLast: Int = 10) async {
        let backups = backupHistory.sorted { $0.timestamp > $1.timestamp }

        guard backups.count > keepLast else { return }

        let toDelete = Array(backups.dropFirst(keepLast))

        for backup in toDelete {
            if let filePath = backup.filePath {
                try? fileManager.removeItem(atPath: filePath)
            }
        }

        backupHistory = Array(backups.prefix(keepLast))

        // Save updated history
        if let encoded = try? jsonEncoder.encode(backupHistory) {
            UserDefaults.standard.set(encoded, forKey: "backupHistory")
        }
    }
}

// MARK: - Backup History Entry Extension
extension BackupHistoryEntry: Codable {
    enum CodingKeys: String, CodingKey {
        case id, timestamp, fileName, filePath, fileSize
        case format, photosCount, categoriesCount
        case compressionLevel, success, errorMessage, automatic
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        timestamp = try container.decode(Int64.self, forKey: .timestamp)
        fileName = try container.decode(String.self, forKey: .fileName)
        filePath = try container.decodeIfPresent(String.self, forKey: .filePath)
        fileSize = try container.decode(Int64.self, forKey: .fileSize)
        format = try container.decode(BackupFormat.self, forKey: .format)
        photosCount = try container.decode(Int.self, forKey: .photosCount)
        categoriesCount = try container.decode(Int.self, forKey: .categoriesCount)

        let levelString = try container.decode(String.self, forKey: .compressionLevel)
        compressionLevel = BackupOptions.CompressionLevel(rawValue: Int(levelString) ?? 5) ?? .normal

        success = try container.decode(Bool.self, forKey: .success)
        errorMessage = try container.decodeIfPresent(String.self, forKey: .errorMessage)
        automatic = try container.decode(Bool.self, forKey: .automatic)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(fileName, forKey: .fileName)
        try container.encodeIfPresent(filePath, forKey: .filePath)
        try container.encode(fileSize, forKey: .fileSize)
        try container.encode(format, forKey: .format)
        try container.encode(photosCount, forKey: .photosCount)
        try container.encode(categoriesCount, forKey: .categoriesCount)
        try container.encode(String(compressionLevel.rawValue), forKey: .compressionLevel)
        try container.encode(success, forKey: .success)
        try container.encodeIfPresent(errorMessage, forKey: .errorMessage)
        try container.encode(automatic, forKey: .automatic)
    }
}

// MARK: - Backup History Entry
struct BackupHistoryEntry {
    let id: String
    let timestamp: Int64
    let fileName: String
    let filePath: String?
    let fileSize: Int64
    let format: BackupFormat
    let photosCount: Int
    let categoriesCount: Int
    let compressionLevel: BackupOptions.CompressionLevel
    let success: Bool
    let errorMessage: String?
    let automatic: Bool
}