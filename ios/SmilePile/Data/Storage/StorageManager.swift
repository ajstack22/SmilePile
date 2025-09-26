import Foundation
import UIKit
import Combine
import os.log

@MainActor
final class StorageManager: ObservableObject {
    static let shared: StorageManager = {
        do {
            return try StorageManager()
        } catch {
            fatalError("Failed to initialize StorageManager: \(error)")
        }
    }()

    private let logger = Logger(subsystem: "com.smilepile", category: "StorageManager")
    private let imageProcessor = ImageProcessor()
    private let safeThumbnailGenerator = SafeThumbnailGenerator()
    private let importCoordinator = PhotoImportCoordinator()

    // Test compatibility method
    func saveImage(_ image: UIImage, filename: String? = nil) async throws -> StorageResult {
        let photoURL = try await savePhotoToInternalStorage(image, filename: filename)

        // Generate thumbnail
        let thumbnailData = imageProcessor.generateThumbnail(from: image)
        var thumbnailPath: String? = nil

        if let thumbnailData = thumbnailData {
            let thumbnailFilename = filename?.replacingOccurrences(of: ".jpg", with: "_thumb.jpg") ?? UUID().uuidString + "_thumb.jpg"
            let thumbnailURL = thumbnailsDirectory.appendingPathComponent(thumbnailFilename)
            try thumbnailData.write(to: thumbnailURL)
            thumbnailPath = thumbnailURL.path
        }

        // Get file size
        let attributes = try FileManager.default.attributesOfItem(atPath: photoURL.path)
        let fileSize = attributes[.size] as? Int64 ?? 0

        return StorageResult(
            photoPath: photoURL.path,
            thumbnailPath: thumbnailPath,
            fileName: photoURL.lastPathComponent,
            fileSize: fileSize
        )
    }

    // MARK: - Memory Management Configuration
    private struct MemoryConfiguration {
        static let maxBatchSize: Int = 5
        static let maxMemoryUsageMB: Int = 100
        static let memoryCheckInterval: TimeInterval = 0.5
        static let processingDelayMs: UInt64 = 100_000_000 // 100ms
    }

    private let photosDirectory: URL
    private let thumbnailsDirectory: URL

    private let photosDirectoryName = "photos"
    private let thumbnailsDirectoryName = "thumbnails"
    private let megabyte: Int64 = 1024 * 1024
    private let minimumFreeSpace: Int64 = 10 * 1024 * 1024 // 10MB

    init() throws {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        photosDirectory = documentsURL.appendingPathComponent(photosDirectoryName)
        thumbnailsDirectory = documentsURL.appendingPathComponent(thumbnailsDirectoryName)

        try createDirectoriesIfNeeded()
    }

    // MARK: - Directory Management

    private func createDirectoriesIfNeeded() throws {
        do {
            try FileManager.default.createDirectory(at: photosDirectory, withIntermediateDirectories: true)
            try FileManager.default.createDirectory(at: thumbnailsDirectory, withIntermediateDirectories: true)
            logger.info("Storage directories created/verified")
        } catch {
            logger.error("Failed to create storage directories: \(error.localizedDescription)")
            throw StorageError.directoryCreationFailed("Failed to create storage directories: \(error.localizedDescription)")
        }
    }

    // MARK: - Photo Import with Batch Processing

    func importPhoto(from sourceURL: URL) async throws -> StorageResult {
        // Check available space
        guard hasEnoughSpace(estimatedSize: minimumFreeSpace) else {
            throw StorageError.insufficientSpace("Not enough storage space available")
        }

        // Check memory pressure
        guard await isMemorySafeToProcess() else {
            throw StorageError.memoryPressure("System memory pressure detected")
        }

        // Read source image data
        let imageData: Data
        do {
            imageData = try Data(contentsOf: sourceURL)
        } catch {
            throw StorageError.importFailed("Failed to read image from \(sourceURL.lastPathComponent): \(error.localizedDescription)")
        }

        // Process using safe thumbnail generator
        return try await processPhotoWithSafeGenerator(imageData: imageData, originalFileName: sourceURL.lastPathComponent)
    }

    func importPhotos(from sourceURLs: [URL]) async throws -> [StorageResult] {
        return try await importPhotosInBatches(sourceURLs: sourceURLs, batchSize: MemoryConfiguration.maxBatchSize)
    }

    /// Import photos in controlled batches to prevent memory issues
    func importPhotosInBatches(
        sourceURLs: [URL],
        batchSize: Int = MemoryConfiguration.maxBatchSize,
        progressHandler: ((Double) -> Void)? = nil
    ) async throws -> [StorageResult] {
        var results: [StorageResult] = []
        let totalCount = sourceURLs.count

        for batchStart in stride(from: 0, to: sourceURLs.count, by: batchSize) {
            // Check memory before each batch
            if !(await isMemorySafeToProcess()) {
                logger.warning("Memory pressure detected - pausing import")
                try await Task.sleep(nanoseconds: 500_000_000) // 500ms

                // Recheck memory
                guard await isMemorySafeToProcess() else {
                    throw StorageError.memoryPressure("Persistent memory pressure")
                }
            }

            let batchEnd = min(batchStart + batchSize, sourceURLs.count)
            let batch = Array(sourceURLs[batchStart..<batchEnd])

            logger.debug("Processing batch \(batchStart/batchSize + 1): \(batch.count) photos")

            // Process batch sequentially
            for (index, sourceURL) in batch.enumerated() {
                do {
                    let result = try await importPhoto(from: sourceURL)
                    results.append(result)

                    // Report progress
                    if let progressHandler = progressHandler {
                        let overallIndex = batchStart + index + 1
                        let progress = Double(overallIndex) / Double(totalCount)
                        await MainActor.run {
                            progressHandler(progress)
                        }
                    }
                } catch {
                    logger.error("Failed to import photo from \(sourceURL.lastPathComponent): \(error.localizedDescription)")
                    // Continue with other photos
                }
            }

            // Delay between batches to prevent CPU overload
            if batchEnd < sourceURLs.count {
                try await Task.sleep(nanoseconds: MemoryConfiguration.processingDelayMs)
            }
        }

        return results
    }

    // MARK: - Photo Saving

    func savePhotoToInternalStorage(_ image: UIImage, filename: String? = nil) async throws -> URL {
        // Generate filename if not provided
        let photoFileName = filename ?? generateFileName()

        // Process image
        guard let (_, imageData) = imageProcessor.processPhoto(image) else {
            throw StorageError.saveFailed("Failed to process image")
        }

        // Save photo
        let photoURL = photosDirectory.appendingPathComponent(photoFileName)
        do {
            try imageData.write(to: photoURL)
            logger.debug("Photo saved to: \(photoURL.lastPathComponent)")
            return photoURL
        } catch {
            throw StorageError.saveFailed("Failed to save photo: \(error.localizedDescription)")
        }
    }

    private func processAndSavePhoto(_ image: UIImage, originalFileName: String? = nil) async throws -> StorageResult {
        // Use safe thumbnail generator for new imports when possible
        if let imageData = image.jpegData(compressionQuality: 0.9) {
            return try await processPhotoWithSafeGenerator(imageData: imageData, originalFileName: originalFileName)
        }

        // Fallback to existing processor
        return try await processPhotoWithLegacyProcessor(image, originalFileName: originalFileName)
    }

    /// Process photo using the safe ImageIO-based generator
    private func processPhotoWithSafeGenerator(imageData: Data, originalFileName: String? = nil) async throws -> StorageResult {
        // Generate unique filename
        let fileName = generateFileName(basedOn: originalFileName)

        // Process image for storage
        let processedData = try await safeThumbnailGenerator.processImageForStorage(
            imageData: imageData,
            maxDimension: SafeThumbnailGenerator.Configuration.maxImageSize
        )

        // Generate thumbnail sequentially
        let thumbnailData = try await safeThumbnailGenerator.generateThumbnail(
            from: processedData,
            targetSize: SafeThumbnailGenerator.Configuration.thumbnailSize
        )

        // Save photo
        let photoPath = photosDirectory.appendingPathComponent(fileName)
        try processedData.write(to: photoPath)

        // Save thumbnail
        let thumbnailFileName = "thumb_\(fileName)"
        let thumbnailURL = thumbnailsDirectory.appendingPathComponent(thumbnailFileName)
        try thumbnailData.write(to: thumbnailURL)

        // Get metadata
        let metadata = safeThumbnailGenerator.getImageMetadata(from: processedData)

        return StorageResult(
            photoPath: photoPath.path,
            thumbnailPath: thumbnailURL.path,
            fileName: fileName,
            fileSize: Int64(processedData.count)
        )
    }

    /// Legacy processor for compatibility
    private func processPhotoWithLegacyProcessor(_ image: UIImage, originalFileName: String? = nil) async throws -> StorageResult {
        // Generate unique filename
        let fileName = generateFileName(basedOn: originalFileName)

        // Process main photo
        guard let (_, photoData) = imageProcessor.processPhoto(image) else {
            throw StorageError.saveFailed("Failed to process photo")
        }

        // Generate thumbnail
        let thumbnailData = imageProcessor.generateThumbnail(from: image)

        // Save photo
        let photoPath = photosDirectory.appendingPathComponent(fileName)
        try photoData.write(to: photoPath)

        // Save thumbnail
        var thumbnailPath: String? = nil
        if let thumbnailData = thumbnailData {
            let thumbnailFileName = "thumb_\(fileName)"
            let thumbnailURL = thumbnailsDirectory.appendingPathComponent(thumbnailFileName)
            try thumbnailData.write(to: thumbnailURL)
            thumbnailPath = thumbnailURL.path
        }

        return StorageResult(
            photoPath: photoPath.path,
            thumbnailPath: thumbnailPath,
            fileName: fileName,
            fileSize: Int64(photoData.count)
        )
    }

    // MARK: - Photo Deletion

    func deletePhoto(at photoPath: String) async throws -> Bool {
        let photoURL = URL(fileURLWithPath: photoPath)
        let thumbnailPath = getThumbnailPath(for: photoPath)

        var deletedPhoto = false
        var deletedThumbnail = false

        // Delete photo
        if FileManager.default.fileExists(atPath: photoPath) {
            do {
                try FileManager.default.removeItem(at: photoURL)
                deletedPhoto = true
                logger.debug("Deleted photo: \(photoURL.lastPathComponent)")
            } catch {
                throw StorageError.deleteFailed("Failed to delete photo: \(error.localizedDescription)")
            }
        }

        // Delete thumbnail
        if let thumbnailPath = thumbnailPath {
            let thumbnailURL = URL(fileURLWithPath: thumbnailPath)
            if FileManager.default.fileExists(atPath: thumbnailPath) {
                do {
                    try FileManager.default.removeItem(at: thumbnailURL)
                    deletedThumbnail = true
                    logger.debug("Deleted thumbnail: \(thumbnailURL.lastPathComponent)")
                } catch {
                    logger.warning("Failed to delete thumbnail: \(error.localizedDescription)")
                }
            }
        }

        return deletedPhoto || deletedThumbnail
    }

    // MARK: - Storage Management

    func calculateStorageUsage() async throws -> StorageUsage {
        var photoBytes: Int64 = 0
        var thumbnailBytes: Int64 = 0
        var photoCount = 0

        // Calculate photo storage
        if let photoFiles = try? FileManager.default.contentsOfDirectory(at: photosDirectory, includingPropertiesForKeys: [.fileSizeKey]) {
            for fileURL in photoFiles {
                if let attributes = try? FileManager.default.attributesOfItem(atPath: fileURL.path),
                   let fileSize = attributes[.size] as? Int64 {
                    photoBytes += fileSize
                    photoCount += 1
                }
            }
        }

        // Calculate thumbnail storage
        if let thumbnailFiles = try? FileManager.default.contentsOfDirectory(at: thumbnailsDirectory, includingPropertiesForKeys: [.fileSizeKey]) {
            for fileURL in thumbnailFiles {
                if let attributes = try? FileManager.default.attributesOfItem(atPath: fileURL.path),
                   let fileSize = attributes[.size] as? Int64 {
                    thumbnailBytes += fileSize
                }
            }
        }

        return StorageUsage(
            totalBytes: photoBytes + thumbnailBytes,
            photoBytes: photoBytes,
            thumbnailBytes: thumbnailBytes,
            photoCount: photoCount
        )
    }

    func cleanupOrphanedThumbnails() async throws -> Int {
        var deletedCount = 0

        // Get all photo filenames
        let photoFiles = try FileManager.default.contentsOfDirectory(at: photosDirectory, includingPropertiesForKeys: nil)
            .map { $0.lastPathComponent }

        // Get all thumbnail files
        let thumbnailFiles = try FileManager.default.contentsOfDirectory(at: thumbnailsDirectory, includingPropertiesForKeys: nil)

        for thumbnailURL in thumbnailFiles {
            let thumbnailName = thumbnailURL.lastPathComponent

            // Check if thumbnail has corresponding photo
            if thumbnailName.hasPrefix("thumb_") {
                let photoName = String(thumbnailName.dropFirst(6)) // Remove "thumb_" prefix
                if !photoFiles.contains(photoName) {
                    // Orphaned thumbnail found, delete it
                    try FileManager.default.removeItem(at: thumbnailURL)
                    deletedCount += 1
                    logger.debug("Deleted orphaned thumbnail: \(thumbnailName)")
                }
            }
        }

        logger.info("Cleaned up \(deletedCount) orphaned thumbnails")
        return deletedCount
    }

    // MARK: - Query Operations

    func getAllInternalPhotos() async throws -> [String] {
        let photoFiles = try FileManager.default.contentsOfDirectory(at: photosDirectory, includingPropertiesForKeys: nil)
        return photoFiles.map { $0.path }
    }

    func getThumbnailPath(for photoPath: String) -> String? {
        let photoURL = URL(fileURLWithPath: photoPath)
        let photoFileName = photoURL.lastPathComponent

        // Check if photo is in our internal storage
        if !isInternalStoragePath(photoPath) {
            return nil
        }

        let thumbnailFileName = "thumb_\(photoFileName)"
        let thumbnailPath = thumbnailsDirectory.appendingPathComponent(thumbnailFileName).path

        // Return path only if thumbnail exists
        if FileManager.default.fileExists(atPath: thumbnailPath) {
            return thumbnailPath
        }

        return nil
    }

    // MARK: - Space Management

    func getAvailableSpace() -> Int64 {
        do {
            let attributes = try FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())
            if let freeSpace = attributes[.systemFreeSize] as? NSNumber {
                return freeSpace.int64Value
            }
        } catch {
            logger.error("Failed to get available space: \(error.localizedDescription)")
        }
        return 0
    }

    func hasEnoughSpace(estimatedSize: Int64 = 10 * 1024 * 1024) -> Bool {
        return getAvailableSpace() > estimatedSize
    }

    func isInternalStoragePath(_ photoPath: String) -> Bool {
        let photoURL = URL(fileURLWithPath: photoPath)
        let photosPath = photosDirectory.path
        return photoPath.hasPrefix(photosPath) || photoURL.path.hasPrefix(photosPath)
    }

    // MARK: - Helper Methods

    private func generateFileName(basedOn originalName: String? = nil) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: Date())

        let uuid = UUID().uuidString.prefix(8)

        if let originalName = originalName,
           let fileExtension = originalName.split(separator: ".").last {
            return "IMG_\(timestamp)_\(uuid).\(fileExtension.lowercased())"
        } else {
            return "IMG_\(timestamp)_\(uuid).jpg"
        }
    }

    // MARK: - Migration Support

    func copyPhotoToInternalStorage(sourceFile: URL) async throws -> StorageResult {
        guard FileManager.default.fileExists(atPath: sourceFile.path) else {
            throw StorageError.fileNotFound("Source file not found: \(sourceFile.lastPathComponent)")
        }

        return try await importPhoto(from: sourceFile)
    }

    func migrateExternalPhotosToInternal(externalPhotoPaths: [String]) async throws -> [StorageResult] {
        // Use batch processing for migration
        let urls = externalPhotoPaths.map { URL(fileURLWithPath: $0) }
        return try await importPhotosInBatches(sourceURLs: urls, batchSize: MemoryConfiguration.maxBatchSize)
    }

    // MARK: - Memory Management

    /// Check if it's safe to process based on memory usage
    private func isMemorySafeToProcess() async -> Bool {
        return safeThumbnailGenerator.isSafeToProcess()
    }

    /// Get current memory usage in MB
    func getCurrentMemoryUsage() -> Int {
        return safeThumbnailGenerator.getCurrentMemoryUsage()
    }

    /// Monitor storage pressure
    func getStoragePressure() -> StoragePressure {
        let availableSpace = getAvailableSpace()
        let availableGB = Double(availableSpace) / (1024 * 1024 * 1024)

        if availableGB < 0.5 {
            return .critical
        } else if availableGB < 1.0 {
            return .high
        } else if availableGB < 2.0 {
            return .medium
        } else {
            return .low
        }
    }

    enum StoragePressure {
        case low
        case medium
        case high
        case critical

        var description: String {
            switch self {
            case .low:
                return "Storage space is adequate"
            case .medium:
                return "Storage space is running low"
            case .high:
                return "Storage space is very low"
            case .critical:
                return "Storage space is critically low"
            }
        }
    }
}