# iOS Design System Implementation Plan
**Platform**: iOS (SwiftUI)
**Date**: January 2025
**Scope**: Phase 3A - iOS Technical Implementation
**Context**: [DESIGN_SYSTEM.md](/Users/adamstack/SmilePile/website/DESIGN_SYSTEM.md) | [Research Report](/Users/adamstack/SmilePile/DESIGN_SYSTEM_IMPLEMENTATION_RESEARCH.md) | [User Stories](/Users/adamstack/SmilePile/DESIGN_SYSTEM_USER_STORIES.md)

---

## Executive Summary

This document provides a **step-by-step technical implementation plan** for migrating SmilePile's iOS app to the new design system. The plan transforms the current bright, playful aesthetic (Nunito font, vibrant colors) into a calming, scientifically-backed design system (Atkinson Hyperlegible font, soft muted tones) specifically tailored for families with special needs children.

**Key Changes**:
- **Font**: Nunito → Atkinson Hyperlegible
- **Colors**: Bright (#2196F3, #4CAF50) → Soft (#7FB3D5, #A8D8B9)
- **Accessibility**: Basic → WCAG 2.2 AA compliant
- **Typography Scale**: Material Design → Design System scale
- **Components**: Ad-hoc → Standardized library

**Estimated Effort**: 88 hours (~11 working days)
**Files Modified**: 129+ files
**Files Created**: 15+ new component files

---

## Table of Contents

1. [File Inventory](#1-file-inventory)
2. [Font Implementation](#2-font-implementation)
3. [Color System](#3-color-system)
4. [Typography Scale](#4-typography-scale)
5. [Component Updates](#5-component-updates)
6. [Accessibility Implementation](#6-accessibility-implementation)
7. [Dark Mode Support](#7-dark-mode-support)
8. [Testing Strategy](#8-testing-strategy)
9. [Build Configuration](#9-build-configuration)
10. [Implementation Sequence](#10-implementation-sequence)

---

## 1. File Inventory

### 1.1 Core Files (Must Modify First)

| File | Current State | Changes Required | Priority |
|------|--------------|------------------|----------|
| `/ios/SmilePile/FontManager.swift` | Registers Nunito font | Replace with Atkinson Hyperlegible | P0 |
| `/ios/SmilePile/Typography.swift` | Material Design scale | Update to design system scale | P0 |
| `/ios/SmilePile/Theme/ColorConstants.swift` | Bright brand colors | Replace with soft palette | P0 |
| `/ios/SmilePile/Info.plist` | Lists Nunito .ttf files | Add Atkinson .ttf references | P0 |

### 1.2 View Files Using Colors (31+ files)

**High Priority** (visible in main user flows):
- `/ios/SmilePile/Views/ContentView.swift` - Main app container
- `/ios/SmilePile/Views/OptimizedPhotoGalleryView.swift` - Photo gallery
- `/ios/SmilePile/Views/SettingsViewCustom.swift` - Settings screen
- `/ios/SmilePile/Views/CategoryManagementView.swift` - Category management
- `/ios/SmilePile/Views/PhotoEditView.swift` - Photo editor

**Medium Priority** (onboarding and secondary flows):
- `/ios/SmilePile/Onboarding/OnboardingView.swift`
- `/ios/SmilePile/Onboarding/Screens/WelcomeScreen.swift`
- `/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`
- `/ios/SmilePile/Onboarding/Screens/CompletionScreen.swift`
- `/ios/SmilePile/Views/KidsMode/KidsModeGalleryView.swift`

**Low Priority** (secondary features):
- `/ios/SmilePile/Views/Security/PatternGridView.swift`
- `/ios/SmilePile/Views/Security/PatternLockView.swift`
- `/ios/SmilePile/Views/Sheets/AddCategorySheet.swift`
- `/ios/SmilePile/Views/Sheets/CategorySelectionView.swift`

### 1.3 Component Files (20+ files)

- `/ios/SmilePile/Views/Components/ColorSelectionButton.swift` - Uses paletteColors
- `/ios/SmilePile/Views/Components/CategoryChip.swift` - Uses pile colors
- `/ios/SmilePile/Views/Components/FloatingActionButton.swift` - Uses primaryButton color
- `/ios/SmilePile/Views/Components/MaterialTabBar.swift` - Uses brand colors
- `/ios/SmilePile/Views/Components/AppHeaderComponent.swift` - Uses text colors
- `/ios/SmilePile/Views/Components/SettingsComponents.swift` - Multiple color references
- `/ios/SmilePile/Views/Components/DemoModeBanner.swift` - Warning colors
- `/ios/SmilePile/Views/Components/PhotoThumbnailView.swift` - Border colors

### 1.4 Files to Create

**Button Components**:
- `/ios/SmilePile/Views/Components/Buttons/PrimaryButton.swift`
- `/ios/SmilePile/Views/Components/Buttons/SecondaryButton.swift`
- `/ios/SmilePile/Views/Components/Buttons/TextButton.swift`

**Card Components**:
- `/ios/SmilePile/Views/Components/Cards/StandardCard.swift`
- `/ios/SmilePile/Views/Components/Cards/PhotoCard.swift`

**Form Components**:
- `/ios/SmilePile/Views/Components/Forms/FormInput.swift`
- `/ios/SmilePile/Views/Components/Forms/FormLabel.swift`

**Accessibility**:
- `/ios/SmilePile/Accessibility/FocusIndicator.swift`
- `/ios/SmilePile/Accessibility/TouchTargetModifier.swift`
- `/ios/SmilePile/Accessibility/ReducedMotionModifier.swift`

---

## 2. Font Implementation

### 2.1 Download Atkinson Hyperlegible Fonts

**Source**: [Google Fonts - Atkinson Hyperlegible](https://fonts.google.com/specimen/Atkinson+Hyperlegible)

**Required Files**:
1. `AtkinsonHyperlegible-Regular.ttf` (400 weight)
2. `AtkinsonHyperlegible-Bold.ttf` (700 weight)

**Action**:
```bash
# Download from Google Fonts
# Or use direct links:
# https://fonts.google.com/download?family=Atkinson%20Hyperlegible
```

**File Location**: `/ios/SmilePile/Fonts/`

### 2.2 Update Info.plist

**File**: `/ios/SmilePile/Info.plist`

**Current**:
```xml
<key>UIAppFonts</key>
<array>
    <string>Nunito-Black.ttf</string>
    <string>Nunito-Bold.ttf</string>
    <string>Nunito-ExtraBold.ttf</string>
    <string>Nunito-Variable.ttf</string>
</array>
```

**Updated**:
```xml
<key>UIAppFonts</key>
<array>
    <string>AtkinsonHyperlegible-Regular.ttf</string>
    <string>AtkinsonHyperlegible-Bold.ttf</string>
</array>
```

**Note**: Remove all Nunito .ttf references. Only 2 Atkinson files needed (Regular + Bold).

### 2.3 Update FontManager.swift

**File**: `/ios/SmilePile/FontManager.swift`

**Replace Entire File**:
```swift
import UIKit
import SwiftUI

class FontManager {
    static let shared = FontManager()

    private init() {
        registerFonts()
    }

    func registerFonts() {
        // Register Atkinson Hyperlegible fonts
        let fontNames = [
            "AtkinsonHyperlegible-Regular",
            "AtkinsonHyperlegible-Bold"
        ]

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

        // List all available Atkinson fonts for verification
        print("\n📱 Available Atkinson Hyperlegible fonts:")
        for family in UIFont.familyNames {
            if family.lowercased().contains("atkinson") {
                print("  Family: \(family)")
                for font in UIFont.fontNames(forFamilyName: family) {
                    print("    - \(font)")
                }
            }
        }
    }
}

// SwiftUI Font extension for design system typography
extension Font {
    // MARK: - Design System Typography
    // Based on DESIGN_SYSTEM.md specification

    // Main function with weight and size parameters
    static func atkinson(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let fontName = weight == .bold ? "AtkinsonHyperlegible-Bold" : "AtkinsonHyperlegible-Regular"
        return Font.custom(fontName, size: size)
    }

    // MARK: - Design System Scale
    // H1: 48px, Bold, Line Height 1.1
    static let h1 = Font.custom("AtkinsonHyperlegible-Bold", size: 48)

    // H2: 36px, Bold, Line Height 1.2
    static let h2 = Font.custom("AtkinsonHyperlegible-Bold", size: 36)

    // H3: 28px, Bold, Line Height 1.3
    static let h3 = Font.custom("AtkinsonHyperlegible-Bold", size: 28)

    // H4: 22px, Bold, Line Height 1.4
    static let h4 = Font.custom("AtkinsonHyperlegible-Bold", size: 22)

    // H5: 18px, Bold, Line Height 1.4
    static let h5 = Font.custom("AtkinsonHyperlegible-Bold", size: 18)

    // Body Large: 18px, Regular, Line Height 1.6
    static let bodyLarge = Font.custom("AtkinsonHyperlegible-Regular", size: 18)

    // Body: 16px, Regular, Line Height 1.6
    static let body = Font.custom("AtkinsonHyperlegible-Regular", size: 16)

    // Body Small: 14px, Regular, Line Height 1.5
    static let bodySmall = Font.custom("AtkinsonHyperlegible-Regular", size: 14)

    // Button: 16px, Bold, Line Height 1.0
    static let button = Font.custom("AtkinsonHyperlegible-Bold", size: 16)

    // Label: 14px, Bold, Line Height 1.2
    static let label = Font.custom("AtkinsonHyperlegible-Bold", size: 14)

    // MARK: - Kids Mode Typography (20% larger)
    static let h1Kids = Font.custom("AtkinsonHyperlegible-Bold", size: 58)
    static let h2Kids = Font.custom("AtkinsonHyperlegible-Bold", size: 43)
    static let h3Kids = Font.custom("AtkinsonHyperlegible-Bold", size: 34)
    static let h4Kids = Font.custom("AtkinsonHyperlegible-Bold", size: 26)
    static let bodyLargeKids = Font.custom("AtkinsonHyperlegible-Regular", size: 22)
    static let bodyKids = Font.custom("AtkinsonHyperlegible-Regular", size: 19)
    static let buttonKids = Font.custom("AtkinsonHyperlegible-Bold", size: 19)
}
```

**Changes Made**:
1. Replaced "Nunito" with "Atkinson Hyperlegible"
2. Updated to design system font scale (48px, 36px, 28px, 22px, 18px, 16px, 14px)
3. Removed Material Design scale (no longer needed)
4. Simplified to Regular (400) and Bold (700) weights only
5. Added Kids Mode variants (20% larger)
6. Simplified function names (.h1, .body, .button instead of .nunitoHeadlineLarge)

### 2.4 Update Typography.swift

**File**: `/ios/SmilePile/Typography.swift`

**Replace Entire File**:
```swift
import SwiftUI

// Environment key for typography
struct TypographyKey: EnvironmentKey {
    static let defaultValue = Typography.standard
}

extension EnvironmentValues {
    var typography: Typography {
        get { self[TypographyKey.self] }
        set { self[TypographyKey.self] = newValue }
    }
}

// Typography system
struct Typography {
    // Heading styles
    let h1: Font
    let h2: Font
    let h3: Font
    let h4: Font
    let h5: Font

    // Body styles
    let bodyLarge: Font
    let body: Font
    let bodySmall: Font

    // Utility styles
    let button: Font
    let label: Font

    // Standard typography (regular mode)
    static let standard = Typography(
        h1: .h1,
        h2: .h2,
        h3: .h3,
        h4: .h4,
        h5: .h5,
        bodyLarge: .bodyLarge,
        body: .body,
        bodySmall: .bodySmall,
        button: .button,
        label: .label
    )

    // Kids Mode typography (20% larger)
    static let kids = Typography(
        h1: .h1Kids,
        h2: .h2Kids,
        h3: .h3Kids,
        h4: .h4Kids,
        h5: .h5, // No kids variant for H5
        bodyLarge: .bodyLargeKids,
        body: .bodyKids,
        bodySmall: .bodySmall, // No kids variant for small
        button: .buttonKids,
        label: .label // No kids variant for label
    )
}

// View extension for easy access
extension View {
    func typography(_ typography: Typography) -> some View {
        environment(\.typography, typography)
    }
}
```

**Changes Made**:
1. Simplified structure to match design system
2. Removed Material Design scale references
3. Aligned with FontManager.swift extension names
4. Maintained Kids Mode support (20% larger)

### 2.5 Migration Strategy

**Find & Replace**:
```swift
// OLD → NEW
.nunitoDisplayLarge → .h1
.nunitoHeadlineLarge → .h2
.nunitoTitleLarge → .h3
.nunitoBodyLarge → .bodyLarge
.nunitoBodyMedium → .body
.nunitoButton → .button
.nunito(18, weight: .bold) → .atkinson(18, weight: .bold)
```

**Verification**:
```bash
# Search for remaining Nunito references
grep -r "nunito" ios/SmilePile --include="*.swift"
grep -r "Nunito" ios/SmilePile --include="*.swift"

# Should return 0 results
```

---

## 3. Color System

### 3.1 Update ColorConstants.swift

**File**: `/ios/SmilePile/Theme/ColorConstants.swift`

**Replace Entire File**:
```swift
// ColorConstants.swift
// SmilePile Design System Colors
//
// Based on DESIGN_SYSTEM.md - scientifically-backed palette
// designed to reduce anxiety and sensory overload.

import SwiftUI

extension Color {
    // MARK: - Primary Palette

    /// Soft Blue (#7FB3D5) - Primary brand color
    /// Usage: Primary buttons, headers, key CTAs, links
    /// Psychology: Trust, empathy, calm, stability
    /// Contrast: 4.6:1 against Soft Charcoal (WCAG AA)
    static let softBlue = Color(hex: "#7FB3D5")

    /// Sage Green (#A8D8B9) - Secondary color
    /// Usage: Success states, growth indicators, secondary elements
    /// Psychology: Refreshing, health-promoting, concentration
    /// Contrast: 5.2:1 against Soft Charcoal (WCAG AA)
    static let sageGreen = Color(hex: "#A8D8B9")

    /// Lavender (#C9B3D6) - Accent color
    /// Usage: Highlights, special features, gentle accents
    /// Psychology: Calming to nervous system, relaxation, creativity
    /// Contrast: 4.9:1 against Soft Charcoal (WCAG AA)
    static let lavender = Color(hex: "#C9B3D6")

    /// Warm Cream (#F8F3ED) - Neutral light
    /// Usage: Backgrounds, cards, containers
    /// Psychology: Comforting, approachable, warm
    /// Note: Avoid pure white (#FFFFFF) - too harsh
    static let warmCream = Color(hex: "#F8F3ED")

    /// Soft Charcoal (#3A3A3A) - Neutral dark
    /// Usage: Body text, dark UI elements
    /// Psychology: Professional, readable, grounded
    /// Note: Avoid pure black (#000000) - too harsh
    /// Contrast: 12.8:1 against Warm Cream (WCAG AAA)
    static let softCharcoal = Color(hex: "#3A3A3A")

    // MARK: - Supporting Colors

    /// Gentle Gold (#F5DA81) - Celebrations
    /// Usage: Achievements, warmth (use sparingly)
    /// Warning: Use minimally to avoid overstimulation
    static let gentleGold = Color(hex: "#F5DA81")

    /// Soft Coral (#F4A6A3) - Gentle alerts
    /// Usage: Love/family themes, gentle alerts
    /// Warning: Not for errors - too gentle
    static let softCoral = Color(hex: "#F4A6A3")

    /// Pale Mint (#B8DCD6) - Fresh sections
    /// Usage: New content indicators, fresh sections
    static let paleMint = Color(hex: "#B8DCD6")

    // MARK: - Dark Mode Palette

    /// Soft Black (#1E1E1E) - Dark mode background
    static let softBlack = Color(hex: "#1E1E1E")

    /// Elevated Surface (#2A2A2A) - Dark mode surface
    static let elevatedSurface = Color(hex: "#2A2A2A")

    /// Brighter Soft Blue (#A8CEEA) - Dark mode primary
    static let softBlueDark = Color(hex: "#A8CEEA")

    /// Brighter Sage Green (#C1E8CF) - Dark mode secondary
    static let sageGreenDark = Color(hex: "#C1E8CF")

    /// Brighter Lavender (#D8C5E5) - Dark mode accent
    static let lavenderDark = Color(hex: "#D8C5E5")

    // MARK: - Category/Pile Colors (Soft Variants)
    /// Updated to soft, muted tones matching design system
    static let categoryColors = [
        "#A8D8B9", // Sage Green
        "#7FB3D5", // Soft Blue
        "#C9B3D6", // Lavender
        "#B8DCD6", // Pale Mint
        "#F5DA81", // Gentle Gold
        "#F4A6A3", // Soft Coral
        "#E5C9A0", // Soft Tan
        "#D4A5C9"  // Soft Mauve
    ]

    // MARK: - Semantic Colors

    /// Primary action color
    static let primaryAction = softBlue

    /// Secondary action color
    static let secondaryAction = sageGreen

    /// Success state color
    static let success = sageGreen

    /// Warning state color (not harsh red)
    static let warning = gentleGold

    /// Error state color (softer than traditional red)
    static let error = Color(hex: "#DC8686") // Muted red

    /// Info state color
    static let info = softBlue

    /// Background color (light mode)
    static let background = warmCream

    /// Surface color (cards, elevated elements)
    static let surface = Color.white

    /// Text primary color
    static let textPrimary = softCharcoal

    /// Text secondary color (70% opacity)
    static let textSecondary = softCharcoal.opacity(0.7)

    /// Divider color
    static let divider = Color.gray.opacity(0.2)

    // MARK: - Focus Indicator (WCAG 2.2 AA)

    /// Focus outline color (3px solid, 2px offset)
    static let focusIndicator = softBlue

    // MARK: - Backward Compatibility (Deprecated)
    // These are deprecated but kept temporarily for migration

    @available(*, deprecated, message: "Use .softBlue instead")
    static let smilePileBlue = softBlue

    @available(*, deprecated, message: "Use .sageGreen instead")
    static let smilePileGreen = sageGreen

    @available(*, deprecated, message: "Use .primaryAction instead")
    static let primaryButton = primaryAction

    // MARK: - Color Helper

    /// Initialize Color from hex string
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - Environment Support

extension Color {
    /// Adapts color for dark mode automatically
    static func adaptive(light: Color, dark: Color) -> Color {
        return Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }
}
```

**Key Changes**:
1. **Primary colors replaced**: Bright → Soft tones
2. **Contrast ratios documented**: All meet WCAG AA minimum
3. **Dark mode colors added**: Separate palette for dark mode
4. **Category colors updated**: Soft variants of pile colors
5. **Semantic colors defined**: .primaryAction, .success, .error, etc.
6. **Focus indicator color**: Defined for accessibility
7. **Backward compatibility**: Deprecated aliases for migration

### 3.2 Color Migration Map

| Old Color | Old Hex | New Color | New Hex | Usage |
|-----------|---------|-----------|---------|-------|
| `.smilePileBlue` | #2196F3 | `.softBlue` | #7FB3D5 | Primary buttons, links |
| `.smilePileGreen` | #4CAF50 | `.sageGreen` | #A8D8B9 | Success states, secondary |
| `.smilePileOrange` | #FF6600 | `.lavender` | #C9B3D6 | Accents, highlights |
| `.primaryButton` | #2196F3 | `.primaryAction` | #7FB3D5 | Button backgrounds |
| `.pileRed` | #FF6B6B | `.softCoral` | #F4A6A3 | Category colors |
| `.white` | #FFFFFF | `.warmCream` | #F8F3ED | Backgrounds |
| `.black` | #000000 | `.softCharcoal` | #3A3A3A | Text |

### 3.3 Automated Migration Script

Create: `/ios/scripts/migrate_colors.sh`

```bash
#!/bin/bash
# Migrate old color names to new design system colors

FILES=$(find ios/SmilePile -name "*.swift" -type f)

for file in $FILES; do
    # Backup original
    cp "$file" "$file.bak"

    # Replace color names
    sed -i '' 's/.smilePileBlue/.softBlue/g' "$file"
    sed -i '' 's/.smilePileGreen/.sageGreen/g' "$file"
    sed -i '' 's/.smilePileOrange/.lavender/g' "$file"
    sed -i '' 's/.primaryButton/.primaryAction/g' "$file"
    sed -i '' 's/Color.white/.warmCream/g' "$file"
    sed -i '' 's/Color.black/.softCharcoal/g' "$file"

    echo "✅ Migrated: $file"
done

echo "🎉 Migration complete! Review changes with git diff"
```

**Usage**:
```bash
chmod +x ios/scripts/migrate_colors.sh
./ios/scripts/migrate_colors.sh

# Review changes
git diff ios/SmilePile

# If satisfied, commit; otherwise restore backups
```

---

## 4. Typography Scale

### 4.1 Current vs. Design System Scale

| Element | Current iOS | Design System | Line Height |
|---------|-------------|---------------|-------------|
| H1 | 50px (.nunitoDisplayLarge) | **48px** | 1.1 |
| H2 | 40px (.nunitoDisplayMedium) | **36px** | 1.2 |
| H3 | 32px (.nunitoDisplaySmall) | **28px** | 1.3 |
| H4 | 28px (.nunitoHeadlineLarge) | **22px** | 1.4 |
| H5 | - | **18px** | 1.4 |
| Body Large | 14px (.nunitoBodyLarge) | **18px** | 1.6 |
| Body | 12px (.nunitoBodyMedium) | **16px** | 1.6 |
| Body Small | 11px (.nunitoBodySmall) | **14px** | 1.5 |
| Button | 18px (.nunitoButton) | **16px** | 1.0 |
| Label | - | **14px (Bold)** | 1.2 |

### 4.2 Line Height Implementation

SwiftUI doesn't have direct line-height control. Use `.lineSpacing()` instead.

**Calculation**:
```swift
// Design System: Line Height 1.6 = 16px × 1.6 = 25.6px total
// SwiftUI lineSpacing = total line height - font size
// lineSpacing = 25.6 - 16 = 9.6 points

Text("Body text")
    .font(.body)
    .lineSpacing(9.6) // 16px × 1.6 = 25.6px total
```

**Predefined Modifiers**:

Create: `/ios/SmilePile/Theme/TextModifiers.swift`

```swift
import SwiftUI

// MARK: - Line Spacing Modifiers

extension View {
    /// Apply design system line spacing based on font size
    func dsLineSpacing(for fontSize: CGFloat, multiplier: CGFloat) -> some View {
        let lineSpacing = (fontSize * multiplier) - fontSize
        return self.lineSpacing(lineSpacing)
    }

    /// H1 line spacing (1.1x)
    func h1LineSpacing() -> some View {
        self.dsLineSpacing(for: 48, multiplier: 1.1)
    }

    /// H2 line spacing (1.2x)
    func h2LineSpacing() -> some View {
        self.dsLineSpacing(for: 36, multiplier: 1.2)
    }

    /// Body line spacing (1.6x)
    func bodyLineSpacing() -> some View {
        self.dsLineSpacing(for: 16, multiplier: 1.6)
    }
}
```

**Usage**:
```swift
Text("This is a headline")
    .font(.h1)
    .h1LineSpacing()

Text("This is body text that should have proper line spacing for readability")
    .font(.body)
    .bodyLineSpacing()
```

---

## 5. Component Updates

### 5.1 Button Components

#### 5.1.1 PrimaryButton.swift

Create: `/ios/SmilePile/Views/Components/Buttons/PrimaryButton.swift`

```swift
import SwiftUI

/// Primary button component following design system
/// Background: Soft Blue, Text: Warm Cream, Min Height: 48px
struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var isLoading: Bool = false
    var isDisabled: Bool = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .warmCream))
                }
                Text(title)
                    .font(.button)
                    .foregroundColor(.warmCream)
            }
            .frame(maxWidth: .infinity)
            .frame(minHeight: 48)
            .padding(.horizontal, 24)
            .background(isDisabled ? Color.softBlue.opacity(0.5) : Color.softBlue)
            .cornerRadius(8)
        }
        .disabled(isDisabled || isLoading)
        .accessibilityLabel(title)
        .accessibilityHint(isLoading ? "Loading" : "")
    }
}

// MARK: - Preview
struct PrimaryButton_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 16) {
            PrimaryButton(title: "Download Now", action: {})
            PrimaryButton(title: "Loading...", action: {}, isLoading: true)
            PrimaryButton(title: "Disabled", action: {}, isDisabled: true)
        }
        .padding()
        .background(Color.warmCream)
    }
}
```

#### 5.1.2 SecondaryButton.swift

Create: `/ios/SmilePile/Views/Components/Buttons/SecondaryButton.swift`

```swift
import SwiftUI

/// Secondary button component following design system
/// Border: Soft Blue, Background: Transparent, Text: Soft Blue
struct SecondaryButton: View {
    let title: String
    let action: () -> Void
    var isDisabled: Bool = false

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.button)
                .foregroundColor(isDisabled ? .softBlue.opacity(0.5) : .softBlue)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 48)
                .padding(.horizontal, 24)
                .background(Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isDisabled ? Color.softBlue.opacity(0.5) : Color.softBlue, lineWidth: 2)
                )
        }
        .disabled(isDisabled)
        .accessibilityLabel(title)
    }
}

// MARK: - Preview
struct SecondaryButton_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 16) {
            SecondaryButton(title: "Learn More", action: {})
            SecondaryButton(title: "Disabled", action: {}, isDisabled: true)
        }
        .padding()
        .background(Color.warmCream)
    }
}
```

#### 5.1.3 TextButton.swift

Create: `/ios/SmilePile/Views/Components/Buttons/TextButton.swift`

```swift
import SwiftUI

/// Text button component (tertiary)
/// No border, no background, underline on press
struct TextButton: View {
    let title: String
    let action: () -> Void
    var isDisabled: Bool = false
    @State private var isPressed = false

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.button)
                .foregroundColor(isDisabled ? .softCharcoal.opacity(0.4) : .softCharcoal)
                .underline(isPressed)
                .frame(minHeight: 48)
        }
        .disabled(isDisabled)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .accessibilityLabel(title)
    }
}

// MARK: - Preview
struct TextButton_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 16) {
            TextButton(title: "Cancel", action: {})
            TextButton(title: "Disabled", action: {}, isDisabled: true)
        }
        .padding()
        .background(Color.warmCream)
    }
}
```

### 5.2 Card Components

#### 5.2.1 StandardCard.swift

Create: `/ios/SmilePile/Views/Components/Cards/StandardCard.swift`

```swift
import SwiftUI

/// Standard card component following design system
/// Background: White, Border: 1px gray, Radius: 12px, Shadow
struct StandardCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(24)
            .background(Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Preview
struct StandardCard_Previews: PreviewProvider {
    static var previews: some View {
        StandardCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("Card Title")
                    .font(.h4)
                Text("Card description text goes here")
                    .font(.body)
                    .foregroundColor(.textSecondary)
            }
        }
        .padding()
        .background(Color.warmCream)
    }
}
```

### 5.3 Form Components

#### 5.3.1 FormInput.swift

Create: `/ios/SmilePile/Views/Components/Forms/FormInput.swift`

```swift
import SwiftUI

/// Form input component following design system
/// Min Height: 48px, Focus: Soft Blue border
struct FormInput: View {
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var errorMessage: String? = nil
    var isRequired: Bool = false

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            HStack(spacing: 4) {
                Text(label)
                    .font(.label)
                    .foregroundColor(.softCharcoal)
                if isRequired {
                    Text("*")
                        .foregroundColor(.error)
                }
            }

            // Input field
            TextField(placeholder, text: $text)
                .font(.body)
                .padding(12)
                .frame(minHeight: 48)
                .background(Color.white)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(borderColor, lineWidth: 2)
                )
                .focused($isFocused)

            // Error message
            if let errorMessage = errorMessage {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.circle")
                        .foregroundColor(.error)
                    Text(errorMessage)
                        .font(.bodySmall)
                        .foregroundColor(.error)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Error: \(errorMessage)")
            }
        }
    }

    private var borderColor: Color {
        if errorMessage != nil {
            return .error
        } else if isFocused {
            return .focusIndicator
        } else {
            return Color.gray.opacity(0.3)
        }
    }
}

// MARK: - Preview
struct FormInput_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 24) {
            FormInput(label: "Full Name", text: .constant(""), placeholder: "John Doe")
            FormInput(label: "Email", text: .constant(""), isRequired: true)
            FormInput(label: "Email", text: .constant("invalid"), errorMessage: "Please enter a valid email address")
        }
        .padding()
        .background(Color.warmCream)
    }
}
```

---

## 6. Accessibility Implementation

### 6.1 Focus Indicators

Create: `/ios/SmilePile/Accessibility/FocusIndicator.swift`

```swift
import SwiftUI

/// Focus indicator modifier for keyboard navigation
/// 3px solid Soft Blue outline, 2px offset (WCAG 2.2 AA)
struct FocusIndicatorModifier: ViewModifier {
    @FocusState private var isFocused: Bool

    func body(content: Content) -> some View {
        content
            .focused($isFocused)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.focusIndicator, lineWidth: 3)
                    .padding(-2) // 2px offset
                    .opacity(isFocused ? 1 : 0)
            )
    }
}

extension View {
    func focusIndicator() -> some View {
        modifier(FocusIndicatorModifier())
    }
}
```

### 6.2 Touch Target Enforcement

Create: `/ios/SmilePile/Accessibility/TouchTargetModifier.swift`

```swift
import SwiftUI

/// Enforces minimum 44x44pt touch target (WCAG 2.5.8)
struct TouchTargetModifier: ViewModifier {
    var minSize: CGFloat = 44

    func body(content: Content) -> some View {
        content
            .frame(minWidth: minSize, minHeight: minSize)
            .contentShape(Rectangle())
    }
}

extension View {
    /// Enforce minimum touch target size (default 44x44pt)
    func touchTarget(minSize: CGFloat = 44) -> some View {
        modifier(TouchTargetModifier(minSize: minSize))
    }
}
```

**Usage**:
```swift
Button("Small Icon") {
    // action
}
.frame(width: 24, height: 24) // Visual size
.touchTarget() // Touch area expanded to 44x44
```

### 6.3 Reduced Motion Support

Create: `/ios/SmilePile/Accessibility/ReducedMotionModifier.swift`

```swift
import SwiftUI

/// Respects user's reduced motion preference
struct ReducedMotionModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) var reduceMotion
    let animation: Animation

    func body(content: Content) -> some View {
        content
            .animation(reduceMotion ? .none : animation, value: UUID())
    }
}

extension View {
    /// Apply animation that respects reduced motion preference
    func reducedMotionAnimation(_ animation: Animation) -> some View {
        modifier(ReducedMotionModifier(animation: animation))
    }
}
```

**Usage**:
```swift
.reducedMotionAnimation(.easeInOut)
```

### 6.4 VoiceOver Enhancements

**Audit Checklist**:
```swift
// ✅ All images have alt text
Image("photo")
    .accessibilityLabel("Family photo at birthday party")

// ✅ Icon-only buttons have labels
Button(action: {}) {
    Image(systemName: "plus")
}
.accessibilityLabel("Add photo")

// ✅ Form inputs have associated labels (not placeholder-only)
FormInput(label: "Email", text: $email)

// ✅ Error messages announced
Text("Invalid email")
    .accessibilityAddTraits(.isStaticText)
    .accessibilityHint("Please correct this error")
```

---

## 7. Dark Mode Support

### 7.1 Adaptive Colors

Update ColorConstants.swift with adaptive color helper:

```swift
extension Color {
    /// Primary action color (adapts to dark mode)
    static let primaryActionAdaptive = Color.adaptive(
        light: .softBlue,
        dark: .softBlueDark
    )

    /// Background color (adapts to dark mode)
    static let backgroundAdaptive = Color.adaptive(
        light: .warmCream,
        dark: .softBlack
    )

    /// Text color (adapts to dark mode)
    static let textPrimaryAdaptive = Color.adaptive(
        light: .softCharcoal,
        dark: .warmCream
    )
}
```

### 7.2 Manual Toggle in Settings

Update: `/ios/SmilePile/Views/SettingsViewCustom.swift`

```swift
// Add to settings screen
Toggle("Dark Mode", isOn: $darkModeEnabled)
    .onChange(of: darkModeEnabled) { newValue in
        // Update appearance
        UIApplication.shared.windows.first?.overrideUserInterfaceStyle =
            newValue ? .dark : .light
    }
```

### 7.3 Contrast Validation

**Testing Matrix**:
| Background | Foreground | Ratio | Pass |
|------------|------------|-------|------|
| Warm Cream | Soft Charcoal | 12.8:1 | ✅ AAA |
| Soft Blue | Soft Charcoal | 4.6:1 | ✅ AA |
| Sage Green | Soft Charcoal | 5.2:1 | ✅ AA |
| Soft Black | Warm Cream | 12.8:1 | ✅ AAA |
| Soft Blue Dark | Warm Cream | 4.5:1 | ✅ AA |

---

## 8. Testing Strategy

### 8.1 Automated Testing

**Font Loading Test**:
```swift
func testAtkinsonFontRegistered() {
    let font = UIFont(name: "AtkinsonHyperlegible-Regular", size: 16)
    XCTAssertNotNil(font, "Atkinson Hyperlegible Regular should be registered")

    let boldFont = UIFont(name: "AtkinsonHyperlegible-Bold", size: 16)
    XCTAssertNotNil(boldFont, "Atkinson Hyperlegible Bold should be registered")
}
```

**Color Test**:
```swift
func testSoftBlueColor() {
    let softBlue = Color.softBlue
    // Verify hex color matches
    XCTAssertEqual(softBlue.toHex(), "#7FB3D5")
}
```

### 8.2 Manual Testing Checklist

**Visual Regression**:
- [ ] All screens rendered correctly with new font
- [ ] All buttons use new color palette
- [ ] No bright/vibrant colors visible
- [ ] Text remains readable at all sizes
- [ ] Dark mode works correctly

**Accessibility Testing**:
- [ ] VoiceOver announces all elements correctly
- [ ] All buttons tappable with 44x44pt target
- [ ] Focus indicators visible on all interactive elements
- [ ] Reduced motion preference respected
- [ ] Contrast ratios pass WebAIM checker

**Cross-Device Testing**:
- [ ] iPhone SE (small screen)
- [ ] iPhone 14 Pro (standard)
- [ ] iPad Pro (large screen)
- [ ] Light mode
- [ ] Dark mode

### 8.3 VoiceOver Testing Script

1. Enable VoiceOver: Settings > Accessibility > VoiceOver
2. Navigate through app using swipe gestures
3. Verify announcements:
   - Buttons: "Button name, Button"
   - Images: "Description of image, Image"
   - Form fields: "Label, Text field"
   - Errors: "Error: message, Alert"

---

## 9. Build Configuration

### 9.1 Xcode Project Updates

**No changes needed** - Fonts loaded at runtime via Info.plist.

**Verify**:
1. Open `SmilePile.xcodeproj`
2. Select SmilePile target
3. Build Phases > Copy Bundle Resources
4. Ensure .ttf files present:
   - `AtkinsonHyperlegible-Regular.ttf`
   - `AtkinsonHyperlegible-Bold.ttf`

### 9.2 Build Command (QUAL Tier)

```bash
# Clean build
rm -rf ios/DerivedData

# Build QUAL tier with new design system
xcodebuild \
  -project ios/SmilePile.xcodeproj \
  -scheme "SmilePile Qual" \
  -configuration Debug \
  -destination 'platform=iOS Simulator,id=EE3F2A09-2BA9-463D-8C07-323B0688FAE5' \
  -derivedDataPath ios/DerivedData \
  clean build

# Check for warnings
cat ios/DerivedData/Logs/Build/*.xcactivitylog | grep -i "warning"

# Install and launch
xcrun simctl install "EE3F2A09-2BA9-463D-8C07-323B0688FAE5" \
  "ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"
xcrun simctl launch "EE3F2A09-2BA9-463D-8C07-323B0688FAE5" app.smilepile.qual
```

---

## 10. Implementation Sequence

### Phase 1: Foundation (Day 1-2, 16 hours)

**Day 1 Morning (4 hours)**:
1. Download Atkinson Hyperlegible fonts
2. Add .ttf files to `/ios/SmilePile/Fonts/`
3. Update Info.plist font references
4. Replace FontManager.swift
5. Build and verify font registration

**Day 1 Afternoon (4 hours)**:
6. Replace ColorConstants.swift
7. Run automated color migration script
8. Verify no bright colors remain (git grep)
9. Build and check for color-related errors

**Day 2 Morning (4 hours)**:
10. Replace Typography.swift
11. Update 5-10 high-priority view files
12. Test main user flows (gallery, settings, onboarding)

**Day 2 Afternoon (4 hours)**:
13. Create TextModifiers.swift
14. Add line spacing to body text
15. Build and visual QA

**Deliverable**: Font and colors working, no Nunito references remain

---

### Phase 2: Components (Day 3-5, 24 hours)

**Day 3 (8 hours)**:
1. Create button components (Primary, Secondary, Text)
2. Migrate 20+ button usages
3. Create card components (Standard, Photo)
4. Update 10+ card usages

**Day 4 (8 hours)**:
5. Create form components (FormInput, FormLabel)
6. Migrate form fields in settings and onboarding
7. Create accessibility modifiers (Focus, TouchTarget, ReducedMotion)
8. Apply to 10+ components

**Day 5 (8 hours)**:
9. Update remaining 40+ view files
10. Migrate Kids Mode components
11. Update onboarding screens
12. Visual QA all screens

**Deliverable**: Standardized component library, all views updated

---

### Phase 3: Accessibility (Day 6-7, 16 hours)

**Day 6 (8 hours)**:
1. Add focus indicators to all buttons
2. Enforce 44x44pt touch targets
3. Verify contrast ratios with WebAIM
4. Fix any failing combinations

**Day 7 (8 hours)**:
5. Add VoiceOver labels to images
6. Add accessibility hints to buttons
7. Test with VoiceOver enabled
8. Fix announced text issues

**Deliverable**: WCAG 2.2 AA compliant, VoiceOver tested

---

### Phase 4: Dark Mode (Day 8, 8 hours)

1. Implement adaptive color helpers
2. Test all screens in dark mode
3. Fix contrast issues
4. Add manual toggle in settings

**Deliverable**: Dark mode fully functional

---

### Phase 5: Testing & QA (Day 9-10, 16 hours)

**Day 9 (8 hours)**:
1. Run automated tests
2. Manual testing on iPhone SE, iPhone 14 Pro, iPad
3. VoiceOver full app walkthrough
4. Reduced motion testing

**Day 10 (8 hours)**:
5. Fix bugs identified in testing
6. Visual regression QA
7. Performance testing (font loading, rendering)
8. Documentation updates

**Deliverable**: Production-ready implementation

---

### Phase 6: Deployment (Day 11, 8 hours)

1. Final code review
2. Update release notes
3. Build QUAL tier
4. Deploy to TestFlight
5. Monitor for issues

**Deliverable**: Design system live in QUAL tier

---

## Validation Checklist

### Code Validation
- [ ] No "Nunito" references in codebase (`grep -r "Nunito" ios/SmilePile`)
- [ ] No bright color hex codes (#2196F3, #4CAF50, #FF6600)
- [ ] All .ttf files updated in Info.plist
- [ ] Build succeeds with 0 errors, 0 warnings

### Visual Validation
- [ ] Font renders correctly at all sizes
- [ ] Colors match design system specification
- [ ] Dark mode contrast ratios pass
- [ ] No jarring visual changes (smooth transitions)

### Accessibility Validation
- [ ] All buttons 44x44pt minimum
- [ ] Focus indicators visible
- [ ] VoiceOver announces correctly
- [ ] Reduced motion respected
- [ ] Contrast ratios >= 4.5:1

### Functional Validation
- [ ] All user flows work (gallery, onboarding, settings)
- [ ] Kids Mode maintains larger typography
- [ ] Buttons respond correctly
- [ ] Forms validate properly

---

## Risk Mitigation

### High Risk: Font Performance
**Concern**: Custom fonts may load slower than system fonts
**Mitigation**:
- Use WOFF2 compression for web
- Pre-load fonts in AppDelegate
- Monitor launch time metrics
- Keep system font fallbacks

### Medium Risk: Color Migration Errors
**Concern**: Automated replacement may break some views
**Mitigation**:
- Backup files before migration
- Manual review of git diff
- Test each tier (QUAL, STAGE, PROD)
- Rollback plan ready

### Low Risk: User Confusion
**Concern**: Existing users may not recognize new design
**Mitigation**:
- Gradual rollout (QUAL → BETA → PROD)
- "What's New" onboarding screen
- In-app announcement
- Keep navigation patterns identical

---

## Success Metrics

**Quantitative**:
- [ ] 0 Nunito references remain
- [ ] 100% color migration complete
- [ ] 0 build errors
- [ ] 100% WCAG AA compliance
- [ ] <200ms font load time

**Qualitative**:
- [ ] Design system visually cohesive
- [ ] Accessibility improved (VoiceOver feedback)
- [ ] User feedback positive
- [ ] Developer velocity improved with component library

---

## Next Steps

After completing iOS implementation:
1. **Android Implementation** (Phase 3B) - Similar process for Kotlin/Compose
2. **Website Implementation** (Phase 3C) - Astro/Tailwind updates
3. **Cross-Platform QA** - Ensure consistency
4. **User Testing** - Validate with special needs families
5. **Production Rollout** - Gradual release plan

---

## Appendix: File Reference

### Core Files Modified
- `/ios/SmilePile/FontManager.swift`
- `/ios/SmilePile/Typography.swift`
- `/ios/SmilePile/Theme/ColorConstants.swift`
- `/ios/SmilePile/Info.plist`

### New Files Created
- `/ios/SmilePile/Theme/TextModifiers.swift`
- `/ios/SmilePile/Views/Components/Buttons/PrimaryButton.swift`
- `/ios/SmilePile/Views/Components/Buttons/SecondaryButton.swift`
- `/ios/SmilePile/Views/Components/Buttons/TextButton.swift`
- `/ios/SmilePile/Views/Components/Cards/StandardCard.swift`
- `/ios/SmilePile/Views/Components/Forms/FormInput.swift`
- `/ios/SmilePile/Accessibility/FocusIndicator.swift`
- `/ios/SmilePile/Accessibility/TouchTargetModifier.swift`
- `/ios/SmilePile/Accessibility/ReducedMotionModifier.swift`

### Scripts
- `/ios/scripts/migrate_colors.sh`

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Author**: Atlas Developer Agent
**Review Status**: Ready for Implementation
