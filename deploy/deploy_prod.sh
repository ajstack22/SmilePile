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

# Bump version
bump_version() {
    local platform=$1

    if [[ "$VERSION_BUMP" == "none" ]]; then
        log INFO "Version bump skipped"
        return 0
    fi

    print_header "Version Bump - $platform"

    case "$platform" in
        android)
            cd "$PROJECT_ROOT/android"
            local gradle_file="app/build.gradle"

            if [[ ! -f "$gradle_file" ]]; then
                log ERROR "Gradle file not found"
                return 1
            fi

            # Get current version
            local current_version=$(grep "versionName" "$gradle_file" | head -n1 | cut -d'"' -f2)
            local current_code=$(grep "versionCode" "$gradle_file" | head -n1 | awk '{print $2}')

            log INFO "Current Android version: $current_version (code: $current_code)"

            # Calculate new version
            local new_version
            if [[ "$VERSION_BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                new_version="$VERSION_BUMP"
            else
                IFS='.' read -ra VERSION_PARTS <<< "$current_version"
                local major="${VERSION_PARTS[0]}"
                local minor="${VERSION_PARTS[1]}"
                local patch="${VERSION_PARTS[2]}"

                case "$VERSION_BUMP" in
                    major)
                        ((major++))
                        minor=0
                        patch=0
                        ;;
                    minor)
                        ((minor++))
                        patch=0
                        ;;
                    patch)
                        ((patch++))
                        ;;
                esac

                new_version="${major}.${minor}.${patch}"
            fi

            local new_code=$((current_code + 1))

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would bump to version $new_version (code: $new_code)"
            else
                # Update gradle file
                sed -i '' "s/versionName \"$current_version\"/versionName \"$new_version\"/" "$gradle_file"
                sed -i '' "s/versionCode $current_code/versionCode $new_code/" "$gradle_file"

                log SUCCESS "Android version bumped to $new_version (code: $new_code)"
            fi
            ;;

        ios)
            cd "$PROJECT_ROOT/ios"

            # Get current version from Info.plist
            local current_version=$(/usr/libexec/PlistBuddy -c "Print CFBundleShortVersionString" SmilePile/Info.plist)
            local current_build=$(/usr/libexec/PlistBuddy -c "Print CFBundleVersion" SmilePile/Info.plist)

            log INFO "Current iOS version: $current_version (build: $current_build)"

            # Calculate new version
            local new_version
            if [[ "$VERSION_BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                new_version="$VERSION_BUMP"
            else
                IFS='.' read -ra VERSION_PARTS <<< "$current_version"
                local major="${VERSION_PARTS[0]}"
                local minor="${VERSION_PARTS[1]}"
                local patch="${VERSION_PARTS[2]}"

                case "$VERSION_BUMP" in
                    major)
                        ((major++))
                        minor=0
                        patch=0
                        ;;
                    minor)
                        ((minor++))
                        patch=0
                        ;;
                    patch)
                        ((patch++))
                        ;;
                esac

                new_version="${major}.${minor}.${patch}"
            fi

            local new_build=$((current_build + 1))

            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would bump to version $new_version (build: $new_build)"
            else
                # Update Info.plist
                /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $new_version" SmilePile/Info.plist
                /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $new_build" SmilePile/Info.plist

                log SUCCESS "iOS version bumped to $new_version (build: $new_build)"
            fi
            ;;
    esac
}

# Run production tests
run_production_tests() {
    local platform=$1

    if [[ "$SKIP_TESTS" == "true" ]]; then
        log WARN "Tests skipped by configuration"
        return 0
    fi

    print_header "Production Tests - $platform"

    case "$platform" in
        android)
            cd "$PROJECT_ROOT/android"

            log INFO "Running Android tests..."
            if [[ "$DRY_RUN" == "true" ]]; then
                log INFO "DRY RUN: Would run full Android test suite"
            else
                ./gradlew test || {
                    log ERROR "Android tests failed"
                    return 1
                }

                # Run lint checks
                ./gradlew lint || {
                    log WARN "Android lint warnings detected"
                }
            fi

            log SUCCESS "Android tests completed"
            ;;

        ios)
            if [[ "$OS_TYPE" == "Darwin" ]]; then
                cd "$PROJECT_ROOT/ios"

                log INFO "Running iOS tests..."
                if [[ "$DRY_RUN" == "true" ]]; then
                    log INFO "DRY RUN: Would run full iOS test suite"
                else
                    xcodebuild test \
                        -project SmilePile.xcodeproj \
                        -scheme SmilePile \
                        -destination 'platform=iOS Simulator,name=iPhone 15' \
                        || {
                        log ERROR "iOS tests failed"
                        return 1
                    }
                fi

                log SUCCESS "iOS tests completed"
            fi
            ;;
    esac
}

# Build Android AAB
build_android_aab() {
    print_header "Building Android App Bundle (AAB)"

    cd "$PROJECT_ROOT/android"

    # Clean if requested
    if [[ "$CLEAN_BUILD" == "true" ]]; then
        log INFO "Cleaning Android build..."
        if [[ "$DRY_RUN" != "true" ]]; then
            ./gradlew clean
        fi
    fi

    # Build AAB
    log INFO "Building release AAB..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would build AAB with ./gradlew bundleRelease"
    else
        ./gradlew bundleRelease || {
            log ERROR "AAB build failed"
            return 1
        }
    fi

    local aab_path="$PROJECT_ROOT/android/app/build/outputs/bundle/release/app-release.aab"

    if [[ ! -f "$aab_path" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "AAB not found at: $aab_path"
        return 1
    fi

    # Copy to artifacts
    mkdir -p "$DEPLOY_ROOT/artifacts/production"
    if [[ "$DRY_RUN" != "true" ]]; then
        cp "$aab_path" "$DEPLOY_ROOT/artifacts/production/SmilePile-${DEPLOYMENT_ID}.aab"

        # Generate APK from AAB for testing (optional)
        log INFO "Generating universal APK from AAB for testing..."
        if command -v bundletool >/dev/null 2>&1; then
            bundletool build-apks \
                --bundle="$aab_path" \
                --output="$DEPLOY_ROOT/artifacts/production/SmilePile-${DEPLOYMENT_ID}.apks" \
                --mode=universal \
                --ks="${ANDROID_KEYSTORE_PATH}" \
                --ks-pass="pass:${ANDROID_KEYSTORE_PASSWORD}" \
                --ks-key-alias="${ANDROID_KEY_ALIAS}" \
                --key-pass="pass:${ANDROID_KEY_PASSWORD}" \
                2>/dev/null || log WARN "Could not generate universal APK"
        fi
    fi

    # Size analysis
    if [[ "$DRY_RUN" != "true" ]]; then
        local aab_size=$(du -h "$aab_path" | cut -f1)
        log INFO "AAB size: $aab_size"

        # Check if size is reasonable
        local size_mb=$(du -m "$aab_path" | cut -f1)
        if [[ $size_mb -gt 150 ]]; then
            log WARN "AAB size exceeds 150MB (Google Play limit)"
        fi
    fi

    log SUCCESS "Android AAB generated successfully"
    log INFO "AAB location: $DEPLOY_ROOT/artifacts/production/"

    echo ""
    echo "📱 Next steps for Android:"
    echo "1. Go to Google Play Console: https://play.google.com/console"
    echo "2. Select your app"
    echo "3. Go to 'Production' > 'Releases'"
    echo "4. Upload the AAB file from artifacts/production/"
    echo "5. Fill in release notes and submit for review"
}

# Build iOS Archive
build_ios_archive() {
    print_header "Building iOS Archive"

    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log ERROR "iOS archive requires macOS"
        return 1
    fi

    cd "$PROJECT_ROOT/ios"

    # Clean if requested
    if [[ "$CLEAN_BUILD" == "true" ]]; then
        log INFO "Cleaning iOS build..."
        if [[ "$DRY_RUN" != "true" ]]; then
            xcodebuild clean -project SmilePile.xcodeproj -scheme SmilePile
            rm -rf ~/Library/Developer/Xcode/DerivedData/SmilePile-*
        fi
    fi

    # Build archive
    log INFO "Building iOS archive..."
    local archive_path="$DEPLOY_ROOT/artifacts/production/SmilePile-${DEPLOYMENT_ID}.xcarchive"

    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would build archive with xcodebuild archive"
    else
        xcodebuild archive \
            -project SmilePile.xcodeproj \
            -scheme SmilePile \
            -configuration Release \
            -archivePath "$archive_path" \
            -destination "generic/platform=iOS" \
            || {
            log ERROR "Archive build failed"
            return 1
        }
    fi

    # Export IPA
    if [[ "$DRY_RUN" != "true" ]] && [[ -d "$archive_path" ]]; then
        log INFO "Exporting IPA from archive..."

        # Create export options plist
        cat > "$DEPLOY_ROOT/artifacts/production/ExportOptions.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store</string>
    <key>teamID</key>
    <string>${APPLE_TEAM_ID:-YOUR_TEAM_ID}</string>
    <key>uploadSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <true/>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>thinning</key>
    <string>&lt;none&gt;</string>
</dict>
</plist>
EOF

        xcodebuild -exportArchive \
            -archivePath "$archive_path" \
            -exportPath "$DEPLOY_ROOT/artifacts/production" \
            -exportOptionsPlist "$DEPLOY_ROOT/artifacts/production/ExportOptions.plist" \
            || {
            log WARN "IPA export failed - archive still available for manual export"
        }
    fi

    log SUCCESS "iOS archive generated successfully"
    log INFO "Archive location: $archive_path"

    echo ""
    echo "📱 Next steps for iOS:"
    echo "1. Open Xcode"
    echo "2. Go to Window > Organizer"
    echo "3. Select the archive (or use the one in artifacts/production/)"
    echo "4. Click 'Distribute App'"
    echo "5. Choose 'App Store Connect' and follow the wizard"
    echo "6. Once uploaded, go to App Store Connect to submit for review"
    echo ""
    echo "Alternative: Use Transporter app with the exported IPA"
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

    # Process each platform
    local deploy_success=true

    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        bump_version "android"
        run_production_tests "android"
        build_android_aab || deploy_success=false
    fi

    if [[ "$PLATFORM" == "ios" ]] || [[ "$PLATFORM" == "both" ]]; then
        bump_version "ios"
        run_production_tests "ios"
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