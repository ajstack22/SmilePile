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
    @State private var showingParentalControls = false
    @State private var showingAboutDialog = false
    @State private var exportProgress: Double = 0.0
    @State private var importProgress: Double = 0.0
    @State private var isExporting = false
    @State private var isImporting = false
    @AppStorage("useOptimizedGallery") private var useOptimizedGallery = true
    @AppStorage("showPerformanceOverlay") private var showPerformanceOverlay = false

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
                    // Appearance Section - Orange accent
                    SettingsSection(
                        title: "Appearance",
                        titleColor: Color(hex: "#FF9800") ?? .orange
                    ) {
                        ThemeSelector(themeMode: $settingsManager.themeMode)
                    }
                    .padding(.horizontal, 16)

                    // Kids Mode Section
                    SettingsSection(
                        title: "Kids Mode",
                        titleColor: Color(hex: "#4CAF50") ?? .green
                    ) {
                        SettingsSwitchItem(
                            title: "Kids Mode",
                            subtitle: "Simplified interface for children",
                            icon: "face.smiling.fill",
                            iconColor: Color(hex: "#4CAF50") ?? .green,
                            isOn: .init(
                                get: { kidsModeViewModel.isKidsMode },
                                set: { newValue in
                                    if newValue {
                                        kidsModeViewModel.toggleKidsMode()
                                    } else {
                                        kidsModeViewModel.exitKidsMode(authenticated: true)
                                    }
                                }
                            )
                        )
                    }
                    .padding(.horizontal, 16)

                    // Security Section - Green accent
                    SettingsSection(
                        title: "Security",
                        titleColor: Color(hex: "#4CAF50") ?? .green
                    ) {
                        VStack(spacing: 8) {
                            if securityViewModel.hasPIN {
                                SettingsActionItem(
                                    title: "Change PIN",
                                    subtitle: "PIN protection is enabled for Parent Mode",
                                    icon: "lock.fill",
                                    iconColor: Color(hex: "#4CAF50") ?? .green,
                                    action: { showPINChange = true }
                                )

                                if securityViewModel.isBiometricAvailable {
                                    SettingsSwitchItem(
                                        title: "Use \(securityViewModel.biometricName)",
                                        subtitle: "Use biometric authentication for parental controls",
                                        icon: securityViewModel.biometricIcon,
                                        iconColor: Color(hex: "#4CAF50") ?? .green,
                                        isOn: $securityViewModel.isBiometricEnabled
                                    )
                                }

                                SettingsActionItem(
                                    title: "Parental Controls",
                                    subtitle: "Access child safety settings and preferences",
                                    icon: "figure.and.child.holdinghands",
                                    iconColor: Color(hex: "#4CAF50") ?? .green,
                                    action: { showingParentalControls = true }
                                )

                                SettingsActionItem(
                                    title: "Remove PIN",
                                    subtitle: "Remove PIN protection from Parent Mode",
                                    icon: "lock.open.fill",
                                    iconColor: Color(hex: "#4CAF50") ?? .green,
                                    action: { securityViewModel.removePIN() }
                                )
                            } else {
                                SettingsActionItem(
                                    title: "Set PIN",
                                    subtitle: "Set a PIN to protect Parent Mode access",
                                    icon: "lock.fill",
                                    iconColor: Color(hex: "#4CAF50") ?? .green,
                                    action: { showPINSetup = true }
                                )
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    // Backup & Restore Section - Blue accent
                    SettingsSection(
                        title: "Backup & Restore",
                        titleColor: Color(hex: "#2196F3") ?? .blue
                    ) {
                        VStack(spacing: 8) {
                            if backupPhotoCount > 0 || backupCategoryCount > 0 {
                                BackupStatsCard(
                                    photoCount: backupPhotoCount,
                                    categoryCount: backupCategoryCount,
                                    accentColor: Color(hex: "#2196F3") ?? .blue
                                )
                            }

                            SettingsActionItem(
                                title: "Export Data",
                                subtitle: "Create a complete backup file (includes photos)",
                                icon: "archivebox",
                                iconColor: Color(hex: "#2196F3") ?? .blue,
                                action: { showingExportSheet = true }
                            )

                            SettingsActionItem(
                                title: "Import Data",
                                subtitle: "Restore from a backup file",
                                icon: "icloud.and.arrow.down",
                                iconColor: Color(hex: "#2196F3") ?? .blue,
                                action: { showingImportPicker = true }
                            )
                        }
                    }
                    .padding(.horizontal, 16)

                    // Performance Section
                    SettingsSection(
                        title: "Performance",
                        titleColor: Color(hex: "#9C27B0") ?? .purple
                    ) {
                        VStack(spacing: 8) {
                            SettingsSwitchItem(
                                title: "Use Optimized Gallery",
                                subtitle: "Enable performance optimizations",
                                icon: "speedometer",
                                iconColor: Color(hex: "#9C27B0") ?? .purple,
                                isOn: $useOptimizedGallery
                            )

                            SettingsSwitchItem(
                                title: "Show Performance Overlay",
                                subtitle: "Display performance metrics",
                                icon: "chart.line.uptrend.xyaxis",
                                iconColor: Color(hex: "#9C27B0") ?? .purple,
                                isOn: $showPerformanceOverlay
                            )

                            HStack {
                                Image(systemName: "memorychip")
                                    .foregroundColor(Color(hex: "#9C27B0") ?? .purple)
                                    .frame(width: 24)
                                Text("Memory Usage")
                                    .font(.subheadline)
                                Spacer()
                                Text("\(MemoryMonitor.shared.currentMemoryUsageMB)MB")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color(UIColor.secondarySystemBackground))
                            )

                            SettingsActionItem(
                                title: "Clear Image Cache",
                                subtitle: "Free up memory by clearing cached images",
                                icon: "trash",
                                iconColor: .red,
                                action: {
                                    Task {
                                        await OptimizedImageCache.shared.clearCache()
                                    }
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 16)

                    // About Section - Pink accent
                    SettingsSection(
                        title: "About",
                        titleColor: Color(hex: "#FF6B6B") ?? .pink
                    ) {
                        SettingsActionItem(
                            title: "About SmilePile",
                            subtitle: "Version 25.09.27.006",
                            icon: "info.circle",
                            iconColor: Color(hex: "#FF6B6B") ?? .pink,
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
        .sheet(isPresented: $showingParentalControls) {
            NavigationView {
                ParentalControlsView()
                    .navigationTitle("Parental Controls")
                    .navigationBarItems(trailing: Button("Done") {
                        showingParentalControls = false
                    })
            }
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
            RadioButtonRow(
                isSelected: themeMode == .system,
                icon: "circle.lefthalf.filled",
                title: "Follow System",
                subtitle: "Automatically match device theme",
                action: { themeMode = .system }
            )

            Divider()
                .padding(.horizontal, 16)

            RadioButtonRow(
                isSelected: themeMode == .light,
                icon: "sun.max.fill",
                title: "Light",
                subtitle: "Always use light theme",
                action: { themeMode = .light }
            )

            Divider()
                .padding(.horizontal, 16)

            RadioButtonRow(
                isSelected: themeMode == .dark,
                icon: "moon.fill",
                title: "Dark",
                subtitle: "Always use dark theme",
                action: { themeMode = .dark }
            )
        }
    }
}


