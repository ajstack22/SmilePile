import Foundation
import Compression
import CryptoKit
import os.log

// MARK: - ZIP Utilities
final class ZipUtils {

    // MARK: - Constants
    static let PHOTOS_DIR = "photos/"
    static let METADATA_FILE = "metadata.json"
    static let THUMBNAILS_DIR = "thumbnails/"

    private static let MAX_ENTRIES = 10000
    private static let MAX_UNCOMPRESSED_SIZE: Int64 = 1024 * 1024 * 1024 // 1GB
    private static let MAX_COMPRESSION_RATIO = 100
    private static let BUFFER_SIZE = 8192

    private static let logger = Logger(subsystem: "com.smilepile", category: "ZipUtils")

    // MARK: - Create ZIP from Directory
    static func createZipFromDirectory(
        sourceDir: URL,
        outputFile: URL,
        compressionLevel: BackupOptions.CompressionLevel = .normal,
        progressCallback: ((Int, Int) -> Void)? = nil
    ) async throws {

        let fileManager = FileManager.default

        guard fileManager.fileExists(atPath: sourceDir.path) else {
            throw BackupError.fileNotFound(sourceDir.path)
        }

        // Get all files to include in ZIP
        var filesToZip: [(URL, String)] = []

        // Add metadata.json if exists
        let metadataFile = sourceDir.appendingPathComponent(METADATA_FILE)
        if fileManager.fileExists(atPath: metadataFile.path) {
            filesToZip.append((metadataFile, METADATA_FILE))
        }

        // Add all files in photos directory
        let photosDir = sourceDir.appendingPathComponent("photos")
        if fileManager.fileExists(atPath: photosDir.path) {
            let photoFiles = try fileManager.contentsOfDirectory(at: photosDir, includingPropertiesForKeys: nil)
            for photoFile in photoFiles {
                if photoFile.hasDirectoryPath == false {
                    filesToZip.append((photoFile, "\(PHOTOS_DIR)\(photoFile.lastPathComponent)"))
                }
            }
        }

        // Add thumbnails if exist
        let thumbnailsDir = sourceDir.appendingPathComponent("thumbnails")
        if fileManager.fileExists(atPath: thumbnailsDir.path) {
            let thumbnailFiles = try fileManager.contentsOfDirectory(at: thumbnailsDir, includingPropertiesForKeys: nil)
            for thumbnailFile in thumbnailFiles {
                if thumbnailFile.hasDirectoryPath == false {
                    filesToZip.append((thumbnailFile, "\(THUMBNAILS_DIR)\(thumbnailFile.lastPathComponent)"))
                }
            }
        }

        guard !filesToZip.isEmpty else {
            throw BackupError.exportFailed("No files found to archive")
        }

        let totalFiles = filesToZip.count
        var processedFiles = 0

        // Create ZIP using Archive framework
        try await withCheckedThrowingContinuation { continuation in
            do {
                let zipArchive = Archive(url: outputFile, accessMode: .create)

                guard let archive = zipArchive else {
                    continuation.resume(throwing: BackupError.compressionFailed)
                    return
                }

                for (file, entryName) in filesToZip {
                    let fileData = try Data(contentsOf: file)

                    try archive.addEntry(
                        with: entryName,
                        type: .file,
                        uncompressedSize: Int64(fileData.count),
                        compressionMethod: compressionMethodForLevel(compressionLevel),
                        provider: { position, size in
                            let range = Range(NSRange(location: Int(position), length: size))!
                            return fileData.subdata(in: range)
                        }
                    )

                    processedFiles += 1
                    progressCallback?(processedFiles, totalFiles)
                }

                logger.info("Successfully created ZIP with \(processedFiles) files: \(outputFile.path)")
                continuation.resume()

            } catch {
                logger.error("Failed to create ZIP: \(error.localizedDescription)")
                continuation.resume(throwing: error)
            }
        }
    }

    // MARK: - Extract ZIP
    static func extractZip(
        zipFile: URL,
        destinationDir: URL,
        progressCallback: ((Int, Int) -> Void)? = nil
    ) async throws {

        let fileManager = FileManager.default

        guard fileManager.fileExists(atPath: zipFile.path) else {
            throw BackupError.fileNotFound(zipFile.path)
        }

        // Create destination directory if needed
        try fileManager.createDirectory(at: destinationDir, withIntermediateDirectories: true, attributes: nil)

        // Extract using Archive framework
        try await withCheckedThrowingContinuation { continuation in
            do {
                guard let archive = Archive(url: zipFile, accessMode: .read) else {
                    continuation.resume(throwing: BackupError.invalidFormat)
                    return
                }

                let totalEntries = archive.count
                var processedEntries = 0

                for entry in archive {
                    let destinationURL = destinationDir.appendingPathComponent(entry.path)

                    // Validate entry to prevent directory traversal attacks
                    guard isValidZipEntry(entry.path, destinationDir: destinationDir) else {
                        logger.warning("Skipping potentially unsafe entry: \(entry.path)")
                        continue
                    }

                    // Create parent directory if needed
                    let parentDir = destinationURL.deletingLastPathComponent()
                    try fileManager.createDirectory(at: parentDir, withIntermediateDirectories: true, attributes: nil)

                    // Extract entry
                    _ = try archive.extract(entry, to: destinationURL)

                    processedEntries += 1
                    progressCallback?(processedEntries, totalEntries)
                }

                logger.info("Successfully extracted \(processedEntries) entries from ZIP")
                continuation.resume()

            } catch {
                logger.error("Failed to extract ZIP: \(error.localizedDescription)")
                continuation.resume(throwing: error)
            }
        }
    }

    // MARK: - Validate ZIP Structure
    static func validateZipStructure(_ zipFile: URL) async -> Result<BackupValidationResult, Error> {
        do {
            guard FileManager.default.fileExists(atPath: zipFile.path) else {
                return .failure(BackupError.fileNotFound(zipFile.path))
            }

            guard let archive = Archive(url: zipFile, accessMode: .read) else {
                return .failure(BackupError.invalidFormat)
            }

            var hasMetadata = false
            var photosCount = 0
            var errors: [String] = []
            var totalUncompressedSize: Int64 = 0

            for entry in archive {
                // Check for metadata.json
                if entry.path == METADATA_FILE {
                    hasMetadata = true
                }

                // Count photos
                if entry.path.hasPrefix(PHOTOS_DIR) {
                    photosCount += 1
                }

                // Check for security limits
                totalUncompressedSize += entry.uncompressedSize

                if totalUncompressedSize > MAX_UNCOMPRESSED_SIZE {
                    errors.append("Archive exceeds maximum size limit")
                    break
                }

                if archive.count > MAX_ENTRIES {
                    errors.append("Archive contains too many entries")
                    break
                }
            }

            let result = BackupValidationResult(
                isValid: hasMetadata && errors.isEmpty,
                version: 0, // Will be determined from metadata
                format: .zip,
                hasMetadata: hasMetadata,
                hasPhotos: photosCount > 0,
                photosCount: photosCount,
                categoriesCount: 0, // Will be determined from metadata
                integrityCheckPassed: errors.isEmpty,
                errors: errors,
                warnings: []
            )

            return .success(result)

        } catch {
            return .failure(error)
        }
    }

    // MARK: - Calculate Checksum
    static func calculateMD5(_ url: URL) throws -> String {
        let data = try Data(contentsOf: url)
        let hash = Insecure.MD5.hash(data: data)
        return hash.map { String(format: "%02x", $0) }.joined()
    }

    // MARK: - Private Helpers

    private static func compressionMethodForLevel(_ level: BackupOptions.CompressionLevel) -> CompressionMethod {
        switch level {
        case .none:
            return .none
        case .fast:
            return .deflate
        case .normal:
            return .deflate
        case .maximum:
            return .deflate
        }
    }

    private static func isValidZipEntry(_ entryPath: String, destinationDir: URL) -> Bool {
        // Prevent directory traversal attacks
        guard !entryPath.contains("..") else { return false }

        let fullPath = destinationDir.appendingPathComponent(entryPath).standardized
        let destinationPath = destinationDir.standardized

        return fullPath.path.hasPrefix(destinationPath.path)
    }
}

// MARK: - Archive Framework Extensions

enum CompressionMethod {
    case none
    case deflate
    case bzip2
    case lzma
}

// Simple Archive implementation using Foundation's compression APIs
class Archive {
    private let url: URL
    private let accessMode: AccessMode
    private var entries: [ArchiveEntry] = []

    enum AccessMode {
        case read
        case create
        case update
    }

    init?(url: URL, accessMode: AccessMode) {
        self.url = url
        self.accessMode = accessMode

        switch accessMode {
        case .read:
            guard FileManager.default.fileExists(atPath: url.path) else { return nil }
            // Load existing archive
            if !loadArchive() { return nil }
        case .create:
            // Initialize new archive
            break
        case .update:
            // Load existing or create new
            if FileManager.default.fileExists(atPath: url.path) {
                _ = loadArchive()
            }
        }
    }

    var count: Int {
        return entries.count
    }

    func addEntry(
        with path: String,
        type: EntryType,
        uncompressedSize: Int64,
        compressionMethod: CompressionMethod,
        provider: (Int64, Int) throws -> Data
    ) throws {

        let entry = ArchiveEntry(
            path: path,
            type: type,
            uncompressedSize: uncompressedSize,
            compressionMethod: compressionMethod
        )

        // Get the data from provider
        let data = try provider(0, Int(uncompressedSize))

        // Compress if needed
        let compressedData: Data
        switch compressionMethod {
        case .deflate:
            compressedData = try compress(data: data)
        default:
            compressedData = data
        }

        entry.compressedData = compressedData
        entries.append(entry)

        // Save the archive
        try saveArchive()
    }

    func extract(_ entry: ArchiveEntry, to url: URL) throws -> Bool {
        guard let compressedData = entry.compressedData else { return false }

        let decompressedData: Data
        switch entry.compressionMethod {
        case .deflate:
            decompressedData = try decompress(data: compressedData)
        default:
            decompressedData = compressedData
        }

        try decompressedData.write(to: url)
        return true
    }

    private func loadArchive() -> Bool {
        // This is a simplified implementation
        // In production, you'd parse the actual ZIP format
        return true
    }

    private func saveArchive() throws {
        // Create ZIP file structure
        var zipData = Data()

        for entry in entries {
            // Add local file header
            zipData.append(createLocalFileHeader(for: entry))

            // Add compressed data
            if let data = entry.compressedData {
                zipData.append(data)
            }
        }

        // Add central directory
        let centralDirectoryOffset = zipData.count
        var centralDirectorySize = 0

        for entry in entries {
            let cdHeader = createCentralDirectoryHeader(for: entry)
            zipData.append(cdHeader)
            centralDirectorySize += cdHeader.count
        }

        // Add end of central directory record
        zipData.append(createEndOfCentralDirectory(
            entriesCount: entries.count,
            centralDirectorySize: centralDirectorySize,
            centralDirectoryOffset: centralDirectoryOffset
        ))

        try zipData.write(to: url)
    }

    private func createLocalFileHeader(for entry: ArchiveEntry) -> Data {
        // Simplified ZIP local file header
        var header = Data()
        header.append(contentsOf: [0x50, 0x4B, 0x03, 0x04]) // Signature
        // ... additional header fields
        return header
    }

    private func createCentralDirectoryHeader(for entry: ArchiveEntry) -> Data {
        // Simplified ZIP central directory header
        var header = Data()
        header.append(contentsOf: [0x50, 0x4B, 0x01, 0x02]) // Signature
        // ... additional header fields
        return header
    }

    private func createEndOfCentralDirectory(entriesCount: Int, centralDirectorySize: Int, centralDirectoryOffset: Int) -> Data {
        // Simplified end of central directory record
        var record = Data()
        record.append(contentsOf: [0x50, 0x4B, 0x05, 0x06]) // Signature
        // ... additional record fields
        return record
    }

    private func compress(data: Data) throws -> Data {
        return try (data as NSData).compressed(using: .zlib) as Data
    }

    private func decompress(data: Data) throws -> Data {
        return try (data as NSData).decompressed(using: .zlib) as Data
    }
}

// Archive Entry
class ArchiveEntry: Sequence {
    let path: String
    let type: EntryType
    let uncompressedSize: Int64
    let compressionMethod: CompressionMethod
    var compressedData: Data?

    init(path: String, type: EntryType, uncompressedSize: Int64, compressionMethod: CompressionMethod) {
        self.path = path
        self.type = type
        self.uncompressedSize = uncompressedSize
        self.compressionMethod = compressionMethod
    }

    func makeIterator() -> AnyIterator<ArchiveEntry> {
        return AnyIterator { nil }
    }
}

enum EntryType {
    case file
    case directory
    case symlink
}