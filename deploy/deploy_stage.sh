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
    log INFO "Checking prerequisites..."
    command -v bundle >/dev/null 2>&1 || {
        log ERROR "bundler not found. Run: gem install bundler"
        exit 1
    }
    command -v fastlane >/dev/null 2>&1 || {
        log ERROR "fastlane not found. Run: bundle install"
        exit 1
    }

    # Check git status
    if [[ "$ALLOW_UNCOMMITTED" != "true" ]]; then
        if [[ -n $(git status --porcelain) ]]; then
            log ERROR "Uncommitted changes detected"
            log ERROR "Commit changes or set ALLOW_UNCOMMITTED=true"
            exit 1
        fi
    fi

    # Load stage environment
    load_environment "stage"

    # Update version numbers
    log INFO "Updating build version..."
    update_version_all_platforms "$PLATFORM" || {
        log ERROR "Failed to update version numbers"
        exit 1
    }

    # Run tests
    if [[ "$SKIP_TESTS" != "true" ]]; then
        if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
            log INFO "Running Android tests..."
            cd "$PROJECT_ROOT/android"
            ./gradlew app:testStageReleaseTier1Critical app:testStageReleaseTier2Important || {
                log ERROR "Android tests failed"
                exit 1
            }
        fi

        if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
            if [[ "$OS_TYPE" == "Darwin" ]]; then
                log INFO "Running iOS tests..."
                cd "$PROJECT_ROOT"
                ./ios/scripts/run-tier-tests.sh tier1 || {
                    log ERROR "iOS tests failed"
                    exit 1
                }
                ./ios/scripts/run-tier-tests.sh tier2 || {
                    log ERROR "iOS tests failed"
                    exit 1
                }
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

    # Commit version changes
    if [[ "$SKIP_COMMIT" != "true" ]] && [[ "$DRY_RUN" != "true" ]]; then
        cd "$PROJECT_ROOT"
        if [[ -n $(git status --porcelain) ]]; then
            log INFO "Committing version changes..."
            git add .build_number ios/SmilePile/Info.plist android/app/build.gradle.kts
            git commit -m "stage: Deploy ${PLATFORM} - v${VERSION_NAME}"
            git tag -a "v${VERSION_NAME}-stage" -m "Stage deployment v${VERSION_NAME}"
            git push origin "$(git rev-parse --abbrev-ref HEAD)"
            git push origin --tags
            log SUCCESS "Changes committed and pushed"
        fi
    fi

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
