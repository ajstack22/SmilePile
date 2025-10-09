import SwiftUI
import LocalAuthentication

struct SettingsViewCustom: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @StateObject private var securityViewModel = SecuritySettingsViewModel()
    @StateObject private var backupViewModel = BackupViewModel()
    @State private var backupPhotoCount: Int = 0
    @State private var backupCategoryCount: Int = 0
    @EnvironmentObject private var settingsManager: SettingsManager
    @State private var showPINSetup = false
    @State private var showPINChange = false
    @State private var showingAboutDialog = false

    // Clear All Data state
    @State private var showClearConfirmation = false
    @State private var isClearing = false
    @State private var clearError: String?
    @State private var showErrorAlert = false
    @State private var showClearSuccess = false

    private var appVersionString: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "Unknown"
    }

    var body: some View {
        VStack(spacing: 0) {
            // App Header with glass effect
            AppHeaderComponent(
                onViewModeClick: {
                    kidsModeViewModel.toggleKidsMode()
                },
                showViewModeButton: true
            )

            // Settings content with cards matching Android
            ScrollView {
                VStack(spacing: 16) {
                    // Appearance Section
                    SettingsSection(
                        title: "Appearance"
                    ) {
                        ThemeSelector(themeMode: $settingsManager.themeMode)
                    }
                    .padding(.horizontal, 16)

                    // Security Section
                    SettingsSection(
                        title: "Security"
                    ) {
                        VStack(spacing: 0) {
                            if securityViewModel.hasPIN {
                                SettingsActionItem(
                                    title: "Change PIN",
                                    subtitle: "Update your security PIN",
                                    icon: "lock.fill",
                                    action: { showPINChange = true }
                                )

                                if securityViewModel.isBiometricAvailable {
                                    SettingsSwitchItem(
                                        title: "Use \(securityViewModel.biometricName)",
                                        subtitle: "Quick access with biometrics",
                                        icon: securityViewModel.biometricIcon,
                                        isOn: $securityViewModel.isBiometricEnabled
                                    )
                                }

                                SettingsActionItem(
                                    title: "Remove PIN",
                                    subtitle: "Disable PIN protection",
                                    icon: "lock.open.fill",
                                    action: { securityViewModel.removePIN() }
                                )
                            } else {
                                SettingsActionItem(
                                    title: "Set PIN",
                                    subtitle: "Protect Parent Mode with PIN",
                                    icon: "lock.fill",
                                    action: { showPINSetup = true }
                                )
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    // Data Section
                    SettingsSection(
                        title: "Data"
                    ) {
                        VStack(spacing: 0) {
                            SettingsActionItem(
                                title: "Export",
                                subtitle: "Save your photos and categories",
                                icon: "square.and.arrow.up",
                                action: {
                                    // Fix SECURITY-M4: Require biometric authentication
                                    authenticateUser {
                                        backupViewModel.exportData()
                                    }
                                }
                            )

                            Divider()
                                .padding(.leading, 56)

                            SettingsActionItem(
                                title: "Import",
                                subtitle: "Restore from backup",
                                icon: "square.and.arrow.down",
                                action: {
                                    // Fix SECURITY-M4: Require biometric authentication
                                    authenticateUser {
                                        backupViewModel.showFilePicker()
                                    }
                                }
                            )

                            Divider()
                                .padding(.leading, 56)

                            SettingsActionItem(
                                title: "Clear All Data",
                                subtitle: "Permanently delete all photos, categories, and settings",
                                icon: "trash.fill",
                                iconColor: .red,
                                action: {
                                    // Check for concurrent operations
                                    guard !backupViewModel.isExporting && !backupViewModel.isImporting else {
                                        clearError = "Cannot clear data while import or export is in progress"
                                        showErrorAlert = true
                                        return
                                    }

                                    // Biometric authentication, then show confirmation
                                    authenticateUser {
                                        showClearConfirmation = true
                                    }
                                }
                            )
                            .disabled(isClearing)
                        }
                    }
                    .padding(.horizontal, 16)

                    // About Section
                    SettingsSection(
                        title: "About"
                    ) {
                        SettingsActionItem(
                            title: "SmilePile",
                            subtitle: appVersionString,
                            icon: "info.circle",
                            action: { showingAboutDialog = true }
                        )
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
                .padding(.top, 16)
            }
        }
        .background(Color(UIColor.systemBackground))
        .sheet(isPresented: $showPINSetup) {
            PINEntryView(
                isPresented: $showPINSetup,
                mode: .setup,
                onSuccess: { pin in
                    try? PINManager.shared.setPIN(pin)
                    securityViewModel.refreshSecurityStatus()
                },
                onCancel: {}
            )
        }
        .sheet(isPresented: $showPINChange) {
            PINEntryView(
                isPresented: $showPINChange,
                mode: .change,
                onSuccess: { pin in
                    try? PINManager.shared.setPIN(pin)
                    securityViewModel.refreshSecurityStatus()
                },
                onCancel: {}
            )
        }
        .sheet(isPresented: $backupViewModel.isExporting) {
            ExportProgressDialog(viewModel: backupViewModel)
        }
        .sheet(isPresented: $backupViewModel.showDocumentPicker) {
            DocumentPickerView(
                selectedURL: .constant(nil),
                onSelect: { url in
                    backupViewModel.handleSelectedFile(url)
                }
            )
        }
        .sheet(isPresented: $backupViewModel.showShareSheet) {
            if let url = backupViewModel.exportedFileURL {
                ShareSheet(items: [url])
                    .onDisappear {
                        backupViewModel.dismissShareSheet()
                    }
            }
        }
        .alert("Restore Backup?", isPresented: $backupViewModel.showImportConfirmation) {
            Button("Cancel", role: .cancel) {
                backupViewModel.cancelImport()
            }
            Button("Restore") {
                backupViewModel.confirmImport()
            }
        } message: {
            if let result = backupViewModel.backupValidationResult {
                Text("\(result.photosCount) photos, \(result.categoriesCount) categories")
            }
        }
        .sheet(isPresented: $backupViewModel.isImporting) {
            ImportProgressDialog(viewModel: backupViewModel)
        }
        .alert("Import Complete", isPresented: $backupViewModel.importSuccess) {
            Button("OK") {
                backupViewModel.dismissImportSuccess()
            }
        } message: {
            if let result = backupViewModel.importResult {
                Text("\(result.photosImported) photos imported successfully")
            }
        }
        .alert("Export Error", isPresented: .constant(backupViewModel.exportError != nil)) {
            Button("OK") {
                backupViewModel.exportError = nil
            }
        } message: {
            if let error = backupViewModel.exportError {
                Text(error.localizedDescription)
            }
        }
        .alert("Import Error", isPresented: .constant(backupViewModel.importError != nil)) {
            Button("OK") {
                backupViewModel.importError = nil
            }
        } message: {
            if let error = backupViewModel.importError {
                Text(error.localizedDescription)
            }
        }
        .sheet(isPresented: $showingAboutDialog) {
            AboutDialog(
                isPresented: $showingAboutDialog,
                appVersion: appVersionString
            )
        }
        .alert("Clear All Data?", isPresented: $showClearConfirmation) {
            Button("Cancel", role: .cancel) {
                // Alert automatically dismisses
            }
            Button("Clear All Data", role: .destructive) {
                performClearAllData()
            }
            .disabled(isClearing)
        } message: {
            Text("This will permanently delete all photos, categories, settings, and PIN. The app will restart to onboarding. This action cannot be undone.")
        }
        .alert("Error", isPresented: $showErrorAlert) {
            Button("OK", role: .cancel) {
                showErrorAlert = false
            }
        } message: {
            Text(clearError ?? "Unknown error occurred")
        }
        .alert("Data Cleared Successfully", isPresented: $showClearSuccess) {
            Button("OK", role: .cancel) {
                showClearSuccess = false
            }
        } message: {
            Text("All photos, categories, settings, and PIN have been deleted. Please restart the app to complete the reset.")
        }
        .onAppear {
            Task {
                do {
                    let photoRepo = PhotoRepositoryImpl()
                    let categoryRepo = CategoryRepositoryImpl()
                    let photos = try await photoRepo.getAllPhotos()
                    let categories = try await categoryRepo.getAllCategories()
                    backupPhotoCount = photos.count
                    backupCategoryCount = categories.count
                } catch {
                    print("Error loading backup stats: \(error)")
                }
            }
            securityViewModel.refreshSecurityStatus()
        }
    }

    // Fix SECURITY-M4: Biometric authentication for sensitive operations
    private func authenticateUser(completion: @escaping () -> Void) {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            let reason = "Authenticate to access backup/restore"

            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, error in
                DispatchQueue.main.async {
                    if success {
                        completion()
                    } else {
                        // Authentication failed - user cancelled or error occurred
                        // No action needed, operation won't proceed
                    }
                }
            }
        } else {
            // No biometric authentication available - proceed anyway
            // (device doesn't support or user hasn't set up)
            completion()
        }
    }

    /// Performs complete app data clearing and restart
    /// Security: Requires biometric authentication + final confirmation
    /// Safety: Checks for concurrent operations before clearing
    private func performClearAllData() {
        Task { @MainActor in
            do {
                // Set loading state to prevent concurrent operations
                isClearing = true

                // Safety check: Ensure no concurrent import/export operations
                guard !backupViewModel.isExporting && !backupViewModel.isImporting else {
                    throw ClearDataError.concurrentOperation
                }

                // Clear all data using BackupManager
                // This handles:
                // - Photo file deletion from filesystem
                // - Category deletion from CoreData
                // - Photo deletion from CoreData
                // - UserDefaults domain removal
                // - PIN clearing via PINManager
                // - Settings reset via SettingsManager
                // - Onboarding flag reset
                // - Keychain cleanup
                try await BackupManager.shared.clearAllData()

                // Wait for persistence to complete
                // Ensures UserDefaults and CoreData changes are written to disk
                try await Task.sleep(nanoseconds: 300_000_000) // 300ms

                // Reset loading state and show success
                isClearing = false
                showClearSuccess = true

            } catch ClearDataError.concurrentOperation {
                // Reset loading state
                isClearing = false

                // Show specific error for concurrent operations
                clearError = "Cannot clear data while import or export is in progress. Please wait for the current operation to complete."
                showErrorAlert = true

            } catch {
                // Reset loading state
                isClearing = false

                // Show generic error
                clearError = "Failed to clear data: \(error.localizedDescription)"
                showErrorAlert = true
            }
        }
    }

}

// MARK: - Clear Data Errors

enum ClearDataError: LocalizedError {
    case concurrentOperation
    case migrationInProgress

    var errorDescription: String? {
        switch self {
        case .concurrentOperation:
            return "Cannot clear data while another operation is in progress"
        case .migrationInProgress:
            return "Cannot clear data during database migration"
        }
    }
}

// MARK: - Progress Dialogs

struct ExportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Exporting Data")
                .font(.headline)

            Text("Creating backup with photos. This may take a moment...")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Text(viewModel.exportMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.exportProgress > 0 {
                Text("Progress: \(Int(viewModel.exportProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}

struct ImportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Importing Data")
                .font(.headline)

            Text(viewModel.importMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.importProgress > 0 {
                Text("Progress: \(Int(viewModel.importProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}

// MARK: - Theme Selector
struct ThemeSelector: View {
    @Binding var themeMode: SettingsManager.ThemeMode

    var body: some View {
        VStack(spacing: 0) {
            // System theme option
            RadioButtonRow(
                isSelected: themeMode == .system,
                icon: "circle.lefthalf.filled",
                title: "System",
                subtitle: "Automatic",
                action: { themeMode = .system }
            )

            Divider()
                .padding(.leading, 56)

            // Light theme option
            RadioButtonRow(
                isSelected: themeMode == .light,
                icon: "sun.max",
                title: "Light",
                subtitle: nil,
                action: { themeMode = .light }
            )

            Divider()
                .padding(.leading, 56)

            // Dark theme option
            RadioButtonRow(
                isSelected: themeMode == .dark,
                icon: "moon",
                title: "Dark",
                subtitle: nil,
                action: { themeMode = .dark }
            )
        }
    }
}


