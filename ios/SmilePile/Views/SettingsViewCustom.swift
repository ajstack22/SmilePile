import SwiftUI

struct SettingsViewCustom: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @StateObject private var securityViewModel = SecuritySettingsViewModel()
    @State private var backupPhotoCount: Int = 0
    @State private var backupCategoryCount: Int = 0
    @EnvironmentObject private var settingsManager: SettingsManager
    @State private var showPINSetup = false
    @State private var showPINChange = false
    @State private var showingExportSheet = false
    @State private var showingImportPicker = false
    @State private var showingAboutDialog = false
    @State private var exportProgress: Double = 0.0
    @State private var importProgress: Double = 0.0
    @State private var isExporting = false
    @State private var isImporting = false

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

                    // Backup & Restore Section
                    SettingsSection(
                        title: "Backup & Restore"
                    ) {
                        VStack(spacing: 0) {
                            SettingsActionItem(
                                title: "Export Data",
                                subtitle: "Save your photos and categories",
                                icon: "square.and.arrow.up",
                                action: { showingExportSheet = true }
                            )

                            Divider()
                                .padding(.leading, 56)

                            SettingsActionItem(
                                title: "Import Data",
                                subtitle: "Restore from backup",
                                icon: "square.and.arrow.down",
                                action: { showingImportPicker = true }
                            )
                        }
                    }
                    .padding(.horizontal, 16)

                    // About Section
                    SettingsSection(
                        title: "About"
                    ) {
                        SettingsActionItem(
                            title: "SmilePile",
                            subtitle: "Version 25.09.27.006",
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
        .sheet(isPresented: $showingExportSheet) {
            NavigationView {
                VStack(spacing: 20) {
                    Text("Exporting backup...")
                        .font(.headline)

                    if isExporting {
                        ProgressView(value: exportProgress)
                            .progressViewStyle(LinearProgressViewStyle())
                            .padding(.horizontal)

                        Text("Preparing backup...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Button("Export") {
                        Task {
                            isExporting = true
                            // TODO: Implement export functionality
                            try? await Task.sleep(nanoseconds: 2_000_000_000)
                            isExporting = false
                            showingExportSheet = false
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isExporting)

                    Spacer()
                }
                .padding()
                .navigationTitle("Export Backup")
                .navigationBarItems(trailing: Button("Cancel") {
                    showingExportSheet = false
                })
            }
        }
        .sheet(isPresented: $showingImportPicker) {
            Text("Import functionality coming soon")
                .padding()
        }
        .sheet(isPresented: $showingAboutDialog) {
            AboutDialog(
                isPresented: $showingAboutDialog,
                appVersion: "25.09.27.006"
            )
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


