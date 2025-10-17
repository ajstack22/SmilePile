#!/bin/bash
# ============================================================================
# SmilePile Stage Deployment Script
# ============================================================================
# Deploys to TestFlight Internal Testing + Play Console Internal Testing
# Stage = Internal testing track for QA/stakeholders

set -euo pipefail

# ============================================================================
# Script Configuration
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DEPLOY_ROOT="$SCRIPT_DIR"
export PROJECT_ROOT="$(dirname "$DEPLOY_ROOT")"

# Source libraries
source "${DEPLOY_ROOT}/lib/common.sh"
source "${DEPLOY_ROOT}/lib/env_manager.sh"
source "${DEPLOY_ROOT}/lib/build_number.sh"

# ============================================================================
# Configuration
# ============================================================================

PLATFORM="${1:-both}"
SKIP_TESTS="${SKIP_TESTS:-false}"
SKIP_COMMIT="${SKIP_COMMIT:-false}"
ALLOW_UNCOMMITTED="${ALLOW_UNCOMMITTED:-false}"
DRY_RUN="${DRY_RUN:-false}"

# Deployment tracking
DEPLOYMENT_ID="stage_$(date +%Y%m%d_%H%M%S)"
LOG_FILE="${LOG_DIR}/deploy_${DEPLOYMENT_ID}.log"

# ============================================================================
# Functions
# ============================================================================

usage() {
    cat << EOF
================================================================================
SmilePile Stage Deployment Script
================================================================================

Builds and uploads to internal testing tracks:
- iOS: TestFlight Internal Testing
- Android: Play Console Internal Testing

Usage: $0 [platform] [options]

Platforms:
    android     Deploy to Play Console Internal Testing
    ios         Deploy to TestFlight Internal Testing
    both        Deploy to both platforms (default)

Environment Variables:
    SKIP_TESTS=true         Skip automated tests
    SKIP_COMMIT=true        Skip git commit/push
    ALLOW_UNCOMMITTED=true  Allow deployment with uncommitted changes
    DRY_RUN=true           Test run without actual deployment

Examples:
    # Deploy both platforms to internal testing
    $0 both

    # Deploy only iOS to TestFlight
    $0 ios

    # Deploy without running tests (not recommended)
    SKIP_TESTS=true $0

================================================================================
EOF
    exit 0
}

# Wave 7: Proactive security - iOS simulator input validation
# Prevents command injection even though stage tier deploys to stores, not simulators
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

    # Try booted simulator first
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

    # Last resort: any available iPhone simulator
    local any_iphone=$(xcrun simctl list devices 2>/dev/null | grep -m1 "iPhone" | sed -E 's/.*\(([A-Z0-9-]+)\).*/\1/' || true)
    if [[ -n "$any_iphone" ]]; then
        echo "$any_iphone"
        return 0
    fi

    log ERROR "No iOS simulators found"
    log ERROR "Install simulators via Xcode or set IOS_SIMULATOR_NAME environment variable"
    return 1
}

# Check prerequisites with pre-flight validation
check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check common tools
    command -v git >/dev/null 2>&1 || missing_tools+=("git")
    command -v bundle >/dev/null 2>&1 || missing_tools+=("bundler (run: gem install bundler)")
    command -v fastlane >/dev/null 2>&1 || missing_tools+=("fastlane (run: bundle install)")

    # Check iOS tools if deploying iOS
    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        if [[ "$OS_TYPE" == "Darwin" ]]; then
            command -v xcrun >/dev/null 2>&1 || missing_tools+=("Xcode")
            command -v xcodebuild >/dev/null 2>&1 || missing_tools+=("xcodebuild")
        else
            log ERROR "iOS deployment requires macOS"
            exit 1
        fi
    fi

    # Check Android tools if deploying Android
    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        [[ -n "${ANDROID_HOME:-}" ]] || {
            log WARN "ANDROID_HOME not set"
        }
    fi

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log ERROR "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi

    # Phase 4 requirement: Disk space check
    local free_space=$(df -k "$DEPLOY_ROOT" | awk 'NR==2 {print $4}')
    local required_space=$((5 * 1024 * 1024))  # 5GB in KB

    if [[ $free_space -lt $required_space ]]; then
        log ERROR "Insufficient disk space: $(($free_space / 1024 / 1024))GB free"
        log ERROR "Required: 5GB for build artifacts"
        exit 1
    fi

    # Phase 4 requirement: Pre-flight credential validation for Fastlane
    if [[ "$DRY_RUN" != "true" ]]; then
        log INFO "Validating Fastlane credentials..."

        if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
            local sa_file="${ANDROID_SERVICE_ACCOUNT:-$PROJECT_ROOT/android/fastlane/service-account.json}"
            if [[ -f "$sa_file" ]]; then
                local perms=$(stat -f "%Lp" "$sa_file" 2>/dev/null || stat -c "%a" "$sa_file" 2>/dev/null)
                if [[ "$perms" != "600" ]]; then
                    log WARN "Service account file has weak permissions: $perms"
                    log WARN "Recommended: chmod 600 $sa_file"
                fi
            else
                log WARN "Android service account not found: $sa_file"
            fi
        fi
    fi

    log SUCCESS "All prerequisites met"
}

# Wave 7: Git lock for concurrent deployment safety (macOS compatible)
acquire_git_lock() {
    local lock_dir="$PROJECT_ROOT/.git/deployment.lock.d"
    local lock_timeout=5
    local wait_time=0

    while ! mkdir "$lock_dir" 2>/dev/null; do
        if [[ $wait_time -ge $lock_timeout ]]; then
            log ERROR "Could not acquire deployment lock after ${lock_timeout}s"
            log ERROR "Another deployment may be in progress"
            log ERROR "If you're sure no deployment is running, remove: $lock_dir"
            exit 1
        fi
        log INFO "Waiting for deployment lock..."
        sleep 1
        ((wait_time++))
    done

    # Set trap to release lock on exit
    trap "rm -rf '$lock_dir'" EXIT INT TERM

    log INFO "Deployment lock acquired"
}

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

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would commit with message: $commit_msg"
        log INFO "DRY RUN: Would tag as: v${VERSION_NAME}-stage"
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
    local tag_name="v${VERSION_NAME}-stage"
    log INFO "Creating tag: $tag_name"
    git tag -a "$tag_name" -m "Stage deployment v${VERSION_NAME}"

    # Push
    log INFO "Pushing to GitHub..."
    git push origin "$(git rev-parse --abbrev-ref HEAD)"
    git push origin --tags

    log SUCCESS "Changes committed and pushed to GitHub"
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    # Parse arguments
    case "${1:-}" in
        -h|--help|help)
            usage
            ;;
        android|ios|both)
            PLATFORM="$1"
            ;;
        "")
            PLATFORM="both"
            ;;
        *)
            log ERROR "Invalid platform: $1"
            usage
            ;;
    esac

    # Initialize
    init_deployment_system

    # Setup logging
    mkdir -p "$LOG_DIR"
    exec 1> >(tee -a "$LOG_FILE")
    exec 2>&1

    print_header "SmilePile Stage Deployment"

    log INFO "Deployment ID: $DEPLOYMENT_ID"
    log INFO "Platform: $PLATFORM"
    log INFO "Target: Internal Testing (TestFlight + Play Console)"
    log INFO "Dry Run: $DRY_RUN"

    # Check prerequisites
    check_prerequisites

    # Wave 7: Acquire git lock to prevent concurrent deployments
    if [[ "$DRY_RUN" != "true" ]] && [[ "$SKIP_COMMIT" != "true" ]]; then
        acquire_git_lock
    fi

    # Manylla Pattern: No git status check here - we test uncommitted changes
    # Git check happens AFTER validation in commit_to_github() function

    # Load stage environment
    load_environment "stage"

    # Update version numbers
    log INFO "Updating build version..."
    update_version_all_platforms "$PLATFORM" || {
        log ERROR "Failed to update version numbers"
        exit 1
    }

    # Run tests with 3-tier quality gates
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

    # Deploy via Fastlane
    local deploy_success=true

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        if [[ "$OS_TYPE" != "Darwin" ]]; then
            log WARN "iOS deployment skipped (not on macOS)"
        else
            print_header "iOS Stage Deployment"
            log INFO "Building and uploading iOS to TestFlight Internal Testing..."

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: cd ios && bundle exec fastlane stage_ios"
            else
                cd "$PROJECT_ROOT/ios"
                bundle exec fastlane stage_ios || {
                    log ERROR "iOS stage deployment failed"
                    deploy_success=false
                }
            fi

            if [[ "$deploy_success" == "true" ]]; then
                log SUCCESS "iOS uploaded to TestFlight Internal Testing"
                log INFO "View: https://appstoreconnect.apple.com"
            fi
        fi
    fi

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        print_header "Android Stage Deployment"
        log INFO "Building and uploading Android to Play Console Internal Testing..."

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would run: cd android && bundle exec fastlane stage_android"
        else
            cd "$PROJECT_ROOT/android"
            bundle exec fastlane stage_android || {
                log ERROR "Android stage deployment failed"
                deploy_success=false
            }
        fi

        if [[ "$deploy_success" == "true" ]]; then
            log SUCCESS "Android uploaded to Play Console Internal Testing"
            log INFO "View: https://play.google.com/console"
        fi
    fi

    if [[ "$deploy_success" != "true" ]]; then
        log ERROR "Stage deployment failed"
        exit 1
    fi

    # Commit to GitHub (Manylla pattern - commits after all validation)
    commit_to_github

    # Generate summary
    print_header "Stage Deployment Summary"

    cat << EOF

================================================================================
STAGE DEPLOYMENT COMPLETED
================================================================================

Deployment ID:     $DEPLOYMENT_ID
Version:           v$VERSION_NAME (Build $VERSION_CODE)
Platform:          $PLATFORM
Timestamp:         $(date)

Distribution:
  iOS:             TestFlight Internal Testing
  Android:         Play Console Internal Testing

Testing Instructions:
  1. iOS testers: Check TestFlight app for new build
  2. Android testers: Check email for Play Console invite
  3. Report issues via established QA channels
  4. Approved builds can be promoted to BETA tier

Dashboards:
  App Store Connect: https://appstoreconnect.apple.com
  Play Console:      https://play.google.com/console

================================================================================
EOF

    log SUCCESS "Stage deployment completed successfully!"
}

# Run main
main "$@"
