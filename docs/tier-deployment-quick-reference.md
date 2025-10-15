# Tier Deployment Quick Reference

## Master Router Usage

### Deploy to any tier with one command

```bash
# QUAL (local testing)
./deploy/deploy.sh qual both

# STAGE (internal testing)
./deploy/deploy.sh stage ios

# BETA (external testing)
./deploy/deploy.sh beta android

# PROD (production)
./deploy/deploy.sh prod both
```

### Direct Script Usage

If you prefer calling tier scripts directly:

```bash
./deploy/deploy_qual.sh both
./deploy/deploy_stage.sh ios
./deploy/deploy_beta.sh android
./deploy/deploy_prod.sh both
```

---

## Common Flags

### All Tiers

**DRY_RUN** - Preview without executing
```bash
DRY_RUN=true ./deploy/deploy_stage.sh both
```

**SKIP_TESTS** - Skip quality gates (use sparingly, emergency only)
```bash
SKIP_TESTS=true ./deploy/deploy_stage.sh both
```

**SKIP_SONAR** - Skip SonarCloud analysis
```bash
SKIP_SONAR=true ./deploy/deploy_stage.sh both
```

**SKIP_COMMIT** - Skip git commit/tag
```bash
SKIP_COMMIT=true ./deploy/deploy_stage.sh both
```

### QUAL/STAGE/BETA Only

**ALLOW_UNCOMMITTED** - Test uncommitted changes (default: true)
```bash
ALLOW_UNCOMMITTED=true ./deploy/deploy_stage.sh both
```

### PROD Only

**REQUIRE_APPROVAL** - Skip manual approval prompt (CI environments only)
```bash
REQUIRE_APPROVAL=false ./deploy/deploy_prod.sh both
```

---

## Tier Characteristics

| Tier | Bundle ID | Upload Target | Tests | Commit |
|------|-----------|---------------|-------|--------|
| QUAL | .qual | Local only | 3-tier | Auto |
| STAGE | base | TestFlight Internal / Play Console Internal | 3-tier | Auto |
| BETA | base | TestFlight External / Play Console Closed | 3-tier | Auto |
| PROD | base | App Store / Play Store Production | 3-tier | Manual |

**Bundle IDs**:
- QUAL: `com.smilepile.qual` (iOS), `com.smilepile.qual` (Android)
- STAGE/BETA/PROD: `com.smilepile` (iOS), `com.smilepile` (Android)

**App Display Names**:
- QUAL: "SmilePile Qual"
- STAGE: "SmilePile Stage"
- BETA: "SmilePile Beta"
- PROD: "SmilePile"

---

## Quality Gates

All tiers use the same 3-tier testing system:

### Tier 1: Critical (BLOCKING)
- **Tests**: Security, Data, Core Business Logic
- **Behavior**: Deployment aborts on failure
- **Examples**: Authentication, data integrity, security checks

### Tier 2: Important (BLOCKING)
- **Tests**: ViewModels, Repositories, Services
- **Behavior**: Deployment aborts on failure
- **Examples**: Business logic, data access, state management

### Tier 3: UI (WARNING)
- **Tests**: UI Components, Integration Tests
- **Behavior**: Deployment continues with warning
- **Examples**: Component rendering, user interactions, UI integration

**To skip all gates** (emergency only):
```bash
SKIP_TESTS=true ./deploy/deploy_stage.sh both
```

---

## Common Workflows

### Test uncommitted changes on QUAL

```bash
# Make changes to code (don't commit yet)
# Deploy to QUAL to test
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both

# If tests pass, commit manually
git add -A
git commit -m "feat: your changes"
```

### Deploy to STAGE for internal testing

```bash
# Ensure changes are committed
git add -A
git commit -m "feat: new feature"

# Deploy to STAGE (uploads to TestFlight Internal + Play Console Internal)
./deploy/deploy_stage.sh both

# Wait for processing (5-10 minutes)
# Distribute to internal testers via App Store Connect / Play Console
```

### Deploy to BETA for external testing

```bash
# Deploy to BETA (uploads to TestFlight External + Play Console Closed)
./deploy/deploy_beta.sh both

# iOS: First time requires TestFlight review submission (1-2 days)
# Android: Available immediately to closed testers
```

### Preview PROD deployment (dry-run)

```bash
# Preview what will happen without actually deploying
DRY_RUN=true ./deploy/deploy_prod.sh both

# Review output, ensure everything looks correct
# When ready, run actual deployment
./deploy/deploy_prod.sh both
```

### Emergency hotfix deployment

```bash
# Fix critical bug
git add -A
git commit -m "fix: critical bug fix"

# Skip non-critical tests if needed (use with caution)
SKIP_TESTS=true ./deploy/deploy_stage.sh both

# Verify fix works
# Deploy to production
./deploy/deploy_prod.sh both
```

---

## Platform Selection

### Deploy to both platforms

```bash
./deploy/deploy.sh qual both
```

### Deploy to iOS only

```bash
./deploy/deploy.sh qual ios
```

### Deploy to Android only

```bash
./deploy/deploy.sh qual android
```

**Note**: All tier scripts support platform selection: `ios`, `android`, or `both`

---

## Examples

### Example 1: Daily development workflow

```bash
# Morning: Test local changes
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both

# Afternoon: Commit and deploy to STAGE
git add -A
git commit -m "feat: implement user feedback"
./deploy/deploy_stage.sh both

# Evening: Team validates on STAGE
# If approved, deploy to BETA for external testing
./deploy/deploy_beta.sh both
```

### Example 2: Weekly release workflow

```bash
# Monday: Start new feature
# Tuesday-Thursday: Develop and test on QUAL
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both

# Friday: Commit and deploy to STAGE
git add -A
git commit -m "feat: weekly feature set"
./deploy/deploy_stage.sh both

# Following Monday: Deploy to BETA
./deploy/deploy_beta.sh both

# Following Friday: Deploy to PROD
./deploy/deploy_prod.sh both
```

### Example 3: Hotfix workflow

```bash
# Critical bug found in production
# Create hotfix branch
git checkout -b hotfix/critical-bug

# Fix bug
# Test fix on QUAL
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both

# Commit fix
git add -A
git commit -m "fix: critical production bug"

# Fast-track to STAGE
SKIP_TESTS=true ./deploy/deploy_stage.sh both

# Verify fix works
# Deploy to PROD immediately
./deploy/deploy_prod.sh both

# Merge hotfix back to main
git checkout main
git merge hotfix/critical-bug
```

---

## Troubleshooting

### "Invalid tier" error

**Problem**: Master router doesn't recognize tier name

**Solution**: Use exact tier names: `qual`, `stage`, `beta`, `prod` (not `staging`, `quality`, `production`)

```bash
# Incorrect
./deploy/deploy.sh staging both

# Correct
./deploy/deploy.sh stage both
```

### "Environment validation failed"

**Problem**: env_manager.sh doesn't recognize tier

**Solution**: This was fixed in Wave 7. If you see this error:
1. Check you have latest deploy/lib/env_manager.sh
2. Verify case statement includes all tier names

### Git lock error

**Problem**: Concurrent deployment attempted

**Solution**: Wait for current deployment to finish, or remove stale lock:
```bash
# Check if deployment is actually running
ps aux | grep deploy

# If no deployment running, remove lock
rm -f .git/index.lock
```

### "Simulator not found" (iOS)

**Problem**: iOS simulator not available or incorrect name

**Solutions**:

**Option 1**: Install simulator via Xcode
```bash
# Open Xcode
# Xcode > Settings > Platforms
# Install iOS Simulator
```

**Option 2**: Set custom simulator name
```bash
IOS_SIMULATOR_NAME="iPhone 15" ./deploy/deploy_qual.sh ios
```

**Option 3**: List available simulators
```bash
xcrun simctl list devices available
```

### Fastlane authentication error

**Problem**: Fastlane can't authenticate to App Store Connect or Play Console

**Solutions**:

**iOS**: Verify API key exists
```bash
ls -la ~/app-store-connect-api-keys/
# Should see AuthKey_*.p8 file
```

**Android**: Verify service account JSON exists
```bash
ls -la android/play-store-credentials.json
# File should exist and not be in .gitignore'd
```

### Build number conflict

**Problem**: Build number already used

**Solution**: Build numbers are auto-incremented. If you see this:
1. Check .build_number file
2. Verify build number hasn't been manually modified
3. Let deploy script auto-increment

### Quality gate failures

**Problem**: Tests failing, blocking deployment

**Solutions**:

**Option 1**: Fix the tests (recommended)
```bash
# Run tests locally to debug
# iOS
xcodebuild test -scheme "SmilePile Qual" -testPlan "Tier1Critical"

# Android
./gradlew app:testTier1Critical
```

**Option 2**: Skip tests temporarily (emergency only)
```bash
SKIP_TESTS=true ./deploy/deploy_stage.sh both
# Then fix tests ASAP
```

---

## Additional Resources

### Documentation
- **Deployment Roadmap**: `/docs/DEPLOYMENT_ROADMAP.md`
- **Quality Gates Guide**: `/docs/quality-gates.md`
- **QUAL Deployment Guide**: `/docs/qual-deployment-guide.md`
- **QUAL Troubleshooting**: `/docs/qual-troubleshooting-guide.md`

### Evidence Files
- **Wave 7 Complete**: `/wave-evidence/wave-7/WAVE-7-COMPLETE.md`
- **Wave 7 Testing**: `/wave-evidence/wave-7/07-testing-report.md`
- **Wave 7 Implementation**: `/wave-evidence/wave-7/06-implementation-summary.md`

### Story
- **STORY-6.7**: `/backlog/sprint-6/STORY-6.7-tier-deployment-scripts.md`

---

## Tips and Best Practices

### Use DRY_RUN for new scripts

When trying a tier script for the first time:
```bash
DRY_RUN=true ./deploy/deploy_beta.sh both
```

Review output carefully before running for real.

### Test on QUAL first

Always test changes on QUAL before deploying to higher tiers:
```bash
# Test on QUAL
./deploy/deploy_qual.sh both

# If passes, deploy to STAGE
./deploy/deploy_stage.sh both
```

### Keep git clean

While STAGE and BETA support uncommitted changes, it's best practice to commit first:
```bash
git add -A
git commit -m "feat: description"
./deploy/deploy_stage.sh both
```

### Monitor uploads

After deploying to STAGE/BETA/PROD, monitor upload progress:

**iOS**: App Store Connect > TestFlight > wait for "Ready to Test"
**Android**: Play Console > Internal Testing > should be immediate

### Use flags sparingly

Flags like SKIP_TESTS and REQUIRE_APPROVAL=false should be used rarely:
- Development: Use them freely on QUAL
- Testing: Use cautiously on STAGE/BETA
- Production: Almost never use on PROD (emergency only)

---

## Quick Command Reference

```bash
# Most common commands

# QUAL - Test local changes
ALLOW_UNCOMMITTED=true ./deploy/deploy_qual.sh both

# STAGE - Internal testing
./deploy/deploy_stage.sh both

# BETA - External testing
./deploy/deploy_beta.sh both

# PROD - Production (with preview)
DRY_RUN=true ./deploy/deploy_prod.sh both  # Preview
./deploy/deploy_prod.sh both                # Actual

# Master router (any tier)
./deploy/deploy.sh <tier> <platform>
```

---

**Document Version**: 1.0
**Last Updated**: 2025-10-15
**Wave**: Wave 7 - Tier-Specific Deployment Scripts
**Maintained By**: SmilePile Development Team
