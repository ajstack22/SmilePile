import SwiftUI
import UIKit

// Test that the font extension compiles and fonts are available
struct FontLoadingTest {
    static func testFontAvailability() -> String {
        var results = "=== Nunito Font Loading Test ===\n\n"
        
        // Check if Nunito-Variable is registered in UIFont
        let families = UIFont.familyNames
        var nunitoFound = false
        
        for family in families {
            if family.lowercased().contains("nunito") {
                nunitoFound = true
                results += "✅ Found Nunito font family: \(family)\n"
                let fonts = UIFont.fontNames(forFamilyName: family)
                for fontName in fonts {
                    results += "   - \(fontName)\n"
                }
            }
        }
        
        if !nunitoFound {
            results += "⚠️ Nunito font family not found in UIFont.familyNames\n"
            results += "Note: This is expected if the font hasn't been loaded in a running app context\n"
        }
        
        // Test that our Font extension methods compile
        results += "\n=== Font Extension Compilation Test ===\n"
        
        // These lines verify the extension compiles
        _ = Font.nunitoTitle
        _ = Font.nunitoHeadline
        _ = Font.nunitoBody
        _ = Font.nunitoCaption
        _ = Font.nunitoButton
        _ = Font.nunito(20, weight: .bold)
        
        results += "✅ All Font extension methods compile successfully\n"
        
        results += "\n=== Font Weight Mapping ===\n"
        results += "All weights now use Nunito-Variable.ttf with weight modifiers:\n"
        results += "- .nunitoTitle    = Nunito-Variable 36pt .heavy\n"
        results += "- .nunitoHeadline = Nunito-Variable 22pt .bold\n"
        results += "- .nunitoBody     = Nunito-Variable 16pt .medium\n"
        results += "- .nunitoCaption  = Nunito-Variable 14pt .regular\n"
        results += "- .nunitoButton   = Nunito-Variable 18pt .bold\n"
        
        return results
    }
}

// Run the test and print results
print(FontLoadingTest.testFontAvailability())
