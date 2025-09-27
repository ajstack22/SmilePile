import Foundation
import SwiftUI
import Combine

/**
 * Centralized settings manager using UserDefaults and @AppStorage for persistent user preferences
 * Provides type-safe access to all app settings with automatic synchronization
 */
class SettingsManager: ObservableObject {
    static let shared = SettingsManager()

    private let userDefaults = UserDefaults.standard
    private let suiteName = "group.com.smilepile" // For app extensions

    // MARK: - Keys

    private struct Keys {
        // Kids Mode Settings
        static let kidsModeEnabled = "kids_mode_enabled"
        static let kidsModePIN = "kids_mode_pin"
        static let kidsModePINEnabled = "kids_mode_pin_enabled"

        // Gallery View Preferences
        static let gridSize = "gallery_grid_size"
        static let sortOrder = "gallery_sort_order"
        static let showHiddenPhotos = "show_hidden_photos"
        static let showPhotoDates = "show_photo_dates"

        // Theme Preferences
        static let themeMode = "theme_mode"
        static let dynamicColorsEnabled = "dynamic_colors_enabled"
        static let accentColor = "accent_color"

        // Photo Quality Settings
        static let uploadQuality = "upload_quality"
        static let thumbnailQuality = "thumbnail_quality"
        static let autoOptimizeStorage = "auto_optimize_storage"

        // Auto-backup Preferences
        static let autoBackupEnabled = "auto_backup_enabled"
        static let backupWifiOnly = "backup_wifi_only"
        static let backupFrequency = "backup_frequency"
        static let lastBackupTime = "last_backup_time"
        static let iCloudBackupEnabled = "icloud_backup_enabled"

        // Import/Export Preferences
        static let defaultCategory = "default_import_category"
        static let autoCategorizeImports = "auto_categorize_imports"
        static let preserveMetadata = "preserve_photo_metadata"

        // Notification Settings
        static let notificationsEnabled = "notifications_enabled"
        static let backupNotifications = "backup_notifications"
        static let memoryNotifications = "memory_notifications"

        // Security Settings
        static let biometricEnabled = "biometric_enabled"
        static let appLockEnabled = "app_lock_enabled"
        static let lockTimeoutMinutes = "lock_timeout_minutes"
        static let faceIDEnabled = "face_id_enabled"
        static let touchIDEnabled = "touch_id_enabled"

        // Performance Settings
        static let cacheSizeMB = "cache_size_mb"
        static let preloadAdjacentImages = "preload_adjacent_images"
        static let animationSpeed = "animation_speed"
        static let lowPowerModeOptimization = "low_power_mode_optimization"

        // App State
        static let firstLaunch = "first_launch"
        static let appVersion = "app_version"
        static let migrationVersion = "migration_version"
        static let lastReviewPrompt = "last_review_prompt"

        // iOS Specific
        static let hapticFeedbackEnabled = "haptic_feedback_enabled"
        static let swipeGesturesEnabled = "swipe_gestures_enabled"
        static let photoLibraryPermissionGranted = "photo_library_permission_granted"
    }

    // MARK: - Enums

    enum SortOrder: String, CaseIterable {
        case dateNewest = "date_newest"
        case dateOldest = "date_oldest"
        case nameAsc = "name_asc"
        case nameDesc = "name_desc"
        case sizeLargest = "size_largest"
        case sizeSmallest = "size_smallest"

        var displayName: String {
            switch self {
            case .dateNewest: return "Newest First"
            case .dateOldest: return "Oldest First"
            case .nameAsc: return "Name (A-Z)"
            case .nameDesc: return "Name (Z-A)"
            case .sizeLargest: return "Largest First"
            case .sizeSmallest: return "Smallest First"
            }
        }
    }

    enum PhotoQuality: String, CaseIterable {
        case original = "original"
        case high = "high"
        case medium = "medium"
        case low = "low"

        var displayName: String {
            switch self {
            case .original: return "Original"
            case .high: return "High"
            case .medium: return "Medium"
            case .low: return "Low"
            }
        }
    }

    enum BackupFrequency: String, CaseIterable {
        case manual = "manual"
        case hourly = "hourly"
        case daily = "daily"
        case weekly = "weekly"
        case monthly = "monthly"

        var displayName: String {
            switch self {
            case .manual: return "Manual Only"
            case .hourly: return "Every Hour"
            case .daily: return "Daily"
            case .weekly: return "Weekly"
            case .monthly: return "Monthly"
            }
        }
    }

    enum ThemeMode: String, CaseIterable {
        case system = "system"
        case light = "light"
        case dark = "dark"

        var displayName: String {
            switch self {
            case .system: return "System"
            case .light: return "Light"
            case .dark: return "Dark"
            }
        }
    }

    // MARK: - Published Properties for SwiftUI

    @AppStorage(Keys.kidsModeEnabled) var kidsModeEnabled: Bool = true
    @AppStorage(Keys.kidsModePINEnabled) var kidsModePINEnabled: Bool = false
    @AppStorage(Keys.gridSize) var gridSize: Int = 3
    @AppStorage(Keys.showHiddenPhotos) var showHiddenPhotos: Bool = false
    @AppStorage(Keys.showPhotoDates) var showPhotoDates: Bool = true
    @AppStorage(Keys.dynamicColorsEnabled) var dynamicColorsEnabled: Bool = true
    @AppStorage(Keys.autoOptimizeStorage) var autoOptimizeStorage: Bool = false
    @AppStorage(Keys.autoBackupEnabled) var autoBackupEnabled: Bool = false
    @AppStorage(Keys.backupWifiOnly) var backupWifiOnly: Bool = true
    @AppStorage(Keys.iCloudBackupEnabled) var iCloudBackupEnabled: Bool = false
    @AppStorage(Keys.autoCategorizeImports) var autoCategorizeImports: Bool = false
    @AppStorage(Keys.preserveMetadata) var preserveMetadata: Bool = true
    @AppStorage(Keys.notificationsEnabled) var notificationsEnabled: Bool = true
    @AppStorage(Keys.backupNotifications) var backupNotifications: Bool = true
    @AppStorage(Keys.memoryNotifications) var memoryNotifications: Bool = false
    @AppStorage(Keys.biometricEnabled) var biometricEnabled: Bool = false
    @AppStorage(Keys.appLockEnabled) var appLockEnabled: Bool = false
    @AppStorage(Keys.lockTimeoutMinutes) var lockTimeoutMinutes: Int = 5
    @AppStorage(Keys.faceIDEnabled) var faceIDEnabled: Bool = false
    @AppStorage(Keys.touchIDEnabled) var touchIDEnabled: Bool = false
    @AppStorage(Keys.cacheSizeMB) var cacheSizeMB: Int = 100
    @AppStorage(Keys.preloadAdjacentImages) var preloadAdjacentImages: Bool = true
    @AppStorage(Keys.animationSpeed) var animationSpeed: Double = 1.0
    @AppStorage(Keys.lowPowerModeOptimization) var lowPowerModeOptimization: Bool = true
    @AppStorage(Keys.firstLaunch) var firstLaunch: Bool = true
    @AppStorage(Keys.appVersion) var appVersion: Int = 0
    @AppStorage(Keys.migrationVersion) var migrationVersion: Int = 0
    @AppStorage(Keys.hapticFeedbackEnabled) var hapticFeedbackEnabled: Bool = true
    @AppStorage(Keys.swipeGesturesEnabled) var swipeGesturesEnabled: Bool = true
    @AppStorage(Keys.photoLibraryPermissionGranted) var photoLibraryPermissionGranted: Bool = false

    // MARK: - Computed Properties for Enums

    var sortOrder: SortOrder {
        get {
            let rawValue = userDefaults.string(forKey: Keys.sortOrder) ?? SortOrder.dateNewest.rawValue
            return SortOrder(rawValue: rawValue) ?? .dateNewest
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: Keys.sortOrder)
            objectWillChange.send()
        }
    }

    var themeMode: ThemeMode {
        get {
            let rawValue = userDefaults.string(forKey: Keys.themeMode) ?? ThemeMode.system.rawValue
            return ThemeMode(rawValue: rawValue) ?? .system
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: Keys.themeMode)
            objectWillChange.send()
        }
    }

    var uploadQuality: PhotoQuality {
        get {
            let rawValue = userDefaults.string(forKey: Keys.uploadQuality) ?? PhotoQuality.high.rawValue
            return PhotoQuality(rawValue: rawValue) ?? .high
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: Keys.uploadQuality)
            objectWillChange.send()
        }
    }

    var thumbnailQuality: PhotoQuality {
        get {
            let rawValue = userDefaults.string(forKey: Keys.thumbnailQuality) ?? PhotoQuality.medium.rawValue
            return PhotoQuality(rawValue: rawValue) ?? .medium
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: Keys.thumbnailQuality)
            objectWillChange.send()
        }
    }

    var backupFrequency: BackupFrequency {
        get {
            let rawValue = userDefaults.string(forKey: Keys.backupFrequency) ?? BackupFrequency.daily.rawValue
            return BackupFrequency(rawValue: rawValue) ?? .daily
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: Keys.backupFrequency)
            objectWillChange.send()
        }
    }

    var defaultCategory: String? {
        get {
            userDefaults.string(forKey: Keys.defaultCategory)
        }
        set {
            if let value = newValue {
                userDefaults.set(value, forKey: Keys.defaultCategory)
            } else {
                userDefaults.removeObject(forKey: Keys.defaultCategory)
            }
            objectWillChange.send()
        }
    }

    var lastBackupTime: Date? {
        get {
            let timestamp = userDefaults.double(forKey: Keys.lastBackupTime)
            return timestamp > 0 ? Date(timeIntervalSince1970: timestamp) : nil
        }
        set {
            if let date = newValue {
                userDefaults.set(date.timeIntervalSince1970, forKey: Keys.lastBackupTime)
            } else {
                userDefaults.removeObject(forKey: Keys.lastBackupTime)
            }
            objectWillChange.send()
        }
    }

    var lastReviewPrompt: Date? {
        get {
            let timestamp = userDefaults.double(forKey: Keys.lastReviewPrompt)
            return timestamp > 0 ? Date(timeIntervalSince1970: timestamp) : nil
        }
        set {
            if let date = newValue {
                userDefaults.set(date.timeIntervalSince1970, forKey: Keys.lastReviewPrompt)
            } else {
                userDefaults.removeObject(forKey: Keys.lastReviewPrompt)
            }
            objectWillChange.send()
        }
    }

    var accentColor: Color {
        get {
            let colorData = userDefaults.data(forKey: Keys.accentColor)
            if let data = colorData,
               let color = try? NSKeyedUnarchiver.unarchivedObject(ofClass: UIColor.self, from: data) {
                return Color(color)
            }
            return .blue
        }
        set {
            let uiColor = UIColor(newValue)
            if let data = try? NSKeyedArchiver.archivedData(withRootObject: uiColor, requiringSecureCoding: true) {
                userDefaults.set(data, forKey: Keys.accentColor)
                objectWillChange.send()
            }
        }
    }

    // MARK: - Private Init

    private init() {
        // Register default values
        registerDefaults()
    }

    // MARK: - Methods

    private func registerDefaults() {
        let defaults: [String: Any] = [
            Keys.kidsModeEnabled: true,
            Keys.kidsModePINEnabled: false,
            Keys.gridSize: 3,
            Keys.sortOrder: SortOrder.dateNewest.rawValue,
            Keys.showHiddenPhotos: false,
            Keys.showPhotoDates: true,
            Keys.themeMode: ThemeMode.system.rawValue,
            Keys.dynamicColorsEnabled: true,
            Keys.uploadQuality: PhotoQuality.high.rawValue,
            Keys.thumbnailQuality: PhotoQuality.medium.rawValue,
            Keys.autoOptimizeStorage: false,
            Keys.autoBackupEnabled: false,
            Keys.backupWifiOnly: true,
            Keys.backupFrequency: BackupFrequency.daily.rawValue,
            Keys.iCloudBackupEnabled: false,
            Keys.autoCategorizeImports: false,
            Keys.preserveMetadata: true,
            Keys.notificationsEnabled: true,
            Keys.backupNotifications: true,
            Keys.memoryNotifications: false,
            Keys.biometricEnabled: false,
            Keys.appLockEnabled: false,
            Keys.lockTimeoutMinutes: 5,
            Keys.faceIDEnabled: false,
            Keys.touchIDEnabled: false,
            Keys.cacheSizeMB: 100,
            Keys.preloadAdjacentImages: true,
            Keys.animationSpeed: 1.0,
            Keys.lowPowerModeOptimization: true,
            Keys.firstLaunch: true,
            Keys.appVersion: 0,
            Keys.migrationVersion: 0,
            Keys.hapticFeedbackEnabled: true,
            Keys.swipeGesturesEnabled: true,
            Keys.photoLibraryPermissionGranted: false
        ]
        userDefaults.register(defaults: defaults)
    }

    // MARK: - Kids Mode PIN Management (Secure)

    func setKidsModePIN(_ pin: String) throws {
        // Use PINManager for secure storage
        try PINManager.shared.setPIN(pin)
        kidsModePINEnabled = true
    }

    func validateKidsModePIN(_ pin: String) -> Bool {
        return PINManager.shared.validatePIN(pin)
    }

    func clearKidsModePIN() throws {
        try PINManager.shared.clearPIN()
        kidsModePINEnabled = false
    }

    // MARK: - Settings Export/Import

    func exportSettings() -> [String: Any] {
        var settings: [String: Any] = [:]

        // Export all non-secure settings
        let keys = [
            Keys.kidsModeEnabled,
            Keys.gridSize,
            Keys.sortOrder,
            Keys.showHiddenPhotos,
            Keys.showPhotoDates,
            Keys.themeMode,
            Keys.dynamicColorsEnabled,
            Keys.uploadQuality,
            Keys.thumbnailQuality,
            Keys.autoOptimizeStorage,
            Keys.autoBackupEnabled,
            Keys.backupWifiOnly,
            Keys.backupFrequency,
            Keys.iCloudBackupEnabled,
            Keys.defaultCategory,
            Keys.autoCategorizeImports,
            Keys.preserveMetadata,
            Keys.notificationsEnabled,
            Keys.backupNotifications,
            Keys.memoryNotifications,
            Keys.biometricEnabled,
            Keys.appLockEnabled,
            Keys.lockTimeoutMinutes,
            Keys.faceIDEnabled,
            Keys.touchIDEnabled,
            Keys.cacheSizeMB,
            Keys.preloadAdjacentImages,
            Keys.animationSpeed,
            Keys.lowPowerModeOptimization,
            Keys.hapticFeedbackEnabled,
            Keys.swipeGesturesEnabled
        ]

        for key in keys {
            if let value = userDefaults.object(forKey: key) {
                settings[key] = value
            }
        }

        return settings
    }

    func importSettings(_ settings: [String: Any], overwrite: Bool = false) {
        if overwrite {
            // Clear existing settings (except secure ones)
            resetToDefaults()
        }

        // Import settings
        for (key, value) in settings {
            // Skip secure keys
            if key == Keys.kidsModePIN {
                continue
            }
            userDefaults.set(value, forKey: key)
        }

        // Notify observers
        objectWillChange.send()
    }

    func exportSettingsToJSON() throws -> Data {
        let settings = exportSettings()
        return try JSONSerialization.data(withJSONObject: settings, options: .prettyPrinted)
    }

    func importSettingsFromJSON(_ data: Data, overwrite: Bool = false) throws {
        guard let settings = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw SettingsError.invalidFormat
        }
        importSettings(settings, overwrite: overwrite)
    }

    // MARK: - Reset

    func resetToDefaults() {
        // Get the registered defaults
        registerDefaults()

        // Remove all stored values to revert to defaults
        let domain = Bundle.main.bundleIdentifier!
        userDefaults.removePersistentDomain(forName: domain)
        userDefaults.synchronize()

        // Re-register defaults
        registerDefaults()

        // Clear secure storage
        try? PINManager.shared.clearPIN()

        // Notify observers
        objectWillChange.send()
    }

    func resetCategory(_ category: String) {
        switch category {
        case "gallery":
            gridSize = 3
            sortOrder = .dateNewest
            showHiddenPhotos = false
            showPhotoDates = true
        case "theme":
            themeMode = .system
            dynamicColorsEnabled = true
            accentColor = .blue
        case "backup":
            autoBackupEnabled = false
            backupWifiOnly = true
            backupFrequency = .daily
            iCloudBackupEnabled = false
        case "security":
            biometricEnabled = false
            appLockEnabled = false
            lockTimeoutMinutes = 5
            faceIDEnabled = false
            touchIDEnabled = false
            try? clearKidsModePIN()
        case "performance":
            cacheSizeMB = 100
            preloadAdjacentImages = true
            animationSpeed = 1.0
            lowPowerModeOptimization = true
        case "notifications":
            notificationsEnabled = true
            backupNotifications = true
            memoryNotifications = false
        default:
            break
        }
        objectWillChange.send()
    }

    // MARK: - Migration

    func migrateIfNeeded() {
        let currentVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        let currentBuildNumber = Int(currentVersion ?? "0") ?? 0

        if appVersion < currentBuildNumber {
            performMigration(from: appVersion, to: currentBuildNumber)
            appVersion = currentBuildNumber
        }

        if firstLaunch {
            firstLaunch = false
            setupFirstLaunch()
        }
    }

    private func performMigration(from oldVersion: Int, to newVersion: Int) {
        // Perform version-specific migrations
        if oldVersion < 100 {
            // Migrate from v1.0.0
            // Example: Convert old settings format to new
        }

        if oldVersion < 200 {
            // Migrate from v2.0.0
            // Example: Add new default settings
        }

        migrationVersion = newVersion
    }

    private func setupFirstLaunch() {
        // Set up default values for first launch
        kidsModeEnabled = true
        notificationsEnabled = true
        preserveMetadata = true
        hapticFeedbackEnabled = true
        swipeGesturesEnabled = true
    }

    // MARK: - Sync with iCloud

    func syncWithiCloud() {
        guard let suiteName = suiteName else { return }

        // Create a shared UserDefaults for app group
        guard let sharedDefaults = UserDefaults(suiteName: suiteName) else { return }

        // Sync non-secure settings
        let settings = exportSettings()
        for (key, value) in settings {
            sharedDefaults.set(value, forKey: key)
        }

        sharedDefaults.synchronize()
    }

    func loadFromiCloud() {
        guard let suiteName = suiteName else { return }

        // Load from shared UserDefaults
        guard let sharedDefaults = UserDefaults(suiteName: suiteName) else { return }

        var settings: [String: Any] = [:]
        for (key, value) in sharedDefaults.dictionaryRepresentation() {
            settings[key] = value
        }

        importSettings(settings, overwrite: false)
    }

    // MARK: - Validation

    func validateGridSize(_ size: Int) -> Int {
        return min(max(size, 2), 5)
    }

    func validateCacheSize(_ size: Int) -> Int {
        return min(max(size, 50), 500)
    }

    func validateAnimationSpeed(_ speed: Double) -> Double {
        return min(max(speed, 0.5), 2.0)
    }

    func validateLockTimeout(_ minutes: Int) -> Int {
        return min(max(minutes, 0), 60)
    }
}

// MARK: - Errors

enum SettingsError: LocalizedError {
    case invalidFormat
    case migrationFailed
    case syncFailed

    var errorDescription: String? {
        switch self {
        case .invalidFormat:
            return "Invalid settings format"
        case .migrationFailed:
            return "Failed to migrate settings"
        case .syncFailed:
            return "Failed to sync settings with iCloud"
        }
    }
}

// MARK: - SwiftUI Property Wrapper Extension

extension View {
    func withSettings() -> some View {
        self.environmentObject(SettingsManager.shared)
    }
}