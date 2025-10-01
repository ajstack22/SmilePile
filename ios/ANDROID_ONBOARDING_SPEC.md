# Android Onboarding Implementation - Complete Specification

## Executive Summary

Android has implemented a refined onboarding wizard with 4 main screens (Welcome → Categories → PIN Setup → Completion). The Photo Import step was removed from the flow. The app now uses "Piles" terminology instead of "Categories" in all user-facing text. The implementation uses a ViewModel-based architecture with Material3 Compose UI components.

## Complete Flow Documentation

### 1. FLOW STRUCTURE

#### Screen Sequence
1. **Welcome Screen** - App introduction with feature highlights
2. **Category Setup Screen** - Create colorful "Piles" for organizing photos
3. **PIN Setup Screen** - Optional security setup (skippable)
4. **Completion Screen** - Success confirmation with summary

#### Removed Features
- **Photo Import Screen** - REMOVED from onboarding flow (still exists in code but not used)
- **Progress Bar** - REMOVED from the onboarding flow

### 2. WELCOME SCREEN

#### Visual Elements
- **Logo**: SmilePile logo icon (100dp size) at top center
- **App Name**: "SmilePile" in multicolored text with shadow effect
  - "Smile" - Yellow (#FFBF00)
  - "P" - Green (#4CAF50)
  - "i" - Blue (#2196F3)
  - "l" - Orange (#FF6600)
  - "e" - Pink (#E86082)
  - Font: Nunito ExtraBold from Google Fonts
  - Size: 36sp
  - Shadow: Black 90% opacity, offset (4f, 4f), blur 6f

#### Text Content
- **Tagline**: "A safe and fun photo gallery for EVERYONE" (18sp, centered)

#### Feature List (3 items with icons)
1. **Icon**: Icons.Outlined.Layers (Yellow #FFBF00)
   - **Title**: "Organize photos into piles"
   - **Description**: "Create colorful piles for your photos"

2. **Icon**: Icons.Default.FitScreen (Orange #FF6600)
   - **Title**: "Distraction-free mode"
   - **Description**: "Good for kids (and everyone else)"

3. **Icon**: Icons.Default.Lock (Green #4CAF50)
   - **Title**: "Optional PIN protection"
   - **Description**: "Prevent inadvertent changes"

#### Button
- **Label**: "Get Started"
- **Color**: Blue (#2196F3)
- **Size**: Full width, 56dp height
- **Font**: 18sp, Bold

### 3. CATEGORY SETUP SCREEN (Called "Piles" in UI)

#### Header
- **Title**: "Create Piles" (in top app bar)
- **Back Button**: Arrow back icon
- **Icon**: Layers icon (48dp, Orange #FF6600) centered at top
- **Instruction Text**: "Organize your photos into colorful piles" (14sp, centered)

#### Your Piles Section (when categories exist)
- **Section Title**: "Your Piles" (18sp, SemiBold)
- **Counter**: "{count}/5" on right side (14sp)
- **Category Cards**:
  - Background: Category color at 10% opacity
  - Color dot: 16dp circle with category color
  - Name text: 16sp
  - Remove button: Cancel icon (24dp)

#### Create Your Own Section
- **Title**: "Create Your Own" (16sp, Medium)
- **Input Field**:
  - Label: "Custom pile name"
  - Add button: Plus icon (Blue #2196F3 when enabled)
- **Color Picker**:
  - Horizontal scrollable row
  - 44dp circles
  - 12 color options: #FF6B6B, #4ECDC4, #45B7D1, #96CEB4, #FFEAA7, #DDA0DD, #FFA07A, #98D8C8, #F7DC6F, #BB8FCE, #85C1E2, #F8B739
  - Selected: 3dp blue border with check icon

#### Quick Add Section
- **Title**: "Or Quick Add" (16sp, Medium)
- **Preset Categories**:
  - "Family" - #FF6B6B (Red)
  - "Friends" - #4ECDC4 (Teal)
  - "Fun" - #FFEAA7 (Yellow)
- **Cards**: 56dp height, color at 15% opacity, plus icon

#### Continue Button
- **Label**: "Continue"
- **Color**: Blue (#2196F3)
- **Size**: Full width, 56dp height
- **Font**: 18sp, Bold
- **Disabled State**: When no categories created
- **Helper Text**: "Add at least one pile to continue" (12sp, centered)

### 4. PIN SETUP SCREEN

#### Visual Elements
- **Title**: "PIN Setup" (in top app bar)
- **Icon**: Lock icon (64dp, Yellow #FFBF00)
- **Main Title**:
  - Initial: "Set Up PIN Protection" (22sp, Bold)
  - Confirming: "Confirm Your PIN" (22sp, Bold)
- **Subtitle** (only when confirming): "Please enter your PIN again" (14sp)

#### PIN Display
- **4 dots** in a row (20dp spacing)
- **Empty**: Gray 20% opacity circles
- **Filled**: Yellow (#FFBF00) circles
- **Size**: 20dp each

#### Number Pad
- **Layout**: 3x3 grid + bottom row
- **Number Buttons**:
  - 70dp circles
  - Gray 10% opacity background
  - Numbers 1-9, 0 in bottom center
  - Font: 24sp, Medium
- **Backspace Button**:
  - Left bottom position
  - Backspace icon (28dp, Gray)
- **Spacing**: 24dp horizontal, 16dp vertical

#### Action Buttons (Side by side)
- **Skip Button** (only on initial entry):
  - Type: OutlinedButton
  - Label: "Skip" (18sp, Medium)
  - Width: 50% of screen (minus padding)
  - Height: 56dp

- **Set PIN Button**:
  - Label: "Set PIN" / "Confirm PIN" (18sp, Bold)
  - Color: Blue (#2196F3)
  - Width: 50% (or full if confirming)
  - Height: 56dp
  - Disabled: Until 4 digits entered

#### Error State
- **Message**: "PINs don't match. Please try again." (12sp, red)
- **Behavior**: Clear confirmation PIN, show error

### 5. COMPLETION SCREEN

#### Success Animation
- **Checkmark Circle**:
  - 120dp outer circle (Green 10% opacity)
  - CheckCircle icon (80dp, Green #4CAF50)
  - Spring animation (dampingRatio: 0.6f)
  - 100ms delay before showing

#### Text Content
- **Title**: "All Set!" (32sp, Bold)
- **Subtitle**: "SmilePile is ready to use" (18sp)
- **Animation**: Fade in + slide in vertically (500ms after checkmark)

#### Summary Card
- **Background**: SurfaceVariant at 50% opacity
- **Rounded Corners**: 12dp
- **Padding**: 20dp
- **Content** (conditional based on setup):
  - **Piles Created**: Layers icon (Orange #FF6600) + "{count} piles created"
  - **Photos Imported**: Photo icon (Teal #4ECDC4) + "{count} photos imported"
  - **PIN Enabled**: Lock icon (Blue #45B7D1) + "PIN protection enabled"

#### Start Button
- **Label**: "Start Using SmilePile"
- **Color**: Blue (#2196F3)
- **Size**: Full width, 56dp height
- **Font**: 18sp, Medium
- **Animation**: Fade in + slide in from bottom

### 6. NAVIGATION & TRANSITIONS

#### Top App Bar (Screens 2-3)
- **Height**: Standard Material3
- **Background**: Transparent
- **Title**: Screen-specific (see each screen)
- **Back Button**: Standard arrow back icon

#### Screen Transitions
- **Forward**: slideInHorizontally from right + fadeIn
- **Backward**: slideInHorizontally from left + fadeIn
- **Duration**: Material3 defaults
- **Label**: "onboarding_content"

#### Back Navigation
- **Available on**: Categories, PIN Setup screens
- **Not available on**: Welcome, Completion screens
- **Hardware back**: Follows same rules

### 7. STATE MANAGEMENT

#### OnboardingViewModel States
```kotlin
enum class OnboardingStep {
    WELCOME,
    CATEGORIES,  // "Piles" in UI
    PIN_SETUP,
    COMPLETE
}

data class OnboardingUiState(
    currentStep: OnboardingStep = WELCOME,
    navigationHistory: List<OnboardingStep> = [],
    categories: List<TempCategory> = [],
    importedPhotos: List<ImportedPhotoData> = [], // Not used
    pinCode: String? = null,
    skipPin: Boolean = false,
    isLoading: Boolean = false,
    error: String? = null
)
```

#### Validation Rules
- **Categories Screen**: Must have at least 1 category to continue
- **PIN Screen**: Must enter 4 digits or explicitly skip
- **Category Limit**: Maximum 5 categories
- **Category Names**: Must be unique (case-insensitive)

### 8. DATA PERSISTENCE

#### On Completion
1. **Categories saved to database**:
   - name: lowercase with underscores
   - displayName: as entered by user
   - position: based on creation order
   - colorHex: selected color
   - isDefault: false
   - createdAt: current timestamp

2. **PIN saved to SecurePreferencesManager** (if set)

3. **Settings updated**:
   - onboardingCompleted: true
   - firstLaunch: false (if applicable)

#### Triggering Conditions
- App launches with no onboardingCompleted flag
- No existing categories in database
- User selects "Reset App" from settings

### 9. LOADING & ERROR STATES

#### Loading Overlay
- **Background**: Black 50% opacity
- **Indicator**: Circular progress (centered)
- **Blocks**: All user interaction

#### Error Dialog
- **Title**: "Error"
- **Message**: Context-specific error text
- **Button**: "OK" to dismiss
- **Behavior**: Non-dismissible backdrop

### 10. TERMINOLOGY CHANGES

#### Global Replacements
- "Category/Categories" → "Pile/Piles" in ALL user-facing text
- "Folder" icons → "Layers" icons throughout
- Navigation labels updated to "Piles"
- FAB icons changed to simple Plus symbols

#### Code vs UI Terminology
- **Code**: Still uses "category" in variable names, database
- **UI**: Always displays "pile/piles" to users
- **Comment in ViewModel**: "UI Note for LLM Developers: 'Pile' in user-facing text = 'Category' in code/database"

## iOS Research Directives

### 1. Current iOS Onboarding Analysis

**Topic name**: Existing iOS Onboarding Implementation
**Why it matters**: Need to understand what exists vs what needs changing
**Key questions**:
- Does iOS currently have all 5 screens or just 4?
- Is PhotoImport screen being used in the flow?
- What terminology is currently used (Categories vs Piles)?
- Are the screen transitions matching Android's horizontal slide?

**Files to examine**:
- /ios/SmilePile/Onboarding/OnboardingCoordinator.swift
- /ios/SmilePile/Onboarding/OnboardingView.swift
- All files in /ios/SmilePile/Onboarding/Screens/

**Success criteria**: Complete mapping of current iOS implementation vs Android spec

### 2. Terminology Replacement Strategy

**Topic name**: Categories to Piles Terminology Migration
**Why it matters**: User-facing text must say "Piles" while code can remain "categories"
**Key questions**:
- Where are all the user-facing strings defined?
- Are there Localizable.strings files?
- Which SwiftUI Text views need updating?
- Are there any hardcoded "Category" strings in the UI?

**Files to examine**:
- Search for "Category" and "Categories" in all .swift files
- Check for Localizable.strings or similar
- Navigation labels and titles

**Success criteria**: Complete list of all text changes needed

### 3. Color System Alignment

**Topic name**: SmilePile Brand Colors Implementation
**Why it matters**: Colors must match Android exactly for brand consistency
**Key questions**:
- How are colors currently defined in iOS?
- Is there a central Color extension or theme?
- Are the exact hex values matching Android?

**Files to examine**:
- Search for Color definitions
- Check Assets.xcassets for color sets
- Look for theme or styling files

**Success criteria**: Color definitions matching all Android values

### 4. Typography and Shadows

**Topic name**: Nunito Font and Shadow Effects
**Why it matters**: The multicolored "SmilePile" text with shadows is a key brand element
**Key questions**:
- Is Nunito font available in iOS project?
- How to implement the colored character text?
- How to add shadow effects in SwiftUI?

**Files to examine**:
- Info.plist for font configurations
- Current WelcomeScreen.swift implementation
- Font loading mechanisms

**Success criteria**: Exact reproduction of Android's styled text

### 5. Navigation Flow Control

**Topic name**: Screen Sequencing and Skip Logic
**Why it matters**: Must remove PhotoImport from flow while keeping code
**Key questions**:
- How is navigation currently controlled?
- Where to modify the flow sequence?
- How to handle the skip functionality?

**Files to examine**:
- OnboardingCoordinator.swift navigation methods
- State management for current step
- Skip button implementations

**Success criteria**: Flow matches Android exactly (Welcome → Categories → PIN → Complete)

### 6. Icon System

**Topic name**: SF Symbols vs Material Icons Mapping
**Why it matters**: Icons must be visually equivalent across platforms
**Key questions**:
- What SF Symbols match the Material icons used?
- Specifically: Layers, FitScreen, Lock, PhotoLibrary, etc.
- Are custom icons needed?

**Files to examine**:
- Current icon usage in onboarding screens
- Check for custom icon assets

**Success criteria**: Visually equivalent icons identified for all uses

### 7. Animation Framework

**Topic name**: SwiftUI Animation Capabilities
**Why it matters**: Need to match Android's spring animations and transitions
**Key questions**:
- How to implement spring animations in SwiftUI?
- How to do slide + fade transitions?
- How to sequence animations with delays?

**Files to examine**:
- Current animation usage in onboarding
- CompletionScreen.swift animations

**Success criteria**: Animations match Android timing and style

### 8. PIN Input Implementation

**Topic name**: Custom Number Pad and PIN Dots
**Why it matters**: PIN screen has specific layout requirements
**Key questions**:
- How is the current PIN input implemented?
- Is there a custom number pad?
- How are the PIN dots displayed?

**Files to examine**:
- PINSetupScreen.swift
- Any PIN-related components

**Success criteria**: PIN input matches Android's visual design exactly

### 9. Data Persistence Layer

**Topic name**: Core Data and UserDefaults Integration
**Why it matters**: Must save onboarding data correctly
**Key questions**:
- How are categories saved to Core Data?
- Where is onboarding completion tracked?
- How is PIN stored securely?

**Files to examine**:
- CoreDataStack.swift
- CategoryRepository.swift
- PINManager or similar
- UserDefaults keys

**Success criteria**: Data saved in same format as Android

### 10. Progress Indicator Removal

**Topic name**: Progress Bar Elimination
**Why it matters**: Android removed the progress bar from onboarding
**Key questions**:
- Is iOS currently showing a progress bar?
- Where is it implemented?
- What needs to be removed?

**Files to examine**:
- OnboardingView.swift
- Any progress indicator components

**Success criteria**: No progress indicators in onboarding flow

## Android File Inventory

### Core Onboarding Files
```
/android/app/src/main/java/com/smilepile/onboarding/
├── OnboardingActivity.kt         - Main activity container
├── OnboardingScreen.kt           - Navigation and layout orchestrator
├── OnboardingViewModel.kt        - State management and business logic
└── screens/
    ├── WelcomeScreen.kt          - Introduction screen
    ├── CategorySetupScreen.kt    - Pile creation screen
    ├── PinSetupScreen.kt         - Security setup screen
    ├── CompletionScreen.kt       - Success confirmation screen
    └── PhotoImportScreen.kt      - UNUSED but kept in codebase
```

### Supporting Files
```
/android/app/src/main/java/com/smilepile/
├── MainActivity.kt                - Onboarding launch logic (lines 84-99, 223-240)
├── settings/SettingsManager.kt    - Onboarding completion tracking
├── security/SecurePreferencesManager.kt - PIN storage
├── data/repository/CategoryRepository.kt - Category persistence
└── ui/theme/ComposeTheme.kt      - Color definitions

/android/app/src/main/res/
├── values/strings.xml            - "Piles" terminology strings
└── drawable/ic_smilepile_logo   - Logo asset
```

### Manifest Entry
```
/android/app/src/main/AndroidManifest.xml
- OnboardingActivity declaration (lines 48-53)
```

## Critical Parity Requirements

### MUST HAVE - Non-Negotiable

1. **Terminology**: ALL user-facing text must say "Piles" not "Categories"
2. **Screen Flow**: Welcome → Categories → PIN → Complete (NO photo import)
3. **Colors**: Exact hex values for brand colors (see specifications above)
4. **Skip Logic**: Only PIN screen can be skipped
5. **Validation**: Cannot proceed from Categories without at least 1 pile
6. **Category Limit**: Maximum 5 piles
7. **No Progress Bar**: Removed from the flow entirely
8. **Multicolor Logo Text**: "SmilePile" with exact colors and shadows
9. **Icon Changes**: Layers icon for Piles, Plus icon for FABs
10. **Button Sizes**: 56dp height for primary actions

### Visual Consistency Requirements

1. **Spacing**: Match Android's padding and margins exactly
2. **Font Sizes**: Use equivalent iOS sizes (sp → points conversion)
3. **Corner Radius**: 8dp for cards, 12dp for completion summary
4. **Animations**: Spring for checkmark, slide+fade for screens
5. **Color Opacity**: Match all alpha values (10%, 15%, 50%, etc.)

### Behavioral Requirements

1. **Back Navigation**: Only on Categories and PIN screens
2. **Error Messages**: Show as alerts, not inline
3. **Loading State**: Full-screen overlay with spinner
4. **PIN Confirmation**: Two-step process with error on mismatch
5. **Auto-proceed**: After PIN confirmation, auto-navigate to complete

### Data Requirements

1. **Category Storage**: Lowercase names with underscores in database
2. **Display Names**: Preserve user's original capitalization
3. **Settings Flags**: Mark onboardingCompleted and firstLaunch
4. **PIN Security**: Store in secure storage, not UserDefaults
5. **Timestamps**: Use milliseconds since epoch for consistency

## Testing Checklist

- [ ] Fresh install shows onboarding
- [ ] "Piles" terminology throughout (no "Categories" visible)
- [ ] Can create 1-5 custom piles with colors
- [ ] Cannot proceed without at least 1 pile
- [ ] Quick add suggestions work and disappear when used
- [ ] PIN can be skipped
- [ ] PIN requires 4 digits exactly
- [ ] PIN confirmation shows error on mismatch
- [ ] Completion screen shows correct summary
- [ ] Data persists after app restart
- [ ] Onboarding doesn't show again after completion
- [ ] All colors match Android exactly
- [ ] All animations match Android timing
- [ ] Back button only on appropriate screens
- [ ] No progress bar visible anywhere

## Implementation Priority

1. **FIRST**: Update all "Category" → "Pile" terminology
2. **SECOND**: Remove PhotoImport from the flow
3. **THIRD**: Update colors to exact brand values
4. **FOURTH**: Implement multicolor logo text with shadows
5. **FIFTH**: Refine animations and transitions
6. **SIXTH**: Verify data persistence matches