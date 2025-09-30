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
        // Use the Variable font with appropriate weight modifier
        return Font.custom("Nunito-Variable", size: size)
            .weight(weight)
    }

    // Convenience methods matching Android and design specs
    static let nunitoTitle = Font.custom("Nunito-Variable", size: 36).weight(.heavy)
    static let nunitoHeadline = Font.custom("Nunito-Variable", size: 22).weight(.bold)
    static let nunitoBody = Font.custom("Nunito-Variable", size: 16).weight(.medium)
    static let nunitoCaption = Font.custom("Nunito-Variable", size: 14).weight(.regular)
    static let nunitoButton = Font.custom("Nunito-Variable", size: 18).weight(.bold)
}