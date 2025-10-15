# QUAL Deployment Guide

## Overview

### What is QUAL Tier?

QUAL (Quality Assurance Local) is the first tier in SmilePile's 4-tier deployment strategy. It serves as the primary development testing environment where developers can rapidly iterate on features, test changes locally, and validate code quality before sharing with the team.

**Key Characteristics:**
- Bundle ID: `com.smilepile.qual` (iOS) / Package: `com.smilepile.qual` (Android)
- App Name: "SmilePile Qual"
- Deployment Target: Local devices and emulators only
- Frequency: Multiple times per day (5-20+ deployments during active development)
- No app store uploads - purely local testing

### Purpose in 4-Tier Deployment Strategy

```
QUAL (local testing)
  |
  v
STAGE (internal team validation)
  |
  v
BETA (external testing with real users)
  |
  v
PROD (public app store release)
```

QUAL sits at the base of the pyramid, providing fast feedback loops during development while maintaining high quality standards through automated testing.

### When to Deploy to QUAL

Deploy to QUAL when:
- Developing new features or fixes
- Testing uncommitted changes before creating a PR
- Validating app behavior on real devices/simulators
- Running quality gates (tiered tests + SonarCloud)
- Verifying build configuration changes
- Quick smoke testing before team demos

## Prerequisites

### Required Tools

1. **Git** - Version control
   ```bash
   git --version  # Should be 2.x or higher
   ```

2. **jq** - JSON processor for build number management
   ```bash
   jq --version  # Required
   # Install: brew install jq
   ```

3. **Fastlane** - Build automation
   ```bash
   bundle exec fastlane --version
   # Already installed via Gemfile
   ```

4. **SonarCloud Scanner** (optional) - Code quality analysis
   ```bash
   # Install: brew install sonar-scanner
   # Can skip with SKIP_SONAR=true
   ```

### Platform-Specific Requirements

**Android:**
- Android Studio or Android SDK
- `ANDROID_HOME` environment variable set
- `adb` available in PATH
- At least one emulator or connected device

**iOS (macOS only):**
- Xcode 15.0 or higher
- iOS Simulator installed
- Command Line Tools: `xcode-select --install`

### Credentials Needed

**None!** QUAL tier uses debug signing and requires no credentials:
- iOS: Automatic code signing
- Android: Debug keystore (auto-generated)

This makes QUAL ideal for onboarding new developers and rapid iteration.

### Environment Setup

**iOS Simulators:**
```bash
# List available simulators
xcrun simctl list devices

# Recommended: Install iPhone 16, 15, or 14 simulators
# Xcode > Preferences > Components > Simulators
```

**Android Emulators:**
```bash
# List available emulators
emulator -list-avds

# Start emulator manually (optional - script can auto-start)
emulator -avd <emulator_name> &
```

## Quick Start

### Basic Deployment

**Deploy Both Platforms:**
```bash
./deploy/deploy_qual.sh both
```

**Android Only:**
```bash
./deploy/deploy_qual.sh android
```

**iOS Only:**
```bash
./deploy/deploy_qual.sh ios
```

### What Happens

1. Version numbers increment automatically
2. Tiered tests run (Tier 1, 2, 3)
3. SonarCloud analysis (optional)
4. Builds are created (APK for Android, .app for iOS)
5. Apps deploy to all available devices/simulators
6. Changes commit to git with version tag
7. Deployment summary generated

**Expected Duration:** ~10 minutes for both platforms

## Deployment Flags

### Skip Tests

```bash
SKIP_TESTS=true ./deploy/deploy_qual.sh both
```

**Use Case:** Quick deployment when tests are already passing
**Warning:** Only use when you're confident in your changes

### Skip SonarCloud Analysis

```bash
SKIP_SONAR=true ./deploy/deploy_qual.sh both
```

**Use Case:** Network issues or when SonarCloud is timing out
**Note:** SonarCloud failures don't block deployment (Tier 3)

### Skip Git Commit

```bash
SKIP_COMMIT=true ./deploy/deploy_qual.sh both
```

**Use Case:** Testing changes without creating a commit
**Result:** Build and deploy locally, but don't push to GitHub

### Dry Run

```bash
DRY_RUN=true ./deploy/deploy_qual.sh both
```

**Use Case:** Preview what would happen without actually executing
**Result:** Shows all commands but doesn't build, test, or commit

### Allow Uncommitted Changes

```bash
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both
```

**Note:** This is the default behavior for QUAL tier (Manylla pattern)
**Purpose:** Test changes before committing to ensure quality

### Custom Commit Message

```bash
COMMIT_MESSAGE="feat: add photo filter feature" ./deploy/deploy_qual.sh both
```

**Use Case:** Provide meaningful commit message
**Default:** "qual: Deploy {platform} - v{version}"

### Disable Version Tagging

```bash
TAG_VERSION=false ./deploy/deploy_qual.sh both
```

**Use Case:** Avoid creating git tags for experimental deployments
**Default:** Tags are created as "vYY.MM.DD.VVV"

## What Happens During Deployment

### Step-by-Step Breakdown

**1. Prerequisites Check (30 seconds)**
   - Verify git, jq, adb, xcodebuild are available
   - Check `ANDROID_HOME` and Xcode installation
   - Validate platform availability

**2. Version Number Update (10 seconds)**
   - Read current version from `.build_number`
   - Increment build number (date-based: YY.MM.DD.VVV)
   - Update iOS Info.plist and Android build.gradle.kts
   - Examples: `25.10.15.001`, `25.10.15.002`

**3. Test Execution (3-6 minutes)**

   **Android:**
   ```
   Tier 1: Critical Tests (BLOCKING)
     - Security tests
     - Data integrity tests
     Duration: ~1 minute

   Tier 2: Important Tests (BLOCKING)
     - ViewModel tests
     - Repository tests
     Duration: ~2 minutes

   Tier 3: UI Tests (WARNING ONLY)
     - Component tests
     - Integration tests
     Duration: ~2 minutes
   ```

   **iOS:**
   ```
   Tier 1: Critical Tests (BLOCKING)
     - Security tests
     - Data integrity tests
     Duration: ~1 minute

   Tier 2: Important Tests (BLOCKING)
     - Repository tests
     - Dependency injection tests
     Duration: ~2 minutes

   Tier 3: UI Tests (WARNING ONLY)
     - SwiftUI component tests
     - Integration tests
     Duration: ~2 minutes
   ```

**4. SonarCloud Analysis (1-2 minutes, optional)**
   - Static code analysis
   - Code coverage reporting
   - Security hotspot detection
   - Code smell identification
   - **Tier 3**: Failures generate warnings, don't block

**5. Build Creation (2-3 minutes)**

   **Android (via Fastlane):**
   ```bash
   bundle exec fastlane qual_android
   # Creates: android/app/build/outputs/apk/qual/debug/app-qual-debug.apk
   ```

   **iOS (via Fastlane):**
   ```bash
   bundle exec fastlane qual_ios
   # Creates: ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app
   ```

**6. Device Deployment (1-2 minutes)**

   **Android:**
   - Detect all connected devices and emulators via `adb devices`
   - Auto-start emulator if none found
   - Install APK on each device: `adb install -r app-qual-debug.apk`
   - Launch app: `com.smilepile.qual`

   **iOS:**
   - Detect booted simulators via `xcrun simctl list`
   - Auto-boot simulator if none running (preference: iPhone 16 > 15 > 14)
   - Install app: `xcrun simctl install <sim-id> SmilePile Qual.app`
   - Launch app: `xcrun simctl launch <sim-id> com.smilepile.qual`

**7. Artifact Storage (10 seconds)**
   - Copy APK to `deploy/artifacts/qual/SmilePile-v{version}-qual.apk`
   - iOS .app directory preserved in DerivedData
   - Artifacts available for manual distribution

**8. Git Commit and Tag (30 seconds)**
   - Stage all changes: `git add -A`
   - Commit with version message
   - Create version tag: `vYY.MM.DD.VVV`
   - Push to GitHub (branch + tags)

**9. Summary Generation**
   - Display deployment ID, version, platform
   - Show git information (branch, commit)
   - List artifact locations
   - Provide coverage report paths

### Quality Gates

**Tier 1: Critical (BLOCKING)**
- Must pass 100% or deployment aborts
- Security-critical tests
- Data integrity validation

**Tier 2: Important (BLOCKING)**
- Must pass 100% or deployment aborts
- Business logic tests
- Core functionality validation

**Tier 3: UI Tests (WARNING)**
- Failures generate warnings but don't block
- Can be flaky due to UI timing issues
- Creates tech debt story for tracking

**SonarCloud (Tier 3 - WARNING)**
- Code quality metrics
- Coverage thresholds
- Can be skipped with `SKIP_SONAR=true`

## Expected Output

### Console Output Example

```
================================================================================
SmilePile Quality Deployment
================================================================================

Deployment ID: qual_20251015_143052
Platform: both
Dry Run: false

================================================================================
Checking Prerequisites
================================================================================
[SUCCESS] All prerequisites met

================================================================================
Updating Build Version
================================================================================
[INFO] Current version: 25.10.15.001
[INFO] New version: 25.10.15.002
[SUCCESS] Version updated to 25.10.15.002

================================================================================
Tiered Test Execution - android
================================================================================
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 1: Critical Tests (Security, Data Integrity)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[SUCCESS] [TIER 1] PASSED - Critical tests successful

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 2: Important Tests (ViewModels, Repositories)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[SUCCESS] [TIER 2] PASSED - Important tests successful

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 3: UI Tests (Components, Integration)
Status: WARNING - Deployment will continue with warning
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[SUCCESS] [TIER 3] PASSED - UI tests successful

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TEST EXECUTION SUMMARY - ANDROID
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[SUCCESS] Tier 1 Critical:  PASSED
[SUCCESS] Tier 2 Important: PASSED
[SUCCESS] Tier 3 UI:        PASSED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

================================================================================
Running SonarCloud Analysis
================================================================================
[INFO] Running code quality analysis with SonarCloud...
[SUCCESS] SonarCloud analysis completed successfully
[INFO] View results at: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile

================================================================================
Android Local Deployment
================================================================================
[INFO] Building Android APK via Fastlane...
[SUCCESS] Deployed to device: emulator-5554
[INFO] APK saved to artifacts as SmilePile-v25.10.15.002-qual.apk
[SUCCESS] Android local deployment completed

================================================================================
Committing to GitHub
================================================================================
[INFO] Uncommitted changes detected - will be included in commit
[INFO] ✅ All validation passed - safe to commit
[INFO] Staging changes...
[INFO] Creating commit...
[INFO] Creating tag: v25.10.15.002
[INFO] Pushing to GitHub...
[SUCCESS] Changes committed and pushed to GitHub

================================================================================
Deployment Summary
================================================================================

================================================================================
QUALITY DEPLOYMENT COMPLETED
================================================================================

Deployment ID:     qual_20251015_143052
Version:           v25.10.15.002 (Build 25102015002)
Platform:          both
Timestamp:         Tue Oct 15 14:35:28 PDT 2025

Git Information:
  Branch:          main
  Commit:          a3f4c2b

Artifacts:
  Location:        /Users/username/SmilePile/deploy/artifacts/qual/

Coverage Reports:
  Android:         /Users/username/SmilePile/android/app/build/reports/jacoco/jacocoDebugTestReport/html/index.html
  iOS:             /Users/username/SmilePile/ios/test_results_*.xcresult

Next Steps:
  1. Test the app on deployed devices
  2. Review coverage reports to track test quality
  3. Share APK/IPA with QA team if needed
  4. Once validated, run deploy_prod.sh to prepare store submission

================================================================================

[SUCCESS] Quality deployment completed successfully!
```

### Success Indicators

Look for these key markers:
- `[SUCCESS] All prerequisites met`
- `[SUCCESS] [TIER 1] PASSED`
- `[SUCCESS] [TIER 2] PASSED`
- `[SUCCESS] Android local deployment completed`
- `[SUCCESS] iOS local deployment completed`
- `[SUCCESS] Changes committed and pushed to GitHub`
- `[SUCCESS] Quality deployment completed successfully!`

### Version Numbering

SmilePile uses date-based semantic versioning:

**Format:** `YY.MM.DD.VVV`

**Examples:**
- `25.10.15.001` - First deployment on October 15, 2025
- `25.10.15.002` - Second deployment on October 15, 2025
- `25.10.15.023` - Twenty-third deployment on October 15, 2025

**Version Code (Android):**
- Calculated as: `YYMMDDVVV` (integer)
- Example: `2510150001` for version `25.10.15.001`

**Build Number (iOS):**
- Same format: `YY.MM.DD.VVV`
- Example: `25.10.15.001`

### Git Commit and Tag Format

**Commit Message:**
```
qual: Deploy {platform} - v{version}

Examples:
- qual: Deploy both - v25.10.15.001
- qual: Deploy android - v25.10.15.002
- feat: add photo filter feature (custom message)
```

**Tag Format:**
```
v{version}

Examples:
- v25.10.15.001
- v25.10.15.002
```

**Tag Annotation:**
```
Release version {version} - Quality deployment
```

## Common Use Cases

### Use Case 1: Test Uncommitted Changes

**Scenario:** You've made changes but want to test on a real device before committing.

**Command:**
```bash
./deploy/deploy_qual.sh both
```

**What Happens:**
1. Tests run against your uncommitted changes
2. If tests pass, changes are committed automatically
3. This is the "Manylla pattern" - validate first, then commit

**Benefit:** Never commit broken code

### Use Case 2: Quick Validation Before PR

**Scenario:** You want to verify everything builds and tests pass before creating a pull request.

**Command:**
```bash
./deploy/deploy_qual.sh both
```

**What Happens:**
1. Full quality gate execution
2. SonarCloud analysis
3. Build verification on both platforms
4. Automatic commit and push

**Benefit:** Catch issues before code review

### Use Case 3: Verify App Launches on Devices

**Scenario:** You need to see the app running on a real device to test UI or behavior.

**Command:**
```bash
./deploy/deploy_qual.sh ios
```

**What Happens:**
1. Builds for iOS simulator
2. Installs on all booted simulators
3. Launches app automatically

**Benefit:** Immediate visual feedback

### Use Case 4: Check Build Configuration

**Scenario:** You've changed gradle files or Xcode settings and want to verify the build still works.

**Command:**
```bash
SKIP_TESTS=true ./deploy/deploy_qual.sh both
```

**What Happens:**
1. Skips tests (if you know they pass)
2. Runs full build process
3. Verifies build artifacts are created

**Benefit:** Fast feedback on build configuration changes

### Use Case 5: Skip Commit for Experimental Changes

**Scenario:** You're experimenting and don't want to commit yet.

**Command:**
```bash
SKIP_COMMIT=true ./deploy/deploy_qual.sh android
```

**What Happens:**
1. Full test execution
2. Build and deploy to devices
3. No git commit or push

**Benefit:** Test risky changes without polluting git history

### Use Case 6: Custom Commit Message

**Scenario:** You want a descriptive commit message instead of the default.

**Command:**
```bash
COMMIT_MESSAGE="feat: implement photo carousel swipe gesture" ./deploy/deploy_qual.sh both
```

**What Happens:**
1. Normal deployment flow
2. Commit uses your custom message
3. Tag still created with version number

**Benefit:** Meaningful git history

## Next Steps After Deployment

1. **Test on Devices**
   - Verify app launches successfully
   - Test your feature changes
   - Check for crashes or visual issues

2. **Review Coverage Reports**
   - Android: Open HTML report in browser
   - iOS: Use `xcrun xccov view --report test_results_*.xcresult`
   - Target: >80% coverage on new code

3. **Check SonarCloud Results**
   - Visit: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile
   - Review code smells and security hotspots
   - Address any critical issues

4. **Share Artifacts (if needed)**
   - APK location: `deploy/artifacts/qual/SmilePile-v{version}-qual.apk`
   - Can be shared with QA team or stakeholders
   - iOS requires Xcode for manual installation

5. **Validate in Team Environment**
   - Once QUAL is stable, deploy to STAGE: `./deploy/deploy_stage.sh --all`
   - STAGE shares with internal team via TestFlight/Play Console

6. **Monitor for Issues**
   - Check device logs if app crashes
   - Review test output for warnings
   - Fix any Tier 3 failures tracked as tech debt

## Tips for Success

1. **Run Frequently:** QUAL is designed for multiple daily deployments
2. **Trust the Tests:** If Tier 1/2 pass, you're safe to commit
3. **Fix Tier 3 Failures:** They won't block you, but track them as tech debt
4. **Use Dry Run:** Preview changes with `DRY_RUN=true`
5. **Keep Devices Connected:** Faster deployment when devices are ready
6. **Review Coverage:** Maintain high test coverage over time

## Related Documentation

- [Troubleshooting Guide](qual-troubleshooting-guide.md) - Common issues and solutions
- [Quality Gates Documentation](quality-gates.md) - Understanding tiered testing
- [Deployment Roadmap](DEPLOYMENT_ROADMAP.md) - Overall 4-tier strategy
- [Atlas Workflow](../atlas/docs/AGENT_WORKFLOW.md) - Development methodology

---

**Document Version:** 1.0
**Last Updated:** 2025-10-15
**Maintained By:** SmilePile Development Team
