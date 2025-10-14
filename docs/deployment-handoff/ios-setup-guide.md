# iOS Setup Guide

Complete iOS configuration for 4-tier deployment system (QUAL, STAGE, BETA, PROD).

## Overview

This guide covers iOS-specific setup including bundle IDs, code signing, xcconfig files, Xcode schemes, and fastlane configuration. Budget 6-8 hours for initial setup.

## Bundle ID Strategy

StackMap's approach (recommended):

- **QUAL**: `com.[YOUR_COMPANY].[YOUR_APP].qual` (unique identifier)
- **STAGE**: `com.[YOUR_COMPANY].[YOUR_APP]` (base identifier)
- **BETA**: `com.[YOUR_COMPANY].[YOUR_APP]` (base identifier)
- **PROD**: `com.[YOUR_COMPANY].[YOUR_APP]` (base identifier)

**Why?** TestFlight differentiates builds by internal vs external testing groups, not by bundle ID. Using the same bundle ID for STAGE/BETA/PROD simplifies code signing and store management.

## Step 1: Register Bundle Identifiers

### Create QUAL Bundle ID

1. Log in to https://developer.apple.com
2. Navigate to "Certificates, Identifiers & Profiles"
3. Click "Identifiers" → "+"
4. Select "App IDs" → "App"
5. Fill in:
   - Description: `[YOUR_APP] QUAL`
   - Bundle ID: Explicit → `com.[YOUR_COMPANY].[YOUR_APP].qual`
6. Enable required capabilities:
   - Push Notifications (if needed)
   - Associated Domains (if needed)
   - Sign in with Apple (if needed)
7. Click "Continue" → "Register"

### Verify Base Bundle ID

1. Should already exist from App Store Connect app creation
2. Bundle ID: `com.[YOUR_COMPANY].[YOUR_APP]`
3. Used for STAGE, BETA, and PROD
4. Verify capabilities match your app requirements

## Step 2: Create Xcconfig Files

Create four xcconfig files in your `ios/` directory to manage per-tier configuration.

### ios/Qual.xcconfig

```xcconfig
// Qual.xcconfig - Development testing configuration

#include "Pods/Target Support Files/Pods-[YOUR_APP]/Pods-[YOUR_APP].debug.xcconfig"

// Bundle identifier with .qual suffix
PRODUCT_BUNDLE_IDENTIFIER = com.[YOUR_COMPANY].[YOUR_APP].qual

// Display name to distinguish in device list
PRODUCT_NAME = [YOUR_APP] Qual

// Qual-specific settings
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
CODE_SIGN_ENTITLEMENTS = [YOUR_APP]/[YOUR_APP].entitlements
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = [YOUR_TEAM_ID]

// Build settings
SWIFT_ACTIVE_COMPILATION_CONDITIONS = $(inherited) QUAL
GCC_PREPROCESSOR_DEFINITIONS = $(inherited) QUAL=1

// Build type for native module
BUILD_TYPE_ENV = qual
```

**Key Points:**
- `PRODUCT_BUNDLE_IDENTIFIER`: Must match registered QUAL bundle ID
- `PRODUCT_NAME`: Shows as "YOUR_APP Qual" on device home screen
- `BUILD_TYPE_ENV`: Compiled into native module (see BuildConfigModule)
- `SWIFT_ACTIVE_COMPILATION_CONDITIONS`: Enables `#if QUAL` in Swift code

### ios/Stage.xcconfig

```xcconfig
// Stage.xcconfig - Internal team validation configuration

#include "Pods/Target Support Files/Pods-[YOUR_APP]/Pods-[YOUR_APP].release.xcconfig"

// Base bundle identifier (no suffix)
PRODUCT_BUNDLE_IDENTIFIER = com.[YOUR_COMPANY].[YOUR_APP]

// Display name to distinguish from BETA/PROD in TestFlight
PRODUCT_NAME = [YOUR_APP] Stage

// Stage-specific settings
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
CODE_SIGN_ENTITLEMENTS = [YOUR_APP]/[YOUR_APP].entitlements
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = [YOUR_TEAM_ID]

// Build settings
SWIFT_ACTIVE_COMPILATION_CONDITIONS = $(inherited) STAGE
GCC_PREPROCESSOR_DEFINITIONS = $(inherited) STAGE=1

// Build type for native module
BUILD_TYPE_ENV = stage
```

**Key Points:**
- Uses base bundle ID (same as BETA/PROD)
- `PRODUCT_NAME` differentiates in TestFlight Internal Testing
- Uses release xcconfig for production-like build settings

### ios/Beta.xcconfig

```xcconfig
// Beta.xcconfig - Closed beta testing configuration

#include "Pods/Target Support Files/Pods-[YOUR_APP]/Pods-[YOUR_APP].release.xcconfig"

// Base bundle identifier (no suffix)
PRODUCT_BUNDLE_IDENTIFIER = com.[YOUR_COMPANY].[YOUR_APP]

// Display name to distinguish from STAGE/PROD in TestFlight
PRODUCT_NAME = [YOUR_APP] Beta

// Beta-specific settings
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
CODE_SIGN_ENTITLEMENTS = [YOUR_APP]/[YOUR_APP].entitlements
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = [YOUR_TEAM_ID]

// Build settings
SWIFT_ACTIVE_COMPILATION_CONDITIONS = $(inherited) BETA
GCC_PREPROCESSOR_DEFINITIONS = $(inherited) BETA=1

// Build type for native module
BUILD_TYPE_ENV = beta
```

**Key Points:**
- Uses base bundle ID (same as STAGE/PROD)
- `PRODUCT_NAME` differentiates in TestFlight External Testing
- External testers see "YOUR_APP Beta" in TestFlight

### ios/Prod.xcconfig

```xcconfig
// Prod.xcconfig - Production release configuration

#include "Pods/Target Support Files/Pods-[YOUR_APP]/Pods-[YOUR_APP].release.xcconfig"

// Base bundle identifier (no suffix)
PRODUCT_BUNDLE_IDENTIFIER = com.[YOUR_COMPANY].[YOUR_APP]

// Production display name (no suffix)
PRODUCT_NAME = [YOUR_APP]

// Production settings
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
CODE_SIGN_ENTITLEMENTS = [YOUR_APP]/[YOUR_APP].entitlements
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = [YOUR_TEAM_ID]

// Build settings
SWIFT_ACTIVE_COMPILATION_CONDITIONS = $(inherited) PROD
GCC_PREPROCESSOR_DEFINITIONS = $(inherited) PROD=1

// Build type for native module
BUILD_TYPE_ENV = prod
```

**Key Points:**
- Uses base bundle ID (same as STAGE/BETA)
- `PRODUCT_NAME` is clean (no suffix) for App Store
- Public users see "YOUR_APP" on home screen

## Step 3: Configure Xcode Schemes

Create Xcode schemes for each tier to link xcconfig files to build configurations.

### Create Schemes

1. Open your project in Xcode
2. Click scheme dropdown (near Play button) → "Manage Schemes..."
3. Create four schemes (duplicate existing scheme):

#### Qual Scheme

1. Duplicate existing scheme → Rename to "[YOUR_APP] Qual"
2. Edit Scheme → Info
3. For each action (Run, Test, Profile, Analyze, Archive):
   - Set Build Configuration → "Debug"
   - Link to Qual.xcconfig
4. Archive → Post-actions:
   - Add "Run Script" if needed for post-build steps

#### Stage Scheme

1. Duplicate existing scheme → Rename to "[YOUR_APP] Stage"
2. Edit Scheme → Info
3. For each action:
   - Set Build Configuration → "Release"
   - Link to Stage.xcconfig
4. Archive → Ensure "Reveal Archive in Organizer" is checked

#### Beta Scheme

1. Duplicate existing scheme → Rename to "[YOUR_APP] Beta"
2. Edit Scheme → Info
3. For each action:
   - Set Build Configuration → "Release"
   - Link to Beta.xcconfig

#### Prod Scheme

1. Duplicate existing scheme → Rename to "[YOUR_APP] Prod"
2. Edit Scheme → Info
3. For each action:
   - Set Build Configuration → "Release"
   - Link to Prod.xcconfig

### Scheme Configuration in project.pbxproj

Ensure schemes are marked as "Shared" so they're available to fastlane:

1. In "Manage Schemes", check "Shared" for all four schemes
2. This creates `.xcscheme` files in `ios/[YOUR_APP].xcodeproj/xcshareddata/xcschemes/`
3. Commit these to git for team consistency

**StackMap Reference**: See `/ios/StackMapNative.xcodeproj/xcshareddata/xcschemes/` for scheme examples

## Step 4: Create Native BuildConfig Module

The native module exposes `BUILD_TYPE_ENV` to JavaScript at runtime for API endpoint routing.

### Option 1: Swift Implementation (Recommended)

Create `ios/[YOUR_APP]/BuildConfigModule.swift`:

```swift
import Foundation

@objc(BuildConfigModule)
class BuildConfigModule: NSObject {

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return false
  }

  @objc
  func constantsToExport() -> [AnyHashable : Any]! {
    // Read BUILD_TYPE_ENV from xcconfig via Info.plist
    let buildType = Bundle.main.object(forInfoDictionaryKey: "BUILD_TYPE_ENV") as? String ?? "unknown"

    return [
      "BUILD_TYPE_ENV": buildType
    ]
  }
}
```

**Bridge Header** (if needed):

Create `ios/[YOUR_APP]/[YOUR_APP]-Bridging-Header.h`:

```objc
#import <React/RCTBridgeModule.h>
```

### Option 2: Objective-C Implementation

Create `ios/[YOUR_APP]/BuildConfigModule.m`:

```objc
#import <React/RCTBridgeModule.h>
#import <Foundation/Foundation.h>

@interface RCT_EXTERN_MODULE(BuildConfigModule, NSObject)

RCT_EXPORT_MODULE();

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (NSDictionary *)constantsToExport
{
  NSString *buildType = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"BUILD_TYPE_ENV"];
  if (buildType == nil) {
    buildType = @"unknown";
  }

  return @{
    @"BUILD_TYPE_ENV": buildType
  };
}

@end
```

### Update Info.plist

Add `BUILD_TYPE_ENV` to `ios/[YOUR_APP]/Info.plist`:

```xml
<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>
```

This reads the `BUILD_TYPE_ENV` variable from your xcconfig file at build time.

### Register Module

Add to `ios/[YOUR_APP]/AppDelegate.mm` (or `.m`):

```objc
// Import at top
#import "[YOUR_APP]-Swift.h"  // If using Swift

// Module is auto-registered by React Native
// No additional code needed
```

**StackMap Reference**: See `/ios/StackMapNative/BuildConfigModule.swift` for complete implementation

## Step 5: Configure Fastlane

Create iOS-specific fastlane configuration for automated deployments.

### Initialize Fastlane (if not done)

```bash
cd ios
fastlane init

# Choose option 2: "Automate beta distribution to TestFlight"
# Enter Apple ID
# Enter App Identifier: com.[YOUR_COMPANY].[YOUR_APP]
# Follow prompts
```

### Create ios/fastlane/Fastfile

See [fastlane-configuration.md](./fastlane-configuration.md) for complete Fastfile setup.

**Key lanes for iOS:**

```ruby
lane :qual_ios do
  # Build for simulator (local testing only)
  build_app(
    scheme: "[YOUR_APP] Qual",
    configuration: "Debug",
    xcconfig: "Qual.xcconfig",
    skip_package_ipa: true,  # Simulator build
    sdk: "iphonesimulator"
  )
end

lane :stage_ios do
  # Build and upload to TestFlight Internal Testing
  build_app(
    scheme: "[YOUR_APP] Stage",
    configuration: "Release",
    xcconfig: "Stage.xcconfig"
  )

  upload_to_testflight(
    skip_waiting_for_build_processing: true,
    distribute_external: false,  # Internal only
    groups: ["Internal Testers"]
  )
end

lane :beta_ios do
  # Build and upload to TestFlight External Testing
  build_app(
    scheme: "[YOUR_APP] Beta",
    configuration: "Release",
    xcconfig: "Beta.xcconfig"
  )

  upload_to_testflight(
    skip_waiting_for_build_processing: true,
    distribute_external: true,  # External testers
    groups: ["Beta Testers"]
  )
end

lane :prod_ios do
  # Build and upload to App Store
  build_app(
    scheme: "[YOUR_APP] Prod",
    configuration: "Release",
    xcconfig: "Prod.xcconfig"
  )

  upload_to_app_store(
    skip_metadata: true,
    skip_screenshots: true,
    submit_for_review: false  # Manual submission
  )
end
```

**StackMap Reference**: See `/ios/fastlane/Fastfile` for complete lane implementations

## Step 6: Code Signing Setup

### Option 1: Automatic Signing (Recommended for Small Teams)

1. In each xcconfig file, set:
   ```xcconfig
   CODE_SIGN_STYLE = Automatic
   DEVELOPMENT_TEAM = [YOUR_TEAM_ID]
   ```

2. Xcode handles certificate and provisioning profile management

3. Ensure Apple ID is logged in: Xcode → Settings → Accounts

### Option 2: fastlane match (Recommended for Teams)

1. Initialize match:
   ```bash
   cd ios
   fastlane match init
   ```

2. Choose storage (git recommended):
   - Create private git repo for certificates
   - Enter repo URL
   - Set encryption password (store securely!)

3. Generate certificates and profiles:
   ```bash
   # Development (for QUAL)
   fastlane match development

   # App Store (for STAGE/BETA/PROD)
   fastlane match appstore
   ```

4. Update xcconfig files:
   ```xcconfig
   CODE_SIGN_STYLE = Manual
   PROVISIONING_PROFILE_SPECIFIER = match AppStore com.[YOUR_COMPANY].[YOUR_APP]
   CODE_SIGN_IDENTITY = iPhone Distribution
   ```

5. Add to Fastfile before build_app:
   ```ruby
   match(type: "appstore", readonly: true)
   ```

**StackMap Approach**: StackMap uses automatic signing for simplicity. For teams >3 developers, fastlane match is recommended.

## Step 7: Verify Configuration

### Build Each Tier Locally

```bash
# QUAL (Simulator)
cd ios
xcodebuild -workspace [YOUR_APP].xcworkspace \
  -scheme "[YOUR_APP] Qual" \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build

# STAGE (Device)
xcodebuild -workspace [YOUR_APP].xcworkspace \
  -scheme "[YOUR_APP] Stage" \
  -configuration Release \
  -sdk iphoneos \
  archive -archivePath build/Stage.xcarchive

# BETA (Device)
xcodebuild -workspace [YOUR_APP].xcworkspace \
  -scheme "[YOUR_APP] Beta" \
  -configuration Release \
  -sdk iphoneos \
  archive -archivePath build/Beta.xcarchive

# PROD (Device)
xcodebuild -workspace [YOUR_APP].xcworkspace \
  -scheme "[YOUR_APP] Prod" \
  -configuration Release \
  -sdk iphoneos \
  archive -archivePath build/Prod.xcarchive
```

### Test BUILD_TYPE_ENV Detection

Add temporary logging to your app:

```javascript
// App.js or index.js
import { NativeModules } from 'react-native';

console.log('BUILD_TYPE_ENV:', NativeModules.BuildConfigModule.BUILD_TYPE_ENV);
```

Build and run each tier, verify console output:
- QUAL: `BUILD_TYPE_ENV: qual`
- STAGE: `BUILD_TYPE_ENV: stage`
- BETA: `BUILD_TYPE_ENV: beta`
- PROD: `BUILD_TYPE_ENV: prod`

## Step 8: First Deployments

### Deploy QUAL

```bash
cd /path/to/project
./scripts/deploy.sh qual --ios

# Verify:
# - Build succeeds
# - App installs on simulator
# - Bundle ID: com.[YOUR_COMPANY].[YOUR_APP].qual
# - Display name: [YOUR_APP] Qual
# - BUILD_TYPE_ENV: qual
```

### Deploy STAGE

```bash
./scripts/deploy.sh stage --ios

# Verify:
# - Build succeeds
# - Upload to TestFlight succeeds
# - Processing completes in App Store Connect (15-30 min)
# - Internal Testing group receives build
# - Bundle ID: com.[YOUR_COMPANY].[YOUR_APP]
# - Display name: [YOUR_APP] Stage
# - BUILD_TYPE_ENV: stage
```

### Deploy BETA

```bash
./scripts/deploy.sh beta --ios

# First time only:
# - Submit for TestFlight review (1-2 days)
# - Await approval

# After approval:
# - External Testing group receives build
# - Bundle ID: com.[YOUR_COMPANY].[YOUR_APP]
# - Display name: [YOUR_APP] Beta
# - BUILD_TYPE_ENV: beta
```

### Deploy PROD

```bash
./scripts/deploy.sh prod --ios

# Verify:
# - Build succeeds
# - Upload to App Store Connect succeeds
# - Shows in "iOS App" section (not TestFlight)
# - Ready for manual submission
# - Bundle ID: com.[YOUR_COMPANY].[YOUR_APP]
# - Display name: [YOUR_APP]
# - BUILD_TYPE_ENV: prod
```

## Common Issues and Solutions

### "No matching provisioning profiles found"

**Solution**:
```bash
# Automatic signing: Log in to Xcode with Apple ID
# Manual signing: Run fastlane match
cd ios
fastlane match appstore
```

### "Team ID not found"

**Solution**: Find your Team ID:
1. https://developer.apple.com → Account → Membership
2. Copy Team ID
3. Update xcconfig files: `DEVELOPMENT_TEAM = [YOUR_TEAM_ID]`

### "xcconfig file not found during build"

**Solution**: Ensure xcconfig files are in `ios/` directory (same level as Podfile)

### "BUILD_TYPE_ENV undefined in JavaScript"

**Solution**:
1. Verify Info.plist contains `<key>BUILD_TYPE_ENV</key>`
2. Rebuild app (clean build folder first)
3. Check native module is registered in AppDelegate

### "App shows wrong name on device"

**Solution**: Check Xcode build used correct scheme. Verify:
```bash
# In build output, look for:
# "Building for scheme: [YOUR_APP] Stage"
```

## iOS-Specific Considerations

### App Icons

Consider creating distinct icons for QUAL/STAGE/BETA:
- QUAL: Add "Q" badge or different color
- STAGE: Add "S" badge
- BETA: Add "β" badge
- PROD: Clean icon (no badge)

Update each xcconfig:
```xcconfig
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon-Qual  // or AppIcon-Stage, etc.
```

### Entitlements

If your app uses entitlements (e.g., Push Notifications, Associated Domains):

1. Create per-tier entitlement files:
   - `[YOUR_APP]/[YOUR_APP]-Qual.entitlements`
   - `[YOUR_APP]/[YOUR_APP]-Stage.entitlements`
   - `[YOUR_APP]/[YOUR_APP]-Beta.entitlements`
   - `[YOUR_APP]/[YOUR_APP]-Prod.entitlements`

2. Update xcconfig files:
   ```xcconfig
   CODE_SIGN_ENTITLEMENTS = [YOUR_APP]/[YOUR_APP]-Qual.entitlements
   ```

3. Configure tier-specific domains (e.g., for Universal Links):
   ```xml
   <!-- Qual.entitlements -->
   <key>com.apple.developer.associated-domains</key>
   <array>
     <string>applinks:qual.[YOUR_DOMAIN].com</string>
   </array>
   ```

### TestFlight Review

First BETA submission requires TestFlight review:
- Timeline: 1-2 days
- Provide test account if app requires login
- Answer compliance questions (encryption, etc.)
- Subsequent BETA builds auto-distribute (no re-review)

## Next Steps

After completing iOS setup:

1. Proceed to [android-setup-guide.md](./android-setup-guide.md) for Android configuration
2. Configure fastlane in [fastlane-configuration.md](./fastlane-configuration.md)
3. Set up build config in [environment-configuration.md](./environment-configuration.md)
4. Test deployments following [deployment-workflow.md](./deployment-workflow.md)

## StackMap Reference Files

Complete working examples from StackMap:

- `/ios/Qual.xcconfig`, `Stage.xcconfig`, `Beta.xcconfig`, `Prod.xcconfig`
- `/ios/StackMapNative.xcodeproj/xcshareddata/xcschemes/` (scheme files)
- `/ios/StackMapNative/BuildConfigModule.swift`
- `/ios/StackMapNative/Info.plist`
- `/ios/fastlane/Fastfile` (lanes: qual_ios, stage_ios, beta_ios, prod_ios)

See [reference-implementations.md](./reference-implementations.md) for detailed code examples.
