import Foundation
import SwiftUI
import Combine

/**
 * Theme mode options - matching Android implementation
 */
enum ThemeMode: String, CaseIterable {
    case light = "LIGHT"
    case dark = "DARK"
    case system = "SYSTEM"

    var displayName: String {
        switch self {
        case .light: return "Light"
        case .dark: return "Dark"
        case .system: return "System"
        }
    }

    var icon: String {
        switch self {
        case .light: return "sun.max.fill"
        case .dark: return "moon.fill"
        case .system: return "circle.lefthalf.filled"
        }
    }
}

/**
 * Singleton manager for app-wide theme state
 * Matches Android ThemeManager.kt functionality
 */
@MainActor
class ThemeManager: ObservableObject {

    // MARK: - Singleton
    static let shared = ThemeManager()

    // MARK: - Constants
    private let themePrefsKey = "theme_prefs"
    private let themeModeKey = "theme_mode"
    private let debounceInterval: TimeInterval = 0.3 // 300ms debounce for rapid switching

    // MARK: - Published Properties
    @Published private(set) var themeMode: ThemeMode = .system
    @Published private(set) var isDarkMode: Bool = false
    @Published private(set) var colorScheme: ColorScheme?

    // MARK: - Private Properties
    private let userDefaults = UserDefaults.standard
    private var cancellables = Set<AnyCancellable>()
    private var debounceTimer: Timer?
    private var themeSwitchCount = 0
    private var lastThemeSwitchTime = Date()

    // MARK: - Initialization
    private init() {
        loadThemeModePreference()
        setupSystemThemeObserver()
        updateColorScheme()
    }

    // MARK: - Theme Mode Management

    /**
     * Loads the saved theme mode preference from UserDefaults
     * Includes validation to prevent injection attacks
     */
    private func loadThemeModePreference() {
        let modeString = userDefaults.string(forKey: themeModeKey)

        // Validate the stored value to prevent injection
        if let modeString = modeString,
           let mode = ThemeMode(rawValue: modeString) {
            themeMode = mode
        } else {
            // Default to system if invalid or not set
            themeMode = .system
            saveThemeModePreference(.system)
        }

        isDarkMode = calculateDarkMode()
    }

    /**
     * Calculates whether dark mode should be active
     */
    private func calculateDarkMode() -> Bool {
        switch themeMode {
        case .light:
            return false
        case .dark:
            return true
        case .system:
            return isSystemInDarkMode()
        }
    }

    /**
     * Checks if the system is currently in dark mode
     */
    private func isSystemInDarkMode() -> Bool {
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
            return windowScene.traitCollection.userInterfaceStyle == .dark
        }
        return false
    }

    /**
     * Updates the color scheme for SwiftUI
     */
    private func updateColorScheme() {
        switch themeMode {
        case .light:
            colorScheme = .light
        case .dark:
            colorScheme = .dark
        case .system:
            colorScheme = nil // Let system decide
        }
    }

    // MARK: - Public Methods

    /**
     * Sets the theme mode with debouncing for rapid switching
     * - Parameter mode: The theme mode to set
     * - Parameter animated: Whether to animate the transition
     */
    func setThemeMode(_ mode: ThemeMode, animated: Bool = true) {
        // Debounce rapid theme switching
        debounceTimer?.invalidate()

        // Track rapid switching for rate limiting
        let now = Date()
        if now.timeIntervalSince(lastThemeSwitchTime) < 0.5 {
            themeSwitchCount += 1
            if themeSwitchCount > 10 {
                // Rate limit: prevent more than 10 switches in 5 seconds
                print("Theme switching rate limited")
                return
            }
        } else {
            themeSwitchCount = 0
        }
        lastThemeSwitchTime = now

        debounceTimer = Timer.scheduledTimer(withTimeInterval: debounceInterval, repeats: false) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.applyThemeMode(mode, animated: animated)
            }
        }
    }

    /**
     * Actually applies the theme mode after debouncing
     */
    private func applyThemeMode(_ mode: ThemeMode, animated: Bool) {
        guard mode != themeMode else { return }

        if animated {
            withAnimation(.easeInOut(duration: 0.3)) {
                themeMode = mode
                isDarkMode = calculateDarkMode()
                updateColorScheme()
            }
        } else {
            themeMode = mode
            isDarkMode = calculateDarkMode()
            updateColorScheme()
        }

        saveThemeModePreference(mode)

        // Provide haptic feedback for theme change (accessibility)
        if SettingsManager.shared.hapticFeedbackEnabled {
            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
            impactFeedback.impactOccurred()
        }

        // Post accessibility notification for VoiceOver users
        UIAccessibility.post(
            notification: .announcement,
            argument: "Theme changed to \(mode.displayName)"
        )
    }

    /**
     * Toggles through themes in order: System → Light → Dark → System
     * Matches Android implementation cycle order
     */
    func toggleTheme() {
        let newMode: ThemeMode
        switch themeMode {
        case .system:
            newMode = .light
        case .light:
            newMode = .dark
        case .dark:
            newMode = .system
        }
        setThemeMode(newMode)
    }

    /**
     * Gets the current effective color scheme
     */
    func currentColorScheme() -> ColorScheme {
        return isDarkMode ? .dark : .light
    }

    // MARK: - System Theme Observer

    /**
     * Sets up observer for system theme changes
     */
    private func setupSystemThemeObserver() {
        NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)
            .sink { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.onSystemThemeChanged()
                }
            }
            .store(in: &cancellables)

        // Also observe trait collection changes
        NotificationCenter.default.publisher(for: Notification.Name("UITraitCollectionDidChange"))
            .sink { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.onSystemThemeChanged()
                }
            }
            .store(in: &cancellables)
    }

    /**
     * Handles system theme changes (when in System mode)
     */
    private func onSystemThemeChanged() {
        if themeMode == .system {
            let newDarkMode = calculateDarkMode()
            if newDarkMode != isDarkMode {
                withAnimation(.easeInOut(duration: 0.3)) {
                    isDarkMode = newDarkMode
                }
            }
        }
    }

    // MARK: - Persistence

    /**
     * Saves the theme mode preference to UserDefaults
     */
    private func saveThemeModePreference(_ mode: ThemeMode) {
        userDefaults.set(mode.rawValue, forKey: themeModeKey)
        userDefaults.synchronize()

        // Also update SettingsManager for compatibility
        Task { @MainActor in
            if let settingsMode = SettingsManager.ThemeMode(rawValue: mode.rawValue.lowercased()) {
                SettingsManager.shared.themeMode = settingsMode
            }
        }
    }

    // MARK: - Migration Support

    /**
     * Migrates from old SettingsManager theme storage if needed
     */
    func migrateFromSettingsManager() {
        // Check if we have a value in SettingsManager but not in our storage
        if userDefaults.string(forKey: themeModeKey) == nil {
            let settingsTheme = SettingsManager.shared.themeMode
            let mappedMode: ThemeMode

            switch settingsTheme {
            case .light:
                mappedMode = .light
            case .dark:
                mappedMode = .dark
            case .system:
                mappedMode = .system
            }

            themeMode = mappedMode
            saveThemeModePreference(mappedMode)
        }
    }

    // MARK: - Validation

    /**
     * Validates a theme mode string to prevent injection
     */
    static func isValidThemeMode(_ modeString: String) -> Bool {
        return ThemeMode(rawValue: modeString) != nil
    }

    // MARK: - Debug

    #if DEBUG
    func debugPrintThemeState() {
        print("""
        Theme Manager State:
        - Mode: \(themeMode.rawValue)
        - Is Dark: \(isDarkMode)
        - System Dark: \(isSystemInDarkMode())
        - Switch Count: \(themeSwitchCount)
        """)
    }
    #endif
}

// MARK: - SwiftUI Extensions

extension View {
    /**
     * Applies the current theme from ThemeManager
     */
    func withThemeManager() -> some View {
        self
            .environmentObject(ThemeManager.shared)
            .preferredColorScheme(ThemeManager.shared.colorScheme)
    }

    /**
     * Convenience modifier to apply theme with animation
     */
    func themedColorScheme() -> some View {
        self.preferredColorScheme(ThemeManager.shared.colorScheme)
    }
}