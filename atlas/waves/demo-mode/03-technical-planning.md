# Phase 3: Technical Implementation Plan
## Demo Mode Feature - SmilePile

**Date**: 2025-10-17
**Phase**: Planning (Atlas Phase 3)
**Status**: DRAFT
**Prepared By**: Developer Agent

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [File Structure](#2-file-structure)
3. [Data Model Design](#3-data-model-design)
4. [Implementation Steps](#4-implementation-steps)
5. [Demo Asset Strategy](#5-demo-asset-strategy)
6. [Testing Strategy](#6-testing-strategy)
7. [Migration & Rollback Plan](#7-migration--rollback-plan)
8. [Edge Cases & Error Handling](#8-edge-cases--error-handling)
9. [Performance Considerations](#9-performance-considerations)
10. [Platform Parity Checklist](#10-platform-parity-checklist)

---

## 1. Architecture Overview

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        App Launch                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Check Onboarding    │
              │  Completion Status   │
              └──────────┬───────────┘
                         │
         ┌───────────────┴────────────────┐
         │                                │
    Not Complete                    Completed
         │                                │
         ▼                                ▼
┌─────────────────┐            ┌──────────────────┐
│ WelcomeScreen   │            │   Main Gallery   │
│                 │            │                  │
│ [Get Started]   │            │ ┌──────────────┐ │
│                 │            │ │   Settings   │ │
│ [Try Demo]  ◄───┼────────────┼─┤ "Try Demo"   │ │
│                 │            │ └──────────────┘ │
└────────┬────────┘            └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│         Demo Mode Entry Point           │
│                                         │
│  1. Set isDemoMode = true               │
│  2. Load demo profile (Jamie Anderson) │
│  3. Pre-populate demo data              │
│  4. Skip standard onboarding            │
└────────────────┬────────────────────────┘
                 │
                 ▼
    ┌────────────────────────────┐
    │   Demo Mode Gallery View   │
    │                            │
    │ ┌────────────────────────┐ │
    │ │  Demo Mode Banner      │ │
    │ │  "Viewing Jamie's..."  │ │
    │ │         [Exit Demo]    │ │
    │ └────────────────────────┘ │
    │                            │
    │  Jamie Anderson Profile    │
    │  75-100 Photos             │
    │  8 Categories              │
    │  All Features Enabled      │
    │  (Read-Only Mode)          │
    └────────────┬───────────────┘
                 │
                 ▼
       ┌─────────────────────┐
       │   Exit Demo Flow    │
       │                     │
       │  1. Confirmation    │
       │  2. Clear Demo Data │
       │  3. Reset isDemoMode│
       │  4. Go to Onboarding│
       └─────────────────────┘
```

### 1.2 Component Interactions

```
┌──────────────────────┐
│  SettingsManager     │  ◄──── Stores isDemoMode flag
│  (iOS)               │
│  PreferencesManager  │
│  (Android)           │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  DemoModeManager     │  ◄──── Orchestrates demo mode
│                      │        - Entry/Exit logic
│  - isDemoMode        │        - Data initialization
│  - enterDemoMode()   │        - State management
│  - exitDemoMode()    │
│  - loadDemoData()    │
└──────────┬───────────┘
           │
           ├──────────────────┐
           │                  │
           ▼                  ▼
┌──────────────────┐  ┌──────────────────────┐
│ PhotoRepository  │  │ CategoryRepository   │
│                  │  │                      │
│ - Demo photos    │  │ - Demo categories    │
│   (isFromAssets: │  │   (8 categories)     │
│    true)         │  │                      │
└──────────────────┘  └──────────────────────┘
```

### 1.3 Data Flow

**Demo Mode Entry:**
```
User Taps "Try Demo"
    │
    ├─→ DemoModeManager.enterDemoMode()
    │       │
    │       ├─→ Set isDemoMode = true
    │       ├─→ Skip onboarding flag
    │       ├─→ Load demo categories (8)
    │       ├─→ Load demo photos (75-100)
    │       └─→ Navigate to gallery
    │
    └─→ Gallery loads with demo data
```

**Demo Mode Exit:**
```
User Taps "Exit Demo"
    │
    ├─→ Show confirmation dialog
    │       │
    │       ├─→ "Start Fresh" selected
    │       │       │
    │       │       ├─→ DemoModeManager.exitDemoMode()
    │       │       │       │
    │       │       │       ├─→ Set isDemoMode = false
    │       │       │       ├─→ Clear demo data flag
    │       │       │       └─→ Reset to onboarding
    │       │       │
    │       │       └─→ Navigate to WelcomeScreen
    │       │
    │       └─→ "Stay in Demo" → Close dialog
    │
    └─→ Continue demo session
```

### 1.4 State Management Approach

**iOS (SwiftUI + Combine):**
- Use `@AppStorage` wrapper for `isDemoMode` flag
- Use `@Published` properties in `DemoModeManager` for reactive updates
- Leverage existing `SettingsManager.shared` pattern
- Use `NotificationCenter` for cross-component communication

**Android (Kotlin + StateFlow):**
- Use `SharedPreferences` for `isDemoMode` flag
- Use `StateFlow` for reactive state management
- Leverage existing `PreferencesManager` pattern
- Use `LiveData` or `StateFlow` for UI updates

---

## 2. File Structure

### 2.1 iOS Files

#### New Files to Create

1. **`/ios/SmilePile/DemoMode/DemoModeManager.swift`**
   - Purpose: Central coordinator for demo mode functionality
   - Responsibilities:
     - Entry/exit logic
     - Demo data initialization
     - State management
     - Demo asset loading

2. **`/ios/SmilePile/DemoMode/DemoData.swift`**
   - Purpose: Demo data definitions
   - Contains:
     - Jamie Anderson profile struct
     - Demo categories array
     - Demo photo metadata array
     - Photo captions and dates

3. **`/ios/SmilePile/DemoMode/DemoModeBanner.swift`**
   - Purpose: Persistent banner UI component
   - Features:
     - "Demo Mode - Viewing Jamie's Photos" text
     - Exit Demo button
     - Soft purple background (#E8E0F5)

4. **`/ios/SmilePile/DemoMode/ExitDemoDialog.swift`**
   - Purpose: Confirmation dialog for exiting demo mode
   - Options:
     - "Start Organizing My Photos" (primary)
     - "Continue Exploring" (secondary)

5. **`/ios/SmilePile/Assets.xcassets/DemoPhotos/`**
   - Purpose: Asset catalog for demo photos
   - Structure:
     - Milestones/ (20 images)
     - Birthdays/ (15 images)
     - Holidays/ (18 images)
     - Family/ (12 images)
     - Playtime/ (10 images)
     - Friends/ (8 images)
     - Creativity/ (8 images)
     - Adventures/ (9 images)

#### Existing Files to Modify

1. **`/ios/SmilePile/Onboarding/Screens/WelcomeScreen.swift`**
   - **Changes:**
     - Add "Try Demo" button below "Get Started" button
     - Add descriptive text: "Explore with Jamie's photos"
     - Add button styling (outlined button style)
     - Add tap handler: `coordinator.enterDemoMode()`

2. **`/ios/SmilePile/Onboarding/OnboardingCoordinator.swift`**
   - **Changes:**
     - Add `isDemoMode` property
     - Add `enterDemoMode()` method
     - Add `exitDemoMode()` method
     - Modify `completeOnboarding()` to skip when in demo mode
     - Add demo data loading logic

3. **`/ios/SmilePile/Settings/SettingsManager.swift`**
   - **Changes:**
     - Add `isDemoMode: Bool` key to Keys struct
     - Add `@AppStorage(Keys.isDemoMode) var isDemoMode: Bool = false`
     - Add demo mode to `resetToDefaults()` method

4. **`/ios/SmilePile/Views/ContentView.swift`**
   - **Changes:**
     - Add conditional rendering for `DemoModeBanner`
     - Pass `isDemoMode` flag to child views
     - Disable edit actions when `isDemoMode == true`

5. **`/ios/SmilePile/Views/SettingsViewCustom.swift`**
   - **Changes:**
     - Add "Try Demo Mode" menu item
     - Only show if user has completed onboarding
     - Tap handler to re-enter demo mode

6. **`/ios/SmilePile/Data/Repositories/PhotoRepositoryImpl.swift`**
   - **Changes:**
     - Add `loadDemoPhotos()` method
     - Filter demo photos when `isDemoMode == true`
     - Prevent modifications when `isFromAssets == true`

7. **`/ios/SmilePile/Data/Repositories/CategoryRepositoryImpl.swift`**
   - **Changes:**
     - Add `loadDemoCategories()` method
     - Return demo categories when `isDemoMode == true`
     - Prevent modifications to demo categories

### 2.2 Android Files

#### New Files to Create

1. **`/android/app/src/main/java/com/smilepile/demomode/DemoModeManager.kt`**
   - Purpose: Central coordinator for demo mode functionality
   - Responsibilities:
     - Entry/exit logic
     - Demo data initialization
     - State management via StateFlow
     - Demo asset loading

2. **`/android/app/src/main/java/com/smilepile/demomode/DemoData.kt`**
   - Purpose: Demo data definitions
   - Contains:
     - Jamie Anderson profile data class
     - Demo categories list
     - Demo photo metadata list
     - Photo captions and dates

3. **`/android/app/src/main/java/com/smilepile/demomode/DemoModeBanner.kt`**
   - Purpose: Persistent banner composable
   - Features:
     - "Demo Mode - Viewing Jamie's Photos" text
     - Exit Demo button
     - Soft purple background (#E8E0F5)

4. **`/android/app/src/main/java/com/smilepile/demomode/ExitDemoDialog.kt`**
   - Purpose: Confirmation dialog composable
   - Options:
     - "Start Organizing My Photos" (primary)
     - "Continue Exploring" (secondary)

5. **`/android/app/src/main/res/drawable-nodpi/demo_photos/`**
   - Purpose: Demo photo assets directory
   - Structure:
     - milestones/ (20 images)
     - birthdays/ (15 images)
     - holidays/ (18 images)
     - family/ (12 images)
     - playtime/ (10 images)
     - friends/ (8 images)
     - creativity/ (8 images)
     - adventures/ (9 images)

#### Existing Files to Modify

1. **`/android/app/src/main/java/com/smilepile/onboarding/screens/WelcomeScreen.kt`**
   - **Changes:**
     - Add "Try Demo" OutlinedButton below "Start Fresh" button
     - Add descriptive text: "Explore with Jamie's photos"
     - Add onClick handler: `onTryDemo()`
     - Match button styling with "Import Backup"

2. **`/android/app/src/main/java/com/smilepile/onboarding/OnboardingViewModel.kt`**
   - **Changes:**
     - Add `_isDemoMode = MutableStateFlow(false)`
     - Add `enterDemoMode()` method
     - Add `exitDemoMode()` method
     - Modify completion logic to skip when in demo mode
     - Add demo data loading logic

3. **`/android/app/src/main/java/com/smilepile/utils/PreferencesManager.kt`**
   - **Changes:**
     - Add `IS_DEMO_MODE` preference key
     - Add `isDemoMode: Boolean` property
     - Add getter/setter for demo mode flag

4. **`/android/app/src/main/java/com/smilepile/ui/MainActivity.kt`**
   - **Changes:**
     - Add conditional rendering for `DemoModeBanner`
     - Pass `isDemoMode` flag to composables
     - Disable edit actions when `isDemoMode == true`

5. **`/android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt`**
   - **Changes:**
     - Add "Try Demo Mode" menu item
     - Only show if user has completed onboarding
     - onClick handler to re-enter demo mode

6. **`/android/app/src/main/java/com/smilepile/data/repository/PhotoRepositoryImpl.kt`**
   - **Changes:**
     - Add `loadDemoPhotos()` method
     - Filter demo photos when `isDemoMode == true`
     - Prevent modifications when `isFromAssets == true`

7. **`/android/app/src/main/java/com/smilepile/data/repository/CategoryRepositoryImpl.kt`**
   - **Changes:**
     - Add `loadDemoCategories()` method
     - Return demo categories when `isDemoMode == true`
     - Prevent modifications to demo categories

---

## 3. Data Model Design

### 3.1 Demo Profile Model

**iOS:**
```swift
struct DemoProfile {
    let name: String = "Jamie Anderson"
    let age: Int = 5
    let profileColor: String = "#8B7CC3"  // Friendly purple
    let photoCount: Int = 90  // Target: 75-100
    let categoryCount: Int = 8
    let timeSpanYears: Int = 5  // Birth to present
}
```

**Android:**
```kotlin
data class DemoProfile(
    val name: String = "Jamie Anderson",
    val age: Int = 5,
    val profileColor: String = "#8B7CC3",  // Friendly purple
    val photoCount: Int = 90,  // Target: 75-100
    val categoryCount: Int = 8,
    val timeSpanYears: Int = 5  // Birth to present
)
```

### 3.2 Demo Photo Specifications

**Structure:**
```swift
struct DemoPhoto {
    let id: String              // Unique identifier
    let assetName: String       // Asset catalog name
    let categoryId: Int64       // Category assignment
    let caption: String         // Parent-perspective caption
    let date: Date              // Realistic date (2019-2024)
    let fileSize: Int64         // Approximate size
    let width: Int              // Image dimensions
    let height: Int
}
```

**Example Data:**
```swift
let demoPhotos: [DemoPhoto] = [
    DemoPhoto(
        id: "demo_milestone_001",
        assetName: "demo_first_steps",
        categoryId: 1,  // Milestones
        caption: "Jamie's first steps - so proud!",
        date: Date(timeIntervalSince1970: 1580515200),  // Feb 2020
        fileSize: 2_500_000,
        width: 1920,
        height: 1080
    ),
    DemoPhoto(
        id: "demo_birthday_001",
        assetName: "demo_birthday_1",
        categoryId: 2,  // Birthdays
        caption: "Birthday #1 - cake smash was a success!",
        date: Date(timeIntervalSince1970: 1577923200),  // Jan 2020
        fileSize: 3_000_000,
        width: 1920,
        height: 1080
    ),
    // ... 88 more photos
]
```

### 3.3 Demo Category Specifications

**8 Categories (matching product story):**

```swift
let demoCategories: [Category] = [
    Category(
        id: 1,
        name: "milestones",
        displayName: "Milestones",
        position: 0,
        iconResource: "star.fill",
        colorHex: "#FFD700",  // Gold
        isDefault: false,
        createdAt: Int64(Date().timeIntervalSince1970 * 1000)
    ),
    Category(
        id: 2,
        name: "birthdays",
        displayName: "Birthdays",
        position: 1,
        iconResource: "gift.fill",
        colorHex: "#FF69B4",  // Pink
        isDefault: false
    ),
    Category(
        id: 3,
        name: "holidays",
        displayName: "Holidays",
        position: 2,
        iconResource: "sparkles",
        colorHex: "#FF4500",  // Orange-Red
        isDefault: false
    ),
    Category(
        id: 4,
        name: "family",
        displayName: "Family",
        position: 3,
        iconResource: "heart.fill",
        colorHex: "#E91E63",  // Deep Pink
        isDefault: false
    ),
    Category(
        id: 5,
        name: "playtime",
        displayName: "Playtime",
        position: 4,
        iconResource: "sportscourt.fill",
        colorHex: "#2196F3",  // Blue
        isDefault: false
    ),
    Category(
        id: 6,
        name: "friends",
        displayName: "Friends",
        position: 5,
        iconResource: "person.2.fill",
        colorHex: "#9C27B0",  // Purple
        isDefault: false
    ),
    Category(
        id: 7,
        name: "creativity",
        displayName: "Creativity",
        position: 6,
        iconResource: "paintbrush.fill",
        colorHex: "#00BCD4",  // Cyan
        isDefault: false
    ),
    Category(
        id: 8,
        name: "adventures",
        displayName: "Adventures",
        position: 7,
        iconResource: "airplane",
        colorHex: "#4CAF50",  // Green
        isDefault: false
    )
]
```

**Photo Distribution:**
- Milestones: 20 photos
- Birthdays: 15 photos
- Holidays: 18 photos
- Family: 12 photos
- Playtime: 10 photos
- Friends: 8 photos
- Creativity: 8 photos
- Adventures: 9 photos
**Total: 100 photos**

### 3.4 Settings Flags Needed

**iOS (SettingsManager.swift):**
```swift
private struct Keys {
    static let isDemoMode = "is_demo_mode"
    static let demoModeEntered = "demo_mode_entered"  // Track if ever entered
    static let demoModeEntryCount = "demo_mode_entry_count"  // Analytics
}

@AppStorage(Keys.isDemoMode) var isDemoMode: Bool = false
@AppStorage(Keys.demoModeEntered) var demoModeEntered: Bool = false
@AppStorage(Keys.demoModeEntryCount) var demoModeEntryCount: Int = 0
```

**Android (PreferencesManager.kt):**
```kotlin
companion object {
    private const val IS_DEMO_MODE = "is_demo_mode"
    private const val DEMO_MODE_ENTERED = "demo_mode_entered"
    private const val DEMO_MODE_ENTRY_COUNT = "demo_mode_entry_count"
}

var isDemoMode: Boolean
    get() = prefs.getBoolean(IS_DEMO_MODE, false)
    set(value) = prefs.edit().putBoolean(IS_DEMO_MODE, value).apply()

var demoModeEntered: Boolean
    get() = prefs.getBoolean(DEMO_MODE_ENTERED, false)
    set(value) = prefs.edit().putBoolean(DEMO_MODE_ENTERED, value).apply()

var demoModeEntryCount: Int
    get() = prefs.getInt(DEMO_MODE_ENTRY_COUNT, 0)
    set(value) = prefs.edit().putInt(DEMO_MODE_ENTRY_COUNT, value).apply()
```

---

## 4. Implementation Steps (Detailed)

### Step 1: Create Demo Mode Infrastructure

**iOS:**
1. Create `/ios/SmilePile/DemoMode/` directory
2. Create `DemoModeManager.swift` with basic structure:
   ```swift
   class DemoModeManager: ObservableObject {
       static let shared = DemoModeManager()
       @Published var isDemoMode: Bool = false

       func enterDemoMode() { }
       func exitDemoMode() { }
       func loadDemoData() { }
   }
   ```
3. Add demo mode keys to `SettingsManager.swift`
4. Register `DemoModeManager` in app initialization

**Android:**
1. Create `/android/app/src/main/java/com/smilepile/demomode/` package
2. Create `DemoModeManager.kt` with basic structure:
   ```kotlin
   object DemoModeManager {
       private val _isDemoMode = MutableStateFlow(false)
       val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

       suspend fun enterDemoMode(context: Context) { }
       suspend fun exitDemoMode(context: Context) { }
       private suspend fun loadDemoData(context: Context) { }
   }
   ```
3. Add demo mode keys to `PreferencesManager.kt`
4. Initialize in `Application` class

### Step 2: Define Demo Data

**iOS:**
1. Create `DemoData.swift` file
2. Define `DemoProfile` struct
3. Define `demoCategories` array (8 categories)
4. Define `demoPhotoMetadata` array (90-100 entries)
5. Include realistic captions and dates (2019-2024)

**Android:**
1. Create `DemoData.kt` file
2. Define `DemoProfile` data class
3. Define `demoCategories` list (8 categories)
4. Define `demoPhotoMetadata` list (90-100 entries)
5. Include realistic captions and dates (2019-2024)

### Step 3: Implement Demo Asset Management

**iOS:**
1. Create `Assets.xcassets/DemoPhotos/` directory structure
2. Add placeholder images for each category (8 subdirectories)
3. Implement `DemoAssetLoader` utility class:
   ```swift
   class DemoAssetLoader {
       static func loadImage(named: String) -> UIImage? {
           return UIImage(named: named)
       }

       static func saveToDocuments(_ image: UIImage, filename: String) -> URL? {
           // Save to app documents directory
       }
   }
   ```
4. Add asset preloading logic to `DemoModeManager`

**Android:**
1. Create `/res/drawable-nodpi/demo_photos/` directory structure
2. Add placeholder images for each category (8 subdirectories)
3. Implement `DemoAssetLoader` utility object:
   ```kotlin
   object DemoAssetLoader {
       fun loadDrawable(context: Context, resourceName: String): Drawable? {
           val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
           return if (resId != 0) context.getDrawable(resId) else null
       }

       fun saveToInternalStorage(context: Context, drawable: Drawable, filename: String): String {
           // Save to app internal storage
       }
   }
   ```
4. Add asset preloading logic to `DemoModeManager`

### Step 4: Update Welcome Screen

**iOS:**
1. Open `WelcomeScreen.swift`
2. Add "Try Demo" button after "Get Started" button:
   ```swift
   // Try Demo button
   Button(action: {
       coordinator.enterDemoMode()
   }) {
       HStack {
           Image(systemName: "photo.on.rectangle.angled")
               .font(.title3)
           Text("Try Demo")
               .font(typography.bodyLarge)
               .fontWeight(.semibold)
       }
       .foregroundColor(.smilePileBlue)
       .frame(maxWidth: .infinity)
       .frame(height: 56)
       .background(
           RoundedRectangle(cornerRadius: 12)
               .stroke(Color.smilePileBlue, lineWidth: 2)
       )
   }
   .padding(.horizontal, 40)

   Text("Explore with Jamie's photos")
       .font(typography.bodySmall)
       .foregroundColor(.secondary)
       .padding(.horizontal, 40)
   ```

**Android:**
1. Open `WelcomeScreen.kt`
2. Add "Try Demo" button after "Import Backup" button:
   ```kotlin
   // Try Demo button
   OutlinedButton(
       onClick = onTryDemo,
       modifier = Modifier
           .fillMaxWidth()
           .height(56.dp),
       shape = MaterialTheme.shapes.medium,
       colors = ButtonDefaults.outlinedButtonColors(
           contentColor = SmilePileBlue
       )
   ) {
       Icon(
           imageVector = Icons.Outlined.PhotoLibrary,
           contentDescription = null,
           modifier = Modifier.size(24.dp)
       )
       Spacer(modifier = Modifier.width(8.dp))
       Text(
           text = "Try Demo",
           fontSize = 18.sp,
           fontWeight = FontWeight.Bold
       )
   }

   Text(
       text = "Explore with Jamie's photos",
       fontSize = 14.sp,
       color = MaterialTheme.colorScheme.onSurfaceVariant,
       textAlign = TextAlign.Center,
       modifier = Modifier.padding(top = 4.dp)
   )
   ```

### Step 5: Implement Demo Mode Entry Logic

**iOS:**
1. In `OnboardingCoordinator.swift`, add:
   ```swift
   func enterDemoMode() {
       Task { @MainActor in
           do {
               // Set demo mode flag
               SettingsManager.shared.isDemoMode = true
               DemoModeManager.shared.isDemoMode = true

               // Load demo data
               try await DemoModeManager.shared.loadDemoData()

               // Skip onboarding
               SettingsManager.shared.onboardingCompleted = true

               // Track entry
               SettingsManager.shared.demoModeEntered = true
               SettingsManager.shared.demoModeEntryCount += 1

               // Navigate to gallery
               isComplete = true
               NotificationCenter.default.post(name: .demoModeEntered, object: nil)

           } catch {
               showError(message: "Failed to load demo mode: \(error.localizedDescription)")
           }
       }
   }
   ```

2. Implement `loadDemoData()` in `DemoModeManager.swift`:
   ```swift
   func loadDemoData() async throws {
       let categoryRepo = CategoryRepositoryImpl.shared
       let photoRepo = PhotoRepositoryImpl()

       // Clear existing data
       try await categoryRepo.deleteAllCategories()
       try await photoRepo.deleteAllPhotos()

       // Insert demo categories
       for category in DemoData.demoCategories {
           try await categoryRepo.insertCategory(category)
       }

       // Load and insert demo photos
       for photoMeta in DemoData.demoPhotoMetadata {
           // Load image from assets
           guard let image = DemoAssetLoader.loadImage(named: photoMeta.assetName) else {
               continue
           }

           // Save to documents
           guard let savedURL = DemoAssetLoader.saveToDocuments(image, filename: photoMeta.assetName) else {
               continue
           }

           // Create Photo object
           let photo = Photo(
               path: savedURL.path,
               categoryId: photoMeta.categoryId,
               name: photoMeta.caption,
               isFromAssets: true,  // Mark as demo photo
               createdAt: Int64(photoMeta.date.timeIntervalSince1970 * 1000),
               fileSize: photoMeta.fileSize,
               width: photoMeta.width,
               height: photoMeta.height
           )

           try await photoRepo.insertPhoto(photo)
       }
   }
   ```

**Android:**
1. In `OnboardingViewModel.kt`, add:
   ```kotlin
   fun enterDemoMode() {
       viewModelScope.launch {
           try {
               // Set demo mode flag
               preferencesManager.isDemoMode = true
               DemoModeManager.enterDemoMode(context)

               // Track entry
               preferencesManager.demoModeEntered = true
               preferencesManager.demoModeEntryCount++

               // Navigate to gallery
               _onboardingComplete.value = true

           } catch (e: Exception) {
               _errorMessage.value = "Failed to load demo mode: ${e.message}"
           }
       }
   }
   ```

2. Implement `enterDemoMode()` in `DemoModeManager.kt`:
   ```kotlin
   suspend fun enterDemoMode(context: Context) = withContext(Dispatchers.IO) {
       val categoryRepo = CategoryRepositoryImpl(context)
       val photoRepo = PhotoRepositoryImpl(context)

       // Clear existing data
       categoryRepo.deleteAllCategories()
       photoRepo.deleteAllPhotos()

       // Insert demo categories
       DemoData.demoCategories.forEach { category ->
           categoryRepo.insertCategory(category)
       }

       // Load and insert demo photos
       DemoData.demoPhotoMetadata.forEach { photoMeta ->
           // Load drawable from resources
           val drawable = DemoAssetLoader.loadDrawable(context, photoMeta.assetName) ?: return@forEach

           // Save to internal storage
           val savedPath = DemoAssetLoader.saveToInternalStorage(
               context,
               drawable,
               photoMeta.assetName
           )

           // Create Photo object
           val photo = Photo(
               path = savedPath,
               categoryId = photoMeta.categoryId,
               name = photoMeta.caption,
               isFromAssets = true,  // Mark as demo photo
               createdAt = photoMeta.date,
               fileSize = photoMeta.fileSize,
               width = photoMeta.width,
               height = photoMeta.height
           )

           photoRepo.insertPhoto(photo)
       }

       _isDemoMode.value = true
   }
   ```

### Step 6: Create Demo Mode Banner

**iOS:**
1. Create `DemoModeBanner.swift`:
   ```swift
   struct DemoModeBanner: View {
       @ObservedObject var demoManager: DemoModeManager
       @State private var showExitDialog = false
       @Environment(\.typography) var typography: Typography

       var body: some View {
           HStack {
               Image(systemName: "photo.on.rectangle.angled")
                   .foregroundColor(.purple)

               Text("Demo Mode - Viewing Jamie's Photos")
                   .font(typography.bodyMedium)
                   .fontWeight(.semibold)
                   .foregroundColor(.purple)

               Spacer()

               Button(action: {
                   showExitDialog = true
               }) {
                   Text("Exit Demo")
                       .font(typography.bodySmall)
                       .fontWeight(.semibold)
                       .foregroundColor(.purple)
                       .padding(.horizontal, 12)
                       .padding(.vertical, 6)
                       .background(Color.white.opacity(0.5))
                       .cornerRadius(8)
               }
           }
           .padding(.horizontal, 16)
           .padding(.vertical, 12)
           .background(Color(hex: "#E8E0F5"))  // Soft purple
           .sheet(isPresented: $showExitDialog) {
               ExitDemoDialog(demoManager: demoManager)
           }
       }
   }
   ```

**Android:**
1. Create `DemoModeBanner.kt`:
   ```kotlin
   @Composable
   fun DemoModeBanner(
       onExitDemo: () -> Unit,
       modifier: Modifier = Modifier
   ) {
       var showExitDialog by remember { mutableStateOf(false) }

       Row(
           modifier = modifier
               .fillMaxWidth()
               .background(Color(0xFFE8E0F5))  // Soft purple
               .padding(horizontal = 16.dp, vertical = 12.dp),
           horizontalArrangement = Arrangement.SpaceBetween,
           verticalAlignment = Alignment.CenterVertically
       ) {
           Row(
               horizontalArrangement = Arrangement.spacedBy(8.dp),
               verticalAlignment = Alignment.CenterVertically
           ) {
               Icon(
                   imageVector = Icons.Outlined.PhotoLibrary,
                   contentDescription = null,
                   tint = Color(0xFF8B7CC3)
               )

               Text(
                   text = "Demo Mode - Viewing Jamie's Photos",
                   fontSize = 16.sp,
                   fontWeight = FontWeight.SemiBold,
                   color = Color(0xFF8B7CC3)
               )
           }

           TextButton(
               onClick = { showExitDialog = true }
           ) {
               Text(
                   text = "Exit Demo",
                   fontSize = 14.sp,
                   fontWeight = FontWeight.SemiBold,
                   color = Color(0xFF8B7CC3)
               )
           }
       }

       if (showExitDialog) {
           ExitDemoDialog(
               onConfirm = {
                   showExitDialog = false
                   onExitDemo()
               },
               onDismiss = { showExitDialog = false }
           )
       }
   }
   ```

### Step 7: Implement Exit Demo Flow

**iOS:**
1. Create `ExitDemoDialog.swift`:
   ```swift
   struct ExitDemoDialog: View {
       @ObservedObject var demoManager: DemoModeManager
       @Environment(\.dismiss) var dismiss
       @Environment(\.typography) var typography: Typography

       var body: some View {
           VStack(spacing: 24) {
               // Title
               Text("Ready to organize your own memories?")
                   .font(typography.headlineSmall)
                   .fontWeight(.bold)
                   .multilineTextAlignment(.center)

               // Description
               Text("You can always return to demo mode from settings")
                   .font(typography.bodyMedium)
                   .foregroundColor(.secondary)
                   .multilineTextAlignment(.center)

               // Buttons
               VStack(spacing: 12) {
                   // Primary: Start Fresh
                   Button(action: {
                       Task {
                           await demoManager.exitDemoMode()
                           dismiss()
                       }
                   }) {
                       Text("Start Organizing My Photos")
                           .font(typography.bodyLarge)
                           .fontWeight(.bold)
                           .foregroundColor(.white)
                           .frame(maxWidth: .infinity)
                           .frame(height: 50)
                           .background(Color.smilePileBlue)
                           .cornerRadius(12)
                   }

                   // Secondary: Continue Exploring
                   Button(action: {
                       dismiss()
                   }) {
                       Text("Continue Exploring")
                           .font(typography.bodyMedium)
                           .foregroundColor(.smilePileBlue)
                   }
               }
           }
           .padding(24)
       }
   }
   ```

2. Implement `exitDemoMode()` in `DemoModeManager.swift`:
   ```swift
   func exitDemoMode() async {
       // Clear demo mode flag
       SettingsManager.shared.isDemoMode = false
       self.isDemoMode = false

       // Clear demo data
       let categoryRepo = CategoryRepositoryImpl.shared
       let photoRepo = PhotoRepositoryImpl()

       do {
           try await categoryRepo.deleteAllCategories()
           try await photoRepo.deleteAllPhotos()
       } catch {
           print("Failed to clear demo data: \(error)")
       }

       // Reset onboarding
       SettingsManager.shared.resetOnboarding()

       // Post notification to restart onboarding
       NotificationCenter.default.post(name: .demoModeExited, object: nil)
   }
   ```

**Android:**
1. Create `ExitDemoDialog.kt`:
   ```kotlin
   @Composable
   fun ExitDemoDialog(
       onConfirm: () -> Unit,
       onDismiss: () -> Unit
   ) {
       AlertDialog(
           onDismissRequest = onDismiss,
           title = {
               Text(
                   text = "Ready to organize your own memories?",
                   fontSize = 20.sp,
                   fontWeight = FontWeight.Bold,
                   textAlign = TextAlign.Center
               )
           },
           text = {
               Text(
                   text = "You can always return to demo mode from settings",
                   fontSize = 16.sp,
                   color = MaterialTheme.colorScheme.onSurfaceVariant,
                   textAlign = TextAlign.Center
               )
           },
           confirmButton = {
               Button(
                   onClick = onConfirm,
                   modifier = Modifier.fillMaxWidth(),
                   colors = ButtonDefaults.buttonColors(
                       containerColor = Color(0xFF2196F3)
                   )
               ) {
                   Text(
                       text = "Start Organizing My Photos",
                       fontSize = 16.sp,
                       fontWeight = FontWeight.Bold
                   )
               }
           },
           dismissButton = {
               TextButton(
                   onClick = onDismiss,
                   modifier = Modifier.fillMaxWidth()
               ) {
                   Text(
                       text = "Continue Exploring",
                       fontSize = 16.sp,
                       color = Color(0xFF2196F3)
                   )
               }
           }
       )
   }
   ```

2. Implement `exitDemoMode()` in `DemoModeManager.kt`:
   ```kotlin
   suspend fun exitDemoMode(context: Context) = withContext(Dispatchers.IO) {
       // Clear demo mode flag
       val prefsManager = PreferencesManager(context)
       prefsManager.isDemoMode = false
       _isDemoMode.value = false

       // Clear demo data
       val categoryRepo = CategoryRepositoryImpl(context)
       val photoRepo = PhotoRepositoryImpl(context)

       try {
           categoryRepo.deleteAllCategories()
           photoRepo.deleteAllPhotos()
       } catch (e: Exception) {
           Log.e("DemoModeManager", "Failed to clear demo data", e)
       }

       // Reset onboarding
       prefsManager.onboardingCompleted = false
       prefsManager.firstLaunch = true
   }
   ```

### Step 8: Add Demo Banner to Main UI

**iOS:**
1. In `ContentView.swift`, add at the top:
   ```swift
   @StateObject var demoManager = DemoModeManager.shared

   var body: some View {
       VStack(spacing: 0) {
           // Demo mode banner
           if demoManager.isDemoMode {
               DemoModeBanner(demoManager: demoManager)
           }

           // Rest of UI
           // ...
       }
   }
   ```

**Android:**
1. In `MainActivity.kt`, add at the top of scaffold:
   ```kotlin
   val isDemoMode by DemoModeManager.isDemoMode.collectAsState()

   Scaffold(
       topBar = {
           Column {
               if (isDemoMode) {
                   DemoModeBanner(
                       onExitDemo = {
                           lifecycleScope.launch {
                               DemoModeManager.exitDemoMode(this@MainActivity)
                               // Navigate to onboarding
                           }
                       }
                   )
               }
               // Regular top bar
           }
       }
   ) { paddingValues ->
       // Content
   }
   ```

### Step 9: Restrict Edit Actions in Demo Mode

**iOS:**
1. In `PhotoGalleryView.swift`:
   ```swift
   @StateObject var demoManager = DemoModeManager.shared

   // Disable add photo button
   .disabled(demoManager.isDemoMode)
   .onTapGesture {
       if demoManager.isDemoMode {
           showDemoModeMessage()
       }
   }

   func showDemoModeMessage() {
       // Show alert: "This feature is available with your own photos..."
   }
   ```

2. Apply to all edit actions:
   - Add photo button
   - Delete photo
   - Edit category
   - Delete category
   - Photo import

**Android:**
1. In photo/category screens:
   ```kotlin
   val isDemoMode by DemoModeManager.isDemoMode.collectAsState()

   // Disable add photo FAB
   if (!isDemoMode) {
       FloatingActionButton(onClick = { /*...*/ }) {
           Icon(Icons.Default.Add, contentDescription = "Add Photo")
       }
   }

   // Show message on attempt to edit
   if (isDemoMode && attemptedEdit) {
       Snackbar(
           message = "This feature is available with your own photos. Exit demo mode to start organizing your memories!"
       )
   }
   ```

### Step 10: Add Re-entry from Settings

**iOS:**
1. In `SettingsViewCustom.swift`:
   ```swift
   // Only show if onboarding completed AND not in demo mode
   if settingsManager.onboardingCompleted && !settingsManager.isDemoMode {
       Button(action: {
           Task {
               await DemoModeManager.shared.enterDemoMode()
           }
       }) {
           HStack {
               Image(systemName: "photo.on.rectangle.angled")
               Text("Try Demo Mode")
               Spacer()
               Image(systemName: "chevron.right")
           }
       }
   }
   ```

**Android:**
1. In `SettingsScreen.kt`:
   ```kotlin
   val prefsManager = PreferencesManager(context)
   val isDemoMode by DemoModeManager.isDemoMode.collectAsState()

   // Only show if onboarding completed AND not in demo mode
   if (prefsManager.onboardingCompleted && !isDemoMode) {
       SettingsItem(
           icon = Icons.Outlined.PhotoLibrary,
           title = "Try Demo Mode",
           subtitle = "Explore with sample photos",
           onClick = {
               lifecycleScope.launch {
                   DemoModeManager.enterDemoMode(context)
               }
           }
       )
   }
   ```

### Step 11: Update Repository Logic

**iOS:**
1. In `PhotoRepositoryImpl.swift`:
   ```swift
   func getAllPhotos() async throws -> [Photo] {
       let demoManager = DemoModeManager.shared

       // If in demo mode, filter for demo photos only
       let allPhotos = try await database.fetchPhotos()

       if demoManager.isDemoMode {
           return allPhotos.filter { $0.isFromAssets }
       }

       return allPhotos
   }

   func insertPhoto(_ photo: Photo) async throws -> Int64 {
       // Prevent inserting new photos in demo mode
       guard !DemoModeManager.shared.isDemoMode else {
           throw PhotoError.demoModeRestriction
       }

       return try await database.insert(photo)
   }
   ```

**Android:**
1. In `PhotoRepositoryImpl.kt`:
   ```kotlin
   override suspend fun getAllPhotos(): List<Photo> {
       val allPhotos = photoDao.getAllPhotos()

       // If in demo mode, filter for demo photos only
       return if (DemoModeManager.isDemoMode.value) {
           allPhotos.filter { it.isFromAssets }
       } else {
           allPhotos
       }
   }

   override suspend fun insertPhoto(photo: Photo): Long {
       // Prevent inserting new photos in demo mode
       if (DemoModeManager.isDemoMode.value) {
           throw IllegalStateException("Cannot modify photos in demo mode")
       }

       return photoDao.insertPhoto(photo)
   }
   ```

### Step 12: Testing & Validation

**iOS:**
1. Build and run on simulator
2. Verify "Try Demo" button appears on welcome screen
3. Tap "Try Demo" and verify:
   - Demo data loads (90+ photos, 8 categories)
   - Banner appears at top
   - All photos are viewable
   - Edit actions are disabled
   - Exit flow works correctly
4. Test re-entry from settings
5. Run unit tests for `DemoModeManager`

**Android:**
1. Build and run on emulator
2. Verify "Try Demo" button appears on welcome screen
3. Tap "Try Demo" and verify:
   - Demo data loads (90+ photos, 8 categories)
   - Banner appears at top
   - All photos are viewable
   - Edit actions are disabled
   - Exit flow works correctly
4. Test re-entry from settings
5. Run unit tests for `DemoModeManager`

---

## 5. Demo Asset Strategy

### 5.1 Photo Sourcing

**Requirements:**
- No real children's photos (privacy/safety)
- High-quality, age-appropriate images
- Diverse representation
- Realistic parent-taken aesthetic (not stock photo look)

**Sources:**
1. **AI-Generated Images** (Primary):
   - Use Midjourney/DALL-E to generate realistic child photos
   - Prompt examples:
     - "Child taking first steps, parent perspective photo, natural lighting"
     - "5-year-old birthday party, cake cutting moment, candid"
   - Advantages: No privacy concerns, customizable, consistent subject

2. **Stock Photos** (Secondary):
   - Unsplash/Pexels with child-safe filters
   - Free commercial licenses
   - Look for candid, parent-perspective shots
   - Avoid overly professional/posed images

3. **Custom Illustrations** (Fallback):
   - If realistic photos prove difficult to source
   - Illustrated style can be charming and safe
   - Maintains privacy by design

**Selected Approach:** AI-Generated + Stock Photos (Mix)
- Use AI for consistency of "Jamie" appearance
- Supplement with stock photos for variety
- Ensure all images are properly licensed

### 5.2 Asset Naming Conventions

**Format:** `demo_[category]_[number]_[description].jpg`

**Examples:**
```
demo_milestones_001_first_steps.jpg
demo_milestones_002_tying_shoes.jpg
demo_birthdays_001_cake_smash.jpg
demo_birthdays_002_presents.jpg
demo_holidays_001_christmas_morning.jpg
demo_family_001_grandparents_visit.jpg
```

**Benefits:**
- Easy to identify demo assets
- Organized by category
- Descriptive for debugging
- Prevents naming conflicts

### 5.3 File Formats and Sizes

**Format:** JPEG (.jpg)
- Universal support
- Good compression
- Realistic for user photos

**Sizes:**
- **Original Resolution:** 1920x1080 (Full HD)
  - Realistic for modern phone cameras
  - Not overly large (2-3MB per photo)

- **Thumbnail Generation:** 300x300
  - Generated at runtime
  - Cached for performance

**Total Bundle Size Estimate:**
- 100 photos × 2.5MB average = 250MB
- Compressed in app bundle: ~200MB
- **Mitigation:** Use asset catalog compression

**Optimization Strategies:**
1. **On-Demand Loading:**
   - Load photos as needed, not all at once
   - Use lazy loading in galleries

2. **Progressive Quality:**
   - Store medium quality (1280x720) in bundle
   - Show message: "Demo photos are lower quality than your photos would be"

3. **Asset Catalog Optimization:**
   - iOS: Use Asset Catalog with compression
   - Android: Use WebP format for smaller sizes

**Revised Approach:**
- Use 1280x720 resolution (reduces to ~1.5MB per photo)
- Total bundle impact: ~150MB
- Acceptable for demo feature

### 5.4 Bundle Optimization

**iOS:**
```xml
<!-- Assets.xcassets compression settings -->
<key>compressionQuality</key>
<string>0.8</string>
<key>resizingMode</key>
<string>aspect-fit</string>
```

**Android:**
```gradle
// build.gradle.kts
android {
    aaptOptions {
        cruncherEnabled = true
        cruncherProcesses = Runtime.getRuntime().availableProcessors()
    }
}
```

**Alternative: On-First-Use Download**
- Store demo photos on CDN
- Download on first demo entry
- Cache locally for subsequent sessions
- **Pros:** Smaller initial bundle
- **Cons:** Requires network, slower first experience

**Decision:** Bundle Assets Locally
- Better user experience (instant demo mode)
- No network requirement
- One-time bundle size increase acceptable
- Users can delete app if needed

---

## 6. Testing Strategy

### 6.1 Unit Tests Needed

**iOS Unit Tests (XCTest):**

1. **DemoModeManagerTests.swift**
   ```swift
   class DemoModeManagerTests: XCTestCase {
       var sut: DemoModeManager!

       func testEnterDemoMode_SetsFlagToTrue()
       func testEnterDemoMode_LoadsDemoCategories()
       func testEnterDemoMode_LoadsDemoPhotos()
       func testExitDemoMode_ClearsDemoFlag()
       func testExitDemoMode_RemovesDemoData()
       func testDemoPhotos_HaveIsFromAssetsTrue()
       func testDemoCategories_MatchExpectedCount()
   }
   ```

2. **DemoDataTests.swift**
   ```swift
   class DemoDataTests: XCTestCase {
       func testDemoProfile_HasCorrectName()
       func testDemoCategories_Count_Equals8()
       func testDemoPhotos_Count_Between75And100()
       func testDemoPhotos_AllHaveCaptions()
       func testDemoPhotos_DatesAre2019To2024()
       func testPhotoDistribution_MatchesSpec()
   }
   ```

3. **RepositoryDemoModeTests.swift**
   ```swift
   class PhotoRepositoryDemoModeTests: XCTestCase {
       func testGetPhotos_InDemoMode_ReturnsOnlyDemoPhotos()
       func testInsertPhoto_InDemoMode_ThrowsError()
       func testDeletePhoto_InDemoMode_ThrowsError()
   }
   ```

**Android Unit Tests (JUnit + Mockk):**

1. **DemoModeManagerTest.kt**
   ```kotlin
   class DemoModeManagerTest {
       @Test
       fun `enterDemoMode sets flag to true`()

       @Test
       fun `enterDemoMode loads demo categories`()

       @Test
       fun `enterDemoMode loads demo photos`()

       @Test
       fun `exitDemoMode clears demo flag`()

       @Test
       fun `exitDemoMode removes demo data`()

       @Test
       fun `demo photos have isFromAssets true`()

       @Test
       fun `demo categories match expected count`()
   }
   ```

2. **DemoDataTest.kt**
   ```kotlin
   class DemoDataTest {
       @Test
       fun `demo profile has correct name`()

       @Test
       fun `demo categories count equals 8`()

       @Test
       fun `demo photos count between 75 and 100`()

       @Test
       fun `all demo photos have captions`()

       @Test
       fun `photo dates are 2019 to 2024`()

       @Test
       fun `photo distribution matches spec`()
   }
   ```

3. **PhotoRepositoryDemoModeTest.kt**
   ```kotlin
   class PhotoRepositoryDemoModeTest {
       @Test
       fun `getPhotos in demo mode returns only demo photos`()

       @Test
       fun `insertPhoto in demo mode throws exception`()

       @Test
       fun `deletePhoto in demo mode throws exception`()
   }
   ```

### 6.2 Integration Tests Needed

**iOS Integration Tests:**

1. **DemoModeFlowTests.swift**
   ```swift
   class DemoModeFlowTests: XCTestCase {
       func testFullDemoFlow_WelcomeToGalleryToExit()
       func testDemoModeReentry_FromSettings()
       func testDemoModeWithExistingData_DataIsolated()
   }
   ```

**Android Integration Tests (Espresso):**

1. **DemoModeFlowTest.kt**
   ```kotlin
   class DemoModeFlowTest {
       @Test
       fun testFullDemoFlow_WelcomeToGalleryToExit()

       @Test
       fun testDemoModeReentry_FromSettings()

       @Test
       fun testDemoModeWithExistingData_DataIsolated()
   }
   ```

### 6.3 Manual Test Scenarios

**Scenario 1: First-Time User Demo Entry**
1. Fresh install app
2. Tap "Try Demo" on welcome screen
3. **Verify:**
   - Demo loads in <2 seconds
   - 8 categories visible
   - 90+ photos visible
   - Banner displays at top
   - All photos are viewable in full screen

**Scenario 2: Demo Mode Restrictions**
1. Enter demo mode
2. Attempt to add photo
3. **Verify:** Educational message displays
4. Attempt to delete category
5. **Verify:** Action is disabled/message displays

**Scenario 3: Exit Demo Flow**
1. In demo mode, tap "Exit Demo"
2. **Verify:** Confirmation dialog appears
3. Tap "Continue Exploring"
4. **Verify:** Dialog closes, still in demo
5. Tap "Exit Demo" again
6. Tap "Start Organizing My Photos"
7. **Verify:**
   - Demo data cleared
   - Returned to welcome screen
   - Can now start fresh onboarding

**Scenario 4: Re-Entry from Settings**
1. Complete onboarding with real data
2. Go to Settings
3. **Verify:** "Try Demo Mode" option visible
4. Tap "Try Demo Mode"
5. **Verify:**
   - Demo mode enters
   - Previous user data hidden
   - Demo data displays

**Scenario 5: Demo Mode Persistence**
1. Enter demo mode
2. Close app (don't exit demo)
3. Reopen app
4. **Verify:**
   - App reopens in demo mode
   - Demo data still present
   - Banner still displays

**Scenario 6: Data Isolation**
1. Create real profile with photos
2. Enter demo mode
3. **Verify:** Real photos not visible
4. Exit demo mode
5. **Verify:** Real photos reappear

### 6.4 Platform-Specific Test Requirements

**iOS Specific:**
- Test on multiple iOS versions (14, 15, 16, 17, 18)
- Test on different device sizes (SE, regular, Plus/Max)
- Test with Dynamic Type (accessibility)
- Test with VoiceOver enabled
- Test with Reduce Motion enabled

**Android Specific:**
- Test on multiple Android versions (6.0 - 14)
- Test on different screen sizes (phone, tablet)
- Test with TalkBack enabled
- Test with different system fonts/sizes
- Test on low-memory devices

**Performance Tests:**
- Demo load time: <2 seconds (target: <1 second)
- Memory usage: <50MB increase from demo assets
- Photo grid scroll: 60fps
- Banner overlay: No frame drops

---

## 7. Migration & Rollback Plan

### 7.1 App Update Handling

**Scenario: User Updates App with Demo Mode Enabled**

**iOS:**
```swift
// In AppDelegate or @main App struct
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

    // Check if demo mode was active in previous version
    if SettingsManager.shared.isDemoMode {
        // Keep demo mode active
        DemoModeManager.shared.isDemoMode = true

        // Verify demo data integrity
        Task {
            await DemoModeManager.shared.verifyDemoData()
        }
    }

    return true
}
```

**Android:**
```kotlin
// In Application class
override fun onCreate() {
    super.onCreate()

    // Check if demo mode was active
    val prefsManager = PreferencesManager(this)
    if (prefsManager.isDemoMode) {
        // Keep demo mode active
        lifecycleScope.launch {
            DemoModeManager.restoreDemoMode(this@App)
        }
    }
}
```

**Migration Strategy:**
1. Check `isDemoMode` flag on app launch
2. If true, restore demo state
3. Verify demo data exists and is complete
4. If demo data corrupted, offer to reload or exit

### 7.2 Rollback Strategy

**If Issues Arise Post-Deployment:**

**Option 1: Disable Feature via Remote Config**
```swift
// iOS
if RemoteConfig.shared.isDemoModeEnabled {
    // Show "Try Demo" button
} else {
    // Hide button, prevent entry
}
```

```kotlin
// Android
if (remoteConfig.getBoolean("demo_mode_enabled")) {
    // Show "Try Demo" button
} else {
    // Hide button, prevent entry
}
```

**Option 2: Graceful Degradation**
- If demo assets fail to load, show error message
- Offer to exit demo mode or retry
- Log error for debugging

**Option 3: Emergency Exit**
- Add hidden gesture to force-exit demo mode
- Accessible via Settings > About > (Tap 7 times)
- Clears all demo data and resets app

**Rollback Decision Tree:**
```
Issue Detected
    │
    ├─ Affects <5% users → Monitor, fix in next release
    │
    ├─ Affects 5-20% users → Remote config disable + hotfix
    │
    └─ Affects >20% users → Emergency rollback, remove feature
```

### 7.3 Data Preservation Concerns

**User Data Safety:**

1. **Demo mode should NEVER affect user's real data**
   - Demo data stored with `isFromAssets = true` flag
   - Separate queries for demo vs. real data
   - Exit demo mode preserves user data

2. **Verification Checks:**
   ```swift
   func verifyDataIntegrity() async throws {
       let photoRepo = PhotoRepositoryImpl()
       let userPhotos = try await photoRepo.getAllPhotos().filter { !$0.isFromAssets }
       let demoPhotos = try await photoRepo.getAllPhotos().filter { $0.isFromAssets }

       // User photos should be unchanged
       assert(userPhotos.count == expectedUserPhotoCount)
   }
   ```

3. **Database Backup Before Demo Entry:**
   ```swift
   func enterDemoMode() async throws {
       // Backup user data before switching to demo
       try await BackupManager.shared.createLocalBackup()

       // Proceed with demo mode entry
       // ...
   }
   ```

**Recovery Plan:**
- If user reports data loss after demo mode:
  1. Check for local backup
  2. Restore from backup
  3. Disable demo mode for user
  4. Log incident for investigation

---

## 8. Edge Cases & Error Handling

### 8.1 Edge Cases

**Edge Case 1: User in Demo Mode Tries to Add Real Photos**

**Scenario:** User taps "Add Photo" button while in demo mode

**Handling:**
```swift
// iOS
if DemoModeManager.shared.isDemoMode {
    showAlert(
        title: "Feature Available with Your Photos",
        message: "This feature is available when you organize your own photos. Exit demo mode to start organizing your memories!"
    )
    return
}
```

```kotlin
// Android
if (DemoModeManager.isDemoMode.value) {
    Snackbar.make(
        view,
        "This feature is available with your own photos. Exit demo mode to start!",
        Snackbar.LENGTH_LONG
    ).show()
    return
}
```

**Edge Case 2: User Has Existing Data When Trying Demo**

**Scenario:** User with photos/categories taps "Try Demo Mode" from settings

**Handling:**
```swift
// iOS
func enterDemoMode() async throws {
    // Check for existing user data
    let photoCount = try await photoRepo.getPhotoCount()

    if photoCount > 0 {
        // Show confirmation dialog
        let confirmed = await showConfirmation(
            title: "Switch to Demo Mode?",
            message: "Your photos will be hidden while in demo mode. They'll reappear when you exit."
        )

        guard confirmed else { return }
    }

    // Proceed with demo mode
    // ...
}
```

**Edge Case 3: Demo Data Corruption**

**Scenario:** Demo assets missing or database corrupted

**Handling:**
```swift
// iOS
func loadDemoData() async throws {
    do {
        try await loadDemoCategories()
        try await loadDemoPhotos()

        // Verify data integrity
        let categoryCount = try await categoryRepo.getCount()
        let photoCount = try await photoRepo.getPhotoCount()

        if categoryCount < 8 || photoCount < 75 {
            throw DemoModeError.incompleteData
        }

    } catch {
        // Show error and offer to retry or exit
        showError(
            title: "Demo Mode Error",
            message: "Failed to load demo data. Would you like to try again?",
            actions: [
                ("Retry", { try await self.loadDemoData() }),
                ("Exit Demo", { await self.exitDemoMode() })
            ]
        )
        throw error
    }
}
```

**Edge Case 4: App Crashes During Demo Mode Initialization**

**Scenario:** App crashes while loading demo assets

**Handling:**
```swift
// iOS - On next launch
func application(_ application: UIApplication, didFinishLaunchingWithOptions...) {
    // Detect incomplete demo mode entry
    if SettingsManager.shared.isDemoMode {
        let demoDataComplete = DemoModeManager.shared.verifyDemoDataSync()

        if !demoDataComplete {
            // Demo mode entry was interrupted
            SettingsManager.shared.isDemoMode = false

            // Show recovery message
            showRecoveryAlert()
        }
    }
}
```

**Edge Case 5: User Tries to Import Backup While in Demo Mode**

**Scenario:** User in demo mode attempts to import a backup file

**Handling:**
```swift
// iOS
func importBackup() async throws {
    // Check if in demo mode
    if DemoModeManager.shared.isDemoMode {
        let exitConfirmed = await showConfirmation(
            title: "Exit Demo Mode?",
            message: "You need to exit demo mode before importing a backup. Exit now?"
        )

        guard exitConfirmed else { return }

        // Exit demo mode first
        await DemoModeManager.shared.exitDemoMode()
    }

    // Proceed with backup import
    // ...
}
```

**Edge Case 6: Network Failure During Asset Download (Future)**

**Scenario:** If demo assets moved to CDN, network fails during download

**Handling:**
```swift
// iOS
func downloadDemoAssets() async throws {
    do {
        try await assetDownloader.downloadAll(progress: { percent in
            updateLoadingProgress(percent)
        })
    } catch NetworkError.connectionLost {
        showError(
            title: "Network Error",
            message: "Demo mode requires internet connection for first use. Please try again when online.",
            actions: [("OK", nil)]
        )
        throw DemoModeError.networkRequired
    }
}
```

### 8.2 Error Handling Strategies

**Error Types:**

```swift
// iOS
enum DemoModeError: LocalizedError {
    case incompleteData
    case assetLoadFailure(String)
    case databaseError(Error)
    case networkRequired
    case userCancelled

    var errorDescription: String? {
        switch self {
        case .incompleteData:
            return "Demo data is incomplete. Please try again."
        case .assetLoadFailure(let assetName):
            return "Failed to load asset: \(assetName)"
        case .databaseError(let error):
            return "Database error: \(error.localizedDescription)"
        case .networkRequired:
            return "Internet connection required for demo mode"
        case .userCancelled:
            return "Demo mode entry cancelled"
        }
    }
}
```

```kotlin
// Android
sealed class DemoModeError : Exception() {
    object IncompleteData : DemoModeError()
    data class AssetLoadFailure(val assetName: String) : DemoModeError()
    data class DatabaseError(override val cause: Throwable) : DemoModeError()
    object NetworkRequired : DemoModeError()
    object UserCancelled : DemoModeError()
}
```

**Error Recovery Actions:**

1. **Retry Logic:**
   - Auto-retry asset loading (3 attempts)
   - Exponential backoff for network errors

2. **User Feedback:**
   - Clear error messages in user-friendly language
   - Offer actionable next steps (Retry, Exit, Contact Support)

3. **Logging:**
   - Log all errors for debugging
   - Include context: OS version, device, error timestamp
   - Send to analytics (anonymized)

4. **Graceful Degradation:**
   - If some assets fail, offer partial demo mode
   - "Some demo photos couldn't load. Continue anyway?"

---

## 9. Performance Considerations

### 9.1 Load Time Targets

**Target: Demo Mode Entry <2 Seconds**

**Breakdown:**
- User taps "Try Demo": 0ms
- Load demo metadata: 100ms
- Load demo assets: 1500ms
- Insert into database: 300ms
- Navigate to gallery: 100ms
**Total: 2000ms**

**Optimization Strategies:**

1. **Lazy Asset Loading:**
   ```swift
   // Don't load all assets at once
   func loadDemoPhotos() async throws {
       // Load first 20 photos immediately (for initial gallery view)
       let priority = DemoData.demoPhotoMetadata.prefix(20)
       try await loadAssets(priority)

       // Load remaining assets in background
       Task.detached(priority: .background) {
           let remaining = Array(DemoData.demoPhotoMetadata.dropFirst(20))
           try await self.loadAssets(remaining)
       }
   }
   ```

2. **Asset Preloading (On App Install):**
   ```swift
   // Pre-process demo assets on first launch
   func application(_ application: UIApplication, didFinishLaunchingWithOptions...) {
       if !UserDefaults.standard.bool(forKey: "demo_assets_preprocessed") {
           Task.detached(priority: .utility) {
               await DemoAssetPreprocessor.optimizeAssets()
               UserDefaults.standard.set(true, forKey: "demo_assets_preprocessed")
           }
       }
   }
   ```

3. **Progressive Loading UI:**
   ```swift
   // Show progress during demo mode entry
   @State private var loadingProgress: Double = 0.0

   ProgressView("Loading Demo Mode...", value: loadingProgress, total: 1.0)
       .onReceive(demoManager.$loadingProgress) { progress in
           loadingProgress = progress
       }
   ```

### 9.2 Memory Usage

**Target: <50MB Memory Increase**

**Baseline Memory:**
- App without demo mode: ~100MB
- App with demo mode active: ~150MB (target)

**Memory Management:**

1. **Lazy Image Loading:**
   ```swift
   // iOS - Use AsyncImage or custom lazy loader
   AsyncImage(url: photoURL) { phase in
       switch phase {
       case .success(let image):
           image.resizable().aspectRatio(contentMode: .fill)
       case .failure, .empty:
           ProgressView()
       @unknown default:
           EmptyView()
       }
   }
   ```

2. **Image Caching:**
   ```swift
   // iOS - Use NSCache with memory limits
   class DemoImageCache {
       private let cache = NSCache<NSString, UIImage>()

       init() {
           cache.totalCostLimit = 50 * 1024 * 1024  // 50MB
           cache.countLimit = 100  // Max 100 images
       }
   }
   ```

3. **Memory Warnings:**
   ```swift
   // iOS - Clear cache on memory warning
   func applicationDidReceiveMemoryWarning(_ application: UIApplication) {
       DemoImageCache.shared.clearCache()
   }
   ```

### 9.3 Asset Bundle Size Impact

**Current App Size:** ~50MB

**Demo Mode Assets:** ~150MB (100 photos × 1.5MB)

**Total App Size:** ~200MB

**Mitigation:**
1. Use on-demand resources (iOS)
2. Use app bundles (Android)
3. Compress assets aggressively
4. Consider CDN for optional download

**Revised Approach:**
- Initial bundle: Include 30 photos (~45MB)
- Full demo: Download remaining 70 photos on first entry
- Best of both worlds: Fast initial install, complete demo when needed

### 9.4 Optimization Strategies

**Strategy 1: WebP Format (Android)**
```kotlin
// Android - Use WebP for smaller file sizes
val webpOptions = BitmapFactory.Options().apply {
    inPreferredConfig = Bitmap.Config.RGB_565
}
```

**Strategy 2: Asset Catalog Compression (iOS)**
```xml
<!-- Lossy compression for demo assets -->
<key>compression-type</key>
<string>lossy</string>
<key>compression-quality</key>
<real>0.75</real>
```

**Strategy 3: Thumbnail Generation**
```swift
// Generate and cache thumbnails on first access
func generateThumbnail(for photo: Photo) -> UIImage {
    let cacheKey = "thumb_\(photo.id)"

    if let cached = thumbnailCache.object(forKey: cacheKey as NSString) {
        return cached
    }

    // Generate thumbnail
    let thumbnail = resizeImage(photo, to: CGSize(width: 300, height: 300))

    // Cache it
    thumbnailCache.setObject(thumbnail, forKey: cacheKey as NSString)

    return thumbnail
}
```

**Strategy 4: Pagination**
```swift
// Load photos in pages of 20
func loadDemoPhotos(page: Int) async throws -> [Photo] {
    let start = page * 20
    let end = min(start + 20, DemoData.demoPhotoMetadata.count)

    return try await loadPhotoRange(start..<end)
}
```

---

## 10. Platform Parity Checklist

### 10.1 Feature Parity Items

| Feature | iOS | Android | Notes |
|---------|-----|---------|-------|
| "Try Demo" button on welcome screen | ✓ | ✓ | Same position, styling |
| Demo mode entry flow | ✓ | ✓ | Same steps, same data loaded |
| Demo banner at top | ✓ | ✓ | Same text, same purple color |
| Exit demo confirmation dialog | ✓ | ✓ | Same options, same wording |
| Demo mode re-entry from settings | ✓ | ✓ | Same menu location |
| Demo profile: Jamie Anderson | ✓ | ✓ | Exact same profile |
| Demo categories: 8 total | ✓ | ✓ | Same names, same colors |
| Demo photos: 90-100 total | ✓ | ✓ | Same images, same captions |
| Photo distribution by category | ✓ | ✓ | Matching counts |
| isFromAssets flag | ✓ | ✓ | Same database column |
| Edit restrictions in demo mode | ✓ | ✓ | Same actions disabled |
| Educational messages | ✓ | ✓ | Same wording |
| Demo mode persistence | ✓ | ✓ | Survives app restart |
| Data isolation | ✓ | ✓ | User data hidden during demo |

### 10.2 UI Consistency Requirements

**Welcome Screen:**
- Button order: Get Started, Import Backup, Try Demo (Android)
- Button order: Get Started, Try Demo (iOS - no Import Backup yet)
- "Try Demo" button style: Outlined, blue border
- Descriptive text: "Explore with Jamie's photos" (14sp/small font)

**Demo Banner:**
- Background: #E8E0F5 (soft purple)
- Text: "Demo Mode - Viewing Jamie's Photos"
- Font size: 16sp/bodyMedium
- Icon: Photo library icon (left)
- "Exit Demo" button (right)

**Exit Dialog:**
- Title: "Ready to organize your own memories?"
- Subtitle: "You can always return to demo mode from settings"
- Primary button: "Start Organizing My Photos" (blue, full width)
- Secondary button: "Continue Exploring" (text button)

**Educational Messages:**
- Wording: "This feature is available with your own photos. Exit demo mode to start organizing your memories!"
- Tone: Friendly, encouraging
- No error/warning icons (positive message)

### 10.3 Behavior Consistency

**Demo Mode Entry:**
1. User taps "Try Demo"
2. Loading indicator shows (if >500ms load time)
3. Demo data loads (categories first, then photos)
4. Navigate directly to gallery (skip onboarding steps)
5. Banner appears immediately
6. Photos load progressively (first 20, then remaining)

**Demo Mode Exit:**
1. User taps "Exit Demo" in banner
2. Confirmation dialog appears
3. If "Continue Exploring": dialog closes, stay in demo
4. If "Start Organizing":
   - Demo flag cleared
   - Demo data removed
   - Navigate to welcome screen
   - Can now start fresh onboarding

**Edit Restrictions:**
- Attempt to add photo: Show educational message
- Attempt to delete photo: Disabled (grayed out)
- Attempt to edit category: Show educational message
- Attempt to delete category: Disabled
- Attempt to import photos: Show educational message

**Data Behavior:**
- getAllPhotos() returns only demo photos when isDemoMode = true
- getAllCategories() returns only demo categories when isDemoMode = true
- insertPhoto() throws error when isDemoMode = true
- deletePhoto() throws error when isDemoMode = true
- User's real data is never deleted, only hidden

### 10.4 Cross-Platform Testing Matrix

| Test Case | iOS | Android | Pass Criteria |
|-----------|-----|---------|---------------|
| Demo entry from welcome | ☐ | ☐ | <2s load time |
| Demo banner displays | ☐ | ☐ | Visible, correct styling |
| 8 categories loaded | ☐ | ☐ | Exact count, correct names |
| 90-100 photos loaded | ☐ | ☐ | Correct count |
| Photos viewable full screen | ☐ | ☐ | All photos open correctly |
| Add photo disabled | ☐ | ☐ | Message displays |
| Delete photo disabled | ☐ | ☐ | Action blocked |
| Exit demo flow | ☐ | ☐ | Dialog works, data cleared |
| Re-entry from settings | ☐ | ☐ | Demo loads again |
| App restart in demo | ☐ | ☐ | Demo mode persists |
| Data isolation | ☐ | ☐ | User data hidden, not deleted |

---

## 11. Implementation Timeline

### 11.1 Estimated Effort

**Phase 5 (Implementation):**
- Setup infrastructure: 2 hours
- Define demo data: 3 hours
- Source/prepare demo assets: 8 hours
- Implement entry logic: 4 hours
- Implement exit logic: 2 hours
- Update UI components: 4 hours
- Add restrictions: 2 hours
- Testing: 4 hours
**Total: ~29 hours (~4 days)**

### 11.2 Critical Path

```
Day 1: Infrastructure + Data Definition
  ├─ Create DemoModeManager (iOS + Android)
  ├─ Add settings flags
  └─ Define DemoData structures

Day 2: Asset Preparation + Entry Logic
  ├─ Source/generate demo photos
  ├─ Organize assets in bundle
  ├─ Implement loadDemoData()
  └─ Implement enterDemoMode()

Day 3: UI Updates + Exit Logic
  ├─ Update WelcomeScreen
  ├─ Create DemoModeBanner
  ├─ Create ExitDemoDialog
  ├─ Implement exitDemoMode()
  └─ Add restrictions to edit actions

Day 4: Testing + Refinement
  ├─ Manual testing (both platforms)
  ├─ Fix bugs
  ├─ Performance optimization
  └─ Cross-platform parity verification
```

---

## 12. Security Considerations

### 12.1 Privacy

- **No Real Children:** All demo photos are AI-generated or stock photos
- **No PII:** Demo profile "Jamie Anderson" is fictional
- **Isolated Data:** Demo data never mixes with user data
- **Clear Separation:** `isFromAssets` flag ensures distinction

### 12.2 Data Integrity

- **Read-Only Mode:** Demo photos cannot be modified or deleted
- **Backup User Data:** Before demo entry, user data is verified intact
- **Recovery Path:** If corruption detected, offer to restore

### 12.3 Logging & Analytics

**Log Events:**
- Demo mode entered (count entry)
- Demo mode exited (time spent)
- Categories viewed in demo
- Photos viewed in demo
- Educational message displayed (edit attempt)

**Privacy-Safe:**
- No personally identifiable information logged
- All events anonymized
- No demo photo content logged

---

## 13. Success Metrics (from Product Story)

**Engagement Metrics:**
- Demo mode activation rate: Target 60% of new downloads
- Average session duration: Target 3-5 minutes
- Categories viewed: Target average 5+ categories
- Photos viewed: Target average 20+ photos

**Conversion Metrics:**
- Demo to real profile: Target 40% conversion rate
- Demo to app deletion: Target <20%
- Return to demo: Target <10%

**Quality Metrics:**
- Demo load time: Target <2 seconds
- Crash rate in demo: Target 0%
- User confusion reports: Target <5%

**Implementation Metrics (Phase 3 Specific):**
- Unit test coverage: >80%
- Performance benchmarks met: 100%
- Platform parity: 100%
- Code review passed: 1st attempt

---

## 14. Conclusion

This technical implementation plan provides a comprehensive roadmap for implementing demo mode in SmilePile. The feature follows existing architectural patterns, leverages the `isFromAssets` flag infrastructure, and maintains strict platform parity between iOS and Android.

**Key Design Decisions:**
1. Demo data bundled locally for instant experience
2. Read-only demo mode with educational messages
3. Data isolation via `isFromAssets` flag
4. Persistent banner for demo mode awareness
5. Simple exit flow with confirmation

**Implementation Readiness:**
- All file changes identified
- Detailed step-by-step instructions provided
- Edge cases and error handling planned
- Testing strategy comprehensive
- Performance targets defined

**Next Steps:**
1. Phase 4: Security Review (validate approach)
2. Phase 4: Peer Review (validate technical decisions)
3. Phase 5: Implementation (follow this plan)
4. Phase 6: Testing (execute test strategy)
5. Phase 7: Validation (verify against acceptance criteria)

---

**Document Status:** Ready for Phase 4 Review
**Prepared By:** Developer Agent
**Date:** 2025-10-17
**Version:** 1.0
