#!/bin/bash
# ============================================================================
# SmilePile Production Deployment Script
# ============================================================================
# Generates AAB for Google Play Store and Archive for App Store
# Production = Create store-ready packages for submission

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
REQUIRE_APPROVAL="${REQUIRE_APPROVAL:-true}"
VERSION_BUMP="${VERSION_BUMP:-patch}"  # major, minor, patch, or specific version
DRY_RUN="${DRY_RUN:-false}"
CLEAN_BUILD="${CLEAN_BUILD:-true}"

# CI Detection
CI="${CI:-false}"
if [[ -n "${GITHUB_ACTIONS:-}" ]] || [[ -n "${JENKINS_HOME:-}" ]] || [[ -n "${GITLAB_CI:-}" ]]; then
    CI="true"
fi

# Deployment tracking
DEPLOYMENT_ID="prod_$(date +%Y%m%d_%H%M%S)"
LOG_FILE="${LOG_DIR}/deploy_${DEPLOYMENT_ID}.log"

# ============================================================================
# Functions
# ============================================================================

usage() {
    cat << EOF
================================================================================
SmilePile Production Deployment Script
================================================================================

Generates store-ready packages for Apple App Store and Google Play Store.

Usage: $0 [platform] [options]

Platforms:
    android     Generate AAB for Google Play Store
    ios         Generate Archive for Apple App Store
    both        Generate both packages (default)

Environment Variables:
    SKIP_TESTS=true         Skip automated tests
    REQUIRE_APPROVAL=false  Skip manual approval (for CI)
    VERSION_BUMP=minor      Version bump type (major/minor/patch)
    CLEAN_BUILD=false       Don't clean before building
    DRY_RUN=true           Test run without actual build

Examples:
    # Generate both store packages
    $0 both

    # Generate Android AAB only
    $0 android

    # Minor version bump for iOS
    VERSION_BUMP=minor $0 ios

    # CI/CD production build
    CI=true REQUIRE_APPROVAL=false $0 both

    # Dry run
    DRY_RUN=true $0

EOF
    exit 0
}

# Wave 7: Proactive security - iOS simulator input validation
# Prevents command injection even though prod tier creates archives, not simulator builds
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

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check Android tools if deploying Android
    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        command -v java >/dev/null 2>&1 || missing_tools+=("java")
        [[ -n "${ANDROID_HOME:-}" ]] || {
            log ERROR "ANDROID_HOME not set"
            exit 1
        }

        # Check for keystore
        if [[ ! -f "${ANDROID_KEYSTORE_PATH:-}" ]] && [[ "$DRY_RUN" != "true" ]]; then
            log ERROR "Android keystore not found. Set ANDROID_KEYSTORE_PATH"
            exit 1
        fi
    fi

    # Check iOS tools if deploying iOS
    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        if [[ "$OS_TYPE" != "Darwin" ]]; then
            log ERROR "iOS deployment requires macOS"
            exit 1
        fi

        command -v xcodebuild >/dev/null 2>&1 || missing_tools+=("xcodebuild")
        command -v xcrun >/dev/null 2>&1 || missing_tools+=("xcrun")
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

    # Phase 4 requirement: Pre-flight credential validation
    if [[ "$DRY_RUN" != "true" ]]; then
        log INFO "Validating credentials..."

        if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
            if [[ ! -f "${ANDROID_KEYSTORE_PATH:-}" ]]; then
                log ERROR "Android keystore not found. Set ANDROID_KEYSTORE_PATH"
                exit 1
            fi

            local keystore_perms=$(stat -f "%Lp" "$ANDROID_KEYSTORE_PATH" 2>/dev/null || stat -c "%a" "$ANDROID_KEYSTORE_PATH" 2>/dev/null)
            if [[ "$keystore_perms" != "600" ]]; then
                log WARN "Keystore file has weak permissions: $keystore_perms"
                log WARN "Recommended: chmod 600 $ANDROID_KEYSTORE_PATH"
            fi
        fi
    fi

    log SUCCESS "All prerequisites met"
}

# Production approval gate
production_approval() {
    if [[ "$REQUIRE_APPROVAL" == "false" ]] || [[ "$CI" == "true" ]] || [[ "$DRY_RUN" == "true" ]]; then
        log INFO "Production approval bypassed (CI/config)"
        return 0
    fi

    print_header "PRODUCTION DEPLOYMENT APPROVAL"

    echo ""
    echo "⚠️  WARNING: You are about to create PRODUCTION store packages"
    echo ""
    echo "Platform:      $PLATFORM"
    echo "Version Bump:  $VERSION_BUMP"
    echo "Clean Build:   $CLEAN_BUILD"
    echo ""
    echo "This will create:"

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        echo "  - Android App Bundle (.aab) for Google Play Store"
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        echo "  - iOS Archive (.xcarchive) for App Store"
    fi

    echo ""
    echo -n "Are you sure you want to proceed? (yes/no): "

    read -r response

    if [[ "$response" != "yes" ]]; then
        log ERROR "Production deployment cancelled by user"
        exit 1
    fi

    log INFO "Production deployment approved"
}

# Wave 7: Version management now uses centralized build_number.sh
# Old bump_version() function removed - using update_version_all_platforms() instead

# Run production tests with 3-tier quality gates
run_production_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log WARN "Tests skipped by configuration"
        return 0
    fi

    print_header "Tiered Test Execution - Production"

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        cd "$PROJECT_ROOT/android"

        # Tier 1: Critical Tests (BLOCKING)
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        log INFO "TIER 1: Critical Tests (Security, Data Integrity)"
        log INFO "Status: BLOCKING - Deployment will abort on failure"
        log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would run: ./gradlew app:testProdReleaseTier1Critical"
        else
            ./gradlew app:testProdReleaseTier1Critical || {
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
            log INFO "DRY RUN: Would run: ./gradlew app:testProdReleaseTier2Important"
        else
            ./gradlew app:testProdReleaseTier2Important || {
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
            log INFO "DRY RUN: Would run: ./gradlew app:testProdReleaseTier3UI"
        else
            ./gradlew app:testProdReleaseTier3UI || {
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
}

# Build Android AAB via Fastlane
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

    echo ""
    echo "📱 Next steps for Android:"
    echo "1. Go to Google Play Console: https://play.google.com/console"
    echo "2. Select your app"
    echo "3. Go to 'Production' > 'Releases'"
    echo "4. Upload AAB: artifacts/production/SmilePile-v${VERSION_NAME}-prod.aab"
    echo "5. Fill in release notes and submit for review"
}

# Build iOS Archive via Fastlane
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

# Generate release notes
generate_release_notes() {
    print_header "Release Notes"

    cd "$PROJECT_ROOT"

    # Get commits since last tag
    local last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    local commits=""

    if [[ -n "$last_tag" ]]; then
        commits=$(git log "$last_tag"..HEAD --pretty=format:"- %s" | head -20)
    else
        commits=$(git log -10 --pretty=format:"- %s")
    fi

    cat > "$DEPLOY_ROOT/artifacts/production/RELEASE_NOTES.md" << EOF
# SmilePile Release Notes
## Version: $(date +%Y.%m.%d)
## Build: ${DEPLOYMENT_ID}

### What's New
$commits

### Platform Notes
- Android: AAB ready for Google Play Store
- iOS: Archive ready for App Store Connect

### Testing Checklist
- [ ] App launches without crashes
- [ ] Photo capture works
- [ ] Gallery displays correctly
- [ ] Categories function properly
- [ ] Kids mode works as expected
- [ ] Export/Import features work

### Submission Checklist
- [ ] Version numbers updated
- [ ] Store descriptions updated
- [ ] Screenshots prepared
- [ ] Release notes written
- [ ] Compliance information reviewed
EOF

    log INFO "Release notes generated: artifacts/production/RELEASE_NOTES.md"
}

# Generate deployment summary
generate_summary() {
    print_header "Production Deployment Summary"

    cat << EOF

================================================================================
PRODUCTION DEPLOYMENT COMPLETED
================================================================================

Deployment ID:     $DEPLOYMENT_ID
Platform:          $PLATFORM
Timestamp:         $(date)

Artifacts Generated:
EOF

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        echo "  Android AAB:   artifacts/production/SmilePile-${DEPLOYMENT_ID}.aab"
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        echo "  iOS Archive:   artifacts/production/SmilePile-${DEPLOYMENT_ID}.xcarchive"
    fi

    cat << EOF

Store Submission Steps:

GOOGLE PLAY STORE:
  1. Go to https://play.google.com/console
  2. Upload AAB from artifacts/production/
  3. Add release notes and submit

APP STORE CONNECT:
  1. Open Xcode > Window > Organizer
  2. Select archive and click 'Distribute App'
  3. Or use Transporter with the IPA
  4. Complete submission in App Store Connect

Release Notes:    artifacts/production/RELEASE_NOTES.md

================================================================================
EOF
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

    print_header "SmilePile Production Deployment"

    log INFO "Deployment ID: $DEPLOYMENT_ID"
    log INFO "Platform: $PLATFORM"
    log INFO "Version Bump: $VERSION_BUMP"
    log INFO "Dry Run: $DRY_RUN"

    # Check prerequisites
    check_prerequisites

    # Load production environment
    load_environment "production"

    # Production approval gate
    production_approval

    # Update version numbers using centralized build_number.sh
    log INFO "Updating build version..."
    update_version_all_platforms "$PLATFORM" || {
        log ERROR "Failed to update version numbers"
        exit 1
    }

    # Run tests with 3-tier quality gates
    run_production_tests

    # Process each platform
    local deploy_success=true

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        build_android_aab || deploy_success=false
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        build_ios_archive || deploy_success=false
    fi

    if [[ "$deploy_success" != "true" ]]; then
        log ERROR "Production deployment failed"
        exit 1
    fi

    # Generate release notes
    generate_release_notes

    # Generate summary
    generate_summary

    # Send notification
    send_notification \
        "Production Build Ready" \
        "Platform: $PLATFORM | ID: $DEPLOYMENT_ID | Ready for store submission" \
        "success"

    log SUCCESS "Production deployment completed successfully!"
    log INFO "Store packages ready in: $DEPLOY_ROOT/artifacts/production/"
}

# Run main
main "$@"