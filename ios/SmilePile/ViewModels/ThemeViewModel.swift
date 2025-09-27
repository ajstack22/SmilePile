import Foundation
import SwiftUI
import Combine

/**
 * Observable view model wrapper for ThemeManager
 * Provides SwiftUI-friendly interface for theme management
 */
@MainActor
class ThemeViewModel: ObservableObject {

    // MARK: - Published Properties
    @Published private(set) var currentTheme: ThemeMode
    @Published private(set) var isDarkMode: Bool
    @Published private(set) var isAnimating: Bool = false

    // MARK: - Private Properties
    private let themeManager = ThemeManager.shared
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Computed Properties

    /**
     * Returns the appropriate color scheme for SwiftUI
     */
    var colorScheme: ColorScheme? {
        themeManager.colorScheme
    }

    /**
     * Returns the display name for the current theme
     */
    var currentThemeDisplayName: String {
        currentTheme.displayName
    }

    /**
     * Returns the icon name for the current theme
     */
    var currentThemeIcon: String {
        currentTheme.icon
    }

    /**
     * Returns all available theme options
     */
    var availableThemes: [ThemeMode] {
        ThemeMode.allCases
    }

    /**
     * Returns the next theme in the cycle
     */
    var nextTheme: ThemeMode {
        switch currentTheme {
        case .system: return .light
        case .light: return .dark
        case .dark: return .system
        }
    }

    /**
     * Returns a description of what the next theme will be
     */
    var nextThemeDescription: String {
        "Switch to \(nextTheme.displayName) mode"
    }

    // MARK: - Initialization

    init() {
        self.currentTheme = themeManager.themeMode
        self.isDarkMode = themeManager.isDarkMode

        setupBindings()
    }

    // MARK: - Setup

    /**
     * Sets up reactive bindings to ThemeManager
     */
    private func setupBindings() {
        // Observe theme changes from ThemeManager
        themeManager.$themeMode
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newTheme in
                self?.currentTheme = newTheme
            }
            .store(in: &cancellables)

        themeManager.$isDarkMode
            .receive(on: DispatchQueue.main)
            .sink { [weak self] isDark in
                self?.isDarkMode = isDark
            }
            .store(in: &cancellables)
    }

    // MARK: - Public Methods

    /**
     * Sets the theme mode with animation
     */
    func setTheme(_ mode: ThemeMode) {
        guard !isAnimating else { return }

        isAnimating = true

        withAnimation(.easeInOut(duration: 0.3)) {
            themeManager.setThemeMode(mode, animated: true)
        }

        // Reset animation flag after animation completes
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.isAnimating = false
        }
    }

    /**
     * Toggles to the next theme in the cycle
     */
    func toggleTheme() {
        guard !isAnimating else { return }

        isAnimating = true

        withAnimation(.easeInOut(duration: 0.3)) {
            themeManager.toggleTheme()
        }

        // Reset animation flag after animation completes
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.isAnimating = false
        }
    }

    /**
     * Cycles through themes with a specific animation
     */
    func cycleTheme(animation: Animation = .spring()) {
        guard !isAnimating else { return }

        withAnimation(animation) {
            themeManager.toggleTheme()
        }
    }

    /**
     * Checks if a specific theme is currently active
     */
    func isThemeActive(_ mode: ThemeMode) -> Bool {
        currentTheme == mode
    }

    // MARK: - Accessibility

    /**
     * Returns an accessibility label for the current theme state
     */
    func accessibilityLabel() -> String {
        "Current theme: \(currentTheme.displayName). \(isDarkMode ? "Dark mode active" : "Light mode active")"
    }

    /**
     * Returns an accessibility hint for the theme toggle
     */
    func accessibilityHint() -> String {
        "Double tap to switch to \(nextTheme.displayName) mode"
    }

    // MARK: - Preview Support

    #if DEBUG
    static func makePreview(theme: ThemeMode = .system) -> ThemeViewModel {
        let viewModel = ThemeViewModel()
        viewModel.setTheme(theme)
        return viewModel
    }
    #endif
}

// MARK: - SwiftUI View Extensions

extension View {
    /**
     * Applies theme view model to the view hierarchy
     */
    func withThemeViewModel() -> some View {
        self.environmentObject(ThemeViewModel())
    }
}

// MARK: - Theme Toggle Button Component

struct ThemeToggleButton: View {
    @StateObject private var viewModel = ThemeViewModel()
    var showLabel: Bool = true
    var size: CGFloat = 24

    var body: some View {
        Button(action: {
            viewModel.toggleTheme()
        }) {
            HStack(spacing: 12) {
                Image(systemName: viewModel.currentThemeIcon)
                    .font(.system(size: size))
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(iconColor)
                    .rotationEffect(.degrees(viewModel.isAnimating ? 180 : 0))

                if showLabel {
                    Text(viewModel.currentThemeDisplayName)
                        .font(.body)
                }
            }
            .padding(.horizontal, showLabel ? 16 : 8)
            .padding(.vertical, 8)
            .background(backgroundGradient)
            .cornerRadius(showLabel ? 20 : 10)
            .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(ThemeToggleButtonStyle())
        .accessibilityLabel(viewModel.accessibilityLabel())
        .accessibilityHint(viewModel.accessibilityHint())
        .disabled(viewModel.isAnimating)
    }

    private var iconColor: Color {
        switch viewModel.currentTheme {
        case .light:
            return .orange
        case .dark:
            return .indigo
        case .system:
            return .blue
        }
    }

    private var backgroundGradient: some View {
        LinearGradient(
            colors: [
                iconColor.opacity(0.15),
                iconColor.opacity(0.05)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - Custom Button Style

struct ThemeToggleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .opacity(configuration.isPressed ? 0.8 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

// MARK: - Theme Picker Component

struct ThemePicker: View {
    @StateObject private var viewModel = ThemeViewModel()
    var title: String = "Theme"

    var body: some View {
        Picker(title, selection: Binding(
            get: { viewModel.currentTheme },
            set: { viewModel.setTheme($0) }
        )) {
            ForEach(viewModel.availableThemes, id: \.self) { theme in
                Label {
                    Text(theme.displayName)
                } icon: {
                    Image(systemName: theme.icon)
                        .symbolRenderingMode(.hierarchical)
                }
                .tag(theme)
            }
        }
        .pickerStyle(SegmentedPickerStyle())
        .disabled(viewModel.isAnimating)
    }
}

// MARK: - Theme Section for Settings

struct ThemeSettingsSection: View {
    @StateObject private var viewModel = ThemeViewModel()

    var body: some View {
        Section("Appearance") {
            VStack(alignment: .leading, spacing: 12) {
                Text("Theme")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                ThemePicker()

                HStack {
                    Image(systemName: viewModel.isDarkMode ? "moon.fill" : "sun.max.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(viewModel.isDarkMode ? "Dark mode is active" : "Light mode is active")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 4)
        }
    }
}

// MARK: - Preview Provider

#if DEBUG
struct ThemeViewModelPreviews: PreviewProvider {
    static var previews: some View {
        Group {
            // Theme Toggle Button Preview
            VStack(spacing: 20) {
                ThemeToggleButton(showLabel: true)
                ThemeToggleButton(showLabel: false)
                ThemeToggleButton(showLabel: true, size: 32)
            }
            .padding()
            .previewDisplayName("Theme Toggle Button")

            // Theme Picker Preview
            VStack(spacing: 20) {
                ThemePicker()
                    .padding()
            }
            .previewDisplayName("Theme Picker")

            // Settings Section Preview
            NavigationView {
                Form {
                    ThemeSettingsSection()
                }
                .navigationTitle("Settings")
            }
            .previewDisplayName("Theme Settings Section")
        }
    }
}
#endif