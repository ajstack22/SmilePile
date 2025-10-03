# Testing Strategy - SmilePile

**Status:** Active
**Last Updated:** 2025-10-02

## Overview

SmilePile uses a **tiered testing approach** to balance deployment speed with quality assurance. Tests are categorized by criticality, with strict blocking for security/data tests and flexibility for UI tests.

## Test Execution Tiers

### Quick Reference

```
┌────────────┬─────────────────────┬──────────┬─────────┬──────────┐
│ Tier       │ Purpose             │ Pass     │ Blocks  │ Runtime  │
│            │                     │ Rate     │ Deploy? │          │
├────────────┼─────────────────────┼──────────┼─────────┼──────────┤
│ Smoke      │ Quick sanity check  │ 100%     │ ✅ Yes  │ ~15s     │
│ Tier 1     │ Security, data      │ 100%     │ ✅ Yes  │ ~30s     │
│ Tier 2     │ Business logic      │ 100%     │ ✅ Yes  │ ~45s     │
│ Tier 3     │ UI, integration     │ Best     │ ❌ No   │ ~30s     │
│            │                     │ effort   │         │          │
└────────────┴─────────────────────┴──────────┴─────────┴──────────┘
```

### Tier 1: Critical (BLOCKING)

**What:** Security, encryption, data integrity, storage safety
**When:** Must pass 100% or deployment aborts
**Count:** 12 test files (46% coverage)

**Key Tests:**
- Encryption/decryption validation
- PIN security & authentication
- Photo import safety (memory, file validation)
- Data repository integrity
- Backup/restore correctness
- Database operations

**Failure Behavior:** 🛑 **Deployment ABORTED** with error

### Tier 2: Important (BLOCKING)

**What:** Business logic, ViewModels, repositories
**When:** Must pass 100% or deployment aborts
**Count:** 9 test files (35% coverage)

**Key Tests:**
- ViewModel state management
- Photo/category business logic
- Dependency injection
- Core feature workflows

**Failure Behavior:** 🛑 **Deployment ABORTED** with error

### Tier 3: UI/Integration (WARNING ONLY)

**What:** UI components, user interactions, integration flows
**When:** Failures log warnings but don't block
**Count:** 5 test files (19% coverage)

**Key Tests:**
- UI component rendering
- CRUD integration tests
- Model/data transformation
- User workflow tests

**Failure Behavior:** ⚠️ **Warning logged**, deployment continues

### Smoke Tests (BLOCKING)

**What:** Ultra-fast critical subset (5 key tests)
**When:** Pre-deployment sanity check
**Runtime:** <15 seconds

**Purpose:** Catch obvious breakage before full test suite

## Commands

### Development

```bash
# Android - Watch critical tests while coding
cd android
./gradlew app:testTier1Critical --continuous

# iOS - Run specific tier
cd /Users/adamstack/SmilePile
./ios/scripts/run-tier-tests.sh tier1

# Quick sanity check before commit
./android/gradlew app:testSmoke
./ios/scripts/run-tier-tests.sh smoke
```

### CI/Deployment

```bash
# Full deployment (runs all tiers automatically)
./deploy/deploy_qual.sh both

# Dry run to see what would happen
DRY_RUN=true ./deploy/deploy_qual.sh both

# Skip tests (emergency only)
SKIP_TESTS=true ./deploy/deploy_qual.sh both
```

## When to Use Each Tier

### ✅ Use Tier 1 (Critical) for:
- Encryption/decryption logic
- Authentication & authorization
- Data persistence & integrity
- Security controls (PIN, biometrics)
- File/storage safety
- Database operations

### ✅ Use Tier 2 (Important) for:
- Business logic & calculations
- State management (ViewModels)
- Repository patterns
- Core feature workflows
- Dependency injection

### ✅ Use Tier 3 (UI) for:
- UI component rendering
- User interaction flows
- Integration tests
- End-to-end workflows
- Visual regression (if added)

## Test Coverage Goals

| Category | Current | Target (6mo) | Target (12mo) |
|----------|---------|--------------|---------------|
| Tier 1   | 12 files | 15 files | 20 files |
| Tier 2   | 9 files | 15 files | 25 files |
| Tier 3   | 5 files | 10 files | 20 files |
| **Total** | **26** | **40** | **65** |

**Coverage %:**
- Critical paths: 80%+ (Tier 1)
- Business logic: 70%+ (Tier 2)
- UI components: 50%+ (Tier 3)

## Adding New Tests

**Decision Flow:**

```
Is it testing security, encryption, or data integrity?
    ├─ Yes → Tier 1 (Critical)
    └─ No  → Is it testing business logic or state?
              ├─ Yes → Tier 2 (Important)
              └─ No  → Tier 3 (UI)
```

**Update Locations:**
1. **Android:** `android/app/tier-tests.gradle`
2. **iOS:** `ios/scripts/run-tier-tests.sh`
3. **Documentation:** `atlas/docs/TEST_TIERS.md`

## Best Practices

### DO:
- ✅ Write tests in the appropriate tier based on criticality
- ✅ Mock external dependencies (network, storage)
- ✅ Use proper cleanup in teardown
- ✅ Run smoke tests before committing
- ✅ Fix Tier 1/2 failures immediately

### DON'T:
- ❌ Skip critical tests to speed up deployment
- ❌ Put security tests in Tier 3
- ❌ Rely on flaky UI tests for deployment decisions
- ❌ Bypass tiered execution (use `SKIP_TESTS` sparingly)

## Monitoring & Maintenance

### Weekly
- Review Tier 3 failures (are they becoming stable?)
- Check for new tests added to wrong tier

### Monthly
- Analyze test execution times
- Identify flaky tests for investigation
- Review tier assignments

### Quarterly
- Update coverage targets
- Assess need for new test categories
- Review and refactor test suite

## Integration with Deployment

The tiered test system is fully integrated with `deploy_qual.sh`:

**Execution Order:**
1. **Tier 1 (Critical)** - Blocks on failure ❌
2. **Tier 2 (Important)** - Blocks on failure ❌
3. **Tier 3 (UI)** - Warns on failure ⚠️
4. **Coverage Report** - Generated after tests
5. **Summary** - Shows pass/fail for each tier

**Exit Codes:**
- `0` = All critical tests passed (Tier 3 may warn)
- `1` = Tier 1 or Tier 2 failed (deployment blocked)

## Troubleshooting

### Issue: Tests pass locally but fail in CI

**Solution:**
- Ensure tests don't depend on local state/files
- Verify all mocks are properly configured
- Check for race conditions in async tests

### Issue: Deployment blocked by flaky Tier 1 test

**Solution:**
1. Fix the test immediately (it's blocking deployments!)
2. If truly flaky and not critical: Temporarily move to Tier 3
3. File bug report to fix properly
4. Never skip Tier 1 tests permanently

### Issue: Want to run only specific tier

**Solution:**
```bash
# Android
./android/gradlew app:testTier1Critical
./android/gradlew app:testTier2Important
./android/gradlew app:testTier3UI

# iOS
./ios/scripts/run-tier-tests.sh tier1
./ios/scripts/run-tier-tests.sh tier2
./ios/scripts/run-tier-tests.sh tier3
```

## Success Metrics

**Goals Achieved (2025-10-02):**
- ✅ Tiered test system implemented
- ✅ Critical tests block deployment (100% enforcement)
- ✅ UI test failures don't block (warning only)
- ✅ Deployment continues when critical paths verified
- ✅ Clear visibility into test health by tier

**Ongoing Metrics to Track:**
- Test execution time per tier
- Tier 1/2 pass rate (target: 100%)
- Tier 3 pass rate (target: >80%, informational)
- Deployment frequency (should increase)
- Time to fix critical test failures

## Related Documentation

- **[Test Tiers](TEST_TIERS.md)** - Detailed tier breakdown & test lists
- **[Atlas Workflow](AGENT_WORKFLOW.md)** - Full development process
- **[Deployment Guide](../../deploy/README.md)** - How to deploy
- **[Story ATLAS-TEST-001](../stories/ATLAS-TEST-001-tiered-testing-implementation.md)** - Implementation details

---

**Next Review:** 2025-11-02
