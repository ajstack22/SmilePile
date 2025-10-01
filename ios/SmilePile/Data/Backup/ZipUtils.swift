// Temporary stub - ZipUtils has compilation errors
// TODO: Fix Archive framework generic parameter inference

import Foundation

class ZipUtils {
    static func createZip(from: URL, to: URL, progress: ((Double) -> Void)?) async throws {
        throw NSError(domain: "ZipUtils", code: -1, userInfo: [NSLocalizedDescriptionKey: "Not implemented"])
    }

    static func extractZip(from: URL, to: URL, progress: ((Double) -> Void)?) async throws {
        throw NSError(domain: "ZipUtils", code: -1, userInfo: [NSLocalizedDescriptionKey: "Not implemented"])
    }
}
