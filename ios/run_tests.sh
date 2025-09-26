#!/bin/bash

# Run tests for SmilePile iOS
# This script compiles and runs tests without Xcode project configuration

set -e

echo "🧪 Running SmilePile iOS Tests"
echo "================================"

# Set up directories
TEST_DIR="/Users/adamstack/SmilePile/ios/SmilePileTests"
SOURCE_DIR="/Users/adamstack/SmilePile/ios/SmilePile"
BUILD_DIR="/Users/adamstack/SmilePile/ios/build_test"

# Clean build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Find all Swift files in the main app (excluding tests and preview content)
SOURCE_FILES=$(find "$SOURCE_DIR" -name "*.swift" -not -path "*/Preview Content/*" | tr '\n' ' ')

# Find all test files
TEST_FILES=$(find "$TEST_DIR" -name "*.swift" | tr '\n' ' ')

echo "📦 Compiling tests..."

# Compile with xcrun swift (simpler approach for testing)
xcrun swift test \
    -Xswiftc -target -Xswiftc x86_64-apple-ios16.0-simulator \
    -Xswiftc -sdk -Xswiftc $(xcrun --sdk iphonesimulator --show-sdk-path) \
    -Xswiftc -I"$SOURCE_DIR" \
    $SOURCE_FILES $TEST_FILES \
    2>&1 | tee "$BUILD_DIR/test_output.log"

echo "✅ Tests completed"