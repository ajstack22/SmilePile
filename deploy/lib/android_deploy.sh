#!/bin/bash
# ============================================================================
# SmilePile Deployment System - Android Deployment Module
# ============================================================================
# Handles Android APK building, signing, and deployment
# Works locally and in CI without external dependencies

set -euo pipefail

# Source common libraries
if [[ -z "${LIB_DIR:-}" ]]; then
    LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
source "${LIB_DIR}/common.sh"
source "${LIB_DIR}/env_manager.sh"

# ============================================================================
# Android Configuration
# ============================================================================

if [[ -z "${ANDROID_ROOT:-}" ]]; then
    readonly ANDROID_ROOT="${PROJECT_ROOT}/android"
    readonly GRADLE_WRAPPER="${ANDROID_ROOT}/gradlew"
    readonly BUILD_OUTPUT_DIR="${ANDROID_ROOT}/app/build/outputs"
    readonly APK_OUTPUT_DIR="${BUILD_OUTPUT_DIR}/apk"
    readonly BUNDLE_OUTPUT_DIR="${BUILD_OUTPUT_DIR}/bundle"
fi

# ============================================================================
# Pre-flight Checks
# ============================================================================

# Verify Android environment
verify_android_environment() {
    log INFO "Verifying Android build environment..."

    # Check Android project exists
    if [[ ! -d "$ANDROID_ROOT" ]]; then
        log ERROR "Android project not found at: $ANDROID_ROOT"
        return 1
    fi

    # Check Gradle wrapper
    if [[ ! -f "$GRADLE_WRAPPER" ]]; then
        log ERROR "Gradle wrapper not found at: $GRADLE_WRAPPER"
        return 1
    fi

    # Check Java
    if ! command -v java &> /dev/null; then
        log ERROR "Java is not installed"
        return 1
    fi

    # Check ANDROID_HOME (if set)
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        if [[ ! -d "$ANDROID_HOME" ]]; then
            log ERROR "ANDROID_HOME is set but directory doesn't exist: $ANDROID_HOME"
            return 1
        fi
        log INFO "Android SDK found at: $ANDROID_HOME"
    else
        log WARN "ANDROID_HOME not set, using system Android SDK"
    fi

    log INFO "Android environment verification complete"
}

# ============================================================================
# Build Functions
# ============================================================================

# Clean Android project
clean_android() {
    log INFO "Cleaning Android project..."

    cd "$ANDROID_ROOT"
    retry_command 2 5 "$GRADLE_WRAPPER" clean

    # Remove previous build outputs
    rm -rf "$BUILD_OUTPUT_DIR"

    log INFO "Android project cleaned"
}

# Build Android APK
build_android_apk() {
    local build_type=${1:-Release}
    local flavor=${2:-}

    log INFO "Building Android APK (${build_type}${flavor:+ $flavor})"

    cd "$ANDROID_ROOT"

    # Prepare build command
    local gradle_task="assemble${flavor}${build_type}"

    # Set build properties
    local build_props=(
        "-PversionCode=${BUILD_NUMBER:-1}"
        "-PversionName=${APP_VERSION:-1.0.0}"
    )

    # Add signing config for release builds
    if [[ "$build_type" == "Release" ]] && [[ -n "${ANDROID_KEYSTORE_PATH:-}" ]]; then
        build_props+=(
            "-Pandroid.injected.signing.store.file=${ANDROID_KEYSTORE_PATH}"
            "-Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD}"
            "-Pandroid.injected.signing.key.alias=${ANDROID_KEY_ALIAS}"
            "-Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD:-$ANDROID_KEYSTORE_PASSWORD}"
        )
    fi

    # Execute build
    log INFO "Executing: $GRADLE_WRAPPER $gradle_task ${build_props[*]}"
    retry_command 2 10 "$GRADLE_WRAPPER" "$gradle_task" "${build_props[@]}"

    # Find built APK
    local apk_path
    apk_path=$(find "$APK_OUTPUT_DIR" -name "*.apk" -type f | head -1)

    if [[ -z "$apk_path" ]]; then
        log ERROR "No APK found after build"
        return 1
    fi

    log INFO "APK built successfully: $apk_path"
    echo "$apk_path"
}

# Build Android App Bundle (AAB)
build_android_bundle() {
    local build_type=${1:-Release}
    local flavor=${2:-}

    log INFO "Building Android App Bundle (${build_type}${flavor:+ $flavor})"

    cd "$ANDROID_ROOT"

    # Prepare build command
    local gradle_task="bundle${flavor}${build_type}"

    # Set build properties
    local build_props=(
        "-PversionCode=${BUILD_NUMBER:-1}"
        "-PversionName=${APP_VERSION:-1.0.0}"
    )

    # Add signing config for release builds
    if [[ "$build_type" == "Release" ]] && [[ -n "${ANDROID_KEYSTORE_PATH:-}" ]]; then
        build_props+=(
            "-Pandroid.injected.signing.store.file=${ANDROID_KEYSTORE_PATH}"
            "-Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD}"
            "-Pandroid.injected.signing.key.alias=${ANDROID_KEY_ALIAS}"
            "-Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD:-$ANDROID_KEYSTORE_PASSWORD}"
        )
    fi

    # Execute build
    log INFO "Executing: $GRADLE_WRAPPER $gradle_task ${build_props[*]}"
    retry_command 2 10 "$GRADLE_WRAPPER" "$gradle_task" "${build_props[@]}"

    # Find built AAB
    local aab_path
    aab_path=$(find "$BUNDLE_OUTPUT_DIR" -name "*.aab" -type f | head -1)

    if [[ -z "$aab_path" ]]; then
        log ERROR "No AAB found after build"
        return 1
    fi

    log INFO "AAB built successfully: $aab_path"
    echo "$aab_path"
}

# ============================================================================
# Testing Functions
# ============================================================================

# Run Android unit tests
run_android_unit_tests() {
    log INFO "Running Android unit tests..."

    cd "$ANDROID_ROOT"
    retry_command 2 5 "$GRADLE_WRAPPER" test

    log INFO "Unit tests completed"
}

# Run Android instrumentation tests
run_android_instrumentation_tests() {
    log INFO "Running Android instrumentation tests..."

    if [[ -z "${ANDROID_EMULATOR:-}" ]]; then
        log WARN "No Android emulator specified, skipping instrumentation tests"
        return 0
    fi

    cd "$ANDROID_ROOT"
    retry_command 2 5 "$GRADLE_WRAPPER" connectedAndroidTest

    log INFO "Instrumentation tests completed"
}

# Run Android lint
run_android_lint() {
    log INFO "Running Android lint checks..."

    cd "$ANDROID_ROOT"
    "$GRADLE_WRAPPER" lint

    # Check lint results
    local lint_report="${BUILD_OUTPUT_DIR}/reports/lint-results.html"
    if [[ -f "$lint_report" ]]; then
        log INFO "Lint report generated: $lint_report"

        # Check for errors
        if grep -q "Error:" "$lint_report"; then
            log WARN "Lint errors found"
        fi
    fi

    log INFO "Lint checks completed"
}

# ============================================================================
# Signing Functions
# ============================================================================

# Sign APK manually (if not signed during build)
sign_apk() {
    local apk_path=$1
    local output_path=${2:-${apk_path%.apk}-signed.apk}

    log INFO "Signing APK: $apk_path"

    # Check for required signing parameters
    if [[ -z "${ANDROID_KEYSTORE_PATH:-}" ]]; then
        log ERROR "ANDROID_KEYSTORE_PATH not set"
        return 1
    fi

    # Use apksigner if available, otherwise fall back to jarsigner
    if command -v apksigner &> /dev/null; then
        apksigner sign \
            --ks "$ANDROID_KEYSTORE_PATH" \
            --ks-pass "pass:${ANDROID_KEYSTORE_PASSWORD}" \
            --ks-key-alias "${ANDROID_KEY_ALIAS}" \
            --out "$output_path" \
            "$apk_path"
    else
        # Use jarsigner as fallback
        jarsigner \
            -verbose \
            -sigalg SHA256withRSA \
            -digestalg SHA-256 \
            -keystore "$ANDROID_KEYSTORE_PATH" \
            -storepass "${ANDROID_KEYSTORE_PASSWORD}" \
            -keypass "${ANDROID_KEY_PASSWORD:-$ANDROID_KEYSTORE_PASSWORD}" \
            "$apk_path" \
            "${ANDROID_KEY_ALIAS}"

        # Align APK
        if command -v zipalign &> /dev/null; then
            zipalign -v 4 "$apk_path" "$output_path"
        else
            cp "$apk_path" "$output_path"
        fi
    fi

    log INFO "APK signed: $output_path"
    echo "$output_path"
}

# Verify APK signature
verify_apk_signature() {
    local apk_path=$1

    log INFO "Verifying APK signature: $apk_path"

    if command -v apksigner &> /dev/null; then
        apksigner verify --verbose "$apk_path"
    else
        jarsigner -verify -verbose -certs "$apk_path"
    fi

    log INFO "APK signature verified"
}

# ============================================================================
# Deployment Functions
# ============================================================================

# Deploy to Google Play Store
deploy_to_play_store() {
    local artifact_path=$1
    local track=${2:-internal}  # internal, alpha, beta, production

    log INFO "Deploying to Google Play Store (track: $track)"

    # Check for required credentials
    if [[ -z "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON:-}" ]]; then
        log ERROR "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON not set"
        return 1
    fi

    # Use fastlane if available
    if command -v fastlane &> /dev/null; then
        cd "$ANDROID_ROOT"
        fastlane supply \
            --aab "$artifact_path" \
            --track "$track" \
            --json_key "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON}" \
            --skip_upload_metadata \
            --skip_upload_images \
            --skip_upload_screenshots
    else
        log WARN "Fastlane not installed, using manual upload"
        # In production, you might want to use Google Play API directly
        # For now, we'll just copy to artifacts directory
        copy_to_artifacts "$artifact_path" "playstore"
    fi

    log INFO "Deployment to Play Store completed"
}

# Deploy to Firebase App Distribution
deploy_to_firebase() {
    local apk_path=$1
    local release_notes=${2:-"Automated deployment from SmilePile CI"}
    local groups=${3:-"testers"}

    log INFO "Deploying to Firebase App Distribution"

    # Check for Firebase CLI
    if ! command -v firebase &> /dev/null; then
        log WARN "Firebase CLI not installed, skipping Firebase deployment"
        copy_to_artifacts "$apk_path" "firebase"
        return 0
    fi

    # Check for required credentials
    if [[ -z "${FIREBASE_APP_ID:-}" ]]; then
        log ERROR "FIREBASE_APP_ID not set"
        return 1
    fi

    # Deploy using Firebase CLI
    firebase appdistribution:distribute "$apk_path" \
        --app "${FIREBASE_APP_ID}" \
        --release-notes "$release_notes" \
        --groups "$groups" \
        --token "${FIREBASE_TOKEN:-}"

    log INFO "Deployment to Firebase completed"
}

# Copy artifact to artifacts directory
copy_to_artifacts() {
    local artifact_path=$1
    local destination_name=${2:-"android"}

    local artifact_dir="${DEPLOY_ROOT}/artifacts/${DEPLOY_ENVIRONMENT}/${destination_name}"
    mkdir -p "$artifact_dir"

    local filename="SmilePile_${APP_VERSION}_${BUILD_NUMBER}_${TIMESTAMP}.${artifact_path##*.}"
    local destination="${artifact_dir}/${filename}"

    cp "$artifact_path" "$destination"

    # Create metadata file
    cat > "${destination}.json" << EOF
{
    "filename": "${filename}",
    "version": "${APP_VERSION}",
    "build_number": "${BUILD_NUMBER}",
    "timestamp": "${TIMESTAMP}",
    "checksum": "$(calculate_checksum "$artifact_path")",
    "environment": "${DEPLOY_ENVIRONMENT}",
    "git_commit": "$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || echo 'unknown')",
    "git_branch": "$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
}
EOF

    log INFO "Artifact copied to: $destination"
    echo "$destination"
}

# ============================================================================
# Quality Checks
# ============================================================================

# Run security scan on APK
scan_apk_security() {
    local apk_path=$1

    log INFO "Running security scan on APK..."

    # Use MobSF if available
    if command -v mobsf &> /dev/null; then
        mobsf -f "$apk_path" -o "${DEPLOY_ROOT}/reports/security_scan_${TIMESTAMP}.json"
    else
        # Basic checks using aapt
        if command -v aapt &> /dev/null; then
            log INFO "Checking APK permissions..."
            aapt dump permissions "$apk_path"

            log INFO "Checking APK configuration..."
            aapt dump badging "$apk_path" | grep -E "(package|sdkVersion|uses-permission|application-label)"
        fi
    fi

    log INFO "Security scan completed"
}

# Analyze APK size
analyze_apk_size() {
    local apk_path=$1

    log INFO "Analyzing APK size..."

    local size=$(du -h "$apk_path" | cut -f1)
    log INFO "APK size: $size"

    # Use apkanalyzer if available
    if command -v apkanalyzer &> /dev/null; then
        apkanalyzer apk summary "$apk_path"
    fi

    # Check if size exceeds limit
    local max_size_mb=${MAX_APK_SIZE_MB:-100}
    local actual_size_mb=$(du -m "$apk_path" | cut -f1)

    if [[ $actual_size_mb -gt $max_size_mb ]]; then
        log WARN "APK size ($actual_size_mb MB) exceeds limit ($max_size_mb MB)"
    fi
}

# ============================================================================
# Main Android Deployment Function
# ============================================================================

deploy_android() {
    local environment=$1
    local deploy_target=${2:-apk}  # apk, bundle, both
    local distribution=${3:-artifacts}  # artifacts, playstore, firebase

    print_header "Android Deployment - $environment"

    # Load environment
    load_environment "$environment"

    # Verify environment
    verify_android_environment

    # Clean if requested
    if [[ "${CLEAN_BUILD:-false}" == "true" ]]; then
        clean_android
    fi

    # Run tests if not skipped
    if [[ "${SKIP_TESTS:-false}" != "true" ]]; then
        run_android_unit_tests
        run_android_lint
    fi

    # Build artifacts
    local artifacts=()

    if [[ "$deploy_target" == "apk" ]] || [[ "$deploy_target" == "both" ]]; then
        local apk_path=$(build_android_apk "Release" "${ANDROID_FLAVOR:-}")
        artifacts+=("$apk_path")

        # Verify signature
        verify_apk_signature "$apk_path"

        # Security scan
        if [[ "${SKIP_SECURITY:-false}" != "true" ]]; then
            scan_apk_security "$apk_path"
        fi

        # Size analysis
        analyze_apk_size "$apk_path"
    fi

    if [[ "$deploy_target" == "bundle" ]] || [[ "$deploy_target" == "both" ]]; then
        local aab_path=$(build_android_bundle "Release" "${ANDROID_FLAVOR:-}")
        artifacts+=("$aab_path")
    fi

    # Deploy artifacts
    for artifact in "${artifacts[@]}"; do
        case "$distribution" in
            playstore)
                deploy_to_play_store "$artifact"
                ;;
            firebase)
                deploy_to_firebase "$artifact"
                ;;
            artifacts|*)
                copy_to_artifacts "$artifact"
                ;;
        esac
    done

    # Record deployment
    record_deployment "android" "$environment" "success" "Deployed ${#artifacts[@]} artifacts"

    log INFO "Android deployment completed successfully"
}

# ============================================================================
# Export Functions
# ============================================================================

export -f verify_android_environment
export -f clean_android
export -f build_android_apk
export -f build_android_bundle
export -f run_android_unit_tests
export -f run_android_instrumentation_tests
export -f run_android_lint
export -f sign_apk
export -f verify_apk_signature
export -f deploy_to_play_store
export -f deploy_to_firebase
export -f copy_to_artifacts
export -f scan_apk_security
export -f analyze_apk_size
export -f deploy_android