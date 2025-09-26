#!/bin/bash
# ============================================================================
# SmilePile Deployment System - Environment Manager
# ============================================================================
# Handles environment configuration, secrets, and feature flags
# No external dependencies - uses only shell and standard Unix tools

set -euo pipefail

# Source common library
if [[ -z "${LIB_DIR:-}" ]]; then
    LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
source "${LIB_DIR}/common.sh"

# ============================================================================
# Environment Configuration
# ============================================================================

# Available environments
if [[ -z "${ENVIRONMENTS:-}" ]]; then
    readonly ENVIRONMENTS=("development" "quality" "staging" "production")
fi

# Environment detection
detect_environment() {
    # Check explicit environment variable
    if [[ -n "${DEPLOY_ENV:-}" ]]; then
        echo "$DEPLOY_ENV"
        return
    fi

    # Check CI environment
    if [[ "${CI:-false}" == "true" ]]; then
        if [[ "${GITHUB_REF:-}" == "refs/heads/main" ]]; then
            echo "production"
        elif [[ "${GITHUB_REF:-}" == "refs/heads/staging" ]]; then
            echo "staging"
        else
            echo "quality"
        fi
        return
    fi

    # Check git branch
    local branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    case "$branch" in
        main|master)
            echo "production"
            ;;
        staging)
            echo "staging"
            ;;
        develop|development)
            echo "quality"
            ;;
        *)
            echo "development"
            ;;
    esac
}

# Load environment configuration
load_environment() {
    local env_name=${1:-$(detect_environment)}
    local env_file="${DEPLOY_ROOT}/environments/${env_name}.env"
    local secrets_file="${DEPLOY_ROOT}/secrets/${env_name}.env"

    log INFO "Loading environment: $env_name"

    # Load base configuration
    if [[ -f "${DEPLOY_ROOT}/environments/base.env" ]]; then
        log DEBUG "Loading base configuration"
        set -a
        source "${DEPLOY_ROOT}/environments/base.env"
        set +a
    fi

    # Load environment-specific configuration
    if [[ -f "$env_file" ]]; then
        log DEBUG "Loading environment configuration: $env_file"
        set -a
        source "$env_file"
        set +a
    else
        log WARN "Environment file not found: $env_file"
    fi

    # Load secrets (if available)
    if [[ -f "$secrets_file" ]]; then
        log DEBUG "Loading secrets from: $secrets_file"
        set -a
        source "$secrets_file"
        set +a
    fi

    # Set environment name
    export DEPLOY_ENVIRONMENT="$env_name"

    # Validate required variables
    validate_environment_vars "$env_name"
}

# Validate environment variables
validate_environment_vars() {
    local env_name=$1
    local missing_vars=()

    # Define required variables per environment
    local required_vars=()

    case "$env_name" in
        production)
            required_vars=(
                "ANDROID_KEYSTORE_PATH"
                "ANDROID_KEYSTORE_PASSWORD"
                "ANDROID_KEY_ALIAS"
                "IOS_CERTIFICATE_PATH"
                "IOS_PROVISIONING_PROFILE"
                "APP_VERSION"
                "BUILD_NUMBER"
            )
            ;;
        staging|quality)
            required_vars=(
                "APP_VERSION"
                "BUILD_NUMBER"
            )
            ;;
    esac

    # Check for missing variables
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            missing_vars+=("$var")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log ERROR "Missing required environment variables for $env_name:"
        for var in "${missing_vars[@]}"; do
            log ERROR "  - $var"
        done
        return 1
    fi

    log INFO "Environment validation successful"
}

# ============================================================================
# Secrets Management
# ============================================================================

# Encrypt a file or value
encrypt_secret() {
    local input=$1
    local output=${2:-}
    local password=${DEPLOY_SECRET_KEY:-}

    if [[ -z "$password" ]]; then
        log ERROR "DEPLOY_SECRET_KEY not set"
        return 1
    fi

    if [[ -f "$input" ]]; then
        # Encrypt file
        if [[ -n "$output" ]]; then
            openssl enc -aes-256-cbc -salt -pbkdf2 -in "$input" -out "$output" -pass pass:"$password"
            log INFO "File encrypted: $output"
        else
            openssl enc -aes-256-cbc -salt -pbkdf2 -in "$input" -pass pass:"$password"
        fi
    else
        # Encrypt string
        echo "$input" | openssl enc -aes-256-cbc -salt -pbkdf2 -pass pass:"$password" -base64
    fi
}

# Decrypt a file or value
decrypt_secret() {
    local input=$1
    local output=${2:-}
    local password=${DEPLOY_SECRET_KEY:-}

    if [[ -z "$password" ]]; then
        log ERROR "DEPLOY_SECRET_KEY not set"
        return 1
    fi

    if [[ -f "$input" ]]; then
        # Decrypt file
        if [[ -n "$output" ]]; then
            openssl enc -aes-256-cbc -d -pbkdf2 -in "$input" -out "$output" -pass pass:"$password"
            log INFO "File decrypted: $output"
        else
            openssl enc -aes-256-cbc -d -pbkdf2 -in "$input" -pass pass:"$password"
        fi
    else
        # Decrypt string
        echo "$input" | base64 -d | openssl enc -aes-256-cbc -d -pbkdf2 -pass pass:"$password"
    fi
}

# Initialize secrets for an environment
init_secrets() {
    local env_name=$1
    local secrets_file="${DEPLOY_ROOT}/secrets/${env_name}.env"

    log INFO "Initializing secrets for $env_name environment"

    # Create secrets directory with secure permissions
    mkdir -p "${DEPLOY_ROOT}/secrets"
    chmod 700 "${DEPLOY_ROOT}/secrets"

    # Create template if doesn't exist
    if [[ ! -f "$secrets_file" ]]; then
        cat > "$secrets_file" << 'EOF'
# ============================================================================
# Secrets for environment - DO NOT COMMIT
# ============================================================================

# Android signing
ANDROID_KEYSTORE_PASSWORD=""
ANDROID_KEY_PASSWORD=""

# iOS signing
IOS_CERTIFICATE_PASSWORD=""
IOS_PROVISIONING_PASSWORD=""

# API Keys
API_KEY_PRODUCTION=""
FIREBASE_API_KEY=""
ANALYTICS_KEY=""

# Notification services
SLACK_WEBHOOK_URL=""
EMAIL_SMTP_PASSWORD=""

# Cloud services
AWS_ACCESS_KEY_ID=""
AWS_SECRET_ACCESS_KEY=""
GCP_SERVICE_ACCOUNT_KEY=""

# Other sensitive data
DATABASE_PASSWORD=""
ENCRYPTION_KEY=""
EOF

        chmod 600 "$secrets_file"
        log INFO "Created secrets template: $secrets_file"
        log WARN "Please fill in the required secrets"
    fi
}

# ============================================================================
# Feature Flags
# ============================================================================

# Check if feature is enabled
is_feature_enabled() {
    local feature=$1
    local env_var="FEATURE_${feature^^}"

    [[ "${!env_var:-false}" == "true" ]]
}

# Set feature flag
set_feature_flag() {
    local feature=$1
    local value=$2
    local env_var="FEATURE_${feature^^}"

    export "$env_var=$value"
    log INFO "Feature flag $feature set to $value"
}

# List all feature flags
list_feature_flags() {
    log INFO "Current feature flags:"
    env | grep "^FEATURE_" | while IFS='=' read -r key value; do
        echo "  ${key#FEATURE_} = $value"
    done
}

# ============================================================================
# Environment Templates
# ============================================================================

# Create environment template
create_environment_template() {
    local env_name=$1
    local env_file="${DEPLOY_ROOT}/environments/${env_name}.env"

    mkdir -p "${DEPLOY_ROOT}/environments"

    if [[ -f "$env_file" ]]; then
        log WARN "Environment file already exists: $env_file"
        return 1
    fi

    cat > "$env_file" << EOF
# ============================================================================
# SmilePile Environment Configuration: ${env_name}
# ============================================================================
# Generated: $(date)

# Environment identification
ENVIRONMENT_NAME="${env_name}"
ENVIRONMENT_TYPE="${env_name}"

# Application configuration
APP_NAME="SmilePile"
APP_VERSION="1.0.0"
BUILD_NUMBER="1"

# API endpoints
API_BASE_URL=""
API_TIMEOUT="30"

# Android configuration
ANDROID_PACKAGE_NAME="com.smilepile.app"
ANDROID_MIN_SDK="24"
ANDROID_TARGET_SDK="33"
ANDROID_BUILD_TYPE="release"
ANDROID_FLAVOR="${env_name}"

# iOS configuration
IOS_BUNDLE_ID="com.smilepile.app"
IOS_DEPLOYMENT_TARGET="14.0"
IOS_BUILD_CONFIGURATION="Release"
IOS_SCHEME="SmilePile"

# Build settings
ENABLE_PROGUARD="true"
ENABLE_MINIFICATION="true"
ENABLE_RESOURCE_SHRINKING="true"
ENABLE_BITCODE="false"

# Feature flags
FEATURE_ANALYTICS="true"
FEATURE_CRASH_REPORTING="true"
FEATURE_REMOTE_CONFIG="false"
FEATURE_PUSH_NOTIFICATIONS="true"
FEATURE_IN_APP_PURCHASES="false"

# Deployment settings
DEPLOY_TIMEOUT="600"
DEPLOY_RETRY_COUNT="3"
DEPLOY_ROLLBACK_ENABLED="true"

# Monitoring
ENABLE_MONITORING="true"
MONITORING_SAMPLE_RATE="1.0"
LOG_LEVEL="INFO"

# Third-party services
FIREBASE_PROJECT_ID=""
SENTRY_DSN=""
MIXPANEL_TOKEN=""

# Storage
ARTIFACT_STORAGE_PATH="${DEPLOY_ROOT}/artifacts/${env_name}"
BACKUP_RETENTION_DAYS="30"

# Notification settings
NOTIFY_ON_SUCCESS="true"
NOTIFY_ON_FAILURE="true"
NOTIFICATION_CHANNELS="slack,email"

# Custom environment variables
# Add environment-specific variables below
EOF

    chmod 644 "$env_file"
    log INFO "Created environment template: $env_file"
}

# ============================================================================
# Environment Utilities
# ============================================================================

# Print environment info
print_environment_info() {
    local env_name=${1:-$(detect_environment)}

    print_header "Environment Information"

    echo "Environment: $env_name"
    echo "User: $USER"
    echo "Hostname: $(hostname)"
    echo "Date: $(date)"
    echo "Git Branch: $(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "Git Commit: $(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
    echo ""
    echo "Key Variables:"
    echo "  APP_VERSION: ${APP_VERSION:-not set}"
    echo "  BUILD_NUMBER: ${BUILD_NUMBER:-not set}"
    echo "  API_BASE_URL: ${API_BASE_URL:-not set}"
    echo ""
}

# Compare environments
compare_environments() {
    local env1=$1
    local env2=$2

    log INFO "Comparing environments: $env1 vs $env2"

    local file1="${DEPLOY_ROOT}/environments/${env1}.env"
    local file2="${DEPLOY_ROOT}/environments/${env2}.env"

    if [[ ! -f "$file1" ]] || [[ ! -f "$file2" ]]; then
        log ERROR "Environment files not found"
        return 1
    fi

    # Show differences
    diff -u "$file1" "$file2" || true
}

# Export environment to JSON
export_environment_json() {
    local env_name=${1:-$(detect_environment)}
    local output_file=${2:-"${DEPLOY_ROOT}/temp/env_${env_name}.json"}

    log INFO "Exporting environment $env_name to JSON"

    # Load environment
    load_environment "$env_name"

    # Export to JSON (excluding secrets)
    (
        echo "{"
        env | grep -E "^(APP_|FEATURE_|DEPLOY_|ANDROID_|IOS_)" | grep -v -E "(PASSWORD|KEY|TOKEN|SECRET)" | while IFS='=' read -r key value; do
            echo "  \"$key\": \"$value\","
        done | sed '$ s/,$//'
        echo "}"
    ) > "$output_file"

    log INFO "Environment exported to: $output_file"
}

# ============================================================================
# Export Functions
# ============================================================================

export -f detect_environment
export -f load_environment
export -f validate_environment_vars
export -f encrypt_secret
export -f decrypt_secret
export -f init_secrets
export -f is_feature_enabled
export -f set_feature_flag
export -f list_feature_flags
export -f create_environment_template
export -f print_environment_info
export -f compare_environments
export -f export_environment_json