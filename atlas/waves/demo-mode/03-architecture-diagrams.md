# Demo Mode Architecture Diagrams
## Visual Reference for Phase 3 Technical Plan

**Date**: 2025-10-17
**Purpose**: Visual aids for implementation

---

## Diagram 1: High-Level Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         SmilePile App                            │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
        ┌───────▼────────┐            ┌────────▼───────┐
        │  UI Layer      │            │  Data Layer    │
        │                │            │                │
        │ - WelcomeScreen│            │ - PhotoRepo    │
        │ - Gallery      │◄───────────┤ - CategoryRepo │
        │ - Settings     │            │ - Settings     │
        │ - DemoBanner   │            │                │
        └────────┬───────┘            └────────┬───────┘
                 │                             │
                 └──────────┬──────────────────┘
                            │
                  ┌─────────▼──────────┐
                  │  DemoModeManager   │
                  │                    │
                  │ - isDemoMode       │
                  │ - enterDemoMode()  │
                  │ - exitDemoMode()   │
                  │ - loadDemoData()   │
                  └─────────┬──────────┘
                            │
                  ┌─────────▼──────────┐
                  │    Demo Assets     │
                  │                    │
                  │ - DemoData.swift   │
                  │ - Asset Catalog    │
                  │ - 100 photos       │
                  │ - 8 categories     │
                  └────────────────────┘
```

---

## Diagram 2: User Flow - Demo Mode Entry

```
┌──────────────┐
│   App Launch │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────┐
│  Onboarding Complete?        │
└──────┬───────────────────────┘
       │
       ├─── NO ──────┐
       │             │
       │             ▼
       │    ┌─────────────────┐
       │    │ WelcomeScreen   │
       │    │                 │
       │    │ [Get Started]   │
       │    │                 │
       │    │ [Try Demo] ◄────┼───── USER TAPS HERE
       │    └────────┬────────┘
       │             │
       │             ▼
       │    ┌─────────────────────────────┐
       │    │ DemoModeManager.enterDemo() │
       │    │                             │
       │    │ 1. Set isDemoMode = true    │
       │    │ 2. Load demo categories     │
       │    │ 3. Load demo photos         │
       │    │ 4. Skip onboarding          │
       │    └────────┬────────────────────┘
       │             │
       │             ▼
       │    ┌─────────────────┐
       │    │ Navigate to     │
       │    │ Gallery View    │
       │    └────────┬────────┘
       │             │
       │             ▼
       │    ┌─────────────────────────────┐
       │    │  Gallery with Demo Data     │
       │    │                             │
       │    │  ╔═══════════════════════╗  │
       │    │  ║ Demo Mode Banner      ║  │
       │    │  ║ "Viewing Jamie's..."  ║  │
       │    │  ║           [Exit Demo] ║  │
       │    │  ╚═══════════════════════╝  │
       │    │                             │
       │    │  Jamie Anderson Profile     │
       │    │  ┌───┐ ┌───┐ ┌───┐ ┌───┐   │
       │    │  │ 🎂│ │ 🎄│ │ 👨‍👩‍👧│ │ 🎨│   │
       │    │  └───┘ └───┘ └───┘ └───┘   │
       │    │  90 photos, 8 categories    │
       │    └─────────────────────────────┘
       │
       └─── YES ──────┐
                      │
                      ▼
              ┌─────────────┐
              │  Main App   │
              │             │
              │  Settings   │
              │  ├─ ...     │
              │  └─ [Try    │
              │     Demo]   │
              └──────┬──────┘
                     │
                     └───────► (Same demo flow as above)
```

---

## Diagram 3: User Flow - Demo Mode Exit

```
┌──────────────────────────┐
│  User in Demo Mode       │
│  (Banner visible)        │
└────────┬─────────────────┘
         │
         │ USER TAPS "Exit Demo"
         │
         ▼
┌──────────────────────────────────┐
│  Exit Demo Confirmation Dialog   │
│                                  │
│  "Ready to organize your own     │
│   memories?"                     │
│                                  │
│  ┌──────────────────────────┐   │
│  │ Start Organizing My Photos│   │
│  └──────────┬───────────────┘   │
│             │                    │
│  ┌──────────────────────────┐   │
│  │ Continue Exploring       │   │
│  └──────────┬───────────────┘   │
└─────────────┼───────────────────┘
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
[Continue]        [Start Fresh]
    │                   │
    │                   ▼
    │          ┌──────────────────────────┐
    │          │ DemoModeManager.exit()   │
    │          │                          │
    │          │ 1. Set isDemoMode=false  │
    │          │ 2. Clear demo data       │
    │          │ 3. Reset onboarding      │
    │          └────────┬─────────────────┘
    │                   │
    │                   ▼
    │          ┌──────────────────────────┐
    │          │  Navigate to Welcome     │
    │          │  (Fresh start)           │
    │          └──────────────────────────┘
    │
    ▼
┌──────────────────┐
│  Close Dialog    │
│  Stay in Demo    │
└──────────────────┘
```

---

## Diagram 4: Data Isolation Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Photo Database                            │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ Photos Table                                       │    │
│  │                                                    │    │
│  │  id │ path          │ categoryId │ isFromAssets  │    │
│  │ ────┼───────────────┼────────────┼──────────────  │    │
│  │  1  │ user_photo1   │     1      │   false       │◄───┼─── User Photos
│  │  2  │ user_photo2   │     2      │   false       │    │    (isFromAssets = false)
│  │  3  │ user_photo3   │     1      │   false       │    │
│  │                                                    │    │
│  │ ────┼───────────────┼────────────┼──────────────  │    │
│  │ 101 │ demo_mile_001 │     1      │   true        │◄───┼─── Demo Photos
│  │ 102 │ demo_birth_001│     2      │   true        │    │    (isFromAssets = true)
│  │ 103 │ demo_holiday_1│     3      │   true        │    │
│  │ ... │ ...           │    ...     │   true        │    │
│  │ 200 │ demo_advent_9 │     8      │   true        │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌──────────────────┐                  ┌─────────────────┐
│ isDemoMode=false │                  │ isDemoMode=true │
│                  │                  │                 │
│ Repository       │                  │ Repository      │
│ returns:         │                  │ returns:        │
│                  │                  │                 │
│ WHERE            │                  │ WHERE           │
│ isFromAssets     │                  │ isFromAssets    │
│ = false          │                  │ = true          │
│                  │                  │                 │
│ Result:          │                  │ Result:         │
│ User photos only │                  │ Demo photos only│
└──────────────────┘                  └─────────────────┘

KEY INSIGHT: User data is HIDDEN, not DELETED during demo mode
```

---

## Diagram 5: State Management - isDemoMode Flag

```
┌─────────────────────────────────────────────────────────┐
│              App State: isDemoMode                       │
└─────────────────────────────────────────────────────────┘
                         │
         ┌───────────────┴────────────────┐
         │                                │
         ▼                                ▼
    ┌────────┐                      ┌─────────┐
    │  iOS   │                      │ Android │
    └────────┘                      └─────────┘
         │                                │
         ▼                                ▼
┌─────────────────┐              ┌──────────────────┐
│ SettingsManager │              │PreferencesManager│
│                 │              │                  │
│ @AppStorage(    │              │ SharedPreferences│
│   "isDemoMode"  │              │ .getBoolean(     │
│ )               │              │  "is_demo_mode"  │
│ var isDemoMode  │              │ )                │
└────────┬────────┘              └────────┬─────────┘
         │                                │
         └────────────┬───────────────────┘
                      │
                      ▼
         ┌────────────────────────┐
         │  DemoModeManager       │
         │                        │
         │  @Published (iOS)      │
         │  StateFlow (Android)   │
         │                        │
         │  var isDemoMode: Bool  │
         └────────┬───────────────┘
                  │
                  │ Observes changes
                  │
         ┌────────┴────────┐
         │                 │
         ▼                 ▼
    ┌─────────┐      ┌──────────┐
    │   UI    │      │Repository│
    │Components│      │  Queries │
    │         │      │          │
    │ Banner  │      │ Filter   │
    │ Gallery │      │ by flag  │
    └─────────┘      └──────────┘

Persistence: Survives app restart, cleared on exit demo
```

---

## Diagram 6: Demo Asset Organization

```
iOS Asset Structure:
┌──────────────────────────────────────────┐
│ Assets.xcassets/                         │
│                                          │
│ └── DemoPhotos/                          │
│     ├── Milestones/                      │
│     │   ├── demo_first_steps.imageset   │
│     │   ├── demo_tying_shoes.imageset   │
│     │   └── ... (20 total)              │
│     │                                    │
│     ├── Birthdays/                       │
│     │   ├── demo_birthday_1.imageset    │
│     │   ├── demo_birthday_2.imageset    │
│     │   └── ... (15 total)              │
│     │                                    │
│     ├── Holidays/                        │
│     │   └── ... (18 images)             │
│     │                                    │
│     ├── Family/                          │
│     │   └── ... (12 images)             │
│     │                                    │
│     ├── Playtime/                        │
│     │   └── ... (10 images)             │
│     │                                    │
│     ├── Friends/                         │
│     │   └── ... (8 images)              │
│     │                                    │
│     ├── Creativity/                      │
│     │   └── ... (8 images)              │
│     │                                    │
│     └── Adventures/                      │
│         └── ... (9 images)              │
│                                          │
│ Total: 100 images @ ~1.5MB each         │
│ Bundle impact: ~150MB                    │
└──────────────────────────────────────────┘

Android Asset Structure:
┌──────────────────────────────────────────┐
│ res/drawable-nodpi/                      │
│                                          │
│ └── demo_photos/                         │
│     ├── milestones/                      │
│     │   ├── demo_first_steps.jpg        │
│     │   ├── demo_tying_shoes.jpg        │
│     │   └── ... (20 total)              │
│     │                                    │
│     ├── birthdays/                       │
│     │   ├── demo_birthday_1.jpg         │
│     │   ├── demo_birthday_2.jpg         │
│     │   └── ... (15 total)              │
│     │                                    │
│     ├── holidays/                        │
│     │   └── ... (18 images)             │
│     │                                    │
│     ├── family/                          │
│     │   └── ... (12 images)             │
│     │                                    │
│     ├── playtime/                        │
│     │   └── ... (10 images)             │
│     │                                    │
│     ├── friends/                         │
│     │   └── ... (8 images)              │
│     │                                    │
│     ├── creativity/                      │
│     │   └── ... (8 images)              │
│     │                                    │
│     └── adventures/                      │
│         └── ... (9 images)              │
│                                          │
│ Total: 100 images @ ~1.5MB each         │
│ APK impact: ~150MB                       │
└──────────────────────────────────────────┘
```

---

## Diagram 7: Repository Query Flow

```
User Action: Load Photos
         │
         ▼
┌─────────────────────┐
│ PhotoRepository     │
│ .getAllPhotos()     │
└────────┬────────────┘
         │
         ▼
    ┌────────────────────────┐
    │ Check isDemoMode flag  │
    └────────┬───────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
isDemoMode       isDemoMode
= true           = false
    │                 │
    ▼                 ▼
┌────────────┐   ┌────────────┐
│ SQL Query: │   │ SQL Query: │
│            │   │            │
│ SELECT *   │   │ SELECT *   │
│ FROM photos│   │ FROM photos│
│ WHERE      │   │ WHERE      │
│ isFromAsset│   │ isFromAsset│
│ s = 1      │   │ s = 0      │
└──────┬─────┘   └──────┬─────┘
       │                │
       ▼                ▼
  ┌─────────┐     ┌──────────┐
  │  Demo   │     │  User    │
  │  Photos │     │  Photos  │
  │  Only   │     │  Only    │
  └─────────┘     └──────────┘

INSERT Operation:
         │
         ▼
┌─────────────────────┐
│ PhotoRepository     │
│ .insertPhoto()      │
└────────┬────────────┘
         │
         ▼
    ┌────────────────────────┐
    │ Check isDemoMode flag  │
    └────────┬───────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
isDemoMode       isDemoMode
= true           = false
    │                 │
    ▼                 ▼
┌────────────┐   ┌────────────┐
│ Throw      │   │ INSERT     │
│ Error:     │   │ photo      │
│ "Cannot    │   │ with       │
│  modify in │   │ isFromAsset│
│  demo mode"│   │ s = 0      │
└────────────┘   └────────────┘
```

---

## Diagram 8: Platform Parity Verification Matrix

```
┌─────────────────────────────────────────────────────────────┐
│              Platform Parity Verification                    │
└─────────────────────────────────────────────────────────────┘

Feature/Component          iOS Implementation    Android Implementation
─────────────────────────  ────────────────────  ──────────────────────
Welcome Screen Button      OutlinedButton        OutlinedButton
                          "Try Demo"            "Try Demo"
                          Blue border           Blue border

Demo Mode Manager         DemoModeManager       DemoModeManager
                         .swift                .kt
                         @Published            StateFlow

Settings Flag            @AppStorage           SharedPreferences
                        isDemoMode            isDemoMode

Demo Banner             DemoModeBanner.swift   DemoModeBanner.kt
                       SwiftUI View           @Composable
                       Purple #E8E0F5         Purple #E8E0F5

Exit Dialog            ExitDemoDialog.swift    ExitDemoDialog.kt
                      .sheet()                AlertDialog()
                      2 buttons               2 buttons

Photo Model Flag      isFromAssets: Bool      isFromAssets: Boolean
                     (existing)              (existing)

Repository Filtering  .filter { $0.is...}    .filter { it.is... }
                     (Swift)                 (Kotlin)

Demo Data            DemoData.swift          DemoData.kt
                    100 photos               100 photos
                    8 categories             8 categories

Assets Location     Assets.xcassets/         res/drawable-nodpi/
                   DemoPhotos/              demo_photos/

┌────────────────────────────────────────────────────────────┐
│ Verification Checklist:                                    │
│ ☐ UI elements match (position, size, color)               │
│ ☐ Functionality identical (same features)                 │
│ ☐ Data structures equivalent (same fields)                │
│ ☐ User experience consistent (same flow)                  │
│ ☐ Performance targets same (same benchmarks)              │
│ ☐ Error messages identical (same wording)                 │
└────────────────────────────────────────────────────────────┘
```

---

## Diagram 9: Error Handling Flow

```
┌──────────────────────────┐
│  Demo Mode Entry         │
│  Attempt                 │
└────────┬─────────────────┘
         │
         ▼
    ┌────────────────┐
    │ Try loading    │
    │ demo data      │
    └────┬───────────┘
         │
         ├──── Success ───────┐
         │                    │
         │                    ▼
         │           ┌────────────────┐
         │           │ Navigate to    │
         │           │ demo gallery   │
         │           └────────────────┘
         │
         └──── Failure ──────┐
                             │
                             ▼
                    ┌─────────────────────────┐
                    │ Error Detection         │
                    │                         │
                    │ - Asset not found?      │
                    │ - Database error?       │
                    │ - Insufficient space?   │
                    │ - Corruption?           │
                    └────────┬────────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                    ▼                 ▼
           ┌──────────────┐   ┌──────────────┐
           │ Recoverable  │   │ Fatal Error  │
           │ Error        │   │              │
           └──────┬───────┘   └──────┬───────┘
                  │                  │
                  ▼                  ▼
         ┌─────────────────┐  ┌──────────────────┐
         │ Show Dialog:    │  │ Show Dialog:     │
         │ "Failed to load │  │ "Demo mode error"│
         │  demo mode"     │  │ "Contact support"│
         │                 │  │                  │
         │ [Retry] [Exit]  │  │ [Exit Demo]      │
         └────┬────────┬───┘  └────────┬─────────┘
              │        │               │
              ▼        ▼               ▼
         ┌────────┐ ┌────────┐  ┌────────────┐
         │ Retry  │ │ Exit   │  │ Exit to    │
         │ Load   │ │ to     │  │ Welcome    │
         │        │ │Welcome │  │ + Log Error│
         └────────┘ └────────┘  └────────────┘
```

---

## Diagram 10: Performance Optimization Strategy

```
Demo Mode Entry Performance Breakdown:
Target: < 2000ms

┌──────────────────────────────────────────────────────┐
│                                                      │
│ User Taps "Try Demo"                                 │
│         │                                            │
│         ├─► Show Loading Indicator (instant)         │
│         │                                            │
│         ├─► Phase 1: Load Metadata (100ms)          │
│         │   └─ Read DemoData.swift/.kt               │
│         │      - 8 categories                        │
│         │      - 100 photo metadata entries          │
│         │      - In-memory, very fast                │
│         │                                            │
│         ├─► Phase 2: Priority Assets (800ms)        │
│         │   └─ Load first 20 photos                  │
│         │      - Enough for initial gallery view     │
│         │      - Parallel loading (5 threads)        │
│         │      - Cached for instant display          │
│         │                                            │
│         ├─► Phase 3: Database Insert (300ms)        │
│         │   └─ Batch insert categories + photos      │
│         │      - Transaction for speed               │
│         │      - Indexed for fast retrieval          │
│         │                                            │
│         ├─► Phase 4: Navigate (100ms)               │
│         │   └─ Transition to gallery                 │
│         │      - SwiftUI/Compose animation           │
│         │                                            │
│         └─► Phase 5: Background Load (async)        │
│             └─ Load remaining 80 photos              │
│                - Low priority                        │
│                - User can browse while loading       │
│                - Progressive enhancement             │
│                                                      │
│ Total Perceived Time: ~1300ms                        │
│ (user sees gallery with 20 photos)                   │
│                                                      │
│ Total Complete Time: ~5000ms                         │
│ (all 100 photos loaded)                              │
│                                                      │
│ ✓ Meets <2s target for initial experience           │
└──────────────────────────────────────────────────────┘

Memory Optimization:
┌─────────────────────────────────────┐
│ Image Cache Strategy                │
│                                     │
│ ┌─────────┐  ┌─────────┐           │
│ │ Memory  │  │  Disk   │           │
│ │ Cache   │  │  Cache  │           │
│ │ 50MB    │  │ 150MB   │           │
│ │ (recent)│  │ (all)   │           │
│ └────┬────┘  └────┬────┘           │
│      │            │                │
│      └──────┬─────┘                │
│             │                      │
│      ┌──────▼──────┐               │
│      │ LRU Eviction│               │
│      │ Strategy    │               │
│      └─────────────┘               │
└─────────────────────────────────────┘
```

---

## Quick Reference: Key File Paths

### iOS
```
ios/SmilePile/
├── DemoMode/
│   ├── DemoModeManager.swift          [NEW]
│   ├── DemoData.swift                 [NEW]
│   ├── DemoModeBanner.swift           [NEW]
│   └── ExitDemoDialog.swift           [NEW]
├── Assets.xcassets/
│   └── DemoPhotos/                    [NEW - 100 images]
├── Onboarding/
│   ├── Screens/WelcomeScreen.swift    [MODIFY]
│   └── OnboardingCoordinator.swift    [MODIFY]
├── Settings/SettingsManager.swift     [MODIFY]
├── Views/
│   ├── ContentView.swift              [MODIFY]
│   └── SettingsViewCustom.swift       [MODIFY]
└── Data/Repositories/
    ├── PhotoRepositoryImpl.swift      [MODIFY]
    └── CategoryRepositoryImpl.swift   [MODIFY]
```

### Android
```
android/app/src/main/
├── java/com/smilepile/
│   ├── demomode/
│   │   ├── DemoModeManager.kt         [NEW]
│   │   ├── DemoData.kt                [NEW]
│   │   ├── DemoModeBanner.kt          [NEW]
│   │   └── ExitDemoDialog.kt          [NEW]
│   ├── onboarding/
│   │   ├── screens/WelcomeScreen.kt   [MODIFY]
│   │   └── OnboardingViewModel.kt     [MODIFY]
│   ├── utils/PreferencesManager.kt    [MODIFY]
│   ├── ui/
│   │   ├── MainActivity.kt            [MODIFY]
│   │   └── screens/SettingsScreen.kt  [MODIFY]
│   └── data/repository/
│       ├── PhotoRepositoryImpl.kt     [MODIFY]
│       └── CategoryRepositoryImpl.kt  [MODIFY]
└── res/
    └── drawable-nodpi/
        └── demo_photos/                [NEW - 100 images]
```

---

**End of Architecture Diagrams**

These visual aids complement the detailed technical planning document and provide quick reference for the implementation phase.
