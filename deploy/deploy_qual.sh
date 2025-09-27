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

    print_header "Running Tests - $platform"

    case "$platform" in
        android)
            log INFO "Running Android tests..."
            cd "$PROJECT_ROOT/android"

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run: ./gradlew test"
            else
                ./gradlew test || {
                    log ERROR "Android tests failed"
                    return 1
                }
            fi

            log SUCCESS "Android tests passed"
            ;;

        ios)
            if [[ "$OS_TYPE" == "Darwin" ]]; then
                log INFO "Running iOS tests..."
                cd "$PROJECT_ROOT/ios"

                if [[ "$DRY_RUN" == "true" ]]; then
                    log INFO "DRY RUN: Would run: xcodebuild test"
                else
                    xcodebuild test \
                        -project SmilePile.xcodeproj \
                        -scheme SmilePile \
                        -destination 'platform=iOS Simulator,name=iPhone 16' \
                        || {
                        log ERROR "iOS tests failed"
                        return 1
                    }
                fi

                log SUCCESS "iOS tests passed"
            fi
            ;;
    esac
}

# Deploy to Android devices
deploy_android_local() {
    print_header "Android Local Deployment"

    cd "$PROJECT_ROOT/android"

    # Build APK
    log INFO "Building Android APK..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would build APK"
    else
        ./gradlew assembleDebug || {
            log ERROR "Android build failed"
            return 1
        }
    fi

    local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

    if [[ ! -f "$apk_path" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "APK not found at: $apk_path"
        return 1
    fi

    # Get connected devices and emulators
    log INFO "Checking for Android devices..."
    local devices=$(adb devices | grep -E "device$|emulator" | cut -f1 || true)

    if [[ -z "$devices" ]]; then
        log WARN "No Android devices found"

        # Try to start emulator
        log INFO "Attempting to start Android emulator..."
        if command -v emulator >/dev/null 2>&1; then
            local emulator_name=$(emulator -list-avds | head -n1)
            if [[ -n "$emulator_name" ]]; then
                if [[ "$DRY_RUN" == "true" ]]; then
                    log INFO "DRY RUN: Would start emulator: $emulator_name"
                else
                    emulator -avd "$emulator_name" -no-window &
                    local emulator_pid=$!

                    # Wait for emulator
                    log INFO "Waiting for emulator to start..."
                    adb wait-for-device
                    sleep 10
                fi
            fi
        fi

        # Re-check devices
        devices=$(adb devices | grep -E "device$|emulator" | cut -f1 || true)
    fi

    if [[ -z "$devices" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "No Android devices available for deployment"
        return 1
    fi

    # Deploy to each device
    for device in $devices; do
        log INFO "Deploying to device: $device"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would install APK on $device"
        else
            adb -s "$device" install -r "$apk_path" || {
                log ERROR "Failed to install on device: $device"
                continue
            }

            # Launch app
            log INFO "Launching app on $device..."
            adb -s "$device" shell monkey -p com.smilepile -c android.intent.category.LAUNCHER 1
        fi

        log SUCCESS "Deployed to device: $device"
    done

    # Copy APK to artifacts
    mkdir -p "$DEPLOY_ROOT/artifacts/qual"
    if [[ "$DRY_RUN" != "true" ]]; then
        cp "$apk_path" "$DEPLOY_ROOT/artifacts/qual/SmilePile-qual-${DEPLOYMENT_ID}.apk"
        log INFO "APK saved to artifacts"
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

    # Build for simulator
    log INFO "Building iOS app..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would build iOS app"
    else
        xcodebuild build \
            -project SmilePile.xcodeproj \
            -scheme SmilePile \
            -configuration Debug \
            -destination 'platform=iOS Simulator,name=iPhone 16' \
            -derivedDataPath ./DerivedData \
            || {
            log ERROR "iOS build failed"
            return 1
        }
    fi

    local app_path="$PROJECT_ROOT/ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile.app"

    # Get available simulators
    log INFO "Checking for iOS simulators..."
    local booted_sims=$(xcrun simctl list devices | grep "Booted" | cut -d'(' -f2 | cut -d')' -f1 || true)

    if [[ -z "$booted_sims" ]]; then
        log INFO "Starting iOS simulator..."
        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would boot iPhone 15 simulator"
        else
            xcrun simctl boot "iPhone 16" 2>/dev/null || true
            sleep 5
            booted_sims=$(xcrun simctl list devices | grep "Booted" | cut -d'(' -f2 | cut -d')' -f1 || true)
        fi
    fi

    # Install on simulators
    for sim in $booted_sims; do
        log INFO "Installing on simulator: $sim"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would install app on simulator $sim"
        else
            xcrun simctl install "$sim" "$app_path" || {
                log ERROR "Failed to install on simulator: $sim"
                continue
            }

            # Launch app
            log INFO "Launching app on simulator $sim..."
            xcrun simctl launch "$sim" com.smilepile.SmilePile
        fi

        log SUCCESS "Deployed to simulator: $sim"
    done

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

    # Check git status
    local changes=$(git status --porcelain)
    if [[ -z "$changes" ]] && [[ "$AUTO_COMMIT" != "true" ]]; then
        log INFO "No changes to commit"
        return 0
    fi

    # Generate commit message
    local commit_msg="${COMMIT_MESSAGE:-"qual: Deploy ${PLATFORM} - ${DEPLOYMENT_ID}"}"

    # Get version info
    local version="unknown"
    if [[ -f "$PROJECT_ROOT/android/app/build.gradle" ]]; then
        version=$(grep "versionName" "$PROJECT_ROOT/android/app/build.gradle" | head -n1 | cut -d'"' -f2 || echo "unknown")
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would commit with message: $commit_msg"
        log INFO "DRY RUN: Would tag as: qual-v${version}-${DEPLOYMENT_ID}"
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
        local tag_name="qual-v${version}-$(date +%Y%m%d-%H%M%S)"
        log INFO "Creating tag: $tag_name"
        git tag -a "$tag_name" -m "Quality deployment: $DEPLOYMENT_ID"
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
Platform:          $PLATFORM
Timestamp:         $(date)

Git Information:
  Branch:          $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

Artifacts:
  Location:        $DEPLOY_ROOT/artifacts/qual/

Next Steps:
  1. Test the app on deployed devices
  2. Share APK/IPA with QA team if needed
  3. Once validated, run deploy_prod.sh to prepare store submission

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

    # Check git status
    if [[ "$ALLOW_UNCOMMITTED" != "true" ]]; then
        if [[ -n $(git status --porcelain) ]]; then
            log ERROR "Uncommitted changes detected"
            log ERROR "Commit changes or set ALLOW_UNCOMMITTED=true"
            exit 1
        fi
    fi

    # Load quality environment
    load_environment "quality"

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