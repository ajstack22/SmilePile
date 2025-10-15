#!/bin/bash
# ============================================================================
# SmilePile Deployment Router
# ============================================================================
# Master deployment script that routes to tier-specific scripts
# Usage: ./deploy.sh <tier> <platform> [flags]

set -euo pipefail

# ============================================================================
# Script Configuration
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DEPLOY_ROOT="$SCRIPT_DIR"
export PROJECT_ROOT="$(dirname "$DEPLOY_ROOT")"

# Source libraries
source "${DEPLOY_ROOT}/lib/common.sh"

# ============================================================================
# Functions
# ============================================================================

usage() {
    cat << EOF
================================================================================
SmilePile Deployment Router
================================================================================

Routes deployment requests to appropriate tier-specific scripts.

Usage: $0 <tier> <platform> [options]

Tiers:
    qual        Quality - Local devices + GitHub commit
    stage       Stage - TestFlight Internal + Play Console Internal
    beta        Beta - TestFlight External + Play Console Closed Testing
    prod        Production - Store packages for manual submission

Platforms:
    android     Deploy to Android only
    ios         Deploy to iOS only
    both        Deploy to both platforms (default)

Environment Variables:
    SKIP_TESTS=true         Skip automated tests
    SKIP_COMMIT=true        Skip git commit/push (qual/stage/beta only)
    ALLOW_UNCOMMITTED=true  Allow deployment with uncommitted changes
    DRY_RUN=true           Test run without actual deployment
    REQUIRE_APPROVAL=false  Skip approval gates (beta/prod)

Examples:
    # Deploy to quality tier (both platforms)
    $0 qual both

    # Deploy iOS to stage tier
    $0 stage ios

    # Deploy to beta with dry run
    DRY_RUN=true $0 beta both

    # Deploy to production (requires approval)
    $0 prod both

    # Quick quality deployment to Android
    $0 qual android

For tier-specific help:
    ./deploy/deploy_qual.sh --help
    ./deploy/deploy_stage.sh --help
    ./deploy/deploy_beta.sh --help
    ./deploy/deploy_prod.sh --help

================================================================================
EOF
    exit 0
}

# Validate tier input
validate_tier() {
    local tier=$1

    # Security: Whitelist validation to prevent path traversal/injection
    if [[ ! "$tier" =~ ^(qual|stage|beta|prod)$ ]]; then
        log ERROR "Invalid tier: $tier"
        log ERROR "Valid tiers: qual, stage, beta, prod"
        return 1
    fi

    return 0
}

# Validate platform input
validate_platform() {
    local platform=$1

    # Security: Whitelist validation
    if [[ ! "$platform" =~ ^(android|ios|both)$ ]]; then
        log ERROR "Invalid platform: $platform"
        log ERROR "Valid platforms: android, ios, both"
        return 1
    fi

    return 0
}

# Check for concurrent deployments (tier-specific lock)
check_deployment_lock() {
    local tier=$1
    local lock_file="$PROJECT_ROOT/.git/deployment-${tier}.lock"

    if [[ -f "$lock_file" ]]; then
        local lock_pid=$(cat "$lock_file" 2>/dev/null || echo "")

        # Check if process is still running
        if [[ -n "$lock_pid" ]] && kill -0 "$lock_pid" 2>/dev/null; then
            log ERROR "Another $tier deployment is already in progress (PID: $lock_pid)"
            log ERROR "If this is incorrect, remove: $lock_file"
            return 1
        else
            # Stale lock file, remove it
            rm -f "$lock_file"
        fi
    fi

    # Create lock file with current PID
    echo $$ > "$lock_file"

    # Set trap to remove lock on exit
    trap "rm -f '$lock_file'" EXIT

    return 0
}

# Route to tier-specific script
route_to_tier() {
    local tier=$1
    local platform=$2

    local script="${DEPLOY_ROOT}/deploy_${tier}.sh"

    if [[ ! -f "$script" ]]; then
        log ERROR "Tier script not found: $script"
        return 1
    fi

    if [[ ! -x "$script" ]]; then
        log ERROR "Tier script not executable: $script"
        log INFO "Fix with: chmod +x $script"
        return 1
    fi

    # Phase 4 requirement: Tier-specific deployment lock
    if [[ "${DRY_RUN:-false}" != "true" ]]; then
        check_deployment_lock "$tier" || return 1
    fi

    log INFO "Routing to tier: $tier ($platform)"
    log INFO "Executing: $script $platform"
    echo ""

    # Execute tier-specific script with all environment variables passed through
    exec "$script" "$platform"
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    # Parse arguments
    local tier="${1:-}"
    local platform="${2:-both}"

    # Handle help flags
    if [[ "$tier" == "-h" ]] || [[ "$tier" == "--help" ]] || [[ "$tier" == "help" ]] || [[ -z "$tier" ]]; then
        usage
    fi

    # Initialize
    init_deployment_system

    print_header "SmilePile Deployment Router"

    log INFO "Requested tier: $tier"
    log INFO "Requested platform: $platform"

    # Validate inputs
    validate_tier "$tier" || exit 1
    validate_platform "$platform" || exit 1

    # Display tier information
    case "$tier" in
        qual)
            log INFO "Quality Tier: Local devices + GitHub"
            ;;
        stage)
            log INFO "Stage Tier: TestFlight Internal + Play Console Internal"
            ;;
        beta)
            log INFO "Beta Tier: TestFlight External + Play Console Closed Testing"
            ;;
        prod)
            log INFO "Production Tier: Store packages for manual submission"
            ;;
    esac

    echo ""

    # Route to tier-specific script
    route_to_tier "$tier" "$platform"
}

# Run main
main "$@"
