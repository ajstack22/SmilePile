import UIKit
import CoreImage
import ImageIO
import UniformTypeIdentifiers

/// Optimizes photos for storage by resizing and compressing
final class PhotoOptimizer {

    // MARK: - Configuration
    struct Configuration {
        static let defaultMaxDimension: CGFloat = 2048
        static let defaultJPEGQuality: CGFloat = 0.9
        static let thumbnailSize: CGFloat = 300
        static let thumbnailQuality: CGFloat = 0.85
        static let maxMemoryUsageMB: Int = 100
        static let heicCompressionQuality: CGFloat = 0.8
    }

    // MARK: - Error Types
    enum OptimizationError: LocalizedError {
        case invalidImage
        case compressionFailed
        case resizingFailed
        case memoryPressure
        case unsupportedFormat

        var errorDescription: String? {
            switch self {
            case .invalidImage:
                return "Invalid or corrupted image"
            case .compressionFailed:
                return "Failed to compress image"
            case .resizingFailed:
                return "Failed to resize image"
            case .memoryPressure:
                return "Insufficient memory for image processing"
            case .unsupportedFormat:
                return "Unsupported image format"
            }
        }
    }

    // MARK: - Properties
    private let ciContext: CIContext
    private let processingQueue: DispatchQueue

    // MARK: - Initialization
    init() {
        // Create Core Image context for image processing
        self.ciContext = CIContext(options: [
            .useSoftwareRenderer: false,
            .highQualityDownsample: true,
            .cacheIntermediates: false
        ])

        // Create dedicated processing queue
        self.processingQueue = DispatchQueue(
            label: "com.smilepile.photooptimizer",
            qos: .userInitiated,
            attributes: .concurrent
        )
    }

    // MARK: - Public Methods

    /// Optimize image with resizing and compression
    func optimizeImage(
        _ image: UIImage,
        maxDimension: CGFloat = Configuration.defaultMaxDimension,
        quality: CGFloat = Configuration.defaultJPEGQuality,
        format: ImageFormat = .jpeg
    ) async throws -> Data {
        return try await withCheckedThrowingContinuation { continuation in
            processingQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(throwing: OptimizationError.memoryPressure)
                    return
                }

                do {
                    // Check if resizing is needed
                    let resizedImage: UIImage
                    if max(image.size.width, image.size.height) > maxDimension {
                        resizedImage = try self.resizeImage(
                            image,
                            maxDimension: maxDimension,
                            preserveAspectRatio: true
                        )
                    } else {
                        resizedImage = image
                    }

                    // Compress image
                    let compressedData = try self.compressImage(
                        resizedImage,
                        quality: quality,
                        format: format
                    )

                    continuation.resume(returning: compressedData)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Generate optimized thumbnail
    func generateThumbnail(
        from image: UIImage,
        targetSize: CGFloat = Configuration.thumbnailSize,
        quality: CGFloat = Configuration.thumbnailQuality
    ) async throws -> Data {
        return try await withCheckedThrowingContinuation { continuation in
            processingQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(throwing: OptimizationError.memoryPressure)
                    return
                }

                do {
                    // Calculate thumbnail dimensions maintaining aspect ratio
                    let thumbnailSize = self.calculateThumbnailSize(
                        originalSize: image.size,
                        targetDimension: targetSize
                    )

                    // Generate thumbnail using Core Graphics for efficiency
                    let thumbnail = try self.createThumbnail(
                        from: image,
                        targetSize: thumbnailSize
                    )

                    // Compress thumbnail as JPEG
                    guard let thumbnailData = thumbnail.jpegData(compressionQuality: quality) else {
                        throw OptimizationError.compressionFailed
                    }

                    continuation.resume(returning: thumbnailData)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Optimize image from data
    func optimizeImageData(
        _ imageData: Data,
        maxDimension: CGFloat = Configuration.defaultMaxDimension,
        quality: CGFloat = Configuration.defaultJPEGQuality
    ) async throws -> Data {
        guard let image = UIImage(data: imageData) else {
            throw OptimizationError.invalidImage
        }

        return try await optimizeImage(image, maxDimension: maxDimension, quality: quality)
    }

    /// Smart compression based on image characteristics
    func smartCompress(_ image: UIImage) async throws -> Data {
        // Analyze image characteristics
        let analysis = analyzeImage(image)

        // Determine optimal settings based on analysis
        let quality: CGFloat
        let maxDimension: CGFloat

        switch analysis.complexity {
        case .low:
            // Simple images can use higher compression
            quality = 0.7
            maxDimension = 1920
        case .medium:
            quality = 0.85
            maxDimension = 2048
        case .high:
            // Complex images need less compression
            quality = 0.95
            maxDimension = 2560
        }

        // Check if image has transparency
        let format: ImageFormat = analysis.hasAlpha ? .png : .jpeg

        return try await optimizeImage(
            image,
            maxDimension: maxDimension,
            quality: quality,
            format: format
        )
    }

    // MARK: - Private Methods

    private func resizeImage(
        _ image: UIImage,
        maxDimension: CGFloat,
        preserveAspectRatio: Bool
    ) throws -> UIImage {
        let originalSize = image.size
        let scale: CGFloat

        if preserveAspectRatio {
            let maxOriginalDimension = max(originalSize.width, originalSize.height)
            scale = maxDimension / maxOriginalDimension
        } else {
            scale = maxDimension / min(originalSize.width, originalSize.height)
        }

        let newSize = CGSize(
            width: originalSize.width * scale,
            height: originalSize.height * scale
        )

        // Use Core Graphics for efficient resizing
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: CGRect(origin: .zero, size: newSize))

        guard let resizedImage = UIGraphicsGetImageFromCurrentImageContext() else {
            throw OptimizationError.resizingFailed
        }

        return resizedImage
    }

    private func createThumbnail(from image: UIImage, targetSize: CGSize) throws -> UIImage {
        // Use ImageIO for efficient thumbnail generation
        guard let imageData = image.jpegData(compressionQuality: 1.0),
              let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) else {
            throw OptimizationError.invalidImage
        }

        let options: [CFString: Any] = [
            kCGImageSourceThumbnailMaxPixelSize: max(targetSize.width, targetSize.height),
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true
        ]

        guard let thumbnailRef = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, options as CFDictionary) else {
            throw OptimizationError.resizingFailed
        }

        return UIImage(cgImage: thumbnailRef)
    }

    private func compressImage(
        _ image: UIImage,
        quality: CGFloat,
        format: ImageFormat
    ) throws -> Data {
        let imageData: Data?

        switch format {
        case .jpeg:
            imageData = image.jpegData(compressionQuality: quality)
        case .png:
            imageData = image.pngData()
        case .heic:
            if #available(iOS 17.0, *) {
                imageData = image.heicData(compressionQuality: Configuration.heicCompressionQuality)
            } else {
                // Fallback to JPEG for older iOS versions
                imageData = image.jpegData(compressionQuality: quality)
            }
        }

        guard let data = imageData else {
            throw OptimizationError.compressionFailed
        }

        return data
    }

    private func calculateThumbnailSize(
        originalSize: CGSize,
        targetDimension: CGFloat
    ) -> CGSize {
        let aspectRatio = originalSize.width / originalSize.height
        var newSize = CGSize.zero

        if aspectRatio > 1 {
            // Landscape
            newSize.width = targetDimension
            newSize.height = targetDimension / aspectRatio
        } else {
            // Portrait or square
            newSize.height = targetDimension
            newSize.width = targetDimension * aspectRatio
        }

        return newSize
    }

    private func analyzeImage(_ image: UIImage) -> ImageAnalysis {
        // Simple analysis based on image characteristics
        let pixelCount = image.size.width * image.size.height
        let hasAlpha = image.cgImage?.alphaInfo != .none

        let complexity: ImageComplexity
        if pixelCount < 1_000_000 {
            complexity = .low
        } else if pixelCount < 4_000_000 {
            complexity = .medium
        } else {
            complexity = .high
        }

        return ImageAnalysis(
            complexity: complexity,
            hasAlpha: hasAlpha,
            pixelCount: Int(pixelCount)
        )
    }

    // MARK: - Supporting Types

    enum ImageFormat {
        case jpeg
        case png
        case heic
    }

    enum ImageComplexity {
        case low
        case medium
        case high
    }

    struct ImageAnalysis {
        let complexity: ImageComplexity
        let hasAlpha: Bool
        let pixelCount: Int
    }
}

// MARK: - UIImage Extensions

@available(iOS 17.0, *)
extension UIImage {
    func heicData(compressionQuality: CGFloat) -> Data? {
        guard let cgImage = self.cgImage else { return nil }

        let options: [CFString: Any] = [
            kCGImageDestinationLossyCompressionQuality: compressionQuality
        ]

        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            data as CFMutableData,
            UTType.heic.identifier as CFString,
            1,
            nil
        ) else {
            return nil
        }

        CGImageDestinationAddImage(destination, cgImage, options as CFDictionary)
        CGImageDestinationFinalize(destination)

        return data as Data
    }
}