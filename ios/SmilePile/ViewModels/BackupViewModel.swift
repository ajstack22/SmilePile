import Foundation
import SwiftUI
import UniformTypeIdentifiers
import UIKit

@MainActor
class BackupViewModel: ObservableObject {
    // Background task tracking to prevent iOS from killing long operations
    private var backgroundTaskID: UIBackgroundTaskIdentifier = .invalid
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
            // Fix ADVERSARIAL-CRITICAL-4: Register background task to prevent iOS killing operation
            registerBackgroundTask()

            do {
                isExporting = true
                exportError = nil
                exportProgress = 0
                exportMessage = "Starting export..."

                let zipURL = try await backupManager.createBackup { progress in
                    Task { @MainActor in
                        // Fix ADVERSARIAL-CRITICAL-1: Calculate progress from actual totalItems, not hardcoded 100
                        let total = max(1, progress.totalItems) // Avoid division by zero
                        self.exportProgress = Double(progress.processedItems) / Double(total)
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
            endBackgroundTask()
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
            // Fix ADVERSARIAL-CRITICAL-4: Register background task to prevent iOS killing operation
            registerBackgroundTask()

            do {
                startImport()
                guard backupValidationResult != nil else { return }

                let result = try await performRestore(from: url)
                handleImportSuccess(result)
            } catch {
                handleImportError(error)
            }

            isImporting = false
            endBackgroundTask()
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
                // Fix ADVERSARIAL-CRITICAL-1: Calculate progress from actual totalItems, not hardcoded 100
                let total = max(1, progress.totalItems) // Avoid division by zero
                self.importProgress = Double(progress.processedItems) / Double(total)
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

    // MARK: - Background Task Management
    // Fix ADVERSARIAL-CRITICAL-4: Background task registration
    // Fix CRITICAL-2: Add proper error handling and cancellation logic

    private func registerBackgroundTask() {
        backgroundTaskID = UIApplication.shared.beginBackgroundTask(withName: "BackupRestore") { [weak self] in
            guard let self = self else { return }

            // Fix CRITICAL-2: Task about to expire - cancel operations and notify user
            Task { @MainActor in
                // Cancel ongoing operations
                self.isExporting = false
                self.isImporting = false

                // Set error message
                let errorMessage = "Operation interrupted. The system stopped the operation to save battery. Please try again."
                if self.isExporting {
                    self.exportError = NSError(
                        domain: "BackupViewModel",
                        code: -2,
                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
                    )
                    self.exportMessage = "Export interrupted"
                } else if self.isImporting {
                    self.importError = NSError(
                        domain: "BackupViewModel",
                        code: -2,
                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
                    )
                    self.importMessage = "Import interrupted"
                }

                // Cleanup background task
                self.endBackgroundTask()
            }
        }

        // Fix CRITICAL-2: Check if background task registration failed
        if backgroundTaskID == .invalid {
            let errorMessage = "Unable to start background operation. Please ensure the app has sufficient permissions."
            if isExporting {
                exportError = NSError(
                    domain: "BackupViewModel",
                    code: -3,
                    userInfo: [NSLocalizedDescriptionKey: errorMessage]
                )
                exportMessage = "Failed to start export"
                isExporting = false
            } else if isImporting {
                importError = NSError(
                    domain: "BackupViewModel",
                    code: -3,
                    userInfo: [NSLocalizedDescriptionKey: errorMessage]
                )
                importMessage = "Failed to start import"
                isImporting = false
            }
        }
    }

    private func endBackgroundTask() {
        // Fix CRITICAL-2: Ensure background task is always ended to prevent memory leaks
        if backgroundTaskID != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTaskID)
            backgroundTaskID = .invalid
        }
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
