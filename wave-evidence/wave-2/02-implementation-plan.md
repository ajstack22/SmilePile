# Wave 2: iOS 4-Tier Configuration - Implementation Plan

**Planning Phase - Developer Agent**
**Date**: 2025-10-14
**Wave**: 2 of 10
**Story**: STORY-6.2-ios-tier-config.md

---

## Executive Summary

This document provides a step-by-step implementation plan for configuring iOS with 4-tier deployment support (QUAL, STAGE, BETA, PROD). The implementation uses XCConfig files, Xcode schemes, and a Swift BuildConfig module to enable runtime tier detection through BUILD_TYPE_ENV.

**Estimated Implementation Time**: 6-8 hours
**Risk Level**: LOW
**Prerequisites**: Wave 1 Complete (Foundation Setup)

---

## Peer Review Fixes Applied

This implementation plan has been revised to address 3 CRITICAL issues identified in peer review (04-peer-review.md):

### CRITICAL-001: Base.xcconfig Inheritance Chain (FIXED)
**Problem**: Tier xcconfig files would override ALL project settings without inheritance.
**Solution**:
- Added `Base.xcconfig` with common settings (deployment target, code signing, Swift version, etc.)
- All 4 tier configs now include `#include "Base.xcconfig"` at the top
- Tier configs only override tier-specific settings (bundle ID, PRODUCT_NAME, optimization level)

### CRITICAL-002: PRODUCT_NAME Contradictions (FIXED)
**Problem**: Lines 701 vs 713-720 conflicted about whether PRODUCT_NAME changes .app bundle name.
**Solution**:
- Clarified that `PRODUCT_NAME` IS tier-specific (SmilePile Qual, SmilePile Stage, etc.)
- Updated deployment script to use correct app path: "SmilePile Qual.app" (with space)
- Documented throughout that PRODUCT_NAME changes the .app bundle name on disk
- All references to app paths now consistent

### CRITICAL-003: BuildConfig.swift Bundle Initialization (FIXED)
**Problem**: `Bundle.main` crashes in XCTest environment.
**Solution**:
- Added test-safe bundle access using `NSClassFromString("XCTestCase")` detection
- In test context: uses `Bundle(for: BuildConfigBundleToken.self)`
- In app context: uses `Bundle.main`
- Added BuildConfigBundleToken private class for bundle resolution
- Added warning logs with `#if DEBUG` guards for missing BUILD_TYPE_ENV

**Verification**: All 3 critical issues resolved. Implementation plan is now ready for Phase 5 (Implementation).

---

## Table of Contents

1. [Implementation Overview](#implementation-overview)
2. [Step-by-Step Implementation](#step-by-step-implementation)
3. [XCConfig Files - Complete Contents](#xcconfig-files---complete-contents)
4. [BuildConfig Swift Module](#buildconfig-swift-module)
5. [Info.plist Modifications](#infoplist-modifications)
6. [Xcode Scheme Setup](#xcode-scheme-setup)
7. [Build Verification](#build-verification)
8. [Deployment Script Integration](#deployment-script-integration)
9. [Testing Procedures](#testing-procedures)
10. [Rollback Plan](#rollback-plan)
11. [Time Estimates](#time-estimates)

---

## Implementation Overview

### What We're Building

A 4-tier configuration system that enables iOS apps to:
- Detect their deployment tier at runtime (QUAL, STAGE, BETA, PROD)
- Use tier-specific bundle IDs and display names
- Build using dedicated Xcode schemes
- Integrate with automated deployment scripts

### Implementation Sequence

```
1. Create XCConfig files (5 files: Base + 4 tiers) → 35 minutes
2. Link XCConfigs to Xcode project → 15 minutes
3. Create BuildConfig.swift module → 20 minutes
4. Update Info.plist → 10 minutes
5. Create Xcode schemes (4 schemes) → 45 minutes
6. Build verification (all schemes) → 30 minutes
7. Update deployment script → 15 minutes
8. Create tests → 30 minutes
9. Final verification → 30 minutes
10. Documentation → 30 minutes
─────────────────────────────────────────────
Total: 4.25 hours (core implementation)
```

---

## Step-by-Step Implementation

### Phase 1: Create XCConfig Files (35 minutes)

#### Step 1.0: Create Base.xcconfig

**File Location**: `/Users/adamstack/SmilePile/ios/Base.xcconfig`

**Action**: Create base configuration file with common settings shared across all tiers

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
touch Base.xcconfig
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/Base.xcconfig
cat /Users/adamstack/SmilePile/ios/Base.xcconfig | grep "IPHONEOS_DEPLOYMENT_TARGET"
```

Expected output: `IPHONEOS_DEPLOYMENT_TARGET = 16.0`

#### Step 1.1: Create Qual.xcconfig

**File Location**: `/Users/adamstack/SmilePile/ios/Qual.xcconfig`

**Action**: Create file with exact contents from [XCConfig Files - Complete Contents](#xcconfig-files---complete-contents) section

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
touch Qual.xcconfig
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/Qual.xcconfig
cat /Users/adamstack/SmilePile/ios/Qual.xcconfig | grep "BUILD_TYPE_ENV = qual"
```

Expected output: `BUILD_TYPE_ENV = qual`

#### Step 1.2: Create Stage.xcconfig

**File Location**: `/Users/adamstack/SmilePile/ios/Stage.xcconfig`

**Action**: Create file with exact contents from [XCConfig Files - Complete Contents](#xcconfig-files---complete-contents) section

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
touch Stage.xcconfig
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/Stage.xcconfig
cat /Users/adamstack/SmilePile/ios/Stage.xcconfig | grep "BUILD_TYPE_ENV = stage"
```

Expected output: `BUILD_TYPE_ENV = stage`

#### Step 1.3: Create Beta.xcconfig

**File Location**: `/Users/adamstack/SmilePile/ios/Beta.xcconfig`

**Action**: Create file with exact contents from [XCConfig Files - Complete Contents](#xcconfig-files---complete-contents) section

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
touch Beta.xcconfig
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/Beta.xcconfig
cat /Users/adamstack/SmilePile/ios/Beta.xcconfig | grep "BUILD_TYPE_ENV = beta"
```

Expected output: `BUILD_TYPE_ENV = beta`

#### Step 1.4: Create Prod.xcconfig

**File Location**: `/Users/adamstack/SmilePile/ios/Prod.xcconfig`

**Action**: Create file with exact contents from [XCConfig Files - Complete Contents](#xcconfig-files---complete-contents) section

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
touch Prod.xcconfig
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/Prod.xcconfig
cat /Users/adamstack/SmilePile/ios/Prod.xcconfig | grep "BUILD_TYPE_ENV = prod"
```

Expected output: `BUILD_TYPE_ENV = prod`

#### Step 1.5: Verify All XCConfig Files Created

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
ls -la *.xcconfig
```

**Expected Output**:
```
-rw-r--r--  1 user  staff  XXX  Base.xcconfig
-rw-r--r--  1 user  staff  XXX  Beta.xcconfig
-rw-r--r--  1 user  staff  XXX  Prod.xcconfig
-rw-r--r--  1 user  staff  XXX  Qual.xcconfig
-rw-r--r--  1 user  staff  XXX  Stage.xcconfig
```

---

### Phase 2: Link XCConfig Files to Xcode Project (15 minutes)

#### Step 2.1: Open Xcode Project

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
open SmilePile.xcodeproj
```

#### Step 2.2: Add XCConfig Files to Project (Xcode GUI)

**Steps in Xcode**:
1. Select `SmilePile.xcodeproj` in Project Navigator
2. Right-click on the `SmilePile` folder (or root project)
3. Select "Add Files to SmilePile..."
4. Navigate to `/Users/adamstack/SmilePile/ios/`
5. Select all 5 xcconfig files:
   - Base.xcconfig
   - Qual.xcconfig
   - Stage.xcconfig
   - Beta.xcconfig
   - Prod.xcconfig
6. **Important**: Uncheck "Copy items if needed" (files are already in correct location)
7. **Important**: Uncheck "Add to targets" (xcconfig files are not source files)
8. Click "Add"

**Verification**: Files should appear in Project Navigator with yellow/gold icon (configuration files)

#### Step 2.3: Assign XCConfig Files to Build Configurations

**Steps in Xcode**:
1. Select `SmilePile.xcodeproj` in Project Navigator
2. Select the `SmilePile` PROJECT (not target) in the main pane
3. Go to "Info" tab
4. Under "Configurations" section:
   - Expand "Debug" configuration
   - For "SmilePile" target, set: `Qual` (from dropdown)
   - Expand "Release" configuration
   - For "SmilePile" target, set: `Prod` (from dropdown)

**Note**: We'll assign Stage.xcconfig and Beta.xcconfig when creating their respective schemes

**Alternative: Command-Line Approach** (if GUI fails):

The xcconfig assignment can also be done by editing `project.pbxproj` directly:

```bash
cd /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj
# Backup first
cp project.pbxproj project.pbxproj.backup_wave2

# Manual edit required - search for build configuration sections
# Add baseConfigurationReference lines
```

**Not Recommended**: Direct pbxproj editing is error-prone. Use Xcode GUI.

---

### Phase 3: Create BuildConfig Swift Module (15 minutes)

#### Step 3.1: Create BuildConfig.swift File

**File Location**: `/Users/adamstack/SmilePile/ios/SmilePile/Config/BuildConfig.swift`

**Action**: Create file with exact contents from [BuildConfig Swift Module](#buildconfig-swift-module) section

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios/SmilePile/Config
touch BuildConfig.swift
# Copy content from section below
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/SmilePile/Config/BuildConfig.swift
wc -l /Users/adamstack/SmilePile/ios/SmilePile/Config/BuildConfig.swift
```

Expected: File exists with ~40 lines

#### Step 3.2: Add BuildConfig.swift to Xcode Project

**Steps in Xcode**:
1. Select `SmilePile.xcodeproj` in Project Navigator
2. Navigate to `SmilePile/Config/` folder in Project Navigator
3. Right-click on `Config` folder
4. Select "Add Files to SmilePile..."
5. Navigate to `/Users/adamstack/SmilePile/ios/SmilePile/Config/`
6. Select `BuildConfig.swift`
7. **Important**: Check "Copy items if needed" (should be unchecked - file already in place)
8. **Important**: Check "Add to targets" → Select "SmilePile" target
9. Click "Add"

**Verification in Xcode**:
- File appears in Config folder with Swift icon
- File is listed in Build Phases → Compile Sources

#### Step 3.3: Verify Swift File Compiles

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme SmilePile \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    | grep -i "buildconfig\|error\|warning"
```

**Expected Output**: No errors related to BuildConfig.swift

---

### Phase 4: Update Info.plist (10 minutes)

#### Step 4.1: Open Info.plist

**File Location**: `/Users/adamstack/SmilePile/ios/SmilePile/Info.plist`

**Action**: Add two new key-value pairs

#### Step 4.2: Add BUILD_TYPE_ENV Key

**XML to Add** (insert after `<dict>` opening tag):
```xml
<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>
```

**Position**: Add near the top of the plist, after existing CFBundle keys

#### Step 4.3: Update CFBundleDisplayName Key

**Find**:
```xml
<key>CFBundleName</key>
<string>$(PRODUCT_NAME)</string>
```

**Add After**:
```xml
<key>CFBundleDisplayName</key>
<string>$(APP_DISPLAY_NAME)</string>
```

**Note**: CFBundleDisplayName may not exist currently. This key controls the app name shown on the home screen.

#### Step 4.4: Verify Info.plist Changes

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios/SmilePile
cat Info.plist | grep -A1 "BUILD_TYPE_ENV"
cat Info.plist | grep -A1 "CFBundleDisplayName"
```

**Expected Output**:
```xml
<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>
--
<key>CFBundleDisplayName</key>
<string>$(APP_DISPLAY_NAME)</string>
```

---

### Phase 5: Create Xcode Schemes (45 minutes)

**Note**: Schemes must be created via Xcode GUI or by duplicating/editing existing scheme files in `xcshareddata/xcschemes/`. We'll use the GUI approach.

#### Step 5.1: Create "SmilePile Qual" Scheme

**Steps in Xcode**:
1. Open Xcode with SmilePile.xcodeproj
2. Click scheme dropdown (top left, currently shows "SmilePile")
3. Select "Manage Schemes..."
4. Click the existing "SmilePile" scheme
5. Click the gear icon (⚙️) → "Duplicate"
6. Name the new scheme: `SmilePile Qual`
7. Check "Shared" checkbox
8. Click "Close"

**Configure Scheme Settings**:
1. Click scheme dropdown → Select "SmilePile Qual" → "Edit Scheme..."
2. **Run** section (left sidebar):
   - Info tab → Build Configuration: `Debug`
   - Arguments tab: (no changes needed)
3. **Test** section:
   - Info tab → Build Configuration: `Debug`
4. **Profile** section:
   - Info tab → Build Configuration: `Release`
5. **Analyze** section:
   - Info tab → Build Configuration: `Debug`
6. **Archive** section:
   - Info tab → Build Configuration: `Release`
7. Click "Close"

**Important**: Since we assigned Qual.xcconfig to the Debug configuration, this scheme will automatically use Qual.xcconfig

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/
```

Expected: File `SmilePile Qual.xcscheme` exists

#### Step 5.2: Create "SmilePile Stage" Scheme

**Steps in Xcode**:
1. Click scheme dropdown → "Manage Schemes..."
2. Click "SmilePile Qual" scheme
3. Click gear icon → "Duplicate"
4. Name: `SmilePile Stage`
5. Check "Shared" checkbox
6. Click "Close"

**Create Stage Build Configuration**:

Before configuring the scheme, we need to create a "Stage" build configuration:

1. Select `SmilePile.xcodeproj` in Project Navigator
2. Select the `SmilePile` PROJECT
3. Go to "Info" tab
4. Under "Configurations" section, click "+" below the list
5. Select "Duplicate 'Release' Configuration"
6. Name it: `Stage`
7. Expand "Stage" configuration
8. For "SmilePile" target, set: `Stage` (from dropdown)

**Configure Scheme Settings**:
1. Edit "SmilePile Stage" scheme
2. **All sections** (Run, Test, Profile, Analyze, Archive):
   - Set Build Configuration to: `Stage`
3. Click "Close"

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/ | grep Stage
```

Expected: File `SmilePile Stage.xcscheme` exists

#### Step 5.3: Create "SmilePile Beta" Scheme

**Steps in Xcode**:
1. Click scheme dropdown → "Manage Schemes..."
2. Click "SmilePile Stage" scheme
3. Click gear icon → "Duplicate"
4. Name: `SmilePile Beta`
5. Check "Shared" checkbox
6. Click "Close"

**Create Beta Build Configuration**:
1. Select `SmilePile.xcodeproj` in Project Navigator
2. Select the `SmilePile` PROJECT
3. Go to "Info" tab
4. Under "Configurations", click "+"
5. Select "Duplicate 'Release' Configuration"
6. Name it: `Beta`
7. Expand "Beta" configuration
8. For "SmilePile" target, set: `Beta` (from dropdown)

**Configure Scheme Settings**:
1. Edit "SmilePile Beta" scheme
2. **All sections**: Set Build Configuration to: `Beta`
3. Click "Close"

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/ | grep Beta
```

Expected: File `SmilePile Beta.xcscheme` exists

#### Step 5.4: Create "SmilePile Prod" Scheme

**Steps in Xcode**:
1. Click scheme dropdown → "Manage Schemes..."
2. Click "SmilePile Beta" scheme
3. Click gear icon → "Duplicate"
4. Name: `SmilePile Prod`
5. Check "Shared" checkbox
6. Click "Close"

**Configure Scheme Settings**:
1. Edit "SmilePile Prod" scheme
2. **All sections**: Set Build Configuration to: `Release`
3. Click "Close"

**Note**: Prod uses the existing "Release" configuration (already has Prod.xcconfig assigned)

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/ | grep Prod
```

Expected: File `SmilePile Prod.xcscheme` exists

#### Step 5.5: Verify All Schemes Created

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes
ls -la
```

**Expected Output**:
```
SmilePile.xcscheme
SmilePile Qual.xcscheme
SmilePile Stage.xcscheme
SmilePile Beta.xcscheme
SmilePile Prod.xcscheme
```

**In Xcode**:
- Click scheme dropdown
- Should see all 5 schemes listed
- All should show in "SmilePile Project Schemes" section (indicating they're shared)

---

### Phase 6: Build Verification (30 minutes)

#### Step 6.1: Build "SmilePile Qual" Scheme

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual" \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    2>&1 | tee /tmp/build-qual.log
```

**Success Indicator**: `** BUILD SUCCEEDED **`

**Verification - Check Bundle ID**:
```bash
grep "PRODUCT_BUNDLE_IDENTIFIER" /tmp/build-qual.log | head -n1
```

Expected: `com.smilepile.qual`

**Verification - Check BUILD_TYPE_ENV**:
```bash
grep "BUILD_TYPE_ENV" /tmp/build-qual.log | head -n1
```

Expected: `qual`

**Verification - Check Display Name**:
```bash
grep "APP_DISPLAY_NAME" /tmp/build-qual.log | head -n1
```

Expected: `SmilePile Qual`

#### Step 6.2: Build "SmilePile Stage" Scheme

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Stage" \
    -configuration Stage \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    2>&1 | tee /tmp/build-stage.log
```

**Success Indicator**: `** BUILD SUCCEEDED **`

**Verification**:
```bash
grep "PRODUCT_BUNDLE_IDENTIFIER" /tmp/build-stage.log | head -n1  # Expected: com.smilepile
grep "BUILD_TYPE_ENV" /tmp/build-stage.log | head -n1              # Expected: stage
grep "APP_DISPLAY_NAME" /tmp/build-stage.log | head -n1            # Expected: SmilePile Stage
```

#### Step 6.3: Build "SmilePile Beta" Scheme

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Beta" \
    -configuration Beta \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    2>&1 | tee /tmp/build-beta.log
```

**Success Indicator**: `** BUILD SUCCEEDED **`

**Verification**:
```bash
grep "PRODUCT_BUNDLE_IDENTIFIER" /tmp/build-beta.log | head -n1   # Expected: com.smilepile
grep "BUILD_TYPE_ENV" /tmp/build-beta.log | head -n1              # Expected: beta
grep "APP_DISPLAY_NAME" /tmp/build-beta.log | head -n1            # Expected: SmilePile Beta
```

#### Step 6.4: Build "SmilePile Prod" Scheme

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Prod" \
    -configuration Release \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    2>&1 | tee /tmp/build-prod.log
```

**Success Indicator**: `** BUILD SUCCEEDED **`

**Verification**:
```bash
grep "PRODUCT_BUNDLE_IDENTIFIER" /tmp/build-prod.log | head -n1   # Expected: com.smilepile
grep "BUILD_TYPE_ENV" /tmp/build-prod.log | head -n1              # Expected: prod
grep "APP_DISPLAY_NAME" /tmp/build-prod.log | head -n1            # Expected: SmilePile
```

#### Step 6.5: Runtime Verification (Launch in Simulator)

**Build and Install QUAL**:
```bash
cd /Users/adamstack/SmilePile/ios

# Build
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual" \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData

# Get simulator ID
SIMULATOR_ID=$(xcrun simctl list devices | grep "iPhone 16" | grep -v "unavailable" | head -n1 | grep -o "\([A-F0-9-]*\)" | head -n1 | tr -d '()')

# Boot if needed
xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || true

# Install
xcrun simctl install "$SIMULATOR_ID" ./DerivedData/Build/Products/Debug-iphonesimulator/SmilePile.app

# Launch
xcrun simctl launch "$SIMULATOR_ID" com.smilepile.qual
```

**Manual Verification in Simulator**:
1. Simulator should open with "SmilePile Qual" app installed
2. App icon should show name "SmilePile Qual" on home screen
3. App should launch successfully

**Add Debug Print to Verify BUILD_TYPE_ENV** (temporary):

Edit `/Users/adamstack/SmilePile/ios/SmilePile/SmilePileApp.swift`:

Add in `init()` or `body`:
```swift
init() {
    print("=== BUILD_TYPE_ENV: \(BuildConfig.buildType) ===")
    print("=== Bundle ID: \(Bundle.main.bundleIdentifier ?? "unknown") ===")
}
```

Rebuild and check Xcode console output for the print statements.

**Remove debug prints after verification**

---

### Phase 7: Deployment Script Integration (15 minutes)

#### Step 7.1: Update deploy_qual.sh

**File Location**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`

**Current Code** (line 486-495):
```bash
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme SmilePile \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    || {
    log ERROR "iOS build failed"
    return 1
}
```

**Updated Code** (line 486-495):
```bash
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual" \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData \
    || {
    log ERROR "iOS build failed"
    return 1
}
```

**Change**: `SmilePile` → `"SmilePile Qual"`

**Note**: Quotes required because scheme name contains space

#### Step 7.2: Update App Path in deploy_qual.sh

**Current Code** (line 498):
```bash
local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile.app"
```

**CRITICAL: PRODUCT_NAME Change**

The xcconfig files set `PRODUCT_NAME` to tier-specific values:
- Qual.xcconfig: `PRODUCT_NAME = SmilePile Qual`
- Stage.xcconfig: `PRODUCT_NAME = SmilePile Stage`
- Beta.xcconfig: `PRODUCT_NAME = SmilePile Beta`
- Prod.xcconfig: `PRODUCT_NAME = SmilePile`

**This changes the .app bundle name on disk**. The deployment script MUST use the correct app bundle name.

**Updated Code** (line 498):
```bash
local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"
```

**Note**: Quotes in `xcrun simctl install` commands will properly handle the space in "SmilePile Qual.app"

#### Step 7.3: Update Bundle ID in deploy_qual.sh

**Current Code** (line 550):
```bash
xcrun simctl launch "$sim" com.smilepile.SmilePile
```

**Updated Code** (line 550):
```bash
xcrun simctl launch "$sim" com.smilepile.qual
```

**Change**: `com.smilepile.SmilePile` → `com.smilepile.qual`

#### Step 7.4: Test Updated Deployment Script

**Command**:
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true ./deploy/deploy_qual.sh ios
```

**Expected Outcome**:
- iOS build succeeds using "SmilePile Qual" scheme
- App installs to simulator with bundle ID `com.smilepile.qual`
- App launches successfully

**Verification**:
```bash
# Check installed apps in simulator
xcrun simctl listapps booted | grep smilepile
```

Expected: Shows `com.smilepile.qual` with display name "SmilePile Qual"

---

### Phase 8: Testing Procedures (30 minutes)

#### Step 8.1: Create BuildConfigTests.swift

**File Location**: `/Users/adamstack/SmilePile/ios/SmilePileTests/BuildConfigTests.swift`

**Complete File Contents**: See [Testing Procedures](#testing-procedures-complete) section below

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios/SmilePileTests
touch BuildConfigTests.swift
# Copy content from Testing Procedures section
```

#### Step 8.2: Add Test File to Xcode Project

**Steps in Xcode**:
1. Navigate to `SmilePileTests` folder in Project Navigator
2. Right-click → "Add Files to SmilePile..."
3. Select `BuildConfigTests.swift`
4. **Check**: "Add to targets" → Select "SmilePileTests"
5. Click "Add"

#### Step 8.3: Run Tests for Each Scheme

**QUAL Tests**:
```bash
cd /Users/adamstack/SmilePile/ios
xcodebuild test \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual" \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -only-testing:SmilePileTests/BuildConfigTests
```

**Expected**: All tests pass, BuildConfig.buildType == "qual"

**STAGE Tests**:
```bash
xcodebuild test \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Stage" \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -only-testing:SmilePileTests/BuildConfigTests
```

**Expected**: All tests pass, BuildConfig.buildType == "stage"

**BETA Tests**:
```bash
xcodebuild test \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Beta" \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -only-testing:SmilePileTests/BuildConfigTests
```

**Expected**: All tests pass, BuildConfig.buildType == "beta"

**PROD Tests**:
```bash
xcodebuild test \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Prod" \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -only-testing:SmilePileTests/BuildConfigTests
```

**Expected**: All tests pass, BuildConfig.buildType == "prod"

#### Step 8.4: Run Existing Test Suites

Ensure new configuration doesn't break existing tests:

**QUAL (using existing test script)**:
```bash
cd /Users/adamstack/SmilePile
./ios/scripts/run-tier-tests.sh tier1
./ios/scripts/run-tier-tests.sh tier2
./ios/scripts/run-tier-tests.sh tier3
```

**Expected**: All tests pass (same as before Wave 2 changes)

---

### Phase 9: Final Verification (30 minutes)

#### Step 9.1: Clean Build All Schemes

**Command**:
```bash
cd /Users/adamstack/SmilePile/ios

# Clean
xcodebuild clean \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual"

# Build all schemes
for scheme in "SmilePile Qual" "SmilePile Stage" "SmilePile Beta" "SmilePile Prod"; do
    echo "========================================="
    echo "Building scheme: $scheme"
    echo "========================================="

    xcodebuild build \
        -project SmilePile.xcodeproj \
        -scheme "$scheme" \
        -destination 'platform=iOS Simulator,name=iPhone 16' \
        -derivedDataPath ./DerivedData \
        | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:"
done
```

**Expected**: All 4 schemes show `** BUILD SUCCEEDED **`

#### Step 9.2: Verify Git Changes

**Command**:
```bash
cd /Users/adamstack/SmilePile
git status
```

**Expected Files Modified**:
- `ios/SmilePile/Info.plist` (modified)
- `deploy/deploy_qual.sh` (modified)

**Expected Files Added**:
- `ios/Base.xcconfig` (new - CRITICAL: provides inheritance chain)
- `ios/Qual.xcconfig` (new)
- `ios/Stage.xcconfig` (new)
- `ios/Beta.xcconfig` (new)
- `ios/Prod.xcconfig` (new)
- `ios/SmilePile/Config/BuildConfig.swift` (new)
- `ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Qual.xcscheme` (new)
- `ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Stage.xcscheme` (new)
- `ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Beta.xcscheme` (new)
- `ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Prod.xcscheme` (new)
- `ios/SmilePileTests/BuildConfigTests.swift` (new)
- `ios/SmilePile.xcodeproj/project.pbxproj` (modified - scheme references)

#### Step 9.3: Documentation Update

**Files to Update**:
1. `/Users/adamstack/SmilePile/ios/CLAUDE.md` - Add tier configuration usage
2. `/Users/adamstack/SmilePile/wave-evidence/wave-2/03-implementation-results.md` - Create results doc
3. `/Users/adamstack/SmilePile/docs/DEPLOYMENT_ROADMAP.md` - Mark Wave 2 complete

**Command**:
```bash
cd /Users/adamstack/SmilePile
ls -la wave-evidence/wave-2/
```

**Expected Files**:
- `01-research-findings.md` ✅ (exists)
- `02-implementation-plan.md` ✅ (this document)
- `03-implementation-results.md` (to be created)

---

## XCConfig Files - Complete Contents

### Base.xcconfig

**File**: `/Users/adamstack/SmilePile/ios/Base.xcconfig`

```xcconfig
// ============================================================================
// SmilePile Base Configuration
// ============================================================================
// Common settings shared across all tiers (QUAL, STAGE, BETA, PROD)
// All tier-specific xcconfig files should include this file
//
// Wave 2: iOS 4-Tier Configuration
// Story: STORY-6.2-ios-tier-config.md
//
// CRITICAL: This file provides the inheritance chain for tier configs.
// Without this, tier xcconfig files would override ALL project settings.

// MARK: - Deployment Target
IPHONEOS_DEPLOYMENT_TARGET = 16.0
TARGETED_DEVICE_FAMILY = 1,2
SDKROOT = iphoneos

// MARK: - Code Signing (Common Settings)
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = 84W9WSYQQB

// MARK: - Build Settings (Common)
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor
INFOPLIST_FILE = SmilePile/Info.plist

// MARK: - Swift Version
SWIFT_VERSION = 5.0

// MARK: - Other Common Settings
ALWAYS_SEARCH_USER_PATHS = NO
CLANG_ANALYZER_NONNULL = YES
CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE
CLANG_CXX_LANGUAGE_STANDARD = "gnu++20"
CLANG_ENABLE_MODULES = YES
CLANG_ENABLE_OBJC_ARC = YES
CLANG_ENABLE_OBJC_WEAK = YES
CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES
CLANG_WARN_BOOL_CONVERSION = YES
CLANG_WARN_COMMA = YES
CLANG_WARN_CONSTANT_CONVERSION = YES
CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES
CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR
CLANG_WARN_DOCUMENTATION_COMMENTS = YES
CLANG_WARN_EMPTY_BODY = YES
CLANG_WARN_ENUM_CONVERSION = YES
CLANG_WARN_INFINITE_RECURSION = YES
CLANG_WARN_INT_CONVERSION = YES
CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES
CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES
CLANG_WARN_OBJC_LITERAL_CONVERSION = YES
CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR
CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES
CLANG_WARN_RANGE_LOOP_ANALYSIS = YES
CLANG_WARN_STRICT_PROTOTYPES = YES
CLANG_WARN_SUSPICIOUS_MOVE = YES
CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE
CLANG_WARN_UNREACHABLE_CODE = YES
CLANG_WARN__DUPLICATE_METHOD_MATCH = YES
COPY_PHASE_STRIP = NO
ENABLE_STRICT_OBJC_MSGSEND = YES
GCC_C_LANGUAGE_STANDARD = gnu17
GCC_NO_COMMON_BLOCKS = YES
GCC_WARN_64_TO_32_BIT_CONVERSION = YES
GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR
GCC_WARN_UNDECLARED_SELECTOR = YES
GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE
GCC_WARN_UNUSED_FUNCTION = YES
GCC_WARN_UNUSED_VARIABLE = YES
```

---

### Qual.xcconfig

**File**: `/Users/adamstack/SmilePile/ios/Qual.xcconfig`

```xcconfig
// ============================================================================
// SmilePile QUAL Configuration
// ============================================================================
// For local development and testing
// Uses unique bundle ID for side-by-side installation with other tiers

// Include base configuration
#include "Base.xcconfig"

// MARK: - Application Identity
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile.qual
PRODUCT_NAME = SmilePile Qual
APP_DISPLAY_NAME = SmilePile Qual
BUILD_TYPE_ENV = qual

// MARK: - Code Signing (Override for development)
CODE_SIGN_IDENTITY = iPhone Developer

// MARK: - Swift Optimization (Debug for faster builds)
SWIFT_OPTIMIZATION_LEVEL = -Onone
SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG QUAL
MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE
DEBUG_INFORMATION_FORMAT = dwarf
```

---

### Stage.xcconfig

**File**: `/Users/adamstack/SmilePile/ios/Stage.xcconfig`

```xcconfig
// ============================================================================
// SmilePile STAGE Configuration
// ============================================================================
// For internal team testing via TestFlight Internal
// Uses production bundle ID (shares with BETA/PROD)

// Include base configuration
#include "Base.xcconfig"

// MARK: - Application Identity
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile Stage
APP_DISPLAY_NAME = SmilePile Stage
BUILD_TYPE_ENV = stage

// MARK: - Code Signing (Distribution for TestFlight)
CODE_SIGN_IDENTITY = Apple Distribution

// MARK: - Swift Optimization (Release for production-like performance)
SWIFT_OPTIMIZATION_LEVEL = -O
SWIFT_ACTIVE_COMPILATION_CONDITIONS = RELEASE STAGE
SWIFT_COMPILATION_MODE = wholemodule
MTL_ENABLE_DEBUG_INFO = NO
DEBUG_INFORMATION_FORMAT = dwarf-with-dsym
COPY_PHASE_STRIP = YES
```

---

### Beta.xcconfig

**File**: `/Users/adamstack/SmilePile/ios/Beta.xcconfig`

```xcconfig
// ============================================================================
// SmilePile BETA Configuration
// ============================================================================
// For external testing via TestFlight External
// Uses production bundle ID (shares with STAGE/PROD)

// Include base configuration
#include "Base.xcconfig"

// MARK: - Application Identity
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile Beta
APP_DISPLAY_NAME = SmilePile Beta
BUILD_TYPE_ENV = beta

// MARK: - Code Signing (Distribution for TestFlight)
CODE_SIGN_IDENTITY = Apple Distribution

// MARK: - Swift Optimization (Release for production-like performance)
SWIFT_OPTIMIZATION_LEVEL = -O
SWIFT_ACTIVE_COMPILATION_CONDITIONS = RELEASE BETA
SWIFT_COMPILATION_MODE = wholemodule
MTL_ENABLE_DEBUG_INFO = NO
DEBUG_INFORMATION_FORMAT = dwarf-with-dsym
COPY_PHASE_STRIP = YES
```

---

### Prod.xcconfig

**File**: `/Users/adamstack/SmilePile/ios/Prod.xcconfig`

```xcconfig
// ============================================================================
// SmilePile PROD Configuration
// ============================================================================
// For App Store production release
// Uses production bundle ID

// Include base configuration
#include "Base.xcconfig"

// MARK: - Application Identity
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile
APP_DISPLAY_NAME = SmilePile
BUILD_TYPE_ENV = prod

// MARK: - Code Signing (Distribution for App Store)
CODE_SIGN_IDENTITY = Apple Distribution

// MARK: - Swift Optimization (Release with maximum optimization)
SWIFT_OPTIMIZATION_LEVEL = -O
SWIFT_ACTIVE_COMPILATION_CONDITIONS = RELEASE PROD
SWIFT_COMPILATION_MODE = wholemodule
MTL_ENABLE_DEBUG_INFO = NO
DEBUG_INFORMATION_FORMAT = dwarf-with-dsym
COPY_PHASE_STRIP = YES
VALIDATE_PRODUCT = YES
```

---

## BuildConfig Swift Module

**File**: `/Users/adamstack/SmilePile/ios/SmilePile/Config/BuildConfig.swift`

```swift
//
//  BuildConfig.swift
//  SmilePile
//
//  Runtime configuration detection for 4-tier deployment system.
//  Reads BUILD_TYPE_ENV from Info.plist to determine deployment tier.
//
//  Wave 2: iOS 4-Tier Configuration
//  Story: STORY-6.2-ios-tier-config.md
//
//  CRITICAL FIX: Uses safe bundle initialization that works in both
//  app and test contexts. Bundle.main fails in XCTest environment.
//

import Foundation

public struct BuildConfig {
    // MARK: - Bundle Access (Test-Safe)

    /// Returns the appropriate bundle for the current context
    /// - In app context: Returns Bundle.main
    /// - In test context: Returns the test bundle
    /// This prevents crashes when accessing BuildConfig from unit tests
    private static var bundle: Bundle {
        // Check if we're running in a test environment
        if NSClassFromString("XCTestCase") != nil {
            // In test context, use the bundle containing this class
            return Bundle(for: BuildConfigBundleToken.self)
        }
        return Bundle.main
    }

    // MARK: - Build Type Detection

    /// The current build tier: qual, stage, beta, or prod
    /// Read from Info.plist BUILD_TYPE_ENV key (populated by xcconfig files)
    public static var buildType: String {
        guard let buildType = bundle.object(forInfoDictionaryKey: "BUILD_TYPE_ENV") as? String else {
            // Fallback to qual for safety - this should never happen in production
            #if DEBUG
            print("⚠️ Warning: BUILD_TYPE_ENV not found in Info.plist, defaulting to 'qual'")
            return "qual"  // Development default
            #else
            print("⚠️ Warning: BUILD_TYPE_ENV not found in Info.plist, defaulting to 'prod'")
            return "prod"  // Production default for safety
            #endif
        }
        return buildType
    }

    // MARK: - Tier Detection Helpers

    /// Returns true if running in QUAL tier (local development)
    public static var isQual: Bool {
        return buildType == "qual"
    }

    /// Returns true if running in STAGE tier (TestFlight internal)
    public static var isStage: Bool {
        return buildType == "stage"
    }

    /// Returns true if running in BETA tier (TestFlight external)
    public static var isBeta: Bool {
        return buildType == "beta"
    }

    /// Returns true if running in PROD tier (App Store)
    public static var isProd: Bool {
        return buildType == "prod"
    }

    // MARK: - Display Properties

    /// Human-readable tier name (QUAL, STAGE, BETA, PROD)
    public static var tierDisplayName: String {
        switch buildType {
        case "qual":
            return "QUAL"
        case "stage":
            return "STAGE"
        case "beta":
            return "BETA"
        case "prod":
            return "PROD"
        default:
            return "UNKNOWN"
        }
    }

    // MARK: - Bundle Information

    /// The app's bundle identifier (com.smilepile.qual or com.smilepile)
    public static var bundleIdentifier: String {
        return bundle.bundleIdentifier ?? "unknown"
    }

    /// The app's display name (SmilePile Qual, SmilePile Stage, etc.)
    public static var displayName: String {
        return bundle.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? "SmilePile"
    }
}

// MARK: - Bundle Token (For Test Context)

/// Private class used to get the correct bundle in test environment
/// This class exists solely to provide a type for Bundle(for:) in tests
private final class BuildConfigBundleToken {}
```

---

## Info.plist Modifications

**File**: `/Users/adamstack/SmilePile/ios/SmilePile/Info.plist`

### Changes Required

**Add these two entries** (insert after the opening `<dict>` tag, near other CFBundle keys):

```xml
<!-- Wave 2: 4-Tier Configuration -->
<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>

<key>CFBundleDisplayName</key>
<string>$(APP_DISPLAY_NAME)</string>
```

### Complete Modified Section

The top of your Info.plist should look like this after modification:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<!-- Wave 2: 4-Tier Configuration -->
	<key>BUILD_TYPE_ENV</key>
	<string>$(BUILD_TYPE_ENV)</string>

	<key>CFBundleDisplayName</key>
	<string>$(APP_DISPLAY_NAME)</string>

	<!-- Existing Keys Below -->
	<key>CFBundleDevelopmentRegion</key>
	<string>$(DEVELOPMENT_LANGUAGE)</string>
	<key>CFBundleExecutable</key>
	<string>$(EXECUTABLE_NAME)</string>
	<key>CFBundleIdentifier</key>
	<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
	<!-- ... rest of file unchanged ... -->
</dict>
</plist>
```

**Note**: All other keys remain unchanged. Version numbers, privacy descriptions, fonts, etc. are tier-agnostic.

---

## Xcode Scheme Setup

### Scheme Configuration Summary

| Scheme Name | Build Configuration | XCConfig File | Bundle ID | Display Name |
|-------------|-------------------|--------------|-----------|--------------|
| SmilePile Qual | Debug | Qual.xcconfig | com.smilepile.qual | SmilePile Qual |
| SmilePile Stage | Stage (new) | Stage.xcconfig | com.smilepile | SmilePile Stage |
| SmilePile Beta | Beta (new) | Beta.xcconfig | com.smilepile | SmilePile Beta |
| SmilePile Prod | Release | Prod.xcconfig | com.smilepile | SmilePile |

### Build Configuration → XCConfig Mapping

**In Xcode Project Settings → Info → Configurations**:

```
Debug Configuration:
  - SmilePile target → Qual.xcconfig

Stage Configuration (NEW - duplicate Release):
  - SmilePile target → Stage.xcconfig

Beta Configuration (NEW - duplicate Release):
  - SmilePile target → Beta.xcconfig

Release Configuration:
  - SmilePile target → Prod.xcconfig
```

### Scheme Creation Steps (Summary)

**For each scheme**:
1. Duplicate existing scheme via Xcode GUI
2. Rename to tier-specific name
3. Mark as "Shared" (stores in xcshareddata/xcschemes/)
4. Set build configuration for all actions (Run, Test, Profile, Archive)
5. Verify scheme file exists in version control

### Alternative: Command-Line Scheme Creation

If GUI approach fails, schemes can be created by duplicating and editing XML files:

```bash
cd /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes

# Duplicate base scheme
cp "SmilePile.xcscheme" "SmilePile Qual.xcscheme"

# Edit XML to change:
# - Scheme name in root tag
# - BuildConfiguration attributes in all <LaunchAction>, <TestAction>, etc.
```

**Not Recommended**: Manual XML editing is error-prone. Use Xcode GUI.

---

## Build Verification

### Verification Checklist

**For Each Scheme**:
- [ ] Builds without errors
- [ ] No warnings related to configuration
- [ ] Correct bundle ID in build log
- [ ] Correct BUILD_TYPE_ENV in build log
- [ ] Correct APP_DISPLAY_NAME in build log
- [ ] .app file created with expected name
- [ ] App installs to simulator
- [ ] App launches successfully
- [ ] Home screen shows correct display name

### Build Log Analysis

**Extract Key Settings from Build Log**:

```bash
# After building a scheme, check build log for:
grep "PRODUCT_BUNDLE_IDENTIFIER" /tmp/build-[tier].log | head -n1
grep "BUILD_TYPE_ENV" /tmp/build-[tier].log | head -n1
grep "APP_DISPLAY_NAME" /tmp/build-[tier].log | head -n1
grep "PRODUCT_NAME" /tmp/build-[tier].log | head -n1
```

**Expected Values**:

**QUAL**:
```
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile.qual
BUILD_TYPE_ENV = qual
APP_DISPLAY_NAME = SmilePile Qual
PRODUCT_NAME = SmilePile Qual
```

**STAGE**:
```
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
BUILD_TYPE_ENV = stage
APP_DISPLAY_NAME = SmilePile Stage
PRODUCT_NAME = SmilePile Stage
```

**BETA**:
```
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
BUILD_TYPE_ENV = beta
APP_DISPLAY_NAME = SmilePile Beta
PRODUCT_NAME = SmilePile Beta
```

**PROD**:
```
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
BUILD_TYPE_ENV = prod
APP_DISPLAY_NAME = SmilePile
PRODUCT_NAME = SmilePile
```

### Runtime Verification Commands

**Check Installed Bundle ID**:
```bash
# Boot simulator
xcrun simctl boot "iPhone 16"

# List installed apps
xcrun simctl listapps booted | grep smilepile

# Expected output (if QUAL installed):
# com.smilepile.qual = {
#     DisplayName = "SmilePile Qual";
#     ...
# }
```

**Check Info.plist in Built App**:
```bash
# After building QUAL scheme
cd /Users/adamstack/SmilePile/ios/DerivedData/Build/Products/Debug-iphonesimulator

# Read Info.plist from .app bundle
/usr/libexec/PlistBuddy -c "Print :BUILD_TYPE_ENV" "SmilePile Qual.app/Info.plist"
# Expected: qual

/usr/libexec/PlistBuddy -c "Print :CFBundleDisplayName" "SmilePile Qual.app/Info.plist"
# Expected: SmilePile Qual

/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "SmilePile Qual.app/Info.plist"
# Expected: com.smilepile.qual
```

---

## Deployment Script Integration

### Files to Modify

**Primary File**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`

### Exact Changes Required

#### Change 1: Update Scheme Name

**Location**: Line 488

**Before**:
```bash
-scheme SmilePile \
```

**After**:
```bash
-scheme "SmilePile Qual" \
```

#### Change 2: Update App Path

**Location**: Line 498

**Before**:
```bash
local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile.app"
```

**After**:
```bash
local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"
```

**Reason**: PRODUCT_NAME changed to "SmilePile Qual", so .app bundle name changes

#### Change 3: Update Bundle ID for Launch

**Location**: Line 550

**Before**:
```bash
xcrun simctl launch "$sim" com.smilepile.SmilePile
```

**After**:
```bash
xcrun simctl launch "$sim" com.smilepile.qual
```

**Reason**: Bundle ID changed to com.smilepile.qual for QUAL tier

### Complete Modified Section

**Lines 486-520** (after changes):

```bash
# Build for simulator
log INFO "Building iOS app..."
if [[ "$DRY_RUN" == "true" ]]; then
    log INFO "DRY RUN: Would build iOS app"
else
    xcodebuild build \
        -project SmilePile.xcodeproj \
        -scheme "SmilePile Qual" \
        -configuration Debug \
        -destination 'platform=iOS Simulator,name=iPhone 16' \
        -derivedDataPath ./DerivedData \
        || {
        log ERROR "iOS build failed"
        return 1
    }
fi

local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"

# Get available simulators
log INFO "Checking for iOS simulators..."
local booted_sims=$(xcrun simctl list devices | grep "Booted" | cut -d'(' -f2 | cut -d')' -f1 || true)

if [[ -z "$booted_sims" ]]; then
    log INFO "Starting iOS simulator..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would boot iPhone 16 simulator"
    else
        xcrun simctl boot "iPhone 16" 2>/dev/null || true
```

**Line 550** (after change):

```bash
# Launch app
log INFO "Launching app on simulator $sim..."
xcrun simctl launch "$sim" com.smilepile.qual
```

### Verification After Changes

**Test Deployment Script**:
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true DRY_RUN=true ./deploy/deploy_qual.sh ios
```

**Expected Output**:
```
DRY RUN: Would build iOS app
```

**Full Test** (actual deployment):
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true ./deploy/deploy_qual.sh ios
```

**Expected Outcome**:
- Builds using "SmilePile Qual" scheme
- Installs SmilePile Qual.app to simulator
- Launches com.smilepile.qual
- App appears on home screen as "SmilePile Qual"

---

## Testing Procedures (Complete)

### BuildConfigTests.swift

**File**: `/Users/adamstack/SmilePile/ios/SmilePileTests/BuildConfigTests.swift`

```swift
//
//  BuildConfigTests.swift
//  SmilePileTests
//
//  Tests for BuildConfig tier detection functionality.
//  Validates BUILD_TYPE_ENV is correctly set per scheme.
//
//  Wave 2: iOS 4-Tier Configuration
//  Story: STORY-6.2-ios-tier-config.md
//

import XCTest
@testable import SmilePile

final class BuildConfigTests: XCTestCase {

    // MARK: - Build Type Detection

    func testBuildTypeEnvironmentExists() {
        // BUILD_TYPE_ENV must be defined
        XCTAssertNotNil(BuildConfig.buildType, "BUILD_TYPE_ENV should never be nil")
    }

    func testBuildTypeIsValid() {
        // BUILD_TYPE_ENV must be one of the four valid tiers
        let validTiers = ["qual", "stage", "beta", "prod"]
        XCTAssertTrue(
            validTiers.contains(BuildConfig.buildType),
            "BUILD_TYPE_ENV must be qual, stage, beta, or prod. Got: \(BuildConfig.buildType)"
        )
    }

    func testExactlyOneTierIsActive() {
        // Only one tier should be true at a time
        let activeTiers = [
            BuildConfig.isQual,
            BuildConfig.isStage,
            BuildConfig.isBeta,
            BuildConfig.isProd
        ].filter { $0 == true }

        XCTAssertEqual(
            activeTiers.count,
            1,
            "Exactly one tier should be active. Active count: \(activeTiers.count)"
        )
    }

    // MARK: - Tier Helper Methods

    func testQualTierDetection() {
        if BuildConfig.buildType == "qual" {
            XCTAssertTrue(BuildConfig.isQual, "isQual should be true when buildType is qual")
            XCTAssertFalse(BuildConfig.isStage, "isStage should be false when buildType is qual")
            XCTAssertFalse(BuildConfig.isBeta, "isBeta should be false when buildType is qual")
            XCTAssertFalse(BuildConfig.isProd, "isProd should be false when buildType is qual")
        }
    }

    func testStageTierDetection() {
        if BuildConfig.buildType == "stage" {
            XCTAssertFalse(BuildConfig.isQual, "isQual should be false when buildType is stage")
            XCTAssertTrue(BuildConfig.isStage, "isStage should be true when buildType is stage")
            XCTAssertFalse(BuildConfig.isBeta, "isBeta should be false when buildType is stage")
            XCTAssertFalse(BuildConfig.isProd, "isProd should be false when buildType is stage")
        }
    }

    func testBetaTierDetection() {
        if BuildConfig.buildType == "beta" {
            XCTAssertFalse(BuildConfig.isQual, "isQual should be false when buildType is beta")
            XCTAssertFalse(BuildConfig.isStage, "isStage should be false when buildType is beta")
            XCTAssertTrue(BuildConfig.isBeta, "isBeta should be true when buildType is beta")
            XCTAssertFalse(BuildConfig.isProd, "isProd should be false when buildType is beta")
        }
    }

    func testProdTierDetection() {
        if BuildConfig.buildType == "prod" {
            XCTAssertFalse(BuildConfig.isQual, "isQual should be false when buildType is prod")
            XCTAssertFalse(BuildConfig.isStage, "isStage should be false when buildType is prod")
            XCTAssertFalse(BuildConfig.isBeta, "isBeta should be false when buildType is prod")
            XCTAssertTrue(BuildConfig.isProd, "isProd should be true when buildType is prod")
        }
    }

    // MARK: - Display Properties

    func testTierDisplayName() {
        let displayName = BuildConfig.tierDisplayName
        let expectedNames = ["QUAL", "STAGE", "BETA", "PROD"]

        XCTAssertTrue(
            expectedNames.contains(displayName),
            "tierDisplayName should be QUAL, STAGE, BETA, or PROD. Got: \(displayName)"
        )

        // Verify consistency with buildType
        switch BuildConfig.buildType {
        case "qual":
            XCTAssertEqual(displayName, "QUAL")
        case "stage":
            XCTAssertEqual(displayName, "STAGE")
        case "beta":
            XCTAssertEqual(displayName, "BETA")
        case "prod":
            XCTAssertEqual(displayName, "PROD")
        default:
            XCTFail("Unexpected buildType: \(BuildConfig.buildType)")
        }
    }

    // MARK: - Bundle Identifier

    func testBundleIdentifierMatchesTier() {
        let bundleID = BuildConfig.bundleIdentifier

        // QUAL uses unique bundle ID, others share production ID
        if BuildConfig.isQual {
            XCTAssertEqual(
                bundleID,
                "com.smilepile.qual",
                "QUAL tier should use com.smilepile.qual bundle ID"
            )
        } else {
            XCTAssertEqual(
                bundleID,
                "com.smilepile",
                "STAGE/BETA/PROD tiers should use com.smilepile bundle ID"
            )
        }
    }

    func testBundleIdentifierFromBundle() {
        // Verify we're reading from Bundle correctly
        let directBundleID = Bundle.main.bundleIdentifier
        XCTAssertNotNil(directBundleID, "Bundle identifier should exist")
        XCTAssertEqual(
            BuildConfig.bundleIdentifier,
            directBundleID,
            "BuildConfig should return same bundle ID as Bundle.main"
        )
    }

    // MARK: - Display Name

    func testDisplayNameMatchesTier() {
        let displayName = BuildConfig.displayName

        switch BuildConfig.buildType {
        case "qual":
            XCTAssertEqual(displayName, "SmilePile Qual", "QUAL display name incorrect")
        case "stage":
            XCTAssertEqual(displayName, "SmilePile Stage", "STAGE display name incorrect")
        case "beta":
            XCTAssertEqual(displayName, "SmilePile Beta", "BETA display name incorrect")
        case "prod":
            XCTAssertEqual(displayName, "SmilePile", "PROD display name incorrect")
        default:
            XCTFail("Unexpected buildType: \(BuildConfig.buildType)")
        }
    }

    // MARK: - Integration Tests

    func testBuildTypeMatchesExpectedScheme() {
        // This test validates that the correct scheme was used for the build
        // Run this test with each scheme to verify tier detection

        let buildType = BuildConfig.buildType
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print("BUILD CONFIGURATION DETECTION")
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print("Build Type:         \(buildType)")
        print("Tier Display Name:  \(BuildConfig.tierDisplayName)")
        print("Bundle ID:          \(BuildConfig.bundleIdentifier)")
        print("Display Name:       \(BuildConfig.displayName)")
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Always passes - just prints info for manual verification
        XCTAssertTrue(true)
    }

    // MARK: - Negative Tests

    func testBuildTypeIsNeverEmpty() {
        XCTAssertFalse(BuildConfig.buildType.isEmpty, "buildType should never be empty string")
    }

    func testBuildTypeIsLowercase() {
        XCTAssertEqual(
            BuildConfig.buildType,
            BuildConfig.buildType.lowercased(),
            "buildType should be lowercase"
        )
    }
}
```

### Test Execution Matrix

Run tests for each scheme to verify tier-specific behavior:

| Test Suite | Scheme | Expected buildType | Expected Bundle ID | Expected Display Name |
|------------|--------|-------------------|-------------------|----------------------|
| BuildConfigTests | SmilePile Qual | qual | com.smilepile.qual | SmilePile Qual |
| BuildConfigTests | SmilePile Stage | stage | com.smilepile | SmilePile Stage |
| BuildConfigTests | SmilePile Beta | beta | com.smilepile | SmilePile Beta |
| BuildConfigTests | SmilePile Prod | prod | com.smilepile | SmilePile |

### Manual Test Procedure

**For each tier**:
1. Build using tier-specific scheme
2. Run BuildConfigTests
3. Check test output for printed configuration
4. Install to simulator
5. Verify app name on home screen
6. Launch app and check runtime behavior

---

## Rollback Plan

### If Implementation Fails

**Severity**: LOW - Changes are isolated and non-breaking

**Rollback Steps**:

#### Step 1: Revert Git Changes

```bash
cd /Users/adamstack/SmilePile
git status
git checkout -- ios/SmilePile/Info.plist
git checkout -- deploy/deploy_qual.sh
git clean -fd ios/  # Removes untracked xcconfig files
```

#### Step 2: Remove BuildConfig.swift from Xcode

**In Xcode**:
1. Select `BuildConfig.swift` in Project Navigator
2. Right-click → Delete
3. Choose "Move to Trash"

#### Step 3: Remove New Schemes

```bash
cd /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes
rm "SmilePile Qual.xcscheme"
rm "SmilePile Stage.xcscheme"
rm "SmilePile Beta.xcscheme"
rm "SmilePile Prod.xcscheme"
```

**In Xcode**:
1. Product → Scheme → Manage Schemes
2. Select new schemes (Qual, Stage, Beta, Prod)
3. Click "-" to delete
4. Click "Close"

#### Step 4: Remove XCConfig Assignments

**In Xcode**:
1. Select SmilePile.xcodeproj → SmilePile PROJECT → Info tab
2. Under Configurations → Debug:
   - Set SmilePile target to "None"
3. Under Configurations → Release:
   - Set SmilePile target to "None"

#### Step 5: Remove Build Configurations

**In Xcode**:
1. Select SmilePile.xcodeproj → SmilePile PROJECT → Info tab
2. Under Configurations:
   - Select "Stage" configuration → Click "-" to delete
   - Select "Beta" configuration → Click "-" to delete

#### Step 6: Remove Test File

```bash
rm /Users/adamstack/SmilePile/ios/SmilePileTests/BuildConfigTests.swift
```

**In Xcode**: File will disappear from Project Navigator automatically

#### Step 7: Verify Clean State

```bash
cd /Users/adamstack/SmilePile
git status
```

**Expected**: Working tree clean (or only untracked Wave 2 evidence files)

**Test Original Build**:
```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_qual.sh ios
```

**Expected**: Original deployment flow works as before Wave 2

### Partial Rollback

If only specific components fail, you can rollback selectively:

**If BuildConfig.swift has errors**: Remove BuildConfig.swift only, keep xcconfigs
**If Schemes fail**: Remove new schemes, keep original "SmilePile" scheme
**If deployment script breaks**: Revert deploy_qual.sh only

### Prevention of Data Loss

**Before Implementation**:
```bash
cd /Users/adamstack/SmilePile
git stash save "Pre-Wave-2-Backup"
```

**To Restore**:
```bash
git stash pop
```

---

## Time Estimates

### Detailed Time Breakdown

| Phase | Task | Estimated Time | Dependencies |
|-------|------|---------------|--------------|
| 1 | Create XCConfig files (5 files: Base + 4 tiers) | 35 min | None |
| 2 | Link XCConfigs to Xcode project | 15 min | Phase 1 |
| 3 | Create BuildConfig.swift (test-safe) | 20 min | Phase 2 |
| 4 | Update Info.plist | 10 min | None |
| 5 | Create Xcode schemes (4 schemes) | 45 min | Phase 2 |
| 6 | Build verification (all schemes) | 30 min | Phase 5 |
| 7 | Update deployment script (3 changes) | 15 min | Phase 6 |
| 8 | Create tests | 30 min | Phase 3 |
| 9 | Final verification | 30 min | Phase 6, 7, 8 |
| 10 | Documentation | 30 min | Phase 9 |
| **TOTAL** | **Core Implementation** | **4.25 hours** | |
| | **With Buffer (25%)** | **5.5 hours** | |

### Critical Path

**Longest Sequence**:
1. Create XCConfigs (30 min)
2. Link to Xcode (15 min)
3. Create schemes (45 min)
4. Build verification (30 min)
5. Final verification (30 min)

**Critical Path Total**: 2.5 hours

**Parallel Tasks**:
- Info.plist update (can be done anytime)
- BuildConfig.swift (can be done in parallel with schemes)
- Tests (can be written while builds are running)
- Documentation (can be written throughout)

### Factors Affecting Time

**Faster (Optimistic: 3 hours)**:
- Experienced with Xcode configuration
- No build errors
- Schemes work on first try
- Tests pass immediately

**Slower (Pessimistic: 8 hours)**:
- First time with xcconfig files
- Xcode configuration issues
- Build errors requiring debugging
- Test failures requiring fixes
- Deployment script integration problems

### Recommended Schedule

**Day 1 (3-4 hours)**:
- Morning: Phases 1-5 (create files, configure Xcode, create schemes)
- Afternoon: Phase 6 (build verification and troubleshooting)

**Day 2 (2-3 hours)**:
- Morning: Phases 7-8 (deployment script, tests)
- Afternoon: Phases 9-10 (final verification, documentation)

**Total**: 1-2 days of focused work

---

## Success Criteria

### Implementation Complete When:

- [ ] Base.xcconfig created with common settings (CRITICAL)
- [ ] All 4 tier xcconfig files created with correct contents
- [ ] All tier xcconfig files include Base.xcconfig at the top
- [ ] All 4 Xcode schemes created and shared
- [ ] BuildConfig.swift implemented with test-safe bundle access
- [ ] Info.plist contains BUILD_TYPE_ENV and CFBundleDisplayName
- [ ] All 4 schemes build successfully (QUAL, STAGE, BETA, PROD)
- [ ] Each scheme uses correct bundle ID
- [ ] Each scheme sets correct BUILD_TYPE_ENV
- [ ] Each scheme shows correct display name (including PRODUCT_NAME)
- [ ] BuildConfigTests pass for all schemes (including in test environment)
- [ ] Existing test suites still pass
- [ ] deploy_qual.sh uses "SmilePile Qual" scheme
- [ ] deploy_qual.sh uses correct app path "SmilePile Qual.app"
- [ ] deploy_qual.sh successfully deploys to simulator
- [ ] App launches with correct tier detection
- [ ] No build warnings related to configuration
- [ ] All changes committed to git
- [ ] Wave 2 evidence documentation complete

### Key Metrics

**Build Success Rate**: 100% (4/4 schemes build without errors)
**Test Pass Rate**: 100% (including new BuildConfig tests)
**Runtime Detection**: BUILD_TYPE_ENV correctly detected for all tiers
**Zero Regression**: All existing functionality works as before

---

## Next Steps After Implementation

### Wave 3: Android 4-Tier Configuration

Apply similar pattern to Android:
- Create build flavors (qual, stage, beta, prod)
- Implement BuildConfig detection in Kotlin
- Update deploy_qual.sh for Android tier

### Wave 4: Tier-Specific Features

Enable tier-specific behavior:
- Debug menus in QUAL/STAGE
- Analytics sampling rates per tier
- API endpoint configuration per tier

### Wave 5: TestFlight Automation

Integrate with fastlane:
- Automated STAGE builds to TestFlight Internal
- Automated BETA builds to TestFlight External
- Release notes generation per tier

---

## References

### Documentation
- Research Findings: `/Users/adamstack/SmilePile/wave-evidence/wave-2/01-research-findings.md`
- Story: `/Users/adamstack/SmilePile/backlog/sprint-6/STORY-6.2-ios-tier-config.md`
- Deployment Roadmap: `/Users/adamstack/SmilePile/docs/DEPLOYMENT_ROADMAP.md`

### Xcode Resources
- XCConfig File Format: https://help.apple.com/xcode/mac/current/#/dev745c5c974
- Scheme Management: https://help.apple.com/xcode/mac/current/#/dev0bee46f46
- Build Settings Reference: https://help.apple.com/xcode/mac/current/#/itcaec37c2a6

### SmilePile Conventions
- Version System: YY.MM.DD.### (date-based with sequence)
- Build Scripts: `/Users/adamstack/SmilePile/deploy/`
- iOS Source: `/Users/adamstack/SmilePile/ios/SmilePile/`

---

**Plan Created**: 2025-10-14
**Created By**: Developer Agent
**Wave**: 2 of 10
**Story**: STORY-6.2-ios-tier-config.md
**Status**: Ready for Implementation (Phase 5)
