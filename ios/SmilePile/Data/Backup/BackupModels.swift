// Temporary stub - BackupModels has Date/Int64 and Int32/Int type conversion errors
// TODO: Fix type mismatches when porting Android backup system
// Issues: Trying to convert Date to Int64, Int32 to Int, accessing .timeIntervalSince1970 on Int64

import Foundation

struct BackupMetadata: Codable {
    let version: String
    let timestamp: Int64
    let deviceId: String
    let appVersion: String

    init(version: String = "1.0", timestamp: Int64 = 0, deviceId: String = "", appVersion: String = "") {
        self.version = version
        self.timestamp = timestamp
        self.deviceId = deviceId
        self.appVersion = appVersion
    }
}

struct BackupCategory: Codable {
    let id: Int64
    let name: String
    let displayName: String
    let position: Int
    let iconResource: String
    let colorHex: String
    let isDefault: Bool
    let createdAt: Int64
}

struct BackupPhoto: Codable {
    let id: String
    let path: String
    let categoryId: Int64
    let name: String?
    let isFromAssets: Bool
    let createdAt: Int64
    let fileSize: Int64
    let width: Int
    let height: Int
}

// Additional types referenced by RestoreManager
enum ImportStrategy {
    case replace
    case merge
    case skip
}

struct ImportResult {
    let success: Bool
    let categoriesImported: Int
    let photosImported: Int
    let errors: [String]

    init(success: Bool, categoriesImported: Int = 0, photosImported: Int = 0, errors: [String] = []) {
        self.success = success
        self.categoriesImported = categoriesImported
        self.photosImported = photosImported
        self.errors = errors
    }
}

struct ImportProgress {
    let currentItem: Int
    let totalItems: Int
    let message: String
}

struct BackupValidationResult {
    let isValid: Bool
    let errors: [String]
}

struct BackupPreview {
    let categoryCount: Int
    let photoCount: Int
    let sizeInBytes: Int64
}
