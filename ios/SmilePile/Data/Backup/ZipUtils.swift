import Foundation
import ZIPFoundation

// MARK: - Backup Errors

enum BackupError: LocalizedError {
    case sourceDirectoryNotFound
    case destinationPathInvalid
    case zipFileNotFound
    case zipCreationFailed(String)
    case zipExtractionFailed(String)
    case insufficientDiskSpace
    case permissionDenied
    case corruptedZipFile
    case securityViolation(String)
    case maxEntriesExceeded
    case maxSizeExceeded
    case invalidEntryPath(String)

    var errorDescription: String? {
        switch self {
        case .sourceDirectoryNotFound:
            return "Source directory not found"
        case .destinationPathInvalid:
            return "Destination path is invalid"
        case .zipFileNotFound:
            return "ZIP file not found"
        case .zipCreationFailed(let message):
            return "Failed to create ZIP: \(message)"
        case .zipExtractionFailed(let message):
            return "Failed to extract ZIP: \(message)"
        case .insufficientDiskSpace:
            return "Insufficient disk space"
        case .permissionDenied:
            return "Permission denied"
        case .corruptedZipFile:
            return "ZIP file is corrupted"
        case .securityViolation(let message):
            return "Security violation: \(message)"
        case .maxEntriesExceeded:
            return "ZIP contains too many entries"
        case .maxSizeExceeded:
            return "ZIP uncompressed size exceeds limit"
        case .invalidEntryPath(let path):
            return "Invalid entry path: \(path)"
        }
    }
}

// MARK: - ZIP Utilities

class ZipUtils {

    // Security limits to prevent ZIP bombs
    private static let MAX_ENTRIES = 10000
    private static let MAX_UNCOMPRESSED_SIZE: Int64 = 1024 * 1024 * 1024 // 1GB
    private static let BUFFER_SIZE = 8192

    // Standard directory structure in ZIP
    static let PHOTOS_DIR = "photos/"
    static let METADATA_FILE = "metadata.json"

    /// Create ZIP file from directory using native iOS NSFileCoordinator
    /// - Parameters:
    ///   - sourcePath: Directory containing files to ZIP
    ///   - destinationPath: Output ZIP file path
    ///   - progressCallback: Optional progress callback (0.0 to 1.0)
    static func createZip(
        from sourcePath: URL,
        to destinationPath: URL,
        progress progressCallback: ((Double) -> Void)? = nil
    ) async throws {
        guard FileManager.default.fileExists(atPath: sourcePath.path) else {
            throw BackupError.sourceDirectoryNotFound
        }

        // Check disk space
        try checkDiskSpace(for: sourcePath)

        // Use NSFileCoordinator to create ZIP (native iOS API)
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            var coordinationError: NSError?
            let coordinator = NSFileCoordinator()

            coordinator.coordinate(
                readingItemAt: sourcePath,
                options: [.forUploading],
                error: &coordinationError
            ) { zippedURL in
                do {
                    // NSFileCoordinator creates a temporary ZIP file
                    // Copy it to our destination
                    if FileManager.default.fileExists(atPath: destinationPath.path) {
                        try FileManager.default.removeItem(at: destinationPath)
                    }

                    try FileManager.default.copyItem(at: zippedURL, to: destinationPath)

                    progressCallback?(1.0)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: BackupError.zipCreationFailed(error.localizedDescription))
                }
            }

            if let error = coordinationError {
                continuation.resume(throwing: BackupError.zipCreationFailed(error.localizedDescription))
            }
        }
    }

    /// Extract ZIP file to destination directory using ZIPFoundation
    /// - Parameters:
    ///   - sourcePath: ZIP file to extract
    ///   - destinationPath: Directory to extract to
    ///   - progressCallback: Optional progress callback (0.0 to 1.0)
    static func extractZip(
        from sourcePath: URL,
        to destinationPath: URL,
        progress progressCallback: ((Double) -> Void)? = nil
    ) async throws {
        guard FileManager.default.fileExists(atPath: sourcePath.path) else {
            throw BackupError.zipFileNotFound
        }

        // Validate ZIP file before extraction
        try await validateZipFile(at: sourcePath)

        // Create destination directory if needed
        try FileManager.default.createDirectory(
            at: destinationPath,
            withIntermediateDirectories: true,
            attributes: nil
        )

        // Use ZIPFoundation to extract
        do {
            guard let archive = Archive(url: sourcePath, accessMode: .read) else {
                throw BackupError.corruptedZipFile
            }

            let totalEntries = archive.reduce(0) { count, _ in count + 1 }
            var processedEntries = 0

            for entry in archive {
                // Sanitize path to prevent directory traversal
                let sanitizedPath = sanitizeEntryName(entry.path)
                let destinationURL = destinationPath.appendingPathComponent(sanitizedPath)

                // Create parent directory if needed
                let parentDir = destinationURL.deletingLastPathComponent()
                try FileManager.default.createDirectory(
                    at: parentDir,
                    withIntermediateDirectories: true,
                    attributes: nil
                )

                // Extract entry
                _ = try archive.extract(entry, to: destinationURL)

                processedEntries += 1
                if let callback = progressCallback {
                    callback(Double(processedEntries) / Double(totalEntries))
                }
            }

            progressCallback?(1.0)
        } catch {
            throw BackupError.zipExtractionFailed(error.localizedDescription)
        }
    }

    /// Validate ZIP file structure and security
    private static func validateZipFile(at path: URL) async throws {
        // Check file size
        let attributes = try FileManager.default.attributesOfItem(atPath: path.path)
        if let fileSize = attributes[.size] as? Int64 {
            if fileSize > MAX_UNCOMPRESSED_SIZE {
                throw BackupError.maxSizeExceeded
            }
        }

        // Validate with ZIPFoundation
        guard let archive = Archive(url: path, accessMode: .read) else {
            throw BackupError.corruptedZipFile
        }

        // Count entries and check for security issues
        var entryCount = 0
        var totalUncompressedSize: Int64 = 0

        for entry in archive {
            entryCount += 1

            // Check entry count limit
            if entryCount > MAX_ENTRIES {
                throw BackupError.maxEntriesExceeded
            }

            // Check for path traversal attempts
            if entry.path.contains("..") {
                throw BackupError.invalidEntryPath(entry.path)
            }

            // Accumulate uncompressed size
            totalUncompressedSize += Int64(entry.uncompressedSize)

            // Check uncompressed size limit
            if totalUncompressedSize > MAX_UNCOMPRESSED_SIZE {
                throw BackupError.maxSizeExceeded
            }

            // Check compression ratio (ZIP bomb detection)
            if entry.compressedSize > 0 {
                let ratio = Double(entry.uncompressedSize) / Double(entry.compressedSize)
                if ratio > 100 {
                    throw BackupError.securityViolation("Suspicious compression ratio detected")
                }
            }
        }
    }

    /// Check if there's sufficient disk space
    private static func checkDiskSpace(for directory: URL) throws {
        let fileManager = FileManager.default

        // Calculate directory size
        var totalSize: Int64 = 0
        if let enumerator = fileManager.enumerator(at: directory, includingPropertiesForKeys: [.fileSizeKey]) {
            for case let fileURL as URL in enumerator {
                if let resourceValues = try? fileURL.resourceValues(forKeys: [.fileSizeKey]),
                   let fileSize = resourceValues.fileSize {
                    totalSize += Int64(fileSize)
                }
            }
        }

        // Check available disk space
        if let systemAttributes = try? fileManager.attributesOfFileSystem(forPath: directory.path),
           let freeSpace = systemAttributes[.systemFreeSize] as? NSNumber {
            let availableSpace = freeSpace.int64Value

            // Require at least 2x the directory size for safety
            if availableSpace < (totalSize * 2) {
                throw BackupError.insufficientDiskSpace
            }
        }
    }

    /// Sanitize entry name to prevent path traversal attacks
    private static func sanitizeEntryName(_ name: String) -> String {
        // Remove any path components that try to escape the extraction directory
        let components = name.components(separatedBy: "/")
        let sanitized = components.filter { $0 != ".." && !$0.isEmpty }
        return sanitized.joined(separator: "/")
    }

    /// List contents of ZIP file
    static func listZipContents(at path: URL) async throws -> [String] {
        guard FileManager.default.fileExists(atPath: path.path) else {
            throw BackupError.zipFileNotFound
        }

        guard let archive = Archive(url: path, accessMode: .read) else {
            throw BackupError.corruptedZipFile
        }

        var contents: [String] = []
        for entry in archive {
            contents.append(entry.path)
        }

        return contents
    }
}
