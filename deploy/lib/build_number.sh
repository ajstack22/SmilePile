#!/bin/bash
# ============================================================================
# Build Number Management Library
# ============================================================================
# Uses the same methodology as StackMap and Manylla:
# - Format: YYMMDDVVV where VVV is the daily build counter (001-999)
# - Version name: YY.MM.DD.VVV (e.g., 25.09.27.001)
# - Automatic increment for multiple builds on the same day
# ============================================================================

set -euo pipefail

# Build number file to track daily builds
BUILD_NUMBER_FILE="${PROJECT_ROOT}/.build_number"

# Get current date components
get_date_components() {
    echo "$(date +%y) $(date +%m) $(date +%d)"
}

# Generate build number in YYMMDDVVV format
generate_build_number() {
    local yy mm dd
    read yy mm dd <<< $(get_date_components)

    local date_prefix="${yy}${mm}${dd}"
    local daily_build_number=1

    # Check if we have a build number file
    if [[ -f "$BUILD_NUMBER_FILE" ]]; then
        local last_date=$(head -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "")
        local last_build=$(tail -n1 "$BUILD_NUMBER_FILE" 2>/dev/null || echo "0")

        if [[ "$last_date" == "$date_prefix" ]]; then
            # Same day, increment build number
            daily_build_number=$((last_build + 1))

            if [[ $daily_build_number -gt 999 ]]; then
                log ERROR "Maximum daily builds (999) exceeded"
                return 1
            fi
        fi
    fi

    # Format with leading zeros
    local build_suffix=$(printf "%03d" $daily_build_number)
    local build_number="${date_prefix}${build_suffix}"

    # Convert to integer (remove leading zeros for versionCode)
    local version_code=$((10#${build_number}))

    # Create version name in YY.MM.DD.VVV format
    local version_name="${yy}.${mm}.${dd}.${build_suffix}"

    # Save for next build
    echo "$date_prefix" > "$BUILD_NUMBER_FILE"
    echo "$daily_build_number" >> "$BUILD_NUMBER_FILE"

    # Export for use in scripts
    export BUILD_NUMBER="$build_number"
    export VERSION_CODE="$version_code"
    export VERSION_NAME="$version_name"

    log INFO "Build Number: $BUILD_NUMBER"
    log INFO "Version Code: $VERSION_CODE"
    log INFO "Version Name: $VERSION_NAME"

    return 0
}

# Update Android build.gradle.kts with new version
update_android_version() {
    local gradle_file="$PROJECT_ROOT/android/app/build.gradle.kts"

    if [[ ! -f "$gradle_file" ]]; then
        log ERROR "Android build.gradle.kts not found"
        return 1
    fi

    log INFO "Updating Android version to $VERSION_NAME ($VERSION_CODE)"

    # Update versionCode
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" "$gradle_file"

    # Update versionName
    sed -i.bak "s/versionName = \"[^\"]*\"/versionName = \"$VERSION_NAME\"/" "$gradle_file"

    # Remove backup file
    rm -f "${gradle_file}.bak"

    log SUCCESS "Android version updated"
}

# Update iOS Info.plist with new version
update_ios_version() {
    local plist_file="$PROJECT_ROOT/ios/SmilePile/Info.plist"

    if [[ ! -f "$plist_file" ]]; then
        log WARN "iOS Info.plist not found, trying alternate location"
        plist_file="$PROJECT_ROOT/ios/SmilePile/SmilePile-Info.plist"
    fi

    if [[ ! -f "$plist_file" ]]; then
        log ERROR "iOS Info.plist not found"
        return 1
    fi

    log INFO "Updating iOS version to $VERSION_NAME ($VERSION_CODE)"

    # Update CFBundleShortVersionString (version name)
    /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $VERSION_NAME" "$plist_file" 2>/dev/null || {
        # Try alternate method with plutil
        plutil -replace CFBundleShortVersionString -string "$VERSION_NAME" "$plist_file"
    }

    # Update CFBundleVersion (build number)
    /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $VERSION_CODE" "$plist_file" 2>/dev/null || {
        # Try alternate method with plutil
        plutil -replace CFBundleVersion -string "$VERSION_CODE" "$plist_file"
    }

    log SUCCESS "iOS version updated"
}

# Update both platforms with new version
update_version_all_platforms() {
    generate_build_number || return 1

    # Update based on platform parameter
    local platform="${1:-both}"

    case "$platform" in
        android)
            update_android_version || return 1
            ;;
        ios)
            update_ios_version || return 1
            ;;
        both)
            update_android_version || return 1
            update_ios_version || log WARN "iOS version update failed (may be on Linux)"
            ;;
        *)
            log ERROR "Invalid platform: $platform"
            return 1
            ;;
    esac

    return 0
}

# Get current version for display
get_current_version() {
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

# Reset daily build counter (for testing)
reset_daily_build_counter() {
    if [[ -f "$BUILD_NUMBER_FILE" ]]; then
        rm -f "$BUILD_NUMBER_FILE"
        log INFO "Build counter reset"
    fi
}

# Validate version format
validate_version_format() {
    local version="$1"

    if [[ ! "$version" =~ ^[0-9]{2}\.[0-9]{2}\.[0-9]{2}\.[0-9]{3}$ ]]; then
        log ERROR "Invalid version format: $version"
        log ERROR "Expected format: YY.MM.DD.VVV (e.g., 25.09.27.001)"
        return 1
    fi

    return 0
}

# Export functions for use in other scripts
export -f generate_build_number
export -f update_android_version
export -f update_ios_version
export -f update_version_all_platforms
export -f get_current_version
export -f reset_daily_build_counter
export -f validate_version_format