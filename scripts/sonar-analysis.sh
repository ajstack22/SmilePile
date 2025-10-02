#!/bin/bash

# SonarCloud Analysis Script for SmilePile
# Public repository - unlimited free analysis!

set -e

echo "🔍 SonarCloud Code Quality Analysis for SmilePile"
echo "================================================="

# Check if SONAR_TOKEN is set
if [ -z "$SONAR_TOKEN" ]; then
    # Try to load from env files
    if [ -f "$HOME/.manylla-env" ]; then
        source "$HOME/.manylla-env"
    elif [ -f "$HOME/.smilepile-env" ]; then
        source "$HOME/.smilepile-env"
    fi

    # Check again after loading
    if [ -z "$SONAR_TOKEN" ]; then
        echo "⚠️  SONAR_TOKEN not set"
        echo "Set it with: export SONAR_TOKEN='your-token'"
        echo "Or create ~/.smilepile-env with: SONAR_TOKEN=\"your-token\""
        echo ""
        echo "To get a token:"
        echo "1. Go to https://sonarcloud.io"
        echo "2. Sign in with GitHub"
        echo "3. Go to My Account > Security"
        echo "4. Generate a token for 'SmilePile'"
        exit 1
    fi
fi

# Get git information for version
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')

echo "📌 Branch: $GIT_BRANCH"
echo "📌 Commit: $GIT_COMMIT"
echo "✅ Public repo: Unlimited analysis!"
echo ""

# IMPORTANT: Skip JRE provisioning to avoid 403 errors
# This must be set BEFORE running the scanner
export SONAR_SCANNER_SKIP_JRE_PROVISIONING=true

# Build Android if gradle is present
if [ -f "android/gradlew" ]; then
    echo "📱 Building Android project..."
    cd android
    ./gradlew assembleDebug testDebugUnitTest lint --no-daemon 2>/dev/null || {
        echo "⚠️  Android build had issues, continuing..."
    }

    # Generate JaCoCo coverage report
    echo "📊 Generating code coverage report..."
    ./gradlew :app:jacocoDebugTestReport --no-daemon || {
        echo "⚠️  Coverage report generation had issues, continuing..."
    }

    # Check if coverage report was generated
    if [ -f "app/build/reports/jacoco/jacocoDebugTestReport/jacocoDebugTestReport.xml" ]; then
        echo "✅ Coverage report generated successfully"
    else
        echo "⚠️  Coverage report not found, analysis will continue without coverage"
    fi
    cd ..
fi

# Build iOS if xcodebuild is available
if command -v xcodebuild >/dev/null 2>&1 && [ -d "ios" ]; then
    echo "🍎 Building iOS project..."
    cd ios

    # Run SwiftLint if available
    if command -v swiftlint >/dev/null 2>&1; then
        swiftlint lint --reporter json > swiftlint-report.json 2>/dev/null || {
            echo "ℹ️  SwiftLint completed with warnings"
        }
    fi

    # Build for analysis
    xcodebuild -project SmilePile.xcodeproj \
               -scheme SmilePile \
               -configuration Debug \
               -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
               -derivedDataPath DerivedData \
               clean build CODE_SIGNING_ALLOWED=NO COMPILER_INDEX_STORE_ENABLE=NO 2>/dev/null || {
        echo "⚠️  iOS build had issues, continuing..."
    }
    cd ..
fi

# Run SonarCloud analysis
echo ""
echo "☁️  Sending analysis to SonarCloud..."
# Token is passed via environment variable for security
# Skip JRE provisioning to avoid 403 errors (set inline to ensure it's passed to npx)
SONAR_SCANNER_SKIP_JRE_PROVISIONING=true npx sonar-scanner \
  -Dsonar.scanner.skipJreProvisioning=true \
  -Dsonar.projectVersion="$GIT_COMMIT" \
  -Dsonar.branch.name="$GIT_BRANCH"

echo ""
echo "✅ Analysis complete!"
echo "📊 View results at: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile"
echo ""
echo "📈 Quality metrics available at:"
echo "   - Code Smells: https://sonarcloud.io/project/issues?id=ajstack22_SmilePile&resolved=false&types=CODE_SMELL"
echo "   - Bugs: https://sonarcloud.io/project/issues?id=ajstack22_SmilePile&resolved=false&types=BUG"
echo "   - Security: https://sonarcloud.io/project/security_hotspots?id=ajstack22_SmilePile"
echo "   - Coverage: https://sonarcloud.io/component_measures?id=ajstack22_SmilePile&metric=coverage"