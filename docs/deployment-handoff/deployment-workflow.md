# Deployment Workflow Guide

Day-to-day deployment commands, validation procedures, verification steps, and tier usage guidelines for the 4-tier system.

## Overview

This guide covers how to deploy to each tier, what checks to perform, how to verify deployments, and when to use each tier. Once initial setup is complete, deployments should take 5-15 minutes per tier.

## Master Deployment Script

All deployments go through a single entry point: `scripts/deploy.sh`

### Basic Command Structure

```bash
./scripts/deploy.sh [TIER] [PLATFORM]

# TIER: qual, stage, beta, prod
# PLATFORM: --ios, --android, --web, --all (default: --all)
```

### Examples

```bash
# Deploy QUAL to all platforms
./scripts/deploy.sh qual --all

# Deploy STAGE to iOS only
./scripts/deploy.sh stage --ios

# Deploy BETA to Android only
./scripts/deploy.sh beta --android

# Deploy PROD to all platforms (requires confirmation)
./scripts/deploy.sh prod --all
```

## Tier Usage Guidelines

### QUAL - Development Testing

**Purpose**: Fast iteration during active development

**Frequency**: Multiple times per day (5-20+ deployments)

**Distribution**: Simulator/emulator only (never distributed through app stores)

**Database**: qual-api database (separate from production data)

**Use QUAL for:**
- Testing new features during development
- Debugging issues locally
- Verifying fixes before promoting to STAGE
- Experimenting with changes
- Multiple developers deploying simultaneously

**Deploy QUAL:**
```bash
./scripts/deploy.sh qual --all

# Verify on simulator/emulator
# Test new feature
# Deploy again if needed (no restrictions)
```

### STAGE - Internal Team Validation

**Purpose**: Internal team testing before external beta

**Frequency**: 1-3 times per week (after QUAL validation)

**Distribution**:
- iOS: TestFlight Internal Testing
- Android: Play Console Internal Testing

**Database**: stage-api database (shares database with QUAL for realistic testing)

**Use STAGE for:**
- Team-wide testing before beta release
- Product manager/QA validation
- Integration testing across platforms
- Performance testing on real devices
- Final checks before exposing to beta users

**Deploy STAGE:**
```bash
# Ensure PENDING_CHANGES.md is updated with changes
./scripts/deploy.sh stage --all

# Wait for TestFlight/Play Console processing (15-30 min)
# Distribute to internal testers
# Team tests on real devices
# Gather feedback before promoting to BETA
```

### BETA - Closed Beta Testing

**Purpose**: Controlled user testing with external beta testers

**Frequency**: 1-2 times per week (after STAGE validation)

**Distribution**:
- iOS: TestFlight External Testing (requires Apple review for first submission)
- Android: Play Console Closed Testing (no review required)

**Database**: beta-api database (uses production database for data consistency)

**Use BETA for:**
- Real user feedback before public release
- Identifying edge cases and bugs
- Performance testing at scale
- Validating new features with target audience
- Marketing/communications preparation

**Deploy BETA:**
```bash
# Ensure PENDING_CHANGES.md is updated with user-facing changes
./scripts/deploy.sh beta --all

# iOS: Wait for TestFlight processing (15-30 min)
# Android: Available immediately
# Monitor crash reports and user feedback
# Iterate if issues found (back to QUAL → STAGE → BETA)
```

**First BETA Deployment (iOS):**
- Submit for TestFlight review (1-2 days)
- Provide test account if app requires login
- Answer export compliance questions
- Subsequent BETA builds auto-distribute (no re-review)

### PROD - Production Release

**Purpose**: Public release to all users

**Frequency**: Weekly or bi-weekly (after BETA validation)

**Distribution**:
- iOS: App Store (requires manual submission for review)
- Android: Play Console Production (requires manual rollout)

**Database**: prod-api database (production data)

**Use PROD for:**
- Stable, well-tested releases
- Public availability
- App Store/Play Store distribution

**Deploy PROD:**
```bash
# Final check: PENDING_CHANGES.md is complete and accurate
./scripts/deploy.sh prod --all

# iOS: Upload completes, then manually submit for review in App Store Connect
# Android: Upload completes, then manually roll out in Play Console
# Monitor crash reports and user feedback closely
# Prepare rollback plan if critical issues arise
```

**Production Deployment Checklist:**
- [ ] All features tested in QUAL, STAGE, and BETA
- [ ] No critical bugs reported in BETA
- [ ] PENDING_CHANGES.md updated with release notes
- [ ] Team notified of deployment
- [ ] Rollback plan prepared
- [ ] Monitoring and alerts configured

## Pre-Deployment Validation

The master script validates before deploying. You can also run validation manually.

### Validation Checks

1. **Git Status**: Working directory must be clean (no uncommitted changes)
   ```bash
   git status
   # Should show: "nothing to commit, working tree clean"
   ```

2. **Dependency Check**: Node modules and native dependencies up to date
   ```bash
   npm install
   cd ios && pod install && cd ..
   ```

3. **Build Configuration**: Verify BUILD_TYPE_ENV for each tier
   ```bash
   # iOS: Check xcconfig files
   cat ios/Qual.xcconfig | grep BUILD_TYPE_ENV
   # Expected: BUILD_TYPE_ENV = qual

   # Android: Check build.gradle
   cat android/app/build.gradle | grep BUILD_TYPE_ENV
   # Expected: buildConfigField "String", "BUILD_TYPE_ENV", '"qual"'
   ```

4. **Credentials**: Verify signing certificates and provisioning profiles
   ```bash
   # iOS: Check code signing
   security find-identity -v -p codesigning

   # Android: Check keystore exists
   ls -la ~/keystores/[YOUR_APP]-production.keystore
   ```

5. **PENDING_CHANGES.md**: Must be updated with deployment notes
   ```bash
   # Verify file exists and has recent changes
   cat PENDING_CHANGES.md
   ```

### Manual Validation Script

Create `scripts/deploy/lib/validation.sh`:

```bash
#!/bin/bash

validate_deployment() {
  local tier="$1"

  echo "Validating deployment for $tier..."

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

  # Check Node modules
  if [[ ! -d "node_modules" ]]; then
    echo "ERROR: node_modules not found. Run: npm install"
    exit 1
  fi

  # iOS checks
  if [[ ! -f "ios/Podfile.lock" ]]; then
    echo "ERROR: iOS pods not installed. Run: cd ios && pod install"
    exit 1
  fi

  # Android checks
  if [[ ! -f "android/keystore.properties" ]]; then
    echo "ERROR: android/keystore.properties not found"
    exit 1
  fi

  echo "Validation passed!"
}
```

**StackMap Reference**: See `/scripts/deploy/lib/validation.sh` for complete implementation.

## Deployment Process

### Step 1: Prepare Changes

```bash
# Commit your changes
git add .
git commit -m "Add feature X"

# Update PENDING_CHANGES.md with deployment notes
# Format:
## Title: [Descriptive title]
### Changes Made:
- Change 1
- Change 2
- Change 3
```

### Step 2: Run Deployment Script

```bash
# Deploy to desired tier
./scripts/deploy.sh [TIER] [PLATFORM]

# Example: Deploy QUAL to iOS only
./scripts/deploy.sh qual --ios
```

### Step 3: Monitor Build Progress

The script will:
1. Validate pre-deployment checks
2. Acquire deployment lock (prevents concurrent deploys)
3. Run tier-specific deployment script
4. Call fastlane lanes for each platform
5. Build and upload (if applicable)
6. Generate deployment summary
7. Release deployment lock

**Build times:**
- iOS: 5-10 minutes
- Android: 2-5 minutes
- Web: 1-2 minutes

### Step 4: Verify Build Success

Check build output for errors:

```bash
# iOS: Look for "Build Succeeded"
# Android: Look for "BUILD SUCCESSFUL"
# Web: Look for "Compiled successfully"
```

### Step 5: Distribute and Test

**QUAL:**
```bash
# iOS: Install on simulator
xcrun simctl install booted ios/build/qual/[YOUR_APP].app

# Android: Install on emulator
adb install android/app/build/outputs/apk/qual/release/app-qual-release.apk

# Test the build
# - Verify BUILD_TYPE_ENV: qual
# - Verify API endpoint: /qual/api
# - Test new features
```

**STAGE:**
```bash
# iOS: Check TestFlight processing
# - App Store Connect → TestFlight → Builds
# - Wait for "Ready to Test" status (15-30 min)
# - Distribute to "Internal Testers" group

# Android: Check Play Console processing
# - Play Console → Internal Testing
# - Build should be available immediately
# - Share link with internal testers

# Team tests on real devices
```

**BETA:**
```bash
# iOS: Check TestFlight processing
# - App Store Connect → TestFlight → Builds
# - Distribute to "Beta Testers" group (External)
# - Testers receive email notification

# Android: Check Play Console processing
# - Play Console → Closed Testing
# - Beta testers can install immediately via link

# Monitor feedback from beta testers
```

**PROD:**
```bash
# iOS: Submit for review
# - App Store Connect → App Store → Prepare for Submission
# - Fill in release notes (from PENDING_CHANGES.md)
# - Submit for review
# - Wait for Apple review (1-2 days)

# Android: Roll out
# - Play Console → Production → Create new release
# - Fill in release notes (from PENDING_CHANGES.md)
# - Roll out percentage (e.g., 10% → 50% → 100%)
# - Monitor crash reports during rollout

# Monitor production closely
```

## Verification Steps

### Verify BUILD_TYPE_ENV

**In app logs:**
```javascript
// Check console output on app start
console.log('BUILD_TYPE_ENV:', NativeModules.BuildConfigModule.BUILD_TYPE_ENV);
```

**Expected values:**
- QUAL: `qual`
- STAGE: `stage`
- BETA: `beta`
- PROD: `prod`

### Verify API Endpoint

**In app logs:**
```javascript
console.log('API_ENDPOINT:', API_ENDPOINT);
```

**Expected values:**
- QUAL: `https://[YOUR_DOMAIN]/qual/api`
- STAGE: `https://[YOUR_DOMAIN]/stage/api`
- BETA: `https://[YOUR_DOMAIN]/beta/api`
- PROD: `https://[YOUR_DOMAIN]/api`

### Verify Bundle ID / Package Name

**iOS:**
```bash
# Extract bundle ID from IPA
unzip -p build/stage/[YOUR_APP].ipa Payload/[YOUR_APP].app/Info.plist | plutil -p - | grep CFBundleIdentifier

# Expected:
# QUAL: com.[YOUR_COMPANY].[YOUR_APP].qual
# STAGE/BETA/PROD: com.[YOUR_COMPANY].[YOUR_APP]
```

**Android:**
```bash
# Extract package name from APK/AAB
aapt dump badging android/app/build/outputs/apk/qual/release/app-qual-release.apk | grep package

# Expected:
# QUAL: com.[YOUR_COMPANY].[YOUR_APP].qual
# STAGE/BETA/PROD: com.[YOUR_COMPANY].[YOUR_APP]
```

### Verify Version Numbers

**iOS:**
```bash
# Check Info.plist
plutil -p ios/build/stage/[YOUR_APP].app/Info.plist | grep CFBundleShortVersionString
plutil -p ios/build/stage/[YOUR_APP].app/Info.plist | grep CFBundleVersion
```

**Android:**
```bash
# Check AndroidManifest.xml
aapt dump badging android/app/build/outputs/apk/qual/release/app-qual-release.apk | grep versionName
```

## Deployment Locking

The master script uses a lock file to prevent concurrent deployments.

**Lock file location:** `/tmp/[YOUR_APP]-deployment.lock`

**How it works:**
1. Script checks for lock file
2. If exists, abort with message: "Deployment already in progress"
3. If not exists, create lock file
4. Perform deployment
5. Remove lock file on success or failure

**Manual unlock (if script crashes):**
```bash
rm /tmp/[YOUR_APP]-deployment.lock
```

## Deployment Summaries

After each deployment, the script generates an HTML summary.

**Summary location:** `deployment-summary-[tier]-[platform].html`

**Summary includes:**
- Tier and platform
- Version and build number
- Timestamp
- Changes from PENDING_CHANGES.md
- Build output
- Success/failure status

**View summary:**
```bash
open deployment-summary-stage-ios.html
```

**StackMap Reference**: See `/scripts/deploy/lib/reporting.sh` for summary generation.

## Common Deployment Scenarios

### Scenario 1: New Feature Development

1. Develop feature locally
2. Deploy to QUAL multiple times for testing
3. When stable, deploy to STAGE for team validation
4. Team approves, deploy to BETA for user testing
5. Beta testers approve, deploy to PROD for public release

**Timeline:** 1-2 weeks (varies by feature complexity)

### Scenario 2: Bug Fix

1. Reproduce bug in QUAL
2. Fix bug, deploy to QUAL to verify
3. Deploy to STAGE for regression testing
4. If critical, fast-track to BETA and PROD
5. If not critical, include in next regular release

**Timeline:** 1-3 days for critical bugs, 1-2 weeks for non-critical

### Scenario 3: Hotfix

1. Identify critical production bug
2. Create hotfix branch from prod
3. Fix bug, deploy to QUAL to verify
4. Deploy to STAGE for quick validation
5. Deploy to BETA for smoke testing
6. Deploy to PROD immediately
7. Submit for expedited review (iOS) or emergency rollout (Android)

**Timeline:** 1-2 days (iOS review can be expedited in emergencies)

### Scenario 4: Platform-Specific Update

1. Update iOS or Android code (not shared code)
2. Deploy platform-specific build to QUAL
3. Deploy to STAGE for team testing
4. Deploy to BETA for user testing
5. Deploy to PROD

**Commands:**
```bash
# iOS-only update
./scripts/deploy.sh qual --ios
./scripts/deploy.sh stage --ios
./scripts/deploy.sh beta --ios
./scripts/deploy.sh prod --ios

# Android-only update
./scripts/deploy.sh qual --android
./scripts/deploy.sh stage --android
./scripts/deploy.sh beta --android
./scripts/deploy.sh prod --android
```

## Rollback Procedures

### QUAL Rollback

No rollback needed - just deploy previous version:
```bash
git checkout [PREVIOUS_COMMIT]
./scripts/deploy.sh qual --all
```

### STAGE/BETA Rollback

Re-deploy previous version:
```bash
git checkout [PREVIOUS_TAG]
./scripts/deploy.sh stage --all
./scripts/deploy.sh beta --all
```

### PROD Rollback

**iOS:**
1. App Store Connect → App Store → Version History
2. Remove current version from sale (if critical)
3. Submit previous version for review
4. Wait for review (1-2 days, can request expedited review)

**Android:**
1. Play Console → Production → Releases
2. Halt rollout immediately
3. Roll back to previous version
4. Previous version available within hours

**Alternative: Hotfix Forward**
1. Fix critical bug
2. Deploy hotfix through QUAL → STAGE → BETA → PROD
3. Usually faster than iOS rollback

## Monitoring Post-Deployment

### Crash Reports

**iOS:**
- App Store Connect → TestFlight/App Store → Crashes
- Check within 24 hours of deployment

**Android:**
- Play Console → Quality → Crashes
- Check within hours of deployment

### User Feedback

**iOS:**
- TestFlight feedback (STAGE/BETA)
- App Store reviews (PROD)

**Android:**
- Play Console reviews (all tiers)
- Closed testing feedback (STAGE/BETA)

### Analytics

Monitor key metrics:
- App launches
- Feature usage
- API errors
- Performance (load times, memory)

## Troubleshooting Deployments

### Build fails with code signing error (iOS)

**Solution:**
```bash
# Re-sync certificates
cd ios
fastlane match appstore

# Or manually update in Xcode
# Xcode → Signing & Capabilities → Automatically manage signing
```

### Upload fails with "Package name mismatch" (Android)

**Solution:** First upload to Play Console must use base package name (no suffix). Upload STAGE or PROD first, never QUAL.

### TestFlight processing stuck

**Solution:** Wait 30-60 minutes. If still stuck, contact Apple Developer Support.

### Deployment lock not released

**Solution:**
```bash
rm /tmp/[YOUR_APP]-deployment.lock
```

### PENDING_CHANGES.md not committing

**Solution:** Ensure it's not in .gitignore:
```bash
git add -f PENDING_CHANGES.md
git commit -m "Update pending changes"
```

## Best Practices

1. **Always update PENDING_CHANGES.md** before deploying STAGE/BETA/PROD
2. **Test in QUAL first** before deploying to higher tiers
3. **Never skip tiers** (except hotfixes) - always promote QUAL → STAGE → BETA → PROD
4. **Deploy in off-hours** for PROD to minimize user impact
5. **Monitor closely** after each PROD deployment
6. **Communicate with team** before STAGE/BETA/PROD deployments
7. **Keep deployment notes** for future reference
8. **Version tags** in git for each PROD release

## Next Steps

After understanding deployment workflow:

1. Review [troubleshooting.md](./troubleshooting.md) for common issues
2. See [secrets-and-credentials.md](./secrets-and-credentials.md) for credential management
3. Check [reference-implementations.md](./reference-implementations.md) for StackMap examples

## StackMap Reference Files

Complete working deployment scripts:

- `/scripts/deploy.sh` (master deployment script)
- `/scripts/deploy/qual_deploy.sh` (QUAL-specific)
- `/scripts/deploy/deploy_stage.sh` (STAGE-specific)
- `/scripts/deploy/deploy_beta.sh` (BETA-specific)
- `/scripts/deploy/prod_deploy.sh` (PROD-specific)
- `/scripts/deploy/lib/validation.sh` (pre-deployment checks)
- `/scripts/deploy/lib/reporting.sh` (deployment summaries)

See [reference-implementations.md](./reference-implementations.md) for detailed code examples.
