# QUAL Deployment Troubleshooting Guide

## Common Issues and Solutions

### Issue: Test Task Not Found

**Error Message:**
```
Task 'testTier1Critical' not found in project ':app'
```

**Cause:**
- Gradle test tasks not properly configured in `android/app/build.gradle.kts`
- Typo in task name
- Gradle sync failed

**Solution:**
```bash
# 1. Verify Gradle configuration
cd android
./gradlew tasks --all | grep test

# 2. Check for tiered test tasks
./gradlew tasks | grep -i tier

# 3. Clean and rebuild
./gradlew clean
./gradlew build

# 4. If tasks are missing, check build.gradle.kts
# Look for Android test source sets and task definitions
```

**Prevention:**
- Always sync Gradle files after modifying build configuration
- Run `./gradlew tasks` periodically to verify available tasks

### Issue: iOS Simulator Not Found

**Error Message:**
```
[ERROR] No iOS simulators found
[ERROR] Install simulators via Xcode or set IOS_SIMULATOR_NAME environment variable
```

**Cause:**
- No simulators installed
- Simulators filtered out (not iPhone/iPad)
- Xcode not properly installed

**Solution:**
```bash
# 1. List all simulators
xcrun simctl list devices

# 2. Install recommended simulators
# Open Xcode > Settings > Platforms > iOS
# Download iPhone 16, 15, or 14 simulator runtimes

# 3. Create a new simulator manually
xcrun simctl create "iPhone 16" "iPhone 16"

# 4. Override simulator selection
IOS_SIMULATOR_NAME="iPhone 15" ./deploy/deploy_qual.sh ios
```

**Prevention:**
- Keep at least one iPhone simulator installed
- Recommended: iPhone 16, 15, or 14
- Verify after Xcode updates: `xcrun simctl list devices`

### Issue: Git Lock File Error

**Error Message:**
```
fatal: Unable to create '.git/index.lock': File exists
```

**Cause:**
- Previous git operation crashed or was interrupted
- Stale lock file from another process

**Solution:**
```bash
# 1. Remove the lock file
rm -f .git/index.lock

# 2. Verify git status
git status

# 3. Retry deployment
./deploy/deploy_qual.sh both
```

**Prevention:**
- Don't interrupt git operations with Ctrl+C
- Wait for processes to complete fully

### Issue: jq Not Found

**Error Message:**
```
[ERROR] Missing required tools: jq (install via: brew install jq)
```

**Cause:**
- jq JSON processor not installed
- Required for `.build_number` file management

**Solution:**
```bash
# Install via Homebrew
brew install jq

# Verify installation
jq --version

# Retry deployment
./deploy/deploy_qual.sh both
```

**Prevention:**
- Run `./deploy/deploy_qual.sh` with no arguments to check prerequisites
- Install all required tools during initial setup

### Issue: SonarCloud Timeout

**Error Message:**
```
[WARN] SonarCloud analysis failed - continuing deployment
ERROR: Connection timeout to sonarcloud.io
```

**Cause:**
- Network connectivity issues
- SonarCloud service temporarily unavailable
- Firewall blocking requests

**Solution:**
```bash
# Skip SonarCloud analysis
SKIP_SONAR=true ./deploy/deploy_qual.sh both

# Or check network connectivity
curl -I https://sonarcloud.io

# Retry after network stabilizes
./deploy/deploy_qual.sh both
```

**Note:** SonarCloud failures are Tier 3 (warning only) and don't block deployment. The script will continue automatically.

**Prevention:**
- Use `SKIP_SONAR=true` in environments with restricted network access
- Configure proxy settings if behind corporate firewall

### Issue: Network Connectivity Problems

**Error Message:**
```
Failed to download gradle dependencies
Could not resolve com.android.tools.build:gradle:X.X.X
```

**Cause:**
- Network outage
- Maven/Gradle repositories unreachable
- Proxy configuration issues

**Solution:**
```bash
# 1. Check network connectivity
ping google.com

# 2. Clear Gradle cache
rm -rf ~/.gradle/caches

# 3. Retry with offline mode if dependencies cached
cd android
./gradlew build --offline

# 4. Configure proxy (if needed)
# Add to ~/.gradle/gradle.properties:
# systemProp.http.proxyHost=proxy.company.com
# systemProp.http.proxyPort=8080
```

**Workaround:**
- Use `SKIP_SONAR=true` to skip external service calls
- Build offline if dependencies are already cached
- Wait for network to recover

### Issue: Fastlane Command Not Found

**Error Message:**
```
bundle exec fastlane: command not found
```

**Cause:**
- Ruby gems not installed
- Bundler not set up
- Wrong directory

**Solution:**
```bash
# 1. Install bundler
gem install bundler

# 2. Install project gems
cd /path/to/SmilePile
bundle install

# 3. Verify Fastlane is available
bundle exec fastlane --version

# 4. Retry deployment
./deploy/deploy_qual.sh both
```

**Prevention:**
- Run `bundle install` after cloning repository
- Keep Gemfile.lock committed in version control

### Issue: Android Emulator Not Starting

**Error Message:**
```
[WARN] No Android devices found
[INFO] Attempting to start Android emulator...
[ERROR] No Android devices available for deployment
```

**Cause:**
- No emulators configured
- Emulator failed to start
- ANDROID_HOME not set

**Solution:**
```bash
# 1. List available emulators
emulator -list-avds

# 2. Create a new emulator via Android Studio
# Tools > Device Manager > Create Device

# 3. Start emulator manually
emulator -avd Pixel_5_API_34 &

# 4. Wait for boot
adb wait-for-device

# 5. Retry deployment
./deploy/deploy_qual.sh android
```

**Alternative:**
- Connect a physical Android device via USB
- Enable USB debugging on device
- Run `adb devices` to verify connection

### Issue: iOS Simulator Won't Boot

**Error Message:**
```
[WARN] Failed to boot simulator <UUID>, trying already booted
```

**Cause:**
- Simulator already booted
- Simulator crashed
- CoreSimulator service issues

**Solution:**
```bash
# 1. Check simulator status
xcrun simctl list devices | grep Booted

# 2. Shutdown all simulators
xcrun simctl shutdown all

# 3. Boot specific simulator
xcrun simctl boot <simulator-uuid>

# 4. Or open Simulator.app and boot manually
open -a Simulator

# 5. Restart CoreSimulator service if needed
sudo killall -9 com.apple.CoreSimulator.CoreSimulatorService
```

**Prevention:**
- Close Simulator.app between deployments
- Don't run too many simulators simultaneously

## Platform-Specific Issues

### iOS Platform Issues

#### Code Signing Error

**Error:**
```
Code signing failed
No valid code signing certificates found
```

**Solution:**
```bash
# QUAL uses automatic signing - no certificates needed
# But verify Xcode configuration:

# 1. Open project in Xcode
open ios/SmilePile.xcworkspace

# 2. Select SmilePile Qual scheme
# 3. Go to Signing & Capabilities tab
# 4. Ensure "Automatically manage signing" is checked
# 5. Select a development team (personal or organization)
```

#### Provisioning Profile Issues

**Error:**
```
Failed to create provisioning profile
```

**Solution:**
```bash
# QUAL builds for simulator only - no provisioning needed
# If error persists:

# 1. Clean DerivedData
rm -rf ~/Library/Developer/Xcode/DerivedData

# 2. Clean build folder
cd ios
xcodebuild clean -workspace SmilePile.xcworkspace -scheme "SmilePile Qual"

# 3. Rebuild
./deploy/deploy_qual.sh ios
```

#### Simulator Installation Path Issues

**Error:**
```
The operation couldn't be completed. (com.apple.SimulatorKit.StderrDiagnosticError error 4)
```

**Solution:**
```bash
# 1. Verify app path exists
ls -la "ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"

# 2. Clean and rebuild
cd ios
bundle exec fastlane qual_ios

# 3. Install manually
xcrun simctl install <simulator-id> "DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"
```

### Android Platform Issues

#### Gradle Build Failed

**Error:**
```
Execution failed for task ':app:compileQualDebugKotlin'
```

**Solution:**
```bash
# 1. Clean build
cd android
./gradlew clean

# 2. Invalidate caches
./gradlew --stop
rm -rf .gradle

# 3. Rebuild
./gradlew app:assembleQualDebug

# 4. Check for compilation errors in output
```

#### Emulator Connection Issues

**Error:**
```
adb: device offline
```

**Solution:**
```bash
# 1. Restart adb server
adb kill-server
adb start-server

# 2. Check devices
adb devices

# 3. Reconnect device
adb reconnect

# 4. Retry deployment
./deploy/deploy_qual.sh android
```

#### Keystore Not Found

**Error:**
```
Keystore file '/path/to/debug.keystore' not found
```

**Solution:**
```bash
# QUAL uses debug keystore, auto-generated by Android SDK
# Generate if missing:

keytool -genkey -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android

# Rebuild
./deploy/deploy_qual.sh android
```

#### Package Conflicts

**Error:**
```
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

**Solution:**
```bash
# Old version with different signature installed
# Uninstall first:

adb uninstall com.smilepile.qual

# Retry installation
./deploy/deploy_qual.sh android
```

## Quality Gate Failures

### Tier 1 Failure (CRITICAL - BLOCKS DEPLOYMENT)

**Error:**
```
[ERROR] CRITICAL FAILURE: Tier 1 tests failed
[ERROR] Analyzing failures...
[ERROR] Deployment ABORTED.
```

**What It Means:**
- Security tests or data integrity tests failed
- These are critical issues that MUST be fixed
- Deployment will not proceed

**Solution Process:**

1. **Review Test Output:**
   ```bash
   # Android
   cat /tmp/tier1-android-output.txt

   # iOS
   cat /tmp/tier1-ios-output.txt
   ```

2. **Identify Failing Tests:**
   ```bash
   # Look for "FAILED" markers
   grep -i "failed\|error" /tmp/tier1-android-output.txt
   ```

3. **Fix the Issue:**
   - Security vulnerabilities must be addressed immediately
   - Data corruption issues are critical
   - Don't skip these tests - fix the root cause

4. **Retest:**
   ```bash
   # Run tests again
   cd android
   ./gradlew app:testTier1Critical

   # Once passing, retry deployment
   ./deploy/deploy_qual.sh both
   ```

**Never Use:** `SKIP_TESTS=true` to bypass Tier 1 failures

### Tier 2 Failure (IMPORTANT - BLOCKS DEPLOYMENT)

**Error:**
```
[ERROR] IMPORTANT FAILURE: Tier 2 tests failed
[ERROR] Analyzing failures...
[ERROR] Deployment ABORTED.
```

**What It Means:**
- ViewModel, Repository, or business logic tests failed
- These are important functional issues
- Deployment will not proceed

**Solution Process:**

1. **Check Logs:**
   ```bash
   cat /tmp/tier2-android-output.txt
   ```

2. **Identify Root Cause:**
   - Logic errors in ViewModels
   - Repository data access issues
   - Dependency injection problems

3. **Fix and Verify:**
   ```bash
   # Fix the code
   # Run specific test
   cd android
   ./gradlew app:testTier2Important --tests YourFailingTest

   # Full test suite
   ./gradlew app:testTier2Important
   ```

4. **Redeploy:**
   ```bash
   ./deploy/deploy_qual.sh both
   ```

### Tier 3 Failure (UI - WARNING ONLY)

**Warning:**
```
[WARN] WARNING: Tier 3 UI tests failed
[WARN] Analyzing failures...
[WARN] These tests verify UI components and user flows.
[WARN] Review failures but deployment will continue.
```

**What It Means:**
- UI component or integration tests failed
- Tests can be flaky due to timing issues
- Deployment continues with warning

**Solution Process:**

1. **Review Warnings:**
   ```bash
   cat /tmp/tier3-android-output.txt
   ```

2. **Assess Severity:**
   - Real UI bug? Fix it
   - Flaky test? Track as tech debt
   - Timing issue? Adjust test delays

3. **Track as Tech Debt:**
   - Script automatically creates story in `/backlog/tech-debt/`
   - Review and prioritize for future sprint

4. **Optional: Fix Immediately:**
   ```bash
   # Fix UI issue
   # Rerun test
   cd android
   ./gradlew app:testTier3UI

   # Redeploy
   ./deploy/deploy_qual.sh both
   ```

**Acceptable:** Let deployment continue if UI issue is minor

### SonarCloud Quality Gate Failure

**Warning:**
```
[WARN] SonarCloud analysis failed - continuing deployment
```

**What It Means:**
- Code quality metrics below threshold
- Security hotspots detected
- Code coverage insufficient
- Tier 3 - won't block deployment

**Solution Process:**

1. **Check SonarCloud Dashboard:**
   ```
   https://sonarcloud.io/project/overview?id=ajstack22_SmilePile
   ```

2. **Review Issues:**
   - Critical security hotspots → Fix immediately
   - Code smells → Track as tech debt
   - Low coverage → Add tests incrementally

3. **Improve Over Time:**
   - Don't let it block your work
   - Address critical issues first
   - Gradually improve coverage

4. **Skip if Needed:**
   ```bash
   SKIP_SONAR=true ./deploy/deploy_qual.sh both
   ```

## Recovery Procedures

### Partial Deployment Failure

**Scenario:** Android deployed successfully, iOS failed

**Recovery Steps:**
```bash
# 1. Check what failed
cat /tmp/tier1-ios-output.txt

# 2. Fix iOS-specific issue

# 3. Deploy iOS only
./deploy/deploy_qual.sh ios

# Result: Both platforms now deployed
```

### Build Corruption

**Scenario:** Builds fail with strange errors after working previously

**Recovery Steps:**
```bash
# 1. Clean all build artifacts
cd android
./gradlew clean
rm -rf build app/build .gradle

cd ../ios
xcodebuild clean -workspace SmilePile.xcworkspace -scheme "SmilePile Qual"
rm -rf DerivedData ~/Library/Developer/Xcode/DerivedData

# 2. Clear dependency caches
rm -rf ~/.gradle/caches
rm -rf ~/Library/Caches/CocoaPods

# 3. Reinstall dependencies
cd ../android
./gradlew build --refresh-dependencies

cd ../ios
pod deintegrate
pod install

# 4. Retry deployment
cd ..
./deploy/deploy_qual.sh both
```

### Version Number Conflict

**Scenario:** Version number already exists or is incorrect

**Recovery Steps:**
```bash
# 1. Check current version
cat .build_number | jq

# 2. Manually adjust if needed
# Edit .build_number with correct version

# 3. Verify
cat .build_number | jq .current_version

# 4. Redeploy
./deploy/deploy_qual.sh both
```

### Git Commit Failure

**Scenario:** Tests passed, build succeeded, but commit failed

**Recovery Steps:**
```bash
# 1. Check what's uncommitted
git status

# 2. Verify no conflicts
git pull

# 3. Manually commit
git add -A
git commit -m "qual: Deploy both - v$(cat .build_number | jq -r .current_version)"
git tag -a "v$(cat .build_number | jq -r .current_version)" -m "Release version"
git push origin main --tags

# Or skip commit if not needed
SKIP_COMMIT=true ./deploy/deploy_qual.sh both
```

### Rollback to Previous Version

**Scenario:** Need to revert to previous deployment

**Recovery Steps:**
```bash
# 1. Find previous version
git tag --sort=-version:refname | head -5

# 2. Checkout previous tag
git checkout v25.10.15.001

# 3. Redeploy from that version
./deploy/deploy_qual.sh both

# 4. Return to main branch
git checkout main

# 5. Revert commit if needed
git revert HEAD
git push origin main
```

## Getting Help

### Check Logs

**Deployment Logs:**
```bash
# Latest deployment log
ls -lt deploy/logs/ | head -1

# View full log
cat deploy/logs/deploy_qual_YYYYMMDD_HHMMSS.log

# Search for errors
grep -i "error\|failed" deploy/logs/deploy_qual_*.log
```

**Test Output:**
```bash
# Android test results
open android/app/build/reports/tests/testTier1CriticalDebugUnitTest/index.html

# iOS test results
open ios/test_results_*.xcresult
```

### Verbose Mode

```bash
# Run with verbose logging
set -x
./deploy/deploy_qual.sh both
set +x
```

### Dry Run for Debugging

```bash
# See what would happen without executing
DRY_RUN=true ./deploy/deploy_qual.sh both
```

### Review Error Messages

**Color-Coded Output:**
- Red `[ERROR]` - Critical issues, deployment aborted
- Yellow `[WARN]` - Warnings, deployment continues
- Green `[SUCCESS]` - Successful operations
- Blue `[INFO]` - Informational messages

### Consult Wave Evidence

**Wave 6 Documentation:**
```
wave-evidence/wave-6/
├── 01-research-findings.md    - Known issues and solutions
├── 07-testing-report.md       - Test scenarios and results
├── 08-acceptance-criteria-validation.md - Validation process
```

### Reference Deployment Documentation

- [QUAL Deployment Guide](qual-deployment-guide.md) - Full deployment guide
- [Quality Gates Documentation](quality-gates.md) - Understanding test tiers
- [Deployment Roadmap](DEPLOYMENT_ROADMAP.md) - Overall strategy

### Contact Team

If issues persist after trying solutions:

1. **Check Git History:**
   ```bash
   git log --oneline -10
   # See recent changes that might have broken deployment
   ```

2. **Review Recent PRs:**
   ```bash
   gh pr list --state merged --limit 5
   ```

3. **Ask for Help:**
   - Describe the error message
   - Share relevant log files
   - Mention what you've already tried

## Prevention Best Practices

### Regular Maintenance

1. **Keep Tools Updated:**
   ```bash
   brew upgrade
   bundle update
   ```

2. **Clean Build Artifacts Weekly:**
   ```bash
   ./gradlew clean
   xcodebuild clean
   ```

3. **Monitor Disk Space:**
   ```bash
   df -h
   # DerivedData and .gradle can consume GBs
   ```

### Before Deployment

1. **Pull Latest Changes:**
   ```bash
   git pull origin main
   ```

2. **Check Prerequisites:**
   ```bash
   ./deploy/deploy_qual.sh --help
   # Verifies all tools are available
   ```

3. **Run Tests Locally First:**
   ```bash
   cd android && ./gradlew testTier1Critical
   cd ../ios && ./ios/scripts/run-tier-tests.sh tier1
   ```

### After Deployment

1. **Verify App Launches:**
   - Open app on deployed devices
   - Check for crashes
   - Test key functionality

2. **Review Test Reports:**
   - Check coverage percentages
   - Address test failures tracked as tech debt

3. **Monitor Git Status:**
   ```bash
   git status
   # Should be clean after deployment
   ```

---

**Document Version:** 1.0
**Last Updated:** 2025-10-15
**Maintained By:** SmilePile Development Team
