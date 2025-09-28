import Foundation
import UIKit
import Photos
import PhotosUI
import os.log
import Swift

/// Simplified photo storage service - direct and reliable
@MainActor
class SimplePhotoStorage: ObservableObject {
    static let shared = SimplePhotoStorage()

    private let logger = Logger(subsystem: "com.smilepile", category: "SimplePhotoStorage")
    private let fileManager = FileManager.default

    // Directories
    private let photosDir: URL
    private let thumbsDir: URL

    init() {
        // Setup directories
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.photosDir = documentsURL.appendingPathComponent("simple_photos", isDirectory: true)
        self.thumbsDir = documentsURL.appendingPathComponent("simple_thumbs", isDirectory: true)

        // Create directories
        do {
            try fileManager.createDirectory(at: photosDir, withIntermediateDirectories: true)
            try fileManager.createDirectory(at: thumbsDir, withIntermediateDirectories: true)
            logger.info("✅ Storage directories created")
            logger.info("📁 Photos: \(self.photosDir.path)")
            logger.info("📁 Thumbs: \(self.thumbsDir.path)")
        } catch {
            logger.error("❌ Failed to create directories: \(error)")
        }
    }

    /// Process picker results - simple and direct
    func processPickerResults(_ results: [PHPickerResult], categoryId: Int64) async -> [Photo] {
        logger.info("📸 Processing \(results.count) picker results for category \(categoryId)")
        var savedPhotos: [Photo] = []

        for (index, result) in results.enumerated() {
            logger.info("Processing photo \(index + 1)/\(results.count)")

            // Load the image
            if let image = await loadImage(from: result) {
                // Save it directly
                if let photo = await savePhoto(image, categoryId: categoryId, index: index) {
                    savedPhotos.append(photo)
                    logger.info("✅ Saved photo \(photo.id)")
                } else {
                    logger.error("❌ Failed to save photo \(index + 1)")
                }
            } else {
                logger.error("❌ Failed to load photo \(index + 1)")
            }
        }

        logger.info("📸 Processed \(savedPhotos.count)/\(results.count) photos successfully")
        return savedPhotos
    }

    /// Load image from picker result - simplified without continuation
    @MainActor
    private func loadImage(from result: PHPickerResult) async -> UIImage? {
        // Use a simpler pattern without explicit continuation
        var loadedImage: UIImage?
        let semaphore = DispatchSemaphore(value: 0)

        result.itemProvider.loadObject(ofClass: UIImage.self) { object, error in
            defer { semaphore.signal() }

            if let error = error {
                print("❌ Error loading image: \(error)")
                return
            }

            guard let image = object as? UIImage else {
                print("❌ Object is not UIImage")
                return
            }

            print("✅ Loaded image: \(image.size)")
            loadedImage = image
        }

        // Wait for completion
        _ = await Task.detached {
            semaphore.wait()
        }.value

        return loadedImage
    }

    /// Save photo to disk - simple and direct
    private func savePhoto(_ image: UIImage, categoryId: Int64, index: Int) async -> Photo? {
        let photoId = PhotoIDGenerator.generateUniqueID()
        let timestamp = Date().timeIntervalSince1970

        // Generate filenames
        let photoFilename = "photo_\(photoId)_\(Int(timestamp)).jpg"
        let thumbFilename = "thumb_\(photoId).jpg"

        let photoURL = photosDir.appendingPathComponent(photoFilename)
        let thumbURL = thumbsDir.appendingPathComponent(thumbFilename)

        // Save full image (resize if too large)
        let maxSize: CGFloat = 2048
        let resized = resizeImage(image, maxDimension: maxSize)

        // Try to save as JPEG first
        var saved = false
        if let jpegData = resized.jpegData(compressionQuality: 0.8) {
            do {
                try jpegData.write(to: photoURL)
                saved = true
                logger.info("✅ Saved photo to: \(photoURL.lastPathComponent)")
            } catch {
                logger.error("❌ Failed to save JPEG: \(error)")
            }
        }

        // Fallback to PNG if JPEG fails
        if !saved, let pngData = resized.pngData() {
            do {
                try pngData.write(to: photoURL)
                saved = true
                logger.info("✅ Saved photo as PNG to: \(photoURL.lastPathComponent)")
            } catch {
                logger.error("❌ Failed to save PNG: \(error)")
                return nil
            }
        }

        guard saved else {
            logger.error("❌ Could not save photo in any format")
            return nil
        }

        // Generate and save thumbnail
        let thumbnail = resizeImage(image, maxDimension: 300)
        if let thumbData = thumbnail.jpegData(compressionQuality: 0.7) {
            do {
                try thumbData.write(to: thumbURL)
                logger.info("✅ Saved thumbnail to: \(thumbURL.lastPathComponent)")
            } catch {
                logger.error("❌ Failed to save thumbnail: \(error)")
            }
        }

        // Create Photo object
        let photo = Photo(
            id: photoId,
            path: photoURL.path,
            categoryId: categoryId,
            name: photoFilename,
            isFromAssets: false,
            createdAt: Int64(timestamp * 1000),
            fileSize: Int64(fileManager.fileSize(at: photoURL)),
            width: Int(resized.size.width),
            height: Int(resized.size.height)
        )

        logger.info("📸 Created photo: id=\(photo.id), category=\(categoryId), path=\(photoURL.lastPathComponent)")
        return photo
    }

    /// Simple image resizing
    private func resizeImage(_ image: UIImage, maxDimension: CGFloat) -> UIImage {
        let size = image.size
        let maxSize = max(size.width, size.height)

        if maxSize <= maxDimension {
            return image
        }

        let scale = maxDimension / maxSize
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        image.draw(in: CGRect(origin: .zero, size: newSize))
        let resized = UIGraphicsGetImageFromCurrentImageContext() ?? image
        UIGraphicsEndImageContext()

        return resized
    }

    /// Get thumbnail URL for a photo
    func thumbnailURL(for photo: Photo) -> URL? {
        let thumbFilename = "thumb_\(photo.id).jpg"
        let url = thumbsDir.appendingPathComponent(thumbFilename)

        // Check if it exists
        if fileManager.fileExists(atPath: url.path) {
            return url
        }

        // Try simple_thumbs directory
        let simpleThumbURL = thumbsDir.appendingPathComponent(thumbFilename)
        if fileManager.fileExists(atPath: simpleThumbURL.path) {
            return simpleThumbURL
        }

        // Fallback to photo itself
        return URL(fileURLWithPath: photo.path)
    }

    /// Load thumbnail image
    func loadThumbnail(for photo: Photo) async -> UIImage? {
        if let url = thumbnailURL(for: photo),
           let data = try? Data(contentsOf: url),
           let image = UIImage(data: data) {
            return image
        }

        // Try to load the full photo as fallback
        if let data = try? Data(contentsOf: URL(fileURLWithPath: photo.path)),
           let image = UIImage(data: data) {
            // Return a smaller version
            return resizeImage(image, maxDimension: 300)
        }

        return nil
    }

    /// Check storage health
    func checkStorageHealth() {
        logger.info("🔍 Storage Health Check:")
        logger.info("📁 Photos dir exists: \(self.fileManager.fileExists(atPath: self.photosDir.path))")
        logger.info("📁 Thumbs dir exists: \(self.fileManager.fileExists(atPath: self.thumbsDir.path))")

        // Count files
        if let photoFiles = try? self.fileManager.contentsOfDirectory(at: self.photosDir, includingPropertiesForKeys: nil) {
            logger.info("📸 Photos stored: \(photoFiles.count)")
        }

        if let thumbFiles = try? self.fileManager.contentsOfDirectory(at: self.thumbsDir, includingPropertiesForKeys: nil) {
            logger.info("🖼 Thumbnails stored: \(thumbFiles.count)")
        }
    }
}

// Helper extension
private extension FileManager {
    func fileSize(at url: URL) -> Int {
        (try? attributesOfItem(atPath: url.path)[.size] as? Int) ?? 0
    }
}