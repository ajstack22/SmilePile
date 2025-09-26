import Foundation

extension FileManager {

    /// SmilePile's document directory
    var smilePileDirectory: URL {
        urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("SmilePile")
    }

    /// Photos storage directory
    var photosDirectory: URL {
        smilePileDirectory.appendingPathComponent("photos")
    }

    /// Thumbnails storage directory
    var thumbnailsDirectory: URL {
        smilePileDirectory.appendingPathComponent("thumbnails")
    }

    /// Create all necessary SmilePile directories
    func createSmilePileDirectories() throws {
        try createDirectory(at: smilePileDirectory, withIntermediateDirectories: true)
        try createDirectory(at: photosDirectory, withIntermediateDirectories: true)
        try createDirectory(at: thumbnailsDirectory, withIntermediateDirectories: true)
    }

    /// Check if SmilePile directories exist
    func checkSmilePileDirectories() -> Bool {
        fileExists(atPath: smilePileDirectory.path) &&
        fileExists(atPath: photosDirectory.path) &&
        fileExists(atPath: thumbnailsDirectory.path)
    }
}