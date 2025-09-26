import Foundation
import UIKit
import CoreGraphics

/// Manager for intelligent thumbnail size selection based on context
class ThumbnailSizeManager {
    static let shared = ThumbnailSizeManager()

    // MARK: - Thumbnail Specifications
    struct ThumbnailSpec {
        let size: ThumbnailSize
        let pixelSize: CGSize
        let quality: CGFloat
        let fileNamePrefix: String

        var estimatedFileSize: Int {
            // Rough estimate: width * height * 4 bytes * quality factor
            Int(pixelSize.width * pixelSize.height * 4 * quality * 0.1)
        }
    }

    // Define thumbnail specifications
    private let thumbnailSpecs: [ThumbnailSize: ThumbnailSpec] = [
        .small: ThumbnailSpec(
            size: .small,
            pixelSize: CGSize(width: 150, height: 150),
            quality: 0.7,
            fileNamePrefix: "thumb_small_"
        ),
        .medium: ThumbnailSpec(
            size: .medium,
            pixelSize: CGSize(width: 300, height: 300),
            quality: 0.8,
            fileNamePrefix: "thumb_medium_"
        ),
        .large: ThumbnailSpec(
            size: .large,
            pixelSize: CGSize(width: 600, height: 600),
            quality: 0.85,
            fileNamePrefix: "thumb_large_"
        )
    ]

    // MARK: - Size Selection

    /// Select optimal thumbnail size based on display context
    func selectThumbnailSize(for context: DisplayContext) -> ThumbnailSize {
        switch context {
        case .grid(let columns):
            return sizeForGrid(columns: columns)
        case .list:
            return .medium
        case .detail:
            return .large
        case .preview:
            return .large
        case .widget:
            return .small
        }
    }

    /// Select size based on grid columns
    private func sizeForGrid(columns: Int) -> ThumbnailSize {
        let screenWidth = UIScreen.main.bounds.width
        let itemWidth = (screenWidth - CGFloat(columns + 1) * 2) / CGFloat(columns)

        // Account for screen scale
        let scale = UIScreen.main.scale
        let pixelWidth = itemWidth * scale

        if pixelWidth <= 300 {
            return .small
        } else if pixelWidth <= 600 {
            return .medium
        } else {
            return .large
        }
    }

    /// Select size based on available memory
    func adjustSizeForMemoryPressure(requested: ThumbnailSize, memoryUsageMB: Int) -> ThumbnailSize {
        // If memory usage is high, downgrade to smaller size
        if memoryUsageMB > 80 {
            switch requested {
            case .large:
                return .medium
            case .medium:
                return .small
            case .small:
                return .small
            }
        } else if memoryUsageMB > 100 {
            // Critical memory pressure - use smallest size
            return .small
        }

        return requested
    }

    /// Get specification for thumbnail size
    func specification(for size: ThumbnailSize) -> ThumbnailSpec? {
        return thumbnailSpecs[size]
    }

    // MARK: - Path Management

    /// Generate thumbnail file path
    func thumbnailPath(for photo: Photo, size: ThumbnailSize) -> URL {
        let spec = thumbnailSpecs[size]!
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let thumbnailsDir = documentsURL.appendingPathComponent("thumbnails")

        // Create size-specific subdirectory
        let sizeDir = thumbnailsDir.appendingPathComponent(size.directoryName)
        try? FileManager.default.createDirectory(at: sizeDir, withIntermediateDirectories: true)

        let photoURL = URL(fileURLWithPath: photo.path)
        let fileName = spec.fileNamePrefix + photoURL.lastPathComponent
        return sizeDir.appendingPathComponent(fileName)
    }

    /// Check if thumbnail exists
    func thumbnailExists(for photo: Photo, size: ThumbnailSize) -> Bool {
        let path = thumbnailPath(for: photo, size: size)
        return FileManager.default.fileExists(atPath: path.path)
    }

    /// Get all thumbnail paths for a photo
    func allThumbnailPaths(for photo: Photo) -> [ThumbnailSize: URL] {
        var paths: [ThumbnailSize: URL] = [:]
        for size in ThumbnailSize.allCases {
            paths[size] = thumbnailPath(for: photo, size: size)
        }
        return paths
    }

    // MARK: - Batch Processing

    /// Determine which thumbnail sizes to generate based on device
    func recommendedSizesForDevice() -> [ThumbnailSize] {
        let device = UIDevice.current

        if device.userInterfaceIdiom == .pad {
            // iPad needs all sizes
            return [.small, .medium, .large]
        } else {
            // iPhone can skip large for initial import
            return [.small, .medium]
        }
    }

    /// Calculate total storage needed for thumbnails
    func estimateStorageNeeded(photoCount: Int, sizes: [ThumbnailSize]) -> Int {
        var totalBytes = 0
        for size in sizes {
            if let spec = thumbnailSpecs[size] {
                totalBytes += spec.estimatedFileSize * photoCount
            }
        }
        return totalBytes
    }

    // MARK: - Performance Optimization

    /// Get optimal prefetch count based on memory
    func optimalPrefetchCount(memoryUsageMB: Int) -> Int {
        if memoryUsageMB < 50 {
            return 20
        } else if memoryUsageMB < 80 {
            return 10
        } else {
            return 5
        }
    }

    /// Determine if should use disk cache
    func shouldUseDiskCache(memoryUsageMB: Int) -> Bool {
        return memoryUsageMB > 60
    }
}

// MARK: - Display Context
enum DisplayContext {
    case grid(columns: Int)
    case list
    case detail
    case preview
    case widget
}

// MARK: - ThumbnailSize Extension
extension ThumbnailSize {
    static var allCases: [ThumbnailSize] {
        return [.small, .medium, .large]
    }

    var directoryName: String {
        switch self {
        case .small:
            return "small"
        case .medium:
            return "medium"
        case .large:
            return "large"
        }
    }

    var displayName: String {
        switch self {
        case .small:
            return "Small (150px)"
        case .medium:
            return "Medium (300px)"
        case .large:
            return "Large (600px)"
        }
    }
}

// MARK: - Storage Statistics
struct ThumbnailStorageStats {
    let totalSize: Int64
    let fileCount: Int
    let sizeBreakdown: [ThumbnailSize: Int64]

    var formattedTotalSize: String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .binary
        return formatter.string(fromByteCount: totalSize)
    }

    static func calculate() -> ThumbnailStorageStats {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let thumbnailsDir = documentsURL.appendingPathComponent("thumbnails")

        var totalSize: Int64 = 0
        var fileCount = 0
        var breakdown: [ThumbnailSize: Int64] = [:]

        for size in ThumbnailSize.allCases {
            let sizeDir = thumbnailsDir.appendingPathComponent(size.directoryName)
            if let files = try? FileManager.default.contentsOfDirectory(at: sizeDir, includingPropertiesForKeys: [.fileSizeKey]) {
                var sizeTotal: Int64 = 0
                for file in files {
                    if let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
                       let fileSize = attributes[FileAttributeKey.size] as? Int64 {
                        sizeTotal += fileSize
                        totalSize += fileSize
                        fileCount += 1
                    }
                }
                breakdown[size] = sizeTotal
            }
        }

        return ThumbnailStorageStats(
            totalSize: totalSize,
            fileCount: fileCount,
            sizeBreakdown: breakdown
        )
    }
}