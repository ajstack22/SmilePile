# Wave 8 Phase 3: Technical Planning - First STAGE Deployment

## 1. Executive Summary

This plan provides step-by-step instructions for executing SmilePile's first real app store deployment - uploading STAGE builds to TestFlight Internal Testing and Play Console Internal Testing. All infrastructure from Waves 1-7 is validated and ready. The deploy_stage.sh script will orchestrate the entire process including quality gates, version management, Fastlane uploads, and git commits. Estimated active time: 45-60 minutes. Total elapsed time: 50-110 minutes (mostly iOS processing wait). Risk level: LOW. Three critical prerequisites must be verified before execution: apps exist in both stores and testing tracks are configured.

---

## 2. Pre-Deployment Checklist

**CRITICAL Prerequisites (MUST verify before execution)**:
- [ ] SmilePile app exists in App Store Connect with bundle ID: com.smilepile
- [ ] "Internal Testers" group configured in TestFlight
- [ ] SmilePile app exists in Google Play Console with package: com.smilepile
- [ ] "Internal testing" track enabled in Play Console
- [ ] At least one team member added to each testing group

**Environment Prerequisites**:
- [ ] macOS system with Xcode installed (for iOS builds)
- [ ] ANDROID_HOME environment variable set
- [ ] Fastlane and bundler installed
- [ ] Minimum 5GB free disk space

**Credential Prerequisites**:
- [ ] App Store Connect API key exists: ~/.fastlane/AuthKey_BJAC3957M4.p8 (600 permissions)
- [ ] Play Store service account exists: ~/.fastlane/play-store-credentials.json (600 permissions)
- [ ] Android production keystore exists: ~/keystores/smilepile-production.keystore (600 permissions)
- [ ] Apple Distribution certificate installed in keychain

**Pre-Flight Validation**:
- [ ] Current git status reviewed (uncommitted changes will be included in deployment commit)
- [ ] Recent QUAL deployment reviewed for red flags
- [ ] 90-minute time window scheduled
- [ ] Team notified of upcoming deployment

---

## 3. Deployment Steps

### Step 1: Verify App Store Prerequisites (10-15 minutes)

**iOS Verification**:
```bash
# Open App Store Connect and verify
# URL: https://appstoreconnect.apple.com/apps
# Check: SmilePile app exists with bundle ID com.smilepile
# Check: TestFlight > Internal Testing > "Internal Testers" group exists
# Check: At least one team member added to group
```

**Android Verification**:
```bash
# Open Play Console and verify
# URL: https://play.google.com/console
# Check: SmilePile app exists with package name com.smilepile
# Check: Testing > Internal testing track is enabled
# Check: Service account has "Release Manager" role
```

**If apps don't exist**: Create them now (15-25 minutes per platform). Deployment cannot proceed without apps in place.

### Step 2: Verify Local Prerequisites (5 minutes)

```bash
# Check Xcode schemes
xcodebuild -project /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj -list | grep -i stage

# Verify signing identity
security find-identity -v -p codesigning | grep "Apple Distribution"

# Check App Store Connect API key
ls -lh ~/.fastlane/AuthKey_BJAC3957M4.p8
# Expected: -rw------- 257 bytes

# Check Android keystore
ls -lh ~/keystores/smilepile-production.keystore
# Expected: -rw------- 4.3KB

# Check service account
ls -lh ~/.fastlane/play-store-credentials.json
# Expected: -rw------- 2.4KB

# Check disk space (requires 5GB minimum)
df -h /Users/adamstack/SmilePile
```

### Step 3: Execute Dry Run (5 minutes)

```bash
cd /Users/adamstack/SmilePile
DRY_RUN=true ./deploy/deploy_stage.sh both
```

**Expected output**:
- All prerequisites pass
- Version numbers displayed
- Test execution simulated
- Fastlane commands shown (but not executed)
- Git operations shown (but not executed)
- No actual uploads occur

**If dry run fails**: Fix issues before proceeding to real deployment.

### Step 4: Execute Real Deployment (3-5 minutes active)

```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_stage.sh both
```

**Monitor output for**:
- Deployment ID generated
- Prerequisites check passes
- Git lock acquired
- Version numbers updated
- Tier 1 tests pass (BLOCKING)
- Tier 2 tests pass (BLOCKING)
- Tier 3 tests complete (warnings OK)
- iOS Fastlane upload starts
- Android Fastlane upload starts

**Script will automatically**:
- Update build numbers for both platforms
- Execute 3-tier quality gate tests
- Build iOS IPA with SmilePile Stage scheme
- Build Android AAB with stageRelease variant
- Upload iOS to TestFlight via pilot action
- Upload Android to Play Console Internal Testing
- Create git commit with version tag
- Push to GitHub with tag
- Generate deployment summary

### Step 5: Monitor Processing (20-60 minutes passive)

**iOS Processing** (parallel with Android):
```bash
# Monitor in browser
# URL: https://appstoreconnect.apple.com
# Navigate: My Apps > SmilePile > TestFlight > Builds
# Watch for status: "Processing" -> "Ready to Test"
# First upload typically: 15-30 minutes
# Peak times may take: 30-60 minutes
```

**Android Processing** (usually completes first):
```bash
# Monitor in browser
# URL: https://play.google.com/console
# Navigate: SmilePile > Testing > Internal testing
# Status should show: "Available" within 1-2 minutes
```

### Step 6: Post-Processing Actions (2-5 minutes)

**iOS**:
- If "Missing Compliance" warning appears (expected), answer:
  - "Does your app use encryption?" → Yes (uses standard HTTPS)
  - "Does it use proprietary encryption?" → No
  - Submit compliance form
- Verify build distributed to "Internal Testers" group (may be automatic)
- Note: Processing complete when status shows "Ready to Test"

**Android**:
- Copy Internal Testing link from Play Console
- Share link with team members
- Testers opt-in via link before they can install

### Step 7: Team Distribution and Testing (30-45 minutes)

**iOS Installation**:
```bash
# Team members install TestFlight app from App Store
# Check TestFlight app for new SmilePile Stage build
# Tap Install
# Verify app name on home screen: "SmilePile Stage"
```

**Android Installation**:
```bash
# Team members open internal testing link
# Tap "Accept invite" and "Download from Play Store"
# Install app
# Verify app name in drawer: "SmilePile Stage"
```

**Functional Testing Checklist**:
- [ ] App launches without crashes (both platforms)
- [ ] App displays "SmilePile Stage" name correctly
- [ ] BUILD_TYPE_ENV reports "stage" (check settings or logs)
- [ ] Core features operational: photo capture, gallery, settings
- [ ] No immediate red flags or critical bugs

### Step 8: Documentation (10-15 minutes)

```bash
# Create deployment log
cd /Users/adamstack/SmilePile/wave-evidence/wave-8
# Document in 09-deployment-log.md:
# - Deployment timestamp and ID
# - iOS processing time
# - Android processing time
# - Any issues encountered
# - Team testing results
# - Lessons learned for Wave 9
```

---

## 4. iOS Upload Process

### Build Configuration

**Scheme**: SmilePile Stage
**Configuration**: Debug (stage tier uses Debug config with optimizations)
**Bundle ID**: com.smilepile
**Signing**: Apple Distribution certificate
**Export Method**: app-store (required for TestFlight)
**Output**: Build/stage/SmilePile-Stage.ipa

### Fastlane Lane: stage_ios

**Location**: ios/fastlane/Fastfile

**Actions executed**:
1. `clear_derived_data` - Clean build environment
2. `gym` - Build and sign IPA
   - Scheme: "SmilePile Stage"
   - Configuration: Debug
   - Output: ./build/stage/SmilePile-Stage.ipa
3. `pilot` - Upload to TestFlight
   - skip_waiting_for_build_processing: true (non-blocking)
   - distribute_external: false (internal only)
   - groups: ["Internal Testers"]
   - notify_external_testers: false
   - app_identifier: com.smilepile
   - team_id: 84W9WSYQQB

### Upload Details

**Authentication**: App Store Connect API Key (AuthKey_BJAC3957M4.p8)
**Team**: 84W9WSYQQB (Adam Stack)
**Target**: TestFlight Internal Testing
**Distribution**: Automatic to "Internal Testers" group (up to 100 testers)
**Processing**: 5-30 minutes (first upload may take longer)
**Review**: NOT required for internal testing

### Command

```bash
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane stage_ios
```

### Expected Timeline

- Clean derived data: 10-20 seconds
- Xcode build: 60-120 seconds
- IPA signing: 10-15 seconds
- Upload to App Store Connect: 60-180 seconds (depends on IPA size, typically 30-50MB)
- Total lane execution: 3-5 minutes
- Apple processing: 5-30 minutes (passive wait, non-blocking)

---

## 5. Android Upload Process

### Build Configuration

**Product Flavor**: stage
**Build Variant**: stageRelease
**Package Name**: com.smilepile
**Version Name Suffix**: -stage
**Build Config Field**: BUILD_TYPE_ENV = "stage"
**Output**: app/build/outputs/bundle/stageRelease/app-stage-release.aab

### Fastlane Lane: stage_android

**Location**: android/fastlane/Fastfile

**Actions executed**:
1. `gradle` - Clean and build AAB
   - Task: "clean bundleStageRelease"
   - Output: AAB bundle for Play Console
2. `upload_to_play_store` - Upload to Internal Testing
   - track: "internal"
   - release_status: "completed" (immediate availability)
   - aab: app/build/outputs/bundle/stageRelease/app-stage-release.aab
   - skip_upload_metadata: true (no store listing changes)
   - skip_upload_changelogs: false (include release notes)

### Upload Details

**Authentication**: Service account JSON (play-store-credentials.json)
**Service Account**: smilepile-fastlane-automation@smilepile-deployment.iam.gserviceaccount.com
**Required Role**: Release Manager
**Target**: Internal Testing track
**Distribution**: Via shareable link (up to 100 testers)
**Processing**: Immediate (1-2 minutes typical)
**Review**: NOT required for internal testing

### Command

```bash
cd /Users/adamstack/SmilePile/android
bundle exec fastlane stage_android
```

### Expected Timeline

- Gradle clean: 5-10 seconds
- Build AAB: 60-90 seconds
- Sign bundle: 5-10 seconds
- Upload to Play Console: 30-60 seconds (depends on AAB size, typically 20-30MB)
- Total lane execution: 2-3 minutes
- Google processing: 1-2 minutes (near-instant availability)

---

## 6. Quality Gates

### 3-Tier Testing System

**Philosophy**: Block deployment on critical/important failures, warn on UI failures.

### Tier 1: Critical Tests (BLOCKING)

**Test Categories**:
- Security tests (authentication, authorization, data protection)
- Data integrity tests (database operations, persistence)

**Platforms**:
- iOS: `./ios/scripts/run-tier-tests.sh tier1`
- Android: `./gradlew app:testStageReleaseTier1Critical`

**Failure Behavior**: Deployment ABORTS immediately with exit code 1

**What blocks**:
- Security vulnerabilities
- Data corruption risks
- Authentication bypass
- Critical functional failures

### Tier 2: Important Tests (BLOCKING)

**Test Categories**:
- ViewModel tests (business logic, state management)
- Repository tests (data layer, API integration)
- Dependency injection tests

**Platforms**:
- iOS: `./ios/scripts/run-tier-tests.sh tier2`
- Android: `./gradlew app:testStageReleaseTier2Important`

**Failure Behavior**: Deployment ABORTS immediately with exit code 1

**What blocks**:
- Business logic errors
- Data layer failures
- API contract violations
- DI container issues

### Tier 3: UI Tests (WARNING ONLY)

**Test Categories**:
- Component tests (UI rendering, layout)
- Integration tests (screen flows, navigation)

**Platforms**:
- iOS: `./ios/scripts/run-tier-tests.sh tier3`
- Android: `./gradlew app:testStageReleaseTier3UI`

**Failure Behavior**: Logged as WARNING, deployment CONTINUES

**What warns** (but doesn't block):
- UI layout issues
- Component rendering glitches
- Animation failures
- Non-critical integration issues

### Test Execution Order

1. Tier 1 (Critical) runs first → MUST PASS
2. Tier 2 (Important) runs second → MUST PASS
3. Tier 3 (UI) runs third → Failures logged but not blocking
4. Summary displayed showing pass/fail status for all tiers

### Skip Tests Option (NOT RECOMMENDED)

```bash
SKIP_TESTS=true ./deploy/deploy_stage.sh both
```

**Use only when**:
- Tests are known to be broken for non-critical reasons
- User explicitly authorizes skipping
- Urgency outweighs risk

---

## 7. Post-Deployment Validation

### Immediate Validation (within 1 hour)

**iOS**:
- [ ] Build status shows "Ready to Test" in App Store Connect
- [ ] Build appears in TestFlight > Builds section
- [ ] Build distributed to "Internal Testers" group
- [ ] Export compliance answered (if prompted)
- [ ] At least 2 team members successfully install from TestFlight

**Android**:
- [ ] Build status shows "Available" in Play Console
- [ ] Internal testing link accessible and shareable
- [ ] At least 2 team members successfully install via link

**Both Platforms**:
- [ ] App displays "SmilePile Stage" name on device
- [ ] App launches without immediate crashes
- [ ] BUILD_TYPE_ENV verified as "stage"
- [ ] Version number visible in app settings matches deployment

### Functional Validation (within 24 hours)

**Core Features**:
- [ ] Photo capture functional
- [ ] Gallery view displays photos
- [ ] Settings menu accessible
- [ ] App navigation smooth

**Crash Monitoring**:
- [ ] iOS: Check App Store Connect > TestFlight > Crashes (should be zero)
- [ ] Android: Check Play Console > Android vitals > Crashes (should be zero)

**Team Feedback**:
- [ ] Installation instructions clear and accurate
- [ ] No critical bugs reported
- [ ] Performance acceptable
- [ ] Tier-specific features working (stage environment detection)

### Documentation Validation

- [ ] Deployment log created: wave-evidence/wave-8/09-deployment-log.md
- [ ] Processing times recorded (iOS and Android)
- [ ] Git commit created with version tag (format: v25.10.15.016-stage)
- [ ] GitHub tag pushed successfully
- [ ] DEPLOYMENT_ROADMAP.md updated with Wave 8 completion

---

## 8. Timeline Estimates

| Phase | Description | Active Time | Elapsed Time | Dependencies |
|-------|-------------|-------------|--------------|--------------|
| Pre-Flight | Verify app store prerequisites | 10 min | 10 min | App Store Connect, Play Console access |
| Local Verification | Check credentials, tools, disk space | 5 min | 5 min | Local environment |
| Dry Run | Test deployment script without uploads | 5 min | 5 min | All prerequisites |
| Script Execution | Run deploy_stage.sh both | 5 min | 5 min | Dry run success |
| Quality Gates | Tier 1, 2, 3 tests (both platforms) | 0 min | 3-5 min | Included in script |
| iOS Build & Upload | Xcode build + Fastlane upload | 0 min | 3-5 min | Included in script |
| Android Build & Upload | Gradle build + Fastlane upload | 0 min | 2-3 min | Included in script |
| Git Commit | Version tag + push to GitHub | 0 min | 10-20 sec | Included in script |
| iOS Processing | Apple processes binary | 0 min | 15-60 min | Apple infrastructure |
| Android Processing | Google processes AAB | 0 min | 1-2 min | Google infrastructure |
| Distribution Setup | Answer compliance, share links | 2 min | 2 min | Processing complete |
| Team Installation | Testers install both apps | 5 min | 15 min | Distribution setup |
| Functional Testing | Core feature validation | 10 min | 20 min | Installation complete |
| Documentation | Create deployment log | 10 min | 10 min | Testing complete |
| **TOTAL** | **End-to-end deployment** | **45-60 min** | **50-110 min** | **All gates passed** |

**Notes**:
- Active Time: Developer hands-on work
- Elapsed Time: Wall clock time (includes waiting)
- iOS processing is the critical path (longest wait)
- Android typically completes much faster
- First uploads may take longer than subsequent deployments

---

## 9. Risk Mitigation

### Top 3 Risks

#### Risk 1: Apps Not Created in Stores

**Likelihood**: 40% (status unknown)
**Impact**: HIGH (blocks deployment completely)
**Severity**: HIGH

**Detection**:
- Pre-flight verification in Step 1 will catch this immediately
- Script does not validate app existence (assumes prerequisite met)

**Mitigation**:
- VERIFY apps exist BEFORE running deploy_stage.sh
- If missing, create apps in stores (instructions below)
- Do not proceed to deployment until apps exist

**Resolution Steps**:

*iOS App Creation* (10-15 minutes):
1. Navigate to https://appstoreconnect.apple.com/apps
2. Click "+" to create new app
3. Select platforms: iOS
4. App name: SmilePile
5. Bundle ID: com.smilepile (must match Xcode config)
6. SKU: com.smilepile
7. User access: Full Access
8. Save and create app
9. Navigate to TestFlight > Internal Testing
10. Create group: "Internal Testers"
11. Add team members with Admin/Developer roles

*Android App Creation* (15-20 minutes):
1. Navigate to https://play.google.com/console
2. Click "Create app"
3. App name: SmilePile
4. Default language: English (United States)
5. App or game: App
6. Free or paid: Free
7. Declarations: Accept policies
8. Create app
9. Navigate to Testing > Internal testing
10. Click "Create new release" to enable track
11. Grant service account "Release Manager" role in Users & Permissions

**Time to Fix**: 25-35 minutes total (both platforms)

#### Risk 2: First Upload Processing Delays

**Likelihood**: 80% (common for first uploads)
**Impact**: MEDIUM (delays team testing, doesn't break deployment)
**Severity**: MEDIUM

**Detection**:
- iOS processing exceeds 30 minutes
- Build stuck in "Processing" status for extended time

**Mitigation**:
- Set expectations: First iOS upload may take 30-60 minutes
- Deploy during off-peak hours (8am-12pm Pacific Time recommended)
- Be patient - this is normal and expected
- Monitor status but don't intervene

**Resolution Steps**:
1. Check App Store Connect status page: https://developer.apple.com/system-status/
2. If no Apple outages, continue waiting
3. Processing will complete eventually (may take up to 90 minutes in rare cases)
4. Use wait time for Android testing and documentation prep

**Time to Fix**: N/A (just wait patiently)

#### Risk 3: Export Compliance Questions (iOS)

**Likelihood**: 95% (almost always required for first upload)
**Impact**: LOW (minor delay, easy to resolve)
**Severity**: LOW

**Detection**:
- "Missing Compliance" warning in App Store Connect after upload
- TestFlight shows yellow warning icon on build

**Mitigation**:
- Expected and documented in post-processing steps
- Can be answered after upload completes
- Does not block internal testing (only required before external beta)

**Resolution Steps**:
1. Navigate to App Store Connect > TestFlight > Build
2. Click yellow warning "Missing Compliance"
3. Question: "Does your app use encryption?"
   - Answer: Yes (app uses HTTPS for network communication)
4. Question: "Does your app use proprietary encryption?"
   - Answer: No (uses only standard iOS/HTTPS encryption)
5. Submit
6. Warning clears within 1-2 minutes

**Time to Fix**: 2 minutes

---

## 10. Rollback Procedures

### Pre-Upload Rollback (TRIVIAL)

**Scenario**: Issues detected before upload completes

**Actions**:
1. Press Ctrl+C to cancel deploy_stage.sh
2. Fix issue (tests failing, credentials invalid, etc.)
3. Re-run deployment from beginning
4. No cleanup required

**Impact**: None (no uploads sent to stores)
**Time**: Immediate

### Post-Upload Rollback (LIMITED OPTIONS)

**Scenario**: Issues discovered after builds uploaded to stores

**iOS Limitations**:
- Cannot delete TestFlight builds once uploaded
- Cannot unpublish from Internal Testing
- Build will remain visible to internal testers
- Can expire build manually (90-day default)

**Android Options**:
- Can halt rollout in Play Console Internal Testing
- Can remove release from track
- Previous release can be re-activated if available

**Recommended Approach**: FIX FORWARD
1. Identify and fix issue locally
2. Test thoroughly with QUAL tier
3. Deploy new STAGE build with incremented version
4. Communicate to team: "Use latest build, ignore previous"
5. Previous build expires after 90 days (iOS) or can be deactivated (Android)

**Why Fix Forward**:
- Faster than trying to undo uploads
- Follows best practices for continuous delivery
- Maintains deployment history/traceability
- Less disruptive to team testing

### Emergency Stop Procedure

**If critical security issue discovered mid-deployment**:

1. Stop script immediately (Ctrl+C)
2. If iOS upload started but not complete: Cannot abort (will finish in background)
3. If iOS upload complete: Manually remove from Internal Testing in App Store Connect
4. If Android upload complete: Halt rollout in Play Console, remove release
5. Notify team immediately: DO NOT INSTALL
6. Fix security issue
7. Deploy new build with fix
8. Document incident in deployment log

**Time to Execute**: 5-10 minutes
**Impact**: May have brief window where bad build is available

---

## 11. Success Criteria

### Deployment Execution Success

- [ ] deploy_stage.sh completes without fatal errors
- [ ] Tier 1 Critical tests: 100% pass rate
- [ ] Tier 2 Important tests: 100% pass rate
- [ ] Tier 3 UI tests: Executed (warnings acceptable)
- [ ] iOS Fastlane lane stage_ios completes successfully
- [ ] Android Fastlane lane stage_android completes successfully
- [ ] Git commit created with format: "stage: Deploy both - v25.10.15.016"
- [ ] Git tag created with format: v25.10.15.016-stage
- [ ] Changes pushed to GitHub successfully

### App Store Upload Success

- [ ] iOS IPA uploaded to App Store Connect
- [ ] iOS build status: "Ready to Test" (after processing)
- [ ] iOS build visible in TestFlight > Builds
- [ ] Android AAB uploaded to Play Console
- [ ] Android release status: "Available" in Internal Testing
- [ ] No upload errors or rejections

### Team Installation Success

- [ ] At least 2 iOS testers install from TestFlight
- [ ] At least 2 Android testers install from Play Console
- [ ] Installation process documented and shareable
- [ ] Team can distinguish Stage from other builds

### Functional Success

- [ ] Both apps launch without crashes (0% crash rate)
- [ ] App name displays correctly: "SmilePile Stage"
- [ ] BUILD_TYPE_ENV reports: "stage"
- [ ] Core features operational: photo capture, gallery, settings
- [ ] No critical bugs blocking further testing

### Documentation Success

- [ ] Deployment log created: wave-evidence/wave-8/09-deployment-log.md
- [ ] Log includes: deployment ID, timestamps, processing times, issues, learnings
- [ ] DEPLOYMENT_ROADMAP.md updated with Wave 8 completion
- [ ] Team notified with installation instructions
- [ ] Any issues documented for Wave 9 improvements

### Milestone Achievement

- [ ] First real app store upload completed (milestone unlocked)
- [ ] Infrastructure validated end-to-end with actual stores
- [ ] Baseline metrics captured for future deployments
- [ ] Team onboarded to app store testing workflow
- [ ] Confidence established for Wave 9 (Beta tier)

---

## 12. Command Reference

### Pre-Flight Verification

```bash
# Verify Xcode schemes
xcodebuild -project /Users/adamstack/SmilePile/ios/SmilePile.xcodeproj -list

# Expected output should include:
# - SmilePile Qual
# - SmilePile Stage
# - SmilePile Beta
# - SmilePile Prod

# Check iOS signing identity
security find-identity -v -p codesigning

# Expected: Apple Distribution: Adam Stack (84W9WSYQQB)

# Verify App Store Connect API key
ls -lh ~/.fastlane/AuthKey_BJAC3957M4.p8

# Expected: -rw------- 1 adamstack staff 257B <date> <time> AuthKey_BJAC3957M4.p8

# Check Android build variants
cd /Users/adamstack/SmilePile/android
./gradlew tasks --all | grep -i bundle

# Expected output should include: bundleStageRelease

# Verify Android keystore
ls -lh ~/keystores/smilepile-production.keystore

# Expected: -rw------- 1 adamstack staff 4.3K <date> <time> smilepile-production.keystore

# Check Play Store service account
ls -lh ~/.fastlane/play-store-credentials.json

# Expected: -rw------- 1 adamstack staff 2.4K <date> <time> play-store-credentials.json

# Verify disk space (requires 5GB minimum)
df -h /Users/adamstack/SmilePile

# Expected: At least 5GB available
```

### Deployment Execution

```bash
# Standard deployment (both platforms, with tests)
cd /Users/adamstack/SmilePile
./deploy/deploy_stage.sh both

# Dry run (test without uploading)
DRY_RUN=true ./deploy/deploy_stage.sh both

# iOS only
./deploy/deploy_stage.sh ios

# Android only
./deploy/deploy_stage.sh android

# Skip tests (NOT RECOMMENDED - use only with explicit user permission)
SKIP_TESTS=true ./deploy/deploy_stage.sh both

# Skip git commit (useful for testing)
SKIP_COMMIT=true ./deploy/deploy_stage.sh both

# Allow uncommitted changes (normally deployment checks for clean git status)
ALLOW_UNCOMMITTED=true ./deploy/deploy_stage.sh both
```

### Manual Fastlane Execution (if script fails)

```bash
# iOS manual upload (if deploy_stage.sh iOS fails)
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane stage_ios

# Android manual upload (if deploy_stage.sh Android fails)
cd /Users/adamstack/SmilePile/android
bundle exec fastlane stage_android
```

### Monitoring

```bash
# View deployment logs (real-time)
tail -f /Users/adamstack/SmilePile/deploy/logs/deploy_stage_*.log

# View most recent deployment log
ls -lt /Users/adamstack/SmilePile/deploy/logs/ | head -n 2

# Check git status after deployment
cd /Users/adamstack/SmilePile
git status
git log --oneline -5
git tag | grep stage

# Check current build version
cat /Users/adamstack/SmilePile/.build_number

# View iOS version in Info.plist
cat /Users/adamstack/SmilePile/ios/SmilePile/Info.plist | grep -A1 "CFBundleShortVersionString"

# View Android version in build.gradle
cat /Users/adamstack/SmilePile/android/app/build.gradle.kts | grep -E "versionCode|versionName"
```

### Troubleshooting

```bash
# Clean iOS derived data (if build fails)
cd /Users/adamstack/SmilePile/ios
rm -rf ~/Library/Developer/Xcode/DerivedData/*

# Clean Android build (if build fails)
cd /Users/adamstack/SmilePile/android
./gradlew clean

# Re-install iOS pods (if dependencies issue)
cd /Users/adamstack/SmilePile/ios
bundle exec pod install

# Verify Fastlane installation
bundle exec fastlane --version

# Update Fastlane (if outdated)
bundle update fastlane

# Test App Store Connect API key
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane run app_store_connect_api_key

# Test Play Store service account
cd /Users/adamstack/SmilePile/android
bundle exec fastlane run validate_play_store_json_key
```

### Post-Deployment Verification

```bash
# Verify git commit created
git log -1 --oneline

# Expected format: stage: Deploy both - v25.10.15.016

# Verify git tag created
git tag -l | grep stage | tail -n 1

# Expected format: v25.10.15.016-stage

# Verify tag pushed to remote
git ls-remote --tags origin | grep stage

# Check for deployment artifacts
ls -lh /Users/adamstack/SmilePile/deploy/artifacts/stage/

# Review deployment summary
cat /Users/adamstack/SmilePile/deploy/logs/deploy_stage_*.log | tail -n 30
```

---

**Planning Complete**: 2025-10-15
**Planning Agent**: developer
**Next Phase**: Security Review (Phase 4 - security + peer-reviewer agents in parallel)
**Wave Status**: 7 of 11 complete, Wave 8 Phase 3 complete
**Confidence**: HIGH (95%)
**Risk Level**: LOW
**Ready for Implementation**: YES (after Phase 4 security review and Phase 6 testing)
