#!/bin/bash
# ============================================================================
# SmilePile Quality Deployment Script
# ============================================================================
# Deploys to local devices/emulators and commits to GitHub
# Quality = Test locally + Share with team + Commit to repo

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
SKIP_SONAR="${SKIP_SONAR:-false}"
SKIP_COMMIT="${SKIP_COMMIT:-false}"
ALLOW_UNCOMMITTED="${ALLOW_UNCOMMITTED:-false}"
AUTO_COMMIT="${AUTO_COMMIT:-true}"
COMMIT_MESSAGE="${COMMIT_MESSAGE:-}"
TAG_VERSION="${TAG_VERSION:-true}"
DRY_RUN="${DRY_RUN:-false}"

# Deployment tracking
DEPLOYMENT_ID="qual_$(date +%Y%m%d_%H%M%S)"
LOG_FILE="${LOG_DIR}/deploy_${DEPLOYMENT_ID}.log"

# ============================================================================
# Functions
# ============================================================================

usage() {
    cat << EOF
================================================================================
SmilePile Quality Deployment Script
================================================================================

Builds, tests, deploys to local devices, and commits to GitHub.

Usage: $0 [platform] [options]

Platforms:
    android     Deploy to Android emulators and ADB devices
    ios         Deploy to iOS simulators and connected devices
    both        Deploy to both platforms (default)

Environment Variables:
    SKIP_TESTS=true         Skip automated tests
    SKIP_SONAR=true         Skip SonarCloud code analysis
    SKIP_COMMIT=true        Skip git commit/push
    ALLOW_UNCOMMITTED=true  Allow deployment with uncommitted changes
    AUTO_COMMIT=false       Don't auto-commit changes
    COMMIT_MESSAGE="msg"    Custom commit message
    TAG_VERSION=false       Don't create version tag
    DRY_RUN=true           Test run without actual deployment

Examples:
    # Deploy to Android devices and commit
    $0 android

    # Deploy both platforms without committing
    SKIP_COMMIT=true $0 both

    # Deploy with custom commit message
    COMMIT_MESSAGE="feat: new photo editor" $0

    # Dry run to see what would happen
    DRY_RUN=true $0

EOF
    exit 0
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check common tools
    command -v git >/dev/null 2>&1 || missing_tools+=("git")
    command -v jq >/dev/null 2>&1 || missing_tools+=("jq (install via: brew install jq)")

    # Check Android tools if deploying Android
    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        command -v adb >/dev/null 2>&1 || missing_tools+=("adb")
        [[ -n "${ANDROID_HOME:-}" ]] || {
            log WARN "ANDROID_HOME not set"
        }
    fi

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

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log ERROR "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi

    log SUCCESS "All prerequisites met"
}

# Run SonarCloud analysis
run_sonarcloud_analysis() {
    print_header "Running SonarCloud Analysis"

    if [[ "$SKIP_SONAR" == "true" ]]; then
        log WARN "SonarCloud analysis skipped by configuration"
        return 0
    fi

    log INFO "Running code quality analysis with SonarCloud..."

    if [[ -f "$PROJECT_ROOT/scripts/sonar-analysis.sh" ]]; then
        if "$PROJECT_ROOT/scripts/sonar-analysis.sh" 2>&1 | tee -a "$LOG_FILE"; then
            log SUCCESS "SonarCloud analysis completed successfully"
            log INFO "View results at: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile"
        else
            log WARN "SonarCloud analysis failed - continuing deployment"
            # Don't fail deployment on SonarCloud issues
        fi
    else
        log WARN "SonarCloud script not found - skipping analysis"
    fi
}

# Run tests
run_tests() {
    local platform=$1

    if [[ "$SKIP_TESTS" == "true" ]]; then
        log WARN "Tests skipped by configuration"
        return 0
    fi

    print_header "Tiered Test Execution - $platform"

    case "$platform" in
        android)
            cd "$PROJECT_ROOT/android"

            # Tier 1: Critical Tests (BLOCKING)
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TIER 1: Critical Tests (Security, Data Integrity)"
            log INFO "Status: BLOCKING - Deployment will abort on failure"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./gradlew app:testTier1Critical"
            else
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
                log SUCCESS "[TIER 1] PASSED - Critical tests successful"
            fi

            # Tier 2: Important Tests (BLOCKING)
            log INFO ""
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            log INFO "TIER 2: Important Tests (ViewModels, Repositories)"
            log INFO "Status: BLOCKING - Deployment will abort on failure"
            log INFO "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./gradlew app:testTier2Important"
            else
                local tier2_output="/tmp/tier2-android-output.txt"
                ./gradlew app:testTier2Important 2>&1 | tee "$tier2_output"
                local tier2_exit=${PIPESTATUS[0]}

                if [[ $tier2_exit -ne 0 ]]; then
                    log ERROR "IMPORTANT FAILURE: Tier 2 tests failed"
                    log ERROR "Analyzing failures..."

                    # Track failures and trigger workflow if NEW failures detected
                    "$PROJECT_ROOT/scripts/test-failure-tracker.sh" tier2 "$tier2_output" || {
                        log ERROR "Deployment ABORTED."
                        exit 1
                    }
                    exit 1
                fi
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
                log INFO "DRY RUN: Would run: ./gradlew app:testTier3UI"
            else
                local tier3_output="/tmp/tier3-android-output.txt"
                ./gradlew app:testTier3UI 2>&1 | tee "$tier3_output"
                local tier3_exit=${PIPESTATUS[0]}

                if [[ $tier3_exit -ne 0 ]]; then
                    tier3_failed=1
                    log WARN "WARNING: Tier 3 UI tests failed"
                    log WARN "Analyzing failures..."

                    # Track failures and create tech debt story if NEW failures detected
                    "$PROJECT_ROOT/scripts/test-failure-tracker.sh" tier3 "$tier3_output" || true

                    log WARN "These tests verify UI components and user flows."
                    log WARN "Review failures but deployment will continue."
                else
                    log SUCCESS "[TIER 3] PASSED - UI tests successful"
                fi
            fi

            # Generate coverage report
            log INFO ""
            log INFO "Generating test coverage report..."
            if [[ "$DRY_RUN" != "true" ]]; then
                ./gradlew jacocoQualDebugTestReport --continue || log WARN "Coverage report generation failed"

                local coverage_report="$PROJECT_ROOT/android/app/build/reports/jacoco/jacocoQualDebugTestReport/html/index.html"
                if [[ -f "$coverage_report" ]]; then
                    log SUCCESS "Coverage report: $coverage_report"
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
            ;;

        ios)
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
                    local tier1_output="/tmp/tier1-ios-output.txt"
                    ./ios/scripts/run-tier-tests.sh tier1 2>&1 | tee "$tier1_output"
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
                    local tier2_output="/tmp/tier2-ios-output.txt"
                    ./ios/scripts/run-tier-tests.sh tier2 2>&1 | tee "$tier2_output"
                    local tier2_exit=${PIPESTATUS[0]}

                    if [[ $tier2_exit -ne 0 ]]; then
                        log ERROR "IMPORTANT FAILURE: Tier 2 tests failed"
                        log ERROR "Analyzing failures..."

                        # Track failures and trigger workflow if NEW failures detected
                        "$PROJECT_ROOT/scripts/test-failure-tracker.sh" tier2 "$tier2_output" || {
                            log ERROR "Deployment ABORTED."
                            exit 1
                        }
                        exit 1
                    fi
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
                    local tier3_output="/tmp/tier3-ios-output.txt"
                    ./ios/scripts/run-tier-tests.sh tier3 2>&1 | tee "$tier3_output"
                    local tier3_exit=${PIPESTATUS[0]}

                    if [[ $tier3_exit -ne 0 ]]; then
                        tier3_failed=1
                        log WARN "WARNING: Tier 3 UI tests failed"
                        log WARN "Analyzing failures..."

                        # Track failures and create tech debt story if NEW failures detected
                        "$PROJECT_ROOT/scripts/test-failure-tracker.sh" tier3 "$tier3_output" || true

                        log WARN "These tests verify UI components and user flows."
                        log WARN "Review failures but deployment will continue."
                    else
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
            ;;
    esac
}

# Get approved iOS simulators (iOS 26.0 only)
get_approved_simulators() {
    # Approved simulator UDIDs (iOS 26.0)
    local -a APPROVED_SIMS=(
        "CC571231-D473-43E5-97F1-83F8289D8153"  # iPhone 17
        "8314ACBC-020A-451F-A94F-B8D9B27227FD"  # iPhone 17 Pro Max
        "B9D4F952-EE7D-45FC-B75F-F4F5ED2C169C"  # iPad Pro 13-inch (M4)
    )

    # Return all approved simulators
    printf "%s\n" "${APPROVED_SIMS[@]}"
}

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

    # Try booted approved simulators first
    local approved_sims=$(get_approved_simulators)
    for sim_id in $approved_sims; do
        local is_booted=$(xcrun simctl list devices 2>/dev/null | grep "$sim_id" | grep "Booted" || true)
        if [[ -n "$is_booted" ]]; then
            echo "$sim_id"
            return 0
        fi
    done

    # Return first approved simulator
    local first_sim=$(echo "$approved_sims" | head -n1)
    if [[ -n "$first_sim" ]]; then
        echo "$first_sim"
        return 0
    fi

    log ERROR "No approved iOS simulators found"
    log ERROR "Approved simulators: iPhone 17, iPhone 17 Pro Max, iPad Pro 13-inch (M4)"
    return 1
}

# Get approved Android emulators
get_approved_emulators() {
    # Approved emulator names
    local -a APPROVED_EMUS=(
        "Pixel_9"
        "Pixel_9_Pro_XL"
        "Pixel_Tablet"
        "Television_4K"
    )

    # Return all approved emulators
    printf "%s\n" "${APPROVED_EMUS[@]}"
}

# Deploy to Android devices
deploy_android_local() {
    print_header "Android Local Deployment"

    cd "$PROJECT_ROOT/android"

    # Build APK (Wave 5: Using Fastlane qual_android lane)
    log INFO "Building Android APK via Fastlane..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would run: bundle exec fastlane qual_android"
    else
        bundle exec fastlane qual_android || {
            log ERROR "Android Fastlane build failed"
            return 1
        }
    fi

    # Wave 3: APK path with flavor directory and fallback
    local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk"

    # Fallback to old path for backward compatibility during transition
    if [[ ! -f "$apk_path" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log WARN "Flavor APK not found at: $apk_path"
        apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
        if [[ ! -f "$apk_path" ]]; then
            log ERROR "APK not found in either location"
            return 1
        fi
        log INFO "Using fallback APK path: $apk_path"
    fi

    # Get all connected devices (physical devices via ADB)
    log INFO "Checking for Android devices..."
    local all_devices=$(adb devices | grep -E "device$" | cut -f1 || true)
    local physical_devices=""
    local running_emulators=""

    # Separate physical devices from emulators
    for device in $all_devices; do
        if [[ "$device" == emulator-* ]]; then
            running_emulators="$running_emulators $device"
        else
            physical_devices="$physical_devices $device"
        fi
    done

    # Filter running emulators to only approved ones
    local approved_emulators=""
    for emu_serial in $running_emulators; do
        local emu_name=$(adb -s "$emu_serial" emu avd name 2>/dev/null | head -1 | tr -d '\r' || true)
        local approved_list=$(get_approved_emulators)
        if echo "$approved_list" | grep -q "^${emu_name}$"; then
            approved_emulators="$approved_emulators $emu_serial"
        else
            log WARN "Skipping unapproved emulator: $emu_name ($emu_serial)"
        fi
    done

    # Combine approved emulators and physical devices
    local target_devices="$physical_devices $approved_emulators"

    # If no devices found, try to start first approved emulator
    if [[ -z "$target_devices" ]]; then
        log WARN "No approved Android devices or emulators found"

        # Try to start first approved emulator
        log INFO "Attempting to start approved Android emulator..."
        if command -v emulator >/dev/null 2>&1; then
            local available_avds=$(emulator -list-avds)
            local approved_list=$(get_approved_emulators)
            local first_approved=""

            for approved_name in $approved_list; do
                if echo "$available_avds" | grep -q "^${approved_name}$"; then
                    first_approved="$approved_name"
                    break
                fi
            done

            if [[ -n "$first_approved" ]]; then
                if [[ "$DRY_RUN" == "true" ]]; then
                    log INFO "DRY RUN: Would start emulator: $first_approved"
                else
                    log INFO "Starting approved emulator: $first_approved"
                    emulator -avd "$first_approved" -no-window &
                    local emulator_pid=$!

                    # Wait for emulator
                    log INFO "Waiting for emulator to start..."
                    adb wait-for-device
                    sleep 10
                fi
            else
                log WARN "No approved emulators available. Approved: Pixel_9, Pixel_9_Pro_XL, Pixel_Tablet, Television_4K"
            fi
        fi

        # Re-check devices
        all_devices=$(adb devices | grep -E "device$" | cut -f1 || true)
        target_devices=""
        for device in $all_devices; do
            if [[ "$device" == emulator-* ]]; then
                local emu_name=$(adb -s "$device" emu avd name 2>/dev/null || true)
                local approved_list=$(get_approved_emulators)
                if echo "$approved_list" | grep -q "^${emu_name}$"; then
                    target_devices="$target_devices $device"
                fi
            else
                target_devices="$target_devices $device"
            fi
        done
    fi

    if [[ -z "$target_devices" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "No Android devices available for deployment"
        log ERROR "Approved emulators: Pixel_9, Pixel_9_Pro_XL, Pixel_Tablet, Television_4K"
        log ERROR "Also deploys to any physically connected ADB devices"
        return 1
    fi

    # Deploy to each approved device
    for device in $target_devices; do
        local device_type="physical device"
        if [[ "$device" == emulator-* ]]; then
            local emu_name=$(adb -s "$device" emu avd name 2>/dev/null || echo "unknown")
            device_type="emulator ($emu_name)"
        fi

        log INFO "Deploying to $device_type: $device"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would install APK on $device"
        else
            adb -s "$device" install -r "$apk_path" || {
                log ERROR "Failed to install on device: $device"
                continue
            }

            # Launch app (Wave 3: Using qual package name)
            log INFO "Launching app on $device..."
            adb -s "$device" shell monkey -p app.smilepile.qual -c android.intent.category.LAUNCHER 1
        fi

        log SUCCESS "Deployed to $device_type: $device"
    done

    # Copy APK to artifacts with version number
    mkdir -p "$DEPLOY_ROOT/artifacts/qual"
    if [[ "$DRY_RUN" != "true" ]]; then
        cp "$apk_path" "$DEPLOY_ROOT/artifacts/qual/SmilePile-v${VERSION_NAME}-qual.apk"
        log INFO "APK saved to artifacts as SmilePile-v${VERSION_NAME}-qual.apk"
    fi

    log SUCCESS "Android local deployment completed"
}

# Deploy to iOS devices
deploy_ios_local() {
    print_header "iOS Local Deployment"

    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "iOS deployment skipped (not on macOS)"
        return 0
    fi

    cd "$PROJECT_ROOT/ios"

    # Build for simulator using xcodebuild directly for QUAL tier
    log INFO "Building iOS app for simulator..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would run xcodebuild for SmilePile Qual"
    else
        # Clean previous builds
        rm -rf "$PROJECT_ROOT/ios/DerivedData"

        # Build using xcodebuild with correct scheme and configuration
        xcodebuild \
            -project SmilePile.xcodeproj \
            -scheme "SmilePile Qual" \
            -configuration Debug \
            -sdk iphonesimulator \
            -derivedDataPath "$PROJECT_ROOT/ios/DerivedData" \
            -destination "generic/platform=iOS Simulator" \
            clean build || {
            log ERROR "iOS xcodebuild failed"
            return 1
        }
    fi

    local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"

    # Get approved simulators
    log INFO "Checking for approved iOS simulators..."
    local approved_sims=$(get_approved_simulators)
    local booted_approved_sims=""
    local available_approved_sims=""

    # Check which approved simulators are booted
    for sim_id in $approved_sims; do
        local is_booted=$(xcrun simctl list devices 2>/dev/null | grep "$sim_id" | grep "Booted" || true)
        if [[ -n "$is_booted" ]]; then
            booted_approved_sims="$booted_approved_sims $sim_id"
        fi
        available_approved_sims="$available_approved_sims $sim_id"
    done

    # If no approved simulators are booted, boot them all
    if [[ -z "$booted_approved_sims" ]]; then
        log INFO "No approved simulators booted, booting all approved simulators..."
        for sim_id in $approved_sims; do
            local sim_name=$(xcrun simctl list devices 2>/dev/null | grep "$sim_id" | sed -E 's/^[[:space:]]+([^(]+).*/\1/' || echo "Unknown")
            log INFO "Booting approved simulator: $sim_name ($sim_id)"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would boot simulator: $sim_id"
                booted_approved_sims="$booted_approved_sims $sim_id"
            else
                xcrun simctl boot "$sim_id" 2>/dev/null || {
                    log WARN "Simulator $sim_id already booted or failed to boot"
                }
                booted_approved_sims="$booted_approved_sims $sim_id"
            fi
        done

        if [[ "$DRY_RUN" != "true" ]]; then
            sleep 5  # Give simulators time to boot
        fi
    fi

    # Install on all approved simulators (booted only)
    local deployed_count=0
    for sim in $booted_approved_sims; do
        local sim_name=$(xcrun simctl list devices 2>/dev/null | grep "$sim" | sed -E 's/^[[:space:]]+([^(]+).*/\1/' || echo "Unknown")
        log INFO "Installing on approved simulator: $sim_name ($sim)"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would install app on simulator $sim"
            deployed_count=$((deployed_count + 1))
        else
            xcrun simctl install "$sim" "$app_path" || {
                log ERROR "Failed to install on simulator: $sim"
                continue
            }

            # Launch app
            log INFO "Launching app on simulator $sim..."
            xcrun simctl launch "$sim" app.smilepile.qual

            deployed_count=$((deployed_count + 1))
            log SUCCESS "Deployed to simulator: $sim_name"
        fi
    done

    if [[ $deployed_count -eq 0 ]]; then
        log ERROR "Failed to deploy to any approved iOS simulators"
        log ERROR "Approved simulators: iPhone 17, iPhone 17 Pro Max, iPad Pro 13-inch (M4)"
        return 1
    fi

    # Check for connected physical devices
    local devices=$(xcrun devicectl list devices | grep -E "iPhone|iPad" | grep -v "Simulator" || true)
    if [[ -n "$devices" ]]; then
        log INFO "Physical iOS devices detected (requires manual installation via Xcode)"
    fi

    log SUCCESS "iOS local deployment completed"
}

# Commit to GitHub
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
    if [[ -z "$changes" ]] && [[ "$AUTO_COMMIT" != "true" ]]; then
        log INFO "No changes to commit"
        return 0
    fi

    if [[ -n "$changes" ]]; then
        log INFO "Uncommitted changes detected - will be included in commit"
        log INFO "✅ All validation passed - safe to commit"
    fi

    # Generate commit message with version
    local commit_msg="${COMMIT_MESSAGE:-"qual: Deploy ${PLATFORM} - v${VERSION_NAME}"}"

    # Version is already set by build_number.sh
    local version="${VERSION_NAME}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would commit with message: $commit_msg"
        log INFO "DRY RUN: Would tag as: v${version}"
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
        git tag -a "$tag_name" -m "Release version ${version} - Quality deployment"
    fi

    # Push
    log INFO "Pushing to GitHub..."
    git push origin "$(git rev-parse --abbrev-ref HEAD)"

    if [[ "$TAG_VERSION" == "true" ]]; then
        git push origin --tags
    fi

    log SUCCESS "Changes committed and pushed to GitHub"
}

# Generate deployment summary
generate_summary() {
    print_header "Deployment Summary"

    cat << EOF

================================================================================
QUALITY DEPLOYMENT COMPLETED
================================================================================

Deployment ID:     $DEPLOYMENT_ID
Version:           v$VERSION_NAME (Build $VERSION_CODE)
Platform:          $PLATFORM
Timestamp:         $(date)

Git Information:
  Branch:          $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

Artifacts:
  Location:        $DEPLOY_ROOT/artifacts/qual/

Coverage Reports:
  Android:         $PROJECT_ROOT/android/app/build/reports/jacoco/jacocoDebugTestReport/html/index.html
  iOS:             $PROJECT_ROOT/ios/test_results_*.xcresult (use 'xcrun xccov view --report')

Next Steps:
  1. Test the app on deployed devices
  2. Review coverage reports to track test quality
  3. Share APK/IPA with QA team if needed
  4. Once validated, run deploy_prod.sh to prepare store submission

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

    print_header "SmilePile Quality Deployment"

    log INFO "Deployment ID: $DEPLOYMENT_ID"
    log INFO "Platform: $PLATFORM"
    log INFO "Dry Run: $DRY_RUN"

    # Check prerequisites
    check_prerequisites

    # Manylla Pattern: Validate FIRST, then commit
    # Do NOT check git status here - we want to test uncommitted changes
    # Git check happens after validation in commit_to_github()

    # Load quality environment
    load_environment "quality"

    # Update version numbers using StackMap/Manylla methodology
    log INFO "Updating build version..."
    update_version_all_platforms "$PLATFORM" || {
        log ERROR "Failed to update version numbers"
        exit 1
    }

    # Run tests
    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        run_tests "android"
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        run_tests "ios"
    fi

    # Run SonarCloud analysis
    run_sonarcloud_analysis

    # Deploy to local devices
    local deploy_success=true

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        deploy_android_local || deploy_success=false
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        deploy_ios_local || deploy_success=false
    fi

    if [[ "$deploy_success" != "true" ]]; then
        log ERROR "Deployment failed"
        exit 1
    fi

    # Commit to GitHub
    commit_to_github

    # Generate summary
    generate_summary

    # Send notification
    send_notification \
        "Quality Deployment Successful" \
        "Platform: $PLATFORM | ID: $DEPLOYMENT_ID" \
        "success"

    log SUCCESS "Quality deployment completed successfully!"
}

# Run main
main "$@"