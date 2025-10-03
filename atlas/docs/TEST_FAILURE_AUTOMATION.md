# Test Failure Automation

**Part of ATLAS-TEST-001 - Tiered Testing System**

## Overview

The SmilePile deployment pipeline includes automated detection and response to test failures. This system differentiates between known failures and NEW failures, taking appropriate action based on test tier criticality.

## How It Works

### 1. Baseline Tracking

The system maintains a baseline of known test failures in `.test-failure-baseline.json`:

```json
{
  "tier1": [],
  "tier2": [],
  "tier3": [
    "com.smilepile.ui.viewmodels.PhotoEditViewModelTest:testLoadPhotoMetadata",
    "..."
  ]
}
```

- **Tier 1 & 2**: Should always be empty (these tests must pass)
- **Tier 3**: Contains known flaky tests that are being tracked as tech debt

### 2. Failure Detection

During each deployment, the `test-failure-tracker.sh` script:

1. Captures test output to `/tmp/tierN-{android|ios}-output.txt`
2. Parses failures from Gradle/xcodebuild output
3. Compares current failures against baseline
4. Detects NEW failures (not in baseline)

### 3. Automated Response

Based on tier and whether failures are NEW:

#### Tier 1/2 NEW Failures (CRITICAL)
- ✅ Creates bug story in `backlog/bugs/BUG-{timestamp}-{tier}-test-failures.md`
- ✅ Prompts to trigger Atlas Emergency workflow
- ✅ **BLOCKS DEPLOYMENT** (exit 1)
- ✅ Story includes:
  - List of failed tests
  - Impact assessment
  - Atlas workflow steps to resolve
  - Commands to investigate and fix

#### Tier 3 NEW Failures (WARNING)
- ✅ Creates tech debt story in `backlog/tech-debt/TECH-DEBT-{timestamp}-tier3-test-failures.md`
- ✅ **ALLOWS DEPLOYMENT** (exit 0)
- ✅ Updates baseline to include new failures
- ✅ Story includes:
  - List of failed tests
  - Root cause guidance (coroutines, UI flakiness, etc.)
  - Recommended fix approaches
  - Atlas Standard workflow steps

#### Known Failures (In Baseline)
- ℹ️ Logs "All failures are known (in baseline)"
- ✅ **ALLOWS DEPLOYMENT** (exit 0)
- ℹ️ No action required

## Integration with deploy_qual.sh

The failure tracker is integrated into the deployment pipeline:

```bash
# Example: Tier 1 Android tests
local tier1_output="/tmp/tier1-android-output.txt"
./gradlew app:testTier1Critical 2>&1 | tee "$tier1_output"
local tier1_exit=${PIPESTATUS[0]}

if [[ $tier1_exit -ne 0 ]]; then
    log ERROR "CRITICAL FAILURE: Tier 1 tests failed"
    log ERROR "Analyzing failures..."

    # Track failures and trigger workflow if NEW failures detected
    "$PROJECT_ROOT/scripts/test-failure-tracker.sh" tier1 "$tier1_output" || {
        log ERROR "Deployment ABORTED."
        exit 1
    }
    exit 1
fi
```

This pattern is applied to all three tiers for both Android and iOS.

## Usage Examples

### Manual Invocation

You can manually run the failure tracker on test output:

```bash
# Run tier 2 tests and analyze failures
./gradlew app:testTier2Important 2>&1 | tee /tmp/test-output.txt
./scripts/test-failure-tracker.sh tier2 /tmp/test-output.txt
```

### Updating Baseline

To update the baseline with current known failures:

```bash
# 1. Run tests to generate current failures
./gradlew app:testTier3UI 2>&1 | tee /tmp/tier3-output.txt

# 2. Manually update .test-failure-baseline.json with failures
# OR let the tracker auto-update for Tier 3:
./scripts/test-failure-tracker.sh tier3 /tmp/tier3-output.txt
```

The baseline is tracked in git and should be committed when:
- Tier 3 tests are intentionally moved to baseline (known flaky tests)
- Tier 3 tests are fixed and removed from baseline

## File Locations

### Tracked in Git
- `.test-failure-baseline.json` - Known failure baseline
- `scripts/test-failure-tracker.sh` - Detection script

### Generated (gitignored)
- `backlog/bugs/BUG-*.md` - Critical failure stories
- `backlog/tech-debt/TECH-DEBT-*.md` - Tech debt stories
- `/tmp/tier*-output.txt` - Test output captures

## Current Baseline Status

As of implementation:

- **Tier 1**: 0 known failures ✅
- **Tier 2**: 0 known failures ✅
- **Tier 3**: 11 known failures ⚠️
  - PhotoEditViewModelTest: 6 failures (coroutine timing issues)
  - SettingsViewModelTest: 5 failures (coroutine timing issues)

These Tier 3 failures are tracked and will be addressed when ViewModels are refactored to inject dispatchers properly.

## Workflow Integration

### Emergency Workflow (Tier 1/2 Failures)

When NEW Tier 1/2 failures are detected:

1. Script creates bug story with CRITICAL priority
2. Prompts user to continue with Atlas workflow
3. User presses ENTER to acknowledge
4. **TODO**: Auto-trigger Atlas Emergency workflow agent
5. Follow 9-phase Atlas workflow to resolve
6. Deployment resumes after fixes pass

### Standard Workflow (Tier 3 Failures)

When NEW Tier 3 failures are detected:

1. Script creates tech debt story
2. Updates baseline to include failures
3. Deployment continues
4. Tech debt story scheduled for future sprint
5. Use Atlas Standard workflow to fix when prioritized

## Maintenance

### When to Update Baseline

**Add to baseline:**
- Moving stable tests from Tier 2 → Tier 3 due to flakiness
- Accepting new Tier 3 test that is flaky but valuable

**Remove from baseline:**
- Tier 3 test fixed and now stable
- Tier 3 test deleted or moved to Tier 2

### Monitoring Baseline Growth

If Tier 3 baseline grows beyond 15-20 tests, consider:
- Sprint to fix batch of flaky tests
- Review if tests should be in Tier 3 or deleted
- Investigate common root causes (dispatcher injection, test environment)

## Benefits

✅ **No Surprises**: Known failures don't block deployment
✅ **Immediate Detection**: NEW failures caught instantly
✅ **Appropriate Response**: Critical failures block, flaky failures tracked
✅ **Automated Documentation**: Stories auto-generated with fix guidance
✅ **Workflow Integration**: Atlas workflow triggered for critical issues
✅ **Historical Tracking**: Baseline shows test health over time

## Related Documentation

- [Test Tiers](TEST_TIERS.md)
- [Testing Strategy](TESTING_STRATEGY.md)
- [Atlas Workflow](AGENT_WORKFLOW.md)
- [Deployment Guide](../../deploy/README.md)
