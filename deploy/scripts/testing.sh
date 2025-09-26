#!/bin/bash
# Testing Module
# Automated testing execution for deployment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# Run unit tests
run_unit_tests() {
    local platform="$1"
    local project_path="$2"
    local report_dir="${3:-${TEMP_DIR:-/tmp}/test_reports}"

    log_info "Running unit tests for $platform"
    mkdir -p "$report_dir"

    case "$platform" in
        android)
            cd "$project_path"

            # Run Android unit tests
            if [ -f "./gradlew" ]; then
                log_info "Executing Android unit tests"

                # Clean previous test results
                ./gradlew clean

                # Run unit tests with coverage
                if ./gradlew testDebugUnitTest jacocoTestReport; then
                    log_success "Android unit tests passed"

                    # Copy test reports
                    if [ -d "app/build/reports/tests" ]; then
                        cp -r app/build/reports/tests/* "$report_dir/" 2>/dev/null || true
                    fi

                    # Copy coverage reports
                    if [ -d "app/build/reports/jacoco" ]; then
                        cp -r app/build/reports/jacoco/* "$report_dir/" 2>/dev/null || true
                    fi

                    return 0
                else
                    log_error "Android unit tests failed"
                    return 1
                fi
            else
                log_error "Gradle wrapper not found"
                return 1
            fi
            ;;

        ios)
            cd "$project_path"

            # Run iOS unit tests
            log_info "Executing iOS unit tests"

            # Determine the scheme
            local scheme="${IOS_SCHEME:-SmilePile}"
            local destination="platform=iOS Simulator,name=iPhone 14,OS=latest"

            # Run tests with xcpretty if available for better output
            if command_exists "xcpretty"; then
                xcodebuild test \
                    -scheme "$scheme" \
                    -destination "$destination" \
                    -derivedDataPath DerivedData \
                    -resultBundlePath "$report_dir/test_results.xcresult" \
                    CODE_SIGN_IDENTITY="" \
                    CODE_SIGNING_REQUIRED=NO \
                    | xcpretty
            else
                xcodebuild test \
                    -scheme "$scheme" \
                    -destination "$destination" \
                    -derivedDataPath DerivedData \
                    -resultBundlePath "$report_dir/test_results.xcresult" \
                    CODE_SIGN_IDENTITY="" \
                    CODE_SIGNING_REQUIRED=NO
            fi

            if [ $? -eq 0 ]; then
                log_success "iOS unit tests passed"
                return 0
            else
                log_error "iOS unit tests failed"
                return 1
            fi
            ;;

        *)
            log_error "Unknown platform: $platform"
            return 1
            ;;
    esac
}

# Run integration tests
run_integration_tests() {
    local platform="$1"
    local project_path="$2"
    local environment="$3"

    log_info "Running integration tests for $platform in $environment"

    case "$platform" in
        android)
            cd "$project_path"

            if [ -f "./gradlew" ]; then
                # Run connected Android tests (requires emulator/device)
                if [ "${SKIP_CONNECTED_TESTS:-false}" = "false" ]; then
                    log_info "Running Android instrumented tests"

                    # Check if emulator or device is connected
                    if adb devices | grep -q "device$"; then
                        ./gradlew connectedDebugAndroidTest || {
                            log_warning "Android instrumented tests failed"
                            return 1
                        }
                        log_success "Android instrumented tests passed"
                    else
                        log_warning "No Android device/emulator connected, skipping instrumented tests"
                    fi
                else
                    log_info "Skipping connected tests (SKIP_CONNECTED_TESTS=true)"
                fi
            fi
            ;;

        ios)
            cd "$project_path"

            if [ "${SKIP_UI_TESTS:-false}" = "false" ]; then
                log_info "Running iOS UI tests"

                local scheme="${IOS_SCHEME:-SmilePile}"
                local destination="platform=iOS Simulator,name=iPhone 14,OS=latest"

                xcodebuild test \
                    -scheme "${scheme}UITests" \
                    -destination "$destination" \
                    -derivedDataPath DerivedData \
                    CODE_SIGN_IDENTITY="" \
                    CODE_SIGNING_REQUIRED=NO \
                    2>/dev/null || {
                    log_warning "iOS UI tests not configured or failed"
                }
            else
                log_info "Skipping UI tests (SKIP_UI_TESTS=true)"
            fi
            ;;
    esac

    return 0
}

# Run smoke tests
run_smoke_tests() {
    local platform="$1"
    local build_path="$2"
    local environment="$3"

    log_info "Running smoke tests for $platform in $environment"

    # Basic build verification
    case "$platform" in
        android)
            if [ -f "$build_path" ]; then
                # Verify APK structure
                log_info "Verifying Android APK structure"

                # Check APK size
                local apk_size=$(du -h "$build_path" | cut -f1)
                log_info "APK size: $apk_size"

                # Verify APK contents
                if command_exists "aapt"; then
                    aapt dump badging "$build_path" > /dev/null 2>&1
                    if [ $? -eq 0 ]; then
                        log_success "APK structure validation passed"

                        # Extract and display basic info
                        local package_name=$(aapt dump badging "$build_path" | grep package: | awk '{print $2}' | sed s/name=//g | sed s/\'//g)
                        local version_code=$(aapt dump badging "$build_path" | grep package: | awk '{print $3}' | sed s/versionCode=//g | sed s/\'//g)

                        log_info "Package: $package_name"
                        log_info "Version Code: $version_code"
                    else
                        log_error "APK structure validation failed"
                        return 1
                    fi
                else
                    log_warning "aapt not found, skipping detailed APK validation"
                fi
            else
                log_error "Build artifact not found: $build_path"
                return 1
            fi
            ;;

        ios)
            if [ -d "$build_path" ]; then
                log_info "Verifying iOS app bundle structure"

                # Check app bundle size
                local app_size=$(du -sh "$build_path" | cut -f1)
                log_info "App bundle size: $app_size"

                # Verify Info.plist
                local plist_path="$build_path/Info.plist"
                if [ -f "$plist_path" ]; then
                    local bundle_id=$(defaults read "$plist_path" CFBundleIdentifier 2>/dev/null || echo "unknown")
                    local version=$(defaults read "$plist_path" CFBundleShortVersionString 2>/dev/null || echo "unknown")

                    log_info "Bundle ID: $bundle_id"
                    log_info "Version: $version"
                    log_success "iOS app bundle validation passed"
                else
                    log_error "Info.plist not found in app bundle"
                    return 1
                fi
            else
                log_error "Build artifact not found: $build_path"
                return 1
            fi
            ;;
    esac

    return 0
}

# Check test coverage
check_test_coverage() {
    local platform="$1"
    local project_path="$2"
    local min_coverage="${3:-60}"

    log_info "Checking test coverage for $platform (minimum: ${min_coverage}%)"

    case "$platform" in
        android)
            # Parse Jacoco coverage report if available
            local coverage_file="$project_path/app/build/reports/jacoco/jacocoTestReport/html/index.html"
            if [ -f "$coverage_file" ]; then
                # Extract coverage percentage (basic parsing)
                local coverage=$(grep -oP 'Total.*?(\d+)%' "$coverage_file" | grep -oP '\d+' | head -1)

                if [ -n "$coverage" ]; then
                    log_info "Test coverage: ${coverage}%"

                    if [ "$coverage" -lt "$min_coverage" ]; then
                        log_warning "Test coverage (${coverage}%) is below minimum (${min_coverage}%)"
                        return 1
                    else
                        log_success "Test coverage meets requirements"
                    fi
                fi
            else
                log_warning "Coverage report not found, skipping coverage check"
            fi
            ;;

        ios)
            # Parse Xcode coverage report if available
            local coverage_path="$project_path/DerivedData/Logs/Test/*.xcresult"
            if ls $coverage_path 1> /dev/null 2>&1; then
                if command_exists "xcrun"; then
                    # Extract coverage using xcrun
                    local coverage_json=$(xcrun xccov view --report --json $coverage_path 2>/dev/null || echo "{}")

                    if [ "$coverage_json" != "{}" ]; then
                        # Parse coverage percentage
                        local coverage=$(echo "$coverage_json" | grep -oP '"lineCoverage":\s*\K[\d.]+' | head -1)
                        coverage=$(echo "$coverage * 100" | bc 2>/dev/null || echo "0")

                        if [ -n "$coverage" ] && [ "$coverage" != "0" ]; then
                            log_info "Test coverage: ${coverage}%"

                            if (( $(echo "$coverage < $min_coverage" | bc -l) )); then
                                log_warning "Test coverage (${coverage}%) is below minimum (${min_coverage}%)"
                                return 1
                            else
                                log_success "Test coverage meets requirements"
                            fi
                        fi
                    fi
                fi
            else
                log_warning "Coverage results not found, skipping coverage check"
            fi
            ;;
    esac

    return 0
}

# Run linting and code quality checks
run_lint_checks() {
    local platform="$1"
    local project_path="$2"

    log_info "Running lint checks for $platform"

    case "$platform" in
        android)
            cd "$project_path"

            if [ -f "./gradlew" ]; then
                log_info "Running Android lint"

                ./gradlew lint

                if [ $? -eq 0 ]; then
                    log_success "Android lint passed"

                    # Check for lint warnings/errors
                    local lint_report="app/build/reports/lint-results.xml"
                    if [ -f "$lint_report" ]; then
                        local error_count=$(grep -c "severity=\"Error\"" "$lint_report" || echo "0")
                        local warning_count=$(grep -c "severity=\"Warning\"" "$lint_report" || echo "0")

                        log_info "Lint results: $error_count errors, $warning_count warnings"

                        if [ "$error_count" -gt 0 ]; then
                            log_error "Lint found $error_count errors"
                            return 1
                        fi
                    fi
                else
                    log_error "Android lint failed"
                    return 1
                fi
            fi
            ;;

        ios)
            cd "$project_path"

            # Run SwiftLint if available
            if command_exists "swiftlint"; then
                log_info "Running SwiftLint"

                swiftlint lint --reporter json > "${TEMP_DIR:-/tmp}/swiftlint_report.json"

                local violations=$(cat "${TEMP_DIR:-/tmp}/swiftlint_report.json" | grep -c "\"severity\" : \"error\"" || echo "0")

                if [ "$violations" -gt 0 ]; then
                    log_error "SwiftLint found $violations errors"
                    return 1
                else
                    log_success "SwiftLint passed"
                fi
            else
                log_warning "SwiftLint not installed, skipping iOS lint checks"
            fi
            ;;
    esac

    return 0
}

# Main testing function
run_all_tests() {
    local platform="$1"
    local project_path="$2"
    local environment="$3"
    local test_level="${4:-basic}"  # basic, full, or smoke

    log_info "Starting test suite for $platform in $environment (level: $test_level)"

    local failed_tests=0

    # Always run lint checks
    run_lint_checks "$platform" "$project_path" || ((failed_tests++))

    case "$test_level" in
        smoke)
            # Only smoke tests for quick validation
            log_info "Running smoke tests only"
            ;;

        basic)
            # Unit tests and basic checks
            run_unit_tests "$platform" "$project_path" || ((failed_tests++))
            check_test_coverage "$platform" "$project_path" || ((failed_tests++))
            ;;

        full)
            # Full test suite
            run_unit_tests "$platform" "$project_path" || ((failed_tests++))
            run_integration_tests "$platform" "$project_path" "$environment" || ((failed_tests++))
            check_test_coverage "$platform" "$project_path" || ((failed_tests++))
            ;;

        *)
            log_error "Unknown test level: $test_level"
            return 1
            ;;
    esac

    if [ $failed_tests -gt 0 ]; then
        log_error "Test suite completed with $failed_tests failures"
        return 1
    else
        log_success "All tests passed successfully"
    fi

    return 0
}

# Export functions
export -f run_unit_tests run_integration_tests run_smoke_tests
export -f check_test_coverage run_lint_checks run_all_tests