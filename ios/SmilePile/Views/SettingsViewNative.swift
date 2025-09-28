import SwiftUI

struct SettingsViewNative: View {
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
        NavigationStack {
            List {
                // Appearance Section
                Section("Appearance") {
                    Picker("Theme", selection: $settingsManager.themeMode) {
                        Label("System", systemImage: "circle.lefthalf.filled")
                            .tag(SettingsManager.ThemeMode.system)
                        Label("Light", systemImage: "sun.max.fill")
                            .tag(SettingsManager.ThemeMode.light)
                        Label("Dark", systemImage: "moon.fill")
                            .tag(SettingsManager.ThemeMode.dark)
                    }
                }

                // Kids Mode Section
                Section("Kids Mode") {
                    Toggle(isOn: $kidsModeViewModel.isKidsMode) {
                        VStack(alignment: .leading) {
                            Text("Kids Mode")
                            Text("Simplified interface for children")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .onChange(of: kidsModeViewModel.isKidsMode) { newValue in
                        if newValue {
                            kidsModeViewModel.toggleKidsMode()
                        } else {
                            kidsModeViewModel.exitKidsMode(authenticated: true)
                        }
                    }
                }

                // Security Section
                Section("Security") {
                    if securityViewModel.hasPIN {
                        Button(action: { showPINChange = true }) {
                            Label("Change PIN", systemImage: "lock.fill")
                        }

                        if securityViewModel.isBiometricAvailable {
                            Toggle(isOn: $securityViewModel.isBiometricEnabled) {
                                Label("Use \(securityViewModel.biometricName)", systemImage: securityViewModel.biometricIcon)
                            }
                        }

                        Button(action: { showingParentalControls = true }) {
                            Label("Parental Controls", systemImage: "figure.and.child.holdinghands")
                        }

                        Button(action: { securityViewModel.removePIN() }) {
                            Label("Remove PIN", systemImage: "lock.open.fill")
                                .foregroundColor(.red)
                        }
                    } else {
                        Button(action: { showPINSetup = true }) {
                            Label("Set PIN", systemImage: "lock.fill")
                        }
                    }
                }

                // Backup & Restore Section
                Section("Backup & Restore") {
                    if backupPhotoCount > 0 || backupCategoryCount > 0 {
                        HStack {
                            Image(systemName: "externaldrive")
                                .foregroundColor(.blue)
                            VStack(alignment: .leading) {
                                Text("Library Contents")
                                    .font(.subheadline)
                                Text("\(backupPhotoCount) photos in \(backupCategoryCount) categories")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }

                    Button(action: { showingExportSheet = true }) {
                        Label("Export Data", systemImage: "archivebox")
                    }

                    Button(action: { showingImportPicker = true }) {
                        Label("Import Data", systemImage: "icloud.and.arrow.down")
                    }
                }

                // Performance Section
                Section("Performance") {
                    Toggle("Use Optimized Gallery", isOn: $useOptimizedGallery)
                    Toggle("Show Performance Overlay", isOn: $showPerformanceOverlay)

                    HStack {
                        Text("Memory Usage")
                        Spacer()
                        Text("\(MemoryMonitor.shared.currentMemoryUsageMB)MB")
                            .foregroundColor(.secondary)
                    }

                    Button(action: {
                        Task {
                            await OptimizedImageCache.shared.clearCache()
                        }
                    }) {
                        Label("Clear Image Cache", systemImage: "trash")
                            .foregroundColor(.red)
                    }
                }

                // About Section
                Section("About") {
                    Button(action: { showingAboutDialog = true }) {
                        HStack {
                            Text("About SmilePile")
                            Spacer()
                            Text("Version 25.09.27.006")
                                .foregroundColor(.secondary)
                            Image(systemName: "chevron.right")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .listStyle(InsetGroupedListStyle())
        }
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
        .alert("Import Progress", isPresented: .constant(isImporting)) {
            Button("Cancel") {
                isImporting = false
            }
        } message: {
            Text("Importing data...")
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