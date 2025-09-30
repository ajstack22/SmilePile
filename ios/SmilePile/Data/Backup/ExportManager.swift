import Foundation
import UIKit
import UniformTypeIdentifiers
import os.log

// MARK: - Export Manager
@MainActor
final class ExportManager: ObservableObject {

    // MARK: - Singleton
    static let shared = ExportManager()

    // MARK: - Published Properties
    @Published var isExporting = false
    @Published var exportProgress: ExportProgress?

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "ExportManager")
    private let fileManager = FileManager.default
    private let jsonEncoder = JSONEncoder()

    // MARK: - Dependencies
    private let categoryRepository: CategoryRepository
    private let photoRepository: PhotoRepository
    private let backupManager: BackupManager

    // MARK: - Constants
    private let exportDirectory: URL
    private let tempDirectory: URL

    // MARK: - Export Formats
    enum ExportFormat {
        case photos          // Export selected photos only
        case photosWithMetadata  // Photos + JSON metadata
        case htmlGallery     // HTML gallery with thumbnails
        case pdfCatalog      // PDF catalog format
        case shareSheet      // iOS Share Sheet
    }

    // MARK: - Initialization
    private init() {
        self.categoryRepository = CategoryRepositoryImpl()
        self.photoRepository = PhotoRepositoryImpl()
        self.backupManager = BackupManager.shared

        // Configure JSON encoder
        self.jsonEncoder.outputFormatting = [.prettyPrinted, .sortedKeys]

        // Setup directories
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.exportDirectory = documentsDirectory.appendingPathComponent("Exports", isDirectory: true)
        self.tempDirectory = fileManager.temporaryDirectory.appendingPathComponent("ExportTemp", isDirectory: true)

        // Create directories if needed
        try? fileManager.createDirectory(at: exportDirectory, withIntermediateDirectories: true, attributes: nil)
        try? fileManager.createDirectory(at: tempDirectory, withIntermediateDirectories: true, attributes: nil)
    }

    // MARK: - Export by Category

    func exportCategory(
        _ category: Category,
        format: ExportFormat = .photosWithMetadata,
        compressionQuality: CGFloat = 0.9
    ) async throws -> URL {

        logger.info("Exporting category: \(category.name) with format: \(String(describing: format))")
        isExporting = true
        defer { isExporting = false }

        // Get photos in category
        let photos = try await photoRepository.getPhotosByCategory(category.id)

        guard !photos.isEmpty else {
            throw BackupError.exportFailed("No photos in category")
        }

        updateProgress(0, photos.count, "Preparing export")

        switch format {
        case .photos:
            return try await exportPhotosOnly(photos: photos, categoryName: category.name, compressionQuality: compressionQuality)
        case .photosWithMetadata:
            return try await exportPhotosWithMetadata(photos: photos, category: category, compressionQuality: compressionQuality)
        case .htmlGallery:
            return try await exportAsHtmlGallery(photos: photos, category: category)
        case .pdfCatalog:
            return try await exportAsPdfCatalog(photos: photos, category: category)
        case .shareSheet:
            return try await prepareForShareSheet(photos: photos, category: category)
        }
    }

    // MARK: - Export by Date Range

    func exportByDateRange(
        startDate: Date,
        endDate: Date,
        format: ExportFormat = .photosWithMetadata,
        compressionQuality: CGFloat = 0.9
    ) async throws -> URL {

        logger.info("Exporting photos from \(startDate) to \(endDate)")
        isExporting = true
        defer { isExporting = false }

        // Get photos in date range
        let allPhotos = try await photoRepository.getAllPhotos()
        let startTimestamp = Int64(startDate.timeIntervalSince1970 * 1000)
        let endTimestamp = Int64(endDate.timeIntervalSince1970 * 1000)
        let photos = allPhotos.filter { photo in
            photo.createdAt >= startTimestamp && photo.createdAt <= endTimestamp
        }

        guard !photos.isEmpty else {
            throw BackupError.exportFailed("No photos in date range")
        }

        updateProgress(0, photos.count, "Preparing export")

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMdd"
        let exportName = "\(dateFormatter.string(from: startDate))_to_\(dateFormatter.string(from: endDate))"

        switch format {
        case .photos:
            return try await exportPhotosOnly(photos: photos, categoryName: exportName, compressionQuality: compressionQuality)
        case .photosWithMetadata:
            return try await exportPhotosWithMetadata(photos: photos, category: nil, exportName: exportName, compressionQuality: compressionQuality)
        case .htmlGallery:
            return try await exportAsHtmlGallery(photos: photos, category: nil, title: "Photos: \(exportName)")
        case .pdfCatalog:
            return try await exportAsPdfCatalog(photos: photos, category: nil, title: "Photos: \(exportName)")
        case .shareSheet:
            return try await prepareForShareSheet(photos: photos, category: nil)
        }
    }

    // MARK: - Export Selected Photos

    func exportSelectedPhotos(
        _ photos: [Photo],
        format: ExportFormat = .photos,
        compressionQuality: CGFloat = 0.9
    ) async throws -> URL {

        logger.info("Exporting \(photos.count) selected photos")
        isExporting = true
        defer { isExporting = false }

        guard !photos.isEmpty else {
            throw BackupError.exportFailed("No photos selected")
        }

        updateProgress(0, photos.count, "Preparing export")

        switch format {
        case .photos:
            return try await exportPhotosOnly(photos: photos, categoryName: "selected", compressionQuality: compressionQuality)
        case .photosWithMetadata:
            return try await exportPhotosWithMetadata(photos: photos, category: nil, exportName: "selected", compressionQuality: compressionQuality)
        case .htmlGallery:
            return try await exportAsHtmlGallery(photos: photos, category: nil, title: "Selected Photos")
        case .pdfCatalog:
            return try await exportAsPdfCatalog(photos: photos, category: nil, title: "Selected Photos")
        case .shareSheet:
            return try await prepareForShareSheet(photos: photos, category: nil)
        }
    }

    // MARK: - Private Export Methods

    private func exportPhotosOnly(
        photos: [Photo],
        categoryName: String,
        compressionQuality: CGFloat
    ) async throws -> URL {

        // Clean temp directory
        cleanTempDirectory()

        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let exportDir = tempDirectory.appendingPathComponent("export_\(categoryName)_\(timestamp)", isDirectory: true)
        try fileManager.createDirectory(at: exportDir, withIntermediateDirectories: true, attributes: nil)

        var processedCount = 0

        for photo in photos {
            autoreleasepool {
                do {
                    if !photo.isFromAssets {
                        let sourceFile = URL(fileURLWithPath: photo.path)
                        if fileManager.fileExists(atPath: sourceFile.path) {
                            let destFile = exportDir.appendingPathComponent(photo.name)

                            if compressionQuality < 1.0 {
                                // Compress photo
                                if let image = UIImage(contentsOfFile: sourceFile.path),
                                   let jpegData = image.jpegData(compressionQuality: compressionQuality) {
                                    try jpegData.write(to: destFile)
                                }
                            } else {
                                // Copy without compression
                                try fileManager.copyItem(at: sourceFile, to: destFile)
                            }
                        }
                    }
                } catch {
                    logger.warning("Failed to export photo: \(photo.name)")
                }

                processedCount += 1
                updateProgress(processedCount, photos.count, "Exporting photos")
            }
        }

        // Create ZIP archive
        let zipFileName = "Photos_\(categoryName)_\(timestamp).zip"
        let zipFile = exportDirectory.appendingPathComponent(zipFileName)

        try await ZipUtils.createZipFromDirectory(
            sourceDir: exportDir,
            outputFile: zipFile,
            compressionLevel: .normal
        )

        // Clean up temp directory
        try? fileManager.removeItem(at: exportDir)

        return zipFile
    }

    private func exportPhotosWithMetadata(
        photos: [Photo],
        category: Category?,
        exportName: String? = nil,
        compressionQuality: CGFloat
    ) async throws -> URL {

        // Clean temp directory
        cleanTempDirectory()

        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let name = exportName ?? category?.name ?? "export"
        let exportDir = tempDirectory.appendingPathComponent("export_\(name)_\(timestamp)", isDirectory: true)
        try fileManager.createDirectory(at: exportDir, withIntermediateDirectories: true, attributes: nil)

        // Create photos directory
        let photosDir = exportDir.appendingPathComponent("photos", isDirectory: true)
        try fileManager.createDirectory(at: photosDir, withIntermediateDirectories: true, attributes: nil)

        var processedCount = 0
        var photoManifest: [PhotoManifestEntry] = []

        for photo in photos {
            autoreleasepool {
                do {
                    if !photo.isFromAssets {
                        let sourceFile = URL(fileURLWithPath: photo.path)
                        if fileManager.fileExists(atPath: sourceFile.path) {
                            let fileName = "\(photo.id)_\(photo.name)"
                            let destFile = photosDir.appendingPathComponent(fileName)

                            if compressionQuality < 1.0 {
                                // Compress photo
                                if let image = UIImage(contentsOfFile: sourceFile.path),
                                   let jpegData = image.jpegData(compressionQuality: compressionQuality) {
                                    try jpegData.write(to: destFile)
                                }
                            } else {
                                // Copy without compression
                                try fileManager.copyItem(at: sourceFile, to: destFile)
                            }

                            // Add to manifest
                            let checksum = try ZipUtils.calculateMD5(destFile)
                            photoManifest.append(PhotoManifestEntry(
                                photoId: photo.id,
                                originalPath: photo.path,
                                zipEntryName: "photos/\(fileName)",
                                fileName: fileName,
                                fileSize: try fileManager.attributesOfItem(atPath: destFile.path)[.size] as? Int64 ?? 0,
                                checksum: checksum
                            ))
                        }
                    }
                } catch {
                    logger.warning("Failed to export photo: \(photo.name)")
                }

                processedCount += 1
                updateProgress(processedCount, photos.count, "Processing photos")
            }
        }

        // Create metadata
        let exportMetadata = ExportMetadata(
            exportDate: Date(),
            category: category?.name,
            photosCount: photos.count,
            photos: photos.map { BackupPhoto.fromPhoto($0) },
            photoManifest: photoManifest
        )

        // Write metadata.json
        let metadataFile = exportDir.appendingPathComponent("metadata.json")
        let metadataData = try jsonEncoder.encode(exportMetadata)
        try metadataData.write(to: metadataFile)

        // Create ZIP archive
        let zipFileName = "Export_\(name)_\(timestamp).zip"
        let zipFile = exportDirectory.appendingPathComponent(zipFileName)

        try await ZipUtils.createZipFromDirectory(
            sourceDir: exportDir,
            outputFile: zipFile,
            compressionLevel: .normal
        )

        // Clean up temp directory
        try? fileManager.removeItem(at: exportDir)

        return zipFile
    }

    private func exportAsHtmlGallery(
        photos: [Photo],
        category: Category?,
        title: String? = nil
    ) async throws -> URL {

        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let galleryDir = tempDirectory.appendingPathComponent("gallery_\(timestamp)", isDirectory: true)
        try fileManager.createDirectory(at: galleryDir, withIntermediateDirectories: true, attributes: nil)

        // Create images and thumbnails directories
        let imagesDir = galleryDir.appendingPathComponent("images", isDirectory: true)
        let thumbsDir = galleryDir.appendingPathComponent("thumbnails", isDirectory: true)
        try fileManager.createDirectory(at: imagesDir, withIntermediateDirectories: true, attributes: nil)
        try fileManager.createDirectory(at: thumbsDir, withIntermediateDirectories: true, attributes: nil)

        var galleryItems: [GalleryItem] = []
        var processedCount = 0

        for photo in photos {
            autoreleasepool {
                do {
                    if !photo.isFromAssets {
                        let sourceFile = URL(fileURLWithPath: photo.path)
                        if fileManager.fileExists(atPath: sourceFile.path),
                           let image = UIImage(contentsOfFile: sourceFile.path) {

                            // Copy full image
                            let imageName = "\(photo.id).jpg"
                            let imageFile = imagesDir.appendingPathComponent(imageName)
                            if let jpegData = image.jpegData(compressionQuality: 0.9) {
                                try jpegData.write(to: imageFile)
                            }

                            // Create thumbnail
                            let thumbName = "thumb_\(photo.id).jpg"
                            let thumbFile = thumbsDir.appendingPathComponent(thumbName)
                            let thumbnail = createThumbnail(from: image, maxSize: 200)
                            if let thumbData = thumbnail.jpegData(compressionQuality: 0.85) {
                                try thumbData.write(to: thumbFile)
                            }

                            galleryItems.append(GalleryItem(
                                id: photo.id,
                                name: photo.name,
                                imagePath: "images/\(imageName)",
                                thumbnailPath: "thumbnails/\(thumbName)",
                                date: Date(timeIntervalSince1970: TimeInterval(photo.createdAt) / 1000)
                            ))
                        }
                    }
                } catch {
                    logger.warning("Failed to process photo for gallery: \(photo.name)")
                }

                processedCount += 1
                updateProgress(processedCount, photos.count, "Creating gallery")
            }
        }

        // Generate HTML
        let html = generateHtmlGallery(
            title: title ?? category?.displayName ?? "Photo Gallery",
            items: galleryItems
        )

        // Write HTML file
        let htmlFile = galleryDir.appendingPathComponent("index.html")
        try html.write(to: htmlFile, atomically: true, encoding: .utf8)

        // Create ZIP archive
        let zipFileName = "Gallery_\(timestamp).zip"
        let zipFile = exportDirectory.appendingPathComponent(zipFileName)

        try await ZipUtils.createZipFromDirectory(
            sourceDir: galleryDir,
            outputFile: zipFile,
            compressionLevel: .normal
        )

        // Clean up temp directory
        try? fileManager.removeItem(at: galleryDir)

        return zipFile
    }

    private func exportAsPdfCatalog(
        photos: [Photo],
        category: Category?,
        title: String? = nil
    ) async throws -> URL {

        // Create PDF
        let pdfRenderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 612, height: 792))
        let pdfTitle = title ?? category?.displayName ?? "Photo Catalog"

        let data = pdfRenderer.pdfData { context in
            // Title page
            context.beginPage()
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 24, weight: .bold)
            ]
            pdfTitle.draw(at: CGPoint(x: 50, y: 100), withAttributes: titleAttributes)

            let dateFormatter = DateFormatter()
            dateFormatter.dateStyle = .long
            let dateString = "Created: \(dateFormatter.string(from: Date()))"
            dateString.draw(at: CGPoint(x: 50, y: 150), withAttributes: [
                .font: UIFont.systemFont(ofSize: 14)
            ])

            // Photos pages (4 photos per page)
            var photoIndex = 0
            let photosPerPage = 4
            let photoSize = CGSize(width: 250, height: 250)

            while photoIndex < photos.count {
                context.beginPage()

                for i in 0..<photosPerPage {
                    guard photoIndex < photos.count else { break }
                    let photo = photos[photoIndex]

                    if !photo.isFromAssets {
                        let sourceFile = URL(fileURLWithPath: photo.path)
                        if let image = UIImage(contentsOfFile: sourceFile.path) {
                            let row = i / 2
                            let col = i % 2
                            let x = CGFloat(50 + col * 280)
                            let y = CGFloat(50 + row * 320)

                            // Draw image
                            let imageRect = CGRect(origin: CGPoint(x: x, y: y), size: photoSize)
                            image.draw(in: imageRect)

                            // Draw caption
                            let captionY = y + photoSize.height + 10
                            photo.name.draw(at: CGPoint(x: x, y: captionY), withAttributes: [
                                .font: UIFont.systemFont(ofSize: 12)
                            ])
                        }
                    }

                    photoIndex += 1
                    updateProgress(photoIndex, photos.count, "Creating PDF catalog")
                }
            }
        }

        // Save PDF
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let pdfFileName = "Catalog_\(timestamp).pdf"
        let pdfFile = exportDirectory.appendingPathComponent(pdfFileName)
        try data.write(to: pdfFile)

        return pdfFile
    }

    private func prepareForShareSheet(
        photos: [Photo],
        category: Category?
    ) async throws -> URL {

        // For share sheet, create a simple ZIP with photos
        return try await exportPhotosOnly(
            photos: photos,
            categoryName: category?.name ?? "shared",
            compressionQuality: 0.9
        )
    }

    // MARK: - Helper Methods

    private func createThumbnail(from image: UIImage, maxSize: CGFloat) -> UIImage {
        let scale = min(maxSize / image.size.width, maxSize / image.size.height)
        let newSize = CGSize(
            width: image.size.width * scale,
            height: image.size.height * scale
        )

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }

    private func generateHtmlGallery(title: String, items: [GalleryItem]) -> String {
        var html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>\(title)</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 20px; background: #f5f5f5; }
                h1 { color: #333; }
                .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
                .photo { background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                .photo img { width: 100%; height: 200px; object-fit: cover; cursor: pointer; }
                .photo .caption { padding: 10px; font-size: 14px; color: #666; }
                .lightbox { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.9); z-index: 1000; }
                .lightbox img { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); max-width: 90%; max-height: 90%; }
                .lightbox.active { display: block; }
                .close { position: absolute; top: 20px; right: 40px; color: white; font-size: 40px; cursor: pointer; }
            </style>
        </head>
        <body>
            <h1>\(title)</h1>
            <div class="gallery">
        """

        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short

        for item in items {
            html += """
                <div class="photo">
                    <img src="\(item.thumbnailPath)" data-full="\(item.imagePath)" onclick="openLightbox(this)" alt="\(item.name)">
                    <div class="caption">
                        <div>\(item.name)</div>
                        <div style="font-size: 12px; color: #999;">\(dateFormatter.string(from: item.date))</div>
                    </div>
                </div>
            """
        }

        html += """
            </div>
            <div class="lightbox" id="lightbox" onclick="closeLightbox()">
                <span class="close">&times;</span>
                <img id="lightboxImg" src="" alt="">
            </div>
            <script>
                function openLightbox(img) {
                    document.getElementById('lightboxImg').src = img.dataset.full;
                    document.getElementById('lightbox').classList.add('active');
                }
                function closeLightbox() {
                    document.getElementById('lightbox').classList.remove('active');
                }
            </script>
        </body>
        </html>
        """

        return html
    }

    private func updateProgress(_ current: Int, _ total: Int, _ operation: String) {
        DispatchQueue.main.async {
            self.exportProgress = ExportProgress(
                current: current,
                total: total,
                operation: operation
            )
        }
    }

    private func cleanTempDirectory() {
        do {
            let tempFiles = try fileManager.contentsOfDirectory(at: tempDirectory, includingPropertiesForKeys: nil)
            for tempFile in tempFiles {
                try? fileManager.removeItem(at: tempFile)
            }
        } catch {
            logger.warning("Failed to clean temp directory: \(error.localizedDescription)")
        }
    }

    // MARK: - Share Sheet Integration

    func shareItems(_ items: [URL]) -> UIActivityViewController {
        let activityVC = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )

        // Exclude certain activity types if needed
        activityVC.excludedActivityTypes = [
            .assignToContact,
            .addToReadingList
        ]

        return activityVC
    }
}

// MARK: - Supporting Types

private struct ExportMetadata: Codable {
    let exportDate: Date
    let category: String?
    let photosCount: Int
    let photos: [BackupPhoto]
    let photoManifest: [PhotoManifestEntry]
}

private struct GalleryItem {
    let id: Int64
    let name: String
    let imagePath: String
    let thumbnailPath: String
    let date: Date
}