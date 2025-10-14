# Wave 2: iOS Tier Configuration - Research Findings

**Research Phase - General Purpose Agent**
**Date**: 2025-10-14
**Wave**: 2 of 10
**Story**: STORY-6.2-ios-tier-config.md

---

## Executive Summary

This research phase investigated SmilePile's current iOS project structure to prepare for implementing the 4-tier deployment configuration (QUAL, STAGE, BETA, PROD). The investigation focused on understanding the Xcode project setup, existing build configurations, Info.plist structure, Swift module patterns, and identifying the optimal approach for implementing tier-specific configurations.

**Key Findings:**
- ✅ Standard Xcode project structure with single target
- ✅ Pure Swift/SwiftUI codebase (no React Native or Objective-C bridging)
- ✅ Existing 2-configuration setup (Debug/Release)
- ✅ Clean project with minimal complexity
- ⚠️ No existing xcconfig files
- ⚠️ No tier-specific schemes
- ⚠️ No BUILD_TYPE_ENV detection mechanism

---

## 1. Xcode Project Structure

### 1.1 Project Location
**Primary Project File:**
```
/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/
├── project.pbxproj (72,284 bytes)
├── project.pbxproj.backup (72,668 bytes)
├── project.xcworkspace/
├── xcshareddata/
│   └── xcschemes/
│       └── SmilePile.xcscheme
├── xcuserdata/
```

**Key Observations:**
- Single Xcode project (no workspace file at root level)
- Standard project structure
- One shared scheme: "SmilePile"
- Swift Package dependency: ZIPFoundation 0.9.20

### 1.2 Target Information
**Found Targets:**
1. **SmilePile** (Main app target)
   - Product Name: `SmilePile`
   - Bundle ID: `com.smilepile.SmilePile`
   - Target Identifier: `AC2F476E75CA92A685EFF389`

2. **SmilePileTests** (Test target)
   - Product Name: `SmilePileTests`
   - Bundle ID: `iosTests.SmilePileTests`
   - Target Identifier: `7B6BD9C72E8DCAE400E42F09`

### 1.3 Existing Schemes
**SmilePile.xcscheme** (Shared):
- Build Configuration: Debug (for testing/launching)
- Profile/Archive Configuration: Release
- Supports both SmilePile and SmilePileTests targets
- Standard scheme with no tier-specific customization

**Path**: `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile.xcscheme`

---

## 2. Build Configurations

### 2.1 Current Configuration Structure

**Debug Configuration** (ID: `462120A43FE372D2001FBCA4`):
```
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor
CODE_SIGN_IDENTITY = iPhone Developer
DEVELOPMENT_TEAM = 84W9WSYQQB
INFOPLIST_FILE = SmilePile/Info.plist
IPHONEOS_DEPLOYMENT_TARGET = 16.0
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile.SmilePile
SDKROOT = iphoneos
SWIFT_OPTIMIZATION_LEVEL = -Onone
TARGETED_DEVICE_FAMILY = 1,2
```

**Release Configuration** (ID: `301265FBFE83A8EF9BC5D99D`):
```
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor
CODE_SIGN_IDENTITY = iPhone Developer
DEVELOPMENT_TEAM = 84W9WSYQQB
INFOPLIST_FILE = SmilePile/Info.plist
IPHONEOS_DEPLOYMENT_TARGET = 16.0
PRODUCT_BUNDLE_IDENTIFIER = com.smilepile.SmilePile
SDKROOT = iphoneos
SWIFT_OPTIMIZATION_LEVEL = -O
TARGETED_DEVICE_FAMILY = 1,2
```

**Project-Level Build Settings** (Debug - ID: `61F288AD099A000ABC97C481`):
```
IPHONEOS_DEPLOYMENT_TARGET = 16.0
SWIFT_VERSION = 5.0
PRODUCT_NAME = $(TARGET_NAME)
MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE
SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG
SWIFT_OPTIMIZATION_LEVEL = -Onone
```

**Project-Level Build Settings** (Release - ID: `20BFC7F5077BF31137CC2AB1`):
```
IPHONEOS_DEPLOYMENT_TARGET = 16.0
SWIFT_VERSION = 5.0
PRODUCT_NAME = $(TARGET_NAME)
MTL_ENABLE_DEBUG_INFO = NO
SWIFT_COMPILATION_MODE = wholemodule
SWIFT_OPTIMIZATION_LEVEL = -O
```

### 2.2 Key Configuration Values

**Bundle Identifier:**
- Current: `com.smilepile.SmilePile`
- Team ID: `84W9WSYQQB`
- Deployment Target: iOS 16.0
- Device Family: iPhone (1) and iPad (2)

**No xcconfig Files Found:**
```bash
# Search result: No files found
find /Users/adamstack/SmilePile/ios -name "*.xcconfig"
```

**Conclusion**: Currently using inline build settings in project.pbxproj. Clean slate for implementing xcconfig-based tier system.

---

## 3. Info.plist Configuration

### 3.1 Info.plist Location
**Primary File**: `/Users/adamstack/SmilePile/ios/SmilePile/Info.plist`

### 3.2 Current Info.plist Values

**Bundle Information:**
```xml
<key>CFBundleIdentifier</key>
<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>

<key>CFBundleName</key>
<string>$(PRODUCT_NAME)</string>

<key>CFBundleShortVersionString</key>
<string>25.10.14.001</string>

<key>CFBundleVersion</key>
<string>251014001</string>
```

**Key Observations:**
- Uses build setting variables: `$(PRODUCT_BUNDLE_IDENTIFIER)`, `$(PRODUCT_NAME)`
- Version: 25.10.14.001 (Build 251014001)
- Display name derived from `$(PRODUCT_NAME)` variable
- **No BUILD_TYPE_ENV key present**

**Privacy Descriptions:**
- NSPhotoLibraryUsageDescription ✅
- NSPhotoLibraryAddUsageDescription ✅
- NSFaceIDUsageDescription ✅

**Custom Configuration:**
- UIAppFonts: Nunito family fonts
- UILaunchScreen: Custom color "LaunchScreenBackground"
- UIRequiresFullScreen: true
- Deployment Target: iOS 16.0

### 3.3 Info.plist Strategy for Tiers

**Recommendation**: Add new key for BUILD_TYPE_ENV detection:
```xml
<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>
```

This will be populated by xcconfig files per tier.

---

## 4. Swift Native Module Patterns

### 4.1 Codebase Analysis

**Source Directory**: `/Users/adamstack/SmilePile/ios/SmilePile/`

**Architecture:**
```
SmilePile/
├── Config/
│   └── AppConfig.swift (Application configuration)
├── Core/
│   └── DI/
│       └── DIContainer.swift (Dependency injection)
├── Data/
├── Managers/
├── Models/
├── Security/
│   ├── PINManager.swift
│   ├── PatternManager.swift
│   ├── KeychainManager.swift
│   └── PhotoLibraryPermissionManager.swift
├── Services/
├── Settings/
│   └── SettingsManager.swift
├── ViewModels/
├── Views/
└── Utils/
```

### 4.2 Existing Configuration Pattern

**AppConfig.swift** (`/Users/adamstack/SmilePile/ios/SmilePile/Config/AppConfig.swift`):
```swift
import Foundation

struct AppConfig {
    // MARK: - URLs
    // SECURITY: Always use HTTPS for web URLs
    static let privacyPolicyURL = "https://smilepile.app/?privacy"
    static let termsOfServiceURL = "https://smilepile.app/?tos"
    static let supportEmail = "support@stackmap.app"

    // MARK: - Computed Properties
    static var supportMailtoURL: URL? {
        URL(string: "mailto:\(supportEmail)")
    }

    // MARK: - URL Validation
    static func isValidURL(_ urlString: String) -> Bool {
        guard let url = URL(string: urlString),
              let scheme = url.scheme else {
            return false
        }
        return scheme == "https" || scheme == "mailto"
    }
}
```

**Key Observations:**
- Pure Swift configuration pattern
- Static properties for app-wide config
- Config directory already exists at `/ios/SmilePile/Config/`
- **Perfect location for new BuildConfig.swift module**

### 4.3 No React Native or Bridging

**Search Results:**
```bash
# No React Native imports found
grep -r "import.*React|RCT|@objc.*Module" ios/SmilePile/
# Result: No files found

# No bridging headers found
find ios/ -name "*-Bridging-Header.h"
# Result: No files found

# No Objective-C files in source
find ios/SmilePile -name "*.m" -o -name "*.mm" -o -name "*.h"
# Result: No files found (only build artifacts)
```

**Conclusion**: Pure Swift/SwiftUI application. No need for Objective-C bridging headers or React Native modules. Implementation will be 100% Swift.

### 4.4 Swift Module Pattern

**Found Pattern**: Standard Swift struct-based configuration
- ViewModels use `ObservableObject` protocol
- Managers use class-based singletons or static methods
- Configuration uses static struct pattern

**Recommended BuildConfig Pattern**:
```swift
// BuildConfig.swift - To be created in Config/
import Foundation

public struct BuildConfig {
    public static var buildType: String {
        guard let buildType = Bundle.main.infoDictionaryString(forKey: "BUILD_TYPE_ENV") else {
            return "qual" // Fallback for safety
        }
        return buildType
    }

    public static var isQual: Bool { buildType == "qual" }
    public static var isStage: Bool { buildType == "stage" }
    public static var isBeta: Bool { buildType == "beta" }
    public static var isProd: Bool { buildType == "prod" }
}
```

---

## 5. Current Build Configuration Approach

### 5.1 Build Settings Storage

**Current Approach**: Inline in project.pbxproj
- All build settings defined directly in Xcode project file
- No external xcconfig files
- Settings managed through Xcode UI or direct pbxproj editing

### 5.2 Advantages of Current Approach
- Simple for single-tier development
- No external file dependencies
- Standard Xcode project structure

### 5.3 Limitations for Multi-Tier
- Cannot easily maintain 4 different bundle IDs
- No way to set BUILD_TYPE_ENV per tier
- Difficult to manage tier-specific configurations
- Prone to human error when switching contexts

### 5.4 Recommendation: Migrate to xcconfig

**Rationale:**
1. **Maintainability**: Separate files per tier (Qual.xcconfig, Stage.xcconfig, etc.)
2. **Version Control**: Text-based configs easier to diff and review
3. **Consistency**: Prevent accidental setting changes
4. **Industry Standard**: Used by professional iOS teams (e.g., fastlane match)

---

## 6. XCConfig Implementation Strategy

### 6.1 No Existing XCConfig Usage

**Current State:**
- Zero xcconfig files in project
- No `#include` references in project.pbxproj
- Clean slate for implementation

**Search Results:**
```bash
find /Users/adamstack/SmilePile/ios -name "*.xcconfig"
# Result: No files found
```

### 6.2 Proposed XCConfig Structure

**File Locations** (to be created):
```
ios/
├── Config/
│   ├── Qual.xcconfig      // QUAL tier
│   ├── Stage.xcconfig     // STAGE tier
│   ├── Beta.xcconfig      // BETA tier
│   └── Prod.xcconfig      // PROD tier
└── SmilePile.xcodeproj/
```

**Alternative Location** (also acceptable):
```
ios/
├── Qual.xcconfig
├── Stage.xcconfig
├── Beta.xcconfig
├── Prod.xcconfig
└── SmilePile.xcodeproj/
```

**Recommendation**: Place at `ios/` root for visibility and consistency with Android approach.

### 6.3 XCConfig Content Template

**Qual.xcconfig:**
```xcconfig
// SmilePile QUAL Configuration
// For local development and testing

PRODUCT_BUNDLE_IDENTIFIER = com.smilepile.qual
PRODUCT_NAME = SmilePile Qual
BUILD_TYPE_ENV = qual
APP_DISPLAY_NAME = SmilePile Qual

// Signing
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = 84W9WSYQQB

// Deployment
IPHONEOS_DEPLOYMENT_TARGET = 16.0
TARGETED_DEVICE_FAMILY = 1,2
```

**Stage.xcconfig:**
```xcconfig
// SmilePile STAGE Configuration
// For internal team testing (TestFlight Internal)

PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile Stage
BUILD_TYPE_ENV = stage
APP_DISPLAY_NAME = SmilePile Stage

// Signing
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = 84W9WSYQQB

// Deployment
IPHONEOS_DEPLOYMENT_TARGET = 16.0
TARGETED_DEVICE_FAMILY = 1,2
```

**Beta.xcconfig:**
```xcconfig
// SmilePile BETA Configuration
// For external testing (TestFlight External)

PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile Beta
BUILD_TYPE_ENV = beta
APP_DISPLAY_NAME = SmilePile Beta

// Signing
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = 84W9WSYQQB

// Deployment
IPHONEOS_DEPLOYMENT_TARGET = 16.0
TARGETED_DEVICE_FAMILY = 1,2
```

**Prod.xcconfig:**
```xcconfig
// SmilePile PROD Configuration
// For App Store production release

PRODUCT_BUNDLE_IDENTIFIER = com.smilepile
PRODUCT_NAME = SmilePile
BUILD_TYPE_ENV = prod
APP_DISPLAY_NAME = SmilePile

// Signing
CODE_SIGN_STYLE = Automatic
DEVELOPMENT_TEAM = 84W9WSYQQB

// Deployment
IPHONEOS_DEPLOYMENT_TARGET = 16.0
TARGETED_DEVICE_FAMILY = 1,2
```

---

## 7. Scheme Configuration Requirements

### 7.1 Current Scheme Analysis

**Existing Scheme**: SmilePile.xcscheme
- Uses Debug configuration for testing/running
- Uses Release configuration for profiling/archiving
- Single scheme for all development

### 7.2 Proposed Scheme Structure

**Four New Schemes** (to replace/supplement existing):
1. **SmilePile Qual** → Uses Qual.xcconfig (Debug mode)
2. **SmilePile Stage** → Uses Stage.xcconfig (Release mode)
3. **SmilePile Beta** → Uses Beta.xcconfig (Release mode)
4. **SmilePile Prod** → Uses Prod.xcconfig (Release mode)

### 7.3 Scheme File Locations

**Shared Schemes** (recommended for team collaboration):
```
ios/SmilePile.xcodeproj/xcshareddata/xcschemes/
├── SmilePile Qual.xcscheme
├── SmilePile Stage.xcscheme
├── SmilePile Beta.xcscheme
└── SmilePile Prod.xcscheme
```

**User Schemes** (not recommended, developer-specific):
```
ios/SmilePile.xcodeproj/xcuserdata/[username]/xcschemes/
```

**Recommendation**: Use shared schemes so all team members have consistent build configurations.

### 7.4 Scheme Configuration Mapping

| Scheme | Build Configuration | XCConfig | Bundle ID | Display Name |
|--------|-------------------|----------|-----------|--------------|
| SmilePile Qual | Debug | Qual.xcconfig | com.smilepile.qual | SmilePile Qual |
| SmilePile Stage | Release | Stage.xcconfig | com.smilepile | SmilePile Stage |
| SmilePile Beta | Release | Beta.xcconfig | com.smilepile | SmilePile Beta |
| SmilePile Prod | Release | Prod.xcconfig | com.smilepile | SmilePile |

**Note**: QUAL uses Debug configuration for development features (faster builds, debugging). All other tiers use Release for optimized production builds.

---

## 8. Version Management

### 8.1 Current Version System

**Version String**: `25.10.14.001`
**Build Number**: `251014001`

**Format**: `YY.MM.DD.###` (date-based with sequence)

**Managed By**: `deploy/lib/build_number.sh`
- Automatically increments on each deployment
- Synchronized across iOS and Android
- Stored in Info.plist for iOS

### 8.2 Version Location in Info.plist

```xml
<key>CFBundleShortVersionString</key>
<string>25.10.14.001</string>

<key>CFBundleVersion</key>
<string>251014001</string>
```

### 8.3 Version Strategy for Tiers

**Recommendation**: Keep existing version system, no tier-specific versions needed
- All tiers use same version number
- Build metadata (BUILD_TYPE_ENV) differentiates tiers
- Simplifies version tracking and promotion
- Matches Android approach

**Example**:
- QUAL: v25.10.14.001 (BUILD_TYPE_ENV=qual)
- STAGE: v25.10.14.001 (BUILD_TYPE_ENV=stage)
- BETA: v25.10.14.001 (BUILD_TYPE_ENV=beta)
- PROD: v25.10.14.001 (BUILD_TYPE_ENV=prod)

---

## 9. Dependencies and Package Management

### 9.1 Swift Package Manager

**Current Dependency**:
- **ZIPFoundation** (0.9.20)
  - Source: https://github.com/weichsel/ZIPFoundation.git
  - Used for backup/import functionality

**Package Resolution**:
- Resolved packages stored in DerivedData/SourcePackages/
- Package.resolved tracked in project
- No Podfile or CocoaPods usage

### 9.2 Impact on Tier Configuration

**No Conflicts Expected**:
- SPM works independently of build configurations
- Dependencies resolved at project level, not per-tier
- No tier-specific dependency requirements

**Note**: Future Android-style product flavors would require investigation if flavor-specific dependencies needed.

---

## 10. Deployment Script Integration

### 10.1 Current Deployment Script

**File**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`

**Current iOS Build Command** (lines 486-495):
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

### 10.2 Required Modifications for Tiers

**QUAL Deployment** (deploy_qual.sh):
```bash
xcodebuild build \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Qual" \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 16' \
    -derivedDataPath ./DerivedData
```

**STAGE Deployment** (deploy_stage.sh - to be created):
```bash
xcodebuild archive \
    -project SmilePile.xcodeproj \
    -scheme "SmilePile Stage" \
    -configuration Release \
    -archivePath ./build/SmilePile-Stage.xcarchive \
    -destination 'generic/platform=iOS'
```

**BETA/PROD Deployments**: Similar to STAGE with scheme change

### 10.3 Deployment Script Locations

**Existing**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`
**To Create**:
- `/Users/adamstack/SmilePile/deploy/deploy_stage.sh`
- `/Users/adamstack/SmilePile/deploy/deploy_beta.sh`
- `/Users/adamstack/SmilePile/deploy/deploy_prod.sh`

---

## 11. Potential Conflicts and Considerations

### 11.1 Bundle ID Side-by-Side Installation

**QUAL**: `com.smilepile.qual` (unique, can install alongside others)
**STAGE/BETA/PROD**: `com.smilepile` (shared, cannot coexist on same device)

**Impact**:
- Developers can test QUAL alongside STAGE/BETA/PROD
- Cannot have STAGE, BETA, and PROD installed simultaneously
- Use TestFlight for STAGE/BETA differentiation
- Only PROD in App Store

**Mitigation**: Document installation behavior in team runbook

### 11.2 Signing and Provisioning

**Current**: Automatic signing with Team ID `84W9WSYQQB`

**Considerations for Tiers**:
- QUAL: Can use development provisioning (local only)
- STAGE/BETA: Require distribution certificate (TestFlight)
- PROD: Require distribution certificate (App Store)

**Recommendation**: Continue using Automatic signing. Xcode will manage provisioning profiles based on scheme and configuration.

### 11.3 App Store Connect Configuration

**Required Setup** (from Wave 1):
- App created in App Store Connect with Bundle ID: `com.smilepile`
- TestFlight Internal Testing group configured (STAGE)
- TestFlight External Testing group configured (BETA)

**QUAL Exception**: Not uploaded to App Store Connect (local only)

### 11.4 Info.plist Conflicts

**Potential Issue**: Multiple tiers modifying same Info.plist

**Solution**: Use build setting variables in Info.plist:
```xml
<key>CFBundleDisplayName</key>
<string>$(APP_DISPLAY_NAME)</string>

<key>BUILD_TYPE_ENV</key>
<string>$(BUILD_TYPE_ENV)</string>
```

XCConfig files provide values per tier, no Info.plist duplication needed.

### 11.5 Asset Catalog Considerations

**Current**: Single asset catalog for all tiers

**Future Consideration**: Tier-specific app icons
- Could use asset catalog variants
- Or separate asset catalogs per configuration
- Not required for Wave 2 MVP

**Recommendation**: Defer tier-specific icons to future iteration if needed.

---

## 12. Testing Strategy

### 12.1 Current Test Infrastructure

**Test Target**: SmilePileTests
**Test Script**: `/Users/adamstack/SmilePile/ios/scripts/run-tier-tests.sh`

**Test Tiers** (from deploy_qual.sh):
- Tier 1: Critical tests (BLOCKING)
- Tier 2: Important tests (BLOCKING)
- Tier 3: UI tests (WARNING ONLY)

**Current Test Execution** (working):
```bash
./ios/scripts/run-tier-tests.sh tier1
./ios/scripts/run-tier-tests.sh tier2
./ios/scripts/run-tier-tests.sh tier3
```

### 12.2 Impact of Tier Configuration

**Test Target Compatibility**:
- Tests run against SmilePile target
- BUILD_TYPE_ENV accessible in tests via BuildConfig
- Tests can validate tier-specific behavior

**Scheme-Specific Testing**:
- Each scheme (Qual, Stage, Beta, Prod) can run tests
- Test configuration inherits from scheme's build configuration
- Allows tier-specific test validation

### 12.3 Recommended Test Additions

**New Test Cases for Wave 2**:
1. `BuildConfigTests.swift` - Validate BUILD_TYPE_ENV detection
2. Verify bundle ID matches expected tier
3. Verify app display name matches tier
4. Test tier helper methods (isQual, isStage, etc.)

**Example Test**:
```swift
func testBuildTypeEnvironment() {
    // Should return qual, stage, beta, or prod
    XCTAssertNotNil(BuildConfig.buildType)
    XCTAssertTrue(["qual", "stage", "beta", "prod"].contains(BuildConfig.buildType))
}
```

---

## 13. Documentation Needs

### 13.1 Existing Documentation

**iOS-Specific**:
- `/Users/adamstack/SmilePile/ios/CLAUDE.md` - Development guide
- `/Users/adamstack/SmilePile/ios/LAUNCH_INSTRUCTIONS.md` - Build/run instructions

**Deployment**:
- `/Users/adamstack/SmilePile/docs/DEPLOYMENT_ROADMAP.md` - Wave implementation plan
- `/Users/adamstack/SmilePile/wave-evidence/wave-1/` - Foundation setup evidence

### 13.2 Documentation to Create/Update

**Wave 2 Deliverables**:
1. Update `ios/CLAUDE.md` with tier configuration instructions
2. Update `LAUNCH_INSTRUCTIONS.md` with scheme selection
3. Create `wave-evidence/wave-2/` directory structure
4. Document BuildConfig Swift module usage
5. Add tier-specific build commands to deployment guides

---

## 14. Recommendations for Wave 2 Implementation

### 14.1 Implementation Order

**Recommended Sequence**:
1. Create four xcconfig files (Qual, Stage, Beta, Prod)
2. Link xcconfig files to Xcode project configurations
3. Create four shared schemes pointing to configurations
4. Create BuildConfig.swift in Config/ directory
5. Update Info.plist to use BUILD_TYPE_ENV variable
6. Build each scheme to verify configuration
7. Update deployment scripts to use tier-specific schemes
8. Create tests to validate BUILD_TYPE_ENV detection
9. Update documentation

### 14.2 Critical Success Criteria

**Must Have**:
- ✅ All four schemes build successfully
- ✅ Bundle IDs correct per tier (com.smilepile.qual for QUAL, com.smilepile for others)
- ✅ BUILD_TYPE_ENV accessible from Swift code
- ✅ App display names different per tier
- ✅ No build errors or warnings
- ✅ Schemes are shared (in xcshareddata)

**Nice to Have**:
- 🔵 Tier-specific app icons (defer to Wave 3+)
- 🔵 Fastlane lane integration (Wave 5)
- 🔵 Automated testing of all tiers (Wave 6)

### 14.3 Risk Mitigation

**Low Risk Items**:
- XCConfig implementation (standard iOS practice)
- BuildConfig Swift module (simple Info.plist read)
- Scheme creation (well-documented Xcode feature)

**Medium Risk Items**:
- Bundle ID conflicts (mitigate: test on real devices)
- Signing issues (mitigate: use Automatic signing)
- Scheme selection in CI/CD (mitigate: explicit scheme flags)

**High Risk Items**:
- None identified for Wave 2

### 14.4 Rollback Plan

**If Implementation Fails**:
1. Revert xcconfig file additions
2. Delete new schemes
3. Restore original SmilePile.xcscheme
4. Remove BuildConfig.swift
5. Git revert to previous commit
6. Continue using existing single-scheme approach

**Rollback Difficulty**: LOW (no breaking changes to existing functionality)

---

## 15. File Paths Reference

### 15.1 Key Project Files

**Xcode Project**:
- Project: `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/project.pbxproj`
- Scheme: `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile.xcscheme`

**Info.plist**:
- `/Users/adamstack/SmilePile/ios/SmilePile/Info.plist`

**Source Code**:
- App Entry: `/Users/adamstack/SmilePile/ios/SmilePile/SmilePileApp.swift`
- Config: `/Users/adamstack/SmilePile/ios/SmilePile/Config/AppConfig.swift`

**Deployment**:
- Script: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`
- Libraries: `/Users/adamstack/SmilePile/deploy/lib/`

### 15.2 Files to Create (Wave 2)

**XCConfig Files**:
- `/Users/adamstack/SmilePile/ios/Qual.xcconfig`
- `/Users/adamstack/SmilePile/ios/Stage.xcconfig`
- `/Users/adamstack/SmilePile/ios/Beta.xcconfig`
- `/Users/adamstack/SmilePile/ios/Prod.xcconfig`

**Swift Modules**:
- `/Users/adamstack/SmilePile/ios/SmilePile/Config/BuildConfig.swift`

**Schemes** (created via Xcode):
- `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Qual.xcscheme`
- `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Stage.xcscheme`
- `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Beta.xcscheme`
- `/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/xcshareddata/xcschemes/SmilePile Prod.xcscheme`

**Tests**:
- `/Users/adamstack/SmilePile/ios/SmilePileTests/BuildConfigTests.swift`

**Evidence**:
- `/Users/adamstack/SmilePile/wave-evidence/wave-2/01-research-findings.md` (this file)
- `/Users/adamstack/SmilePile/wave-evidence/wave-2/02-implementation-plan.md`
- Additional evidence files as implementation progresses

---

## 16. Next Steps

### 16.1 Immediate Actions

**Phase 2: Story Creation** (Product-Manager Agent):
```
Create STORY-6.2-ios-tier-config.md with acceptance criteria:
- Four xcconfig files created (Qual, Stage, Beta, Prod)
- Four Xcode schemes created and shared
- BUILD_TYPE_ENV Swift module implemented
- Info.plist updated with BUILD_TYPE_ENV key
- All schemes build successfully for their respective tiers
- BUILD_TYPE_ENV detection working at runtime
- Documentation updated
```

**Phase 3: Planning** (Developer Agent):
```
Create detailed implementation plan with:
- Exact xcconfig file contents
- Step-by-step Xcode configuration instructions
- BuildConfig.swift full implementation
- Info.plist modification details
- Testing verification commands
- Deployment script updates
```

### 16.2 Dependencies

**Prerequisites**:
- ✅ Wave 1 complete (accounts and credentials set up)
- ✅ Xcode project structure understood
- ✅ Team ID confirmed (84W9WSYQQB)

**Blockers**:
- None identified

### 16.3 Estimated Timeline

**Phase Estimates**:
- Phase 1 (Research): COMPLETE
- Phase 2 (Story): 30 minutes
- Phase 3 (Planning): 1 hour
- Phase 4 (Security Review): 30 minutes (parallel)
- Phase 5 (Implementation): 3-4 hours
- Phase 6 (Testing): 1-2 hours (parallel)
- Phase 7 (Validation): 30 minutes
- Phase 8 (Clean-up): 30 minutes
- Phase 9 (Deployment): 30 minutes

**Total**: 6-8 hours over 1-2 days

---

## 17. Conclusion

SmilePile's iOS project is well-structured and ready for tier configuration implementation. The pure Swift/SwiftUI architecture provides a clean foundation without React Native or Objective-C complexity. The existing AppConfig pattern can be extended with a new BuildConfig module for BUILD_TYPE_ENV detection.

**Key Strengths**:
- ✅ Clean project structure with single target
- ✅ Standard Xcode configuration (no legacy complexity)
- ✅ Existing Config directory for BuildConfig placement
- ✅ Modern Swift/SwiftUI architecture
- ✅ Working deployment and test infrastructure
- ✅ Team ID and signing already configured

**Implementation Readiness**: HIGH

**Risk Level**: LOW

**Recommended Approach**: Proceed with Phase 2 (Story Creation) to define detailed acceptance criteria and begin planning implementation.

---

**Research Completed By**: General-Purpose Agent
**Date**: 2025-10-14
**Next Phase**: Story Creation (Product-Manager Agent)
**Story Reference**: /backlog/sprint-6/STORY-6.2-ios-tier-config.md (to be created)
