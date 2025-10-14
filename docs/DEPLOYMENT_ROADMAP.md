# SmilePile 4-Tier Deployment System - Atlas Implementation Roadmap

## Overview

This roadmap implements a professional 4-tier deployment system (QUAL, STAGE, BETA, PROD) for SmilePile using the Atlas agent-driven workflow methodology. Each implementation wave follows the complete 9-phase Atlas workflow with specialized agents, parallel execution, and evidence-driven development.

## Current State Assessment

**SmilePile Existing Setup:**
- iOS Project: `ios/SmilePile.xcodeproj`
- Bundle ID: `com.smilepile.SmilePile`
- Android Package: `com.smilepile`
- Current Version: `25.10.09.008`
- Existing Script: `deploy/deploy_qual.sh` (already Atlas-compliant!)

**Missing Components:**
- No xcconfig files for iOS tiers
- No Android product flavors
- No BUILD_TYPE_ENV detection
- No fastlane automation
- No tier-specific deployment scripts (STAGE, BETA, PROD)

**Target Architecture:**
- **QUAL**: `com.smilepile.qual` (local testing only)
- **STAGE/BETA/PROD**: `com.smilepile` (shared bundle ID, differentiated by TestFlight/Play Console tracks)

## Implementation Timeline

**Total Estimated Time**: 45-60 hours over 2-3 weeks
- **Calendar Time**: 2-3 weeks (accounting for Apple/Google approval delays)
- **Active Development**: 45-60 hours using Atlas workflow with parallel agents

#### Phase 1: Research (General-Purpose Agent)
**Agent Prompt:**
```
Launch general-purpose agent to research SmilePile's deployment requirements:
1. Investigate current Apple Developer and Google Play Console status
2. Research certificate management approaches (automatic vs fastlane match)
3. Identify iOS bundle ID and Android package name conventions
4. Find existing deployment credentials and keystore locations
5. Document current git workflow and branching strategy
6. Search for any existing TestFlight or Play Console configurations
```

**Evidence Creation:**
- `wave-evidence/wave-1/01-research-findings.md`
- Document current state, gaps, and recommendations

#### Phase 2: Story Creation (Product-Manager Agent)
**Agent Prompt:**
```
Launch product-manager agent to create story for Wave 1 Foundation Setup.
Use research findings from Phase 1. Create acceptance criteria for:
- Apple Developer account enrollment and App Store Connect app creation
- Google Play Console account and app creation
- iOS certificate and provisioning profile generation
- Android keystore generation and Play App Signing enrollment
- Testing track setup (Internal/External TestFlight, Play Console tracks)
OUTPUT: /backlog/sprint-6/STORY-6.1-foundation-setup.md
```

**Story Acceptance Criteria:**
- [ ] Apple Developer account active with App Store Connect access
- [ ] iOS app created in App Store Connect
- [ ] TestFlight Internal Testing and External Testing groups configured
- [ ] Google Play Console account active
- [ ] Android app created in Play Console
- [ ] Play Console Internal Testing and Closed Testing tracks configured
- [ ] iOS distribution certificate generated and stored securely
- [ ] Android production keystore generated and backed up (multiple locations)
- [ ] App Store Connect API key generated for fastlane automation
- [ ] Play Console service account JSON generated for fastlane automation

#### Phase 3: Planning (Developer Agent)
**Agent Prompt:**
```
Launch developer agent to create detailed implementation plan for STORY-6.1.
Detail step-by-step procedures for:
- Apple account enrollment process and timeline
- Google account setup and app creation
- Certificate generation commands and storage
- Keystore generation and backup strategy
- API key and service account creation steps
- Credential storage locations and .gitignore updates
```

**Planning Deliverables:**
- Detailed command sequences for certificate/keystore generation
- Credential storage strategy (local vs secrets manager)
- Timeline with approval wait periods factored in

#### Phase 4: Security Review (Security + Peer-Reviewer Agents in Parallel)
**Agent Prompts (Parallel Execution):**
```
PARALLEL LAUNCH:
1. Security agent: Audit foundation setup plan for security vulnerabilities
   - Review keystore backup strategy
   - Evaluate credential storage approach
   - Assess certificate management security
   - Identify risks in API key/service account handling

2. Peer-reviewer agent: Review foundation plan for edge cases
   - Identify missing steps or requirements
   - Validate approval timelines are realistic
   - Check for platform-specific gotchas
   - Ensure rollback procedures exist
```

**Security Considerations:**
- Keystores MUST be backed up to multiple secure locations
- NO credentials committed to git (verify .gitignore)
- API keys stored with restrictive permissions
- Service account JSON encrypted at rest

#### Phase 5: Implementation (Developer Agent)
**Agent Prompt:**
```
Launch developer agent to execute STORY-6.1 foundation setup following approved plan.
Execute sequentially:
1. Enroll in Apple Developer Program (wait for approval)
2. Create App Store Connect app and configure TestFlight
3. Enroll in Google Play Console (pay fee, wait for approval)
4. Create Play Console app and configure testing tracks
5. Generate iOS certificates and provisioning profiles
6. Generate Android keystores with proper backup
7. Create App Store Connect API key
8. Create Play Console service account JSON
9. Store all credentials securely
10. Update .gitignore to exclude secrets
```

**Implementation Timeline:**
- Day 1-2: Account enrollment submissions (Apple 1-3 days, Google 1-2 days approval)
- Days 3-5: Certificate/keystore generation once accounts approved
- Estimated: 5-7 calendar days, 8-12 hours active work

**Key Files Created:**
- `~/keystores/smilepile-production.keystore` (BACKUP MULTIPLE LOCATIONS!)
- `~/keystores/smilepile-qual.keystore` (or use debug keystore)
- `~/app-store-connect-api-keys/AuthKey_XXXXXXXXXX.p8`
- `android/play-store-credentials.json` (add to .gitignore)
- Updated `.gitignore` with credential exclusions

#### Phase 6: Testing (UX-Analyst + Peer-Reviewer Agents in Parallel)
**Agent Prompts (Parallel Execution):**
```
PARALLEL LAUNCH:
1. UX-analyst agent: Verify all account dashboards are accessible and configured
   - Test App Store Connect login and app visibility
   - Test Play Console login and app visibility
   - Verify TestFlight groups are set up correctly
   - Verify Play Console testing tracks are configured

2. Peer-reviewer agent: Validate credential security and backup
   - Verify keystores exist in backup locations
   - Confirm .gitignore excludes all secrets
   - Test certificate access and validity
   - Validate service account permissions
```

**Testing Checklist:**
- [ ] App Store Connect accessible with app visible
- [ ] TestFlight Internal Testing group exists
- [ ] TestFlight External Testing group configured (pending first build)
- [ ] Play Console accessible with app visible
- [ ] Play Console Internal Testing track exists
- [ ] Play Console Closed Testing track configured
- [ ] iOS certificate valid and accessible
- [ ] Android keystores exist in 3+ backup locations
- [ ] API keys and service accounts functional
- [ ] No credentials committed to git

#### Phase 7: Validation (Product-Manager Agent)
**Agent Prompt:**
```
Launch product-manager agent to validate STORY-6.1 acceptance criteria.
Verify all foundation components are in place and documented.
Provide sign-off for Wave 1 completion.
```

**Validation Deliverables:**
- Acceptance criteria verification report
- Wave 1 completion sign-off
- Handoff notes for Wave 2

#### Phase 8: Clean-up (General-Purpose Agent)
**Agent Prompt:**
```
Launch general-purpose agent to clean up Wave 1 artifacts:
1. Organize all documentation in /backlog/sprint-6/
2. Move evidence to wave-evidence/wave-1/
3. Update DOCUMENTATION_STANDARDS.md compliance
4. Close STORY-6.1
5. Create Wave 1 completion report
```

**Clean-up Deliverables:**
- `wave-evidence/wave-1/completion-report.md`
- All documentation organized per standards
- Story closed in backlog

#### Phase 9: Deployment (DevOps Agent)
**Agent Prompt:**
```
Launch devops agent to deploy Wave 1 documentation and configuration:
1. Commit foundation documentation to git
2. Verify credential storage security
3. Create deployment notes for team handoff
Use: COMMIT_MESSAGE="docs: Wave 1 - 4-tier deployment foundation setup" ./deploy/deploy_qual.sh both
```

**Deployment Notes:**
- No code changes yet, only documentation and external account setup
- Credentials stored locally, NOT in git
- Team handoff documentation created

---

## Wave 2: iOS Tier Configuration

**Objective**: Create xcconfig files, Xcode schemes, and BuildConfig native module for iOS tier detection.

### Atlas Workflow - Wave 2

#### Phase 1: Research (General-Purpose Agent)
**Agent Prompt:**
```
Launch general-purpose agent to research iOS tier configuration:
1. Search for current Xcode project structure (xcodeproj, schemes)
2. Find Info.plist location and current configuration
3. Research Swift native module patterns in SmilePile
4. Identify current build configuration approach
5. Search for any existing xcconfig usage
```

#### Phase 2: Story Creation (Product-Manager Agent)
**Agent Prompt:**
```
Launch product-manager agent to create iOS tier configuration story.
OUTPUT: /backlog/sprint-6/STORY-6.2-ios-tier-config.md
Acceptance criteria:
- Four xcconfig files created (Qual, Stage, Beta, Prod)
- Four Xcode schemes created and shared
- BUILD_TYPE_ENV native module (Swift) implemented
- Info.plist updated with BUILD_TYPE_ENV key
- All schemes build successfully for their respective tiers
- BUILD_TYPE_ENV detection working at runtime
```

#### Phase 3: Planning (Developer Agent)
**Agent Prompt:**
```
Launch developer agent to plan iOS tier configuration implementation.
Detail specific file paths and configuration values for SmilePile:
- xcconfig file locations and content
- Xcode scheme setup steps
- BuildConfigModule.swift implementation
- Info.plist modifications
- Build verification commands
```

**SmilePile-Specific Configuration:**
- Bundle IDs: `com.smilepile.qual` (QUAL), `com.smilepile` (STAGE/BETA/PROD)
- App Display Names: "SmilePile Qual", "SmilePile Stage", "SmilePile Beta", "SmilePile"
- BUILD_TYPE_ENV values: qual, stage, beta, prod

#### Phase 4: Security Review (Security + Peer-Reviewer in Parallel)
**Parallel Agent Execution:**
- Security: Audit BUILD_TYPE_ENV implementation for tampering risks
- Peer-Reviewer: Check for Xcode configuration edge cases

#### Phase 5: Implementation (Developer Agent)
**Agent Prompt:**
```
Launch developer agent to implement STORY-6.2 iOS tier configuration.
Create files in this order:
1. ios/Qual.xcconfig
2. ios/Stage.xcconfig
3. ios/Beta.xcconfig
4. ios/Prod.xcconfig
5. Configure four Xcode schemes (mark as Shared)
6. Create ios/SmilePile/BuildConfigModule.swift
7. Create bridging header if needed
8. Update ios/SmilePile/Info.plist with BUILD_TYPE_ENV key
9. Build each scheme to verify
```

**Key Files Created:**
- `ios/Qual.xcconfig` (BUILD_TYPE_ENV=qual, PRODUCT_BUNDLE_IDENTIFIER=com.smilepile.qual)
- `ios/Stage.xcconfig` (BUILD_TYPE_ENV=stage, PRODUCT_BUNDLE_IDENTIFIER=com.smilepile)
- `ios/Beta.xcconfig` (BUILD_TYPE_ENV=beta, PRODUCT_BUNDLE_IDENTIFIER=com.smilepile)
- `ios/Prod.xcconfig` (BUILD_TYPE_ENV=prod, PRODUCT_BUNDLE_IDENTIFIER=com.smilepile)
- `ios/SmilePile/BuildConfigModule.swift` (native module)
- Updated `ios/SmilePile/Info.plist`
- Four shared schemes in `ios/SmilePile.xcodeproj/xcshareddata/xcschemes/`

#### Phase 6: Testing (Parallel Agents)
**Parallel Execution:**
- UX-Analyst: Build each scheme and verify app name/bundle ID on device
- Peer-Reviewer: Code review BuildConfigModule and xcconfig files

**Testing Commands:**
```bash
# Build QUAL
xcodebuild -workspace ios/SmilePile.xcworkspace \
  -scheme "SmilePile Qual" \
  -configuration Debug \
  -sdk iphonesimulator

# Build STAGE
xcodebuild -workspace ios/SmilePile.xcworkspace \
  -scheme "SmilePile Stage" \
  -configuration Release \
  -sdk iphoneos

# Verify BUILD_TYPE_ENV in built apps
plutil -p build/Debug-iphonesimulator/SmilePile.app/Info.plist | grep BUILD_TYPE_ENV
```

#### Phase 7: Validation (Product-Manager Agent)
**Validation Criteria:**
- All four schemes build without errors
- BUILD_TYPE_ENV correctly set for each tier
- Bundle IDs match specification
- App display names correct on device

#### Phase 8: Clean-up (General-Purpose Agent)
**Clean-up Tasks:**
- Organize Wave 2 evidence
- Close STORY-6.2
- Update documentation

#### Phase 9: Deployment (DevOps Agent)
**Deployment:**
```bash
COMMIT_MESSAGE="feat: iOS 4-tier configuration with BUILD_TYPE_ENV detection" \
  ./deploy/deploy_qual.sh ios
```

**Estimated Time**: 6-8 hours over 1-2 days

---

## Wave 3: Android Tier Configuration

**Objective**: Configure Android product flavors, build types, and BuildConfig native module.

### Atlas Workflow - Wave 3

#### Phase 1: Research (General-Purpose Agent)
**Agent Prompt:**
```
Launch general-purpose agent to research Android configuration:
1. Find android/app/build.gradle structure
2. Search for existing product flavors or build types
3. Research Kotlin native module patterns in SmilePile
4. Identify current package name and signing configuration
```

#### Phase 2: Story Creation (Product-Manager Agent)
**Agent Prompt:**
```
Launch product-manager agent for Android tier configuration story.
OUTPUT: /backlog/sprint-6/STORY-6.3-android-tier-config.md
Acceptance criteria:
- Product flavors defined (qual, stage, beta, prod)
- Signing configs for each flavor
- BUILD_TYPE_ENV buildConfigField in each flavor
- BuildConfigModule.kt native module implemented
- BuildConfigPackage registered in MainApplication
- All flavors build successfully
- BUILD_TYPE_ENV detection working at runtime
```

#### Phase 3: Planning (Developer Agent)
**SmilePile-Specific Planning:**
- Package Names: `com.smilepile.qual` (QUAL), `com.smilepile` (STAGE/BETA/PROD)
- App Names: "SmilePile Qual", "SmilePile Stage", "SmilePile Beta", "SmilePile"
- Signing: Use keystores generated in Wave 1
- Build variants: qualDebug, qualRelease, stageRelease, betaRelease, prodRelease

#### Phase 4: Security Review (Parallel Agents)
**Security Focus:**
- Keystore password handling in gradle
- ProGuard configuration for BuildConfig
- Package name validation

#### Phase 5: Implementation (Developer Agent)
**Implementation Order:**
```
1. Create android/keystore.properties (add to .gitignore!)
2. Update android/app/build.gradle:
   - Add flavor dimensions
   - Define four product flavors
   - Configure signing configs
   - Add buildConfigField for BUILD_TYPE_ENV
3. Create android/app/src/main/java/com/smilepile/BuildConfigModule.kt
4. Create android/app/src/main/java/com/smilepile/BuildConfigPackage.kt
5. Update MainApplication to register BuildConfigPackage
6. Update android/app/src/main/AndroidManifest.xml (use @string/app_name)
```

**Key Files Modified/Created:**
- `android/keystore.properties` (NEVER commit!)
- `android/app/build.gradle` (product flavors, signing configs)
- `android/app/src/main/java/com/smilepile/BuildConfigModule.kt`
- `android/app/src/main/java/com/smilepile/BuildConfigPackage.kt`
- Updated `MainApplication.kt`

#### Phase 6: Testing (Parallel Agents)
**Testing Commands:**
```bash
# Build all flavors
./gradlew assembleQualRelease
./gradlew bundleStageRelease
./gradlew bundleBetaRelease
./gradlew bundleProdRelease

# Verify package names
aapt dump badging app/build/outputs/apk/qual/release/app-qual-release.apk | grep package
# Expected: com.smilepile.qual

aapt dump badging app/build/outputs/bundle/stageRelease/app-stage-release.aab | grep package
# Expected: com.smilepile
```

#### Phase 7: Validation (Product-Manager Agent)
**Validation:**
- All flavors build without errors
- Package names correct for each flavor
- BUILD_TYPE_ENV accessible from Kotlin/Java
- App names display correctly

#### Phase 8: Clean-up (General-Purpose Agent)

#### Phase 9: Deployment (DevOps Agent)
```bash
COMMIT_MESSAGE="feat: Android 4-tier configuration with product flavors" \
  ./deploy/deploy_qual.sh android
```

**Estimated Time**: 4-6 hours over 1-2 days

---

## Wave 4: JavaScript/TypeScript BUILD_TYPE_ENV Integration

**Objective**: Create unified buildConfig module for JavaScript/TypeScript to access BUILD_TYPE_ENV and route API endpoints.

### Atlas Workflow - Wave 4

#### Phase 1: Research (General-Purpose Agent)
**Research Tasks:**
- Find existing API client implementation
- Search for environment/config files
- Identify TypeScript vs JavaScript usage in SmilePile

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.4-js-buildconfig-integration.md`
**Acceptance Criteria:**
- `src/config/buildConfig.ts` created with BUILD_TYPE_ENV detection
- API endpoint routing based on BUILD_TYPE
- Helper functions (isQual, isStage, isBeta, isProd)
- Platform-agnostic (works on both iOS and Android)
- Type-safe TypeScript implementation
- Runtime logging for debug builds

#### Phase 3-9: Follow Standard Atlas Workflow

**Implementation Files:**
- `src/config/buildConfig.ts` (platform-agnostic BUILD_TYPE detection)
- Update existing API client to use BUILD_TYPE_ENV
- Add console logging for tier detection

**API Endpoint Strategy (Update based on SmilePile backend):**
```typescript
// Example - adjust for actual SmilePile backend URLs
export function getApiEndpoint(): string {
  switch (BUILD_TYPE) {
    case 'qual':
      return 'https://api-qual.smilepile.com/api';
    case 'stage':
      return 'https://api-stage.smilepile.com/api';
    case 'beta':
      return 'https://api-beta.smilepile.com/api';
    case 'prod':
      return 'https://api.smilepile.com/api';
    default:
      return 'https://api-qual.smilepile.com/api';
  }
}
```

**Deployment:**
```bash
COMMIT_MESSAGE="feat: JavaScript BUILD_TYPE_ENV integration and API routing" \
  ./deploy/deploy_qual.sh both
```

**Estimated Time**: 3-4 hours over 1 day

---

## Wave 5: Fastlane Automation Setup

**Objective**: Configure fastlane for iOS and Android to automate builds and uploads for each tier.

### Atlas Workflow - Wave 5

#### Phase 1: Research (General-Purpose Agent)
**Research:**
- Check if fastlane is installed
- Search for existing Fastfile or fastlane configuration
- Identify fastlane plugin requirements

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.5-fastlane-automation.md`
**Acceptance Criteria:**
- Fastlane installed (Homebrew or Bundler)
- `ios/fastlane/Fastfile` with 4 lanes (qual_ios, stage_ios, beta_ios, prod_ios)
- `ios/fastlane/Appfile` configured
- `android/fastlane/Fastfile` with 4 lanes (qual_android, stage_android, beta_android, prod_android)
- `android/fastlane/Appfile` configured
- App Store Connect API key configured
- Play Console service account JSON configured
- All lanes execute successfully

#### Phase 3: Planning (Developer Agent)
**Planning Focus:**
- fastlane installation method
- Lane definitions for each tier
- Credential management (API keys, service accounts)
- Upload vs local build distinction

#### Phase 5: Implementation (Developer Agent)
**iOS Fastlane Setup:**
```bash
cd ios
fastlane init
# Configure Appfile with Apple ID and Team ID
# Create lanes in Fastfile for each tier
```

**iOS Lanes:**
- `qual_ios`: Build for simulator (skip_package_ipa: true)
- `stage_ios`: Build + upload to TestFlight Internal
- `beta_ios`: Build + upload to TestFlight External
- `prod_ios`: Build + upload to App Store (manual submission)

**Android Fastlane Setup:**
```bash
cd android
fastlane init
# Configure Appfile with package name and service account JSON
# Create lanes in Fastfile for each tier
```

**Android Lanes:**
- `qual_android`: Build APK (local testing only)
- `stage_android`: Build AAB + upload to Internal Testing
- `beta_android`: Build AAB + upload to Closed Testing
- `prod_android`: Build AAB + upload to Production (draft)

**Key Files Created:**
- `ios/fastlane/Fastfile` (4 lanes)
- `ios/fastlane/Appfile` (Apple ID, Team ID)
- `android/fastlane/Fastfile` (4 lanes)
- `android/fastlane/Appfile` (package name, service account)
- `Gemfile` (optional, for bundler-based fastlane)

#### Phase 6: Testing (Parallel Agents)
**Test Each Lane:**
```bash
# iOS
cd ios
fastlane qual_ios      # Should build for simulator
# Don't test stage_ios yet (requires TestFlight setup)

# Android
cd android
fastlane qual_android  # Should build APK
# Don't test stage_android yet (requires Play Console setup)
```

#### Phase 9: Deployment (DevOps Agent)
```bash
COMMIT_MESSAGE="feat: Fastlane automation for 4-tier deployments" \
  ./deploy/deploy_qual.sh both
```

**Estimated Time**: 6-8 hours over 2-3 days

---

## Wave 6: Tier-Specific Deployment Scripts

**Objective**: Create deployment scripts for STAGE, BETA, and PROD tiers that integrate with fastlane.

### Atlas Workflow - Wave 6

#### Phase 1: Research (General-Purpose Agent)
**Research:**
- Analyze existing `deploy/deploy_qual.sh` structure
- Identify reusable functions and patterns
- Search for deploy script libraries (deploy/lib/*)

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.6-tier-deployment-scripts.md`
**Acceptance Criteria:**
- `deploy/deploy_stage.sh` created (calls fastlane stage lanes)
- `deploy/deploy_beta.sh` created (calls fastlane beta lanes)
- `deploy/deploy_prod.sh` created (calls fastlane prod lanes)
- Master script `deploy/deploy.sh` routes to tier scripts
- Validation checks for each tier (git status, PENDING_CHANGES.md, etc.)
- Deployment locking to prevent concurrent deploys
- Deployment summaries generated for each tier

#### Phase 3: Planning (Developer Agent)
**Script Architecture:**
```
deploy/
├── deploy.sh                 # Master router script
├── deploy_qual.sh            # Existing (already working!)
├── deploy_stage.sh           # New - STAGE tier
├── deploy_beta.sh            # New - BETA tier
├── deploy_prod.sh            # New - PROD tier
└── lib/
    ├── common.sh             # Shared functions
    ├── validation.sh         # Pre-deployment checks
    ├── reporting.sh          # Deployment summaries
    └── quality-gates.sh      # Quality checks
```

#### Phase 5: Implementation (Developer Agent)
**Implementation Order:**
1. Create `deploy/deploy_stage.sh`:
   - Validate PENDING_CHANGES.md exists
   - Check git status (must be clean)
   - Call `fastlane stage_ios` and `fastlane stage_android`
   - Wait for TestFlight/Play Console processing
   - Generate deployment summary
2. Create `deploy/deploy_beta.sh`:
   - Similar to STAGE but with external tester notifications
   - First-time BETA iOS requires TestFlight review submission
3. Create `deploy/deploy_prod.sh`:
   - Additional confirmation prompt (production safety)
   - Upload only, don't auto-submit for review
   - Generate release notes from PENDING_CHANGES.md
4. Create master `deploy/deploy.sh` router

**Master Script Usage:**
```bash
# Deploy to QUAL (local testing)
./deploy/deploy.sh qual --all

# Deploy to STAGE (internal testing)
./deploy/deploy.sh stage --all

# Deploy to BETA (external testing)
./deploy/deploy.sh beta --all

# Deploy to PROD (app store submission)
./deploy/deploy.sh prod --all
```

#### Phase 6: Testing (Parallel Agents)
**Test Scenarios:**
1. QUAL deployment (should work as before)
2. STAGE deployment (dry-run initially)
3. Validation checks (dirty git, missing PENDING_CHANGES.md)
4. Deployment locking (concurrent deploy prevention)

#### Phase 9: Deployment (DevOps Agent)
```bash
COMMIT_MESSAGE="feat: Complete 4-tier deployment script system" \
  ./deploy/deploy_qual.sh both
```

**Estimated Time**: 6-8 hours over 2-3 days

---

## Wave 7: First STAGE Deployment & Validation

**Objective**: Execute first STAGE deployment to TestFlight Internal Testing and Play Console Internal Testing.

### Atlas Workflow - Wave 7

#### Phase 1: Research (General-Purpose Agent)
**Pre-Deployment Checks:**
- Verify TestFlight Internal Testing group exists
- Verify Play Console Internal Testing track exists
- Confirm team members added as internal testers

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.7-first-stage-deployment.md`
**Acceptance Criteria:**
- STAGE iOS build uploaded to TestFlight Internal
- STAGE Android build uploaded to Play Console Internal
- Internal testers can install both apps
- BUILD_TYPE_ENV reports "stage" in both apps
- API endpoints route to stage environment
- No crashes on launch
- Team validation successful

#### Phase 3-8: Follow Standard Atlas Workflow

#### Phase 9: Deployment (DevOps Agent)
**First STAGE Deployment:**
```bash
# Prepare deployment
echo "## STAGE Deployment Test
### Changes:
- First STAGE tier deployment
- Validate 4-tier system infrastructure
- Test BUILD_TYPE_ENV detection
- Verify TestFlight/Play Console integration" > PENDING_CHANGES.md

# Deploy to STAGE
./deploy/deploy_stage.sh --all

# Monitor processing
# - iOS: App Store Connect → TestFlight → wait for "Ready to Test"
# - Android: Play Console → Internal Testing → should be immediate

# Distribute to internal testers
# - iOS: Distribute to "Internal Testers" group
# - Android: Share internal testing link with team

# Team testing
# - Install on real devices
# - Verify app name: "SmilePile Stage"
# - Verify BUILD_TYPE_ENV: stage
# - Verify API endpoint routing
# - Test core functionality
```

**Estimated Time**: 4-6 hours over 1-2 days (including TestFlight processing wait)

---

## Wave 8: First BETA Deployment & External Testing

**Objective**: Deploy to BETA tier for external testers (TestFlight External, Play Console Closed Testing).

### Atlas Workflow - Wave 8

#### Phase 1: Research (General-Purpose Agent)
**Beta Readiness:**
- Confirm TestFlight External Testing group configured
- Confirm Play Console Closed Testing track configured
- Prepare test account credentials if app requires login
- Review Apple export compliance requirements

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.8-first-beta-deployment.md`
**Acceptance Criteria:**
- BETA iOS submitted for TestFlight review (first time only)
- BETA Android uploaded to Closed Testing (no review required)
- External testers can install both apps after approval
- BUILD_TYPE_ENV reports "beta"
- Beta testers receive welcome email/instructions
- Crash reporting configured and monitored

#### Phase 9: Deployment (DevOps Agent)
**First BETA Deployment:**
```bash
# Deploy to BETA
./deploy/deploy_beta.sh --all

# iOS: Submit for TestFlight Review (first time only)
# - Go to App Store Connect → TestFlight
# - Submit for External Testing review
# - Provide test account if needed
# - Answer export compliance questions
# - Wait 1-2 days for approval

# Android: Immediate availability
# - Play Console → Closed Testing
# - Share opt-in link with beta testers

# Monitor beta feedback
```

**Estimated Time**: 2-4 hours active work, 1-2 days wait for TestFlight approval

---

## Wave 9: Documentation & Team Onboarding

**Objective**: Create comprehensive documentation and train team on 4-tier deployment workflow.

### Atlas Workflow - Wave 9

#### Phase 1: Research (General-Purpose Agent)
**Documentation Audit:**
- Identify gaps in current documentation
- Review team questions and common issues during implementation

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.9-documentation-team-training.md`
**Acceptance Criteria:**
- Deployment workflow documentation complete
- Troubleshooting guide created
- Team runbook created
- Secrets management documentation
- Video walkthrough recorded (optional)
- At least 2 team members successfully deploy to QUAL independently
- All credentials documented and accessible to authorized team members

#### Phase 5: Implementation (Developer Agent)
**Documentation to Create:**
1. `docs/deployment-workflow-smileple.md`:
   - How to deploy to each tier
   - When to use each tier
   - Validation steps
   - Rollback procedures
2. `docs/troubleshooting-deployments.md`:
   - Common issues and solutions
   - Platform-specific gotchas
   - Contact information for escalation
3. `docs/secrets-management.md`:
   - Where credentials are stored
   - How to access keystores and certificates
   - Rotation procedures
   - Backup verification steps
4. Team runbook in `docs/team-runbook.md`

#### Phase 6: Testing - Team Training
**Training Exercises:**
1. Walk through QUAL deployment
2. Practice STAGE deployment (dry-run)
3. Simulate common failure scenarios and recovery
4. Test rollback procedures

#### Phase 9: Deployment (DevOps Agent)
```bash
COMMIT_MESSAGE="docs: Complete 4-tier deployment documentation and team onboarding" \
  ./deploy/deploy_qual.sh both
```

**Estimated Time**: 4-6 hours over 2-3 days

---

## Wave 10: First PROD Deployment Readiness

**Objective**: Prepare for first production release with all necessary app store metadata and submission requirements.

### Atlas Workflow - Wave 10

#### Phase 1: Research (General-Purpose Agent)
**PROD Readiness Audit:**
- Review App Store Connect metadata requirements
- Review Play Console listing requirements
- Check screenshot requirements for all device sizes
- Review age rating questionnaires

#### Phase 2: Story Creation (Product-Manager Agent)
**Story:** `/backlog/sprint-6/STORY-6.10-prod-deployment-readiness.md`
**Acceptance Criteria:**
- App Store metadata complete (description, keywords, screenshots)
- Play Store metadata complete (listing, graphics)
- Age ratings completed on both platforms
- Privacy policy URL accessible
- Support URL accessible
- PROD build tested locally and works perfectly
- PROD deployment script tested in dry-run mode
- Team agrees build is ready for public release
- Rollback plan documented

#### Phase 5: Implementation (Developer Agent)
**Preparation Tasks:**
1. Complete App Store Connect metadata
2. Complete Play Console listing
3. Generate all required screenshots
4. Write compelling app descriptions
5. Set up pricing and availability
6. Complete compliance questionnaires
7. Test PROD deployment in dry-run:
   ```bash
   DRY_RUN=true ./deploy/deploy_prod.sh --all
   ```

#### Phase 9: Deployment (DevOps Agent)
**First PROD Deployment:**
```bash
# Final validation
./deploy/deploy_qual.sh both    # Test locally first
./deploy/deploy_stage.sh --all  # Deploy to team
# Team validates STAGE build thoroughly

# When ready for PROD
./deploy/deploy_prod.sh --all

# iOS: Manual submission
# - App Store Connect → App → Prepare for Submission
# - Fill in release notes
# - Submit for review
# - Wait 1-2 days for Apple review

# Android: Manual rollout
# - Play Console → Production
# - Create new release
# - Roll out to 10% → 50% → 100% over 3-7 days
# - Monitor crash reports closely
```

**Estimated Time**: 8-12 hours spread over 1-2 weeks (including review wait times)

---

## Post-Implementation: Ongoing Operations

### Regular Deployment Cadence

**QUAL (Development Testing):**
- **Frequency**: Multiple times per day (5-20+ deployments)
- **Purpose**: Fast iteration during development
- **Command**: `./deploy/deploy_qual.sh both`

**STAGE (Internal Validation):**
- **Frequency**: 1-3 times per week
- **Purpose**: Team validation before external beta
- **Command**: `./deploy/deploy_stage.sh --all`

**BETA (External Testing):**
- **Frequency**: 1-2 times per week
- **Purpose**: User feedback and testing
- **Command**: `./deploy/deploy_beta.sh --all`

**PROD (Public Release):**
- **Frequency**: Weekly or bi-weekly
- **Purpose**: Stable public releases
- **Command**: `./deploy/deploy_prod.sh --all`

### Promotion Strategy

Standard promotion path:
```
QUAL (local testing, multiple deploys)
  ↓
STAGE (team validation, 1-2 days)
  ↓
BETA (external testing, 3-7 days)
  ↓
PROD (public release)
```

Hotfix path (critical bugs only):
```
QUAL (verify fix)
  ↓
STAGE (quick team check, same day)
  ↓
BETA (smoke test, same day)
  ↓
PROD (emergency release, submit for expedited review)
```

### Monitoring & Maintenance

**Post-Deployment Monitoring:**
- Monitor crash reports in App Store Connect and Play Console
- Track user reviews and feedback
- Review analytics for anomalies
- Verify API endpoint routing is correct

**Credential Maintenance:**
- Rotate API keys annually
- Backup keystores quarterly
- Verify certificate expiration dates (renew 1 month before expiration)
- Test disaster recovery procedures semi-annually

**Quality Gates:**
- All QUAL deploys use deploy_qual.sh (enforces tests)
- STAGE deploys require clean git status
- BETA deploys require PENDING_CHANGES.md
- PROD deploys require team sign-off

---

## Success Metrics

### Timeline Metrics
- **Foundation Setup**: 5-7 calendar days (8-12 hours active)
- **iOS Configuration**: 1-2 days (6-8 hours)
- **Android Configuration**: 1-2 days (4-6 hours)
- **JavaScript Integration**: 1 day (3-4 hours)
- **Fastlane Setup**: 2-3 days (6-8 hours)
- **Deployment Scripts**: 2-3 days (6-8 hours)
- **First STAGE Deploy**: 1-2 days (4-6 hours)
- **First BETA Deploy**: 1-2 days active + 1-2 days TestFlight approval (2-4 hours)
- **Documentation**: 2-3 days (4-6 hours)
- **PROD Readiness**: 1-2 weeks (8-12 hours)

**Total Calendar Time**: 2-3 weeks
**Total Active Work**: 45-60 hours

### Quality Metrics
- Zero rollbacks due to tier misconfiguration
- 100% test coverage for BUILD_TYPE_ENV detection
- All deployments automated via scripts
- Documentation complete and maintained
- Team trained and self-sufficient

### Operational Metrics
- QUAL deployments: <10 minutes
- STAGE deployments: 15-20 minutes + processing time
- BETA deployments: 15-20 minutes + processing time
- PROD deployments: 20-30 minutes + review/approval time

---

## Risk Mitigation

### High-Risk Areas

**1. Certificate/Keystore Loss**
- **Risk**: Cannot update app in stores
- **Mitigation**: Triple-backup keystores, document rotation procedures
- **Verification**: Quarterly backup verification tests

**2. TestFlight Review Rejection**
- **Risk**: First BETA deploy delayed
- **Mitigation**: Prepare test account, review guidelines thoroughly
- **Contingency**: Use Play Console for beta testing while resolving iOS issues

**3. Bundle ID / Package Name Conflicts**
- **Risk**: Cannot install multiple tiers side-by-side (except QUAL)
- **Mitigation**: QUAL uses `.qual` suffix, others share base identifier
- **Verification**: Test installations on actual devices

**4. BUILD_TYPE_ENV Misconfiguration**
- **Risk**: PROD build hitting stage API
- **Mitigation**: Extensive testing in each tier, runtime validation
- **Verification**: Automated tests in deploy_qual.sh

**5. Team Knowledge Loss**
- **Risk**: Only one person knows deployment process
- **Mitigation**: Comprehensive documentation, team training
- **Verification**: Multiple team members successfully deploy independently

### Rollback Procedures

**QUAL/STAGE/BETA Rollback:**
```bash
# Checkout previous commit
git checkout [PREVIOUS_TAG]

# Redeploy
./deploy/deploy_stage.sh --all

# Return to main branch when ready
git checkout main
```

**PROD Rollback:**
- **iOS**: Remove from sale → Submit previous version → Wait for review
- **Android**: Halt rollout → Roll back to previous version (immediate)
- **Alternative**: Hotfix forward (usually faster than iOS rollback)

---

## Critical Decision Points

### Decision 1: Code Signing Strategy
**Options:**
- Automatic signing (simple, works for small teams)
- fastlane match (better for teams, shared certificates)

**Recommendation for SmilePile**: Start with automatic signing. Migrate to fastlane match if team grows beyond 3 developers.

### Decision 2: Keystore Management
**Options:**
- Keep keystores locally (simple, requires secure backup)
- Use Google Play App Signing (recommended, Google manages production key)

**Recommendation**: Use Google Play App Signing with upload keystore for security.

### Decision 3: API Endpoint Routing
**Current Unknown**: SmilePile backend URL structure

**Action Required Before Wave 4**:
- Determine if backend supports tier-specific endpoints
- Options:
  - Separate domains: api-qual.smilepile.com, api-stage.smilepile.com, etc.
  - Path-based: api.smilepile.com/qual, api.smilepile.com/stage, etc.
  - Single endpoint with tier header (not recommended for security)

### Decision 4: TestFlight Group Strategy
**Options:**
- Internal: Team members only
- External: Beta testers + team

**Recommendation**: Keep team in Internal Testing only. Use External Testing exclusively for beta users to maintain clear separation.

---

## Appendix A: SmilePile-Specific Configuration Reference

### Bundle IDs & Package Names
- **QUAL iOS**: `com.smilepile.qual`
- **STAGE/BETA/PROD iOS**: `com.smilepile`
- **QUAL Android**: `com.smilepile.qual`
- **STAGE/BETA/PROD Android**: `com.smilepile`

### App Display Names
- **QUAL**: "SmilePile Qual"
- **STAGE**: "SmilePile Stage"
- **BETA**: "SmilePile Beta"
- **PROD**: "SmilePile"

### Team IDs (Fill in after setup)
- **Apple Team ID**: `[TO BE FILLED]`
- **Google Play Console Project**: `[TO BE FILLED]`

### API Endpoints (To be determined)
- **QUAL**: `[TO BE DETERMINED]`
- **STAGE**: `[TO BE DETERMINED]`
- **BETA**: `[TO BE DETERMINED]`
- **PROD**: `[TO BE DETERMINED]`

---

## Appendix B: Atlas Agent Workflow Quick Reference

### 9-Phase Atlas Workflow

1. **Research** (general-purpose agent): Search codebase, understand patterns
2. **Story Creation** (product-manager agent): Define requirements, acceptance criteria
3. **Planning** (developer agent): Technical approach, implementation order
4. **Security Review** (security + peer-reviewer in parallel): Vulnerabilities, edge cases
5. **Implementation** (developer agent): Write code, make changes
6. **Testing** (ux-analyst + peer-reviewer in parallel): Verify functionality, code quality
7. **Validation** (product-manager agent): Confirm acceptance criteria met
8. **Clean-up** (general-purpose agent): Organize docs, close stories
9. **Deployment** (devops agent): Deploy using deploy_qual.sh (tests enforced!)

### Agent Specializations
- **general-purpose**: Research, search, exploration
- **product-manager**: Requirements, validation, sign-off
- **developer**: Planning, implementation, technical design
- **security**: Vulnerability auditing
- **peer-reviewer**: Quality review, edge case identification
- **ux-analyst**: UI/UX verification
- **devops**: Deployment, CI/CD, quality gates

### Parallel Execution Opportunities
- Phase 1: Multiple research agents for independent searches
- Phase 4: Security + peer-reviewer run simultaneously
- Phase 5: Multiple developers for independent components
- Phase 6: UX-analyst + peer-reviewer run simultaneously

---

## Next Steps

### Immediate Actions
1. **Review this roadmap** with team and stakeholders
2. **Reserve time** for 2-3 week implementation window
3. **Enroll in Apple Developer Program** (wait time: 1-3 days)
4. **Enroll in Google Play Console** (wait time: 1-2 days)
5. **Schedule Wave 1** once accounts are approved

### Kickoff Wave 1
Once ready to begin:
```
"I need to implement Wave 1 (Foundation & Account Setup) of the 4-tier deployment system.
Please orchestrate the Atlas agent workflow, launching appropriate agents for each phase.
Execute research, planning, and security review in parallel where possible.
Start with the research agent to assess our current deployment infrastructure."
```

---

**Document Version**: 1.0
**Last Updated**: 2025-10-13
**Maintained By**: SmilePile Development Team
**Reference**: Based on StackMap 4-tier deployment system handoff documentation
