// Temporary stub - BackupManager has multiple compilation errors
// TODO: Port Android's working BackupManager.kt to iOS
// Issues: ZipUtils methods don't exist, Date/Int64 type mismatches, missing SettingsManager.isDarkMode

import Foundation

class BackupManager {
    static let shared = BackupManager()
    private init() {}
}
