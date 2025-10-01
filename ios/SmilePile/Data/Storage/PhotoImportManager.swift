// Temporary stub - PhotoImportManager has multiple compilation errors
// TODO: Implement proper photo import with batch limits and duplicate detection

import Foundation
import Photos

class PhotoImportManager {
    struct Configuration {
        static let maxPhotosPerBatch = 50
    }

    static let shared = PhotoImportManager()
    private init() {}
}
