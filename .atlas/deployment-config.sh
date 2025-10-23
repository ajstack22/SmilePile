#!/bin/bash
# ============================================================================
# SmilePile Deployment Configuration
# ============================================================================
# Project-specific configuration for automated deployment pipelines
# Integrates with Atlas workflow system and SmilePile's 4-tier strategy
# ============================================================================

set -euo pipefail

# ============================================================================
# Project Information
# ============================================================================

PROJECT_NAME="SmilePile"
PROJECT_TYPE="mobile"  # mobile, web, backend, fullstack
PROJECT_PLATFORMS=("ios" "android")
PROJECT_ROOT="${PROJECT_ROOT:-/Users/adamstack/SmilePile}"

# ============================================================================
# Version Configuration
# ============================================================================
# Uses YY.MM.DD.XXX format (e.g., 25.10.17.001)

BUILD_NUMBER_FILE="${PROJECT_ROOT}/.build_number"
VERSION_FORMAT="YY.MM.DD.XXX"  # XXX = 001-999 daily build counter

# Function to get current build number from file
get_current_build_number() {
    if [[ -f "$BUILD_NUMBER_FILE" ]]; then
        local last_date=$(head -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "")
        local last_build=$(tail -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "0")

        if [[ -n "$last_date" ]]; then
            local yy="${last_date:0:2}"
            local mm="${last_date:2:2}"
            local dd="${last_date:4:2}"
            local build_suffix=$(printf "%03d" $last_build)
            echo "${yy}.${mm}.${dd}.${build_suffix}"
        else
            echo "0.0.0.000"
        fi
    else
        echo "0.0.0.000"
    fi
}

# Function to bump build number
bump_build_number() {
    local yy=$(date +%y)
    local mm=$(date +%m)
    local dd=$(date +%d)
    local date_prefix="${yy}${mm}${dd}"
    local daily_build_number=1

    if [[ -f "$BUILD_NUMBER_FILE" ]]; then
        local last_date=$(head -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "")
        local last_build=$(tail -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "0")

        if [[ "$last_date" == "$date_prefix" ]]; then
            daily_build_number=$((last_build + 1))
            if [[ $daily_build_number -gt 999 ]]; then
                echo "ERROR: Maximum daily builds (999) exceeded" >&2
                return 1
            fi
        fi
    fi

    local build_suffix=$(printf "%03d" $daily_build_number)
    local new_version="${yy}.${mm}.${dd}.${build_suffix}"

    # Save for next build
    echo "$date_prefix" > "$BUILD_NUMBER_FILE"
    echo "$daily_build_number" >> "$BUILD_NUMBER_FILE"

    echo "$new_version"
}

# ============================================================================
# Build Configuration
# ============================================================================

# iOS Build Settings
IOS_PROJECT="${PROJECT_ROOT}/ios/SmilePile.xcodeproj"
IOS_BUILD_DIR="${PROJECT_ROOT}/ios/DerivedData"
IOS_SIMULATOR_SDK="iphonesimulator"
IOS_DEVICE_SDK="iphoneos"

# Android Build Settings
ANDROID_PROJECT="${PROJECT_ROOT}/android"
ANDROID_BUILD_DIR="${PROJECT_ROOT}/android/app/build"
ANDROID_GRADLE_WRAPPER="${ANDROID_PROJECT}/gradlew"

# Test Configuration
TEST_COMMAND="${PROJECT_ROOT}/deploy/deploy_qual.sh"
TEST_TIERS=("tier1" "tier2" "tier3")
TIER1_BLOCKING=true  # Critical tests block deployment
TIER2_BLOCKING=true  # Important tests block deployment
TIER3_BLOCKING=false # UI tests don't block (warning only)

# ============================================================================
# Environment Configuration (4-Tier Strategy)
# ============================================================================

DEPLOYMENT_TIERS=("qual" "stage" "beta" "prod")

# Environment-specific settings
declare -A TIER_CONFIG

# QUAL (Development/Testing)
TIER_CONFIG[qual_name]="Quality"
TIER_CONFIG[qual_branch]="main"
TIER_CONFIG[qual_ios_scheme]="SmilePile Qual"
TIER_CONFIG[qual_ios_configuration]="Debug"
TIER_CONFIG[qual_ios_bundle_id]="app.smilepile.qual"
TIER_CONFIG[qual_android_variant]="qualDebug"
TIER_CONFIG[qual_android_package]="app.smilepile.qual"
TIER_CONFIG[qual_requires_signing]=false

# STAGE (Staging)
TIER_CONFIG[stage_name]="Staging"
TIER_CONFIG[stage_branch]="main"
TIER_CONFIG[stage_ios_scheme]="SmilePile Stage"
TIER_CONFIG[stage_ios_configuration]="Stage"
TIER_CONFIG[stage_ios_bundle_id]="app.smilepile"
TIER_CONFIG[stage_android_variant]="stageRelease"
TIER_CONFIG[stage_android_package]="com.smilepile"
TIER_CONFIG[stage_requires_signing]=true

# BETA (Beta Testing)
TIER_CONFIG[beta_name]="Beta"
TIER_CONFIG[beta_branch]="main"
TIER_CONFIG[beta_ios_scheme]="SmilePile Beta"
TIER_CONFIG[beta_ios_configuration]="Beta"
TIER_CONFIG[beta_ios_bundle_id]="app.smilepile"
TIER_CONFIG[beta_android_variant]="betaRelease"
TIER_CONFIG[beta_android_package]="com.smilepile"
TIER_CONFIG[beta_requires_signing]=true

# PROD (Production)
TIER_CONFIG[prod_name]="Production"
TIER_CONFIG[prod_branch]="main"
TIER_CONFIG[prod_ios_scheme]="SmilePile Prod"
TIER_CONFIG[prod_ios_configuration]="Release"
TIER_CONFIG[prod_ios_bundle_id]="app.smilepile"
TIER_CONFIG[prod_android_variant]="prodRelease"
TIER_CONFIG[prod_android_package]="com.smilepile"
TIER_CONFIG[prod_requires_signing]=true

# ============================================================================
# iOS Configuration
# ============================================================================

# iOS Schemes (must match Xcode project exactly)
IOS_SCHEMES=(
    "SmilePile Qual"
    "SmilePile Stage"
    "SmilePile Beta"
    "SmilePile Prod"
)

# iOS Bundle IDs
declare -A IOS_BUNDLE_IDS
IOS_BUNDLE_IDS[qual]="app.smilepile.qual"    # Unique for side-by-side install
IOS_BUNDLE_IDS[stage]="app.smilepile"        # Shared ID
IOS_BUNDLE_IDS[beta]="app.smilepile"         # Shared ID
IOS_BUNDLE_IDS[prod]="app.smilepile"         # Shared ID

# iOS XCConfig Files
declare -A IOS_XCCONFIG_FILES
IOS_XCCONFIG_FILES[qual]="${PROJECT_ROOT}/ios/Qual.xcconfig"
IOS_XCCONFIG_FILES[stage]="${PROJECT_ROOT}/ios/Stage.xcconfig"
IOS_XCCONFIG_FILES[beta]="${PROJECT_ROOT}/ios/Beta.xcconfig"
IOS_XCCONFIG_FILES[prod]="${PROJECT_ROOT}/ios/Prod.xcconfig"

# ============================================================================
# Android Configuration
# ============================================================================

# Android Build Variants
declare -A ANDROID_BUILD_VARIANTS
ANDROID_BUILD_VARIANTS[qual]="qualDebug"
ANDROID_BUILD_VARIANTS[stage]="stageRelease"
ANDROID_BUILD_VARIANTS[beta]="betaRelease"
ANDROID_BUILD_VARIANTS[prod]="prodRelease"

# Android Package Names
declare -A ANDROID_PACKAGE_NAMES
ANDROID_PACKAGE_NAMES[qual]="app.smilepile.qual"   # Unique for side-by-side
ANDROID_PACKAGE_NAMES[stage]="com.smilepile"       # Shared package
ANDROID_PACKAGE_NAMES[beta]="com.smilepile"        # Shared package
ANDROID_PACKAGE_NAMES[prod]="com.smilepile"        # Shared package

# Android APK Paths
declare -A ANDROID_APK_PATHS
ANDROID_APK_PATHS[qual]="${ANDROID_BUILD_DIR}/outputs/apk/qual/debug/app-qual-debug.apk"
ANDROID_APK_PATHS[stage]="${ANDROID_BUILD_DIR}/outputs/apk/stage/release/app-stage-release.apk"
ANDROID_APK_PATHS[beta]="${ANDROID_BUILD_DIR}/outputs/apk/beta/release/app-beta-release.apk"
ANDROID_APK_PATHS[prod]="${ANDROID_BUILD_DIR}/outputs/apk/prod/release/app-prod-release.apk"

# ============================================================================
# Quality Gates Configuration
# ============================================================================

# Test Requirements
REQUIRE_TESTS="${REQUIRE_TESTS:-true}"
REQUIRE_LINT="${REQUIRE_LINT:-false}"
REQUIRE_SONAR="${REQUIRE_SONAR:-false}"

# Git Requirements
ALLOW_UNCOMMITTED="${ALLOW_UNCOMMITTED:-false}"
REQUIRE_CLEAN_BRANCH="${REQUIRE_CLEAN_BRANCH:-false}"
AUTO_COMMIT="${AUTO_COMMIT:-true}"

# Security Requirements
REQUIRE_SECURITY_SCAN="${REQUIRE_SECURITY_SCAN:-false}"
BLOCK_ON_HIGH_SEVERITY="${BLOCK_ON_HIGH_SEVERITY:-true}"

# ============================================================================
# Helper Functions
# ============================================================================

# Get the deployment tier based on environment or git branch
get_deployment_tier() {
    # Check explicit environment variable
    if [[ -n "${DEPLOY_TIER:-}" ]]; then
        echo "$DEPLOY_TIER"
        return
    fi

    # Check CI environment
    if [[ "${CI:-false}" == "true" ]]; then
        if [[ "${GITHUB_REF:-}" == "refs/heads/main" ]]; then
            echo "prod"
        elif [[ "${GITHUB_REF:-}" == "refs/heads/staging" ]]; then
            echo "stage"
        elif [[ "${GITHUB_REF:-}" == "refs/heads/beta" ]]; then
            echo "beta"
        else
            echo "qual"
        fi
        return
    fi

    # Check git branch
    local branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    case "$branch" in
        main|master)
            echo "prod"
            ;;
        staging|stage)
            echo "stage"
            ;;
        beta)
            echo "beta"
            ;;
        *)
            echo "qual"
            ;;
    esac
}

# Get iOS scheme for a tier
get_ios_scheme() {
    local tier="${1:-$(get_deployment_tier)}"
    echo "${TIER_CONFIG[${tier}_ios_scheme]}"
}

# Get iOS bundle ID for a tier
get_ios_bundle_id() {
    local tier="${1:-$(get_deployment_tier)}"
    echo "${IOS_BUNDLE_IDS[$tier]}"
}

# Get Android variant for a tier
get_android_variant() {
    local tier="${1:-$(get_deployment_tier)}"
    echo "${ANDROID_BUILD_VARIANTS[$tier]}"
}

# Get Android package name for a tier
get_android_package() {
    local tier="${1:-$(get_deployment_tier)}"
    echo "${ANDROID_PACKAGE_NAMES[$tier]}"
}

# Check if tier requires code signing
requires_signing() {
    local tier="${1:-$(get_deployment_tier)}"
    [[ "${TIER_CONFIG[${tier}_requires_signing]}" == "true" ]]
}

# Validate deployment prerequisites
validate_deployment() {
    local tier="${1:-$(get_deployment_tier)}"
    local errors=0

    # Check uncommitted changes
    if [[ "$ALLOW_UNCOMMITTED" != "true" ]]; then
        if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain 2>/dev/null)" ]]; then
            echo "ERROR: Uncommitted changes detected. Use ALLOW_UNCOMMITTED=true to override." >&2
            ((errors++))
        fi
    fi

    # Check branch for production
    if [[ "$tier" == "prod" ]] && [[ "$REQUIRE_CLEAN_BRANCH" == "true" ]]; then
        local current_branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
        local expected_branch="${TIER_CONFIG[${tier}_branch]}"
        if [[ "$current_branch" != "$expected_branch" ]]; then
            echo "ERROR: Production deployment requires branch: $expected_branch (current: $current_branch)" >&2
            ((errors++))
        fi
    fi

    # Check signing requirements
    if requires_signing "$tier"; then
        # iOS signing check
        if [[ ! -f "${PROJECT_ROOT}/ios/ExportOptions.plist" ]]; then
            echo "WARNING: iOS ExportOptions.plist not found for tier: $tier" >&2
        fi

        # Android signing check
        if [[ ! -f "${PROJECT_ROOT}/android/keystore.properties" ]]; then
            echo "WARNING: Android keystore.properties not found for tier: $tier" >&2
        fi
    fi

    return $errors
}

# Build iOS for a specific tier
build_ios() {
    local tier="${1:-qual}"
    local scheme="${TIER_CONFIG[${tier}_ios_scheme]}"
    local configuration="${TIER_CONFIG[${tier}_ios_configuration]}"
    local bundle_id="${IOS_BUNDLE_IDS[$tier]}"

    echo "Building iOS for tier: $tier"
    echo "  Scheme: $scheme"
    echo "  Configuration: $configuration"
    echo "  Bundle ID: $bundle_id"

    xcodebuild \
        -project "$IOS_PROJECT" \
        -scheme "$scheme" \
        -configuration "$configuration" \
        -sdk "$IOS_SIMULATOR_SDK" \
        -derivedDataPath "$IOS_BUILD_DIR" \
        -destination "generic/platform=iOS Simulator" \
        clean build
}

# Build Android for a specific tier
build_android() {
    local tier="${1:-qual}"
    local variant="${ANDROID_BUILD_VARIANTS[$tier]}"
    local package="${ANDROID_PACKAGE_NAMES[$tier]}"

    echo "Building Android for tier: $tier"
    echo "  Variant: $variant"
    echo "  Package: $package"

    cd "$ANDROID_PROJECT"
    ./gradlew "assemble${variant^}"  # Capitalize first letter
}

# Run tests for a specific tier
run_tests() {
    local tier="${1:-qual}"

    if [[ "$REQUIRE_TESTS" != "true" ]]; then
        echo "Tests skipped by configuration"
        return 0
    fi

    echo "Running tests for tier: $tier"

    # Run tier-specific tests
    case "$tier" in
        qual)
            # Run all test tiers for QUAL
            "$TEST_COMMAND" both
            ;;
        stage|beta)
            # Run critical and important tests for STAGE/BETA
            echo "Running Tier 1 and Tier 2 tests only..."
            SKIP_TIER3=true "$TEST_COMMAND" both
            ;;
        prod)
            # Run only critical tests for PROD (should already be tested)
            echo "Running Tier 1 critical tests only..."
            SKIP_TIER2=true SKIP_TIER3=true "$TEST_COMMAND" both
            ;;
    esac
}

# Deploy to a specific tier
deploy_tier() {
    local tier="${1:-qual}"
    local platform="${2:-both}"

    echo "=========================================="
    echo "Deploying to tier: $tier"
    echo "Platform: $platform"
    echo "=========================================="

    # Validate prerequisites
    if ! validate_deployment "$tier"; then
        echo "ERROR: Deployment validation failed" >&2
        return 1
    fi

    # Update version
    local new_version=$(bump_build_number)
    echo "Version: $new_version"

    # Run tests
    run_tests "$tier"

    # Build and deploy
    case "$platform" in
        ios)
            build_ios "$tier"
            ;;
        android)
            build_android "$tier"
            ;;
        both)
            build_ios "$tier"
            build_android "$tier"
            ;;
    esac

    echo "Deployment to $tier completed successfully!"
}

# ============================================================================
# Export Configuration
# ============================================================================

# Export all configuration for use by other scripts
export PROJECT_NAME PROJECT_TYPE PROJECT_ROOT
export BUILD_NUMBER_FILE VERSION_FORMAT
export IOS_PROJECT IOS_BUILD_DIR
export ANDROID_PROJECT ANDROID_BUILD_DIR
export DEPLOYMENT_TIERS
export REQUIRE_TESTS REQUIRE_LINT ALLOW_UNCOMMITTED

# Export functions
export -f get_current_build_number
export -f bump_build_number
export -f get_deployment_tier
export -f get_ios_scheme
export -f get_ios_bundle_id
export -f get_android_variant
export -f get_android_package
export -f requires_signing
export -f validate_deployment
export -f build_ios
export -f build_android
export -f run_tests
export -f deploy_tier

# ============================================================================
# Script Entry Point (for testing)
# ============================================================================

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Script is being executed directly (not sourced)
    echo "SmilePile Deployment Configuration Loaded"
    echo "=========================================="
    echo "Project: $PROJECT_NAME"
    echo "Type: $PROJECT_TYPE"
    echo "Current Version: $(get_current_build_number)"
    echo "Current Tier: $(get_deployment_tier)"
    echo "=========================================="
    echo ""
    echo "Usage: source this file to load configuration"
    echo "  source $0"
    echo ""
    echo "Available functions:"
    echo "  get_deployment_tier     - Get current deployment tier"
    echo "  bump_build_number       - Generate new build number"
    echo "  build_ios [tier]        - Build iOS for a tier"
    echo "  build_android [tier]    - Build Android for a tier"
    echo "  run_tests [tier]        - Run tests for a tier"
    echo "  deploy_tier [tier]      - Full deployment to a tier"
fi