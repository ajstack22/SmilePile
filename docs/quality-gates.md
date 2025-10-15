# SmilePile Quality Gates Documentation

## Overview

SmilePile uses a comprehensive 3-tier quality gate system to ensure code quality and prevent regressions. This system is integrated into all QUAL deployments via `deploy_qual.sh` and provides automatic quality enforcement with clear feedback.

### Purpose of Tiered Testing

The tiered approach allows for:
- **Early failure detection**: Critical tests run first, failing fast if fundamental issues exist
- **Clear prioritization**: Tests are categorized by importance and impact
- **Selective blocking**: Critical tests block deployment, while UI tests provide warnings
- **Better feedback**: Developers understand which category of tests failed
- **Efficient CI/CD**: Sequential execution with early exit saves time

## Quality Gate Tiers

### Tier 1: Critical Tests (BLOCKING)

**Status**: BLOCKS DEPLOYMENT
**Scope**: Security, Data Integrity, Core Functionality
**Run Order**: First
**Failure Action**: Abort deployment immediately

#### What's Tested

**Android**:
- `SecurityManagerTest`: PhotoAccessManager security validation
- `EncryptionTest`: Data encryption and secure storage
- `PermissionTest`: Runtime permission handling
- `DataIntegrityTest`: Database integrity and migrations
- `BackupRestoreTest`: Data backup and recovery

**iOS**:
- `SecurityTests`: Keychain access and secure storage
- `EncryptionTests`: Data encryption validation
- `PermissionTests`: Photo library access permissions
- `DataIntegrityTests`: Core Data integrity
- `BackupTests`: iCloud backup handling

#### Why These Block Deployment

These tests protect:
- **User privacy**: Photo access permissions and encryption
- **Data safety**: Database integrity and backup functionality
- **Security**: Secure storage and authentication
- **Legal compliance**: Permission handling and data protection

**If Tier 1 fails**, the deployment is immediately aborted. No further tests run. This prevents shipping code with fundamental security or data integrity flaws.

#### Example Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 1: Critical Tests (Security, Data Integrity)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

> Task :app:testTier1Critical

SecurityManagerTest > testPhotoAccessPermissions PASSED
SecurityManagerTest > testSecurePhotoStorage PASSED
DataIntegrityTest > testDatabaseMigrations PASSED
...

[TIER 1] PASSED - Critical tests successful
```

### Tier 2: Important Tests (BLOCKING)

**Status**: BLOCKS DEPLOYMENT
**Scope**: ViewModels, Repositories, Business Logic
**Run Order**: Second (after Tier 1 passes)
**Failure Action**: Abort deployment

#### What's Tested

**Android**:
- `PhotoRepositoryTest`: Photo CRUD operations
- `AlbumViewModelTest`: Album management logic
- `SearchViewModelTest`: Search functionality
- `SyncManagerTest`: Cloud sync operations
- `CacheManagerTest`: Image caching logic

**iOS**:
- `PhotoRepositoryTests`: Photo management
- `AlbumViewModelTests`: Album operations
- `SearchServiceTests`: Search implementation
- `SyncManagerTests`: iCloud sync
- `CacheTests`: Image cache management

#### Why These Block Deployment

These tests protect:
- **Core features**: Photo management, albums, search
- **Data consistency**: Repository layer correctness
- **Business logic**: ViewModel behavior and state management
- **Performance**: Caching and optimization logic

**If Tier 2 fails**, the deployment is aborted after Tier 1 passes. These tests ensure that the application's core functionality works correctly before deployment.

#### Example Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 2: Important Tests (ViewModels, Repositories)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

> Task :app:testTier2Important

PhotoRepositoryTest > testCreatePhoto PASSED
PhotoRepositoryTest > testDeletePhoto PASSED
AlbumViewModelTest > testAddPhotoToAlbum PASSED
...

[TIER 2] PASSED - Important tests successful
```

### Tier 3: UI Tests (WARNING ONLY)

**Status**: WARNS BUT CONTINUES
**Scope**: UI Components, Integration, Visual Regression
**Run Order**: Third (after Tier 1 and 2 pass)
**Failure Action**: Log warning, continue deployment

#### What's Tested

**Android**:
- `PhotoGridComponentTest`: Grid layout rendering
- `AlbumDetailScreenTest`: Album detail UI
- `SearchScreenTest`: Search interface
- `NavigationTest`: App navigation flows
- `ThemeTest`: Theme switching and dark mode

**iOS**:
- `PhotoGridViewTests`: Grid layout and rendering
- `AlbumDetailViewTests`: Album detail screens
- `SearchViewTests`: Search UI components
- `NavigationTests`: Navigation flows
- `ThemeTests`: Theme and appearance

#### Why These Don't Block

UI tests often have:
- **Flakiness**: Timing issues, animation delays
- **Platform quirks**: Simulator vs device differences
- **Environment sensitivity**: Screen sizes, OS versions
- **Lower risk**: Visual issues are visible in manual testing

**If Tier 3 fails**, a warning is logged but deployment continues. These failures are tracked in the test-failure-tracker system, which:
1. Compares against known failures
2. Creates tech debt stories for NEW failures
3. Allows deployment to continue for known issues

#### Example Output - Success

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 3: UI Tests (Components, Integration)
Status: WARNING - Deployment will continue with warning
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

> Task :app:testTier3UI

PhotoGridComponentTest > testGridLayout PASSED
AlbumDetailScreenTest > testPhotoDisplay PASSED
...

[TIER 3] PASSED - UI tests successful
```

#### Example Output - Failure

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 3: UI Tests (Components, Integration)
Status: WARNING - Deployment will continue with warning
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

> Task :app:testTier3UI

PhotoGridComponentTest > testGridLayout FAILED
  Expected 4 columns, got 3

WARNING: Tier 3 UI tests failed
Analyzing failures...
[test-failure-tracker] Known failure: PhotoGridComponentTest.testGridLayout
[test-failure-tracker] No new failures detected - continuing deployment

These tests verify UI components and user flows.
Review failures but deployment will continue.
```

## SonarCloud Quality Gates

In addition to automated tests, SmilePile integrates with SonarCloud for static code analysis.

### What SonarCloud Checks

- **Code Coverage**: Minimum 70% test coverage
- **Code Smells**: Maintainability issues
- **Bugs**: Potential runtime issues
- **Vulnerabilities**: Security weaknesses
- **Security Hotspots**: Areas requiring security review
- **Duplications**: Code duplication percentage
- **Technical Debt**: Estimated time to fix issues

### When SonarCloud Runs

SonarCloud analysis runs:
1. After all test tiers complete successfully
2. Before deployment to devices
3. Results are logged but don't block deployment (configurable)

### SonarCloud Failure Handling

By default, SonarCloud failures:
- Log warnings
- Provide dashboard link
- Don't block deployment
- Are tracked for technical debt

To make SonarCloud blocking:
```bash
BLOCK_ON_SONAR_FAILURE=true ./deploy/deploy_qual.sh both
```

### Example Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Running SonarCloud Analysis
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Running code quality analysis with SonarCloud...
SonarCloud analysis completed successfully
View results at: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile

Quality Gate: PASSED
- Coverage: 78.3% (threshold: 70%)
- Code Smells: 12 (A rating)
- Bugs: 0 (A rating)
- Vulnerabilities: 0 (A rating)
```

## Test Execution Strategy

### Sequential Execution

Tests run sequentially in tier order:

```
Tier 1 → Tier 2 → Tier 3 → SonarCloud
  ↓        ↓        ↓          ↓
BLOCK    BLOCK    WARN       WARN
```

### Early Exit

If Tier 1 fails:
- Tier 2 never runs
- Tier 3 never runs
- SonarCloud never runs
- Deployment aborts immediately

If Tier 2 fails:
- Tier 1 passed
- Tier 3 never runs
- SonarCloud never runs
- Deployment aborts

If Tier 3 fails:
- Tier 1 and 2 passed
- SonarCloud still runs
- Deployment continues with warning

### Execution Time

Typical execution times:
- **Tier 1**: 30-60 seconds (critical tests are fast)
- **Tier 2**: 1-2 minutes (business logic tests)
- **Tier 3**: 2-5 minutes (UI tests, slower)
- **SonarCloud**: 1-2 minutes (static analysis)

**Total**: 5-10 minutes for full quality gate suite

### Parallel Execution

Currently, tiers run sequentially. Future optimization could:
- Run Android and iOS tiers in parallel
- Run multiple test classes concurrently
- Use distributed test execution

## Test Configuration

### Android Test Configuration

Tests are configured in `android/app/build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests {
            all {
                it.testLogging {
                    events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED)
                }
            }
        }
    }
}

tasks.register<Test>("testTier1Critical") {
    filter {
        includeTestsMatching("*SecurityManagerTest")
        includeTestsMatching("*DataIntegrityTest")
        includeTestsMatching("*EncryptionTest")
        // ... more critical tests
    }
}

tasks.register<Test>("testTier2Important") {
    filter {
        includeTestsMatching("*ViewModelTest")
        includeTestsMatching("*RepositoryTest")
        // ... more important tests
    }
}

tasks.register<Test>("testTier3UI") {
    filter {
        includeTestsMatching("*ComponentTest")
        includeTestsMatching("*ScreenTest")
        // ... more UI tests
    }
}
```

### iOS Test Configuration

Tests are configured using test plans in `ios/SmilePile.xcodeproj`:

- **Tier1CriticalTests.xctestplan**: Security, data integrity
- **Tier2ImportantTests.xctestplan**: ViewModels, repositories
- **Tier3UITests.xctestplan**: UI components, integration

Executed via `ios/scripts/run-tier-tests.sh`:

```bash
#!/bin/bash
TIER=$1

case "$TIER" in
    tier1)
        xcodebuild test -workspace SmilePile.xcworkspace \
            -scheme "SmilePile Qual" \
            -testPlan Tier1CriticalTests \
            -destination 'platform=iOS Simulator,name=iPhone 16'
        ;;
    tier2)
        # Similar for tier2
        ;;
    tier3)
        # Similar for tier3
        ;;
esac
```

## Test Failure Tracking

### Test-Failure-Tracker System

SmilePile includes an intelligent failure tracking system (`scripts/test-failure-tracker.sh`) that:

1. **Tracks known failures**: Maintains a database of expected test failures
2. **Detects new failures**: Compares current failures against known failures
3. **Creates tech debt stories**: Automatically generates backlog items for NEW failures
4. **Provides clear feedback**: Distinguishes between known and new failures

### How It Works

When a test fails:

```bash
# Run tests (example: Tier 2 fails)
./gradlew app:testTier2Important 2>&1 | tee /tmp/tier2-output.txt
EXIT_CODE=$?

if [[ $EXIT_CODE -ne 0 ]]; then
    # Call failure tracker
    scripts/test-failure-tracker.sh tier2 /tmp/tier2-output.txt
    TRACKER_EXIT=$?

    if [[ $TRACKER_EXIT -ne 0 ]]; then
        # New failures detected - abort
        exit 1
    else
        # Only known failures - continue (for Tier 3 only)
        echo "Known failures, continuing..."
    fi
fi
```

### Failure Tracker Database

Located at `.test-failures/known-failures.json`:

```json
{
  "tier2": [
    "PhotoRepositoryTest.testSyncConflictResolution",
    "AlbumViewModelTest.testConcurrentModification"
  ],
  "tier3": [
    "PhotoGridComponentTest.testGridLayout",
    "SearchScreenTest.testResultAnimation"
  ]
}
```

### Idempotent Story Creation

The failure tracker is idempotent:
- Creates story only for NEW failures
- Checks if story already exists before creating
- Updates story if failure reoccurs
- Never creates duplicate stories

## Overriding Quality Gates

Quality gates can be selectively disabled for specific scenarios.

### Skip All Tests

```bash
SKIP_TESTS=true ./deploy/deploy_qual.sh both
```

**Use case**: When you've already run tests manually and just need to deploy.

**Warning**: Only use when you're CERTAIN tests pass. Never skip tests before committing.

### Skip SonarCloud

```bash
SKIP_SONAR=true ./deploy/deploy_qual.sh both
```

**Use case**: When SonarCloud is down or you're working offline.

### Dry Run

```bash
DRY_RUN=true ./deploy/deploy_qual.sh both
```

**Use case**: Test the deployment flow without actually building or deploying.

### Skip Specific Tiers

Not directly supported. To skip tiers, you must modify `deploy_qual.sh` or use `SKIP_TESTS=true`.

### When to Skip Tests

**Acceptable scenarios**:
- You've manually run all tests and they pass
- You're doing a dry run or testing the deployment script itself
- Emergency hotfix where tests are known to pass

**Never skip tests**:
- Before committing to main branch
- When deploying to STAGE, BETA, or PROD
- When you're unsure if tests pass
- When you've made code changes

### The Manylla Pattern

SmilePile follows the Manylla pattern:

1. **Test FIRST** with uncommitted changes
2. **Validate** via quality gates
3. **Commit AFTER** validation passes

This pattern ensures:
- No untested code is committed
- Uncommitted changes are validated before commit
- Git history only contains tested code
- Deploy failures don't pollute git history

Quality gates enforce this pattern in `deploy_qual.sh`:

```bash
# Check git status AFTER validation, not before
# This allows testing uncommitted changes
run_tests "android"
run_tests "ios"
run_sonarcloud_analysis

# Only after validation passes, commit
commit_to_github  # Checks git status here
```

## Quality Gate Best Practices

### 1. Run Quality Gates Locally

Always run quality gates before pushing:

```bash
# Test your changes before commit
./deploy/deploy_qual.sh both

# Quality gates will validate everything
# If they pass, your changes are safe to commit
```

### 2. Fix Tier 1 Failures Immediately

Tier 1 failures indicate:
- Security vulnerabilities
- Data integrity issues
- Critical bugs

**Never** skip or ignore Tier 1 failures. Fix them immediately.

### 3. Address Tier 2 Failures Before Committing

Tier 2 failures indicate:
- Business logic bugs
- Feature regressions
- API contract violations

While technically you can skip tests, **always** fix Tier 2 failures before committing.

### 4. Track Tier 3 Failures

Tier 3 failures are warnings, but:
- Review the failures
- Create tech debt stories (automatic via test-failure-tracker)
- Fix them when possible
- Don't let them accumulate indefinitely

### 5. Monitor SonarCloud Trends

Even though SonarCloud doesn't block:
- Check the dashboard regularly
- Track code coverage trends
- Address new code smells
- Fix security hotspots

### 6. Use Dry Run for Testing

When modifying deployment scripts:

```bash
DRY_RUN=true ./deploy/deploy_qual.sh both
```

This tests the flow without actual builds.

### 7. Never Skip Tests on Main Branch

Quality gates protect the main branch:
- Always run full quality gates before merging
- Never use `SKIP_TESTS=true` on main
- Ensure CI runs quality gates on PRs

## Quality Gate Metrics

### Coverage Requirements

- **Tier 1 Tests**: 100% coverage of security and data integrity code
- **Tier 2 Tests**: 80%+ coverage of business logic
- **Tier 3 Tests**: 60%+ coverage of UI components
- **Overall**: 70%+ code coverage (enforced by SonarCloud)

### Performance Targets

- **Tier 1**: < 60 seconds
- **Tier 2**: < 2 minutes
- **Tier 3**: < 5 minutes
- **SonarCloud**: < 2 minutes
- **Total**: < 10 minutes

If quality gates take longer:
- Optimize slow tests
- Parallelize where possible
- Consider splitting large test classes

### Failure Rate Targets

- **Tier 1**: 0% acceptable failure rate
- **Tier 2**: 0% acceptable failure rate
- **Tier 3**: < 5% acceptable failure rate (known flaky tests)
- **SonarCloud**: < 10% acceptable warning rate

## Troubleshooting Quality Gates

See `/Users/adamstack/SmilePile/docs/qual-troubleshooting-guide.md` for detailed troubleshooting.

Common issues:
- Tests fail locally but pass in CI (or vice versa)
- Tier 3 tests are flaky
- SonarCloud fails to analyze
- Quality gates take too long

## Future Improvements

### Planned Enhancements

1. **Parallel Test Execution**: Run Android and iOS tiers simultaneously
2. **Test Result Caching**: Skip unchanged tests on subsequent runs
3. **Visual Regression Testing**: Add screenshot comparison to Tier 3
4. **Performance Benchmarks**: Add performance tests to Tier 2
5. **Integration Test Tier**: Add Tier 4 for E2E integration tests
6. **Quality Gate Dashboard**: Web dashboard showing test trends

### Long-Term Vision

- Fully automated quality enforcement
- Zero-touch deployments when quality gates pass
- Predictive failure detection (ML-based)
- Self-healing tests (automatic flaky test detection and fixes)

---

**Document Version**: 1.0
**Last Updated**: 2025-10-15
**Maintained By**: SmilePile Development Team
**Related Documentation**:
- `/Users/adamstack/SmilePile/docs/qual-deployment-guide.md`
- `/Users/adamstack/SmilePile/docs/qual-troubleshooting-guide.md`
- `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`
