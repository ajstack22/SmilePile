# Wave 6 Phase 4a: Security Review of QUAL Tier Changes

**Review Date:** October 15, 2025
**Reviewer:** Claude (Sonnet 4.5) - Security Agent
**Scope:** Wave 6 QUAL tier validation planned changes
**Status:** APPROVED WITH CONDITIONS

---

## Executive Summary

**OVERALL SECURITY POSTURE: MEDIUM RISK**

The planned changes for Wave 6 QUAL tier validation have been reviewed from a security perspective. The deployment script and infrastructure demonstrate good security practices overall, with proper secrets management, file permissions, and credential protection. However, several command injection vulnerabilities and security improvements have been identified that should be addressed.

**Critical Finding:** One CRITICAL command injection vulnerability in iOS simulator detection logic must be fixed before implementation.

**Key Strengths:**
- Excellent secrets management with 600 permissions on keystore files
- Comprehensive .gitignore coverage for sensitive files
- No hardcoded credentials in deployment scripts
- Proper encryption support for secrets (AES-256-CBC)
- Good separation of environments and configuration

**Key Concerns:**
- Command injection vulnerability in iOS simulator name handling
- Missing input validation on user-controlled variables
- jq dependency introduces supply chain risk (low severity)
- Potential for log file exposure of sensitive information

---

## Critical Issues (MUST FIX BEFORE IMPLEMENTATION)

### CRITICAL-1: Command Injection in iOS Simulator Boot Logic

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:510`

**Current Code (Line 510):**
```bash
xcrun simctl boot "iPhone 16" 2>/dev/null || true
```

**Vulnerability:**
The hardcoded simulator name "iPhone 16" is safe, but the planned dynamic detection code introduces command injection risk:

**Planned Code (Technical Plan lines 305-310):**
```bash
# Try to boot a simulator (try multiple models)
for sim_name in "iPhone 15" "iPhone 14" "iPhone 16"; do
    if xcrun simctl boot "$sim_name" 2>/dev/null; then
        log INFO "Booted simulator: $sim_name"
        break
    fi
done
```

**Planned Detection Code (Technical Plan lines 236-274):**
```bash
detect_simulator() {
    # First, try to get a booted simulator
    local booted_sim=$(xcrun simctl list devices | grep "Booted" | head -1 | sed -E 's/.*\((.*)\).*/\1/')
    if [[ -n "$booted_sim" ]]; then
        echo "platform=iOS Simulator,id=${booted_sim}"
        return 0
    fi

    # Check for environment variable override
    if [[ -n "${IOS_SIMULATOR_NAME:-}" ]]; then
        echo "platform=iOS Simulator,name=${IOS_SIMULATOR_NAME},OS=latest"
        return 0
    fi
    # ... more code
}
```

**Attack Vector:**
An attacker who can control the `IOS_SIMULATOR_NAME` environment variable can inject arbitrary commands:

```bash
IOS_SIMULATOR_NAME='iPhone 15"; rm -rf /; echo "' ./deploy/deploy_qual.sh ios
```

This would result in:
```bash
echo "platform=iOS Simulator,name=iPhone 15"; rm -rf /; echo ",OS=latest"
```

**Impact:** CRITICAL
- Arbitrary command execution with user privileges
- Potential for data destruction or system compromise
- Could be exploited via CI/CD pipeline environment variable injection

**Fix Required:**
1. Add input validation to sanitize `IOS_SIMULATOR_NAME`
2. Use array-based command execution to prevent injection
3. Validate simulator names against known safe values

**Recommended Secure Implementation:**
```bash
detect_simulator() {
    # Validate environment variable if provided
    if [[ -n "${IOS_SIMULATOR_NAME:-}" ]]; then
        # Whitelist validation: only allow alphanumeric, spaces, and hyphens
        if [[ "$IOS_SIMULATOR_NAME" =~ ^[a-zA-Z0-9\ \-]+$ ]]; then
            # Additional check: verify simulator actually exists
            if xcrun simctl list devices | grep -qF "$IOS_SIMULATOR_NAME"; then
                echo "platform=iOS Simulator,name=${IOS_SIMULATOR_NAME},OS=latest"
                return 0
            else
                log WARN "Simulator '$IOS_SIMULATOR_NAME' not found, using fallback"
            fi
        else
            log ERROR "Invalid IOS_SIMULATOR_NAME: contains unsafe characters"
            return 1
        fi
    fi

    # Rest of function...
}
```

**Validation Command:**
```bash
# Test injection attempt (should be blocked)
IOS_SIMULATOR_NAME='iPhone 15"; echo "INJECTED"' ./ios/scripts/run-tier-tests.sh tier1 --dry-run
# Should ERROR and not execute the injection
```

---

## High Priority Issues (SHOULD FIX)

### HIGH-1: Missing Input Validation on Test Task Names

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:172, 200, 229`

**Current Code:**
```bash
./gradlew app:testQualDebugTier1Critical 2>&1 | tee "$tier1_output"
```

**Planned Code:**
```bash
./gradlew app:testTier1Critical 2>&1 | tee "$tier1_output"
```

**Issue:**
While the task names are currently hardcoded (which is safe), there's no validation that prevents future modifications from introducing command injection via the `$tier1_output` variable.

**Attack Vector:**
If `tier1_output` is ever derived from user input:
```bash
tier1_output="/tmp/tier1-output.txt; rm -rf /; echo"
```

**Impact:** HIGH
- Command injection if variable source changes
- File path traversal potential

**Fix Required:**
Add validation for output file paths:

```bash
# Function to validate output file path
validate_output_path() {
    local path="$1"

    # Must be in /tmp or project directory
    if [[ ! "$path" =~ ^/tmp/ ]] && [[ ! "$path" =~ ^"$PROJECT_ROOT"/ ]]; then
        log ERROR "Invalid output path: $path"
        return 1
    fi

    # No special characters except underscore, hyphen, dot, slash
    if [[ "$path" =~ [^a-zA-Z0-9/_.\-] ]]; then
        log ERROR "Output path contains unsafe characters: $path"
        return 1
    fi

    return 0
}

# Usage:
local tier1_output="/tmp/tier1-android-output.txt"
validate_output_path "$tier1_output" || exit 1
./gradlew app:testTier1Critical 2>&1 | tee "$tier1_output"
```

---

### HIGH-2: jq Dependency Check - Supply Chain Risk

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:94` (planned addition)

**Planned Code:**
```bash
command -v jq >/dev/null 2>&1 || missing_tools+=("jq")
```

**Issue:**
Adding jq as a dependency introduces a supply chain attack vector. While jq is a well-known and trusted tool, the script doesn't verify:
1. jq binary integrity (checksum validation)
2. jq version compatibility
3. Whether jq has been tampered with

**Impact:** MEDIUM
- Compromised jq binary could manipulate JSON data
- Test failure tracking uses jq to parse failure lists
- Version inconsistencies could cause silent failures

**Fix Required:**
1. Add version check for jq
2. Optionally add checksum verification (advanced)
3. Provide clear installation instructions from trusted sources

**Recommended Implementation:**
```bash
check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check common tools
    command -v git >/dev/null 2>&1 || missing_tools+=("git")

    # Check jq with version validation
    if command -v jq >/dev/null 2>&1; then
        local jq_version=$(jq --version 2>&1 | grep -oE '[0-9]+\.[0-9]+')
        local jq_major=$(echo "$jq_version" | cut -d. -f1)
        local jq_minor=$(echo "$jq_version" | cut -d. -f2)

        # Require jq 1.5 or higher
        if [[ "$jq_major" -lt 1 ]] || { [[ "$jq_major" -eq 1 ]] && [[ "$jq_minor" -lt 5 ]]; }; then
            log ERROR "jq version $jq_version is too old. Requires jq >= 1.5"
            log INFO "Update with: brew upgrade jq"
            exit 1
        fi
        log INFO "jq version $jq_version detected"
    else
        missing_tools+=("jq")
    fi

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log ERROR "Missing required tools: ${missing_tools[*]}"
        log INFO ""
        log INFO "Installation instructions:"
        for tool in "${missing_tools[@]}"; do
            case "$tool" in
                jq)
                    log INFO "  jq (JSON processor):"
                    log INFO "    macOS:   brew install jq"
                    log INFO "    Linux:   apt-get install jq (Debian/Ubuntu)"
                    log INFO "             yum install jq (RHEL/CentOS)"
                    log INFO "    Verify:  https://stedolan.github.io/jq/"
                    ;;
            esac
        done
        exit 1
    fi

    log SUCCESS "All prerequisites met"
}
```

---

### HIGH-3: Potential for Sensitive Data in Log Files

**Location:** Multiple locations throughout `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`

**Current Code:**
```bash
exec 1> >(tee -a "$LOG_FILE")
exec 2>&1
```

**Issue:**
All stdout and stderr are redirected to log files, which could capture sensitive information:
- Environment variables containing passwords
- Gradle command output showing keystore paths
- Fastlane output potentially exposing credentials
- Test output containing API keys or tokens

**Current Log Location:** `${LOG_DIR}/deploy_${DEPLOYMENT_ID}.log`

**Impact:** MEDIUM
- Credentials could be exposed in log files
- Log files are not automatically cleaned up
- Log files don't have restricted permissions (default umask applies)

**Evidence of Risk:**
From `deploy/lib/android_deploy.sh:108-110`:
```bash
"-Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD}"
"-Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD:-$ANDROID_KEYSTORE_PASSWORD}"
```

These properties might appear in Gradle verbose output.

**Fix Required:**
1. Set restrictive permissions on log files
2. Add log sanitization function
3. Implement log rotation and cleanup
4. Document sensitive data handling in logs

**Recommended Implementation:**
```bash
# In deploy_qual.sh main() function, after LOG_FILE creation:

# Setup logging with secure permissions
mkdir -p "$LOG_DIR"
touch "$LOG_FILE"
chmod 600 "$LOG_FILE"  # Owner read/write only

# Create sanitized log for sharing
LOG_FILE_SANITIZED="${LOG_FILE%.log}-sanitized.log"

# Function to sanitize log output
sanitize_log() {
    sed -E \
        -e 's/(password|passwd|pwd|secret|token|key|apikey)=[^[:space:]&]+/\1=***REDACTED***/gi' \
        -e 's/(password|passwd|pwd|secret|token|key|apikey)":"[^"]+/\1":"***REDACTED***/gi' \
        -e 's/Authorization: Bearer [^[:space:]]+/Authorization: Bearer ***REDACTED***/gi' \
        "$LOG_FILE" > "$LOG_FILE_SANITIZED"
    chmod 644 "$LOG_FILE_SANITIZED"
}

# Trap to sanitize logs on exit
trap 'sanitize_log' EXIT

# Add to generate_summary:
echo "Logs:"
echo "  Full log (sensitive):      $LOG_FILE (permissions: 600)"
echo "  Sanitized log (shareable): $LOG_FILE_SANITIZED (permissions: 644)"
```

---

### HIGH-4: Android Test Task Names - Validation of Gradle Output

**Location:** `/Users/adamstack/SmilePile/scripts/test-failure-tracker.sh:35`

**Current Code:**
```bash
printf '%s\n' "${failures[@]}" | jq -R . | jq -s .
```

**Issue:**
The test failure tracker parses Gradle output without validating that the test output is legitimate. A compromised or malicious test could inject crafted output that manipulates the failure tracking system.

**Attack Vector:**
```bash
# Malicious test output could contain:
com.example.MaliciousTest > testHack FAILED
../../../../../../etc/passwd > content FAILED
```

This could cause the tracker to write files outside the project directory.

**Impact:** MEDIUM
- Path traversal via crafted test names
- Potential for unauthorized file creation
- Tech debt story creation with malicious content

**Fix Required:**
Add validation to test failure parsing:

```bash
parse_test_results() {
    local tier="$1"
    local test_output_file="$2"
    local failures=()

    # Extract failed test names from Gradle output
    while IFS= read -r line; do
        if [[ "$line" =~ ^(.+)\ \>\ (.+)\ FAILED$ ]]; then
            local test_class="${BASH_REMATCH[1]}"
            local test_method="${BASH_REMATCH[2]}"

            # SECURITY: Validate test class and method names
            # Must be valid Java/Kotlin identifiers (no path traversal)
            if [[ "$test_class" =~ ^[a-zA-Z0-9_.]+$ ]] && [[ "$test_method" =~ ^[a-zA-Z0-9_]+$ ]]; then
                failures+=("${test_class}:${test_method}")
            else
                echo "WARNING: Ignoring invalid test name: $test_class > $test_method" >&2
            fi
        fi
    done < "$test_output_file"

    # Return failures as JSON array
    printf '%s\n' "${failures[@]}" | jq -R . | jq -s .
}
```

---

## Medium Priority Issues (NICE TO HAVE)

### MEDIUM-1: Git Commit Messages May Contain Sensitive Information

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:564-583`

**Current Code:**
```bash
local commit_msg="${COMMIT_MESSAGE:-"qual: Deploy ${PLATFORM} - v${VERSION_NAME}"}"
```

**Issue:**
Custom commit messages via `COMMIT_MESSAGE` environment variable are not sanitized. An attacker with environment variable control could inject credentials into git history.

**Attack Vector:**
```bash
COMMIT_MESSAGE="qual: Deploy android - password123 - v1.0.0" ./deploy/deploy_qual.sh android
```

**Impact:** LOW
- Sensitive data could be committed to git history
- Git history is permanent (even after force push)
- Requires attacker to control environment variables

**Fix Required:**
Validate commit message or use only templated messages:

```bash
# Generate commit message with version
if [[ -n "$COMMIT_MESSAGE" ]]; then
    # Sanitize custom message
    local sanitized_msg=$(echo "$COMMIT_MESSAGE" | sed -E 's/(password|secret|token|key)[:=][^[:space:]]+/\1:***REDACTED***/gi')
    local commit_msg="$sanitized_msg"
else
    local commit_msg="qual: Deploy ${PLATFORM} - v${VERSION_NAME}"
fi
```

---

### MEDIUM-2: Secrets Directory World-Executable

**Location:** `/Users/adamstack/SmilePile/deploy/lib/common.sh:79-82`

**Current Code:**
```bash
mkdir -p "${DEPLOY_ROOT}/secrets"
chmod 700 "${DEPLOY_ROOT}/secrets"
```

**Issue:**
While the permissions are correctly set to 700 (owner only), there's no verification that:
1. The parent directory has appropriate permissions
2. Secrets files inside have 600 permissions
3. Backup files of secrets are also protected

**Current State:**
```bash
# From gitignore:
secrets/
```

Good: Secrets directory is gitignored.

**Impact:** LOW
- Secrets could be readable if directory permissions are changed
- Backup or temporary files might not have restricted permissions

**Fix Required:**
Add secrets file permission verification:

```bash
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

    # Verify and fix permissions on all secrets files
    find "${DEPLOY_ROOT}/secrets" -type f -exec chmod 600 {} \;

    # Check parent directory isn't world-readable
    local parent_perms=$(stat -f "%A" "${DEPLOY_ROOT}" 2>/dev/null || stat -c "%a" "${DEPLOY_ROOT}" 2>/dev/null)
    if [[ "${parent_perms: -1}" != "0" ]]; then
        log WARN "Deploy directory is world-accessible. Consider: chmod 750 ${DEPLOY_ROOT}"
    fi
}
```

---

### MEDIUM-3: iOS Simulator Detection - Regex Injection

**Location:** `/Users/adamstack/SmilePile/ios/scripts/run-tier-tests.sh:13` (planned change)

**Planned Code (Technical Plan line 241):**
```bash
local booted_sim=$(xcrun simctl list devices | grep "Booted" | head -1 | sed -E 's/.*\((.*)\).*/\1/')
```

**Issue:**
The regex `'.*\((.*)\).*'` is vulnerable to ReDoS (Regular Expression Denial of Service) with crafted input containing many nested parentheses.

**Attack Vector:**
```bash
# Malicious simulator name (unlikely but possible)
xcrun simctl create "iPhone ((((((((((((((((( Test" com.apple.CoreSimulator.SimDeviceType.iPhone-15
```

**Impact:** LOW
- Potential for script hang/DoS
- Requires attacker to create malicious simulator (low probability)
- Only affects local development environment

**Fix Required:**
Use simpler, non-greedy regex or parse UUID directly:

```bash
detect_simulator() {
    # First, try to get a booted simulator by UUID directly
    local booted_sim=$(xcrun simctl list devices | grep "Booted" | grep -oE '[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}' | head -1)
    if [[ -n "$booted_sim" ]]; then
        echo "platform=iOS Simulator,id=${booted_sim}"
        return 0
    fi
    # Rest of function...
}
```

---

### MEDIUM-4: Missing SonarCloud Token Validation

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:124-145`

**Current Code:**
```bash
run_sonarcloud_analysis() {
    # ...
    if [[ -f "$PROJECT_ROOT/scripts/sonar-analysis.sh" ]]; then
        if "$PROJECT_ROOT/scripts/sonar-analysis.sh" 2>&1 | tee -a "$LOG_FILE"; then
            log SUCCESS "SonarCloud analysis completed successfully"
        else
            log WARN "SonarCloud analysis failed - continuing deployment"
        fi
    fi
}
```

**Issue:**
No validation that `SONAR_TOKEN` is set or valid before running analysis. Failed SonarCloud scans could indicate:
1. Expired token (security issue - token should be rotated)
2. Compromised token (rate limiting or ban)
3. Analysis containing security vulnerabilities being ignored

**Impact:** LOW (informational)
- Security scan failures are silently ignored
- No alerting when analysis fails
- Could mask security vulnerabilities

**Fix Required:**
Add token validation and failure alerting:

```bash
run_sonarcloud_analysis() {
    print_header "Running SonarCloud Analysis"

    if [[ "$SKIP_SONAR" == "true" ]]; then
        log WARN "SonarCloud analysis skipped by configuration"
        return 0
    fi

    # Validate SonarCloud token exists
    if [[ -z "${SONAR_TOKEN:-}" ]]; then
        log WARN "SONAR_TOKEN not set - skipping SonarCloud analysis"
        log INFO "Set SONAR_TOKEN to enable code quality analysis"
        return 0
    fi

    log INFO "Running code quality analysis with SonarCloud..."

    if [[ -f "$PROJECT_ROOT/scripts/sonar-analysis.sh" ]]; then
        if "$PROJECT_ROOT/scripts/sonar-analysis.sh" 2>&1 | tee -a "$LOG_FILE"; then
            log SUCCESS "SonarCloud analysis completed successfully"
            log INFO "View results at: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile"
        else
            local exit_code=$?
            log WARN "SonarCloud analysis failed (exit code: $exit_code)"

            # Check for common issues
            if [[ $exit_code -eq 401 ]] || [[ $exit_code -eq 403 ]]; then
                log ERROR "Authentication failed - SONAR_TOKEN may be invalid or expired"
                log ERROR "Please regenerate token at: https://sonarcloud.io/account/security"
            fi

            log WARN "Continuing deployment, but review SonarCloud issues"
        fi
    else
        log WARN "SonarCloud script not found - skipping analysis"
    fi
}
```

---

## Low Priority Issues (INFORMATIONAL)

### LOW-1: Emulator Boot Race Condition

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:427-433`

**Current Code:**
```bash
emulator -avd "$emulator_name" -no-window &
local emulator_pid=$!

# Wait for emulator
log INFO "Waiting for emulator to start..."
adb wait-for-device
sleep 10
```

**Issue:**
Fixed 10-second sleep is arbitrary. Emulator might not be fully booted:
- `adb wait-for-device` only waits for ADB connection
- System UI might not be ready
- App installation could fail due to incomplete boot

**Impact:** LOW (reliability, not security)
- Intermittent deployment failures
- Does not create security vulnerability

**Fix Required (if desired):**
Use proper boot completion check:

```bash
emulator -avd "$emulator_name" -no-window &
local emulator_pid=$!

log INFO "Waiting for emulator to start..."
adb wait-for-device

# Wait for boot completion
log INFO "Waiting for Android system to boot..."
timeout=60
while [[ $timeout -gt 0 ]]; do
    boot_completed=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [[ "$boot_completed" == "1" ]]; then
        log SUCCESS "Emulator booted successfully"
        break
    fi
    sleep 2
    ((timeout-=2))
done

if [[ $timeout -le 0 ]]; then
    log ERROR "Emulator failed to boot within 60 seconds"
    return 1
fi
```

---

### LOW-2: No Verification of Git Remote URL

**Location:** `/Users/adamstack/SmilePile/deploy/deploy_qual.sh:596-600`

**Current Code:**
```bash
log INFO "Pushing to GitHub..."
git push origin "$(git rev-parse --abbrev-ref HEAD)"

if [[ "$TAG_VERSION" == "true" ]]; then
    git push origin --tags
fi
```

**Issue:**
No verification that the git remote "origin" points to the correct repository. An attacker who compromises the developer's machine could:
1. Change the remote URL to a malicious repository
2. Script would push code to attacker's repo
3. Attacker gains access to source code and credentials in .env files

**Impact:** LOW (requires prior machine compromise)
- Code exfiltration possible
- Credentials could be pushed to wrong repository

**Fix Required (if desired):**
Add remote URL verification:

```bash
commit_to_github() {
    # ... existing code ...

    # Verify git remote points to expected repository
    local remote_url=$(git remote get-url origin 2>/dev/null || echo "")
    local expected_patterns=("github.com/ajstack22/SmilePile" "github.com:ajstack22/SmilePile")

    local valid_remote=false
    for pattern in "${expected_patterns[@]}"; do
        if [[ "$remote_url" =~ $pattern ]]; then
            valid_remote=true
            break
        fi
    done

    if [[ "$valid_remote" != "true" ]]; then
        log ERROR "Git remote URL does not match expected repository"
        log ERROR "Current remote: $remote_url"
        log ERROR "Expected pattern: github.com/ajstack22/SmilePile"
        log ERROR "Aborting push to prevent accidental code exposure"
        return 1
    fi

    log INFO "Pushing to GitHub..."
    git push origin "$(git rev-parse --abbrev-ref HEAD)"
    # ... rest of function ...
}
```

---

## Security Recommendations

### Recommendation 1: Implement Secrets Scanning in Pre-Commit Hook

**Priority:** HIGH
**Effort:** Medium (2-4 hours)

Add automated secrets scanning using `gitleaks` or similar tool:

```bash
# .git/hooks/pre-commit
#!/bin/bash

echo "Running secrets scan..."
if command -v gitleaks >/dev/null 2>&1; then
    gitleaks detect --source . --verbose --redact
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Secrets detected in commit!"
        echo "Please remove secrets and try again."
        exit 1
    fi
else
    echo "WARNING: gitleaks not installed. Install with: brew install gitleaks"
fi
```

**Rationale:**
- Prevents accidental credential commits
- Current `.gitignore` is good but not foolproof
- Developer might commit secrets via `git add -f`

---

### Recommendation 2: Add Keystore Integrity Verification

**Priority:** MEDIUM
**Effort:** Low (1-2 hours)

Verify keystore hasn't been tampered with before signing:

```bash
# In deploy_qual.sh before Android deployment
verify_keystore_integrity() {
    local keystore_path="${ANDROID_KEYSTORE_PATH:-}"
    local checksum_file="${keystore_path}.sha256"

    if [[ ! -f "$keystore_path" ]]; then
        log ERROR "Keystore not found: $keystore_path"
        return 1
    fi

    if [[ ! -f "$checksum_file" ]]; then
        log WARN "Keystore checksum file not found. Creating..."
        calculate_checksum "$keystore_path" > "$checksum_file"
        chmod 600 "$checksum_file"
        return 0
    fi

    local expected_checksum=$(cat "$checksum_file")
    if verify_checksum "$keystore_path" "$expected_checksum"; then
        log SUCCESS "Keystore integrity verified"
        return 0
    else
        log ERROR "CRITICAL: Keystore has been modified!"
        log ERROR "Expected: $expected_checksum"
        log ERROR "Actual:   $(calculate_checksum "$keystore_path")"
        log ERROR "Keystore may be compromised. Aborting deployment."
        return 1
    fi
}
```

**Rationale:**
- Detect keystore tampering
- Prevent signing with compromised keystore
- Alert to potential security breach

---

### Recommendation 3: Implement Deployment Audit Trail

**Priority:** MEDIUM
**Effort:** Medium (2-3 hours)

Create tamper-evident deployment log:

```bash
# Add to deploy_qual.sh
create_deployment_audit_entry() {
    local audit_file="${PROJECT_ROOT}/.deployment-audit.log"

    local entry=$(cat <<EOF
{
    "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
    "deployment_id": "$DEPLOYMENT_ID",
    "platform": "$PLATFORM",
    "version": "$VERSION_NAME",
    "user": "$USER",
    "hostname": "$(hostname)",
    "git_commit": "$(git rev-parse HEAD)",
    "git_branch": "$(git rev-parse --abbrev-ref HEAD)",
    "tests_run": ${SKIP_TESTS:-false},
    "sonar_run": ${SKIP_SONAR:-false},
    "checksum": ""
}
EOF
)

    # Calculate checksum of entry
    local checksum=$(echo "$entry" | shasum -a 256 | awk '{print $1}')
    entry=$(echo "$entry" | sed "s/\"checksum\": \"\"/\"checksum\": \"$checksum\"/")

    # Append to audit log
    echo "$entry" >> "$audit_file"
    chmod 600 "$audit_file"

    log INFO "Deployment audit entry created"
}
```

**Rationale:**
- Track who deployed what and when
- Detect unauthorized deployments
- Comply with audit requirements for production systems

---

### Recommendation 4: Add Rate Limiting to Prevent CI/CD Abuse

**Priority:** LOW
**Effort:** Low (1 hour)

Prevent excessive deployments (DoS or resource abuse):

```bash
# Add to deploy_qual.sh
check_deployment_rate_limit() {
    local rate_limit_file="${DEPLOY_ROOT}/.deployment-rate-limit"
    local max_deployments_per_hour=10
    local current_hour=$(date +%Y%m%d%H)

    # Read or create rate limit file
    if [[ -f "$rate_limit_file" ]]; then
        local stored_hour=$(sed -n '1p' "$rate_limit_file")
        local count=$(sed -n '2p' "$rate_limit_file")

        if [[ "$stored_hour" == "$current_hour" ]]; then
            if [[ "$count" -ge "$max_deployments_per_hour" ]]; then
                log ERROR "Deployment rate limit exceeded"
                log ERROR "Maximum $max_deployments_per_hour deployments per hour"
                log ERROR "This may indicate a misconfigured CI/CD pipeline"
                return 1
            fi
            ((count++))
        else
            # New hour
            stored_hour="$current_hour"
            count=1
        fi
    else
        stored_hour="$current_hour"
        count=1
    fi

    # Update rate limit file
    echo "$stored_hour" > "$rate_limit_file"
    echo "$count" >> "$rate_limit_file"

    log INFO "Deployment rate limit: $count / $max_deployments_per_hour this hour"
    return 0
}
```

---

## Approved for Implementation: YES (WITH CONDITIONS)

**Conditions:**
1. **CRITICAL-1 MUST be fixed** - iOS simulator command injection vulnerability
2. **HIGH-1 recommended** - Add input validation for test output paths
3. **HIGH-2 recommended** - Implement jq version checking and installation validation
4. **HIGH-3 recommended** - Add log file sanitization and permission restrictions

**Other findings** can be addressed in future iterations but do not block Wave 6 implementation.

---

## Security Validation Checklist

Before deployment to QUAL, verify:

- [ ] iOS simulator detection validates environment variable input (CRITICAL-1)
- [ ] Test output file paths validated (HIGH-1)
- [ ] jq dependency check includes version validation (HIGH-2)
- [ ] Log files created with 600 permissions (HIGH-3)
- [ ] Keystore files have 600 permissions (verified: ✓)
- [ ] .gitignore covers all sensitive files (verified: ✓)
- [ ] No hardcoded credentials in scripts (verified: ✓)
- [ ] Git history clean of secrets (verified: ✓)
- [ ] Secrets directory has 700 permissions (verified: ✓)

---

## Positive Security Findings

The following security best practices were observed and should be maintained:

1. **Excellent Secrets Management:**
   - Keystore file permissions: 600 (owner read/write only) ✓
   - Secrets directory: 700 (owner only) ✓
   - Comprehensive .gitignore for sensitive files ✓
   - No hardcoded passwords in deployment scripts ✓

2. **Good Configuration Management:**
   - Environment-based configuration separation ✓
   - Support for encrypted secrets (AES-256-CBC) ✓
   - Clear separation of development/staging/production configs ✓

3. **Secure Git Practices:**
   - No keystore files in git history ✓
   - No password strings committed ✓
   - Service account JSON files gitignored ✓

4. **Fail-Safe Defaults:**
   - Git uncommitted changes check (blocks deployment) ✓
   - Test failures block deployment (Tier 1 and 2) ✓
   - Dry run mode available for testing ✓

---

## Security Testing Commands

Use these commands to validate security fixes:

```bash
# Test 1: Verify CRITICAL-1 fix (command injection protection)
IOS_SIMULATOR_NAME='iPhone 15"; echo "INJECTED"' ./deploy/deploy_qual.sh ios --dry-run
# Expected: Should ERROR and not execute injection

# Test 2: Verify HIGH-1 fix (output path validation)
tier1_output="../../../etc/passwd" ./deploy/deploy_qual.sh android
# Expected: Should reject invalid path

# Test 3: Verify HIGH-3 (log file permissions)
./deploy/deploy_qual.sh both --dry-run
ls -la deploy/logs/deploy_*.log
# Expected: Should show -rw------- (600 permissions)

# Test 4: Verify keystore permissions
ls -la android/app/keystore.properties
# Expected: -rw------- (600 permissions) ✓ VERIFIED

# Test 5: Check for secrets in git history
git log --all --full-history --source -- "*keystore*" "*password*" "*.env"
# Expected: Only .env.example and documentation references

# Test 6: Verify .gitignore coverage
git check-ignore -v android/keystore.properties
git check-ignore -v deploy/secrets/
# Expected: Both should be ignored
```

---

## Conclusion

The Wave 6 QUAL tier validation changes are **APPROVED FOR IMPLEMENTATION** with the condition that **CRITICAL-1** (iOS simulator command injection) is fixed before the code is deployed.

The deployment infrastructure demonstrates strong security fundamentals with proper secrets management, file permissions, and separation of concerns. The identified issues are primarily defensive improvements to prevent future vulnerabilities rather than exploitation of existing ones.

**Security Risk Level After Fixes: LOW**

**Recommended Timeline:**
- Fix CRITICAL-1: Immediate (before Phase 5 implementation)
- Fix HIGH issues: Within this wave (Phase 5)
- Implement recommendations: Wave 7 or later

---

**Security Review Completed By:** Claude (Sonnet 4.5) - Security Agent
**Review Date:** October 15, 2025
**Next Review:** After Phase 5 implementation (before production deployment)
