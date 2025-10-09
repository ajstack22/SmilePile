import SwiftUI

// Environment key for typography
struct TypographyKey: EnvironmentKey {
    static let defaultValue = Typography.nunito
}

extension EnvironmentValues {
    var typography: Typography {
        get { self[TypographyKey.self] }
        set { self[TypographyKey.self] = newValue }
    }
}

// Typography system
struct Typography {
    // Display styles
    let displayLarge: Font
    let displayMedium: Font
    let displaySmall: Font

    // Headline styles
    let headlineLarge: Font
    let headlineMedium: Font
    let headlineSmall: Font

    // Title styles
    let titleLarge: Font
    let titleMedium: Font
    let titleSmall: Font

    // Body styles
    let bodyLarge: Font
    let bodyMedium: Font
    let bodySmall: Font

    // Label styles
    let labelLarge: Font
    let labelMedium: Font
    let labelSmall: Font

    // Default Nunito typography (matches regular mode)
    static let nunito = Typography(
        displayLarge: .nunitoDisplayLarge,
        displayMedium: .nunitoDisplayMedium,
        displaySmall: .nunitoDisplaySmall,
        headlineLarge: .nunitoHeadlineLarge,
        headlineMedium: .nunitoHeadlineMedium,
        headlineSmall: .nunitoHeadlineSmall,
        titleLarge: .nunitoTitleLarge,
        titleMedium: .nunitoTitleMedium,
        titleSmall: .nunitoTitleSmall,
        bodyLarge: .nunitoBodyLarge,
        bodyMedium: .nunitoBodyMedium,
        bodySmall: .nunitoBodySmall,
        labelLarge: .nunitoLabelLarge,
        labelMedium: .nunitoLabelMedium,
        labelSmall: .nunitoLabelSmall
    )

    // Kids Mode typography (larger and bolder)
    static let nunitoKids = Typography(
        displayLarge: .nunitoDisplayLargeKids,
        displayMedium: .nunitoDisplayMediumKids,
        displaySmall: .nunitoDisplaySmallKids,
        headlineLarge: .nunitoHeadlineLargeKids,
        headlineMedium: .nunitoHeadlineMediumKids,
        headlineSmall: .nunitoHeadlineSmallKids,
        titleLarge: .nunitoTitleLargeKids,
        titleMedium: .nunitoTitleMediumKids,
        titleSmall: .nunitoTitleSmallKids,
        bodyLarge: .nunitoBodyLargeKids,
        bodyMedium: .nunitoBodyMediumKids,
        bodySmall: .nunitoBodySmallKids,
        labelLarge: .nunitoLabelLargeKids,
        labelMedium: .nunitoLabelMediumKids,
        labelSmall: .nunitoLabelSmallKids
    )
}

// View extension for easy access
extension View {
    func typography(_ typography: Typography) -> some View {
        environment(\.typography, typography)
    }
}
