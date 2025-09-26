import UIKit
import SwiftUI

class FontManager {
    static let shared = FontManager()

    private init() {
        registerFonts()
    }

    func registerFonts() {
        let fontNames = ["Nunito-Black", "Nunito-Bold", "Nunito-ExtraBold", "Nunito-Variable"]

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
    static func nunito(_ weight: Font.Weight = .regular, size: CGFloat) -> Font {
        let fontName: String
        switch weight {
        case .black, .heavy:
            fontName = "Nunito-Black"
        case .bold, .semibold:
            fontName = "Nunito-Bold"
        case .medium, .regular:
            fontName = "Nunito-ExtraBold"
        default:
            fontName = "Nunito-ExtraBold"
        }

        return Font.custom(fontName, size: size)
    }
}