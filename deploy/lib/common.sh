#!/bin/bash
# ============================================================================
# SmilePile Deployment System - Common Library
# ============================================================================
# Shared utilities and functions for all deployment scripts
# Compatible with macOS and Linux, no external dependencies

set -euo pipefail

# ============================================================================
# Global Configuration
# ============================================================================

# Deployment system version
if [[ -z "${DEPLOY_SYSTEM_VERSION:-}" ]]; then
    readonly DEPLOY_SYSTEM_VERSION="1.0.0"
fi

# Script identification
if [[ -z "${SCRIPT_NAME:-}" ]]; then
    readonly SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
fi
if [[ -z "${SCRIPT_DIR:-}" ]]; then
    readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
if [[ -z "${DEPLOY_ROOT:-}" ]]; then
    readonly DEPLOY_ROOT="$(dirname "$SCRIPT_DIR")"
fi
if [[ -z "${PROJECT_ROOT:-}" ]]; then
    readonly PROJECT_ROOT="$(dirname "$DEPLOY_ROOT")"
fi

# Timestamps and logging
if [[ -z "${TIMESTAMP:-}" ]]; then
    readonly TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
fi
if [[ -z "${DATE_STAMP:-}" ]]; then
    readonly DATE_STAMP=$(date '+%Y-%m-%d')
fi
if [[ -z "${LOG_DIR:-}" ]]; then
    readonly LOG_DIR="${DEPLOY_ROOT}/logs"
fi
if [[ -z "${DEPLOYMENT_HISTORY:-}" ]]; then
    readonly DEPLOYMENT_HISTORY="${DEPLOY_ROOT}/history/deployments.json"
fi

# Platform detection
if [[ -z "${OS_TYPE:-}" ]]; then
    readonly OS_TYPE="$(uname -s)"
fi
if [[ -z "${ARCH_TYPE:-}" ]]; then
    readonly ARCH_TYPE="$(uname -m)"
fi

# Color codes for output (works on both macOS and Linux)
if [[ -z "${RED:-}" ]]; then
    readonly RED='\033[0;31m'
    readonly GREEN='\033[0;32m'
    readonly YELLOW='\033[1;33m'
    readonly BLUE='\033[0;34m'
    readonly MAGENTA='\033[0;35m'
    readonly CYAN='\033[0;36m'
    readonly WHITE='\033[1;37m'
    readonly NC='\033[0m' # No Color
fi

# ============================================================================
# Utility Functions
# ============================================================================

# Initialize deployment system
init_deployment_system() {
    # Create required directories
    mkdir -p "$LOG_DIR"
    mkdir -p "${DEPLOY_ROOT}/history"
    mkdir -p "${DEPLOY_ROOT}/backups"
    mkdir -p "${DEPLOY_ROOT}/artifacts"
    mkdir -p "${DEPLOY_ROOT}/temp"
    mkdir -p "${DEPLOY_ROOT}/secrets"

    # Set secure permissions on secrets directory
    chmod 700 "${DEPLOY_ROOT}/secrets"
}

# Enhanced logging with levels
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        ERROR)
            echo -e "${RED}[ERROR]${NC} [${timestamp}] ${message}" >&2
            echo "[ERROR] [${timestamp}] ${message}" >> "${LOG_FILE:-/dev/null}"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} [${timestamp}] ${message}"
            echo "[WARN] [${timestamp}] ${message}" >> "${LOG_FILE:-/dev/null}"
            ;;
        INFO)
            echo -e "${GREEN}[INFO]${NC} [${timestamp}] ${message}"
            echo "[INFO] [${timestamp}] ${message}" >> "${LOG_FILE:-/dev/null}"
            ;;
        DEBUG)
            if [[ "${DEBUG_MODE:-false}" == "true" ]]; then
                echo -e "${CYAN}[DEBUG]${NC} [${timestamp}] ${message}"
                echo "[DEBUG] [${timestamp}] ${message}" >> "${LOG_FILE:-/dev/null}"
            fi
            ;;
        *)
            echo "[${timestamp}] ${message}"
            echo "[${timestamp}] ${message}" >> "${LOG_FILE:-/dev/null}"
            ;;
    esac
}

# Print section headers
print_header() {
    local title="$1"
    local width=80
    local padding=$(( (width - ${#title} - 2) / 2 ))

    echo ""
    echo "$(printf '=%.0s' {1..80})"
    printf "%*s %s %*s\n" $padding "" "$title" $padding ""
    echo "$(printf '=%.0s' {1..80})"
    echo ""
}

# Print step with status
print_step() {
    local step_num=$1
    local step_desc=$2
    echo -e "${BLUE}Step ${step_num}:${NC} ${step_desc}"
}

# Check command existence
require_command() {
    local cmd=$1
    local install_hint=${2:-"Please install $cmd"}

    if ! command -v "$cmd" &> /dev/null; then
        log ERROR "Required command '$cmd' not found. ${install_hint}"
        exit 1
    fi
}

# Check multiple commands
require_commands() {
    local commands=("$@")
    for cmd in "${commands[@]}"; do
        require_command "$cmd"
    done
}

# Verify environment setup
verify_environment() {
    local env_name=$1

    log INFO "Verifying ${env_name} environment setup..."

    # Check required tools based on platform
    case "$OS_TYPE" in
        Darwin)
            require_commands git curl jq openssl shasum
            ;;
        Linux)
            require_commands git curl jq openssl sha256sum
            ;;
        *)
            log ERROR "Unsupported OS: $OS_TYPE"
            exit 1
            ;;
    esac
}

# Calculate checksum (cross-platform)
calculate_checksum() {
    local file=$1

    if [[ "$OS_TYPE" == "Darwin" ]]; then
        shasum -a 256 "$file" | awk '{print $1}'
    else
        sha256sum "$file" | awk '{print $1}'
    fi
}

# Verify file integrity
verify_checksum() {
    local file=$1
    local expected_checksum=$2

    local actual_checksum=$(calculate_checksum "$file")

    if [[ "$actual_checksum" != "$expected_checksum" ]]; then
        log ERROR "Checksum verification failed for $file"
        log ERROR "Expected: $expected_checksum"
        log ERROR "Actual: $actual_checksum"
        return 1
    fi

    log INFO "Checksum verified for $file"
    return 0
}

# Create backup
create_backup() {
    local source=$1
    local backup_name=${2:-"backup"}
    local backup_dir="${DEPLOY_ROOT}/backups/${backup_name}_${TIMESTAMP}"

    log INFO "Creating backup of $source to $backup_dir"
    mkdir -p "$backup_dir"

    if [[ -d "$source" ]]; then
        cp -r "$source" "$backup_dir/"
    else
        cp "$source" "$backup_dir/"
    fi

    # Create backup manifest
    cat > "$backup_dir/manifest.json" << EOF
{
    "timestamp": "${TIMESTAMP}",
    "source": "${source}",
    "checksum": "$(calculate_checksum "$source" 2>/dev/null || echo 'directory')",
    "created_by": "${USER}",
    "hostname": "$(hostname)"
}
EOF

    log INFO "Backup created: $backup_dir"
    echo "$backup_dir"
}

# Restore from backup
restore_backup() {
    local backup_dir=$1
    local target=$2

    log INFO "Restoring from backup: $backup_dir"

    if [[ ! -f "$backup_dir/manifest.json" ]]; then
        log ERROR "Invalid backup: missing manifest.json"
        return 1
    fi

    # Read manifest
    local source=$(jq -r '.source' "$backup_dir/manifest.json")

    # Perform restore
    if [[ -d "$backup_dir/$(basename "$source")" ]]; then
        cp -r "$backup_dir/$(basename "$source")" "$target"
    else
        cp "$backup_dir/$(basename "$source")" "$target"
    fi

    log INFO "Restore completed"
}

# Execute with retry logic
retry_command() {
    local max_attempts=${1:-3}
    local delay=${2:-5}
    shift 2
    local command=("$@")
    local attempt=1

    while (( attempt <= max_attempts )); do
        log INFO "Attempt $attempt of $max_attempts: ${command[*]}"

        if "${command[@]}"; then
            log INFO "Command succeeded on attempt $attempt"
            return 0
        fi

        if (( attempt < max_attempts )); then
            log WARN "Command failed, retrying in ${delay}s..."
            sleep "$delay"
        fi

        (( attempt++ ))
    done

    log ERROR "Command failed after $max_attempts attempts"
    return 1
}

# Record deployment event
record_deployment() {
    local platform=$1
    local environment=$2
    local status=$3
    local details=${4:-""}

    local deployment_record=$(cat << EOF
{
    "timestamp": "${TIMESTAMP}",
    "platform": "${platform}",
    "environment": "${environment}",
    "status": "${status}",
    "details": "${details}",
    "user": "${USER}",
    "hostname": "$(hostname)",
    "git_commit": "$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || echo 'unknown')",
    "git_branch": "$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
}
EOF
)

    # Ensure history directory exists
    mkdir -p "$(dirname "$DEPLOYMENT_HISTORY")"

    # Append to history file
    if [[ -f "$DEPLOYMENT_HISTORY" ]]; then
        echo "," >> "$DEPLOYMENT_HISTORY"
    else
        echo "[" > "$DEPLOYMENT_HISTORY"
    fi

    echo "$deployment_record" >> "$DEPLOYMENT_HISTORY"
}

# Get last deployment info
get_last_deployment() {
    local platform=$1
    local environment=$2

    if [[ ! -f "$DEPLOYMENT_HISTORY" ]]; then
        echo "{}"
        return
    fi

    # This is simplified - in production, use proper JSON parsing
    grep -E "\"platform\": \"${platform}\".*\"environment\": \"${environment}\"" "$DEPLOYMENT_HISTORY" | tail -1 || echo "{}"
}

# Confirm action (interactive)
confirm_action() {
    local message=$1
    local default=${2:-n}

    # Skip confirmation in CI/non-interactive mode
    if [[ "${CI:-false}" == "true" ]] || [[ ! -t 0 ]]; then
        log INFO "Auto-confirming in CI/non-interactive mode"
        return 0
    fi

    local prompt
    if [[ "$default" == "y" ]]; then
        prompt="$message [Y/n]: "
    else
        prompt="$message [y/N]: "
    fi

    read -r -p "$prompt" response

    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        [nN][oO]|[nN])
            return 1
            ;;
        "")
            if [[ "$default" == "y" ]]; then
                return 0
            else
                return 1
            fi
            ;;
        *)
            return 1
            ;;
    esac
}

# Send notification (pluggable)
send_notification() {
    local title=$1
    local message=$2
    local priority=${3:-info}

    # Log the notification
    log INFO "Notification: $title - $message"

    # Platform-specific notifications
    if [[ "$OS_TYPE" == "Darwin" ]]; then
        # macOS notification
        osascript -e "display notification \"$message\" with title \"$title\"" 2>/dev/null || true
    fi

    # Slack notification (if configured)
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        send_slack_notification "$title" "$message" "$priority"
    fi

    # Email notification (if configured)
    if [[ -n "${EMAIL_RECIPIENT:-}" ]]; then
        send_email_notification "$title" "$message" "$priority"
    fi
}

# Slack notification
send_slack_notification() {
    local title=$1
    local message=$2
    local priority=$3

    local color
    case $priority in
        error) color="danger" ;;
        warning) color="warning" ;;
        success) color="good" ;;
        *) color="#808080" ;;
    esac

    local payload=$(cat << EOF
{
    "attachments": [{
        "color": "${color}",
        "title": "${title}",
        "text": "${message}",
        "footer": "SmilePile Deployment System",
        "ts": $(date +%s)
    }]
}
EOF
)

    curl -X POST -H 'Content-type: application/json' \
        --data "$payload" \
        "${SLACK_WEBHOOK_URL}" &>/dev/null || true
}

# Email notification (simplified)
send_email_notification() {
    local title=$1
    local message=$2
    local priority=$3

    if command -v mail &> /dev/null; then
        echo "$message" | mail -s "[SmilePile Deploy] $title" "${EMAIL_RECIPIENT}" || true
    fi
}

# Cleanup temporary files
cleanup() {
    log INFO "Cleaning up temporary files..."
    rm -rf "${DEPLOY_ROOT}/temp/"*
}

# Trap cleanup on exit
trap cleanup EXIT

# Export functions for use in other scripts
export -f log
export -f print_header
export -f print_step
export -f require_command
export -f require_commands
export -f verify_environment
export -f calculate_checksum
export -f verify_checksum
export -f create_backup
export -f restore_backup
export -f retry_command
export -f record_deployment
export -f get_last_deployment
export -f confirm_action
export -f send_notification
export -f cleanup