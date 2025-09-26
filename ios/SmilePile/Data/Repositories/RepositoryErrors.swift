import Foundation

enum PhotoRepositoryError: LocalizedError {
    case invalidCategory(String)
    case insertFailed(String)
    case updateFailed(String)
    case deleteFailed(String)
    case fetchFailed(String)
    case notFound(String)
    case invalidInput(String)
    case encryptionFailed(String)
    case decryptionFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidCategory(let message),
             .insertFailed(let message),
             .updateFailed(let message),
             .deleteFailed(let message),
             .fetchFailed(let message),
             .notFound(let message),
             .invalidInput(let message),
             .encryptionFailed(let message),
             .decryptionFailed(let message):
            return message
        }
    }
}

enum CategoryRepositoryError: LocalizedError {
    case insertFailed(String)
    case updateFailed(String)
    case deleteFailed(String)
    case fetchFailed(String)
    case notFound(String)
    case initializationFailed(String)
    case duplicateName(String)
    case invalidInput(String)
    case defaultCategoryModification(String)

    var errorDescription: String? {
        switch self {
        case .insertFailed(let message),
             .updateFailed(let message),
             .deleteFailed(let message),
             .fetchFailed(let message),
             .notFound(let message),
             .initializationFailed(let message),
             .duplicateName(let message),
             .invalidInput(let message),
             .defaultCategoryModification(let message):
            return message
        }
    }
}

enum StorageError: LocalizedError {
    case importFailed(String)
    case saveFailed(String)
    case deleteFailed(String)
    case insufficientSpace(String)
    case fileNotFound(String)
    case thumbnailGenerationFailed(String)
    case invalidImageData(String)
    case permissionDenied(String)
    case directoryCreationFailed(String)

    var errorDescription: String? {
        switch self {
        case .importFailed(let message),
             .saveFailed(let message),
             .deleteFailed(let message),
             .insufficientSpace(let message),
             .fileNotFound(let message),
             .thumbnailGenerationFailed(let message),
             .invalidImageData(let message),
             .permissionDenied(let message),
             .directoryCreationFailed(let message):
            return message
        }
    }
}