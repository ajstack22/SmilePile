import Foundation

/// Utility class for generating unique photo IDs
/// Ensures consistent Int64 ID generation across the app
final class PhotoIDGenerator {

    /// Thread-safe counter for auto-increment IDs
    private static var counter: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    private static let queue = DispatchQueue(label: "com.smilepile.idgenerator", attributes: .concurrent)

    /// Generates a unique Int64 ID using auto-increment
    /// - Returns: A unique Int64 ID that won't collide
    static func generateUniqueID() -> Int64 {
        // Use auto-increment for guaranteed uniqueness
        return queue.sync(flags: .barrier) {
            counter += 1
            return counter
        }
    }

    /// Converts a UUID string to Int64 ID safely
    /// - Parameter uuidString: The UUID string to convert
    /// - Returns: An Int64 ID derived from the UUID
    static func idFromUUID(_ uuidString: String) -> Int64 {
        // For migration: convert UUID to stable Int64
        // Use stable hash that won't change across runs
        var hash: Int64 = 5381
        for char in uuidString.utf8 {
            hash = ((hash << 5) &+ hash) &+ Int64(char)
        }
        return abs(hash)
    }

    /// Validates if an ID is valid (non-zero and positive)
    /// - Parameter id: The ID to validate
    /// - Returns: True if the ID is valid
    static func isValidID(_ id: Int64) -> Bool {
        return id > 0
    }

    /// Generates an ID for a photo based on its path
    /// Useful for ensuring the same photo doesn't get duplicate IDs
    /// - Parameter path: The file path of the photo
    /// - Returns: A deterministic Int64 ID based on the path
    static func idFromPath(_ path: String) -> Int64 {
        // Use stable hash for path-based IDs
        let data = Data(path.utf8)
        var result: Int64 = 0

        data.withUnsafeBytes { bytes in
            var hash: Int64 = 5381
            for byte in bytes {
                hash = ((hash << 5) &+ hash) &+ Int64(byte)
            }
            result = abs(hash)
        }

        return result > 0 ? result : generateUniqueID()
    }
}