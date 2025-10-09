import UIKit
import SwiftUI

class FontManager {
    static let shared = FontManager()

    private init() {
        registerFonts()
    }

    func registerFonts() {
        // Only register Nunito-Variable as the other files are corrupted
        let fontNames = ["Nunito-Variable"]

        for fontName in fontNames {
            guard let fontURL = Bundle.main.url(forResource: fontName, withExtension: "ttf") else {
                print("❌ Font file not found: \(fontName).ttf")
                continue
            }

            guard let fontData = try? Data(contentsOf: fontURL) else {
                print("❌ Could not load font data: \(fontName)")
                continue
            }

            guard let provider = CGDataProvider(data: fontData as CFData) else {
                print("❌ Could not create data provider for: \(fontName)")
                continue
            }

            guard let font = CGFont(provider) else {
                print("❌ Could not create font from data: \(fontName)")
                continue
            }

            var error: Unmanaged<CFError>?
            if !CTFontManagerRegisterGraphicsFont(font, &error) {
                if let error = error?.takeRetainedValue() {
                    let errorDescription = CFErrorCopyDescription(error)
                    print("❌ Failed to register font \(fontName): \(errorDescription ?? "" as CFString)")

                    // If font is already registered, that's OK
                    if (error as Error).localizedDescription.contains("already registered") {
                        print("✅ Font \(fontName) was already registered")
                    }
                } else {
                    print("❌ Failed to register font \(fontName): unknown error")
                }
            } else {
                print("✅ Successfully registered font: \(fontName)")
            }
        }

        // List all available fonts for verification
        print("\n📱 Available Nunito fonts:")
        for family in UIFont.familyNames {
            if family.lowercased().contains("nunito") {
                print("  Family: \(family)")
                for font in UIFont.fontNames(forFamilyName: family) {
                    print("    - \(font)")
                }
            }
        }
    }
}

// SwiftUI Font extension for easy usage
extension Font {
    // Main nunito function with weight and size parameters
    static func nunito(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        // Use the Nunito font with appropriate weight modifier
        return Font.custom("Nunito", size: size)
            .weight(weight)
    }

    // Legacy convenience methods for backward compatibility
    static let nunitoTitle = Font.custom("Nunito", size: 36).weight(.heavy)
    static let nunitoHeadline = Font.custom("Nunito", size: 22).weight(.bold)
    static let nunitoBody = Font.custom("Nunito", size: 16).weight(.medium)
    static let nunitoCaption = Font.custom("Nunito", size: 14).weight(.regular)
    static let nunitoButton = Font.custom("Nunito", size: 18).weight(.bold)

    // MARK: - Material Design Typography System (matching Android)

    // Display styles - largest text on screen
    static let nunitoDisplayLarge = Font.custom("Nunito", size: 50).weight(.bold)
    static let nunitoDisplayMedium = Font.custom("Nunito", size: 40).weight(.bold)
    static let nunitoDisplaySmall = Font.custom("Nunito", size: 32).weight(.semibold)

    // Headline styles - high-emphasis text
    static let nunitoHeadlineLarge = Font.custom("Nunito", size: 28).weight(.semibold)
    static let nunitoHeadlineMedium = Font.custom("Nunito", size: 24).weight(.semibold)
    static let nunitoHeadlineSmall = Font.custom("Nunito", size: 21).weight(.medium)

    // Title styles - medium-emphasis text
    static let nunitoTitleLarge = Font.custom("Nunito", size: 19).weight(.medium)
    static let nunitoTitleMedium = Font.custom("Nunito", size: 16).weight(.medium)
    static let nunitoTitleSmall = Font.custom("Nunito", size: 12).weight(.medium)

    // Body styles - default text
    static let nunitoBodyLarge = Font.custom("Nunito", size: 14).weight(.regular)
    static let nunitoBodyMedium = Font.custom("Nunito", size: 12).weight(.regular)
    static let nunitoBodySmall = Font.custom("Nunito", size: 11).weight(.regular)

    // Label styles - small utility text
    static let nunitoLabelLarge = Font.custom("Nunito", size: 12).weight(.medium)
    static let nunitoLabelMedium = Font.custom("Nunito", size: 11).weight(.medium)
    static let nunitoLabelSmall = Font.custom("Nunito", size: 10).weight(.medium)

    // MARK: - Kids Mode Typography (larger and bolder, matching Android)

    // Kids Display styles
    static let nunitoDisplayLargeKids = Font.custom("Nunito", size: 56).weight(.heavy)
    static let nunitoDisplayMediumKids = Font.custom("Nunito", size: 46).weight(.heavy)
    static let nunitoDisplaySmallKids = Font.custom("Nunito", size: 37).weight(.bold)

    // Kids Headline styles
    static let nunitoHeadlineLargeKids = Font.custom("Nunito", size: 32).weight(.bold)
    static let nunitoHeadlineMediumKids = Font.custom("Nunito", size: 28).weight(.bold)
    static let nunitoHeadlineSmallKids = Font.custom("Nunito", size: 24).weight(.semibold)

    // Kids Title styles
    static let nunitoTitleLargeKids = Font.custom("Nunito", size: 23).weight(.semibold)
    static let nunitoTitleMediumKids = Font.custom("Nunito", size: 19).weight(.semibold)
    static let nunitoTitleSmallKids = Font.custom("Nunito", size: 16).weight(.semibold)

    // Kids Body styles
    static let nunitoBodyLargeKids = Font.custom("Nunito", size: 18).weight(.medium)
    static let nunitoBodyMediumKids = Font.custom("Nunito", size: 16).weight(.medium)
    static let nunitoBodySmallKids = Font.custom("Nunito", size: 14).weight(.medium)

    // Kids Label styles
    static let nunitoLabelLargeKids = Font.custom("Nunito", size: 16).weight(.semibold)
    static let nunitoLabelMediumKids = Font.custom("Nunito", size: 14).weight(.semibold)
    static let nunitoLabelSmallKids = Font.custom("Nunito", size: 12).weight(.semibold)
}