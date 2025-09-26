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

    // MARK: - Photo Import

    func importPhoto(from sourceURL: URL) async throws -> StorageResult {
        // Check available space
        guard hasEnoughSpace(estimatedSize: minimumFreeSpace) else {
            throw StorageError.insufficientSpace("Not enough storage space available")
        }

        // Read source image data
        let imageData: Data
        do {
            imageData = try Data(contentsOf: sourceURL)
        } catch {
            throw StorageError.importFailed("Failed to read image from \(sourceURL.lastPathComponent): \(error.localizedDescription)")
        }

        // Validate image
        guard let sourceImage = UIImage(data: imageData) else {
            throw StorageError.invalidImageData("Invalid image data at \(sourceURL.lastPathComponent)")
        }

        // Process and save photo
        return try await processAndSavePhoto(sourceImage, originalFileName: sourceURL.lastPathComponent)
    }

    func importPhotos(from sourceURLs: [URL]) async throws -> [StorageResult] {
        var results: [StorageResult] = []

        for sourceURL in sourceURLs {
            do {
                let result = try await importPhoto(from: sourceURL)
                results.append(result)
            } catch {
                logger.error("Failed to import photo from \(sourceURL.lastPathComponent): \(error.localizedDescription)")
                // Continue with other photos
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
        var results: [StorageResult] = []

        for photoPath in externalPhotoPaths {
            let photoURL = URL(fileURLWithPath: photoPath)
            do {
                let result = try await copyPhotoToInternalStorage(sourceFile: photoURL)
                results.append(result)
            } catch {
                logger.error("Failed to migrate photo \(photoURL.lastPathComponent): \(error.localizedDescription)")
            }
        }

        return results
    }
}