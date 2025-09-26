#!/bin/bash
# ============================================================================
# SmilePile Deployment System - Initialization Script
# ============================================================================
# Sets up the deployment system for first-time use
# Run this script after cloning the repository

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Print colored message
print_message() {
    local color=$1
    shift
    echo -e "${color}$*${NC}"
}

# Print header
print_header() {
    echo ""
    echo "============================================================================"
    echo " $1"
    echo "============================================================================"
    echo ""
}

# Main initialization
main() {
    print_header "SmilePile Deployment System Initialization"

    print_message "$BLUE" "Initializing deployment system at: $SCRIPT_DIR"

    # Step 1: Create directory structure
    print_message "$YELLOW" "Creating directory structure..."

    mkdir -p "$SCRIPT_DIR"/{lib,environments,secrets,artifacts,backups,logs,reports,history,temp}
    mkdir -p "$SCRIPT_DIR"/artifacts/{development,quality,staging,production}/{android,ios}

    print_message "$GREEN" "✓ Directory structure created"

    # Step 2: Set permissions
    print_message "$YELLOW" "Setting permissions..."

    chmod 755 "$SCRIPT_DIR"/*.sh 2>/dev/null || true
    chmod 755 "$SCRIPT_DIR"/lib/*.sh 2>/dev/null || true
    chmod 700 "$SCRIPT_DIR"/secrets
    chmod 755 "$SCRIPT_DIR"/{artifacts,backups,logs,reports,history}

    print_message "$GREEN" "✓ Permissions configured"

    # Step 3: Create example secrets file
    print_message "$YELLOW" "Creating example secrets file..."

    if [[ ! -f "$SCRIPT_DIR/secrets/example.env" ]]; then
        cat > "$SCRIPT_DIR/secrets/example.env" << 'EOF'
# ============================================================================
# Example Secrets File
# ============================================================================
# Copy this file to <environment>.env and fill in actual values
# NEVER commit actual secret files to version control

# Android Signing
ANDROID_KEYSTORE_PASSWORD="change_me"
ANDROID_KEY_PASSWORD="change_me"
ANDROID_KEY_ALIAS="change_me"

# iOS Signing
IOS_CERTIFICATE_PASSWORD="change_me"
IOS_PROVISIONING_PASSWORD="change_me"
APP_STORE_USERNAME="change_me"
APP_STORE_PASSWORD="change_me"

# API Keys
FIREBASE_API_KEY="change_me"
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON="/path/to/service-account.json"
APP_STORE_CONNECT_API_KEY="change_me"

# Notification Services
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/XXX/YYY/ZZZ"
EMAIL_SMTP_PASSWORD="change_me"

# Cloud Services
AWS_ACCESS_KEY_ID="change_me"
AWS_SECRET_ACCESS_KEY="change_me"

# Deployment Secret Key (for encryption)
DEPLOY_SECRET_KEY="change_me_strong_password"
EOF
        chmod 600 "$SCRIPT_DIR/secrets/example.env"
        print_message "$GREEN" "✓ Example secrets file created"
    else
        print_message "$BLUE" "ℹ Example secrets file already exists"
    fi

    # Step 4: Verify dependencies
    print_message "$YELLOW" "Verifying dependencies..."

    local missing_deps=()

    # Check required commands
    for cmd in git bash curl; do
        if ! command -v "$cmd" &> /dev/null; then
            missing_deps+=("$cmd")
        fi
    done

    # Check optional but recommended commands
    local optional_deps=()
    for cmd in jq openssl; do
        if ! command -v "$cmd" &> /dev/null; then
            optional_deps+=("$cmd")
        fi
    done

    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        print_message "$RED" "✗ Missing required dependencies: ${missing_deps[*]}"
        print_message "$RED" "Please install these before continuing"
        exit 1
    else
        print_message "$GREEN" "✓ All required dependencies found"
    fi

    if [[ ${#optional_deps[@]} -gt 0 ]]; then
        print_message "$YELLOW" "⚠ Missing optional dependencies: ${optional_deps[*]}"
        print_message "$YELLOW" "  Some features may not work without these"
    fi

    # Step 5: Check platform-specific requirements
    print_message "$YELLOW" "Checking platform-specific requirements..."

    if [[ "$(uname -s)" == "Darwin" ]]; then
        # macOS - check for Xcode
        if command -v xcodebuild &> /dev/null; then
            print_message "$GREEN" "✓ Xcode found (iOS builds available)"
        else
            print_message "$YELLOW" "⚠ Xcode not found (iOS builds will not work)"
        fi
    else
        print_message "$BLUE" "ℹ Not on macOS - iOS builds will not be available"
    fi

    # Check for Android SDK
    if [[ -n "${ANDROID_HOME:-}" ]] && [[ -d "$ANDROID_HOME" ]]; then
        print_message "$GREEN" "✓ Android SDK found at: $ANDROID_HOME"
    else
        print_message "$YELLOW" "⚠ Android SDK not found (set ANDROID_HOME)"
    fi

    # Step 6: Initialize git hooks (optional)
    if [[ -d "$PROJECT_ROOT/.git" ]]; then
        print_message "$YELLOW" "Setting up git hooks..."

        # Create pre-commit hook to prevent secrets
        cat > "$PROJECT_ROOT/.git/hooks/pre-commit" << 'EOF'
#!/bin/bash
# Prevent committing secrets

# Check for common secret patterns
if git diff --cached --name-only | xargs grep -E "(password|secret|key|token).*=.*['\"]" 2>/dev/null; then
    echo "Error: Possible secrets detected in commit"
    echo "Please review your changes"
    exit 1
fi
EOF
        chmod +x "$PROJECT_ROOT/.git/hooks/pre-commit" 2>/dev/null || true

        print_message "$GREEN" "✓ Git hooks configured"
    fi

    # Step 7: Create initial deployment history
    if [[ ! -f "$SCRIPT_DIR/history/deployments.json" ]]; then
        echo "[]" > "$SCRIPT_DIR/history/deployments.json"
        print_message "$GREEN" "✓ Deployment history initialized"
    fi

    # Step 8: Perform test run
    print_message "$YELLOW" "Performing test run..."

    # Source common library to test
    if source "$SCRIPT_DIR/lib/common.sh" 2>/dev/null; then
        print_message "$GREEN" "✓ Libraries loaded successfully"
    else
        print_message "$RED" "✗ Failed to load libraries"
        exit 1
    fi

    # Step 9: Generate configuration report
    print_header "Configuration Summary"

    echo "Deployment System: Initialized"
    echo "Location: $SCRIPT_DIR"
    echo "Project Root: $PROJECT_ROOT"
    echo ""
    echo "Available Environments:"
    for env_file in "$SCRIPT_DIR"/environments/*.env; do
        if [[ -f "$env_file" ]]; then
            echo "  - $(basename "$env_file" .env)"
        fi
    done
    echo ""
    echo "Platform Support:"
    echo "  - Android: $(command -v "$PROJECT_ROOT/android/gradlew" &> /dev/null && echo "Ready" || echo "Not configured")"
    echo "  - iOS: $(command -v xcodebuild &> /dev/null && echo "Ready" || echo "Not available")"
    echo ""

    print_header "Next Steps"

    cat << 'EOF'
1. Configure secrets for your environment:
   cp secrets/example.env secrets/development.env
   # Edit secrets/development.env with actual values

2. Set up Android signing (if deploying Android):
   # Place your keystore in a secure location
   # Update ANDROID_KEYSTORE_PATH in environment config

3. Set up iOS signing (if deploying iOS):
   # Configure certificates in Xcode
   # Update iOS provisioning profiles

4. Test deployment (dry run):
   DRY_RUN=true ./deploy_qual.sh android

5. Perform first deployment:
   ./deploy_qual.sh android

For more information, see README.md
EOF

    print_header "Initialization Complete"

    print_message "$GREEN" "✅ SmilePile Deployment System is ready to use!"
    print_message "$BLUE" "Run './deploy_qual.sh -h' for usage information"
}

# Run main function
main "$@"