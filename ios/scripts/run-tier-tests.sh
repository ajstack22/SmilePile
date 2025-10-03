#!/bin/bash
# SmilePile iOS Tiered Test Execution
# Phase 5 Implementation - ATLAS-TEST-001

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
IOS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$IOS_DIR")"

# Configuration
SCHEME="SmilePile"
DESTINATION="platform=iOS Simulator,name=iPhone 15,OS=latest"
DERIVED_DATA_PATH="${IOS_DIR}/DerivedData"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to run tests for a specific tier
run_tier_tests() {
    local tier_id="$1"
    local tier_name="$2"
    shift 2
    local test_classes=("$@")

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}${tier_name}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    local test_args=()
    for test_class in "${test_classes[@]}"; do
        test_args+=("-only-testing:SmilePileTests/${test_class}")
    done

    xcodebuild test \
        -scheme "$SCHEME" \
        -destination "$DESTINATION" \
        -derivedDataPath "$DERIVED_DATA_PATH" \
        "${test_args[@]}" \
        2>&1 | grep -E "Test Suite|Test Case|passed|failed|\*\* TEST" || true

    local result=${PIPESTATUS[0]}

    echo ""
    if [ $result -eq 0 ]; then
        echo -e "${GREEN}✅ ${tier_name} PASSED${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}❌ ${tier_name} FAILED${NC}"
        echo ""
        return 1
    fi
}

# Main execution
main() {
    local tier="${1:-all}"

    case "$tier" in
        tier1|critical)
            run_tier_tests "tier1" "TIER 1: Critical Tests (Security, Data Integrity)" \
                "PINManagerTests" \
                "PhotoImportSafetyTests" \
                "Core/Storage/StorageManagerTests" \
                "Core/Storage/ImageProcessorTests" \
                "Core/Data/CoreDataStackTests"
            ;;
        tier2|important)
            run_tier_tests "tier2" "TIER 2: Important Tests (Repositories, DI)" \
                "PhotoRepositoryTests" \
                "CategoryRepositoryTests" \
                "Core/DI/DIContainerTests"
            ;;
        tier3|ui)
            run_tier_tests "tier3" "TIER 3: UI Tests (Components, Integration)" \
                "SmilePileTests" \
                "Tests/EnhancedPhotoViewerTests"
            ;;
        smoke)
            run_tier_tests "smoke" "SMOKE TESTS: Quick Validation" \
                "PINManagerTests" \
                "PhotoImportSafetyTests"
            ;;
        all)
            echo -e "${YELLOW}Running all test tiers sequentially...${NC}"
            echo ""

            run_tier_tests "tier1" "TIER 1: Critical Tests (Security, Data Integrity)" \
                "PINManagerTests" \
                "PhotoImportSafetyTests" \
                "Core/Storage/StorageManagerTests" \
                "Core/Storage/ImageProcessorTests" \
                "Core/Data/CoreDataStackTests" || exit 1

            run_tier_tests "tier2" "TIER 2: Important Tests (Repositories, DI)" \
                "PhotoRepositoryTests" \
                "CategoryRepositoryTests" \
                "Core/DI/DIContainerTests" || exit 1

            run_tier_tests "tier3" "TIER 3: UI Tests (Components, Integration)" \
                "SmilePileTests" \
                "Tests/EnhancedPhotoViewerTests" || exit 1

            echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
            echo -e "${GREEN}All test tiers PASSED${NC}"
            echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
            echo ""
            ;;
        *)
            echo "Usage: $0 {tier1|tier2|tier3|smoke|all}"
            echo ""
            echo "Options:"
            echo "  tier1, critical  - Run Tier 1 Critical Tests (Security, Data)"
            echo "  tier2, important - Run Tier 2 Important Tests (Repositories)"
            echo "  tier3, ui        - Run Tier 3 UI Tests (Components)"
            echo "  smoke            - Run Smoke Tests (Quick validation)"
            echo "  all              - Run all tiers sequentially"
            exit 1
            ;;
    esac
}

main "$@"
