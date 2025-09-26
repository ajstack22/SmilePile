#!/bin/bash
# Deployment Utilities Module
# Shared functions for deployment scripts

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Validate required tools
validate_tools() {
    local tools=("$@")
    local missing_tools=()

    for tool in "${tools[@]}"; do
        if ! command_exists "$tool"; then
            missing_tools+=("$tool")
        fi
    done

    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        return 1
    fi

    log_success "All required tools are installed"
    return 0
}

# Load environment configuration
load_environment() {
    local env_name="$1"
    local env_file="${DEPLOY_ROOT}/environments/.env.${env_name}"

    if [ ! -f "$env_file" ]; then
        log_error "Environment file not found: $env_file"
        return 1
    fi

    # Source environment file
    set -a
    source "$env_file"
    set +a

    log_info "Loaded environment: $env_name"
    return 0
}

# Load secrets from secure storage
load_secrets() {
    local env_name="$1"
    local secrets_file="${DEPLOY_ROOT}/environments/.secrets.${env_name}"

    # Check if running in CI/CD environment
    if [ -n "${CI:-}" ]; then
        log_info "Running in CI environment, loading secrets from environment variables"
        return 0
    fi

    # Load local secrets file if exists (development only)
    if [ -f "$secrets_file" ]; then
        log_warning "Loading secrets from local file (development mode)"
        set -a
        source "$secrets_file"
        set +a
    else
        log_warning "No secrets file found. Ensure secrets are set in environment variables"
    fi

    return 0
}

# Validate environment variables
validate_env_vars() {
    local required_vars=("$@")
    local missing_vars=()

    for var in "${required_vars[@]}"; do
        if [ -z "${!var:-}" ]; then
            missing_vars+=("$var")
        fi
    done

    if [ ${#missing_vars[@]} -gt 0 ]; then
        log_error "Missing required environment variables: ${missing_vars[*]}"
        return 1
    fi

    log_success "All required environment variables are set"
    return 0
}

# Run command with retry logic
run_with_retry() {
    local max_attempts="${1:-3}"
    local delay="${2:-5}"
    shift 2
    local command=("$@")
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        log_info "Attempt $attempt of $max_attempts: ${command[*]}"

        if "${command[@]}"; then
            log_success "Command succeeded"
            return 0
        fi

        if [ $attempt -lt $max_attempts ]; then
            log_warning "Command failed, retrying in ${delay} seconds..."
            sleep "$delay"
        fi

        ((attempt++))
    done

    log_error "Command failed after $max_attempts attempts"
    return 1
}

# Create backup of current deployment
create_backup() {
    local app_name="$1"
    local backup_dir="${BACKUP_DIR:-/tmp/deploy_backups}"
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local backup_path="${backup_dir}/${app_name}_${timestamp}"

    mkdir -p "$backup_dir"

    log_info "Creating backup at $backup_path"
    # Implementation depends on platform
    return 0
}

# Restore from backup
restore_backup() {
    local backup_path="$1"

    if [ ! -d "$backup_path" ]; then
        log_error "Backup not found: $backup_path"
        return 1
    fi

    log_info "Restoring from backup: $backup_path"
    # Implementation depends on platform
    return 0
}

# Send deployment notification
send_notification() {
    local status="$1"
    local message="$2"
    local env_name="${ENVIRONMENT:-unknown}"

    # Slack notification if webhook is configured
    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        local color="good"
        [ "$status" = "ERROR" ] && color="danger"
        [ "$status" = "WARNING" ] && color="warning"

        local payload=$(cat <<EOF
{
    "attachments": [{
        "color": "$color",
        "title": "Deployment Notification - $env_name",
        "text": "$message",
        "timestamp": "$(date '+%s')"
    }]
}
EOF
        )

        curl -X POST -H 'Content-Type: application/json' \
             -d "$payload" \
             "$SLACK_WEBHOOK_URL" 2>/dev/null || true
    fi

    # Log the notification
    case "$status" in
        SUCCESS) log_success "$message" ;;
        WARNING) log_warning "$message" ;;
        ERROR) log_error "$message" ;;
        *) log_info "$message" ;;
    esac
}

# Cleanup function for trap
cleanup() {
    local exit_code=$?

    if [ $exit_code -ne 0 ]; then
        log_error "Deployment failed with exit code: $exit_code"
        send_notification "ERROR" "Deployment failed for ${APP_NAME:-unknown}"
    fi

    # Cleanup temporary files
    if [ -n "${TEMP_DIR:-}" ] && [ -d "$TEMP_DIR" ]; then
        rm -rf "$TEMP_DIR"
    fi

    exit $exit_code
}

# Setup trap for cleanup
trap cleanup EXIT INT TERM

# Export utility functions
export -f log_info log_success log_warning log_error
export -f command_exists validate_tools
export -f load_environment load_secrets validate_env_vars
export -f run_with_retry create_backup restore_backup
export -f send_notification cleanup