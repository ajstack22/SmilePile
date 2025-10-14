# Troubleshooting Guide

Common deployment issues, error messages, and solutions for iOS, Android, and web deployments in the 4-tier system.

## Overview

This guide covers the most common deployment problems you'll encounter, their causes, and step-by-step solutions. Issues are organized by platform and category.

## General Deployment Issues

### Deployment Lock Not Released

**Symptom:**
```
ERROR: Deployment already in progress
Lock file: /tmp/[YOUR_APP]-deployment.lock
```

**Cause:** Previous deployment crashed or was interrupted without releasing lock.

**Solution:**
```bash
# Remove lock file manually
rm /tmp/[YOUR_APP]-deployment.lock

# Or use full path
rm /private/tmp/[YOUR_APP]-deployment.lock

# Then retry deployment
./scripts/deploy.sh [TIER] [PLATFORM]
```

**Prevention:** Ensure deployment scripts have proper error handling to release lock on failure.

### Git Working Directory Not Clean

**Symptom:**
```
ERROR: Working directory has uncommitted changes
Cannot deploy with uncommitted changes
```

**Cause:** Uncommitted files in working directory.

**Solution:**
```bash
# Check what files are uncommitted
git status

# Option 1: Commit changes
git add .
git commit -m "Your commit message"

# Option 2: Stash changes temporarily
git stash

# Then retry deployment
./scripts/deploy.sh [TIER] [PLATFORM]

# Restore stashed changes after deployment
git stash pop
```

### PENDING_CHANGES.md Not Found

**Symptom:**
```
ERROR: PENDING_CHANGES.md not found
Please create PENDING_CHANGES.md with deployment notes
```

**Cause:** Missing PENDING_CHANGES.md file.

**Solution:**
```bash
# Create file with deployment notes
cat > PENDING_CHANGES.md << 'EOF'
## Title: [Your deployment title]

### Changes Made:
- Change 1
- Change 2
- Change 3

### Testing:
- Tested on QUAL
- Verified in STAGE

### Notes:
- Any additional notes
EOF

# Commit and retry
git add PENDING_CHANGES.md
git commit -m "Add pending changes"
./scripts/deploy.sh [TIER] [PLATFORM]
```

### Node Modules Not Installed

**Symptom:**
```
ERROR: node_modules not found
ERROR: Cannot find module 'react-native'
```

**Cause:** Missing dependencies.

**Solution:**
```bash
# Install dependencies
npm install

# Verify installation
ls node_modules | head

# Retry deployment
./scripts/deploy.sh [TIER] [PLATFORM]
```

### Fastlane Not Found

**Symptom:**
```
bash: fastlane: command not found
```

**Cause:** Fastlane not installed or not in PATH.

**Solution:**
```bash
# Check if installed
which fastlane

# If not installed, install via Homebrew
brew install fastlane

# Or via RubyGems
sudo gem install fastlane

# Verify installation
fastlane --version

# Retry deployment
./scripts/deploy.sh [TIER] [PLATFORM]
```

## iOS Deployment Issues

### Code Signing Errors

#### Error: "No matching provisioning profiles found"

**Symptom:**
```
Error: No profiles for '[YOUR_BUNDLE_ID]' were found
Xcode couldn't find any iOS App Development provisioning profiles matching '[YOUR_BUNDLE_ID]'
```

**Cause:** Missing or expired provisioning profile.

**Solution:**

**If using automatic signing:**
```bash
# Open Xcode
open ios/[YOUR_APP].xcworkspace

# Navigate to: Signing & Capabilities tab
# Ensure "Automatically manage signing" is checked
# Select correct team
# Xcode will generate new profile
```

**If using fastlane match:**
```bash
cd ios

# Regenerate profiles
fastlane match appstore --force_for_new_devices

# Or regenerate all
fastlane match development --readonly false --force
fastlane match appstore --readonly false --force

# Retry build
cd ..
./scripts/deploy.sh [TIER] --ios
```

#### Error: "Code signing certificate not found"

**Symptom:**
```
Error: No signing certificate "iOS Distribution" found
```

**Cause:** Distribution certificate not installed in keychain.

**Solution:**

**If using fastlane match:**
```bash
cd ios
fastlane match appstore

# Enter match passphrase
# Certificates will be installed in keychain
```

**If using manual certificates:**
```bash
# Open Keychain Access
open -a "Keychain Access"

# Import certificate
# File → Import Items → Select .p12 file
# Enter password
# Verify certificate appears in "My Certificates"

# Retry build
./scripts/deploy.sh [TIER] --ios
```

#### Error: "User interaction is not allowed"

**Symptom:**
```
User interaction is not allowed
Error: Could not access keychain
```

**Cause:** Keychain locked or requires user interaction during automated build.

**Solution:**
```bash
# Unlock keychain
security unlock-keychain -p [YOUR_PASSWORD] ~/Library/Keychains/login.keychain-db

# Set keychain to not lock
security set-keychain-settings -t 3600 -l ~/Library/Keychains/login.keychain-db

# Retry deployment
./scripts/deploy.sh [TIER] --ios
```

**For CI/CD:**
```bash
# Create dedicated build keychain
security create-keychain -p [PASSWORD] build.keychain
security default-keychain -s build.keychain
security unlock-keychain -p [PASSWORD] build.keychain
security set-keychain-settings -t 3600 -l build.keychain
```

### Build Errors

#### Error: "Command PhaseScriptExecution failed"

**Symptom:**
```
PhaseScriptExecution failed with a nonzero exit code
```

**Cause:** Script phase failed (often React Native bundling).

**Solution:**
```bash
# Check specific error in Xcode build log
open ios/[YOUR_APP].xcworkspace

# Build → Show Build Log
# Search for "PhaseScriptExecution" and read error details

# Common fix: Clear caches
rm -rf node_modules
rm -rf ~/Library/Developer/Xcode/DerivedData
npm install
cd ios && pod install && cd ..

# Retry build
./scripts/deploy.sh [TIER] --ios
```

#### Error: "Library not found"

**Symptom:**
```
ld: library not found for -lPods-[YOUR_APP]
```

**Cause:** CocoaPods not installed or Pods not up to date.

**Solution:**
```bash
cd ios

# Reinstall pods
rm -rf Pods Podfile.lock
pod install

# If pod install fails, update CocoaPods
sudo gem install cocoapods

# Retry pod install
pod install

cd ..
./scripts/deploy.sh [TIER] --ios
```

#### Error: "Module not found"

**Symptom:**
```
'React/RCTBridgeModule.h' file not found
```

**Cause:** Missing React Native headers or incorrect pod setup.

**Solution:**
```bash
cd ios

# Clean and reinstall
rm -rf Pods Podfile.lock ~/Library/Developer/Xcode/DerivedData
pod install

# Clean project in Xcode
open [YOUR_APP].xcworkspace
# Product → Clean Build Folder (Cmd+Shift+K)

cd ..
./scripts/deploy.sh [TIER] --ios
```

### Upload Errors

#### Error: "Invalid IPA"

**Symptom:**
```
Asset validation failed
Invalid IPA: [error details]
```

**Cause:** IPA doesn't meet App Store requirements.

**Solution:**
```bash
# Check error details in Xcode Organizer or fastlane output

# Common fixes:

# 1. Missing icons
# Ensure all required icon sizes are present in Assets.xcassets

# 2. Wrong bundle ID
# Verify PRODUCT_BUNDLE_IDENTIFIER in xcconfig matches App Store Connect

# 3. Invalid entitlements
# Review entitlements file, ensure capabilities match bundle ID

# 4. Missing compliance info
# Provide export compliance in Info.plist or during upload

# Rebuild and retry
./scripts/deploy.sh [TIER] --ios
```

#### Error: "App Store Connect operation failed"

**Symptom:**
```
Error uploading to App Store Connect
Unauthorized
```

**Cause:** Invalid credentials or API key.

**Solution:**

**If using API key:**
```bash
# Verify API key file exists
ls ~/app-store-connect-api-keys/AuthKey_*.p8

# Verify key ID and issuer ID in Fastfile
cat ios/fastlane/Fastfile | grep key_id

# Re-download API key if needed (from App Store Connect)
# Keys are only downloadable once, use backup if available
```

**If using Apple ID:**
```bash
# Re-authenticate
fastlane fastlane-credentials remove --username your@email.com
fastlane fastlane-credentials add --username your@email.com

# Enter app-specific password
# Retry deployment
./scripts/deploy.sh [TIER] --ios
```

#### Error: "Build processing stuck"

**Symptom:**
```
Build uploaded but stuck in "Processing" state in App Store Connect
```

**Cause:** Apple's backend processing issue (usually resolves automatically).

**Solution:**
```bash
# Wait 30-60 minutes
# Check App Store Connect → TestFlight → Builds

# If still stuck after 2 hours:
# 1. Contact Apple Developer Support
# 2. Or upload new build with incremented build number

# Increment build number
cd ios
fastlane increment_build_number
cd ..

# Retry deployment
./scripts/deploy.sh [TIER] --ios
```

### TestFlight Issues

#### Error: "Export compliance required"

**Symptom:**
```
Missing compliance in TestFlight
Cannot distribute to external testers
```

**Cause:** Export compliance not provided.

**Solution:**
```xml
<!-- Add to Info.plist -->
<key>ITSAppUsesNonExemptEncryption</key>
<false/>

<!-- OR if your app uses encryption: -->
<key>ITSAppUsesNonExemptEncryption</key>
<true/>
<key>ITSEncryptionExportComplianceCode</key>
<string>[YOUR_ERN_NUMBER]</string>
```

Then rebuild and reupload.

#### Error: "TestFlight review rejected"

**Symptom:**
```
Build rejected for TestFlight External Testing
Reason: [rejection reason]
```

**Cause:** Violation of TestFlight review guidelines.

**Solution:**
1. Read rejection reason carefully
2. Fix issue (common: demo account needed, age rating incorrect)
3. Resubmit same build with updated info, or upload new build
4. Respond to reviewer notes if needed

## Android Deployment Issues

### Keystore Errors

#### Error: "Keystore file not found"

**Symptom:**
```
Keystore file '[PATH]' not found.
```

**Cause:** Keystore path incorrect in keystore.properties or environment variable.

**Solution:**
```bash
# Verify keystore exists
ls ~/keystores/[YOUR_APP]-production.keystore

# Check path in keystore.properties
cat android/keystore.properties

# Use absolute path
PROD_STORE_FILE=/Users/[USERNAME]/keystores/[YOUR_APP]-production.keystore

# Or relative to android/app
PROD_STORE_FILE=../../../keystores/[YOUR_APP]-production.keystore

# Retry deployment
./scripts/deploy.sh [TIER] --android
```

#### Error: "Keystore password incorrect"

**Symptom:**
```
Keystore was tampered with, or password was incorrect
```

**Cause:** Wrong keystore password or corrupted keystore.

**Solution:**
```bash
# Verify password
keytool -list -v -keystore ~/keystores/[YOUR_APP]-production.keystore
# Enter password
# If correct, keystore details display

# If password forgotten: CANNOT RECOVER
# - If not yet uploaded to Play Store: Generate new keystore
# - If already in Play Store: Contact Google support (may require new app)

# If keystore corrupted: Restore from backup
cp ~/Backups/[YOUR_APP]-production.keystore ~/keystores/

# Retry deployment
./scripts/deploy.sh [TIER] --android
```

#### Error: "Key alias not found"

**Symptom:**
```
Alias '[YOUR_ALIAS]' does not exist in keystore
```

**Cause:** Wrong alias in keystore.properties.

**Solution:**
```bash
# List all aliases in keystore
keytool -list -v -keystore ~/keystores/[YOUR_APP]-production.keystore

# Find correct alias
# Update keystore.properties with correct alias
PROD_KEY_ALIAS=[CORRECT_ALIAS]

# Retry deployment
./scripts/deploy.sh [TIER] --android
```

### Build Errors

#### Error: "Gradle build failed"

**Symptom:**
```
FAILURE: Build failed with an exception.
```

**Cause:** Various (check error details).

**Solution:**
```bash
# Clean and rebuild
cd android
./gradlew clean

# Check for specific error in output

# Common fixes:

# 1. Dependency issue
./gradlew --refresh-dependencies

# 2. Build cache issue
./gradlew cleanBuildCache

# 3. Gradle version issue
# Update gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-all.zip

# Retry build
cd ..
./scripts/deploy.sh [TIER] --android
```

#### Error: "Duplicate class found"

**Symptom:**
```
Duplicate class [CLASS_NAME] found in modules
```

**Cause:** Conflicting dependencies.

**Solution:**
```bash
# Check dependency tree
cd android
./gradlew app:dependencies

# Find duplicate dependencies
# Exclude one in build.gradle:
dependencies {
  implementation('com.example:library:1.0') {
    exclude group: 'com.duplicate', module: 'module'
  }
}

# Retry build
cd ..
./scripts/deploy.sh [TIER] --android
```

#### Error: "Out of memory"

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause:** Insufficient memory for Gradle build.

**Solution:**
```bash
# Increase Gradle memory
# Edit android/gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError

# Enable parallel builds
org.gradle.parallel=true

# Enable daemon
org.gradle.daemon=true

# Retry build
./scripts/deploy.sh [TIER] --android
```

### Upload Errors

#### Error: "Package name mismatch"

**Symptom:**
```
Package name '[PACKAGE_NAME]' does not match target package name '[OTHER_PACKAGE_NAME]'
```

**Cause:** First upload must use base package name (no suffix).

**Solution:**
```bash
# First upload to Play Console must be STAGE or PROD, never QUAL
# QUAL uses .qual suffix which doesn't match Play Console app

# Upload STAGE first
./scripts/deploy.sh stage --android

# After first upload, all subsequent uploads must use same package name
```

#### Error: "Version code must be greater"

**Symptom:**
```
Version code [VERSION_CODE] has already been used
```

**Cause:** Version code not incremented.

**Solution:**
```bash
# Increment version code in android/app/build.gradle
android {
  defaultConfig {
    versionCode 2  // Increment from 1
  }
}

# Or use dynamic version code
versionCode Integer.parseInt(new Date().format("yyMMddHH"))

# Rebuild and retry
./scripts/deploy.sh [TIER] --android
```

#### Error: "Upload not authorized"

**Symptom:**
```
The caller does not have permission
Forbidden
```

**Cause:** Service account lacks permissions or JSON file invalid.

**Solution:**
```bash
# Verify service account has "Release Manager" role
# Play Console → Setup → API access → View service accounts

# Verify JSON file is correct
cat android/fastlane/play-store-credentials.json

# Re-download JSON if needed
# Play Console → Setup → API access → Service account → Manage keys

# Retry deployment
./scripts/deploy.sh [TIER] --android
```

#### Error: "AAB file not found"

**Symptom:**
```
Could not find AAB at path: [PATH]
```

**Cause:** Build failed or AAB output path incorrect.

**Solution:**
```bash
# Check build output
ls android/app/build/outputs/bundle/stageRelease/

# Expected: app-stage-release.aab

# If missing, check build logs for errors
cd android
./gradlew bundleStageRelease --stacktrace

# Fix any build errors, then retry
cd ..
./scripts/deploy.sh [TIER] --android
```

### Play Console Issues

#### Error: "Release not created"

**Symptom:**
```
Upload succeeded but release not visible in Play Console
```

**Cause:** Upload completed but release not created.

**Solution:**
```bash
# Manually create release in Play Console
# 1. Navigate to correct testing track (Internal/Closed/Production)
# 2. Click "Create new release"
# 3. Select uploaded AAB
# 4. Add release notes
# 5. Review and roll out

# OR modify fastlane lane to create release:
upload_to_play_store(
  track: "internal",
  release_status: "completed",  # Or "draft"
  rollout: "1.0"  # 100% rollout
)
```

## Web Deployment Issues

### Build Errors

#### Error: "Module not found"

**Symptom:**
```
Module not found: Can't resolve '[MODULE]'
```

**Cause:** Missing dependency or incorrect import path.

**Solution:**
```bash
# Install missing dependency
npm install [MODULE]

# Or check import path
# Ensure relative paths are correct: '../config/buildConfig'

# Rebuild
npm run build:[TIER]
```

#### Error: "Environment variable not set"

**Symptom:**
```
process.env.REACT_APP_BUILD_TYPE is undefined
```

**Cause:** .env file not loaded or incorrect.

**Solution:**
```bash
# Verify .env file exists
ls .env.qual .env.stage .env.beta .env.prod

# Verify environment variable prefix (REACT_APP_ for Create React App)
cat .env.qual

# Ensure build script loads correct .env file
# package.json:
"web:build:qual": "env-cmd -f .env.qual react-scripts build"

# Or use dotenv
# Install: npm install dotenv-cli
"web:build:qual": "dotenv -e .env.qual react-scripts build"

# Rebuild
npm run web:build:qual
```

### Deployment Errors

#### Error: "Permission denied (publickey)"

**Symptom:**
```
Permission denied (publickey).
fatal: Could not read from remote repository.
```

**Cause:** SSH key not added to server or incorrect permissions.

**Solution:**
```bash
# Add SSH key to ssh-agent
ssh-add ~/.ssh/[YOUR_APP]_deploy_key

# Test SSH connection
ssh -T git@github.com  # For GitHub
ssh user@your-server.com  # For your server

# If permission denied, add public key to server
cat ~/.ssh/[YOUR_APP]_deploy_key.pub
# Copy output, add to server ~/.ssh/authorized_keys

# Retry deployment
./scripts/deploy.sh [TIER] --web
```

#### Error: "Disk space full"

**Symptom:**
```
No space left on device
```

**Cause:** Server disk full.

**Solution:**
```bash
# SSH to server
ssh user@your-server.com

# Check disk usage
df -h

# Clean up old deployments
rm -rf /path/to/old/deployment

# Clean up logs
rm /var/log/old-logs/*

# Retry deployment
./scripts/deploy.sh [TIER] --web
```

## BUILD_TYPE_ENV Issues

### BUILD_TYPE_ENV is "unknown"

**Symptom:**
```javascript
console.log(BUILD_TYPE_ENV); // "unknown"
```

**Cause:** Native module not exposing BUILD_TYPE_ENV correctly.

**Solution:**

**iOS:**
```bash
# Verify Info.plist contains BUILD_TYPE_ENV
plutil -p ios/[YOUR_APP]/Info.plist | grep BUILD_TYPE_ENV

# Should show: "BUILD_TYPE_ENV" => "qual" (or stage/beta/prod)

# If missing, add to Info.plist:
# <key>BUILD_TYPE_ENV</key>
# <string>$(BUILD_TYPE_ENV)</string>

# Rebuild
cd ios
xcodebuild clean
cd ..
./scripts/deploy.sh [TIER] --ios
```

**Android:**
```bash
# Verify BuildConfig contains BUILD_TYPE_ENV
cat android/app/build.gradle | grep BUILD_TYPE_ENV

# Should show: buildConfigField "String", "BUILD_TYPE_ENV", '"qual"'

# If missing, add to productFlavors

# Clean and rebuild
cd android
./gradlew clean
cd ..
./scripts/deploy.sh [TIER] --android
```

### API requests going to wrong endpoint

**Symptom:**
```
API requests going to prod instead of qual
```

**Cause:** BUILD_TYPE_ENV detection or routing logic incorrect.

**Solution:**
```javascript
// Verify BUILD_TYPE_ENV detection
import { NativeModules } from 'react-native';
console.log('Native BUILD_TYPE:', NativeModules.BuildConfigModule.BUILD_TYPE_ENV);

// Verify API endpoint calculation
import { API_ENDPOINT, BUILD_TYPE } from './config/buildConfig';
console.log('BUILD_TYPE:', BUILD_TYPE);
console.log('API_ENDPOINT:', API_ENDPOINT);

// If BUILD_TYPE correct but API_ENDPOINT wrong, check routing logic
// src/config/buildConfig.js should have correct endpoint mapping
```

## Fastlane Issues

### Error: "Fastlane command not found"

See "Fastlane Not Found" in General Deployment Issues above.

### Error: "Could not find action"

**Symptom:**
```
Could not find action, lane or variable '[ACTION_NAME]'
```

**Cause:** Action not installed or misspelled.

**Solution:**
```bash
# List available actions
fastlane actions

# If action missing, install plugin
fastlane add_plugin [PLUGIN_NAME]

# Example:
fastlane add_plugin increment_build_number

# Retry deployment
./scripts/deploy.sh [TIER] [PLATFORM]
```

### Error: "Fastlane session expired"

**Symptom:**
```
Your session has expired. Please log in.
```

**Cause:** Apple ID session expired.

**Solution:**
```bash
# Re-authenticate
fastlane fastlane-credentials add --username your@email.com

# Enter password
# Enter app-specific password

# Retry deployment
./scripts/deploy.sh [TIER] --ios
```

## Performance Issues

### iOS build extremely slow

**Symptom:** Xcode build takes 10+ minutes.

**Solution:**
```bash
# Clean derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# Disable Spotlight indexing for DerivedData
# System Preferences → Spotlight → Privacy → Add DerivedData folder

# Use new build system (Xcode 10+)
# File → Workspace Settings → Build System → New Build System

# Enable parallel builds
# Build Settings → Build Options → "Parallelize Build" = Yes

# Reduce Swift optimization for debug builds
# Build Settings → Swift Compiler - Code Generation → Optimization Level → None (-Onone)
```

### Android build extremely slow

**Symptom:** Gradle build takes 5+ minutes.

**Solution:**
```bash
# Increase Gradle memory (see "Out of memory" above)

# Enable parallel builds and daemon
# android/gradle.properties:
org.gradle.parallel=true
org.gradle.daemon=true
org.gradle.configureondemand=true

# Use Gradle build cache
org.gradle.caching=true

# Upgrade Gradle version
# android/gradle/wrapper/gradle-wrapper.properties:
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-all.zip

# Retry build
./scripts/deploy.sh [TIER] --android
```

## Getting More Help

### Check Logs

**iOS:**
```bash
# Xcode build log
open ios/[YOUR_APP].xcworkspace
# Build → Show Build Log

# Console log
# Window → Devices and Simulators → Select device → Open Console
```

**Android:**
```bash
# Gradle build log
cd android
./gradlew assembleQualRelease --stacktrace --info

# Logcat
adb logcat
```

### Enable Verbose Mode

```bash
# Fastlane verbose
fastlane [LANE] --verbose

# Gradle verbose
./gradlew [TASK] --info --stacktrace
```

### Search Error Messages

- Copy exact error message
- Search GitHub issues in relevant projects (react-native, fastlane, etc.)
- Search Stack Overflow

### Contact Support

**Apple Developer Support:**
- https://developer.apple.com/support/
- Phone support available

**Google Play Support:**
- https://support.google.com/googleplay/android-developer/
- Email support

**Community:**
- React Native Discord
- Stack Overflow (tag: react-native)
- Fastlane Slack

## StackMap-Specific Issues

### StackMap deployment issue patterns

StackMap encountered these recurring issues:

1. **iOS builds timing out** - Increased timeout to 600000ms (10 min)
2. **Android keystore path issues** - Used absolute paths in keystore.properties
3. **BUILD_TYPE_ENV not updating** - Required clean builds after xcconfig/gradle changes
4. **TestFlight processing delays** - Automated skip_waiting_for_build_processing
5. **Git lock file persistence** - Added trap handlers to release lock on script exit

See `/scripts/deploy/` for working solutions to these patterns.

## Preventive Measures

To avoid common issues:

1. **Version control all configs** - Commit xcconfig, build.gradle, Fastfile
2. **Backup keystores** - 3+ locations, test restoration quarterly
3. **Document secrets** - Password manager with clear labels
4. **Test in QUAL first** - Never deploy directly to STAGE/BETA/PROD
5. **Monitor builds** - Don't walk away during first deployment to new tier
6. **Keep dependencies updated** - npm, CocoaPods, Gradle, fastlane
7. **Regular clean builds** - Weekly clean builds catch dependency issues early

## Summary

Most deployment issues fall into these categories:

1. **Missing secrets** - Check keystore.properties, credentials, certificates
2. **Incorrect paths** - Use absolute paths or verify relative paths
3. **Stale caches** - Clean and rebuild resolves many issues
4. **Version conflicts** - Check versionCode/CFBundleVersion increments
5. **Permission errors** - Verify service account roles, code signing certificates

**General debugging approach:**
1. Read error message carefully
2. Check this guide for known issue
3. Search error message online
4. Enable verbose logging
5. Ask for help with full error details

**Time-saving tip:** Keep a deployment log noting issues and solutions specific to your project. Patterns will emerge.
