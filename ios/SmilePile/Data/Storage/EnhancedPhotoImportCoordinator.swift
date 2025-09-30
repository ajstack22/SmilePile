import Foundation
import UIKit
import Photos
import PhotosUI
import CoreData
import UniformTypeIdentifiers
import os.log

/// Enhanced photo import coordinator with multi-size thumbnail generation
actor EnhancedPhotoImportCoordinator {

    // MARK: - Configuration
    struct Configuration {
        static let maxBatchSize: Int = 3 // Reduced for better memory management
        static let maxMemoryUsageMB: Int = 80
        static let processingDelayMs: UInt64 = 200_000_000 // 200ms between batches
        static let maxRetries: Int = 2
        static let sessionTimeout: TimeInterval = 300 // 5 minutes
    }

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "EnhancedPhotoImportCoordinator")
    private let thumbnailGenerator = SafeThumbnailGenerator()
    private let thumbnailSizeManager = ThumbnailSizeManager.shared
    private let sessionManager = PhotoImportSessionManager()
    private let storageManager: StorageManager
    private let photoRepository: PhotoRepositoryImpl
    // Memory monitor will be accessed via MainActor context
    private nonisolated var memoryMonitor: MemoryMonitor {
        get async {
            await MainActor.run { MemoryMonitor.shared }
        }
    }

    private var currentState: ImportState = .idle
    private var activeTask: Task<Void, Error>?
    private var progressHandlers: [(EnhancedImportProgress) -> Void] = []

    // MARK: - State
    enum ImportState {
        case idle
        case preparing
        case importing(sessionId: String)
        case paused(sessionId: String)
        case completed
        case failed(Error)
    }

    // MARK: - Initialization
    init(storageManager: StorageManager,
         photoRepository: PhotoRepositoryImpl = PhotoRepositoryImpl()) {
        self.storageManager = storageManager
        self.photoRepository = photoRepository
    }

    // Convenience initializer that can be called from MainActor context
    @MainActor
    static func createDefault() -> EnhancedPhotoImportCoordinator {
        return EnhancedPhotoImportCoordinator(
            storageManager: StorageManager.shared,
            photoRepository: PhotoRepositoryImpl()
        )
    }

    // MARK: - Public Methods

    /// Start enhanced photo import with multi-size thumbnails
    func startEnhancedImport(
        from pickerResults: [PHPickerResult],
        categoryId: Int64,
        progressHandler: @escaping (EnhancedImportProgress) -> Void
    ) async throws -> EnhancedImportResult {
        guard case .idle = currentState else {
            throw ImportError.importInProgress
        }

        // Validate category ID
        guard categoryId > 0 else {
            logger.error("Invalid category ID: \(categoryId)")
            throw ImportError.invalidCategory("Category ID must be greater than 0")
        }

        currentState = .preparing
        progressHandlers.append(progressHandler)

        logger.info("Starting enhanced import of \(pickerResults.count) photos into category \(categoryId)")

        do {
            // Convert picker results to URLs
            let photoURLs = try await extractPhotoURLs(from: pickerResults)

            // Check available storage
            let requiredSizes = await MainActor.run {
                thumbnailSizeManager.recommendedSizesForDevice()
            }
            let estimatedStorage = await MainActor.run {
                thumbnailSizeManager.estimateStorageNeeded(
                    photoCount: photoURLs.count,
                    sizes: requiredSizes
                )
            }

            guard hasEnoughStorage(bytes: estimatedStorage) else {
                throw ImportError.insufficientStorage
            }

            // Create import session
            let session = try await sessionManager.createSession(
                photoURLs: photoURLs,
                categoryId: categoryId
            )

            currentState = .importing(sessionId: session.sessionId)

            // Log session creation with category
            logger.info("Created import session \(session.sessionId) for category \(categoryId)")

            // Start import with enhanced processing
            let result = try await performEnhancedImport(
                session: session,
                photoURLs: photoURLs,
                categoryId: categoryId,
                thumbnailSizes: requiredSizes
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

    // MARK: - Enhanced Import Implementation

    private func performEnhancedImport(
        session: PhotoImportSession,
        photoURLs: [URL],
        categoryId: Int64,
        thumbnailSizes: [ThumbnailSize]
    ) async throws -> EnhancedImportResult {
        var processedPhotos: [Photo] = []
        var failedURLs: [URL] = []
        var thumbnailStats: [ThumbnailSize: Int] = [:]
        let totalCount = photoURLs.count

        // Initialize stats
        for size in thumbnailSizes {
            thumbnailStats[size] = 0
        }

        // Process in small batches for memory efficiency
        for batchStart in stride(from: 0, to: photoURLs.count, by: Configuration.maxBatchSize) {
            // Check for cancellation
            if Task.isCancelled {
                throw ImportError.cancelled
            }

            // Check memory pressure
            try await checkMemoryPressureWithAdaptation()

            let batchEnd = min(batchStart + Configuration.maxBatchSize, photoURLs.count)
            let batch = Array(photoURLs[batchStart..<batchEnd])

            logger.debug("Processing enhanced batch \(batchStart/Configuration.maxBatchSize + 1): \(batch.count) photos")

            // Process batch sequentially
            for (index, photoURL) in batch.enumerated() {
                do {
                    let (photo, generatedSizes) = try await processPhotoWithMultipleThumbnails(
                        from: photoURL,
                        categoryId: categoryId,
                        sizes: thumbnailSizes
                    )

                    processedPhotos.append(photo)

                    // Update thumbnail stats
                    for size in generatedSizes {
                        thumbnailStats[size]? += 1
                    }

                    // Update progress
                    let overallIndex = batchStart + index + 1
                    let monitor = await memoryMonitor
                    let memUsage = await MainActor.run {
                        monitor.currentMemoryUsageMB
                    }
                    let progress = EnhancedImportProgress(
                        currentPhoto: overallIndex,
                        totalPhotos: totalCount,
                        currentStage: .generatingThumbnails,
                        percentComplete: Double(overallIndex) / Double(totalCount),
                        memoryUsageMB: memUsage
                    )
                    await notifyProgress(progress)

                } catch {
                    logger.error("Failed to process photo: \(error.localizedDescription)")
                    failedURLs.append(photoURL)
                }
            }

            // Adaptive delay based on memory pressure
            let delayMs = await adaptiveDelay()
            if batchEnd < photoURLs.count {
                try await Task.sleep(nanoseconds: delayMs)
            }
        }

        // Complete session
        try await sessionManager.completeSession(session.sessionId)

        let result = EnhancedImportResult(
            sessionId: session.sessionId,
            totalPhotos: totalCount,
            successCount: processedPhotos.count,
            failedCount: failedURLs.count,
            importedPhotos: processedPhotos,
            failedURLs: failedURLs,
            thumbnailStats: thumbnailStats
        )

        logger.info("Enhanced import completed: \(result.successCount)/\(result.totalPhotos) successful")
        logger.info("Thumbnails generated: \(thumbnailStats)")

        return result
    }

    private func processPhotoWithMultipleThumbnails(
        from url: URL,
        categoryId: Int64,
        sizes: [ThumbnailSize]
    ) async throws -> (Photo, [ThumbnailSize]) {
        // Load image data
        let imageData = try Data(contentsOf: url)

        // Validate image
        guard UIImage(data: imageData) != nil else {
            throw ImportError.invalidImageData
        }

        // Process image for storage
        let processedData = try await thumbnailGenerator.processImageForStorage(
            imageData: imageData
        )

        // Get metadata
        let metadata = thumbnailGenerator.getImageMetadata(from: processedData)

        // Save original photo
        let fileName = generateFileName(for: url)
        let photoPath = try await savePhotoData(processedData, fileName: fileName)

        // Generate thumbnails for each size
        var generatedSizes: [ThumbnailSize] = []
        var thumbnailPaths: [ThumbnailSize: String] = [:]

        for size in sizes {
            // Adapt size based on memory pressure
            let actualSize = await adaptThumbnailSize(requested: size)

            do {
                let thumbnailData = try await thumbnailGenerator.generateThumbnail(
                    from: processedData,
                    targetSize: actualSize.pixelSize
                )

                let thumbnailPath = try await saveThumbnail(
                    data: thumbnailData,
                    fileName: fileName,
                    size: actualSize
                )

                thumbnailPaths[actualSize] = thumbnailPath
                generatedSizes.append(actualSize)

            } catch {
                logger.error("Failed to generate \(String(describing: actualSize)) thumbnail: \(error.localizedDescription)")
                // Continue with other sizes
            }
        }

        // Create Photo object with paths - ensure categoryId is preserved
        let photo = Photo(
            path: photoPath,
            categoryId: categoryId,  // Explicitly set category ID
            name: url.lastPathComponent,
            isFromAssets: false,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            fileSize: Int64(processedData.count),
            width: metadata?.width ?? 0,
            height: metadata?.height ?? 0
        )

        // Validate category before saving
        guard photo.categoryId > 0 else {
            logger.error("Photo has invalid category ID: \(photo.categoryId)")
            throw ImportError.invalidCategory("Photo must have valid category ID")
        }

        // Save to repository and get the generated ID
        let photoId = try await photoRepository.insertPhoto(photo)
        logger.debug("Saved photo with ID \(photoId) to category \(categoryId)")

        return (photo, generatedSizes)
    }

    // MARK: - Adaptive Memory Management

    private func checkMemoryPressureWithAdaptation() async throws {
        let monitor = await memoryMonitor
        let memoryInfo = await MainActor.run {
            monitor.getCurrentMemoryInfo()
        }

        switch memoryInfo.pressureLevel {
        case .normal:
            // Continue normally
            break
        case .warning:
            // Small delay
            logger.warning("Memory warning: \(memoryInfo.currentMB)MB")
            try await Task.sleep(nanoseconds: 200_000_000) // 200ms
        case .critical:
            // Longer delay and request cleanup
            logger.warning("Critical memory: \(memoryInfo.currentMB)MB")
            await MainActor.run {
                monitor.requestMemoryCleanup()
            }
            try await Task.sleep(nanoseconds: 500_000_000) // 500ms
        case .danger:
            // Pause import
            throw ImportError.memoryPressure
        }
    }

    private func adaptThumbnailSize(requested: ThumbnailSize) async -> ThumbnailSize {
        let monitor = await memoryMonitor
        let memoryUsage = await MainActor.run {
            monitor.currentMemoryUsageMB
        }
        return thumbnailSizeManager.adjustSizeForMemoryPressure(
            requested: requested,
            memoryUsageMB: memoryUsage
        )
    }

    private func adaptiveDelay() async -> UInt64 {
        let monitor = await memoryMonitor
        let memoryInfo = await MainActor.run {
            monitor.getCurrentMemoryInfo()
        }

        switch memoryInfo.pressureLevel {
        case .normal:
            return Configuration.processingDelayMs
        case .warning:
            return Configuration.processingDelayMs * 2
        case .critical:
            return Configuration.processingDelayMs * 3
        case .danger:
            return Configuration.processingDelayMs * 5
        }
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

        // Return relative path from Documents directory
        let relativePath = photoURL.path.replacingOccurrences(of: documentsURL.path + "/", with: "")
        logger.debug("Saved photo to relative path: \(relativePath)")
        return relativePath
    }

    private func saveThumbnail(data: Data, fileName: String, size: ThumbnailSize) async throws -> String {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let thumbnailsDir = documentsURL.appendingPathComponent("thumbnails")
        let sizeDir = thumbnailsDir.appendingPathComponent(size.directoryName)
        try FileManager.default.createDirectory(at: sizeDir, withIntermediateDirectories: true)

        let spec = thumbnailSizeManager.specification(for: size)!
        let thumbnailFileName = spec.fileNamePrefix + fileName
        let thumbnailURL = sizeDir.appendingPathComponent(thumbnailFileName)
        try data.write(to: thumbnailURL)

        // Return relative path from Documents directory
        let relativePath = thumbnailURL.path.replacingOccurrences(of: documentsURL.path + "/", with: "")
        logger.debug("Saved thumbnail to relative path: \(relativePath)")
        return relativePath
    }

    private func generateFileName(for url: URL) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: Date())
        let uuid = UUID().uuidString.prefix(8)
        let ext = url.pathExtension.isEmpty ? "jpg" : url.pathExtension.lowercased()
        return "IMG_\(timestamp)_\(uuid).\(ext)"
    }

    private func hasEnoughStorage(bytes: Int) -> Bool {
        do {
            let documentDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let attributes = try FileManager.default.attributesOfFileSystem(forPath: documentDirectory.path)
            if let freeSpace = attributes[.systemFreeSize] as? NSNumber {
                return freeSpace.int64Value > Int64(bytes) + (10 * 1024 * 1024) // Keep 10MB buffer
            }
        } catch {
            logger.error("Failed to check storage: \(error.localizedDescription)")
        }
        return false
    }

    private func notifyProgress(_ progress: EnhancedImportProgress) async {
        let handlers = progressHandlers
        await MainActor.run {
            for handler in handlers {
                handler(progress)
            }
        }
    }
}

// MARK: - Import Progress
struct EnhancedImportProgress {
    let currentPhoto: Int
    let totalPhotos: Int
    let currentStage: ImportStage
    let percentComplete: Double
    let memoryUsageMB: Int

    enum ImportStage {
        case preparing
        case loadingPhotos
        case processingImages
        case generatingThumbnails
        case savingToDatabase
        case completed
    }
}

// MARK: - Enhanced Import Result
struct EnhancedImportResult {
    let sessionId: String
    let totalPhotos: Int
    let successCount: Int
    let failedCount: Int
    let importedPhotos: [Photo]
    let failedURLs: [URL]
    let thumbnailStats: [ThumbnailSize: Int]

    var successRate: Double {
        guard totalPhotos > 0 else { return 0 }
        return Double(successCount) / Double(totalPhotos)
    }

    var summary: String {
        """
        Import Complete:
        - Success: \(successCount)/\(totalPhotos)
        - Failed: \(failedCount)
        - Success Rate: \(String(format: "%.1f%%", successRate * 100))
        - Thumbnails: \(thumbnailStats.map { "\($0.key): \($0.value)" }.joined(separator: ", "))
        """
    }
}

// MARK: - Enhanced Import Error
// Extension removed - ImportError doesn't have storageError case