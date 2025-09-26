#!/bin/bash
# Security Scanning Module
# Security checks for deployment process

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# Run dependency vulnerability scan
run_dependency_scan() {
    local platform="$1"
    local project_path="$2"

    log_info "Running dependency vulnerability scan for $platform"

    case "$platform" in
        android)
            if command_exists "gradle"; then
                cd "$project_path"
                # Check for dependency updates and vulnerabilities
                ./gradlew dependencyCheckAnalyze 2>/dev/null || {
                    log_warning "Dependency check plugin not configured, skipping detailed scan"
                }

                # Basic dependency listing for manual review
                ./gradlew dependencies > "${TEMP_DIR:-/tmp}/dependencies.txt"
                log_info "Dependencies exported for review"
            else
                log_warning "Gradle not found, skipping Android dependency scan"
            fi
            ;;

        ios)
            if command_exists "pod"; then
                cd "$project_path"
                # CocoaPods vulnerability check
                pod outdated || true

                # Export dependencies for review
                pod list > "${TEMP_DIR:-/tmp}/dependencies.txt"
                log_info "Pod dependencies exported for review"
            else
                log_warning "CocoaPods not found, skipping iOS dependency scan"
            fi

            # Swift Package Manager check if available
            if [ -f "Package.swift" ]; then
                swift package show-dependencies > "${TEMP_DIR:-/tmp}/swift_dependencies.txt"
            fi
            ;;

        *)
            log_error "Unknown platform: $platform"
            return 1
            ;;
    esac

    log_success "Dependency scan completed for $platform"
    return 0
}

# Scan for hardcoded secrets
scan_for_secrets() {
    local scan_path="$1"
    local exclude_dirs=(".git" "node_modules" "build" "dist" "DerivedData")

    log_info "Scanning for hardcoded secrets in $scan_path"

    # Build exclude pattern
    local exclude_pattern=""
    for dir in "${exclude_dirs[@]}"; do
        exclude_pattern="$exclude_pattern -not -path '*/$dir/*'"
    done

    # Common patterns for secrets
    local patterns=(
        "api[_-]?key"
        "api[_-]?secret"
        "access[_-]?token"
        "auth[_-]?token"
        "private[_-]?key"
        "secret[_-]?key"
        "password"
        "passwd"
        "pwd"
        "credentials"
        "AKIA[0-9A-Z]{16}"  # AWS Access Key
        "AIza[0-9A-Za-z_-]{35}"  # Google API Key
        "[0-9a-f]{40}"  # Generic API token pattern
    )

    local found_issues=0

    for pattern in "${patterns[@]}"; do
        # Search for pattern in files (case-insensitive)
        local results=$(eval "find '$scan_path' -type f $exclude_pattern -exec grep -l -i '$pattern' {} \; 2>/dev/null" || true)

        if [ -n "$results" ]; then
            log_warning "Potential secrets found matching pattern: $pattern"
            echo "$results" | while read -r file; do
                log_warning "  - $file"
                ((found_issues++))
            done
        fi
    done

    if [ $found_issues -gt 0 ]; then
        log_warning "Found $found_issues potential security issues"
        log_warning "Please review the files above and ensure no secrets are hardcoded"
        return 1
    fi

    log_success "No hardcoded secrets detected"
    return 0
}

# Check file permissions
check_permissions() {
    local check_path="$1"
    local platform="$2"

    log_info "Checking file permissions for $platform"

    # Find files with overly permissive permissions
    local world_writable=$(find "$check_path" -type f -perm -002 2>/dev/null || true)

    if [ -n "$world_writable" ]; then
        log_warning "Found world-writable files:"
        echo "$world_writable" | while read -r file; do
            log_warning "  - $file"
        done
        log_warning "Consider restricting permissions on sensitive files"
    fi

    # Check for executable files that shouldn't be
    case "$platform" in
        android)
            # Check Java/Kotlin files aren't executable
            local exec_source=$(find "$check_path" -name "*.java" -o -name "*.kt" -o -name "*.xml" | xargs ls -l | grep "^-..x" || true)
            ;;
        ios)
            # Check Swift/Objective-C files aren't executable
            local exec_source=$(find "$check_path" -name "*.swift" -o -name "*.m" -o -name "*.h" | xargs ls -l | grep "^-..x" || true)
            ;;
    esac

    if [ -n "${exec_source:-}" ]; then
        log_warning "Found source files with executable permissions"
        log_warning "Consider removing executable permission from source files"
    fi

    log_success "Permission check completed"
    return 0
}

# Validate SSL/TLS configuration
validate_ssl_config() {
    local platform="$1"
    local project_path="$2"

    log_info "Validating SSL/TLS configuration for $platform"

    case "$platform" in
        android)
            # Check network security config
            local manifest_path="$project_path/app/src/main/AndroidManifest.xml"
            if [ -f "$manifest_path" ]; then
                if grep -q "cleartextTrafficPermitted=\"true\"" "$manifest_path"; then
                    log_warning "Clear text traffic is permitted in AndroidManifest.xml"
                    log_warning "Consider disabling cleartext traffic for production"
                fi
            fi
            ;;

        ios)
            # Check App Transport Security settings
            local plist_path="$project_path/SmilePile/Info.plist"
            if [ -f "$plist_path" ]; then
                if grep -A 5 "NSAppTransportSecurity" "$plist_path" | grep -q "NSAllowsArbitraryLoads.*true"; then
                    log_warning "App Transport Security allows arbitrary loads"
                    log_warning "Consider restricting ATS settings for production"
                fi
            fi
            ;;
    esac

    log_success "SSL/TLS validation completed"
    return 0
}

# Run SAST (Static Application Security Testing)
run_sast_scan() {
    local platform="$1"
    local project_path="$2"
    local report_path="${3:-${TEMP_DIR:-/tmp}/sast_report.json}"

    log_info "Running SAST scan for $platform"

    # Check if semgrep is available (recommended SAST tool)
    if command_exists "semgrep"; then
        log_info "Running Semgrep security scan"

        # Run semgrep with security rulesets
        semgrep --config=auto \
                --json \
                --output="$report_path" \
                "$project_path" 2>/dev/null || {
            log_warning "Semgrep scan completed with findings"
        }

        # Parse and display critical findings
        if [ -f "$report_path" ]; then
            local critical_count=$(jq '.results | map(select(.extra.severity == "ERROR")) | length' "$report_path" 2>/dev/null || echo "0")
            local warning_count=$(jq '.results | map(select(.extra.severity == "WARNING")) | length' "$report_path" 2>/dev/null || echo "0")

            if [ "$critical_count" -gt 0 ]; then
                log_error "Found $critical_count critical security issues"
                return 1
            elif [ "$warning_count" -gt 0 ]; then
                log_warning "Found $warning_count security warnings"
            else
                log_success "No security issues found by SAST scan"
            fi
        fi
    else
        log_warning "Semgrep not installed. Install with: pip install semgrep"
        log_info "Falling back to basic security checks"

        # Basic pattern matching for common vulnerabilities
        scan_for_secrets "$project_path"
    fi

    return 0
}

# Check for outdated dependencies with known vulnerabilities
check_vulnerable_dependencies() {
    local platform="$1"
    local project_path="$2"

    log_info "Checking for vulnerable dependencies in $platform"

    case "$platform" in
        android)
            if [ -f "$project_path/build.gradle" ] || [ -f "$project_path/build.gradle.kts" ]; then
                # Check for common vulnerable libraries
                local gradle_files=$(find "$project_path" -name "*.gradle" -o -name "*.gradle.kts")

                # List of known vulnerable library patterns (example)
                local vulnerable_patterns=(
                    "com.squareup.okhttp:okhttp:2."  # OkHttp 2.x has known vulnerabilities
                    "log4j:log4j:1."  # Log4j 1.x vulnerabilities
                )

                for pattern in "${vulnerable_patterns[@]}"; do
                    if grep -r "$pattern" $gradle_files > /dev/null 2>&1; then
                        log_warning "Potentially vulnerable dependency found: $pattern"
                    fi
                done
            fi
            ;;

        ios)
            if [ -f "$project_path/Podfile.lock" ]; then
                # Check CocoaPods for known issues
                log_info "Checking CocoaPods dependencies"

                # Example: Check for outdated pods
                cd "$project_path"
                pod outdated 2>/dev/null | grep -E "The following pod updates are available" && {
                    log_warning "Some pods have updates available, which may include security fixes"
                }
            fi
            ;;
    esac

    log_success "Dependency vulnerability check completed"
    return 0
}

# Main security check function
run_security_checks() {
    local platform="$1"
    local project_path="$2"
    local environment="$3"
    local strict_mode="${4:-false}"

    log_info "Starting security checks for $platform in $environment environment"

    local failed_checks=0

    # Run all security checks
    run_dependency_scan "$platform" "$project_path" || ((failed_checks++))
    scan_for_secrets "$project_path" || ((failed_checks++))
    check_permissions "$project_path" "$platform" || ((failed_checks++))
    validate_ssl_config "$platform" "$project_path" || ((failed_checks++))
    check_vulnerable_dependencies "$platform" "$project_path" || ((failed_checks++))

    # Run SAST only in qual/prod environments
    if [ "$environment" != "development" ]; then
        run_sast_scan "$platform" "$project_path" || ((failed_checks++))
    fi

    if [ $failed_checks -gt 0 ]; then
        log_warning "Security checks completed with $failed_checks warnings/errors"

        if [ "$strict_mode" = "true" ] && [ "$environment" = "production" ]; then
            log_error "Security checks failed in strict mode for production"
            return 1
        fi
    else
        log_success "All security checks passed successfully"
    fi

    return 0
}

# Export functions
export -f run_dependency_scan scan_for_secrets check_permissions
export -f validate_ssl_config run_sast_scan check_vulnerable_dependencies
export -f run_security_checks