import Foundation
import SwiftUI
import UniformTypeIdentifiers

@MainActor
class BackupViewModel: ObservableObject {
    // Export state
    @Published var isExporting = false
    @Published var exportProgress: Double = 0
    @Published var exportMessage: String = ""
    @Published var exportError: Error?
    @Published var exportedFileURL: URL?
    @Published var showShareSheet = false

    // Import state
    @Published var isImporting = false
    @Published var importProgress: Double = 0
    @Published var importMessage: String = ""
    @Published var importError: Error?
    @Published var showImportConfirmation = false
    @Published var backupValidationResult: BackupValidationResult?
    @Published var showDocumentPicker = false
    @Published var importSuccess = false
    @Published var importResult: ImportResult?

    private var selectedImportURL: URL?
    private let backupManager: BackupManager
    private let restoreManager: RestoreManager

    init(
        backupManager: BackupManager = BackupManager.shared,
        restoreManager: RestoreManager = RestoreManager.shared
    ) {
        self.backupManager = backupManager
        self.restoreManager = restoreManager
    }

    // MARK: - Export

    func exportData() {
        Task {
            do {
                isExporting = true
                exportError = nil
                exportProgress = 0
                exportMessage = "Starting export..."

                let zipURL = try await backupManager.createBackup { progress in
                    Task { @MainActor in
                        self.exportProgress = Double(progress.processedItems) / 100.0
                        self.exportMessage = progress.currentOperation
                    }
                }

                exportedFileURL = zipURL
                exportMessage = "Export complete!"
                exportProgress = 1.0

                // Show share sheet after short delay
                try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
                showShareSheet = true

            } catch {
                exportError = error
                exportMessage = "Export failed: \(error.localizedDescription)"
            }

            isExporting = false
        }
    }

    func dismissShareSheet() {
        showShareSheet = false
        // Clean up exported file after sharing
        if let url = exportedFileURL {
            try? FileManager.default.removeItem(at: url)
            exportedFileURL = nil
        }
    }

    // MARK: - Import

    func showFilePicker() {
        showDocumentPicker = true
    }

    func handleSelectedFile(_ url: URL) {
        selectedImportURL = url

        Task {
            do {
                startValidation()
                let validationResult = try await restoreManager.validateBackup(at: url)
                backupValidationResult = validationResult

                handleValidationResult(validationResult)
                isImporting = false
            } catch {
                handleValidationError(error)
            }
        }
    }

    func confirmImport() {
        guard let url = selectedImportURL else { return }

        Task {
            do {
                startImport()
                guard backupValidationResult != nil else { return }

                let result = try await performRestore(from: url)
                handleImportSuccess(result)
            } catch {
                handleImportError(error)
            }

            isImporting = false
        }
    }

    func cancelImport() {
        showImportConfirmation = false
        backupValidationResult = nil
        isImporting = false
    }

    func dismissImportSuccess() {
        importSuccess = false
        importResult = nil
        backupValidationResult = nil
    }

    // MARK: - Helper Methods

    private func startValidation() {
        isImporting = true
        importError = nil
        importMessage = "Validating backup..."
    }

    private func handleValidationResult(_ result: BackupValidationResult) {
        if result.isValid {
            showImportConfirmation = true
        } else {
            importError = createValidationError(from: result.errors)
            importMessage = "Invalid backup file"
        }
    }

    private func handleValidationError(_ error: Error) {
        importError = error
        importMessage = "Validation failed: \(error.localizedDescription)"
        isImporting = false
    }

    private func createValidationError(from errors: [String]) -> NSError {
        NSError(
            domain: "BackupViewModel",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: errors.joined(separator: "\n")]
        )
    }

    private func startImport() {
        isImporting = true
        importError = nil
        importProgress = 0
        importMessage = "Starting import..."
        showImportConfirmation = false
    }

    private func performRestore(from url: URL) async throws -> ImportResult {
        try await restoreManager.restoreBackup(
            from: url,
            options: createRestoreOptions()
        ) { progress in
            Task { @MainActor in
                self.importProgress = Double(progress.processedItems) / 100.0
                self.importMessage = progress.currentOperation
            }
        }
    }

    private func createRestoreOptions() -> RestoreOptions {
        RestoreOptions(
            strategy: .merge,
            duplicateResolution: .skip,
            validateIntegrity: true,
            restoreThumbnails: true,
            restoreSettings: true,
            dryRun: false
        )
    }

    private func handleImportSuccess(_ result: ImportResult) {
        importResult = result
        importMessage = "Import complete! \(result.photosImported) photos restored."
        importProgress = 1.0
        importSuccess = true
    }

    private func handleImportError(_ error: Error) {
        importError = error
        importMessage = "Import failed: \(error.localizedDescription)"
    }
}

// MARK: - Document Picker Coordinator

struct DocumentPickerView: UIViewControllerRepresentable {
    @Binding var selectedURL: URL?
    let onSelect: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: [.zip],
            asCopy: true
        )
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: DocumentPickerView

        init(_ parent: DocumentPickerView) {
            self.parent = parent
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            parent.selectedURL = url
            parent.onSelect(url)
        }

        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            // User cancelled
        }
    }
}

// Note: ShareSheet is defined in ShareManager.swift
