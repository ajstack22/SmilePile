# Reference Implementations

Links to StackMap's production implementation files with code examples and customization guidance.

## Overview

This document references StackMap's production 4-tier deployment system. Use these as working examples when implementing your own system. All file paths are relative to StackMap project root: `/Users/adamstack/StackMap/StackMap/`

## Important Note

StackMap's implementation is production-tested but specific to StackMap's needs. When adapting for your project:

1. **Replace identifiers**: Change `com.adamstack.stackmapnative` to your bundle ID/package name
2. **Adjust API endpoints**: Change `stackmap.app` to your domain
3. **Modify naming**: Change "StackMap" to your app name
4. **Review logic**: Understand WHY code works before copying
5. **Test thoroughly**: Every project has unique requirements

## iOS Configuration Files

### xcconfig Files

**Location:** `/ios/`

#### Qual.xcconfig
```
ios/Qual.xcconfig
```

**Key sections:**
- `PRODUCT_BUNDLE_IDENTIFIER = com.adamstack.stackmapnative.qual`
- `BUILD_TYPE_ENV = qual`
- `PRODUCT_NAME = StackMap Qual`
- Pod include for debug configuration

**Customization:**
- Replace `com.adamstack.stackmapnative` with your bundle ID
- Replace `StackMap` with your app name
- Adjust pod include path if different

#### Stage.xcconfig
```
ios/Stage.xcconfig
```

**Key sections:**
- `PRODUCT_BUNDLE_IDENTIFIER = com.adamstack.stackmapnative` (no suffix)
- `BUILD_TYPE_ENV = stage`
- `PRODUCT_NAME = StackMap Stage`
- Pod include for release configuration

**Customization:**
- Use base bundle ID (no suffix) for STAGE/BETA/PROD
- Display name differentiates in TestFlight

#### Beta.xcconfig
```
ios/Beta.xcconfig
```

**Key sections:**
- `PRODUCT_BUNDLE_IDENTIFIER = com.adamstack.stackmapnative` (no suffix)
- `BUILD_TYPE_ENV = beta`
- `PRODUCT_NAME = StackMap Beta`

#### Prod.xcconfig
```
ios/Prod.xcconfig
```

**Key sections:**
- `PRODUCT_BUNDLE_IDENTIFIER = com.adamstack.stackmapnative` (no suffix)
- `BUILD_TYPE_ENV = prod`
- `PRODUCT_NAME = StackMap` (clean name)

### Xcode Scheme Files

**Location:** `/ios/StackMapNative.xcodeproj/xcshareddata/xcschemes/`

#### Schemes
- `StackMapNative Qual.xcscheme`
- `StackMapNative Stage.xcscheme`
- `StackMapNative Beta.xcscheme`
- `StackMapNative Prod.xcscheme`

**Key sections in each:**
- `buildConfiguration` set to "Debug" (QUAL) or "Release" (STAGE/BETA/PROD)
- Scheme name corresponds to tier
- Marked as "Shared" for team access

**Customization:**
- Replace "StackMapNative" with your Xcode project name
- Ensure schemes link to correct xcconfig files

### Native Module

**Location:** `/ios/StackMapNative/BuildConfigModule.swift`

**Purpose:** Exposes BUILD_TYPE_ENV from xcconfig to JavaScript

**Key implementation:**
```swift
@objc
func constantsToExport() -> [AnyHashable : Any]! {
  let buildType = Bundle.main.object(forInfoDictionaryKey: "BUILD_TYPE_ENV") as? String ?? "unknown"

  return [
    "BUILD_TYPE_ENV": buildType
  ]
}
```

**Customization:**
- Change class name if desired (update imports accordingly)
- Add additional build info exports (version, build number, etc.)
- Implement in Objective-C if preferred (see docs)

**Also see:**
- `/ios/StackMapNative/Info.plist` - Contains `<key>BUILD_TYPE_ENV</key><string>$(BUILD_TYPE_ENV)</string>`

### iOS Fastlane Configuration

**Location:** `/ios/fastlane/`

#### Fastfile
```
ios/fastlane/Fastfile
```

**Lanes:**
- `qual_ios` - Simulator build for local testing
- `stage_ios` - TestFlight Internal Testing upload
- `beta_ios` - TestFlight External Testing upload
- `prod_ios` - App Store upload (manual submission)

**Key patterns:**

**QUAL lane:**
```ruby
lane :qual_ios do
  increment_build_number(xcodeproj: "StackMapNative.xcodeproj")

  build_app(
    scheme: "StackMapNative Qual",
    configuration: "Debug",
    xcconfig: "Qual.xcconfig",
    skip_package_ipa: true,  # Simulator build
    sdk: "iphonesimulator"
  )
end
```

**STAGE/BETA/PROD lanes:**
```ruby
lane :stage_ios do
  increment_build_number(xcodeproj: "StackMapNative.xcodeproj")

  build_app(
    scheme: "StackMapNative Stage",
    configuration: "Release",
    xcconfig: "Stage.xcconfig",
    export_method: "app-store"
  )

  upload_to_testflight(
    skip_waiting_for_build_processing: true,
    distribute_external: false,  # Internal only
    groups: ["Internal Testers"]
  )
end
```

**Customization:**
- Replace "StackMapNative" with your Xcode project name
- Adjust TestFlight groups to match your setup
- Add error handling, notifications, badges, etc.
- Consider using fastlane match for code signing

#### README.md
```
ios/fastlane/README.md
```

Auto-generated documentation by fastlane. Regenerate with:
```bash
cd ios
fastlane docs
```

## Android Configuration Files

### build.gradle

**Location:** `/android/app/build.gradle`

**Key sections:**

#### Product Flavors
```gradle
flavorDimensions "tier"

productFlavors {
    qual {
        dimension "tier"
        applicationId "com.adamstack.stackmapnative.qual"
        versionNameSuffix "-qual"
        resValue "string", "app_name", "StackMap Qual"
        buildConfigField "String", "BUILD_TYPE_ENV", '"qual"'
    }

    stage {
        dimension "tier"
        applicationId "com.adamstack.stackmapnative"
        versionNameSuffix "-stage"
        resValue "string", "app_name", "StackMap Stage"
        buildConfigField "String", "BUILD_TYPE_ENV", '"stage"'
    }

    beta {
        dimension "tier"
        applicationId "com.adamstack.stackmapnative"
        versionNameSuffix "-beta"
        resValue "string", "app_name", "StackMap Beta"
        buildConfigField "String", "BUILD_TYPE_ENV", '"beta"'
    }

    prod {
        dimension "tier"
        applicationId "com.adamstack.stackmapnative"
        resValue "string", "app_name", "StackMap"
        buildConfigField "String", "BUILD_TYPE_ENV", '"prod"'
    }
}
```

**Customization:**
- Replace `com.adamstack.stackmapnative` with your package name
- Replace "StackMap" with your app name
- Add additional buildConfigFields as needed (API_ENDPOINT, etc.)

#### Signing Configs
```gradle
signingConfigs {
    debug {
        storeFile file('debug.keystore')
        storePassword 'android'
        keyAlias 'androiddebugkey'
        keyPassword 'android'
    }

    qual {
        if (keystorePropertiesFile.exists()) {
            storeFile file(keystoreProperties['QUAL_STORE_FILE'])
            storePassword keystoreProperties['QUAL_STORE_PASSWORD']
            keyAlias keystoreProperties['QUAL_KEY_ALIAS']
            keyPassword keystoreProperties['QUAL_KEY_PASSWORD']
        }
    }

    production {
        if (keystorePropertiesFile.exists()) {
            storeFile file(keystoreProperties['PROD_STORE_FILE'])
            storePassword keystoreProperties['PROD_STORE_PASSWORD']
            keyAlias keystoreProperties['PROD_KEY_ALIAS']
            keyPassword keystoreProperties['PROD_KEY_PASSWORD']
        }
    }
}
```

**Customization:**
- Adjust keystore.properties keys if using different naming
- Consider environment variables for CI/CD instead of keystore.properties

### Native Module

**Location:** `/android/app/src/main/java/com/stackmapnative/`

#### BuildConfigModule.kt
```
android/app/src/main/java/com/stackmapnative/BuildConfigModule.kt
```

**Key implementation:**
```kotlin
@ReactModule(name = BuildConfigModule.NAME)
class BuildConfigModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return NAME
    }

    override fun getConstants(): Map<String, Any> {
        val constants: MutableMap<String, Any> = HashMap()
        constants["BUILD_TYPE_ENV"] = BuildConfig.BUILD_TYPE_ENV
        return constants
    }

    companion object {
        const val NAME = "BuildConfigModule"
    }
}
```

**Customization:**
- Change package name `com.stackmapnative` to yours
- Add additional constants from BuildConfig as needed
- Implement in Java if preferred

#### BuildConfigPackage.kt
```
android/app/src/main/java/com/stackmapnative/BuildConfigPackage.kt
```

**Purpose:** Registers BuildConfigModule with React Native

**Key implementation:**
```kotlin
class BuildConfigPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(BuildConfigModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

**Registration in MainApplication.kt:**
```kotlin
override fun getPackages(): List<ReactPackage> {
    return PackageList(this).packages.apply {
        add(BuildConfigPackage())
    }
}
```

**Customization:**
- Change package name to match your project

### Android Fastlane Configuration

**Location:** `/android/fastlane/`

#### Fastfile
```
android/fastlane/Fastfile
```

**Lanes:**
- `qual_android` - APK build for emulator
- `stage_android` - Play Console Internal Testing upload
- `beta_android` - Play Console Closed Testing upload
- `prod_android` - Play Console Production upload

**Key patterns:**

**QUAL lane:**
```ruby
lane :qual_android do
  gradle(
    task: "clean assembleQualRelease",
    project_dir: "."
  )

  puts "QUAL APK built: #{lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH]}"
end
```

**STAGE/BETA/PROD lanes:**
```ruby
lane :stage_android do
  gradle(
    task: "clean bundleStageRelease",
    project_dir: "."
  )

  upload_to_play_store(
    track: "internal",
    aab: "app/build/outputs/bundle/stageRelease/app-stage-release.aab",
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: true
  )
end
```

**Customization:**
- Adjust task names if flavor names differ
- Update AAB paths to match your output directory
- Add error handling, notifications, version incrementing, etc.
- Reference service account JSON in Appfile

#### README.md
```
android/fastlane/README.md
```

Auto-generated documentation. Regenerate with:
```bash
cd android
fastlane docs
```

## JavaScript/TypeScript Configuration

### Build Config Module

**Location:** `/src/config/buildConfig.js`

**Purpose:** Exposes BUILD_TYPE_ENV to JavaScript and calculates API endpoints

**Key implementation:**
```javascript
import { NativeModules } from 'react-native';

const nativeBuildConfig = NativeModules.BuildConfigModule;

export const BUILD_TYPE = nativeBuildConfig?.BUILD_TYPE_ENV || 'unknown';

export function getApiEndpoint() {
  switch (BUILD_TYPE) {
    case 'qual':
      return 'https://stackmap.app/qual/api';
    case 'stage':
      return 'https://stackmap.app/stage/api';
    case 'beta':
      return 'https://stackmap.app/beta/api';
    case 'prod':
      return 'https://stackmap.app/api';
    default:
      console.warn(`Unknown BUILD_TYPE: ${BUILD_TYPE}`);
      return 'https://stackmap.app/qual/api';
  }
}

export const API_ENDPOINT = getApiEndpoint();

export const isQual = BUILD_TYPE === 'qual';
export const isStage = BUILD_TYPE === 'stage';
export const isBeta = BUILD_TYPE === 'beta';
export const isProd = BUILD_TYPE === 'prod';
```

**Customization:**
- Replace `stackmap.app` with your domain
- Add TypeScript types if using TypeScript
- Export additional build info as needed
- Add web support with process.env fallback

**Usage throughout app:**
```javascript
import { API_ENDPOINT, BUILD_TYPE, isProd } from './config/buildConfig';

// Use API_ENDPOINT for fetch calls
fetch(`${API_ENDPOINT}/users/me`)

// Conditional features
if (!isProd) {
  // Show debug info
}
```

## Deployment Scripts

### Master Deployment Script

**Location:** `/scripts/deploy.sh`

**Purpose:** Single entry point for all deployments. Validates, locks, delegates to tier-specific scripts.

**Key sections:**

#### Argument Parsing
```bash
TIER="$1"  # qual, stage, beta, prod
PLATFORM="$2"  # --web, --ios, --android, --all

if [[ -z "$TIER" ]]; then
  echo "Usage: ./scripts/deploy.sh [TIER] [PLATFORM]"
  exit 1
fi
```

#### Validation
```bash
# Check git status
if [[ -n $(git status --porcelain) ]]; then
  echo "ERROR: Working directory has uncommitted changes"
  exit 1
fi

# Check PENDING_CHANGES.md exists
if [[ ! -f "PENDING_CHANGES.md" ]]; then
  echo "ERROR: PENDING_CHANGES.md not found"
  exit 1
fi
```

#### Locking
```bash
LOCK_FILE="/tmp/stackmap-deployment.lock"

if [[ -f "$LOCK_FILE" ]]; then
  echo "ERROR: Deployment already in progress"
  exit 1
fi

touch "$LOCK_FILE"
trap "rm -f $LOCK_FILE" EXIT
```

#### Tier Delegation
```bash
case "$TIER" in
  qual)
    ./scripts/deploy/qual_deploy.sh "$PLATFORM"
    ;;
  stage)
    ./scripts/deploy/deploy_stage.sh "$PLATFORM"
    ;;
  beta)
    ./scripts/deploy/deploy_beta.sh "$PLATFORM"
    ;;
  prod)
    ./scripts/deploy/prod_deploy.sh "$PLATFORM"
    ;;
  *)
    echo "ERROR: Invalid tier: $TIER"
    exit 1
    ;;
esac
```

**Customization:**
- Replace `stackmap` in lock file path
- Adjust validation checks for your project
- Add notifications (Slack, email, etc.)
- Add logging and summary generation

### Tier-Specific Scripts

**Location:** `/scripts/deploy/`

#### qual_deploy.sh
```
scripts/deploy/qual_deploy.sh
```

**Purpose:** Deploy QUAL (local testing only, never uploaded to stores)

**Key sections:**
- Builds for simulator/emulator
- No upload step
- Fast iteration focus

**Pattern:**
```bash
if [[ "$PLATFORM" == "--ios" ]]; then
  cd ios
  fastlane qual_ios
fi

if [[ "$PLATFORM" == "--android" ]]; then
  cd android
  fastlane qual_android
fi
```

#### deploy_stage.sh
```
scripts/deploy/deploy_stage.sh
```

**Purpose:** Deploy STAGE to internal testing

**Key sections:**
- Builds for device
- Uploads to TestFlight Internal / Play Console Internal
- Team validation focus

#### deploy_beta.sh
```
scripts/deploy/deploy_beta.sh
```

**Purpose:** Deploy BETA to closed beta testers

**Key sections:**
- Builds for device
- Uploads to TestFlight External / Play Console Closed
- External tester focus

#### prod_deploy.sh
```
scripts/deploy/prod_deploy.sh
```

**Purpose:** Deploy PROD to production

**Key sections:**
- Builds for device
- Uploads to App Store / Play Console Production
- Requires confirmation prompt
- Public release focus

**Pattern:**
```bash
read -p "Deploy to PRODUCTION? (yes/no): " confirm
if [[ "$confirm" != "yes" ]]; then
  echo "Production deployment cancelled"
  exit 0
fi

# Proceed with deployment
```

### Supporting Libraries

**Location:** `/scripts/deploy/lib/`

#### validation.sh
```
scripts/deploy/lib/validation.sh
```

**Purpose:** Pre-deployment validation checks

**Key functions:**
- `validate_git_status()` - Check for uncommitted changes
- `validate_dependencies()` - Check npm/pod/gradle
- `validate_credentials()` - Check signing credentials
- `validate_pending_changes()` - Check PENDING_CHANGES.md

#### reporting.sh
```
scripts/deploy/lib/reporting.sh
```

**Purpose:** Generate deployment summaries

**Key functions:**
- `generate_summary()` - Create HTML summary
- `send_notification()` - Slack/email notifications
- `log_deployment()` - Log to deployment history

**StackMap pattern:**
```bash
generate_summary() {
  local tier="$1"
  local platform="$2"
  local version="$3"

  cat > "deployment-summary-${tier}-${platform}.html" << EOF
<!DOCTYPE html>
<html>
<head><title>${tier} Deployment</title></head>
<body>
  <h1>${tier} Deployment - ${platform}</h1>
  <p>Version: ${version}</p>
  <p>Date: $(date)</p>
  <h2>Changes</h2>
  <pre>$(cat PENDING_CHANGES.md)</pre>
</body>
</html>
EOF
}
```

#### quality-gates.sh
```
scripts/deploy/lib/quality-gates.sh
```

**Purpose:** Run quality checks before deployment

**Key functions:**
- `run_tests()` - Execute test suite
- `check_lint()` - Run linters
- `check_types()` - TypeScript type checking
- `check_builds()` - Verify builds succeed

**Example:**
```bash
run_quality_gates() {
  echo "Running quality gates..."

  npm run lint || exit 1
  npm run typecheck || exit 1
  npm test || exit 1

  echo "Quality gates passed!"
}
```

## Documentation

### Deployment Guides

**Location:** `/docs/deployment/`

- `README.md` - Overview of deployment system
- `BETA_DEPLOYMENT_GUIDE.md` - BETA-specific procedures
- `STAGE_DEPLOYMENT_SETUP.md` - STAGE-specific setup
- `FOUR_TIER_ARCHITECTURE.md` - System architecture
- `FOUR_TIER_BUILD_GUIDE.md` - Building for each tier
- `QUALITY_GATES.md` - Pre-deployment checks

**Key insights:**
- Complete deployment procedures
- Tier-specific nuances
- Quality gate definitions
- Troubleshooting tips

### Testing Documentation

**Location:** `/docs/testing/`

- `simple-testing-guide.md` - Testing approach for each tier

**Key sections:**
- What to test in each tier
- Test checklist per platform
- Regression testing approach

## .gitignore Patterns

**Location:** `/.gitignore`

**Key sections for secrets:**
```gitignore
# iOS
*.mobileprovision
*.p12
*.p8
*.cer
ios/fastlane/report.xml
ios/fastlane/Preview.html

# Android
*.keystore
keystore.properties
android/fastlane/report.xml
android/fastlane/Preview.html
android/app/google-services.json
android/fastlane/play-store-credentials.json

# Environment
.env
.env.*

# Deployment
deployment-summary-*.html
*.lock

# Build outputs
ios/build/
android/app/build/
android/app/release/
```

**Customization:**
- Add project-specific secrets
- Add build outputs if different locations
- Add third-party service config files

## Common Patterns and Customization Points

### 1. Bundle ID / Package Name Changes

**Find and replace:**
- iOS xcconfig files: `com.adamstack.stackmapnative`
- Android build.gradle: `com.adamstack.stackmapnative`
- Native modules: Package declarations

**Pattern:**
```bash
# Global find and replace
find . -type f \( -name "*.xcconfig" -o -name "*.gradle" -o -name "*.kt" -o -name "*.swift" \) \
  -exec sed -i '' 's/com\.adamstack\.stackmapnative/com.yourcompany.yourapp/g' {} +
```

### 2. App Name Changes

**Find and replace:**
- iOS xcconfig: `PRODUCT_NAME = StackMap`
- Android build.gradle: `resValue "string", "app_name", "StackMap"`
- Fastfile comments: "StackMap"

### 3. API Endpoint Changes

**Find and replace:**
- buildConfig.js: `stackmap.app` domain
- .env files (web): `REACT_APP_API_ENDPOINT`

**Pattern:**
```javascript
// Centralized endpoint configuration
const API_BASE = 'yourapp.com';

export function getApiEndpoint() {
  switch (BUILD_TYPE) {
    case 'qual': return `https://${API_BASE}/qual/api`;
    case 'stage': return `https://${API_BASE}/stage/api`;
    // etc.
  }
}
```

### 4. Version Number Management

**StackMap approach:** Manual version increment in:
- iOS: `Info.plist` → CFBundleShortVersionString
- Android: `build.gradle` → versionName

**Alternative:** Automated versioning
```ruby
# Fastlane lane
lane :increment_version do
  increment_version_number(
    bump_type: "patch"  # or "minor", "major"
  )
end
```

### 5. Build Number Management

**StackMap approach:** Fastlane auto-increment
```ruby
increment_build_number(xcodeproj: "StackMapNative.xcodeproj")
```

**Alternative:** Date-based
```gradle
// Android build.gradle
versionCode Integer.parseInt(new Date().format("yyMMddHH"))
```

## Testing Your Implementation

After adapting StackMap's code:

### 1. Verify BUILD_TYPE_ENV Detection

```bash
# Build each tier
./scripts/deploy.sh qual --all
./scripts/deploy.sh stage --ios
./scripts/deploy.sh beta --android

# Check logs for BUILD_TYPE_ENV
# Expected: qual, stage, beta, prod (not "unknown")
```

### 2. Verify API Endpoints

```javascript
// Add to App.js temporarily
console.log('BUILD_TYPE:', BUILD_TYPE);
console.log('API_ENDPOINT:', API_ENDPOINT);

// Verify matches expected tier
```

### 3. Verify Bundle IDs / Package Names

```bash
# iOS
unzip -p ios/build/stage/StackMap.ipa Payload/StackMap.app/Info.plist | plutil -p -

# Android
aapt dump badging android/app/build/outputs/apk/qual/release/app-qual-release.apk | grep package
```

### 4. Verify App Names on Device

Install each tier and check home screen:
- QUAL: "[YOUR_APP] Qual"
- STAGE: "[YOUR_APP] Stage"
- BETA: "[YOUR_APP] Beta"
- PROD: "[YOUR_APP]"

## Getting the Most from These References

### 1. Study Before Copying

Read StackMap's implementation to understand:
- Why certain patterns are used
- What problems they solve
- What could be simplified for your project

### 2. Adapt, Don't Copy Blindly

StackMap's system evolved for StackMap's specific needs. Your project may need:
- Different tier names or counts
- Different API structure
- Additional quality gates
- Simpler or more complex workflows

### 3. Incremental Implementation

Don't try to implement everything at once:
1. Start with QUAL only
2. Add STAGE when ready for team testing
3. Add BETA when ready for user testing
4. Add PROD when ready for public release

### 4. Document Your Changes

As you adapt StackMap's code:
- Document WHY you made changes
- Note what differs from StackMap's approach
- Create your own reference docs

## Questions and Support

If StackMap's implementation is unclear:

1. **Check documentation**: `/docs/deployment/` has detailed explanations
2. **Read git history**: `git log -p -- [FILE]` shows evolution and rationale
3. **Search codebase**: Similar patterns used elsewhere may clarify intent

**Remember:** StackMap's system is a reference implementation, not a rigid template. Adapt it to your project's unique requirements.

## Summary

StackMap's 4-tier deployment system provides a complete, production-tested reference for:

- **iOS**: xcconfig files, schemes, native modules, fastlane lanes
- **Android**: product flavors, signing configs, native modules, fastlane lanes
- **JavaScript**: Build config module, API endpoint routing
- **Deployment**: Master script, tier scripts, validation, reporting
- **Documentation**: Guides, checklists, troubleshooting

Use these references as a starting point, adapt to your needs, and build a deployment system that works for your team.

**Good luck with your 4-tier deployment implementation!**
