import Foundation
import ImageIO
import CoreGraphics
import UniformTypeIdentifiers
import os.log

/// Memory-safe thumbnail generator using ImageIO instead of UIGraphicsContext
/// Prevents memory crashes by processing images at the lowest possible level
final class SafeThumbnailGenerator {

    // MARK: - Configuration
    struct Configuration {
        static let thumbnailSize: CGFloat = 300
        static let maxImageSize: CGFloat = 2048
        static let jpegQuality: CGFloat = 0.85
        static let thumbnailQuality: CGFloat = 0.8
        static let maxMemoryUsageMB: Int = 100
        static let memoryCheckInterval: TimeInterval = 0.5
    }

    // MARK: - Error Types
    enum GeneratorError: LocalizedError {
        case invalidImageSource
        case memoryPressure
        case processingFailed
        case unsupportedFormat
        case fileNotFound
        case exceedsMaxSize

        var errorDescription: String? {
            switch self {
            case .invalidImageSource:
                return "Invalid image source or corrupted file"
            case .memoryPressure:
                return "System memory pressure detected - pausing processing"
            case .processingFailed:
                return "Failed to process image"
            case .unsupportedFormat:
                return "Unsupported image format"
            case .fileNotFound:
                return "Image file not found"
            case .exceedsMaxSize:
                return "Image exceeds maximum allowed size"
            }
        }
    }

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "SafeThumbnailGenerator")
    private var lastMemoryCheck = Date()
    private let processQueue = DispatchQueue(label: "com.smilepile.thumbnail", qos: .userInitiated)

    // Memory monitoring
    private var currentMemoryUsage: Int64 {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }

        return result == KERN_SUCCESS ? Int64(info.resident_size) : 0
    }

    private var memoryUsageMB: Int {
        return Int(currentMemoryUsage / (1024 * 1024))
    }

    // MARK: - Public Methods

    /// Generate thumbnail from image data using ImageIO (memory-safe)
    func generateThumbnail(from imageData: Data, targetSize: CGFloat = Configuration.thumbnailSize) async throws -> Data {
        // Check memory pressure
        try await checkMemoryPressure()

        return try await withCheckedThrowingContinuation { continuation in
            processQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(throwing: GeneratorError.processingFailed)
                    return
                }

                do {
                    let thumbnailData = try self.createThumbnailWithImageIO(
                        from: imageData,
                        maxPixelSize: targetSize
                    )
                    continuation.resume(returning: thumbnailData)
                } catch {
                    self.logger.error("Thumbnail generation failed: \(error.localizedDescription)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Generate thumbnail from file URL using ImageIO (memory-safe)
    func generateThumbnail(from fileURL: URL, targetSize: CGFloat = Configuration.thumbnailSize) async throws -> Data {
        // Check memory pressure
        try await checkMemoryPressure()

        // Check file exists
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            throw GeneratorError.fileNotFound
        }

        return try await withCheckedThrowingContinuation { continuation in
            processQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(throwing: GeneratorError.processingFailed)
                    return
                }

                do {
                    let thumbnailData = try self.createThumbnailFromFile(
                        at: fileURL,
                        maxPixelSize: targetSize
                    )
                    continuation.resume(returning: thumbnailData)
                } catch {
                    self.logger.error("Thumbnail generation from file failed: \(error.localizedDescription)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Process and resize image data for storage
    func processImageForStorage(imageData: Data, maxDimension: CGFloat = Configuration.maxImageSize) async throws -> Data {
        // Check memory pressure
        try await checkMemoryPressure()

        return try await withCheckedThrowingContinuation { continuation in
            processQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(throwing: GeneratorError.processingFailed)
                    return
                }

                do {
                    let processedData = try self.resizeImageWithImageIO(
                        from: imageData,
                        maxPixelSize: maxDimension
                    )
                    continuation.resume(returning: processedData)
                } catch {
                    self.logger.error("Image processing failed: \(error.localizedDescription)")
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    // MARK: - Sequential Batch Processing

    /// Process thumbnails sequentially to prevent memory overload
    func generateThumbnailsSequentially(
        for imageDatas: [(data: Data, identifier: String)],
        targetSize: CGFloat = Configuration.thumbnailSize,
        progressHandler: ((Double) -> Void)? = nil
    ) async throws -> [(identifier: String, thumbnailData: Data)] {
        var results: [(identifier: String, thumbnailData: Data)] = []
        let total = imageDatas.count

        for (index, item) in imageDatas.enumerated() {
            // Check memory before each thumbnail
            try await checkMemoryPressure()

            do {
                let thumbnailData = try await generateThumbnail(from: item.data, targetSize: targetSize)
                results.append((identifier: item.identifier, thumbnailData: thumbnailData))

                // Report progress
                let progress = Double(index + 1) / Double(total)
                await MainActor.run {
                    progressHandler?(progress)
                }

                logger.debug("Generated thumbnail \(index + 1)/\(total) for \(item.identifier)")
            } catch {
                logger.error("Failed to generate thumbnail for \(item.identifier): \(error.localizedDescription)")
                // Continue with next thumbnail
            }

            // Small delay between thumbnails to prevent CPU overload
            if index < imageDatas.count - 1 {
                try await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }

        return results
    }

    // MARK: - Private ImageIO Methods

    private func createThumbnailWithImageIO(from imageData: Data, maxPixelSize: CGFloat) throws -> Data {
        guard let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) else {
            throw GeneratorError.invalidImageSource
        }

        let thumbnailOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
        ]

        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, thumbnailOptions as CFDictionary) else {
            throw GeneratorError.processingFailed
        }

        // Convert CGImage to JPEG data
        let jpegData = try createJPEGData(from: cgImage, quality: Configuration.thumbnailQuality)

        return jpegData
    }

    private func createThumbnailFromFile(at fileURL: URL, maxPixelSize: CGFloat) throws -> Data {
        guard let imageSource = CGImageSourceCreateWithURL(fileURL as CFURL, nil) else {
            throw GeneratorError.invalidImageSource
        }

        let thumbnailOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
        ]

        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, thumbnailOptions as CFDictionary) else {
            throw GeneratorError.processingFailed
        }

        // Convert CGImage to JPEG data
        let jpegData = try createJPEGData(from: cgImage, quality: Configuration.thumbnailQuality)

        return jpegData
    }

    private func resizeImageWithImageIO(from imageData: Data, maxPixelSize: CGFloat) throws -> Data {
        guard let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) else {
            throw GeneratorError.invalidImageSource
        }

        // Get original image properties
        guard let properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? CGFloat,
              let height = properties[kCGImagePropertyPixelHeight] as? CGFloat else {
            throw GeneratorError.invalidImageSource
        }

        // Check if resizing is needed
        let maxDimension = max(width, height)
        if maxDimension <= maxPixelSize {
            return imageData // No resizing needed
        }

        // Calculate new size
        let scale = maxPixelSize / maxDimension
        let newWidth = width * scale
        let newHeight = height * scale

        let resizeOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
        ]

        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, resizeOptions as CFDictionary) else {
            throw GeneratorError.processingFailed
        }

        // Convert CGImage to JPEG data
        let jpegData = try createJPEGData(from: cgImage, quality: Configuration.jpegQuality)

        logger.debug("Resized image from \(width)x\(height) to \(newWidth)x\(newHeight)")

        return jpegData
    }

    private func createJPEGData(from cgImage: CGImage, quality: CGFloat) throws -> Data {
        let data = NSMutableData()

        guard let destination = CGImageDestinationCreateWithData(
            data as CFMutableData,
            UTType.jpeg.identifier as CFString,
            1,
            nil
        ) else {
            throw GeneratorError.processingFailed
        }

        let options: [CFString: Any] = [
            kCGImageDestinationLossyCompressionQuality: quality
        ]

        CGImageDestinationAddImage(destination, cgImage, options as CFDictionary)

        guard CGImageDestinationFinalize(destination) else {
            throw GeneratorError.processingFailed
        }

        return data as Data
    }

    // MARK: - Memory Management

    private func checkMemoryPressure() async throws {
        // Check memory usage
        if self.memoryUsageMB > Configuration.maxMemoryUsageMB {
            logger.warning("Memory usage high: \(self.memoryUsageMB)MB")

            // Wait for memory to be available
            try await Task.sleep(nanoseconds: 500_000_000) // 500ms

            // Check again
            if self.memoryUsageMB > Configuration.maxMemoryUsageMB {
                throw GeneratorError.memoryPressure
            }
        }

        // Update last check time
        lastMemoryCheck = Date()
    }

    /// Get current memory usage in MB
    func getCurrentMemoryUsage() -> Int {
        return memoryUsageMB
    }

    /// Check if it's safe to process
    func isSafeToProcess() -> Bool {
        return memoryUsageMB < Configuration.maxMemoryUsageMB
    }

    // MARK: - Image Metadata

    func getImageMetadata(from imageData: Data) -> ImageMetadata? {
        guard let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) else {
            return nil
        }

        guard let properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, nil) as? [CFString: Any] else {
            return nil
        }

        let width = properties[kCGImagePropertyPixelWidth] as? Int ?? 0
        let height = properties[kCGImagePropertyPixelHeight] as? Int ?? 0
        let orientation = properties[kCGImagePropertyOrientation] as? Int ?? 1

        return ImageMetadata(
            width: width,
            height: height,
            orientation: orientation,
            fileSize: imageData.count
        )
    }
}

// MARK: - Supporting Types

struct ImageMetadata {
    let width: Int
    let height: Int
    let orientation: Int
    let fileSize: Int
}