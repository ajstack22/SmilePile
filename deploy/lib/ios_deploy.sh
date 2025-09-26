#!/bin/bash
# ============================================================================
# SmilePile Deployment System - iOS Deployment Module
# ============================================================================
# Handles iOS IPA building, signing, and deployment
# Works locally and in CI without external dependencies

set -euo pipefail

# Source common libraries
if [[ -z "${LIB_DIR:-}" ]]; then
    LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
source "${LIB_DIR}/common.sh"
source "${LIB_DIR}/env_manager.sh"

# ============================================================================
# iOS Configuration
# ============================================================================

if [[ -z "${IOS_ROOT:-}" ]]; then
    readonly IOS_ROOT="${PROJECT_ROOT}/ios"
    readonly XCODE_PROJECT="${IOS_ROOT}/SmilePile.xcodeproj"
    readonly XCODE_WORKSPACE="${IOS_ROOT}/SmilePile.xcworkspace"
    readonly BUILD_DIR="${IOS_ROOT}/build"
    readonly DERIVED_DATA_PATH="${IOS_ROOT}/DerivedData"
    readonly ARCHIVE_PATH="${BUILD_DIR}/SmilePile.xcarchive"
fi

# ============================================================================
# Pre-flight Checks
# ============================================================================

# Verify iOS environment
verify_ios_environment() {
    log INFO "Verifying iOS build environment..."

    # Check if running on macOS
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log ERROR "iOS builds require macOS"
        return 1
    fi

    # Check Xcode installation
    if ! command -v xcodebuild &> /dev/null; then
        log ERROR "Xcode is not installed"
        return 1
    fi

    # Check Xcode version
    local xcode_version=$(xcodebuild -version | head -1 | awk '{print $2}')
    log INFO "Xcode version: $xcode_version"

    # Check iOS project exists
    if [[ ! -d "$IOS_ROOT" ]]; then
        log ERROR "iOS project not found at: $IOS_ROOT"
        return 1
    fi

    # Check project or workspace
    local project_file=""
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        project_file="$XCODE_WORKSPACE"
        log INFO "Using workspace: $project_file"
    elif [[ -d "$XCODE_PROJECT" ]]; then
        project_file="$XCODE_PROJECT"
        log INFO "Using project: $project_file"
    else
        log ERROR "No Xcode project or workspace found"
        return 1
    fi

    # Check for CocoaPods if Podfile exists
    if [[ -f "${IOS_ROOT}/Podfile" ]]; then
        if ! command -v pod &> /dev/null; then
            log ERROR "CocoaPods is required but not installed"
            return 1
        fi
    fi

    log INFO "iOS environment verification complete"
}

# ============================================================================
# Build Functions
# ============================================================================

# Clean iOS project
clean_ios() {
    log INFO "Cleaning iOS project..."

    cd "$IOS_ROOT"

    # Clean build directory
    rm -rf "$BUILD_DIR"
    rm -rf "$DERIVED_DATA_PATH"

    # Clean using xcodebuild
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        xcodebuild clean \
            -workspace "$(basename "$XCODE_WORKSPACE")" \
            -scheme "${IOS_SCHEME:-SmilePile}" \
            -configuration "${IOS_BUILD_CONFIGURATION:-Release}"
    else
        xcodebuild clean \
            -project "$(basename "$XCODE_PROJECT")" \
            -scheme "${IOS_SCHEME:-SmilePile}" \
            -configuration "${IOS_BUILD_CONFIGURATION:-Release}"
    fi

    log INFO "iOS project cleaned"
}

# Install iOS dependencies
install_ios_dependencies() {
    log INFO "Installing iOS dependencies..."

    cd "$IOS_ROOT"

    # Install CocoaPods if needed
    if [[ -f "Podfile" ]]; then
        log INFO "Installing CocoaPods dependencies..."
        pod install --repo-update
    fi

    # Install Swift Package Manager dependencies (handled by xcodebuild)

    log INFO "Dependencies installed"
}

# Build iOS archive
build_ios_archive() {
    local scheme=${1:-${IOS_SCHEME:-SmilePile}}
    local configuration=${2:-${IOS_BUILD_CONFIGURATION:-Release}}

    log INFO "Building iOS archive (scheme: $scheme, configuration: $configuration)"

    cd "$IOS_ROOT"

    # Create build directory
    mkdir -p "$BUILD_DIR"

    # Prepare build settings
    local build_settings=(
        "MARKETING_VERSION=${APP_VERSION:-1.0.0}"
        "CURRENT_PROJECT_VERSION=${BUILD_NUMBER:-1}"
        "DEVELOPMENT_TEAM=${IOS_DEVELOPMENT_TEAM:-}"
        "CODE_SIGN_STYLE=${IOS_CODE_SIGN_STYLE:-Automatic}"
    )

    # Add provisioning profile if specified
    if [[ -n "${IOS_PROVISIONING_PROFILE:-}" ]]; then
        build_settings+=(
            "PROVISIONING_PROFILE_SPECIFIER=${IOS_PROVISIONING_PROFILE}"
        )
    fi

    # Build command
    local xcodebuild_cmd=(
        xcodebuild
        archive
        -scheme "$scheme"
        -configuration "$configuration"
        -archivePath "$ARCHIVE_PATH"
        -derivedDataPath "$DERIVED_DATA_PATH"
        -allowProvisioningUpdates
    )

    # Add workspace or project
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        xcodebuild_cmd+=(-workspace "$(basename "$XCODE_WORKSPACE")")
    else
        xcodebuild_cmd+=(-project "$(basename "$XCODE_PROJECT")")
    fi

    # Add build settings
    for setting in "${build_settings[@]}"; do
        xcodebuild_cmd+=("$setting")
    done

    # Execute build
    log INFO "Executing: ${xcodebuild_cmd[*]}"
    retry_command 2 10 "${xcodebuild_cmd[@]}"

    if [[ ! -d "$ARCHIVE_PATH" ]]; then
        log ERROR "Archive not created"
        return 1
    fi

    log INFO "Archive created successfully: $ARCHIVE_PATH"
}

# Export iOS IPA
export_ios_ipa() {
    local export_method=${1:-${IOS_EXPORT_METHOD:-app-store}}  # app-store, ad-hoc, enterprise, development

    log INFO "Exporting IPA (method: $export_method)"

    if [[ ! -d "$ARCHIVE_PATH" ]]; then
        log ERROR "No archive found at: $ARCHIVE_PATH"
        return 1
    fi

    # Create export options plist
    local export_options="${BUILD_DIR}/ExportOptions.plist"
    create_export_options_plist "$export_method" "$export_options"

    # Export IPA
    local export_path="${BUILD_DIR}/Export"
    xcodebuild -exportArchive \
        -archivePath "$ARCHIVE_PATH" \
        -exportPath "$export_path" \
        -exportOptionsPlist "$export_options"

    # Find exported IPA
    local ipa_path
    ipa_path=$(find "$export_path" -name "*.ipa" -type f | head -1)

    if [[ -z "$ipa_path" ]]; then
        log ERROR "No IPA found after export"
        return 1
    fi

    log INFO "IPA exported successfully: $ipa_path"
    echo "$ipa_path"
}

# Create export options plist
create_export_options_plist() {
    local method=$1
    local output_file=$2

    cat > "$output_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>$method</string>
    <key>teamID</key>
    <string>${IOS_DEVELOPMENT_TEAM:-}</string>
    <key>uploadBitcode</key>
    <${ENABLE_BITCODE:-false}/>
    <key>compileBitcode</key>
    <${ENABLE_BITCODE:-false}/>
    <key>uploadSymbols</key>
    <true/>
    <key>signingStyle</key>
    <string>${IOS_CODE_SIGN_STYLE:-automatic}</string>
EOF

    # Add provisioning profiles if manual signing
    if [[ "${IOS_CODE_SIGN_STYLE:-automatic}" == "manual" ]]; then
        cat >> "$output_file" << EOF
    <key>provisioningProfiles</key>
    <dict>
        <key>${IOS_BUNDLE_ID}</key>
        <string>${IOS_PROVISIONING_PROFILE}</string>
    </dict>
EOF
    fi

    cat >> "$output_file" << EOF
</dict>
</plist>
EOF
}

# ============================================================================
# Testing Functions
# ============================================================================

# Run iOS unit tests
run_ios_unit_tests() {
    local scheme=${1:-${IOS_SCHEME:-SmilePile}}

    log INFO "Running iOS unit tests..."

    cd "$IOS_ROOT"

    local xcodebuild_cmd=(
        xcodebuild
        test
        -scheme "$scheme"
        -destination "platform=iOS Simulator,name=${IOS_SIMULATOR:-iPhone 14}"
        -derivedDataPath "$DERIVED_DATA_PATH"
    )

    # Add workspace or project
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        xcodebuild_cmd+=(-workspace "$(basename "$XCODE_WORKSPACE")")
    else
        xcodebuild_cmd+=(-project "$(basename "$XCODE_PROJECT")")
    fi

    retry_command 2 5 "${xcodebuild_cmd[@]}"

    log INFO "Unit tests completed"
}

# Run iOS UI tests
run_ios_ui_tests() {
    local scheme=${1:-${IOS_SCHEME:-SmilePile}UITests}

    log INFO "Running iOS UI tests..."

    if [[ "${SKIP_UI_TESTS:-true}" == "true" ]]; then
        log INFO "UI tests skipped"
        return 0
    fi

    cd "$IOS_ROOT"

    local xcodebuild_cmd=(
        xcodebuild
        test
        -scheme "$scheme"
        -destination "platform=iOS Simulator,name=${IOS_SIMULATOR:-iPhone 14}"
        -derivedDataPath "$DERIVED_DATA_PATH"
    )

    # Add workspace or project
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        xcodebuild_cmd+=(-workspace "$(basename "$XCODE_WORKSPACE")")
    else
        xcodebuild_cmd+=(-project "$(basename "$XCODE_PROJECT")")
    fi

    retry_command 2 5 "${xcodebuild_cmd[@]}"

    log INFO "UI tests completed"
}

# Analyze iOS project
run_ios_analyze() {
    log INFO "Analyzing iOS project..."

    cd "$IOS_ROOT"

    local xcodebuild_cmd=(
        xcodebuild
        analyze
        -scheme "${IOS_SCHEME:-SmilePile}"
        -configuration "${IOS_BUILD_CONFIGURATION:-Release}"
        -derivedDataPath "$DERIVED_DATA_PATH"
    )

    # Add workspace or project
    if [[ -f "$XCODE_WORKSPACE/contents.xcworkspacedata" ]]; then
        xcodebuild_cmd+=(-workspace "$(basename "$XCODE_WORKSPACE")")
    else
        xcodebuild_cmd+=(-project "$(basename "$XCODE_PROJECT")")
    fi

    "${xcodebuild_cmd[@]}" | tee "${BUILD_DIR}/analyze.log"

    # Check for issues
    if grep -q "error:" "${BUILD_DIR}/analyze.log"; then
        log ERROR "Analysis found errors"
        return 1
    fi

    if grep -q "warning:" "${BUILD_DIR}/analyze.log"; then
        log WARN "Analysis found warnings"
    fi

    log INFO "Analysis completed"
}

# ============================================================================
# Deployment Functions
# ============================================================================

# Deploy to App Store Connect
deploy_to_app_store() {
    local ipa_path=$1
    local validate_only=${2:-false}

    log INFO "Deploying to App Store Connect"

    # Check for required credentials
    if [[ -z "${APP_STORE_CONNECT_API_KEY:-}" ]] && [[ -z "${APP_STORE_USERNAME:-}" ]]; then
        log ERROR "App Store Connect credentials not configured"
        return 1
    fi

    # Use xcrun altool or Transporter
    if [[ -n "${APP_STORE_CONNECT_API_KEY:-}" ]]; then
        # Use API key authentication
        local auth_args=(
            --apiKey "${APP_STORE_CONNECT_API_KEY_ID}"
            --apiIssuer "${APP_STORE_CONNECT_API_ISSUER}"
        )

        # Store API key
        echo "${APP_STORE_CONNECT_API_KEY}" > "${BUILD_DIR}/AuthKey.p8"
        auth_args+=(--privateKey "@${BUILD_DIR}/AuthKey.p8")
    else
        # Use username/password authentication
        local auth_args=(
            -u "${APP_STORE_USERNAME}"
            -p "${APP_STORE_PASSWORD}"
        )
    fi

    if [[ "$validate_only" == "true" ]]; then
        # Validate only
        xcrun altool --validate-app \
            -f "$ipa_path" \
            -t ios \
            "${auth_args[@]}"
    else
        # Upload to App Store Connect
        xcrun altool --upload-app \
            -f "$ipa_path" \
            -t ios \
            "${auth_args[@]}"
    fi

    log INFO "Deployment to App Store Connect completed"
}

# Deploy to TestFlight
deploy_to_testflight() {
    local ipa_path=$1

    log INFO "Deploying to TestFlight"

    # First upload to App Store Connect
    deploy_to_app_store "$ipa_path" false

    # TestFlight distribution is automatic after upload
    log INFO "IPA uploaded to TestFlight"
}

# Deploy to Firebase App Distribution
deploy_ios_to_firebase() {
    local ipa_path=$1
    local release_notes=${2:-"Automated deployment from SmilePile CI"}
    local groups=${3:-"testers"}

    log INFO "Deploying to Firebase App Distribution"

    # Check for Firebase CLI
    if ! command -v firebase &> /dev/null; then
        log WARN "Firebase CLI not installed, skipping Firebase deployment"
        copy_ios_to_artifacts "$ipa_path" "firebase"
        return 0
    fi

    # Check for required credentials
    if [[ -z "${FIREBASE_APP_ID_IOS:-}" ]]; then
        log ERROR "FIREBASE_APP_ID_IOS not set"
        return 1
    fi

    # Deploy using Firebase CLI
    firebase appdistribution:distribute "$ipa_path" \
        --app "${FIREBASE_APP_ID_IOS}" \
        --release-notes "$release_notes" \
        --groups "$groups" \
        --token "${FIREBASE_TOKEN:-}"

    log INFO "Deployment to Firebase completed"
}

# Copy IPA to artifacts directory
copy_ios_to_artifacts() {
    local ipa_path=$1
    local destination_name=${2:-"ios"}

    local artifact_dir="${DEPLOY_ROOT}/artifacts/${DEPLOY_ENVIRONMENT}/${destination_name}"
    mkdir -p "$artifact_dir"

    local filename="SmilePile_${APP_VERSION}_${BUILD_NUMBER}_${TIMESTAMP}.ipa"
    local destination="${artifact_dir}/${filename}"

    cp "$ipa_path" "$destination"

    # Copy dSYM if available
    local dsym_path="${ARCHIVE_PATH}/dSYMs/SmilePile.app.dSYM"
    if [[ -d "$dsym_path" ]]; then
        local dsym_zip="${artifact_dir}/SmilePile_${APP_VERSION}_${BUILD_NUMBER}_${TIMESTAMP}.dSYM.zip"
        cd "$(dirname "$dsym_path")"
        zip -r "$dsym_zip" "$(basename "$dsym_path")"
        log INFO "dSYM archived: $dsym_zip"
    fi

    # Create metadata file
    cat > "${destination}.json" << EOF
{
    "filename": "${filename}",
    "version": "${APP_VERSION}",
    "build_number": "${BUILD_NUMBER}",
    "timestamp": "${TIMESTAMP}",
    "checksum": "$(calculate_checksum "$ipa_path")",
    "environment": "${DEPLOY_ENVIRONMENT}",
    "export_method": "${IOS_EXPORT_METHOD:-app-store}",
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

# Analyze IPA size
analyze_ipa_size() {
    local ipa_path=$1

    log INFO "Analyzing IPA size..."

    local size=$(du -h "$ipa_path" | cut -f1)
    log INFO "IPA size: $size"

    # Unzip and analyze contents
    local temp_dir="${BUILD_DIR}/ipa_analysis"
    rm -rf "$temp_dir"
    mkdir -p "$temp_dir"

    unzip -q "$ipa_path" -d "$temp_dir"

    # Analyze app size
    local app_dir=$(find "$temp_dir" -name "*.app" -type d | head -1)
    if [[ -n "$app_dir" ]]; then
        log INFO "App bundle contents:"
        du -sh "$app_dir"/* | sort -rh | head -10
    fi

    rm -rf "$temp_dir"

    # Check if size exceeds limit
    local max_size_mb=${MAX_IPA_SIZE_MB:-200}
    local actual_size_mb=$(du -m "$ipa_path" | cut -f1)

    if [[ $actual_size_mb -gt $max_size_mb ]]; then
        log WARN "IPA size ($actual_size_mb MB) exceeds limit ($max_size_mb MB)"
    fi
}

# Verify IPA structure
verify_ipa_structure() {
    local ipa_path=$1

    log INFO "Verifying IPA structure..."

    local temp_dir="${BUILD_DIR}/ipa_verify"
    rm -rf "$temp_dir"
    mkdir -p "$temp_dir"

    unzip -q "$ipa_path" -d "$temp_dir"

    # Check for required files
    local app_dir=$(find "$temp_dir" -name "*.app" -type d | head -1)

    if [[ -z "$app_dir" ]]; then
        log ERROR "No app bundle found in IPA"
        rm -rf "$temp_dir"
        return 1
    fi

    # Check Info.plist
    if [[ ! -f "$app_dir/Info.plist" ]]; then
        log ERROR "Info.plist not found"
        rm -rf "$temp_dir"
        return 1
    fi

    # Extract and verify bundle information
    local bundle_id=$(defaults read "$app_dir/Info.plist" CFBundleIdentifier 2>/dev/null || echo "")
    local bundle_version=$(defaults read "$app_dir/Info.plist" CFBundleShortVersionString 2>/dev/null || echo "")

    log INFO "Bundle ID: $bundle_id"
    log INFO "Bundle Version: $bundle_version"

    rm -rf "$temp_dir"

    log INFO "IPA structure verified"
}

# ============================================================================
# Main iOS Deployment Function
# ============================================================================

deploy_ios() {
    local environment=$1
    local export_method=${2:-app-store}  # app-store, ad-hoc, enterprise, development
    local distribution=${3:-artifacts}  # artifacts, appstore, testflight, firebase

    print_header "iOS Deployment - $environment"

    # Check if on macOS
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log ERROR "iOS deployment requires macOS"
        return 1
    fi

    # Load environment
    load_environment "$environment"

    # Set export method
    export IOS_EXPORT_METHOD="$export_method"

    # Verify environment
    verify_ios_environment

    # Install dependencies
    if [[ "${SKIP_DEPENDENCIES:-false}" != "true" ]]; then
        install_ios_dependencies
    fi

    # Clean if requested
    if [[ "${CLEAN_BUILD:-false}" == "true" ]]; then
        clean_ios
    fi

    # Run tests if not skipped
    if [[ "${SKIP_TESTS:-false}" != "true" ]]; then
        run_ios_unit_tests
        run_ios_analyze
    fi

    # Build archive
    build_ios_archive

    # Export IPA
    local ipa_path=$(export_ios_ipa "$export_method")

    # Verify IPA
    verify_ipa_structure "$ipa_path"
    analyze_ipa_size "$ipa_path"

    # Deploy
    case "$distribution" in
        appstore)
            deploy_to_app_store "$ipa_path"
            ;;
        testflight)
            deploy_to_testflight "$ipa_path"
            ;;
        firebase)
            deploy_ios_to_firebase "$ipa_path"
            ;;
        artifacts|*)
            copy_ios_to_artifacts "$ipa_path"
            ;;
    esac

    # Record deployment
    record_deployment "ios" "$environment" "success" "Deployed IPA via $distribution"

    log INFO "iOS deployment completed successfully"
}

# ============================================================================
# Export Functions
# ============================================================================

export -f verify_ios_environment
export -f clean_ios
export -f install_ios_dependencies
export -f build_ios_archive
export -f export_ios_ipa
export -f create_export_options_plist
export -f run_ios_unit_tests
export -f run_ios_ui_tests
export -f run_ios_analyze
export -f deploy_to_app_store
export -f deploy_to_testflight
export -f deploy_ios_to_firebase
export -f copy_ios_to_artifacts
export -f analyze_ipa_size
export -f verify_ipa_structure
export -f deploy_ios