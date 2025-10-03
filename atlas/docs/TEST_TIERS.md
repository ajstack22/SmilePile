# Test Tier Strategy - SmilePile

**Implementation Date:** 2025-10-02
**Atlas Story:** ATLAS-TEST-001
**Status:** Active

## Overview

SmilePile uses a 3-tier test execution strategy to enable safe deployments while managing test reliability:

- **Tier 1 (Critical)**: Security, encryption, data integrity - **100% must pass** for deployment
- **Tier 2 (Important)**: Business logic, ViewModels - **100% must pass** for deployment
- **Tier 3 (UI)**: UI components, integration - **Failures allowed** (warning only)

## Quick Reference

| Tier | Purpose | Pass Rate | Blocks Deploy? | Est. Runtime |
|------|---------|-----------|----------------|--------------|
| **Smoke** | Sanity check | 100% | ✅ Yes | ~15s |
| **Tier 1** | Security, data | 100% | ✅ Yes | ~30s |
| **Tier 2** | Business logic | 100% | ✅ Yes | ~45s |
| **Tier 3** | UI, integration | Best effort | ❌ No | ~30s |

## Test Commands

### Android

```bash
# Individual tiers
cd android
./gradlew app:testSmoke          # Quick sanity check (3 tests)
./gradlew app:testTier1Critical  # Critical security/data tests (6 tests)
./gradlew app:testTier2Important # Business logic tests (6 tests)
./gradlew app:testTier3UI        # UI/integration tests (2 tests)

# Run all tiers
./gradlew app:testAllTiers
```

### iOS

```bash
# Individual tiers
cd /Users/adamstack/SmilePile
./ios/scripts/run-tier-tests.sh smoke     # Quick sanity check (2 tests)
./ios/scripts/run-tier-tests.sh tier1     # Critical security/data (5 tests)
./ios/scripts/run-tier-tests.sh tier2     # Business logic (3 tests)
./ios/scripts/run-tier-tests.sh tier3     # UI/integration (2 tests)

# Run all tiers
./ios/scripts/run-tier-tests.sh all
```

### Deployment

The deployment script automatically runs all tiers:

```bash
# Full deployment with tiered tests
./deploy/deploy_qual.sh both

# Skip tests entirely (not recommended)
SKIP_TESTS=true ./deploy/deploy_qual.sh both
```

## Test Categorization

### Tier 1: Critical (12 files - BLOCKING)

**Android (7 tests):**
- `MetadataEncryptionTest.kt` - Encryption/decryption validation
- `SecurityValidationTest.kt` - PIN, inactivity, failed attempts
- `PhotoImportSafetyTest.kt` - Import limits, file validation, malicious detection
- `PhotoRepositoryImplTest.kt` - Data integrity, CRUD operations
- `BackupManagerTest.kt` - Backup data integrity
- `RestoreManagerTest.kt` - Restore data integrity
- `SecureActivityIntegrationTest.kt` - Screenshot prevention (integration)

**iOS (5 tests):**
- `PINManagerTests.swift` - PIN validation, security
- `PhotoImportSafetyTests.swift` - Memory-safe import, safety checks
- `StorageManagerTests.swift` - Storage integrity
- `ImageProcessorTests.swift` - Image processing safety
- `CoreDataStackTests.swift` - Database integrity

**Deployment Behavior:**
- ❌ **Fails immediately** if ANY test fails
- 🛑 **Blocks deployment** with error message
- Exit code: 1 (failure)

### Tier 2: Important (9 files - BLOCKING)

**Android (6 tests):**
- `PhotoGalleryViewModelTest.kt` - Gallery state management
- `PhotoEditViewModelTest.kt` - Photo editing logic
- `BackupViewModelTest.kt` - Backup UI logic
- `SettingsViewModelTest.kt` - Settings management
- `CategoryViewModelTest.kt` - Category management
- `CategoryRepositoryImplTest.kt` - Category business logic

**iOS (3 tests):**
- `PhotoRepositoryTests.swift` - Photo CRUD operations
- `CategoryRepositoryTests.swift` - Category operations
- `DIContainerTests.swift` - Dependency injection

**Deployment Behavior:**
- ❌ **Fails immediately** if ANY test fails
- 🛑 **Blocks deployment** with error message
- Exit code: 1 (failure)

### Tier 3: UI/Integration (5 files - WARNING ONLY)

**Android (3 tests):**
- `CrudOperationsTest.kt` - End-to-end CRUD (integration)
- `PhotoMetadataTest.kt` - Metadata model tests
- `PhotoImportManagerTest.kt` - Import workflow tests

**iOS (2 tests):**
- `SmilePileTests.swift` - General app tests
- `EnhancedPhotoViewerTests.swift` - Photo viewer UI

**Deployment Behavior:**
- ⚠️ **Logs warning** if tests fail
- ✅ **Allows deployment** to continue
- Exit code: 0 (success with warning)

### Smoke Tests (5 files - BLOCKING)

Critical subset for ultra-fast validation:

**Android (3 tests):**
- `MetadataEncryptionTest.kt`
- `SecurityValidationTest.kt`
- `PhotoImportSafetyTest.kt`

**iOS (2 tests):**
- `PINManagerTests.swift`
- `PhotoImportSafetyTests.swift`

## Deployment Flow

### Normal Execution (All Tests Pass)

```
┌─────────────────────────────────────────┐
│ TIER 1: Critical Tests                  │
│ Status: BLOCKING                        │
│ Result: ✅ PASSED                        │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│ TIER 2: Important Tests                 │
│ Status: BLOCKING                        │
│ Result: ✅ PASSED                        │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│ TIER 3: UI Tests                        │
│ Status: WARNING ONLY                    │
│ Result: ✅ PASSED                        │
└─────────────────────────────────────────┘
                   ↓
           Deployment Continues ✅
```

### Tier 1 Failure (Deployment Blocked)

```
┌─────────────────────────────────────────┐
│ TIER 1: Critical Tests                  │
│ Status: BLOCKING                        │
│ Result: ❌ FAILED                        │
└─────────────────────────────────────────┘
                   ↓
           🛑 DEPLOYMENT ABORTED

ERROR: CRITICAL FAILURE
These tests verify core security and data integrity.
Fix immediately before deploying.
```

### Tier 3 Failure (Deployment Continues)

```
┌─────────────────────────────────────────┐
│ TIER 1: Critical Tests                  │
│ Status: BLOCKING                        │
│ Result: ✅ PASSED                        │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│ TIER 2: Important Tests                 │
│ Status: BLOCKING                        │
│ Result: ✅ PASSED                        │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│ TIER 3: UI Tests                        │
│ Status: WARNING ONLY                    │
│ Result: ⚠️ FAILED (WARNING)             │
└─────────────────────────────────────────┘
                   ↓
    Deployment Continues with Warning ⚠️

WARNING: Tier 3 UI tests failed
Review failures but deployment will continue.
```

## Adding New Tests

### Decision Tree

**Step 1: Determine Test Purpose**
- Does it test encryption, auth, or data integrity? → **Tier 1**
- Does it test core business logic or state management? → **Tier 2**
- Does it test UI rendering or user interactions? → **Tier 3**

**Step 2: Add to Appropriate Tier**

#### Android - Add to `android/app/tier-tests.gradle`

```gradle
// For Tier 1 (Critical)
tasks.register('testTier1Critical', Exec) {
    commandLine './gradlew', 'testDebugUnitTest',
        // ... existing tests ...
        '--tests', 'com.smilepile.NEW_PACKAGE.NewSecurityTest'  // ADD HERE
}

// For Tier 2 (Important)
tasks.register('testTier2Important', Exec) {
    commandLine './gradlew', 'testDebugUnitTest',
        // ... existing tests ...
        '--tests', 'com.smilepile.NEW_PACKAGE.NewViewModelTest'  // ADD HERE
}

// For Tier 3 (UI)
tasks.register('testTier3UI', Exec) {
    commandLine './gradlew', 'testDebugUnitTest',
        // ... existing tests ...
        '--tests', 'com.smilepile.NEW_PACKAGE.NewUITest'  // ADD HERE
}
```

#### iOS - Add to `ios/scripts/run-tier-tests.sh`

```bash
# For Tier 1 (Critical)
tier1|critical)
    run_tier_tests "tier1" "TIER 1: Critical Tests" \
        "PINManagerTests" \
        # ... existing tests ...
        "NewSecurityTests"  # ADD HERE
    ;;

# For Tier 2 (Important)
tier2|important)
    run_tier_tests "tier2" "TIER 2: Important Tests" \
        "PhotoRepositoryTests" \
        # ... existing tests ...
        "NewRepositoryTests"  # ADD HERE
    ;;

# For Tier 3 (UI)
tier3|ui)
    run_tier_tests "tier3" "TIER 3: UI Tests" \
        "SmilePileTests" \
        # ... existing tests ...
        "NewUITests"  # ADD HERE
    ;;
```

**Step 3: Verify Test is Included**

```bash
# Android - verify task runs new test
cd android
./gradlew app:testTier1Critical --dry-run
./gradlew app:testTier1Critical

# iOS - verify script runs new test
cd /Users/adamstack/SmilePile
./ios/scripts/run-tier-tests.sh tier1
```

## Troubleshooting

### Issue: "Test not found" error

**Cause:** Test class name doesn't match filter pattern

**Solution:**
1. Verify exact test class name:
   ```bash
   # Android
   grep "class.*Test" android/app/src/test/java/path/to/NewTest.kt

   # iOS
   grep "class.*Tests" ios/SmilePileTests/NewTests.swift
   ```

2. Update tier configuration with exact name (case-sensitive)

### Issue: Tier 1 tests fail on CI but pass locally

**Cause:** Environment differences (Android SDK, iOS Simulator, etc.)

**Solution:**
1. Check test uses proper mocking (no real network/storage)
2. Verify test doesn't depend on specific device/simulator configuration
3. Add explicit cleanup in test teardown

### Issue: Want to temporarily skip a tier

**Solution:**
```bash
# Skip all tests
SKIP_TESTS=true ./deploy/deploy_qual.sh

# Skip specific tier (edit tier-tests.gradle temporarily)
# Comment out the test from the task:
# '--tests', 'com.smilepile.flaky.FlakyTest',  # TEMP SKIP
```

## Maintenance

### Monthly Review
1. Check for tests consistently failing in Tier 3
2. Review if any Tier 2 tests should be Tier 1 (or vice versa)
3. Verify all new features have appropriate tier coverage

### When to Reclassify
- **Tier 3 → Tier 2:** Test becomes stable and tests critical business logic
- **Tier 2 → Tier 1:** Test verifies security or data integrity
- **Tier 1 → Tier 2:** Test doesn't verify security/data (rare, requires approval)

## Performance Metrics

**Total Test Execution Time:**
- Sequential (all tiers): ~2 minutes
- Smoke tests only: ~15 seconds
- Tier 1 only: ~30 seconds

**Coverage by Tier:**
- Tier 1: 12 test files (46% of total)
- Tier 2: 9 test files (35% of total)
- Tier 3: 5 test files (19% of total)

## Related Documentation

- [Atlas Workflow](AGENT_WORKFLOW.md) - Full development workflow
- [Testing Strategy](../../docs/TESTING_STRATEGY.md) - Overall test approach
- [Deployment Guide](../../deploy/README.md) - Deployment process
- [User Story ATLAS-TEST-001](../stories/ATLAS-TEST-001-tiered-testing-implementation.md)

---

**Last Updated:** 2025-10-02
**Next Review:** 2025-11-02
