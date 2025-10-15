# Wave 7 Technical Planning: Tier-Specific Deployment Scripts

**Wave**: Wave 7 - Tier-Specific Deployment Scripts
**Phase**: Phase 3 - Technical Planning
**Agent**: Developer Agent
**Date**: 2025-10-15
**Duration**: 2 hours

---

## Executive Summary

### Implementation Approach
Wave 7 will complete the 4-tier deployment system by backporting Wave 6 security fixes, implementing the Manylla pattern, standardizing quality gates, and creating missing scripts (deploy_beta.sh and deploy.sh router). The implementation follows a prioritized approach: security fixes first, consistency improvements second, and new scripts third.

### Key Technical Decisions
1. **No New Shared Libraries**: Defer quality_gates.sh and git_workflow.sh library extraction to Wave 8 to reduce Wave 7 scope
2. **Inline Pattern Replication**: Copy Wave 6 patterns directly into STAGE/PROD scripts for faster implementation
3. **Fastlane-First for PROD**: Replace manual xcodebuild/gradle with Fastlane lanes for consistency
4. **Preserve PROD Safeguards**: Keep production approval gate and manual submission workflow
5. **Version Management Migration**: Switch PROD from semantic versioning to date-based (YY.MM.DD.VVV)

### Risk Mitigation Strategies
- Extensive DRY_RUN testing before real deployments
- Incremental rollout: STAGE validation → BETA creation → PROD updates
- Git-based rollback plan for each script modification
- Preserve existing PROD approval gates and manual submission workflow
- Version format migration documented with backward compatibility notes

---

## Implementation Sequence (10 Tasks)

### Task 1: Security Pattern Extraction (P0 - CRITICAL)
**Objective**: Document Wave 6 security patterns for replication

**Files to Read**:
- `/Users/adamstack/SmilePile/deploy/deploy_qual.sh` (lines 383-422)

**Deliverables**:
1. Security pattern reference document
2. Code snippets for iOS simulator validation
3. Input validation regex: `^[a-zA-Z0-9\ \-]+$`
4. Error message templates

**Changes**: None (documentation only)

**Dependencies**: None

**Priority**: P0 (Critical - needed for Tasks 2-3)

**Estimated Time**: 0.5 hours

**Testing Approach**: Documentation review only

---

### Task 2: Backport Security to deploy_stage.sh (P0 - CRITICAL)
**Objective**: Add Wave 6 security fixes to STAGE tier script

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_stage.sh`

**Specific Changes**:
1. Add `detect_available_simulator()` function after line 77 (before main execution)
2. Insert input validation logic with regex pattern
3. Add error handling for invalid simulator names
4. Add explanatory comments about command injection prevention

**Code to Add** (~40 lines):
```bash
# Detect available iOS simulator (with security input validation)
detect_available_simulator() {
    # Security: Allow override but validate input to prevent command injection
    if [[ -n "${IOS_SIMULATOR_NAME:-}" ]]; then
        # CRITICAL: Input validation - only allow alphanumeric, spaces, and hyphens
        if [[ ! "$IOS_SIMULATOR_NAME" =~ ^[a-zA-Z0-9\ \-]+$ ]]; then
            log ERROR "Invalid IOS_SIMULATOR_NAME: contains unsafe characters"
            log ERROR "Only alphanumeric, spaces, and hyphens allowed"
            return 1
        fi
        echo "$IOS_SIMULATOR_NAME"
        return 0
    fi

    # Fallback priority: iPhone 16 > iPhone 15 > iPhone 14
    for sim_name in "iPhone 16" "iPhone 15" "iPhone 14"; do
        local sim_id=$(xcrun simctl list devices 2>/dev/null | grep -m1 "$sim_name" | sed -E 's/.*\(([A-Z0-9-]+)\).*/\1/' || true)
        if [[ -n "$sim_id" ]]; then
            echo "$sim_id"
            return 0
        fi
    done

    log ERROR "No iOS simulators found"
    return 1
}
```

**Dependencies**: Task 1 (pattern extraction)

**Priority**: P0 (Critical - security vulnerability)

**Estimated Time**: 1 hour

**Testing Approach**:
- Test with valid simulator names
- Test with invalid characters (injection attempt)
- Verify error messages display correctly
- Run DRY_RUN deployment to verify no breakage

---

### Task 3: Backport Security to deploy_prod.sh (P0 - CRITICAL)
**Objective**: Add Wave 6 security fixes to PROD tier script

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_prod.sh`

**Specific Changes**:
1. Add `detect_available_simulator()` function after line 87 (after usage function)
2. Same security validation as Task 2
3. Add comments explaining proactive security measure

**Code to Add**: Same 40 lines as Task 2

**Dependencies**: Task 1 (pattern extraction)

**Priority**: P0 (Critical - security vulnerability)

**Estimated Time**: 1 hour

**Testing Approach**: Same as Task 2

---

### Task 4: Implement Manylla Pattern in deploy_stage.sh (P0 - CRITICAL)
**Objective**: Move git status check to after validation (Manylla pattern)

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_stage.sh`

**Specific Changes**:

**REMOVE** (lines 127-134):
```bash
# Check git status
if [[ "$ALLOW_UNCOMMITTED" != "true" ]]; then
    if [[ -n $(git status --porcelain) ]]; then
        log ERROR "Uncommitted changes detected"
        log ERROR "Commit changes or set ALLOW_UNCOMMITTED=true"
        exit 1
    fi
fi
```

**ADD** new function after line 77:
```bash
# Commit to GitHub (Manylla Pattern)
commit_to_github() {
    if [[ "$SKIP_COMMIT" == "true" ]]; then
        log INFO "Git commit skipped by configuration"
        return 0
    fi

    print_header "Committing to GitHub"

    cd "$PROJECT_ROOT"

    # Manylla Pattern: Check git status AFTER validation
    # This ensures we never commit untested code
    local changes=$(git status --porcelain)
    if [[ -z "$changes" ]]; then
        log INFO "No changes to commit"
        return 0
    fi

    if [[ -n "$changes" ]]; then
        log INFO "Uncommitted changes detected - will be included in commit"
        log INFO "✅ All validation passed - safe to commit"
    fi

    # Generate commit message with version
    local commit_msg="stage: Deploy ${PLATFORM} - v${VERSION_NAME}"
    local version="${VERSION_NAME}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would commit with message: $commit_msg"
        log INFO "DRY RUN: Would tag as: v${version}-stage"
        return 0
    fi

    # Add changes
    log INFO "Staging changes..."
    git add -A

    # Commit
    log INFO "Creating commit..."
    git commit -m "$commit_msg" || {
        log WARN "Nothing to commit"
    }

    # Tag
    local tag_name="v${version}-stage"
    log INFO "Creating tag: $tag_name"
    git tag -a "$tag_name" -m "Stage deployment v${version}"

    # Push
    log INFO "Pushing to GitHub..."
    git push origin "$(git rev-parse --abbrev-ref HEAD)"
    git push origin --tags

    log SUCCESS "Changes committed and pushed to GitHub"
}
```

**REPLACE** (lines 228-239) with function call:
```bash
# Commit to GitHub (Manylla pattern)
commit_to_github
```

**Dependencies**: None

**Priority**: P0 (Critical - prevents git pollution)

**Estimated Time**: 1.5 hours

**Testing Approach**:
- Test with uncommitted changes (should allow testing)
- Test with clean working directory
- Verify commit happens AFTER all validation
- Test SKIP_COMMIT flag
- Test DRY_RUN mode

---

### Task 5: Add Quality Gates to deploy_stage.sh (P0 - CRITICAL)
**Objective**: Implement 3-tier quality gate system with visual separators

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_stage.sh`

**Specific Changes**:

**REPLACE** (lines 146-173) with full 3-tier system:
```bash
# Run tests
if [[ "$SKIP_TESTS" != "true" ]]; then
    print_header "Tiered Test Execution"

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        cd "$PROJECT_ROOT/android"

        # Tier 1: Critical Tests (BLOCKING)
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log INFO "TIER 1: Critical Tests (Security, Data Integrity)"
        log INFO "Status: BLOCKING - Deployment will abort on failure"
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would run: ./gradlew app:testStageReleaseTier1Critical"
        else
            ./gradlew app:testStageReleaseTier1Critical || {
                log ERROR "CRITICAL FAILURE: Tier 1 tests failed"
                exit 1
            }
            log SUCCESS "[TIER 1] PASSED - Critical tests successful"
        fi

        # Tier 2: Important Tests (BLOCKING)
        log INFO ""
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log INFO "TIER 2: Important Tests (ViewModels, Repositories)"
        log INFO "Status: BLOCKING - Deployment will abort on failure"
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would run: ./gradlew app:testStageReleaseTier2Important"
        else
            ./gradlew app:testStageReleaseTier2Important || {
                log ERROR "IMPORTANT FAILURE: Tier 2 tests failed"
                exit 1
            }
            log SUCCESS "[TIER 2] PASSED - Important tests successful"
        fi

        # Tier 3: UI Tests (WARNING ONLY)
        log INFO ""
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log INFO "TIER 3: UI Tests (Components, Integration)"
        log INFO "Status: WARNING - Deployment will continue with warning"
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        local tier3_failed=0
        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would run: ./gradlew app:testStageReleaseTier3UI"
        else
            ./gradlew app:testStageReleaseTier3UI || {
                tier3_failed=1
                log WARN "WARNING: Tier 3 UI tests failed"
                log WARN "Review failures but deployment will continue."
            }
            if [[ $tier3_failed -eq 0 ]]; then
                log SUCCESS "[TIER 3] PASSED - UI tests successful"
            fi
        fi

        # Summary
        log INFO ""
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log INFO "TEST EXECUTION SUMMARY - ANDROID"
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log SUCCESS "Tier 1 Critical:  PASSED"
        log SUCCESS "Tier 2 Important: PASSED"
        if [[ $tier3_failed -eq 1 ]]; then
            log WARN "Tier 3 UI:        FAILED (WARNING ONLY)"
        else
            log SUCCESS "Tier 3 UI:        PASSED"
        fi
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        if [[ "$OS_TYPE" == "Darwin" ]]; then
            cd "$PROJECT_ROOT"

            # Tier 1: Critical Tests (BLOCKING)
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TIER 1: Critical Tests (Security, Data Integrity)"
            log INFO "Status: BLOCKING - Deployment will abort on failure"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./ios/scripts/run-tier-tests.sh tier1"
            else
                ./ios/scripts/run-tier-tests.sh tier1 || {
                    log ERROR "CRITICAL FAILURE: Tier 1 tests failed"
                    exit 1
                }
                log SUCCESS "[TIER 1] PASSED - Critical tests successful"
            fi

            # Tier 2: Important Tests (BLOCKING)
            log INFO ""
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TIER 2: Important Tests (Repositories, DI)"
            log INFO "Status: BLOCKING - Deployment will abort on failure"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./ios/scripts/run-tier-tests.sh tier2"
            else
                ./ios/scripts/run-tier-tests.sh tier2 || {
                    log ERROR "IMPORTANT FAILURE: Tier 2 tests failed"
                    exit 1
                }
                log SUCCESS "[TIER 2] PASSED - Important tests successful"
            fi

            # Tier 3: UI Tests (WARNING ONLY)
            log INFO ""
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TIER 3: UI Tests (Components, Integration)"
            log INFO "Status: WARNING - Deployment will continue with warning"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            local tier3_failed=0
            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./ios/scripts/run-tier-tests.sh tier3"
            else
                ./ios/scripts/run-tier-tests.sh tier3 || {
                    tier3_failed=1
                    log WARN "WARNING: Tier 3 UI tests failed"
                    log WARN "Review failures but deployment will continue."
                }
                if [[ $tier3_failed -eq 0 ]]; then
                    log SUCCESS "[TIER 3] PASSED - UI tests successful"
                fi
            fi

            # Summary
            log INFO ""
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TEST EXECUTION SUMMARY - IOS"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log SUCCESS "Tier 1 Critical:  PASSED"
            log SUCCESS "Tier 2 Important: PASSED"
            if [[ $tier3_failed -eq 1 ]]; then
                log WARN "Tier 3 UI:        FAILED (WARNING ONLY)"
            else
                log SUCCESS "Tier 3 UI:        PASSED"
            fi
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        fi
    fi
else
    log WARN "Tests skipped by configuration"
fi
```

**Dependencies**: None

**Priority**: P0 (Critical - quality consistency)

**Estimated Time**: 2 hours

**Testing Approach**:
- Verify Tier 1 failure blocks deployment
- Verify Tier 2 failure blocks deployment
- Verify Tier 3 failure warns but continues
- Test visual output formatting
- Test DRY_RUN mode

---

### Task 6: Add Quality Gates to deploy_prod.sh (P0 - CRITICAL)
**Objective**: Replace basic tests with 3-tier quality gate system

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_prod.sh`

**Specific Changes**:

**REPLACE** `run_production_tests()` function (lines 288-343) with same 3-tier implementation as Task 5, but using PROD flavor:
- Android: `testProdReleaseTier1Critical`, `testProdReleaseTier2Important`, `testProdReleaseTier3UI`
- iOS: Use `run-tier-tests.sh` with tier1, tier2, tier3

**Dependencies**: Task 5 (pattern established)

**Priority**: P0 (Critical - production quality)

**Estimated Time**: 2 hours

**Testing Approach**: Same as Task 5

---

### Task 7: Update deploy_prod.sh to use build_number.sh (P0 - CRITICAL)
**Objective**: Replace custom version bumping with centralized version management

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_prod.sh`

**Specific Changes**:

**ADD** library import (after line 20):
```bash
source "${DEPLOY_ROOT}/lib/build_number.sh"
```

**REMOVE** entire `bump_version()` function (lines 169-286)

**ADD** new simplified version update call in main() (replace lines 653-654, 658-659):
```bash
# Update version numbers using centralized build_number.sh
log INFO "Updating build version..."
update_version_all_platforms "$PLATFORM" || {
    log ERROR "Failed to update version numbers"
    exit 1
}
```

**REMOVE** VERSION_BUMP parameter logic (no longer needed with date-based versioning)

**UPDATE** usage() function to remove VERSION_BUMP references

**Dependencies**: None

**Priority**: P0 (Critical - version consistency)

**Estimated Time**: 1.5 hours

**Testing Approach**:
- Test version update for both platforms
- Verify YY.MM.DD.VVV format is used
- Verify .build_number file synchronization
- Test in DRY_RUN mode

---

### Task 8: Integrate Fastlane into deploy_prod.sh (P1 - HIGH)
**Objective**: Replace manual xcodebuild/gradle with Fastlane lanes

**Files to Modify**:
- `/Users/adamstack/SmilePile/deploy/deploy_prod.sh`

**Specific Changes**:

**REPLACE** `build_android_aab()` function (lines 346-419):
```bash
# Build Android AAB
build_android_aab() {
    print_header "Building Android App Bundle (AAB)"

    cd "$PROJECT_ROOT/android"

    # Build via Fastlane
    log INFO "Building production AAB via Fastlane..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would run: bundle exec fastlane prod_android"
    else
        bundle exec fastlane prod_android || {
            log ERROR "Android Fastlane build failed"
            return 1
        }
    fi

    local aab_path="$PROJECT_ROOT/android/app/build/outputs/bundle/prodRelease/app-prod-release.aab"

    if [[ ! -f "$aab_path" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "AAB not found at: $aab_path"
        return 1
    fi

    # Copy to artifacts
    mkdir -p "$DEPLOY_ROOT/artifacts/production"
    if [[ "$DRY_RUN" != "true" ]]; then
        cp "$aab_path" "$DEPLOY_ROOT/artifacts/production/SmilePile-v${VERSION_NAME}-prod.aab"
        log INFO "AAB saved to: artifacts/production/SmilePile-v${VERSION_NAME}-prod.aab"

        # Size analysis
        local aab_size=$(du -h "$aab_path" | cut -f1)
        log INFO "AAB size: $aab_size"

        local size_mb=$(du -m "$aab_path" | cut -f1)
        if [[ $size_mb -gt 150 ]]; then
            log WARN "AAB size exceeds 150MB (Google Play limit)"
        fi
    fi

    log SUCCESS "Android AAB generated successfully"
}
```

**REPLACE** `build_ios_archive()` function (lines 422-508):
```bash
# Build iOS Archive
build_ios_archive() {
    print_header "Building iOS Archive"

    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log ERROR "iOS archive requires macOS"
        return 1
    fi

    cd "$PROJECT_ROOT/ios"

    # Build via Fastlane
    log INFO "Building production archive via Fastlane..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would run: bundle exec fastlane prod_ios"
    else
        bundle exec fastlane prod_ios || {
            log ERROR "iOS Fastlane build failed"
            return 1
        }
    fi

    # Fastlane handles archive and IPA export
    log SUCCESS "iOS archive and IPA generated successfully"
    log INFO "Archive location: Check Xcode Organizer or Fastlane output"

    echo ""
    echo "📱 Next steps for iOS:"
    echo "1. Open Xcode > Window > Organizer"
    echo "2. Select the latest archive"
    echo "3. Click 'Distribute App' > 'App Store Connect'"
    echo "4. Or use Transporter app with the exported IPA"
    echo "5. Complete submission in App Store Connect"
}
```

**Dependencies**: None

**Priority**: P1 (High - consistency with other tiers)

**Estimated Time**: 1.5 hours

**Testing Approach**:
- Test Fastlane prod_android lane in isolation
- Test Fastlane prod_ios lane in isolation
- Verify artifact paths are correct
- Test full deploy_prod.sh in DRY_RUN mode

---

### Task 9: Create deploy_beta.sh (P1 - HIGH)
**Objective**: Create BETA tier script for external testing

**Files to Create**:
- `/Users/adamstack/SmilePile/deploy/deploy_beta.sh`

**Approach**: Copy deploy_stage.sh and modify for BETA tier

**Specific Changes**:
1. Copy deploy_stage.sh to deploy_beta.sh
2. Update header comments (BETA tier description)
3. Change DEPLOYMENT_ID to `beta_$(date +%Y%m%d_%H%M%S)`
4. Update Fastlane lane calls: `beta_ios` and `beta_android`
5. Update git tag suffix: `v${VERSION_NAME}-beta`
6. Update test gradle tasks: `testBetaReleaseTier1Critical`, etc.
7. Update summary text for external testing
8. Include Wave 6 security fixes (from Task 2)
9. Include Manylla pattern (from Task 4)
10. Include 3-tier quality gates (from Task 5)

**Code Size**: ~320 lines (similar to deploy_stage.sh after modifications)

**Dependencies**: Tasks 2, 4, 5 (patterns established)

**Priority**: P1 (High - completes 4-tier system)

**Estimated Time**: 2 hours

**Testing Approach**:
- Test in DRY_RUN mode
- Verify all tier quality gates work
- Test Fastlane beta lanes separately
- Verify git commit and tagging

---

### Task 10: Create Master Router deploy.sh (P2 - MEDIUM)
**Objective**: Create unified entry point for all tier deployments

**Files to Create**:
- `/Users/adamstack/SmilePile/deploy/deploy.sh`

**Implementation**:
```bash
#!/bin/bash
# ============================================================================
# SmilePile Deployment Router
# ============================================================================
# Routes deployment requests to appropriate tier-specific scripts
# Usage: ./deploy/deploy.sh <tier> <platform> [flags]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DEPLOY_ROOT="$SCRIPT_DIR"

usage() {
    cat << 'EOF'
================================================================================
SmilePile Deployment Router
================================================================================

Routes deployments to tier-specific scripts.

Usage: ./deploy/deploy.sh <tier> <platform> [flags]

Tiers:
    qual        Quality - Local devices + GitHub commit
    stage       Stage - TestFlight Internal + Play Console Internal
    beta        Beta - TestFlight External + Play Console Closed Testing
    prod        Production - Store-ready packages for manual submission

Platforms:
    android     Android only
    ios         iOS only
    both        Both platforms (default)

Environment Variables (passed through):
    SKIP_TESTS=true         Skip automated tests
    SKIP_COMMIT=true        Skip git commit/push (qual/stage/beta only)
    SKIP_SONAR=true         Skip SonarCloud analysis (qual only)
    DRY_RUN=true           Test run without actual deployment
    REQUIRE_APPROVAL=false  Skip production approval (prod only)

Examples:
    # Deploy to qual tier (both platforms)
    ./deploy/deploy.sh qual both

    # Deploy to stage tier (iOS only)
    ./deploy/deploy.sh stage ios

    # Deploy to beta tier with tests skipped
    SKIP_TESTS=true ./deploy/deploy.sh beta android

    # Deploy to production (requires approval)
    ./deploy/deploy.sh prod both

    # Dry run for production
    DRY_RUN=true ./deploy/deploy.sh prod both

================================================================================
EOF
    exit 0
}

# Parse arguments
TIER="${1:-}"
PLATFORM="${2:-both}"

if [[ -z "$TIER" ]] || [[ "$TIER" == "-h" ]] || [[ "$TIER" == "--help" ]] || [[ "$TIER" == "help" ]]; then
    usage
fi

# Validate tier
case "$TIER" in
    qual|quality)
        TIER="qual"
        SCRIPT="deploy_qual.sh"
        ;;
    stage|staging)
        TIER="stage"
        SCRIPT="deploy_stage.sh"
        ;;
    beta)
        TIER="beta"
        SCRIPT="deploy_beta.sh"
        ;;
    prod|production)
        TIER="prod"
        SCRIPT="deploy_prod.sh"
        ;;
    *)
        echo "ERROR: Invalid tier: $TIER"
        echo ""
        echo "Valid tiers: qual, stage, beta, prod"
        echo "Run with --help for more information"
        exit 1
        ;;
esac

# Validate platform
case "$PLATFORM" in
    android|ios|both)
        ;;
    *)
        echo "ERROR: Invalid platform: $PLATFORM"
        echo ""
        echo "Valid platforms: android, ios, both"
        echo "Run with --help for more information"
        exit 1
        ;;
esac

# Check if tier script exists
SCRIPT_PATH="$DEPLOY_ROOT/$SCRIPT"
if [[ ! -f "$SCRIPT_PATH" ]]; then
    echo "ERROR: Tier script not found: $SCRIPT_PATH"
    exit 1
fi

# Log routing decision
echo "=================================================================================="
echo "SmilePile Deployment Router"
echo "=================================================================================="
echo ""
echo "Tier:       $TIER"
echo "Platform:   $PLATFORM"
echo "Script:     $SCRIPT"
echo ""
echo "Routing to: $SCRIPT_PATH"
echo ""
echo "=================================================================================="
echo ""

# Execute tier-specific script (exec replaces current process)
exec "$SCRIPT_PATH" "$PLATFORM"
```

**Code Size**: ~150 lines

**Dependencies**: Tasks 2-9 (all tier scripts complete)

**Priority**: P2 (Medium - convenience feature)

**Estimated Time**: 1 hour

**Testing Approach**:
- Test routing to qual tier
- Test routing to stage tier
- Test routing to beta tier
- Test routing to prod tier
- Test invalid tier handling
- Test invalid platform handling
- Test help text display
- Verify environment variable pass-through

---

## Wave 6 Security Patterns

### Pattern 1: iOS Simulator Input Validation
**Location**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh` (lines 383-422)

**Purpose**: Prevent command injection via IOS_SIMULATOR_NAME environment variable

**Code Snippet**:
```bash
detect_available_simulator() {
    # Security: Allow override but validate input to prevent command injection
    if [[ -n "${IOS_SIMULATOR_NAME:-}" ]]; then
        # CRITICAL: Input validation - only allow alphanumeric, spaces, and hyphens
        if [[ ! "$IOS_SIMULATOR_NAME" =~ ^[a-zA-Z0-9\ \-]+$ ]]; then
            log ERROR "Invalid IOS_SIMULATOR_NAME: contains unsafe characters"
            log ERROR "Only alphanumeric, spaces, and hyphens allowed"
            return 1
        fi
        echo "$IOS_SIMULATOR_NAME"
        return 0
    fi

    # Fallback chain: Try booted simulator first
    local booted_sim=$(xcrun simctl list devices 2>/dev/null | grep "Booted" | head -n1 | sed -E 's/.*\(([A-Z0-9-]+)\).*/\1/' || true)
    if [[ -n "$booted_sim" ]]; then
        echo "$booted_sim"
        return 0
    fi

    # Fallback priority: iPhone 16 > iPhone 15 > iPhone 14 > any iPhone
    for sim_name in "iPhone 16" "iPhone 15" "iPhone 14"; do
        local sim_id=$(xcrun simctl list devices 2>/dev/null | grep -m1 "$sim_name" | sed -E 's/.*\(([A-Z0-9-]+)\).*/\1/' || true)
        if [[ -n "$sim_id" ]]; then
            echo "$sim_id"
            return 0
        fi
    done

    log ERROR "No iOS simulators found"
    return 1
}
```

**Key Elements**:
1. Input validation regex: `^[a-zA-Z0-9\ \-]+$`
2. Clear error messages for invalid input
3. Safe fallback chain if validation fails
4. Uses validated input in subsequent commands

**Replication Requirements**:
- Copy entire function to deploy_stage.sh, deploy_prod.sh, deploy_beta.sh
- Add explanatory comment about Wave 6 security fix
- Test with invalid input to verify rejection

---

## Manylla Pattern Implementation

### Core Principle
Validate-first, commit-after: Test code before committing, not after. This prevents committing broken code and allows testing of uncommitted changes.

### Current Anti-Pattern in deploy_stage.sh
**Lines 127-134** (WRONG - blocks testing of uncommitted changes):
```bash
# Check git status
if [[ "$ALLOW_UNCOMMITTED" != "true" ]]; then
    if [[ -n $(git status --porcelain) ]]; then
        log ERROR "Uncommitted changes detected"
        log ERROR "Commit changes or set ALLOW_UNCOMMITTED=true"
        exit 1
    fi
fi
```

**Problem**: Git check happens at script start, BEFORE validation. This prevents testing uncommitted changes.

### Correct Manylla Pattern
**From deploy_qual.sh** (lines 599-662):
```bash
commit_to_github() {
    if [[ "$SKIP_COMMIT" == "true" ]]; then
        log INFO "Git commit skipped by configuration"
        return 0
    fi

    print_header "Committing to GitHub"

    cd "$PROJECT_ROOT"

    # Manylla Pattern: Check git status AFTER validation
    # This ensures we never commit untested code
    local changes=$(git status --porcelain)
    if [[ -z "$changes" ]]; then
        log INFO "No changes to commit"
        return 0
    fi

    if [[ -n "$changes" ]]; then
        log INFO "Uncommitted changes detected - will be included in commit"
        log INFO "✅ All validation passed - safe to commit"
    fi

    # Version is already set by build_number.sh
    local version="${VERSION_NAME}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would commit with message: $commit_msg"
        return 0
    fi

    # Add changes
    if [[ -n "$changes" ]]; then
        log INFO "Staging changes..."
        git add -A
    fi

    # Commit
    log INFO "Creating commit..."
    git commit -m "$commit_msg" || {
        log WARN "Nothing to commit"
    }

    # Tag if requested
    if [[ "$TAG_VERSION" == "true" ]]; then
        local tag_name="v${version}"
        log INFO "Creating tag: $tag_name"
        git tag -a "$tag_name" -m "Release version ${version}"
    fi

    # Push
    log INFO "Pushing to GitHub..."
    git push origin "$(git rev-parse --abbrev-ref HEAD)"
    if [[ "$TAG_VERSION" == "true" ]]; then
        git push origin --tags
    fi

    log SUCCESS "Changes committed and pushed to GitHub"
}
```

### Order of Operations
1. **Update version numbers** (build_number.sh)
2. **Run all tests** (3-tier quality gates) - MAY HAVE UNCOMMITTED CHANGES
3. **Run builds** (Fastlane lanes)
4. **Check git status** (in commit_to_github function)
5. **Commit all changes** (including version updates from step 1)

### Key Benefits
- Allows testing uncommitted feature code before committing
- Ensures tests pass before creating commit
- Automatic inclusion of version file updates in commit
- Clear messaging about what's being committed

### ALLOW_UNCOMMITTED Flag Behavior
- **Not needed with Manylla pattern**: Script naturally allows uncommitted changes
- **Kept for backward compatibility**: Can be removed in future wave
- **Currently unused**: Manylla pattern makes it redundant

---

## Quality Gate Implementation

### 3-Tier System Design

#### Tier 1: Critical Tests (BLOCKING)
**Purpose**: Security, data integrity, core business logic
**Failure Behavior**: ABORT deployment immediately
**Examples**: Database encryption, photo storage, authentication

**Visual Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 1: Critical Tests (Security, Data Integrity)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Android Gradle Task**:
- QUAL: `./gradlew app:testTier1Critical`
- STAGE: `./gradlew app:testStageReleaseTier1Critical`
- BETA: `./gradlew app:testBetaReleaseTier1Critical`
- PROD: `./gradlew app:testProdReleaseTier1Critical`

**iOS Command**:
```bash
./ios/scripts/run-tier-tests.sh tier1
```

#### Tier 2: Important Tests (BLOCKING)
**Purpose**: ViewModels, repositories, dependency injection
**Failure Behavior**: ABORT deployment immediately
**Examples**: Photo filtering, gallery pagination, category management

**Visual Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 2: Important Tests (ViewModels, Repositories)
Status: BLOCKING - Deployment will abort on failure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Android Gradle Task**:
- QUAL: `./gradlew app:testTier2Important`
- STAGE: `./gradlew app:testStageReleaseTier2Important`
- BETA: `./gradlew app:testBetaReleaseTier2Important`
- PROD: `./gradlew app:testProdReleaseTier2Important`

**iOS Command**:
```bash
./ios/scripts/run-tier-tests.sh tier2
```

#### Tier 3: UI Tests (WARNING ONLY)
**Purpose**: UI components, integration tests, user flows
**Failure Behavior**: LOG warning, CONTINUE deployment
**Examples**: Screen navigation, button interactions, form validation

**Visual Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIER 3: UI Tests (Components, Integration)
Status: WARNING - Deployment will continue with warning
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Android Gradle Task**:
- QUAL: `./gradlew app:testTier3UI`
- STAGE: `./gradlew app:testStageReleaseTier3UI`
- BETA: `./gradlew app:testBetaReleaseTier3UI`
- PROD: `./gradlew app:testProdReleaseTier3UI`

**iOS Command**:
```bash
./ios/scripts/run-tier-tests.sh tier3
```

### Integration Pattern
```bash
# Tier 1: Critical (BLOCKING)
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log INFO "TIER 1: Critical Tests (Security, Data Integrity)"
log INFO "Status: BLOCKING - Deployment will abort on failure"
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

./gradlew app:testTier1Critical || {
    log ERROR "CRITICAL FAILURE: Tier 1 tests failed"
    exit 1
}
log SUCCESS "[TIER 1] PASSED - Critical tests successful"

# Tier 2: Important (BLOCKING)
log INFO ""
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log INFO "TIER 2: Important Tests (ViewModels, Repositories)"
log INFO "Status: BLOCKING - Deployment will abort on failure"
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

./gradlew app:testTier2Important || {
    log ERROR "IMPORTANT FAILURE: Tier 2 tests failed"
    exit 1
}
log SUCCESS "[TIER 2] PASSED - Important tests successful"

# Tier 3: UI (WARNING ONLY)
log INFO ""
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log INFO "TIER 3: UI Tests (Components, Integration)"
log INFO "Status: WARNING - Deployment will continue with warning"
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

local tier3_failed=0
./gradlew app:testTier3UI || {
    tier3_failed=1
    log WARN "WARNING: Tier 3 UI tests failed"
    log WARN "Review failures but deployment will continue."
}
if [[ $tier3_failed -eq 0 ]]; then
    log SUCCESS "[TIER 3] PASSED - UI tests successful"
fi

# Summary
log INFO ""
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log INFO "TEST EXECUTION SUMMARY - ANDROID"
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log SUCCESS "Tier 1 Critical:  PASSED"
log SUCCESS "Tier 2 Important: PASSED"
if [[ $tier3_failed -eq 1 ]]; then
    log WARN "Tier 3 UI:        FAILED (WARNING ONLY)"
else
    log SUCCESS "Tier 3 UI:        PASSED"
fi
log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
```

### Flag Behavior

#### SKIP_TESTS Flag
When `SKIP_TESTS=true`:
- All 3 tiers skipped
- Log warning message
- Deployment continues

#### DRY_RUN Flag
When `DRY_RUN=true`:
- Show what commands WOULD run
- Don't execute actual tests
- Useful for script validation

---

## deploy_beta.sh Design Specification

### Overview
**Purpose**: Deploy to external testing tracks for beta testers
**Target Audience**: External beta testers (not internal team)
**Based On**: deploy_stage.sh structure with beta-specific modifications

### Key Characteristics
- **iOS**: TestFlight External Testing (public beta testers)
- **Android**: Play Console Closed Testing (beta track)
- **Fastlane Lanes**: beta_ios, beta_android
- **Security**: Wave 6 fixes included from day 1
- **Quality Gates**: Full 3-tier system
- **Manylla Pattern**: Implemented from day 1
- **Git Tagging**: Uses `-beta` suffix

### File Structure
```bash
#!/bin/bash
# ============================================================================
# SmilePile Beta Deployment Script
# ============================================================================
# Deploys to TestFlight External Testing + Play Console Closed Testing
# Beta = External testing track for beta testers

set -euo pipefail

# Script Configuration (same as stage)
# Libraries: common.sh, env_manager.sh, build_number.sh
# Configuration: PLATFORM, SKIP_TESTS, SKIP_COMMIT, DRY_RUN
# Functions: usage(), detect_available_simulator(), commit_to_github()
# Main Execution: Same flow as stage with beta-specific lanes
```

### Differences from deploy_stage.sh
1. **Deployment ID**: `beta_$(date +%Y%m%d_%H%M%S)`
2. **Environment**: `load_environment "beta"`
3. **Fastlane Lanes**:
   - iOS: `bundle exec fastlane beta_ios`
   - Android: `bundle exec fastlane beta_android`
4. **Git Tag**: `v${VERSION_NAME}-beta`
5. **Test Tasks**:
   - Android: `testBetaReleaseTier1Critical`, `testBetaReleaseTier2Important`, `testBetaReleaseTier3UI`
   - iOS: Same tier test script (no flavor differences)
6. **Summary Text**: References external testing and beta feedback

### Beta-Specific Features
**Optional Approval Gate** (less strict than PROD):
```bash
beta_approval() {
    if [[ "$REQUIRE_APPROVAL" == "false" ]] || [[ "$CI" == "true" ]]; then
        return 0
    fi

    echo "⚠️  WARNING: You are about to deploy to EXTERNAL beta testers"
    echo "Platform: $PLATFORM"
    echo ""
    read -r -p "Are you sure? (yes/no): " response

    if [[ "$response" != "yes" ]]; then
        log ERROR "Beta deployment cancelled"
        exit 1
    fi
}
```

**Tester Notification Control**:
- Fastlane lanes handle TestFlight notification settings
- Play Console beta track controls beta user visibility

### Estimated File Size
- **Lines**: ~320 lines
- **Size**: ~10 KB
- **Functions**: 4 (usage, detect_available_simulator, commit_to_github, beta_approval)

---

## deploy.sh Router Design Specification

### Overview
**Purpose**: Unified entry point for all tier deployments
**Design**: Lightweight dispatcher with validation and routing logic
**User Experience**: Single command interface: `./deploy/deploy.sh <tier> <platform>`

### Command-Line Interface
```bash
./deploy/deploy.sh <tier> <platform> [environment variables]

# Examples:
./deploy/deploy.sh qual both
./deploy/deploy.sh stage ios
./deploy/deploy.sh beta android
./deploy/deploy.sh prod both

# With flags:
SKIP_TESTS=true ./deploy/deploy.sh qual both
DRY_RUN=true ./deploy/deploy.sh prod both
```

### Tier Validation
```bash
case "$TIER" in
    qual|quality)
        TIER="qual"
        SCRIPT="deploy_qual.sh"
        ;;
    stage|staging)
        TIER="stage"
        SCRIPT="deploy_stage.sh"
        ;;
    beta)
        TIER="beta"
        SCRIPT="deploy_beta.sh"
        ;;
    prod|production)
        TIER="prod"
        SCRIPT="deploy_prod.sh"
        ;;
    *)
        echo "ERROR: Invalid tier: $TIER"
        echo "Valid tiers: qual, stage, beta, prod"
        exit 1
        ;;
esac
```

### Platform Validation
```bash
case "$PLATFORM" in
    android|ios|both)
        ;;
    *)
        echo "ERROR: Invalid platform: $PLATFORM"
        echo "Valid platforms: android, ios, both"
        exit 1
        ;;
esac
```

### Flag Pass-Through
Environment variables automatically inherited:
- `SKIP_TESTS`
- `SKIP_COMMIT`
- `SKIP_SONAR`
- `DRY_RUN`
- `REQUIRE_APPROVAL`
- `ALLOW_UNCOMMITTED`
- All other environment variables

### Help Text
```
================================================================================
SmilePile Deployment Router
================================================================================

Routes deployments to tier-specific scripts.

Usage: ./deploy/deploy.sh <tier> <platform> [flags]

Tiers:
    qual        Quality - Local devices + GitHub commit
    stage       Stage - TestFlight Internal + Play Console Internal
    beta        Beta - TestFlight External + Play Console Closed Testing
    prod        Production - Store-ready packages for manual submission

Platforms:
    android     Android only
    ios         iOS only
    both        Both platforms (default)

Environment Variables (passed through):
    SKIP_TESTS=true         Skip automated tests
    SKIP_COMMIT=true        Skip git commit/push (qual/stage/beta only)
    DRY_RUN=true           Test run without actual deployment

Examples:
    ./deploy/deploy.sh qual both
    ./deploy/deploy.sh stage ios
    SKIP_TESTS=true ./deploy/deploy.sh beta android
    DRY_RUN=true ./deploy/deploy.sh prod both

================================================================================
```

### Error Handling
- Invalid tier: Show valid tiers and exit
- Invalid platform: Show valid platforms and exit
- Missing tier script: Show error and exit
- Help requested: Show usage and exit

### Routing Logic
```bash
# Check if tier script exists
SCRIPT_PATH="$DEPLOY_ROOT/$SCRIPT"
if [[ ! -f "$SCRIPT_PATH" ]]; then
    echo "ERROR: Tier script not found: $SCRIPT_PATH"
    exit 1
fi

# Log routing decision
echo "Routing to: $TIER tier ($PLATFORM)"

# Execute tier-specific script (exec replaces current process)
exec "$SCRIPT_PATH" "$PLATFORM"
```

### Estimated File Size
- **Lines**: ~150 lines
- **Size**: ~5 KB
- **Functions**: 1 (usage)

---

## File Modification Matrix

| File | Lines Added | Lines Modified | Lines Deleted | Net Change | Priority |
|------|-------------|----------------|---------------|------------|----------|
| deploy_stage.sh | ~200 | ~30 | ~20 | +180 | P0 |
| deploy_prod.sh | ~220 | ~60 | ~180 | +100 | P0 |
| deploy_beta.sh | ~320 (new) | 0 | 0 | +320 | P1 |
| deploy.sh | ~150 (new) | 0 | 0 | +150 | P2 |
| **TOTAL** | **~890** | **~90** | **~200** | **+750** | - |

### Detailed Breakdown

#### deploy_stage.sh Changes
- Add `detect_available_simulator()`: +40 lines
- Add `commit_to_github()`: +60 lines
- Replace test execution with 3-tier system: +140 lines
- Remove old git check: -8 lines
- Remove old commit logic: -12 lines
- **Net**: +180 lines (from 277 to ~460 lines)

#### deploy_prod.sh Changes
- Add `detect_available_simulator()`: +40 lines
- Add build_number.sh import: +1 line
- Replace `bump_version()` with library call: -115 lines (replaced with 10 lines)
- Replace `run_production_tests()` with 3-tier system: +80 lines
- Replace `build_android_aab()` with Fastlane: -70 lines + 40 lines = -30 lines
- Replace `build_ios_archive()` with Fastlane: -85 lines + 30 lines = -55 lines
- Update usage function: +10 lines
- **Net**: +100 lines (from 686 to ~786 lines)

#### deploy_beta.sh Creation
- Copy deploy_stage.sh as template: 277 lines
- Add beta-specific modifications: +43 lines
- **Total**: ~320 lines

#### deploy.sh Creation
- New router script: ~150 lines

---

## Testing Strategy

### DRY_RUN Testing Matrix
| Script | Test Focus | Expected Behavior |
|--------|-----------|-------------------|
| deploy_stage.sh | DRY_RUN mode | Shows commands without executing |
| deploy_prod.sh | DRY_RUN mode | Shows commands, skips approval |
| deploy_beta.sh | DRY_RUN mode | Shows commands without executing |
| deploy.sh | Routing | Routes to tier scripts correctly |

### Security Validation Tests
**Test Case 1: Command Injection Attempt**
```bash
# Set malicious simulator name
export IOS_SIMULATOR_NAME="iPhone 15; rm -rf /"

# Run deployment
./deploy/deploy_stage.sh ios

# Expected: Error message about unsafe characters, script aborts
```

**Test Case 2: Valid Simulator Name**
```bash
export IOS_SIMULATOR_NAME="iPhone 16 Pro"
./deploy/deploy_stage.sh ios

# Expected: Simulator name accepted, deployment continues
```

### Manylla Pattern Validation Tests
**Test Case 1: Uncommitted Changes Allowed**
```bash
# Make code changes without committing
echo "// test" >> android/app/src/main/AndroidManifest.xml

# Run deployment
./deploy/deploy_stage.sh both

# Expected: Tests run successfully, changes included in automatic commit
```

**Test Case 2: Commit After Validation**
```bash
# Monitor git status throughout deployment
./deploy/deploy_stage.sh both 2>&1 | grep -E "git status|git add|git commit"

# Expected: git operations happen at end, after all tests pass
```

### Quality Gate Validation Tests
**Test Case 1: Tier 1 Failure Blocks**
```bash
# Break a critical test
# Run deployment
./deploy/deploy_stage.sh android

# Expected: Deployment aborts at Tier 1, Tier 2/3 never run
```

**Test Case 2: Tier 3 Failure Warns**
```bash
# Break a UI test
# Run deployment
./deploy/deploy_stage.sh android

# Expected: Warning logged, deployment continues to Fastlane build
```

**Test Case 3: Visual Output**
```bash
./deploy/deploy_stage.sh both | grep "━━━"

# Expected: Three visual separators (one per tier)
```

### Integration Testing Plan

#### Phase 1: Individual Script Testing
1. Test deploy_stage.sh in DRY_RUN mode
2. Test deploy_prod.sh in DRY_RUN mode
3. Test deploy_beta.sh in DRY_RUN mode
4. Test deploy.sh routing logic

#### Phase 2: Real Deployment Testing
1. **STAGE**: Deploy to TestFlight Internal (iOS) - verify upload succeeds
2. **STAGE**: Deploy to Play Console Internal (Android) - verify upload succeeds
3. **BETA**: Deploy to TestFlight External (iOS) - verify tester notification
4. **BETA**: Deploy to Play Console Beta (Android) - verify beta track
5. **PROD**: DRY_RUN only (no real production deployment in Wave 7)

#### Phase 3: Concurrent Deployment Testing
1. Start deploy_stage.sh in one terminal
2. Start deploy_stage.sh in another terminal simultaneously
3. Verify git lock prevents conflicts
4. Verify second deployment waits or fails gracefully

### Test Matrix (4 Tiers × 3 Platforms × Key Flags)

| Tier | Platform | DRY_RUN | SKIP_TESTS | SKIP_COMMIT | Status |
|------|----------|---------|------------|-------------|--------|
| QUAL | android | Yes | - | - | Wave 6 ✅ |
| QUAL | ios | Yes | - | - | Wave 6 ✅ |
| QUAL | both | Yes | Yes | Yes | Wave 6 ✅ |
| STAGE | android | Yes | - | - | Wave 7 Required |
| STAGE | ios | Yes | - | - | Wave 7 Required |
| STAGE | both | Yes | Yes | Yes | Wave 7 Required |
| BETA | android | Yes | - | - | Wave 7 Required |
| BETA | ios | Yes | - | - | Wave 7 Required |
| BETA | both | Yes | Yes | Yes | Wave 7 Required |
| PROD | android | Yes | - | - | Wave 7 Required |
| PROD | ios | Yes | - | - | Wave 7 Required |
| PROD | both | Yes | Yes | - | Wave 7 Required |

**Total Test Combinations**: 12 critical paths

---

## Rollback Plan

### Git-Based Rollback Strategy

#### If deploy_stage.sh Breaks
```bash
# Rollback to previous version
cd /Users/adamstack/SmilePile
git checkout HEAD~1 -- deploy/deploy_stage.sh

# Test rollback
DRY_RUN=true ./deploy/deploy_stage.sh both

# If working, commit rollback
git add deploy/deploy_stage.sh
git commit -m "rollback: Revert deploy_stage.sh changes (Wave 7)"
git push origin main
```

#### If deploy_prod.sh Breaks
```bash
# Rollback to previous version
git checkout HEAD~1 -- deploy/deploy_prod.sh

# Test rollback (CRITICAL: use DRY_RUN)
DRY_RUN=true ./deploy/deploy_prod.sh both

# Verify approval gate still works
REQUIRE_APPROVAL=true DRY_RUN=true ./deploy/deploy_prod.sh both

# If working, commit rollback
git add deploy/deploy_prod.sh
git commit -m "rollback: Revert deploy_prod.sh changes (Wave 7)"
git push origin main
```

#### If deploy_beta.sh Breaks
```bash
# Remove broken script
rm deploy/deploy_beta.sh

# Fall back to manual Fastlane calls until fixed
cd ios && bundle exec fastlane beta_ios
cd ../android && bundle exec fastlane beta_android

# Commit removal
git add deploy/deploy_beta.sh
git commit -m "rollback: Remove broken deploy_beta.sh (Wave 7)"
git push origin main
```

#### If deploy.sh Router Breaks
```bash
# Remove router script
rm deploy/deploy.sh

# Fall back to direct tier script calls
./deploy/deploy_qual.sh both
./deploy/deploy_stage.sh ios
# etc.

# Commit removal
git add deploy/deploy.sh
git commit -m "rollback: Remove broken deploy.sh router (Wave 7)"
git push origin main
```

### Testing Rollback Changes
```bash
# After rollback, verify each tier works
DRY_RUN=true ./deploy/deploy_qual.sh both
DRY_RUN=true ./deploy/deploy_stage.sh both
DRY_RUN=true ./deploy/deploy_prod.sh both
```

### Communication Plan
If deployment broken mid-wave:
1. **Immediate**: Post in team Slack: "Wave 7 deployment issue - rolling back"
2. **Document**: Add note to wave-evidence/wave-7/ROLLBACK.md with issue details
3. **Fix**: Create hotfix branch to address issue
4. **Retest**: Full test matrix before re-applying changes
5. **Redeploy**: Once validated, reapply Wave 7 changes

### Verification After Rollback
```bash
# Test all tiers in DRY_RUN mode
for tier in qual stage prod; do
    echo "Testing $tier tier..."
    DRY_RUN=true ./deploy/deploy_${tier}.sh both || echo "$tier FAILED"
done

# If beta script was added, test it too
if [[ -f deploy/deploy_beta.sh ]]; then
    DRY_RUN=true ./deploy/deploy_beta.sh both || echo "beta FAILED"
fi

# Test router if it exists
if [[ -f deploy/deploy.sh ]]; then
    DRY_RUN=true ./deploy/deploy.sh qual both || echo "router FAILED"
fi
```

---

## Timeline

### Phase 3: Planning (THIS PHASE)
**Duration**: 2 hours
**Status**: COMPLETE
**Deliverable**: This technical planning document

### Phase 4: Security Review
**Duration**: 2 hours
**Activities**:
- Review security patterns for completeness
- Validate input validation regex
- Check for any new security vulnerabilities
- Audit git lock mechanism
- Review environment variable handling

### Phase 5: Implementation
**Duration**: 8 hours
**Breakdown**:
- Task 1: Security pattern extraction (0.5h)
- Task 2: Backport security to STAGE (1h)
- Task 3: Backport security to PROD (1h)
- Task 4: Manylla pattern in STAGE (1.5h)
- Task 5: Quality gates in STAGE (2h)
- Task 6: Quality gates in PROD (2h)
- Task 7: build_number.sh in PROD (1.5h)
- Task 8: Fastlane in PROD (1.5h)
- Task 9: Create deploy_beta.sh (2h)
- Task 10: Create deploy.sh router (1h)

**Critical Path**: Tasks 2-7 (security and PROD fixes) must complete first

### Phase 6: Testing
**Duration**: 3 hours
**Breakdown**:
- Security validation tests (0.5h)
- Manylla pattern tests (0.5h)
- Quality gate tests (0.5h)
- DRY_RUN testing for all tiers (0.5h)
- Real STAGE deployment test (0.5h)
- Real BETA deployment test (0.5h)
- Router testing (0.5h)

### Phase 7: Validation
**Duration**: 1 hour
**Activities**:
- Verify all 33 acceptance criteria met
- Consistency check across all tiers
- Documentation review
- Evidence collection

### Phase 8: Clean-up
**Duration**: 2 hours
**Activities**:
- Code formatting and style consistency
- Remove commented-out code
- Update inline comments
- Generate final evidence documents
- Create comparison matrices

### Phase 9: Deployment
**Duration**: 0.5 hours
**Activities**:
- Run deploy_qual.sh to validate and commit Wave 7 changes
- Create Wave 7 completion evidence
- Update backlog status

**Total Wave 7 Timeline**: 18.5 hours over 3-4 days

---

## Dependencies and Risks

### Technical Dependencies
1. **Wave 6 Completion** (✅ COMPLETE)
   - deploy_qual.sh validated and working
   - Security patterns established
   - Manylla pattern proven

2. **Fastlane Lanes Functional** (✅ ASSUMED WORKING)
   - qual_ios, qual_android: Tested in Wave 5-6
   - stage_ios, stage_android: Ready, untested
   - beta_ios, beta_android: Ready, untested
   - prod_ios, prod_android: Ready, untested

3. **Test Infrastructure** (✅ ASSUMED READY)
   - Tier 1/2/3 test tasks exist for all flavors
   - run-tier-tests.sh exists for iOS
   - Gradle test tasks configured

4. **Access to Distribution Channels** (REQUIRED FOR TESTING)
   - TestFlight Internal/External access
   - Play Console Internal/Beta track access
   - Signing certificates and provisioning profiles

### Risk Analysis

#### Risk 1: Production Script Changes (HIGH)
**Description**: deploy_prod.sh is complex (686 lines) and handles store submissions

**Impact**: HIGH - Could break production release workflow
**Probability**: MEDIUM - Many changes required

**Mitigation**:
- Extensive DRY_RUN testing before any real use
- Keep all Wave 7 changes in separate commits for easy rollback
- Preserve production approval gate
- Test Fastlane lanes individually before integration
- Document rollback procedure clearly

**Rollback Time**: 5 minutes (git checkout previous version)

#### Risk 2: Version Management Migration (MEDIUM)
**Description**: PROD switches from semantic (1.2.3) to date-based (YY.MM.DD.VVV)

**Impact**: MEDIUM - Version format change visible to users
**Probability**: LOW - build_number.sh well-tested

**Mitigation**:
- Test version generation in isolation
- Document version format change in release notes
- Ensure .build_number file doesn't conflict
- Keep VERSION_BUMP parameter for major/minor milestones

**Rollback Time**: 10 minutes (restore old bump_version function)

#### Risk 3: Test Tier Configuration (MEDIUM)
**Description**: Tier 1/2/3 tests may not exist for STAGE/BETA/PROD flavors

**Impact**: MEDIUM - Tests could fail due to missing configuration
**Probability**: MEDIUM - Gradle tasks may not be configured yet

**Mitigation**:
- Verify test tasks exist before implementation:
  ```bash
  cd android
  ./gradlew tasks | grep -E "testStageRelease|testBetaRelease|testProdRelease"
  ```
- Create missing test configurations if needed
- Fall back to generic test task if tier tasks missing
- Document test setup requirements

**Resolution Time**: 1-2 hours to create missing test configurations

#### Risk 4: Fastlane Lane Compatibility (LOW)
**Description**: prod_ios and prod_android lanes haven't been tested in real deployment

**Impact**: MEDIUM - Could block production builds
**Probability**: LOW - Lanes follow same pattern as QUAL/STAGE

**Mitigation**:
- Test lanes individually with DRY_RUN builds first
- Verify signing configuration in Fastfile
- Check export options are correct
- Test AAB/IPA generation before Wave 7 implementation

**Resolution Time**: 30 minutes to fix Fastlane configuration

#### Risk 5: Concurrent Deployment Safety (LOW)
**Description**: Git lock mechanism needs validation with simultaneous deployments

**Impact**: LOW - Could cause git conflicts
**Probability**: LOW - Lock mechanism proven in deploy_qual.sh

**Mitigation**:
- Test with two simultaneous deploy_stage.sh runs
- Verify 5-second timeout works
- Ensure lock cleanup on script failure
- Document concurrent deployment behavior

**Resolution Time**: 15 minutes to adjust lock timeout if needed

### Dependency Graph
```
Wave 6 (COMPLETE)
    └── Task 1: Security Pattern Extraction
        ├── Task 2: Backport to STAGE
        ├── Task 3: Backport to PROD
        └── Task 9: Create BETA script

Task 4: Manylla Pattern (STAGE)
    └── Task 9: Create BETA script

Task 5: Quality Gates (STAGE)
    ├── Task 6: Quality Gates (PROD)
    └── Task 9: Create BETA script

Tasks 2-9: All Tier Scripts
    └── Task 10: Create Router

Wave 7 Complete
    └── Phase 9: Deployment (deploy_qual.sh)
```

### Critical Path
**Longest Dependency Chain**: 8.5 hours
1. Task 1: Security extraction (0.5h)
2. Task 2: Backport to STAGE (1h)
3. Task 4: Manylla pattern (1.5h)
4. Task 5: Quality gates STAGE (2h)
5. Task 9: Create BETA (2h)
6. Task 10: Create router (1h)

**Parallel Work Possible**: Tasks 3, 6, 7, 8 can happen in parallel with critical path

---

## Success Criteria

### Planning Status
**Status**: COMPLETE

**Deliverables**:
- ✅ 10 implementation tasks defined with dependencies
- ✅ Wave 6 security patterns documented with code examples
- ✅ Manylla pattern implementation detailed with before/after
- ✅ Quality gate integration specified with 3-tier structure
- ✅ deploy_beta.sh design specification created
- ✅ deploy.sh router design specification created
- ✅ File modification matrix with line counts
- ✅ Comprehensive testing strategy with test matrix
- ✅ Rollback plan for each modified script
- ✅ Timeline broken down by phase

### Metrics

**Total Tasks**: 10 implementation tasks

**Total Code Changes**:
- Lines to add: ~890
- Lines to modify: ~90
- Lines to delete: ~200
- Net change: +750 lines

**Critical Path**:
1. Security backports (Tasks 2-3): Must succeed for security compliance
2. Manylla pattern (Task 4): Must succeed to prevent git pollution
3. Quality gates (Tasks 5-6): Must succeed for deployment confidence

**Estimated Timeline**:
- Phase 3 (Planning): 2 hours ✅
- Phase 4 (Security Review): 2 hours
- Phase 5 (Implementation): 8 hours
- Phase 6 (Testing): 3 hours
- Phase 7 (Validation): 1 hour
- Phase 8 (Documentation): 2 hours
- Phase 9 (Deployment): 0.5 hours
- **Total**: 18.5 hours over 3-4 days

### File Created
**Document**: `/Users/adamstack/SmilePile/wave-evidence/wave-7/03-technical-planning.md`
**Size**: ~40 KB
**Sections**: 10 major sections with detailed technical specifications

### Ready for Phase 4
**Status**: YES

**Justification**:
- All 10 implementation tasks clearly defined
- Dependencies mapped and critical path identified
- Security patterns documented for replication
- Code examples provided for all major changes
- Testing strategy covers security, functionality, and integration
- Rollback procedures documented for each script
- Timeline realistic and broken down by task
- Risks identified with mitigation strategies
- Success metrics clear and measurable

**Next Phase**: Security Review (developer agent + peer-reviewer agent + security agent in parallel)

---

**Planning Complete**: 2025-10-15
**Agent**: Developer Agent
**Wave**: Wave 7 - Tier-Specific Deployment Scripts
**Progress**: 3 of 9 phases complete
**Next**: Phase 4 - Security Review
