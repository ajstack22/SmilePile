import UIKit
import Photos
import PhotosUI
import CoreImage
import CryptoKit
import ImageIO
import CoreLocation

/// Handles photo asset processing with memory-safe loading and caching
class PhotoAssetProcessor: ObservableObject {

    // MARK: - Configuration
    struct Configuration {
        static let maxImageDimension: CGFloat = 2048  // Max dimension for full images - matches Android
        static let thumbnailSize: CGFloat = 300       // Thumbnail size - matches Android (300px)
        static let jpegQuality: CGFloat = 0.90        // JPEG compression quality - matches Android (90%)
        static let thumbnailQuality: CGFloat = 0.85   // Thumbnail quality - matches Android (85%)
        static let maxMemoryCacheSizeMB: Int = 50     // Max memory cache in MB
        static let batchSize: Int = 10                // Process photos in batches
        static let maxConcurrentLoads: Int = 3        // Max concurrent image loads
        static let maxPhotosPerBatch: Int = 50        // Maximum photos per import batch
    }

    // MARK: - Error Types
    enum ProcessingError: LocalizedError {
        case invalidAsset
        case loadingFailed
        case insufficientMemory
        case unsupportedFormat
        case iCloudDownloadRequired
        case processingTimeout
        case cancelled

        var errorDescription: String? {
            switch self {
            case .invalidAsset:
                return "Invalid photo asset"
            case .loadingFailed:
                return "Failed to load photo"
            case .insufficientMemory:
                return "Insufficient memory to process photo"
            case .unsupportedFormat:
                return "Unsupported photo format"
            case .iCloudDownloadRequired:
                return "Photo needs to be downloaded from iCloud"
            case .processingTimeout:
                return "Photo processing timed out"
            case .cancelled:
                return "Processing was cancelled"
            }
        }
    }

    // MARK: - Properties
    private let imageManager = PHImageManager.default()
    private let documentDirectory: URL
    private let thumbnailDirectory: URL
    private var activeRequests: [PHImageRequestID] = []
    private let processingQueue = DispatchQueue(label: "com.smilepile.photoprocessing", qos: .userInitiated)
    private let semaphore: DispatchSemaphore
    private var importedHashes = Set<String>()

    // Memory monitoring
    private var memoryWarningObserver: NSObjectProtocol?

    // MARK: - Initialization
    init() {
        // Setup document directory for photos - store in root Documents folder
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.documentDirectory = documentsURL

        // Setup thumbnail directory
        self.thumbnailDirectory = documentsURL.appendingPathComponent("thumbnails", isDirectory: true)

        // Create directories if needed
        try? FileManager.default.createDirectory(at: thumbnailDirectory, withIntermediateDirectories: true)

        // Setup concurrent load limiting
        self.semaphore = DispatchSemaphore(value: Configuration.maxConcurrentLoads)

        // Monitor memory warnings
        setupMemoryWarningObserver()
    }

    deinit {
        cancelAllRequests()
        if let observer = memoryWarningObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    // MARK: - Public Methods

    /// Process PHPickerResults into Photos with memory-safe loading
    func processPickerResults(
        _ results: [PHPickerResult],
        categoryId: Int64,
        progressHandler: ((Double) -> Void)? = nil
    ) async throws -> [Photo] {
        var processedPhotos: [Photo] = []
        let totalCount = results.count
        var processedCount = 0

        // Process in batches to manage memory
        for batchStart in stride(from: 0, to: results.count, by: Configuration.batchSize) {
            let batchEnd = min(batchStart + Configuration.batchSize, results.count)
            let batch = Array(results[batchStart..<batchEnd])

            let batchPhotos = try await processBatch(batch, categoryId: categoryId)
            processedPhotos.append(contentsOf: batchPhotos)

            processedCount += batch.count
            let progress = Double(processedCount) / Double(totalCount)
            await MainActor.run {
                progressHandler?(progress)
            }

            // Small delay between batches to prevent memory pressure
            if batchEnd < results.count {
                try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
            }
        }

        return processedPhotos
    }

    /// Process a single PHAsset into a Photo
    func processAsset(
        _ asset: PHAsset,
        categoryId: Int64
    ) async throws -> Photo {
        // Check if asset is in iCloud and needs download
        if !asset.isLocallyAvailable {
            throw ProcessingError.iCloudDownloadRequired
        }

        // Load and process image
        let imageData = try await loadImageData(from: asset)
        let processedPath = try await saveProcessedImage(imageData, asset: asset)

        // Extract metadata
        let metadata = extractMetadata(from: asset, imageData: imageData)

        return Photo(
            path: processedPath,
            categoryId: categoryId,
            name: asset.getFileName() ?? "",
            isFromAssets: false,
            createdAt: Int64((asset.creationDate ?? Date()).timeIntervalSince1970 * 1000),
            fileSize: Int64(imageData.count),
            width: metadata.width,
            height: metadata.height
        )
    }

    /// Generate optimized thumbnail for a photo and save to thumbnails directory
    func generateThumbnail(for photo: Photo, size: CGFloat = Configuration.thumbnailSize) async throws -> UIImage {
        let fileURL = URL(fileURLWithPath: photo.path)

        // Try to load from file
        guard let imageData = try? Data(contentsOf: fileURL),
              let image = UIImage(data: imageData) else {
            throw ProcessingError.loadingFailed
        }

        // Generate thumbnail
        let thumbnailSize = CGSize(width: size, height: size)
        let thumbnail = try await generateThumbnail(from: image, targetSize: thumbnailSize)

        // Save thumbnail to thumbnails directory
        let thumbnailFilename = "thumb_\(photo.id).jpg"
        let thumbnailURL = thumbnailDirectory.appendingPathComponent(thumbnailFilename)
        if let thumbnailData = thumbnail.jpegData(compressionQuality: Configuration.thumbnailQuality) {
            try? thumbnailData.write(to: thumbnailURL)
        }

        return thumbnail
    }

    // MARK: - Private Methods

    private func processBatch(
        _ results: [PHPickerResult],
        categoryId: Int64
    ) async throws -> [Photo] {
        var photos: [Photo] = []

        for result in results {
            do {
                semaphore.wait()
                defer { semaphore.signal() }

                if result.itemProvider.canLoadObject(ofClass: UIImage.self) {
                    let photo = try await processPickerResult(result, categoryId: categoryId)
                    photos.append(photo)
                }
            } catch {
                // Log error but continue processing other photos
                print("Failed to process photo: \(error)")
                continue
            }
        }

        return photos
    }

    private func processPickerResult(
        _ result: PHPickerResult,
        categoryId: Int64
    ) async throws -> Photo {
        return try await withCheckedThrowingContinuation { continuation in
            result.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] (object, error) in
                guard let self = self else {
                    continuation.resume(throwing: ProcessingError.cancelled)
                    return
                }

                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let image = object as? UIImage else {
                    continuation.resume(throwing: ProcessingError.invalidAsset)
                    return
                }

                Task {
                    do {
                        let processedPhoto = try await self.processImage(image, categoryId: categoryId, identifier: result.assetIdentifier)
                        continuation.resume(returning: processedPhoto)
                    } catch {
                        continuation.resume(throwing: error)
                    }
                }
            }
        }
    }

    private func processImage(
        _ image: UIImage,
        categoryId: Int64,
        identifier: String?
    ) async throws -> Photo {
        // Check for duplicates first
        guard let originalData = image.jpegData(compressionQuality: 1.0) else {
            throw ProcessingError.invalidAsset
        }

        let hash = calculateHash(for: originalData)
        if importedHashes.contains(hash) {
            throw ProcessingError.cancelled // Skip duplicates
        }

        // Resize if needed to manage memory
        let resizedImage = try await resizeImageIfNeeded(image)

        // Optimize image with higher quality for main photo
        guard let imageData = resizedImage.jpegData(compressionQuality: Configuration.jpegQuality) else {
            throw ProcessingError.processingTimeout
        }

        // Generate unique filename with timestamp
        let filename = generateFilename(identifier: identifier)
        let fileURL = documentDirectory.appendingPathComponent(filename)

        // Save to disk
        try imageData.write(to: fileURL)

        // Mark as imported
        importedHashes.insert(hash)

        // Extract metadata
        let metadata = extractMetadata(from: image, identifier: identifier)

        // Create Photo object with enhanced metadata
        return Photo(
            path: fileURL.path,
            categoryId: categoryId,
            name: identifier ?? filename,
            isFromAssets: false,
            createdAt: Int64((metadata.creationDate ?? Date()).timeIntervalSince1970 * 1000),
            fileSize: Int64(imageData.count),
            width: metadata.width,
            height: metadata.height
        )
    }

    // MARK: - Helper Methods

    private func calculateHash(for data: Data) -> String {
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }

    private func generateFilename(identifier: String?) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = dateFormatter.string(from: Date())
        let uuid = UUID().uuidString.prefix(8)

        if let identifier = identifier,
           let ext = identifier.split(separator: ".").last {
            return "IMG_\(timestamp)_\(uuid).\(ext)"
        }
        return "IMG_\(timestamp)_\(uuid).jpg"
    }

    private func extractMetadata(from image: UIImage, identifier: String?) -> (creationDate: Date?, width: Int, height: Int) {
        let width = Int(image.size.width * image.scale)
        let height = Int(image.size.height * image.scale)

        // Try to get PHAsset for more metadata
        if let identifier = identifier {
            let fetchResult = PHAsset.fetchAssets(withLocalIdentifiers: [identifier], options: nil)
            if let asset = fetchResult.firstObject {
                return (
                    creationDate: asset.creationDate,
                    width: asset.pixelWidth,
                    height: asset.pixelHeight
                )
            }
        }

        return (creationDate: Date(), width: width, height: height)
    }

    private func loadImageData(from asset: PHAsset) async throws -> Data {
        let options = PHImageRequestOptions()
        options.version = .current
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true
        options.isSynchronous = false

        return try await withCheckedThrowingContinuation { continuation in
            let requestID = imageManager.requestImageDataAndOrientation(for: asset, options: options) { (data, _, _, info) in
                if let error = info?[PHImageErrorKey] as? Error {
                    continuation.resume(throwing: error)
                } else if let data = data {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(throwing: ProcessingError.loadingFailed)
                }
            }
            self.activeRequests.append(requestID)
        }
    }

    private func saveProcessedImage(_ imageData: Data, asset: PHAsset) async throws -> String {
        let filename = "\(asset.localIdentifier.replacingOccurrences(of: "/", with: "_")).jpg"
        let fileURL = documentDirectory.appendingPathComponent(filename)
        try imageData.write(to: fileURL)
        return fileURL.path
    }

    private func resizeImageIfNeeded(_ image: UIImage) async throws -> UIImage {
        let maxDimension = max(image.size.width, image.size.height)

        if maxDimension <= Configuration.maxImageDimension {
            return image
        }

        let scale = Configuration.maxImageDimension / maxDimension
        let newSize = CGSize(
            width: image.size.width * scale,
            height: image.size.height * scale
        )

        // Properly resize image with correct context options to reduce memory
        return try await resizeImage(from: image, targetSize: newSize)
    }

    private func generateThumbnail(from image: UIImage, targetSize: CGSize) async throws -> UIImage {
        return try await resizeImage(from: image, targetSize: targetSize)
    }

    private func resizeImage(from image: UIImage, targetSize: CGSize) async throws -> UIImage {
        return try await withCheckedThrowingContinuation { continuation in
            processingQueue.async {
                // Use lower scale factor to reduce memory usage
                let format = UIGraphicsImageRendererFormat()
                format.scale = 1.0  // Force scale to 1 to reduce memory

                let renderer = UIGraphicsImageRenderer(size: targetSize, format: format)
                let resized = renderer.image { context in
                    image.draw(in: CGRect(origin: .zero, size: targetSize))
                }

                continuation.resume(returning: resized)
            }
        }
    }

    private func extractMetadata(from asset: PHAsset, imageData: Data) -> (width: Int, height: Int) {
        return (width: Int(asset.pixelWidth), height: Int(asset.pixelHeight))
    }

    private func setupMemoryWarningObserver() {
        memoryWarningObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleMemoryWarning()
        }
    }

    private func handleMemoryWarning() {
        // Cancel non-critical operations but don't clear caches during memory warnings
        // Cache clearing can cause more memory pressure from rebuilding
        print("Memory warning received - cancelling pending requests")
        cancelAllRequests()

        // Clear imported hashes to free some memory
        importedHashes.removeAll(keepingCapacity: false)
    }

    private func cancelAllRequests() {
        for requestID in activeRequests {
            imageManager.cancelImageRequest(requestID)
        }
        activeRequests.removeAll()
    }
}

// MARK: - PHAsset Extensions

private extension PHAsset {
    var isLocallyAvailable: Bool {
        let resources = PHAssetResource.assetResources(for: self)
        return resources.first?.isLocallyAvailable ?? false
    }

    func getFileName() -> String? {
        let resources = PHAssetResource.assetResources(for: self)
        return resources.first?.originalFilename
    }
}

private extension PHAssetResource {
    var isLocallyAvailable: Bool {
        return true // Simplified - in production, check actual availability
    }
}