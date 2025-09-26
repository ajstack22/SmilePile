import UIKit
import CoreImage
import os.log

enum ImageProcessingError: Error {
    case resizeFailed
    case compressionFailed
    case invalidImage
}

final class ImageProcessor {
    private let logger = Logger(subsystem: "com.smilepile", category: "ImageProcessor")

    private let maxPhotoSize: CGFloat = 2048
    private let photoQuality: CGFloat = 0.90
    private let thumbnailSize: CGFloat = 300
    private let thumbnailQuality: CGFloat = 0.85

    // MARK: - Photo Processing

    func processPhoto(_ image: UIImage) -> (processedImage: UIImage, data: Data)? {
        guard let resizedImage = resizeImage(image, maxDimension: maxPhotoSize) else {
            logger.error("Failed to resize photo")
            return nil
        }

        guard let imageData = resizedImage.jpegData(compressionQuality: photoQuality) else {
            logger.error("Failed to compress photo to JPEG")
            return nil
        }

        return (resizedImage, imageData)
    }

    func processUIImage(_ image: UIImage, maxSize: CGFloat? = nil) async throws -> Data {
        let targetSize = maxSize ?? maxPhotoSize

        guard let resizedImage = resizeImage(image, maxDimension: targetSize) else {
            throw ImageProcessingError.resizeFailed
        }

        guard let imageData = resizedImage.jpegData(compressionQuality: photoQuality) else {
            throw ImageProcessingError.compressionFailed
        }

        return imageData
    }

    // MARK: - Thumbnail Generation

    func generateThumbnail(from image: UIImage) -> Data? {
        guard let thumbnailImage = resizeImage(image, maxDimension: thumbnailSize, aspectFill: true) else {
            logger.error("Failed to create thumbnail image")
            return nil
        }

        guard let thumbnailData = thumbnailImage.jpegData(compressionQuality: thumbnailQuality) else {
            logger.error("Failed to compress thumbnail to JPEG")
            return nil
        }

        return thumbnailData
    }

    func generateThumbnail(from imageData: Data) -> Data? {
        guard let image = UIImage(data: imageData) else {
            logger.error("Failed to create UIImage from data")
            return nil
        }

        return generateThumbnail(from: image)
    }

    // MARK: - Image Information

    func getImageDimensions(from imageData: Data) -> (width: Int, height: Int)? {
        guard let image = UIImage(data: imageData) else {
            return nil
        }

        return (Int(image.size.width * image.scale), Int(image.size.height * image.scale))
    }

    func getImageDimensions(at url: URL) -> (width: Int, height: Int)? {
        guard let imageData = try? Data(contentsOf: url) else {
            return nil
        }

        return getImageDimensions(from: imageData)
    }

    // MARK: - Private Helpers

    private func resizeImage(_ image: UIImage, maxDimension: CGFloat, aspectFill: Bool = false) -> UIImage? {
        let originalSize = image.size

        // Calculate new size maintaining aspect ratio
        let ratio = aspectFill ?
            max(maxDimension / originalSize.width, maxDimension / originalSize.height) :
            min(maxDimension / originalSize.width, maxDimension / originalSize.height)

        // Don't scale up images smaller than max dimension
        let scalingRatio = min(ratio, 1.0)

        let newSize = CGSize(
            width: originalSize.width * scalingRatio,
            height: originalSize.height * scalingRatio
        )

        // Create graphics context
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        defer { UIGraphicsEndImageContext() }

        // Draw resized image
        image.draw(in: CGRect(origin: .zero, size: newSize))

        // Get resized image
        guard let resizedImage = UIGraphicsGetImageFromCurrentImageContext() else {
            logger.error("Failed to get resized image from context")
            return nil
        }

        // For aspect fill thumbnails, crop to square
        if aspectFill && newSize.width != newSize.height {
            return cropToSquare(resizedImage, targetSize: maxDimension)
        }

        return resizedImage
    }

    private func cropToSquare(_ image: UIImage, targetSize: CGFloat) -> UIImage? {
        let size = image.size
        let minDimension = min(size.width, size.height)

        let cropRect = CGRect(
            x: (size.width - minDimension) / 2,
            y: (size.height - minDimension) / 2,
            width: minDimension,
            height: minDimension
        )

        guard let cgImage = image.cgImage?.cropping(to: cropRect) else {
            return nil
        }

        let croppedImage = UIImage(cgImage: cgImage)

        // Resize to exact target size if needed
        if minDimension != targetSize {
            UIGraphicsBeginImageContextWithOptions(CGSize(width: targetSize, height: targetSize), false, 1.0)
            defer { UIGraphicsEndImageContext() }

            croppedImage.draw(in: CGRect(x: 0, y: 0, width: targetSize, height: targetSize))
            return UIGraphicsGetImageFromCurrentImageContext()
        }

        return croppedImage
    }

    // MARK: - Rotation

    func rotateImage(_ image: UIImage, degrees: CGFloat) -> UIImage? {
        // Calculate radians
        let radians = degrees * CGFloat.pi / 180

        // Calculate new size after rotation
        var newSize = CGRect(origin: .zero, size: image.size)
            .applying(CGAffineTransform(rotationAngle: radians))
            .size

        // Ensure positive dimensions
        newSize.width = abs(newSize.width)
        newSize.height = abs(newSize.height)

        // Create context
        UIGraphicsBeginImageContextWithOptions(newSize, false, image.scale)
        defer { UIGraphicsEndImageContext() }

        guard let context = UIGraphicsGetCurrentContext() else {
            logger.error("Failed to get graphics context for rotation")
            return nil
        }

        // Move to center, rotate, then draw
        context.translateBy(x: newSize.width / 2, y: newSize.height / 2)
        context.rotate(by: radians)

        // Draw image centered at origin
        image.draw(in: CGRect(
            x: -image.size.width / 2,
            y: -image.size.height / 2,
            width: image.size.width,
            height: image.size.height
        ))

        return UIGraphicsGetImageFromCurrentImageContext()
    }

    // MARK: - Cropping

    func cropImage(_ image: UIImage, to rect: CGRect) -> UIImage? {
        // Ensure crop rect is within image bounds
        let imageBounds = CGRect(origin: .zero, size: image.size)
        let cropRect = rect.intersection(imageBounds)

        guard !cropRect.isEmpty else {
            logger.error("Invalid crop rect for image")
            return nil
        }

        // Scale crop rect to match image's scale
        let scaledRect = CGRect(
            x: cropRect.origin.x * image.scale,
            y: cropRect.origin.y * image.scale,
            width: cropRect.size.width * image.scale,
            height: cropRect.size.height * image.scale
        )

        // Perform crop
        guard let cgImage = image.cgImage?.cropping(to: scaledRect) else {
            logger.error("Failed to crop CGImage")
            return nil
        }

        return UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
    }

    // MARK: - Aspect Ratio Calculations

    enum AspectRatio {
        case free
        case square    // 1:1
        case standard  // 4:3
        case wide      // 16:9

        var ratio: CGFloat? {
            switch self {
            case .free: return nil
            case .square: return 1.0
            case .standard: return 4.0/3.0
            case .wide: return 16.0/9.0
            }
        }
    }

    func calculateAspectRatioCrop(for imageSize: CGSize, aspectRatio: AspectRatio) -> CGRect {
        guard let ratio = aspectRatio.ratio else {
            // Free crop - return full image bounds
            return CGRect(origin: .zero, size: imageSize)
        }

        let imageRatio = imageSize.width / imageSize.height
        var cropSize = CGSize.zero

        if imageRatio > ratio {
            // Image is wider than target ratio
            cropSize.height = imageSize.height
            cropSize.width = cropSize.height * ratio
        } else {
            // Image is taller than target ratio
            cropSize.width = imageSize.width
            cropSize.height = cropSize.width / ratio
        }

        // Center the crop rect
        let origin = CGPoint(
            x: (imageSize.width - cropSize.width) / 2,
            y: (imageSize.height - cropSize.height) / 2
        )

        return CGRect(origin: origin, size: cropSize)
    }

    // MARK: - Batch Processing Support

    func processImageForSaving(_ image: UIImage, rotation: CGFloat = 0, cropRect: CGRect? = nil) -> UIImage? {
        var processedImage = image

        // Apply rotation if needed
        if rotation != 0 {
            guard let rotated = rotateImage(processedImage, degrees: rotation) else {
                logger.error("Failed to rotate image")
                return nil
            }
            processedImage = rotated
        }

        // Apply crop if specified
        if let crop = cropRect {
            guard let cropped = cropImage(processedImage, to: crop) else {
                logger.error("Failed to crop image")
                return nil
            }
            processedImage = cropped
        }

        // Ensure final image doesn't exceed max dimensions
        if let resized = resizeImage(processedImage, maxDimension: maxPhotoSize) {
            processedImage = resized
        }

        return processedImage
    }

    // MARK: - Memory Management

    func createPreviewImage(_ image: UIImage, maxDimension: CGFloat = 800) -> UIImage? {
        // Create a smaller preview for UI display to save memory
        return resizeImage(image, maxDimension: maxDimension)
    }

    // MARK: - EXIF Handling

    func normalizeImageOrientation(_ image: UIImage) -> UIImage? {
        guard image.imageOrientation != .up else {
            return image
        }

        UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: CGRect(origin: .zero, size: image.size))
        return UIGraphicsGetImageFromCurrentImageContext()
    }

    // MARK: - Utility Functions

    func isValidImageData(_ data: Data) -> Bool {
        return UIImage(data: data) != nil
    }

    func isValidImageFile(at url: URL) -> Bool {
        guard let data = try? Data(contentsOf: url) else {
            return false
        }
        return isValidImageData(data)
    }

    func getImageOrientation(from imageData: Data) -> UIImage.Orientation {
        guard let image = UIImage(data: imageData) else {
            return .up
        }
        return image.imageOrientation
    }
}