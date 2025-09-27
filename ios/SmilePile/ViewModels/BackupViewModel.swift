import Foundation
import SwiftUI
import UniformTypeIdentifiers
import Combine

// MARK: - Backup View Model
@MainActor
class BackupViewModel: ObservableObject {

    // MARK: - Published Properties

    // Export State
    @Published var isExporting = false
    @Published var exportProgress: ExportProgress?
    @Published var exportError: String?
    @Published var lastExportedFile: URL?

    // Import State
    @Published var isImporting = false
    @Published var importProgress: ImportProgress?
    @Published var importError: String?
    @Published var importPreview: BackupPreview?

    // Backup Stats
    @Published var backupStats: BackupStats?
    @Published var backupHistory: [BackupHistoryEntry] = []
    @Published var lastBackupDate: Date?

    // Schedule Settings
    @Published var isScheduleEnabled = false
    @Published var scheduleFrequency: BackupScheduleFrequency = .weekly
    @Published var nextScheduledBackup: Date?
    @Published var requiresWiFi = true
    @Published var requiresCharging = false

    // iCloud Settings
    @Published var iCloudBackupEnabled = false
    @Published var iCloudAvailable = false

    // UI State
    @Published var showingExportOptions = false
    @Published var showingImportPicker = false
    @Published var showingShareSheet = false
    @Published var selectedExportFormat: ExportFormat = .zip
    @Published var selectedImportStrategy: ImportStrategy = .merge
    @Published var selectedCategories: Set<String> = []
    @Published var dateRangeStart: Date = Date().addingTimeInterval(-30 * 24 * 60 * 60) // 30 days ago
    @Published var dateRangeEnd: Date = Date()
    @Published var compressionLevel: BackupOptions.CompressionLevel = .normal

    // MARK: - Private Properties
    private let backupManager = BackupManager.shared
    private let restoreManager = RestoreManager.shared
    private let exportManager = ExportManager.shared
    private let scheduler = BackupScheduler.shared
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Export Format
    enum ExportFormat: String, CaseIterable {
        case zip = "Full Backup (ZIP)"
        case json = "Metadata Only (JSON)"
        case photos = "Photos Only"
        case htmlGallery = "HTML Gallery"
        case pdfCatalog = "PDF Catalog"

        var icon: String {
            switch self {
            case .zip: return "doc.zipper"
            case .json: return "doc.text"
            case .photos: return "photo.stack"
            case .htmlGallery: return "globe"
            case .pdfCatalog: return "doc.richtext"
            }
        }
    }

    // MARK: - Initialization
    init() {
        setupBindings()
        Task {
            await loadBackupData()
        }
    }

    // MARK: - Setup
    private func setupBindings() {
        // Bind to backup manager
        backupManager.$isExporting
            .assign(to: &$isExporting)

        backupManager.$exportProgress
            .assign(to: &$exportProgress)

        backupManager.$lastBackupDate
            .assign(to: &$lastBackupDate)

        backupManager.$backupHistory
            .assign(to: &$backupHistory)

        // Bind to restore manager
        restoreManager.$isImporting
            .assign(to: &$isImporting)

        restoreManager.$importProgress
            .assign(to: &$importProgress)

        // Bind to scheduler
        scheduler.$isScheduleEnabled
            .assign(to: &$isScheduleEnabled)

        scheduler.$scheduleFrequency
            .assign(to: &$scheduleFrequency)

        scheduler.$nextScheduledBackup
            .assign(to: &$nextScheduledBackup)

        scheduler.$requiresWiFi
            .assign(to: &$requiresWiFi)

        scheduler.$requiresCharging
            .assign(to: &$requiresCharging)

        // Check iCloud availability
        checkiCloudAvailability()
    }

    // MARK: - Export Methods

    func exportBackup() async {
        exportError = nil

        do {
            let options = createBackupOptions()

            switch selectedExportFormat {
            case .zip:
                lastExportedFile = try await backupManager.exportToZip(options: options) { current, total, operation in
                    await MainActor.run {
                        self.exportProgress = ExportProgress(current: current, total: total, operation: operation)
                    }
                }
                showingShareSheet = true

            case .json:
                let jsonString = try await backupManager.exportToJson()
                let tempFile = FileManager.default.temporaryDirectory
                    .appendingPathComponent("backup_\(Date().timeIntervalSince1970).json")
                try jsonString.write(to: tempFile, atomically: true, encoding: .utf8)
                lastExportedFile = tempFile
                showingShareSheet = true

            case .photos:
                let categories = try await CategoryRepository.shared.getAllCategories()
                let selectedCats = categories.filter { selectedCategories.contains($0.name) }
                if let firstCategory = selectedCats.first {
                    lastExportedFile = try await exportManager.exportCategory(
                        firstCategory,
                        format: .photos,
                        compressionQuality: compressionQualityForLevel()
                    )
                    showingShareSheet = true
                }

            case .htmlGallery:
                let photos = try await getFilteredPhotos()
                lastExportedFile = try await exportManager.exportSelectedPhotos(
                    photos,
                    format: .htmlGallery,
                    compressionQuality: compressionQualityForLevel()
                )
                showingShareSheet = true

            case .pdfCatalog:
                let photos = try await getFilteredPhotos()
                lastExportedFile = try await exportManager.exportSelectedPhotos(
                    photos,
                    format: .pdfCatalog,
                    compressionQuality: compressionQualityForLevel()
                )
                showingShareSheet = true
            }

            await loadBackupData()

        } catch {
            exportError = error.localizedDescription
        }
    }

    func exportCategory(_ category: Category) async {
        exportError = nil

        do {
            lastExportedFile = try await exportManager.exportCategory(
                category,
                format: .photosWithMetadata,
                compressionQuality: compressionQualityForLevel()
            )
            showingShareSheet = true
        } catch {
            exportError = error.localizedDescription
        }
    }

    func exportDateRange() async {
        exportError = nil

        do {
            lastExportedFile = try await exportManager.exportByDateRange(
                startDate: dateRangeStart,
                endDate: dateRangeEnd,
                format: .photosWithMetadata,
                compressionQuality: compressionQualityForLevel()
            )
            showingShareSheet = true
        } catch {
            exportError = error.localizedDescription
        }
    }

    // MARK: - Import Methods

    func importBackup(from url: URL) async {
        importError = nil
        isImporting = true

        do {
            // First validate and preview
            let previewResult = await restoreManager.validateBackupFile(url)
            switch previewResult {
            case .success(let preview):
                importPreview = preview

            case .failure(let error):
                throw error
            }

            // Perform import based on file type
            let fileName = url.lastPathComponent.lowercased()
            let result: ImportResult

            if fileName.hasSuffix(".zip") || fileName.hasSuffix(".smilepile") {
                result = try await restoreManager.importFromZip(
                    zipFile: url,
                    strategy: selectedImportStrategy
                ) { current, total, operation in
                    await MainActor.run {
                        self.importProgress = ImportProgress(
                            totalItems: total,
                            processedItems: current,
                            currentOperation: operation,
                            errors: []
                        )
                    }
                }
            } else if fileName.hasSuffix(".json") {
                let jsonData = try Data(contentsOf: url)
                let jsonString = String(data: jsonData, encoding: .utf8)!
                result = try await restoreManager.importFromJson(
                    jsonString: jsonString,
                    strategy: selectedImportStrategy
                )
            } else {
                throw BackupError.invalidFormat
            }

            // Show results
            if !result.errors.isEmpty {
                importError = "Import completed with errors:\n" + result.errors.joined(separator: "\n")
            } else {
                importError = nil
            }

            await loadBackupData()

        } catch {
            importError = error.localizedDescription
        }

        isImporting = false
    }

    // MARK: - Schedule Methods

    func enableSchedule() {
        scheduler.enableSchedule(frequency: scheduleFrequency)
    }

    func disableSchedule() {
        scheduler.disableSchedule()
    }

    func performManualBackup() async {
        await scheduler.performImmediateBackup()
        await loadBackupData()
    }

    // MARK: - iCloud Methods

    func toggleiCloudBackup() {
        iCloudBackupEnabled.toggle()
        UserDefaults.standard.set(iCloudBackupEnabled, forKey: "enableiCloudBackup")
    }

    private func checkiCloudAvailability() {
        iCloudAvailable = FileManager.default.url(forUbiquityContainerIdentifier: nil) != nil
        iCloudBackupEnabled = UserDefaults.standard.bool(forKey: "enableiCloudBackup")
    }

    // MARK: - Data Loading

    func loadBackupData() async {
        backupStats = await backupManager.getBackupStats()
    }

    func cleanupOldBackups(keepLast: Int = 10) async {
        await backupManager.cleanupOldBackups(keepLast: keepLast)
        await loadBackupData()
    }

    // MARK: - Helper Methods

    private func createBackupOptions() -> BackupOptions {
        BackupOptions(
            includePhotos: true,
            includeSettings: true,
            includeCategories: true,
            compressionLevel: compressionLevel,
            dateRange: shouldFilterByDate() ? BackupOptions.DateRange(
                start: dateRangeStart,
                end: dateRangeEnd
            ) : nil,
            categoryFilter: selectedCategories.isEmpty ? nil : Array(selectedCategories)
        )
    }

    private func shouldFilterByDate() -> Bool {
        // Check if date range is not default (last 30 days)
        return dateRangeStart != Date().addingTimeInterval(-30 * 24 * 60 * 60) ||
               dateRangeEnd != Date()
    }

    private func compressionQualityForLevel() -> CGFloat {
        switch compressionLevel {
        case .none:
            return 1.0
        case .fast:
            return 0.95
        case .normal:
            return 0.85
        case .maximum:
            return 0.7
        }
    }

    private func getFilteredPhotos() async throws -> [Photo] {
        let allPhotos = try await PhotoRepository.shared.getAllPhotos()

        // Build category lookup map to avoid circular dependencies and repeated lookups
        var categoryLookup: [Int64: Category] = [:]
        if !selectedCategories.isEmpty {
            do {
                let allCategories = try await CategoryRepository.shared.getAllCategories()
                for category in allCategories {
                    categoryLookup[category.id] = category
                }

                // Log category mapping for debugging
                print("[BackupViewModel] Loaded \(categoryLookup.count) categories for filtering")
            } catch {
                print("[BackupViewModel] Error loading categories for filtering: \(error)")
                // Continue without category filtering if there's an error
                return allPhotos
            }
        }

        var filteredPhotos: [Photo] = []
        var excludedByCategory = 0
        var excludedByDate = 0

        for photo in allPhotos {
            var shouldInclude = true

            // Filter by category if selected
            if !selectedCategories.isEmpty {
                // Look up category by ID to get its name
                guard let category = categoryLookup[photo.categoryId] else {
                    // Category not found - exclude photo
                    excludedByCategory += 1
                    shouldInclude = false
                    continue
                }

                // Check if category name is in selected categories
                if !selectedCategories.contains(category.name) {
                    excludedByCategory += 1
                    shouldInclude = false
                    continue
                }
            }

            // Filter by date range
            if shouldFilterByDate() {
                if photo.createdAt < dateRangeStart || photo.createdAt > dateRangeEnd {
                    excludedByDate += 1
                    shouldInclude = false
                    continue
                }
            }

            if shouldInclude {
                filteredPhotos.append(photo)
            }
        }

        // Log filtering results for debugging
        print("[BackupViewModel] Filtered photos: \(filteredPhotos.count) included, \(excludedByCategory) excluded by category, \(excludedByDate) excluded by date")

        return filteredPhotos
    }

    // MARK: - Formatting

    func formatFileSize(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }

    func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    func getBackupSummary() -> String {
        guard let stats = backupStats else {
            return "No backup data available"
        }

        return "\(stats.categoryCount) categories, \(stats.photoCount) photos • \(formatFileSize(stats.estimatedSize))"
    }

    // MARK: - Document Picker

    func presentDocumentPicker() -> DocumentPicker {
        DocumentPicker(
            allowedContentTypes: [
                UTType(filenameExtension: "zip") ?? .data,
                UTType(filenameExtension: "json") ?? .json,
                UTType(filenameExtension: "smilepile") ?? .data
            ],
            onPick: { url in
                Task {
                    await self.importBackup(from: url)
                }
            }
        )
    }
}

// MARK: - Document Picker
struct DocumentPicker: UIViewControllerRepresentable {
    let allowedContentTypes: [UTType]
    let onPick: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: allowedContentTypes)
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: DocumentPicker

        init(_ parent: DocumentPicker) {
            self.parent = parent
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }

            // Start accessing security-scoped resource
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }

            parent.onPick(url)
        }
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    let onDismiss: (() -> Void)?

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )

        controller.completionWithItemsHandler = { _, _, _, _ in
            onDismiss?()
        }

        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Preview Provider
#if DEBUG
extension BackupViewModel {
    static var preview: BackupViewModel {
        let viewModel = BackupViewModel()
        viewModel.backupStats = BackupStats(
            categoryCount: 5,
            photoCount: 123,
            estimatedSize: 1024 * 1024 * 50, // 50MB
            success: true,
            errorMessage: nil
        )
        viewModel.lastBackupDate = Date().addingTimeInterval(-3600)
        return viewModel
    }
}
#endif