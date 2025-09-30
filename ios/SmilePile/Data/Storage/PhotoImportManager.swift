import Foundation
import Photos
import PhotosUI
import CryptoKit
import UIKit
import os.log

/// Main manager for photo import functionality with complete Android feature parity
@MainActor
final class PhotoImportManager: ObservableObject {

    // MARK: - Configuration
    struct Configuration {
        static let maxPhotosPerBatch = 50
        static let maxImageDimension: CGFloat = 2048
        static let thumbnailSize: CGFloat = 300
        static let jpegQuality: CGFloat = 0.9
        static let thumbnailQuality: CGFloat = 0.85
        static let supportedFormats = ["jpeg", "jpg", "png", "heif", "heic"]
        static let maxRetries = 2
        static let processingDelayMs: UInt64 = 100_000_000 // 100ms
    }

    // MARK: - Import State
    enum ImportState {
        case idle
        case requestingPermission
        case selecting
        case processing(progress: Double, message: String)
        case completed(count: Int)
        case failed(Error)
    }

    // MARK: - Published Properties
    @Published private(set) var state: ImportState = .idle
    @Published private(set) var importProgress: Double = 0
    @Published private(set) var importMessage: String = ""
    @Published private(set) var processedCount: Int = 0
    @Published private(set) var totalCount: Int = 0

    // MARK: - Dependencies
    private let storageManager: StorageManager
    private let photoRepository: PhotoRepository
    private let metadataExtractor: PhotoMetadataExtractor
    private let photoOptimizer: PhotoOptimizer
    private let duplicateDetector: DuplicateDetector
    private let permissionManager: PhotoLibraryPermissionManager

    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoImportManager")

    // MARK: - Private Properties
    private var currentImportTask: Task<Void, Error>?
    private var importedHashes = Set<String>()

    // MARK: - Initialization
    init(
        storageManager: StorageManager? = nil,
        photoRepository: PhotoRepository? = nil
    ) {
        self.storageManager = storageManager ?? StorageManager.shared
        self.photoRepository = photoRepository ?? PhotoRepositoryImpl()
        self.metadataExtractor = PhotoMetadataExtractor()
        self.photoOptimizer = PhotoOptimizer()
        self.duplicateDetector = DuplicateDetector()
        self.permissionManager = PhotoLibraryPermissionManager.shared

        // Load existing photo hashes for duplicate detection
        Task {
            await loadExistingPhotoHashes()
        }
    }

    // MARK: - Public Methods

    /// Start photo import flow with permission handling
    func startPhotoImport(categoryId: Int64) async throws {
        guard state == .idle || state == .failed(_) else {
            throw ImportError.importInProgress
        }

        state = .requestingPermission

        // Check photo library permission
        let status = await permissionManager.requestAuthorization()

        switch status {
        case .authorized, .limited:
            state = .selecting
            // Permission granted, UI will show picker
        case .denied:
            state = .failed(ImportError.permissionDenied)
            throw ImportError.permissionDenied
        case .restricted:
            state = .failed(ImportError.permissionRestricted)
            throw ImportError.permissionRestricted
        case .notDetermined:
            state = .idle
        @unknown default:
            state = .idle
        }
    }

    /// Process selected photos from PHPicker
    func processSelectedPhotos(
        _ results: [PHPickerResult],
        categoryId: Int64
    ) async throws -> PhotoManagerImportResult {
        // Validate batch size
        guard results.count <= Configuration.maxPhotosPerBatch else {
            throw ImportError.batchSizeLimitExceeded(limit: Configuration.maxPhotosPerBatch)
        }

        // Cancel any existing import
        currentImportTask?.cancel()

        // Reset state
        state = .processing(progress: 0, message: "Preparing photos...")
        importProgress = 0
        processedCount = 0
        totalCount = results.count

        logger.info("Starting import of \(results.count) photos for category \(categoryId)")

        // Create import task
        currentImportTask = Task {
            do {
                let result = try await performImport(results: results, categoryId: categoryId)

                state = .completed(count: result.successCount)
                importMessage = "Successfully imported \(result.successCount) photos"

                return result
            } catch {
                state = .failed(error)
                throw error
            }
        }

        return try await currentImportTask!.value as! PhotoManagerImportResult
    }

    /// Cancel current import operation
    func cancelImport() {
        currentImportTask?.cancel()
        state = .idle
        importProgress = 0
        importMessage = ""
        logger.info("Import cancelled by user")
    }

    // MARK: - Private Import Methods

    private func performImport(
        results: [PHPickerResult],
        categoryId: Int64
    ) async throws -> PhotoManagerImportResult {
        var successfulImports: [Photo] = []
        var failedItems: [(index: Int, error: Error)] = []
        var skippedDuplicates: [String] = []

        for (index, result) in results.enumerated() {
            // Check for cancellation
            if Task.isCancelled {
                throw ImportError.cancelled
            }

            // Update progress
            let progress = Double(index) / Double(results.count)
            await updateProgress(progress, message: "Processing photo \(index + 1) of \(results.count)")

            do {
                // Load image from picker result
                let (image, metadata) = try await loadImage(from: result)

                // Check for duplicates
                let imageData = image.jpegData(compressionQuality: 1.0) ?? Data()
                let hash = duplicateDetector.calculateHash(for: imageData)

                if importedHashes.contains(hash) {
                    skippedDuplicates.append(result.assetIdentifier ?? "Unknown")
                    logger.debug("Skipping duplicate photo with hash: \(hash)")
                    continue
                }

                // Optimize image
                let optimizedData = try await photoOptimizer.optimizeImage(
                    image,
                    maxDimension: Configuration.maxImageDimension,
                    quality: Configuration.jpegQuality
                )

                // Generate thumbnail
                let thumbnailData = try await photoOptimizer.generateThumbnail(
                    from: image,
                    targetSize: Configuration.thumbnailSize,
                    quality: Configuration.thumbnailQuality
                )

                // Save to storage
                let filename = generateFileName(from: metadata)
                let storageResult = try await saveToStorage(
                    photoData: optimizedData,
                    thumbnailData: thumbnailData,
                    filename: filename
                )

                // Create Photo entity
                let photo = Photo(
                    path: storageResult.photoPath,
                    categoryId: categoryId,
                    name: metadata.originalFilename ?? filename,
                    isFromAssets: false,
                    createdAt: Int64((metadata.creationDate ?? Date()).timeIntervalSince1970 * 1000),
                    fileSize: Int64(storageResult.fileSize),
                    width: metadata.pixelWidth,
                    height: metadata.pixelHeight
                )

                // Save to repository
                _ = try await photoRepository.insertPhoto(photo)

                // Track success
                successfulImports.append(photo)
                importedHashes.insert(hash)
                processedCount += 1

            } catch {
                logger.error("Failed to import photo at index \(index): \(error.localizedDescription)")
                failedItems.append((index: index, error: error))
            }

            // Small delay between photos to prevent memory pressure
            if index < results.count - 1 {
                try await Task.sleep(nanoseconds: Configuration.processingDelayMs)
            }
        }

        // Final progress update
        await updateProgress(1.0, message: "Import complete")

        let result = PhotoManagerImportResult(
            totalCount: results.count,
            successCount: successfulImports.count,
            failedCount: failedItems.count,
            skippedDuplicates: skippedDuplicates.count,
            importedPhotos: successfulImports,
            errors: failedItems.map { $0.error }
        )

        logger.info("Import completed: \(result.successCount) successful, \(result.failedCount) failed, \(result.skippedDuplicates) duplicates")

        return result
    }

    private func loadImage(from result: PHPickerResult) async throws -> (UIImage, PhotoMetadataExtractor.PhotoMetadata) {
        return try await withCheckedThrowingContinuation { continuation in
            result.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] object, error in
                guard let self = self else {
                    continuation.resume(throwing: ImportError.managerDeallocated)
                    return
                }

                if let error = error {
                    continuation.resume(throwing: ImportError.loadingFailed(error.localizedDescription))
                    return
                }

                guard let image = object as? UIImage else {
                    continuation.resume(throwing: ImportError.invalidImageData)
                    return
                }

                // Extract metadata
                let metadata = self.metadataExtractor.extractMetadata(
                    from: result,
                    image: image
                )

                continuation.resume(returning: (image, metadata))
            }
        }
    }

    private func saveToStorage(
        photoData: Data,
        thumbnailData: Data,
        filename: String
    ) async throws -> StorageResult {
        // Save photo
        let photosDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: photosDir, withIntermediateDirectories: true)

        let photoURL = photosDir.appendingPathComponent(filename)
        try photoData.write(to: photoURL)

        // Save thumbnail
        let thumbnailsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("thumbnails", isDirectory: true)
        try FileManager.default.createDirectory(at: thumbnailsDir, withIntermediateDirectories: true)

        let thumbnailURL = thumbnailsDir.appendingPathComponent("thumb_\(filename)")
        try thumbnailData.write(to: thumbnailURL)

        return StorageResult(
            photoPath: photoURL.path,
            thumbnailPath: thumbnailURL.path,
            fileName: filename,
            fileSize: Int64(photoData.count)
        )
    }

    private func generateFileName(from metadata: PhotoMetadataExtractor.PhotoMetadata) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: metadata.creationDate ?? Date())
        let uuid = UUID().uuidString.prefix(8)
        let ext = metadata.fileExtension ?? "jpg"
        return "IMG_\(timestamp)_\(uuid).\(ext.lowercased())"
    }

    private func updateProgress(_ progress: Double, message: String) async {
        await MainActor.run {
            self.importProgress = progress
            self.importMessage = message
            self.state = .processing(progress: progress, message: message)
        }
    }

    private func loadExistingPhotoHashes() async {
        do {
            let photos = try await photoRepository.getAllPhotos()
            for photo in photos {
                if let data = try? Data(contentsOf: URL(fileURLWithPath: photo.path)) {
                    let hash = duplicateDetector.calculateHash(for: data)
                    importedHashes.insert(hash)
                }
            }
            logger.debug("Loaded \(importedHashes.count) existing photo hashes")
        } catch {
            logger.error("Failed to load existing photo hashes: \(error.localizedDescription)")
        }
    }
}

// MARK: - Import Result

struct PhotoManagerImportResult {
    let totalCount: Int
    let successCount: Int
    let failedCount: Int
    let skippedDuplicates: Int
    let importedPhotos: [Photo]
    let errors: [Error]

    var successRate: Double {
        guard totalCount > 0 else { return 0 }
        return Double(successCount) / Double(totalCount)
    }

    var summary: String {
        var parts: [String] = []

        if successCount > 0 {
            parts.append("\(successCount) imported")
        }

        if skippedDuplicates > 0 {
            parts.append("\(skippedDuplicates) duplicates skipped")
        }

        if failedCount > 0 {
            parts.append("\(failedCount) failed")
        }

        return parts.joined(separator: ", ")
    }
}

// MARK: - Import Errors

enum ImportError: LocalizedError {
    case importInProgress
    case permissionDenied
    case permissionRestricted
    case batchSizeLimitExceeded(limit: Int)
    case loadingFailed(String)
    case invalidImageData
    case invalidCategory(String)
    case unsupportedFormat(String)
    case processingFailed(String)
    case storageFull
    case cancelled
    case managerDeallocated

    var errorDescription: String? {
        switch self {
        case .importInProgress:
            return "An import is already in progress"
        case .permissionDenied:
            return "Photo library access denied. Please enable in Settings."
        case .permissionRestricted:
            return "Photo library access is restricted on this device"
        case .batchSizeLimitExceeded(let limit):
            return "Please select up to \(limit) photos at a time"
        case .loadingFailed(let reason):
            return "Failed to load photo: \(reason)"
        case .invalidImageData:
            return "Invalid or corrupted image data"
        case .invalidCategory(let reason):
            return "Invalid category: \(reason)"
        case .unsupportedFormat(let format):
            return "Unsupported image format: \(format)"
        case .processingFailed(let reason):
            return "Failed to process photo: \(reason)"
        case .storageFull:
            return "Not enough storage space available"
        case .cancelled:
            return "Import was cancelled"
        case .managerDeallocated:
            return "Import manager was deallocated"
        }
    }
}