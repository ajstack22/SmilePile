import Foundation
import UIKit
import Photos
import PhotosUI
import CoreData
import UniformTypeIdentifiers
import CryptoKit
import CoreLocation
import ImageIO
import CoreImage
import os.log

/// Thread-safe coordinator for photo imports using Swift actors
/// Prevents race conditions and ensures safe concurrent processing
actor PhotoImportCoordinator {

    // MARK: - Configuration

    struct Configuration {
        static let maxBatchSize: Int = 5
        static let maxMemoryUsageMB: Int = 100
        static let processingDelayMs: UInt64 = 100_000_000 // 100ms between batches
        static let maxRetries: Int = 2
        static let sessionTimeout: TimeInterval = 300 // 5 minutes
    }

    // MARK: - State

    enum ImportState {
        case idle
        case preparing
        case importing(sessionId: String)
        case paused(sessionId: String)
        case completed
        case failed(Error)
    }

    // MARK: - Properties

    private var currentState: ImportState = .idle
    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoImportCoordinator")
    private let thumbnailGenerator = SafeThumbnailGenerator()
    private let sessionManager = PhotoImportSessionManager()
    private let storageManager: StorageManager
    private let photoRepository: PhotoRepositoryImpl
    private var activeTask: Task<Void, Error>?
    private var progressHandlers: [(Double) -> Void] = []

    // Memory monitoring
    private var lastMemoryCheck = Date()
    private var memoryPressureCount = 0

    // MARK: - Initialization

    init(storageManager: StorageManager,
         photoRepository: PhotoRepositoryImpl? = nil) {
        self.storageManager = storageManager
        self.photoRepository = photoRepository ?? PhotoRepositoryImpl()
    }

    // Convenience initializer that can be called from MainActor context
    @MainActor
    static func createDefault() -> PhotoImportCoordinator {
        return PhotoImportCoordinator(
            storageManager: StorageManager.shared,
            photoRepository: PhotoRepositoryImpl()
        )
    }

    // MARK: - Public Methods

    /// Start a new photo import with safety checks
    func startImport(
        from pickerResults: [PHPickerResult],
        categoryId: Int64,
        progressHandler: @escaping (Double) -> Void
    ) async throws -> ImportResult {
        guard case .idle = currentState else {
            throw ImportError.importInProgress
        }

        currentState = .preparing
        progressHandlers.append(progressHandler)

        logger.info("Starting import of \(pickerResults.count) photos")

        do {
            // Convert picker results to URLs
            let photoURLs = try await extractPhotoURLs(from: pickerResults)

            // Create import session
            let session = try await sessionManager.createSession(
                photoURLs: photoURLs,
                categoryId: categoryId
            )

            currentState = .importing(sessionId: session.sessionId)

            // Start import task
            let result = try await performImport(
                session: session,
                photoURLs: photoURLs,
                categoryId: categoryId
            )

            currentState = .completed
            progressHandlers.removeAll()

            return result
        } catch {
            currentState = .failed(error)
            progressHandlers.removeAll()
            throw error
        }
    }

    /// Resume a paused import session
    func resumeImport(sessionId: String) async throws -> ImportResult {
        guard case .idle = currentState else {
            throw ImportError.importInProgress
        }

        guard let session = try await sessionManager.resumeSession(sessionId) else {
            throw ImportError.sessionNotFound
        }

        currentState = .importing(sessionId: sessionId)

        logger.info("Resuming import session: \(sessionId)")

        do {
            let result = try await performImport(
                session: session,
                photoURLs: session.pendingURLs,
                categoryId: session.categoryId
            )

            currentState = .completed
            return result
        } catch {
            currentState = .failed(error)
            throw error
        }
    }

    /// Pause the current import
    func pauseImport() async throws {
        guard case .importing(let sessionId) = currentState else {
            throw ImportError.noActiveImport
        }

        activeTask?.cancel()
        try await sessionManager.pauseSession(sessionId)
        currentState = .paused(sessionId: sessionId)

        logger.info("Import paused: \(sessionId)")
    }

    /// Cancel the current import
    func cancelImport() async throws {
        switch currentState {
        case .importing(let sessionId), .paused(let sessionId):
            activeTask?.cancel()
            try await sessionManager.cancelSession(sessionId)
            currentState = .idle
            logger.info("Import cancelled: \(sessionId)")
        default:
            throw ImportError.noActiveImport
        }
    }

    /// Get current import state
    func getCurrentState() -> ImportState {
        return currentState
    }

    /// Check if import is active
    func isImporting() -> Bool {
        switch currentState {
        case .importing:
            return true
        default:
            return false
        }
    }

    // MARK: - Private Import Methods

    private func performImport(
        session: PhotoImportSession,
        photoURLs: [URL],
        categoryId: Int64
    ) async throws -> ImportResult {
        var processedPhotos: [Photo] = []
        var failedURLs: [URL] = []
        var processedIds: [String] = []
        let totalCount = photoURLs.count

        // Process in batches
        for batchStart in stride(from: 0, to: photoURLs.count, by: Configuration.maxBatchSize) {
            // Check for cancellation
            if Task.isCancelled {
                throw ImportError.cancelled
            }

            // Check memory pressure
            try await checkMemoryPressure()

            let batchEnd = min(batchStart + Configuration.maxBatchSize, photoURLs.count)
            let batch = Array(photoURLs[batchStart..<batchEnd])

            logger.debug("Processing batch \(batchStart/Configuration.maxBatchSize + 1): \(batch.count) photos")

            // Process batch sequentially for safety
            for (index, photoURL) in batch.enumerated() {
                do {
                    let photo = try await processPhoto(
                        from: photoURL,
                        categoryId: categoryId
                    )
                    processedPhotos.append(photo)
                    processedIds.append(photoURL.absoluteString)

                    // Update progress
                    let overallIndex = batchStart + index + 1
                    let progress = Double(overallIndex) / Double(totalCount)
                    await notifyProgress(progress)

                    // Update session progress periodically
                    if overallIndex % 5 == 0 || overallIndex == totalCount {
                        try await sessionManager.updateProgress(
                            sessionId: session.sessionId,
                            processedCount: processedPhotos.count,
                            failedCount: failedURLs.count,
                            processedIds: processedIds,
                            failedURLs: failedURLs
                        )
                    }

                } catch {
                    logger.error("Failed to process photo: \(error.localizedDescription)")
                    failedURLs.append(photoURL)
                }
            }

            // Delay between batches
            if batchEnd < photoURLs.count {
                try await Task.sleep(nanoseconds: Configuration.processingDelayMs)
            }
        }

        // Complete session
        try await sessionManager.completeSession(session.sessionId)

        let result = ImportResult(
            sessionId: session.sessionId,
            totalPhotos: totalCount,
            successCount: processedPhotos.count,
            failedCount: failedURLs.count,
            importedPhotos: processedPhotos,
            failedURLs: failedURLs
        )

        logger.info("Import completed: \(result.successCount)/\(result.totalPhotos) successful")

        return result
    }

    private func processPhoto(from url: URL, categoryId: Int64) async throws -> Photo {
        // Load image data
        let imageData = try Data(contentsOf: url)

        // Check image validity
        guard UIImage(data: imageData) != nil else {
            throw ImportError.invalidImageData
        }

        // Process image for storage
        let processedData = try await thumbnailGenerator.processImageForStorage(
            imageData: imageData
        )

        // Generate thumbnail sequentially
        let thumbnailData = try await thumbnailGenerator.generateThumbnail(
            from: processedData,
            targetSize: SafeThumbnailGenerator.Configuration.thumbnailSize
        )

        // Get metadata
        let metadata = thumbnailGenerator.getImageMetadata(from: processedData)

        // Save to storage
        let fileName = generateFileName(for: url)
        let photoPath = try await savePhotoData(processedData, fileName: fileName)
        let thumbnailPath = try await saveThumbnailData(thumbnailData, fileName: "thumb_\(fileName)")

        // Create Photo object
        let photo = Photo(
            path: photoPath,
            categoryId: categoryId,
            name: url.lastPathComponent,
            isFromAssets: false,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            fileSize: Int64(processedData.count),
            width: metadata?.width ?? 0,
            height: metadata?.height ?? 0
        )

        // Save to repository
        _ = try await photoRepository.insertPhoto(photo)

        return photo
    }

    // MARK: - Helper Methods

    private func extractPhotoURLs(from pickerResults: [PHPickerResult]) async throws -> [URL] {
        var urls: [URL] = []

        for result in pickerResults {
            if result.itemProvider.canLoadObject(ofClass: UIImage.self) {
                let url = try await loadPhotoURL(from: result)
                urls.append(url)
            }
        }

        return urls
    }

    private func loadPhotoURL(from result: PHPickerResult) async throws -> URL {
        return try await withCheckedThrowingContinuation { continuation in
            result.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { url, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let url = url {
                    // Copy to temporary location
                    let tempURL = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString)
                        .appendingPathExtension("jpg")

                    do {
                        if FileManager.default.fileExists(atPath: tempURL.path) {
                            try FileManager.default.removeItem(at: tempURL)
                        }
                        try FileManager.default.copyItem(at: url, to: tempURL)
                        continuation.resume(returning: tempURL)
                    } catch {
                        continuation.resume(throwing: error)
                    }
                } else {
                    continuation.resume(throwing: ImportError.failedToLoadPhoto)
                }
            }
        }
    }

    private func savePhotoData(_ data: Data, fileName: String) async throws -> String {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let photosDirectory = documentsURL.appendingPathComponent("photos")
        try FileManager.default.createDirectory(at: photosDirectory, withIntermediateDirectories: true)

        let photoURL = photosDirectory.appendingPathComponent(fileName)
        try data.write(to: photoURL)

        return photoURL.path
    }

    private func saveThumbnailData(_ data: Data, fileName: String) async throws -> String {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let thumbnailsDirectory = documentsURL.appendingPathComponent("thumbnails")
        try FileManager.default.createDirectory(at: thumbnailsDirectory, withIntermediateDirectories: true)

        let thumbnailURL = thumbnailsDirectory.appendingPathComponent(fileName)
        try data.write(to: thumbnailURL)

        return thumbnailURL.path
    }

    private func generateFileName(for url: URL) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: Date())
        let uuid = UUID().uuidString.prefix(8)
        let ext = url.pathExtension.isEmpty ? "jpg" : url.pathExtension.lowercased()
        return "IMG_\(timestamp)_\(uuid).\(ext)"
    }

    // MARK: - Memory Management

    private func checkMemoryPressure() async throws {
        let memoryUsage = thumbnailGenerator.getCurrentMemoryUsage()

        if memoryUsage > Configuration.maxMemoryUsageMB {
            self.memoryPressureCount += 1
            logger.warning("Memory pressure detected: \(memoryUsage)MB (count: \(self.memoryPressureCount))")

            if memoryPressureCount > 3 {
                throw ImportError.memoryPressure
            }

            // Wait for memory to be freed
            try await Task.sleep(nanoseconds: 500_000_000) // 500ms
        } else {
            memoryPressureCount = 0
        }

        lastMemoryCheck = Date()
    }

    private func notifyProgress(_ progress: Double) async {
        let handlers = progressHandlers
        await MainActor.run {
            for handler in handlers {
                handler(progress)
            }
        }
    }
}

// MARK: - Import Result

struct ImportResult {
    let sessionId: String
    let totalPhotos: Int
    let successCount: Int
    let failedCount: Int
    let importedPhotos: [Photo]
    let failedURLs: [URL]

    var successRate: Double {
        guard totalPhotos > 0 else { return 0 }
        return Double(successCount) / Double(totalPhotos)
    }
}

// MARK: - Import Error

enum ImportError: LocalizedError {
    case importInProgress
    case sessionNotFound
    case noActiveImport
    case cancelled
    case memoryPressure
    case invalidImageData
    case failedToLoadPhoto
    case storageError(String)

    var errorDescription: String? {
        switch self {
        case .importInProgress:
            return "An import is already in progress"
        case .sessionNotFound:
            return "Import session not found"
        case .noActiveImport:
            return "No active import to pause or cancel"
        case .cancelled:
            return "Import was cancelled"
        case .memoryPressure:
            return "System memory pressure - please try importing fewer photos"
        case .invalidImageData:
            return "Invalid or corrupted image data"
        case .failedToLoadPhoto:
            return "Failed to load photo from picker"
        case .storageError(let message):
            return "Storage error: \(message)"
        }
    }
}

