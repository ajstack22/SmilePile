import Foundation

struct StorageResult {
    let photoPath: String
    let thumbnailPath: String?
    let fileName: String
    let fileSize: Int64
}

struct StorageUsage {
    let totalBytes: Int64
    let photoBytes: Int64
    let thumbnailBytes: Int64
    let photoCount: Int

    var formattedTotalSize: String {
        ByteCountFormatter.string(fromByteCount: totalBytes, countStyle: .binary)
    }

    var formattedPhotoSize: String {
        ByteCountFormatter.string(fromByteCount: photoBytes, countStyle: .binary)
    }

    var formattedThumbnailSize: String {
        ByteCountFormatter.string(fromByteCount: thumbnailBytes, countStyle: .binary)
    }
}