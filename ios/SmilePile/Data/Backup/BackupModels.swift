import Foundation

// MARK: - Constants

let CURRENT_BACKUP_VERSION = 2

// MARK: - Enums

enum BackupFormat: String, Codable {
    case json = "JSON"
    case zip = "ZIP"
}

enum ImportStrategy: String, Codable {
    case replace = "REPLACE"
    case merge = "MERGE"
    case skip = "SKIP"
}

enum DuplicateResolution: String, Codable {
    case skip = "SKIP"
    case replace = "REPLACE"
    case rename = "RENAME"
    case askUser = "ASK_USER"
}

enum CompressionLevel: String, Codable {
    case low = "LOW"
    case medium = "MEDIUM"
    case high = "HIGH"
}

// MARK: - Main Backup Structure

struct AppBackup: Codable {
    let version: Int
    let exportDate: Int64
    let appVersion: String
    let format: String
    let categories: [BackupCategory]
    let photos: [BackupPhoto]
    let settings: BackupSettings
    let photoManifest: [PhotoManifestEntry]

    init(
        version: Int = CURRENT_BACKUP_VERSION,
        exportDate: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        appVersion: String = "",
        format: String = BackupFormat.zip.rawValue,
        categories: [BackupCategory],
        photos: [BackupPhoto],
        settings: BackupSettings,
        photoManifest: [PhotoManifestEntry] = []
    ) {
        self.version = version
        self.exportDate = exportDate
        self.appVersion = appVersion
        self.format = format
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
        return Category(
            id: id,
            name: name,
            displayName: displayName,
            position: position,
            iconResource: iconResource,
            colorHex: colorHex,
            isDefault: isDefault,
            createdAt: createdAt
        )
    }

    static func fromCategory(_ category: Category) -> BackupCategory {
        return BackupCategory(
            id: category.id,
            name: category.name,
            displayName: category.displayName,
            position: category.position,
            iconResource: category.iconResource,
            colorHex: category.colorHex,
            isDefault: category.isDefault,
            createdAt: category.createdAt
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
        return Photo(
            id: id,
            path: path,
            categoryId: categoryId,
            name: name,
            isFromAssets: isFromAssets,
            createdAt: createdAt,
            fileSize: fileSize,
            width: width,
            height: height
        )
    }

    static func fromPhoto(_ photo: Photo) -> BackupPhoto {
        return BackupPhoto(
            id: photo.id,
            path: photo.path,
            categoryId: photo.categoryId,
            name: photo.name,
            isFromAssets: photo.isFromAssets,
            createdAt: photo.createdAt,
            fileSize: photo.fileSize,
            width: photo.width,
            height: photo.height
        )
    }
}

// MARK: - Backup Settings

struct BackupSettings: Codable {
    let isDarkMode: Bool
    let securitySettings: BackupSecuritySettings
}

struct BackupSecuritySettings: Codable {
    let hasPIN: Bool
    let hasPattern: Bool
    let kidSafeModeEnabled: Bool
    let deleteProtectionEnabled: Bool
}

// MARK: - Photo Manifest

struct PhotoManifestEntry: Codable {
    let photoId: Int64
    let originalPath: String
    let zipEntryName: String
    let fileName: String
    let fileSize: Int64
    let checksum: String?
}

// MARK: - Progress & Results

struct ImportProgress {
    let totalItems: Int
    let processedItems: Int
    let currentOperation: String
    let errors: [String]

    var percentage: Int {
        guard totalItems > 0 else { return 0 }
        return (processedItems * 100) / totalItems
    }
}

struct ImportResult {
    let success: Bool
    let categoriesImported: Int
    let photosImported: Int
    let photosSkipped: Int
    let photoFilesRestored: Int
    let errors: [String]
    let warnings: [String]

    init(
        success: Bool,
        categoriesImported: Int = 0,
        photosImported: Int = 0,
        photosSkipped: Int = 0,
        photoFilesRestored: Int = 0,
        errors: [String] = [],
        warnings: [String] = []
    ) {
        self.success = success
        self.categoriesImported = categoriesImported
        self.photosImported = photosImported
        self.photosSkipped = photosSkipped
        self.photoFilesRestored = photoFilesRestored
        self.errors = errors
        self.warnings = warnings
    }
}

struct ExportProgress {
    let totalItems: Int
    let processedItems: Int
    let currentOperation: String
    let currentFile: String?
    let bytesProcessed: Int64
    let totalBytes: Int64
    let errors: [String]

    var percentage: Int {
        guard totalItems > 0 else { return 0 }
        return (processedItems * 100) / totalItems
    }

    var bytesPercentage: Int {
        guard totalBytes > 0 else { return 0 }
        return Int((bytesProcessed * 100) / totalBytes)
    }
}

// MARK: - Validation

struct BackupValidationResult {
    let isValid: Bool
    let version: Int
    let format: BackupFormat
    let hasMetadata: Bool
    let hasPhotos: Bool
    let photosCount: Int
    let categoriesCount: Int
    let integrityCheckPassed: Bool
    let errors: [String]
    let warnings: [String]

    init(
        isValid: Bool,
        version: Int = 0,
        format: BackupFormat = .zip,
        hasMetadata: Bool = false,
        hasPhotos: Bool = false,
        photosCount: Int = 0,
        categoriesCount: Int = 0,
        integrityCheckPassed: Bool = false,
        errors: [String] = [],
        warnings: [String] = []
    ) {
        self.isValid = isValid
        self.version = version
        self.format = format
        self.hasMetadata = hasMetadata
        self.hasPhotos = hasPhotos
        self.photosCount = photosCount
        self.categoriesCount = categoriesCount
        self.integrityCheckPassed = integrityCheckPassed
        self.errors = errors
        self.warnings = warnings
    }
}

// MARK: - Restore Options

struct RestoreOptions: Codable {
    let strategy: ImportStrategy
    let duplicateResolution: DuplicateResolution
    let validateIntegrity: Bool
    let restoreThumbnails: Bool
    let restoreSettings: Bool
    let dryRun: Bool

    init(
        strategy: ImportStrategy = .merge,
        duplicateResolution: DuplicateResolution = .skip,
        validateIntegrity: Bool = true,
        restoreThumbnails: Bool = true,
        restoreSettings: Bool = true,
        dryRun: Bool = false
    ) {
        self.strategy = strategy
        self.duplicateResolution = duplicateResolution
        self.validateIntegrity = validateIntegrity
        self.restoreThumbnails = restoreThumbnails
        self.restoreSettings = restoreSettings
        self.dryRun = dryRun
    }
}

// MARK: - Backup Options

struct BackupOptions: Codable {
    let includePhotos: Bool
    let includeThumbnails: Bool
    let includeSettings: Bool
    let selectedCategories: [Int64]?
    let dateRangeStart: Int64?
    let dateRangeEnd: Int64?
    let compressionLevel: CompressionLevel
    let includeMetadata: Bool

    init(
        includePhotos: Bool = true,
        includeThumbnails: Bool = true,
        includeSettings: Bool = true,
        selectedCategories: [Int64]? = nil,
        dateRangeStart: Int64? = nil,
        dateRangeEnd: Int64? = nil,
        compressionLevel: CompressionLevel = .medium,
        includeMetadata: Bool = true
    ) {
        self.includePhotos = includePhotos
        self.includeThumbnails = includeThumbnails
        self.includeSettings = includeSettings
        self.selectedCategories = selectedCategories
        self.dateRangeStart = dateRangeStart
        self.dateRangeEnd = dateRangeEnd
        self.compressionLevel = compressionLevel
        self.includeMetadata = includeMetadata
    }
}
