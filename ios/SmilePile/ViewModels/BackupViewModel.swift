// Temporary stub - BackupViewModel has multiple ExportManager dependency errors
// TODO: Create proper SettingsViewModel with export/import UI
// Issues: ExportManager methods don't exist, Repository.shared pattern wrong, async/sync mismatches

import Foundation
import SwiftUI

@MainActor
class BackupViewModel: ObservableObject {
    @Published var isExporting = false
    @Published var isImporting = false
    @Published var exportProgress: Double = 0
    @Published var importProgress: Double = 0
    @Published var errorMessage: String?

    static let shared = BackupViewModel()
    private init() {}
}
