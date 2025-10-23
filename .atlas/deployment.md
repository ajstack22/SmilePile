# SmilePile Deployment Guide

## Overview

SmilePile uses a **4-tier deployment system** with progressive quality gates and validation stages. Each tier serves a specific purpose in the development lifecycle, from local testing to production release.

### Deployment Philosophy
- **QUAL**: Test locally + Share with team + Commit to repo
- **STAGE**: Internal validation before external testing
- **BETA**: External user feedback collection
- **PROD**: Stable production release

## 4-Tier Deployment System

| Tier | Purpose | Audience | Frequency | Validation Level |
|------|---------|----------|-----------|-----------------|
| **QUAL** | Development & Testing | Dev Team | Multiple times/day | Full test suite + SonarCloud |
| **STAGE** | Internal Staging | Internal QA | Daily/Weekly | Smoke tests + Integration |
| **BETA** | External Testing | Beta Users | Weekly | User acceptance |
| **PROD** | Production Release | All Users | Bi-weekly/Monthly | Full validation |

## Environment Configuration

### iOS Bundle Identifiers
- **QUAL**: `app.smilepile.qual`
- **STAGE**: `app.smilepile.stage`
- **BETA**: `app.smilepile.beta`
- **PROD**: `app.smilepile`

### Android Application IDs
- **QUAL**: `com.smilepile.qual`
- **STAGE**: `com.smilepile.stage`
- **BETA**: `com.smilepile.beta`
- **PROD**: `com.smilepile`

### Xcode Schemes
- `SmilePile Qual` - Local development builds
- `SmilePile Stage` - Internal staging builds
- `SmilePile Beta` - TestFlight/beta builds
- `SmilePile Prod` - App Store production builds

### Android Build Variants
- `qualDebug` - Development builds with debugging
- `qualRelease` - Development release builds
- `stageRelease` - Staging builds (no debug variant)
- `betaRelease` - Beta testing builds (no debug variant)
- `prodRelease` - Production builds (NEVER debug)

## Prerequisites

### Common Requirements
- Git configured with proper credentials
- Node.js and npm/yarn installed
- Ruby and Bundler for Fastlane
- jq for JSON processing
- SonarCloud token configured (optional)

### iOS Requirements
- macOS with Xcode installed
- Valid Apple Developer account
- Provisioning profiles configured
- iOS simulators installed

### Android Requirements
- Android SDK and build tools
- ANDROID_HOME environment variable set
- Android emulator or physical device
- Gradle wrapper configured
- Keystore for production signing (for BETA/PROD)

## Deployment Commands

### Primary Deployment Script
```bash
# Deploy to QUAL (default for both platforms)
./deploy/deploy_qual.sh

# Deploy specific platform
./deploy/deploy_qual.sh android
./deploy/deploy_qual.sh ios
./deploy/deploy_qual.sh both

# Deploy to other tiers
./deploy/deploy_stage.sh  # Internal staging
./deploy/deploy_beta.sh   # Beta testing
./deploy/deploy_prod.sh   # Production release
```

### Environment Variables
```bash
# Control deployment behavior
SKIP_TESTS=true         # Skip automated tests (NOT RECOMMENDED)
SKIP_SONAR=true         # Skip SonarCloud analysis
SKIP_COMMIT=true        # Skip git commit/push
ALLOW_UNCOMMITTED=true  # Allow deployment with uncommitted changes
AUTO_COMMIT=false       # Don't auto-commit changes
COMMIT_MESSAGE="msg"    # Custom commit message
TAG_VERSION=false       # Don't create version tag
DRY_RUN=true           # Test run without actual deployment
```

## Pre-Deployment Checklist

### QUAL Deployment
- [ ] All code changes saved
- [ ] Tests written for new features
- [ ] Build compiles without errors
- [ ] No hardcoded secrets or credentials
- [ ] Demo mode data configured (if applicable)

### STAGE Deployment
- [ ] QUAL deployment successful
- [ ] No critical bugs in QUAL testing
- [ ] API endpoints configured for staging
- [ ] Database migrations ready
- [ ] Release notes drafted

### BETA Deployment
- [ ] STAGE testing completed
- [ ] User-facing features documented
- [ ] Beta test plan created
- [ ] Crash reporting configured
- [ ] Feedback collection mechanism ready

### PROD Deployment
- [ ] BETA feedback addressed
- [ ] All tests passing (Tier 1 & 2 MUST pass)
- [ ] Performance benchmarks met
- [ ] Security review completed
- [ ] Rollback plan documented
- [ ] App Store/Play Store metadata updated

## Test Execution Tiers

The deployment process uses a **3-tier test system** with different blocking levels:

### Tier 1: Critical Tests (BLOCKING)
- Security tests
- Data integrity tests
- Authentication/authorization
- **Failure Action**: Deployment ABORTED

### Tier 2: Important Tests (BLOCKING)
- ViewModels/Repositories
- Business logic
- Core functionality
- **Failure Action**: Deployment ABORTED

### Tier 3: UI Tests (WARNING ONLY)
- Component tests
- Integration tests
- User flow validation
- **Failure Action**: Warning logged, deployment continues

### Test Commands
```bash
# Android
./gradlew app:testTier1Critical
./gradlew app:testTier2Important
./gradlew app:testTier3UI

# iOS
./ios/scripts/run-tier-tests.sh tier1
./ios/scripts/run-tier-tests.sh tier2
./ios/scripts/run-tier-tests.sh tier3
```

## Post-Deployment Verification

### QUAL Tier
1. Verify app launches on emulator/simulator
2. Check basic functionality works
3. Review test coverage reports:
   - Android: `android/app/build/reports/jacoco/jacocoQualDebugTestReport/html/index.html`
   - iOS: `ios/test_results_*.xcresult`
4. Monitor deployment logs: `deploy/logs/deploy_*.log`

### STAGE Tier
1. Verify staging API connectivity
2. Test user authentication flow
3. Validate data persistence
4. Check analytics/logging integration
5. Run smoke test suite

### BETA Tier
1. Confirm TestFlight/Play Console upload
2. Verify beta tester access
3. Monitor crash reports
4. Collect user feedback
5. Track usage analytics

### PROD Tier
1. Verify store listing updated
2. Monitor crash-free rate
3. Check performance metrics
4. Review user ratings/reviews
5. Confirm rollback capability

## Rollback Process

### Immediate Rollback (< 1 hour)
```bash
# Revert last commit
git revert HEAD
git push

# Redeploy previous version
git checkout <previous-tag>
./deploy/deploy_<tier>.sh
```

### Emergency Rollback (Production)
1. **Stop Current Release**: Halt rollout in store console
2. **Revert Code**:
   ```bash
   git checkout v<previous-version>
   ./deploy/deploy_prod.sh
   ```
3. **Notify Users**: Update status page/social media
4. **Post-Mortem**: Document issue and prevention steps

## Version Management

### Build Number Format
SmilePile uses **YY.MM.DD.XXX** format where:
- **YY**: Two-digit year (e.g., 25 for 2025)
- **MM**: Two-digit month (01-12)
- **DD**: Two-digit day (01-31)
- **XXX**: Daily build counter (001-999)

### Examples
- `25.10.17.001` - First build on October 17, 2025
- `25.10.17.002` - Second build on same day
- `25.10.18.001` - First build on October 18, 2025

### Version Code
- Calculated as: YYMMDDXXX (integer format)
- Example: `25101701` for version `25.10.17.001`

### Version Bumping
Versions are **automatically incremented** by the deployment scripts:
1. Date changes: Counter resets to 001
2. Same day: Counter increments (002, 003, etc.)
3. Maximum: 999 builds per day

### Git Tags
Production deployments create tags:
- Format: `v<version>` (e.g., `v25.10.17.002`)
- Message: "Release version <version> - <tier> deployment"

## Artifacts & Logs

### Build Artifacts
```
deploy/artifacts/
├── qual/
│   ├── SmilePile-v25.10.17.001-qual.apk
│   └── SmilePile-v25.10.17.001-qual.ipa
├── stage/
├── beta/
└── prod/
```

### Deployment Logs
```
deploy/logs/
├── deploy_qual_20251017_143022.log
├── deploy_stage_20251017_153045.log
└── deployment_history.json
```

### Coverage Reports
- **Android**: `android/app/build/reports/jacoco/`
- **iOS**: `ios/test_results_*.xcresult`
- **SonarCloud**: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile

## Troubleshooting

### Common Issues

#### Tests Failing
```bash
# View detailed test output
./gradlew app:testTier1Critical --info
./ios/scripts/run-tier-tests.sh tier1 --verbose

# Run specific test
./gradlew app:test --tests "com.smilepile.SecurityTest"
```

#### Build Failures
```bash
# Clean and rebuild Android
cd android && ./gradlew clean && ./gradlew assembleQualDebug

# Clean and rebuild iOS
cd ios && rm -rf DerivedData && xcodebuild clean build
```

#### Deployment Stuck
```bash
# Check background processes
ps aux | grep deploy

# Kill stuck deployment
kill -9 <process-id>

# Clean up lock files
rm -f deploy/.lock
```

#### Simulator/Emulator Issues
```bash
# iOS: Reset simulator
xcrun simctl erase all

# Android: Cold boot emulator
emulator -avd <name> -no-snapshot-load
```

## Security Considerations

### Never Deploy With
- Hardcoded API keys or secrets
- Debug logging in production
- Unencrypted sensitive data
- Disabled certificate pinning (PROD)
- Test accounts in production

### Always Ensure
- Environment-specific configurations
- Proper signing certificates
- Encrypted storage for credentials
- Security headers configured
- Input validation enabled

## Atlas Integration

This deployment guide integrates with the Atlas 9-phase workflow:

**Phase 9: Deployment** is executed by the DevOps agent using:
1. Quality gates validation (tests must pass)
2. Environment-specific deployment scripts
3. Version management and tagging
4. Artifact generation and storage
5. Post-deployment verification

### Invoking DevOps Agent
```bash
# In Atlas workflow, Phase 9 automatically uses:
atlas-agent-devops --deploy --tier=qual
```

### Quality Gates
The DevOps agent enforces:
- ✅ Tests pass (Tier 1 & 2 required)
- ✅ Linting passes
- ✅ Build succeeds
- ✅ Changelog updated
- ✅ No uncommitted changes (unless overridden)

## Contact & Support

### Deployment Issues
- Check logs: `deploy/logs/`
- Review test reports
- Consult Atlas DevOps agent documentation

### Emergency Contacts
- Define your team's escalation path
- Document on-call rotation
- Maintain incident response playbook

---

*Last Updated: October 2025*
*SmilePile Version: 25.10.17.002*