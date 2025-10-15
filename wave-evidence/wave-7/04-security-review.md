# Wave 7 Security Review: Tier-Specific Deployment Scripts

**Wave**: Wave 7 - Tier-Specific Deployment Scripts
**Phase**: Phase 4 - Security Review
**Agent**: Security Agent
**Date**: 2025-10-15
**Duration**: 2 hours

---

## Executive Summary

### Overall Security Assessment: **SECURE WITH CONCERNS**

The Wave 7 implementation plan is fundamentally secure but requires attention to several defensive security measures and input validation patterns before implementation begins.

### Critical Vulnerabilities Found: **0**
- No active command injection vulnerabilities found in existing deploy_stage.sh or deploy_prod.sh
- Wave 6 security fix (iOS simulator input validation) already absent from these scripts (they don't use simulators)
- No direct user input used in shell commands without validation

### High-Risk Issues: **2**
1. Missing proactive security patterns in deploy_stage.sh and deploy_prod.sh
2. Lack of git lock mechanism in deploy_stage.sh creates race condition risk

### Medium-Risk Issues: **3**
1. Manual xcodebuild/gradle commands in deploy_prod.sh increase complexity attack surface
2. Custom version bumping logic in deploy_prod.sh susceptible to parsing bugs
3. No input validation framework for new environment variables

### Implementation Blockers: **0**
- No security issues block Wave 7 implementation
- All identified issues can be fixed during implementation

### Top 3 Recommendations:
1. **Add proactive input validation** to all environment variables in new scripts
2. **Implement git lock protection** in deploy_stage.sh before Wave 7 completion
3. **Create security validation checklist** for Phase 6 testing

---

## Wave 6 Security Pattern Audit

### Pattern Replication: **CORRECT**

The technical planning document correctly identifies and documents the Wave 6 security fix pattern:

**Wave 6 Pattern** (from deploy_qual.sh lines 383-422):
```bash
detect_available_simulator() {
    # Security: Allow override but validate input to prevent command injection
    if [[ -n "${IOS_SIMULATOR_NAME:-}" ]]; then
        # CRITICAL: Input validation - only allow alphanumeric, spaces, and hyphens
        if [[ ! "$IOS_SIMULATOR_NAME" =~ ^[a-zA-Z0-9\ \-]+$ ]]; then
            log ERROR "Invalid IOS_SIMULATOR_NAME: contains unsafe characters"
            log ERROR "Only alphanumeric, spaces, and hyphens allowed"
            return 1
        fi
        echo "$IOS_SIMULATOR_NAME"
        return 0
    fi
    # ... fallback logic
}
```

**Security Validation**:
- ✅ Regex pattern `^[a-zA-Z0-9\ \-]+$` is correct and safe
- ✅ Error messages are clear and non-exploitable
- ✅ Rejection happens before any command execution
- ✅ Fallback logic doesn't use untrusted input

### deploy_stage.sh Analysis
**Status**: NO SIMULATOR DETECTION PRESENT
**Risk Level**: LOW (currently) → MEDIUM (if added later)

**Finding**: deploy_stage.sh (277 lines) does not include iOS simulator detection function.

**Reasoning**: Stage tier deploys to TestFlight/Play Console, not local simulators.

**Recommendation**: Add `detect_available_simulator()` function proactively even though not currently used. This prevents future developers from adding simulator detection without security validation.

**Implementation Approach** (from Technical Plan Task 2):
- Add function after line 77 (before main execution)
- Use identical code from deploy_qual.sh lines 383-422
- Add comment: "Wave 7: Proactive security - prevents future command injection"

**VERDICT**: Pattern replication is **CORRECT** in planning document.

### deploy_prod.sh Analysis
**Status**: NO SIMULATOR DETECTION PRESENT
**Risk Level**: LOW (currently) → MEDIUM (if added later)

**Finding**: deploy_prod.sh (686 lines) does not include iOS simulator detection function.

**Reasoning**: Production tier creates store packages, not local simulator builds.

**Recommendation**: Add `detect_available_simulator()` function proactively for same defensive reasons as deploy_stage.sh.

**Implementation Approach** (from Technical Plan Task 3):
- Add function after line 87 (after usage function)
- Use identical code from deploy_qual.sh lines 383-422
- Add comment: "Wave 7: Proactive security - prevents future command injection"

**VERDICT**: Pattern replication is **CORRECT** in planning document.

### deploy_beta.sh Design (New Script)
**Status**: NOT YET CREATED
**Risk Level**: LOW (if implemented correctly)

**Planning Assessment**: Technical plan Task 9 specifies:
> "Include Wave 6 security fixes (from Task 2)"

This means deploy_beta.sh will have simulator detection with validation from day 1.

**VERDICT**: Pattern replication is **CORRECT** in planning document.

### Additional Input Validation Points

**Beyond iOS Simulator Names**, I identified these potential input vectors that need validation:

1. **COMMIT_MESSAGE environment variable** (deploy_qual.sh line 623)
   - Used in: `git commit -m "$commit_msg"`
   - Risk: Command injection if contains backticks or $(...)
   - Current Protection: None explicit
   - Recommendation: Validate or sanitize before use

2. **IOS_SIMULATOR_NAME** (all scripts that may add simulator support)
   - Already covered by Wave 6 fix

3. **File paths from environment variables**:
   - `ANDROID_KEYSTORE_PATH` (deploy_prod.sh line 104)
   - Risk: Path traversal if not validated
   - Current Protection: Existence check only
   - Recommendation: Validate path doesn't contain `..` or special characters

4. **Platform argument** (all scripts)
   - Used in: case statements for routing
   - Current Protection: Whitelist validation (android|ios|both)
   - Status: ✅ SECURE

**VERDICT**: Input validation is MOSTLY CORRECT but can be strengthened.

---

## Command Injection Analysis

### deploy_stage.sh: **SECURE**

**Analysis of potential injection vectors:**

1. **Platform selection** (lines 89-99):
   ```bash
   case "${1:-}" in
       android|ios|both)
           PLATFORM="$1"
   ```
   - ✅ Whitelist validation prevents injection
   - ✅ Only three allowed values

2. **Version management** (lines 140-144):
   ```bash
   update_version_all_platforms "$PLATFORM" || {
   ```
   - ✅ Uses library function (build_number.sh)
   - ✅ Platform validated before use

3. **Test execution** (lines 151-154):
   ```bash
   ./gradlew app:testStageReleaseTier1Critical app:testStageReleaseTier2Important
   ```
   - ✅ Hardcoded test task names
   - ✅ No user input in gradle commands

4. **Fastlane invocation** (lines 189, 210):
   ```bash
   bundle exec fastlane stage_ios
   bundle exec fastlane stage_android
   ```
   - ✅ Hardcoded lane names
   - ✅ No dynamic command construction

5. **Git operations** (lines 232-236):
   ```bash
   git add .build_number ios/SmilePile/Info.plist android/app/build.gradle.kts
   git commit -m "stage: Deploy ${PLATFORM} - v${VERSION_NAME}"
   git tag -a "v${VERSION_NAME}-stage" -m "Stage deployment v${VERSION_NAME}"
   ```
   - ⚠️ `$VERSION_NAME` comes from build_number.sh (trusted source)
   - ⚠️ `$PLATFORM` validated by case statement (trusted)
   - ✅ Commit message uses interpolation (generally safe in bash)
   - ⚠️ POTENTIAL ISSUE: If VERSION_NAME contains special characters from .build_number file tampering

**VERDICT**: deploy_stage.sh is **SECURE** with minor defensive improvements needed.

**Recommendations**:
1. Validate VERSION_NAME format (YY.MM.DD.VVV) before using in git commands
2. Add git lock protection (not for security, but for concurrent safety)

### deploy_prod.sh: **SECURE**

**Analysis of potential injection vectors:**

1. **Platform selection** (lines 609-623):
   - ✅ Same whitelist validation as deploy_stage.sh
   - ✅ SECURE

2. **Custom version bumping** (lines 169-286):
   ```bash
   local current_version=$(grep "versionName" "$gradle_file" | head -n1 | cut -d'"' -f2)
   sed -i '' "s/versionName \"$current_version\"/versionName \"$new_version\"/" "$gradle_file"
   ```
   - ⚠️ Reads version from gradle file (trusted source)
   - ⚠️ Uses sed with user-influenced version (from VERSION_BUMP env var)
   - ⚠️ POTENTIAL ISSUE: If VERSION_BUMP contains regex special characters

   **Example Attack**:
   ```bash
   VERSION_BUMP='1.0.0"; malicious code; #' ./deploy/deploy_prod.sh
   ```

   **Analysis**: Would this work?
   - Sed command: `sed -i '' "s/versionName \"1.0.0\"/versionName \"1.0.0\"; malicious code; #\"/"`
   - The sed command is in double quotes, so special chars are interpreted
   - BUT: Version is validated by regex match in lines 197-198: `[0-9]+\.[0-9]+\.[0-9]+`
   - ✅ Regex validation prevents injection

3. **Test execution** (lines 307-336):
   ```bash
   ./gradlew test
   xcodebuild test -project SmilePile.xcodeproj -scheme SmilePile ...
   ```
   - ✅ Hardcoded commands
   - ✅ No user input

4. **Build commands** (lines 364, 448-457):
   ```bash
   ./gradlew bundleRelease
   xcodebuild archive -project SmilePile.xcodeproj -scheme SmilePile ...
   ```
   - ✅ Hardcoded commands
   - ⚠️ Complex xcodebuild command increases attack surface (not injection, just complexity)

**VERDICT**: deploy_prod.sh is **SECURE** with validation in place.

**Recommendations**:
1. Replace custom version bumping with build_number.sh (reduces complexity)
2. Replace xcodebuild with Fastlane (reduces complexity and potential for bugs)

### deploy_beta.sh Design: **SECURE** (if implemented per plan)

The technical plan specifies deploy_beta.sh will be based on deploy_stage.sh, which is secure.

**Specific Concerns**:
1. Beta approval gate (optional) - no injection vectors identified
2. Fastlane beta lanes - hardcoded, no dynamic construction
3. Tester notification control - Fastlane handles this, no direct shell involvement

**VERDICT**: Design is **SECURE** if implementation follows deploy_stage.sh pattern.

### deploy.sh Router Design: **SECURE**

**Analysis of routing logic** (from Technical Plan lines 694-763):

```bash
TIER="${1:-}"
PLATFORM="${2:-both}"

case "$TIER" in
    qual|quality)
        TIER="qual"
        SCRIPT="deploy_qual.sh"
        ;;
    # ... other tiers
    *)
        echo "ERROR: Invalid tier: $TIER"
        exit 1
        ;;
esac

case "$PLATFORM" in
    android|ios|both)
        ;;
    *)
        echo "ERROR: Invalid platform: $PLATFORM"
        exit 1
        ;;
esac

exec "$SCRIPT_PATH" "$PLATFORM"
```

**Security Validation**:
- ✅ Whitelist validation for tier (qual|stage|beta|prod)
- ✅ Whitelist validation for platform (android|ios|both)
- ✅ Script path constructed from validated tier
- ✅ File existence check before exec
- ✅ `exec` replaces current process (no injection vector)

**Potential Issue - Path Traversal**:
```bash
SCRIPT="deploy_qual.sh"  # User-influenced via tier selection
SCRIPT_PATH="$DEPLOY_ROOT/$SCRIPT"
exec "$SCRIPT_PATH" "$PLATFORM"
```

**Attack Scenario**:
```bash
./deploy.sh "../../../etc/passwd" both
```

**Analysis**: Would this work?
- Tier validation: `../../../etc/passwd` doesn't match any case → ERROR
- ✅ Attack prevented by whitelist validation

**VERDICT**: deploy.sh router design is **SECURE**.

---

## Git Workflow Security

### Manylla Pattern Security: **SECURE**

**Core Principle**: Validate-first, commit-after prevents committing untested code.

**Security Benefits**:
1. ✅ Tests run before any git operations
2. ✅ Failed tests prevent git commit
3. ✅ Version file updates happen before tests, included in commit after validation
4. ✅ No window for committing broken/malicious code

**Potential Security Concern**: Uncommitted changes could contain malicious code that gets tested and then committed.

**Analysis**: This is not a security vulnerability because:
- Developer must explicitly create malicious code locally
- Tests should catch malicious behavior (e.g., data exfiltration, privilege escalation)
- Code review happens via pull requests
- This is a development workflow, not production deployment

**VERDICT**: Manylla pattern is **SECURE** for its purpose.

### Git Lock Mechanism Safety

**Current Implementation** (deploy_qual.sh lines 599-662):
- Uses flock for file-based locking
- 5-second timeout for lock acquisition
- Automatic cleanup on exit via trap

**deploy_stage.sh Status**: ❌ NO GIT LOCK
**Risk**: Concurrent deployments could cause git conflicts

**Security Impact**:
- Not a vulnerability (no privilege escalation or data breach)
- Availability issue (deployments could fail)
- Data integrity issue (git conflicts could corrupt version files)

**Recommendation**: Add git lock to deploy_stage.sh (Technical Plan Task 4).

**deploy_prod.sh Status**: N/A (doesn't commit to git)
**Reasoning**: Production script creates artifacts but doesn't auto-commit.

**VERDICT**: Git lock missing in deploy_stage.sh is a **MEDIUM CONCERN** (not critical security, but important for reliability).

### Git Operation Safety

**Commands Used in deploy_stage.sh** (lines 232-236):
```bash
git add .build_number ios/SmilePile/Info.plist android/app/build.gradle.kts
git commit -m "stage: Deploy ${PLATFORM} - v${VERSION_NAME}"
git tag -a "v${VERSION_NAME}-stage" -m "Stage deployment v${VERSION_NAME}"
git push origin "$(git rev-parse --abbrev-ref HEAD)"
git push origin --tags
```

**Security Analysis**:
1. **git add**: Hardcoded file paths (no injection)
2. **git commit**: Message uses validated variables (no injection)
3. **git tag**: Tag name uses validated variables (no injection)
4. **git push**: Uses git command substitution for branch name
   - `$(git rev-parse --abbrev-ref HEAD)` executes git command
   - Safe because git is trusted command, not user input

**Potential Issue - Git Configuration Tampering**:
- If .git/config is tampered with, git push could push to malicious remote
- This is outside the scope of deployment script security
- Mitigated by file system permissions and code review

**VERDICT**: Git operations are **SECURE**.

### Race Condition Analysis

**Scenario**: Two developers run deploy_stage.sh simultaneously

**Without Git Lock**:
1. Both read .build_number file (version 25.10.15.001)
2. Both increment to 25.10.15.002
3. Both update version files
4. Both run tests (tests pass)
5. Both try to git add/commit/push
6. First push succeeds
7. Second push fails (merge conflict)
8. Second deployment aborts, leaving uncommitted version changes

**Impact**:
- ❌ Version numbers get out of sync
- ❌ Second deployment fails unexpectedly
- ❌ .build_number file could have wrong version

**With Git Lock** (as in deploy_qual.sh):
1. First deployment acquires lock
2. Second deployment waits (5 seconds timeout)
3. If first completes, second acquires lock and proceeds
4. If first takes too long, second aborts gracefully

**VERDICT**: Git lock is **NECESSARY** for deploy_stage.sh and deploy_beta.sh.

### Manylla Pattern vs. Git Lock

These are complementary, not conflicting:
- **Manylla Pattern**: Test before commit (correctness)
- **Git Lock**: Prevent concurrent access (consistency)

Both are needed for a robust deployment system.

---

## Fastlane Integration Security

### Credential Handling: **SECURE WITH CONCERNS**

**iOS Credentials** (Fastfile analysis):
- Uses `app_store_connect_api_key` (lines from Fastfile)
- API key loaded from environment or file
- Never logged or exposed in scripts

**Android Credentials** (Fastfile analysis):
- Uses service account JSON file
- Path specified in Fastfile or environment
- Not exposed in shell commands

**Concern 1 - Credential File Permissions**:
- Service account JSON: Should be 0600 (owner read-only)
- API key file: Should be 0600 (owner read-only)
- Keystore files: Should be 0600 (owner read-only)

**Recommendation**: Add permission checks in prerequisite validation:
```bash
check_prerequisites() {
    # Check credential file permissions
    if [[ -f "$SERVICE_ACCOUNT_JSON" ]]; then
        local perms=$(stat -f "%Lp" "$SERVICE_ACCOUNT_JSON")
        if [[ "$perms" != "600" ]]; then
            log WARN "Service account file has weak permissions: $perms"
            log WARN "Recommended: chmod 600 $SERVICE_ACCOUNT_JSON"
        fi
    fi
}
```

**Concern 2 - Credential Leakage in Logs**:
- deploy_stage.sh logs Fastlane output (lines 189-192)
- Fastlane may include credential paths in error messages
- Not a critical issue (paths are not credentials)

**Recommendation**: Filter sensitive patterns from logs:
```bash
bundle exec fastlane stage_ios 2>&1 | grep -v "api_key\|service_account" || {
    log ERROR "iOS stage deployment failed"
}
```

**VERDICT**: Credential handling is **MOSTLY SECURE** with room for defensive improvements.

### API Key Protection: **SECURE**

**App Store Connect API Key**:
- Loaded by Fastlane from file or environment
- Not passed as command-line argument (would be visible in ps)
- Not echoed in logs

**Play Console Service Account**:
- JSON file path specified in Fastfile
- File contents not exposed in scripts
- Fastlane handles authentication internally

**VERDICT**: API key protection is **SECURE**.

### Service Account JSON File Handling: **SECURE WITH CONCERNS**

**Current Approach**:
- Path specified in environment variable or Fastfile
- File read by Fastlane (Ruby process)
- Not copied or moved by shell scripts

**Concerns**:
1. ✅ File not logged: Scripts don't cat or echo JSON contents
2. ⚠️ File permissions: Not validated in scripts
3. ⚠️ File location: Could be in world-readable directory

**Recommendation**: Add validation in deploy_stage.sh:
```bash
check_prerequisites() {
    # ... existing checks

    # Check service account file
    if [[ "$PLATFORM" == "android" ]] || [[ "$PLATFORM" == "both" ]]; then
        local sa_file="${ANDROID_SERVICE_ACCOUNT:-$PROJECT_ROOT/android/fastlane/service-account.json}"
        if [[ ! -f "$sa_file" ]]; then
            log ERROR "Android service account not found: $sa_file"
            exit 1
        fi

        local perms=$(stat -f "%Lp" "$sa_file")
        if [[ "$perms" -gt 600 ]]; then
            log ERROR "Service account file has insecure permissions: $perms"
            log ERROR "Fix with: chmod 600 $sa_file"
            exit 1
        fi
    fi
}
```

**VERDICT**: Service account handling is **SECURE** but would benefit from permission validation.

### Fastlane Command Injection: **SECURE**

**Analysis of Fastlane invocations**:

**deploy_stage.sh**:
```bash
bundle exec fastlane stage_ios
bundle exec fastlane stage_android
```

**deploy_prod.sh (planned)**:
```bash
bundle exec fastlane prod_ios
bundle exec fastlane prod_android
```

**Security Validation**:
- ✅ Lane names are hardcoded (no user input)
- ✅ No dynamic command construction
- ✅ No environment variable interpolation in lane names
- ✅ Fastlane handles argument parsing internally

**Potential Attack**: Malicious Fastfile
- If Fastfile contains malicious Ruby code, it could be executed
- This is outside the scope of deployment script security
- Mitigated by code review and version control

**VERDICT**: Fastlane commands are **SECURE** from injection.

---

## File System Security

### Temp File Handling: **SECURE**

**Analysis of temporary files**:

**Test output files** (deploy_qual.sh lines 172-173):
```bash
local tier1_output="/tmp/tier1-android-output.txt"
./gradlew app:testTier1Critical 2>&1 | tee "$tier1_output"
```

**Security Concerns**:
1. ⚠️ Predictable filename: `/tmp/tier1-android-output.txt`
2. ⚠️ Symlink attack: Malicious user could create symlink before script runs
3. ⚠️ Race condition: Multiple concurrent deployments overwrite same file

**Risk Assessment**:
- **Likelihood**: LOW (requires local access, malicious intent)
- **Impact**: LOW (test output is not sensitive, no privilege escalation)

**Recommendation**: Use mktemp for secure temp file creation:
```bash
local tier1_output=$(mktemp /tmp/tier1-android-XXXXXX.txt)
trap "rm -f $tier1_output" EXIT
./gradlew app:testTier1Critical 2>&1 | tee "$tier1_output"
```

**VERDICT**: Temp file handling is **MOSTLY SECURE** with minor improvements recommended.

### Artifact Storage: **SECURE**

**Artifact locations**:
- `$DEPLOY_ROOT/artifacts/qual/` (deploy_qual.sh)
- `$DEPLOY_ROOT/artifacts/stage/` (planned for deploy_stage.sh)
- `$DEPLOY_ROOT/artifacts/production/` (deploy_prod.sh)

**Security Analysis**:
1. ✅ Artifacts stored in predictable location (deploy/artifacts/)
2. ✅ Directory created with mkdir -p (safe)
3. ✅ Files copied, not moved (original preserved)
4. ⚠️ No permission checks on artifact directory

**Potential Issue**: World-readable artifacts
- If deploy/artifacts/ has 755 permissions, artifacts are readable by all users
- APKs and AABs could contain proprietary code or assets

**Recommendation**: Set restrictive permissions on artifact directories:
```bash
mkdir -p "$DEPLOY_ROOT/artifacts/prod"
chmod 700 "$DEPLOY_ROOT/artifacts/prod"  # Owner access only
```

**VERDICT**: Artifact storage is **SECURE** but would benefit from explicit permission setting.

### Log File Protection: **SECURE WITH CONCERNS**

**Log files**:
- Location: `$LOG_DIR/deploy_${DEPLOYMENT_ID}.log`
- `LOG_DIR` defined in common.sh (likely `deploy/logs/`)

**Security Analysis**:
1. ✅ Logs stored in predictable location
2. ✅ Unique filename per deployment (timestamp-based)
3. ⚠️ Logs may contain sensitive information:
   - Fastlane output (could include API responses)
   - Error messages (could include file paths)
   - Version numbers (not sensitive)
   - Git commit hashes (not sensitive)

**Concern**: Logs are potentially world-readable

**Recommendation**: Set restrictive permissions on log directory:
```bash
init_deployment_system() {
    mkdir -p "$LOG_DIR"
    chmod 700 "$LOG_DIR"  # Owner access only
}
```

**VERDICT**: Log file protection is **MOSTLY SECURE** with permission improvements recommended.

### Path Traversal Analysis

**Potential attack vectors**:

1. **Artifact paths**: `$DEPLOY_ROOT/artifacts/prod/SmilePile-${DEPLOYMENT_ID}.aab`
   - `DEPLOYMENT_ID` is generated by script: `prod_$(date +%Y%m%d_%H%M%S)`
   - ✅ No user input, no path traversal

2. **Script paths**: `$DEPLOY_ROOT/$SCRIPT` (deploy.sh router)
   - `SCRIPT` comes from validated tier selection
   - ✅ Whitelist validation prevents traversal

3. **Keystore path**: `${ANDROID_KEYSTORE_PATH}` (deploy_prod.sh line 104)
   - Environment variable, could be set by user
   - ⚠️ No validation that path doesn't contain `..` or `/../../`
   - ⚠️ Existence check only (line 104-107)

**Attack Scenario**:
```bash
ANDROID_KEYSTORE_PATH="/etc/passwd" ./deploy/deploy_prod.sh android
```

**Analysis**: Would this work?
- Script checks: `if [[ ! -f "${ANDROID_KEYSTORE_PATH:-}" ]]`
- If /etc/passwd exists, check passes
- Gradle tries to use /etc/passwd as keystore
- Gradle fails (invalid keystore format)
- ✅ No privilege escalation (can't read arbitrary files due to keystore validation)

**Recommendation**: Add path validation:
```bash
if [[ "$ANDROID_KEYSTORE_PATH" =~ \.\. ]]; then
    log ERROR "Invalid keystore path: contains path traversal"
    exit 1
fi
```

**VERDICT**: Path traversal risk is **LOW** but defensive validation recommended.

---

## Version Number Security

### .build_number Handling: **SECURE WITH CONCERNS**

**File location**: `$PROJECT_ROOT/.build_number`

**Usage in build_number.sh library**:
1. Read current version from .build_number
2. Parse date and counter
3. Increment counter if same date, reset if new date
4. Write updated version back to .build_number
5. Update iOS Info.plist and Android build.gradle.kts

**Security Analysis**:

**Concern 1 - File Tampering**:
- Malicious developer could modify .build_number before deployment
- Example: Change `25.10.15.003` to `25.10.15.999`
- Impact: Version mismatch, potential store rejection

**Analysis**:
- File is tracked in git (committed after deployment)
- Tampering would show in git diff
- Code review would catch tampering
- Not a critical security issue (no privilege escalation)

**Concern 2 - Race Condition**:
- Two deployments read .build_number simultaneously
- Both see version 25.10.15.001
- Both write 25.10.15.002
- One overwrites the other

**Current Protection**: None in build_number.sh

**Analysis**: Should there be file locking?
- Git lock in deploy_qual.sh prevents concurrent deployments
- deploy_stage.sh doesn't have git lock (ISSUE!)
- Race condition is possible in deploy_stage.sh

**Recommendation**: Add file lock to .build_number operations:
```bash
update_version_all_platforms() {
    local lock_file="$PROJECT_ROOT/.build_number.lock"

    # Acquire lock
    exec 200>"$lock_file"
    flock -w 5 200 || {
        log ERROR "Could not acquire version lock"
        return 1
    }

    # Update version (existing logic)
    # ...

    # Release lock
    flock -u 200
}
```

**VERDICT**: Version number handling is **MOSTLY SECURE** but race condition risk exists without git lock.

### File Permission Security

**Concern**: Is .build_number world-writable?

**Analysis**:
- File created by git (default permissions)
- Likely 0644 (owner write, group/world read)
- Other users can't write (no security issue)

**Recommendation**: No changes needed.

### Corrupt Version Number Handling

**Scenario**: .build_number contains malformed data
```
VERSION=25.10.15.00X  # Invalid counter
```

**build_number.sh behavior**:
- Regex validation: `^[0-9]{2}\.[0-9]{2}\.[0-9]{2}\.[0-9]{3}$`
- Should reject invalid format
- Fallback: Generate new version

**Concern**: What if validation is bypassed?

**Analysis**:
- Invalid version would be used in git commit message
- Example: `git commit -m "qual: Deploy both - v25.10.15.00X"`
- Git accepts this (no security issue)
- Store submission might fail (QA issue, not security)

**Recommendation**: Ensure build_number.sh validates format strictly.

**VERDICT**: Corrupt version handling is **SECURE** with validation in place.

---

## New Scripts Security Assessment

### deploy_beta.sh Design: **SECURE**

**Based on**: deploy_stage.sh structure (Technical Plan Task 9)

**Differences**:
1. Fastlane lanes: `beta_ios`, `beta_android`
2. Git tag suffix: `-beta`
3. Test tasks: `testBetaReleaseTier1Critical`, etc.
4. Optional approval gate

**Security Analysis**:

**Approval Gate** (from Technical Plan lines 1146-1161):
```bash
beta_approval() {
    if [[ "$REQUIRE_APPROVAL" == "false" ]] || [[ "$CI" == "true" ]]; then
        return 0
    fi

    read -r -p "Are you sure? (yes/no): " response

    if [[ "$response" != "yes" ]]; then
        log ERROR "Beta deployment cancelled"
        exit 1
    fi
}
```

**Security Validation**:
- ✅ `read -r` prevents backslash interpretation
- ✅ Response validated (must be "yes")
- ✅ No command execution based on user input
- ✅ Can be bypassed in CI (expected behavior)

**Other Security Considerations**:
- Same Fastlane integration as deploy_stage.sh (secure)
- Same Manylla pattern as deploy_stage.sh (secure)
- Same quality gates as deploy_stage.sh (secure)
- Wave 6 security fixes included from day 1 (secure)

**VERDICT**: deploy_beta.sh design is **SECURE**.

### deploy.sh Router: **SECURE**

**Input Validation** (from Technical Plan lines 702-740):

```bash
case "$TIER" in
    qual|quality|stage|staging|beta|prod|production)
        # Map to canonical name
        ;;
    *)
        echo "ERROR: Invalid tier: $TIER"
        exit 1
        ;;
esac

case "$PLATFORM" in
    android|ios|both)
        ;;
    *)
        echo "ERROR: Invalid platform: $PLATFORM"
        exit 1
        ;;
esac
```

**Security Validation**:
- ✅ Whitelist validation for tier
- ✅ Whitelist validation for platform
- ✅ Clear error messages (no sensitive information)
- ✅ No command execution based on invalid input

**Script Execution** (from Technical Plan line 762):
```bash
exec "$SCRIPT_PATH" "$PLATFORM"
```

**Security Validation**:
- ✅ `SCRIPT_PATH` constructed from validated tier
- ✅ File existence check before exec
- ✅ `exec` replaces process (no background jobs)
- ✅ No command injection vectors

**Environment Variable Pass-Through**:
- Router inherits environment variables
- All variables passed to tier scripts
- No new variables introduced by router

**Potential Issue**: Could malicious environment variables be passed?

**Analysis**:
- Example: `MALICIOUS='$(rm -rf /)' ./deploy/deploy.sh qual both`
- Variable would be passed to deploy_qual.sh
- deploy_qual.sh doesn't use MALICIOUS variable
- Bash doesn't evaluate variable contents automatically
- ✅ No injection unless tier script explicitly uses variable

**VERDICT**: deploy.sh router is **SECURE**.

---

## Additional Security Recommendations

### 1. Deployment Locking to Prevent Concurrent Executions

**Current State**:
- deploy_qual.sh: ✅ Has git lock
- deploy_stage.sh: ❌ No git lock
- deploy_prod.sh: N/A (no git operations)
- deploy_beta.sh: ❌ No git lock (not created yet)

**Recommendation**: Add git lock to deploy_stage.sh and deploy_beta.sh

**Security Benefit**:
- Prevents race conditions in .build_number updates
- Prevents git merge conflicts
- Prevents version number desynchronization

**Implementation**: Reuse lock mechanism from deploy_qual.sh

**Priority**: HIGH (reliability issue, not security vulnerability)

### 2. Disk Space Checks to Prevent DoS

**Current State**: No disk space checks in any script

**Attack Scenario**:
- Deployment runs out of disk space during build
- Partial artifacts created
- Build fails midway
- Deployment state inconsistent

**Recommendation**: Add disk space check in prerequisite validation:
```bash
check_prerequisites() {
    # ... existing checks

    # Check disk space (require 5GB free)
    local free_space=$(df -k "$DEPLOY_ROOT" | awk 'NR==2 {print $4}')
    local required_space=$((5 * 1024 * 1024))  # 5GB in KB

    if [[ $free_space -lt $required_space ]]; then
        log ERROR "Insufficient disk space: $(($free_space / 1024 / 1024))GB free"
        log ERROR "Required: 5GB for build artifacts"
        exit 1
    fi
}
```

**Security Benefit**:
- Prevents accidental DoS from full disk
- Prevents partial builds
- Improves reliability

**Priority**: MEDIUM (nice to have, not critical)

### 3. Rate Limiting on Deployments

**Current State**: No rate limiting

**Attack Scenario**:
- Malicious actor triggers deployments repeatedly
- Depletes CI/CD resources
- Increases costs (TestFlight builds, Play Console uploads)

**Analysis**: Is this a real threat?
- Requires git push access (trusted developers)
- Manual deployments: Limited by human time
- CI deployments: Rate-limited by CI system

**Recommendation**: Not necessary for Wave 7

**Priority**: LOW (not a practical threat)

### 4. Deployment Log Encryption

**Current State**: Logs stored in plaintext

**Sensitivity Analysis**:
- Logs contain: Commands executed, error messages, version numbers
- Logs may contain: File paths, branch names, commit hashes
- Logs should not contain: API keys, passwords, secrets

**Recommendation**: Not necessary if logs don't contain secrets

**Alternative**: Ensure logs don't contain sensitive information:
```bash
bundle exec fastlane stage_ios 2>&1 | grep -v "api_key\|password\|secret" | tee -a "$LOG_FILE"
```

**Priority**: LOW (preventive measure, not critical)

### 5. Audit Logging of Who Deployed What

**Current State**: Deployment logs capture what was deployed, but not who

**Enhancement**: Add user tracking to logs:
```bash
log INFO "Deployment initiated by: $(whoami)"
log INFO "From host: $(hostname)"
log INFO "Git user: $(git config user.name) <$(git config user.email)>"
```

**Security Benefit**:
- Accountability for production deployments
- Forensics in case of issues
- Compliance with audit requirements

**Recommendation**: Add to all deployment scripts

**Priority**: MEDIUM (good practice, not critical)

### 6. Input Validation Framework

**Current State**: Input validation scattered across scripts

**Recommendation**: Create validation functions in common.sh:
```bash
# Validate version number format
validate_version() {
    local version=$1
    if [[ ! "$version" =~ ^[0-9]{2}\.[0-9]{2}\.[0-9]{2}\.[0-9]{3}$ ]]; then
        log ERROR "Invalid version format: $version"
        return 1
    fi
}

# Validate platform
validate_platform() {
    local platform=$1
    case "$platform" in
        android|ios|both) return 0 ;;
        *) log ERROR "Invalid platform: $platform"; return 1 ;;
    esac
}

# Validate file path (no path traversal)
validate_path() {
    local path=$1
    if [[ "$path" =~ \.\. ]]; then
        log ERROR "Invalid path: contains .."
        return 1
    fi
}
```

**Security Benefit**:
- Centralized validation logic
- Consistent validation across scripts
- Easier to audit and maintain

**Priority**: HIGH (improves maintainability and security)

### 7. Secrets Detection in Logs

**Current State**: No automatic secrets detection

**Recommendation**: Add secrets detection using grep patterns:
```bash
# After deployment completes
detect_secrets_in_logs() {
    local log_file=$1

    # Patterns that might indicate secrets
    local patterns=(
        "api[_-]?key"
        "password"
        "secret"
        "token"
        "credential"
    )

    for pattern in "${patterns[@]}"; do
        if grep -qi "$pattern" "$log_file"; then
            log WARN "Potential secret detected in logs: $pattern"
            log WARN "Review log file: $log_file"
        fi
    done
}
```

**Security Benefit**:
- Prevents accidental secret leakage
- Alerts developer to review logs
- Compliance with security best practices

**Priority**: MEDIUM (preventive measure)

---

## Implementation Blockers

### Critical Blockers: **NONE**

No security issues block Wave 7 implementation.

### Blocking Recommendations: **NONE**

All security improvements can be implemented during Wave 7:
- Wave 6 security pattern (proactive, not blocking)
- Git lock mechanism (reliability, not security)
- Input validation framework (enhancement, not blocker)

### Non-Blocking Recommendations: **7**

The following can be implemented in Wave 7 or deferred to Wave 8:
1. Disk space checks (nice to have)
2. Audit logging (good practice)
3. Secrets detection (preventive)
4. Credential file permission validation (defensive)
5. Temp file security improvements (low risk)
6. Log encryption (unnecessary if no secrets)
7. Rate limiting (not a practical threat)

---

## Security Testing Requirements

### Test 1: Command Injection Validation

**Objective**: Verify input validation prevents command injection

**Test Cases**:

1. **iOS Simulator Name Injection** (deploy_qual.sh, deploy_stage.sh, deploy_prod.sh):
   ```bash
   # Test malicious simulator name
   export IOS_SIMULATOR_NAME="iPhone 15; rm -rf /"
   ./deploy/deploy_qual.sh ios

   # Expected: Error message about unsafe characters
   # Expected: Deployment aborts
   # Expected: No files deleted
   ```

2. **Platform Injection** (all scripts):
   ```bash
   ./deploy/deploy_stage.sh "android; rm -rf /"

   # Expected: Error about invalid platform
   # Expected: Deployment aborts
   ```

3. **Tier Injection** (deploy.sh router):
   ```bash
   ./deploy/deploy.sh "qual; rm -rf /" both

   # Expected: Error about invalid tier
   # Expected: Deployment aborts
   ```

### Test 2: Path Traversal Validation

**Objective**: Verify path traversal attempts are blocked

**Test Cases**:

1. **Keystore Path Traversal** (deploy_prod.sh):
   ```bash
   ANDROID_KEYSTORE_PATH="/etc/passwd" ./deploy/deploy_prod.sh android

   # Expected: Gradle rejects invalid keystore
   # Expected: No unauthorized file access
   ```

2. **Script Path Traversal** (deploy.sh router):
   ```bash
   ./deploy/deploy.sh "../../../etc/passwd" both

   # Expected: Error about invalid tier
   # Expected: No arbitrary file execution
   ```

### Test 3: Concurrent Deployment Safety

**Objective**: Verify git lock prevents race conditions

**Test Cases**:

1. **Concurrent qual Deployments**:
   ```bash
   # Terminal 1
   ./deploy/deploy_qual.sh both &

   # Terminal 2 (start immediately)
   ./deploy/deploy_qual.sh both &

   # Expected: Second deployment waits for first
   # Expected: No version number conflicts
   # Expected: No git merge conflicts
   ```

2. **Concurrent stage Deployments** (after git lock added):
   ```bash
   # Terminal 1
   ./deploy/deploy_stage.sh both &

   # Terminal 2 (start immediately)
   ./deploy/deploy_stage.sh both &

   # Expected: Second deployment waits for first
   # Expected: No version number conflicts
   # Expected: No git merge conflicts
   ```

### Test 4: Credential File Security

**Objective**: Verify credential files are protected

**Test Cases**:

1. **Service Account Permissions**:
   ```bash
   # Set insecure permissions
   chmod 644 android/fastlane/service-account.json

   # Run deployment
   ./deploy/deploy_stage.sh android

   # Expected: Warning about insecure permissions
   # Expected: Deployment continues (warning only)
   ```

2. **Missing Service Account**:
   ```bash
   # Rename service account file
   mv android/fastlane/service-account.json android/fastlane/service-account.json.bak

   # Run deployment
   ./deploy/deploy_stage.sh android

   # Expected: Error about missing file
   # Expected: Deployment aborts
   ```

### Test 5: Version Number Validation

**Objective**: Verify version number handling is secure

**Test Cases**:

1. **Corrupt .build_number**:
   ```bash
   # Corrupt .build_number
   echo "VERSION=25.10.15.00X" > .build_number

   # Run deployment
   ./deploy/deploy_qual.sh both

   # Expected: Validation error
   # Expected: Fallback to generating new version
   ```

2. **Missing .build_number**:
   ```bash
   # Remove .build_number
   rm .build_number

   # Run deployment
   ./deploy/deploy_qual.sh both

   # Expected: New .build_number created
   # Expected: Version starts fresh
   ```

### Test 6: DRY_RUN Mode Security

**Objective**: Verify DRY_RUN doesn't execute dangerous operations

**Test Cases**:

1. **DRY_RUN with Production**:
   ```bash
   DRY_RUN=true ./deploy/deploy_prod.sh both

   # Expected: No actual builds
   # Expected: No git operations
   # Expected: No store uploads
   # Expected: Preview of what would happen
   ```

2. **DRY_RUN with Stage**:
   ```bash
   DRY_RUN=true ./deploy/deploy_stage.sh both

   # Expected: No actual builds
   # Expected: No TestFlight uploads
   # Expected: No git commits
   ```

### Test 7: Approval Gate Bypass

**Objective**: Verify approval gates can't be accidentally bypassed

**Test Cases**:

1. **Production Approval Required**:
   ```bash
   # Try to deploy without approval
   echo "no" | ./deploy/deploy_prod.sh both

   # Expected: Deployment cancelled
   # Expected: No artifacts created
   ```

2. **Production Approval Bypass in CI**:
   ```bash
   CI=true ./deploy/deploy_prod.sh both

   # Expected: Approval bypassed
   # Expected: Deployment proceeds
   ```

### Test 8: Router Validation

**Objective**: Verify deploy.sh router correctly validates and routes

**Test Cases**:

1. **Valid Routing**:
   ```bash
   DRY_RUN=true ./deploy/deploy.sh qual both

   # Expected: Routes to deploy_qual.sh
   # Expected: Correct tier script executes
   ```

2. **Invalid Tier**:
   ```bash
   ./deploy/deploy.sh invalid_tier both

   # Expected: Error message
   # Expected: Usage help displayed
   # Expected: Exit with error code
   ```

3. **Invalid Platform**:
   ```bash
   ./deploy/deploy.sh qual invalid_platform

   # Expected: Error message
   # Expected: Usage help displayed
   # Expected: Exit with error code
   ```

---

## Summary of Security Findings

### Overall Security Posture: **STRONG**

Wave 7's implementation plan demonstrates strong security awareness:
- ✅ Wave 6 security patterns correctly identified and documented
- ✅ No command injection vulnerabilities in current scripts
- ✅ Input validation uses whitelist approach (secure by default)
- ✅ No path traversal vulnerabilities (whitelist validation prevents)
- ✅ Fastlane integration follows secure practices
- ✅ Credential handling is appropriate for CI/CD context
- ✅ Git operations are safe from injection

### Risk Distribution

**Critical Risks**: 0
**High Risks**: 2
- Missing proactive security patterns (defensive measure)
- Missing git lock in deploy_stage.sh (reliability issue)

**Medium Risks**: 3
- Manual xcodebuild/gradle commands (complexity increases risk)
- Custom version bumping logic (potential parsing bugs)
- No credential file permission validation (defensive measure)

**Low Risks**: 5
- Predictable temp file names (requires local access)
- Artifact directory permissions (no sensitive data exposure)
- Log file permissions (no secrets in logs)
- Path traversal in keystore path (validated by Gradle)
- Missing audit logging (accountability, not security)

### Implementation Strategy

**Wave 7 Can Proceed Safely** with the following security measures integrated during implementation:

1. **Task 2-3**: Add proactive iOS simulator validation (P0)
2. **Task 4**: Add git lock to deploy_stage.sh (P0)
3. **Task 9**: Include security patterns in deploy_beta.sh from day 1 (P0)
4. **Task 10**: Validate router input thoroughly (P0)
5. **Phase 6**: Execute comprehensive security testing (P0)

**Post-Wave 7 Improvements** (can be deferred to Wave 8):
- Disk space checks
- Audit logging
- Secrets detection in logs
- Credential file permission validation
- Temp file security hardening

### Security Review Sign-Off

**Security Assessment**: ✅ APPROVED FOR IMPLEMENTATION

**Conditions**:
1. All P0 security measures implemented during Phase 5
2. Security test matrix executed in Phase 6
3. No new environment variables introduced without validation
4. All TODO comments related to security addressed before completion

**Reviewer**: Security Agent
**Date**: 2025-10-15
**Next Review**: Phase 6 (Testing) - validate security test results

---

## Appendix: Security Checklist for Phase 5 Implementation

### Pre-Implementation Checklist

- [ ] Review Wave 6 security pattern (iOS simulator validation)
- [ ] Review git lock mechanism from deploy_qual.sh
- [ ] Review input validation patterns (whitelist approach)
- [ ] Review Fastlane security best practices

### During Implementation Checklist

**Task 2 (deploy_stage.sh security)**:
- [ ] Add detect_available_simulator() function
- [ ] Use exact regex: `^[a-zA-Z0-9\ \-]+$`
- [ ] Add clear error messages
- [ ] Test with malicious input (command injection attempt)

**Task 3 (deploy_prod.sh security)**:
- [ ] Add detect_available_simulator() function
- [ ] Use exact regex: `^[a-zA-Z0-9\ \-]+$`
- [ ] Add clear error messages
- [ ] Test with malicious input (command injection attempt)

**Task 4 (Manylla pattern in deploy_stage.sh)**:
- [ ] Add git lock before git operations
- [ ] Use flock with 5-second timeout
- [ ] Add trap for cleanup
- [ ] Test concurrent deployments

**Task 9 (deploy_beta.sh creation)**:
- [ ] Include detect_available_simulator() from day 1
- [ ] Include git lock from day 1
- [ ] Validate approval gate logic
- [ ] Test approval bypass (should require explicit "yes")

**Task 10 (deploy.sh router)**:
- [ ] Validate tier with whitelist
- [ ] Validate platform with whitelist
- [ ] Check script file existence
- [ ] Test invalid inputs (should reject gracefully)

### Post-Implementation Checklist

- [ ] All security test cases passed
- [ ] No new command injection vectors introduced
- [ ] Git lock prevents concurrent deployments
- [ ] Router rejects invalid inputs
- [ ] DRY_RUN mode prevents dangerous operations
- [ ] Approval gates function correctly
- [ ] No secrets in logs
- [ ] Credential files protected

---

**Security Review Complete**: 2025-10-15
**Status**: APPROVED FOR PHASE 5 IMPLEMENTATION
**Next Phase**: Phase 5 - Implementation (Developer Agent)
