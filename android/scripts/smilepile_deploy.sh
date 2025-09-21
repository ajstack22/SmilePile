#!/bin/bash

# SmilePile Deployment Validation Script
# This script validates the app before deployment by checking:
# - APK size (<20MB)
# - Debug log count (max 5 Log.d statements)
# - TODO count (max 20)
# - Running smoke tests

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
MAX_APK_SIZE_MB=20
MAX_DEBUG_LOGS=5
MAX_TODOS=20

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}SmilePile Deployment Validation${NC}"
echo -e "${GREEN}======================================${NC}"

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Project root: $PROJECT_ROOT"
cd "$PROJECT_ROOT"

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" = "PASS" ]; then
        echo -e "[${GREEN}PASS${NC}] $message"
    elif [ "$status" = "FAIL" ]; then
        echo -e "[${RED}FAIL${NC}] $message"
    elif [ "$status" = "INFO" ]; then
        echo -e "[${YELLOW}INFO${NC}] $message"
    fi
}

# Check 1: APK Size
echo -e "\n${YELLOW}1. Checking APK size...${NC}"

APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"

if [ ! -f "$APK_PATH" ]; then
    print_status "INFO" "Release APK not found, building it first..."
    ./gradlew assembleRelease
fi

if [ -f "$APK_PATH" ]; then
    APK_SIZE_BYTES=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
    APK_SIZE_MB=$((APK_SIZE_BYTES / 1024 / 1024))

    if [ $APK_SIZE_MB -lt $MAX_APK_SIZE_MB ]; then
        print_status "PASS" "APK size: ${APK_SIZE_MB}MB (< ${MAX_APK_SIZE_MB}MB)"
    else
        print_status "FAIL" "APK size: ${APK_SIZE_MB}MB exceeds limit of ${MAX_APK_SIZE_MB}MB"
        exit 1
    fi
else
    print_status "FAIL" "Could not find or build release APK"
    exit 1
fi

# Check 2: Debug Log Count
echo -e "\n${YELLOW}2. Checking debug log count...${NC}"

DEBUG_LOG_COUNT=$(find "$PROJECT_ROOT/app/src" -name "*.kt" -exec grep -l "Log\.d\|Log\.debug" {} \; | wc -l | tr -d ' ')

if [ $DEBUG_LOG_COUNT -le $MAX_DEBUG_LOGS ]; then
    print_status "PASS" "Debug logs: $DEBUG_LOG_COUNT files (≤ $MAX_DEBUG_LOGS)"
else
    print_status "FAIL" "Debug logs: $DEBUG_LOG_COUNT files exceeds limit of $MAX_DEBUG_LOGS"
    echo "Files with debug logs:"
    find "$PROJECT_ROOT/app/src" -name "*.kt" -exec grep -l "Log\.d\|Log\.debug" {} \;
    exit 1
fi

# Check 3: TODO Count
echo -e "\n${YELLOW}3. Checking TODO count...${NC}"

TODO_COUNT=$(find "$PROJECT_ROOT/app/src" -name "*.kt" -exec grep -i "todo\|fixme\|hack" {} \; | wc -l | tr -d ' ')

if [ $TODO_COUNT -le $MAX_TODOS ]; then
    print_status "PASS" "TODOs: $TODO_COUNT (≤ $MAX_TODOS)"
else
    print_status "FAIL" "TODOs: $TODO_COUNT exceeds limit of $MAX_TODOS"
    echo "TODOs found:"
    find "$PROJECT_ROOT/app/src" -name "*.kt" -exec grep -i "todo\|fixme\|hack" {} \; | head -10
    exit 1
fi

# Check 4: Smoke Tests
echo -e "\n${YELLOW}4. Running smoke tests...${NC}"

# Check if we have connected devices for testing
ADB_DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')

if [ $ADB_DEVICES -gt 0 ]; then
    print_status "INFO" "Found $ADB_DEVICES connected device(s), running connected tests..."
    if ./gradlew connectedAndroidTest --quiet; then
        print_status "PASS" "Smoke tests completed successfully"
    else
        print_status "FAIL" "Smoke tests failed"
        exit 1
    fi
else
    print_status "INFO" "No connected devices found, running unit tests instead..."
    if ./gradlew test --quiet; then
        print_status "PASS" "Unit tests completed successfully"
    else
        print_status "FAIL" "Unit tests failed"
        exit 1
    fi
fi

# All checks passed
echo -e "\n${GREEN}======================================${NC}"
echo -e "${GREEN}All validation checks PASSED!${NC}"
echo -e "${GREEN}App is ready for deployment${NC}"
echo -e "${GREEN}======================================${NC}"

exit 0