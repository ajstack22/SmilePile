# iOS Onboarding Implementation Plan

## Executive Summary

This plan outlines the complete iOS onboarding update to achieve parity with the Android implementation. The work is divided into **24 testable chunks** that will transform the current 5-screen flow (including PhotoImport) to match Android's 4-screen flow with updated terminology, colors, and visual design.

**Total Estimated Time**: 20-30 hours (24 chunks × 30-90 minutes each)
**Risk Level**: Medium - Most changes are visual/textual with minimal architectural impact
**Testing Strategy**: Each chunk independently verifiable with build tests and visual comparisons

## Implementation Order

| Phase | Chunks | Description | Priority |
|-------|--------|-------------|----------|
| Foundation | CHUNK-001 to CHUNK-005 | Color system, typography, icons | CRITICAL |
| Navigation | CHUNK-006 to CHUNK-008 | Flow control, progress removal | HIGH |
| Terminology | CHUNK-009 to CHUNK-013 | Categories → Piles everywhere | HIGH |
| Visual Parity | CHUNK-014 to CHUNK-020 | Screen-by-screen updates | MEDIUM |
| Polish | CHUNK-021 to CHUNK-024 | Animations, shadows, final QA | LOW |

## Detailed Chunk Specifications

---

## CHUNK-001: Create Color Constants File

**Estimated Time**: 30 minutes
**Dependencies**: None
**Risk Level**: Low

### Files to Create:
- `/Users/adamstack/SmilePile/ios/SmilePile/Theme/ColorConstants.swift`

### Detailed Tasks:
1. Create new directory `/Users/adamstack/SmilePile/ios/SmilePile/Theme/`
2. Create `ColorConstants.swift` with SmilePile brand colors
3. Add file to Xcode project (SmilePile target)

### Code to Write:
```swift
// ColorConstants.swift
import SwiftUI

extension Color {
    // SmilePile Brand Colors
    static let smilePileYellow = Color(hex: "#FFBF00")!  // Smile character
    static let smilePileGreen = Color(hex: "#4CAF50")!   // P character
    static let smilePileBlue = Color(hex: "#2196F3")!    // i character, buttons
    static let smilePileOrange = Color(hex: "#FF6600")!  // l character, icons
    static let smilePilePink = Color(hex: "#E86082")!    // e character

    // Category/Pile Colors
    static let pileRed = Color(hex: "#FF6B6B")!
    static let pileTeal = Color(hex: "#4ECDC4")!
    static let pileYellow = Color(hex: "#FFEAA7")!

    // Additional Palette Colors
    static let paletteColors = [
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
    ]

    // UI Colors
    static let primaryButton = smilePileBlue
    static let secondaryText = Color.secondary
}
```

### Acceptance Criteria:
1. ✅ File compiles without errors
2. ✅ All brand colors defined with exact hex values from Android
3. ✅ Color extension doesn't conflict with existing Color(hex:) in Photo.swift
4. ✅ Colors accessible via `Color.smilePilePink` syntax
5. ✅ File added to Xcode project navigator
6. ✅ Build succeeds: `xcodebuild -project ios/SmilePile.xcodeproj -scheme SmilePile -sdk iphonesimulator clean build`
7. ✅ No compilation warnings

### Evidence Required:
1. Screenshot of Xcode showing file in project navigator
2. Screenshot of successful build output
3. Git diff showing new file with all color definitions

### Peer Review Checklist:
- [ ] Hex values match Android spec exactly (#FFBF00 for yellow, etc.)
- [ ] All required colors present
- [ ] File properly added to Xcode project
- [ ] No duplicate Color extensions
- [ ] Build completes without errors

---

## CHUNK-002: Update Icon References ✅ COMPLETED

**Estimated Time**: 45 minutes
**Dependencies**: None
**Risk Level**: Low
**Status**: ✅ Committed (a9887c8..HEAD)
**Completed**: 2025-09-30

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/WelcomeScreen.swift`
- Any other files using "folder" icons

### Detailed Tasks:
1. Replace "folder.fill" with "square.stack" (iOS equivalent of Android's Layers)
2. Update icon sizes to match Android (48dp → 48 points)
3. Update icon colors to use new constants

### Code Changes:

**WelcomeScreen.swift** (Line 32):
```swift
// OLD:
FeatureRow(
    icon: "folder.fill",
    title: "Organize photos",
    description: "Create colorful categories"
)

// NEW:
FeatureRow(
    icon: "square.stack",
    title: "Organize photos into piles",  // Note: terminology update
    description: "Create colorful piles for your photos"
)
```

**WelcomeScreen.swift** (Line 38-39):
```swift
// OLD:
FeatureRow(
    icon: "photo.fill",
    title: "Import memories",
    description: "Add your favorite photos"
)

// NEW:
FeatureRow(
    icon: "viewfinder",  // Better match for Android's FitScreen
    title: "Distraction-free mode",
    description: "Good for kids (and everyone else)"
)
```

### Acceptance Criteria:
1. ✅ All folder icons replaced with square.stack
2. ✅ Icon colors use ColorConstants
3. ✅ Icons visually similar to Android equivalents
4. ✅ No "folder" references remain in onboarding
5. ✅ Build succeeds without warnings
6. ✅ Icons display correctly in simulator

### Evidence Required:
1. Screenshot comparing iOS icons to Android
2. Git diff showing all icon changes
3. Search results showing no "folder" in onboarding files

### Peer Review Checklist:
- [ ] Square.stack icon used for Piles/Categories
- [ ] Viewfinder icon used for distraction-free
- [ ] Lock icon remains for PIN
- [ ] All icons use brand colors
- [ ] No folder references in UI

---

## CHUNK-003: Implement Nunito Font Support

**Estimated Time**: 60 minutes
**Dependencies**: None
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Theme/ColorConstants.swift` (add font extensions)

### Detailed Tasks:
1. Create Font extension for Nunito variants
2. Map Android font weights to iOS equivalents
3. Test font loading

### Code to Add:
```swift
// Add to ColorConstants.swift

extension Font {
    // Nunito Font Family
    static func nunito(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        switch weight {
        case .black:
            return Font.custom("Nunito-Black", size: size)
        case .heavy, .bold:
            return Font.custom("Nunito-Bold", size: size)
        case .semibold, .medium:
            return Font.custom("Nunito-ExtraBold", size: size)
        default:
            return Font.custom("Nunito-Variable", size: size)
        }
    }

    // Convenience methods matching Android
    static let nunitoTitle = nunito(36, weight: .heavy)  // ExtraBold
    static let nunitoHeadline = nunito(22, weight: .bold)
    static let nunitoBody = nunito(16, weight: .medium)
    static let nunitoCaption = nunito(14, weight: .regular)
    static let nunitoButton = nunito(18, weight: .bold)
}
```

### Acceptance Criteria:
1. ✅ Nunito fonts load correctly
2. ✅ Font weights map to Android equivalents
3. ✅ No font loading errors in console
4. ✅ Fonts display in simulator
5. ✅ Build succeeds
6. ✅ Font files confirmed in bundle

### Evidence Required:
1. Screenshot showing Nunito font in use
2. Console output showing no font errors
3. Build log showing success

### Peer Review Checklist:
- [ ] All Nunito TTF files present in project
- [ ] Font extension matches Android weights
- [ ] Custom fonts load without errors
- [ ] Fallback to system font if custom fails
- [ ] Info.plist contains font entries

---

## CHUNK-004: Create Multicolor SmilePile Logo Component ✅ COMPLETED

**Estimated Time**: 90 minutes
**Dependencies**: CHUNK-001 (colors), CHUNK-003 (fonts)
**Risk Level**: Medium
**Status**: ✅ Committed (88c8123..HEAD)
**Completed**: 2025-09-30

### Files to Create:
- `/Users/adamstack/SmilePile/ios/SmilePile/Components/MulticolorSmilePileLogo.swift`

### Code to Write:
```swift
import SwiftUI

struct MulticolorSmilePileLogo: View {
    let fontSize: CGFloat
    let showShadow: Bool

    init(fontSize: CGFloat = 36, showShadow: Bool = true) {
        self.fontSize = fontSize
        self.showShadow = showShadow
    }

    var body: some View {
        HStack(spacing: 0) {
            Text("Smile")
                .foregroundColor(.smilePileYellow)
                .font(.nunito(fontSize, weight: .heavy))

            Text("P")
                .foregroundColor(.smilePileGreen)
                .font(.nunito(fontSize, weight: .heavy))

            Text("i")
                .foregroundColor(.smilePileBlue)
                .font(.nunito(fontSize, weight: .heavy))

            Text("l")
                .foregroundColor(.smilePileOrange)
                .font(.nunito(fontSize, weight: .heavy))

            Text("e")
                .foregroundColor(.smilePilePink)
                .font(.nunito(fontSize, weight: .heavy))
        }
        .if(showShadow) { view in
            view.shadow(
                color: .black.opacity(0.9),
                radius: 6,
                x: 4,
                y: 4
            )
        }
    }
}

// Helper modifier
extension View {
    @ViewBuilder
    func `if`<Transform: View>(_ condition: Bool, transform: (Self) -> Transform) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Each character has correct color
2. ✅ Shadow matches Android (black 90%, offset 4,4, blur 6)
3. ✅ Nunito ExtraBold font used
4. ✅ Component reusable with different sizes
5. ✅ Build succeeds
6. ✅ Visual match to Android logo

### Evidence Required:
1. Screenshot of logo side-by-side with Android
2. Code showing shadow parameters
3. Different size variations working

### Peer Review Checklist:
- [ ] Colors match exactly (Yellow, Green, Blue, Orange, Pink)
- [ ] Shadow parameters exact (90% opacity, 4pt offset, 6pt blur)
- [ ] Font is Nunito ExtraBold/Heavy
- [ ] Spacing between characters correct
- [ ] Component is reusable

---

## CHUNK-005: Update Settings Manager for Onboarding

**Estimated Time**: 45 minutes
**Dependencies**: None
**Risk Level**: Low

### Files to Examine and Potentially Modify:
- Search for SettingsManager or UserDefaults usage
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingCoordinator.swift`

### Detailed Tasks:
1. Verify onboarding completion flag matches Android
2. Ensure firstLaunch flag is set correctly
3. Add any missing settings keys

### Code Changes:

**OnboardingCoordinator.swift** (Line 173):
```swift
// OLD:
UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")

// NEW:
UserDefaults.standard.set(true, forKey: "onboardingCompleted")  // Match Android key
UserDefaults.standard.set(false, forKey: "firstLaunch")  // Also set this flag
```

### Acceptance Criteria:
1. ✅ Settings keys match Android exactly
2. ✅ Onboarding completion properly tracked
3. ✅ App doesn't show onboarding after completion
4. ✅ Reset functionality works
5. ✅ Build succeeds

### Evidence Required:
1. UserDefaults keys matching Android
2. Test showing onboarding only on first launch
3. Settings persistence after app restart

### Peer Review Checklist:
- [ ] Key names match Android ("onboardingCompleted")
- [ ] Both flags set on completion
- [ ] Settings persist across launches
- [ ] Can trigger onboarding reset
- [ ] No duplicate key usage

---

## CHUNK-006: Remove Photo Import from Navigation Flow

**Estimated Time**: 60 minutes
**Dependencies**: None
**Risk Level**: High (flow change)

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingCoordinator.swift`
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingView.swift`

### Detailed Tasks:
1. Update navigation flow to skip PhotoImport
2. Keep PhotoImport code but remove from flow
3. Update step enumeration

### Code Changes:

**OnboardingCoordinator.swift** (Lines 79-89):
```swift
// OLD:
func navigateToNext() {
    guard validateCurrentStep() else { return }
    navigationHistory.append(currentStep)

    switch currentStep {
    case .welcome:
        currentStep = .categories
    case .categories:
        currentStep = .photoImport  // REMOVE THIS
    case .photoImport:
        currentStep = .pinSetup
    case .pinSetup:
        completeOnboarding()
    case .complete:
        break
    }
}

// NEW:
func navigateToNext() {
    guard validateCurrentStep() else { return }
    navigationHistory.append(currentStep)

    switch currentStep {
    case .welcome:
        currentStep = .categories
    case .categories:
        currentStep = .pinSetup  // Skip directly to PIN
    case .photoImport:
        currentStep = .pinSetup  // Keep for compatibility but not used
    case .pinSetup:
        completeOnboarding()
    case .complete:
        break
    }
}
```

### Acceptance Criteria:
1. ✅ Flow goes Welcome → Categories → PIN → Complete
2. ✅ PhotoImport screen never shown
3. ✅ PhotoImport code still compiles (not deleted)
4. ✅ Navigation works correctly
5. ✅ Back button works properly
6. ✅ Build and run succeeds

### Evidence Required:
1. Video showing navigation flow
2. Git diff showing flow changes
3. Confirmation PhotoImportScreen.swift still exists

### Peer Review Checklist:
- [ ] PhotoImport removed from flow only
- [ ] File not deleted (per requirements)
- [ ] Navigation sequence matches Android
- [ ] Back navigation still works
- [ ] Skip logic updated if needed

---

## CHUNK-007: Remove Progress Bar from Onboarding

**Estimated Time**: 30 minutes
**Dependencies**: CHUNK-006
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingView.swift`
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingCoordinator.swift`

### Detailed Tasks:
1. Remove ProgressView from OnboardingView
2. Remove progress calculation from coordinator
3. Clean up spacing

### Code Changes:

**OnboardingView.swift** (Lines 21-26):
```swift
// DELETE THESE LINES:
if coordinator.currentStep != .welcome && coordinator.currentStep != .complete {
    ProgressView(value: coordinator.progress)
        .progressViewStyle(LinearProgressViewStyle(tint: Color(red: 1.0, green: 0.42, blue: 0.42)))
        .padding()
}
```

**OnboardingCoordinator.swift** (Lines 67-70):
```swift
// DELETE THESE LINES:
var progress: Double {
    Double(currentStep.rawValue) / Double(OnboardingStep.allCases.count - 1)
}
```

### Acceptance Criteria:
1. ✅ No progress bar visible anywhere
2. ✅ Layout spacing adjusted correctly
3. ✅ No progress-related code remains
4. ✅ Build succeeds
5. ✅ UI looks clean without bar

### Evidence Required:
1. Screenshot showing no progress bar
2. Git diff showing removal
3. Visual comparison with Android

### Peer Review Checklist:
- [ ] Progress bar completely removed
- [ ] Progress calculation removed
- [ ] No visual gaps in layout
- [ ] Matches Android (no progress)
- [ ] Clean code removal

---

## CHUNK-008: Update Navigation Bar Styling

**Estimated Time**: 45 minutes
**Dependencies**: CHUNK-007
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingView.swift`

### Detailed Tasks:
1. Update navigation bar to match Android styling
2. Move title to proper position
3. Ensure back button only on correct screens

### Code Changes:

**OnboardingView.swift** (Lines 28-58):
```swift
// OLD: Complex navigation bar
// NEW: Simplified to match Android

if coordinator.currentStep == .categories || coordinator.currentStep == .pinSetup {
    HStack {
        Button(action: {
            coordinator.navigateBack()
        }) {
            Image(systemName: "chevron.left")
                .font(.title2)
                .foregroundColor(.primary)
        }
        .padding()

        Spacer()

        Text(coordinator.currentStep == .categories ? "Create Piles" : "PIN Setup")
            .font(.nunito(18, weight: .semibold))

        Spacer()

        // Skip button only for PIN
        if coordinator.currentStep == .pinSetup {
            // Skip handled within PIN screen
            Color.clear
                .frame(width: 44, height: 44)
                .padding()
        } else {
            Color.clear
                .frame(width: 44, height: 44)
                .padding()
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Back button only on Categories and PIN screens
2. ✅ Title text matches Android style
3. ✅ No navigation on Welcome/Complete
4. ✅ Transparent background
5. ✅ Proper spacing

### Evidence Required:
1. Screenshots of each screen's navigation
2. Comparison with Android navigation
3. Back button functionality test

### Peer Review Checklist:
- [ ] Navigation matches Android exactly
- [ ] Back button placement correct
- [ ] Title styling consistent
- [ ] No progress indicators
- [ ] Clean, minimal design

---

## CHUNK-009: Update Onboarding Step Titles (Categories → Piles)

**Estimated Time**: 30 minutes
**Dependencies**: None
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/OnboardingCoordinator.swift`

### Code Changes:

**OnboardingCoordinator.swift** (Lines 12-25):
```swift
var title: String {
    switch self {
    case .welcome:
        return "Welcome"
    case .categories:
        return "Create Piles"  // Changed from "Create Categories"
    case .photoImport:
        return "Add Photos"
    case .pinSetup:
        return "PIN Setup"  // Changed from "Security Setup"
    case .complete:
        return "All Set!"
    }
}
```

### Acceptance Criteria:
1. ✅ "Categories" → "Piles" in title
2. ✅ "Security Setup" → "PIN Setup"
3. ✅ Titles display correctly
4. ✅ Build succeeds
5. ✅ No "Categories" in user-facing text

### Evidence Required:
1. Screenshot of "Create Piles" title
2. Git diff showing changes
3. Search showing no "Categories" in titles

### Peer Review Checklist:
- [ ] All titles match Android spec
- [ ] No "Categories" visible to users
- [ ] Code variables can stay as "categories"
- [ ] Consistency throughout
- [ ] Professional terminology

---

## CHUNK-010: Update Welcome Screen Text and Layout

**Estimated Time**: 60 minutes
**Dependencies**: CHUNK-001, CHUNK-003, CHUNK-004
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/WelcomeScreen.swift`

### Complete Rewrite:
```swift
import SwiftUI

struct WelcomeScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator

    var body: some View {
        VStack(spacing: 30) {
            Spacer()

            // Logo and title section
            VStack(spacing: 16) {
                // App icon
                Image("smilepile_logo")  // Or use SF Symbol temporarily
                    .resizable()
                    .scaledToFit()
                    .frame(width: 100, height: 100)

                // Multicolor app name
                MulticolorSmilePileLogo(fontSize: 36, showShadow: true)

                // Tagline
                Text("A safe and fun photo gallery for EVERYONE")
                    .font(.nunito(18, weight: .regular))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            Spacer()

            // Features list
            VStack(alignment: .leading, spacing: 24) {
                FeatureRow(
                    icon: "square.stack",
                    iconColor: .smilePileYellow,
                    title: "Organize photos into piles",
                    description: "Create colorful piles for your photos"
                )

                FeatureRow(
                    icon: "viewfinder",
                    iconColor: .smilePileOrange,
                    title: "Distraction-free mode",
                    description: "Good for kids (and everyone else)"
                )

                FeatureRow(
                    icon: "lock.fill",
                    iconColor: .smilePileGreen,
                    title: "Optional PIN protection",
                    description: "Prevent inadvertent changes"
                )
            }
            .padding(.horizontal, 40)

            Spacer()

            // Get Started button
            Button(action: {
                coordinator.navigateToNext()
            }) {
                Text("Get Started")
                    .font(.nunito(18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.smilePileBlue)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 50)
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(iconColor)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.nunito(16, weight: .semibold))
                    .foregroundColor(.primary)

                Text(description)
                    .font(.nunito(14, weight: .regular))
                    .foregroundColor(.secondary)
            }
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Multicolor logo displays correctly
2. ✅ "EVERYONE" in caps in tagline
3. ✅ Three features with correct icons/colors
4. ✅ Button is 56pt height with blue color
5. ✅ All text uses Nunito font
6. ✅ Matches Android layout

### Evidence Required:
1. Screenshot comparison with Android
2. All three feature rows visible
3. Multicolor logo with shadow

### Peer Review Checklist:
- [ ] Logo has all 5 colors with shadow
- [ ] Feature icons match Android
- [ ] Button height exactly 56pt
- [ ] "Piles" terminology used
- [ ] Font weights correct

---

## CHUNK-011: Update Category Setup Screen - Part 1 (Header and Terminology)

**Estimated Time**: 45 minutes
**Dependencies**: CHUNK-009
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`

### Code Changes:

**Lines 30-37** (Update header):
```swift
// OLD:
Text("Create Categories")
    .font(.title2)
    .fontWeight(.bold)

Text("Organize your photos into colorful categories")
    .font(.subheadline)

// NEW:
Image(systemName: "square.stack")
    .font(.system(size: 48))
    .foregroundColor(.smilePileOrange)
    .padding(.bottom, 16)

Text("Create Piles")
    .font(.nunito(22, weight: .bold))

Text("Organize your photos into colorful piles")
    .font(.nunito(14, weight: .regular))
```

**Lines 46-47** (Section header):
```swift
// OLD:
Text("Quick Add")

// NEW:
Text("Or Quick Add")
    .font(.nunito(16, weight: .medium))
```

**Lines 72-75** (Custom section):
```swift
// OLD:
Text("Create Custom")

// NEW:
Text("Create Your Own")
    .font(.nunito(16, weight: .medium))
```

### Acceptance Criteria:
1. ✅ Orange layers icon at top
2. ✅ "Piles" used throughout
3. ✅ Section headers match Android
4. ✅ Nunito fonts used
5. ✅ Icon is 48pt size

### Evidence Required:
1. Screenshot of updated header
2. Text showing "Piles" not "Categories"
3. Orange icon visible

### Peer Review Checklist:
- [ ] All user-facing text says "Piles"
- [ ] Icon color is orange (#FF6600)
- [ ] Font sizes match spec
- [ ] Section titles correct
- [ ] No "Categories" visible

---

## CHUNK-012: Update Category Setup Screen - Part 2 (Your Piles Section)

**Estimated Time**: 60 minutes
**Dependencies**: CHUNK-011
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`

### Code Changes:

**Lines 145-170** (Your Categories section):
```swift
// OLD:
Text("Your Categories")
    .font(.headline)

// NEW:
Text("Your Piles")
    .font(.nunito(18, weight: .semibold))

// Update the counter display
Text("\(coordinator.onboardingData.categories.count)/5")
    .font(.nunito(14, weight: .regular))
    .foregroundColor(.secondary)

// Update the empty state message (line 178):
Text("Add at least one pile to continue")
    .font(.nunito(12, weight: .regular))
    .foregroundColor(.secondary)
```

**Update CreatedCategoryRow** (Lines 257-287):
```swift
struct CreatedCategoryRow: View {
    let category: TempCategory
    let onRemove: () -> Void

    var body: some View {
        HStack {
            Circle()
                .fill(Color(hex: category.colorHex))
                .frame(width: 16, height: 16)  // Increased from 12

            Text(category.name)
                .font(.nunito(16, weight: .regular))
                .foregroundColor(.primary)

            Spacer()

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 24))  // Specified size
                    .foregroundColor(.gray.opacity(0.5))
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(hex: category.colorHex).opacity(0.1))
        )
    }
}
```

### Acceptance Criteria:
1. ✅ "Your Piles" header displays
2. ✅ Counter shows X/5 format
3. ✅ Color dots are 16pt
4. ✅ Background uses 10% opacity
5. ✅ Remove button properly sized

### Evidence Required:
1. Screenshot with piles created
2. Counter showing correctly
3. Color dots visible

### Peer Review Checklist:
- [ ] "Piles" terminology everywhere
- [ ] Color dots 16pt circles
- [ ] Opacity at 10% for backgrounds
- [ ] Font weights correct
- [ ] Layout matches Android

---

## CHUNK-013: Update Category Setup Screen - Part 3 (Buttons and Validation)

**Estimated Time**: 45 minutes
**Dependencies**: CHUNK-012
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`

### Code Changes:

**Lines 118-134** (Add button):
```swift
// OLD:
Text("Add Category")
    .font(.subheadline)

// NEW:
Text("Add Pile")
    .font(.nunito(16, weight: .medium))
    .foregroundColor(.white)
    .frame(maxWidth: .infinity)
    .frame(height: 44)  // Specific height
    .background(
        newCategoryName.isEmpty ?
        Color.gray.opacity(0.3) :
        Color.smilePileBlue
    )
    .cornerRadius(8)
```

**Lines 183-198** (Continue button):
```swift
// NEW:
Button(action: {
    coordinator.navigateToNext()
}) {
    Text("Continue")
        .font(.nunito(18, weight: .bold))
        .foregroundColor(.white)
        .frame(maxWidth: .infinity)
        .frame(height: 56)  // Match Android
        .background(
            coordinator.onboardingData.categories.isEmpty ?
            Color.gray.opacity(0.3) :
            Color.smilePileBlue
        )
        .cornerRadius(12)
}
```

**Update validation message** (Line 119):
```swift
// OLD error message
showError(message: "Please create at least one category")

// NEW:
showError(message: "Please create at least one pile")
```

### Acceptance Criteria:
1. ✅ Button heights match spec (56pt primary, 44pt secondary)
2. ✅ Blue color when enabled
3. ✅ Gray when disabled
4. ✅ "Pile" in all messages
5. ✅ Validation works correctly

### Evidence Required:
1. Screenshot of enabled/disabled states
2. Error message showing "pile"
3. Button heights verified

### Peer Review Checklist:
- [ ] Continue button 56pt height
- [ ] Add button 44pt height
- [ ] Blue color from constants
- [ ] Disabled state styling
- [ ] All validation messages updated

---

## CHUNK-014: Update Category Quick Add Presets

**Estimated Time**: 30 minutes
**Dependencies**: CHUNK-013
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`

### Code Changes:

**Lines 10-17** (Suggested categories):
```swift
// OLD:
let suggestedCategories = [
    ("Family", "#FF6B6B", "👨‍👩‍👧‍👦"),
    ("Friends", "#4ECDC4", "👫"),
    ("Vacation", "#45B7D1", "🏖️"),
    ("Pets", "#96CEB4", "🐾"),
    ("Fun", "#FFEAA7", "🎉"),
    ("School", "#DDA0DD", "🎒")
]

// NEW (Only 3, matching Android):
let suggestedCategories = [
    ("Family", "#FF6B6B", nil),  // No emoji
    ("Friends", "#4ECDC4", nil),
    ("Fun", "#FFEAA7", nil)
]
```

**Update SuggestedCategoryCard** (Lines 227-255):
```swift
struct SuggestedCategoryCard: View {
    let name: String
    let colorHex: String
    let onAdd: () -> Void

    var body: some View {
        Button(action: onAdd) {
            HStack {
                Image(systemName: "plus")
                    .font(.system(size: 20))
                    .foregroundColor(Color(hex: colorHex))

                Text(name)
                    .font(.nunito(16, weight: .medium))
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 20)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: colorHex).opacity(0.15))
            )
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Only 3 presets (Family, Friends, Fun)
2. ✅ No emojis in suggestions
3. ✅ Plus icon on left
4. ✅ 56pt height cards
5. ✅ 15% opacity backgrounds

### Evidence Required:
1. Screenshot showing 3 presets only
2. Plus icons visible
3. Correct colors and opacity

### Peer Review Checklist:
- [ ] Exactly 3 presets
- [ ] Colors match spec
- [ ] No emoji icons
- [ ] Card height 56pt
- [ ] Opacity at 15%

---

## CHUNK-015: Update PIN Setup Screen - Visual Design

**Estimated Time**: 75 minutes
**Dependencies**: CHUNK-001, CHUNK-003
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/PINSetupScreen.swift`

### Major Updates:

**Lines 18-34** (Header section):
```swift
// NEW:
VStack(spacing: 16) {
    Image(systemName: "lock.fill")
        .font(.system(size: 64))
        .foregroundColor(.smilePileYellow)  // Yellow not pink
        .padding(.bottom, 20)

    Text(isConfirming ? "Confirm Your PIN" : "Set Up PIN Protection")
        .font(.nunito(22, weight: .bold))

    if isConfirming {
        Text("Please enter your PIN again")
            .font(.nunito(14, weight: .regular))
            .foregroundColor(.secondary)
    }
}
.padding(.top, 40)
```

**Lines 42-52** (PIN dots):
```swift
// NEW:
HStack(spacing: 20) {
    ForEach(0..<pinLength, id: \.self) { index in
        Circle()
            .fill(index < currentPinLength ?
                  Color.smilePileYellow :
                  Color.gray.opacity(0.2))
            .frame(width: 20, height: 20)
    }
}
```

**Lines 224-236** (Number button):
```swift
struct NumberButton: View {
    let number: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(.nunito(24, weight: .medium))
                .foregroundColor(.primary)
                .frame(width: 70, height: 70)
                .background(
                    Circle()
                        .fill(Color.gray.opacity(0.1))
                )
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Lock icon is yellow (64pt)
2. ✅ PIN dots fill with yellow
3. ✅ Number pad uses Nunito font
4. ✅ Buttons are 70pt circles
5. ✅ Spacing matches Android

### Evidence Required:
1. Screenshot of PIN screen
2. Yellow lock icon visible
3. Number pad layout correct

### Peer Review Checklist:
- [ ] Yellow color for icon and dots
- [ ] 64pt lock icon size
- [ ] Number buttons 70pt
- [ ] Gray 10% backgrounds
- [ ] Font weights correct

---

## CHUNK-016: Update PIN Setup Screen - Buttons Layout

**Estimated Time**: 60 minutes
**Dependencies**: CHUNK-015
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/PINSetupScreen.swift`

### Code Changes:

**Lines 110-144** (Action buttons):
```swift
// NEW: Side-by-side buttons
HStack(spacing: 12) {
    if !isConfirming {
        // Skip button
        Button(action: {
            coordinator.onboardingData.skipPIN = true
            coordinator.navigateToNext()
        }) {
            Text("Skip")
                .font(.nunito(18, weight: .medium))
                .foregroundColor(.primary)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                )
        }
    }

    // Set/Confirm PIN button
    if pinCode.count == pinLength {
        Button(action: {
            if isConfirming {
                confirmPin()
            } else {
                proceedToConfirm()
            }
        }) {
            Text(isConfirming ? "Confirm PIN" : "Set PIN")
                .font(.nunito(18, weight: .bold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.smilePileBlue)
                .cornerRadius(12)
        }
    }
}
.padding(.horizontal, 40)
.padding(.bottom, 20)
```

### Acceptance Criteria:
1. ✅ Buttons side-by-side when both shown
2. ✅ Skip is outlined button
3. ✅ Set PIN is filled blue
4. ✅ Both 56pt height
5. ✅ Proper spacing

### Evidence Required:
1. Screenshot showing both buttons
2. Outline vs filled styles
3. Side-by-side layout

### Peer Review Checklist:
- [ ] Horizontal layout for buttons
- [ ] Skip button outlined only
- [ ] Blue filled for Set PIN
- [ ] Equal widths when both shown
- [ ] 56pt height maintained

---

## CHUNK-017: Update Completion Screen - Checkmark Animation

**Estimated Time**: 90 minutes
**Dependencies**: CHUNK-001, CHUNK-003
**Risk Level**: Medium

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CompletionScreen.swift`

### Complete Rewrite:
```swift
import SwiftUI

struct CompletionScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var showCheckmark = false
    @State private var showContent = false

    var body: some View {
        VStack(spacing: 40) {
            Spacer()

            // Success animation
            if showCheckmark {
                ZStack {
                    Circle()
                        .fill(Color.smilePileGreen.opacity(0.1))
                        .frame(width: 120, height: 120)

                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 80))
                        .foregroundColor(.smilePileGreen)
                }
                .scaleEffect(showCheckmark ? 1 : 0.5)
                .animation(.spring(response: 0.5, dampingFraction: 0.6), value: showCheckmark)
            }

            // Text content
            if showContent {
                VStack(spacing: 12) {
                    Text("All Set!")
                        .font(.nunito(32, weight: .bold))

                    Text("SmilePile is ready to use")
                        .font(.nunito(18, weight: .regular))
                        .foregroundColor(.secondary)
                }
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            // Summary card
            if showContent {
                VStack(alignment: .leading, spacing: 16) {
                    // Piles created
                    if !coordinator.onboardingData.categories.isEmpty {
                        HStack(spacing: 12) {
                            Image(systemName: "square.stack")
                                .foregroundColor(.smilePileOrange)

                            Text("\(coordinator.onboardingData.categories.count) piles created")
                                .font(.nunito(16, weight: .regular))
                        }
                    }

                    // PIN enabled
                    if !coordinator.onboardingData.skipPIN {
                        HStack(spacing: 12) {
                            Image(systemName: "lock.fill")
                                .foregroundColor(.smilePileBlue)

                            Text("PIN protection enabled")
                                .font(.nunito(16, weight: .regular))
                        }
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.1))
                )
                .padding(.horizontal, 40)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            Spacer()

            // Start button
            if showContent {
                Button(action: {
                    // Will trigger dismissal
                }) {
                    Text("Start Using SmilePile")
                        .font(.nunito(18, weight: .medium))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.smilePileBlue)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 50)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .onAppear {
            // Animate in sequence
            withAnimation(.default.delay(0.1)) {
                showCheckmark = true
            }
            withAnimation(.default.delay(0.6)) {
                showContent = true
            }
        }
    }
}
```

### Acceptance Criteria:
1. ✅ Green checkmark with spring animation
2. ✅ 120pt outer circle, 80pt icon
3. ✅ Content animates after checkmark
4. ✅ Summary shows piles count
5. ✅ "Piles" not "categories"

### Evidence Required:
1. Video of animation sequence
2. Summary card showing correctly
3. Spring animation visible

### Peer Review Checklist:
- [ ] Spring animation (damping 0.6)
- [ ] 100ms initial delay
- [ ] 500ms delay for content
- [ ] Green colors used
- [ ] "Piles created" text

---

## CHUNK-018: Update Main App Navigation Labels

**Estimated Time**: 45 minutes
**Dependencies**: None
**Risk Level**: Low

### Files to Search and Modify:
- All files containing "Category" or "Categories" in navigation/UI

### Search and Replace Tasks:
1. Find all navigation labels with "Categories"
2. Update to "Piles"
3. Ensure consistency across app

### Code to Search For:
```bash
# Search for UI text containing Category
grep -r "\"Category" --include="*.swift" ios/
grep -r "\"Categories" --include="*.swift" ios/
```

### Expected Changes:
- Navigation tab labels
- Screen titles
- Button labels
- Menu items

### Acceptance Criteria:
1. ✅ All navigation shows "Piles"
2. ✅ No "Category" in UI text
3. ✅ Code variables unchanged
4. ✅ Search finds no UI instances
5. ✅ App navigation consistent

### Evidence Required:
1. Search results showing no matches
2. Screenshots of updated navigation
3. List of files changed

### Peer Review Checklist:
- [ ] Complete search performed
- [ ] All UI text updated
- [ ] Variables remain unchanged
- [ ] No mixed terminology
- [ ] Consistent throughout app

---

## CHUNK-019: Fix Color Inconsistencies

**Estimated Time**: 60 minutes
**Dependencies**: CHUNK-001
**Risk Level**: Medium

### Files to Modify:
- All onboarding screens using hardcoded colors

### Task:
Replace all Color(red: 1.0, green: 0.42, blue: 0.42) with proper constants

### Search and Replace:
```swift
// OLD:
Color(red: 1.0, green: 0.42, blue: 0.42)

// NEW:
Color.smilePilePink  // Or appropriate color from constants
```

### Files to Check:
- OnboardingView.swift
- All screen files
- Any component files

### Acceptance Criteria:
1. ✅ No hardcoded RGB values
2. ✅ All colors from constants
3. ✅ Consistent color usage
4. ✅ Build succeeds
5. ✅ Colors match Android

### Evidence Required:
1. Search showing no RGB colors
2. All colors using constants
3. Visual consistency check

### Peer Review Checklist:
- [ ] No Color(red:green:blue:)
- [ ] All colors from ColorConstants
- [ ] Pink used appropriately
- [ ] Blue for buttons
- [ ] Yellow for accents

---

## CHUNK-020: Update Color Picker Palette

**Estimated Time**: 30 minutes
**Dependencies**: CHUNK-001
**Risk Level**: Low

### Files to Modify:
- `/Users/adamstack/SmilePile/ios/SmilePile/Onboarding/Screens/CategorySetupScreen.swift`

### Code Changes:

**Lines 19-24** (Color palette):
```swift
// Ensure these match Android exactly:
let colorOptions = [
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
    "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
    "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
]
```

**Lines 99-114** (Color picker display):
```swift
// Update to show selected with check icon:
Circle()
    .fill(Color(hex: color))
    .frame(width: 44, height: 44)
    .overlay(
        ZStack {
            if selectedColor == color {
                Circle()
                    .stroke(Color.smilePileBlue, lineWidth: 3)
                Image(systemName: "checkmark")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }
        }
    )
```

### Acceptance Criteria:
1. ✅ 12 colors matching Android
2. ✅ 44pt circles
3. ✅ Blue border on selected
4. ✅ Check icon on selected
5. ✅ Scrollable row layout

### Evidence Required:
1. Screenshot of color picker
2. Selected state with check
3. All 12 colors visible

### Peer Review Checklist:
- [ ] Exact hex values from Android
- [ ] 44pt circle size
- [ ] 3pt blue border
- [ ] White checkmark
- [ ] Horizontal scroll

---

## CHUNK-021: Add Shadow Effects to Logo

**Estimated Time**: 30 minutes
**Dependencies**: CHUNK-004
**Risk Level**: Low

### Files to Modify:
- Already handled in CHUNK-004, verify implementation

### Verification Tasks:
1. Confirm shadow parameters match Android
2. Test on different backgrounds
3. Ensure performance is good

### Shadow Parameters:
- Color: Black 90% opacity
- X offset: 4pt
- Y offset: 4pt
- Blur radius: 6pt

### Acceptance Criteria:
1. ✅ Shadow visible on logo
2. ✅ Parameters match exactly
3. ✅ No performance issues
4. ✅ Looks good on all backgrounds
5. ✅ Can toggle shadow

### Evidence Required:
1. Screenshot with shadow
2. Code showing parameters
3. Performance metrics

### Peer Review Checklist:
- [ ] Black 90% opacity
- [ ] 4pt offset both axes
- [ ] 6pt blur radius
- [ ] Toggle parameter works
- [ ] No lag or flicker

---

## CHUNK-022: Final Button and Layout Adjustments

**Estimated Time**: 60 minutes
**Dependencies**: Previous chunks
**Risk Level**: Low

### Files to Review:
- All onboarding screens

### Tasks:
1. Verify all button heights (56pt primary, 44pt secondary)
2. Check padding consistency (40pt horizontal)
3. Ensure corner radius correct (12pt buttons, 8pt cards)
4. Verify font sizes match spec

### Checklist:
- [ ] Get Started button: 56pt height
- [ ] Continue button: 56pt height
- [ ] Skip button: 56pt height
- [ ] Add Pile button: 44pt height
- [ ] All primary buttons blue
- [ ] All buttons use Nunito Bold 18pt

### Acceptance Criteria:
1. ✅ All measurements match spec
2. ✅ Consistent padding
3. ✅ Proper corner radius
4. ✅ Font sizes correct
5. ✅ Visual harmony

### Evidence Required:
1. Measurement screenshots
2. Side-by-side with Android
3. Consistency across screens

### Peer Review Checklist:
- [ ] 56pt primary buttons
- [ ] 44pt secondary buttons
- [ ] 40pt horizontal padding
- [ ] 12pt/8pt corner radius
- [ ] Nunito fonts throughout

---

## CHUNK-023: End-to-End Flow Testing

**Estimated Time**: 90 minutes
**Dependencies**: All previous chunks
**Risk Level**: High (integration test)

### Test Scenarios:

1. **Fresh Install Flow**:
   - Launch app
   - See Welcome screen
   - Tap Get Started
   - Create 3 piles
   - Set PIN
   - See completion
   - Launch main app

2. **Skip Scenarios**:
   - Skip PIN setup
   - Verify completion shows correctly
   - Ensure app launches

3. **Validation Tests**:
   - Try to continue without piles
   - Enter mismatched PINs
   - Create 5 piles (max)
   - Try to create 6th pile

4. **Data Persistence**:
   - Complete onboarding
   - Kill app
   - Relaunch
   - Verify no onboarding
   - Check piles exist

### Acceptance Criteria:
1. ✅ All flows work correctly
2. ✅ Validation prevents errors
3. ✅ Data persists properly
4. ✅ No crashes or hangs
5. ✅ Smooth transitions

### Evidence Required:
1. Video of complete flow
2. Test results document
3. Data persistence proof

### Peer Review Checklist:
- [ ] Welcome → Piles → PIN → Complete
- [ ] Skip PIN works
- [ ] Validation works
- [ ] Data saves correctly
- [ ] No onboarding on relaunch

---

## CHUNK-024: Final Polish and Cleanup

**Estimated Time**: 60 minutes
**Dependencies**: CHUNK-023
**Risk Level**: Low

### Tasks:
1. Remove any debug code
2. Clean up comments
3. Verify all TODOs addressed
4. Run linter/formatter
5. Final build verification

### Final Checklist:
- [ ] No console.log/print statements
- [ ] No commented-out code
- [ ] No TODO comments
- [ ] Code properly formatted
- [ ] All files saved
- [ ] Git status clean
- [ ] Build succeeds
- [ ] No warnings

### Acceptance Criteria:
1. ✅ Clean codebase
2. ✅ No debug artifacts
3. ✅ Professional code quality
4. ✅ Build without warnings
5. ✅ Ready for deployment

### Evidence Required:
1. Clean build output
2. No warnings in Xcode
3. Git diff showing cleanup

### Peer Review Checklist:
- [ ] No debug code
- [ ] Clean formatting
- [ ] No unused imports
- [ ] Comments appropriate
- [ ] Ready for production

---

## Testing Strategy

### Unit Testing
Each chunk should be tested individually before proceeding to the next. Key test points:
- Color values match exactly
- Font weights correct
- Button dimensions precise
- Navigation flow correct
- Data persistence working

### Integration Testing
After chunks are complete:
1. Full flow walkthrough
2. Edge case testing
3. Performance testing
4. Memory leak checks
5. Device compatibility

### Visual Testing
Side-by-side comparison with Android for:
- Colors
- Typography
- Spacing
- Animations
- Icons

### Regression Testing
Ensure existing functionality still works:
- Main app navigation
- Photo management
- Category features
- Settings

## Rollback Strategy

### Immediate Rollback
If critical issues found:
1. Git stash all changes
2. Revert to last known good commit
3. Cherry-pick successful chunks only
4. Re-test thoroughly

### Partial Rollback
For specific chunk failures:
1. Identify problematic chunk
2. Revert only those files
3. Re-implement with fixes
4. Continue with plan

### Full Rollback Process
```bash
# Save current work
git stash save "iOS onboarding implementation WIP"

# Revert to backup branch
git checkout main
git reset --hard backup/pre-onboarding-update

# Or revert specific commits
git revert <commit-hash>
```

### Recovery Checkpoints
Create git tags after major milestones:
- After foundation chunks (CHUNK-005)
- After navigation changes (CHUNK-008)
- After terminology updates (CHUNK-013)
- After visual updates (CHUNK-020)
- Before final testing (CHUNK-023)

## Success Metrics

### Mandatory Requirements Met
- ✅ 4-screen flow (no PhotoImport)
- ✅ "Piles" terminology throughout
- ✅ Exact color matching
- ✅ Multicolor logo with shadow
- ✅ No progress bar
- ✅ Nunito fonts
- ✅ 56pt button heights
- ✅ Correct icon usage

### Quality Metrics
- Zero build warnings
- No console errors
- Smooth animations (60 fps)
- Fast screen transitions (<200ms)
- Memory usage stable
- No layout glitches

### User Experience
- Intuitive flow
- Clear instructions
- Proper validation
- Smooth animations
- Professional appearance
- Consistent with Android

## Notes for Developers

### Critical Points
1. **Never** show "Categories" to users - always "Piles"
2. **Keep** PhotoImportScreen.swift file (don't delete)
3. **Match** Android exactly - no "improvements"
4. **Test** each chunk before proceeding
5. **Verify** colors with hex values, not visually

### Common Pitfalls
- Forgetting to update navigation labels
- Missing color constant usage
- Incorrect button heights
- Wrong font weights
- Skipping validation tests

### Resources
- Android spec: `/Users/adamstack/SmilePile/ios/ANDROID_ONBOARDING_SPEC.md`
- Color values: See CHUNK-001
- Font weights: See CHUNK-003
- Icon mappings: See CHUNK-002

This plan ensures systematic, testable implementation with minimal risk and maximum quality assurance.