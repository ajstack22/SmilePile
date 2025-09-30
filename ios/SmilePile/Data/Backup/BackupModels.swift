import Foundation

// MARK: - Constants
let CURRENT_BACKUP_VERSION = 2
let MIN_SUPPORTED_VERSION = 1
let MAX_SUPPORTED_VERSION = CURRENT_BACKUP_VERSION

// MARK: - Backup Format
enum BackupFormat: String, Codable {
    case json = "JSON"   // Version 1: JSON export without photo files
    case zip = "ZIP"     // Version 2: ZIP export with photo files included
}

// MARK: - Import Strategy
enum ImportStrategy {
    case merge    // Merge with existing data
    case replace  // Replace all existing data
}

// MARK: - Root Backup Structure
struct AppBackup: Codable {
    let version: Int
    let exportDate: Int64
    let appVersion: String
    let format: String
    let categories: [BackupCategory]
    let photos: [BackupPhoto]
    let settings: BackupSettings
    let photoManifest: [PhotoManifestEntry]

    init(version: Int = CURRENT_BACKUP_VERSION,
         exportDate: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
         appVersion: String = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
         format: BackupFormat = .zip,
         categories: [BackupCategory],
         photos: [BackupPhoto],
         settings: BackupSettings,
         photoManifest: [PhotoManifestEntry] = []) {
        self.version = version
        self.exportDate = exportDate
        self.appVersion = appVersion
        self.format = format.rawValue
        self.categories = categories
        self.photos = photos
        self.settings = settings
        self.photoManifest = photoManifest
    }
}

// MARK: - Backup Category
struct BackupCategory: Codable {
    let id: Int64
    let name: String
    let displayName: String
    let position: Int
    let iconResource: String?
    let colorHex: String?
    let isDefault: Bool
    let createdAt: Int64

    func toCategory() -> Category {
        Category(
            id: id,
            name: name,
            displayName: displayName,
            position: Int32(position),
            iconResource: iconResource,
            colorHex: colorHex,
            isDefault: isDefault,
            createdAt: Date(timeIntervalSince1970: TimeInterval(createdAt / 1000))
        )
    }

    static func fromCategory(_ category: Category) -> BackupCategory {
        BackupCategory(
            id: category.id,
            name: category.name,
            displayName: category.displayName,
            position: Int(category.position),
            iconResource: category.iconResource,
            colorHex: category.colorHex,
            isDefault: category.isDefault,
            createdAt: Int64(category.createdAt.timeIntervalSince1970 * 1000)
        )
    }
}

// MARK: - Backup Photo
struct BackupPhoto: Codable {
    let id: Int64
    let path: String
    let categoryId: Int64
    let name: String
    let isFromAssets: Bool
    let createdAt: Int64
    let fileSize: Int64
    let width: Int
    let height: Int

    func toPhoto() -> Photo {
        Photo(
            id: id,
            path: path,
            categoryId: categoryId,
            name: name,
            isFromAssets: isFromAssets,
            createdAt: Date(timeIntervalSince1970: TimeInterval(createdAt / 1000)),
            fileSize: fileSize,
            width: Int32(width),
            height: Int32(height),
        )
    }

    static func fromPhoto(_ photo: Photo) -> BackupPhoto {
        BackupPhoto(
            id: photo.id,
            path: photo.path,
            categoryId: photo.categoryId,
            name: photo.name,
            isFromAssets: photo.isFromAssets,
            createdAt: Int64(photo.createdAt.timeIntervalSince1970 * 1000),
            fileSize: photo.fileSize,
            width: Int(photo.width),
            height: Int(photo.height),
        )
    }
}

// MARK: - Backup Settings
struct BackupSettings: Codable {
    let isDarkMode: Bool
    let securitySettings: BackupSecuritySettings
}

// MARK: - Security Settings
struct BackupSecuritySettings: Codable {
    let hasPIN: Bool
    let hasPattern: Bool
    let kidSafeModeEnabled: Bool
    let cameraAccessAllowed: Bool
    let deleteProtectionEnabled: Bool
}

// MARK: - Photo Manifest Entry
struct PhotoManifestEntry: Codable {
    let photoId: Int64
    let originalPath: String
    let zipEntryName: String
    let fileName: String
    let fileSize: Int64
    let checksum: String?
}

// MARK: - Import Progress
struct ImportProgress {
    let totalItems: Int
    let processedItems: Int
    let currentOperation: String
    let errors: [String]

    var percentage: Int {
        totalItems > 0 ? (processedItems * 100) / totalItems : 0
    }
}

// MARK: - Import Result
struct ImportResult {
    let success: Bool
    let categoriesImported: Int
    let photosImported: Int
    let photosSkipped: Int
    let photoFilesRestored: Int
    let errors: [String]
    let warnings: [String]
}

// MARK: - Backup Validation Result
struct BackupValidationResult {
    let isValid: Bool
    let version: String?
    let format: BackupFormat
    let categoriesFound: Int
    let photosFound: Int
    let errors: [String]
    let warnings: [String]
}

// MARK: - Backup Preview
struct BackupPreview {
    let version: Int
    let exportDate: Int64
    let appVersion: String
    let categoriesCount: Int
    let photosCount: Int
    let missingPhotosCount: Int
    let missingPhotos: [String]
    let isZipFormat: Bool
}

// MARK: - Backup Statistics
struct BackupStats {
    let categoryCount: Int
    let photoCount: Int
    let estimatedSize: Int64
    let success: Bool
    let errorMessage: String?
}

// MARK: - Backup Error
enum BackupError: LocalizedError {
    case unsupportedVersion(Int)
    case invalidFormat
    case fileNotFound(String)
    case exportFailed(String)
    case importFailed(String)
    case compressionFailed
    case checksumMismatch
    case insufficientStorage

    var errorDescription: String? {
        switch self {
        case .unsupportedVersion(let version):
            return "Unsupported backup version: \(version). Supported versions: \(MIN_SUPPORTED_VERSION)-\(MAX_SUPPORTED_VERSION)"
        case .invalidFormat:
            return "Invalid backup file format"
        case .fileNotFound(let path):
            return "File not found: \(path)"
        case .exportFailed(let reason):
            return "Export failed: \(reason)"
        case .importFailed(let reason):
            return "Import failed: \(reason)"
        case .compressionFailed:
            return "Failed to compress backup data"
        case .checksumMismatch:
            return "File integrity check failed"
        case .insufficientStorage:
            return "Insufficient storage space for backup"
        }
    }
}

// MARK: - Export Progress
struct ExportProgress {
    let current: Int
    let total: Int
    let operation: String
    let percentage: Int

    init(current: Int, total: Int, operation: String) {
        self.current = current
        self.total = total
        self.operation = operation
        self.percentage = total > 0 ? (current * 100) / total : 0
    }
}

// MARK: - Schedule Frequency
enum BackupScheduleFrequency: String, CaseIterable {
    case daily = "Daily"
    case weekly = "Weekly"
    case monthly = "Monthly"
    case disabled = "Disabled"

    var interval: TimeInterval? {
        switch self {
        case .daily:
            return 24 * 60 * 60
        case .weekly:
            return 7 * 24 * 60 * 60
        case .monthly:
            return 30 * 24 * 60 * 60
        case .disabled:
            return nil
        }
    }
}

// MARK: - Backup Options
struct BackupOptions {
    let includePhotos: Bool
    let includeSettings: Bool
    let includeCategories: Bool
    let compressionLevel: CompressionLevel
    let dateRange: DateRange?
    let categoryFilter: [String]?

    enum CompressionLevel: Int {
        case none = 0
        case fast = 1
        case normal = 5
        case maximum = 9
    }

    struct DateRange {
        let start: Date
        let end: Date
    }

    static let `default` = BackupOptions(
        includePhotos: true,
        includeSettings: true,
        includeCategories: true,
        compressionLevel: .normal,
        dateRange: nil,
        categoryFilter: nil
    )
}